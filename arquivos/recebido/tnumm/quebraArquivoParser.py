import os
import sys
import time
import shutil
from lxml import etree
from datetime import datetime
trailer_element = None
# --- Funções auxiliares ---
def log(msg, level="INFO"):
    agora = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(f"[{level}] {agora} - {msg}")


def deep_copy_element(element):
    return etree.fromstring(etree.tostring(element))


def valid_file_size():
    file_size = os.path.getsize(input_file_path)
    if file_size < max_size_bytes:
        log(f"Arquivo menor que {max_size_mb}MB. Movendo para: {destino}")
        shutil.move(input_file_path, os.path.join(destino, os.path.basename(input_file_path)))
        sys.exit(0)


def get_subfolder(path):
    partes = os.path.normpath(path).split(os.sep)
    try:
        idx = partes.index("recebido")
        return partes[idx + 1]
    except ValueError:
        return "desconhecido"


def save_file(blocks, count, nsmaps):
    new_root = deep_copy_element(root)
    corpo = new_root.find(".//ns:corpoMensagem", namespaces=nsmaps)

    if corpo is None:
        log("Elemento <corpoMensagem> não encontrado na cópia do XML.", "ERROR")
        sys.exit(1)

    old_a900 = corpo.find("ns:A900", namespaces=nsmaps)
    if old_a900 is not None:
        corpo.remove(old_a900)

    new_a900_copy = deep_copy_element(base_a900)
    for blk in blocks:
        new_a900_copy.append(blk)

    for total in quantidade_totais:
        new_a900_copy.append(total)

    corpo.append(new_a900_copy)

    if trailer_element is not None:
      corpo.append(trailer_element)

    out_path = os.path.join(temp_folder, f"{base_name}--{count}{ext}")
    etree.ElementTree(new_root).write(out_path, pretty_print=True, encoding="UTF-8", xml_declaration=True)
    log(f"Arquivo gerado: {out_path}")


# --- Variáveis globais ---
max_size_mb = 180
max_size_bytes = max_size_mb * 1024 * 1024

input_file_path = sys.argv[1] if len(sys.argv) > 1 else None
if not input_file_path:
    log("Argumentos inválidos para: python quebraArquivo.py <caminho_arquivo>", "ERROR")
    sys.exit(1)

base_recebido = os.path.normpath(os.environ.get("MONITOR_PATH"))
base_disponiveis = base_recebido.replace("recebido", "disponiveis")

subpasta = get_subfolder(input_file_path)
destino = os.path.join(base_disponiveis, subpasta)
os.makedirs(destino, exist_ok=True)

base_name, ext = os.path.splitext(os.path.basename(input_file_path))
temp_folder = os.path.join(os.path.dirname(input_file_path), base_name)
os.makedirs(temp_folder, exist_ok=True)

quantidade_totais = []
outros_filhos = []
tipo_carga_elem = None
current_blocks = []
current_size = 0
counter = 1

# --- Execução ---
valid_file_size()
log(f"Processando arquivo: {input_file_path}")

# Parse inicial para capturar header
context = etree.iterparse(input_file_path, events=("start", "end"), remove_blank_text=True)
_, root = next(context)  # pega raiz
nsmap = {"ns": root.nsmap[None]} if None in root.nsmap else {}
log(f"Namespace detectado: {nsmap}")

a900 = root.find(".//ns:A900", namespaces=nsmap)
if a900 is None:
    log("Tag <A900> não encontrada.", "ERROR")
    sys.exit(1)

# Coleta de elementos fixos
for child in a900:
    tag_localname = etree.QName(child).localname
    if tag_localname == "tipoCarga":
        tipo_carga_elem = deep_copy_element(child)
    elif tag_localname in ["quantidadeTotalR902", "quantidadeTotalR905"]:
        quantidade_totais.append(deep_copy_element(child))
    elif tag_localname not in ["materiais", "medicamentos"]:
        outros_filhos.append(deep_copy_element(child))

# Criação do A900 base sem materiais/medicamentos
base_a900 = etree.Element(a900.tag, nsmap=a900.nsmap)
if tipo_carga_elem is not None:
    base_a900.append(tipo_carga_elem)
for elem in outros_filhos:
    base_a900.append(elem)

# Adiciona totalizadores por último
for el in quantidade_totais:
    base_a900.append(el)

# Iteração final: captura de blocos <materiais> e <medicamentos>
for event, elem in etree.iterparse(input_file_path, events=("end",), tag=("{*}materiais", "{*}medicamentos"), remove_blank_text=True):
    size = len(etree.tostring(elem, encoding="utf-8"))
    
    if elem.tag.endswith("quantidadeTotalR902") or elem.tag.endswith("quantidadeTotalR905"):
        quantidade_totais.append(deep_copy_element(elem))
    
    if current_size + size > max_size_bytes and current_blocks:
        if event == "end" and elem.tag.endswith("trailer"):
          trailer_element = deep_copy_element(elem)
        
        save_file(current_blocks, counter, nsmap)
        counter += 1
        current_blocks = []
        current_size = 0
    current_blocks.append(deep_copy_element(elem))
    current_size += size

    # Libera memória
    elem.clear()
    while elem.getprevious() is not None:
        del elem.getparent()[0]

if current_blocks:
    if event == "end" and elem.tag.endswith("trailer"):
      trailer_element = deep_copy_element(elem)
    save_file(current_blocks, counter, nsmap)

# Move arquivos quebrados
time.sleep(2)
for fname in os.listdir(temp_folder):
    shutil.move(os.path.join(temp_folder, fname), os.path.join(destino, fname))

# Remove original e temporários
try:
    time.sleep(2)
    os.remove(input_file_path)
    shutil.rmtree(temp_folder)
    log("Arquivo original e temporários removidos.")
except Exception as e:
    log(f"Erro ao remover arquivos temporários: {e}", "ERROR")

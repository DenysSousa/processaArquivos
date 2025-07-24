import os
import sys
import shutil
from lxml import etree
from datetime import datetime

caminho_arquivo = sys.argv[1]

# Verifica se um caminho de arquivo foi passado como argumento
if len(sys.argv) < 2:
    print("Uso: python quebraArquivo.py <caminho_arquivo>")
    sys.exit(1)

input_file_path = sys.argv[1]
max_size_mb = 200
max_size_bytes = max_size_mb * 1024 * 1024

def log(msg, level="INFO"):
    agora = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(f"[{level}] {agora} - {msg}")

# Função auxiliar para extrair a subpasta relativa (tnumm, rol, etc.)
def get_subfolder(path):
    partes = os.path.normpath(path).split(os.sep)
    try:
        idx = partes.index("recebido")
        return partes[idx + 1]
    except ValueError:
        return "desconhecido"

# Caminhos base
base_recebido = os.path.normpath(os.environ.get("MONITOR_PATH"))
base_disponiveis = base_recebido.replace("recebido", "disponiveis")

# Identifica a subpasta
subpasta = get_subfolder(input_file_path)
destino = os.path.join(base_disponiveis, subpasta)
os.makedirs(destino, exist_ok=True)

# Verifica o tamanho do arquivo
file_size = os.path.getsize(input_file_path)

# Se menor que 200MB, apenas move
if file_size < max_size_bytes:
    log(f"Arquivo menor que {max_size_mb}MB. Movendo para: {destino}")
    shutil.move(input_file_path, os.path.join(destino, os.path.basename(input_file_path)))
    sys.exit(0)

# Caso contrário, processa e divide
log(f"Quebrando arquivo: {input_file_path}")

# Parser do XML
parser = etree.XMLParser(remove_blank_text=True)
tree = etree.parse(input_file_path, parser)
root = tree.getroot()

nsmap = {'ns': root.nsmap[None]}
a900 = root.find(".//ns:A900", namespaces=nsmap)
materiais = a900.findall("ns:materiais", namespaces=nsmap)
medicamentos = a900.findall("ns:medicamentos", namespaces=nsmap)

for node in materiais + medicamentos:
    a900.remove(node)

base_name, ext = os.path.splitext(os.path.basename(input_file_path))
temp_folder = os.path.join(os.path.dirname(input_file_path), base_name)
os.makedirs(temp_folder, exist_ok=True)

counter = 1
current_size = 0
current_blocks = []

def save_file(blocks, count):
    new_tree = etree.ElementTree(etree.fromstring(etree.tostring(root)))
    new_root = new_tree.getroot()
    new_a900 = new_root.find(".//ns:A900", namespaces=nsmap)
    for b in blocks:
        new_a900.append(b)
    out_path = os.path.join(temp_folder, f"{base_name}--{count}{ext}")
    new_tree.write(out_path, pretty_print=True, encoding="UTF-8", xml_declaration=True)

all_blocks = materiais + medicamentos

for block in all_blocks:
    size = len(etree.tostring(block, encoding="utf-8"))
    if current_size + size > max_size_bytes and current_blocks:
        save_file(current_blocks, counter)
        counter += 1
        current_blocks = []
        current_size = 0
    current_blocks.append(block)
    current_size += size

if current_blocks:
    save_file(current_blocks, counter)

# Move arquivos quebrados para a pasta de destino
for fname in os.listdir(temp_folder):
    full_path = os.path.join(temp_folder, fname)
    shutil.move(full_path, os.path.join(destino, fname))

# Remove o arquivo original e a pasta temporária
try:
    os.remove(input_file_path)
    shutil.rmtree(temp_folder)
    log(f"Arquivo original e pasta temporária removidos com sucesso.")
except Exception as e:
    log(f"Ao tentar remover arquivos: {e}", "Erro")

import os
import sys
import shutil
from datetime import datetime

# --- Configurações ---
CHUNK_SIZE = 200_000_000  # 200 MB (aprox. em caracteres)
ENCODING = 'utf-8'

# --- Funções auxiliares ---
def log(msg, level="INFO"):
    agora = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(f"[{level}] {agora} - {msg}")

def extrair_cabecalho_e_trailer(texto):
    fim_tipo_carga = texto.find("</tipoCarga>") + len("</tipoCarga>")
    inicio_trailer = texto.rfind("<quantidadeTotalR902>")
    if fim_tipo_carga == -1 or inicio_trailer == -1:
        raise Exception("Tags </tipoCarga> ou <quantidadeTotalR902> não encontradas.")
    return texto[:fim_tipo_carga], texto[inicio_trailer:], fim_tipo_carga, inicio_trailer

def encontrar_posicao_de_corte(trecho):
    marcadores = {
        '</materiais>': trecho.rfind('</materiais>'),
        '</medicamentos>': trecho.rfind('</medicamentos>'),
        '<quantidadeTotalR902>': trecho.rfind('<quantidadeTotalR902>')
    }
    marcadores_validos = {k: v for k, v in marcadores.items() if v != -1}
    if not marcadores_validos:
        return -1, None

    tag_final, posicao_final = max(marcadores_validos.items(), key=lambda x: x[1])
    size_tag = len(tag_final)
    
    if tag_final == '<quantidadeTotalR902>':
        size_tag = 0

    return posicao_final + size_tag, tag_final
    
# Função auxiliar para extrair a subpasta relativa (tnumm, rol, etc.)
def get_subfolder(path):
    partes = os.path.normpath(path).split(os.sep)
    try:
        idx = partes.index("recebido")
        return partes[idx + 1]
    except ValueError:
        return "desconhecido"
    
def move_arquivos():
    base_name, ext = os.path.splitext(os.path.basename(input_path))
    temp_folder = os.path.join(os.path.dirname(input_path), base_name)
    log(temp_folder)
    os.makedirs(temp_folder, exist_ok=True)
    
    # Caminhos base
    base_recebido = os.path.normpath(os.environ.get("MONITOR_PATH"))
    base_disponiveis = base_recebido.replace("recebido", "disponiveis")

    # Identifica a subpasta
    subpasta = get_subfolder(input_path)
    destino = os.path.join(base_disponiveis, subpasta)
    os.makedirs(destino, exist_ok=True)

    # Move arquivos quebrados para a pasta de destino
    for fname in os.listdir(temp_folder):
        full_path = os.path.join(temp_folder, fname)
        shutil.move(full_path, os.path.join(destino, fname))

    # Remove o arquivo original e a pasta temporária
    try:
        os.remove(input_path)
        shutil.rmtree(temp_folder)
        log(f"Arquivo original e pasta temporária removidos com sucesso.")
    except Exception as e:
        log(f"Ao tentar remover arquivos: {e}", "Erro")

# --- Função principal ---
def quebrar_arquivo():
    nome_arquivo = os.path.splitext(os.path.basename(input_path))[0]
    output_dir = os.path.join(os.path.dirname(input_path), nome_arquivo)
    os.makedirs(output_dir, exist_ok=True)

    log(f"Lendo arquivo completo: {input_path}")
    with open(input_path, 'r', encoding=ENCODING) as f:
        texto = f.read()

    cabecalho, trailer, inicio_dados, fim_dados = extrair_cabecalho_e_trailer(texto)
    log("Cabecalho e trailer extraídos.")

    posicao_base = inicio_dados
    indice = 1

    while posicao_base < fim_dados:
        trecho = texto[posicao_base:posicao_base + CHUNK_SIZE]
        pos_corte, tag_usada = encontrar_posicao_de_corte(trecho)

        if pos_corte == -1:
            log(f"Nenhuma tag de corte encontrada no trecho {indice}. Parando.")
            break

        copy_ate = posicao_base + pos_corte
        conteudo = texto[posicao_base:copy_ate]

        nome_saida = os.path.join(output_dir, f"{nome_arquivo}--{indice}.xml")
        with open(nome_saida, 'w', encoding=ENCODING) as f_out:
            f_out.write(cabecalho)
            f_out.write(conteudo)
            f_out.write(trailer)

        log(f"Arquivo gerado: {nome_saida} (tag de corte: {tag_usada})")

        posicao_base = copy_ate
        indice += 1

    move_arquivos()

    log("Processo finalizado.")

# --- Execução direta ---
if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("Uso: python quebra_xml_por_tags.py caminho/para/arquivo.xml")
        sys.exit(1)

    input_path = sys.argv[1]
    quebrar_arquivo()

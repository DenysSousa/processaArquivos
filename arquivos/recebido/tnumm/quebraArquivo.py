import os
import sys
import shutil
import mmap
from datetime import datetime

# --- Configurações ---
CHUNK_SIZE = 200_000_000  # 200 MB (aprox. em caracteres)
ENCODING = 'utf-8'
MAX_SIZE_MB = 200
MAX_SIZE_FILE = MAX_SIZE_MB * 1024 * 1024 #200MB

# --- Funções auxiliares ---
def log(msg, level="INFO"):
    agora = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(f"[{level}] {agora} - {msg}")

def extrair_comeco_e_final(bytesMap):
    limite_inicial = 1000

    trecho = bytesMap[:limite_inicial]  # acessa um pedaço (como uma string)
    texto = trecho.decode('utf-8', errors='ignore')
    pos_corte_cabecalho = texto.find("</tipoCarga>")
    
    while pos_corte_cabecalho == -1:
        limite_inicial += 1000
        trecho = bytesMap[:limite_inicial]  # acessa um pedaço (como uma string)
        texto = trecho.decode('utf-8', errors='ignore')
        pos_corte_cabecalho = texto.find("</tipoCarga>")

    inicio_dados = pos_corte_cabecalho + len("</tipoCarga>")

    #Pega o começo padrão de todos os arquivos gerados
    comeco = texto[:inicio_dados]

    #Pega a parte final do arquivo
    final = bytesMap[-2000:].decode('utf-8', errors='ignore')
    final = final[final.find("<quantidadeTotalR902>"):]
    
    return comeco, final, inicio_dados
    

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
    
    if tag_final == '</medicamentos>' and trecho[:11] == '<materiais>':
        tag_final = '</materiais>'
        posicao_final = trecho.rfind(tag_final)

    size_tag = len(tag_final)

    if tag_final == '<quantidadeTotalR902>':
        size_tag = 0

    return posicao_final + size_tag, tag_final
    
# Função auxiliar para extrair a subpasta relativa (tnumm, rol, etc.)
def get_subfolder(path):
    partes = os.path.normpath(path).split(os.sep)
    try:
        idx = partes.index("recebidos")
        return partes[idx + 1]
    except ValueError:
        return "desconhecido"
    
def move_arquivos():
    base_name, ext = os.path.splitext(os.path.basename(input_path))
    temp_folder = os.path.join(os.path.dirname(input_path), base_name)
    log(temp_folder)
    os.makedirs(temp_folder, exist_ok=True)

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
def verifica_tamanho():
    file_size = os.path.getsize(input_path)

    # Se menor que 200MB, apenas move
    if file_size < MAX_SIZE_FILE:
        log(f"Arquivo menor que {MAX_SIZE_MB}MB. Movendo para: {destino}")
        shutil.move(input_path, os.path.join(destino, os.path.basename(input_path)))
        sys.exit(0)

def quebrar_arquivo():
    nome_arquivo = os.path.splitext(os.path.basename(input_path))[0]
    output_dir = os.path.join(os.path.dirname(input_path), nome_arquivo)
    os.makedirs(output_dir, exist_ok=True)
    
    log(f"Leitura do arquivo com o mmap: {input_path}")
    with open(input_path, 'r+b') as f:
        bytesMap = mmap.mmap(f.fileno(), 0)  # mapeia o arquivo todo

    comeco, final, inicio_dados = extrair_comeco_e_final(bytesMap)
    log("Cabecalho e final extraídos.")

    posicao_base = inicio_dados
    indice = 1

    while posicao_base > 0:
        tMap = bytesMap[posicao_base:posicao_base + CHUNK_SIZE]
        trecho = tMap.decode('utf-8', errors='ignore')
        pos_corte, tag_usada = encontrar_posicao_de_corte(trecho)

        if pos_corte == -1:
            log(f"Nenhuma tag de corte encontrada no trecho {indice}. Parando.")
            break

        conteudo = trecho[:pos_corte]

        nome_saida = os.path.join(output_dir, f"{nome_arquivo}--{indice}.xml")
        with open(nome_saida, 'w', encoding=ENCODING) as f_out:
            f_out.write(comeco)
            f_out.write(conteudo)
            f_out.write(final)

        log(f"Arquivo gerado: {nome_saida} (tag de corte: {tag_usada})")

        posicao_base += pos_corte
        indice += 1
        
        if tag_usada == '<quantidadeTotalR902>':
            posicao_base = 0


    # Libera o mmap da memória
    bytesMap.close()
    move_arquivos()

    log("Processo finalizado.")

# --- Execução direta ---
if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("Uso: python quebra_xml_por_tags.py caminho/para/arquivo.xml")
        sys.exit(1)

    input_path = sys.argv[1]
    
    # Caminhos base
    base_recebido = os.path.normpath(os.environ.get("MONITOR_PATH"))
    base_disponiveis = base_recebido.replace("recebidos", "disponiveis")

    # Identifica a subpasta
    subpasta = get_subfolder(input_path)
    destino = os.path.join(base_disponiveis, subpasta)
    os.makedirs(destino, exist_ok=True)
    
    verifica_tamanho()
    quebrar_arquivo()

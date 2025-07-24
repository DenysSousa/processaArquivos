import os
import time
import threading
import subprocess
from watchdog.observers import Observer
from watchdog.events import FileSystemEventHandler
from datetime import datetime

CAMINHO_BASE = os.environ.get("MONITOR_PATH")

if not CAMINHO_BASE:
    raise EnvironmentError("A variável de ambiente 'MONITOR_PATH' não está definida.")

TEMPO_VERIFICACAO = 5  # segundos

# Dicionário para controlar múltiplas verificações
arquivos_em_transferencia = {}

def log(msg, level="INFO"):
    agora = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(f"[{level}] {agora} - {msg}")

def arquivo_esta_estavel(caminho):
    try:
        tamanho_anterior = os.path.getsize(caminho)
        time.sleep(TEMPO_VERIFICACAO)
        tamanho_atual = os.path.getsize(caminho)
        return tamanho_anterior == tamanho_atual
    except FileNotFoundError:
        return False

def executar_quebra_file(caminho_arquivo):
    pasta = os.path.dirname(caminho_arquivo)
    script = os.path.join(pasta, "quebraArquivo.py")

    if os.path.exists(script):
        log(f"Executando {script} para {os.path.basename(caminho_arquivo)}")
        subprocess.Popen(["python", script, caminho_arquivo], cwd=pasta)
    else:
        log(f"Script quebraArquivo.py não encontrado em {pasta}")

def verificar_arquivo_finalizado(caminho_arquivo):
    if not os.path.isfile(caminho_arquivo):
        return
    if arquivo_esta_estavel(caminho_arquivo):
        executar_quebra_file(caminho_arquivo)
        arquivos_em_transferencia.pop(caminho_arquivo, None)

class ManipuladorEventos(FileSystemEventHandler):
    def on_created(self, event):
        self.tratar_evento(event)

    def on_modified(self, event):
        self.tratar_evento(event)
        
    def tratar_evento(self, event):
        if event.is_directory or event.src_path.lower().endswith(".py"):
            return  # ignora diretórios e scripts .py

        # Caminho relativo à raiz monitorada
        caminho_relativo = os.path.relpath(event.src_path, CAMINHO_BASE)
        partes = caminho_relativo.split(os.sep)

        # Verifica se está dentro de uma subpasta direta (ex: tnumm/arquivo.txt), e não tnumm/outra/sub/arquivo
        if len(partes) != 2:
            return  # ignora arquivos em sub-subpastas

        # Se passou nos filtros, agenda verificação
        self.agendar_verificacao(event.src_path)

    def agendar_verificacao(self, caminho_arquivo):
        if caminho_arquivo not in arquivos_em_transferencia:
            log(f"Novo arquivo detectado: {caminho_arquivo}")
            arquivos_em_transferencia[caminho_arquivo] = True
            threading.Thread(target=verificar_arquivo_finalizado, args=(caminho_arquivo,), daemon=True).start()

# Iniciar observador
if __name__ == "__main__":
    observer = Observer()
    event_handler = ManipuladorEventos()
    observer.schedule(event_handler, CAMINHO_BASE, recursive=True)

    log(f"{CAMINHO_BASE} e subpastas...", "MONITORANDO")
    observer.start()
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        observer.stop()
    observer.join()

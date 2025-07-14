package br.inf.solus.processaArquivos.Thread;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MonitoraTnumm {

    @Autowired
    private static JobLauncher jobLauncher;

    @Autowired
    private static Job processaRemessaJob;

    private static final String DiretorioDisponivel = "disponiveis/tnumm";
    private static final String DiretorioProcessado = "processados/tnumm";

    public static void main(String[] args) {
        try {
            // Lê o diretório a partir da variável de ambiente
            String path = System.getenv("ARQUIVOS_DIR");

            if (path == null || path.isBlank()) {
                System.err.println("Erro: Variável de ambiente 'ARQUIVOS_DIR' não está definida.");
                return;
            }

            Path caminhoDisponiveis = Paths.get(path, DiretorioDisponivel);
            Path caminhoProcessados = Paths.get(path, DiretorioProcessado);

            if (!Files.exists(caminhoDisponiveis) || !Files.isDirectory(caminhoDisponiveis)) {
                System.err.println("Erro: Subpasta '" + caminhoDisponiveis + "' não existe ou não é um diretório.");
                return;
            }

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

            Runnable tarefa = () -> {

                File[] arquivos = caminhoDisponiveis.toFile().listFiles(File::isFile);

                if (arquivos != null) {
                    for (File arquivo : arquivos) {
                        String nomeArquivo = arquivo.getName();
                        String nomeSemExtensao = nomeArquivo.contains(".") ?
                                nomeArquivo.substring(0, nomeArquivo.lastIndexOf('.')) :
                                nomeArquivo;

                        Path destinoFinal = caminhoProcessados.resolve(nomeSemExtensao);

                        // Cria a pasta de destino se não existir
                        try {
                            Files.createDirectories(destinoFinal);
                        } catch (IOException e) {
                            System.out.println("Erro ao criar a pasta " + destinoFinal + "! " + e.getMessage());
                        }

                        // Move o arquivo
                        Path origem = arquivo.toPath();
                        Path destino = destinoFinal.resolve(nomeArquivo);

                        try {
                            Files.move(origem, destino, StandardCopyOption.REPLACE_EXISTING);
                            iniciarJobAsync(destino);
                        } catch (IOException e) {
                            System.out.println("Erro ao mover o arquivo " + origem + "! " + e.getMessage());
                        }

                        System.out.printf("✔ Arquivo '%s' movido para '%s'%n \n", nomeArquivo, destino);
                    }
                }
            };

            scheduler.scheduleAtFixedRate(tarefa, 0, 2, TimeUnit.SECONDS);

        } catch (Exception e) {
            System.out.println("Erro ao processar a leitura " + e.getMessage());

        }
    }

    private static void iniciarJobAsync(Path caminhoCompletoDoArquivo) {
        new Thread(() -> {
            try {
                JobParameters jobParameters = new JobParametersBuilder()
                        .addString("filepath", caminhoCompletoDoArquivo.toString())
                        .addLong("timestamp", System.currentTimeMillis()) // Evita conflito de execução duplicada
                        .toJobParameters();

                JobExecution execution = jobLauncher.run(processaRemessaJob, jobParameters);
                System.out.println("Job iniciado para arquivo: " + caminhoCompletoDoArquivo);
            } catch (Exception e) {
                System.err.println("Erro ao executar job para arquivo: " + caminhoCompletoDoArquivo);
                System.err.println(e.getMessage());
            }
        }).start(); // Executa em uma thread separada
    }
}

package br.inf.solus.processaArquivos.Thread;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class MonitoraTnumm {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job processaRemessaTnummJob;

    private static final String DiretorioDisponivel = "disponiveis/tnumm";
    private static final String DiretorioProcessado = "processados/tnumm";

    @PostConstruct
    public void iniciarMonitoramento() {
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

        scheduler.scheduleAtFixedRate(() -> {
            File[] arquivos = caminhoDisponiveis.toFile().listFiles(File::isFile);

            for (File arquivo : arquivos != null ? arquivos : new File[0]) {
                String nomeArquivo = arquivo.getName();
                String nomeSemExtensao = nomeArquivo.contains(".")
                        ? nomeArquivo.substring(0, nomeArquivo.lastIndexOf('.'))
                        : nomeArquivo;

                Path destinoFinal = caminhoProcessados.resolve(nomeSemExtensao);

                try {
                    Files.createDirectories(destinoFinal);
                } catch (IOException e) {
                    System.out.println("Erro ao criar a pasta " + destinoFinal + "! " + e.getMessage());
                }

                Path origem = arquivo.toPath();
                Path destino = destinoFinal.resolve(nomeArquivo);

                try {
                    Files.move(origem, destino, StandardCopyOption.REPLACE_EXISTING);
                    iniciarJobAsync(destino);
                    System.out.printf("✔ Arquivo '%s' movido para '%s'%n", nomeArquivo, destino);
                } catch (IOException e) {
                    System.out.println("Erro ao mover o arquivo " + origem + "! " + e.getMessage());
                }
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    private void iniciarJobAsync(Path caminhoCompletoDoArquivo) {
        new Thread(() -> {
            try {
                JobParameters jobParameters = new JobParametersBuilder()
                        .addString("filePath", caminhoCompletoDoArquivo.toString())
                        .addLong("timestamp", System.currentTimeMillis()) // Evita execução duplicada
                        .toJobParameters();

                JobExecution execution = jobLauncher.run(processaRemessaTnummJob, jobParameters);
                System.out.println("Job iniciado para arquivo: " + caminhoCompletoDoArquivo);
            } catch (Exception e) {
                System.err.println("Erro ao executar job para arquivo: " + caminhoCompletoDoArquivo);
                System.err.println(e.getMessage());
            }
        }).start();
    }
}

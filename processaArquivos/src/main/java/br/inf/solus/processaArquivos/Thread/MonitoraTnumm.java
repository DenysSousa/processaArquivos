package br.inf.solus.processaArquivos.Thread;

import jakarta.annotation.PostConstruct;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
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

    // Pool fixo de até 4 threads para processar arquivos
    private final ExecutorService jobExecutor = Executors.newFixedThreadPool(1);

    @PostConstruct
    public void iniciarMonitoramento() {
        String path = System.getenv("ARQUIVOS_DIR");

        if (path == null || path.isBlank()) {
            System.err.println("✖ Erro: Variável de ambiente 'ARQUIVOS_DIR' não está definida.");
            return;
        }

        Path caminhoDisponiveis = Paths.get(path, DiretorioDisponivel);
        Path caminhoProcessados = Paths.get(path, DiretorioProcessado);

        if (!Files.exists(caminhoDisponiveis) || !Files.isDirectory(caminhoDisponiveis)) {
            System.err.println("✖ Erro: Subpasta '" + caminhoDisponiveis + "' não existe ou não é um diretório.");
            return;
        }

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            File[] arquivos = caminhoDisponiveis.toFile().listFiles(File::isFile);

            for (File arquivo : arquivos != null ? arquivos : new File[0]) {

                try {
                    long size1 = arquivo.length();
                    Thread.sleep(2000);
                    long size2 = arquivo.length();

                    if (size1 != size2) {
                        return;
                    }
                } catch (Exception e) {
                    System.out.printf("✖ Erro ao validar arquivo em processo! %s%n", e.getMessage());
                    return;
                }

                String nomeArquivo = arquivo.getName();
                String nomeSemExtensao = nomeArquivo.contains(".")
                        ? nomeArquivo.substring(0, nomeArquivo.lastIndexOf('.'))
                        : nomeArquivo;

                nomeSemExtensao = (nomeSemExtensao.contains("--"))
                        ? nomeSemExtensao.substring(0, nomeSemExtensao.indexOf("--"))
                        : nomeSemExtensao;

                Path destinoFinal = caminhoProcessados.resolve(nomeSemExtensao);

                try {
                    Files.createDirectories(destinoFinal);
                } catch (IOException e) {
                    System.out.println("✖ Erro ao criar a pasta " + destinoFinal + "! " + e.getMessage());
                    return;
                }

                Path origem = arquivo.toPath();
                Path destino = destinoFinal.resolve(nomeArquivo);

                try {
                    Files.move(origem, destino, StandardCopyOption.REPLACE_EXISTING);
                    System.out.printf("♣ Arquivo '%s' movido para '%s'%n", nomeArquivo, destino);
                } catch (IOException e) {
                    System.out.println("✖ Erro ao mover o arquivo " + origem + "! " + e.getMessage());
                    return;
                }

                enviarParaFilaDeExecucao(destino);
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    private void enviarParaFilaDeExecucao(Path caminhoCompletoDoArquivo) {
        jobExecutor.submit(() -> {
            try {
                JobParameters jobParameters = new JobParametersBuilder()
                        .addString("filePath", caminhoCompletoDoArquivo.toString())
                        .addLong("timestamp", System.currentTimeMillis())
                        .toJobParameters();

                System.out.printf("✔ %s Job para %s INICIADO %n",
                        LocalDateTime.now(), caminhoCompletoDoArquivo.getFileName());

                JobExecution execution = jobLauncher.run(processaRemessaTnummJob, jobParameters);

                System.out.printf("✔ %s Job para %s FINALIZADO com status %s%n",
                        LocalDateTime.now(), caminhoCompletoDoArquivo.getFileName(), execution.getStatus());
            } catch (Exception e) {
                System.err.printf("✖ Erro ao executar job para %s: %s%n",
                        caminhoCompletoDoArquivo.getFileName(), e.getMessage());
            }
        });
    }
}

package br.inf.solus.processaArquivos.Thread;

import br.inf.solus.processaArquivos.Utils.FileUtils;
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

    // Pool fixo de até 4 threads para processar arquivos
    private final ExecutorService jobExecutor = Executors.newFixedThreadPool(1);

    @PostConstruct
    public void iniciarMonitoramento() {

        Path caminhoDisponiveis = FileUtils.AvailabePath("tnumm");
        if (caminhoDisponiveis == null) {
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

                Path destino = FileUtils.MoveToBase("processando/tnumm", arquivo.toPath());

                if (destino == null) {
                    return;
                }

                enviarParaFilaDeExecucao(destino);
            }
        }, 0, 5, TimeUnit.SECONDS);
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

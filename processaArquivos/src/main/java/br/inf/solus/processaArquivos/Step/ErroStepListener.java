package br.inf.solus.processaArquivos.Step;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class ErroStepListener implements StepExecutionListener {

    private String filePath;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        filePath = stepExecution
                .getJobExecution()
                .getJobParameters()
                .getString("filePath");
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        if (stepExecution.getExitStatus().equals(ExitStatus.FAILED)) {
            try {
                Path path = Paths.get(filePath);
                File erroFile = path.getParent().resolve("erro.txt").toFile();

                // Formata a data/hora atual
                String timestamp = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(erroFile, true))) {
                    writer.write("[" + timestamp + "]");
                    writer.newLine();
                    writer.write("Erro no step: " + stepExecution.getStepName());
                    writer.newLine();
                    writer.write("Descrição: " + stepExecution.getFailureExceptions().toString());
                    writer.newLine();
                    writer.write("--------------------------------------------------");
                    writer.newLine();
                }
            } catch (Exception e) {
                System.err.printf("Erro ao gravar o erro no step %s%n", e.getMessage());
                e.printStackTrace();
            }
        }
        return stepExecution.getExitStatus();
    }
}

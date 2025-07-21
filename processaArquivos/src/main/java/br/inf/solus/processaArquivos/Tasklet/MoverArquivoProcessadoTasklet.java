package br.inf.solus.processaArquivos.Tasklet;

import br.inf.solus.processaArquivos.Utils.FileUtils;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class MoverArquivoProcessadoTasklet implements Tasklet {

    private void moveArquivo(ChunkContext context) {
        String filePath = context.getStepContext().getJobParameters().get("filePath").toString();
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("Parâmetro 'filePath' não foi informado.");
        }

        Path origem = Paths.get(filePath);

        FileUtils.MoveToBase("processados/" + FileUtils.GetLastFolder(filePath, 1), origem);

    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext context) throws Exception {
        moveArquivo(context);
        return RepeatStatus.FINISHED;

    }
}

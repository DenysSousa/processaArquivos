package br.inf.solus.processaArquivos.Step;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class MoverArquivoProcessadoStep {

    private final Tasklet moverArquivoProcessadoTasklet;
    private final StepListener stepListener;

    public MoverArquivoProcessadoStep(Tasklet moverArquivoProcessadoTasklet, StepListener stepListener) {
        this.moverArquivoProcessadoTasklet = moverArquivoProcessadoTasklet;
        this.stepListener = stepListener;
    }

    @Bean
    public Step stepMoverArquivoProcessado(JobRepository jobRepository,
                                           PlatformTransactionManager transactionManager,
                                           Tasklet moverArquivoProcessadoTasklet,
                                           StepListener stepListener) {
        return new StepBuilder("stepMoverArquivoProcessado", jobRepository)
                .tasklet(moverArquivoProcessadoTasklet, transactionManager)
                .listener(stepListener)
                .build();
    }
}

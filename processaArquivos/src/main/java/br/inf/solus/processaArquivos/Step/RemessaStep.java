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
public class RemessaStep {

    private final Tasklet remessaTasklet;
    private final StepListener stepListener;

    public RemessaStep(Tasklet remessaTasklet, StepListener stepListener) {

        this.remessaTasklet = remessaTasklet;
        this.stepListener = stepListener;
    }

    @Bean
    public Step stepRemessa(JobRepository jobRepository,
                            PlatformTransactionManager transactionManager,
                            Tasklet remessaTasklet,
                            StepListener stepListener) {
        return new StepBuilder("stepRemessa", jobRepository)
                .tasklet(remessaTasklet, transactionManager)
                .listener(stepListener)
                .build();
    }
}

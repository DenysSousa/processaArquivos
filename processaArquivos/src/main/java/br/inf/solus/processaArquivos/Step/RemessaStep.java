package br.inf.solus.processaArquivos.Step;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class RemessaStep {

    private final Tasklet remessaTasklet;

    public RemessaStep(Tasklet remessaTasklet) {
        this.remessaTasklet = remessaTasklet;
    }

    @Bean
    public Step stepRemessa(JobRepository jobRepository,
                            PlatformTransactionManager transactionManager,
                            Tasklet remessaTasklet) {
        return new StepBuilder("stepRemessa", jobRepository)
                .tasklet(remessaTasklet, transactionManager)
                .build();
    }
}

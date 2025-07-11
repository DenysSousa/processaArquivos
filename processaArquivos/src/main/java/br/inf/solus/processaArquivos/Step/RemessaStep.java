package br.inf.solus.processaArquivos.Step;

import br.inf.solus.processaArquivos.Utils.DataUtils;
import br.inf.solus.processaArquivos.Utils.FileUtils;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.config.Task;
import org.springframework.transaction.PlatformTransactionManager;

import java.sql.Date;

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

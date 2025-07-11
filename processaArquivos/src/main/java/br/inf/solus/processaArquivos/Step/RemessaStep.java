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
import org.springframework.transaction.PlatformTransactionManager;

import java.sql.Date;

@Configuration
public class RemessaStep {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Bean
    public Step stepRemessa(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("stepRemessa", jobRepository)
                .tasklet(remessaTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet remessaTasklet() {
        return (contribution, context) -> {
            String filePath = context.getStepContext().getJobParameters().get("filePath").toString();
            String fileName = FileUtils.GetName(filePath, false);

            String sqlSelect = "SELECT ID FROM REMESSA WHERE NOME = ?";
            Long id;

            try {
                id = jdbcTemplate.queryForObject(sqlSelect, Long.class, fileName);
            } catch (EmptyResultDataAccessException e) {
                String sqlInsert = "INSERT INTO REMESSA (NOME, VIGENCIA, TIPO) VALUES (?, ?, ?) RETURNING id";
                Date vigencia = DataUtils.CompToDate(fileName.substring(fileName.length() - 6));
                String tipo = FileUtils.GetLastFolder(filePath);
                id = jdbcTemplate.queryForObject(sqlInsert, Long.class, fileName, vigencia, tipo);
            }

            context.getStepContext().getStepExecution().getJobExecution()
                    .getExecutionContext().put("remessaId", id);
            return RepeatStatus.FINISHED;
        };
    }
}

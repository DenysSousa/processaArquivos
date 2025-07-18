package br.inf.solus.processaArquivos.Tasklet;

import br.inf.solus.processaArquivos.Utils.DataUtils;
import br.inf.solus.processaArquivos.Utils.FileUtils;
import org.apache.catalina.valves.rewrite.InternalRewriteMap;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Date;

@Component
public class RemessaTasklet implements Tasklet {

    private final JdbcTemplate jdbcTemplate;

    public RemessaTasklet(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private void verificaRemessa(ChunkContext context) {
        String filePath = context.getStepContext().getJobParameters().get("filePath").toString();
        String fileName = FileUtils.GetName(filePath, false);
        if (fileName.contains("--")) {
            fileName = fileName.substring(0, fileName.indexOf("--"));
        }

        String sqlSelect = "SELECT ID FROM REMESSA WHERE NOME = ?";
        Long id;

        try {
            id = jdbcTemplate.queryForObject(sqlSelect, Long.class, fileName);
        } catch (EmptyResultDataAccessException e) {
            try {
                String sqlInsert = "INSERT INTO REMESSA (NOME, VIGENCIA, TIPO) VALUES (?, ?, ?) RETURNING id";
                Date vigencia = DataUtils.CompToDate(fileName.substring(fileName.length() - 6));
                String tipo = FileUtils.GetLastFolder(filePath, 1);
                id = jdbcTemplate.queryForObject(sqlInsert, Long.class, fileName, vigencia, tipo.toUpperCase());
            } catch (Exception insertException) {
                try {
                    id = jdbcTemplate.queryForObject(sqlSelect, Long.class, fileName);
                } catch (Exception finalFail) {
                    System.out.printf("Não inseriu o achou o registro do %s - %s%n", fileName, finalFail.getMessage());
                    throw new RuntimeException("Erro ao inserir ou buscar ID da remessa para o arquivo: " +
                            fileName, finalFail);
                }
            }
        }

        context.getStepContext().getStepExecution().getJobExecution().getExecutionContext().put("remessaId", id);
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        verificaRemessa(chunkContext);
        return RepeatStatus.FINISHED;
    }
}

package br.inf.solus.processaArquivos.Tasklet;

import br.inf.solus.processaArquivos.Utils.DataUtils;
import br.inf.solus.processaArquivos.Utils.FileUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
public class RegistraRemessaTasklet implements Tasklet {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Value("#{jobParameters['filePath']}")
    private String filePath;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        String fileName = FileUtils.GetName(filePath, false);

        String sqlSelect = "SELECT ID FROM REMESSA WHERE NOME = :pNome";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("pNome", fileName);

        Long id = 0L;

        try {
            id = jdbcTemplate.queryForObject(sqlSelect, params, Long.class);
        } catch (EmptyResultDataAccessException e) {
            String sqlInsert = "INSERT INTO  REMESSA (NOME, VIGENCIA, TIPO) " +
                    "VALUES (:pNome, :pVigencia, :pTipo) RETURNING id";

            params.addValue("pVigencia", DataUtils.CompToDate(fileName.substring(fileName.length() - 6)));
            params.addValue("pTipo", FileUtils.GetLastFolder(filePath));

            id = jdbcTemplate.queryForObject(sqlInsert, params, Long.class);
        }

        id = (id == null) ? 0 : id;

        // Guarda o ID no ExecutionContext para o próximo step
        chunkContext.getStepContext().getStepExecution()
                .getExecutionContext().putLong("remessaId", id);

        return RepeatStatus.FINISHED;
    }

}

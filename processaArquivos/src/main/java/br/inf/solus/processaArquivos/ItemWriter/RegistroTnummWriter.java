package br.inf.solus.processaArquivos.ItemWriter;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class RegistroTnummWriter implements ItemWriter<String> {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("#{jobExecutionContext['remessaId']}")
    private Long remessaId;

    private int pagina = 1;
    private int contador = 1;


    @Override
    public void write(Chunk<? extends String> chunk) throws Exception {
        String sqlInsert = "INSERT INTO registro (remessa_id, pagina, jsondados) VALUES ";//(?, ?, ?::jsonb)";
        String sqlValues = "";
        for (var json : chunk) {
            json = json.replaceAll("'", "''");

            sqlValues += "(" + remessaId + "," + pagina + ", '" + json + "'::jsonb),";

            if (contador > 0 && contador % 100 == 0) {
                pagina++;
            }
            contador++;
        }

        if (!sqlValues.isEmpty()) {
            sqlValues = sqlValues.substring(0, sqlValues.length() - 1);

            jdbcTemplate.update(sqlInsert + sqlValues);
        }
    }
}

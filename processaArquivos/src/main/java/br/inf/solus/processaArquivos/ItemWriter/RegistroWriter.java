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
public class RegistroWriter implements ItemWriter<String> {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("#{jobExecutionContext['remessaId']}")
    private Long remessaId;

    private int pagina = 1;
    private int contador = 0;


    @Override
    public void write(Chunk<? extends String> chunk) throws Exception {
        String sql = "INSERT INTO registro (remessa_id, pagina, jsondados) VALUES (?, ?, ?::jsonb)";
        for (String json : chunk) {
            if (contador > 0 && contador % 100 == 0) {
                pagina++;
            }
            jdbcTemplate.update(sql, remessaId, pagina, json);
            contador++;
        }
    }
}

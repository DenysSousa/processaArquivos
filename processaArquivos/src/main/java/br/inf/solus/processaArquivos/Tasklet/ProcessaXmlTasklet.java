package br.inf.solus.processaArquivos.Tasklet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.StringWriter;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

@Component
@StepScope
@RequiredArgsConstructor
public class ProcessaXmlTasklet implements Tasklet {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Value("#{jobParameters['filePath']}")
    private String filePath;

    private int pagina;


    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        Long remessaId = chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .getLong("remessaId");

        File xmlFile = new File(filePath);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(xmlFile);

        pagina = 1;
        // Processar blocos <materiais> e <medicamentos>
        processaBloco(document, "materiais", remessaId);
        processaBloco(document, "medicamentos", remessaId);

        return RepeatStatus.FINISHED;
    }

    private void processaBloco(Document doc, String tag, Long remessaId) {
        NodeList blocos = doc.getElementsByTagName(tag);
        for (int i = 0; i < blocos.getLength(); i++) {

            if ((i > 0) && (pagina % 100 == 0)) {
                pagina++;
            }

            Element elem = (Element) blocos.item(i);

            // Converter em JSON
            String json = nodeToJson(elem);

            // Salvar no banco
            String sql = "INSERT INTO registro (remessa_id, pagina, jsondados) " +
                    "VALUES (:remessa_id, :pagina, :jsondados)";

            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("remessa_id", remessaId);
            params.addValue("pagina", pagina);
            params.addValue("jsondados", new SqlParameterValue(Types.OTHER, json));

            jdbcTemplate.update(sql, params);
        }
    }

    private String nodeToJson(Node node) {
        String retorno = "";
        try {

            // 1. Converter o Node para XML string
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(node), new StreamResult(writer));
            String xmlString = writer.toString();

            // 2. Usar XmlMapper para converter XML string em JsonNode
            XmlMapper xmlMapper = new XmlMapper();
            //  xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            JsonNode jsonNode = xmlMapper.readTree(xmlString.getBytes());

            // 3. Converter JsonNode para string JSON
            retorno = new ObjectMapper().writeValueAsString(jsonNode);
        } catch (Exception e) {
            retorno = "{\"Descrição\":\"Não foi possível gerar o json do bloco informado\"," +
                    "\"Detalhes\":\" " + e.getMessage() + "\"}";
        }
        return retorno;
    }

}

package br.inf.solus.processaArquivos.Controller;

import br.inf.solus.processaArquivos.Configuration.DataSourceConf;
import br.inf.solus.processaArquivos.DTO.RetornoDTO;
import br.inf.solus.processaArquivos.Utils.DataUtils;
import br.inf.solus.processaArquivos.Utils.FileUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.configuration.xml.ExceptionElementParser;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

@RestController
public class StartImportacao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private JobLauncher jobLauncher;

    private final Job processaArquivosJob;

    private int pagina;

    public StartImportacao(NamedParameterJdbcTemplate jdbcTemplate, Job processaArquivosJob) {
        this.jdbcTemplate = jdbcTemplate;
        this.processaArquivosJob = processaArquivosJob;
    }

    @PostMapping("/importar")
    public ResponseEntity<RetornoDTO> importaArquivo(@RequestParam(required = false) String filePath) throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        if (filePath == null || filePath.trim().isEmpty())
            return ResponseEntity
                    .badRequest()
                    .body(new RetornoDTO(400, "O parâmetro caminho é obrigatório!"));

        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .addString("filePath", filePath)
                .toJobParameters();

        jobLauncher.run(processaArquivosJob, jobParameters);

        return ResponseEntity
                .ok()
                .body(new RetornoDTO(200, "Job Executado!"));
    }

}

package br.inf.solus.processaArquivos.Step;

import br.inf.solus.processaArquivos.ItemProcessor.RegistroProcessor;
import br.inf.solus.processaArquivos.ItemReader.RegistroReader;
import br.inf.solus.processaArquivos.ItemWriter.RegistroWriter;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.w3c.dom.Node;

@Configuration
public class RegistroEstep {
    @Autowired
    private RegistroReader reader;

    @Autowired
    private RegistroProcessor processor;

    @Autowired
    private RegistroWriter writer;

    @Bean
    public Step stepRegistro(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("stepRegistro", jobRepository)
                .<Node, String>chunk(600, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }
}

package br.inf.solus.processaArquivos.Step;

import br.inf.solus.processaArquivos.ItemProcessor.RegistroTnummProcessor;
import br.inf.solus.processaArquivos.ItemReader.RegistroTnummReader;
import br.inf.solus.processaArquivos.ItemWriter.RegistroTnummWriter;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.w3c.dom.Node;

@Configuration
public class RegistroTnummStep {

    @Autowired
    private RegistroTnummReader reader;

    @Autowired
    private RegistroTnummProcessor processor;

    @Autowired
    private RegistroTnummWriter writer;

    @Autowired
    private StepListener erroStepListener;

    @Bean
    public Step stepRegistroTnumm(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("stepRegistroTnumm", jobRepository)
                .<Node, String>chunk(300, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .listener(erroStepListener)
                .build();
    }
}

package br.inf.solus.processaArquivos.JOB;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
public class ProcessaArquivos {

    @Bean
    public Job processaArquivosJob(JobRepository jobRepository, Step stepRemessa, Step stepRegistro) {
        return new JobBuilder("processRemessaJob", jobRepository)
                .start(stepRemessa)
                .next(stepRegistro)
                .build();
    }

}

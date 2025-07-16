package br.inf.solus.processaArquivos.JOB;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
public class ProcessaRemessaTnummJob {

    @Autowired
    JobExecutionListener jobFinalizadoListener;

    @Bean
    public Job jobProcessaRemessaTnumm(JobRepository jobRepository, Step stepRemessa, Step stepRegistroTnumm) {
        return new JobBuilder("jobProcessaRemessaTnumm", jobRepository)
                .listener(jobFinalizadoListener)
                .start(stepRemessa)
                .next(stepRegistroTnumm)
                .build();
    }

}

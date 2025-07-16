package br.inf.solus.processaArquivos.JOB;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class JobFinalizadoListener implements JobExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(JobFinalizadoListener.class);

    @Override
    public void beforeJob(JobExecution jobExecution) {
        String caminho = jobExecution.getJobParameters().getString("filepath");
        logger.info("🚀 Iniciando job para arquivo: {}", caminho);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String status = jobExecution.getStatus().toString();
        String caminho = jobExecution.getJobParameters().getString("filepath");

        if (ExitStatus.COMPLETED.getExitCode().equals(jobExecution.getExitStatus().getExitCode())) {
            logger.info("✅ Job finalizado com sucesso para arquivo: {}", caminho);
        } else {
            logger.error("❌ Job falhou para o arquivo: {}", caminho);
            jobExecution.getAllFailureExceptions().forEach(ex -> {
                logger.error("Detalhes do erro: {}", ex.getMessage(), ex);
            });
        }
    }
}

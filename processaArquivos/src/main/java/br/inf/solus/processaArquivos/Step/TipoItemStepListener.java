package br.inf.solus.processaArquivos.Step;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

public class TipoItemStepListener implements StepExecutionListener {

    private final String tipo;

    public TipoItemStepListener(String tipo) {
        this.tipo = tipo;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        stepExecution.getExecutionContext().putString("tipo", tipo);
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return ExitStatus.COMPLETED;
    }
}


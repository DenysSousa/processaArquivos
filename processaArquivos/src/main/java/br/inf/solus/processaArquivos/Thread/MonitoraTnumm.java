package br.inf.solus.processaArquivos.Thread;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class MonitoraTnumm {
    public static void main(String[] args) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        Runnable tarefa = () -> {
            try {
                String dataHoraAtual = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
                System.out.println("Data e hora atual: " + dataHoraAtual);
            } catch (Exception e) {
                System.out.println("Erro ao pegar a data! " + e.getMessage());
            }
        };

        // Executa a cada 1 segundo com atraso inicial de 0 segundos
        scheduler.scheduleAtFixedRate(tarefa, 0, 1, TimeUnit.SECONDS);
    }
}

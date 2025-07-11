package br.inf.solus.processaArquivos.Utils;

import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class DataUtils {

    public static Date CompToDate(String competence) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMyyyy");

            YearMonth yearMonth = YearMonth.parse(competence, formatter);
            LocalDate localDate = yearMonth.atDay(1);


            return Date.valueOf(localDate);
        } catch (Exception e) {
            System.out.printf("Erro ao converter a competência em data " + e.getMessage());
            return null;
        }
    }

}

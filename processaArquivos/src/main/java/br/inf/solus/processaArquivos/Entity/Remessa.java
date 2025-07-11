package br.inf.solus.processaArquivos.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.sql.Date;
import java.sql.Timestamp;

@Entity
@Table
@Data
public class Remessa {
    @Id
    private Long id;
    private String nome;
    private Date vigencia;
    private String tipo;
    private Timestamp dataenvio;
}

package br.inf.solus.processaArquivos.Entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table
@Data
public class Registro {

    @Id
    private Long id;

    @ManyToOne
    @JoinColumn(name = "remessa_id")
    private Remessa remessa;

    private Integer pagina;

    private String jsondados;
}

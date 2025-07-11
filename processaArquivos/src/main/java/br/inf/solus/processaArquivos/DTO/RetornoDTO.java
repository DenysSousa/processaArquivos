package br.inf.solus.processaArquivos.DTO;

import lombok.*;

@Getter
@Setter
public class RetornoDTO {

    private int status;
    private String mensagem;

    public RetornoDTO(int status, String mensagem) {
        this.status = status;
        this.mensagem = mensagem;
    }
}

package br.inf.solus.processaArquivos.Mapper;

import br.inf.solus.processaArquivos.Entity.Remessa;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.validation.BindException;

import java.sql.Date;

public class RemessaMapper implements FieldSetMapper<Remessa> {

    @Override
    public Remessa mapFieldSet(FieldSet fieldSet) throws BindException {
        Remessa remessa = new Remessa() ;
        remessa.setNome(fieldSet.readString("nome"));
        remessa.setVigencia(Date.valueOf(fieldSet.readString("vigencia")));
        remessa.setTipo(fieldSet.readString("tipo"));

        return remessa;
    }
}

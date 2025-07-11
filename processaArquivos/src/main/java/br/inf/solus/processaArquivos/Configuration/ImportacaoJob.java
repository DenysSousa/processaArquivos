package br.inf.solus.processaArquivos.Configuration;

import br.inf.solus.processaArquivos.Entity.Remessa;
import br.inf.solus.processaArquivos.Mapper.RemessaMapper;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class ImportacaoJob {

    @Autowired
    private PlatformTransactionManager transactionManager;


    public Job job(Step passoInicial, JobRepository jobRepository) {
        return new JobBuilder("importacao-arquivo", jobRepository)
                .start(passoInicial)
                .incrementer(new RunIdIncrementer())
                .build();
    }


    public Step passoInicial(ItemStreamReader<Remessa> reader, ItemWriter<Remessa> writer, JobRepository jobRepository) {
        return new StepBuilder("passo-inicial", jobRepository)
                .<Remessa, Remessa>chunk(200, transactionManager)
                .reader(reader)
                .writer(writer)
                .build();
    }


    public ItemStreamReader<Remessa> reader(@Value("#{jobParameters['filePath']}") String filePath) {
        return new FlatFileItemReaderBuilder<Remessa>()
                .name("leitura-csv")
                .resource(new FileSystemResource(filePath))
                .comments("--")
                .delimited()
                .delimiter(";")
                .names("nome", "vigencia", "tipo")
                .fieldSetMapper(new RemessaMapper())
                .build();
    }


    public ItemWriter<Remessa> writer(DataSource dataSource) {
        System.out.printf("chegou 2");
        return new JdbcBatchItemWriterBuilder<Remessa>()
                .dataSource(dataSource)
                .sql(
                        "INSERT INTO REMESSA (NOME, VIGENCIA, TIPO) " +
                                "VALUES (:nome, :vigencia, :tipo)"
                ) /*os parâmetros no values, não pode ser snake case, ele faz camelcase*/
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .build();
    }
}

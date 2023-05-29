package com.springbatch.config;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import com.springbatch.dao.Hotels;
import com.springbatch.service.DBLogProcessor;

@Configuration
@EnableBatchProcessing
public class BatchConfiguation {

	@Autowired
	private JobBuilderFactory builderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

	@Value("classPath:/input/hotels.csv")
	private Resource inputResource;

	@Bean
	public DataSource dataSource() {
		EmbeddedDatabaseBuilder embeddedDatabaseBuilder = new EmbeddedDatabaseBuilder();
		return embeddedDatabaseBuilder.addScript("classpath:org/springframework/batch/core/schema-drop-h2.sql")
				.addScript("classpath:org/springframework/batch/core/schema-h2.sql").addScript("classpath:hotels.sql")
				.setType(EmbeddedDatabaseType.H2).build();
	}

	@Bean
	public JdbcBatchItemWriter<Hotels> writer() {
		JdbcBatchItemWriter<Hotels> itemWriter = new JdbcBatchItemWriter<Hotels>();
		itemWriter.setDataSource(dataSource());
		itemWriter.setSql(
				"INSERT INTO HOTELS ( ID,NAME,DESCRIPTION,CITY,RATING) VALUES ( :id, :name, :description, :city, :rating )");
		itemWriter.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<Hotels>());
		return itemWriter;
	}

	@Bean
	public LineMapper<Hotels> lineMapper() {
		DefaultLineMapper<Hotels> lineMapper = new DefaultLineMapper<Hotels>();
		DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
		BeanWrapperFieldSetMapper<Hotels> fieldSetMapper = new BeanWrapperFieldSetMapper<Hotels>();
		lineTokenizer.setNames(new String[] { "id", "name", "description", "city", "rating" });
		lineTokenizer.setIncludedFields(new int[] { 0, 1, 2, 3, 4 });
		fieldSetMapper.setTargetType(Hotels.class);
		lineMapper.setLineTokenizer(lineTokenizer);
		lineMapper.setFieldSetMapper(fieldSetMapper);
		return lineMapper;
	}

	@Bean
	public ItemProcessor<Hotels, Hotels> processor() {
		return new DBLogProcessor();
	}

	@Bean
	public FlatFileItemReader<Hotels> reader() {
		FlatFileItemReader<Hotels> itemReader = new FlatFileItemReader<Hotels>();
		itemReader.setLineMapper(lineMapper());
		itemReader.setLinesToSkip(1);
		itemReader.setResource(inputResource);
		return itemReader;
	}

	@Bean
	public Step step() {
		return stepBuilderFactory.get("step").<Hotels, Hotels>chunk(5).reader(reader()).processor(processor())
				.writer(writer()).build();
	}

	@Bean
	public Job readCSVFileJob() {
		return builderFactory.get("readCSVFileJob").incrementer(new RunIdIncrementer()).start(step()).build();
	}

}
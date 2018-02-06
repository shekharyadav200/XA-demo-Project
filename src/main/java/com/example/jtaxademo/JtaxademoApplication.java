package com.example.jtaxademo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.management.RuntimeErrorException;
import javax.sql.DataSource;
import javax.transaction.Transactional;

import org.h2.jdbcx.JdbcDataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jta.XADataSourceWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class JtaxademoApplication {
private final XADataSourceWrapper wrapper;

	public JtaxademoApplication(XADataSourceWrapper wrapper) {
	super();
	this.wrapper = wrapper;
}
	public static void main(String[] args) {
		SpringApplication.run(JtaxademoApplication.class, args);
	}
	@Bean
	@ConfigurationProperties(prefix="a	")
	DataSource a() throws Exception {
		return this.wrapper.wrapDataSource( dataSource("a"));
	}
	@Bean
	@ConfigurationProperties(prefix="b")
	DataSource b()  throws Exception {
		return this.wrapper.wrapDataSource( dataSource("b"));
	}
	@Bean
	DataSourceInitializer aInit(DataSource a) {
		return init(a,"a");
	}
	@Bean
	DataSourceInitializer bInit(DataSource b) {
		return init(b,"b");
	}
	private DataSourceInitializer init(DataSource a,String name) {
		DataSourceInitializer dataSourceInitializer=new DataSourceInitializer();
		dataSourceInitializer.setDataSource(a);
		dataSourceInitializer.setDatabasePopulator(new ResourceDatabasePopulator(new ClassPathResource(name+".sql")));
		return dataSourceInitializer;
	}
	
	private JdbcDataSource dataSource(String b)	{
		JdbcDataSource jdbcDataSource=new JdbcDataSource();
		jdbcDataSource.setUrl("jdbc:h2:./"+b);
		jdbcDataSource.setUser("sa");
		jdbcDataSource.setPassword("");
		return jdbcDataSource;
	}
	@RestController
	public static class xatest{
		private final JdbcTemplate a,b;
		
		public xatest(DataSource a, DataSource b) {
			
			this.a = new JdbcTemplate(a);
			this.b = new JdbcTemplate(b);
		}
	@GetMapping("/pets")	
public Collection<String> pets(){
	return this.a.query("select * from PET", new RowMapper<String>() {

		@Override
		public String mapRow(ResultSet arg0, int arg1) throws SQLException {
			// TODO Auto-generated method stub
			return arg0.getString("NICKNAME");
		}
	});
}
	@GetMapping("/messages")	
	public Collection<String> messages(){
		return this.b.query("select * from MESSAGE", new RowMapper<String>() {

			@Override
			public String mapRow(ResultSet arg0, int arg1) throws SQLException {
				// TODO Auto-generated method stub
				return arg0.getString("message");
			}
		});
	}
		@PostMapping
		@Transactional
		public void write(@RequestBody Map<String, String> payload,
				@RequestParam Optional<Boolean> roolback) {
			String  name=payload.get("name");
			String messge="Hello"+name+"!";
			this.a.update("insert into PET (id,nickname) values (?,?)",UUID.randomUUID().toString(),name);
			this.b.update("insert into MESSAGE (id,message) values (?,?)",UUID.randomUUID().toString(),messge);
			if(roolback.orElse(false)) {
				throw new RuntimeException("could not write data to the database");
			}

		}
	}
	
}

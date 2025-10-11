package com.neo.ftpserver.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

@Primary
@Configuration
public class DBConfiguration {

    @ConditionalOnProperty(prefix = "spring.datasource", name = "jdbc-url")
    @Bean(name = "dbprimary")
    @ConfigurationProperties("spring.datasource")
    @Primary
    public HikariDataSource primaryDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @ConditionalOnProperty(prefix = "spring.datasource-log", name = "jdbc-url")
    @Bean(name = "db-log")
    @ConfigurationProperties("spring.datasource-log")
    public HikariDataSource logDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(@Qualifier("dbprimary") DataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean(name = "logJdbcTemplate")
    public JdbcTemplate logJdbcTemplate(@Qualifier("db-log") DataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean
    public DataSourceTransactionManager dbLogTransactionManager(@Qualifier("db-log") DataSource dbLogDataSource) {
        return new DataSourceTransactionManager(dbLogDataSource);
    }
}
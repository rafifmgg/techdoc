package com.ocmsintranet.apiservice.crud.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import lombok.extern.slf4j.Slf4j;

import java.util.Properties;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;

/**
 * CAS Database Configuration for OCMS
 * Configures connection to CAS Oracle database for VIP vehicle detection
 * Pattern copied from plusadminapi CasdbConfig.java
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    entityManagerFactoryRef = "casdbEntityManagerFactory",
    transactionManagerRef = "casdbTransactionManager",
    basePackages = {"com.ocmsintranet.apiservice.crud.dao.casdb.repositories"}
)
@Slf4j
public class CasdbConfig {
    String databaseUpdate = "false";

    @Bean
    @ConfigurationProperties(prefix = "spring.casdb.datasource")
    public DataSourceProperties casdbDataSourceProperties() {
        DataSourceProperties source = new DataSourceProperties();
        log.info("CAS Data url: {}", source.getUrl());
        return source;
    }

    @Bean(name = "casdbDataSource")
    @ConfigurationProperties(prefix = "spring.casdb.datasource.hikari")
    public DataSource casdbDataSource(
            @Qualifier("casdbDataSourceProperties") DataSourceProperties dataSourceProperties) {
        HikariDataSource dataSource = dataSourceProperties.initializeDataSourceBuilder()
            .type(HikariDataSource.class)
            .build();

        dataSource.setPoolName("CASDBHikariCP");
        log.info("CAS database connection pool initialized: CASDBHikariCP");
        return dataSource;
    }

    @Bean(name = "casEntityManager")
    public EntityManager casEntityManager(
            @Qualifier("casdbEntityManagerFactory") LocalContainerEntityManagerFactoryBean casdbEntityManagerFactory) {
        EntityManagerFactory emf = casdbEntityManagerFactory.getObject();
        if (emf == null) {
            throw new IllegalStateException("CAS EntityManagerFactory is null");
        }
        return emf.createEntityManager();
    }

    @Bean(name = "casdbEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean casdbEntityManagerFactory(
            @Qualifier("casdbDataSource") DataSource dataSource,
            EntityManagerFactoryBuilder builder) {

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.ocmsintranet.apiservice.crud.dao.casdb.models");
        em.setPersistenceUnitName("casdb");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        em.setJpaProperties(getJpaProperties());
        return em;
    }

    private Properties getJpaProperties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.OracleDialect");
        properties.setProperty("hibernate.enable_lazy_load_no_trans", "true");
        if ("true".equals(databaseUpdate)) {
            properties.setProperty("hibernate.hbm2ddl.auto", "update");
        } else {
            properties.setProperty("hibernate.hbm2ddl.auto", "none");
        }
        return properties;
    }

    @Bean(name = "casdbTransactionManager")
    public PlatformTransactionManager casdbTransactionManager(
            @Qualifier("casdbEntityManagerFactory") EntityManagerFactory factory) {
        return new JpaTransactionManager(factory);
    }
}

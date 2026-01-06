package com.ocmseservice.apiservice.crud.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import com.zaxxer.hikari.HikariDataSource;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@Slf4j
public class MultiDataSourceConfig {

    private DataSource withRetryCapability(HikariDataSource originalDataSource, String name) {
        log.info("Setting up retry capability for {} data source", name);
        return originalDataSource;
    }

    // Helper method to create entity manager factory
    protected LocalContainerEntityManagerFactoryBean createEntityManagerFactory(DataSource dataSource, String packageToScan) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan(packageToScan, packageToScan + ".**");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(false);
        em.setJpaVendorAdapter(vendorAdapter);

        Properties properties = new Properties();
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.SQLServerDialect");
        properties.setProperty("hibernate.format_sql", "false");
        properties.setProperty("hibernate.show_sql", "false");
        properties.setProperty("hibernate.physical_naming_strategy",
                "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");
        em.setJpaProperties(properties);

        return em;
    }

    // Internet Configuration
    @Configuration
    @Primary
    @EnableJpaRepositories(
            basePackages = "com.ocmseservice.apiservice.crud.ocmsezdb",
            entityManagerFactoryRef = "internetEntityManagerFactory",
            transactionManagerRef = "internetTransactionManager"
    )
    public class InternetDbConfig {
        @Bean
        @ConfigurationProperties("spring.datasource.internet")
        public DataSourceProperties internetDataSourceProperties() {
            return new DataSourceProperties();
        }

        @Primary
        @Bean(name = "internetDataSource")
        @ConfigurationProperties("spring.datasource.internet.hikari")
        public DataSource internetDataSource() {
            HikariDataSource dataSource = internetDataSourceProperties()
                    .initializeDataSourceBuilder()
                    .type(HikariDataSource.class)
                    .build();

            dataSource.setConnectionTestQuery("SELECT 1");
            dataSource.setValidationTimeout(5000);

            return withRetryCapability(dataSource, "internet");
        }

        @Primary
        @Bean(name = "internetEntityManagerFactory")
        public LocalContainerEntityManagerFactoryBean internetEntityManagerFactory(
                @Qualifier("internetDataSource") DataSource dataSource) {
            LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
            em.setDataSource(dataSource);
            em.setPackagesToScan(
                    "com.ocmseservice.apiservice.crud.ocmsezdb",
                    "com.ocmseservice.apiservice.crud.ocmsezdb.eocmsusermessage",
                    "com.ocmseservice.apiservice.crud.ocmsezdb.eocmspaymentmatrix",
                    "com.ocmseservice.apiservice.crud.ocmsezdb.eocmsvalidoffencenotice",
                    "com.ocmseservice.apiservice.crud.ocmsezdb.eocmswebtxnaudit",
                    "com.ocmseservice.apiservice.crud.ocmsezdb.eocmswebtxndetail",
                    "com.ocmseservice.apiservice.crud.ocmsezdb.eocmstemplatestore",
                    "com.ocmseservice.apiservice.crud.ocmsezdb.eocmsdrivernotice",
                    "com.ocmseservice.apiservice.crud.ocmsezdb.eocmsrequestdriverparticulars"
            );

            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            vendorAdapter.setGenerateDdl(false);
            em.setJpaVendorAdapter(vendorAdapter);

            Properties properties = new Properties();
            properties.setProperty("hibernate.dialect", "org.hibernate.dialect.SQLServerDialect");
            properties.setProperty("hibernate.format_sql", "false");
            properties.setProperty("hibernate.show_sql", "false");
            properties.setProperty("hibernate.physical_naming_strategy",
                    "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");
            em.setJpaProperties(properties);

            return em;
        }

        @Primary
        @Bean(name = "internetTransactionManager")
        public PlatformTransactionManager internetTransactionManager(
                @Qualifier("internetEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
            return new JpaTransactionManager(entityManagerFactory.getObject());
        }
    }

    // PII Configuration
    @Configuration
    @EnableJpaRepositories(
        basePackages = "com.ocmseservice.apiservice.crud.ocmspii",
        entityManagerFactoryRef = "piiEntityManagerFactory",
        transactionManagerRef = "piiTransactionManager"
    )
    public class PiiDbConfig {
        @Bean
        @ConfigurationProperties("spring.datasource.pii")
        public DataSourceProperties piiDataSourceProperties() {
            return new DataSourceProperties();
        }

        @Bean(name = "piiDataSource")
        @ConfigurationProperties("spring.datasource.pii.hikari")
        public DataSource piiDataSource() {
            HikariDataSource dataSource = piiDataSourceProperties()
                    .initializeDataSourceBuilder()
                    .type(HikariDataSource.class)
                    .build();

            dataSource.setConnectionTestQuery("SELECT 1");
            dataSource.setValidationTimeout(5000);

            return withRetryCapability(dataSource, "pii");
        }

        @Bean(name = "piiEntityManagerFactory")
        public LocalContainerEntityManagerFactoryBean piiEntityManagerFactory(
                @Qualifier("piiDataSource") DataSource dataSource) {
            LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
            em.setDataSource(dataSource);
            em.setPackagesToScan(
                    "com.ocmseservice.apiservice.crud.ocmspii",
                    "com.ocmseservice.apiservice.crud.ocmspii.eocmsoffencenoticeownerdriver"
            );

            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            vendorAdapter.setGenerateDdl(false);
            em.setJpaVendorAdapter(vendorAdapter);

            Properties properties = new Properties();
            properties.setProperty("hibernate.dialect", "org.hibernate.dialect.SQLServerDialect");
            properties.setProperty("hibernate.format_sql", "false");
            properties.setProperty("hibernate.show_sql", "false");
            properties.setProperty("hibernate.physical_naming_strategy",
                    "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");
            em.setJpaProperties(properties);

            return em;
        }

        @Bean(name = "piiTransactionManager")
        public PlatformTransactionManager piiTransactionManager(
                @Qualifier("piiEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
            return new JpaTransactionManager(entityManagerFactory.getObject());
        }
    }

    // REPCCS Configuration
//    @Configuration
//    @EnableJpaRepositories(
//            basePackages = {"com.ocmseservice.apiservice.crud.repccsdb", "com.ocmseservice.apiservice.crud.cascomizdb"},
//            entityManagerFactoryRef = "repccsEntityManagerFactory",
//            transactionManagerRef = "repccsTransactionManager"
//    )
//    public class RepCcsDbConfig {
//        @Bean
//        @ConfigurationProperties("spring.datasource.repccs")
//        public DataSourceProperties repccsDataSourceProperties() {
//            return new DataSourceProperties();
//        }
//
//        @Bean(name = "repccsDataSource")
//        @ConfigurationProperties("spring.datasource.repccs.hikari")
//        public DataSource repccsDataSource() {
//            HikariDataSource dataSource = repccsDataSourceProperties()
//                    .initializeDataSourceBuilder()
//                    .type(HikariDataSource.class)
//                    .build();
//
//            dataSource.setConnectionTestQuery("SELECT 1");
//            dataSource.setValidationTimeout(5000);
//
//            return withRetryCapability(dataSource, "repccs");
//        }
//
//        @Bean(name = "repccsEntityManagerFactory")
//        public LocalContainerEntityManagerFactoryBean repccsEntityManagerFactory(
//                @Qualifier("repccsDataSource") DataSource dataSource) {
//            LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
//            em.setDataSource(dataSource);
//            em.setPackagesToScan(
//                    "com.ocmseservice.apiservice.crud.repccsdb",
//                    "com.ocmseservice.apiservice.crud.cascomizdb"
//            );
//
//            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
//            vendorAdapter.setGenerateDdl(false);
//            em.setJpaVendorAdapter(vendorAdapter);
//
//            Properties properties = new Properties();
//            properties.setProperty("hibernate.dialect", "org.hibernate.dialect.SQLServerDialect");
//            properties.setProperty("hibernate.format_sql", "false");
//            properties.setProperty("hibernate.show_sql", "false");
//            properties.setProperty("hibernate.physical_naming_strategy",
//                    "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");
//            em.setJpaProperties(properties);
//
//            return em;
//        }
//
//        @Bean(name = "repccsTransactionManager")
//        public PlatformTransactionManager repccsTransactionManager(
//                @Qualifier("repccsEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
//            return new JpaTransactionManager(entityManagerFactory.getObject());
//        }
//    }
}
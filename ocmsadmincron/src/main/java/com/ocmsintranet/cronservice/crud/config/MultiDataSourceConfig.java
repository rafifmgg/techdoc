package com.ocmsintranet.cronservice.crud.config;

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

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class MultiDataSourceConfig {

    private DataSource withRetryCapability(HikariDataSource originalDataSource, String name) {
        System.out.println("Setting up retry capability for " + name + " data source");
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

    // Intranet Configuration
    @Configuration
    @EnableJpaRepositories(
        basePackages = "com.ocmsintranet.cronservice.crud.ocmsizdb",
        entityManagerFactoryRef = "intranetEntityManagerFactory",
        transactionManagerRef = "intranetTransactionManager"
    )
    public class IntranetDbConfig {
        @Bean
        @ConfigurationProperties("spring.datasource.intranet")
        public DataSourceProperties intranetDataSourceProperties() {
            return new DataSourceProperties();
        }

        @Primary
        @Bean(name = "intranetDataSource")
        @ConfigurationProperties("spring.datasource.intranet.hikari")
        public DataSource intranetDataSource() {
            HikariDataSource dataSource = intranetDataSourceProperties()
                    .initializeDataSourceBuilder()
                    .type(HikariDataSource.class)
                    .build();
            
            dataSource.setConnectionTestQuery("SELECT 1");
            dataSource.setValidationTimeout(5000);
            
            return withRetryCapability(dataSource, "intranet");
        }

        @Primary
        @Bean(name = {"intranetEntityManagerFactory", "entityManagerFactory"})
        public LocalContainerEntityManagerFactoryBean intranetEntityManagerFactory(
                @Qualifier("intranetDataSource") DataSource dataSource) {
            return createEntityManagerFactory(dataSource, "com.ocmsintranet.cronservice.crud.ocmsizdb");
        }

        @Primary
        @Bean(name = {"intranetTransactionManager", "transactionManager"})
        public PlatformTransactionManager intranetTransactionManager(
                @Qualifier("intranetEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
            return new JpaTransactionManager(entityManagerFactory.getObject());
        }
    }

    // EZ Configuration
    @Configuration
    @EnableJpaRepositories(
        basePackages = "com.ocmsintranet.cronservice.crud.ocmsezdb",
        entityManagerFactoryRef = "ezEntityManagerFactory",
        transactionManagerRef = "ezTransactionManager"
    )
    public class EzDbConfig {
        @Bean
        @ConfigurationProperties("spring.datasource.ez")
        public DataSourceProperties ezDataSourceProperties() {
            return new DataSourceProperties();
        }

        @Bean(name = "ezDataSource")
        @ConfigurationProperties("spring.datasource.ez.hikari")
        public DataSource ezDataSource() {
            HikariDataSource dataSource = ezDataSourceProperties()
                    .initializeDataSourceBuilder()
                    .type(HikariDataSource.class)
                    .build();
            
            dataSource.setConnectionTestQuery("SELECT 1");
            dataSource.setValidationTimeout(5000);
            
            return withRetryCapability(dataSource, "ez");
        }

        @Bean(name = "ezEntityManagerFactory")
        public LocalContainerEntityManagerFactoryBean ezEntityManagerFactory(
                @Qualifier("ezDataSource") DataSource dataSource) {
            LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
            em.setDataSource(dataSource);
            em.setPackagesToScan(
                "com.ocmsintranet.cronservice.crud.ocmsezdb"
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

        @Bean(name = "ezTransactionManager")
        public PlatformTransactionManager ezTransactionManager(
                @Qualifier("ezEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
            return new JpaTransactionManager(entityManagerFactory.getObject());
        }
    }

    // PII Configuration
    @Configuration
    @EnableJpaRepositories(
        basePackages = "com.ocmsintranet.cronservice.crud.ocmspiiezdb",
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
            LocalContainerEntityManagerFactoryBean em = createEntityManagerFactory(dataSource, "com.ocmsintranet.cronservice.crud.ocmspiiezdb");
            em.setPersistenceUnitName("pii");
            return em;
        }

        @Bean(name = "piiTransactionManager")
        public PlatformTransactionManager piiTransactionManager(
                @Qualifier("piiEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
            return new JpaTransactionManager(entityManagerFactory.getObject());
        }
    }

    // // REPCCS Configuration
    // @Configuration
    // @EnableJpaRepositories(
    //     basePackages = "com.ocmsintranet.cronservice.crud.repccsdb",
    //     entityManagerFactoryRef = "repccsEntityManagerFactory",
    //     transactionManagerRef = "repccsTransactionManager" 
    // )
    // public class RepCcsDbConfig {
    //     @Bean
    //     @ConfigurationProperties("spring.datasource.repccs")
    //     public DataSourceProperties repccsDataSourceProperties() {
    //         return new DataSourceProperties();
    //     }

    //     @Bean(name = "repccsDataSource")
    //     @ConfigurationProperties("spring.datasource.repccs.hikari")
    //     public DataSource repccsDataSource() {
    //         HikariDataSource dataSource = repccsDataSourceProperties()
    //                 .initializeDataSourceBuilder()
    //                 .type(HikariDataSource.class)
    //                 .build();
            
    //         dataSource.setConnectionTestQuery("SELECT 1");
    //         dataSource.setValidationTimeout(5000);
            
    //         return withRetryCapability(dataSource, "repccs");
    //     }

    //     @Bean(name = "repccsEntityManagerFactory")
    //     public LocalContainerEntityManagerFactoryBean repccsEntityManagerFactory(
    //             @Qualifier("repccsDataSource") DataSource dataSource) {
    //         return createEntityManagerFactory(dataSource, "com.ocmsintranet.cronservice.crud.repccsdb");
    //     }

    //     @Bean(name = "repccsTransactionManager")
    //     public PlatformTransactionManager repccsTransactionManager(
    //             @Qualifier("repccsEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
    //         return new JpaTransactionManager(entityManagerFactory.getObject());
    //     }
    // }
}
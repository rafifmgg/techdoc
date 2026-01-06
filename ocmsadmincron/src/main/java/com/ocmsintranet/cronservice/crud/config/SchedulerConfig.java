package com.ocmsintranet.cronservice.crud.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;
import java.util.TimeZone;

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m") // Default lock timeout
public class SchedulerConfig {

    /**
     * Configure ShedLock to use the database table for distributed locking
     */
    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .withTableName("ocmsizmgr.ocms_shedlock") // Match your schema and table name
                        .withTimeZone(TimeZone.getTimeZone("UTC"))
                        .build()
        );
    }
}
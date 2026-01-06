package com.ocmsintranet.cronservice.crud.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
@EnableRetry
public class RetryConfig {
    
    @Bean(name = "dataSourceRetryTemplate")
    public RetryTemplate dataSourceRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // Set backoff policy - wait longer between retry attempts
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000); // 1 second
        backOffPolicy.setMultiplier(2.0); // double the wait time for each retry
        backOffPolicy.setMaxInterval(30000); // max 30 seconds wait
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        // Set retry policy - retry 5 times on specific exceptions
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(5);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        return retryTemplate;
    }
}
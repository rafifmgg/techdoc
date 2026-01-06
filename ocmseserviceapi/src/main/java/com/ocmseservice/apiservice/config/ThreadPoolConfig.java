package com.ocmseservice.apiservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Configuration for thread pools used in the application
 */
@Configuration
public class ThreadPoolConfig {

    @Value("${app.threadpool.size:5}")
    private int threadPoolSize;

    /**
     * Creates an executor service for asynchronous tasks like email sending
     */
    @Bean
    public ExecutorService executorService() {
        return Executors.newFixedThreadPool(threadPoolSize);
    }
}

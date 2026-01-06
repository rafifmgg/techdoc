package com.ocmsintranet.cronservice.config;

import com.samskivert.mustache.Mustache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

/**
 * Configuration for Mustache templating engine
 */
@Configuration
public class MustacheConfig {

    private final ResourceLoader resourceLoader;

    public MustacheConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * Creates a Mustache compiler bean that loads templates from the classpath
     * @return Mustache compiler instance
     */
    @Bean
    public Mustache.Compiler mustacheCompiler() {
        return Mustache.compiler()
                .withLoader(name -> {
                    try {
                        return new java.io.InputStreamReader(
                            resourceLoader.getResource("classpath:templates/" + name).getInputStream(),
                            java.nio.charset.StandardCharsets.UTF_8
                        );
                    } catch (Exception e) {
                        return null;
                    }
                })
                .defaultValue("")
                .escapeHTML(false);  // Don't escape HTML by default to allow HTML in error messages
    }
}

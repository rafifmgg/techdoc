package com.ocmsintranet.cronservice.dummyintegrations.emailsmtpserver;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Lazy;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import java.util.Properties;

/**
 * Brevo SMTP configuration for SIT environment
 */
@Configuration
@Profile({"sit", "local", "dev"})
@Lazy // Make this configuration lazy-loaded
@Slf4j
public class BrevoConfigSit {
    @PostConstruct
    public void init() {
        log.info("==========================================");
        log.info("BREVO SMTP SERVICE INITIALIZED FOR SIT");
        log.info("Using real Brevo SMTP service in SIT environment");
        log.info("==========================================");
    }

    @Bean
    @Primary
    @Lazy // Make this bean lazy-loaded
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp-relay.brevo.com");
        mailSender.setPort(587);
        mailSender.setUsername("8c3e7f001@smtp-brevo.com");
        mailSender.setPassword("bO8FKfA26DG4N9md");
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "false"); // Disable mail debug
        
        return mailSender;
    }
}

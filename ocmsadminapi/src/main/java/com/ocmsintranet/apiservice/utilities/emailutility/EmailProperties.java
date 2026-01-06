package com.ocmsintranet.apiservice.utilities.emailutility;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "email")
public class EmailProperties {
    private String from;
    private boolean enableSmtp;
    
    // Getters and setters
    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    public boolean isEnableSmtp() { return enableSmtp; }
    public void setEnableSmtp(boolean enableSmtp) { this.enableSmtp = enableSmtp; }
}

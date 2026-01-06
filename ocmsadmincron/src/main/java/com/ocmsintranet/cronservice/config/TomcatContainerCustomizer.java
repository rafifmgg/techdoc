package com.ocmsintranet.cronservice.config;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Customizes the Tomcat container to handle client abort exceptions more gracefully.
 */
@Configuration
public class TomcatContainerCustomizer {

    /**
     * Customizes the Tomcat server factory to add connector customizers.
     * This helps suppress broken pipe exceptions by setting the appropriate
     * properties on the Tomcat connector.
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return (factory) -> {
            // Add a connector customizer to handle client abort exceptions
            factory.addConnectorCustomizers(new TomcatConnectorCustomizer() {
                @Override
                public void customize(Connector connector) {
                    // Get the protocol handler
                    ProtocolHandler protocolHandler = connector.getProtocolHandler();
                    
                    // Set properties to handle client abort exceptions
                    if (protocolHandler instanceof AbstractHttp11Protocol) {
                        AbstractHttp11Protocol<?> http11Protocol = (AbstractHttp11Protocol<?>) protocolHandler;
                        
                        // Set the connection timeout to a lower value to reduce hanging connections
                        http11Protocol.setConnectionTimeout(20000);
                        
                        // Reduce the socket timeout to handle disconnects faster
                        http11Protocol.setKeepAliveTimeout(5000);
                        
                        // Reduce the max keep-alive requests to minimize connection issues
                        http11Protocol.setMaxKeepAliveRequests(1);
                    }
                }
            });
        };
    }
}

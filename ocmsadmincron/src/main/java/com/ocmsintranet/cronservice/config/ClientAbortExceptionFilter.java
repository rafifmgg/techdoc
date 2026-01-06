package com.ocmsintranet.cronservice.config;

import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filter to catch and suppress ClientAbortException that occurs when a client
 * disconnects before the server finishes sending the response.
 */
@Component
public class ClientAbortExceptionFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(ClientAbortExceptionFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } catch (ClientAbortException e) {
            // Silently ignore this exception - it's a normal occurrence when clients disconnect
            // Only log at trace level for debugging purposes if needed
            if (logger.isTraceEnabled()) {
                logger.trace("Client disconnected prematurely", e);
            }
            
            // Mark the response as complete to prevent further processing
            if (response instanceof HttpServletResponse) {
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                if (!httpResponse.isCommitted()) {
                    httpResponse.setStatus(HttpServletResponse.SC_OK);
                    httpResponse.getOutputStream().close();
                }
            }
        }
    }

    @Override
    public void init(FilterConfig filterConfig) {
        // No initialization needed
    }

    @Override
    public void destroy() {
        // No cleanup needed
    }
}

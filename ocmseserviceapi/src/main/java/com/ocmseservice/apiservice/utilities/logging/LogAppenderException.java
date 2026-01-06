package com.ocmseservice.apiservice.utilities.logging;

import java.io.IOException;

public class LogAppenderException extends RuntimeException {
    public LogAppenderException(String message) {
        super(message);
    }

    public LogAppenderException(String message, Throwable cause) {
        super(message, cause);
    }

    public LogAppenderException(IOException e) {
        super(e);
    }
}
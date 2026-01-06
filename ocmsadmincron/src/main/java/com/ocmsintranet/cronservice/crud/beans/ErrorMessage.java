package com.ocmsintranet.cronservice.crud.beans;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ErrorMessage {
    private ErrorData data;

    @Getter
    @Setter
    @AllArgsConstructor
    public static class ErrorData {
        private String appCode;
        private String message;
    }
}
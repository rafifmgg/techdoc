package com.ocmsintranet.cronservice.crud.beans;

public class ResponseMessage {
    private Data data;

    public ResponseMessage(String appCode, String message) {
        this.data = new Data(appCode, message);
    }

    public Data getData() {
        return data;
    }

    public static class Data {
        private String appCode;
        private String message;

        public Data(String appCode, String message) {
            this.appCode = appCode;
            this.message = message;
        }

        public String getAppCode() {
            return appCode;
        }

        public String getMessage() {
            return message;
        }

    }
}

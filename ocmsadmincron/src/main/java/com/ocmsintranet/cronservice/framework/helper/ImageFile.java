package com.ocmsintranet.cronservice.framework.helper;

public interface ImageFile {

    String getFileName();
    void setFileName(String fileName);
    byte[] getFileContent();
    void setFileContent(byte[] fileContent);
    String getMimeType();
    void setMimeType(String mimeType);
    String getNopoNumber();
    void setNopoNumber(String nopoNumber);
}

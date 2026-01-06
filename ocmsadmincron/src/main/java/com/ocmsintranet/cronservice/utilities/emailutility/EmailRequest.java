package com.ocmsintranet.cronservice.utilities.emailutility;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {
    private String from;
    private String to;
    private String subject;
    private String htmlContent;
    private String cc;
    private List<Attachment> attachments;
    
    @Data
    public static class Attachment {
        private String fileName;
        private byte[] fileContent;
    }
}

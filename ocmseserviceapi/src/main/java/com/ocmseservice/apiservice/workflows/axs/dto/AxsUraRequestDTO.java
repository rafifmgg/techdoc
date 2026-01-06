package com.ocmseservice.apiservice.workflows.axs.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AxsUraRequestDTO {

    private String sender;
    private String targetReceiver;
    private String dateSent;
    private String timeSent;
    private String signature;
    private String transactionID;
    private String searchField;
    private String searchValue;

    @Override
    public String toString() {
        return "AxsUraRequestDTO{" +
                "sender='" + sender + '\'' +
                ", targetReceiver='" + targetReceiver + '\'' +
                ", dateSent='" + dateSent + '\'' +
                ", timeSent='" + timeSent + '\'' +
                ", signature='" + signature + '\'' +
                ", transactionID='" + transactionID + '\'' +
                ", searchField='" + searchField + '\'' +
                ", searchValue='" + searchValue + '\'' +
                '}';
    }
}

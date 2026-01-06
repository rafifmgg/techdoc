package com.ocmseservice.apiservice.workflows.axs.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class AxsResponseDTO {
    private String sender;
    private String targetReceiver;
    private String dateSent;
    private String timeSent;
    private String signature;
    private String transactionID;
    private String searchField;
    private String searchValue;
    private String status;
    private String errorMsg;
    private List<AxsParkingFinesDTO> ponDetails;

}
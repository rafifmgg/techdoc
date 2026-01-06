package com.ocmsintranet.apiservice.workflows.notice_creation.core.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RepWebHookPayloadDto {

    @JsonProperty("TransactionID")
    private String transactionId;

    @JsonProperty("SubSystemLabel")
    private String subSystemLabel;

    @JsonProperty("EnforcementCarParkID")
    private String enforcementCarParkId;

    @JsonProperty("EnforcementCarParkName")
    private String enforcementCarParkName;

    @JsonProperty("OperatorID")
    private String operatorId;

    @JsonProperty("CaptureDateTime")
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime captureDateTime;

    @JsonProperty("CreatedDateTime")
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime createdDateTime;

    @JsonProperty("ParkingEntryDateTime")
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime parkingEntryDateTime;

    @JsonProperty("ParkingStartDateTime")
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime parkingStartDateTime;

    @JsonProperty("ParkingEndDateTime")
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime parkingEndDateTime;

    @JsonProperty("ChargeAmount")
    private String chargeAmount;

    @JsonProperty("ViolationCode")
    private String violationCode;

    @JsonProperty("OBULabel")
    private String obuLabel;

    @JsonProperty("OBULatitude")
    private String obuLatitude;

    @JsonProperty("OBULongitude")
    private String obuLongitude;

    @JsonProperty("VehicleRegistration")
    private String vehicleRegistration;

    @JsonProperty("VehicleCategory")
    private String vehicleCategory;

    @JsonProperty("LicencePlate")
    private String licencePlate;

    @JsonProperty("OffenceCode")
    private Integer offenceCode;

    @JsonProperty("NOPONumber")
    private String nopoNumber;

    @JsonProperty("ANSFlag")
    private String ansFlag;

    @JsonProperty("FineAmount")
    private String fineAmount;

    @JsonProperty("ERP2CCSRemarks")
    private String erp2ccsRemarks;

    @JsonProperty("ImageID")
    private String imageId;

    @JsonProperty("VideoID")
    private String videoId;

    @JsonProperty("REPCCSRemarks")
    private String repRemarks;

}

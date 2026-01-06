package com.ocmseservice.apiservice.workflows.spcp.model;

import java.util.List;
import lombok.Data;

/**
 * Response model for MyInfo data
 */
@Data
public class MyInfoResponse {
    private MobileNo mobileNo;
    private RegisteredAddress registeredAddress;
    private List<Vehicle> vehicles;
    private String responseCode;
    private String responseMsg;
    private String name;
    private String email;
    
    @Data
    public static class MobileNo {
        private String prefix;
        private String countryCode;
        private String number;
    }
    
    @Data
    public static class RegisteredAddress {
        private String block;
        private String building;
        private String floor;
        private String unitNo;
        private String street;
        private String postalCode;
        private String country;
        private String countryCode;
    }
    
    @Data
    public static class Vehicle {
        private String vehicleNumber;
        private String chassisNumber;
        private String statusCode;
        private String status;
        private String iuLabelNumber;
        private String engineNumber;
        private String motorNumber;
        private String type;
        private String make;
        private String model;
        private String scheme;
        private String coeCategory;
        private String primaryColor;
        private String secondaryColor;
        private String attachment1;
        private String attachment2;
        private String attachment3;
        private String propellant;
        private Integer engineCapacity;
        private Integer length;
        private Integer width;
        private Integer height;
        private Integer maxUnladenWeight;
        private Integer maxLadenWeight;
        private Integer numberOfTransfer;
        private String yearOfManufacture;
        private Integer co2Emission;
        private Double coEmission;
        private Double noxEmission;
        private Double thcEmission;
        private Double pmEmission;
        private Integer powerRate;
        private Integer minParfBenefit;
        private Integer prevailingQuotaPremium;
        private Integer openMarketValue;
        private String firstRegistrationDate;
        private String originalRegistrationDate;
        private String temporaryTransferStartDate;
        private String temporaryTransferEndDate;
        private String roadTaxExpiryDate;
        private String coeExpiryDate;
        private String ownershipEffectiveDatetime;
    }
}

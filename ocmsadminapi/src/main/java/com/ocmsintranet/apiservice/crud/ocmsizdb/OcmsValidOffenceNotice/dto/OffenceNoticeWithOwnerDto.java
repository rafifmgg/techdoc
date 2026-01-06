package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.dto;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNotice;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFilter;

@JsonFilter("OffenceNoticeWithOwner.fields")
public class OffenceNoticeWithOwnerDto {
    // Notice fields
    private String noticeNo;
    private BigDecimal compositionAmount;
    private Integer computerRuleCode;
    private LocalDateTime dueDateOfRevival;
    private LocalDateTime eprDateOfSuspension;
    private String eprReasonOfSuspension;
    private LocalDateTime lastProcessingDate;
    private String lastProcessingStage;
    private LocalDateTime nextProcessingDate;
    private String nextProcessingStage;
    private LocalDateTime noticeDateAndTime;
    private String offenceNoticeType;
    private String parkingLotNo;
    private String ppCode;
    private String ppName;
    private LocalDateTime prevProcessingDate;
    private String prevProcessingStage;
    private String suspensionType;
    private String vehicleCategory;
    private String vehicleNo;
    private String vehicleRegistrationType;
    private String subsystemLabel;
    private String wardenNo;
    private BigDecimal repChargeAmount;
    private BigDecimal administrationFee;
    private BigDecimal amountPayable;
    private LocalDateTime crsDateOfSuspension;
    private String crsReasonOfSuspension;
    private LocalDateTime creDate;
    private String creUserId;
    private LocalDateTime updDate;
    private String updUserId;
    private String otherRemark;
    private String anFlag;
    private BigDecimal amountPaid;
    private String paymentStatus;
    private LocalDateTime paymentDueDate;
    private String paymentAcceptanceAllowed;

    // Owner/driver fields
    private String idType;
    private String idNo; // Changed from nricNo to match entity field name
    private String offenderIndicator;
    private String name;
    private String ownerDriverIndicator;

    public OffenceNoticeWithOwnerDto() {
    }
    
    // Create a constructor that takes an OcmsValidOffenceNotice and copies all fields
    public OffenceNoticeWithOwnerDto(OcmsValidOffenceNotice notice) {
        if (notice != null) {
            this.noticeNo = notice.getNoticeNo();
            this.compositionAmount = notice.getCompositionAmount();
            this.computerRuleCode = notice.getComputerRuleCode();
            this.dueDateOfRevival = notice.getDueDateOfRevival();
            this.eprDateOfSuspension = notice.getEprDateOfSuspension();
            this.eprReasonOfSuspension = notice.getEprReasonOfSuspension();
            this.lastProcessingDate = notice.getLastProcessingDate();
            this.lastProcessingStage = notice.getLastProcessingStage();
            this.nextProcessingDate = notice.getNextProcessingDate();
            this.nextProcessingStage = notice.getNextProcessingStage();
            this.noticeDateAndTime = notice.getNoticeDateAndTime();
            this.offenceNoticeType = notice.getOffenceNoticeType();
            this.parkingLotNo = notice.getParkingLotNo();
            this.ppCode = notice.getPpCode();
            this.ppName = notice.getPpName();
            this.prevProcessingDate = notice.getPrevProcessingDate();
            this.prevProcessingStage = notice.getPrevProcessingStage();
            this.suspensionType = notice.getSuspensionType();
            this.vehicleCategory = notice.getVehicleCategory();
            this.vehicleNo = notice.getVehicleNo();
            this.vehicleRegistrationType = notice.getVehicleRegistrationType();
            this.subsystemLabel = notice.getSubsystemLabel();
            this.wardenNo = notice.getWardenNo();
            this.repChargeAmount = notice.getRepChargeAmount();
            this.administrationFee = notice.getAdministrationFee();
            this.amountPayable = notice.getAmountPayable();
            this.crsDateOfSuspension = notice.getCrsDateOfSuspension();
            this.crsReasonOfSuspension = notice.getCrsReasonOfSuspension();
            this.otherRemark = notice.getOtherRemark();
            this.anFlag = notice.getAnFlag();
            this.amountPaid = notice.getAmountPaid();
            this.paymentStatus = notice.getPaymentStatus();
            this.paymentDueDate = notice.getPaymentDueDate();
            this.creDate = notice.getCreDate();
            this.creUserId = notice.getCreUserId();
            this.updDate = notice.getUpdDate();
            this.updUserId = notice.getUpdUserId();
            this.paymentAcceptanceAllowed =  notice.getPaymentAcceptanceAllowed();
        }
    }

    // Getters and setters for all fields
    public String getNoticeNo() {
        return noticeNo;
    }

    public void setNoticeNo(String noticeNo) {
        this.noticeNo = noticeNo;
    }

    public BigDecimal getCompositionAmount() {
        return compositionAmount;
    }

    public void setCompositionAmount(BigDecimal compositionAmount) {
        this.compositionAmount = compositionAmount;
    }

    public Integer getComputerRuleCode() {
        return computerRuleCode;
    }

    public void setComputerRuleCode(Integer computerRuleCode) {
        this.computerRuleCode = computerRuleCode;
    }

    public LocalDateTime getDueDateOfRevival() {
        return dueDateOfRevival;
    }

    public void setDueDateOfRevival(LocalDateTime dueDateOfRevival) {
        this.dueDateOfRevival = dueDateOfRevival;
    }

    public LocalDateTime getEprDateOfSuspension() {
        return eprDateOfSuspension;
    }

    public void setEprDateOfSuspension(LocalDateTime eprDateOfSuspension) {
        this.eprDateOfSuspension = eprDateOfSuspension;
    }

    public String getEprReasonOfSuspension() {
        return eprReasonOfSuspension;
    }

    public void setEprReasonOfSuspension(String eprReasonOfSuspension) {
        this.eprReasonOfSuspension = eprReasonOfSuspension;
    }

    public LocalDateTime getLastProcessingDate() {
        return lastProcessingDate;
    }

    public void setLastProcessingDate(LocalDateTime lastProcessingDate) {
        this.lastProcessingDate = lastProcessingDate;
    }

    public String getLastProcessingStage() {
        return lastProcessingStage;
    }

    public void setLastProcessingStage(String lastProcessingStage) {
        this.lastProcessingStage = lastProcessingStage;
    }

    public LocalDateTime getNextProcessingDate() {
        return nextProcessingDate;
    }

    public void setNextProcessingDate(LocalDateTime nextProcessingDate) {
        this.nextProcessingDate = nextProcessingDate;
    }

    public String getNextProcessingStage() {
        return nextProcessingStage;
    }

    public void setNextProcessingStage(String nextProcessingStage) {
        this.nextProcessingStage = nextProcessingStage;
    }

    public LocalDateTime getNoticeDateAndTime() {
        return noticeDateAndTime;
    }

    public void setNoticeDateAndTime(LocalDateTime noticeDateAndTime) {
        this.noticeDateAndTime = noticeDateAndTime;
    }

    public String getOffenceNoticeType() {
        return offenceNoticeType;
    }

    public void setOffenceNoticeType(String offenceNoticeType) {
        this.offenceNoticeType = offenceNoticeType;
    }

    public String getParkingLotNo() {
        return parkingLotNo;
    }

    public void setParkingLotNo(String parkingLotNo) {
        this.parkingLotNo = parkingLotNo;
    }

    public String getPpCode() {
        return ppCode;
    }

    public void setPpCode(String ppCode) {
        this.ppCode = ppCode;
    }

    public String getPpName() {
        return ppName;
    }

    public void setPpName(String ppName) {
        this.ppName = ppName;
    }

    public LocalDateTime getPrevProcessingDate() {
        return prevProcessingDate;
    }

    public void setPrevProcessingDate(LocalDateTime prevProcessingDate) {
        this.prevProcessingDate = prevProcessingDate;
    }

    public String getPrevProcessingStage() {
        return prevProcessingStage;
    }

    public void setPrevProcessingStage(String prevProcessingStage) {
        this.prevProcessingStage = prevProcessingStage;
    }

    public String getSuspensionType() {
        return suspensionType;
    }

    public void setSuspensionType(String suspensionType) {
        this.suspensionType = suspensionType;
    }

    public String getVehicleCategory() {
        return vehicleCategory;
    }

    public void setVehicleCategory(String vehicleCategory) {
        this.vehicleCategory = vehicleCategory;
    }

    public String getVehicleNo() {
        return vehicleNo;
    }

    public void setVehicleNo(String vehicleNo) {
        this.vehicleNo = vehicleNo;
    }

    public String getVehicleRegistrationType() {
        return vehicleRegistrationType;
    }

    public void setVehicleRegistrationType(String vehicleRegistrationType) {
        this.vehicleRegistrationType = vehicleRegistrationType;
    }

    public String getSubsystemLabel() {
        return subsystemLabel;
    }

    public void setSubsystemLabel(String subsystemLabel) {
        this.subsystemLabel = subsystemLabel;
    }

    public String getWardenNo() {
        return wardenNo;
    }

    public void setWardenNo(String wardenNo) {
        this.wardenNo = wardenNo;
    }

    public BigDecimal getRepChargeAmount() {
        return repChargeAmount;
    }

    public void setRepChargeAmount(BigDecimal repChargeAmount) {
        this.repChargeAmount = repChargeAmount;
    }

    public BigDecimal getAdministrationFee() {
        return administrationFee;
    }

    public void setAdministrationFee(BigDecimal administrationFee) {
        this.administrationFee = administrationFee;
    }

    public BigDecimal getAmountPayable() {
        return amountPayable;
    }

    public void setAmountPayable(BigDecimal amountPayable) {
        this.amountPayable = amountPayable;
    }

    public BigDecimal getAmountPaid() {
        return amountPayable;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public LocalDateTime getCreDate() {
        return creDate;
    }

    public void setCreDate(LocalDateTime creDate) {
        this.creDate = creDate;
    }

    public String getCreUserId() {
        return creUserId;
    }

    public void setCreUserId(String creUserId) {
        this.creUserId = creUserId;
    }

    public LocalDateTime getUpdDate() {
        return updDate;
    }

    public void setUpdDate(LocalDateTime updDate) {
        this.updDate = updDate;
    }

    public String getUpdUserId() {
        return updUserId;
    }

    public void setUpdUserId(String updUserId) {
        this.updUserId = updUserId;
    }

    public LocalDateTime getCrsDateOfSuspension() {
        return crsDateOfSuspension;
    }

    public void setCrsDateOfSuspension(LocalDateTime crsDateOfSuspension) {
        this.crsDateOfSuspension = crsDateOfSuspension;
    }

    public String getCrsReasonOfSuspension() {
        return crsReasonOfSuspension;
    }

    public void setCrsReasonOfSuspension(String crsReasonOfSuspension) {
        this.crsReasonOfSuspension = crsReasonOfSuspension;
    }

    public String getIdType() {
        return idType;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }

    public String getIdNo() {
        return idNo;
    }

    public void setIdNo(String idNo) {
        this.idNo = idNo;
    }
    
    // Keeping backward compatibility with old code
    public String getNricNo() {
        return idNo;
    }

    public void setNricNo(String nricNo) {
        this.idNo = nricNo;
    }

    public String getOffenderIndicator() {
        return offenderIndicator;
    }

    public void setOffenderIndicator(String offenderIndicator) {
        this.offenderIndicator = offenderIndicator;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwnerDriverIndicator() {
        return ownerDriverIndicator;
    }

    public void setOwnerDriverIndicator(String ownerDriverIndicator) {
        this.ownerDriverIndicator = ownerDriverIndicator;
    }

    public String getOtherRemark() {
        return otherRemark;
    }

    public void setOtherRemark(String otherRemark) {
        this.otherRemark = otherRemark;
    }

    public String getAnFlag() {
        return anFlag;
    }

    public void setAnFlag(String anFlag) {
        this.anFlag = anFlag;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public LocalDateTime getPaymentDueDate() {
        return paymentDueDate;
    }

    public void setPaymentDueDate(LocalDateTime paymentDueDate) {
        this.paymentDueDate = paymentDueDate;
    }

    public String getPaymentAcceptanceAllowed() {
        return paymentAcceptanceAllowed;
    }

    public void setPaymentAcceptanceAllowed(String paymentAcceptanceAllowed) {
        this.paymentAcceptanceAllowed = paymentAcceptanceAllowed;
    }

}

package com.ocmsintranet.apiservice.workflows.notice_creation.core.helpers;

import com.ocmsintranet.apiservice.crud.beans.SystemConstant;
import com.ocmsintranet.apiservice.crud.cascomizdb.Icarpark.IcarparkRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsWebTransactionAudit.OcmsWebTransactionAudit;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsWebTransactionAudit.OcmsWebTransactionAuditRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.offencerulecode.OffenceRuleCodeRepository;
import com.ocmsintranet.apiservice.workflows.notice_creation.core.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@Slf4j
public class ValidationServices {

    @Autowired
    private OcmsValidOffenceNoticeRepository ocmsValidOffenceNoticeRepository;

    @Autowired
    private OffenceRuleCodeRepository offenceRuleCodeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OcmsWebTransactionAuditRepository ocmsWebTransactionAuditRepository;

    @Autowired
    private IcarparkRepository icarpark;


    /**
     * Helper method to extract values from string using a prefix
     */
    private String extractValue(String source, String prefix) {
        if (source == null || prefix == null) {
            return null;
        }

        int startIndex = source.indexOf(prefix);
        if (startIndex == -1) {
            return null;
        }

        startIndex += prefix.length();
        int endIndex = source.indexOf(",", startIndex);

        if (endIndex == -1) {
            // If no comma, look for <End> tag or just take the rest
            endIndex = source.indexOf("<End>", startIndex);
            if (endIndex == -1) {
                return source.substring(startIndex).trim();
            }
        }

        return source.substring(startIndex, endIndex).trim();
    }

    /**
     * Check if a notice with the given notice number already exists in the system
     *
     * @param noticeNo The notice number to check
     * @return true if the notice already exists, false otherwise or in case of error
     */
    public boolean checkNoticeExisting(String noticeNo) {
        try {
            if (noticeNo == null || noticeNo.trim().isEmpty()) {
                log.warn("Cannot check for duplicate notice: Notice Number is null or empty");
                return false;
            }

            log.info("Checking if notice with Notice Number '{}' already exists", noticeNo);
            // Use repository method to find existing notice
            boolean exists = ocmsValidOffenceNoticeRepository.existsById(noticeNo);

            if (exists) {
                log.warn("Duplicate notice found with Notice Number: {}", noticeNo);
                return true;
            }

            log.info("No existing notice found with Notice Number: {}", noticeNo);
            return false;
        } catch (Exception e) {
            log.error("Error checking for existing notice: {}", e.getMessage(), e);
            return false;
        }
    }



    //New function to check and validate Offence type
    public CesCreateNoticeDto validateToDto(CesCreateNoticeDto eht) {
        CesCreateNoticeDto dto = new CesCreateNoticeDto();

        dto.setSubsystemLabel(eht.getSubsystemLabel());
        dto.setAnFlag(eht.getAnFlag());
        dto.setIuNo(eht.getIuNo());
        dto.setRepObuLatitude(eht.getRepObuLatitude());
        dto.setRepObuLongitude(eht.getRepObuLongitude());
        dto.setRepOperatorId(eht.getRepOperatorId());
        dto.setRepViolationCode(eht.getRepViolationCode());
        dto.setPpName(eht.getPpName());
        dto.setCompositionAmount(eht.getCompositionAmount());

        dto.setWardenNo(eht.getWardenNo());
        dto.setVehicleMake(eht.getVehicleMake());
        dto.setColorOfVehicle(eht.getColorOfVehicle());
        // Fine / Charge amount mapping
        Object amountObj = eht.getRepChargeAmount();
        BigDecimal chargeAmount;

        if (amountObj == null) {
            chargeAmount = BigDecimal.ZERO;
        } else if (amountObj instanceof BigDecimal) {
            chargeAmount = (BigDecimal) amountObj;
        } else {
            try {
                chargeAmount = new BigDecimal(amountObj.toString().trim());
            } catch (NumberFormatException ex) {
                log.error("Invalid ChargeAmount format: '{}' for transaction: {}, defaulting to 0.00",
                        amountObj, eht.getTransactionId(), ex);
                chargeAmount = BigDecimal.ZERO;
            }
        }

        dto.setRepChargeAmount(chargeAmount);
        if (eht.getVehicleCategory() != null && !eht.getVehicleCategory().isEmpty()) {
            String firstChar = String.valueOf(eht.getVehicleCategory().charAt(0));
            dto.setVehicleCategory(firstChar);
            //log.info("Check If Vehicle Car: " + firstChar);
        }

        // Offence code mapping
//        dto.setOffenceNoticeType(eht.getOffenceNoticeType());
//        Integer computerRuleCode = eht.getComputerRuleCode();
//        if (computerRuleCode == null) {
//            log.error("computerRuleCode is missing for transaction: {}", eht.getTransactionId());
//            throw new IllegalArgumentException("offenceCode Not Exist");
//        }
//        dto.setComputerRuleCode(computerRuleCode);
//
//        Optional<String> offenceTypeOptional = offenceRuleCodeRepository
//                .findOffenceTypeByRuleCodeAndVehicleCategory(
//                        computerRuleCode,
//                        eht.getVehicleCategory(),
//                        LocalDateTime.now());
//        offenceTypeOptional.ifPresent(dto::setOffenceNoticeType);
//        if (offenceTypeOptional.isEmpty()) {
//            log.warn("No offence type found for ruleCode={}, vehicleCategory={} at {}",
//                    computerRuleCode, eht.getVehicleCategory(), LocalDateTime.now());
//            throw new IllegalArgumentException("offenceCode Not Exist");
//        }

        dto.setOffenceNoticeType(eht.getOffenceNoticeType());
        Integer computerRuleCode = eht.getComputerRuleCode();

        if (computerRuleCode == null) {
            log.error("computerRuleCode is missing for transaction: {}", eht.getTransactionId());
            throw new ValidationException("Invalid Offence Rule Code");
        }

        dto.setComputerRuleCode(computerRuleCode);

        Optional<String> offenceTypeOptional = offenceRuleCodeRepository
                .findOffenceTypeByRuleCodeAndVehicleCategory(
                        computerRuleCode,
                        eht.getVehicleCategory(),
                        LocalDateTime.now()
                );

        if (offenceTypeOptional.isEmpty()) {
            log.warn("No offence type found for ruleCode={}, vehicleCategory={} at {}",
                    computerRuleCode, eht.getVehicleCategory(), LocalDateTime.now());
            throw new ValidationException("Invalid Offence Rule Code");
        }

        dto.setOffenceNoticeType(offenceTypeOptional.get());

        dto.setCreUserId(SystemConstant.User.DEFAULT_SYSTEM_USER_ID);

        dto.setPpCode(eht.getPpCode());

        // Parse remarks if exist
        dto.setRuleRemark1(eht.getRuleRemark1());
        dto.setRuleRemark2(eht.getRuleRemark2());
        //dto.setSysNonPrintedComment(eht.getSysNonPrintedComment());
        dto.setSysNonPrintedComment(eht.getSysNonPrintedComment());
        dto.setUserNonPrintedComment(eht.getUserNonPrintedComment());
        dto.setParkingLotNo(eht.getParkingLotNo());

        // Vehicle & notice info
        dto.setVehicleNo(eht.getVehicleNo());
        dto.setCreDate(String.valueOf(eht.getCreDate()));
        dto.setVehicleRegistrationType(eht.getVehicleRegistrationType());
        dto.setNoticeNo(eht.getNoticeNo());
        dto.setTransactionId(eht.getTransactionId());


        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        DateTimeFormatter apiDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        dto.setRepParkingEntryDt(eht.getRepParkingEntryDt());
        dto.setRepParkingStartDt(eht.getRepParkingStartDt());
        dto.setRepParkingEndDt(eht.getRepParkingEndDt());
        dto.setNoticeDateAndTime(eht.getNoticeDateAndTime());
        dto.setCreDate(eht.getCreDate());
        //dto.setOtherRemark(eht.getOtherRemark());


        // Map createdDt field
        dto.setCreatedDt(eht.getCreatedDt());
        log.info("Mapped createdDt: {} for transaction: {}", eht.getCreatedDt(), eht.getTransactionId());

        // Vehicle category cleanup



        return dto;
    }
    public class ValidationException extends RuntimeException {
        public ValidationException(String message) { super(message); }
    }

    public RepCreateNoticeDto mapToDto(RepWebHookPayloadDto eht) {


        RepCreateNoticeDto dto = new RepCreateNoticeDto();

        String fineAmountStr = eht.getFineAmount();
        if (fineAmountStr == null || fineAmountStr.trim().isEmpty()) {
            log.warn("FineAmount is null or empty for transaction: {}, defaulting to 0.00", eht.getFineAmount());
            dto.setCompositionAmount(BigDecimal.ZERO);
        } else {
            try {
                BigDecimal fineAmount = new BigDecimal(fineAmountStr.trim());
                dto.setCompositionAmount(fineAmount);
            } catch (NumberFormatException ex) {
                log.error("Invalid FineAmount format: '{}' for transaction: {}, defaulting to 0.00", fineAmountStr, eht.getFineAmount(), ex);
                dto.setCompositionAmount(BigDecimal.ZERO);
            }
        }
        dto.setSubsystemLabel(eht.getSubSystemLabel());
        dto.setAnFlag(eht.getAnsFlag());
        dto.setIuNo(eht.getObuLabel());
        dto.setRepObuLatitude(eht.getObuLatitude());
        dto.setRepObuLongitude(eht.getObuLongitude());
        dto.setRepOperatorId(eht.getOperatorId());
        dto.setRepViolationCode(eht.getViolationCode());
        String chargeAmountStr = eht.getChargeAmount();
        if (chargeAmountStr == null || chargeAmountStr.trim().isEmpty()) {
            log.warn("FineAmount is null or empty for transaction: {}, defaulting to 0.00", eht.getChargeAmount());
            // dto.setRepChargeAmount(BigDecimal.ZERO);
        } else {
            try {
                BigDecimal chargeAmount = new BigDecimal(fineAmountStr.trim());
                dto.setRepChargeAmount(chargeAmount);
            } catch (NumberFormatException ex) {
                log.error("Invalid FineAmount format: '{}' for transaction: {}, defaulting to 0.00", fineAmountStr, eht.getFineAmount(), ex);
                //dto.setRepChargeAmount(BigDecimal.ZERO);
            }
        }

        dto.setComputerRuleCode(eht.getOffenceCode());
        dto.setCreUserId(eht.getOperatorId());
        dto.setNoticeDateAndTime(eht.getCaptureDateTime() != null ? eht.getCaptureDateTime().toString() : null);
        //dto.setOffenceNoticeType("O");
        //dto.setPpName(eht.getEnforcementCarParkName());
        dto.setPpCode(eht.getEnforcementCarParkId());
        // dto.setComments(eht.getOperatorRemarks());
        // Parse OperatorRemarks string
        if (eht.getErp2ccsRemarks() != null) {
            String remarks = eht.getErp2ccsRemarks();

            // Extract Rule Remark 1
            dto.setRuleRemark1(extractValue(remarks, "<Rule Remarks1>:"));

            // Extract Rule Remark 2
            dto.setRuleRemark2(extractValue(remarks, "<Rule Remarks2>:"));

            // Extract Non-printed Comments
            dto.setUserNonPrintedComment(extractValue(remarks, "<Non-printed Comments>:"));
            // LOT NUMBER
            dto.setParkingLotNo(extractValue(remarks, "<Lot No.>:"));
        }

        dto.setCreatedDt(eht.getCreatedDateTime() != null ? eht.getCreatedDateTime().toString() : null);
//        String fineAmountStr = String.valueOf(eht.getFineAmount());
//        BigDecimal fineAmount = new BigDecimal(fineAmountStr);
//        dto.setCompositionAmount(fineAmount);


        dto.setVehicleNo(eht.getLicencePlate());
        dto.setCreDate(String.valueOf(eht.getCreatedDateTime()));
        dto.setVehicleRegistrationType(eht.getVehicleRegistration());
        dto.setNoticeNo(eht.getNopoNumber());
        dto.setTransactionId(eht.getTransactionId());
        DateTimeFormatter apiDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        dto.setRepParkingEntryDt(
                eht.getParkingEntryDateTime() != null ? eht.getParkingEntryDateTime().format(apiDateTimeFormatter)
                        : null);
        dto.setRepParkingStartDt(
                eht.getParkingStartDateTime() != null ? eht.getParkingStartDateTime().format(apiDateTimeFormatter)
                        : null);
        dto.setRepParkingEndDt(
                eht.getParkingEndDateTime() != null ? eht.getParkingEndDateTime().format(apiDateTimeFormatter) : null);
       // dto.setOtherRemark(eht.getRepRemarks());
        dto.setSysNonPrintedComment(eht.getRepRemarks());
        dto.setImageId(eht.getImageId());
        dto.setVideoId(eht.getVideoId());

        String vehicleCategory = null;
        if (eht.getVehicleCategory() != null) {
            String[] parts = eht.getVehicleCategory().split("[–-]");
            vehicleCategory = parts[0].trim();
            dto.setVehicleCategory(vehicleCategory);
        }
        try {
            Integer computerRuleCode = Integer.valueOf(eht.getOffenceCode());
            LocalDateTime now = LocalDateTime.now();

            Optional<String> offenceTypeOptional = offenceRuleCodeRepository
                    .findOffenceTypeByRuleCodeAndVehicleCategory(computerRuleCode, vehicleCategory, now);

            offenceTypeOptional.ifPresent(dto::setOffenceNoticeType);
        } catch (NumberFormatException e) {
            log.error("Invalid OffenceCode format: {}", eht.getOffenceCode(), e);
        }

        try {
            String carParkid = eht.getEnforcementCarParkId();
            Optional<String> carparkNameOptional = icarpark.findCarParkName(carParkid);
            log.info("check value carpark "+ carParkid + "Value name "+carparkNameOptional);
            carparkNameOptional.ifPresent(dto::setPpName);
        } catch (NumberFormatException e) {
            log.error("Invalid OffenceCode format: {}", eht.getOffenceCode(), e);
        }

        return dto;
    }

    public void saveAudit(RepWebHookPayloadDto payloadSent, String WebTxnid) {
        try {

            OcmsWebTransactionAudit audit = new OcmsWebTransactionAudit();

            audit.setWebTxnId(WebTxnid);
            audit.setSender(SystemConstant.Rep.SENDER);
            audit.setTargetReceiver(SystemConstant.Rep.RRECEIVER);
            //audit.setMsgError();
            audit.setRecordCounter(1);
            audit.setSendDate(LocalDate.now());
            audit.setSendTime(LocalTime.now());
            audit.setStatusNum("0");
            try {
                String jsonPayload = objectMapper.writeValueAsString(payloadSent);
                audit.setTxnDetail(jsonPayload);
            } catch (Exception e) {
                e.printStackTrace();
                audit.setTxnDetail("Error serializing payload");
            }
            audit.setCreDate(LocalDateTime.now());
            audit.setCreUserId(SystemConstant.User.DEFAULT_SYSTEM_USER_ID);
            // Format serviceCode
            audit.setServiceCode(SystemConstant.Rep.SERVICE_CODE);

            ocmsWebTransactionAuditRepository.save(audit);

            log.debug("Saved audit log for transaction ID: {}", payloadSent.getTransactionId());
        } catch (Exception e) {
            log.error("Failed to save audit log for transaction ID: {}", payloadSent.getTransactionId(), e);
        }
    }

    public void updateAuditError(String webTxnId, String step, Throwable t) {
        System.out.println("value of txnid"+ webTxnId);
        if (webTxnId == null) return;

        try {
            Optional<OcmsWebTransactionAudit> auditOpt = ocmsWebTransactionAuditRepository.findById(webTxnId);
            if (!auditOpt.isPresent()) return;

            OcmsWebTransactionAudit audit = auditOpt.get();

            String errMsg = (t != null && t.getMessage() != null) ? t.getMessage() : "Unknown error";
            if (errMsg.length() > 1900) {
                errMsg = errMsg.substring(0, 1900) + "...";
            }

            String newMsg = String.format("%s: %s", step, errMsg);

            log.info("Check Position Process: {}", newMsg);

            audit.setMsgError(newMsg);
            audit.setStatusNum("1");
            audit.setUpdDate(LocalDateTime.now());

            ocmsWebTransactionAuditRepository.save(audit); // Use the instance, not the class

            log.info("Audit msgError updated for webTxnId={} with step={}", webTxnId, step);
        } catch (Exception ex) {
            log.error("Failed to update audit error for webTxnId={}", webTxnId, ex);
        }
    }

}

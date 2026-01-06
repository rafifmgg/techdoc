package com.ocmseservice.apiservice.utilities.axs;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmseservice.apiservice.crud.ocmsezdb.eocmswebtxnaudit.EocmsWebTxnAudit;
import com.ocmseservice.apiservice.workflows.axs.dto.AxsUraRequestDTO;
import com.ocmseservice.apiservice.workflows.axs.dto.PaymentUpdateDTO;
import com.ocmseservice.apiservice.workflows.axs.dto.TransactionDTO;

import com.ocmseservice.apiservice.crud.ocmsezdb.eocmswebtxndetail.EocmsWebTxnDetail;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class AxsMapperUtil {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter PAYMENT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter PAYMENT_TIME_FORMAT = DateTimeFormatter.ofPattern("HHmmss");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static EocmsWebTxnAudit mapDtoToEntity(AxsUraRequestDTO dto) {
        EocmsWebTxnAudit entity = new EocmsWebTxnAudit();

        entity.setSender(dto.getSender());
        entity.setTargetReceiver(dto.getTargetReceiver());

        LocalDate date = LocalDate.parse(dto.getDateSent(), PAYMENT_DATE_FORMAT);
        LocalTime time = LocalTime.parse(dto.getTimeSent(), PAYMENT_TIME_FORMAT);

        entity.setSendDate(LocalDateTime.of(date, time));
        entity.setSendTime(time);

        entity.setWebTxnId(dto.getTransactionID());
        entity.setRecordCounter(1);
        entity.setStatusNum("0");
        entity.setMsgError(null);

        // Serialize DTO ke JSON string untuk txnDetail
        try {
            String jsonTxnDetail = objectMapper.writeValueAsString(dto);
            entity.setTxnDetail(jsonTxnDetail);
        } catch (JsonProcessingException e) {
            // Tangani error serialisasi sesuai kebutuhan
            e.printStackTrace();
            entity.setTxnDetail("{}"); // fallback JSON kosong
        }

        return entity;
    }

    public static EocmsWebTxnAudit mapPaymentUpdateDtoToEntity(PaymentUpdateDTO dto) {
        EocmsWebTxnAudit entity = new EocmsWebTxnAudit();

        entity.setSender(dto.getSender());
        entity.setTargetReceiver(dto.getTargetReceiver());
        entity.setWebTxnId(dto.getTransactionID());
        entity.setRecordCounter(Integer.parseInt(dto.getRecordCounter()));
        entity.setStatusNum("1"); // Default status for payment updates

        // Parse date and time from strings using payment-specific formats
        LocalDate date = LocalDate.parse(dto.getDateSent(), PAYMENT_DATE_FORMAT);
        LocalTime time = LocalTime.parse(dto.getTimeSent(), PAYMENT_TIME_FORMAT);
        LocalDateTime sendDateTime = LocalDateTime.of(date, time);
        LocalTime sendTime = time;
        
        entity.setSendDate(sendDateTime);
        entity.setSendTime(sendTime);

        // Serialize PaymentUpdateDTO to JSON string for txnDetail
        try {
            String jsonTxnDetail = objectMapper.writeValueAsString(dto);
            entity.setTxnDetail(jsonTxnDetail);
        } catch (JsonProcessingException e) {
            // Handle serialization error
            e.printStackTrace();
            entity.setTxnDetail("{}"); // fallback empty JSON
        }

        return entity;
    }

    public static EocmsWebTxnDetail mapTransactionDtoToDetail(TransactionDTO transaction, PaymentUpdateDTO paymentUpdateDTO) {
        EocmsWebTxnDetail detailEntity = new EocmsWebTxnDetail();
        
        // Map TransactionDTO fields to EocmsWebTxnDetail
        detailEntity.setReceiptNo(transaction.getReceiptNo());
        detailEntity.setTypeOfReceipt(transaction.getTypeOfReceipt());
        
        // Merge transactionDate and transactionTime to LocalDateTime using payment-specific formats
        LocalDate transactionDate = LocalDate.parse(transaction.getTransactionDate(), PAYMENT_DATE_FORMAT);
        LocalTime transactionTime = LocalTime.parse(transaction.getTransactionTime(), PAYMENT_TIME_FORMAT);
        detailEntity.setTransactionDateAndTime(LocalDateTime.of(transactionDate, transactionTime));
        
        detailEntity.setOffenceNoticeNo(transaction.getNoticeNo());
        detailEntity.setVehicleNo(transaction.getVehicleNo());
        detailEntity.setAtomsFlag(transaction.getAtomsFlag());
        detailEntity.setPaymentMode(transaction.getPaymentMode());
        detailEntity.setPaymentAmount(transaction.getPaymentAmount());
        detailEntity.setRemarks(transaction.getRemarks());
        
        // Set additional required fields from PaymentUpdateDTO
        detailEntity.setSender(paymentUpdateDTO.getSender());
        detailEntity.setStatus("S"); // Success status
        detailEntity.setIsSync(false); // Not synced initially
        detailEntity.setMerchantRefNo(paymentUpdateDTO.getTransactionID());
        
        return detailEntity;
    }
}
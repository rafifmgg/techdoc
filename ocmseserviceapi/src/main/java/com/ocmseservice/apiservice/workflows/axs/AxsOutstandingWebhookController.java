package com.ocmseservice.apiservice.workflows.axs;

import com.ocmseservice.apiservice.crud.beans.CrudResponse;
import com.ocmseservice.apiservice.crud.beans.ResponseMessage;
import com.ocmseservice.apiservice.crud.ocmsezdb.eocmswebtxnaudit.EocmsWebTxnAudit;
import com.ocmseservice.apiservice.crud.ocmsezdb.eocmswebtxnaudit.EocmsWebTxnAuditService;
import com.ocmseservice.apiservice.crud.ocmsezdb.eocmswebtxndetail.EocmsWebTxnDetail;
import com.ocmseservice.apiservice.crud.ocmsezdb.eocmswebtxndetail.EocmsWebTxnDetailService;
import com.ocmseservice.apiservice.utilities.TimeFormat;
import com.ocmseservice.apiservice.utilities.axs.AxsMapperUtil;
import com.ocmseservice.apiservice.utilities.axs.AxsSearchNoticesUtils;
import com.ocmseservice.apiservice.workflows.searchnoticetopay.dto.SearchParkingNoticeDTO;
import com.ocmseservice.apiservice.workflows.axs.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import java.util.stream.Collectors;

import static com.ocmseservice.apiservice.crud.beans.CrudResponse.AppCodes.SUCCESS;
import static com.ocmseservice.apiservice.utilities.ErrorMessageFormatter.formatErrorMessage;

@Slf4j
@RestController
@RequestMapping("v1")
public class AxsOutstandingWebhookController {


    @Autowired
    private AxsSearchNoticesUtils axsSearchNoticesUtils;
    
    @Autowired
    private EocmsWebTxnAuditService eocmsWebTxnAuditService;
    
    @Autowired
    private EocmsWebTxnDetailService eocmsWebTxnDetailService;

    @PostMapping("/axs-parkingfines")
    public ResponseEntity<?> findParkingFines(@RequestBody AxsUraRequestDTO axsUraRequestDTO) {

        // Audit logging - create and save transaction audit
        EocmsWebTxnAudit auditEntity = AxsMapperUtil.mapDtoToEntity(axsUraRequestDTO);

        try {
            eocmsWebTxnAuditService.save(auditEntity);
            log.info("Audit record saved for transaction ID: {}", axsUraRequestDTO.getTransactionID());

            log.info("---PARKING FINES CONTROLLER----"+ " " + axsUraRequestDTO);
            int limit = 10;
            int skip = 0;

            // Check which parameter is provided
            if (axsUraRequestDTO.getSearchField().equals("NN")) {
                String noticeNumber = axsUraRequestDTO.getSearchValue();

                if (noticeNumber == null || noticeNumber.isEmpty()) {
                    throw new IllegalArgumentException("noticeNo is required and cannot be empty");
                }

                SearchParkingNoticeDTO searchResult2 = axsSearchNoticesUtils.checkNotices(noticeNumber);

                if(searchResult2.getVehicleNo() ==null){
                    String formattedMessage = formatErrorMessage(searchResult2, searchResult2.getErrorMessage());
                    ResponseMessage responseMessage = new ResponseMessage(SUCCESS,formattedMessage);
                    return new ResponseEntity<>(responseMessage, HttpStatus.OK);
                }

                AxsParkingFinesDTO dto = new AxsParkingFinesDTO();
                String errorMessage = searchResult2.getErrorMessage();

                dto.setPlaceOfOffence(searchResult2.getCarparkName());
                dto.setNoticeNo(searchResult2.getNoticeNo());
                dto.setVehicleNo(searchResult2.getVehicleNo());
                TimeFormat td = new TimeFormat(searchResult2.getNoticeDateAndTime());
                dto.setOffenceDate(td.getFormattedDate());
                TimeFormat tf = new TimeFormat(searchResult2.getNoticeDateAndTime());
                dto.setOffenceTime(tf.getFormattedTime());
                dto.setAmountPayable(searchResult2.getAmountPayable());
                dto.setProcessingStage(searchResult2.getLastProcessingStage());
                dto.setShowPON(searchResult2.getShow());
                dto.setPaymentAllowedFlag(searchResult2.getNoticePaymentFlag());
                if (errorMessage != null) {
                    // Format current date as MM/dd/yyyy
                    String formattedMessage = formatErrorMessage(searchResult2, searchResult2.getErrorMessage());
                    dto.setDisplayMsg(formattedMessage);
                }
                dto.setAtomsFlag("N");

//                List<AxsParkingFinesDTO> dtoList = List.of(dto);
//                int total = dtoList.size();
//                SingleResponse<List<AxsParkingFinesDTO>> response = SingleResponse.createPaginatedResponse(dtoList, total, limit, skip);

                AxsResponseDTO finals = new AxsResponseDTO();
                finals.setSender("URA");
                finals.setTargetReceiver("AXS");
                TimeFormat oD = new TimeFormat(LocalDateTime.now());
                finals.setDateSent(oD.getFormattedDate());
                TimeFormat oF = new TimeFormat(LocalDateTime.now());
                finals.setTimeSent(oF.getFormattedTime());
                finals.setSignature(axsUraRequestDTO.getSignature());
                finals.setTransactionID(axsUraRequestDTO.getTransactionID());
                //int limit = (limitVal instanceof Number) ? ((Number) limitVal).intValue() : 10;
                finals.setSearchField(
                        "VN".equalsIgnoreCase(axsUraRequestDTO.getSearchField()) ? "Vehicle" :
                                "NN".equalsIgnoreCase(axsUraRequestDTO.getSearchField()) ? "Notice Number" :
                                        axsUraRequestDTO.getSearchField()
                );
                finals.setSearchValue(axsUraRequestDTO.getSearchValue());
                finals.setStatus(SUCCESS);
                finals.setErrorMsg(null);
                finals.setPonDetails(Collections.singletonList(dto));
                return new ResponseEntity<>(finals, HttpStatus.OK);
            }


            else if (axsUraRequestDTO.getSearchField().equals("VN")) {

                String vehicleNo = axsUraRequestDTO.getSearchValue();

                if (vehicleNo == null || vehicleNo.isEmpty()) {
                    throw new IllegalArgumentException("vehicleNo is required and cannot be empty");
                }

                List<SearchParkingNoticeDTO> searchResults = axsSearchNoticesUtils.findParkingFinesByVehicleNo(vehicleNo);

                boolean allVehicleNoEmpty = (searchResults.stream().allMatch(dto -> dto.getVehicleNo() == null ));

                if (allVehicleNoEmpty) {
                    SearchParkingNoticeDTO dto = searchResults.get(0);

                    String errorMessage = Optional.ofNullable(searchResults.get(0).getErrorMessage())
                            .orElse("Data Not Found");
                    String formattedMessage = formatErrorMessage(dto, errorMessage);
                    ResponseMessage responseMessage = new ResponseMessage(SUCCESS, formattedMessage);
                    return new ResponseEntity<>(responseMessage, HttpStatus.OK);
                }

                // Filter and map the results to ParkingFinesDTO objects
                List<AxsParkingFinesDTO> parkingFinesDTOs = searchResults.stream()
                        .map(searchResult -> {
                            AxsParkingFinesDTO dto = new AxsParkingFinesDTO();
                            String errorMessage = searchResult.getErrorMessage();
                            dto.setNoticeNo(searchResult.getNoticeNo());
                            dto.setVehicleNo(searchResult.getVehicleNo());
                            dto.setPlaceOfOffence(searchResult.getCarparkName());
                            dto.setAmountPayable(searchResult.getAmountPayable());

                            TimeFormat td = new TimeFormat(searchResult.getNoticeDateAndTime());
                            dto.setOffenceDate(td.getFormattedDate());

                            TimeFormat tf = new TimeFormat(searchResult.getNoticeDateAndTime());
                            dto.setOffenceTime(tf.getFormattedTime());
                            dto.setAmountPayable(searchResult.getAmountPayable());
                            dto.setProcessingStage(searchResult.getLastProcessingStage());
                            dto.setShowPON(searchResult.getShow());
                            dto.setPaymentAllowedFlag(searchResult.getNoticePaymentFlag());
                            if (errorMessage != null) {
                                String formattedMessage = formatErrorMessage(searchResult, searchResult.getErrorMessage());
                                dto.setDisplayMsg(formattedMessage);
                            }
                            dto.setAtomsFlag("N");
                            return dto;
                        })
                        .collect(Collectors.toList());
//                int total = parkingFinesDTOs.size();
//                List<AxsParkingFinesDTO> paginatedList = parkingFinesDTOs.stream()
//                        .skip(skip)
//                        .limit(limit)
//                        .collect(Collectors.toList());
//
//                SingleResponse<List<AxsParkingFinesDTO>> response = SingleResponse.createPaginatedResponse(paginatedList,total,limit,skip);

                AxsResponseDTO finals = new AxsResponseDTO();
                finals.setSender("URA");
                finals.setTargetReceiver("AXS");
                TimeFormat oD = new TimeFormat(LocalDateTime.now());
                finals.setDateSent(oD.getFormattedDate());
                TimeFormat oF = new TimeFormat(LocalDateTime.now());
                finals.setTimeSent(oF.getFormattedTime());
                finals.setSignature(axsUraRequestDTO.getSignature());
                finals.setTransactionID(axsUraRequestDTO.getTransactionID());
                //int limit = (limitVal instanceof Number) ? ((Number) limitVal).intValue() : 10;
                finals.setSearchField(
                        "VN".equalsIgnoreCase(axsUraRequestDTO.getSearchField()) ? "Vehicle" :
                                "NN".equalsIgnoreCase(axsUraRequestDTO.getSearchField()) ? "Notice Number" :
                                        axsUraRequestDTO.getSearchField()
                );
                finals.setSearchValue(axsUraRequestDTO.getSearchValue());
                finals.setStatus(SUCCESS);
                finals.setErrorMsg(null);
                finals.setPonDetails(parkingFinesDTOs);
                // Return the wrapped result
                return new ResponseEntity<>(finals, HttpStatus.OK);
            }
            else {
                System.out.println("No valid parameter provided");
                // No valid parameter provided
                return new ResponseEntity<>(
                        CrudResponse.error(
                                CrudResponse.AppCodes.BAD_REQUEST,
                                "At least one of noticeNo, idNo, or vehicleNo must be provided"
                        ),
                        HttpStatus.BAD_REQUEST
                );
            }
        } catch (Exception e) {
            System.out.println("EXCEPTION: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            // Return consistent array format for all exceptions
            Object[] errorResponse = {-2, "Curl Failed: currError=[Operation timed out after 60000 milliseconds with 0 bytes received], curlErrorNo=[28]"};

            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/axs-update-payment")
    public ResponseEntity<?> updatePayment(@RequestBody PaymentUpdateDTO paymentUpdateDTO) {
        log.info("Received AXS payment update request: {}", paymentUpdateDTO);
        
        try {
            // Save PaymentUpdateDTO fields to EocmsWebTxnAudit using mapper utility
            EocmsWebTxnAudit auditEntity = AxsMapperUtil.mapPaymentUpdateDtoToEntity(paymentUpdateDTO);
            
            // Save audit record
            eocmsWebTxnAuditService.save(auditEntity);
            log.info("Saved audit record with ID: {}", auditEntity.getWebTxnId());
            
            // Process each transaction in the list using mapper utility
            if (paymentUpdateDTO.getTxnList() != null && !paymentUpdateDTO.getTxnList().isEmpty()) {
                for (TransactionDTO transaction : paymentUpdateDTO.getTxnList()) {
                    EocmsWebTxnDetail detailEntity = AxsMapperUtil.mapTransactionDtoToDetail(transaction, paymentUpdateDTO);
                    
                    // Save transaction detail
                    eocmsWebTxnDetailService.save(detailEntity);
                    log.info("Saved transaction detail for receipt: {}", transaction.getReceiptNo());
                }
            }
            
            // Create and return UraPaymentUpdateResponseDTO
            UraPaymentUpdateResponseDTO responseDTO = new UraPaymentUpdateResponseDTO();
            responseDTO.setSender(paymentUpdateDTO.getSender());
            responseDTO.setTargetReceiver(paymentUpdateDTO.getTargetReceiver());
            responseDTO.setDateSent(paymentUpdateDTO.getDateSent());
            responseDTO.setTimeSent(paymentUpdateDTO.getTimeSent());
            responseDTO.setSignature(paymentUpdateDTO.getSignature());
            responseDTO.setTransactionID(paymentUpdateDTO.getTransactionID());
            responseDTO.setRecordCounter(paymentUpdateDTO.getRecordCounter());
            responseDTO.setTotalAmt(paymentUpdateDTO.getTotalAmt());
            
            // Map TransactionDTO list to UraPaymentTransactionDTO list
            if (paymentUpdateDTO.getTxnList() != null) {
                List<UraPaymentTransactionDTO> responseTransactions = paymentUpdateDTO.getTxnList().stream()
                    .map(txn -> {
                        UraPaymentTransactionDTO responseTxn = new UraPaymentTransactionDTO();
                        responseTxn.setReceiptNo(txn.getReceiptNo());
                        responseTxn.setTypeOfReceipt(txn.getTypeOfReceipt());
                        responseTxn.setTransactionDate(txn.getTransactionDate());
                        responseTxn.setTransactionTime(txn.getTransactionTime());
                        responseTxn.setNoticeNo(txn.getNoticeNo());
                        responseTxn.setVehicleNo(txn.getVehicleNo());
                        responseTxn.setAtomsFlag(txn.getAtomsFlag());
                        responseTxn.setPaymentMode(txn.getPaymentMode());
                        responseTxn.setPaymentAmount(txn.getPaymentAmount());
                        responseTxn.setRemarks(txn.getRemarks());
                        return responseTxn;
                    })
                    .collect(Collectors.toList());
                responseDTO.setTxnList(responseTransactions);
            }
            
            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
            
        } catch (Exception e) {
            log.error("Error processing payment update: {}", e.getMessage(), e);
            
            // Create error response using UraPaymentUpdateResponseDTO
            UraPaymentUpdateResponseDTO errorResponse = new UraPaymentUpdateResponseDTO();
            errorResponse.setSender(paymentUpdateDTO.getSender());
            errorResponse.setTargetReceiver(paymentUpdateDTO.getTargetReceiver());
            errorResponse.setDateSent(paymentUpdateDTO.getDateSent());
            errorResponse.setTimeSent(paymentUpdateDTO.getTimeSent());
            errorResponse.setSignature(paymentUpdateDTO.getSignature());
            errorResponse.setTransactionID(paymentUpdateDTO.getTransactionID());
            errorResponse.setRecordCounter(paymentUpdateDTO.getRecordCounter());
            errorResponse.setTotalAmt(paymentUpdateDTO.getTotalAmt());
            errorResponse.setErrorMsg("Unable to Validate Signature.");
            errorResponse.setStatus("FAIL");
            errorResponse.setTxnList(null); // No transaction list in error response
            
            // Wrap the DTO in an array with error code -3
            Object[] finalErrorResponse = {-3, errorResponse};
            return new ResponseEntity<>(finalErrorResponse, HttpStatus.BAD_REQUEST);
        }
    }

}

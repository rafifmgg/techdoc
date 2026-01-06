package com.ocmseservice.apiservice.utilities.parkingfines;

import com.ocmseservice.apiservice.crud.ocmsezdb.eocmsusermessage.EocmsUserMessageRepository;
import com.ocmseservice.apiservice.crud.ocmsezdb.eocmsvalidoffencenotice.EocmsValidOffenceNotice;
import com.ocmseservice.apiservice.crud.ocmsezdb.eocmsvalidoffencenotice.EocmsValidOffenceNoticeRepository;
import com.ocmseservice.apiservice.crud.ocmsezdb.eocmswebtxndetail.EocmsWebTxnDetail;
import com.ocmseservice.apiservice.crud.ocmsezdb.eocmswebtxndetail.EocmsWebTxnDetailRepository;
import com.ocmseservice.apiservice.crud.ocmspii.eocmsoffencenoticeownerdriver.EocmsOffenceNoticeOwnerDriverRepository;
import com.ocmseservice.apiservice.workflows.searchnoticetopay.dto.SearchParkingNoticeDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class SearchNoticesUtils {

    private final EocmsValidOffenceNoticeRepository eocmsValidOffenceNoticeRepository;
    private final EocmsUserMessageRepository eocmsUserMessageRepository;
    private final EocmsWebTxnDetailRepository eocmsWebTxnDetailRepository;
    private final EocmsOffenceNoticeOwnerDriverRepository eocmsOffenceNoticeOwnerDriverRepository;
    private static EocmsUserMessageRepository userMessageRepository;

    @Autowired
    public SearchNoticesUtils(EocmsValidOffenceNoticeRepository eocmsValidOffenceNoticeRepository,
                              EocmsUserMessageRepository eocmsUserMessageRepository,
                              EocmsWebTxnDetailRepository eocmsWebTxnDetailRepository,
                              EocmsOffenceNoticeOwnerDriverRepository eocmsOffenceNoticeOwnerDriverRepository) {
        this.eocmsValidOffenceNoticeRepository = eocmsValidOffenceNoticeRepository;
        this.eocmsUserMessageRepository = eocmsUserMessageRepository;
        this.eocmsWebTxnDetailRepository = eocmsWebTxnDetailRepository;
        this.eocmsOffenceNoticeOwnerDriverRepository = eocmsOffenceNoticeOwnerDriverRepository;
        SearchNoticesUtils.userMessageRepository = eocmsUserMessageRepository;

    }

    public static String getMessage(String errorCode, String defaultMessage) {
        if (userMessageRepository == null) {
            return defaultMessage;
        }
        String message = userMessageRepository.findMessageByErrorCode(errorCode);
        return message != null ? message : defaultMessage;
    }


    public SearchParkingNoticeDTO checkNotices(String noticeNumber) {
        EocmsValidOffenceNotice entity = eocmsValidOffenceNoticeRepository.findByNoticeNo(noticeNumber);
        SearchParkingNoticeDTO dto = new SearchParkingNoticeDTO();

        if (entity == null) {
            String errorCode = "E9";
            dto.setNoticeNo(noticeNumber);
            dto.setErrorMessage(SearchNoticesUtils.getMessage(errorCode,
                    "There is no outstanding parking offence for the vehicle number $VEHICLE_NO$, as of $DATE$. If you wish to check or pay for a particular notice number, you may click here."));
            //dto.setAppCode(errorCode);
            return dto;
        }

        // Set data awal dari entity ke DTO
        dto.setNoticeNo(entity.getNoticeNo());
        dto.setVehicleNo(entity.getVehicleNo());
        dto.setNoticeDateAndTime(entity.getNoticeDateAndTime());
        dto.setAmountPayable(entity.getAmountPayable());
        dto.setPpCode(entity.getPpCode());
        dto.setLastProcessingStage(entity.getLastProcessingStage());
        dto.setVehicleRegistrationType(entity.getVehicleRegistrationType());
        dto.setOffenceNoticeType(entity.getOffenceNoticeType());
        dto.setAnFlag(entity.getAnFlag());
        dto.setPaymentAcceptanceAllowed(entity.getPaymentAcceptanceAllowed());
        dto.setSeachBy("NN");


        //  payment flag menggunakan utility
        String noticePaymentFlag = PaymentFlagUtil.getNoticePaymentFlag(
                entity.getSuspensionType(),
                entity.getEprReasonOfSuspension(),
                entity.getCrsReasonOfSuspension()

        );
        dto.setNoticePaymentFlag(noticePaymentFlag);

        // Cek apakah masuk kategori PP/TS atau PRA/PS
        boolean isPPTS =
                ("PP".equals(entity.getCrsReasonOfSuspension()) && "TS".equals(entity.getSuspensionType())) ||
                        ("PRA".equals(entity.getCrsReasonOfSuspension()) && "PS".equals(entity.getSuspensionType()));

        if (isPPTS) {
            String errorCode = "E7";
            dto.setErrorMessage(SearchNoticesUtils.getMessage(errorCode,
                    "Transaction is still being processed. Please wait at least 5 minutes before trying again."));
            dto.setAppCode(errorCode);
            dto.setShow("N");
            dto.setPaymentAcceptanceAllowed("N");
            dto.setAllowSelect("N");
            return dto; // Hentikan proses selanjutnya, validator tidak dipanggil
        }

        // Validasi dengan WebTxnAuditValidator
        SearchNoticesUtils.WebTxnAuditValidator webTxnValidator =
                new SearchNoticesUtils.WebTxnAuditValidator(this.eocmsWebTxnDetailRepository, this.eocmsUserMessageRepository);
        SearchParkingNoticeDTO validation = webTxnValidator.validateTransaction(dto);

        if (validation != null) {
            dto.setPaymentAcceptanceAllowed(validation.getPaymentAcceptanceAllowed());
            dto.setShow(validation.getShow());
            dto.setErrorMessage(validation.getErrorMessage());
            dto.setAllowSelect(validation.getAllowSelect());
            dto.setAppCode(validation.getAppCode());
            dto.setNoticePaymentFlag(validation.getNoticePaymentFlag());
            dto.setTransactionDateAndTime(validation.getTransactionDateAndTime());
        }

        return dto;
    }

    public List<SearchParkingNoticeDTO> findParkingFinesByVehicleNo(String vehicleNo) {
        List<EocmsValidOffenceNotice> fullEntities = eocmsValidOffenceNoticeRepository.findAllByVehicleNoIn(vehicleNo);
        List<SearchParkingNoticeDTO> results = new ArrayList<>();

        if (fullEntities == null || fullEntities.isEmpty()) {
            SearchParkingNoticeDTO dto = new SearchParkingNoticeDTO();
            String errorCode = "E7";
            dto.setNoticeNo(vehicleNo);
            dto.setErrorMessage(SearchNoticesUtils.getMessage(errorCode,
                    "There is no outstanding parking offence for the vehicle number $VEHICLE_NO$, as of $DATE$. If you wish to check or pay for a particular notice number, you may click here."));
            //dto.setAppCode(errorCode);
            results.add(dto);
            return results;
        }

        for (EocmsValidOffenceNotice entity : fullEntities) {
            SearchParkingNoticeDTO dto = new SearchParkingNoticeDTO();

            // Mapping basic entity data
            dto.setNoticeNo(entity.getNoticeNo());
            dto.setVehicleNo(entity.getVehicleNo());
            dto.setAmountPayable(entity.getAmountPayable());
            dto.setNoticeDateAndTime(entity.getNoticeDateAndTime());
           // System.out.println("ada ga jamnya"+entity.getNoticeDateAndTime());
            dto.setPpCode(entity.getPpCode());
            dto.setLastProcessingStage(entity.getLastProcessingStage());
            dto.setVehicleRegistrationType(entity.getVehicleRegistrationType());
            dto.setAnFlag(entity.getAnFlag());
            dto.setPaymentAcceptanceAllowed(entity.getPaymentAcceptanceAllowed());
            dto.setOffenceNoticeType(entity.getOffenceNoticeType());
            dto.setSeachBy("VN");



            // Payment flag
            String suspen = entity.getSuspensionType();
            System.out.println("cek hasil entity===="+suspen);
            String noticePaymentFlag = PaymentFlagUtil.getNoticePaymentFlag(
                    entity.getSuspensionType(),
                    entity.getEprReasonOfSuspension(),
                    entity.getCrsReasonOfSuspension()
            );
            System.out.println("hasil cek =="+noticePaymentFlag);
            dto.setNoticePaymentFlag(noticePaymentFlag);

            // Check TS-PP or PS-PRA case
            boolean isPPTS =
                    ("PP".equals(entity.getCrsReasonOfSuspension()) && "TS".equals(entity.getSuspensionType())) ||
                            ("PRA".equals(entity.getCrsReasonOfSuspension()) && "PS".equals(entity.getSuspensionType()));

            if (isPPTS) {
                String errorCode = "E7";
                dto.setErrorMessage(SearchNoticesUtils.getMessage(errorCode,
                        "Transaction is still being processed. Please wait at least 5 minutes before trying again."));
                dto.setAppCode(errorCode);
                dto.setPaymentAcceptanceAllowed("N");
                dto.setAllowSelect("N");
                dto.setShow("N");

            } else {
                // Lanjutkan ke WebTxnAuditValidator
                SearchNoticesUtils.WebTxnAuditValidator webTxnValidator =
                        new SearchNoticesUtils.WebTxnAuditValidator(this.eocmsWebTxnDetailRepository, this.eocmsUserMessageRepository);
                SearchParkingNoticeDTO validation = webTxnValidator.validateTransaction(dto);

                if (validation != null) {
                    dto.setPaymentAcceptanceAllowed(validation.getPaymentAcceptanceAllowed());
                    dto.setAllowSelect(validation.getAllowSelect());
                    dto.setShow(validation.getShow());
                    dto.setAppCode(validation.getAppCode());
                    dto.setErrorMessage(validation.getErrorMessage());
                    dto.setNoticePaymentFlag(validation.getNoticePaymentFlag());
                    dto.setTransactionDateAndTime(validation.getTransactionDateAndTime());
                }
            }

            results.add(dto);
        }

        return results;
    }


    public List<SearchParkingNoticeDTO> findParkingFinesByIdNo(String idNos) {
        List<SearchParkingNoticeDTO> results = new ArrayList<>();

        // Step 1: Get all notice numbers for this ID
        List<String> noticeNumbers = eocmsOffenceNoticeOwnerDriverRepository.findNoticeNosByIdNoAndOffenderIndicator(idNos);

        if (noticeNumbers == null || noticeNumbers.isEmpty()) return results;

        // Step 2: Get all corresponding offence entities
        List<EocmsValidOffenceNotice> entities = eocmsValidOffenceNoticeRepository.findAllByNoticeNoIn(noticeNumbers);


        if (entities == null || entities.isEmpty()) {
            SearchParkingNoticeDTO dto = new SearchParkingNoticeDTO();
            String errorCode = "E8";
            dto.setNoticeNo(idNos);
            dto.setErrorMessage(SearchNoticesUtils.getMessage(errorCode,
                    "There is no outstanding parking offence for the vehicle number $VEHICLE_NO$, as of $DATE$. If you wish to check or pay for a particular notice number, you may click here."));
            //dto.setAppCode(errorCode);
            results.add(dto);
            return results;
        }


        for (EocmsValidOffenceNotice entity : entities) {
            SearchParkingNoticeDTO dto = new SearchParkingNoticeDTO();

            // Mapping awal
            dto.setNoticeNo(entity.getNoticeNo());
            dto.setVehicleNo(entity.getVehicleNo());
            dto.setAmountPayable(entity.getAmountPayable());
            dto.setPpCode(entity.getPpCode());
            dto.setLastProcessingStage(entity.getLastProcessingStage());
            dto.setVehicleRegistrationType(entity.getVehicleRegistrationType());
            dto.setAnFlag(entity.getAnFlag());
            dto.setOffenceNoticeType(entity.getOffenceNoticeType());
            dto.setPaymentAcceptanceAllowed(entity.getPaymentAcceptanceAllowed());
            dto.setSeachBy("IN");
            dto.setNoticeDateAndTime(entity.getNoticeDateAndTime());

            // Set Payment Flag
            String noticePaymentFlag = PaymentFlagUtil.getNoticePaymentFlag(
                    entity.getSuspensionType(),
                    entity.getEprReasonOfSuspension(),
                    entity.getCrsReasonOfSuspension()
            );
            dto.setNoticePaymentFlag(noticePaymentFlag);

            // Step 3: Check TS/PP or PS/PRA rule
            boolean isPPTS =
                    ("PP".equals(entity.getCrsReasonOfSuspension()) && "TS".equals(entity.getSuspensionType())) ||
                            ("PRA".equals(entity.getCrsReasonOfSuspension()) && "PS".equals(entity.getSuspensionType()));

            if (isPPTS) {
                String errorCode = "E7";
                dto.setErrorMessage(SearchNoticesUtils.getMessage(errorCode,
                        "Transaction is still being processed. Please wait at least 5 minutes before trying again."));
                dto.setAppCode(errorCode);
                dto.setShow("N");
                dto.setPaymentAcceptanceAllowed("N");
                dto.setAllowSelect("N");
            } else {
                // Step 4: Run WebTxnAuditValidator if not PPTS
                SearchNoticesUtils.WebTxnAuditValidator webTxnValidator =
                        new SearchNoticesUtils.WebTxnAuditValidator(this.eocmsWebTxnDetailRepository, this.eocmsUserMessageRepository);

                SearchParkingNoticeDTO validation = webTxnValidator.validateTransaction(dto);

                if (validation != null) {
                    dto.setPaymentAcceptanceAllowed(validation.getPaymentAcceptanceAllowed());
                    dto.setShow(validation.getShow());
                    dto.setAllowSelect(validation.getAllowSelect());
                    dto.setAppCode(validation.getAppCode());
                    dto.setErrorMessage(validation.getErrorMessage());
                    dto.setNoticePaymentFlag(validation.getNoticePaymentFlag());
                    dto.setTransactionDateAndTime(validation.getTransactionDateAndTime());
                }
            }

            results.add(dto);
        }

        return results;
    }


    public static class WebTxnAuditValidator {
        private final EocmsWebTxnDetailRepository webTxnDetailRepository;
        private final EocmsUserMessageRepository userMessageRepository;

        public WebTxnAuditValidator(EocmsWebTxnDetailRepository webTxnDetailRepository,
                                    EocmsUserMessageRepository userMessageRepository) {
            this.webTxnDetailRepository = webTxnDetailRepository;
            this.userMessageRepository = userMessageRepository;
        }
        public SearchParkingNoticeDTO validateTransaction(SearchParkingNoticeDTO dto) {
            String noticeNo = dto.getNoticeNo();
           // EocmsWebTxnDetail transactions = webTxnDetailRepository.findByNotice(noticeNo);

            Optional<EocmsWebTxnDetail> transactions = webTxnDetailRepository.findByNotice(noticeNo);

            //System.out.println("Check transaksi hari = " + transactions);
            if (transactions.isEmpty()) {

                return com.ocmseservice.apiservice.utilities.stages.PaymentMatrikResponse.validate(dto);
            }
            EocmsWebTxnDetail txn = transactions.get();

           // LocalDateTime txnTime = transactions.getTransactionDateAndTime();
            LocalDateTime txnTime = txn.getTransactionDateAndTime();
            System.out.println("cek isi time = " + txnTime);

            SearchParkingNoticeDTO result = new SearchParkingNoticeDTO();
            result.setNoticeNo(txn.getOffenceNoticeNo());
            result.setVehicleNo(txn.getVehicleNo());
            result.setPpCode(dto.getPpCode());
            result.setAnFlag(dto.getAnFlag());
            result.setLastProcessingStage(dto.getLastProcessingStage());

            // Default settings
            result.setNoticeDateAndTime(dto.getNoticeDateAndTime());
            result.setTransactionDateAndTime(txnTime);
            result.setPaymentAcceptanceAllowed("N");
            result.setNoticePaymentFlag("NOT PAYABLE");
            result.setShow("Y");
            result.setAllowSelect("N");

            if (isWithin300Seconds(txnTime)) {
                result.setErrorMessage(SearchNoticesUtils.getMessage(
                        "E11",
                        "Your payment for notice number(s) $NOTICE_NO$ is being processed. Please check again 5 minutes later."
                ));
                result.setAppCode("E11");
                return result;
            } else {
                result.setErrorMessage(SearchNoticesUtils.getMessage(
                        "E10",
                        "There was a payment attempt to this notice on $DATE$. If you have successfully made the payment on eNETS, please do not make another payment."
                ));
                result.setAppCode("E10");
                return result;
            }
        }
        public boolean isWithin300Seconds(LocalDateTime transactionDateAndTime) {
            if (transactionDateAndTime == null) return false;

            LocalDateTime now = LocalDateTime.now();
            long secondsDifference = Duration.between(transactionDateAndTime, now).getSeconds();

            return secondsDifference <= 300;

        }
        public static boolean isMoreThan300Seconds(LocalDateTime transactionDateAndTime) {
            if (transactionDateAndTime == null) return false;

            LocalDateTime now = LocalDateTime.now();
            long secondsDifference = Duration.between(transactionDateAndTime, now).getSeconds();

            return secondsDifference > 300;
        }
    }

}

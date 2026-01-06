package com.ocmseservice.apiservice.utilities.stages;

import com.ocmseservice.apiservice.crud.ocmsezdb.eocmsusermessage.EocmsUserMessageRepository;
import com.ocmseservice.apiservice.workflows.searchnoticetopay.dto.SearchParkingNoticeDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class PaymentMatrikResponse {

    //private static String ruleCode;
    private static String lastProcessingStage;
    private static String nextProcessingStage;
    private static String vehicleRegistrationType;
    private static String anFlag;
    //private static String paymentAcceptanceFlag;
    private static String paymentAcceptanceAllowed;
    private static String offenceNoticeType;
    private static String noticePaymentFlag;// ini dari query
    private static String seachBy;
    private static EocmsUserMessageRepository userMessageRepository;

    @Autowired
    public PaymentMatrikResponse(
                                 EocmsUserMessageRepository userMessageRepository) {
        PaymentMatrikResponse.userMessageRepository = userMessageRepository;
    }


    private static String getMessage(String errorCode, String defaultMessage) {
        if (userMessageRepository == null) {
            return defaultMessage;
        }
        String message = userMessageRepository.findMessageByErrorCode(errorCode);
        return message != null ? message : defaultMessage;
    }


    public static SearchParkingNoticeDTO validateOffenceTypeO(SearchParkingNoticeDTO dto) {
        lastProcessingStage = dto.getLastProcessingStage();
        vehicleRegistrationType = dto.getVehicleRegistrationType();
        anFlag = dto.getAnFlag();
        offenceNoticeType = dto.getOffenceNoticeType();
        paymentAcceptanceAllowed = dto.getPaymentAcceptanceAllowed();
        seachBy = dto.getSeachBy();

        SearchParkingNoticeDTO result = new SearchParkingNoticeDTO();
        result.setNoticeNo(dto.getNoticeNo());
        result.setVehicleNo(dto.getVehicleNo());
        result.setAmountPayable(dto.getAmountPayable());
        result.setPpCode(dto.getPpCode());
        result.setOffenceNoticeType(dto.getOffenceNoticeType());

        if (anFlag.equals("Y")) {
            result.setPaymentAcceptanceAllowed("N");
            result.setNoticePaymentFlag("NOT PAYABLE");
            result.setShow("N");
            result.setAllowSelect("N");
            result.setErrorCode("E5");
            String errorCode = "E5";
            result.setErrorMessage(PaymentMatrikResponse.getMessage(errorCode,
                    "This is an Advisory Notice. No payment is required. (Notice number: 500500128F)"));
        }

        if ("F".equals(vehicleRegistrationType) && !anFlag.equals("Y")) {
            result.setPaymentAcceptanceAllowed("Y");
            result.setNoticePaymentFlag("PAYABLE");
            result.setShow("Y");
            result.setAllowSelect("Y");
            result.setErrorCode("E6");
            String errorCode = "E6";
            //System.out.println("masuk sini harusnya===="+vehicleRegistrationType);
            result.setErrorMessage(PaymentMatrikResponse.getMessage(errorCode,
                    "Transaction is still being processed. Please wait at least 5 minutes before trying again."));
        }
        if (!"F".equals(vehicleRegistrationType) && !"Y".equals(anFlag)) {
            String[] stageType = { "NPA", "ROV", "ENA", "RD1", "RD2", "RR3", "DN1", "DN2", "DR3" };
            boolean exists = Arrays.asList(stageType).contains(lastProcessingStage);

            if (exists) {
                result.setPaymentAcceptanceAllowed("Y");
                result.setNoticePaymentFlag("PAYABLE");
                result.setShow("Y");
                result.setAllowSelect("Y");
                result.setErrorCode("E1");
                String errorCode = "E1";
                result.setErrorMessage(PaymentMatrikResponse.getMessage(errorCode,
                        "Transaction is still being processed. Please wait at least 5 minutes before trying again."));
            }

            else {
                if (seachBy.equals("VN") && paymentAcceptanceAllowed.equals("Y")) {
                    //set notice_payment_flag = Payable
                    //set show = N
                    //set error_message from code E2
                    result.setPaymentAcceptanceAllowed("Y");
                    result.setNoticePaymentFlag("PAYABLE");
                    result.setShow("N");
                    result.setAllowSelect("N");
                    result.setErrorCode("E2");
                    String errorCode = "E2";
                    result.setErrorMessage(PaymentMatrikResponse.getMessage(errorCode,
                            "Transaction is still being processed. Please wait at least 5 minutes before trying again."));

                }
                if (seachBy.equals("VN") && !paymentAcceptanceAllowed.equals("Y")) {
                    //set notice_payment_flag = Not Payable
                    //set show = N
                    //set error_message from code E2
                    result.setPaymentAcceptanceAllowed("N");
                    result.setNoticePaymentFlag("NOT PAYABLE");
                    result.setShow("N");
                    result.setAllowSelect("N");
                    result.setErrorCode("E2");
                    String errorCode = "E2";
                    result.setErrorMessage(PaymentMatrikResponse.getMessage(errorCode,
                            "Transaction is still being processed. Please wait at least 5 minutes before trying again."));
                }
                if (!seachBy.equals("VN") && paymentAcceptanceAllowed.equals("Y")) {
                    //set notice_payment_flag = Payable
                    //set show = Y
                    //set error_message from code E1
                    result.setPaymentAcceptanceAllowed("Y");
                    result.setNoticePaymentFlag("PAYABLE");
                    result.setShow("Y");
                    result.setAllowSelect("N");
                    result.setErrorCode("E1");
                    String errorCode = "E1";
                    result.setErrorMessage(PaymentMatrikResponse.getMessage(errorCode,
                            "Transaction is still being processed. Please wait at least 5 minutes before trying again."));
                }
                if (!seachBy.equals("VN") && !paymentAcceptanceAllowed.equals("Y")) {
                    //set notice_payment_flag = Not Payable
                    //set show = Y
                    //set error_message from code E4
                    result.setPaymentAcceptanceAllowed("N");
                    result.setNoticePaymentFlag("NOT PAYABLE");
                    result.setShow("Y");
                    result.setAllowSelect("N");
                    result.setErrorCode("E4");
                    String errorCode = "E4";
                    result.setErrorMessage(PaymentMatrikResponse.getMessage(errorCode,
                            "Transaction is still being processed. Please wait at least 5 minutes before trying again."));
                }
            }
        }



        return result;
    }



    /**
     * Metode untuk memvalidasi pembayaran untuk tipe pelanggaran U (URA)
     *
     * @param dto DTO yang berisi semua informasi yang diperlukan
     * @return SearchParkingNoticeDTO dengan hasil validasi
     */
    public static SearchParkingNoticeDTO validateOffenceTypeU(SearchParkingNoticeDTO dto) {
        lastProcessingStage = dto.getLastProcessingStage();
        vehicleRegistrationType = dto.getVehicleRegistrationType();
        anFlag = dto.getAnFlag();
        offenceNoticeType = dto.getOffenceNoticeType();
        paymentAcceptanceAllowed = dto.getPaymentAcceptanceAllowed();
        seachBy = dto.getSeachBy();

        SearchParkingNoticeDTO result = new SearchParkingNoticeDTO();
        result.setNoticeNo(dto.getNoticeNo());
        result.setVehicleNo(dto.getVehicleNo());
        result.setAmountPayable(dto.getAmountPayable());
        result.setPpCode(dto.getPpCode());
        result.setOffenceNoticeType(dto.getOffenceNoticeType());

        if ("F".equals(vehicleRegistrationType)) {
            result.setPaymentAcceptanceAllowed("Y");
            result.setNoticePaymentFlag("PAYABLE");
            result.setShow("Y");
            result.setAllowSelect("Y");
            result.setErrorCode("E6");
            String errorCode = "E6";
            result.setErrorMessage(PaymentMatrikResponse.getMessage(errorCode,
                    "Transaction is still being processed. Please wait at least 5 minutes before trying again."));
        }
        if (!"F".equals(vehicleRegistrationType)){
            String[] stageType = { "NPA", "ROV", "ENA", "DN1", "DN2", "DR3" };
            boolean exists = Arrays.asList(stageType).contains(lastProcessingStage);

            if (exists) {
                result.setPaymentAcceptanceAllowed("Y");
                result.setNoticePaymentFlag("PAYABLE");
                result.setShow("Y");
                result.setAllowSelect("Y");
                result.setErrorCode("E1");
                String errorCode = "E1";
                result.setErrorMessage(PaymentMatrikResponse.getMessage(errorCode,
                        "Transaction is still being processed. Please wait at least 5 minutes before trying again."));
            }
            else {
                if (seachBy.equals("VN") && paymentAcceptanceAllowed.equals("Y")) {
                    //set notice_payment_flag = Payable
                    //set show = N
                    //set error_message from code E2
                    result.setPaymentAcceptanceAllowed("Y");
                    result.setNoticePaymentFlag("PAYABLE");
                    result.setShow("N");
                    result.setAllowSelect("N");
                    result.setErrorCode("E2");
                    String errorCode = "E2";
                    result.setErrorMessage(PaymentMatrikResponse.getMessage(errorCode,
                            "Transaction is still being processed. Please wait at least 5 minutes before trying again."));

                }
                if (seachBy.equals("VN") && !paymentAcceptanceAllowed.equals("Y")) {
                    //set notice_payment_flag = Not Payable
                    //set show = N
                    //set error_message from code E2
                    result.setPaymentAcceptanceAllowed("N");
                    result.setNoticePaymentFlag("NOT PAYABLE");
                    result.setShow("N");
                    result.setAllowSelect("N");
                    result.setErrorCode("E2");
                    String errorCode = "E2";
                    result.setErrorMessage(PaymentMatrikResponse.getMessage(errorCode,
                            "Transaction is still being processed. Please wait at least 5 minutes before trying again."));
                }
                if (!seachBy.equals("VN") && paymentAcceptanceAllowed.equals("Y")) {
                    //set notice_payment_flag = Payable
                    //set show = Y
                    //set error_message from code E1
                    result.setPaymentAcceptanceAllowed("Y");
                    result.setNoticePaymentFlag("PAYABLE");
                    result.setShow("Y");
                    result.setAllowSelect("N");
                    result.setErrorCode("E1");
                    String errorCode = "E1";
                    result.setErrorMessage(PaymentMatrikResponse.getMessage(errorCode,
                            "Transaction is still being processed. Please wait at least 5 minutes before trying again."));
                }
                if (!seachBy.equals("VN") && !paymentAcceptanceAllowed.equals("Y")) {
                    //set notice_payment_flag = Not Payable
                    //set show = Y
                    //set error_message from code E4
                    result.setPaymentAcceptanceAllowed("N");
                    result.setNoticePaymentFlag("NOT PAYABLE");
                    result.setShow("Y");
                    result.setAllowSelect("N");
                    result.setErrorCode("E4");
                    String errorCode = "E4";
                    result.setErrorMessage(PaymentMatrikResponse.getMessage(errorCode,
                            "Transaction is still being processed. Please wait at least 5 minutes before trying again."));
                }
            }
        }

        return result;
    }



    public static SearchParkingNoticeDTO validate(SearchParkingNoticeDTO dto) {
        if ("O".equals(dto.getOffenceNoticeType()) || "E".equals(dto.getOffenceNoticeType())  ) {
            return validateOffenceTypeO(dto);
        } else if ("U".equals(dto.getOffenceNoticeType())) {
            return validateOffenceTypeU(dto);
        }
        else {
            System.out.println("jangan2 masuk sini "+ dto.getNoticeNo());
            // Tipe pelanggaran tidak dikenal
            String errorCodes= "E4";
            dto.setErrorMessage(PaymentMatrikResponse.getMessage(errorCodes, "Transaction is still being processed. Please wait at least 5 minutes before trying again."));

            return dto;
        }
    }
}

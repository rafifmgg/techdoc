package com.ocmseservice.apiservice.utilities;

import com.ocmseservice.apiservice.workflows.searchnoticetopay.dto.SearchParkingNoticeDTO;

import java.text.SimpleDateFormat;
import java.util.Date;


public class ErrorMessageFormatter {

    /**
     * Format error message by replacing predefined placeholders.
     *
     * Supported placeholders:
     * - $NOTICE_NO$ / $NOTICE_NUMBERS$
     * - $VEHICLE_NO$
     * - $DATE$ (MM/dd/yyyy)
     *
     * @param dto The DTO containing notice and vehicle information
     * @param errorMessage The original error message template
     * @return Formatted error message with actual values
     */
    public static String formatErrorMessage(SearchParkingNoticeDTO dto, String errorMessage) {
        if (errorMessage == null) {
            return null;
        }


        String currentDate = new SimpleDateFormat("MM/dd/yyyy").format(new Date());
        String noticeNumbers = dto.getNoticeNo() != null ? dto.getNoticeNo() : "";
        String vehicleNumber = dto.getVehicleNo() != null ? dto.getVehicleNo() : "";
        // String idNumber = dto.getIdNo() != null ? dto.getIdNo() : "";

        return errorMessage
                .replace("$NOTICE_NO$", noticeNumbers)
                .replace("$NOTICE_NUMBERS$", noticeNumbers)
                .replace("$VEHICLE_NO$", vehicleNumber)
                // .replace("$ID_NO$", idNumber)
                .replace("$DATE$", currentDate);
    }
}
package com.ocmseservice.apiservice.utilities.parkingfines;

public class PaymentFlagUtil {
    public static String getNoticePaymentFlag(String suspensionType, String reasonOfSuspension, String getCrsReasonOfSuspension) {

        // Step 1: Check for "PAID"
        if ("FP".equals(getCrsReasonOfSuspension) ||
                "PP".equals(getCrsReasonOfSuspension) ||
                "PRA".equals(getCrsReasonOfSuspension)) {
            return "PAID";
        }

        // Step 2: Check for "NOT PAYABLE"
        if ("PS".equals(suspensionType) &&
                getCrsReasonOfSuspension != null &&
                !getCrsReasonOfSuspension.equals("FOR") &&
                !getCrsReasonOfSuspension.equals("MID") &&
                !getCrsReasonOfSuspension.equals("RIP") &&
                !getCrsReasonOfSuspension.equals("DIP")) {
            return "NOT PAYABLE";
        }

        // Step 3: Default to "PAYABLE"
        return "PAYABLE";
    }
}


package com.ocmsintranet.apiservice.workflows.notice_creation.core.helpers;

import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import com.ocmsintranet.apiservice.workflows.notice_creation.core.dto.OffenceNoticeDto;
import com.ocmsintranet.apiservice.crud.ocmsizdb.sequence.SequenceService;
import com.ocmsintranet.apiservice.crud.beans.SystemConstant;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class NoticeNumberHelper {
    
    @Autowired
    private SequenceService sequenceService;

    /**
     * Process notice number - extract existing or generate new
     * Format: Year(1) + Subsystem(3) + Sequence(5) + Checksum(1) = 10 characters total
     */
    public String processNoticeNumber(Map<String, Object> singleDtoMap) {
        log.info("Extracting/generating notice number");

        OffenceNoticeDto dto = (OffenceNoticeDto) singleDtoMap.get("dto");
        String noticeNumber = dto != null ? dto.getNoticeNo() : null;

        if (noticeNumber == null || noticeNumber.isEmpty()) {
            if (dto == null) {
                throw new IllegalArgumentException("DTO is required to generate notice number");
            }

            String subsystemLabel = dto.getSubsystemLabel();
            if (subsystemLabel == null || subsystemLabel.isEmpty()) {
                throw new IllegalArgumentException("Subsystem label is required to generate notice number");
            }

            // Only generate notice numbers for allowed subsystems
            if (!SystemConstant.Subsystem.OCMS_CODE.equals(subsystemLabel) &&
                    !SystemConstant.Subsystem.PLUS_CODE.equals(subsystemLabel) &&
                    !SystemConstant.Subsystem.MEV_CODE_006.equals(subsystemLabel) &&
                    !SystemConstant.Subsystem.MEV_CODE_007.equals(subsystemLabel) &&
                    !SystemConstant.Subsystem.MEV_CODE_008.equals(subsystemLabel) &&
                    !SystemConstant.Subsystem.MEV_CODE_009.equals(subsystemLabel) &&
                    !SystemConstant.Subsystem.MEV_CODE_010.equals(subsystemLabel) &&
                    !SystemConstant.Subsystem.REPCCS_ENC_CODE_020.equals(subsystemLabel) &&
                    !SystemConstant.Subsystem.REPCCS_ENC_CODE_021.equals(subsystemLabel) &&
                    !SystemConstant.Subsystem.REPCCS_EHT_CODE.equals(subsystemLabel)) {
                log.error("Cannot generate notice number for unsupported subsystem: {}", subsystemLabel);
                throw new IllegalArgumentException("Notice number generation is only supported for OCMS, PLUS, and EEPS subsystems. Received: " + subsystemLabel);
            }

            // Generate components
            String yearDigit = getLastDigitOfYear();
            String subsystemCode = subsystemLabel;
            String runningSeriesNext = getNextSequenceNumber(subsystemLabel);

            // Build the 9-digit number for checksum calculation
            String baseNumber = yearDigit + subsystemCode + runningSeriesNext;
            String checksum = calculateChecksum(baseNumber);

            // Combine all components
            noticeNumber = baseNumber + checksum;

            dto.setNoticeNo(noticeNumber);

            log.info("Generated new notice number: {} (base: {}, checksum: {})",
                    noticeNumber, baseNumber, checksum);
        } else {
            log.info("Using existing notice number: {}", noticeNumber);
        }

        return noticeNumber;
    }

    /**
     * Get the last digit of the current year
     * Example: 2025 -> 5, 2030 -> 0
     */
    private String getLastDigitOfYear() {
        // Step 1: Get current year from system
        int currentYear = LocalDateTime.now().getYear();

        // Step 2: Extract last digit using modulo 10
        // Year 2025 % 10 = 5
        return String.valueOf(currentYear % 10);
    }

    /**
     * Get next sequence number from database sequence specific to subsystem
     * Formats the sequence to 5 digits with leading zeros (e.g., 00001)
     */
    private String getNextSequenceNumber(String subsystemLabel) {
        try {
            // Map numeric subsystem codes to their corresponding names
            String mappedSubsystem;
            switch (subsystemLabel.trim()) {
                case "004":
                    mappedSubsystem = SystemConstant.Subsystem.OCMS_NAME;
                    break;
                case "005":
                    mappedSubsystem = SystemConstant.Subsystem.PLUS_NAME;
                    break;
                case "006":
                case "007":
                case "008":
                case "009":
                case "010":
                    mappedSubsystem = SystemConstant.Subsystem.MEV_NAME;
                    break;
                case "020":
                case "021":
                    mappedSubsystem = SystemConstant.Subsystem.REPCCS_ENC_NAME;
                    break;
                case "023":
                    mappedSubsystem = SystemConstant.Subsystem.REPCCS_EHT_NAME;
                    break;
                default:
                    mappedSubsystem = subsystemLabel;
            }

            log.info("Mapped subsystem '{}' to '{}' for sequence retrieval", subsystemLabel, mappedSubsystem);

            // Call sequence service with mapped subsystem to get next notice number
            Long nextVal = sequenceService.getNextNoticeNumber(mappedSubsystem);

            // Format to 5 digits with leading zeros
            return String.format("%05d", nextVal);
        } catch (Exception e) {
            log.error("Error getting next sequence number for subsystem {}: {}", subsystemLabel, e.getMessage());
            throw new RuntimeException("Failed to get next sequence number for subsystem: " + subsystemLabel, e);
        }
    }

    /**
     * Calculate checksum using Modulo 11 with character mapping
     * Always returns an alphabetic character (A-K)
     */
    private String calculateChecksum(String input) {
        // Using Modulo 11 algorithm with weights
        int[] weights = {6, 5, 4, 3, 2, 7, 6, 5, 4};
        int sum = 0;

        for (int i = 0; i < input.length() && i < weights.length; i++) {
            int digit = Character.getNumericValue(input.charAt(i));
            sum += digit * weights[i];
        }

        int remainder = sum % 11;

        // Convert remainder to alphabetic character (A-K)
        // 0 -> A, 1 -> B, 2 -> C, ..., 10 -> K
        char checksumChar = (char) ('A' + remainder);

        return String.valueOf(checksumChar);
    }

}
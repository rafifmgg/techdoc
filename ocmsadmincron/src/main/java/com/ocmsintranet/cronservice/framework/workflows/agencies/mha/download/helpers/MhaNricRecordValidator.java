package com.ocmsintranet.cronservice.framework.workflows.agencies.mha.download.helpers;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Validates and processes MHA NRIC download records.
 * Implements PINK/RED shapes validation and duplicate detection workflow.
 */
@Slf4j
@Component
public class MhaNricRecordValidator {

    /**
     * Data structure for validated record with metadata
     */
    @Data
    public static class ValidatedRecord {
        private Map<String, Object> record;
        private String uin;
        private String uraReferenceNo;
        private String filename;
        private LocalDateTime dateAddressChange;
        private boolean flagTsNro;
        private String tsNroReason;
        private List<String> validationErrors = new ArrayList<>();

        public boolean isValid() {
            return validationErrors.isEmpty();
        }
    }

    /**
     * Validate record length (238 characters)
     * PINK SHAPE: Check total length = 238
     */
    public boolean isValidRecordLength(String line) {
        if (line == null) return false;
        int length = line.length();
        if (length != 238) {
            log.warn("Invalid record length: {} (expected 238)", length);
            return false;
        }
        return true;
    }

    /**
     * Validate primary fields (UIN and URA Reference No)
     * PINK SHAPE: validate primary field (UIN & URA ref no)
     */
    public boolean hasPrimaryFields(Map<String, Object> record) {
        String uin = (String) record.get("uin");
        String uraRefNo = (String) record.get("uraReferenceNo");
        return uin != null && !uin.isEmpty() &&
               uraRefNo != null && !uraRefNo.isEmpty();
    }

    /**
     * Check for missing critical fields
     * PINK SHAPE: Any critical field has missing data?
     * BLUE NOTE: Critical Fields For Missing Data Check
     */
    public boolean hasMissingCriticalFields(Map<String, Object> record) {
        String[] criticalFields = {
            "uin", "name", "dateOfBirth", "addressType",
            // "blockHouseNo",
            "streetName",
            // "floorNo", "unitNo", "buildingName", "postalCode",
            "lifeStatus", "uraReferenceNo", "batchDateTime",
            "lastChangeAddressDate", "timestamp"
        };

        for (String field : criticalFields) {
            Object value = record.get(field);
            if (value == null || (value instanceof String && ((String) value).trim().isEmpty())) {
                log.debug("Missing critical field: {}", field);
                return true;
            }
        }
        return false;
    }

    /**
     * Validate field formats
     * PINK SHAPE: any invalid format?
     * BLUE NOTE: Format Validation Rules
     */
    public boolean hasInvalidFormat(Map<String, Object> record) {
        // UIN format: SXXXXXXXA
        String uin = (String) record.get("uin");
        if (uin != null && !uin.matches("[STFGM]\\d{7}[A-Z]")) {
            log.debug("Invalid UIN format: {}", uin);
            return true;
        }

        // Date Of Birth: YYYYMMDD and < today
        String dob = (String) record.get("dateOfBirth");
        if (!isValidDateBeforeToday(dob)) {
            log.debug("Invalid date of birth: {}", dob);
            return true;
        }

        // MHA_Address_type: IN (A,B,X,C,D,E,F,Q,I)
        String addressType = (String) record.get("addressType");
        if (addressType != null && !Arrays.asList("A","B","X","C","D","E","F","Q","I").contains(addressType)) {
            log.debug("Invalid address type: {}", addressType);
            return true;
        }

        // Life Status: IN (A, D)
        String lifeStatus = (String) record.get("lifeStatus");
        if (lifeStatus != null && !Arrays.asList("A", "D").contains(lifeStatus)) {
            log.debug("Invalid life status: {}", lifeStatus);
            return true;
        }

        // Invalid Address Tag: if NOT NULL then IN (D,M,F,G,I,N,P,S)
        String invalidAddrTag = (String) record.get("invalidAddressTag");
        if (invalidAddrTag != null && !invalidAddrTag.isEmpty()) {
            if (!Arrays.asList("D","M","F","G","I","N","P","S").contains(invalidAddrTag)) {
                log.debug("Invalid address tag value: {}", invalidAddrTag);
                return true;
            }
        }

        // Date of Death: if NOT NULL then YYYYMMDD
        String dod = (String) record.get("dateOfDeath");
        if (dod != null && !dod.isEmpty() && !isValidDate(dod)) {
            log.debug("Invalid date of death: {}", dod);
            return true;
        }

        return false;
    }

    /**
     * Check for invalid symbols in critical fields
     * PINK SHAPE: Any critical field has symbols?
     * BLUE NOTE: Critical Fields for Symbols Check
     */
    public boolean hasCriticalFieldSymbols(Map<String, Object> record) {
        String[] fieldsToCheck = {
            "uin", "dateOfBirth", "addressType", "blockHouseNo", "floorNo",
            "postalCode", "lifeStatus", "uraReferenceNo", "batchDateTime",
            "lastChangeAddressDate", "timestamp"
        };

        Pattern invalidPattern = Pattern.compile("[^a-zA-Z0-9\\s-]");

        for (String field : fieldsToCheck) {
            String value = (String) record.get(field);
            if (value != null && invalidPattern.matcher(value).find()) {
                log.debug("Invalid symbols in field {}: {}", field, value);
                return true;
            }
        }
        return false;
    }

    /**
     * Check for invalid address
     * RED SHAPE: Any invalid address?
     * BLUE NOTE: Invalid Address Conditions
     */
    public boolean hasInvalidAddress(Map<String, Object> record) {
        // Condition 1: Invalid Address Tag has value
        String invalidAddrTag = (String) record.get("invalidAddressTag");
        if (invalidAddrTag != null && !invalidAddrTag.trim().isEmpty()) {
            log.debug("Invalid address: has invalid address tag");
            return true;
        }

        // Condition 2: Street Name IN ("NA", "N.A.", "N.A", "NA.")
        String streetName = (String) record.get("streetName");
        if (streetName != null) {
            String upperStreet = streetName.toUpperCase().trim();
            if (Arrays.asList("NA", "N.A.", "N.A", "NA.").contains(upperStreet)) {
                log.debug("Invalid address: street name is NA");
                return true;
            }
        }

        // Condition 3: Postal code = "000000"
        String postalCode = (String) record.get("postalCode");
        if ("000000".equals(postalCode)) {
            log.debug("Invalid address: postal code is 000000");
            return true;
        }

        // Condition 4: Date Address Change > today
        String dateAddrChangeStr = (String) record.get("lastChangeAddressDate");
        LocalDateTime dateAddrChange = parseDate(dateAddrChangeStr);
        if (dateAddrChange != null && dateAddrChange.toLocalDate().isAfter(LocalDate.now())) {
            log.debug("Invalid address: date address change is in future");
            return true;
        }

        return false;
    }

    /**
     * Check for inconsistent life status and date of death
     * PINK SHAPE: Check life status consistency
     * BLUE NOTE: Life Status Validation Rules
     * - If lifeStatus = 'A' (Alive) but dateOfDeath has value → Inconsistent
     * - If lifeStatus = 'D' (Death) but dateOfDeath is null → Inconsistent
     */
    public boolean hasInconsistentLifeStatus(Map<String, Object> record) {
        String lifeStatus = (String) record.get("lifeStatus");
        String dateOfDeath = (String) record.get("dateOfDeath");

        // Condition 1: Alive but has date of death
        if ("A".equals(lifeStatus) && dateOfDeath != null && !dateOfDeath.trim().isEmpty()) {
            log.debug("Inconsistent life status: lifeStatus='A' (Alive) but dateOfDeath has value");
            return true;
        }

        // Condition 2: Dead but no date of death
        if ("D".equals(lifeStatus) && (dateOfDeath == null || dateOfDeath.trim().isEmpty())) {
            log.debug("Inconsistent life status: lifeStatus='D' (Death) but dateOfDeath is null/empty");
            return true;
        }

        return false;
    }

    /**
     * Handle duplicate UINs with 2-level grouping logic
     * RED SHAPES: Duplicate detection workflow
     *
     * Level 1: Group by UIN
     * Level 2: Group by uraReferenceNo (within each UIN group)
     *
     * Logic:
     * - Different notices (same UIN, different uraReferenceNo) → Keep ALL, flag ALL TS-NRO (suspicious)
     * - True duplicate (same UIN, same uraReferenceNo):
     *   - Different dates → Keep most recent only, NO flag
     *   - Same dates → Keep first, flag TS-NRO
     */
    public List<ValidatedRecord> handleDuplicateUins(List<ValidatedRecord> allRecords) {
        log.info("Starting duplicate UIN detection for {} records", allRecords.size());

        // Level 1: Group by UIN
        Map<String, List<ValidatedRecord>> groupedByUin = allRecords.stream()
            .collect(Collectors.groupingBy(ValidatedRecord::getUin));

        List<ValidatedRecord> goodRecords = new ArrayList<>();
        int duplicateUinGroupCount = 0;
        int trueDuplicateCount = 0;
        int differentNoticesCount = 0;
        int duplicateFlaggedCount = 0;

        for (Map.Entry<String, List<ValidatedRecord>> entry : groupedByUin.entrySet()) {
            String uin = entry.getKey();
            List<ValidatedRecord> records = entry.getValue();

            if (records.size() == 1) {
                // No duplicate for this UIN
                goodRecords.add(records.get(0));
                log.debug("No duplicate for UIN: {}", uin);
            } else {
                // Duplicate UIN detected
                duplicateUinGroupCount++;
                log.warn("Duplicate UIN detected: {} ({} records)", uin, records.size());

                // Level 2: Group by uraReferenceNo within this UIN
                Map<String, List<ValidatedRecord>> groupedByNotice = records.stream()
                    .collect(Collectors.groupingBy(ValidatedRecord::getUraReferenceNo));

                log.info("  UIN {} has {} unique notice(s)", uin, groupedByNotice.size());

                // Check if all records have different notices
                if (groupedByNotice.size() == records.size()) {
                    // All different notices → Suspicious behavior, flag ALL
                    differentNoticesCount++;
                    log.warn("  Suspicious: Same UIN with {} different notices - flagging ALL", records.size());

                    for (ValidatedRecord rec : records) {
                        rec.setFlagTsNro(true);
                        rec.setTsNroReason("Duplicate UIN with different notices");
                        goodRecords.add(rec);
                        duplicateFlaggedCount++;

                        log.info("    Flagged TS-NRO: Notice={}, Date={}",
                            rec.getUraReferenceNo(),
                            rec.getDateAddressChange() != null ? rec.getDateAddressChange().toLocalDate() : "NULL");
                    }
                } else {
                    // Has true duplicates (same UIN + same notice)
                    log.info("  Has true duplicates: processing {} notice group(s)", groupedByNotice.size());

                    for (Map.Entry<String, List<ValidatedRecord>> noticeEntry : groupedByNotice.entrySet()) {
                        String noticeNo = noticeEntry.getKey();
                        List<ValidatedRecord> duplicateRecords = noticeEntry.getValue();

                        if (duplicateRecords.size() == 1) {
                            // This notice appears only once for this UIN
                            ValidatedRecord rec = duplicateRecords.get(0);
                            goodRecords.add(rec);
                            log.debug("    Unique notice {} for UIN {}", noticeNo, uin);
                        } else {
                            // TRUE DUPLICATE: same UIN + same notice
                            trueDuplicateCount++;
                            log.warn("    True duplicate: UIN={}, Notice={}, {} records",
                                uin, noticeNo, duplicateRecords.size());

                            // Select most recent record
                            ValidatedRecord selected = selectMostRecentRecord(uin, noticeNo, duplicateRecords);
                            goodRecords.add(selected);

                            if (selected.isFlagTsNro()) {
                                duplicateFlaggedCount++;
                            }
                        }
                    }
                }
            }
        }

        log.info("Duplicate detection summary:");
        log.info("  - {} duplicate UIN groups found", duplicateUinGroupCount);
        log.info("  - {} groups with different notices (all flagged)", differentNoticesCount);
        log.info("  - {} true duplicate groups (same UIN + same notice)", trueDuplicateCount);
        log.info("  - {} records flagged for TS-NRO", duplicateFlaggedCount);
        log.info("  - Total records retained: {}", goodRecords.size());

        return goodRecords;
    }

    /**
     * Select most recent record from true duplicates (same UIN + same notice)
     * RED SHAPES: Compare Date Address Change field, identify most recent
     *
     * Logic:
     * - If all dates are same or null → Keep first, flag TS-NRO
     * - If dates are different → Keep most recent, NO flag
     *
     * @param uin UIN of the duplicate records
     * @param noticeNo Notice number of the duplicate records
     * @param duplicates List of duplicate records to select from
     * @return Selected record (most recent or first if dates are same)
     */
    private ValidatedRecord selectMostRecentRecord(String uin, String noticeNo,
                                                   List<ValidatedRecord> duplicates) {
        // Log all duplicate records
        log.info("      Analyzing {} duplicate records for UIN={}, Notice={}",
            duplicates.size(), uin, noticeNo);

        for (int i = 0; i < duplicates.size(); i++) {
            ValidatedRecord rec = duplicates.get(i);
            log.info("        Record {}: Date={}",
                i + 1,
                rec.getDateAddressChange() != null ? rec.getDateAddressChange().toLocalDate() : "NULL");
        }

        // Collect unique dates (excluding nulls)
        Set<LocalDateTime> uniqueDates = duplicates.stream()
            .map(ValidatedRecord::getDateAddressChange)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (uniqueDates.isEmpty()) {
            // All dates are null → Keep first, flag TS-NRO
            log.warn("      All Date Address Change values are NULL - selecting first, flagging TS-NRO");
            ValidatedRecord selected = duplicates.get(0);
            selected.setFlagTsNro(true);
            selected.setTsNroReason("True duplicate with missing Date Address Change");
            return selected;
        } else if (uniqueDates.size() == 1) {
            // All non-null dates are identical → Keep first, flag TS-NRO
            LocalDateTime identicalDate = uniqueDates.iterator().next();
            log.warn("      All Date Address Change values are identical ({}) - selecting first, flagging TS-NRO",
                identicalDate.toLocalDate());
            ValidatedRecord selected = duplicates.get(0);
            selected.setFlagTsNro(true);
            selected.setTsNroReason("Duplicate same date: "
                + identicalDate.toLocalDate());
            return selected;
        } else {
            // Different dates → Select most recent, NO flag
            ValidatedRecord mostRecent = duplicates.stream()
                .max(Comparator.comparing(ValidatedRecord::getDateAddressChange,
                    Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(duplicates.get(0));

            log.info("      Selected most recent record with Date Address Change: {} (NOT flagged)",
                mostRecent.getDateAddressChange() != null ?
                    mostRecent.getDateAddressChange().toLocalDate() : "NULL");

            return mostRecent;
        }
    }

    /**
     * DEPRECATED: This method is no longer used.
     *
     * Old logic (v1): Selected ONE record from duplicates based on Date Address Change only.
     *
     * Current logic (v2): Uses 2-level grouping with hybrid approach:
     * - Level 1: Group by UIN
     * - Level 2: Group by uraReferenceNo
     * - Different notices → Keep ALL, flag ALL
     * - True duplicates → Use selectMostRecentRecord() method
     *
     * See handleDuplicateUins() and selectMostRecentRecord() for current implementation.
     *
     * This method is preserved for reference only.
     */
    @Deprecated
    @SuppressWarnings("unused")
    private ValidatedRecord selectRecordFromDuplicates(String uin, List<ValidatedRecord> duplicates) {
        // Check if all have same Date Address Change
        Set<LocalDateTime> uniqueDates = duplicates.stream()
            .map(ValidatedRecord::getDateAddressChange)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (uniqueDates.size() <= 1) {
            // All same date or null → Flag as TS-NRO and return first
            log.warn("Duplicate UIN {} has same Date Address Change, flagging for TS-NRO", uin);
            ValidatedRecord selected = duplicates.get(0);
            selected.setFlagTsNro(true);
            selected.setTsNroReason("Duplicate UIN with identical Date Address Change");
            return selected;
        } else {
            // Different dates → Select most recent
            ValidatedRecord mostRecent = duplicates.stream()
                .max(Comparator.comparing(ValidatedRecord::getDateAddressChange,
                    Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(duplicates.get(0));

            log.info("Selected most recent record for UIN {} with Date Address Change: {}",
                uin, mostRecent.getDateAddressChange());

            return mostRecent;
        }
    }

    // ========== Helper Methods ==========

    private boolean isValidDate(String date) {
        if (date == null || date.length() != 8) return false;
        try {
            LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidDateBeforeToday(String date) {
        if (!isValidDate(date)) return false;
        try {
            LocalDate parsedDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd"));
            return parsedDate.isBefore(LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        try {
            return LocalDateTime.parse(dateStr + "000000",
                DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        } catch (Exception e) {
            log.warn("Could not parse date: {}", dateStr);
            return null;
        }
    }
}

# OCMS 20 - Unclaimed Reminders: Condition & Validation Plan

## Document Information

| Item | Value |
|------|-------|
| Version | 1.0 |
| Date | 2026-01-07 |
| Feature | Unclaimed Reminders & HST Suspension |
| Source | Functional Document v1.1, Backend Code, Flowchart |

---

## 1. Frontend Validations

### 1.1 Unclaimed Reminder Form Fields

| Field | Validation Rule | Error Message | UI Behavior |
|-------|-----------------|---------------|-------------|
| Notice Number | Required, alphanumeric | "Notice Number is required" | Red border, inline error |
| Notice Number | Format: 10 characters | "Invalid Notice Number format" | Red border, inline error |
| Date of Return | Required | "Date of Return is required" | Red border, inline error |
| Date of Return | Cannot be future date | "Date of Return cannot be in the future" | Date picker max = today |
| Date of Return | Cannot be before Date of Letter | "Date of Return must be after Date of Letter" | Inline error |
| Reason of Return | Required, from dropdown | "Please select a Reason of Return" | Red border on dropdown |
| Remarks on Envelope | Optional, max 500 chars | "Remarks cannot exceed 500 characters" | Character counter |
| Suspension Remarks | Optional, max 500 chars | "Remarks cannot exceed 500 characters" | Character counter |
| Period of Suspension | Required if suspension allowed | "Period of Suspension is required" | Auto-populated from DB |

### 1.2 Excel Upload Validations

| Validation | Rule | Error Message |
|------------|------|---------------|
| File Type | Must be .xlsx or .xls | "Please upload a valid Excel file (.xlsx or .xls)" |
| File Size | Max 10MB | "File size cannot exceed 10MB" |
| Template Format | Must match expected columns | "Invalid template format. Please use the correct template." |
| Notice Numbers | All cells must have value | "Row [X]: Notice Number is required" |
| Notice Numbers | No duplicates in file | "Duplicate Notice Number found: [number]" |
| Max Records | Max 100 records per upload | "Maximum 100 records allowed per upload" |

### 1.3 UI State Conditions

| Condition | Field State | UI Display |
|-----------|-------------|------------|
| Suspension Type = PS | Suspension Code = Disabled | Display "PS/XXX" |
| Suspension Type = PS | Period of Suspension = Disabled | Grey out field |
| Suspension Type = PS | Suspension Remarks = Disabled | Grey out field |
| LPS = Court Stage | Suspension Code = Disabled | Display "Court" |
| LPS = Court Stage | Period of Suspension = Disabled | Grey out field |
| LPS = Court Stage | Suspension Remarks = Disabled | Grey out field |
| Suspension Type = TS | Suspension Code = "UNC" | Auto-populate |
| Suspension Type = TS | Period of Suspension = Editable | From Suspension Reason DB |
| Suspension Type = TS | Suspension Remarks = Editable | Allow input |
| No Suspension | Suspension Code = "UNC" | Auto-populate |
| No Suspension | Period of Suspension = Editable | From Suspension Reason DB |
| No Suspension | Suspension Remarks = Editable | Allow input |

### 1.4 Non-Court Stages (Allow TS-UNC)

```
Stages that allow TS-UNC suspension:
- NPA (New Parking Adjudication)
- ROV (Registered Owner Verification)
- ENA (eNotification)
- RD1 (Reminder 1)
- RD2 (Reminder 2)
- RR3 (Reminder 3)
- DN1 (Driver Notice 1)
- DN2 (Driver Notice 2)
- DR3 (Driver Notice 3)
- CFC (Court Filing Check)
- CPC (Court Payment Check)
```

---

## 2. Backend Validations

### 2.1 Check Unclaimed API Validations

| Rule ID | Field | Condition | Action | Error Code |
|---------|-------|-----------|--------|------------|
| VAL-CHK-001 | noticeNumbers | Field is missing | Return error | OCMS-4000 |
| VAL-CHK-002 | noticeNumbers | Array is empty | Return error | OCMS-4001 |
| VAL-CHK-003 | noticeNumbers | Array > 100 items | Return error | OCMS-4000 |
| VAL-CHK-004 | noticeNo (each) | Not found in VON table | Set validationStatus = "NOT_FOUND" | - |
| VAL-CHK-005 | noticeNo (each) | No offender in ONOD | Set validationStatus = "NO_OFFENDER" | - |
| VAL-CHK-006 | noticeNo (each) | Has PS suspension | Set validationStatus = "PERMANENT_SUSPENSION" | - |
| VAL-CHK-007 | noticeNo (each) | In Court stage | Set validationStatus = "COURT_STAGE" | - |
| VAL-CHK-008 | noticeNo (each) | Has active TS-UNC | Set validationStatus = "ALREADY_SUSPENDED" | - |

### 2.2 Submit Unclaimed API Validations

| Rule ID | Field | Condition | Action | Error Code |
|---------|-------|-----------|--------|------------|
| VAL-SUB-001 | request body | Is null or empty | Return error | OCMS-4002 |
| VAL-SUB-002 | noticeNo | Missing in any record | Return error | OCMS-4003 |
| VAL-SUB-003 | reasonOfSuspension | Not equal to "UNC" | Return error | OCMS-4004 |
| VAL-SUB-004 | daysOfRevival | Negative or zero | Return error | OCMS-4006 |
| VAL-SUB-005 | noticeNo | Not found in VON | Skip record, log error | OCMS-5002 |

### 2.3 Business Rule Validations

| Rule ID | Rule Name | Condition | System Action |
|---------|-----------|-----------|---------------|
| BIZ-UNC-001 | PS Check | suspension_type = 'PS' in ocms_suspended_notice | Do NOT apply TS-UNC, disable fields |
| BIZ-UNC-002 | Court Stage Check | processing_stage IN ('CFI', 'CRG', 'CHG', 'CSM', 'CMP', 'CDT', 'CDJ') | Do NOT apply TS-UNC, disable fields |
| BIZ-UNC-003 | Existing TS Check | Active TS suspension exists (date_of_revival IS NULL) | Allow TS-UNC (stacking allowed) |
| BIZ-UNC-004 | Current Offender Check | offender_indicator = 'Y' in ONOD | Process only current offender |
| BIZ-UNC-005 | Revival Period | daysOfRevival not provided | Default to 10 days |
| BIZ-UNC-006 | SR Number | New suspension record | sr_no = MAX(sr_no) + 1 for notice |
| BIZ-UNC-007 | Offender Type Routing | ownerHirerIndicator = 'D' | Update DN table |
| BIZ-UNC-008 | Offender Type Routing | ownerHirerIndicator IN ('O', 'H') | Update RDP table |

---

## 3. Database Conditions

### 3.1 Query Conditions

| Query | Table | WHERE Conditions |
|-------|-------|------------------|
| Get Notice Details | ocms_valid_offence_notice | notice_no = :noticeNo |
| Get Offender | ocms_offence_notice_owner_driver | notice_no = :noticeNo AND offender_indicator = 'Y' |
| Get Max SR No | ocms_suspended_notice | notice_no = :noticeNo |
| Get Active TS-UNC | ocms_suspended_notice | notice_no = :noticeNo AND suspension_type = 'TS' AND reason_of_suspension = 'UNC' AND date_of_revival IS NULL |
| Get PS Suspension | ocms_suspended_notice | notice_no = :noticeNo AND suspension_type = 'PS' AND date_of_revival IS NULL |
| Get RDP Records | ocms_request_driver_particulars | notice_no = :noticeNo |
| Get DN Records | ocms_driver_notice | notice_no = :noticeNo |
| Get UNC Batch Data | ocms_temp_unc_hst_addr | query_reason = 'UNC' AND (report_generated IS NULL OR report_generated = 'N') |

### 3.2 Insert/Update Conditions

| Operation | Table | Conditions/Logic |
|-----------|-------|------------------|
| INSERT Suspension | ocms_suspended_notice | Always insert new record with next sr_no |
| UPDATE VON | ocms_valid_offence_notice | Set suspension_type, epr_date_of_suspension, epr_reason_of_suspension |
| UPDATE RDP | ocms_request_driver_particulars | Only if records exist for notice |
| UPDATE DN | ocms_driver_notice | Only if records exist for notice |
| INSERT NRO Temp | ocms_nro_temp | Always insert for MHA/DataHive query |
| UPDATE Temp Addr | ocms_temp_unc_hst_addr | Set report_generated = 'Y' after report generated |

---

## 4. External API Conditions

### 4.1 MHA Integration

| Condition Type | Condition | Action |
|----------------|-----------|--------|
| Pre-Call | ID Type = 'N' (NRIC) | Include in MHA enquiry file |
| Pre-Call | ID Type != 'N' | Skip MHA, use DataHive only |
| Pre-Call | Query Reason = 'UNC' | Add to ocms_adhoc_nro_queue |
| Response | Result returned | Store in ocms_temp_unc_hst_addr |
| Response | No result | Store with blank address fields |
| Response | Error code returned | Store error_code in temp table |
| Post-Process | For UNC query | Do NOT update ONOD (address check only) |
| Post-Process | For stage change | Update ONOD with new address |

### 4.2 DataHive Integration

| Condition Type | Condition | Action |
|----------------|-----------|--------|
| Pre-Call | ID Type = 'N' (NRIC) | Query personal address |
| Pre-Call | ID Type = 'F' (FIN) | Query FIN address |
| Pre-Call | ID Type = 'U' (UEN) | Query company registered address |
| Pre-Call | Query Reason = 'UNC' | Add to ocms_adhoc_nro_queue |
| Response | Result returned | Store in ocms_temp_unc_hst_addr |
| Response | Company de-registered | Store company_status = 'De-registered' |
| Response | Prison status found | Store prison_status in temp table |
| Response | Comcare status found | Store comcare_status in temp table |
| Post-Process | For UNC query | Do NOT update ONOD (address check only) |

### 4.3 Result Processing Logic

```
IF result is for stage change processing:
    Continue standard flow (update ONOD)
ELSE IF result is for UNC or HST processing:
    Store address in ocms_temp_unc_hst_addr
    Do NOT update ONOD
    Mark for batch report generation
END IF
```

---

## 5. Decision Trees

### 5.1 Check Unclaimed Notice Decision Tree

```
START: Receive noticeNumbers array
│
├─ noticeNumbers missing or empty?
│   └─ YES → Return OCMS-4000/4001 error
│
├─ FOR EACH noticeNo:
│   │
│   ├─ Query VON table
│   │   └─ Not found? → validationStatus = "NOT_FOUND"
│   │
│   ├─ Query ONOD table (offender_indicator = 'Y')
│   │   └─ Not found? → validationStatus = "NO_OFFENDER"
│   │
│   ├─ Check suspension_type in suspended_notice
│   │   └─ = 'PS'? → validationStatus = "PERMANENT_SUSPENSION"
│   │
│   ├─ Check processing_stage
│   │   └─ In Court stages? → validationStatus = "COURT_STAGE"
│   │
│   ├─ Check existing TS-UNC
│   │   └─ Active TS-UNC exists? → validationStatus = "ALREADY_SUSPENDED"
│   │
│   └─ All checks passed → validationStatus = "OK"
│
END: Return results array
```

### 5.2 Submit Unclaimed Decision Tree

```
START: Receive unclaimed records array
│
├─ Request body empty?
│   └─ YES → Return OCMS-4002 error
│
├─ Any record missing noticeNo?
│   └─ YES → Return OCMS-4003 error
│
├─ Any record with reasonOfSuspension != 'UNC'?
│   └─ YES → Return OCMS-4004 error
│
├─ FOR EACH record:
│   │
│   ├─ Query VON table
│   │   └─ Not found? → Log error, skip record
│   │
│   ├─ Get next sr_no from suspended_notice
│   │
│   ├─ Calculate due_date_of_revival
│   │   └─ = current_date + daysOfRevival (default 10)
│   │
│   ├─ INSERT into ocms_suspended_notice
│   │   ├─ Success? → Continue
│   │   └─ Failed? → Retry 1x
│   │       └─ Still failed? → Log error, continue
│   │
│   ├─ Check offender type
│   │   ├─ = 'D' (Driver)? → UPDATE ocms_driver_notice
│   │   └─ = 'O' or 'H'? → UPDATE ocms_request_driver_particulars
│   │
│   ├─ INSERT into ocms_nro_temp for MHA/DataHive query
│   │
│   └─ Add to processing results
│
├─ Generate Unclaimed Reminder Report
│   ├─ Success? → Upload to Blob
│   └─ Failed? → Return OCMS-5001 error
│
├─ Upload to Blob Storage
│   ├─ Success? → Get report URL
│   └─ Failed? → Return OCMS-5004 error
│
END: Return response with report URL
```

### 5.3 Page Display Handling Decision Tree

```
START: Portal reads Notice details
│
├─ Query suspension status from VON/Suspended Notice
│
├─ Check suspension_type
│   │
│   ├─ = 'PS' (Permanent Suspension)?
│   │   ├─ Current Suspension field = "PS/XXX"
│   │   ├─ Suspension Code field = DISABLED
│   │   ├─ Period of Suspension field = DISABLED
│   │   └─ Suspension Remarks field = DISABLED
│   │
│   ├─ Check processing_stage (LPS)
│   │   │
│   │   ├─ In Court stage (after CPC)?
│   │   │   ├─ Current Suspension field = "Court"
│   │   │   ├─ Suspension Code field = DISABLED
│   │   │   ├─ Period of Suspension field = DISABLED
│   │   │   └─ Suspension Remarks field = DISABLED
│   │   │
│   │   └─ In Non-Court stage?
│   │       │
│   │       ├─ = 'TS' (Temporary Suspension)?
│   │       │   ├─ Current Suspension field = "TS/XXX"
│   │       │   ├─ Suspension Code field = "UNC"
│   │       │   ├─ Period of Suspension = From Suspension Reason DB
│   │       │   └─ Suspension Remarks field = EDITABLE
│   │       │
│   │       └─ No suspension?
│   │           ├─ Current Suspension field = "-"
│   │           ├─ Suspension Code field = "UNC"
│   │           ├─ Period of Suspension = From Suspension Reason DB
│   │           └─ Suspension Remarks field = EDITABLE
│
END: Display form with appropriate field states
```

### 5.4 Batch Data Report Generation Decision Tree

```
START: Cron job triggered or manual execution
│
├─ Check ocms_temp_unc_hst_addr for unprocessed UNC results
│   └─ query_reason = 'UNC' AND report_generated != 'Y'
│
├─ No records found?
│   └─ YES → Return "No data to process"
│
├─ Fetch all UNC batch data records
│
├─ Process records for Excel (15 columns)
│
├─ Generate Excel workbook
│   └─ Failed? → Throw RuntimeException
│
├─ Upload to Blob Storage
│   ├─ Success? → Get report URL
│   └─ Failed? → Throw RuntimeException
│
├─ Save report metadata to ocms_unclaimed_report
│
├─ Mark records as processed
│   └─ UPDATE ocms_temp_unc_hst_addr SET report_generated = 'Y'
│
END: Return success with report URL
```

---

## 6. Error Handling Conditions

### 6.1 Retry Logic

| Operation | Max Retries | Retry Condition | On Final Failure |
|-----------|-------------|-----------------|------------------|
| INSERT ocms_suspended_notice | 1 | Any exception | Log error, skip record |
| UPDATE ocms_valid_offence_notice | 1 | Any exception | Log error, continue |
| UPDATE RDP/DN tables | 0 | - | Log error, continue (non-blocking) |
| Upload to Blob | 1 | Upload failed | Throw ReportGenerationException |
| MHA/DataHive query | 0 | - | Handled by scheduled cron retry |

### 6.2 Error Consolidation

| Scenario | Action |
|----------|--------|
| Any record fails during processing | Add to failedRecords list |
| All records fail | Return error response with all failures |
| Some records fail | Return partial success with failedRecords |
| Report generation fails | Return OCMS-5001 error |
| Blob upload fails | Return OCMS-5004 error |

---

## 7. Assumptions Log

| # | Assumption | Impact | Status |
|---|------------|--------|--------|
| A-001 | Table `ocms_nro_temp` is used instead of `ocms_adhoc_nro_queue` as shown in flowchart | Queue table name mismatch | [NEEDS CLARIFICATION] |
| A-002 | VON update (suspension_type, epr_date_of_suspension) is required but not implemented in current code | Missing feature | [NEEDS IMPLEMENTATION] |
| A-003 | Retry logic for failed TS-UNC applications is required but not implemented | Missing feature | [NEEDS IMPLEMENTATION] |
| A-004 | Court stages that block TS-UNC: CFI, CRG, CHG, CSM, CMP, CDT, CDJ | Based on FD Section 2.3 | [CONFIRMED] |
| A-005 | Maximum 100 records per Excel upload | Performance constraint | [ASSUMPTION] |
| A-006 | Default daysOfRevival = 10 if not provided | Based on code implementation | [CONFIRMED] |
| A-007 | Both RDP and DN tables are updated for each notice regardless of offender type | Based on code implementation | [NEEDS OPTIMIZATION] |
| A-008 | Officer ID is hardcoded as "SYSTEM" - should get from security context | Missing feature | [NEEDS IMPLEMENTATION] |
| A-009 | Suspension stacking allowed (can apply TS-UNC even if TS already exists) | Based on FD interpretation | [NEEDS CLARIFICATION] |
| A-010 | Batch Data Report generated only when MHA/DataHive results are available | Based on FD Section 2.5 | [CONFIRMED] |

---

## 8. Validation Rules Summary

### 8.1 By Layer

| Layer | Rule Count | Critical |
|-------|------------|----------|
| Frontend | 15 | 5 |
| API Validation | 8 | 5 |
| Business Logic | 8 | 4 |
| Database | 8 | 3 |
| External API | 10 | 2 |
| **Total** | **49** | **19** |

### 8.2 By Priority

| Priority | Description | Rule IDs |
|----------|-------------|----------|
| P1 - Critical | Must implement, blocks feature | VAL-CHK-001, VAL-SUB-001, BIZ-UNC-001, BIZ-UNC-002 |
| P2 - High | Should implement, affects data integrity | VAL-CHK-004-008, BIZ-UNC-004, BIZ-UNC-006 |
| P3 - Medium | Good to have, improves UX | Frontend validations, BIZ-UNC-005 |
| P4 - Low | Nice to have, edge cases | A-005, A-007 |

---

## Appendix A: Court Stages Reference

| Stage Code | Stage Name | TS-UNC Allowed |
|------------|------------|----------------|
| NPA | New Parking Adjudication | YES |
| ROV | Registered Owner Verification | YES |
| ENA | eNotification | YES |
| RD1 | Reminder 1 | YES |
| RD2 | Reminder 2 | YES |
| RR3 | Reminder 3 | YES |
| DN1 | Driver Notice 1 | YES |
| DN2 | Driver Notice 2 | YES |
| DR3 | Driver Notice 3 | YES |
| CFC | Court Filing Check | YES |
| CPC | Court Payment Check | YES |
| CFI | Court Filing | NO |
| CRG | Court Registration | NO |
| CHG | Court Hearing | NO |
| CSM | Court Summons | NO |
| CMP | Court Mention Plea | NO |
| CDT | Court Date | NO |
| CDJ | Court Judgment | NO |

## Appendix B: Suspension Type Reference

| Type | Code | Description | Can Apply TS-UNC? |
|------|------|-------------|-------------------|
| Permanent Suspension | PS | Long-term suspension | NO |
| Temporary Suspension | TS | Short-term suspension | YES (stacking) |
| No Suspension | - | Notice not suspended | YES |

## Appendix C: Reason for Suspension Reference

| Code | Description | Default Period (Days) |
|------|-------------|----------------------|
| UNC | Unclaimed Letter | 10 |
| HST | House Tenant | 20 |
| INV | Investigation | 30 |
| OTH | Other | 10 |

# Condition Plan - OCMS 15: Manage Change Processing Stage

**Document Information**
- Version: 1.3
- Date: 2026-01-22
- Source: Functional Document v1.2 + Backend Code Analysis + Data Dictionary
- Feature: Manual Change Processing Stage & PLUS Integration

---

## 1. Frontend Validations (OCMS Staff Portal)

### 1.1 Search Form Validations

| Rule ID | Field | Validation Rule | Error Message | When |
|---------|-------|----------------|---------------|------|
| FE-001 | Notice No | Format validation: Alphanumeric, 10 chars including spaces | Invalid Notice no. | On submit |
| FE-002 | ID No | Format validation: Alphanumeric, max 12 chars (NRIC/FIN format) | Invalid ID no. | On submit |
| FE-003 | Vehicle No | Format warning: Local vehicle number format check | Number does not match the local vehicle number format. | On input (warning only, can proceed) |
| FE-004 | Search Criteria | At least one search criterion must be provided | Please enter at least one search criterion | On submit |
| FE-005 | Date of Current Processing Stage | Valid date format: dd/MM/yyyy | Invalid date format | On date picker |

### 1.2 Change Processing Stage Form Validations

| Rule ID | Field | Validation Rule | Error Message | When |
|---------|-------|----------------|---------------|------|
| FE-010 | New Processing Stage | Required field | New Processing Stage is required | On submit |
| FE-011 | Reason of Change | Required field | Reason of Change is required | On submit |
| FE-012 | Remark | Required if Reason of Change = "OTH" (Others) | Remarks are mandatory when reason for change is 'OTH' (Others) | On submit |
| FE-013 | Notice Selection | At least one notice must be selected (checkbox checked) | Please select at least one notice | On click Change Processing Stage button |
| FE-014 | Send for DH/MHA Check | If unchecked, prompt user confirmation | Notice will not go for DH/MHA check. Do you want to proceed? | On submit |

### 1.3 Offender Type Validation (Portal-Side)

| Rule ID | Condition | Allowed New Stages | Validation |
|---------|-----------|-------------------|------------|
| FE-020 | Offender Type = DRIVER | DN1, DN2, DR3 | If new stage not in allowed list → mark as non-changeable |
| FE-021 | Offender Type = OWNER/HIRER/DIRECTOR | ROV, RD1, RD2, RR3 | If new stage not in allowed list → mark as non-changeable |
| FE-022 | Mixed Offender Types | Per-notice validation | Each notice validated individually |

### 1.4 UI State Validations

| Rule ID | State | Validation | Action |
|---------|-------|------------|--------|
| FE-030 | No search results | Display message | "No record found" |
| FE-031 | All notices non-changeable | Block submission | "All selected notices are not eligible for stage change. Please uncheck inapplicable notices." |
| FE-032 | Mixed changeable/non-changeable | Allow submission with warning | "Some notices are not eligible. Please uncheck inapplicable notices or proceed with eligible ones only." |
| FE-033 | Duplicate record warning | Prompt user confirmation | "Notice No. xxx has existing stage change update. Do you want to proceed?" |

---

## 2. Backend Validations (OCMS Backend API)

### 2.1 Request Format Validations

| Rule ID | Field | Validation | Error Code | Error Message |
|---------|-------|------------|------------|---------------|
| BE-001 | items | Cannot be empty/null | OCMS.CPS.INVALID_FORMAT | Items list cannot be empty |
| BE-002 | noticeNo | Required for each item | OCMS.CPS.MISSING_DATA | noticeNo is required |
| BE-003 | notices | Cannot be empty/null (validate API) | OCMS.CPS.INVALID_FORMAT | Notices list cannot be empty |
| BE-004 | newProcessingStage | Required (validate API) | OCMS.CPS.MISSING_DATA | newProcessingStage is required |

### 2.2 Business Logic Validations

| Rule ID | Validation Check | Logic | Error Code | Error Message | Source |
|---------|-----------------|-------|------------|---------------|---------|
| BE-010 | VON Exists | Query ocms_valid_offence_notice by notice_no | OCMS.CPS.NOT_FOUND | VON not found | FD §2.5.1 Step 3 |
| BE-011 | Court Stage Check | Check if current_processing_stage NOT IN Allowed Stages (NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3, CPC, CFC) | OCMS.CPS.COURT_STAGE | Notice is in court stage | FD §2.3.1 Step 5 |
| BE-012 | Duplicate Record Check | Query ocms_change_of_processing by notice_no and date_of_change (today) | OCMS.CPS.DUPLICATE_RECORD | Existing change record found for this notice today | FD §2.5.1 Step 4 |
| BE-013 | Stage Transition Allowed | Validate stage transition using ocms_stage_map | OCMS.CPS.INVALID_TRANSITION | Stage transition not allowed: [current] -> [new] | FD §2.4.2 Step 4-6 |
| BE-014 | Remarks Required | If reason = "OTH", remarks must not be empty | OCMS.CPS.REMARKS_REQUIRED | Remarks are mandatory when reason for change is 'OTH' (Others) | Code: ValidateChangeProcessingStageRequest |
| BE-015 | Offender Type Match | Driver → DN1/DN2/DR3, Owner/Hirer/Director → ROV/RD1/RD2/RR3 | OCMS.CPS.INVALID_STAGE_FOR_OFFENDER | Stage [stage] not allowed for offender type [type] | FD §2.4.2 Step 5 |
| BE-016 | **ENA Stage Blocked** | **newProcessingStage = 'ENA' is not allowed** | **OCMS.CPS.ENA_NOT_ALLOWED** | **ENA stage change is not allowed** | FD §3 Note |

### 2.3 Search Eligibility Checks

| Rule ID | Check | Condition | Result | Reason Code |
|---------|-------|-----------|--------|-------------|
| BE-020 | Court Stage | current_processing_stage NOT IN Reminder Stages (NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3, CPC, CFC) | Ineligible | OCMS.CPS.ELIG.COURT_STAGE |
| BE-021 | PS Blocked | Permanent suspension active | Ineligible | OCMS.CPS.ELIG.PS_BLOCKED |
| BE-021a | TS Blocked | Temporary suspension active | Ineligible | OCMS.CPS.ELIG.TS_BLOCKED |
| BE-022 | Suspended Notice | suspension_status = 'ACTIVE' | Check suspension type | - |
| BE-023 | Offender Type | owner_driver_indicator = 'D' | Eligible for DN stages | - |
| BE-024 | Offender Type | owner_driver_indicator IN ('O', 'H', 'R') | Eligible for ROV/RD stages | - |

### 2.4 Date Range Validations (Report API)

| Rule ID | Field | Validation | Error Code | Error Message |
|---------|-------|------------|------------|---------------|
| BE-030 | startDate, endDate | Both required | INVALID_DATE_RANGE | Start date and end date are required |
| BE-031 | Date Range | endDate >= startDate | INVALID_DATE_RANGE | End date must be after start date |
| BE-032 | Date Range | Days between <= 90 | INVALID_DATE_RANGE | Date range cannot exceed 90 days. Current range: [X] days |

---

## 3. External API Conditions (PLUS Integration)

### 3.1 PLUS Request Validations

| Rule ID | Field | Validation | Error Code | Error Message |
|---------|-------|------------|------------|---------------|
| EXT-001 | noticeNo | Array cannot be empty | OCMS-4000 | Notice numbers are required |
| EXT-002 | lastStageName | Required | OCMS-4000 | Last stage name is required |
| EXT-003 | nextStageName | Required | OCMS-4000 | Next stage name is required |
| EXT-004 | Stage Transition | Validate using stage_map | OCMS-4000 | Stage transition not allowed: [last] -> [next] |
| EXT-005 | Source Code | Must be "005" for PLUS | OCMS-4000 | Invalid source code |

### 3.2 PLUS Business Rules

| Rule ID | Condition | Action | Error Code | Message |
|---------|-----------|--------|------------|---------|
| EXT-010 | All notices in court stage | Reject request | OCMS-4000 | All notices are in court stage |
| EXT-011 | Mixed valid/invalid notices | Process valid, return errors for invalid | OCMS-4000 | Partial success: [X] succeeded, [Y] failed |
| EXT-012 | Stage transition not in stage map | Reject request | OCMS-4000 | Stage transition not allowed |
| EXT-013 | **PLUS requests CFC stage** | **Reject request** | **OCMS-4004** | **CFC not allowed from PLUS source** |
| EXT-014 | offenceType='U' AND nextStage IN (DN1,DN2,DR3,CPC) | Skip notice | - | Unidentified offender cannot change to driver stages |
| EXT-015 | **PLUS requests ENA stage** | **Reject request** | **OCMS-4000** | **ENA not allowed from PLUS source** |

---

## 4. Internal API Conditions (Toppan Cron)

### 4.1 Toppan Request Validations

| Rule ID | Field | Validation | Error Response |
|---------|-------|------------|----------------|
| INT-001 | noticeNumbers | Array cannot be empty | success: false, errors: ["Notice numbers required"] |
| INT-002 | currentStage | Required | success: false, errors: ["Current stage required"] |
| INT-003 | processingDate | Valid datetime format | success: false, errors: ["Invalid date format"] |

### 4.2 Toppan Processing Rules

| Rule ID | Condition | Action | Type |
|---------|-----------|--------|------|
| INT-010 | Manual stage change record exists in ocms_change_of_processing | Skip VON update (already updated manually) | Manual Update |
| INT-011 | No manual stage change record | Update VON processing stage automatically | Automatic Update |
| INT-012 | VON not found | Skip notice, add to errors list | Skipped |
| INT-013 | Stage transition not allowed | Skip notice, add to errors list | Skipped |

---

## 5. Amount Payable Calculation Matrix

### 5.1 Calculation Rules

When processing stage changes, the `amount_payable` field in VON is recalculated based on the stage transition:

| Previous Stage | New Stage | Formula | Description |
|----------------|-----------|---------|-------------|
| ROV, ENA, RD1, RD2 | RR3 | composition + adminFee | Final reminder to owner adds admin fee |
| DN1, DN2 | DR3 | composition + adminFee | Final reminder to driver adds admin fee |
| ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3 | CFC, CPC | composition + surcharge | Court stage adds surcharge |
| CFC, CPC | RR3, DR3 | composition + adminFee | Return from court to final reminder |
| CFC, CPC | ROV, RD1, RD2, DN1, DN2 | composition | Return from court to earlier stage |
| RR3 | ROV, RD1, RD2 | composition | Revert from final to earlier owner stage |
| DR3 | DN1, DN2 | composition | Revert from final to earlier driver stage |
| ROV, ENA, RD1 | RD1, RD2 | composition | Progress within owner stages |
| DN1 | DN2 | composition | Progress within driver stages |
| RD1, RD2, RR3 | DN1, DN2 | composition | Switch from owner to driver |
| DN1, DN2, DR3 | ROV, RD1, RD2 | composition | Switch from driver to owner |

### 5.2 Fee Parameters

| Parameter | Source | Description |
|-----------|--------|-------------|
| adminFee | `ocms_parameter` table (code='ADM', type='AMOUNT') | Administration fee for final reminders |
| surcharge | `ocms_parameter` table (code='SURCHARGE', type='AMOUNT') | Surcharge for court stages |
| composition | `ocms_valid_offence_notice.composition_amount` | Base composition amount |

### 5.3 Calculation Logic

```
IF newStage IN (RR3, DR3) AND previousStage NOT IN (CFC, CPC):
    amountPayable = compositionAmount + adminFee

ELSE IF newStage IN (CFC, CPC):
    amountPayable = compositionAmount + surcharge

ELSE IF previousStage IN (CFC, CPC) AND newStage IN (RR3, DR3):
    amountPayable = compositionAmount + adminFee

ELSE:
    amountPayable = compositionAmount
```

---

## 6. Error Codes Reference

### 6.1 Eligibility Error Codes

| Error Code | Description | User Message |
|------------|-------------|--------------|
| OCMS.CPS.ELIG.NOT_FOUND | Notice not found in VON or ONOD | Notice not found |
| OCMS.CPS.ELIG.ROLE_CONFLICT | Cannot determine offender role | Cannot determine offender role |
| OCMS.CPS.ELIG.NO_STAGE_RULE | Cannot derive next stage from stage map | Cannot derive next stage |
| OCMS.CPS.ELIG.INELIGIBLE_STAGE | Stage not eligible for this offender role | Stage not eligible for offender type |
| OCMS.CPS.ELIG.COURT_STAGE | Notice is at court stage (NOT in Allowed Stages) | Notice is in court stage |
| OCMS.CPS.ELIG.PS_BLOCKED | Permanent suspension active | Notice has permanent suspension |
| OCMS.CPS.ELIG.TS_BLOCKED | Temporary suspension active | Notice has temporary suspension |
| OCMS.CPS.ELIG.EXISTING_CHANGE_TODAY | Duplicate change exists for today | Existing change record found for this notice today |

### 6.2 Validation Error Codes

| Error Code | Description | User Message |
|------------|-------------|--------------|
| OCMS.CPS.INVALID_FORMAT | Request format invalid | Items list cannot be empty |
| OCMS.CPS.MISSING_DATA | Required field missing | noticeNo is required |
| OCMS.CPS.REMARKS_REQUIRED | Remarks required for OTH reason | Remarks are mandatory when reason for change is 'OTH' |
| OCMS.CPS.ENA_NOT_ALLOWED | ENA stage not allowed | ENA stage change is not allowed |
| OCMS.CPS.VALIDATION_ERROR | General validation error | Validation error |

### 6.3 Search Error Codes

| Error Code | Description | User Message |
|------------|-------------|--------------|
| OCMS.CPS.SEARCH.COURT_STAGE | Notice in court stage | Notice is in court stage |
| OCMS.CPS.SEARCH.PS_ACTIVE | Permanent suspension active | Notice has permanent suspension |
| OCMS.CPS.SEARCH.ERROR | Search error | Search error occurred |

### 6.4 PLUS API Error Codes

| Error Code | Description | User Message |
|------------|-------------|--------------|
| OCMS-4000 | Bad request / Invalid data | Invalid request |
| OCMS-4004 | CFC not allowed from PLUS | CFC not allowed from PLUS source |
| NOTICE_NOT_FOUND | Notice not found in VON | Notice not found |
| STAGE_NOT_ELIGIBLE | Stage not eligible | Stage not eligible |
| EXISTING_STAGE_CHANGE | Duplicate stage change | Existing stage change |
| PROCESSING_FAILED | Multiple notices failed | Processing failed |

---

## 7. Decision Trees

### 7.1 Search Notice Eligibility Decision Tree

```
START
  ↓
Is VON found?
  ├─ NO → Return "No record found"
  ├─ YES → Check Court Stage
       ↓
Is in Court Stage (NOT in Reminder Stages)?
  ├─ YES → Mark as INELIGIBLE (reason: COURT_STAGE)
  ├─ NO → Check Suspension Status
       ↓
Is PS/TS Blocked?
  ├─ YES → Mark as INELIGIBLE (reason: PS_BLOCKED or TS_BLOCKED)
  ├─ NO → Mark as ELIGIBLE
       ↓
Return segregated lists (eligible + ineligible)
  ↓
END
```

### 7.2 Validate Processing Stage Decision Tree

```
START
  ↓
Is Reason = "OTH"?
  ├─ YES → Is Remarks provided?
       ├─ NO → Return error (REMARKS_REQUIRED)
       ├─ YES → Continue
  ├─ NO → Continue
       ↓
For each notice:
  ↓
Is VON found?
  ├─ NO → Mark as NON-CHANGEABLE (NOT_FOUND)
  ├─ YES → Is in Court/PS Stage?
       ├─ YES → Mark as NON-CHANGEABLE (COURT_STAGE)
       ├─ NO → Is stage transition allowed?
            ├─ NO → Mark as NON-CHANGEABLE (INVALID_TRANSITION)
            ├─ YES → Is offender type match?
                 ├─ NO → Mark as NON-CHANGEABLE (INVALID_STAGE_FOR_OFFENDER)
                 ├─ YES → Mark as CHANGEABLE
  ↓
Return changeable + non-changeable lists
  ↓
END
```

### 7.3 Submit Change Processing Stage Decision Tree

```
START
  ↓
Is request format valid?
  ├─ NO → Return error (INVALID_FORMAT / MISSING_DATA)
  ├─ YES → For each item:
       ↓
Is VON found?
  ├─ NO → Mark as FAILED (NOT_FOUND)
  ├─ YES → Check duplicate record
       ↓
Duplicate record exists?
  ├─ YES → Is isConfirmation = true?
       ├─ NO → Mark as FAILED (DUPLICATE_RECORD, prompt user)
       ├─ YES → Continue (user confirmed)
  ├─ NO → Continue
       ↓
Is stage transition valid?
  ├─ NO → Mark as FAILED (INVALID_TRANSITION)
  ├─ YES → Process stage change
       ↓
Update VON + Insert change record + Calculate amount payable
  ↓
Mark as SUCCESS
  ↓
Generate report
  ↓
Return batch response (SUCCESS / PARTIAL / FAILED)
  ↓
END
```

### 7.4 Toppan Stage Update Decision Tree

```
START (for each notice)
  ↓
Is VON found?
  ├─ NO → Skip (add to errors)
  ├─ YES → Check manual stage change record
       ↓
Manual record exists today?
  ├─ YES → Skip VON update (count as manual update)
  ├─ NO → Update VON stage automatically (count as automatic update)
       ↓
Is stage transition allowed?
  ├─ NO → Skip (add to errors)
  ├─ YES → Update VON processing stage
       ↓
       Continue to next notice
  ↓
Return summary (total, automatic, manual, skipped, errors)
  ↓
END
```

---

## 8. Validation Sequence

### 8.1 Search API Validation Sequence

1. **Request Validation** (Frontend)
   - At least one search criterion provided (FE-004)
   - Format validation (FE-001, FE-002, FE-003)

2. **Backend Validation**
   - Query VON table
   - Apply eligibility checks (BE-020 to BE-024)
   - Segregate results

3. **Response**
   - Return eligible + ineligible lists

### 8.2 Validate API Validation Sequence

1. **Request Validation**
   - notices array not empty (BE-003)
   - newProcessingStage provided (BE-004)
   - Remarks check if reason = "OTH" (BE-014)

2. **Per-Notice Validation**
   - VON exists check (BE-010)
   - Court stage check (BE-011)
   - Stage transition check (BE-013)
   - Offender type match check (BE-015)

3. **Response**
   - Return changeable + non-changeable lists

### 8.3 Submit API Validation Sequence

1. **Request Format Validation**
   - items array not empty (BE-001)
   - noticeNo provided (BE-002)

2. **Per-Item Processing**
   - VON exists (BE-010)
   - Duplicate record check (BE-012)
   - If duplicate and isConfirmation=false → return warning
   - If duplicate and isConfirmation=true → proceed
   - Stage transition validation (BE-013)

3. **Database Operations**
   - UPDATE ocms_valid_offence_notice (last_processing_stage, last_processing_date)
   - INSERT ocms_change_of_processing (new record)
   - Calculate amount payable (if applicable)
   - Update DH/MHA check flag (if applicable)

4. **Report Generation**
   - Generate Excel report
   - Upload to Azure Blob Storage
   - Return signed URL

5. **Response**
   - Return batch summary + per-notice results

---

## 9. Assumptions Log

| ID | Assumption | Rationale | Status |
|----|------------|-----------|--------|
| A-001 | Court stages = Any stage NOT IN Allowed Stages (NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3, CPC, CFC). Note: CPC and CFC are court stages but still ALLOWED for change. | Per FD: "Court Stages (after CFC and CPC)" | ✅ Confirmed in FD |
| A-002 | Suspension blocking: PS_BLOCKED, TS_BLOCKED | Confirmed from EligibilityService.java | ✅ Confirmed in Code |
| A-003 | Stage map table contains all valid transitions | Required for validation logic | ✅ Confirmed in Code |
| A-004 | Toppan cron runs daily at 00:30 (0030hr) | Cron expression: 0 30 0 * * ? | ✅ Confirmed in Code |
| A-005 | Report files stored in Azure Blob Storage path: reports/change-stage/ | Based on code implementation | ✅ Confirmed in Code |
| A-006 | User ID extracted from X-User-Id header or defaults to "SYSTEM" | Based on code implementation | ✅ Confirmed in Code |
| A-007 | DH/MHA check field name: `mha_dh_check_allow` in ONOD table | Confirmed from Data Dictionary | ✅ Confirmed in DD |
| A-008 | id_no field max length: 12 chars (varchar(12)) | Confirmed from Data Dictionary | ✅ Confirmed in DD |
| A-009 | Table name: `ocms_change_of_processing` | Confirmed from Data Dictionary | ✅ Confirmed in DD |
| A-010 | remarks field max length: 200 chars (varchar(200)) | Confirmed from Data Dictionary | ✅ Confirmed in DD |

---

## 10. Source Constants

| Constant | Value | Description |
|----------|-------|-------------|
| SOURCE_OCMS | "OCMS" | Change from OCMS Staff Portal |
| SOURCE_PLUS | "PLUS" | Change from PLUS Staff Portal |
| SOURCE_AVSS | "AVSS" | Change from AVSS system |
| SOURCE_SYSTEM | "SYSTEM" | Change from system/cron |
| PLUS_CODE | "005" | PLUS subsystem code |
| OCMS_CODE | "004" | OCMS subsystem code |

---

**End of Condition Plan**

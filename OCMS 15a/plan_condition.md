# OCMS 15a - Change Processing Stage Condition Plan

## Document Information

| Field | Value |
|-------|-------|
| Version | 1.0 |
| Date | 2025-01-26 |
| Feature | OCMS 15a - Manual Change Processing Stage |
| Source | Functional Document v1.2, Backend Code (ocmsadminapi) |
| Status | Draft |

---

## 1. Frontend Validations

### 1.1 Search Form Validations

| Field | Rule ID | Validation Rule | Error Message | Type |
|-------|---------|----------------|---------------|------|
| Notice No | FE-SEARCH-001 | Alphanumeric, max 10 characters | "Invalid Notice no." | Format |
| Notice No | FE-SEARCH-002 | If provided, must match pattern `^[A-Z0-9]{10}$` | "Invalid Notice no." | Format |
| ID No | FE-SEARCH-003 | Alphanumeric, max 20 characters | "Invalid ID no." | Format |
| ID No | FE-SEARCH-004 | If provided, must match NRIC/FIN format | "Invalid ID no." | Format |
| Vehicle No | FE-SEARCH-005 | Alphanumeric, max 14 characters | "Invalid Vehicle no." | Format |
| Processing Stage | FE-SEARCH-006 | Must be valid stage code if provided | "Invalid Processing Stage" | Enum |
| Date | FE-SEARCH-007 | Must be valid date format (YYYY-MM-DD) | "Invalid date format" | Format |
| Search Criteria | FE-SEARCH-008 | At least one search criterion is required | "At least one search criterion is required." | Required |

### 1.2 Change Processing Stage Form Validations

| Field | Rule ID | Validation Rule | Error Message | Type |
|-------|---------|----------------|---------------|------|
| Selected Notices | FE-CPS-001 | At least one notice must be selected | "Please select at least one notice." | Required |
| New Processing Stage | FE-CPS-002 | Required field | "New Processing Stage is required." | Required |
| New Processing Stage | FE-CPS-003 | Must be valid stage code from dropdown | "Invalid Processing Stage" | Enum |
| Reason of Change | FE-CPS-004 | Required field | "Reason of Change is required." | Required |
| Reason of Change | FE-CPS-005 | Must be valid reason code (REC/RSN/SUP/OTH/WOD) | "Invalid Reason of Change" | Enum |
| Remarks | FE-CPS-006 | Required if Reason of Change = "OTH" | "Remarks are mandatory when reason for change is 'OTH' (Others)." | Conditional |
| Remarks | FE-CPS-007 | Max 500 characters | "Remarks cannot exceed 500 characters." | Length |
| DH/MHA Checkbox | FE-CPS-008 | Default checked for all selected notices | N/A | Default |

### 1.3 Offender Type vs Stage Validation (Portal)

| Rule ID | Condition | Allowed Stages | Error Message |
|---------|-----------|----------------|---------------|
| FE-CPS-009 | Offender = DRIVER | DN1, DN2, DR3 | "Selected stage is not applicable for Driver notices." |
| FE-CPS-010 | Offender = OWNER/HIRER/DIRECTOR | ROV, RD1, RD2, RR3 | "Selected stage is not applicable for Owner/Hirer/Director notices." |
| FE-CPS-011 | Stage = CFC | Not allowed for selection | "CFC stage is not allowed for manual change." |
| FE-CPS-012 | Stage = ENA | Not allowed for selection | "ENA stage is not allowed for manual change." |
| FE-CPS-013 | Court Stage | Not allowed (Court stages blocked) | "Court stages are not allowed for manual change." |

### 1.4 Report Download Validations

| Field | Rule ID | Validation Rule | Error Message | Type |
|-------|---------|----------------|---------------|------|
| Start Date | FE-RPT-001 | Required field | "Start Date is required." | Required |
| End Date | FE-RPT-002 | Required field | "End Date is required." | Required |
| Date Range | FE-RPT-003 | End Date must be >= Start Date | "End date must be after start date." | Logic |
| Date Range | FE-RPT-004 | Maximum 90 days range | "Date range cannot exceed 90 days." | Range |

### 1.5 UI State Validations

| Rule ID | Condition | UI Behavior |
|---------|-----------|-------------|
| FE-UI-001 | No eligible notices returned | Display "No record found." message |
| FE-UI-002 | Mix of eligible and ineligible notices | Show eligible notices with checkboxes, ineligible notices in read-only section with reason |
| FE-UI-003 | All notices ineligible | Display error message, disable Submit button |
| FE-UI-004 | Form submission in progress | Show loading spinner, disable Submit button |
| FE-UI-005 | Existing record warning received | Show confirmation dialog before proceeding |

---

## 2. Backend Validations

### 2.1 Request Format Validations

| API | Rule ID | Validation Rule | Error Code | Error Message |
|-----|---------|----------------|------------|---------------|
| Search | BE-VAL-001 | At least one search criterion required | OCMS-4000 | "At least one search criterion is required." |
| Validate | BE-VAL-002 | notices array cannot be null or empty | OCMS-4000 | "Notices list cannot be empty." |
| Validate | BE-VAL-003 | newProcessingStage is required | OCMS-4000 | "newProcessingStage is required." |
| Submit | BE-VAL-004 | items array cannot be null or empty | OCMS-4000 | "Items list cannot be empty." |
| Submit | BE-VAL-005 | noticeNo is required for each item | OCMS-4000 | "noticeNo is required." |
| Reports | BE-VAL-006 | startDate and endDate required | OCMS-4000 | "Date range is required." |
| Reports | BE-VAL-007 | Date range cannot exceed 90 days | OCMS-4000 | "Date range cannot exceed 90 days." |
| Reports | BE-VAL-008 | End date must be >= Start date | OCMS-4000 | "End date must be after start date." |
| Download | BE-VAL-009 | reportId cannot be empty | OCMS-4000 | "Report ID cannot be empty." |
| Download | BE-VAL-010 | reportId must not contain invalid characters | OCMS-4000 | "Report ID contains invalid characters." |

### 2.2 PLUS API Validations

| Rule ID | Validation Rule | Error Code | Error Message |
|---------|----------------|------------|---------------|
| BE-PLUS-001 | noticeNo array is required and not empty | OCMS-4000 | "noticeNo is required." |
| BE-PLUS-002 | lastStageName is required and not empty | OCMS-4000 | "lastStageName is required." |
| BE-PLUS-003 | nextStageName is required and not empty | OCMS-4000 | "nextStageName is required." |
| BE-PLUS-004 | offenceType is required (D/O/H/DIR) | OCMS-4000 | "offenceType is required." |
| BE-PLUS-005 | source is required | OCMS-4000 | "source is required." |
| BE-PLUS-006 | Stage change to CFC not allowed from PLUS (source=005) | OCMS-4000 | "Stage change to CFC is not allowed from PLUS source." |
| BE-PLUS-007 | Stage transition must exist in ocms_stage_map | OCMS-4000 | "Stage transition not allowed: {lastStage} -> {nextStage}." |

### 2.3 Toppan API Validations

| Rule ID | Validation Rule | Error Code | Error Message |
|---------|----------------|------------|---------------|
| BE-TOP-001 | noticeNumbers array is required | OCMS-4000 | "noticeNumbers is required." |
| BE-TOP-002 | currentStage is required | OCMS-4000 | "currentStage is required." |
| BE-TOP-003 | processingDate is required | OCMS-4000 | "processingDate is required." |

---

## 3. Business Rules

### 3.1 Eligibility Rules

| Rule ID | Rule Name | Condition | Result | Reference |
|---------|-----------|-----------|--------|-----------|
| BR-ELIG-001 | Notice Exists | Notice not found in VON AND ONOD | NOT CHANGEABLE | §3.1, §3.2 R1 |
| BR-ELIG-002 | Role Determination | Cannot determine offender role from VON/ONOD | NOT CHANGEABLE | §3.2 R2 |
| BR-ELIG-003 | Court Stage Block | Notice is at court stage | NOT CHANGEABLE | §3.2 R7 |
| BR-ELIG-004 | Driver Stage Eligibility | Offender = DRIVER → Allowed stages: DN1, DN2, DR3 | CHANGEABLE if stage matches | §3.2 R4 |
| BR-ELIG-005 | Owner/Hirer/Director Stage Eligibility | Offender = OWNER/HIRER/DIRECTOR → Allowed stages: ROV, RD1, RD2, RR3 | CHANGEABLE if stage matches | §3.2 R5 |
| BR-ELIG-006 | Stage Mismatch | Requested stage not in allowed set for role | NOT CHANGEABLE | §3.2 R6 |
| BR-ELIG-007 | Permanent Suspension Block | Notice has active Permanent Suspension (PS) | NOT CHANGEABLE | §3.2 R8 |
| BR-ELIG-008 | CFC/CPC Block | CFC and CPC stages not allowed for manual change | NOT CHANGEABLE | FD §3 |
| BR-ELIG-009 | ENA Block | ENA stage not allowed for manual change | NOT CHANGEABLE | FD §3 |

### 3.2 Existing Record Check Rules

| Rule ID | Rule Name | Condition | Action | Reference |
|---------|-----------|-----------|--------|-----------|
| BR-DUP-001 | Duplicate Check | Record exists in CPS for same notice, same stage, same date | Return WARNING | §2.5.1 Step 4 |
| BR-DUP-002 | Confirmation Required | Duplicate exists AND isConfirmation = false | Block with warning message | §2.5.1 Step 4 |
| BR-DUP-003 | Confirmed Override | Duplicate exists AND isConfirmation = true | Proceed with update | §2.5.1 Step 4 |

### 3.3 Remarks Requirement Rules

| Rule ID | Rule Name | Condition | Action | Reference |
|---------|-----------|-----------|--------|-----------|
| BR-RMK-001 | Remarks Mandatory for OTH | Reason of Change = "OTH" AND Remarks is empty | Reject with error | FD Form §2.4 |
| BR-RMK-002 | Remarks Optional for Others | Reason of Change != "OTH" | Remarks is optional | FD Form §2.4 |

### 3.4 Stage Transition Rules

| Rule ID | Current Stage | Target Stage | Offender Type | Allowed | Reference |
|---------|---------------|--------------|---------------|---------|-----------|
| BR-STG-001 | NPA/ENA | DN1 | DRIVER | Yes | Stage Map |
| BR-STG-002 | DN1 | DN2 | DRIVER | Yes | Stage Map |
| BR-STG-003 | DN2 | DR3 | DRIVER | Yes | Stage Map |
| BR-STG-004 | NPA | ROV | OWNER/HIRER/DIRECTOR | Yes | Stage Map |
| BR-STG-005 | ROV | RD1 | OWNER/HIRER/DIRECTOR | Yes | Stage Map |
| BR-STG-006 | RD1 | RD2 | OWNER/HIRER/DIRECTOR | Yes | Stage Map |
| BR-STG-007 | RD2 | RR3 | OWNER/HIRER/DIRECTOR | Yes | Stage Map |
| BR-STG-008 | ANY | Court Stage | ANY | No (Court Block) | §3.2 R7 |
| BR-STG-009 | ANY → CFC | ANY (from PLUS) | ANY | No (PLUS Block) | FD §3 |

### 3.5 PLUS-Specific Rules

| Rule ID | Rule Name | Condition | Action | Reference |
|---------|-----------|-----------|--------|-----------|
| BR-PLUS-001 | CFC Block from PLUS | source = "005" AND nextStageName = "CFC" | Reject request | FD §3 |
| BR-PLUS-002 | Skip for U Type | offenceType = "U" AND nextStageName IN (DN1, DN2, DR3, CPC) | Skip processing, return success | Diagram Step 2 |
| BR-PLUS-003 | Stage Map Validation | Stage transition not in ocms_stage_map | Reject with error | Diagram Step 3 |

### 3.6 Toppan-Specific Rules

| Rule ID | Rule Name | Condition | Action | Reference |
|---------|-----------|-----------|--------|-----------|
| BR-TOP-001 | Manual Change Detection | Record exists in CPS with source = OCMS or PLUS | Mark as MANUAL change | §2.5.2 Step 6 |
| BR-TOP-002 | Automatic Change Detection | No record in CPS OR source = SYSTEM | Mark as AUTOMATIC change | §2.5.2 Step 8 |
| BR-TOP-003 | Manual Amount Handling | MANUAL change detected | DO NOT update amount_payable | §2.5.2 Step 7 |
| BR-TOP-004 | Automatic Amount Handling | AUTOMATIC change detected | Calculate and update amount_payable | §2.5.2 Step 8 |
| BR-TOP-005 | Stage Mismatch Check | VON.next_processing_stage != currentStage | Skip notice, log error | §2.5.2 |

---

## 4. Amount Payable Calculation Rules

### 4.1 Calculation Matrix

| Rule ID | Previous Stage | New Stage | Formula | Reference |
|---------|----------------|-----------|---------|-----------|
| BR-AMT-001 | ROV/ENA/RD1/RD2 | RR3 | composition + adminFee | §2.5.1.3 |
| BR-AMT-002 | DN1/DN2 | DR3 | composition + adminFee | §2.5.1.3 |
| BR-AMT-003 | ROV/ENA/RD1/RD2/RR3/DN1/DN2/DR3 | CFC/CPC | composition + surcharge | §2.5.1.3 |
| BR-AMT-004 | CFC/CPC | RR3/DR3 | composition + adminFee | §2.5.1.3 |
| BR-AMT-005 | CFC/CPC | ROV/RD1/RD2/DN1/DN2 | composition (no change) | §2.5.1.3 |
| BR-AMT-006 | RR3 | ROV/RD1/RD2 | composition (no change) | §2.5.1.3 |
| BR-AMT-007 | DR3 | DN1/DN2 | composition (no change) | §2.5.1.3 |
| BR-AMT-008 | ROV/ENA/RD1 | RD1/RD2 | composition (no change) | §2.5.1.3 |
| BR-AMT-009 | DN1 | DN2 | composition (no change) | §2.5.1.3 |
| BR-AMT-010 | RD1/RD2/RR3 | DN1/DN2 | composition (no change) | §2.5.1.3 |
| BR-AMT-011 | DN1/DN2/DR3 | ROV/RD1/RD2 | composition (no change) | §2.5.1.3 |

### 4.2 Parameter Values

| Parameter ID | Code | Description | Source |
|--------------|------|-------------|--------|
| ADMIN_FEE | AMOUNT | Administration fee amount | ocms_parameter |
| SURCHARGE | AMOUNT | Court surcharge amount | ocms_parameter |
| STAGEDAYS | {stage} | Days until next stage | ocms_parameter |
| NEXT_STAGE_{stage} | NEXT_STAGE | Next stage mapping | ocms_parameter |

---

## 5. VON Update Rules

### 5.1 Stage Field Updates

| Rule ID | Field | Old Value Source | New Value | Reference |
|---------|-------|------------------|-----------|-----------|
| BR-VON-001 | prev_processing_stage | last_processing_stage | Copy from last before update | §2.1 Line 469 |
| BR-VON-002 | prev_processing_date | last_processing_date | Copy from last before update | §2.1 Line 470 |
| BR-VON-003 | last_processing_stage | next_processing_stage OR newStage | Update to new stage | §2.1 Line 471 |
| BR-VON-004 | last_processing_date | Current timestamp | NOW() | §2.1 Line 472 |
| BR-VON-005 | next_processing_stage | NEXT_STAGE_{newStage} parameter | Computed from parameter | §2.1 Line 473 |
| BR-VON-006 | next_processing_date | NOW() + STAGEDAYS | Computed from parameter | §2.1 Line 479 |
| BR-VON-007 | amount_payable | Calculated value | Based on calculation matrix | §2.5.1.3 |
| BR-VON-008 | payment_acceptance_allowed | "Y" | Set to Y on stage change | Code |

### 5.2 ONOD Update Rules

| Rule ID | Field | Condition | New Value | Reference |
|---------|-------|-----------|-----------|-----------|
| BR-ONOD-001 | dh_mha_check_allow | dhMhaCheck = true | "Y" | §4.4 |
| BR-ONOD-002 | dh_mha_check_allow | dhMhaCheck = false | "N" | §4.4 |

---

## 6. Decision Trees

### 6.1 Search Eligibility Decision

```
START: Search Notice
  │
  ├─ Query VON/ONOD
  │   │
  │   ├─ Record NOT found?
  │   │   └─ Return: "No record found."
  │   │
  │   └─ Record found
  │       │
  │       ├─ Check Court Stage?
  │       │   ├─ YES → Add to INELIGIBLE list (Reason: "Notice is in court stage")
  │       │   └─ NO → Continue
  │       │
  │       ├─ Check Permanent Suspension (PS)?
  │       │   ├─ YES → Add to INELIGIBLE list (Reason: "Permanent Suspension is active")
  │       │   └─ NO → Continue
  │       │
  │       └─ Add to ELIGIBLE list
  │
END: Return segregated lists
```

### 6.2 Validation Decision

```
START: Validate Notice for Stage Change
  │
  ├─ Check Reason = "OTH" AND Remarks empty?
  │   ├─ YES → Mark as NON-CHANGEABLE (Reason: "Remarks mandatory")
  │   └─ NO → Continue
  │
  ├─ Query VON/ONOD
  │   │
  │   ├─ Not found?
  │   │   └─ Mark as NON-CHANGEABLE (Reason: "Notice not found")
  │   │
  │   └─ Found
  │       │
  │       ├─ Determine Role (DRIVER/OWNER/HIRER/DIRECTOR)
  │       │   ├─ Cannot determine → Mark as NON-CHANGEABLE
  │       │   └─ Role determined → Continue
  │       │
  │       ├─ Check Court Stage?
  │       │   ├─ YES → Mark as NON-CHANGEABLE (Reason: "Court stage")
  │       │   └─ NO → Continue
  │       │
  │       ├─ Get Eligible Stages for Role
  │       │   ├─ DRIVER: [DN1, DN2, DR3]
  │       │   └─ OWNER/HIRER/DIRECTOR: [ROV, RD1, RD2, RR3]
  │       │
  │       └─ Is newStage IN eligible set?
  │           ├─ YES → Mark as CHANGEABLE
  │           └─ NO → Mark as NON-CHANGEABLE (Reason: "Stage not eligible for role")
  │
END: Return changeable/non-changeable lists
```

### 6.3 Submit Decision

```
START: Submit Change Processing Stage
  │
  ├─ Validate Request Format
  │   ├─ Invalid → Return 400 Bad Request
  │   └─ Valid → Continue
  │
  ├─ For Each Notice:
  │   │
  │   ├─ Check Duplicate Record (same notice, same stage, same date)?
  │   │   ├─ YES AND isConfirmation = false → Return WARNING
  │   │   ├─ YES AND isConfirmation = true → Continue
  │   │   └─ NO → Continue
  │   │
  │   ├─ Check Eligibility
  │   │   ├─ NOT CHANGEABLE → Mark as FAILED
  │   │   └─ CHANGEABLE → Continue
  │   │
  │   ├─ Get VON Record
  │   │   ├─ Not found → Mark as FAILED
  │   │   └─ Found → Continue
  │   │
  │   ├─ Calculate Amount Payable
  │   │
  │   ├─ Update VON Stage Fields
  │   │
  │   ├─ Update ONOD DH/MHA Check (if provided)
  │   │
  │   ├─ Insert CPS Audit Record
  │   │
  │   └─ Mark as UPDATED
  │
  ├─ Generate Report (if any success)
  │
  └─ Send Error Notification Email (if any failures)
  │
END: Return batch response with results
```

### 6.4 PLUS Change Stage Decision

```
START: PLUS Change Stage Request
  │
  ├─ STEP 1: Check source = "005" AND nextStage = "CFC"?
  │   ├─ YES → Reject (CFC not allowed from PLUS)
  │   └─ NO → Continue
  │
  ├─ STEP 2: Check offenceType = "U" AND nextStage IN (DN1, DN2, DR3, CPC)?
  │   ├─ YES → Skip processing, return SUCCESS
  │   └─ NO → Continue
  │
  ├─ STEP 3: Query StageMap for lastStage → nextStage
  │   ├─ Not found → Reject (Stage transition not allowed)
  │   └─ Found → Continue
  │
  ├─ STEP 4-5: Validate Parameters (NEXT_STAGE, STAGEDAYS)
  │   ├─ Missing → Log warning, use defaults
  │   └─ Found → Continue
  │
  ├─ For Each Notice:
  │   │
  │   ├─ STEP 6: Get VON Record
  │   │   ├─ Not found → Mark as FAILED
  │   │   └─ Found → Update VON using universal method
  │   │
  │   └─ STEP 7: Insert CPS Audit Record
  │
  ├─ Any Errors?
  │   ├─ YES → Throw exception with all errors
  │   └─ NO → Continue
  │
  └─ STEP 8: Generate Excel Report
  │
END: Return SUCCESS
```

---

## 7. Error Response Mapping

### 7.1 Validation Error to Response Mapping

| Scenario | Error Code | HTTP Status | Response Format |
|----------|------------|-------------|-----------------|
| Empty request body | OCMS-4000 | 400 | `{ "data": { "appCode": "OCMS-4000", "message": "..." } }` |
| Missing mandatory field | OCMS-4000 | 400/422 | `{ "data": { "appCode": "OCMS-4000", "message": "..." } }` |
| Invalid format | OCMS-4000 | 400 | `{ "data": { "appCode": "OCMS-4000", "message": "..." } }` |
| Resource not found | OCMS-4004 | 404 | `{ "data": { "appCode": "OCMS-4004", "message": "..." } }` |
| Business rule violation | OCMS-4000 | 200/422 | Depends on API |
| Internal server error | OCMS-5000 | 500 | `{ "data": { "appCode": "OCMS-5000", "message": "..." } }` |

### 7.2 Eligibility Error Mapping

| Internal Code | Standard Code | Message |
|---------------|---------------|---------|
| OCMS.CPS.ELIG.NOT_FOUND | OCMS-4004 | Notice not found in VON or ONOD |
| OCMS.CPS.ELIG.ROLE_CONFLICT | OCMS-4000 | Cannot determine offender role |
| OCMS.CPS.ELIG.COURT_STAGE | OCMS-4000 | Notice is at court stage |
| OCMS.CPS.ELIG.INELIGIBLE_STAGE | OCMS-4000 | Stage not eligible for role |
| OCMS.CPS.ELIG.PS_BLOCKED | OCMS-4000 | Notice has permanent suspension |
| OCMS.CPS.ELIG.EXISTING_CHANGE_TODAY | OCMS-4000 | Existing change for same day |

---

## 8. Assumptions Log

| ID | Assumption | Category | Impact |
|----|------------|----------|--------|
| [A1] | STAGEDAYS default to 14 days if parameter not found | Parameter | Next processing date calculation |
| [A2] | Role priority: DRIVER > OWNER > HIRER > DIRECTOR | Logic | Role resolution from ONOD |
| [A3] | Default to OWNER role if VON exists but no ONOD | Logic | Role determination |
| [A4] | Permanent Suspension check can be optionally disabled | Config | PS blocking behavior |
| [A5] | Report generation failure does not fail the batch | Non-critical | Report availability |
| [A6] | Error notification email failure does not fail the batch | Non-critical | Email delivery |

---

## Appendix A: Reason of Change Codes

| Code | Description | Remarks Required |
|------|-------------|------------------|
| REC | Recheck with Vault | No |
| RSN | Resend Notice | No |
| SUP | Speed up processing | No |
| OTH | Others | **Yes** |
| WOD | Sent to wrong owner/hirer/driver | No |
| PLS | PLUS system (auto-generated) | No |

## Appendix B: Processing Stage Codes

| Code | Description | Offender Type | Changeable |
|------|-------------|---------------|------------|
| NPA | Notice of Parking Contravention | All | No (Initial) |
| ENA | E-Notification | All | No (Blocked) |
| ROV | Registered Owner/Vehicle | Owner/Hirer/Director | Yes |
| RD1 | Reminder 1 (Owner) | Owner/Hirer/Director | Yes |
| RD2 | Reminder 2 (Owner) | Owner/Hirer/Director | Yes |
| RR3 | Final Reminder (Owner) | Owner/Hirer/Director | Yes |
| DN1 | Driver Notice 1 | Driver | Yes |
| DN2 | Driver Notice 2 | Driver | Yes |
| DR3 | Driver Reminder 3 | Driver | Yes |
| Court Stage | Court stages | All | No (Court) |

## Appendix C: Change Log

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-01-26 | System | Initial version based on FD v1.2 and code |

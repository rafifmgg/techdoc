# Condition Plan: OCMS 15 - Change Processing Stage

## Overview

| Attribute | Value |
| --- | --- |
| Feature Name | Change Processing Stage |
| Version | v1.0 |
| Author | Claude |
| Created Date | 18/01/2026 |
| Last Updated | 18/01/2026 |
| Status | Draft |
| Functional Document | OCMS 15 Functional Document v1.1 |
| Technical Document | OCMS 15 Technical Doc |

---

## 1. Reference Documents

| Document Type | Reference | Description |
| --- | --- | --- |
| Functional Document | OCMS 15 FD v1.1 | Change Processing Stage Functional Document |
| Backend Code | ChangeOfProcessingController.java | Main controller |
| Backend Code | ChangeOfProcessingService.java | Service layer |
| Backend Code | EligibilityService.java | Eligibility validation |
| Data Dictionary | intranet.json | Database schema |

---

## 2. Business Conditions

### 2.1 Eligibility Validation Conditions

**Description:** Conditions to determine if a notice is eligible for processing stage change.

#### Condition Matrix

| Condition ID | Condition Name | Input Fields | Logic | Output/Action |
| --- | --- | --- | --- | --- |
| C001 | Notice Existence | noticeNo | VON and ONOD both not found | NOT_FOUND error |
| C002 | Role Resolution | VON, ONOD | Cannot determine offender role | ROLE_CONFLICT error |
| C003 | Court Stage Check | lastProcessingStage | Stage IN (CRT, CRC, CFI) | COURT_STAGE error |
| C004 | PS Block Check | suspensionType | suspensionType = 'PS' | PS_BLOCKED error (optional) |
| C005 | Stage Eligibility | role, requestedStage | Stage not in eligible set | INELIGIBLE_STAGE error |
| C006 | Stage Rule Check | currentStage, role | Cannot derive next stage | NO_STAGE_RULE error |
| C007 | Duplicate Check | noticeNo, newStage, date | Same change on same day | EXISTING_CHANGE_TODAY warning |
| C008 | Remarks Required | reasonOfChange, remarks | reason=OTH AND remarks empty | REMARKS_REQUIRED error |

#### Condition Details

**C001: Notice Existence Check**

| Attribute | Value |
| --- | --- |
| Description | Check if notice exists in VON or ONOD table |
| Trigger | At the start of eligibility validation |
| Input | noticeNo |
| Logic | Query VON by noticeNo AND Query ONOD by noticeNo |
| Output | Continue validation if at least one found |
| Else | Return NOT_FOUND error |

```
IF VON.noticeNo NOT FOUND AND ONOD.noticeNo NOT FOUND
THEN RETURN {changeable: false, code: "OCMS.CPS.ELIG.NOT_FOUND", message: "Notice not found in VON or ONOD"}
ELSE CONTINUE
```

**C002: Role Resolution**

| Attribute | Value |
| --- | --- |
| Description | Determine offender role from ONOD record |
| Trigger | After notice existence check |
| Input | ONOD.ownerDriverIndicator |
| Logic | Get role from ONOD, normalize to DRIVER/OWNER/HIRER/DIRECTOR |
| Output | Resolved role |
| Else | Default to OWNER if VON exists but no ONOD |

```
IF ONOD.ownerDriverIndicator IS NOT NULL
THEN role = normalizeRole(ONOD.ownerDriverIndicator)
ELSE IF VON EXISTS
THEN role = "OWNER" (default)
ELSE RETURN {changeable: false, code: "OCMS.CPS.ELIG.ROLE_CONFLICT", message: "Cannot determine offender role"}
```

**C003: Court Stage Check**

| Attribute | Value |
| --- | --- |
| Description | Check if notice is at court stage (non-changeable) |
| Trigger | After role resolution |
| Input | VON.lastProcessingStage |
| Logic | Check if stage is in COURT_STAGES set |
| Output | If court stage, reject |
| Else | Continue validation |

```
COURT_STAGES = {"CRT", "CRC", "CFI"}

IF VON.lastProcessingStage IN COURT_STAGES
THEN RETURN {changeable: false, code: "OCMS.CPS.ELIG.COURT_STAGE", message: "Notice is at court stage"}
ELSE CONTINUE
```

**C005: Stage Eligibility by Role**

| Attribute | Value |
| --- | --- |
| Description | Check if requested stage is allowed for offender role |
| Trigger | After court stage check |
| Input | role, requestedStage |
| Logic | Get eligible stages for role, check if requestedStage in set |
| Output | If eligible, proceed |
| Else | Return INELIGIBLE_STAGE error |

```
DRIVER_STAGES = {"DN1", "DN2", "DR3"}
OWNER_HIRER_DIRECTOR_STAGES = {"ROV", "RD1", "RD2", "RR3"}

IF role = "DRIVER"
THEN eligibleStages = DRIVER_STAGES
ELSE eligibleStages = OWNER_HIRER_DIRECTOR_STAGES

IF requestedStage NOT IN eligibleStages
THEN RETURN {changeable: false, code: "OCMS.CPS.ELIG.INELIGIBLE_STAGE", message: "Stage X not eligible for role Y"}
ELSE RETURN {changeable: true}
```

---

### 2.2 PLUS API Validation Conditions

**Description:** Specific validation conditions for PLUS external API.

#### Condition Matrix

| Condition ID | Condition Name | Input Fields | Logic | Output/Action |
| --- | --- | --- | --- | --- |
| P001 | CFC Restriction | source, nextStageName | source=005 AND nextStage=CFC | OCMS-4004 error |
| P002 | Skip Condition | offenceType, nextStageName | type=U AND stage IN (DN1,DN2,DR3,CPC) | Skip processing |
| P003 | Stage Map Validation | lastStageName, nextStageName | Query ocms_stage_map | OCMS-4000 if not found |

#### Condition Details

**P001: CFC Restriction from PLUS**

| Attribute | Value |
| --- | --- |
| Description | PLUS cannot change stage to CFC (Claim for Complaint) |
| Trigger | At start of PLUS API processing |
| Input | source, nextStageName |
| Logic | Check if source=005 (PLUS) AND nextStageName=CFC |
| Output | OCMS-4004 error |
| Else | Continue |

```
IF source = "005" AND nextStageName = "CFC"
THEN RETURN {status: "FAILED", code: "OCMS-4004", message: "Stage change to CFC is not allowed from PLUS source"}
ELSE CONTINUE
```

**P002: Skip Processing Condition**

| Attribute | Value |
| --- | --- |
| Description | Skip processing for certain offence type and stage combinations |
| Trigger | After CFC restriction check |
| Input | offenceType, nextStageName |
| Logic | Check if type=U AND stage IN specific set |
| Output | Return SUCCESS immediately (no processing) |
| Else | Continue processing |

```
SKIP_STAGES = {"DN1", "DN2", "DR3", "CPC"}

IF offenceType = "U" AND nextStageName IN SKIP_STAGES
THEN RETURN {status: "SUCCESS"} // No actual processing
ELSE CONTINUE
```

**P003: Stage Map Validation**

| Attribute | Value |
| --- | --- |
| Description | Validate stage transition is allowed per ocms_stage_map |
| Trigger | After skip condition check |
| Input | lastStageName, nextStageName |
| Logic | Query ocms_stage_map for valid transition |
| Output | Continue if found |
| Else | OCMS-4000 error |

```
stageMapCount = SELECT COUNT(*) FROM ocms_stage_map
                WHERE last_processing_stage = lastStageName
                AND next_processing_stage LIKE '%' + nextStageName + '%'

IF stageMapCount = 0
THEN RETURN {status: "FAILED", code: "OCMS-4000", message: "Stage transition not allowed: X -> Y"}
ELSE CONTINUE
```

---

### 2.3 Toppan Cron Conditions

**Description:** Conditions for Toppan cron processing to differentiate manual vs automatic changes.

#### Condition Matrix

| Condition ID | Condition Name | Input Fields | Logic | Output/Action |
| --- | --- | --- | --- | --- |
| T001 | Manual Change Detection | noticeNo, date, newStage | Check ocms_change_of_processing | isManual=true/false |
| T002 | Stage Mismatch | currentStage, VON.nextProcessingStage | Stages don't match | Skip notice |

#### Condition Details

**T001: Manual vs Automatic Change Detection**

| Attribute | Value |
| --- | --- |
| Description | Determine if a notice has a manual change record |
| Trigger | For each notice in Toppan batch |
| Input | noticeNo, processingDate, currentStage |
| Logic | Query ocms_change_of_processing for manual sources |
| Output | isManual = true (stage update only) |
| Else | isManual = false (stage + amount_payable update) |

```
MANUAL_SOURCES = {"OCMS", "PLUS"}

changeRecord = SELECT * FROM ocms_change_of_processing
               WHERE notice_no = noticeNo
               AND date_of_change = processingDate
               AND new_processing_stage = currentStage

IF changeRecord EXISTS AND changeRecord.source IN MANUAL_SOURCES
THEN isManual = TRUE  // Update stage only, DO NOT touch amount_payable
ELSE isManual = FALSE // Update stage AND calculate amount_payable
```

---

## 3. Decision Tree

### 3.1 Eligibility Check Decision Flow

```
START
  │
  ├─► Query VON by noticeNo
  │     │
  │     ├─ FOUND ─► Continue
  │     │
  │     └─ NOT FOUND ─► Query ONOD by noticeNo
  │                       │
  │                       ├─ FOUND ─► Continue (VON=null, ONOD exists)
  │                       │
  │                       └─ NOT FOUND ─► RETURN NOT_FOUND ─► END
  │
  ├─► Resolve Role from ONOD
  │     │
  │     ├─ RESOLVED ─► Continue
  │     │
  │     └─ CANNOT RESOLVE ─► RETURN ROLE_CONFLICT ─► END
  │
  ├─► Check Court Stage?
  │     │
  │     ├─ YES (CRT/CRC/CFI) ─► RETURN COURT_STAGE ─► END
  │     │
  │     └─ NO ─► Continue
  │
  ├─► Check PS Status? (Optional)
  │     │
  │     ├─ PS Active ─► RETURN PS_BLOCKED (if configured) ─► END
  │     │
  │     └─ No PS ─► Continue
  │
  ├─► Resolve Target Stage
  │     │
  │     ├─ PROVIDED ─► Use requestedStage
  │     │
  │     └─ NOT PROVIDED ─► Derive from StageMap
  │                          │
  │                          ├─ DERIVED ─► Continue
  │                          │
  │                          └─ CANNOT DERIVE ─► RETURN NO_STAGE_RULE ─► END
  │
  ├─► Check Stage Eligibility for Role
  │     │
  │     ├─ ELIGIBLE ─► RETURN CHANGEABLE ─► END
  │     │
  │     └─ NOT ELIGIBLE ─► RETURN INELIGIBLE_STAGE ─► END
  │
END
```

### 3.2 PLUS API Decision Flow

```
START
  │
  ├─► Check: source=005 AND nextStage=CFC?
  │     │
  │     ├─ YES ─► RETURN OCMS-4004 ─► END
  │     │
  │     └─ NO ─► Continue
  │
  ├─► Check: offenceType=U AND nextStage IN (DN1,DN2,DR3,CPC)?
  │     │
  │     ├─ YES ─► RETURN SUCCESS (skip) ─► END
  │     │
  │     └─ NO ─► Continue
  │
  ├─► Query Stage Map Validation
  │     │
  │     ├─ VALID ─► Continue
  │     │
  │     └─ INVALID ─► RETURN OCMS-4000 ─► END
  │
  ├─► Process Each Notice
  │     │
  │     ├─► Update VON using updateVonStage()
  │     │
  │     ├─► Insert Audit Trail to ocms_change_of_processing
  │     │
  │     └─► Next Notice
  │
  ├─► All Success?
  │     │
  │     ├─ YES ─► Generate Report ─► RETURN SUCCESS ─► END
  │     │
  │     └─ NO ─► RETURN PROCESSING_FAILED with errors ─► END
  │
END
```

### Decision Table

| Court Stage | PS Active | Role Resolved | Stage Eligible | Result |
| --- | --- | --- | --- | --- |
| YES | - | - | - | COURT_STAGE error |
| NO | YES | - | - | PS_BLOCKED (optional) |
| NO | NO | NO | - | ROLE_CONFLICT error |
| NO | NO | YES | NO | INELIGIBLE_STAGE error |
| NO | NO | YES | YES | CHANGEABLE |

---

## 4. Validation Rules

### 4.1 Field Validations

| Field Name | Data Type | Required | Validation Rule | Error Code | Error Message |
| --- | --- | --- | --- | --- | --- |
| noticeNo | string | Yes | max 10 chars, not empty | VAL001 | noticeNo is required |
| newProcessingStage | string | Conditional | max 3 chars, in valid stages | VAL002 | Invalid processing stage |
| reasonOfChange | string | No | max 3 chars | VAL003 | Invalid reason code |
| remarks | string | Conditional | max 200 chars, required if reason=OTH | VAL004 | Remarks required for reason OTH |
| idNo | string | No | max 20 chars | VAL005 | Invalid ID number format |
| vehicleNo | string | No | max 14 chars | VAL006 | Invalid vehicle number |
| startDate | date | Yes (reports) | format yyyy-MM-dd | VAL007 | Invalid date format |
| endDate | date | Yes (reports) | format yyyy-MM-dd, >= startDate | VAL008 | End date must be after start date |

### 4.2 Cross-Field Validations

| Validation ID | Fields Involved | Rule | Error Code | Error Message |
| --- | --- | --- | --- | --- |
| XF001 | reasonOfChange, remarks | If reason=OTH then remarks required | OCMS.CPS.REMARKS_REQUIRED | Remarks are mandatory when reason is OTH |
| XF002 | startDate, endDate | endDate - startDate <= 90 days | INVALID_DATE_RANGE | Date range cannot exceed 90 days |
| XF003 | role, newStage | Stage must match role eligibility | OCMS.CPS.ELIG.INELIGIBLE_STAGE | Stage not allowed for role |

### 4.3 Business Rule Validations

| Rule ID | Rule Name | Description | Condition | Error Code | Error Message |
| --- | --- | --- | --- | --- | --- |
| BR001 | Search Criteria Required | At least one search field needed | All search fields empty | VAL010 | At least one search criterion required |
| BR002 | Duplicate Change Warning | Same stage change on same day | Exists in ocms_change_of_processing | OCMS.CPS.ELIG.EXISTING_CHANGE_TODAY | Existing stage change found |
| BR003 | PLUS CFC Restriction | PLUS cannot change to CFC | source=005 AND stage=CFC | OCMS-4004 | CFC not allowed from PLUS |
| BR004 | Court Stage Immutable | Cannot change court stage notices | stage IN (CRT, CRC, CFI) | OCMS.CPS.ELIG.COURT_STAGE | Notice is at court stage |

---

## 5. Status Transitions

### 5.1 Processing Stage Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           DRIVER STAGES                                      │
│                                                                              │
│  ┌──────┐    Toppan    ┌──────┐    Toppan    ┌──────┐                       │
│  │ DN1  │ ───────────► │ DN2  │ ───────────► │ DR3  │                       │
│  └──────┘              └──────┘              └──────┘                       │
│     │                      │                     │                          │
│     │ Manual               │ Manual              │ Manual                   │
│     ▼                      ▼                     ▼                          │
│  [Can change to DN2/DR3] [Can change to DR3]   [End stage]                  │
│                                                                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                        OWNER/HIRER/DIRECTOR STAGES                          │
│                                                                              │
│  ┌──────┐    Toppan    ┌──────┐    Toppan    ┌──────┐    Toppan    ┌──────┐│
│  │ ROV  │ ───────────► │ RD1  │ ───────────► │ RD2  │ ───────────► │ RR3  ││
│  └──────┘              └──────┘              └──────┘              └──────┘│
│     │                      │                     │                     │    │
│     │ Manual               │ Manual              │ Manual              │    │
│     ▼                      ▼                     ▼                     ▼    │
│  [Can change]          [Can change]          [Can change]          [End]   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Stage Transition Matrix

| From Stage | To Stage | Allowed For | Source |
| --- | --- | --- | --- |
| NPA/ENA | DN1 | DRIVER | Toppan/Manual |
| DN1 | DN2 | DRIVER | Toppan/Manual |
| DN2 | DR3 | DRIVER | Toppan/Manual |
| NPA | ROV | OWNER/HIRER/DIRECTOR | Toppan/Manual |
| ROV | RD1 | OWNER/HIRER/DIRECTOR | Toppan/Manual |
| RD1 | RD2 | OWNER/HIRER/DIRECTOR | Toppan/Manual |
| RD2 | RR3 | OWNER/HIRER/DIRECTOR | Toppan/Manual |

### Stage Definitions

| Stage | Code | Description | Allowed Roles |
| --- | --- | --- | --- |
| DN1 | DN1 | Driver Notice 1st Reminder | DRIVER |
| DN2 | DN2 | Driver Notice 2nd Reminder | DRIVER |
| DR3 | DR3 | Driver 3rd Reminder | DRIVER |
| ROV | ROV | Registered Owner Vehicle | OWNER/HIRER/DIRECTOR |
| RD1 | RD1 | Registered Owner 1st Reminder | OWNER/HIRER/DIRECTOR |
| RD2 | RD2 | Registered Owner 2nd Reminder | OWNER/HIRER/DIRECTOR |
| RR3 | RR3 | Registered Owner 3rd Reminder | OWNER/HIRER/DIRECTOR |
| CRT | CRT | Court Stage | (Non-changeable) |
| CRC | CRC | Court Case | (Non-changeable) |
| CFI | CFI | Court Final | (Non-changeable) |
| CFC | CFC | Claim for Complaint | (Not allowed from PLUS) |

---

## 6. Calculation Formulas

### 6.1 Amount Payable Calculation

| Attribute | Value |
| --- | --- |
| Description | Calculate new amount_payable based on stage transition |
| Formula | Based on stage transition rules and composition amount |
| Input Fields | previousStage, newStage, currentAmountPayable |
| Output Field | newAmountPayable |
| Precision | 2 decimal places |

**Logic:**
- For MANUAL changes via Toppan: amount_payable is NOT recalculated (already set during manual submission)
- For AUTOMATIC changes via Toppan: amount_payable IS recalculated based on stage transition

### 6.2 Next Processing Date Calculation

| Attribute | Value |
| --- | --- |
| Description | Calculate next_processing_date from stage days parameter |
| Formula | `next_processing_date = current_datetime + STAGEDAYS` |
| Input Fields | currentStage, current_datetime |
| Output Field | next_processing_date |

```
stageDays = SELECT value FROM ocms_parameter
            WHERE parameter_id = 'STAGEDAYS' AND code = newStage

IF stageDays IS NULL OR stageDays < 0
THEN stageDays = 14 (default)

next_processing_date = NOW() + stageDays days
```

---

## 7. Condition-Action Mapping

### 7.1 Change Processing Stage Flow

| Scenario | Conditions | Actions | Database Updates |
| --- | --- | --- | --- |
| Successful Change | All validations pass | Update VON, Insert audit | VON: stages, dates; CPS: new record |
| Duplicate Warning | Same change exists today, not confirmed | Return warning | None |
| Duplicate Confirmed | Same change exists, isConfirmation=true | Proceed with update | VON: stages, dates; CPS: new record |
| Eligibility Failure | Any eligibility check fails | Return error | None |
| PLUS Skip | offenceType=U AND stage in skip set | Return success | None |
| Toppan Manual | isManual=true | Update VON stage only | VON: stages, dates (NOT amount_payable) |
| Toppan Automatic | isManual=false | Update VON stage + amount | VON: stages, dates, amount_payable |

### Detailed Scenarios

**Scenario 1: Successful Manual Stage Change**

- **Trigger:** User submits change request via OCMS Staff Portal
- **Conditions:**
  - Notice exists in VON/ONOD
  - Role resolved successfully
  - Not at court stage
  - Stage eligible for role
- **Actions:**
  1. Calculate new amount_payable
  2. Update VON processing stages
  3. Update ONOD dh_mha_check_allow (if provided)
  4. Insert audit record to ocms_change_of_processing
  5. Generate Excel report
- **Database Updates:**
  ```
  UPDATE ocms_valid_offence_notice
  SET prev_processing_stage = :oldLastStage,
      prev_processing_date = :oldLastDate,
      last_processing_stage = :newStage,
      last_processing_date = NOW(),
      next_processing_stage = :computedNextStage,
      next_processing_date = NOW() + :stageDays,
      amount_payable = :newAmount
  WHERE notice_no = :noticeNo

  INSERT INTO ocms_change_of_processing (...)
  ```
- **Expected Result:** Stage changed, report generated

---

## 8. Exception Handling

### 8.1 Business Exceptions

| Exception Code | Exception Name | Condition | Handling |
| --- | --- | --- | --- |
| OCMS.CPS.ELIG.NOT_FOUND | Notice Not Found | VON and ONOD both not found | Return 404 with error details |
| OCMS.CPS.ELIG.COURT_STAGE | Court Stage | lastProcessingStage IN (CRT,CRC,CFI) | Return 422 with error details |
| OCMS.CPS.ELIG.INELIGIBLE_STAGE | Stage Not Eligible | Stage not in role's eligible set | Return 422 with error details |
| OCMS.CPS.REMARKS_REQUIRED | Remarks Required | reason=OTH AND remarks empty | Return 400 with error details |
| OCMS-4000 | Stage Transition Invalid | Stage map validation fails | Return 422 with error details |
| OCMS-4004 | CFC Not Allowed | PLUS tries to change to CFC | Return 422 with error details |

### 8.2 System Exceptions

| Exception Code | Exception Name | Condition | Handling |
| --- | --- | --- | --- |
| DB_ERROR | Database Error | DB operation fails | Retry, then log and return 500 |
| BLOB_ERROR | Blob Storage Error | Report upload fails | Log error, return response without reportUrl |
| EMAIL_ERROR | Email Service Error | Notification fails | Log error, continue (non-critical) |

---

## 9. Test Scenarios

### Condition Test Cases

| Test ID | Condition | Test Input | Expected Output | Status |
| --- | --- | --- | --- | --- |
| TC001 | Valid notice search | noticeNo=N-001 | 200 OK with eligible notice | - |
| TC002 | Notice not found | noticeNo=INVALID | 200 OK with empty results | - |
| TC003 | Court stage notice | Notice at CRT | Ineligible with COURT_STAGE reason | - |
| TC004 | PS active notice | Notice with PS | Ineligible with PS_ACTIVE reason | - |
| TC005 | Valid stage change | DRIVER DN1->DN2 | 200 OK, stage updated | - |
| TC006 | Invalid stage for role | DRIVER DN1->RD1 | 422 INELIGIBLE_STAGE | - |
| TC007 | Duplicate same day | Same change twice | Warning on first, success if confirmed | - |
| TC008 | PLUS CFC restriction | source=005, stage=CFC | 422 OCMS-4004 | - |
| TC009 | PLUS skip condition | type=U, stage=DN1 | 200 OK (no processing) | - |
| TC010 | Remarks required | reason=OTH, remarks=null | 400 REMARKS_REQUIRED | - |

### Edge Cases

| Test ID | Scenario | Test Input | Expected Output |
| --- | --- | --- | --- |
| EC001 | Empty search criteria | All fields null | 400 Bad Request |
| EC002 | Date range > 90 days | 100 day range | 400 INVALID_DATE_RANGE |
| EC003 | Batch with mixed results | Some valid, some invalid | 200 with PARTIAL status |
| EC004 | Report not found | Invalid reportId | 404 REPORT_NOT_FOUND |

---

## 10. Changelog

| Version | Date | Author | Changes |
| --- | --- | --- | --- |
| 1.0 | 18/01/2026 | Claude | Initial version |

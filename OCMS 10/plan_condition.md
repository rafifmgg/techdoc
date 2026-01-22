# Condition and Validation Planning Document - OCMS 10: Advisory Notices Processing

## Document Information
- **Version:** 1.2
- **Date:** 2026-01-22
- **Source Documents:**
  - Functional Document: v1.2_OCMS 10_Functional Document.md
  - Backend Code: ura-project-ocmsadminapi-5e962080c0b4
  - Key Files: AdvisoryNoticeHelper.java, CreateNoticeServiceImpl.java, ValidationServices.java
- **Related Documents:**
  - OCMS 3 Technical Document (REPCCS API Specification)
  - OCMS 5 Technical Document (Notice Creation)
  - OCMS 21 Technical Document (Double Booking)

**Change Log:**
- v1.2 (2026-01-22): Aligned with corrected Technical Document. Updated assumptions log with confirmed findings (hardcoded AN messages, actual database tables). Added references to OCMS 3, 5, 21 documents.
- v1.1 (2026-01-15): Updated to align with FD v1.2 - Removed Same-Day AN Check, updated Past Offense logic

---

## 1. Frontend Validations

### 1.1 Create Notice Form (OCMS Staff Portal)

#### 1.1.1 Mandatory Fields Validation

| Field Name | Validation Rule | Error Message |
|------------|----------------|---------------|
| Notice Number | Required, max 10 characters | "Notice number is required" |
| Composition Amount | Required, numeric, > 0 | "Composition amount must be greater than 0" |
| Computer Rule Code | Required, integer | "Offense rule code is required" |
| Notice Date and Time | Required, valid datetime | "Notice date and time is required" |
| Offense Notice Type | Required, exactly 1 character | "Offense notice type is required" |
| PP Code | Required, max 5 characters | "Parking place code is required" |
| PP Name | Required, max 100 characters | "Parking place name is required" |
| Vehicle Category | Required, exactly 1 character | "Vehicle category is required" |
| Vehicle Number | Required, max 14 characters | "Vehicle number is required" |

#### 1.1.2 Field Format Validation

| Field Name | Format Rule | Example | Error Message |
|------------|-------------|---------|---------------|
| Notice Number | Alphanumeric | "PS1234567" | "Invalid notice number format" |
| Composition Amount | Decimal (2 decimal places) | "80.00" | "Invalid amount format" |
| Computer Rule Code | Integer | "20412" | "Invalid rule code format" |
| Vehicle Number | Alphanumeric with special chars | "SBA1234A" | "Invalid vehicle number format" |
| Subsystem Label | 3-8 alphanumeric characters | "030" or "030ABC" | "Subsystem label must be 3-8 characters" |

#### 1.1.3 Rule-Specific UI Behavior

**Trigger:** User selects Offense Rule Code 11210

**UI Changes:**
1. Display drop-down for Rule Remark selection
   - Options: "Handicap Lot", "Loading Bay"
2. Display additional remarks field for free text entry

**Validation:**
- If Rule Code = 11210, Rule Remark must be selected
- Additional remarks field is optional

---

### 1.2 Create Notice Form (PLUS Staff Portal)

Same as Section 1.1, with automatic subsystem label set to PLUS code.

---

### 1.3 CES Webhook Form (Frontend)

[ASSUMPTION] If CES has a frontend submission form:

#### 1.3.1 Subsystem Label Validation

| Field Name | Validation Rule | Error Message |
|------------|----------------|---------------|
| Subsystem Label | Min length: 3, Max length: 8 | "Subsystem label must be 3-8 characters" |
| Subsystem Label | First 3 characters must be numeric | "Subsystem label must start with 3 digits" |
| Subsystem Label | First 3 digits must be 030-999 | "Subsystem label must be in range 030-999" |

---

## 2. Backend Validations

### 2.1 Gating Conditions (Pre-qualification)

#### Offence Notice Type Check
**Rule:** Offence notice type must be 'O' (Offender) for AN eligibility

**Implementation:** `AdvisoryNoticeHelper.checkQualification()`

**Logic:**
```
IF offenceNoticeType != 'O' THEN
  RETURN not qualified
  REASON: "Offense type must be 'O'"
END IF
```

**Location in Code:**
- File: AdvisoryNoticeHelper.java
- Lines: 89-93

**Test Cases:**
- ✓ offenseType = 'O' → Pass
- ✗ offenseType = 'U' → Fail
- ✗ offenseType = NULL → Fail

---

#### Vehicle Type Check
**Rule:** Vehicle registration type must be S, D, V, or I (local vehicles only)

**Implementation:** `AdvisoryNoticeHelper.checkQualification()`

**Logic:**
```
IF vehicleRegType NOT IN ['S', 'D', 'V', 'I'] THEN
  RETURN not qualified
  REASON: "Vehicle type must be S/D/V/I"
END IF
```

**Location in Code:**
- File: AdvisoryNoticeHelper.java
- Lines: 96-99

**Test Cases:**
- ✓ vehicleRegType = 'S' → Pass
- ✓ vehicleRegType = 'D' → Pass
- ✓ vehicleRegType = 'V' → Pass
- ✓ vehicleRegType = 'I' → Pass
- ✗ vehicleRegType = 'F' → Fail (Foreign)
- ✗ vehicleRegType = 'X' → Fail (UPL)

---

### 2.2 Qualification Conditions

#### Same-Day Limit Check
**Rule:** Maximum 1 AN per vehicle per calendar day

**Implementation:** `AdvisoryNoticeHelper.checkSameDayLimit()`

**Logic:**
```
Query ocms_valid_offence_notice WHERE:
  - vehicle_no = current vehicle
  - an_flag = 'Y'
  - notice_date_and_time is same calendar day as currentNoticeDate

IF record exists THEN
  RETURN false (already has AN today - not qualified)
ELSE
  RETURN true (can proceed with AN qualification)
END IF
```

**Location in Code:**
- File: AdvisoryNoticeHelper.java
- Lines: 104-107 (check call), 142-182 (method implementation)

**Test Cases:**
- ✓ Vehicle has no AN today → Pass (can qualify)
- ✗ Vehicle already has AN today → Fail (limit 1 per day)

> **Note:** FD v1.2 states this check should be REMOVED, but it is still actively implemented in code.

---

#### Past Offense Check (24-month window)
**Rule (Actual Code - Simplified Phase 2):** Any past offense in 24 months qualifies for AN

**Implementation:** `AdvisoryNoticeHelper.hasPastQualifyingOffense()`

**Logic (Actual Code Implementation):**
```
Calculate date 24 months ago from current notice date
Query ocms_valid_offence_notice WHERE:
  - vehicle_no = current vehicle
  - notice_date_and_time >= (currentDate - 24 months)
  - notice_date_and_time < currentNoticeDate

IF any record found THEN
  RETURN true (has past offense = qualified for AN)
ELSE
  RETURN false (no past offense = not qualified)
END IF
```

**Location in Code:**
- File: AdvisoryNoticeHelper.java
- Lines: 228-271

**Database Query:**
```sql
SELECT COUNT(*) FROM ocms_valid_offence_notice
WHERE vehicle_no = ?
  AND notice_date_and_time >= ?
  AND notice_date_and_time < ?
```

**Test Cases:**
- ✓ Vehicle has any past offense in 24 months → Qualified
- ✗ Vehicle has no past offense in 24 months → Not Qualified

> **Note:** FD v1.2 requires checking ANS PS reasons (CAN/CFA/DBB/VST), but current code uses simplified logic per code comment: "For Phase 2, we'll keep it simple: any past offense qualifies"

**FD v1.2 Requirement (NOT YET IMPLEMENTED):**
- Past offense qualifies for AN only if all past notices are suspended with ANS PS reasons
- ANS PS reasons: CAN (Cancelled), CFA (Compound Fine Accepted), DBB (Double Booking), VST (Vehicle Stopped)

---

### 2.3 Exemption Rules

#### Rule Code 20412 with $80 Composition
**Rule:** Offense code 20412 with composition amount = $80 is exempt from AN

**Implementation:** `AdvisoryNoticeHelper.isExemptFromAN()`

**Logic:**
```
IF computerRuleCode = 20412 AND compositionAmount = 80.00 THEN
  RETURN exempt (not qualified for AN)
  REASON: "Offense exempt from AN"
END IF
```

**Location in Code:**
- File: AdvisoryNoticeHelper.java
- Lines: 201-207

**Test Cases:**
- ✗ Rule 20412 + $80 → Exempt (not AN)
- ✓ Rule 20412 + $100 → Not exempt
- ✓ Rule 20412 + $50 → Not exempt
- ✓ Rule 20413 + $80 → Not exempt

---

#### Rule Code 11210 with Vehicle Category LB or HL
**Rule:** Offense code 11210 with vehicle category "LB" (Loading Bay) or "HL" (Handicap Lot) is exempt from AN

**Implementation:** `AdvisoryNoticeHelper.isExemptFromAN()`

**Logic:**
```
IF computerRuleCode = 11210 AND vehicleCategory IN ['LB', 'HL'] THEN
  RETURN exempt (not qualified for AN)
  REASON: "Offense exempt from AN"
END IF
```

**Location in Code:**
- File: AdvisoryNoticeHelper.java
- Lines: 210-216

**Test Cases:**
- ✗ Rule 11210 + Category LB → Exempt (not AN)
- ✗ Rule 11210 + Category HL → Exempt (not AN)
- ✓ Rule 11210 + Category A → Not exempt
- ✓ Rule 11211 + Category LB → Not exempt

---

### 2.4 External API Conditions

#### LTA Vehicle Ownership Check
**Condition:** After AN qualification, retrieve vehicle owner information from LTA

**Pre-call Validation:**
- Vehicle number must not be "UNLICENSED_PARKING"
- Vehicle registration type must not be 'I' (military)
- Vehicle registration type must not be 'X' (UPL)

**Call Conditions:**
```
IF vehicleRegType IN ['S', 'D', 'V', 'F'] AND vehicleNo != "UNLICENSED_PARKING" THEN
  CALL LTA API
END IF
```

**Response Handling:**
- **Success:** Store owner details in database
- **Timeout:** Retry 3 times with exponential backoff (1s, 2s, 4s)
- **Owner Not Found:** Mark notice for manual review
- **API Error:** Log error, trigger email alert, mark notice for manual review

**Error Decision (Yi Jie Compliance):**
```
IF LTA returns error THEN
  Log error details
  IF retryCount >= 3 THEN
    Trigger email alert to administrators
  END IF
  Create manual review task
  Continue notice processing with limited owner info
END IF
```

---

#### DataHive Contact Retrieval
**Condition:** After LTA ownership check, retrieve contact information for eNotification

**Pre-call Validation:**
- Owner NRIC/FIN must be available from LTA response
- Notice must NOT be suspended
- Owner must NOT be in eNotification exclusion list

**Call Conditions:**
```
IF ownerNRIC != NULL AND suspensionType = NULL AND notInExclusionList() THEN
  CALL DataHive API
END IF
```

**Response Handling:**
- **Success with Mobile/Email:** Proceed to eNotification flow
- **No Contact Found:** Proceed to physical letter flow
- **Timeout:** Retry 3 times with exponential backoff (1s, 2s, 4s)
- **API Error:** Log error, trigger email alert after 3 retries, fallback to physical letter flow

**Qualification for eNotification:**
```
IF (mobileNumber != NULL OR emailAddress != NULL) AND notSuspended AND notInExclusionList THEN
  Qualify for eAN
ELSE
  Proceed to physical letter
END IF
```

---

#### MHA Address Retrieval
**Condition:** When eNotification is not eligible, retrieve registered address for physical letter

**Pre-call Validation:**
- Owner NRIC/FIN must be available
- eNotification qualification failed

**Call Conditions:**
```
IF eANNotQualified AND ownerNRIC != NULL THEN
  CALL MHA API
END IF
```

**Response Handling:**
- **Success:** Proceed to SLIFT letter generation
- **Timeout:** Retry 3 times with exponential backoff (1s, 2s, 4s)
- **Address Not Found:** Mark notice for manual review
- **API Error:** Retry 3 times, trigger email alert after all retries fail, then manual review

---

#### SLIFT Letter Submission
**Condition:** Submit AN letter for physical printing

**Pre-call Validation:**
- Owner's registered address must be available
- Notice must have AN flag = 'Y'
- Letter template must be generated

**Call Conditions:**
```
IF registeredAddress != NULL AND anFlag = 'Y' THEN
  Generate letter PDF/XML
  SUBMIT to SLIFT/SFTP
END IF
```

**Response Handling:**
- **Success:** Update notice with letter sent status
- **Timeout:** 60 seconds (file transfer)
- **Submission Failed:** Retry 3 times with exponential backoff (1s, 2s, 4s)
- **Persistent Failure:** Trigger email alert, mark for manual review

---

### 2.5 Duplicate Detection

#### Duplicate Notice Number Check
**Rule:** Notice number must be unique in system

**Implementation:** `ValidationServices.checkNoticeExisting()`

**Logic:**
```
Query ocms_valid_offence_notice WHERE notice_no = ?

IF record exists THEN
  RETURN duplicate detected
  ERROR varies by source:
    - REPCCS: OCMS-4000 (HTTP 400)
    - CES: OCMS-2026 (HTTP 226)
ELSE
  RETURN no duplicate
END IF
```

**Location in Code:**
- File: CreateNoticeServiceImpl.java (REPCCS/CES webhooks)
- Lines: 250-259 (REPCCS), 358-366 (CES)

**Error Response - REPCCS Webhook (HTTP 400):**
```json
{
  "data": {
    "appCode": "OCMS-4000",
    "message": "Invalid input format or failed validation"
  }
}
```

**Error Response - CES Webhook (HTTP 226 IM Used):**
```json
{
  "data": {
    "appCode": "OCMS-2026",
    "message": "Notice no Already exists"
  }
}
```

**Test Cases:**
- ✓ New notice number → Pass
- ✗ Existing notice number (REPCCS) → Fail with OCMS-4000
- ✗ Existing notice number (CES) → Fail with OCMS-2026

**Note:** Duplicate notice number check is currently COMMENTED OUT in standard create notice flow (lines 816-847) per user request. Only active in REPCCS/CES webhooks.

---

#### Duplicate Offense Details Check (Double Booking)
**Rule:** Detect duplicate offense for same vehicle, rule code, and date/time

**Reference:** Refer to OCMS 21 Technical Document for detailed Double Booking detection logic.

**Implementation:** `CreateNoticeHelper.checkDuplicateOffenseDetails()`

**Logic:**
```
Query ocms_valid_offence_notice WHERE:
  - vehicle_no = ?
  - computer_rule_code = ?
  - notice_date_and_time = ? (with tolerance)

IF record exists THEN
  RETURN duplicate offense (PS-DBB)
  Create suspended notice with reason "DBB"
ELSE
  RETURN no duplicate
END IF
```

**Location in Code:**
- File: CreateNoticeServiceImpl.java
- Lines: 1065-1253

**Suspension Action:**
- Create PS-DBB suspension record
- Update notice with suspension_type = 'PS'
- Update notice with epr_reason_of_suspension = 'DBB'
- Calculate due_date_of_revival = current date + 30 days (configurable)

**Test Cases:**
- ✓ No matching offense → Pass (no suspension)
- ✗ Matching offense found → Create PS-DBB suspension

**Note:** Duplicate offense check is currently COMMENTED OUT in lines 866-890 per user request. Active check starts at line 1065.

---

### 2.6 Mandatory Field Validation (Backend)

#### Required Fields for Notice Creation
**Rule:** All mandatory fields must be present and valid

**Implementation:** `CreateNoticeHelper.validateMandatoryFieldsForBatch()`

**Mandatory Fields:**
1. `compositionAmount` (not null, > 0)
2. `computerRuleCode` (not null)
3. `creDate` (not null)
4. `creUserId` (not null, non-empty)
5. `lastProcessingDate` (not null)
6. `lastProcessingStage` (not null, non-empty)
7. `noticeDateAndTime` (not null)
8. `offenceNoticeType` (not null, non-empty)
9. `ppCode` (not null, non-empty)
10. `ppName` (not null, non-empty)
11. `vehicleCategory` (not null, non-empty)
12. `vehicleNo` (not null, non-empty)

**Validation Logic:**
```
FOR each DTO in batch DO
  IF any mandatory field is NULL or empty THEN
    Mark DTO as invalid
    Add to failedValidationNotices map
  ELSE
    Add to validDtos list
  END IF
END FOR

IF validDtos is empty THEN
  RETURN error response with all failed notices
END IF
```

**Location in Code:**
- File: CreateNoticeServiceImpl.java
- Lines: 472-503, 562-593

**Error Response (Single Notice):**
```json
{
  "appCode": "OCMS-4000",
  "message": "Missing Mandatory Fields"
}
```

**Error Response (Batch):**
```json
{
  "appCode": "OCMS-4000",
  "message": "Missing Mandatory Fields",
  "failedNotices": ["PS1234567", "PS1234568"]
}
```

---

### 2.7 REPCCS/CES Specific Validations

#### REPCCS Payload Validation
**Rule:** REPCCS webhook payload must have all mandatory fields

**Implementation:** `RepWebHookPayloadValidator.validateMandatoryFields()`

**Validation Steps:**
1. Check for NULL payload
2. Validate mandatory fields present
3. Check notice number not empty
4. Validate offense rule code exists

**Error Response:**
```json
{
  "appCode": "OCMS-4000",
  "message": "Invalid input format or failed validation"
}
```

**Location in Code:**
- File: CreateNoticeServiceImpl.java
- Lines: 212-238

---

#### CES Subsystem Label Validation
**Rule:** CES subsystem label must be 3-8 characters, starting with 030-999

**Implementation:** `CreateNoticeServiceImpl.processEHTWebhook()`

**Validation Steps:**
1. Check subsystemLabel is not NULL or empty
2. Check length is between 3 and 8 characters
3. Extract first 3 digits
4. Validate first 3 characters are numeric
5. Validate numeric value is in range 030-999

**Validation Logic:**
```
IF subsystemLabel = NULL OR subsystemLabel.trim() = "" THEN
  RETURN error "not Exist"
END IF

IF subsystemLabel.length < 3 THEN
  RETURN error "subsystemlabel Length < 3"
END IF

IF subsystemLabel.length > 8 THEN
  RETURN error "subsystemlabel Length > 8"
END IF

prefix = subsystemLabel.substring(0, 3)

IF NOT prefix.matches("\\d{3}") THEN
  RETURN error "Subsystem label must be a 3-digit numeric value"
END IF

value = Integer.parseInt(prefix)

IF value < 30 OR value > 999 THEN
  RETURN error "subsystemlabel Not in Range 030 - 999"
END IF
```

**Location in Code:**
- File: CreateNoticeServiceImpl.java
- Lines: 325-356

**Test Cases:**
- ✓ "030" → Pass
- ✓ "999ABC" → Pass
- ✗ "029" → Fail (< 030)
- ✗ "AB" → Fail (length < 3)
- ✗ "123456789" → Fail (length > 8)
- ✗ "ABC" → Fail (not numeric)

---

#### CES Offense Rule Code Validation
**Rule:** CES offense rule code must be valid

**Implementation:** `ValidationServices.validateToDto()`

**Validation:**
- Check if computer_rule_code exists in offense_rule_code table
- If not found, throw exception

**Error Response (HTTP 226 IM Used):**
```json
{
  "data": {
    "appCode": "OCMS-2026",
    "message": "Invalid input format or failed validation - Invalid Offence Rule Code"
  }
}
```

**Note:** CES webhook returns OCMS-2026 with HTTP 226 for invalid rule code, which differs from other webhooks that use OCMS-4000.

**Location in Code:**
- File: CreateNoticeServiceImpl.java
- Lines: 374-386

---

### 2.8 EHT/Certis AN Flag Handling

#### EHT AN Flag Check
**Rule:** For EHT/Certis subsystems (030-999), if an_flag='Y' in payload, create PS-ANS suspension immediately

**Implementation:** `CreateNoticeHelper.checkANSForEHT()`

**Logic:**
```
Identify subsystem label (first 3 digits)

IF subsystemLabel matches 030-999 THEN
  Check an_flag in payload

  IF an_flag = 'Y' THEN
    Generate notice number
    Create notice records
    Trigger PS-ANS suspension (AutoSuspensionHelper.triggerAdvisoryNotice)
    Process uploaded file
    RETURN early (skip Step 7 and 8)
  ELSE
    Continue to Step 5 (standard notice creation)
  END IF
END IF
```

**Location in Code:**
- File: CreateNoticeServiceImpl.java
- Lines: 936-995

**Suspension Details:**
- `suspension_type` = 'PS'
- `reason_of_suspension` = 'ANS'
- `suspension_source` = 'ocmsiz_app_conn'
- `officer_authorising_suspension` = 'ocmsizmgr_conn'
- `suspension_remarks` = "Advisory Notice detected - subsystem: [label]"

**Test Cases:**
- EHT subsystem (030-999) + an_flag='Y' → Create PS-ANS, early return
- EHT subsystem (030-999) + an_flag='N' → Continue standard flow
- Non-EHT subsystem + an_flag='Y' → Continue to OCMS AN qualification check

---

### 2.9 Database Query Retry Logic

#### Double Booking Query Retry
**Rule:** If DBB query fails, retry 3 times before applying TS-OLD fallback

**Implementation:** `CreateNoticeHelper.checkDuplicateOffenseDetails()` with retry wrapper

**Retry Logic:**
```
maxRetries = 3
retryCount = 0

WHILE retryCount < maxRetries DO
  TRY
    Execute DBB query
    RETURN result
  CATCH exception
    retryCount++
    Log error
    IF retryCount < maxRetries THEN
      Wait exponentialBackoff(retryCount)
    END IF
  END TRY
END WHILE

IF all retries failed THEN
  Set dbbQueryFailed = true
  Set dbbQueryException = exception message
  Trigger email alert (Yi Jie Compliance)
  Apply TS-OLD fallback
END IF
```

**Email Alert on DBB Query Failure (Yi Jie Compliance):**
| Field | Value |
|-------|-------|
| Subject | [OCMS-ALERT] DBB Query Failed - Notice Creation |
| To | System administrators, Support team |
| Body | Notice: {notice_no}<br>Error: {exception_message}<br>Timestamp: {datetime}<br>Retry Count: 3<br>Fallback: TS-OLD applied |

**TS-OLD Fallback Logic:**
```
IF dbbQueryFailed = true THEN
  Log error: "DBB query failed after 3 retries"
  Send email alert to administrators

  Update notice:
    - suspension_type = 'TS'
    - epr_reason_of_suspension = 'OLD'
    - epr_date_of_suspension = currentDate
    - due_date_of_revival = currentDate + 30 days

  Create suspended_notice:
    - suspension_type = 'TS'
    - reason_of_suspension = 'OLD'
    - suspension_remarks = "TS-OLD: DBB query failed after 3 retries - [exception]"

  Process uploaded file
  RETURN early (skip Step 7 and 8)
END IF
```

**Location in Code:**
- File: CreateNoticeServiceImpl.java
- Lines: 1068-1157

**Test Cases:**
- DBB query succeeds on 1st attempt → Continue normally
- DBB query fails 2 times, succeeds on 3rd → Continue normally
- DBB query fails all 3 times → Apply TS-OLD, early return

---

## 3. Decision Trees

### 3.1 AN Qualification Decision Tree (Actual Code Implementation)

> **Note:** This reflects actual code implementation, which differs from FD v1.2 in Steps 3 and 5.

```
START: New Notice Created
│
├─► STEP 1: Check Offence Notice Type
│   ├─► IF offenceNoticeType != 'O'
│   │   └─► Not AN → Standard Processing
│   └─► IF offenceNoticeType = 'O'
│       └─► Continue to STEP 2
│
├─► STEP 2: Check Vehicle Type
│   ├─► IF vehicleRegType NOT IN ['S','D','V','I']
│   │   └─► Not AN → Standard Processing
│   └─► IF vehicleRegType IN ['S','D','V','I']
│       └─► Continue to STEP 3
│
├─► STEP 3: Check Same-Day Limit [STILL ACTIVE in code]
│   ├─► IF vehicle already has AN today
│   │   └─► Not AN → Standard Processing (limit 1 per day)
│   └─► IF no AN today
│       └─► Continue to STEP 4
│
├─► STEP 4: Check Exemption Rules
│   ├─► IF Rule 20412 + $80
│   │   └─► Exempt → Not AN → Standard Processing
│   ├─► IF Rule 11210 + (LB or HL)
│   │   └─► Exempt → Not AN → Standard Processing
│   └─► IF not exempt
│       └─► Continue to STEP 5
│
├─► STEP 5: Check Past Offenses (Simplified Phase 2)
│   ├─► IF any past offense in 24 months
│   │   └─► Qualified for AN
│   └─► IF no past offense in 24 months
│       └─► Not AN → Standard Processing
│
└─► RESULT: Advisory Notice
    ├─► Update an_flag = 'Y'
    ├─► Update payment_acceptance_allowed = 'N'
    └─► Continue to eAN/Letter Processing
```

> **Differences from FD v1.2:**
> - STEP 3: FD says remove same-day check, but code still implements it
> - STEP 5: FD requires ANS PS reason check, but code uses simplified "any past offense qualifies"

---

### 3.2 Notice Creation Flow Decision Tree

```
START: Receive Notice Data
│
├─► SOURCE CHECK
│   ├─► REPCCS Webhook
│   ├─► CES Webhook
│   ├─► Staff Portal
│   ├─► PLUS Portal
│   └─► EHT SFTP
│
├─► VALIDATION
│   ├─► Mandatory Fields Check
│   │   └─► IF missing → Return Error 400
│   └─► IF valid → Continue
│
├─► DUPLICATE CHECK (REPCCS/CES only)
│   ├─► IF duplicate notice number
│   │   └─► Return Error 4000
│   └─► IF not duplicate → Continue
│
├─► VEHICLE TYPE DETECTION
│   ├─► IF vehicleNo contains "MID" or "MINDEF"
│   │   └─► Set vehicleRegType = 'I'
│   ├─► IF vehicleNo = "UNLICENSED_PARKING"
│   │   └─► Set vehicleRegType = 'X'
│   └─► ELSE
│       └─► Use SpecialVehUtils.checkVehregistration()
│
├─► EHT/CERTIS CHECK (if subsystemLabel 030-999)
│   ├─► IF an_flag = 'Y' in payload
│   │   ├─► Generate notice number
│   │   ├─► Create notice records
│   │   ├─► Trigger PS-ANS suspension
│   │   ├─► Process file
│   │   └─► RETURN early
│   └─► IF an_flag = 'N' → Continue
│
├─► NOTICE CREATION
│   ├─► Set vehicle type-specific settings
│   ├─► Generate notice number
│   └─► Create notice records
│
├─► DOUBLE BOOKING CHECK (with retry)
│   ├─► TRY DBB query (max 3 retries)
│   │   ├─► IF duplicate found
│   │   │   ├─► Create PS-DBB suspension
│   │   │   ├─► Process file
│   │   │   └─► RETURN early
│   │   └─► IF no duplicate → Continue
│   └─► IF query fails all retries
│       ├─► Create TS-OLD suspension
│       ├─► Process file
│       └─► RETURN early
│
├─► OCMS AN QUALIFICATION (if offenseType='O' and vehicleType in [S,D,V,I])
│   ├─► Run checkAnQualification()
│   ├─► IF qualified
│   │   ├─► Update an_flag = 'Y'
│   │   └─► Update payment_acceptance_allowed = 'N'
│   └─► IF not qualified → Continue
│
├─► FINAL PROCESSING
│   ├─► IF vehicleRegType = 'I'
│   │   └─► Populate address for military
│   ├─► Process uploaded file
│   └─► RETURN notice number
│
└─► END
```

---

### 3.3 eNotification vs Physical Letter Decision Tree

```
START: AN Qualified
│
├─► STEP 1: Vehicle Ownership Check
│   ├─► IF vehicleRegType = 'I' (Military)
│   │   ├─► Owner = MINDEF
│   │   └─► Skip LTA API → Continue to STEP 2
│   └─► ELSE
│       ├─► CALL LTA API
│       ├─► IF owner not found
│       │   └─► Manual Review Queue
│       └─► IF owner found → Continue to STEP 2
│
├─► STEP 2: Suspension Check
│   ├─► IF notice is suspended
│   │   └─► Do not send notification
│   └─► IF not suspended → Continue to STEP 3
│
├─► STEP 3: eNotification Exclusion Check
│   ├─► IF owner in exclusion list
│   │   └─► Proceed to Physical Letter Flow
│   └─► IF not in exclusion list → Continue to STEP 4
│
├─► STEP 4: Contact Information Retrieval
│   ├─► CALL DataHive API
│   ├─► IF mobile OR email found
│   │   └─► Qualify for eAN → eNotification Flow
│   └─► IF no contact found
│       └─► Proceed to Physical Letter Flow
│
├─► eNotification Flow
│   ├─► Generate eAN content
│   ├─► IF mobile found
│   │   └─► Send SMS via SMS gateway
│   ├─► IF email found
│   │   └─► Send email via email service
│   └─► Log notification sent
│
└─► Physical Letter Flow
    ├─► CALL MHA API for registered address
    ├─► IF address not found
    │   └─► Manual Review Queue
    ├─► IF address found
    │   ├─► Generate letter PDF/XML
    │   ├─► CALL SLIFT/SFTP for printing
    │   └─► Log letter sent
    └─► END
```

---

## 4. Assumptions Log

### Assumptions (Need Confirmation)

[ASSUMPTION] eNotification exclusion list table structure and query method need to be confirmed.

[ASSUMPTION] LTA API authentication uses API key stored in Azure Key Vault (secret name: "lta-api-key").

[ASSUMPTION] DataHive API timeout is 30 seconds with 3 retry attempts and exponential backoff.

[ASSUMPTION] MHA API response includes full postal address in structured format (building, street, postal code, unit).

[ASSUMPTION] DBB query retry logic uses exponential backoff: 1s, 2s, 4s for attempts 1, 2, 3.

[ASSUMPTION] Suspension duration for PS-DBB and TS-OLD is configurable via system parameters table with default 30 days.

[ASSUMPTION] Military vehicle detection uses string contains "MID" or "MINDEF" (case-insensitive) in vehicle number.

[ASSUMPTION] Frontend form validation error messages are displayed inline below each field.

[ASSUMPTION] Staff Portal and PLUS Portal share same validation rules with identical error messages.

### Confirmed Findings (From Code Analysis)

[CONFIRMED] AN SMS/Email messages are currently hardcoded in `NotificationSmsEmailHelper.java` (method `generateAnMessages()`). Code contains TODO comment: "Replace with actual approved template from BA/Product Owner".

[CONFIRMED] SLIFT is used for **encryption** before SFTP upload to Toppan printing vendor, NOT as a direct API call. Reference: `ToppanLettersGeneratorJob.java`.

[CONFIRMED] Email notification records are stored in `ocms_email_notification_records` table.

[CONFIRMED] SMS notification records are stored in `ocms_sms_notification_records` table.

[CONFIRMED] AN Letter records are stored in `ocms_an_letter` table.

[CONFIRMED] Vehicle owner particulars are stored in `ocms_offence_notice_owner_driver` table.

[CONFIRMED] ANS Exemption rules are stored in `ocms_an_exemption_rules` table with fields: `rule_code`, `start_effective_date`, `end_effective_date`, `rule_remarks`, `composition_amount`.

[CONFIRMED] ANS PS Reasons are stored in `ocms_standard_code` table WHERE `reference_code = 'ANS_PS_Reason'` AND `code_status = 'A'`. Valid codes: CAN, CFA, DBB, VST.

---

## 5. Integration Test Scenarios

### 5.1 AN Qualification Flow (FD v1.2)

**Test Case 1: Fully Qualified AN (first offense)**
- Offense Type: O
- Vehicle Type: S
- Not exempt (Rule 20413)
- No past offense in 24 months (first offense for this vehicle)
- **Expected:** an_flag='Y', payment_acceptance_allowed='N'

**Test Case 2: Fully Qualified AN (all past offenses suspended with ANS PS)**
- Offense Type: O
- Vehicle Type: D
- Not exempt
- Has past offenses, ALL suspended with CAN/CFA/DBB/VST
- **Expected:** an_flag='Y', payment_acceptance_allowed='N'

**Test Case 3: Exempt Rule 20412**
- Offense Type: O
- Vehicle Type: V
- Rule Code: 20412, Amount: $80
- **Expected:** Exempt, not AN

**Test Case 4: No Past Offense in 24 Months**
- Offense Type: O
- Vehicle Type: I
- No past offense in 24 months
- **Expected:** Not AN (simplified logic requires past offense)

**Test Case 5: Same-Day Limit Exceeded [STILL ACTIVE in code]**
- Offense Type: O
- Vehicle Type: S
- Vehicle already has AN today
- **Expected:** Not AN (limit 1 per day)

> **Note:** FD v1.2 states same-day limit should be removed, but code still implements this check.

---

### 5.2 EHT/Certis Flow

**Test Case 1: EHT with AN Flag Y**
- Subsystem Label: "030ABC"
- an_flag: 'Y'
- **Expected:** Create PS-ANS, early return

**Test Case 2: EHT with AN Flag N**
- Subsystem Label: "999XYZ"
- an_flag: 'N'
- **Expected:** Continue to OCMS AN qualification

**Test Case 3: Invalid Subsystem Label**
- Subsystem Label: "AB"
- **Expected:** Error 4000, "subsystemlabel Length < 3"

---

### 5.3 DBB Detection Flow

**Test Case 1: Duplicate Offense Detected**
- Vehicle: SBA1234A
- Rule Code: 20412
- Date: 2026-01-09 14:00
- Duplicate exists with same details
- **Expected:** Create PS-DBB suspension

**Test Case 2: DBB Query Fails 3 Times**
- Vehicle: SBA1234A
- DBB query throws SQLException
- Retry 3 times, all fail
- **Expected:** Create TS-OLD suspension, early return

---

### 5.4 External API Error Handling

**Test Case 1: LTA API Timeout**
- Call LTA API
- Response time > 30 seconds
- **Expected:** Retry 3 times, then manual review queue

**Test Case 2: DataHive Returns No Contact**
- Call DataHive API
- Response: mobile=NULL, email=NULL
- **Expected:** Proceed to physical letter flow

**Test Case 3: MHA API Error**
- Call MHA API
- Response: 500 Internal Server Error
- **Expected:** Retry 3 times, then manual review queue

---

## 6. Error Code Summary

| Error Code | HTTP Status | Description | Use Case |
|------------|-------------|-------------|----------|
| OCMS-2000 | 200 | Operation completed successfully | Success response |
| OCMS-2026 | 226 (IM Used) | Notice already exists / Invalid rule code | CES webhook only - duplicate notice, invalid offence rule code |
| OCMS-4000 | 400 | Bad request - Invalid input | Missing fields, validation failure, duplicate notice (REPCCS) |
| OCMS-5000 | 500 | Internal server error | System error |

**Note on Error Code Differences:**
- **REPCCS webhook:** Returns OCMS-4000 (HTTP 400) for all validation errors including duplicate notice
- **CES webhook:** Returns OCMS-2026 (HTTP 226) for duplicate notice and invalid rule code errors; returns OCMS-4000 (HTTP 400) for other validation errors (mandatory fields, subsystem label format)

---

## End of Document

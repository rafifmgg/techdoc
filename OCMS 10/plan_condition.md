# Condition and Validation Planning Document - OCMS 10: Advisory Notices Processing

## Document Information
- **Version:** 1.0
- **Date:** 2026-01-09
- **Source Documents:**
  - Functional Document: v1.1_OCMS 10_Functional Document (1).md
  - Backend Code: ura-project-ocmsadminapi-5e962080c0b4
  - Key Files: AdvisoryNoticeHelper.java, CreateNoticeServiceImpl.java, ValidationServices.java

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

#### BR-AN-GATE-001: Offense Type Check
**Rule:** Offense type must be 'O' (Offender) for AN eligibility

**Implementation:** `AdvisoryNoticeHelper.checkQualification()`

**Logic:**
```
IF offenseType != 'O' THEN
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

#### BR-AN-GATE-002: Vehicle Type Check
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

#### BR-AN-QUAL-001: Same-Day Limit Check
**Rule:** Maximum 1 AN per vehicle per calendar day

**Implementation:** `AdvisoryNoticeHelper.checkSameDayLimit()`

**Logic:**
```
Get start and end of notice date (calendar day)
Query ocms_valid_offence_notice WHERE:
  - vehicle_no = current vehicle
  - an_flag = 'Y'
  - notice_date_and_time BETWEEN dayStart AND dayEnd

IF any records found THEN
  RETURN limit exceeded (not qualified)
  REASON: "Same-day limit exceeded"
ELSE
  RETURN within limit (qualified)
END IF
```

**Location in Code:**
- File: AdvisoryNoticeHelper.java
- Lines: 142-182

**Database Query:**
```sql
SELECT * FROM ocms_valid_offence_notice
WHERE vehicle_no = ?
  AND an_flag = 'Y'
  AND notice_date_and_time >= ?
  AND notice_date_and_time <= ?
```

**Test Cases:**
- ✓ No AN on same day → Pass
- ✗ 1 AN already exists on same day → Fail
- ✗ Multiple ANs on same day → Fail

---

#### BR-AN-QUAL-002: Past Offense Check (24-month window)
**Rule:** Vehicle must have qualifying offense in past 24 months

**Implementation:** `AdvisoryNoticeHelper.hasPastQualifyingOffense()`

**Logic:**
```
Calculate date 24 months ago from current notice date
Query ocms_valid_offence_notice WHERE:
  - vehicle_no = current vehicle
  - notice_date_and_time >= (currentDate - 24 months)
  - notice_date_and_time < currentNoticeDate

IF any records found THEN
  RETURN has past offense (qualified)
ELSE
  RETURN no past offense (not qualified)
  REASON: "No qualifying offense in past 24 months"
END IF
```

**Location in Code:**
- File: AdvisoryNoticeHelper.java
- Lines: 228-271

**Database Query:**
```sql
SELECT * FROM ocms_valid_offence_notice
WHERE vehicle_no = ?
  AND notice_date_and_time >= ?
  AND notice_date_and_time < ?
```

**Test Cases:**
- ✓ Vehicle has 1 past offense within 24 months → Pass
- ✓ Vehicle has multiple past offenses within 24 months → Pass
- ✗ Vehicle has past offense > 24 months ago → Fail
- ✗ Vehicle has no past offenses → Fail

**Business Rule Clarification:**
- Phase 2 simplification: Any past offense qualifies (no PS suspension code check)
- Future phases may add PS suspension code filtering (CAN, CFA, DBB, VST)

---

### 2.3 Exemption Rules

#### BR-AN-EXEMPT-001: Rule Code 20412 with $80 Composition
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

#### BR-AN-EXEMPT-002: Rule Code 11210 with Vehicle Category LB or HL
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

#### OP-AN-LTA-001: LTA Vehicle Ownership Check
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
- **Timeout:** Retry 3 times with exponential backoff
- **Owner Not Found:** Mark notice for manual review
- **API Error:** Log error, mark notice for manual review

**Error Decision:**
```
IF LTA returns error THEN
  Log error details
  Create manual review task
  Continue notice processing with limited owner info
END IF
```

---

#### OP-AN-DATAHIVE-001: DataHive Contact Retrieval
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
- **API Error:** Fallback to physical letter flow

**Qualification for eNotification:**
```
IF (mobileNumber != NULL OR emailAddress != NULL) AND notSuspended AND notInExclusionList THEN
  Qualify for eAN
ELSE
  Proceed to physical letter
END IF
```

---

#### OP-AN-MHA-001: MHA Address Retrieval
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
- **Address Not Found:** Mark notice for manual review
- **API Error:** Retry 3 times, then manual review

---

#### OP-AN-SLIFT-001: SLIFT Letter Submission
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
- **Submission Failed:** Retry 3 times with exponential backoff
- **Persistent Failure:** Mark for manual review

---

### 2.5 Duplicate Detection

#### BR-AN-DUP-001: Duplicate Notice Number Check
**Rule:** Notice number must be unique in system

**Implementation:** `ValidationServices.checkNoticeExisting()`

**Logic:**
```
Query ocms_valid_offence_notice WHERE notice_no = ?

IF record exists THEN
  RETURN duplicate detected
  ERROR: "OCMS-4000: Notice no Already exists"
ELSE
  RETURN no duplicate
END IF
```

**Location in Code:**
- File: CreateNoticeServiceImpl.java (REPCCS/CES webhooks)
- Lines: 250-259 (REPCCS), 358-366 (CES)

**Test Cases:**
- ✓ New notice number → Pass
- ✗ Existing notice number → Fail

**Note:** Duplicate notice number check is currently COMMENTED OUT in standard create notice flow (lines 816-847) per user request. Only active in REPCCS/CES webhooks.

---

#### BR-AN-DUP-002: Duplicate Offense Details Check (Double Booking)
**Rule:** Detect duplicate offense for same vehicle, rule code, and date/time

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

#### BR-AN-MAND-001: Required Fields for Notice Creation
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
  "HTTPStatusCode": "400",
  "HTTPStatusDescription": "Bad Request",
  "data": {
    "appCode": "OCMS-4000",
    "message": "Missing Mandatory Fields"
  }
}
```

**Error Response (Batch):**
```json
{
  "HTTPStatusCode": "200",
  "HTTPStatusDescription": "OK",
  "data": {
    "successfulNotices": [],
    "failedNotices": {
      "PS1234567": "Missing Mandatory Fields",
      "PS1234568": "Missing Mandatory Fields"
    }
  }
}
```

---

### 2.7 REPCCS/CES Specific Validations

#### BR-AN-REP-001: REPCCS Payload Validation
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
  "HTTPStatusCode": "400",
  "HTTPStatusDescription": "Bad Request",
  "data": {
    "appCode": "OCMS-4000",
    "message": "Invalid input format or failed validation"
  }
}
```

**Location in Code:**
- File: CreateNoticeServiceImpl.java
- Lines: 212-238

---

#### BR-AN-CES-001: CES Subsystem Label Validation
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

#### BR-AN-CES-002: CES Offense Rule Code Validation
**Rule:** CES offense rule code must be valid

**Implementation:** `ValidationServices.validateToDto()`

**Validation:**
- Check if computer_rule_code exists in offense_rule_code table
- If not found, throw exception

**Error Response:**
```json
{
  "HTTPStatusCode": "226",
  "HTTPStatusDescription": "IM Used",
  "data": {
    "appCode": "OCMS-2026",
    "message": "Invalid input format or failed validation - Invalid Offence Rule Code"
  }
}
```

**Location in Code:**
- File: CreateNoticeServiceImpl.java
- Lines: 374-386

---

### 2.8 EHT/Certis AN Flag Handling

#### BR-AN-EHT-001: EHT AN Flag Check
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

#### OP-AN-DBB-RETRY: Double Booking Query Retry
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
  Apply TS-OLD fallback
END IF
```

**TS-OLD Fallback Logic:**
```
IF dbbQueryFailed = true THEN
  Log error: "DBB query failed after 3 retries"

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

### 3.1 AN Qualification Decision Tree

```
START: New Notice Created
│
├─► STEP 1: Check Offense Type
│   ├─► IF offenseType != 'O'
│   │   └─► Not AN → Standard Processing
│   └─► IF offenseType = 'O'
│       └─► Continue to STEP 2
│
├─► STEP 2: Check Vehicle Type
│   ├─► IF vehicleRegType NOT IN ['S','D','V','I']
│   │   └─► Not AN → Standard Processing
│   └─► IF vehicleRegType IN ['S','D','V','I']
│       └─► Continue to STEP 3
│
├─► STEP 3: Check Same-Day Limit
│   ├─► IF already has AN today
│   │   └─► Not AN → Standard Processing
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
├─► STEP 5: Check Past Offenses (24 months)
│   ├─► IF no past offense in 24 months
│   │   └─► Not AN → Standard Processing
│   └─► IF has past offense
│       └─► Qualified for AN
│
└─► RESULT: Advisory Notice
    ├─► Update an_flag = 'Y'
    ├─► Update payment_acceptance_allowed = 'N'
    └─► Continue to eAN/Letter Processing
```

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

[ASSUMPTION] eNotification exclusion list table structure and query method need to be confirmed.

[ASSUMPTION] LTA API authentication uses API key stored in Azure Key Vault (secret name: "lta-api-key").

[ASSUMPTION] DataHive API timeout is 30 seconds with 3 retry attempts and exponential backoff.

[ASSUMPTION] MHA API response includes full postal address in structured format (building, street, postal code, unit).

[ASSUMPTION] SLIFT/SFTP file format is PDF for letter submission (not XML).

[ASSUMPTION] DBB query retry logic uses exponential backoff: 1s, 2s, 4s for attempts 1, 2, 3.

[ASSUMPTION] Suspension duration for PS-DBB and TS-OLD is configurable via system parameters table with default 30 days.

[ASSUMPTION] Military vehicle detection uses string contains "MID" or "MINDEF" (case-insensitive) in vehicle number.

[ASSUMPTION] Frontend form validation error messages are displayed inline below each field.

[ASSUMPTION] Staff Portal and PLUS Portal share same validation rules with identical error messages.

---

## 5. Integration Test Scenarios

### 5.1 AN Qualification Flow

**Test Case 1: Fully Qualified AN**
- Offense Type: O
- Vehicle Type: S
- No AN today
- Not exempt (Rule 20413)
- Has past offense in 24 months
- **Expected:** an_flag='Y', payment_acceptance_allowed='N'

**Test Case 2: Fails Same-Day Limit**
- Offense Type: O
- Vehicle Type: D
- Already has AN today
- **Expected:** Not AN, standard processing

**Test Case 3: Exempt Rule 20412**
- Offense Type: O
- Vehicle Type: V
- Rule Code: 20412, Amount: $80
- **Expected:** Exempt, not AN

**Test Case 4: No Past Offense**
- Offense Type: O
- Vehicle Type: I
- No past offense in 24 months
- **Expected:** Not AN, standard processing

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

| Error Code | Description | HTTP Status | Use Case |
|------------|-------------|-------------|----------|
| OCMS-2000 | Operation completed successfully | 200 | Success response |
| OCMS-2026 | Notice number already exists | 226 | Duplicate notice (CES) |
| OCMS-4000 | Bad request - Invalid input | 400 | Missing fields, validation failure |
| OCMS-4001 | Unauthorized access | 401 | Authentication failure |
| OCMS-5000 | Internal server error | 500 | System error |
| OCMS-5001 | Database connection failed | 500 | DB error |

---

## End of Document

# Flowchart Planning Document - OCMS 10: Advisory Notices Processing

## Document Information
- **Version:** 1.1
- **Date:** 2026-01-15
- **Source Documents:**
  - Functional Document: v1.2_OCMS 10_Functional Document.md
  - plan_api.md
  - plan_condition.md
- **Reference Guidelines:** Docs/guidelines/plan_flowchart.md

**Change Log:**
- v1.1 (2026-01-15): Updated to align with FD v1.2 - Removed Same-Day AN Check, updated CES handling

---

## Section 2: High-Level Flow

### Tab 2.1: High-Level AN Processing Overview

#### Process Overview

| Attribute | Value |
|-----------|-------|
| Process Name | High-Level Advisory Notices Processing |
| Section | 2.1 |
| Trigger | Notice creation from multiple sources (REPCCS, CES, Staff Portal, PLUS Portal) |
| Frequency | Real-time |
| Systems Involved | Frontend (Staff Portal), Backend (OCMS API), Database (Intranet/Internet), External Systems (LTA, DataHive, MHA, SLIFT, REPCCS, CES) |

#### Process Steps Table

| Step | Definition | Brief Description |
|------|-----------|-------------------|
| Start | Entry point | Notice data received from external source or manual creation |
| Notice Creation | Create notice record | Validate mandatory fields and create notice in system |
| AN Verification | Check qualification | Verify if notice qualifies as Advisory Notice (exemption rules + past offense check). Note: CES notices with an_flag='Y' skip qualification and go directly to PS-ANS |
| Decision: Qualifies? | AN qualification result | Check if notice passed all AN qualification criteria |
| Update AN Flags | Mark as AN | Set an_flag='Y', payment_acceptance_allowed='N' |
| Retrieve Owner Info | Get vehicle owner | Call LTA API to retrieve vehicle owner particulars |
| Decision: eAN Eligible? | Check contact availability | Determine if eNotification is possible based on contact info |
| Send eNotification | Electronic notification | Send SMS/Email to vehicle owner |
| Send AN Letter | Physical letter | Generate and send physical letter via SLIFT |
| Suspend with PS-ANS | Create suspension | Create PS-ANS suspension record for qualified AN |
| End | Process complete | AN processing completed |

#### Decision Logic

| Decision | Input | Condition | True Action | False Action |
|----------|-------|-----------|-------------|--------------|
| Qualifies as AN? | Verification result | Passes all checks (offense type, vehicle type, exemption rules, past offense with ANS PS reasons). For CES: if an_flag='Y' in payload, skip checks and create PS-ANS | Update AN flags | Process as standard notice |
| eAN Eligible? | Owner contact info | Mobile OR email available AND not in exclusion list AND not suspended | Send eNotification | Send physical letter |

#### Swimlanes Definition

| Swimlane | Color Code | Components |
|----------|-----------|------------|
| Frontend | Blue (#dae8fc) | Staff Portal, PLUS Portal |
| Backend | Purple (#e1d5e7) | OCMS API, Business Logic, Validation Services |
| Database | Yellow (#fff2cc) | Intranet DB, Internet DB |
| External System | Green (#d5e8d4) | REPCCS, CES, LTA, DataHive, MHA, SLIFT |

#### Error Handling

| Error Point | Error Type | Handling | Recovery |
|-------------|-----------|----------|----------|
| Notice Creation | Validation failure | Return error response OCMS-4000 | User corrects input |
| LTA API Call | Timeout/Error | Retry 3 times, then manual review queue | Staff manual verification |
| DataHive API Call | No contact found | Proceed to physical letter flow | AN letter sent via SLIFT |
| SLIFT Submission | Submission failure | Retry 3 times, then manual review queue | Staff manual follow-up |

---

## Section 3: Detailed Flows

### Tab 3.1: AN Verification - Qualification Check

> **Note:** FD v1.2 states Same-Day AN Check should be removed, but it is **STILL ACTIVE** in actual code implementation (AdvisoryNoticeHelper.java:104-107, 142-182).

#### Process Overview

| Attribute | Value |
|-----------|-------|
| Process Name | AN Qualification Check |
| Section | 3.1 |
| Trigger | Notice creation with offence_notice_type='O' and vehicle_type in [S,D,V,I]. **Note:** CES notices with an_flag='Y' skip this check and go directly to PS-ANS (per FD v1.2 Section 4.2) |
| Frequency | Real-time (per notice creation) |
| Systems Involved | Backend (AdvisoryNoticeHelper), Database (Intranet) |

#### Process Steps Table

| Step | Definition | Brief Description |
|------|-----------|-------------------|
| Start | Qualification check | Check if notice qualifies as Advisory Notice |
| Check Offense Type | Validate offense type | Apply BR-AN-GATE-001: offence_notice_type must be 'O' |
| Decision: Offense Type = 'O'? | Gate check 1 | Check if offense type is 'O' (Offender) |
| Check Vehicle Type | Validate vehicle type | Apply BR-AN-GATE-002: vehicle_registration_type must be in [S,D,V,I] |
| Decision: Vehicle Type Valid? | Gate check 2 | Check if vehicle type is local (S/D/V/I) |
| Check Exemption Rules | Exemption validation | Apply BR-AN-EXEMPT-001 and BR-AN-EXEMPT-002 |
| Decision: Exempt? | Exemption check | Check if offense is exempt from AN |
| Check Past Offense | 24-month window check | Apply BR-AN-QUAL-002: Check for past qualifying offense |
| Decision: Has Past Offense? | Past offense validation | Check if vehicle has offense in past 24 months |
| Return Qualified | Pass qualification | Return qualified=true with AN details |
| Return Not Qualified | Fail qualification | Return qualified=false with reason |
| End | Check complete | Qualification result returned |

#### Decision Logic

| Decision | Input | Condition | True Action | False Action |
|----------|-------|-----------|-------------|--------------|
| Offense Type = 'O'? | offence_notice_type | offence_notice_type == 'O' | Continue to vehicle type check | Return not qualified ("Offense type must be 'O'") |
| Vehicle Type Valid? | vehicle_registration_type | vehicle_registration_type IN ['S','D','V','I'] | Continue to exemption check | Return not qualified ("Vehicle type must be S/D/V/I") |
| Exempt? | Rule code + composition/category | (Rule 20412 AND amount=$80) OR (Rule 11210 AND category in [LB,HL]) | Return not qualified ("Offense exempt from AN") | Continue to past offense check |
| Has Past Offense? | Query result | count > 0 | Return qualified | Return not qualified ("No qualifying offense in past 24 months") |

#### Business Rules Applied

**BR-AN-GATE-001: Offense Type Check**
- Rule: Offense type must be 'O' (Offender) for AN eligibility
- Implementation: AdvisoryNoticeHelper.checkQualification()
- Action: If offenseType != 'O', return not qualified

**BR-AN-GATE-002: Vehicle Type Check**
- Rule: Vehicle registration type must be S, D, V, or I (local vehicles only)
- Implementation: AdvisoryNoticeHelper.checkQualification()
- Action: If vehicleRegType NOT IN ['S','D','V','I'], return not qualified

**BR-AN-EXEMPT-001: Rule Code 20412 with $80 Composition**
- Rule: Offense code 20412 with composition amount = $80 is exempt from AN
- Implementation: AdvisoryNoticeHelper.isExemptFromAN()
- Action: If computerRuleCode = 20412 AND compositionAmount = 80.00, return exempt

**BR-AN-EXEMPT-002: Rule Code 11210 with Vehicle Category LB or HL**
- Rule: Offense code 11210 with vehicle category "LB" (Loading Bay) or "HL" (Handicap Lot) is exempt from AN
- Implementation: AdvisoryNoticeHelper.isExemptFromAN()
- Action: If computerRuleCode = 11210 AND vehicleCategory IN ['LB','HL'], return exempt

**BR-AN-QUAL-002: Past Offense Check (24-month window)**
- Rule: Vehicle must have qualifying offense in past 24 months
- Implementation: AdvisoryNoticeHelper.hasPastQualifyingOffense()
- Action: Query for past offenses from (currentDate - 24 months) to currentNoticeDate

#### Database Query (Past Offense Check)

**Table:** `ocms_valid_offence_notice` (Intranet)

**Query Logic:**
```
SELECT * FROM ocms_valid_offence_notice
WHERE vehicle_no = [current vehicle]
  AND notice_date_and_time >= [currentDate - 24 months]
  AND notice_date_and_time < [currentNoticeDate]
```

**Fields Used:**
- `vehicle_no`: Vehicle registration number
- `notice_date_and_time`: Notice date and time

#### Error Handling

| Error Point | Error Type | Handling | Recovery |
|-------------|-----------|----------|----------|
| Past Offense Query | SQLException | Return qualification failed | Process as standard notice |
| Null Parameters | Validation error | Return not qualified | Standard notice processing |

#### Swimlanes Definition

| Swimlane | Color | Steps |
|----------|-------|-------|
| Backend | Purple (#e1d5e7) | All validation steps (Offense type check, Vehicle type check, Exemption check, Return result) |
| Database | Yellow (#fff2cc) | Query past offenses |

---

### Tab 3.2: Receive AN Data from REPCCS

> **Note (FD v1.2 Section 4.2):** OCMS DISREGARDS the anFlag value sent by REPCCS. OCMS performs its own AN Qualification check for all REPCCS notices.

#### Process Overview

| Attribute | Value |
|-----------|-------|
| Process Name | Receive AN Data from REPCCS |
| Section | 3.2 |
| Trigger | REPCCS webhook call to /v1/repccsWebhook |
| Frequency | Real-time (event-driven) |
| Systems Involved | External System (REPCCS), Backend (OCMS API), Database (Intranet) |

#### Process Steps Table

| Step | Definition | Brief Description |
|------|-----------|-------------------|
| Start | Webhook received | REPCCS sends notice data to OCMS webhook endpoint |
| Receive Webhook Payload | Parse request | Receive and parse RepWebHookPayloadDto |
| Validate Mandatory Fields | Field validation | Apply BR-AN-REP-001: Check all mandatory fields present |
| Decision: Valid Payload? | Validation result | Check if payload has all required fields |
| Check Duplicate Notice | Duplicate detection | Apply BR-AN-DUP-001: Check if notice number already exists |
| Decision: Duplicate? | Duplicate check result | Check if notice number found in database |
| Detect Vehicle Type | Vehicle type detection | Apply special vehicle detection (MID/MINDEF, UNLICENSED_PARKING) |
| Generate Notice Number | Create notice ID | Generate notice number if not provided |
| Insert Notice Records | Create records | Insert into ocms_valid_offence_notice and ocms_offence_notice_detail |
| Check Double Booking | DBB detection | Apply BR-AN-DUP-002: Query for duplicate offense details |
| Decision: Duplicate Offense? | DBB check result | Check if matching offense found |
| Create PS-DBB Suspension | Suspend notice | Create PS-DBB suspension record, early return |
| Run AN Qualification | Qualification check | Call checkQualification() for offence_notice_type='O' and vehicle_type in [S,D,V,I] |
| Decision: Qualifies as AN? | AN qualification result | Check if notice passed all AN checks |
| Update AN Flags | Mark as AN | Set an_flag='Y', payment_acceptance_allowed='N' |
| Process Uploaded File | File handling | Process any uploaded photos/videos |
| Return Success Response | Response to REPCCS | Return modified response: message="OK", no notice number |
| Return Error Response | Error to REPCCS | Return error response with app code and message |
| End | Webhook complete | REPCCS webhook processing finished |

#### Decision Logic

| Decision | Input | Condition | True Action | False Action |
|----------|-------|-----------|-------------|--------------|
| Valid Payload? | Validation result | All mandatory fields present and valid | Continue to duplicate check | Return error OCMS-4000 |
| Duplicate? | Query result | Notice number exists in database | Return error OCMS-4000 "Duplicate notice detected" | Continue to vehicle type detection |
| Duplicate Offense? | DBB query result | Matching offense found (same vehicle, rule code, date/time) | Create PS-DBB suspension, early return | Continue to AN qualification |
| Qualifies as AN? | Qualification result | Passes all AN checks | Update AN flags | Continue to file processing |

#### API Call

**Endpoint:** POST /v1/repccsWebhook

**Request Payload (RepWebHookPayloadDto):**
```json
{
  "transactionId": "string",
  "subsystemLabel": "string (8)",
  "anFlag": "string (1)",
  "noticeNo": "string (10)",
  "vehicleNo": "string (14)",
  "computerRuleCode": "integer",
  "compositionAmount": "decimal",
  "noticeDateAndTime": "datetime",
  "offenceNoticeType": "string (1)",
  "vehicleCategory": "string (1)",
  "vehicleRegistrationType": "string (1)",
  "ppCode": "string (5)",
  "ppName": "string (100)",
  "creUserId": "string",
  "creDate": "datetime"
}
```

**Success Response (HTTP 200):**
```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "OK"
  }
}
```

**Error Response (HTTP 400):**
```json
{
  "data": {
    "appCode": "OCMS-4000",
    "message": "Missing mandatory fields / Duplicate notice detected"
  }
}
```

**Note:** REPCCS webhook uses OCMS-4000 (HTTP 400) for ALL errors including duplicate notice.

#### Database Operations

**Insert Tables:**
1. `ocms_valid_offence_notice` (Intranet) - Parent record
2. `ocms_offence_notice_detail` (Intranet) - Detail record
3. `eocms_valid_offence_notice` (Internet) - Mirror record
4. `ocms_suspended_notice` (Intranet) - If PS-DBB detected

**Update Tables:**
- `ocms_valid_offence_notice`: Set an_flag='Y', payment_acceptance_allowed='N' if qualified

#### Error Handling

| Error Point | Error Type | Handling | Recovery |
|-------------|-----------|----------|----------|
| Mandatory Fields Validation | Missing fields | Return OCMS-4000 error | REPCCS retries with complete data |
| Duplicate Notice Check | Notice exists | Return OCMS-4000 error | REPCCS does not retry |
| Database Insert | SQLException | Return OCMS-5000 error | REPCCS retries |
| DBB Query Retry | SQLException | Retry 3 times, apply TS-OLD fallback | Create TS-OLD suspension |

#### Swimlanes Definition

| Swimlane | Color | Steps |
|----------|-------|-------|
| External System | Green (#d5e8d4) | Send webhook payload, Receive response |
| Backend | Purple (#e1d5e7) | Validate fields, Detect vehicle type, Run qualification, Return response |
| Database | Yellow (#fff2cc) | Check duplicate, Insert notice records, Query DBB, Update AN flags |

---

### Tab 3.3: Receive AN Data from CES

> **CRITICAL (FD v1.2 Section 4.2):** OCMS uses the anFlag value directly from CES. If CES sends an_flag='Y', OCMS creates the notice with an_flag='Y' and **immediately suspends with PS-ANS WITHOUT performing AN Qualification check**.

#### Process Overview

| Attribute | Value |
|-----------|-------|
| Process Name | Receive AN Data from CES (Certis) |
| Section | 3.3 |
| Trigger | CES webhook call to /v1/cesWebhook-create-notice |
| Frequency | Real-time (event-driven) |
| Systems Involved | External System (CES), Backend (OCMS API), Database (Intranet) |

#### Process Steps Table

| Step | Definition | Brief Description |
|------|-----------|-------------------|
| Start | Webhook received | CES sends notice data to OCMS webhook endpoint |
| Receive Webhook Payload | Parse request | Receive and parse CesCreateNoticeDto |
| Validate Mandatory Fields | Field validation | Apply BR-AN-CES-001: Check all mandatory fields present |
| Decision: Valid Payload? | Validation result | Check if payload has all required fields |
| Validate Subsystem Label | Subsystem validation | Apply BR-AN-CES-001: Check subsystemLabel format (3-8 chars, 030-999 range) |
| Decision: Valid Subsystem? | Subsystem check result | Check if subsystem label passes all validations |
| Check Duplicate Notice | Duplicate detection | Apply BR-AN-DUP-001: Check if notice number already exists |
| Decision: Duplicate? | Duplicate check result | Check if notice number found in database |
| Validate Offense Rule Code | Rule code validation | Apply BR-AN-CES-002: Check if computer_rule_code exists in offense_rule_code table |
| Decision: Valid Rule Code? | Rule validation result | Check if rule code exists |
| Detect Vehicle Type | Vehicle type detection | Apply special vehicle detection (MID/MINDEF, UNLICENSED_PARKING) |
| Check EHT/Certis AN Flag | EHT check | Apply BR-AN-EHT-001: If subsystem 030-999 AND an_flag='Y' in payload |
| Decision: EHT with AN Flag? | EHT AN check | Check if EHT subsystem with an_flag='Y' |
| Create PS-ANS Suspension | Immediate suspension | Create notice, trigger PS-ANS suspension, early return |
| Insert Notice Records | Create records | Insert into ocms_valid_offence_notice and ocms_offence_notice_detail |
| Check Double Booking | DBB detection | Apply BR-AN-DUP-002: Query for duplicate offense details |
| Decision: Duplicate Offense? | DBB check result | Check if matching offense found |
| Create PS-DBB Suspension | Suspend notice | Create PS-DBB suspension record, early return |
| Run AN Qualification | Qualification check | Call checkQualification() for offence_notice_type='O' and vehicle_type in [S,D,V,I] |
| Decision: Qualifies as AN? | AN qualification result | Check if notice passed all AN checks |
| Update AN Flags | Mark as AN | Set an_flag='Y', payment_acceptance_allowed='N' |
| Process Uploaded File | File handling | Process any uploaded photos/videos |
| Return Success Response | Response to CES | Return success response with app code and message |
| Return Error Response | Error to CES | Return error response with specific error code |
| End | Webhook complete | CES webhook processing finished |

#### Decision Logic

| Decision | Input | Condition | True Action | False Action |
|----------|-------|-----------|-------------|--------------|
| Valid Payload? | Validation result | All mandatory fields present and valid | Continue to subsystem validation | Return error OCMS-4000 (HTTP 400) |
| Valid Subsystem? | Subsystem validation | Length 3-8 chars AND first 3 chars numeric 030-999 | Continue to duplicate check | Return error OCMS-4000 (HTTP 400) with specific message |
| Duplicate? | Query result | Notice number exists in database | Return error **OCMS-2026 (HTTP 226)** "Notice no Already exists" | Continue to rule code validation |
| Valid Rule Code? | Rule validation | Rule code exists in offense_rule_code table | Continue to vehicle detection | Return error **OCMS-2026 (HTTP 226)** "Invalid Offence Rule Code" |
| EHT with AN Flag? | Subsystem + flag check | Subsystem 030-999 AND an_flag='Y' | Create PS-ANS, early return | Continue to standard flow |
| Duplicate Offense? | DBB query result | Matching offense found (same vehicle, rule code, date/time) | Create PS-DBB suspension, early return | Continue to AN qualification |
| Qualifies as AN? | Qualification result | Passes all AN checks | Update AN flags | Continue to file processing |

#### API Call

**Endpoint:** POST /v1/cesWebhook-create-notice

**Request Payload (CesCreateNoticeDto):**
```json
{
  "transactionId": "string",
  "subsystemLabel": "string (3-8, 030-999)",
  "anFlag": "string (1)",
  "noticeNo": "string (10)",
  "vehicleNo": "string (14)",
  "computerRuleCode": "integer",
  "compositionAmount": "decimal",
  "noticeDateAndTime": "datetime",
  "offenceNoticeType": "string (1)",
  "vehicleCategory": "string (1)",
  "vehicleRegistrationType": "string (1)",
  "ppCode": "string (5)",
  "ppName": "string (100)",
  "creUserId": "string",
  "creDate": "datetime"
}
```

**Success Response (HTTP 200):**
```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Resource created successfully"
  }
}
```

**Error Response - Validation Errors (HTTP 400):**
```json
{
  "data": {
    "appCode": "OCMS-4000",
    "message": "Invalid input format or failed validation"
  }
}
```

**Error Response - Duplicate Notice / Invalid Rule Code (HTTP 226 IM Used):**
```json
{
  "data": {
    "appCode": "OCMS-2026",
    "message": "Notice no Already exists / Invalid Offence Rule Code"
  }
}
```

**Note:** CES webhook uses different error codes than REPCCS:
- **OCMS-4000 (HTTP 400):** For validation errors (mandatory fields, subsystem label format)
- **OCMS-2026 (HTTP 226):** For duplicate notice and invalid rule code errors

#### Database Operations

**Insert Tables:**
1. `ocms_valid_offence_notice` (Intranet) - Parent record
2. `ocms_offence_notice_detail` (Intranet) - Detail record
3. `eocms_valid_offence_notice` (Internet) - Mirror record
4. `ocms_suspended_notice` (Intranet) - If PS-ANS or PS-DBB

**Update Tables:**
- `ocms_valid_offence_notice`: Set an_flag='Y', payment_acceptance_allowed='N' if qualified

#### Error Handling

| Error Point | Error Type | Handling | Recovery |
|-------------|-----------|----------|----------|
| Subsystem Label Validation | Invalid format | Return OCMS-4000 (HTTP 400) | CES corrects subsystem label |
| Duplicate Notice Check | Notice exists | Return **OCMS-2026 (HTTP 226)** | CES does not retry |
| Rule Code Validation | Invalid rule code | Return **OCMS-2026 (HTTP 226)** | CES corrects rule code |
| Database Insert | SQLException | Return OCMS-5000 error | CES retries |
| DBB Query Retry | SQLException | Retry 3 times, apply TS-OLD fallback | Create TS-OLD suspension |

#### Swimlanes Definition

| Swimlane | Color | Steps |
|----------|-------|-------|
| External System | Green (#d5e8d4) | Send webhook payload, Receive response |
| Backend | Purple (#e1d5e7) | Validate fields, Validate subsystem, Check EHT flag, Run qualification, Return response |
| Database | Yellow (#fff2cc) | Check duplicate, Validate rule code, Insert notice records, Query DBB, Update AN flags |

---

### Tab 3.4: Retrieve Vehicle Owner Particulars (LTA + DataHive/MHA)

#### Process Overview

| Attribute | Value |
|-----------|-------|
| Process Name | Retrieve Vehicle Owner Particulars |
| Section | 3.4 |
| Trigger | After AN qualification passes (an_flag='Y') |
| Frequency | Real-time (per qualified AN) |
| Systems Involved | Backend (OCMS API), External Systems (LTA, DataHive, MHA) |

#### Process Steps Table

| Step | Definition | Brief Description |
|------|-----------|-------------------|
| Start | Owner retrieval | Begin vehicle owner particulars retrieval for qualified AN |
| Check Vehicle Type | Vehicle type validation | Apply OP-AN-LTA-001: Check vehicle registration type |
| Decision: Military Vehicle? | Military check | Check if vehicle_registration_type = 'I' (Military) |
| Set Military Owner | Military handling | Set owner = MINDEF, skip LTA API call |
| Call LTA API | Ownership query | Apply OP-AN-LTA-001: Call LTA API with vehicle number to retrieve owner details |
| Decision: LTA Success? | LTA response check | Check if LTA API returned owner information |
| Store Owner Details | Save owner info | Store vehicle owner name, NRIC/FIN, registration details in database |
| Check Suspension Status | Suspension validation | Apply OP-AN-DATAHIVE-001: Check if notice is suspended |
| Decision: Notice Suspended? | Suspension check | Check if suspension_type is not NULL |
| Skip Notification | No notification | Do not send notification for suspended notice, end process |
| Check Exclusion List | Exclusion validation | Apply OP-AN-DATAHIVE-001: Check if owner in eNotification exclusion list |
| Decision: In Exclusion List? | Exclusion check | Check if owner NRIC in exclusion list |
| Call DataHive API | Contact retrieval | Apply OP-AN-DATAHIVE-001: Call DataHive API with owner NRIC to get mobile/email |
| Decision: Contact Found? | Contact availability | Check if mobile OR email retrieved from DataHive |
| Qualify for eAN | eNotification path | Set eAN eligible flag, proceed to eAN sending (Tab 3.5) |
| Call MHA API | Address retrieval | Apply OP-AN-MHA-001: Call MHA API with owner NRIC to get registered address |
| Decision: Address Found? | Address availability | Check if registered address retrieved from MHA |
| Qualify for AN Letter | Physical letter path | Set AN letter flag, proceed to letter generation (Tab 3.6) |
| Manual Review Queue | Manual handling | Add to manual review queue for staff follow-up |
| End | Retrieval complete | Owner particulars retrieval process finished |

#### Decision Logic

| Decision | Input | Condition | True Action | False Action |
|----------|-------|-----------|-------------|--------------|
| Military Vehicle? | vehicle_registration_type | vehicle_registration_type == 'I' | Set owner=MINDEF, skip LTA | Call LTA API |
| LTA Success? | LTA API response | Response contains owner info AND status=success | Store owner details | Manual review queue |
| Notice Suspended? | suspension_type | suspension_type != NULL | Skip notification, end | Continue to exclusion check |
| In Exclusion List? | Exclusion query | Owner NRIC found in exclusion table | Skip DataHive, call MHA | Call DataHive API |
| Contact Found? | DataHive response | mobile != NULL OR email != NULL | Qualify for eAN | Call MHA API |
| Address Found? | MHA response | Registered address retrieved | Qualify for AN letter | Manual review queue |

#### External API Calls

**OP-AN-LTA-001: LTA Vehicle Ownership Check**

**Pre-call Validation:**
- Vehicle number must not be "UNLICENSED_PARKING"
- Vehicle registration type must not be 'I' (military)
- Vehicle registration type must not be 'X' (UPL)

**Request:**
```
GET LTA_API/vehicle/owner?vehicleNo=[vehicle_number]
Headers:
  API-Key: [from Azure Key Vault]
```

**Response:**
```json
{
  "ownerName": "string",
  "ownerNRIC": "string",
  "vehicleMake": "string",
  "vehicleModel": "string",
  "roadTaxExpiry": "date",
  "diplomaticFlag": "boolean"
}
```

**Timeout:** 30 seconds
**Retry:** 3 attempts with exponential backoff

**OP-AN-DATAHIVE-001: DataHive Contact Retrieval**

**Pre-call Validation:**
- Owner NRIC/FIN must be available from LTA response
- Notice must NOT be suspended
- Owner must NOT be in eNotification exclusion list

**Request:**
```
GET DATAHIVE_API/contact?nric=[owner_nric]
Headers:
  API-Key: [from Azure Key Vault]
```

**Response:**
```json
{
  "mobileNumber": "string",
  "emailAddress": "string"
}
```

**Timeout:** 30 seconds
**Retry:** 3 attempts with exponential backoff

**OP-AN-MHA-001: MHA Address Retrieval**

**Pre-call Validation:**
- Owner NRIC/FIN must be available
- eNotification qualification failed

**Request:**
```
GET MHA_API/address?nric=[owner_nric]
Headers:
  API-Key: [from Azure Key Vault]
```

**Response:**
```json
{
  "buildingName": "string",
  "streetName": "string",
  "postalCode": "string",
  "unitNumber": "string"
}
```

**Timeout:** 30 seconds
**Retry:** 3 attempts with exponential backoff

#### Error Handling

| Error Point | Error Type | Handling | Recovery |
|-------------|-----------|----------|----------|
| LTA API Timeout | Timeout (>30s) | Retry 3 times with exponential backoff | Manual review queue |
| LTA API Error | 500 Internal Server Error | Log error, retry | Manual review queue after 3 failures |
| LTA Owner Not Found | 404 Not Found | Log warning | Manual review queue |
| DataHive API Error | Any error | Log error, fallback to physical letter | Call MHA API |
| DataHive No Contact | Empty response | Proceed to physical letter flow | Call MHA API |
| MHA API Error | Any error | Retry 3 times | Manual review queue after failures |
| MHA Address Not Found | Empty response | Log warning | Manual review queue |

#### Swimlanes Definition

| Swimlane | Color | Steps |
|----------|-------|-------|
| Backend | Purple (#e1d5e7) | Check vehicle type, Check suspension, Check exclusion list, Qualify for eAN/letter |
| External System (LTA) | Green (#d5e8d4) | LTA API call, Return owner details |
| External System (DataHive) | Green (#d5e8d4) | DataHive API call, Return contact info |
| External System (MHA) | Green (#d5e8d4) | MHA API call, Return registered address |
| Database | Yellow (#fff2cc) | Store owner details, Check exclusion list |

---

### Tab 3.5: Send eNotification (eAN)

#### Process Overview

| Attribute | Value |
|-----------|-------|
| Process Name | Send eNotification for Advisory Notice (eAN) |
| Section | 3.5 |
| Trigger | After owner contact retrieval (mobile OR email available) |
| Frequency | Real-time (per qualified eAN) |
| Systems Involved | Backend (OCMS API), External Systems (SMS Gateway, Email Service) |

#### Process Steps Table

| Step | Definition | Brief Description |
|------|-----------|-------------------|
| Start | eAN sending | Begin eNotification sending process for qualified AN |
| Retrieve Notice Details | Get notice info | Retrieve notice number, offense details, composition amount from database |
| Retrieve Owner Contact | Get contact info | Retrieve mobile number and/or email address from previous step |
| Generate eAN Content | Create message | Generate eAN message content with notice details, offense info, appeal instructions |
| Decision: Mobile Available? | Mobile check | Check if mobile number exists |
| Send SMS | SMS notification | Call SMS Gateway API to send eAN SMS to mobile number |
| Log SMS Sent | SMS logging | Log SMS sent status, timestamp, recipient |
| Decision: SMS Success? | SMS result check | Check if SMS sent successfully |
| Decision: Email Available? | Email check | Check if email address exists |
| Send Email | Email notification | Call Email Service API to send eAN email |
| Log Email Sent | Email logging | Log email sent status, timestamp, recipient |
| Decision: Email Success? | Email result check | Check if email sent successfully |
| Update eAN Status | Update database | Update notice with eAN sent flag, sent timestamp, notification method |
| Log Notification Complete | Completion logging | Log eAN notification completion |
| Retry Notification | Retry logic | Retry failed notification (max 3 times) |
| Fallback to Letter | Physical letter path | If all retries fail, proceed to AN letter sending (Tab 3.6) |
| End | eAN complete | eNotification sending process finished |

#### Decision Logic

| Decision | Input | Condition | True Action | False Action |
|----------|-------|-----------|-------------|--------------|
| Mobile Available? | Mobile number | mobile != NULL AND mobile != "" | Send SMS | Skip to email check |
| SMS Success? | SMS API response | Response status = success | Log SMS sent | Retry SMS (max 3 times) |
| Email Available? | Email address | email != NULL AND email != "" | Send Email | End process |
| Email Success? | Email API response | Response status = success | Log email sent | Retry email (max 3 times) |

#### eAN Message Content

**SMS Content Template:**
```
Advisory Notice [NoticeNo]
Offense: [OffenseDescription]
Location: [PPName]
Date: [NoticeDate]
Amount: $[CompositionAmount]
Payment not accepted. You may appeal via OCMS portal.
```

**Email Content Template:**
```
Subject: Advisory Notice [NoticeNo] - [VehicleNo]

Dear Vehicle Owner,

This is an Advisory Notice for the following offense:

Notice Number: [NoticeNo]
Vehicle Number: [VehicleNo]
Offense: [OffenseDescription]
Location: [PPName]
Date: [NoticeDate]
Composition Amount: $[CompositionAmount]

This is an advisory notice only. Payment is not accepted at this time.

You may appeal this notice via the OCMS portal at [PortalURL].

Regards,
Urban Redevelopment Authority
```

#### External API Calls

**SMS Gateway API**

**Request:**
```
POST SMS_GATEWAY_API/send
{
  "mobileNumber": "[owner_mobile]",
  "message": "[eAN SMS content]",
  "sender": "URA_OCMS"
}
```

**Response:**
```json
{
  "status": "success/failed",
  "messageId": "string",
  "timestamp": "datetime"
}
```

**Timeout:** 10 seconds
**Retry:** 3 attempts

**Email Service API**

**Request:**
```
POST EMAIL_SERVICE_API/send
{
  "emailAddress": "[owner_email]",
  "subject": "[eAN email subject]",
  "body": "[eAN email content]",
  "sender": "noreply@ura.gov.sg"
}
```

**Response:**
```json
{
  "status": "success/failed",
  "messageId": "string",
  "timestamp": "datetime"
}
```

**Timeout:** 10 seconds
**Retry:** 3 attempts

#### Database Operations

**Update Tables:**
- `ocms_valid_offence_notice`:
  - Set eAN_sent_flag = 'Y'
  - Set eAN_sent_timestamp = current timestamp
  - Set notification_method = 'SMS' or 'EMAIL' or 'BOTH'

**Insert Tables:**
- `ocms_ean_notification_log`:
  - notice_no
  - recipient_mobile/email
  - message_content
  - sent_timestamp
  - status (SUCCESS/FAILED)
  - retry_count

#### Error Handling

| Error Point | Error Type | Handling | Recovery |
|-------------|-----------|----------|----------|
| SMS Gateway Timeout | Timeout (>10s) | Retry 3 times with exponential backoff | Proceed to email if mobile fails |
| SMS Gateway Error | API error | Log error, retry | Try email after 3 failures |
| Email Service Timeout | Timeout (>10s) | Retry 3 times | Fallback to physical letter if both fail |
| Email Service Error | API error | Log error, retry | Fallback to physical letter after 3 failures |
| All Notifications Failed | All retries exhausted | Log critical error | Proceed to AN letter sending (Tab 3.6) |

#### Swimlanes Definition

| Swimlane | Color | Steps |
|----------|-------|-------|
| Backend | Purple (#e1d5e7) | Retrieve details, Generate content, Update status, Log completion |
| External System (SMS) | Green (#d5e8d4) | Send SMS, Return SMS result |
| External System (Email) | Green (#d5e8d4) | Send Email, Return email result |
| Database | Yellow (#fff2cc) | Retrieve notice details, Update eAN status, Log notification |

---

### Tab 3.6: Send AN Letter (SLIFT)

#### Process Overview

| Attribute | Value |
|-----------|-------|
| Process Name | Send Advisory Notice Letter |
| Section | 3.6 |
| Trigger | After owner address retrieval OR eNotification failed |
| Frequency | Real-time (per qualified AN letter) |
| Systems Involved | Backend (OCMS API), External System (SLIFT/SFTP) |

#### Process Steps Table

| Step | Definition | Brief Description |
|------|-----------|-------------------|
| Start | AN letter sending | Begin AN letter generation and sending process |
| Retrieve Notice Details | Get notice info | Retrieve notice number, offense details, composition amount from database |
| Retrieve Owner Address | Get address info | Retrieve registered address from MHA API response |
| Retrieve Owner Name | Get owner name | Retrieve vehicle owner name from LTA API response |
| Generate Letter Template | Create letter | Generate AN letter PDF/XML with notice details, owner address, letter content |
| Populate Letter Fields | Fill template | Populate letter with notice number, vehicle number, offense details, owner name/address |
| Format Letter Content | Format PDF/XML | Format letter content according to SLIFT requirements |
| Decision: Valid Letter? | Letter validation | Check if letter PDF/XML generated successfully |
| Call SLIFT API | Submit for printing | Apply OP-AN-SLIFT-001: Submit letter to SLIFT/SFTP for printing and mailing |
| Decision: SLIFT Success? | Submission result | Check if SLIFT accepted letter submission |
| Update Letter Status | Update database | Update notice with letter_sent_flag='Y', sent_timestamp, submission_id |
| Log Letter Sent | Completion logging | Log AN letter sent status, timestamp, recipient |
| Retry Submission | Retry logic | Retry failed submission (max 3 times with exponential backoff) |
| Manual Review Queue | Manual handling | Add to manual review queue if all retries fail |
| End | Letter complete | AN letter sending process finished |

#### Decision Logic

| Decision | Input | Condition | True Action | False Action |
|----------|-------|-----------|-------------|--------------|
| Valid Letter? | Letter generation result | PDF/XML generated successfully AND size > 0 | Call SLIFT API | Log error, manual review queue |
| SLIFT Success? | SLIFT API response | Response status = success AND submission_id returned | Update letter status | Retry submission (max 3 times) |

#### AN Letter Content Template

**Letter Structure:**
```
Urban Redevelopment Authority
Car Parks Department

[Owner Name]
[Building Name]
[Street Name]
[Unit Number]
[Postal Code]

Date: [Current Date]

Advisory Notice Number: [NoticeNo]

Dear Sir/Madam,

RE: ADVISORY NOTICE - VEHICLE [VehicleNo]

This is an Advisory Notice for the following parking offense:

Offense Details:
- Notice Number: [NoticeNo]
- Vehicle Number: [VehicleNo]
- Offense: [OffenseDescription]
- Location: [PPName]
- Date and Time: [NoticeDateAndTime]
- Composition Amount: $[CompositionAmount]

This is an advisory notice only. Payment is not accepted for this notice.

You have the right to appeal this notice. Please visit the OCMS portal
at [PortalURL] to submit your appeal.

If you have any questions, please contact us at [ContactNumber].

Yours faithfully,
Urban Redevelopment Authority
```

#### External API Call

**OP-AN-SLIFT-001: SLIFT Letter Submission**

**Pre-call Validation:**
- Owner's registered address must be available
- Notice must have an_flag = 'Y'
- Letter PDF/XML must be generated

**Request:**
```
POST SLIFT_API/submit
Headers:
  API-Key: [from Azure Key Vault]
Body (multipart/form-data):
  {
    "noticeNumber": "[notice_no]",
    "vehicleNumber": "[vehicle_no]",
    "letterFile": "[PDF/XML file]",
    "letterType": "ADVISORY_NOTICE",
    "recipientName": "[owner_name]",
    "recipientAddress": "[full_address]",
    "priority": "NORMAL"
  }
```

**Response:**
```json
{
  "status": "success/failed",
  "submissionId": "string",
  "timestamp": "datetime",
  "estimatedPrintDate": "date"
}
```

**Timeout:** 30 seconds
**Retry:** 3 attempts with exponential backoff (1s, 2s, 4s)

**File Format:** PDF (preferred) or XML based on SLIFT requirements

#### Database Operations

**Update Tables:**
- `ocms_valid_offence_notice`:
  - Set letter_sent_flag = 'Y'
  - Set letter_sent_timestamp = current timestamp
  - Set slift_submission_id = submission ID from SLIFT

**Insert Tables:**
- `ocms_an_letter_log`:
  - notice_no
  - recipient_name
  - recipient_address
  - letter_file_path
  - slift_submission_id
  - sent_timestamp
  - status (SUCCESS/FAILED)
  - retry_count

#### Error Handling

| Error Point | Error Type | Handling | Recovery |
|-------------|-----------|----------|----------|
| Letter Generation | Generation failure | Log error, retry generation | Manual review queue if fails |
| SLIFT API Timeout | Timeout (>30s) | Retry 3 times with exponential backoff | Manual review queue after 3 failures |
| SLIFT API Error | API error | Log error, retry | Manual review queue after 3 failures |
| SLIFT Rejection | Validation error | Log error with reason | Manual review queue |
| File Too Large | File size error | Compress PDF, retry | Manual review queue if compression fails |

#### Swimlanes Definition

| Swimlane | Color | Steps |
|----------|-------|-------|
| Backend | Purple (#e1d5e7) | Retrieve details, Generate letter, Format content, Update status, Log completion |
| External System (SLIFT) | Green (#d5e8d4) | Submit letter, Return submission result |
| Database | Yellow (#fff2cc) | Retrieve notice/owner details, Update letter status, Log letter sent |

---

### Tab 3.7: Suspend Notice with PS-ANS

#### Process Overview

| Attribute | Value |
|-----------|-------|
| Process Name | Suspend Notice with PS-ANS |
| Section | 3.7 |
| Trigger | After AN qualification and notification (eAN or letter) sent |
| Frequency | Real-time (per qualified AN) |
| Systems Involved | Backend (OCMS API), Database (Intranet) |

#### Process Steps Table

| Step | Definition | Brief Description |
|------|-----------|-------------------|
| Start | Suspension process | Begin PS-ANS suspension creation for qualified AN |
| Retrieve Notice Details | Get notice info | Retrieve notice number, notice date from database |
| Calculate Suspension Date | Set suspension date | Set date_of_suspension = current date |
| Calculate Revival Date | Set revival date | Calculate due_date_of_revival = current date + 30 days (configurable) |
| Generate SR Number | Create serial number | Generate sequential sr_no for suspension record |
| Create Suspension Record | Insert suspension | Insert record into ocms_suspended_notice table |
| Update Notice with Suspension | Update notice | Update ocms_valid_offence_notice with suspension details |
| Sync to Internet DB | Mirror record | Insert/update suspension record in eocms_suspended_notice (Internet) |
| Log Suspension Created | Logging | Log PS-ANS suspension creation with timestamp |
| End | Suspension complete | PS-ANS suspension process finished |

#### Database Operations

**Insert Table: ocms_suspended_notice (Intranet)**

**Fields:**
- `notice_no`: Notice number (PK)
- `date_of_suspension`: Current date (PK)
- `sr_no`: Sequential number (PK)
- `suspension_type`: 'PS' (Permanent Suspension)
- `reason_of_suspension`: 'ANS' (Advisory Notice Suspension)
- `suspension_source`: 'ocmsiz_app_conn'
- `officer_authorising_suspension`: 'ocmsizmgr_conn'
- `due_date_of_revival`: current date + 30 days
- `suspension_remarks`: 'Advisory Notice detected - qualified AN'
- `created_by`: 'ocmsiz_app_conn'
- `created_date`: Current timestamp

**Update Table: ocms_valid_offence_notice (Intranet)**

**Fields:**
- `suspension_type`: 'PS'
- `epr_reason_of_suspension`: 'ANS'
- `epr_date_of_suspension`: Current date
- `due_date_of_revival`: current date + 30 days
- `modified_by`: 'ocmsiz_app_conn'
- `modified_date`: Current timestamp

**Insert/Update Table: eocms_valid_offence_notice (Internet)**

Mirror same suspension fields from intranet table.

#### Suspension Parameters

| Parameter | Value | Source |
|-----------|-------|--------|
| Suspension Type | PS (Permanent Suspension) | Fixed |
| Reason of Suspension | ANS (Advisory Notice Suspension) | Fixed |
| Suspension Source | ocmsiz_app_conn | System parameter |
| Officer Authorising | ocmsizmgr_conn | System parameter |
| Revival Period | 30 days (configurable) | System configuration table |

#### Error Handling

| Error Point | Error Type | Handling | Recovery |
|-------------|-----------|----------|----------|
| Suspension Insert | SQLException | Log error, rollback | Retry insert, manual review if fails |
| Notice Update | SQLException | Log error, rollback | Retry update, ensure data consistency |
| Internet Sync | Sync failure | Log error | Retry sync, manual verification |
| Revival Date Calculation | Date error | Use default 30 days | Log warning, continue with default |

#### Swimlanes Definition

| Swimlane | Color | Steps |
|----------|-------|-------|
| Backend | Purple (#e1d5e7) | Calculate dates, Generate SR number, Log suspension |
| Database | Yellow (#fff2cc) | Retrieve notice details, Create suspension record, Update notice, Sync to Internet |

---

### Tab 3.8: Generate ANS Reports

#### Process Overview

| Attribute | Value |
|-----------|-------|
| Process Name | Generate Advisory Notice Suspension Reports |
| Section | 3.8 |
| Trigger | Ad-hoc request from staff portal OR scheduled cron job |
| Frequency | On-demand / Daily (scheduled) |
| Systems Involved | Backend (OCMS API), Database (Intranet), Frontend (Staff Portal) |

#### Process Steps Table

| Step | Definition | Brief Description |
|------|-----------|-------------------|
| Start | Report generation | Staff requests ANS report from portal |
| Receive Report Parameters | Get report criteria | Receive date range, vehicle number, subsystem filter from user |
| Validate Parameters | Parameter validation | Validate date range (max 31 days), required fields |
| Decision: Valid Parameters? | Validation result | Check if parameters are valid |
| Query ANS Suspension Data | Database query | Query ocms_suspended_notice joined with ocms_valid_offence_notice for reason_of_suspension='ANS' |
| Apply Filters | Filter results | Apply vehicle number, subsystem, date range filters |
| Decision: Any Records? | Record check | Check if query returned any records |
| Sort Results | Order data | Sort by suspension date descending |
| Format Report Data | Format output | Format data for report display (Excel/PDF) |
| Generate Report File | Create file | Generate Excel or PDF report file |
| Return Report | Download response | Return report file to user for download |
| Log Report Generated | Logging | Log report generation with parameters, timestamp, user |
| Return No Data Message | Empty result | Return message "No records found for the selected criteria" |
| Return Error Response | Error handling | Return error message for invalid parameters |
| End | Report complete | ANS report generation finished |

#### Decision Logic

| Decision | Input | Condition | True Action | False Action |
|----------|-------|-----------|-------------|--------------|
| Valid Parameters? | Validation result | Date range <= 31 days AND required fields present | Query suspended notices | Return error response |
| Any Records? | Query result | count > 0 | Sort results and generate report | Return no data message |

#### Report Parameters

| Parameter | Type | Required | Validation |
|-----------|------|----------|------------|
| Date From | Date | Yes | Must be <= Date To |
| Date To | Date | Yes | Date range max 31 days |
| Vehicle Number | String | No | Alphanumeric |
| Subsystem | String | No | Valid subsystem code |

#### Database Query

**Query Logic:**
```sql
SELECT
  sn.notice_no,
  sn.date_of_suspension,
  sn.due_date_of_revival,
  sn.suspension_remarks,
  von.vehicle_no,
  von.subsystem_label,
  von.computer_rule_code,
  von.composition_amount,
  von.notice_date_and_time,
  von.pp_code,
  von.pp_name,
  von.offense_notice_type
FROM ocms_suspended_notice sn
INNER JOIN ocms_valid_offence_notice von
  ON sn.notice_no = von.notice_no
WHERE sn.reason_of_suspension = 'ANS'
  AND sn.date_of_suspension BETWEEN [dateFrom] AND [dateTo]
  AND ([vehicleNo] IS NULL OR von.vehicle_no = [vehicleNo])
  AND ([subsystem] IS NULL OR von.subsystem_label = [subsystem])
ORDER BY sn.date_of_suspension DESC
```

#### Report Output Format

**Excel Report Columns:**
1. Notice Number
2. Vehicle Number
3. Subsystem
4. Suspension Date
5. Revival Date
6. Offense Code
7. Composition Amount
8. Notice Date
9. Parking Place
10. Suspension Remarks

**PDF Report Structure:**
```
Advisory Notice Suspension Report
Generated: [Current Date Time]
Date Range: [Date From] to [Date To]

+--------------+-------------+----------+----------------+-------------+
| Notice No    | Vehicle No  | Subsystem| Suspension Date| Revival Date|
+--------------+-------------+----------+----------------+-------------+
| [NoticeNo]   | [VehicleNo] | [System] | [SuspDate]     | [RevDate]   |
+--------------+-------------+----------+----------------+-------------+

Total Records: [Count]
```

#### Error Handling

| Error Point | Error Type | Handling | Recovery |
|-------------|-----------|----------|----------|
| Parameter Validation | Invalid date range | Return error "Date range exceeds 31 days" | User corrects parameters |
| Database Query | SQLException | Log error, return error message | User retries |
| Report Generation | File creation error | Log error, return error message | User retries |
| No Data | Empty result | Return friendly message | User adjusts criteria |

#### Swimlanes Definition

| Swimlane | Color | Steps |
|----------|-------|-------|
| Frontend | Blue (#dae8fc) | Receive report request, Download report |
| Backend | Purple (#e1d5e7) | Validate parameters, Apply filters, Format data, Generate report, Return response |
| Database | Yellow (#fff2cc) | Query suspended notices, Join notice details |

---

### Tab 3.9: Send Unqualified AN List (REPCCS/CES)

#### Process Overview

| Attribute | Value |
|-----------|-------|
| Process Name | Send Unqualified AN List to REPCCS/CES |
| Section | 3.9 |
| Trigger | Scheduled cron job (daily) |
| Frequency | Daily at configured time |
| Systems Involved | Backend (OCMS Cron), Database (Intranet), External Systems (REPCCS, CES) |

#### Process Steps Table

| Step | Definition | Brief Description |
|------|-----------|-------------------|
| Start | Cron job triggered | Daily scheduled job to send unqualified AN list |
| Calculate Date Range | Determine period | Calculate date range for notices to process (e.g., previous day) |
| Query Unqualified ANs | Database query | Query notices where an_flag='N' or NULL for REPCCS/CES subsystems |
| Filter by Subsystem | Separate lists | Separate unqualified notices by subsystem (REPCCS vs CES) |
| Decision: Any REPCCS Records? | REPCCS check | Check if any unqualified notices for REPCCS |
| Format REPCCS List | Create payload | Format unqualified AN list for REPCCS API |
| Call REPCCS API | Send to REPCCS | Send unqualified AN list to REPCCS webhook |
| Decision: REPCCS Success? | REPCCS response | Check if REPCCS accepted the list |
| Log REPCCS Sent | REPCCS logging | Log successful transmission to REPCCS |
| Retry REPCCS | Retry logic | Retry failed REPCCS transmission (max 3 times) |
| Decision: Any CES Records? | CES check | Check if any unqualified notices for CES |
| Format CES List | Create payload | Format unqualified AN list for CES API |
| Call CES API | Send to CES | Send unqualified AN list to CES webhook |
| Decision: CES Success? | CES response | Check if CES accepted the list |
| Log CES Sent | CES logging | Log successful transmission to CES |
| Retry CES | Retry logic | Retry failed CES transmission (max 3 times) |
| Update Notice Status | Mark as sent | Update notices with unqualified_list_sent_flag='Y', sent_timestamp |
| Log Cron Complete | Completion logging | Log cron job completion with summary |
| End | Cron job finished | Unqualified AN list sending complete |

#### Decision Logic

| Decision | Input | Condition | True Action | False Action |
|----------|-------|-----------|-------------|--------------|
| Any REPCCS Records? | Query result | count > 0 for REPCCS subsystem | Format REPCCS list | Skip REPCCS transmission |
| REPCCS Success? | REPCCS API response | Response status = success | Log sent | Retry REPCCS (max 3 times) |
| Any CES Records? | Query result | count > 0 for CES subsystem | Format CES list | Skip CES transmission |
| CES Success? | CES API response | Response status = success | Log sent | Retry CES (max 3 times) |

#### Database Query

**Query for Unqualified ANs:**
```sql
SELECT
  notice_no,
  vehicle_no,
  subsystem_label,
  computer_rule_code,
  composition_amount,
  notice_date_and_time,
  offense_notice_type,
  an_flag,
  created_date
FROM ocms_valid_offence_notice
WHERE created_date BETWEEN [dateFrom] AND [dateTo]
  AND (an_flag = 'N' OR an_flag IS NULL)
  AND subsystem_label IN ([REPCCS_subsystems], [CES_subsystems])
  AND unqualified_list_sent_flag = 'N'
ORDER BY created_date ASC
```

**REPCCS Subsystems:** Configured list of REPCCS subsystem codes
**CES Subsystems:** Subsystems starting with 030-999

#### Unqualified AN Conditions (Actual Code Implementation)

Notices are considered unqualified if:
1. `offence_notice_type` != 'O'
2. `vehicle_registration_type` NOT IN ['S','D','V','I']
3. Vehicle already has AN today (same-day limit - still active in code)
4. Exempt offense (Rule 20412 + $80 OR Rule 11210 + LB/HL)
5. No past offense in 24 months (simplified logic - any past offense qualifies)

> **Note:** Code uses simplified logic. FD v1.2 requires ANS PS reason check, but code just checks for any past offense.

#### External API Calls

**REPCCS Unqualified List API**

**Request:**
```
POST REPCCS_API/unqualified-an-list
Headers:
  API-Key: [from Azure Key Vault]
Body:
{
  "transmissionDate": "[current_date]",
  "unqualifiedNotices": [
    {
      "noticeNo": "string",
      "vehicleNo": "string",
      "subsystemLabel": "string",
      "noticeDate": "datetime",
      "reason": "string"
    }
  ]
}
```

**Response:**
```json
{
  "status": "success/failed",
  "receivedCount": "integer",
  "timestamp": "datetime"
}
```

**CES Unqualified List API**

**Request:**
```
POST CES_API/unqualified-an-list
Headers:
  API-Key: [from Azure Key Vault]
Body:
{
  "transmissionDate": "[current_date]",
  "unqualifiedNotices": [
    {
      "noticeNo": "string",
      "vehicleNo": "string",
      "subsystemLabel": "string",
      "noticeDate": "datetime",
      "reason": "string"
    }
  ]
}
```

**Response:**
```json
{
  "status": "success/failed",
  "receivedCount": "integer",
  "timestamp": "datetime"
}
```

**Timeout:** 60 seconds (can be large list)
**Retry:** 3 attempts with exponential backoff

#### Database Operations

**Update Table: ocms_valid_offence_notice**

**Fields:**
- `unqualified_list_sent_flag`: 'Y'
- `unqualified_list_sent_timestamp`: Current timestamp
- `modified_by`: 'ocmsiz_cron'
- `modified_date`: Current timestamp

**Insert Table: ocms_unqualified_an_transmission_log**

**Fields:**
- `transmission_date`: Current date
- `subsystem`: 'REPCCS' or 'CES'
- `total_count`: Number of notices sent
- `success_count`: Number successfully transmitted
- `failed_count`: Number failed
- `retry_count`: Number of retries
- `status`: 'SUCCESS' / 'PARTIAL' / 'FAILED'
- `created_date`: Current timestamp

#### Error Handling

| Error Point | Error Type | Handling | Recovery |
|-------------|-----------|----------|----------|
| REPCCS API Timeout | Timeout (>60s) | Retry 3 times with exponential backoff | Log failure, alert admin |
| REPCCS API Error | API error | Log error, retry | Alert admin after 3 failures |
| CES API Timeout | Timeout (>60s) | Retry 3 times with exponential backoff | Log failure, alert admin |
| CES API Error | API error | Log error, retry | Alert admin after 3 failures |
| Database Update | SQLException | Log error | Continue to next batch |

#### Cron Job Configuration

| Parameter | Value |
|-----------|-------|
| Schedule | Daily at 02:00 AM SGT |
| Date Range | Previous day (00:00:00 to 23:59:59) |
| Batch Size | 1000 notices per API call |
| Max Retry | 3 attempts |
| Timeout | 60 seconds per API call |

#### Swimlanes Definition

| Swimlane | Color | Steps |
|----------|-------|-------|
| Backend | Purple (#e1d5e7) | Calculate date range, Filter by subsystem, Format lists, Update status, Log completion |
| Database | Yellow (#fff2cc) | Query unqualified ANs, Update notice status, Log transmission |
| External System (REPCCS) | Green (#d5e8d4) | Receive REPCCS list, Return response |
| External System (CES) | Green (#d5e8d4) | Receive CES list, Return response |

---

## End of Document

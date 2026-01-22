# API Planning Document - OCMS 10: Advisory Notices Processing

## Document Information
- **Version:** 1.2
- **Date:** 2026-01-22
- **Source Documents:**
  - Functional Document: v1.2_OCMS 10_Functional Document.md
  - Backend Code: ura-project-ocmsadminapi-5e962080c0b4
  - Key Files: AdvisoryNoticeHelper.java, CreateNoticeServiceImpl.java, CreateNoticeController.java
- **Related Documents:**
  - OCMS 3 Technical Document (REPCCS API Specification)
  - OCMS 5 Technical Document (Notice Creation)
  - OCMS 21 Technical Document (Double Booking)

**Change Log:**
- v1.2 (2026-01-22): Aligned with corrected Technical Document. Added database tables for notifications (ocms_email_notification_records, ocms_sms_notification_records, ocms_an_letter, ocms_offence_notice_owner_driver). Added references to OCMS 3, 5, 21 documents.
- v1.1 (2026-01-15): Updated to align with FD v1.2 - Added REPCCS/CES AN flag handling notes

---

## 1. Internal APIs

### 1.1 Notice Creation Endpoints

#### 1.1.1 Create Notice (Standard)
**Endpoint:** `POST /v1/create-notice`

**Description:** Standard notice creation endpoint for OCMS Staff Portal

**Request:**
- **Method:** POST
- **Content-Type:** application/json
- **Body:** String (raw JSON)

**Request Payload (OffenceNoticeDto):**
```json
{
  "noticeNo": "string (10)",
  "compositionAmount": "decimal",
  "computerRuleCode": "integer",
  "creDate": "datetime",
  "creUserId": "string",
  "lastProcessingDate": "datetime",
  "lastProcessingStage": "string (3)",
  "noticeDateAndTime": "datetime",
  "offenceNoticeType": "string (1)",
  "ppCode": "string (5)",
  "ppName": "string (100)",
  "vehicleCategory": "string (1)",
  "vehicleNo": "string (14)",
  "vehicleRegistrationType": "string (1)",
  "subsystemLabel": "string (8)",
  "wardenNo": "string (5)",
  "parkingLotNo": "string (5)",
  "anFlag": "string (1)",
  "paymentAcceptanceAllowed": "string (1)",
  "photos": ["string"],
  "videos": ["string"]
}
```

**Response:**
```json
{
  "data": {
    "appCode": "string",
    "message": "string",
    "noticeNo": "string"
  }
}
```

**Success Codes:**
- `OCMS-2000`: Operation completed successfully

**Error Codes:**
- `OCMS-4000`: Bad request - Missing mandatory fields or invalid format
- `OCMS-5000`: Internal server error

---

#### 1.1.2 Staff Create Notice
**Endpoint:** `POST /v1/staff-create-notice`

**Description:** Notice creation endpoint specifically for OCMS Staff Portal users

**Request:** Same as 1.1.1
**Response:** Same as 1.1.1

---

#### 1.1.3 PLUS Create Notice
**Endpoint:** `POST /v1/plus-create-notice`

**Description:** Notice creation endpoint for PLUS Staff Portal users

**Request:** Same as 1.1.1 (with automatic subsystemLabel set to PLUS_CODE)
**Response:** Same as 1.1.1

---

### 1.2 External System Integration Endpoints

#### 1.2.1 REPCCS Webhook
**Endpoint:** `POST /v1/repccsWebhook`

**Description:** Webhook endpoint to receive notice data from REPCCS system

**Reference:** Refer to OCMS 3 Technical Document section 1.1.3 for detailed API specification and section 2.1 for data mapping.

**Request:**
- **Method:** POST
- **Content-Type:** application/json

**Request Payload (RepWebHookPayloadDto → RepCreateNoticeDto):**
```json
{
  "transactionId": "string",
  "subsystemLabel": "string (8)",
  "anFlag": "string (1)",
  "iuNo": "string",
  "repObuLatitude": "string",
  "repObuLongitude": "string",
  "repOperatorId": "string",
  "repParkingEntryDt": "string (datetime)",
  "repParkingStartDt": "string (datetime)",
  "repParkingEndDt": "string (datetime)",
  "repChargeAmount": "decimal",
  "repViolationCode": "string",
  "compositionAmount": "decimal",
  "computerRuleCode": "integer",
  "noticeDateAndTime": "string (datetime)",
  "offenceNoticeType": "string (1)",
  "vehicleNo": "string (14)",
  "vehicleCategory": "string (1)",
  "vehicleRegistrationType": "string (1)",
  "noticeNo": "string (10)",
  "otherRemark": "string",
  "ppCode": "string (5)",
  "ppName": "string (100)",
  "creUserId": "string",
  "creDate": "string (datetime)",
  "createdDt": "string (datetime)"
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
    "message": "Invalid input format or failed validation"
  }
}
```

**Special Behavior:**
- Success response returns "OK" message (modified from standard "Resource created successfully")
- Duplicate check is performed before notice creation
- All errors return HTTP 400 with OCMS-4000

**Error Codes:**
- `OCMS-4000` (HTTP 400): Missing mandatory fields / Invalid input format / Duplicate notice detected
- `OCMS-5000` (HTTP 500): System error

---

#### 1.2.2 CES Webhook (Certis)
**Endpoint:** `POST /v1/cesWebhook-create-notice`

**Description:** Webhook endpoint to receive notice data from CES (Certis) system

**Request:**
- **Method:** POST
- **Content-Type:** application/json

**Request Payload (CesCreateNoticeDto):**
```json
{
  "transactionId": "string",
  "subsystemLabel": "string (8, min: 3, max: 8, must start with 030-999)",
  "anFlag": "string (1)",
  "noticeNo": "string (10)",
  "vehicleNo": "string (14)",
  "computerRuleCode": "integer",
  "compositionAmount": "decimal",
  "noticeDateAndTime": "string (datetime)",
  "offenceNoticeType": "string (1)",
  "vehicleCategory": "string (1)",
  "vehicleRegistrationType": "string (1)",
  "ppCode": "string (5)",
  "ppName": "string (100)",
  "creUserId": "string",
  "creDate": "string (datetime)",
  "createdDt": "string (datetime)"
}
```

**Validations:**
- subsystemLabel must be 3-8 characters
- First 3 digits must be numeric and in range 030-999
- Duplicate notice check performed
- Offence rule code validation

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

**Error Codes:**
- `OCMS-4000` (HTTP 400): Invalid subsystemLabel format or range / Missing mandatory fields
- `OCMS-2026` (HTTP 226): Notice number already exists / Invalid Offence Rule Code
- `OCMS-5000` (HTTP 500): Internal server error

**Note:** CES webhook uses HTTP 226 (IM Used) status code for duplicate notice and invalid rule code errors, which differs from REPCCS webhook that uses HTTP 400 for all errors.

---

#### 1.2.3 EHT SFTP Create Notice
**Endpoint:** `POST /v1/ehtsftp-create-notice`

**Description:** Notice creation endpoint for EHT SFTP system

**Request:** Same as 1.1.1
**Response:** Same as 1.1.1

---

### 1.3 Advisory Notice Specific Internal APIs

#### 1.3.1 Check AN Qualification (Internal)
**Function:** `AdvisoryNoticeHelper.checkQualification()`

**Description:** Internal function to check if a notice qualifies for Advisory Notice processing

**Input Parameters:**
```java
Map<String, Object> data {
  "dto": OffenceNoticeDto,
  "offenseType": String,
  "vehicleRegType": String
}
```

**Return:**
```java
AdvisoryNoticeResult {
  boolean qualified,
  String reasonNotQualified,
  Map<String, Object> details
}
```

**Qualification Criteria (Actual Code Implementation):**
1. Offense Type = 'O' (Offender notices only)
2. Vehicle Type in [S, D, V, I] (Local vehicles only)
3. Same-day limit check - Maximum 1 AN per vehicle per calendar day
4. Exemption rules check (Rule 20412+$80, Rule 11210+LB/HL)
5. Past offense check (24-month window) - Simplified: any past offense in 24 months qualifies

> **Note:** FD v1.2 states same-day limit check should be removed, but code still implements this check. Past offense logic is simplified in current Phase 2 implementation.

---

#### 1.3.2 Update Notice with AN Flags (Internal)
**Function:** `AdvisoryNoticeHelper.updateNoticeWithAnFlags()`

**Description:** Updates notice with AN flag after qualification check

**Input:** `String noticeNumber`

**Database Updates:**
- `an_flag` = 'Y'
- `payment_acceptance_allowed` = 'N'

---

#### 1.3.3 Check Same-Day Limit (Internal)
**Function:** `AdvisoryNoticeHelper.checkSameDayLimit()`

**Description:** Checks if vehicle already has an AN on the same day

**Input:**
- `vehicleNo`: String
- `currentNoticeDate`: LocalDateTime

**Return:** `boolean` (true if no same-day AN exists, false if already has AN today)

**Logic:**
```
Query ocms_valid_offence_notice WHERE:
  - vehicle_no = current vehicle
  - an_flag = 'Y'
  - notice_date_and_time is same calendar day as currentNoticeDate

IF record exists THEN
  RETURN false (already has AN today)
ELSE
  RETURN true (can proceed with AN)
END IF
```

> **Note:** FD v1.2 states this check should be removed, but it is still actively implemented in code (AdvisoryNoticeHelper.java:104-107, 142-182).

---

#### 1.3.4 Check Exemption Rules (Internal)
**Function:** `AdvisoryNoticeHelper.isExemptFromAN()`

**Description:** Checks if offense is exempt from AN

**Exemption Rules:**
1. **Rule 1:** Offense code 20412 with composition amount = $80
2. **Rule 2:** Offense code 11210 with vehicle category LB or HL

**Input:**
- `computerRuleCode`: Integer
- `dto`: OffenceNoticeDto

**Return:** `boolean` (true if exempt, false if not exempt)

---

#### 1.3.5 Check Past Qualifying Offense (Internal)
**Function:** `AdvisoryNoticeHelper.hasPastQualifyingOffense()`

**Description:** Checks if vehicle has qualifying offense in past 24 months

**Input:**
- `vehicleNo`: String
- `currentNoticeDate`: LocalDateTime

**Return:** `boolean` (true if has past offense, false otherwise)

**Query Window:** Current date minus 24 months

**Logic (Simplified - Phase 2 Implementation):**
```
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

> **Note:** FD v1.2 requires checking if past offenses are suspended with ANS PS reasons (CAN/CFA/DBB/VST). Current code uses simplified logic: "any past offense qualifies" (per code comment: "For Phase 2, we'll keep it simple").

---

## 2. External API Integrations

> **⚠️ Implementation Status:** The following external API integrations (Sections 2.0 - 2.4) are documented per Yi Jie standards but **NOT YET IMPLEMENTED** in current codebase. These are planned for future phases.

### 2.0 External API Standards (Yi Jie Compliance) [FUTURE PHASE]

#### 2.0.1 Token Refresh Handling

**Rule:** If token expired or invalid, the system should retry to get another refreshed token to continue. Should NOT stop processing.

| Scenario | Action | Result |
|----------|--------|--------|
| Token valid | Proceed with API call | Normal flow |
| Token expired (401) | Auto refresh token | Continue with new token |
| Token invalid | Auto refresh token | Continue with new token |
| Refresh failed | Retry refresh 3 times | If all fail, log error and alert |

**Implementation:**
```
1. Make API call with current token
2. IF response = 401 (Unauthorized) THEN
   a. Call token refresh endpoint
   b. IF refresh success THEN
      - Store new token
      - Retry original API call with new token
   c. ELSE
      - Retry refresh up to 3 times
      - IF all retries fail THEN trigger email alert
   END IF
3. Continue processing
```

#### 2.0.2 Retry Mechanism with Email Alert

**Rule:** When OCMS calls external APIs and fails to connect, it should auto retry 3 times before stopping and triggering email alert.

| Retry | Wait Time | Action |
|-------|-----------|--------|
| 1st attempt | 0s | Initial call |
| 2nd attempt | 1s | Retry with exponential backoff |
| 3rd attempt | 2s | Retry with exponential backoff |
| 4th attempt | 4s | Final retry |
| All failed | - | Stop processing, trigger email alert |

**Email Alert on Failure:**

| Field | Value |
|-------|-------|
| Subject | [OCMS-ALERT] External API Connection Failed - {API_NAME} |
| To | System administrators, Support team |
| Body | API: {endpoint}<br>Error: {error_message}<br>Timestamp: {datetime}<br>Retry Count: 3<br>Action Required: Check external system connectivity |

---

### 2.1 LTA Vehicle Ownership Check

**Purpose:** Retrieve vehicle owner information for Advisory Notices

**Integration Point:** LTA API (external system)

**Trigger:** After AN qualification check passes

**Data Retrieved:**
- Vehicle owner name
- Vehicle owner NRIC/FIN
- Vehicle registration details
- Vehicle make and model
- Road tax expiry date
- Diplomatic flag

**Error Handling:**
- Timeout: 30 seconds
- Retry: 3 attempts with exponential backoff (see Section 2.0.2)
- Email Alert: Triggered after 3 failed retries (see Section 2.0.2)
- Fallback: Manual review queue

---

### 2.2 DataHive Contact Information

**Purpose:** Retrieve vehicle owner's mobile number and email for eNotifications

**Integration Point:** DataHive API (external system)

**Trigger:** After LTA ownership check completes

**Data Retrieved:**
- Mobile phone number
- Email address

**Qualification for eNotification:**
- Mobile number OR email must be available
- Owner must NOT be in eNotification exclusion list
- Notice must NOT be suspended

**Error Handling:**
- Timeout: 30 seconds
- Retry: 3 attempts with exponential backoff (see Section 2.0.2)
- Email Alert: Triggered after 3 failed retries (see Section 2.0.2)
- Fallback: Proceed to physical letter flow

---

### 2.3 MHA Address Retrieval

**Purpose:** Retrieve vehicle owner's registered address for physical letter

**Integration Point:** MHA API (external system)

**Trigger:** When eNotification is not eligible

**Data Retrieved:**
- Registered postal address
- Building name
- Street name
- Postal code
- Unit number

**Error Handling:**
- Timeout: 30 seconds
- Retry: 3 attempts with exponential backoff (see Section 2.0.2)
- Email Alert: Triggered after 3 failed retries (see Section 2.0.2)
- Fallback: Manual review queue

---

### 2.4 SLIFT/SFTP Letter Printing

**Purpose:** Submit AN letter for physical printing and mailing

**Integration Point:** SLIFT/SFTP system (external)

**Trigger:** After address retrieval for non-eAN

**Data Submitted:**
- Notice number
- Vehicle registration number
- Offense details
- Owner's registered address
- Letter template ID

**File Format:** PDF or XML (based on SLIFT requirements)

**Error Handling:**
- Timeout: 60 seconds (file transfer)
- Retry: 3 attempts with exponential backoff (see Section 2.0.2)
- Email Alert: Triggered after 3 failed retries (see Section 2.0.2)
- Fallback: Manual review queue, letter pending status

---

## 3. Database Operations

### 3.0 Database Standards (Yi Jie Compliance)

#### 3.0.1 SQL Server Sequences

**Rule:** All numbering formats (e.g., notice number, sr_no) must use SQL Server sequences.

| Table | Field | Sequence Name |
|-------|-------|---------------|
| ocms_valid_offence_notice | notice_no | Generated by source system |
| ocms_suspended_notice | sr_no | `nextval('ocms_suspended_notice_sr_no_seq')` |
| ocms_offence_notice_detail | - | Uses notice_no from VON |

#### 3.0.2 Insert Order (Parent First, Child After)

**Rule:** When inserting related records, parent table must be updated/inserted first, followed by child tables.

**Insert Order for Notice Creation:**
1. `ocms_valid_offence_notice` (VON) - Parent table
2. `ocms_offence_notice_detail` (OND) - Child table
3. `ocms_suspended_notice` - If suspension applies

**Insert Order for Suspension:**
1. UPDATE `ocms_valid_offence_notice` (set suspension fields)
2. INSERT `ocms_suspended_notice` (create suspension record)
3. UPDATE `eocms_valid_offence_notice` (sync to internet)

#### 3.0.3 Audit User Fields

**Rule:** cre_user_id and upd_user_id cannot use "SYSTEM". Use database user instead.

| Zone | Audit User |
|------|------------|
| Intranet | `ocmsiz_app_conn` |
| Internet | `ocmsez_app_conn` |

### 3.1 Primary Tables

#### 3.1.1 ocms_valid_offence_notice (Intranet)
**Schema:** ocmsizmgr

**Key Fields for AN:**
- `notice_no` (PK): Notice number
- `an_flag`: Advisory Notice flag (Y/N)
- `payment_acceptance_allowed`: Payment acceptance flag
- `offence_notice_type`: Type of offence notice (O/U/E)
- `vehicle_registration_type`: Vehicle registration type
- `computer_rule_code`: Offense rule code
- `composition_amount`: Composition amount
- `vehicle_category`: Vehicle category
- `vehicle_no`: Vehicle registration number
- `notice_date_and_time`: Notice date and time
- `suspension_type`: Suspension type (PS/TS)
- `epr_reason_of_suspension`: Suspension reason (ANS/DBB/FOR/OLD)
- `epr_date_of_suspension`: Suspension date
- `due_date_of_revival`: Due date for revival

**Operations:**
- INSERT: Create new notice
- UPDATE: Set AN flags after qualification
- SELECT: Query for same-day limit check and past offense check

---

#### 3.1.2 eocms_valid_offence_notice (Internet)
**Schema:** ocmsez_app_conn

**Purpose:** Mirror of intranet table for public portal

**Sync Trigger:** After intranet table update

---

#### 3.1.3 ocms_suspended_notice
**Schema:** ocmsizmgr

**Key Fields:**
- `notice_no` (PK): Notice number
- `date_of_suspension` (PK): Suspension date
- `sr_no` (PK): Serial number
- `suspension_type`: PS/TS
- `reason_of_suspension`: ANS/DBB/FOR/OLD
- `suspension_source`: ocmsiz_app_conn
- `officer_authorising_suspension`: ocmsizmgr_conn
- `due_date_of_revival`: Due date for revival
- `suspension_remarks`: Remarks

**Operations:**
- INSERT: Create suspension record for PS-ANS, PS-DBB, PS-FOR, TS-OLD

---

#### 3.1.4 ocms_offence_notice_detail
**Schema:** ocmsizmgr

**Purpose:** Detailed offense information

**Key Fields:**
- `notice_no` (PK): Notice number
- Additional offense details from DTO

**Operations:**
- INSERT: Create detail record during notice creation

---

#### 3.1.5 ocms_offence_notice_owner_driver
**Schema:** ocmsizmgr

**Purpose:** Store vehicle owner particulars from LTA/DataHive/MHA

**Key Fields:**
- `notice_no` (PK): Notice number
- `owner_driver_indicator`: O (Owner) or D (Driver)
- `owner_nric_no`: Owner NRIC/FIN
- `owner_name`: Owner name
- `owner_blk_hse_no`: Block/House number
- `owner_street`: Street name
- `owner_floor`: Floor number
- `owner_unit`: Unit number
- `owner_bldg`: Building name
- `owner_postal_code`: Postal code
- `cre_user_id`: ocmsiz_app_conn
- `cre_date`: Timestamp

**Operations:**
- INSERT: Create owner record after LTA/MHA retrieval

---

#### 3.1.6 ocms_email_notification_records
**Schema:** ocmsizmgr

**Purpose:** Log email notifications sent

**Key Fields:**
- `notice_no`: Notice number
- `processing_stage`: Processing stage (RD1)
- `content`: Email content
- `date_sent`: Sent timestamp
- `email_addr`: Recipient email
- `status`: sent/failed
- `subject`: Email subject
- `cre_user_id`: ocmsiz_app_conn
- `cre_date`: Timestamp

**Operations:**
- INSERT: Create record after sending email notification

---

#### 3.1.7 ocms_sms_notification_records
**Schema:** ocmsizmgr

**Purpose:** Log SMS notifications sent

**Key Fields:**
- `notice_no`: Notice number
- `processing_stage`: Processing stage (RD1)
- `content`: SMS content
- `date_sent`: Sent timestamp
- `mobile_no`: Recipient mobile number
- `status`: sent/failed
- `cre_user_id`: ocmsiz_app_conn
- `cre_date`: Timestamp

**Operations:**
- INSERT: Create record after sending SMS notification

---

#### 3.1.8 ocms_an_letter
**Schema:** ocmsizmgr

**Purpose:** Track AN letters sent via SLIFT/SFTP

**Key Fields:**
- `notice_no`: Notice number
- `date_of_processing`: Processing date
- `date_of_an_letter`: Letter date
- `owner_nric_no`: Owner NRIC
- `owner_name`: Owner name
- `owner_id_type`: ID type (N=NRIC, F=FIN, P=Passport)
- `owner_blk_hse_no`: Block/House number
- `owner_street`: Street name
- `owner_floor`: Floor number
- `owner_unit`: Unit number
- `owner_bldg`: Building name
- `owner_postal_code`: Postal code
- `processing_stage`: Processing stage (RD1)
- `cre_user_id`: ocmsiz_app_conn
- `cre_date`: Timestamp

**Operations:**
- INSERT: Create record after sending AN letter

---

### 3.2 Query Patterns

#### Same-Day AN Check Query [STILL ACTIVE in code]
```sql
SELECT COUNT(*) FROM ocms_valid_offence_notice
WHERE vehicle_no = ?
  AND an_flag = 'Y'
  AND CAST(notice_date_and_time AS DATE) = CAST(? AS DATE)
```

> **Note:** FD v1.2 states this should be removed, but code still implements this check.

#### Past Offense Check Query (24-month window)
```sql
SELECT * FROM ocms_valid_offence_notice
WHERE vehicle_no = ?
  AND notice_date_and_time >= ? (current date - 24 months)
  AND notice_date_and_time < ? (current notice date)
```

#### Duplicate Offense Check Query
```sql
SELECT * FROM ocms_valid_offence_notice
WHERE vehicle_no = ?
  AND computer_rule_code = ?
  AND notice_date_and_time = ?
```

---

## 4. Assumptions Log

[ASSUMPTION] DataHive API response format and error codes need to be confirmed with actual API specification.

[ASSUMPTION] MHA API timeout and retry logic follow standard 30-second timeout with 3 retries.

[ASSUMPTION] SLIFT is used for encryption before SFTP upload to Toppan printing vendor (confirmed from code: ToppanLettersGeneratorJob.java).

[ASSUMPTION] eNotification exclusion list is maintained in a separate table (not found in current codebase).

[ASSUMPTION] LTA API authentication mechanism uses API key stored in Azure Key Vault.

[ASSUMPTION] Advisory Notice suspension duration (due_date_of_revival) is calculated as current date + 30 days (configurable via system parameters).

[CONFIRMED] AN SMS/Email messages are currently hardcoded in NotificationSmsEmailHelper.java (method generateAnMessages). There is a TODO comment: "Replace with actual approved template from BA/Product Owner".

[CONFIRMED] Notification records are stored in ocms_email_notification_records and ocms_sms_notification_records tables.

[CONFIRMED] AN Letter records are stored in ocms_an_letter table.

---

## 5. API Security

### 5.1 Authentication
- **REPCCS/CES:** API key authentication via Azure Key Vault
- **Staff Portal:** Session-based authentication (existing OCMS authentication)
- **External APIs (LTA/DataHive/MHA):** API key or OAuth 2.0

### 5.2 Authorization
- Staff users: Role-based access control (RBAC)
- External systems: API key validation

### 5.3 Data Masking
- Sensitive data (NRIC, mobile, email) masked in logs
- Implementation: `maskSensitiveData()` function in CreateNoticeServiceImpl

---

## 6. API Performance Requirements

### 6.1 Response Times
- Notice creation: < 3 seconds
- AN qualification check: < 2 seconds
- External API calls (LTA/DataHive/MHA): < 5 seconds each

### 6.2 Throughput
- REPCCS/CES webhooks: Support burst of 100 requests/minute
- Staff Portal: Support 50 concurrent users

### 6.3 Availability
- Target: 99.5% uptime
- Maintenance window: Weekly, off-peak hours

---

## 7. API Versioning

Current version: v1

Future versions (if needed):
- v2: Planned for enhanced eNotification features
- Versioning strategy: URL-based (/v1/, /v2/)

---

## End of Document

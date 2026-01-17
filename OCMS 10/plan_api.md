# API Planning Document - OCMS 10: Advisory Notices Processing

## Document Information
- **Version:** 1.0
- **Date:** 2026-01-09
- **Source Documents:**
  - Functional Document: v1.1_OCMS 10_Functional Document (1).md
  - Backend Code: ura-project-ocmsadminapi-5e962080c0b4
  - Key Files: AdvisoryNoticeHelper.java, CreateNoticeServiceImpl.java, CreateNoticeController.java

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
  "HTTPStatusCode": "string",
  "HTTPStatusDescription": "string",
  "data": {
    "appCode": "string",
    "message": "string",
    "noticeNo": "string"
  }
}
```

**Success Codes:**
- `OCMS-2000`: Operation completed successfully
- `OCMS-2001`: Resource created successfully

**Error Codes:**
- `OCMS-4000`: Bad request - Missing mandatory fields or invalid format
- `OCMS-4001`: Unauthorized access
- `OCMS-5000`: Internal server error
- `OCMS-5001`: Database connection failed

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

**Response:**
```json
{
  "HTTPStatusCode": "string",
  "HTTPStatusDescription": "string",
  "data": {
    "appCode": "OCMS-2000",
    "message": "OK"
  }
}
```

**Special Behavior:**
- Success response message is modified to return "OK" instead of full message
- Notice number is removed from response for REPCCS
- Duplicate check is performed before notice creation

**Error Codes:**
- `OCMS-4000`: Missing mandatory fields or invalid input format
- `OCMS-4000`: Duplicate notice detected
- `OCMS-5000`: System error

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

**Response:**
```json
{
  "HTTPStatusCode": "string",
  "HTTPStatusDescription": "string",
  "data": {
    "appCode": "string",
    "message": "string"
  }
}
```

**Error Codes:**
- `OCMS-4000`: Invalid subsystemLabel format or range
- `OCMS-2026`: Notice number already exists
- `OCMS-5000`: Internal server error

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

**Qualification Criteria:**
1. Offense Type = 'O' (Offender notices only)
2. Vehicle Type in [S, D, V, I] (Local vehicles only)
3. Same-day limit: Maximum 1 AN per vehicle per day
4. Exemption rules check
5. Past offense check (24-month window)

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

**Description:** Ensures maximum 1 AN per vehicle per calendar day

**Input:**
- `vehicleNo`: String
- `noticeDateAndTime`: LocalDateTime

**Return:** `boolean` (true if within limit, false if exceeded)

**Query:** Searches `ocms_valid_offence_notice` for:
- Same vehicle number
- `an_flag` = 'Y'
- Same calendar day

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

---

## 2. External API Integrations

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
- Retry: 3 attempts
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

---

## 3. Database Operations

### 3.1 Primary Tables

#### 3.1.1 ocms_valid_offence_notice (Intranet)
**Schema:** ocmsizmgr

**Key Fields for AN:**
- `notice_no` (PK): Notice number
- `an_flag`: Advisory Notice flag (Y/N)
- `payment_acceptance_allowed`: Payment acceptance flag
- `offense_type`: Type of offense
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
- SELECT: Query for same-day limit check, past offense check

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

### 3.2 Query Patterns

#### Same-Day AN Check Query
```sql
SELECT * FROM ocms_valid_offence_notice
WHERE vehicle_no = ?
  AND an_flag = 'Y'
  AND notice_date_and_time >= ? (start of day)
  AND notice_date_and_time <= ? (end of day)
```

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

[ASSUMPTION] SLIFT/SFTP letter format (PDF vs XML) needs confirmation from SLIFT system documentation.

[ASSUMPTION] eNotification exclusion list is maintained in a separate table (not found in current codebase).

[ASSUMPTION] LTA API authentication mechanism uses API key stored in Azure Key Vault.

[ASSUMPTION] Advisory Notice suspension duration (due_date_of_revival) is calculated as current date + 30 days (configurable via system parameters).

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

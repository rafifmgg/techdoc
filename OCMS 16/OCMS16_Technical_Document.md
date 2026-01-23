# OCMS 16 – Reduction

**Prepared by**

Guthrie GTS

---

## Version History

| Version | Updated By | Date | Changes |
| --- | --- | --- | --- |
| v1.0 | Claude | 18/01/2026 | Document Initiation |

---

## Table of Content

| Section | Content | Pages |
| --- | --- | --- |
| 1 | Reduction Initiated by PLUS | 1 |
| 1.1 | Use Case | 1 |
| 1.2 | High Level Flow | 2 |
| 2 | Reduction Process in OCMS Backend | 3 |
| 2.1 | Use Case | 3 |
| 2.2 | PLUS Apply Reduction Flow | 4 |
| 2.2.1 | API Specification | 5 |
| 2.2.2 | Data Mapping | 7 |
| 2.2.3 | Success Outcome | 8 |
| 2.2.4 | Error Handling | 9 |
| 3 | Reduction Scenarios and Outcomes | 10 |

---

# Section 1 – Reduction Initiated by PLUS

## 1.1 Use Case

1. A Reduction refers to the scenario where PLUS triggers a request to OCMS to reduce the composition amount of a notice.

2. This occurs after PLUS officers review the appeal and determine that the notice qualifies for a reduction.

3. The PLUS system performs validation to decide whether a Notice is eligible for a Reduction before the reduction request is sent to the OCMS Intranet Backend.

4. When the OCMS Intranet Backend receives a reduction request, it performs the following actions:<br>a. Verifies whether the notice has been paid to determine if a reduction is required.<br>b. Checks Computer Rule Code eligibility: If the computer rule code is 30305/31302/30302/21300, composition amount can be reduced at NPA/ROV/ENA/RD1/RD2/RR3/DN1/DN2/DR3 stage.<br>c. Checks that the last processing stage of the notice is RR3/DR3 if the computer rule code is not in the eligible list.<br>d. Updates the notice's payable amount with the reduced amount and suspends the Notice with TS-RED.

**Note:** For MVP1, the reduction process is fully managed within the PLUS system, while the OCMS backend is only responsible for updating the notice with the data provided in the reduction request.

## 1.2 High Level Flow

![High Level Flow](./images/Reduction_High_Level.png)

*Refer to Tab: Reduction_High_Level*

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | PLUS officer initiates a reduction request for a notice via PLUS Staff Portal | Entry point |
| Initiate Reduction | Officer selects notice and initiates reduction request in PLUS system | User action |
| Check Eligibility | PLUS system checks if the notice is eligible for reduction based on pre-defined rules | Pre-condition validation |
| Send Request to OCMS | If eligible, PLUS sends a request to OCMS Intranet Backend for Reduction via API | API call to OCMS |
| Route Request | Request is routed through APIM to OCMS Backend | Gateway routing |
| Receive Request | OCMS Intranet Backend receives the request | Request reception |
| Validate Notice | OCMS validates format, mandatory data, payment status, and eligibility | Backend validation |
| Process Reduction | When validation passes, OCMS updates the notice with reduced amount and TS-RED suspension | Database update |
| Response to PLUS | OCMS responds to PLUS indicating whether the reduction was successful or failed | API response |
| End | Process complete | Exit point |

---

# Section 2 – Reduction Process in OCMS Backend

## 2.1 Use Case

1. The OCMS backend performs the reduction process when it receives a reduction request from the PLUS system via API through APIM.

2. OCMS validates the request format and mandatory fields to ensure the data is complete and correct.

3. OCMS verifies whether the notice has already been paid. If it has been paid, OCMS responds to PLUS that the notice has been paid and the reduction is not applicable.

4. If the notice is outstanding, OCMS checks the eligibility based on:<br>a. Computer Rule Code: If in eligible list (30305, 31302, 30302, 21300), check if last processing stage is in extended list (NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3).<br>b. If Computer Rule Code is not in eligible list, only notices at RR3/DR3 stage are eligible for reduction.

5. If the notice is eligible, OCMS updates the notice with the reduced payable amount provided in the request and suspends the notice with a temporary suspension reason "RED".

6. OCMS returns a response to PLUS indicating whether the reduction was successfully applied or if it failed.

**Note:** Reduction of notices can be performed at the RR3/DR3 stage and at court stages from CFC to CWT. For MVP1, only the RR3/DR3 stage is included.

## 2.2 PLUS Apply Reduction Flow

![PLUS Apply Reduction Flow](./images/PLUS_Apply_Reduction.png)

*Refer to Tab: PLUS_Apply_Reduction*

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | PLUS System sends a request to OCMS Intranet Backend for Reduction via API | Entry point |
| Receive Request | OCMS Intranet Backend receives the request routed through APIM | Request reception |
| Validate Request Format | OCMS checks if the API request data is complete and valid. If the request is invalid, OCMS responds to PLUS with "Invalid format" and ends the process | Format validation |
| Check Mandatory Data | OCMS checks whether all required fields are present. If data is missing, OCMS responds to PLUS with "Missing data" and ends the process | Mandatory field check |
| Query Notice | OCMS queries the notice by notice_no from ocms_valid_offence_notice table | Database lookup |

#### Database Query for Notice Lookup

```sql
SELECT
  notice_no,
  crs_reason_of_suspension,
  computer_rule_code,
  last_processing_stage,
  composition_amount,
  suspension_type,
  epr_reason_of_suspension,
  amount_payable,
  epr_date_of_suspension,
  due_date_of_revival
FROM ocms_valid_offence_notice
WHERE notice_no = :noticeNo
```

**Note:** Never use `SELECT *` in production code. Always specify only the fields needed for the operation.
| Check Notice Exists | If notice is not found, OCMS responds to PLUS with "Notice not found" and ends the process | Existence check |
| Check Already Reduced | If notice already has TS-RED status, OCMS returns "Reduction Success" (idempotent response) without making changes | Idempotency check |
| Check Payment Status | OCMS checks the CRS Reason of Suspension field. If the value is "FP" or "PRA", the notice has been paid and OCMS responds with "Notice has been paid" | Payment validation |
| Check Computer Rule Code | If notice is outstanding, OCMS validates whether the notice's computer rule code matches any of the Eligible Computer Rule Codes (30305, 31302, 30302, 21300) | Eligibility check - Rule Code |
| Check Last Processing Stage (Extended) | For eligible codes, OCMS checks if last processing stage is in extended list (NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3). If not, respond "Notice is not eligible" | Eligibility check - Stage |
| Check Last Processing Stage (Special Case) | For non-eligible codes, OCMS checks if last processing stage is RR3 or DR3. If not, respond "Notice is not eligible" | Special case eligibility |
| Validate Amounts and Dates | OCMS validates that reduction amounts are consistent and expiry date is after reduction date | Amount/Date validation |
| Begin Transaction | OCMS begins database transaction for atomic updates | Transaction start |
| Update VON | OCMS updates ocms_valid_offence_notice with suspension_type='TS', epr_reason_of_suspension='RED', new amount_payable, epr_date_of_suspension, and due_date_of_revival | Intranet VON update |
| Insert Suspended Notice | OCMS inserts a new record into ocms_suspended_notice table to record the suspension | Suspension record |
| Insert Reduced Offence Amount | OCMS inserts a new record into ocms_reduced_offence_amount table to log the reduction | Reduction log |
| Update Internet eVON | OCMS updates eocms_valid_offence_notice with 4 fields: suspension_type, epr_reason_of_suspension, epr_date_of_suspension, amount_payable | Internet mirror |
| Check Update Success | If any update fails, OCMS performs rollback and responds with "Reduction fail" | Success validation |
| Commit Transaction | If all updates successful, OCMS commits the transaction | Transaction commit |
| Response Success | OCMS responds to PLUS with "Reduction Success" | Success response |
| End | Process complete | Exit point |

### 2.2.1 API Specification

#### API for PLUS System Integration

##### POST /v1/plus-apply-reduction

| Field | Value |
| --- | --- |
| API Name | plus-apply-reduction |
| URL | UAT: https://[uat-domain]/ocms/v1/plus-apply-reduction <br> PRD: https://[prd-domain]/ocms/v1/plus-apply-reduction |
| Description | Apply reduction to a notice by updating the payable amount and suspending with TS-RED |
| Method | POST |
| Header | `{ "Authorization": "Bearer [token]", "Content-Type": "application/json" }` |

**Request Payload:**

```json
{
  "noticeNo": "500500303J",
  "amountReduced": 55.00,
  "amountPayable": 15.00,
  "dateOfReduction": "2025-08-01T19:00:02",
  "expiryDateOfReduction": "2025-08-15T00:00:00",
  "reasonOfReduction": "RED",
  "authorisedOfficer": "JOHNLEE",
  "suspensionSource": "005",
  "remarks": "Reduction granted based on appeal review"
}
```

**Request Field Description:**

| Field | Type | Max Length | Required | Nullable | Description |
| --- | --- | --- | --- | --- | --- |
| noticeNo | string | 20 | Yes | No | Notice number to be reduced (Pattern: 9 digits + 1 uppercase letter) |
| amountReduced | decimal(10,2) | - | Yes | No | The amount reduced from the original composition amount |
| amountPayable | decimal(10,2) | - | Yes | No | The new amount payable after reduction |
| dateOfReduction | datetime | - | Yes | No | Date and time when reduction is applied (Format: yyyy-MM-dd'T'HH:mm:ss, Timezone: SGT) |
| expiryDateOfReduction | datetime | - | Yes | No | Expiry date of the reduction offer / due date of revival (Format: yyyy-MM-dd'T'HH:mm:ss, Timezone: SGT) |
| reasonOfReduction | string | 100 | Yes | No | Reason for the reduction |
| authorisedOfficer | string | 50 | Yes | No | Officer authorizing the reduction |
| suspensionSource | string | 10 | Yes | No | Source system code (e.g., "005" for PLUS) |
| remarks | string | 200 | No | Yes | Optional remarks for the reduction (max 200 chars per data dictionary) |

#### Date/Time Format Specification

**Format:** `yyyy-MM-dd'T'HH:mm:ss`

**Timezone:** All dates and times are in **Singapore Time (SGT, UTC+8)**

**Examples:**
- `2025-08-01T19:00:02` → 1 August 2025, 7:00:02 PM SGT
- `2025-08-15T00:00:00` → 15 August 2025, 12:00:00 AM SGT

**Note:** Do not include timezone offset in the timestamp string. All times are implicitly SGT.

#### Authentication and Token Handling

**Authentication:** Bearer token passed in Authorization header

**Token Expiry Handling:**
- If token is expired or invalid (HTTP 401 response)
- PLUS system is responsible for refreshing the token using refresh token flow
- PLUS should retry the request with the new token
- Processing continues seamlessly (not stopped due to token expiry)

**Token Refresh Responsibility:** PLUS system (caller)

#### Retry Policy and Error Handling

**PLUS System Retry Strategy:**

PLUS system should implement retry logic for transient failures:

| Scenario | Retry? | Strategy |
| --- | --- | --- |
| HTTP 500 (Internal Server Error) | Yes | 3 retries with exponential backoff |
| HTTP 503 (Service Unavailable) | Yes | 3 retries with exponential backoff |
| Connection Timeout | Yes | 3 retries with exponential backoff |
| Connection Refused | Yes | 3 retries with exponential backoff |
| HTTP 400 (Bad Request) | No | Business validation error - do not retry |
| HTTP 404 (Not Found) | No | Notice doesn't exist - do not retry |
| HTTP 409 (Conflict) | No | Business rule violation - do not retry |

**Retry Backoff Schedule:**
- 1st retry: Wait 1 second
- 2nd retry: Wait 2 seconds (exponential)
- 3rd retry: Wait 4 seconds (exponential)
- After 3 failures: Alert PLUS operations team via email

**OCMS Responsibility:**
- OCMS API is synchronous (request-response pattern)
- No server-side retry mechanism
- OCMS logs all requests and errors for troubleshooting
- OCMS returns appropriate HTTP status codes for caller to decide retry behavior

**Success Response (HTTP 200):**

```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Reduction Success"
  }
}
```

**Error Responses:**

| HTTP Status | Response | Description |
| --- | --- | --- |
| 400 | `{ "data": { "appCode": "OCMS-4000", "message": "Invalid format" } }` | Request data is incomplete or has invalid structure |
| 400 | `{ "data": { "appCode": "OCMS-4000", "message": "Missing data" } }` | Mandatory fields are missing |
| 404 | `{ "data": { "appCode": "OCMS-4004", "message": "Notice not found" } }` | Notice number does not exist in OCMS |
| 409 | `{ "data": { "appCode": "OCMS-4090", "message": "Notice has been paid" } }` | Notice has already been fully paid |
| 409 | `{ "data": { "appCode": "OCMS-4091", "message": "Notice is not eligible" } }` | Notice does not meet eligibility criteria |
| 500 | `{ "data": { "appCode": "OCMS-5000", "message": "Reduction fail" } }` | Database update failed or internal error |
| 503 | `{ "data": { "appCode": "OCMS-5001", "message": "System unavailable" } }` | OCMS backend cannot access database |

### 2.2.2 Data Mapping

#### Database Data Mapping

| Zone | Database Table | Field Name |
| --- | --- | --- |
| Intranet | ocms_valid_offence_notice | notice_no (WHERE clause) |
| Intranet | ocms_valid_offence_notice | suspension_type |
| Intranet | ocms_valid_offence_notice | epr_reason_of_suspension |
| Intranet | ocms_valid_offence_notice | amount_payable |
| Intranet | ocms_valid_offence_notice | epr_date_of_suspension |
| Intranet | ocms_valid_offence_notice | due_date_of_revival |
| Intranet | ocms_suspended_notice | notice_no |
| Intranet | ocms_suspended_notice | date_of_suspension |
| Intranet | ocms_suspended_notice | sr_no |
| Intranet | ocms_suspended_notice | suspension_source |
| Intranet | ocms_suspended_notice | suspension_type |
| Intranet | ocms_suspended_notice | reason_of_suspension |
| Intranet | ocms_suspended_notice | officer_authorising_suspension |
| Intranet | ocms_suspended_notice | due_date_of_revival |
| Intranet | ocms_suspended_notice | suspension_remarks |
| Intranet | ocms_reduced_offence_amount | notice_no |
| Intranet | ocms_reduced_offence_amount | date_of_reduction |
| Intranet | ocms_reduced_offence_amount | sr_no |
| Intranet | ocms_reduced_offence_amount | amount_reduced |
| Intranet | ocms_reduced_offence_amount | amount_payable |
| Intranet | ocms_reduced_offence_amount | reason_of_reduction |
| Intranet | ocms_reduced_offence_amount | expiry_date |
| Intranet | ocms_reduced_offence_amount | authorised_officer |
| Intranet | ocms_reduced_offence_amount | remarks |
| Internet | eocms_valid_offence_notice | notice_no (WHERE clause) |
| Internet | eocms_valid_offence_notice | suspension_type |
| Internet | eocms_valid_offence_notice | epr_reason_of_suspension |
| Internet | eocms_valid_offence_notice | epr_date_of_suspension |
| Internet | eocms_valid_offence_notice | amount_payable |

#### Request to Database Field Mapping

| Request Field | Database Field | Value/Transformation | Source |
| --- | --- | --- | --- |
| noticeNo | notice_no | Direct mapping (WHERE clause) | API request payload from PLUS |
| amountPayable | amount_payable | Direct mapping | API request payload from PLUS |
| dateOfReduction | epr_date_of_suspension, date_of_suspension, date_of_reduction | Direct mapping | API request payload from PLUS |
| expiryDateOfReduction | due_date_of_revival, expiry_date | Direct mapping | API request payload from PLUS |
| reasonOfReduction | reason_of_reduction | Direct mapping | API request payload from PLUS |
| authorisedOfficer | officer_authorising_suspension, authorised_officer | Direct mapping | API request payload from PLUS |
| suspensionSource | suspension_source | Direct mapping | API request payload from PLUS |
| amountReduced | amount_reduced | Direct mapping | API request payload from PLUS |
| remarks | suspension_remarks (max 200), remarks (max 200) | Direct mapping | API request payload from PLUS |
| - | suspension_type | Fixed: "TS" | System constant (Temporary Suspension) |
| - | epr_reason_of_suspension, reason_of_suspension | Fixed: "RED" | System constant (Pay Reduced Amount) |
| - | sr_no | Auto-generated | Application-level: MAX(sr_no) + 1 per notice |
| - | cre_user_id | Database user | Connection pool user: ocmsiz_app_conn |
| - | upd_user_id | Database user | Connection pool user: ocmsiz_app_conn |
| - | cre_dtm | Current timestamp | System-generated timestamp |
| - | upd_dtm | Current timestamp | System-generated timestamp |

**Note:** The due_date_of_revival field does NOT exist in the Internet database schema. Only 4 fields are synced to Internet.

#### Audit User Fields

All database insert and update operations must use the following audit user values:

| Field | Value | Zone | Notes |
| --- | --- | --- | --- |
| cre_user_id | ocmsiz_app_conn | Intranet | Database connection pool user (NOT literal string "SYSTEM") |
| upd_user_id | ocmsiz_app_conn | Intranet | Database connection pool user (NOT literal string "SYSTEM") |
| cre_dtm | CURRENT_TIMESTAMP | Both | System-generated timestamp |
| upd_dtm | CURRENT_TIMESTAMP | Both | System-generated timestamp |

**Important:** Never use the string "SYSTEM" for user ID fields. Use the database connection pool user name.

**Applies to tables:**
- ocms_valid_offence_notice (UPDATE)
- ocms_suspended_notice (INSERT)
- ocms_reduced_offence_amount (INSERT)
- eocms_valid_offence_notice (UPDATE)

### 2.2.3 Success Outcome

When reduction is successfully applied:

- Notice's suspension_type is set to "TS" (Temporary Suspension)
- Notice's epr_reason_of_suspension is set to "RED" (Pay Reduced Amount)
- Notice's amount_payable is updated with the new reduced amount
- Notice's epr_date_of_suspension is set to the date of reduction
- Notice's due_date_of_revival is set to the expiry date of reduction
- New record is created in ocms_suspended_notice table
- New record is created in ocms_reduced_offence_amount table
- Internet database eocms_valid_offence_notice is mirrored with 4 fields
- Response "Reduction Success" is returned to PLUS system

### 2.2.4 Error Handling

#### Application Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Invalid Format | Request JSON is malformed or contains incorrect data types | Return HTTP 400 with "Invalid format" |
| Missing Data | One or more mandatory fields are not provided | Return HTTP 400 with "Missing data" |
| Notice Not Found | Notice number does not exist in ocms_valid_offence_notice | Return HTTP 404 with "Notice not found" |
| Notice Paid | CRS Reason of Suspension is "FP" or "PRA" | Return HTTP 409 with "Notice has been paid" |
| Not Eligible | Notice does not meet eligibility criteria for reduction | Return HTTP 409 with "Notice is not eligible" |
| Database Error | Database operation fails during update | Rollback transaction, return HTTP 500 with "Reduction fail" |
| System Unavailable | OCMS cannot access database or system error | Return HTTP 503 with "System unavailable" |

#### API Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| Invalid Format | OCMS-4000 | Invalid format | Request data is incomplete or has invalid structure |
| Missing Data | OCMS-4000 | Missing data | Mandatory fields are missing |
| Notice Not Found | OCMS-4004 | Notice not found | Notice number does not exist in OCMS |
| Notice Paid | OCMS-4090 | Notice has been paid | Notice has already been fully paid (CRS = FP/PRA) |
| Not Eligible | OCMS-4091 | Notice is not eligible | Notice does not meet eligibility criteria |
| Database Error | OCMS-5000 | Reduction fail | Database update failed or internal error |
| System Unavailable | OCMS-5001 | System unavailable | OCMS backend cannot access database |

#### Eligibility Rules

**Eligible Computer Rule Codes:**

| Code | Allowed Processing Stages |
| --- | --- |
| 30305 | NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3 |
| 31302 | NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3 |
| 30302 | NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3 |
| 21300 | NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3 |
| Others | RR3, DR3 only (Special Case) |

#### Transaction Management

All database updates are performed within a single transaction:

1. Update ocms_valid_offence_notice
2. Insert into ocms_suspended_notice
3. Insert into ocms_reduced_offence_amount
4. Update eocms_valid_offence_notice (Internet)

If any step fails, the entire transaction is rolled back and "Reduction fail" is returned.

#### Idempotency

If a notice already has TS-RED status (suspension_type='TS' AND epr_reason_of_suspension='RED'), the API returns "Reduction Success" without making any changes. This prevents duplicate reductions from retried requests.

## 2.3 Implementation Guidance

### 2.3.1 Transaction Management

**Transaction Isolation Level:** `READ_COMMITTED`

**Transaction Scope:**
All database operations (Update VON, Insert SN, Insert ROA, Update eVON) must execute within a single transaction to ensure data consistency.

**Pseudo-code Pattern:**
```
BEGIN TRANSACTION (isolation=READ_COMMITTED, timeout=30 seconds)
  Step 1: UPDATE ocms_valid_offence_notice
  Step 2: INSERT INTO ocms_suspended_notice
  Step 3: INSERT INTO ocms_reduced_offence_amount
  Step 4: UPDATE eocms_valid_offence_notice

  IF all steps successful THEN
    COMMIT TRANSACTION
  ELSE
    ROLLBACK TRANSACTION
    Return HTTP 500 "Reduction fail"
  END IF
END TRANSACTION
```

**Rollback Strategy:**
- If ANY step fails → Rollback ALL changes
- Log error with full context (notice_no, step failed, error message)
- Return HTTP 500 with appCode OCMS-5000

### 2.3.2 Concurrency Control

**Scenario:** Two PLUS officers attempt to reduce the same notice simultaneously.

**Solution:** Optimistic Locking using version field or timestamp

**Implementation Pattern:**
```sql
UPDATE ocms_valid_offence_notice
SET suspension_type = 'TS',
    epr_reason_of_suspension = 'RED',
    amount_payable = :amountPayable,
    upd_dtm = CURRENT_TIMESTAMP,
    version = version + 1  -- Optimistic lock increment
WHERE notice_no = :noticeNo
  AND version = :expectedVersion  -- Check version hasn't changed
```

If concurrent update detected (0 rows updated):
- Return HTTP 409 "Reduction fail - concurrent modification detected"
- PLUS should retry the request with fresh data

### 2.3.3 Idempotency Implementation

**Check Strategy:**
- Combine idempotency check with initial notice lookup (single query)
- Check: `suspension_type = 'TS' AND epr_reason_of_suspension = 'RED'`
- If TRUE → Return success immediately (no database changes)
- If FALSE → Continue with validation and processing

**Performance Optimization:**
No separate query needed - idempotency check uses data already loaded for validation.

### 2.3.4 Error Logging Requirements

**For Each Error Scenario, Log:**

| Error Type | Log Level | Log Content |
| --- | --- | --- |
| Invalid Format | WARN | Sanitized request payload, validation errors |
| Missing Data | WARN | Missing field names |
| Notice Not Found | INFO | notice_no attempted |
| Notice Paid | INFO | notice_no, CRS reason value |
| Not Eligible | INFO | notice_no, computer_rule_code, last_processing_stage |
| Database Error | ERROR | Full exception stack trace, SQL statement, parameters |
| Rollback | CRITICAL | Transaction details, which step failed, notice_no |

**Log Format:**
```
[timestamp] [level] [correlation-id] [notice_no] [message]
```

### 2.3.5 Performance Requirements

| Metric | Requirement |
| --- | --- |
| Response Time (95th percentile) | < 3 seconds |
| Response Time (99th percentile) | < 5 seconds |
| API Timeout | 30 seconds |
| Concurrent Requests Capacity | 100 requests/second |
| Database Connection Pool | Min: 5, Max: 20 connections |

### 2.3.6 Validation Execution Order

**Optimize for fail-fast approach (cheapest checks first):**

```
1. Request format validation (fail fast - no DB hit)
   ↓
2. Mandatory data check (fail fast - no DB hit)
   ↓
3. Database lookup (expensive - single query loads all needed data)
   ↓
4. Idempotency check (free - data already loaded)
   ↓
5. Payment status check (free - data already loaded)
   ↓
6. Eligibility check (free - data already loaded)
   ↓
7. Amount/date validation (cheap - calculation only)
   ↓
8. Database transaction (most expensive - only if all validations pass)
```

### 2.3.7 Database Connection Strategy

**Intranet Zone:**
- Connection Pool User: `ocmsiz_app_conn`
- Schema: OCMS Intranet
- Tables: ocms_valid_offence_notice, ocms_suspended_notice, ocms_reduced_offence_amount

**Internet Zone:**
- Connection Pool User: `ocmsez_app_conn`
- Schema: OCMS Internet
- Tables: eocms_valid_offence_notice

**Note:** Both connections must participate in the same distributed transaction to ensure atomicity.

### 2.3.8 Field Constraints and Validation

| Field | Data Type | Max Length | Nullable | Default | Validation Rule |
| --- | --- | --- | --- | --- | --- |
| noticeNo | VARCHAR | 20 | No | - | Pattern: `[0-9]{9}[A-Z]` (9 digits + 1 uppercase letter) |
| amountReduced | DECIMAL | (10,2) | No | - | Must be > 0 and <= composition_amount |
| amountPayable | DECIMAL | (10,2) | No | - | Must be >= 0 |
| dateOfReduction | DATETIME | - | No | - | Valid datetime format |
| expiryDateOfReduction | DATETIME | - | No | - | Must be > dateOfReduction |
| reasonOfReduction | VARCHAR | 100 | No | - | Not blank, trimmed |
| authorisedOfficer | VARCHAR | 50 | No | - | Not blank, trimmed |
| suspensionSource | VARCHAR | 10 | No | - | Not blank (e.g., "005" for PLUS) |
| remarks | VARCHAR | 200 | Yes | NULL | Optional, trimmed if provided |

### 2.3.9 Integration Testing Approach

**Test Environment Setup:**
1. Intranet database with test data
2. Internet database with corresponding test data
3. APIM gateway configured for UAT environment
4. Test PLUS account with valid authentication token

**Key Test Scenarios:**
- **Happy Path:** Eligible notice (code 30305, stage NPA), reduction applied successfully
- **Idempotency:** Call API twice with identical payload, both return success
- **Concurrency:** Two threads reduce same notice simultaneously
- **Payment Check:** Notice with CRS="FP", should reject
- **Eligibility:** Test various rule code + stage combinations
- **Rollback:** Force database error during Step 3, verify all steps rolled back
- **Performance:** Load test with 100 concurrent requests

**Sample Test Data:**
```json
{
  "noticeNo": "500500303J",
  "amountReduced": 55.00,
  "amountPayable": 15.00,
  "dateOfReduction": "2025-08-01T19:00:02",
  "expiryDateOfReduction": "2025-08-15T00:00:00",
  "reasonOfReduction": "RED",
  "authorisedOfficer": "TESTUSER",
  "suspensionSource": "005",
  "remarks": "Test reduction for automation"
}
```

### 2.3.10 Serial Number Generation Strategy

**Sequence Generation Method:**

The `sr_no` field (serial number) for both `ocms_suspended_notice` and `ocms_reduced_offence_amount` tables is generated using an **application-level sequence strategy** rather than database-level sequences.

**Implementation:**
```sql
-- Query to get next serial number for a notice
SELECT MAX(sr_no) FROM ocms_suspended_notice WHERE notice_no = :noticeNo
-- Result: maxSrNo
-- Next sr_no = maxSrNo + 1 (or 1 if no records exist)
```

**Key Characteristics:**
1. **Notice-Scoped:** Serial numbers are unique per notice (not globally unique)
2. **Shared Value:** The same `sr_no` value is used in both `ocms_suspended_notice` and `ocms_reduced_offence_amount` for a single reduction transaction
3. **Application-Controlled:** Generated by the application layer before database insert

**Rationale:**
- **Notice-Specific Numbering:** Each notice maintains its own sequence of suspension/reduction records (sr_no starts at 1 for each notice)
- **Simpler Implementation:** Application-level MAX+1 is simpler than managing per-notice database sequences
- **Transaction Safety:** Generated within the same transaction as the inserts, ensuring consistency
- **Adequate Performance:** For the expected volume (few records per notice), MAX+1 query performance is acceptable

**Concurrency Consideration:**
- The entire reduction process is wrapped in a transaction with optimistic locking on `ocms_valid_offence_notice`
- Concurrent reductions for the same notice will be serialized by the optimistic lock, preventing duplicate sr_no values

**Example:**
```
Notice 500500303J:
- First reduction: sr_no = 1 (both in suspended_notice and reduced_offence_amount)
- Second reduction: sr_no = 2 (both tables)
- Third reduction: sr_no = 3 (both tables)
```

---

# Section 3 – Reduction Scenarios and Outcomes

The table below lists the possible scenarios and outcomes that may occur and how OCMS handles each scenario:

| Scenario | Description | Outcome | Handling |
| --- | --- | --- | --- |
| Invalid Request Format | Request received by OCMS from PLUS is incomplete or has invalid structure | Rejected by OCMS | Respond to PLUS with "Invalid format" and end processing |
| Missing Mandatory Data | Request is missing mandatory data fields | Rejected by OCMS | Respond to PLUS with "Missing data" and end processing |
| Notice Not Found | Notice number does not exist in OCMS | Rejected by OCMS | Respond to PLUS with "Notice not found" and end processing |
| Notice Already Reduced | Notice already has TS-RED status | Accepted (Idempotent) | Respond to PLUS with "Reduction Success" without changes |
| Notice Has Been Paid | The notice has already been fully paid (CRS = FP or PRA) | Rejected by OCMS | Respond to PLUS with "Notice has been paid" and end processing |
| Notice Not Eligible (Rule Code) | Notice's computer rule code is not in eligible list and stage is not RR3/DR3 | Rejected by OCMS | Respond to PLUS with "Notice is not eligible" and end processing |
| Notice Not Eligible (Stage) | Notice's computer rule code is eligible but stage is not in extended list | Rejected by OCMS | Respond to PLUS with "Notice is not eligible" and end processing |
| Invalid Amounts | Amount reduced exceeds original or amount payable calculation is incorrect | Rejected by OCMS | Respond to PLUS with "Invalid format" and end processing |
| Invalid Dates | Expiry date is before or equal to reduction date | Rejected by OCMS | Respond to PLUS with "Invalid format" and end processing |
| Successful Update | Request is valid, notice is eligible, all updates succeed | Notice updated and suspended | Respond to PLUS with "Reduction Success" and end processing |
| Update Failure | Request passes validation but update fails internally (e.g., DB error) | Update unsuccessful | Rollback all changes, respond to PLUS with "Reduction fail" |
| Partial Update | Data updated but subsequent operation fails | Incomplete processing | Rollback all changes, respond to PLUS with "Reduction fail" |
| Backend System Unavailable | OCMS cannot access the database or system error occurs | Update fails | Respond to PLUS with "System unavailable" error and end processing |

---

## Appendix: Processing Stage Descriptions

| Stage Code | Description |
| --- | --- |
| NPA | Notice Pending Action |
| ROV | Revival |
| ENA | Enforcement Action |
| RD1 | Reminder 1 |
| RD2 | Reminder 2 |
| RR3 | Reminder 3 |
| DN1 | Demand Notice 1 |
| DN2 | Demand Notice 2 |
| DR3 | Demand Reminder 3 |

---

## Appendix: CRS Reason of Suspension Values

| Value | Meaning | Can Apply Reduction? |
| --- | --- | --- |
| NULL/BLANK | Notice is outstanding | Yes |
| FP | Full Payment made | No |
| PRA | Paid (PRA status) | No |

---

*End of Document*

# Business Rules & Validation Plan: OCMS - CAS - PLUS
## CAS-OCMS Integration Conditions and Decision Trees

---

## Document Information

| Field | Value |
|-------|-------|
| **Version** | 1.0 |
| **Date** | 2025-01-14 |
| **Source** | Flowchart "Only Option (23 dec 2025 – v2)" |
| **Epic** | OCMS CAS Integration |
| **Related Documents** | plan_api.md |

---

## Overview

This document defines all business rules, validation conditions, and decision trees for the CAS-OCMS-PLUS integration. All validations are implemented on **both Frontend and Backend** unless specified otherwise.

---

## Rule ID Convention

**Format:** `RULE-{API}-{CATEGORY}-{NUMBER}`

Example: `RULE-API1-VAL-001` = API 1, Validation, Rule 001

Categories:
- `VAL` = Validation
- `BIZ` = Business Logic
- `SEC` = Security
- `EXT` = External API
- `DB` = Database

---

## API 1: Get Payment Status - Conditions

### Frontend Validations

**Pre-Request Validation (CAS Side):**

| Field | Rule ID | Validation | Error Message | Error Code |
|-------|---------|------------|---------------|------------|
| requestId | RULE-API1-VAL-001 | Required, UUID format | "Request ID is required and must be valid UUID" | OCMS-4001 |
| requestTimestamp | RULE-API1-VAL-002 | Required, ISO 8601 format | "Request timestamp is required and must be valid datetime" | OCMS-4002 |
| notices | RULE-API1-VAL-003 | Required, non-empty array, max 500 items | "Notices array is required with 1-500 items" | OCMS-4003 |
| noticeNo | RULE-API1-VAL-004 | Required, alphanumeric, max 10 chars | "Notice number must be alphanumeric and max 10 characters" | OCMS-4004 |

**Validation Flow:**
```
START
  ↓
Check requestId format (UUID)
  ↓ (invalid)
Return OCMS-4001 ←──────────────┐
  ↓ (valid)                     │
Check requestTimestamp format    │
  ↓ (invalid)                   │
Return OCMS-4002 ←──────────────┤
  ↓ (valid)                     │
Check notices array (1-500)      │
  ↓ (invalid)                   │
Return OCMS-4003 ←──────────────┤
  ↓ (valid)                     │
Check batch limit (max 500)      │
  ↓ (exceeded)                  │
Return OCMS-4131 (HTTP 413) ────┤
  ↓ (within limit)              │
For each noticeNo:              │
  ↓ (invalid)                   │
  Return OCMS-4004 ─────────────┘
  ↓ (valid)
Call API
END
```

### Backend Validations

**Authentication & Authorization:**

| Rule ID | Condition | Action | Error Code |
|---------|-----------|--------|------------|
| RULE-API1-SEC-001 | X-API-Key header missing | Return error immediately | OCMS-4011 |
| RULE-API1-SEC-002 | X-API-Key invalid | Return error immediately | OCMS-4011 |
| RULE-API1-SEC-003 | Rate limit exceeded | Return error immediately | OCMS-4291 |
| RULE-API1-SEC-004 | Batch limit exceeded (>500 notices) | Return error immediately | OCMS-4131 |

**Batch Limit Validation:**
```
IF notices.length > 500 THEN
   Return HTTP 413 Payload Too Large
   Return OCMS-4131: "Request exceeds maximum batch limit of 500 notices"
END IF
```

**Request Payload Validation:**

| Rule ID | Condition | Source | Error Code |
|---------|-----------|--------|------------|
| RULE-API1-VAL-005 | Notice number exists in database | Query: ocms_valid_offence_notice.notice_no | OCMS-4008 |

**Rate Limiting:**
```
IF request_count > 10 within 1 hour for same API key THEN
   Return OCMS-4291
END IF
```

### Business Logic Rules

**Note:** EMCON handling has been removed from API scope. EMCON notices will be handled manually by users in both OCMS and CAS systems.

**RULE-API1-BIZ-001: Suspension Information**

**Condition:**
```
suspension_type = suspension_type (from DB)
crs_reason_of_suspension = crs_reason_of_suspension (from DB)
epr_reason_of_suspension = epr_reason_of_suspension (from DB)
```

**Data Source:**
- All fields from: ocms_valid_offence_notice table

**RULE-API1-BIZ-002: Payment Information**

**Condition:**
```
payment_status = payment_status (from DB)
amount_payable = amount_payable (from DB)
amount_paid = amount_paid (from DB)
payment_allowance_flag = payment_acceptance_allowed (from DB)
```

**Data Source:**
- All fields from: ocms_valid_offence_notice table

---

## API 2: Update Court Notices - Conditions

### Frontend Validations

**Pre-Request Validation (CAS Side):**

| Field | Rule ID | Validation | Error Message | Error Code |
|-------|---------|------------|---------------|------------|
| requestId | RULE-API2-VAL-001 | Required, UUID format | "Request ID is required and must be valid UUID" | OCMS-4001 |
| requestTimestamp | RULE-API2-VAL-002 | Required, ISO 8601 format | "Request timestamp is required and must be valid datetime" | OCMS-4002 |
| notices | RULE-API2-VAL-003 | Required, non-empty array, max 500 items | "Notices array is required with 1-500 items" | OCMS-4003 |
| noticeNo | RULE-API2-VAL-004 | Required, exists in database | "Notice number must exist in system" | OCMS-4008 |
| suspensionType | RULE-API2-VAL-005 | Optional, values: "PS", "TS" | "Suspension type must be PS or TS if provided" | OCMS-4005 |
| eprReasonOfSuspension | RULE-API2-VAL-006 | Optional, values: "PS-WWP", "PS-OTH", "PS-WWC" | "EPR reason must be valid value" | OCMS-4006 |
| processingStage | RULE-API2-VAL-007 | Optional, must exist in ocms_stage_map | "Processing stage must be valid" | OCMS-4007 |

**Validation Flow:**
```
START
  ↓
Validate all fields (RULE-API2-VAL-001 to 007)
  ↓ (any invalid)
Return appropriate OCMS-4XXX error ←────────┐
  ↓ (all valid)                            │
For each notice:                          │
  ↓                                        │
  Check notice exists in DB (RULE-API2-VAL-004)
  ↓ (not exists)                           │
  Collect error (OCMS-4008) ───────────────┤
  ↓ (exists)                              │
  Check if court notice (RULE-API2-BIZ-001)
  ↓ (not court notice)                    │
  Collect error (OCMS-4008) ───────────────┤
  ↓ (is court notice)                     │
  Process update                          │
  ↓                                        │
If all successful:                        │
  Return OCMS-2000 with updated data ─────┤
If partial success:                       │
  Return OCMS-2001 with errors ───────────┘
END
```

### Backend Validations

**Authentication & Authorization:**

| Rule ID | Condition | Action | Error Code |
|---------|-----------|--------|------------|
| RULE-API2-SEC-001 | X-API-Key header missing | Return error immediately | OCMS-4011 |
| RULE-API2-SEC-002 | X-API-Key invalid | Return error immediately | OCMS-4011 |
| RULE-API2-SEC-003 | Rate limit exceeded | Return error immediately | OCMS-4291 |
| RULE-API2-SEC-004 | Batch limit exceeded (>500 notices) | Return error immediately | OCMS-4131 |

**Batch Limit Validation:**
```
IF notices.length > 500 THEN
   Return HTTP 413 Payload Too Large
   Return OCMS-4131: "Request exceeds maximum batch limit of 500 notices"
END IF
```

**Database Validation:**

| Rule ID | Condition | Source | Error Code |
|---------|-----------|--------|------------|
| RULE-API2-VAL-008 | Notice exists in ocms_valid_offence_notice | Query: notice_no | OCMS-4008 |
| RULE-API2-VAL-009 | processing_stage exists in ocms_stage_map (if provided) | Query: processing_stage | OCMS-4007 |

### Business Logic Rules

**RULE-API2-BIZ-001: Court Notice Validation**

**Condition:**
```
SELECT dayend FROM ocms_valid_offence_notice WHERE notice_no = ?

IF dayend = 'Y' THEN
   -- Is court notice, proceed with update
   Continue to RULE-API2-BIZ-002
ELSE
   -- Not court notice, return error
   Return OCMS-4008
END IF
```

**Data Source:**
- `dayend`: Source = ocms_valid_offence_notice.dayend

**Decision Tree:**
```
START: Validate court notice
  ↓
Query: SELECT dayend FROM ocms_valid_offence_notice WHERE notice_no = ?
  ↓
Is dayend = 'Y'?
  ↓ YES                         ↓ NO
  ↓                             ↓
Proceed with update           Return error OCMS-4008
  ↓                             ↓
  ↓←────────Return────────────→↓
END
```

**RULE-API2-BIZ-002: Update Court Notice**

**Condition:**
```
UPDATE ocms_valid_offence_notice
SET
  suspension_type = ?,
  epr_reason_of_suspension = ?,
  processing_stage = ?,
  upd_date = GETDATE(),
  upd_user_id = 'ocmsiz_app_conn'
WHERE notice_no = ?
```

**Data Source:**
- `suspension_type`: Source = API request
- `epr_reason_of_suspension`: Source = API request
- `processing_stage`: Source = API request
- `upd_date`: Source = GETDATE() (SQL Server function)
- `upd_user_id`: Source = 'ocmsiz_app_conn' (NOT "SYSTEM")

**Update Order (Parent-Child):**
1. Parent table: ocms_valid_offence_notice updated first
2. Child table: (if applicable) updated after

**RULE-API2-BIZ-003: Response Data Mapping**

**Condition:**
```
SELECT
  notice_no,
  amount_payable,
  payment_acceptance_allowed,
  payment_due_date
FROM ocms_valid_offence_notice
WHERE notice_no IN (updated_notices)
```

**Data Source:**
- `notice_no`: Source = ocms_valid_offence_notice.notice_no
- `amount_payable`: Source = ocms_valid_offence_notice.amount_payable
- `payment_allowance_flag`: Source = ocms_valid_offence_notice.payment_acceptance_allowed
- `court_payment_due_date`: Source = ocms_valid_offence_notice.payment_due_date

---

## API 3: Refresh VIP Vehicle - Conditions

### Frontend Validations

**Pre-Request Validation (OCMS Side):**

| Field | Rule ID | Validation | Error Message | Error Code |
|-------|---------|------------|---------------|------------|
| requestId | RULE-API3-VAL-001 | Required, UUID format | "Request ID is required and must be valid UUID" | OCMS-4001 |
| requestTimestamp | RULE-API3-VAL-002 | Required, ISO 8601 format | "Request timestamp is required and must be valid datetime" | OCMS-4002 |

### Backend Validations

**Authentication & Authorization:**

| Rule ID | Condition | Action | Error Code |
|---------|-----------|--------|------------|
| RULE-API3-SEC-001 | TLS certificate missing | Return error immediately | OCMS-4011 |
| RULE-API3-SEC-002 | TLS certificate invalid | Return error immediately | OCMS-4011 |
| RULE-API3-SEC-003 | Rate limit exceeded (1 req/day) | Return error immediately | OCMS-4291 |

### External API Conditions

**RULE-API3-EXT-001: Call CAS API**

**Pre-Call Conditions:**
- Valid TLS certificate available
- Within rate limit (1 request per day)
- Network connectivity to CAS available

**Request:**
```
POST {CAS_ENDPOINT}/getVIPVehicleList
Headers: {
  "Content-Type": "application/json",
  "X-Request-ID": {requestId},
  "Client-Certificate": {TLS_CERT}
}
Body: {
  "requestId": "...",
  "requestTimestamp": "..."
}
```

**Response Handling:**

| Response Code | Action | Retry |
|---------------|--------|-------|
| 200 OK | Process data | No |
| 401 Unauthorized | Log error, check certificate | No |
| 500 Internal Server Error | Retry (max 3 times) | Yes |
| 503 Service Unavailable | Retry (max 3 times) | Yes |

**RULE-API3-EXT-002: Retry Mechanism**

**Condition:**
```
retry_count = 0
max_retries = 3

WHILE retry_count < max_retries:
  TRY
    response = call_cas_api()
    IF response.status = 200 THEN
      Process response
      BREAK
    ELSE IF response.status IN (500, 503) THEN
      retry_count++
      WAIT (2 ^ retry_count) seconds  -- Exponential backoff
    ELSE
      Log error
      BREAK
  CATCH (connection_error)
    retry_count++
    IF retry_count = max_retries THEN
      SEND_EMAIL_ALERT()
      Log error
    END IF
    WAIT (2 ^ retry_count) seconds
  END TRY
END WHILE
```

**Email Alert Trigger:**
- After 3 failed retries
- Send to: operations team
- Subject: "CAS API Refresh VIP Vehicle Failed - All Retries Exhausted"
- Body: Include error details, timestamp, retry count

### Business Logic Rules

**RULE-API3-BIZ-001: Refresh VIP Vehicle Table**

**Condition:**
```
STEP 1: TRUNCATE existing data
TRUNCATE TABLE ocms_vip_vehicle;

STEP 2: INSERT all records from CAS
INSERT INTO ocms_vip_vehicle (
  vehicle_no,
  vip_vehicle,
  description,
  status,
  cre_date,
  cre_user_id
)
VALUES
  (?, ?, ?, ?, GETDATE(), 'ocmsiz_app_conn'),
  ... (repeat for all records)
```

**Data Source:**
- `vehicle_no`: Source = CAS API response
- `vip_vehicle`: Source = CAS API response
- `description`: Source = CAS API response
- `status`: Source = CAS API response
- `cre_date`: Source = GETDATE() (SQL Server function)
- `cre_user_id`: Source = 'ocmsiz_app_conn' (NOT "SYSTEM")

**RULE-API3-BIZ-002: Logging Strategy**

**Condition:**
```
IF job_frequency = "daily_frequent_sync" THEN
  Log to application logs only
  No batch job table logging
ELSE
  Log to application logs + batch job table
END IF
```

**Log Content:**
- Job start time (recorded immediately)
- Job end time
- Record count processed
- Success/failure status
- Error messages (if any)

---

## API 4: Query Notice Info (PLUS Integration) - Conditions

### Frontend Validations

**Pre-Request Validation (PLUS Side via IZ-APIM):**

| Field | Rule ID | Validation | Error Message | Error Code |
|-------|---------|------------|---------------|------------|
| requestId | RULE-API4-VAL-001 | Required, UUID format | "Request ID is required and must be valid UUID" | OCMS-4001 |
| requestTimestamp | RULE-API4-VAL-002 | Required, ISO 8601 format | "Request timestamp is required and must be valid datetime" | OCMS-4002 |
| notices | RULE-API4-VAL-003 | Required, non-empty array, max 100 items | "Notices array is required with 1-100 items" | OCMS-4003 |
| noticeNo | RULE-API4-VAL-004 | Required, alphanumeric, max 10 chars | "Notice number must be alphanumeric and max 10 characters" | OCMS-4004 |

### Backend Validations

**Authentication & Authorization:**

| Rule ID | Condition | Action | Error Code |
|---------|-----------|--------|------------|
| RULE-API4-SEC-001 | Authorization header missing | Return error immediately | OCMS-4011 |
| RULE-API4-SEC-002 | OAuth token invalid/expired | Attempt token refresh, return error if failed | OCMS-4011 |
| RULE-API4-SEC-003 | Rate limit exceeded | Return error immediately | OCMS-4291 |
| RULE-API4-SEC-004 | Batch limit exceeded (>100 notices) | Return error immediately | OCMS-4131 |

**Batch Limit Validation:**
```
IF notices.length > 100 THEN
   Return HTTP 413 Payload Too Large
   Return OCMS-4131: "Request exceeds maximum batch limit of 100 notices"
END IF
```

**Rate Limiting:**
```
IF request_count > 100 within 1 minute for same token THEN
   Return OCMS-4291
END IF
```

### External API Conditions

**RULE-API4-EXT-001: OAuth Token Handling**

**Token Refresh Logic:**
```
TRY
  response = call_ocms_api()
  IF response.status = 401 AND token_expired THEN
    -- Auto refresh token
    new_token = refresh_oauth_token()
    IF new_token IS NOT NULL THEN
      -- Update Authorization header
      Retry original request with new_token
    ELSE
      Return OCMS-4011
    END IF
  ELSE
    Return response
  END IF
CATCH (token_refresh_error)
  Return OCMS-4011
END TRY
```

**Token Refresh Endpoint:** (TBD from IZ-APIM team)

**RULE-API4-EXT-002: Retry Mechanism**

**Condition:**
```
retry_count = 0
max_retries = 3

WHILE retry_count < max_retries:
  TRY
    response = call_ocms_api()
    IF response.status = 200 THEN
      Process response
      BREAK
    ELSE IF response.status IN (500, 502, 503, 504) THEN
      retry_count++
      WAIT (2 ^ retry_count) seconds  -- Exponential backoff
    ELSE
      Return response error
      BREAK
  CATCH (connection_error)
    retry_count++
    IF retry_count = max_retries THEN
      SEND_EMAIL_ALERT()
      Log error
      Return OCMS-5001
    END IF
    WAIT (2 ^ retry_count) seconds
  END TRY
END WHILE
```

**Email Alert Trigger:**
- After 3 failed retries
- Send to: operations team
- Subject: "OCMS API Query Notice Info Failed - All Retries Exhausted"
- Body: Include error details, timestamp, request_id

### Business Logic Rules

**RULE-API4-BIZ-001: Payment Allowance Calculation**

**Condition:**
```
SELECT
  notice_no,
  payment_due_date,
  payment_acceptance_allowed,
  amount_payable
FROM ocms_valid_offence_notice
WHERE notice_no IN (notice_numbers)

-- Calculate payment_allowance_flag
FOR each notice:
  IF GETDATE() > payment_due_date THEN
    payment_allowance_flag = 'N'  -- Past due date
  ELSE
    payment_allowance_flag = payment_acceptance_allowed
  END IF
END FOR
```

**Data Source:**
- `notice_no`: Source = ocms_valid_offence_notice.notice_no
- `court_payment_due_date`: Source = ocms_valid_offence_notice.payment_due_date
- `payment_allowance_flag`: Source = Calculated (GETDATE() > payment_due_date)
- `amount_payable`: Source = ocms_valid_offence_notice.amount_payable

**Decision Tree:**
```
START: Calculate payment allowance
  ↓
Query notice details from ocms_valid_offence_notice
  ↓
Get current date: GETDATE()
  ↓
Compare: current_date > payment_due_date?
  ↓ YES                           ↓ NO
  ↓                               ↓
payment_allowance_flag = 'N'    payment_allowance_flag = payment_acceptance_allowed
(Past due date, not payable)    (Use DB flag)
  ↓                               ↓
  ↓←────────Return to PLUS───────↓
END
```

**RULE-API4-BIZ-002: Court Payment Due Date Mapping**

**Condition:**
```
court_payment_due_date = payment_due_date
```

**Data Source:**
- `court_payment_due_date`: Source = ocms_valid_offence_notice.payment_due_date

**RULE-API4-BIZ-003: Persist Court Payment Due Date**

**Condition:**
```
-- Insert or update court payment due date tracking in ocms_court_case
MERGE ocms_court_case AS target
USING (
  SELECT
    notice_no,
    payment_due_date AS court_payment_due_date
  FROM ocms_valid_offence_notice
  WHERE notice_no IN (notice_numbers)
) AS source
ON (target.notice_no = source.notice_no)
WHEN MATCHED THEN
  UPDATE SET
    court_payment_due_date = source.court_payment_due_date,
    upd_date = GETDATE(),
    upd_user_id = 'ocmsiz_app_conn'
WHEN NOT MATCHED THEN
  INSERT (notice_no, court_payment_due_date, cre_date, cre_user_id)
  VALUES (source.notice_no, source.court_payment_due_date, GETDATE(), 'ocmsiz_app_conn');
```

**Data Source:**
- `notice_no`: Source = ocms_valid_offence_notice.notice_no
- `court_payment_due_date`: Source = ocms_valid_offence_notice.payment_due_date
- `cre_date`: Source = GETDATE() (SQL Server function)
- `cre_user_id`: Source = 'ocmsiz_app_conn' (database connection user)
- `upd_date`: Source = GETDATE() (SQL Server function)
- `upd_user_id`: Source = 'ocmsiz_app_conn' (database connection user)

**Decision Tree:**
```
START: Persist court payment due date
  ↓
Query notice details from ocms_valid_offence_notice
  ↓
MERGE into ocms_court_case
  ↓
Record exists in ocms_court_case?
  ↓ YES                              ↓ NO
  ↓                                  ↓
UPDATE existing record              INSERT new record
  ↓                                  ↓
  ↓←────────────Merge────────────────↓
  ↓
Continue to return response to PLUS
END
```

---

## Cross-Cutting Business Rules

### RULE-CROSS-001: Audit User Fields

**Condition:**
```
For INSERT operations:
  cre_user_id = 'ocmsiz_app_conn'  -- Intranet
  -- OR --
  cre_user_id = 'ocmsez_app_conn'  -- Internet

For UPDATE operations:
  upd_user_id = 'ocmsiz_app_conn'  -- Intranet
  -- OR --
  upd_user_id = 'ocmsez_app_conn'  -- Internet

NEVER use "SYSTEM" as user_id
```

**Data Source:**
- `cre_user_id`: Source = Database connection user
- `upd_user_id`: Source = Database connection user

### RULE-CROSS-002: SQL Query Best Practice

**Condition:**
```
DO NOT use:
  SELECT * FROM table_name

ALWAYS specify required fields:
  SELECT field1, field2, field3 FROM table_name
```

**Rationale:**
- Performance optimization
- Network bandwidth reduction
- Clear data mapping

### RULE-CROSS-003: Error Response Format

**Condition:**
```
ALL error responses must follow:
{
  "appCode": "OCMS-XXXX",
  "message": "Clear, actionable error message"
}

Success responses must follow:
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Success",
    ...additional fields
  }
}
```

### RULE-CROSS-004: Data Source Attribution

**Condition:**
```
EVERY data field in documentation MUST specify source:

Format examples:
- "Source: table_name.column_name"
- "Source: Calculated (formula/logic)"
- "Source: API response field"
- "Source: GETDATE() function"
- "Source: Standard code table"
```

---

## Decision Trees Summary

### Decision Tree 1: API Request Validation

```
                    START API Request
                            ↓
                    Validate all required fields
                            ↓
                      ┌─────┴─────┐
                 Invalid            Valid
                      ↓              ↓
              Return OCMS-4XXX   Check authentication
                            ↓
                      ┌─────┴─────┐
                 Auth failed       Auth success
                      ↓              ↓
              Return OCMS-4011  Check rate limit
                            ↓
                      ┌─────┴─────┐
              Rate limited         Within limit
                      ↓              ↓
              Return OCMS-4291  Process request
                            ↓
                        Return response
                            ↓
                          END
```

### Decision Tree 2: Batch Limit Validation

```
                START: Validate batch size
                            ↓
            Get notices array length
                            ↓
            ┌─────────────────┴─────────────────┐
   API 1 or API 2 (limit 500)           API 4 (limit 100)
                            ↓                     ↓
            notices.length > 500?      notices.length > 100?
                            ↓                     ↓
            ┌─────┴─────┐              ┌─────┴─────┐
          YES          NO            YES          NO
            ↓           ↓              ↓           ↓
    Return HTTP 413   Continue    Return HTTP 413  Continue
    OCMS-4131        processing   OCMS-4131       processing
            ↓           ↓              ↓           ↓
            └───────────┴──────────────┴───────────┘
                            ↓
                          END
```

### Decision Tree 3: Payment Allowance for PLUS

```
                START: PLUS query notice
                            ↓
            Query notice details from DB
                            ↓
            Calculate: current_date > payment_due_date?
                            ↓
          ┌───────────────────┴───────────────────┐
   YES (past due date)                      NO (within due date)
            ↓                                   ↓
   payment_allowance_flag = 'N'        payment_allowance_flag =
   (NOT payable)                         payment_acceptance_allowed
            ↓                                   ↓
            └───────────────────┬───────────────┘
                            ↓
              Return notice info to PLUS
                            ↓
                          END
```

---

## Assumptions Log

| ID | Assumption | Category | Impact | Status |
|----|------------|----------|--------|--------|
| ASSUMPTION-001 | Court notice identified by dayend = 'Y' | Database | High - affects API 2 validation | ⚠️ Pending confirmation |
| ASSUMPTION-002 | CAS API for VIP vehicle returns exactly 4 fields: vehicle_no, vip_vehicle, description, status | External API | Medium - affects ocms_vip_vehicle schema | ⚠️ Pending confirmation |
| ASSUMPTION-003 | IZ-APIM handles OAuth token refresh automatically or provides refresh endpoint | External API | High - affects API 4 implementation | ⚠️ Pending confirmation |
| ASSUMPTION-004 | Rate limits are per API key/token, not per IP | Security | Low - affects rate limiting logic | ⚠️ Pending confirmation |
| ASSUMPTION-005 | Batch limits of 500 (API 1&2) and 100 (API 4) are sufficient for expected load | Performance | Medium - need load testing | ⚠️ Pending confirmation |
| ASSUMPTION-006 | EMCON handling is done manually by users in both OCMS and CAS systems | Business Logic | N/A - confirmed removed from API scope | ✅ Confirmed |

---

## Validation Matrix

### API 1: Get Payment Status

| Layer | Validation | Rule ID | Error Code |
|-------|------------|---------|------------|
| FE | UUID format check | RULE-API1-VAL-001 | OCMS-4001 |
| FE | ISO 8601 datetime check | RULE-API1-VAL-002 | OCMS-4002 |
| FE | Array size 1-500 | RULE-API1-VAL-003 | OCMS-4003 |
| FE | Alphanumeric max 10 chars | RULE-API1-VAL-004 | OCMS-4004 |
| BE | API key validation | RULE-API1-SEC-001/002 | OCMS-4011 |
| BE | Rate limit check | RULE-API1-SEC-003 | OCMS-4291 |
| BE | Batch limit check (max 500) | RULE-API1-SEC-004 | OCMS-4131 |
| BE | Notice exists in DB | RULE-API1-VAL-005 | OCMS-4008 |

### API 2: Update Court Notices

| Layer | Validation | Rule ID | Error Code |
|-------|------------|---------|------------|
| FE | UUID format check | RULE-API2-VAL-001 | OCMS-4001 |
| FE | ISO 8601 datetime check | RULE-API2-VAL-002 | OCMS-4002 |
| FE | Array size 1-500 | RULE-API2-VAL-003 | OCMS-4003 |
| FE | Notice exists in DB | RULE-API2-VAL-004 | OCMS-4008 |
| FE | Valid suspension type | RULE-API2-VAL-005 | OCMS-4005 |
| FE | Valid EPR reason | RULE-API2-VAL-006 | OCMS-4006 |
| FE | Valid processing stage | RULE-API2-VAL-007 | OCMS-4007 |
| BE | API key validation | RULE-API2-SEC-001/002 | OCMS-4011 |
| BE | Batch limit check (max 500) | RULE-API2-SEC-004 | OCMS-4131 |
| BE | Court notice check | RULE-API2-BIZ-001 | OCMS-4008 |

### API 3: Refresh VIP Vehicle

| Layer | Validation | Rule ID | Error Code |
|-------|------------|---------|------------|
| FE | UUID format check | RULE-API3-VAL-001 | OCMS-4001 |
| FE | ISO 8601 datetime check | RULE-API3-VAL-002 | OCMS-4002 |
| BE | TLS certificate validation | RULE-API3-SEC-001/002 | OCMS-4011 |
| BE | Rate limit check (1/day) | RULE-API3-SEC-003 | OCMS-4291 |
| EXT | CAS API retry (3 times) | RULE-API3-EXT-002 | Email alert |

### API 4: Query Notice Info (PLUS)

| Layer | Validation | Rule ID | Error Code |
|-------|------------|---------|------------|
| FE | UUID format check | RULE-API4-VAL-001 | OCMS-4001 |
| FE | ISO 8601 datetime check | RULE-API4-VAL-002 | OCMS-4002 |
| FE | Array size 1-100 | RULE-API4-VAL-003 | OCMS-4003 |
| FE | Alphanumeric max 10 chars | RULE-API4-VAL-004 | OCMS-4004 |
| BE | OAuth token validation | RULE-API4-SEC-001/002 | OCMS-4011 |
| BE | Rate limit check (100/min) | RULE-API4-SEC-003 | OCMS-4291 |
| BE | Batch limit check (max 100) | RULE-API4-SEC-004 | OCMS-4131 |
| BE | Payment allowance calculation | RULE-API4-BIZ-001 | Business logic |
| BE | Court payment due date persist | RULE-API4-BIZ-003 | Database operation |
| EXT | OAuth token auto refresh | RULE-API4-EXT-001 | Auto retry |
| EXT | API retry (3 times) | RULE-API4-EXT-002 | Email alert |

---

## Error Code Reference

| App Code | HTTP Status | Message | Triggered By |
|----------|-------------|---------|--------------|
| OCMS-2000 | 200 | Success | All APIs |
| OCMS-2001 | 207 | Partial success | API 2 |
| OCMS-4001 | 400 | Invalid request payload | All APIs (validation) |
| OCMS-4002 | 400 | Invalid request timestamp format | All APIs (validation) |
| OCMS-4003 | 400 | Notices array invalid | All APIs (validation) |
| OCMS-4004 | 400 | Invalid notice number format | All APIs (validation) |
| OCMS-4005 | 400 | Invalid suspension type | API 2 |
| OCMS-4006 | 400 | Invalid EPR reason | API 2 |
| OCMS-4007 | 400 | Invalid processing stage | API 2 |
| OCMS-4008 | 404 | Notice not found or not court notice | API 1, API 2 |
| OCMS-4011 | 401 | Invalid or missing credentials | All APIs (auth) |
| OCMS-4131 | 413 | Request exceeds maximum batch limit | API 1 (500), API 2 (500), API 4 (100) |
| OCMS-4291 | 429 | Rate limit exceeded | All APIs (rate limit) |
| OCMS-5001 | 500 | Internal server error | All APIs (system) |
| OCMS-5031 | 503 | Service unavailable | API 3 (CAS down) |

---

## Open Questions

| ID | Question | Related Rule | Assigned To |
|----|----------|--------------|-------------|
| Q1 | Which database field identifies court notice (dayend or other)? | RULE-API2-BIZ-001 | Data Team |
| Q2 | Exact field structure of CAS VIP vehicle API response? | RULE-API3-BIZ-001 | CAS Team |
| Q3 | IZ-APIM OAuth token endpoint and refresh mechanism? | RULE-API4-EXT-001 | API Gateway Team |
| Q4 | Are batch limits of 500/100 sufficient for expected load? | RULE-API1-SEC-004, RULE-API2-SEC-004, RULE-API4-SEC-004 | DevOps Team |

---

## Next Steps

1. ✅ Complete plan_condition.md (following Yi Jie guidelines)
2. ⏳ Get confirmation on assumptions from relevant teams
3. ⏳ Create plan_flowchart.md (system flows)
4. ⏳ Design flowchart in .drawio format
5. ⏳ Generate technical documentation

---

**Document End**

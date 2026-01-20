# API Plan - OCMS 17 Temporary Suspension

**Document Information**
- Version: 1.0
- Date: 10/01/2026
- Source: OCMS 17 Functional Document, OCMS 18 Reference

---

## 1. Internal APIs (for eService)

These APIs are provided by OCMS Backend for OCMS Staff Portal.

### API 1: Get Valid Offence Notice List

**Purpose:** Search and retrieve notices for TS operation

**Endpoint:** POST /v1/validoffencenoticelist

**Request:**
```json
{
  "$skip": 0,
  "$limit": 10,
  "$field": "noticeNo, vehicleNo, lastProcessingStage, suspensionType, reasonOfSuspension, noticeType, amountPaid",
  "noticeNo": "500500303J",
  "vehicleNo": "SBA1234A",
  "lastProcessingStage": "RD1",
  "suspensionType": "TS"
}
```

**Response Success:**
```json
{
  "appCode": "OCMS-2000",
  "message": "Success",
  "data": [
    {
      "noticeNo": "500500303J",
      "vehicleNo": "SBA1234A",
      "lastProcessingStage": "RD1",
      "suspensionType": null,
      "reasonOfSuspension": null,
      "noticeType": "PN",
      "amountPaid": 0.00
    }
  ],
  "pagination": {
    "skip": 0,
    "limit": 10,
    "totalRecords": 1
  }
}
```

**Response Empty:**
```json
{
  "appCode": "OCMS-2000",
  "message": "Success",
  "data": [],
  "pagination": {
    "skip": 0,
    "limit": 10,
    "totalRecords": 0
  }
}
```

**Response Failure:**
```json
{
  "appCode": "OCMS-4000",
  "message": "Invalid search parameters"
}
```

**Database Query:**
```sql
-- Note: reason_of_suspension is in ocms_suspended_notice (child table), not in parent table
SELECT von.notice_no, von.vehicle_no, von.last_processing_stage,
       von.suspension_type, von.offence_notice_type, von.amount_paid,
       sn.reason_of_suspension
FROM ocms_valid_offence_notice von
LEFT JOIN ocms_suspended_notice sn
  ON von.notice_no = sn.notice_no
  AND sn.sr_no = (SELECT MAX(sr_no) FROM ocms_suspended_notice WHERE notice_no = von.notice_no)
WHERE von.notice_no IN (:noticeNo)
  AND von.last_processing_stage IN (:stages)

-- Note: Supports up to 10 notice numbers per request
-- amount_paid from von table (NOT amount_collected - field doesn't exist)
-- reason_of_suspension from sn table (latest suspension record)
```

---

### API 2: Get Suspension Reason List

**Purpose:** Get list of TS codes for dropdown selection

**Endpoint:** POST /v1/suspensionreasonlist

**Request:**
```json
{
  "suspensionType": "TS",
  "status": "A"
}
```

**Response Success:**
```json
{
  "appCode": "OCMS-2000",
  "message": "Success",
  "data": [
    {
      "code": "ACR",
      "description": "ACRA",
      "suspensionDays": 21,
      "looping": false,
      "allowedStages": ["ROV", "ENA", "RD1", "RD2", "RR3", "DN1", "DN2", "DR3", "CPC", "CFC"]
    },
    {
      "code": "APE",
      "description": "Appeal Extension",
      "suspensionDays": 21,
      "looping": false,
      "allowedStages": ["NPA", "ROV", "ENA", "RD1", "RD2", "RR3", "DN1", "DN2", "DR3", "CPC", "CFC"]
    },
    {
      "code": "CLV",
      "description": "Classified Vehicles",
      "suspensionDays": 90,
      "looping": true,
      "allowedStages": ["RR3", "DR3"]
    },
    {
      "code": "HST",
      "description": "House Tenants",
      "suspensionDays": 30,
      "looping": true,
      "allowedStages": ["ROV", "ENA", "RD1", "RD2", "RR3", "DN1", "DN2", "DR3", "CPC", "CFC"]
    }
  ]
}
```

**Response Empty:**
```json
{
  "appCode": "OCMS-2000",
  "message": "Success",
  "data": []
}
```

**Response Failure:**
```json
{
  "appCode": "OCMS-5000",
  "message": "Unable to retrieve suspension reason list"
}
```

**Database Query:**
```sql
-- For OCMS Staff Portal (suspension_source = 'OCMS_FE')
SELECT reason_of_suspension, description, no_of_days_for_revival,
       suspension_source, status
FROM ocms_suspension_reason
WHERE suspension_type = 'TS'
  AND status = 'A'
  AND suspension_source = 'OCMS_FE'
ORDER BY reason_of_suspension

-- Note: looping and allowed_stages are NOT in DB - hardcoded in application
```

**Note:** Empty response handling - UI should display "No suspension codes available"

---

### API 3: Apply Suspension (Staff Portal)

**Purpose:** Apply TS manually from OCMS Staff Portal

**Endpoint:** POST /v1/staff-apply-suspension

**Request:**
```json
{
  "noticeNo": ["500500303J", "500500304K"],
  "suspensionType": "TS",
  "reasonOfSuspension": "ACR",
  "suspensionRemarks": "ACRA investigation pending",
  "officerAuthorisingSuspension": "JOHNLEE"
}
```

**Request Parameters:**
| Field | Type | Required | Max Length | Description |
|-------|------|----------|------------|-------------|
| noticeNo | Array[String] | Yes | - | List of notice numbers. Max 10 per batch |
| suspensionType | String | Yes | 2 | "TS" for Temporary Suspension |
| reasonOfSuspension | String | Yes | 3 | TS Code (ACR, APE, APP, etc.) |
| suspensionRemarks | String | No | 200 | Remarks for the suspension |
| officerAuthorisingSuspension | String | Yes | 50 | OIC username |
| checking | Boolean | No | - | Dry-run mode: validate only without applying TS (default: false) |

**Response Success:**
```json
{
  "totalProcessed": 2,
  "successCount": 2,
  "errorCount": 0,
  "results": [
    {
      "noticeNo": "500500303J",
      "srNo": 1234,
      "dueDateOfRevival": "2025-04-12T19:00:02",
      "appCode": "OCMS-2000",
      "message": "TS Success"
    },
    {
      "noticeNo": "500500304K",
      "srNo": 1235,
      "dueDateOfRevival": "2025-04-12T19:00:02",
      "appCode": "OCMS-2000",
      "message": "TS Success"
    }
  ]
}
```

**Response Partial Success:**
```json
{
  "totalProcessed": 2,
  "successCount": 1,
  "errorCount": 1,
  "results": [
    {
      "noticeNo": "500500303J",
      "srNo": 1234,
      "dueDateOfRevival": "2025-04-12T19:00:02",
      "appCode": "OCMS-2000",
      "message": "TS Success"
    },
    {
      "noticeNo": "500500304K",
      "appCode": "OCMS-4002",
      "message": "TS Code cannot be applied due to Last Processing Stage"
    }
  ]
}
```

**Response Failure:**
```json
{
  "totalProcessed": 1,
  "successCount": 0,
  "errorCount": 1,
  "results": [
    {
      "noticeNo": "500500303J",
      "appCode": "OCMS-4002",
      "message": "TS Code cannot be applied due to Last Processing Stage"
    }
  ]
}
```

**Database Operations:**
```sql
-- Step 1: Validate notice eligibility
-- Note: reason_of_suspension from child table (ocms_suspended_notice)
SELECT von.notice_no, von.last_processing_stage,
       von.suspension_type, sn.reason_of_suspension
FROM ocms_valid_offence_notice von
LEFT JOIN ocms_suspended_notice sn
  ON von.notice_no = sn.notice_no
  AND sn.sr_no = (SELECT MAX(sr_no) FROM ocms_suspended_notice WHERE notice_no = von.notice_no)
WHERE von.notice_no = ?

-- Step 2: Check TS code rules and source authorization
SELECT reason_of_suspension, description, no_of_days_for_revival,
       suspension_source, status
FROM ocms_suspension_reason
WHERE reason_of_suspension = ?
  AND suspension_type = 'TS'
  AND status = 'A'
  AND suspension_source = 'OCMS_FE'  -- Source must be authorized

-- Step 3: Update parent table
-- Note: reason_of_suspension is NOT stored in this table - it goes in ocms_suspended_notice
UPDATE ocms_valid_offence_notice
SET suspension_type = 'TS',
    -- Note: reason_of_suspension stored in ocms_suspended_notice, not here
    due_date_of_revival = CURRENT_TIMESTAMP + INTERVAL ? DAY,
    is_sync = 'N',
    upd_date = CURRENT_TIMESTAMP,
    upd_user_id = ?
WHERE notice_no = ?

-- Step 4: Insert suspension record (use nextval for sr_no)
INSERT INTO ocms_suspended_notice
(notice_no, sr_no, suspension_source,
 suspension_type, reason_of_suspension,
 officer_authorising_suspension,
 due_date_of_revival, suspension_remarks,
 date_of_suspension, cre_date, cre_user_id,
 process_indicator)
VALUES (?, nextval, 'OCMS',
 'TS', ?,
 ?,
 CURRENT_TIMESTAMP + INTERVAL ? DAY, ?,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?,
 'manual')
```

---

### API 4: Get User List

**Purpose:** Get list of users for dropdown filters (for OIC selection)

**Endpoint:** POST /v1/userlist

**Request:**
```json
{
  "status": "A"
}
```

**Response Success:**
```json
{
  "appCode": "OCMS-2000",
  "message": "Success",
  "data": [
    {
      "userId": "JOHNLEE",
      "firstName": "John",
      "lastName": "Lee"
    },
    {
      "userId": "MARYTAN",
      "firstName": "Mary",
      "lastName": "Tan"
    }
  ]
}
```

**Response Empty:**
```json
{
  "appCode": "OCMS-2000",
  "message": "Success",
  "data": []
}
```

**Database Query:**
```sql
SELECT user_id, first_name, last_name
FROM ocms_user
WHERE status = 'A'
ORDER BY first_name, last_name
```

---

## 2. External APIs (API Provide to PLUS)

These APIs are provided by OCMS for PLUS system.

### API 5: Apply Temporary Suspension (from PLUS)

**Purpose:** PLUS applies TS to notices via API

**Endpoint:** POST /v1/plus-apply-suspension

**Request:**
```json
{
  "notice_no": "500500303J",
  "date_of_suspension": "2025-03-22T19:00:02",
  "suspension_source": "PLUS",
  "suspension_type": "TS",
  "reason_of_suspension": "APP",
  "officer_authorising_suspension": "PLMJOHN",
  "suspension_remarks": "Appeal under review"
}
```

**Request Parameters:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| notice_no | String | Yes | Notice number |
| date_of_suspension | String (DateTime) | Yes | Suspension date (ISO 8601) |
| suspension_source | String | No | Default: "PLUS" |
| suspension_type | String | Yes | Must be "TS" |
| reason_of_suspension | String | Yes | TS Code (APE, APP, CCE, PRI, RED) |
| officer_authorising_suspension | String | Yes | PLM username |
| suspension_remarks | String | No | Remarks (max 200 chars) |

**Response Success:**
```json
{
  "totalProcessed": 1,
  "successCount": 1,
  "errorCount": 0,
  "results": [
    {
      "noticeNo": "500500303J",
      "srNo": 1234,
      "dueDateOfRevival": "2025-04-12T19:00:02",
      "appCode": "OCMS-2000",
      "message": "TS Success"
    }
  ]
}
```

**Response Failure:**
```json
{
  "totalProcessed": 1,
  "successCount": 0,
  "errorCount": 1,
  "results": [
    {
      "noticeNo": "500500303J",
      "appCode": "OCMS-4000",
      "message": "Source not authorized to use this Suspension Code"
    }
  ]
}
```

**Database Operations:**
Same as API 3, but with:
- suspension_source = 'PLUS'
- Validate suspension_source = 'PLUS' (source must be authorized)
- Use ocmsez_app_conn for Internet zone

---

## 3. Error Codes

### Standard Error Codes

| Code | Category | Description | User Message |
|------|----------|-------------|--------------|
| OCMS-2000 | Success | Success | Success |
| OCMS-2001 | Success | Already TS / Later Revival Date | Notice already has TS with later revival date |
| OCMS-4000 | Client Error | Bad Request / Validation Failed | Invalid request. Please check and try again. |
| OCMS-4001 | Client Error | Invalid Notice / Unauthorized | Notice not found or invalid. |
| OCMS-4002 | Client Error | Business Rule Violation | TS cannot be applied due to validation rules. |
| OCMS-4003 | Client Error | Notice Paid | TS cannot be applied to paid notices. |
| OCMS-4007 | Client Error | System Error | System error occurred during processing. |
| OCMS-5000 | Server Error | Internal Server Error | Something went wrong. Please try again later. |

### Validation Error Codes (OCMS-4002 variants)

| Scenario | App Error Code | Message |
|----------|----------------|---------|
| Invalid Processing Stage | OCMS-4002 | TS Code cannot be applied due to Last Processing Stage |
| Source not authorized | OCMS-4000 | Source not authorized to use this Suspension Code |
| Notice already has TS (same/later revival) | OCMS-2001 | Notice already has active TS with same or later revival date |
| Invalid TS code | OCMS-4000 | Invalid suspension code |
| TS code not active | OCMS-4000 | Suspension code is not active |
| Notice not found | OCMS-4001 | Notice not found |
| Notice paid/partially paid | OCMS-4003 | TS cannot be applied to paid notices |
| PS does not allow TS | OCMS-4002 | TS cannot be applied due to existing PS |

---

## 4. API Authentication

All APIs require JWT Bearer token in header:

```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

For PLUS APIs, additional APIM subscription key required:

```
Ocp-Apim-Subscription-Key: <APIM_KEY>
```

### 4.1 Token Refresh Handling (Yi Jie Standard)

**Rule:** If token expired or invalid, the system should retry to get another refreshed token to continue. Should NOT stop processing.

| Scenario | Action | Result |
|----------|--------|--------|
| Token valid | Proceed with API call | Normal flow |
| Token expired | Auto refresh token | Continue processing with new token |
| Token invalid | Auto refresh token | Continue processing with new token |
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
      - IF all retries fail THEN log error
   END IF
3. Continue processing
```

---

## 5. External API Integration (Yi Jie Standard)

### 5.1 Retry Mechanism

**Rule:** When OCMS calls external APIs (e.g., PLUS API) and fails to connect, it should auto retry 3 times before stopping and triggering email alert.

| Retry | Wait Time | Action |
|-------|-----------|--------|
| 1st attempt | 0s | Initial call |
| 2nd attempt | 5s | Retry after 5 seconds |
| 3rd attempt | 10s | Retry after 10 seconds |
| 4th attempt | 15s | Final retry |
| All failed | - | Stop processing, trigger email alert |

**Implementation:**
```
retryCount = 0
maxRetries = 3
waitTimes = [0, 5000, 10000, 15000]

WHILE retryCount <= maxRetries DO
  TRY
    response = callExternalAPI()
    IF response.success THEN
      RETURN response
    END IF
  CATCH connectionError
    retryCount++
    IF retryCount > maxRetries THEN
      triggerEmailAlert(connectionError)
      logError("External API failed after " + maxRetries + " retries")
      THROW connectionError
    END IF
    WAIT waitTimes[retryCount]
  END TRY
END WHILE
```

### 5.2 Email Alert on Failure

**Trigger:** After 3 failed retry attempts to external API

**Email Content:**
| Field | Value |
|-------|-------|
| Subject | [OCMS-ALERT] External API Connection Failed - {API_NAME} |
| To | System administrators, Support team |
| Body | API: {endpoint}<br>Error: {error_message}<br>Timestamp: {datetime}<br>Retry Count: 3<br>Action Required: Check external system connectivity |

---

## 6. API Request/Response Rules (Yi Jie Standard)

**IMPORTANT - All APIs follow these rules:**

### 6.1 HTTP Method
- **ALL APIs use POST method** (no GET, PUT, PATCH, DELETE)

### 6.2 Standard Response Format

**Success/Error Response (Yi Jie Format):**
```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Success"
  }
}
```

**List Response with Pagination:**
```json
{
  "total": 100,
  "limit": 10,
  "skip": 0,
  "data": [...]
}
```

**Batch Operation Response:**
```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Success",
    "totalProcessed": 2,
    "successCount": 2,
    "errorCount": 0,
    "results": [
      {
        "noticeNo": "500500303J",
        "appCode": "OCMS-2000",
        "message": "TS Success"
      }
    ]
  }
}
```

### 6.3 Empty Response
```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Success"
  }
}
```
- For list: return empty array `[]` in data field

### 6.4 Error Response
```json
{
  "data": {
    "appCode": "OCMS-4000",
    "message": "Invalid request. Please check and try again."
  }
}
```

### 6.5 Date Format
- Use ISO 8601: `2025-03-22T19:00:02`

---

## 7. Database Connection Users

| Zone | Connection User | Purpose |
|------|----------------|---------|
| Intranet | ocmsiz_app_conn | OCMS Staff Portal operations |
| Internet | ocmsez_app_conn | PLUS operations |

---

## 8. Summary

**Total APIs:** 5
- Internal (eService): 4 APIs
- External (PLUS): 1 API

**Key Features:**
- Batch TS application (up to 10 notices)
- Comprehensive validation
- Source authorization via suspension_source field
- 20 TS codes with different rules (updated from 18)
- Auto-calculation of due_date_of_revival using no_of_days_for_revival

**Yi Jie Standard Compliance:**
- ✅ All APIs use POST method
- ✅ Response format follows `{ data: { appCode, message } }`
- ✅ Token refresh handling documented (Section 4.1)
- ✅ Retry mechanism 3x + email alert (Section 5)
- ✅ Audit users: ocmsiz_app_conn / ocmsez_app_conn
- ✅ SQL Server sequences for numbering
- ✅ Parent table first, child table after

**Offence Notice Type:**
- ✅ TS applies to ALL offence notice types (O, E, U) - No restriction in code

**Next Steps:**
1. ✅ plan_condition.md created with 20 TS code rules
2. ✅ plan_flowchart.md created with flow structure
3. ✅ OCMS17_Flowchart.drawio created
4. ✅ QA Steps 1-3 completed and APPROVED
5. 🔄 Generate Technical Document

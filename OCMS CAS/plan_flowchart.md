# Flowchart Plan: OCMS - CAS - PLUS Integration
## CAS-OCMS Integration for Court Notices Management

---

## Document Information

| Field | Value |
|-------|-------|
| **Version** | 1.0 |
| **Date** | 2025-01-14 |
| **Source** | plan_api.md, plan_condition.md |
| **Epic** | OCMS CAS Integration |
| **Systems** | CAS, OCMS, PLUS, IZ-APIM |

---

## Overview

This document defines the flowchart structure for the CAS-OCMS-PLUS integration. The flowcharts will visualize:
1. **Section 2: High-Level Flows** - Overview of each API integration
2. **Section 3: Detailed Flows** - Complete flows with validations, conditions, and database operations

---

## Swimlanes Definition

| Swimlane | Color Code | Description |
|----------|------------|-------------|
| **CAS System** | Green (#d5e8d4) | External Court Administration System |
| **OCMS Backend** | Purple (#e1d5e7) | OCMS Intranet API and Cron Jobs |
| **OCMS Database** | Yellow (#fff2cc) | Database operations (ocms_valid_offence_notice, new tables) |
| **PLUS / IZ-APIM** | Blue (#dae8fc) | PLUS system and API Gateway |

---

## Section 2: High-Level Flows

### 2.1 API 1: Get Payment Status (High-Level)

**Process Overview:**

| Attribute | Value |
|-----------|-------|
| Process Name | CAS Get Payment Status |
| Section | 2.1 |
| Trigger | Daily scheduled job (CAS) |
| Frequency | Once a day (time TBD) |
| Systems Involved | CAS, OCMS Backend, OCMS Database |

**Flow Steps:**

| Step | Swimlane | Definition | Description |
|------|----------|------------|-------------|
| 1 | CAS | Start | Cron job triggered |
| 2 | CAS | Prepare unpaid notice list | Get unpaid court notices |
| 3 | CAS → OCMS | POST /v1/ocms/cas-getPaymentStatus | Send unpaid notice numbers |
| 4 | OCMS Backend | Validate request | Check auth, rate limit |
| 5 | OCMS Database | Query notice details | Get payment & stage info |
| 6 | OCMS Backend | Calculate EMCON flag | Check stage codes |
| 7 | OCMS Database | INSERT to ocms_cas_emcons | If court notice goes to reminder |
| 8 | OCMS → CAS | Return payment status | Send notice data |
| 9 | CAS | Process response | Update CAS records |
| 10 | CAS | End | Process complete |

**Flow Pattern:** Linear with single database insert side-effect

**Key Decision:** EMCON flag calculation (based on processing stage)

---

### 2.2 API 2: Update Court Notices (High-Level)

**Process Overview:**

| Attribute | Value |
|-----------|-------|
| Process Name | CAS Update Court Notices |
| Section | 2.2 |
| Trigger | After API 1 completion (CAS) |
| Frequency | Once a day (after API 1) |
| Systems Involved | CAS, OCMS Backend, OCMS Database |

**Flow Steps:**

| Step | Swimlane | Definition | Description |
|------|----------|------------|-------------|
| 1 | CAS | Start | After API 1 completes |
| 2 | CAS | Prepare court notice updates | Stage & suspension changes |
| 3 | CAS → OCMS | POST /v1/ocms/cas-updateCourtNotices | Send update request |
| 4 | OCMS Backend | Validate request | Check auth, notice exists |
| 5 | OCMS Database | Validate court notice | Check dayend flag |
| 6 | OCMS Database | UPDATE ocms_valid_offence_notice | Apply stage/suspension changes |
| 7 | OCMS Database | Query updated notices | Get payment info |
| 8 | OCMS → CAS | Return updated data | Confirm changes |
| 9 | CAS | Process response | Update CAS records |
| 10 | CAS | End | Process complete |

**Flow Pattern:** Linear with validation and update

**Key Decision:** Court notice validation (dayend flag check)

---

### 2.3 API 3: Refresh VIP Vehicle (High-Level)

**Process Overview:**

| Attribute | Value |
|-----------|-------|
| Process Name | OCMS Refresh VIP Vehicle |
| Section | 2.3 |
| Trigger | Daily scheduled job (OCMS) |
| Frequency | Once a day (time TBD) |
| Systems Involved | OCMS Backend, CAS, OCMS Database |

**Flow Steps:**

| Step | Swimlane | Definition | Description |
|------|----------|------------|-------------|
| 1 | OCMS Backend | Start | Cron job triggered |
| 2 | OCMS Backend | Prepare request | Generate requestId |
| 3 | OCMS → CAS | POST /v1/ocms/cas-refreshVIPVehicle | Request VIP list |
| 4 | CAS | Validate TLS certificate | Check mutual TLS auth |
| 5 | CAS → OCMS | Return VIP vehicle list | All vehicles |
| 6 | OCMS Backend | Process response | Parse vehicle data |
| 7 | OCMS Database | TRUNCATE ocms_vip_vehicle | Clear existing data |
| 8 | OCMS Database | INSERT all vehicles | Bulk insert from CAS |
| 9 | OCMS Backend | Log completion | Application logs |
| 10 | OCMS Backend | End | Process complete |

**Flow Pattern:** Linear with database refresh (truncate + insert)

**Key Decision:** Success response handling with retry logic

---

### 2.4 API 4: Query Notice Info (High-Level)

**Process Overview:**

| Attribute | Value |
|-----------|-------|
| Process Name | PLUS Query Notice Info |
| Section | 2.4 |
| Trigger | User query (PLUS) |
| Frequency | Adhoc (on-demand) |
| Systems Involved | PLUS, IZ-APIM, OCMS Backend, OCMS Database |

**Flow Steps:**

| Step | Swimlane | Definition | Description |
|------|----------|------------|-------------|
| 1 | PLUS | Start | User queries notice |
| 2 | PLUS → IZ-APIM | Forward request | Route to OCMS |
| 3 | IZ-APIM | Validate OAuth token | Check & refresh if needed |
| 4 | IZ-APIM → OCMS | POST /v1/plus-queryNoticeInfo | Send notice numbers |
| 5 | OCMS Backend | Validate request | Check auth, rate limit |
| 6 | OCMS Database | Query notice details | Get payment info |
| 7 | OCMS Backend | Calculate payment allowance | Check due date vs today |
| 8 | OCMS → IZ-APIM | Return notice info | With payment allowance |
| 9 | IZ-APIM → PLUS | Forward response | Route to PLUS |
| 10 | PLUS | Display to user | Show payment eligibility |
| 11 | PLUS | End | Process complete |

**Flow Pattern:** Linear with payment allowance calculation

**Key Decision:** Payment allowance flag (current_date > court_payment_due_date)

---

## Section 3: Detailed Flows

### 3.1 API 1: Get Payment Status (Detailed)

**Process Overview:**

| Attribute | Value |
|-----------|-------|
| Process Name | CAS Get Payment Status (Detailed) |
| Section | 3.1 |
| Trigger | Daily scheduled job (CAS) |
| Frequency | Once a day |
| Swimlanes | CAS, OCMS Backend, OCMS Database |

**Detailed Steps Table:**

| Step | Swimlane | Type | Definition | Description | Next Step |
|------|----------|------|------------|-------------|-----------|
| 1 | CAS | Start | Cron Start | Daily job triggered | 2 |
| 2 | CAS | Process | Query unpaid court notices | Get list of unpaid notice numbers | 3 |
| 3 | CAS | Process | Prepare request payload | Build JSON with notices array | 4 |
| 4 | CAS → OCMS | API Call | POST /v1/ocms/cas-getPaymentStatus | Send request with API key | 5 |
| 5 | OCMS Backend | Decision | Valid API key? | Check X-API-Key header | Yes→6, No→E1 |
| 6 | OCMS Backend | Decision | Rate limit OK? | Check request count | Yes→7, No→E2 |
| 7 | OCMS Backend | Decision | Payload valid? | Validate UUID, timestamp, array | Yes→8, No→E3 |
| 8 | OCMS Database | Query | SELECT notice details | Query ocms_valid_offence_notice | 9 |
| 9 | OCMS Backend | Decision | Notices exist? | Check query results | Yes→10, No→E4 |
| 10 | OCMS Backend | Process | Calculate EMCON flag | Check stage IN ('EM1','EM2','DN1','DN2') | 11 |
| 11 | OCMS Backend | Decision | Is EMCON notice? | Stage check result | Yes→12, No→13 |
| 12 | OCMS Database | Insert | INSERT into ocms_cas_emcons | Store notice_no | 13 |
| 13 | OCMS Backend | Process | Build response | Map all fields from DB | 14 |
| 14 | OCMS → CAS | API Response | Return 200 OK | Send notice data array | 15 |
| 15 | CAS | Process | Process response | Update CAS records | 16 |
| 16 | CAS | End | End | Process complete | - |
| E1 | OCMS Backend | Error | Return 401 Unauthorized | Invalid API key | End |
| E2 | OCMS Backend | Error | Return 429 Rate Limit | Too many requests | End |
| E3 | OCMS Backend | Error | Return 400 Bad Request | Validation failed | End |
| E4 | OCMS Backend | Error | Return 404 Not Found | Notice doesn't exist | End |

**Decision Logic Table:**

| Decision ID | Input | Condition | True Action | False Action |
|-------------|-------|-----------|-------------|--------------|
| D1 | X-API-Key header | API key exists and valid | Continue to D2 | Return 401 |
| D2 | Request count | count ≤ 10 per hour | Continue to D3 | Return 429 |
| D3 | Request payload | All fields valid format | Continue to D4 | Return 400 |
| D4 | Query result | Notice exists in DB | Continue to D5 | Return 404 |
| D5 | Processing stage | Stage IN ('EM1','EM2','DN1','DN2') | Insert to ocms_cas_emcons | Skip insert |

**Error Handling:**

| Error Point | Error Type | Handling | Recovery |
|-------------|------------|----------|----------|
| API Key Validation | Invalid/Missing | Return 401 immediately | No retry |
| Rate Limit | Exceeded | Return 429 | Wait and retry |
| Payload Validation | Invalid format | Return 400 with field error | Fix and retry |
| Notice Not Found | Missing notice | Return 404 | Check notice number |
| Database Error | Connection/Query | Return 500 | Log error |

**Data Operations:**

| Operation | Table | Fields | Source |
|-----------|-------|--------|--------|
| SELECT | ocms_valid_offence_notice | notice_no, payment_status, amount_payable, amount_paid, payment_acceptance_allowed, prev_processing_stage, next_processing_stage, prev_processing_date, next_processing_date, suspension_type, crs_reason_of_suspension, epr_reason_of_suspension | Database query |
| INSERT | ocms_cas_emcons | notice_no, cre_date, cre_user_id | Calculated (EMCON flag) |

---

### 3.2 API 2: Update Court Notices (Detailed)

**Process Overview:**

| Attribute | Value |
|-----------|-------|
| Process Name | CAS Update Court Notices (Detailed) |
| Section | 3.2 |
| Trigger | After API 1 completion |
| Frequency | Once a day |
| Swimlanes | CAS, OCMS Backend, OCMS Database |

**Detailed Steps Table:**

| Step | Swimlane | Type | Definition | Description | Next Step |
|------|----------|------|------------|-------------|-----------|
| 1 | CAS | Start | Start | After API 1 completes | 2 |
| 2 | CAS | Process | Prepare update payload | Build JSON with stage/suspension | 3 |
| 3 | CAS → OCMS | API Call | POST /v1/ocms/cas-updateCourtNotices | Send update request | 4 |
| 4 | OCMS Backend | Decision | Valid API key? | Check X-API-Key header | Yes→5, No→E1 |
| 5 | OCMS Backend | Decision | Payload valid? | Validate all fields | Yes→6, No→E2 |
| 6 | OCMS Database | Query | SELECT notice details | Query ocms_valid_offence_notice | 7 |
| 7 | OCMS Database | Decision | Notice exists? | Check query results | Yes→8, No→E3 |
| 8 | OCMS Database | Decision | Is court notice? | Check dayend = 'Y' | Yes→9, No→E4 |
| 9 | OCMS Database | Update | UPDATE ocms_valid_offence_notice | Set suspension_type, epr_reason, processing_stage | 10 |
| 10 | OCMS Database | Query | SELECT updated notices | Get payment info | 11 |
| 11 | OCMS Backend | Process | Build response | Map updated data | 12 |
| 12 | OCMS → CAS | API Response | Return 200 OK | Confirm changes | 13 |
| 13 | CAS | Process | Process response | Update CAS records | 14 |
| 14 | CAS | End | End | Process complete | - |
| E1 | OCMS Backend | Error | Return 401 Unauthorized | Invalid API key | End |
| E2 | OCMS Backend | Error | Return 400 Bad Request | Validation failed | End |
| E3 | OCMS Backend | Error | Return 404 Not Found | Notice doesn't exist | End |
| E4 | OCMS Backend | Error | Return 400 Invalid | Not a court notice | End |

**Decision Logic Table:**

| Decision ID | Input | Condition | True Action | False Action |
|-------------|-------|-----------|-------------|--------------|
| D1 | X-API-Key header | API key exists and valid | Continue to D2 | Return 401 |
| D2 | Request payload | All fields valid | Continue to D3 | Return 400 |
| D3 | Query result | Notice exists | Continue to D4 | Return 404 |
| D4 | dayend flag | dayend = 'Y' | Process update | Return 400 |

**Error Handling:**

| Error Point | Error Type | Handling | Recovery |
|-------------|------------|----------|----------|
| API Key Validation | Invalid/Missing | Return 401 immediately | No retry |
| Payload Validation | Invalid format | Return 400 with field error | Fix and retry |
| Notice Not Found | Missing notice | Return 404 | Check notice number |
| Not Court Notice | Invalid notice type | Return 400 | Verify notice type |
| Database Error | Connection/Query | Return 500 | Log error |

**Data Operations:**

| Operation | Table | Fields | Source |
|-----------|-------|--------|--------|
| SELECT | ocms_valid_offence_notice | notice_no, dayend | Database query (validation) |
| UPDATE | ocms_valid_offence_notice | suspension_type, epr_reason_of_suspension, processing_stage, upd_date, upd_user_id | API request |
| SELECT | ocms_valid_offence_notice | notice_no, amount_payable, payment_acceptance_allowed, payment_due_date | Database query (response) |

---

### 3.3 API 3: Refresh VIP Vehicle (Detailed)

**Process Overview:**

| Attribute | Value |
|-----------|-------|
| Process Name | OCMS Refresh VIP Vehicle (Detailed) |
| Section | 3.3 |
| Trigger | Daily scheduled job (OCMS) |
| Frequency | Once a day |
| Swimlanes | OCMS Backend, CAS, OCMS Database |

**Detailed Steps Table:**

| Step | Swimlane | Type | Definition | Description | Next Step |
|------|----------|------|------------|-------------|-----------|
| 1 | OCMS Backend | Start | Cron Start | Daily job triggered | 2 |
| 2 | OCMS Backend | Process | Record start time | Log job start immediately | 3 |
| 3 | OCMS Backend | Process | Prepare request | Generate requestId, timestamp | 4 |
| 4 | OCMS → CAS | API Call | POST /v1/ocms/cas-refreshVIPVehicle | Send with TLS certificate | 5 |
| 5 | CAS | Decision | Valid TLS certificate? | Check mutual TLS | Yes→6, No→E1 |
| 6 | CAS | Process | Query VIP vehicles | Get full list from CAS DB | 7 |
| 7 | CAS → OCMS | API Response | Return 200 OK | Send vehicle array | 8 |
| 8 | OCMS Backend | Decision | Success response? | Check HTTP status | Yes→9, No→E2 |
| 9 | OCMS Backend | Decision | Retry needed? | 500/503 error and retry < 3 | Yes→4, No→10 |
| 10 | OCMS Database | Process | TRUNCATE ocms_vip_vehicle | Clear all existing data | 11 |
| 11 | OCMS Database | Insert | INSERT all vehicles | Bulk insert from CAS response | 12 |
| 12 | OCMS Backend | Decision | Insert success? | Check row count | Yes→13, No→E3 |
| 13 | OCMS Backend | Process | Record completion | Log to application logs | 14 |
| 14 | OCMS Backend | End | End | Process complete | - |
| E1 | CAS | Error | Return 401 Unauthorized | Invalid TLS certificate | End |
| E2 | OCMS Backend | Error | Retry or fail | After 3 retries → Email alert | End |
| E3 | OCMS Backend | Error | Return 500 Internal Error | Database insert failed | End |

**Decision Logic Table:**

| Decision ID | Input | Condition | True Action | False Action |
|-------------|-------|-----------|-------------|--------------|
| D1 | TLS certificate | Certificate valid | Return vehicle list | Return 401 |
| D2 | HTTP status | status = 200 | Process data | Check retry count |
| D3 | Retry count | count < 3 | Retry request (exponential backoff) | Send email alert |
| D4 | Insert result | Rows inserted > 0 | Log success | Return 500 |

**Error Handling:**

| Error Point | Error Type | Handling | Recovery |
|-------------|------------|----------|----------|
| TLS Certificate | Invalid/Missing | Return 401 | No retry |
| Connection Failed | Network error | Retry 3x with backoff | Email alert after 3 failures |
| CAS Server Error | 500/503 | Retry 3x with backoff | Email alert after 3 failures |
| Truncate Failed | Database error | Return 500 | Log error, alert DBA |
| Insert Failed | Database error | Return 500 | Log error, alert DBA |

**Retry Mechanism:**

```
Retry Logic:
- Max retries: 3
- Backoff: Exponential (2^retry_count seconds)
- Retry conditions: HTTP 500, 503, connection error
- After 3 failures: Send email alert to operations team
```

**Data Operations:**

| Operation | Table | Fields | Source |
|-----------|-------|--------|--------|
| SELECT | (CAS Database) | vehicle_no, vip_vehicle, description, status | CAS API response |
| TRUNCATE | ocms_vip_vehicle | All rows | Clear existing data |
| INSERT | ocms_vip_vehicle | vehicle_no, vip_vehicle, description, status, cre_date, cre_user_id | CAS API response |

---

### 3.4 API 4: Query Notice Info (Detailed)

**Process Overview:**

| Attribute | Value |
|-----------|-------|
| Process Name | PLUS Query Notice Info (Detailed) |
| Section | 3.4 |
| Trigger | User query (PLUS) |
| Frequency | Adhoc (on-demand) |
| Swimlanes | PLUS, IZ-APIM, OCMS Backend, OCMS Database |

**Detailed Steps Table:**

| Step | Swimlane | Type | Definition | Description | Next Step |
|------|----------|------|------------|-------------|-----------|
| 1 | PLUS | Start | Start | User queries notice payment info | 2 |
| 2 | PLUS | Process | Build request | Create payload with notice numbers | 3 |
| 3 | PLUS → IZ-APIM | API Call | Forward to OCMS | Route through gateway | 4 |
| 4 | IZ-APIM | Decision | OAuth token valid? | Check Authorization header | Yes→5, No→E1 |
| 5 | IZ-APIM | Decision | Token expired? | Check token expiry | Yes→6, No→7 |
| 6 | IZ-APIM | Process | Refresh OAuth token | Get new access token | 7 |
| 7 | IZ-APIM → OCMS | API Call | POST /v1/plus-queryNoticeInfo | Forward with Bearer token | 8 |
| 8 | OCMS Backend | Decision | Valid OAuth token? | Check Authorization header | Yes→9, No→E2 |
| 9 | OCMS Backend | Decision | Rate limit OK? | Check request count | Yes→10, No→E3 |
| 10 | OCMS Backend | Decision | Payload valid? | Validate UUID, timestamp, array | Yes→11, No→E4 |
| 11 | OCMS Database | Query | SELECT notice details | Query ocms_valid_offence_notice | 12 |
| 12 | OCMS Backend | Decision | Notices exist? | Check query results | Yes→13, No→E5 |
| 13 | OCMS Backend | Process | Calculate payment allowance | IF current_date > payment_due_date THEN 'N' ELSE flag | 14 |
| 14 | OCMS Database | Database | MERGE ocms_court_case | Insert/Update court_payment_due_date for persistence | 15 |
| 15 | OCMS Backend | Process | Build response | Map notice_no, court_payment_due_date, payment_allowance_flag, amount_payable | 16 |
| 16 | OCMS → IZ-APIM | API Response | Return 200 OK | Send notice data | 17 |
| 17 | IZ-APIM → PLUS | Forward | Route response | Forward to PLUS | 18 |
| 18 | PLUS | Process | Display to user | Show payment eligibility | 19 |
| 19 | PLUS | End | End | Process complete | - |
| E1 | IZ-APIM | Error | Return 401 Unauthorized | Invalid token, attempt refresh | End |
| E2 | OCMS Backend | Error | Return 401 Unauthorized | Invalid token after refresh | End |
| E3 | OCMS Backend | Error | Return 429 Rate Limit | Too many requests | End |
| E4 | OCMS Backend | Error | Return 400 Bad Request | Validation failed | End |
| E5 | OCMS Backend | Error | Return 404 Not Found | Notice doesn't exist | End |

**Decision Logic Table:**

| Decision ID | Input | Condition | True Action | False Action |
|-------------|-------|-----------|-------------|--------------|
| D1 | OAuth token | Token valid | Continue to D2 | Return 401 |
| D2 | Token expiry | Token expired | Refresh token | Continue to D3 |
| D3 | Authorization header | Valid Bearer token | Continue to D4 | Return 401 |
| D4 | Request count | count ≤ 100 per minute | Continue to D5 | Return 429 |
| D5 | Request payload | All fields valid | Continue to D6 | Return 400 |
| D6 | Query result | Notice exists | Continue to D7 | Return 404 |
| D7 | Payment allowance | current_date > payment_due_date | Set flag = 'N' | Use DB flag |

**Error Handling:**

| Error Point | Error Type | Handling | Recovery |
|-------------|------------|----------|----------|
| OAuth Token (IZ-APIM) | Invalid/Expired | Auto refresh & retry | Continue after refresh |
| OAuth Token (OCMS) | Invalid after refresh | Return 401 | No retry |
| Rate Limit | Exceeded | Return 429 | Wait and retry |
| Payload Validation | Invalid format | Return 400 with field error | Fix and retry |
| Notice Not Found | Missing notice | Return 404 | Check notice number |
| Connection Error | Network failure | Retry 3x with backoff | Email alert after failures |

**Payment Allowance Calculation Logic:**

```
FOR each notice:
  GET current_date = GETDATE()
  GET payment_due_date FROM ocms_valid_offence_notice

  IF current_date > payment_due_date THEN
     payment_allowance_flag = 'N'  -- Past due date
  ELSE
     payment_allowance_flag = payment_acceptance_allowed  -- Use DB value
  END IF
END FOR
```

**Token Refresh Logic:**

```
IF token_expired THEN
  TRY
    new_token = refresh_oauth_token(endpoint)
    IF new_token IS NOT NULL THEN
      Update Authorization header
      Retry original request
    ELSE
      Return 401
    END IF
  CATCH (error)
    Return 401
  END TRY
END IF
```

**Data Operations:**

| Operation | Table | Fields | Source |
|-----------|-------|--------|--------|
| SELECT | ocms_valid_offence_notice | notice_no, payment_due_date, payment_acceptance_allowed, amount_payable | Database query |
| Calculate | (N/A) | payment_allowance_flag | Calculated: GETDATE() > payment_due_date |
| MERGE | ocms_court_case | notice_no, court_payment_due_date, cre_date, cre_user_id, upd_date, upd_user_id | Payment due date persistence |

**Court Case Persist Logic:**

```sql
-- Insert or update court payment due date for persistence
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

---

## Flowchart Sizing Estimates

### 3.1 API 1: Get Payment Status
- **Complexity**: Medium (15-18 steps)
- **Estimated Elements**: 18
- **Estimated Page Width**: 2400-2800px
- **Estimated Page Height**: 900-1100px (3 swimlanes)
- **Swimlane Height**: 300-350px each

### 3.2 API 2: Update Court Notices
- **Complexity**: Medium (14-16 steps)
- **Estimated Elements**: 16
- **Estimated Page Width**: 2200-2600px
- **Estimated Page Height**: 900-1100px (3 swimlanes)
- **Swimlane Height**: 300-350px each

### 3.3 API 3: Refresh VIP Vehicle
- **Complexity**: Medium (14-16 steps with retry loop)
- **Estimated Elements**: 16
- **Estimated Page Width**: 2200-2600px
- **Estimated Page Height**: 700-900px (3 swimlanes)
- **Swimlane Height**: 250-300px each

### 3.4 API 4: Query Notice Info
- **Complexity**: High (18-20 steps with token refresh)
- **Estimated Elements**: 20
- **Estimated Page Width**: 2600-3200px
- **Estimated Page Height**: 1100-1400px (4 swimlanes)
- **Swimlane Height**: 275-350px each

---

## Flowchart Checklist

Before creating drawio files:

- [x] All steps have clear names
- [x] All decision points have Yes/No paths defined
- [x] All paths lead to an End point
- [x] Error handling paths are included
- [x] Database operations are marked with specific operations (SELECT, INSERT, UPDATE, TRUNCATE)
- [x] Swimlanes are defined for each system/tier
- [x] Color coding is consistent (CAS: Green, OCMS Backend: Purple, OCMS Database: Yellow, PLUS/IZ-APIM: Blue)
- [x] Step descriptions are complete
- [x] Data source attribution included in plan_condition.md
- [x] Error codes defined in plan_api.md
- [x] Business rules documented with Rule IDs

---

## Next Steps

1. ✅ Complete plan_flowchart.md
2. ⏳ Create flowchart .drawio file for:
   - Section 2: High-Level Flows (2.1, 2.2, 2.3, 2.4)
   - Section 3: Detailed Flows (3.1, 3.2, 3.3, 3.4)
3. ⏳ Generate technical documentation
4. ⏳ Review against Yi Jie checklist

---

**Document End**

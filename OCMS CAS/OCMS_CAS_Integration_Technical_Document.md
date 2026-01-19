# OCMS CAS – CAS-OCMS-PLUS Integration

**Prepared by**

MGG Technology

---

## Version History

| Version | Updated By | Date | Changes |
| --- | --- | --- | --- |
| v1.0 | Technical Writer | 14/01/2025 | Document Initiation |
| v1.1 | Claude | 15/01/2026 | Updated based on latest flowchart |

---

## Table of Content

| Section | Content | Pages |
| --- | --- | --- |
| Overview | High Level Flow | 3 |
| 1 | API 1: Get Payment Status | 5 |
| 1.1 | Use Case | 5 |
| 1.2 | Flow Diagram | 5 |
| 1.3 | API Specification | 6 |
| 1.4 | Data Mapping | 7 |
| 1.5 | Error Handling | 7 |
| 2 | API 2: Update Court Notices | 9 |
| 2.1 | Use Case | 9 |
| 2.2 | Flow Diagram | 9 |
| 2.3 | API Specification | 10 |
| 2.4 | Data Mapping | 11 |
| 2.5 | Error Handling | 12 |
| 3 | API 3: Refresh VIP Vehicle | 14 |
| 3.1 | Use Case | 14 |
| 3.2 | Flow Diagram | 14 |
| 3.3 | API Specification | 15 |
| 3.4 | Data Mapping | 16 |
| 3.5 | Error Handling | 17 |
| 4 | API 4: Query Notice Info | 19 |
| 4.1 | Use Case | 19 |
| 4.2 | Flow Diagram | 19 |
| 4.3 | API Specification | 20 |
| 4.4 | Data Mapping | 21 |
| 4.5 | Error Handling | 22 |

---

# Overview – High Level Flow

## Integration Overview

This document describes the integration between CAS (Court Administration System), OCMS (Online Collection Management System), and PLUS systems for court notice payment processing.

![High Level Flow](./images/2_High_Level_All_APIs.png)

NOTE: Due to page size limit, the full-sized image is appended.

## API Summary

| API | Direction | Description |
| --- | --- | --- |
| API 1: Get Payment Status | CAS → OCMS | CAS queries payment status of unpaid notices from OCMS |
| API 2: Update Court Notices | CAS → OCMS | CAS sends court notice updates to OCMS |
| API 3: Refresh VIP Vehicle | OCMS → CAS | OCMS requests VIP vehicle list refresh from CAS |
| API 4: Query Notice Info | PLUS → OCMS | PLUS queries notice information from OCMS via IZ |

---

# Section 1 – API 1: Get Payment Status

## Use Case

1. CAS System performs daily cron job to query payment status of unpaid notices from OCMS.

2. OCMS receives the request, validates API key, and queries notice data from database.

3. OCMS returns payment status information including suspension type, payment status, amount payable, and payment acceptance allowed flag.

4. This is a query-only operation - no database write occurs. EMCON is handled manually.

## Flow Diagram

![API 1 Flow Diagram](./images/3.1_API1_Detailed.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | CAS cron job initiates the process | Process initiation |
| CAS prepares unpaid notice list | CAS System prepares list of notice numbers to query | Prepare request data |
| Call OCMS API | CAS calls OCMS API with notice numbers | API call |
| Check API Key | OCMS validates the API key from request header | Authentication |
| Validate Request Schema | OCMS validates request payload format | Schema validation |
| Check Batch Limit | OCMS verifies notice count is within limit (max 500) | Batch validation |
| Query Notice Data from DB | OCMS queries notice data from ocms_valid_offence_notice table | Database query |
| Build Response | OCMS maps database fields to response format | Response building |
| Return 200 OK | OCMS returns successful response to CAS | API response |
| Receive Response | CAS receives and processes the response | Response handling |
| End | Process completes | Process termination |

## API Specification

### API Provide

#### API cas-getPaymentStatus

| Field | Value |
| --- | --- |
| API Name | cas-getPaymentStatus |
| URL | POST /v1/ocms/cas-getPaymentStatus |
| Description | Query payment status of notices for CAS System |
| Method | POST |
| Header | `{ "Content-Type": "application/json", "Ocp-Apim-Subscription-Key": "[API Key]" }` |
| Payload | `{ "noticeNumbers": ["500400058A", "500400059B"] }` |
| Response | `{ "notices": [{ "noticeNo": "500400058A", "suspensionType": "TS", "crsReasonOfSuspension": "APP", "eprReasonOfSuspension": null, "paymentStatus": null, "amountPayable": 300.00, "amountPaid": 0.00, "paymentAcceptanceAllowed": "Y" }] }` |
| Response (Empty) | `{ "notices": [] }` |

## Data Mapping

### Database Data Mapping

| Zone | Database Table | Field Name |
| --- | --- | --- |
| OCMS | ocms_valid_offence_notice | notice_no |
| OCMS | ocms_valid_offence_notice | suspension_type |
| OCMS | ocms_valid_offence_notice | crs_reason_of_suspension |
| OCMS | ocms_valid_offence_notice | epr_reason_of_suspension |
| OCMS | ocms_valid_offence_notice | payment_status |
| OCMS | ocms_valid_offence_notice | amount_payable |
| OCMS | ocms_valid_offence_notice | amount_paid |
| OCMS | ocms_valid_offence_notice | payment_acceptance_allowed |

### SQL Query

```sql
SELECT notice_no, suspension_type, crs_reason_of_suspension,
       epr_reason_of_suspension, payment_status, amount_payable,
       amount_paid, payment_acceptance_allowed
FROM ocms_valid_offence_notice
WHERE notice_no IN (:noticeNumbers)
```

## Error Handling

### API Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| Invalid API Key | OCMS-4001 | Unauthorized | API key validation failed |
| Invalid Request | OCMS-4000 | Bad Request | Request schema validation failed |
| Payload Too Large | OCMS-4013 | Payload Too Large | Exceeds max 500 notices per request |
| Notice Not Found | - | Return empty | Notice does not exist in database |

---

# Section 2 – API 2: Update Court Notices

## Use Case

1. CAS System performs daily update of court notice information to OCMS.

2. Only court notices (where dayend = 'Y') are processed.

3. OCMS validates the notice exists and is a court notice (last_processing_stage not in NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3).

4. OCMS updates multiple fields in ocms_valid_offence_notice table and logs the operation.

## Flow Diagram

![API 2 Flow Diagram](./images/3.2_API2_Detailed.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | CAS initiates court notice update | Process initiation |
| CAS prepares court notice data | CAS prepares notice data with processing stages and dates | Prepare request data |
| Call OCMS API | CAS calls OCMS update API | API call |
| Check API Key | OCMS validates API key | Authentication |
| Validate Request Schema | OCMS validates request payload | Schema validation |
| Check Batch Limit | OCMS verifies notice count (max 500) | Batch validation |
| Query Notice from DB | OCMS queries existing notice data | Database query |
| Notice exists? | Check if notice exists in database | Existence check |
| last_processing_stage check | Verify notice is court notice (not NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3) | Court notice validation |
| Update Notice in DB | Update notice fields in database | Database update |
| Log to App Log | Log operation to application log | Logging |
| Build Response | Build response with update status | Response building |
| Return 200 OK | Return success response | API response |

## API Specification

### API Provide

#### API cas-updateCourtNotices

| Field | Value |
| --- | --- |
| API Name | cas-updateCourtNotices |
| URL | POST /v1/ocms/cas-updateCourtNotices |
| Description | Update court notice information from CAS System |
| Method | POST |
| Header | `{ "Content-Type": "application/json", "Ocp-Apim-Subscription-Key": "[API Key]" }` |
| Payload | `{ "notices": [{ "noticeNo": "500400058A", "suspensionType": "TS", "lastProcessingStage": "CSD", "prevProcessingStage": "CPC", "nextProcessingStage": "CSR", "lastProcessingDate": "2024-01-15", "prevProcessingDate": "2024-01-10", "nextProcessingDate": "2024-01-20", "atomsFlag": "Y", "amountPayable": 300.00, "paymentAcceptanceAllowed": "Y", "eprReasonOfSuspension": "APP", "eprDateOfSuspension": "2024-01-15" }] }` |
| Response | `{ "updatedCount": 2, "failedCount": 0, "notices": [{ "noticeNo": "500400058A", "appCode": "OCMS-2000", "status": "OK" }] }` |

## Data Mapping

### Database Data Mapping

| Zone | Database Table | Field Name |
| --- | --- | --- |
| OCMS | ocms_valid_offence_notice | notice_no |
| OCMS | ocms_valid_offence_notice | last_processing_stage |
| OCMS | ocms_valid_offence_notice | prev_processing_stage |
| OCMS | ocms_valid_offence_notice | next_processing_stage |
| OCMS | ocms_valid_offence_notice | last_processing_date |
| OCMS | ocms_valid_offence_notice | prev_processing_date |
| OCMS | ocms_valid_offence_notice | next_processing_date |
| OCMS | ocms_valid_offence_notice | atoms_flag |
| OCMS | ocms_valid_offence_notice | amount_payable |
| OCMS | ocms_valid_offence_notice | payment_acceptance_allowed |
| OCMS | ocms_valid_offence_notice | epr_reason_of_suspension |
| OCMS | ocms_valid_offence_notice | epr_date_of_suspension |
| OCMS | ocms_valid_offence_notice | suspension_type |

### SQL Queries

**SELECT Query:**
```sql
SELECT notice_no, last_processing_stage
FROM ocms_valid_offence_notice
WHERE notice_no = :noticeNo
```

**UPDATE Query:**
```sql
UPDATE ocms_valid_offence_notice
SET last_processing_stage = :lastProcessingStage,
    prev_processing_stage = :prevProcessingStage,
    next_processing_stage = :nextProcessingStage,
    last_processing_date = :lastProcessingDate,
    prev_processing_date = :prevProcessingDate,
    next_processing_date = :nextProcessingDate,
    atoms_flag = :atomsFlag,
    amount_payable = :amountPayable,
    payment_acceptance_allowed = :paymentAllowance,
    epr_reason_of_suspension = :eprReason,
    epr_date_of_suspension = :eprDate,
    suspension_type = :suspensionType
WHERE notice_no = :noticeNo
```

## Error Handling

### API Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| Invalid API Key | OCMS-4001 | Unauthorized | API key validation failed |
| Invalid Request | OCMS-4000 | Bad Request | Request schema validation failed |
| Payload Too Large | OCMS-4013 | Payload Too Large | Exceeds max 500 notices per request |
| Notice Not Found | - | Return empty | Notice does not exist in database |
| Not Court Notice | OCMS-4009 | Not Court Notice | Notice is not a court notice |

---

# Section 3 – API 3: Refresh VIP Vehicle

## Use Case

1. OCMS scheduled job runs daily to refresh VIP vehicle data from CAS System.

2. OCMS calls CAS API via APIM to retrieve current VIP vehicle list.

3. On successful response, OCMS truncates existing data and inserts new VIP vehicle records.

4. Retry mechanism: Up to 3 retries on failure before sending email alert to operations.

## Flow Diagram

![API 3 Flow Diagram](./images/3.3_API3_Detailed.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | OCMS cron job initiates | Process initiation |
| Record Start Time | Record job start timestamp | Timestamp recording |
| Prepare Request | Prepare API request | Request preparation |
| Call CAS API using APIM | Call CAS API through APIM gateway | API call |
| CAS validates APIM | CAS validates APIM credentials | CAS authentication |
| CAS queries VIP vehicles | CAS queries VIP vehicle data | CAS database query |
| CAS returns vehicle list | CAS returns VIP vehicle list | CAS response |
| Receive VIP Data | OCMS receives response | Response handling |
| Success? | Check if API call was successful | Success check |
| Retry < 3? | Check retry count | Retry check |
| Insert VIP Vehicle records | Insert records into ocms_vip_vehicle table | Database insert |
| Insert Success? | Check if insert was successful | Insert check |
| Log Completion | Log successful completion | Logging |
| End | Process completes | Process termination |

## API Specification

### API Consume

#### API refreshVIPVehicle

| Field | Value |
| --- | --- |
| API Name | refreshVIPVehicle |
| URL | POST /v1/cas/refreshVIPVehicle (TBD) |
| Description | Retrieve VIP vehicle list from CAS System |
| Method | POST |
| Header | `{ "Content-Type": "application/json", "Ocp-Apim-Subscription-Key": "[APIM Key]" }` |
| Response | `{ "data": [{ "vehicleNo": "ABC1234", "description": "VIP Vehicle", "status": "ACTIVE" }], "totalCount": 150 }` |

## Data Mapping

### Database Data Mapping

| Zone | Database Table | Field Name |
| --- | --- | --- |
| OCMS | ocms_vip_vehicle | vehicle_no |
| OCMS | ocms_vip_vehicle | vip_vehicle |
| OCMS | ocms_vip_vehicle | description |
| OCMS | ocms_vip_vehicle | status |
| OCMS | ocms_vip_vehicle | cre_date |
| OCMS | ocms_vip_vehicle | cre_user_id |

### SQL Query

**INSERT Query:**
```sql
INSERT INTO ocms_vip_vehicle
(vehicle_no, vip_vehicle, description, status, cre_date, cre_user_id)
VALUES (:vehicleNo, :vipVehicle, :description, :status, GETDATE(), 'ocmsiz_app_conn')
```

## Error Handling

### Application Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| API Call Failure | CAS API returns error after 3 retries | Send email alert to operations |
| DB Insert Failure | Database insert operation fails | Log error to application log |

---

# Section 4 – API 4: Query Notice Info

## Use Case

1. PLUS System performs adhoc/real-time queries for notice information via IZ-APIM gateway.

2. OCMS validates IZ-APIM key, request schema, and batch limit (max 10 notices).

3. OCMS checks crs_reason_of_suspension:<br>a. If null: Update VON (payment_acceptance_allowed) and Court Case (court_payment_due_date), return success<br>b. If not null: Return OCMS-4003 (notice already paid)

4. Response includes updated count and status for each notice.

## Flow Diagram

![API 4 Flow Diagram](./images/3.4_API4_Detailed.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | PLUS initiates query | Process initiation |
| PLUS prepares notice list | PLUS prepares notice numbers and payment due date | Prepare request data |
| Call IZ-APIM Gateway | PLUS calls API through IZ-APIM | Gateway call |
| IZ-APIM validate | IZ-APIM validates request | Gateway validation |
| Route to OCMS | IZ-APIM routes request to OCMS | Request routing |
| Validate IZ-APIM key | OCMS validates IZ-APIM key | Authentication |
| Validate Request Schema | OCMS validates request payload | Schema validation |
| Check Batch Limit | OCMS verifies notice count (max 10) | Batch validation |
| check crs_reason_of_suspension | Query notice to check crs_reason_of_suspension | Payment status check |
| crs_reason_of_suspension = null? | Decision: Is crs_reason_of_suspension null? | Payment decision |
| update VON | Update payment_acceptance_allowed in ocms_valid_offence_notice | VON update |
| update Court Case | Update court_payment_due_date in ocms_court_case | Court case update |
| return OCMS-4003 | Return notice already paid error | Already paid response |
| Build Response | Build response with update results | Response building |
| Return 200 OK | Return success response | API response |

## API Specification

### API Provide

#### API plus-queryNoticeInfo

| Field | Value |
| --- | --- |
| API Name | plus-queryNoticeInfo |
| URL | POST /v1/plus-queryNoticeInfo |
| Description | Query and update notice info for PLUS System via IZ-APIM |
| Method | POST |
| Header | `{ "Content-Type": "application/json", "Ocp-Apim-Subscription-Key": "[IZ-APIM Key]" }` |
| Payload | `{ "noticeNumbers": ["500400058A", "500400059B"], "paymentDueDate": "2024-01-15" }` |
| Response | `{ "updatedCount": 1, "failedCount": 0, "notices": [{ "noticeNo": "500400058A", "appCode": "OCMS-2000", "status": "OK" }] }` |

## Data Mapping

### Database Data Mapping

| Zone | Database Table | Field Name |
| --- | --- | --- |
| OCMS | ocms_valid_offence_notice | notice_no |
| OCMS | ocms_valid_offence_notice | crs_reason_of_suspension |
| OCMS | ocms_valid_offence_notice | payment_acceptance_allowed |
| OCMS | ocms_court_case | notice_no |
| OCMS | ocms_court_case | court_payment_due_date |

### SQL Queries

**SELECT Query:**
```sql
SELECT notice_no, crs_reason_of_suspension
FROM ocms_valid_offence_notice
WHERE notice_no = :noticeNo
```

**UPDATE VON Query:**
```sql
UPDATE ocms_valid_offence_notice
SET payment_acceptance_allowed = :paymentAllowance
WHERE notice_no = :noticeNo
```

**UPDATE Court Case Query:**
```sql
UPDATE ocms_court_case
SET court_payment_due_date = :paymentDueDate
WHERE notice_no = :noticeNo
```

## Error Handling

### API Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| Invalid IZ-APIM Key | OCMS-4001 | Unauthorized | IZ-APIM key validation failed |
| Invalid Request | OCMS-4000 | Bad Request | Request schema validation failed |
| Payload Too Large | OCMS-4013 | Payload Too Large | Exceeds max 10 notices per request |
| Notice Already Paid | OCMS-4003 | Notice Already Paid | crs_reason_of_suspension is not null |

---

# Summary

## API Overview

| API | Direction | Frequency | Batch Limit | Description |
| --- | --- | --- | --- | --- |
| API 1 | CAS → OCMS | Daily | 500 | Get Payment Status |
| API 2 | CAS → OCMS | Daily | 500 | Update Court Notices |
| API 3 | OCMS → CAS | Daily | N/A | Refresh VIP Vehicle |
| API 4 | PLUS → OCMS | Adhoc | 10 | Query Notice Info |

## Database Tables

| Table | Description |
| --- | --- |
| ocms_valid_offence_notice | Main notice table with payment and processing information |
| ocms_vip_vehicle | VIP vehicle reference data from CAS |
| ocms_court_case | Court case information including payment due dates |

## Error Codes

| Code | Description |
| --- | --- |
| OCMS-2000 | Success |
| OCMS-4000 | Bad Request |
| OCMS-4001 | Unauthorized |
| OCMS-4003 | Notice Already Paid |
| OCMS-4009 | Not Court Notice |
| OCMS-4013 | Payload Too Large |

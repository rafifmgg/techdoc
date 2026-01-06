# OCMS 18 - Permanent Suspension API Plan

## Document Information

| Item | Details |
|------|---------|
| Version | 1.0 |
| Date | 2026-01-05 |
| Source Document | v1.4_OCMS 18_Functional_Document.md |
| Source Code | ocmsadminapi, ocmsadmincron |
| Author | Technical Documentation |

---

## 1. API Overview

### 1.1 Base URL
```
${api.version} = v1
Base URL: /v1
```

### 1.2 Authentication
All APIs require JWT token authentication via Spring Security.

### 1.3 API Summary

| # | Endpoint | Method | Source | Description |
|---|----------|--------|--------|-------------|
| 1 | `/v1/suspension-codes` | POST | OCMS Staff Portal | Get suspension codes for UI dropdowns |
| 2 | `/v1/staff-apply-suspension` | POST | OCMS Staff Portal | Manual PS/TS by OCMS OIC |
| 3 | `/v1/plus-apply-suspension` | POST | PLUS Staff Portal | Manual PS/TS by PLU OIC |
| 4 | `/v1/apply-suspension` | POST | OCMS Backend | Auto PS/TS by system |
| 5 | `/v1/staff-revive-suspension` | POST | OCMS Staff Portal | Manual revival by OCMS OIC |
| 6 | `/v1/plus-revive-suspension` | POST | PLUS Staff Portal | Manual revival by PLU OIC |
| 7 | `/v1/ps-report/by-system` | POST | OCMS Staff Portal | PS Report by System |
| 8 | `/v1/ps-report/by-officer` | POST | OCMS Staff Portal | PS Report by Officer |

---

## 2. Internal APIs (OCMS)

### 2.1 Get Suspension Codes

Get list of suspension codes for UI dropdown.

**Endpoint:** `POST /v1/suspension-codes`

**Request Headers:**
```
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
```

**Request Body:**
```json
{
  "suspensionType": "PS",
  "source": "OCMS"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| suspensionType | String | No | Filter by type: "TS" or "PS". If not provided, returns all. |
| source | String | No | Filter by source: "OCMS" or "PLUS". If not provided, returns all. |

**Response - Success (200):**
```json
{
  "appCode": "OCMS-2000",
  "message": "Success",
  "suspensionCodes": [
    {
      "suspensionType": "PS",
      "reasonOfSuspension": "APP",
      "description": "Appeal Accepted",
      "noOfDaysForRevival": null,
      "status": "A"
    },
    {
      "suspensionType": "PS",
      "reasonOfSuspension": "CFA",
      "description": "Cancelled By Field Admin Unit",
      "noOfDaysForRevival": null,
      "status": "A"
    }
  ],
  "totalCount": 22
}
```

**Response - Error (500):**
```json
{
  "appCode": "OCMS-4007",
  "message": "System error. Please inform Administrator"
}
```

---

### 2.2 OCMS Staff Apply Suspension

Apply PS or TS suspension manually by OCMS Staff Portal.

**Endpoint:** `POST /v1/staff-apply-suspension`

**Request Headers:**
```
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
```

**Request Body:**
```json
{
  "noticeNo": ["500500303J", "500500304J"],
  "suspensionType": "PS",
  "reasonOfSuspension": "CFA",
  "suspensionRemarks": "Incorrect issuance by Parking Warden",
  "officerAuthorisingSuspension": "JOHNLEE",
  "caseNo": ""
}
```

| Field | Type | Required | Max Length | Description |
|-------|------|----------|------------|-------------|
| noticeNo | Array[String] | Yes | - | List of notice numbers to suspend. Max 10 per batch. |
| suspensionType | String | Yes | 2 | "PS" for Permanent Suspension |
| reasonOfSuspension | String | Yes | 3 | PS Code (see Section 4.1) |
| suspensionRemarks | String | No | 200 | Remarks for the suspension |
| officerAuthorisingSuspension | String | Yes | 50 | OIC username |
| caseNo | String | No | 20 | Case number (if applicable) |

**Allowed PS Codes for OCMS Staff:**
```
ANS, CAN, CFA, CFP, DBB, DIP, FCT, FOR, FTC, IST, MID, OTH,
RIP, RP2, SCT, SLC, SSV, VCT, VST, WWC, WWF, WWP
```

**Response - Success (200):**
```json
{
  "totalProcessed": 2,
  "successCount": 2,
  "errorCount": 0,
  "results": [
    {
      "noticeNo": "500500303J",
      "srNo": "1234",
      "appCode": "OCMS-2000",
      "message": "PS Success"
    },
    {
      "noticeNo": "500500304J",
      "srNo": "1235",
      "appCode": "OCMS-2000",
      "message": "PS Success"
    }
  ]
}
```

**Response - Partial Success (200):**
```json
{
  "totalProcessed": 2,
  "successCount": 1,
  "errorCount": 1,
  "results": [
    {
      "noticeNo": "500500303J",
      "srNo": "1234",
      "appCode": "OCMS-2000",
      "message": "PS Success"
    },
    {
      "noticeNo": "500500304J",
      "appCode": "OCMS-4002",
      "message": "PS Code cannot be applied due to Last Processing Stage is not among the eligible stages."
    }
  ]
}
```

**Response - Already PS (200):**
```json
{
  "totalProcessed": 1,
  "successCount": 1,
  "errorCount": 0,
  "results": [
    {
      "noticeNo": "500500303J",
      "srNo": "1234",
      "appCode": "OCMS-2001",
      "message": "Success Notice already PS"
    }
  ]
}
```

---

### 2.3 OCMS Backend Apply Suspension (Internal)

Auto-apply PS or TS suspension by OCMS Backend (cron jobs).

**Endpoint:** `POST /v1/apply-suspension`

**Request Headers:**
```
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
```

**Request Body:**
```json
{
  "noticeNo": ["500500303J"],
  "suspensionType": "PS",
  "reasonOfSuspension": "ANS",
  "suspensionRemarks": "Auto suspension - Advisory Notice",
  "suspensionSource": "OCMS",
  "officerAuthorisingSuspension": "ocmsizmgr"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| noticeNo | Array[String] | Yes | List of notice numbers |
| suspensionType | String | Yes | "PS" for Permanent Suspension |
| reasonOfSuspension | String | Yes | PS Code |
| suspensionRemarks | String | No | Remarks (max 200 chars) |
| suspensionSource | String | No | Default: "OCMS" |
| officerAuthorisingSuspension | String | Yes | System user ID |

**Allowed PS Codes for OCMS Backend:**
```
ANS, DBB, DIP, FOR, MID, RIP, RP2, FP, PRA, CFP, IST, WWC, WWF, WWP
```

**Response - Success (200):**
```json
{
  "totalProcessed": 1,
  "successCount": 1,
  "errorCount": 0,
  "results": [
    {
      "noticeNo": "500500303J",
      "srNo": "1236",
      "appCode": "OCMS-2000",
      "message": "PS Success"
    }
  ]
}
```

---

### 2.4 OCMS Staff Revive Suspension

Revive PS or TS suspension manually by OCMS Staff Portal.

**Endpoint:** `POST /v1/staff-revive-suspension`

**Request Headers:**
```
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
```

**Request Body:**
```json
{
  "noticeNo": ["500500303J", "500500304J"],
  "suspensionType": "PS",
  "revivalReason": "MAN",
  "revivalRemarks": "Issue resolved - appeal withdrawn",
  "officerAuthorisingRevival": "JOHNLEE"
}
```

| Field | Type | Required | Max Length | Description |
|-------|------|----------|------------|-------------|
| noticeNo | Array[String] | Yes | - | List of notice numbers. Max 10 per batch. |
| suspensionType | String | Yes | 2 | "PS" for Permanent Suspension |
| revivalReason | String | Yes | 3 | Revival reason code |
| revivalRemarks | String | No | 200 | Remarks for the revival |
| officerAuthorisingRevival | String | Yes | 50 | OIC username |

**Response - Success (200):**
```json
{
  "totalProcessed": 2,
  "successCount": 2,
  "errorCount": 0,
  "results": [
    {
      "noticeNo": "500500303J",
      "appCode": "OCMS-2000",
      "message": "PS Revival Success"
    },
    {
      "noticeNo": "500500304J",
      "appCode": "OCMS-2000",
      "message": "PS Revival Success"
    }
  ]
}
```

**Response - Error (200):**
```json
{
  "totalProcessed": 1,
  "successCount": 0,
  "errorCount": 1,
  "results": [
    {
      "noticeNo": "500500303J",
      "appCode": "OCMS-4005",
      "message": "No active PS suspension found in suspended_notice table"
    }
  ]
}
```

---

### 2.5 PS Report - By System

Generate PS Report grouped by system (OCMS, PLUS, Backend).

**Endpoint:** `POST /v1/ps-report/by-system`

**Request Headers:**
```
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
```

**Request Body:**
```json
{
  "suspensionDateFrom": "2025-01-01",
  "suspensionDateTo": "2025-12-31",
  "suspensionSource": ["OCMS", "PLUS"],
  "page": 1,
  "limit": 50,
  "$sort[suspensionDate]": -1
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| suspensionDateFrom | String (Date) | Yes | Start date (YYYY-MM-DD). Max range: 1 year. |
| suspensionDateTo | String (Date) | Yes | End date (YYYY-MM-DD). Max range: 1 year. |
| suspensionSource | Array[String] | No | Filter by source: "OCMS", "PLUS" |
| page | Integer | No | Page number (default: 1) |
| limit | Integer | No | Records per page (default: 50) |
| $sort[fieldName] | Integer | No | Sort direction: 1 (ASC), -1 (DESC) |

**Response - Success (200):**
```json
{
  "appCode": "OCMS-2000",
  "message": "Success",
  "data": [
    {
      "vehicleNo": "SBA1234A",
      "noticeNo": "500500303J",
      "noticeType": "PN",
      "vehicleRegistrationType": "PV",
      "vehicleCategory": "M",
      "computerRuleCode": "01",
      "dateTimeOfOffence": "2025-03-15T10:30:00",
      "placeOfOffence": "BLK 123 ANG MO KIO AVE 3",
      "source": "OCMS",
      "refundIdentifiedDate": null,
      "suspensionDate": "2025-03-22T19:00:02",
      "suspensionReason": "CFA",
      "previousPsSuspensionReason": null,
      "previousPsSuspensionDate": null
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 50,
    "totalRecords": 150,
    "totalPages": 3
  }
}
```

---

### 2.6 PS Report - By Officer

Generate PS Report grouped by officer.

**Endpoint:** `POST /v1/ps-report/by-officer`

**Request Headers:**
```
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
```

**Request Body:**
```json
{
  "suspensionDateFrom": "2025-01-01",
  "suspensionDateTo": "2025-12-31",
  "psingOfficerName": ["JOHNLEE", "MARYTAN"],
  "authorisingOfficerName": ["ADMIN1"],
  "page": 1,
  "limit": 50,
  "$sort[suspensionDate]": -1
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| suspensionDateFrom | String (Date) | Yes | Start date (YYYY-MM-DD). Max range: 1 year. |
| suspensionDateTo | String (Date) | Yes | End date (YYYY-MM-DD). Max range: 1 year. |
| psingOfficerName | Array[String] | No | Filter by PSing Officer (multi-select) |
| authorisingOfficerName | Array[String] | No | Filter by Authorising Officer (multi-select) |
| page | Integer | No | Page number (default: 1) |
| limit | Integer | No | Records per page (default: 50) |
| $sort[fieldName] | Integer | No | Sort direction: 1 (ASC), -1 (DESC) |

**Response - Success (200):**
```json
{
  "appCode": "OCMS-2000",
  "message": "Success",
  "data": [
    {
      "vehicleNo": "SBA1234A",
      "noticeNo": "500500303J",
      "noticeType": "PN",
      "vehicleRegistrationType": "PV",
      "vehicleCategory": "M",
      "computerRuleCode": "01",
      "dateTimeOfOffence": "2025-03-15T10:30:00",
      "placeOfOffence": "BLK 123 ANG MO KIO AVE 3",
      "authorisingOfficer": "ADMIN1",
      "psingOfficer": "JOHNLEE",
      "refundIdentifiedDate": null,
      "suspensionDate": "2025-03-22T19:00:02",
      "suspensionReason": "CFA",
      "suspensionRemarks": "Incorrect issuance",
      "previousPsSuspensionReason": null,
      "previousPsSuspensionDate": null
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 50,
    "totalRecords": 75,
    "totalPages": 2
  }
}
```

---

## 3. External APIs (PLUS Integration)

### 3.1 PLUS Apply Suspension

Apply PS or TS suspension from PLUS Staff Portal.

**Endpoint:** `POST /v1/plus-apply-suspension`

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| checking | Boolean | No | If `true`, validate only (dry-run). Default: `false` |

**Request Headers:**
```
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
```

**Request Body:**
```json
{
  "noticeNo": ["500500303J"],
  "caseNo": "P250113001",
  "suspensionType": "PS",
  "reasonOfSuspension": "APP",
  "suspensionRemarks": "Appeal accepted by PLM",
  "officerAuthorisingSuspension": "PLU_OFFICER1"
}
```

| Field | Type | Required | Max Length | Description |
|-------|------|----------|------------|-------------|
| noticeNo | Array[String] | Yes | - | List of notice numbers |
| caseNo | String | Yes | 20 | Appeal case number from PLUS |
| suspensionType | String | Yes | 2 | "PS" for Permanent Suspension |
| reasonOfSuspension | String | Yes | 3 | PS Code (see allowed codes below) |
| suspensionRemarks | String | No | 200 | Remarks for the suspension |
| officerAuthorisingSuspension | String | Yes | 50 | PLU Officer ID |

**Allowed PS Codes for PLUS:**
```
APP, CAN, CFA, OTH, VST
```

**Response - Success (200):**
```json
{
  "totalProcessed": 1,
  "successCount": 1,
  "errorCount": 0,
  "results": [
    {
      "noticeNo": "500500303J",
      "srNo": "1237",
      "appCode": "OCMS-2000",
      "message": "PS Success"
    }
  ]
}
```

**Response - Validation Only (checking=true) - Success (200):**
```json
{
  "totalProcessed": 1,
  "successCount": 1,
  "errorCount": 0,
  "results": [
    {
      "noticeNo": "500500303J",
      "appCode": "OCMS-2000",
      "message": "Validation passed - eligible for PS"
    }
  ]
}
```

**Response - Validation Only (checking=true) - Error (200):**
```json
{
  "totalProcessed": 1,
  "successCount": 0,
  "errorCount": 1,
  "results": [
    {
      "noticeNo": "500500303J",
      "appCode": "OCMS-4003",
      "message": "Paid/partially paid notices only allow APP, CFA, or VST"
    }
  ]
}
```

---

### 3.2 PLUS Revive Suspension

Revive PS or TS suspension from PLUS Staff Portal.

**Endpoint:** `POST /v1/plus-revive-suspension`

**Request Headers:**
```
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
```

**Request Body:**
```json
{
  "noticeNo": ["500500303J"],
  "suspensionType": "PS",
  "revivalReason": "MAN",
  "revivalRemarks": "Appeal case closed",
  "officerAuthorisingRevival": "PLU_OFFICER1"
}
```

| Field | Type | Required | Max Length | Description |
|-------|------|----------|------------|-------------|
| noticeNo | Array[String] | Yes | - | List of notice numbers |
| suspensionType | String | Yes | 2 | "PS" for Permanent Suspension |
| revivalReason | String | Yes | 3 | Revival reason code |
| revivalRemarks | String | No | 200 | Remarks for the revival |
| officerAuthorisingRevival | String | Yes | 50 | PLU Officer ID |

**Response - Success (200):**
```json
{
  "totalProcessed": 1,
  "successCount": 1,
  "errorCount": 0,
  "results": [
    {
      "noticeNo": "500500303J",
      "appCode": "OCMS-2000",
      "message": "PS Revival Success"
    }
  ]
}
```

---

## 4. Reference Data

### 4.1 PS Suspension Codes

| Code | Description | OCMS Staff | OCMS Backend | PLUS |
|------|-------------|------------|--------------|------|
| ANS | Advisory Notice | Yes | Yes | No |
| APP | Appeal Accepted | No | No | Yes |
| CAN | Cancelled Advisory Notice | Yes | No | Yes |
| CFA | Cancelled By Field Admin Unit | Yes | No | Yes |
| CFP | Court Fine Paid | Yes | Yes | No |
| DBB | Double Booking | Yes | Yes | No |
| DIP | Diplomatic Vehicle | Yes | Yes | No |
| FCT | Foreigner Cannot Be Traced | Yes | No | No |
| FOR | Foreign Vehicle | Yes | Yes | No |
| FP | Fully Paid | No | Yes | No |
| FTC | Foreigner Left the Country | Yes | No | No |
| IST | Instalment Plan Granted | Yes | Yes | No |
| MID | MINDEF Vehicle | Yes | Yes | No |
| OTH | Others | Yes | No | Yes |
| PRA | Payment with Reduced Amount | No | Yes | No |
| RIP | Motorist Deceased On/After Offence Date | Yes | Yes | No |
| RP2 | Motorist Deceased Before Offence Date | Yes | Yes | No |
| SCT | Singaporean Cannot Be Traced | Yes | No | No |
| SLC | Singaporean Left the Country | Yes | No | No |
| SSV | Singapore Security Vehicle | Yes | No | No |
| VCT | Vehicle Cannot Be Traced | Yes | No | No |
| VST | Valid Season Ticket | Yes | No | Yes |
| WWC | Withdrawn with Composition Amount | Yes | Yes | No |
| WWF | Withdrawn with Fine | Yes | Yes | No |
| WWP | Withdrawn without Payment | Yes | Yes | No |

### 4.2 Exception Codes (Special Rules)

These codes have special behavior:
- Allow TS on top of PS
- Allow FP/PRA without revival
- Allow stacking

```
DIP, FOR, MID, RIP, RP2
```

### 4.3 Refund Trigger Codes

These codes trigger refund when applied to paid notices:
```
APP, CFA, VST
```

### 4.4 CRS Codes (Payment-Triggered)

Applied automatically by backend during payment processing:
```
FP, PRA
```

### 4.5 Allowed Processing Stages for PS

| Stage Code | Description | Allowed |
|------------|-------------|---------|
| NPA | New Parking Advice | Yes |
| ROV | Registered Owner/Vehicle | Yes |
| ENA | Enable Address | Yes |
| RD1 | Reminder 1 | Yes |
| RD2 | Reminder 2 | Yes |
| RR3 | Reminder 3 (Registered) | Yes |
| DN1 | Demand Note 1 | Yes |
| DN2 | Demand Note 2 | Yes |
| DR3 | Demand Note 3 (Registered) | Yes |
| CPC | Court Processing Complete | Yes |
| CFC | Court Fine Complete | Yes |
| CRT | Court (Active) | No |
| CRC | Court (Active) | No |

---

## 5. Error Codes

### 5.1 Success Codes (2xxx)

| Code | Message | Description |
|------|---------|-------------|
| OCMS-2000 | PS Success | Operation completed successfully |
| OCMS-2001 | Success Notice already PS | Notice already has same PS code (idempotent) |
| OCMS-2002 | Request accepted for processing | Async processing accepted |

### 5.2 Client Errors (4xxx)

| Code | Message | Scenario |
|------|---------|----------|
| OCMS-4000 | Invalid input format or failed validation | Missing mandatory field |
| OCMS-4000 | Suspension Source is missing | suspensionSource not provided |
| OCMS-4000 | Source not authorized to use this Suspension Code | Source permission denied |
| OCMS-4001 | Invalid Notice Number | Notice not found in database |
| OCMS-4001 | Unauthorized Access | JWT token invalid/expired |
| OCMS-4002 | Notice is under Court processing | Notice at CRT/CRC stage |
| OCMS-4002 | PS Code cannot be applied due to Last Processing Stage is not among the eligible stages | Stage not allowed |
| OCMS-4003 | Paid/partially paid notices only allow APP, CFA, or VST | Invalid PS code for paid notice |
| OCMS-4005 | No active PS suspension found in suspended_notice table | Revival target not found |
| OCMS-4007 | Suspension Type is missing | suspensionType not provided |
| OCMS-4007 | Reason of Suspension is missing | reasonOfSuspension not provided |
| OCMS-4007 | Officer Authorising Suspension is missing | officer not provided |
| OCMS-4007 | Invalid Suspension Type | suspensionType not TS or PS |
| OCMS-4007 | Invalid Date of Suspension | Future date or invalid format |
| OCMS-4007 | Suspension remarks exceed 200 characters | Remarks too long |
| OCMS-4007 | Batch size exceeds limit of 10 notices | Too many notices in single request |
| OCMS-4007 | System error. Please inform Administrator | Undefined error |
| OCMS-4008 | Cannot apply PS-FP/PRA on existing PS | CRS PS not allowed on non-exception PS |

### 5.3 System Errors (5xxx)

| Code | Message | Description |
|------|---------|-------------|
| OCMS-5000 | Something went wrong. Please try again later | Internal server error |
| OCMS-5001 | Unable to save data due to server issues | Database write failed |
| OCMS-5002 | The request timed out | Request timeout |
| OCMS-5004 | Service unavailable | System maintenance |

---

## 6. Database Tables

### 6.1 Tables Updated by PS APIs

| Table | Database | Description |
|-------|----------|-------------|
| ocms_valid_offence_notice | Intranet | Main notice table - suspension fields updated |
| eocms_valid_offence_notice | Internet | eService notice table - synced for public portal |
| ocms_suspended_notice | Intranet | Suspension history/audit table |

### 6.2 Fields Updated in ocms_valid_offence_notice

| Field | Type | Description |
|-------|------|-------------|
| suspension_type | VARCHAR(2) | "PS" for Permanent Suspension |
| epr_reason_of_suspension | VARCHAR(3) | PS Code |
| epr_date_of_suspension | TIMESTAMP | Date/time suspension applied |
| due_date_of_revival | TIMESTAMP | Always NULL for PS |

### 6.3 Fields in ocms_suspended_notice

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| notice_no | VARCHAR(10) | Yes | Notice number |
| date_of_suspension | TIMESTAMP | Yes | When suspension applied |
| sr_no | INTEGER | Yes | Serial number (sequence) |
| suspension_source | VARCHAR(10) | Yes | OCMS, PLUS |
| case_no | VARCHAR(20) | No | Appeal case number |
| suspension_type | VARCHAR(2) | Yes | PS |
| reason_of_suspension | VARCHAR(3) | Yes | PS Code |
| officer_authorising_suspension | VARCHAR(50) | Yes | OIC username |
| due_date_of_revival | TIMESTAMP | No | Always NULL for PS |
| suspension_remarks | VARCHAR(200) | No | Remarks |
| date_of_revival | TIMESTAMP | No | When revived |
| revival_reason | VARCHAR(3) | No | Revival code |
| officer_authorising_revival | VARCHAR(50) | No | Revival OIC |
| revival_remarks | VARCHAR(200) | No | Revival remarks |
| cre_date | TIMESTAMP | Yes | Record creation date |
| cre_user_id | VARCHAR(50) | Yes | Created by |
| upd_date | TIMESTAMP | No | Last update date |
| upd_user_id | VARCHAR(50) | No | Updated by |

---

## 7. Validation Rules Summary

### 7.1 Request Validation

| Rule ID | Field | Validation | Error Code |
|---------|-------|------------|------------|
| VAL-001 | noticeNo | Required, non-empty array | OCMS-4001 |
| VAL-002 | noticeNo | Max 10 notices per batch | OCMS-4007 |
| VAL-003 | suspensionType | Required, must be "PS" | OCMS-4007 |
| VAL-004 | reasonOfSuspension | Required, valid PS code | OCMS-4007 |
| VAL-005 | officerAuthorisingSuspension | Required | OCMS-4007 |
| VAL-006 | suspensionRemarks | Max 200 characters | OCMS-4007 |
| VAL-007 | dateOfSuspension | Not future date | OCMS-4007 |

### 7.2 Business Validation

| Rule ID | Rule | Error Code |
|---------|------|------------|
| BIZ-001 | Notice must exist in database | OCMS-4001 |
| BIZ-002 | Source must be authorized for PS code | OCMS-4000 |
| BIZ-003 | Last Processing Stage must be allowed | OCMS-4002 |
| BIZ-004 | Not at Court stage (CRT/CRC) | OCMS-4002 |
| BIZ-005 | Paid notices only allow APP/CFA/VST | OCMS-4003 |
| BIZ-006 | CRS PS (FP/PRA) only on exception codes | OCMS-4008 |

---

## 8. Change Log

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-05 | Technical Documentation | Initial version |

---

*End of Document*

# API Plan: OCMS 15 - Change Processing Stage

## Overview

| Attribute | Value |
| --- | --- |
| API Name | Change Processing Stage API |
| Version | v1.0 |
| Author | Claude |
| Created Date | 18/01/2026 |
| Last Updated | 18/01/2026 |
| Status | Draft |
| Related Document | OCMS 15 Technical Doc |

---

## 1. Purpose

The Change Processing Stage API allows authorized users to manually change the processing stage of offence notices. This feature supports batch operations from OCMS Staff Portal, external integrations with PLUS Portal, and internal Toppan cron processing. The API ensures eligibility validation, audit trail recording, and report generation.

---

## 2. API Endpoints

### 2.1 Search Notices for Change Processing Stage

| Attribute | Value |
| --- | --- |
| Method | POST |
| URL | `/api/v1/change-processing-stage/search` |
| Authentication | Bearer Token |
| Description | Search notices based on criteria and return segregated lists of eligible vs ineligible notices |

#### Request

**Headers:**

| Header | Required | Description |
| --- | --- | --- |
| Authorization | Yes | Bearer {token} |
| Content-Type | Yes | application/json |

**Request Body:**

```json
{
  "noticeNo": "N-001",
  "idNo": "S1234567A",
  "vehicleNo": "SBA1234A",
  "currentProcessingStage": "DN1",
  "dateOfCurrentProcessingStage": "2025-12-20"
}
```

**Request Body Schema:**

| Field | Type | Required | Validation | Description |
| --- | --- | --- | --- | --- |
| noticeNo | string | No | max: 10 chars | Notice number |
| idNo | string | No | max: 20 chars | Offender ID number (NRIC/FIN/Passport) |
| vehicleNo | string | No | max: 14 chars | Vehicle registration number |
| currentProcessingStage | string | No | max: 3 chars | Current processing stage code |
| dateOfCurrentProcessingStage | date | No | format: yyyy-MM-dd | Date of current processing stage |

> **Note:** At least one search criterion is required.

#### Response

**Success Response (200 OK):**

```json
{
  "eligibleNotices": [
    {
      "noticeNo": "N-001",
      "offenceType": "SP",
      "offenceDateTime": "2025-12-01 10:30:00",
      "offenderName": "John Doe",
      "offenderId": "S1234567A",
      "vehicleNo": "SBA1234A",
      "currentProcessingStage": "DN1",
      "currentProcessingStageDate": "2025-12-15 09:00:00",
      "suspensionType": null,
      "suspensionStatus": null,
      "ownerDriverIndicator": "D",
      "entityType": null
    }
  ],
  "ineligibleNotices": [
    {
      "noticeNo": "N-002",
      "offenceType": "SP",
      "offenceDateTime": "2025-12-01 11:00:00",
      "offenderName": "Jane Smith",
      "offenderId": "S9876543B",
      "vehicleNo": "SBA5678B",
      "currentProcessingStage": "CRT",
      "currentProcessingStageDate": "2025-12-18 14:00:00",
      "reasonCode": "OCMS.CPS.SEARCH.COURT_STAGE",
      "reasonMessage": "Notice is in court stage"
    }
  ],
  "summary": {
    "total": 2,
    "eligible": 1,
    "ineligible": 1
  }
}
```

**Response Schema:**

| Field | Type | Description |
| --- | --- | --- |
| eligibleNotices | array | Notices eligible for stage change |
| eligibleNotices[].noticeNo | string | Notice number |
| eligibleNotices[].offenceType | string | Offence type code |
| eligibleNotices[].offenceDateTime | datetime | Offence date and time |
| eligibleNotices[].offenderName | string | Offender name |
| eligibleNotices[].offenderId | string | Offender ID number |
| eligibleNotices[].vehicleNo | string | Vehicle number |
| eligibleNotices[].currentProcessingStage | string | Current stage code |
| eligibleNotices[].currentProcessingStageDate | datetime | Current stage date |
| eligibleNotices[].suspensionType | string | Suspension type (TS/PS/null) |
| eligibleNotices[].suspensionStatus | string | Suspension status |
| eligibleNotices[].ownerDriverIndicator | string | D=Driver, O=Owner, H=Hirer, DIR=Director |
| eligibleNotices[].entityType | string | Entity type for company |
| ineligibleNotices | array | Notices NOT eligible for stage change |
| ineligibleNotices[].reasonCode | string | Reason code for ineligibility |
| ineligibleNotices[].reasonMessage | string | Human-readable reason |
| summary | object | Summary statistics |
| summary.total | integer | Total notices found |
| summary.eligible | integer | Eligible count |
| summary.ineligible | integer | Ineligible count |

---

### 2.2 Validate Change Processing Stage

| Attribute | Value |
| --- | --- |
| Method | POST |
| URL | `/api/v1/change-processing-stage/validate` |
| Authentication | Bearer Token |
| Description | Validate notices BEFORE submission to identify changeable vs non-changeable notices |

#### Request

**Request Body:**

```json
{
  "notices": [
    {
      "noticeNo": "N-001",
      "currentStage": "DN1",
      "offenderType": "DRIVER",
      "entityType": null
    }
  ],
  "newProcessingStage": "DN2",
  "reasonOfChange": "SUP",
  "remarks": "Speed up processing"
}
```

**Request Body Schema:**

| Field | Type | Required | Validation | Description |
| --- | --- | --- | --- | --- |
| notices | array | Yes | not empty | List of notices to validate |
| notices[].noticeNo | string | Yes | max: 10 chars | Notice number |
| notices[].currentStage | string | No | max: 3 chars | Current stage (fetched from DB if not provided) |
| notices[].offenderType | string | No | DRIVER/OWNER/HIRER/DIRECTOR | Offender type |
| notices[].entityType | string | No | - | Entity type (for CFC validation) |
| newProcessingStage | string | Yes | max: 3 chars | Target processing stage |
| reasonOfChange | string | No | max: 3 chars | Reason code |
| remarks | string | Conditional | max: 200 chars | Remarks (required if reasonOfChange=OTH) |

#### Response

**Success Response (200 OK):**

```json
{
  "changeableNotices": [
    {
      "noticeNo": "N-001",
      "currentStage": "DN1",
      "offenderType": "DRIVER",
      "entityType": null,
      "message": "Eligible for stage change to DN2"
    }
  ],
  "nonChangeableNotices": [
    {
      "noticeNo": "N-002",
      "currentStage": "CRT",
      "code": "OCMS.CPS.COURT_STAGE",
      "message": "Notice is in court stage"
    }
  ],
  "summary": {
    "total": 2,
    "changeable": 1,
    "nonChangeable": 1
  }
}
```

---

### 2.3 Submit Change Processing Stage (Batch)

| Attribute | Value |
| --- | --- |
| Method | POST |
| URL | `/api/v1/change-processing-stage` |
| Authentication | Bearer Token |
| Description | Submit batch request to change processing stage for multiple notices |

#### Request

**Request Body:**

```json
{
  "items": [
    {
      "noticeNo": "N-001",
      "newStage": "DN2",
      "reason": "SUP",
      "remark": "Verified by AO",
      "dhMhaCheck": false,
      "isConfirmation": false
    }
  ]
}
```

**Request Body Schema:**

| Field | Type | Required | Validation | Description |
| --- | --- | --- | --- | --- |
| items | array | Yes | not empty | List of notices to change |
| items[].noticeNo | string | Yes | max: 10 chars | Notice number |
| items[].newStage | string | No | max: 3 chars | New stage (can be derived from StageMap) |
| items[].reason | string | No | max: 3 chars | Reason code |
| items[].remark | string | No | max: 200 chars | Additional remarks |
| items[].dhMhaCheck | boolean | No | - | Flag to update DH MHA check |
| items[].isConfirmation | boolean | No | - | Confirmation flag for duplicate override |

#### Response

**Success Response (200 OK):**

```json
{
  "status": "PARTIAL",
  "summary": {
    "requested": 5,
    "succeeded": 3,
    "failed": 2
  },
  "results": [
    {
      "noticeNo": "N-001",
      "outcome": "UPDATED",
      "previousStage": "DN1",
      "newStage": "DN2",
      "message": "Stage changed successfully"
    },
    {
      "noticeNo": "N-002",
      "outcome": "FAILED",
      "code": "OCMS.CPS.NOT_FOUND",
      "message": "VON not found"
    },
    {
      "noticeNo": "N-003",
      "outcome": "WARNING",
      "code": "OCMS.CPS.ELIG.EXISTING_CHANGE_TODAY",
      "message": "Notice No. N-003 has existing stage change update. Please confirm to proceed."
    }
  ],
  "reportUrl": "https://blob.storage/reports/change-stage/ChangeStageReport_20251220.xlsx",
  "existingRecordWarning": true
}
```

**Response Schema:**

| Field | Type | Description |
| --- | --- | --- |
| status | string | SUCCESS / PARTIAL / FAILED / WARNING |
| summary.requested | integer | Total notices requested |
| summary.succeeded | integer | Successfully updated count |
| summary.failed | integer | Failed count (includes warnings) |
| results | array | Per-notice results |
| results[].noticeNo | string | Notice number |
| results[].outcome | string | UPDATED / FAILED / WARNING |
| results[].previousStage | string | Previous stage (for success) |
| results[].newStage | string | New stage (for success) |
| results[].code | string | Error code (for failure) |
| results[].message | string | Result message |
| reportUrl | string | URL to download Excel report |
| existingRecordWarning | boolean | Indicates duplicate record exists |

---

### 2.4 Retrieve Change Stage Reports

| Attribute | Value |
| --- | --- |
| Method | GET |
| URL | `/api/v1/change-processing-stage/reports` |
| Authentication | Bearer Token |
| Description | Retrieve list of change stage reports by date range |

#### Request

**Query Parameters:**

| Parameter | Type | Required | Validation | Description |
| --- | --- | --- | --- | --- |
| startDate | date | Yes | format: yyyy-MM-dd | Start date (inclusive) |
| endDate | date | Yes | format: yyyy-MM-dd | End date (inclusive) |

> **Note:** Date range cannot exceed 90 days.

#### Response

**Success Response (200 OK):**

```json
{
  "reports": [
    {
      "reportDate": "2025-12-20",
      "generatedBy": "USER01",
      "noticeCount": 15,
      "reportUrl": "reports/change-stage/ChangeStageReport_20251220_USER01.xlsx"
    }
  ],
  "totalReports": 1,
  "totalNotices": 15
}
```

---

### 2.5 Download Change Stage Report

| Attribute | Value |
| --- | --- |
| Method | GET |
| URL | `/api/v1/change-processing-stage/report/{reportId}` |
| Authentication | Bearer Token |
| Description | Download individual change stage report as Excel file |

#### Request

**Path Parameters:**

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| reportId | string | Yes | Report filename/ID |

#### Response

**Success Response (200 OK):**

- Content-Type: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- Content-Disposition: `attachment; filename="ChangeStageReport_20251220.xlsx"`
- Body: Excel file binary

---

### 2.6 PLUS Manual Change Processing Stage (External API)

| Attribute | Value |
| --- | --- |
| Method | POST |
| URL | `/api/v1/external/plus/change-processing-stage` |
| Authentication | API Key |
| Description | External API for PLUS Portal to manually change processing stages |

#### Request

**Request Body:**

```json
{
  "noticeNo": ["N-001", "N-002"],
  "lastStageName": "DN1",
  "nextStageName": "DN2",
  "lastStageDate": "2025-09-25T06:58:42",
  "newStageDate": "2025-09-30T06:58:42",
  "offenceType": "D",
  "source": "005"
}
```

**Request Body Schema:**

| Field | Type | Required | Validation | Description |
| --- | --- | --- | --- | --- |
| noticeNo | array | Yes | not empty | List of notice numbers |
| lastStageName | string | Yes | max: 3 chars | Current/last processing stage |
| nextStageName | string | Yes | max: 3 chars | New processing stage |
| lastStageDate | string | No | ISO 8601 | Last stage date |
| newStageDate | string | No | ISO 8601 | New stage date |
| offenceType | string | Yes | O/D/H/DIR | Offence type (Owner/Driver/Hirer/Director) |
| source | string | Yes | max: 8 chars | Source code (005 for PLUS) |

#### Response

**Success Response (200 OK):**

```json
{
  "status": "SUCCESS",
  "message": "Stage change processed successfully",
  "noticeCount": 2
}
```

**Error Response (422 Unprocessable Entity):**

```json
{
  "status": "FAILED",
  "code": "OCMS-4000",
  "message": "Stage transition not allowed: DN1 -> CFC"
}
```

---

### 2.7 Toppan Stage Update (Internal API)

| Attribute | Value |
| --- | --- |
| Method | POST |
| URL | `/api/v1/internal/toppan/update-stages` |
| Authentication | Internal |
| Description | Internal API called by Toppan cron to update VON processing stages |

#### Request

**Request Body:**

```json
{
  "noticeNumbers": ["N-001", "N-002", "N-003"],
  "currentStage": "DN1",
  "processingDate": "2025-12-19T00:30:00"
}
```

**Request Body Schema:**

| Field | Type | Required | Validation | Description |
| --- | --- | --- | --- | --- |
| noticeNumbers | array | Yes | not empty | List of notice numbers processed by Toppan |
| currentStage | string | Yes | max: 3 chars | Current processing stage |
| processingDate | datetime | Yes | ISO 8601 | Processing date/time |

#### Response

**Success Response (200 OK):**

```json
{
  "totalNotices": 3,
  "automaticUpdates": 2,
  "manualUpdates": 1,
  "skipped": 0,
  "errors": null,
  "success": true
}
```

**Response Schema:**

| Field | Type | Description |
| --- | --- | --- |
| totalNotices | integer | Total notices in request |
| automaticUpdates | integer | Notices updated as automatic (with amount_payable calculation) |
| manualUpdates | integer | Notices updated as manual (without amount_payable change) |
| skipped | integer | Notices skipped (not found, stage mismatch) |
| errors | array | List of error messages |
| success | boolean | Overall success flag |

---

## 3. Error Responses

### Standard Error Format

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable error message"
  }
}
```

### Error Codes

| HTTP Status | Error Code | Description | Resolution |
| --- | --- | --- | --- |
| 400 | OCMS.CPS.INVALID_FORMAT | Request validation failed | Check request body format |
| 400 | OCMS.CPS.MISSING_DATA | Required field missing | Provide all required fields |
| 400 | INVALID_DATE_RANGE | Date range exceeds 90 days | Reduce date range |
| 404 | OCMS.CPS.NOT_FOUND | VON not found | Verify notice number exists |
| 404 | REPORT_NOT_FOUND | Report file not found | Verify report ID |
| 422 | OCMS.CPS.ELIG.COURT_STAGE | Notice is at court stage | Cannot change court stage notices |
| 422 | OCMS.CPS.ELIG.PS_BLOCKED | Permanent suspension active | Cannot change PS notices |
| 422 | OCMS.CPS.ELIG.INELIGIBLE_STAGE | Stage not allowed for role | Check eligible stages for role |
| 422 | OCMS.CPS.ELIG.ROLE_CONFLICT | Cannot determine offender role | Check ONOD data |
| 422 | OCMS.CPS.REMARKS_REQUIRED | Remarks mandatory for OTH reason | Provide remarks |
| 422 | OCMS-4000 | Stage transition not allowed | Check stage map |
| 422 | OCMS-4004 | CFC not allowed from PLUS | Use OCMS Staff Portal for CFC |
| 500 | OCMS.CPS.UNEXPECTED | Unexpected server error | Contact support |

---

## 4. Data Mapping

### Request to Database

| API Field | Database Table | Database Field | Transformation |
| --- | --- | --- | --- |
| noticeNo | ocms_change_of_processing | notice_no | Direct mapping |
| newStage | ocms_change_of_processing | new_processing_stage | Direct mapping |
| reason | ocms_change_of_processing | reason_of_change | Direct mapping |
| remark | ocms_change_of_processing | remarks | Direct mapping |
| - | ocms_change_of_processing | last_processing_stage | From VON.last_processing_stage |
| - | ocms_change_of_processing | date_of_change | Current date |
| - | ocms_change_of_processing | authorised_officer | From user session |
| - | ocms_change_of_processing | source | OCMS/PLUS/SYSTEM |

### VON Update Mapping

| Field | Source | Description |
| --- | --- | --- |
| prev_processing_stage | old last_processing_stage | Previous stage backup |
| prev_processing_date | old last_processing_date | Previous date backup |
| last_processing_stage | old next_processing_stage or newStage | New current stage |
| last_processing_date | current datetime | Current timestamp |
| next_processing_stage | ocms_parameter (NEXT_STAGE_{newStage}) | Computed next stage |
| next_processing_date | current datetime + STAGEDAYS | Computed next date |
| amount_payable | Calculated | Based on stage transition |

---

## 5. Database Tables

### Primary Tables

| Table Name | Purpose |
| --- | --- |
| ocms_valid_offence_notice | Main notice data (VON) |
| ocms_offence_notice_owner_driver | Offender details (ONOD) |
| ocms_change_of_processing | Audit trail for stage changes |
| ocms_stage_map | Stage transition mapping |
| ocms_parameter | System parameters (NEXT_STAGE, STAGEDAYS) |

---

## 6. Dependencies

| Service/System | Type | Purpose |
| --- | --- | --- |
| OCMS Admin API Intranet | Backend | API service |
| Azure Blob Storage | Storage | Report file storage |
| PLUS Portal | External | External integration |
| Toppan Cron | Internal | Automated stage processing |
| Email Service | Internal | Error notification |

---

## 7. Changelog

| Version | Date | Author | Changes |
| --- | --- | --- | --- |
| 1.0 | 18/01/2026 | Claude | Initial version |

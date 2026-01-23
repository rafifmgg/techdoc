# API Plan - OCMS 15: Manage Change Processing Stage

**Document Information**
- Version: 1.3
- Date: 2026-01-22
- Source: Backend Code Analysis + Functional Document v1.2 + Data Dictionary
- Feature: Manual Change Processing Stage & PLUS Integration

---

## 1. Internal APIs (Backend to Frontend)

### 1.1 Search Notices for Change Processing Stage

**API Name:** searchChangeProcessingStage

**Endpoint:**
- UAT: `https://[domain]/ocms/v1/change-processing-stage/search`
- PRD: `https://[domain]/ocms/v1/change-processing-stage/search`

**Method:** POST

**Description:** Search for notices based on criteria and return segregated lists of eligible vs ineligible notices

**Request Payload:**
```json
{
  "noticeNo": "N-001",
  "idNo": "S1234567A",
  "vehicleNo": "SBA1234A",
  "currentProcessingStage": "DN1",
  "dateOfCurrentProcessingStage": "2025-12-20"
}
```

**Request Rules:**
- At least one search criterion is required
- When searching by ID Number: Backend queries ONOD table first, then joins with VON table

**Response (Success with Results):**
```json
{
  "eligibleNotices": [
    {
      "noticeNo": "N-001",
      "offenceType": "SP",
      "offenceDateTime": "2025-12-01T10:30:00",
      "offenderName": "John Doe",
      "offenderId": "S1234567A",
      "offenderAddress": "BLK 123 TAMPINES ST 11 #01-234 SINGAPORE 520123",
      "vehicleNo": "SBA1234A",
      "currentProcessingStage": "DN1",
      "currentProcessingStageDate": "2025-12-15T09:00:00",
      "suspensionType": null,
      "suspensionStatus": null,
      "suspensionReason": null,
      "ownerDriverIndicator": "D",
      "entityType": null
    }
  ],
  "ineligibleNotices": [
    {
      "noticeNo": "N-002",
      "offenceType": "SP",
      "offenceDateTime": "2025-12-01T11:00:00",
      "offenderName": "Jane Smith",
      "offenderId": "S9876543B",
      "offenderAddress": "BLK 456 BEDOK NORTH AVE 1 #05-678 SINGAPORE 460456",
      "vehicleNo": "SBA5678B",
      "currentProcessingStage": "CRT",
      "currentProcessingStageDate": "2025-12-18T14:00:00",
      "suspensionType": null,
      "suspensionStatus": null,
      "suspensionReason": null,
      "reasonCode": "OCMS.CPS.ELIG.COURT_STAGE",
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

**Response (No Results):**
```json
{
  "eligibleNotices": [],
  "ineligibleNotices": [],
  "summary": {
    "total": 0,
    "eligible": 0,
    "ineligible": 0
  }
}
```

**Response (Failure):**
```json
{
  "eligibleNotices": [],
  "ineligibleNotices": [],
  "summary": {
    "total": 0,
    "eligible": 0,
    "ineligible": 0
  }
}
```

---

### 1.2 Validate Change Processing Stage Eligibility

**API Name:** validateChangeProcessingStage

**Endpoint:**
- UAT: `https://[domain]/ocms/v1/change-processing-stage/validate`
- PRD: `https://[domain]/ocms/v1/change-processing-stage/validate`

**Method:** POST

**Description:** Validate notices BEFORE submission to identify which notices are eligible vs ineligible for requested stage change

**Request Payload:**
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

**Request Rules:**
- `notices` array cannot be empty
- If `reasonOfChange` = "OTH", then `remarks` is mandatory
- `newProcessingStage` is required

**Response (Success):**
```json
{
  "changeableNotices": [
    {
      "noticeNo": "N-001",
      "currentStage": "DN1",
      "offenderType": "DRIVER",
      "message": "Eligible for stage change"
    }
  ],
  "nonChangeableNotices": [
    {
      "noticeNo": "N-002",
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

**Response (Failure - Remarks Required):**
```json
{
  "changeableNotices": [],
  "nonChangeableNotices": [
    {
      "noticeNo": "N-001",
      "code": "OCMS.CPS.REMARKS_REQUIRED",
      "message": "Remarks are mandatory when reason for change is 'OTH' (Others)"
    }
  ],
  "summary": {
    "total": 1,
    "changeable": 0,
    "nonChangeable": 1
  }
}
```

---

### 1.3 Submit Change Processing Stage

**API Name:** changeProcessingStage

**Endpoint:**
- UAT: `https://[domain]/ocms/v1/change-processing-stage`
- PRD: `https://[domain]/ocms/v1/change-processing-stage`

**Method:** POST

**Description:** Submit batch change processing stage request

**Request Payload:**
```json
{
  "items": [
    {
      "noticeNo": "N-001",
      "newStage": "DN2",
      "reason": "SUP",
      "remark": "Manual adjustment",
      "source": "PORTAL",
      "dhMhaCheck": false,
      "isConfirmation": false
    }
  ]
}
```

**Request Rules:**
- `items` array cannot be empty
- `noticeNo` is required for each item
- `newStage` is optional (can be derived from current stage)
- `dhMhaCheck` defaults to false
- `isConfirmation` defaults to false (set true to override duplicate record warning)

**Response (Success):**
```json
{
  "status": "SUCCESS",
  "summary": {
    "requested": 1,
    "succeeded": 1,
    "failed": 0
  },
  "results": [
    {
      "noticeNo": "N-001",
      "outcome": "UPDATED",
      "previousStage": "DN1",
      "newStage": "DN2",
      "code": "OCMS-2000",
      "message": "Success"
    }
  ],
  "report": {
    "url": "https://signed-url.xlsx",
    "expiresAt": "2025-10-28T16:00:00+08:00"
  }
}
```

**Response (Partial Success):**
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
      "code": "OCMS-2000",
      "message": "Success"
    },
    {
      "noticeNo": "N-002",
      "outcome": "FAILED",
      "code": "OCMS.CPS.NOT_FOUND",
      "message": "VON not found"
    }
  ],
  "report": {
    "url": "https://signed-url.xlsx",
    "expiresAt": "2025-10-28T16:00:00+08:00"
  }
}
```

**Response (Duplicate Record Warning):**
```json
{
  "status": "PARTIAL",
  "summary": {
    "requested": 1,
    "succeeded": 0,
    "failed": 1
  },
  "results": [
    {
      "noticeNo": "N-001",
      "outcome": "FAILED",
      "code": "OCMS.CPS.DUPLICATE_RECORD",
      "message": "Existing change record found for this notice today. Set isConfirmation=true to proceed."
    }
  ]
}
```

**Response (Failure):**
```json
{
  "status": "FAILED",
  "summary": {
    "requested": 0,
    "succeeded": 0,
    "failed": 0
  },
  "results": [
    {
      "noticeNo": "",
      "outcome": "FAILED",
      "code": "OCMS.CPS.INVALID_FORMAT",
      "message": "Items list cannot be empty"
    }
  ]
}
```

---

### 1.4 Get Change Processing Stage Reports

**API Name:** getChangeStageReports

**Endpoint:**
- UAT: `https://[domain]/ocms/v1/change-processing-stage/reports?startDate=2025-01-01&endDate=2025-01-31`
- PRD: `https://[domain]/ocms/v1/change-processing-stage/reports?startDate=2025-01-01&endDate=2025-01-31`

**Method:** GET

**Description:** Retrieve list of change stage reports within date range

**Query Parameters:**
- `startDate` (required): Start date in format yyyy-MM-dd
- `endDate` (required): End date in format yyyy-MM-dd

**Request Rules:**
- Date range cannot exceed 90 days
- End date must be after start date

**Response (Success):**
```json
{
  "reports": [
    {
      "reportDate": "2025-01-15",
      "generatedBy": "USER01, USER02",
      "noticeCount": 25,
      "reportUrl": "reports/change-stage/ChangeStageReport_20250115_USER01.xlsx"
    },
    {
      "reportDate": "2025-01-10",
      "generatedBy": "USER03",
      "noticeCount": 10,
      "reportUrl": "reports/change-stage/ChangeStageReport_20250110_USER03.xlsx"
    }
  ],
  "totalReports": 2,
  "totalNotices": 35
}
```

**Response (Failure - Invalid Date Range):**
```json
{
  "error": "INVALID_DATE_RANGE",
  "message": "Date range cannot exceed 90 days. Current range: 120 days"
}
```

---

### 1.5 Download Change Processing Stage Report

**API Name:** downloadChangeStageReport

**Endpoint:**
- UAT: `https://[domain]/ocms/v1/change-processing-stage/report/{reportId}`
- PRD: `https://[domain]/ocms/v1/change-processing-stage/report/{reportId}`

**Method:** GET

**Description:** Download individual change processing stage report Excel file

**Path Parameters:**
- `reportId` (required): Report filename (e.g., "ChangeStageReport_20251220_143022_USER01.xlsx")

**Response (Success):**
- HTTP 200 OK
- Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
- Content-Disposition: attachment; filename="[reportId]"
- Body: Excel file bytes

**Response (Failure - Not Found):**
```json
{
  "error": "REPORT_NOT_FOUND",
  "message": "Report not found: ChangeStageReport_20251220_143022_USER01.xlsx"
}
```

**Response (Failure - Invalid Report ID):**
```json
{
  "error": "INVALID_REPORT_ID",
  "message": "Report ID contains invalid characters"
}
```

---

## 2. External APIs (Integration with External Systems)

### 2.1 PLUS Manual Change Processing Stage

**API Name:** plusChangeProcessingStage

**Endpoint:**
- UAT: `https://[domain]/ocms/v1/external/plus/change-processing-stage`
- PRD: `https://[domain]/ocms/v1/external/plus/change-processing-stage`

**Method:** POST

**Description:** PLUS Staff Portal requests to change notice processing stage

**Request Payload:**
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

**Request Rules:**
- `noticeNo` is array of notice numbers
- Stage transition must be allowed according to stage map
- **PLUS cannot request CFC stage** - will return OCMS-4004 error
- **PLUS cannot request ENA stage** - will return OCMS-4000 error
- If `offenceType='U'` (Unidentified) and `nextStageName` IN (DN1, DN2, DR3, CPC), notice will be skipped

**Response (Success):**
```json
{
  "status": "SUCCESS",
  "message": "Stage change processed successfully",
  "noticeCount": 2
}
```

**Response (Failure - Invalid Transition):**
```json
{
  "status": "FAILED",
  "code": "OCMS-4000",
  "message": "Stage transition not allowed: DN1 -> CFC"
}
```

**Response (Failure - CFC Not Allowed for PLUS):**
```json
{
  "status": "FAILED",
  "code": "OCMS-4004",
  "message": "CFC not allowed from PLUS source"
}
```

**Response (System Error):**
```json
{
  "status": "ERROR",
  "code": "INTERNAL_ERROR",
  "message": "Unexpected system error: [error details]"
}
```

---

## 3. Internal APIs (Cron/System Integration)

### 3.1 Toppan Stage Update (Internal)

**API Name:** updateToppanStages

**Endpoint:**
- UAT: `https://[domain]/ocms/v1/internal/toppan/update-stages`
- PRD: `https://[domain]/ocms/v1/internal/toppan/update-stages`

**Method:** POST

**Description:** Called by generate_toppan_letters cron job to update VON processing stages after Toppan files are generated

**Request Payload:**
```json
{
  "noticeNumbers": ["N-001", "N-002", "N-003"],
  "currentStage": "DN1",
  "processingDate": "2025-12-19T00:30:00"
}
```

**Response (Success):**
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

**Response (Failure):**
```json
{
  "totalNotices": 3,
  "automaticUpdates": 0,
  "manualUpdates": 0,
  "skipped": 0,
  "errors": ["Unexpected error: [error details]"],
  "success": false
}
```

---

## 4. Error Codes

### 4.1 General Error Codes

| Code | Category | Description | User Message |
|------|----------|-------------|--------------|
| OCMS-2000 | Success | Operation successful | Success |
| OCMS-4000 | Client Error | Bad Request / Invalid Data | Invalid request. Please check and try again. |
| OCMS-4004 | Client Error | CFC not allowed from PLUS | CFC not allowed from PLUS source |
| OCMS-5000 | Server Error | Internal Server Error | Something went wrong. Please try again later. |

### 4.2 Eligibility Error Codes

| Code | Category | Description | User Message |
|------|----------|-------------|--------------|
| OCMS.CPS.ELIG.NOT_FOUND | Eligibility | Notice not found in VON or ONOD | Notice not found |
| OCMS.CPS.ELIG.ROLE_CONFLICT | Eligibility | Cannot determine offender role | Cannot determine offender role |
| OCMS.CPS.ELIG.NO_STAGE_RULE | Eligibility | Cannot derive next stage | Cannot derive next stage |
| OCMS.CPS.ELIG.INELIGIBLE_STAGE | Eligibility | Stage not eligible for role | Stage not eligible for offender type |
| OCMS.CPS.ELIG.COURT_STAGE | Eligibility | Notice at court stage (NOT in Allowed Stages) | Notice is in court stage |
| OCMS.CPS.ELIG.PS_BLOCKED | Eligibility | Permanent suspension active | Notice has permanent suspension |
| OCMS.CPS.ELIG.TS_BLOCKED | Eligibility | Temporary suspension active | Notice has temporary suspension |
| OCMS.CPS.ELIG.EXISTING_CHANGE_TODAY | Eligibility | Duplicate change today | Existing change record found for this notice today |

### 4.3 Validation Error Codes

| Code | Category | Description | User Message |
|------|----------|-------------|--------------|
| OCMS.CPS.INVALID_FORMAT | Validation | Invalid request format | Items list cannot be empty |
| OCMS.CPS.MISSING_DATA | Validation | Required field missing | noticeNo is required |
| OCMS.CPS.REMARKS_REQUIRED | Validation | Remarks mandatory for OTH | Remarks are mandatory when reason for change is 'OTH' |
| OCMS.CPS.VALIDATION_ERROR | Validation | General validation error | Validation error |

### 4.4 PLUS API Error Codes

| Code | Category | Description | User Message |
|------|----------|-------------|--------------|
| NOTICE_NOT_FOUND | PLUS | Notice not found in VON | Notice not found |
| STAGE_NOT_ELIGIBLE | PLUS | Stage not eligible | Stage not eligible |
| EXISTING_STAGE_CHANGE | PLUS | Duplicate stage change | Existing stage change |
| PROCESSING_FAILED | PLUS | Multiple notices failed | Processing failed |

---

## 5. Database Tables Used

| Zone | Table Name | Purpose |
|------|------------|---------|
| Intranet | ocms_valid_offence_notice | Main notice records |
| Intranet | ocms_offence_notice_owner_driver | Offender details (for ID search) |
| Intranet | ocms_change_of_processing | Change processing stage records |
| Intranet | ocms_stage_map | Stage transition rules |
| Intranet | ocms_parameter | System parameters (STAGEDAYS, ADM, SURCHARGE) |
| Intranet | ocmsiz_app_conn | Audit trail (Intranet) |
| Internet | ocmsez_app_conn | Audit trail (Internet) |

### 5.1 ocms_change_of_processing Table Structure

**Composite Primary Key:** (notice_no, date_of_change, new_processing_stage)

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| notice_no | varchar(10) | NO (PK) | Offence notice number |
| date_of_change | datetime2(7) | NO (PK) | Date when processing stage was changed |
| new_processing_stage | varchar(3) | NO (PK) | New processing stage after the change |
| last_processing_stage | varchar(3) | YES | Previous processing stage |
| reason_of_change | varchar(3) | YES | Reason code for changing |
| authorised_officer | varchar(50) | YES | Officer who authorized the change |
| source | varchar(8) | YES | Source: OCMS/PLUS/AVSS/SYSTEM |
| remarks | varchar(200) | YES | Additional remarks |
| cre_date | datetime2(7) | NO | Record creation timestamp |
| cre_user_id | varchar(10) | NO | User who created the record |
| upd_date | datetime2(7) | YES | Last update timestamp |
| upd_user_id | varchar(50) | YES | User who last updated |

### 5.2 Key Field Constraints

| Field | Table | Type | Max Length |
|-------|-------|------|------------|
| notice_no | VON/ONOD/COP | varchar | 10 |
| id_no | ONOD | varchar | 12 |
| processing_stage | VON | varchar | 3 |
| remarks | COP | varchar | 200 |
| authorised_officer | COP | varchar | 50 |

---

## 6. Report Specification

### 6.1 Report Columns (16 columns)

| # | Column Name | Source |
|---|-------------|--------|
| 1 | S/N | Sequential number |
| 2 | Notice Number | ocms_valid_offence_notice.notice_no |
| 3 | Offence Type | ocms_valid_offence_notice.offence_notice_type |
| 4 | Offence Date & Time | ocms_valid_offence_notice.notice_date_and_time |
| 5 | Vehicle No | ocms_valid_offence_notice.vehicle_no |
| 6 | Offender ID | ocms_offence_notice_owner_driver.id_no |
| 7 | Offender Name | ocms_offence_notice_owner_driver.name |
| 8 | Previous Processing Stage | ocms_valid_offence_notice.last_processing_stage (before change) |
| 9 | Previous Processing Date | ocms_valid_offence_notice.last_processing_date (before change) |
| 10 | Current Processing Stage | ocms_change_of_processing.new_processing_stage |
| 11 | Current Processing Date | ocms_change_of_processing.date_of_change |
| 12 | Reason for Change | ocms_change_of_processing.reason_of_change |
| 13 | Remarks | ocms_change_of_processing.remarks |
| 14 | Authorised Officer | ocms_change_of_processing.authorised_officer |
| 15 | Submitted Date | ocms_change_of_processing.cre_date |
| 16 | Source | ocms_change_of_processing.source |

### 6.2 Report File Naming

**Format:** `ChangeStageReport_yyyyMMdd_HHmmss_[userID].xlsx`

**Example:** `ChangeStageReport_20260122_143022_JOHNLEE.xlsx`

### 6.3 Storage Location

**Path:** `reports/change-stage/`

**Storage:** Azure Blob Storage

---

## 7. Configuration

### 7.1 Retry Configuration

| Parameter | Value | Description |
|-----------|-------|-------------|
| MAX_GENERATION_RETRIES | 1 | Max retries for Excel generation |
| MAX_UPLOAD_RETRIES | 1 | Max retries for Azure Blob upload |
| RETRY_DELAY_MS | 1000 | Delay between retries (milliseconds) |

### 7.2 Toppan Cron Schedule

| Parameter | Value | Description |
|-----------|-------|-------------|
| Cron Expression | `0 30 0 * * ?` | Daily at 00:30 |
| ShedLock Name | `generate_toppan_letters` | Distributed lock name |
| Min Lock Duration | PT5M | 5 minutes |
| Max Lock Duration | PT3H | 3 hours |
| Config Property | `cron.toppan.upload.enabled` | Enable/disable flag |

---

## 8. Notes

1. **Search Optimization:**
   - When searching by ID Number, system queries ONOD table first, then joins with VON
   - All other searches query VON table directly

2. **Eligibility Check:**
   - Search API returns pre-segregated lists (eligible vs ineligible)
   - Validate API performs deeper validation before submission
   - Court stage notices (any stage NOT IN Allowed Stages: NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3, CPC, CFC) are ineligible
   - Note: CPC and CFC are court stages but still ALLOWED for change
   - **ENA stage is not allowed as target stage** (for both OCMS and PLUS)

3. **Duplicate Handling:**
   - System checks for existing change record on same date using composite key
   - First attempt returns warning (OCMS.CPS.ELIG.EXISTING_CHANGE_TODAY)
   - Second attempt with `isConfirmation=true` proceeds with update

4. **Report Generation:**
   - Reports generated automatically upon successful change
   - Stored in Azure Blob Storage with retry logic
   - Accessible via download API or signed URL

5. **PLUS Integration:**
   - PLUS uses separate endpoint with different payload structure
   - Source code "005" identifies PLUS origin
   - **PLUS cannot request CFC stage** (returns OCMS-4004)
   - **PLUS cannot request ENA stage** (returns OCMS-4000)
   - Validates stage transition rules before processing

6. **Toppan Integration:**
   - Internal endpoint for cron job only (runs daily at 00:30)
   - Differentiates between manual vs automatic stage changes
   - Manual changes (source=OCMS/PLUS) skip amount_payable recalculation
   - Automatic changes recalculate amount_payable

7. **Source Constants:**
   - OCMS = "OCMS" (Staff Portal)
   - PLUS = "PLUS" (PLUS Portal)
   - AVSS = "AVSS" (AVSS System)
   - SYSTEM = "SYSTEM" (Cron/Batch)

---

**End of API Plan**

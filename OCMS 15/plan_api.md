# API Plan - OCMS 15: Manage Change Processing Stage

**Document Information**
- Version: 1.0
- Date: 2026-01-21
- Source: Backend Code Analysis + Functional Document v1.2
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
      "vehicleNo": "SBA1234A",
      "currentProcessingStage": "DN1",
      "currentProcessingStageDate": "2025-12-15T09:00:00",
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
      "offenceDateTime": "2025-12-01T11:00:00",
      "offenderName": "Jane Smith",
      "offenderId": "S9876543B",
      "vehicleNo": "SBA5678B",
      "currentProcessingStage": "CRT",
      "currentProcessingStageDate": "2025-12-18T14:00:00",
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

**Response (Success):**
```json
{
  "status": "SUCCESS",
  "message": "Stage change processed successfully",
  "noticeCount": 2
}
```

**Response (Failure):**
```json
{
  "status": "FAILED",
  "code": "OCMS-4000",
  "message": "Stage transition not allowed: DN1 -> CFC"
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

| Code | Category | Description | User Message |
|------|----------|-------------|--------------|
| OCMS-2000 | Success | Operation successful | Success |
| OCMS-4000 | Client Error | Bad Request / Invalid Data | Invalid request. Please check and try again. |
| OCMS-5000 | Server Error | Internal Server Error | Something went wrong. Please try again later. |
| OCMS.CPS.INVALID_FORMAT | Validation | Invalid request format | Items list cannot be empty |
| OCMS.CPS.MISSING_DATA | Validation | Required field missing | noticeNo is required |
| OCMS.CPS.REMARKS_REQUIRED | Validation | Remarks mandatory for OTH reason | Remarks are mandatory when reason for change is 'OTH' (Others) |
| OCMS.CPS.NOT_FOUND | Business Logic | Notice not found in database | VON not found |
| OCMS.CPS.COURT_STAGE | Business Logic | Notice is in court stage | Notice is in court stage and cannot be changed |
| OCMS.CPS.DUPLICATE_RECORD | Business Logic | Duplicate change record exists | Existing change record found for this notice today |
| OCMS.CPS.SEARCH.COURT_STAGE | Search | Notice ineligible due to court stage | Notice is in court stage |
| OCMS.CPS.UNEXPECTED | System Error | Unexpected error occurred | Unexpected error: [details] |

---

## 5. Database Tables Used

| Zone | Table Name | Purpose |
|------|------------|---------|
| Intranet | ocms_valid_offence_notice | Main notice records |
| Intranet | ocms_offence_notice_owner_driver | Offender details (for ID search) |
| Intranet | ocms_change_of_processing | Change processing stage records |
| Intranet | ocms_stage_map | Stage transition rules |
| Intranet | ocmsiz_app_conn | Audit trail (Intranet) |
| Internet | ocmsez_app_conn | Audit trail (Internet) |

---

## 6. Notes

1. **Search Optimization:**
   - When searching by ID Number, system queries ONOD table first, then joins with VON
   - All other searches query VON table directly

2. **Eligibility Check:**
   - Search API returns pre-segregated lists (eligible vs ineligible)
   - Validate API performs deeper validation before submission
   - Court stage notices are always ineligible

3. **Duplicate Handling:**
   - System checks for existing change record on same date
   - First attempt returns warning
   - Second attempt with `isConfirmation=true` proceeds with update

4. **Report Generation:**
   - Reports generated automatically upon successful change
   - Stored in Azure Blob Storage
   - Accessible via download API or signed URL

5. **PLUS Integration:**
   - PLUS uses separate endpoint with different payload structure
   - Source code "005" identifies PLUS origin
   - Validates stage transition rules before processing

6. **Toppan Integration:**
   - Internal endpoint for cron job only
   - Differentiates between manual vs automatic stage changes
   - Updates VON records after Toppan file generation

---

**End of API Plan**

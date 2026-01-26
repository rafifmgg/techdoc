# OCMS 15a - Change Processing Stage API Plan

## Document Information

| Field | Value |
|-------|-------|
| Version | 1.0 |
| Date | 2025-01-26 |
| Feature | OCMS 15a - Manual Change Processing Stage |
| Source | Functional Document v1.2, Backend Code (ocmsadminapi) |
| Status | Draft |

---

## 1. API Overview

### 1.1 Base URL
```
Production: https://parking.uraz.gov.sg/ocms/v1
UAT: https://parking2.uraz.gov.sg/ocms/v1
```

### 1.2 Authentication
- All APIs require valid JWT token in Authorization header
- Internal APIs require X-User-Id header for audit trail
- External APIs (PLUS) require API key via APIM

### 1.3 Common Headers
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
X-User-Id: {user_id}
X-Request-Id: {uuid}
```

---

## 2. Internal APIs (OCMS Staff Portal)

### 2.1 Search Notices for Change Processing Stage

**Endpoint:** `POST /v1/change-processing-stage/search`

**Description:** Search notices based on criteria and return segregated lists of eligible vs ineligible notices for stage change.

**Reference:** FD Section 2.3.1

#### Request

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| noticeNo | String | Optional | Notice number (10 chars, alphanumeric) |
| idNo | String | Optional | ID number (NRIC/FIN, max 20 chars) |
| vehicleNo | String | Optional | Vehicle registration number |
| lastProcessingStage | String | Optional | Current processing stage code |
| dateOfCurrentProcessingStage | Date | Optional | Date of current processing stage (YYYY-MM-DD) |

> **Note:** At least one search criterion is required.

#### Request Examples

**Example 1: Search by Notice Number**
```json
{
  "noticeNo": "441000001X"
}
```

**Example 2: Search by ID Number**
```json
{
  "idNo": "S1234567A"
}
```

**Example 3: Search by Vehicle Number**
```json
{
  "vehicleNo": "SBA1234A"
}
```

**Example 4: Search by Processing Stage**
```json
{
  "lastProcessingStage": "DN1"
}
```

**Example 5: Search by Stage and Date**
```json
{
  "lastProcessingStage": "RR3",
  "dateOfCurrentProcessingStage": "2025-12-20"
}
```

**Example 6: Multiple Criteria**
```json
{
  "vehicleNo": "SBA1234A",
  "lastProcessingStage": "ROV"
}
```

#### Response

| Field | Type | Description |
|-------|------|-------------|
| eligibleNotices | Array | List of notices eligible for stage change |
| ineligibleNotices | Array | List of notices not eligible for stage change |
| summary | Object | Summary counts |

**Eligible Notice Object:**

| Field | Type | Description |
|-------|------|-------------|
| noticeNo | String | Notice number |
| offenceType | String | Type of offence |
| offenceDateTime | DateTime | Date and time of offence |
| offenderName | String | Name of offender |
| offenderId | String | ID of offender (NRIC/FIN) |
| vehicleNo | String | Vehicle registration number |
| currentProcessingStage | String | Current processing stage |
| currentProcessingStageDate | DateTime | Date of current stage |
| suspensionType | String | Type of suspension (null if none) |
| suspensionStatus | String | Suspension status |
| ownerDriverIndicator | String | O=Owner, D=Driver, H=Hirer |
| entityType | String | Entity type (INDIVIDUAL/COMPANY) |

**Ineligible Notice Object:**

| Field | Type | Description |
|-------|------|-------------|
| noticeNo | String | Notice number |
| offenceType | String | Type of offence |
| offenceDateTime | DateTime | Date and time of offence |
| offenderName | String | Name of offender |
| offenderId | String | ID of offender |
| vehicleNo | String | Vehicle registration number |
| currentProcessingStage | String | Current processing stage |
| currentProcessingStageDate | DateTime | Date of current stage |
| reasonCode | String | Error code for ineligibility |
| reasonMessage | String | Human-readable reason |

#### Response Examples

**Success Response (HTTP 200):**
```json
{
  "eligibleNotices": [
    {
      "noticeNo": "441000001X",
      "offenceType": "O",
      "offenceDateTime": "2025-12-01T10:30:00",
      "offenderName": "TAN AH KOW",
      "offenderId": "S1234567A",
      "vehicleNo": "SBA1234A",
      "currentProcessingStage": "DN1",
      "currentProcessingStageDate": "2025-12-15T09:00:00",
      "suspensionType": null,
      "suspensionStatus": null,
      "ownerDriverIndicator": "D",
      "entityType": "INDIVIDUAL"
    },
    {
      "noticeNo": "441000004D",
      "offenceType": "O",
      "offenceDateTime": "2025-12-02T14:00:00",
      "offenderName": "LIM BEE HONG",
      "offenderId": "S8888888H",
      "vehicleNo": "SBA1234A",
      "currentProcessingStage": "RD1",
      "currentProcessingStageDate": "2025-12-18T10:00:00",
      "suspensionType": null,
      "suspensionStatus": null,
      "ownerDriverIndicator": "O",
      "entityType": "INDIVIDUAL"
    }
  ],
  "ineligibleNotices": [
    {
      "noticeNo": "441000002Y",
      "offenceType": "O",
      "offenceDateTime": "2025-12-01T11:00:00",
      "offenderName": "LIM MEI LING",
      "offenderId": "S9876543B",
      "vehicleNo": "SBA5678B",
      "currentProcessingStage": "CRT",
      "currentProcessingStageDate": "2025-12-18T14:00:00",
      "reasonCode": "OCMS-4000",
      "reasonMessage": "Notice is in court stage"
    },
    {
      "noticeNo": "441000003Z",
      "offenceType": "O",
      "offenceDateTime": "2025-12-02T08:00:00",
      "offenderName": "WONG KAI MING",
      "offenderId": "S5555555C",
      "vehicleNo": "SBA9999C",
      "currentProcessingStage": "RD2",
      "currentProcessingStageDate": "2025-12-19T10:00:00",
      "reasonCode": "OCMS-4000",
      "reasonMessage": "Permanent Suspension is active"
    }
  ],
  "summary": {
    "total": 4,
    "eligible": 2,
    "ineligible": 2
  }
}
```

**No Records Found (HTTP 200):**
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

**Bad Request - No Criteria (HTTP 400):**
```json
{
  "data": {
    "appCode": "OCMS-4000",
    "message": "At least one search criterion is required."
  }
}
```

---

### 2.2 Validate Notices for Stage Change

**Endpoint:** `POST /v1/change-processing-stage/validate`

**Description:** Validate notices BEFORE submission to identify which are eligible vs ineligible for the requested stage change.

**Reference:** FD Section 2.4.2

#### Request

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| notices | Array | Yes | List of notices to validate |
| newProcessingStage | String | Yes | Target processing stage |
| reasonOfChange | String | Yes | Reason code (REC/RSN/SUP/OTH/WOD) |
| remarks | String | Conditional | Required if reasonOfChange = "OTH" |

**Notice Object:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| noticeNo | String | Yes | Notice number |
| currentStage | String | Optional | Current processing stage |
| offenderType | String | Optional | DRIVER/OWNER/HIRER/DIRECTOR |
| entityType | String | Optional | INDIVIDUAL/COMPANY |

#### Request Examples

**Example 1: Validate Driver Notices for DN2**
```json
{
  "notices": [
    {
      "noticeNo": "441000001X",
      "currentStage": "DN1",
      "offenderType": "DRIVER",
      "entityType": null
    },
    {
      "noticeNo": "441000002Y",
      "currentStage": "DN1",
      "offenderType": "DRIVER",
      "entityType": null
    }
  ],
  "newProcessingStage": "DN2",
  "reasonOfChange": "SUP",
  "remarks": "Speed up processing for urgent case"
}
```

**Example 2: Validate Owner Notices for RR3**
```json
{
  "notices": [
    {
      "noticeNo": "441000010A",
      "currentStage": "RD2",
      "offenderType": "OWNER",
      "entityType": "INDIVIDUAL"
    }
  ],
  "newProcessingStage": "RR3",
  "reasonOfChange": "REC",
  "remarks": "Recheck with Vault completed"
}
```

**Example 3: Validate with Reason = OTH (Remarks Mandatory)**
```json
{
  "notices": [
    {
      "noticeNo": "441000001X",
      "currentStage": "DN1",
      "offenderType": "DRIVER",
      "entityType": null
    }
  ],
  "newProcessingStage": "DN2",
  "reasonOfChange": "OTH",
  "remarks": "Special request from supervisor - Ref: SR-2025-001234"
}
```

#### Response

| Field | Type | Description |
|-------|------|-------------|
| changeableNotices | Array | Notices eligible for stage change |
| nonChangeableNotices | Array | Notices not eligible for stage change |
| summary | Object | Validation summary |

#### Response Examples

**Success Response (HTTP 200):**
```json
{
  "changeableNotices": [
    {
      "noticeNo": "441000001X",
      "currentStage": "DN1",
      "offenderType": "DRIVER",
      "entityType": null,
      "message": "Eligible for stage change to DN2"
    }
  ],
  "nonChangeableNotices": [
    {
      "noticeNo": "441000002Y",
      "currentStage": "CRT",
      "code": "OCMS-4000",
      "message": "Notice is at court stage"
    }
  ],
  "summary": {
    "total": 2,
    "changeable": 1,
    "nonChangeable": 1
  }
}
```

**Remarks Required Error (HTTP 200):**
```json
{
  "changeableNotices": [],
  "nonChangeableNotices": [
    {
      "noticeNo": "441000001X",
      "code": "OCMS-4000",
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

### 2.3 Submit Change Processing Stage

**Endpoint:** `POST /v1/change-processing-stage`

**Description:** Submit batch change processing stage request to update notice stages.

**Reference:** FD Section 2.5

#### Request

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| items | Array | Yes | List of notices to update |

**Item Object:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| noticeNo | String | Yes | Notice number |
| newStage | String | Optional | Target processing stage (can be derived) |
| reason | String | Yes | Reason code (REC/RSN/SUP/OTH/WOD) |
| remark | String | Conditional | Remarks (required if reason = OTH) |
| dhMhaCheck | Boolean | Optional | Send for DH/MHA check (default: false) |
| isConfirmation | Boolean | Optional | Confirm override existing record (default: false) |

#### Request Examples

**Example 1: Single Notice - Driver**
```json
{
  "items": [
    {
      "noticeNo": "441000001X",
      "newStage": "DN2",
      "reason": "SUP",
      "remark": "Speed up processing",
      "dhMhaCheck": true,
      "isConfirmation": false
    }
  ]
}
```

**Example 2: Batch Notices - Mixed**
```json
{
  "items": [
    {
      "noticeNo": "441000010A",
      "newStage": "RD2",
      "reason": "RSN",
      "remark": "Resend notice to updated address",
      "dhMhaCheck": true,
      "isConfirmation": false
    },
    {
      "noticeNo": "441000011B",
      "newStage": "RR3",
      "reason": "REC",
      "remark": "Recheck with Vault completed",
      "dhMhaCheck": false,
      "isConfirmation": false
    },
    {
      "noticeNo": "441000012C",
      "newStage": "ROV",
      "reason": "WOD",
      "remark": "Sent to wrong owner, resending to correct owner",
      "dhMhaCheck": true,
      "isConfirmation": false
    }
  ]
}
```

**Example 3: Confirmation for Existing Record**
```json
{
  "items": [
    {
      "noticeNo": "441000001X",
      "newStage": "DN2",
      "reason": "SUP",
      "remark": "Speed up processing - confirmed override",
      "dhMhaCheck": true,
      "isConfirmation": true
    }
  ]
}
```

**Example 4: Skip DH/MHA Check**
```json
{
  "items": [
    {
      "noticeNo": "441000001X",
      "newStage": "DR3",
      "reason": "OTH",
      "remark": "Special case - no DH/MHA check required per approval REF-2025-999",
      "dhMhaCheck": false,
      "isConfirmation": false
    }
  ]
}
```

#### Response

| Field | Type | Description |
|-------|------|-------------|
| status | String | Overall status (SUCCESS/FAILED/PARTIAL/WARNING) |
| summary | Object | Batch summary |
| results | Array | Per-notice results |
| reportUrl | String | Download URL for generated report |
| existingRecordWarning | Boolean | Flag indicating existing record warning |

**Result Object:**

| Field | Type | Description |
|-------|------|-------------|
| noticeNo | String | Notice number |
| outcome | String | Result (UPDATED/FAILED/WARNING) |
| previousStage | String | Previous stage (if updated) |
| newStage | String | New stage (if updated) |
| code | String | Error code (if failed) |
| message | String | Result message |

#### Response Examples

**Full Success (HTTP 200):**
```json
{
  "status": "SUCCESS",
  "summary": {
    "requested": 2,
    "succeeded": 2,
    "failed": 0
  },
  "results": [
    {
      "noticeNo": "441000001X",
      "outcome": "UPDATED",
      "previousStage": "DN1",
      "newStage": "DN2",
      "message": "Stage changed successfully"
    },
    {
      "noticeNo": "441000002Y",
      "outcome": "UPDATED",
      "previousStage": "DN1",
      "newStage": "DN2",
      "message": "Stage changed successfully"
    }
  ],
  "reportUrl": "https://storage.blob.core.windows.net/reports/change-stage/ChangeStageReport_20251220_143022_OIC001.xlsx",
  "existingRecordWarning": false
}
```

**Partial Success (HTTP 200):**
```json
{
  "status": "PARTIAL",
  "summary": {
    "requested": 3,
    "succeeded": 2,
    "failed": 1
  },
  "results": [
    {
      "noticeNo": "441000010A",
      "outcome": "UPDATED",
      "previousStage": "RD1",
      "newStage": "RD2",
      "message": "Stage changed successfully"
    },
    {
      "noticeNo": "441000011B",
      "outcome": "UPDATED",
      "previousStage": "RD2",
      "newStage": "RR3",
      "message": "Stage changed successfully"
    },
    {
      "noticeNo": "441000012C",
      "outcome": "FAILED",
      "code": "OCMS-4000",
      "message": "Notice is at court stage"
    }
  ],
  "reportUrl": "https://storage.blob.core.windows.net/reports/change-stage/ChangeStageReport_20251220_143022_OIC001.xlsx",
  "existingRecordWarning": false
}
```

**Warning - Existing Record (HTTP 200):**
```json
{
  "status": "WARNING",
  "summary": {
    "requested": 1,
    "succeeded": 0,
    "failed": 1
  },
  "results": [
    {
      "noticeNo": "441000001X",
      "outcome": "WARNING",
      "code": "OCMS-4000",
      "message": "Notice No. 441000001X has existing stage change update. Please confirm to proceed."
    }
  ],
  "reportUrl": null,
  "existingRecordWarning": true
}
```

**Full Failure (HTTP 200):**
```json
{
  "status": "FAILED",
  "summary": {
    "requested": 2,
    "succeeded": 0,
    "failed": 2
  },
  "results": [
    {
      "noticeNo": "441000001X",
      "outcome": "FAILED",
      "code": "OCMS-4004",
      "message": "VON not found"
    },
    {
      "noticeNo": "441000002Y",
      "outcome": "FAILED",
      "code": "OCMS-4000",
      "message": "Notice is at court stage"
    }
  ],
  "reportUrl": null,
  "existingRecordWarning": false
}
```

**Validation Error (HTTP 400):**
```json
{
  "data": {
    "appCode": "OCMS-4000",
    "message": "Items list cannot be empty."
  }
}
```

**Missing Data Error (HTTP 422):**
```json
{
  "data": {
    "appCode": "OCMS-4000",
    "message": "noticeNo is required."
  }
}
```

---

### 2.4 Retrieve Change Stage Reports

**Endpoint:** `POST /v1/change-processing-stage/reports`

**Description:** Retrieve list of change processing stage reports by date range.

**Reference:** FD Section 2.8

#### Request

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| startDate | Date | Yes | Start date (YYYY-MM-DD) |
| endDate | Date | Yes | End date (YYYY-MM-DD) |

> **Note:** Maximum date range is 90 days.

#### Request Examples

**Example 1: Get Reports for December 2025**
```json
{
  "startDate": "2025-12-01",
  "endDate": "2025-12-31"
}
```

**Example 2: Get Reports for Last 7 Days**
```json
{
  "startDate": "2025-12-19",
  "endDate": "2025-12-26"
}
```

#### Response

| Field | Type | Description |
|-------|------|-------------|
| reports | Array | List of report info |
| totalReports | Integer | Total number of reports |
| totalNotices | Integer | Total notices across all reports |

**Report Object:**

| Field | Type | Description |
|-------|------|-------------|
| reportDate | Date | Date of the report |
| generatedBy | String | User(s) who generated changes |
| noticeCount | Integer | Number of notices in report |
| reportUrl | String | Path to report file |

#### Response Examples

**Success Response (HTTP 200):**
```json
{
  "reports": [
    {
      "reportDate": "2025-12-20",
      "generatedBy": "OIC001",
      "noticeCount": 15,
      "reportUrl": "reports/change-stage/ChangeStageReport_20251220_OIC001.xlsx"
    },
    {
      "reportDate": "2025-12-19",
      "generatedBy": "OIC002, OIC003",
      "noticeCount": 8,
      "reportUrl": "reports/change-stage/ChangeStageReport_20251219_OIC002.xlsx"
    },
    {
      "reportDate": "2025-12-18",
      "generatedBy": "PLUS_SYSTEM",
      "noticeCount": 25,
      "reportUrl": "reports/change-stage/ChangeStageReport_20251218_PLUS_SYSTEM.xlsx"
    }
  ],
  "totalReports": 3,
  "totalNotices": 48
}
```

**Invalid Date Range (HTTP 400):**
```json
{
  "data": {
    "appCode": "OCMS-4000",
    "message": "Date range cannot exceed 90 days. Current range: 120 days."
  }
}
```

**End Date Before Start Date (HTTP 400):**
```json
{
  "data": {
    "appCode": "OCMS-4000",
    "message": "End date must be after start date."
  }
}
```

---

### 2.5 Download Individual Report

**Endpoint:** `POST /v1/change-processing-stage/report/download`

**Description:** Download individual change processing stage report file.

**Reference:** FD Section 2.8

#### Request

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| reportId | String | Yes | Report filename/ID |

#### Request Examples

**Example 1: Download Report**
```json
{
  "reportId": "ChangeStageReport_20251220_143022_OIC001.xlsx"
}
```

#### Response

**Success Response (HTTP 200):**
- Content-Type: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- Content-Disposition: `attachment; filename="ChangeStageReport_20251220_143022_OIC001.xlsx"`
- Body: Binary Excel file content

**Report Not Found (HTTP 404):**
```json
{
  "data": {
    "appCode": "OCMS-4004",
    "message": "Report not found: ChangeStageReport_20251220_143022_OIC001.xlsx"
  }
}
```

**Invalid Report ID (HTTP 400):**
```json
{
  "data": {
    "appCode": "OCMS-4000",
    "message": "Report ID cannot be empty."
  }
}
```

**Invalid Characters (HTTP 400):**
```json
{
  "data": {
    "appCode": "OCMS-4000",
    "message": "Report ID contains invalid characters."
  }
}
```

---

## 3. External APIs (PLUS Integration)

### 3.1 PLUS Change Processing Stage

**Endpoint:** `POST /v1/external/plus/change-processing-stage`

**Description:** External API for PLUS system to trigger manual stage change.

**Reference:** FD Section 3

#### Request

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| noticeNo | Array[String] | Yes | List of notice numbers |
| lastStageName | String | Yes | Current processing stage |
| nextStageName | String | Yes | Target processing stage |
| lastStageDate | DateTime | Optional | Date of current stage (ISO 8601) |
| newStageDate | DateTime | Optional | Date for new stage (ISO 8601) |
| offenceType | String | Yes | O=Owner, D=Driver, H=Hirer, DIR=Director |
| source | String | Yes | Source code (005 for PLUS) |

#### Request Examples

**Example 1: Single Notice - Driver**
```json
{
  "noticeNo": ["441000001X"],
  "lastStageName": "DN1",
  "nextStageName": "DN2",
  "lastStageDate": "2025-09-25T06:58:42",
  "newStageDate": "2025-09-30T06:58:42",
  "offenceType": "D",
  "source": "005"
}
```

**Example 2: Batch Notices - Owner**
```json
{
  "noticeNo": ["441000010A", "441000011B", "441000012C"],
  "lastStageName": "RD1",
  "nextStageName": "RD2",
  "lastStageDate": "2025-10-01T08:00:00",
  "newStageDate": "2025-10-05T08:00:00",
  "offenceType": "O",
  "source": "005"
}
```

**Example 3: Hirer to RR3**
```json
{
  "noticeNo": ["441000020X"],
  "lastStageName": "RD2",
  "nextStageName": "RR3",
  "lastStageDate": "2025-10-10T10:00:00",
  "newStageDate": "2025-10-15T10:00:00",
  "offenceType": "H",
  "source": "005"
}
```

**Example 4: Director**
```json
{
  "noticeNo": ["441000030X"],
  "lastStageName": "RD1",
  "nextStageName": "RD2",
  "lastStageDate": "2025-10-15T09:00:00",
  "newStageDate": "2025-10-20T09:00:00",
  "offenceType": "DIR",
  "source": "005"
}
```

#### Response

**Success Response (HTTP 200):**
```json
{
  "status": "SUCCESS",
  "message": "Stage change processed successfully",
  "noticeCount": 3
}
```

**Stage Transition Not Allowed (HTTP 422):**
```json
{
  "data": {
    "appCode": "OCMS-4000",
    "message": "Stage transition not allowed: DN1 -> RR3. No matching record found in ocms_stage_map table."
  }
}
```

**CFC Not Allowed from PLUS (HTTP 422):**
```json
{
  "data": {
    "appCode": "OCMS-4000",
    "message": "Stage change to CFC is not allowed from PLUS source (source=005). This operation is restricted."
  }
}
```

**Notice Not Found (HTTP 422):**
```json
{
  "data": {
    "appCode": "OCMS-4004",
    "message": "Notice number '441000001X' not found in ocms_valid_offence_notice table."
  }
}
```

**VON Update Failed (HTTP 500):**
```json
{
  "data": {
    "appCode": "OCMS-5001",
    "message": "Error updating VON for notice '441000001X': Database connection timeout."
  }
}
```

**Partial Failure (HTTP 422):**
```json
{
  "data": {
    "appCode": "OCMS-4000",
    "message": "Notice 441000001X failed: Notice not found; Notice 441000002Y failed: Stage not eligible."
  }
}
```

**Internal Error (HTTP 500):**
```json
{
  "data": {
    "appCode": "OCMS-5000",
    "message": "Unexpected system error: Connection refused."
  }
}
```

---

## 4. Internal APIs (Toppan Cron)

### 4.1 Toppan Stage Update

**Endpoint:** `POST /v1/internal/toppan/update-stages`

**Description:** Internal API for Toppan cron job to update VON processing stages after letter generation.

**Reference:** FD Section 2.5.2

#### Request

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| noticeNumbers | Array[String] | Yes | List of notice numbers |
| currentStage | String | Yes | Current processing stage |
| processingDate | DateTime | Yes | Processing date (ISO 8601) |

#### Request Example

```json
{
  "noticeNumbers": ["441000001X", "441000002Y", "441000003Z"],
  "currentStage": "DN2",
  "processingDate": "2025-12-20T00:30:00"
}
```

#### Response

| Field | Type | Description |
|-------|------|-------------|
| totalNotices | Integer | Total notices in request |
| automaticUpdates | Integer | Count of automatic stage updates |
| manualUpdates | Integer | Count of manual stage updates |
| skipped | Integer | Count of skipped notices |
| errors | Array[String] | List of error messages (null if none) |
| success | Boolean | Overall success flag |

#### Response Examples

**Full Success (HTTP 200):**
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

**Partial Success (HTTP 200):**
```json
{
  "totalNotices": 3,
  "automaticUpdates": 1,
  "manualUpdates": 1,
  "skipped": 1,
  "errors": [
    "441000003Z: Stage mismatch (expected DN2, actual RD1)"
  ],
  "success": false
}
```

**Multiple Errors (HTTP 200):**
```json
{
  "totalNotices": 5,
  "automaticUpdates": 2,
  "manualUpdates": 0,
  "skipped": 3,
  "errors": [
    "441000003Z: VON not found",
    "441000004A: Stage mismatch (expected DN2, actual RR3)",
    "441000005B: Database error: Connection timeout"
  ],
  "success": false
}
```

**Internal Error (HTTP 500):**
```json
{
  "data": {
    "appCode": "OCMS-5000",
    "message": "Unexpected error: Database connection failed."
  }
}
```

---

## 5. Error Codes Reference

### 5.1 Standard Error Codes

| Code | HTTP Status | Description | Resolution |
|------|-------------|-------------|------------|
| `OCMS-2000` | 200 | Success | N/A |
| `OCMS-4000` | 400 | Bad request / Validation error | Check request format and data |
| `OCMS-4001` | 401 | Unauthorized | Check authentication |
| `OCMS-4003` | 403 | Forbidden | Check permissions |
| `OCMS-4004` | 404 | Not found | Check resource exists |
| `OCMS-5000` | 500 | Internal server error | Contact support |
| `OCMS-5001` | 500 | Database connection failed | Retry or contact support |
| `OCMS-5002` | 500 | Request timeout | Retry |

### 5.2 Error Messages by Scenario

#### Search API Errors

| Scenario | appCode | Message |
|----------|---------|---------|
| No search criteria | OCMS-4000 | At least one search criterion is required. |
| Notice in court stage | OCMS-4000 | Notice is in court stage. |
| Permanent Suspension active | OCMS-4000 | Permanent Suspension is active. |
| Search processing error | OCMS-5000 | Search processing error. Please try again later. |

#### Validation API Errors

| Scenario | appCode | Message |
|----------|---------|---------|
| Empty notices list | OCMS-4000 | Notices list cannot be empty. |
| Remarks required for OTH | OCMS-4000 | Remarks are mandatory when reason for change is 'OTH' (Others). |
| Notice not found | OCMS-4004 | Notice not found in VON or ONOD. |
| Cannot determine role | OCMS-4000 | Cannot determine offender role. |
| Stage not eligible | OCMS-4000 | Stage {stage} not eligible for role {role}. |
| Notice at court stage | OCMS-4000 | Notice is at court stage. |

#### Submit API Errors

| Scenario | appCode | Message |
|----------|---------|---------|
| Empty items list | OCMS-4000 | Items list cannot be empty. |
| Missing noticeNo | OCMS-4000 | noticeNo is required. |
| Notice not found | OCMS-4004 | VON not found. |
| Existing change today | OCMS-4000 | Notice No. {noticeNo} has existing stage change update. Please confirm to proceed. |
| Stage not eligible | OCMS-4000 | Stage {stage} not eligible for role {role}. |
| Court stage blocked | OCMS-4000 | Notice is at court stage. |

#### Report API Errors

| Scenario | appCode | Message |
|----------|---------|---------|
| Date range exceeds 90 days | OCMS-4000 | Date range cannot exceed 90 days. |
| End date before start date | OCMS-4000 | End date must be after start date. |
| Report not found | OCMS-4004 | Report not found: {reportId} |
| Invalid report ID | OCMS-4000 | Report ID cannot be empty. |
| Download failed | OCMS-5000 | Failed to download report. |

#### PLUS API Errors

| Scenario | appCode | Message |
|----------|---------|---------|
| Stage transition not allowed | OCMS-4000 | Stage transition not allowed: {lastStage} -> {nextStage}. |
| CFC not allowed from PLUS | OCMS-4000 | Stage change to CFC is not allowed from PLUS source. |
| Notice not found | OCMS-4004 | Notice number '{noticeNo}' not found. |
| VON update failed | OCMS-5001 | Error updating VON for notice '{noticeNo}'. |
| Processing failed | OCMS-4000 | Processing failed: {details}. |
| Internal error | OCMS-5000 | Unexpected system error. |

#### Toppan API Errors

| Scenario | appCode | Message |
|----------|---------|---------|
| VON not found | OCMS-4004 | VON not found for notice {noticeNo}. |
| Stage mismatch | OCMS-4000 | Stage mismatch (expected {expected}, actual {actual}). |
| Database error | OCMS-5001 | Database connection failed. |
| Internal error | OCMS-5000 | Unexpected error. |

---

## 6. Reference Data

### 6.1 Reason of Change Codes

| Code | Description | Remarks Required |
|------|-------------|------------------|
| `REC` | Recheck with Vault | No |
| `RSN` | Resend Notice | No |
| `SUP` | Speed up processing | No |
| `OTH` | Others | **Yes** |
| `WOD` | Sent to wrong owner/hirer/driver | No |
| `PLS` | PLUS system (auto-generated) | No |

### 6.2 Offender Type Codes

| Code | Full Name | Eligible Stages |
|------|-----------|-----------------|
| `D` | Driver | DN1, DN2, DR3 |
| `O` | Owner | ROV, RD1, RD2, RR3 |
| `H` | Hirer | ROV, RD1, RD2, RR3 |
| `DIR` | Director | ROV, RD1, RD2, RR3 |

### 6.3 Offence Type Codes

| Code | Description |
|------|-------------|
| `O` | Offence type for Owner notice |
| `E` | Offence type for Electronic notice |
| `U` | Offence type for Unconfirmed/Unknown |

### 6.4 Processing Stage Codes

| Code | Description | Offender Type |
|------|-------------|---------------|
| `NPA` | Notice of Parking Contravention | All |
| `ENA` | E-Notification | All |
| `ROV` | Registered Owner/Vehicle | Owner/Hirer/Director |
| `RD1` | Reminder 1 (Owner) | Owner/Hirer/Director |
| `RD2` | Reminder 2 (Owner) | Owner/Hirer/Director |
| `RR3` | Final Reminder (Owner) | Owner/Hirer/Director |
| `DN1` | Driver Notice 1 | Driver |
| `DN2` | Driver Notice 2 | Driver |
| `DR3` | Driver Reminder 3 | Driver |
| `CFC` | Court - First Call | All (not changeable) |
| `CPC` | Court - Post Call | All (not changeable) |
| `CRT` | Court | All (not changeable) |
| `CRC` | Court - Resolved/Closed | All (not changeable) |
| `CFI` | Court - Final | All (not changeable) |

### 6.5 Source Codes

| Code | Description |
|------|-------------|
| `OCMS` | OCMS Staff Portal |
| `PLUS` | PLUS Integration |
| `AVSS` | AVSS System |
| `SYSTEM` | Automatic/Cron Job |
| `005` | PLUS (External Code) |

---

## 7. Database Tables

### 7.1 Tables Used

| Table | Description | Operations |
|-------|-------------|------------|
| `ocms_valid_offence_notice` (VON) | Main notice table | SELECT, UPDATE |
| `ocms_offence_notice_owner_driver` (ONOD) | Offender details | SELECT, UPDATE |
| `ocms_change_of_processing_stage` (CPS) | Audit trail for changes | SELECT, INSERT |
| `ocms_parameter` | System parameters | SELECT |
| `ocms_stage_map` | Stage transition rules | SELECT |

### 7.2 VON Fields Updated

| Field | Description |
|-------|-------------|
| prev_processing_stage | Previous processing stage (from old last) |
| prev_processing_date | Previous processing date (from old last) |
| last_processing_stage | Current processing stage (new value) |
| last_processing_date | Current processing date (now) |
| next_processing_stage | Next scheduled stage |
| next_processing_date | Next scheduled date (now + STAGEDAYS) |
| amount_payable | Updated amount payable |

### 7.3 Parameters Used

| Parameter ID | Code | Description |
|--------------|------|-------------|
| ADMIN_FEE | AMOUNT | Administration fee amount |
| SURCHARGE | AMOUNT | Court surcharge amount |
| STAGEDAYS | {stage} | Days until next stage |
| NEXT_STAGE_{stage} | NEXT_STAGE | Next stage mapping |

---

## Appendix A: Change Log

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-01-26 | System | Initial version based on FD v1.2 and code |

---

## Appendix B: Assumptions

| ID | Assumption | Impact |
|----|------------|--------|
| [A1] | All APIs use JSON format | Request/Response format |
| [A2] | Authentication via JWT token | Security implementation |
| [A3] | PLUS uses APIM for API calls | External integration |
| [A4] | Reports stored in Azure Blob Storage | Report download |

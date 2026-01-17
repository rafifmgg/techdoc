# OCMS 19 - Revive Suspensions: API Planning Document

## Document Information
| Attribute | Value |
|-----------|-------|
| Version | 1.0 |
| Date | 2026-01-07 |
| OCMS Number | OCMS 19 |
| Feature | Revive Suspensions |
| Source | Functional Document v1.1, Backend Code Analysis |

---

## 1. Internal APIs

### 1.1 HST Revival API

**Endpoint:** `POST /v1/revive-hst`
**Purpose:** Revive TS-HST suspensions and remove HST record from the system
**Source System:** OCMS Staff Portal (HST Module)

#### Request Payload
| Field | Type | Required | Description | Sample Value |
|-------|------|----------|-------------|--------------|
| idNo | String | Yes | Offender ID number (NRIC/UEN) | S1234567A |
| name | String | Yes | Offender name | John Doe |
| streetName | String | Yes | Street name | Bukit Merah View |
| blkHseNo | String | Yes | Block/House number | 65 |
| floorNo | String | Yes | Floor number | 29 |
| unitNo | String | Yes | Unit number | 1551 |
| bldgName | String | Yes | Building name | The Amberlyn |
| postalCode | String | Yes | Postal code | 578640 |

#### Response Structure
```json
[
  {
    "noticeNo": "541000009T",
    "appCode": "OCMS-2000",
    "message": "OK"
  }
]
```

#### Response Codes
| Code | Description |
|------|-------------|
| OCMS-2000 | Success |
| OCMS-4000 | Required field missing |
| OCMS-4001 | HST ID not found |
| OCMS-5000 | System error |

---

### 1.2 PLUS Staff Revival API

**Endpoint:** `POST /plus-revive-suspension`
**Purpose:** Manual revival of TS/PS suspensions by PLUS staff
**Source System:** PLUS Staff Portal

#### Request Payload
| Field | Type | Required | Description | Sample Value |
|-------|------|----------|-------------|--------------|
| noticeNo | String[] | Yes | Array of notice numbers | ["500500303J"] |
| suspensionType | String | Yes | Suspension type (TS/PS) | TS |
| revivalReason | String | Yes | Revival reason code | MAN |
| revivalRemarks | String | No | Revival remarks (max 50 chars) | Appeal resolved |
| officerAuthorisingRevival | String | Yes | Officer ID | PLU_1 |

#### Supported Revival Reason Codes (PLUS)
| Code | Description |
|------|-------------|
| APE | Appeal Expired |
| APP | Appeal Pending |
| CCE | Call Centre Enquiry |
| MS | Manual Suspension |
| PRI | Priority |
| RED | Redirect |

#### Response Structure
```json
{
  "totalProcessed": 2,
  "successCount": 1,
  "errorCount": 1,
  "results": [
    {
      "noticeNo": "500500303J",
      "appCode": "OCMS-2000",
      "message": "TS Revival Success"
    },
    {
      "noticeNo": "500500304J",
      "appCode": "OCMS-4001",
      "message": "Invalid Notice Number"
    }
  ]
}
```

---

### 1.3 OCMS Staff Revival API

**Endpoint:** `POST /staff-revive-suspension`
**Purpose:** Manual revival of TS/PS suspensions by OCMS staff
**Source System:** OCMS Staff Portal

#### Request Payload
| Field | Type | Required | Description | Sample Value |
|-------|------|----------|-------------|--------------|
| noticeNo | String[] | Yes | Array of notice numbers | ["500500303J", "500500304J"] |
| suspensionType | String | Yes | Suspension type (TS/PS) | TS |
| revivalReason | String | Yes | Revival reason code | MAN |
| revivalRemarks | String | No | Revival remarks (max 50 chars) | Issue resolved |
| officerAuthorisingRevival | String | Yes | Officer ID | JOHNLEE |

#### Response Structure
Same as PLUS Staff Revival API (Section 1.2)

---

### 1.4 Internal Revival Processing API

**Endpoint:** `POST /ocms/v1/suspension/apply-revival`
**Purpose:** Internal API for revival processing (called by SuspensionApiClient)
**Source System:** OCMS Backend, OCMS Cron Service

#### Request Payload
| Field | Type | Required | Description | Sample Value |
|-------|------|----------|-------------|--------------|
| noticeNos | String[] | Yes | List of notice numbers | ["500500303J"] |
| revivalRemarks | String | No | Revival remarks | Suspension period over |
| officerAuthorisingRevival | String | Yes | Officer ID | ocmsizmgr |
| revivalSource | String | Yes | Source code (004=OCMS, 005=PLUS) | 004 |

#### Retry Configuration
| Parameter | Value | Description |
|-----------|-------|-------------|
| maxAttempts | 3 | Maximum retry attempts |
| initialDelay | 2000ms | Initial retry delay |
| maxDelay | 30000ms | Maximum retry delay |
| backoffStrategy | Exponential | With 0-15% jitter |

---

### 1.5 Auto Revival Trigger API

**Endpoint:** `POST /api/cron/auto-revival/trigger`
**Purpose:** Manually trigger auto-revival job (for testing/ad-hoc execution)
**Source System:** OCMS Admin Cron Service

#### Response Structure
```json
{
  "success": true,
  "message": "Job execution summary"
}
```

---

## 2. Revival Reason Codes (Master Data)

| Code | Reason | Scenario |
|------|--------|----------|
| CSR | Change suspension reason | A new suspension reason has been identified |
| CWN | Cancelled wrong notice | PS was incorrectly applied to the Notice |
| DPU | Driver's Particulars Updated | OCMS received updated Driver's particulars |
| HAV | HST Address Verified | TS-HST lifted after address verification |
| OPU | Owner's Particulars Updated | OCMS received updated Owner's particulars |
| OTH | Others | Suspension lifted due to other reasons |
| PSR | Permanent suspension revival | PS has been lifted |
| RDN | Redirect notice | TS lifted to redirect Notice to another offender |
| SPO | Suspension period is over | TS lifted as suspension has expired |
| SUP | Speed-up processing | TS lifted to expedite Notice processing |
| SYS | System issue resolved | TS lifted as system issue has been resolved |

---

## 3. Error Codes Reference

| Code | Meaning | Context |
|------|---------|---------|
| OCMS-2000 | Success | All successful operations |
| OCMS-4000 | Invalid request | Missing required fields, invalid format |
| OCMS-4001 | Not found | Notice/HST ID not found |
| OCMS-4002 | Invalid state | Notice not suspended when revival requested |
| OCMS-4003 | Not supported | PS revival attempted (not yet implemented) |
| OCMS-4004 | Unknown suspension type | Invalid suspension_type value |
| OCMS-4005 | Data missing | Suspended notice records not found |
| OCMS-4006 | No active suspension | Active TS suspension not found |
| OCMS-4007 | Not allowed | HST revival via regular endpoint |
| OCMS-5000 | System error | Exception during processing |
| OCMS-9999 | API error | SuspensionApiClient retry exhausted |

---

## 4. Database Tables Affected

### 4.1 ocms_valid_offence_notice (Intranet)

| Field | Update Action | Description |
|-------|---------------|-------------|
| suspension_type | Set to NULL | Clears active suspension |
| epr_reason_suspension | Set to NULL | Clears EPR suspension reason |
| epr_reason_suspension_date | Set to NULL | Clears EPR suspension date |
| crs_reason_suspension | Set to NULL | Clears CRS suspension reason |
| crs_reason_suspension_date | Set to NULL | Clears CRS suspension date |
| due_date_of_revival | Set to NULL | Clears revival due date |
| next_processing_date | Set to NOW() + 2 days | Sets next processing date |
| upd_date | Set to NOW() | Audit timestamp |
| upd_user_id | Set to officer ID | Audit trail |

### 4.2 eocms_valid_offence_notice (Internet)

| Field | Update Action | Description |
|-------|---------------|-------------|
| suspension_type | Set to NULL | Clears active suspension |

### 4.3 ocms_suspended_notice

| Field | Update Action | Description |
|-------|---------------|-------------|
| date_of_revival | Set to NOW() | Revival timestamp |
| revival_reason | Set to revival code | Reason for revival |
| officer_authorising_revival | Set to officer ID | Officer who revived |
| revival_remarks | Set to remarks | Optional remarks |

---

## 5. Source System Codes

| Code | System |
|------|--------|
| 004 | OCMS (Staff Portal, Backend, Cron) |
| 005 | PLUS (Staff Portal) |

---

## 6. External APIs

### 6.1 PLUS Backend Integration

**Direction:** PLUS Backend → OCMS Backend
**Protocol:** REST API over HTTPS
**Authentication:** JWT Token + API Key

#### Request Flow
1. PLUS Staff Portal triggers revival request
2. PLUS Backend calls OCMS Backend API
3. OCMS Backend validates and processes revival
4. OCMS Backend returns response to PLUS Backend
5. PLUS Backend forwards response to PLUS Staff Portal

---

## Appendix: API Response Scenarios

| Scenario | Response Message |
|----------|------------------|
| API authentication failed (JWT) | Invalid JWT token |
| API authentication failed (API key) | Invalid Auth Token |
| sr_no not found in ocms_suspended_notices | Suspension record not found |
| Suspension is not active (invalid status) | No active suspension |
| Notice is under Court Stage (not CPC/CFC) | Notice is under Court processing |
| User is not authorized for TS-HST revival | User not authorised to revive TS-HST |
| TS was revived successfully with no follow-up | TS revived successfully |
| TS was revived and follow-up reapplied | TS revived and looping {type}-{code} re-applied |
| PS-FP was revived and refund marked | PS-FP revived and refund date identified |
| Failed to reapply follow-up TS | Looping {type}-{code} failed |
| Next Processing Date missing or corrupt | Invalid Next Processing Date |
| Generic internal error during revival | Backend error during revival |

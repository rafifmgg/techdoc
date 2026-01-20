# API Plan: OCMS 14 Section 6 - VIP Vehicle Notice Processing

## Overview

| Attribute | Value |
| --- | --- |
| API Name | VIP Vehicle Notice Processing APIs |
| Version | v1.0 |
| Author | Claude |
| Created Date | 15/01/2026 |
| Last Updated | 15/01/2026 |
| Status | Draft |
| Related Document | OCMS 14 Technical Doc Section 6 |
| Functional Document | v1.8_OCMS 14 Functional Document Section 7 |

---

## 1. Purpose

This document defines the API specifications for VIP Vehicle Notice Processing in OCMS. It covers notice creation with VIP detection, suspension management (TS-OLD, TS-CLV), stage transitions, furnish driver/hirer functionality, and the Classified Vehicle Report generation.

---

## 2. Internal APIs

### 2.1 VIP Vehicle Detection (During Notice Creation)

| Attribute | Value |
| --- | --- |
| Method | Internal Function (Called during notice creation) |
| Trigger | Notice creation flow |
| Source | OCMS 7 - Vehicle Registration Type Check |

#### Logic Flow

```
1. After Notice is created at NPA stage
2. System checks if vehicle_registration_type = 'V'
3. If VIP detected:
   - Apply TS-OLD suspension (21 days)
   - Set next_processing_stage = 'ROV'
   - Set due_date_of_revival = current_date + 21 days
```

#### Database Updates

| Table | Field | Value |
| --- | --- | --- |
| ocms_valid_offence_notice | vehicle_registration_type | 'V' |
| ocms_valid_offence_notice | suspension_type | 'TS' |
| ocms_valid_offence_notice | epr_reason_of_suspension | 'OLD' |
| ocms_valid_offence_notice | due_date_of_revival | current_date + 21 days |
| ocms_suspended_notice | suspension_type | 'TS' |
| ocms_suspended_notice | reason_of_suspension | 'OLD' |

---

### 2.2 Check VIP Vehicle from CAS/FOMS

| Attribute | Value |
| --- | --- |
| Method | POST |
| URL | `/api/v1/cas/vip-vehicle/check` |
| Authentication | Internal Service |
| Purpose | Check if vehicle has valid VIP Parking Label |

#### Request

**Request Body:**

```json
{
  "vehicleNo": "SBA1234A"
}
```

**Request Body Schema:**

| Field | Type | Required | Validation | Description |
| --- | --- | --- | --- | --- |
| vehicleNo | string | Yes | max: 14 chars | Vehicle number to check |

#### Response

**Success Response (200 OK):**

```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Success",
    "vehicleNo": "SBA1234A",
    "isVipVehicle": true,
    "vipLabelStatus": "ACTIVE",
    "labelExpiryDate": "2026-12-31"
  }
}
```

**Response Schema:**

| Field | Type | Description | Source |
| --- | --- | --- | --- |
| appCode | string | Application response code | System generated |
| message | string | Response message | System generated |
| vehicleNo | string | Vehicle number | Request input |
| isVipVehicle | boolean | True if vehicle has valid VIP label | Derived from vip_vehicle.status |
| vipLabelStatus | string | ACTIVE / EXPIRED / CANCELLED | vip_vehicle.status |
| labelExpiryDate | date | VIP label expiry date | vip_vehicle.label_expiry_date |

**Failure Response:**

```json
{
  "data": {
    "appCode": "OCMS-5000",
    "message": "Service unavailable. Please try again later."
  }
}
```

#### [ASSUMPTION]
CAS database connection is temporarily disabled. When FOMS goes live, this will query FOMS instead of CAS.

---

### 2.3 Manual Revival of TS-OLD Suspension

| Attribute | Value |
| --- | --- |
| Method | POST |
| URL | `/api/v1/notices/{noticeNo}/revival` |
| Authentication | Bearer Token (OIC Role) |
| Purpose | OIC manually revives TS-OLD suspension after investigation |

#### Request

**Path Parameters:**

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| noticeNo | string | Yes | Notice number |

**Request Body:**

```json
{
  "revivalReason": "INV",
  "revivalRemarks": "Investigation completed, vehicle confirmed as VIP",
  "officerAuthorising": "OIC_JOHN"
}
```

**Request Body Schema:**

| Field | Type | Required | Validation | Description |
| --- | --- | --- | --- | --- |
| revivalReason | string | Yes | max: 3 chars | Revival reason code |
| revivalRemarks | string | No | max: 200 chars | Remarks for revival |
| officerAuthorising | string | Yes | max: 50 chars | Officer authorizing revival |

#### Response

**Success Response (200 OK):**

```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Suspension revived successfully",
    "noticeNo": "500500303J",
    "previousSuspension": "TS-OLD",
    "revivalDate": "2026-01-15T10:30:00Z",
    "nextProcessingStage": "ROV",
    "nextProcessingDate": "2026-01-15"
  }
}
```

**Failure Response:**

```json
{
  "data": {
    "appCode": "OCMS-4004",
    "message": "Notice not found"
  }
}
```

#### Database Updates

| Table | Field | Value |
| --- | --- | --- |
| ocms_valid_offence_notice | suspension_type | null |
| ocms_valid_offence_notice | epr_reason_of_suspension | null |
| ocms_valid_offence_notice | next_processing_stage | 'ROV' |
| ocms_valid_offence_notice | next_processing_date | current_date |
| ocms_suspended_notice | date_of_revival | current_datetime |
| ocms_suspended_notice | revival_reason | revivalReason |
| ocms_suspended_notice | officer_authorising_revival | officerAuthorising |

---

### 2.4 Apply TS-CLV Suspension

| Attribute | Value |
| --- | --- |
| Method | POST |
| URL | `/api/v1/notices/{noticeNo}/suspension/clv` |
| Authentication | Bearer Token (OIC/System) |
| Purpose | Apply TS-CLV suspension at CPC stage (or RR3/DR3 for MVP1) |

#### Request

**Path Parameters:**

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| noticeNo | string | Yes | Notice number |

**Request Body:**

```json
{
  "suspensionSource": "OCMS",
  "officerAuthorising": "ocmsiz_app_conn",
  "suspensionRemarks": "Auto TS-CLV at end of RR3 stage"
}
```

#### Response

**Success Response (200 OK):**

```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "TS-CLV suspension applied successfully",
    "noticeNo": "500500303J",
    "suspensionType": "TS",
    "reasonOfSuspension": "CLV",
    "suspensionDate": "2026-01-15T23:59:59Z",
    "dueRevivalDate": "2026-02-14T23:59:59Z"
  }
}
```

**Failure Response:**

```json
{
  "data": {
    "appCode": "VIP-004",
    "message": "Invalid suspension state for this operation"
  }
}
```

#### Database Updates

| Table | Field | Value |
| --- | --- | --- |
| ocms_valid_offence_notice | suspension_type | 'TS' |
| ocms_valid_offence_notice | epr_reason_of_suspension | 'CLV' |
| ocms_valid_offence_notice | epr_date_of_suspension | current_datetime |
| ocms_valid_offence_notice | due_date_of_revival | current_date + suspension_period |
| ocms_valid_offence_notice | last_processing_stage | 'CPC' (or 'RR3'/'DR3' for MVP1) |
| ocms_valid_offence_notice | next_processing_stage | 'CSD' |
| ocms_suspended_notice | (new record) | suspension details |

---

### 2.5 Furnish Driver/Hirer for VIP Notice

| Attribute | Value |
| --- | --- |
| Method | POST |
| URL | `/api/v1/notices/{noticeNo}/furnish` |
| Authentication | Bearer Token (eService Portal) |
| Purpose | Vehicle owner furnishes driver/hirer particulars |
| Reference | OCMS 41 Functional Document |

#### Request

**Path Parameters:**

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| noticeNo | string | Yes | Notice number |

**Request Body:**

```json
{
  "hirerDriverIndicator": "D",
  "furnishIdType": "N",
  "furnishIdNo": "S1234567A",
  "furnishName": "JOHN DOE",
  "furnishTelCode": "65",
  "furnishTelNo": "91234567",
  "furnishEmailAddr": "john@example.com",
  "supportingDocuments": [
    {
      "docType": "RENTAL_AGREEMENT",
      "fileName": "rental.pdf",
      "fileBase64": "..."
    }
  ]
}
```

**Request Body Schema:**

| Field | Type | Required | Validation | DB Field | Description |
| --- | --- | --- | --- | --- | --- |
| hirerDriverIndicator | string | Yes | Enum: D, H | hirer_driver_indicator | D=Driver, H=Hirer |
| furnishIdType | string | Yes | Enum: N, F, P | furnish_id_type | N=NRIC, F=FIN, P=Passport |
| furnishIdNo | string | Yes | max: 12 chars | furnish_id_no | ID number |
| furnishName | string | Yes | max: 66 chars | furnish_name | Full name |
| furnishTelCode | string | No | max: 3 chars | furnish_tel_code | Country code |
| furnishTelNo | string | No | max: 12 chars | furnish_tel_no | Contact number |
| furnishEmailAddr | string | No | max: 320 chars | furnish_email_addr | Email address |
| supportingDocuments | array | No | max: 5 files | (separate table) | Supporting documents |

#### Response

**Success Response (200 OK):**

```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Furnish application submitted successfully",
    "noticeNo": "500500303J",
    "txnRefNo": "FA20260115001",
    "status": "PENDING",
    "submittedDate": "2026-01-15T10:30:00Z"
  }
}
```

**Response Field Mapping:**

| Response Field | DB Field | Source |
| --- | --- | --- |
| txnRefNo | txn_ref_no | Generated sequence |
| status | status | Default: PENDING |

**Failure Response:**

```json
{
  "data": {
    "appCode": "OCMS-4010",
    "message": "Furnish not allowed at this stage"
  }
}
```

#### Processing Flow

1. Internet database: Insert into `eocms_furnish_application`
2. Sync to Intranet: `ocms_furnish_application`
3. Backend validation of particulars
4. If auto-approve criteria met → Auto approve
5. If not → OIC manual review required
6. After approval → Update notice stage to DN1 (Driver) or RD1 (Hirer)

---

## 3. Cron Job APIs

### 3.1 Auto Revival TS-OLD Cron

| Attribute | Value |
| --- | --- |
| Job Name | Auto Revival TS-OLD |
| Schedule | Daily at 00:00 |
| Purpose | Auto revive expired TS-OLD suspensions |

#### Query Condition

```sql
SELECT notice_no
FROM ocms_valid_offence_notice
WHERE vehicle_registration_type = 'V'
  AND suspension_type = 'TS'
  AND epr_reason_of_suspension = 'OLD'
  AND due_date_of_revival <= CURRENT_DATE
```

#### Processing Steps

1. Query notices with expired TS-OLD
2. For each notice:
   - Update suspension_type = null
   - Update epr_reason_of_suspension = null
   - Set next_processing_stage = 'ROV'
   - Set next_processing_date = current_date
   - Create revival record in ocms_suspended_notice

---

### 3.2 Prepare RD1/RD2/RR3 for MHA/DH Checks Cron

| Attribute | Value |
| --- | --- |
| Job Name | Prepare Stage for MHA/DH Checks |
| Schedule | Daily at EOD |
| Purpose | Validate offender particulars before stage transition |

#### Query Condition (Example for RD1)

```sql
SELECT notice_no
FROM ocms_valid_offence_notice
WHERE vehicle_registration_type = 'V'
  AND last_processing_stage = 'ROV'
  AND next_processing_stage = 'RD1'
  AND next_processing_date <= CURRENT_DATE
  AND (suspension_type IS NULL OR suspension_type = 'TS')
```

#### Processing Steps

1. Query eligible notices
2. Submit NRIC/FIN to MHA for validation
3. Submit to DataHive for address enrichment
4. Store retrieved data in database
5. Update notice for stage transition

---

### 3.3 Prepare Stage Transition Cron (RD1/RD2/RR3)

| Attribute | Value |
| --- | --- |
| Job Name | Prepare for Stage (RD1/RD2/RR3) |
| Schedule | Daily at EOD (after MHA/DH checks) |
| Purpose | Generate reminder letters and update stage |

#### Processing Steps

1. Query notices ready for stage transition
2. Add notices to letter generation list
3. Send letter list to printing vendor via SFTP
4. Update last_processing_stage
5. Update next_processing_stage and next_processing_date

#### Database Updates

| From Stage | To Stage | Letter Type |
| --- | --- | --- |
| ROV | RD1 | RD1 Reminder Letter |
| RD1 | RD2 | RD2 Reminder Letter |
| RD2 | RR3 | RR3 Final Reminder Letter |

---

### 3.4 Apply TS-CLV at CPC Cron (MVP1: at RR3/DR3)

| Attribute | Value |
| --- | --- |
| Job Name | Suspend VIP Notice with TS-CLV |
| Schedule | Daily at EOD |
| Purpose | Apply TS-CLV to outstanding VIP notices at CPC stage |

#### Query Condition

```sql
SELECT notice_no
FROM ocms_valid_offence_notice
WHERE vehicle_registration_type = 'V'
  AND last_processing_stage IN ('RR3', 'DR3')
  AND next_processing_stage = 'CPC'
  AND next_processing_date <= CURRENT_DATE
  AND (suspension_type IS NULL OR suspension_type = 'TS')
```

#### Processing Steps

1. Query outstanding VIP notices at end of RR3/DR3
2. Move notice to CPC stage
3. Apply TS-CLV suspension
4. Create suspension record in ocms_suspended_notice
5. Sync to Internet database

---

### 3.5 Auto Re-apply TS-CLV Cron

| Attribute | Value |
| --- | --- |
| Job Name | Auto Re-apply TS-CLV |
| Schedule | Daily at 00:00 |
| Purpose | Re-apply TS-CLV when previous TS-CLV expires (looping TS) |

#### Query Condition

```sql
SELECT notice_no
FROM ocms_valid_offence_notice
WHERE vehicle_registration_type = 'V'
  AND last_processing_stage = 'CPC'
  AND suspension_type = 'TS'
  AND epr_reason_of_suspension = 'CLV'
  AND due_date_of_revival <= CURRENT_DATE
```

#### Processing Steps

1. Query VIP notices with expired TS-CLV at CPC stage
2. For each notice:
   - Create revival record
   - Re-apply TS-CLV with new due_date_of_revival
   - Create new suspension record

---

### 3.6 Generate Classified Vehicle Report Cron

| Attribute | Value |
| --- | --- |
| Job Name | Generate Classified Vehicle Notices Report |
| Schedule | Daily at configured time |
| Purpose | Generate daily report for OICs |

#### Query Conditions

**Type V Notices:**
```sql
SELECT * FROM ocms_valid_offence_notice von
JOIN vip_vehicle vv ON von.vehicle_no = vv.vehicle_no
WHERE vv.status = 'ACTIVE'
```

**Amended Notices (V→S):**
```sql
SELECT * FROM ocms_valid_offence_notice von
JOIN ocms_suspended_notice sn ON von.notice_no = sn.notice_no
WHERE sn.reason_of_suspension = 'CLV'
  AND sn.date_of_revival IS NOT NULL
  AND von.vehicle_registration_type = 'S'
```

#### Output

Excel report with 3 sheets:
1. **Summary** - Total issued, outstanding, settled, amended counts
2. **Type V Notices Detail** - All Type V notice details
3. **Amended Notices (V→S)** - Notices changed from V to S

#### Email

| Attribute | Value |
| --- | --- |
| Recipients | OICs (configured in system) |
| Subject | Classified Vehicle Notices Report - {date} |
| Body | Summary counts |
| Attachment | Excel report file |

---

## 4. External API Integrations

### 4.1 LTA VRLS - Vehicle Ownership Check

| Attribute | Value |
| --- | --- |
| Direction | Outbound |
| Trigger | After TS-OLD revived, at ROV stage |
| Purpose | Retrieve vehicle ownership details |

#### Request

```json
{
  "vehicleNo": "SBA1234A",
  "requestType": "OWNERSHIP"
}
```

#### Response

```json
{
  "vehicleNo": "SBA1234A",
  "ownerName": "JOHN DOE",
  "ownerIdType": "NRIC",
  "ownerIdNo": "S1234567A",
  "registrationDate": "2020-01-15",
  "vehicleType": "CAR"
}
```

---

### 4.2 MHA - Offender Particulars Validation

| Attribute | Value |
| --- | --- |
| Direction | Outbound |
| Trigger | Before each stage transition (RD1, RD2, RR3) |
| Purpose | Validate and retrieve offender particulars |

#### Request

```json
{
  "idType": "NRIC",
  "idNumber": "S1234567A"
}
```

#### Response

```json
{
  "idNumber": "S1234567A",
  "name": "JOHN DOE",
  "dateOfBirth": "1980-01-15",
  "nationality": "SINGAPOREAN",
  "address": {
    "block": "123",
    "street": "ORCHARD ROAD",
    "unit": "#01-01",
    "postalCode": "238888"
  },
  "isDeceased": false
}
```

---

### 4.3 DataHive - Address Enrichment

| Attribute | Value |
| --- | --- |
| Direction | Outbound |
| Trigger | Before each stage transition |
| Purpose | Enrich offender address and UEN information |

#### Request

```json
{
  "queryType": "NRIC",
  "queryValue": "S1234567A"
}
```

#### Response

```json
{
  "queryValue": "S1234567A",
  "currentAddress": {
    "fullAddress": "123 ORCHARD ROAD #01-01 SINGAPORE 238888",
    "lastUpdated": "2025-12-01"
  },
  "uen": null
}
```

---

### 4.4 SFTP - Toppan Letter Generation

| Attribute | Value |
| --- | --- |
| Direction | Outbound |
| Trigger | During stage transition (RD1, RD2, RR3) |
| Purpose | Send letter generation list to printing vendor |

#### File Format

CSV file with notice details for letter printing.

---

## 5. Token Handling and Retry Mechanism

### 5.1 Token Expiry Handling

All external API integrations (LTA VRLS, MHA, DataHive) must implement token refresh handling.

| Attribute | Value |
| --- | --- |
| Token Type | Bearer Token (OAuth 2.0) |
| Token Expiry | Configurable per service |
| Refresh Strategy | Auto refresh on expiry |

#### Token Refresh Flow

```
1. Make API call with current token
2. IF response = 401 (Unauthorized/Token Expired):
   a. Request new token from auth service
   b. Store new token in cache
   c. Retry original request with new token
   d. Continue processing
3. NEVER stop processing due to token expiry
4. Log token refresh events for monitoring
```

#### Implementation Notes

- Token refresh MUST be automatic and transparent
- Processing MUST continue after token refresh
- System should NOT stop or fail due to token expiry
- Failed token refresh after 3 attempts: Log error and notify admin

### 5.2 API Retry Mechanism

All external API calls must implement retry mechanism with email alert.

| Attribute | Value |
| --- | --- |
| Max Retries | 3 |
| Retry Interval | Exponential backoff (1s, 2s, 4s) |
| Alert Trigger | After all retries exhausted |
| Alert Method | Email notification |

#### Retry Flow

```
1. Make API call
2. IF connection failure or timeout:
   a. Retry 1: Wait 1 second, retry
   b. Retry 2: Wait 2 seconds, retry
   c. Retry 3: Wait 4 seconds, retry
3. IF all retries fail:
   a. Stop processing for this record
   b. Trigger email alert to admin
   c. Log error with full details
   d. Continue with next record (if batch processing)
```

#### Email Alert Configuration

| Attribute | Value |
| --- | --- |
| Recipients | Configured admin list (from parameter table) |
| Subject | `[OCMS Alert] External API Failure - {API_NAME}` |
| Body Content | API name, error details, timestamp, affected record(s) |
| Template | Stored in `ocms_template_store` |

#### Error Scenarios Triggering Email

| Scenario | API | Action |
| --- | --- | --- |
| Connection Timeout | LTA VRLS | Retry 3x, then email alert |
| Connection Timeout | MHA | Retry 3x, then email alert |
| Connection Timeout | DataHive | Retry 3x, then email alert |
| Authentication Failed | Any | Retry token refresh 3x, then email alert |
| Service Unavailable (503) | Any | Retry 3x, then email alert |

---

## 6. Batch Job Configuration

### 6.1 Shedlock Naming Convention

All batch jobs must follow the Shedlock naming convention for distributed lock management.

#### Naming Pattern

| Type | Pattern | Example |
| --- | --- | --- |
| File/Report Operations | `[action]_[subject]_[suffix]` | `generate_report_cvnotices` |
| API/Other Operations | `[action]_[subject]` | `sync_payment` |
| Special Cases | `[action]_[subject][term]` | `process_photosUpload` |

#### Batch Job Shedlock Names

| Job Name | Shedlock Name | Schedule | Type |
| --- | --- | --- | --- |
| Auto Revival TS-OLD | `revive_tsold_expired` | Daily 00:00 | API/Other |
| Prepare RD1 for MHA/DH | `prepare_rd1_mhadh` | Daily EOD | API/Other |
| Prepare RD2 for MHA/DH | `prepare_rd2_mhadh` | Daily EOD | API/Other |
| Prepare RR3 for MHA/DH | `prepare_rr3_mhadh` | Daily EOD | API/Other |
| Apply TS-CLV at CPC | `apply_tsclv_cpc` | Daily EOD | API/Other |
| Auto Re-apply TS-CLV | `reapply_tsclv_expired` | Daily 00:00 | API/Other |
| Generate CV Report | `generate_report_cvnotices` | Daily (TBD) | File/Report |
| AN Letter Generation | `generate_letter_advisory` | Daily EOD | File/Report |

### 6.2 Batch Job Tracking

All batch jobs must implement start time recording for monitoring and recovery.

#### Tracking Requirements

| Attribute | Description |
| --- | --- |
| Start Time | Record immediately when job starts |
| End Time | Record upon job completion |
| Status | RUNNING / COMPLETED / FAILED |
| Records Processed | Count of processed records |
| Records Failed | Count of failed records |
| Error Details | Last error message if failed |

#### Tracking Flow

```
1. Job Starts:
   - Record start_time = CURRENT_TIMESTAMP
   - Record status = 'RUNNING'
   - Record job_name, schedule_time

2. During Processing:
   - Update records_processed count periodically
   - Update records_failed count on errors

3. Job Completes:
   - Record end_time = CURRENT_TIMESTAMP
   - Record status = 'COMPLETED' or 'FAILED'
   - Calculate duration = end_time - start_time

4. Benefits:
   - Identify jobs that started but didn't end (stuck)
   - Identify jobs that didn't start at all
   - Monitor long-running jobs
   - Enable job recovery
```

### 6.3 Batch Job Archival

| Attribute | Value |
| --- | --- |
| Retention Period | 3 months |
| Archival Schedule | Monthly cleanup job |
| Archival Action | Delete records older than 3 months |

### 6.4 Logging Guidelines

| Job Type | Logging Destination | Details |
| --- | --- | --- |
| Frequent Sync Jobs | Application logs only | Error messages, no batch table |
| Standard Batch Jobs | Batch table + Application logs | Full tracking |
| Report Generation | Batch table + Application logs | Full tracking |

**Frequent Sync Jobs (Application Logs Only):**
- Auto Re-apply TS-CLV (runs daily, high frequency)

**Standard Batch Jobs (Full Tracking):**
- Generate Classified Vehicle Report
- AN Letter Generation
- Stage transition jobs

---

## 7. Error Handling

### Error Codes

| HTTP Status | Error Code | Description | Resolution |
| --- | --- | --- | --- |
| 400 | VIP_001 | Invalid vehicle number format | Check vehicle number |
| 400 | VIP_002 | Notice not found | Verify notice number |
| 400 | VIP_003 | Notice is not VIP type | Check vehicle_registration_type |
| 400 | VIP_004 | Invalid suspension state | Check current suspension |
| 403 | VIP_005 | Insufficient permissions | User not authorized |
| 409 | VIP_006 | Notice already suspended | Cannot apply duplicate suspension |
| 422 | VIP_007 | Business rule violation | See error details |
| 500 | VIP_008 | External service error | Retry or contact support |

---

## 8. Data Mapping

### Request to Database

| API Field | Database Table | Database Field | Transformation |
| --- | --- | --- | --- |
| noticeNo | ocms_valid_offence_notice | notice_no | Direct |
| suspensionType | ocms_valid_offence_notice | suspension_type | Direct |
| reasonOfSuspension | ocms_valid_offence_notice | epr_reason_of_suspension | Direct |
| revivalReason | ocms_suspended_notice | revival_reason | Direct |
| officerAuthorising | ocms_suspended_notice | officer_authorising_suspension | Direct |

---

## 9. Assumptions Log

| ID | Assumption | Reason | Impact |
| --- | --- | --- | --- |
| A001 | CAS VIP_VEHICLE table connection disabled | Backend code shows temporarily disabled | VIP check may use alternate method |
| A002 | TS-CLV applied at RR3/DR3 for MVP1 | FD states court processing post-MVP1 | Will change to CPC post-MVP1 |
| A003 | TS-CLV suspension period configurable | FD references OCMS_Suspension_Reason table | Period stored in configuration |
| A004 | Report timing TBD with users | FD states timing not finalized | To be confirmed |

---

## 10. Changelog

| Version | Date | Author | Changes |
| --- | --- | --- | --- |
| 1.0 | 15/01/2026 | Claude | Initial version |
| 1.1 | 19/01/2026 | Claude | Fixed Critical Issues: Changed GET to POST, updated response format, added token handling, retry mechanism, Shedlock naming, batch job tracking |

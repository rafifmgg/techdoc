# API Plan: Notice Processing Flow for Deceased Offenders

## Overview

| Attribute | Value |
| --- | --- |
| Feature Name | Notice Processing Flow for Deceased Offenders |
| Version | v1.0 |
| Author | Claude |
| Created Date | 15/01/2026 |
| Last Updated | 15/01/2026 |
| Status | Draft |
| FD Reference | OCMS 14 - Section 4 |
| TD Reference | OCMS 14 - Section 3 |

---

## 1. Purpose

This document outlines the APIs, service calls, and database operations involved in processing notices where the offender is identified as deceased. The system detects deceased offenders through MHA (for NRIC holders) or DataHive (for FIN holders) and applies permanent suspension (PS-RIP or PS-RP2) based on the date of death relative to the offence date.

---

## 2. API Overview

### 2.1 Internal Service Calls

The Deceased Offender Processing involves the following internal services:

| Service | Type | Purpose |
| --- | --- | --- |
| Permanent Suspension Service | Internal Service | Apply PS-RIP or PS-RP2 suspension to notices |
| Offender Particulars Service | Internal Service | Update life_status and date_of_death |
| RIP Report Service | Internal Service | Generate "RIP Hirer Driver Furnished" report |
| Notice Redirect Service | Internal Service | Redirect notices to new offender after PS revival |

---

## 3. External System Integration

### 3.1 MHA Life Status Check (NRIC Holders)

| Attribute | Value |
| --- | --- |
| Type | External API Integration |
| System | Ministry of Home Affairs (MHA) |
| Protocol | SFTP File Exchange |
| Purpose | Retrieve life status and date of death for NRIC holders |
| Reference | OCMS 8 - Section 3.2.2 |

#### Input (Request to MHA)

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| nric | String | Yes | NRIC number of the offender |
| request_type | String | Yes | Type of request (owner/hirer/driver check) |

#### Output (Response from MHA)

| Field | Type | Description |
| --- | --- | --- |
| life_status | String(1) | 'A' = Alive, 'D' = Dead |
| date_of_death | Date | Date of death (if deceased) |
| name | String | Full name of the person |
| address | String | Registered address |

#### Life Status Values

| Code | Description |
| --- | --- |
| A | Alive |
| D | Dead (Deceased) |

---

### 3.2 DataHive FIN Death Check (FIN Holders)

| Attribute | Value |
| --- | --- |
| Type | External Database Query |
| System | DataHive |
| Dataset | Dead Foreign Pass Holders (D90) |
| Purpose | Check if FIN holder is in the deceased dataset |
| Reference | OCMS 8 - Section 7.5 |

#### Query Pattern

```sql
SELECT FIN, DATE_OF_DEATH, REFERENCE_PERIOD
FROM V_DH_MHA_FINDEATH
WHERE FIN = '<fin_number>'
```

#### Input

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| fin | String | Yes | FIN number of the offender |

#### Output

| Field | Type | Description |
| --- | --- | --- |
| fin | String | FIN number |
| date_of_death | Date | Date of death |
| reference_period | String | Reference period of the data |

#### DataHive Life Status Derivation

| Condition | life_status | Description |
| --- | --- | --- |
| FIN found in D90 dataset | D | FIN holder is deceased |
| FIN not found in D90 dataset | A | FIN holder is alive (default) |

---

## 4. Internal APIs

### 4.1 Apply PS-RIP/RP2 Suspension

| Attribute | Value |
| --- | --- |
| Type | Internal Service Call |
| Service | PermanentSuspensionHelper |
| Method | processPS() |
| Trigger | Auto-triggered when deceased offender detected |

#### Input Parameters

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| noticeNo | String | Yes | Notice number to suspend |
| suspensionSource | String | Yes | Source: 'CRON' for auto-suspension |
| reasonOfSuspension | String | Yes | 'RIP' or 'RP2' |
| suspensionRemarks | String | No | Additional remarks |
| officerAuthorisingSuspension | String | Yes | 'SYSTEM' for auto-suspension |
| srNo | String | No | Suspension record number |
| caseNo | String | No | Related case number |

#### Suspension Code Determination

| Condition | Suspension Code | Description |
| --- | --- | --- |
| date_of_death >= offence_date | RIP | Motorist Deceased On or After Offence Date |
| date_of_death < offence_date | RP2 | Motorist Deceased Before Offence Date |

#### Response

| Field | Type | Description |
| --- | --- | --- |
| data.appCode | String | Application code (e.g., "OCMS-2000" for success) |
| data.message | String | Response message |
| noticeNo | String | Notice number (on success) |

**Response Format (Success):**
```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "PS suspension applied successfully"
  },
  "noticeNo": "A12345678X"
}
```

**Response Format (Error):**
```json
{
  "data": {
    "appCode": "OCMS-4001",
    "message": "Invalid Notice Number"
  }
}
```

#### Error Codes

| App Code | Message | Scenario |
| --- | --- | --- |
| OCMS-2000 | PS suspension applied successfully | Success |
| OCMS-2001 | Notice already has this PS code | Notice already has this PS code (idempotent - treated as success) |
| OCMS-4000 | Suspension Source is missing | Source not provided |
| OCMS-4001 | Invalid Notice Number | Notice not found in database |
| OCMS-4003 | Paid/partially paid notices only allow APP, CFA, or VST | Cannot apply RIP/RP2 to paid notice |
| OCMS-5000 | System error. Please inform Administrator | Unexpected system error |

---

### 4.2 Update Offender Particulars

| Attribute | Value |
| --- | --- |
| Type | Internal Database Update |
| Table | ocms_offence_notice_owner_driver |
| Trigger | When MHA/DataHive returns deceased status |

#### Fields Updated

| Field | Type | Description | Sample Value |
| --- | --- | --- | --- |
| life_status | String(1) | Life status indicator | 'D' |
| date_of_death | DateTime | Date of death from MHA/DataHive | 2024-09-18 00:00:00 |
| upd_date | DateTime | Record update timestamp | Current timestamp |
| upd_user_id | String | User who updated | 'SYSTEM' |

---

### 4.3 Generate RIP Hirer Driver Furnished Report

| Attribute | Value |
| --- | --- |
| Type | Internal Service - Scheduled Report |
| Trigger | Daily cron job |
| Purpose | Generate report for RP2 notices where deceased is Hirer/Driver |

#### Query Criteria

```sql
-- Query notices with PS-RP2 applied today where current offender is deceased Hirer/Driver
SELECT
    von.notice_no,
    ond.name AS offender_name,
    ond.id_no AS offender_id,
    ond.owner_driver_indicator,
    ond.life_status,
    ond.date_of_death,
    von.notice_date_and_time AS offence_date,
    sn.date_of_suspension
FROM ocms_suspended_notice sn
JOIN ocms_valid_offence_notice von ON sn.notice_no = von.notice_no
JOIN ocms_offence_notice_owner_driver ond ON von.notice_no = ond.notice_no
WHERE sn.suspension_type = 'PS'
  AND sn.reason_of_suspension = 'RP2'
  AND CAST(sn.date_of_suspension AS DATE) = CAST(GETDATE() AS DATE)
  AND sn.due_date_of_revival IS NULL
  AND ond.owner_driver_indicator IN ('H', 'D')
  AND ond.offender_indicator = 'Y'
  AND ond.life_status = 'D'
```

#### Report Output Fields

| Field | Description |
| --- | --- |
| notice_no | Notice number |
| offender_name | Name of deceased offender |
| offender_id | NRIC/FIN of deceased |
| owner_driver_indicator | H (Hirer) or D (Driver) |
| date_of_death | Date of death |
| suspension_date | Date PS-RP2 was applied |
| offence_date | Original offence date |

---

### 4.4 Revive PS-RIP/RP2 and Redirect Notice

| Attribute | Value |
| --- | --- |
| Type | Internal Service - Manual Action |
| Source | OCMS Staff Portal |
| Trigger | OIC manually revives PS and redirects notice |

#### Step 1: Revive PS Suspension

**Update ocms_suspended_notice:**

| Field | Value | Description |
| --- | --- | --- |
| date_of_revival | Current timestamp | When PS was revived |
| revival_reason | User input | Reason for revival |
| officer_authorising_revival | OIC ID | Officer who authorized |

**Update ocms_valid_offence_notice:**

| Field | Value | Description |
| --- | --- | --- |
| suspension_type | NULL or new value | Clear or update suspension |
| epr_reason_of_suspension | NULL or new value | Clear or update reason |

#### Step 2: Update Offender Particulars

**Update ocms_offence_notice_owner_driver:**

| Field | Description |
| --- | --- |
| All offender fields | Updated with new offender details |
| offender_indicator | Set to 'Y' for new current offender |

**Update ocms_offence_notice_owner_driver_addr:**

| Field | Description |
| --- | --- |
| All address fields | Updated with new offender address |

#### Step 3: Redirect Notice

**Update ocms_valid_offence_notice:**

| Field | Value | Description |
| --- | --- | --- |
| last_processing_stage | RD1 or DN1 | Notice redirected to appropriate stage |
| current_offender_id | New offender ID | Updated to new offender |

---

## 5. Data Flow

### 5.1 Input from Source Systems

| Source System | Data Provided | Trigger |
| --- | --- | --- |
| MHA | life_status, date_of_death for NRIC | NRIC lookup during notice creation/update |
| DataHive | date_of_death for FIN (if in D90) | FIN lookup during notice creation/update |

### 5.2 Processing Flow

```
1. Receive life_status from MHA/DataHive
2. If life_status = 'D':
   a. Update offender particulars (life_status, date_of_death)
   b. Compare date_of_death with offence_date
   c. Apply PS-RIP (if DoD >= offence_date) OR PS-RP2 (if DoD < offence_date)
   d. If PS-RP2 and offender is Hirer/Driver: Include in daily report
3. If life_status = 'A':
   a. Continue normal processing
```

### 5.3 Output to Database

| Zone | Table | Fields Updated |
| --- | --- | --- |
| Intranet | ocms_offence_notice_owner_driver | life_status, date_of_death |
| Intranet | ocms_valid_offence_notice | suspension_type, epr_reason_of_suspension |
| Intranet | ocms_suspended_notice | New record with RIP/RP2 |
| Internet | eocms_offence_notice_owner_driver | Synced life_status, date_of_death |
| Internet | eocms_valid_offence_notice | Synced suspension fields |

---

## 6. Data Mapping

### 6.1 Database Tables Affected

| Table | Purpose | Key Fields |
| --- | --- | --- |
| ocms_offence_notice_owner_driver | Store offender life status | life_status, date_of_death |
| ocms_valid_offence_notice | Store suspension status | suspension_type, epr_reason_of_suspension |
| ocms_suspended_notice | Suspension history | reason_of_suspension, date_of_suspension |
| ocms_offence_notice_owner_driver_addr | Offender address | type_of_address |

### 6.2 Field Mapping - ocms_offence_notice_owner_driver

| Database Field | Source | Description |
| --- | --- | --- |
| life_status | MHA/DataHive | 'A' or 'D' |
| date_of_death | MHA/DataHive | Date of death if deceased |
| owner_driver_indicator | Notice data | 'O'=Owner, 'H'=Hirer, 'D'=Driver |
| offender_indicator | System | 'Y'=Current offender |

### 6.3 Field Mapping - ocms_suspended_notice

| Database Field | Value | Description |
| --- | --- | --- |
| notice_no | From notice | Notice number |
| suspension_type | 'PS' | Permanent Suspension |
| reason_of_suspension | 'RIP' or 'RP2' | Based on date comparison |
| date_of_suspension | Current timestamp | When suspension applied |
| due_date_of_revival | NULL | PS never auto-revives |
| officer_authorising_suspension | 'SYSTEM' | Auto-applied |
| sr_no | Auto-generated | Suspension record number |

---

## 7. Error Handling

### 7.1 System Exceptions

| Exception | Condition | Handling |
| --- | --- | --- |
| MHA Connection Error | Cannot connect to MHA | Log error, retry later |
| DataHive Query Error | D90 query fails | Log error, default to life_status='A' |
| Database Error | Insert/Update fails | Rollback transaction, log error |

### 7.2 Business Exceptions

| Exception | Condition | Handling |
| --- | --- | --- |
| Notice Already Suspended | PS-RIP/RP2 already applied | Return success (idempotent) |
| Invalid Notice | Notice not found | Return error OCMS-4001 |
| Paid Notice | Notice has payment | Return error OCMS-4003 |

---

## 8. Dependencies

| Service/System | Type | Purpose |
| --- | --- | --- |
| MHA Integration | External | NRIC life status lookup |
| DataHive | External | FIN death status lookup |
| Suspension Service | Internal | Apply PS-RIP/RP2 |
| Report Service | Internal | Generate daily RIP report |
| Email Service | Internal | Send report notifications |

---

## 9. Changelog

| Version | Date | Author | Changes |
| --- | --- | --- | --- |
| 1.0 | 15/01/2026 | Claude | Initial version based on FD Section 4 and backend code analysis |
| 1.1 | 19/01/2026 | Claude | Yi Jie compliance: Fixed SELECT * usage, aligned response format, fixed field name consistency |

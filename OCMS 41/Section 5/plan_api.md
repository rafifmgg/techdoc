# OCMS 41 Section 5 - Batch Furnish and Batch Update API Specification

## Document Information

| Field | Value |
|-------|-------|
| Version | 1.1 |
| Date | 2026-01-19 |
| Source | Functional Document v1.1, Section 5 |
| Module | OCMS 41 - Section 5: Batch Furnish and Batch Update |

---

## Table of Contents

1. [Overview](#1-overview)
2. [Internal APIs](#2-internal-apis)
3. [Error Codes Reference](#3-error-codes-reference)
4. [Data Types Reference](#4-data-types-reference)
5. [Technical Standards](#5-technical-standards)

---

## 1. Overview

Section 5 covers the Staff Portal batch functions:
- **Batch Furnish** - Furnish the same offender particulars to multiple Notices at once
- **Batch Update Mailing Address** - Update mailing address for all outstanding Notices of a specific person

### 1.1 Actors

| Actor | Description |
|-------|-------------|
| OIC | Officer-In-Charge who performs batch operations |

### 1.2 API Base Path

```
/api/v1/notice/offender
```

---

## 2. Internal APIs

### 2.1 Check Batch Furnishability

**Endpoint:** `POST /notice/offender/batch/check-furnishability`

**Description:** Checks whether multiple Notices can be furnished. Returns furnishability status for each Notice.

**User Stories:** OCMS41.5.2

#### Request

```json
{
  "noticeList": [
    "500500303J",
    "500500304K",
    "500500305L"
  ]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| noticeList | Array<String> | Yes | List of Notice numbers to check (max 100) |

#### Response (Success)

```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Furnishability check completed",
    "totalNotices": 3,
    "furnishableCount": 2,
    "nonFurnishableCount": 1,
    "results": [
      {
        "noticeNo": "500500303J",
        "furnishable": true,
        "reasonCode": null,
        "reasonMessage": null
      },
      {
        "noticeNo": "500500304K",
        "furnishable": true,
        "reasonCode": null,
        "reasonMessage": null
      },
      {
        "noticeNo": "500500305L",
        "furnishable": false,
        "reasonCode": "LAST_STAGE_AFTER_CPC",
        "reasonMessage": "Notice cannot be furnished because processing stage is after CPC"
      }
    ]
  }
}
```

---

### 2.2 Batch Furnish Offender

**Endpoint:** `POST /notice/offender/batch/furnish`

**Description:** Furnishes the same offender particulars to multiple Notices. Processes each Notice individually and returns consolidated results.

**User Stories:** OCMS41.5.2

#### Request

```json
{
  "noticeList": [
    "500500303J",
    "500500304K"
  ],
  "ownerDriverIndicator": "H",
  "idType": "NRIC",
  "idNo": "S1234567A",
  "name": "JOHN LEE",
  "emailAddr": "john.lee@email.com",
  "countryCode": "65",
  "offenderTelNo": "91234567",
  "address": {
    "blockNo": "123",
    "streetName": "ORCHARD ROAD",
    "buildingName": "PLAZA TOWER",
    "floorNo": "01",
    "unitNo": "01",
    "postalCode": "238888"
  }
}
```

| Field | Type | Required | Max Length | Description |
|-------|------|----------|------------|-------------|
| noticeList | Array<String> | Yes | 100 items | List of Notice numbers to furnish |
| ownerDriverIndicator | String | Yes | 1 | Role: O (Owner), H (Hirer), D (Driver) |
| idType | String | Yes | 20 | ID type: NRIC, FIN, UEN, PASSPORT |
| idNo | String | Yes | 20 | ID number |
| name | String | Yes | 66 | Full name |
| emailAddr | String | No | 320 | Email address |
| countryCode | String | No | 5 | Phone country code |
| offenderTelNo | String | No | 20 | Contact number |
| address | Object | No | - | Mailing address |

#### Response (Success)

```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Batch furnish completed",
    "totalProcessed": 2,
    "successCount": 2,
    "failureCount": 0,
    "successRecords": [
      {
        "noticeNo": "500500303J",
        "offenderName": "JOHN LEE",
        "offenderId": "S1234567A",
        "ownerDriverIndicator": "H",
        "newProcessingStage": "RD2"
      },
      {
        "noticeNo": "500500304K",
        "offenderName": "JOHN LEE",
        "offenderId": "S1234567A",
        "ownerDriverIndicator": "H",
        "newProcessingStage": "RD1"
      }
    ],
    "failedRecords": []
  }
}
```

#### Response (Partial Success)

```json
{
  "data": {
    "appCode": "OCMS-2001",
    "message": "Batch furnish completed with errors",
    "totalProcessed": 3,
    "successCount": 2,
    "failureCount": 1,
    "successRecords": [...],
    "failedRecords": [
      {
        "noticeNo": "500500305L",
        "furnishedName": "JOHN LEE",
        "furnishedId": "S1234567A",
        "errorCode": "DB_UPDATE_FAILED",
        "errorMessage": "Failed to update database"
      }
    ]
  }
}
```

---

### 2.3 Get Outstanding Notices by ID

**Endpoint:** `POST /notice/offender/outstanding`

**Description:** Retrieves all outstanding Notices where the person (by ID number) is the Current Offender. Excludes Notices with active Permanent Suspension.

**User Stories:** OCMS41.5.3

#### Request

```json
{
  "idNo": "S1234567A"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| idNo | String | Yes | ID number to search |

#### Response (Success - Records Found)

```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Outstanding notices retrieved",
    "idNo": "S1234567A",
    "totalNotices": 3,
    "offenderDetails": {
      "name": "JOHN LEE",
      "idType": "NRIC",
      "idNo": "S1234567A"
    },
    "registeredAddress": {
      "blockNo": "456",
      "streetName": "BUKIT TIMAH ROAD",
      "buildingName": null,
      "floorNo": "05",
      "unitNo": "123",
      "postalCode": "289628"
    },
    "notices": [
      {
        "noticeNo": "500500303J",
        "summonNo": "SUM123456",
        "vehicleNo": "SNC7392R",
        "offenceDateTime": "2025-06-15T14:30:00",
        "ownerDriverIndicator": "H",
        "offenceRuleCode": "PV01",
        "amountPayable": 70.00,
        "lastProcessingStage": "RD1",
        "mailingAddress": {
          "blockNo": "123",
          "streetName": "ORCHARD ROAD",
          "buildingName": "PLAZA TOWER",
          "floorNo": "01",
          "unitNo": "01",
          "postalCode": "238888"
        },
        "offenderTelNo": "91234567",
        "emailAddr": "john.lee@email.com"
      },
      {
        "noticeNo": "500500304K",
        "summonNo": "SUM123457",
        "vehicleNo": "SNC7392R",
        "offenceDateTime": "2025-07-20T10:15:00",
        "ownerDriverIndicator": "O",
        "offenceRuleCode": "PV02",
        "amountPayable": 120.00,
        "lastProcessingStage": "OW",
        "mailingAddress": null,
        "offenderTelNo": null,
        "emailAddr": null
      }
    ]
  }
}
```

#### Response (No Records Found)

```json
{
  "data": {
    "appCode": "OCMS-4004",
    "message": "Offender not found",
    "idNo": "S1234567A",
    "totalNotices": 0
  }
}
```

---

### 2.4 Batch Update Mailing Address

**Endpoint:** `POST /notice/offender/batch/update-address`

**Description:** Updates mailing address for multiple Notices of the same offender.

**User Stories:** OCMS41.5.3

#### Request

```json
{
  "idNo": "S1234567A",
  "noticeList": [
    {
      "noticeNo": "500500303J",
      "ownerDriverIndicator": "H"
    },
    {
      "noticeNo": "500500304K",
      "ownerDriverIndicator": "O"
    }
  ],
  "newAddress": {
    "blockNo": "789",
    "streetName": "MARINA BAY",
    "buildingName": "TOWER A",
    "floorNo": "10",
    "unitNo": "05",
    "postalCode": "018989"
  },
  "offenderTelNo": "98765432",
  "emailAddr": "john.new@email.com"
}
```

| Field | Type | Required | Max Length | Description |
|-------|------|----------|------------|-------------|
| idNo | String | Yes | 20 | ID number of offender |
| noticeList | Array | Yes | 100 items | List of notices with their offender types |
| noticeList[].noticeNo | String | Yes | 10 | Notice number |
| noticeList[].ownerDriverIndicator | String | Yes | 1 | O/H/D |
| newAddress | Object | Yes | - | New mailing address |
| offenderTelNo | String | No | 20 | Updated contact number |
| emailAddr | String | No | 320 | Updated email address |

#### Response (Success)

```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Batch update completed",
    "totalProcessed": 2,
    "successCount": 2,
    "failureCount": 0,
    "successRecords": [
      {
        "noticeNo": "500500303J",
        "ownerDriverIndicator": "H",
        "addressUpdated": true
      },
      {
        "noticeNo": "500500304K",
        "ownerDriverIndicator": "O",
        "addressUpdated": true
      }
    ],
    "failedRecords": []
  }
}
```

---

### 2.5 Get Latest Offender and Notice Details

**Endpoint:** `POST /notice/offender/details`

**Description:** Retrieves latest offender and notice details for result page display after batch operations.

**User Stories:** OCMS41.5.2, OCMS41.5.3

#### Request

```json
{
  "noticeNoList": ["500500303J", "500500304K"]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| noticeNoList | Array<String> | Yes | List of Notice numbers |

#### Response (Success)

```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Details retrieved",
    "results": [
      {
        "noticeNo": "500500303J",
        "currentOffender": {
          "name": "JOHN LEE",
          "idNo": "S1234567A",
          "ownerDriverIndicator": "H"
        },
        "lastProcessingStage": "RD2"
      }
    ]
  }
}
```

---

## 3. Error Codes Reference

### 3.1 Validation Errors (HTTP 400)

| Error Type | Field | Message |
|------------|-------|---------|
| VALIDATION_ERROR | noticeList | Notice list is required |
| VALIDATION_ERROR | noticeList | Notice list cannot be empty |
| VALIDATION_ERROR | noticeList | Maximum 100 notices allowed per batch |
| VALIDATION_ERROR | idNo | ID number is required |
| VALIDATION_ERROR | name | Name is required |
| VALIDATION_ERROR | name | Name exceeds maximum length (66 characters) |
| VALIDATION_ERROR | newAddress | Address is required |

### 3.2 Business Errors (HTTP 409)

| Reason | Message |
|--------|---------|
| ALL_NOT_FURNISHABLE | All selected Notices are not furnishable |
| OFFENDER_NOT_FOUND | No outstanding Notices found for this ID |
| NOTICE_NOT_FOUND | Notice not found |
| NOT_CURRENT_OFFENDER | Cannot update - not the current offender |
| PERMANENT_SUSPENSION | Notice has active permanent suspension |

### 3.3 Technical Errors (HTTP 500)

| Operation | Message |
|-----------|---------|
| DB_UPDATE | Failed to update database |
| BATCH_PROCESSING | Error processing batch request |

---

## 4. Data Types Reference

### 4.1 Owner/Driver Indicator (owner_driver_indicator)

| Code | Description |
|------|-------------|
| O | Vehicle Owner |
| H | Hirer |
| D | Driver |

### 4.2 ID Types

| Code | Description |
|------|-------------|
| NRIC | Singapore NRIC |
| FIN | Foreign Identification Number |
| UEN | Unique Entity Number (Company) |
| PASSPORT | Passport Number |

### 4.3 Non-Furnishable Reason Codes

| Code | Description |
|------|-------------|
| LAST_STAGE_AFTER_CPC | Processing stage is after CPC |
| PERMANENT_SUSPENSION | Notice has active permanent suspension |
| INVALID_STAGE | Notice is not at furnishable stage |

### 4.4 Address Object

| Field | Type | Max Length | Description |
|-------|------|------------|-------------|
| blockNo | String | 10 | House/Block number |
| streetName | String | 100 | Street name |
| buildingName | String | 100 | Building name (optional) |
| floorNo | String | 5 | Floor number |
| unitNo | String | 10 | Unit number |
| postalCode | String | 6 | Postal code |

---

## 5. Technical Standards

### 5.1 HTTP Method

All APIs use **POST** method only. No GET, PUT, PATCH, or DELETE allowed.

### 5.2 Audit User Fields

Database operations must use proper audit user:

| Zone | Audit User | Usage |
|------|------------|-------|
| Intranet | `ocmsiz_app_conn` | cre_user_id, upd_user_id for Intranet tables |
| Internet/PII | `ocmsez_app_conn` | cre_user_id, upd_user_id for PII tables |

**Note:** Never use "SYSTEM" as audit user.

### 5.3 SQL Query Best Practice

- **Do NOT use `SELECT *`** in any query
- Always specify only the required fields
- Example: `SELECT notice_no, vehicle_no, last_processing_stage FROM ocms_valid_offence_notice WHERE notice_no = ?`

### 5.4 Internet Sync & Retry Mechanism

For syncing to Internet/PII zone:
- **Retry**: 3 attempts on connection failure
- **Alert**: Email notification after all retries fail
- **Target Table**: `eocms_furnish_application` (NOT eocms_offence_notice_owner_driver)

### 5.5 Column Name Mapping (Data Dictionary)

| API Field | Database Column | Table |
|-----------|-----------------|-------|
| ownerDriverIndicator | owner_driver_indicator | OND |
| emailAddr | email_addr | OND |
| offenderTelNo | offender_tel_no | OND |
| lastProcessingStage | last_processing_stage | VON |
| offenderIndicator | offender_indicator | OND |

---

## Appendix A: Database Tables

### A.1 Tables Used

| Table | Zone | Purpose |
|-------|------|---------|
| ocms_valid_offence_notice (VON) | Intranet | Notice information |
| ocms_offence_notice_owner_driver (OND) | Intranet | Owner/Hirer/Driver records |
| ocms_offence_notice_owner_driver_addr (OND_ADDR) | Intranet | Address records |
| ocms_suspended_notice | Intranet | Suspension records |
| ocms_furnish_application (FA) | Intranet | Furnish application records |
| eocms_furnish_application (eFA) | Internet/PII | PII sync for furnish applications |

### A.2 Database Operations

#### Batch Furnish - Per Notice
```sql
-- Query notice (Intranet)
SELECT notice_no, vehicle_no, last_processing_stage
FROM ocms_valid_offence_notice
WHERE notice_no = ?

-- Insert/Update offender (Intranet)
-- cre_user_id/upd_user_id = 'ocmsiz_app_conn'
INSERT/UPDATE ocms_offence_notice_owner_driver
SET offender_indicator = 'Y', ...

-- Clear previous offender indicator
UPDATE ocms_offence_notice_owner_driver
SET offender_indicator = 'N', upd_user_id = 'ocmsiz_app_conn'
WHERE notice_no = ? AND offender_indicator = 'Y'

-- Sync to PII zone
-- cre_user_id/upd_user_id = 'ocmsez_app_conn'
INSERT INTO eocms_furnish_application (...)
```

#### Batch Update Address - Per Notice
```sql
-- Query offender (Intranet)
SELECT notice_no, id_no, id_type, name, email_addr, offender_tel_no, offender_indicator, owner_driver_indicator
FROM ocms_offence_notice_owner_driver
WHERE id_no = ? AND offender_indicator = 'Y'

-- Update address (Intranet)
-- upd_user_id = 'ocmsiz_app_conn'
UPDATE ocms_offence_notice_owner_driver_addr
SET block_no = ?, street_name = ?, ...
WHERE notice_no = ? AND owner_driver_indicator = ?
```

---

*Document generated for OCMS 41 Section 5 API planning*

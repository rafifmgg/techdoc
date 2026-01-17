# OCMS 41 Section 5 - Batch Furnish and Batch Update API Specification

## Document Information

| Field | Value |
|-------|-------|
| Version | 1.0 |
| Date | 2026-01-07 |
| Source | Functional Document v1.1, Section 5 |
| Module | OCMS 41 - Section 5: Batch Furnish and Batch Update |

---

## Table of Contents

1. [Overview](#1-overview)
2. [Internal APIs](#2-internal-apis)
3. [Error Codes Reference](#3-error-codes-reference)
4. [Data Types Reference](#4-data-types-reference)

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
| noticeList | Array | Yes | List of Notice numbers to check |

#### Response (Success)

```json
{
  "success": true,
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
  "offenderType": "HIRER",
  "idType": "NRIC",
  "idNo": "S1234567A",
  "name": "JOHN LEE",
  "email": "john.lee@email.com",
  "countryCode": "65",
  "contactNo": "91234567",
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

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| noticeList | Array | Yes | List of Notice numbers to furnish |
| offenderType | String | Yes | Role: OWNER, HIRER, DRIVER |
| idType | String | Yes | ID type: NRIC, FIN, UEN, PASSPORT |
| idNo | String | Yes | ID number |
| name | String | Yes | Full name |
| email | String | No | Email address |
| countryCode | String | No | Phone country code |
| contactNo | String | No | Contact number |
| address | Object | No | Mailing address |

#### Response (Success)

```json
{
  "success": true,
  "totalProcessed": 2,
  "successCount": 2,
  "failureCount": 0,
  "successRecords": [
    {
      "noticeNo": "500500303J",
      "offenderName": "JOHN LEE",
      "offenderId": "S1234567A",
      "offenderType": "HIRER",
      "newProcessingStage": "RD2"
    },
    {
      "noticeNo": "500500304K",
      "offenderName": "JOHN LEE",
      "offenderId": "S1234567A",
      "offenderType": "HIRER",
      "newProcessingStage": "RD1"
    }
  ],
  "failedRecords": []
}
```

#### Response (Partial Success)

```json
{
  "success": true,
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
```

---

### 2.3 Get Outstanding Notices by ID

**Endpoint:** `GET /notice/offender/outstanding/{idNo}`

**Description:** Retrieves all outstanding Notices where the person (by ID number) is the Current Offender. Excludes Notices with active Permanent Suspension.

**User Stories:** OCMS41.5.3

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| idNo | String | Yes | ID number to search |

#### Response (Success - Records Found)

```json
{
  "success": true,
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
      "offenderType": "HIRER",
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
      "contactNo": "91234567",
      "email": "john.lee@email.com"
    },
    {
      "noticeNo": "500500304K",
      "summonNo": "SUM123457",
      "vehicleNo": "SNC7392R",
      "offenceDateTime": "2025-07-20T10:15:00",
      "offenderType": "OWNER",
      "offenceRuleCode": "PV02",
      "amountPayable": 120.00,
      "lastProcessingStage": "OW",
      "mailingAddress": null,
      "contactNo": null,
      "email": null
    }
  ]
}
```

#### Response (No Records Found)

```json
{
  "success": true,
  "idNo": "S1234567A",
  "totalNotices": 0,
  "errorCode": "OCMS-4004",
  "errorMessage": "Offender not found"
}
```

---

### 2.4 Batch Update Mailing Address

**Endpoint:** `PATCH /notice/offender/batch/update-address`

**Description:** Updates mailing address for multiple Notices of the same offender.

**User Stories:** OCMS41.5.3

#### Request

```json
{
  "idNo": "S1234567A",
  "noticeList": [
    {
      "noticeNo": "500500303J",
      "offenderType": "HIRER"
    },
    {
      "noticeNo": "500500304K",
      "offenderType": "OWNER"
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
  "contactNo": "98765432",
  "email": "john.new@email.com"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| idNo | String | Yes | ID number of offender |
| noticeList | Array | Yes | List of notices with their offender types |
| newAddress | Object | Yes | New mailing address |
| contactNo | String | No | Updated contact number |
| email | String | No | Updated email address |

#### Response (Success)

```json
{
  "success": true,
  "totalProcessed": 2,
  "successCount": 2,
  "failureCount": 0,
  "successRecords": [
    {
      "noticeNo": "500500303J",
      "offenderType": "HIRER",
      "addressUpdated": true
    },
    {
      "noticeNo": "500500304K",
      "offenderType": "OWNER",
      "addressUpdated": true
    }
  ],
  "failedRecords": []
}
```

---

### 2.5 Get Latest Offender and Notice Details

**Endpoint:** `GET /notice/offender/details`

**Description:** Retrieves latest offender and notice details for result page display after batch operations.

**User Stories:** OCMS41.5.2, OCMS41.5.3

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| noticeNo | String | Yes | Notice number (can be multiple, comma-separated) |

#### Response (Success)

```json
{
  "success": true,
  "results": [
    {
      "noticeNo": "500500303J",
      "currentOffender": {
        "name": "JOHN LEE",
        "idNo": "S1234567A",
        "offenderType": "HIRER"
      },
      "currentProcessingStage": "RD2"
    }
  ]
}
```

---

## 3. Error Codes Reference

### 3.1 Validation Errors (HTTP 400)

| Error Type | Field | Message |
|------------|-------|---------|
| VALIDATION_ERROR | noticeList | Notice list is required |
| VALIDATION_ERROR | noticeList | Notice list cannot be empty |
| VALIDATION_ERROR | idNo | ID number is required |
| VALIDATION_ERROR | name | Name is required |
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

### 4.1 Offender Types

| Code | Description |
|------|-------------|
| OWNER | Vehicle Owner |
| HIRER | Hirer |
| DRIVER | Driver |

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

| Field | Type | Description |
|-------|------|-------------|
| blockNo | String | House/Block number |
| streetName | String | Street name |
| buildingName | String | Building name (optional) |
| floorNo | String | Floor number |
| unitNo | String | Unit number |
| postalCode | String | Postal code |

---

## Appendix A: Database Tables

### A.1 Tables Used

| Table | Zone | Purpose |
|-------|------|---------|
| ocms_valid_offence_notice | Intranet | Notice information |
| ocms_offence_notice_owner_driver | Intranet | Owner/Hirer/Driver records |
| ocms_offence_notice_owner_driver_addr | Intranet | Address records |
| ocms_suspended_notice | Intranet | Suspension records |
| eocms_offence_notice_owner_driver | Internet | Internet copy for eService |

---

*Document generated for OCMS 41 Section 5 API planning*

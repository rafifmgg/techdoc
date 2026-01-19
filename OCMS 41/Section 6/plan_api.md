# OCMS 41 Section 6 - PLUS Integration API Specification

## Document Information

| Field | Value |
|-------|-------|
| Version | 1.0 |
| Date | 2026-01-07 |
| Source | Functional Document v1.1, Section 6 |
| Module | OCMS 41 - Section 6: PLUS External System Integration |

---

## Table of Contents

1. [Overview](#1-overview)
2. [External APIs (PLUS Integration)](#2-external-apis-plus-integration)
3. [Error Codes Reference](#3-error-codes-reference)
4. [Data Types Reference](#4-data-types-reference)

---

## 1. Overview

Section 6 covers the integration between PLUS (external system) and OCMS for:
- **Update Hirer/Driver from PLUS** - PLM furnishes Hirer/Driver particulars via PLUS Staff Portal
- **Redirect from PLUS** - PLM redirects Notice to new Offender via PLUS Staff Portal

### 1.1 Actors

| Actor | Description |
|-------|-------------|
| PLM | PLUS Officer who performs furnish/redirect operations |

### 1.2 Integration Architecture

```
PLUS Staff Portal → PLUS Intranet Backend → OCMS Intranet Backend
```

### 1.3 API Base Path (External)

```
/api/v1/external/plus
```

### 1.4 Reference

> Refer to OCMS 41 Functional Document Section 4.6.2.2 - Backend Furnish-Redirect Check

---

## 2. External APIs (PLUS Integration)

### 2.1 Check Furnishability for PLUS

**Endpoint:** `POST /external/plus/check-furnishability`

**Description:** Checks whether a Notice can be furnished with Hirer/Driver by PLUS. Returns furnishability status and allowed offender types.

**User Stories:** OCMS41.6.2.1

#### Request

```json
{
  "noticeNo": "500500303J"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| noticeNo | String | Yes | Notice number to check |

#### Response (Furnishable)

```json
{
  "success": true,
  "noticeNo": "500500303J",
  "furnishable": true,
  "allowedOffenderTypes": ["HIRER", "DRIVER"],
  "currentProcessingStage": "OW",
  "reasonCode": null,
  "reasonMessage": null
}
```

#### Response (Not Furnishable)

```json
{
  "success": true,
  "noticeNo": "500500303J",
  "furnishable": false,
  "allowedOffenderTypes": [],
  "currentProcessingStage": "CPC",
  "reasonCode": "LAST_STAGE_AFTER_CPC",
  "reasonMessage": "Notice cannot be furnished because processing stage is after CPC"
}
```

---

### 2.2 Get Existing Hirer/Driver for PLUS

**Endpoint:** `GET /external/plus/offender/{noticeNo}`

**Description:** Retrieves existing Hirer or Driver particulars for a Notice. Used by PLUS to pre-populate the form.

**User Stories:** OCMS41.6.2.1

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| noticeNo | String | Yes | Notice number |

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| offenderType | String | Yes | HIRER or DRIVER |

#### Response (Existing Offender Found)

```json
{
  "success": true,
  "noticeNo": "500500303J",
  "existingOffender": {
    "offenderType": "HIRER",
    "name": "JOHN LEE",
    "idType": "NRIC",
    "idNo": "S1234567A",
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
}
```

#### Response (No Existing Offender)

```json
{
  "success": true,
  "noticeNo": "500500303J",
  "existingOffender": null
}
```

---

### 2.3 Furnish Hirer/Driver from PLUS

**Endpoint:** `POST /external/plus/furnish`

**Description:** Furnishes Hirer or Driver particulars for a Notice from PLUS. The address provided is saved as Mailing Address.

**User Stories:** OCMS41.6.2.1

#### Request

```json
{
  "noticeNo": "500500303J",
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
| noticeNo | String | Yes | Notice number |
| offenderType | String | Yes | HIRER or DRIVER |
| idType | String | Yes | ID type: NRIC, FIN, UEN, PASSPORT |
| idNo | String | Yes | ID number |
| name | String | Yes | Full name |
| email | String | No | Email address |
| countryCode | String | No | Phone country code |
| contactNo | String | No | Contact number |
| address | Object | Yes | Mailing address from PLUS |

#### Response (Success)

```json
{
  "success": true,
  "noticeNo": "500500303J",
  "offenderName": "JOHN LEE",
  "offenderId": "S1234567A",
  "offenderType": "HIRER",
  "newProcessingStage": "RD1",
  "nextPrintSchedule": "2026-01-08"
}
```

#### Response (Validation Error)

```json
{
  "success": false,
  "errorCode": "VALIDATION_ERROR",
  "errorMessage": "ID number is required",
  "field": "idNo"
}
```

---

### 2.4 Check Redirect Eligibility for PLUS

**Endpoint:** `POST /external/plus/check-redirect`

**Description:** Checks whether a Notice can be redirected to a new Offender by PLUS.

**User Stories:** OCMS41.6.3.1

#### Request

```json
{
  "noticeNo": "500500303J"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| noticeNo | String | Yes | Notice number to check |

#### Response (Redirect Allowed)

```json
{
  "success": true,
  "noticeNo": "500500303J",
  "redirectable": true,
  "allowedTargetTypes": ["OWNER", "HIRER", "DRIVER"],
  "currentOffender": {
    "offenderType": "OWNER",
    "name": "TAN AH KOW",
    "idNo": "S9876543B"
  },
  "reasonCode": null,
  "reasonMessage": null
}
```

#### Response (Redirect Not Allowed)

```json
{
  "success": true,
  "noticeNo": "500500303J",
  "redirectable": false,
  "allowedTargetTypes": [],
  "currentOffender": null,
  "reasonCode": "LAST_STAGE_AFTER_CPC",
  "reasonMessage": "Notice cannot be redirected because processing stage is after CPC"
}
```

---

### 2.5 Get All Offenders for Redirect

**Endpoint:** `GET /external/plus/offenders/{noticeNo}`

**Description:** Retrieves all existing Owner, Hirer, and Driver particulars for a Notice. Used by PLUS to display redirect options.

**User Stories:** OCMS41.6.3.1

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| noticeNo | String | Yes | Notice number |

#### Response (Success)

```json
{
  "success": true,
  "noticeNo": "500500303J",
  "currentOffender": "OWNER",
  "offenders": {
    "owner": {
      "name": "TAN AH KOW",
      "idType": "NRIC",
      "idNo": "S9876543B",
      "email": "tan@email.com",
      "contactNo": "91112222",
      "address": {
        "blockNo": "456",
        "streetName": "BUKIT TIMAH ROAD",
        "buildingName": null,
        "floorNo": "05",
        "unitNo": "123",
        "postalCode": "289628"
      }
    },
    "hirer": {
      "name": "JOHN LEE",
      "idType": "NRIC",
      "idNo": "S1234567A",
      "email": "john.lee@email.com",
      "contactNo": "91234567",
      "address": {
        "blockNo": "123",
        "streetName": "ORCHARD ROAD",
        "buildingName": "PLAZA TOWER",
        "floorNo": "01",
        "unitNo": "01",
        "postalCode": "238888"
      }
    },
    "driver": null
  }
}
```

---

### 2.6 Redirect Notice from PLUS

**Endpoint:** `POST /external/plus/redirect`

**Description:** Redirects a Notice to a new Offender from PLUS. The new offender becomes the Current Offender.

**User Stories:** OCMS41.6.3.1

#### Request

```json
{
  "noticeNo": "500500303J",
  "targetOffenderType": "HIRER",
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
| noticeNo | String | Yes | Notice number |
| targetOffenderType | String | Yes | Target: OWNER, HIRER, DRIVER |
| idType | String | Yes | ID type of new offender |
| idNo | String | Yes | ID number of new offender |
| name | String | Yes | Full name of new offender |
| email | String | No | Email address |
| countryCode | String | No | Phone country code |
| contactNo | String | No | Contact number |
| address | Object | Yes | Mailing address |

#### Response (Success)

```json
{
  "success": true,
  "noticeNo": "500500303J",
  "previousOffender": {
    "offenderType": "OWNER",
    "name": "TAN AH KOW",
    "idNo": "S9876543B"
  },
  "newOffender": {
    "offenderType": "HIRER",
    "name": "JOHN LEE",
    "idNo": "S1234567A"
  },
  "newProcessingStage": "RD1",
  "nextPrintSchedule": "2026-01-08"
}
```

---

## 3. Error Codes Reference

### 3.1 Validation Errors (HTTP 400)

| Error Type | Field | Message |
|------------|-------|---------|
| VALIDATION_ERROR | noticeNo | Notice number is required |
| VALIDATION_ERROR | idNo | ID number is required |
| VALIDATION_ERROR | name | Name is required |
| VALIDATION_ERROR | offenderType | Offender type is required |
| VALIDATION_ERROR | address | Address is required |
| VALIDATION_ERROR | idNo | Invalid NRIC format |
| VALIDATION_ERROR | email | Invalid email format |

### 3.2 Business Errors (HTTP 409)

| Reason | Message |
|--------|---------|
| NOT_FURNISHABLE | Notice cannot be furnished at current processing stage |
| NOT_REDIRECTABLE | Notice cannot be redirected at current processing stage |
| LAST_STAGE_AFTER_CPC | Notice processing stage is after CPC |
| NOTICE_NOT_FOUND | Notice not found |
| INVALID_OFFENDER_TYPE | Invalid offender type for current processing stage |
| SAME_OFFENDER | Cannot redirect to the same current offender |

### 3.3 Technical Errors (HTTP 500)

| Operation | Message |
|-----------|---------|
| DB_UPDATE | Failed to update database |
| EXTERNAL_API | Failed to communicate with external system |

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

### 4.3 Processing Stage Transitions (PLUS)

| Operation | From Stage | Offender Type | New Stage | NPS |
|-----------|------------|---------------|-----------|-----|
| Furnish | OW | HIRER | RD1 | Next Day |
| Furnish | OW | DRIVER | DN | Next Day |
| Furnish | RD1 | DRIVER | DN | Next Day |
| Redirect | Any | HIRER | RD1 | Next Day |
| Redirect | Any | DRIVER | DN | Next Day |

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

## Appendix A: Integration Notes

### A.1 Address Handling

- Address provided by PLUS is saved as **Mailing Address** only
- OCMS will retrieve **Registered Address** from MHA/DataHive separately
- Registered Address is used for Reminder Letter sending

### A.2 Offender Flag Transfer

For **Redirect** operation:
- Previous offender's `offender_indicator` is set to 'N'
- New offender's `offender_indicator` is set to 'Y'

For **Furnish** operation (when person is not current offender):
- This is optional based on business rules
- May transfer flag if explicitly requested

### A.3 Database Tables Used

| Table | Zone | Purpose |
|-------|------|---------|
| ocms_valid_offence_notice | Intranet | Notice information |
| ocms_offence_notice_owner_driver | Intranet | Owner/Hirer/Driver records |
| ocms_offence_notice_owner_driver_addr | Intranet | Address records |
| eocms_offence_notice_owner_driver | Internet | Internet copy for eService |

---

*Document generated for OCMS 41 Section 6 API planning*

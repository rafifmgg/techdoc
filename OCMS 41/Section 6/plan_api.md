# OCMS 41 Section 6 - PLUS Integration API Specification

## Document Information

| Field | Value |
|-------|-------|
| Version | 1.1 |
| Date | 2026-01-19 |
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

### 1.4 Audit User

| Zone | Audit User |
|------|------------|
| Intranet | ocmsiz_app_conn |
| Internet (PII) | ocmsez_app_conn |

### 1.5 Reference

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
  "data": {
    "appCode": "OCMS-2000",
    "message": "Check furnishability successful",
    "noticeNo": "500500303J",
    "furnishable": true,
    "allowedOffenderTypes": ["H", "D"],
    "currentProcessingStage": "OW"
  }
}
```

#### Response (Not Furnishable)

```json
{
  "data": {
    "appCode": "OCMS-4003",
    "message": "Notice cannot be furnished because processing stage is after CPC",
    "noticeNo": "500500303J",
    "furnishable": false,
    "allowedOffenderTypes": [],
    "currentProcessingStage": "CPC"
  }
}
```

---

### 2.2 Get Existing Hirer/Driver for PLUS

**Endpoint:** `POST /external/plus/get-offender`

**Description:** Retrieves existing Hirer or Driver particulars for a Notice. Used by PLUS to pre-populate the form.

**User Stories:** OCMS41.6.2.1

#### Request

```json
{
  "noticeNo": "500500303J",
  "ownerDriverIndicator": "H"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| noticeNo | String | Yes | Notice number |
| ownerDriverIndicator | String | Yes | H (Hirer) or D (Driver) |

#### Response (Existing Offender Found)

```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Get offender successful",
    "noticeNo": "500500303J",
    "existingOffender": {
      "ownerDriverIndicator": "H",
      "name": "JOHN LEE",
      "idType": "NRIC",
      "idNo": "S1234567A",
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
  }
}
```

#### Response (No Existing Offender)

```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "No existing offender found",
    "noticeNo": "500500303J",
    "existingOffender": null
  }
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

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| noticeNo | String | Yes | Notice number |
| ownerDriverIndicator | String | Yes | H (Hirer) or D (Driver) |
| idType | String | Yes | ID type: NRIC, FIN, UEN, PASSPORT |
| idNo | String | Yes | ID number |
| name | String(66) | Yes | Full name (max 66 characters) |
| emailAddr | String | No | Email address |
| countryCode | String | No | Phone country code |
| offenderTelNo | String | No | Contact number |
| address | Object | Yes | Mailing address from PLUS |

#### Response (Success)

```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Furnish successful",
    "noticeNo": "500500303J",
    "offenderName": "JOHN LEE",
    "offenderId": "S1234567A",
    "ownerDriverIndicator": "H",
    "newProcessingStage": "RD1",
    "nextPrintSchedule": "2026-01-08"
  }
}
```

#### Response (Validation Error)

```json
{
  "data": {
    "appCode": "OCMS-4001",
    "message": "ID number is required",
    "field": "idNo"
  }
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
  "data": {
    "appCode": "OCMS-2000",
    "message": "Check redirect successful",
    "noticeNo": "500500303J",
    "redirectable": true,
    "allowedTargetTypes": ["O", "H", "D"],
    "currentOffender": {
      "ownerDriverIndicator": "O",
      "name": "TAN AH KOW",
      "idNo": "S9876543B"
    }
  }
}
```

#### Response (Redirect Not Allowed)

```json
{
  "data": {
    "appCode": "OCMS-4004",
    "message": "Notice cannot be redirected because processing stage is after CPC",
    "noticeNo": "500500303J",
    "redirectable": false,
    "allowedTargetTypes": [],
    "currentOffender": null
  }
}
```

---

### 2.5 Get All Offenders for Redirect

**Endpoint:** `POST /external/plus/get-all-offenders`

**Description:** Retrieves all existing Owner, Hirer, and Driver particulars for a Notice. Used by PLUS to display redirect options.

**User Stories:** OCMS41.6.3.1

#### Request

```json
{
  "noticeNo": "500500303J"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| noticeNo | String | Yes | Notice number |

#### Response (Success)

```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Get all offenders successful",
    "noticeNo": "500500303J",
    "currentOffenderIndicator": "O",
    "offenders": {
      "owner": {
        "name": "TAN AH KOW",
        "idType": "NRIC",
        "idNo": "S9876543B",
        "emailAddr": "tan@email.com",
        "offenderTelNo": "91112222",
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
        "emailAddr": "john.lee@email.com",
        "offenderTelNo": "91234567",
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
  "targetOwnerDriverIndicator": "H",
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

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| noticeNo | String | Yes | Notice number |
| targetOwnerDriverIndicator | String | Yes | Target: O (Owner), H (Hirer), D (Driver) |
| idType | String | Yes | ID type of new offender |
| idNo | String | Yes | ID number of new offender |
| name | String(66) | Yes | Full name of new offender (max 66 characters) |
| emailAddr | String | No | Email address |
| countryCode | String | No | Phone country code |
| offenderTelNo | String | No | Contact number |
| address | Object | Yes | Mailing address |

#### Response (Success)

```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Redirect successful",
    "noticeNo": "500500303J",
    "previousOffender": {
      "ownerDriverIndicator": "O",
      "name": "TAN AH KOW",
      "idNo": "S9876543B"
    },
    "newOffender": {
      "ownerDriverIndicator": "H",
      "name": "JOHN LEE",
      "idNo": "S1234567A"
    },
    "newProcessingStage": "RD1",
    "nextPrintSchedule": "2026-01-08"
  }
}
```

---

## 3. Error Codes Reference

### 3.1 Success Codes

| App Code | Message |
|----------|---------|
| OCMS-2000 | Operation successful |

### 3.2 Validation Errors (HTTP 400)

| App Code | Field | Message |
|----------|-------|---------|
| OCMS-4001 | noticeNo | Notice number is required |
| OCMS-4001 | idNo | ID number is required |
| OCMS-4001 | name | Name is required |
| OCMS-4001 | ownerDriverIndicator | Owner/Driver indicator is required |
| OCMS-4001 | address | Address is required |
| OCMS-4002 | idNo | Invalid NRIC format |
| OCMS-4002 | emailAddr | Invalid email format |

### 3.3 Business Errors (HTTP 409)

| App Code | Message |
|----------|---------|
| OCMS-4003 | Notice cannot be furnished at current processing stage |
| OCMS-4004 | Notice cannot be redirected at current processing stage |
| OCMS-4005 | Notice processing stage is after CPC |
| OCMS-4006 | Notice not found |
| OCMS-4007 | Invalid offender type for current processing stage |
| OCMS-4008 | Cannot redirect to the same current offender |
| OCMS-4009 | No current offender found |

### 3.4 Technical Errors (HTTP 500)

| App Code | Message |
|----------|---------|
| OCMS-5001 | Failed to update database |
| OCMS-5002 | Failed to communicate with external system |

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

### 4.3 Processing Stage Transitions (PLUS)

| Operation | From Stage | Offender Type | New Stage | NPS |
|-----------|------------|---------------|-----------|-----|
| Furnish | OW | H (Hirer) | RD1 | Next Day |
| Furnish | OW | D (Driver) | DN | Next Day |
| Furnish | RD1 | D (Driver) | DN | Next Day |
| Redirect | Any | H (Hirer) | RD1 | Next Day |
| Redirect | Any | D (Driver) | DN | Next Day |

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

### A.2 Offender Indicator Transfer

For **Redirect** operation:
- Previous offender's `offender_indicator` is set to 'N'
- New offender's `offender_indicator` is set to 'Y'

For **Furnish** operation (when person is not current offender):
- This is optional based on business rules
- May transfer flag if explicitly requested

### A.3 Database Tables Used

| Table | Zone | Audit User | Purpose |
|-------|------|------------|---------|
| ocms_valid_offence_notice | Intranet | ocmsiz_app_conn | Notice information |
| ocms_offence_notice_owner_driver | Intranet | ocmsiz_app_conn | Owner/Hirer/Driver records |
| ocms_offence_notice_owner_driver_addr | Intranet | ocmsiz_app_conn | Address records |
| ocms_furnish_application | Intranet | ocmsiz_app_conn | Furnish application records |
| eocms_furnish_application | Internet | ocmsez_app_conn | PII zone sync for eService |

---

*Document generated for OCMS 41 Section 6 API planning*

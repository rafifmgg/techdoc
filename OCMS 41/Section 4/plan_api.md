# OCMS 41 Section 4 - Staff Portal Manual Furnish/Update API Specification

## Document Information

| Field | Value |
|-------|-------|
| Version | 1.0 |
| Date | 2026-01-07 |
| Source | Functional Document v1.1, Section 4 |
| Module | OCMS 41 - Section 4: Staff Portal Manual Furnish or Update Owner, Hirer or Driver Particulars |

---

## Table of Contents

1. [Overview](#1-overview)
2. [Internal APIs](#2-internal-apis)
3. [Error Codes Reference](#3-error-codes-reference)
4. [Data Types Reference](#4-data-types-reference)

---

## 1. Overview

Section 4 covers the Staff Portal functions for OIC to manually:
- **Furnish** - Add a new Owner, Hirer or Driver as the Current Offender
- **Redirect** - Transfer the Offender Indicator to another person/role
- **Update** - Modify particulars of an existing Current Offender

### 1.1 Actors

| Actor | Description |
|-------|-------------|
| OIC | Officer-In-Charge who manages offender particulars |

### 1.2 API Base Path

```
/api/v1/notice/offender
```

---

## 2. Internal APIs

### 2.1 Check Furnish/Redirect Eligibility

**Endpoint:** `POST /notice/offender/check-eligibility`

**Description:** Checks whether a Notice can be furnished or redirected.

**User Stories:** OCMS41.4.6.2.2, OCMS41.4.7.2.2

#### Request

```json
{
  "noticeNo": "500500303J",
  "offenderType": "HIRER"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| noticeNo | String | Yes | Notice number |
| offenderType | String | Yes | Role to check: OWNER, HIRER, DRIVER |

#### Response (Success - Can Furnish/Redirect)

```json
{
  "success": true,
  "canFurnish": true,
  "canRedirect": true,
  "currentStage": "RD1",
  "reasonCode": null,
  "reasonMessage": null
}
```

#### Response (Cannot Furnish/Redirect)

```json
{
  "success": true,
  "canFurnish": false,
  "canRedirect": false,
  "currentStage": "CPC",
  "reasonCode": "LAST_STAGE_AFTER_CPC",
  "reasonMessage": "Notice is at final processing stage"
}
```

---

### 2.2 Furnish Offender

**Endpoint:** `POST /notice/offender/furnish`

**Description:** Adds a new Owner, Hirer or Driver as the Current Offender. If an existing offender exists for the role, it will be overwritten.

**User Stories:** OCMS41.4.6.1-4.6.2.3

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
    "unitNo": "01-01",
    "postalCode": "238888"
  },
  "relationship": "Employee",
  "rentalPeriodFrom": "2025-01-01",
  "rentalPeriodTo": "2025-12-31",
  "remarks": "Furnished by OIC"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| noticeNo | String | Yes | Notice number |
| offenderType | String | Yes | Role: OWNER, HIRER, DRIVER |
| idType | String | Yes | ID type: NRIC, FIN, UEN, PASSPORT |
| idNo | String | Yes | ID number |
| name | String | Yes | Full name |
| email | String | No | Email address |
| countryCode | String | No | Phone country code |
| contactNo | String | No | Contact number |
| address | Object | No | Mailing address |
| relationship | String | No | Relationship to vehicle owner |
| rentalPeriodFrom | Date | No | Rental start date (for Hirer) |
| rentalPeriodTo | Date | No | Rental end date (for Hirer) |
| remarks | String | No | OIC remarks |

#### Response (Success)

```json
{
  "success": true,
  "noticeNo": "500500303J",
  "offenderType": "HIRER",
  "offenderRecordCreated": true,
  "offenderIndicatorSet": true,
  "processingStageUpdated": true,
  "newProcessingStage": "RD2",
  "message": "Hirer furnished successfully"
}
```

#### Response (Business Error)

```json
{
  "success": false,
  "errorType": "BUSINESS_ERROR",
  "reason": "NOT_FURNISHABLE",
  "message": "Notice is not at furnishable stage"
}
```

---

### 2.3 Redirect Notice

**Endpoint:** `POST /notice/offender/redirect`

**Description:** Transfers the Offender Indicator to another person/role and initiates processing for the new offender.

**User Stories:** OCMS41.4.7.1-4.7.2.3

#### Request

```json
{
  "noticeNo": "500500303J",
  "fromOffenderType": "OWNER",
  "toOffenderType": "HIRER",
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
    "unitNo": "01-01",
    "postalCode": "238888"
  },
  "remarks": "Redirected by OIC"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| noticeNo | String | Yes | Notice number |
| fromOffenderType | String | Yes | Current offender role |
| toOffenderType | String | Yes | Target offender role |
| idType | String | Yes | ID type of new offender |
| idNo | String | Yes | ID number of new offender |
| name | String | Yes | Full name of new offender |
| email | String | No | Email address |
| countryCode | String | No | Phone country code |
| contactNo | String | No | Contact number |
| address | Object | No | Mailing address |
| remarks | String | No | OIC remarks |

#### Response (Success)

```json
{
  "success": true,
  "noticeNo": "500500303J",
  "redirectedFrom": "OWNER",
  "redirectedTo": "HIRER",
  "previousOffenderCleared": true,
  "newOffenderCreated": true,
  "offenderIndicatorTransferred": true,
  "processingStageUpdated": true,
  "newProcessingStage": "RD1",
  "message": "Notice redirected successfully"
}
```

---

### 2.4 Update Offender Particulars

**Endpoint:** `PATCH /notice/offender/update`

**Description:** Updates the particulars of an existing Current Offender without changing the Offender Indicator.

**User Stories:** OCMS41.4.8.1-4.8.2.3

#### Request

```json
{
  "noticeNo": "500500303J",
  "offenderType": "HIRER",
  "name": "JOHN LEE UPDATED",
  "email": "john.lee.new@email.com",
  "countryCode": "65",
  "contactNo": "98765432",
  "address": {
    "blockNo": "456",
    "streetName": "MARINA BAY",
    "buildingName": "TOWER A",
    "unitNo": "02-02",
    "postalCode": "018989"
  },
  "remarks": "Address updated by OIC"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| noticeNo | String | Yes | Notice number |
| offenderType | String | Yes | Current offender role |
| name | String | No | Updated name |
| email | String | No | Updated email |
| countryCode | String | No | Updated country code |
| contactNo | String | No | Updated contact |
| address | Object | No | Updated address |
| remarks | String | No | OIC remarks |

#### Response (Success)

```json
{
  "success": true,
  "noticeNo": "500500303J",
  "offenderType": "HIRER",
  "particularsUpdated": true,
  "message": "Offender particulars updated successfully"
}
```

---

### 2.5 Get Notice Offender Details

**Endpoint:** `GET /notice/offender/{noticeNo}`

**Description:** Retrieves Owner, Hirer, and Driver details for a Notice.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| noticeNo | String | Yes | Notice number |

#### Response (Success)

```json
{
  "success": true,
  "noticeNo": "500500303J",
  "vehicleNo": "SNC7392R",
  "currentProcessingStage": "RD1",
  "owner": {
    "exists": true,
    "isCurrentOffender": false,
    "idType": "NRIC",
    "idNo": "S9876543B",
    "name": "VEHICLE OWNER PTE LTD",
    "email": "owner@company.com",
    "contactNo": "61234567",
    "address": {...}
  },
  "hirer": {
    "exists": true,
    "isCurrentOffender": true,
    "idType": "NRIC",
    "idNo": "S1234567A",
    "name": "JOHN LEE",
    "email": "john.lee@email.com",
    "contactNo": "91234567",
    "address": {...}
  },
  "driver": {
    "exists": false,
    "isCurrentOffender": false
  }
}
```

---

## 3. Error Codes Reference

### 3.1 Validation Errors (HTTP 400)

| Error Type | Field | Message |
|------------|-------|---------|
| VALIDATION_ERROR | noticeNo | Notice number is required |
| VALIDATION_ERROR | offenderType | Invalid offender type |
| VALIDATION_ERROR | idNo | ID number is required |
| VALIDATION_ERROR | name | Name is required |

### 3.2 Authentication Errors (HTTP 401)

| Reason | Message |
|--------|---------|
| JWT_INVALID | Invalid JWT token |
| API_KEY_INVALID | Invalid Auth Token |

### 3.3 Business Errors (HTTP 409)

| Reason | Message |
|--------|---------|
| NOT_FURNISHABLE | Notice is not at furnishable stage |
| LAST_STAGE_AFTER_CPC | Notice is at final processing stage |
| OFFENDER_NOT_FOUND | Offender record not found |
| NOT_CURRENT_OFFENDER | Cannot update - not the current offender |
| REDIRECT_SAME_ROLE | Cannot redirect to the same role |

### 3.4 Technical Errors (HTTP 500)

| Operation | Message |
|-----------|---------|
| DB_UPDATE | Failed to update database |
| SYNC_INTERNET | Failed to sync to Internet DB |

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

### 4.3 Processing Stages

| Stage | Description |
|-------|-------------|
| OW | Owner Stage |
| RD1 | Registered Driver Stage 1 |
| RD2 | Registered Driver Stage 2 |
| DN | Driver Named Stage |
| CPC | Court Processing Complete |

### 4.4 Action Button Display Rules

| Condition | FURNISH Button | REDIRECT Button | UPDATE Button |
|-----------|----------------|-----------------|---------------|
| No existing offender for role | Show | Hide | Hide |
| Offender exists, is Current Offender | Hide | Hide | Show |
| Offender exists, not Current Offender | Hide | Show | Hide |

---

## Appendix A: Database Tables

### A.1 Tables Used

| Table | Zone | Purpose |
|-------|------|---------|
| ocms_valid_offence_notice | Intranet | Notice information |
| ocms_offence_notice_owner_driver | Intranet | Owner/Hirer/Driver records |
| ocms_offence_notice_owner_driver_addr | Intranet | Address records |
| eocms_offence_notice_owner_driver | Internet | Internet copy for eService |

---

*Document generated for OCMS 41 Section 4 API planning*

# OCMS 41 Section 4 - Staff Portal Manual Furnish/Update API Specification

## Document Information

| Field | Value |
|-------|-------|
| Version | 1.2 |
| Date | 2026-01-19 |
| Source | Functional Document v1.1, Section 4 |
| Module | OCMS 41 - Section 4: Staff Portal Manual Furnish or Update Owner, Hirer or Driver Particulars |

---

## Table of Contents

1. [Overview](#1-overview)
2. [Internal APIs](#2-internal-apis)
3. [Error Codes Reference](#3-error-codes-reference)
4. [Data Types Reference](#4-data-types-reference)
5. [Database Specification](#5-database-specification)

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

### 1.3 API Standards

| Standard | Rule |
|----------|------|
| HTTP Method | **POST only** for all APIs |
| Response Format | `{ "data": { "appCode", "message", ... } }` |
| Sensitive Data | No sensitive data in URL |
| SQL Query | No `SELECT *` - specify only required fields |

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
  "offenderType": "H"
}
```

| Field | Type | Required | Max Length | Description | Source |
|-------|------|----------|------------|-------------|--------|
| noticeNo | VARCHAR | Yes | 10 | Notice number | User input |
| offenderType | VARCHAR | Yes | 1 | Role to check: O (Owner), H (Hirer), D (Driver) | User input |

#### Response (Success - Can Furnish/Redirect)

```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Eligibility check successful",
    "noticeNo": "500500303J",
    "canFurnish": true,
    "canRedirect": true,
    "currentStage": "RD1",
    "reasonCode": null,
    "reasonMessage": null
  }
}
```

| Field | Type | Description | Source |
|-------|------|-------------|--------|
| appCode | VARCHAR(20) | Application response code | System generated |
| message | VARCHAR(200) | Response message | System generated |
| noticeNo | VARCHAR(10) | Notice number | Request input |
| canFurnish | BOOLEAN | Whether notice can be furnished | Calculated from VON.last_processing_stage |
| canRedirect | BOOLEAN | Whether notice can be redirected | Calculated from VON.last_processing_stage |
| currentStage | VARCHAR(3) | Current processing stage | VON.last_processing_stage |
| reasonCode | VARCHAR(50) | Reason code if not eligible | System generated |
| reasonMessage | VARCHAR(200) | Reason message if not eligible | System generated |

#### Response (Cannot Furnish/Redirect)

```json
{
  "data": {
    "appCode": "OCMS-4001",
    "message": "Notice is not eligible for furnish/redirect",
    "noticeNo": "500500303J",
    "canFurnish": false,
    "canRedirect": false,
    "currentStage": "CPC",
    "reasonCode": "LAST_STAGE_AFTER_CPC",
    "reasonMessage": "Notice is at final processing stage"
  }
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
  "ownerDriverIndicator": "H",
  "idType": "N",
  "idNo": "S1234567A",
  "name": "JOHN LEE",
  "emailAddr": "john.lee@email.com",
  "telCode": "65",
  "telNo": "91234567",
  "address": {
    "blkHseNo": "123",
    "streetName": "ORCHARD ROAD",
    "bldgName": "PLAZA TOWER",
    "floorNo": "01",
    "unitNo": "01",
    "postalCode": "238888"
  },
  "hirerOwnerRelationship": "L",
  "othersRelationshipDesc": null,
  "rentalPeriodFrom": "2025-01-01",
  "rentalPeriodTo": "2025-12-31",
  "remarks": "Furnished by OIC"
}
```

| Field | Type | Required | Max Length | Nullable | Description | Source | Maps To |
|-------|------|----------|------------|----------|-------------|--------|---------|
| noticeNo | VARCHAR | Yes | 10 | NOT NULL | Notice number | User input | OND.notice_no |
| ownerDriverIndicator | VARCHAR | Yes | 1 | NOT NULL | Role: O (Owner), H (Hirer), D (Driver) | User input | OND.owner_driver_indicator |
| idType | VARCHAR | Yes | 1 | NOT NULL | ID type: N (NRIC), F (FIN), U (UEN), P (Passport) | User input | OND.id_type |
| idNo | VARCHAR | Yes | 12 | NOT NULL | ID number | User input | OND.id_no |
| name | VARCHAR | Yes | 66 | NOT NULL | Full name | User input | OND.name |
| emailAddr | VARCHAR | No | 320 | NULL | Email address | User input | OND.email_addr |
| telCode | VARCHAR | No | 3 | NULL | Phone country code | User input | OND.offender_tel_code |
| telNo | VARCHAR | No | 12 | NULL | Contact number | User input | OND.offender_tel_no |
| address | Object | No | - | NULL | Mailing address | User input | OND_ADDR table |
| address.blkHseNo | VARCHAR | No | 10 | NULL | Block/house number | User input | OND_ADDR.blk_hse_no |
| address.streetName | VARCHAR | No | 32 | NULL | Street name | User input | OND_ADDR.street_name |
| address.bldgName | VARCHAR | No | 65 | NULL | Building name | User input | OND_ADDR.bldg_name |
| address.floorNo | VARCHAR | No | 3 | NULL | Floor number | User input | OND_ADDR.floor_no |
| address.unitNo | VARCHAR | No | 5 | NULL | Unit number | User input | OND_ADDR.unit_no |
| address.postalCode | VARCHAR | No | 6 | NULL | Postal code | User input | OND_ADDR.postal_code |
| hirerOwnerRelationship | VARCHAR | Yes* | 1 | NOT NULL | Relationship code (*required for Hirer) | User input | FA.hirer_owner_relationship |
| othersRelationshipDesc | VARCHAR | No | 15 | NULL | Description if relationship='O' (Others) | User input | FA.others_relationship_desc |
| rentalPeriodFrom | DATETIME | No | - | NULL | Rental start date (for Hirer with relationship='L') | User input | FA.rental_period_from |
| rentalPeriodTo | DATETIME | No | - | NULL | Rental end date (for Hirer with relationship='L') | User input | FA.rental_period_to |
| remarks | VARCHAR | No | 200 | NULL | OIC remarks | User input | FA.remarks |

**Note:** FA = ocms_furnish_application table

#### Response (Success)

```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Hirer furnished successfully",
    "noticeNo": "500500303J",
    "ownerDriverIndicator": "H",
    "newProcessingStage": "RD2"
  }
}
```

| Field | Type | Description | Source |
|-------|------|-------------|--------|
| appCode | VARCHAR(20) | Application response code | System generated |
| message | VARCHAR(200) | Success message | System generated |
| noticeNo | VARCHAR(10) | Notice number | Request input |
| ownerDriverIndicator | VARCHAR(1) | Offender type furnished | Request input |
| newProcessingStage | VARCHAR(3) | Updated processing stage | Calculated based on offender type |

#### Response (Business Error)

```json
{
  "data": {
    "appCode": "OCMS-4002",
    "message": "Notice is not at furnishable stage",
    "noticeNo": "500500303J",
    "reasonCode": "NOT_FURNISHABLE"
  }
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
  "fromOwnerDriverIndicator": "O",
  "toOwnerDriverIndicator": "H",
  "idType": "N",
  "idNo": "S1234567A",
  "name": "JOHN LEE",
  "emailAddr": "john.lee@email.com",
  "telCode": "65",
  "telNo": "91234567",
  "address": {
    "blkHseNo": "123",
    "streetName": "ORCHARD ROAD",
    "bldgName": "PLAZA TOWER",
    "floorNo": "01",
    "unitNo": "01",
    "postalCode": "238888"
  },
  "remarks": "Redirected by OIC"
}
```

| Field | Type | Required | Max Length | Nullable | Description | Source | Maps To |
|-------|------|----------|------------|----------|-------------|--------|---------|
| noticeNo | VARCHAR | Yes | 10 | NOT NULL | Notice number | User input | OND.notice_no |
| fromOwnerDriverIndicator | VARCHAR | Yes | 1 | NOT NULL | Current offender role | User input | OND.owner_driver_indicator (source) |
| toOwnerDriverIndicator | VARCHAR | Yes | 1 | NOT NULL | Target offender role | User input | OND.owner_driver_indicator (target) |
| idType | VARCHAR | Yes | 1 | NOT NULL | ID type of new offender | User input | OND.id_type |
| idNo | VARCHAR | Yes | 12 | NOT NULL | ID number of new offender | User input | OND.id_no |
| name | VARCHAR | Yes | 66 | NOT NULL | Full name of new offender | User input | OND.name |
| emailAddr | VARCHAR | No | 320 | NULL | Email address | User input | OND.email_addr |
| telCode | VARCHAR | No | 3 | NULL | Phone country code | User input | OND.offender_tel_code |
| telNo | VARCHAR | No | 12 | NULL | Contact number | User input | OND.offender_tel_no |
| address | Object | No | - | NULL | Mailing address | User input | OND_ADDR table |
| remarks | VARCHAR | No | 200 | NULL | OIC remarks | User input | FA.remarks |

#### Response (Success)

```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Notice redirected successfully",
    "noticeNo": "500500303J",
    "redirectedFrom": "O",
    "redirectedTo": "H",
    "newProcessingStage": "RD1"
  }
}
```

| Field | Type | Description | Source |
|-------|------|-------------|--------|
| appCode | VARCHAR(20) | Application response code | System generated |
| message | VARCHAR(200) | Success message | System generated |
| noticeNo | VARCHAR(10) | Notice number | Request input |
| redirectedFrom | VARCHAR(1) | Previous offender type | Request input |
| redirectedTo | VARCHAR(1) | New offender type | Request input |
| newProcessingStage | VARCHAR(3) | Reset processing stage | Calculated based on target offender type |

---

### 2.4 Update Offender Particulars

**Endpoint:** `POST /notice/offender/update`

**Description:** Updates the particulars of an existing Current Offender without changing the Offender Indicator.

**User Stories:** OCMS41.4.8.1-4.8.2.3

#### Request

```json
{
  "noticeNo": "500500303J",
  "ownerDriverIndicator": "H",
  "name": "JOHN LEE UPDATED",
  "emailAddr": "john.lee.new@email.com",
  "telCode": "65",
  "telNo": "98765432",
  "address": {
    "blkHseNo": "456",
    "streetName": "MARINA BAY",
    "bldgName": "TOWER A",
    "floorNo": "02",
    "unitNo": "02",
    "postalCode": "018989"
  },
  "remarks": "Address updated by OIC"
}
```

| Field | Type | Required | Max Length | Nullable | Description | Source | Maps To |
|-------|------|----------|------------|----------|-------------|--------|---------|
| noticeNo | VARCHAR | Yes | 10 | NOT NULL | Notice number | User input | OND.notice_no |
| ownerDriverIndicator | VARCHAR | Yes | 1 | NOT NULL | Current offender role | User input | OND.owner_driver_indicator |
| name | VARCHAR | No | 66 | NULL | Updated name | User input | OND.name |
| emailAddr | VARCHAR | No | 320 | NULL | Updated email | User input | OND.email_addr |
| telCode | VARCHAR | No | 3 | NULL | Updated country code | User input | OND.offender_tel_code |
| telNo | VARCHAR | No | 12 | NULL | Updated contact | User input | OND.offender_tel_no |
| address | Object | No | - | NULL | Updated address | User input | OND_ADDR table |
| remarks | VARCHAR | No | 200 | NULL | OIC remarks | User input | FA.remarks |

#### Response (Success)

```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Offender particulars updated successfully",
    "noticeNo": "500500303J",
    "ownerDriverIndicator": "H"
  }
}
```

| Field | Type | Description | Source |
|-------|------|-------------|--------|
| appCode | VARCHAR(20) | Application response code | System generated |
| message | VARCHAR(200) | Success message | System generated |
| noticeNo | VARCHAR(10) | Notice number | Request input |
| ownerDriverIndicator | VARCHAR(1) | Offender type updated | Request input |

---

### 2.5 Get Notice Offender Details

**Endpoint:** `POST /notice/offender/get-details`

**Description:** Retrieves Owner, Hirer, and Driver details for a Notice.

#### Request

```json
{
  "noticeNo": "500500303J"
}
```

| Field | Type | Required | Max Length | Description | Source |
|-------|------|----------|------------|-------------|--------|
| noticeNo | VARCHAR | Yes | 10 | Notice number | User input |

#### Response (Success)

```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Notice offender details retrieved",
    "noticeNo": "500500303J",
    "vehicleNo": "SNC7392R",
    "currentProcessingStage": "RD1",
    "owner": {
      "exists": true,
      "isCurrentOffender": false,
      "idType": "N",
      "idNo": "S9876543B",
      "name": "VEHICLE OWNER PTE LTD",
      "emailAddr": "owner@company.com",
      "telNo": "61234567",
      "address": {
        "blkHseNo": "100",
        "streetName": "MAIN STREET",
        "bldgName": "CORP BUILDING",
        "floorNo": "10",
        "unitNo": "01",
        "postalCode": "123456"
      }
    },
    "hirer": {
      "exists": true,
      "isCurrentOffender": true,
      "idType": "N",
      "idNo": "S1234567A",
      "name": "JOHN LEE",
      "emailAddr": "john.lee@email.com",
      "telNo": "91234567",
      "address": {
        "blkHseNo": "123",
        "streetName": "ORCHARD ROAD",
        "bldgName": "PLAZA TOWER",
        "floorNo": "01",
        "unitNo": "01",
        "postalCode": "238888"
      }
    },
    "driver": {
      "exists": false,
      "isCurrentOffender": false
    }
  }
}
```

| Field | Type | Description | Source |
|-------|------|-------------|--------|
| appCode | VARCHAR(20) | Application response code | System generated |
| message | VARCHAR(200) | Response message | System generated |
| noticeNo | VARCHAR(10) | Notice number | VON.notice_no |
| vehicleNo | VARCHAR(14) | Vehicle registration number | VON.vehicle_no |
| currentProcessingStage | VARCHAR(3) | Current processing stage | VON.last_processing_stage |
| owner.exists | BOOLEAN | Whether owner record exists | Calculated from OND where owner_driver_indicator='O' |
| owner.isCurrentOffender | BOOLEAN | Whether owner is current offender | OND.offender_indicator = 'Y' |
| owner.idType | VARCHAR(1) | Owner ID type | OND.id_type |
| owner.idNo | VARCHAR(12) | Owner ID number | OND.id_no |
| owner.name | VARCHAR(66) | Owner name | OND.name |
| owner.emailAddr | VARCHAR(320) | Owner email | OND.email_addr |
| owner.telNo | VARCHAR(12) | Owner contact number | OND.offender_tel_no |
| owner.address | Object | Owner address | OND_ADDR table (type_of_address='furnished_mail') |
| hirer.* | - | Same structure as owner | OND where owner_driver_indicator='H' |
| driver.* | - | Same structure as owner | OND where owner_driver_indicator='D' |

---

## 3. Error Codes Reference

### 3.1 Success Codes (HTTP 200)

| App Code | Message |
|----------|---------|
| OCMS-2000 | Success |

### 3.2 Validation Errors (HTTP 400)

```json
{
  "data": {
    "appCode": "OCMS-4000",
    "message": "Validation error: {field} - {error message}",
    "noticeNo": "500500303J"
  }
}
```

| App Code | Field | Message |
|----------|-------|---------|
| OCMS-4000 | noticeNo | Notice number is required |
| OCMS-4000 | ownerDriverIndicator | Invalid offender type |
| OCMS-4000 | idNo | ID number is required |
| OCMS-4000 | name | Name is required |

### 3.3 Authentication Errors (HTTP 401)

```json
{
  "data": {
    "appCode": "OCMS-4010",
    "message": "Authentication failed"
  }
}
```

| App Code | Reason | Message |
|----------|--------|---------|
| OCMS-4010 | JWT_INVALID | Invalid JWT token |
| OCMS-4011 | API_KEY_INVALID | Invalid Auth Token |

### 3.4 Business Errors (HTTP 409)

```json
{
  "data": {
    "appCode": "OCMS-4090",
    "message": "Business rule violation",
    "noticeNo": "500500303J",
    "reasonCode": "NOT_FURNISHABLE"
  }
}
```

| App Code | Reason Code | Message |
|----------|-------------|---------|
| OCMS-4001 | NOT_FURNISHABLE | Notice is not at furnishable stage |
| OCMS-4002 | LAST_STAGE_AFTER_CPC | Notice is at final processing stage |
| OCMS-4003 | OFFENDER_NOT_FOUND | Offender record not found |
| OCMS-4004 | NOT_CURRENT_OFFENDER | Cannot update - not the current offender |
| OCMS-4005 | REDIRECT_SAME_ROLE | Cannot redirect to the same role |

### 3.5 Technical Errors (HTTP 500)

```json
{
  "data": {
    "appCode": "OCMS-5000",
    "message": "Internal server error",
    "noticeNo": "500500303J"
  }
}
```

| App Code | Operation | Message |
|----------|-----------|---------|
| OCMS-5001 | DB_UPDATE | Failed to update database |
| OCMS-5002 | SYNC_INTERNET | Failed to sync to Internet DB after 3 retries |

---

## 4. Data Types Reference

### 4.1 Owner Driver Indicator Values

| Code | Description | Source |
|------|-------------|--------|
| O | Vehicle Owner | Standard Code: OWNER_DRIVER_IND |
| H | Hirer | Standard Code: OWNER_DRIVER_IND |
| D | Driver | Standard Code: OWNER_DRIVER_IND |

### 4.2 ID Types

| Code | Description | Source |
|------|-------------|--------|
| N | Singapore NRIC | Standard Code: ID_TYPE |
| F | Foreign Identification Number (FIN) | Standard Code: ID_TYPE |
| U | Unique Entity Number (Company) | Standard Code: ID_TYPE |
| P | Passport Number | Standard Code: ID_TYPE |

### 4.3 Processing Stages

| Stage | Description | Source |
|-------|-------------|--------|
| OW | Owner Stage | Standard Code: PROCESSING_STAGE |
| RD1 | Registered Driver Stage 1 | Standard Code: PROCESSING_STAGE |
| RD2 | Registered Driver Stage 2 | Standard Code: PROCESSING_STAGE |
| DN | Driver Named Stage | Standard Code: PROCESSING_STAGE |
| CPC | Court Processing Complete | Standard Code: PROCESSING_STAGE |

### 4.4 Hirer-Owner Relationship Codes

| Code | Description | Source |
|------|-------------|--------|
| E | Employee | Standard Code: HIRER_OWNER_REL |
| L | Vehicle is Leased | Standard Code: HIRER_OWNER_REL |
| F | Family Member | Standard Code: HIRER_OWNER_REL |
| O | Others (requires othersRelationshipDesc) | Standard Code: HIRER_OWNER_REL |

### 4.5 Address Type Values

| Code | Description | Source |
|------|-------------|--------|
| mha_reg | MHA Registered Address | OND_ADDR.type_of_address |
| lta_reg | LTA Registered Address | OND_ADDR.type_of_address |
| lta_mail | LTA Mailing Address | OND_ADDR.type_of_address |
| furnished_mail | Furnished Mailing Address | OND_ADDR.type_of_address |

### 4.6 Action Button Display Rules

| Condition | FURNISH Button | REDIRECT Button | UPDATE Button |
|-----------|----------------|-----------------|---------------|
| No existing offender for role | Show | Hide | Hide |
| Offender exists, is Current Offender | Hide | Hide | Show |
| Offender exists, not Current Offender | Hide | Show | Hide |

---

## 5. Database Specification

### 5.1 Tables Used

| Table Alias | Full Table Name | Zone | Purpose |
|-------------|-----------------|------|---------|
| VON | ocms_valid_offence_notice | Intranet | Notice information |
| OND | ocms_offence_notice_owner_driver | Intranet | Owner/Hirer/Driver records |
| OND_ADDR | ocms_offence_notice_owner_driver_addr | Intranet | Address records |
| FA | ocms_furnish_application | Intranet | Furnish application details |
| eFA | eocms_furnish_application | PII | Internet copy of furnish application |

### 5.2 Audit User Configuration

| Zone | User ID for cre_user_id / upd_user_id |
|------|---------------------------------------|
| Intranet | `ocmsiz_app_conn` |
| Internet/PII | `ocmsez_app_conn` |

**Important:** Do NOT use "SYSTEM" for audit user fields.

### 5.3 SQL Query Best Practice

- **Do NOT use `SELECT *`** in any query
- Always specify only the fields required for the operation
- Example:
  ```sql
  -- CORRECT
  SELECT notice_no, last_processing_stage, vehicle_no
  FROM ocms_valid_offence_notice
  WHERE notice_no = :noticeNo

  -- INCORRECT
  SELECT * FROM ocms_valid_offence_notice WHERE notice_no = :noticeNo
  ```

### 5.4 Insert/Update Order

When performing database operations, follow this order:

1. **Parent table first:** `ocms_valid_offence_notice` (VON)
2. **Child table after:** `ocms_offence_notice_owner_driver` (OND)
3. **Address table:** `ocms_offence_notice_owner_driver_addr` (OND_ADDR)
4. **Furnish application:** `ocms_furnish_application` (FA)

### 5.5 Sync Flag Configuration

| Field | Table | Type | Default | Description |
|-------|-------|------|---------|-------------|
| is_sync | OND | varchar(1) | 'N' | Indicates if record needs sync to Internet |

**Sync Flag Values:**
| Value | Description |
|-------|-------------|
| Y | Sync successful |
| N | Pending sync / sync failed |

### 5.6 Internet Sync Retry Mechanism

When syncing to Internet database:

| Step | Action |
|------|--------|
| 1 | Attempt sync to Internet DB |
| 2 | If failed, retry up to **3 times** |
| 3 | If all retries fail, set `is_sync = 'N'` |
| 4 | Trigger **email alert** to support team |
| 5 | Cron job will pick up failed syncs for retry |

**Retry Configuration:**
| Parameter | Value |
|-----------|-------|
| Max Retries | 3 |
| Retry Interval | 5 seconds |
| Alert Email | Configured in parameter table |

---

## 5.7 Key Column Mappings

### VON (ocms_valid_offence_notice) Fields

| API Field | DB Column | Data Type | Nullable | Description |
|-----------|-----------|-----------|----------|-------------|
| noticeNo | notice_no | varchar(10) | NOT NULL | Primary key |
| vehicleNo | vehicle_no | varchar(14) | NOT NULL | Vehicle number |
| currentStage | last_processing_stage | varchar(3) | NOT NULL | Current processing stage |

### OND (ocms_offence_notice_owner_driver) Fields

| API Field | DB Column | Data Type | Nullable | Description |
|-----------|-----------|-----------|----------|-------------|
| noticeNo | notice_no | varchar(10) | NOT NULL | Primary key (composite) |
| ownerDriverIndicator | owner_driver_indicator | varchar(1) | NOT NULL | Primary key (composite): O/H/D |
| idType | id_type | varchar(1) | NULL | ID type code: N/F/U/P |
| idNo | id_no | varchar(12) | NOT NULL | ID number |
| name | name | varchar(66) | NULL | Full name |
| emailAddr | email_addr | varchar(320) | NULL | Email address |
| telCode | offender_tel_code | varchar(3) | NULL | Country code |
| telNo | offender_tel_no | varchar(12) | NULL | Contact number |
| - | offender_indicator | varchar(1) | NULL | Y = Current offender, N = Not current |
| - | is_sync | varchar(1) | NOT NULL | Sync status (default 'N') |
| - | cre_user_id | varchar(50) | NOT NULL | Created by user |
| - | cre_date | datetime2(7) | NOT NULL | Created timestamp |
| - | upd_user_id | varchar(50) | NULL | Updated by user |
| - | upd_date | datetime2(7) | NULL | Updated timestamp |

### OND_ADDR (ocms_offence_notice_owner_driver_addr) Fields

| API Field | DB Column | Data Type | Nullable | Description |
|-----------|-----------|-----------|----------|-------------|
| - | notice_no | varchar(10) | NOT NULL | Primary key (composite) |
| - | owner_driver_indicator | varchar(1) | NOT NULL | Primary key (composite) |
| - | type_of_address | varchar(20) | NOT NULL | Primary key (composite): mha_reg/lta_reg/lta_mail/furnished_mail |
| address.blkHseNo | blk_hse_no | varchar(10) | NULL | Block/house number |
| address.streetName | street_name | varchar(32) | NULL | Street name |
| address.bldgName | bldg_name | varchar(65) | NULL | Building name |
| address.floorNo | floor_no | varchar(3) | NULL | Floor number |
| address.unitNo | unit_no | varchar(5) | NULL | Unit number |
| address.postalCode | postal_code | varchar(6) | NULL | Postal code |

**Note:** For Staff Portal Manual Furnish, use `type_of_address = 'furnished_mail'`

### FA (ocms_furnish_application) Fields

| API Field | DB Column | Data Type | Nullable | Description |
|-----------|-----------|-----------|----------|-------------|
| - | txn_no | varchar(20) | NOT NULL | Primary key - unique submission reference |
| noticeNo | notice_no | varchar(10) | NOT NULL | Notice number |
| - | vehicle_no | varchar(14) | NOT NULL | Vehicle number |
| ownerDriverIndicator | owner_driver_indicator | varchar(1) | NOT NULL | O/H/D |
| name | furnish_name | varchar(66) | NOT NULL | Furnished person name |
| idType | furnish_id_type | varchar(1) | NOT NULL | ID type |
| idNo | furnish_id_no | varchar(12) | NOT NULL | ID number |
| address.blkHseNo | furnish_mail_blk_no | varchar(10) | NOT NULL | Block number |
| address.streetName | furnish_mail_street_name | varchar(32) | NOT NULL | Street name |
| address.bldgName | furnish_mail_bldg_name | varchar(65) | NULL | Building name |
| address.floorNo | furnish_mail_floor | varchar(3) | NULL | Floor number |
| address.unitNo | furnish_mail_unit_no | varchar(5) | NULL | Unit number |
| address.postalCode | furnish_mail_postal_code | varchar(6) | NOT NULL | Postal code |
| telCode | furnish_tel_code | varchar(3) | NULL | Country code |
| telNo | furnish_tel_no | varchar(12) | NULL | Contact number |
| emailAddr | furnish_email_addr | varchar(320) | NULL | Email address |
| hirerOwnerRelationship | hirer_owner_relationship | varchar(1) | NOT NULL | Relationship code: E/L/F/O |
| othersRelationshipDesc | others_relationship_desc | varchar(15) | NOT NULL | Description if relationship='O' |
| rentalPeriodFrom | rental_period_from | datetime2(7) | NULL | Rental start date |
| rentalPeriodTo | rental_period_to | datetime2(7) | NULL | Rental end date |
| remarks | remarks | varchar(200) | NULL | Remarks |
| - | status | varchar(1) | NOT NULL | P=Pending, A=Approved, R=Rejected |

---

*Document generated for OCMS 41 Section 4 API planning*
*Version 1.2 - Updated with Data Dictionary compliance*

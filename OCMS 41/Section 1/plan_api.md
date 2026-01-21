# OCMS 41 - Furnish Hirer/Driver API Specification

## Document Information

| Field | Value |
|-------|-------|
| Version | 1.0 |
| Date | 2026-01-07 |
| Source | Functional Document v1.1, Backend Code Analysis |
| Module | OCMS 41 - Furnish Hirer and Driver |

---

## Table of Contents

1. [Overview](#1-overview)
2. [Internal APIs - Furnish Submission](#2-internal-apis---furnish-submission)
3. [Internal APIs - Officer Dashboard](#3-internal-apis---officer-dashboard)
4. [Internal APIs - Officer Approval](#4-internal-apis---officer-approval)
5. [Internal APIs - Officer Rejection](#5-internal-apis---officer-rejection)
6. [Internal APIs - Manual Furnish](#6-internal-apis---manual-furnish)
7. [Cron APIs - Sync Furnished](#7-cron-apis---sync-furnished)
8. [Error Codes Reference](#8-error-codes-reference)
9. [Data Types Reference](#9-data-types-reference)

---

## 1. Overview

### 1.1 Base URLs

| Environment | Service | Base URL |
|-------------|---------|----------|
| Intranet | ocmsadminapi | `https://{intranet-host}/api/{version}` |
| Intranet | ocmsadmincron | `https://{intranet-host}/cron/{version}` |

### 1.2 Authentication

All APIs require JWT authentication via Authorization header:
```
Authorization: Bearer {jwt_token}
```

### 1.3 Common Headers

| Header | Required | Description |
|--------|----------|-------------|
| `Content-Type` | Yes | `application/json` |
| `Authorization` | Yes | `Bearer {jwt_token}` |
| `X-Request-ID` | No | Unique request identifier for tracing |

### 1.4 Audit User Configuration

| Environment | Database User | Usage |
|-------------|---------------|-------|
| Intranet | `ocmsiz_app_conn` | All cre_user_id and upd_user_id fields |
| Internet | `ocmsez_app_conn` | All cre_user_id and upd_user_id fields |

> **Important:** Do NOT use "SYSTEM" for audit user fields. Always use the database user above.

---

## 2. Internal APIs - Furnish Submission

### 2.1 Submit Furnish Application

Submit furnish hirer/driver application from eService portal.

| Field | Value |
|-------|-------|
| **Endpoint** | `POST /furnish/submit` |
| **User Story** | OCMS41.4-41.7 |
| **Actor** | eService Portal (System) |

#### Request Body

```json
{
  "txnNo": "TXN202601070001",
  "noticeNo": "500506201A",
  "vehicleNo": "SBA1234A",
  "offenceDate": "2026-01-05T14:30:00",
  "ppCode": "PP001",
  "ppName": "Toa Payoh Central Car Park",
  "furnishName": "John Tan Wei Ming",
  "furnishIdType": "NRIC",
  "furnishIdNo": "S1234567A",
  "furnishMailBlkNo": "12",
  "furnishMailFloor": "08",
  "furnishMailStreetName": "Toa Payoh Lorong 1",
  "furnishMailUnitNo": "01",
  "furnishMailBldgName": "Starlight Centre",
  "furnishMailPostalCode": "408732",
  "furnishTelCode": "65",
  "furnishTelNo": "91234567",
  "furnishEmailAddr": "john.tan@email.com",
  "ownerDriverIndicator": "D",
  "hirerOwnerRelationship": "EMPLOYEE",
  "othersRelationshipDesc": null,
  "quesOneAns": "Y",
  "quesTwoAns": "Y",
  "quesThreeAns": null,
  "rentalPeriodFrom": "2026-01-01T00:00:00",
  "rentalPeriodTo": "2026-12-31T23:59:59",
  "ownerName": "ABC Company Pte Ltd",
  "ownerIdNo": "201234567A",
  "ownerTelCode": "65",
  "ownerTelNo": "61234567",
  "ownerEmailAddr": "admin@abccompany.com",
  "corppassStaffName": "Mary Lim",
  "documentReferences": ["DOC001", "DOC002"]
}
```

#### Request Field Validation

> **Source:** Data Dictionary - `ocms_furnish_application` table

| Field | Type | Required | Validation | Max Length | DB Column |
|-------|------|----------|------------|------------|-----------|
| txnNo | String | Yes | Not blank | 20 | txn_no (PK) |
| noticeNo | String | Yes | Not blank, exists in DB | 10 | notice_no |
| vehicleNo | String | Yes | Not blank, matches notice | 14 | vehicle_no |
| offenceDate | DateTime | Yes | ISO 8601 format | - | offence_date |
| ppCode | String | Yes | Not blank | 5 | pp_code |
| ppName | String | Yes | Not blank | 100 | pp_name |
| lastProcessingStage | String | Yes | Valid stage code | 3 | last_processing_stage |
| furnishName | String | Yes | Not blank | 66 | furnish_name |
| furnishIdType | String | Yes | N=NRIC, F=FIN, P=Passport, U=UEN | 1 | furnish_id_type |
| furnishIdNo | String | Yes | Not blank | 12 | furnish_id_no |
| furnishMailBlkNo | String | Yes | Not blank | 10 | furnish_mail_blk_no |
| furnishMailFloor | String | No | - | 3 | furnish_mail_floor |
| furnishMailStreetName | String | Yes | Not blank | 32 | furnish_mail_street_name |
| furnishMailUnitNo | String | No | - | 5 | furnish_mail_unit_no |
| furnishMailBldgName | String | No | - | 65 | furnish_mail_bldg_name |
| furnishMailPostalCode | String | Yes | Not blank, 6 digits | 6 | furnish_mail_postal_code |
| furnishTelCountryCode | String | No | Country code | 4 | furnish_tel_country_code |
| furnishTelAreaCode | String | No | Area code | 3 | furnish_tel_area_code |
| furnishTelCode | String | No | Telephone code | 4 | furnish_tel_code |
| furnishTelNo | String | No | - | 12 | furnish_tel_no |
| furnishAltTelCountryCode | String | No | Alt country code | 10 | furnish_alt_tel_country_code |
| furnishAltTelAreaCode | String | No | Alt area code | 3 | furnish_alt_tel_area_code |
| furnishAltContactNo | String | No | Alt contact number | 12 | furnish_alt_contact_no |
| furnishEmailAddr | String | No | Email format | 320 | furnish_email_addr |
| ownerDriverIndicator | String | Yes | O=Owner, H=Hirer, D=Driver | 1 | owner_driver_indicator |
| hirerOwnerRelationship | String | Yes | Relationship code | 1 | hirer_owner_relationship |
| othersRelationshipDesc | String | No | If relationship=Others | 15 | others_relationship_desc |
| quesOneAns | String | Yes | Answer text | 32 | ques_one_ans |
| quesTwoAns | String | Yes | Answer text | 32 | ques_two_ans |
| quesThreeAns | String | No | Answer text | 32 | ques_three_ans |
| rentalPeriodFrom | DateTime | No | Rental start date | - | rental_period_from |
| rentalPeriodTo | DateTime | No | Rental end date | - | rental_period_to |
| status | String | Yes | P=Pending, A=Approved, R=Rejected | 1 | status |
| ownerName | String | Yes | Not blank | 66 | owner_name |
| ownerIdNo | String | Yes | Not blank | 12 | owner_id_no |
| ownerTelCode | String | No | Telephone code | 4 | owner_tel_code |
| ownerTelNo | String | No | - | 20 | owner_tel_no |
| ownerEmailAddr | String | No | Email format | 320 | owner_email_addr |
| corppassStaffName | String | No | Corppass staff name | 66 | corppass_staff_name |
| reasonForReview | String | No | Reason if pending | 255 | reason_for_review |
| remarks | String | No | Officer remarks | 200 | remarks |
| creDate | DateTime | Yes | Auto-generated | - | cre_date |
| creUserId | String | Yes | ocmsiz_app_conn / ocmsez_app_conn | 50 | cre_user_id |
| updDate | DateTime | No | Auto-updated | - | upd_date |
| updUserId | String | No | ocmsiz_app_conn / ocmsez_app_conn | 50 | upd_user_id |

#### Response - Success (Auto-Approved)

**HTTP Status: 200 OK**

```json
{
  "success": true,
  "txnNo": "TXN202601070001",
  "noticeNo": "500506201A",
  "status": "A",
  "autoApproved": true,
  "hirerDriverRecordCreated": true,
  "suspensionApplied": true,
  "message": "Furnish submission auto-approved successfully"
}
```

#### Response - Success (Pending Manual Review)

**HTTP Status: 200 OK**

```json
{
  "success": false,
  "errorType": "BUSINESS_ERROR",
  "checkType": "NOTICE_PERMANENTLY_SUSPENDED",
  "message": "Notice is permanently suspended. Submission requires manual review.",
  "requiresManualReview": true,
  "txnNo": "TXN202601070001",
  "noticeNo": "500506201A",
  "status": "P"
}
```

#### Response - Validation Error

**HTTP Status: 400 Bad Request**

```json
{
  "success": false,
  "errorType": "VALIDATION_ERROR",
  "field": "furnishIdNo",
  "message": "Furnished person ID number is required",
  "violations": [
    "furnishIdNo: Furnished person ID number is required",
    "furnishMailPostalCode: Postal code is required"
  ]
}
```

#### Response - Technical Error

**HTTP Status: 500 Internal Server Error**

```json
{
  "success": false,
  "errorType": "TECHNICAL_ERROR",
  "operation": "CREATE_FURNISH_APPLICATION",
  "message": "Failed to create furnish application record",
  "cause": "DataIntegrityViolationException",
  "details": {
    "table": "ocms_furnish_application",
    "constraint": "pk_furnish_application"
  }
}
```

---

## 3. Internal APIs - Officer Dashboard

### 3.1 List Furnish Applications

List furnish applications with optional filters for officer dashboard.

| Field | Value |
|-------|-------|
| **Endpoint** | `POST /furnish/officer/list` |
| **User Story** | OCMS41.9-41.12 |
| **Actor** | OIC (Officer-in-Charge) |

#### Request Body

```json
{
  "status": "P",
  "noticeNo": null,
  "vehicleNo": null,
  "furnishIdNo": null,
  "ownerDriverIndicator": null,
  "dateFrom": "2026-01-01",
  "dateTo": "2026-01-31",
  "page": 0,
  "size": 20,
  "sortBy": "creDate",
  "sortDirection": "DESC"
}
```

#### Request Parameters

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| status | String | No | Filter by status (P/A/R) |
| noticeNo | String | No | Filter by notice number |
| vehicleNo | String | No | Filter by vehicle number |
| furnishIdNo | String | No | Filter by furnished person ID |
| ownerDriverIndicator | String | No | Filter by type (H/D) |
| dateFrom | Date | No | Submission date from |
| dateTo | Date | No | Submission date to |
| page | Integer | No | Page number (default: 0) |
| size | Integer | No | Page size (default: 20) |
| sortBy | String | No | Sort field (default: creDate) |
| sortDirection | String | No | ASC or DESC (default: DESC) |

#### Response - Success

**HTTP Status: 200 OK**

```json
{
  "total": 150,
  "limit": 20,
  "skip": 0,
  "data": [
    {
      "txnNo": "TXN202601070001",           // Source: ocms_furnish_application.txn_no
      "noticeNo": "500506201A",             // Source: ocms_furnish_application.notice_no
      "vehicleNo": "SBA1234A",              // Source: ocms_furnish_application.vehicle_no
      "offenceDate": "2026-01-05T14:30:00", // Source: ocms_furnish_application.offence_date
      "ppName": "Toa Payoh Central Car Park", // Source: ocms_furnish_application.pp_name
      "furnishName": "John Tan Wei Ming",   // Source: ocms_furnish_application.furnish_name
      "furnishIdNo": "S1234567A",           // Source: ocms_furnish_application.furnish_id_no
      "ownerDriverIndicator": "D",          // Source: ocms_furnish_application.owner_driver_indicator
      "status": "P",                        // Source: ocms_furnish_application.status
      "reasonForReview": "Notice is permanently suspended", // Source: ocms_furnish_application.reason_for_review
      "creDate": "2026-01-07T09:00:00"      // Source: ocms_furnish_application.cre_date
    }
  ]
}
```

---

### 3.2 Get Application Detail

Get detailed view of a furnish application.

| Field | Value |
|-------|-------|
| **Endpoint** | `POST /furnish/officer/detail` |
| **User Story** | OCMS41.13-41.14 |
| **Actor** | OIC |

#### Request Body

```json
{
  "txnNo": "TXN202601070001"
}
```

#### Request Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| txnNo | String | Yes | Transaction number |

#### Response - Success

**HTTP Status: 200 OK**

```json
{
  "success": true,
  "data": {
    "txnNo": "TXN202601070001",
    "noticeNo": "500506201A",
    "vehicleNo": "SBA1234A",
    "offenceDate": "2026-01-05T14:30:00",
    "ppCode": "PP001",
    "ppName": "Toa Payoh Central Car Park",
    "lastProcessingStage": "RD1",
    "furnishName": "John Tan Wei Ming",
    "furnishIdType": "NRIC",
    "furnishIdNo": "S1234567A",
    "furnishMailBlkNo": "12",
    "furnishMailFloor": "08",
    "furnishMailStreetName": "Toa Payoh Lorong 1",
    "furnishMailUnitNo": "01",
    "furnishMailBldgName": "Starlight Centre",
    "furnishMailPostalCode": "408732",
    "furnishTelCode": "65",
    "furnishTelNo": "91234567",
    "furnishEmailAddr": "john.tan@email.com",
    "ownerDriverIndicator": "D",
    "hirerOwnerRelationship": "EMPLOYEE",
    "ownerName": "ABC Company Pte Ltd",
    "ownerIdNo": "201234567A",
    "ownerTelCode": "65",
    "ownerTelNo": "61234567",
    "ownerEmailAddr": "admin@abccompany.com",
    "status": "P",
    "reasonForReview": "Notice is permanently suspended",
    "remarks": null,
    "creDate": "2026-01-07T09:00:00",
    "documents": [
      {
        "attachmentId": 1,
        "docName": "rental_agreement.pdf",
        "mime": "application/pdf",
        "size": 1024567
      }
    ]
  }
}
```

#### Response - Not Found

**HTTP Status: 404 Not Found**

```json
{
  "success": false,
  "errorType": "NOT_FOUND",
  "message": "Furnish application not found: TXN202601070001"
}
```

---

## 4. Internal APIs - Officer Approval

### 4.1 Approve Furnish Application

Approve a pending furnish application.

| Field | Value |
|-------|-------|
| **Endpoint** | `POST /furnish/officer/approve` |
| **User Story** | OCMS41.15-41.23 |
| **Actor** | OIC |

#### Request Body

```json
{
  "txnNo": "TXN202601070001",
  "officerId": "OIC001",
  "remarks": "Approved after verification",
  "sendEmailToOwner": true,
  "sendEmailToFurnished": true,
  "sendSmsToFurnished": false,
  "updatedOwnerTelCode": "65",
  "updatedOwnerTelNo": "91234567",
  "updatedOwnerEmailAddr": "owner.updated@email.com"
}
```

#### Request Parameters

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| txnNo | String | Yes | Transaction number |
| officerId | String | Yes | Officer ID |
| remarks | String | No | Approval remarks (max 200 chars) |
| sendEmailToOwner | Boolean | No | Send email to owner (default: false) |
| sendEmailToFurnished | Boolean | No | Send email to furnished person (default: false) |
| sendSmsToFurnished | Boolean | No | Send SMS to furnished person (default: false) |
| updatedOwnerTelCode | String | No | Updated owner telephone country code |
| updatedOwnerTelNo | String | No | Updated owner telephone number |
| updatedOwnerEmailAddr | String | No | Updated owner email address |

#### Response - Success

**HTTP Status: 200 OK**

```json
{
  "success": true,
  "txnNo": "TXN202601070001",
  "noticeNo": "500506201A",
  "hirerDriverRecordUpdated": true,
  "suspensionRevived": true,
  "emailSentToOwner": true,
  "emailSentToFurnished": true,
  "smsSentToFurnished": false,
  "message": "Furnish application approved successfully"
}
```

#### Response - Business Error

**HTTP Status: 409 Conflict**

```json
{
  "success": false,
  "errorType": "BUSINESS_ERROR",
  "reason": "ALREADY_PROCESSED",
  "message": "Furnish application has already been processed"
}
```

---

## 5. Internal APIs - Officer Rejection

### 5.1 Reject Furnish Application

Reject a pending furnish application.

| Field | Value |
|-------|-------|
| **Endpoint** | `POST /furnish/officer/reject` |
| **User Story** | OCMS41.24-41.33 |
| **Actor** | OIC |

#### Request Body

```json
{
  "txnNo": "TXN202601070001",
  "officerId": "OIC001",
  "remarks": "Documents not clear, please resubmit",
  "sendEmailToOwner": true
}
```

#### Request Parameters

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| txnNo | String | Yes | Transaction number |
| officerId | String | Yes | Officer ID |
| remarks | String | No | Rejection remarks (max 200 chars) |
| sendEmailToOwner | Boolean | No | Send email to owner (default: false) |

#### Response - Success

**HTTP Status: 200 OK**

```json
{
  "success": true,
  "txnNo": "TXN202601070001",
  "noticeNo": "500506201A",
  "emailSentToOwner": true,
  "noticeResentToPortal": true,
  "message": "Furnish application rejected successfully"
}
```

---

## 6. Internal APIs - Manual Furnish

### 6.1 Check Notice Furnishable

Check if a notice is at furnishable processing stage.

| Field | Value |
|-------|-------|
| **Endpoint** | `POST /furnish/officer/manual/check-furnishable` |
| **User Story** | OCMS41.43 |
| **Actor** | OIC |

#### Request Body

```json
{
  "noticeNo": "500506201A"
}
```

#### Request Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| noticeNo | String | Yes | Notice number |

#### Response - Furnishable

**HTTP Status: 200 OK**

```json
{
  "success": true,
  "data": {
    "noticeNo": "500506201A",
    "isFurnishable": true,
    "currentProcessingStage": "RD1",
    "reason": null
  }
}
```

#### Response - Not Furnishable

**HTTP Status: 200 OK**

```json
{
  "success": true,
  "data": {
    "noticeNo": "500506201A",
    "isFurnishable": false,
    "currentProcessingStage": "CPC",
    "reason": "Notice is at stage 'CPC', which is not furnishable (only stages up to CPC are furnishable)"
  }
}
```

---

### 6.2 Check Existing Particulars

Check existing owner/hirer/driver particulars for a notice.

| Field | Value |
|-------|-------|
| **Endpoint** | `POST /furnish/officer/manual/check-existing` |
| **User Story** | OCMS41.44-41.45 |
| **Actor** | OIC |

#### Request Body

```json
{
  "noticeNo": "500506201A"
}
```

#### Request Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| noticeNo | String | Yes | Notice number |

#### Response - Success

**HTTP Status: 200 OK**

```json
{
  "success": true,
  "data": {
    "noticeNo": "500506201A",
    "hasExistingParticulars": true,
    "existingParticulars": [
      {
        "ownerDriverIndicator": "O",
        "idType": "UEN",
        "idNo": "201234567A",
        "name": "ABC Company Pte Ltd",
        "currentOffenderIndicator": "Y",
        "blkNo": "10",
        "floor": "05",
        "streetName": "Orchard Road",
        "unitNo": "01",
        "bldgName": "ABC Building",
        "postalCode": "238888",
        "telCode": "65",
        "telNo": "61234567",
        "emailAddr": "admin@abccompany.com"
      },
      {
        "ownerDriverIndicator": "H",
        "idType": "NRIC",
        "idNo": "S9876543B",
        "name": "Jane Lee",
        "currentOffenderIndicator": "N",
        "blkNo": "20",
        "floor": null,
        "streetName": "Tampines Ave 1",
        "unitNo": null,
        "bldgName": null,
        "postalCode": "528888",
        "telCode": "65",
        "telNo": "81234567",
        "emailAddr": "jane.lee@email.com"
      }
    ]
  }
}
```

---

### 6.3 Manual Furnish Single Notice

Manually furnish hirer/driver for a single notice.

| Field | Value |
|-------|-------|
| **Endpoint** | `POST /furnish/officer/manual/single` |
| **User Story** | OCMS41.46-41.48 |
| **Actor** | OIC |

#### Request Body

```json
{
  "noticeNo": "500506201A",
  "ownerDriverIndicator": "D",
  "idType": "NRIC",
  "idNo": "S1234567A",
  "name": "John Tan Wei Ming",
  "telCode": "65",
  "telNo": "91234567",
  "emailAddr": "john.tan@email.com",
  "blkNo": "12",
  "floor": "08",
  "streetName": "Toa Payoh Lorong 1",
  "unitNo": "01",
  "bldgName": "Starlight Centre",
  "postalCode": "408732",
  "officerId": "OIC001"
}
```

#### Response - Success

**HTTP Status: 200 OK**

```json
{
  "success": true,
  "noticeNo": "500506201A",
  "ownerDriverIndicator": "D",
  "idNo": "S1234567A",
  "name": "John Tan Wei Ming",
  "recordUpdated": true,
  "message": "Driver particulars furnished successfully"
}
```

#### Response - Business Error

**HTTP Status: 409 Conflict**

```json
{
  "success": false,
  "errorType": "BUSINESS_ERROR",
  "reason": "NOT_FURNISHABLE",
  "message": "Notice is not at furnishable stage"
}
```

---

### 6.4 Bulk Furnish Multiple Notices

Furnish hirer/driver for multiple notices in bulk.

| Field | Value |
|-------|-------|
| **Endpoint** | `POST /furnish/officer/manual/bulk` |
| **User Story** | OCMS41.50-41.51 |
| **Actor** | OIC |

#### Request Body

```json
{
  "noticeNos": ["500506201A", "500506202B", "500506203C"],
  "ownerDriverIndicator": "D",
  "idType": "NRIC",
  "idNo": "S1234567A",
  "name": "John Tan Wei Ming",
  "telCode": "65",
  "telNo": "91234567",
  "emailAddr": "john.tan@email.com",
  "blkNo": "12",
  "floor": "08",
  "streetName": "Toa Payoh Lorong 1",
  "unitNo": "01",
  "bldgName": "Starlight Centre",
  "postalCode": "408732",
  "officerId": "OIC001"
}
```

#### Response - Success (Partial)

**HTTP Status: 200 OK**

```json
{
  "success": true,
  "totalNotices": 3,
  "successCount": 2,
  "failedCount": 1,
  "successNotices": ["500506201A", "500506202B"],
  "failedNotices": [
    {
      "noticeNo": "500506203C",
      "reason": "Notice is permanently suspended"
    }
  ],
  "message": "Bulk furnish completed: 2 success, 1 failed"
}
```

---

## 7. Cron APIs - Sync Furnished

### 7.1 Execute Furnished Sync

Manually trigger synchronization of furnished submissions from Internet DB to Intranet DB.

| Field | Value |
|-------|-------|
| **Endpoint** | `POST /ocms41/sync-furnished/execute` |
| **Service** | ocmsadmincron |
| **Actor** | System/Admin |

#### Response - Success

**HTTP Status: 200 OK**

```json
{
  "success": true,
  "message": "OCMS 41 furnished sync completed successfully",
  "description": "OCMS 41: Sync furnished submissions from Internet DB to Intranet DB"
}
```

#### Response - Disabled

**HTTP Status: 200 OK**

```json
{
  "success": false,
  "enabled": false,
  "message": "OCMS 41 furnished sync is disabled. Enable it by setting ocms41.sync.enabled=true"
}
```

#### Response - Error

**HTTP Status: 500 Internal Server Error**

```json
{
  "success": false,
  "error": "Error executing OCMS 41 furnished sync: Connection timeout"
}
```

---

## 8. Error Codes Reference

### 8.1 Error Types

| Error Type | HTTP Status | Description |
|------------|-------------|-------------|
| `VALIDATION_ERROR` | 400 | Request validation failed (bean/business) |
| `BUSINESS_ERROR` | 200/409 | Business rule check failed |
| `TECHNICAL_ERROR` | 500 | System/database error |
| `NOT_FOUND` | 404 | Resource not found |

### 8.2 Auto-Approval Check Types

| Check Type | Description | Result |
|------------|-------------|--------|
| `NOTICE_PERMANENTLY_SUSPENDED` | Notice has permanent suspension (PS) | Pending Review |
| `FURNISHED_ID_CURRENT_OFFENDER` | Furnished ID is already current offender | Pending Review |
| `FURNISHED_ID_IN_HIRER_DRIVER` | Furnished ID exists in Hirer/Driver Details | Pending Review |
| `HIRER_DRIVER_PARTICULARS_EXISTS` | Hirer/Driver particulars already present | Pending Review |
| `OWNER_NOT_CURRENT_OFFENDER` | Owner/Hirer is no longer current offender | Pending Review |

### 8.3 Business Error Reasons

| Reason Code | Description |
|-------------|-------------|
| `ALREADY_PROCESSED` | Application has already been approved/rejected |
| `NOT_FURNISHABLE` | Notice is not at furnishable stage |
| `NOTICE_NOT_FOUND` | Notice number does not exist |
| `INVALID_OWNER_DRIVER_INDICATOR` | Invalid owner/driver indicator (must be H or D) |
| `VEHICLE_NUMBER_MISMATCH` | Vehicle number does not match notice |
| `PERMANENT_SUSPENSION` | Notice is permanently suspended |

### 8.4 Technical Error Operations

| Operation | Description |
|-----------|-------------|
| `CREATE_FURNISH_APPLICATION` | Failed to create furnish application record |
| `UPDATE_FURNISH_APPLICATION` | Failed to update furnish application record |
| `CREATE_OFFENDER_RECORD` | Failed to create offender record |
| `UPDATE_OFFENDER_RECORD` | Failed to update offender record |
| `UPDATE_PROCESSING_STAGE` | Failed to update notice processing stage |
| `SEND_EMAIL` | Failed to send email notification |
| `SEND_SMS` | Failed to send SMS notification |
| `SYNC_BLOB` | Failed to sync blob file |

### 8.5 Validation Error Messages

| Field | Error Message |
|-------|---------------|
| txnNo | Transaction number is required |
| noticeNo | Notice number is required |
| vehicleNo | Vehicle number is required |
| offenceDate | Offence date is required |
| ppCode | Car park code is required |
| ppName | Car park name is required |
| furnishName | Furnished person name is required |
| furnishIdType | Furnished person ID type is required |
| furnishIdNo | Furnished person ID number is required |
| furnishMailBlkNo | Block/house number is required |
| furnishMailStreetName | Street name is required |
| furnishMailPostalCode | Postal code is required |
| ownerDriverIndicator | Owner/driver indicator is required (H or D) |
| hirerOwnerRelationship | Hirer/owner relationship is required |
| quesOneAns | Question 1 answer is required |
| quesTwoAns | Question 2 answer is required |
| ownerName | Owner name is required |
| ownerIdNo | Owner ID number is required |

---

## 9. Data Types Reference

### 9.1 Owner/Driver Indicator

| Code | Description |
|------|-------------|
| `O` | Owner |
| `H` | Hirer |
| `D` | Driver |

### 9.2 Furnish Application Status

| Code | Description |
|------|-------------|
| `P` | Pending - Requires manual review |
| `A` | Approved - Officer approved |
| `R` | Rejected - Officer rejected |

### 9.3 ID Types

| Code | Description |
|------|-------------|
| `NRIC` | Singapore NRIC |
| `FIN` | Foreign Identification Number |
| `PASSPORT` | Passport Number |
| `UEN` | Unique Entity Number (Company) |

### 9.4 Processing Stages (Furnishable)

| Stage Code | Description | Furnishable |
|------------|-------------|-------------|
| `ENA` | Enforcement Notice A | Yes |
| `RD1` | Reminder 1 | Yes |
| `RD2` | Reminder 2 | Yes |
| `RR3` | Reminder 3 | Yes |
| `CPC` | Court Proceedings Commenced | No |

### 9.5 Suspension Types

| Code | Description |
|------|-------------|
| `TS-PDP` | Temporary Suspension - Pending Driver Particulars |
| `PS` | Permanent Suspension |

---

## Appendix A: Database Tables

| Table | Database | Description |
|-------|----------|-------------|
| `ocms_furnish_application` | Intranet | Furnish application records |
| `ocms_furnish_application_doc` | Intranet | Supporting documents metadata |
| `eocms_furnish_application` | Internet | Furnish application (eService) |
| `eocms_furnish_application_doc` | Internet | Supporting documents (eService) |
| `ocms_offence_notice_owner_driver` | Intranet | Owner/Hirer/Driver records |
| `ocms_offence_notice_owner_driver_addr` | Intranet | Offender address records |
| `ocms_valid_offence_notice` | Intranet | Notice records |
| `ocms_suspended_notice` | Intranet | Suspension records |

---

## Appendix B: Assumptions

| ID | Assumption | Impact |
|----|------------|--------|
| [ASSUMPTION-1] | HST ID check is not implemented in current code | Auto-approval may pass for HST IDs |
| [ASSUMPTION-2] | FIN/Passport ID check is not implemented | Auto-approval may pass for FIN/Passport |
| [ASSUMPTION-3] | Furnishable stage codes may differ between FD and implementation | Stage validation may need alignment |
| [ASSUMPTION-4] | Retry mechanism (x3) is not implemented | Single failure will skip record |

---

*Document generated based on OCMS 41 Functional Document v1.1 and backend code analysis*

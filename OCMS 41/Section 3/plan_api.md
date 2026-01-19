# OCMS 41 Section 3 - Staff Portal Manual Review API Specification

## Document Information

| Field | Value |
|-------|-------|
| Version | 1.0 |
| Date | 2026-01-07 |
| Source | Functional Document v1.1, Backend Code |
| Module | OCMS 41 - Section 3: Staff Portal Manual Review |

---

## Table of Contents

1. [Overview](#1-overview)
2. [Internal APIs](#2-internal-apis)
3. [Error Codes Reference](#3-error-codes-reference)
4. [Data Types Reference](#4-data-types-reference)

---

## 1. Overview

Section 3 covers the Staff Portal Manual Review functionality where OICs can:
- View pending furnished submissions
- Search for submissions
- Review submission details
- Approve or reject submissions
- Send email/SMS notifications

### 1.1 Actors

| Actor | Description |
|-------|-------------|
| OIC | Officer-In-Charge who reviews furnished submissions |

### 1.2 API Base Path

```
/api/v1/furnish/officer
```

### 1.3 Audit User Configuration

| Environment | Database User | Usage |
|-------------|---------------|-------|
| Intranet | `ocmsiz_app_conn` | All cre_user_id and upd_user_id fields |
| Internet | `ocmsez_app_conn` | All cre_user_id and upd_user_id fields |

> **Important:** Do NOT use "SYSTEM" for audit user fields. Always use the database user above.

---

## 2. Internal APIs

### 2.1 List Furnished Applications

**Endpoint:** `POST /furnish/officer/list`

**Description:** Lists furnished applications with optional filters for OIC dashboard.

**User Stories:** OCMS41.9-41.12

#### Request

```json
{
  "statuses": ["P", "Resubmission"],
  "noticeNo": "500500303J",
  "vehicleNo": "SNC7392R",
  "furnishIdNo": "S1234567A",
  "submissionDateFrom": "2025-01-01T00:00:00",
  "submissionDateTo": "2025-12-31T23:59:59",
  "page": 0,
  "pageSize": 10,
  "sortBy": "submissionDate",
  "sortDirection": "DESC"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| statuses | String[] | No | Filter by status. Values: P, Resubmission, A, R. Default: P, Resubmission |
| noticeNo | String | No | Filter by notice number (exact match) |
| vehicleNo | String | No | Filter by vehicle number (partial match) |
| furnishIdNo | String | No | Filter by furnished person's ID (NRIC/FIN/UEN/Passport) |
| submissionDateFrom | DateTime | No | Filter submissions from this date |
| submissionDateTo | DateTime | No | Filter submissions to this date |
| page | Integer | No | Page number (0-indexed). Default: 0 |
| pageSize | Integer | No | Records per page. Default: 10 |
| sortBy | String | No | Sort field: noticeNo, submissionDate, vehicleNo, status |
| sortDirection | String | No | ASC or DESC. Default: ASC |

#### Response (Success)

```json
{
  "total": 100,
  "limit": 10,
  "skip": 0,
  "data": [
    {
      "txnNo": "FH202601070001",           // Source: ocms_furnish_application.txn_no
      "noticeNo": "500500303J",             // Source: ocms_furnish_application.notice_no
      "vehicleNo": "SNC7392R",              // Source: ocms_furnish_application.vehicle_no
      "offenceDateTime": "2025-07-10T08:26:00", // Source: ocms_furnish_application.offence_date
      "processingStage": "RD1",             // Source: ocms_furnish_application.last_processing_stage
      "status": "P",                        // Source: ocms_furnish_application.status
      "submissionDateTime": "2025-08-04T17:41:00", // Source: ocms_furnish_application.cre_date
      "submitterName": "JOHN LEE",          // Source: ocms_furnish_application.owner_name
      "furnishedName": "ADAM TAN",          // Source: ocms_furnish_application.furnish_name
      "hirerDriverIndicator": "D",          // Source: ocms_furnish_application.owner_driver_indicator
      "reasonForReview": "HST ID"           // Source: ocms_furnish_application.reason_for_review
    }
  ]
}
```

---

### 2.2 Get Application Detail

**Endpoint:** `POST /furnish/officer/detail`

**Description:** Retrieves detailed view of a furnished application for OIC review.

**User Stories:** OCMS41.13-41.14

#### Request Body

```json
{
  "txnNo": "FH202601070001"
}
```

#### Request Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| txnNo | String | Yes | Transaction number |

#### Response (Success)

```json
{
  "success": true,
  "data": {
    "txnNo": "FH202601070001",
    "noticeDetails": {
      "noticeNo": "500500303J",
      "vehicleNo": "SNC7392R",
      "ppCode": "A0004",
      "ppName": "ALIWAL STREET",
      "offenceDateTime": "2025-07-10T08:26:00"
    },
    "submitterDetails": {
      "name": "Tiger City Rentals Pte Ltd",
      "idNo": "20051827M",
      "idType": "UEN",
      "email": "kennylim@tigercityrentals.com",
      "countryCode": "65",
      "contactNo": "81234567",
      "staffName": "Kenny Lim"
    },
    "furnishedDetails": {
      "name": "John Lee",
      "idNo": "S1234567A",
      "idType": "NRIC",
      "email": "john.lee@abcmail.com",
      "countryCode": "65",
      "contactNo": "91234567",
      "hirerDriverIndicator": "D",
      "relationship": "Employee",
      "rentalPeriodFrom": null,
      "rentalPeriodTo": null,
      "quesOneAns": "Y",
      "quesTwoAns": "Y",
      "quesThreeAns": "N"
    },
    "submissionInfo": {
      "processingStage": "RD1",
      "status": "P",
      "reasonForReview": "HST ID",
      "submissionDateTime": "2025-08-04T17:41:00",
      "createdDate": "2025-06-15T14:36:05",
      "createdBy": "ocmsiz_app_conn",
      "updatedDate": "2025-06-15T14:36:07",
      "updatedBy": "ocmsiz_app_conn"
    },
    "supportingDocuments": [
      {
        "fileName": "rental_agreement.pdf",
        "fileSize": "2MB",
        "mimeType": "application/pdf",
        "downloadUrl": "/api/v1/furnish/document/{docId}"
      }
    ],
    "noticeStatus": {
      "isFurnishable": true,
      "isPermanentlySuspended": false,
      "currentProcessingStage": "RD1"
    }
  }
}
```

#### Response (Not Found)

```json
{
  "success": false,
  "errorType": "NOT_FOUND",
  "message": "Furnished application not found: FH202601070001"
}
```

---

### 2.3 Approve Furnished Submission

**Endpoint:** `POST /furnish/officer/approve`

**Description:** Approves a furnished submission and adds the furnished person as Current Offender.

**User Stories:** OCMS41.15-41.23

#### Request

```json
{
  "txnNo": "FH202601070001",
  "officerId": "ALICIATAN",
  "sendEmailToOwner": true,
  "sendEmailToFurnished": true,
  "sendSmsToFurnished": false,
  "emailTemplateId": "APPROVED_HIRER_INDIVIDUAL",
  "customEmailSubject": null,
  "customEmailBody": null,
  "customSmsBody": null,
  "remarks": "Approved as verified hirer"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| txnNo | String | Yes | Transaction number |
| officerId | String | Yes | OIC's user ID |
| sendEmailToOwner | Boolean | No | Send notification email to owner. Default: false |
| sendEmailToFurnished | Boolean | No | Send notification email to furnished person. Default: false |
| sendSmsToFurnished | Boolean | No | Send SMS to furnished person. Default: false |
| emailTemplateId | String | No | Email template ID. Null = default template |
| customEmailSubject | String | No | Custom email subject (overrides template) |
| customEmailBody | String | No | Custom email body (overrides template) |
| customSmsBody | String | No | Custom SMS body (overrides template) |
| remarks | String | No | OIC remarks |

#### Response (Success)

```json
{
  "success": true,
  "txnNo": "FH202601070001",
  "noticeNo": "500500303J",
  "hirerDriverRecordUpdated": true,
  "suspensionRevived": true,
  "emailSentToOwner": true,
  "emailSentToFurnished": true,
  "smsSentToFurnished": false,
  "message": "Furnished submission approved successfully"
}
```

#### Response (Business Error)

```json
{
  "success": false,
  "errorType": "BUSINESS_ERROR",
  "reason": "ALREADY_PROCESSED",
  "message": "Submission has already been processed"
}
```

---

### 2.4 Reject Furnished Submission

**Endpoint:** `POST /furnish/officer/reject`

**Description:** Rejects a furnished submission and sends rejection notification.

**User Stories:** OCMS41.24-41.33

#### Request

```json
{
  "txnNo": "FH202601070001",
  "officerId": "ALICIATAN",
  "sendEmailToOwner": true,
  "emailTemplateId": "REJECTED_DOCS_REQUIRED",
  "customEmailSubject": null,
  "customEmailBody": null,
  "rejectionReason": "Insufficient supporting documents",
  "remarks": "Required documents not provided"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| txnNo | String | Yes | Transaction number |
| officerId | String | Yes | OIC's user ID |
| sendEmailToOwner | Boolean | No | Send notification email to owner. Default: false |
| emailTemplateId | String | No | Template ID: REJECTED_DOCS_REQUIRED, REJECTED_MULTIPLE_HIRERS, REJECTED_RENTAL_DISCREPANCY, REJECTED_GENERAL |
| customEmailSubject | String | No | Custom email subject |
| customEmailBody | String | No | Custom email body |
| rejectionReason | String | Yes | Reason for rejection |
| remarks | String | No | OIC remarks |

#### Response (Success)

```json
{
  "success": true,
  "txnNo": "FH202601070001",
  "noticeNo": "500500303J",
  "emailSentToOwner": true,
  "noticeResentToPortal": true,
  "message": "Furnished submission rejected successfully"
}
```

---

### 2.5 Download Supporting Document

**Endpoint:** `POST /furnish/document/download`

**Description:** Downloads a supporting document attached to a furnished submission.

#### Request Body

```json
{
  "docId": "DOC001"
}
```

#### Request Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| docId | String | Yes | Document ID |

#### Response

Binary file download with appropriate Content-Type header.

---

### 2.6 Get Email Templates

**Endpoint:** `POST /furnish/templates/list`

**Description:** Retrieves available email/SMS templates for furnish notifications.

#### Request Body

```json
{
  "type": "EMAIL",
  "decision": "APPROVE"
}
```

#### Request Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| type | String | No | Filter by type: EMAIL, SMS. Default: all |
| decision | String | No | Filter by decision: APPROVE, REJECT. Default: all |

#### Response (Success)

```json
{
  "success": true,
  "data": [
    {
      "templateId": "APPROVED_HIRER_INDIVIDUAL",
      "type": "EMAIL",
      "decision": "APPROVE",
      "name": "Email to Owner on approved Hirer (individual) submission",
      "subject": "URA Parking Offence Notice - Hirer Approved",
      "body": "Dear {{submitterName}},\n\nYour furnished hirer particulars for Notice No. {{noticeNo}}..."
    }
  ]
}
```

---

## 3. Error Codes Reference

### 3.1 Validation Errors (HTTP 400)

| Error Type | Field | Message |
|------------|-------|---------|
| VALIDATION_ERROR | txnNo | Transaction number is required |
| VALIDATION_ERROR | officerId | Officer ID is required |
| VALIDATION_ERROR | rejectionReason | Rejection reason is required |

### 3.2 Business Errors (HTTP 409)

| Reason | Message |
|--------|---------|
| ALREADY_PROCESSED | Submission has already been processed |
| NOT_PENDING | Submission is not in pending status |
| NOTICE_NOT_FURNISHABLE | Notice is no longer at furnishable stage |
| PERMANENTLY_SUSPENDED | Notice is permanently suspended |

### 3.3 Technical Errors (HTTP 500)

| Operation | Message |
|-----------|---------|
| DB_UPDATE | Failed to update database |
| EMAIL_SEND | Failed to send email notification |
| SMS_SEND | Failed to send SMS notification |
| SYNC_INTERNET | Failed to sync outcome to Internet DB |

---

## 4. Data Types Reference

### 4.1 Status Codes

| Code | Description |
|------|-------------|
| P | Pending - Awaiting OIC review |
| Resubmission | Resubmission - Previously rejected, resubmitted by owner |
| A | Approved - OIC approved the submission |
| R | Rejected - OIC rejected the submission |

### 4.2 Hirer/Driver Indicator

| Code | Description |
|------|-------------|
| H | Hirer |
| D | Driver |

### 4.3 ID Types

> **Source:** Data Dictionary - `furnish_id_type` varchar(1)

| Code | Description |
|------|-------------|
| N | Singapore NRIC |
| F | Foreign Identification Number (FIN) |
| U | Unique Entity Number (UEN/Company) |
| P | Passport Number |

### 4.4 Reason for Review

| Reason | Description |
|--------|-------------|
| Notice is permanently suspended | Notice has active permanent suspension |
| HST ID | Furnished ID is in HST database |
| FIN or Passport ID | Furnished ID type is FIN or Passport |
| Notice is no longer at furnishable stage | Notice stage changed after submission |
| Prior submission under notice | Another submission exists for same notice |
| Hirer/Driver's particulars already present | H/D record already exists with different ID |
| Furnished ID already in Hirer or Driver Details | Furnished ID already in H/D Details |
| Owner/Hirer (submitter) no longer current offender | Submitter is not current offender |

---

## Appendix A: Database Tables

### A.1 Tables Used

| Table | Zone | Purpose |
|-------|------|---------|
| ocms_furnish_application | Intranet | Main furnished submission table |
| ocms_furnish_email | Intranet | Email message records |
| ocms_furnish_email_attachment | Intranet | Email attachments |
| ocms_furnish_sms | Intranet | SMS message records |
| ocms_offence_notice_owner_driver | Intranet | Hirer/Driver records |
| eocms_furnish_application | Internet | Internet copy for eService display |

---

*Document generated for OCMS 41 Section 3 API planning*

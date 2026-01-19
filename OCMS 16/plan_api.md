# API Plan: OCMS 16 - Reduction

## Overview

| Attribute | Value |
| --- | --- |
| API Name | PLUS Apply Reduction API |
| Version | v1.0 |
| Author | Claude |
| Created Date | 18/01/2026 |
| Last Updated | 18/01/2026 |
| Status | Draft |
| Related Document | OCMS 16 Technical Doc |
| Source | Functional Document v1.3, Backend Code, Functional Flowchart |

---

## 1. Purpose

This API allows the PLUS system to request a reduction of the composition amount for a notice in OCMS. When a PLUS officer approves an appeal for fee reduction, PLUS sends a request to OCMS to apply the reduced composition amount to the notice and suspend the notice with a temporary suspension reason "RED - Pay Reduced Amount".

**Reference:** Functional Document Section 2 & Section 3

---

## 2. API Endpoints

### 2.1 Apply Reduction (PLUS to OCMS)

| Attribute | Value |
| --- | --- |
| Method | POST |
| URL | `/v1/plus-apply-reduction` |
| Authentication | Bearer Token (via APIM) |
| Timeout | 30 seconds |
| Source System | PLUS Staff Portal |

#### Request

**Headers:**

| Header | Required | Description |
| --- | --- | --- |
| Authorization | Yes | Bearer {token} |
| Content-Type | Yes | application/json |
| X-Request-ID | No | Unique request identifier for tracing |

**Request Body:**

```json
{
  "noticeNo": "500500303J",
  "amountReduced": 55.00,
  "amountPayable": 15.00,
  "dateOfReduction": "2025-08-01T19:00:02",
  "expiryDateOfReduction": "2025-08-15T00:00:00",
  "reasonOfReduction": "Appeal approved - reduced amount",
  "authorisedOfficer": "JOHNLEE",
  "suspensionSource": "005",
  "remarks": "Reduction granted based on appeal review"
}
```

**Request Body Schema:**

| Field | Type | Required | Validation | Description |
| --- | --- | --- | --- | --- |
| noticeNo | string | Yes | Not blank | Notice number to be reduced |
| amountReduced | decimal | Yes | Positive value, <= original composition amount | The amount reduced from the original composition amount |
| amountPayable | decimal | Yes | amountPayable = originalAmount - amountReduced, >= 0 | The new amount payable after reduction |
| dateOfReduction | datetime | Yes | Format: yyyy-MM-dd'T'HH:mm:ss | Date and time when reduction is applied |
| expiryDateOfReduction | datetime | Yes | Format: yyyy-MM-dd'T'HH:mm:ss, must be after dateOfReduction | Expiry date of the reduction offer (due date of revival) |
| reasonOfReduction | string | Yes | Not blank | Reason for the reduction |
| authorisedOfficer | string | Yes | Not blank | Officer authorizing the reduction (cre_user_id from PLUS) |
| suspensionSource | string | Yes | Not blank | Source system code (e.g., "005" for PLUS) |
| remarks | string | No | - | Optional remarks for the reduction |

**Reference:** FD Section 2.3.1 - PLUS Backend requests parameter data via API call

#### Response

**Success Response (200 OK):**

```json
{
  "success": true,
  "message": "Reduction Success"
}
```

**Error Responses:**

**400 Bad Request - Invalid Format:**
```json
{
  "success": false,
  "message": "Invalid format"
}
```

**400 Bad Request - Missing Data:**
```json
{
  "success": false,
  "message": "Missing data"
}
```

**404 Not Found - Notice Not Found:**
```json
{
  "success": false,
  "message": "Notice not found"
}
```

**409 Conflict - Notice Has Been Paid:**
```json
{
  "success": false,
  "message": "Notice has been paid"
}
```

**409 Conflict - Notice Not Eligible:**
```json
{
  "success": false,
  "message": "Notice is not eligible"
}
```

**500 Internal Server Error - Reduction Fail:**
```json
{
  "success": false,
  "message": "Reduction fail"
}
```

**503 Service Unavailable:**
```json
{
  "success": false,
  "message": "System unavailable"
}
```

**Reference:** FD Section 3.3 - Reduction Scenarios and Outcomes

---

## 3. Error Responses Summary

| HTTP Status | Error Code | Message | Description | Resolution |
| --- | --- | --- | --- | --- |
| 200 | - | Reduction Success | Reduction applied successfully | - |
| 400 | INVALID_FORMAT | Invalid format | Request data is incomplete or has invalid structure | Check request body format and data types |
| 400 | MISSING_DATA | Missing data | Mandatory fields are missing | Provide all required fields |
| 404 | NOTICE_NOT_FOUND | Notice not found | Notice number does not exist in OCMS | Verify notice number is correct |
| 409 | NOTICE_PAID | Notice has been paid | Notice has already been fully paid (CRS = FP or PRA) | Cannot reduce a paid notice |
| 409 | NOT_ELIGIBLE | Notice is not eligible | Notice does not meet eligibility criteria based on computer rule code and processing stage | Check processing stage and computer rule code eligibility |
| 500 | REDUCTION_FAIL | Reduction fail | Database update failed or internal error | Retry or contact support |
| 500 | ROLLBACK_FAILURE | Reduction fail | Partial update occurred and rollback failed | Manual intervention required |
| 503 | SYSTEM_UNAVAILABLE | System unavailable | OCMS backend cannot access database | Retry later |

**Reference:** FD Section 3.3 - Reduction Scenarios and Outcomes table

---

## 4. Data Mapping

### 4.1 Request to Intranet Database - ocms_valid_offence_notice (UPDATE)

| API Field | Database Field | Transformation |
| --- | --- | --- |
| noticeNo | notice_no | Used for lookup (WHERE clause) |
| amountPayable | amount_payable | Direct mapping |
| dateOfReduction | epr_date_of_suspension | Direct mapping |
| expiryDateOfReduction | due_date_of_revival | Direct mapping |
| - | suspension_type | Set to "TS" (Temporary Suspension) |
| - | epr_reason_of_suspension | Set to "RED" (Pay Reduced Amount) |

**Reference:** FD Section 3.4.1

### 4.2 Request to Intranet Database - ocms_suspended_notice (INSERT)

| API Field | Database Field | Transformation |
| --- | --- | --- |
| noticeNo | notice_no | Direct mapping |
| dateOfReduction | date_of_suspension | Direct mapping |
| - | sr_no | Auto-generated (next sequence for this notice) |
| suspensionSource | suspension_source | Direct mapping (e.g., "PLUS") |
| - | suspension_type | Set to "TS" |
| - | reason_of_suspension | Set to "RED" |
| authorisedOfficer | officer_authorising_suspension | Direct mapping |
| expiryDateOfReduction | due_date_of_revival | Direct mapping |
| reasonOfReduction | suspension_remarks | Direct mapping |

**Reference:** FD Section 3.4.2

### 4.3 Request to Intranet Database - ocms_reduced_offence_amount (INSERT)

| API Field | Database Field | Transformation |
| --- | --- | --- |
| noticeNo | notice_no | Direct mapping |
| dateOfReduction | date_of_reduction | Direct mapping |
| - | sr_no | Same as suspended_notice sr_no |
| amountReduced | amount_reduced | Direct mapping |
| amountPayable | amount_payable | This is the amount BEFORE reduction (original) |
| reasonOfReduction | reason_of_reduction | Direct mapping |
| expiryDateOfReduction | expiry_date | Direct mapping |
| authorisedOfficer | officer_authorising_reduction | Direct mapping |
| remarks | remarks | Direct mapping |

**Note:** The `amount_payable` in this table refers to the original amount before reduction, per FD Section 3.4.3.

**Reference:** FD Section 3.4.3

### 4.4 Request to Internet Database - eocms_valid_offence_notice (UPDATE)

| Intranet Field | Internet Field | Notes |
| --- | --- | --- |
| suspension_type | suspension_type | Set to "TS" |
| epr_reason_of_suspension | epr_reason_of_suspension | Set to "RED" |
| epr_date_of_suspension | epr_date_of_suspension | Direct mapping |
| amount_payable | amount_payable | Direct mapping |

**Note:** The `due_date_of_revival` field does NOT exist in the Internet database schema. Only 4 fields are synced.

**Reference:** FD Section 3.4.1

---

## 5. Database Tables Affected

### Intranet Zone

| Table Name | Operation | Description |
| --- | --- | --- |
| ocms_valid_offence_notice | UPDATE | Update notice with TS-RED suspension and new amount payable |
| ocms_suspended_notice | INSERT | Create new suspension record |
| ocms_reduced_offence_amount | INSERT | Create reduction log record (follows CAS format) |

### Internet Zone

| Table Name | Operation | Description |
| --- | --- | --- |
| eocms_valid_offence_notice | UPDATE | Mirror intranet changes (4 fields only) |

**Reference:** FD Section 3.4

---

## 6. Processing Flow

Based on **Functional Flowchart - Backend Reduction** tab:

```
Step 1: Receive Reduction Request
    │
    ▼
Step 2: Validate Request Format ──► Invalid? ──► Return "Invalid format"
    │
    ▼
Step 3: Check Mandatory Data ──► Missing? ──► Return "Missing data"
    │
    ▼
Step 4: Load Notice by notice_no ──► Not found? ──► Return "Notice not found"
    │
    ▼
Step 5: Check if Already Reduced (TS-RED) ──► Yes? ──► Return "Reduction Success" (Idempotent)
    │
    ▼
Step 6: Check Payment Status (CRS = FP/PRA?) ──► Paid? ──► Return "Notice has been paid"
    │
    ▼
Step 7: Check Eligibility (Computer Rule Code + Last Processing Stage)
    │
    ├── Eligible Code (30305/31302/30302/21300)?
    │       │
    │       ├── YES ──► Stage in (NPA/ROV/ENA/RD1/RD2/RR3/DN1/DN2/DR3)? ──► No? ──► "Not eligible"
    │       │
    │       └── NO ──► Stage in (RR3/DR3)? ──► No? ──► "Not eligible"
    │
    ▼
Step 8: Validate Amounts and Dates
    │
    ▼
Step 9: Apply Reduction (Transactional)
    │   ├── Update ocms_valid_offence_notice
    │   ├── Insert ocms_suspended_notice
    │   ├── Insert ocms_reduced_offence_amount
    │   └── Update eocms_valid_offence_notice
    │
    ▼
Step 10: Check Update Success ──► Failed? ──► Rollback ──► Return "Reduction fail"
    │
    ▼
Return "Reduction Success"
```

**Reference:** Functional Flowchart - Backend Reduction tab

---

## 7. Transaction Management

All database operations are wrapped in a single transaction:

1. Update intranet ocms_valid_offence_notice
2. Insert into ocms_suspended_notice
3. Insert into ocms_reduced_offence_amount
4. Update internet eocms_valid_offence_notice

**Rollback Behavior:**
- If any step fails, all changes are rolled back
- Response to PLUS: "Reduction fail"

**Partial Update Scenario:**
- If data updated but suspension record creation fails → Rollback all → Return "Reduction fail"

**Idempotency:**
- If a notice already has TS-RED status, the API returns success without making changes
- This prevents duplicate reductions from retried requests

**Reference:** FD Section 3.3 - Partial Update scenario

---

## 8. Sample cURL

```bash
# Success case - Apply Reduction
curl -X POST \
  'https://api.ocms.gov.sg/v1/plus-apply-reduction' \
  -H 'Authorization: Bearer {token}' \
  -H 'Content-Type: application/json' \
  -d '{
    "noticeNo": "500500303J",
    "amountReduced": 55.00,
    "amountPayable": 15.00,
    "dateOfReduction": "2025-08-01T19:00:02",
    "expiryDateOfReduction": "2025-08-15T00:00:00",
    "reasonOfReduction": "RED",
    "authorisedOfficer": "JOHNLEE",
    "suspensionSource": "005",
    "remarks": "Reduction granted based on appeal review"
  }'
```

---

## 9. Dependencies

| Service/System | Type | Purpose |
| --- | --- | --- |
| APIM | Gateway | Routes request from PLUS to OCMS Backend |
| OCMS Intranet Database | Database | Primary data store for notices |
| OCMS Internet Database | Database | Public-facing data mirror |
| PLUS System | External System | Source of reduction requests |

---

## 10. Changelog

| Version | Date | Author | Changes |
| --- | --- | --- | --- |
| 1.0 | 18/01/2026 | Claude | Initial version based on FD v1.3, Backend Code, and Functional Flowchart |

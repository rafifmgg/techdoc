# OCMS 41 - Furnish Hirer/Driver Conditions & Validations

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
2. [Frontend Validations](#2-frontend-validations)
3. [Backend Validations](#3-backend-validations)
4. [Auto-Approval Conditions](#4-auto-approval-conditions)
5. [Manual Review Conditions](#5-manual-review-conditions)
6. [Business Rule Conditions](#6-business-rule-conditions)
7. [Processing Stage Conditions](#7-processing-stage-conditions)
8. [Decision Trees](#8-decision-trees)
9. [Error Handling Conditions](#9-error-handling-conditions)
10. [Assumptions Log](#10-assumptions-log)

---

## 1. Overview

This document defines all validation rules, conditions, and decision logic for OCMS 41 Furnish Hirer/Driver module.

### 1.1 Validation Layers

| Layer | Responsibility | Implementation |
|-------|----------------|----------------|
| Frontend | Form field validation, UI state management | Staff Portal, eService Portal |
| API Gateway | Authentication, rate limiting | JWT validation |
| Backend - Bean | Request format validation | Jakarta Bean Validation (@NotBlank, etc.) |
| Backend - Business | Business rule validation | FurnishValidator, ManualFurnishValidationService |
| Database | Data integrity constraints | Primary keys, foreign keys, check constraints |

---

## 2. Frontend Validations

### 2.1 eService Portal - Furnish Submission Form

#### 2.1.1 Furnished Person Details

> **Source:** Data Dictionary - `ocms_furnish_application` table

| Rule ID | Field | Validation Rule | Error Message | DB Column (Max Length) |
|---------|-------|-----------------|---------------|------------------------|
| FE-001 | Furnished Name | Required, max 66 chars | Please enter the name of the person | furnish_name (66) |
| FE-002 | Furnished ID Type | Required, dropdown (N/F/P/U) | Please select ID type | furnish_id_type (1) |
| FE-003 | Furnished ID No | Required, max 12 chars, format based on ID type | Please enter a valid ID number | furnish_id_no (12) |
| FE-004 | Furnished ID No (NRIC) | Pattern: ^[STFG]\d{7}[A-Z]$ | Invalid NRIC format | - |
| FE-005 | Furnished ID No (FIN) | Pattern: ^[FGMK]\d{7}[A-Z]$ | Invalid FIN format | - |
| FE-006 | Furnished ID No (UEN) | Pattern: ^\d{8,9}[A-Z]$ or ^[TSR]\d{2}[A-Z]{2}\d{4}[A-Z]$ | Invalid UEN format | - |

#### 2.1.2 Furnished Person Address

> **Source:** Data Dictionary - `ocms_furnish_application` table

| Rule ID | Field | Validation Rule | Error Message | DB Column (Max Length) |
|---------|-------|-----------------|---------------|------------------------|
| FE-010 | Block/House No | Required, max 10 chars | Please enter block/house number | furnish_mail_blk_no (10) |
| FE-011 | Floor | Optional, max 3 chars | Invalid floor number | furnish_mail_floor (3) |
| FE-012 | Unit No | Optional, max 5 chars | - | furnish_mail_unit_no (5) |
| FE-013 | Street Name | Required, max 32 chars | Please enter street name | furnish_mail_street_name (32) |
| FE-014 | Building Name | Optional, max 65 chars | - | furnish_mail_bldg_name (65) |
| FE-015 | Postal Code | Required, exactly 6 digits | Please enter a valid 6-digit postal code | furnish_mail_postal_code (6) |

#### 2.1.3 Furnished Person Contact

> **Source:** Data Dictionary - `ocms_furnish_application` table

| Rule ID | Field | Validation Rule | Error Message | DB Column (Max Length) |
|---------|-------|-----------------|---------------|------------------------|
| FE-020 | Country Code | Optional, max 4 chars | Invalid country code | furnish_tel_country_code (4) |
| FE-021 | Area Code | Optional, max 3 chars | Invalid area code | furnish_tel_area_code (3) |
| FE-022 | Tel Code | Optional, max 4 chars | Invalid telephone code | furnish_tel_code (4) |
| FE-023 | Telephone No | Optional, max 12 chars | Invalid telephone number | furnish_tel_no (12) |
| FE-024 | Alt Country Code | Optional, max 10 chars | Invalid alt country code | furnish_alt_tel_country_code (10) |
| FE-025 | Alt Area Code | Optional, max 3 chars | Invalid alt area code | furnish_alt_tel_area_code (3) |
| FE-026 | Alt Contact No | Optional, max 12 chars | Invalid alt contact number | furnish_alt_contact_no (12) |
| FE-027 | Email Address | Optional, max 320 chars, email format | Invalid email format | furnish_email_addr (320) |

#### 2.1.4 Furnish Details

> **Source:** Data Dictionary - `ocms_furnish_application` table

| Rule ID | Field | Validation Rule | Error Message | DB Column (Max Length) |
|---------|-------|-----------------|---------------|------------------------|
| FE-030 | Owner/Driver Indicator | Required, O=Owner/H=Hirer/D=Driver | Please select Owner, Hirer or Driver | owner_driver_indicator (1) |
| FE-031 | Relationship | Required when H selected, max 1 char code | Please select relationship | hirer_owner_relationship (1) |
| FE-032 | Others Description | Required when Relationship = "Others", max 15 chars | Please describe the relationship | others_relationship_desc (15) |
| FE-033 | Rental Period From | Required for rental relationship | Please select rental start date | rental_period_from |
| FE-034 | Rental Period To | Required for rental relationship, >= From date | Please select rental end date | rental_period_to |

#### 2.1.5 Questionnaire

> **Source:** Data Dictionary - `ocms_furnish_application` table

| Rule ID | Field | Validation Rule | Error Message | DB Column (Max Length) |
|---------|-------|-----------------|---------------|------------------------|
| FE-040 | Question 1 Answer | Required, max 32 chars | Please answer Question 1 | ques_one_ans (32) |
| FE-041 | Question 2 Answer | Required, max 32 chars | Please answer Question 2 | ques_two_ans (32) |
| FE-042 | Question 3 Answer | Optional, max 32 chars | Please answer Question 3 | ques_three_ans (32) |

#### 2.1.6 Document Upload

| Rule ID | Field | Validation Rule | Error Message |
|---------|-------|-----------------|---------------|
| FE-050 | Supporting Document | At least 1 required | Please upload at least one supporting document |
| FE-051 | File Type | PDF, JPG, PNG only | Invalid file type. Only PDF, JPG, PNG allowed |
| FE-052 | File Size | Max 5MB per file | File size exceeds 5MB limit |
| FE-053 | Total Files | Max 5 files | Maximum 5 files allowed |

---

### 2.2 Staff Portal - Manual Review Form

#### 2.2.1 Editable Fields

> **Source:** Data Dictionary - `ocms_furnish_application` table

| Rule ID | Field | Validation Rule | Error Message | DB Column (Max Length) |
|---------|-------|-----------------|---------------|------------------------|
| FE-060 | Submitter's Email | Required, max 320 chars, email format | Invalid email format | furnish_email_addr (320) |
| FE-061 | Submitter's Country Code | Required, max 4 chars | Invalid country code | furnish_tel_country_code (4) |
| FE-062 | Submitter's Contact No | Required, max 12 chars | Invalid contact number | furnish_tel_no (12) |
| FE-063 | Decision | Required, radio (Approve Hirer/Approve Driver/Reject) | Please select a decision | status (1) |
| FE-064 | Remarks | Optional, max 200 chars | - | remarks (200) |
| FE-065 | Send Email/SMS | Checkbox, default unchecked | - | - |

---

### 2.3 Staff Portal - Manual Furnish Form

#### 2.3.1 Offender Details

> **Source:** Data Dictionary - `ocms_furnish_application` table

| Rule ID | Field | Validation Rule | Error Message | DB Column (Max Length) |
|---------|-------|-----------------|---------------|------------------------|
| FE-070 | Name | Required, max 66 chars | Please enter name | furnish_name (66) |
| FE-071 | ID Type | Required, dropdown (N/F/P/U) | Please select ID type | furnish_id_type (1) |
| FE-072 | ID No | Required, max 12 chars, format based on ID type | Please enter valid ID number | furnish_id_no (12) |
| FE-073 | Telephone Country Code | Optional, max 4 chars | Invalid country code | furnish_tel_country_code (4) |
| FE-074 | Telephone No | Optional, max 12 chars | Invalid telephone number | furnish_tel_no (12) |
| FE-075 | Email Address | Optional, max 320 chars, email format | Invalid email format | furnish_email_addr (320) |

#### 2.3.2 Mailing Address

> **Source:** Data Dictionary - `ocms_furnish_application` table

| Rule ID | Field | Validation Rule | Error Message | DB Column (Max Length) |
|---------|-------|-----------------|---------------|------------------------|
| FE-080 | Block/House No | Required, max 10 chars | Please enter block/house number | furnish_mail_blk_no (10) |
| FE-081 | Floor | Optional, max 3 chars | - | furnish_mail_floor (3) |
| FE-082 | Unit No | Optional, max 5 chars | - | furnish_mail_unit_no (5) |
| FE-083 | Street Name | Required, max 32 chars | Please enter street name | furnish_mail_street_name (32) |
| FE-084 | Building Name | Optional, max 65 chars | - | furnish_mail_bldg_name (65) |
| FE-085 | Postal Code | Required, 6 digits | Please enter valid postal code | furnish_mail_postal_code (6) |

---

## 3. Backend Validations

### 3.1 Bean Validations (Request Format)

| Rule ID | Field | Annotation | Validation | Error Message |
|---------|-------|------------|------------|---------------|
| BE-001 | txnNo | @NotBlank | Not null/empty | Transaction number is required |
| BE-002 | noticeNo | @NotBlank | Not null/empty | Notice number is required |
| BE-003 | vehicleNo | @NotBlank | Not null/empty | Vehicle number is required |
| BE-004 | offenceDate | @NotNull | Not null | Offence date is required |
| BE-005 | ppCode | @NotBlank | Not null/empty | Car park code is required |
| BE-006 | ppName | @NotBlank | Not null/empty | Car park name is required |
| BE-007 | furnishName | @NotBlank | Not null/empty | Furnished person name is required |
| BE-008 | furnishIdType | @NotBlank | Not null/empty | Furnished person ID type is required |
| BE-009 | furnishIdNo | @NotBlank | Not null/empty | Furnished person ID number is required |
| BE-010 | furnishMailBlkNo | @NotBlank | Not null/empty | Block/house number is required |
| BE-011 | furnishMailStreetName | @NotBlank | Not null/empty | Street name is required |
| BE-012 | furnishMailPostalCode | @NotBlank | Not null/empty | Postal code is required |
| BE-013 | ownerDriverIndicator | @NotBlank | Not null/empty | Owner/driver indicator is required (H or D) |
| BE-014 | hirerOwnerRelationship | @NotBlank | Not null/empty | Hirer/owner relationship is required |
| BE-015 | quesOneAns | @NotBlank | Not null/empty | Question 1 answer is required |
| BE-016 | quesTwoAns | @NotBlank | Not null/empty | Question 2 answer is required |
| BE-017 | ownerName | @NotBlank | Not null/empty | Owner name is required |
| BE-018 | ownerIdNo | @NotBlank | Not null/empty | Owner ID number is required |

### 3.2 Business Validations

| Rule ID | Validation | Query/Logic | Error Type | Error Message |
|---------|------------|-------------|------------|---------------|
| BE-020 | Notice exists | `SELECT notice_no, vehicle_no, last_processing_stage FROM ocms_valid_offence_notice WHERE notice_no = ?` | IllegalArgumentException | Notice number not found: {noticeNo} |
| BE-021 | Vehicle number matches | `notice.vehicleNo == request.vehicleNo` | IllegalArgumentException | Vehicle number mismatch for notice: {noticeNo} |
| BE-022 | Valid owner/driver indicator | `indicator IN ('H', 'D')` | IllegalArgumentException | Invalid owner/driver indicator. Must be 'H' or 'D' |
| BE-023 | Application not exists | `SELECT txn_no, status FROM ocms_furnish_application WHERE txn_no = ?` | BusinessError | Furnish application already exists |
| BE-024 | Application not processed | `application.status == 'P'` | BusinessError | Furnish application has already been processed |

---

## 4. Auto-Approval Conditions

### 4.1 Auto-Approval Check Sequence

Auto-approval checks are performed in sequence. If ANY check fails, the submission is routed for manual review.

| Order | Check ID | Check Name | Pass Condition | Fail Action |
|-------|----------|------------|----------------|-------------|
| 1 | AA-001 | Permanent Suspension Check | Notice has NO active PS suspension | Set status = 'P', reason = "Notice is permanently suspended" |
| 2 | AA-002 | HST ID Check | Furnished ID NOT in HST database | Set status = 'P', reason = "HST ID" |
| 3 | AA-003 | FIN/Passport Check | ID type NOT FIN or Passport | Set status = 'P', reason = "FIN or Passport ID" |
| 4 | AA-004 | Furnishable Stage Check | Notice stage in furnishable list | Set status = 'P', reason = "Notice is no longer at furnishable stage" |
| 5 | AA-005 | Prior Submission Check | No prior submissions for notice | Set status = 'P', reason = "Prior submission under notice" |
| 6 | AA-006 | Existing Hirer/Driver Check | No existing H/D with any ID | Set status = 'P', reason = "Hirer/Driver's particulars already present" |
| 7 | AA-007 | Furnished ID Exists Check | Furnished ID NOT in H/D details | Set status = 'P', reason = "Furnished ID already in Hirer or Driver Details" |
| 8 | AA-008 | Owner Current Offender Check | Owner/Hirer IS current offender | Set status = 'P', reason = "Owner/Hirer no longer current offender" |

### 4.2 Auto-Approval Check Details

#### AA-001: Permanent Suspension Check

```sql
SELECT COUNT(*) FROM ocms_suspended_notice
WHERE notice_no = :noticeNo
  AND suspension_type = 'PS'
  AND (date_of_revival IS NULL OR date_of_revival = '')
```

| Condition | Result |
|-----------|--------|
| COUNT = 0 | PASS - Continue to next check |
| COUNT > 0 | FAIL - Route to manual review |

#### AA-002: HST ID Check [ASSUMPTION - Not implemented in code]

```sql
SELECT COUNT(*) FROM hst_id_database
WHERE id_no = :furnishIdNo
```

| Condition | Result |
|-----------|--------|
| COUNT = 0 | PASS - Not an HST ID |
| COUNT > 0 | FAIL - Is an HST ID, requires manual review |

#### AA-003: FIN/Passport Check [ASSUMPTION - Not implemented in code]

| Condition | Result |
|-----------|--------|
| furnishIdType NOT IN ('FIN', 'PASSPORT') | PASS |
| furnishIdType IN ('FIN', 'PASSPORT') | FAIL - Requires manual review |

#### AA-004: Furnishable Stage Check

```sql
SELECT last_processing_stage FROM ocms_valid_offence_notice
WHERE notice_no = :noticeNo
```

| Stage | Furnishable |
|-------|-------------|
| ENA | Yes |
| RD1 | Yes |
| RD2 | Yes |
| RR3 | Yes |
| CPC and above | No |

#### AA-005: Prior Submission Check

```sql
SELECT COUNT(*) FROM ocms_furnish_application
WHERE notice_no = :noticeNo
  AND txn_no != :currentTxnNo
```

| Condition | Result |
|-----------|--------|
| COUNT = 0 | PASS - No prior submissions |
| COUNT > 0 | FAIL - Has prior submissions |

#### AA-006: Existing Hirer/Driver Check

```sql
SELECT COUNT(*) FROM ocms_offence_notice_owner_driver
WHERE notice_no = :noticeNo
  AND owner_driver_indicator = :ownerDriverIndicator
```

| Condition | Result |
|-----------|--------|
| COUNT = 0 | PASS - No existing H/D record |
| COUNT > 0 | FAIL - H/D particulars exist |

#### AA-007: Furnished ID Exists Check

```sql
SELECT COUNT(*) FROM ocms_offence_notice_owner_driver
WHERE notice_no = :noticeNo
  AND id_no = :furnishIdNo
```

| Condition | Result |
|-----------|--------|
| COUNT = 0 | PASS - Furnished ID not in records |
| COUNT > 0 | FAIL - Furnished ID already exists |

#### AA-008: Owner Current Offender Check

```sql
SELECT COUNT(*) FROM ocms_offence_notice_owner_driver
WHERE notice_no = :noticeNo
  AND id_no = :ownerIdNo
  AND offender_indicator = 'Y'
```

| Condition | Result |
|-----------|--------|
| COUNT > 0 | PASS - Owner is current offender |
| COUNT = 0 | FAIL - Owner no longer current offender |

---

## 5. Manual Review Conditions

### 5.1 Reasons for Manual Review

| Reason Code | Reason Description | Trigger Condition |
|-------------|-------------------|-------------------|
| MR-001 | Notice is permanently suspended | AA-001 failed |
| MR-002 | HST ID | AA-002 failed |
| MR-003 | FIN or Passport ID | AA-003 failed |
| MR-004 | Notice is no longer at furnishable stage | AA-004 failed |
| MR-005 | Prior submission under notice | AA-005 failed |
| MR-006 | Hirer/Driver's particulars already present (any ID) | AA-006 failed |
| MR-007 | Furnished ID already in Hirer or Driver Details | AA-007 failed |
| MR-008 | Owner/Hirer (submitter) no longer current offender | AA-008 failed |

### 5.2 Manual Review Status Transitions

| Current Status | Action | New Status | Post-Action |
|----------------|--------|------------|-------------|
| P (Pending) | Approve as Hirer | A (Approved) | Create Hirer record, set as current offender |
| P (Pending) | Approve as Driver | A (Approved) | Create Driver record, set as current offender |
| P (Pending) | Reject | R (Rejected) | Send notification, allow resubmission |

---

## 6. Business Rule Conditions

### 6.1 Furnishable Notice Rules

| Rule ID | Rule Description | Condition |
|---------|------------------|-----------|
| BR-001 | Only Parking Offence and Payment Evasion notices are furnishable | `offence_notice_type IN ('O', 'E')` |
| BR-002 | Next Processing Stage must be up to RR3 | `next_processing_stage IN ('ENA', 'RD1', 'RD2', 'RR3')` |
| BR-003 | No active Permanent Suspension | `suspension_type = 'PS' AND date_of_revival IS NULL` must be FALSE |
| BR-004 | Furnisher must be current Offender (Owner or Hirer) | `owner_driver_indicator IN ('O', 'H') AND offender_indicator = 'Y'` |
| BR-005 | Furnisher must be NRIC, FIN or UEN holder | Login via Singpass/Corppass required |

### 6.2 Offender Record Rules

| Rule ID | Rule Description | Condition |
|---------|------------------|-----------|
| BR-010 | Only one current offender per notice | `COUNT(offender_indicator = 'Y') = 1` per notice |
| BR-011 | Owner cannot be removed | Owner record always exists |
| BR-012 | Hirer/Driver can be added | Insert new record with owner_driver_indicator = H/D |
| BR-013 | Current offender transfer | Set old offender_indicator = 'N', new = 'Y' |

### 6.3 Processing Stage Transition Rules

| Rule ID | From Stage | Action | To Stage |
|---------|------------|--------|----------|
| BR-020 | Any furnishable | Furnish Driver approved | DN (Driver Notice) |
| BR-021 | Any furnishable | Furnish Hirer approved | RD (Redirect) |
| BR-022 | Any | Permanent Suspension | PS |

### 6.4 Suspension Rules

| Rule ID | Rule Description | Condition |
|---------|------------------|-----------|
| BR-030 | Apply TS-PDP on pending submission | When status = 'P', apply TS-PDP suspension |
| BR-031 | Revive TS-PDP on approval | When status changes 'P' → 'A', revive TS-PDP |
| BR-032 | Keep TS-PDP on rejection | When status = 'R', TS-PDP remains for resubmission |

---

## 7. Processing Stage Conditions

### 7.1 Furnishable Stages

| Stage Code | Stage Name | Furnishable | Notes |
|------------|------------|-------------|-------|
| ENA | Enforcement Notice A | Yes | Initial stage |
| RD1 | Reminder 1 | Yes | First reminder |
| RD2 | Reminder 2 | Yes | Second reminder |
| RR3 | Reminder 3 | Yes | Third reminder |
| CPC | Court Proceedings Commenced | No | Beyond furnishable |
| PS | Permanent Suspension | No | Suspended |

### 7.2 Stage Transition on Furnish

| Current Stage | Furnish Type | New Stage | Condition |
|---------------|--------------|-----------|-----------|
| ENA/RD1/RD2/RR3 | Driver | DN | Auto-approved or OIC approved as Driver |
| ENA/RD1/RD2/RR3 | Hirer | RD | Auto-approved or OIC approved as Hirer |
| Any | - | PS | Permanent Suspension applied |

---

## 8. Decision Trees

### 8.1 Furnish Submission Decision Tree

```
START: Receive Furnish Submission
│
├─► Validate Request Format (Bean Validation)
│   ├─► FAIL → Return VALIDATION_ERROR (400)
│   └─► PASS → Continue
│
├─► Validate Business Rules
│   ├─► Notice exists?
│   │   ├─► NO → Return "Notice not found" (400)
│   │   └─► YES → Continue
│   │
│   ├─► Vehicle number matches?
│   │   ├─► NO → Return "Vehicle number mismatch" (400)
│   │   └─► YES → Continue
│   │
│   └─► Valid owner/driver indicator?
│       ├─► NO → Return "Invalid indicator" (400)
│       └─► YES → Continue
│
├─► Create Furnish Application Record
│   ├─► FAIL → Return TECHNICAL_ERROR (500)
│   └─► SUCCESS → Continue
│
├─► Perform Auto-Approval Checks (8 checks in sequence)
│   │
│   ├─► ALL PASS
│   │   ├─► Set status = 'A' (Approved)
│   │   ├─► Create Hirer/Driver record
│   │   ├─► Set as current offender
│   │   ├─► Update processing stage
│   │   ├─► Apply TS-PDP suspension
│   │   └─► Return SUCCESS (autoApproved: true)
│   │
│   └─► ANY FAIL
│       ├─► Set status = 'P' (Pending)
│       ├─► Set reason_for_review
│       ├─► Apply TS-PDP suspension
│       └─► Return SUCCESS (requiresManualReview: true)
│
END
```

### 8.2 Officer Approval Decision Tree

```
START: Receive Approval Request
│
├─► Validate Request
│   ├─► txnNo exists?
│   │   ├─► NO → Return NOT_FOUND (404)
│   │   └─► YES → Continue
│   │
│   └─► Status = 'P'?
│       ├─► NO → Return "Already processed" (409)
│       └─► YES → Continue
│
├─► Update Furnish Application
│   ├─► Set status = 'A'
│   ├─► Set remarks
│   └─► Set upd_user_id, upd_date
│
├─► Create/Update Hirer/Driver Record
│   ├─► Insert into ocms_offence_notice_owner_driver
│   ├─► Set offender_indicator = 'Y'
│   └─► Update previous offender to 'N'
│
├─► Update Address Record
│   └─► Insert into ocms_offence_notice_owner_driver_addr
│
├─► Update Processing Stage
│   ├─► If Driver → Set stage = 'DN'
│   └─► If Hirer → Set stage = 'RD'
│
├─► Revive TS-PDP Suspension
│   └─► Set date_of_revival = NOW()
│
├─► Send Notifications (if requested)
│   ├─► Email to Owner
│   ├─► Email to Furnished Person
│   └─► SMS to Furnished Person
│
└─► Return SUCCESS
│
END
```

### 8.3 Officer Rejection Decision Tree

```
START: Receive Rejection Request
│
├─► Validate Request
│   ├─► txnNo exists?
│   │   ├─► NO → Return NOT_FOUND (404)
│   │   └─► YES → Continue
│   │
│   └─► Status = 'P'?
│       ├─► NO → Return "Already processed" (409)
│       └─► YES → Continue
│
├─► Update Furnish Application
│   ├─► Set status = 'R'
│   ├─► Set remarks
│   └─► Set upd_user_id, upd_date
│
├─► Keep TS-PDP Suspension Active
│   └─► (No change - allows resubmission)
│
├─► Send Notifications (if requested)
│   └─► Email to Owner (with rejection reason)
│
├─► Resend Notice to eService Portal
│   └─► Sync notice back for resubmission
│
└─► Return SUCCESS
│
END
```

### 8.4 Cron Sync Decision Tree

```
START: Cron Job Triggered
│
├─► Check sync enabled?
│   ├─► NO → Log "disabled", EXIT
│   └─► YES → Continue
│
├─► [MISSING] Check for locked/running job?
│   ├─► YES → Log, abort, EXIT
│   └─► NO → Continue
│
├─► Query unsynced records (is_sync = 'N')
│   ├─► COUNT = 0 → Log "no records", EXIT
│   └─► COUNT > 0 → Continue
│
├─► FOR EACH record:
│   │
│   ├─► Check if exists in Intranet DB
│   │   ├─► YES → UPDATE existing record
│   │   └─► NO → INSERT new record
│   │
│   ├─► Save to Intranet DB
│   │   ├─► FAIL → Log error, increment failureCount, CONTINUE
│   │   └─► SUCCESS → Continue
│   │
│   ├─► Set is_sync = 'Y' in Internet DB
│   │   ├─► FAIL → Log error, CONTINUE
│   │   └─► SUCCESS → increment successCount
│   │
│   └─► NEXT record
│
├─► Sync Supporting Documents (same pattern)
│
├─► [MISSING] Consolidate errors
│
├─► [MISSING] Send error email if failures > 0
│
├─► [MISSING] Log to batch job table
│
└─► Return result
│
END
```

---

## 9. Error Handling Conditions

### 9.1 Retry Conditions

> **Implementation Required:** All retry logic below MUST be implemented by the programmer. These are mandatory requirements per Yi Jie technical standards.

| Operation | Retry Count | Retry Condition | Implementation Guidance |
|-----------|-------------|-----------------|-------------------------|
| DB Connection | 3 | Connection timeout/failure | Use Spring Retry or custom retry loop with exponential backoff (1s, 2s, 4s) |
| Create Record | 3 | Insert failure (non-duplicate) | Catch DataAccessException, retry if not constraint violation |
| Patch Sync Flag | 3 | Update failure | Retry on timeout, skip on business error |
| Blob Sync | 3 | Upload/download failure | Retry on IOException, log and skip after 3 failures |

#### Retry Pattern Example

```java
// Recommended retry implementation
@Retryable(
    value = {DataAccessException.class, IOException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
public void syncRecord(FurnishApplication record) {
    // Implementation
}

@Recover
public void recoverSync(Exception e, FurnishApplication record) {
    // Add to error list, send email alert after all retries exhausted
    errorList.add(new SyncError(record.getTxnNo(), e.getMessage()));
}
```

#### Post-Retry Failure Handling

| After 3 Failures | Action |
|------------------|--------|
| Add to error list | Collect all failed records with error message |
| Continue processing | Do not stop batch for single record failure |
| Send email alert | After batch completes, send consolidated error email |
| Log to batch table | Record batch status with failure count |

### 9.2 Skip Conditions

| Condition | Action | Log Level |
|-----------|--------|-----------|
| Record already exists (duplicate) | Skip, add to error list | WARN |
| Validation failure | Skip, add to error list | WARN |
| Max retries exceeded | Skip, add to error list | ERROR |

### 9.3 Abort Conditions

| Condition | Action |
|-----------|--------|
| Concurrent job running | Abort new job, log warning |
| Critical DB connection failure | Abort job, send alert email |
| Configuration error | Abort job, log error |

---

## 10. Assumptions Log

### 10.1 Code Implementation Assumptions

| ID | Assumption | Source | Impact | Status |
|----|------------|--------|--------|--------|
| ASMP-001 | HST ID check is not implemented in current code | Code Analysis | Auto-approval may pass for HST IDs | Needs Implementation |
| ASMP-002 | FIN/Passport ID check is not implemented | Code Analysis | Auto-approval may pass for FIN/Passport | Needs Implementation |
| ASMP-003 | Prior submission check method returns false (stub) | Code Analysis | All submissions treated as first-time | Needs Implementation |
| ASMP-004 | Job locking mechanism not implemented | Code Analysis | Concurrent jobs may run | Needs Implementation |
| ASMP-005 | Retry mechanism (x3) not implemented | Code Analysis | Single failure will skip record | Needs Implementation |
| ASMP-006 | Error email notification not implemented | Code Analysis | No alerts on sync failures | Needs Implementation |
| ASMP-007 | Batch job status table logging not implemented | Code Analysis | No job history tracking | Needs Implementation |

### 10.2 Stage Code Assumptions

| ID | Assumption | FD Value | Code Value | Impact |
|----|------------|----------|------------|--------|
| ASMP-010 | Furnishable stage codes differ | ENA, RD1, RD2, RR3 | PRE, 1ST, 2ND, 3RD, CPC | Stage validation mismatch |

### 10.3 Status Code Assumptions

| ID | Assumption | FD Value | Code Value | Impact |
|----|------------|----------|------------|--------|
| ASMP-020 | Resubmission status code differs | S | Resubmission | Status comparison may fail |
| ASMP-021 | Null status for new submission | N | Not defined | Initial status handling unclear |

### 10.4 Business Logic Assumptions

| ID | Assumption | Description |
|----|------------|-------------|
| ASMP-030 | Auto-approval runs in API, not Cron | FD shows auto-approval in Cron flow, but code has it in submission API |
| ASMP-031 | PDD date check not implemented | Flowchart shows "Is PDD > current date?" check, not found in code |
| ASMP-032 | Error list consolidation not implemented | Only counts tracked, no detailed error list |

---

## Appendix A: Validation Rule Summary

| Category | Total Rules | Implemented | Missing |
|----------|-------------|-------------|---------|
| Frontend Validations | 40 | N/A (Frontend) | N/A |
| Bean Validations | 18 | 18 | 0 |
| Auto-Approval Checks | 8 | 5 | 3 |
| Business Rules | 15 | ~10 | ~5 |
| Retry Logic | 4 | 0 | 4 |

---

## Appendix B: Cross-Reference to User Stories

| User Story | Validation Rules |
|------------|------------------|
| OCMS41.4-41.7 | BE-001 to BE-018, AA-001 to AA-008 |
| OCMS41.9-41.12 | FE-060 to FE-065 |
| OCMS41.15-41.23 | Approval decision tree |
| OCMS41.24-41.33 | Rejection decision tree |
| OCMS41.43-41.51 | BR-001 to BR-005, FE-070 to FE-085 |

---

*Document generated based on OCMS 41 Functional Document v1.1 and backend code analysis*

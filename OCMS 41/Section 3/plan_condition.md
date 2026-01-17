# OCMS 41 Section 3 - Staff Portal Manual Review Conditions & Validations

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
2. [Frontend Validations](#2-frontend-validations)
3. [Backend Validations](#3-backend-validations)
4. [Business Rule Conditions](#4-business-rule-conditions)
5. [Decision Trees](#5-decision-trees)
6. [Email/SMS Conditions](#6-emailsms-conditions)
7. [Error Handling Conditions](#7-error-handling-conditions)
8. [Assumptions Log](#8-assumptions-log)

---

## 1. Overview

This document defines all validation rules, conditions, and decision logic for OCMS 41 Section 3 - Staff Portal Manual Review of Furnished Submissions.

### 1.1 Actors

| Actor | Description |
|-------|-------------|
| OIC | Officer-In-Charge who reviews furnished submissions |

### 1.2 Validation Layers

| Layer | Responsibility | Implementation |
|-------|----------------|----------------|
| Frontend | Form field validation, UI state management | Staff Portal |
| API Gateway | Authentication, authorization | JWT validation |
| Backend - Bean | Request format validation | Jakarta Bean Validation |
| Backend - Business | Business rule validation | Service layer |
| Database | Data integrity constraints | Primary keys, foreign keys |

---

## 2. Frontend Validations

### 2.1 Summary List Page

#### 2.1.1 Search Parameters

| Rule ID | Field | Validation Rule | Error Message |
|---------|-------|-----------------|---------------|
| FE-S01 | Notice No | Optional, alphanumeric, exact match | - |
| FE-S02 | Vehicle No | Optional, alphanumeric, partial match | - |
| FE-S03 | Submitter's ID No | Optional, alphanumeric (NRIC/FIN/UEN/Passport) | - |
| FE-S04 | Submission Date From | Optional, date picker, format DD/MM/YYYY | - |
| FE-S05 | Submission Date To | Optional, date picker, format DD/MM/YYYY, >= From | End date must be after start date |
| FE-S06 | Status | Optional, dropdown: Pending, Resubmission, Approved, Rejected | - |

#### 2.1.2 Default Behavior

| Rule ID | Condition | Default Behavior |
|---------|-----------|------------------|
| FE-D01 | Page Load | Display submissions with status = 'Pending' OR 'Resubmission' |
| FE-D02 | Sort Order | Default sort by Submission Date DESC |
| FE-D03 | Pagination | Default page size = 10 |

---

### 2.2 Submission Details Screen

#### 2.2.1 Notice Details Sub-Section (Read-Only)

| Rule ID | Field | Display Condition |
|---------|-------|-------------------|
| FE-N01 | Notice No | Always displayed, non-editable |
| FE-N02 | Vehicle No | Always displayed, non-editable |
| FE-N03 | Car Park | Always displayed, non-editable |
| FE-N04 | Offence Date & Time | Always displayed, non-editable |

#### 2.2.2 Furnished Details Sub-Section (Read-Only)

| Rule ID | Field | Display Condition |
|---------|-------|-------------------|
| FE-F01 | Submission Date & Time | Always displayed, non-editable |
| FE-F02 | Current Processing Stage | Always displayed, non-editable |
| FE-F03 | Status | Always displayed, non-editable |
| FE-F04 | Reason for Review | Always displayed, non-editable |
| FE-F05 | Submitter's Particulars | Always displayed, non-editable |
| FE-F06 | Furnished Person's Particulars | Always displayed, non-editable |

#### 2.2.3 Editable Fields

> **Source:** Data Dictionary - `ocms_furnish_application` table

| Rule ID | Field | Validation Rule | Error Message | DB Column (Max Length) |
|---------|-------|-----------------|---------------|------------------------|
| FE-E01 | Submitter's Email | Required, max 320 chars, email format | Invalid email format | furnish_email_addr (320) |
| FE-E02 | Submitter's Country Code | Required, max 4 chars | Invalid country code | furnish_tel_country_code (4) |
| FE-E03 | Submitter's Contact No | Required, max 12 chars | Invalid contact number | furnish_tel_no (12) |

#### 2.2.4 OIC Review Sub-Section

> **Source:** Data Dictionary - `ocms_furnish_application` table

| Rule ID | Field | Validation Rule | Error Message | DB Column (Max Length) |
|---------|-------|-----------------|---------------|------------------------|
| FE-R01 | Decision | Required, radio button (Approve Hirer/Approve Driver/Reject) | Please select a decision | status (1) |
| FE-R02 | Remarks | Optional, max 200 chars | Maximum 200 characters allowed | remarks (200) |
| FE-R03 | Send Email/SMS | Optional, checkbox, default unchecked | - | - |

---

### 2.3 Compose Email/SMS Sub-Tab

#### 2.3.1 Message Mode Field

| Rule ID | Field | Validation Rule | Error Message |
|---------|-------|-----------------|---------------|
| FE-M01 | Message Mode | Required, radio button (Email/SMS/Both) | Please select message mode |

#### 2.3.2 Email Fields

| Rule ID | Field | Validation Rule | Error Message |
|---------|-------|-----------------|---------------|
| FE-EM01 | Recipient Type | Required when mode=Email, dropdown (Submitter/Furnished Person/Both) | Please select recipient |
| FE-EM02 | Email Subject | Required when mode=Email, max 200 chars | Please enter email subject |
| FE-EM03 | Email Body | Required when mode=Email, max 2000 chars | Please enter email body |
| FE-EM04 | Template Selection | Optional, dropdown | - |

#### 2.3.3 SMS Fields

| Rule ID | Field | Validation Rule | Error Message |
|---------|-------|-----------------|---------------|
| FE-SM01 | Recipient | Required when mode=SMS, auto-populated to Furnished Person | - |
| FE-SM02 | SMS Body | Required when mode=SMS, max 160 chars | Please enter SMS message |

---

### 2.4 Summary Page

#### 2.4.1 Pre-Submit Validations

| Rule ID | Validation | Error Action |
|---------|------------|--------------|
| FE-PS01 | Decision must be selected | Redirect to Submission Details, prompt to select decision |
| FE-PS02 | If Send Email/SMS checked, message must be composed | Redirect to Compose page |

#### 2.4.2 Confirmation Prompt

| Rule ID | Trigger | Behavior |
|---------|---------|----------|
| FE-CP01 | User clicks Submit | Display confirmation dialog |
| FE-CP02 | User clicks Confirm | Send to backend |
| FE-CP03 | User clicks Cancel | Return to Submission Details |

---

## 3. Backend Validations

### 3.1 Bean Validations (Request Format)

| Rule ID | Field | Annotation | Error Message |
|---------|-------|------------|---------------|
| BE-001 | txnNo | @NotBlank | Transaction number is required |
| BE-002 | officerId | @NotBlank | Officer ID is required |
| BE-003 | rejectionReason | @NotBlank (for reject) | Rejection reason is required |

### 3.2 Business Validations

| Rule ID | Validation | Query/Logic | Error Type | Error Message |
|---------|------------|-------------|------------|---------------|
| BE-010 | Application exists | `SELECT txn_no, status, notice_no FROM ocms_furnish_application WHERE txn_no = ?` | NOT_FOUND | Furnished application not found: {txnNo} |
| BE-011 | Status is Pending or Resubmission | `status IN ('P', 'S')` | ALREADY_PROCESSED | Submission has already been processed |
| BE-012 | Notice is still furnishable | Check notice processing stage | NOTICE_NOT_FURNISHABLE | Notice is no longer at furnishable stage |
| BE-013 | Notice not permanently suspended | Check active PS suspension | PERMANENTLY_SUSPENDED | Notice is permanently suspended |

---

## 4. Business Rule Conditions

### 4.1 Furnishability Check

| Rule ID | Condition | Action |
|---------|-----------|--------|
| BR-001 | Notice stage NOT in furnishable list | Display prompt: "Notice is no longer furnishable. Reject?" |
| BR-002 | Notice has active Permanent Suspension | Display warning, allow continue or reject |
| BR-003 | User selects "Reject" on prompt | Set decision = Reject automatically |
| BR-004 | User selects "Continue Review" | Allow OIC to proceed with review |

### 4.2 Processing Stage Changes on Approval

| Rule ID | Decision | Processing Stage Change |
|---------|----------|------------------------|
| BR-010 | Approve Hirer | Notice stage changes to RD (Reminder Driver/Hirer) |
| BR-011 | Approve Driver | Notice stage changes to DN (Driver Notice) |
| BR-012 | Reject | No stage change, notice resent to eService |

### 4.3 Offender Record Updates on Approval

| Rule ID | Action | Description |
|---------|--------|-------------|
| BR-020 | Create Hirer/Driver Record | Insert into ocms_offence_notice_owner_driver |
| BR-021 | Create Address Record | Insert into ocms_offence_notice_owner_driver_addr |
| BR-022 | Set Current Offender | Set offender_indicator = 'Y' |
| BR-023 | Update Previous Offender | Set previous offender's offender_indicator = 'N' |
| BR-024 | Revive TS-PDP Suspension | Set date_of_revival = NOW() |

### 4.4 Rejection Actions

| Rule ID | Action | Description |
|---------|--------|-------------|
| BR-030 | Keep Suspension Active | TS-PDP suspension remains (allows resubmission) |
| BR-031 | Resend to eService | Sync notice back to Internet for resubmission |
| BR-032 | Update Status | Set furnish application status = 'R' |

---

## 5. Decision Trees

### 5.1 View Furnished Submission Decision Tree

```
START: OIC clicks on Notice No. from Summary List
│
├─► Retrieve Furnished Submission details from Intranet DB
│   ├─► FAIL → Display error message
│   └─► SUCCESS → Continue
│
├─► Retrieve latest Notice details
│   └─► Get suspension info, Owner/Hirer/Driver info
│
├─► Check if Notice is still furnishable
│   │
│   ├─► NOT FURNISHABLE
│   │   ├─► Display prompt: "Notice is no longer furnishable"
│   │   ├─► User clicks "Reject" → Auto-set decision = Reject, continue to Step 10
│   │   └─► User clicks "Continue Review" → Continue to Step 5
│   │
│   └─► FURNISHABLE → Continue to Step 5
│
├─► Display Furnished Submission Details screen
│   ├─► Notice details (read-only)
│   ├─► Furnished details (read-only)
│   └─► OIC Review section (editable)
│
END
```

### 5.2 Submit Approval Decision Tree

```
START: Receive Approval Request
│
├─► Validate Request Format
│   ├─► txnNo blank? → Return VALIDATION_ERROR
│   ├─► officerId blank? → Return VALIDATION_ERROR
│   └─► PASS → Continue
│
├─► Validate Business Rules
│   ├─► Application exists?
│   │   ├─► NO → Return NOT_FOUND (404)
│   │   └─► YES → Continue
│   │
│   ├─► Status = 'P' or 'S'?
│   │   ├─► NO → Return ALREADY_PROCESSED (409)
│   │   └─► YES → Continue
│   │
│   └─► Notice still furnishable?
│       ├─► NO → Return NOTICE_NOT_FURNISHABLE (409)
│       └─► YES → Continue
│
├─► Update Furnish Application
│   ├─► Set status = 'A'
│   ├─► Set remarks
│   ├─► Set upd_user_id = officerId
│   └─► Set upd_date = NOW()
│
├─► Create/Update Hirer/Driver Record
│   ├─► Insert into ocms_offence_notice_owner_driver
│   ├─► Set offender_indicator = 'Y'
│   └─► Update previous offender to 'N'
│
├─► Create Address Record
│   └─► Insert into ocms_offence_notice_owner_driver_addr
│
├─► Update Processing Stage
│   ├─► If hirerDriverIndicator = 'H' → Set stage = 'RD'
│   └─► If hirerDriverIndicator = 'D' → Set stage = 'DN'
│
├─► Revive TS-PDP Suspension
│   └─► Set date_of_revival = NOW()
│
├─► Sync to Internet DB
│   ├─► Update eocms_furnish_application
│   ├─► Update eocms_offence_notice_owner_driver
│   └─► Update eocms_offence_notice_owner_driver_addr
│
├─► Send Notifications (if requested)
│   ├─► sendEmailToOwner = true → Send email to owner
│   ├─► sendEmailToFurnished = true → Send email to furnished person
│   └─► sendSmsToFurnished = true → Send SMS to furnished person
│
└─► Return SUCCESS
│
END
```

### 5.3 Submit Rejection Decision Tree

```
START: Receive Rejection Request
│
├─► Validate Request Format
│   ├─► txnNo blank? → Return VALIDATION_ERROR
│   ├─► officerId blank? → Return VALIDATION_ERROR
│   ├─► rejectionReason blank? → Return VALIDATION_ERROR
│   └─► PASS → Continue
│
├─► Validate Business Rules
│   ├─► Application exists?
│   │   ├─► NO → Return NOT_FOUND (404)
│   │   └─► YES → Continue
│   │
│   └─► Status = 'P' or 'S'?
│       ├─► NO → Return ALREADY_PROCESSED (409)
│       └─► YES → Continue
│
├─► Update Furnish Application
│   ├─► Set status = 'R'
│   ├─► Set rejection_reason
│   ├─► Set remarks
│   ├─► Set upd_user_id = officerId
│   └─► Set upd_date = NOW()
│
├─► Keep TS-PDP Suspension Active
│   └─► (No change - allows owner to resubmit)
│
├─► Sync to Internet DB
│   └─► Update eocms_furnish_application status = 'R'
│
├─► Resend Notice to eService Portal
│   └─► Make notice available for resubmission
│
├─► Send Notifications (if requested)
│   └─► sendEmailToOwner = true → Send rejection email to owner
│
└─► Return SUCCESS
│
END
```

---

## 6. Email/SMS Conditions

### 6.1 Email Template Selection

| Template ID | Decision | Recipient | Description |
|-------------|----------|-----------|-------------|
| APPROVED_HIRER_INDIVIDUAL | Approve Hirer | Owner | Hirer approval notification (individual submitter) |
| APPROVED_HIRER_COMPANY | Approve Hirer | Owner | Hirer approval notification (company submitter) |
| APPROVED_DRIVER_INDIVIDUAL | Approve Driver | Owner | Driver approval notification (individual) |
| APPROVED_DRIVER_COMPANY | Approve Driver | Owner | Driver approval notification (company) |
| APPROVED_FURNISHED_PERSON | Approve | Furnished Person | Notification to furnished person |
| REJECTED_DOCS_REQUIRED | Reject | Owner | Rejection - documents required |
| REJECTED_MULTIPLE_HIRERS | Reject | Owner | Rejection - multiple hirers detected |
| REJECTED_RENTAL_DISCREPANCY | Reject | Owner | Rejection - rental period discrepancy |
| REJECTED_GENERAL | Reject | Owner | General rejection notification |

### 6.2 Notification Conditions

| Rule ID | Condition | Action |
|---------|-----------|--------|
| NT-001 | sendEmailToOwner = true AND owner email exists | Send email to owner |
| NT-002 | sendEmailToFurnished = true AND furnished email exists | Send email to furnished person |
| NT-003 | sendSmsToFurnished = true AND furnished mobile exists | Send SMS to furnished person |
| NT-004 | Email/SMS send fails | Log error, continue processing (non-blocking) |

### 6.3 Email/SMS Data Mapping

| Placeholder | Source Field |
|-------------|--------------|
| {{submitterName}} | furnish_application.owner_name |
| {{noticeNo}} | furnish_application.notice_no |
| {{vehicleNo}} | furnish_application.vehicle_no |
| {{furnishedName}} | furnish_application.furnish_name |
| {{offenceDate}} | notice.offence_date |
| {{decisionDate}} | NOW() |
| {{rejectionReason}} | request.rejectionReason |

---

## 7. Error Handling Conditions

### 7.1 Validation Errors (HTTP 400)

| Error Code | Field | Message |
|------------|-------|---------|
| VALIDATION_ERROR | txnNo | Transaction number is required |
| VALIDATION_ERROR | officerId | Officer ID is required |
| VALIDATION_ERROR | rejectionReason | Rejection reason is required |

### 7.2 Business Errors (HTTP 409)

| Reason Code | Message | Handling |
|-------------|---------|----------|
| ALREADY_PROCESSED | Submission has already been processed | Refresh list, notify OIC |
| NOT_PENDING | Submission is not in pending status | Refresh list, notify OIC |
| NOTICE_NOT_FURNISHABLE | Notice is no longer at furnishable stage | Prompt to reject |
| PERMANENTLY_SUSPENDED | Notice is permanently suspended | Display warning |

### 7.3 Technical Errors (HTTP 500)

| Error Code | Message | Handling |
|------------|---------|----------|
| DB_UPDATE | Failed to update database | Rollback, retry, log error |
| EMAIL_SEND | Failed to send email notification | Log error, continue (non-blocking) |
| SMS_SEND | Failed to send SMS notification | Log error, continue (non-blocking) |
| SYNC_INTERNET | Failed to sync outcome to Internet DB | Log error, retry scheduled |

---

## 8. Assumptions Log

### 8.1 Code Implementation Assumptions

| ID | Assumption | Basis | Impact if Wrong |
|----|------------|-------|-----------------|
| ASM-001 | Email/SMS send failure is non-blocking | Common pattern for notification systems | May need to add retry queue |
| ASM-002 | Internet DB sync happens synchronously | Code analysis pattern | May need async with retry |
| ASM-003 | Only one OIC can process a submission at a time | Implied by status check | May need optimistic locking |

### 8.2 Functional Assumptions

| ID | Assumption | Basis | Impact if Wrong |
|----|------------|-------|-----------------|
| ASM-010 | OIC must have valid session/JWT | Standard security practice | Need to add auth validation |
| ASM-011 | Furnished person address is optional for approval | FD doesn't mandate | May need additional validation |
| ASM-012 | Rejection allows unlimited resubmissions | FD mentions resubmission | May need resubmission limit |

---

## Appendix A: User Story Mapping

| User Story | Validation Rules |
|------------|------------------|
| OCMS41.9-41.12 | FE-S01 to FE-S06, FE-D01 to FE-D03 |
| OCMS41.13-41.14 | FE-N01 to FE-N04, FE-F01 to FE-F06 |
| OCMS41.15-41.23 | BE-001, BE-002, BE-010 to BE-013, BR-010 to BR-024 |
| OCMS41.24-41.33 | BE-001, BE-002, BE-003, BR-030 to BR-032 |

---

## Appendix B: Status Transitions

| From Status | Action | To Status |
|-------------|--------|-----------|
| Pending (P) | OIC Approve | Approved (A) |
| Pending (P) | OIC Reject | Rejected (R) |
| Resubmission (S) | OIC Approve | Approved (A) |
| Resubmission (S) | OIC Reject | Rejected (R) |

---

*Document generated for OCMS 41 Section 3 condition planning*

# OCMS 41 Section 6 - Validation Conditions

## Document Information

| Field | Value |
|-------|-------|
| Version | 1.0 |
| Date | 2026-01-07 |
| Source | Functional Document v1.1, Section 6 |
| Module | OCMS 41 - Section 6: PLUS External System Integration |

---

## Table of Contents

1. [Frontend Validations (PLUS)](#1-frontend-validations-plus)
2. [Backend Validations (OCMS)](#2-backend-validations-ocms)
3. [Decision Trees](#3-decision-trees)
4. [Assumptions Log](#4-assumptions-log)

---

## 1. Frontend Validations (PLUS)

> Note: These validations are performed by PLUS Staff Portal before calling OCMS APIs.

### 1.1 Update Hirer/Driver Form (Section 6.2.1)

| Field | Rule ID | Validation | Error Message |
|-------|---------|------------|---------------|
| Offender Type | PL-FRM-001 | Required (Hirer/Driver only) | Offender type is required |
| Name | PL-FRM-002 | Required | Name is required |
| Name | PL-FRM-003 | Max 100 characters | Name exceeds maximum length |
| ID Type | PL-FRM-004 | Required | ID Type is required |
| ID Number | PL-FRM-005 | Required | ID Number is required |
| ID Number | PL-FRM-006 | Format check based on ID Type | Invalid {idType} format |
| Entity Type | PL-FRM-007 | Required if ID Type = UEN | Entity Type is required for UEN |
| Email | PL-FRM-008 | Valid email format if provided | Invalid email format |
| Contact No | PL-FRM-009 | Valid format if provided | Invalid contact number |
| Address | PL-FRM-010 | Required | Address is required |
| Postal Code | PL-FRM-011 | 6 digits for Singapore address | Invalid postal code |

### 1.2 Redirect Form (Section 6.3.1)

| Field | Rule ID | Validation | Error Message |
|-------|---------|------------|---------------|
| Target Offender Type | PL-RD-001 | Required (Owner/Hirer/Driver) | Target offender type is required |
| Target Offender Type | PL-RD-002 | Cannot be same as current offender | Cannot redirect to same offender type |
| Name | PL-RD-003 | Required | Name is required |
| ID Type | PL-RD-004 | Required | ID Type is required |
| ID Number | PL-RD-005 | Required | ID Number is required |
| Address | PL-RD-006 | Required | Address is required |

---

## 2. Backend Validations (OCMS)

### 2.1 Check Furnishability (Section 6.2.1)

| Rule ID | Check | Condition | Action if Failed |
|---------|-------|-----------|------------------|
| PL-BE-001 | Notice Exists | Notice must exist in database | Return NOT_FOUND |
| PL-BE-002 | Processing Stage | Notice must be at furnishable stage | Return NOT_FURNISHABLE |
| PL-BE-003 | Not Final Stage | Stage must not be after CPC | Return LAST_STAGE_AFTER_CPC |
| PL-BE-004 | Offender Type Allowed | Requested type must be allowed for stage | Return INVALID_OFFENDER_TYPE |

#### Furnishable Stages and Allowed Types

| Stage | Can Furnish HIRER | Can Furnish DRIVER |
|-------|-------------------|-------------------|
| OW | Yes | Yes |
| RD1 | No | Yes |
| RD2 | No | Yes |
| DN | No | No |
| CPC+ | No | No |

> Note: PLUS can only furnish HIRER or DRIVER (not OWNER)

### 2.2 Check Redirect Eligibility (Section 6.3.1)

| Rule ID | Check | Condition | Action if Failed |
|---------|-------|-----------|------------------|
| PL-RD-BE-001 | Notice Exists | Notice must exist in database | Return NOT_FOUND |
| PL-RD-BE-002 | Processing Stage | Notice must be at redirectable stage | Return NOT_REDIRECTABLE |
| PL-RD-BE-003 | Not Final Stage | Stage must not be after CPC | Return LAST_STAGE_AFTER_CPC |
| PL-RD-BE-004 | Has Current Offender | Notice must have a current offender | Return NO_CURRENT_OFFENDER |
| PL-RD-BE-005 | Different Target | Target must be different from current | Return SAME_OFFENDER |

### 2.3 Furnish Processing Rules (Section 6.2.1)

| Step | Rule ID | Check | Action |
|------|---------|-------|--------|
| 1 | PL-PR-001 | Validate request payload | Return error if invalid |
| 2 | PL-PR-002 | Save address as Mailing Address | Insert to ocms_offence_notice_owner_driver_addr |
| 3 | PL-PR-003 | Transfer offender flag (optional) | Set offender_indicator to 'Y' if not current |
| 4 | PL-PR-004 | Update processing stage | Change to RD1 (Hirer) or DN (Driver) |
| 5 | PL-PR-005 | Set NPS | Set Next Print Schedule to next day |
| 6 | PL-PR-006 | Sync to Internet | Sync record to eocms tables |

### 2.4 Redirect Processing Rules (Section 6.3.1)

| Step | Rule ID | Check | Action |
|------|---------|-------|--------|
| 1 | PL-RD-PR-001 | Validate request payload | Return error if invalid |
| 2 | PL-RD-PR-002 | Save updated address as Mailing Address | Insert/Update address record |
| 3 | PL-RD-PR-003 | Transfer offender flag | Set previous offender_indicator to 'N' |
| 4 | PL-RD-PR-004 | Set new offender flag | Set new offender_indicator to 'Y' |
| 5 | PL-RD-PR-005 | Update processing stage | Change to RD1 (Hirer) or DN (Driver) |
| 6 | PL-RD-PR-006 | Set NPS | Set Next Print Schedule to next day |
| 7 | PL-RD-PR-007 | Sync to Internet | Sync record to eocms tables |

---

## 3. Decision Trees

### 3.1 PLUS Update Hirer/Driver Decision Tree

```
START: PLM initiates Update Hirer/Driver in PLUS Staff Portal
  │
  ├── PLUS Backend receives request
  │     │
  │     └── Call OCMS Check Furnishability API
  │           │
  │           ├── Decision: Update allowed?
  │           │     │
  │           │     ├── No
  │           │     │     └── Return "Update not allowed" to PLUS Portal
  │           │     │           └── Display error message → END
  │           │     │
  │           │     └── Yes → Continue
  │           │
  │           └── Call OCMS Get Existing Offender API
  │                 │
  │                 ├── Decision: Any existing Hirer/Driver?
  │                 │     │
  │                 │     ├── Yes → Display form with existing data populated
  │                 │     │     └── PLM overwrites existing particulars
  │                 │     │
  │                 │     └── No → Display form with blank fields
  │                 │           └── PLM furnishes new particulars
  │                 │
  │                 └── PLM submits form
  │                       │
  │                       └── PLUS Backend calls OCMS Furnish API
  │                             │
  │                             ├── Decision: Any missing data or format error?
  │                             │     │
  │                             │     ├── Yes → Return error to PLUS
  │                             │     │     └── Display error message → END
  │                             │     │
  │                             │     └── No → Process furnish
  │                             │           │
  │                             │           ├── Save address as Mailing Address
  │                             │           ├── (Optional) Transfer offender flag
  │                             │           ├── Update processing stage (RD1/DN)
  │                             │           ├── Set NPS = Next Day
  │                             │           │
  │                             │           └── Decision: Furnish successful?
  │                             │                 │
  │                             │                 ├── No → Return error → END
  │                             │                 │
  │                             │                 └── Yes → Return success
  │                             │                       └── Display success message → END
  │
END
```

### 3.2 PLUS Redirect Decision Tree

```
START: PLM initiates Redirect in PLUS Staff Portal
  │
  ├── PLUS Backend receives request
  │     │
  │     └── Call OCMS Check Redirect Eligibility API
  │           │
  │           ├── Decision: Redirect allowed?
  │           │     │
  │           │     ├── No
  │           │     │     └── Return "Redirect not allowed" to PLUS Portal
  │           │     │           └── Display error message → END
  │           │     │
  │           │     └── Yes → Continue
  │           │
  │           └── Call OCMS Get All Offenders API
  │                 │
  │                 └── Return existing Owner, Hirer, Driver data
  │                       │
  │                       └── Display form with existing particulars
  │                             │
  │                             ├── (Optional) PLM edits particulars
  │                             │
  │                             └── PLM submits redirect request
  │                                   │
  │                                   └── PLUS Backend calls OCMS Redirect API
  │                                         │
  │                                         ├── Decision: Any missing data or format error?
  │                                         │     │
  │                                         │     ├── Yes → Return error to PLUS
  │                                         │     │     └── Display error message → END
  │                                         │     │
  │                                         │     └── No → Process redirect
  │                                         │           │
  │                                         │           ├── Save address as Mailing Address
  │                                         │           ├── Transfer offender flag (mandatory)
  │                                         │           ├── Update processing stage (RD1/DN)
  │                                         │           ├── Set NPS = Next Day
  │                                         │           │
  │                                         │           └── Decision: Redirect successful?
  │                                         │                 │
  │                                         │                 ├── No → Return error → END
  │                                         │                 │
  │                                         │                 └── Yes → Return success
  │                                         │                       └── Display success message → END
  │
END
```

### 3.3 Furnish vs Redirect Comparison

| Aspect | Furnish (6.2.1) | Redirect (6.3.1) |
|--------|-----------------|------------------|
| Purpose | Add/Update Hirer or Driver | Change current offender |
| Offender Types | HIRER, DRIVER only | OWNER, HIRER, DRIVER |
| Offender Flag Transfer | Optional | Mandatory |
| Existing Data | May or may not exist | Must have current offender |
| Pre-check API | Check Furnishability | Check Redirect Eligibility |
| Retrieve API | Get single offender type | Get all offender types |

---

## 4. Assumptions Log

### 4.1 Assumptions Made

| ID | Assumption | Basis | Impact |
|----|------------|-------|--------|
| ASM-001 | PLUS can only furnish HIRER or DRIVER (not OWNER) | Functional doc specifies "Update Hirer or Driver" | API limits offender types |
| ASM-002 | Address from PLUS is saved as Mailing Address only | Functional doc note about registered address | Registered address retrieved separately |
| ASM-003 | OCMS retrieves registered address from MHA/DataHive | Functional doc note | Separate integration flow |
| ASM-004 | Redirect always transfers offender flag | Flow shows mandatory flag transfer | Unlike furnish which is optional |
| ASM-005 | Processing stage changes to RD1 for Hirer, DN for Driver | Consistent with Section 4 rules | Stage transition logic |
| ASM-006 | NPS (Next Print Schedule) set to next day | Functional doc specifies "NPS = Next Day" | Reminder letter scheduling |

### 4.2 Questions for Clarification

| ID | Question | Status |
|----|----------|--------|
| Q-001 | Can PLUS redirect to the same person with different offender type? | Assumed NO - must be different offender |
| Q-002 | Should OCMS validate PLUS authentication separately? | Assumed YES - external API requires auth |
| Q-003 | Is there rate limiting for PLUS API calls? | Implementation detail - not specified |

---

*Document generated for OCMS 41 Section 6 condition planning*

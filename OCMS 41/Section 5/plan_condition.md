# OCMS 41 Section 5 - Validation Conditions

## Document Information

| Field | Value |
|-------|-------|
| Version | 1.0 |
| Date | 2026-01-07 |
| Source | Functional Document v1.1, Section 5 |
| Module | OCMS 41 - Section 5: Batch Furnish and Batch Update |

---

## Table of Contents

1. [Frontend Validations](#1-frontend-validations)
2. [Backend Validations](#2-backend-validations)
3. [Decision Trees](#3-decision-trees)
4. [Assumptions Log](#4-assumptions-log)

---

## 1. Frontend Validations

### 1.1 Batch Furnish - Notice Selection (Section 5.2)

| Field | Rule ID | Validation | Error Message |
|-------|---------|------------|---------------|
| Notice Selection | BF-001 | At least one Notice must be selected | Please select at least one Notice |
| Notice Selection | BF-002 | Maximum 100 Notices per batch | Maximum 100 Notices allowed per batch |

### 1.2 Batch Furnish - Form Validations

| Field | Rule ID | Validation | Error Message |
|-------|---------|------------|---------------|
| Role | BF-FRM-001 | Required (Owner/Hirer/Driver) | Role is required |
| Name | BF-FRM-002 | Required | Name is required |
| Name | BF-FRM-003 | Max 100 characters | Name exceeds maximum length |
| ID Type | BF-FRM-004 | Required | ID Type is required |
| ID Number | BF-FRM-005 | Required | ID Number is required |
| ID Number | BF-FRM-006 | Format check based on ID Type | Invalid {idType} format |
| Entity Type | BF-FRM-007 | Required if ID Type = UEN | Entity Type is required for UEN |
| Email | BF-FRM-008 | Valid email format if provided | Invalid email format |
| Contact No | BF-FRM-009 | Valid format if provided | Invalid contact number |

### 1.3 Batch Update - Search Validations (Section 5.3)

| Field | Rule ID | Validation | Error Message |
|-------|---------|------------|---------------|
| ID Number | BU-001 | Required | ID Number is required |
| ID Number | BU-002 | Valid format (NRIC/FIN/UEN/Passport) | Invalid ID Number format |

### 1.4 Batch Update - Form Validations

| Field | Rule ID | Validation | Error Message |
|-------|---------|------------|---------------|
| Notice Selection | BU-FRM-001 | At least one Notice must be selected | Please select at least one Notice |
| Postal Code | BU-FRM-002 | 6 digits for Singapore address | Invalid postal code |
| Email | BU-FRM-003 | Valid email format if provided | Invalid email format |
| Contact No | BU-FRM-004 | Valid format if provided | Invalid contact number |

---

## 2. Backend Validations

### 2.1 Batch Furnish - Furnishability Check (Section 5.2)

| Rule ID | Check | Condition | Action if Failed |
|---------|-------|-----------|------------------|
| BF-BE-001 | Notice Exists | Notice must exist in database | Return NOT_FOUND for that Notice |
| BF-BE-002 | Processing Stage | Notice must be at furnishable stage | Return NOT_FURNISHABLE for that Notice |
| BF-BE-003 | Not Final Stage | Stage must not be after CPC | Return LAST_STAGE_AFTER_CPC |
| BF-BE-004 | All Non-Furnishable | If all notices non-furnishable | Stop and prompt user |

#### Furnishable Stages (Same as Section 4)

| Stage | Can Furnish Owner | Can Furnish Hirer | Can Furnish Driver |
|-------|-------------------|-------------------|-------------------|
| OW | No | Yes | Yes |
| RD1 | No | No | Yes |
| RD2 | No | No | Yes |
| DN | No | No | No |
| CPC+ | No | No | No |

### 2.2 Batch Update - Outstanding Notice Check (Section 5.3)

| Rule ID | Check | Condition | Action if Failed |
|---------|-------|-----------|------------------|
| BU-BE-001 | Offender Exists | ID must exist in ocms_offence_notice_owner_driver | Return OFFENDER_NOT_FOUND |
| BU-BE-002 | Is Current Offender | offender_indicator must be 'Y' | Skip Notice (not current offender) |
| BU-BE-003 | Notice Exists | Notice must exist in ocms_valid_offence_notice | Skip Notice |
| BU-BE-004 | No Active PS | Notice must not have active Permanent Suspension | Remove from eligible list |

#### Permanent Suspension Check Logic

| Step | Check | Condition | Result |
|------|-------|-----------|--------|
| 1 | Query suspension records | suspension_type = 'PS' | Continue if found |
| 2 | Check revival status | date_of_revival IS NULL | Has active PS |
| 3 | Check revival status | date_of_revival IS NOT NULL | PS has been revived (eligible) |

### 2.3 Batch Furnish Processing Rules

| Step | Rule ID | Check | Action |
|------|---------|-------|--------|
| 1 | BF-PR-001 | Process each Notice individually | Loop through Notice list |
| 2 | BF-PR-002 | Check existing offender for role | If exists → PATCH, else → POST |
| 3 | BF-PR-003 | Set offender_indicator | Set to 'Y' for new offender |
| 4 | BF-PR-004 | Clear previous offender | Set previous offender_indicator to 'N' |
| 5 | BF-PR-005 | Update processing stage | Change based on offender type |
| 6 | BF-PR-006 | Store result | Add to success/failure list |
| 7 | BF-PR-007 | Sync to Internet | Sync each record to Internet DB |

### 2.4 Batch Update Processing Rules

| Step | Rule ID | Check | Action |
|------|---------|-------|--------|
| 1 | BU-PR-001 | Validate offender is current | Verify offender_indicator = 'Y' |
| 2 | BU-PR-002 | Update address record | Update ocms_offence_notice_owner_driver_addr |
| 3 | BU-PR-003 | Update contact info | Update contact_no and email if provided |
| 4 | BU-PR-004 | No stage change | Processing stage remains unchanged |

---

## 3. Decision Trees

### 3.1 Batch Furnish Decision Tree

```
START: OIC selects Notices and clicks BATCH FURNISH
  │
  ├── Call Check Furnishability API
  │     │
  │     ├── All Notices Non-Furnishable
  │     │     └── Display error, return to search
  │     │
  │     ├── Some Notices Non-Furnishable
  │     │     │
  │     │     ├── Prompt: "X Notices not furnishable. Continue?"
  │     │     │     │
  │     │     │     ├── User clicks NO → Return to search
  │     │     │     │
  │     │     │     └── User clicks YES → Continue with furnishable Notices
  │     │     │
  │     │
  │     └── All Notices Furnishable → Continue
  │
  ├── Display Batch Furnish Form
  │     │
  │     ├── OIC enters offender details
  │     ├── OIC clicks SUBMIT
  │     │
  │     ├── Frontend Validation
  │     │     │
  │     │     ├── FAIL → Show validation error
  │     │     │
  │     │     └── PASS → Prompt overwrite warning
  │     │           │
  │     │           ├── User clicks BACK → Edit form
  │     │           │
  │     │           └── User clicks CONFIRM → Process batch
  │     │
  │     └── Process Each Notice
  │           │
  │           ├── Call Furnish API per Notice
  │           ├── Store success/failure result
  │           ├── Loop until all processed
  │           │
  │           └── Display Result Page
  │
END
```

### 3.2 Batch Update Decision Tree

```
START: OIC navigates to Batch Update screen
  │
  ├── OIC enters ID Number and clicks SEARCH
  │     │
  │     ├── Call Outstanding Notices API
  │     │     │
  │     │     ├── No records found
  │     │     │     └── Display "Offender not found"
  │     │     │
  │     │     └── Records found → Display list
  │     │
  │     ├── Backend processes:
  │     │     ├── Query ocms_offence_notice_owner_driver by ID
  │     │     ├── Filter by offender_indicator = 'Y'
  │     │     ├── Query ocms_valid_offence_notice for each
  │     │     ├── Check ocms_suspended_notice for PS
  │     │     ├── Remove notices with active PS
  │     │     ├── Query mailing address for each
  │     │     └── Return consolidated results
  │     │
  │     └── Display Notice list with current mailing address
  │
  ├── OIC selects Notices to update
  ├── OIC enters new mailing address
  ├── OIC clicks SUBMIT
  │     │
  │     ├── Frontend Validation
  │     │     │
  │     │     ├── FAIL → Show validation error
  │     │     │
  │     │     └── PASS → Call Batch Update API
  │     │
  │     └── Display Result Page
  │
END
```

### 3.3 Permanent Suspension Check Tree

```
START: Check Notice for Permanent Suspension
  │
  ├── Query ocms_suspended_notice
  │     │
  │     ├── No record found → Notice is eligible
  │     │
  │     └── Record found → Check suspension_type
  │           │
  │           ├── suspension_type != 'PS' → Notice is eligible
  │           │
  │           └── suspension_type = 'PS' → Check date_of_revival
  │                 │
  │                 ├── date_of_revival IS NULL → Has Active PS (exclude)
  │                 │
  │                 └── date_of_revival IS NOT NULL → PS Revived (eligible)
  │
END
```

---

## 4. Assumptions Log

### 4.1 Assumptions Made

| ID | Assumption | Basis | Impact |
|----|------------|-------|--------|
| ASM-001 | Same offender particulars applied to all Notices in batch furnish | Functional doc shows single form for all | Single API call per Notice with same data |
| ASM-002 | Frontend processes Notices sequentially (not parallel) | Portal loops API calls per Notice | Progress tracking per Notice |
| ASM-003 | Batch update only updates mailing address, not registered address | Section 5.3 specifies mailing address only | Registered address remains unchanged |
| ASM-004 | Notices with active PS are excluded silently | Flow shows removal from list | No error shown for PS notices |
| ASM-005 | Maximum batch size is implementation-defined | Not specified in functional doc | Suggest 100 Notices max |

### 4.2 Questions for Clarification

| ID | Question | Status |
|----|----------|--------|
| Q-001 | What is the maximum number of Notices that can be processed in one batch? | Assumed 100 |
| Q-002 | Should batch update also sync to Internet DB? | Assumed YES for consistency |
| Q-003 | If one Notice fails during batch furnish, should processing continue? | Assumed YES (continue processing) |

---

*Document generated for OCMS 41 Section 5 condition planning*

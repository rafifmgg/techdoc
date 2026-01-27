# OCMS 42 - Conditions & Validations Planning Document

## Document Information
| Item | Detail |
|------|--------|
| Version | 1.3 |
| Date | 2026-01-27 |
| Source | Functional Flow (ocms 42 functional flow.drawio) |
| Data Reference | Functional Document (v1.3_OCMS 42_Functional_Doc.md) |
| Feature | Furnish Driver's or Hirer's Particulars via eService |

**Note:** Flow logic from Functional Flow (updated), data fields from FD (for reference only).

---

## 1. Decision Trees

### 1.1 Get Furnishable Notices Decision Flow

```
START
  │
  ▼
Query owner_driver table by ID
  │
  ├─ No records? ──────────────────────► RETURN: "No furnishable notices"
  │
  ▼
Check owner_driver_indicator
  │
  ├─ indicator = 'D' (Driver)? ────────► RETURN: "Cannot furnish - offender is driver"
  │                                       Message: "There are other outstanding notice(s)
  │                                       for [Name] that cannot be furnished.
  │                                       Please proceed to pay."
  ▼
Get Notice Numbers
  │
  ▼
Query eVON for outstanding notices
  │
  ├─ Filter: offence_notice_type IN ('O', 'E')
  ├─ Filter: last_processing_stage IN ('ROV', 'ENA', 'RD1', 'RD2')
  ├─ Filter: crs_reason_of_suspension IS NULL
  └─ EXCLUDE: PS with certain EPR reasons
  │
  ▼
Query furnish_application for existing submissions
  │
  ├─ EXCLUDE: status IN ('P', 'S', 'A')
  │
  ▼
Any furnishable notices?
  │
  ├─ No ────────────────────────────────► RETURN: "No furnishable notices"
  │
  ▼
RETURN: List of furnishable notices
```

### 1.2 Furnish Submission Decision Flow

```
START (User submits furnish form)
  │
  ▼
Portal UI Validation
  │
  ├─ Validation FAIL? ─────────────────► User edits submission (loop back)
  │
  ▼
Send to Backend
  │
  ▼
Backend processes and stores to DB
  │
  ▼
Submission Successful?
  │
  ├─ No ───────────────────────────────► RETURN: "Furnish was unsuccessful.
  │                                       Please try again."
  ▼
RETURN: Success
  │
  ▼
Redirect to Pending Notices screen
```

---

## 2. Backend Validations

### 2.1 Furnishable Notices Eligibility Rules

| Rule ID | Rule | Field(s) | Condition | Action | FD Ref |
|---------|------|----------|-----------|--------|--------|
| BR-001 | Owner/Driver Check | `offender_indicator` | Must be 'Y' | Include in query | Sec 2.2 |
| BR-002 | Driver Cannot Furnish | `owner_driver_indicator` | If = 'D' | Return warning, cannot furnish | Sec 2.2 |
| BR-003 | Offence Type Check | `offence_notice_type` | Must be 'O' or 'E' | Include only O/E types | Sec 2.2 |
| BR-004 | Processing Stage Check | `last_processing_stage` | Must be ROV, ENA, RD1, or RD2 | Include only valid stages | Sec 2.2 |
| BR-005 | No Court Suspension | `crs_reason_of_suspension` | Must be NULL | Exclude if has court suspension | Sec 2.2 |
| BR-006 | PS Exclusion | `suspension_type` + `epr_reason_of_suspension` | If PS with excluded EPR | Exclude from list | Sec 2.2 |
| BR-007 | No Existing Application | `status` in furnish_app | NOT IN ('P', 'S', 'A') | Exclude if already has pending/approved | Sec 2.2 |

**Note:** All rules must be evaluated in sequence. If any rule fails, the notice is excluded from furnishable list.

### 2.2 PS EPR Exclusion List (BR-006)

Notices with `suspension_type` = 'PS' AND `epr_reason_of_suspension` in the following are **EXCLUDED**:

| Code | Description |
|------|-------------|
| ANS | Another Suspension |
| APP | Appeal |
| CAN | Cancelled |
| CFA | Court Fine Appeal |
| DBB | Double Booking |
| FCT | Fine Court |
| FTC | Fine to Court |
| OTH | Other |
| SCT | Sent to Court |
| SLC | SLC |
| SSV | SSV |
| VCT | VCT |
| VST | VST |

**Note:** Internet DB does not have `suspended_notices` table to check PS-MID, PS-DIP, PS-RIP/RP2, PS-FOR.

### 2.3 Pending Submissions Rules

| Rule ID | Rule | Field(s) | Condition |
|---------|------|----------|-----------|
| BR-008 | Owner Match | `owner_id_no` | Must match Singpass/Corppass ID |
| BR-009 | 6 Months Filter | `cre_date` | Must be within past 6 months |
| BR-010 | Pending Status | `status` | Must be 'P' (Pending) or 'S' (Resubmission) |

### 2.4 Approved Submissions Rules

| Rule ID | Rule | Field(s) | Condition |
|---------|------|----------|-----------|
| BR-011 | Owner Match | `owner_id_no` | Must match Singpass/Corppass ID |
| BR-012 | 6 Months Filter | `cre_date` | Must be within past 6 months |
| BR-013 | Approved Status | `status` | Must be 'A' (Approved) |

---

## 3. Frontend Validations

**Reference:** FD Section 3 - Furnish Driver's/Hirer's Particulars

### 3.1 Furnish Particulars Form - Field Validations

#### Submitter (Owner) Particulars

| Field | DD Column | Type | Max Length | Mandatory | Validation | Error Message |
|-------|-----------|------|------------|-----------|------------|---------------|
| Name | owner_name | String | 66 | Y | Required, alphanumeric + spaces | Please enter your name |
| ID Number | owner_id_no | String | 12 | Y | Auto-filled from SPCP | - |
| Country Code | owner_tel_code | String | 4 | N | Format: +XX | Please enter valid country code |
| Contact Number | owner_tel_no | String | 20 | N | Numeric | Please enter valid contact number |
| Email Address | owner_email_addr | String | 320 | N | Valid email format | Please enter valid email |

#### Furnished Person (Driver/Hirer) Particulars

| Field | DD Column | Type | Max Length | Mandatory | Validation | Error Message |
|-------|-----------|------|------------|-----------|------------|---------------|
| Name | furnish_name | String | 66 | Y | Required, alphanumeric + spaces | Please enter name |
| ID Type | furnish_id_type | Dropdown | 1 | Y | N=NRIC, F=FIN, U=UEN | Please select ID type |
| ID Number | furnish_id_no | String | 12 | Y | Required, format per ID type | Please enter valid ID number |
| Block/House No | furnish_mail_blk_no | String | 10 | Y | Required | Please enter block/house number |
| Floor | furnish_mail_floor | String | 3 | N | Numeric | Please enter valid floor |
| Unit No | furnish_mail_unit_no | String | 5 | N | Alphanumeric | Please enter valid unit number |
| Street Name | furnish_mail_street_name | String | 32 | Y | Required | Please enter street name |
| Building Name | furnish_mail_bldg_name | String | 65 | N | Alphanumeric | Please enter valid building name |
| Postal Code | furnish_mail_postal_code | String | 6 | Y | 6 digits | Please enter valid postal code |
| Country Code | furnish_tel_code | String | 4 | N | Format: +XX | Please enter valid country code |
| Contact Number | furnish_tel_no | String | 12 | N | Numeric | Please enter valid contact number |
| Email Address | furnish_email_addr | String | 320 | N | Valid email format | Please enter valid email |

#### Relationship & Questionnaire

| Field | DD Column | Type | Max Length | Mandatory | Validation | Error Message |
|-------|-----------|------|------------|-----------|------------|---------------|
| Driver/Hirer | owner_driver_indicator | Radio | 1 | Y | D=Driver, H=Hirer | Please select driver or hirer |
| Relationship | hirer_owner_relationship | Dropdown | 1 | Y | F=Family, E=Employee, L=Leased, O=Others | Please select relationship |
| Others Description | others_relationship_desc | String | 15 | Conditional | Required if relationship = O | Please describe relationship |
| Question 1 Answer | ques_one_ans | String | 32 | Y | Required | Please answer question 1 |
| Question 2 Answer | ques_two_ans | String | 32 | Y | Required | Please answer question 2 |
| Question 3 Answer | ques_three_ans | String | 32 | N | Optional | - |
| Rental From | rental_period_from | DateTime | - | Conditional | Required if relationship = L | Please enter rental start date |
| Rental To | rental_period_to | DateTime | - | Conditional | Required if relationship = L | Please enter rental end date |

#### Declaration

| Field | Type | Mandatory | Validation | Error Message |
|-------|------|-----------|------------|---------------|
| Declaration Checkbox | Boolean | Y | Must be checked (true) | Please accept the declaration |

### 3.2 ID Number Format Validation

| ID Type | Format | Regex Pattern | Example |
|---------|--------|---------------|---------|
| NRIC | S/T + 7 digits + 1 letter | `^[ST]\d{7}[A-Z]$` | S1234567A, T7654321B |
| FIN | F/G/M + 7 digits + 1 letter | `^[FGM]\d{7}[A-Z]$` | F1234567K, G7654321M |
| UEN | 9-10 alphanumeric | `^[0-9A-Z]{9,10}$` | 201234567A, T12AB1234X |

**Note:** Checksum validation should be done server-side.

### 3.3 Contact Number Validation

| Rule | Pattern | Regex | Example |
|------|---------|-------|---------|
| Singapore Mobile | 8 digits, starts with 8 or 9 | `^[89]\d{7}$` | 91234567, 81234567 |
| Singapore Landline | 8 digits, starts with 6 | `^6\d{7}$` | 61234567 |

**Combined Regex:** `^[689]\d{7}$`

---

## 4. UI States & Messages

### 4.1 Furnishable Notices Screen

| Condition | UI Display |
|-----------|------------|
| No furnishable notices | "There are no furnishable notices" |
| Offender is Driver | "There are other outstanding notice(s) for [Name] that cannot be furnished. Please proceed to pay." |
| Has furnishable notices | Display list of notices with checkbox selection |

### 4.2 Pending Submissions Screen

| Condition | UI Display |
|-----------|------------|
| No pending submissions | "There are no Pending Submissions" |
| Has pending submissions | Display list of pending submissions |

### 4.3 Approved Submissions Screen

| Condition | UI Display |
|-----------|------------|
| No approved submissions | "There are no Approved Notices" |
| Has approved submissions | Display list of approved submissions |

### 4.4 Submission Result Screen

| Condition | UI Display |
|-----------|------------|
| Success | "Your submission has been received." + Submission details |
| Failure | "Furnish was unsuccessful. Please try again." |

---

## 5. Submission Success Response Fields

| Field | Description | Example |
|-------|-------------|---------|
| Submission Reference No | System-generated reference | 1234567890 |
| Number of Notice(s) Furnished | Count of notices | 1 |
| Submission Date & Time | Timestamp | 26/04/2025 14:00 |
| Notice Number | Notice identifier | N1234567890 |
| Offence Date | Date of offence | 15/01/2025 |
| Offence Type | O or E | O |
| Vehicle Number | Vehicle plate | SBA1234A |

**Note (V1.3):** Owner details will NO LONGER be displayed upon successful furnish submission.

---

## 6. Status Definitions

| Status Code | Status Name | Description |
|-------------|-------------|-------------|
| P | Pending | Submission awaiting approval |
| S | Resubmission | Requires user to resubmit with corrections |
| A | Approved | Furnishing approved, notice redirected |
| R | Rejected | Furnishing rejected |

---

## 7. Processing Stage Definitions

| Stage Code | Description | Furnishable? |
|------------|-------------|--------------|
| ROV | Review of Violation | Yes |
| ENA | Enforcement Action | Yes |
| RD1 | Reminder 1 | Yes |
| RD2 | Reminder 2 | Yes |
| Other stages | - | No |

---

## 8. Offence Notice Type Definitions

| Type Code | Description | Furnishable? |
|-----------|-------------|--------------|
| O | Owner Offence | Yes |
| E | Enforcement Offence | Yes |
| Other types | - | No |

---

## 9. Eligibility Check Sub-flows

### 9.1 Individual Vehicle Owner/Hirer (Singpass)

| Scenario | ID Source | Eligible | Notes |
|----------|-----------|----------|-------|
| Individual is Owner | Singpass NRIC/FIN | Yes | Can furnish to driver/hirer |
| Individual is Driver | Singpass NRIC/FIN | No | Returns OFFENDER_IS_DRIVER |
| Owner furnishing to self | Singpass NRIC/FIN | No | Must furnish to different person |

**Rules:**
- Owner's ID comes from Singpass authentication
- Driver/Hirer ID entered manually (does NOT need to match Singpass ID)
- Submission allowed for any driver/hirer ID (NRIC/FIN/UEN)
- No additional verification required for driver/hirer

### 9.2 Company Vehicle Owner/Hirer (Corppass)

| Scenario | ID Source | Eligible | Notes |
|----------|-----------|----------|-------|
| Company is Owner | Corppass UEN | Yes | Can furnish to driver/hirer |
| Company employee as Driver | Corppass UEN | No | Returns OFFENDER_IS_DRIVER |
| Authorized representative | Corppass login | Yes | Can submit on company behalf |

**Rules:**
- Company ID (UEN) comes from Corppass authentication
- Authorized representative logs in via Corppass
- Driver/Hirer ID entered manually (NRIC/FIN/UEN allowed)
- UEN validation required for company

---

## 10. Edge Cases & Error Handling

### 10.1 Concurrent Submission Scenarios

| Scenario | Detection | Handling |
|----------|-----------|----------|
| Same user submits same notice twice | Check existing P/S/A status before insert | First wins, second gets NOTICE_NOT_FURNISHABLE |
| Two different owners submit same notice | Not possible | Same notice only linked to one owner |
| User A starts form, User B submits first | Re-verify eligibility before insert | User A gets NOTICE_NOT_FURNISHABLE |

### 10.2 Token/Session Scenarios

| Scenario | Detection | Handling |
|----------|-----------|----------|
| Token expires during form fill | Validate token before API call | Return AUTH_FAILED, redirect to login |
| Session timeout | Backend session check | Return AUTH_FAILED |
| User logs in with different ID | New session | Fresh furnishable list |

### 10.3 Data Change Scenarios

| Scenario | Detection | Handling |
|----------|-----------|----------|
| Notice paid while form is open | Re-check notice status before submit | Return NOTICE_NOT_FURNISHABLE |
| Notice processing stage changes | Re-check last_processing_stage | Return NOTICE_NOT_FURNISHABLE |
| Notice gets suspended | Re-check suspension fields | Return NOTICE_NOT_FURNISHABLE |

### 10.4 Validation Error Scenarios

| Scenario | Detection | Handling | Retry? |
|----------|-----------|----------|--------|
| Invalid ID format | Regex validation | Return VALIDATION_FAILED | Yes, fix input |
| Missing mandatory field | Null/empty check | Return VALIDATION_FAILED | Yes, fill field |
| Invalid contact number | Regex validation | Return VALIDATION_FAILED | Yes, fix input |
| Declaration not checked | Boolean check | Return VALIDATION_FAILED | Yes, check declaration |

### 10.5 System Error Scenarios

| Scenario | Detection | Handling | Retry? |
|----------|-----------|----------|--------|
| Database timeout | Connection exception | Return SUBMISSION_FAILED | Yes, with backoff |
| Database unavailable | Connection exception | Return SUBMISSION_FAILED | Yes, with backoff |
| Insert fails (constraint) | SQL exception | Rollback, return SUBMISSION_FAILED | Check data first |

---

## 11. Data Flow Summary

```
┌─────────────────────────────────────────────────────────────────┐
│                        eService Portal                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────────┐ │
│  │Furnishable│  │ Pending  │  │ Approved │  │ Submit Furnish   │ │
│  │ Notices  │  │Submissions│  │Submissions│  │   Particulars   │ │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────────┬─────────┘ │
└───────┼─────────────┼─────────────┼─────────────────┼───────────┘
        │             │             │                 │
        ▼             ▼             ▼                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                     eService Backend (Internet)                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    API Processing                         │   │
│  │  - Validate token (JWT from SPCP)                         │   │
│  │  - Validate request payload                               │   │
│  │  - Query databases (NO SELECT *)                          │   │
│  │  - Apply business rules (BR-001 to BR-013)               │   │
│  │  - Return response with pagination                        │   │
│  └──────────────────────────────────────────────────────────┘   │
└───────┼─────────────┼─────────────┼─────────────────┼───────────┘
        │             │             │                 │
        ▼             ▼             ▼                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Database (PII)                            │
│  ┌────────────────────────┐  ┌─────────────────────────────┐    │
│  │eocms_offence_notice_   │  │  eocms_furnish_application  │    │
│  │    owner_driver        │  │  (is_sync flag for sync)    │    │
│  └────────────────────────┘  └─────────────────────────────┘    │
│                                                                  │
│  ┌────────────────────────┐                                     │
│  │eocms_valid_offence_    │                                     │
│  │       notice           │                                     │
│  └────────────────────────┘                                     │
└─────────────────────────────────────────────────────────────────┘
        │
        ▼ (Scheduled Sync Job)
┌─────────────────────────────────────────────────────────────────┐
│                     OCMS Intranet (Staff Portal)                 │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Process Furnish Applications (Approve/Reject)            │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

**Sync Mechanism:**
- Internet → Intranet: Scheduled job (every X minutes)
- Query `is_sync = 'N'` records
- Push to Intranet DB
- Update `is_sync = 'Y'`

---

## 12. Assumptions Log

| # | Assumption | Rationale | Status |
|---|------------|-----------|--------|
| 1 | All APIs use POST method | Yi Jie standard - no GET for data retrieval | Confirmed |
| 2 | 6 months lookback for pending/approved | Based on Functional Flow query conditions | Confirmed |
| 3 | Contact number is optional | Based on FD field requirements | Confirmed |
| 4 | Owner details not shown in success response | V1.3 FD change | Confirmed |
| 5 | Singpass provides ID after authentication | Standard SPCP flow | Confirmed |
| 6 | Owner cannot furnish to self | Logical - must furnish to different person | Assumption |
| 7 | Multiple notices can be submitted at once | Based on "List of notices" in FD | Assumption |
| 8 | Checksum validation done server-side | Security best practice | Assumption |

# OCMS 41 Section 5 - Technical Flowchart Plan

## Document Information

| Field | Value |
|-------|-------|
| Version | 1.1 |
| Date | 2026-01-19 |
| Source | Functional Document v1.1, Section 5 |
| Module | OCMS 41 - Section 5: Batch Furnish and Batch Update |

---

## Table of Contents

1. [Overview](#1-overview)
2. [Diagram List](#2-diagram-list)
3. [Diagram Specifications](#3-diagram-specifications)

---

## 1. Overview

Section 5 Technical Flow akan menjelaskan detail teknis untuk batch furnish dan batch update mailing address dari Staff Portal OCMS.

### 1.1 Reference Diagrams

Functional Flow drawio memiliki diagram berikut:
- 5.2 Batch Furnish Owners, Hirer or Drivers
- 5.3 Batch Update Mailing Addr

### 1.2 Technical Flow Diagrams Needed

| Diagram | Name | Description |
|---------|------|-------------|
| 5.2 | Batch Furnish | Detailed batch furnish process with API & DB |
| 5.3 | Batch Update Mailing Address | Detailed batch update process with API & DB |
| 5.3.1 | Retrieve Outstanding Notices | Backend flow for retrieving notices by ID |

---

## 2. Diagram List

### 2.1 Tabs Structure

| Tab | Name | Type | Swimlanes |
|-----|------|------|-----------|
| 5.2 | Batch_Furnish_Offender | Detailed | Staff Portal, OCMS Admin API, Intranet DB, Internet DB |
| 5.3 | Batch_Update_Mailing_Addr | Detailed | Staff Portal, OCMS Admin API, Intranet DB |
| 5.3.1 | Retrieve_Outstanding_Notices | Backend | OCMS Admin API, Intranet DB |

---

## 3. Diagram Specifications

### 3.1 Diagram 5.2: Batch Furnish Offender (Detailed)

**Purpose:** Detailed technical flow for batch furnishing offender to multiple notices

**Swimlanes:**
1. Staff Portal (Blue) - Frontend validation, API calls, result handling
2. OCMS Admin API Intranet (Green) - Backend processing
3. Intranet Database (Yellow) - Data storage
4. Internet Database (Orange) - Sync to Internet

**Flow Elements:**

**Staff Portal:**
1. Start
2. OIC navigates to Search Notices page
3. OIC searches and selects multiple Notices
4. OIC clicks APPLY OPERATION → BATCH FURNISH
5. Call POST /batch/check-furnishability
6. Receive furnishability response
7. Decision: All non-furnishable? → Yes: Show error, End
8. Decision: Some non-furnishable? → Yes: Prompt continue
9. User confirms to continue
10. Display Batch Furnish form
11. OIC enters offender details
12. OIC clicks SUBMIT
13. Frontend validation
14. Decision: Valid? → No: Show error
15. Display overwrite warning prompt
16. User clicks CONFIRM
17. Loop: Process each Notice
18. Call POST /notice/offender/furnish per Notice
19. Store result (success/failure)
20. Query DB for latest details
21. Display Result Page
22. End

**OCMS Admin API (Check Furnishability):**
1. Receive check request
2. Loop: Check each Notice
3. Query Notice by noticeNo
4. Check processing stage
5. Return furnishability result

**OCMS Admin API (Furnish):**
1. Receive furnish request
2. Bean validation
3. Decision: Valid? → No: Return VALIDATION_ERROR
4. Query notice by noticeNo
5. Decision: Exists? → No: Return NOT_FOUND
6. Check furnishability
7. Decision: Can furnish? → No: Return NOT_FURNISHABLE
8. Decision: Existing offender? → POST/PATCH
9. Create/Update offender record
10. Set offender_indicator = Y
11. Clear previous offender indicator
12. Update processing stage
13. Sync to Internet DB
14. Return success response

**Database Operations:**
- ocms_valid_offence_notice: Query notice
- ocms_offence_notice_owner_driver: INSERT/UPDATE offender
- eocms_furnish_application: Sync to Internet/PII

**Estimated Dimensions:**
- Width: ~3600px
- Height: ~1300px

---

### 3.2 Diagram 5.3: Batch Update Mailing Address (Detailed)

**Purpose:** Detailed technical flow for batch updating mailing address

**Swimlanes:**
1. Staff Portal (Blue) - Search, display, update
2. OCMS Admin API Intranet (Green) - Backend processing
3. Intranet Database (Yellow) - Data storage

**Flow Elements:**

**Staff Portal:**
1. Start
2. OIC navigates to Batch Update screen
3. OIC enters ID Number
4. OIC clicks SEARCH
5. Call POST /notice/offender/outstanding
6. Receive response
7. Decision: Records found? → No: Show "Offender not found"
8. Display Notice list with mailing addresses
9. OIC selects Notices to update
10. OIC enters new mailing address
11. OIC clicks SUBMIT
12. Frontend validation
13. Decision: Valid? → No: Show error
14. Call POST /batch/update-address
15. Receive response
16. Display Result Page
17. End

**OCMS Admin API (Get Outstanding):**
1. Receive request with idNo
2. Query ocms_offence_notice_owner_driver by id_no
3. Decision: Records found? → No: Return OFFENDER_NOT_FOUND
4. Filter by offender_indicator = 'Y'
5. For each record: Query ocms_valid_offence_notice
6. For each notice: Query ocms_suspended_notice
7. Check for active PS (type='PS' AND date_of_revival IS NULL)
8. Remove notices with active PS
9. Query ocms_offence_notice_owner_driver_addr for mailing
10. Consolidate results
11. Return response

**OCMS Admin API (Update Address):**
1. Receive update request
2. Validate request
3. Loop: Process each Notice
4. Query offender record
5. Decision: Is current offender? → No: Skip
6. Update/Insert address record
7. Update contact info if provided
8. Return success response

**Database Operations:**
- ocms_offence_notice_owner_driver: Query by id_no
- ocms_valid_offence_notice: Query notice details
- ocms_suspended_notice: Check suspension status
- ocms_offence_notice_owner_driver_addr: Query/Update address

**Estimated Dimensions:**
- Width: ~3200px
- Height: ~1200px

---

### 3.3 Diagram 5.3.1: Retrieve Outstanding Notices (Backend Detail)

**Purpose:** Detailed backend flow for retrieving outstanding notices by ID

**Swimlanes:**
1. OCMS Admin API Intranet (Green) - Processing logic
2. Intranet Database (Yellow) - Data queries

**Flow Elements:**

**OCMS Admin API:**
1. Function Start
2. Extract ID number from request
3. Query DB for offender records
4. Decision: Records found? → No: Return NOT_FOUND
5. Filter by offender_indicator = 'Y'
6. Result: List of notices where ID is current offender
7. Loop: For each notice
8. Query ocms_valid_offence_notice
9. Decision: Notice found? → No: Skip
10. Query ocms_suspended_notice
11. Decision: Any suspension record? → No: Add to eligible list
12. Decision: suspension_type = 'PS'? → No: Add to eligible list
13. Decision: date_of_revival IS NULL? → Yes: Has active PS (exclude)
14. Add to eligible list (PS revived)
15. Continue loop until all processed
16. Result: List of notices without active PS
17. Query ocms_offence_notice_owner_driver_addr for each
18. Decision: Address found? → Mixed results possible
19. Consolidate all data
20. Return response
21. Function End

**Database Queries:**
- Query 1: ocms_offence_notice_owner_driver WHERE id_no = ?
- Query 2: ocms_valid_offence_notice WHERE notice_no = ?
- Query 3: ocms_suspended_notice WHERE notice_no = ?
- Query 4: ocms_offence_notice_owner_driver_addr WHERE notice_no = ? AND owner_driver_indicator = ?

**Estimated Dimensions:**
- Width: ~2800px
- Height: ~1400px

---

## 4. Sizing Guidelines

Following the flowchart-sizing-standard.md:

| Diagram | Est. Elements | Page Width | Page Height |
|---------|---------------|------------|-------------|
| 5.2 Batch Furnish | ~35 | 3600 | 1300 |
| 5.3 Batch Update | ~30 | 3200 | 1200 |
| 5.3.1 Retrieve Outstanding | ~25 | 2800 | 1400 |

---

## 5. Element Naming Convention

| Element Type | Prefix | Example |
|--------------|--------|---------|
| Start/End | start-, end- | start-1, end-1 |
| Process | p- | p-validate, p-query |
| Decision | d- | d-valid, d-exists |
| Swimlane | swim- | swim-portal, swim-api |
| Edge | e- | e-1, e-yes, e-no |
| Database box | db- | db-offender, db-notice |
| API box | api- | api-furnish, api-update |
| Loop | loop- | loop-process, loop-notice |

---

## 6. Special Flow Patterns

### 6.1 Loop Pattern for Batch Processing

```
[Loop Start] → [Process Item] → [Store Result] → [Decision: More items?]
                                                        │
                                                        ├── Yes → [Loop Start]
                                                        │
                                                        └── No → [Continue]
```

### 6.2 Permanent Suspension Check Pattern

```
[Query ocms_suspended_notice] → [Decision: Record found?]
                                        │
                                        ├── No → [Eligible]
                                        │
                                        └── Yes → [Decision: type = PS?]
                                                        │
                                                        ├── No → [Eligible]
                                                        │
                                                        └── Yes → [Decision: date_of_revival IS NULL?]
                                                                        │
                                                                        ├── Yes → [Exclude (Active PS)]
                                                                        │
                                                                        └── No → [Eligible (PS Revived)]
```

---

*Document generated for OCMS 41 Section 5 flowchart planning*

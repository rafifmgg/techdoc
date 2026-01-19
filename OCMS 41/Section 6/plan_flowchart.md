# OCMS 41 Section 6 - Technical Flowchart Plan

## Document Information

| Field | Value |
|-------|-------|
| Version | 1.0 |
| Date | 2026-01-07 |
| Source | Functional Document v1.1, Section 6 |
| Module | OCMS 41 - Section 6: PLUS External System Integration |

---

## Table of Contents

1. [Overview](#1-overview)
2. [Diagram List](#2-diagram-list)
3. [Diagram Specifications](#3-diagram-specifications)

---

## 1. Overview

Section 6 Technical Flow akan menjelaskan detail teknis untuk integrasi PLUS dengan OCMS untuk furnish dan redirect operations.

### 1.1 Reference Diagrams

Functional Flow drawio memiliki diagram berikut:
- 6.2.1 PLUS Update Hirer/Driver
- 6.3.1 PLUS Redirect

### 1.2 Technical Flow Diagrams Needed

| Diagram | Name | Description |
|---------|------|-------------|
| 6.2 | PLUS Update Hirer/Driver | Detailed flow for PLUS furnish operation with API & DB |
| 6.3 | PLUS Redirect | Detailed flow for PLUS redirect operation with API & DB |

---

## 2. Diagram List

### 2.1 Tabs Structure

| Tab | Name | Type | Swimlanes |
|-----|------|------|-----------|
| 6.2 | PLUS_Update_Hirer_Driver | Detailed | PLUS Staff Portal, PLUS Backend, OCMS Admin API, Intranet DB |
| 6.3 | PLUS_Redirect | Detailed | PLUS Staff Portal, PLUS Backend, OCMS Admin API, Intranet DB |

---

## 3. Diagram Specifications

### 3.1 Diagram 6.2: PLUS Update Hirer/Driver (Detailed)

**Purpose:** Detailed technical flow for PLUS updating Hirer or Driver particulars via OCMS API

**Swimlanes:**
1. PLUS Staff Portal (Light Blue) - UI interactions
2. PLUS Intranet Backend (Blue) - PLUS backend processing
3. OCMS Admin API Intranet (Green) - OCMS API processing
4. Intranet Database (Yellow) - Data storage

**Flow Elements:**

**PLUS Staff Portal:**
1. Start
2. PLM initiates Update Hirer/Driver
3. Receive furnishability result
4. Decision: Update allowed? → No: Display error, End
5. Receive existing offender data
6. Decision: Existing offender? → Yes: Display form with data / No: Display blank form
7. PLM enters/updates particulars
8. PLM clicks SUBMIT
9. Receive API response
10. Decision: Success? → No: Display error
11. Display success message
12. End

**PLUS Intranet Backend:**
1. Receive UI request for furnishability check
2. Call OCMS API: POST /external/plus/check-furnishability
3. Return result to Portal
4. Decision: Update allowed? → No: Stop
5. Call OCMS API: GET /external/plus/offender/{noticeNo}
6. Return existing offender data to Portal
7. Receive submit request from Portal
8. Call OCMS API: POST /external/plus/furnish
9. Return result to Portal

**OCMS Admin API (Check Furnishability):**
1. Receive check request from PLUS
2. Query notice by noticeNo
3. Decision: Notice exists? → No: Return NOT_FOUND
4. Check processing stage
5. Decision: Can furnish? → No: Return NOT_FURNISHABLE
6. Return furnishable = true with allowed types

**OCMS Admin API (Get Existing Offender):**
1. Receive get request from PLUS
2. Query ocms_offence_notice_owner_driver by notice and type
3. Decision: Record exists? → No: Return null
4. Query ocms_offence_notice_owner_driver_addr for address
5. Return existing offender data

**OCMS Admin API (Furnish):**
1. Receive furnish request from PLUS
2. Bean validation
3. Decision: Valid? → No: Return VALIDATION_ERROR
4. Query notice by noticeNo
5. Decision: Exists? → No: Return NOT_FOUND
6. Check furnishability
7. Decision: Can furnish? → No: Return NOT_FURNISHABLE
8. Decision: Existing offender? → Yes: Update / No: Insert
9. Save address as Mailing Address
10. Decision: Transfer flag? → Yes: Update offender_indicator
11. Update processing stage (RD1/DN)
12. Set NPS = Next Day
13. Sync to Internet DB
14. Return success response

**Database Operations:**
- ocms_valid_offence_notice: Query notice
- ocms_offence_notice_owner_driver: SELECT/INSERT/UPDATE offender
- ocms_offence_notice_owner_driver_addr: INSERT/UPDATE address
- eocms_offence_notice_owner_driver: Sync to Internet

**Estimated Dimensions:**
- Width: ~3800px
- Height: ~1400px

---

### 3.2 Diagram 6.3: PLUS Redirect (Detailed)

**Purpose:** Detailed technical flow for PLUS redirecting Notice to new Offender via OCMS API

**Swimlanes:**
1. PLUS Staff Portal (Light Blue) - UI interactions
2. PLUS Intranet Backend (Blue) - PLUS backend processing
3. OCMS Admin API Intranet (Green) - OCMS API processing
4. Intranet Database (Yellow) - Data storage

**Flow Elements:**

**PLUS Staff Portal:**
1. Start
2. PLM initiates Redirect
3. Receive redirect eligibility result
4. Decision: Redirect allowed? → No: Display error, End
5. Receive all offenders data
6. Display form with existing Owner/Hirer/Driver particulars
7. PLM selects target offender type
8. (Optional) PLM edits particulars
9. PLM clicks SUBMIT
10. Receive API response
11. Decision: Success? → No: Display error
12. Display success message
13. End

**PLUS Intranet Backend:**
1. Receive UI request for redirect eligibility check
2. Call OCMS API: POST /external/plus/check-redirect
3. Return result to Portal
4. Decision: Redirect allowed? → No: Stop
5. Call OCMS API: GET /external/plus/offenders/{noticeNo}
6. Return all offenders data to Portal
7. Receive redirect request from Portal
8. Call OCMS API: POST /external/plus/redirect
9. Return result to Portal

**OCMS Admin API (Check Redirect):**
1. Receive check request from PLUS
2. Query notice by noticeNo
3. Decision: Notice exists? → No: Return NOT_FOUND
4. Check processing stage
5. Decision: Can redirect? → No: Return NOT_REDIRECTABLE
6. Query current offender
7. Decision: Has current offender? → No: Return NO_CURRENT_OFFENDER
8. Return redirectable = true with current offender info

**OCMS Admin API (Get All Offenders):**
1. Receive get request from PLUS
2. Query ocms_offence_notice_owner_driver for all types
3. Query ocms_offence_notice_owner_driver_addr for each
4. Consolidate Owner, Hirer, Driver data
5. Return all offenders with addresses

**OCMS Admin API (Redirect):**
1. Receive redirect request from PLUS
2. Bean validation
3. Decision: Valid? → No: Return VALIDATION_ERROR
4. Query notice by noticeNo
5. Decision: Exists? → No: Return NOT_FOUND
6. Check redirect eligibility
7. Decision: Can redirect? → No: Return NOT_REDIRECTABLE
8. Query current offender
9. Decision: Same as target? → Yes: Return SAME_OFFENDER
10. Save updated address as Mailing Address
11. Set previous offender_indicator = 'N'
12. Set new offender_indicator = 'Y'
13. Update processing stage (RD1/DN)
14. Set NPS = Next Day
15. Sync to Internet DB
16. Return success response

**Database Operations:**
- ocms_valid_offence_notice: Query notice
- ocms_offence_notice_owner_driver: SELECT Owner/Hirer/Driver, UPDATE indicators
- ocms_offence_notice_owner_driver_addr: SELECT/INSERT/UPDATE addresses
- eocms_offence_notice_owner_driver: Sync to Internet

**Estimated Dimensions:**
- Width: ~3800px
- Height: ~1500px

---

## 4. Sizing Guidelines

Following the flowchart-sizing-standard.md:

| Diagram | Est. Elements | Page Width | Page Height |
|---------|---------------|------------|-------------|
| 6.2 PLUS Update | ~40 | 3800 | 1400 |
| 6.3 PLUS Redirect | ~45 | 3800 | 1500 |

---

## 5. Element Naming Convention

| Element Type | Prefix | Example |
|--------------|--------|---------|
| Start/End | start-, end- | start-1, end-1 |
| Process | p- | p-validate, p-query |
| Decision | d- | d-valid, d-exists |
| Swimlane | swim- | swim-plus-portal, swim-ocms |
| Edge | e- | e-1, e-yes, e-no |
| Database box | db- | db-offender, db-notice |
| API call | api- | api-furnish, api-redirect |

---

## 6. Special Flow Patterns

### 6.1 External System Integration Pattern

```
[PLUS Portal] → [PLUS Backend] → [OCMS API] → [Database]
      ↑              ↑                ↑             │
      └──────────────┴────────────────┴─────────────┘
                  Response Flow
```

### 6.2 Three-Step Operation Pattern

Each operation follows:
1. **Check** - Verify eligibility (Check Furnishability / Check Redirect)
2. **Retrieve** - Get existing data (Get Offender / Get All Offenders)
3. **Execute** - Perform action (Furnish / Redirect)

### 6.3 Offender Flag Transfer Pattern (Redirect)

```
[Query Current Offender] → [Set offender_indicator = 'N']
                                        │
                                        ↓
[Query Target Offender] → [Set offender_indicator = 'Y']
```

---

*Document generated for OCMS 41 Section 6 flowchart planning*

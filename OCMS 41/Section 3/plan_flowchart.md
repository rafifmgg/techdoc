# OCMS 41 Section 4 - Technical Flowchart Plan

## Document Information

| Field | Value |
|-------|-------|
| Version | 1.1 |
| Date | 2026-01-19 |
| Source | Functional Document v1.1, Section 4 |
| Module | OCMS 41 - Section 4: Staff Portal Manual Furnish or Update |

---

## Table of Contents

1. [Overview](#1-overview)
2. [Diagram List](#2-diagram-list)
3. [Diagram Specifications](#3-diagram-specifications)

---

## 1. Overview

Section 4 Technical Flow akan menjelaskan detail teknis untuk fungsi manual furnish, redirect, dan update dari Staff Portal OCMS.

### 1.1 Reference Diagrams

Functional Flow drawio memiliki diagram berikut:
- 4.2_HL Portal_furnish_update (High Level)
- 4.5.1_Action_button_checks
- 4.6.2 Process Flow for Furnish
- 4.7.2 Process Flow for Redirect
- 4.8.2 Process Flow for Update

### 1.2 Technical Flow Diagrams Needed

| Diagram | Name | Description |
|---------|------|-------------|
| 4.2 | High Level Flow | Overview of Furnish/Redirect/Update flow |
| 4.5 | Action Button Check | Determine which buttons to display |
| 4.6 | Furnish Submission | Detailed furnish process with API & DB |
| 4.7 | Redirect Notice | Detailed redirect process with API & DB |
| 4.8 | Update Particulars | Detailed update process with API & DB |

---

## 2. Diagram List

### 2.1 Tabs Structure

| Tab | Name | Type | Swimlanes |
|-----|------|------|-----------|
| 4.2 | High_Level_Furnish_Update | Overview | Staff Portal |
| 4.5 | Action_Button_Check | Decision | Staff Portal, Backend |
| 4.6 | Furnish_Offender | Detailed | Staff Portal, OCMS Admin API, Intranet DB, Internet DB |
| 4.7 | Redirect_Notice | Detailed | Staff Portal, OCMS Admin API, Intranet DB, Internet DB |
| 4.8 | Update_Particulars | Detailed | Staff Portal, OCMS Admin API, Intranet DB |

---

## 3. Diagram Specifications

### 3.1 Diagram 4.2: High Level Furnish/Update Flow

**Purpose:** Show overview of manual furnish/update process

**Swimlanes:**
1. Staff Portal (Blue)

**Flow Elements:**
1. Start
2. OIC navigates to Search Notices page
3. OIC searches for Notice
4. OIC clicks Notice Number → Opens detail page in new tab
5. OIC clicks Owner/Hirer/Driver Details sub-tab
6. Portal displays Owner/Hirer/Driver details
7. OIC clicks UNLOCK button
8. Portal unlocks fields and detects presence of particulars
9. Decision: Which action?
   - FURNISH → Add new offender
   - REDIRECT → Transfer to another offender
   - UPDATE → Modify existing offender
10. Portal performs data validation
11. Portal calls backend API
12. Portal receives processing result
13. Portal displays outcome
14. End

**Estimated Dimensions:**
- Width: ~2000px
- Height: ~600px (single swimlane)

---

### 3.2 Diagram 4.5: Action Button Check

**Purpose:** Show logic for determining which buttons to display

**Swimlanes:**
1. Staff Portal (Blue)
2. Backend Logic (Green)

**Flow Elements:**
1. Start
2. OIC clicks UNLOCK button
3. Portal detects presence of particulars and Offender Indicator
4. Decision: Does offender record exist?
   - NO → Display FURNISH button only
   - YES → Check: Is Current Offender?
     - YES → Display UPDATE button only
     - NO → Display REDIRECT button only
5. End

**Estimated Dimensions:**
- Width: ~1800px
- Height: ~700px

---

### 3.3 Diagram 4.6: Furnish Offender (Detailed)

**Purpose:** Detailed technical flow for furnishing offender

**Swimlanes:**
1. Staff Portal (Blue) - Frontend validation, API calls
2. OCMS Admin API Intranet (Green) - Backend processing
3. Intranet Database (Yellow) - Data storage
4. Internet Database (Orange) - Sync to Internet

**Flow Elements:**

**Staff Portal:**
1. Start
2. OIC clicks FURNISH button
3. OIC enters offender details
4. OIC clicks Submit
5. Frontend validation
6. Decision: Valid? → No: Show error
7. Call POST /notice/offender/furnish
8. Receive response
9. Display success/error message
10. End

**OCMS Admin API:**
1. Receive request
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
- ocms_valid_offence_notice (VON): Query notice (last_processing_stage, vehicle_no)
- ocms_offence_notice_owner_driver (OND): INSERT/UPDATE offender
- ocms_offence_notice_owner_driver_addr (OND_ADDR): INSERT/UPDATE address (type_of_address='furnished_mail')
- ocms_furnish_application (FA): INSERT furnish application record
- eocms_furnish_application (eFA): Sync to PII zone

**Estimated Dimensions:**
- Width: ~3200px
- Height: ~1100px

---

### 3.4 Diagram 4.7: Redirect Notice (Detailed)

**Purpose:** Detailed technical flow for redirecting notice

**Swimlanes:**
1. Staff Portal (Blue)
2. OCMS Admin API Intranet (Green)
3. Intranet Database (Yellow)
4. Internet Database (Orange)

**Flow Elements:**

**Staff Portal:**
1. Start
2. OIC clicks REDIRECT button
3. OIC selects target role
4. OIC enters new offender details
5. OIC clicks Submit
6. Frontend validation
7. Call POST /notice/offender/redirect
8. Receive response
9. Display success/error message
10. End

**OCMS Admin API:**
1. Receive request
2. Validate request
3. Query notice and current offender
4. Check redirect eligibility
5. Decision: Can redirect? → No: Return error
6. Clear current offender indicator
7. Decision: Target offender exists? → Create/Update
8. Set new offender indicator = Y
9. Reset processing stage
10. Sync to Internet DB
11. Return success response

**Estimated Dimensions:**
- Width: ~3200px
- Height: ~1100px

---

### 3.5 Diagram 4.8: Update Particulars (Detailed)

**Purpose:** Detailed technical flow for updating offender particulars

**Swimlanes:**
1. Staff Portal (Blue)
2. OCMS Admin API Intranet (Green)
3. Intranet Database (Yellow)

**Flow Elements:**

**Staff Portal:**
1. Start
2. OIC clicks UPDATE button
3. OIC modifies particulars
4. OIC clicks Submit
5. Frontend validation
6. Call POST /notice/offender/update
7. Receive response
8. Display success/error message
9. End

**OCMS Admin API:**
1. Receive request
2. Validate request
3. Query offender record
4. Decision: Is current offender? → No: Return NOT_CURRENT_OFFENDER
5. Update offender particulars
6. Return success response

**Note:** Update does NOT change:
- Offender indicator
- Processing stage

**Estimated Dimensions:**
- Width: ~2400px
- Height: ~900px

---

## 4. Sizing Guidelines

Following the flowchart-sizing-standard.md:

| Diagram | Est. Elements | Page Width | Page Height |
|---------|---------------|------------|-------------|
| 4.2 HL | ~15 | 2000 | 600 |
| 4.5 Button | ~12 | 1800 | 700 |
| 4.6 Furnish | ~25 | 3200 | 1100 |
| 4.7 Redirect | ~25 | 3200 | 1100 |
| 4.8 Update | ~15 | 2400 | 900 |

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
| API box | api- | api-furnish, api-redirect |

---

*Document generated for OCMS 41 Section 4 flowchart planning*

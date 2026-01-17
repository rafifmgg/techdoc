# OCMS 21 - Duplicate Notices (DBB) - Flowchart Planning Document

## Document Information

| Field | Value |
|-------|-------|
| **Document Version** | 1.2 |
| **Date** | 2026-01-13 |
| **Epic** | OCMS 21 - Manage Duplicate Notices |
| **Feature** | Technical Flowchart Planning |
| **Source Documents** | v1.0_OCMS_21_Duplicate_Notices.md (FD)<br>ocms 21 - Func flow_.drawio (Functional Flow) |
| **Code Reference** | CreateNoticeServiceImpl.java<br>NoticeValidationHelper.java |
| **Author** | Claude Code (Planning Phase) |

---

## Table of Contents

1. [Overview](#overview)
2. [Section Mapping (FD → TD)](#section-mapping-fd--td)
3. [Flowchart Tab Structure](#flowchart-tab-structure)
4. [Swimlane Design](#swimlane-design)
5. [Section 1 (FD 2.2): DBB in Create Notice Flow](#section-1-fd-22-dbb-in-create-notice-flow)
6. [Section 2 (FD 3.2): High-Level DBB Processing](#section-2-fd-32-high-level-dbb-processing)
7. [Section 3 (FD 3.3): Type O DBB Criteria](#section-3-fd-33-type-o-dbb-criteria)
8. [Section 4 (FD 3.4): Type E DBB Criteria](#section-4-fd-34-type-e-dbb-criteria)
9. [Yellow Boxes (API/DB Operations)](#yellow-boxes-apidb-operations)
10. [BR/OP Code Expansion Rules](#brop-code-expansion-rules)
11. [Data Dictionary Validation](#data-dictionary-validation)

---

## 1. Overview

### Purpose
This document plans the technical flowchart structure for OCMS 21 Duplicate Notices (DBB) feature. It maps functional flow diagrams to technical implementation with proper swimlanes, database operations, and detailed steps.

### Key References
- **Functional Flow:** `ocms 21 - Func flow_.drawio` (4 tabs)
- **Functional Document:** `v1.0_OCMS_21_Duplicate_Notices.md`
- **Backend Code:** `CreateNoticeServiceImpl.java`, `NoticeValidationHelper.java`
- **Guidelines:** `Docs/guidelines/create_drawio.md`

### Critical Rule
⚠️ **Section Numbering:** FD Section 2 → TD Section 1, FD Section 3 → TD Section 2 (skip FD Section 1 - User Story)

---

## 2. Section Mapping (FD → TD)

| FD Section | FD Title | TD Section | TD Title | Flowchart Tab |
|------------|----------|------------|----------|---------------|
| ❌ Section 1 | User Story | - | **(Skip)** | - |
| ✅ Section 2 | Explanation: Duplicate Notices Handling | **Section 1** | DBB in Create Notice Flow | **Tab 1.2** |
| ✅ Section 3 | Function: DBB Detection & Suspension | **Section 2** | DBB Processing Function | **Tab 2.2** |
| ✅ Section 3.3 | Type O DBB Criteria | **Section 2.3** | Type O Sub-flow | **Tab 2.3** |
| ✅ Section 3.4 | Type E DBB Criteria | **Section 2.4** | Type E Sub-flow | **Tab 2.4** |

**FD Reference:** CLAUDE.md - "Skip FD Section 1 from TD, renumber FD Sec 2 → TD Sec 1"

---

## 3. Flowchart Tab Structure

### Technical Flowchart Tabs (Planned)

| Tab Name | Content | Source FD Section | Source Func Flow Tab |
|----------|---------|-------------------|----------------------|
| **1.2 - DBB in Create Notice** | Integration of DBB check in notice creation | FD 2.2 | Func Flow Tab 1 |
| **2.2 - High-Level DBB Processing** | Main DBB detection function | FD 3.2 | Func Flow Tab 2 |
| **2.3 - Type O DBB Criteria** | Detailed Type O (Parking) logic | FD 3.3 | Func Flow Tab 3 |
| **2.4 - Type E DBB Criteria** | Detailed Type E (Payment Evasion) logic | FD 3.4 | Func Flow Tab 4 |

**Note:** Tab numbering follows TD section numbers (1.2, 2.2, 2.3, 2.4).

---

## 4. Swimlane Design

### 4.1 Swimlane Structure (All Tabs)

⚠️ **Critical:** Functional flow does NOT have explicit swimlanes. Technical flowchart MUST add them.

| Swimlane | Responsibilities | Examples |
|----------|------------------|----------|
| **Frontend** | User interactions (N/A for DBB - automated) | - |
| **Backend** | Business logic, validations, orchestration | Check DBB criteria, Apply suspension |
| **Database** | Query execution, data storage | SELECT from ocms_valid_offence_notice, INSERT into ocms_suspended_notice |
| **External System** | External APIs (N/A for DBB - internal only) | - |

**For OCMS 21 DBB:**
- **Frontend swimlane:** Empty (no user interaction)
- **Backend swimlane:** Main logic flow
- **Database swimlane:** Query/update operations
- **External System swimlane:** Empty (no external APIs)

**Guideline Reference:** `Docs/guidelines/create_drawio.md` - Section on swimlanes

---

### 4.2 Swimlane Placement Rules

| Element | Swimlane | Connection Type |
|---------|----------|-----------------|
| Function/Process box | Backend | Solid line |
| Decision diamond | Backend | Solid line |
| Query box (yellow) | Database | Dashed line from Backend |
| Response box (yellow) | Database | Dashed line to Backend |
| Update box (yellow) | Database | Dashed line from Backend |
| Note box | Any | No connection |

---

## 5. Section 1 (FD 2.2): DBB in Create Notice Flow

### 5.1 Tab Overview
**Tab Name:** `1.2 - DBB in Create Notice`

**Purpose:** Show where DBB check fits within the overall notice creation workflow.

**FD Reference:** Section 2.2 - "Revised Create Notice Processing Flow"

**Functional Flow Reference:** Tab 1 - "2.2 - DBB in create notice"

**Code Reference:** `CreateNoticeServiceImpl.java:780-1299`

---

### 5.2 Swimlane Content

#### Backend Swimlane
```
START
  ↓
[Receive data to create Notice(s)]
  ↓
[Check duplicate Notice Number] → {Notice exists?} → YES → [Return OCMS-4000] → END
  ↓ NO
[Identify Offence Type] → {Type = U?} → YES → [Route to Type U processing] → END
  ↓ NO (Type O or E)
[Check Veh Reg type]
  ↓
[Create Notice] ← DASHED CONNECTION TO DATABASE
  ↓
[Check for Double Booking] ← THIS IS WHERE DBB LOGIC HAPPENS
  ↓
{DBB Detected?}
  ├─→ YES → [Apply PS-DBB Suspension] → END
  └─→ NO → [Continue to AN Qualification (Type O only)] → END
```

#### Database Swimlane
```
(Yellow Box) [Query: Check notice_no exists]
  ↓
(Yellow Response) [Return: EXISTS/NOT EXISTS]
  ↓
(Yellow Box) [INSERT: Create notice record]
  ↓
(Yellow Response) [Return: Success]
  ↓
(Dashed connection to "Check for Double Booking")
  ↓
(Yellow Box) [Query: Find matching notices]
  ↓
(Yellow Response) [Return: List of potential duplicates]
  ↓
(Yellow Box) [INSERT: Create suspended_notice record]
  ↓
(Yellow Box) [UPDATE: ocms_valid_offence_notice]
  ↓
(Yellow Box) [UPDATE: eocms_valid_offence_notice]
```

---

### 5.3 Key Boxes to Add (Not in Functional Flow)

| Box Name | Swimlane | Color | Content | Code Reference |
|----------|----------|-------|---------|----------------|
| Query duplicate notice number | Database | Yellow | `SELECT * FROM ocms_valid_offence_notice WHERE notice_no = ?` | Line 251 |
| Create notice (INSERT) | Database | Yellow | `INSERT INTO ocms_valid_offence_notice (...)` | Line 1000-1040 |
| Query matching notices | Database | Yellow | `SELECT * FROM ocms_valid_offence_notice WHERE vehicle_no = ? AND computer_rule_code = ? AND pp_code = ?` | Line 386-390 |
| Update notice with PS-DBB | Database | Yellow | `UPDATE ocms_valid_offence_notice SET suspension_type='PS', epr_reason_of_suspension='DBB' WHERE notice_no = ?` | Line 1171-1186 |
| Insert suspended_notice | Database | Yellow | `INSERT INTO ocms_suspended_notice (...)` | Line 1209-1233 |

---

### 5.4 BR/OP Codes to Expand

**Expand in flow boxes:**
- BR-DBB-001 → "DBB check runs automatically after notice creation"
- BR-DBB-002 → "Exclude current notice from query results"

**Keep abbreviated in note boxes:**
- OCMS-4000 (with explanation: "Notice number already exists")

---

## 6. Section 2 (FD 3.2): High-Level DBB Processing

### 6.1 Tab Overview
**Tab Name:** `2.2 - High-Level DBB Processing`

**Purpose:** Main DBB detection function entry point and routing logic.

**FD Reference:** Section 3.2 - "High-Level Process Flow"

**Functional Flow Reference:** Tab 2 - "3.2 - DBB processing flow"

**Code Reference:** `NoticeValidationHelper.java:359-547`

---

### 6.2 Swimlane Content

#### Backend Swimlane
```
START: Check for Double Booking (triggered from Create Notice)
  ↓
[Extract notice data: vehicle_no, rule_code, pp_code, date_time]
  ↓
{All required fields present?}
  ├─→ NO → [Skip DBB check] → RETURN: Not duplicate
  └─→ YES → Continue
  ↓
{Offence Type?}
  ├─→ Type O → [Call Type O DBB Sub-flow] → (Link to Tab 2.3)
  ├─→ Type E → [Call Type E DBB Sub-flow] → (Link to Tab 2.4)
  └─→ Type U → [Skip DBB (manual processing)] → RETURN: Not duplicate
  ↓
{Duplicate detected by sub-flow?}
  ├─→ YES → RETURN: Duplicate (with original notice_no)
  └─→ NO → RETURN: Not duplicate
END
```

#### Database Swimlane
```
(No direct DB operations - routing logic only)
(Sub-flows handle DB queries)
```

---

### 6.3 Key Decision Points

| Decision Diamond | Condition | Path 1 | Path 2 | Code Reference |
|------------------|-----------|--------|--------|----------------|
| All fields present? | vehicle_no, date_time, rule_code, pp_code NOT NULL | YES: Continue | NO: Skip DBB | Line 378-381 |
| Offence Type? | offenceType value | O: Type O flow | E: Type E flow | Line 450/456 |
| Duplicate detected? | Return from sub-flow | YES: Apply PS-DBB | NO: Continue normal | Line 1159 |

---

## 7. Section 3 (FD 3.3): Type O DBB Criteria

### 7.1 Tab Overview
**Tab Name:** `2.3 - Type O DBB Criteria and Handling`

**Purpose:** Detailed logic for Parking Offence (Type O) duplicate detection.

**FD Reference:** Section 3.3 - "DBB Criteria and Handling for Parking Offence Notices (Type O)"

**Functional Flow Reference:** Tab 3 - "3.3 - Type O DBB processing flow"

**Code Reference:** `NoticeValidationHelper.java:450-455` (date match), `476-514` (eligibility)

---

### 7.2 Swimlane Content

#### Backend Swimlane
```
START: Type O DBB Check
  ↓
[Prepare query params: vehicle_no, computer_rule_code, pp_code]
  ↓
(Dashed to DB) [Query matching notices]
  ↓
{Query successful?}
  ├─→ NO → {Retry < 3?}
  |         ├─→ YES → [Wait (exponential backoff)] → Retry query
  |         └─→ NO → [Flag: dbbQueryFailed = true] → RETURN: TS-OLD required
  └─→ YES → Continue
  ↓
(Yellow Response from DB) [Receive: List of matching notices]
  ↓
{Any records returned?}
  ├─→ NO → RETURN: Not duplicate
  └─→ YES → [FOR EACH notice in list...]
  ↓
{Self-matching? (notice_no == current)}
  └─→ YES → [Skip this notice] → Next iteration
  └─→ NO → Continue
  ↓
[Extract: existing_notice_date_and_time]
  ↓
{Date NULL?}
  └─→ YES → [Skip this notice] → Next iteration
  └─→ NO → Continue
  ↓
[Compare DATE ONLY (ignore time)]
  ↓
{Date Match?}
  ├─→ NO → [Skip this notice] → Next iteration
  └─→ YES → Continue
  ↓
[Check suspension status]
  ↓
{suspension_type?}
  ├─→ NULL → DUPLICATE (active notice)
  ├─→ 'PS' → {epr_reason?}
  |            ├─→ 'FOR' → DUPLICATE (Foreign)
  |            ├─→ 'ANS' → DUPLICATE (Advisory Notice - Type O only) ⚠️ SIMPLIFIED: Direct to DUPLICATE
  |            ├─→ 'DBB' → DUPLICATE (Already DBB) ⚠️ SIMPLIFIED: Direct to DUPLICATE
  |            ├─→ Other → {crs_reason?}
  |                        ├─→ 'FP' → DUPLICATE (Paid - Further Processing)
  |                        ├─→ 'PRA' → DUPLICATE (Paid - Pending Review)
  |                        └─→ Other → NOT DUPLICATE (Other PS codes)
  └─→ Other → DUPLICATE (Treat as active)
  ↓
[Store: duplicateNoticeNo, duplicateReason]
  ↓
RETURN: Duplicate detected
END
```

#### Database Swimlane
```
(Yellow Box) [SELECT * FROM ocms_valid_offence_notice
               WHERE vehicle_no = ?
               AND computer_rule_code = ?
               AND pp_code = ?]
  ↓
(Yellow Response) [Return: List<OcmsValidOffenceNotice>]
```

---

### 7.3 Key Yellow Boxes (Type O)

| Box Name | SQL | Connection | Code Reference |
|----------|-----|------------|----------------|
| Query matching notices | `SELECT * FROM ocms_valid_offence_notice WHERE vehicle_no = ? AND computer_rule_code = ? AND pp_code = ?` | Dashed from Backend | Line 386-402 |
| Response: List of notices | Return query results | Dashed to Backend | Line 430-438 |

---

### 7.4 BR/OP Codes to Expand (Type O)

**Expand in flow boxes:**
- BR-DBB-O-001 → "Type O: Match by Rule Code, PP Code, Vehicle, **Date only** (ignore time)"
- BR-DBB-ELIG-001 → "Check suspension eligibility: NULL, PS-FOR, PS-ANS (Type O only), PS-DBB, PS-FP, PS-PRA"

**Note boxes (keep abbreviated):**
- PS-FOR, PS-ANS, PS-DBB, PS-FP, PS-PRA (with explanation in note)

---

## 8. Section 4 (FD 3.4): Type E DBB Criteria

### 8.1 Tab Overview
**Tab Name:** `2.4 - Type E DBB Criteria and Handling`

**Purpose:** Detailed logic for Payment Evasion (Type E) duplicate detection.

**FD Reference:** Section 3.4 - "DBB Criteria and Handling for Payment Evasion Notices (Type E)"

**Functional Flow Reference:** Tab 4 - "3.4 - Type E DBB processing flow"

**Code Reference:** `NoticeValidationHelper.java:456-465` (date+time match), `476-514` (eligibility)

---

### 8.2 Swimlane Content

#### Backend Swimlane
```
START: Type E DBB Check
  ↓
[Prepare query params: vehicle_no, computer_rule_code, pp_code]
  ↓
(Dashed to DB) [Query matching notices]
  ↓
{Query successful?}
  ├─→ NO → {Retry < 3?}
  |         ├─→ YES → [Wait (exponential backoff)] → Retry query
  |         └─→ NO → [Flag: dbbQueryFailed = true] → RETURN: TS-OLD required
  └─→ YES → Continue
  ↓
(Yellow Response from DB) [Receive: List of matching notices]
  ↓
{Any records returned?}
  ├─→ NO → RETURN: Not duplicate
  └─→ YES → [FOR EACH notice in list...]
  ↓
{Self-matching? (notice_no == current)}
  └─→ YES → [Skip this notice] → Next iteration
  └─→ NO → Continue
  ↓
[Extract: existing_notice_date_and_time]
  ↓
{Date/Time NULL?}
  └─→ YES → [Skip this notice] → Next iteration
  └─→ NO → Continue
  ↓
[Compare DATE + TIME (HH:MM, ignore seconds)]
  ↓
{Date Match?}
  └─→ NO → [Skip] → Next
  └─→ YES → Continue
  ↓
{Hour Match?}
  └─→ NO → [Skip] → Next
  └─→ YES → Continue
  ↓
{Minute Match?}
  └─→ NO → [Skip] → Next
  └─→ YES → Continue (DATE+TIME MATCH)
  ↓
[Check suspension status]
  ↓
{suspension_type?}
  ├─→ NULL → DUPLICATE (active notice)
  ├─→ 'PS' → {epr_reason?}
  |            ├─→ 'FOR' → DUPLICATE (Foreign)
  |            ├─→ Other → {crs_reason?}  ⚠️ FD COMPLIANCE: Type E skips ANS check entirely
  |                        ├─→ 'FP' → DUPLICATE (Paid - Further Processing)
  |                        ├─→ 'PRA' → DUPLICATE (Paid - Pending Review)
  |                        └─→ Other → NOT DUPLICATE (Other PS codes)
  └─→ Other → DUPLICATE (Treat as active)
  ↓
[Store: duplicateNoticeNo, duplicateReason]
  ↓
RETURN: Duplicate detected
END

⚠️ **FLOWCHART UPDATE (2026-01-13):** Type E eligibility flow simplified per FD Section 3.4:
- Removed ANS check path (ANS not mentioned in FD 3.4)
- Removed DBB check path (DBB not mentioned in FD 3.4)
- Only checks: FOR, FP, PRA per FD Section 3.4 Step 8
```

#### Database Swimlane
```
(Yellow Box) [SELECT * FROM ocms_valid_offence_notice
               WHERE vehicle_no = ?
               AND computer_rule_code = ?
               AND pp_code = ?]
  ↓
(Yellow Response) [Return: List<OcmsValidOffenceNotice>]
```

---

### 8.3 Key Difference: Type O vs Type E

| Element | Type O (Parking) | Type E (Payment Evasion) |
|---------|------------------|--------------------------|
| **Date Comparison** | Date only (ignore time) | Date + Time (HH:MM) |
| **Time Comparison** | ❌ Ignore | ✅ Compare HH:MM (ignore seconds) |
| **PS-ANS Check** | ✅ Yes - ANS qualifies | ❌ No - ANS NOT in eligibility list |
| **PS-DBB Check** | ✅ Yes - DBB qualifies | ❌ No - DBB NOT in eligibility list |
| **Eligible Reasons** | FOR, ANS, DBB, FP, PRA | FOR, FP, PRA only |

**⚠️ FD COMPLIANCE NOTE (2026-01-13):**
- FD Section 3.3 (Type O): Lists FOR, ANS, DBB, FP, PRA as eligible
- FD Section 3.4 (Type E): Lists FOR, FP, PRA only - NO ANS, NO DBB mentioned

**Code Reference:**
- Type O: `NoticeValidationHelper.java:450-455`
- Type E: `NoticeValidationHelper.java:456-465`
- ANS check: `NoticeValidationHelper.java:499` (with `&& "O".equals(offenseType)`)

---

### 8.4 BR/OP Codes to Expand (Type E)

**Expand in flow boxes:**
- BR-DBB-E-001 → "Type E: Match by Rule Code, PP Code, Vehicle, **Date + Time (HH:MM)** (ignore seconds)"
- BR-DBB-ELIG-002 → "Check suspension eligibility: NULL, PS-FOR, PS-FP, PS-PRA (⚠️ NO PS-ANS, NO PS-DBB for Type E per FD 3.4)"

**Note boxes (keep abbreviated):**
- PS-FOR, PS-FP, PS-PRA (Type E only checks these per FD Section 3.4)

---

## 9. Yellow Boxes (API/DB Operations)

### 9.1 Yellow Box Rules

**When to Use Yellow Box:**
- SELECT queries (Database swimlane)
- INSERT operations (Database swimlane)
- UPDATE operations (Database swimlane)
- API calls (External System swimlane - N/A for DBB)
- Response boxes (Database swimlane)

**Connection Type:** Dashed line (from Backend to DB, DB back to Backend)

**Guideline Reference:** `Docs/guidelines/create_drawio.md` - Section 11A.2

---

### 9.2 Yellow Boxes List (All Tabs)

| Box Name | SQL/Operation | Tab | Swimlane | Code Ref |
|----------|---------------|-----|----------|----------|
| Query duplicate notice number | `SELECT ... WHERE notice_no = ?` | 1.2 | Database | Line 251 |
| Response: Notice exists | Return EXISTS/NOT EXISTS | 1.2 | Database | Line 253-258 |
| Insert notice | `INSERT INTO ocms_valid_offence_notice (...)` | 1.2 | Database | Line 1000-1040 |
| Response: Notice created | Return SUCCESS | 1.2 | Database | Line 1055 |
| Query matching notices | `SELECT ... WHERE vehicle_no = ? AND ...` | 2.3/2.4 | Database | Line 386-402 |
| Response: List of notices | Return query results | 2.3/2.4 | Database | Line 430-438 |
| Update notice (PS-DBB) | `UPDATE ocms_valid_offence_notice SET suspension_type='PS', epr_reason_of_suspension='DBB' WHERE notice_no = ?` | 1.2 | Database | Line 1171-1186 |
| Response: Notice updated | Return SUCCESS | 1.2 | Database | Line 1183 |
| Update Internet DB | `UPDATE eocms_valid_offence_notice SET suspension_type='PS', epr_reason_of_suspension='DBB' WHERE notice_no = ?` | 1.2 | Database | Line 1188-1207 |
| Response: Internet updated | Return SUCCESS | 1.2 | Database | Line 1200 |
| Insert suspended_notice | `INSERT INTO ocms_suspended_notice (...)` | 1.2 | Database | Line 1209-1233 |
| Response: Suspension created | Return SUCCESS | 1.2 | Database | Line 1235 |

---

### 9.3 Yellow Box Color Code

**All yellow boxes use:**
- Fill Color: `#fff2cc` (light yellow)
- Border Color: `#d6b656` (yellow border)

**Guideline Reference:** `Docs/guidelines/create_drawio.md` - Section 11A.4

---

## 10. BR/OP Code Expansion Rules

### 10.1 When to Expand

**✅ EXPAND in flow process boxes:**
- BR-XXX → Full business rule text
- OP-XXX → Full operation description

**❌ KEEP ABBREVIATED in note boxes:**
- BR-XXX with explanation in note box
- OCMS-XXXX error codes
- PS-XXX, TS-XXX suspension codes
- Domain codes: PS, TS, FP, PRA, FOR, ANS, DBB

**Guideline Reference:** `Docs/guidelines/create_drawio.md` - Section 11A.3

---

### 10.2 BR Codes for OCMS 21

| BR Code | Full Text (for flow boxes) | Where Used |
|---------|----------------------------|------------|
| BR-DBB-001 | DBB check runs automatically after notice creation succeeds | Tab 1.2 |
| BR-DBB-002 | Exclude current notice from duplicate check query results | Tab 2.3/2.4 |
| BR-DBB-003 | Retry DBB query up to 3 times with exponential backoff (100ms, 200ms, 400ms). If all retries fail, apply TS-OLD suspension | Tab 2.3/2.4 |
| BR-DBB-004 | Latest created notice (by OCMS creation timestamp) gets PS-DBB suspension. Earlier notice remains valid | Tab 1.2 |
| BR-DBB-005 | Type U (UPL) offences route to manual processing, no automated DBB check | Tab 1.2/2.2 |
| BR-DBB-O-001 | Type O (Parking): Match by Computer Rule Code, PP Code, Vehicle Number, and Date only (ignore time) | Tab 2.3 |
| BR-DBB-E-001 | Type E (Payment Evasion): Match by Computer Rule Code, PP Code, Vehicle Number, Date and Time in HH:MM format (ignore seconds) | Tab 2.4 |
| BR-DBB-ELIG-001 | Check suspension eligibility: Active (NULL), PS-FOR, PS-ANS (Type O only), PS-DBB, PS-FP, PS-PRA qualify as duplicates | Tab 2.3/2.4 |

---

## 11. Data Dictionary Validation

### 11.1 Table/Field Names to Validate

⚠️ **CRITICAL:** Validate ALL table and field names against `Docs/data-dictionary/`

| Table | Zone | Data Dictionary File | Status |
|-------|------|----------------------|--------|
| `ocms_valid_offence_notice` | Intranet | `intranet.json` | ✅ To verify |
| `eocms_valid_offence_notice` | Internet | `internet.json` | ✅ To verify |
| `ocms_suspended_notice` | Intranet | `intranet.json` | ✅ To verify |

---

### 11.2 Fields to Validate

| Field Name | Table | Expected in Dictionary | Code Reference |
|------------|-------|------------------------|----------------|
| `notice_no` | All | ✅ | Multiple |
| `vehicle_no` | ocms_valid_offence_notice | ✅ | Line 372 |
| `notice_date_and_time` | ocms_valid_offence_notice | ✅ | Line 373 |
| `computer_rule_code` | ocms_valid_offence_notice | ✅ | Line 374 |
| `pp_code` | ocms_valid_offence_notice | ✅ | Line 375 |
| `suspension_type` | ocms_valid_offence_notice | ✅ | Line 476 |
| `epr_reason_of_suspension` | ocms_valid_offence_notice | ✅ | Line 477 |
| `crs_reason_of_suspension` | ocms_valid_offence_notice | ✅ | Line 478 |
| `epr_date_of_suspension` | ocms_valid_offence_notice | ✅ | Line 1180 |
| `due_date_of_revival` | ocms_valid_offence_notice | ✅ | Line 1181 |
| `date_of_suspension` | ocms_suspended_notice | ✅ | Line 1218 |
| `sr_no` | ocms_suspended_notice | ✅ | Line 1215 |
| `suspension_source` | ocms_suspended_notice | ✅ | Line 1219 |
| `officer_authorising_suspension` | ocms_suspended_notice | ✅ | Line 1222 |
| `suspension_remarks` | ocms_suspended_notice | ✅ | Line 1229 |

**Guideline Reference:** `Docs/guidelines/create_drawio.md` - Section 11A.1

---

### 11.3 Common Errors to Avoid

| Incorrect | Correct | Source |
|-----------|---------|--------|
| `reason_suspension` | `reason_of_suspension` | Data Dictionary |
| `epr_date` | `epr_date_of_suspension` | Data Dictionary |
| `car_park_code` | `pp_code` | Code uses `pp_code` |

---

## 12. Summary

### Flowchart Structure Overview

```
Technical Flowchart: OCMS21_Flowchart.drawio

├─ Tab 1.2: DBB in Create Notice Flow
│   └─ Shows: Where DBB check fits in notice creation
│   └─ Swimlanes: Frontend (empty), Backend (main flow), Database (queries/updates)
│
├─ Tab 2.2: High-Level DBB Processing
│   └─ Shows: Main DBB function routing (Type O vs E)
│   └─ Swimlanes: Backend (routing logic), Database (sub-flow queries)
│
├─ Tab 2.3: Type O DBB Criteria
│   └─ Shows: Detailed Type O (Parking) duplicate detection
│   └─ Swimlanes: Backend (criteria checks), Database (query matching notices)
│
└─ Tab 2.4: Type E DBB Criteria
    └─ Shows: Detailed Type E (Payment Evasion) duplicate detection
    └─ Swimlanes: Backend (criteria checks + time), Database (query matching notices)
```

---

### Key Implementation Notes

1. ✅ **Add Swimlanes** - Functional flow has none, technical flowchart needs 4 swimlanes
2. ✅ **Yellow Boxes** - All DB operations (SELECT/INSERT/UPDATE) + Response boxes
3. ✅ **Dashed Lines** - From Backend to Database, Database back to Backend
4. ✅ **BR/OP Expansion** - Expand in flow boxes, keep abbreviated in notes
5. ✅ **Section Renumbering** - FD Sec 2 → TD Sec 1, FD Sec 3 → TD Sec 2
6. ✅ **Data Dictionary** - Validate ALL table/field names
7. ⚠️ **Abbreviations** - Expand NPD, VON, SN, MVP (first use), keep PS/TS/FP domain codes

---

### Ready for Flowchart Creation

**Prerequisites:**
- ✅ plan_api.md (database operations documented)
- ✅ plan_condition.md (validation rules documented)
- ✅ plan_flowchart.md (flowchart structure planned)

**Next Step:** Create `OCMS21_Flowchart.drawio` following this plan

---

**Document Status:** ✅ Flowchart Completed & Verified

**Total Tabs Created:** 4 (1.2, 2.2, 2.3, 2.4)

---

## Revision History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-01-08 | Initial planning document |
| 1.1 | 2026-01-13 | Updated based on flowchart implementation and FD compliance review |
| 1.2 | 2026-01-13 | Final verification - fixed eligibility, audit fields, TS-OLD consistency |

### v1.2 Changes (2026-01-13)

**Tab 1.2 Fixes:**
1. ✅ **note3 Eligibility:** Fixed TS from "NOT eligible" to "eligible" (per FD 3.3 Step 5: "If suspension_type is not PS, qualifies as duplicate")
2. ✅ **db5 (TS-OLD UPDATE):** Added `upd_user_id='ocmsiz_app_conn'` for both Intranet and Internet UPDATE
3. ✅ **db7 (TS-OLD INSERT):**
   - Changed `suspension_source` from `'ocmsiz_app_conn'` to `'OCMS'` (consistent with db10)
   - Changed `officer_authorising_suspension` from `'ocmsizmgr_conn'` to `'ocmsiz_app_conn'` (consistent with db10)
   - Added `cre_user_id='ocmsiz_app_conn'`

**Tab 2.2 Fixes:**
4. ✅ **t2_call_e:** Changed description from "PS-ANS NOT eligible" to "Eligible: FOR, FP, PRA only" (clearer FD 3.4 compliance)

---

### v1.1 Changes (2026-01-13)

**Fixes Applied to Flowchart:**
1. ✅ **Typo Fix (Tab 1.2, DB7):** `officer_authorising_supension` → `officer_authorising_suspension`
2. ✅ **Audit Fields Added (Tab 1.2):**
   - db8: Added `upd_user_id='ocmsiz_app_conn'`
   - db9: Added `upd_user_id='ocmsiz_app_conn'`
   - db10: Added `cre_user_id='ocmsiz_app_conn'`

**Flow Simplifications:**
3. ✅ **Tab 2.3 (Type O):** Simplified ANS and DBB paths - direct to DUPLICATE
4. ✅ **Tab 2.4 (Type E):** Made FD-compliant - removed ANS and DBB handling per FD Section 3.4

**Type E Eligibility (FD Section 3.4 Compliance):**
- ✅ FOR → DUPLICATE
- ✅ FP/PRA → DUPLICATE
- ❌ ANS → NOT in FD 3.4 (removed from flow)
- ❌ DBB → NOT in FD 3.4 (removed from flow)

# OCMS 17 Plan Files - Revision Notes

**Date:** 2026-01-27
**Purpose:** Document changes made to align plan files with Functional Document (FD)

---

## 1. plan_condition.md

### 1.1 PP Code Removed

**Action:** Deleted entire Section 3.15 (PP - Partial Payment)

**Reason:** PP code does not exist in FD. FD only defines 18 TS codes.

| Before | After |
|--------|-------|
| 20 TS codes (included PP) | 18 TS codes (PP removed) |

---

### 1.2 Suspension Days Corrected

| Code | Description | Before | After (FD) | Status |
|------|-------------|--------|------------|--------|
| ACR | ACRA | 21 | 21 | No change |
| APE | Appeal Extension | 21 | **14** | Fixed |
| APP | Appeal | 21 | 21 | No change |
| CCE | Call Centre Enquiry | 1 | 1 | No change |
| CLV | Classified Vehicles | 90 | **21** | Fixed |
| FPL | Furnished Particulars Late | 14 | 14 | No change |
| HST | House Tenants | 30 | 30 | No change |
| INS | Insufficient Particulars | 14 | 14 | No change |
| MS | Miscellaneous | 14 | **21** | Fixed |
| NRO | Offender Data Exception | 7 | **21** | Fixed |
| OLD | Pending Investigations | 30 | **21** | Fixed |
| OUT | Enquiry Outstanding Notice | 14 | **21** | Fixed |
| PAM | Partially Matched | 14 | **21** | Fixed |
| PDP | Pending Driver Furnish | 14 | **21** | Fixed |
| PRI | Pending Release from Prison | 90 | **180** | Fixed |
| RED | Pay Reduced Amount | 21 | **14** | Fixed |
| ROV | Vehicle Ownership Exception | 7 | **21** | Fixed |
| SYS | System Issue | 7 | **21** | Fixed |
| UNC | Returned as Unclaimed | 14 | **21** | Fixed |

**Total Fixed:** 13 codes

---

### 1.3 Section Renumbering

After PP removal, sections were renumbered:

| Code | Before | After |
|------|--------|-------|
| PP | 3.15 | **Removed** |
| PRI | 3.16 | 3.15 |
| RED | 3.17 | 3.16 |
| ROV | 3.18 | 3.17 |
| SYS | 3.19 | 3.18 |
| UNC | 3.20 | 3.19 |

---

### 1.4 Summary Section Updated

| Item | Before | After |
|------|--------|-------|
| Auto Backend Codes | 9 (ACR, CLV, HST, NRO, PAM, PDP, **PP**, ROV, SYS) | 8 (ACR, CLV, HST, NRO, PAM, PDP, ROV, SYS) |

---

## 2. plan_api.md

### 2.1 SQL INSERT Statement - Field Clarification

**Location:** API 3 Database Operations, Step 4

**Clarification Added:**
```sql
-- Note: cre_date = CURRENT_TIMESTAMP, cre_user_id = officer_authorising_suspension
```

| Field | Value |
|-------|-------|
| cre_date | CURRENT_TIMESTAMP |
| cre_user_id | officer_authorising_suspension (from request) |

---

### 2.2 Summary Section Updated

| Item | Before | After |
|------|--------|-------|
| Key Features | 20 TS codes with different rules | **18 TS codes** with different rules |
| Next Steps | plan_condition.md created with 20 TS code rules | plan_condition.md created with **18 TS code rules** |

---

## 3. plan_flowchart.md

### 3.1 Auto TS Scenarios Updated

**Location:** Section 8.3

| Item | Before | After |
|------|--------|-------|
| Title | Auto TS Scenarios (9 codes) | Auto TS Scenarios (**8 codes**) |
| PP Row | Present | **Removed** |
| Total Process Boxes | 35-40 | **30-35** |

**Row Removed:**
```
| Partial payment | PP | Payment cron detects partial payment | 5-6 boxes |
```

---

## 4. Summary of All Changes

| File | Changes Made |
|------|--------------|
| plan_condition.md | PP removed, 13 suspension days fixed, sections renumbered, summary updated |
| plan_api.md | SQL clarification added, TS code count fixed (20→18) |
| plan_flowchart.md | PP removed from Auto TS table, code count fixed (9→8) |

---

## 5. Final TS Code List (18 codes per FD)

| # | Code | Days | Looping | Auto BE | OCMS Staff | PLUS Staff |
|---|------|------|---------|---------|------------|------------|
| 1 | ACR | 21 | No | Yes | Yes | No |
| 2 | APE | 14 | No | No | No | Yes |
| 3 | APP | 21 | No | No | No | Yes |
| 4 | CCE | 1 | No | No | No | Yes |
| 5 | CLV | 21 | **Yes** | Yes | Yes | No |
| 6 | FPL | 14 | No | No | Yes | No |
| 7 | HST | 30 | **Yes** | Yes | Yes | No |
| 8 | INS | 14 | No | No | Yes | No |
| 9 | MS | 21 | No | No | Yes | Yes |
| 10 | NRO | 21 | No | Yes | Yes | No |
| 11 | OLD | 21 | No | No | Yes | No |
| 12 | OUT | 21 | No | No | Yes | No |
| 13 | PAM | 21 | No | Yes | Yes | No |
| 14 | PDP | 21 | No | Yes | Yes | No |
| 15 | PRI | 180 | No | No | Yes | Yes |
| 16 | RED | 14 | No | No | No | Yes |
| 17 | ROV | 21 | No | Yes | Yes | No |
| 18 | SYS | 21 | No | Yes | Yes | No |
| 19 | UNC | 21 | No | No | Yes | No |

**Note:** PP (Partial Payment) is NOT in the FD TS code list.

---

## 6. Fields Clarification

### 6.1 Fields NOT in API Request (User Clarification)

| Field | Status | Notes |
|-------|--------|-------|
| case_no | Removed | Not required in TS request |

### 6.2 Fields in INSERT Statement

| Field | Source |
|-------|--------|
| cre_date | CURRENT_TIMESTAMP |
| cre_user_id | officer_authorising_suspension |

---

**End of Revision Notes**

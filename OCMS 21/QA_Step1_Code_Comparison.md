# OCMS 21 - QA Step 1: Flowchart vs Code Comparison

## Document Information

| Field | Value |
|-------|-------|
| **QA Step** | Step 1 - Compare with Code |
| **Date** | 2026-01-08 |
| **Flowchart** | OCMS21_Flowchart.drawio |
| **Code Reference** | CreateNoticeServiceImpl.java, NoticeValidationHelper.java |
| **Purpose** | Verify flowchart accuracy against actual implementation |
| **Status** | ✅ RESOLVED - Decisions logged in DECISION_LOG.md |

---

## 🎯 DECISION UPDATE (2026-01-08)

Critical mismatches have been **RESOLVED** with BA approval:

| Issue | Decision | Reference |
|-------|----------|-----------|
| **PS-DBB Revival Date** | ✅ Follow FD - Use **NULL** (permanent suspension) | DECISION_LOG.md Fix 1 |
| **Suspension Source** | ✅ Use **"OCMS"** (system name, not connection user) | DECISION_LOG.md Fix 2 |

**Action:** Backend team to implement fixes per DECISION_LOG.md

**Flowchart Status:** ✅ Correct (follows FD requirements)

---

## Status Legend

| Symbol | Meaning | Action Required |
|--------|---------|-----------------|
| ✅ | **Match** | Flowchart correctly represents code |
| ⚠️ | **Mismatch** | Different implementation, needs clarification |
| ❌ | **Missing** | Feature not in code OR not in flowchart |

---

## Tab 1: 1.2 - DBB in Create Notice Flow

### Comparison Table

| # | Item | Flowchart | Code Location | Status | Notes |
|---|------|-----------|---------------|--------|-------|
| 1 | Entry point: Receive data to create Notice | START → Receive data | CreateNoticeServiceImpl.java:187-286 | ✅ Match | Multiple endpoints (standard, staff, REPCCS, EHT) |
| 2 | Check duplicate Notice Number | Function box with OCMS-4000 error | Line 241-258 (REPCCS), Line 816-847 (commented out for others) | ⚠️ Mismatch | Check is ACTIVE for REPCCS, COMMENTED OUT for other sources |
| 3 | Identify Offence Type | Function box → Decision (O/E/U) | Line 849-863 | ✅ Match | Gets offense type from rule code table |
| 4 | Route Type U to manual processing | Decision branch → External flow | Line 791 comment, not explicitly handled | ⚠️ Missing | Comment mentions Type U, but no explicit routing in code |
| 5 | Create Notice | Process box | Line 1000-1055 | ✅ Match | Creates notice in DB (both zones) |
| 6 | Check for Double Booking | Main DBB function call | Line 1063-1065 | ✅ Match | `checkDuplicateOffenseDetails()` |
| 7 | DBB Query Failure → TS-OLD | Error path to TS-OLD suspension | Line 1068-1157 | ✅ Match | Applies TS-OLD when `dbbQueryFailed = true` |
| 8 | Duplicate Detected → PS-DBB | Success path to PS-DBB suspension | Line 1159-1253 | ✅ Match | Applies PS-DBB when duplicate found |
| 9 | Update Intranet notice (PS-DBB) | Yellow box: UPDATE ocms_valid_offence_notice | Line 1171-1186 | ✅ Match | Sets suspension_type, epr_reason, epr_date |
| 10 | Update Internet notice (PS-DBB) | Yellow box: UPDATE eocms_valid_offence_notice | Line 1188-1207 | ✅ Match | Same fields as Intranet |
| 11 | Insert suspended_notice | Yellow box: INSERT into ocms_suspended_notice | Line 1209-1235 | ✅ Match | Creates suspension record with SR number |
| 12 | Calculate due_date_of_revival | Process box → Calculate date | Line 1167-1169 | ⚠️ Mismatch | **FD says NULL, code calculates duration** |
| 13 | Continue to AN Qualification (Type O) | Next flow for non-DBB Type O | Line 1257-1277 | ✅ Match | OCMS AN check for Type O only |

**Tab 1 Summary:**
- ✅ **Match:** 10 items
- ⚠️ **Mismatch:** 3 items (duplicate check commented out, Type U routing unclear, revival date calculation)
- ❌ **Missing:** 0 items

---

## Tab 2: 2.2 - High-Level DBB Processing

### Comparison Table

| # | Item | Flowchart | Code Location | Status | Notes |
|---|------|-----------|---------------|--------|-------|
| 14 | Entry: Check for Double Booking | Function entry point | NoticeValidationHelper.java:359 | ✅ Match | `checkDuplicateOffenseDetails()` |
| 15 | Extract notice data fields | Process box: vehicle_no, rule_code, pp_code, date_time | Line 363-375 | ✅ Match | Extracts from DTO |
| 16 | Validate required fields | Decision: All fields present? | Line 378-381 | ✅ Match | Returns false if any field is NULL |
| 17 | Get offense type from data map | Process box | Line 370 | ✅ Match | Offense type set by caller |
| 18 | Route by offense type | Decision diamond: O/E/U | Line 450 (Type O), 456 (Type E), 467 (fallback) | ✅ Match | Type-specific logic |
| 19 | Type U → Skip DBB | Decision branch | Line 467 (fallback to exact match) | ⚠️ Mismatch | No explicit Type U skip, uses fallback logic |
| 20 | Type O → Call Type O sub-flow | Link to Tab 2.3 | Line 450-455 (date-only logic) | ✅ Match | Date comparison only |
| 21 | Type E → Call Type E sub-flow | Link to Tab 2.4 | Line 456-465 (date+time logic) | ✅ Match | Date + HH:MM comparison |
| 22 | Return duplicate status | Return true/false | Line 534 (duplicate), 540 (not duplicate) | ✅ Match | Boolean return with stored data |

**Tab 2 Summary:**
- ✅ **Match:** 8 items
- ⚠️ **Mismatch:** 1 item (Type U handling implicit, not explicit)
- ❌ **Missing:** 0 items

---

## Tab 3: 2.3 - Type O DBB Criteria

### Comparison Table

| # | Item | Flowchart | Code Location | Status | Notes |
|---|------|-----------|---------------|--------|-------|
| 23 | Prepare query params | Process box: vehicle_no, computer_rule_code, pp_code | Line 387-390 | ✅ Match | Map with query parameters |
| 24 | Query matching notices | Yellow box: SELECT from ocms_valid_offence_notice | Line 402 | ✅ Match | `validOffenceNoticeService.getAll()` |
| 25 | Retry logic (3 attempts) | Loop with retry counter | Line 394-420 | ✅ Match | maxRetries = 3, retryCount loop |
| 26 | Exponential backoff wait | Process: 100ms, 200ms, 400ms | Line 410-417 | ✅ Match | `100 * Math.pow(2, retryCount - 1)` |
| 27 | All retries failed → Flag TS-OLD | Decision → Set dbbQueryFailed flag | Line 422-428 | ✅ Match | Sets flag and exception message |
| 28 | Check records returned | Decision: Any record returned? | Line 430-438 | ✅ Match | Checks if response.getData() has items |
| 29 | Self-matching prevention | Decision: Skip if notice_no matches | Line 436-439 | ✅ Match | BR-DBB-002 implemented |
| 30 | Extract existing date/time | Process box | Line 441-445 | ✅ Match | Gets noticeDateAndTime, checks NULL |
| 31 | Type O: Compare DATE only | Process: Date-only comparison (ignore time) | Line 450-455 | ✅ Match | `toLocalDate().equals()` |
| 32 | Date match check | Decision diamond | Line 472 | ✅ Match | `if (dateTimeMatch)` |
| 33 | Check suspension_type | Decision: NULL/PS/TS/Other | Line 476-520 | ✅ Match | Complete decision tree |
| 34 | Check PS-FOR | Decision: epr_reason = 'FOR' | Line 494-496 | ✅ Match | Foreign vehicle eligibility |
| 35 | Check PS-ANS (Type O only) | Decision: epr_reason = 'ANS' AND type = 'O' | Line 499-501 | ✅ Match | **Type O specific check** |
| 36 | Check PS-DBB | Decision: epr_reason = 'DBB' | Line 502-504 | ✅ Match | Already DBB suspended |
| 37 | Check PS-FP/PRA | Decision: crs_reason = 'FP' OR 'PRA' | Line 507-509 | ✅ Match | Paid notice eligibility |
| 38 | Other PS codes → Not duplicate | Decision branch | Line 510-514 | ✅ Match | Other PS codes don't qualify |
| 39 | Store duplicate notice info | Process: duplicateNoticeNo, duplicateReason | Line 532-533 | ✅ Match | Stored in data map |
| 40 | Return: Duplicate detected | Return true | Line 534 | ✅ Match | Boolean return |

**Tab 3 Summary:**
- ✅ **Match:** 18 items
- ⚠️ **Mismatch:** 0 items
- ❌ **Missing:** 0 items

---

## Tab 4: 2.4 - Type E DBB Criteria

### Comparison Table

| # | Item | Flowchart | Code Location | Status | Notes |
|---|------|-----------|---------------|--------|-------|
| 41 | Prepare query params | Process box: vehicle_no, computer_rule_code, pp_code | Line 387-390 | ✅ Match | Same as Type O |
| 42 | Query matching notices | Yellow box: SELECT from ocms_valid_offence_notice | Line 402 | ✅ Match | Same query as Type O |
| 43 | Retry logic (3 attempts) | Loop with retry counter | Line 394-420 | ✅ Match | Same retry mechanism |
| 44 | Exponential backoff wait | Process: 100ms, 200ms, 400ms | Line 410-417 | ✅ Match | Same backoff strategy |
| 45 | All retries failed → Flag TS-OLD | Decision → Set dbbQueryFailed flag | Line 422-428 | ✅ Match | Same fallback logic |
| 46 | Check records returned | Decision: Any record returned? | Line 430-438 | ✅ Match | Same check as Type O |
| 47 | Self-matching prevention | Decision: Skip if notice_no matches | Line 436-439 | ✅ Match | Same as Type O |
| 48 | Extract existing date/time | Process box | Line 441-445 | ✅ Match | Same as Type O |
| 49 | Type E: Compare DATE + TIME (HH:MM) | Process: Date + Hour + Minute (ignore seconds) | Line 456-465 | ✅ Match | **3-part comparison: date, hour, minute** |
| 50 | Date match check | Decision: date equals? | Line 458 | ✅ Match | `toLocalDate().equals()` |
| 51 | Hour match check | Decision: hour equals? | Line 459 | ✅ Match | `getHour() == getHour()` |
| 52 | Minute match check | Decision: minute equals? | Line 460 | ✅ Match | `getMinute() == getMinute()` |
| 53 | Overall date+time match | Decision: All 3 match | Line 461 | ✅ Match | `dateMatch && hourMatch && minuteMatch` |
| 54 | Check suspension_type | Decision: NULL/PS/TS/Other | Line 476-520 | ✅ Match | Same as Type O |
| 55 | Check PS-FOR | Decision: epr_reason = 'FOR' | Line 494-496 | ✅ Match | Same as Type O |
| 56 | Check PS-ANS (Type E skips) | Decision: ANS check ONLY for Type O | Line 499 (`&& "O".equals(offenseType)`) | ✅ Match | **Type E explicitly excludes ANS** |
| 57 | Check PS-DBB | Decision: epr_reason = 'DBB' | Line 502-504 | ✅ Match | Same as Type O |
| 58 | Check PS-FP/PRA | Decision: crs_reason = 'FP' OR 'PRA' | Line 507-509 | ✅ Match | Same as Type O |
| 59 | Other PS codes → Not duplicate | Decision branch | Line 510-514 | ✅ Match | Same as Type O |
| 60 | Store duplicate notice info | Process: duplicateNoticeNo, duplicateReason | Line 532-533 | ✅ Match | Same as Type O |
| 61 | Return: Duplicate detected | Return true | Line 534 | ✅ Match | Same as Type O |

**Tab 4 Summary:**
- ✅ **Match:** 21 items
- ⚠️ **Mismatch:** 0 items
- ❌ **Missing:** 0 items

---

## Overall QA Step 1 Summary

### Total Items Checked: 61

| Status | Count | Percentage |
|--------|-------|------------|
| ✅ **Match** | 57 | 93.4% |
| ⚠️ **Mismatch** | 4 | 6.6% |
| ❌ **Missing** | 0 | 0% |

---

## Critical Mismatches Requiring Action

### 1. Duplicate Notice Number Check (Item #2)

**Flowchart:** Shows check is always performed with OCMS-4000 error

**Code Reality:**
- REPCCS endpoint: ✅ Check is ACTIVE (Line 241-258)
- Other endpoints: ⚠️ Check is COMMENTED OUT (Line 815-847 with `/* USER ASK TO TAKE OUT THIS PROCESS FOR NOW */`)

**Impact:** Flowchart shows universal check, but code only checks for REPCCS source

**Recommendation:**
- ✅ **Option 1:** Update flowchart to show check is REPCCS-only
- ✅ **Option 2:** Uncomment check for all sources (if BA confirms needed)
- ⚠️ **Option 3:** Add note box explaining source-specific behavior

**Action:** Clarify with BA whether duplicate notice number check should be universal or source-specific

---

### 2. Type U Routing (Item #4, #19)

**Flowchart:** Shows explicit "Route to Type U processing" branch

**Code Reality:**
- No explicit Type U handling in DBB check
- Type U uses fallback exact date/time match (Line 467-469)
- Comment mentions Type U at Line 791 but no routing logic

**Impact:** Flowchart implies Type U has special handling, code treats it as fallback

**Recommendation:**
- Add explicit Type U skip logic in code: `if ("U".equals(offenseType)) return false;`
- OR update flowchart to show Type U uses default exact match logic

**Action:** Clarify with BA whether Type U should be explicitly excluded from DBB

---

### 3. PS-DBB Revival Date (Item #12)

**Flowchart:** Shows calculation of due_date_of_revival

**Code Reality:**
```java
// Line 1167-1169
int suspensionDurationDays = getSuspensionDurationDays("DBB", 30);
LocalDateTime dueDateOfRevival = currentDate.plusDays(suspensionDurationDays);
```

**FD Requirement:** Section 3.5.1 - `due_date_of_revival: NULL (as notice is permanently suspended)`

**Impact:** 🔴 **CRITICAL** - PS-DBB notices will auto-revive after duration days instead of staying permanently suspended

**Recommendation:**
- **MUST FIX CODE:** Change to `dueDateOfRevival = null;` for PS-DBB
- Update flowchart to show NULL assignment (not calculation)

**Action:** 🔴 **HIGH PRIORITY** - Fix code to set NULL for permanent suspension

---

### 4. Suspension Source Field (Throughout)

**Flowchart:** Shows `suspension_source = "ocmsiz_app_conn"`

**Code Reality:**
```java
// Line 1130
suspendedNotice.setSuspensionSource("ocmsiz_app_conn");

// Line 1133
suspendedNotice.setOfficerAuthorisingSupension("ocmsizmgr_conn");
```

**FD Requirement:** Section 3.5.1 - `suspension_source: OCMS` and `officer_authorising_suspension: ocmsiz_app_conn`

**Impact:** ⚠️ Field values don't match FD specification

**Recommendation:**
- Clarify correct values with BA
- Update code OR flowchart to match agreed values

**Action:** Confirm system user naming convention

---

## Positive Findings (Excellent Alignment)

### Strengths:
1. ✅ **Type O vs Type E Logic:** Code perfectly implements date-only vs date+time comparison
2. ✅ **Retry Mechanism:** Exponential backoff (100ms, 200ms, 400ms) exactly as designed
3. ✅ **TS-OLD Fallback:** Flag-based fallback logic correctly implemented
4. ✅ **Suspension Eligibility:** All 6 eligible codes (NULL, FOR, ANS*, DBB, FP, PRA) correctly checked
5. ✅ **ANS Type O Specific:** Code correctly implements `&& "O".equals(offenseType)` for ANS check
6. ✅ **Self-Matching Prevention:** BR-DBB-002 correctly implemented
7. ✅ **Dual-Zone Updates:** Both Intranet and Internet DB updated correctly
8. ✅ **SR Number Auto-Increment:** MAX(sr_no) + 1 logic correctly implemented

---

## Recommendations

### Immediate Actions Required:

1. 🔴 **HIGH:** Fix PS-DBB revival date (NULL instead of calculated)
2. 🟡 **MEDIUM:** Clarify duplicate notice number check scope (universal vs REPCCS-only)
3. 🟡 **MEDIUM:** Clarify Type U handling (explicit skip vs fallback)
4. 🟡 **MEDIUM:** Confirm suspension_source and officer_authorising_suspension values

### Documentation Updates:

5. Update flowchart note boxes to highlight:
   - Duplicate check is source-dependent (if confirmed by BA)
   - Type U uses fallback logic (if confirmed by BA)
   - Revival date field mismatch (until fixed)

---

## Conclusion

**Overall Assessment:** ✅ **STRONG ALIGNMENT** (93.4% match)

The flowchart accurately represents the implemented code logic with only 4 mismatches, 1 of which is critical (revival date) and requires immediate fix. The core DBB detection logic (Type O/E criteria, retry mechanism, suspension eligibility) is perfectly aligned between flowchart and code.

**QA Step 1 Status:** ⚠️ **CONDITIONAL PASS** - Pending resolution of 4 mismatches

**Next Step:** Proceed to QA Step 2 (Verify with FD) while BA clarifies mismatch items.

---

**Prepared By:** Claude Code
**Date:** 2026-01-08
**Review Required:** BA/Tech Lead sign-off on mismatch resolutions

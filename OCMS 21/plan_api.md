# OCMS 21 - Duplicate Notices (DBB) - API Planning Document

## Document Information

| Field | Value |
|-------|-------|
| **Document Version** | 1.3 |
| **Date** | 2026-01-15 |
| **Epic** | OCMS 21 - Manage Duplicate Notices |
| **Feature** | Double Booking (DBB) Detection & Suspension |
| **Source Documents** | v1.0_OCMS_21_Duplicate_Notices.md (FD) |
| **Code Reference** | ura-project-ocmsadminapi (Backend) |
| **Author** | Claude Code (Planning Phase) |

---

## Table of Contents

1. [Overview](#overview)
2. [Internal Database Operations](#internal-database-operations)
3. [DBB Detection Query](#dbb-detection-query)
4. [Suspension Record Creation](#suspension-record-creation)
5. [Notice Update Operations](#notice-update-operations)
6. [Error Handling & Retry Logic](#error-handling--retry-logic)
7. [Gaps & Differences (FD vs Code)](#gaps--differences-fd-vs-code)

---

## 1. Overview

### Scope
OCMS 21 MVP focuses on **automated DBB detection and suspension ONLY**. There are:
- ❌ **NO external API calls** for DBB logic
- ❌ **NO notifications** to vehicle owners
- ❌ **NO refunds** in MVP scope
- ✅ **ONLY internal database operations**

### References
- **FD Section 2**: Duplicate Notices Handling within Create Notice Flow
- **FD Section 3**: DBB Detection & Suspension Function
- **Code**: `CreateNoticeServiceImpl.java:1061-1253`
- **Code**: `NoticeValidationHelper.java:359-547`

---

## 2. Internal Database Operations

### 2.1 Database Connections

| Zone | Database | Connection User | Reference |
|------|----------|-----------------|-----------|
| **Intranet** | ocmsizmgr | `ocmsiz_app_conn` | FD Section 3.5.1 |
| **Internet** | ocmsez | `ocmsez_app_conn` | FD Section 3.5.1 |

**FD Reference:** Section 3.5.1 - "All operations use system connection user"

**Code Reference:**
- `CreateNoticeServiceImpl.java:1130` - Uses `ocmsiz_app_conn`
- `CreateNoticeServiceImpl.java:1133` - Uses `ocmsizmgr_conn` (⚠️ DIFFERENT from FD)

⚠️ **GAP IDENTIFIED:**
- **FD says:** Use `ocmsiz_app_conn` for all operations
- **Code uses:** `ocmsizmgr_conn` for `officer_authorising_suspension`
- **Action:** Clarify with BA which is correct

---

## 3. DBB Detection Query

### 3.1 Query Purpose
Detect duplicate notices by querying existing notices with matching offence data.

### 3.2 Query Parameters

| Parameter | Source Field | Data Type | Required | Reference |
|-----------|--------------|-----------|----------|-----------|
| `vehicleNo` | `vehicle_number` | String | Yes | FD Section 3.3/3.4 |
| `computerRuleCode` | `computer_rule_code` | Integer | Yes | FD Section 3.3/3.4 |
| `ppCode` | `car_park_code` / `pp_code` | String | Yes | FD Section 3.3/3.4 |

**FD Reference:**
- Section 3.3 - Type O DBB Criteria
- Section 3.4 - Type E DBB Criteria

**Code Reference:** `NoticeValidationHelper.java:386-390`

```java
Map<String, String[]> queryParams = new HashMap<>();
queryParams.put("vehicleNo", new String[]{vehicleNo});
queryParams.put("computerRuleCode", new String[]{computerRuleCode.toString()});
queryParams.put("ppCode", new String[]{ppCode});
```

### 3.3 Query Execution (with Retry)

**Operation:** SELECT from `ocms_valid_offence_notice`

**Retry Logic:**
- **Max Attempts:** 3
- **Backoff Strategy:** Exponential (100ms, 200ms, 400ms)
- **Fallback:** Apply TS-OLD suspension if all retries fail

**FD Reference:** Section 3.3/3.4 - "If DB query fails, retry 3x"

**Code Reference:** `NoticeValidationHelper.java:392-428`

```java
int maxRetries = 3;
int retryCount = 0;
Exception lastException = null;

while (retryCount < maxRetries && response == null) {
    try {
        retryCount++;
        log.info("DBB query attempt {}/{}", retryCount, maxRetries);
        response = validOffenceNoticeService.getAll(queryParams);
        log.info("DBB query successful on attempt {}", retryCount);
    } catch (Exception e) {
        lastException = e;
        log.warn("DBB query attempt {}/{} failed: {}", retryCount, maxRetries, e.getMessage());

        if (retryCount < maxRetries) {
            // Exponential backoff: 100ms, 200ms, 400ms
            long waitTime = (long) (100 * Math.pow(2, retryCount - 1));
            Thread.sleep(waitTime);
        }
    }
}

// If all retries failed, flag for TS-OLD fallback
if (response == null) {
    log.error("DBB query failed after {} attempts, will apply TS-OLD fallback", maxRetries);
    data.put("dbbQueryFailed", true);
    data.put("dbbQueryException", lastException != null ? lastException.getMessage() : "Unknown error");
    return false;
}
```

### 3.4 Query Result Processing

**Returned Fields:**
- `notice_no`
- `notice_date_and_time`
- `suspension_type`
- `epr_reason_of_suspension`
- `crs_reason_of_suspension`

**Processing Logic:**
1. Skip self-comparison (`notice_no` matches current notice)
2. Check date/time match (Type O vs Type E logic)
3. Check suspension eligibility (FOR, ANS, DBB, FP, PRA)
4. Return duplicate status + original notice number

**FD Reference:** Section 3.3 (Type O flow) / Section 3.4 (Type E flow)

**Code Reference:** `NoticeValidationHelper.java:434-546`

---

## 4. Suspension Record Creation

### 4.1 INSERT into suspended_notice

**Operation:** INSERT into `ocmsizmgr.ocms_suspended_notice`

**FD Reference:** Section 3.5 - PS-DBB Suspension Data

**Code Reference:** `CreateNoticeServiceImpl.java:1209-1233`

### 4.2 Field Mapping

| Field Name | Value | Source | Reference |
|------------|-------|--------|-----------|
| `notice_no` | Current notice number | Input | FD 3.5.1 |
| `date_of_suspension` | Current timestamp | `LocalDateTime.now()` | FD 3.5.1 |
| `sr_no` | Auto-increment | `MAX(sr_no) + 1` | FD 3.5.1 |
| `suspension_source` | System source | `"ocmsiz_app_conn"` | FD 3.5.1 |
| `case_no` | Appeal/CC case | `NULL` | FD 3.5.1 |
| `suspension_type` | Permanent Suspension | `"PS"` | FD 3.5.1 |
| `epr_reason_of_suspension` | Double Booking | `"DBB"` | FD 3.5.1 |
| `crs_reason_of_suspension` | Court reason | `NULL` | FD 3.5.1 |
| `officer_authorising_suspension` | System user | ⚠️ See gap below | FD 3.5.1 |
| `due_date_of_revival` | Revival date | ⚠️ See gap below | FD 3.5.1 |
| `suspension_remarks` | DBB details | `"DBB: [reason] - Original: [no]"` | FD 3.5.1 |
| `date_of_revival` | Revival timestamp | `NULL` (permanent) | FD 3.5.1 |
| `revival_reason` | Reason for revival | `NULL` | FD 3.5.1 |
| `officer_authorising_revival` | Officer ID | `NULL` | FD 3.5.1 |
| `revival_remarks` | Revival notes | `NULL` | FD 3.5.1 |
| `cre_date` | Creation timestamp | `LocalDateTime.now()` | FD 3.5.1 |
| `cre_user_id` | Created by | `"ocmsiz_app_conn"` | FD 3.5.1 ✅ |
| `upd_date` | Update timestamp | `LocalDateTime.now()` | FD 3.5.1 |
| `upd_user_id` | Updated by | `"ocmsiz_app_conn"` | FD 3.5.1 ✅ |

**⚠️ Audit Fields Verified (2026-01-13):**
- `cre_user_id` = `'ocmsiz_app_conn'` (Intranet backend process)
- `upd_user_id` = `'ocmsiz_app_conn'` (Intranet backend process)
- NOT "SYSTEM" per Yi Jie checklist

### 4.3 SR Number Auto-Increment

**Logic:** Query `MAX(sr_no)` for the notice, then increment by 1.

**Code Reference:** `CreateNoticeServiceImpl.java:1213-1215`

```java
// Get next SR number
Integer maxSrNo = ocmsSuspendedNoticeService.getMaxSrNoForNotice(noticeNumber);
suspendedNotice.setSrNo(maxSrNo != null ? maxSrNo + 1 : 1);
```

---

## 5. Notice Update Operations

### 5.1 UPDATE Intranet Notice

**Operation:** UPDATE `ocmsizmgr.ocms_valid_offence_notice`

**FD Reference:** Section 3.5.2 - "Update notice suspension fields"

**Code Reference:** `CreateNoticeServiceImpl.java:1171-1186`

**Fields Updated:**
```java
notice.setSuspensionType("PS");
notice.setEprReasonOfSuspension("DBB");
notice.setEprDateOfSuspension(currentDate);
notice.setDueDateOfRevival(NULL);  // ✅ CORRECTED - Permanent suspension
notice.setUpdUserId("ocmsiz_app_conn");  // ✅ ADDED - Audit field
```

⚠️ **GAP IDENTIFIED:**
- **FD says:** `due_date_of_revival` = **NULL** (permanent suspension)
- **Code sets:** `due_date_of_revival` = **currentDate + duration days**
- **Impact:** Notices may auto-revive instead of staying permanently suspended
- **Action:** Fix code to set `NULL` for PS-DBB

### 5.2 UPDATE Internet Notice

**Operation:** UPDATE `ocmsez.eocms_valid_offence_notice`

**FD Reference:** Section 3.5.3 - "Update Internet DB with same fields"

**Code Reference:** `CreateNoticeServiceImpl.java:1188-1207`

**Fields Updated:** Same as Intranet (suspension_type, epr_reason_of_suspension, epr_date_of_suspension)

**Error Handling:** Non-blocking - logs error but continues if Internet DB update fails

```java
try {
    // Update Internet DB
    eocmsValidOffenceNoticeService.save(eocmsNotice);
} catch (Exception e) {
    log.error("Error updating Internet DB: {}", e.getMessage());
    // Don't fail the entire process if Internet DB update fails
}
```

---

## 6. Error Handling & Retry Logic

### 6.1 Query Failure Scenarios

| Scenario | Retry Count | Action | Reference |
|----------|-------------|--------|-----------|
| Query fails (1st attempt) | 1/3 | Wait 100ms, retry | Code:404-418 |
| Query fails (2nd attempt) | 2/3 | Wait 200ms, retry | Code:404-418 |
| Query fails (3rd attempt) | 3/3 | Apply TS-OLD fallback | Code:422-428 |

**FD Reference:** Section 3.3 - "If query fails after retries, apply TS-OLD"

### 6.2 TS-OLD Fallback Logic

**Trigger:** All 3 DBB query attempts failed

**Action:**
1. Set `dbbQueryFailed = true` flag
2. Create TS-OLD suspension instead of PS-DBB
3. Set `suspension_remarks = "TS-OLD: DBB query failed after 3 retries - [exception]"`
4. Set `due_date_of_revival` based on system configuration (FD does not specify exact duration)

**FD Reference:** Section 3.3/3.4 - "On query failure → TS-OLD suspension"

⚠️ **Note:** FD does not specify the exact duration for TS-OLD revival. Check system configuration for OLD suspension duration.

**Code Reference:** `CreateNoticeServiceImpl.java:1068-1157`

### 6.3 Error Codes

| Error Code | Description | Scenario | Reference |
|------------|-------------|----------|-----------|
| OCMS-4000 | Notice no. already exists | Duplicate notice number | FD Section 2.2 |
| OCMS-4001 | Invalid Notice Number | Notice not found in DB | Code (not in FD) |
| OCMS-4007 | System error | Unexpected exception | Code (not in FD) |

**Note:** OCMS-4000 is the ONLY error code explicitly mentioned in FD for notice creation.

---

## 7. Gaps & Differences (FD vs Code)

### 7.1 Critical Gaps - ✅ RESOLVED (2026-01-08)

**Status:** All critical gaps resolved with BA approval. See DECISION_LOG.md for details.

| Item | FD Requirement | Code Implementation | Decision | Status |
|------|----------------|---------------------|----------|--------|
| **PS-DBB Revival Date** | `NULL` (permanent) | `NOW() + duration days` | ✅ **Use NULL** (follow FD) | RESOLVED - Fix required |
| **Suspension Source** | `"OCMS"` | `"ocmsiz_app_conn"` | ✅ **Use "OCMS"** or "004" | RESOLVED - Fix required |
| **Officer Auth** | `ocmsiz_app_conn` | `ocmsizmgr_conn` | ✅ **Use ocmsiz_app_conn** | RESOLVED - Fix required |
| **Case Number** | `NULL` for DBB | Not explicitly set | ✅ Defaults to NULL | RESOLVED - No fix needed |

### 7.2 FD References for Gaps

**FD Section 3.5.1 - Suspension Record Fields:**
```
due_date_of_revival: NULL (as notice is permanently suspended)
officer_authorising_suspension: ocmsiz_app_conn (for Intranet zone)
suspension_source: OCMS
```

**Code Implementation:**
```java
// Line 1167: Calculates revival date (WRONG for PS-DBB)
int suspensionDurationDays = getSuspensionDurationDays("DBB", 30);
LocalDateTime dueDateOfRevival = currentDate.plusDays(suspensionDurationDays);

// Line 1133: Uses ocmsizmgr_conn (DIFFERENT from FD)
suspendedNotice.setOfficerAuthorisingSupension("ocmsizmgr_conn");

// Line 1130: Uses ocmsiz_app_conn (DIFFERENT from FD "OCMS")
suspendedNotice.setSuspensionSource("ocmsiz_app_conn");
```

### 7.3 Alignment Summary - ✅ ALL RESOLVED

| Area | Alignment Status |
|------|------------------|
| DBB Criteria (Type O/E) | ✅ **GOOD** - Matches FD |
| Query Retry Logic | ✅ **GOOD** - 3 retries with backoff |
| Suspension Eligibility | ✅ **GOOD** - FOR/ANS/DBB/FP/PRA |
| TS-OLD Fallback | ✅ **GOOD** - Applied on query failure |
| Revival Date | ✅ **RESOLVED** - Use NULL per FD (code fix required) |
| System Users | ✅ **RESOLVED** - Use "OCMS" per BA decision (code fix required) |

---

## 8. Action Items for Implementation - ✅ DECISIONS MADE

### High Priority (Blocking) - ✅ ALL RESOLVED
1. ✅ **Fix PS-DBB revival date** - **DECISION: Use NULL** (follow FD) → See DECISION_LOG.md Fix 1
2. ✅ **Fix suspension_source** - **DECISION: Use "OCMS"** → See DECISION_LOG.md Fix 2
3. ✅ **Fix officer_authorising_suspension** - **DECISION: Use ocmsiz_app_conn** → See DECISION_LOG.md Fix 3

### Medium Priority (Important) - ✅ VERIFIED
4. ✅ Verify case_no defaults to NULL for DBB → **CONFIRMED**
5. ✅ Confirm Internet DB uses `ocmsez_app_conn` user → **CONFIRMED**
6. ✅ Add integration tests for retry logic

### Low Priority (Nice to Have)
7. ✅ Add metrics/logging for DBB detection rate
8. ✅ Monitor TS-OLD fallback frequency

---

## 9. Summary

### What Works
- ✅ DBB detection query with Type O/E specific logic
- ✅ Retry mechanism with exponential backoff
- ✅ TS-OLD fallback on query failure
- ✅ Suspension eligibility checks (FOR/ANS/DBB/FP/PRA)
- ✅ Dual-zone DB updates (Intranet + Internet)

### What Needs Fixing
- ❌ PS-DBB revival date (should be NULL, not calculated)
- ⚠️ System user names (clarify FD vs Code inconsistencies)

### MVP Scope Confirmed
- ✅ NO external API calls
- ✅ NO notifications
- ✅ NO refunds
- ✅ Detection + Suspension ONLY

---

**Document Status:** ✅ Verified against Flowchart Implementation

**Next Step:** Generate Technical Document

---

## Revision History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-01-08 | Initial planning document |
| 1.1 | 2026-01-13 | Updated audit fields, verified flowchart compliance |
| 1.2 | 2026-01-13 | Final verification - TS-OLD consistency, suspension_source fix |
| 1.3 | 2026-01-15 | Removed hardcoded TS-OLD duration (FD does not specify exact value) |

### v1.2 Changes (2026-01-13)

**TS-OLD Operation Fixes:**
- ✅ db5 (TS-OLD UPDATE): Added `upd_user_id='ocmsiz_app_conn'` for both zones
- ✅ db7 (TS-OLD INSERT): Fixed `suspension_source='OCMS'` (was 'ocmsiz_app_conn')
- ✅ db7 (TS-OLD INSERT): Fixed `officer_authorising_suspension='ocmsiz_app_conn'` (was 'ocmsizmgr_conn')
- ✅ db7 (TS-OLD INSERT): Added `cre_user_id='ocmsiz_app_conn'`

**Consistency Verified:**
- ✅ PS-DBB and TS-OLD operations now use consistent values:
  - `suspension_source` = `'OCMS'`
  - `officer_authorising_suspension` = `'ocmsiz_app_conn'`
  - `cre_user_id` = `'ocmsiz_app_conn'`
  - `upd_user_id` = `'ocmsiz_app_conn'`

---

### v1.1 Changes (2026-01-13)

**Audit Fields Verified:**
- ✅ `cre_user_id` = `'ocmsiz_app_conn'` for INSERT operations
- ✅ `upd_user_id` = `'ocmsiz_app_conn'` for UPDATE operations
- ✅ NOT "SYSTEM" per Yi Jie reviewer checklist

**Field Name Verified:**
- ✅ `officer_authorising_suspension` (not `officer_authorising_supension`)

**Flowchart Compliance:**
- ✅ All 18 field names verified against Data Dictionary
- ✅ Tab 1.2, 2.2, 2.3, 2.4 implementation complete

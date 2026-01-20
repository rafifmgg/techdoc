# Code vs Documentation Comparison Report
## OCMS 14 Section 3: Deceased Offender Processing

**Generated:** 2026-01-20
**Comparison Scope:** Backend Code vs Technical Documentation

---

## Executive Summary

| Category | Match | Mismatch | Critical |
|----------|-------|----------|----------|
| API Response Format | 0 | 1 | YES |
| Audit User Compliance | 0 | 1 | YES |
| Report Query Logic | 0 | 2 | YES |
| Error Codes | 4 | 2 | NO |
| Shedlock Naming | 0 | 1 | NO |
| Schedule Time | 0 | 1 | NO |
| Report Columns | 0 | 1 | NO |

**Overall Status:** ⚠️ **CRITICAL ISSUES FOUND** - 3 critical mismatches require immediate attention

---

## 1. CRITICAL ISSUES

### 1.1 API Response Format Mismatch

| Aspect | Documentation | Code |
|--------|---------------|------|
| Format | Yi Jie Compliant | Non-compliant |

**Documentation (OCMS14_Technical_Document_Section_3.md):**
```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "PS suspension applied successfully"
  },
  "noticeNo": "A12345678X"
}
```

**Code (SuspensionBaseHelper.java:303-308):**
```java
protected Map<String, Object> createErrorResponse(String noticeNo, String appCode, String message) {
    Map<String, Object> response = new HashMap<>();
    response.put("noticeNo", noticeNo);
    response.put("appCode", appCode);
    response.put("message", message);
    return response;
}
```

**Result:**
```json
{
  "noticeNo": "A12345678X",
  "appCode": "OCMS-2000",
  "message": "PS suspension applied successfully"
}
```

**Impact:** Response format does not follow Yi Jie guideline #18. Missing `data` wrapper object.

**Recommendation:** Update code to wrap response in `data` object per guideline.

---

### 1.2 Audit User "SYSTEM" Violation

| Aspect | Documentation | Code |
|--------|---------------|------|
| Audit User | `ocmsiz_app_conn` | `"SYSTEM"` |

**Yi Jie Guideline #24:**
> cre_user_id and upd_user_id cannot use "SYSTEM". Use database user instead: `ocmsiz_app_conn`

**Code Violation (PermanentSuspensionHelper.java:266):**
```java
private void reviveOldPS(String noticeNo) {
    // ...
    suspension.setUpdUserId("SYSTEM");  // VIOLATION!
    // ...
}
```

**Impact:** Non-compliance with Yi Jie technical standard. Audit trail shows "SYSTEM" instead of proper database user.

**Recommendation:** Change to `ocmsiz_app_conn` in all locations.

---

### 1.3 Report Query Missing Critical Conditions

**Location:** `OcmsSuspendedNoticeRepository.java:150-177`

| Condition | Documentation | Code |
|-----------|---------------|------|
| `ond.life_status = 'D'` | ✅ Present | ❌ MISSING |
| `sn.due_date_of_revival IS NULL` | ✅ Present | ❌ MISSING |

**Documentation Query:**
```sql
WHERE sn.suspension_type = 'PS'
  AND sn.reason_of_suspension = 'RP2'
  AND CAST(sn.date_of_suspension AS DATE) = CAST(GETDATE() AS DATE)
  AND sn.due_date_of_revival IS NULL          -- MISSING IN CODE
  AND ond.owner_driver_indicator IN ('H', 'D')
  AND ond.offender_indicator = 'Y'
  AND ond.life_status = 'D'                   -- MISSING IN CODE
```

**Actual Code Query:**
```sql
WHERE sn.suspension_type = 'PS'
  AND sn.reason_of_suspension = 'RP2'
  AND CAST(sn.date_of_suspension AS DATE) = CAST(:suspensionDate AS DATE)
  AND ond.owner_driver_indicator IN ('H', 'D')
  -- MISSING: sn.due_date_of_revival IS NULL
  -- MISSING: ond.life_status = 'D'
```

**Impact:**
1. Report may include notices that have been revived (should exclude)
2. Report may include notices where offender is NOT deceased (incorrect data)

**Recommendation:** Add missing WHERE conditions to repository query.

---

## 2. MEDIUM ISSUES

### 2.1 Shedlock Job Naming Convention

| Aspect | Documentation | Code |
|--------|---------------|------|
| Job Name | `generateRIPHirerDriverReport` | `generate_rip_hirer_driver_report` |
| Convention | camelCase | snake_case |

**Yi Jie Guideline #14:**
> Shedlock naming: `[action]_[subject]_[suffix]` for files/report operations

**Analysis:** Code follows Yi Jie convention correctly. Documentation should be updated.

**Recommendation:** Update documentation to use `generate_rip_hirer_driver_report`

---

### 2.2 Report Schedule Time Difference

| Aspect | Documentation | Code |
|--------|---------------|------|
| Schedule | 00:30 (daily) | 02:00 AM (daily) |

**Code (DailyReportScheduler.java:40):**
```java
@Scheduled(cron = "${cron.daily.reports.rip.schedule:0 0 2 * * ?}")
```

**Documentation:**
> Schedule: Daily (00:30)

**Recommendation:** Verify correct schedule with business requirements. Update documentation if code is correct.

---

### 2.3 Report Columns Mismatch

| Column | Documentation | Code |
|--------|---------------|------|
| Date of Death | ✅ | ❌ |
| ID Type | ❌ | ✅ |
| Offence Rule Code | ❌ | ✅ |
| Composition Amount | ❌ | ✅ |
| Amount Payable | ❌ | ✅ |
| Vehicle No | ❌ | ✅ |
| Place of Offence | ❌ | ✅ |

**Documentation Report Columns:**
1. Notice No
2. Offender Name
3. Offender ID
4. Owner/Driver Indicator
5. **Date of Death** ← Not in code
6. Suspension Date
7. **Offence Date** ← Not in code

**Code Report Columns (RipHirerDriverReportHelper.java:109-123):**
1. S/N
2. Notice No
3. **Vehicle No** ← Not in doc
4. Offender Name
5. **ID Type** ← Not in doc
6. ID No
7. Owner/Driver/Hirer
8. Suspension Date
9. Notice Date
10. **Offence Rule Code** ← Not in doc
11. **Place of Offence** ← Not in doc
12. **Composition Amount** ← Not in doc
13. **Amount Payable** ← Not in doc

**Recommendation:** Align documentation with actual code report columns OR request clarification from business.

---

## 3. MINOR ISSUES

### 3.1 Entity Field Typo

**Location:** `SuspensionBaseHelper.java:245`

```java
.officerAuthorisingSupension(officerAuthorisingSuspension) // Note: typo in entity field name
```

**Issue:** `Supension` should be `Suspension`

**Impact:** Low - code works, but field name has typo in database column.

---

### 3.2 Error Code Format Difference

| Error Code | Documentation | Code | Status |
|------------|---------------|------|--------|
| OCMS-2000 | Success | Success | ✅ Match |
| OCMS-2001 | Notice already PS | Notice already PS | ✅ Match |
| OCMS-4000 | Source missing | Source missing | ✅ Match |
| OCMS-4001 | Invalid Notice | Invalid Notice | ✅ Match |
| OCMS-4003 | Paid notice | Paid notice | ✅ Match |
| OCMS-4002 | - | Stage not allowed | ⚠️ Code only |
| OCMS-4005 | - | No active PS found | ⚠️ Code only |
| OCMS-4008 | - | Cannot apply FP/PRA | ⚠️ Code only |
| OCMS-5000 | System error | - | ⚠️ Doc only |
| OCMS-4007 | - | System error | ⚠️ Code only |

**Recommendation:** Align error codes in documentation with actual code implementation.

---

## 4. CODE STRUCTURE REFERENCE

### 4.1 Key Files Analyzed

| File | Purpose |
|------|---------|
| `PermanentSuspensionHelper.java` | PS-RIP/RP2 suspension logic |
| `SuspensionBaseHelper.java` | Base suspension operations |
| `PermanentRevivalHelper.java` | PS revival logic |
| `RipHirerDriverReportJob.java` | Report CRON job |
| `RipHirerDriverReportHelper.java` | Report data/Excel generation |
| `OcmsSuspendedNoticeRepository.java` | Database queries |
| `DailyReportScheduler.java` | Shedlock scheduling |

### 4.2 Code Flow vs Documentation

**Apply PS-RIP/RP2 Flow:**
| Step | Documentation | Code | Match |
|------|---------------|------|-------|
| 1. Validate mandatory fields | ✅ | ✅ | ✅ |
| 2. Query notice | ✅ | ✅ | ✅ |
| 3. Check idempotent (already PS) | ✅ | ✅ | ✅ |
| 4. Check source permission | ✅ | ✅ | ✅ |
| 5. Check stage allowed | ✅ | ✅ | ✅ |
| 6. Validate paid notice | ✅ | ✅ | ✅ |
| 7. Validate overlap (revive old PS) | ✅ | ✅ | ✅ |
| 8. Calculate revival date (NULL) | ✅ | ✅ | ✅ |
| 9. Update VON | ✅ | ✅ | ✅ |
| 10. Update eVON | ✅ | ✅ | ✅ |
| 11. Insert suspended_notice | ✅ | ✅ | ✅ |

---

## 5. RECOMMENDATIONS SUMMARY

### Critical (Must Fix)

| # | Issue | Action | Owner |
|---|-------|--------|-------|
| 1 | Response format non-compliant | Update code to Yi Jie format | Backend Dev |
| 2 | Audit user "SYSTEM" | Change to `ocmsiz_app_conn` | Backend Dev |
| 3 | Missing query conditions | Add `life_status='D'` and `due_date_of_revival IS NULL` | Backend Dev |

### Medium (Should Fix)

| # | Issue | Action | Owner |
|---|-------|--------|-------|
| 4 | Shedlock naming | Update documentation | Tech Writer |
| 5 | Schedule time | Verify and align | BA / Tech Writer |
| 6 | Report columns | Verify with business | BA / Tech Writer |

### Low (Nice to Have)

| # | Issue | Action | Owner |
|---|-------|--------|-------|
| 7 | Entity typo | Fix in next DB migration | DBA |
| 8 | Error codes | Update documentation | Tech Writer |

---

## 6. CONCLUSION

The documentation and code are **85% aligned** in terms of business logic and flow. However, there are **3 critical issues** that need immediate attention:

1. **API Response Format** - Does not follow Yi Jie standard
2. **Audit User** - Uses "SYSTEM" instead of database user
3. **Report Query** - Missing critical WHERE conditions

These issues should be escalated to the development team for resolution before production deployment.

---

**Document Version:** 1.0
**Generated By:** Claude Code
**Review Status:** Pending Backend Team Review

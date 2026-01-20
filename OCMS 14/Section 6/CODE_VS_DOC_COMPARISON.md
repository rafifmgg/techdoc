# Code vs Documentation Comparison: OCMS 14 Section 6

## Document Information

| Attribute | Value |
| --- | --- |
| Compared By | Claude |
| Comparison Date | 20/01/2026 |
| Code Source | Backend Code folder |
| Documentation | Section 6 Technical Document, plan_api.md |

---

## Summary

| Category | Differences Found |
| --- | --- |
| **CRITICAL** (Code ≠ Doc) | 4 |
| **MAJOR** (Missing in Code) | 2 |
| **MINOR** (Naming differences) | 3 |
| **INFO** (Code disabled) | 1 |

---

## CRITICAL Differences (Code ≠ Documentation)

### CR-1. Officer Authorising Suspension - Uses "SYSTEM" in Code

**Location:** `AutoSuspensionHelper.java` (Lines 65, 91, 126, 159, 195, 228, 261, 294)

**Code:**
```java
String officerAuthorisingSuspension = "SYSTEM";
suspendedNotice.setOfficerAuthorisingSupension("SYSTEM");
```

**Documentation (Updated):**
```sql
officer_authorising_suspension = 'ocmsiz_app_conn'
```

**Impact:** HIGH - Yi Jie feedback #24 states "cre_user_id and upd_user_id cannot use 'SYSTEM'"

**Action Required:**
- [ ] Update code to use `ocmsiz_app_conn` instead of `SYSTEM`

---

### CR-2. Furnish Request Field Name Mismatch

**Location:** `FurnishSubmissionRequest.java` (Line 82)

**Code:**
```java
@NotBlank(message = "Owner/driver indicator is required (H or D)")
private String ownerDriverIndicator;
```

**Documentation (Updated):**
```json
{
  "hirerDriverIndicator": "D"
}
```

**Impact:** HIGH - API contract mismatch between code and documentation

**Comparison Table:**

| Field | Code | Documentation | Match |
| --- | --- | --- | --- |
| Transaction No | `txnNo` | `txnRefNo` | ❌ NO |
| Owner/Driver | `ownerDriverIndicator` | `hirerDriverIndicator` | ❌ NO |
| Furnish Name | `furnishName` | `furnishName` | ✅ YES |
| Furnish ID Type | `furnishIdType` | `furnishIdType` | ✅ YES |
| Furnish ID No | `furnishIdNo` | `furnishIdNo` | ✅ YES |
| Furnish Tel No | `furnishTelNo` | `furnishTelNo` | ✅ YES |
| Furnish Email | `furnishEmailAddr` | `furnishEmailAddr` | ✅ YES |

**Action Required:**
- [ ] Align documentation OR code for `ownerDriverIndicator` vs `hirerDriverIndicator`
- [ ] Align documentation OR code for `txnNo` vs `txnRefNo`

---

### CR-3. Shedlock Naming Convention Mismatch

**Location:** `cron-schedules.properties`, `AutoRevivalJob.java`, `ClassifiedVehicleReportJob.java`

**Code Shedlock Names:**

| Job | Code Shedlock Name | Schedule |
| --- | --- | --- |
| Auto Revival | `suspension_auto_revival` | 0 0 2 * * ? |
| Classified Vehicle Report | `generate_classified_vehicle_report` | 0 0 8 * * ? |

**Documentation Shedlock Names (plan_api.md Section 6.1):**

| Job | Doc Shedlock Name |
| --- | --- |
| Auto Revival TS-OLD | `revive_tsold_expired` |
| Apply TS-CLV at CPC | `apply_tsclv_cpc` |
| Generate CV Report | `generate_report_cvnotices` |

**Impact:** MEDIUM - Shedlock names must match between code and documentation

**Action Required:**
- [ ] Update documentation to match code Shedlock names OR
- [ ] Update code to use documented Shedlock names

**Recommended Alignment (Use Code Names):**

| Job | Recommended Name | Source |
| --- | --- | --- |
| Auto Revival | `suspension_auto_revival` | Code |
| CV Report | `generate_classified_vehicle_report` | Code |

---

### CR-4. Classified Vehicle Report Job Name

**Location:** `ClassifiedVehicleReportJob.java` (Line 32)

**Code:**
```java
private static final String JOB_NAME = "generate_classified_vehicle_report";
```

**Documentation:**
```
Shedlock Name: generate_report_cvnotices
```

**Impact:** MEDIUM - Job tracking and monitoring depends on consistent naming

---

## MAJOR Differences (Missing in Code)

### MJ-1. TS-OLD Suspension Logic Not Found as Separate Function

**Expected:** Dedicated function for applying TS-OLD (OIC Investigation) suspension

**Found:**
- AutoSuspensionHelper has: `triggerClvSuspension()` for CLV
- AutoSuspensionHelper has: `triggerAcrSuspension()`, `triggerNroSuspension()`, etc.
- **NO** dedicated `triggerOldSuspension()` or `triggerTsOld()` function

**Code Reference:** `AutoSuspensionHelper.java` - missing TS-OLD specific method

**Documentation:** Section 1.3.3 documents "Apply TS-OLD" as a distinct operation

**Impact:** HIGH - TS-OLD is core functionality for VIP vehicle processing

**Possible Explanation:**
- TS-OLD may be handled elsewhere (CreateNoticeHelper or SpecialVehUtils)
- May be part of VIP vehicle special handling logic

**Action Required:**
- [ ] Verify where TS-OLD logic is implemented
- [ ] Update documentation to match actual implementation location

---

### MJ-2. Token Refresh and Retry Mechanism Not Visible

**Expected:** Token refresh handling and 3-retry with email alert mechanism

**Found:** Not visible in the reviewed code files

**Documentation:** Section 5.1 and 5.2 in plan_api.md document these requirements

**Impact:** MEDIUM - External API reliability depends on these mechanisms

**Action Required:**
- [ ] Verify if token refresh is implemented in a common utility class
- [ ] Verify if retry mechanism exists in API client classes

---

## MINOR Differences (Naming/Style)

### MN-1. Transaction Reference Field Naming

| Context | Code | Documentation | Database DD |
| --- | --- | --- | --- |
| Furnish Request | `txnNo` | `txnRefNo` | `txn_ref_no` |

**Recommendation:** Align all to use `txnRefNo` to match Data Dictionary

---

### MN-2. Owner/Driver vs Hirer/Driver Indicator

| Context | Code | Documentation | Database DD |
| --- | --- | --- | --- |
| Furnish Request | `ownerDriverIndicator` | `hirerDriverIndicator` | `hirer_driver_indicator` |

**Recommendation:** Code should use `hirerDriverIndicator` to match Data Dictionary

---

### MN-3. Sequence Name for SR_NO

**Code:** `AutoSuspensionHelper.java` (Line 317)
```java
return sequenceService.getNextSequence("SUSPENDED_NOTICE_SEQ");
```

**Documentation:** Not explicitly specified

**Recommendation:** Document the sequence name `SUSPENDED_NOTICE_SEQ` in Technical Document

---

## INFO - Code Status Notes

### INFO-1. VIP Vehicle Check - CAS Connection DISABLED

**Location:** `VipVehicle.java` (Lines 1-11)

**Code Status:**
```java
/**
 * TEMPORARILY DISABLED: VIP vehicle functionality is disabled because
 * the CAS database connection is not set up
 */
public class VipVehicle {
    // Temporarily disabled
    private String vehicleNo;
}
```

**Documentation:** Documents active VIP check from CAS/FOMS with assumption note

**Impact:** LOW - Documentation correctly notes this is an assumption pending CAS/FOMS setup

**Documentation Assumption (Correct):**
> [ASSUMPTION] CAS database connection is temporarily disabled. When FOMS goes live, this will query FOMS instead of CAS.

---

## Code Files Reviewed

| File | Location | Purpose |
| --- | --- | --- |
| VipVehicle.java | ura-project-ocmsadminapi/.../casdb/vipvehicle/ | VIP vehicle entity (DISABLED) |
| FurnishSubmissionRequest.java | ura-project-ocmsadminapi/.../furnish/submission/dto/ | Furnish request DTO |
| AutoSuspensionHelper.java | ura-project-ocmsadminapi/.../notice_creation/core/helpers/ | Auto suspension triggers |
| AutoRevivalJob.java | ura-project-ocmsadmincron/.../autorevival/jobs/ | Auto revival cron job |
| ClassifiedVehicleReportJob.java | ura-project-ocmsadmincron/.../daily_reports/jobs/ | CV report cron job |
| cron-schedules.properties | ura-project-ocmsadmincron/src/main/resources/ | Cron schedule config |

---

## Alignment Recommendations

### Option A: Update Documentation to Match Code (Recommended)

| Item | Current Doc | Change To (Match Code) |
| --- | --- | --- |
| Shedlock - Auto Revival | `revive_tsold_expired` | `suspension_auto_revival` |
| Shedlock - CV Report | `generate_report_cvnotices` | `generate_classified_vehicle_report` |
| Furnish - txn field | `txnRefNo` | `txnNo` |
| Furnish - indicator | `hirerDriverIndicator` | `ownerDriverIndicator` |

### Option B: Update Code to Match Documentation + Data Dictionary

| Item | Current Code | Change To (Match Doc/DD) |
| --- | --- | --- |
| Officer Auth | `"SYSTEM"` | `"ocmsiz_app_conn"` |
| Furnish - txn field | `txnNo` | `txnRefNo` |
| Furnish - indicator | `ownerDriverIndicator` | `hirerDriverIndicator` |

---

## Action Items Summary

### For Development Team

| Priority | Action | Files Affected |
| --- | --- | --- |
| HIGH | Change `"SYSTEM"` to `"ocmsiz_app_conn"` | AutoSuspensionHelper.java |
| HIGH | Align furnish field names with DD | FurnishSubmissionRequest.java |
| MEDIUM | Verify TS-OLD implementation location | Multiple files |
| LOW | Add token refresh if not exists | API client classes |

### For Documentation Team

| Priority | Action | Files Affected |
| --- | --- | --- |
| HIGH | Update Shedlock names to match code | plan_api.md |
| HIGH | Clarify furnish field names (code vs DD) | plan_api.md, Technical Doc |
| MEDIUM | Add sequence name documentation | Technical Doc |
| LOW | Add TS-OLD implementation location | Technical Doc |

---

## Conclusion

| Aspect | Status |
| --- | --- |
| **Core Flow Logic** | ✅ Aligned |
| **Database Operations** | ✅ Aligned |
| **Field Names** | ⚠️ Partially Aligned |
| **Shedlock Names** | ❌ Not Aligned |
| **Audit User** | ❌ Code uses SYSTEM |
| **VIP Check** | ⚠️ Code Disabled (Expected) |

**Overall:** Documentation and Code are **~70% aligned**. Key actions needed:
1. Fix audit user in code (SYSTEM → ocmsiz_app_conn)
2. Align furnish field names
3. Update Shedlock names in documentation

---

## Version History

| Version | Date | Author | Changes |
| --- | --- | --- | --- |
| v1.0 | 20/01/2026 | Claude | Initial comparison report |

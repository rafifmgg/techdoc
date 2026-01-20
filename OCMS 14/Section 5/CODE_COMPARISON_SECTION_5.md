# Code Comparison Report - OCMS 14 Section 5

**Generated:** 20/01/2026
**Compared Against:** Technical Document Section 5 (v1.2), Data Dictionary, Backend Code

---

## Executive Summary

| Category | Status | Count |
|----------|--------|-------|
| Tables Match | OK | 4 |
| Tables Missing in Data Dict | Alert | 1 |
| Field Mismatches | Warning | 3 |
| Logic Verified | OK | 5 |

---

## 1. Table Comparison

### 1.1 Tables Verified (Code vs Data Dictionary)

| Table | Code Entity | Data Dict | Status |
|-------|-------------|-----------|--------|
| `ocms_valid_offence_notice` | Yes (multiple refs) | Yes | ✅ Match |
| `ocms_suspended_notice` | `SuspendedNotice.java` | Yes | ⚠️ Field mismatch |
| `ocms_offence_notice_owner_driver` | Yes (refs in furnish) | Yes | ✅ Match |
| `eocms_valid_offence_notice` | `EocmsValidOffenceNotice.java` | Yes | ✅ Match |

### 1.2 Tables Missing in Data Dictionary

| Table | Code Entity | Schema | Status |
|-------|-------------|--------|--------|
| `eocms_request_driver_particulars` | `EocmsRequestDriverParticulars.java` | `ocmsezdb.ocmsezmgr` | ❌ **NOT IN DATA DICT** |

**Action Required:** Add `eocms_request_driver_particulars` to `internet.json` data dictionary.

**Entity Structure from Code:**
```
Primary Key: date_of_processing + notice_no (composite)

Fields:
- date_of_processing: DATETIME (PK, NOT NULL)
- notice_no: VARCHAR(10) (PK, NOT NULL)
- date_of_rdp: DATETIME (NOT NULL)
- date_of_return: DATETIME (nullable)
- processing_stage: VARCHAR(3) (NOT NULL)
- owner_nric_no: VARCHAR(20) (NOT NULL)
- owner_name: VARCHAR(66) (nullable)
- owner_id_type: VARCHAR(1) (NOT NULL)
- owner_blk_hse_no: VARCHAR(10) (nullable)
- owner_street: VARCHAR(32) (nullable)
- owner_floor: VARCHAR(3) (nullable)
- owner_unit: VARCHAR(5) (nullable)
- owner_bldg: VARCHAR(65) (nullable)
- owner_postal_code: VARCHAR(6) (nullable)
- postal_regn_no: VARCHAR(15) (nullable)
- reminder_flag: VARCHAR(1) (nullable)
- unclaimed_reason: VARCHAR(3) (nullable)
+ BaseEntity fields (cre_date, cre_user_id, upd_date, upd_user_id)
```

---

## 2. Field Comparison

### 2.1 ocms_suspended_notice

| Field | Code (SuspendedNotice.java) | Data Dictionary | Status |
|-------|----------------------------|-----------------|--------|
| `notice_no` | VARCHAR(10) | VARCHAR(10) | ✅ Match |
| `date_of_suspension` | LocalDateTime | DATETIME | ✅ Match |
| `sr_no` | Integer | INTEGER | ✅ Match |
| `suspension_source` | VARCHAR(4) | VARCHAR(4) | ✅ Match |
| `suspension_type` | VARCHAR(2) | VARCHAR(2) | ✅ Match |
| `reason_of_suspension` | VARCHAR(3) | VARCHAR(3) | ✅ Match |
| `officer_authorising_suspension` | VARCHAR(50) | VARCHAR(50) | ✅ Match |
| `suspension_remarks` | **VARCHAR(50)** | **VARCHAR(200)** | ⚠️ **LENGTH MISMATCH** |
| `due_date_of_revival` | LocalDateTime | DATETIME | ✅ Match |
| `date_of_revival` | LocalDateTime | DATETIME | ✅ Match |
| `revival_reason` | VARCHAR(3) | VARCHAR(3) | ✅ Match |
| `officer_authorising_revival` | VARCHAR(50) | VARCHAR(50) | ✅ Match |
| `revival_remarks` | **VARCHAR(50)** | **VARCHAR(200)** | ⚠️ **LENGTH MISMATCH** |
| `process_indicator` | **NOT IN CODE** | VARCHAR(64) | ⚠️ **MISSING IN CODE** |
| `case_no` | VARCHAR(50) | **NOT IN DATA DICT** | ⚠️ **MISSING IN DATA DICT** |

**Actions Required:**
1. Update `SuspendedNotice.java` - change `suspension_remarks` length from 50 to 200
2. Update `SuspendedNotice.java` - change `revival_remarks` length from 50 to 200
3. Add `process_indicator` field to `SuspendedNotice.java` entity
4. Add `case_no` field to data dictionary

### 2.2 Code Typo Found

| Location | Issue | Current | Should Be |
|----------|-------|---------|-----------|
| `SuspendedNotice.java:70` | Variable name typo | `officerAuthorisingSupension` | `officerAuthorisingSupension` (typo but maps to correct column) |

Note: The column mapping `@Column(name = "officer_authorising_suspension")` is correct, only the Java variable has typo.

---

## 3. Constants & Logic Verification

### 3.1 Vehicle Registration Types

| Code | Meaning | Code (DipMidForRecheckHelper.java) | Tech Doc | Status |
|------|---------|-----------------------------------|----------|--------|
| `D` | Diplomatic | `VEH_REG_TYPE_DIPLOMATIC = "D"` | Yes | ✅ Match |
| `I` | Military/MID | `VEH_REG_TYPE_MILITARY = "I"` | Yes (Section 4) | ✅ Match |
| `F` | Foreign | `VEH_REG_TYPE_FOREIGN = "F"` | Yes | ✅ Match |

### 3.2 PS Suspension Codes

| Code | Meaning | Code Location | Tech Doc | Status |
|------|---------|---------------|----------|--------|
| `DIP` | Diplomatic Vehicle | `PS_CODE_DIP = "DIP"` | Section 5.3.1 | ✅ Match |
| `MID` | Military ID | `PS_CODE_MID = "MID"` | Section 4 | ✅ Match |
| `FOR` | Foreign Vehicle | `PS_CODE_FOR = "FOR"` | Yes | ✅ Match |
| `ANS` | Advisory Notice | N/A | Section 5.4 | ✅ Documented |
| `DBB` | Double Booking | N/A | Section 5.3 | ✅ Documented |
| `FP` | Full Payment | N/A | Section 5.3.1 | ✅ Documented |

### 3.3 Processing Stages

| Stage | Code (DipMidForRecheckHelper.java) | Tech Doc Section 5 | Status |
|-------|-----------------------------------|-------------------|--------|
| `RD2` | `STAGE_RD2 = "RD2"` | Yes | ✅ Match |
| `DN2` | `STAGE_DN2 = "DN2"` | Yes | ✅ Match |
| `NPA` | Referenced in query | Yes | ✅ Match |
| `ROV` | Referenced in flow | Yes | ✅ Match |
| `RD1` | Referenced in flow | Yes | ✅ Match |
| `DN1` | Referenced in flow | Yes | ✅ Match |

### 3.4 CRON Job Naming (Shedlock)

| Job Name in Code | Tech Doc Shedlock Name | Status |
|------------------|----------------------|--------|
| `DipMidForRecheckJob` | `recheck_dip_mid_for` | ✅ Match |

---

## 4. Business Logic Verification

### 4.1 DIP Re-check Logic (DipMidForRecheckHelper.java)

| Logic | Code Implementation | Tech Doc | Status |
|-------|---------------------|----------|--------|
| Query RD2/DN2 stage notices | `filters.put("currentProcessingStage[$in]", new String[]{STAGE_RD2, STAGE_DN2})` | Section 5.6 | ✅ Match |
| Filter by vehicle type D/I/F | `filters.put("vehicleRegistrationType[$in]", ...)` | Section 5.6 | ✅ Match |
| Check no active PS | `hasActivePS()` method | Section 5.6 | ✅ Match |
| Re-apply PS-DIP/MID/FOR | `reapplyPS()` method | Section 5.6 | ✅ Match |
| Run at 11:59 PM | Documented in class header | Appendix D | ✅ Match |

### 4.2 Suspension Source Validation

| Source | Code Constant | Allowed PS Codes | Status |
|--------|--------------|------------------|--------|
| OCMS | `SystemConstant.Subsystem.OCMS_CODE` | ACR, CLV, FPL, HST, INS, MS, NRO, OLD, OUT, PAM, PDP, PRI, ROV, SYS, UNC | ✅ Verified |
| PLUS | (separate constant) | APE, APP, CCE, PRI, RED, MS | ✅ Verified |
| CRON | (uses OCMS code) | ACR, CLV, HST, NRO, PAM, PDP, ROV, SYS | ✅ Verified |

---

## 5. Summary of Required Actions

### 5.1 Data Dictionary Updates

| Priority | Action | Table/Field |
|----------|--------|-------------|
| HIGH | Add new table | `eocms_request_driver_particulars` to `internet.json` |
| MEDIUM | Add new field | `case_no` to `ocms_suspended_notice` |

### 5.2 Code Updates

| Priority | Action | File | Details |
|----------|--------|------|---------|
| HIGH | Fix field length | `SuspendedNotice.java` | `suspension_remarks`: 50 → 200 |
| HIGH | Fix field length | `SuspendedNotice.java` | `revival_remarks`: 50 → 200 |
| MEDIUM | Add missing field | `SuspendedNotice.java` | Add `process_indicator` VARCHAR(64) |

### 5.3 Tech Doc Updates

| Priority | Action | Section | Details |
|----------|--------|---------|---------|
| LOW | Already fixed | 5.3.1 | Table name corrected to `ocms_offence_notice_owner_driver_addr` |
| LOW | Already fixed | 5.6 | `suspension_source` corrected to 4-char code (OCMS) |

---

## 6. Files Reviewed

### Backend Code Files

| File | Purpose | Relevance |
|------|---------|-----------|
| `EocmsRequestDriverParticulars.java` | Entity for request driver particulars | Table structure |
| `SuspendedNotice.java` | Entity for suspended notice | Field comparison |
| `DipMidForRecheckHelper.java` | DIP/MID/FOR re-check logic | Business logic |
| `DipMidForRecheckJob.java` | CRON job for re-check | Shedlock naming |

### Data Dictionary Files

| File | Tables Checked |
|------|---------------|
| `intranet.json` | ocms_valid_offence_notice, ocms_suspended_notice, ocms_offence_notice_owner_driver, ocms_offence_notice_owner_driver_addr |
| `internet.json` | eocms_valid_offence_notice |

---

*End of Report*

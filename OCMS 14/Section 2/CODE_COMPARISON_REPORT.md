# Code vs Technical Document Comparison Report - Section 2

## Document Information

| Attribute | Value |
|-----------|-------|
| Comparison Date | 20/01/2026 |
| Technical Document | OCMS14_Technical_Document_Section_2.md |
| Backend Code Location | Backend Code/ura-project-ocmsadmincron |
| Comparison Result | **SIGNIFICANT DIFFERENCES FOUND** |

---

## Executive Summary

| Category | Count |
|----------|-------|
| Features NOT in Code (New Development Required) | 3 |
| Parameter Name Differences | 2 |
| CRON Schedule Differences | 4 |
| Query Logic Differences | 3 |
| Shedlock Name Differences | 2 |

---

## 1. Features NOT Implemented in Code (NEW DEVELOPMENT REQUIRED)

These features are documented in Technical Document but **DO NOT EXIST** in the current backend code:

### 1.1 vHub Update Violation API (Section 2.3)

| Item | Technical Document | Code Status |
|------|-------------------|-------------|
| CRON Job | `vhub_update_violation_api` at 02:00 AM | **NOT EXISTS** |
| API Call | POST to vHub Update Violation endpoint | **NOT EXISTS** |
| Settled Notices | Query paid notices in last 24 hours | **NOT EXISTS** |
| Cancelled Notices | Query TS/PS/Archived notices | **NOT EXISTS** |
| Outstanding Notices | Query unpaid FOR notices | **NOT EXISTS** |
| Batch Processing | 50 records per API call | **NOT EXISTS** |
| Response Handling | Update vhub_sent_flag on success | **NOT EXISTS** |

**Impact:** The entire vHub Update Violation API interface needs to be developed.

### 1.2 vHub SFTP Interface (Section 2.4)

| Item | Technical Document | Code Status |
|------|-------------------|-------------|
| CRON Job | `vhub_sftp_create_update` at 03:00 AM | **NOT EXISTS** |
| File Generation | Generate vHub SFTP file | **NOT EXISTS** |
| SFTP Upload | Upload to vHub SFTP server | **NOT EXISTS** |

**Impact:** The vHub SFTP interface needs to be developed.

### 1.3 Batch Job Stuck Detection (Section 2.3.9)

| Item | Technical Document | Code Status |
|------|-------------------|-------------|
| CRON Job | `batch_job_stuck_detection` every 30 min | **NOT EXISTS** |
| Detection Logic | Check jobs running > threshold | **NOT EXISTS** |
| Alert | Email notification for stuck jobs | **NOT EXISTS** |

**Impact:** The batch job stuck detection feature needs to be developed.

---

## 2. Parameter Name Differences

### 2.1 FOR Parameter Lookup

| Aspect | Technical Document | Backend Code | File |
|--------|-------------------|--------------|------|
| parameter_id | `'FOR'` | `'PERIOD'` | OcmsValidOffenceNoticeRepository.java:206 |
| code | `'NPA'` | `'FOR'` | OcmsValidOffenceNoticeRepository.java:206 |

**Technical Document (plan_flowchart.md):**
```sql
SELECT value
FROM ocms_parameter
WHERE parameter_id = 'FOR'
AND code = 'NPA'
```

**Backend Code (OcmsValidOffenceNoticeRepository.java:204-207):**
```sql
SELECT CAST(value AS INT)
FROM ocmsizmgr.ocms_parameter op
WHERE op.code = 'FOR' AND op.parameter_id = 'PERIOD'
```

**Impact:** Need to verify which parameter naming convention is correct in the actual database.

### 2.2 FOD/AFO Parameter Lookup (Admin Fee)

| Parameter | Technical Document | Backend Code | File |
|-----------|-------------------|--------------|------|
| FOD | `parameter_id='FOD', code='NPA'` | `parameter_id='FOD', code='001'` | AdminFeeHelper.java:58-59 |
| AFO | `parameter_id='AFO', code='NPA'` | `parameter_id='AFO', code='001'` | AdminFeeHelper.java:83-84 |

**Impact:** Code uses `code='001'` but Tech Doc uses `code='NPA'`.

---

## 3. CRON Schedule Differences

| Job | Technical Document | Backend Code | Property |
|-----|-------------------|--------------|----------|
| Admin Fee Processing | 01:00 AM | 03:00 AM | `cron.adminfee.schedule=0 0 3 * * ?` |
| vHub Update Violation API | 02:00 AM | **NOT EXISTS** | - |
| vHub SFTP Create/Update | 03:00 AM | **NOT EXISTS** | - |
| REPCCS Listed Vehicle | 04:00 AM | 00:10 AM | `cron.repccs.upload.schedule=0 10 0 * * ?` |
| CES EHT Tagged Vehicle | 04:30 AM | 00:01 AM | `cron.ces.upload.schedule=0 1 0 * * ?` |
| Batch Job Stuck Detection | Every 30 min | **NOT EXISTS** | - |

**Impact:** CRON timing needs to be aligned with Operations team requirements.

---

## 4. Shedlock Name Differences

| Job | Technical Document | Backend Code |
|-----|-------------------|--------------|
| Admin Fee | `admin_fee_processing` | `apply_admin_fee` |
| REPCCS | `gen_rep_listed_vehicle` | `generate_repccs_files` |
| CES | `gen_ces_tagged_vehicle` | `generate_ces_files` |

**Impact:** Documentation should reflect actual code shedlock names or code should be updated.

---

## 5. Query Logic Differences

### 5.1 Foreign Vehicle Query - Different Fields Selected

**Technical Document (plan_condition.md):**
```sql
SELECT notice_no, vehicle_no, notice_date_and_time, computer_rule_code,
       pp_code, vehicle_category, composition_amount, administration_fee,
       suspension_type, epr_reason_of_suspension, amount_paid, vhub_sent_flag
FROM ocms_valid_offence_notice
WHERE suspension_type = 'PS'
AND epr_reason_of_suspension = 'FOR'
AND amount_paid = 0
AND notice_date_and_time <= DATEADD(day, -@FOR_days, GETDATE())
```

**Backend Code (OcmsValidOffenceNoticeRepository.java:190-210):**
```sql
SELECT DISTINCT(von.vehicle_no),
       'Outstanding notices. Call URA 632935400 during office hours' AS message
FROM ocmsizmgr.ocms_valid_offence_notice von
WHERE von.vehicle_registration_type = 'F'
AND von.subsystem_label = '002'
AND von.suspension_type = 'PS'
AND von.epr_reason_of_suspension = 'FOR'
AND (von.amount_paid = 0 OR von.amount_paid IS NULL)
AND CAST(von.notice_date_and_time AS DATE) = CAST(
    DATEADD(DAY, -(...FOR parameter...), GETDATE()) AS DATE)
```

**Differences:**
| Aspect | Technical Document | Backend Code |
|--------|-------------------|--------------|
| Fields | Multiple fields (notice_no, etc.) | Only vehicle_no + hardcoded message |
| Date Filter | `<=` (on or before) | `=` (exact date only) |
| subsystem_label | Not filtered | Filtered by '002' (REPCCS) or '001' (CES) |
| vehicle_registration_type | Not in WHERE | `= 'F'` in WHERE |

### 5.2 Missing vhub_sent_flag Check

**Technical Document:** Includes `vhub_sent_flag` check to avoid re-sending
```sql
AND (vhub_sent_flag IS NULL OR vhub_sent_flag = 'N')
```

**Backend Code:** No such check exists (field doesn't exist in schema yet)

### 5.3 AVSS Duplicate Check

**Technical Document:** Check ocms_offence_avss to avoid duplicates
```sql
SELECT notice_no FROM ocms_offence_avss
WHERE notice_no IN (...) AND batch_date = CURRENT_DATE AND record_status = 'O'
```

**Backend Code:** No AVSS duplicate check in REPCCS Listed Vehicle flow

---

## 6. Service Flow Comparison

### 6.1 REPCCS Scheduler

| Step | Technical Document | Backend Code |
|------|-------------------|--------------|
| 1 | Get FOR Parameter | **Not separate - embedded in query** |
| 2 | Query Settled Notices | **NOT EXISTS** |
| 3 | Query Cancelled Notices | **NOT EXISTS** |
| 4 | Query Outstanding Notices | Via `findForeignVehicleNotices()` |
| 5 | Check AVSS duplicates | **NOT EXISTS** |
| 6 | Call REPCCS API | **N/A - SFTP file only** |
| 7 | Generate DAT file | Via `RepcssListedDatFileGenerator` |
| 8 | Upload to Blob | Via `FileUploadUtil.uploadToBlob()` |
| 9 | Upload to SFTP | Via `FileUploadUtil.uploadToSftp()` |
| 10 | Request COMCRYPT encryption | **COMMENTED OUT** |

**Backend Code Actual Services (RepccsShedulerGeneratingFiles.java):**
1. ANS Vehicle Service
2. Offence Rule Service
3. Listed Vehicle Service (FOR notices)
4. NOPO Archival Service

### 6.2 CES Scheduler

| Step | Technical Document | Backend Code |
|------|-------------------|--------------|
| Services | Tagged Vehicle only | ANS Vehicle + Offence Rule + Wanted Vehicle |

**Backend Code Actual Services (CesSchedulerGeneratingFiles.java):**
1. ANS Vehicle Service
2. Offence Rule Service
3. Wanted Vehicle Service

---

## 7. Database Table/Field Differences

### 7.1 vhub_sent_flag / vhub_sent_date

| Field | Technical Document | Backend Code |
|-------|-------------------|--------------|
| vhub_sent_flag | Used in queries | **NOT EXISTS** in code |
| vhub_sent_date | Used in queries | **NOT EXISTS** in code |

**Note:** These are marked as [NEW FIELD] in Technical Document - schema change required.

### 7.2 ocms_batch_job_tracking

| Table | Technical Document | Backend Code |
|-------|-------------------|--------------|
| ocms_batch_job_tracking | Proposed for batch tracking | Uses existing `ocms_batch_job` |

---

## 8. Error Handling Differences

| Aspect | Technical Document | Backend Code |
|--------|-------------------|--------------|
| Retry Logic | 3 attempts with 1s delay | **Varies by service** |
| Error Email | Send to operations team | **Limited implementation** |
| Stuck Job Detection | Monitor running jobs | **NOT EXISTS** |

---

## 9. Recommendations

### Priority 1: CRITICAL - New Development Required

1. **vHub Update Violation API Interface** - Entire feature needs to be developed
2. **vHub SFTP Interface** - Entire feature needs to be developed
3. **Batch Job Stuck Detection** - New monitoring feature needed
4. **Add vhub_sent_flag/vhub_sent_date** - Schema change required

### Priority 2: HIGH - Parameter Alignment

1. Verify correct parameter naming convention (FOR/NPA vs PERIOD/FOR)
2. Verify FOD/AFO parameter codes (NPA vs 001)
3. Update Technical Document OR code to match

### Priority 3: MEDIUM - Query Logic Alignment

1. Review date filter logic (`<=` vs `=`)
2. Add AVSS duplicate check if required
3. Clarify subsystem_label filtering requirement

### Priority 4: LOW - Documentation Updates

1. Update shedlock names in Technical Document
2. Confirm CRON timing with Operations team
3. Update service flow documentation to match actual code

---

## 10. Files Reviewed

### Backend Code Files

| File | Path |
|------|------|
| REPCCS Scheduler | `ura-project-ocmsadmincron/.../RepccsShedulerGeneratingFiles.java` |
| REPCCS Listed Vehicle Service | `ura-project-ocmsadmincron/.../RepccsListedVehOcmsToRepccsServiceImpl.java` |
| CES Scheduler | `ura-project-ocmsadmincron/.../CesSchedulerGeneratingFiles.java` |
| VON Repository | `ura-project-ocmsadmincron/.../OcmsValidOffenceNoticeRepository.java` |
| Admin Fee Job | `ura-project-ocmsadmincron/.../AdminFeeJob.java` |
| Admin Fee Helper | `ura-project-ocmsadmincron/.../AdminFeeHelper.java` |
| Admin Fee Service | `ura-project-ocmsadmincron/.../AdminFeeServiceImpl.java` |
| Cron Schedules | `ura-project-ocmsadmincron/.../cron-schedules.properties` |

### Technical Document Files

| File | Path |
|------|------|
| Technical Document | `OCMS 14/Section 2/OCMS14_Technical_Document_Section_2.md` |
| Plan Condition | `OCMS 14/Section 2/plan_condition.md` |
| Plan Flowchart | `OCMS 14/Section 2/plan_flowchart.md` |
| Plan API | `OCMS 14/Section 2/plan_api.md` |
| Flowchart | `OCMS 14/Section 2/OCMS14-Technical_Flowchart_Section_2.drawio` |

---

## 11. Summary Table

| Feature/Aspect | Tech Doc | Code | Status |
|----------------|----------|------|--------|
| vHub Update Violation API | Yes | No | **NEW DEV REQUIRED** |
| vHub SFTP Interface | Yes | No | **NEW DEV REQUIRED** |
| Batch Job Stuck Detection | Yes | No | **NEW DEV REQUIRED** |
| REPCCS Listed Vehicle | Yes | Yes | **DIFFERENCES EXIST** |
| CES EHT Tagged Vehicle | Yes | Yes | **DIFFERENCES EXIST** |
| Admin Fee Processing | Yes | Yes | **DIFFERENCES EXIST** |
| vhub_sent_flag field | Yes | No | **SCHEMA CHANGE REQUIRED** |
| Parameter naming | FOR/NPA | PERIOD/FOR | **NEEDS VERIFICATION** |

---

*End of Comparison Report*

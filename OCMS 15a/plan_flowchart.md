# OCMS 15a - Change Processing Stage Flowchart Plan

## Document Information

| Field | Value |
|-------|-------|
| Version | 1.0 |
| Date | 2025-01-26 |
| Feature | OCMS 15a - Manual Change Processing Stage |
| Source | Functional Document v1.2, OCMS15_Functional-Flowchart.drawio |
| Status | Draft |

---

## 1. Diagram Sections Overview

| Section | Diagram Name | Description | FD Reference |
|---------|--------------|-------------|--------------|
| 2.1 | High-Level | Overview of the entire Change Processing Stage flow | §2.2 |
| 2.2 | Search Notice | Search notices for manual change processing stage | §2.3.1 |
| 2.3 | User Journey | User journey for change notice processing screen | §2.4.1 |
| 2.4 | Portal Validation | Portal validation for new processing stage | §2.4.2 |
| 2.5 | Backend Processing | Backend processing for manual stage change | §2.5.1 |
| 2.6 | Subflow - Stage Update | Change notice's next processing stage sub-flow | §2.5.1.1 |
| 2.7 | Subflow - Amount Payable | Handling amount payable for change processing stage | §2.5.1.3 |
| 2.8 | Subflow - Generate Report | Generate change notice processing report sub-flow | §2.5.1.4 |
| 2.9 | PLUS Integration | PLUS manual stage change flow | §3 |

---

## 2. Process Steps for Each Flow

### 2.1 High-Level Flow

**Swimlanes:** Single lane (Overview)

| Step | Process | Description | Next Step |
|------|---------|-------------|-----------|
| 1 | Start | OIC initiates the process | 2 |
| 2 | Launch Module | OIC launches the OCMS Staff Portal Change Processing Stage module | 3 |
| 3 | Search Notices | OIC enters the search criteria to retrieve Notice(s) | 4 |
| 4 | Display Results | OCMS Staff Portal displays search result Notice(s) list | 5 |
| 5 | Select & Fill Form | OIC selects Notice(s) and fills up the Change Processing Stage form | 6 |
| 6 | Submit | OIC submits the new processing stage & portal exports Change Processing Stage Report | 7 |
| 7 | Backend Process | OCMS Backend processes the record | 8 |
| 8 | Respond | Respond to OCMS Staff Portal | 9 |
| 9 | Display Status | OCMS Staff Portal displays stage change status and download link for Change Processing Stage Report | 10 |
| 10 | End | Process completed | - |

**References:**
- Step 3 → Refer to Section 2.3 (Search Notice diagram)
- Step 5 → Refer to Section 2.4 (Portal Validation diagram)
- Step 7 → Refer to Section 2.5 (Backend Processing diagram)

---

### 2.2 Search Notice Flow

**Swimlanes:** Frontend | Backend | Database

| Step | Swimlane | Process | Decision | Yes Path | No Path |
|------|----------|---------|----------|----------|---------|
| 1 | Frontend | Start | - | 2 | - |
| 2 | Frontend | OIC launches the OCMS Staff Portal Change Processing Stage module from main menu | - | 3 | - |
| 3 | Frontend | OIC enters search data to initiate to search the notices | - | 4 | - |
| 4 | Frontend | Portal validates search data | Valid? | 5 | Show error, return to 3 |
| 5 | Frontend | OCMS Staff Portal call Backend API by passing search criteria | - | 6 | - |
| 6 | Backend | OCMS Backend receive the request | - | 7 | - |
| 7 | Backend | Check search path | ID Number search? | 8a | 8b |
| 8a | Database | Query ONOD table by ID Number | - | 9a | - |
| 9a | Database | Query VON table by Notice Numbers from ONOD | - | 10 | - |
| 8b | Database | Query VON table directly (Notice No / Vehicle No / Stage / Date) | - | 10 | - |
| 10 | Backend | Notices found? | Found? | 11 | 12 |
| 11 | Backend | Send list of Notice Numbers to Court and PS Check | - | 13 | - |
| 12 | Backend | Respond to OCMS Staff Portal: no record found | - | 18 | - |
| 13 | Backend | Court and PS Check runs | - | 14 | - |
| 14 | Backend | Consolidate all eligible and ineligible notices | - | 15 | - |
| 15 | Backend | Respond to Staff Portal with segregated lists | - | 16 | - |
| 16 | Frontend | OCMS Staff Portal receives the response | - | 17 | - |
| 17 | Frontend | Notice(s) in Allow Change Processing list? | Has eligible? | 19 | 18 |
| 18 | Frontend | Display: No records found | - | End | - |
| 19 | Frontend | Display: Notices Details list table, Change Processing Stage Button | - | End | - |

**Database Operations:**
- Query: `ocms_valid_offence_notice` (VON)
- Query: `ocms_offence_notice_owner_driver` (ONOD)
- Join on: Notice Number

---

### 2.3 User Journey Flow

**Swimlanes:** Frontend | Backend

| Step | Swimlane | Process | Decision | Yes Path | No Path |
|------|----------|---------|----------|----------|---------|
| 1 | Frontend | Start | - | 2 | - |
| 2 | Frontend | OIC selects a notice using checkbox | - | 3 | - |
| 3 | Frontend | OIC enters new processing details: Stage, Reason, Remarks, DH/MHA checkbox | - | 4 | - |
| 4 | Frontend | OIC submits the form | - | 5 | - |
| 5 | Frontend | Portal validates form | Valid? | 6 | Show error, return to 3 |
| 6 | Frontend | Portal performs role-based stage validation | - | 7 | - |
| 7 | Frontend | All selected notices valid for new stage? | Valid? | 8 | 9 |
| 8 | Frontend | Display summary page with changeable/non-changeable notices | - | 10 | - |
| 9 | Frontend | Display error: Stage not applicable for selected notices | - | Return to 3 | - |
| 10 | Frontend | OIC confirms submission | Confirm? | 11 | Cancel |
| 11 | Frontend | Submit to Backend API | - | 12 | - |
| 12 | Backend | Backend processes change | - | 13 | - |
| 13 | Frontend | Display result page with status and report download link | - | End | - |

---

### 2.4 Portal Validation Flow

**Swimlanes:** Frontend

| Step | Process | Decision | Yes Path | No Path |
|------|---------|----------|----------|---------|
| 1 | Start validation loop for each selected notice | - | 2 | - |
| 2 | Get next notice from selection | - | 3 | - |
| 3 | Determine offender role (Driver/Owner/Hirer/Director) | - | 4 | - |
| 4 | Get eligible stages for role | - | 5 | - |
| 5 | Is new processing stage in eligible set? | Eligible? | 6 | 7 |
| 6 | Assign to changeable list | - | 8 | - |
| 7 | Assign to non-changeable list | - | 8 | - |
| 8 | Is this the last record? | Last? | 9 | 2 |
| 9 | Check results | Has changeable? | 10 | 11 |
| 10 | Display summary with changeable/non-changeable notices | - | End | - |
| 11 | Display error: Selected stage is not applicable | - | End | - |

**Eligible Stages by Role:**
- DRIVER: DN1, DN2, DR3
- OWNER/HIRER/DIRECTOR: ROV, RD1, RD2, RR3

---

### 2.5 Backend Processing Flow

**Swimlanes:** Backend | Database | External

| Step | Swimlane | Process | Decision | Yes Path | No Path |
|------|----------|---------|----------|----------|---------|
| 1 | Backend | Receive request from Staff Portal | - | 2 | - |
| 2 | Backend | Validate request data (format, required fields) | Valid? | 3 | Return error |
| 3 | Backend | Is this initial request or confirmation? | Initial? | 4 | 6 |
| 4 | Backend | Check for existing stage change record today | Exists? | 5 | 6 |
| 5 | Backend | Return WARNING: existing record, confirm to proceed | - | Wait for confirmation | - |
| 6 | Backend | Start processing loop for each notice | - | 7 | - |
| 7 | Backend | Check eligibility (court stage, PS, role) | Eligible? | 8 | Mark FAILED, next |
| 8 | Database | Query VON record | Found? | 9 | Mark FAILED, next |
| 9 | Backend | Call Amount Payable Calculation Sub-flow | - | 10 | - |
| 10 | Backend | Call Stage Update Sub-flow | - | 11 | - |
| 11 | Database | Update VON record | Success? | 12 | Mark FAILED, next |
| 12 | Database | Update ONOD dh_mha_check_allow (if provided) | - | 13 | - |
| 13 | Database | Insert CPS audit record | - | 14 | - |
| 14 | Backend | Mark notice as UPDATED | - | 15 | - |
| 15 | Backend | Is this the last notice? | Last? | 16 | 6 |
| 16 | Backend | Any successful updates? | Has success? | 17 | 18 |
| 17 | Backend | Call Generate Report Sub-flow | - | 18 | - |
| 18 | Backend | Any failures? | Has failure? | 19 | 20 |
| 19 | External | Send error notification email | - | 20 | - |
| 20 | Backend | Build response with results | - | 21 | - |
| 21 | Backend | Respond to Staff Portal | - | End | - |

**Database Operations:**
- Query: `ocms_valid_offence_notice`
- Query: `ocms_offence_notice_owner_driver`
- Update: `ocms_valid_offence_notice`
- Update: `ocms_offence_notice_owner_driver`
- Insert: `ocms_change_of_processing_stage`

---

### 2.6 Subflow - Stage Update

**Swimlanes:** Backend | Database

| Step | Swimlane | Process | Description |
|------|----------|---------|-------------|
| 1 | Backend | Receive VON record and new stage | Input parameters |
| 2 | Backend | Save old last_processing_stage and last_processing_date | Preserve for prev fields |
| 3 | Backend | Set prev_processing_stage = old last_processing_stage | Update prev |
| 4 | Backend | Set prev_processing_date = old last_processing_date | Update prev |
| 5 | Backend | Set last_processing_stage = new stage | Update last |
| 6 | Backend | Set last_processing_date = NOW() | Update last |
| 7 | Database | Query NEXT_STAGE_{newStage} parameter | Get next stage |
| 8 | Backend | Set next_processing_stage = parameter value | Update next |
| 9 | Database | Query STAGEDAYS parameter for new stage | Get days |
| 10 | Backend | Set next_processing_date = NOW() + STAGEDAYS | Calculate next date |
| 11 | Backend | Return updated VON | Output |

**Parameters Used:**
- `NEXT_STAGE_{stage}` → Next stage value
- `STAGEDAYS` → Days until next stage

---

### 2.7 Subflow - Amount Payable Calculation

**Swimlanes:** Backend | Database

| Step | Swimlane | Process | Decision | Yes Path | No Path |
|------|----------|---------|----------|----------|---------|
| 1 | Backend | Receive previous stage, new stage, composition amount | - | 2 | - |
| 2 | Backend | Check calculation rule | - | - | - |
| 3 | Backend | Rule 1: prev IN (ROV,ENA,RD1,RD2) AND new = RR3? | Match? | 4a | 4b |
| 4a | Database | Get ADMIN_FEE parameter | - | 5a | - |
| 5a | Backend | amount = composition + adminFee | - | End | - |
| 4b | Backend | Rule 2: prev IN (DN1,DN2) AND new = DR3? | Match? | 4c | 4d |
| 4c | Database | Get ADMIN_FEE parameter | - | 5b | - |
| 5b | Backend | amount = composition + adminFee | - | End | - |
| 4d | Backend | Rule 3: prev IN (all reminder stages) AND new IN (CFC,CPC)? | Match? | 4e | 4f |
| 4e | Database | Get SURCHARGE parameter | - | 5c | - |
| 5c | Backend | amount = composition + surcharge | - | End | - |
| 4f | Backend | Rule 4: prev IN (CFC,CPC) AND new IN (RR3,DR3)? | Match? | 4g | 4h |
| 4g | Database | Get ADMIN_FEE parameter | - | 5d | - |
| 5d | Backend | amount = composition + adminFee | - | End | - |
| 4h | Backend | Other transitions | - | 5e | - |
| 5e | Backend | amount = composition (no change) | - | End | - |

**Calculation Matrix:**
| Previous Stage | New Stage | Formula |
|----------------|-----------|---------|
| ROV/ENA/RD1/RD2 | RR3 | composition + adminFee |
| DN1/DN2 | DR3 | composition + adminFee |
| All reminder stages | CFC/CPC | composition + surcharge |
| CFC/CPC | RR3/DR3 | composition + adminFee |
| Other transitions | Any | composition (no change) |

---

### 2.8 Subflow - Generate Report

**Swimlanes:** Backend | External (Azure Blob)

| Step | Swimlane | Process | Decision | Yes Path | No Path |
|------|----------|---------|----------|----------|---------|
| 1 | Backend | Receive list of successful change records | - | 2 | - |
| 2 | Backend | Query today's change records from CPS table | - | 3 | - |
| 3 | Backend | Has records? | Has data? | 4 | End (no report) |
| 4 | Backend | Build Excel report content | - | 5 | - |
| 5 | Backend | Generate filename: ChangeStageReport_{date}_{time}_{userId}.xlsx | - | 6 | - |
| 6 | External | Upload to Azure Blob Storage | - | 7 | - |
| 7 | Backend | Get report URL | - | 8 | - |
| 8 | Backend | Return report URL | - | End | - |

**Report Content:**
- Notice Number
- Previous Stage
- New Stage
- Reason of Change
- Remarks
- Date of Change
- Authorised Officer

---

### 2.9 PLUS Integration Flow

**Swimlanes:** PLUS | Backend | Database

| Step | Swimlane | Process | Decision | Yes Path | No Path |
|------|----------|---------|----------|----------|---------|
| 1 | PLUS | PLUS officer initiates stage change request | - | 2 | - |
| 2 | PLUS | PLUS system checks eligibility | Eligible? | 3 | End (PLUS side) |
| 3 | PLUS | PLUS sends request to OCMS Backend API | - | 4 | - |
| 4 | Backend | Receive PLUS request | - | 5 | - |
| 5 | Backend | Validate: source=005 AND nextStage=CFC? | CFC from PLUS? | Return error | 6 |
| 6 | Backend | Validate: offenceType=U AND nextStage IN (DN1,DN2,DR3,CPC)? | Skip condition? | Return success (skip) | 7 |
| 7 | Database | Query stage_map for transition validation | - | 8 | - |
| 8 | Backend | Stage transition allowed? | Allowed? | 9 | Return error |
| 9 | Backend | Process each notice in batch | - | 10 | - |
| 10 | Database | Query VON record | Found? | 11 | Mark error, next |
| 11 | Backend | Call Stage Update Sub-flow | - | 12 | - |
| 12 | Database | Update VON record | - | 13 | - |
| 13 | Database | Insert CPS audit record (source=PLUS) | - | 14 | - |
| 14 | Backend | Is this the last notice? | Last? | 15 | 9 |
| 15 | Backend | Any errors? | Has error? | 16 | 17 |
| 16 | Backend | Return error with all error messages | - | End | - |
| 17 | Backend | Generate Excel report | - | 18 | - |
| 18 | Backend | Return success response | - | End | - |

**PLUS Request Fields:**
- noticeNo (array)
- lastStageName
- nextStageName
- lastStageDate
- newStageDate
- offenceType (D/O/H/DIR)
- source (005)

---

## 3. Swimlane Reference

| Swimlane | Description | Color |
|----------|-------------|-------|
| Frontend | OCMS Staff Portal UI | White |
| Backend | OCMS Backend API | Light Gray |
| Database | Database operations (VON, ONOD, CPS, Parameter) | Purple |
| External | External systems (Azure Blob, Email, PLUS) | Light Blue |

---

## 4. Shape Legend

| Shape | Description |
|-------|-------------|
| Ellipse | Start / End |
| Rectangle | Process / Action |
| Diamond | Decision |
| Rectangle with double border | Sub-process (reference to another diagram) |
| Parallelogram | Input / Output |
| Cylinder | Database |

---

## 5. Cross-Reference Matrix

| Diagram | References To | Referenced By |
|---------|---------------|---------------|
| High-Level | Search Notice, Portal Validation, Backend Processing, Report Management | - |
| Search Notice | - | High-Level |
| User Journey | Portal Validation, Backend Processing | High-Level |
| Portal Validation | - | User Journey, High-Level |
| Backend Processing | Stage Update, Amount Payable, Generate Report | High-Level, User Journey |
| Stage Update | - | Backend Processing, PLUS Integration, Toppan Integration |
| Amount Payable | - | Backend Processing, Toppan Integration |
| Generate Report | - | Backend Processing, PLUS Integration |
| PLUS Integration | Stage Update, Generate Report | External (PLUS System) |
| Report Management | - | High-Level (Staff Portal) |
| Toppan Integration | Stage Update, Amount Payable | External (Toppan Cron Job) |

---

### 2.10 Report Management Flow

**Swimlanes:** Staff Portal | Backend | Azure Blob Storage

#### [A] Retrieve Reports List

| Step | Swimlane | Process | Decision | Yes Path | No Path |
|------|----------|---------|----------|----------|---------|
| 1 | Staff Portal | User clicks View Reports | - | 2 | - |
| 2 | Staff Portal | Enter date range (startDate, endDate) | - | 3 | - |
| 3 | Staff Portal | Call Reports API | - | 4 | - |
| 4 | Backend | Receive request | - | 5 | - |
| 5 | Backend | Validate date range | Valid? | 6 | Return error |
| 6 | Backend | Check: Range <= 90 days? | Valid? | 7 | Return error: Range exceeds 90 days |
| 7 | Backend | Query CPS table by date range | - | 8 | - |
| 8 | Backend | Group records by date | - | 9 | - |
| 9 | Backend | Build report metadata list | - | 10 | - |
| 10 | Backend | Return response | - | 11 | - |
| 11 | Staff Portal | Display reports list | - | End | - |

**API Request:**
```json
POST /change-processing-stage/reports
{
  "startDate": "2025-12-01",
  "endDate": "2025-12-31"
}
```

**Database Operation:**
```sql
SELECT * FROM ocms_change_of_processing_stage
WHERE cre_date BETWEEN ? AND ?
```

#### [B] Download Individual Report

| Step | Swimlane | Process | Decision | Yes Path | No Path |
|------|----------|---------|----------|----------|---------|
| 1 | Staff Portal | User clicks Download on a report | - | 2 | - |
| 2 | Staff Portal | Call Download API | - | 3 | - |
| 3 | Backend | Receive request | - | 4 | - |
| 4 | Backend | Validate reportId (sanitize, check format) | Valid? | 5 | Return error: Invalid ID |
| 5 | Backend | Build blob path | - | 6 | - |
| 6 | Azure Blob | Download file from Azure Blob Storage | - | 7 | - |
| 7 | Backend | File found? | Found? | 8 | Return 404: Not found |
| 8 | Backend | Stream file to response | - | 9 | - |
| 9 | Staff Portal | Save/Open Excel file | - | End | - |

**API Request:**
```json
POST /change-processing-stage/report/download
{
  "reportId": "ChangeStageReport_20251220_143022_OIC001.xlsx"
}
```

**Response:**
- Content-Type: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- Content-Disposition: `attachment; filename="ChangeStageReport_20251220_143022_OIC001.xlsx"`
- Body: Binary Excel file

---

### 2.11 Toppan Integration Flow

**Swimlanes:** Toppan Cron Job | Backend | Database

| Step | Swimlane | Process | Decision | Yes Path | No Path |
|------|----------|---------|----------|----------|---------|
| 1 | Toppan Cron | Start (scheduled job) | - | 2 | - |
| 2 | Toppan Cron | Generate letter files | - | 3 | - |
| 3 | Toppan Cron | Call OCMS Update API | - | 4 | - |
| 4 | Backend | Receive request | - | 5 | - |
| 5 | Backend | Validate request | - | 6 | - |
| 6 | Backend | Loop each notice | - | 7 | - |
| 7 | Database | Query CPS table: is manual change? | - | 8 | - |
| 8 | Backend | Is manual? (source = OCMS/PLUS) | Manual? | 9a | 9b |
| 9a | Backend | [MANUAL] Mark for stage update ONLY | - | 10 | - |
| 9b | Backend | [AUTO] Mark for stage + amount update | - | 10 | - |
| 10 | Database | Query VON record | Found? | 11 | Add to errors, skip |
| 11 | Backend | Update VON stage (call updateVonStage) | - | 12 | - |
| 12 | Backend | Is AUTO? | AUTO? | 13 | 15 |
| 13 | Backend | Calculate amount payable | - | 14 | - |
| 14 | Database | Update VON amount_payable | - | 15 | - |
| 15 | Database | Save VON | - | 16 | - |
| 16 | Backend | Increment counter (manual/auto) | - | 17 | - |
| 17 | Backend | Is last notice? | Last? | 18 | 6 |
| 18 | Backend | Build statistics response | - | 19 | - |
| 19 | Backend | Return response | - | 20 | - |
| 20 | Toppan Cron | Log results | - | End | - |

**API Request:**
```json
POST /internal/toppan/update-stages
{
  "noticeNumbers": ["441000001X", "441000002Y", "441000003Z"],
  "currentStage": "DN2",
  "processingDate": "2025-12-20T00:30:00"
}
```

**Manual vs Automatic Detection:**
```sql
SELECT * FROM ocms_change_of_processing_stage
WHERE notice_no = ? AND date_of_change = ?
  AND source IN ('OCMS', 'PLUS')
```
- If record found → MANUAL (amount already calculated at submit)
- If no record → AUTOMATIC (Toppan calculates amount)

**Response:**
```json
{
  "totalNotices": 3,
  "automaticUpdates": 2,
  "manualUpdates": 1,
  "skipped": 0,
  "errors": null,
  "success": true
}
```

---

## Appendix A: Diagram File Mapping

| Section | Diagram Tab Name in .drawio |
|---------|----------------------------|
| 2.1 | High-Level |
| 2.2 | Search Notice |
| 2.3 | Change_Notice_Processing_Stage_Unclaimed_Form_User_Journey |
| 2.4 | Portal_validation |
| 2.5 | Backend_Change_Processing_Stage |
| 2.6 | Change_Processing_Stage_Subflow |
| 2.7 | Handling_amount_payable_update |
| 2.8 | Generate-report |
| 2.9 | PLUS-Manual-Stage-Change |
| 2.10 | Report Management |
| 2.11 | Toppan Integration |

## Appendix B: Change Log

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-01-26 | System | Initial version based on FD v1.2 and drawio |

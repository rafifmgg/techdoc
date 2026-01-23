# Flowchart Plan: Manage Change Processing Stage

## Overview

| Attribute | Value |
| --- | --- |
| Feature Name | Manage Change Processing Stage |
| Version | v1.3 |
| Author | Claude |
| Created Date | 21/01/2026 |
| Updated Date | 22/01/2026 |
| Status | Active |
| FD Reference | OCMS 15 - Section 2 & Section 3 |
| TD Reference | OCMS 15 - Section 1 & Section 2 |

---

## 1. Diagram Sections (Tabs)

The Technical Flowchart will contain the following tabs/sections:

| Tab # | Tab Name | Description | Priority |
| --- | --- | --- | --- |
| 1 | Section_1_High_Level | High-level overview of Manual Change Processing Stage via OCMS Staff Portal | High |
| 2 | Section_1_Search_Notices | Detailed flow for searching notices and checking eligibility | High |
| 3 | Section_1_Portal_Validation | Portal validation for new processing stage eligibility | High |
| 4 | Section_1_Backend_Processing | Backend processing for change processing stage submission | High |
| 5 | Section_1_Change_Stage_Subflow | Sub-flow for changing notice's next processing stage | High |
| 6 | Section_1_Generate_Report | Sub-flow for generating Change Processing Stage Report | High |
| 7 | Section_2_High_Level | High-level overview of Manual Change Processing Stage via PLUS Staff Portal | Medium |
| 8 | Section_2_PLUS_Integration | Detailed flow for PLUS integration and backend processing | Medium |
| 9 | Section_3_Toppan_Integration | Flow for Toppan cron job handling manual stage changes | Medium |

---

## 2. Systems Involved (Swimlanes)

| System/Tier | Color Code | Hex | Description |
| --- | --- | --- | --- |
| OCMS Staff Portal (Frontend) | Light Blue | #dae8fc | Portal user interface |
| OCMS Backend API | Light Green | #d5e8d4 | Backend API processing |
| Database (Intranet) | Light Yellow | #fff2cc | Intranet database operations |
| PLUS Staff Portal | Light Purple | #e1d5e7 | External system (PLUS) |
| External System (Toppan Cron) | Light Orange | #ffe6cc | Batch job system |

---

## 3. Section 1: Manual Change Processing Stage via OCMS Staff Portal

### 3.1 Tab 1: Section_1_High_Level

#### 3.1.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | Manual Change Processing Stage - High Level |
| Section | 1.1 |
| Trigger | OIC initiates change processing stage request in OCMS Staff Portal |
| Frequency | Real-time (on-demand by OIC) |
| Systems Involved | OCMS Staff Portal, OCMS Backend, Database |
| Expected Outcome | Notice processing stage updated and report generated |

#### 3.1.2 High Level Flow Diagram (ASCII)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     OCMS STAFF PORTAL                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│    ┌───────────┐                                                         │
│    │   Start   │                                                         │
│    └─────┬─────┘                                                         │
│          │                                                               │
│          ▼                                                               │
│    ┌────────────────────────────────────────┐                           │
│    │ OIC launches Change Processing Stage   │                           │
│    │ module and enters search criteria      │                           │
│    └────────────────┬───────────────────────┘                           │
│                     │                                                    │
│                     ▼                                                    │
│    ┌────────────────────────────────────────┐                           │
│    │ Search notices and check eligibility   │                           │
│    │ (displays eligible and ineligible)     │                           │
│    └────────────────┬───────────────────────┘                           │
│                     │                                                    │
│                     ▼                                                    │
│    ┌────────────────────────────────────────┐                           │
│    │ OIC selects notice(s) and fills form   │                           │
│    │ - New Processing Stage                 │                           │
│    │ - Reason of Change                     │                           │
│    │ - Remarks (if reason = OTH)            │                           │
│    │ - DH/MHA Check option                  │                           │
│    └────────────────┬───────────────────────┘                           │
│                     │                                                    │
│                     ▼                                                    │
│    ┌────────────────────────────────────────┐                           │
│    │ Portal validates new processing stage  │                           │
│    │ (checks offender type matching)        │                           │
│    └────────────────┬───────────────────────┘                           │
│                     │                                                    │
│                     ▼                                                    │
│    ┌────────────────────────────────────────┐                           │
│    │ Submit to OCMS Backend API             │                           │
│    └────────────────┬───────────────────────┘                           │
│                     │                                                    │
│                     ▼                                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                     OCMS BACKEND                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                     │                                                    │
│                     ▼                                                    │
│    ┌────────────────────────────────────────┐                           │
│    │ Backend processes change request       │                           │
│    │ - Validate request                     │                           │
│    │ - Check VON exists                     │                           │
│    │ - Check duplicate record               │                           │
│    │ - Update VON processing stage          │                           │
│    │ - Insert change record                 │                           │
│    │ - Calculate amount payable             │                           │
│    │ - Generate report                      │                           │
│    └────────────────┬───────────────────────┘                           │
│                     │                                                    │
│                     ▼                                                    │
│    ┌────────────────────────────────────────┐                           │
│    │ Return response with status and        │                           │
│    │ report URL to Staff Portal             │                           │
│    └────────────────┬───────────────────────┘                           │
│                     │                                                    │
│                     ▼                                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                     OCMS STAFF PORTAL                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                     │                                                    │
│                     ▼                                                    │
│    ┌────────────────────────────────────────┐                           │
│    │ Display status and auto-download       │                           │
│    │ Change Processing Stage Report         │                           │
│    └────────────────┬───────────────────────┘                           │
│                     │                                                    │
│                     ▼                                                    │
│    ┌───────────┐                                                         │
│    │    End    │                                                         │
│    └───────────┘                                                         │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 3.1.3 Process Steps Table (High Level)

| Step | Definition | Brief Description |
| --- | --- | --- |
| Start | Entry point | OIC initiates change processing stage module |
| Launch Module | User action | OIC launches Change Processing Stage module and enters search criteria |
| Search Notices | Search and filter | Backend searches notices and checks eligibility |
| Fill Form | User input | OIC selects notices and fills change processing stage form |
| Portal Validation | Frontend validation | Portal validates new processing stage eligibility |
| Submit to Backend | API call | Portal sends change request to backend |
| Backend Processing | Backend action | Backend validates, updates database, generates report |
| Return Response | API response | Backend returns status and report URL |
| Display Result | UI display | Portal displays status and auto-downloads report |
| End | Exit point | Process complete |

---

### 3.2 Tab 2: Section_1_Search_Notices

#### 3.2.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | Search Notices for Change Processing Stage |
| Section | 1.2 |
| Trigger | OIC enters search criteria and submits |
| Frequency | Real-time |
| Systems Involved | OCMS Staff Portal, OCMS Backend, Database |
| Expected Outcome | Return segregated lists of eligible and ineligible notices |

#### 3.2.2 Process Steps Table

| Step | Type | Definition | Brief Description | Next Step |
| --- | --- | --- | --- | --- |
| 1 | Start | Start Search Process | OIC launches Change Processing Stage module | 2 |
| 2 | Process | OIC enters search criteria | OIC enters at least one: Notice No, ID No, Vehicle No, Current Stage, Date | 3 |
| 3 | Process | Portal validates search data | Portal validates format (FE-001 to FE-005) | 4 |
| 4 | Decision | Search data valid? | Check if entered data passes format validation | Yes→5, No→E1 |
| 5 | Process | Send search request to Backend | Portal calls searchChangeProcessingStage API | 6 |
| 6 | Process | Backend queries ocms_valid_offence_notice | If ID No: Query ONOD first, then join VON. Otherwise: Query VON directly | 7 |
| 7 | Decision | Any record found? | Check if query returns any result | Yes→8, No→E2 |
| 8 | Process | Check notice eligibility | Backend checks if notice is eligible for stage change | 9 |
| 9 | Decision | Is in Court Stage? | Check if current_processing_stage NOT IN Reminder Stages (NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3, CPC, CFC) | Yes→10, No→11 |
| 10 | Process | Mark as INELIGIBLE | Add to ineligibleNotices list with reason COURT_STAGE | 12 |
| 11 | Decision | Is PS/TS Blocked? | Check if permanent or temporary suspension active | Yes→13, No→14 |
| 12 | Decision | Last record? | Check if current record is last in result set | Yes→15, No→8 |
| 13 | Process | Mark as INELIGIBLE | Add to ineligibleNotices list with reason PS_BLOCKED or TS_BLOCKED | 12 |
| 14 | Process | Mark as ELIGIBLE | Add to eligibleNotices list | 12 |
| 15 | Process | Return segregated lists | Return eligibleNotices and ineligibleNotices to Portal | 16 |
| 16 | Process | Portal displays results | Display notice list with details and checkboxes | End |
| E1 | Error | Display validation error | Show "Invalid <field>" message | End |
| E2 | Error | Display no record message | Show "No record found" | End |
| End | End | End Search | Process complete | - |

#### 3.2.3 Decision Logic

| ID | Decision | Input | Condition | Yes Action | No Action |
| --- | --- | --- | --- | --- | --- |
| D1 | Search data valid? | Form input | Passes FE-001 to FE-005 validation | Continue to Step 5 | Error E1 |
| D2 | Any record found? | Query result | count > 0 | Continue to Step 8 | Error E2 |
| D3 | Is in Court Stage? | current_processing_stage | NOT IN Reminder Stages | Mark INELIGIBLE | Go to D4 |
| D4 | Is PS/TS Blocked? | suspension_status | PS or TS suspension active | Mark INELIGIBLE | Mark ELIGIBLE |
| D5 | Last record? | Loop counter | current == total | Return results | Continue to next record |

#### 3.2.4 Database Operations

| Step | Operation | Database | Table | Query/Action |
| --- | --- | --- | --- | --- |
| 6 (ID Search) | SELECT | Intranet | ocms_offence_notice_owner_driver | Query by id_no, then JOIN with VON |
| 6 (Other Search) | SELECT | Intranet | ocms_valid_offence_notice | Query by notice_no, vehicle_no, current_processing_stage, or date |

#### 3.2.5 API Specification Reference

**API:** searchChangeProcessingStage
**Endpoint:** `/ocms/v1/change-processing-stage/search`
**Method:** POST
**Reference:** plan_api.md Section 1.1

---

### 3.3 Tab 3: Section_1_Portal_Validation

#### 3.3.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | Portal Validation for New Processing Stage |
| Section | 1.3 |
| Trigger | OIC submits Change Processing Stage form |
| Frequency | Real-time |
| Systems Involved | OCMS Staff Portal (Frontend only) |
| Expected Outcome | Validated notices ready for backend submission or error displayed |

#### 3.3.2 Process Steps Table

| Step | Type | Definition | Brief Description | Next Step |
| --- | --- | --- | --- | --- |
| 1 | Start | Start Validation | OIC has filled form and clicks submit | 2 |
| 2 | Decision | Required fields filled? | Check New Processing Stage, Reason of Change, Remarks (if OTH) | Yes→3, No→E1 |
| 3 | Decision | DH/MHA check unchecked? | Check if Send for DH/MHA Check is unchecked | Yes→4, No→5 |
| 4 | Process | Prompt user confirmation | Show "Notice will not go for DH/MHA check. Proceed?" | 5 |
| 5 | Process | Initialize loop | Start validating each selected notice | 6 |
| 6 | Decision | Is offender = DRIVER? | Check owner_driver_indicator = 'D' | Yes→7, No→9 |
| 7 | Decision | New stage in DN1/DN2/DR3? | Check if newProcessingStage in allowed list for Driver | Yes→11, No→12 |
| 8 | Decision | Last record? | Check if current notice is last in selection | Yes→13, No→6 |
| 9 | Decision | Is offender = OWNER/HIRER/DIRECTOR? | Check owner_driver_indicator IN ('O', 'H', 'R') | Yes→10, No→12 |
| 10 | Decision | New stage in ROV/RD1/RD2/RR3? | Check if newProcessingStage in allowed list for Owner | Yes→11, No→12 |
| 11 | Process | Mark as CHANGEABLE | Add to changeableNotices list | 8 |
| 12 | Process | Mark as NON-CHANGEABLE | Add to nonChangeableNotices list | 8 |
| 13 | Decision | Has non-changeable notices? | Check if nonChangeableNotices list is not empty | Yes→E2, No→14 |
| 14 | Process | Display summary page | Show summary for OIC review before backend submission | 15 |
| 15 | Process | OIC confirms submission | OIC reviews and confirms | End |
| E1 | Error | Display validation error | Show "Required field missing" or "Remarks mandatory for OTH" | End |
| E2 | Error | Display non-changeable error | Show "Some notices not eligible. Uncheck inapplicable notices." | End |
| End | End | End Validation | Proceed to backend submission | - |

#### 3.3.3 Decision Logic

| ID | Decision | Input | Condition | Yes Action | No Action |
| --- | --- | --- | --- | --- | --- |
| D1 | Required fields filled? | Form fields | All mandatory fields not empty + Remarks check | Continue | Error E1 |
| D2 | DH/MHA unchecked? | Checkbox state | Send for DH/MHA Check = false | Prompt user | Continue |
| D3 | Is offender = DRIVER? | owner_driver_indicator | = 'D' | Go to D4 | Go to D5 |
| D4 | New stage in DN list? | newProcessingStage | IN ('DN1', 'DN2', 'DR3') | Mark CHANGEABLE | Mark NON-CHANGEABLE |
| D5 | Is offender = OWNER? | owner_driver_indicator | IN ('O', 'H', 'R') | Go to D6 | Mark NON-CHANGEABLE |
| D6 | New stage in RO list? | newProcessingStage | IN ('ROV', 'RD1', 'RD2', 'RR3') | Mark CHANGEABLE | Mark NON-CHANGEABLE |
| D7 | Has non-changeable? | nonChangeableNotices | count > 0 | Error E2 | Continue |
| D8 | Last record? | Loop counter | current == total | Check results | Continue loop |

---

### 3.4 Tab 4: Section_1_Backend_Processing

#### 3.4.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | Backend Processing for Change Processing Stage |
| Section | 1.4 |
| Trigger | Portal submits change request via API |
| Frequency | Real-time |
| Systems Involved | OCMS Backend API, Database |
| Expected Outcome | VON updated, change record inserted, report generated |

#### 3.4.2 Process Steps Table

| Step | Type | Definition | Brief Description | Next Step |
| --- | --- | --- | --- | --- |
| 1 | Start | Start Backend Processing | Backend receives API request from Portal | 2 |
| 2 | Process | Validate request format | Check items not empty, noticeNo present (BE-001, BE-002) | 3 |
| 3 | Decision | Request valid? | Check if request format passes validation | Yes→4, No→E1 |
| 4 | Process | Initialize batch processing | Start processing each item in request | 5 |
| 5 | Process | Query VON by notice_no | Query ocms_valid_offence_notice to check existence | 6 |
| 6 | Decision | VON exists? | Check if query returns record | Yes→7, No→E2 |
| 7 | Process | Check duplicate change record | Query ocms_change_of_processing by notice_no and today's date | 8 |
| 8 | Decision | Duplicate record exists? | Check if change record exists for today | Yes→9, No→11 |
| 9 | Decision | isConfirmation = true? | Check if user confirmed to proceed with duplicate | Yes→11, No→E3 |
| 10 | Decision | Last item? | Check if current item is last in batch | Yes→20, No→5 |
| 11 | Process | Validate stage transition | Check ocms_stage_map for allowed transition | 12 |
| 12 | Decision | Stage transition allowed? | Check if current stage → new stage is valid | Yes→13, No→E4 |
| 13 | Process | Call Change Stage Sub-flow | Execute sub-flow to update VON and insert change record | 14 |
| 14 | Decision | Change stage success? | Check if sub-flow completed successfully | Yes→15, No→E5 |
| 15 | Process | Mark item as SUCCESS | Add to results with outcome = UPDATED | 10 |
| 16 | Process | Increment success counter | Update summary.succeeded count | 10 |
| 17 | Process | Mark item as FAILED | Add to results with outcome = FAILED and error message | 10 |
| 18 | Process | Increment failed counter | Update summary.failed count | 10 |
| 19 | Decision | Any success? | Check if summary.succeeded > 0 | Yes→20, No→22 |
| 20 | Process | Generate Change Stage Report | Call report generation sub-flow | 21 |
| 21 | Process | Upload report to Azure Blob | Store report and get signed URL | 22 |
| 22 | Process | Build response | Build response with summary, results, and report URL | 23 |
| 23 | Process | Return response to Portal | Send SUCCESS/PARTIAL/FAILED response | End |
| E1 | Error | Return format error | Return OCMS.CPS.INVALID_FORMAT error | End |
| E2 | Error | Mark as FAILED (NOT_FOUND) | VON not found | 17 |
| E3 | Error | Mark as FAILED (DUPLICATE) | Return DUPLICATE_RECORD warning | 17 |
| E4 | Error | Mark as FAILED (INVALID_TRANSITION) | Stage transition not allowed | 17 |
| E5 | Error | Mark as FAILED (UNEXPECTED) | Unexpected error during stage change | 17 |
| End | End | End Backend Processing | Response sent to Portal | - |

#### 3.4.3 Decision Logic

| ID | Decision | Input | Condition | Yes Action | No Action |
| --- | --- | --- | --- | --- | --- |
| D1 | Request valid? | Request payload | items not empty + noticeNo present | Continue | Error E1 |
| D2 | VON exists? | Query result | VON found in database | Continue | Error E2 |
| D3 | Duplicate exists? | Query result | Change record exists for today | Go to D4 | Continue |
| D4 | isConfirmation = true? | Request flag | isConfirmation field = true | Proceed | Error E3 |
| D5 | Stage transition allowed? | stage_map query | Transition rule exists | Continue | Error E4 |
| D6 | Change stage success? | Sub-flow result | Sub-flow returns success | Mark SUCCESS | Error E5 |
| D7 | Last item? | Loop counter | current == total | Check any success | Continue |
| D8 | Any success? | summary.succeeded | count > 0 | Generate report | Skip report |

#### 3.4.4 Database Operations

| Step | Operation | Database | Table | Query/Action |
| --- | --- | --- | --- | --- |
| 5 | SELECT | Intranet | ocms_valid_offence_notice | Query by notice_no |
| 7 | SELECT | Intranet | ocms_change_of_processing | Query by notice_no and today's date_of_change |
| 11 | SELECT | Intranet | ocms_stage_map | Validate stage transition |
| 13 | UPDATE | Intranet | ocms_valid_offence_notice | Update next_processing_stage (see sub-flow) |
| 13 | INSERT | Intranet | ocms_change_of_processing | Insert change record (see sub-flow) |
| 13 | UPDATE | Intranet | ocms_offence_notice_owner_driver | Update DH/MHA check flag (see sub-flow) |

#### 3.4.5 API Specification Reference

**API:** changeProcessingStage
**Endpoint:** `/ocms/v1/change-processing-stage`
**Method:** POST
**Reference:** plan_api.md Section 1.3

---

### 3.5 Tab 5: Section_1_Change_Stage_Subflow

#### 3.5.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | Change Notice's Next Processing Stage Sub-flow |
| Section | 1.5 |
| Trigger | Backend calls sub-flow from main processing |
| Frequency | Per item in batch |
| Systems Involved | OCMS Backend, Database |
| Expected Outcome | VON updated with new stage, change record inserted |

#### 3.5.2 Process Steps Table

| Step | Type | Definition | Brief Description | Next Step |
| --- | --- | --- | --- | --- |
| 1 | Start | Start Sub-flow | Receives noticeNo and newStage | 2 |
| 2 | Process | Query current VON record | Get current stage and payment data | 3 |
| 3 | Process | Calculate new stage date | Set next_processing_date based on new stage | 4 |
| 4 | Process | Calculate amount payable | Calculate amount based on new stage rules | 5 |
| 5 | Process | Update VON next_processing_stage | UPDATE ocms_valid_offence_notice SET next_processing_stage, next_processing_date | 6 |
| 6 | Decision | VON update success? | Check if UPDATE succeeded | Yes→7, No→E1 |
| 7 | Process | Insert change record | INSERT into ocms_change_of_processing | 8 |
| 8 | Decision | Insert change record success? | Check if INSERT succeeded | Yes→9, No→E2 |
| 9 | Decision | DH/MHA check required? | Check if dhMhaCheck flag = true | Yes→10, No→12 |
| 10 | Process | Update ONOD DH/MHA flag | UPDATE ocms_offence_notice_owner_driver SET mha_dh_check_allow = 'Y' | 11 |
| 11 | Decision | ONOD update success? | Check if UPDATE succeeded | Yes→12, No→E3 |
| 12 | Process | Return success | Return success to main flow | End |
| E1 | Error | Return error (VON update failed) | Return error to main flow | End |
| E2 | Error | Return error (Insert failed) | Return error to main flow | End |
| E3 | Error | Return error (ONOD update failed) | Return error to main flow | End |
| End | End | End Sub-flow | Control returns to main flow | - |

#### 3.5.3 Decision Logic

| ID | Decision | Input | Condition | Yes Action | No Action |
| --- | --- | --- | --- | --- | --- |
| D1 | VON update success? | Update result | Rows affected > 0 | Continue | Error E1 |
| D2 | Insert success? | Insert result | Rows affected > 0 | Continue | Error E2 |
| D3 | DH/MHA check required? | dhMhaCheck flag | = true | Update ONOD | Skip |
| D4 | ONOD update success? | Update result | Rows affected > 0 | Success | Error E3 |

#### 3.5.4 Database Operations

| Step | Operation | Database | Table | Fields Updated |
| --- | --- | --- | --- | --- |
| 5 | UPDATE | Intranet | ocms_valid_offence_notice | prev_processing_stage, prev_processing_date, last_processing_stage, last_processing_date, next_processing_stage, next_processing_date, amount_payable, payment_acceptance_allowed, upd_user_id, upd_dtm |
| 7 | INSERT | Intranet | ocms_change_of_processing | notice_no, date_of_change, last_processing_stage, new_processing_stage, reason_of_change, authorised_officer, source, remarks, cre_date, cre_user_id |
| 10 | UPDATE | Intranet | ocms_offence_notice_owner_driver | mha_dh_check_allow, upd_user_id, upd_dtm |

**Audit User:** ocmsiz_app_conn (Do NOT use "SYSTEM")

---

### 3.6 Tab 6: Section_1_Generate_Report

#### 3.6.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | Generate Change Processing Stage Report |
| Section | 1.6 |
| Trigger | Backend completes change processing with at least 1 success |
| Frequency | Per batch submission |
| Systems Involved | OCMS Backend, Azure Blob Storage |
| Expected Outcome | Excel report generated and uploaded, URL returned |

#### 3.6.2 Process Steps Table

| Step | Type | Definition | Brief Description | Next Step |
| --- | --- | --- | --- | --- |
| 1 | Start | Start Report Generation | Receives batch results | 2 |
| 2 | Process | Prepare report data | Collect notice details and results | 3 |
| 3 | Process | Generate Excel file | Create Excel with submitted notice details | 4 |
| 4 | Decision | Excel generation success? | Check if file created successfully | Yes→5, No→E1 |
| 5 | Process | Generate report filename | Format: ChangeStageReport_yyyyMMdd_HHmmss_[userID].xlsx | 6 |
| 6 | Process | Upload to Azure Blob Storage | Upload to path: reports/change-stage/ | 7 |
| 7 | Decision | Upload success? | Check if upload succeeded | Yes→8, No→E2 |
| 8 | Process | Generate signed URL | Get temporary signed URL with expiry | 9 |
| 9 | Process | Return report URL | Return signed URL to main flow | End |
| E1 | Error | Return error (Excel generation failed) | Return error to main flow | End |
| E2 | Error | Return error (Upload failed) | Return error to main flow | End |
| End | End | End Report Generation | Control returns to main flow | - |

#### 3.6.3 Decision Logic

| ID | Decision | Input | Condition | Yes Action | No Action |
| --- | --- | --- | --- | --- | --- |
| D1 | Excel generation success? | File creation result | File created | Continue | Error E1 |
| D2 | Upload success? | Upload result | Blob stored | Continue | Error E2 |

#### 3.6.4 Report Data Mapping

| Report Field | Source Data | Database Table |
| --- | --- | --- |
| Notice No | notice_no | ocms_valid_offence_notice |
| Previous Stage | last_processing_stage | ocms_valid_offence_notice |
| New Stage | new_processing_stage | ocms_change_of_processing |
| Reason of Change | reason_of_change | ocms_change_of_processing |
| Remarks | remarks | ocms_change_of_processing |
| Authorised Officer | authorised_officer | ocms_change_of_processing |
| Date of Change | date_of_change | ocms_change_of_processing |
| Status | outcome | API response |

---

## 4. Section 2: Manual Change Processing Stage via PLUS Staff Portal

### 4.1 Tab 7: Section_2_High_Level

#### 4.1.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | Manual Change Processing Stage via PLUS - High Level |
| Section | 2.1 |
| Trigger | PLUS officer initiates change processing stage request |
| Frequency | Real-time |
| Systems Involved | PLUS Staff Portal, OCMS Backend, Database |
| Expected Outcome | Notice processing stage updated and confirmation returned to PLUS |

#### 4.1.2 High Level Flow Diagram (ASCII)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     PLUS STAFF PORTAL                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│    ┌───────────┐                                                         │
│    │   Start   │                                                         │
│    └─────┬─────┘                                                         │
│          │                                                               │
│          ▼                                                               │
│    ┌────────────────────────────────────────┐                           │
│    │ PLUS officer initiates stage change    │                           │
│    │ for notice(s)                          │                           │
│    └────────────────┬───────────────────────┘                           │
│                     │                                                    │
│                     ▼                                                    │
│    ┌────────────────────────────────────────┐                           │
│    │ PLUS system validates eligibility      │                           │
│    │ based on pre-defined rules             │                           │
│    └────────────────┬───────────────────────┘                           │
│                     │                                                    │
│                     ▼                                                    │
│    ┌────────────────────────────────────────┐                           │
│    │ PLUS sends request to OCMS via API     │                           │
│    │ (through APIM)                         │                           │
│    └────────────────┬───────────────────────┘                           │
│                     │                                                    │
│                     ▼                                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                     OCMS BACKEND                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                     │                                                    │
│                     ▼                                                    │
│    ┌────────────────────────────────────────┐                           │
│    │ Backend processes PLUS request         │                           │
│    │ - Validate request data                │                           │
│    │ - Check notice eligibility             │                           │
│    │ - Validate stage transition            │                           │
│    │ - Update VON processing stage          │                           │
│    │ - Insert change record                 │                           │
│    │ - Generate report                      │                           │
│    └────────────────┬───────────────────────┘                           │
│                     │                                                    │
│                     ▼                                                    │
│    ┌────────────────────────────────────────┐                           │
│    │ Return response to PLUS                │                           │
│    │ - Success: Updated notice details      │                           │
│    │ - Failure: Error message and lists    │                           │
│    └────────────────┬───────────────────────┘                           │
│                     │                                                    │
│                     ▼                                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                     PLUS STAFF PORTAL                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                     │                                                    │
│                     ▼                                                    │
│    ┌────────────────────────────────────────┐                           │
│    │ PLUS displays result to officer        │                           │
│    └────────────────┬───────────────────────┘                           │
│                     │                                                    │
│                     ▼                                                    │
│    ┌───────────┐                                                         │
│    │    End    │                                                         │
│    └───────────┘                                                         │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 4.1.3 Process Steps Table (High Level)

| Step | Definition | Brief Description |
| --- | --- | --- |
| Start | Entry point | PLUS officer initiates change processing stage |
| PLUS Initiates | User action | PLUS officer selects notice(s) and new stage |
| PLUS Validates | System validation | PLUS system checks eligibility |
| PLUS Sends Request | API call | PLUS sends request to OCMS via API |
| Backend Processing | Backend action | OCMS validates and processes change |
| Return Response | API response | OCMS returns status to PLUS |
| PLUS Displays Result | UI display | PLUS shows result to officer |
| End | Exit point | Process complete |

---

### 4.2 Tab 8: Section_2_PLUS_Integration

#### 4.2.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | PLUS Integration - Backend Processing |
| Section | 2.2 |
| Trigger | PLUS sends API request to OCMS |
| Frequency | Real-time |
| Systems Involved | PLUS Portal (External), OCMS Backend, Database |
| Expected Outcome | Stage change processed or error returned |

#### 4.2.2 Process Steps Table

| Step | Type | Definition | Brief Description | Next Step |
| --- | --- | --- | --- | --- |
| 1 | Start | Start PLUS Integration | OCMS receives request from PLUS via APIM | 2 |
| 2 | Process | Validate request format | Check noticeNo array, lastStageName, nextStageName (EXT-001 to EXT-003) | 3 |
| 3 | Decision | Request valid? | Check if request format passes validation | Yes→4, No→E1 |
| 4 | Decision | Source = '005'? | Check if source code is '005' (PLUS) | Yes→5, No→E2 |
| 5 | Process | Validate stage transition | Query ocms_stage_map for lastStageName → nextStageName | 6 |
| 6 | Decision | Stage transition allowed? | Check if transition rule exists | Yes→7, No→E3 |
| 7 | Process | Initialize batch processing | Start processing each notice in noticeNo array | 8 |
| 8 | Process | Query VON by notice_no | Query ocms_valid_offence_notice | 9 |
| 9 | Decision | VON exists? | Check if query returns record | Yes→10, No→E4 |
| 10 | Decision | Is in Court Stage? | Check if current_processing_stage NOT IN Allowed Stages (NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3, CPC, CFC) | Yes→E5, No→11 |
| 11 | Process | Call Change Stage Sub-flow | Execute sub-flow to update VON and insert change record | 12 |
| 12 | Decision | Change stage success? | Check if sub-flow completed successfully | Yes→13, No→E6 |
| 13 | Process | Add to success list | Increment noticeCount | 14 |
| 14 | Decision | Last notice? | Check if current notice is last in array | Yes→15, No→8 |
| 15 | Decision | Any success? | Check if noticeCount > 0 | Yes→16, No→18 |
| 16 | Process | Generate Change Stage Report | Call report generation sub-flow | 17 |
| 17 | Process | Return success response | Return status=SUCCESS, noticeCount, message | End |
| 18 | Process | Return failure response | Return status=FAILED, error code, message | End |
| E1 | Error | Return format error | Return status=FAILED, code=OCMS-4000, message="Invalid format" | End |
| E2 | Error | Return source error | Return status=FAILED, code=OCMS-4000, message="Invalid source code" | End |
| E3 | Error | Return transition error | Return status=FAILED, code=OCMS-4000, message="Stage transition not allowed" | End |
| E4 | Error | Mark notice as failed | Add to failed list with "VON not found" | 14 |
| E5 | Error | Mark notice as failed | Add to failed list with "Notice in court stage" | 14 |
| E6 | Error | Mark notice as failed | Add to failed list with "Unexpected error" | 14 |
| End | End | End PLUS Integration | Response sent to PLUS | - |

#### 4.2.3 Decision Logic

| ID | Decision | Input | Condition | Yes Action | No Action |
| --- | --- | --- | --- | --- | --- |
| D1 | Request valid? | Request payload | All required fields present | Continue | Error E1 |
| D2 | Source = '005'? | source field | = '005' | Continue | Error E2 |
| D3 | Stage transition allowed? | stage_map query | Transition rule exists | Continue | Error E3 |
| D4 | VON exists? | Query result | VON found | Continue | Error E4 |
| D5 | Is in Court Stage? | current_processing_stage | NOT IN Allowed Stages | Error E5 | Continue |
| D6 | Change stage success? | Sub-flow result | Success returned | Add to success | Error E6 |
| D7 | Last notice? | Loop counter | current == total | Check any success | Continue |
| D8 | Any success? | noticeCount | > 0 | Generate report | Return failure |

#### 4.2.4 API Specification Reference

**API:** plusChangeProcessingStage
**Endpoint:** `/ocms/v1/external/plus/change-processing-stage`
**Method:** POST
**Reference:** plan_api.md Section 2.1

---

## 5. Section 3: Toppan Integration

### 5.1 Tab 9: Section_3_Toppan_Integration

#### 5.1.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | Toppan Stage Update (Internal Cron) |
| Section | 3.1 |
| Trigger | generate_toppan_letters cron job completes |
| Frequency | Daily at 00:30 (0030hr) |
| Cron Expression | `0 30 0 * * ?` |
| ShedLock Name | generate_toppan_letters |
| Systems Involved | Toppan Cron, OCMS Backend, Database |
| Expected Outcome | VON stages updated automatically or skipped if manual change exists |

#### 5.1.2 Process Steps Table

| Step | Type | Definition | Brief Description | Next Step |
| --- | --- | --- | --- | --- |
| 1 | Start | Start Toppan Cron | Cron job generates Toppan files and calls update API | 2 |
| 2 | Process | Validate request | Check noticeNumbers, currentStage, processingDate (INT-001 to INT-003) | 3 |
| 3 | Decision | Request valid? | Check if request format passes validation | Yes→4, No→E1 |
| 4 | Process | Initialize processing | Start processing each notice in noticeNumbers array | 5 |
| 5 | Process | Query VON by notice_no | Query ocms_valid_offence_notice | 6 |
| 6 | Decision | VON exists? | Check if query returns record | Yes→7, No→E2 |
| 7 | Process | Check manual change record | Query ocms_change_of_processing for today's manual change | 8 |
| 8 | Decision | Manual record exists? | Check if manual change record exists for today | Yes→9, No→11 |
| 9 | Process | Skip VON update | Count as manual update (already updated manually) | 10 |
| 10 | Decision | Last notice? | Check if current notice is last in array | Yes→15, No→5 |
| 11 | Process | Validate stage transition | Check if currentStage → next stage is allowed | 12 |
| 12 | Decision | Stage transition allowed? | Check if transition rule exists | Yes→13, No→E3 |
| 13 | Process | Update VON processing stage | UPDATE ocms_valid_offence_notice automatically | 14 |
| 14 | Process | Count as automatic update | Increment automaticUpdates counter | 10 |
| 15 | Process | Build response summary | Build response with totalNotices, automaticUpdates, manualUpdates, skipped, errors | 16 |
| 16 | Process | Return response | Return summary with success = true/false | End |
| E1 | Error | Add to errors list | Add "Invalid request format" to errors | 15 |
| E2 | Error | Skip notice | Count as skipped, add "VON not found" to errors | 10 |
| E3 | Error | Skip notice | Count as skipped, add "Stage transition not allowed" to errors | 10 |
| End | End | End Toppan Integration | Cron job completes | - |

#### 5.1.3 Decision Logic

| ID | Decision | Input | Condition | Yes Action | No Action |
| --- | --- | --- | --- | --- | --- |
| D1 | Request valid? | Request payload | All required fields present | Continue | Error E1 |
| D2 | VON exists? | Query result | VON found | Continue | Error E2 |
| D3 | Manual record exists? | Query result | Manual change record found for today | Skip VON update | Continue |
| D4 | Stage transition allowed? | stage_map query | Transition rule exists | Update VON | Error E3 |
| D5 | Last notice? | Loop counter | current == total | Build response | Continue |

#### 5.1.4 Database Operations

| Step | Operation | Database | Table | Query/Action |
| --- | --- | --- | --- | --- |
| 5 | SELECT | Intranet | ocms_valid_offence_notice | Query by notice_no |
| 7 | SELECT | Intranet | ocms_change_of_processing | Query by notice_no and today's date_of_change |
| 13 | UPDATE | Intranet | ocms_valid_offence_notice | Update prev_processing_stage, prev_processing_date, last_processing_stage, last_processing_date, next_processing_stage, next_processing_date, amount_payable (for automatic only) |

#### 5.1.5 API Specification Reference

**API:** updateToppanStages
**Endpoint:** `/ocms/v1/internal/toppan/update-stages`
**Method:** POST
**Reference:** plan_api.md Section 3.1

---

## 6. Error Handling

### 6.1 Frontend Errors

| Error Code | Error Point | Condition | Error Message |
| --- | --- | --- | --- |
| FE-001 | Search form | Invalid Notice No format | Invalid Notice no. |
| FE-002 | Search form | Invalid ID No format | Invalid ID no. |
| FE-003 | Search form | Invalid Vehicle No format | Number does not match the local vehicle number format. |
| FE-004 | Search form | No search criteria | Please enter at least one search criterion |
| FE-010 | Change form | New Processing Stage missing | New Processing Stage is required |
| FE-011 | Change form | Reason of Change missing | Reason of Change is required |
| FE-012 | Change form | Remarks missing when reason=OTH | Remarks are mandatory when reason for change is 'OTH' (Others) |
| FE-013 | Change form | No notice selected | Please select at least one notice |
| FE-031 | Portal validation | All notices non-changeable | All selected notices are not eligible for stage change |
| FE-032 | Portal validation | Mixed changeable/non-changeable | Some notices are not eligible. Please uncheck inapplicable notices |

### 6.2 Backend Errors

| Error Code | Error Point | Condition | Error Message |
| --- | --- | --- | --- |
| OCMS.CPS.INVALID_FORMAT | Request validation | Items list empty | Items list cannot be empty |
| OCMS.CPS.MISSING_DATA | Request validation | Required field missing | noticeNo is required |
| OCMS.CPS.ELIG.NOT_FOUND | Eligibility | Notice not found | Notice not found |
| OCMS.CPS.ELIG.COURT_STAGE | Eligibility | Notice in court stage (NOT in Reminder Stages) | Notice is in court stage |
| OCMS.CPS.ELIG.PS_BLOCKED | Eligibility | Permanent suspension active | Notice has permanent suspension |
| OCMS.CPS.ELIG.TS_BLOCKED | Eligibility | Temporary suspension active | Notice has temporary suspension |
| OCMS.CPS.ELIG.EXISTING_CHANGE_TODAY | Eligibility | Duplicate change record | Existing change record found for this notice today |
| OCMS.CPS.ELIG.ROLE_CONFLICT | Eligibility | Cannot determine offender role | Cannot determine offender role |
| OCMS.CPS.ELIG.NO_STAGE_RULE | Eligibility | Cannot derive next stage | Cannot derive next stage |
| OCMS.CPS.ELIG.INELIGIBLE_STAGE | Eligibility | Stage not eligible for role | Stage not eligible for offender type |
| OCMS.CPS.REMARKS_REQUIRED | Business logic | Remarks missing for OTH | Remarks are mandatory when reason for change is 'OTH' (Others) |
| OCMS-4000 | External API | Bad request | Invalid request / data |
| OCMS-4004 | External API | CFC not allowed from PLUS | CFC not allowed from PLUS source |
| OCMS-5000 | System error | Internal server error | Something went wrong. Please try again later. |

---

## 7. Flowchart Checklist

Before creating the technical flowchart:

- [x] All steps have clear names
- [x] All decision points have Yes/No paths defined
- [x] All paths lead to an End point
- [x] Error handling paths are included with error codes
- [x] Database operations are identified
- [x] Swimlanes are defined for each system/tier
- [x] Color coding is specified
- [x] Step descriptions are complete
- [x] External system calls are documented
- [x] API specifications referenced
- [x] Audit user configuration noted (ocmsiz_app_conn, NOT "SYSTEM")
- [x] Insert/Update order documented

---

## 8. Notes for Technical Flowchart Creation

### 8.1 Shape Guidelines

| Element | Shape | Style |
| --- | --- | --- |
| Start/End | Terminator (rounded rectangle) | strokeWidth=2 |
| Process | Rectangle | rounded=1, arcSize=14 |
| Decision | Diamond | shape=mxgraph.flowchart.decision |
| Database | Cylinder | shape=cylinder |
| API Call | Rectangle with double border | shape=process |
| Error | Rectangle (red border) | strokeColor=#b85450 |

### 8.2 Connector Guidelines

| Connection Type | Style |
| --- | --- |
| Normal flow | Solid arrow |
| Database operation | Dashed line |
| API call | Dashed line |
| Error path | Red solid arrow |
| Return path | Dotted line |

### 8.3 Swimlane Colors

| System | Hex Color | Use |
| --- | --- | --- |
| OCMS Staff Portal | #dae8fc | Frontend UI |
| OCMS Backend API | #d5e8d4 | Backend processing |
| Database | #fff2cc | Database operations |
| PLUS Staff Portal | #e1d5e7 | External system |
| Toppan Cron | #ffe6cc | Batch job |

---

## 9. Changelog

| Version | Date | Author | Changes |
| --- | --- | --- | --- |
| v1.0 | 21/01/2026 | Claude | Initial version based on plan_api.md and plan_condition.md |
| v1.1 | 22/01/2026 | Claude | Updated based on code analysis and FD review: Fixed court stages definition (NOT in Reminder Stages), added prev_processing fields, fixed mha_dh_check_allow field name, added Toppan schedule details, updated error codes, added ENA restriction |
| v1.2 | 22/01/2026 | Claude | Removed specific court stage codes (CRT, CS1, CS2). Court stage check now uses: NOT IN Allowed Stages (NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3, CPC, CFC). Note: CPC and CFC are court stages but still allowed. |
| v1.3 | 22/01/2026 | Claude | Updated per Data Dictionary: Changed cre_dtm to date_of_change for duplicate check query, added source field to INSERT columns |

---

## 10. VON Update Pattern (Universal)

When updating VON processing stage, the following pattern is applied:

```
prev_processing_stage = old last_processing_stage
prev_processing_date = old last_processing_date
last_processing_stage = old next_processing_stage (or newStage if specified)
last_processing_date = current timestamp
next_processing_stage = NEXT_STAGE_{newStage} from parameter table
next_processing_date = current timestamp + STAGEDAYS from parameter table
```

### 10.1 Manual vs Automatic Updates

| Source | Type | Amount Payable |
| --- | --- | --- |
| OCMS (Staff Portal) | Manual | Calculated based on matrix |
| PLUS (PLUS Portal) | Manual | Calculated based on matrix |
| SYSTEM (Toppan Cron) | Automatic | Calculated based on matrix |
| AVSS | Manual | Calculated based on matrix |

**Note:** For Toppan cron, if a manual change record exists (source=OCMS or PLUS), the VON update is skipped (counted as manual update).

---

**End of Flowchart Plan**

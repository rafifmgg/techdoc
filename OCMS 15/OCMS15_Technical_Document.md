# OCMS 15 – Manage Change Processing Stage

**Prepared by**

NCS Pte Ltd

---

<!--
IMPORTANT: If information already exists in the Functional Document (FD),
refer to FD instead of duplicating content.
-->

---

## Version History

| Version | Updated By | Date | Changes |
| --- | --- | --- | --- |
| v1.0 | Claude | 21/01/2026 | Document Initiation |
| v1.1 | Claude | 22/01/2026 | Updated based on revised flowchart structure (9 tabs) |
| v1.2 | Claude | 22/01/2026 | Review fixes - Yi Jie standards compliance (0 critical, 3 major, 8 minor issues resolved) |
| v1.3 | Claude | 22/01/2026 | Aligned with plan files v1.3, drawio, and Data Dictionary: Fixed court stages definition, id_no length, DH/MHA field name, VON field names, Toppan query field, added prev_processing fields, ENA restrictions, source field in INSERT |
| v1.4 | Claude | 26/01/2026 | Restructured to follow FD section structure: Updated Use Case to follow FD, removed invalid PS stages, added Court & PS Check Common Function, added Owner/Hirer pop-up for RD1/RD2/RR3, added sections 1.6/1.7/1.8 for UI displays and reports, moved Toppan to 1.5.4, removed Section 4 Implementation Guidance, simplified flow description tables (removed separate Error Paths tables) |

---

## Table of Content

| Section | Content | Pages |
| --- | --- | --- |
| 1 | Function: Manual Change Processing Stage | 1 |
| 1.1 | Use Case | 1 |
| 1.2 | High-Level Process Flow | 2 |
| 1.3 | Search Notices for Manual Change Processing Stage | 3 |
| 1.3.1 | API Specification | 5 |
| 1.3.2 | Data Mapping | 6 |
| 1.3.3 | Success Outcome | 7 |
| 1.3.4 | Error Handling | 7 |
| 1.4 | Submission of the Notice Change Processing Stage | 8 |
| 1.4.1 | Portal Validation for New Processing Stage | 8 |
| 1.4.2 | Success Outcome | 9 |
| 1.4.3 | Error Handling | 9 |
| 1.5 | Backend Processing for Change Notice Processing Stage | 11 |
| 1.5.1 | Backend Processing for Manual Change Processing Stage | 11 |
| 1.5.1.1 | Change Notice's Next Processing Stage Sub-flow | 17 |
| 1.5.1.2 | Data Update for Change Processing Stage | 18 |
| 1.5.1.3 | Handling Amount Payable for Change Processing Stage | 19 |
| 1.5.1.4 | Generate Change Notice Processing Report Sub-flow | 20 |
| 1.5.2 | Handling for Manual Stage Change in Generate_toppan_letters Cron | 29 |
| 1.6 | OCMS Staff Portal Display for Successful Change Processing Stage | 33 |
| 1.7 | OCMS Staff Portal Display for Failed Change Processing Stage | 33 |
| 1.8 | Accessing Change Notice Processing Reports | 34 |
| 2 | Function: Manual Change Processing Stage initiated by PLUS | 35 |
| 2.1 | Use Case | 35 |
| 2.2 | Manual Change Processing Stage via PLUS Processing Flow | 36 |
| 2.3 | Integration with PLUS | 37 |
| 2.3.1 | API Specification | 38 |
| 2.3.2 | Data Mapping | 39 |
| 2.3.3 | Success Outcome | 40 |
| 2.3.4 | Error Handling | 40 |

---

# Section 1 – Function: Manual Change Processing Stage

## 1.1 Use Case

1. The Change Processing Stage module allows OICs to manually change a notice's processing stage, which in turn changes its workflow.

2. The OCMS Staff Portal has a Change Notice Processing Stage module that allows OCMS OICs to:<br>a. Search for notice(s) to be updated with a new processing stage<br>b. Display the search results which are applicable to change the new processing stage on screen<br>c. Change the new processing stage by selecting the notice and completing the new processing stage form<br>d. Perform a validity check for the new processing stage is allowed to change before sending the change request to the OCMS Backend<br>e. Retrieve and download the Change Notice Processing Stage Data Report from the backend to view the Notices that manually change the new processing stage

3. When the Change Processing Stage is initiated from the Portal, the OCMS Backend will:<br>a. Validate the request and update the previous processing stage and date, last processing stage and date, next processing stage and date of the notice in the VON table. Update the previous processing stage with the value stored in the last processing stage, update the last processing stage and the next processing stage with the new processing stage value<br>b. Update the amount payable, administration fees immediately<br>c. Generate the report for internal tracking<br>d. Return the updated notice details (with the updated last processing stage and next processing stage) together with the report link to the OCMS Staff Portal

---

## 1.2 High-Level Process Flow

![High Level Flow](./images/section1-highlevel.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | Entry point | OIC initiates change processing stage module |
| Launch Module | User action | OIC launches Change Processing Stage module and enters search criteria |
| Search Notices | Search and filter | Backend searches notices and segregates eligible vs ineligible notices |
| Display Results | UI display | Portal displays search results with notice details and checkboxes |
| Fill Form | User input | OIC selects notices and fills change processing stage form |
| Portal Validation | Frontend validation | Portal validates new processing stage eligibility based on offender type |
| Submit to Backend | API call | Portal sends change request to backend API |
| Backend Processing | Backend action | Backend validates, updates database, and generates report |
| Return Response | API response | Backend returns status summary and report URL |
| Display Result | UI display | Portal displays status and auto-downloads report |
| End | Exit point | Process complete |

---

## 1.3 Search Notices for Manual Change Processing Stage

![Search Notices Flow](./images/section1-search-notices.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | Start search process | OIC launches Change Processing Stage module |
| Enter Criteria | User input | OIC enters at least one search criterion: Notice No, ID No, Vehicle No, Current Stage, or Date |
| Portal Validation | Frontend validation | Portal validates format (FE-001 to FE-005): Notice No format, ID No format, Vehicle No format warning |
| Validation Check | Decision point | Check if entered data passes format validation |
| Send Request | API call | Portal calls searchChangeProcessingStage API with search criteria |
| Query Database | Database operation | If ID No: Query ONOD first, then join VON. Otherwise: Query VON directly by search filters |
| Record Found Check | Decision point | Check if query returns any result |
| Send to Court & PS Check | API call | Backend sends list of Notice Numbers to the Court & PS Check Common Function |
| Court & PS Check | Process | Common Function checks whether the Notices have existing and active Permanent Suspensions OR are currently in Court Stages (after CFC and CPC). Refer to OCMS 41 Section 4.6.2 |
| Check Eligibility | Process loop | Backend checks each notice for eligibility based on Court & PS Check results |
| Court/PS Check Result | Decision point | Check if notice is flagged as Court Stage or has active Permanent Suspension |
| Mark Ineligible | Process | Add to ineligibleNotices list with reason (Court Stage or Permanent Suspension) |
| Mark Eligible | Process | Add to eligibleNotices list |
| Last Record Check | Decision point | Check if current record is last in result set |
| Return Results | API response | Return segregated lists: eligibleNotices and ineligibleNotices with summary counts |
| Display Results | UI display | Portal displays notice list with details and checkboxes for selection |
| End | Exit point | Search process complete |

---

### 1.3.1 API Specification

#### API for eService

##### API searchChangeProcessingStage

| Field | Value |
| --- | --- |
| API Name | searchChangeProcessingStage |
| URL | UAT: https://parking2.uraz.gov.sg/ocms/v1/change-processing-stage/search PRD: https://parking.uraz.gov.sg/ocms/v1/change-processing-stage/search |
| Description | Search for notices based on criteria and return segregated lists of eligible vs ineligible notices |
| Method | POST |
| Header | `{ "Authorization": "Bearer [token]", "Content-Type": "application/json" }` |
| Payload | `{ "noticeNo": "441000001X", "idNo": "S1234567D", "vehicleNo": "SBA1234A", "currentProcessingStage": "DN1", "dateOfCurrentProcessingStage": "2025-01-20" }` |
| Response | `{ "appCode": "OCMS-2000", "message": "Success", "data": { "eligibleNotices": [ { "noticeNo": "441000001X", "offenceType": "SP", "offenceDateTime": "2025-01-01T10:30:00", "offenderName": "TAN AH KOW", "offenderId": "S1234567D", "vehicleNo": "SBA1234A", "currentProcessingStage": "DN1", "currentProcessingStageDate": "2025-01-15T09:00:00", "ownerDriverIndicator": "D" } ], "ineligibleNotices": [ { "noticeNo": "441000002X", "offenceType": "SP", "currentProcessingStage": "CFC", "reasonCode": "OCMS.CPS.COURT_STAGE", "reasonMessage": "Notice is in court stage" } ], "summary": { "total": 2, "eligible": 1, "ineligible": 1 } } }` |
| Response (Empty) | `{ "appCode": "OCMS-2000", "message": "Success", "data": { "eligibleNotices": [], "ineligibleNotices": [], "summary": { "total": 0, "eligible": 0, "ineligible": 0 } } }` |
| Response Failure | `{ "appCode": "OCMS-5000", "message": "Something went wrong on our end. Please try again later." }` |

**Request Rules:**
- At least one search criterion is required
- When searching by ID Number: Backend queries ONOD table first, then joins with VON table
- Other searches: Backend queries VON table directly

---

### 1.3.2 Data Mapping

#### Search Parameter Data Mapping

| Zone | Database Table | Field Name | Value |
| --- | --- | --- | --- |
| Intranet | ocms_valid_offence_notice | notice_no | 441000001X |
| Intranet | ocms_valid_offence_notice | vehicle_no | SBA1234A |
| Intranet | ocms_valid_offence_notice | last_processing_stage | DN1 |
| Intranet | ocms_valid_offence_notice | last_processing_date | 2025-01-20 |
| Intranet | ocms_offence_notice_owner_driver | id_no | S1234567D |

#### UI Data Mapping Search Results

| Zone | Database Table | Field Name | Value |
| --- | --- | --- | --- |
| Intranet | ocms_valid_offence_notice | notice_no | 441000001X |
| Intranet | ocms_valid_offence_notice | vehicle_no | SBA1234A |
| Intranet | ocms_valid_offence_notice | last_processing_stage | DN1 |
| Intranet | ocms_valid_offence_notice | last_processing_date | 2025-01-15T09:00:00 |
| Intranet | ocms_valid_offence_notice | offence_type | SP |
| Intranet | ocms_valid_offence_notice | offence_datetime | 2025-01-01T10:30:00 |
| Intranet | ocms_offence_notice_owner_driver | id_no | S1234567D |
| Intranet | ocms_offence_notice_owner_driver | offender_name | TAN AH KOW |
| Intranet | ocms_offence_notice_owner_driver | owner_driver_indicator | D |

---

### 1.3.3 Success Outcome

- Backend successfully queries database and retrieves matching notices
- Backend sends list of Notice Numbers to the Court & PS Check Common Function
- Court & PS Check Common Function returns results indicating which notices are in Court Stage or have active Permanent Suspension
- Notices are segregated into eligible and ineligible lists based on Court & PS Check results:
  - Notices in Court Stages (after CFC and CPC) are marked as ineligible
  - Notices with existing and active Permanent Suspensions are marked as ineligible
  - Other notices are marked as eligible
- Response includes summary with total count, eligible count, and ineligible count
- Portal displays search results with notice details and checkboxes for selection

---

### 1.3.4 Error Handling

#### Frontend Validation Errors

| Error Scenario | Validation Rule | Error Message | Action |
| --- | --- | --- | --- |
| Invalid Notice No Format | Alphanumeric, 10 chars including spaces | Invalid Notice no. | Display error, block submission |
| Invalid ID No Format | Alphanumeric, max 12 chars | Invalid ID no. | Display error, block submission |
| Invalid Vehicle No Format | Local vehicle number format check | Number does not match the local vehicle number format. | Display warning, allow proceed |
| No Search Criteria | At least one field must be filled | Please enter at least one search criterion | Display error, block submission |
| Invalid Date Format | Valid date format dd/MM/yyyy | Invalid date format | Display error, block submission |

#### Backend Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| No Record Found | Query returns zero results | Display "No record found" message to user |
| Database Connection Error | Unable to connect to database | Log error, return generic error message to user |
| Invalid Query Parameters | Query parameters fail validation | Return error message with details |

#### API Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| General Server Error | OCMS-5000 | Something went wrong. Please try again later. | Internal server error |
| Bad Request | OCMS-4000 | Invalid request. Please check and try again. | Invalid request format |

---

## 1.4 Submission of the Notice Change Processing Stage

### 1.4.1 Portal Validation for New Processing Stage

![Portal Validation Flow](./images/section1-portal-validation.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | Start validation | OIC has filled form and clicks submit |
| Required Fields Check | Frontend validation | Check New Processing Stage, Reason of Change, Remarks (if reason=OTH) are filled |
| Fields Valid Check | Decision point | Check if all mandatory fields are filled correctly |
| DH/MHA Check | Decision point | Check if Send for DH/MHA Check checkbox is unchecked |
| Prompt Confirmation | User prompt | Show "Notice will not go for DH/MHA check. Proceed?" confirmation dialog |
| User Confirms | Decision point | Check if user confirms to proceed without DH/MHA check |
| Initialize Loop | Process | Start validating each selected notice for offender type matching |
| Offender Type Check | Decision point | Check if owner_driver_indicator = 'D' (Driver) |
| Driver Stage Check | Decision point | For Driver: Check if newProcessingStage IN ('DN1', 'DN2', 'DR3') |
| Owner Type Check | Decision point | Check if owner_driver_indicator IN ('O', 'H', 'R') (Owner/Hirer/Director) |
| Owner Stage Check | Decision point | For Owner: Check if newProcessingStage IN ('ROV', 'RD1', 'RD2', 'RR3') |
| Owner/Hirer Selection | User prompt | If newProcessingStage IN ('RD1', 'RD2', 'RR3') AND notice has both owner and hirer offender details, display pop-up prompt allowing user to select owner or hirer |
| Mark Changeable | Process | Add notice to changeableNotices list |
| Mark Non-Changeable | Process | Add notice to nonChangeableNotices list with reason |
| Last Record Check | Decision point | Check if current notice is last in selection |
| Non-Changeable Check | Decision point | Check if nonChangeableNotices list is not empty |
| Display Summary | UI display | Show summary page for OIC review before backend submission |
| OIC Confirms | User action | OIC reviews summary and confirms submission |
| End | Exit point | Proceed to backend submission |

---

### 1.4.2 Success Outcome

- Portal validates all mandatory fields are filled
- If DH/MHA check is unchecked, user confirms to proceed
- Portal validates each selected notice's offender type matches the new processing stage
- If newProcessingStage is RD1/RD2/RR3 and notice has both owner and hirer offender details, user selects owner or hirer via pop-up prompt
- Changeable notices are segregated from non-changeable notices
- If all notices are changeable, portal displays summary page for OIC review
- OIC confirms submission and portal proceeds to backend API call

---

### 1.4.3 Error Handling

#### Frontend Validation Errors

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Missing New Processing Stage | New Processing Stage field is empty | Display "New Processing Stage is required" |
| Missing Reason of Change | Reason of Change field is empty | Display "Reason of Change is required" |
| Missing Remarks for OTH | Reason = OTH but Remarks is empty | Display "Remarks are mandatory when reason for change is 'OTH' (Others)" |
| No Notice Selected | No notice checkbox is selected | Display "Please select at least one notice" |
| DH/MHA Check Unchecked | Send for DH/MHA Check is unchecked | Prompt "Notice will not go for DH/MHA check. Do you want to proceed?" |
| Invalid Stage for Driver | Offender Type = DRIVER and newProcessingStage not in DN1/DN2/DR3 | Display "Stage not allowed for Driver offender type" |
| Invalid Stage for Owner | Offender Type = OWNER/HIRER/DIRECTOR and newProcessingStage not in ROV/RD1/RD2/RR3 | Display "Stage not allowed for Owner offender type" |
| Owner/Hirer Selection Required | newProcessingStage IN (RD1/RD2/RR3) AND notice has both owner and hirer | Display pop-up "Please select Owner or Hirer for this notice" |

---

## 1.5 Backend Processing for Change Notice Processing Stage

### 1.5.1 Backend Processing for Manual Change Processing Stage

![Backend Processing Flow](./images/section1-backend-processing.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | Start backend processing | Backend receives API request from Portal |
| Validate Request | Backend validation | Check items not empty, noticeNo present (BE-001, BE-002) |
| Request Valid Check | Decision point | Check if request format passes validation |
| Initialize Batch | Process loop | Start processing each item in request batch |
| Query VON | Database operation | Query ocms_valid_offence_notice by notice_no |
| VON Exists Check | Decision point | Check if query returns record |
| Check Duplicate | Database operation | Query ocms_change_of_processing by notice_no and today's date |
| Duplicate Exists Check | Decision point | Check if change record exists for today |
| Confirmation Check | Decision point | Check if isConfirmation flag = true (user confirmed to proceed) |
| Validate Transition | Database operation | Check ocms_stage_map for allowed stage transition |
| Transition Valid Check | Decision point | Check if current stage → new stage is valid in stage map |
| Call Change Stage Subflow | Subflow call | Execute sub-flow to update VON and insert change record |
| Subflow Success Check | Decision point | Check if sub-flow completed successfully |
| Mark Success | Process | Add to results with outcome = UPDATED, increment success counter |
| Mark Failed | Process | Add to results with outcome = FAILED, increment failed counter |
| Last Item Check | Decision point | Check if current item is last in batch |
| Any Success Check | Decision point | Check if summary.succeeded > 0 |
| Generate Report | Subflow call | Call report generation sub-flow |
| Upload to Azure | Process | Store report in Azure Blob Storage and get signed URL |
| Build Response | Process | Build response with summary, results, and report URL |
| Return Response | API response | Send SUCCESS/PARTIAL/FAILED response to Portal |
| End | Exit point | Backend processing complete |

---

#### 1.5.1.1 API Specification

#### API for eService

##### API changeProcessingStage

| Field | Value |
| --- | --- |
| API Name | changeProcessingStage |
| URL | UAT: https://parking2.uraz.gov.sg/ocms/v1/change-processing-stage PRD: https://parking.uraz.gov.sg/ocms/v1/change-processing-stage |
| Description | Submit batch change processing stage request |
| Method | POST |
| Header | `{ "Authorization": "Bearer [token]", "Content-Type": "application/json", "X-User-Id": "OIC001" }` |
| Payload | `{ "items": [ { "noticeNo": "441000001X", "newStage": "DN2", "reason": "SUP", "remark": "Resend reminder", "source": "PORTAL", "dhMhaCheck": false, "isConfirmation": false } ] }` |
| Response | `{ "appCode": "OCMS-2000", "message": "Success", "data": { "summary": { "requested": 1, "succeeded": 1, "failed": 0 }, "results": [ { "noticeNo": "441000001X", "outcome": "UPDATED", "previousStage": "DN1", "newStage": "DN2" } ], "report": { "url": "https://storage.blob.core.windows.net/reports/ChangeStageReport_20250126.xlsx" } } }` |
| Response (Partial) | `{ "appCode": "OCMS-2000", "message": "Partial Success", "data": { "summary": { "requested": 2, "succeeded": 1, "failed": 1 }, "results": [ { "noticeNo": "441000001X", "outcome": "UPDATED" }, { "noticeNo": "441000002X", "outcome": "FAILED", "errorCode": "OCMS.CPS.COURT_STAGE" } ] } }` |
| Response Failure | `{ "appCode": "OCMS-5000", "message": "Something went wrong on our end. Please try again later." }` |

**Request Rules:**
- items array cannot be empty
- noticeNo is required for each item
- newStage is optional (can be derived from current stage)
- dhMhaCheck defaults to false
- isConfirmation defaults to false (set true to override duplicate record warning)

---

#### 1.5.1.2 Data Update for Change Processing Stage

#### Database Data Mapping - UPDATE ocms_valid_offence_notice

| Zone | Database Table | Field Name | Value |
| --- | --- | --- | --- |
| Intranet | ocms_valid_offence_notice | notice_no | 441000001X |
| Intranet | ocms_valid_offence_notice | prev_processing_stage | DN1 |
| Intranet | ocms_valid_offence_notice | prev_processing_date | 2025-01-15T09:00:00 |
| Intranet | ocms_valid_offence_notice | last_processing_stage | DN2 |
| Intranet | ocms_valid_offence_notice | last_processing_date | 2025-01-26T10:30:00 |
| Intranet | ocms_valid_offence_notice | next_processing_stage | DR3 |
| Intranet | ocms_valid_offence_notice | next_processing_date | 2025-02-26T00:00:00 |
| Intranet | ocms_valid_offence_notice | amount_payable | 150.00 |
| Intranet | ocms_valid_offence_notice | upd_user_id | ocmsiz_app_conn |
| Intranet | ocms_valid_offence_notice | upd_dtm | System generated |

#### Database Data Mapping - INSERT ocms_change_of_processing

| Zone | Database Table | Field Name | Value |
| --- | --- | --- | --- |
| Intranet | ocms_change_of_processing | notice_no | 441000001X |
| Intranet | ocms_change_of_processing | date_of_change | 2025-01-26T10:30:00 |
| Intranet | ocms_change_of_processing | last_processing_stage | DN1 |
| Intranet | ocms_change_of_processing | new_processing_stage | DN2 |
| Intranet | ocms_change_of_processing | reason_of_change | SUP |
| Intranet | ocms_change_of_processing | authorised_officer | OIC001 |
| Intranet | ocms_change_of_processing | remarks | Resend reminder |
| Intranet | ocms_change_of_processing | source | PORTAL |
| Intranet | ocms_change_of_processing | cre_date | System generated |
| Intranet | ocms_change_of_processing | cre_user_id | ocmsiz_app_conn |

#### Database Data Mapping - UPDATE ocms_offence_notice_owner_driver (Conditional)

| Zone | Database Table | Field Name | Value |
| --- | --- | --- | --- |
| Intranet | ocms_offence_notice_owner_driver | notice_no | 441000001X |
| Intranet | ocms_offence_notice_owner_driver | mha_dh_check_allow | Y |
| Intranet | ocms_offence_notice_owner_driver | upd_user_id | ocmsiz_app_conn |
| Intranet | ocms_offence_notice_owner_driver | upd_dtm | System generated |

#### Database Data Mapping - Stage Validation

| Zone | Database Table | Field Name | Value |
| --- | --- | --- | --- |
| Intranet | ocms_stage_map | current_stage | DN1 |
| Intranet | ocms_stage_map | next_stage | DN2 |
| Intranet | ocms_stage_map | is_allowed | Y |

**Audit User Configuration:**

| Zone | Database Table | Field Name | Value |
| --- | --- | --- | --- |
| Intranet | All tables | cre_user_id | ocmsiz_app_conn |
| Intranet | All tables | upd_user_id | ocmsiz_app_conn |
| Internet | All tables | cre_user_id | ocmsez_app_conn |
| Internet | All tables | upd_user_id | ocmsez_app_conn |

**Note:** Do NOT use "SYSTEM" for audit user fields. Always use the database connection user.

---

#### 1.5.1.3 Success Outcome

- Backend validates request format successfully
- For each item in batch:
  - VON record is found in database
  - No duplicate change record exists for today, OR user confirmed to proceed with isConfirmation=true
  - Stage transition is valid according to ocms_stage_map
  - Change stage sub-flow completes successfully
  - Item is marked as SUCCESS with outcome = UPDATED
- At least one item succeeds in the batch
- Change Processing Stage Report is generated and uploaded to Azure Blob Storage
- Response is built with summary (total requested, succeeded, failed) and per-item results
- Response includes report URL with expiry time
- Portal receives response and displays status to user
- Report is auto-downloaded by Portal

---

#### 1.5.1.4 Error Handling

#### Backend Validation Errors

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Items list empty | Items array is empty or null | Return OCMS.CPS.INVALID_FORMAT, reject request |
| Notice No missing | noticeNo field is missing for an item | Return OCMS.CPS.MISSING_DATA, mark item as FAILED |
| VON not found | Query ocms_valid_offence_notice returns no record | Return OCMS.CPS.NOT_FOUND, mark item as FAILED |
| Court stage | Notice current_processing_stage is in court stages | Return OCMS.CPS.COURT_STAGE, mark item as FAILED |
| Duplicate record | Change record exists for notice today and isConfirmation=false | Return OCMS.CPS.DUPLICATE_RECORD, mark as FAILED |
| Invalid transition | Stage transition not allowed per ocms_stage_map | Return OCMS.CPS.INVALID_TRANSITION, mark item as FAILED |
| Remarks required | Reason = OTH but remarks is empty | Return OCMS.CPS.REMARKS_REQUIRED, mark item as FAILED |
| ENA stage restriction | Cannot change to ENA stage via OCMS Staff Portal | Return OCMS.CPS.ENA_NOT_ALLOWED, mark item as FAILED |
| Unexpected error | Unexpected error during processing | Return OCMS.CPS.UNEXPECTED, mark item as FAILED |

#### Duplicate Handling Logic

| Attempt | Condition | Action | Response |
| --- | --- | --- | --- |
| 1st Attempt | Duplicate record exists, isConfirmation=false | Return warning to user | OCMS.CPS.DUPLICATE_RECORD |
| 2nd Attempt | Duplicate record exists, isConfirmation=true | Proceed with update (user confirmed) | OCMS-2000 (Success) |

---

### 1.5.2 Change Notice's Next Processing Stage Sub-flow

![Change Stage Subflow](./images/section1-change-stage-subflow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | Start sub-flow | Receives noticeNo and newStage from main flow |
| Query VON | Database operation | Get current stage and payment data from VON |
| Calculate Stage Date | Business logic | Set next_processing_date based on new stage rules |
| Calculate Amount Payable | Business logic | Calculate amount based on new stage payment rules |
| Update VON | Database operation | UPDATE ocms_valid_offence_notice SET next_processing_stage, next_processing_date, amount_payable |
| VON Update Success Check | Decision point | Check if UPDATE succeeded (rows affected > 0) |
| Insert Change Record | Database operation | INSERT into ocms_change_of_processing with change details |
| Insert Success Check | Decision point | Check if INSERT succeeded (rows affected > 0) |
| DH/MHA Check Required | Decision point | Check if dhMhaCheck flag = true |
| Update ONOD DH/MHA Flag | Database operation | UPDATE ocms_offence_notice_owner_driver SET mha_dh_check_allow = 'Y' |
| ONOD Update Success Check | Decision point | Check if UPDATE succeeded (rows affected > 0) |
| Return Success | Subflow return | Return success to main flow |
| End | Exit point | Control returns to main flow |

---

#### 1.5.2.1 Data Mapping

#### Database Data Mapping - Step 1: UPDATE ocms_valid_offence_notice

| Zone | Database Table | Field Name | Value |
| --- | --- | --- | --- |
| Intranet | ocms_valid_offence_notice | prev_processing_stage | DN1 |
| Intranet | ocms_valid_offence_notice | prev_processing_date | 2025-01-15T09:00:00 |
| Intranet | ocms_valid_offence_notice | last_processing_stage | DN2 |
| Intranet | ocms_valid_offence_notice | last_processing_date | 2025-01-26T10:30:00 |
| Intranet | ocms_valid_offence_notice | next_processing_stage | DR3 |
| Intranet | ocms_valid_offence_notice | next_processing_date | 2025-02-26T00:00:00 |
| Intranet | ocms_valid_offence_notice | amount_payable | 150.00 |
| Intranet | ocms_valid_offence_notice | upd_user_id | ocmsiz_app_conn |
| Intranet | ocms_valid_offence_notice | upd_dtm | System generated |

#### Database Data Mapping - Step 2: INSERT ocms_change_of_processing

| Zone | Database Table | Field Name | Value |
| --- | --- | --- | --- |
| Intranet | ocms_change_of_processing | notice_no | 441000001X |
| Intranet | ocms_change_of_processing | date_of_change | 2025-01-26T10:30:00 |
| Intranet | ocms_change_of_processing | last_processing_stage | DN1 |
| Intranet | ocms_change_of_processing | new_processing_stage | DN2 |
| Intranet | ocms_change_of_processing | reason_of_change | SUP |
| Intranet | ocms_change_of_processing | authorised_officer | OIC001 |
| Intranet | ocms_change_of_processing | remarks | Resend reminder |
| Intranet | ocms_change_of_processing | source | PORTAL |
| Intranet | ocms_change_of_processing | cre_date | System generated |
| Intranet | ocms_change_of_processing | cre_user_id | ocmsiz_app_conn |

#### Database Data Mapping - Step 3: UPDATE ocms_offence_notice_owner_driver (Conditional)

| Zone | Database Table | Field Name | Value |
| --- | --- | --- | --- |
| Intranet | ocms_offence_notice_owner_driver | mha_dh_check_allow | Y |
| Intranet | ocms_offence_notice_owner_driver | upd_user_id | ocmsiz_app_conn |
| Intranet | ocms_offence_notice_owner_driver | upd_dtm | System generated |

**Note:** Step 3 update is only performed if dhMhaCheck flag = true in the request.

---

#### 1.5.2.2 Success Outcome

- Current VON record is successfully queried
- next_processing_date is calculated based on new stage rules
- amount_payable is calculated based on new stage payment rules
- VON record is successfully updated with new stage, date, and amount
- Change record is successfully inserted into ocms_change_of_processing
- If dhMhaCheck is true, ONOD record is successfully updated with mha_dh_check_allow = 'Y'
- Sub-flow returns success to main flow
- Main flow proceeds to generate report

---

#### 1.5.2.3 Error Handling

| Error Scenario | Definition | Action |
| --- | --- | --- |
| VON Update Failed | UPDATE operation returns 0 rows affected | Return error to main flow, mark item as FAILED |
| Insert Failed | INSERT operation fails or returns 0 rows affected | Return error to main flow, mark item as FAILED |
| ONOD Update Failed | UPDATE operation returns 0 rows affected (when dhMhaCheck=true) | Return error to main flow, mark item as FAILED |
| Database Connection Error | Unable to connect to database | Log error, return error to main flow, mark item as FAILED |

---

### 1.5.3 Generate Change Notice Processing Report Sub-flow

![Generate Report Flow](./images/section1-generate-report.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | Start report generation | Receives batch results from main flow |
| Prepare Data | Process | Collect notice details and results from successful items |
| Generate Excel | Process | Create Excel file with submitted notice details and outcomes |
| Excel Success Check | Decision point | Check if file created successfully |
| Generate Filename | Process | Format: ChangeStageReport_yyyyMMdd_HHmmss_[userID].xlsx |
| Upload to Azure | Process | Upload to Azure Blob Storage path: reports/change-stage/ |
| Upload Success Check | Decision point | Check if upload succeeded |
| Generate Signed URL | Process | Get temporary signed URL with expiry time |
| Return Report URL | Subflow return | Return signed URL to main flow |
| End | Exit point | Control returns to main flow |

---

#### 1.5.3.1 Data Mapping

#### Report Data Fields

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
| Message | message | API response |

#### Report Storage

| Attribute | Value |
| --- | --- |
| Storage Type | Azure Blob Storage |
| Storage Path | reports/change-stage/ |
| File Format | Excel (.xlsx) |
| Filename Pattern | ChangeStageReport_yyyyMMdd_HHmmss_[userID].xlsx |
| Access Method | Signed URL with expiry |
| URL Expiry | Configurable (e.g., 24 hours) |

#### Excel Report Structure

| Column | Header | Description |
| --- | --- | --- |
| A | Notice No | Notice number |
| B | Previous Stage | Processing stage before change |
| C | New Stage | Processing stage after change |
| D | Reason of Change | Reason code for change |
| E | Remarks | Additional remarks |
| F | Authorised Officer | Officer who initiated change |
| G | Date of Change | Date and time of change |
| H | Status | SUCCESS or FAILED |
| I | Message | Success message or error message |

---

#### 1.5.3.2 Success Outcome

- Report data is successfully prepared from batch results
- Excel file is successfully generated with notice details and outcomes
- Report filename is generated with timestamp and user ID
- Excel file is successfully uploaded to Azure Blob Storage
- Signed URL is successfully generated with expiry time
- Report URL is returned to main flow
- Main flow includes report URL in response to Portal
- Portal auto-downloads the report

---

#### 1.5.3.3 Error Handling

| Error Scenario | Definition | Action |
| --- | --- | --- |
| Excel Generation Failed | Unable to create Excel file | Log error, return error to main flow |
| Upload to Azure Failed | Azure Blob upload error | Log error, return error to main flow |
| Signed URL Generation Failed | Unable to generate signed URL | Log error, return error to main flow |
| No Successful Items | summary.succeeded = 0 | Skip report generation (no report to generate) |

---

## 1.6 OCMS Staff Portal Display for Successful Change Processing Stage

Refer to FD Section 2.6 for detailed UI specifications.

When a Change Processing Stage online form has been successfully submitted, the OCMS Staff Portal displays:

| Field | Description | Example |
| --- | --- | --- |
| Status | Success indicator | Success |
| User Message | User-friendly message | Change Processing Stage is successful |
| Notice Number | Notice number that was changed | 441000001X |
| Previous Processing Stage | Original stage before change | RR3 |
| New Processing Stage | Updated stage after change | RD1 |
| Reason of Change | Selected reason code | RSN |
| Remarks | User-entered remarks | Resend notice |
| Send for DH/MHA checkbox | DH/MHA check indicator | Y |
| Report Download | Auto-download link | Change_Processing_Stage_Report_20250122.xlsx |

---

## 1.7 OCMS Staff Portal Display for Failed Change Processing Stage

Refer to FD Section 2.7 for detailed UI specifications.

When a Change Processing Stage submission fails, the OCMS Staff Portal displays:

| Field | Description | Example |
| --- | --- | --- |
| Status | Failure indicator | Failed |
| User Message | Error message | Stage Change Failed |
| Error Reason | Specific failure reason | Notice is in Court Stage |
| Notice Number | Notice number that failed | 441000002X |
| Current Processing Stage | Current stage (unchanged) | CFC |

**Note:** Display successful Change Processing Stage notices if any successful records exist. Refer to Section 1.6 for the UI display.

---

## 1.8 Accessing Change Notice Processing Reports

Refer to FD Section 2.8 for detailed specifications.

The Stage Change Report can be retrieved in 2 ways:

### 1.8.1 Auto Download Upon Manual Stage Change Submission

When a Change Processing Stage online form has been submitted, the OCMS generates the report and returns the report URL so that the Staff Portal can download the report automatically.

### 1.8.2 Manual Download from Staff Portal Report Module

The Staff Portal Report module has a function where users can select the Change Processing Stage Report to view the reports in the system.

| Field | Description | Example | Remarks |
| --- | --- | --- | --- |
| Notice Number | Notice number | 441000001X | |
| Date of Report | Date of report generation | 22/03/2025 | |
| Report Generated By | Name of Officer | John Lee | |
| Action | Download button | Download | On click, downloads report |

---

# Section 2 – Function: Manual Change Processing Stage initiated by PLUS

## 2.1 Use Case

1. The Change Processing Stage refers to the scenario where PLUS triggers a request to OCMS to manually change a notice's processing stage, which in turn changes its workflow.

2. This occurs when PLUS officers need to change the current processing stage of a notice, and the change is carried out manually within the notice processing workflow.

3. The PLUS system will perform validation to decide whether a Notice is eligible for a stage change before the stage change request is sent to the OCMS Intranet Backend.

4. When the OCMS Intranet Backend receives a stage change request, it performs the following actions for change processing stage submission:<br>a. Validate the request and update the last and next_processing_stage of the notice in the VON table to this new stage<br>b. Generate the report for internal tracking<br>c. Return the notice details (with the updated last and next_processing_stage) together with the report link to the OCMS Staff Portal

Note: Since PLUS is not allowed to change the stage to CFC and both OCMS and PLUS are also not allowed to change the stage to ENA, these options should not be displayed in the dropdown list of New Processing Stage.

---

## 2.2 High Level Flow

![PLUS High Level Flow](./images/section2-highlevel.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | Entry point | PLUS officer initiates change processing stage |
| PLUS Initiates | User action | PLUS officer selects notice(s) and new stage in PLUS Portal |
| PLUS Validates | System validation | PLUS system checks eligibility based on pre-defined rules |
| PLUS Sends Request | API call | PLUS sends request to OCMS via APIM with source="005" |
| OCMS Receives | API gateway | OCMS Backend receives request routed through APIM |
| Backend Processing | Backend action | OCMS validates request, checks eligibility, updates VON, generates report |
| Return Response | API response | OCMS returns status (SUCCESS/FAILED) with notice details or error |
| PLUS Displays Result | UI display | PLUS shows result to officer |
| End | Exit point | Process complete |

---

## 2.3 PLUS API Integration

![PLUS Integration Flow](./images/section2-plus-integration.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | Start PLUS integration | OCMS receives request from PLUS via APIM |
| Validate Request | Backend validation | Check noticeNo array, lastStageName, nextStageName (EXT-001 to EXT-003) |
| Request Valid Check | Decision point | Check if request format passes validation |
| Source Code Check | Decision point | Check if source = "005" (PLUS identifier) |
| Validate Transition | Database operation | Query ocms_stage_map for lastStageName → nextStageName |
| Transition Valid Check | Decision point | Check if transition rule exists |
| Initialize Batch | Process loop | Start processing each notice in noticeNo array |
| Query VON | Database operation | Query ocms_valid_offence_notice by notice_no |
| VON Exists Check | Decision point | Check if query returns record |
| Court Stage Check | Decision point | Check if current_processing_stage NOT IN Allowed Stages (NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3, CPC, CFC) |
| Call Change Stage Subflow | Subflow call | Execute sub-flow to update VON and insert change record |
| Subflow Success Check | Decision point | Check if sub-flow completed successfully |
| Add to Success List | Process | Increment noticeCount for successful changes |
| Last Notice Check | Decision point | Check if current notice is last in array |
| Any Success Check | Decision point | Check if noticeCount > 0 |
| Generate Report | Subflow call | Call report generation sub-flow |
| Return Success | API response | Return status=SUCCESS, noticeCount, message |
| Return Failure | API response | Return status=FAILED, error code, message |
| End | Exit point | Response sent to PLUS |

---

### 2.3.1 API Specification

#### External API for PLUS Integration

##### API plusChangeProcessingStage

| Field | Value |
| --- | --- |
| API Name | plusChangeProcessingStage |
| URL | UAT: https://parking2.uraz.gov.sg/ocms/v1/external/plus/change-processing-stage PRD: https://parking.uraz.gov.sg/ocms/v1/external/plus/change-processing-stage |
| Description | PLUS Staff Portal requests to change notice processing stage |
| Method | POST |
| Header | `{ "Content-Type": "application/json", "Ocp-Apim-Subscription-Key": "[APIM secret value]" }` |
| Payload | `{ "noticeNo": ["441000001X", "441000002X"], "lastStageName": "DN1", "nextStageName": "DN2", "lastStageDate": "2025-01-15T09:00:00", "newStageDate": "2025-01-26T10:30:00", "offenceType": "D", "source": "005" }` |
| Response | `{ "status": "0", "message": "Stage change processed successfully", "noticeCount": 2 }` |
| Response Failure | `{ "status": "1", "errorCode": "OCMS-4000", "errorMsg": "Stage transition not allowed: DN1 -> CFC" }` |

**Request Rules:**
- noticeNo is array of notice numbers
- Stage transition must be allowed according to stage_map
- source must be "005" to identify PLUS origin
- PLUS is not allowed to change stage to CFC

---

### 2.3.2 Data Mapping

#### Database Data Mapping - PLUS Integration

| Zone | Database Table | Field Name | Value |
| --- | --- | --- | --- |
| Intranet | ocms_valid_offence_notice | notice_no | 441000001X |
| Intranet | ocms_valid_offence_notice | last_processing_stage | DN1 |
| Intranet | ocms_valid_offence_notice | next_processing_stage | DN2 |
| Intranet | ocms_valid_offence_notice | last_processing_date | 2025-01-15T09:00:00 |
| Intranet | ocms_valid_offence_notice | next_processing_date | 2025-01-26T10:30:00 |
| Intranet | ocms_valid_offence_notice | offence_type | D |
| Intranet | ocms_change_of_processing | source | 005 |
| Intranet | ocms_change_of_processing | reason_of_change | SUP |
| Intranet | ocms_stage_map | current_stage | DN1 |
| Intranet | ocms_stage_map | next_stage | DN2 |

---

### 2.3.3 Success Outcome

- OCMS Backend receives request from PLUS via APIM
- Request format is valid (noticeNo array, lastStageName, nextStageName present)
- Source code is "005" (PLUS identifier)
- Stage transition is valid according to ocms_stage_map
- For each notice in noticeNo array:
  - VON record is found in database
  - Notice is not in court stage
  - Change stage sub-flow completes successfully
  - noticeCount is incremented
- Change Processing Stage Report is generated
- Response is returned to PLUS with status=SUCCESS and noticeCount
- PLUS displays result to officer

---

### 2.3.4 Error Handling

#### PLUS API Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Notice numbers missing | noticeNo array is empty | Return OCMS-4000 "Notice numbers are required" |
| Last stage name missing | lastStageName field is empty | Return OCMS-4000 "Last stage name is required" |
| Next stage name missing | nextStageName field is empty | Return OCMS-4000 "Next stage name is required" |
| Stage transition not allowed | Transition not valid per ocms_stage_map | Return OCMS-4000 "Stage transition not allowed: [last] -> [next]" |
| Invalid source code | Source code is not "005" | Return OCMS-4000 "Invalid source code" |
| ENA stage restriction | Cannot change to ENA stage via PLUS | Return OCMS-4000 "ENA stage not allowed for PLUS" |
| All notices in court stage | All notices are in court stage | Return OCMS-4000 with list of notices |
| Mixed valid/invalid notices | Some notices valid, some invalid | Return partial success with counts |
| Unexpected system error | Unexpected error during processing | Return INTERNAL_ERROR with error details |

---

### 1.5.4 Handling for Manual Stage Change in Generate_toppan_letters Cron

Refer to FD Section 2.5.2 for detailed specifications.

#### 1.5.4.1 Overview (5W1H)

| Question | Description |
| --- | --- |
| **WHAT** | Toppan Stage Update - An internal function that updates VON processing stages after Toppan letter files are generated. The cron job differentiates between manual vs automatic stage changes and skips VON updates if a manual change record already exists. |
| **WHY** | After the generate_toppan_letters cron job generates Toppan files for sending physical letters, the system needs to update the notice processing stages to reflect that letters have been generated. However, if an OIC manually changed the stage earlier on the same day, the automatic update should be skipped to avoid overwriting the manual change. |
| **WHERE** | Internal cron job (generate_toppan_letters) and OCMS Backend (Intranet). Uses database tables: ocms_valid_offence_notice and ocms_change_of_processing. |
| **WHEN** | Triggered daily after the generate_toppan_letters cron job completes Toppan file generation. Typically runs at a fixed time each day (e.g., after midnight). |
| **WHO** | System Actors: Toppan Cron Job, OCMS Backend API, Database. No user interaction - fully automated internal process. |
| **HOW** | Cron completes Toppan generation → Calls internal API → For each notice: Check if manual change exists today → If yes: Skip VON update (manual) → If no: Update VON automatically → Return summary (total, automatic, manual, skipped, errors) |

**CRON Job Specification**:
| Attribute | Value |
|-----------|-------|
| CRON Name | Toppan Stage Update |
| Shedlock Name | `update_toppan_stages` |
| Naming Convention | [action]_[subject] |
| Lock Duration | 30 minutes (max) |
| Start Time Recording | Record immediately when cron begins for stuck job detection |

#### 1.5.4.2 Use Case Description

1. The Toppan Stage Update is an internal function within OCMS that handles automatic processing stage updates after Toppan letter generation.

2. The generate_toppan_letters cron job runs daily and performs the following:<br>a. Generate Toppan letter files for notices that require physical letter sending<br>b. Call updateToppanStages internal API with list of notice numbers<br>c. Receive summary of automatic vs manual updates

3. The OCMS Backend processes the Toppan stage update request:<br>a. Validate request format (noticeNumbers, currentStage, processingDate required)<br>b. For each notice: Check if manual change record exists for today in ocms_change_of_processing<br>c. If manual record exists: Skip VON update, count as manual update<br>d. If no manual record: Update VON processing stage automatically, count as automatic update<br>e. If VON not found or stage transition not allowed: Skip notice, add to errors list<br>f. Return summary: totalNotices, automaticUpdates, manualUpdates, skipped, errors

4. This approach ensures that manual stage changes take precedence over automatic updates, preventing data conflicts.

---

#### 1.5.4.3 Toppan Stage Update Flow

![Toppan Integration Flow](./images/section3-toppan-integration.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | Start Toppan cron | Cron job generates Toppan files and calls update API |
| Validate Request | Backend validation | Check noticeNumbers, currentStage, processingDate (INT-001 to INT-003) |
| Request Valid Check | Decision point | Check if request format passes validation |
| Initialize Processing | Process loop | Start processing each notice in noticeNumbers array |
| Query VON | Database operation | Query ocms_valid_offence_notice by notice_no |
| VON Exists Check | Decision point | Check if query returns record |
| Check Manual Record | Database operation | Query ocms_change_of_processing for today's manual change by notice_no and date_of_change |
| Manual Record Exists Check | Decision point | Check if manual change record exists for today |
| Skip VON Update | Process | Count as manual update (already updated manually by OIC) |
| Validate Transition | Database operation | Check if currentStage → next stage is allowed in stage_map |
| Transition Valid Check | Decision point | Check if transition rule exists |
| Update VON Automatically | Database operation | UPDATE ocms_valid_offence_notice processing stage |
| Count Automatic Update | Process | Increment automaticUpdates counter |
| Last Notice Check | Decision point | Check if current notice is last in array |
| Build Response | Process | Build response with totalNotices, automaticUpdates, manualUpdates, skipped, errors |
| Return Response | API response | Return summary with success = true/false |
| End | Exit point | Cron job completes |

---

##### 1.5.4.4 API Specification

#### Internal API for Toppan Cron

##### API updateToppanStages

| Field | Value |
| --- | --- |
| API Name | updateToppanStages |
| URL | UAT: https://parking2.uraz.gov.sg/ocms/v1/internal/toppan/update-stages PRD: https://parking.uraz.gov.sg/ocms/v1/internal/toppan/update-stages |
| Description | Called by generate_toppan_letters cron job to update VON processing stages after Toppan files are generated |
| Method | POST |
| Header | `{ "Authorization": "Bearer [internal-token]", "Content-Type": "application/json" }` |
| Payload | `{ "noticeNumbers": ["441000001X", "441000002X", "441000003X"], "currentStage": "DN1", "processingDate": "2025-01-26T00:30:00" }` |
| Response | `{ "appCode": "OCMS-2000", "message": "Success", "data": { "totalNotices": 3, "automaticUpdates": 2, "manualUpdates": 1, "skipped": 0 } }` |
| Response Failure | `{ "appCode": "OCMS-5000", "message": "Something went wrong on our end. Please try again later." }` |

**Request Rules:**
- noticeNumbers array cannot be empty
- currentStage is required
- processingDate must be valid datetime format

---

##### 1.5.4.5 Data Mapping

#### Database Data Mapping - Toppan Cron UPDATE ocms_valid_offence_notice

| Zone | Database Table | Field Name | Value |
| --- | --- | --- | --- |
| Intranet | ocms_valid_offence_notice | notice_no | 441000001X |
| Intranet | ocms_valid_offence_notice | last_processing_stage | DN1 |
| Intranet | ocms_valid_offence_notice | last_processing_date | 2025-01-26T00:30:00 |
| Intranet | ocms_valid_offence_notice | next_processing_stage | DN2 |
| Intranet | ocms_valid_offence_notice | upd_user_id | ocmsiz_app_conn |
| Intranet | ocms_valid_offence_notice | upd_dtm | System generated |

#### Database Data Mapping - Manual Change Check Query

| Zone | Database Table | Field Name | Value |
| --- | --- | --- | --- |
| Intranet | ocms_change_of_processing | notice_no | 441000001X |
| Intranet | ocms_change_of_processing | date_of_change | 2025-01-26 |

**Query Logic:**
- If record exists for today: Skip VON update (manual update already done)
- If no record: Proceed with automatic VON update

---

##### 1.5.4.6 Success Outcome

- Toppan cron job completes Toppan file generation
- Cron calls updateToppanStages internal API with notice list
- Request format is valid (noticeNumbers, currentStage, processingDate present)
- For each notice in noticeNumbers array:
  - VON record is found in database
  - Manual change record check is performed for today
  - If manual record exists: VON update is skipped, counted as manual update
  - If no manual record: VON is updated automatically, counted as automatic update
  - If error occurs: Notice is skipped, added to errors list
- Response summary is built with counts
- Cron job receives summary and completes
- System logs summary for audit trail

---

##### 1.5.4.7 Error Handling

#### Toppan API Error Handling

| Error Scenario | Condition | Action | Count As |
| --- | --- | --- | --- |
| Invalid Request Format | Required fields missing | Add to errors list, return success=false | - |
| VON Not Found | Query returns no record | Skip notice, add "VON not found" to errors | Skipped |
| Stage Transition Not Allowed | Transition not in stage_map | Skip notice, add "Stage transition not allowed" to errors | Skipped |
| Database Connection Error | Unable to connect to database | Add to errors list, return success=false | - |

#### Manual vs Automatic Update Logic

| Scenario | Condition | Action | Count As |
| --- | --- | --- | --- |
| Manual Record Exists | ocms_change_of_processing has record for notice today | Skip VON update (OIC already changed it manually) | Manual Update |
| No Manual Record | No record in ocms_change_of_processing for today | Update VON automatically | Automatic Update |
| VON Not Found | VON query returns no record | Skip notice | Skipped |
| Stage Transition Invalid | Stage transition not allowed per stage_map | Skip notice | Skipped |

**Response Summary Fields:**

| Field | Description |
| --- | --- |
| totalNotices | Total number of notices in request |
| automaticUpdates | Count of notices updated automatically (no manual change today) |
| manualUpdates | Count of notices skipped because manual change exists today |
| skipped | Count of notices skipped due to errors (VON not found, invalid transition) |
| errors | Array of error messages (null if no errors) |
| success | true if processing completed without critical errors, false otherwise |

---

**End of OCMS 15 Technical Document**

# OCMS 15 – Manage Change Processing Stage

**Prepared by**

MGG SOFTWARE

---

## Version History

| Version | Updated By | Date | Changes |
| --- | --- | --- | --- |
| v1.0 | System | 26/01/2025 | Document Initiation based on FD v1.2 |

---

## Table of Content

| Section | Content | Pages |
| --- | --- | --- |
| 1 | Manual Change Processing Stage | - |
| 1.1 | Use Case | - |
| 1.2 | High-Level Process Flow for Manual Change Processing Stage via OCMS Staff Portal | - |
| 1.3 | Change Notice Processing Stage Screen | - |
| 1.3.1 | Search Notices for Manual Change Processing Stage | - |
| 1.4 | Submission of the Notice Change Processing Stage | - |
| 1.4.1 | User Journey for Change Notice Processing Screen | - |
| 1.4.2 | Portal Validation for New Processing Stage | - |
| 1.4.3 | Change Notice Processing Stage Details Online Form Fields | - |
| 1.5 | Backend Processing for Change Notice Processing Stage Submission | - |
| 1.5.1 | Backend Processing for Manual Change Processing Stage | - |
| 1.5.1.1 | Change Notice's Next Processing Stage Sub-flow | - |
| 1.5.1.2 | Data Update for Change Processing Stage of Manual Stage Change Submission | - |
| 1.5.1.3 | Handling amount payable for Change Processing Stage | - |
| 1.5.1.4 | Generate Change Notice Processing Report Sub-flow | - |
| 1.6 | OCMS Staff Portal displays for successful Change Processing Stage | - |
| 1.7 | OCMS Staff Portal display for failed Change Processing Stage Notices | - |
| 1.8 | Accessing Stage Change Reports | - |
| 1.8.1 | Auto Download Upon Manual Stage Change Submission | - |
| 1.8.2 | Manual Download from Staff Portal Report Module | - |
| 2 | Manual Stage Change Initiated by PLUS | - |
| 2.1 | Use Case | - |
| 2.2 | Manual Change Processing Stage via PLUS Staff Portal Processing Flow | - |
| 2.3 | Integration with PLUS | - |
| 2.4 | Processing of Stage Change in OCMS Backend | - |

---

# Section 1 – Manual Change Processing Stage

## 1.1 Use Case

1. The Change Processing Stage module allows OICs to manually change a notice's processing stage, which in turn changes its workflow.

2. The OCMS Staff Portal has a Change Notice Processing Stage module that allows OCMS OICs to:<br>a. Search for notice(s) to be updated with a new processing stage.<br>b. Display the search results which are applicable to change the new processing stage on screen.<br>c. Change the new processing stage by selecting the notice and completing the new processing stage form.<br>d. Perform a validity check for the new processing stage is allowed to change before sending the change request to the OCMS backend.<br>e. Retrieve and download the Change Notice Processing Stage Data Report from the backend to view the Notices that manually change the new processing stage.

3. When the Change Processing Stage is initiated from the Portal, the OCMS Backend will:<br>a. Validate the request and update the previous processing stage and date, last processing stage and date, next processing stage and date of the notice in the VON table. Update the previous processing stage with the value stored in the last processing stage, update the last processing stage and the next processing stage with the new processing stage value.<br>b. Update the amount payable, administration fees immediately.<br>c. Generate the report for internal tracking.<br>d. Return the updated notice details (with the updated last processing stage and next processing stage) together with the report link to the OCMS Staff Portal.

---

## 1.2 High-Level Process Flow for Manual Change Processing Stage via OCMS Staff Portal

<!-- Insert flow diagram here -->
![High-Level Flow](./images/3.1_High-Level.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Launch CPS module | OIC launches the OCMS Staff Portal Change Processing Stage module | Launch CPS module |
| Enter search criteria | OIC enters search criteria to retrieve notice(s) - Notice number, ID number, Vehicle number, Current processing stage, Date of current processing stage | Enter search criteria |
| Display results | Staff Portal displays search results with eligible and ineligible notices | Display results |
| Select notices, fill form | OIC selects notice(s) and fills up Change Processing Stage form with new processing stage, reason for change, and remarks | Select notices, fill form |
| Submit request | OIC submits the new processing stage request | Submit request |
| Backend processes | OCMS Backend processes to change the processing stage | Backend processes |
| Display result | OCMS Staff Portal displays stage change status and report link | Display result |

---

## 1.3 Change Notice Processing Stage Screen

Refer to FD Section 2.3 for detailed screen layout and search parameters.

The Change Processing Stage Screen provides the following features:
- Search for notice(s) to be updated with a new processing stage
- Select the notice(s) and submit a stage change request by completing the new processing stage form

### 1.3.1 Search Notices for Manual Change Processing Stage

<!-- Insert flow diagram here -->
![Search Notices Flow](./images/3.2_Search_Notice.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Launch module | OIC launches the OCMS Staff Portal Change Processing Stage module | Launch module |
| Enter search criteria | OIC enters search criteria and submits | Enter search criteria |
| Valid? | Staff Portal validates search data. If invalid, show error. If valid, call Search API. | Valid? |
| Receive request | OCMS Backend receives the search request | Receive request |
| Validate | Backend validates request parameters | Validate |
| ID search? | Check if search is by ID number. If yes, query ONOD table first. If no, query VON table directly. | ID search? |
| Query ONOD/VON | Query ocms_offence_notice_owner_driver (if ID search) or ocms_valid_offence_notice table | Query ONOD/VON |
| Found? | Check if records found. If no, return "No record found." If yes, proceed. | Found? |
| Check Court/PS | Send list of Notice Numbers to Court and PS Check Common Function | Check Court/PS |
| Segregate lists | Segregate list into eligible and ineligible notices based on Court/PS check | Segregate lists |
| Return response | Return eligible and ineligible notices list to Staff Portal | Return response |
| Display results | Display notices on screen with checkbox for selection | Display results |

#### API Specification

##### API Search Notices

| Field | Value |
| --- | --- |
| API Name | search-notices |
| URL | UAT: https://parking2.uraz.gov.sg/ocms/v1/change-processing-stage/search <br> PRD: https://parking.uraz.gov.sg/ocms/v1/change-processing-stage/search |
| Description | Search notices based on criteria and return segregated lists of eligible vs ineligible notices for stage change |
| Method | POST |
| Header | `{ "Authorization": "Bearer [token]", "Content-Type": "application/json" }` |
| Payload | `{ "noticeNo": "441000001X", "idNo": "S1234567A", "vehicleNo": "SBA1234A", "lastProcessingStage": "DN1", "dateOfCurrentProcessingStage": "2025-12-20" }` (all fields optional, at least one required) |
| Response | `{ "data": { "appCode": "OCMS-2000", "message": "Success", "eligibleNotices": [{ "noticeNo": "441000001X", "offenceType": "O", "offenceDateTime": "2025-12-01T10:30:00", "offenderName": "TAN AH KOW", "offenderId": "S1234567A", "vehicleNo": "SBA1234A", "currentProcessingStage": "DN1", "currentProcessingStageDate": "2025-12-15T09:00:00", "suspensionType": null, "ownerDriverIndicator": "D" }], "ineligibleNotices": [], "summary": { "total": 1, "eligible": 1, "ineligible": 0 } } }` |
| Response (Empty) | `{ "data": { "appCode": "OCMS-2000", "message": "Success", "eligibleNotices": [], "ineligibleNotices": [], "summary": { "total": 0, "eligible": 0, "ineligible": 0 } } }` |
| Response Failure | `{ "data": { "appCode": "OCMS-4000", "message": "At least one search criterion is required." } }` |

#### Data Mapping

| Zone | Database Table | Field Name | Value |
| --- | --- | --- | --- |
| Intranet | ocms_valid_offence_notice | notice_no | Notice number |
| Intranet | ocms_valid_offence_notice | offence_type | O/D/H |
| Intranet | ocms_valid_offence_notice | offence_date_time | Offence date and time |
| Intranet | ocms_valid_offence_notice | vehicle_no | Vehicle number |
| Intranet | ocms_valid_offence_notice | last_processing_stage | Current processing stage |
| Intranet | ocms_valid_offence_notice | last_processing_date | Current processing date |
| Intranet | ocms_valid_offence_notice | suspension_type | Suspension type |
| Intranet | ocms_offence_notice_owner_driver | offender_name | Offender name |
| Intranet | ocms_offence_notice_owner_driver | offender_id | Offender ID |
| Intranet | ocms_offence_notice_owner_driver | owner_driver_indicator | D/O/H |

#### Success Outcome

- Notices matching search criteria are retrieved from the database
- Eligible and ineligible notices are segregated based on Court and PS validation
- Results are returned to Staff Portal for display
- The workflow reaches the End state without triggering any error-handling paths

#### Error Handling

##### Application Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| No Search Criteria | No search parameter provided | Return error, require at least one criterion |
| Invalid Notice Format | Notice number format is invalid | Display validation error message |
| No Records Found | No matching records in database | Return empty result with message |
| Database Error | Unable to query database | Log error, return system error message |

##### API Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| No Criteria | OCMS-4000 | At least one search criterion is required. | Validation error |
| Court Stage | OCMS-4000 | Notice is in court stage. | Ineligible notice |
| Permanent Suspension | OCMS-4000 | Permanent Suspension is active. | Ineligible notice |
| Server Error | OCMS-5000 | Something went wrong. Please try again later. | Internal error |

---

## 1.4 Submission of the Notice Change Processing Stage

### 1.4.1 User Journey for Change Notice Processing Screen

<!-- Insert flow diagram here -->
![User Journey Flow](./images/3.3_User_Journey.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Select notices | User selects notice(s) using checkbox | Select notices |
| Fill form | User fills form: New Stage, Reason, Remarks (if OTH), MHA/DH check | Fill form |
| Click Submit | User clicks Submit button | Click Submit |
| Validate form | Portal validates form fields (required fields, format) | Validate form |
| Valid? | Check if form is valid. If no, show error. If yes, proceed. | Valid? |
| Check role eligibility | Check if new stage is eligible for offender type (D: DN1/DN2/DR3, O/H: ROV/RD1/RD2/RR3) | Check role eligibility |
| Eligible? | Check if stage transition is allowed. If no, show error. If yes, proceed. | Eligible? |
| Show summary | Display summary of changes for confirmation | Show summary |
| Confirm? | User confirms the changes. If no, cancel. If yes, proceed. | Confirm? |
| Call Submit API | Send request to backend API | Call Submit API |
| Display result | Display result from backend (success/failure) | Display result |

Refer to FD Section 2.4.1 for detailed user journey flow.

#### Data Mapping

| Zone | Database Table | Field Name | Value |
| --- | --- | --- | --- |
| Intranet | ocms_valid_offence_notice | notice_no | Selected notice number |
| Intranet | ocms_valid_offence_notice | last_processing_stage | Current processing stage |
| Intranet | ocms_offence_notice_owner_driver | owner_driver_indicator | D/O/H |
| Frontend | Form Input | newProcessingStage | User selected new stage |
| Frontend | Form Input | reasonOfChange | User selected reason |
| Frontend | Form Input | remarks | User entered remarks |
| Frontend | Form Input | mhaDhCheckAllow | Y/N |

#### Success Outcome

- User successfully selects notices and fills form fields
- Form validation passes on all required fields
- Request is sent to backend for processing

#### Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| No Notice Selected | OCMS-4000 | Please select at least one notice. | Validation error |
| Stage Not Selected | OCMS-4000 | Please select a new processing stage. | Validation error |
| Reason Not Selected | OCMS-4000 | Please select a reason for change. | Validation error |
| Remarks Required | OCMS-4000 | Remarks are required when reason is 'Others'. | Validation error |

### 1.4.2 Portal Validation for New Processing Stage

<!-- Insert flow diagram here -->
![Portal Validation Flow](./images/3.4_Portal_Validation.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start loop | Begin validation loop for all selected notices | Start loop |
| Get next notice | Get next notice from selected list | Get next notice |
| Determine role | Determine offender type (D/O/H) from owner_driver_indicator | Determine role |
| Get eligible stages | Get list of eligible stages based on role | Get eligible stages |
| Stage eligible? | Check if new stage is in eligible stages list. If yes, add to changeable. If no, add to non-changeable. | Stage eligible? |
| Add to changeable | Add notice to changeable list | Add to changeable |
| Add to non-changeable | Add notice to non-changeable list with reason | Add to non-changeable |
| Last record? | Check if this is the last notice. If no, loop back. If yes, proceed. | Last record? |
| Has changeable? | Check if at least one notice is changeable. If no, show error. If yes, proceed. | Has changeable? |
| Show summary | Display summary with changeable and non-changeable notices | Show summary |

**Offender Type Stage Eligibility:**

| Offender Type | Eligible Stages |
| --- | --- |
| DRIVER (D) | DN1, DN2, DR3 |
| OWNER (O) | ROV, RD1, RD2, RR3 |
| HIRER (H) | ROV, RD1, RD2, RR3 |

#### API Specification

##### API Validate Notices

| Field | Value |
| --- | --- |
| API Name | validate-notices |
| URL | UAT: https://parking2.uraz.gov.sg/ocms/v1/change-processing-stage/validate <br> PRD: https://parking.uraz.gov.sg/ocms/v1/change-processing-stage/validate |
| Description | Validate notices BEFORE submission to identify eligible vs ineligible notices |
| Method | POST |
| Header | `{ "Authorization": "Bearer [token]", "Content-Type": "application/json" }` |
| Payload | `{ "notices": [{ "noticeNo": "441000001X", "currentStage": "DN1", "offenderType": "D" }], "newProcessingStage": "DN2", "reasonOfChange": "SUP", "remarks": "Speed up processing" }` |
| Response | `{ "data": { "appCode": "OCMS-2000", "message": "Success", "changeableNotices": [{ "noticeNo": "441000001X", "currentStage": "DN1", "offenderType": "D", "message": "Eligible for stage change to DN2" }], "nonChangeableNotices": [], "summary": { "total": 1, "changeable": 1, "nonChangeable": 0 } } }` |
| Response Failure | `{ "data": { "appCode": "OCMS-4000", "message": "Remarks are mandatory when reason for change is 'OTH'" } }` |

#### Data Mapping

| Zone | Database Table | Field Name | Value |
| --- | --- | --- | --- |
| Intranet | ocms_valid_offence_notice | notice_no | Notice number |
| Intranet | ocms_valid_offence_notice | last_processing_stage | Current processing stage |
| Intranet | ocms_offence_notice_owner_driver | owner_driver_indicator | D/O/H |
| Intranet | ocms_stage_map | last_stage | Current stage for mapping |
| Intranet | ocms_stage_map | next_stage | Allowed next stage |

#### Success Outcome

- All notices are validated against business rules
- Changeable and non-changeable notices are identified
- Validation results returned to portal for display

#### Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| Empty List | OCMS-4000 | Notices list cannot be empty. | Validation error |
| Remarks Required | OCMS-4000 | Remarks are mandatory when reason for change is 'OTH' (Others). | Validation error |
| Stage Not Eligible | OCMS-4000 | Stage {stage} not eligible for role {role}. | Business rule |
| Server Error | OCMS-5000 | Something went wrong. Please try again later. | Internal error |

### 1.4.3 Change Notice Processing Stage Details Online Form Fields

Refer to FD Section 2.4.3 for complete form field definitions.

**Key Form Fields:**

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| New Processing Stage | Dropdown | Yes | Target processing stage |
| Reason for Change | Dropdown | Yes | REC, RSN, SUP, OTH, WOD |
| Remarks | Text | Conditional | Required if reason = OTH |
| MHA/DH Check | Checkbox | No | Y = Send to MHA/DH (default), N = Skip MHA/DH check |

**Reason of Change Codes Reference:**

| Code | Description | Remarks Required |
| --- | --- | --- |
| REC | Recheck with Vault | No |
| RSN | Resend Notice | No |
| SUP | Speed up processing | No |
| OTH | Others | **Yes** |
| WOD | Sent to wrong owner/hirer/driver | No |
| PLS | PLUS system (auto-generated) | No |

---

## 1.5 Backend Processing for Change Notice Processing Stage Submission

### 1.5.1 Backend Processing for Manual Change Processing Stage

<!-- Insert flow diagram here -->
![Backend Processing Flow](./images/3.5_Backend_Processing.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Submit | Staff Portal submits change processing stage request | Submit |
| Receive | Backend receives the request | Receive |
| JWT valid? | Validate JWT token. If invalid, return 401 Unauthorized. | JWT valid? |
| Validate format | Validate request payload format and required fields | Validate format |
| Initial? | Check isConfirmation flag. If false (initial request), check existing. If true (confirmed), skip to loop. | Initial? |
| Check existing | Query CPS table for existing record today for this notice | Check existing |
| Exists? | If existing record found, return WARNING to confirm. If not, proceed. | Exists? |
| Loop notices | Loop through each notice in the batch | Loop notices |
| Eligible? | Check if stage transition is allowed. If not eligible, mark as FAILED. | Eligible? |
| Query VON | Query VON record for current stage and amount | Query VON |
| Calc amount | Execute amount payable calculation (Refer to 3.7) | Calc amount |
| Update stage | Execute Change Notice's Next Processing Stage Sub-flow (Refer to 3.6) | Update stage |
| Update VON | Update VON table with new stage values and amount | Update VON |
| Update ONOD | Update ONOD table with mha_dh_check_allow flag if requested | Update ONOD |
| Insert CPS | Insert audit record into ocms_change_of_processing table | Insert CPS |
| Last? | Check if this is the last notice in batch. If no, loop back. If yes, proceed. | Last? |
| Any success? | Check if any notice was successfully updated. If yes, generate report. If all failed, skip report. | Any success? |
| Gen report | Execute Generate Report Sub-flow (Refer to 3.8) | Gen report |
| Build response | Build response with summary and results | Build response |
| Return | Return response to Staff Portal | Return |
| Display result | Staff Portal displays result to user | Display result |

#### API Specification

##### API Submit Change Processing Stage

| Field | Value |
| --- | --- |
| API Name | change-processing-stage |
| URL | UAT: https://parking2.uraz.gov.sg/ocms/v1/change-processing-stage <br> PRD: https://parking.uraz.gov.sg/ocms/v1/change-processing-stage |
| Description | Submit batch change processing stage request to update notice stages |
| Method | POST |
| Header | `{ "Authorization": "Bearer [token]", "Content-Type": "application/json" }` |
| Payload | `{ "notices": [{ "noticeNo": "441000001X", "currentStage": "DN1" }], "newStage": "DN2", "reason": "SUP", "remark": "Speed up processing", "mhaDhCheckAllow": "Y", "isConfirmation": false }` |
| Response | `{ "data": { "appCode": "OCMS-2000", "message": "Success", "summary": { "requested": 1, "succeeded": 1, "failed": 0 }, "results": [{ "noticeNo": "441000001X", "outcome": "UPDATED", "previousStage": "DN1", "newStage": "DN2", "message": "Stage changed successfully" }], "reportUrl": "https://storage.blob.core.windows.net/reports/ChangeStageReport_20251220_OIC001.xlsx" } }` |
| Response (Warning) | `{ "data": { "appCode": "OCMS-4001", "message": "Notice No. 441000001X has existing stage change update. Please confirm to proceed." } }` |
| Response Failure | `{ "data": { "appCode": "OCMS-4000", "message": "Request cannot be empty." } }` |

### 1.5.1.1 Change Notice's Next Processing Stage Sub-flow

<!-- Insert flow diagram here -->
![Stage Update Sub-flow](./images/3.6_Stage_Update_Subflow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Save old values | Save old last_processing_stage and last_processing_date values | Save old values |
| Set prev = old last | Set prev_processing_stage = oldLastStage, prev_processing_date = oldLastDate | Set prev = old last |
| Set last = new stage | Set last_processing_stage = newStage, last_processing_date = GETDATE() | Set last = new stage |
| Get NEXT_STAGE param | Query ocms_parameter for NEXT_STAGE where param_code = newStage | Get NEXT_STAGE param |
| Set next stage | Set next_processing_stage = parameter value from NEXT_STAGE | Set next stage |
| Get STAGEDAYS param | Query ocms_parameter for STAGEDAYS where param_code = newStage | Get STAGEDAYS param |
| Set next date | Calculate next_processing_date = DATEADD(DAY, stageDays, GETDATE()) | Set next date |
| Return | Return updated values | Return |

#### Data Mapping

| Zone | Database Table | Field Name | Value |
| --- | --- | --- | --- |
| Intranet | ocms_valid_offence_notice | prev_processing_stage | Old last_processing_stage |
| Intranet | ocms_valid_offence_notice | prev_processing_date | Old last_processing_date |
| Intranet | ocms_valid_offence_notice | last_processing_stage | New stage value |
| Intranet | ocms_valid_offence_notice | last_processing_date | Current datetime |
| Intranet | ocms_valid_offence_notice | next_processing_stage | From ocms_parameter |
| Intranet | ocms_valid_offence_notice | next_processing_date | Calculated from STAGEDAYS |
| Intranet | ocms_parameter | param_id = 'NEXT_STAGE' | Next stage mapping |
| Intranet | ocms_parameter | param_id = 'STAGEDAYS' | Days until next stage |

#### Success Outcome

- Previous processing stage and date are preserved
- Last processing stage updated to new stage value
- Next processing stage derived from ocms_parameter
- Next processing date calculated based on STAGEDAYS parameter

#### Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| Parameter Not Found | OCMS-4004 | Stage configuration not found for {stage}. | Missing parameter |
| Invalid Stage | OCMS-4000 | Invalid processing stage: {stage}. | Validation error |

### 1.5.1.2 Data Update for Change Processing Stage of Manual Stage Change Submission

| Zone | Database Table | Field Name | Value |
| --- | --- | --- | --- |
| Intranet | ocms_valid_offence_notice | prev_processing_stage | Old last_processing_stage (UPDATE) |
| Intranet | ocms_valid_offence_notice | prev_processing_date | Old last_processing_date (UPDATE) |
| Intranet | ocms_valid_offence_notice | last_processing_stage | New stage value (UPDATE) |
| Intranet | ocms_valid_offence_notice | last_processing_date | Current datetime (UPDATE) |
| Intranet | ocms_valid_offence_notice | next_processing_stage | From ocms_parameter (UPDATE) |
| Intranet | ocms_valid_offence_notice | next_processing_date | Calculated from STAGEDAYS (UPDATE) |
| Intranet | ocms_valid_offence_notice | amount_payable | Recalculated amount (UPDATE) |
| Intranet | ocms_valid_offence_notice | upd_user_id | Logged-in user ID (UPDATE) |
| Intranet | ocms_valid_offence_notice | upd_date_time | Current datetime (UPDATE) |
| Intranet | ocms_offence_notice_owner_driver | mha_dh_check_allow | Y = Send to MHA/DH, N = Skip (default Y) |
| Intranet | ocms_offence_notice_owner_driver | upd_user_id | Logged-in user ID (UPDATE) |
| Intranet | ocms_offence_notice_owner_driver | upd_date_time | Current datetime (UPDATE) |
| Intranet | ocms_change_of_processing | notice_no, date_of_change, last_processing_stage, new_processing_stage, reason_of_change, authorised_officer, source, remarks, cre_date, cre_user_id | Audit record (INSERT) |

**Notes:**
- Audit user for Manual Change Processing Stage: Use logged-in user ID (not `ocmsiz_app_conn`)
- Insert order: Update VON (parent) first, then update ONOD, then insert CPS (child)

#### Success Outcome

- VON record updated with new processing stage values
- ONOD mha_dh_check_allow flag updated if requested
- Audit record inserted into ocms_change_of_processing table
- All database operations completed in correct order (parent before child)

#### Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| VON Update Failed | OCMS-5001 | Unable to update notice record. | Database error |
| ONOD Update Failed | OCMS-5001 | Unable to update offender record. | Database error |
| CPS Insert Failed | OCMS-5001 | Unable to create audit record. | Database error |
| Transaction Rollback | OCMS-5000 | Database transaction failed. Please try again. | Transaction error |

### 1.5.1.3 Handling amount payable for Change Processing Stage

<!-- Insert flow diagram here -->
![Amount Payable Flow](./images/3.7_Amount_Payable.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Get composition | Get current composition amount from VON | Get composition |
| Get admin fee | Get ADMIN_FEE from ocms_parameter | Get admin fee |
| Get surcharge | Get SURCHARGE from ocms_parameter | Get surcharge |
| Check rule | Check stage transition rule | Check rule |
| Calculate amount | Apply calculation based on rule | Calculate amount |
| Update VON | Update amount_payable in VON | Update VON |

#### Data Mapping

| Zone | Database Table | Field Name | Value |
| --- | --- | --- | --- |
| Intranet | ocms_valid_offence_notice | composition | Base amount |
| Intranet | ocms_valid_offence_notice | amount_payable | Calculated amount |
| Intranet | ocms_parameter | param_id = 'ADMIN_FEE' | Administration fee |
| Intranet | ocms_parameter | param_id = 'SURCHARGE' | Court surcharge |

#### Success Outcome

- Amount payable correctly calculated based on stage transition rules
- Administration fee or surcharge applied where applicable
- VON record updated with new amount_payable value

#### Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| Parameter Not Found | OCMS-4004 | Fee parameter not found: {param_id}. | Missing fee config |
| Calculation Error | OCMS-5000 | Unable to calculate amount payable. | Calculation failed |

### 1.5.1.4 Generate Change Notice Processing Report Sub-flow

<!-- Insert flow diagram here -->
![Generate Report Flow](./images/3.8_Generate_Report.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Query CPS records | Query today's change records from CPS table for current user | Query CPS records |
| Has data? | Check if records found. If no, return without report. If yes, proceed. | Has data? |
| Build Excel | Build Excel report content with change records | Build Excel |
| Generate filename | Generate filename: ChangeStageReport_{yyyyMMdd}_{HHmmss}_{userId}.xlsx | Generate filename |
| Upload to Azure | Upload Excel file to Azure Blob Storage container (ocms-reports) | Upload to Azure |
| Get URL | Get report URL from Azure Blob Storage | Get URL |
| Return URL | Return report URL to caller | Return URL |

**Retry Logic:**
- If upload fails, retry once (MAX_GENERATION_RETRIES = 1)
- If all retries fail, send error notification email

#### Data Mapping

| Zone | Database Table | Field Name | Value |
| --- | --- | --- | --- |
| Intranet | ocms_change_of_processing | notice_no, date_of_change, last_processing_stage, new_processing_stage, reason_of_change, remarks, cre_user_id, cre_date | Audit record fields |
| Azure | Blob Storage | Report file | Excel report |

#### Success Outcome

- VON record updated with new processing stage values
- ONOD mha_dh_check_allow updated if requested
- Audit record inserted into ocms_change_of_processing
- Amount payable recalculated based on stage transition
- Excel report generated and uploaded to Azure Blob
- Report URL returned to portal for download

#### Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| Empty Items | OCMS-4000 | Items list cannot be empty. | Validation error |
| Missing NoticeNo | OCMS-4000 | noticeNo is required. | Validation error |
| VON Not Found | OCMS-4004 | VON not found. | Notice not found |
| Existing Record | OCMS-4000 | Notice has existing stage change update. Please confirm. | Warning |
| Court Stage | OCMS-4000 | Notice is at court stage. | Business rule |
| Database Error | OCMS-5001 | Unable to save data. Please try again later. | Database error |
| Server Error | OCMS-5000 | Something went wrong. Please try again later. | Internal error |

---

## 1.6 OCMS Staff Portal displays for successful Change Processing Stage

Refer to FD Section 2.6 for UI display specifications.

**Success Display Elements:**
- Success message displayed for each updated notice
- Report download link provided
- Updated notice details shown (new stage, new date)

---

## 1.7 OCMS Staff Portal display for failed Change Processing Stage Notices

Refer to FD Section 2.7 for UI display specifications.

**Failure Display Elements:**
- Error message displayed for each failed notice
- Error code and description shown
- User can retry or modify request

---

## 1.8 Accessing Stage Change Reports

### 1.8.1 Auto Download Upon Manual Stage Change Submission

After successful submission, the Change Processing Stage Report is automatically generated and the download link is provided in the response.

Report filename format: `ChangeStageReport_{YYYYMMDD}_{HHMMSS}_{userId}.xlsx`

### 1.8.2 Manual Download from Staff Portal Report Module

<!-- Insert flow diagram here -->
![Report Management Flow](./images/3.10_Report_Management.png)

NOTE: Due to page size limit, the full-sized image is appended.

**[A] Retrieve Reports List**

| Step | Description | Brief Description |
| --- | --- | --- |
| User clicks View Reports | User clicks View Reports on Staff Portal | User clicks View Reports |
| Enter date range | User enters date range (startDate, endDate) | Enter date range |
| Call Reports API | Portal calls Reports API with date range | Call Reports API |
| Receive request | Backend receives the request | Receive request |
| Validate date range | Validate date range format | Validate date range |
| Range <= 90 days? | Check if date range is within 90 days. If no, return error. If yes, proceed. | Range <= 90 days? |
| Query CPS by date range | Query CPS table by date range | Query CPS by date range |
| Group by date | Group records by date | Group by date |
| Build report metadata list | Build list of reports with metadata (date, user, count) | Build report metadata list |
| Return response | Return reports list to portal | Return response |
| Display reports list | Display reports list on screen | Display reports list |

**[B] Download Individual Report**

| Step | Description | Brief Description |
| --- | --- | --- |
| User clicks Download | User clicks Download button on a report | User clicks Download |
| Call Download API | Portal calls Download API with report ID | Call Download API |
| Receive request | Backend receives the request | Receive request |
| Query report | Query report path from storage | Query report |
| Found? | Check if report exists. If no, return error. If yes, proceed. | Found? |
| Fetch from Azure | Retrieve file from Azure Blob Storage | Fetch from Azure |
| Stream file | Stream Excel file to user browser | Stream file |
| Save/Open Excel | User saves or opens the Excel file | Save/Open Excel |

#### API Specification

##### API Retrieve Reports List

| Field | Value |
| --- | --- |
| API Name | reports-list |
| URL | UAT: https://parking2.uraz.gov.sg/ocms/v1/change-processing-stage/reports <br> PRD: https://parking.uraz.gov.sg/ocms/v1/change-processing-stage/reports |
| Description | Retrieve list of change processing stage reports by date range |
| Method | POST |
| Header | `{ "Authorization": "Bearer [token]", "Content-Type": "application/json" }` |
| Payload | `{ "startDate": "2025-12-01", "endDate": "2025-12-31" }` |
| Response | `{ "data": { "appCode": "OCMS-2000", "message": "Success", "reports": [{ "reportDate": "2025-12-20", "generatedBy": "OIC001", "noticeCount": 15, "reportUrl": "reports/ChangeStageReport_20251220_OIC001.xlsx" }], "totalReports": 1, "totalNotices": 15 } }` |
| Response Failure | `{ "data": { "appCode": "OCMS-4000", "message": "Date range cannot exceed 90 days." } }` |

##### API Download Report

| Field | Value |
| --- | --- |
| API Name | report-download |
| URL | UAT: https://parking2.uraz.gov.sg/ocms/v1/change-processing-stage/report/download <br> PRD: https://parking.uraz.gov.sg/ocms/v1/change-processing-stage/report/download |
| Description | Download individual report file |
| Method | POST |
| Header | `{ "Authorization": "Bearer [token]", "Content-Type": "application/json" }` |
| Payload | `{ "reportId": "ChangeStageReport_20251220_143022_OIC001.xlsx" }` |
| Response | Binary Excel file with Content-Type: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` |
| Response Failure | `{ "data": { "appCode": "OCMS-4004", "message": "Report not found." } }` |

#### Data Mapping

| Zone | Database Table | Field Name | Value |
| --- | --- | --- | --- |
| Intranet | ocms_change_of_processing | cre_date | Report creation date |
| Intranet | ocms_change_of_processing | cre_user_id | User who generated |
| Azure | Blob Storage | Report file | Excel report |

#### Success Outcome

- Reports list retrieved by date range
- Report files downloaded from Azure Blob Storage
- Excel file streamed to user browser

#### Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| Range Exceeds Limit | OCMS-4000 | Date range cannot exceed 90 days. | Validation error |
| Report Not Found | OCMS-4004 | Report not found. | File not found |
| Server Error | OCMS-5000 | Something went wrong. Please try again later. | Internal error |

---

# Section 2 – Manual Stage Change Initiated by PLUS

## 2.1 Use Case

1. The Change Processing Stage refers to the scenario where PLUS triggers a request to OCMS to manually change a notice's processing stage, which in turn changes its workflow.

2. This occurs when PLUS officers need to change the current processing stage of a notice, and the change is carried out manually within the notice processing workflow.

3. The PLUS system will perform validation to decide whether a Notice is eligible for a stage change before the stage change request is sent to the OCMS Intranet Backend.

4. When the OCMS Intranet Backend receives a stage change request, it performs the following actions:<br>a. Validate the request and update the last and next_processing_stage of the notice in the VON table to this new stage.<br>b. Generate the report for internal tracking.<br>c. Return the notice details (with the updated last and next_processing_stage) together with the report link.

Note: Since PLUS is not allowed to change the stage to CFC and both OCMS and PLUS are also not allowed to change the stage to ENA, these options should not be displayed in the dropdown list of New Processing Stage.

---

## 2.2 Manual Change Processing Stage via PLUS Staff Portal Processing Flow

<!-- Insert flow diagram here -->
![PLUS Integration Flow](./images/3.9_PLUS_Integration.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Initiate | Pre-condition: PLUS officer initiates the stage change request | Initiate |
| Check eligibility | Pre-condition: PLUS system checks eligibility based on pre-defined rules | Check eligibility |
| Send to OCMS | Pre-condition: PLUS sends request to OCMS via API through APIM | Send to OCMS |
| Receive | OCMS Intranet Backend receives the request | Receive |
| CFC from PLUS? | Check if nextStageName=CFC AND source=005. If yes, return error (CFC blocked). | CFC from PLUS? |
| U type skip? | Check if offenceType=U AND nextStageName IN (DN1,DN2,DR3,CPC). If yes, return success without update. | U type skip? |
| Query stage_map | Query ocms_stage_map to validate stage transition | Query stage_map |
| Allowed? | Check if stage transition is allowed. If not, return error. | Allowed? |
| Loop notices | Loop through each notice in the batch | Loop notices |
| Query VON | Query VON record by notice number | Query VON |
| Found? | Check if VON record found. If not found, mark as FAILED and continue loop. | Found? |
| Update stage | Calculate new stage values (prev, last, next) | Update stage |
| Update VON | Update VON table with new stage values | Update VON |
| Insert CPS | Insert audit record into ocms_change_of_processing with source='PLUS' | Insert CPS |
| Last? | Check if this is the last notice. If no, loop back. If yes, proceed. | Last? |
| Errors? | Check if any errors occurred. If yes, return errors. If no, proceed. | Errors? |
| Gen report | Generate Excel report | Gen report |
| Return success | Return success response with per-notice results to PLUS | Return success |
| Receive response | PLUS system receives response from OCMS | Receive response |

#### Data Mapping

| Zone | Database Table | Field Name | Value |
| --- | --- | --- | --- |
| Intranet | ocms_valid_offence_notice | notice_no | Notice number |
| Intranet | ocms_valid_offence_notice | last_processing_stage | Current stage |
| Intranet | ocms_valid_offence_notice | next_processing_stage | Next stage |
| Intranet | ocms_valid_offence_notice | amount_payable | Amount payable |
| Intranet | ocms_change_of_processing | notice_no, date_of_change, source | Audit record |
| Intranet | ocms_stage_map | last_stage, next_stage | Stage mapping |

#### Success Outcome

- PLUS request received and validated by OCMS Backend
- Stage transition validated against ocms_stage_map
- VON records updated with new processing stage
- Audit records inserted with source = 'PLUS'
- Success response returned to PLUS system

#### Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| CFC Blocked | OCMS-4000 | Stage change to CFC is not allowed from PLUS. | Business rule |
| Invalid Transition | OCMS-4000 | Stage transition not allowed: {from} -> {to}. | Validation error |
| Notice Not Found | OCMS-4004 | Notice number not found: {noticeNo}. | Not found |
| Database Error | OCMS-5001 | Unable to process stage change. | Database error |

---

## 2.3 Integration with PLUS

The PLUS Backend is integrated with the OCMS Backend via an API provided by OCMS.

#### API Specification

##### API PLUS Change Processing Stage

| Field | Value |
| --- | --- |
| API Name | plus-change-processing-stage |
| URL | UAT: https://parking2.uraz.gov.sg/ocms/v1/plus-change-processing-stage <br> PRD: https://parking.uraz.gov.sg/ocms/v1/plus-change-processing-stage |
| Description | External API for PLUS system to trigger manual stage change |
| Method | POST |
| Header | `{ "Authorization": "Bearer [token]", "Content-Type": "application/json", "Ocp-Apim-Subscription-Key": "[APIM key]" }` |
| Payload | `{ "noticeNo": ["441000001X", "441000002X"], "lastStageName": "DN1", "nextStageName": "DN2", "lastStageDate": "2025-09-25T06:58:42", "newStageDate": "2025-09-30T06:58:42", "offenceType": "O", "source": "005" }` |
| Response | `{ "data": { "appCode": "OCMS-2000", "message": "Success", "summary": { "total": 2, "success": 1, "failed": 1 }, "results": [{ "noticeNo": "441000001X", "status": "SUCCESS" }, { "noticeNo": "441000002X", "status": "FAILED", "error": "Notice not found" }] } }` |
| Response Failure | `{ "data": { "appCode": "OCMS-4000", "message": "Stage transition not allowed: DN1 -> RR3." } }` |

**Request Parameters (from PLUS):**

| Field Name | Description | Sample Value |
| --- | --- | --- |
| noticeNo | List of notice numbers | ["441000001X"] |
| lastStageName | Current processing stage | DN1 |
| nextStageName | Target processing stage | DN2 |
| lastStageDate | Date of current stage | 2025-09-25T06:58:42 |
| newStageDate | Date for new stage | 2025-09-30T06:58:42 |
| offenceType | O, E, U | O |
| source | Source code (005 for PLUS) | 005 |

#### Data Mapping

| Zone | Database Table | Field Name | Value |
| --- | --- | --- | --- |
| Intranet | ocms_valid_offence_notice | notice_no | Notice number |
| Intranet | ocms_valid_offence_notice | last_processing_stage | New stage value |
| Intranet | ocms_valid_offence_notice | next_processing_stage | From ocms_parameter |
| Intranet | ocms_valid_offence_notice | amount_payable | Recalculated amount |
| Intranet | ocms_change_of_processing | notice_no, date_of_change, last_processing_stage, new_processing_stage, reason_of_change, authorised_officer, source, remarks, cre_date, cre_user_id | Audit record (INSERT) |
| Intranet | ocms_stage_map | last_stage, next_stage | Stage mapping |

**Notes:**
- Audit user for PLUS operations: Use logged-in user ID
- Source value: "PLUS" or "005"

#### Success Outcome

- Stage transition validated against ocms_stage_map
- VON record updated with new processing stage
- Amount payable recalculated
- Audit record inserted
- Success response returned to PLUS

#### Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| CFC Blocked | OCMS-4000 | Stage change to CFC is not allowed from PLUS source. | Business rule |
| Transition Invalid | OCMS-4000 | Stage transition not allowed: {lastStage} -> {nextStage}. | Validation error |
| Notice Not Found | OCMS-4004 | Notice number not found. | Not found |
| VON Update Failed | OCMS-5001 | Error updating VON. | Database error |
| Server Error | OCMS-5000 | Unexpected system error. | Internal error |

---

## 2.4 Processing of Stage Change in OCMS Backend

Refer to Section 1.5 – Backend Processing for Change Notice Processing Stage Submission.

The PLUS processing flow follows the same backend logic as the OCMS Staff Portal submission, with additional validations:
- CFC stage change is blocked for PLUS source
- offenceType=U with DN1/DN2/DR3/CPC target is skipped


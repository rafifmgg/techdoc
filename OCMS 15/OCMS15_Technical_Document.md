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

---

## Data Dictionary Compliance

**IMPORTANT**: All database tables and fields in this document comply with OCMS Data Dictionary (Intranet zone).

**Tables Used**:
- `ocms_valid_offence_notice` - Valid Offence Notice (main table)
- `ocms_offence_notice_owner_driver` - Owner/Driver details
- `ocms_change_of_processing` - Processing stage change records
- `ocms_stage_map` - Stage transition validation mapping

> **Note**: Before implementation, verify all field names and data types match the current Data Dictionary version. Report any discrepancies immediately.

---

## API Design Standards

**All APIs in this module follow these standards**:

| Standard | Requirement | Compliance |
|----------|-------------|------------|
| **HTTP Method** | All APIs use POST method only | ✅ All 4 APIs use POST |
| **Response Format** | Standard format with appCode and message | ✅ Compliant |
| **Field Selection** | APIs return only required fields for current screen | ✅ Compliant |
| **No Sensitive Data** | No sensitive data exposed in URL endpoints | ✅ All use POST body |
| **API Naming** | Intuitive and consistent naming | ✅ Clear function names |
| **Error Handling** | Complete error codes and messages | ✅ Documented |

**Database Query Standards**:
- ❌ NEVER use SELECT *
- ✅ Always specify only required fields
- ✅ Use `ocmsiz_app_conn` for cre_user_id/upd_user_id (Intranet)
- ✅ Use `ocmsez_app_conn` for Internet operations
- ❌ NEVER use "SYSTEM" as audit user

---

## Table of Content

| Section | Content | Pages |
| --- | --- | --- |
| 1 | Manual Change Processing Stage via OCMS Staff Portal | 1 |
| 1.1 | Use Case | 1 |
| 1.2 | High Level Flow | 2 |
| 1.3 | Search Notices | 3 |
| 1.3.1 | API Specification | 5 |
| 1.3.2 | Data Mapping | 6 |
| 1.3.3 | Success Outcome | 7 |
| 1.3.4 | Error Handling | 7 |
| 1.4 | Portal Validation | 8 |
| 1.4.1 | Success Outcome | 10 |
| 1.4.2 | Error Handling | 10 |
| 1.5 | Backend Processing | 11 |
| 1.5.1 | API Specification | 13 |
| 1.5.2 | Data Mapping | 14 |
| 1.5.3 | Success Outcome | 15 |
| 1.5.4 | Error Handling | 16 |
| 1.6 | Change Stage Subflow | 17 |
| 1.6.1 | Data Mapping | 18 |
| 1.6.2 | Success Outcome | 19 |
| 1.6.3 | Error Handling | 20 |
| 1.7 | Generate Report | 20 |
| 1.7.1 | Data Mapping | 21 |
| 1.7.2 | Success Outcome | 22 |
| 1.7.3 | Error Handling | 22 |
| 2 | Manual Change Processing Stage via PLUS Staff Portal | 23 |
| 2.1 | Use Case | 23 |
| 2.2 | High Level Flow | 24 |
| 2.3 | PLUS API Integration | 25 |
| 2.3.1 | API Specification | 26 |
| 2.3.2 | Data Mapping | 27 |
| 2.3.3 | Success Outcome | 28 |
| 2.3.4 | Error Handling | 28 |
| 3 | Toppan Cron Integration | 29 |
| 3.1 | Use Case | 29 |
| 3.2 | Toppan Stage Update Flow | 30 |
| 3.2.1 | API Specification | 31 |
| 3.2.2 | Data Mapping | 32 |
| 3.2.3 | Success Outcome | 33 |
| 3.2.4 | Error Handling | 33 |

---

# Section 1 – Manual Change Processing Stage via OCMS Staff Portal

## 1.1 Use Case

### 1.1.1 Overview (5W1H)

| Question | Description |
| --- | --- |
| **WHAT** | Manual Change Processing Stage - A function that allows authorised OICs to manually change a notice's processing stage, which in turn changes its workflow. The OCMS Staff Portal provides a user interface to search for notices, validate eligibility, and submit stage change requests. |
| **WHY** | OICs need to change the processing stage of a notice to revert to an earlier stage to resend reminders or to escalate to speed up the processing of a notice. Different processing stages have different workflows, payment terms, and follow-up actions. |
| **WHERE** | OCMS Staff Portal (Frontend) and OCMS Backend (Intranet). The function integrates with database tables: ocms_valid_offence_notice, ocms_change_of_processing, ocms_stage_map, and ocms_offence_notice_owner_driver. |
| **WHEN** | Triggered when an OIC decides to update the processing stage of a notice through the OCMS Staff Portal. The process is real-time and on-demand. |
| **WHO** | System Actors: OIC (Notice Processing Officer), OCMS Staff Portal, OCMS Backend API, Database. The OIC initiates the process, while the backend validates and processes the stage change. |
| **HOW** | Sequential flow: Search notices → Portal validation → Backend validation → Update VON → Insert change record → Calculate amount payable → Generate report → Return response |

### 1.1.2 Use Case Description

Refer to FD Section 2.1 for detailed use case description.

1. The Change Processing Stage module allows OICs to manually change a notice's processing stage through the OCMS Staff Portal.

2. The OCMS Staff Portal provides the following features:<br>a. Search for notice(s) to be updated with a new processing stage<br>b. Display search results segregated into eligible and ineligible notices<br>c. Validate new processing stage eligibility based on offender type<br>d. Submit change request to OCMS Backend<br>e. Download Change Processing Stage Report

3. The OCMS Backend performs the following actions:<br>a. Validate request format and data<br>b. Check VON existence and eligibility<br>c. Check duplicate change record for today<br>d. Validate stage transition using ocms_stage_map<br>e. Update VON processing stage and payment data<br>f. Insert change record into ocms_change_of_processing<br>g. Generate Change Processing Stage Report<br>h. Return response with status and report URL

4. Refer to FD Section 2.2 for the High-Level Process Flow.

> **Note**: All flows, data mappings, and business rules in this technical document have been verified against FD OCMS 15 and maintain full consistency with functional specifications. Any discrepancies between this document and the FD should be reported immediately.

### 1.1.3 Business Context

| Processing Stage Type | Business Impact | Offender Type |
| --- | --- | --- |
| DN1, DN2, DR3 | Driver processing stages - Demand Notice stages for drivers | Driver (owner_driver_indicator = 'D') |
| ROV, RD1, RD2, RR3 | Owner/Hirer/Director processing stages - Registered Owner stages | Owner/Hirer/Director (owner_driver_indicator IN ('O', 'H', 'R')) |
| Court Stages (NOT IN Allowed Stages) | Court processing - Always ineligible for manual change. Court stages are any stage NOT IN Allowed Stages (NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3, CPC, CFC) | All offender types |
| PS Stages (PS1, PS2, PS-FOR, PS-MSF, PS-CUS) | Payment Scheme stages - Always ineligible for manual change | All offender types |

---

## 1.2 High Level Flow

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

## 1.3 Search Notices

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
| Check Eligibility | Process loop | Backend checks each notice for eligibility based on current processing stage |
| Court Stage Check | Decision point | Check if current_processing_stage NOT IN Allowed Stages (NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3, CPC, CFC) |
| Mark Ineligible (Court) | Process | Add to ineligibleNotices list with reason OCMS.CPS.SEARCH.COURT_STAGE |
| PS Stage Check | Decision point | Check if current_processing_stage IN (PS1, PS2, PS-FOR, PS-MSF, PS-CUS) |
| Mark Ineligible (PS) | Process | Add to ineligibleNotices list with reason OCMS.CPS.SEARCH.PS_STAGE |
| Mark Eligible | Process | Add to eligibleNotices list |
| Last Record Check | Decision point | Check if current record is last in result set |
| Return Results | API response | Return segregated lists: eligibleNotices and ineligibleNotices with summary counts |
| Display Results | UI display | Portal displays notice list with details and checkboxes for selection |
| End | Exit point | Search process complete |

**Error Paths:**

| Error Scenario | Condition | Action |
| --- | --- | --- |
| Invalid Search Data | Format validation fails | Display validation error "Invalid <field>" and stop |
| No Record Found | Query returns zero results | Display "No record found" message and stop |

---

### 1.3.1 API Specification

#### API for eService

##### API searchChangeProcessingStage

| Field | Value |
| --- | --- |
| API Name | searchChangeProcessingStage |
| URL | UAT: https://uat-api.ocms.ura.gov.sg/ocms/v1/change-processing-stage/search <br> PRD: https://api.ocms.ura.gov.sg/ocms/v1/change-processing-stage/search |
| Description | Search for notices based on criteria and return segregated lists of eligible vs ineligible notices |
| Method | POST |
| Header | `{ "Authorization": "Bearer [token]", "Content-Type": "application/json" }` |
| Payload | `{ "noticeNo": "N-001", "idNo": "S1234567A", "vehicleNo": "SBA1234A", "currentProcessingStage": "DN1", "dateOfCurrentProcessingStage": "2025-12-20" }` |
| Response (Success) | `{ "eligibleNotices": [ { "noticeNo": "N-001", "offenceType": "SP", "offenceDateTime": "2025-12-01T10:30:00", "offenderName": "John Doe", "offenderId": "S1234567A", "vehicleNo": "SBA1234A", "currentProcessingStage": "DN1", "currentProcessingStageDate": "2025-12-15T09:00:00", "suspensionType": null, "suspensionStatus": null, "ownerDriverIndicator": "D", "entityType": null } ], "ineligibleNotices": [ { "noticeNo": "N-002", "offenceType": "SP", "offenceDateTime": "2025-12-01T11:00:00", "offenderName": "Jane Smith", "offenderId": "S9876543B", "vehicleNo": "SBA5678B", "currentProcessingStage": "CRT", "currentProcessingStageDate": "2025-12-18T14:00:00", "reasonCode": "OCMS.CPS.SEARCH.COURT_STAGE", "reasonMessage": "Notice is in court stage" } ], "summary": { "total": 2, "eligible": 1, "ineligible": 1 } }` |
| Response (No Results) | `{ "eligibleNotices": [], "ineligibleNotices": [], "summary": { "total": 0, "eligible": 0, "ineligible": 0 } }` |
| Response Failure | `{ "eligibleNotices": [], "ineligibleNotices": [], "summary": { "total": 0, "eligible": 0, "ineligible": 0 } }` |

**Request Rules:**
- At least one search criterion is required
- When searching by ID Number: Backend queries ONOD table first, then joins with VON table
- Other searches: Backend queries VON table directly

---

### 1.3.2 Data Mapping

#### Input Parameters

| Parameter | Data Type | Max Length | Required | Nullable | Source | Description |
| --- | --- | --- | --- | --- | --- | --- |
| noticeNo | varchar | 10 | No | Yes | User input | Notice number. Alphanumeric, 10 chars including spaces |
| idNo | varchar | 12 | No | Yes | User input | Identification number. Alphanumeric, max 12 chars |
| vehicleNo | varchar | 14 | No | Yes | User input | Vehicle registration number. Alphanumeric, up to 14 chars |
| currentProcessingStage | varchar | 10 | No | Yes | User selection | Current processing stage code from dropdown |
| dateOfCurrentProcessingStage | date | - | No | Yes | User selection | Date of current processing stage (dd/MM/yyyy format) |

**Intranet Zone:**

| Table | Field Name | Data Type | Max Length | Nullable | Description |
| --- | --- | --- | --- | --- | --- |
| ocms_valid_offence_notice | notice_no | varchar | 10 | No | Notice number (Primary Key) |
| ocms_valid_offence_notice | vehicle_no | varchar | 14 | No | Vehicle registration number |
| ocms_valid_offence_notice | last_processing_stage | varchar | 10 | Yes | Last/current processing stage code |
| ocms_valid_offence_notice | last_processing_date | timestamp | - | Yes | Date and time of last/current processing stage |
| ocms_valid_offence_notice | offence_type | varchar | 2 | No | Type of offence |
| ocms_valid_offence_notice | offence_datetime | timestamp | - | No | Date and time of offence |
| ocms_valid_offence_notice | suspension_type | varchar | 10 | Yes | Type of suspension (if applicable) |
| ocms_valid_offence_notice | suspension_status | varchar | 10 | Yes | Status of suspension |
| ocms_offence_notice_owner_driver | id_no | varchar | 12 | No | Offender ID number |
| ocms_offence_notice_owner_driver | offender_name | varchar | 200 | Yes | Offender name |
| ocms_offence_notice_owner_driver | owner_driver_indicator | varchar | 1 | No | D=Driver, O=Owner, H=Hirer, R=Director |
| ocms_offence_notice_owner_driver | entity_type | varchar | 10 | Yes | Entity type (if applicable) |

#### Database Query Strategy

| Search Criterion | Query Strategy | Tables Involved |
| --- | --- | --- |
| ID Number | Query ONOD first by id_no, then JOIN with VON | ocms_offence_notice_owner_driver, ocms_valid_offence_notice |
| Other criteria | Query VON directly by search filters | ocms_valid_offence_notice |

**Data Source Legend**:
- `VON Query` - Retrieved from ocms_valid_offence_notice table query
- `ONOD Query` - Retrieved from ocms_offence_notice_owner_driver query
- `User Input` - Provided by user from UI form
- `System Generated` - Calculated/generated by system
- `Configuration` - From parameter/standard code tables

---

### 1.3.3 Success Outcome

- Backend successfully queries database and retrieves matching notices
- Notices are segregated into eligible and ineligible lists based on current processing stage
- Court stage notices (NOT IN Allowed Stages: NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3, CPC, CFC) are marked as ineligible with reason code OCMS.CPS.SEARCH.COURT_STAGE
- PS stage notices (PS1, PS2, PS-FOR, PS-MSF, PS-CUS) are marked as ineligible with reason code OCMS.CPS.SEARCH.PS_STAGE
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

## 1.4 Portal Validation

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
| Mark Changeable | Process | Add notice to changeableNotices list |
| Mark Non-Changeable | Process | Add notice to nonChangeableNotices list with reason |
| Last Record Check | Decision point | Check if current notice is last in selection |
| Non-Changeable Check | Decision point | Check if nonChangeableNotices list is not empty |
| Display Summary | UI display | Show summary page for OIC review before backend submission |
| OIC Confirms | User action | OIC reviews summary and confirms submission |
| End | Exit point | Proceed to backend submission |

**Error Paths:**

| Error Scenario | Condition | Action |
| --- | --- | --- |
| Missing Required Fields | New Processing Stage or Reason of Change not filled | Display error, block submission |
| Remarks Required | Reason = OTH but Remarks is empty | Display "Remarks are mandatory when reason for change is 'OTH' (Others)" |
| All Non-Changeable | All selected notices are non-changeable | Display "All selected notices are not eligible for stage change" |
| Mixed Changeable/Non-Changeable | Some notices are non-changeable | Display "Some notices are not eligible. Please uncheck inapplicable notices." |

**Frontend Validation Rules:**

| Rule ID | Field | Validation Rule | Error Message |
| --- | --- | --- | --- |
| FE-010 | New Processing Stage | Required field | New Processing Stage is required |
| FE-011 | Reason of Change | Required field | Reason of Change is required |
| FE-012 | Remark | Required if Reason of Change = "OTH" | Remarks are mandatory when reason for change is 'OTH' (Others) |
| FE-013 | Notice Selection | At least one notice must be selected | Please select at least one notice |
| FE-014 | Send for DH/MHA Check | Prompt confirmation if unchecked | Notice will not go for DH/MHA check. Do you want to proceed? |
| FE-020 | Offender Type = DRIVER | newProcessingStage must be in DN1/DN2/DR3 | Stage not allowed for Driver offender type |
| FE-021 | Offender Type = OWNER/HIRER/DIRECTOR | newProcessingStage must be in ROV/RD1/RD2/RR3 | Stage not allowed for Owner offender type |

---

### 1.4.1 Success Outcome

- Portal validates all mandatory fields are filled
- If DH/MHA check is unchecked, user confirms to proceed
- Portal validates each selected notice's offender type matches the new processing stage
- Changeable notices are segregated from non-changeable notices
- If all notices are changeable, portal displays summary page for OIC review
- OIC confirms submission and portal proceeds to backend API call

---

### 1.4.2 Error Handling

#### Frontend Validation Errors

| Error Scenario | Error Message | User Action |
| --- | --- | --- |
| Missing New Processing Stage | New Processing Stage is required | Fill in the field |
| Missing Reason of Change | Reason of Change is required | Select a reason |
| Missing Remarks for OTH | Remarks are mandatory when reason for change is 'OTH' (Others) | Enter remarks |
| No Notice Selected | Please select at least one notice | Select at least one notice |
| All Non-Changeable Notices | All selected notices are not eligible for stage change. Please uncheck inapplicable notices. | Uncheck inapplicable notices |
| Mixed Changeable/Non-Changeable | Some notices are not eligible. Please uncheck inapplicable notices or proceed with eligible ones only. | Uncheck or proceed |

---

## 1.5 Backend Processing

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

**Error Paths:**

| Error Code | Error Scenario | Action |
| --- | --- | --- |
| OCMS.CPS.INVALID_FORMAT | Items list is empty or noticeNo missing | Return error, skip processing |
| OCMS.CPS.NOT_FOUND | VON not found in database | Mark item as FAILED, continue with next item |
| OCMS.CPS.DUPLICATE_RECORD | Duplicate change record exists and isConfirmation=false | Return warning, mark as FAILED |
| OCMS.CPS.INVALID_TRANSITION | Stage transition not allowed per stage map | Mark item as FAILED, continue with next item |
| OCMS.CPS.UNEXPECTED | Unexpected error during stage change | Mark item as FAILED, continue with next item |

**Backend Validation Rules:**

| Rule ID | Validation Check | Logic | Error Code |
| --- | --- | --- | --- |
| BE-001 | Items not empty | Check items array is not empty | OCMS.CPS.INVALID_FORMAT |
| BE-002 | Notice No present | Check noticeNo field is not empty | OCMS.CPS.MISSING_DATA |
| BE-010 | VON exists | Query ocms_valid_offence_notice by notice_no | OCMS.CPS.NOT_FOUND |
| BE-011 | Court stage check | Check if current_processing_stage in court stages | OCMS.CPS.COURT_STAGE |
| BE-012 | Duplicate record check | Query ocms_change_of_processing by notice_no and today's date | OCMS.CPS.DUPLICATE_RECORD |
| BE-013 | Stage transition allowed | Validate stage transition using ocms_stage_map | OCMS.CPS.INVALID_TRANSITION |
| BE-014 | Remarks required | If reason = "OTH", remarks must not be empty | OCMS.CPS.REMARKS_REQUIRED |
| BE-016 | ENA stage restriction | Cannot change to ENA stage via OCMS Staff Portal | OCMS.CPS.ENA_NOT_ALLOWED |

---

### 1.5.1 API Specification

#### API for eService

##### API changeProcessingStage

| Field | Value |
| --- | --- |
| API Name | changeProcessingStage |
| URL | UAT: https://uat-api.ocms.ura.gov.sg/ocms/v1/change-processing-stage <br> PRD: https://api.ocms.ura.gov.sg/ocms/v1/change-processing-stage |
| Description | Submit batch change processing stage request |
| Method | POST |
| Header | `{ "Authorization": "Bearer [token]", "Content-Type": "application/json", "X-User-Id": "[userId]" }` |
| Payload | `{ "items": [ { "noticeNo": "N-001", "newStage": "DN2", "reason": "SUP", "remark": "Manual adjustment", "source": "PORTAL", "dhMhaCheck": false, "isConfirmation": false } ] }` |
| Response (Success) | `{ "status": "SUCCESS", "summary": { "requested": 1, "succeeded": 1, "failed": 0 }, "results": [ { "noticeNo": "N-001", "outcome": "UPDATED", "previousStage": "DN1", "newStage": "DN2", "code": "OCMS-2000", "message": "Success" } ], "report": { "url": "https://signed-url.xlsx", "expiresAt": "2025-10-28T16:00:00+08:00" } }` |
| Response (Partial Success) | `{ "status": "PARTIAL", "summary": { "requested": 5, "succeeded": 3, "failed": 2 }, "results": [ { "noticeNo": "N-001", "outcome": "UPDATED", "previousStage": "DN1", "newStage": "DN2", "code": "OCMS-2000", "message": "Success" }, { "noticeNo": "N-002", "outcome": "FAILED", "code": "OCMS.CPS.NOT_FOUND", "message": "VON not found" } ], "report": { "url": "https://signed-url.xlsx", "expiresAt": "2025-10-28T16:00:00+08:00" } }` |
| Response (Duplicate Warning) | `{ "status": "PARTIAL", "summary": { "requested": 1, "succeeded": 0, "failed": 1 }, "results": [ { "noticeNo": "N-001", "outcome": "FAILED", "code": "OCMS.CPS.DUPLICATE_RECORD", "message": "Existing change record found for this notice today. Set isConfirmation=true to proceed." } ] }` |
| Response Failure | `{ "status": "FAILED", "summary": { "requested": 0, "succeeded": 0, "failed": 0 }, "results": [ { "noticeNo": "", "outcome": "FAILED", "code": "OCMS.CPS.INVALID_FORMAT", "message": "Items list cannot be empty" } ] }` |

**Request Rules:**
- items array cannot be empty
- noticeNo is required for each item
- newStage is optional (can be derived from current stage)
- dhMhaCheck defaults to false
- isConfirmation defaults to false (set true to override duplicate record warning)

---

### 1.5.2 Data Mapping

#### Request Payload Fields

| Field | Data Type | Required | Description |
| --- | --- | --- | --- |
| items | array | Yes | Array of change processing stage requests |
| items[].noticeNo | varchar(10) | Yes | Notice number |
| items[].newStage | varchar(10) | No | New processing stage code (optional) |
| items[].reason | varchar(10) | Yes | Reason code for change |
| items[].remark | varchar(500) | Conditional | Remarks (required if reason = "OTH") |
| items[].source | varchar(10) | Yes | Source system code (PORTAL/PLUS) |
| items[].dhMhaCheck | boolean | No | Send for DH/MHA check (default: false) |
| items[].isConfirmation | boolean | No | User confirmed duplicate (default: false) |

#### Database Tables (Intranet Zone)

**ocms_valid_offence_notice:**

| Field Name | Data Type | Max Length | Nullable | Description | Updated By |
| --- | --- | --- | --- | --- | --- |
| notice_no | varchar | 10 | No | Notice number (Primary Key) | - |
| prev_processing_stage | varchar | 10 | Yes | Previous processing stage (backup) | Backend update |
| prev_processing_date | timestamp | - | Yes | Previous processing date (backup) | Backend update |
| last_processing_stage | varchar | 10 | Yes | Last/current processing stage | Backend update |
| last_processing_date | timestamp | - | Yes | Last/current processing date | Backend update |
| next_processing_stage | varchar | 10 | Yes | New processing stage after change | Backend update |
| next_processing_date | timestamp | - | Yes | Date for next processing stage | Backend update |
| amount_payable | decimal | 10,2 | Yes | Amount payable after stage change | Backend calculation |
| upd_user_id | varchar | 50 | No | Audit user who updated record | ocmsiz_app_conn |
| upd_dtm | timestamp | - | No | Audit timestamp of update | System generated |

**ocms_change_of_processing:**

| Field Name | Data Type | Max Length | Nullable | Description | Value Source |
| --- | --- | --- | --- | --- | --- |
| notice_no | varchar | 10 | No | Notice number | Request payload |
| date_of_change | timestamp | - | No | Date and time of change | System generated |
| last_processing_stage | varchar | 10 | No | Previous processing stage | VON current stage |
| new_processing_stage | varchar | 10 | No | New processing stage | Request payload |
| reason_of_change | varchar | 10 | No | Reason code for change | Request payload |
| authorised_officer | varchar | 50 | No | Officer who initiated change | X-User-Id header |
| remarks | varchar | 500 | Yes | Additional remarks | Request payload |
| source | varchar | 10 | No | Source system code (PORTAL/005) | Request payload |
| cre_date | timestamp | - | No | Record creation date | System generated |
| cre_user_id | varchar | 50 | No | Audit user who created record | ocmsiz_app_conn |

**Primary Key Generation**:
- If `ocms_change_of_processing` uses auto-increment ID, it must use SQL Server sequences (not application-generated IDs)
- Sequence naming convention: `seq_change_of_processing_id`

**ocms_offence_notice_owner_driver:**

| Field Name | Data Type | Max Length | Nullable | Description | Updated By |
| --- | --- | --- | --- | --- | --- |
| notice_no | varchar | 10 | No | Notice number | - |
| mha_dh_check_allow | varchar | 1 | Yes | DH/MHA check flag (Y/N) | Backend update |
| upd_user_id | varchar | 50 | No | Audit user who updated record | ocmsiz_app_conn |
| upd_dtm | timestamp | - | No | Audit timestamp of update | System generated |

**ocms_stage_map:**

| Field Name | Data Type | Description |
| --- | --- | --- |
| current_stage | varchar(10) | Current processing stage |
| next_stage | varchar(10) | Allowed next processing stage |
| is_allowed | varchar(1) | Y=Allowed, N=Not Allowed |

**Audit User Configuration:**

| Zone | Field | Value | Description |
| --- | --- | --- | --- |
| Intranet | cre_user_id | ocmsiz_app_conn | Database user for record creation |
| Intranet | upd_user_id | ocmsiz_app_conn | Database user for record update |
| Internet | cre_user_id | ocmsez_app_conn | Database user for record creation |
| Internet | upd_user_id | ocmsez_app_conn | Database user for record update |

**Note:** Do NOT use "SYSTEM" for audit user fields. Always use the database connection user.

---

### 1.5.3 Success Outcome

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

### 1.5.4 Error Handling

#### Backend Validation Errors

| Error Code | Error Condition | Error Message | HTTP Status | Action |
| --- | --- | --- | --- | --- |
| OCMS.CPS.INVALID_FORMAT | Items list is empty or null | Items list cannot be empty | 400 | Return error, reject request |
| OCMS.CPS.MISSING_DATA | noticeNo is missing for an item | noticeNo is required | 400 | Mark item as FAILED |
| OCMS.CPS.NOT_FOUND | VON not found in database | VON not found | 400 | Mark item as FAILED |
| OCMS.CPS.COURT_STAGE | Notice is in court stage | Notice is in court stage | 400 | Mark item as FAILED |
| OCMS.CPS.DUPLICATE_RECORD | Duplicate change record exists and isConfirmation=false | Existing change record found for this notice today. Set isConfirmation=true to proceed. | 400 | Return warning, mark as FAILED |
| OCMS.CPS.INVALID_TRANSITION | Stage transition not allowed | Stage transition not allowed: [current] -> [new] | 400 | Mark item as FAILED |
| OCMS.CPS.REMARKS_REQUIRED | Reason=OTH but remarks is empty | Remarks are mandatory when reason for change is 'OTH' (Others) | 400 | Mark item as FAILED |
| OCMS.CPS.UNEXPECTED | Unexpected error during processing | Unexpected error: [details] | 500 | Mark item as FAILED |

#### Duplicate Handling Logic

| Attempt | Condition | Action | Response |
| --- | --- | --- | --- |
| 1st Attempt | Duplicate record exists, isConfirmation=false | Return warning to user | OCMS.CPS.DUPLICATE_RECORD |
| 2nd Attempt | Duplicate record exists, isConfirmation=true | Proceed with update (user confirmed) | OCMS-2000 (Success) |

---

## 1.6 Change Stage Subflow

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

**Error Paths:**

| Error Point | Condition | Action |
| --- | --- | --- |
| VON Update Failed | UPDATE rows affected = 0 | Return error to main flow |
| Insert Failed | INSERT rows affected = 0 | Return error to main flow |
| ONOD Update Failed | UPDATE rows affected = 0 | Return error to main flow |

---

### 1.6.1 Data Mapping

#### Database Operations

**Step 1: UPDATE ocms_valid_offence_notice**

| Field | Value | Source |
| --- | --- | --- |
| prev_processing_stage | Previous processing stage (backup) | VON last_processing_stage |
| prev_processing_date | Previous processing date (backup) | VON last_processing_date |
| last_processing_stage | Current processing stage before change | VON last_processing_stage |
| last_processing_date | Current processing date before change | VON last_processing_date |
| next_processing_stage | New processing stage code | Request payload newStage |
| next_processing_date | Calculated date based on stage rules | Business logic calculation |
| amount_payable | Calculated amount based on stage | Business logic calculation |
| upd_user_id | ocmsiz_app_conn | Database connection user |
| upd_dtm | Current timestamp | System generated |

**SQL Operation:**
```sql
UPDATE ocms_valid_offence_notice
SET prev_processing_stage = last_processing_stage,
    prev_processing_date = last_processing_date,
    last_processing_stage = ?,
    last_processing_date = ?,
    next_processing_stage = ?,
    next_processing_date = ?,
    amount_payable = ?,
    upd_user_id = 'ocmsiz_app_conn',
    upd_dtm = CURRENT_TIMESTAMP
WHERE notice_no = ?
```

**Query Standards**:
- ❌ Do NOT use SELECT *
- ✅ Always specify only the required fields as shown above

**Step 2: INSERT ocms_change_of_processing**

| Field | Value | Source |
| --- | --- | --- |
| notice_no | Notice number | Request payload |
| date_of_change | Current timestamp | System generated |
| last_processing_stage | Previous processing stage | VON current stage |
| new_processing_stage | New processing stage | Request payload newStage |
| reason_of_change | Reason code | Request payload reason |
| authorised_officer | Officer user ID | X-User-Id header |
| remarks | Additional remarks | Request payload remark |
| source | Source system code | Request payload source (PORTAL/005) |
| cre_date | Current timestamp | System generated |
| cre_user_id | ocmsiz_app_conn | Database connection user |

**SQL Operation:**
```sql
INSERT INTO ocms_change_of_processing
(notice_no, date_of_change, last_processing_stage, new_processing_stage,
 reason_of_change, authorised_officer, remarks, source, cre_date, cre_user_id)
VALUES (?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, 'ocmsiz_app_conn')
```

**Query Standards**:
- ❌ Do NOT use SELECT *
- ✅ Always specify only the required fields as shown above

**Step 3: UPDATE ocms_offence_notice_owner_driver (Conditional)**

| Field | Value | Source |
| --- | --- | --- |
| mha_dh_check_allow | 'Y' or 'N' | Request payload dhMhaCheck |
| upd_user_id | ocmsiz_app_conn | Database connection user |
| upd_dtm | Current timestamp | System generated |

**SQL Operation:**
```sql
UPDATE ocms_offence_notice_owner_driver
SET mha_dh_check_allow = ?,
    upd_user_id = 'ocmsiz_app_conn',
    upd_dtm = CURRENT_TIMESTAMP
WHERE notice_no = ?
```

**Query Standards**:
- ❌ Do NOT use SELECT *
- ✅ Always specify only the required fields as shown above

**Note:** This update is only performed if dhMhaCheck flag = true in the request.

---

### 1.6.2 Success Outcome

- Current VON record is successfully queried
- next_processing_date is calculated based on new stage rules
- amount_payable is calculated based on new stage payment rules
- VON record is successfully updated with new stage, date, and amount
- Change record is successfully inserted into ocms_change_of_processing
- If dhMhaCheck is true, ONOD record is successfully updated with mha_dh_check_allow = 'Y'
- Sub-flow returns success to main flow
- Main flow proceeds to generate report

---

### 1.6.3 Error Handling

| Error Scenario | Definition | Action |
| --- | --- | --- |
| VON Update Failed | UPDATE operation returns 0 rows affected | Return error to main flow, mark item as FAILED |
| Insert Failed | INSERT operation fails or returns 0 rows affected | Return error to main flow, mark item as FAILED |
| ONOD Update Failed | UPDATE operation returns 0 rows affected (when dhMhaCheck=true) | Return error to main flow, mark item as FAILED |
| Database Connection Error | Unable to connect to database | Log error, return error to main flow, mark item as FAILED |

---

## 1.7 Generate Report

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

**Error Paths:**

| Error Point | Condition | Action |
| --- | --- | --- |
| Excel Generation Failed | File creation error | Return error to main flow |
| Upload Failed | Azure Blob upload error | Return error to main flow |

---

### 1.7.1 Data Mapping

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

### 1.7.2 Success Outcome

- Report data is successfully prepared from batch results
- Excel file is successfully generated with notice details and outcomes
- Report filename is generated with timestamp and user ID
- Excel file is successfully uploaded to Azure Blob Storage
- Signed URL is successfully generated with expiry time
- Report URL is returned to main flow
- Main flow includes report URL in response to Portal
- Portal auto-downloads the report

---

### 1.7.3 Error Handling

| Error Scenario | Definition | Action |
| --- | --- | --- |
| Excel Generation Failed | Unable to create Excel file | Log error, return error to main flow |
| Upload to Azure Failed | Azure Blob upload error | Log error, return error to main flow |
| Signed URL Generation Failed | Unable to generate signed URL | Log error, return error to main flow |
| No Successful Items | summary.succeeded = 0 | Skip report generation (no report to generate) |

---

# Section 2 – Manual Change Processing Stage via PLUS Staff Portal

## 2.1 Use Case

### 2.1.1 Overview (5W1H)

| Question | Description |
| --- | --- |
| **WHAT** | PLUS Integration - A function that allows PLUS officers to request changes to a notice's processing stage via the PLUS Staff Portal. OCMS validates these requests and updates the workflow accordingly, returning confirmation or error messages to PLUS. |
| **WHY** | PLUS officers need the ability to change the processing stage of notices to support their operational requirements. This integration enables PLUS to manage notice workflows without requiring direct access to OCMS Staff Portal. |
| **WHERE** | PLUS Staff Portal (External System) and OCMS Backend (Intranet). The integration goes through APIM gateway. Uses same database tables as manual change: ocms_valid_offence_notice, ocms_change_of_processing, ocms_stage_map. |
| **WHEN** | Triggered when PLUS officers initiate a stage change request in PLUS Staff Portal. The request is sent to OCMS via API in real-time. |
| **WHO** | System Actors: PLUS Officer, PLUS Staff Portal, APIM Gateway, OCMS Backend API, Database. PLUS officers initiate the process, while OCMS backend validates and processes the stage change. |
| **HOW** | PLUS validates eligibility → Sends request via API (source="005") → OCMS validates → Updates VON → Inserts change record → Generates report → Returns response to PLUS |

### 2.1.2 Use Case Description

Refer to FD Section 3.1 for detailed use case description.

1. The Change Processing Stage refers to the scenario where PLUS triggers a request to OCMS to manually change a notice's processing stage, which in turn changes its workflow.

2. This occurs when PLUS officers need to change the current processing stage of a notice, and the change is carried out manually within the notice processing workflow.

3. The PLUS system performs validation to decide whether a Notice is eligible for stage change before the request is sent to the OCMS Intranet Backend.

4. When the OCMS Intranet Backend receives a stage change request from PLUS:<br>a. Validate the request format and data<br>b. Check source code is "005" (identifies PLUS origin)<br>c. Validate stage transition using ocms_stage_map<br>d. Update next_processing_stage of the notice in VON table<br>e. Insert change record into ocms_change_of_processing<br>f. Generate report for internal tracking<br>g. Return notice details with updated stage and report link to PLUS

5. Note: PLUS is not allowed to change the stage to CFC (Court For Cancellation), so this option should not be displayed in the dropdown list of New Processing Stage in PLUS Portal.

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

**Error Paths:**

| Error Code | Error Scenario | Action |
| --- | --- | --- |
| OCMS-4000 | Invalid request format | Return error response, reject request |
| OCMS-4000 | Invalid source code (not "005") | Return error response, reject request |
| OCMS-4000 | Stage transition not allowed | Return error response, reject request |
| OCMS-4000 | VON not found | Add to failed list, continue with next notice |
| OCMS-4000 | Notice in court stage | Add to failed list, continue with next notice |
| INTERNAL_ERROR | Unexpected system error | Return error response with details |

---

### 2.3.1 API Specification

#### External API for PLUS Integration

##### API plusChangeProcessingStage

| Field | Value |
| --- | --- |
| API Name | plusChangeProcessingStage |
| URL | UAT: https://uat-api.ocms.ura.gov.sg/ocms/v1/external/plus/change-processing-stage <br> PRD: https://api.ocms.ura.gov.sg/ocms/v1/external/plus/change-processing-stage |
| Description | PLUS Staff Portal requests to change notice processing stage |
| Method | POST |
| Header | `{ "Authorization": "Bearer [token]", "Content-Type": "application/json" }` |
| Payload | `{ "noticeNo": ["N-001", "N-002"], "lastStageName": "DN1", "nextStageName": "DN2", "lastStageDate": "2025-09-25T06:58:42", "newStageDate": "2025-09-30T06:58:42", "offenceType": "D", "source": "005" }` |
| Response (Success) | `{ "status": "SUCCESS", "message": "Stage change processed successfully", "noticeCount": 2 }` |
| Response (Failure - Transition) | `{ "status": "FAILED", "code": "OCMS-4000", "message": "Stage transition not allowed: DN1 -> CFC" }` |
| Response (System Error) | `{ "status": "ERROR", "code": "INTERNAL_ERROR", "message": "Unexpected system error: [error details]" }` |

**Request Rules:**
- noticeNo is array of notice numbers
- Stage transition must be allowed according to stage_map
- source must be "005" to identify PLUS origin
- PLUS is not allowed to change stage to CFC

---

### 2.3.2 Data Mapping

#### Request Payload Fields

| Field | Data Type | Required | Description |
| --- | --- | --- | --- |
| noticeNo | array | Yes | Array of notice numbers to change |
| lastStageName | varchar(10) | Yes | Current processing stage |
| nextStageName | varchar(10) | Yes | New processing stage |
| lastStageDate | timestamp | Yes | Date of current processing stage |
| newStageDate | timestamp | Yes | Date for new processing stage |
| offenceType | varchar(2) | Yes | Type of offence |
| source | varchar(10) | Yes | Source code (must be "005" for PLUS) |

#### Validation Rules

| Rule ID | Field | Validation | Error Code | Error Message |
| --- | --- | --- | --- | --- |
| EXT-001 | noticeNo | Array cannot be empty | OCMS-4000 | Notice numbers are required |
| EXT-002 | lastStageName | Required | OCMS-4000 | Last stage name is required |
| EXT-003 | nextStageName | Required | OCMS-4000 | Next stage name is required |
| EXT-004 | Stage Transition | Validate using stage_map | OCMS-4000 | Stage transition not allowed: [last] -> [next] |
| EXT-005 | Source Code | Must be "005" for PLUS | OCMS-4000 | Invalid source code |
| EXT-015 | ENA stage restriction | Cannot change to ENA stage via PLUS | OCMS-4000 | ENA stage not allowed for PLUS |

#### Database Tables

**Same tables as Section 1.5.2:**
- ocms_valid_offence_notice
- ocms_change_of_processing
- ocms_stage_map

**PLUS-specific fields:**

| Table | Field | Value for PLUS | Description |
| --- | --- | --- | --- |
| ocms_change_of_processing | source | "005" | Identifies PLUS as source system |
| ocms_change_of_processing | reason_of_change | From PLUS request | Reason code provided by PLUS |

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

| Error Code | Error Condition | Error Message | HTTP Status | Action |
| --- | --- | --- | --- | --- |
| OCMS-4000 | Notice numbers are missing | Notice numbers are required | 400 | Return error, reject request |
| OCMS-4000 | Last stage name is missing | Last stage name is required | 400 | Return error, reject request |
| OCMS-4000 | Next stage name is missing | Next stage name is required | 400 | Return error, reject request |
| OCMS-4000 | Stage transition not allowed | Stage transition not allowed: [last] -> [next] | 400 | Return error, reject request |
| OCMS-4000 | Invalid source code | Invalid source code | 400 | Return error, reject request |
| OCMS-4000 | All notices in court stage | All notices are in court stage | 400 | Return error with list |
| OCMS-4000 | Mixed valid/invalid notices | Partial success: [X] succeeded, [Y] failed | 200 | Return partial success |
| INTERNAL_ERROR | Unexpected system error | Unexpected system error: [details] | 500 | Log error, return error response |

---

# Section 3 – Toppan Cron Integration

## 3.1 Use Case

### 3.1.1 Overview (5W1H)

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

### 3.1.2 Use Case Description

1. The Toppan Stage Update is an internal function within OCMS that handles automatic processing stage updates after Toppan letter generation.

2. The generate_toppan_letters cron job runs daily and performs the following:<br>a. Generate Toppan letter files for notices that require physical letter sending<br>b. Call updateToppanStages internal API with list of notice numbers<br>c. Receive summary of automatic vs manual updates

3. The OCMS Backend processes the Toppan stage update request:<br>a. Validate request format (noticeNumbers, currentStage, processingDate required)<br>b. For each notice: Check if manual change record exists for today in ocms_change_of_processing<br>c. If manual record exists: Skip VON update, count as manual update<br>d. If no manual record: Update VON processing stage automatically, count as automatic update<br>e. If VON not found or stage transition not allowed: Skip notice, add to errors list<br>f. Return summary: totalNotices, automaticUpdates, manualUpdates, skipped, errors

4. This approach ensures that manual stage changes take precedence over automatic updates, preventing data conflicts.

---

## 3.2 Toppan Stage Update Flow

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

**Error Paths:**

| Error Scenario | Condition | Action |
| --- | --- | --- |
| Invalid Request Format | Required fields missing | Add to errors list, return success=false |
| VON Not Found | Query returns no record | Skip notice, count as skipped, add to errors |
| Stage Transition Not Allowed | Transition not in stage_map | Skip notice, count as skipped, add to errors |

---

### 3.2.1 API Specification

#### Internal API for Toppan Cron

##### API updateToppanStages

| Field | Value |
| --- | --- |
| API Name | updateToppanStages |
| URL | UAT: https://uat-api.ocms.ura.gov.sg/ocms/v1/internal/toppan/update-stages <br> PRD: https://api.ocms.ura.gov.sg/ocms/v1/internal/toppan/update-stages |
| Description | Called by generate_toppan_letters cron job to update VON processing stages after Toppan files are generated |
| Method | POST |
| Header | `{ "Authorization": "Bearer [internal-token]", "Content-Type": "application/json" }` |
| Payload | `{ "noticeNumbers": ["N-001", "N-002", "N-003"], "currentStage": "DN1", "processingDate": "2025-12-19T00:30:00" }` |
| Response (Success) | `{ "totalNotices": 3, "automaticUpdates": 2, "manualUpdates": 1, "skipped": 0, "errors": null, "success": true }` |
| Response (Failure) | `{ "totalNotices": 3, "automaticUpdates": 0, "manualUpdates": 0, "skipped": 0, "errors": ["Unexpected error: [error details]"], "success": false }` |

**Request Rules:**
- noticeNumbers array cannot be empty
- currentStage is required
- processingDate must be valid datetime format

---

### 3.2.2 Data Mapping

#### Request Payload Fields

| Field | Data Type | Required | Description |
| --- | --- | --- | --- |
| noticeNumbers | array | Yes | Array of notice numbers to update |
| currentStage | varchar(10) | Yes | Current processing stage code |
| processingDate | timestamp | Yes | Date and time for processing stage update |

#### Validation Rules

| Rule ID | Field | Validation | Error Response |
| --- | --- | --- | --- |
| INT-001 | noticeNumbers | Array cannot be empty | success: false, errors: ["Notice numbers required"] |
| INT-002 | currentStage | Required | success: false, errors: ["Current stage required"] |
| INT-003 | processingDate | Valid datetime format | success: false, errors: ["Invalid date format"] |

#### Database Tables (Intranet Zone)

**ocms_valid_offence_notice:**

| Field Name | Data Type | Max Length | Nullable | Description | Updated By |
| --- | --- | --- | --- | --- | --- |
| notice_no | varchar | 10 | No | Notice number (Primary Key) | - |
| last_processing_stage | varchar | 10 | Yes | Updated to previous stage | Automatic update |
| last_processing_date | timestamp | - | Yes | Updated to previous stage date | Automatic update |
| next_processing_stage | varchar | 10 | Yes | Updated to next stage | Automatic update |
| upd_user_id | varchar | 50 | No | Audit user who updated record | ocmsiz_app_conn |
| upd_dtm | timestamp | - | No | Audit timestamp of update | System generated |

**ocms_change_of_processing:**

| Field Name | Data Type | Description | Query Purpose |
| --- | --- | --- | --- |
| notice_no | varchar(10) | Notice number | Check if manual change exists |
| date_of_change | timestamp | Date of change record | Check if changed today |

**Query Logic:**
```sql
SELECT notice_no FROM ocms_change_of_processing
WHERE notice_no = ? AND DATE(date_of_change) = CURRENT_DATE
```

**Query Standards**:
- ❌ Do NOT use SELECT *
- ✅ Always specify only the required fields as shown above

If record exists: Skip VON update (manual update already done)
If no record: Proceed with automatic VON update

---

### 3.2.3 Success Outcome

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

### 3.2.4 Error Handling

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

# Section 4 - Implementation Guidance

## 4.1 Edge Cases & Boundary Conditions

### 4.1.1 Section 1 - Manual Change Processing Stage (OCMS Staff Portal)

**Data Edge Cases**:
- Empty search results - Return empty array with total=0
- Duplicate stage change on same day - Reject with validation error (unless isConfirmation=true)
- Invalid offender type - Return error code OCMS.CPS.INVALID_TRANSITION
- Notice not found - Mark as FAILED with code OCMS.CPS.NOT_FOUND

**Date/Time Edge Cases**:
- Midnight boundary - Use transaction start time as reference for "today" checks
- Future-dated notices - Include in results if stage change allowed per business rules
- Backdated notices - Process normally, use current timestamp for change record

**Concurrent Processing**:
- Multiple OICs changing same notice simultaneously - Last update wins, log conflict warning
- Concurrent batch operations - Database transaction isolation ensures consistency
- Race condition on duplicate check - Use database-level locking or isConfirmation flag

**Batch Processing Edge Cases**:
- All items in batch fail validation - Return status=FAILED with individual error messages
- Partial batch success - Return status=PARTIAL with summary counts
- Single item batch - Process normally, return SUCCESS or FAILED accordingly

### 4.1.2 Section 2 - PLUS Integration

**Data Edge Cases**:
- Empty noticeNo array - Return error code OCMS-4000 immediately
- Stage transition not in stage_map - Return error before processing notices
- All notices in court stage - Return error with full list
- Mixed valid/invalid notices - Return partial success with count breakdown

**Integration Edge Cases**:
- Invalid source code (not "005") - Reject request immediately
- APIM token expiration - Handle 401 error and retry with refreshed token
- Network timeout - Return timeout error after configured wait period
- Malformed JSON payload - Return 400 Bad Request error

**Stage Transition Edge Cases**:
- CFC stage requested from PLUS - Block at PLUS portal level (do not show in dropdown)
- Stage transition exists but not for this offence type - Validate and reject
- Current stage does not match lastStageName - Return validation error

### 4.1.3 Section 3 - Toppan Cron Integration

**Data Edge Cases**:
- Empty noticeNumbers array - Return success=false with error message
- Notice not found in VON table - Skip notice, count as "skipped"
- Manual change record exists for today - Skip VON update, count as "manual update"
- Stage transition not allowed - Skip notice, add to errors list

**Timing Edge Cases**:
- Cron runs before midnight, check crosses midnight - Use cre_dtm DATE comparison
- Multiple cron executions on same day - Shedlock prevents concurrent runs
- Delayed cron execution - Process all pending notices, use current timestamp

**Job Monitoring Edge Cases**:
- Cron stuck/hanging - Shedlock timeout releases lock after max duration
- Partial completion due to error - Return summary with success=false and error details
- Zero notices to process - Return summary with all counts = 0, success=true

---

## 4.2 Service Layer Structure

**Recommended Service Architecture**:

```
Controller Layer (API Endpoints)
    ↓
Service Layer (Business Logic)
    ↓
Repository Layer (Database Operations)
```

**Service Components**:

1. **ChangeProcessingStageService** (Section 1)
   - searchNotices(criteria) → Returns eligible/ineligible lists
   - changeStage(items) → Processes batch change request
   - validateStageTransition(currentStage, newStage) → Checks stage_map
   - checkDuplicateChange(noticeNo, date) → Queries change records

2. **PlusIntegrationService** (Section 2)
   - processPlus ChangeRequest(request) → Handles PLUS API calls
   - validateSourceCode(source) → Verifies "005" code
   - transformPlusRequest(plusRequest) → Converts to internal format

3. **ToppanCronService** (Section 3)
   - updateToppanStages(noticeNumbers) → Processes automatic updates
   - checkManualChange(noticeNo, date) → Queries manual change records
   - skipOrUpdate(notice) → Decision logic for manual vs automatic

4. **CommonServices**:
   - StageMapValidator → Validates stage transitions
   - ReportGeneratorService → Generates Excel reports
   - AzureBlobService → Handles Azure storage operations
   - AuditService → Logs all stage changes

---

## 4.3 DTO Examples

**Request DTO (Change Processing Stage)**:
```java
public class ChangeStageRequest {
    private List<ChangeStageItem> items;
}

public class ChangeStageItem {
    private String noticeNo;        // Required, varchar(10)
    private String newStage;        // Optional, varchar(10)
    private String reason;          // Required, varchar(10)
    private String remark;          // Conditional (required if reason="OTH")
    private String source;          // Required, "PORTAL" or "PLUS"
    private Boolean dhMhaCheck;     // Optional, default false
    private Boolean isConfirmation; // Optional, default false
}
```

**Response DTO (Change Processing Stage)**:
```java
public class ChangeStageResponse {
    private String status;              // SUCCESS, PARTIAL, FAILED
    private SummaryDto summary;
    private List<ResultDto> results;
    private ReportDto report;           // Nullable
}

public class SummaryDto {
    private Integer requested;
    private Integer succeeded;
    private Integer failed;
}

public class ResultDto {
    private String noticeNo;
    private String outcome;             // UPDATED, FAILED
    private String previousStage;       // Nullable
    private String newStage;            // Nullable
    private String code;                // OCMS-2000, OCMS.CPS.*
    private String message;
}

public class ReportDto {
    private String url;                 // Signed URL
    private String expiresAt;           // ISO 8601 datetime
}
```

---

## 4.4 Transaction Boundaries

**Section 1 - Change Stage Subflow (Critical Transaction)**:

```
BEGIN TRANSACTION
  1. UPDATE ocms_valid_offence_notice (next_processing_stage, amount_payable)
  2. INSERT ocms_change_of_processing (change record)
  3. UPDATE ocms_offence_notice_owner_driver (if dhMhaCheck=true)
COMMIT TRANSACTION

ROLLBACK on any failure
```

**Transaction Isolation Level**: READ COMMITTED (default)

**Deadlock Prevention**:
- Always update tables in consistent order: VON → change_of_processing → ONOD
- Keep transactions short and focused
- Avoid nested transactions

**Transaction Timeout**: 30 seconds (configurable)

---

## 4.5 Error Handling Patterns

**Retry Logic (External APIs)**:
```java
public Response callExternalAPI(Request request) {
    int maxRetries = 3;
    int retryDelay = 1000; // 1 second

    for (int attempt = 1; attempt <= maxRetries; attempt++) {
        try {
            return executeAPICall(request);
        } catch (TimeoutException e) {
            if (attempt == maxRetries) {
                throw new ExternalAPIException("Max retries reached", e);
            }
            Thread.sleep(retryDelay * attempt); // Exponential backoff
        }
    }
}
```

**Validation Error Handling**:
```java
public ValidationResult validateRequest(ChangeStageRequest request) {
    List<String> errors = new ArrayList<>();

    if (request.getItems() == null || request.getItems().isEmpty()) {
        errors.add("OCMS.CPS.INVALID_FORMAT: Items list cannot be empty");
    }

    for (ChangeStageItem item : request.getItems()) {
        if (StringUtils.isEmpty(item.getNoticeNo())) {
            errors.add("OCMS.CPS.MISSING_DATA: noticeNo is required");
        }

        if ("OTH".equals(item.getReason()) && StringUtils.isEmpty(item.getRemark())) {
            errors.add("OCMS.CPS.REMARKS_REQUIRED: Remarks mandatory for reason=OTH");
        }
    }

    return new ValidationResult(errors.isEmpty(), errors);
}
```

**Database Error Handling**:
```java
public void updateVON(String noticeNo, String newStage) {
    try {
        int rowsAffected = jdbcTemplate.update(updateSql, newStage, noticeNo);
        if (rowsAffected == 0) {
            throw new DataNotFoundException("OCMS.CPS.NOT_FOUND: VON not found");
        }
    } catch (DataAccessException e) {
        log.error("Database error updating VON: {}", e.getMessage());
        throw new DatabaseException("OCMS.CPS.UNEXPECTED: Database error", e);
    }
}
```

**Batch Processing Error Handling**:
```java
public ChangeStageResponse processBatch(List<ChangeStageItem> items) {
    List<ResultDto> results = new ArrayList<>();
    int succeeded = 0, failed = 0;

    for (ChangeStageItem item : items) {
        try {
            processItem(item);
            results.add(new ResultDto(item.getNoticeNo(), "UPDATED", "OCMS-2000", "Success"));
            succeeded++;
        } catch (BusinessException e) {
            results.add(new ResultDto(item.getNoticeNo(), "FAILED", e.getCode(), e.getMessage()));
            failed++;
        }
    }

    String status = (failed == 0) ? "SUCCESS" : (succeeded > 0) ? "PARTIAL" : "FAILED";
    return new ChangeStageResponse(status, new SummaryDto(items.size(), succeeded, failed), results);
}
```

---

**End of OCMS 15 Technical Document**

---

## Appendix: Full-Sized Flowchart Images

NOTE: Full-sized flowchart images are appended on separate pages for better visibility and printing.

### Section 1.2 - High Level Flow
![Section 1.2 - High Level Flow](./images/section1-highlevel.png)

### Section 1.3 - Search Notices
![Section 1.3 - Search Notices](./images/section1-search-notices.png)

### Section 1.4 - Portal Validation
![Section 1.4 - Portal Validation](./images/section1-portal-validation.png)

### Section 1.5 - Backend Processing
![Section 1.5 - Backend Processing](./images/section1-backend-processing.png)

### Section 1.6 - Change Stage Subflow
![Section 1.6 - Change Stage Subflow](./images/section1-change-stage-subflow.png)

### Section 1.7 - Generate Report
![Section 1.7 - Generate Report](./images/section1-generate-report.png)

### Section 2.2 - PLUS High Level Flow
![Section 2.2 - PLUS High Level Flow](./images/section2-highlevel.png)

### Section 2.3 - PLUS Integration
![Section 2.3 - PLUS Integration](./images/section2-plus-integration.png)

### Section 3.2 - Toppan Integration
![Section 3.2 - Toppan Integration](./images/section3-toppan-integration.png)

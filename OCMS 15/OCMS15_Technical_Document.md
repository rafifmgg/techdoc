# OCMS 15 – Change Processing Stage

**Prepared by**

MGG SOFTWARE

---

## Version History

| Version | Updated By | Date | Changes |
| --- | --- | --- | --- |
| v1.0 | Technical Documentation | 18/01/2026 | Document Initiation |

---

## Table of Contents

| Section | Content | Pages |
| --- | --- | --- |
| 1 | Function: Change Processing Stage |  |
| 1.1 | Use Case |  |
| 1.2 | High-Level Processing Flow |  |
| 1.3 | Change Processing Stage Data |  |
| 1.4 | Change Processing Stage Rules |  |
| 1.5 | Validating CPS Requests |  |
| 1.5.1 | UI-Side Validation Flow |  |
| 1.5.2 | Backend-Side Validation Flow |  |
| 1.6 | Data Mapping |  |
| 1.7 | Success Outcome |  |
| 1.8 | Error Handling |  |
| 2 | OCMS Staff Portal Manual CPS |  |
| 2.1 | Use Case |  |
| 2.2 | Process Flow |  |
| 2.3 | API Specification |  |
| 2.4 | Data Mapping |  |
| 2.5 | Success Outcome |  |
| 2.6 | Error Handling |  |
| 2.7 | Eligible Stages by Role |  |
| 3 | Change Processing Stage Initiated by PLUS |  |
| 3.1 | Use Case |  |
| 3.2 | Process Flow |  |
| 3.3 | API Specification |  |
| 3.4 | Data Mapping |  |
| 3.5 | Success Outcome |  |
| 3.6 | Error Handling |  |
| 3.7 | PLUS Restrictions |  |
| 4 | Auto Change Processing Stage by Toppan |  |
| 4.1 | Use Case |  |
| 4.2 | Process Flow |  |
| 4.3 | API Specification |  |
| 4.4 | Data Mapping |  |
| 4.5 | Success Outcome |  |
| 4.6 | Error Handling |  |
| 4.7 | Manual vs Automatic Detection |  |
| 5 | Change Processing Stage Report |  |
| 5.1 | Use Case |  |
| 5.2 | Process Flow |  |
| 5.3 | API Specification |  |
| 5.4 | Data Mapping |  |
| 5.5 | Success Outcome |  |
| 5.6 | Error Handling |  |

---

## Document References

| Reference | Document Name | Description |
| --- | --- | --- |
| FD | v1.1_OCMS15_Change_Processing_Stage.docx | Functional Document containing detailed business rules, validation rules, and field specifications |

**Note:** For detailed validation rules, error code definitions, and complete field lists, refer to the Functional Document (FD).

---

# Section 1 – Function: Change Processing Stage

## 1.1 Use Case

1. Change Processing Stage (CPS) allows authorised users to manually change the processing stage of offence notices before they naturally progress through the Notice Processing Workflow.

2. A CPS may be applied when a Notice needs to be moved to a different processing stage due to operational reasons, such as speeding up or adjusting the notice processing timeline.

3. When CPS is applied, the notice's processing stage is updated along with an audit trail recording the change details including reason and authorised officer.

4. Notices can have their processing stage changed through the following modes:<br>a. Manual change by OICs via the OCMS Staff Portal<br>b. Manual change by PLMs via the PLUS Staff Portal<br>c. Automatic stage progression by the Toppan cron batch job

5. When a CPS is applied to an Offence Notice:<br>a. The Notice's last processing stage is updated to the new stage.<br>b. The Notice's previous stage information is backed up.<br>c. The next processing stage and date are recalculated.<br>d. An audit record is created in the ocms_change_of_processing table.

---

## 1.2 High-Level Processing Flow

<!-- Insert flow diagram here -->
![High-Level CPS Flow](./images/section1-highlevel-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Receive CPS Request | OCMS Backend receives the CPS request from the source | Staff Portal, PLUS, or Toppan Cron sends request |
| Validate CPS Request | Backend validates the request against eligibility rules | Check notice status, court stage, role eligibility |
| Process CPS | Backend updates database if validation passes | UPDATE ocms_valid_offence_notice, INSERT ocms_change_of_processing |
| Generate Report | Backend generates Excel report for successful changes | Create change stage report file |
| Return Response | Backend returns response to the requesting source | Success or error response with message |

---

## 1.3 Change Processing Stage Data

**Data Source:** `ocms_change_of_processing` table (Intranet zone)

| Field Name | Description | Sample Value | Data Type |
| --- | --- | --- | --- |
| notice_no* | Notice number | 500500303J | varchar(10) |
| date_of_change* | Date the stage change was applied | 2025-12-20 | datetime2(7) |
| last_processing_stage* | Previous processing stage before change | DN1 | varchar(3) |
| new_processing_stage* | New processing stage after change | DN2 | varchar(3) |
| reason_of_change | Reason code for the stage change | SUP | varchar(3) |
| authorised_officer* | Officer who authorised the change | JOHNLEE | varchar(50) |
| source* | Which system initiated the change | OCMS / PLUS / SYSTEM | varchar(8) |
| remarks | Additional remarks for the change | Speed up processing | varchar(200) |
| cre_date | Date and time the record was created | 2025-12-20T10:30:00 | datetime2(7) |
| cre_user_id | User or system that created the record | ocmsiz_app_conn | varchar(10) |
| upd_date | Date and time the record was updated | | datetime2(7) |
| upd_user_id | User or system that updated the record | | varchar(50) |

*Denotes mandatory fields

**source values:**
| Value | Description |
| --- | --- |
| OCMS | Change initiated manually via OCMS Staff Portal |
| PLUS | Change initiated via PLUS Staff Portal |
| SYSTEM | Change initiated automatically by Toppan cron batch |

---

## 1.4 Change Processing Stage Rules

### 1.4.1 Global Rules for Change Processing Stage

| Condition | Rule | Description |
| --- | --- | --- |
| Court Stage Notices | Not Allowed | Notices at court stages (CRT, CRC, CFI) cannot have their processing stage changed. |
| PS Active Notices | Configurable | Notices with active Permanent Suspension may be blocked from stage change depending on system configuration. |
| Stage by Role | Must Match | The new processing stage must be valid for the offender's role (Driver vs Owner/Hirer/Director). |
| Duplicate Change Warning | Warning Issued | If a notice already has a stage change record for the same day, a warning is issued and confirmation is required. |
| Remarks for OTH Reason | Mandatory | If reason of change is "OTH" (Other), remarks field becomes mandatory. |

### 1.4.2 Eligible Processing Stages by Role

| Role | Eligible Stages | Description |
| --- | --- | --- |
| DRIVER | DN1, DN2, DR3 | Driver Notice stages |
| OWNER | ROV, RD1, RD2, RR3 | Registered Owner stages |
| HIRER | ROV, RD1, RD2, RR3 | Hirer stages (same as Owner) |
| DIRECTOR | ROV, RD1, RD2, RR3 | Director stages (same as Owner) |

### 1.4.3 Non-Changeable Stages (Court Stages)

| Stage Code | Description |
| --- | --- |
| CRT | Court Stage |
| CRC | Court Case |
| CFI | Court Final |

---

## 1.5 Validating CPS Requests

### 1.5.1 UI-Side Validation Flow

<!-- Insert UI validation flow diagram here -->
![UI-Side Validation Flow](./images/section1-ui-validation-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Enter Search Criteria | OIC enters search criteria (at least one required) | Notice No, ID No, Vehicle No, Stage, Date |
| Validate Search Input | Check if at least one search criterion is provided | Mandatory validation |
| Search Notices | Call Search API to retrieve notices | Query VON and ONOD tables |
| Classify Results | Segregate eligible vs ineligible notices | Check court stage, PS status |
| Display Results | Show eligible and ineligible notices separately | User sees both lists |
| Select Notices | OIC selects eligible notices for stage change | Checkbox selection |
| Select New Stage | OIC selects new processing stage from dropdown | Must match role eligibility |
| Select Reason | OIC selects reason of change | Dropdown with reason codes |
| Enter Remarks | OIC enters remarks (mandatory if reason=OTH) | Text input field |
| Submit Request | Call Validate API then Submit API | Two-step submission |

### 1.5.2 Backend-Side Validation Flow

<!-- Insert Backend validation flow diagram here -->
![Backend-Side Validation Flow](./images/section1-backend-validation-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Receive Request | Backend receives CPS API request | JWT authentication |
| Validate Mandatory Fields | Check all required fields are present | Return 400 if missing |
| Query Notice | Query VON and ONOD by notice number | Database lookup |
| Check Notice Exists | Verify notice exists in system | Return NOT_FOUND if not found |
| Resolve Role | Determine offender role from ONOD | DRIVER, OWNER, HIRER, DIRECTOR |
| Check Court Stage | Verify notice is not at court stage | Return COURT_STAGE error if at court |
| Check PS Status | Check if PS blocks stage change (configurable) | Return PS_BLOCKED if blocked |
| Validate Stage Eligibility | Check if new stage is valid for role | Return INELIGIBLE_STAGE if invalid |
| Check Duplicate | Check for existing change on same day | Return warning or proceed |
| Process Change | Update VON and insert audit record | Database transaction |
| Return Response | Return success or error response | Include report URL if successful |

### 1.5.3 Key Validation Principles

- All requests to change processing stage will be validated.

- When a CPS request is initiated from a Staff Portal (OCMS or PLUS):
  - The Staff Portal will perform the first round of validation based on the valid offence notice data the portal received from the OCMS backend.
  - If the Notice(s) are determined to be eligible for CPS, the Portal will trigger the CPS request to the OCMS backend, which will also perform another round of validation against all eligibility rules.

- When a CPS request is initiated from Toppan cron, the OCMS backend will also validate the request to determine if the stage change can be applied.

---

## 1.6 Data Mapping

| Zone | Database Table | Field Name |
| --- | --- | --- |
| Intranet | ocms_valid_offence_notice | notice_no |
| Intranet | ocms_valid_offence_notice | prev_processing_stage |
| Intranet | ocms_valid_offence_notice | prev_processing_date |
| Intranet | ocms_valid_offence_notice | last_processing_stage |
| Intranet | ocms_valid_offence_notice | last_processing_date |
| Intranet | ocms_valid_offence_notice | next_processing_stage |
| Intranet | ocms_valid_offence_notice | next_processing_date |
| Intranet | ocms_valid_offence_notice | amount_payable |
| Intranet | ocms_change_of_processing | notice_no |
| Intranet | ocms_change_of_processing | date_of_change |
| Intranet | ocms_change_of_processing | last_processing_stage |
| Intranet | ocms_change_of_processing | new_processing_stage |
| Intranet | ocms_change_of_processing | reason_of_change |
| Intranet | ocms_change_of_processing | authorised_officer |
| Intranet | ocms_change_of_processing | source |
| Intranet | ocms_change_of_processing | remarks |
| Intranet | ocms_change_of_processing | cre_date |
| Intranet | ocms_change_of_processing | cre_user_id |
| Intranet | ocms_offence_notice_owner_driver | notice_no |
| Intranet | ocms_offence_notice_owner_driver | owner_driver_indicator |
| Intranet | ocms_stage_map | last_processing_stage |
| Intranet | ocms_stage_map | next_processing_stage |
| Intranet | ocms_parameter | parameter_id |
| Intranet | ocms_parameter | code |
| Intranet | ocms_parameter | value |

**Note:**
- Audit user fields (cre_user_id, upd_user_id) use database connection user:
  - Intranet: **ocmsiz_app_conn**
  - Internet: **ocmsez_app_conn**
- Insert order: Parent table first (UPDATE ocms_valid_offence_notice), then child table (INSERT ocms_change_of_processing)

---

## 1.7 Success Outcome

- CPS record successfully inserted into ocms_change_of_processing table
- ocms_valid_offence_notice updated with new processing stages and dates
- Previous stage information backed up to prev_processing_stage and prev_processing_date
- Next processing stage and date calculated from ocms_parameter
- Amount payable recalculated (for automatic changes)
- Success response returned to requesting source (Staff Portal, PLUS, or Toppan)
- Excel report generated with change details
- The workflow reaches the End state without triggering any error-handling paths

---

## 1.8 Error Handling

### Application Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Invalid Notice Number | Notice number not found in VON or ONOD | Return error response, no record created |
| Court Stage Notice | Notice is at CRT, CRC, or CFI stage | Return error response, CPS not applied |
| PS Blocked | Notice has active PS and system configured to block | Return error response, CPS not applied |
| Role Conflict | Cannot determine offender role from ONOD | Return error response, CPS not applied |
| Ineligible Stage | New stage not valid for offender's role | Return error response, CPS not applied |
| Missing Remarks | Reason is OTH but remarks not provided | Return error response, CPS not applied |
| Missing Search Criteria | No search criteria provided | Return error response, search not executed |
| Database Error | Unable to insert/update database record | Log error, return error response |

### API Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| General Server Error | OCMS-5000 | Something went wrong on our end. Please try again later. | Server encountered an unexpected condition |
| Bad Request | OCMS-4000 | The request could not be processed. Please check and try again. | Invalid request syntax |
| Unauthorized Access | OCMS-4001 | You are not authorized to access this resource. | Authentication failed |
| Not Found | OCMS.CPS.ELIG.NOT_FOUND | Notice not found. | Notice number does not exist |
| Court Stage | OCMS.CPS.ELIG.COURT_STAGE | Notice is at court stage. | Cannot change court stage notices |
| PS Blocked | OCMS.CPS.ELIG.PS_BLOCKED | Notice has active permanent suspension. | PS blocks stage change |
| Role Conflict | OCMS.CPS.ELIG.ROLE_CONFLICT | Cannot determine offender role. | Role resolution failed |
| Ineligible Stage | OCMS.CPS.ELIG.INELIGIBLE_STAGE | Stage not valid for offender role. | Stage/role mismatch |
| Remarks Required | OCMS.CPS.REMARKS_REQUIRED | Remarks are mandatory for reason OTH. | Missing remarks |
| Existing Change | OCMS.CPS.ELIG.EXISTING_CHANGE_TODAY | Existing stage change found for today. | Duplicate warning |

---

# Section 2 – OCMS Staff Portal Manual CPS

## 2.1 Use Case

1. The manual CPS function in the OCMS Staff Portal allows authorised OICs to change the processing stage of Notices.

2. Authorised users can search for the notices using the Staff Portal's Search Notice function by:<br>a. Entering search parameters to retrieve one or more Notices (Notice No, ID No, Vehicle No, Current Processing Stage, Date of Current Processing Stage)<br>b. At least one search criterion must be provided

3. From the search results, Users can see notices segregated into eligible and ineligible lists based on eligibility criteria (court stage, PS status, etc.).

4. Users select eligible notices and specify:<br>a. New Processing Stage (from dropdown based on offender role)<br>b. Reason of Change (from dropdown)<br>c. Remarks (mandatory if reason is OTH)

5. When CPS is initiated from the Staff Portal, the Portal validates the eligibility before calling the Validate API to get changeable vs non-changeable status.

6. After confirmation, the Submit API is called to process the stage change.

7. If CPS is successful, Users will be presented with a success message and a link to download the Excel report; an error message will be displayed if the change is not successful.

---

## 2.2 Process Flow

<!-- Insert flow diagram here -->
![Staff Portal Manual CPS Flow](./images/section2-manual-cps-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Search Notice | OIC navigates to Change Processing Stage Page and searches for notices | Enter search criteria (at least one required) |
| Display Results | System displays eligible and ineligible notices separately | Segregated results with reasons |
| Select Notice(s) | OIC selects eligible notice(s) from search results | Checkbox selection |
| Select New Stage | OIC selects new processing stage from dropdown | Must match role eligibility |
| Select Reason | OIC selects reason of change from dropdown | SUP, OTH, etc. |
| Enter Remarks | OIC enters remarks (mandatory if reason=OTH) | Text input |
| Submit | OIC clicks Submit to initiate validation | Call Validate API |
| Display Validation | System displays changeable vs non-changeable notices | Show validation results |
| Confirm | OIC confirms to proceed with changeable notices | Confirmation dialog |
| Backend Processing | Backend validates request and processes CPS | UPDATE VON, INSERT audit |
| Response | Display success message with report download link | Show result and report URL |

---

## 2.3 API Specification

### API for eService

#### API Search Notices for Change Processing Stage

| Field | Value |
| --- | --- |
| API Name | change-processing-stage-search |
| URL | UAT: https://parking2.ura.gov.sg/ocms/v1/change-processing-stage/search <br> PRD: https://parking.ura.gov.sg/ocms/v1/change-processing-stage/search |
| Description | Search notices and return segregated lists of eligible vs ineligible notices for CPS |
| Method | POST |
| Header | `{ "Content-Type": "application/json", "Authorization": "Bearer <JWT_TOKEN>" }` |
| Payload | `{ "noticeNo": "N-001", "idNo": "S1234567A", "vehicleNo": "SBA1234A", "currentProcessingStage": "DN1", "dateOfCurrentProcessingStage": "2025-12-20" }` |
| Response | `{ "appCode": "OCMS-2000", "message": "Success", "eligibleNotices": [...], "ineligibleNotices": [...], "summary": { "total": 2, "eligible": 1, "ineligible": 1 } }` |
| Response (Empty) | `{ "appCode": "OCMS-2000", "message": "Success", "eligibleNotices": [], "ineligibleNotices": [], "summary": { "total": 0, "eligible": 0, "ineligible": 0 } }` |
| Response Failure | `{ "appCode": "OCMS-4000", "message": "At least one search criterion is required" }` |

**Request Parameter Data:**

| Field Name | Type | Required | Max Length | Description |
| --- | --- | --- | --- | --- |
| noticeNo | String | No | 10 | Notice number |
| idNo | String | No | 20 | Offender ID number (NRIC/FIN/Passport) |
| vehicleNo | String | No | 14 | Vehicle registration number |
| currentProcessingStage | String | No | 3 | Current processing stage code |
| dateOfCurrentProcessingStage | Date | No | - | Date of current processing stage (yyyy-MM-dd) |

**Note:** At least one search criterion is required.

**Response Data - Eligible Notices:**

| Field Name | Type | Description |
| --- | --- | --- |
| noticeNo | String | Notice number |
| offenceType | String | Offence type code |
| offenceDateTime | DateTime | Offence date and time |
| offenderName | String | Offender name |
| offenderId | String | Offender ID number |
| vehicleNo | String | Vehicle number |
| currentProcessingStage | String | Current stage code |
| currentProcessingStageDate | DateTime | Current stage date |
| suspensionType | String | Suspension type (TS/PS/null) |
| ownerDriverIndicator | String | D=Driver, O=Owner, H=Hirer, DIR=Director |

**Response Data - Ineligible Notices:**

| Field Name | Type | Description |
| --- | --- | --- |
| noticeNo | String | Notice number |
| currentProcessingStage | String | Current stage code |
| reasonCode | String | Reason code for ineligibility |
| reasonMessage | String | Human-readable reason |

---

#### API Validate Change Processing Stage

| Field | Value |
| --- | --- |
| API Name | change-processing-stage-validate |
| URL | UAT: https://parking2.ura.gov.sg/ocms/v1/change-processing-stage/validate <br> PRD: https://parking.ura.gov.sg/ocms/v1/change-processing-stage/validate |
| Description | Validate notices before submission to identify changeable vs non-changeable |
| Method | POST |
| Header | `{ "Content-Type": "application/json", "Authorization": "Bearer <JWT_TOKEN>" }` |
| Payload | `{ "notices": [{ "noticeNo": "N-001", "currentStage": "DN1", "offenderType": "DRIVER" }], "newProcessingStage": "DN2", "reasonOfChange": "SUP", "remarks": "Speed up processing" }` |
| Response | `{ "appCode": "OCMS-2000", "message": "Success", "changeableNotices": [...], "nonChangeableNotices": [...], "summary": { "total": 2, "changeable": 1, "nonChangeable": 1 } }` |
| Response Failure | `{ "appCode": "OCMS-4000", "message": "Notices array cannot be empty" }` |

**Request Parameter Data:**

| Field Name | Type | Required | Max Length | Description |
| --- | --- | --- | --- | --- |
| notices | Array | Yes | - | List of notices to validate |
| notices[].noticeNo | String | Yes | 10 | Notice number |
| notices[].currentStage | String | No | 3 | Current stage (fetched from DB if not provided) |
| notices[].offenderType | String | No | - | DRIVER/OWNER/HIRER/DIRECTOR |
| newProcessingStage | String | Yes | 3 | Target processing stage |
| reasonOfChange | String | No | 3 | Reason code |
| remarks | String | Conditional | 200 | Remarks (required if reasonOfChange=OTH) |

---

#### API Submit Change Processing Stage

| Field | Value |
| --- | --- |
| API Name | change-processing-stage |
| URL | UAT: https://parking2.ura.gov.sg/ocms/v1/change-processing-stage <br> PRD: https://parking.ura.gov.sg/ocms/v1/change-processing-stage |
| Description | Submit batch request to change processing stage for multiple notices |
| Method | POST |
| Header | `{ "Content-Type": "application/json", "Authorization": "Bearer <JWT_TOKEN>" }` |
| Payload | `{ "items": [{ "noticeNo": "N-001", "newStage": "DN2", "reason": "SUP", "remark": "Verified by AO", "isConfirmation": false }] }` |
| Response | `{ "totalProcessed": 1, "successCount": 1, "errorCount": 0, "results": [{ "noticeNo": "N-001", "appCode": "OCMS-2000", "message": "Stage changed successfully", "previousStage": "DN1", "newStage": "DN2" }], "reportUrl": "https://blob.storage/reports/change-stage/ChangeStageReport_20251220.xlsx" }` |
| Response Failure | `{ "totalProcessed": 1, "successCount": 0, "errorCount": 1, "results": [{ "noticeNo": "N-001", "appCode": "OCMS.CPS.ELIG.COURT_STAGE", "message": "Notice is at court stage" }] }` |

**Request Parameter Data:**

| Field Name | Type | Required | Max Length | Description |
| --- | --- | --- | --- | --- |
| items | Array | Yes | - | List of notices to change |
| items[].noticeNo | String | Yes | 10 | Notice number |
| items[].newStage | String | No | 3 | New stage (can be derived from StageMap) |
| items[].reason | String | No | 3 | Reason code |
| items[].remark | String | No | 200 | Additional remarks |
| items[].isConfirmation | Boolean | No | - | Confirmation flag for duplicate override |

---

#### API Get Reason of Change List

| Field | Value |
| --- | --- |
| API Name | change-stage-reason-list |
| URL | UAT: https://parking2.ura.gov.sg/ocms/v1/change-stage-reason-list <br> PRD: https://parking.ura.gov.sg/ocms/v1/change-stage-reason-list |
| Description | Get list of reason codes for change processing stage dropdown |
| Method | POST |
| Header | `{ "Content-Type": "application/json", "Authorization": "Bearer <JWT_TOKEN>" }` |
| Payload | `{ "status": "A" }` |
| Response | `{ "appCode": "OCMS-2000", "message": "Success", "data": [{ "code": "SUP", "description": "Speed up processing" }, { "code": "OTH", "description": "Other" }] }` |
| Response (Empty) | `{ "appCode": "OCMS-2000", "message": "Success", "data": [] }` |
| Response Failure | `{ "appCode": "OCMS-5000", "message": "Unable to retrieve reason list" }` |

---

## 2.4 Data Mapping

| Zone | Database Table | Field Name |
| --- | --- | --- |
| Intranet | ocms_valid_offence_notice | notice_no |
| Intranet | ocms_valid_offence_notice | prev_processing_stage |
| Intranet | ocms_valid_offence_notice | prev_processing_date |
| Intranet | ocms_valid_offence_notice | last_processing_stage |
| Intranet | ocms_valid_offence_notice | last_processing_date |
| Intranet | ocms_valid_offence_notice | next_processing_stage |
| Intranet | ocms_valid_offence_notice | next_processing_date |
| Intranet | ocms_valid_offence_notice | amount_payable |
| Intranet | ocms_change_of_processing | notice_no |
| Intranet | ocms_change_of_processing | date_of_change |
| Intranet | ocms_change_of_processing | last_processing_stage |
| Intranet | ocms_change_of_processing | new_processing_stage |
| Intranet | ocms_change_of_processing | reason_of_change |
| Intranet | ocms_change_of_processing | authorised_officer |
| Intranet | ocms_change_of_processing | source |
| Intranet | ocms_change_of_processing | remarks |
| Intranet | ocms_change_of_processing | cre_date |
| Intranet | ocms_change_of_processing | cre_user_id |

**Note:** Audit user fields (cre_user_id, upd_user_id) use database connection user: **ocmsiz_app_conn** (Intranet)

---

## 2.5 Success Outcome

- User successfully navigates to Change Processing Stage Page
- Notice(s) retrieved based on search criteria
- Frontend validation passes for selected notice(s)
- Backend validation confirms notices are changeable
- CPS record inserted into ocms_change_of_processing table
- ocms_valid_offence_notice updated with new processing stages
- Excel report generated and uploaded to Azure Blob Storage
- Success message displayed to user with report download link
- The workflow reaches the End state without triggering any error-handling paths

---

## 2.6 Error Handling

### Application Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| No Search Criteria | No search criterion provided | Display error message, search not executed |
| No Results Found | Search returns empty results | Display "No records found" message |
| No Eligible Notices | All notices are ineligible | Display ineligible list with reasons |
| Stage Not Valid | Selected stage not valid for offender role | Display validation error |
| Missing Remarks | Reason is OTH but remarks not entered | Display validation error |
| Backend Validation Failed | Backend rejects CPS request | Display error message from backend response |
| Connection Error | Unable to connect to backend API | Display system error message, prompt user to retry |

### API Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| General Server Error | OCMS-5000 | Something went wrong. Please try again later. | Server error |
| Validation Error | OCMS-4000 | Invalid request. Please check and try again. | Backend validation failed |
| Connection Error | OCMS-5001 | Service temporarily unavailable. | Unable to connect to backend |

Refer to Section 1.8 for complete backend error handling scenarios.

---

## 2.7 Eligible Stages by Role

| Role | Eligible Stages | Typical Progression |
| --- | --- | --- |
| DRIVER | DN1, DN2, DR3 | DN1 → DN2 → DR3 |
| OWNER | ROV, RD1, RD2, RR3 | ROV → RD1 → RD2 → RR3 |
| HIRER | ROV, RD1, RD2, RR3 | ROV → RD1 → RD2 → RR3 |
| DIRECTOR | ROV, RD1, RD2, RR3 | ROV → RD1 → RD2 → RR3 |

**Stage Descriptions:**

| Stage Code | Description | Role |
| --- | --- | --- |
| DN1 | Driver Notice 1st Reminder | DRIVER |
| DN2 | Driver Notice 2nd Reminder | DRIVER |
| DR3 | Driver 3rd Reminder | DRIVER |
| ROV | Registered Owner Vehicle | OWNER/HIRER/DIRECTOR |
| RD1 | Registered Owner 1st Reminder | OWNER/HIRER/DIRECTOR |
| RD2 | Registered Owner 2nd Reminder | OWNER/HIRER/DIRECTOR |
| RR3 | Registered Owner 3rd Reminder | OWNER/HIRER/DIRECTOR |

---

# Section 3 – Change Processing Stage Initiated by PLUS

## 3.1 Use Case

1. PLUS can apply CPS via an API provided by OCMS.

2. The PLUS system and PLUS Users will initiate CPS under the following circumstances:<br>a. PLMs need to change the processing stage of a notice during appeal processing<br>b. Batch stage changes for multiple notices

3. When a CPS is initiated through the PLUS Staff Portal or backend, the PLUS system shall provide the stage change details, which will be logged in OCMS in the ocms_change_of_processing table.

4. Upon receiving the request, OCMS validates the submitted data including stage map validation and PLUS-specific restrictions.

5. PLUS can apply CPS to multiple Notices simultaneously.

6. PLUS has specific restrictions, such as not being allowed to change stage to CFC (Claim for Complaint).

---

## 3.2 Process Flow

<!-- Insert flow diagram here -->
![PLUS CPS Flow](./images/section3-plus-cps-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| PLM initiates CPS | PLM accesses PLUS Staff Portal and initiates CPS for notice(s) | Access stage change module |
| PLUS validates eligibility | PLUS Staff Portal validates eligibility based on notice data | Check processing stage restrictions |
| Enter CPS Data | PLM provides stage change details | Stage, reason, remarks |
| Check PLUS Restrictions | OCMS checks PLUS-specific restrictions (CFC not allowed) | Return OCMS-4004 if CFC from PLUS |
| Check Skip Condition | Check if offence type and stage combination should skip | Return SUCCESS without processing |
| Stage Map Validation | Validate stage transition against ocms_stage_map | Return OCMS-4000 if invalid |
| Backend Processing | If valid, backend updates VON and inserts audit record | UPDATE parent, INSERT child table |
| Response | OCMS Backend returns success or error response to PLUS | Return result to PLUS system |

---

## 3.3 API Specification

### API Provide

#### API PLUS Change Processing Stage

| Field | Value |
| --- | --- |
| API Name | plus-change-processing-stage |
| URL | UAT: https://parking2.ura.gov.sg/ocms/v1/external/plus/change-processing-stage <br> PRD: https://parking.ura.gov.sg/ocms/v1/external/plus/change-processing-stage |
| Description | External API for PLUS Portal to manually change processing stages |
| Method | POST |
| Header | `{ "Authorization": "Bearer [token]", "Content-Type": "application/json", "Ocp-Apim-Subscription-Key": "[APIM key]" }` |
| Payload | `{ "noticeNo": ["N-001", "N-002"], "lastStageName": "DN1", "nextStageName": "DN2", "lastStageDate": "2025-09-25T06:58:42", "newStageDate": "2025-09-30T06:58:42", "offenceType": "D", "source": "005" }` |
| Response | `{ "appCode": "OCMS-2000", "message": "Stage change processed successfully", "noticeCount": 2 }` |
| Response Failure | `{ "appCode": "OCMS-4004", "message": "Stage change to CFC is not allowed from PLUS source" }` |

### Request Parameter Data

| Field Name | Type | Required | Max Length | Description |
| --- | --- | --- | --- | --- |
| noticeNo | Array | Yes | - | List of notice numbers |
| lastStageName | String | Yes | 3 | Current/last processing stage |
| nextStageName | String | Yes | 3 | New processing stage |
| lastStageDate | DateTime | No | - | Last stage date (ISO 8601) |
| newStageDate | DateTime | No | - | New stage date (ISO 8601) |
| offenceType | String | Yes | - | O=Owner, D=Driver, H=Hirer, DIR=Director |
| source | String | Yes | 8 | Source code (005 for PLUS) |

---

## 3.4 Data Mapping

| Zone | Database Table | Field Name |
| --- | --- | --- |
| Intranet | ocms_valid_offence_notice | notice_no |
| Intranet | ocms_valid_offence_notice | last_processing_stage |
| Intranet | ocms_valid_offence_notice | last_processing_date |
| Intranet | ocms_valid_offence_notice | next_processing_stage |
| Intranet | ocms_valid_offence_notice | next_processing_date |
| Intranet | ocms_change_of_processing | notice_no |
| Intranet | ocms_change_of_processing | date_of_change |
| Intranet | ocms_change_of_processing | last_processing_stage |
| Intranet | ocms_change_of_processing | new_processing_stage |
| Intranet | ocms_change_of_processing | source |
| Intranet | ocms_change_of_processing | cre_date |
| Intranet | ocms_change_of_processing | cre_user_id |
| Intranet | ocms_stage_map | last_processing_stage |
| Intranet | ocms_stage_map | next_processing_stage |

**Note:** Audit user fields (cre_user_id, upd_user_id) use database connection user: **ocmsiz_app_conn** (Intranet)

---

## 3.5 Success Outcome

- PLUS request validation passes
- PLUS-specific restrictions checked (CFC not allowed)
- Stage map validation passes
- CPS request successfully processed
- CPS record inserted into ocms_change_of_processing table
- ocms_valid_offence_notice updated with new processing stages
- Success response returned to PLUS
- The workflow reaches the End state without triggering any error-handling paths

---

## 3.6 Error Handling

### Application Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| CFC Not Allowed | PLUS tries to change stage to CFC | Return OCMS-4004 error |
| Stage Map Invalid | Stage transition not found in ocms_stage_map | Return OCMS-4000 error |
| Notice Not Found | Notice number does not exist | Return error response |
| PLUS Validation Failed | Notice does not meet CPS eligibility on PLUS side | Display error message on PLUS Staff Portal |
| Token Expired | Authentication token has expired | Auto refresh token and continue processing |

### API Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| General Server Error | OCMS-5000 | Something went wrong on our end. Please try again later. | Server encountered an unexpected condition |
| Bad Request | OCMS-4000 | Stage transition not allowed. | Invalid stage transition |
| CFC Restriction | OCMS-4004 | Stage change to CFC is not allowed from PLUS source. | PLUS-specific restriction |
| Unauthorized Access | OCMS-4001 | You are not authorized to access this resource. | Authentication failed |

---

## 3.7 PLUS Restrictions

| Restriction | Condition | Error Code | Description |
| --- | --- | --- | --- |
| CFC Not Allowed | source=005 AND nextStageName=CFC | OCMS-4004 | PLUS cannot change stage to CFC (Claim for Complaint) |
| Skip Condition | offenceType=U AND nextStage IN (DN1,DN2,DR3,CPC) | N/A (SUCCESS) | Skip processing and return success |
| Stage Map Invalid | Transition not in ocms_stage_map | OCMS-4000 | Stage transition must be defined in stage map |

---

# Section 4 – Auto Change Processing Stage by Toppan

## 4.1 Use Case

1. The Toppan cron batch job automatically triggers processing stage changes for notices that have been processed by the Toppan letter generation system.

2. When notices reach their next processing date and Toppan generates reminder letters, the stage is automatically advanced.

3. The Toppan cron detects whether a notice has a manual change record for the same day to determine if the stage change is MANUAL or AUTOMATIC.

4. For MANUAL changes (already changed via Staff Portal or PLUS), only the stage is updated without recalculating amount_payable.

5. For AUTOMATIC changes (no prior manual change), both stage and amount_payable are updated.

---

## 4.2 Process Flow

<!-- Insert flow diagram here -->
![Toppan Auto CPS Flow](./images/section4-toppan-cps-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Toppan Batch Start | Toppan cron batch job starts processing | Scheduled batch execution |
| Generate Letters | Generate Toppan enquiry files for notices | Letter generation process |
| Collect Notice Numbers | Collect list of processed notice numbers | Batch notice collection |
| Call Internal API | Call OCMS internal Toppan API | API call with notice list |
| Query VON | For each notice, query VON by notice number | Database lookup |
| Check Stage Match | Verify current stage matches expected stage | Skip if mismatch |
| Query Change Record | Query ocms_change_of_processing for manual changes | Check for manual change today |
| Determine Update Type | If manual change exists: MANUAL, else: AUTOMATIC | Manual vs Automatic detection |
| Update VON (Manual) | Update VON stage only (NOT amount_payable) | Stage-only update |
| Update VON (Auto) | Update VON stage AND calculate amount_payable | Full update with amount |
| Insert Audit Record | Insert/update audit trail record | Log the change |
| Compile Statistics | Compile automatic, manual, skipped counts | Statistics compilation |
| Return Response | Return response with statistics | Process complete |

---

## 4.3 API Specification

### Internal API

#### API Toppan Stage Update

| Field | Value |
| --- | --- |
| API Name | toppan-update-stages |
| URL | UAT: https://parking2.ura.gov.sg/ocms/v1/internal/toppan/update-stages <br> PRD: https://parking.ura.gov.sg/ocms/v1/internal/toppan/update-stages |
| Description | Internal API called by Toppan cron to update VON processing stages |
| Method | POST |
| Header | `{ "Authorization": "Internal", "Content-Type": "application/json" }` |
| Payload | `{ "noticeNumbers": ["N-001", "N-002", "N-003"], "currentStage": "DN1", "processingDate": "2025-12-19T00:30:00" }` |
| Response | `{ "appCode": "OCMS-2000", "message": "Success", "totalNotices": 3, "automaticUpdates": 2, "manualUpdates": 1, "skipped": 0, "errors": null, "success": true }` |
| Response Failure | `{ "appCode": "OCMS-5000", "message": "Batch processing failed", "totalNotices": 3, "automaticUpdates": 0, "manualUpdates": 0, "skipped": 0, "errors": ["Error processing N-001"], "success": false }` |

### Request Parameter Data

| Field Name | Type | Required | Description |
| --- | --- | --- | --- |
| noticeNumbers | Array | Yes | List of notice numbers processed by Toppan |
| currentStage | String | Yes | Current processing stage |
| processingDate | DateTime | Yes | Processing date/time (ISO 8601) |

### Response Data

| Field Name | Type | Description |
| --- | --- | --- |
| totalNotices | Integer | Total notices in request |
| automaticUpdates | Integer | Notices updated as automatic (with amount_payable calculation) |
| manualUpdates | Integer | Notices updated as manual (without amount_payable change) |
| skipped | Integer | Notices skipped (not found, stage mismatch) |
| errors | Array | List of error messages (null if no errors) |
| success | Boolean | Overall success flag |

---

## 4.4 Data Mapping

| Zone | Database Table | Field Name |
| --- | --- | --- |
| Intranet | ocms_valid_offence_notice | notice_no |
| Intranet | ocms_valid_offence_notice | last_processing_stage |
| Intranet | ocms_valid_offence_notice | last_processing_date |
| Intranet | ocms_valid_offence_notice | next_processing_stage |
| Intranet | ocms_valid_offence_notice | next_processing_date |
| Intranet | ocms_valid_offence_notice | amount_payable |
| Intranet | ocms_change_of_processing | notice_no |
| Intranet | ocms_change_of_processing | date_of_change |
| Intranet | ocms_change_of_processing | new_processing_stage |
| Intranet | ocms_change_of_processing | source |

**Note:** Audit user fields (cre_user_id, upd_user_id) use database connection user: **ocmsiz_app_conn** (Intranet - Backend)

---

## 4.5 Success Outcome

- Toppan batch job executed successfully
- Notices processed and categorised correctly (automatic vs manual)
- For MANUAL notices: Stage updated without amount_payable change
- For AUTOMATIC notices: Stage and amount_payable both updated
- Audit records created/updated in ocms_change_of_processing
- Statistics returned with counts
- The workflow reaches the End state without triggering any error-handling paths

---

## 4.6 Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Notice Not Found | Notice number does not exist in VON | Add to skipped list, continue batch |
| Stage Mismatch | VON current stage doesn't match expected stage | Add to skipped list, continue batch |
| Database Error | Unable to update database record | Log error, add to errors list, continue batch |
| Batch Failure | Critical error during batch processing | Return failure response with error details |

---

## 4.7 Manual vs Automatic Detection

| Type | Condition | VON Update |
| --- | --- | --- |
| MANUAL | Change record exists in ocms_change_of_processing with source IN (OCMS, PLUS) on same date | Stage only (amount_payable NOT touched) |
| AUTOMATIC | No manual change record for same date | Stage + amount_payable recalculated |

**Detection Logic:**

```
MANUAL_SOURCES = {"OCMS", "PLUS"}

changeRecord = SELECT * FROM ocms_change_of_processing
               WHERE notice_no = noticeNo
               AND date_of_change = processingDate
               AND new_processing_stage = currentStage

IF changeRecord EXISTS AND changeRecord.source IN MANUAL_SOURCES
THEN isManual = TRUE  // Update stage only, DO NOT touch amount_payable
ELSE isManual = FALSE // Update stage AND calculate amount_payable
```

---

# Section 5 – Change Processing Stage Report

## 5.1 Use Case

1. The OCMS Staff Portal's Report module allows users to export a report to review stage changes that were applied based on retrieval criteria.

2. Users can search for change stage records by date range (maximum 90 days).

3. The report includes details such as notice number, previous stage, new stage, date of change, reason, authorised officer, and source.

---

## 5.2 Process Flow

<!-- Insert flow diagram here -->
![CPS Report Flow](./images/section5-cps-report-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Access Report Module | User navigates to OCMS Staff Portal's Report module | Navigate to Report menu |
| Select Report Type | User selects Change Processing Stage Report | Choose CPS Report option |
| Enter Search Criteria | User enters date range (mandatory, max 90 days) | Date range validation |
| Validate Input | System validates search parameters | Check date range limit |
| Execute Query | System retrieves CPS records from ocms_change_of_processing | Query with date filter |
| Generate Report | System compiles report data | Join with VON for notice details |
| Display Results | Display available reports with download links | Show report list |
| Download | User downloads Excel report | Download file |

---

## 5.3 API Specification

### API for eService

#### API Get Change Stage Reports

| Field | Value |
| --- | --- |
| API Name | change-processing-stage-reports |
| URL | UAT: https://parking2.ura.gov.sg/ocms/v1/change-processing-stage/reports <br> PRD: https://parking.ura.gov.sg/ocms/v1/change-processing-stage/reports |
| Description | Retrieve list of change stage reports by date range |
| Method | POST |
| Header | `{ "Content-Type": "application/json", "Authorization": "Bearer <JWT_TOKEN>" }` |
| Payload | `{ "startDate": "2025-10-01", "endDate": "2025-12-31" }` |
| Response | `{ "appCode": "OCMS-2000", "message": "Success", "reports": [{ "reportDate": "2025-12-20", "generatedBy": "USER01", "noticeCount": 15, "reportUrl": "reports/change-stage/ChangeStageReport_20251220_USER01.xlsx" }], "totalReports": 1, "totalNotices": 15 }` |
| Response (Empty) | `{ "appCode": "OCMS-2000", "message": "Success", "reports": [], "totalReports": 0, "totalNotices": 0 }` |
| Response Failure | `{ "appCode": "OCMS-4000", "message": "Date range cannot exceed 90 days" }` |

### Request Parameter Data

| Field Name | Type | Required | Description |
| --- | --- | --- | --- |
| startDate | Date | Yes | Start date (yyyy-MM-dd) |
| endDate | Date | Yes | End date (yyyy-MM-dd), max 90 days from start |

---

#### API Download Change Stage Report

| Field | Value |
| --- | --- |
| API Name | change-processing-stage-report-download |
| URL | UAT: https://parking2.ura.gov.sg/ocms/v1/change-processing-stage/report/{reportId} <br> PRD: https://parking.ura.gov.sg/ocms/v1/change-processing-stage/report/{reportId} |
| Description | Download individual change stage report as Excel file |
| Method | POST |
| Header (Request) | `{ "Content-Type": "application/json", "Authorization": "Bearer <JWT_TOKEN>" }` |
| Header (Response) | `{ "Content-Type": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Content-Disposition": "attachment; filename=ChangeStageReport.xlsx" }` |
| Response | Binary (Excel file stream) |
| Response Failure | `{ "appCode": "OCMS-4004", "message": "Report not found" }` |

---

## 5.4 Data Mapping

| Zone | Database Table | Field Name |
| --- | --- | --- |
| Intranet | ocms_change_of_processing | notice_no |
| Intranet | ocms_change_of_processing | date_of_change |
| Intranet | ocms_change_of_processing | last_processing_stage |
| Intranet | ocms_change_of_processing | new_processing_stage |
| Intranet | ocms_change_of_processing | reason_of_change |
| Intranet | ocms_change_of_processing | authorised_officer |
| Intranet | ocms_change_of_processing | source |
| Intranet | ocms_change_of_processing | remarks |
| Intranet | ocms_valid_offence_notice | offender_name |
| Intranet | ocms_valid_offence_notice | vehicle_no |

**Report Output Fields:**

| Field | Source |
| --- | --- |
| Notice Number | ocms_change_of_processing.notice_no |
| Previous Stage | ocms_change_of_processing.last_processing_stage |
| New Stage | ocms_change_of_processing.new_processing_stage |
| Date of Change | ocms_change_of_processing.date_of_change |
| Reason of Change | ocms_change_of_processing.reason_of_change |
| Authorised Officer | ocms_change_of_processing.authorised_officer |
| Source | ocms_change_of_processing.source |
| Remarks | ocms_change_of_processing.remarks |
| Offender Name | ocms_valid_offence_notice.offender_name |
| Vehicle Number | ocms_valid_offence_notice.vehicle_no |

---

## 5.5 Success Outcome

- User successfully accesses Report module
- Date range validated successfully (within 90 days limit)
- Query executed within acceptable time
- Report data retrieved from ocms_change_of_processing and ocms_valid_offence_notice
- Excel report generated and available for download
- Report downloaded successfully
- The workflow reaches the End state without triggering any error-handling paths

---

## 5.6 Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Invalid Date Range | Date range exceeds 90 days | Display prompt "Date range cannot exceed 90 days" |
| Missing Required Field | Start date or end date not provided | Display prompt "Please enter date range" |
| Report Not Found | Report file does not exist in storage | Display prompt "Report not found" |
| Database Error | Unable to retrieve data from database | Display system error message |

---

## Appendix: Processing Stage Descriptions

| Stage Code | Description | Role |
| --- | --- | --- |
| NPA | Notice Pending Action | Initial |
| ENA | Enforcement Action | Initial |
| DN1 | Driver Notice 1st Reminder | DRIVER |
| DN2 | Driver Notice 2nd Reminder | DRIVER |
| DR3 | Driver 3rd Reminder | DRIVER |
| ROV | Registered Owner Vehicle | OWNER/HIRER/DIRECTOR |
| RD1 | Registered Owner 1st Reminder | OWNER/HIRER/DIRECTOR |
| RD2 | Registered Owner 2nd Reminder | OWNER/HIRER/DIRECTOR |
| RR3 | Registered Owner 3rd Reminder | OWNER/HIRER/DIRECTOR |
| CRT | Court Stage | Court (Non-changeable) |
| CRC | Court Case | Court (Non-changeable) |
| CFI | Court Final | Court (Non-changeable) |
| CFC | Claim for Complaint | Court |
| CPC | Court Processing Complete | Court |

---

## Appendix: Reason of Change Codes

| Code | Description |
| --- | --- |
| SUP | Speed up processing |
| OTH | Other (remarks required) |

---

## Appendix: Source Codes

| Code | Description |
| --- | --- |
| OCMS | Change initiated via OCMS Staff Portal |
| PLUS | Change initiated via PLUS Staff Portal |
| SYSTEM | Change initiated by Toppan cron batch |

---

*End of Document*

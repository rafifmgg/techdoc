# OCMS 18 – Permanent Suspension

**Prepared by**

MGG SOFTWARE

---

## Version History

| Version | Updated By | Date | Changes |
| --- | --- | --- | --- |
| v1.0 | Technical Documentation | 06/01/2026 | Document Initiation |

---

## Table of Contents

| Section | Content |
| --- | --- |
| 1 | Function: Permanent Suspension |
| 1.1 | Use Case |
| 1.2 | High-Level Processing Flow |
| 1.3 | Permanent Suspension Data |
| 1.4 | Permanent Suspension Rules |
| 1.5 | Validating PS Requests |
| 1.5.1 | UI-Side Validation Flow |
| 1.5.2 | Backend-Side Validation Flow |
| 1.6 | Data Mapping |
| 1.7 | Success Outcome |
| 1.8 | Error Handling |
| 2 | OCMS Staff Portal Manual PS |
| 2.1 | Use Case |
| 2.2 | Process Flow |
| 2.3 | API Specification |
| 2.4 | Data Mapping |
| 2.5 | Success Outcome |
| 2.6 | Error Handling |
| 2.7 | Suspension Codes Used by OCMS Staff Portal |
| 3 | Auto Permanent Suspensions in OCMS |
| 3.1 | Use Case |
| 3.2 | Process Flow |
| 3.3 | Data Mapping |
| 3.4 | Success Outcome |
| 3.5 | Error Handling |
| 3.6 | Scenarios that Auto-Trigger PS |
| 4 | Permanent Suspension initiated by PLUS |
| 4.1 | Use Case |
| 4.2 | Process Flow |
| 4.3 | API Specification |
| 4.4 | Data Mapping |
| 4.5 | Success Outcome |
| 4.6 | Error Handling |
| 4.7 | Suspension Codes used by PLUS |
| 5 | Permanent Suspension Report |
| 5.1 | Use Case |
| 5.2 | Process Flow |
| 5.3 | API Specification |
| 5.4 | Data Mapping |
| 5.5 | Success Outcome |
| 5.6 | Error Handling |
| 5.7 | Search Parameters |
| 5.8 | Retrieval Behavior |

---

# Section 1 – Function: Permanent Suspension

## 1.1 Use Case

- Permanent Suspension (PS) may be applied to Notices as they progress through the standard Notice Processing Workflow.

- A PS is applied when a Notice no longer requires further processing within the Notice Processing Workflow. Examples of scenarios include notices which has been fully paid and notices waived by the Appeal Processing Officer during appeal processing.

- When PS is applied, a suspension code will be used to indicate the reason of suspension.

- Notices can be permanently suspended through the following modes:
  - Manual suspension by EPU OICs via the OCMS Staff Portal
  - Automatic suspension by the OCMS backend when pre-defined scenarios occur during batch processing, for example Notices issued to military vehicles are suspended with PS-MID if they are still outstanding at the end of the second reminder stage to prevent the Notices from escalating to the Court stage
  - Manual suspension by PLU Officers via the PLUS Staff Portal

- When a PS is applied to an Offence Notice:
  - The Notice remains at the last processing stage it is currently at.
  - The Notice will not advance to the next stage when the Next Processing Date is reached.

---

## 1.2 High-Level Processing Flow

<!-- Insert flow diagram here -->
![High-Level PS Flow](./images/section1-highlevel-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Receive PS Request | OCMS Backend receives the PS request from the source | Staff Portal, PLUS, or Backend Auto-trigger sends request |
| Validate PS Request | Backend validates the request against PS rules and eligibility | Check notice status, PS code rules, processing stage |
| Process PS | Backend updates database if validation passes | UPDATE ocms_valid_offence_notice, INSERT ocms_suspended_notice |
| Return Response | Backend returns response to the requesting source | Success or error response with message |

---

## 1.3 Permanent Suspension Data

| Field Name | Description | Sample Value |
| --- | --- | --- |
| notice_no* | Notice number | 500500303J |
| date_of_suspension* | Date the suspension was applied | 2025-03-22T19:00:02 |
| sr_no* | Serial number for suspension, running number | 1 |
| suspension_source* | Which system initiated the suspension | PLUS |
| suspension_type* | Type of suspension | PS |
| epr_reason_of_suspension* | EPR suspension code that indicates the reason for the suspension | APP – Appeal accepted |
| crs_reason of suspension | CRS suspension code that indicates the reason for suspension | |
| officer_authorising_suspension* | Name of Officer who initiated the suspension | JOHNLEE |
| due_date_of_revival | Date when the Suspension will be lifted. Field will be empty when PS is applied. | |
| suspension_remarks | Suspension remarks entered by the Officer who initiated the Suspension | Appeal received |
| date_of_revival | Date the Suspension was lifted. Field will be empty when PS is applied. | |
| revival_reason | Reason for lifting the Suspension. Field will be empty when PS is applied | |
| officer_authorising_revival | Officer who lifted the Suspension. Field will be empty when a Suspension is applied | |
| revival_remarks | Revival remarks entered by the Officer who lifted the Suspension. Field will be empty when a Suspension is applied | |
| cre_date | Date and time the record was created | 2025-03-22T19:00:02 |
| cre_user_id | Name of OIC or system that triggered the record creation | JOHNLEE |
| upd_date | Date and time the record was updated | |
| upd_user_id | Name of OIC or system that updated the record | |

*Denotes mandatory fields

---

## 1.4 Permanent Suspension Rules

### 1.4.1 Global Rules for Permanent Suspension

| Condition | Rule | Description |
| --- | --- | --- |
| Displaying PS in the Staff Portal | Display the most recent active suspension | Where there are multiple suspensions (PS or TS) applied to a Notice, the Staff Portal's Notice Overview tab displays the most recently applied active suspension on screen. |
| Apply new EPR Reason of Suspension PS Code to a Notice with an existing PS Suspension | Allowed for authorised users | Only authorised OCMS OICs and PLU PLMs are allowed to apply a new PS Suspension Code to a Notice with an existing PS. When a new PS is triggered via the OCMS or PLUS Staff Portal, OCMS will revive the existing PS with the revival code 'PSR' (Permanent Suspension Revival) and apply the new PS automatically. This rule applies to all Notices with any existing and active PS Suspension. |
| Apply new CRS Reason of Suspension PS Code to a Notice with an existing PS Suspension | Allowed for OCMS Backend during payment processing | Only OCMS backend is allowed to apply PS-FP and PS-PRA during payment processing. The following existing and active PS codes allow new PS-FP and PS-PRA suspensions to be applied without revival: DIP, FOR, MID, RIP, RP2. PS-FP and PS-PRA cannot be applied to notices with other existing and active PS code that are not listed. |
| Apply TS to a Notice with an existing PS | Depends on existing PS Code | TS cannot be applied to a Notice that has an existing PS. The following PS Codes are exceptions and allow TS to be applied on top of the existing PS: DIP, MID, FOR, RIP, RP2. The TS-UNC Suspension Code cannot be applied to any notice with an existing PS, including the ones listed. |
| Apply PS to a paid or partially paid Notice | Depends on new PS Code | Paid notices are suspended with: PS-FP (Fully paid notices) and PS-PRA (Paid at reduced amount). Only PS-APP, PS-CFA and PS-VST can be applied on top of paid notices. The suspension will trigger OCMS backend to trigger a refund. |
| Apply PS to a Notice up to CFC and CPC stages | Depends on new PS Code | The following PS Codes can be applied to Court Notices up to CFC and CPC stages: APP, CFA, DBB, FCT, FOR, FP, FTC, OTH, PRA, RIP, RP2, SCT, SLC, SSV, VCT, VST. |
| Notices are payable via the eService or AXS | Allowed for selected case | Outstanding Notices with the following PS codes are payable: DIP, MID, FOR, RIP, RP2 |
| Notice is furnishable via the eService | Not Allowed | Notices with a PS are not furnishable. |
| New Hirer/Driver can be added via the OCMS Staff Portal | Not Allowed | Outstanding Notices with the following PS codes need to be revived first before new hirer/driver are added via the OCMS Staff Portal: DIP, MID, RIP, RP2 |
| New Hirer/Driver can be added via the PLUS Staff Portal | Not Allowed | Outstanding Notices with the following PS codes need to be revived first before new hirer/driver are added via the PLUS Staff Portal: DIP, MID, RIP, RP2 |
| Notice can be linked to an Appeal | Allowed | All notices, regardless of its Last Processing Stage and suspension/payment status, can be linked to an Appeal. |

### 1.4.2 Suspension Code Rules

- The OCMS backend maintains a set of rules governing the application of PS to Notices and whether Notice processing operations such as Update Hirer/Driver are allowed.

- The rules will include:
  - Which codes can be applied by PLUS System Users
  - Which codes can be applied by OCMS System Users
  - Which codes can only be applied by the OCMS backend system
  - Which Processing Stages allow or disallow PS
  - Whether Update Driver/Hirer can be applied to notices with existing PS
  - Whether Reduction can be applied to Notices with existing PS

---

## 1.5 Validating PS Requests

### 1.5.1 UI-Side Validation Flow

<!-- Insert UI validation flow diagram here -->
![UI-Side Validation Flow](./images/section1-ui-validation-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

### 1.5.2 Backend-Side Validation Flow

<!-- Insert Backend validation flow diagram here -->
![Backend-Side Validation Flow](./images/section1-backend-validation-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

### 1.5.3 Key Validation Principles

- All requests to apply PS to Notices will be validated.

- When a PS request is initiated from a Staff Portal (OCMS or PLUS):
  - The Staff Portal will perform the first round of validation based on the valid offence notice data the portal received from the OCMS backend.
  - If the Notice(s) are determined to be eligible for PS, the Portal will trigger the PS request to the OCMS backend, which will also perform another round of validation against all rules for PS.

- When a PS request is initiated from the Backend, for example auto apply PS-RIP, the OCMS backend will also validate the request to determine if the suspension can be applied.

### 1.5.4 Validating PS Requests in the Staff Portal (OCMS and PLUS)

- The Staff Portal performs first-level validation to provide immediate feedback and prevent invalid suspension requests from being sent to the backend system.

- The Staff Portal submits suspension requests to the backend only if the first-level validation confirms the Notice's eligibility.

- This approach improves user experience as it:
  - Provides instant feedback so users know immediately if a Notice is ineligible
  - Guides users to deselect invalid Notices before submission
  - Checks known eligibility rules (e.g., processing stage, PS status) upfront
  - Prevents obviously invalid requests from reaching the backend

- The following checks will be performed on the UI side, based on valid offence notice data received from the backend:

| What to Validate in UI | Description |
| --- | --- |
| User Permission | The Portal checks if the user is authorised to perform PS |
| Notice's Last Processing Stage | The Portal checks the Last Processing Stage to confirm whether the Notice is at a stage that permits PS |
| Payment Status | The Portal checks the following fields to verify if the Notice has been paid: Suspension Type, CRS Reason for Suspension |

---

## 1.6 Data Mapping

| Zone | Database Table | Field Name |
| --- | --- | --- |
| Intranet | ocms_valid_offence_notice | notice_no |
| Intranet | ocms_valid_offence_notice | suspension_type |
| Intranet | ocms_valid_offence_notice | last_processing_stage |
| Intranet | ocms_valid_offence_notice | next_processing_date |
| Intranet | ocms_suspended_notice | notice_no |
| Intranet | ocms_suspended_notice | date_of_suspension |
| Intranet | ocms_suspended_notice | sr_no |
| Intranet | ocms_suspended_notice | suspension_source |
| Intranet | ocms_suspended_notice | suspension_type |
| Intranet | ocms_suspended_notice | epr_reason_of_suspension |
| Intranet | ocms_suspended_notice | officer_authorising_suspension |
| Intranet | ocms_suspended_notice | suspension_remarks |
| Intranet | ocms_suspended_notice | cre_date |
| Intranet | ocms_suspended_notice | cre_user_id |

**Note:**
- Audit user fields (cre_user_id, upd_user_id) use database connection user:
  - Intranet: **ocmsiz_app_conn**
  - Internet: **ocmsez_app_conn**
- Insert order: Parent table first (UPDATE ocms_valid_offence_notice), then child table (INSERT ocms_suspended_notice)

---

## 1.7 Success Outcome

- PS record successfully inserted into ocms_suspended_notice table
- ocms_valid_offence_notice updated with suspension_type = 'PS'
- Notice processing is stopped at current stage
- Notice will not advance to next processing stage
- Success response returned to requesting source (Staff Portal, PLUS, or Backend)
- The workflow reaches the End state without triggering any error-handling paths

---

## 1.8 Error Handling

### Application Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Invalid Notice Number | Notice number not found in database | Return error response, no record created |
| Notice Fully Paid | Notice already has PS-FP or PS-PRA suspension | Return error response, PS not applied |
| Unauthorized Source | Source system not authorized to use the PS code | Return error response, PS not applied |
| Invalid Processing Stage | PS code cannot be applied at Notice's current processing stage | Return error response, PS not applied |
| Invalid Date of Suspension | Suspension date is missing or is a future date | Return error response, PS not applied |
| Missing Suspension Source | Suspension source field is empty | Return error response, PS not applied |
| Missing Suspension Type | Suspension type field is empty | Return error response, PS not applied |
| Missing Reason of Suspension | Reason of suspension field is empty | Return error response, PS not applied |
| Missing Officer | Officer authorising suspension field is empty | Return error response, PS not applied |
| Database Error | Unable to insert/update database record | Log error, return error response |

### API Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| General Server Error | OCMS-5000 | Something went wrong on our end. Please try again later. | Server encountered an unexpected condition |
| Bad Request | OCMS-4000 | The request could not be processed. Please check and try again. | Invalid request syntax |
| Unauthorized Access | OCMS-4001 | You are not authorized to access this resource. | Authentication failed |
| Not Found | OCMS-4004 | Notice not found. | Notice number does not exist |
| Validation Error | OCMS-4022 | PS cannot be applied due to validation rules. | Business rule validation failed |
| Undefined Error | OCMS-5999 | Undefined error. Please inform Administrator. | Unexpected system error |

---

# Section 2 – OCMS Staff Portal Manual PS

## 2.1 Use Case

- The manual PS function in the OCMS Staff Portal allows authorised OICs to permanently suspend Notices.

- Authorised users can search for the notices using the Staff Portal's Search Notice function by:
  - Entering search parameters to retrieve one or more Notices
  - Uploading a spreadsheet containing a list of notice numbers to retrieve the Notices

- From the search results, Users can apply PS using one of two ways:
  - Select one or more Notices in the Search Result interface and trigger the PS operation from the drop-down list to initiate a batch PS
  - Click on a Notice to view the details and apply the PS from the View Notice Details interface

- When the PS is initiated from the Staff Portal, the Staff Portal validates the suspension eligibility of each Notice before redirecting Users to the Suspension Detail Page to:
  - Select the PS Suspension Code
  - Input Suspension Remarks, if any

- After the Suspension Details are submitted, OCMS sends the suspension details to the OCMS backend for processing.

- If PS is successful, Users will be presented with a success message on screen; an error message will be displayed if the suspension is not successful.

---

## 2.2 Process Flow

<!-- Insert flow diagram here -->
![Staff Portal Manual PS Flow](./images/section2-manual-ps-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Search Notice | OIC navigates to Search Notice Page and searches for notices to apply PS | Enter search criteria or upload notice list |
| Select Notice(s) | OIC selects notice(s) from search results | Single or multiple notice selection |
| Select Apply PS | OIC selects 'Apply PS' option from Apply Operation dropdown and clicks Select | Initiate PS operation |
| Validate Eligibility | Staff Portal validates eligibility based on notice data received from backend | Check processing stage, PS status, user permission |
| Redirect to Suspension Detail Page | If eligible, redirect to Suspension Detail Page | Navigate to PS input form |
| Enter PS Data | OIC selects PS Code and enters Suspension Remarks | Select code from dropdown, optional remarks |
| Submit | OIC clicks 'Apply PS' to submit | Confirm and send request |
| Confirmation | System prompts for confirmation | User confirms action |
| Backend Processing | Backend validates request and processes PS | UPDATE ocms_valid_offence_notice, INSERT ocms_suspended_notice |
| Response | Display success or error message | Show result to user |

---

## 2.3 API Specification

### API for eService

#### API Apply Suspension (Staff Portal)

| Field | Value |
| --- | --- |
| API Name | staff-apply-suspension |
| URL | UAT: https://parking2.ura.gov.sg/ocms/v1/staff-apply-suspension <br> PRD: https://parking.ura.gov.sg/ocms/v1/staff-apply-suspension |
| Description | Apply PS or TS suspension manually by OCMS Staff Portal |
| Method | POST |
| Header | `{ "Content-Type": "application/json", "Authorization": "Bearer <JWT_TOKEN>" }` |
| Payload | `{ "noticeNo": ["500500303J"], "suspensionType": "PS", "reasonOfSuspension": "CFA", "suspensionRemarks": "Incorrect issuance", "officerAuthorisingSuspension": "JOHNLEE" }` |
| Response | `{ "totalProcessed": 1, "successCount": 1, "errorCount": 0, "results": [{ "noticeNo": "500500303J", "srNo": "1234", "appCode": "OCMS-2000", "message": "PS Success" }] }` |
| Response Failure | `{ "totalProcessed": 1, "successCount": 0, "errorCount": 1, "results": [{ "noticeNo": "500500303J", "appCode": "OCMS-4002", "message": "PS Code cannot be applied due to Last Processing Stage" }] }` |

### Request Parameter Data

| Field Name | Type | Required | Max Length | Description |
| --- | --- | --- | --- | --- |
| noticeNo | Array[String] | Yes | - | List of notice numbers to suspend. Max 10 per batch |
| suspensionType | String | Yes | 2 | "PS" for Permanent Suspension |
| reasonOfSuspension | String | Yes | 3 | PS Code (ANS, CAN, CFA, etc.) |
| suspensionRemarks | String | No | 200 | Remarks for the suspension |
| officerAuthorisingSuspension | String | Yes | 50 | OIC username |

---

## 2.4 Data Mapping

| Zone | Database Table | Field Name |
| --- | --- | --- |
| Intranet | ocms_valid_offence_notice | notice_no |
| Intranet | ocms_valid_offence_notice | suspension_type |
| Intranet | ocms_valid_offence_notice | last_processing_stage |
| Intranet | ocms_suspended_notice | notice_no |
| Intranet | ocms_suspended_notice | date_of_suspension |
| Intranet | ocms_suspended_notice | sr_no |
| Intranet | ocms_suspended_notice | suspension_source |
| Intranet | ocms_suspended_notice | suspension_type |
| Intranet | ocms_suspended_notice | epr_reason_of_suspension |
| Intranet | ocms_suspended_notice | officer_authorising_suspension |
| Intranet | ocms_suspended_notice | suspension_remarks |
| Intranet | ocms_suspended_notice | cre_date |
| Intranet | ocms_suspended_notice | cre_user_id |

**Note:** Audit user fields (cre_user_id, upd_user_id) use database connection user: **ocmsiz_app_conn** (Intranet)

---

## 2.5 Success Outcome

- User successfully navigates to Search Notice Page
- Notice(s) retrieved based on search criteria
- Frontend validation passes for selected notice(s)
- User successfully redirected to Suspension Detail Page
- PS record inserted into ocms_suspended_notice table
- ocms_valid_offence_notice updated with suspension status
- Success message displayed to user
- The workflow reaches the End state without triggering any error-handling paths

---

## 2.6 Error Handling

### Application Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| No Permission | User does not have authorization to perform PS | Display error message, user cannot proceed |
| Invalid Processing Stage | Notice is at a stage that does not permit PS | Display validation error, notice excluded from batch |
| Already Suspended | Notice already has an active PS that cannot be overridden | Display validation error, notice excluded from batch |
| Backend Validation Failed | Backend rejects PS request due to rule violation | Display error message from backend response |
| Connection Error | Unable to connect to backend API | Display system error message, prompt user to retry |

### API Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| General Server Error | OCMS-5000 | Something went wrong. Please try again later. | Server error |
| Validation Error | OCMS-4022 | PS cannot be applied due to validation rules. | Backend validation failed |
| Connection Error | OCMS-5001 | Service temporarily unavailable. | Unable to connect to backend |

Refer to Section 1.8 for complete backend error handling scenarios.

---

## 2.7 Suspension Codes Used by OCMS Staff Portal

| Suspension Type | Code | Scenario |
| --- | --- | --- |
| PS | ANS | OIC has investigated the Offence Notice case and verified that the Notice qualifies as an AN. |
| PS | APP | OIC has verified that the Notice can be suspended as the Appeal was accepted. |
| PS | CAN | OIC has investigated the Offence Notice case and verified that the AN should be cancelled. |
| PS | CFA | OIC has investigated the Offence Notice case and verified that the Notice should be cancelled due to wrong issuance by the Parking Warden. |
| PS | CFP | OCMS has been notified by the State Court that the Court Fine has been collected. |
| PS | DBB | Duplicate notices have been issued for the same offence. The second notice will be suspended as a double booking notice. |
| PS | DIP | Diplomatic vehicle Notice is outstanding at the end of the RD2/DN2 stage and needs to be suspended. |
| PS | FCT | OIC cannot follow up with the Offender as the Offender is a foreigner and cannot be traced. |
| PS | FOR | OIC has verified that the vehicle is a foreign vehicle. |
| PS | FTC | OIC cannot follow up with the Offender as the Offender is a foreigner and has left the country. |
| PS | IST | OIC manually applies the PS when an Instalment Plan for payment of the Court Fine has been issued. |
| PS | MID | MINDEF vehicle Notice is outstanding at the end of the RD2/DN2 stage and needs to be suspended. |
| PS | OTH | OIC suspends the Notice for other scenarios which have no specific suspension codes. |
| PS | RIP | OIC has investigated and verified that the Offender died on or after the Offence Date. |
| PS | RP2 | OIC has investigated and verified that the Offender died before the Offence Date. |
| PS | SCT | OIC has investigated and verified that no information can be found for the local offender. |
| PS | SLC | OIC has investigated and verified that the local offender has left the country. |
| PS | SSV | OIC has investigated and verified that the vehicle is a security vehicle. |
| PS | VCT | OIC has investigated and verified that vehicle information cannot be obtained. |
| PS | VST | OIC has investigated and verified that the vehicle has a valid season ticket. |
| PS | WWC | OIC has verified that the Court Notice is fully paid by the Defendant. |
| PS | WWF | OIC has verified that the Court Fine has been collected by URA. |
| PS | WWP | OIC has verified that the Notice needs to be withdrawn due to specific reasons, for example Defendant passed away. |

---

# Section 3 – Auto Permanent Suspensions in OCMS

## 3.1 Use Case

- The OCMS system will automatically trigger Permanent Suspensions to Notices when pre-defined scenarios occur.

- The scenarios are pre-defined in the Notice Processing Workflow at various Processing Stages.

- When the conditions of the scenarios are met OCMS will automatically Suspend the Notices with pre-defined Suspension Codes and alert OICs about the exceptions.

---

## 3.2 Process Flow

<!-- Insert flow diagram here -->
![Auto PS Flow](./images/section3-auto-ps-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Trigger Condition Met | OCMS Backend detects a pre-defined scenario during batch processing | DIP, MID, FOR, RIP, RP2, FP, PRA, etc. |
| Validate PS Eligibility | Backend validates if auto PS can be applied to the notice | Check existing PS, processing stage |
| Apply PS | Backend updates ocms_valid_offence_notice and inserts record into ocms_suspended_notice | UPDATE parent, INSERT child table |
| Alert OIC | System generates alert/exception report for OICs | Exception notification sent |
| Complete | Auto PS process completed | Process ends successfully |

---

## 3.3 Data Mapping

| Zone | Database Table | Field Name |
| --- | --- | --- |
| Intranet | ocms_valid_offence_notice | notice_no |
| Intranet | ocms_valid_offence_notice | suspension_type |
| Intranet | ocms_valid_offence_notice | last_processing_stage |
| Intranet | ocms_valid_offence_notice | vehicle_registration_type |
| Intranet | ocms_suspended_notice | notice_no |
| Intranet | ocms_suspended_notice | date_of_suspension |
| Intranet | ocms_suspended_notice | sr_no |
| Intranet | ocms_suspended_notice | suspension_source |
| Intranet | ocms_suspended_notice | suspension_type |
| Intranet | ocms_suspended_notice | epr_reason_of_suspension |
| Intranet | ocms_suspended_notice | cre_date |
| Intranet | ocms_suspended_notice | cre_user_id |

**Note:** Audit user fields (cre_user_id, upd_user_id) use database connection user: **ocmsiz_app_conn** (Intranet - Backend)

---

## 3.4 Success Outcome

- Pre-defined scenario condition detected by OCMS Backend
- PS record successfully inserted into ocms_suspended_notice table
- ocms_valid_offence_notice updated with suspension_type = 'PS'
- Notice processing stopped at current stage
- OIC alert/exception report generated
- The workflow reaches the End state without triggering any error-handling paths

---

## 3.5 Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Validation Failed | Notice does not meet auto PS criteria | Log error, skip notice, continue batch processing |
| Database Error | Unable to insert/update database record | Log error, retry mechanism triggered, alert generated |
| Duplicate PS | Notice already has the same PS code applied | Skip notice, no duplicate record created |

Refer to Section 1.8 for complete backend error handling scenarios.

---

## 3.6 Scenarios that Auto-Trigger PS

| Scenario | Suspension Code | Auto Triggered by |
| --- | --- | --- |
| The notice will be suspended for the following reasons: Certis or REPCCS indicates that the notice is an AN. OCMS performs AN qualification checks and determine that the Notice qualifies as an AN. | ANS | OCMS Backend |
| Duplicate notices are detected for the same offence and OCMS automatically suspends the second notice as a double booking notice. | DBB | OCMS Backend |
| Diplomatic vehicle Notice has reached the end of the RD2/DN2 stage. | DIP | OCMS Backend |
| Vehicle that committed the Offence is a foreign vehicle. | FOR | OCMS Backend |
| MINDEF vehicle Notice has reached the end of the RD2/DN2 stage. | MID | OCMS Backend |
| Partial payment occurred due to the Notice escalation and the Notice's amount payable has been adjusted. | PRA | OCMS Backend |
| OCMS receives the date of death from MHA or DataHive and verifies that the Offender died on or after the Offence Date. | RIP | OCMS Backend |
| OCMS receives the date of death from MHA or DataHive and verifies that the Offender died before the Offence Date. | RP2 | OCMS Backend |
| OCMS receives payment for the Notice and verified that the notice is fully paid. | FP | OCMS Backend |

---

# Section 4 – Permanent Suspension initiated by PLUS

## 4.1 Use Case

- PLUS can apply PS via an API provided by OCMS.

- The PLUS system and PLUS Users will initiate PS under the following circumstances:
  - Appeal Processing Officers (PLMs) suspend a notice when the Appeal is accepted.
  - Appeal Processing Officers (PLMs) suspend a notice due to specific reasons, for example the Notice was wrongly issued by the Parking Warden, or the Appellant has a valid season parking ticket.

- When a suspension is initiated through the PLUS Staff Portal or backend, the PLUS system shall provide the suspension details, which will be logged in OCMS in the Suspended Notices database table.

- Upon receiving the request, OCMS validates the submitted suspension data before proceeding with the PS.

- PLUS can apply PS to multiple Notices simultaneously.

---

## 4.2 Process Flow

<!-- Insert flow diagram here -->
![PLUS PS Flow](./images/section4-plus-ps-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| PLM initiates PS | PLM accesses PLUS Staff Portal and initiates PS for notice(s) | Access appeal processing module |
| PLUS validates eligibility | PLUS Staff Portal validates eligibility based on notice data | Check processing stage, PS status |
| Enter PS Data | PLM selects PS Code and enters Suspension Remarks | APP, CAN, CFA, OTH, VST codes available |
| Submit PS Request | PLUS Backend sends PS request to OCMS Backend via API | API call with suspension details |
| OCMS Backend Validation | OCMS Backend validates request against PS rules and eligibility | Validate all PS rules |
| Backend Processing | If valid, backend updates ocms_valid_offence_notice and inserts record into ocms_suspended_notice | UPDATE parent, INSERT child table |
| Response | OCMS Backend returns success or error response to PLUS | Return result to PLUS system |

---

## 4.3 API Specification

### API Provide

#### API Apply Permanent Suspension

| Field | Value |
| --- | --- |
| API Name | applyPermanentSuspension |
| URL | UAT: https://parking2.ura.gov.sg/ocms/v1/suspension/permanent <br> PRD: https://parking.ura.gov.sg/ocms/v1/suspension/permanent |
| Description | API to apply Permanent Suspension to notice(s) from PLUS |
| Method | POST |
| Header | `{ "Authorization": "Bearer [token]", "Content-Type": "application/json", "Ocp-Apim-Subscription-Key": "[APIM key]" }` |
| Payload | `{ "notice_no": "500500303J", "date_of_suspension": "2025-03-22T19:00:02", "suspension_source": "PLUS", "suspension_type": "PS", "reason_of_suspension": "APP", "officer_authorising_suspension": "JOHNLEE", "suspension_remarks": "Appeal accepted" }` |
| Response | `{ "totalProcessed": 1, "successCount": 1, "errorCount": 0, "results": [{ "noticeNo": "500500303J", "srNo": "1234", "appCode": "OCMS-2000", "message": "PS Success" }] }` |
| Response Failure | `{ "totalProcessed": 1, "successCount": 0, "errorCount": 1, "results": [{ "noticeNo": "500500303J", "appCode": "OCMS-4002", "message": "PS Code cannot be applied" }] }` |

### Request Parameter Data

| Field Name | Description | Mandatory | Sample Value |
| --- | --- | --- | --- |
| notice_no | Notice number | Yes | 500500303J |
| date_of_suspension | Date the suspension was applied | Yes | 2025-03-22T19:00:02 |
| suspension_source | Which system initiated the suspension | No | PLUS |
| suspension_type | Type of suspension | Yes | PS |
| reason_of_suspension | EPR suspension code that indicates the reason for the suspension | Yes | APP |
| officer_authorising_suspension | Name of Officer who initiated the suspension | Yes | JOHNLEE |
| suspension_remarks | Suspension remarks entered by the Officer | No | Appeal accepted |

---

## 4.4 Data Mapping

| Zone | Database Table | Field Name |
| --- | --- | --- |
| Internet | ocms_valid_offence_notice | notice_no |
| Internet | ocms_valid_offence_notice | suspension_type |
| Internet | ocms_valid_offence_notice | last_processing_stage |
| Internet | ocms_suspended_notice | notice_no |
| Internet | ocms_suspended_notice | date_of_suspension |
| Internet | ocms_suspended_notice | sr_no |
| Internet | ocms_suspended_notice | suspension_source |
| Internet | ocms_suspended_notice | suspension_type |
| Internet | ocms_suspended_notice | epr_reason_of_suspension |
| Internet | ocms_suspended_notice | officer_authorising_suspension |
| Internet | ocms_suspended_notice | suspension_remarks |
| Internet | ocms_suspended_notice | cre_date |
| Internet | ocms_suspended_notice | cre_user_id |

**Note:** Audit user fields (cre_user_id, upd_user_id) use database connection user: **ocmsez_app_conn** (Internet)

---

## 4.5 Success Outcome

- PLUS Staff Portal validation passes
- PS request successfully sent to OCMS Backend via API
- OCMS Backend validation passes
- PS record inserted into ocms_suspended_notice table
- ocms_valid_offence_notice updated with suspension status
- Success response returned to PLUS
- The workflow reaches the End state without triggering any error-handling paths

---

## 4.6 Error Handling

### Application Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| PLUS Validation Failed | Notice does not meet PS eligibility on PLUS side | Display error message on PLUS Staff Portal |
| OCMS Validation Failed | OCMS Backend rejects PS request due to rule violation | Return error response to PLUS with error message |
| Token Expired | Authentication token has expired | Auto refresh token and continue processing |

### API Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| General Server Error | OCMS-5000 | Something went wrong on our end. Please try again later. | Server encountered an unexpected condition |
| Bad Request | OCMS-4000 | The request could not be processed. Please check and try again. | Invalid request syntax or missing fields |
| Unauthorized Access | OCMS-4001 | You are not authorized to access this resource. | Authentication failed or token invalid |
| Invalid Notice | OCMS-4004 | Notice not found. | Notice number does not exist in database |
| Validation Error | OCMS-4022 | PS cannot be applied due to validation rules. | Business rule validation failed |
| Connection Error | OCMS-5001 | Service temporarily unavailable. Please try again later. | Unable to connect to backend |

Refer to Section 1.8 for complete backend error handling scenarios.

---

## 4.7 Suspension Codes used by PLUS

| Suspension Type | Code | Scenario |
| --- | --- | --- |
| PS | APP | PLM has reviewed the Appeal and decided to accept the Appeal and waive the Notice |
| PS | CAN | PLM has investigated the Offence Notice case and verified that the AN should be cancelled. |
| PS | CFA | PLM has investigated the Offence Notice case and verified that the Notice should be cancelled due to wrong issuance by the Parking Warden. |
| PS | OTH | PLM suspends the Notice for other scenarios which have no specific suspension codes. |
| PS | VST | PLM has investigated and verified that vehicle has a valid season ticket. |

---

# Section 5 – Permanent Suspension Report

## 5.1 Use Case

- The OCMS Staff Portal's Report module allows users to export a report to review Notices which were permanently suspended based on retrieval criteria.

- The report consists of 2 sections:
  - List of PS applied by the various sub-systems (e.g., OCMS Staff Portal, PLUS, OCMS Backend)
  - List of PS applied by OICs

---

## 5.2 Process Flow

<!-- Insert flow diagram here -->
![PS Report Flow](./images/section5-ps-report-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Access Report Module | User navigates to OCMS Staff Portal's Report module | Navigate to Report menu |
| Select Report Type | User selects Permanent Suspension Report | Choose PS Report option |
| Enter Search Criteria | User enters search parameters (Date Range, Officer Names) | Mandatory date range, optional officer filter |
| Validate Input | System validates search parameters | Check date range, max 1 year limit |
| Execute Query | System retrieves PS records based on criteria from ocms_suspended_notice and ocms_valid_offence_notice tables | Query with result limit of 1000 records |
| Generate Report | System generates report with two sections: PS by Sub-System and PS by OIC | Two separate report sections |
| Export | User exports report | Download report file |

---

## 5.3 API Specification

### API for eService

#### API Get User List

| Field | Value |
| --- | --- |
| API Name | userlist |
| URL | UAT: https://parking2.ura.gov.sg/ocms/v1/userlist <br> PRD: https://parking.ura.gov.sg/ocms/v1/userlist |
| Description | Get list of active users for dropdown filters |
| Method | GET |
| Header | `{ "Content-Type": "application/json", "Authorization": "Bearer <JWT_TOKEN>" }` |
| Payload | N/A |
| Response | `{ "appCode": "OCMS-2000", "message": "Success", "data": [{ "userId": "JOHNLEE", "firstName": "John", "lastName": "Lee" }, { "userId": "MARYTAN", "firstName": "Mary", "lastName": "Tan" }] }` |
| Response (Empty) | `{ "appCode": "OCMS-2000", "message": "Success", "data": [] }` |
| Response Failure | `{ "appCode": "OCMS-5000", "message": "Unable to retrieve officer list" }` |

**Data Source:**

| Zone | Database Table | Field Name | Description |
| --- | --- | --- | --- |
| Intranet | ocms_user | user_id | User ID (VARCHAR2 10) |
| Intranet | ocms_user | first_name | First Name (VARCHAR2 50) |
| Intranet | ocms_user | last_name | Last Name (VARCHAR2 50) |
| Intranet | ocms_user | status | User Status - A (Active), I (Inactive) |

**Query Condition:**
- Only return users where `status = 'A'` (Active users)

**Note:** If `ocms_user` table has no active users, the API will return empty array `[]`. The UI should handle this by displaying "No officers available" or disabling the filter dropdown.

#### API PS Report By System

| Field | Value |
| --- | --- |
| API Name | ps-report-by-system |
| URL | UAT: https://parking2.ura.gov.sg/ocms/v1/ps-report/by-system <br> PRD: https://parking.ura.gov.sg/ocms/v1/ps-report/by-system |
| Description | Generate PS Report grouped by system (OCMS, PLUS, Backend) |
| Method | POST |
| Header | `{ "Content-Type": "application/json", "Authorization": "Bearer <JWT_TOKEN>" }` |
| Payload | `{ "suspensionDateFrom": "2025-01-01", "suspensionDateTo": "2025-12-31", "suspensionSource": ["OCMS", "PLUS"], "page": 1, "limit": 50 }` |
| Response | `{ "appCode": "OCMS-2000", "message": "Success", "data": [...], "pagination": { "page": 1, "limit": 50, "totalRecords": 150, "totalPages": 3 } }` |
| Response Failure | `{ "appCode": "OCMS-4000", "message": "The duration between the Start Date and End Date must not exceed a year." }` |

#### API PS Report By Officer

| Field | Value |
| --- | --- |
| API Name | ps-report-by-officer |
| URL | UAT: https://parking2.ura.gov.sg/ocms/v1/ps-report/by-officer <br> PRD: https://parking.ura.gov.sg/ocms/v1/ps-report/by-officer |
| Description | Generate PS Report grouped by officer |
| Method | POST |
| Header | `{ "Content-Type": "application/json", "Authorization": "Bearer <JWT_TOKEN>" }` |
| Payload | `{ "suspensionDateFrom": "2025-01-01", "suspensionDateTo": "2025-12-31", "psingOfficerName": ["JOHNLEE"], "authorisingOfficerName": ["ADMIN1"], "page": 1, "limit": 50 }` |
| Response | `{ "appCode": "OCMS-2000", "message": "Success", "data": [...], "pagination": { "page": 1, "limit": 50, "totalRecords": 75, "totalPages": 2 } }` |
| Response Failure | `{ "appCode": "OCMS-4000", "message": "The result set is too large. Please narrow your search." }` |

### Request Parameter Data

| Field Name | Type | Required | Description |
| --- | --- | --- | --- |
| suspensionDateFrom | String (Date) | Yes | Start date (YYYY-MM-DD). Max range: 1 year |
| suspensionDateTo | String (Date) | Yes | End date (YYYY-MM-DD). Max range: 1 year |
| suspensionSource | Array[String] | No | Filter by source: "OCMS", "PLUS" |
| psingOfficerName | Array[String] | No | Filter by PSing Officer (multi-select) |
| authorisingOfficerName | Array[String] | No | Filter by Authorising Officer (multi-select) |
| page | Integer | No | Page number (default: 1) |
| limit | Integer | No | Records per page (default: 50, max: 1000) |

---

## 5.4 Data Mapping

### 5.4.1 Report Data Mapping for PS by Sub-System

| Zone | Database Table | Field Name |
| --- | --- | --- |
| Intranet | ocms_valid_offence_notice | vehicle_no |
| Intranet | ocms_valid_offence_notice | offence_notice_type |
| Intranet | ocms_valid_offence_notice | vehicle_registration_type |
| Intranet | ocms_valid_offence_notice | vehicle_category |
| Intranet | ocms_valid_offence_notice | computer_rule_code |
| Intranet | ocms_valid_offence_notice | notice_date_and_time |
| Intranet | ocms_valid_offence_notice | pp_code |
| Intranet | ocms_suspended_notice | notice_no |
| Intranet | ocms_suspended_notice | suspension_source |
| Intranet | ocms_suspended_notice | date_of_suspension |
| Intranet | ocms_suspended_notice | reason_of_suspension |
| Intranet | New DB table for notices to be refunded | refund_identified_date |

### 5.4.2 Report Data Mapping for PS by Officer

| Zone | Database Table | Field Name |
| --- | --- | --- |
| Intranet | ocms_valid_offence_notice | vehicle_no |
| Intranet | ocms_valid_offence_notice | offence_notice_type |
| Intranet | ocms_valid_offence_notice | vehicle_registration_type |
| Intranet | ocms_valid_offence_notice | vehicle_category |
| Intranet | ocms_valid_offence_notice | computer_rule_code |
| Intranet | ocms_valid_offence_notice | notice_date_and_time |
| Intranet | ocms_valid_offence_notice | pp_code |
| Intranet | ocms_suspended_notice | notice_no |
| Intranet | ocms_suspended_notice | officer_authorising_suspension |
| Intranet | ocms_suspended_notice | cre_user_id |
| Intranet | ocms_suspended_notice | date_of_suspension |
| Intranet | ocms_suspended_notice | reason_of_suspension |
| Intranet | ocms_suspended_notice | suspension_remarks |
| Intranet | New DB table for notices to be refunded | refund_identified_date |

---

## 5.5 Success Outcome

- User successfully accesses Report module
- Search parameters validated successfully
- Query executed within acceptable time
- Report generated with both sections (PS by Sub-System and PS by OIC)
- Report exported successfully
- The workflow reaches the End state without triggering any error-handling paths

---

## 5.6 Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Invalid Date Range | Date range exceeds 1 year or dates are invalid | Display prompt "The duration between the Start Date and End Date must not exceed a year." |
| Result Set Too Large | Search results exceed 1000 records | Display prompt "The result set is too large. Please narrow your search by adding another search criteria." |
| Missing Required Field | Mandatory search parameter not provided | Display prompt "Please enter another field to complete search" |
| Database Error | Unable to retrieve data from database | Display system error message |

---

## 5.7 Search Parameters

| Search Parameter | Description | Functionality |
| --- | --- | --- |
| PSing Officer Name | Drop-down list for user to select PSing Officer's Name | Allows multi-selection of values or select ALL. Retrieval Combinations with PS Suspension Date Range |
| Authorising Officer Name | Drop-down list for user to select Authorising Officer's Name | Allows multi-selection of values or select ALL. Retrieval Combinations with PS Suspension Date Range |
| PS Suspension Date From* | Date picker for start date. Validation: Cannot be later than today's date, cannot be later than "To" date, cannot be earlier than 1 year before today's date. | Mandatory to retrieve PS Notices |
| PS Suspension Date To* | Date picker for end date. Validation: Cannot be later than today's date, cannot be earlier than "From" date, cannot be earlier than 1 year before today's date. | Mandatory to retrieve PS Notices |

*Denotes mandatory fields

---

## 5.8 Retrieval Behavior

| Functionality & Behavior | Description |
| --- | --- |
| Multi-selection of values | Users can select multiple officer names from the drop-down list, with an option to Select All officers. |
| Multi-Parameter Retrieval Criteria | The parameter must be used with another parameter. The page will display the prompt "Please enter another field to complete search" if this is the only search field input. |
| Search Result Limits | If the search results exceeds 1000 records, the system will abort the search function. The page will display the prompt "The result set is too large. Please narrow your search by adding another search criteria." |
| 1 Year Suspension Period Query Limit | The maximum period for querying suspension dates is one year. If the selected duration exceeds this limit, the system will display the prompt: "The duration between the Start Date and End Date must not exceed a year." |

---

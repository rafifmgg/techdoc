# OCMS 17 – Temporary Suspension

**Prepared by**

MGG SOFTWARE

---

## Version History

| Version | Updated By | Date | Changes |
| --- | --- | --- | --- |
| v1.0 | Claude Code | 20/01/2026 | Document Initiation |

---

## Table of Content

| Section | Content | Pages |
| --- | --- | --- |
| 1 | Temporary Suspension | |
| 1.1 | Use Case | |
| 1.2 | High-Level Processing Flow | |
| 2 | OCMS Staff Portal Manual TS | |
| 2.1 | Use Case | |
| 2.2 | High-Level Processing Flow | |
| 2.3 | UI-Side Validation | |
| 2.4 | Backend-Side Validation | |
| 2.5 | Apply Temporary Suspension | |
| 2.5.1 | API Specification | |
| 2.5.2 | Data Mapping | |
| 2.5.3 | Success Outcome | |
| 2.5.4 | Error Handling | |
| 3 | Auto Temporary Suspensions in OCMS | |
| 3.1 | Use Case | |
| 3.2 | Auto TS Triggers Processing Flow | |
| 3.3 | Data Mapping | |
| 3.4 | Success Outcome | |
| 3.5 | Error Handling | |
| 4 | Temporary Suspension initiated by PLUS | |
| 4.1 | Use Case | |
| 4.2 | PLUS Apply TS Processing Flow | |
| 4.3 | API Specification | |
| 4.4 | Data Mapping | |
| 4.5 | Success Outcome | |
| 4.6 | Error Handling | |
| 5 | Looping Temporary Suspensions | |
| 5.1 | Use Case | |
| 5.2 | Looping TS Processing Flow | |
| 5.3 | Data Mapping | |
| 5.4 | Success Outcome | |
| 5.5 | Error Handling | |

---

# Section 1 – Temporary Suspension

## 1.1 Use Case

1. Temporary Suspension (TS) may be applied to Notices as they progress through the standard Notice Processing Workflow.

2. A TS is applied when a Notice requires further investigation.

3. A suspension code will be used to indicate the reason of suspension.

4. Notices can be temporarily suspended through the following modes:<br>a. Manual suspension by EPU OICs via the OCMS Staff Portal<br>b. Automatic suspension by the system when predefined errors or data exceptions occur during batch processing<br>c. Manual suspension by PLU Officers via the PLUS Staff Portal

5. When a TS is applied to an Offence Notice:<br>a. OCMS pauses processing for a predefined period (the Suspension Period).<br>b. The Notice remains at its last processing stage during the Suspension Period.<br>c. The Notice does not advance to the next stage when the Next Processing Date is reached.

6. When multiple TS are applied to an Offence Notice, the notice will only be revived based on the latest revival date.

7. When the Suspension Period expires, OCMS automatically resumes processing. The Notice may:<br>a. Continue along the standard workflow if the Next Processing Date has not yet been reached<br>b. Move to the next processing stage if the Next Processing Date has passed<br>c. Be suspended again if the suspension code is a Looping TS code

## 1.2 High-Level Processing Flow

![Flow Diagram](./OCMS17_Flowchart.drawio - Tab 1)

NOTE: Due to page size limit, the full-sized image is appended.

| No. | Process Name | Description of Process |
| --- | --- | --- |
| 1 | Start | User or system initiates TS operation. |
| 2 | Determine TS Mode | System determines which mode of TS to apply: OCMS Staff Manual, PLUS Manual, or Auto Backend. |
| 3 | OCMS Staff Manual TS | If OCMS Staff Portal, user searches and selects notices, then applies TS via Staff Portal interface. |
| 4 | PLUS Manual TS | If PLUS Staff Portal, PLM applies TS via PLUS interface which calls OCMS API. |
| 5 | Auto Backend TS | If Auto TS triggered by cron job detecting exception scenarios. |
| 6 | Validate TS Request | Backend validates the TS request (source authorization, notice eligibility, processing stage). |
| 7 | Apply TS | If validation passed, backend updates VON table and inserts record into Suspended Notice table. |
| 8 | Return Response | Backend returns success or error response to the caller. |
| 9 | End | TS operation completed. |

---

# Section 2 – OCMS Staff Portal Manual TS

## 2.1 Use Case

1. The manual TS function in the OCMS Staff Portal allows authorised OICs to temporarily suspend Notices.

2. Users can search for the notices using the Staff Portal's Search Notice function by:<br>a. Entering search parameters to retrieve one or more Notices<br>b. Uploading a spreadsheet containing a list of notice numbers to retrieve the Notices

3. From the search results, Users can apply TS using one of two ways:<br>a. Select one or more Notices in the Search Result interface and trigger the TS operation from the drop-down list to initiate a batch TS<br>b. Click on a Notice to view the details and apply the TS from the View Notice Details interface

4. When the TS is initiated from the Staff Portal, the Staff Portal validates the suspension eligibility of each Notice before redirecting Users to the Suspension Detail Page to:<br>a. Select the TS Suspension Code<br>b. Review/edit the Suspension Period<br>c. Input Suspension Remarks, if any

5. After the Suspension Details are submitted, OCMS sends the suspension details to the OCMS backend for processing.

6. If TS is successful, Users will be presented with a success message on screen; an error message will be displayed if the suspension is not successful.

## 2.2 High-Level Processing Flow

![Flow Diagram](./OCMS17_Flowchart.drawio - Tab 4)

NOTE: Due to page size limit, the full-sized image is appended.

| No. | Process Name | Description of Process |
| --- | --- | --- |
| 1 | Start | User logged in to OCMS Staff Portal. |
| 2 | Search for Notice(s) | User navigates to Search Notice page and enters search criteria. |
| 3 | Select Notice(s) | User selects one or more notices from search results (maximum 10). |
| 4 | Validate Eligibility | Staff Portal checks if selected notices are eligible for TS based on processing stage and existing suspension status. |
| 5 | Redirect to Suspension Detail Page | Staff Portal redirects user to Suspension Detail Page. |
| 6 | Input Suspension Details | User selects TS Code, reviews suspension period, and enters remarks. |
| 7 | Submit TS Request | User confirms and submits the TS application. |
| 8 | Backend Validation | Backend validates authentication, authorization, and business rules. |
| 9 | Apply TS to Database | Backend updates VON table and inserts suspended notice record. |
| 10 | Return Response | Backend returns success or error response. |
| 11 | Display Result | Staff Portal displays success message or error details to user. |
| 12 | End | TS operation completed. |

## 2.3 UI-Side Validation

![Flow Diagram](./OCMS17_Flowchart.drawio - Tab 2)

NOTE: Due to page size limit, the full-sized image is appended.

| No. | Validation Rule | Description | Error Action |
| --- | --- | --- | --- |
| 1 | User Permission | Check if user has TS permission in their role. | Hide "Apply TS" option if no permission. |
| 2 | Notice Selection Count | At least 1 notice must be selected, maximum 10 notices per batch. | Display error message if invalid count. |
| 3 | TS Code Selection | A TS code must be selected from dropdown. | Disable Submit button until code selected. |
| 4 | Suspension Remarks | Maximum 200 characters allowed. | Display character count and prevent excess input. |
| 5 | Processing Stage Check | Notice must be at an allowed processing stage for the selected TS code. | Mark notice as ineligible with reason. |
| 6 | Existing Suspension Check | Check if notice already has active TS or PS. | Mark notice as ineligible if PS exists (except DIP, MID, FOR, RIP, RP2). |
| 7 | Payment Status Check | Check if notice is already paid (amount_paid > 0). | Mark notice as ineligible if paid. |

Refer to FD Section 2.6 for detailed UI-Side Validation Process Flow.

## 2.4 Backend-Side Validation

![Flow Diagram](./OCMS17_Flowchart.drawio - Tab 3)

NOTE: Due to page size limit, the full-sized image is appended.

| No. | Validation Rule | Description | Error Code |
| --- | --- | --- | --- |
| 1 | JWT Token Validation | Validate JWT token is present and valid. | OCMS-4001 |
| 2 | User Permission Check | Check if user has TS permission. | OCMS-4001 |
| 3 | Payload Validation - noticeNo | noticeNo array must not be empty. | OCMS-4000 |
| 4 | Payload Validation - suspensionType | suspensionType must be "TS". | OCMS-4000 |
| 5 | Payload Validation - reasonOfSuspension | reasonOfSuspension code must not be empty. | OCMS-4000 |
| 6 | TS Code Exists | Query ocms_suspension_reason to verify code exists and is active. | OCMS-4000 |
| 7 | Source Authorization | Check if source (OCMS_FE) is authorized for the TS code via reason_source field. | OCMS-4000 |
| 8 | Notice Exists | Query ocms_valid_offence_notice to verify notice exists. | OCMS-4004 |
| 9 | Notice Status Valid | Check notice is not cancelled or void. | OCMS-4002 |
| 10 | Processing Stage Allowed | Check last_processing_stage is in allowed stages for the TS code. | OCMS-4002 |
| 11 | Existing PS Check | If notice has PS, check if PS reason is in exception list (DIP, MID, FOR, RIP, RP2). | OCMS-4002 |
| 12 | Existing TS Handling | If notice has existing TS, revive it with reason 'TSR' before applying new TS. | - |

Refer to FD Section 2.7 for detailed validation rules and FD Section 2.8 for OCMS Backend TS Response Scenarios.

## 2.5 Apply Temporary Suspension

This section describes the API and database operations when applying TS via OCMS Staff Portal.

### 2.5.1 API Specification

#### POST /staff-apply-suspension

| Field | Value |
| --- | --- |
| API Name | staff-apply-suspension |
| URL | POST /v1/staff-apply-suspension |
| Description | Apply Temporary Suspension to one or more notices via OCMS Staff Portal |
| Method | POST |
| Header | `{ "Authorization": "Bearer {token}", "Content-Type": "application/json" }` |

**Request Payload:**

```json
{
  "noticeNo": ["500500303J", "500500304J"],
  "suspensionType": "TS",
  "reasonOfSuspension": "ACR",
  "daysToRevive": 21,
  "suspensionRemarks": "ACRA investigation pending",
  "officerAuthorisingSuspension": "JOHNLEE",
  "caseNo": ""
}
```

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| noticeNo | Array[String] | Yes | Array of notice numbers (max 10) |
| suspensionType | String | Yes | Must be "TS" for Temporary Suspension |
| reasonOfSuspension | String | Yes | TS code (ACR, CLV, FPL, HST, INS, MS, NRO, OLD, OUT, PAM, PDP, PRI, ROV, SYS, UNC) |
| daysToRevive | Integer | No | Optional override for suspension days. If not provided, uses value from ocms_suspension_reason |
| suspensionRemarks | String | No | Optional remarks (max 200 characters) |
| officerAuthorisingSuspension | String | Yes | Officer ID authorising the suspension |
| caseNo | String | No | Optional case number |

**Response (Success):**

```json
{
  "totalProcessed": 2,
  "successCount": 2,
  "errorCount": 0,
  "results": [
    {
      "noticeNo": "500500303J",
      "srNo": "12345",
      "dueDateOfRevival": "2026-02-10T00:00:00",
      "appCode": "OCMS-2000",
      "message": "TS Success"
    },
    {
      "noticeNo": "500500304J",
      "srNo": "12346",
      "dueDateOfRevival": "2026-02-10T00:00:00",
      "appCode": "OCMS-2000",
      "message": "TS Success"
    }
  ]
}
```

**Response (Partial Success):**

```json
{
  "totalProcessed": 2,
  "successCount": 1,
  "errorCount": 1,
  "results": [
    {
      "noticeNo": "500500303J",
      "srNo": "12345",
      "appCode": "OCMS-2000",
      "message": "TS Success"
    },
    {
      "noticeNo": "500500304J",
      "appCode": "OCMS-4002",
      "message": "TS Code cannot be applied due to Last Processing Stage"
    }
  ]
}
```

### 2.5.2 Data Mapping

**Step 1: Update Valid Offence Notice (Parent Table)**

| Zone | Database Table | Field Name | Value/Source |
| --- | --- | --- | --- |
| Intranet | ocms_valid_offence_notice | suspension_type | 'TS' |
| Intranet | ocms_valid_offence_notice | due_date_of_revival | CURRENT_TIMESTAMP + no_of_days_for_revival from ocms_suspension_reason |
| Intranet | ocms_valid_offence_notice | is_sync | 'N' |
| Intranet | ocms_valid_offence_notice | upd_date | CURRENT_TIMESTAMP |
| Intranet | ocms_valid_offence_notice | upd_user_id | ocmsiz_app_conn |

**Step 2: Insert Suspended Notice (Child Table)**

| Zone | Database Table | Field Name | Value/Source |
| --- | --- | --- | --- |
| Intranet | ocms_suspended_notice | notice_no | From request payload |
| Intranet | ocms_suspended_notice | sr_no | nextval from SUSPENDED_NOTICE_SEQ |
| Intranet | ocms_suspended_notice | suspension_source | 'OCMS' |
| Intranet | ocms_suspended_notice | suspension_type | 'TS' |
| Intranet | ocms_suspended_notice | reason_of_suspension | From request payload (TS code) |
| Intranet | ocms_suspended_notice | officer_authorising_suspension | From request payload |
| Intranet | ocms_suspended_notice | due_date_of_revival | CURRENT_TIMESTAMP + no_of_days_for_revival |
| Intranet | ocms_suspended_notice | suspension_remarks | From request payload |
| Intranet | ocms_suspended_notice | date_of_suspension | CURRENT_TIMESTAMP |
| Intranet | ocms_suspended_notice | cre_date | CURRENT_TIMESTAMP |
| Intranet | ocms_suspended_notice | cre_user_id | ocmsiz_app_conn |
| Intranet | ocms_suspended_notice | process_indicator | 'manual' |

### 2.5.3 Success Outcome

- VON table updated with suspension_type = 'TS' and due_date_of_revival calculated.
- New record inserted into ocms_suspended_notice with unique sr_no.
- API returns success response with totalProcessed, successCount, and sr_no for each notice.
- Staff Portal displays success message to user.

### 2.5.4 Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| Authentication Failed | OCMS-4001 | You are not authorized. Please log in and try again. | JWT token invalid or expired. |
| Authorization Failed | OCMS-4001 | You are not authorized for this operation. | User does not have TS permission. |
| Invalid Request | OCMS-4000 | Invalid request. Please check and try again. | Missing or invalid payload fields. |
| Invalid TS Code | OCMS-4000 | Invalid suspension code. | TS code not found or inactive. |
| Source Not Authorized | OCMS-4000 | Source not authorized for this suspension code. | OCMS_FE not in reason_source. |
| Notice Not Found | OCMS-4004 | Notice not found. | Notice does not exist in database. |
| Invalid Notice Status | OCMS-4002 | TS cannot be applied to cancelled/void notice. | Notice is cancelled or void. |
| Invalid Processing Stage | OCMS-4002 | TS Code cannot be applied due to Last Processing Stage. | Processing stage not in allowed list. |
| PS Conflict | OCMS-4002 | TS cannot be applied on notices with PS. | PS exists and not in exception list. |
| System Error | OCMS-4007 | Something went wrong. Please try again later. | Database or system error. |

---

# Section 3 – Auto Temporary Suspensions in OCMS

## 3.1 Use Case

1. The OCMS system will automatically trigger Temporary Suspensions to Notices when pre-defined scenarios occur.

2. The scenarios are pre-defined in the Notice Processing Workflow at various Processing Stages.

3. When the conditions of the scenarios are met, OCMS will automatically Suspend the Notices with pre-defined Suspension Codes and alert OICs about the exceptions.

## 3.2 Auto TS Triggers Processing Flow

![Flow Diagram](./OCMS17_Flowchart.drawio - Tab 6)

NOTE: Due to page size limit, the full-sized image is appended.

| Scenario | TS Code | Auto Triggered by | Description |
| --- | --- | --- | --- |
| ACRA Exception | ACR | OCMS Backend | DataHive responds that Company is deregistered or no suitable match for director/owner/partner. |
| Appeal Linked | APP | PLUS Backend | Notice has been linked to an Appeal via the eService. |
| Classified Vehicle | CLV | OCMS Backend | Notice for VIP vehicle remains unpaid at end of RR3/DR3 stage. |
| House Tenants | HST | OCMS Backend | Offender no longer residing at registered address, no new address furnished. |
| MHA/DataHive Exception | NRO | OCMS Backend | MHA responds address invalid, ID invalid, or no record returned. |
| Partial Match | PAM | OCMS Backend | Vehicle number mismatch between Internet and Intranet. |
| Pending Driver Furnish | PDP | OCMS Backend | Driver details furnished via eService requires OIC verification. |
| ROV Exception | ROV | OCMS Backend | LTA file error or vehicle record error during ownership query. |
| System Error | SYS | OCMS Backend | Interface failure or system issues during Notice processing. |

Refer to FD Section 4.2 for detailed scenarios that auto trigger TS.

## 3.3 Data Mapping

**Update Valid Offence Notice (Parent Table)**

| Zone | Database Table | Field Name | Value/Source |
| --- | --- | --- | --- |
| Intranet | ocms_valid_offence_notice | suspension_type | 'TS' |
| Intranet | ocms_valid_offence_notice | due_date_of_revival | CURRENT_TIMESTAMP + no_of_days_for_revival |
| Intranet | ocms_valid_offence_notice | is_sync | 'N' |
| Intranet | ocms_valid_offence_notice | upd_date | CURRENT_TIMESTAMP |
| Intranet | ocms_valid_offence_notice | upd_user_id | ocmsiz_app_conn |

**Insert Suspended Notice (Child Table)**

| Zone | Database Table | Field Name | Value/Source |
| --- | --- | --- | --- |
| Intranet | ocms_suspended_notice | notice_no | From cron job processing |
| Intranet | ocms_suspended_notice | sr_no | nextval from SUSPENDED_NOTICE_SEQ |
| Intranet | ocms_suspended_notice | suspension_source | 'OCMS' |
| Intranet | ocms_suspended_notice | suspension_type | 'TS' |
| Intranet | ocms_suspended_notice | reason_of_suspension | Auto TS code based on scenario |
| Intranet | ocms_suspended_notice | officer_authorising_suspension | NULL (auto) |
| Intranet | ocms_suspended_notice | due_date_of_revival | CURRENT_TIMESTAMP + no_of_days_for_revival |
| Intranet | ocms_suspended_notice | suspension_remarks | Auto-generated based on scenario |
| Intranet | ocms_suspended_notice | date_of_suspension | CURRENT_TIMESTAMP |
| Intranet | ocms_suspended_notice | cre_date | CURRENT_TIMESTAMP |
| Intranet | ocms_suspended_notice | cre_user_id | ocmsiz_app_conn |
| Intranet | ocms_suspended_notice | process_indicator | 'auto' |

## 3.4 Success Outcome

- Auto TS applied successfully when exception scenario detected.
- VON table updated with suspension_type = 'TS'.
- New record inserted into ocms_suspended_notice.
- OIC alerted about the exception via email notification.

## 3.5 Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Database Error | Database connection or query failure. | Log error and retry on next cron run. |
| Notice Already Suspended | Notice already has active TS or PS. | Skip auto TS, log status. |
| Invalid Processing Stage | Notice not at allowed stage for auto TS code. | Skip auto TS, log reason. |

---

# Section 4 – Temporary Suspension initiated by PLUS

## 4.1 Use Case

1. PLUS can apply a TS via an API provided by OCMS.

2. The PLUS system and PLUS Users will initiate TS under the following circumstances:<br>a. One or more notices are linked to an appeal.<br>b. Appeal Processing Officers (PLMs) suspend a notice while the appeal is under review.<br>c. PLMs extend an existing suspension to allow more time for investigation.<br>d. Call Centre Officers suspend a notice while checks are ongoing.<br>e. PLMs reduce the Notice amount payable and grant the offender a payment grace period.

3. When a suspension is initiated through the PLUS Staff Portal or backend, the PLUS system shall provide the suspension details, which will be logged in OCMS in the Suspended Notices database table.

4. Upon receiving the request, OCMS validates the submitted suspension data before proceeding with the TS.

5. PLUS can apply TS to multiple Notices simultaneously.

## 4.2 PLUS Apply TS Processing Flow

![Flow Diagram](./OCMS17_Flowchart.drawio - Tab 5)

NOTE: Due to page size limit, the full-sized image is appended.

| No. | Process Name | Description of Process |
| --- | --- | --- |
| 1 | Start | PLM initiates TS operation from PLUS Staff Portal. |
| 2 | Select Notice(s) | PLM searches and selects notices to apply TS. |
| 3 | Select TS Code | PLM selects from allowed PLUS TS codes (APE, APP, CCE, MS, PRI, RED). |
| 4 | Input Details | PLM enters case number, suspension remarks, and other details. |
| 5 | Submit Request | PLUS calls OCMS API POST /suspension/temporary. |
| 6 | Backend Validation | OCMS validates request (source = PLUS, code authorized for PLUS). |
| 7 | Apply TS | OCMS updates VON and inserts suspended notice record. |
| 8 | Return Response | OCMS returns success or error response to PLUS. |
| 9 | Display Result | PLUS displays result to PLM. |
| 10 | End | TS operation completed. |

Refer to FD Section 5.2 for detailed PLUS TS process flows.

## 4.3 API Specification

#### POST /plus-apply-suspension

| Field | Value |
| --- | --- |
| API Name | plus-apply-suspension |
| URL | POST /v1/plus-apply-suspension |
| Description | Apply Temporary Suspension to notices via PLUS system |
| Method | POST |
| Header | `{ "Authorization": "Bearer {token}", "Content-Type": "application/json" }` |
| Query Parameter | `checking` (optional, default: false) - Set to true for validation-only mode |

**Request Payload:**

```json
{
  "noticeNo": ["500500303J"],
  "caseNo": "P250113001",
  "suspensionType": "TS",
  "reasonOfSuspension": "APP",
  "daysToRevive": 21,
  "suspensionRemarks": "Appeal under review",
  "officerAuthorisingSuspension": "PLU_1"
}
```

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| noticeNo | Array[String] | Yes | Array of notice numbers |
| caseNo | String | Yes | PLUS case number |
| suspensionType | String | Yes | Must be "TS" for Temporary Suspension |
| reasonOfSuspension | String | Yes | PLUS TS code (APE, APP, CCE, MS, PRI, RED) |
| daysToRevive | Integer | No | Optional override for suspension days |
| suspensionRemarks | String | No | Optional remarks |
| officerAuthorisingSuspension | String | Yes | PLUS officer ID |

**Response (Success - Execution Mode):**

```json
{
  "totalProcessed": 1,
  "successCount": 1,
  "errorCount": 0,
  "results": [
    {
      "noticeNo": "500500303J",
      "srNo": "12346",
      "dueDateOfRevival": "2026-02-10T00:00:00",
      "appCode": "OCMS-2000",
      "message": "TS Success"
    }
  ]
}
```

**Response (Validation Mode - checking=true):**

```json
{
  "appCode": "OCMS-2000",
  "message": "Validate suspension success",
  "noticeNo": "500500303J",
  "suspensionType": "TS",
  "reasonOfSuspension": "APP",
  "eligible": true
}
```

**Response (Error):**

```json
{
  "totalProcessed": 1,
  "successCount": 0,
  "errorCount": 1,
  "results": [
    {
      "noticeNo": "500500303J",
      "appCode": "OCMS-4000",
      "message": "Source not authorized to use this Suspension Code"
    }
  ]
}
```

## 4.4 Data Mapping

**Update Valid Offence Notice (Parent Table)**

| Zone | Database Table | Field Name | Value/Source |
| --- | --- | --- | --- |
| Internet | eocms_valid_offence_notice | suspension_type | 'TS' |
| Internet | eocms_valid_offence_notice | due_date_of_revival | From request or calculated |
| Internet | eocms_valid_offence_notice | is_sync | 'N' |
| Internet | eocms_valid_offence_notice | upd_date | CURRENT_TIMESTAMP |
| Internet | eocms_valid_offence_notice | upd_user_id | ocmsez_app_conn |

**Insert Suspended Notice (Child Table)**

| Zone | Database Table | Field Name | Value/Source |
| --- | --- | --- | --- |
| Internet | eocms_suspended_notice | notice_no | From request payload |
| Internet | eocms_suspended_notice | sr_no | nextval from SUSPENDED_NOTICE_SEQ |
| Internet | eocms_suspended_notice | suspension_source | 'PLUS' |
| Internet | eocms_suspended_notice | suspension_type | 'TS' |
| Internet | eocms_suspended_notice | reason_of_suspension | From request payload (APE/APP/CCE/MS/PRI/RED) |
| Internet | eocms_suspended_notice | officer_authorising_suspension | From request payload |
| Internet | eocms_suspended_notice | due_date_of_revival | From request or CURRENT_TIMESTAMP + no_of_days_for_revival |
| Internet | eocms_suspended_notice | suspension_remarks | From request payload |
| Internet | eocms_suspended_notice | date_of_suspension | From request (cre_date) or CURRENT_TIMESTAMP |
| Internet | eocms_suspended_notice | case_no | From request payload |
| Internet | eocms_suspended_notice | cre_date | From request payload |
| Internet | eocms_suspended_notice | cre_user_id | ocmsez_app_conn |
| Internet | eocms_suspended_notice | process_indicator | 'manual' |

Refer to FD Section 5.6 for detailed Temporary Suspension Data from PLUS.

## 4.5 Success Outcome

- PLUS TS applied successfully via OCMS API.
- VON table (Internet) updated with suspension_type = 'TS'.
- New record inserted into eocms_suspended_notice.
- API returns success response to PLUS.
- Data synced to Intranet via standard sync process.

## 4.6 Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| Authentication Failed | OCMS-4001 | You are not authorized. | API token invalid. |
| Invalid TS Code | OCMS-4000 | Invalid suspension code. | TS code not allowed for PLUS. |
| Source Not Authorized | OCMS-4000 | Source not authorized. | PLUS not in reason_source for this code. |
| Notice Not Found | OCMS-4004 | Notice not found. | Notice does not exist. |
| Invalid Processing Stage | OCMS-4002 | TS cannot be applied at this stage. | Processing stage not allowed. |
| PS Conflict | OCMS-4002 | TS cannot be applied on notices with PS. | PS exists and not in exception list. |

Refer to FD Section 5.4.1 for PLUS API request parameters.

---

# Section 5 – Looping Temporary Suspensions

## 5.1 Use Case

1. Looping TS is applicable only to selected Suspension Codes, specifically:<br>a. CLV (Classified Vehicles) - 21 days<br>b. HST (House Tenants) - 30 days

2. Looping occurs when a Notice's TS period expires and the suspension is re-applied immediately.

3. A Looping TS will continue to re-apply the same suspension until it is lifted manually by an OIC.

*Note: While it is called a Looping TS, the function to detect the Suspension Code and auto re-apply the TS is triggered by the Suspension Revival function.*

## 5.2 Looping TS Processing Flow

![Flow Diagram](./OCMS17_Flowchart.drawio - Tab 7)

NOTE: Due to page size limit, the full-sized image is appended.

| No. | Process Name | Description of Process |
| --- | --- | --- |
| 1 | Start | Daily OCMS Backend cron job runs to check for suspended notices due for revival. |
| 2 | Query Suspended Notices | OCMS queries ocms_suspended_notice for records with due_date_of_revival reached and date_of_revival IS NULL. |
| 3 | Check Suspension Code | System checks if the suspension code is CLV or HST. |
| 4 | Non-Looping Code | If code is NOT CLV or HST, proceed with normal revival (OCMS 19 flow). |
| 5 | Looping Code Detected | If code IS CLV or HST, proceed with looping TS. |
| 6 | Lift Existing Suspension | Update existing suspended notice record: set date_of_revival = CURRENT_TIMESTAMP, revival_reason = 'Auto Revival - Looping TS'. |
| 7 | Calculate New Revival Date | Calculate new due_date_of_revival = CURRENT_TIMESTAMP + no_of_days_for_revival. |
| 8 | Create New TS Record | Insert new record into ocms_suspended_notice with same TS code and new revival date. |
| 9 | Update VON | Update ocms_valid_offence_notice with new due_date_of_revival. |
| 10 | Log Action | Log looping TS action for audit. |
| 11 | End | Looping TS applied. Will be checked again on new due date. |

Refer to FD Section 6.2 for detailed Looping TS Process Flow.

## 5.3 Data Mapping

**Step 1: Revive Existing Suspended Notice**

| Zone | Database Table | Field Name | Value/Source |
| --- | --- | --- | --- |
| Intranet | ocms_suspended_notice | date_of_revival | CURRENT_TIMESTAMP |
| Intranet | ocms_suspended_notice | revival_reason | 'Auto Revival - Looping TS' |
| Intranet | ocms_suspended_notice | upd_date | CURRENT_TIMESTAMP |
| Intranet | ocms_suspended_notice | upd_user_id | ocmsiz_app_conn |

**Step 2: Insert New Suspended Notice (Looping)**

| Zone | Database Table | Field Name | Value/Source |
| --- | --- | --- | --- |
| Intranet | ocms_suspended_notice | notice_no | Same as revived notice |
| Intranet | ocms_suspended_notice | sr_no | nextval from SUSPENDED_NOTICE_SEQ |
| Intranet | ocms_suspended_notice | suspension_source | 'OCMS' |
| Intranet | ocms_suspended_notice | suspension_type | 'TS' |
| Intranet | ocms_suspended_notice | reason_of_suspension | Same as revived (CLV or HST) |
| Intranet | ocms_suspended_notice | officer_authorising_suspension | NULL (auto looping) |
| Intranet | ocms_suspended_notice | due_date_of_revival | CURRENT_TIMESTAMP + no_of_days_for_revival (21 for CLV, 30 for HST) |
| Intranet | ocms_suspended_notice | suspension_remarks | 'Auto Looping TS' |
| Intranet | ocms_suspended_notice | date_of_suspension | CURRENT_TIMESTAMP |
| Intranet | ocms_suspended_notice | cre_date | CURRENT_TIMESTAMP |
| Intranet | ocms_suspended_notice | cre_user_id | ocmsiz_app_conn |
| Intranet | ocms_suspended_notice | process_indicator | 'auto' |

**Step 3: Update Valid Offence Notice**

| Zone | Database Table | Field Name | Value/Source |
| --- | --- | --- | --- |
| Intranet | ocms_valid_offence_notice | due_date_of_revival | New calculated date |
| Intranet | ocms_valid_offence_notice | is_sync | 'N' |
| Intranet | ocms_valid_offence_notice | upd_date | CURRENT_TIMESTAMP |
| Intranet | ocms_valid_offence_notice | upd_user_id | ocmsiz_app_conn |

## 5.4 Success Outcome

- Existing TS revived with revival_reason = 'Auto Revival - Looping TS'.
- New TS record created with same suspension code (CLV or HST).
- VON table updated with new due_date_of_revival.
- Looping continues until manually lifted by OIC.

## 5.5 Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Database Error | Database connection or query failure. | Log error and retry on next cron run. |
| Notice Not Found | Notice no longer exists or was deleted. | Skip looping, log error. |
| Notice Already Revived | Notice was manually revived before cron. | Skip looping, log status. |
| Invalid Suspension Code | Suspension code not in looping list. | Proceed with normal revival. |

---

# Appendix

## TS Suspension Codes Reference

Refer to FD Section 2.5 for complete list of TS Suspension Codes with:
- Suspension days
- Allowed processing stages
- Source authorization (OCMS_FE, PLUS, OCMS_BE)
- Looping indicator

## Processing Stages Reference

| Code | Stage Name |
| --- | --- |
| NPA | Notice of Parking Action |
| ROV | Registry of Vehicles |
| ENA | Enforcement Notice A |
| RD1 | Reminder (Driver) 1 |
| RD2 | Reminder (Driver) 2 |
| RR3 | Reminder (Registered Owner) 3 |
| DN1 | Default Notice 1 |
| DN2 | Default Notice 2 |
| DR3 | Default Notice (Registered Owner) 3 |
| CPC | Court Pre-Charge |
| CFC | Court File Creation |

## Error Codes Reference

Refer to FD Section 2.8 for complete OCMS Backend TS Response Scenarios and error codes.

---

*End of Document*

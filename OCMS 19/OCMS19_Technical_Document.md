# OCMS 19 – Revive Suspensions

**Technical Document**

**Prepared by**

MGG SOFTWARE

---

## Version History

| Version | Updated By | Date | Changes |
| --- | --- | --- | --- |
| v1.0 | MGG Software | 14/01/2026 | Document Initiation |

---

## Table of Contents

| Section | Content | Page |
| --- | --- | --- |
| 1 | Revive Suspension Function | - |
| 1.1 | Use Case | - |
| 1.2 | High Level Flow | - |
| 1.3 | UI-side Validation | - |
| 1.4 | Backend Validation | - |
| 1.5 | Reviving PS Suspensions | - |
| 1.5.1 | Main Flow - Reviving PS | - |
| 1.5.2 | Sub-Flow - Revive PS-FP/PRA | - |
| 1.5.3 | Sub-Flow - Revive PS-FOR/MID | - |
| 1.5.4 | Sub-Flow - Revive Other PS | - |
| 1.6 | Reviving TS Suspensions | - |
| 1.6.1 | Main Flow - Reviving TS | - |
| 1.6.2 | Sub-Flow - Revive TS-HST/CLV | - |
| 1.6.3 | Micro Flow - Auto Revive HST/CLV | - |
| 1.6.4 | Micro Flow - Manual Revive HST/CLV | - |
| 1.6.5 | Sub-Flow - Revive TS-RED | - |
| 1.6.6 | Sub-Flow - Revive TS-PDP | - |
| 1.6.7 | Micro Flow - Auto Revive PDP | - |
| 1.6.8 | Micro Flow - Manual Revive PDP | - |
| 1.6.9 | Sub-Flow - Revive TS-ROV | - |
| 1.6.10 | Sub-Flow - Revive TS-NRO | - |
| 1.6.11 | Sub-Flow - Revive Other TS | - |
| 1.7 | Common Functions | - |
| 1.7.1 | Update Suspended Notice | - |
| 1.7.2 | Retry with Exponential Backoff | - |
| 1.7.3 | Patch NPD in VON | - |
| 1.7.4 | Manage Failed Follow-up | - |
| 1.8 | Data Mapping | - |
| 1.9 | Success Outcome | - |
| 1.10 | Error Handling | - |
| 2 | OCMS Staff Portal Manual Revival | - |
| 2.1 | Use Case | - |
| 2.2 | Process Flow | - |
| 2.3 | API Specification | - |
| 2.4 | Data Mapping | - |
| 2.5 | Success Outcome | - |
| 2.6 | Error Handling | - |
| 3 | Auto Revive in OCMS | - |
| 3.1 | Use Case | - |
| 3.2 | Auto Revive TS-HST/CLV | - |
| 3.3 | Auto Revive TS-PDP | - |
| 3.4 | Data Mapping | - |
| 3.5 | Success Outcome | - |
| 3.6 | Error Handling | - |
| 4 | Manual Revival initiated by PLUS | - |
| 4.1 | Use Case | - |
| 4.2 | Process Flow | - |
| 4.3 | API Specification | - |
| 4.4 | Data Mapping | - |
| 4.5 | Success Outcome | - |
| 4.6 | Error Handling | - |

---

# Section 1 – Revive Suspension Function

## 1.1 Use Case

- The Revive Suspension function lifts or removes a Temporary Suspension or Permanent Suspension on a Notice for various reasons. Common scenarios include:
  - TS has expired
  - To change the suspension reason
  - To redirect the Notice to another Offender
  - When the Offender's updated particulars has been received
  - To speed up Notice processing

- Notice Revival is initiated when:
  - Notice Processing Officers from EPU manually trigger the revival function via the OCMS Staff Portal
  - OCMS backend auto triggers revival function during Notice Revival batch job processing
  - Appeal Processing Officers from PLU manually trigger the revival function via the PLUS Staff Portal

- During Revival, the Revival function will:
  - Lift the existing TS and PS
  - Update the existing Suspension record with the revival date and Revival Reason
  - Detect whether the PS or TS Suspension Code to be lifted requires follow-up actions, such as to re-apply the same TS Suspension Code or to initiate a refund
  - Initiate the follow-up action if required

## 1.2 High Level Flow

![High Level Flow](./OCMS19_Flowchart_fixed.drawio - Tab 3.1)

### Entry Points

| Entry Point | Source | Description |
| --- | --- | --- |
| Manual Revival (OCMS) | OCMS Staff Portal | OIC manually triggers revival via Apply Operation dropdown |
| Manual Revival (PLUS) | PLUS Staff Portal | PLM manually triggers revival for appeal-related cases |
| Auto Revival | OCMS Backend Cron | System automatically revives TS when due date is reached |

### Flow Description

| Step | Definition | Brief Description |
| --- | --- | --- |
| Receive revival request | System receives revival request from Staff Portal or Cron job | Entry point for revival |
| Staff Portal validation | Portal performs initial check to determine if revival is allowed based on user permission and notice eligibility | Check permission & eligibility |
| Submit request via API | Portal calls backend API to process revival request with notice numbers, revival reason, and officer details | Call backend API |
| Backend validation | Backend validates JWT token, API key, and request payload before processing | Validate request |
| Process revival | Backend updates suspended notice records and clears suspension from Valid Offence Notice (VON) | Update DB records |
| Follow-up action check | Backend checks if additional action is required after revival (e.g., re-apply looping TS, mark refund) | Check follow-up needed |
| Return response | Backend returns consolidated results with success/error status for each notice | Return results

*Refer to FD Section 2.2 for detailed high-level processing flow description.*

---

## 1.3 UI-side Validation

![UI-side Validation Flow](./OCMS19_Flowchart_fixed.drawio - Tab 3.2)

| Step | Definition | Brief Description |
| --- | --- | --- |
| Check user permission | Verify access rights | Portal checks if user has Revival permission |
| Validate notice selection | Check eligibility | Check if selected notices are eligible for revival |
| Validate suspension type | Type validation | Verify TS-HST must use HST module, PS max 1 per request |
| Validate input fields | Field validation | Check revival reason is selected, remarks max 200 chars |
| Display validation result | Show result | Show error message if validation fails, proceed if pass |

*Refer to FD Section 2.3 for detailed validation rules.*

---

## 1.4 Backend Validation

![Backend Validation Flow](./OCMS19_Flowchart_fixed.drawio - Tab 3.3)

| Step | Definition | Brief Description |
| --- | --- | --- |
| Validate authentication | Auth check | Check JWT token and API key are valid |
| Validate request payload | Payload check | Check required fields: noticeNo, suspensionType, revivalReason |
| Validate notice exists | Notice lookup | Query database to verify notice numbers exist |
| Validate suspension active | Status check | Check notice has active suspension matching requested type |
| Validate revival code | Code validation | Check revival reason code is allowed for source (OCMS/PLUS) |
| Return validation result | Return result | Return success or error with appCode and message |

*Refer to FD Section 2.4 for detailed validation rules and error codes.*

---

## 1.5 Reviving PS Suspensions

### 1.5.1 Main Flow - Reviving PS

![Revive PS Main Flow](./OCMS19_Flowchart_fixed.drawio - Tab 3.4.1)

| Step | Definition | Brief Description |
| --- | --- | --- |
| Receive PS revival request | Entry point | Backend receives request to revive PS suspension |
| Identify PS code | Identify code | Determine which PS code is being revived (FP, PRA, FOR, MID, other) |
| Route to sub-flow | Route request | Route to appropriate sub-flow based on PS code |
| Execute sub-flow | Process revival | Call sub-flow to handle PS-specific revival logic |
| Return result | Return status | Return revival result from sub-flow to caller |

### 1.5.2 Sub-Flow - Revive PS-FP/PRA

![Revive PS-FP/PRA](./OCMS19_Flowchart_fixed.drawio - Tab 3.4.2)

| Step | Definition | Brief Description |
| --- | --- | --- |
| Receive PS-FP/PRA request | Entry point | Sub-flow receives request to revive PS-FP or PS-PRA |
| Update suspended notice | Update SN | Call CF: Update Revival in SN DB with revival details |
| Create refund notice record | Create refund | Create record in ocms_refund_notice for refund processing |
| Patch NPD in VON | Update VON | Call CF: Patch NPD in VON to clear suspension |
| Return result | Return status | Return success with refund follow-up status |

### 1.5.3 Sub-Flow - Revive PS-FOR/MID

![Revive PS-FOR/MID](./OCMS19_Flowchart_fixed.drawio - Tab 3.4.3)

| Step | Definition | Brief Description |
| --- | --- | --- |
| Receive PS-FOR/MID request | Entry point | Sub-flow receives request to revive PS-FOR or PS-MID |
| Update suspended notice | Update SN | Call CF: Update Revival in SN DB with revival details |
| Patch NPD in VON | Update VON | Call CF: Patch NPD in VON to clear suspension |
| Return result | Return status | Return success (no follow-up action needed) |

### 1.5.4 Sub-Flow - Revive Other PS

![Revive Other PS](./OCMS19_Flowchart_fixed.drawio - Tab 3.4.4)

| Step | Definition | Brief Description |
| --- | --- | --- |
| Receive other PS request | Entry point | Sub-flow receives request to revive other PS codes |
| Update suspended notice | Update SN | Call CF: Update Revival in SN DB with revival details |
| Patch NPD in VON | Update VON | Call CF: Patch NPD in VON to clear suspension |
| Return result | Return status | Return success (no follow-up action needed) |

*Refer to FD Section 2.5 for PS revival business rules.*

---

## 1.6 Reviving TS Suspensions

### 1.6.1 Main Flow - Reviving TS

![Revive TS Main Flow](./OCMS19_Flowchart_fixed.drawio - Tab 3.5.1)

| Step | Definition | Brief Description |
| --- | --- | --- |
| Receive revival request | Entry point | Main flow receives TS revival request with notice_no, revival_reason, officer_id |
| Route by TS type | Check suspension | Check epr_reason_of_suspension to determine TS sub-type |
| TS-HST or TS-CLV | Sub-flow call | Route to Sub-Flow: Revive TS-HST/CLV |
| TS-RED | Sub-flow call | Route to Sub-Flow: Revive TS-RED |
| TS-PDP | Sub-flow call | Route to Sub-Flow: Revive TS-PDP |
| TS-ROV | Sub-flow call | Route to Sub-Flow: Revive TS-ROV |
| TS-NRO | Sub-flow call | Route to Sub-Flow: Revive TS-NRO |
| Other TS types | Sub-flow call | Route to Sub-Flow: Revive Other TS |
| Aggregate results | Collect results | Collect and aggregate results from sub-flow |
| Return result | Return status | Return success with revival details or error status |

### 1.6.2 Sub-Flow - Revive TS-HST/CLV

![Revive TS-HST/CLV](./OCMS19_Flowchart_fixed.drawio - Tab 3.5.2)

| Step | Definition | Brief Description |
| --- | --- | --- |
| Receive TS-HST/CLV request | Entry point | Sub-flow receives request to revive TS-HST or TS-CLV suspension |
| Check revival source | Check source | Determine if revival is auto (system) or manual (user) triggered |
| Auto revival | Micro flow call | Route to Micro Flow: Auto Revive HST/CLV |
| Manual revival | Micro flow call | Route to Micro Flow: Manual Revive HST/CLV |
| Aggregate results | Collect results | Collect results from micro flow |
| Return result | Return status | Return success with revival details or error status |

### 1.6.3 Micro Flow - Auto Revive HST/CLV

![Auto Revive HST/CLV](./OCMS19_Flowchart_fixed.drawio - Tab 3.5.3)

| Step | Definition | Brief Description |
| --- | --- | --- |
| Receive auto revival request | Entry point | Micro flow receives auto revival request for TS-HST/CLV |
| Update suspended notice | Update SN | Call CF: Update Revival in SN DB with revival_reason = 'SPO' |
| Check for looping TS | Check VIP status | For TS-CLV: Check if VIP at RR3/DR3 is unpaid |
| Apply looping TS | Re-apply suspension | If VIP unpaid, re-apply TS-CLV with new due date |
| Stop looping | Clear suspension | If VIP paid (FP status), stop looping TS |
| Patch NPD in VON | Update VON | Call CF: Patch NPD in VON to clear suspension |
| Return result | Return status | Return success with looping status or clear status |

### 1.6.4 Micro Flow - Manual Revive HST/CLV

![Manual Revive HST/CLV](./OCMS19_Flowchart_fixed.drawio - Tab 3.5.4)

| Step | Definition | Brief Description |
| --- | --- | --- |
| Receive manual revival request | Entry point | Micro flow receives manual revival request from OIC |
| Update suspended notice | Update SN | Call CF: Update Revival in SN DB with revival details |
| Patch NPD in VON | Update VON | Call CF: Patch NPD in VON to clear suspension and set next_processing_date |
| Return result | Return status | Return success with revival confirmation |

### 1.6.5 Sub-Flow - Revive TS-RED

![Revive TS-RED](./OCMS19_Flowchart_fixed.drawio - Tab 3.5.5)

| Step | Definition | Brief Description |
| --- | --- | --- |
| Receive TS-RED revival request | Entry point | Sub-flow receives request to revive TS-RED suspension |
| Update suspended notice | Update SN | Call CF: Update Revival in SN DB with revival details |
| Patch NPD in VON | Update VON | Call CF: Patch NPD in VON to clear suspension |
| Return result | Return status | Return success with revival confirmation |

### 1.6.6 Sub-Flow - Revive TS-PDP

![Revive TS-PDP](./OCMS19_Flowchart_fixed.drawio - Tab 3.5.6)

| Step | Definition | Brief Description |
| --- | --- | --- |
| Receive TS-PDP revival request | Entry point | Sub-flow receives request to revive TS-PDP suspension |
| Check revival source | Check source | Determine if revival is auto (system) or manual (user) triggered |
| Auto revival | Micro flow call | Route to Micro Flow: Auto Revive PDP |
| Manual revival | Micro flow call | Route to Micro Flow: Manual Revive PDP |
| Aggregate results | Collect results | Collect results from micro flow |
| Return result | Return status | Return success with revival details or error status |

### 1.6.7 Micro Flow - Auto Revive PDP

![Auto Revive PDP](./OCMS19_Flowchart_fixed.drawio - Tab 3.5.7)

| Step | Definition | Brief Description |
| --- | --- | --- |
| Receive auto revival request | Entry point | Micro flow receives auto revival request for TS-PDP |
| Update suspended notice | Update SN | Call CF: Update Revival in SN DB with revival_reason = 'SPO' |
| Check for looping TS | Check PDP status | Check if pending dispute payment still exists |
| Apply looping TS | Re-apply suspension | If PDP still pending, re-apply TS-PDP with new due date |
| Stop looping | Clear suspension | If PDP resolved, stop looping TS |
| Patch NPD in VON | Update VON | Call CF: Patch NPD in VON to clear suspension |
| Return result | Return status | Return success with looping status or clear status |

### 1.6.8 Micro Flow - Manual Revive PDP

![Manual Revive PDP](./OCMS19_Flowchart_fixed.drawio - Tab 3.5.8)

| Step | Definition | Brief Description |
| --- | --- | --- |
| Receive manual revival request | Entry point | Micro flow receives manual revival request from OIC |
| Update suspended notice | Update SN | Call CF: Update Revival in SN DB with revival details |
| Patch NPD in VON | Update VON | Call CF: Patch NPD in VON to clear suspension and set next_processing_date |
| Return result | Return status | Return success with revival confirmation |

### 1.6.9 Sub-Flow - Revive TS-ROV

![Revive TS-ROV](./OCMS19_Flowchart_fixed.drawio - Tab 3.5.9)

| Step | Definition | Brief Description |
| --- | --- | --- |
| Receive TS-ROV revival request | Entry point | Sub-flow receives request to revive TS-ROV (Record Of Vehicle) suspension |
| Update suspended notice | Update SN | Call CF: Update Revival in SN DB with revival details |
| Patch NPD in VON | Update VON | Call CF: Patch NPD in VON to clear suspension |
| Return result | Return status | Return success with revival confirmation |

### 1.6.10 Sub-Flow - Revive TS-NRO

![Revive TS-NRO](./OCMS19_Flowchart_fixed.drawio - Tab 3.5.10)

| Step | Definition | Brief Description |
| --- | --- | --- |
| Receive TS-NRO revival request | Entry point | Sub-flow receives request to revive TS-NRO (No Record Owner) suspension |
| Update suspended notice | Update SN | Call CF: Update Revival in SN DB with revival details |
| Patch NPD in VON | Update VON | Call CF: Patch NPD in VON to clear suspension |
| Return result | Return status | Return success with revival confirmation |

### 1.6.11 Sub-Flow - Revive Other TS

![Revive Other TS](./OCMS19_Flowchart_fixed.drawio - Tab 3.5.11)

| Step | Definition | Brief Description |
| --- | --- | --- |
| Receive Other TS revival request | Entry point | Sub-flow receives request to revive other TS types (not HST/CLV/RED/PDP/ROV/NRO) |
| Update suspended notice | Update SN | Call CF: Update Revival in SN DB with revival details |
| Patch NPD in VON | Update VON | Call CF: Patch NPD in VON to clear suspension |
| Return result | Return status | Return success with revival confirmation |

*Refer to FD Section 2.6 for TS revival business rules including looping TS scenarios.*

---

## 1.7 Common Functions

### 1.7.1 Update Suspended Notice

![Update Suspended Notice Flow](./OCMS19_Flowchart_fixed.drawio - Tab 3.6.1)

Updates the ocms_suspended_notice table with revival information.

| Step | Definition | Brief Description |
| --- | --- | --- |
| Receive input parameters | Receive sr_no, revival_reason, officer_id, revival_remarks from caller | Get input from caller |
| Build UPDATE query | Build UPDATE query for ocms_suspended_notice table | Prepare SQL statement |
| Execute UPDATE | Execute UPDATE to set date_of_revival, revival_reason, officer_authorising_revival, revival_remarks, upd_date, upd_user_id | Run database update |
| Return result | Return success status with sr_no, or failure status with error details | Return status to caller

### 1.7.2 Retry with Exponential Backoff

![Retry Flow](./OCMS19_Flowchart_fixed.drawio - Tab 3.6.2)

Implements retry logic for external API calls and database operations.

| Parameter | Value | Description |
| --- | --- | --- |
| maxRetries | 3 | Maximum retry attempts |
| initialDelay | 1000ms | First retry delay |
| maxDelay | 30000ms | Maximum retry delay |
| backoffStrategy | Exponential | With 0-15% jitter |

---

### 1.7.3 Patch Next Processing Date (NPD) in Valid Offence Notice (VON)

![Patch NPD Flow](./OCMS19_Flowchart_fixed.drawio - Tab 3.6.3)

Updates the ocms_valid_offence_notice table after revival.

| Step | Definition | Brief Description |
| --- | --- | --- |
| Receive input | Receive notice_no and officer_id from caller | Get input from caller |
| Calculate new NPD | Calculate next_processing_date = NOW() + 2 days | Compute new date |
| Build UPDATE query | Build UPDATE query for ocms_valid_offence_notice table | Prepare SQL statement |
| Execute UPDATE | Clear suspension_type, epr_reason_of_suspension, crs_reason_of_suspension, due_date_of_revival to NULL; Set next_processing_date, upd_date, upd_user_id | Run database update |
| Return result | Return success status to caller; Log warning if update fails (non-blocking) | Return status to caller

**Note:** After revival, eNA (eNotice Advice) and eReminder SMS/email notifications are NOT sent for the Last Processing Stage. Notifications resume when notices move to the next processing stage.

### 1.7.4 Manage Failed Follow-up

![Manage Failed Follow-up](./OCMS19_Flowchart_fixed.drawio - Tab 3.6.4)

Handles failed follow-up actions during revival processing.

| Step | Definition | Brief Description |
| --- | --- | --- |
| Receive failed follow-up | Entry point | Common function receives failed follow-up action |
| Log failure details | Record error | Log failure details with notice_no, action_type, error_code |
| Determine retry eligibility | Check retry | Check if action is eligible for retry based on error type |
| Schedule retry | Queue retry | If eligible, schedule retry with exponential backoff |
| Mark as failed | Record failure | If not eligible or max retries exceeded, mark as permanently failed |
| Return status | Return result | Return follow-up handling status to caller |

---

## 1.8 Data Mapping

This data mapping documents the UPDATE operation performed on database tables when reviving a suspension. The ocms_suspended_notice table is updated first with revival fields, then ocms_valid_offence_notice is patched to clear suspension fields.


| Zone | Database Table | Field Name | Update Value |
| --- | --- | --- | --- |
| Intranet | ocms_suspended_notice | date_of_revival | Current timestamp |
| Intranet | ocms_suspended_notice | revival_reason | Revival reason code |
| Intranet | ocms_suspended_notice | officer_authorising_revival | Officer ID |
| Intranet | ocms_suspended_notice | revival_remarks | Remarks (if provided) |
| Intranet | ocms_valid_offence_notice | suspension_type | NULL |
| Intranet | ocms_valid_offence_notice | epr_reason_of_suspension | NULL |
| Intranet | ocms_valid_offence_notice | epr_date_of_suspension | NULL |
| Intranet | ocms_valid_offence_notice | crs_reason_of_suspension | NULL |
| Intranet | ocms_valid_offence_notice | crs_date_of_suspension | NULL |
| Intranet | ocms_valid_offence_notice | due_date_of_revival | NULL |
| Intranet | ocms_valid_offence_notice | next_processing_date | Current date + 2 days |
| Intranet | ocms_valid_offence_notice | upd_user_id | ocmsiz_app_conn |
| Intranet | ocms_valid_offence_notice | upd_date | Current timestamp |
| Internet | eocms_valid_offence_notice | suspension_type | NULL |
| Internet | eocms_valid_offence_notice | upd_user_id | ocmsez_app_conn |
| Internet | eocms_valid_offence_notice | upd_date | Current timestamp |

**Notes:**
- Audit user field `ocmsiz_app_conn` is used for intranet zone
- Audit user field `ocmsez_app_conn` is used for internet zone
- Update order: ocms_suspended_notice updated first with revival fields, then ocms_valid_offence_notice is patched to clear suspension fields

---

## 1.9 Success Outcome

- Revival is applied successfully to the Notice
- Suspended notice records are updated with revival information
- Valid Offence Notice (VON) records are updated with cleared suspension fields
- Next processing date is set to current date + 2 days
- Follow-up actions (if any) are triggered correctly
- The workflow reaches the End state without triggering any error-handling paths

---

## 1.10 Error Handling

*Refer to FD Section 2.9 for complete error codes and scenarios.*

---

# Section 2 – OCMS Staff Portal Manual Revival

## 2.1 Use Case

- The manual Revive Suspension function in the OCMS Staff Portal allows authorised OICs to lift suspensions on Notices.

- Users can search for the notices using the Staff Portal's Search Notice function by:
  - Entering search parameters to retrieve one or more Notices
  - Uploading a spreadsheet containing a list of notice numbers to retrieve the Notices

- From the search results, Users can apply revival TS/PS using one of two ways:
  - Select one or more Notices in the Search Result interface and trigger the revive TS/PS operation from the drop-down list to initiate a batch revival
  - Click on a Notice to view the details and apply the revive TS/PS from the View Notice Details interface

- OCMS OICs can revive multiple TS or multiple Notices simultaneously.

- For manual revivals, OCMS OICs can only revive 1 PS for 1 Notice at a time.

- When the revive TS/PS is initiated from the Staff Portal, the Staff Portal validates the revival suspension eligibility of each Notice before redirecting Users to the Revive Suspension Detail Page to:
  - Select the revive TS/PS Suspension Code
  - Input Revival Suspension Remarks, if any

- After the Revive Suspension Details are submitted, OCMS sends the revival suspension details to the OCMS backend for processing.

- If revive TS/PS is successful, Users will be presented with a success message on screen; an error message will be displayed if the revival of the suspension is not successful.

## 2.2 Process Flow

![OCMS Manual Revival Flow](./OCMS19_Flowchart_fixed.drawio - Tab 3.7)

| Step | Definition | Brief Description |
| --- | --- | --- |
| OIC access OCMS Staff Portal | OIC logs in to OCMS Staff Portal | Login to portal |
| Browse and select Notice(s) | OIC searches and selects one or more notices with suspension | Select notices |
| Click "Apply Operation" dropdown | OIC clicks dropdown to see available operations | Open dropdown menu |
| Check OIC permission | Portal checks if OIC has Revival permission; if no, dropdown does not show "Apply Revival" | Verify permission |
| Select "Apply Revival" option | OIC selects Apply Revival from dropdown | Choose revival action |
| Frontend validation | Portal validates eligibility (TS-HST via HST module only, max 1 PS per request, reason required, remarks max 200 chars) | Validate input |
| Enter revival reason and remarks | OIC enters revival reason and optional remarks | Fill in details |
| Call OCMS Backend API | Portal sends POST request to /ocms/v1/staff-revive-suspension API | Submit to backend |
| Backend validation | Backend validates JWT, API Key, permission, request fields, notice exists & suspended, suspension type match | Server-side validation |
| Process revival | Backend routes to MAIN FLOW PS or MAIN FLOW TS based on suspension type | Execute revival |
| Receive response from backend | Portal receives response with totalProcessed, successCount, errorCount, results | Get API response |
| Display result | Portal displays success or error message to OIC | Show result to user

---

## 2.3 API Specification

### Endpoint: Revive Suspension (OCMS Staff Portal)

**URL:** `POST /ocms/v1/staff-revive-suspension`

**Authentication:** JWT Token (OCMS Staff Portal)

**Request Body:**
```json
{
  "noticeNo": ["500400058A"],
  "suspensionType": "TS",
  "revivalReason": "OTH",
  "revivalRemarks": "Issue Resolved",
  "officerAuthorisingRevival": "JOHNLEE"
}
```

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| noticeNo | String[] | Yes | Array of notice numbers |
| suspensionType | String | Yes | Suspension type (TS/PS) |
| revivalReason | String | Yes | Revival reason code |
| revivalRemarks | String | No | Revival remarks (max 200 chars) |
| officerAuthorisingRevival | String | Yes | Officer ID |

**Success Response:**
```json
{
  "totalProcessed": 1,
  "successCount": 1,
  "errorCount": 0,
  "results": [
    {
      "noticeNo": "500400058A",
      "appCode": "OCMS-2000",
      "message": "Revival Success"
    }
  ]
}
```

**Error Response:**
```json
{
  "totalProcessed": 1,
  "successCount": 0,
  "errorCount": 1,
  "results": [
    {
      "noticeNo": "500400058A",
      "appCode": "OCMS-4001",
      "message": "Invalid Notice Number"
    }
  ]
}
```

*Refer to FD Section 3.3 for complete API specifications and error codes.*

---

## 2.4 Data Mapping

This data mapping documents the UPDATE operation performed when OIC revives a suspension via OCMS Staff Portal. It updates ocms_suspended_notice with revival information and patches ocms_valid_offence_notice to clear suspension fields.

| Zone | Database Table | Field Name | Update Value |
| --- | --- | --- | --- |
| Intranet | ocms_suspended_notice | date_of_revival | Current timestamp |
| Intranet | ocms_suspended_notice | revival_reason | Revival reason code |
| Intranet | ocms_suspended_notice | officer_authorising_revival | Officer ID |
| Intranet | ocms_suspended_notice | revival_remarks | Remarks (if provided) |
| Intranet | ocms_valid_offence_notice | suspension_type | NULL |
| Intranet | ocms_valid_offence_notice | epr_reason_of_suspension | NULL |
| Intranet | ocms_valid_offence_notice | due_date_of_revival | NULL |
| Intranet | ocms_valid_offence_notice | next_processing_date | Current date + 2 days |
| Internet | eocms_valid_offence_notice | suspension_type | NULL |
| Internet | eocms_valid_offence_notice | upd_user_id | ocmsez_app_conn |
| Internet | eocms_valid_offence_notice | upd_date | Current timestamp |

---

## 2.5 Success Outcome

- Revival is applied successfully to all selected Notices
- Suspended notice records are updated with revival information
- Valid Offence Notice (VON) records are updated with cleared suspension fields
- Next processing date is set to current date + 2 days
- Portal displays success message with revival count
- The workflow reaches the End state without triggering any error-handling paths

---

## 2.6 Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Invalid JWT Token | JWT token is missing or invalid | Return OCMS-4000 error, authentication fails |
| Unauthorized User | User not authorized to revive suspensions | Return OCMS-4007 error, block action |
| Notice Not Found | Notice number does not exist in database | Return OCMS-4001 error, skip notice |
| Notice Not Suspended | Notice does not have active suspension | Return OCMS-4002 error, skip notice |
| Court Processing | Notice is under court processing | Return OCMS-4007 error, revival not allowed |
| Database Error | Unable to update database records | Log error, return OCMS-5000 error |

*Refer to FD Section 3.5 for complete error handling scenarios.*

---

# Section 3 – Auto Revive in OCMS

## 3.1 Use Case

- The OCMS Backend will only initiate Suspension Revival automatically for selected use cases.

- When reviving a Notice, the OCMS backend will use a Revival Reason code to indicate the reason of suspension.

## 3.2 Auto Revive TS-HST/CLV

![Auto Revive TS-HST/CLV Flow](./OCMS19_Flowchart_fixed.drawio - Tab 3.5.3)

| Step | Definition | Brief Description |
| --- | --- | --- |
| Cron job triggers | Auto revival batch job runs at scheduled time (ShedLock: suspension_auto_revival) | Start batch job |
| Query eligible notices | Query ocms_suspended_notice where suspension_type = 'TS' AND date_of_revival IS NULL AND due_date_of_revival <= TODAY | Find notices to revive |
| Process each notice | Loop through each eligible notice | Iterate notices |
| Update suspended notice | Call Common Function: Update Revival in SN DB with revival_reason = 'SPO' | Record revival in SN |
| Check for looping TS | For TS-CLV: Check if VIP at RR3/DR3 is unpaid | Determine if re-apply |
| Apply looping TS | If VIP unpaid, re-apply TS-CLV with new due date; If VIP paid (FP status), stop looping | Re-apply or stop |
| Patch NPD in VON | Call Common Function: Patch NPD in VON to clear suspension and set next_processing_date | Update VON record |
| Return result | Set result with appCode and message | Return job status

---

## 3.3 Auto Revive TS-PDP

![Auto Revive TS-PDP Flow](./OCMS19_Flowchart_fixed.drawio - Tab 3.5.7)

| Step | Definition | Brief Description |
| --- | --- | --- |
| Cron job triggers | Start batch | Auto revival batch job runs at scheduled time (ShedLock: suspension_auto_revival) |
| Query TS-PDP notices | Query SN | Query ocms_suspended_notice where suspension_type = 'TS' AND epr_reason_of_suspension = 'PDP' AND due_date <= TODAY |
| Process each notice | Loop notices | Loop through each eligible TS-PDP notice |
| Update suspended notice | Update SN | Call CF: Update Revival in SN DB with revival_reason = 'SPO' |
| Check PDP status | Check pending | Check if pending dispute payment still exists |
| Re-apply TS-PDP | Loop suspension | If PDP still pending, re-apply TS-PDP with new due date |
| Clear suspension | Stop loop | If PDP resolved, clear suspension from VON |
| Patch NPD in VON | Update VON | Call CF: Patch NPD in VON to set next_processing_date |
| Return result | Return status | Return batch job completion status |

*Refer to FD Section 4.3 for TS-PDP auto revival business rules.*

---

## 3.4 Data Mapping

This data mapping documents the UPDATE operation performed by the auto revival batch job. It updates ocms_suspended_notice with revival details and patches ocms_valid_offence_notice to clear or re-apply suspension.

| Zone | Database Table | Field Name | Update Value |
| --- | --- | --- | --- |
| Intranet | ocms_suspended_notice | date_of_revival | Current timestamp |
| Intranet | ocms_suspended_notice | revival_reason | SPO |
| Intranet | ocms_suspended_notice | officer_authorising_revival | ocmsiz_app_conn |
| Intranet | ocms_valid_offence_notice | suspension_type | NULL (or next TS code) |
| Intranet | ocms_valid_offence_notice | epr_reason_of_suspension | NULL (or next TS code) |
| Intranet | ocms_valid_offence_notice | next_processing_date | Current date + 2 days |
| Internet | eocms_valid_offence_notice | suspension_type | NULL (or next TS code) |
| Internet | eocms_valid_offence_notice | upd_user_id | ocmsez_app_conn |
| Internet | eocms_valid_offence_notice | upd_date | Current timestamp |

---

## 3.5 Success Outcome

- Auto revival job completes successfully
- All eligible notices are processed
- Revival records are updated with reason code "SPO" (Suspension Period Over)
- Officer ID is set to "ocmsiz_app_conn" for system-initiated revivals
- Follow-up actions (looping TS) are triggered where applicable
- Job execution logged to batch job table
- The workflow reaches the End state without triggering any error-handling paths

---

## 3.6 Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Query Failure | Unable to query eligible notices | Log error, retry with exponential backoff |
| Database Connection Lost | Database connection fails mid-process | Rollback transaction, log error, retry |
| Failed to Update VON | Unable to update Valid Offence Notice (VON) | Retry with exponential backoff, log if all retries fail |
| Batch Job Timeout | Batch job exceeds maximum execution time | Log incomplete notices, resume in next run |

---

# Section 4 – Manual Revival initiated by PLUS

## 4.1 Use Case

- PLMs can initiate Revival via an API provided by OCMS.

- PLMs may initiate a revival under the following circumstances:
  - PLM accepts the appeal and grants a waiver for a paid Notice, triggering revival of the PS-FP suspension (i.e., replacing PS-FP with PS-APP)
  - PLM rejects the appeal and lifts the TS, allowing Notice processing to continue

- During Revival, PLUS will initiate a Revival on the latest Suspension.

## 4.2 Process Flow

![PLUS Manual Revival Flow](./OCMS19_Flowchart_fixed.drawio - Tab 3.8)

| Step | Definition | Brief Description |
| --- | --- | --- |
| PLM access PLUS Staff Portal | PLM logs in to PLUS Staff Portal | Login to portal |
| Browse and select Notice(s) | PLM searches and selects one or more notices with suspension | Select notices |
| Click "Apply Operation" dropdown | PLM clicks dropdown to see available operations | Open dropdown menu |
| Check PLM permission | Portal checks if PLM has Revival permission; if no, dropdown does not show "Apply Revival" | Verify permission |
| Select "Apply Revival" option | PLM selects Apply Revival from dropdown | Choose revival action |
| Frontend validation | Portal validates eligibility (similar to OCMS validation) | Validate input |
| Enter revival reason and remarks | PLM enters revival reason (CSR or OTH only) and optional remarks | Fill in details |
| Call OCMS Backend API via PLUS Backend | PLUS Portal calls PLUS Backend, which calls POST /ocms/v1/plus-revive-suspension | Submit to backend |
| Backend validation | OCMS Backend validates JWT, API Key, permission, request fields, notice exists & suspended | Server-side validation |
| Process revival | Backend routes to MAIN FLOW PS (CSR/OTH codes only) or MAIN FLOW TS based on suspension type | Execute revival |
| Receive response from backend | Portal receives response with totalProcessed, successCount, errorCount, results | Get API response |
| Display result | Portal displays success or error message to PLM | Show result to user

---

## 4.3 API Specification

### Endpoint: Revive Suspension (PLUS)

**URL:** `POST /ocms/v1/plus-revive-suspension`

**Authentication:** JWT Token + API Key (PLUS Backend)

**Request Body:**
```json
{
  "noticeNo": ["500400058A"],
  "suspensionType": "TS",
  "revivalReason": "CSR",
  "revivalRemarks": "Issue Resolved",
  "officerAuthorisingRevival": "PLU_1"
}
```

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| noticeNo | String[] | Yes | Array of notice numbers |
| suspensionType | String | Yes | Suspension type (TS/PS) |
| revivalReason | String | Yes | Revival reason code (CSR or OTH only for PLUS) |
| revivalRemarks | String | No | Revival remarks (max 200 chars) |
| officerAuthorisingRevival | String | Yes | PLM Officer ID |

**Supported Revival Reason Codes (PLUS):**
| Code | Description |
| --- | --- |
| CSR | Change suspension reason |
| OTH | Others |

**Success Response:**
```json
{
  "totalProcessed": 1,
  "successCount": 1,
  "errorCount": 0,
  "results": [
    {
      "noticeNo": "500400058A",
      "appCode": "OCMS-2000",
      "message": "Revival Success"
    }
  ]
}
```

**Error Response:**
```json
{
  "totalProcessed": 1,
  "successCount": 0,
  "errorCount": 1,
  "results": [
    {
      "noticeNo": "500400058A",
      "appCode": "OCMS-4001",
      "message": "Invalid Notice Number"
    }
  ]
}
```

*Refer to FD Section 5.3 for complete API specifications.*

---

## 4.4 Data Mapping

This data mapping documents the UPDATE operation performed when PLM revives a suspension via PLUS Staff Portal. It updates ocms_suspended_notice with revival information and patches ocms_valid_offence_notice to clear suspension fields.

| Zone | Database Table | Field Name | Update Value |
| --- | --- | --- | --- |
| Intranet | ocms_suspended_notice | date_of_revival | Current timestamp |
| Intranet | ocms_suspended_notice | revival_reason | Revival reason code |
| Intranet | ocms_suspended_notice | officer_authorising_revival | PLM Officer ID |
| Intranet | ocms_suspended_notice | revival_remarks | Remarks (if provided) |
| Intranet | ocms_valid_offence_notice | suspension_type | NULL |
| Intranet | ocms_valid_offence_notice | epr_reason_of_suspension | NULL |
| Intranet | ocms_valid_offence_notice | next_processing_date | Current date + 2 days |
| Internet | eocms_valid_offence_notice | suspension_type | NULL |
| Internet | eocms_valid_offence_notice | upd_user_id | ocmsez_app_conn |
| Internet | eocms_valid_offence_notice | upd_date | Current timestamp |

---

## 4.5 Success Outcome

- Revival request from PLUS is processed successfully
- All validation rules are satisfied
- Suspended notice records are updated with revival information
- Valid Offence Notice (VON) records are updated with cleared suspension fields
- Response is returned to PLUS Backend with appCode "OCMS-2000"
- The workflow reaches the End state without triggering any error-handling paths

---

## 4.6 Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Invalid API Key | API key is missing or invalid | Return OCMS-4000 error, authentication fails |
| Invalid JWT Token | JWT token is missing or invalid | Return OCMS-4000 error, authentication fails |
| Unauthorized User | User not authorized to revive suspensions | Return OCMS-4007 error, block action |
| Notice Not Found | Notice number does not exist | Return OCMS-4001 error, request rejected |
| Notice Not Suspended | Notice does not have active suspension | Return OCMS-4002 error, request rejected |
| Invalid Revival Code | PLUS uses code other than CSR/OTH | Return OCMS-4007 error, code not allowed |
| Database Error | Unable to update database records | Log error, return OCMS-5000 error |

**Note:** PLUS CAN revive PS (per FD Section 2.6.1), but limited to revival codes CSR and OTH only (per FD Section 5.3).

*Refer to FD Section 5.5 for complete error handling scenarios.*

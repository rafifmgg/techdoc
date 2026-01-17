# OCMS 19 – Revive Suspensions

**Technical Document**

**Prepared by**

MGG SOFTWARE

---

## Version History

| Version | Updated By | Date | Changes |
| --- | --- | --- | --- |
| v1.0 | Claude Code | 08/01/2026 | Document Initiation |

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
| 1.5.2 | Sub-Flow: Revive PS-FP/PRA | - |
| 1.5.3 | Sub-Flow: Revive PS-FOR/MID | - |
| 1.5.4 | Sub-Flow: Revive Other PS | - |
| 1.5.5 | CF: Update Revival in Suspended Notice DB | - |
| 1.5.6 | CF: Retry Saving to DB | - |
| 1.5.7 | CF: Patch Next Processing Date in VON | - |
| 1.5.8 | CF: Manage Failed Follow-up | - |
| 1.6 | Reviving TS Suspensions | - |
| 1.6.1 | Main Flow - Reviving TS | - |
| 1.6.2 | Sub-Flow: Revive TS-HST/CLV | - |
| 1.6.3 | Micro Flow: Auto Revive TS-HST/CLV | - |
| 1.6.4 | Micro Flow: Manual Revive TS-HST/CLV | - |
| 1.6.5 | Sub-Flow: Revive TS-RED | - |
| 1.6.6 | Sub-Flow: Revive TS-PDP | - |
| 1.6.7 | Micro Flow: Auto Revive TS-PDP | - |
| 1.6.8 | Micro Flow: Manual Revive TS-PDP | - |
| 1.6.9 | Sub-Flow: Revive TS-ROV | - |
| 1.6.10 | Sub-Flow: Revive TS-NRO | - |
| 1.6.11 | Sub-Flow: Revive Other TS | - |
| 1.6.12 | CF: Update Revival in Suspended Notice DB | - |
| 1.6.13 | CF: Retry Saving to DB | - |
| 1.6.14 | CF: Patch Next Processing Date in VON | - |
| 1.6.15 | CF: Manage Failed Follow-up | - |
| 1.7 | Data Mapping | - |
| 1.8 | Success Outcome | - |
| 1.9 | Error Handling | - |
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

<!-- Insert flow diagram: Tab 3.1 - High-Level Overview -->
![High Level Flow](./OCMS19_Flowchart.drawio - Tab 3.1)

NOTE: Due to page size limit, the full-sized image is appended.

### Entry Points

| Entry Point | Source | Description |
| --- | --- | --- |
| Manual Revival (OCMS) | OCMS Staff Portal | OIC manually triggers revival via Apply Operation dropdown |
| Manual Revival (PLUS) | PLUS Staff Portal | PLM manually triggers revival for appeal-related cases |
| Auto Revival | OCMS Backend Cron | System automatically revives TS when due date is reached |

### Flow Description

| Step | Action | Description |
| --- | --- | --- |
| 1 | Receive revival request | System receives revival request from Staff Portal or Cron job |
| 2 | Staff Portal validation | Portal performs initial check to determine if revival is allowed based on user permission and notice eligibility |
| 3 | Submit request via API | Portal calls backend API to process revival request with notice numbers, revival reason, and officer details |
| 4 | Backend validation | Backend validates JWT token, API key, and request payload before processing |
| 5 | Process revival | Backend updates suspended notice records and clears suspension from Valid Offence Notice (VON) |
| 6 | Follow-up action check | Backend checks if additional action is required after revival (e.g., re-apply looping TS, mark refund) |
| 7 | Return response | Backend returns consolidated results with success/error status for each notice |

*Refer to FD Section 2.2 for detailed high-level processing flow description.*

---

## 1.3 UI-side Validation

<!-- Insert flow diagram: Tab 3.2 - UI-side Revive Validation -->
![UI-side Validation Flow](./OCMS19_Flowchart.drawio - Tab 3.2)

NOTE: Due to page size limit, the full-sized image is appended.

*Refer to FD Section 2.3 for detailed validation rules.*

---

## 1.4 Backend Validation

<!-- Insert flow diagram: Tab 3.3 - Backend-side Revive Validation -->
![Backend Validation Flow](./OCMS19_Flowchart.drawio - Tab 3.3)

NOTE: Due to page size limit, the full-sized image is appended.

*Refer to FD Section 2.4 for detailed validation rules and error codes.*

---

## 1.5 Reviving PS Suspensions

### 1.5.1 Main Flow - Reviving PS

<!-- Insert flow diagram: Tab 3.4.1 - Main Flow Reviving PS -->
![Revive PS Main Flow](./OCMS19_Flowchart.drawio - Tab 3.4.1)

Main processing flow for reviving Permanent Suspension (PS) types. Routes to appropriate sub-flow based on PS suspension code.

*Refer to FD Section 2.5 for PS revival business rules.*

---

### 1.5.2 Sub-Flow: Revive PS-FP/PRA

<!-- Insert flow diagram: Tab 3.4.2 - Revive PS-FP/PRA -->
![Revive PS-FP/PRA Flow](./OCMS19_Flowchart.drawio - Tab 3.4.2)

Handles revival of:
- **PS-FP** (Full Payment): Triggers refund process when revived
- **PS-PRA** (Payment Received Awaiting): Awaiting payment confirmation

| Step | Action | Description |
| --- | --- | --- |
| 1 | Validate PS type | Confirm suspension is PS-FP or PS-PRA |
| 2 | Update suspended notice | Mark suspension as revived |
| 3 | Clear VON suspension | Clear suspension fields in ocms_valid_offence_notice |
| 4 | Trigger refund (PS-FP only) | Record refund date for MVP |

---

### 1.5.3 Sub-Flow: Revive PS-FOR/MID

<!-- Insert flow diagram: Tab 3.4.3 - Revive PS-FOR/MID -->
![Revive PS-FOR/MID Flow](./OCMS19_Flowchart.drawio - Tab 3.4.3)

Handles revival of:
- **PS-FOR** (Foreigner): Notice suspended due to foreign vehicle
- **PS-MID** (Missing ID): Notice suspended due to missing ID details

| Step | Action | Description |
| --- | --- | --- |
| 1 | Validate PS type | Confirm suspension is PS-FOR or PS-MID |
| 2 | Update suspended notice | Mark suspension as revived |
| 3 | Clear VON suspension | Clear suspension fields in ocms_valid_offence_notice |
| 4 | Resume processing | Notice resumes from next_processing_date |

---

### 1.5.4 Sub-Flow: Revive Other PS

<!-- Insert flow diagram: Tab 3.4.4 - Revive Other PS -->
![Revive Other PS Flow](./OCMS19_Flowchart.drawio - Tab 3.4.4)

Handles revival of other PS types including:
- **PS-WDH** (Withdrawal)
- **PS-CAN** (Cancelled)
- **PS-APP** (Appeal)
- **PS-INS** (Insufficient)
- Other permanent suspension codes

| Step | Action | Description |
| --- | --- | --- |
| 1 | Validate PS type | Confirm suspension is other PS type |
| 2 | Update suspended notice | Mark suspension as revived |
| 3 | Clear VON suspension | Clear suspension fields in ocms_valid_offence_notice |
| 4 | Resume processing | Notice resumes from next_processing_date |

---

### 1.5.5 Common Function: Update Revival in Suspended Notice DB

<!-- Insert flow diagram: Tab 3.6.1 - CF Update Suspended Notice -->
![CF Update Suspended Notice](./OCMS19_Flowchart.drawio - Tab 3.6.1)

Updates the `ocms_suspended_notice` table with revival information for PS suspensions.

| Step | Action | Description |
| --- | --- | --- |
| 1 | Query suspended notice | SELECT from ocms_suspended_notice WHERE notice_no = ? AND date_of_revival IS NULL |
| 2 | Update revival fields | SET date_of_revival, revival_reason, officer_authorising_revival, revival_remarks |
| 3 | Update audit fields | SET upd_date = NOW(), upd_user_id = ocmsiz_app_conn |
| 4 | Commit transaction | Persist changes to database |

---

### 1.5.6 Common Function: Retry Saving to DB

<!-- Insert flow diagram: Tab 3.6.2 - CF Retry Saving -->
![CF Retry Saving to DB](./OCMS19_Flowchart.drawio - Tab 3.6.2)

Implements retry logic with exponential backoff for database operations during PS revival.

| Parameter | Value | Description |
| --- | --- | --- |
| Max Retries | 3 | Maximum retry attempts |
| Initial Delay | 1000ms | First retry delay |
| Max Delay | 30000ms | Maximum retry delay |
| Backoff Strategy | Exponential | Delay doubles each retry with 0-15% jitter |

**Error Handling:**
- Retryable errors: Connection timeout, deadlock, temporary unavailability
- Non-retryable errors: Constraint violation, data validation errors
- After max retries: Escalate to failed follow-up handler

---

### 1.5.7 Common Function: Patch Next Processing Date in VON

<!-- Insert flow diagram: Tab 3.6.3 - CF Patch NPD -->
![CF Patch NPD in VON](./OCMS19_Flowchart.drawio - Tab 3.6.3)

Updates the `next_processing_date` in `ocms_valid_offence_notice` table after PS revival to resume notice processing workflow.

| Step | Action | Description |
| --- | --- | --- |
| 1 | Calculate NPD | Determine next processing date based on current stage and suspension duration |
| 2 | Update VON | UPDATE ocms_valid_offence_notice SET next_processing_date = ? WHERE notice_no = ? |
| 3 | Clear suspension fields | SET suspension_type = NULL, epr_reason_of_suspension = NULL |
| 4 | Update audit fields | SET upd_date = NOW(), upd_user_id = ocmsiz_app_conn |

---

### 1.5.8 Common Function: Manage Failed Follow-up

<!-- Insert flow diagram: Tab 3.6.4 - CF Manage Failed Follow-up -->
![CF Manage Failed Follow-up](./OCMS19_Flowchart.drawio - Tab 3.6.4)

Handles scenarios where PS revival follow-up actions fail (e.g., external API calls, database updates). Prevents notice from progressing without proper follow-up completion.

**Follow-up Actions:**
- PS-FP: Mark refund date for MVP
- Update VON suspension fields
- Trigger downstream notifications

**Failure Handling:**
1. Log failure with error details
2. Mark notice for manual intervention
3. Send alert to support team
4. Prevent notice from moving to next stage

**Important:** If this function also fails, OCMS will stop retrying to prevent endless loop.

---

## 1.6 Reviving TS Suspensions

### 1.6.1 Main Flow - Reviving TS

<!-- Insert flow diagram: Tab 3.5.1 - Main Flow Reviving TS -->
![Revive TS Main Flow](./OCMS19_Flowchart.drawio - Tab 3.5.1)

Main processing flow for reviving Temporary Suspension (TS) types. Routes to appropriate sub-flow based on TS suspension code.

*Refer to FD Section 2.6 for TS revival business rules including looping TS scenarios.*

---

### 1.6.2 Sub-Flow: Revive TS-HST/CLV

<!-- Insert flow diagram: Tab 3.5.2 - Revive TS-HST/CLV -->
![Revive TS-HST/CLV Flow](./OCMS19_Flowchart.drawio - Tab 3.5.2)

Handles revival of looping TS types:
- **TS-HST** (Hirer/Driver Particulars): Requires hirer/driver submission approval
- **TS-CLV** (Compounded Late/Very Late): Requires payment clearance

**Key Behavior:** After revival, checks if other active TS exists. If yes, applies the TS with latest due_date_of_revival to VON. If no, clears all suspension fields.

---

### 1.6.3 Micro Flow: Auto Revive TS-HST/CLV

<!-- Insert flow diagram: Tab 3.5.3 - Auto Revive HST/CLV -->
![Auto Revive TS-HST/CLV](./OCMS19_Flowchart.drawio - Tab 3.5.3)

Automated revival triggered by OCMS backend cron job when:
- TS-HST: Hirer/driver particulars submission approved
- TS-CLV: Payment cleared in system

---

### 1.6.4 Micro Flow: Manual Revive TS-HST/CLV

<!-- Insert flow diagram: Tab 3.5.4 - Manual Revive HST/CLV -->
![Manual Revive TS-HST/CLV](./OCMS19_Flowchart.drawio - Tab 3.5.4)

Manual revival initiated by:
- **TS-HST:** OCMS Staff via HST module only
- **TS-CLV:** OCMS/PLUS Staff via manual revival interface

---

### 1.6.5 Sub-Flow: Revive TS-RED

<!-- Insert flow diagram: Tab 3.5.5 - Revive TS-RED -->
![Revive TS-RED Flow](./OCMS19_Flowchart.drawio - Tab 3.5.5)

Handles revival of:
- **TS-RED** (Redemption): Notice under redemption process

**Special Rule:** Can only be revived by PLUS OICs via PLUS Portal.

---

### 1.6.6 Sub-Flow: Revive TS-PDP

<!-- Insert flow diagram: Tab 3.5.6 - Revive TS-PDP -->
![Revive TS-PDP Flow](./OCMS19_Flowchart.drawio - Tab 3.5.6)

Handles revival of:
- **TS-PDP** (Pending Driver/Hirer Particulars): Awaiting particulars submission

**Key Behavior:** Looping TS type. After revival, checks if hirer/driver particulars submission has been approved/rejected to determine if TS-PDP needs to be re-applied.

---

### 1.6.7 Micro Flow: Auto Revive TS-PDP

<!-- Insert flow diagram: Tab 3.5.7 - Auto Revive PDP -->
![Auto Revive TS-PDP](./OCMS19_Flowchart.drawio - Tab 3.5.7)

Automated revival triggered when particulars submission is:
- **Approved:** TS-PDP revived, notice resumes processing
- **Rejected:** TS-PDP revived, but may be re-applied based on BR-020

---

### 1.6.8 Micro Flow: Manual Revive TS-PDP

<!-- Insert flow diagram: Tab 3.5.8 - Manual Revive PDP -->
![Manual Revive TS-PDP](./OCMS19_Flowchart.drawio - Tab 3.5.8)

Manual revival by OCMS/PLUS Staff with reasons:
- **DPU** (Driver's Particulars Updated)
- **OPU** (Owner's Particulars Updated)

---

### 1.6.9 Sub-Flow: Revive TS-ROV

<!-- Insert flow diagram: Tab 3.5.9 - Revive TS-ROV -->
![Revive TS-ROV Flow](./OCMS19_Flowchart.drawio - Tab 3.5.9)

Handles revival of:
- **TS-ROV** (Reduced on Vetting): Notice amount reduced during vetting

| Step | Action | Description |
| --- | --- | --- |
| 1 | Validate TS type | Confirm suspension is TS-ROV |
| 2 | Update suspended notice | Mark suspension as revived |
| 3 | Update VON TS looping | Check other active TS, apply latest or clear |
| 4 | Resume processing | Notice resumes from next_processing_date |

---

### 1.6.10 Sub-Flow: Revive TS-NRO

<!-- Insert flow diagram: Tab 3.5.10 - Revive TS-NRO -->
![Revive TS-NRO Flow](./OCMS19_Flowchart.drawio - Tab 3.5.10)

Handles revival of:
- **TS-NRO** (Notice Reduced to Order): Notice converted to Court Order

| Step | Action | Description |
| --- | --- | --- |
| 1 | Validate TS type | Confirm suspension is TS-NRO |
| 2 | Update suspended notice | Mark suspension as revived |
| 3 | Update VON TS looping | Check other active TS, apply latest or clear |
| 4 | Resume processing | Notice resumes from next_processing_date |

---

### 1.6.11 Sub-Flow: Revive Other TS

<!-- Insert flow diagram: Tab 3.5.11 - Revive Other TS -->
![Revive Other TS Flow](./OCMS19_Flowchart.drawio - Tab 3.5.11)

Handles revival of other TS types including:
- **TS-SYS** (System): System-triggered suspension
- **TS-APP** (Appeal): Notice under appeal
- Other temporary suspension codes

| Step | Action | Description |
| --- | --- | --- |
| 1 | Validate TS type | Confirm suspension is other TS type |
| 2 | Update suspended notice | Mark suspension as revived |
| 3 | Update VON TS looping | Check other active TS, apply latest or clear |
| 4 | Resume processing | Notice resumes from next_processing_date |

---

### 1.6.12 Common Function: Update Revival in Suspended Notice DB

<!-- Insert flow diagram: Tab 3.6.1 - CF Update Suspended Notice -->
![CF Update Suspended Notice](./OCMS19_Flowchart.drawio - Tab 3.6.1)

Updates the `ocms_suspended_notice` table with revival information for TS suspensions.

| Step | Action | Description |
| --- | --- | --- |
| 1 | Query suspended notice | SELECT from ocms_suspended_notice WHERE notice_no = ? AND date_of_revival IS NULL |
| 2 | Update revival fields | SET date_of_revival, revival_reason, officer_authorising_revival, revival_remarks |
| 3 | Update audit fields | SET upd_date = NOW(), upd_user_id = ocmsiz_app_conn |
| 4 | Commit transaction | Persist changes to database |

---

### 1.6.13 Common Function: Retry Saving to DB

<!-- Insert flow diagram: Tab 3.6.2 - CF Retry Saving -->
![CF Retry Saving to DB](./OCMS19_Flowchart.drawio - Tab 3.6.2)

Implements retry logic with exponential backoff for database operations during TS revival.

| Parameter | Value | Description |
| --- | --- | --- |
| Max Retries | 3 | Maximum retry attempts |
| Initial Delay | 1000ms | First retry delay |
| Max Delay | 30000ms | Maximum retry delay |
| Backoff Strategy | Exponential | Delay doubles each retry with 0-15% jitter |

**Error Handling:**
- Retryable errors: Connection timeout, deadlock, temporary unavailability
- Non-retryable errors: Constraint violation, data validation errors
- After max retries: Escalate to failed follow-up handler

---

### 1.6.14 Common Function: Patch Next Processing Date in VON

<!-- Insert flow diagram: Tab 3.6.3 - CF Patch NPD -->
![CF Patch NPD in VON](./OCMS19_Flowchart.drawio - Tab 3.6.3)

Updates the `next_processing_date` in `ocms_valid_offence_notice` table after TS revival to resume notice processing workflow.

| Step | Action | Description |
| --- | --- | --- |
| 1 | Calculate NPD | Determine next processing date based on current stage and suspension duration |
| 2 | Update VON | UPDATE ocms_valid_offence_notice SET next_processing_date = ? WHERE notice_no = ? |
| 3 | Apply TS looping | Check for other active TS, apply latest or clear suspension fields |
| 4 | Update audit fields | SET upd_date = NOW(), upd_user_id = ocmsiz_app_conn |

---

### 1.6.15 Common Function: Manage Failed Follow-up

<!-- Insert flow diagram: Tab 3.6.4 - CF Manage Failed Follow-up -->
![CF Manage Failed Follow-up](./OCMS19_Flowchart.drawio - Tab 3.6.4)

Handles scenarios where TS revival follow-up actions fail (e.g., external API calls, database updates, TS looping logic). Prevents notice from progressing without proper follow-up completion.

**Follow-up Actions:**
- TS looping: Apply latest active TS or clear suspension
- Update VON suspension fields
- Trigger downstream notifications

**Failure Handling:**
1. Log failure with error details
2. Mark notice for manual intervention
3. Send alert to support team
4. Prevent notice from moving to next stage

**Important:** If this function also fails, OCMS will stop retrying to prevent endless loop.

---

## 1.7 Data Mapping

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

**Notes:**
- Audit user field `ocmsiz_app_conn` is used for intranet zone
- Audit user field `ocmsez_app_conn` is used for internet zone
- Insert order: Parent table (ocms_valid_offence_notice) updated first, then child table (ocms_suspended_notice)

---

## 1.8 Success Outcome

- Revival is applied successfully to the Notice
- Suspended notice records are updated with revival information
- Valid Offence Notice (VON) records are updated with cleared suspension fields
- Next processing date is set to current date + 2 days
- Follow-up actions (if any) are triggered correctly
- The workflow reaches the End state without triggering any error-handling paths

---

## 1.9 Error Handling

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

<!-- Insert flow diagram: Tab 3.7 - OCMS Manual Revival -->
![OCMS Manual Revival Flow](./OCMS19_Flowchart.drawio - Tab 3.7)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Action | Description |
| --- | --- | --- |
| 1 | Search notices | OIC searches for notices via Search Notice function |
| 2 | Select notices | OIC selects one or more notices from search results |
| 3 | Initiate revival | OIC triggers revive TS/PS from dropdown or notice details page |
| 4 | UI validation | Portal validates eligibility (permission, suspension status, notice stage) |
| 5 | Enter revival details | OIC enters revival reason and optional remarks |
| 6 | Submit to backend | Portal sends POST request to /v1/revive-suspension API |
| 7 | Backend processing | Backend validates and processes revival request |
| 8 | Display result | Portal displays success or error message to user |

---

## 2.3 API Specification

### Endpoint: Revive Suspension (OCMS Staff Portal)

**URL:** `POST /v1/revive-suspension`

**Authentication:** JWT Token (OCMS Staff Portal)

**Request Body:**
```json
{
  "noticeNumbers": ["string"],
  "suspensionType": "TS" | "PS",
  "revivalReason": "string",
  "officerAuthorisingRevival": "string",
  "revivalRemarks": "string"
}
```

**Success Response:**
```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Revival processed successfully",
    "results": [
      {
        "noticeNumber": "string",
        "success": true
      }
    ]
  }
}
```

**Error Response:**
```json
{
  "appCode": "OCMS-4XXX",
  "message": "Error description"
}
```

*Refer to FD Section 3.3 for complete API specifications and error codes.*

---

## 2.4 Data Mapping

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

<!-- Insert flow diagram: Tab 3.8 - Auto Revive TS Cron -->
![Auto Revive TS Flow](./OCMS19_Flowchart.drawio - Tab 3.8)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Action | Description |
| --- | --- | --- |
| 1 | Cron job triggers | Auto revival batch job runs at scheduled time |
| 2 | Query eligible notices | Query ocms_suspended_notice where suspension_type = 'TS' AND date_of_revival IS NULL AND due_date_of_revival <= TODAY |
| 3 | Process each notice | Loop through each eligible notice |
| 4 | Update suspended notice | Set date_of_revival = NOW(), revival_reason = 'SPO', officer_authorising_revival = 'ocmsiz_app_conn' |
| 5 | Check for looping TS | Check if TS-CLV or TS-PDP requires re-application |
| 6 | Apply looping TS | If required, re-apply TS with new due date |
| 7 | Update VON | Clear suspension fields or apply new TS to Valid Offence Notice (VON) |
| 8 | Log batch job | Record batch job execution status |

---

## 3.3 Auto Revive TS-PDP

<!-- Insert flow diagram: Tab 3.9 - Auto Revive TS-PDP -->
![Auto Revive TS-PDP Flow](./OCMS19_Flowchart.drawio - Tab 3.9)

*Refer to FD Section 4.3 for TS-PDP auto revival business rules.*

---

## 3.4 Data Mapping

| Zone | Database Table | Field Name | Update Value |
| --- | --- | --- | --- |
| Intranet | ocms_suspended_notice | date_of_revival | Current timestamp |
| Intranet | ocms_suspended_notice | revival_reason | SPO |
| Intranet | ocms_suspended_notice | officer_authorising_revival | ocmsiz_app_conn |
| Intranet | ocms_valid_offence_notice | suspension_type | NULL (or next TS code) |
| Intranet | ocms_valid_offence_notice | epr_reason_of_suspension | NULL (or next TS code) |
| Intranet | ocms_valid_offence_notice | next_processing_date | Current date + 2 days |
| Internet | eocms_valid_offence_notice | suspension_type | NULL (or next TS code) |

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

<!-- Insert flow diagram: Tab 3.10 - PLUS Manual Revival -->
![PLUS Manual Revival Flow](./OCMS19_Flowchart.drawio - Tab 3.10)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Action | Description |
| --- | --- | --- |
| 1 | PLUS initiates | PLM triggers revival from PLUS Staff Portal |
| 2 | Call OCMS API | PLUS Backend calls POST /external/v1/revive-suspension |
| 3 | API Key validation | OCMS validates API key from PLUS |
| 4 | Backend validation | OCMS backend validates request payload |
| 5 | Process revival | OCMS backend updates suspended notice and VON records |
| 6 | Record case number | Store PLUS case number in ocms_suspended_notice |
| 7 | Return response | OCMS returns success/error response to PLUS |

---

## 4.3 API Specification

### Endpoint: Revive Suspension (PLUS External)

**URL:** `POST /external/v1/revive-suspension`

**Authentication:** API Key (PLUS Backend)

**Request Body:**
```json
{
  "noticeNumber": "string",
  "suspensionType": "TS" | "PS",
  "revivalReason": "string",
  "officerAuthorisingRevival": "string",
  "revivalRemarks": "string",
  "caseNo": "string"
}
```

**Success Response:**
```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Revival processed successfully"
  }
}
```

**Error Response:**
```json
{
  "appCode": "OCMS-4XXX",
  "message": "Error description"
}
```

*Refer to FD Section 5.3 for complete API specifications.*

---

## 4.4 Data Mapping

| Zone | Database Table | Field Name | Update Value |
| --- | --- | --- | --- |
| Intranet | ocms_suspended_notice | date_of_revival | Current timestamp |
| Intranet | ocms_suspended_notice | revival_reason | Revival reason code |
| Intranet | ocms_suspended_notice | officer_authorising_revival | PLM Officer ID |
| Intranet | ocms_suspended_notice | revival_remarks | Remarks (if provided) |
| Intranet | ocms_suspended_notice | case_no | Appeal case number |
| Intranet | ocms_valid_offence_notice | suspension_type | NULL |
| Intranet | ocms_valid_offence_notice | epr_reason_of_suspension | NULL |
| Intranet | ocms_valid_offence_notice | next_processing_date | Current date + 2 days |
| Internet | eocms_valid_offence_notice | suspension_type | NULL |

---

## 4.5 Success Outcome

- Revival request from PLUS is processed successfully
- All validation rules are satisfied
- Suspended notice records are updated with revival information
- Case number from PLUS is recorded in ocms_suspended_notice
- Valid Offence Notice (VON) records are updated with cleared suspension fields
- Response is returned to PLUS Backend with appCode "OCMS-2000"
- The workflow reaches the End state without triggering any error-handling paths

---

## 4.6 Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Invalid API Key | API key is missing or invalid | Return OCMS-4000 error, authentication fails |
| Notice Not Found | Notice number does not exist | Return OCMS-4001 error, request rejected |
| PS Revival Not Supported | PLUS attempts to revive PS | Return OCMS-4003 error, feature not available |
| Notice Not Suspended | Notice does not have active suspension | Return OCMS-4002 error, request rejected |
| Database Error | Unable to update database records | Log error, return OCMS-5000 error |

*Refer to FD Section 5.5 for complete error handling scenarios.*

# OCMS 41 - Furnish Hirer/Driver Technical Document

## Document Information

| Field | Value |
|-------|-------|
| Document ID | OCMS-41-TD |
| Version | 1.6 |
| Date | 2026-01-19 |
| Status | Draft |
| Author | Technical Writer |
| Module | OCMS 41 - Furnish Hirer/Driver |

---

## Version History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-19 | Technical Writer | Initial version |
| 1.1 | 2026-01-19 | Technical Writer | Added all flowchart diagram references |
| 1.2 | 2026-01-19 | Technical Writer | Added data source attribution for API responses (Yi Jie compliance) |
| 1.3 | 2026-01-19 | Technical Writer | Fixed table names per data dictionary (ocms_suspended_notice, ocms_furnish_application_doc) |
| 1.4 | 2026-01-19 | Technical Writer | Aligned processing stages (ENA, RR3) and status codes (S) with FD |
| 1.5 | 2026-01-19 | Technical Writer | Fixed OW→ENA in all Stage Transition tables |
| 1.6 | 2026-01-19 | Technical Writer | Fixed DN→DN1 per FD (Driver stage is DN1, not DN) |

---

## Table of Contents

1. [Overview](#1-overview)
2. [Section 2: Processing Furnished Hirer/Driver from eService](#2-section-2-processing-furnished-hirerdriver-from-eservice)
3. [Section 3: Staff Portal Manual Review](#3-section-3-staff-portal-manual-review)
4. [Section 4: Staff Portal Manual Furnish or Update](#4-section-4-staff-portal-manual-furnish-or-update)
5. [Section 5: Batch Furnish and Batch Update](#5-section-5-batch-furnish-and-batch-update)
6. [Section 6: PLUS External System Integration](#6-section-6-plus-external-system-integration)
7. [Technical Standards](#7-technical-standards)
8. [Database Reference](#8-database-reference)

---

## 1. Overview

### 1.1 Purpose

This technical document provides implementation guidance for the OCMS 41 module - Furnish Hirer/Driver. The module enables:
- Processing of furnished Hirer/Driver submissions from eService
- Staff Portal manual review of submissions
- Staff Portal manual furnish/redirect/update operations
- Batch furnish and batch update operations
- External system integration with PLUS

### 1.2 Actors

| Actor | Description |
|-------|-------------|
| Motorist | Vehicle owner who submits furnished particulars via eService |
| OIC | Officer-In-Charge who reviews and processes submissions |
| PLM | PLUS Officer who performs furnish/redirect operations |

### 1.3 Reference Documents

| Document | Section |
|----------|---------|
| OCMS 41 Functional Document v1.1 | Section 2-6 |
| Data Dictionary | docs/data-dictionary/ |

---

## 2. Section 2: Processing Furnished Hirer/Driver from eService

### 2.1 Use Case

> Refer to FD Section 2 - Use Cases OCMS41.1-41.8

This section covers the end-to-end flow of processing furnished Hirer/Driver submissions from eService Portal.

### 2.2 Flowchart Diagrams

**Flowchart File:** `OCMS 41 Section 2 Technical Flow.drawio`

| Diagram ID | Diagram Name | Description |
|------------|--------------|-------------|
| 2.4.2 | Sync_Furnish_Cron | Cron job that syncs furnished submissions from Internet to Intranet |
| 2.4.3 | Auto_Approval_Review | Auto-approval validation checks for furnished submissions |
| 2.4.4 | Manual_OIC_Review | OIC manual review workflow for submissions |
| 2.4.5 | Update_Internet_Outcome | Sync approval/rejection outcome back to Internet |

---

### 2.3 Flow 2.4.2: Sync Furnish Cron

**Purpose:** Scheduled job that retrieves furnished submissions from Internet database and creates records in Intranet database.

#### 2.3.1 Job Configuration

| Parameter | Value |
|-----------|-------|
| Shedlock Name | sync_furnish_submission |
| Schedule | Every 5 minutes |
| Lock Duration | 4 minutes |
| Batch Size | 50 records |

#### 2.3.2 Swimlanes

| Lane | System |
|------|--------|
| 1 | Cron Tier |

#### 2.3.3 Process Steps

| Step | Process | Description |
|------|---------|-------------|
| 1 | Cron Trigger | Scheduled trigger every 5 minutes |
| 2 | Check Job Lock | Query ocms_shedlock for existing running job |
| 3 | Decision: Job Running? | If locked → abort, else → continue |
| 4 | Query Internet DB | SELECT from eocms_furnish_application WHERE is_sync = 'N' |
| 5 | Process Each Record | Loop through batch (max 50 records) |
| 6 | Create Intranet Record | INSERT into ocms_furnish_application |
| 7 | Decision: Create Success? | If fail → retry 3 times |
| 8 | Update Sync Flag | SET is_sync = 'Y' in eocms_furnish_application |
| 9 | Decision: Patch Success? | If fail → retry 3 times |
| 10 | Consolidate Errors | Aggregate all record-level errors |
| 11 | Send Error Email | Notify support team if errors exist |
| 12 | Log Job Status | Record in ocms_batch_job table |
| 13 | Cron End | Job completion |

#### 2.3.4 Database Operations

**Source (Internet):**
```
Table: eocms_furnish_application
Query: SELECT txn_no, notice_no, vehicle_no, furnish_name, furnish_id_type,
              furnish_id_no, owner_driver_indicator, status, ...
       WHERE is_sync = 'N'
       ORDER BY cre_date
       LIMIT 50
```

**Target (Intranet):**
```
Table: ocms_furnish_application
Action: INSERT (cre_user_id = 'ocmsiz_app_conn')
```

**Update Flag:**
```
Table: eocms_furnish_application
Action: UPDATE is_sync = 'Y', upd_user_id = 'ocmsez_app_conn'
```

---

### 2.4 Flow 2.4.3: Auto-Approval Review

**Purpose:** Validates furnished submissions against 8 auto-approval criteria to determine if submission can be auto-approved or requires manual review.

#### 2.4.1 Swimlanes

| Lane | System |
|------|--------|
| 1 | Cron Tier |

#### 2.4.2 Process Steps

| Step | Process | Description |
|------|---------|-------------|
| 1 | Process Start | Receive synced records for review |
| 2 | Read Furnished Details | Load submission data |
| 3 | CHECK 1: Permanent Suspension | Query ocms_suspended_notice for PS type |
| 4 | CHECK 2: HST ID | Verify ID not in HST database |
| 5 | CHECK 3: FIN/Passport | Check if ID type is FIN or Passport |
| 6 | CHECK 4: Furnishable Stage | Verify stage IN (ENA, RD1, RD2, RR3) |
| 7 | CHECK 5: Prior Submission | Check for existing submissions |
| 8 | CHECK 6: Existing H/D | Compare with existing H/D records |
| 9 | CHECK 7: Furnished ID Exists | Check if ID already in H/D Details |
| 10 | CHECK 8: Owner Current Offender | Verify owner is current offender |
| 11 | Decision: All Checks Pass? | Route to approval or manual review |
| 12 | Auto-Approve Path | Update status = 'A', create H/D record |
| 13 | Manual Review Path | Apply TS-PDP, set status = 'P', record reason |

#### 2.4.3 Auto-Approval Checks Detail

| Check | Rule ID | Pass Condition | Fail Reason |
|-------|---------|----------------|-------------|
| 1 | AA-001 | Notice NOT permanently suspended | "Notice is permanently suspended" |
| 2 | AA-002 | Furnished ID NOT in HST database | "HST ID" |
| 3 | AA-003 | ID type is NOT FIN or Passport | "FIN or Passport ID" |
| 4 | AA-004 | Stage IN (ENA, RD1, RD2, RR3) | "Notice no longer at furnishable stage" |
| 5 | AA-005 | No prior submissions for notice | "Prior submission under notice" |
| 6 | AA-006 | No existing H/D with furnished ID | "H/D particulars already present" |
| 7 | AA-007 | Furnished ID NOT already in H/D | "Furnished ID already in H/D Details" |
| 8 | AA-008 | Owner IS current offender | "Owner no longer current offender" |

#### 2.4.4 Processing Outcomes

**Auto-Approval Path (All Checks Pass):**

| Step | Action | Table |
|------|--------|-------|
| 1 | Update status = 'A' | ocms_furnish_application |
| 2 | Insert H/D record | ocms_offence_notice_owner_driver |
| 3 | Insert address | ocms_offence_notice_owner_driver_addr |
| 4 | Set offender_indicator = 'Y' | ocms_offence_notice_owner_driver |
| 5 | Clear previous offender indicator | ocms_offence_notice_owner_driver |
| 6 | Update processing stage (RD/DN) | ocms_valid_offence_notice |

**Manual Review Path (Any Check Fails):**

| Step | Action | Table |
|------|--------|-------|
| 1 | Apply TS-PDP suspension | ocms_suspended_notice |
| 2 | Update status = 'P' | ocms_furnish_application |
| 3 | Record reason_for_review | ocms_furnish_application |

---

### 2.5 Flow 2.4.4: Manual OIC Review

**Purpose:** OIC reviews submissions that failed auto-approval and makes approval/rejection decision.

#### 2.5.1 Swimlanes

| Lane | System |
|------|--------|
| 1 | Staff Portal (Intranet) |
| 2 | OCMS Intranet Backend |

#### 2.5.2 Process Steps

| Step | Lane | Process | Description |
|------|------|---------|-------------|
| 1 | Portal | OIC Login | Officer authenticates |
| 2 | Portal | View Dashboard | Display pending submissions count |
| 3 | Portal | Select Submission | Choose submission to review |
| 4 | Backend | Load Details | Fetch full submission data |
| 5 | Portal | Display Review Form | Show submission with documents |
| 6 | Portal | View Supporting Docs | Display uploaded documents |
| 7 | Portal | Make Decision | Select Approve/Reject |
| 8 | Portal | Enter Remarks | Optional remarks input |
| 9 | Portal | Select Notification | Check email/SMS options |
| 10 | Portal | Submit Decision | Send decision to backend |
| 11 | Backend | Validate Status | Verify status still 'P' |
| 12 | Backend | Process Decision | Route to approve/reject flow |

---

### 2.6 Flow 2.4.5: Update Internet Outcome

**Purpose:** Syncs approval/rejection outcome from Intranet to Internet database for eService Portal display.

#### 2.6.1 Swimlanes

| Lane | System |
|------|--------|
| 1 | OCMS Intranet Backend |
| 2 | Internet Database |

#### 2.6.2 Process Steps

| Step | Lane | Process | Description |
|------|------|---------|-------------|
| 1 | Backend | Receive Outcome | Get approved/rejected submission |
| 2 | Backend | Connect to Internet DB | Establish connection |
| 3 | Backend | Decision: Connected? | If fail → retry 3 times |
| 4 | Internet DB | Update Status | SET status = 'A' or 'R' |
| 5 | Internet DB | Update Remarks | Set approval/rejection remarks |
| 6 | Internet DB | Update Timestamps | Set upd_date, upd_user_id = 'ocmsez_app_conn' |
| 7 | Backend | Commit Transaction | Persist changes |
| 8 | Backend | Return Status | Return success/failure |

#### 2.6.3 Database Operation

```
Table: eocms_furnish_application
Action: UPDATE status, remarks, upd_date, upd_user_id
Where: txn_no = :txnNo
Audit User: ocmsez_app_conn
```

---

## 3. Section 3: Staff Portal Manual Review

### 3.1 Use Case

> Refer to FD Section 3 - Use Cases OCMS41.9-41.33

This section covers the OIC manual review workflow for furnished submissions.

### 3.2 Flowchart Diagrams

**Flowchart File:** `OCMS 41 Section 3 Technical Flow.drawio`

| Diagram ID | Diagram Name | Description |
|------------|--------------|-------------|
| 3.2 | Manual_Review_HL | High-level overview of manual review process |
| 3.3 | Email_Report_Cron | Daily email report to OICs |
| 3.4 | List_Applications | List furnished applications API flow |
| 3.4.3 | Default_Page_Behaviour | Default page load behavior |
| 3.4.4 | Search_Submissions | Search submissions flow |
| 3.4.5 | Check_Furnishability | Check notice furnishability flow |
| 3.5 | Get_Detail | Get application detail API flow |
| 3.6 | Approve_Submission | Approve submission API flow |
| 3.7 | Reject_Submission | Reject submission API flow |

---

### 3.3 Flow 3.2: Manual Review High-Level

**Purpose:** Overview of the complete manual review process from email notification to outcome.

#### 3.3.1 Swimlanes

| Lane | System | Color Code |
|------|--------|------------|
| 1 | Staff Portal | Blue (#dae8fc) |
| 2 | OCMS Admin API | Green (#d5e8d4) |
| 3 | Intranet Database | Yellow (#fff2cc) |
| 4 | Internet Database | Yellow (#fff2cc) |

#### 3.3.2 High-Level Process Steps

| Step | Lane | Process | Description |
|------|------|---------|-------------|
| 1 | Backend | Cron: Email Report | Daily cron sends pending submissions email |
| 2 | Portal | OIC Login | OIC accesses Staff Portal |
| 3 | Portal | View Summary List | OIC views pending submissions |
| 4 | Backend | List API | Fetch filtered submissions |
| 5 | Database | Query Applications | Retrieve from ocms_furnish_application |
| 6 | Portal | Select Submission | OIC clicks submission to review |
| 7 | Backend | Detail API | Fetch submission details |
| 8 | Database | Query Detail | Retrieve application and notice details |
| 9 | Portal | Review & Decide | OIC reviews and enters decision |
| 10 | Portal | Compose Notification | Optional email/SMS message |
| 11 | Portal | Submit Decision | OIC submits approval or rejection |
| 12 | Backend | Process Decision | Backend processes decision |
| 13 | Database | Update Records | Update application status |
| 14 | Internet DB | Sync Outcome | Sync to Internet database |
| 15 | Backend | Send Notifications | Send email/SMS if requested |
| 16 | Portal | Display Result | Show success message |

---

### 3.4 Flow 3.3: Email Report Cron

**Purpose:** Generate and send daily email report to OICs for pending submissions.

#### 3.4.1 Job Configuration

| Parameter | Value |
|-----------|-------|
| Schedule | Monday-Friday 9:00 AM |
| Frequency | Daily |

#### 3.4.2 Swimlanes

| Lane | System |
|------|--------|
| 1 | Cron Tier |
| 2 | Intranet Database |

#### 3.4.3 Process Steps

| Step | Lane | Process | Description |
|------|------|---------|-------------|
| 1 | Cron | Cron Start | Daily scheduled trigger |
| 2 | Database | Query Pending | SELECT WHERE status IN ('P', 'Resubmission') |
| 3 | Cron | Decision: Any Records? | Check if results exist |
| 4 | Cron | Generate Report | Build email content |
| 5 | Cron | Send Email | Send to OIC distribution list |
| 6 | Cron | Decision: Email Sent? | Check send result |
| 7 | Cron | Log Status | Log success or error |
| 8 | Cron | Cron End | Job completion |

---

### 3.5 Flow 3.4: List Applications

**Purpose:** API flow for fetching and filtering furnished applications for OIC dashboard.

#### 3.5.1 API Specification

| Attribute | Value |
|-----------|-------|
| Endpoint | POST /furnish/officer/list |
| Description | Lists furnished applications with filters |

#### 3.5.2 Swimlanes

| Lane | System |
|------|--------|
| 1 | Staff Portal |
| 2 | OCMS Admin API |
| 3 | Intranet Database |

#### 3.5.3 Process Steps

| Step | Lane | Process | Description |
|------|------|---------|-------------|
| 1 | Portal | Start | Page load or search trigger |
| 2 | Portal | Build Request | Construct request with filters |
| 3 | API | Receive Request | POST /furnish/officer/list |
| 4 | API | Apply Default Filters | Default status = ['P', 'Resubmission'] |
| 5 | API | Build Query | Construct SQL with pagination |
| 6 | Database | Execute Query | Query ocms_furnish_application |
| 7 | Database | Return Results | Return paginated results |
| 8 | API | Map Response | Map entity to DTO |
| 9 | API | Return Response | Return JSON response |
| 10 | Portal | Display Results | Render in table |

#### 3.5.4 Request/Response

**Request:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| statuses | String[] | No | Filter: P, Resubmission, A, R |
| noticeNo | String | No | Filter by notice number |
| vehicleNo | String | No | Filter by vehicle number |
| page | Integer | No | Page number (default: 0) |
| pageSize | Integer | No | Records per page (default: 10) |

**Response:**

| Field | Type | Source |
|-------|------|--------|
| total | Integer | Calculated: COUNT(*) from query |
| limit | Integer | Request parameter: pageSize |
| skip | Integer | Calculated: page × pageSize |
| data[].txnNo | String | ocms_furnish_application.txn_no |
| data[].noticeNo | String | ocms_furnish_application.notice_no |
| data[].vehicleNo | String | ocms_furnish_application.vehicle_no |
| data[].status | String | ocms_furnish_application.status |
| data[].reasonForReview | String | ocms_furnish_application.reason_for_review |
| data[].submissionDate | DateTime | ocms_furnish_application.cre_date |
| data[].furnishName | String | ocms_furnish_application.furnish_name |
| data[].furnishIdNo | String | ocms_furnish_application.furnish_id_no |

---

### 3.6 Flow 3.4.3: Default Page Behaviour

**Purpose:** Defines the default page load behavior when OIC accesses the Summary List page.

#### 3.6.1 Process Steps

| Step | Process | Description |
|------|---------|-------------|
| 1 | Page Load | OIC navigates to Summary List |
| 2 | Apply Default Filter | Status = ['P', 'Resubmission'] |
| 3 | Set Default Sort | Sort by submissionDate DESC |
| 4 | Set Default Pagination | Page = 0, PageSize = 10 |
| 5 | Call List API | Fetch with default parameters |
| 6 | Display Results | Render pending submissions |

---

### 3.7 Flow 3.4.4: Search Submissions

**Purpose:** Search functionality for filtering submissions by various criteria.

#### 3.7.1 Process Steps

| Step | Process | Description |
|------|---------|-------------|
| 1 | Enter Search Criteria | OIC enters notice no, vehicle no, ID no, date range |
| 2 | Validate Inputs | Frontend validation |
| 3 | Build Search Request | Construct request with filters |
| 4 | Call List API | POST with search parameters |
| 5 | Display Results | Show filtered results |
| 6 | No Results | Display "No matching records" if empty |

---

### 3.8 Flow 3.4.5: Check Furnishability

**Purpose:** Check if a notice is still furnishable before OIC makes decision.

#### 3.8.1 Process Steps

| Step | Process | Description |
|------|---------|-------------|
| 1 | Query Notice | Get notice by notice_no |
| 2 | Check Stage | Verify last_processing_stage |
| 3 | Check Suspension | Check for permanent suspension |
| 4 | Decision: Furnishable? | Return furnishability status |
| 5 | If Not Furnishable | Prompt OIC to reject |

#### 3.8.2 Furnishability Rules

| Stage | Furnishable |
|-------|-------------|
| ENA | Yes |
| RD1 | Yes |
| RD2 | Yes |
| RR3 | Yes |
| DN1 | No |
| CPC+ | No |

---

### 3.9 Flow 3.5: Get Application Detail

**Purpose:** API flow for retrieving detailed submission information for OIC review.

#### 3.9.1 API Specification

| Attribute | Value |
|-----------|-------|
| Endpoint | POST /furnish/officer/detail |
| Description | Retrieves detailed submission for review |

#### 3.9.2 Swimlanes

| Lane | System |
|------|--------|
| 1 | Staff Portal |
| 2 | OCMS Admin API |
| 3 | Intranet Database |

#### 3.9.3 Process Steps

| Step | Lane | Process | Description |
|------|------|---------|-------------|
| 1 | Portal | Click Notice No. | OIC clicks submission |
| 2 | Portal | Navigate | Navigate to Detail page |
| 3 | API | Receive Request | POST with txnNo |
| 4 | Database | Query Application | Query by txnNo |
| 5 | API | Decision: Exists? | If not found → return 404 |
| 6 | Database | Query Notice | Join with ocms_valid_offence_notice |
| 7 | Database | Query Suspension | Get active suspension info |
| 8 | Database | Query Documents | Get supporting documents |
| 9 | API | Check Furnishability | Validate notice still furnishable |
| 10 | API | Build Response | Construct response |
| 11 | Portal | Decision: Furnishable? | Show prompt if not furnishable |
| 12 | Portal | Display Details | Render detail page |

#### 3.9.4 Response Structure

| Section | Fields | Source |
|---------|--------|--------|
| noticeDetails | noticeNo, vehicleNo, ppCode, offenceDateTime | ocms_furnish_application |
| submitterDetails | name, idNo, idType, email, contactNo | ocms_furnish_application (owner_*) |
| furnishedDetails | name, idNo, idType, email, hirerDriverIndicator | ocms_furnish_application (furnish_*) |
| submissionInfo | status, reasonForReview, submissionDateTime | ocms_furnish_application |
| supportingDocuments | fileName, fileSize, mimeType, downloadUrl | ocms_furnish_application_doc |
| noticeStatus | isFurnishable, isPermanentlySuspended | Calculated |

---

### 3.10 Flow 3.6: Approve Submission

**Purpose:** API flow for OIC approval of furnished submission.

#### 3.10.1 API Specification

| Attribute | Value |
|-----------|-------|
| Endpoint | POST /furnish/officer/approve |
| Description | Approves submission and creates H/D record |

#### 3.10.2 Swimlanes

| Lane | System |
|------|--------|
| 1 | Staff Portal |
| 2 | OCMS Admin API |
| 3 | Intranet Database |
| 4 | Internet Database |

#### 3.10.3 Process Steps

| Step | Lane | Process | Description |
|------|------|---------|-------------|
| 1 | Portal | Click Confirm | OIC confirms approval |
| 2 | API | Receive Request | POST /furnish/officer/approve |
| 3 | API | Bean Validation | Validate txnNo, officerId |
| 4 | API | Decision: Valid? | If invalid → return 400 |
| 5 | Database | Query Application | Find by txnNo |
| 6 | API | Decision: Exists? | If not found → return 404 |
| 7 | API | Decision: Status P/S? | If not pending → return 409 |
| 8 | Database | Update Application | SET status = 'A', remarks |
| 9 | Database | Insert H/D Record | INSERT ocms_offence_notice_owner_driver |
| 10 | Database | Insert Address | INSERT ocms_offence_notice_owner_driver_addr |
| 11 | Database | Set Current Offender | SET offender_indicator = 'Y' |
| 12 | Database | Clear Previous | SET previous offender_indicator = 'N' |
| 13 | Database | Update Stage | Update processing stage (RD/DN) |
| 14 | Database | Revive Suspension | SET TS-PDP date_of_revival |
| 15 | Internet DB | Sync Application | UPDATE eocms_furnish_application |
| 16 | Internet DB | Sync H/D | Sync offender to Internet |
| 17 | API | Decision: Send Notification? | Check notification flags |
| 18 | API | Send Email/SMS | Send if requested |
| 19 | API | Build Response | Construct success response |
| 20 | Portal | Display Result | Show success message |

#### 3.10.4 Processing Stage Update

| Offender Type | From Stage | New Stage |
|---------------|------------|-----------|
| H (Hirer) | ENA | RD1 |
| H (Hirer) | RD1 | RD2 |
| D (Driver) | Any | DN1 |

---

### 3.11 Flow 3.7: Reject Submission

**Purpose:** API flow for OIC rejection of furnished submission.

#### 3.11.1 API Specification

| Attribute | Value |
|-----------|-------|
| Endpoint | POST /furnish/officer/reject |
| Description | Rejects submission and notifies owner |

#### 3.11.2 Swimlanes

| Lane | System |
|------|--------|
| 1 | Staff Portal |
| 2 | OCMS Admin API |
| 3 | Intranet Database |
| 4 | Internet Database |

#### 3.11.3 Process Steps

| Step | Lane | Process | Description |
|------|------|---------|-------------|
| 1 | Portal | Click Confirm | OIC confirms rejection |
| 2 | API | Receive Request | POST /furnish/officer/reject |
| 3 | API | Bean Validation | Validate txnNo, officerId, rejectionReason |
| 4 | API | Decision: Valid? | If invalid → return 400 |
| 5 | Database | Query Application | Find by txnNo |
| 6 | API | Decision: Exists? | If not found → return 404 |
| 7 | API | Decision: Status P/S? | If not pending → return 409 |
| 8 | Database | Update Application | SET status = 'R', rejection_reason |
| 9 | Internet DB | Sync Application | UPDATE eocms_furnish_application |
| 10 | Internet DB | Resend to Portal | Make notice available for resubmission |
| 11 | API | Decision: Send Email? | Check sendEmailToOwner flag |
| 12 | API | Send Email | Send rejection email |
| 13 | API | Build Response | Construct success response |
| 14 | Portal | Display Result | Show success message |

#### 3.11.4 Approval vs Rejection Comparison

| Aspect | Approval | Rejection |
|--------|----------|-----------|
| Status | 'A' | 'R' |
| H/D Record | Created | Not created |
| Processing Stage | Updated (RD/DN) | No change |
| TS-PDP Suspension | Revived | Remains active |
| Notice in eService | Removed | Resent for resubmission |

---

## 4. Section 4: Staff Portal Manual Furnish or Update

### 4.1 Use Case

> Refer to FD Section 4 - Use Cases OCMS41.4.x

This section covers manual operations by OIC:
- **Furnish** - Add new offender as Current Offender
- **Redirect** - Transfer Offender Indicator to another person
- **Update** - Modify particulars of existing Current Offender

### 4.2 Flowchart Diagrams

**Flowchart File:** `OCMS 41 Section 4 Technical Flow.drawio`

| Diagram ID | Diagram Name | Description |
|------------|--------------|-------------|
| 4.2 | High_Level_Furnish_Update | High-level overview of furnish/redirect/update |
| 4.5 | Action_Button_Check | Logic to determine which buttons to display |
| 4.6 | Furnish_Offender | Detailed furnish process with API & DB |
| 4.7 | Redirect_Notice | Detailed redirect process with API & DB |
| 4.8 | Update_Particulars | Detailed update process with API & DB |

---

### 4.3 Flow 4.2: High-Level Furnish/Update

**Purpose:** Overview of the manual furnish/update process from navigation to outcome.

#### 4.3.1 Swimlanes

| Lane | System |
|------|--------|
| 1 | Staff Portal |

#### 4.3.2 Process Steps

| Step | Process | Description |
|------|---------|-------------|
| 1 | Navigate to Search | OIC navigates to Search Notices page |
| 2 | Search Notice | OIC searches for notice |
| 3 | Click Notice No. | Opens detail page in new tab |
| 4 | Click O/H/D Sub-tab | View Owner/Hirer/Driver details |
| 5 | Display Details | Portal displays O/H/D details |
| 6 | Click UNLOCK | Portal unlocks fields |
| 7 | Detect Particulars | Portal detects presence of particulars |
| 8 | Decision: Action? | FURNISH / REDIRECT / UPDATE |
| 9 | Perform Validation | Portal validates input |
| 10 | Call Backend API | Execute action |
| 11 | Receive Result | Process response |
| 12 | Display Outcome | Show success/error message |

---

### 4.4 Flow 4.5: Action Button Check

**Purpose:** Determine which action buttons (FURNISH/REDIRECT/UPDATE) to display based on offender status.

#### 4.4.1 Swimlanes

| Lane | System |
|------|--------|
| 1 | Staff Portal |
| 2 | Backend Logic |

#### 4.4.2 Process Steps

| Step | Process | Description |
|------|---------|-------------|
| 1 | OIC Clicks UNLOCK | Trigger button check |
| 2 | Detect Particulars | Check presence of offender record |
| 3 | Decision: Offender Exists? | Check ocms_offence_notice_owner_driver |
| 4 | If No → Display FURNISH | Show FURNISH button only |
| 5 | If Yes → Check Current? | Check offender_indicator = 'Y' |
| 6 | If Current → Display UPDATE | Show UPDATE button only |
| 7 | If Not Current → Display REDIRECT | Show REDIRECT button only |

#### 4.4.3 Button Display Rules

| Condition | FURNISH | REDIRECT | UPDATE |
|-----------|---------|----------|--------|
| No existing offender for role | Show | Hide | Hide |
| Offender exists, is Current Offender | Hide | Hide | Show |
| Offender exists, NOT Current Offender | Hide | Show | Hide |

---

### 4.5 Flow 4.6: Furnish Offender

**Purpose:** Detailed technical flow for adding a new offender as Current Offender.

#### 4.5.1 API Specification

| Attribute | Value |
|-----------|-------|
| Endpoint | POST /notice/offender/furnish |
| Description | Adds new offender as Current Offender |

#### 4.5.2 Swimlanes

| Lane | System |
|------|--------|
| 1 | Staff Portal |
| 2 | OCMS Admin API |
| 3 | Intranet Database |
| 4 | Internet Database |

#### 4.5.3 Process Steps

| Step | Lane | Process | Description |
|------|------|---------|-------------|
| 1 | Portal | Click FURNISH | OIC initiates furnish |
| 2 | Portal | Enter Details | OIC enters offender details |
| 3 | Portal | Click Submit | Submit form |
| 4 | Portal | Frontend Validation | Validate required fields |
| 5 | Portal | Decision: Valid? | If invalid → show error |
| 6 | API | Receive Request | POST /notice/offender/furnish |
| 7 | API | Bean Validation | Server-side validation |
| 8 | API | Decision: Valid? | If invalid → return VALIDATION_ERROR |
| 9 | Database | Query Notice | SELECT notice_no, last_processing_stage FROM VON |
| 10 | API | Decision: Exists? | If not found → return NOT_FOUND |
| 11 | API | Check Furnishability | Verify stage is furnishable |
| 12 | API | Decision: Furnishable? | If not → return NOT_FURNISHABLE |
| 13 | Database | Check Existing | Check if offender exists for role |
| 14 | API | Decision: Exists? | Determine POST or PATCH |
| 15 | Database | Create/Update Offender | INSERT/UPDATE OND |
| 16 | Database | Create/Update Address | INSERT/UPDATE OND_ADDR (type='furnished_mail') |
| 17 | Database | Set Offender Indicator | SET offender_indicator = 'Y' |
| 18 | Database | Clear Previous | SET previous offender_indicator = 'N' |
| 19 | Database | Update Stage | Update VON.last_processing_stage |
| 20 | Database | Insert FA Record | INSERT ocms_furnish_application |
| 21 | Internet DB | Sync to Internet | INSERT eocms_furnish_application |
| 22 | API | Return Success | Return success response |
| 23 | Portal | Display Success | Show success message |

#### 4.5.4 Request Fields

| Field | Type | Max | Required | Maps To |
|-------|------|-----|----------|---------|
| noticeNo | VARCHAR | 10 | Yes | OND.notice_no |
| ownerDriverIndicator | VARCHAR | 1 | Yes | OND.owner_driver_indicator |
| idType | VARCHAR | 1 | Yes | OND.id_type |
| idNo | VARCHAR | 12 | Yes | OND.id_no |
| name | VARCHAR | 66 | Yes | OND.name |
| emailAddr | VARCHAR | 320 | No | OND.email_addr |
| telNo | VARCHAR | 12 | No | OND.offender_tel_no |

#### 4.5.5 Processing Stage Update

| Offender Type | From Stage | New Stage |
|---------------|------------|-----------|
| H (Hirer) | ENA | RD1 |
| H (Hirer) | RD1 | RD2 |
| D (Driver) | ENA | DN1 |
| D (Driver) | RD1 | DN1 |
| D (Driver) | RD2 | DN1 |

---

### 4.6 Flow 4.7: Redirect Notice

**Purpose:** Detailed technical flow for transferring Offender Indicator to another person/role.

#### 4.6.1 API Specification

| Attribute | Value |
|-----------|-------|
| Endpoint | POST /notice/offender/redirect |
| Description | Transfers Offender Indicator to new offender |

#### 4.6.2 Swimlanes

| Lane | System |
|------|--------|
| 1 | Staff Portal |
| 2 | OCMS Admin API |
| 3 | Intranet Database |
| 4 | Internet Database |

#### 4.6.3 Process Steps

| Step | Lane | Process | Description |
|------|------|---------|-------------|
| 1 | Portal | Click REDIRECT | OIC initiates redirect |
| 2 | Portal | Select Target Role | Choose O/H/D |
| 3 | Portal | Enter New Details | Enter new offender details |
| 4 | Portal | Click Submit | Submit form |
| 5 | Portal | Frontend Validation | Validate inputs |
| 6 | API | Receive Request | POST /notice/offender/redirect |
| 7 | API | Validate Request | Server-side validation |
| 8 | Database | Query Notice | Verify notice exists |
| 9 | Database | Query Current Offender | Get current offender record |
| 10 | API | Check Eligibility | Verify redirect allowed |
| 11 | API | Decision: Allowed? | If not → return error |
| 12 | Database | Clear Current Indicator | SET offender_indicator = 'N' for source |
| 13 | Database | Check Target Exists | Check if target offender exists |
| 14 | API | Decision: Exists? | Create or Update |
| 15 | Database | Create/Update Target | INSERT/UPDATE target offender |
| 16 | Database | Set New Indicator | SET offender_indicator = 'Y' for target |
| 17 | Database | Reset Stage | Reset processing stage |
| 18 | Internet DB | Sync to Internet | Sync records |
| 19 | API | Return Success | Return response |
| 20 | Portal | Display Success | Show message |

#### 4.6.4 Redirect Processing

| Step | Action | Table |
|------|--------|-------|
| 1 | SET offender_indicator = 'N' | OND (source) |
| 2 | INSERT/UPDATE offender | OND (target) |
| 3 | SET offender_indicator = 'Y' | OND (target) |
| 4 | Reset last_processing_stage | VON |

---

### 4.7 Flow 4.8: Update Particulars

**Purpose:** Detailed technical flow for updating particulars of existing Current Offender.

#### 4.7.1 API Specification

| Attribute | Value |
|-----------|-------|
| Endpoint | POST /notice/offender/update |
| Description | Updates particulars of Current Offender |

#### 4.7.2 Swimlanes

| Lane | System |
|------|--------|
| 1 | Staff Portal |
| 2 | OCMS Admin API |
| 3 | Intranet Database |

#### 4.7.3 Process Steps

| Step | Lane | Process | Description |
|------|------|---------|-------------|
| 1 | Portal | Click UPDATE | OIC initiates update |
| 2 | Portal | Modify Fields | Edit particulars |
| 3 | Portal | Click Submit | Submit changes |
| 4 | Portal | Frontend Validation | Validate inputs |
| 5 | API | Receive Request | POST /notice/offender/update |
| 6 | API | Validate Request | Server-side validation |
| 7 | Database | Query Offender | Find offender record |
| 8 | API | Decision: Current? | Verify offender_indicator = 'Y' |
| 9 | API | If Not Current | Return NOT_CURRENT_OFFENDER |
| 10 | Database | Update Particulars | UPDATE OND fields |
| 11 | Database | Update Address | UPDATE OND_ADDR if provided |
| 12 | API | Return Success | Return response |
| 13 | Portal | Display Success | Show message |

#### 4.7.4 Update Rules

| Rule | Description |
|------|-------------|
| No Stage Change | Processing stage remains unchanged |
| No Indicator Change | offender_indicator remains 'Y' |
| Partial Update | Only provided fields are updated |

---

## 5. Section 5: Batch Furnish and Batch Update

### 5.1 Use Case

> Refer to FD Section 5 - Use Cases OCMS41.5.x

This section covers batch operations:
- **Batch Furnish** - Furnish same offender to multiple notices
- **Batch Update** - Update mailing address for multiple notices

### 5.2 Flowchart Diagrams

**Flowchart File:** `OCMS 41 Section 5 Technical Flow.drawio`

| Diagram ID | Diagram Name | Description |
|------------|--------------|-------------|
| 5.2 | Batch_Furnish_Offender | Batch furnish process flow |
| 5.2.1 | API_Payloads_Batch_Furnish | API request/response for batch furnish |
| 5.2.2 | UI_Field_Behavior | UI field behavior for batch furnish |
| 5.3 | Batch_Update_Mailing_Addr | Batch update mailing address flow |
| 5.3.1 | Retrieve_Outstanding_Notices | Retrieve outstanding notices by ID |
| 5.3.1 | API_Payloads_Update_Addr | API request/response for batch update |

---

### 5.3 Flow 5.2: Batch Furnish Offender

**Purpose:** Furnish the same offender particulars to multiple notices at once.

#### 5.3.1 API Specification

| Attribute | Value |
|-----------|-------|
| Endpoint | POST /notice/offender/batch/furnish |
| Description | Batch furnish same offender to multiple notices |

#### 5.3.2 Swimlanes

| Lane | System |
|------|--------|
| 1 | Staff Portal |
| 2 | OCMS Admin API |
| 3 | Intranet Database |
| 4 | Internet Database |

#### 5.3.3 Process Steps

| Step | Lane | Process | Description |
|------|------|---------|-------------|
| 1 | Portal | Select Notices | OIC selects multiple notices |
| 2 | Portal | Click Batch Furnish | Initiate batch operation |
| 3 | Portal | Check Furnishability | Call check-furnishability API |
| 4 | API | Check Each Notice | Validate each notice |
| 5 | Portal | Display Results | Show furnishable/non-furnishable |
| 6 | Portal | Enter Offender Details | Enter common offender info |
| 7 | Portal | Click Submit | Submit batch request |
| 8 | API | Receive Request | POST /notice/offender/batch/furnish |
| 9 | API | Validate Request | Validate batch size <= 100 |
| 10 | API | Loop: Each Notice | Process each notice |
| 11 | Database | Check Furnishability | Verify notice can be furnished |
| 12 | Database | Create/Update Offender | INSERT/UPDATE OND |
| 13 | Database | Update Stage | Update processing stage |
| 14 | API | Record Result | Add to success or failed list |
| 15 | Internet DB | Sync Each | Sync to Internet per notice |
| 16 | API | Consolidate Results | Build response |
| 17 | Portal | Display Results | Show success/failure counts |

#### 5.3.4 Request Fields

| Field | Type | Max | Required | Maps To |
|-------|------|-----|----------|---------|
| noticeList | Array<String> | 100 | Yes | User input (selected notices) |
| ownerDriverIndicator | String | 1 | Yes | OND.owner_driver_indicator |
| idType | String | 20 | Yes | OND.id_type |
| idNo | String | 20 | Yes | OND.id_no |
| name | String | 66 | Yes | OND.name |
| address | Object | - | No | OND_ADDR.* |

#### 5.3.5 Response Structure

| Field | Type | Source |
|-------|------|--------|
| totalProcessed | Integer | Calculated: count of noticeList |
| successCount | Integer | Calculated: count of successful operations |
| failureCount | Integer | Calculated: count of failed operations |
| successRecords | Array | Aggregated from successful INSERT/UPDATE |
| failedRecords | Array | Aggregated from failed operations with error codes |

---

### 5.4 Flow 5.2.1: API Payloads Batch Furnish

**Purpose:** Document the API request and response structures for batch furnish.

#### 5.4.1 Request Payload

```json
{
  "noticeList": ["500500303J", "500500304K"],
  "ownerDriverIndicator": "H",
  "idType": "NRIC",
  "idNo": "S1234567A",
  "name": "JOHN LEE",
  "emailAddr": "john.lee@email.com",
  "offenderTelNo": "91234567",
  "address": {
    "blockNo": "123",
    "streetName": "ORCHARD ROAD",
    "postalCode": "238888"
  }
}
```

#### 5.4.2 Response Payload

```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Batch furnish completed",
    "totalProcessed": 2,
    "successCount": 2,
    "failureCount": 0,
    "successRecords": [...],
    "failedRecords": []
  }
}
```

---

### 5.5 Flow 5.2.2: UI Field Behavior

**Purpose:** Define UI field behavior for batch furnish form.

#### 5.5.1 Field States

| Field | Initial State | After Selection |
|-------|---------------|-----------------|
| Notice List | Empty | Populated from selection |
| Offender Type | Disabled | Enabled after notice selection |
| ID Type | Disabled | Enabled after offender type |
| ID Number | Disabled | Enabled after ID type |
| Name | Disabled | Enabled after ID number |
| Address | Optional | Enabled |

---

### 5.6 Flow 5.3: Batch Update Mailing Address

**Purpose:** Update mailing address for multiple notices of the same offender.

#### 5.6.1 API Specification

| Attribute | Value |
|-----------|-------|
| Endpoint | POST /notice/offender/batch/update-address |
| Description | Batch update mailing address |

#### 5.6.2 Swimlanes

| Lane | System |
|------|--------|
| 1 | Staff Portal |
| 2 | OCMS Admin API |
| 3 | Intranet Database |

#### 5.6.3 Process Steps

| Step | Lane | Process | Description |
|------|------|---------|-------------|
| 1 | Portal | Enter ID Number | OIC enters offender ID |
| 2 | Portal | Click Search | Search for outstanding notices |
| 3 | API | Retrieve Outstanding | POST /notice/offender/outstanding |
| 4 | Database | Query Notices | Find notices where person is current offender |
| 5 | Portal | Display Results | Show list of notices |
| 6 | Portal | Select Notices | OIC selects notices to update |
| 7 | Portal | Enter New Address | Enter new mailing address |
| 8 | Portal | Click Update | Submit batch update |
| 9 | API | Receive Request | POST /notice/offender/batch/update-address |
| 10 | API | Loop: Each Notice | Process each selected notice |
| 11 | Database | Verify Current Offender | Check offender_indicator = 'Y' |
| 12 | Database | Update Address | UPDATE OND_ADDR |
| 13 | Database | Update Contact | UPDATE OND (tel, email) |
| 14 | API | Record Result | Add to result list |
| 15 | Portal | Display Results | Show success/failure |

---

### 5.7 Flow 5.3.1: Retrieve Outstanding Notices

**Purpose:** Retrieve all outstanding notices where person is Current Offender.

#### 5.7.1 API Specification

| Attribute | Value |
|-----------|-------|
| Endpoint | POST /notice/offender/outstanding |
| Description | Get outstanding notices by offender ID |

#### 5.7.2 Process Steps

| Step | Process | Description |
|------|---------|-------------|
| 1 | Receive ID Number | Get offender ID from request |
| 2 | Query Offender Records | Find all OND records with ID |
| 3 | Filter Current Offender | WHERE offender_indicator = 'Y' |
| 4 | Exclude PS Notices | Exclude permanent suspension |
| 5 | Join Notice Details | Get notice info from VON |
| 6 | Return Results | Return list of notices |

#### 5.7.3 Query Logic

```sql
SELECT OND.notice_no, VON.vehicle_no, VON.last_processing_stage,
       OND.owner_driver_indicator, OND.name, OND.id_no
FROM ocms_offence_notice_owner_driver OND
JOIN ocms_valid_offence_notice VON ON OND.notice_no = VON.notice_no
WHERE OND.id_no = :idNo
  AND OND.offender_indicator = 'Y'
  AND NOT EXISTS (
    SELECT 1 FROM ocms_suspended_notice SN
    WHERE SN.notice_no = VON.notice_no
      AND SN.suspension_type = 'PS'
      AND SN.date_of_revival IS NULL
  )
```

#### 5.7.4 Response Structure

| Field | Type | Source |
|-------|------|--------|
| data[].noticeNo | String | OND.notice_no |
| data[].vehicleNo | String | VON.vehicle_no |
| data[].lastProcessingStage | String | VON.last_processing_stage |
| data[].ownerDriverIndicator | String | OND.owner_driver_indicator |
| data[].name | String | OND.name |
| data[].idNo | String | OND.id_no |

---

## 6. Section 6: PLUS External System Integration

### 6.1 Use Case

> Refer to FD Section 6 - Use Cases OCMS41.6.x

This section covers integration with PLUS external system for:
- **Update Hirer/Driver** - PLM furnishes H/D via PLUS Staff Portal
- **Redirect** - PLM redirects notice to new offender via PLUS

### 6.2 Flowchart Diagrams

**Flowchart File:** `OCMS 41 Section 6 Technical Flow.drawio`

| Diagram ID | Diagram Name | Description |
|------------|--------------|-------------|
| 6.2 | PLUS_Update_Hirer_Driver | PLUS furnish Hirer/Driver flow |
| 6.3 | PLUS_Redirect | PLUS redirect notice flow |

---

### 6.3 Integration Architecture

```
PLUS Staff Portal → PLUS Backend → OCMS Admin API → OCMS Database
```

### 6.4 Three-Step Operation Pattern

Each PLUS operation follows this pattern:

| Step | API | Purpose |
|------|-----|---------|
| 1 | Check | Verify eligibility |
| 2 | Retrieve | Get existing data |
| 3 | Execute | Perform action |

---

### 6.5 Flow 6.2: PLUS Update Hirer/Driver

**Purpose:** Detailed technical flow for PLUS furnishing Hirer or Driver particulars.

#### 6.5.1 Swimlanes

| Lane | System |
|------|--------|
| 1 | PLUS Staff Portal |
| 2 | PLUS Intranet Backend |
| 3 | OCMS Admin API |
| 4 | Intranet Database |

#### 6.5.2 Process Steps

| Step | Lane | Process | Description |
|------|------|---------|-------------|
| 1 | PLUS Portal | Initiate Update | PLM starts Update H/D |
| 2 | PLUS Backend | Call Check API | POST /external/plus/check-furnishability |
| 3 | OCMS API | Query Notice | SELECT notice_no, last_processing_stage FROM VON |
| 4 | OCMS API | Check Stage | Verify furnishable stage |
| 5 | OCMS API | Return Eligibility | Return furnishable status |
| 6 | PLUS Portal | Decision: Allowed? | If no → display error, end |
| 7 | PLUS Backend | Call Get Offender | POST /external/plus/get-offender |
| 8 | OCMS API | Query Offender | SELECT from OND by notice and type |
| 9 | OCMS API | Return Data | Return existing offender if any |
| 10 | PLUS Portal | Display Form | Show form with/without data |
| 11 | PLUS Portal | Enter Details | PLM enters/updates particulars |
| 12 | PLUS Portal | Click Submit | Submit form |
| 13 | PLUS Backend | Call Furnish API | POST /external/plus/furnish |
| 14 | OCMS API | Bean Validation | Validate request |
| 15 | OCMS API | Check Furnishability | Re-verify eligibility |
| 16 | Database | Save Offender | INSERT/UPDATE OND |
| 17 | Database | Save Address | INSERT OND_ADDR (type='furnished_mail') |
| 18 | Database | Set Indicator | SET offender_indicator = 'Y' if transfer |
| 19 | Database | Update Stage | Update last_processing_stage |
| 20 | Database | Set NPS | Set Next Print Schedule = Next Day |
| 21 | Database | Sync to Internet | INSERT eocms_furnish_application |
| 22 | OCMS API | Return Success | Return response |
| 23 | PLUS Portal | Display Success | Show success message |

#### 6.5.3 APIs Used

| Step | Endpoint | Method | Purpose |
|------|----------|--------|---------|
| Check | /external/plus/check-furnishability | POST | Verify eligibility |
| Retrieve | /external/plus/get-offender | POST | Get existing H/D |
| Execute | /external/plus/furnish | POST | Furnish H/D |

#### 6.5.4 Processing Stage Transitions

| From Stage | Offender Type | New Stage | NPS |
|------------|---------------|-----------|-----|
| ENA | H (Hirer) | RD1 | Next Day |
| ENA | D (Driver) | DN1 | Next Day |
| RD1 | D (Driver) | DN1 | Next Day |

---

### 6.6 Flow 6.3: PLUS Redirect

**Purpose:** Detailed technical flow for PLUS redirecting notice to new offender.

#### 6.6.1 Swimlanes

| Lane | System |
|------|--------|
| 1 | PLUS Staff Portal |
| 2 | PLUS Intranet Backend |
| 3 | OCMS Admin API |
| 4 | Intranet Database |

#### 6.6.2 Process Steps

| Step | Lane | Process | Description |
|------|------|---------|-------------|
| 1 | PLUS Portal | Initiate Redirect | PLM starts redirect |
| 2 | PLUS Backend | Call Check API | POST /external/plus/check-redirect |
| 3 | OCMS API | Query Notice | Verify notice exists |
| 4 | OCMS API | Check Stage | Verify redirectable stage |
| 5 | OCMS API | Query Current | Get current offender |
| 6 | OCMS API | Decision: Has Current? | If no → return error |
| 7 | OCMS API | Return Eligibility | Return redirect status |
| 8 | PLUS Portal | Decision: Allowed? | If no → display error, end |
| 9 | PLUS Backend | Call Get All | POST /external/plus/get-all-offenders |
| 10 | OCMS API | Query All Offenders | Get O/H/D records |
| 11 | OCMS API | Return Data | Return all offender data |
| 12 | PLUS Portal | Display Form | Show form with all particulars |
| 13 | PLUS Portal | Select Target | PLM selects target offender type |
| 14 | PLUS Portal | Edit Details | Optional: edit particulars |
| 15 | PLUS Portal | Click Submit | Submit redirect |
| 16 | PLUS Backend | Call Redirect API | POST /external/plus/redirect |
| 17 | OCMS API | Validate Request | Bean validation |
| 18 | OCMS API | Check Eligibility | Re-verify redirect allowed |
| 19 | OCMS API | Decision: Same? | If same offender → return error |
| 20 | Database | Save Address | INSERT/UPDATE OND_ADDR |
| 21 | Database | Clear Previous | SET offender_indicator = 'N' |
| 22 | Database | Set New | SET offender_indicator = 'Y' |
| 23 | Database | Update Stage | Update last_processing_stage |
| 24 | Database | Set NPS | Set Next Print Schedule = Next Day |
| 25 | Database | Sync to Internet | Sync to eocms tables |
| 26 | OCMS API | Return Success | Return response |
| 27 | PLUS Portal | Display Success | Show success message |

#### 6.6.3 APIs Used

| Step | Endpoint | Method | Purpose |
|------|----------|--------|---------|
| Check | /external/plus/check-redirect | POST | Verify eligibility |
| Retrieve | /external/plus/get-all-offenders | POST | Get all O/H/D |
| Execute | /external/plus/redirect | POST | Redirect notice |

#### 6.6.4 Offender Indicator Transfer

| Step | Action | Table |
|------|--------|-------|
| 1 | SET offender_indicator = 'N' | OND (previous) |
| 2 | SET offender_indicator = 'Y' | OND (new) |

---

### 6.7 PLUS-Specific Rules

| Rule | Description |
|------|-------------|
| Offender Types | PLUS can only furnish H or D (not O) |
| Address | Address saved as Mailing Address only |
| Registered Address | Retrieved separately from MHA/DataHive |
| NPS | Always set to Next Day |

---

## 7. Technical Standards

### 7.1 API Standards

| Standard | Rule |
|----------|------|
| HTTP Method | **POST only** for all APIs |
| Response Format | `{ "data": { "appCode", "message", ... } }` |
| Pagination | `{ "total", "limit", "skip", "data": [...] }` |
| Sensitive Data | No sensitive data in URL |

### 7.2 Database Standards

| Standard | Rule |
|----------|------|
| SQL Query | No `SELECT *` - specify only required fields |
| Insert Order | Parent table first, then child tables |
| Audit User (Intranet) | `ocmsiz_app_conn` |
| Audit User (Internet) | `ocmsez_app_conn` |

### 7.3 Error Handling

| HTTP Status | Category | Example |
|-------------|----------|---------|
| 200 | Success | Operation completed |
| 400 | Validation Error | Required field missing |
| 401 | Authentication | Invalid JWT token |
| 409 | Business Error | Notice not furnishable |
| 500 | Technical Error | Database update failed |

### 7.4 Retry Mechanism

| Parameter | Value |
|-----------|-------|
| Max Retries | 3 |
| Retry Interval | 5 seconds |
| On Failure | Set is_sync = 'N', Send email alert |

---

## 8. Database Reference

### 8.1 Tables Summary

| Table | Zone | Purpose |
|-------|------|---------|
| ocms_valid_offence_notice (VON) | Intranet | Notice information |
| ocms_offence_notice_owner_driver (OND) | Intranet | Owner/Hirer/Driver records |
| ocms_offence_notice_owner_driver_addr (OND_ADDR) | Intranet | Address records |
| ocms_furnish_application (FA) | Intranet | Furnish application records |
| ocms_furnish_application_doc | Intranet | Supporting documents |
| ocms_suspended_notice | Intranet | Suspension records |
| ocms_shedlock | Intranet | Batch job locks |
| ocms_batch_job | Intranet | Batch job logging |
| eocms_furnish_application (eFA) | Internet | PII zone sync |

### 8.2 Key Column Reference

#### OND (ocms_offence_notice_owner_driver)

| Column | Type | Description |
|--------|------|-------------|
| notice_no | varchar(10) | PK (composite) |
| owner_driver_indicator | varchar(1) | PK (composite): O/H/D |
| id_type | varchar(1) | N/F/U/P |
| id_no | varchar(12) | ID number |
| name | varchar(66) | Full name |
| email_addr | varchar(320) | Email address |
| offender_tel_no | varchar(12) | Contact number |
| offender_indicator | varchar(1) | Y = Current offender |
| is_sync | varchar(1) | Sync status |

#### VON (ocms_valid_offence_notice)

| Column | Type | Description |
|--------|------|-------------|
| notice_no | varchar(10) | PK |
| vehicle_no | varchar(14) | Vehicle number |
| last_processing_stage | varchar(3) | Current stage |

### 8.3 Processing Stage Reference

| Code | Can Furnish H | Can Furnish D |
|------|---------------|---------------|
| ENA | Yes | Yes |
| RD1 | Yes | Yes |
| RD2 | Yes | Yes |
| RR3 | Yes | Yes |
| DN1 | No | No |
| CPC | No | No |

### 8.4 Status Codes

| Code | Description |
|------|-------------|
| P | Pending - Awaiting review |
| A | Approved |
| R | Rejected |
| S | Resubmission |

---

## Appendix A: Flowchart Diagram Index

### Section 2 Diagrams

| ID | Name | File Location |
|----|------|---------------|
| 2.4.2 | Sync_Furnish_Cron | OCMS 41 Section 2 Technical Flow.drawio |
| 2.4.3 | Auto_Approval_Review | OCMS 41 Section 2 Technical Flow.drawio |
| 2.4.4 | Manual_OIC_Review | OCMS 41 Section 2 Technical Flow.drawio |
| 2.4.5 | Update_Internet_Outcome | OCMS 41 Section 2 Technical Flow.drawio |

### Section 3 Diagrams

| ID | Name | File Location |
|----|------|---------------|
| 3.2 | Manual_Review_HL | OCMS 41 Section 3 Technical Flow.drawio |
| 3.3 | Email_Report_Cron | OCMS 41 Section 3 Technical Flow.drawio |
| 3.4 | List_Applications | OCMS 41 Section 3 Technical Flow.drawio |
| 3.4.3 | Default_Page_Behaviour | OCMS 41 Section 3 Technical Flow.drawio |
| 3.4.4 | Search_Submissions | OCMS 41 Section 3 Technical Flow.drawio |
| 3.4.5 | Check_Furnishability | OCMS 41 Section 3 Technical Flow.drawio |
| 3.5 | Get_Detail | OCMS 41 Section 3 Technical Flow.drawio |
| 3.6 | Approve_Submission | OCMS 41 Section 3 Technical Flow.drawio |
| 3.7 | Reject_Submission | OCMS 41 Section 3 Technical Flow.drawio |

### Section 4 Diagrams

| ID | Name | File Location |
|----|------|---------------|
| 4.2 | High_Level_Furnish_Update | OCMS 41 Section 4 Technical Flow.drawio |
| 4.5 | Action_Button_Check | OCMS 41 Section 4 Technical Flow.drawio |
| 4.6 | Furnish_Offender | OCMS 41 Section 4 Technical Flow.drawio |
| 4.7 | Redirect_Notice | OCMS 41 Section 4 Technical Flow.drawio |
| 4.8 | Update_Particulars | OCMS 41 Section 4 Technical Flow.drawio |

### Section 5 Diagrams

| ID | Name | File Location |
|----|------|---------------|
| 5.2 | Batch_Furnish_Offender | OCMS 41 Section 5 Technical Flow.drawio |
| 5.2.1 | API_Payloads_Batch_Furnish | OCMS 41 Section 5 Technical Flow.drawio |
| 5.2.2 | UI_Field_Behavior | OCMS 41 Section 5 Technical Flow.drawio |
| 5.3 | Batch_Update_Mailing_Addr | OCMS 41 Section 5 Technical Flow.drawio |
| 5.3.1 | Retrieve_Outstanding_Notices | OCMS 41 Section 5 Technical Flow.drawio |
| 5.3.1 | API_Payloads_Update_Addr | OCMS 41 Section 5 Technical Flow.drawio |

### Section 6 Diagrams

| ID | Name | File Location |
|----|------|---------------|
| 6.2 | PLUS_Update_Hirer_Driver | OCMS 41 Section 6 Technical Flow.drawio |
| 6.3 | PLUS_Redirect | OCMS 41 Section 6 Technical Flow.drawio |

---

## Appendix B: Error Codes

### Success Codes

| Code | Message |
|------|---------|
| OCMS-2000 | Operation successful |
| OCMS-2001 | Partial success (batch) |

### Validation Errors (4xxx)

| Code | Message |
|------|---------|
| OCMS-4000 | Validation error |
| OCMS-4001 | Required field missing |
| OCMS-4002 | Invalid format |

### Business Errors (4xxx)

| Code | Message |
|------|---------|
| OCMS-4090 | Notice not found |
| OCMS-4091 | Notice not furnishable |
| OCMS-4092 | Not current offender |
| OCMS-4093 | Already processed |

### Technical Errors (5xxx)

| Code | Message |
|------|---------|
| OCMS-5000 | Internal server error |
| OCMS-5001 | Database update failed |
| OCMS-5002 | Sync to Internet failed |

---

*Document generated for OCMS 41 Furnish Hirer/Driver module*
*All flowchart diagrams from Sections 2-6 are documented*

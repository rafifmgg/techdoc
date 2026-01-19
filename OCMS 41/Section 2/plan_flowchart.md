# OCMS 41 - Furnish Hirer/Driver Flowchart Plan

## Document Information

| Field | Value |
|-------|-------|
| Version | 1.0 |
| Date | 2026-01-07 |
| Source | Functional Document v1.1, Existing Drawio Flowcharts |
| Module | OCMS 41 - Section 2: Processing Furnished Hirer and Driver from eService |

---

## Table of Contents

1. [Diagram Structure Overview](#1-diagram-structure-overview)
2. [Section 2: High-Level Flows](#2-section-2-high-level-flows)
3. [Section 3: Detailed Flows](#3-section-3-detailed-flows)
4. [Swimlane Definitions](#4-swimlane-definitions)
5. [Cross-Reference](#5-cross-reference)

---

## 1. Diagram Structure Overview

### 1.1 Recommended Diagram Tabs

| Tab ID | Tab Name | Flow Type | Description |
|--------|----------|-----------|-------------|
| 2.2 | eService High-Level Flow | High-Level | Overview of end-to-end eService furnish processing |
| 2.4.2 | Sync Furnish Cron | High-Level | Cron job orchestration flow |
| 2.4.2.1 | Retrieve Data from Internet | Detailed | Detailed process for fetching furnished submissions |
| 2.4.2.2 | Sync Supporting Documents | Detailed | Document synchronization from Internet to Intranet |
| 2.4.3 | Auto-Approval Review | Detailed | Auto-approval validation checks flow |
| 2.4.4 | Manual OIC Review | Detailed | Officer manual review and decision flow |
| 2.4.5 | Update Internet Outcome | Detailed | Sync approval/rejection outcome to Internet |

### 1.2 Diagram Hierarchy

```
Section 2: Processing Furnished Hirer and Driver from eService
│
├── 2.2 eService High-Level Flow (Overview)
│
├── 2.4 Intranet Processing
│   │
│   ├── 2.4.2 Sync Furnish Cron (Orchestrator)
│   │   ├── 2.4.2.1 Retrieve Data from Internet
│   │   └── 2.4.2.2 Sync Supporting Documents
│   │
│   ├── 2.4.3 Auto-Approval Review
│   │   └── 2.4.3.1 Auto-Approval Checks
│   │
│   ├── 2.4.4 Manual OIC Review
│   │   ├── 2.4.4.1 Approve Submission
│   │   └── 2.4.4.2 Reject Submission
│   │
│   └── 2.4.5 Update Internet Outcome
│
└── 2.5 eService Portal Display
```

---

## 2. Section 2: High-Level Flows

### 2.2 eService High-Level Flow

**Purpose**: Provide overview of the complete eService furnished hirer/driver processing flow.

#### Swimlanes (4 Lanes)

| Lane | System | Network Zone |
|------|--------|--------------|
| 1 | OCMS Intranet Backend | Intranet |
| 2 | Staff Portal (Intranet) | Intranet |
| 3 | OCMS Internet Backend | Internet |
| 4 | eService Portal (Internet) | Internet |

#### Process Steps

| Step | Lane | Process | Description |
|------|------|---------|-------------|
| 1 | Intranet Backend | Notice Created/Updated | Notice creation or status change triggers sync |
| 2 | Intranet Backend | Save to Internet DB | Backend saves notice data directly to Internet DB |
| 3 | Internet Backend | Notice Available | Updated notice available in Internet zone |
| 4 | eService Portal | Motorist Retrieves Notice | User views notice in eService |
| 5 | eService Portal | Motorist Furnishes H/D | User submits furnish request |
| 6 | Internet Backend | Save Submission | Furnished submission saved to Internet DB |
| 7 | Intranet Backend | Cron Sync | Scheduled job retrieves submissions (every 5 mins) |
| 8 | Intranet Backend | Retrieve & Save Data | Fetch furnished data from Internet |
| 9 | Intranet Backend | Sync Attachments | Sync supporting documents |
| 10 | Intranet Backend | Auto-Approval Review | Check if submission qualifies for auto-approval |
| 11 | Staff Portal | OIC Manual Review | Officer reviews submissions requiring manual review |
| 12 | Intranet Backend | Update Outcome | Update submission status in Internet DB |
| 13 | eService Portal | View Outcome | Motorist views submission result / resubmits |

#### Decision Points

| ID | Decision | Yes Path | No Path |
|----|----------|----------|---------|
| D1 | Auto-approval passed? | Update as Approved | Route to Manual Review |
| D2 | OIC Approve? | Update as Approved | Update as Rejected |

---

### 2.4.2 Sync Furnish Cron (Orchestrator)

**Purpose**: Orchestrate the scheduled synchronization of furnished submissions from Internet to Intranet.

#### Job Configuration

| Setting | Value |
|---------|-------|
| **Shedlock Name** | `sync_furnish_submission` |
| **Schedule** | Every 5 minutes |
| **Lock Duration** | 4 minutes |
| **Batch Size** | 50 records |

#### Swimlanes (1 Lane)

| Lane | System |
|------|--------|
| 1 | Cron Tier |

#### Process Steps

| Step | Process | Description |
|------|---------|-------------|
| 1 | Cron Trigger | Scheduled trigger (Daily, every 5 mins) |
| 2 | Cron Start | Job initialization |
| 3 | Check Job Lock | Query ocms_shedlock for existing running job |
| 4 | Continue/Abort | If locked, abort; otherwise continue |
| 5 | PROCESS: Retrieve Data | Sub-process to fetch furnished data from Internet |
| 6 | PROCESS: Sync Attachments | Sub-process to sync supporting documents |
| 7 | PROCESS: Auto-Approval | Sub-process to review for auto-approval |
| 8 | Check Errors | Check for record-level errors during processing |
| 9 | Consolidate Errors | Aggregate all errors from processing |
| 10 | Send Error Email | Notify support team of interface errors |
| 11 | Log Job Status | Record job outcome in ocms_batch_job table |
| 12 | Cron End | Job completion |

#### Decision Points

| ID | Decision | Yes Path | No Path |
|----|----------|----------|---------|
| D1 | Existing job running? | Log & Abort | Continue processing |
| D2 | Retrieve data successful? | Continue to Sync Attachments | Log error, Stop processing |
| D3 | Sync attachments successful? | Continue to Auto-Approval | Log error, Stop processing |
| D4 | Auto-approval successful? | Continue to Error Check | Log error, Stop processing |
| D5 | Any record-level errors? | Consolidate & Send Email | Log success status |

#### Job Status Outcomes

| Status | Condition | Database Record |
|--------|-----------|-----------------|
| Success | All processes completed | run_status = 'Success' |
| Failed - Fetch | Retrieve data process failed | run_status = 'Failed', log_text = error |
| Failed - Sync | Sync documents failed | run_status = 'Failed', log_text = error |
| Failed - Approval | Auto-approval process failed | run_status = 'Failed', log_text = error |

---

## 3. Section 3: Detailed Flows

### 2.4.2.1 Retrieve Data from Internet

**Purpose**: Detailed flow for fetching and storing furnished submissions from Internet DB to Intranet DB.

#### Swimlanes (1 Lane)

| Lane | System |
|------|--------|
| 1 | Cron Tier |

#### Process Steps

| Step | Process | Description |
|------|---------|-------------|
| 1 | Process Start | Function receives batch data (max 50 records) |
| 2 | Process Next Record | Iterate through each record in batch |
| 3 | Create Record in Intranet | Insert furnished submission to Intranet DB |
| 4 | Check Create Success | Verify record creation |
| 5 | Retry Create (x3) | Retry on failure up to 3 times |
| 6 | Check Duplicate Error | If error due to existing record |
| 7 | Patch is_sync Flag | Update Internet DB record as synced |
| 8 | Connect to Internet DB | Establish connection to Internet database |
| 9 | Retry Connection (x3) | Retry connection on failure |
| 10 | Update Sync Flag | Set is_sync = 'Y' in Internet record |
| 11 | Retry Patch (x3) | Retry patch on failure |
| 12 | Record Processing Complete | Mark individual record as processed |
| 13 | Check More Records | Loop to next record or exit |
| 14 | Consolidate Errors | Aggregate all record-level errors |
| 15 | Return Status | Return processing status and errors |

#### Decision Points

| ID | Decision | Yes Path | No Path |
|----|----------|----------|---------|
| D1 | Create record success? | Patch is_sync flag | Check if duplicate error |
| D2 | Duplicate error? | Add to error list, Skip | Retry create (x3) |
| D3 | Retry successful? | Continue | Add to error list, Skip |
| D4 | Connect to Internet success? | Patch sync flag | Retry connection (x3) |
| D5 | Retry connection success? | Continue | Log error, Add to error list |
| D6 | Patch sync success? | Record complete | Retry patch (x3) |
| D7 | Retry patch success? | Record complete | Add to error list |
| D8 | More records? | Process next record | Consolidate errors |

#### Database Operations

**Intranet DB - Create Record:**
```
Table: ocms_furnish_application
Fields: txn_no*, notice_no*, vehicle_no*, offence_date*, pp_code*, pp_name*,
        last_processing_stage*, furnish_name*, furnish_id_type*, furnish_id_no*,
        furnish_mail_blk_no*, furnish_mail_floor, furnish_mail_street_name*,
        furnish_mail_unit_no, furnish_mail_bldg_name, furnish_mail_postal_code*,
        furnish_tel_code, furnish_tel_no, furnish_email_addr,
        owner_driver_indicator*, hirer_owner_relationship*, others_relationship_desc,
        ques_one_ans*, ques_two_ans*, ques_three_ans,
        rental_period_to, rental_period_from, status*,
        owner_name*, owner_id_no*, owner_tel_code, owner_tel_no, owner_email_addr,
        corppass_staff_name, reason_for_review, remarks,
        cre_date*, cre_user_id*, upd_date, upd_user_id
```

**Internet DB - Patch Sync Flag:**
```
Table: eocms_furnish_application
Fields: is_sync*, reason_for_review, remarks, upd_date, upd_user_id
```

---

### 2.4.2.2 Sync Supporting Documents

**Purpose**: Synchronize supporting document metadata from Internet to Intranet.

#### Process Steps

| Step | Process | Description |
|------|---------|-------------|
| 1 | Process Start | Receive list of submissions for document sync |
| 2 | Process Next Record | Iterate through each submission |
| 3 | Query Document Metadata | Fetch document info from Internet DB |
| 4 | Create Document Record | Insert metadata to Intranet DB |
| 5 | Sync Blob to Intranet | Copy document from Internet to Intranet blob storage |
| 6 | Retry Blob Sync (x3) | Retry on failure |
| 7 | Update Sync Flag | Mark document as synced in Internet DB |
| 8 | Record Complete | Individual document processed |
| 9 | Check More Documents | Loop or exit |
| 10 | Consolidate Errors | Aggregate errors |
| 11 | Return Status | Return processing status |

#### Decision Points

| ID | Decision | Yes Path | No Path |
|----|----------|----------|---------|
| D1 | Blob sync success? | Update sync flag | Retry (x3) |
| D2 | Retry success? | Continue | Add to error list, Skip |
| D3 | More documents? | Process next | Consolidate errors |

---

### 2.4.3.1 Auto-Approval Review

**Purpose**: Validate furnished submissions against auto-approval criteria.

#### Swimlanes (1 Lane)

| Lane | System |
|------|--------|
| 1 | Cron Tier |

#### Process Steps

| Step | Process | Description |
|------|---------|-------------|
| 1 | Process Start | Receive Intranet furnished records for review |
| 2 | Process Next Record | Iterate through each submission |
| 3 | Read Furnished Details | Load submission data for checks |
| 4 | CHECK 1: Permanent Suspension | Query if notice has active PS suspension |
| 5 | CHECK 2: HST ID | Verify furnished ID is not HST ID |
| 6 | CHECK 3: FIN/Passport | Check if ID type is FIN or Passport |
| 7 | CHECK 4: Furnishable Stage | Verify notice is still at furnishable stage |
| 8 | CHECK 5: Prior Submission | Check for existing submissions under notice |
| 9 | CHECK 6: Existing H/D | Compare with existing hirer/driver records |
| 10 | Apply TS-PDP Suspension | Suspend notice with TS-PDP (for manual review) |
| 11 | Update Status - Pending | Set submission status to 'P' (Pending) |
| 12 | Update Status - Approved | Set submission status to 'A' (Approved) |
| 13 | Add Furnished Particulars | Create hirer/driver record as current offender |
| 14 | Change Processing Stage | Update notice stage (DN for Driver, RD for Hirer) |
| 15 | Record Complete | Individual submission processed |
| 16 | Check More Records | Loop or consolidate results |
| 17 | Return Results | List of auto-approved and manual-review submissions |

#### Auto-Approval Checks (8 Checks)

| Check | Function | Pass Condition | Fail Reason |
|-------|----------|----------------|-------------|
| 1 | Permanent Suspension | Notice NOT permanently suspended | "Notice is permanently suspended" |
| 2 | HST ID | Furnished ID NOT in HST database | "HST ID" |
| 3 | FIN/Passport | ID type is NOT FIN or Passport | "FIN or Passport ID" |
| 4 | Furnishable Stage | Stage IN (ENA, RD1, RD2, RR3) | "Notice no longer at furnishable stage" |
| 5 | Prior Submission | No prior submissions for notice | "Prior submission under notice" |
| 6 | Existing H/D | No existing H/D with furnished ID | "H/D particulars already present" |
| 7 | Furnished ID Exists | Furnished ID NOT already in H/D | "Furnished ID already in H/D Details" |
| 8 | Owner Current Offender | Owner IS current offender | "Owner no longer current offender" |

#### Decision Points

| ID | Decision | Yes Path | No Path |
|----|----------|----------|---------|
| D1 | Notice permanently suspended? | Fail - Manual Review | Continue |
| D2 | Furnished ID is HST? | Fail - Manual Review | Continue |
| D3 | ID type FIN/Passport? | Fail - Manual Review | Continue |
| D4 | Notice at furnishable stage? | Continue | Fail - Manual Review |
| D5 | Prior submission exists? | Fail - Manual Review | Continue |
| D6 | Existing H/D particulars? | Fail - Manual Review | Continue |
| D7 | Furnished ID in H/D? | Fail - Manual Review | Continue |
| D8 | Owner is current offender? | PASS - Auto Approve | Fail - Manual Review |
| D9 | TS-PDP suspension success? | Continue | Retry (x3) |
| D10 | Retry success? | Continue | Add to error list |
| D11 | More records? | Process next | Return results |

#### Processing Paths

**Auto-Approval Path (All Checks Pass):**
```
Pass All Checks → Update Status='A' → Add Furnished Particulars
→ Change Processing Stage → Record Complete
```

**Manual Review Path (Any Check Fails):**
```
Fail Any Check → Result: "Does not qualify" → Apply TS-PDP
→ Update Status='P' + Reason → Record Complete
```

---

### 2.4.4 Manual OIC Review (To Be Created)

**Purpose**: Officer reviews submissions requiring manual approval.

#### Swimlanes (2 Lanes)

| Lane | System |
|------|--------|
| 1 | Staff Portal (Intranet) |
| 2 | OCMS Intranet Backend |

#### Process Steps

| Step | Lane | Process | Description |
|------|------|---------|-------------|
| 1 | Staff Portal | OIC Login | Officer authenticates |
| 2 | Staff Portal | View Dashboard | Display pending submissions count |
| 3 | Staff Portal | Select Submission | Choose submission to review |
| 4 | Backend | Load Submission Details | Fetch full submission data |
| 5 | Staff Portal | Display Review Form | Show submission with editable fields |
| 6 | Staff Portal | View Supporting Docs | Display uploaded documents |
| 7 | Staff Portal | Make Decision | Select Approve Hirer/Approve Driver/Reject |
| 8 | Staff Portal | Enter Remarks | Optional remarks input |
| 9 | Staff Portal | Select Notification | Check email/SMS options |
| 10 | Staff Portal | Submit Decision | Send decision to backend |
| 11 | Backend | Validate Decision | Verify submission status is still 'P' |
| 12 | Backend | Process Approval | If approved - see 2.4.4.1 |
| 13 | Backend | Process Rejection | If rejected - see 2.4.4.2 |
| 14 | Backend | Send Notifications | Email/SMS if selected |
| 15 | Staff Portal | Display Success | Show confirmation message |

#### Decision Points

| ID | Decision | Yes Path | No Path |
|----|----------|----------|---------|
| D1 | Decision = Approve? | Process Approval | Process Rejection |
| D2 | Send Notification? | Send Email/SMS | Skip notification |

---

### 2.4.5 Update Internet Outcome (To Be Created)

**Purpose**: Sync approval/rejection outcome from Intranet to Internet DB.

#### Process Steps

| Step | Process | Description |
|------|---------|-------------|
| 1 | Receive Outcome | Get approved/rejected submission data |
| 2 | Connect to Internet DB | Establish database connection |
| 3 | Retry Connection (x3) | Retry on failure |
| 4 | Update Status | Set status field (A or R) |
| 5 | Update Remarks | Set approval/rejection remarks |
| 6 | Update Timestamps | Set upd_date and upd_user_id |
| 7 | Commit Transaction | Persist changes |
| 8 | Return Status | Return success/failure |

---

## 4. Swimlane Definitions

### 4.1 Standard 4-Lane Template (High-Level)

```
┌─────────────────────────────────────────────────────────────────────┐
│                    OCMS Intranet Backend                            │
├─────────────────────────────────────────────────────────────────────┤
│                    Staff Portal (Intranet)                          │
├─────────────────────────────────────────────────────────────────────┤
│                    OCMS Internet Backend                            │
├─────────────────────────────────────────────────────────────────────┤
│                    eService Portal (Internet)                       │
└─────────────────────────────────────────────────────────────────────┘
```

### 4.2 Standard 3-Lane Template (Detailed - Processing)

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Cron Tier                                   │
├─────────────────────────────────────────────────────────────────────┤
│                    OCMS Intranet Backend                            │
├─────────────────────────────────────────────────────────────────────┤
│                    OCMS Internet Backend                            │
└─────────────────────────────────────────────────────────────────────┘
```

### 4.3 Standard 2-Lane Template (Detailed - Staff Portal)

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Staff Portal (Intranet)                          │
├─────────────────────────────────────────────────────────────────────┤
│                    OCMS Intranet Backend                            │
└─────────────────────────────────────────────────────────────────────┘
```

### 4.4 Swimlane Colors

| Swimlane | Fill Color | Border Color | Notes |
|----------|------------|--------------|-------|
| Cron Tier | #1ba1e2 | #006EAF | Blue - Background jobs |
| Intranet Backend | White | Black | Default |
| Staff Portal | White | Black | Default |
| Internet Backend | White | Black | Default |
| eService Portal | White | Black | Default |
| Database Box | #e1d5e7 | #9673a6 | Purple - DB operations |
| Note/Info Box | #dae8fc | #6c8ebf | Light Blue - Notes |
| Outcome Box | #fff2cc | #d6b656 | Yellow - Results |
| Error Box | #eeeeee | #36393d | Gray - Error states |

---

## 5. Cross-Reference

### 5.1 Flow to API Mapping

| Flow | API Endpoints (from plan_api.md) |
|------|----------------------------------|
| 2.4.2.1 Retrieve Data | Internal sync operations (no external API) |
| 2.4.3.1 Auto-Approval | Internal processing (no external API) |
| 2.4.4 Manual Review | GET /furnish/pending, GET /furnish/{txnNo} |
| 2.4.4.1 Approve | POST /furnish/approve |
| 2.4.4.2 Reject | POST /furnish/reject |
| 2.4.5 Update Outcome | PATCH /furnish/sync-status |

### 5.2 Flow to Condition Mapping

| Flow | Conditions (from plan_condition.md) |
|------|-------------------------------------|
| 2.4.3.1 Auto-Approval | AA-001 to AA-008 (8 checks) |
| 2.4.4 Manual Review | FE-060 to FE-065 (form validations) |
| 2.4.4.1 Approve | BE-020 to BE-024 (business validations) |
| Error Handling | Retry conditions (DB x3, Patch x3, Blob x3) |

### 5.3 Shape Legend

| Shape | Meaning |
|-------|---------|
| Rectangle | Process step |
| Diamond | Decision point |
| Rounded Rectangle | Sub-process (PROCESS) |
| Parallelogram | Data input/output |
| Circle | Start/End node |
| Document | Database operation |
| Dashed Rectangle | Optional/Conditional step |

---

## Appendix A: Flowchart Checklist

### Before Creating Flowchart

- [ ] plan_api.md exists and is reviewed
- [ ] plan_condition.md exists and is reviewed
- [ ] All swimlanes identified
- [ ] All process steps documented
- [ ] All decision points defined
- [ ] Error handling paths included

### During Creation

- [ ] Use consistent colors per swimlane definitions
- [ ] Include shape legend in each diagram
- [ ] Number each decision point (D1, D2, etc.)
- [ ] Add notes for complex logic
- [ ] Include database operation details
- [ ] Show retry logic where applicable

### After Creation

- [ ] Verify all paths lead to end nodes
- [ ] Cross-check with API endpoints
- [ ] Cross-check with validation rules
- [ ] Review with stakeholders

---

*Document generated for OCMS 41 Section 2 flowchart planning*

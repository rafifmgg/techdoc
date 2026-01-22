# OCMS 41 Section 3 - Staff Portal Manual Review Flowchart Plan

## Document Information

| Field | Value |
|-------|-------|
| Version | 1.0 |
| Date | 2026-01-07 |
| Source | Functional Document v1.1, Existing Drawio Flowcharts, plan_api.md, plan_condition.md |
| Module | OCMS 41 - Section 3: Staff Portal Manual Review |

---

## Table of Contents

1. [Diagram Structure Overview](#1-diagram-structure-overview)
2. [High-Level Flows](#2-high-level-flows)
3. [Detailed Flows](#3-detailed-flows)
4. [Swimlane Definitions](#4-swimlane-definitions)
5. [Cross-Reference](#5-cross-reference)

---

## 1. Diagram Structure Overview

### 1.1 Recommended Diagram Tabs

| Tab ID | Tab Name | Flow Type | Description |
|--------|----------|-----------|-------------|
| 3.2 | Manual Review High-Level | High-Level | End-to-end manual review process overview |
| 3.3 | Email Report Cron | Detailed | Scheduled job for pending submissions report email |
| 3.4 | List Furnished Applications | Detailed | API flow for fetching and filtering applications |
| 3.5 | Get Application Detail | Detailed | API flow for retrieving submission details |
| 3.6 | Approve Furnished Submission | Detailed | API flow for OIC approval decision |
| 3.7 | Reject Furnished Submission | Detailed | API flow for OIC rejection decision |

### 1.2 Diagram Hierarchy

```
Section 3: Staff Portal Manual Review
│
├── 3.2 Manual Review High-Level (Overview)
│
├── 3.3 Email Report Cron
│   └── Scheduled daily job to notify OICs of pending submissions
│
├── 3.4 List Furnished Applications
│   └── POST /furnish/officer/list
│
├── 3.5 Get Application Detail
│   └── GET /furnish/officer/{txnNo}
│
├── 3.6 Approve Furnished Submission
│   ├── POST /furnish/officer/approve
│   ├── Update Furnish Application Status
│   ├── Create Hirer/Driver Record
│   ├── Update Processing Stage
│   ├── Revive TS-PDP Suspension
│   ├── Sync to Internet
│   └── Send Notifications
│
└── 3.7 Reject Furnished Submission
    ├── POST /furnish/officer/reject
    ├── Update Furnish Application Status
    ├── Sync to Internet
    ├── Resend to eService Portal
    └── Send Notifications
```

---

## 2. High-Level Flows

### 2.1 Manual Review High-Level Flow (Tab 3.2)

**Purpose**: Provide overview of the complete manual review process from email notification to outcome.

#### Swimlanes (4 Lanes)

| Lane | System | Color Code |
|------|--------|------------|
| 1 | Staff Portal | Blue (#dae8fc) |
| 2 | OCMS Admin API | Green (#d5e8d4) |
| 3 | Intranet Database | Yellow (#fff2cc) |
| 4 | Internet Database | Yellow (#fff2cc) |

#### Process Steps

| Step | Lane | Process | Description |
|------|------|---------|-------------|
| 1 | Backend | Cron: Email Report | Daily cron sends email with pending submissions |
| 2 | Staff Portal | OIC Login | OIC accesses Staff Portal |
| 3 | Staff Portal | View Summary List | OIC views list of pending submissions |
| 4 | Backend | List API | Fetch filtered submissions from database |
| 5 | Database | Query Applications | Retrieve from ocms_furnish_application |
| 6 | Staff Portal | Select Submission | OIC clicks on a submission to review |
| 7 | Backend | Detail API | Fetch submission details |
| 8 | Database | Query Detail + Notice | Retrieve application and notice details |
| 9 | Staff Portal | Review & Decide | OIC reviews and enters decision |
| 10 | Staff Portal | Compose Email/SMS | Optional: OIC composes notification message |
| 11 | Staff Portal | Submit Decision | OIC submits approval or rejection |
| 12 | Backend | Process Decision | Backend processes approval or rejection |
| 13 | Database | Update Records | Update application status and related records |
| 14 | Internet DB | Sync Outcome | Sync decision to Internet database |
| 15 | Backend | Send Notifications | Send email/SMS if requested |
| 16 | Staff Portal | Display Result | Show success or error message |

#### Decision Points

| ID | Decision | Yes Path | No Path |
|----|----------|----------|---------|
| D1 | Pending submissions exist? | Send email report | Skip (no email) |
| D2 | Notice still furnishable? | Continue review | Prompt to reject |
| D3 | Decision = Approve? | Approve flow | Reject flow |
| D4 | Send notification checked? | Send email/SMS | Skip notification |

---

## 3. Detailed Flows

### 3.1 Email Report Cron (Tab 3.3)

**Purpose**: Generate and send daily email report to OICs for pending submissions.

| Attribute | Value |
|-----------|-------|
| Process Name | Email Report Cron |
| Trigger | Scheduled (Monday-Friday 9:00 AM) |
| Frequency | Daily |
| Systems | Cron Tier, Intranet Database |

#### Swimlanes (2 Lanes)

| Lane | System | Color Code |
|------|--------|------------|
| 1 | Cron Tier | Purple (#e1d5e7) |
| 2 | Intranet Database | Yellow (#fff2cc) |

#### Process Steps

| Step | Lane | Type | Process | Description |
|------|------|------|---------|-------------|
| 1 | Cron | Start | Cron Start | Daily scheduled trigger (9:00 AM weekdays) |
| 2 | Database | Process | Query Pending Submissions | Query ocms_furnish_application WHERE status IN ('P', 'S') |
| 3 | Cron | Decision | Any record? | Check if query returns results |
| 4 | Cron | Process | Generate Report | Build email content with submission details |
| 5 | Cron | Process | Send Email | Send email to OIC distribution list |
| 6 | Cron | Decision | Email sent? | Check email sending result |
| 7 | Cron | Process | Log Success | Log successful email send |
| 8 | Cron | Process | Log Error | Log email send failure |
| 9 | Cron | End | Cron End | Job completion |

#### Decision Points

| ID | Decision | Condition | Yes Path | No Path |
|----|----------|-----------|----------|---------|
| D1 | Any record? | count > 0 | Generate Report | Log "No pending records", End |
| D2 | Email sent? | sendResult == success | Log Success | Log Error |

---

### 3.2 List Furnished Applications (Tab 3.4)

**Purpose**: API flow for fetching and filtering furnished applications for OIC dashboard.

| Attribute | Value |
|-----------|-------|
| Process Name | List Furnished Applications |
| Endpoint | POST /furnish/officer/list |
| Trigger | OIC accesses Summary List page |
| Systems | Staff Portal, OCMS Admin API, Intranet Database |

#### Swimlanes (3 Lanes)

| Lane | System | Color Code |
|------|--------|------------|
| 1 | Staff Portal | Blue (#dae8fc) |
| 2 | OCMS Admin API | Green (#d5e8d4) |
| 3 | Intranet Database | Yellow (#fff2cc) |

#### Process Steps

| Step | Lane | Type | Process | Description |
|------|------|------|---------|-------------|
| 1 | Portal | Start | Start | Page load or search trigger |
| 2 | Portal | Process | Build Request | Construct list request with filters |
| 3 | API | Process | Receive Request | POST /furnish/officer/list |
| 4 | API | Process | Apply Default Filters | Default status = ['P', 'S'] if not specified |
| 5 | API | Process | Build Query | Construct SQL with filters and pagination |
| 6 | Database | Process | Execute Query | Query ocms_furnish_application with joins |
| 7 | Database | Process | Return Results | Return paginated results |
| 8 | API | Process | Map Response | Map entity to DTO |
| 9 | API | Process | Return Response | Return JSON response with pagination info |
| 10 | Portal | Process | Display Results | Render in Summary List table |
| 11 | Portal | End | End | Complete |

#### API Request/Response

**Request Payload:**
```json
{
  "statuses": ["P", "S"],
  "noticeNo": "500500303J",
  "vehicleNo": "SNC7392R",
  "page": 0,
  "pageSize": 10,
  "sortBy": "submissionDate",
  "sortDirection": "DESC"
}
```

**Response:**
```json
{
  "success": true,
  "total": 100,
  "page": 0,
  "pageSize": 10,
  "data": [...]
}
```

---

### 3.3 Get Application Detail (Tab 3.5)

**Purpose**: API flow for retrieving detailed submission information for OIC review.

| Attribute | Value |
|-----------|-------|
| Process Name | Get Application Detail |
| Endpoint | POST /furnish/officer/detail |
| Trigger | OIC clicks on a submission from Summary List |
| Systems | Staff Portal, OCMS Admin API, Intranet Database |

#### Swimlanes (3 Lanes)

| Lane | System | Color Code |
|------|--------|------------|
| 1 | Staff Portal | Blue (#dae8fc) |
| 2 | OCMS Admin API | Green (#d5e8d4) |
| 3 | Intranet Database | Yellow (#fff2cc) |

#### Process Steps

| Step | Lane | Type | Process | Description |
|------|------|------|---------|-------------|
| 1 | Portal | Start | Start | OIC clicks Notice No. |
| 2 | Portal | Process | Navigate | Navigate to Detail page |
| 3 | API | Process | Receive Request | POST /furnish/officer/detail |
| 4 | Database | Process | Query Application | Query ocms_furnish_application by txnNo |
| 5 | API | Decision | Application exists? | Check if record found |
| 6 | Database | Process | Query Notice Details | Join with ocms_valid_offence_notice |
| 7 | Database | Process | Query Suspension | Get active suspension info |
| 8 | Database | Process | Query Documents | Get supporting documents list |
| 9 | API | Process | Check Furnishability | Validate notice is still furnishable |
| 10 | API | Process | Build Response | Construct detailed response |
| 11 | API | Process | Return Response | Return JSON with all details |
| 12 | Portal | Decision | Furnishable? | Check furnishability flag |
| 13 | Portal | Process | Display Prompt | Show "Not furnishable" prompt if needed |
| 14 | Portal | Process | Display Details | Render Submission Details page |
| 15 | Portal | End | End | Complete |

#### Decision Points

| ID | Decision | Condition | Yes Path | No Path |
|----|----------|-----------|----------|---------|
| D1 | Application exists? | record != null | Query Notice Details | Return NOT_FOUND (404) |
| D2 | Furnishable? | isFurnishable == true | Display Details | Display Prompt (Reject/Continue) |

---

### 3.4 Approve Furnished Submission (Tab 3.6)

**Purpose**: API flow for OIC approval of furnished submission.

| Attribute | Value |
|-----------|-------|
| Process Name | Approve Furnished Submission |
| Endpoint | POST /furnish/officer/approve |
| Trigger | OIC clicks Confirm after selecting Approve decision |
| Systems | Staff Portal, OCMS Admin API, Intranet Database, Internet Database |

#### Swimlanes (4 Lanes)

| Lane | System | Color Code |
|------|--------|------------|
| 1 | Staff Portal | Blue (#dae8fc) |
| 2 | OCMS Admin API | Green (#d5e8d4) |
| 3 | Intranet Database | Yellow (#fff2cc) |
| 4 | Internet Database | Yellow (#fff2cc) |

#### Process Steps

| Step | Lane | Type | Process | Description |
|------|------|------|---------|-------------|
| 1 | Portal | Start | Start | OIC clicks Confirm |
| 2 | API | Process | Receive Request | POST /furnish/officer/approve |
| 3 | API | Process | Bean Validation | Validate txnNo, officerId required |
| 4 | API | Decision | Valid? | Check validation result |
| 5 | Database | Process | Query Application | Find application by txnNo |
| 6 | API | Decision | Exists? | Check if application found |
| 7 | API | Decision | Status = P or S? | Check if pending or resubmission |
| 8 | Database | Process | Update Application | Set status='A', remarks, upd_user_id, upd_date |
| 9 | Database | Process | Insert H/D Record | Insert into ocms_offence_notice_owner_driver |
| 10 | Database | Process | Insert Address | Insert into ocms_offence_notice_owner_driver_addr |
| 11 | Database | Process | Set Current Offender | Set offender_indicator='Y' |
| 12 | Database | Process | Update Previous | Set previous offender's indicator='N' |
| 13 | Database | Process | Update Stage | Update notice processing stage (RD or DN) |
| 14 | Database | Process | Revive Suspension | Set TS-PDP date_of_revival = NOW() |
| 15 | Internet DB | Process | Sync Application | Update eocms_furnish_application |
| 16 | Internet DB | Process | Sync H/D Record | Sync offender to Internet |
| 17 | API | Decision | Send notifications? | Check notification flags |
| 18 | API | Process | Send Email/SMS | Send notifications if requested |
| 19 | API | Process | Build Response | Construct success response |
| 20 | Portal | Process | Display Result | Show success message |
| 21 | Portal | End | End | Complete |

#### Decision Points

| ID | Decision | Condition | Yes Path | No Path |
|----|----------|-----------|----------|---------|
| D1 | Valid? | validation passed | Query Application | Return VALIDATION_ERROR (400) |
| D2 | Exists? | application != null | Check Status | Return NOT_FOUND (404) |
| D3 | Status = P or S? | status IN ('P','S') | Update Application | Return ALREADY_PROCESSED (409) |
| D4 | Send notifications? | any notification flag = true | Send Email/SMS | Skip notifications |

#### Processing Stage Logic

| Hirer/Driver Indicator | New Stage | Description |
|------------------------|-----------|-------------|
| H (Hirer) | RD | Reminder Driver/Hirer stage |
| D (Driver) | DN | Driver Notice stage |

---

### 3.5 Reject Furnished Submission (Tab 3.7)

**Purpose**: API flow for OIC rejection of furnished submission.

| Attribute | Value |
|-----------|-------|
| Process Name | Reject Furnished Submission |
| Endpoint | POST /furnish/officer/reject |
| Trigger | OIC clicks Confirm after selecting Reject decision |
| Systems | Staff Portal, OCMS Admin API, Intranet Database, Internet Database |

#### Swimlanes (4 Lanes)

| Lane | System | Color Code |
|------|--------|------------|
| 1 | Staff Portal | Blue (#dae8fc) |
| 2 | OCMS Admin API | Green (#d5e8d4) |
| 3 | Intranet Database | Yellow (#fff2cc) |
| 4 | Internet Database | Yellow (#fff2cc) |

#### Process Steps

| Step | Lane | Type | Process | Description |
|------|------|------|---------|-------------|
| 1 | Portal | Start | Start | OIC clicks Confirm |
| 2 | API | Process | Receive Request | POST /furnish/officer/reject |
| 3 | API | Process | Bean Validation | Validate txnNo, officerId, rejectionReason required |
| 4 | API | Decision | Valid? | Check validation result |
| 5 | Database | Process | Query Application | Find application by txnNo |
| 6 | API | Decision | Exists? | Check if application found |
| 7 | API | Decision | Status = P or S? | Check if pending or resubmission |
| 8 | Database | Process | Update Application | Set status='R', rejection_reason, remarks, upd_user_id, upd_date |
| 9 | Internet DB | Process | Sync Application | Update eocms_furnish_application status='R' |
| 10 | Internet DB | Process | Resend to Portal | Make notice available for resubmission |
| 11 | API | Decision | Send email? | Check sendEmailToOwner flag |
| 12 | API | Process | Send Email | Send rejection email to owner |
| 13 | API | Process | Build Response | Construct success response |
| 14 | Portal | Process | Display Result | Show success message |
| 15 | Portal | End | End | Complete |

#### Decision Points

| ID | Decision | Condition | Yes Path | No Path |
|----|----------|-----------|----------|---------|
| D1 | Valid? | validation passed | Query Application | Return VALIDATION_ERROR (400) |
| D2 | Exists? | application != null | Check Status | Return NOT_FOUND (404) |
| D3 | Status = P or S? | status IN ('P','S') | Update Application | Return ALREADY_PROCESSED (409) |
| D4 | Send email? | sendEmailToOwner = true | Send Email | Skip email |

#### Key Difference from Approval

| Aspect | Approval | Rejection |
|--------|----------|-----------|
| Status Update | 'A' (Approved) | 'R' (Rejected) |
| H/D Record | Created | Not created |
| Processing Stage | Updated (RD/DN) | No change |
| TS-PDP Suspension | Revived | Remains active |
| Notice in eService | Removed | Resent for resubmission |

---

## 4. Swimlane Definitions

### 4.1 Standard Swimlane Colors

| System | Color | Hex Code | Usage |
|--------|-------|----------|-------|
| Staff Portal | Blue | #dae8fc | Frontend/UI operations |
| OCMS Admin API | Green | #d5e8d4 | Backend API processing |
| Cron Tier | Purple | #e1d5e7 | Scheduled batch jobs |
| Intranet Database | Yellow | #fff2cc | Intranet DB operations |
| Internet Database | Yellow | #fff2cc | Internet DB operations |

### 4.2 Shape Standards

| Shape | Usage | Example |
|-------|-------|---------|
| Rounded Rectangle | Start/End terminators | Start, End |
| Rectangle | Process step | Update Application |
| Diamond | Decision point | Status = P? |
| Parallelogram | Input/Output | Receive Request |

---

## 5. Cross-Reference

### 5.1 Mapping to API Endpoints

| Diagram | API Endpoint | plan_api.md Section |
|---------|--------------|---------------------|
| 3.4 List | POST /furnish/officer/list | 2.1 |
| 3.5 Detail | POST /furnish/officer/detail | 2.2 |
| 3.6 Approve | POST /furnish/officer/approve | 2.3 |
| 3.7 Reject | POST /furnish/officer/reject | 2.4 |

### 5.2 Mapping to Conditions

| Diagram | plan_condition.md Section |
|---------|--------------------------|
| 3.4 List | 2.1 (Search Parameters) |
| 3.5 Detail | 4.1 (Furnishability Check) |
| 3.6 Approve | 5.2 (Approval Decision Tree) |
| 3.7 Reject | 5.3 (Rejection Decision Tree) |

### 5.3 Mapping to Database Tables

| Diagram | Tables Used |
|---------|-------------|
| 3.3 Cron | ocms_furnish_application |
| 3.4 List | ocms_furnish_application |
| 3.5 Detail | ocms_furnish_application, ocms_valid_offence_notice, ocms_notice_suspension |
| 3.6 Approve | ocms_furnish_application, ocms_offence_notice_owner_driver, ocms_offence_notice_owner_driver_addr, eocms_* tables |
| 3.7 Reject | ocms_furnish_application, eocms_furnish_application |

### 5.4 Mapping to User Stories

| Diagram | User Stories |
|---------|--------------|
| 3.2 High-Level | OCMS41.9-41.33 (all Section 3) |
| 3.3 Cron | OCMS41.9 (email report) |
| 3.4 List | OCMS41.9-41.12 |
| 3.5 Detail | OCMS41.13-41.14 |
| 3.6 Approve | OCMS41.15-41.23 |
| 3.7 Reject | OCMS41.24-41.33 |

---

## Appendix A: Flowchart Checklist

Before creating diagrams:

- [x] All steps have clear names
- [x] All decision points have Yes/No paths defined
- [x] All paths lead to an End point
- [x] Error handling paths are included
- [x] Database operations identified
- [x] Swimlanes defined for each system
- [x] Color coding specified
- [x] API endpoints mapped

---

*Document generated for OCMS 41 Section 3 flowchart planning*

# Flowchart Plan: Foreign Vehicle Notice Processing

## Overview

| Attribute | Value |
| --- | --- |
| Feature Name | Foreign Vehicle Notice Processing |
| Version | v2.0 |
| Author | Claude |
| Created Date | 15/01/2026 |
| Last Updated | 27/01/2026 |
| Status | Revised |
| FD Reference | OCMS 14 - Section 3 |
| TD Reference | OCMS 14 - Section 2 |

---

## 1. Diagram Sections (Tabs)

The Technical Flowchart will contain the following tabs/sections:

| Tab # | Tab Name | FD Reference | Description | Priority |
| --- | --- | --- | --- | --- |
| 1 | Section_2_High_Level | 3.2 | High-level overview of FOR processing | High |
| 2 | vHub_API_Process_Flow | 3.4.1.2 | vHub API main process flow | High |
| 3 | vHub_API_Sub_Flows | 3.4.1.2 | vHub API sub-flows (parameter, lists) | High |
| 4 | vHub_SFTP_Create_Update | 3.4.2.2 | vHub SFTP main flow | High |
| 5 | vHub_SFTP_Sub_Flows | 3.4.2.2 | vHub SFTP sub-flows | Medium |
| 6 | vHub_SFTP_ACK_High_Level | 3.4.2.3 | vHub ACK processing main flow | Medium |
| 7 | vHub_SFTP_ACK_Sub_Flow | 3.4.2.3 | vHub ACK sub-flows | Medium |
| 8 | vHub_NTL_High_Level | 3.4.3.2 | vHub NTL main flow | Medium |
| 9 | vHub_NTL_Sub_Flow | 3.4.3.3 | vHub NTL sub-flows | Medium |
| 10 | REPCCS_Listed_Vehicles | 3.5.2 | REPCCS listed vehicles main flow | High |
| 11 | REPCCS_Sub_Flow | 3.5.3 | REPCCS sub-flows | Medium |
| 12 | CES_EHT_Tagged_Vehicles | 3.6.2 | CES EHT tagged vehicles flow | High |
| 13 | FOR_Admin_Fee | 3.7.2 | Admin fee processing flow | High |

---

## 2. Systems Involved (Swimlanes)

| System/Tier | Color Code | Hex | Description |
| --- | --- | --- | --- |
| OCMS Backend / CRON | Light Blue | #dae8fc | Backend processing / CRON jobs |
| OCMS Admin API | Light Green | #d5e8d4 | Intranet API services |
| Database (Intranet) | Light Yellow | #fff2cc | Intranet database operations |
| SLIFT Service | Light Pink | #f8cecc | Encryption/Decryption service |
| External System (vHub) | Light Orange | #ffe6cc | vHub API/SFTP |
| External System (REPCCS) | Light Orange | #ffe6cc | REPCCS SFTP |
| External System (CES EHT) | Light Orange | #ffe6cc | CES EHT SFTP |
| Azure Blob Storage | Light Purple | #e1d5e7 | File storage |

---

## 3. Tab 1: Section_2_High_Level

### 3.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | Foreign Vehicle Notice Processing - High Level |
| Section | 2.1 |
| Trigger | Notice created with vehicle_registration_type = 'F' |
| Frequency | Real-time (per notice creation) + Daily CRON jobs |
| Systems Involved | OCMS Backend, vHub, REPCCS, CES EHT |
| Expected Outcome | FOR notice lifecycle management |

### 3.2 High Level Flow Diagram (ASCII)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    FOREIGN VEHICLE NOTICE PROCESSING - HIGH LEVEL           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐                   │
│  │ Pre-cond:   │     │ Pre-cond:   │     │ Pre-cond:   │                   │
│  │ Raw offence │────►│ Process &   │────►│ Create FOR  │                   │
│  │ data with   │     │ detect      │     │ Notice      │                   │
│  │ veh_reg=F   │     │ veh_reg=F   │     │             │                   │
│  └─────────────┘     └─────────────┘     └──────┬──────┘                   │
│                                                  │                          │
│  Data sources: REPCCS, EHT, EEPS,               │                          │
│  PLUS Staff Portal, OCMS Staff Portal           ▼                          │
│                                          ┌─────────────┐                    │
│                                          │ Suspend     │                    │
│                                          │ with PS-FOR │                    │
│                                          └──────┬──────┘                    │
│                                                  │                          │
│                                                  │ X days unpaid            │
│                                                  ▼                          │
│                                          ┌─────────────┐                    │
│                                          │ Send to     │                    │
│                                          │ vHub,REPCCS,│                    │
│                                          │ CES EHT     │                    │
│                                          └──────┬──────┘                    │
│                                                  │                          │
│                                                  │ Y days unpaid            │
│                                                  ▼                          │
│                                          ┌─────────────┐                    │
│                                          │ Add Admin   │                    │
│                                          │ Fee (AFO)   │                    │
│                                          └──────┬──────┘                    │
│                                                  │                          │
│                                                  │ Z years unpaid           │
│                                                  ▼                          │
│                                          ┌─────────────┐     ┌─────────────┐│
│                                          │ OIC Review  │────►│ Archive &   ││
│                                          │ & Approve   │     │ Purge       ││
│                                          └─────────────┘     └──────┬──────┘│
│                                                                      │       │
│                                                                      ▼       │
│                                                               ┌───────────┐  │
│                                                               │    End    │  │
│                                                               └───────────┘  │
│                                                                              │
│  ═══════════════════════════════════════════════════════════════════════    │
│  Notice is PAYABLE during entire flow until archived                         │
│  ═══════════════════════════════════════════════════════════════════════    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.3 Process Steps Table (High Level)

| Step | Definition | Brief Description |
| --- | --- | --- |
| Pre-condition | Raw Offence Data Received | Data from REPCCS, EHT, EEPS, PLUS, OCMS Staff Portal with veh_reg_type = 'F' |
| 1 | Process & Detect | OCMS processes offence data and detects vehicle_registration_type = 'F' |
| 2 | Create FOR Notice | Notice created for foreign vehicle |
| 3 | Suspend with PS-FOR | Auto-suspend with PS-FOR at NPA stage |
| 4 | Send for Enforcement | After X days unpaid, send to vHub, REPCCS, CES EHT |
| 5 | Add Admin Fee | After Y days unpaid, add AFO amount |
| 6 | OIC Review | After Z years, OIC reviews for archival |
| 7 | Archive & Purge | Approved notices archived and purged |
| End | Process Complete | Notice exits FOR flow |

### 3.4 Exit Conditions

| Exit Point | Condition | Action |
| --- | --- | --- |
| Any Stage | Payment received | Send 'S' (Settled) to vHub |
| Any Stage | TS applied | Send 'C' (Cancelled) to vHub |
| Any Stage | PS (non-FOR) applied | Send 'C' (Cancelled) to vHub |
| End | Archived | Send 'C' (Cancelled) to vHub |

---

## 4. Tab 2: vHub_API_Process_Flow

### 4.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | vHub Update Violation API Interface |
| Section | 2.2 |
| Trigger | Scheduled CRON job |
| Frequency | Daily (timing TBD) |
| Systems Involved | OCMS Backend, REPCCS API, vHub API, Intranet DB |
| Expected Outcome | FOR notices sent to vHub via API |

### 4.2 Main Flow Diagram (ASCII)

```
┌────────────────┬────────────────────┬────────────────────┬──────────────────┐
│  OCMS CRON     │   OCMS Backend     │   External APIs    │   Database       │
├────────────────┼────────────────────┼────────────────────┼──────────────────┤
│                │                    │                    │                  │
│ ┌────────────┐ │                    │                    │                  │
│ │   Start    │ │                    │                    │                  │
│ │ vHub API   │ │                    │                    │                  │
│ │   CRON     │ │                    │                    │                  │
│ └─────┬──────┘ │                    │                    │                  │
│       │        │                    │                    │                  │
│       ▼        │                    │                    │                  │
│ ┌────────────┐ │                    │                    │ ┌──────────────┐ │
│ │ Get FOR    │─┼────────────────────┼────────────────────┼►│ Query        │ │
│ │ Parameter  │ │                    │                    │ │ ocms_param   │ │
│ └─────┬──────┘ │                    │                    │ └──────────────┘ │
│       │        │                    │                    │                  │
│       ▼        │                    │                    │                  │
│ ┌────────────┐ │                    │                    │                  │
│ │ Calculate  │ │                    │                    │                  │
│ │ FOR Date   │ │                    │                    │                  │
│ └─────┬──────┘ │                    │                    │                  │
│       │        │                    │                    │                  │
│       ├────────┼──► Prepare Settled List (sub-flow)      │                  │
│       │        │                    │                    │                  │
│       ├────────┼──► Prepare Cancelled List (sub-flow)    │                  │
│       │        │                    │                    │                  │
│       ├────────┼──► Prepare Outstanding List (sub-flow)  │                  │
│       │        │                    │                    │                  │
│       ▼        │                    │                    │                  │
│ ┌────────────┐ │                    │                    │                  │
│ │ Consolidate│ │                    │                    │                  │
│ │ All Lists  │ │                    │                    │                  │
│ └─────┬──────┘ │                    │                    │                  │
│       │        │                    │                    │                  │
│       ▼        │                    │                    │                  │
│ ┌────────────┐ │                    │                    │                  │
│ │ Verify     │ │                    │                    │                  │
│ │ Record Cnt │ │                    │                    │                  │
│ └─────┬──────┘ │                    │                    │                  │
│       │        │                    │                    │                  │
│       ▼        │                    │ ┌────────────────┐ │                  │
│ ┌────────────┐ │                    │ │ Call vHub API  │ │                  │
│ │ Batch 50   │─┼────────────────────┼►│ Update         │ │                  │
│ │ Records    │ │                    │ │ Violation      │ │                  │
│ └─────┬──────┘ │                    │ └───────┬────────┘ │                  │
│       │        │                    │         │          │                  │
│       ▼        │                    │         ▼          │                  │
│ ┌────────────┐ │                    │ ┌────────────────┐ │                  │
│ │ Success?   │ │                    │ │ Process        │ │                  │
│ └──┬─────┬───┘ │                    │ │ Response       │ │                  │
│  Yes     No    │                    │ └────────────────┘ │                  │
│    │     │     │                    │                    │                  │
│    ▼     ▼     │                    │                    │ ┌──────────────┐ │
│ ┌─────┐ ┌────┐ │                    │                    │ │ Save to      │ │
│ │Save │ │Add │─┼────────────────────┼────────────────────┼►│ ocms_offence │ │
│ │OK   │ │Err │ │                    │                    │ │ _avss        │ │
│ └──┬──┘ └─┬──┘ │                    │                    │ └──────────────┘ │
│    │      │    │                    │                    │                  │
│    └──┬───┘    │                    │                    │                  │
│       │        │                    │                    │                  │
│       ▼        │                    │                    │                  │
│ ┌────────────┐ │                    │                    │                  │
│ │ More       │ │                    │                    │                  │
│ │ Batches?   │ │                    │                    │                  │
│ └──┬─────┬───┘ │                    │                    │                  │
│  Yes     No    │                    │                    │                  │
│    │     │     │                    │                    │                  │
│    │     ▼     │                    │                    │                  │
│    │  ┌──────┐ │                    │                    │                  │
│    │  │Consol│ │                    │                    │                  │
│    │  │Errors│ │                    │                    │                  │
│    │  └──┬───┘ │                    │                    │                  │
│    │     │     │                    │                    │                  │
│    │     ▼     │                    │                    │                  │
│    │  ┌──────┐ │                    │                    │                  │
│    │  │ Send │ │                    │                    │                  │
│    │  │ Error│ │                    │                    │                  │
│    │  │ Email│ │                    │                    │                  │
│    │  └──┬───┘ │                    │                    │                  │
│    │     │     │                    │                    │                  │
│    └──►──┴─────┼────────────────────┼────────────────────┼──►┌───────────┐ │
│                │                    │                    │   │    End    │ │
│                │                    │                    │   └───────────┘ │
└────────────────┴────────────────────┴────────────────────┴──────────────────┘
```

### 4.3 Process Steps Table

| Step | Type | Definition | Brief Description | Next Step |
| --- | --- | --- | --- | --- |
| 1 | Start | vHub API CRON Start | Scheduled CRON job triggers | 2 |
| 2 | Process | Get FOR Parameter | Query ocms_parameter for FOR value | 3 |
| 3 | Process | Calculate FOR Date | Today - FOR days | 4-6 |
| 4 | Sub-flow | Prepare Settled List | Get paid FOR notices (24h) | 7 |
| 5 | Sub-flow | Prepare Cancelled List | Get TS/PS/Archive notices | 7 |
| 6 | Sub-flow | Prepare Outstanding List | Get unpaid FOR notices past period | 7 |
| 7 | Process | Consolidate Lists | Merge all lists | 8 |
| 8 | Process | Verify Record Count | Ensure totals match | 9 |
| 9 | Process | Batch 50 Records | Prepare batch for API | 10 |
| 10 | External | Call vHub API | Send to vHub Update Violation API | 11 |
| 11 | Decision | Record Sent OK? | Check API response per record | Yes→12, No→13 |
| 12 | Process | Save Success | Save to ocms_offence_avss with success | 14 |
| 13 | Process | Add to Error Log | Save to ocms_offence_avss with error | 14 |
| 14 | Decision | More Batches? | Check if more records to send | Yes→9, No→15 |
| 15 | Process | Consolidate Errors | Gather all errors | 16 |
| 16 | Process | Send Error Email | Send interfacing error email | End |
| End | End | Process Complete | CRON job finished | - |

### 4.4 Decision Logic

| ID | Decision | Input | Condition | Yes Action | No Action |
| --- | --- | --- | --- | --- | --- |
| D1 | Record Sent OK? | vHub API response | status_code = '0' | Save success status | Add to error log |
| D2 | More Batches? | Record count | remaining > 0 | Process next batch | Consolidate errors |

### 4.5 Database Operations

| Step | Operation | Database | Table | Query/Action |
| --- | --- | --- | --- | --- |
| 2 | SELECT | Intranet | ocms_parameter | Get FOR parameter value |
| 4-6 | SELECT | Intranet | ocms_valid_offence_notice | Query notices by criteria |
| 12-13 | INSERT | Intranet | ocms_offence_avss | Save API results |

---

## 5. Tab 3: vHub_API_Sub_Flows

### 5.1 Sub-flow: Get FOR Parameter & Calculate Date

```
┌─────────────────────────────────────────────────────────────────────────────┐
│              GET FOR PARAMETER & CALCULATE DATE SUB-FLOW                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌───────────┐     ┌───────────────┐     ┌───────────┐                     │
│  │  Start    │────►│ Query         │────►│ Success?  │                     │
│  │           │     │ ocms_param    │     │           │                     │
│  └───────────┘     │ FOR value     │     └─────┬─────┘                     │
│                    └───────────────┘       Yes/│\No                        │
│                                               │  │                          │
│  Query:                                  ┌────┘  └────┐                     │
│  parameter_id = 'PERIOD'                     │            │                     │
│  code = 'FOR'                      ▼            ▼                     │
│                                    ┌───────────┐ ┌───────────┐             │
│                                    │ Calculate │ │ Retry 1x  │             │
│                                    │ Date:     │ └─────┬─────┘             │
│                                    │ Today-FOR │       │                    │
│                                    └─────┬─────┘       ▼                    │
│                                          │       ┌───────────┐             │
│  Formula:                                │       │ Success?  │             │
│  FOR Date = Today - FOR days             │       └─────┬─────┘             │
│                                          │         Yes/│\No                │
│  Example:                                │            │  │                  │
│  FOR = 2, Today = 18 Jun 2025            │       ┌────┘  └────┐            │
│  FOR Date = 16 Jun 2025                  │       │            │            │
│                                          │       ▼            ▼            │
│                                          │  ┌───────┐   ┌───────────┐      │
│                                          │  │Proceed│   │ Log Error │      │
│                                          │  └───┬───┘   └─────┬─────┘      │
│                                          │      │             │            │
│                                          └──────┼─────────────┘            │
│                                                 ▼                          │
│                                          ┌───────────┐                     │
│                                          │    End    │                     │
│                                          └───────────┘                     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 Sub-flow: Prepare Settled Notices List

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    PREPARE SETTLED NOTICES SUB-FLOW                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌───────────┐     ┌───────────────┐     ┌───────────┐                     │
│  │  Start    │────►│ Query VON     │────►│ Any       │                     │
│  │           │     │ Newly Paid    │     │ Records?  │                     │
│  └───────────┘     │ FOR Notices   │     └─────┬─────┘                     │
│                    └───────────────┘       Yes/│\No                        │
│                                               │  │                          │
│  Query Conditions:                       ┌────┘  └────┐                     │
│  - epr_reason_of_suspension = 'FOR'      │            │                     │
│  - amount_paid > 0                       ▼            ▼                     │
│  - payment_date >= today - 24h     ┌───────────┐ ┌───────────┐             │
│                                    │ Call      │ │ Return    │             │
│                                    │ REPCCS    │ │ Empty     │             │
│                                    │ CarPark   │ │ List      │             │
│                                    │ API       │ └─────┬─────┘             │
│                                    └─────┬─────┘       │                    │
│                                          │             │                    │
│                                          ▼             │                    │
│                                    ┌───────────┐       │                    │
│                                    │ Success?  │       │                    │
│                                    └─────┬─────┘       │                    │
│                                      Yes/│\No         │                    │
│                                         │  │          │                    │
│                                    ┌────┘  └────┐     │                    │
│                                    │            │     │                    │
│                                    ▼            ▼     │                    │
│                              ┌───────────┐ ┌───────┐  │                    │
│                              │ Get PP    │ │Retry  │  │                    │
│                              │ Code Dict │ │2x @1s │  │                    │
│                              └─────┬─────┘ └───┬───┘  │                    │
│                                    │           │      │                    │
│                                    ▼           │      │                    │
│                              ┌───────────┐     │      │                    │
│                              │ Veh Cat   │     │      │                    │
│                              │ H or Y?   │     │      │                    │
│                              └─────┬─────┘     │      │                    │
│                                Yes/│\No       │      │                    │
│                                   │  │        │      │                    │
│                              ┌────┘  │        │      │                    │
│                              │       │        │      │                    │
│                              ▼       │        │      │                    │
│                        ┌───────────┐ │        │      │                    │
│                        │ Map H→B   │ │        │      │                    │
│                        │ Map Y→M   │ │        │      │                    │
│                        └─────┬─────┘ │        │      │                    │
│                              │       │        │      │                    │
│                              └───┬───┘        │      │                    │
│                                  │            │      │                    │
│                                  ▼            │      │                    │
│                            ┌───────────┐      │      │                    │
│                            │ Generate  │◄─────┘      │                    │
│                            │ Settled   │             │                    │
│                            │ List      │             │                    │
│                            └─────┬─────┘             │                    │
│                                  │                   │                    │
│                                  └───────────────────┘                    │
│                                          │                                 │
│                                          ▼                                 │
│                                   ┌───────────┐                            │
│                                   │    End    │                            │
│                                   └───────────┘                            │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.3 Sub-flow: Prepare Outstanding Notices List

| Step | Type | Definition | Description | Next |
| --- | --- | --- | --- | --- |
| 1 | Start | Start | Begin sub-flow | 2 |
| 2 | Process | Query VON | Query unpaid FOR notices past FOR period | 3 |
| 3 | Decision | Any Records? | Check if notices found | Yes→4, No→End |
| 4 | Process | Call REPCCS API | Get car park codes | 5 |
| 5 | Decision | Success? | Check API response | Yes→6, No→Retry |
| 6 | Process | Map Vehicle Types | H→B, Y→M | 7 |
| 7 | Process | Generate List | Create Outstanding list with status 'O' | End |

### 5.4 Sub-flow: Prepare Cancelled Notices List

| Step | Type | Definition | Description | Next |
| --- | --- | --- | --- | --- |
| 1 | Start | Start | Begin sub-flow | 2 |
| 2 | Process | Query TS Today | Get FOR notices with TS applied today | 3 |
| 3 | Process | Query PS Today | Get FOR notices with non-FOR PS today | 4 |
| 4 | Process | Query Archive | Get FOR notices scheduled for archive tomorrow | 5 |
| 5 | Process | Merge Lists | Combine all cancelled notices | 6 |
| 6 | Process | Generate List | Create Cancelled list with status 'C' | End |

---

## 6. Tab 4: vHub_SFTP_Create_Update

### 6.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | vHub Violation Case Create/Update SFTP |
| Section | 2.4 |
| Trigger | Scheduled CRON job |
| Frequency | Daily |
| Systems Involved | OCMS Backend, Azure Blob, vHub SFTP |
| Expected Outcome | XML file sent to vHub via SFTP |

### 6.2 Process Steps Table

| Step | Type | Definition | Brief Description | Next Step |
| --- | --- | --- | --- | --- |
| 1 | Start | CRON Start | Scheduled job triggers | 2 |
| 2 | Sub-flow | Get FOR & Calculate | Same as API flow | 3-5 |
| 3 | Sub-flow | Prepare Settled | Query paid notices | 6 |
| 4 | Sub-flow | Prepare Cancelled | Query TS/PS/Archive | 6 |
| 5 | Sub-flow | Prepare Outstanding | Query unpaid past FOR | 6 |
| 6 | Process | Consolidate Lists | Merge all records | 7 |
| 7 | Process | Generate XML | Create XML file | 8 |
| 8 | Decision | Gen Success? | Check file generation | Yes→9, No→E1 |
| 9 | External | **Call SLIFT Encrypt** | Encrypt file via SLIFT service | 10 |
| 10 | Decision | **Encrypt Success?** | Check SLIFT response | Yes→11, No→E4 |
| 11 | Process | Upload Azure | Store encrypted file in Azure Blob | 12 |
| 12 | Decision | Azure Success? | Check Azure upload | Yes→13, No→E2 |
| 13 | Process | Upload SFTP | Send encrypted file to vHub SFTP | 14 |
| 14 | Decision | SFTP Success? | Check SFTP upload | Yes→15, No→E3 |
| 15 | Process | Save Results | Store in ocms_offence_avss_sftp | End |
| E1 | Error | Gen Error | Log error, send email | End |
| E2 | Error | Azure Error | Log error, send email | End |
| E3 | Error | SFTP Error | Log error, send email | End |
| E4 | Error | **SLIFT Error** | Log error, send email, abort transfer | End |

---

## 7. Tab 5: vHub_SFTP_Sub_Flows

Similar structure to Tab 3, with additional:

### 7.1 Sub-flow: Generate XML File

| Step | Definition | Description |
| --- | --- | --- |
| 1 | Start | Begin XML generation |
| 2 | Build Header | Create XML header with batch info |
| 3 | Loop Records | Iterate through consolidated list |
| 4 | Build Record | Create XML element per record |
| 5 | Validate XML | Validate XML structure |
| 6 | Success? | Check validation |
| 7 | Return XML | Return generated XML |

### 7.2 Sub-flow: Upload to Azure

| Step | Definition | Description |
| --- | --- | --- |
| 1 | Start | Begin Azure upload |
| 2 | Connect Azure | Establish connection |
| 3 | Upload File | Upload XML to blob |
| 4 | Verify Upload | Confirm file exists |
| 5 | Return Status | Return success/failure |

---

## 8. Tab 6: vHub_SFTP_ACK_High_Level

### 8.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | vHub SFTP ACK Processing |
| Section | 2.6 |
| Trigger | Scheduled CRON job |
| Frequency | Daily |
| Systems Involved | OCMS Backend, vHub SFTP, Intranet DB |
| Expected Outcome | ACK file processed, statuses updated |

### 8.2 Process Steps Table

| Step | Type | Definition | Brief Description | Next Step |
| --- | --- | --- | --- | --- |
| 1 | Start | CRON Start | Job triggers | 2 |
| 2 | Process | Connect SFTP | Connect to vHub SFTP | 3 |
| 3 | Decision | ACK File Exists? | Check for ACK file | Yes→4, No→End |
| 4 | Process | Download ACK | Download encrypted ACK file | 5 |
| 5 | Process | Upload to Azure | Store encrypted file in Azure Blob | 6 |
| 6 | External | **Call SLIFT Decrypt** | Decrypt file via SLIFT service | 7 |
| 7 | Decision | **Decrypt Success?** | Check SLIFT response | Yes→8, No→E1 |
| 8 | Process | Parse ACK | Parse XML ACK file | 9 |
| 9 | Process | Loop Records | Process each record | 10 |
| 10 | Decision | Record Success? | Check ACK status | Yes→11, No→12 |
| 11 | Process | Update Success | Update ocms_offence_avss_sftp | 13 |
| 12 | Process | Log Error | Add to error log | 13 |
| 13 | Decision | More Records? | Check remaining | Yes→9, No→14 |
| 14 | Process | Send Error Email | If any errors | End |
| E1 | Error | **SLIFT Error** | Log error, mark file as failed | End |

---

## 9. Tab 7: vHub_SFTP_ACK_Sub_Flow

### 9.1 Sub-flow: Parse ACK File

| Step | Definition | Description |
| --- | --- | --- |
| 1 | Start | Begin parsing |
| 2 | Load XML | Load ACK XML file |
| 3 | Extract Records | Get all record elements |
| 4 | Map Fields | Map ACK fields to internal fields |
| 5 | Return Records | Return parsed records |

---

## 10. Tab 8: vHub_NTL_High_Level

### 10.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | vHub NTL (No Trace Letter) Processing |
| Section | 2.8 |
| Trigger | Scheduled CRON job |
| Frequency | Daily |
| Systems Involved | OCMS Backend, vHub SFTP |
| Expected Outcome | NTL responses processed |

### 10.2 Process Steps Table

| Step | Type | Definition | Brief Description | Next Step |
| --- | --- | --- | --- | --- |
| 1 | Start | CRON Start | Job triggers | 2 |
| 2 | Process | Connect SFTP | Connect to vHub | 3 |
| 3 | Decision | NTL File Exists? | Check for NTL file | Yes→4, No→End |
| 4 | Process | Download NTL | Download encrypted NTL file | 5 |
| 5 | Process | Upload to Azure | Store encrypted file in Azure Blob | 6 |
| 6 | External | **Call SLIFT Decrypt** | Decrypt file via SLIFT service | 7 |
| 7 | Decision | **Decrypt Success?** | Check SLIFT response | Yes→8, No→E1 |
| 8 | Process | Parse NTL | Parse file content | 9 |
| 9 | Process | Update Notices | Update affected notices | 10 |
| 10 | Process | Log Results | Log processing results | End |
| E1 | Error | **SLIFT Error** | Log error, mark file as failed | End |

---

## 11. Tab 9: vHub_NTL_Sub_Flow

Similar structure for NTL sub-flows.

---

## 12. Tab 10: REPCCS_Listed_Vehicles

### 12.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | Generate REPCCS Listed Vehicle File |
| Section | 2.10 |
| Trigger | Scheduled CRON job |
| Frequency | Daily |
| Systems Involved | OCMS Backend, Azure Blob, REPCCS SFTP |
| Expected Outcome | Listed vehicle file sent to REPCCS |

### 12.2 Main Flow Diagram (ASCII)

```
┌────────────────┬────────────────────┬────────────────────┬──────────────────┐
│  OCMS CRON     │   OCMS Backend     │   External         │   Database       │
├────────────────┼────────────────────┼────────────────────┼──────────────────┤
│                │                    │                    │                  │
│ ┌────────────┐ │                    │                    │                  │
│ │   Start    │ │                    │                    │                  │
│ │ REPCCS     │ │                    │                    │                  │
│ │ CRON       │ │                    │                    │                  │
│ └─────┬──────┘ │                    │                    │                  │
│       │        │                    │                    │                  │
│       ▼        │                    │                    │                  │
│ ┌────────────┐ │                    │                    │ ┌──────────────┐ │
│ │ Get FOR    │─┼────────────────────┼────────────────────┼►│ Query        │ │
│ │ Parameter  │ │                    │                    │ │ ocms_param   │ │
│ └─────┬──────┘ │                    │                    │ └──────────────┘ │
│       │        │                    │                    │                  │
│       ▼        │                    │                    │                  │
│ ┌────────────┐ │                    │                    │ ┌──────────────┐ │
│ │ Query      │─┼────────────────────┼────────────────────┼►│ Query VON    │ │
│ │ Listed     │ │                    │                    │ │ PS-FOR,      │ │
│ │ Vehicles   │ │                    │                    │ │ unpaid       │ │
│ └─────┬──────┘ │                    │                    │ └──────────────┘ │
│       │        │                    │                    │                  │
│       ▼        │                    │                    │                  │
│ ┌────────────┐ │                    │                    │                  │
│ │ Generate   │ │                    │                    │                  │
│ │ File       │ │                    │                    │                  │
│ └─────┬──────┘ │                    │                    │                  │
│       │        │                    │                    │                  │
│       ▼        │                    │                    │                  │
│ ┌────────────┐ │                    │ ┌────────────────┐ │                  │
│ │ Upload     │─┼────────────────────┼►│ Azure Blob     │ │                  │
│ │ Azure      │ │                    │ │ Storage        │ │                  │
│ └─────┬──────┘ │                    │ └────────────────┘ │                  │
│       │        │                    │                    │                  │
│       ▼        │                    │                    │                  │
│ ┌────────────┐ │                    │                    │                  │
│ │ Success?   │ │                    │                    │                  │
│ └──┬─────┬───┘ │                    │                    │                  │
│  Yes     No    │                    │                    │                  │
│    │     │     │                    │                    │                  │
│    ▼     ▼     │                    │ ┌────────────────┐ │                  │
│ ┌─────┐ ┌────┐ │                    │ │ REPCCS SFTP    │ │                  │
│ │SFTP │ │Err │ │                    │ │ Server         │ │                  │
│ │Upload│ │   │─┼──► Send Error Email│ └────────────────┘ │                  │
│ └──┬──┘ └────┘ │                    │         ▲          │                  │
│    │           │                    │         │          │                  │
│    └───────────┼────────────────────┼─────────┘          │                  │
│                │                    │                    │                  │
│       ▼        │                    │                    │                  │
│ ┌────────────┐ │                    │                    │                  │
│ │    End     │ │                    │                    │                  │
│ └────────────┘ │                    │                    │                  │
│                │                    │                    │                  │
└────────────────┴────────────────────┴────────────────────┴──────────────────┘
```

### 12.3 Process Steps Table

| Step | Type | Definition | Brief Description | Next Step |
| --- | --- | --- | --- | --- |
| 1 | Start | CRON Start | Job triggers | 2 |
| 2 | Process | Get FOR Parameter | Query parameter | 3 |
| 3 | Process | Query Listed Vehicles | Query qualifying notices | 4 |
| 4 | Process | Generate File | Create listed vehicle file | 5 |
| 5 | External | **Call SLIFT Encrypt** | Encrypt file via SLIFT service | 6 |
| 6 | Decision | **Encrypt Success?** | Check SLIFT response | Yes→7, No→E3 |
| 7 | Process | Upload Azure | Store encrypted file in Azure Blob | 8 |
| 8 | Decision | Azure Success? | Check upload | Yes→9, No→E1 |
| 9 | Process | Upload SFTP | Send encrypted file to REPCCS SFTP | 10 |
| 10 | Decision | SFTP Success? | Check upload | Yes→End, No→E2 |
| E1 | Error | Azure Error | Log, send email | End |
| E2 | Error | SFTP Error | Log, send email | End |
| E3 | Error | **SLIFT Error** | Log error, send email, abort transfer | End |

### 12.4 Query Conditions

```sql
SELECT vehicle_no, offence_date_and_time, offence_rule, pp_code, composition_amount
FROM ocms_valid_offence_notice
WHERE suspension_type = 'PS'
AND epr_reason_of_suspension = 'FOR'
AND amount_paid = 0
AND offence_date_and_time <= DATEADD(day, -@FOR_days, GETDATE())
```

---

## 13. Tab 11: REPCCS_Sub_Flow

### 13.1 Sub-flow: Generate Listed Vehicle File

| Step | Definition | Description |
| --- | --- | --- |
| 1 | Start | Begin file generation |
| 2 | Create Header | Add file header |
| 3 | Loop Records | Iterate notices |
| 4 | Format Record | Format as CSV/TXT line |
| 5 | Write Record | Add to file |
| 6 | Create Footer | Add file footer with count |
| 7 | Return File | Return generated file |

---

## 14. Tab 12: CES_EHT_Tagged_Vehicles

### 14.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | Generate CES EHT Tagged Vehicle File |
| Section | 2.12 |
| Trigger | Scheduled CRON job |
| Frequency | Daily |
| Systems Involved | OCMS Backend, Azure Blob, CES EHT SFTP |
| Expected Outcome | Tagged vehicle file sent to CES EHT |

### 14.2 Process Steps Table

| Step | Type | Definition | Brief Description | Next Step |
| --- | --- | --- | --- | --- |
| 1 | Start | CRON Start | Job triggers | 2 |
| 2 | Process | Get FOR Parameter | Query parameter | 3 |
| 3 | Process | Query Tagged Vehicles | Query qualifying notices | 4 |
| 4 | Process | Generate File | Create tagged vehicle file | 5 |
| 5 | External | **Call SLIFT Encrypt** | Encrypt file via SLIFT service | 6 |
| 6 | Decision | **Encrypt Success?** | Check SLIFT response | Yes→7, No→E3 |
| 7 | Process | Upload Azure | Store encrypted file in Azure Blob | 8 |
| 8 | Decision | Azure Success? | Check upload | Yes→9, No→E1 |
| 9 | Process | Upload SFTP | Send encrypted file to CES EHT SFTP | 10 |
| 10 | Decision | SFTP Success? | Check upload | Yes→End, No→E2 |
| E1 | Error | Azure Error | Log, send email | End |
| E2 | Error | SFTP Error | Log, send email | End |
| E3 | Error | **SLIFT Error** | Log error, send email, abort transfer | End |

---

## 15. Tab 13: FOR_Admin_Fee

### 15.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | Admin Fee Processing for FOR Notices |
| Section | 2.13 |
| Trigger | Scheduled CRON job |
| Frequency | Daily |
| Systems Involved | OCMS Backend, Intranet DB |
| Expected Outcome | Admin fee added to eligible FOR notices |

### 15.2 Main Flow Diagram (ASCII)

```
┌────────────────┬────────────────────────────────────────┬──────────────────┐
│  OCMS CRON     │   OCMS Backend                         │   Database       │
├────────────────┼────────────────────────────────────────┼──────────────────┤
│                │                                        │                  │
│ ┌────────────┐ │                                        │                  │
│ │   Start    │ │                                        │                  │
│ │ Admin Fee  │ │                                        │                  │
│ │ CRON       │ │                                        │                  │
│ └─────┬──────┘ │                                        │                  │
│       │        │                                        │                  │
│       ▼        │                                        │ ┌──────────────┐ │
│ ┌────────────┐ │                                        │ │ Query        │ │
│ │ Get FOD    │─┼────────────────────────────────────────┼►│ ocms_param   │ │
│ │ Parameter  │ │                                        │ │ FOD value    │ │
│ └─────┬──────┘ │                                        │ └──────────────┘ │
│       │        │                                        │                  │
│       ▼        │                                        │                  │
│ ┌────────────┐ │                                        │                  │
│ │ FOD Found? │ │                                        │                  │
│ └──┬─────┬───┘ │                                        │                  │
│  Yes     No    │                                        │                  │
│    │     │     │                                        │                  │
│    │     └─────┼──► Log Error, Stop ──────────────────► │ End             │
│    │           │                                        │                  │
│    ▼           │                                        │ ┌──────────────┐ │
│ ┌────────────┐ │                                        │ │ Query        │ │
│ │ Get AFO    │─┼────────────────────────────────────────┼►│ ocms_param   │ │
│ │ Parameter  │ │                                        │ │ AFO value    │ │
│ └─────┬──────┘ │                                        │ └──────────────┘ │
│       │        │                                        │                  │
│       ▼        │                                        │                  │
│ ┌────────────┐ │                                        │                  │
│ │ AFO Found? │ │                                        │                  │
│ └──┬─────┬───┘ │                                        │                  │
│  Yes     No    │                                        │                  │
│    │     │     │                                        │                  │
│    │     └─────┼──► Log Error, Stop ──────────────────► │ End             │
│    │           │                                        │                  │
│    ▼           │                                        │ ┌──────────────┐ │
│ ┌────────────┐ │                                        │ │ Query VON    │ │
│ │ Query      │─┼────────────────────────────────────────┼►│ PS-FOR,      │ │
│ │ Eligible   │ │                                        │ │ unpaid,      │ │
│ │ Notices    │ │                                        │ │ past FOD     │ │
│ └─────┬──────┘ │                                        │ └──────────────┘ │
│       │        │                                        │                  │
│       ▼        │                                        │                  │
│ ┌────────────┐ │                                        │                  │
│ │ Any        │ │                                        │                  │
│ │ Eligible?  │ │                                        │                  │
│ └──┬─────┬───┘ │                                        │                  │
│  Yes     No    │                                        │                  │
│    │     │     │                                        │                  │
│    │     └─────┼──► Log No Records ───────────────────► │ End             │
│    │           │                                        │                  │
│    ▼           │                                        │ ┌──────────────┐ │
│ ┌────────────┐ │                                        │ │ Batch Update │ │
│ │ Batch      │─┼────────────────────────────────────────┼►│ VON:         │ │
│ │ Update     │ │                                        │ │ admin_fee    │ │
│ │ Admin Fee  │ │                                        │ │ += AFO       │ │
│ └─────┬──────┘ │                                        │ └──────────────┘ │
│       │        │                                        │                  │
│       ▼        │                                        │                  │
│ ┌────────────┐ │                                        │                  │
│ │ Prepare    │ │                                        │                  │
│ │ for vHub   │ │                                        │                  │
│ │ (Status O) │ │                                        │                  │
│ └─────┬──────┘ │                                        │                  │
│       │        │                                        │                  │
│       ▼        │                                        │                  │
│ ┌────────────┐ │                                        │                  │
│ │    End     │ │                                        │                  │
│ └────────────┘ │                                        │                  │
│                │                                        │                  │
└────────────────┴────────────────────────────────────────┴──────────────────┘
```

### 15.3 Process Steps Table

| Step | Type | Definition | Brief Description | Next Step |
| --- | --- | --- | --- | --- |
| 1 | Start | CRON Start | Job triggers | 2 |
| 2 | Process | Get FOD Parameter | Query FOD value (days) | 3 |
| 3 | Decision | FOD Found? | Check parameter exists | Yes→4, No→E1 |
| 4 | Process | Get AFO Parameter | Query AFO value (amount) | 5 |
| 5 | Decision | AFO Found? | Check parameter exists | Yes→6, No→E2 |
| 6 | Process | Query Eligible | Query FOR notices past FOD | 7 |
| 7 | Decision | Any Eligible? | Check records found | Yes→8, No→End |
| 8 | Process | Batch Update | Add AFO to composition | 9 |
| 9 | Process | Prepare for vHub | Mark for vHub as 'O' | End |
| E1 | Error | FOD Not Found | Log error, stop | End |
| E2 | Error | AFO Not Found | Log error, stop | End |

### 15.4 Database Operations

| Step | Operation | Table | Action |
| --- | --- | --- | --- |
| 2 | SELECT | ocms_parameter | Get FOD value |
| 4 | SELECT | ocms_parameter | Get AFO value |
| 6 | SELECT | ocms_valid_offence_notice | Query eligible notices |
| 8 | UPDATE | ocms_valid_offence_notice | Add admin_fee, update amount_payable |

### 15.5 Query Conditions

```sql
SELECT *
FROM ocms_valid_offence_notice
WHERE suspension_type = 'PS'
AND epr_reason_of_suspension = 'FOR'
AND amount_paid = 0
AND offence_date_and_time <= DATEADD(day, -@FOD_days, GETDATE())
AND (admin_fee IS NULL OR admin_fee = 0)
```

---

## 16. Error Handling Summary

| Tab | Error Point | Error Type | Handling | Recovery |
| --- | --- | --- | --- | --- |
| 2,3 | Get FOR Param | DB Error | Retry 1x | Log, send email, stop |
| 2 | vHub API Call | API Timeout | Retry 2x | Log error, continue next batch |
| 2 | vHub API Call | API Error | Log per record | Add to error report |
| 4,5 | Generate XML | Gen Error | - | Log, send email |
| 4,5 | **SLIFT Encrypt** | Encrypt Error | Retry 3x | Log, send email, abort transfer |
| 4,5 | Azure Upload | Upload Error | Retry | Log, send email |
| 4,5 | SFTP Upload | SFTP Error | Retry | Log, send email |
| 6,7 | Download ACK | SFTP Error | Retry | Log, send email |
| 6,7 | **SLIFT Decrypt** | Decrypt Error | Retry 3x | Log, send email, mark file failed |
| 8,9 | **SLIFT Decrypt** | Decrypt Error | Retry 3x | Log, send email, mark file failed |
| 10,12 | **SLIFT Encrypt** | Encrypt Error | Retry 3x | Log, send email, abort transfer |
| 10,12 | Azure Upload | Upload Error | Retry | Log, send email |
| 10,12 | SFTP Upload | SFTP Error | Retry | Log, send email |
| 13 | Get FOD/AFO | Not Found | - | Log, stop processing |

---

## 17. Flowchart Checklist

Before creating the technical flowchart:

- [x] All tabs/sections are defined (13 tabs)
- [x] All steps have clear names
- [x] All decision points have Yes/No paths defined
- [x] All paths lead to an End point
- [x] Error handling paths are included
- [x] Database operations are identified
- [x] Swimlanes are defined for each system/tier
- [x] Color coding is specified
- [x] Step descriptions are complete
- [x] External system calls are documented
- [x] Sub-flows are documented

---

## 18. Notes for Technical Flowchart Creation

### 18.1 Shape Guidelines

| Element | Shape | Style |
| --- | --- | --- |
| Start/End | Terminator | strokeWidth=2; shape=mxgraph.flowchart.terminator |
| Process | Rectangle | rounded=1; arcSize=14; strokeWidth=2 |
| Decision | Diamond | shape=mxgraph.flowchart.decision |
| Database | Cylinder | shape=cylinder |
| External Call | Rectangle | double border |
| Sub-flow Reference | Dashed Rectangle | dashed=1; strokeColor=#3333FF |

### 18.2 Connector Guidelines

| Connection Type | Style |
| --- | --- |
| Normal flow | Solid arrow |
| Database operation | Dashed line |
| External system call | Dashed line |
| Error path | Red solid arrow |

### 18.3 Swimlane Colors

| Swimlane | Fill Color | Stroke Color |
| --- | --- | --- |
| OCMS Backend/CRON | #dae8fc | #6c8ebf |
| OCMS Admin API | #d5e8d4 | #82b366 |
| Database | #fff2cc | #d6b656 |
| **SLIFT Service** | **#f8cecc** | **#b85450** |
| External System | #ffe6cc | #d79b00 |
| Azure | #e1d5e7 | #9673a6 |

---

## 19. Changelog

| Version | Date | Author | Changes |
| --- | --- | --- | --- |
| 1.0 | 15/01/2026 | Claude | Initial version based on plan_api.md and plan_condition.md |
| 2.0 | 27/01/2026 | Claude | Added SLIFT swimlane and integration steps to all SFTP flows. Updated: vHub SFTP (encrypt), vHub ACK (decrypt), vHub NTL (decrypt), REPCCS (encrypt), CES EHT (encrypt). Added SLIFT error handling. Aligned with Functional Flowchart and Data Dictionary. |

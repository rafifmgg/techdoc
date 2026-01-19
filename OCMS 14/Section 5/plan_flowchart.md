# Flowchart Plan: Diplomatic Vehicle Notice Processing

## Overview

| Attribute | Value |
| --- | --- |
| Feature Name | Notice Processing Flow for Diplomatic Vehicles |
| Version | v1.0 |
| Author | Claude |
| Created Date | 15/01/2026 |
| Last Updated | 15/01/2026 |
| Status | Draft |
| FD Reference | OCMS 14 - Section 6 |
| TD Reference | OCMS 14 - Section 5 |

---

## 1. Diagram Sections (Tabs)

The Technical Flowchart will contain the following tabs/sections:

| Tab # | Tab Name | Description | FD Reference | Priority |
| --- | --- | --- | --- | --- |
| 1 | Section_5_High_Level | High-level overview of DIP Notice processing | 6.2.1 | High |
| 2 | Section_5_DIP_Type_OE | Detailed flow for Type O & E Diplomatic notices | 6.2.2 | High |
| 3 | Section_5_DIP_AN_Subflow | Advisory Notice sub-flow for DIP | 6.2.4 | Medium |
| 4 | Section_5_DIP_Furnish_Subflow | Furnish Driver/Hirer sub-flow | 6.2.5 | Medium |
| 5 | Section_5_PS_DIP_Suspension | PS-DIP suspension CRON process | 6.7 | High |

---

## 2. Systems Involved (Swimlanes)

| System/Tier | Color Code | Hex | Description |
| --- | --- | --- | --- |
| Staff Portal / PLUS Portal | Light Blue | #dae8fc | Frontend applications |
| OCMS Admin API (Intranet) | Light Green | #d5e8d4 | Backend API processing |
| CRON Service | Light Green | #d5e8d4 | Scheduled jobs |
| Database (Intranet) | Light Yellow | #fff2cc | Intranet database operations |
| Database (Internet) | Light Yellow | #fff2cc | Internet database operations |
| External System (LTA) | Light Orange | #ffe6cc | LTA VRLS API |
| External System (MHA) | Light Orange | #ffe6cc | MHA API |
| External System (DataHive) | Light Orange | #ffe6cc | DataHive API |
| External System (TOPPAN) | Light Orange | #ffe6cc | Letter printing vendor |

---

## 3. Tab 1: Section_5_High_Level

### 3.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | Diplomatic Vehicle Notice Processing - High Level |
| Section | 5.1 |
| Trigger | Notice created with vehicle_registration_type = 'D' |
| Frequency | Real-time (per notice creation) |
| Systems Involved | Backend API, LTA, MHA, DataHive, TOPPAN, Database |
| Expected Outcome | Notice processed up to RD2/DN2, then suspended with PS-DIP |

### 3.2 High Level Flow Diagram (ASCII)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    DIPLOMATIC VEHICLE NOTICE PROCESSING                          │
│                              HIGH LEVEL FLOW                                     │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│    ┌───────────────┐                                                             │
│    │     Start     │                                                             │
│    │ (Type D Notice)│                                                            │
│    └───────┬───────┘                                                             │
│            │                                                                     │
│            ▼                                                                     │
│    ┌───────────────────────────────────────────────────────────────────────┐    │
│    │              PRE-CONDITIONS                                            │    │
│    │  • Validate Data Format                                                │    │
│    │  • Check Duplicate Notice Number                                       │    │
│    │  • Identify Offence Type (O, E, or U)                                 │    │
│    └───────────────────────────────────────────────────────────────────────┘    │
│            │                                                                     │
│            ▼                                                                     │
│    ┌───────────────┐                                                             │
│    │ Offence Type? │                                                             │
│    └───────┬───────┘                                                             │
│       O/E  │  U                                                                  │
│      ┌─────┴─────┐                                                               │
│      │           │                                                               │
│      ▼           ▼                                                               │
│ ┌─────────────┐  ┌─────────────┐                                                │
│ │ Type O & E  │  │  Type U     │                                                │
│ │ Workflow    │  │ (UPL - KIV) │                                                │
│ │ (Tab 2)     │  │             │                                                │
│ └──────┬──────┘  └─────────────┘                                                │
│        │                                                                         │
│        ▼                                                                         │
│ ┌─────────────────────────────────────────────────────────────────────────┐     │
│ │                    TYPE O & E PROCESSING STAGES                         │     │
│ │                                                                          │     │
│ │   NPA ──► ROV ──► RD1 ──► RD2 ──► PS-DIP (if outstanding)              │     │
│ │    │      │       │       │              │                               │     │
│ │    │      │       │       │              │ Payment → PS-FP              │     │
│ │    │      │       │       │              │                               │     │
│ │    │   LTA/MHA/   │       │              │ Furnish → DN1 → DN2 → PS-DIP │     │
│ │    │   DataHive   │       │              │                               │     │
│ │                                                                          │     │
│ │   Key Differences from Standard Flow:                                    │     │
│ │   • ENA Stage: BYPASSED (no SMS/email)                                  │     │
│ │   • LTA/MHA/DataHive: INCLUDED (not bypassed like MID)                  │     │
│ │   • End Stage: PS-DIP (not Court)                                       │     │
│ └─────────────────────────────────────────────────────────────────────────┘     │
│        │                                                                         │
│        ▼                                                                         │
│    ┌───────────┐                                                                 │
│    │    End    │                                                                 │
│    └───────────┘                                                                 │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 3.3 Process Steps Table (High Level)

| Step | Definition | Brief Description |
| --- | --- | --- |
| Start | Entry point | Diplomatic Vehicle Notice (Type D) is created |
| Pre-conditions | Validation | Validate data, check duplicates, identify offence type |
| Offence Type Check | Decision | Route to Type O&E workflow or Type U (UPL - KIV) |
| Type O&E Workflow | Processing | NPA → ROV → RD1 → RD2 → PS-DIP |
| End | Exit point | Notice processing complete |

---

## 4. Tab 2: Section_5_DIP_Type_OE

### 4.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | Diplomatic Vehicle Notice Processing (Type O & E) |
| Section | 5.2 |
| Trigger | Notice created with type = 'D' and offence_type IN ('O', 'E') |
| Frequency | Real-time + CRON jobs |
| Systems Involved | Backend API, LTA, MHA, DataHive, TOPPAN, Intranet DB, Internet DB |
| Expected Outcome | Notice processed through all stages until PS-DIP or PS-FP |

### 4.2 Detailed Flow Diagram (ASCII)

```
┌──────────────────┬──────────────────┬──────────────────┬──────────────────┬──────────────────┐
│   Backend API    │   External APIs  │   CRON Service   │   Intranet DB    │   Internet DB    │
├──────────────────┼──────────────────┼──────────────────┼──────────────────┼──────────────────┤
│                  │                  │                  │                  │                  │
│ ┌──────────────┐ │                  │                  │                  │                  │
│ │    Start     │ │                  │                  │                  │                  │
│ │ (Type O/E)   │ │                  │                  │                  │                  │
│ └──────┬───────┘ │                  │                  │                  │                  │
│        │         │                  │                  │                  │                  │
│        ▼         │                  │                  │                  │                  │
│ ┌──────────────┐ │                  │                  │                  │                  │
│ │ Duplicate    │ │                  │                  │                  │                  │
│ │ Check?       │ │                  │                  │                  │                  │
│ └──────┬───────┘ │                  │                  │                  │                  │
│   Yes/ │ \No     │                  │                  │                  │                  │
│       │  │       │                  │                  │                  │                  │
│  ┌────┘  └────┐  │                  │                  │                  │                  │
│  ▼            ▼  │                  │                  │                  │                  │
│ PS-DBB    ┌──────────────┐         │                  │                  │                  │
│ ──►End    │Detect Type=D │         │                  │                  │                  │
│           └──────┬───────┘         │                  │                  │                  │
│                  │                  │                  │                  │                  │
│                  ▼                  │                  │                  │                  │
│           ┌──────────────┐         │                  │ ┌──────────────┐ │                  │
│           │Create Notice │         │                  │ │    INSERT    │ │                  │
│           │at NPA Stage  │─────────┼──────────────────┼─►von, owner,  │ │                  │
│           └──────┬───────┘         │                  │ │address, etc  │ │                  │
│                  │                  │                  │ └──────────────┘ │                  │
│                  ▼                  │                  │                  │                  │
│           ┌──────────────┐         │                  │                  │                  │
│           │Double Booking│         │                  │                  │                  │
│           │Check (PS-DBB)│         │                  │                  │                  │
│           └──────┬───────┘         │                  │                  │                  │
│             DBB/ │ \No             │                  │                  │                  │
│                 │  │               │                  │                  │                  │
│            ┌────┘  └────┐          │                  │                  │                  │
│            ▼            ▼          │                  │                  │                  │
│         PS-DBB    ┌──────────────┐ │                  │                  │                  │
│          ──►End   │AN Qualified? │ │                  │                  │                  │
│                   └──────┬───────┘ │                  │                  │                  │
│                     Yes/ │ \No     │                  │                  │                  │
│                         │  │       │                  │                  │                  │
│                    ┌────┘  └────┐  │                  │                  │                  │
│                    ▼            ▼  │                  │                  │                  │
│              AN Sub-flow   ┌──────────────────────────┐                  │                  │
│              (Tab 3)       │                          │                  │                  │
│               ──►PS-ANS    │  ROV Stage Processing    │                  │                  │
│                            │                          │                  │                  │
│                            │ ┌──────────────┐         │                  │                  │
│                            │ │Query LTA VRLS│◄────────┼──────────────────┼──────────────────┤
│                            │ └──────┬───────┘         │                  │                  │
│                            │        │                 │                  │                  │
│                            │        ▼                 │                  │                  │
│                            │ ┌──────────────┐         │                  │                  │
│                            │ │Query MHA     │◄────────┼──────────────────┼──────────────────┤
│                            │ └──────┬───────┘         │                  │                  │
│                            │        │                 │                  │                  │
│                            │        ▼                 │                  │                  │
│                            │ ┌──────────────┐         │                  │                  │
│                            │ │Query DataHive│◄────────┼──────────────────┼──────────────────┤
│                            │ └──────┬───────┘         │                  │                  │
│                            │        │                 │                  │                  │
│                            └────────┼─────────────────┘                  │                  │
│                                     │                  │                  │                  │
│                                     ▼                  │                  │                  │
│                            ┌──────────────┐            │ ┌──────────────┐ │ ┌──────────────┐ │
│                            │Update Owner  │            │ │UPDATE owner  │ │ │    SYNC      │ │
│                            │Address Info  │────────────┼─►address table │ │ │  to Internet │ │
│                            └──────┬───────┘            │ └──────────────┘ │ └──────────────┘ │
│                                   │                    │                  │                  │
│                                   │         ┌─────────────────────────────┘                  │
│                                   │         │                                                │
│                                   ▼         ▼                                                │
│                            ┌─────────────────────────────┐                                   │
│                            │                             │                                   │
│                            │   CRON: Prepare for RD1     │                                   │
│                            │   • Generate RD1 Letter     │                                   │
│                            │   • Send to TOPPAN (SFTP)   │──────────────────────►TOPPAN      │
│                            │   • Update stage to RD1     │                                   │
│                            │                             │                                   │
│                            └─────────────┬───────────────┘                                   │
│                                          │                                                   │
│                                          ▼                                                   │
│                            ┌─────────────────────────────┐                                   │
│                            │       Notice at RD1         │                                   │
│                            │                             │                                   │
│                            │   Owner can:                │                                   │
│                            │   • Make Payment → PS-FP    │                                   │
│                            │   • Furnish Driver (Tab 4)  │                                   │
│                            │                             │                                   │
│                            └─────────────┬───────────────┘                                   │
│                                          │                                                   │
│                                          ▼                                                   │
│                            ┌─────────────────────────────┐                                   │
│                            │  MHA/DataHive Re-check      │                                   │
│                            │  (Update address if changed)│                                   │
│                            └─────────────┬───────────────┘                                   │
│                                          │                                                   │
│                                          ▼                                                   │
│                            ┌─────────────────────────────┐                                   │
│                            │                             │                                   │
│                            │   CRON: Prepare for RD2     │                                   │
│                            │   • Generate RD2 Letter     │                                   │
│                            │   • Send to TOPPAN (SFTP)   │──────────────────────►TOPPAN      │
│                            │   • Update stage to RD2     │                                   │
│                            │                             │                                   │
│                            └─────────────┬───────────────┘                                   │
│                                          │                                                   │
│                                          ▼                                                   │
│                            ┌─────────────────────────────┐                                   │
│                            │       Notice at RD2         │                                   │
│                            │                             │                                   │
│                            │   Owner can:                │                                   │
│                            │   • Make Payment → PS-FP    │                                   │
│                            │   • Furnish Driver (Tab 4)  │                                   │
│                            │                             │                                   │
│                            └─────────────┬───────────────┘                                   │
│                                          │                                                   │
│                                          ▼                                                   │
│                            ┌─────────────────────────────┐                                   │
│                            │  Outstanding at End of RD2? │                                   │
│                            └─────────────┬───────────────┘                                   │
│                                     Yes/ │ \No (Paid)                                        │
│                                         │  │                                                 │
│                                    ┌────┘  └────┐                                            │
│                                    ▼            ▼                                            │
│                            ┌──────────────┐  ┌──────────────┐                                │
│                            │ Apply PS-DIP │  │ Apply PS-FP  │                                │
│                            │ (Tab 5)      │  │              │                                │
│                            └──────┬───────┘  └──────┬───────┘                                │
│                                   │                 │                                        │
│                                   └────────┬────────┘                                        │
│                                            │                                                 │
│                                            ▼                                                 │
│                                   ┌──────────────┐                                           │
│                                   │     End      │                                           │
│                                   └──────────────┘                                           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

### 4.3 Process Steps Table (Detailed)

| Step | Type | Definition | Brief Description | Next Step |
| --- | --- | --- | --- | --- |
| 1 | Start | Start Type O/E Processing | Notice with type O or E received | 2 |
| 2 | Decision | Duplicate Check? | Check for duplicate notice details | Yes→3, No→4 |
| 3 | Process | Apply PS-DBB | Suspend duplicate notice with PS-DBB | End |
| 4 | Process | Detect Vehicle Type D | Confirm vehicle registration type = 'D' | 5 |
| 5 | Process | Create Notice at NPA | Insert notice with stage NPA, next stage ROV | 6 |
| 6 | Decision | Double Booking? | Check for double booking after creation | Yes→7, No→8 |
| 7 | Process | Apply PS-DBB | Suspend notice with PS-DBB | End |
| 8 | Decision | AN Qualified? | Check if Type O qualifies for Advisory Notice | Yes→9, No→10 |
| 9 | Process | AN Sub-flow | Process via Advisory Notice flow (Tab 3) | PS-ANS→End |
| 10 | Process | Query LTA VRLS | Get vehicle ownership details | 11 |
| 11 | Process | Query MHA | Get owner address from MHA | 12 |
| 12 | Process | Query DataHive | Get additional owner info from DataHive | 13 |
| 13 | Process | Update Owner/Address | Store owner and address in database | 14 |
| 14 | Process | CRON: Prepare RD1 | Generate RD1 letter, send to TOPPAN, update stage | 15 |
| 15 | Process | Notice at RD1 | Owner can pay or furnish driver | 16 |
| 16 | Process | MHA/DataHive Re-check | Re-check address before RD2 | 17 |
| 17 | Process | CRON: Prepare RD2 | Generate RD2 letter, send to TOPPAN, update stage | 18 |
| 18 | Process | Notice at RD2 | Owner can pay or furnish driver | 19 |
| 19 | Decision | Outstanding at End? | Check if notice still unpaid at end of RD2 | Yes→20, No→21 |
| 20 | Process | Apply PS-DIP | Suspend outstanding notice with PS-DIP (Tab 5) | End |
| 21 | Process | Apply PS-FP | Notice paid, apply PS-FP | End |
| End | End | End | Notice processing complete | - |

### 4.4 Decision Logic

| ID | Decision | Input | Condition | Yes Action | No Action |
| --- | --- | --- | --- | --- | --- |
| D1 | Duplicate Check? | Notice details | Existing notice with same veh_no, date, rule, lot, pp_code | Apply PS-DBB | Continue |
| D2 | Double Booking? | Notice after creation | Same vehicle, same date/time | Apply PS-DBB | Continue |
| D3 | AN Qualified? | offence_type, AN criteria | Type O AND meets AN criteria | AN Sub-flow | ROV Stage |
| D4 | Outstanding at End? | crs_reason_of_suspension | NULL (not paid) | Apply PS-DIP | Apply PS-FP |

### 4.5 Database Operations

| Step | Operation | Database | Table | Fields |
| --- | --- | --- | --- | --- |
| 5 | INSERT | Intranet | ocms_valid_offence_notice | notice_no, vehicle_registration_type='D', last_stage='NPA', next_stage='ROV' |
| 5 | INSERT | Intranet | ocms_offence_notice_owner_driver | Owner details from LTA/MHA/DH |
| 5 | INSERT | Intranet | ocms_offence_notice_owner_driver_addr | Address details |
| 5 | INSERT | Internet | eocms_valid_offence_notice | Sync from Intranet |
| 13 | UPDATE | Intranet | ocms_offence_notice_owner_driver | Updated owner info |
| 13 | UPDATE | Intranet | ocms_offence_notice_owner_driver_addr | Updated address |
| 14 | UPDATE | Intranet | ocms_valid_offence_notice | last_stage='ROV'→'RD1', next_stage='RD2' |
| 17 | UPDATE | Intranet | ocms_valid_offence_notice | last_stage='RD1'→'RD2', next_stage='RR3' |
| 20 | UPDATE | Intranet | ocms_valid_offence_notice | suspension_type='PS', epr_reason='DIP' |
| 20 | INSERT | Intranet | ocms_suspended_notice | Suspension record |

### 4.6 External System Calls

| Step | System | Call | Input | Output |
| --- | --- | --- | --- | --- |
| 10 | LTA VRLS | Query Vehicle | vehicle_no | Owner details, vehicle info |
| 11 | MHA | Query ID | id_type, id_no | Name, address, life_status |
| 12 | DataHive | Query Profile | id_type, id_no | FIN details, company profile |
| 14, 17 | TOPPAN | SFTP Upload | Letter file | Upload confirmation |

---

## 5. Tab 3: Section_5_DIP_AN_Subflow

### 5.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | Diplomatic Vehicle Advisory Notice Sub-flow |
| Section | 5.3 |
| Trigger | Type O notice qualifies for Advisory Notice |
| Frequency | Per qualifying notice |
| Systems Involved | Backend API, TOPPAN, Database |
| Expected Outcome | AN letter sent, notice suspended with PS-ANS |

### 5.2 Flow Diagram (ASCII)

```
┌──────────────────────────────────────────────────────────────────────┐
│                    DIP ADVISORY NOTICE SUB-FLOW                       │
├──────────────────────────────────────────────────────────────────────┤
│                                                                       │
│    ┌───────────────┐                                                  │
│    │     Start     │                                                  │
│    │ (AN Qualified)│                                                  │
│    └───────┬───────┘                                                  │
│            │                                                          │
│            ▼                                                          │
│    ┌───────────────┐                                                  │
│    │ Set an_flag   │                                                  │
│    │ = 'Y'         │                                                  │
│    └───────┬───────┘                                                  │
│            │                                                          │
│            ▼                                                          │
│    ┌───────────────┐                                                  │
│    │Double Booking │                                                  │
│    │Check?         │                                                  │
│    └───────┬───────┘                                                  │
│       DBB/ │ \No                                                      │
│           │  │                                                        │
│      ┌────┘  └────┐                                                   │
│      ▼            ▼                                                   │
│   PS-DBB    ┌───────────────┐                                         │
│   ──►End    │Update next    │                                         │
│             │stage to RD1   │                                         │
│             └───────┬───────┘                                         │
│                     │                                                 │
│                     ▼                                                 │
│             ┌───────────────┐                                         │
│             │ CRON: Generate│                                         │
│             │ AN Letter     │                                         │
│             └───────┬───────┘                                         │
│                     │                                                 │
│                     ▼                                                 │
│             ┌───────────────┐                                         │
│             │ Send to TOPPAN│                                         │
│             │ via SFTP      │──────────────────────────►TOPPAN        │
│             └───────┬───────┘                                         │
│                     │                                                 │
│                     ▼                                                 │
│             ┌───────────────┐                                         │
│             │ Apply PS-ANS  │                                         │
│             │ Suspension    │                                         │
│             └───────┬───────┘                                         │
│                     │                                                 │
│                     ▼                                                 │
│             ┌───────────────┐                                         │
│             │     End       │                                         │
│             └───────────────┘                                         │
│                                                                       │
└──────────────────────────────────────────────────────────────────────┘
```

### 5.3 Process Steps Table

| Step | Type | Definition | Brief Description | Next Step |
| --- | --- | --- | --- | --- |
| 1 | Start | Start AN Sub-flow | Notice qualifies for Advisory Notice | 2 |
| 2 | Process | Set an_flag = 'Y' | Mark notice as Advisory Notice | 3 |
| 3 | Decision | Double Booking? | Check for duplicate booking | Yes→4, No→5 |
| 4 | Process | Apply PS-DBB | Suspend with double booking | End |
| 5 | Process | Update next stage | Set next_processing_stage = 'RD1' | 6 |
| 6 | Process | CRON: Generate AN Letter | Scheduled job generates AN letter | 7 |
| 7 | Process | Send to TOPPAN | Upload letter file via SFTP | 8 |
| 8 | Process | Apply PS-ANS | Suspend notice with PS-ANS | End |
| End | End | End | AN sub-flow complete | - |

---

## 6. Tab 4: Section_5_DIP_Furnish_Subflow

### 6.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | Furnish Driver/Hirer for Diplomatic Vehicle |
| Section | 5.4 |
| Trigger | Vehicle Owner submits Driver/Hirer details via eService |
| Frequency | Per furnish request |
| Systems Involved | Internet Portal, Backend API, MHA, DataHive, Database |
| Expected Outcome | Notice redirected to Driver/Hirer, stage changed to DN1 |

### 6.2 Flow Diagram (ASCII)

```
┌──────────────────────────────────────────────────────────────────────┐
│                    FURNISH DRIVER/HIRER SUB-FLOW                      │
├──────────────────────────────────────────────────────────────────────┤
│                                                                       │
│    ┌───────────────────────────────────────────────────────────────┐  │
│    │                    eService Portal (Internet)                  │  │
│    ├───────────────────────────────────────────────────────────────┤  │
│    │                                                                │  │
│    │    ┌───────────────┐                                           │  │
│    │    │     Start     │                                           │  │
│    │    │ (Owner Submit)│                                           │  │
│    │    └───────┬───────┘                                           │  │
│    │            │                                                   │  │
│    │            ▼                                                   │  │
│    │    ┌───────────────┐                                           │  │
│    │    │ Submit Furnish│                                           │  │
│    │    │ Application   │                                           │  │
│    │    └───────┬───────┘                                           │  │
│    │            │                                                   │  │
│    │            ▼                                                   │  │
│    │    ┌───────────────┐         ┌──────────────────────────────┐  │  │
│    │    │ Store in      │─────────►│ eocms_furnish_application   │  │  │
│    │    │ Internet DB   │         │ (furnish pending approval)   │  │  │
│    │    └───────────────┘         └──────────────────────────────┘  │  │
│    │                                                                │  │
│    └───────────────────────────────────────────────────────────────┘  │
│                                   │                                   │
│                                   ▼                                   │
│    ┌───────────────────────────────────────────────────────────────┐  │
│    │                    CRON Service (Intranet)                     │  │
│    ├───────────────────────────────────────────────────────────────┤  │
│    │                                                                │  │
│    │    ┌───────────────┐                                           │  │
│    │    │ Sync Furnish  │                                           │  │
│    │    │ to Intranet   │                                           │  │
│    │    └───────┬───────┘                                           │  │
│    │            │                                                   │  │
│    │            ▼                                                   │  │
│    │    ┌───────────────┐                                           │  │
│    │    │ Validate via  │                                           │  │
│    │    │ MHA/DataHive  │──────────────────────────►MHA/DataHive    │  │
│    │    └───────┬───────┘                                           │  │
│    │            │                                                   │  │
│    │            ▼                                                   │  │
│    │    ┌───────────────┐                                           │  │
│    │    │ Auto-approve? │                                           │  │
│    │    └───────┬───────┘                                           │  │
│    │       Yes/ │ \No                                               │  │
│    │           │  │                                                 │  │
│    │      ┌────┘  └────┐                                            │  │
│    │      ▼            ▼                                            │  │
│    │ ┌───────────┐  ┌───────────────┐                               │  │
│    │ │ Approve   │  │ OIC Manual    │                               │  │
│    │ │           │  │ Review        │                               │  │
│    │ └─────┬─────┘  └───────┬───────┘                               │  │
│    │       │           Approve/│\Reject                             │  │
│    │       │                  │  │                                  │  │
│    │       │             ┌────┘  └────┐                             │  │
│    │       │             ▼            ▼                             │  │
│    │       │        ┌─────────┐  ┌─────────────┐                    │  │
│    │       └───────►│ Create  │  │ Remove      │                    │  │
│    │                │ Driver/ │  │ Pending     │                    │  │
│    │                │ Hirer   │  │ Status      │                    │  │
│    │                │ Record  │  └──────┬──────┘                    │  │
│    │                └────┬────┘         │                           │  │
│    │                     │              │                           │  │
│    │                     ▼              │                           │  │
│    │                ┌───────────┐       │                           │  │
│    │                │ Change    │       │                           │  │
│    │                │ next stage│       │                           │  │
│    │                │ to DN1    │       │                           │  │
│    │                └─────┬─────┘       │                           │  │
│    │                      │             │                           │  │
│    │                      └──────┬──────┘                           │  │
│    │                             │                                  │  │
│    │                             ▼                                  │  │
│    │                      ┌───────────┐                             │  │
│    │                      │    End    │                             │  │
│    │                      └───────────┘                             │  │
│    │                                                                │  │
│    └───────────────────────────────────────────────────────────────┘  │
│                                                                       │
│    Note: Furnish is NOT allowed when notice has PS-DIP suspension     │
│                                                                       │
└──────────────────────────────────────────────────────────────────────┘
```

### 6.3 Furnish Allowed Stages

| Stage | Furnish Allowed | Notes |
| --- | --- | --- |
| NPA | No | Too early |
| ROV | Yes | After LTA check |
| RD1 | Yes | First reminder sent |
| RD2 | Yes | Second reminder sent |
| DN1 | Yes | Already furnished, can re-furnish |
| DN2 | Yes | Already furnished, can re-furnish |
| **PS-DIP** | **No** | Notice suspended, cannot furnish |

---

## 7. Tab 5: Section_5_PS_DIP_Suspension

### 7.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | PS-DIP Suspension CRON Process |
| Section | 5.5 |
| Trigger | CRON job runs at end of RD2/DN2 stage |
| Frequency | Daily (before Prepare for RR3/DR3 job) |
| Systems Involved | CRON Service, Intranet DB, Internet DB |
| Expected Outcome | Outstanding DIP notices suspended with PS-DIP |

### 7.2 Flow Diagram (ASCII)

```
┌──────────────────────────────────────────────────────────────────────┐
│                    PS-DIP SUSPENSION CRON PROCESS                     │
├──────────────────────────────────────────────────────────────────────┤
│                                                                       │
│    ┌───────────────────────────────────────────────────────────────┐  │
│    │                    CRON Service                                │  │
│    ├───────────────────────────────────────────────────────────────┤  │
│    │                                                                │  │
│    │    ┌───────────────┐                                           │  │
│    │    │  CRON Start   │                                           │  │
│    │    │ (End of RD2/  │                                           │  │
│    │    │  DN2 stage)   │                                           │  │
│    │    └───────┬───────┘                                           │  │
│    │            │                                                   │  │
│    │            ▼                                                   │  │
│    │    ┌───────────────────────────────────────────────────────┐   │  │
│    │    │ Query ocms_valid_offence_notice                        │   │  │
│    │    │                                                        │   │  │
│    │    │ WHERE vehicle_registration_type = 'D'                  │   │  │
│    │    │   AND crs_reason_of_suspension IS NULL                 │   │  │
│    │    │   AND last_processing_stage IN ('RD2', 'DN2')          │   │  │
│    │    │   AND next_processing_stage IN ('RR3', 'DR3')          │   │  │
│    │    └───────────────────────────────────────────────────────┘   │  │
│    │            │                                                   │  │
│    │            ▼                                                   │  │
│    │    ┌───────────────┐                                           │  │
│    │    │ Records Found?│                                           │  │
│    │    └───────┬───────┘                                           │  │
│    │       Yes/ │ \No                                               │  │
│    │           │  │                                                 │  │
│    │      ┌────┘  └────────────────────────────────────┐            │  │
│    │      │                                            │            │  │
│    │      ▼                                            ▼            │  │
│    │ ┌─────────────────┐                        ┌───────────┐       │  │
│    │ │ FOR EACH notice │                        │ Log: No   │       │  │
│    │ └────────┬────────┘                        │ Records   │       │  │
│    │          │                                 └─────┬─────┘       │  │
│    │          ▼                                       │             │  │
│    │ ┌─────────────────────────────────────┐         │             │  │
│    │ │ Suspend Notice with PS-DIP           │         │             │  │
│    │ │                                      │         │             │  │
│    │ │ UPDATE ocms_valid_offence_notice    │         │             │  │
│    │ │ SET suspension_type = 'PS'          │         │             │  │
│    │ │     epr_reason_of_suspension = 'DIP'│         │             │  │
│    │ │     epr_date_of_suspension = NOW()  │         │             │  │
│    │ └────────────────┬────────────────────┘         │             │  │
│    │                  │                              │             │  │
│    │                  ▼                              │             │  │
│    │ ┌─────────────────────────────────────┐         │             │  │
│    │ │ INSERT into ocms_suspended_notice    │         │             │  │
│    │ │                                      │         │             │  │
│    │ │ • notice_no                          │         │             │  │
│    │ │ • suspension_type = 'PS'             │         │             │  │
│    │ │ • reason_of_suspension = 'DIP'       │         │             │  │
│    │ │ • date_of_suspension = NOW()         │         │             │  │
│    │ │ • suspension_source = 'ocmsiz_app_conn'│       │             │  │
│    │ │ • sr_no = running number             │         │             │  │
│    │ │ • cre_user_id = 'ocmsiz_app_conn'    │         │             │  │
│    │ └────────────────┬────────────────────┘         │             │  │
│    │                  │                              │             │  │
│    │                  ▼                              │             │  │
│    │ ┌─────────────────────────────────────┐         │             │  │
│    │ │ SYNC to Internet                     │         │             │  │
│    │ │                                      │         │             │  │
│    │ │ UPDATE eocms_valid_offence_notice   │         │             │  │
│    │ │ SET suspension_type = 'PS'          │         │             │  │
│    │ │     epr_reason_of_suspension = 'DIP'│         │             │  │
│    │ │     epr_date_of_suspension = NOW()  │         │             │  │
│    │ └────────────────┬────────────────────┘         │             │  │
│    │                  │                              │             │  │
│    │                  └───────────────┬──────────────┘             │  │
│    │                                  │                            │  │
│    │                                  ▼                            │  │
│    │                           ┌───────────┐                       │  │
│    │                           │    End    │                       │  │
│    │                           └───────────┘                       │  │
│    │                                                                │  │
│    └───────────────────────────────────────────────────────────────┘  │
│                                                                       │
└──────────────────────────────────────────────────────────────────────┘
```

### 7.3 Process Steps Table

| Step | Type | Definition | Brief Description | Next Step |
| --- | --- | --- | --- | --- |
| 1 | Start | CRON Start | Scheduled job triggered at end of RD2/DN2 | 2 |
| 2 | Process | Query VON | Query for eligible DIP notices | 3 |
| 3 | Decision | Records Found? | Check if any notices match criteria | Yes→4, No→7 |
| 4 | Process | Suspend with PS-DIP | Update VON with suspension details | 5 |
| 5 | Process | Insert Suspension Record | Create record in ocms_suspended_notice | 6 |
| 6 | Process | Sync to Internet | Update eocms_valid_offence_notice | End |
| 7 | Process | Log No Records | Log that no records found | End |
| End | End | End | CRON job complete | - |

### 7.4 Database Operations

| Step | Operation | Database | Table | Fields |
| --- | --- | --- | --- | --- |
| 2 | SELECT | Intranet | ocms_valid_offence_notice | Query criteria as above |
| 4 | UPDATE | Intranet | ocms_valid_offence_notice | suspension_type, epr_reason_of_suspension, epr_date_of_suspension |
| 5 | INSERT | Intranet | ocms_suspended_notice | notice_no, suspension_type, reason_of_suspension, date_of_suspension, suspension_source, sr_no, cre_user_id, cre_date |
| 6 | UPDATE | Internet | eocms_valid_offence_notice | suspension_type, epr_reason_of_suspension, epr_date_of_suspension |

---

## 8. Error Handling

| Tab | Error Point | Error Type | Condition | Handling | Recovery |
| --- | --- | --- | --- | --- | --- |
| Tab 2 | LTA Query | External API Error | LTA service unavailable | Retry 3 times | Continue with available data |
| Tab 2 | MHA Query | External API Error | MHA service unavailable | Retry 3 times | Continue with LTA data |
| Tab 2 | DataHive Query | External API Error | DataHive unavailable | Retry 3 times | Continue with available data |
| Tab 2 | TOPPAN SFTP | External System Error | SFTP upload failed | Retry 3 times | Send alert email |
| Tab 5 | DB Update | Database Error | Update failed | Rollback transaction | Log and retry |
| Tab 4 | Furnish Validation | Business Error | Invalid driver details | Reject furnish | OIC manual review |

---

## 9. Data Mapping Summary

### 9.1 Key Database Fields

| Field | Table | Sample Value | Description |
| --- | --- | --- | --- |
| vehicle_registration_type | ocms_valid_offence_notice | 'D' | Diplomatic |
| suspension_type | ocms_valid_offence_notice | 'PS' | Permanent Suspension |
| epr_reason_of_suspension | ocms_valid_offence_notice | 'DIP' | Diplomatic Vehicle |
| last_processing_stage | ocms_valid_offence_notice | 'RD2' | Current stage |
| next_processing_stage | ocms_valid_offence_notice | 'RR3' | Next stage |
| an_flag | ocms_valid_offence_notice | 'Y'/'N' | Advisory Notice flag |

### 9.2 Audit Fields

| Field | Value | Used In |
| --- | --- | --- |
| cre_user_id | ocmsiz_app_conn | Intranet operations |
| cre_user_id | ocmsez_app_conn | Internet operations |
| upd_user_id | ocmsiz_app_conn / [OIC ID] | Update operations |

---

## 10. Flowchart Checklist

Before creating the technical flowchart:

- [x] All steps have clear names
- [x] All decision points have Yes/No paths defined
- [x] All paths lead to an End point
- [x] Error handling paths are included
- [x] Database operations are marked (dashed lines)
- [x] Swimlanes are defined for each system/tier
- [x] Color coding is specified
- [x] Step descriptions are complete
- [x] External system calls are documented
- [x] Differences from MID flow are highlighted
- [x] PS-DIP specific rules are documented

---

## 11. Key Differences: DIP vs MID

| Aspect | MID (Military) | DIP (Diplomatic) |
| --- | --- | --- |
| LTA Check | Bypass | **Included** |
| MHA Check | Bypass | **Included** |
| DataHive Check | Bypass | **Included** |
| Owner Address | Hardcoded MINDEF | **From LTA/MHA/DH** |
| Next Stage from NPA | RD1 | **ROV** |
| ENA Stage | Bypass | Bypass |
| Suspension Code | PS-MID | **PS-DIP** |

---

## 12. Notes for Technical Flowchart Creation

### 12.1 Shape Guidelines

| Element | Shape | Style |
| --- | --- | --- |
| Start/End | Terminator (rounded rectangle) | strokeWidth=2 |
| Process | Rectangle | rounded=1, arcSize=14 |
| Decision | Diamond | shape=mxgraph.flowchart.decision |
| Database | Cylinder | shape=cylinder |
| External Call | Rectangle with double border | shape=process |
| API Box | Rectangle | fillColor=#fff2cc, dashed |

### 12.2 Connector Guidelines

| Connection Type | Style |
| --- | --- |
| Normal flow | Solid arrow |
| Database operation | Dashed line |
| External system call | Dashed line |
| Error path | Red solid arrow |
| Sync operation | Dashed line with arrow |

### 12.3 Label Guidelines

| Decision | Yes Label | No Label |
| --- | --- | --- |
| Duplicate? | Yes (Duplicate) | No |
| Double Booking? | Yes (DBB) | No |
| AN Qualified? | Yes (AN) | No (Standard) |
| Outstanding? | Yes (Unpaid) | No (Paid) |
| Records Found? | Yes | No |

---

## 13. Changelog

| Version | Date | Author | Changes |
| --- | --- | --- | --- |
| 1.0 | 15/01/2026 | Claude | Initial version based on plan_api.md and plan_condition.md |

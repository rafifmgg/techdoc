# Flowchart Plan: Military Vehicle Notice Processing

## Overview

| Attribute | Value |
| --- | --- |
| Feature Name | Notice Processing Flow for Military Vehicles |
| Version | v1.1 |
| Author | Claude |
| Created Date | 15/01/2026 |
| Last Updated | 27/01/2026 |
| Status | Draft |
| FD Reference | OCMS 14 - Section 5 |
| TD Reference | OCMS 14 - Section 4 |

---

## 1. Diagram Sections (Tabs)

The Technical Flowchart will contain the following tabs/sections:

| Tab # | Tab Name | Description | Priority |
| --- | --- | --- | --- |
| 1 | Section_4_High_Level | High-level overview of Military Vehicle Notice Processing | High |
| 2 | Section_4_MID_Type_O_E | Detailed flow for Type O & E processing | High |
| 3 | Section_4_MID_Type_U | Type U (UPL) processing with OIC compoundability decision | High |
| 4 | Section_4_MID_AN_Subflow | Advisory Notice (AN) sub-flow for Military Vehicles | High |
| 5 | Section_4_MID_Furnish_Subflow | Furnish Driver/Hirer sub-flow | Medium |
| 6 | Section_4_MID_PS_Suspension | PS-MID Suspension flow at RD2/DN2 | High |

---

## 2. Systems Involved (Swimlanes)

| System/Tier | Color Code | Hex | Description |
| --- | --- | --- | --- |
| Staff Portal / PLUS Portal | Light Blue | #dae8fc | Frontend applications |
| OCMS Admin API (Intranet) | Light Green | #d5e8d4 | Backend API services |
| OCMS Cron Service | Light Green | #d5e8d4 | Scheduled jobs |
| Database (Intranet) | Light Yellow | #fff2cc | Intranet database operations |
| Database (Internet) | Light Yellow | #fff2cc | Internet database operations |
| External System (TOPPAN) | Light Yellow | #fff2cc | Letter printing vendor |
| External System (MHA/DataHive) | Light Yellow | #fff2cc | ID validation (for Furnished Driver only) |

---

## 3. Tab 1: Section_4_High_Level

### 3.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | Military Vehicle Notice Processing - High Level |
| Section | 4.1 |
| Trigger | Raw offence data received with vehicle type 'I' (Military) |
| Frequency | Real-time (per notice creation) |
| Systems Involved | Backend API, Database, CRON Jobs |
| Expected Outcome | Notice processed through RD1→RD2→PS-MID or DN1→DN2→PS-MID |

### 3.2 High Level Flow Diagram (ASCII)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│              MILITARY VEHICLE NOTICE PROCESSING - HIGH LEVEL                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                        PRE-CONDITIONS                                │    │
│  │  1. Raw Offence Data Received                                        │    │
│  │  2. Validate Data                                                    │    │
│  │  3. Check Duplicate Notice Number                                    │    │
│  │  4. Detect Vehicle Registration Type = 'I' (Military)               │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                    │                                         │
│                                    ▼                                         │
│                          ┌─────────────────┐                                │
│                          │ Identify Offence │                                │
│                          │      Type        │                                │
│                          └────────┬────────┘                                │
│                                   │                                          │
│              ┌────────────────────┼────────────────────┐                    │
│              │                    │                    │                    │
│              ▼                    ▼                    ▼                    │
│     ┌────────────────┐  ┌────────────────┐  ┌────────────────┐             │
│     │   Type O & E   │  │    Type U      │  │                │             │
│     │   Workflow     │  │   Workflow     │  │                │             │
│     │                │  │    (KIV)       │  │                │             │
│     └───────┬────────┘  └────────────────┘  └────────────────┘             │
│             │                                                               │
│             ▼                                                               │
│     ┌────────────────┐                                                      │
│     │ End of RD2/DN2 │                                                      │
│     │     Stage      │                                                      │
│     └───────┬────────┘                                                      │
│             │                                                               │
│             ▼                                                               │
│     ┌────────────────┐     Yes    ┌────────────────┐                       │
│     │  Outstanding?  │───────────►│  Apply PS-MID  │                       │
│     └───────┬────────┘            └───────┬────────┘                       │
│             │ No                          │                                 │
│             ▼                             ▼                                 │
│     ┌────────────────┐            ┌────────────────┐                       │
│     │  Apply PS-FP   │            │ Notice Process │                       │
│     │   (Paid)       │            │     Stops      │                       │
│     └───────┬────────┘            └────────────────┘                       │
│             │                                                               │
│             ▼                                                               │
│     ┌────────────────┐                                                      │
│     │ Notice Process │                                                      │
│     │     Stops      │                                                      │
│     └────────────────┘                                                      │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.3 Process Steps Table (High Level)

| Step | Definition | Brief Description |
| --- | --- | --- |
| Pre-condition 1 | Raw Offence Data Received | Data from REPCCS, EHT, EEPS, PLUS/OCMS Staff Portal |
| Pre-condition 2 | Validate Data | Check mandatory fields, format validation |
| Pre-condition 3 | Check Duplicate Notice | Prevent duplicate notice numbers |
| Pre-condition 4 | Detect Vehicle Type | Identify as Military (Type I) via MID/MINDEF pattern |
| Identify Offence Type | Decision point | Route based on Type O, E, or U |
| Type O & E Workflow | Sub-process | Process via NPA→RD1→RD2 or DN flow |
| Type U Workflow | Sub-process | UPL processing (KIV - pending requirements) |
| End of RD2/DN2 | Check point | Check if notice is outstanding |
| Outstanding? | Decision | Yes = PS-MID, No = PS-FP (Paid) |
| Apply PS-MID | Process | Permanent suspension for Military |
| Apply PS-FP | Process | Full payment suspension |
| Notice Process Stops | End | Processing complete |

---

## 4. Tab 2: Section_4_MID_Type_O_E

### 4.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | Military Vehicle Type O & E Processing |
| Section | 4.2 |
| Trigger | Notice created with Type O or E and vehicle_registration_type = 'I' |
| Frequency | Real-time + Daily CRON |
| Systems Involved | Backend API, CRON, Database, TOPPAN SFTP |
| Expected Outcome | Notice progresses NPA→RD1→RD2→PS-MID |

### 4.2 Detailed Flow Diagram (ASCII)

```
┌──────────────────┬─────────────────────┬──────────────────┬─────────────────┐
│   Backend API    │    CRON Service     │    Database      │  External       │
├──────────────────┼─────────────────────┼──────────────────┼─────────────────┤
│                  │                     │                  │                 │
│ ┌──────────┐     │                     │                  │                 │
│ │  Start   │     │                     │                  │                 │
│ │ Type O/E │     │                     │                  │                 │
│ └────┬─────┘     │                     │                  │                 │
│      │           │                     │                  │                 │
│      ▼           │                     │                  │                 │
│ ┌──────────────┐ │                     │ ┌──────────────┐ │                 │
│ │Duplicate     │ │                     │ │   Query      │ │                 │
│ │Notice Check  │─┼─────────────────────┼─│existing      │ │                 │
│ └──────┬───────┘ │                     │ │notices       │ │                 │
│        │         │                     │ └──────────────┘ │                 │
│   No Dup         │                     │                  │                 │
│        │         │                     │                  │                 │
│        ▼         │                     │                  │                 │
│ ┌──────────────┐ │                     │                  │                 │
│ │Vehicle Type  │ │                     │                  │                 │
│ │= 'I' (MID)   │ │                     │                  │                 │
│ └──────┬───────┘ │                     │                  │                 │
│        │         │                     │                  │                 │
│        ▼         │                     │                  │                 │
│ ┌──────────────┐ │                     │ ┌──────────────┐ │                 │
│ │Create Notice │ │                     │ │ INSERT INTO  │ │                 │
│ │at NPA with   │─┼─────────────────────┼─│ ocms_valid_  │ │                 │
│ │MINDEF details│ │                     │ │ offence_     │ │                 │
│ └──────┬───────┘ │                     │ │ notice       │ │                 │
│        │         │                     │ └──────────────┘ │                 │
│        ▼         │                     │                  │                 │
│ ┌──────────────┐ │                     │                  │                 │
│ │Check Double  │ │                     │                  │                 │
│ │Booking (DBB) │ │                     │                  │                 │
│ └──────┬───────┘ │                     │                  │                 │
│    DBB?│         │                     │                  │                 │
│   ┌────┴────┐    │                     │                  │                 │
│   │Yes      │No  │                     │                  │                 │
│   ▼         ▼    │                     │                  │                 │
│ ┌──────┐ ┌──────────────┐              │                  │                 │
│ │PS-DBB│ │AN Qualify    │              │                  │                 │
│ │ End  │ │Check (Type O)│              │                  │                 │
│ └──────┘ └──────┬───────┘              │                  │                 │
│            │    │                      │                  │                 │
│      ┌─────┴────┴─────┐                │                  │                 │
│      │Yes             │No              │                  │                 │
│      ▼                ▼                │                  │                 │
│ ┌──────────┐    │ ┌──────────────┐     │                  │ ┌─────────────┐ │
│ │AN Subflow│    │ │Prepare RD1   │     │                  │ │   TOPPAN    │ │
│ │(Tab 3)   │    │ │Letter Gen    │─────┼──────────────────┼─│   SFTP      │ │
│ └──────────┘    │ └──────┬───────┘     │                  │ └─────────────┘ │
│                 │        │             │                  │                 │
│                 │        ▼             │                  │                 │
│                 │ ┌──────────────┐     │ ┌──────────────┐ │                 │
│                 │ │Notice at RD1 │     │ │ UPDATE       │ │                 │
│                 │ │stage         │─────┼─│ last_proc_   │ │                 │
│                 │ └──────┬───────┘     │ │ stage='RD1'  │ │                 │
│                 │        │             │ └──────────────┘ │                 │
│                 │   ┌────┴────┐        │                  │                 │
│                 │   │Payment? │        │                  │                 │
│                 │   └────┬────┘        │                  │                 │
│                 │  Yes/  │  \No        │                  │                 │
│                 │       │    │         │                  │                 │
│                 │  ┌────┘    └────┐    │                  │                 │
│                 │  ▼              ▼    │                  │                 │
│                 │ PS-FP    ┌──────────────┐               │ ┌─────────────┐ │
│                 │ End      │Prepare RD2   │               │ │   TOPPAN    │ │
│                 │          │Letter Gen    │───────────────┼─│   SFTP      │ │
│                 │          └──────┬───────┘               │ └─────────────┘ │
│                 │                 │                       │                 │
│                 │                 ▼                       │                 │
│                 │          ┌──────────────┐ ┌───────────┐ │                 │
│                 │          │Notice at RD2 │ │ UPDATE    │ │                 │
│                 │          │stage         │─│ stage=    │ │                 │
│                 │          └──────┬───────┘ │ 'RD2'     │ │                 │
│                 │                 │         └───────────┘ │                 │
│                 │            ┌────┴────┐                  │                 │
│                 │            │Payment? │                  │                 │
│                 │            └────┬────┘                  │                 │
│                 │           Yes/  │  \No                  │                 │
│                 │               │    │                    │                 │
│                 │          ┌────┘    └────┐               │                 │
│                 │          ▼              ▼               │                 │
│                 │         PS-FP    ┌──────────────┐       │                 │
│                 │         End      │Outstanding?  │       │                 │
│                 │                  └──────┬───────┘       │                 │
│                 │                    Yes/ │  \No          │                 │
│                 │                        │    │           │                 │
│                 │                   ┌────┘    └────┐      │                 │
│                 │                   ▼              ▼      │                 │
│                 │            ┌──────────────┐   PS-FP     │                 │
│                 │            │Apply PS-MID  │   End       │                 │
│                 │            │(Tab 5)       │             │                 │
│                 │            └──────┬───────┘             │                 │
│                 │                   │                     │                 │
│                 │                   ▼                     │                 │
│                 │            ┌──────────────┐             │                 │
│                 │            │     End      │             │                 │
│                 │            └──────────────┘             │                 │
│                 │                                         │                 │
└─────────────────┴─────────────────────────────────────────┴─────────────────┘
```

### 4.3 Process Steps Table (Type O & E)

| Step | Type | Definition | Brief Description | Next Step |
| --- | --- | --- | --- | --- |
| 1 | Start | Start Type O/E Flow | Process for Parking or Payment Evasion offence | 2 |
| 2 | Process | Duplicate Notice Check | Compare against existing notices | 3 |
| 3 | Process | Vehicle Type = 'I' | Confirm Military vehicle detected | 4 |
| 4 | Process | Create Notice at NPA | Insert notice with MINDEF details | 5 |
| 5 | Decision | Double Booking? | Check for DBB condition | Yes→6, No→7 |
| 6 | Process | Apply PS-DBB | Suspend duplicate notice | End |
| 7 | Decision | AN Qualify? (Type O) | Check Advisory Notice eligibility | Yes→8, No→9 |
| 8 | Sub-flow | AN Sub-flow | Process Advisory Notice (Tab 3) | End (PS-ANS) |
| 9 | CRON | Prepare for RD1 | Generate RD1 letter, send to TOPPAN | 10 |
| 10 | Process | Notice at RD1 | Update stage to RD1 | 11 |
| 11 | Decision | Payment Made? | Check if payment received at RD1 | Yes→PS-FP, No→12 |
| 12 | CRON | Prepare for RD2 | Generate RD2 letter, send to TOPPAN | 13 |
| 13 | Process | Notice at RD2 | Update stage to RD2 | 14 |
| 14 | Decision | Payment Made? | Check if payment received at RD2 | Yes→PS-FP, No→15 |
| 15 | Decision | Outstanding? | Check if still unpaid at end of RD2 | Yes→16, No→PS-FP |
| 16 | Process | Apply PS-MID | Permanent suspension (Tab 5) | End |
| End | End | End | Process complete | - |

### 4.4 Decision Logic

| ID | Decision | Input | Condition | Yes Action | No Action |
| --- | --- | --- | --- | --- | --- |
| D1 | Double Booking? | Notice details | Matches 5 DBB criteria | Apply PS-DBB, End | Continue |
| D2 | AN Qualify? | Notice, Type O | Meets AN criteria (OCMS 10) | AN Sub-flow | Prepare RD1 |
| D3 | Payment at RD1? | Notice status | payment_status = 'PAID' | Apply PS-FP, End | Prepare RD2 |
| D4 | Payment at RD2? | Notice status | payment_status = 'PAID' | Apply PS-FP, End | Check Outstanding |
| D5 | Outstanding? | Notice status | Outstanding at end of RD2 | Apply PS-MID | Already Paid |

### 4.5 Database Operations

| Step | Operation | Database | Table | Query/Action |
| --- | --- | --- | --- | --- |
| 2 | SELECT | Intranet | ocms_valid_offence_notice | Check duplicate by veh_no, date, rule, lot, pp_code |
| 4 | INSERT | Intranet | ocms_valid_offence_notice | Create notice at NPA stage |
| 4 | INSERT | Intranet | ocms_offence_notice_owner_driver | Insert MINDEF owner details |
| 4 | INSERT | Intranet | ocms_offence_notice_owner_driver_address | Insert MINDEF address |
| 4 | INSERT | Intranet | ocms_offence_notice_detail | Insert notice details |
| 10 | UPDATE | Intranet | ocms_valid_offence_notice | Set last_processing_stage = 'RD1' |
| 13 | UPDATE | Intranet | ocms_valid_offence_notice | Set last_processing_stage = 'RD2' |
| 16 | UPDATE | Both | ocms_valid_offence_notice | Set suspension_type = 'PS', epr_reason = 'MID' |
| 16 | INSERT | Intranet | ocms_suspended_notice | Insert suspension record |

---

## 5. Tab 3: Section_4_MID_Type_U

### 5.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | Military Vehicle Type U Processing |
| Section | 4.3 |
| Trigger | Notice created with Type U and vehicle_registration_type = 'I' |
| Frequency | Real-time + Daily CRON |
| Systems Involved | Backend API, Staff Portal, CRON, Database, TOPPAN SFTP |
| Expected Outcome | OIC decides compoundability → DN1→DN2→PS-MID or immediate PS-MID |

### 5.2 Detailed Flow Diagram (ASCII)

```
┌──────────────────┬─────────────────────┬──────────────────┬─────────────────┐
│   Backend API    │    Staff Portal     │    Database      │  External       │
├──────────────────┼─────────────────────┼──────────────────┼─────────────────┤
│                  │                     │                  │                 │
│ ┌──────────┐     │                     │                  │                 │
│ │  Start   │     │                     │                  │                 │
│ │ Type U   │     │                     │                  │                 │
│ └────┬─────┘     │                     │                  │                 │
│      │           │                     │                  │                 │
│      ▼           │                     │                  │                 │
│ ┌──────────────┐ │                     │ ┌──────────────┐ │                 │
│ │Duplicate     │ │                     │ │   Query      │ │                 │
│ │Notice Check  │─┼─────────────────────┼─│existing      │ │                 │
│ └──────┬───────┘ │                     │ │notices       │ │                 │
│        │         │                     │ └──────────────┘ │                 │
│   No Dup         │                     │                  │                 │
│        │         │                     │                  │                 │
│        ▼         │                     │                  │                 │
│ ┌──────────────┐ │                     │                  │                 │
│ │Vehicle Type  │ │                     │                  │                 │
│ │= 'I' (MID)   │ │                     │                  │                 │
│ └──────┬───────┘ │                     │                  │                 │
│        │         │                     │                  │                 │
│        ▼         │                     │                  │                 │
│ ┌──────────────┐ │                     │ ┌──────────────┐ │                 │
│ │Create Notice │ │                     │ │ INSERT INTO  │ │                 │
│ │at NPA with   │─┼─────────────────────┼─│ ocms_valid_  │ │                 │
│ │MINDEF details│ │                     │ │ offence_     │ │                 │
│ └──────┬───────┘ │                     │ │ notice       │ │                 │
│        │         │                     │ └──────────────┘ │                 │
│        ▼         │                     │                  │                 │
│ ┌──────────────┐ │                     │                  │                 │
│ │Check Double  │ │                     │                  │                 │
│ │Booking (DBB) │ │                     │                  │                 │
│ └──────┬───────┘ │                     │                  │                 │
│    DBB?│         │                     │                  │                 │
│   ┌────┴────┐    │                     │                  │                 │
│   │Yes      │No  │                     │                  │                 │
│   ▼         ▼    │                     │                  │                 │
│ ┌──────┐ ┌──────────────┐              │                  │                 │
│ │PS-DBB│ │Stay at NPA   │              │                  │                 │
│ │ End  │ │pending OIC   │              │                  │                 │
│ └──────┘ │decision      │              │                  │                 │
│          └──────┬───────┘              │                  │                 │
│                 │                      │                  │                 │
│                 │        ┌──────────────────┐             │                 │
│                 │        │ OIC Reviews      │             │                 │
│                 └───────►│ Notice and       │             │                 │
│                          │ decides          │             │                 │
│                          │ compoundability  │             │                 │
│                          └────────┬─────────┘             │                 │
│                                   │                       │                 │
│                          ┌────────┴────────┐              │                 │
│                          │Is Compoundable? │              │                 │
│                          └────────┬────────┘              │                 │
│                          Yes/     │      \No              │                 │
│                                  │                        │                 │
│                          ┌───────┘        └───────┐       │                 │
│                          ▼                        ▼       │                 │
│                   ┌──────────────┐         ┌──────────────┐                 │
│                   │Next Stage    │         │Apply PS-MID  │                 │
│                   │= DN1         │         │immediately   │                 │
│                   └──────┬───────┘         └──────┬───────┘                 │
│                          │                        │                         │
│                          ▼                        ▼                         │
│                   ┌──────────────┐         ┌──────────────┐                 │
│                   │CRON: Prepare │         │     End      │                 │
│                   │for DN1       │         │              │                 │
│                   └──────┬───────┘         └──────────────┘                 │
│                          │                                                  │
│                          ▼                                                  │
│                   ┌──────────────┐                         ┌──────────────┐ │
│                   │Notice at DN1 │                         │   TOPPAN     │ │
│                   │stage         │─────────────────────────│   SFTP       │ │
│                   └──────┬───────┘                         └──────────────┘ │
│                          │                                                  │
│                   ┌──────┴──────┐                                           │
│                   │Payment Made?│                                           │
│                   └──────┬──────┘                                           │
│                   Yes/   │  \No                                             │
│                         │    │                                              │
│                   ┌─────┘    └─────┐                                        │
│                   ▼                ▼                                        │
│                 PS-FP      ┌──────────────┐                ┌──────────────┐ │
│                 End        │CRON: Prepare │                │   TOPPAN     │ │
│                            │for DN2       │────────────────│   SFTP       │ │
│                            └──────┬───────┘                └──────────────┘ │
│                                   │                                         │
│                                   ▼                                         │
│                            ┌──────────────┐                                 │
│                            │Notice at DN2 │                                 │
│                            │stage         │                                 │
│                            └──────┬───────┘                                 │
│                                   │                                         │
│                            ┌──────┴──────┐                                  │
│                            │Payment Made?│                                  │
│                            └──────┬──────┘                                  │
│                            Yes/   │  \No                                    │
│                                  │    │                                     │
│                            ┌─────┘    └─────┐                               │
│                            ▼                ▼                               │
│                          PS-FP       ┌──────────────┐                       │
│                          End         │Apply PS-MID  │                       │
│                                      │              │                       │
│                                      └──────┬───────┘                       │
│                                             │                               │
│                                             ▼                               │
│                                      ┌──────────────┐                       │
│                                      │     End      │                       │
│                                      └──────────────┘                       │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.3 Process Steps Table (Type U)

| Step | Type | Definition | Brief Description | Next Step |
| --- | --- | --- | --- | --- |
| 1 | Start | Start Type U Flow | Process for UPL offence | 2 |
| 2 | Process | Duplicate Notice Check | Compare against existing notices | 3 |
| 3 | Process | Vehicle Type = 'I' | Confirm Military vehicle detected | 4 |
| 4 | Process | Create Notice at NPA | Insert notice with MINDEF details | 5 |
| 5 | Decision | Double Booking? | Check for DBB condition | Yes→6, No→7 |
| 6 | Process | Apply PS-DBB | Suspend duplicate notice | End |
| 7 | Process | Stay at NPA | Notice pending OIC review | 8 |
| 8 | Process | OIC Reviews Notice | OIC determines compoundability | 9 |
| 9 | Decision | Is Compoundable? | OIC decision | Yes→10, No→11 |
| 10 | Process | Next Stage = DN1 | Set next_processing_stage | 12 |
| 11 | Process | Apply PS-MID Immediately | Non-compoundable → suspend | End |
| 12 | CRON | Prepare for DN1 | Generate DN1 letter, send to TOPPAN | 13 |
| 13 | Process | Notice at DN1 | Update stage to DN1 | 14 |
| 14 | Decision | Payment Made? | Check if payment received at DN1 | Yes→PS-FP, No→15 |
| 15 | CRON | Prepare for DN2 | Generate DN2 letter, send to TOPPAN | 16 |
| 16 | Process | Notice at DN2 | Update stage to DN2 | 17 |
| 17 | Decision | Payment Made? | Check if payment received at DN2 | Yes→PS-FP, No→18 |
| 18 | Process | Apply PS-MID | Permanent suspension | End |
| End | End | End | Process complete | - |

### 5.4 Decision Logic (Type U)

| ID | Decision | Input | Condition | Yes Action | No Action |
| --- | --- | --- | --- | --- | --- |
| D1 | Double Booking? | Notice details | Matches 5 DBB criteria | Apply PS-DBB, End | Continue |
| D2 | Is Compoundable? | OIC decision | OIC marks compoundable | Set next stage DN1 | Apply PS-MID |
| D3 | Payment at DN1? | Notice status | payment_status = 'PAID' | Apply PS-FP, End | Prepare DN2 |
| D4 | Payment at DN2? | Notice status | payment_status = 'PAID' | Apply PS-FP, End | Apply PS-MID |

---

## 6. Tab 4: Section_4_MID_AN_Subflow

### 6.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | Military Vehicle Advisory Notice Sub-flow |
| Section | 4.3 |
| Trigger | Type O notice qualifies for AN |
| Frequency | Daily CRON |
| Systems Involved | CRON, Database, TOPPAN SFTP |
| Expected Outcome | AN Letter generated, notice suspended with PS-ANS |

### 6.2 AN Sub-flow Diagram (ASCII)

```
┌──────────────────┬─────────────────────┬──────────────────┬─────────────────┐
│    Backend       │    CRON Service     │    Database      │  TOPPAN SFTP    │
├──────────────────┼─────────────────────┼──────────────────┼─────────────────┤
│                  │                     │                  │                 │
│                  │ ┌──────────────┐    │                  │                 │
│                  │ │ CRON Start   │    │                  │                 │
│                  │ │ AN Letter Gen│    │                  │                 │
│                  │ └──────┬───────┘    │                  │                 │
│                  │        │            │                  │                 │
│                  │        ▼            │                  │                 │
│                  │ ┌──────────────┐    │ ┌──────────────┐ │                 │
│                  │ │Query Notices │    │ │   SELECT     │ │                 │
│                  │ │for AN        │────┼─│ WHERE type=I │ │                 │
│                  │ │              │    │ │ an_flag='Y'  │ │                 │
│                  │ │              │    │ │ stage=NPA    │ │                 │
│                  │ └──────┬───────┘    │ │ next=RD1     │ │                 │
│                  │        │            │ │ no susp      │ │                 │
│                  │        │            │ └──────────────┘ │                 │
│                  │   ┌────┴────┐       │                  │                 │
│                  │   │Records? │       │                  │                 │
│                  │   └────┬────┘       │                  │                 │
│                  │  Yes/  │  \No       │                  │                 │
│                  │       │    │        │                  │                 │
│                  │  ┌────┘    └────┐   │                  │                 │
│                  │  ▼              ▼   │                  │                 │
│                  │ ┌──────────┐   End  │                  │                 │
│                  │ │Generate  │        │                  │                 │
│                  │ │AN Letter │        │                  │                 │
│                  │ │PDF       │        │                  │                 │
│                  │ └────┬─────┘        │                  │                 │
│                  │      │              │                  │                 │
│                  │      ▼              │                  │ ┌─────────────┐ │
│                  │ ┌──────────────┐    │                  │ │  Upload     │ │
│                  │ │Drop to SFTP  │────┼──────────────────┼─│  PDF files  │ │
│                  │ └──────┬───────┘    │                  │ └─────────────┘ │
│                  │        │            │                  │                 │
│                  │        ▼            │                  │                 │
│                  │ ┌──────────────┐    │ ┌──────────────┐ │                 │
│                  │ │Update Stage  │    │ │ UPDATE       │ │                 │
│                  │ │to RD1        │────┼─│ last_stage=  │ │                 │
│                  │ └──────┬───────┘    │ │ 'RD1'        │ │                 │
│                  │        │            │ └──────────────┘ │                 │
│                  │        ▼            │                  │                 │
│                  │ ┌──────────────┐    │ ┌──────────────┐ │                 │
│                  │ │Apply PS-ANS  │    │ │ UPDATE       │ │                 │
│                  │ │Suspension    │────┼─│ susp='PS'    │ │                 │
│                  │ └──────┬───────┘    │ │ reason='ANS' │ │                 │
│                  │        │            │ └──────────────┘ │                 │
│                  │        ▼            │                  │                 │
│                  │ ┌──────────────┐    │                  │                 │
│                  │ │     End      │    │                  │                 │
│                  │ └──────────────┘    │                  │                 │
│                  │                     │                  │                 │
└──────────────────┴─────────────────────┴──────────────────┴─────────────────┘
```

### 6.3 AN Query Conditions

```sql
SELECT * FROM ocms_valid_offence_notice
WHERE vehicle_registration_type = 'I'
  AND an_flag = 'Y'
  AND last_processing_stage = 'NPA'
  AND next_processing_stage = 'RD1'
  AND next_processing_date <= CURRENT_DATE
  AND suspension_type IS NULL
  AND epr_reason_of_suspension IS NULL
```

### 6.4 Process Steps Table (AN Sub-flow)

| Step | Type | Definition | Brief Description | Next Step |
| --- | --- | --- | --- | --- |
| 1 | CRON Start | AN Letter Generation Job | Daily scheduled job | 2 |
| 2 | Process | Query Notices for AN | Get eligible AN notices | 3 |
| 3 | Decision | Records Found? | Check if any notices qualify | Yes→4, No→End |
| 4 | Process | Generate AN Letter PDF | Create PDF for each notice | 5 |
| 5 | Process | Drop to SFTP | Send PDFs to TOPPAN | 6 |
| 6 | Process | Update Stage to RD1 | Set last_processing_stage = 'RD1' | 7 |
| 7 | Process | Apply PS-ANS | Suspend with PS-ANS | End |
| End | End | End | AN processing complete | - |

---

## 7. Tab 5: Section_4_MID_Furnish_Subflow

### 7.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | Furnish Driver/Hirer for Military Vehicle |
| Section | 4.4 |
| Trigger | Vehicle Owner submits Driver/Hirer via eService |
| Frequency | Real-time + Daily sync |
| Systems Involved | eService (Internet), Backend API, Database, MHA/DataHive |
| Expected Outcome | Notice redirected to Driver (DN flow) or Hirer (RD flow) |

### 7.2 Furnish Sub-flow Diagram (ASCII)

```
┌─────────────────┬──────────────────┬─────────────────┬──────────────────────┐
│  eService Portal│   Backend API    │    Database     │  External (MHA/DH)   │
├─────────────────┼──────────────────┼─────────────────┼──────────────────────┤
│                 │                  │                 │                      │
│ ┌─────────────┐ │                  │                 │                      │
│ │ Owner       │ │                  │                 │                      │
│ │ Furnish     │ │                  │                 │                      │
│ │ Driver/Hirer│ │                  │                 │                      │
│ └──────┬──────┘ │                  │                 │                      │
│        │        │                  │                 │                      │
│        ▼        │                  │                 │                      │
│ ┌─────────────┐ │                  │ ┌─────────────┐ │                      │
│ │ Submit      │ │                  │ │ INSERT INTO │ │                      │
│ │ Application │─┼──────────────────┼─│ eocms_      │ │                      │
│ └─────────────┘ │                  │ │ furnish_app │ │                      │
│                 │                  │ └─────────────┘ │                      │
│                 │ ┌──────────────┐ │                 │                      │
│                 │ │ CRON: Sync   │ │                 │                      │
│                 │ │ to Intranet  │ │                 │                      │
│                 │ └──────┬───────┘ │                 │                      │
│                 │        │         │                 │                      │
│                 │        ▼         │                 │                      │
│                 │ ┌──────────────┐ │                 │                      │
│                 │ │ Validate     │ │                 │                      │
│                 │ │ Particulars  │ │                 │                      │
│                 │ └──────┬───────┘ │                 │                      │
│                 │   Pass/│ \Fail   │                 │                      │
│                 │       │    │     │                 │                      │
│                 │  ┌────┘    └────┐│                 │                      │
│                 │  ▼              ▼│                 │                      │
│                 │ ┌──────────┐ ┌──────────────┐      │                      │
│                 │ │Auto      │ │OIC Manual    │      │                      │
│                 │ │Approve   │ │Review        │      │                      │
│                 │ └────┬─────┘ └──────┬───────┘      │                      │
│                 │      │         │    │              │                      │
│                 │      │    ┌────┴────┴────┐         │                      │
│                 │      │    │Approve/Reject│         │                      │
│                 │      │    └──────┬───────┘         │                      │
│                 │      │     Appr/ │  \Rej           │                      │
│                 │      │          │    │             │                      │
│                 │      │     ┌────┘    └────┐        │                      │
│                 │      │     │              ▼        │                      │
│                 │      │     │       ┌───────────┐   │                      │
│                 │      │     │       │Remove     │   │                      │
│                 │      │     │       │Pending    │   │                      │
│                 │      │     │       │Status     │   │                      │
│                 │      │     │       └─────┬─────┘   │                      │
│                 │      │     │             │         │                      │
│                 │      │     │             ▼         │                      │
│                 │      │     │       ┌───────────┐   │                      │
│                 │      │     │       │Allow      │   │                      │
│                 │      │     │       │Re-furnish │   │                      │
│                 │      │     │       └───────────┘   │                      │
│                 │      │     │                       │                      │
│                 │      └─────┼───────┐               │                      │
│                 │            ▼       │               │                      │
│                 │      ┌──────────────┐              │                      │
│                 │      │Create Driver/│              │                      │
│                 │      │Hirer Record  │              │                      │
│                 │      └──────┬───────┘              │                      │
│                 │             │                      │                      │
│                 │        ┌────┴────┐                 │                      │
│                 │        │Driver or│                 │                      │
│                 │        │Hirer?   │                 │                      │
│                 │        └────┬────┘                 │                      │
│                 │       Drv/  │  \Hirer              │                      │
│                 │            │    │                  │                      │
│                 │       ┌────┘    └────┐             │                      │
│                 │       ▼              ▼             │                      │
│                 │ Next Stage    Next Stage           │                      │
│                 │ = DN1         = RD1                │                      │
│                 │       │              │             │                      │
│                 │       └──────┬───────┘             │                      │
│                 │              │                     │                      │
│                 │              ▼                     │ ┌──────────────────┐ │
│                 │ ┌──────────────────┐               │ │ MHA / DataHive   │ │
│                 │ │Prepare MHA/DH    │───────────────┼─│ Validate NRIC/   │ │
│                 │ │Check before      │               │ │ FIN particulars  │ │
│                 │ │RD2/DN2           │               │ └──────────────────┘ │
│                 │ └──────────────────┘               │                      │
│                 │              │                     │                      │
│                 │              ▼                     │                      │
│                 │ ┌──────────────────┐               │                      │
│                 │ │Continue to       │               │                      │
│                 │ │RD2/DN2→PS-MID    │               │                      │
│                 │ └──────────────────┘               │                      │
│                 │                                    │                      │
└─────────────────┴────────────────────────────────────┴──────────────────────┘
```

### 7.3 Furnish Eligibility by Stage

| Stage | Furnish Allowed | Reason |
| --- | --- | --- |
| NPA | No | Notice just created |
| ROV | No | Awaiting review |
| RD1 | Yes | Owner can furnish |
| RD2 | Yes | Owner can furnish |
| DN1 | No | Already furnished |
| DN2 | No | Already furnished |
| PS-MID | No | Notice suspended |

### 7.4 Process Steps Table (Furnish Sub-flow)

| Step | Type | Definition | Brief Description | Next Step |
| --- | --- | --- | --- | --- |
| 1 | Start | Owner Furnish | Owner submits via eService | 2 |
| 2 | Process | Submit Application | Store in Internet DB | 3 |
| 3 | CRON | Sync to Intranet | Copy furnish data to Intranet | 4 |
| 4 | Process | Validate Particulars | Check Driver/Hirer info | 5 |
| 5 | Decision | Pass Validation? | Auto-check criteria | Yes→6, No→7 |
| 6 | Process | Auto Approve | System approves | 9 |
| 7 | Process | OIC Manual Review | OIC reviews application | 8 |
| 8 | Decision | OIC Decision | Approve or Reject | Approve→9, Reject→10 |
| 9 | Process | Create Driver/Hirer Record | Add to owner_driver table | 11 |
| 10 | Process | Remove Pending Status | Allow re-furnish | End |
| 11 | Decision | Driver or Hirer? | Determine next stage | Driver→DN1, Hirer→RD1 |
| 12 | CRON | MHA/DataHive Check | Validate Driver/Hirer before RD2/DN2 transition | 13 |
| 13 | Process | Continue to RD2/DN2 | Progress to next stage | 14 |
| 14 | Process | End of RD2/DN2 | If outstanding, apply PS-MID | End |
| End | End | End | Furnish complete | - |

### 7.5 MHA/DataHive Check Details

| Attribute | Value |
| --- | --- |
| Type | CRON Job (Separate step) |
| Trigger | Before RD2/DN2 letter generation |
| Purpose | Validate furnished Driver/Hirer particulars |

#### Process Steps

1. Query notices with furnished Driver/Hirer at RD1/DN1
2. Call MHA API to validate NRIC/FIN
3. Call DataHive API to get latest address/particulars
4. Update offender details if changed
5. Continue to RD2/DN2 letter generation

---

## 8. Tab 6: Section_4_MID_PS_Suspension

### 8.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | PS-MID Suspension for Military Vehicles |
| Section | 4.5 |
| Trigger | Notice outstanding at end of RD2 or DN2 |
| Frequency | Daily CRON (before RR3/DR3 prep) |
| Systems Involved | CRON, Database (Intranet & Internet) |
| Expected Outcome | Notice suspended with PS-MID, processing stops |

### 8.2 PS-MID Suspension Flow Diagram (ASCII)

```
┌──────────────────┬─────────────────────┬──────────────────────────────────────┐
│    CRON Service  │    Intranet DB      │    Internet DB                        │
├──────────────────┼─────────────────────┼──────────────────────────────────────┤
│                  │                     │                                       │
│ ┌──────────────┐ │                     │                                       │
│ │ CRON Start   │ │                     │                                       │
│ │ End of RD2/  │ │                     │                                       │
│ │ DN2 (11:59PM)│ │                     │                                       │
│ └──────┬───────┘ │                     │                                       │
│        │         │                     │                                       │
│        ▼         │                     │                                       │
│ ┌──────────────┐ │ ┌─────────────────┐ │                                       │
│ │Query VON     │ │ │  SELECT WHERE   │ │                                       │
│ │Table for MID │─┼─│  veh_reg_type=  │ │                                       │
│ │Notices       │ │ │  'I' AND        │ │                                       │
│ │              │ │ │  stage IN       │ │                                       │
│ │              │ │ │  (RD2,DN2) AND  │ │                                       │
│ │              │ │ │  next IN        │ │                                       │
│ │              │ │ │  (RR3,DR3) AND  │ │                                       │
│ │              │ │ │  crs_reason     │ │                                       │
│ │              │ │ │  IS NULL        │ │                                       │
│ └──────┬───────┘ │ └─────────────────┘ │                                       │
│        │         │                     │                                       │
│   ┌────┴────┐    │                     │                                       │
│   │Records? │    │                     │                                       │
│   └────┬────┘    │                     │                                       │
│   Yes/ │  \No    │                     │                                       │
│       │    │     │                     │                                       │
│  ┌────┘    └────┐│                     │                                       │
│  ▼              ▼│                     │                                       │
│ ┌──────────────┐ │                     │                                       │
│ │For Each      │ │                     │                                       │
│ │Notice        │ │                     │                                       │
│ └──────┬───────┘ │                     │                                       │
│        │         │                     │                                       │
│        ▼         │                     │                                       │
│ ┌──────────────┐ │ ┌─────────────────┐ │                                       │
│ │Update VON    │ │ │  UPDATE SET     │ │                                       │
│ │Intranet      │─┼─│  suspension_    │ │                                       │
│ │              │ │ │  type='PS'      │ │                                       │
│ │              │ │ │  epr_reason=    │ │                                       │
│ │              │ │ │  'MID'          │ │                                       │
│ │              │ │ │  epr_date=NOW() │ │                                       │
│ └──────┬───────┘ │ └─────────────────┘ │                                       │
│        │         │                     │                                       │
│        ▼         │                     │ ┌─────────────────────────────────────┐
│ ┌──────────────┐ │                     │ │  UPDATE SET                         │
│ │Update eVON   │─┼─────────────────────┼─│  suspension_type='PS'               │
│ │Internet      │ │                     │ │  epr_reason='MID'                   │
│ └──────┬───────┘ │                     │ │  epr_date=NOW()                     │
│        │         │                     │ └─────────────────────────────────────┘
│        ▼         │                     │                                       │
│ ┌──────────────┐ │ ┌─────────────────┐ │                                       │
│ │Insert        │ │ │  INSERT INTO    │ │                                       │
│ │Suspended     │─┼─│  ocms_suspended │ │                                       │
│ │Notice Record │ │ │  _notice        │ │                                       │
│ └──────┬───────┘ │ └─────────────────┘ │                                       │
│        │         │                     │                                       │
│        ▼         │                     │                                       │
│ ┌──────────────┐ │                     │                                       │
│ │     End      │ │                     │                                       │
│ │ Processing   │ │                     │                                       │
│ │   Stops      │ │                     │                                       │
│ └──────────────┘ │                     │                                       │
│                  │                     │                                       │
└──────────────────┴─────────────────────┴───────────────────────────────────────┘
```

### 8.3 PS-MID Query Conditions

**Note:** Query uses `suspension_type IS NULL` per Data Dictionary alignment.

```sql
SELECT * FROM ocms_valid_offence_notice
WHERE vehicle_registration_type = 'I'
  AND suspension_type IS NULL           -- No existing PS/TS
  AND last_processing_stage IN ('RD2', 'DN2')
  AND next_processing_stage IN ('RR3', 'DR3')
  AND next_processing_date <= DATEADD(day, 1, CURRENT_DATE)
```

### 8.4 Process Steps Table (PS-MID Suspension)

| Step | Type | Definition | Brief Description | Next Step |
| --- | --- | --- | --- | --- |
| 1 | CRON Start | End of RD2/DN2 | Daily at 11:59 PM | 2 |
| 2 | Process | Query VON Table | Get MID notices at RD2/DN2 | 3 |
| 3 | Decision | Records Found? | Check if any notices qualify | Yes→4, No→End |
| 4 | Loop | For Each Notice | Process each notice | 5 |
| 5 | Process | Update VON Intranet | Set suspension_type='PS', epr_reason='MID' | 6 |
| 6 | Process | Update eVON Internet | Sync suspension to Internet | 7 |
| 7 | Process | Insert Suspended Notice | Create record in ocms_suspended_notice | End |
| End | End | Processing Stops | Notice no longer progresses | - |

### 8.5 Daily Re-check (DipMidForRecheckJob)

| Attribute | Value |
| --- | --- |
| Schedule | Daily at 11:59 PM |
| Purpose | Re-apply PS-MID if accidentally revived |
| Query | Type IN ('D','I','F') AND stage IN ('RD2','DN2') AND no PS |

---

## 9. Error Handling

| Tab | Error Point | Error Type | Handling | Recovery |
| --- | --- | --- | --- | --- |
| Tab 2 | Step 4 | DB Insert Fail | Log error, rollback | Retry or manual review |
| Tab 2 | Step 9 | SFTP Fail | Send error email | Retry next CRON |
| Tab 3 | Step 5 | SFTP Fail | Send error email | Retry next CRON |
| Tab 4 | Step 4 | Validation Fail | Route to OIC | Manual review |
| Tab 4 | Step 12 | MHA/DataHive Fail | Log warning | Continue with existing data |
| Tab 5 | Step 5 | DB Update Fail | Log error | Manual PS-MID via Staff Portal |

---

## 10. Flowchart Checklist

Before creating the technical flowchart:

- [x] All steps have clear names
- [x] All decision points have Yes/No paths defined
- [x] All paths lead to an End point
- [x] Error handling paths are included
- [x] Database operations are identified
- [x] Swimlanes are defined for each system/tier
- [x] Color coding is specified
- [x] Step descriptions are complete
- [x] External system calls are documented
- [x] CRON jobs are identified

---

## 11. Notes for Technical Flowchart Creation

### 11.1 Shape Guidelines

| Element | Shape | Style |
| --- | --- | --- |
| Start/End | Terminator | strokeWidth=2, shape=mxgraph.flowchart.terminator |
| Process | Rectangle | rounded=1, arcSize=14, strokeWidth=2 |
| Decision | Diamond | shape=mxgraph.flowchart.decision |
| Sub-flow | Process Box | shape=process, double border |
| Database | Cylinder | shape=cylinder |
| CRON Job | Rectangle | fillColor=#d5e8d4 (green) |

### 11.2 Connector Guidelines

| Connection Type | Style |
| --- | --- |
| Normal flow | Solid arrow |
| Database operation | Dashed line |
| External system call | Dashed line |
| SFTP transfer | Dashed line with label |

### 11.3 API Payload Box Format

For API calls, use yellow box (#fff2cc) with dashed border:

```
┌─────────────────────────────┐
│ API Payload:                │
│ {                           │
│   "notice_no": "500500303J",│
│   "suspension_type": "PS",  │
│   "reason": "MID"           │
│ }                           │
└─────────────────────────────┘
```

---

## 12. Changelog

| Version | Date | Author | Changes |
| --- | --- | --- | --- |
| 1.0 | 15/01/2026 | Claude | Initial version based on plan_api.md and plan_condition.md |
| 1.1 | 27/01/2026 | Claude | Added Tab 3: Type U flow with OIC compoundability decision, updated section numbering, Data Dictionary alignment: Fixed PS-MID query to use suspension_type IS NULL, added next_processing_date condition, added process_indicator and suspension_source fields |

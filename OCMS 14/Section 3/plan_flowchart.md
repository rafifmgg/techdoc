# Flowchart Plan: Notice Processing Flow for Deceased Offenders

## Overview

| Attribute | Value |
| --- | --- |
| Feature Name | Notice Processing Flow for Deceased Offenders |
| Version | v1.0 |
| Author | Claude |
| Created Date | 15/01/2026 |
| Last Updated | 15/01/2026 |
| Status | Draft |
| FD Reference | OCMS 14 - Section 4 |
| TD Reference | OCMS 14 - Section 3 |

---

## 1. Diagram Sections (Tabs)

The Technical Flowchart will contain the following tabs/sections:

| Tab # | Tab Name | Description | Priority |
| --- | --- | --- | --- |
| 1 | Section_3_High_Level | High-level overview of Deceased Offender Processing | High |
| 2 | Section_3_Detect_and_Suspend | Detailed flow of detecting deceased and applying PS-RIP/RP2 | High |
| 3 | Section_3_RIP_Report | Generate "RIP Hirer Driver Furnished" report for PS-RP2 | Medium |
| 4 | Section_3_Redirect_RIP | OIC revives PS-RIP/RP2 and redirects notice | Medium |

---

## 2. Systems Involved (Swimlanes)

| System/Tier | Color Code | Hex | Description |
| --- | --- | --- | --- |
| OCMS Staff Portal | Light Blue | #dae8fc | Frontend - OIC manual actions |
| OCMS Admin API (Backend) | Light Green | #d5e8d4 | Backend API processing |
| External System (MHA) | Light Yellow | #fff2cc | MHA life status integration |
| External System (DataHive) | Light Yellow | #fff2cc | DataHive FIN death check |
| Database (Intranet) | Light Yellow | #fff2cc | Intranet database operations |

---

## 3. Tab 1: Section_3_High_Level

### 3.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | Deceased Offender Processing - High Level |
| Section | 3.1 |
| Trigger | MHA returns life_status='D' OR FIN found in DataHive D90 |
| Frequency | Real-time (per notice processing) |
| Systems Involved | MHA, DataHive, Backend API, Database |
| Expected Outcome | Notice suspended with PS-RIP or PS-RP2 |

### 3.2 High Level Flow Diagram (ASCII)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    HIGH LEVEL: DECEASED OFFENDER PROCESSING                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌───────────────────────┐     ┌───────────────────────┐                        │
│  │ MHA returns NRIC      │     │ FIN holder found in   │                        │
│  │ Life Status as "D"    │     │ DataHive "Dead FIN"   │                        │
│  │ (Deceased)            │     │ dataset (D90)         │                        │
│  └───────────┬───────────┘     └───────────┬───────────┘                        │
│              │                             │                                     │
│              └──────────────┬──────────────┘                                     │
│                             │                                                    │
│                             ▼                                                    │
│                ┌────────────────────────┐                                        │
│                │  Update Offender       │                                        │
│                │  Particulars           │                                        │
│                │  (life_status,         │                                        │
│                │   date_of_death)       │                                        │
│                └────────────┬───────────┘                                        │
│                             │                                                    │
│                             ▼                                                    │
│                ┌────────────────────────┐                                        │
│                │  PROCESS:              │                                        │
│                │  Check Date of Death   │                                        │
│                │  and apply PS-RIP or   │                                        │
│                │  PS-RP2                │                                        │
│                └────────────┬───────────┘                                        │
│                             │                                                    │
│                             ▼                                                    │
│                ┌────────────────────────┐                                        │
│                │  Generate "RIP Hirer   │                                        │
│                │  Driver Furnished"     │                                        │
│                │  report for eligible   │                                        │
│                │  RP2 Notices           │                                        │
│                └────────────┬───────────┘                                        │
│                             │                                                    │
│                             ▼                                                    │
│                ┌────────────────────────┐                                        │
│                │  PROCESS:              │                                        │
│                │  OIC redirect RIP/RP2  │                                        │
│                │  Notices (if needed)   │                                        │
│                └────────────┬───────────┘                                        │
│                             │                                                    │
│                             ▼                                                    │
│                       ┌──────────┐                                               │
│                       │   End    │                                               │
│                       └──────────┘                                               │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 3.3 Process Steps Table (High Level)

| Step | Definition | Brief Description |
| --- | --- | --- |
| Trigger 1 | MHA Life Status | MHA returns NRIC offender life status as "D" (Deceased) |
| Trigger 2 | DataHive D90 | FIN holder found in DataHive's "Dead Foreign Pass Holders" dataset |
| Update Particulars | Update offender data | Store life_status='D' and date_of_death in database |
| Suspend Notice | Apply PS-RIP or PS-RP2 | Based on date_of_death vs offence_date comparison |
| Generate Report | RIP Hirer Driver Report | Daily report for PS-RP2 notices with deceased Hirer/Driver |
| OIC Redirect | Manual follow-up | OIC may revive PS and redirect notice to new offender |
| End | Exit point | Process complete |

---

## 4. Tab 2: Section_3_Detect_and_Suspend

### 4.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | Detect Deceased and Apply Suspension |
| Section | 3.2 |
| Trigger | Offender particulars returned from MHA/DataHive |
| Frequency | Real-time (per notice) |
| Systems Involved | MHA, DataHive, Backend API, Database |
| Expected Outcome | PS-RIP or PS-RP2 applied to notice |

### 4.2 Detailed Flow Diagram (ASCII)

```
┌──────────────────┬──────────────────────┬────────────────────┬──────────────────┐
│  External System │    Backend API       │     Database       │     Result       │
│  (MHA/DataHive)  │                      │                    │                  │
├──────────────────┼──────────────────────┼────────────────────┼──────────────────┤
│                  │                      │                    │                  │
│ ┌──────────────┐ │                      │                    │                  │
│ │    Start     │ │                      │                    │                  │
│ └──────┬───────┘ │                      │                    │                  │
│        │         │                      │                    │                  │
│        ▼         │                      │                    │                  │
│ ┌──────────────┐ │                      │                    │                  │
│ │ Receive      │ │                      │                    │                  │
│ │ life_status  │ │                      │                    │                  │
│ │ from MHA/    │ │                      │                    │                  │
│ │ DataHive     │ │                      │                    │                  │
│ └──────┬───────┘ │                      │                    │                  │
│        │         │                      │                    │                  │
│        ▼         │                      │                    │                  │
│ ┌──────────────┐ │                      │                    │                  │
│ │life_status   │ │                      │                    │                  │
│ │   = 'D'?     │ │                      │                    │                  │
│ └──────┬───────┘ │                      │                    │                  │
│   Yes/ │ \No     │                      │                    │                  │
│       │  │       │                      │                    │                  │
│  ┌────┘  └────────────────────────────────────────────────────► Continue       │
│  │               │                      │                    │  Normal Flow    │
│  ▼               │                      │                    │  ───────►End    │
│                  │ ┌──────────────────┐ │                    │                  │
│                  │ │ Update Offender  │ │                    │                  │
│                  │ │ Particulars      ├─┼───────────────────►│                  │
│                  │ │ life_status='D'  │ │ UPDATE             │                  │
│                  │ │ date_of_death    │ │ ocms_offence_      │                  │
│                  │ └────────┬─────────┘ │ notice_owner_driver│                  │
│                  │          │           │                    │                  │
│                  │          ▼           │                    │                  │
│                  │ ┌──────────────────┐ │                    │                  │
│                  │ │ date_of_death    │ │                    │                  │
│                  │ │ >= offence_date? │ │                    │                  │
│                  │ └────────┬─────────┘ │                    │                  │
│                  │     Yes/ │ \No       │                    │                  │
│                  │         │  │         │                    │                  │
│                  │    ┌────┘  └────┐    │                    │                  │
│                  │    │            │    │                    │                  │
│                  │    ▼            ▼    │                    │                  │
│                  │ ┌────────┐ ┌────────┐│                    │                  │
│                  │ │Apply   │ │Apply   ││                    │                  │
│                  │ │PS-RIP  │ │PS-RP2  ││                    │                  │
│                  │ └───┬────┘ └───┬────┘│                    │                  │
│                  │     │          │     │                    │                  │
│                  │     └────┬─────┘     │                    │                  │
│                  │          │           │                    │                  │
│                  │          ▼           │                    │                  │
│                  │ ┌──────────────────┐ │                    │                  │
│                  │ │ Update Notice    │ │                    │                  │
│                  │ │ suspension_type  ├─┼───────────────────►│                  │
│                  │ │ = 'PS'           │ │ UPDATE             │                  │
│                  │ │ epr_reason =     │ │ ocms_valid_        │                  │
│                  │ │ 'RIP' or 'RP2'   │ │ offence_notice     │                  │
│                  │ └────────┬─────────┘ │                    │                  │
│                  │          │           │                    │                  │
│                  │          ▼           │                    │                  │
│                  │ ┌──────────────────┐ │                    │                  │
│                  │ │ Insert Suspended ├─┼───────────────────►│                  │
│                  │ │ Notice Record    │ │ INSERT             │                  │
│                  │ │ reason='RIP/RP2' │ │ ocms_suspended_    │                  │
│                  │ └────────┬─────────┘ │ notice             │                  │
│                  │          │           │                    │                  │
│                  │          ▼           │                    │                  │
│                  │    ┌──────────┐      │                    │                  │
│                  │    │   End    │      │                    │                  │
│                  │    └──────────┘      │                    │                  │
│                  │                      │                    │                  │
└──────────────────┴──────────────────────┴────────────────────┴──────────────────┘
```

### 4.3 Process Steps Table (Detailed)

| Step | Type | Definition | Brief Description | Next Step |
| --- | --- | --- | --- | --- |
| 1 | Start | Start Detection | Receive offender particulars from MHA/DataHive | 2 |
| 2 | Process | Receive life_status | Extract life_status field from response | 3 |
| 3 | Decision | life_status = 'D'? | Check if offender is deceased | Yes→4, No→End |
| 4 | Process | Update Offender Particulars | Set life_status='D', date_of_death in DB | 5 |
| 5 | Decision | date_of_death >= offence_date? | Compare death date with offence date | Yes→6, No→7 |
| 6 | Process | Apply PS-RIP | Apply permanent suspension with reason 'RIP' | 8 |
| 7 | Process | Apply PS-RP2 | Apply permanent suspension with reason 'RP2' | 8 |
| 8 | Process | Update Notice | Set suspension_type='PS', epr_reason='RIP/RP2' | 9 |
| 9 | Process | Insert Suspended Notice | Create record in ocms_suspended_notice | 10 |
| 10 | End | End | Suspension complete | - |

### 4.4 Decision Logic

| ID | Decision | Input | Condition | Yes Action | No Action |
| --- | --- | --- | --- | --- | --- |
| D1 | life_status = 'D'? | life_status from MHA/DataHive | life_status == 'D' | Update particulars | Continue normal flow |
| D2 | date_of_death >= offence_date? | date_of_death, notice_date_and_time | DoD >= offence_date | Apply PS-RIP | Apply PS-RP2 |

### 4.5 Database Operations

| Step | Operation | Database | Table | Query/Action |
| --- | --- | --- | --- | --- |
| 4 | UPDATE | Intranet | ocms_offence_notice_owner_driver | `SET life_status='D', date_of_death=<value>` |
| 8 | UPDATE | Intranet | ocms_valid_offence_notice | `SET suspension_type='PS', epr_reason_of_suspension='RIP/RP2'` |
| 9 | INSERT | Intranet | ocms_suspended_notice | `INSERT reason_of_suspension='RIP/RP2', suspension_type='PS'` |

### 4.6 External System Calls

| Step | System | Call | Input | Output |
| --- | --- | --- | --- | --- |
| 2 | MHA | SFTP Response | NRIC | life_status, date_of_death |
| 2 | DataHive | D90 Query | FIN | date_of_death (if exists) |

---

## 5. Tab 3: Section_3_RIP_Report

### 5.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | Generate RIP Hirer Driver Furnished Report |
| Section | 3.3 |
| Trigger | Daily cron job |
| Frequency | Daily |
| Systems Involved | Backend (Cron), Database, Email Service |
| Expected Outcome | Report generated and emailed to OICs |

### 5.2 Detailed Flow Diagram (ASCII)

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                   RIP HIRER DRIVER FURNISHED REPORT GENERATION                    │
├──────────────────┬───────────────────────────────────────┬───────────────────────┤
│    Backend       │              Database                 │       Output          │
│    (Cron)        │                                       │                       │
├──────────────────┼───────────────────────────────────────┼───────────────────────┤
│                  │                                       │                       │
│ ┌──────────────┐ │                                       │                       │
│ │ Cron Start   │ │                                       │                       │
│ │ (Daily)      │ │                                       │                       │
│ └──────┬───────┘ │                                       │                       │
│        │         │                                       │                       │
│        ▼         │                                       │                       │
│ ┌──────────────┐ │                                       │                       │
│ │ Query for    │ │                                       │                       │
│ │ notices with ├─┼──────────────────────────────────────►│                       │
│ │ PS-RP2 today │ │  SELECT from ocms_suspended_notice    │                       │
│ │ + deceased   │ │  JOIN ocms_offence_notice_owner_driver│                       │
│ │ Hirer/Driver │ │  WHERE reason='RP2'                   │                       │
│ └──────┬───────┘ │  AND date = today                     │                       │
│        │         │  AND owner_driver_indicator IN (H,D)  │                       │
│        ▼         │  AND life_status = 'D'                │                       │
│ ┌──────────────┐ │                                       │                       │
│ │ Any records? │ │                                       │                       │
│ └──────┬───────┘ │                                       │                       │
│   Yes/ │ \No     │                                       │                       │
│       │  └───────────────────────────────────────────────┼────► End (No report) │
│       │          │                                       │                       │
│       ▼          │                                       │                       │
│ ┌──────────────┐ │                                       │                       │
│ │ Format Data  │ │                                       │                       │
│ │ for Report   │ │                                       │                       │
│ └──────┬───────┘ │                                       │                       │
│        │         │                                       │                       │
│        ▼         │                                       │                       │
│ ┌──────────────┐ │                                       │                       │
│ │ Generate     │ │                                       │                       │
│ │ Report File  ├─┼───────────────────────────────────────┼────► Report File     │
│ └──────┬───────┘ │                                       │      (.xlsx/.pdf)    │
│        │         │                                       │                       │
│        ▼         │                                       │                       │
│ ┌──────────────┐ │                                       │                       │
│ │ Send Email   │ │                                       │                       │
│ │ to OICs      ├─┼───────────────────────────────────────┼────► Email Sent      │
│ └──────┬───────┘ │                                       │                       │
│        │         │                                       │                       │
│        ▼         │                                       │                       │
│   ┌──────────┐   │                                       │                       │
│   │   End    │   │                                       │                       │
│   └──────────┘   │                                       │                       │
│                  │                                       │                       │
└──────────────────┴───────────────────────────────────────┴───────────────────────┘
```

### 5.3 Process Steps Table

| Step | Type | Definition | Brief Description | Next Step |
| --- | --- | --- | --- | --- |
| 1 | Start | Cron Start | Daily scheduled job starts | 2 |
| 2 | Process | Query Eligible Notices | Query notices with PS-RP2 today + deceased Hirer/Driver | 3 |
| 3 | Decision | Any records? | Check if query returned any results | Yes→4, No→End |
| 4 | Process | Format Data | Prepare data for report format | 5 |
| 5 | Process | Generate Report | Create report file (Excel/PDF) | 6 |
| 6 | Process | Send Email | Email report to OIC distribution list | 7 |
| 7 | End | End | Report generation complete | - |

### 5.4 Query Details

```sql
-- Query for RIP Hirer Driver Furnished Report
SELECT
    von.notice_no,
    ond.name AS offender_name,
    ond.id_no AS offender_id,
    ond.owner_driver_indicator,
    ond.date_of_death,
    sn.date_of_suspension AS suspension_date,
    von.notice_date_and_time AS offence_date
FROM ocms_suspended_notice sn
JOIN ocms_valid_offence_notice von ON sn.notice_no = von.notice_no
JOIN ocms_offence_notice_owner_driver ond ON von.notice_no = ond.notice_no
WHERE sn.suspension_type = 'PS'
  AND sn.reason_of_suspension = 'RP2'
  AND CAST(sn.date_of_suspension AS DATE) = CAST(GETDATE() AS DATE)
  AND sn.due_date_of_revival IS NULL
  AND ond.owner_driver_indicator IN ('H', 'D')
  AND ond.offender_indicator = 'Y'
  AND ond.life_status = 'D'
ORDER BY von.notice_no
```

### 5.5 Report Output Fields

| Column | Source | Description |
| --- | --- | --- |
| Notice No | von.notice_no | Notice number |
| Offender Name | ond.name | Deceased offender name |
| Offender ID | ond.id_no | NRIC/FIN of deceased |
| Type | ond.owner_driver_indicator | H (Hirer) or D (Driver) |
| Date of Death | ond.date_of_death | When offender died |
| Suspension Date | sn.date_of_suspension | When PS-RP2 applied |
| Offence Date | von.notice_date_and_time | Original offence date |

---

## 6. Tab 4: Section_3_Redirect_RIP

### 6.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | Redirect PS-RIP/RP2 Notices |
| Section | 3.4 |
| Trigger | OIC manual action from Staff Portal |
| Frequency | On-demand (manual) |
| Systems Involved | Staff Portal, Backend API, Database |
| Expected Outcome | Notice redirected to new offender |

### 6.2 Detailed Flow Diagram (ASCII)

```
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│                              REDIRECT PS-RIP/RP2 NOTICES                                     │
├────────────────────┬─────────────────────────┬─────────────────────┬────────────────────────┤
│   Staff Portal     │      Backend API        │      Database       │       Result           │
├────────────────────┼─────────────────────────┼─────────────────────┼────────────────────────┤
│                    │                         │                     │                        │
│ ┌────────────────┐ │                         │                     │                        │
│ │     Start      │ │                         │                     │                        │
│ └───────┬────────┘ │                         │                     │                        │
│         │          │                         │                     │                        │
│         ▼          │                         │                     │                        │
│ ┌────────────────┐ │                         │                     │                        │
│ │ OIC revives    │ │                         │                     │                        │
│ │ PS-RIP or      │ │                         │                     │                        │
│ │ PS-RP2 from    ├─┼────────────────────────►│                     │                        │
│ │ Staff Portal   │ │                         │                     │                        │
│ └───────┬────────┘ │                         │                     │                        │
│         │          │                         │                     │                        │
│         │          │ ┌─────────────────────┐ │                     │                        │
│         │          │ │ Update Suspension   │ │                     │                        │
│         │          │ │ date_of_revival =   ├─┼────────────────────►│                        │
│         │          │ │ NOW()               │ │ UPDATE              │                        │
│         │          │ │ revival_reason      │ │ ocms_suspended_     │                        │
│         │          │ └─────────┬───────────┘ │ notice              │                        │
│         │          │           │             │                     │                        │
│         │          │           │             │ UPDATE              │                        │
│         │          │           ├─────────────┼────────────────────►│                        │
│         │          │           │             │ ocms_valid_         │                        │
│         │          │           │             │ offence_notice      │                        │
│         │          │           │             │                     │                        │
│         ▼          │           ▼             │                     │                        │
│ ┌────────────────┐ │ ┌─────────────────────┐ │                     │                        │
│ │ OIC updates    │ │ │ Receive update      │ │                     │                        │
│ │ Owner/Hirer/   ├─┼►│ request and update  │ │                     │                        │
│ │ Driver details │ │ │ offender data       │ │                     │                        │
│ └───────┬────────┘ │ └─────────┬───────────┘ │                     │                        │
│         │          │           │             │                     │                        │
│         │          │           │             │ UPDATE              │                        │
│         │          │           ├─────────────┼────────────────────►│                        │
│         │          │           │             │ ocms_offence_       │                        │
│         │          │           │             │ notice_owner_driver │                        │
│         │          │           │             │                     │                        │
│         │          │           │             │ UPDATE              │                        │
│         │          │           ├─────────────┼────────────────────►│                        │
│         │          │           │             │ ocms_offence_notice_│                        │
│         │          │           │             │ owner_driver_addr   │                        │
│         │          │           │             │                     │                        │
│         │          │           ▼             │                     │                        │
│         │          │ ┌─────────────────────┐ │                     │                        │
│         │          │ │ Redirect Notice     │ │                     │                        │
│         │          │ │ to new offender     │ │                     │                        │
│         │          │ │ (update stage to    ├─┼────────────────────►│                        │
│         │          │ │  RD1 or DN1)        │ │ UPDATE              │                        │
│         │          │ └─────────┬───────────┘ │ ocms_valid_         │                        │
│         │          │           │             │ offence_notice      │                        │
│         │          │           │             │                     │                        │
│         │          │           ▼             │                     │                        │
│         │          │ ┌─────────────────────┐ │                     │                        │
│         │          │ │ Notice continues    │ │                     │ ┌────────────────────┐ │
│         │          │ │ along standard      ├─┼─────────────────────┼─┤ Notice redirected  │ │
│         │          │ │ processing flow     │ │                     │ │ to new offender    │ │
│         │          │ └─────────┬───────────┘ │                     │ └────────────────────┘ │
│         │          │           │             │                     │                        │
│         │          │           ▼             │                     │                        │
│         │          │      ┌──────────┐       │                     │                        │
│         │          │      │   End    │       │                     │                        │
│         │          │      └──────────┘       │                     │                        │
│         │          │                         │                     │                        │
└─────────┴──────────┴─────────────────────────┴─────────────────────┴────────────────────────┘
```

### 6.3 Process Steps Table

| Step | Type | Definition | Brief Description | Next Step |
| --- | --- | --- | --- | --- |
| 1 | Start | Start Redirect | OIC initiates from Staff Portal | 2 |
| 2 | Process | Revive PS | OIC lifts PS-RIP or PS-RP2 suspension | 3 |
| 3 | Process | Update Suspension Record | Set date_of_revival, revival_reason in DB | 4 |
| 4 | Process | Update Notice Status | Clear/update suspension_type in notice | 5 |
| 5 | Process | Update Offender Details | OIC enters new offender information | 6 |
| 6 | Process | Update Owner/Driver Table | Save new offender to database | 7 |
| 7 | Process | Update Address Table | Save new offender address | 8 |
| 8 | Process | Redirect Notice | Backend redirects to appropriate stage | 9 |
| 9 | Process | Continue Standard Flow | Notice proceeds with new offender | 10 |
| 10 | End | End | Redirect complete | - |

### 6.4 Database Operations

| Step | Operation | Database | Table | Query/Action |
| --- | --- | --- | --- | --- |
| 3 | UPDATE | Intranet | ocms_suspended_notice | `SET date_of_revival=NOW(), revival_reason=<reason>` |
| 4 | UPDATE | Intranet | ocms_valid_offence_notice | `SET suspension_type=NULL, epr_reason_of_suspension=NULL` |
| 6 | UPDATE | Intranet | ocms_offence_notice_owner_driver | `UPDATE offender details, SET offender_indicator='Y'` |
| 7 | UPDATE | Intranet | ocms_offence_notice_owner_driver_addr | `UPDATE address details` |
| 8 | UPDATE | Intranet | ocms_valid_offence_notice | `SET last_processing_stage='RD1' or 'DN1'` |

### 6.5 Redirect Scenarios

| Scenario | Description | Redirect To | New Stage |
| --- | --- | --- | --- |
| Wrongly Furnished RIP | RIP ID was incorrectly provided | Owner | RD1 or DN1 |
| NOK Furnishes Hirer | Next-of-Kin provides Hirer info | Hirer | RD1 or DN1 |
| NOK Furnishes Driver | Next-of-Kin provides Driver info | Driver | RD1 or DN1 |

---

## 7. Error Handling

| Error Point | Error Type | Condition | Handling | Recovery |
| --- | --- | --- | --- | --- |
| Step 2 (Detect) | MHA Error | MHA connection fails | Log error, retry | Continue without update |
| Step 2 (Detect) | DataHive Error | D90 query fails | Default life_status='A' | Normal processing |
| Step 8 (Suspend) | DB Error | Insert fails | Rollback, log error | Manual intervention |
| Step 6 (Report) | Email Error | Email service fails | Log error | Retry, manual send |
| Step 6 (Redirect) | Validation Error | Invalid offender data | Return error to UI | OIC corrects data |

---

## 8. Output Mapping

### 8.1 Suspension Codes

| Code | Suspension Type | Full Name | Stored In |
| --- | --- | --- | --- |
| RIP | PS | Motorist Deceased On or After Offence Date | ocms_valid_offence_notice.epr_reason_of_suspension |
| RP2 | PS | Motorist Deceased Before Offence Date | ocms_valid_offence_notice.epr_reason_of_suspension |

### 8.2 Data Flow to Next Process

| After Suspension | Next Processing | Reference |
| --- | --- | --- |
| PS-RIP applied | Wait for OIC review/redirect | OCMS 14 Section 4.8 |
| PS-RP2 applied | Include in daily report (if H/D) | OCMS 14 Section 4.7 |
| PS revived & redirected | Continue standard flow | OCMS 11 |

---

## 9. Flowchart Checklist

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
- [x] Return values are mapped

---

## 10. Notes for Technical Flowchart Creation

### 10.1 Shape Guidelines

| Element | Shape | Style |
| --- | --- | --- |
| Start/End | Terminator (rounded rectangle) | strokeWidth=2 |
| Process | Rectangle | rounded=1, arcSize=14 |
| Decision | Diamond | shape=mxgraph.flowchart.decision |
| Database | Cylinder | shape=cylinder |
| External Call | Rectangle with double border | shape=process |

### 10.2 Connector Guidelines

| Connection Type | Style |
| --- | --- |
| Normal flow | Solid arrow |
| Database operation | Dashed line |
| External system call | Dashed line |
| Error path | Red solid arrow |

### 10.3 Label Guidelines

| Decision | Yes Label | No Label |
| --- | --- | --- |
| life_status = 'D'? | Yes (Deceased) | No (Alive) |
| date_of_death >= offence_date? | Yes (RIP) | No (RP2) |
| Any records? | Yes (Has records) | No (Empty) |

### 10.4 API Payload Box Format

For suspension operations, include payload boxes with:

```
┌─────────────────────────────────────┐
│ Apply PS-RIP/RP2 Suspension         │
├─────────────────────────────────────┤
│ noticeNo: "A12345678"               │
│ suspensionSource: "CRON"            │
│ reasonOfSuspension: "RIP" or "RP2"  │
│ officerAuthorisingSuspension:       │
│   "SYSTEM"                          │
└─────────────────────────────────────┘
```

---

## 11. Changelog

| Version | Date | Author | Changes |
| --- | --- | --- | --- |
| 1.0 | 15/01/2026 | Claude | Initial version based on plan_api.md and plan_condition.md |
| 1.1 | 19/01/2026 | Claude | Yi Jie compliance: Fixed field name consistency (reason_suspension_date → date_of_suspension) |

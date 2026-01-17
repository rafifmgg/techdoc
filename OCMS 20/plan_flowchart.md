# OCMS 20 - Unclaimed Reminders: Flowchart Plan

## Document Information

| Item | Value |
|------|-------|
| Version | 1.0 |
| Date | 2026-01-07 |
| Feature | Unclaimed Reminders & Batch Data Report |
| Prerequisites | plan_api.md, plan_condition.md |

---

## 1. Flowchart Overview

### 1.1 Diagrams to Create

| Tab | Name | Type | Description |
|-----|------|------|-------------|
| 2.1 | High-Level Process Flow | Overview | End-to-end unclaimed reminders process |
| 3.1 | User Journey - Unclaimed Form | Detailed | OIC interaction with Staff Portal |
| 3.2 | Portal Query & Display Logic | Detailed | Frontend validation and display handling |
| 3.3 | Backend Processing - Submit Unclaimed | Detailed | API processing with DB operations |
| 3.4 | Backend Processing - Generate Batch Report | Detailed | Cron job for MHA/DataHive results |

### 1.2 Swimlanes

| Swimlane | Color | Description |
|----------|-------|-------------|
| Frontend (Staff Portal) | Light Blue | OIC interactions, form display, validations |
| Backend (OCMS API) | Light Green | API processing, business logic |
| Database | Light Yellow | DB queries, inserts, updates |
| External System | Light Orange | MHA, DataHive, Blob Storage |

---

## 2. High-Level Flow (Tab 2.1)

### 2.1 Process Flow - Unclaimed Reminders

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        HIGH-LEVEL PROCESS FLOW                                   │
│                        Unclaimed Reminders                                       │
└─────────────────────────────────────────────────────────────────────────────────┘

  ┌───────┐
  │ Start │
  └───┬───┘
      │
      ▼
┌─────────────────┐
│ Reminder Letter │ (Manual trigger)
│ returned to URA │
└───────┬─────────┘
        │
        ▼
┌─────────────────────────┐
│ OIC launches OCMS Staff │
│ Portal Unclaimed module │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐     ┌─────────────────────┐
│ OIC fills up Unclaimed  │────▶│ Refer to Section    │
│ Reminder form           │     │ 3.1 User Journey    │
└───────────┬─────────────┘     └─────────────────────┘
            │
            ▼
┌─────────────────────────┐
│ OIC submits Unclaimed   │
│ Reminder & portal       │
│ exports Unclaimed Report│
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐     ┌─────────────────────┐
│ OCMS Backend processes  │────▶│ Refer to Section    │
│ the record              │     │ 3.3 Backend Process │
└───────────┬─────────────┘     └─────────────────────┘
            │
            ▼
┌─────────────────────────┐     ┌─────────────────────┐
│ OCMS Backend query      │────▶│ Refer to Section    │
│ MHA/DataHive for        │     │ 3.4 Batch Report    │
│ latest address          │     └─────────────────────┘
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ OCMS Backend generates  │
│ Unclaimed Batch Data    │
│ Report & store to Blob  │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ OIC downloads Unclaimed │
│ Batch Data Report from  │
│ Staff Portal            │
└───────────┬─────────────┘
            │
            ▼
        ┌───────┐
        │  End  │
        └───────┘
```

### 2.2 Flow Steps Description

| Step | Actor | Action | Output |
|------|-------|--------|--------|
| 1 | Manual | Reminder Letter returned to URA | Trigger for OIC action |
| 2 | OIC | Launch OCMS Staff Portal Unclaimed module | Access to Unclaimed form |
| 3 | OIC | Fill up Unclaimed Reminder form | Form data ready |
| 4 | OIC | Submit Unclaimed Reminder | API call to backend |
| 5 | Backend | Process record, apply TS-UNC | Suspension applied |
| 6 | Backend | Queue for MHA/DataHive query | Record in queue |
| 7 | Cron | MHA/DataHive returns results | Address data |
| 8 | Backend | Generate Batch Data Report | Excel report in Blob |
| 9 | OIC | Download Batch Data Report | Report for follow-up |

---

## 3. Detailed Flows

### 3.1 User Journey - Unclaimed Form (Tab 3.1)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ SWIMLANE: Frontend (Staff Portal)                                               │
└─────────────────────────────────────────────────────────────────────────────────┘

  ┌───────┐
  │ Start │
  └───┬───┘
      │
      ▼
┌─────────────────────────┐
│ OIC launches OCMS Staff │
│ Portal Unclaimed module │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ OIC selects "Reminders" │
└───────────┬─────────────┘
            │
            ▼
      ┌─────────────┐
      │ Input Mode? │
      └──────┬──────┘
             │
     ┌───────┴───────┐
     │               │
     ▼               ▼
┌─────────┐    ┌───────────────┐
│ Manual  │    │ Upload Excel  │
│ Entry   │    │               │
└────┬────┘    └───────┬───────┘
     │                 │
     ▼                 ▼
┌─────────────┐  ┌─────────────────┐
│ OIC enters  │  │ Page displays   │
│ Notice No   │  │ Upload Excel UI │
└──────┬──────┘  └────────┬────────┘
       │                  │
       │                  ▼
       │         ┌─────────────────┐
       │         │ OIC uploads     │
       │         │ template file   │
       │         └────────┬────────┘
       │                  │
       │                  ▼
       │         ┌─────────────────┐
       │         │ Any data errors │
       │         │ in submission?  │
       │         └────────┬────────┘
       │                  │
       │          ┌───────┴───────┐
       │          │ Yes           │ No
       │          ▼               │
       │   ┌─────────────┐        │
       │   │ Display     │        │
       │   │ error list  │        │
       │   └──────┬──────┘        │
       │          │               │
       │          ▼               │
       │   ┌─────────────┐        │
       │   │ OIC corrects│        │
       │   │ errors      │        │
       │   └──────┬──────┘        │
       │          │               │
       │          └───────────────┤
       │                          │
       └──────────────────────────┤
                                  │
                                  ▼
                    ┌─────────────────────┐
                    │ OIC clicks Submit   │
                    └──────────┬──────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│ PROCESS: Portal submit query & process results                                   │
│ (Refer to Section 3.2)                                                          │
└─────────────────────────────────────────────────────────────────────────────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │ OIC reviews         │
                    │ Unclaimed Reminder  │
                    │ form with Notice &  │
                    │ Letter details      │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │ OIC fills up Return │
                    │ information         │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │ OIC clicks Export   │
                    │ (OPTIONAL)          │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │ OIC clicks Submit   │
                    └──────────┬──────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│ PROCESS: Backend process Unclaimed Reminders                                     │
│ (Refer to Section 3.3)                                                          │
└─────────────────────────────────────────────────────────────────────────────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │ Portal displays     │
                    │ "Unclaimed Report   │
                    │ generated" message  │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │ Portal auto download│
                    │ Unclaimed Reminder  │
                    │ Report              │
                    └──────────┬──────────┘
                               │
                               ▼
                           ┌───────┐
                           │  End  │
                           └───────┘
```

---

### 3.2 Portal Query & Display Logic (Tab 3.2)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ SWIMLANE: Frontend (Staff Portal) + Database                                    │
└─────────────────────────────────────────────────────────────────────────────────┘

  ┌─────────────────────────┐
  │ OIC submits Notice      │
  │ number(s) to retrieve   │
  │ Notice and Letter       │
  │ details                 │
  └───────────┬─────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│ PROCESS: Portal submit query & process results                                   │
└─────────────────────────────────────────────────────────────────────────────────┘
              │
              ▼
  ┌─────────────────────────┐     ┌─────────────────────────────────────┐
  │ Portal query RDP/DN DB  │────▶│ DB: ocms_request_driver_particulars │
  │ using Notice Number     │     │ DB: ocms_driver_notice              │
  └───────────┬─────────────┘     │ Query Param: notice_no              │
              │                   └─────────────────────────────────────┘
              ▼
        ┌───────────────┐
        │ Any result    │
        │ returned?     │
        └───────┬───────┘
                │
        ┌───────┴───────┐
        │ No            │ Yes
        ▼               ▼
  ┌───────────┐   ┌─────────────────────────┐
  │ Display   │   │ Portal query VON DB to  │
  │ "Notice   │   │ retrieve offence details│
  │ not found"│   └───────────┬─────────────┘
  └───────────┘               │
                              ▼
                        ┌───────────────┐
                        │ Any result    │
                        │ returned?     │
                        └───────┬───────┘
                                │
                        ┌───────┴───────┐
                        │ No            │ Yes
                        ▼               ▼
                  ┌───────────┐   ┌─────────────────────────┐
                  │ Display   │   │ Portal read Notice      │
                  │ error     │   │ details and handle      │
                  └───────────┘   │ page display            │
                                  └───────────┬─────────────┘
                                              │
                                              ▼
                                  ┌─────────────────────────┐
                                  │ Suspension Type = PS?   │
                                  └───────────┬─────────────┘
                                              │
                                      ┌───────┴───────┐
                                      │ Yes           │ No
                                      ▼               ▼
                              ┌─────────────┐   ┌─────────────────────────┐
                              │ PAGE DISPLAY│   │ LPS = Non-Court stage?  │
                              │ HANDLING:   │   └───────────┬─────────────┘
                              │             │               │
                              │ Current susp│       ┌───────┴───────┐
                              │ = "PS/XXX"  │       │ No            │ Yes
                              │             │       ▼               ▼
                              │ Susp Code   │ ┌─────────────┐ ┌─────────────────┐
                              │ = Disabled  │ │ PAGE DISPLAY│ │ Suspension Type │
                              │             │ │ HANDLING:   │ │ = TS?           │
                              │ Period      │ │             │ └────────┬────────┘
                              │ = Disabled  │ │ Current susp│          │
                              │             │ │ = "Court"   │  ┌───────┴───────┐
                              │ Remarks     │ │             │  │ Yes           │ No
                              │ = Disabled  │ │ Susp Code   │  ▼               ▼
                              └──────┬──────┘ │ = Disabled  │ ┌─────────┐ ┌─────────┐
                                     │        │             │ │ PAGE    │ │ Notice  │
                                     │        │ Period      │ │ DISPLAY │ │ is not  │
                                     │        │ = Disabled  │ │ TS/XXX  │ │suspended│
                                     │        │             │ │ UNC     │ │ "-"     │
                                     │        │ Remarks     │ │ Editable│ │ UNC     │
                                     │        │ = Disabled  │ └────┬────┘ │ Editable│
                                     │        └──────┬──────┘      │      └────┬────┘
                                     │               │             │           │
                                     └───────────────┴─────────────┴───────────┘
                                                           │
                                                           ▼
                                              ┌─────────────────────────┐
                                              │ Page stores the records │
                                              │ in temp memory          │
                                              └───────────┬─────────────┘
                                                          │
                                                          ▼
                                              ┌─────────────────────────┐
                                              │ Display form to OIC     │
                                              └─────────────────────────┘
```

---

### 3.3 Backend Processing - Submit Unclaimed (Tab 3.3)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ SWIMLANES: Backend + Database                                                    │
└─────────────────────────────────────────────────────────────────────────────────┘

  ┌─────────────────────────┐
  │ Staff Portal submits    │
  │ Unclaimed Reminder      │
  │ Record(s) to Backend    │
  │ via API                 │
  │ POST /v1/submit-unclaim │
  └───────────┬─────────────┘
              │
              ▼
  ┌─────────────────────────┐
  │ OCMS Backend receives   │
  │ the API request data    │
  └───────────┬─────────────┘
              │
              ▼
  ┌─────────────────────────┐
  │ Validate request:       │
  │ - noticeNo required     │
  │ - reasonOfSuspension    │
  │   must be "UNC"         │
  └───────────┬─────────────┘
              │
              ▼
        ┌───────────────┐
        │ Validation    │
        │ passed?       │
        └───────┬───────┘
                │
        ┌───────┴───────┐
        │ No            │ Yes
        ▼               │
  ┌───────────────┐     │
  │ Return error  │     │
  │ OCMS-4000/    │     │
  │ 4003/4004     │     │
  └───────────────┘     │
                        ▼
              ┌─────────────────────────┐
              │ Filter records with     │
              │ Suspension Code = UNC   │
              └───────────┬─────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│ PROCESS: Update RDP/DN and apply TS-UNC                                         │
│ FOR EACH record in submission:                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
                          │
                          ▼
              ┌─────────────────────────┐     ┌─────────────────────────────────┐
              │ Query VON table to      │────▶│ DB: ocms_valid_offence_notice   │
              │ validate notice exists  │     │ SELECT * WHERE notice_no = ?    │
              └───────────┬─────────────┘     └─────────────────────────────────┘
                          │
                          ▼
                    ┌───────────────┐
                    │ Notice found? │
                    └───────┬───────┘
                            │
                    ┌───────┴───────┐
                    │ No            │ Yes
                    ▼               ▼
              ┌───────────┐   ┌─────────────────────────┐
              │ Log error │   │ Offender Indicator      │
              │ skip      │   │ = Driver?               │
              │ record    │   └───────────┬─────────────┘
              └───────────┘               │
                                  ┌───────┴───────┐
                                  │ Yes           │ No
                                  ▼               ▼
                          ┌─────────────┐   ┌─────────────┐
                          │ Query DN    │   │ Query RDP   │
                          │ table       │   │ table       │
                          └──────┬──────┘   └──────┬──────┘
                                 │                 │
                                 └────────┬────────┘
                                          │
                                          ▼
                                    ┌───────────────┐
                                    │ Record found? │
                                    └───────┬───────┘
                                            │
                                    ┌───────┴───────┐
                                    │ No            │ Yes
                                    ▼               ▼
                              ┌───────────┐   ┌─────────────────────────┐
                              │ Log debug │   │ UPDATE RDP/DN with:     │
                              │ continue  │   │ - date_of_return        │
                              └───────────┘   │ - unclaimed_reason      │
                                              └───────────┬─────────────┘
                                                          │
                                                          ▼
                                                    ┌───────────────┐
                                                    │ Update        │
                                                    │ success?      │
                                                    └───────┬───────┘
                                                            │
                                                    ┌───────┴───────┐
                                                    │ No            │ Yes
                                                    ▼               │
                                              ┌───────────┐         │
                                              │ Retry 1x  │         │
                                              └──────┬────┘         │
                                                     │              │
                                                     ▼              │
                                              ┌───────────────┐     │
                                              │ Retry success?│     │
                                              └───────┬───────┘     │
                                                      │             │
                                              ┌───────┴───────┐     │
                                              │ No            │ Yes │
                                              ▼               │     │
                                        ┌───────────┐        │     │
                                        │ Log error │        │     │
                                        │ continue  │        │     │
                                        └───────────┘        │     │
                                                             └─────┤
                                                                   │
                                                                   ▼
                                              ┌─────────────────────────┐
                                              │ Get MAX(sr_no) from     │
                                              │ ocms_suspended_notice   │
                                              │ for this notice         │
                                              └───────────┬─────────────┘
                                                          │
                                                          ▼
                                              ┌─────────────────────────┐
                                              │ CREATE suspension       │
                                              │ record in               │
                                              │ ocms_suspended_notice:  │
                                              │ - sr_no = MAX + 1       │
                                              │ - suspension_type = TS  │
                                              │ - reason = UNC          │
                                              │ - due_date_of_revival   │
                                              └───────────┬─────────────┘
                                                          │
                                                          ▼
                                                    ┌───────────────┐
                                                    │ Any record    │
                                                    │ fail?         │
                                                    └───────┬───────┘
                                                            │
                                                    ┌───────┴───────┐
                                                    │ Yes           │ No
                                                    ▼               │
                                              ┌───────────┐         │
                                              │ Retry 1x  │         │
                                              └──────┬────┘         │
                                                     │              │
                                                     ▼              │
                                              ┌───────────────┐     │
                                              │ Retry success?│     │
                                              └───────┬───────┘     │
                                                      │             │
                                              ┌───────┴───────┐     │
                                              │ No            │ Yes │
                                              ▼               │     │
                                        ┌───────────┐        │     │
                                        │ Log error │        │     │
                                        │ continue  │        │     │
                                        └───────────┘        │     │
                                                             └─────┤
                                                                   │
                                                                   ▼
                                              ┌─────────────────────────┐
                                              │ Any record left to      │
                                              │ process?                │
                                              └───────────┬─────────────┘
                                                          │
                                                  ┌───────┴───────┐
                                                  │ Yes           │ No
                                                  │ (Loop back)   │
                                                  └───────────────┤
                                                                  │
                                                                  ▼
                                              ┌─────────────────────────┐
                                              │ INSERT to ocms_nro_temp │
                                              │ for MHA/DataHive query  │
                                              │ - query_reason = 'UNC'  │
                                              └───────────┬─────────────┘
                                                          │
                                                          ▼
                                              ┌─────────────────────────┐
                                              │ Generate Unclaimed      │
                                              │ Reminder Report (Excel) │
                                              └───────────┬─────────────┘
                                                          │
                                                          ▼
                                                    ┌───────────────┐
                                                    │ Report gen    │
                                                    │ success?      │
                                                    └───────┬───────┘
                                                            │
                                                    ┌───────┴───────┐
                                                    │ No            │ Yes
                                                    ▼               ▼
                                              ┌───────────┐   ┌─────────────────┐
                                              │ Return    │   │ Upload to Blob  │
                                              │ OCMS-5001 │   │ Storage         │
                                              └───────────┘   └────────┬────────┘
                                                                       │
                                                                       ▼
                                                                 ┌───────────────┐
                                                                 │ Upload        │
                                                                 │ success?      │
                                                                 └───────┬───────┘
                                                                         │
                                                                 ┌───────┴───────┐
                                                                 │ No            │ Yes
                                                                 ▼               ▼
                                                           ┌───────────┐   ┌─────────────┐
                                                           │ Return    │   │ Check for   │
                                                           │ OCMS-5004 │   │ any errors  │
                                                           └───────────┘   │ logged      │
                                                                           └──────┬──────┘
                                                                                  │
                                                                          ┌───────┴───────┐
                                                                          │ Yes           │ No
                                                                          ▼               ▼
                                                                    ┌───────────┐   ┌───────────────┐
                                                                    │ Send error│   │ Respond to    │
                                                                    │ email     │   │ Staff Portal  │
                                                                    │ (TODO)    │   │ with Report   │
                                                                    └─────┬─────┘   │ URL           │
                                                                          │         └───────┬───────┘
                                                                          └─────────────────┤
                                                                                            │
                                                                                            ▼
                                                                                        ┌───────┐
                                                                                        │  End  │
                                                                                        └───────┘
```

---

### 3.4 Backend Processing - Generate Batch Data Report (Tab 3.4)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ SWIMLANES: Backend (Cron) + External System + Database                          │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│ PRE-CONDITION: IDs for query are added to ocms_nro_temp                         │
└─────────────────────────────────────────────────────────────────────────────────┘

  ┌─────────────────────────┐
  │ Wait for MHA enquiry    │
  │ cron                    │
  └───────────┬─────────────┘
              │
              ▼
  ┌─────────────────────────┐
  │ CRON: MHA enquiry cron  │
  │ starts                  │
  └───────────┬─────────────┘
              │
              ▼
  ┌─────────────────────────┐     ┌─────────────────────────────────┐
  │ Query ocms_nro_temp for │────▶│ DB: ocms_nro_temp               │
  │ IDs to send to MHA      │     │ WHERE query_reason = 'UNC'      │
  │                         │     │ AND processed = 'N'             │
  └───────────┬─────────────┘     └─────────────────────────────────┘
              │
              ▼
  ┌─────────────────────────┐     ┌─────────────────────────────────┐
  │ Generate MHA enquiry    │────▶│ EXTERNAL: MHA SFTP              │
  │ file and send to MHA    │     │ Drop file to SFTP               │
  └───────────┬─────────────┘     └─────────────────────────────────┘
              │
              ▼
  ┌─────────────────────────┐
  │ Wait for MHA result     │
  │ cron                    │
  └───────────┬─────────────┘
              │
              ▼
  ┌─────────────────────────┐
  │ CRON: MHA results       │
  │ processing starts       │
  └───────────┬─────────────┘
              │
              ▼
  ┌─────────────────────────┐
  │ Result is for Notice    │
  │ that will change stage? │
  └───────────┬─────────────┘
              │
      ┌───────┴───────┐
      │ Yes           │ No
      ▼               ▼
┌───────────────┐   ┌─────────────────────────┐
│ Continue      │   │ Store address in        │
│ standard flow │   │ ocms_temp_unc_hst_addr  │
│ (update ONOD) │   │ - query_reason = 'UNC'  │
└───────────────┘   └───────────┬─────────────┘
                                │
                                ▼
                    ┌─────────────────────────┐
                    │ DataHive query for      │
                    │ NRIC, FIN and UEN       │
                    └───────────┬─────────────┘
                                │
                                ▼
                    ┌─────────────────────────┐
                    │ DataHive results        │
                    │ processed               │
                    └───────────┬─────────────┘
                                │
                                ▼
                    ┌─────────────────────────┐
                    │ Result is for Notice    │
                    │ that will change stage? │
                    └───────────┬─────────────┘
                                │
                        ┌───────┴───────┐
                        │ Yes           │ No
                        ▼               ▼
                  ┌───────────────┐   ┌─────────────────────────┐
                  │ Continue      │   │ Store address in        │
                  │ standard flow │   │ ocms_temp_unc_hst_addr  │
                  │ (update ONOD) │   └───────────┬─────────────┘
                  └───────────────┘               │
                                                  ▼
                                    ┌─────────────────────────┐
                                    │ Consolidate all MHA and │
                                    │ DataHive results in     │
                                    │ ocms_temp_unc_hst_addr  │
                                    └───────────┬─────────────┘
                                                │
                                                ▼
                                    ┌─────────────────────────┐
                                    │ Check for unprocessed   │
                                    │ UNC results             │
                                    │ (report_generated != Y) │
                                    └───────────┬─────────────┘
                                                │
                                                ▼
                                          ┌───────────────┐
                                          │ Records       │
                                          │ found?        │
                                          └───────┬───────┘
                                                  │
                                          ┌───────┴───────┐
                                          │ No            │ Yes
                                          ▼               ▼
                                    ┌───────────┐   ┌─────────────────────────┐
                                    │ Return    │   │ Generate Unclaimed      │
                                    │ "No data" │   │ Batch Data Report       │
                                    └───────────┘   │ (15 columns)            │
                                                    └───────────┬─────────────┘
                                                                │
                                                                ▼
                                                          ┌───────────────┐
                                                          │ Report gen    │
                                                          │ success?      │
                                                          └───────┬───────┘
                                                                  │
                                                          ┌───────┴───────┐
                                                          │ No            │ Yes
                                                          ▼               ▼
                                                    ┌───────────┐   ┌─────────────────┐
                                                    │ Retry 1x  │   │ Upload to Blob  │
                                                    └─────┬─────┘   │ Storage         │
                                                          │         └────────┬────────┘
                                                          ▼                  │
                                                    ┌───────────────┐        │
                                                    │ Still failed? │        │
                                                    └───────┬───────┘        │
                                                            │                │
                                                    ┌───────┴───────┐        │
                                                    │ Yes           │ No     │
                                                    ▼               └────────┤
                                              ┌───────────┐                  │
                                              │ Throw     │                  │
                                              │ Exception │                  │
                                              └───────────┘                  │
                                                                             ▼
                                                              ┌─────────────────────────┐
                                                              │ Upload success?         │
                                                              └───────────┬─────────────┘
                                                                          │
                                                                  ┌───────┴───────┐
                                                                  │ No            │ Yes
                                                                  ▼               ▼
                                                            ┌───────────┐   ┌─────────────────┐
                                                            │ Retry 1x  │   │ Save report URL │
                                                            └───────────┘   │ to DB           │
                                                                            └────────┬────────┘
                                                                                     │
                                                                                     ▼
                                                                      ┌─────────────────────────┐
                                                                      │ Mark UNC results as     │
                                                                      │ processed               │
                                                                      │ (report_generated = 'Y')│
                                                                      └───────────┬─────────────┘
                                                                                  │
                                                                                  ▼
                                                                      ┌─────────────────────────┐
                                                                      │ Report available in     │
                                                                      │ Staff Portal Report     │
                                                                      │ module                  │
                                                                      └───────────┬─────────────┘
                                                                                  │
                                                                                  ▼
                                                                              ┌───────┐
                                                                              │  End  │
                                                                              └───────┘
```

---

## 4. Database Operations Summary

### 4.1 Per Flow Step

| Flow | Step | Table | Operation | Fields |
|------|------|-------|-----------|--------|
| 3.2 | Query Notice | ocms_request_driver_particulars | SELECT | notice_no |
| 3.2 | Query Notice | ocms_driver_notice | SELECT | notice_no |
| 3.2 | Query VON | ocms_valid_offence_notice | SELECT | notice_no |
| 3.3 | Validate Notice | ocms_valid_offence_notice | SELECT | notice_no |
| 3.3 | Get Offender | ocms_offence_notice_owner_driver | SELECT | notice_no, offender_indicator |
| 3.3 | Update RDP | ocms_request_driver_particulars | UPDATE | date_of_return, unclaimed_reason |
| 3.3 | Update DN | ocms_driver_notice | UPDATE | date_of_return, reason_for_unclaim |
| 3.3 | Get Max SR | ocms_suspended_notice | SELECT | MAX(sr_no) |
| 3.3 | Create Suspension | ocms_suspended_notice | INSERT | All suspension fields |
| 3.3 | Queue for MHA | ocms_nro_temp | INSERT | notice_no, id_no, query_reason |
| 3.4 | Get MHA Queue | ocms_nro_temp | SELECT | query_reason = 'UNC' |
| 3.4 | Store Results | ocms_temp_unc_hst_addr | INSERT | Address fields |
| 3.4 | Get UNC Data | ocms_temp_unc_hst_addr | SELECT | query_reason = 'UNC', report_generated != 'Y' |
| 3.4 | Mark Processed | ocms_temp_unc_hst_addr | UPDATE | report_generated = 'Y' |

---

## 5. Error Handling Summary

| Flow | Error Scenario | Action | Error Code |
|------|----------------|--------|------------|
| 3.3 | noticeNumbers missing | Return error | OCMS-4000 |
| 3.3 | noticeNo missing | Return error | OCMS-4003 |
| 3.3 | reasonOfSuspension != UNC | Return error | OCMS-4004 |
| 3.3 | Notice not found | Skip record, log | - |
| 3.3 | RDP/DN update failed | Retry 1x, continue | - |
| 3.3 | Suspension insert failed | Retry 1x, log | - |
| 3.3 | Report generation failed | Return error | OCMS-5001 |
| 3.3 | Blob upload failed | Return error | OCMS-5004 |
| 3.4 | No UNC data found | Return "No data" | - |
| 3.4 | Report generation failed | Retry 1x, throw exception | - |
| 3.4 | Blob upload failed | Retry 1x | - |

---

## 6. Notes for Flowchart Creation

### 6.1 Shape Legend

| Shape | Meaning |
|-------|---------|
| Rounded Rectangle | Start/End |
| Rectangle | Process/Action |
| Rectangle (double border) | Sub-process (reference to another flow) |
| Diamond | Decision |
| Parallelogram | Input/Output |
| Cylinder | Database |
| Cloud | External System |

### 6.2 Color Coding

| Element | Color |
|---------|-------|
| Frontend swimlane | #DAE8FC (Light Blue) |
| Backend swimlane | #D5E8D4 (Light Green) |
| Database swimlane | #FFF2CC (Light Yellow) |
| External System swimlane | #FFE6CC (Light Orange) |
| Error path | #F8CECC (Light Red) |
| Success path | #D5E8D4 (Light Green) |
| Reference box | #E1D5E7 (Light Purple) |

### 6.3 Connector Labels

| Label | Meaning |
|-------|---------|
| Yes/No | Decision outcome |
| Loop back | Return to previous step |
| Refer to Section X.X | Sub-process reference |
| OCMS-XXXX | Error code |

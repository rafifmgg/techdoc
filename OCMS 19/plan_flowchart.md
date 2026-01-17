# OCMS 19 - Revive Suspensions: Flowchart Planning Document

## Document Information
| Attribute | Value |
|-----------|-------|
| Version | 1.0 |
| Date | 2026-01-07 |
| OCMS Number | OCMS 19 |
| Feature | Revive Suspensions |
| Source | Functional Document v1.1, Functional Flow Diagram (OCMS_19_Functional_Flow.drawio) |

---

## 1. Flowchart Tab Structure

Based on the Functional Flow diagram (OCMS_19_Functional_Flow.drawio), the Technical Flowchart should have the following tabs:

| Tab No | Tab Name | Description |
|--------|----------|-------------|
| 3.1 | Revive-High-Level | High-level overview of revival processing |
| 3.2 | UI-side-Revive_validation | Staff Portal UI-Side validation flow |
| 3.3 | Backend_Revive_Validate_High_Level | Backend validation high-level flow |
| 3.4 | Backend_Revive_PS_Main | Main PS revival processing flow |
| 3.5 | Backend_Revive_TS_Main | Main TS revival processing flow |
| 3.6 | Backend-Replace_PS | Replace PS with new PS flow |
| 3.7 | OCMS_Portal_Manual_Revive | OCMS Staff Portal manual revival flow |
| 3.8 | PLUS_Portal_Manual_Revive | PLUS Staff Portal manual revival flow |

---

## 2. Swimlane Configuration

Use **3 swimlanes** for all flows:

| Swimlane | Description |
|----------|-------------|
| Frontend | Staff Portal UI (OCMS or PLUS) |
| Backend | OCMS Backend + Database operations |
| External System | PLUS Backend (only for PLUS integration flows) |

---

## 3. High-Level Flow (Tab 3.1: Revive-High-Level)

### 3.1 Overview
Two main entry points for revival:
1. **Manual Revive from Staff Portal** (OCMS or PLUS)
2. **Auto trigger by OCMS Backend** (Cron job)

### 3.2 Flow Diagram

```
                    ┌──────────────────────┐
                    │       START          │
                    └──────────┬───────────┘
                               │
                               ▼
        ┌──────────────────────────────────────────┐
        │           Entry Points:                   │
        │  • OCMS apply Revival (Manual)           │
        │  • PLUS apply Revival (Manual)           │
        │  • OCMS Backend Cron job (Auto)          │
        └──────────────────┬───────────────────────┘
                           │
                           ▼
        ┌──────────────────────────────────────────┐
        │    Staff Portal check if allowed         │
        └──────────────────┬───────────────────────┘
                           │
                           ▼
                    ◇─────────────◇
                    │  Revival    │
                    │  allowed?   │
                    ◇──────┬──────◇
                     Yes   │   No
              ┌────────────┴────────────┐
              │                         │
              ▼                         ▼
┌─────────────────────────┐   ┌─────────────────────┐
│ Staff Portal call       │   │ Show error message  │
│ backend API             │   └──────────┬──────────┘
└────────────┬────────────┘              │
             │                           ▼
             ▼                    ┌──────────────┐
┌─────────────────────────────┐  │     END      │
│ OCMS Backend check if       │  └──────────────┘
│ Revival is allowed          │
│ (PROCESS)                   │
└────────────┬────────────────┘
             │
             ▼
      ◇─────────────◇
      │  Revival    │
      │  allowed?   │
      ◇──────┬──────◇
       Yes   │   No
  ┌──────────┴──────────┐
  │                     │
  ▼                     ▼
┌───────────────┐   ┌─────────────────────┐
│ OCMS Backend  │   │ Respond with error  │
│ revive        │   │ message             │
│ Suspension    │   └──────────┬──────────┘
│ (PROCESS)     │              │
└───────┬───────┘              ▼
        │                ┌──────────────┐
        ├────────────┐   │     END      │
        │            │   └──────────────┘
        ▼            ▼
┌───────────────┐  ◇─────────────────◇
│ Respond       │  │ Follow-up action │
│ revival       │  │ required?        │
│ result        │  ◇────────┬────────◇
└───────┬───────┘     Yes   │   No
        │         ┌─────────┴─────────┐
        ▼         │                   │
┌──────────────┐  ▼                   ▼
│     END      │ ┌─────────────┐ ┌──────────────┐
└──────────────┘ │ Backend     │ │     END      │
                 │ initiate    │ └──────────────┘
                 │ follow-up   │
                 │ action      │
                 └──────┬──────┘
                        │
                        ▼
                 ┌──────────────┐
                 │     END      │
                 └──────────────┘
```

---

## 4. UI-Side Revive Validation (Tab 3.2: UI-side-Revive_validation)

### 4.1 Purpose
Staff Portal performs first-level validation before sending to backend

### 4.2 Swimlanes
| Swimlane | Actor |
|----------|-------|
| Frontend | Staff Portal (OCMS/PLUS) |

### 4.3 Flow Diagram

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                              FRONTEND                                         │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────┐                                                     │
│  │ OIC access Staff    │                                                     │
│  │ Portal              │                                                     │
│  └──────────┬──────────┘                                                     │
│             │                                                                │
│             ▼                                                                │
│  ┌─────────────────────────────────┐                                         │
│  │ OIC browse and select Notice(s) │                                         │
│  │ by clicking checkbox            │                                         │
│  └──────────┬──────────────────────┘                                         │
│             │                                                                │
│             ▼                                                                │
│  ┌─────────────────────────────────┐                                         │
│  │ OIC clicks on "Apply Operation" │                                         │
│  │ drop-down list                  │                                         │
│  └──────────┬──────────────────────┘                                         │
│             │                                                                │
│             ▼                                                                │
│      ◇─────────────────────────◇                                             │
│      │ OIC has permission to   │                                             │
│      │ perform Revival?        │                                             │
│      ◇───────────┬─────────────◇                                             │
│           Yes    │    No                                                     │
│      ┌───────────┴───────────┐                                               │
│      │                       │                                               │
│      ▼                       ▼                                               │
│  ┌───────────────────┐  ┌─────────────────────────┐                          │
│  │ Portal displays   │  │ Drop-down list will not │                          │
│  │ Apply Revival     │  │ display Apply Revival   │                          │
│  │ option            │  │ option                  │                          │
│  └─────────┬─────────┘  └──────────┬──────────────┘                          │
│            │                       │                                         │
│            ▼                       ▼                                         │
│  ┌───────────────────────┐   ┌──────────────┐                                │
│  │ OIC selects "Apply    │   │     END      │                                │
│  │ Revival" option       │   └──────────────┘                                │
│  └─────────┬─────────────┘                                                   │
│            │                                                                 │
│            ▼                                                                 │
│  ┌───────────────────────────┐                                               │
│  │ OIC clicks Select button  │                                               │
│  └─────────┬─────────────────┘                                               │
│            │                                                                 │
│            ▼                                                                 │
│  ┌───────────────────────────────┐                                           │
│  │ Staff Portal validation       │                                           │
│  │ initiated (PROCESS)           │                                           │
│  └─────────┬─────────────────────┘                                           │
│            │                                                                 │
│            ▼                                                                 │
│  ┌───────────────────────────────┐                                           │
│  │ Detect whether user selected  │                                           │
│  │ Notices with TS-HST           │                                           │
│  └─────────┬─────────────────────┘                                           │
│            │                                                                 │
│            ▼                                                                 │
│      ◇───────────────────────◇                                               │
│      │ Notice(s) contains    │                                               │
│      │ TS-HST?               │                                               │
│      ◇───────────┬───────────◇                                               │
│           Yes    │    No                                                     │
│      ┌───────────┴───────────┐                                               │
│      │                       │                                               │
│      ▼                       │                                               │
│  ┌───────────────────────┐   │                                               │
│  │ Display message:      │   │                                               │
│  │ "Revival is not       │   │                                               │
│  │ allowed for TS-HST    │   │                                               │
│  │ notices"              │   │                                               │
│  └─────────┬─────────────┘   │                                               │
│            │                 │                                               │
│            ▼                 │                                               │
│  ┌───────────────────────┐   │                                               │
│  │ OIC uncheck Notices   │◄──┘                                               │
│  │ that cannot be        │                                                   │
│  │ processed             │                                                   │
│  └─────────┬─────────────┘                                                   │
│            │                                                                 │
│            ▼                                                                 │
│  ┌───────────────────────────────┐                                           │
│  │ Detect whether more than 1    │                                           │
│  │ notice has PS suspensions     │                                           │
│  └─────────┬─────────────────────┘                                           │
│            │                                                                 │
│            ▼                                                                 │
│      ◇───────────────────────◇                                               │
│      │ More than 1 Notice    │                                               │
│      │ with PS?              │                                               │
│      ◇───────────┬───────────◇                                               │
│           Yes    │    No                                                     │
│      ┌───────────┴───────────┐                                               │
│      │                       │                                               │
│      ▼                       ▼                                               │
│  ┌───────────────────┐  ┌───────────────────────────┐                        │
│  │ Display message:  │  │ Portal redirect OIC to    │                        │
│  │ "You can only     │  │ Apply Suspension Detail   │                        │
│  │ revive one Notice │  │ page                      │                        │
│  │ with PS"          │  └─────────┬─────────────────┘                        │
│  └─────────┬─────────┘            │                                          │
│            │                      ▼                                          │
│            │           ┌───────────────────────────────┐                     │
│            │           │ Portal loads page and queries │                     │
│            │           │ DB: ocms_valid_offence_notice │                     │
│            │           └─────────┬─────────────────────┘                     │
│            │                     │                                           │
│            │                     ▼                                           │
│            │           ┌───────────────────────────────┐                     │
│            │           │ Portal displays list of       │                     │
│            │           │ Notice(s) and suspension      │                     │
│            │           │ details                       │                     │
│            │           └─────────┬─────────────────────┘                     │
│            │                     │                                           │
│            │                     ▼                                           │
│            │           ┌───────────────────────────────┐                     │
│            │           │ OIC selects Revival Reason    │                     │
│            │           │ and enters remarks (max 50)   │                     │
│            └──────────►│ Note: Same for all Notices    │◄────────────────┐   │
│                        └─────────┬─────────────────────┘                 │   │
│                                  │                                       │   │
│                                  ▼                                       │   │
│                        ┌───────────────────────┐                         │   │
│                        │ OIC clicks Apply      │                         │   │
│                        │ button                │                         │   │
│                        └─────────┬─────────────┘                         │   │
│                                  │                                       │   │
│                                  ▼                                       │   │
│                        ┌───────────────────────────┐                     │   │
│                        │ Portal displays           │                     │   │
│                        │ confirmation prompt       │                     │   │
│                        └─────────┬─────────────────┘                     │   │
│                                  │                                       │   │
│                                  ▼                                       │   │
│                           ◇─────────────────◇                            │   │
│                           │ OIC click yes   │                            │   │
│                           │ to confirm?     │                            │   │
│                           ◇────────┬────────◇                            │   │
│                              Yes   │   No                                │   │
│                         ┌──────────┴──────────┐                          │   │
│                         │                     │                          │   │
│                         ▼                     ▼                          │   │
│               ┌───────────────────┐  ┌─────────────────────┐             │   │
│               │ Portal            │  │ Portal closes pop-up│             │   │
│               │ consolidate all   │  └──────────┬──────────┘             │   │
│               │ eligible notices  │             │                        │   │
│               └─────────┬─────────┘             └────────────────────────┘   │
│                         │                                                    │
│                         ▼                                                    │
│               ┌─────────────────────────────┐                                │
│               │ Portal call API in batches  │                                │
│               │ of 10 notices               │                                │
│               └─────────┬───────────────────┘                                │
│                         │                                                    │
│                         ▼                                                    │
│               ┌─────────────────────────────┐                                │
│               │ Portal receive response     │                                │
│               │ with results                │                                │
│               └─────────┬───────────────────┘                                │
│                         │                                                    │
│                         ▼                                                    │
│               ┌─────────────────────────────┐                                │
│               │ Portal sort results:        │                                │
│               │ (a) Revival successful      │                                │
│               │ (b) Revival failed          │                                │
│               └─────────┬───────────────────┘                                │
│                         │                                                    │
│                         ▼                                                    │
│               ┌─────────────────────────────┐                                │
│               │ Portal display all results  │                                │
│               │ in table - each Notice 1 row│                                │
│               └─────────┬───────────────────┘                                │
│                         │                                                    │
│                         ▼                                                    │
│                  ┌──────────────┐                                            │
│                  │     END      │                                            │
│                  └──────────────┘                                            │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Backend Revive Validation High Level (Tab 3.3: Backend_Revive_Validate_High_Level)

### 5.1 Purpose
Backend validates and processes revival requests (MASTER FLOW)

### 5.2 Swimlanes
| Swimlane | Actor |
|----------|-------|
| Backend | OCMS Backend + Database |

### 5.3 Flow Diagram (MASTER FLOW)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                              BACKEND                                          │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────┐                                                 │
│  │ Request to Revive       │                                                 │
│  │ (Received from Portal   │                                                 │
│  │ or Cron job)            │                                                 │
│  └──────────┬──────────────┘                                                 │
│             │                                                                │
│             ▼                                                                │
│  ┌───────────────────────────────────┐                                       │
│  │ MASTER PROCESS: Revival &         │                                       │
│  │ initiate follow-up action start   │                                       │
│  └──────────┬────────────────────────┘                                       │
│             │                                                                │
│             ▼                                                                │
│  ┌─────────────────────────────────────────┐                                 │
│  │ Identify Requestor                       │                                │
│  │ • OCMS Staff Portal                      │                                │
│  │ • OCMS Backend (Cron)                    │                                │
│  │ • PLUS Staff Portal                      │                                │
│  └──────────┬──────────────────────────────┘                                 │
│             │                                                                │
│             ▼                                                                │
│  ┌─────────────────────────────────────────┐                                 │
│  │ Identify Request Type                    │                                │
│  │ • Revive TS                              │                                │
│  │ • Revive PS                              │                                │
│  │ • Overwrite PS (OCMS 18)                 │                                │
│  └──────────┬──────────────────────────────┘                                 │
│             │                                                                │
│             ▼                                                                │
│  ┌─────────────────────────────────────────┐                                 │
│  │ Get next Notice number to process       │                                 │
│  └──────────┬──────────────────────────────┘                                 │
│             │                                                                │
│             ▼                                                                │
│  ┌─────────────────────────────────────────┐                                 │
│  │ Master Query: Query DB to get all       │                                 │
│  │ active suspensions sr_no(s)             │                                 │
│  │ DB: ocms_suspended_notice               │                                 │
│  └──────────┬──────────────────────────────┘                                 │
│             │                                                                │
│             ▼                                                                │
│      ◇─────────────────────────◇                                             │
│      │ Request type =          │                                             │
│      │ Revive TS?              │                                             │
│      ◇───────────┬─────────────◇                                             │
│           Yes    │    No                                                     │
│      ┌───────────┴───────────┐                                               │
│      │                       │                                               │
│      ▼                       ▼                                               │
│  ┌───────────────────┐  ┌───────────────────────────┐                        │
│  │ Get next Notice   │  │ Request is either Revive  │                        │
│  │ to process TS     │  │ PS or Overwrite PS        │                        │
│  └─────────┬─────────┘  └─────────┬─────────────────┘                        │
│            │                      │                                          │
│            ▼                      ▼                                          │
│  ┌───────────────────────┐  ┌───────────────────────────┐                    │
│  │ MAIN TS FLOW:         │  │ Get next Notice to        │                    │
│  │ Revive all TS for     │  │ process PS                │                    │
│  │ 1 Notice              │  └─────────┬─────────────────┘                    │
│  │ (PROCESS - Tab 3.5)   │            │                                      │
│  └─────────┬─────────────┘            ▼                                      │
│            │               ◇─────────────────────────◇                       │
│            │               │ Request type =          │                       │
│            │               │ Revive PS?              │                       │
│            │               ◇───────────┬─────────────◇                       │
│            │                    Yes    │    No                               │
│            │               ┌───────────┴───────────┐                         │
│            │               │                       │                         │
│            │               ▼                       ▼                         │
│            │     ┌───────────────────────┐  ┌───────────────────────┐        │
│            │     │ MAIN PS FLOW:         │  │ Request type =        │        │
│            │     │ Revive all PS for     │  │ Overwrite PS          │        │
│            │     │ 1 Notice              │  │ (Refer to OCMS 18)    │        │
│            │     │ (PROCESS - Tab 3.4)   │  └───────────────────────┘        │
│            │     └─────────┬─────────────┘                                   │
│            │               │                                                 │
│            ▼               ▼                                                 │
│  ┌───────────────────┐  ┌───────────────────────────┐                        │
│  │ TS revival for    │  │ Main PS Flow completed -  │                        │
│  │ 1 Notice completed│  │ Received outcome for      │                        │
│  └─────────┬─────────┘  │ 1 Notice                  │                        │
│            │            └─────────┬─────────────────┘                        │
│            │                      │                                          │
│            ▼                      ▼                                          │
│      ◇───────────────────◇  ◇───────────────────────◇                        │
│      │ Request type =    │  │ Any more notices in   │                        │
│      │ Revive PS?        │  │ payload to revive PS? │                        │
│      ◇───────┬───────────◇  ◇───────────┬───────────◇                        │
│        Yes   │   No              Yes    │   No                               │
│      ┌───────┴───────┐        ┌─────────┴─────────┐                          │
│      │               │        │                   │                          │
│      │               │        │ (loop back)       │                          │
│      │               │        │                   ▼                          │
│      │               │        │         ┌─────────────────────┐              │
│      │               │        │         │ PS revival for      │              │
│      │               │        │         │ 1 Notice completed  │              │
│      │               │        │         └─────────┬───────────┘              │
│      │               │        │                   │                          │
│      ▼               ▼        │                   │                          │
│  (loop to PS)  ◇─────────────────────────◇◄───────┘                          │
│                │ Any more notices in     │                                   │
│                │ payload to revive TS?   │                                   │
│                ◇───────────┬─────────────◇                                   │
│                     Yes    │    No                                           │
│                ┌───────────┴───────────┐                                     │
│                │ (loop back)           │                                     │
│                │                       ▼                                     │
│                │         ┌───────────────────────────────┐                   │
│                │         │ Consolidate all Notice        │                   │
│                │         │ numbers and revival statuses  │                   │
│                │         └───────────┬───────────────────┘                   │
│                │                     │                                       │
│                │                     ▼                                       │
│                │         ┌───────────────────────────────┐                   │
│                │         │ Initiate process to update    │                   │
│                │         │ latest suspension status in   │                   │
│                │         │ VON DB                        │                   │
│                │         └───────────┬───────────────────┘                   │
│                │                     │                                       │
│                │                     ▼                                       │
│                │         ┌───────────────────────────────┐                   │
│                │         │ Batch Query DB to get all     │                   │
│                │         │ active suspensions            │                   │
│                │         │ DB: ocms_suspended_notice     │                   │
│                │         └───────────┬───────────────────┘                   │
│                │                     │                                       │
│                │                     ▼                                       │
│                │         ┌───────────────────────────────┐                   │
│                │         │ Result: List of active        │                   │
│                │         │ suspensions for each Notice   │                   │
│                │         └───────────┬───────────────────┘                   │
│                │                     │                                       │
│                │                     ▼                                       │
│                │         ┌───────────────────────────────┐                   │
│                │         │ Filter notices with no        │                   │
│                │         │ active suspension             │                   │
│                │         └───────────┬───────────────────┘                   │
│                │                     │                                       │
│                │                     ▼                                       │
│                │              ◇─────────────────◇                            │
│                │              │ Any Notice(s)?  │                            │
│                │              ◇────────┬────────◇                            │
│                │                 Yes   │   No                                │
│                │            ┌──────────┴──────────┐                          │
│                │            │                     │                          │
│                │            ▼                     │                          │
│                │  ┌───────────────────────┐       │                          │
│                │  │ Batch patch suspension│       │                          │
│                │  │ status in VON DB      │       │                          │
│                │  └─────────┬─────────────┘       │                          │
│                │            │                     │                          │
│                │            ▼                     │                          │
│                │  ┌───────────────────────┐       │                          │
│                │  │ Update completed      │       │                          │
│                │  └─────────┬─────────────┘       │                          │
│                │            │                     │                          │
│                │            ▼                     │                          │
│                │     ◇─────────────────◇          │                          │
│                │     │ Any more        │          │                          │
│                │     │ Notices?        │          │                          │
│                │     ◇────────┬────────◇          │                          │
│                │        Yes   │   No              │                          │
│                │     ┌────────┴────────┐          │                          │
│                │     │ (loop back)     │          │                          │
│                │     │                 ▼          ▼                          │
│                │     │    ┌───────────────────────────────┐                  │
│                │     │    │ Consolidate all successful/   │                  │
│                │     │    │ failed TS and PS revival      │                  │
│                │     │    │ statuses                      │                  │
│                │     │    └───────────┬───────────────────┘                  │
│                │     │                │                                      │
│                │     │                ▼                                      │
│                │     │    ┌───────────────────────────────┐                  │
│                │     │    │ Respond back to Requestor     │                  │
│                │     │    └───────────┬───────────────────┘                  │
│                │     │                │                                      │
│                │     │                ▼                                      │
│                │     │    ┌───────────────────────────────┐                  │
│                │     │    │ Consolidate all errors and    │                  │
│                │     │    │ failed statuses               │                  │
│                │     │    └───────────┬───────────────────┘                  │
│                │     │                │                                      │
│                │     │                ▼                                      │
│                │     │    ┌───────────────────────────────┐                  │
│                │     │    │ Send interfacing email        │                  │
│                │     │    └───────────┬───────────────────┘                  │
│                │     │                │                                      │
│                │     │                ▼                                      │
│                │     │         ┌──────────────┐                              │
│                │     │         │ Master       │                              │
│                │     │         │ Process ends │                              │
│                │     │         └──────────────┘                              │
│                │     │                                                       │
└────────────────┴─────┴───────────────────────────────────────────────────────┘
```

---

## 6. Backend Revive PS Main (Tab 3.4: Backend_Revive_PS_Main)

### 6.1 Purpose
Main flow for reviving PS suspensions

### 6.2 Swimlanes
| Swimlane | Actor |
|----------|-------|
| Backend | OCMS Backend + Database |

### 6.3 Key Flow Steps

```
┌─────────────────────────────────────────────────────────────────┐
│                          BACKEND                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌───────────────────────────┐                                  │
│  │ Receive Notice number     │                                  │
│  │ to process PS             │                                  │
│  └──────────┬────────────────┘                                  │
│             │                                                   │
│             ▼                                                   │
│  ┌───────────────────────────────┐                              │
│  │ Query active PS suspension    │                              │
│  │ DB: ocms_suspended_notice     │                              │
│  │ WHERE date_of_revival IS NULL │                              │
│  │ AND suspension_type = 'PS'    │                              │
│  └──────────┬────────────────────┘                              │
│             │                                                   │
│             ▼                                                   │
│      ◇─────────────────────────◇                                │
│      │ Active PS found?        │                                │
│      ◇───────────┬─────────────◇                                │
│           Yes    │    No                                        │
│      ┌───────────┴───────────┐                                  │
│      │                       │                                  │
│      ▼                       ▼                                  │
│  ┌───────────────────┐  ┌───────────────────────┐               │
│  │ Validate PS can   │  │ Return error:         │               │
│  │ be revived        │  │ "No active suspension"│               │
│  └─────────┬─────────┘  └──────────┬────────────┘               │
│            │                       │                            │
│            ▼                       ▼                            │
│  ┌───────────────────────────┐  ┌──────────────┐                │
│  │ Update ocms_suspended_    │  │ Return to    │                │
│  │ notice with revival:      │  │ Master Flow  │                │
│  │ • date_of_revival=NOW()   │  └──────────────┘                │
│  │ • revival_reason          │                                  │
│  │ • officer_authorising_    │                                  │
│  │   revival                 │                                  │
│  │ • revival_remarks         │                                  │
│  └──────────┬────────────────┘                                  │
│             │                                                   │
│             ▼                                                   │
│  ┌───────────────────────────────┐                              │
│  │ Update ocms_valid_offence_    │                              │
│  │ notice (clear suspension):    │                              │
│  │ • suspension_type = NULL      │                              │
│  │ • epr_reason_suspension=NULL  │                              │
│  │ • crs_reason_suspension=NULL  │                              │
│  │ • due_date_of_revival = NULL  │                              │
│  │ • next_processing_date =      │                              │
│  │   NOW() + 2 days              │                              │
│  └──────────┬────────────────────┘                              │
│             │                                                   │
│             ▼                                                   │
│      ◇─────────────────────────────◇                            │
│      │ PS code = PS-FP?            │                            │
│      │ (Refund required)           │                            │
│      ◇───────────┬─────────────────◇                            │
│           Yes    │    No                                        │
│      ┌───────────┴───────────┐                                  │
│      │                       │                                  │
│      ▼                       ▼                                  │
│  ┌───────────────────┐  ┌───────────────────────┐               │
│  │ Mark refund date  │  │ No follow-up action   │               │
│  │ identified        │  │ required              │               │
│  └─────────┬─────────┘  └──────────┬────────────┘               │
│            │                       │                            │
│            ▼                       ▼                            │
│  ┌─────────────────────────────────────────────┐                │
│  │ Return revival result to Master Flow        │                │
│  │ • noticeNo                                  │                │
│  │ • appCode: OCMS-2000 (success)              │                │
│  │ • message: "PS revived successfully"        │                │
│  └──────────┬──────────────────────────────────┘                │
│             │                                                   │
│             ▼                                                   │
│      ┌──────────────┐                                           │
│      │ Return to    │                                           │
│      │ Master Flow  │                                           │
│      └──────────────┘                                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 7. Backend Revive TS Main (Tab 3.5: Backend_Revive_TS_Main)

### 7.1 Purpose
Main flow for reviving TS suspensions

### 7.2 Swimlanes
| Swimlane | Actor |
|----------|-------|
| Backend | OCMS Backend + Database |

### 7.3 Key Flow Steps

```
┌─────────────────────────────────────────────────────────────────┐
│                          BACKEND                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌───────────────────────────┐                                  │
│  │ Receive Notice number     │                                  │
│  │ to process TS             │                                  │
│  └──────────┬────────────────┘                                  │
│             │                                                   │
│             ▼                                                   │
│  ┌───────────────────────────────┐                              │
│  │ Query ALL active TS           │                              │
│  │ suspensions                   │                              │
│  │ DB: ocms_suspended_notice     │                              │
│  │ WHERE date_of_revival IS NULL │                              │
│  │ AND suspension_type = 'TS'    │                              │
│  └──────────┬────────────────────┘                              │
│             │                                                   │
│             ▼                                                   │
│      ◇─────────────────────────◇                                │
│      │ Active TS found?        │                                │
│      ◇───────────┬─────────────◇                                │
│           Yes    │    No                                        │
│      ┌───────────┴───────────┐                                  │
│      │                       │                                  │
│      ▼                       ▼                                  │
│      ◇───────────────────◇  ┌───────────────────────┐           │
│      │ TS code = HST?    │  │ Return error:         │           │
│      ◇───────┬───────────◇  │ "No active suspension"│           │
│        Yes   │   No         └──────────┬────────────┘           │
│      ┌───────┴───────┐                 │                        │
│      │               │                 ▼                        │
│      ▼               ▼          ┌──────────────┐                │
│  ┌───────────────┐  │           │ Return to    │                │
│  │ Return error: │  │           │ Master Flow  │                │
│  │ "TS-HST not   │  │           └──────────────┘                │
│  │ allowed"      │  │                                           │
│  │ (OCMS-4007)   │  │                                           │
│  └───────┬───────┘  │                                           │
│          │          │                                           │
│          ▼          │                                           │
│  ┌──────────────┐   │                                           │
│  │ Return to    │   │                                           │
│  │ Master Flow  │   │                                           │
│  └──────────────┘   │                                           │
│                     ▼                                           │
│           ┌───────────────────────────┐                         │
│           │ Update ALL active TS      │                         │
│           │ in ocms_suspended_notice: │                         │
│           │ • date_of_revival=NOW()   │                         │
│           │ • revival_reason          │                         │
│           │ • officer_authorising_    │                         │
│           │   revival                 │                         │
│           │ • revival_remarks         │                         │
│           └──────────┬────────────────┘                         │
│                      │                                          │
│                      ▼                                          │
│             ◇─────────────────────────────◇                     │
│             │ Other active TS exists      │                     │
│             │ after revival?              │                     │
│             ◇───────────┬─────────────────◇                     │
│                  Yes    │    No                                 │
│             ┌───────────┴───────────┐                           │
│             │                       │                           │
│             ▼                       ▼                           │
│  ┌───────────────────────┐  ┌───────────────────────────┐       │
│  │ Apply TS with LATEST  │  │ Update VON (clear all):   │       │
│  │ due date to VON       │  │ • suspension_type=NULL    │       │
│  └──────────┬────────────┘  │ • epr_reason_suspension   │       │
│             │               │   =NULL                   │       │
│             │               │ • due_date_of_revival     │       │
│             │               │   =NULL                   │       │
│             │               └──────────┬────────────────┘       │
│             │                          │                        │
│             ▼                          ▼                        │
│  ┌───────────────────────────────────────────────────┐          │
│  │ Set next_processing_date = NOW() + 2 days         │          │
│  └──────────┬────────────────────────────────────────┘          │
│             │                                                   │
│             ▼                                                   │
│      ◇─────────────────────────────◇                            │
│      │ Follow-up action required?  │                            │
│      │ (TS-CLV looping, etc.)      │                            │
│      ◇───────────┬─────────────────◇                            │
│           Yes    │    No                                        │
│      ┌───────────┴───────────┐                                  │
│      │                       │                                  │
│      ▼                       ▼                                  │
│  ┌───────────────────┐  ┌───────────────────────┐               │
│  │ Initiate follow-  │  │ No follow-up action   │               │
│  │ up action         │  │ required              │               │
│  │ (re-apply TS)     │  └──────────┬────────────┘               │
│  └─────────┬─────────┘             │                            │
│            │                       │                            │
│            ▼                       ▼                            │
│  ┌─────────────────────────────────────────────┐                │
│  │ Return revival result to Master Flow        │                │
│  │ • noticeNo                                  │                │
│  │ • appCode: OCMS-2000 (success)              │                │
│  │ • message: "TS revived successfully"        │                │
│  └──────────┬──────────────────────────────────┘                │
│             │                                                   │
│             ▼                                                   │
│      ┌──────────────┐                                           │
│      │ Return to    │                                           │
│      │ Master Flow  │                                           │
│      └──────────────┘                                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 8. Backend Replace PS (Tab 3.6: Backend-Replace_PS)

### 8.1 Purpose
Handle replacing existing PS with new PS (auto-revive existing PS)

### 8.2 Note
This flow relates to OCMS 18 (Apply Suspensions). When a new PS is applied to a Notice with existing PS, the system auto-revives the existing PS with reason code "PSR" before applying the new PS.

### 8.3 Key Flow Steps

```
┌─────────────────────────────────────────────────────────────────┐
│                          BACKEND                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌───────────────────────────────┐                              │
│  │ Receive request to apply      │                              │
│  │ new PS to Notice              │                              │
│  └──────────┬────────────────────┘                              │
│             │                                                   │
│             ▼                                                   │
│      ◇─────────────────────────────◇                            │
│      │ Notice has existing         │                            │
│      │ active PS?                  │                            │
│      ◇───────────┬─────────────────◇                            │
│           Yes    │    No                                        │
│      ┌───────────┴───────────┐                                  │
│      │                       │                                  │
│      ▼                       ▼                                  │
│      ◇───────────────────────────◇  ┌───────────────────────┐   │
│      │ Existing PS is special    │  │ Apply new PS directly │   │
│      │ (DIP, FOR, MID, RIP, RP2)?│  └──────────┬────────────┘   │
│      ◇───────────┬───────────────◇             │                │
│           Yes    │    No                       │                │
│      ┌───────────┴───────────┐                 │                │
│      │                       │                 │                │
│      ▼                       ▼                 │                │
│      ◇───────────────────◇  ┌─────────────────┐│                │
│      │ New PS is CRS     │  │ Auto-revive     ││                │
│      │ (PS-FP/PS-PRA)?   │  │ existing PS     ││                │
│      ◇───────┬───────────◇  │ with PSR code   ││                │
│        Yes   │   No         └────────┬────────┘│                │
│      ┌───────┴───────┐              │          │                │
│      │               │              │          │                │
│      ▼               ▼              │          │                │
│  ┌───────────────┐  ┌───────────────┐          │                │
│  │ Apply new PS  │  │ Return error: │          │                │
│  │ WITHOUT       │  │ "CRS not      │          │                │
│  │ revival       │  │ allowed for   │          │                │
│  │ (special case)│  │ non-special   │          │                │
│  └───────┬───────┘  │ PS"           │          │                │
│          │          └───────────────┘          │                │
│          │                                     │                │
│          ▼                                     │                │
│  ┌───────────────────────────────────────────────────┐          │
│  │ Apply new PS to Notice                            │◄─────────┘
│  └──────────┬────────────────────────────────────────┘
│             │
│             ▼
│      ┌──────────────┐
│      │ Return       │
│      │ result       │
│      └──────────────┘
│
└─────────────────────────────────────────────────────────────────┘
```

---

## 9. OCMS Portal Manual Revive (Tab 3.7) & PLUS Portal Manual Revive (Tab 3.8)

### 9.1 OCMS Portal Flow
Similar to Tab 3.2 but specific to OCMS Staff Portal context.

### 9.2 PLUS Portal Flow
Uses 3 swimlanes:

| Swimlane | Actor |
|----------|-------|
| Frontend | PLUS Staff Portal |
| Backend | OCMS Backend |
| External System | PLUS Backend |

Flow includes additional step where PLUS Portal → PLUS Backend → OCMS Backend API.

---

## 10. Cross-References

| Related OCMS | Description | Cross-Reference Point |
|--------------|-------------|----------------------|
| OCMS 18 | Apply Suspensions | Replace PS flow (Tab 3.6) |
| OCMS 39 | Parameters Management | Revival Reason codes |

---

## 11. Notes for Drawio Creation

1. **Use same tab names** as Functional Flow diagram
2. **Swimlanes:** Maximum 3 (Frontend, Backend, External System)
3. **Connectors:** Use arrows (→) between all shapes
4. **Decision diamonds:** Show Yes/No paths clearly
5. **Process boxes:** Use rounded rectangles for sub-processes
6. **Database operations:** Include in Backend swimlane with DB reference note
7. **Loop indicators:** Show clearly when flow loops back
8. **Error paths:** Show error handling with separate end points

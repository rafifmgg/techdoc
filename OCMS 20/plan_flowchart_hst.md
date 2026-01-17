# OCMS 20 - HST Suspension: Flowchart Plan

## Document Information

| Item | Value |
|------|-------|
| Version | 1.0 |
| Date | 2026-01-07 |
| Feature | HST (House-to-House Search/Tenant) Suspension |
| Prerequisites | plan_api_hst.md, plan_condition_hst.md |

---

## 1. Flowchart Overview

### 1.1 Diagrams to Create

| Page | Name | Type | Description |
|------|------|------|-------------|
| 1 | HST_High_Level_Flow | Overview | End-to-end HST processing overview |
| 2 | HST_Manual_Suspend_User_Journey | Detailed | OIC creates HST via Staff Portal |
| 3 | HST_Manual_Suspend_Backend | Detailed | Backend processing for HST creation |
| 4 | HST_Update_User_Journey | Detailed | OIC updates HST (new address) |
| 5 | HST_Update_Backend | Detailed | Backend processing for HST update |
| 6 | HST_Revival_User_Journey | Detailed | OIC revives/lifts HST |
| 7 | HST_Revival_Backend | Detailed | Backend processing for HST revival |
| 8 | HST_Monthly_Cron | Detailed | Monthly DataHive checks & looping |

### 1.2 Swimlanes

| Swimlane | Color | Description |
|----------|-------|-------------|
| Staff Portal | #dae8fc (Light Blue) | OIC interactions, form display |
| OCMS Admin API Intranet | #d5e8d4 (Light Green) | Backend API processing |

---

## 2. High-Level Flow (Page 1)

### 2.1 HST_High_Level_Flow

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                     HIGH-LEVEL PROCESS FLOW - HST                                │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────┐
│ PRE-CONDITION:          │
│ No new address found    │
│ for Unclaimed Offender  │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐     ┌─────────────────────────┐
│ PROCESS:                │────▶│ Refer to                │
│ Create HST Suspension   │     │ HST_Manual_Suspend      │
│ via Staff Portal        │     └─────────────────────────┘
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐     ┌─────────────────────────┐
│ PROCESS:                │────▶│ Refer to                │
│ Suspend all Notices     │     │ HST_Manual_Suspend      │
│ under ID with TS-HST    │     │ _Backend                │
└───────────┬─────────────┘     └─────────────────────────┘
            │
            ▼
┌─────────────────────────┐
│ Note: New Notices under │
│ ID will be suspended    │
│ with TS-HST before RD1  │
└───────────┬─────────────┘
            │
            ▼
        ┌───────────────┐
        │ Payment made? │
        └───────┬───────┘
                │
        ┌───────┴───────┐
        │ Yes           │ No
        ▼               ▼
┌─────────────┐   ┌─────────────────────────┐
│ Notice      │   │ Looping TS-HST applied  │
│ suspended   │   │ upon suspension expiry  │
│ with PS-FP  │   └───────────┬─────────────┘
└─────────────┘               │
                              ▼
            ┌─────────────────────────┐     ┌─────────────────────────┐
            │ CRON:                   │────▶│ Refer to                │
            │ Monthly DataHive        │     │ HST_Monthly_Cron        │
            │ checks & Reports        │     └─────────────────────────┘
            └───────────┬─────────────┘
                        │
                        ▼
                  ┌───────────────┐
                  │ New address   │
                  │ found?        │
                  └───────┬───────┘
                          │
                  ┌───────┴───────┐
                  │ Yes           │ No (Loop back)
                  ▼               │
    ┌─────────────────────────┐   │
    │ PROCESS:                │   │
    │ Manual HST Update       │   │
    │ (New address found)     │   │
    └───────────┬─────────────┘   │
                │                 │
                ▼                 │
┌─────────────────────────┐       │
│ Offender name/address   │       │
│ for all TS-HST Notices  │       │
│ will be updated         │       │
└───────────┬─────────────┘       │
            │                     │
            ▼                     │
┌─────────────────────────┐       │
│ PROCESS:                │       │
│ Manual HST Revival      │       │
└───────────┬─────────────┘       │
            │                     │
            ▼                     │
┌─────────────────────────┐       │
│ All HST suspensions     │       │
│ under the ID lifted     │       │
└───────────┬─────────────┘       │
            │                     │
            ▼                     │
        ┌───────┐                 │
        │  End  │◀────────────────┘
        └───────┘
            │
            ▼
┌─────────────────────────┐
│ Notice continues along  │
│ standard Notice workflow│
└─────────────────────────┘
```

---

## 3. Detailed Flows

### 3.1 HST_Manual_Suspend_User_Journey (Page 2)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ SWIMLANE: Staff Portal (Light Blue)                                              │
└─────────────────────────────────────────────────────────────────────────────────┘

  ┌───────┐
  │ Start │
  └───┬───┘
      │
      ▼
┌─────────────────────────┐
│ OIC launches OCMS Staff │
│ Portal HST module       │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ OIC selects "Manual HST │
│ Suspension"             │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ Portal displays ID      │
│ number input text box   │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ OIC enters Unclaimed    │
│ Offender's ID number    │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ Portal detects ID type  │
│ (NRIC/FIN/UEN)          │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ Auto populate ID type   │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐     ┌─────────────────────────────────────┐
│ Portal queries Backend  │────▶│ API: POST /v1/hst/check-offender    │
│ to check if Offender    │     │                                     │
│ is an HST ID            │     └─────────────────────────────────────┘
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ Portal reads result     │
│ returned by Backend     │
└───────────┬─────────────┘
            │
            ▼
      ┌───────────────┐
      │ Offender ID   │
      │ found in HST? │
      └───────┬───────┘
              │
      ┌───────┴───────┐
      │ Yes           │ No
      ▼               ▼
┌─────────────┐ ┌─────────────────────────┐
│ Display     │ │ Portal displays         │
│ "ID is      │ │ Offender's name and     │
│ suspended   │ │ latest registered       │
│ under HST"  │ │ address details         │
└──────┬──────┘ └───────────┬─────────────┘
       │                    │
       ▼                    ▼
┌─────────────┐ ┌─────────────────────────┐
│ OIC         │ │ OIC reviews Offender's  │
│ dismisses   │ │ address details         │
│ prompt      │ └───────────┬─────────────┘
└──────┬──────┘             │
       │                    ▼
       │        ┌─────────────────────────┐
       │        │ OPTIONAL: OIC edit      │
       │        │ Offender's address      │
       │        │ details                 │
       │        └───────────┬─────────────┘
       │                    │
       │                    ▼
       │        ┌─────────────────────────┐
       │        │ OIC clicks "Apply HST"  │
       │        └───────────┬─────────────┘
       │                    │
       │                    ▼
       │        ┌─────────────────────────┐     ┌─────────────────────────────────┐
       │        │ Portal sends request to │────▶│ API: POST /v1/hst/create-       │
       │        │ Backend to create HST   │     │      suspension                 │
       │        └───────────┬─────────────┘     │ Refer to HST_Manual_Suspend     │
       │                    │                   │ _Backend flow                   │
       │                    ▼                   └─────────────────────────────────┘
       │        ┌─────────────────────────┐
       │        │ Portal reads result     │
       │        │ returned by Backend     │
       │        └───────────┬─────────────┘
       │                    │
       │                    ▼
       │              ┌───────────────┐
       │              │ HST ID created│
       │              │ successfully? │
       │              └───────┬───────┘
       │                      │
       │              ┌───────┴───────┐
       │              │ Yes           │ No
       │              ▼               ▼
       │    ┌───────────────┐   ┌─────────────────┐
       │    │ Any notices   │   │ Display "Unable │
       │    │ suspended?    │   │ to create HST   │
       │    └───────┬───────┘   │ ID"             │
       │            │           └────────┬────────┘
       │    ┌───────┴───────┐            │
       │    │ Yes           │ No         │
       │    ▼               ▼            │
       │ ┌─────────────┐ ┌─────────────┐ │
       │ │ Display     │ │ Display     │ │
       │ │ "Suspension │ │ "HST ID     │ │
       │ │ and HST ID  │ │ created. No │ │
       │ │ processed"  │ │ Notices to  │ │
       │ │ + table     │ │ suspend."   │ │
       │ └──────┬──────┘ └──────┬──────┘ │
       │        │               │        │
       │        └───────────────┼────────┘
       │                        │
       │                        ▼
       │        ┌─────────────────────────┐
       │        │ OIC dismisses prompt    │
       │        └───────────┬─────────────┘
       │                    │
       └────────────────────┤
                            │
                            ▼
            ┌─────────────────────────┐
            │ Portal redirects to     │
            │ Manual HST Suspension   │
            │ main screen             │
            └───────────┬─────────────┘
                        │
                        ▼
                    ┌───────┐
                    │  End  │
                    └───────┘
```

---

### 3.2 HST_Manual_Suspend_Backend (Page 3)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ SWIMLANES: Staff Portal (Blue) + OCMS Admin API Intranet (Green)                 │
└─────────────────────────────────────────────────────────────────────────────────┘

STAFF PORTAL:
┌─────────────────────────┐
│ OIC clicks "Apply HST"  │
└───────────┬─────────────┘
            │
            ▼
OCMS ADMIN API:
┌─────────────────────────┐     ┌─────────────────────────────────────┐
│ Receive API request     │────▶│ POST /v1/hst/create-suspension      │
│                         │     │ { idNo, idType, address, remarks }  │
└───────────┬─────────────┘     └─────────────────────────────────────┘
            │
            ▼
┌─────────────────────────┐
│ Validate JWT            │
│ Authentication          │
└───────────┬─────────────┘
            │
            ▼
      ┌───────────────┐
      │ JWT valid?    │
      └───────┬───────┘
              │
      ┌───────┴───────┐
      │ No            │ Yes
      ▼               │
┌───────────────┐     │
│ Return error  │     │
│ OCMS-4001     │     │
└───────────────┘     │
                      ▼
          ┌─────────────────────────┐
          │ Validate payload        │
          │ - idNo required         │
          │ - idType required       │
          └───────────┬─────────────┘
                      │
                      ▼
                ┌───────────────┐
                │ Payload valid?│
                └───────┬───────┘
                        │
                ┌───────┴───────┐
                │ No            │ Yes
                ▼               │
          ┌───────────────┐     │
          │ Return error  │     │
          │ OCMS-4000     │     │
          └───────────────┘     │
                                ▼
                    ┌─────────────────────────┐     ┌─────────────────────────────────┐
                    │ Query ocms_hst_id to    │────▶│ SELECT * FROM ocms_hst_id       │
                    │ check if ID already     │     │ WHERE id_no = ? AND status='A'  │
                    │ exists                  │     └─────────────────────────────────┘
                    └───────────┬─────────────┘
                                │
                                ▼
                          ┌───────────────┐
                          │ ID exists?    │
                          └───────┬───────┘
                                  │
                          ┌───────┴───────┐
                          │ Yes           │ No
                          ▼               │
                    ┌───────────────┐     │
                    │ Return error  │     │
                    │ OCMS-4004     │     │
                    │ "Already HST" │     │
                    └───────────────┘     │
                                          ▼
                              ┌─────────────────────────┐     ┌─────────────────────────────────┐
                              │ INSERT to ocms_hst_id   │────▶│ INSERT INTO ocms_hst_id         │
                              │ (Create HST ID record)  │     │ (id_no, id_type, status,        │
                              │                         │     │  offender_name, address, ...)   │
                              └───────────┬─────────────┘     └─────────────────────────────────┘
                                          │
                                          ▼
                              ┌─────────────────────────┐     ┌─────────────────────────────────┐
                              │ Query                   │────▶│ SELECT notice_no FROM           │
                              │ ocms_offence_notice_    │     │ ocms_offence_notice_owner_driver│
                              │ owner_driver for        │     │ WHERE id_no = ?                 │
                              │ notices under this ID   │     │ AND current_offender_ind = 'Y'  │
                              └───────────┬─────────────┘     └─────────────────────────────────┘
                                          │
                                          ▼
                                    ┌───────────────┐
                                    │ Notices found?│
                                    └───────┬───────┘
                                            │
                                    ┌───────┴───────┐
                                    │ No            │ Yes
                                    ▼               │
                              ┌───────────────┐     │
                              │ Return success│     │
                              │ HST created,  │     │
                              │ no notices    │     │
                              └───────────────┘     │
                                                    ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│ FOR EACH notice:                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
                                                    │
                                                    ▼
                                        ┌─────────────────────────┐
                                        │ Check notice suspension │
                                        │ eligibility             │
                                        └───────────┬─────────────┘
                                                    │
                                                    ▼
                                              ┌───────────────┐
                                              │ Can suspend?  │
                                              │ (Not PS/Court)│
                                              └───────┬───────┘
                                                      │
                                              ┌───────┴───────┐
                                              │ No            │ Yes
                                              ▼               │
                                        ┌───────────────┐     │
                                        │ Skip notice,  │     │
                                        │ log debug     │     │
                                        └───────────────┘     │
                                                              ▼
                                              ┌─────────────────────────┐
                                              │ Get MAX(sr_no) from     │
                                              │ ocms_suspended_notice   │
                                              └───────────┬─────────────┘
                                                          │
                                                          ▼
                                              ┌─────────────────────────┐     ┌─────────────────────────────────┐
                                              │ INSERT to               │────▶│ INSERT INTO ocms_suspended_     │
                                              │ ocms_suspended_notice   │     │ notice (notice_no, sr_no,       │
                                              │ (TS-HST)                │     │ suspension_type='TS',           │
                                              │                         │     │ reason_of_suspension='HST', ..) │
                                              └───────────┬─────────────┘     └─────────────────────────────────┘
                                                          │
                                                          ▼
                                                    ┌───────────────┐
                                                    │ More notices? │
                                                    └───────┬───────┘
                                                            │
                                                    ┌───────┴───────┐
                                                    │ Yes           │ No
                                                    │ (Loop back)   │
                                                    └───────────────┤
                                                                    │
                                                                    ▼
                                                    ┌─────────────────────────┐
                                                    │ Return success response │
                                                    │ with suspended notices  │
                                                    │ list                    │
                                                    └───────────┬─────────────┘
                                                                │
                                                                ▼
STAFF PORTAL:                                       ┌─────────────────────────┐
                                                    │ Display success message │
                                                    └───────────┬─────────────┘
                                                                │
                                                                ▼
                                                            ┌───────┐
                                                            │  End  │
                                                            └───────┘
```

---

### 3.3 HST_Update_User_Journey (Page 4)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ SWIMLANE: Staff Portal (Light Blue)                                              │
└─────────────────────────────────────────────────────────────────────────────────┘

  ┌───────┐
  │ Start │
  └───┬───┘
      │
      ▼
┌─────────────────────────┐
│ OIC launches OCMS Staff │
│ Portal HST module       │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ OIC selects "HST Update"│
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ Portal displays HST     │
│ records list with       │
│ "New Address Found"     │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ OIC selects HST record  │
│ to update               │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ Portal displays current │
│ and new address details │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ OIC reviews new address │
│ details                 │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ OIC clicks "Update"     │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐     ┌─────────────────────────────────────┐
│ Portal sends request    │────▶│ API: POST /v1/hst/update-address    │
│ to Backend              │     │ Refer to HST_Update_Backend flow    │
└───────────┬─────────────┘     └─────────────────────────────────────┘
            │
            ▼
┌─────────────────────────┐
│ Display success message │
│ "HST record updated"    │
└───────────┬─────────────┘
            │
            ▼
        ┌───────┐
        │  End  │
        └───────┘
```

---

### 3.4 HST_Revival_User_Journey (Page 6)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ SWIMLANE: Staff Portal (Light Blue)                                              │
└─────────────────────────────────────────────────────────────────────────────────┘

  ┌───────┐
  │ Start │
  └───┬───┘
      │
      ▼
┌─────────────────────────┐
│ OIC launches OCMS Staff │
│ Portal HST module       │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ OIC selects "HST        │
│ Revival"                │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ Portal displays active  │
│ HST records list        │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ OIC selects HST record  │
│ to revive               │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ Portal displays HST     │
│ details with suspended  │
│ notices list            │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ OIC enters revival      │
│ reason and remarks      │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ OIC clicks "Revive HST" │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ Portal displays         │
│ confirmation dialog     │
│ "Lift all TS-HST?"      │
└───────────┬─────────────┘
            │
            ▼
      ┌───────────────┐
      │ OIC confirms? │
      └───────┬───────┘
              │
      ┌───────┴───────┐
      │ No            │ Yes
      ▼               │
┌─────────────┐       │
│ Cancel,     │       │
│ return to   │       │
│ list        │       │
└─────────────┘       │
                      ▼
          ┌─────────────────────────┐     ┌─────────────────────────────────────┐
          │ Portal sends request    │────▶│ API: POST /v1/hst/revive            │
          │ to Backend              │     │ Refer to HST_Revival_Backend flow   │
          └───────────┬─────────────┘     └─────────────────────────────────────┘
                      │
                      ▼
          ┌─────────────────────────┐
          │ Display success message │
          │ "HST revived, notices   │
          │ continue normal flow"   │
          └───────────┬─────────────┘
                      │
                      ▼
                  ┌───────┐
                  │  End  │
                  └───────┘
```

---

### 3.5 HST_Monthly_Cron (Page 8)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ SWIMLANE: OCMS Admin Cron Service (Light Green)                                  │
└─────────────────────────────────────────────────────────────────────────────────┘

  ┌─────────────┐
  │ Cron Start  │
  │ (Monthly)   │
  └──────┬──────┘
         │
         ▼
┌─────────────────────────┐     ┌─────────────────────────────────────┐
│ Query ocms_hst_id for   │────▶│ SELECT * FROM ocms_hst_id           │
│ active HST IDs          │     │ WHERE status = 'A'                  │
└───────────┬─────────────┘     └─────────────────────────────────────┘
            │
            ▼
      ┌───────────────┐
      │ Any records?  │
      └───────┬───────┘
              │
      ┌───────┴───────┐
      │ No            │ Yes
      ▼               │
┌─────────────┐       │
│ Log "No     │       │
│ active HST" │       │
│ End         │       │
└─────────────┘       │
                      ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│ FOR EACH active HST ID:                                                          │
└─────────────────────────────────────────────────────────────────────────────────┘
                      │
                      ▼
          ┌─────────────────────────┐
          │ Queue ID for DataHive   │
          │ query (NRIC/FIN/UEN)    │
          └───────────┬─────────────┘
                      │
                      ▼
          ┌─────────────────────────┐
          │ Wait for DataHive       │
          │ results (async)         │
          └───────────┬─────────────┘
                      │
                      ▼
          ┌─────────────────────────┐
          │ Process DataHive        │
          │ results                 │
          └───────────┬─────────────┘
                      │
                      ▼
                ┌───────────────┐
                │ New address   │
                │ found?        │
                └───────┬───────┘
                        │
                ┌───────┴───────┐
                │ Yes           │ No
                ▼               ▼
    ┌─────────────────┐ ┌─────────────────────────┐
    │ Store new       │ │ Continue TS-HST loop    │
    │ address in      │ │ (Re-apply on expiry)    │
    │ temp table      │ │                         │
    │                 │ │ Refer to OCMS 17 FD     │
    │ Generate "New   │ │ for suspension expiry   │
    │ Address" report │ │ handling                │
    └────────┬────────┘ └───────────┬─────────────┘
             │                      │
             └──────────────────────┘
                        │
                        ▼
                  ┌───────────────┐
                  │ More HST IDs? │
                  └───────┬───────┘
                          │
                  ┌───────┴───────┐
                  │ Yes           │ No
                  │ (Loop back)   │
                  └───────────────┤
                                  │
                                  ▼
                  ┌─────────────────────────┐
                  │ Generate Monthly HST    │
                  │ Summary Report          │
                  └───────────┬─────────────┘
                              │
                              ▼
                  ┌─────────────────────────┐
                  │ Upload reports to       │
                  │ Azure Blob Storage      │
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
| 3.1 | Check HST | ocms_hst_id | SELECT | id_no, status |
| 3.1 | Query Offender | ocms_offence_notice_owner_driver | SELECT | id_no, offender_name |
| 3.1 | Query Address | ocms_offence_notice_owner_driver_addr | SELECT | address fields |
| 3.2 | Create HST | ocms_hst_id | INSERT | All HST fields |
| 3.2 | Get Notices | ocms_offence_notice_owner_driver | SELECT | notice_no, current_offender_ind |
| 3.2 | Create Suspension | ocms_suspended_notice | INSERT | TS-HST suspension |
| 3.3 | Update Address | ocms_offence_notice_owner_driver_addr | UPDATE | New address fields |
| 3.4 | Revive HST | ocms_hst_id | UPDATE | status = 'R' |
| 3.4 | Remove Suspension | ocms_suspended_notice | UPDATE/DELETE | Remove TS-HST |
| 3.5 | Monthly Check | ocms_hst_id | SELECT | Active HST IDs |
| 3.5 | Store Results | ocms_temp_unc_hst_addr | INSERT | DataHive results |

---

## 5. Error Handling Summary

| Flow | Error Scenario | Action | Error Code |
|------|----------------|--------|------------|
| 3.1 | ID already in HST | Return message | - |
| 3.2 | idNo missing | Return error | OCMS-4000 |
| 3.2 | JWT invalid | Return error | OCMS-4001 |
| 3.2 | ID already HST | Return error | OCMS-4004 |
| 3.3 | HST ID not found | Return error | OCMS-4003 |
| 3.4 | HST ID not active | Return error | OCMS-4004 |
| 3.5 | DataHive timeout | Retry 3 times | - |

---

## 6. Notes for Flowchart Creation

### 6.1 Shape Legend

Same as Unclaimed flowchart (refer to plan_flowchart.md)

### 6.2 Color Coding

| Element | Color |
|---------|-------|
| Staff Portal swimlane | #dae8fc (Light Blue) |
| OCMS Admin API swimlane | #d5e8d4 (Light Green) |
| API boxes | #fff2cc (Light Yellow) |
| Info notes | #dae8fc (Light Blue) |
| Reference text | Italic (fontStyle=2) |

### 6.3 External References

| Reference | Document |
|-----------|----------|
| OCMS 11 | New Notice Creation (HST check before RD1) |
| OCMS 17 | Suspension Expiry Handling (TS-HST looping) |
| DataHive Integration TD | DataHive query details |
| MHA Integration TD | MHA SFTP query details |

---

*End of Document*

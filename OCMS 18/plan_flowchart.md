# OCMS 18 - Permanent Suspension Flowchart Plan

## Document Information

| Item | Details |
|------|---------|
| Version | 1.0 |
| Date | 2026-01-05 |
| Source Document | v1.4_OCMS 18_Functional_Document.md |
| Source Flowchart | FOR USERS - PS_Functional_Flow.drawio |
| Related Documents | plan_api.md, plan_condition.md |
| Author | Technical Documentation |

---

## 1. Diagram Sections Overview

### 1.1 Required Tabs/Pages

| Tab # | Tab Name | Description | Priority |
|-------|----------|-------------|----------|
| 1 | PS_High_Level | High-level overview of PS process | High |
| 2 | UI_Side_Validation | Staff Portal validation flow | High |
| 3 | Backend_Side_Validation | OCMS Backend validation flow | High |
| 4 | OCMS_Staff_Portal_Manual_PS | OCMS Staff Portal complete flow | High |
| 5 | PLUS_Staff_Portal_Manual_PS | PLUS Staff Portal complete flow | High |
| 6 | Auto_PS_Triggers | Auto PS scenarios by backend | Medium |
| 7 | PS_Revival | PS Revival flow | Medium |
| 8 | PS_Report | PS Report generation flow | Low |

### 1.2 Swimlanes (Systems)

| Swimlane | Color | Description |
|----------|-------|-------------|
| Frontend (Staff Portal) | Light Blue | OCMS/PLUS Staff Portal UI |
| Backend (OCMS) | Light Green | OCMS Backend API |
| Database | Light Yellow | Intranet & Internet DB |
| External System | Light Orange | PLUS System |

---

## 2. High-Level Flow (Tab 1: PS_High_Level)

### 2.1 Overview Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         PERMANENT SUSPENSION                             │
│                          HIGH-LEVEL FLOW                                 │
└─────────────────────────────────────────────────────────────────────────┘

                    ┌─────────────────────┐
                    │       START         │
                    └──────────┬──────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
              ▼                ▼                ▼
     ┌────────────────┐ ┌────────────────┐ ┌────────────────┐
     │ Manual PS from │ │ Manual PS from │ │ Auto PS by     │
     │ OCMS Staff     │ │ PLUS Staff     │ │ OCMS Backend   │
     │ Portal         │ │ Portal         │ │                │
     └───────┬────────┘ └───────┬────────┘ └───────┬────────┘
             │                  │                  │
             ▼                  ▼                  ▼
     ┌────────────────┐ ┌────────────────┐        │
     │ Staff Portal   │ │ PLUS Portal    │        │
     │ UI Validation  │ │ UI Validation  │        │
     └───────┬────────┘ └───────┬────────┘        │
             │                  │                  │
             ▼                  ▼                  │
        ┌─────────┐        ┌─────────┐            │
        │Allowed? │        │Allowed? │            │
        └────┬────┘        └────┬────┘            │
         Yes │ No           Yes │ No              │
             │  │               │  │              │
             │  ▼               │  ▼              │
             │ [Error]          │ [Error]         │
             │                  │                 │
             └──────────┬───────┴─────────────────┘
                        │
                        ▼
               ┌────────────────┐
               │ Call OCMS      │
               │ Backend API    │
               └───────┬────────┘
                       │
                       ▼
               ┌────────────────┐
               │ Backend        │
               │ Validation     │
               └───────┬────────┘
                       │
                       ▼
                  ┌─────────┐
                  │Allowed? │
                  └────┬────┘
                   Yes │ No
                       │  │
          ┌────────────┘  └────────────┐
          ▼                            ▼
   ┌────────────────┐          ┌────────────────┐
   │ Apply PS       │          │ Return Error   │
   │ Update DB      │          │ Response       │
   └───────┬────────┘          └───────┬────────┘
           │                           │
           └───────────┬───────────────┘
                       ▼
                  ┌─────────┐
                  │   END   │
                  └─────────┘
```

### 2.2 Process Steps

| Step | Actor | Action | Next Step |
|------|-------|--------|-----------|
| 1 | OIC/PLM/System | Initiate PS request | 2 |
| 2 | Staff Portal | UI-side validation (if manual) | 3 |
| 3 | Staff Portal | Check if PS allowed | 4a (Yes) / 4b (No) |
| 4a | Staff Portal | Call OCMS Backend API | 5 |
| 4b | Staff Portal | Show error message | END |
| 5 | OCMS Backend | Validate PS request | 6 |
| 6 | OCMS Backend | Check if PS allowed | 7a (Yes) / 7b (No) |
| 7a | OCMS Backend | Apply PS, update database | 8 |
| 7b | OCMS Backend | Return error response | 8 |
| 8 | - | End | - |

---

## 3. UI-Side Validation Flow (Tab 2: UI_Side_Validation)

### 3.1 Swimlanes
- **Staff Portal (Frontend)**
- **Database**

### 3.2 Process Steps

| Step | Swimlane | Action | Decision | Yes Path | No Path |
|------|----------|--------|----------|----------|---------|
| 1 | Frontend | OIC access Staff Portal | - | 2 | - |
| 2 | Frontend | OIC browse and select Notice(s) | - | 3 | - |
| 3 | Frontend | OIC clicks "Apply Operation" dropdown | - | 4 | - |
| 4 | Frontend | Check: OIC has permission? | Decision | 5 | END (hide option) |
| 5 | Frontend | Display "Apply PS" option | - | 6 | - |
| 6 | Frontend | OIC selects "Apply PS" | - | 7 | - |
| 7 | Frontend | OIC clicks "Select" button | - | 8 | - |
| 8 | Frontend | Portal validation initiated | - | 9 | - |
| 9 | Frontend | Check: LPS allows PS? | Decision | 13 | 10 |
| 10 | Frontend | Display "Unable to PS at this LPS" popup | - | 11 | - |
| 11 | Frontend | OIC dismisses popup | - | 12 | - |
| 12 | Frontend | OIC unchecks ineligible notices | - | 9 (loop) | - |
| 13 | Frontend | Redirect to Suspension Detail page | - | 14 | - |
| 14 | Database | Query notice details | - | 15 | - |
| 15 | Frontend | Display notice list and form | - | 16 | - |
| 16 | Frontend | OIC selects Suspension Code | - | 17 | - |
| 17 | Frontend | OIC enters remarks (max 200) | - | 18 | - |
| 18 | Frontend | OIC clicks "Apply" button | - | 19 | - |
| 19 | Frontend | Show confirmation dialog | Decision | 20 | END (cancel) |
| 20 | Frontend | Portal detects new PS code | - | 21 | - |
| 21 | Database | Query existing PS suspensions | - | 22 | - |
| 22 | Frontend | Any past suspension found? | Decision | 23 | 28 |
| 23 | Frontend | Sort: (a) No-payment (b) Paid | - | 24 | - |
| 24 | Frontend | For Paid: Check PS code = APP/CFA/VST? | Decision | 26 | 25 |
| 25 | Frontend | Add to "Not Allowed" result list | - | 27 | - |
| 26 | Frontend | Add to "Eligible" list | - | 27 | - |
| 27 | Frontend | Process remaining notices | Loop | 28 | - |
| 28 | Frontend | Call batch revive API (if needed) | - | 29 | - |
| 29 | Frontend | Call apply PS API (batches of 10) | - | 30 | - |
| 30 | Frontend | Receive response | - | 31 | - |
| 31 | Frontend | Display results table | - | END | - |

### 3.3 Decision Points

| # | Decision | Condition | Yes Action | No Action |
|---|----------|-----------|------------|-----------|
| D1 | OIC has permission? | JWT role check | Show Apply PS option | Hide option |
| D2 | LPS allows PS? | Stage in allowed list | Continue | Show error popup |
| D3 | Confirm proceed? | User clicks Yes | Continue processing | Cancel |
| D4 | Past suspension found? | Query returns records | Sort and validate | Call API directly |
| D5 | PS code = APP/CFA/VST? | Code in refund list | Eligible for PS | Not allowed |

---

## 4. Backend-Side Validation Flow (Tab 3: Backend_Side_Validation)

### 4.1 Swimlanes
- **OCMS Backend**
- **Database**

### 4.2 Process Steps

| Step | Swimlane | Action | Decision | Yes Path | No Path |
|------|----------|--------|----------|----------|---------|
| 1 | Backend | PS API request received | - | 2 | - |
| 2 | Backend | Authenticate request (JWT) | Decision | 4 | 3 |
| 3 | Backend | Return: OCMS-4001 Unauthorized | - | END | - |
| 4 | Backend | Validate mandatory fields | Decision | 6 | 5 |
| 5 | Backend | Return: OCMS-4007 Missing field | - | END | - |
| 6 | Backend | Check source permission | Decision | 8 | 7 |
| 7 | Backend | Return: OCMS-4000 Not authorized | - | END | - |
| 8 | Database | Query VON using notice number | Decision | 10 | 9 |
| 9 | Backend | Return: OCMS-4001 Invalid notice | - | END | - |
| 10 | Backend | Check LPS allowed for code | Decision | 12 | 11 |
| 11 | Backend | Return: OCMS-4002 LPS not allowed | - | END | - |
| 12 | Backend | Check paid notice status | Decision | 14 | 13 |
| 13 | Backend | Return: OCMS-4003 Code not allowed | - | END | - |
| 14 | Database | Query Suspended Notice table | - | 15 | - |
| 15 | Backend | Any active PS? | Decision | 16 | 22 |
| 16 | Backend | Same PS code? | Decision | 17 | 18 |
| 17 | Backend | Return: OCMS-2001 Already PS | - | END | - |
| 18 | Backend | Existing = Exception code? | Decision | 22 | 19 |
| 19 | Backend | New = CRS code (FP/PRA)? | Decision | 20 | 21 |
| 20 | Backend | Return: OCMS-4008 CRS not allowed | - | END | - |
| 21 | Backend | Revive existing PS (reason=CSR) | - | 22 | - |
| 22 | Backend | Apply new PS | - | 23 | - |
| 23 | Database | Create PS record in suspended_notice | - | 24 | - |
| 24 | Database | Update VON (suspension fields) | - | 25 | - |
| 25 | Database | Update eVON (sync internet DB) | - | 26 | - |
| 26 | Backend | Return: OCMS-2000 Success | - | END | - |

### 4.3 Decision Points

| # | Decision | Condition | Yes Action | No Action |
|---|----------|-----------|------------|-----------|
| D1 | Auth success? | Valid JWT | Continue | OCMS-4001 |
| D2 | Fields valid? | All required present | Continue | OCMS-4007 |
| D3 | Source allowed? | Code in source list | Continue | OCMS-4000 |
| D4 | Notice exists? | Found in VON | Continue | OCMS-4001 |
| D5 | LPS allowed? | Stage in allowed list | Continue | OCMS-4002 |
| D6 | Paid + valid code? | APP/CFA/VST if paid | Continue | OCMS-4003 |
| D7 | Any active PS? | Query returns active | Check overlap | Apply directly |
| D8 | Same PS code? | New = Existing | Return idempotent | Continue |
| D9 | Exception code? | DIP/FOR/MID/RIP/RP2 | Allow stacking | Check CRS |
| D10 | CRS on non-exception? | FP/PRA on regular PS | Block | Revive first |

---

## 5. OCMS Staff Portal Manual PS (Tab 4: OCMS_Staff_Portal_Manual_PS)

### 5.1 Swimlanes
- **Staff Portal (Frontend)**
- **OCMS Backend**
- **Database**

### 5.2 Process Steps

| Step | Swimlane | Action | Reference |
|------|----------|--------|-----------|
| 1 | Frontend | Start - OIC wishes to apply PS | - |
| 2 | Frontend | Search for Notice(s) | Search function |
| 3 | Frontend | Select Notice(s) from results | Checkbox selection |
| 4 | Frontend | Check OIC permission | FE-001 |
| 5 | Frontend | Check Notice LPS | Section 2.6.2 |
| 6 | Frontend | Redirect to Suspension Detail page | - |
| 7 | Frontend | Enter suspension details | Form input |
| 8 | Frontend | Check PS eligibility | UI validation |
| 9 | Backend | Send PS request to backend | API call |
| 10 | Backend | Backend validation | Section 2.6.3 |
| 11 | Frontend | Display result | Success/Error |
| 12 | Frontend | End | - |

### 5.3 Allowed Suspension Codes (OCMS Staff)

```
ANS, CAN, CFA, CFP, DBB, DIP, FCT, FOR, FTC, IST, MID, OTH,
RIP, RP2, SCT, SLC, SSV, VCT, VST, WWC, WWF, WWP
```

---

## 6. PLUS Staff Portal Manual PS (Tab 5: PLUS_Staff_Portal_Manual_PS)

### 6.1 Swimlanes
- **PLUS Staff Portal (Frontend)**
- **PLUS Backend**
- **OCMS Backend**
- **Database**

### 6.2 Process Steps

| Step | Swimlane | Action | Reference |
|------|----------|--------|-----------|
| 1 | PLUS Frontend | Start - PLM wishes to apply PS | - |
| 2 | PLUS Frontend | Search for Notice by case number | Appeal link |
| 3 | PLUS Frontend | Select Notice(s) | - |
| 4 | PLUS Frontend | Check PLM permission | FE-021 |
| 5 | PLUS Frontend | Enter suspension details | Case number required |
| 6 | PLUS Frontend | Validation (checking=true) | Optional dry-run |
| 7 | PLUS Backend | Call OCMS API | Integration |
| 8 | OCMS Backend | Backend validation | Section 2.6.3 |
| 9 | OCMS Backend | Process PS request | - |
| 10 | PLUS Frontend | Display result | Success/Error |
| 11 | PLUS Frontend | End | - |

### 6.3 Allowed Suspension Codes (PLUS)

```
APP, CAN, CFA, OTH, VST
```

### 6.4 API Parameters (PLUS → OCMS)

| Parameter | Required | Description |
|-----------|----------|-------------|
| notice_no | Yes | Notice number |
| date_of_suspension | Yes | Suspension date |
| suspension_source | No | Default: PLUS |
| case_no | Yes | Appeal case number |
| suspension_type | Yes | PS |
| reason_of_suspension | Yes | PS Code |
| officer_authorising_suspension | Yes | PLM ID |
| suspension_remarks | No | Remarks |

---

## 7. Auto PS Triggers (Tab 6: Auto_PS_Triggers)

### 7.1 Swimlanes
- **OCMS Backend (Cron)**
- **Database**

### 7.2 Auto PS Scenarios

| # | Scenario | PS Code | Trigger Stage | Condition |
|---|----------|---------|---------------|-----------|
| 1 | Advisory Notice | ANS | Notice Creation | Notice type = AN |
| 2 | Double Booking | DBB | Notice Creation | Duplicate detected |
| 3 | Diplomatic Vehicle | DIP | RD2/DN2 End | Vehicle = Diplomatic |
| 4 | Foreign Vehicle | FOR | ROV | Vehicle = Foreign |
| 5 | MINDEF Vehicle | MID | RD2/DN2 End | Vehicle = MINDEF |
| 6 | Fully Paid | FP | Payment | Full payment received |
| 7 | Partial Payment | PRA | Payment | Reduced payment received |
| 8 | Deceased On/After | RIP | MHA Response | Death date >= Offence date |
| 9 | Deceased Before | RP2 | MHA Response | Death date < Offence date |

### 7.3 Process Flow

```
[Batch Job Start]
       │
       ▼
[Query eligible notices]
       │
       ▼
[Check trigger condition]
       │
       ▼
   ┌───────┐
   │ Met?  │
   └───┬───┘
    Yes│ No
       │  └──► [Skip notice]
       ▼
[Call internal PS API]
       │
       ▼
[Update database]
       │
       ▼
[Log result]
       │
       ▼
[Batch Job End]
```

---

## 8. PS Revival Flow (Tab 7: PS_Revival)

### 8.1 Swimlanes
- **Staff Portal (Frontend)**
- **OCMS Backend**
- **Database**

### 8.2 Process Steps

| Step | Swimlane | Action | Decision | Yes Path | No Path |
|------|----------|--------|----------|----------|---------|
| 1 | Frontend | OIC initiates revival | - | 2 | - |
| 2 | Frontend | Select notice(s) to revive | - | 3 | - |
| 3 | Frontend | Enter revival details | - | 4 | - |
| 4 | Backend | Validate request | Decision | 6 | 5 |
| 5 | Backend | Return error | - | END | - |
| 6 | Database | Query active PS | Decision | 8 | 7 |
| 7 | Backend | Return: OCMS-4005 | - | END | - |
| 8 | Backend | Check refund required (FP/PRA) | - | 9 | - |
| 9 | Database | Update suspended_notice (revival) | - | 10 | - |
| 10 | Backend | Check other active PS | Decision | 11 | 12 |
| 11 | Database | Apply latest PS to VON | - | 13 | - |
| 12 | Database | Clear VON suspension fields | - | 13 | - |
| 13 | Backend | Return success | - | 14 | - |
| 14 | Frontend | Display result | - | END | - |

### 8.3 Revival Fields Updated

| Field | Value |
|-------|-------|
| date_of_revival | Current timestamp |
| revival_reason | User input / CSR (auto) |
| officer_authorising_revival | OIC username |
| revival_remarks | User input |
| upd_date | Current timestamp |
| upd_user_id | OIC username |

---

## 9. PS Report Flow (Tab 8: PS_Report)

### 9.1 Swimlanes
- **Staff Portal (Frontend)**
- **OCMS Backend**
- **Database**

### 9.2 Process Steps

| Step | Swimlane | Action |
|------|----------|--------|
| 1 | Frontend | User enters report parameters |
| 2 | Frontend | Validate date range (max 1 year) |
| 3 | Frontend | Submit report request |
| 4 | Backend | Query PS by System data |
| 5 | Database | Query suspended_notice (source, dates) |
| 6 | Database | Query VON (notice details) |
| 7 | Backend | Consolidate data |
| 8 | Backend | Query PS by Officer data |
| 9 | Database | Query suspended_notice (officer, dates) |
| 10 | Backend | Consolidate all data |
| 11 | Backend | Return data to portal |
| 12 | Frontend | Generate report file (XLS) |
| 13 | Frontend | Download to user |
| 14 | Frontend | End |

### 9.3 Report Parameters

| Parameter | Required | Type | Description |
|-----------|----------|------|-------------|
| suspensionDateFrom | Yes | Date | Start date |
| suspensionDateTo | Yes | Date | End date (max 1 year range) |
| psingOfficerName | No | Multi-select | Filter by PSing officer |
| authorisingOfficerName | No | Multi-select | Filter by authorising officer |

### 9.4 Report Output Fields

**PS by System:**
- Vehicle number, Notice Number, Notice Type
- Vehicle Registration Type, Vehicle Category
- Computer Rule Code, Date/time of Offence
- Place of Offence, Source
- Refund Identified Date, Suspension Date
- Suspension Reason, Previous PS info

**PS by Officer:**
- All fields from PS by System
- Authorising Officer, PSing Officer
- Suspension Remarks

---

## 10. Database Operations Summary

### 10.1 Tables Affected

| Table | Operation | Flow |
|-------|-----------|------|
| ocms_valid_offence_notice | UPDATE | Apply PS, Revival |
| eocms_valid_offence_notice | UPDATE | Apply PS, Revival |
| ocms_suspended_notice | INSERT | Apply PS |
| ocms_suspended_notice | UPDATE | Revival |

### 10.2 Fields Updated - Apply PS

**ocms_valid_offence_notice:**
| Field | Value |
|-------|-------|
| suspension_type | "PS" |
| epr_reason_of_suspension | PS Code |
| epr_date_of_suspension | Current timestamp |
| due_date_of_revival | NULL |

**ocms_suspended_notice (INSERT):**
| Field | Value |
|-------|-------|
| notice_no | Notice number |
| date_of_suspension | Current timestamp |
| sr_no | Sequence number |
| suspension_source | OCMS/PLUS |
| case_no | Case number |
| suspension_type | "PS" |
| reason_of_suspension | PS Code |
| officer_authorising_suspension | OIC ID |
| due_date_of_revival | NULL |
| suspension_remarks | Remarks |

---

## 11. Error Handling Summary

| Error Code | Message | Flow Action |
|------------|---------|-------------|
| OCMS-2000 | PS Success | End (success) |
| OCMS-2001 | Already PS | End (idempotent) |
| OCMS-4000 | Source not authorized | End (error) |
| OCMS-4001 | Invalid Notice / Unauthorized | End (error) |
| OCMS-4002 | LPS not allowed | End (error) |
| OCMS-4003 | Paid notice code restriction | End (error) |
| OCMS-4005 | No active PS for revival | End (error) |
| OCMS-4007 | Missing/invalid field | End (error) |
| OCMS-4008 | CRS on non-exception PS | End (error) |

---

## 12. Change Log

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-05 | Technical Documentation | Initial version |

---

*End of Document*

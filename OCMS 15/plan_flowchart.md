# OCMS 15 - Change Processing Stage Flowchart Plan

## Document Information

| Item | Details |
|------|---------|
| Version | 1.0 |
| Date | 2026-01-18 |
| Source Document | v1.1_OCMS15_Change_Processing_Stage.docx |
| Source Flowchart | OCMS15_Functional-Flowchart.drawio |
| Related Documents | plan_api.md, plan_condition.md |
| Author | Technical Documentation |

---

## 1. Diagram Sections Overview

### 1.1 Required Tabs/Pages

| Tab # | Tab Name | Description | Priority |
|-------|----------|-------------|----------|
| 1 | CPS_High_Level | High-level overview of Change Processing Stage process | High |
| 2 | UI_Side_Validation | Staff Portal validation flow | High |
| 3 | Backend_Side_Validation | OCMS Backend eligibility validation flow | High |
| 4 | OCMS_Staff_Portal_Manual_CPS | OCMS Staff Portal complete flow | High |
| 5 | PLUS_Manual_CPS | PLUS Portal integration flow | High |
| 6 | Toppan_Auto_CPS | Toppan cron automated stage processing | Medium |
| 7 | CPS_Report | Change Stage report generation flow | Low |

### 1.2 Swimlanes (Systems)

| Swimlane | Color | Description |
|----------|-------|-------------|
| Staff Portal (Frontend) | Light Blue (#dae8fc) | OCMS Staff Portal UI |
| OCMS Backend | Light Green (#d5e8d4) | OCMS Admin API Intranet |
| Database | Light Yellow (#fff2cc) | Intranet Database |
| External System (PLUS) | Light Orange (#ffe6cc) | PLUS Portal |

---

## 2. High-Level Flow (Tab 1: CPS_High_Level)

### 2.1 Overview Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      CHANGE PROCESSING STAGE                             │
│                         HIGH-LEVEL FLOW                                  │
└─────────────────────────────────────────────────────────────────────────┘

                    ┌─────────────────────┐
                    │       START         │
                    └──────────┬──────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
              ▼                ▼                ▼
     ┌────────────────┐ ┌────────────────┐ ┌────────────────┐
     │ Manual CPS     │ │ Manual CPS     │ │ Auto CPS by    │
     │ from OCMS      │ │ from PLUS      │ │ Toppan Cron    │
     │ Staff Portal   │ │ Portal         │ │                │
     └───────┬────────┘ └───────┬────────┘ └───────┬────────┘
             │                  │                  │
             ▼                  ▼                  │
     ┌────────────────┐ ┌────────────────┐        │
     │ Search Notice  │ │ Call OCMS      │        │
     │ Check Eligible │ │ External API   │        │
     └───────┬────────┘ └───────┬────────┘        │
             │                  │                  │
             ▼                  ▼                  │
        ┌─────────┐        ┌─────────┐            │
        │Eligible?│        │Allowed? │            │
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
                  │Changeable│
                  └────┬────┘
                   Yes │ No
                       │  │
          ┌────────────┘  └────────────┐
          ▼                            ▼
   ┌────────────────┐          ┌────────────────┐
   │ Update VON     │          │ Return Error   │
   │ Insert Audit   │          │ Response       │
   └───────┬────────┘          └───────┬────────┘
           │                           │
           ▼                           │
   ┌────────────────┐                  │
   │ Generate       │                  │
   │ Report         │                  │
   └───────┬────────┘                  │
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
| 1 | OIC/PLM/System | Initiate CPS request | 2 |
| 2 | Staff Portal | Search notices | 3 |
| 3 | Staff Portal | UI-side eligibility check | 4a (Yes) / 4b (No) |
| 4a | Staff Portal | Call OCMS Backend API | 5 |
| 4b | Staff Portal | Show error message | END |
| 5 | OCMS Backend | Validate CPS request | 6 |
| 6 | OCMS Backend | Check if changeable | 7a (Yes) / 7b (No) |
| 7a | OCMS Backend | Update VON, insert audit trail | 8 |
| 7b | OCMS Backend | Return error response | 9 |
| 8 | OCMS Backend | Generate Excel report | 9 |
| 9 | - | End | - |

---

## 3. UI-Side Validation Flow (Tab 2: UI_Side_Validation)

### 3.1 Swimlanes
- **Staff Portal (Frontend)**
- **Database**

### 3.2 Process Steps

| Step | Swimlane | Action | Decision | Yes Path | No Path |
|------|----------|--------|----------|----------|---------|
| 1 | Frontend | OIC access Staff Portal | - | 2 | - |
| 2 | Frontend | OIC enters search criteria | - | 3 | - |
| 3 | Frontend | Check: At least one criteria provided? | Decision | 4 | END (show error) |
| 4 | Frontend | Call Search API | - | 5 | - |
| 5 | Database | Query VON by criteria | - | 6 | - |
| 6 | Database | Query ONOD for offender details | - | 7 | - |
| 7 | Frontend | Any records found? | Decision | 8 | END (show no results) |
| 8 | Frontend | Check: Notice at court stage? | Decision | 9 (No) | 10 (Yes) |
| 9 | Frontend | Add to eligible list | - | 11 | - |
| 10 | Frontend | Add to ineligible list (COURT_STAGE) | - | 11 | - |
| 11 | Frontend | Check: PS active? | Decision | 12 (No) | 13 (Yes) |
| 12 | Frontend | Mark as eligible | - | 14 | - |
| 13 | Frontend | Add to ineligible list (PS_ACTIVE) | - | 14 | - |
| 14 | Frontend | Display search results (segregated) | - | 15 | - |
| 15 | Frontend | OIC selects eligible notice(s) | - | 16 | - |
| 16 | Frontend | OIC selects new processing stage | - | 17 | - |
| 17 | Frontend | Check: Stage valid for role? | Decision | 18 | END (show error) |
| 18 | Frontend | OIC selects reason of change | - | 19 | - |
| 19 | Frontend | Check: Reason = OTH? | Decision | 20 | 21 |
| 20 | Frontend | Require remarks input | - | 21 | - |
| 21 | Frontend | OIC clicks Submit | - | 22 | - |
| 22 | Frontend | Call Validate API | - | 23 | - |
| 23 | Frontend | Display validation results | - | 24 | - |
| 24 | Frontend | OIC confirms submission | - | 25 | - |
| 25 | Frontend | Call Submit API | - | END | - |

### 3.3 Decision Points

| # | Decision | Condition | Yes Action | No Action |
|---|----------|-----------|------------|-----------|
| D1 | Search criteria provided? | At least one field filled | Proceed search | Show VAL010 error |
| D2 | Records found? | Query returns results | Classify notices | Show no results |
| D3 | Court stage? | Stage IN (CRT, CRC, CFI) | Mark ineligible | Check PS |
| D4 | PS active? | suspensionType = PS | Mark ineligible | Mark eligible |
| D5 | Stage valid for role? | DRIVER→DN stages, OWNER→RD stages | Proceed | Show error |
| D6 | Reason = OTH? | reasonOfChange = "OTH" | Require remarks | Optional remarks |

---

## 4. Backend-Side Validation Flow (Tab 3: Backend_Side_Validation)

### 4.1 Swimlanes
- **OCMS Backend**
- **Database**

### 4.2 Process Steps

| Step | Swimlane | Action | Decision | Yes Path | No Path |
|------|----------|--------|----------|----------|---------|
| 1 | Backend | CPS API request received | - | 2 | - |
| 2 | Backend | Authenticate request (JWT) | Decision | 4 | 3 |
| 3 | Backend | Return: 401 Unauthorized | - | END | - |
| 4 | Backend | Validate mandatory fields | Decision | 6 | 5 |
| 5 | Backend | Return: 400 Missing field | - | END | - |
| 6 | Database | Query VON by noticeNo | Decision | 8 | 7a |
| 7a | Database | Query ONOD by noticeNo | Decision | 8 | 7b |
| 7b | Backend | Return: OCMS.CPS.ELIG.NOT_FOUND | - | END | - |
| 8 | Backend | Resolve offender role | Decision | 10 | 9 |
| 9 | Backend | Return: OCMS.CPS.ELIG.ROLE_CONFLICT | - | END | - |
| 10 | Backend | Check: Court stage? | Decision | 11 (No) | 12 |
| 11 | Backend | Continue | - | 13 | - |
| 12 | Backend | Return: OCMS.CPS.ELIG.COURT_STAGE | - | END | - |
| 13 | Backend | Check: PS active? (configurable) | Decision | 14 (No) | 15 |
| 14 | Backend | Continue | - | 16 | - |
| 15 | Backend | Return: OCMS.CPS.ELIG.PS_BLOCKED | - | END | - |
| 16 | Backend | Resolve target stage | Decision | 18 | 17 |
| 17 | Backend | Return: OCMS.CPS.ELIG.NO_STAGE_RULE | - | END | - |
| 18 | Backend | Get eligible stages for role | - | 19 | - |
| 19 | Backend | Check: Stage in eligible set? | Decision | 21 | 20 |
| 20 | Backend | Return: OCMS.CPS.ELIG.INELIGIBLE_STAGE | - | END | - |
| 21 | Database | Check duplicate change today | Decision | 22 (No) | 23 |
| 22 | Backend | Proceed with change | - | 24 | - |
| 23 | Backend | Check: isConfirmation=true? | Decision | 22 | 24 |
| 24 | Backend | Return: EXISTING_CHANGE_TODAY warning | - | END | - |
| 25 | Backend | Mark as CHANGEABLE | - | END | - |

### 4.3 Decision Points

| # | Decision | Condition | Yes Action | No Action |
|---|----------|-----------|------------|-----------|
| D1 | Auth success? | Valid JWT | Continue | 401 Unauthorized |
| D2 | Fields valid? | All required present | Continue | 400 Bad Request |
| D3 | Notice exists? | Found in VON or ONOD | Continue | NOT_FOUND |
| D4 | Role resolved? | ONOD.ownerDriverIndicator present | Continue | ROLE_CONFLICT |
| D5 | Court stage? | Stage IN (CRT, CRC, CFI) | COURT_STAGE error | Continue |
| D6 | PS active? | suspensionType = PS | PS_BLOCKED (optional) | Continue |
| D7 | Stage derivable? | StageMap lookup success | Continue | NO_STAGE_RULE |
| D8 | Stage eligible? | Stage in role's allowed set | CHANGEABLE | INELIGIBLE_STAGE |
| D9 | Duplicate today? | Exists in ocms_change_of_processing | Check confirmation | Proceed |
| D10 | Confirmed? | isConfirmation = true | Proceed | Return warning |

### 4.4 Eligible Stages by Role

| Role | Eligible Stages |
|------|-----------------|
| DRIVER | DN1, DN2, DR3 |
| OWNER | ROV, RD1, RD2, RR3 |
| HIRER | ROV, RD1, RD2, RR3 |
| DIRECTOR | ROV, RD1, RD2, RR3 |

---

## 5. OCMS Staff Portal Manual CPS (Tab 4: OCMS_Staff_Portal_Manual_CPS)

### 5.1 Swimlanes
- **Staff Portal (Frontend)**
- **OCMS Backend**
- **Database**

### 5.2 Process Steps

| Step | Swimlane | Action | Reference |
|------|----------|--------|-----------|
| 1 | Frontend | Start - OIC wishes to change processing stage | - |
| 2 | Frontend | Enter search criteria | At least one required |
| 3 | Frontend | Click Search | - |
| 4 | Backend | Call Search API | API 2.1 |
| 5 | Database | Query VON and ONOD | - |
| 6 | Backend | Classify eligible vs ineligible | Eligibility rules |
| 7 | Frontend | Display segregated results | - |
| 8 | Frontend | Select eligible notice(s) | Checkbox |
| 9 | Frontend | Select new processing stage | Dropdown |
| 10 | Frontend | Select reason of change | Dropdown |
| 11 | Frontend | Enter remarks (if OTH) | Text field |
| 12 | Frontend | Click Submit | - |
| 13 | Backend | Call Validate API | API 2.2 |
| 14 | Frontend | Display changeable vs non-changeable | - |
| 15 | Frontend | OIC confirms to proceed | Confirmation dialog |
| 16 | Backend | Call Submit API | API 2.3 |
| 17 | Backend | Validate eligibility | Backend validation |
| 18 | Backend | Calculate new amount_payable | - |
| 19 | Database | Update VON (stages, dates, amount) | - |
| 20 | Database | Insert ocms_change_of_processing | Audit trail |
| 21 | Backend | Generate Excel report | - |
| 22 | Frontend | Display results with report URL | - |
| 23 | Frontend | End | - |

### 5.3 Search Criteria Fields

| Field | Type | Required | Max Length |
|-------|------|----------|------------|
| noticeNo | String | No | 10 |
| idNo | String | No | 20 |
| vehicleNo | String | No | 14 |
| currentProcessingStage | String | No | 3 |
| dateOfCurrentProcessingStage | Date | No | - |

> **Note:** At least one field must be provided.

### 5.4 Stage Change Form Fields

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| newProcessingStage | Dropdown | Yes | Must be eligible for role |
| reasonOfChange | Dropdown | Yes | Valid reason codes |
| remarks | Text | Conditional | Required if reason = OTH |

---

## 6. PLUS Manual CPS (Tab 5: PLUS_Manual_CPS)

### 6.1 Swimlanes
- **PLUS Portal (Frontend)**
- **OCMS Backend**
- **Database**

### 6.2 Process Steps

| Step | Swimlane | Action | Decision | Yes Path | No Path |
|------|----------|--------|----------|----------|---------|
| 1 | PLUS Frontend | Start - PLM initiates CPS | - | 2 | - |
| 2 | PLUS Frontend | Send CPS request to OCMS | - | 3 | - |
| 3 | Backend | Receive PLUS API request | - | 4 | - |
| 4 | Backend | Check: source = 005? | Decision | 5 | 6 |
| 5 | Backend | Check: nextStage = CFC? | Decision | 7 (No) | 8 |
| 6 | Backend | Proceed normal flow | - | 9 | - |
| 7 | Backend | Continue PLUS flow | - | 9 | - |
| 8 | Backend | Return: OCMS-4004 CFC not allowed | - | END | - |
| 9 | Backend | Check: offenceType=U AND stage skip? | Decision | 10 | 11 |
| 10 | Backend | Return: SUCCESS (skip processing) | - | END | - |
| 11 | Database | Query stage_map validation | Decision | 13 | 12 |
| 12 | Backend | Return: OCMS-4000 Stage invalid | - | END | - |
| 13 | Backend | For each notice: eligibility check | - | 14 | - |
| 14 | Database | Update VON stages | - | 15 | - |
| 15 | Database | Insert audit trail | - | 16 | - |
| 16 | Backend | Compile success/failed lists | - | 17 | - |
| 17 | Backend | Return response to PLUS | - | END | - |

### 6.3 PLUS Restrictions

| Restriction | Condition | Error Code |
|-------------|-----------|------------|
| CFC not allowed | source=005 AND nextStage=CFC | OCMS-4004 |
| Skip condition | offenceType=U AND stage IN (DN1,DN2,DR3,CPC) | Skip (SUCCESS) |
| Stage map invalid | Transition not in ocms_stage_map | OCMS-4000 |

### 6.4 API Parameters (PLUS → OCMS)

| Parameter | Required | Type | Description |
|-----------|----------|------|-------------|
| noticeNo | Yes | Array | List of notice numbers |
| lastStageName | Yes | String | Current processing stage |
| nextStageName | Yes | String | New processing stage |
| lastStageDate | No | DateTime | Last stage date |
| newStageDate | No | DateTime | New stage date |
| offenceType | Yes | String | O/D/H/DIR |
| source | Yes | String | "005" for PLUS |

---

## 7. Toppan Auto CPS (Tab 6: Toppan_Auto_CPS)

### 7.1 Swimlanes
- **OCMS Backend (Cron)**
- **Database**

### 7.2 Process Steps

| Step | Swimlane | Action | Decision | Yes Path | No Path |
|------|----------|--------|----------|----------|---------|
| 1 | Cron | Toppan batch job starts | - | 2 | - |
| 2 | Cron | Generate Toppan enquiry files | - | 3 | - |
| 3 | Cron | Collect processed notice numbers | - | 4 | - |
| 4 | Cron | Call internal Toppan API | - | 5 | - |
| 5 | Backend | Receive Toppan update request | - | 6 | - |
| 6 | Backend | For each notice in batch | Loop | 7 | 18 |
| 7 | Database | Query VON by noticeNo | Decision | 9 | 8 |
| 8 | Backend | Add to skipped list | - | 6 (next) | - |
| 9 | Backend | Check: Stage matches currentStage? | Decision | 11 | 10 |
| 10 | Backend | Add to skipped (stage mismatch) | - | 6 (next) | - |
| 11 | Database | Query ocms_change_of_processing | - | 12 | - |
| 12 | Backend | Check: Manual change exists today? | Decision | 13 | 14 |
| 13 | Backend | Mark as MANUAL update | - | 15 | - |
| 14 | Backend | Mark as AUTOMATIC update | - | 16 | - |
| 15 | Database | Update VON (stage only, NOT amount) | - | 17 | - |
| 16 | Database | Update VON (stage + amount_payable) | - | 17 | - |
| 17 | Database | Insert/update audit trail | - | 6 (next) | - |
| 18 | Backend | Compile statistics | - | 19 | - |
| 19 | Backend | Return response | - | END | - |

### 7.3 Manual vs Automatic Detection

| Type | Condition | VON Update |
|------|-----------|------------|
| MANUAL | Change record exists with source IN (OCMS, PLUS) on same date | Stage only (amount_payable NOT touched) |
| AUTOMATIC | No manual change record | Stage + amount_payable recalculated |

### 7.4 Toppan Response Fields

| Field | Type | Description |
|-------|------|-------------|
| totalNotices | Integer | Total notices in request |
| automaticUpdates | Integer | Count of automatic updates |
| manualUpdates | Integer | Count of manual updates |
| skipped | Integer | Count of skipped notices |
| errors | Array | List of error messages |
| success | Boolean | Overall success flag |

---

## 8. CPS Report Flow (Tab 7: CPS_Report)

### 8.1 Swimlanes
- **Staff Portal (Frontend)**
- **OCMS Backend**
- **Database**

### 8.2 Process Steps

| Step | Swimlane | Action |
|------|----------|--------|
| 1 | Frontend | User enters report parameters |
| 2 | Frontend | Validate date range (max 90 days) |
| 3 | Frontend | Submit report request |
| 4 | Backend | Call Reports API |
| 5 | Database | Query ocms_change_of_processing |
| 6 | Database | Join with VON for notice details |
| 7 | Backend | Compile report data |
| 8 | Backend | Return report list |
| 9 | Frontend | Display available reports |
| 10 | Frontend | User clicks download |
| 11 | Backend | Call Download API |
| 12 | Backend | Retrieve file from Azure Blob |
| 13 | Frontend | Download Excel file |
| 14 | Frontend | End |

### 8.3 Report Parameters

| Parameter | Required | Type | Validation |
|-----------|----------|------|------------|
| startDate | Yes | Date | Format: yyyy-MM-dd |
| endDate | Yes | Date | Format: yyyy-MM-dd, <= 90 days from start |

### 8.4 Report Output Fields

| Field | Source |
|-------|--------|
| Notice Number | ocms_change_of_processing.notice_no |
| Previous Stage | ocms_change_of_processing.last_processing_stage |
| New Stage | ocms_change_of_processing.new_processing_stage |
| Date of Change | ocms_change_of_processing.date_of_change |
| Reason of Change | ocms_change_of_processing.reason_of_change |
| Authorised Officer | ocms_change_of_processing.authorised_officer |
| Source | ocms_change_of_processing.source |
| Remarks | ocms_change_of_processing.remarks |
| Offender Name | ocms_valid_offence_notice.offender_name |
| Vehicle Number | ocms_valid_offence_notice.vehicle_no |

---

## 9. Database Operations Summary

### 9.1 Tables Affected

| Table | Operation | Flow |
|-------|-----------|------|
| ocms_valid_offence_notice | SELECT | Search, Eligibility |
| ocms_valid_offence_notice | UPDATE | Apply CPS |
| ocms_offence_notice_owner_driver | SELECT | Role resolution |
| ocms_offence_notice_owner_driver | UPDATE | DH MHA check (optional) |
| ocms_change_of_processing | SELECT | Duplicate check |
| ocms_change_of_processing | INSERT | Audit trail |
| ocms_stage_map | SELECT | Stage validation |
| ocms_parameter | SELECT | NEXT_STAGE, STAGEDAYS |

### 9.2 Fields Updated - Apply CPS

**ocms_valid_offence_notice:**

| Field | Value |
|-------|-------|
| prev_processing_stage | Old last_processing_stage |
| prev_processing_date | Old last_processing_date |
| last_processing_stage | New stage |
| last_processing_date | Current timestamp |
| next_processing_stage | From ocms_parameter (NEXT_STAGE_{newStage}) |
| next_processing_date | Current + STAGEDAYS |
| amount_payable | Calculated (if automatic) |

**ocms_change_of_processing (INSERT):**

| Field | Value |
|-------|-------|
| notice_no | Notice number |
| date_of_change | Current date |
| last_processing_stage | Previous stage |
| new_processing_stage | New stage |
| reason_of_change | Reason code |
| authorised_officer | User ID |
| source | OCMS/PLUS/SYSTEM |
| remarks | User remarks |
| cre_date | Current timestamp |
| cre_user_id | User ID |

---

## 10. Error Handling Summary

| Error Code | Message | Flow Action |
|------------|---------|-------------|
| OCMS.CPS.ELIG.NOT_FOUND | Notice not found | End (error) |
| OCMS.CPS.ELIG.ROLE_CONFLICT | Cannot determine role | End (error) |
| OCMS.CPS.ELIG.COURT_STAGE | Notice at court stage | End (error) |
| OCMS.CPS.ELIG.PS_BLOCKED | PS active | End (error) |
| OCMS.CPS.ELIG.NO_STAGE_RULE | Cannot derive stage | End (error) |
| OCMS.CPS.ELIG.INELIGIBLE_STAGE | Stage not allowed | End (error) |
| OCMS.CPS.ELIG.EXISTING_CHANGE_TODAY | Duplicate warning | Prompt confirm |
| OCMS.CPS.REMARKS_REQUIRED | Remarks mandatory | End (error) |
| OCMS-4000 | Stage transition invalid | End (error) |
| OCMS-4004 | CFC not allowed from PLUS | End (error) |

---

## 11. Flowchart Checklist

Before creating the .drawio file:

- [ ] All steps have clear names
- [ ] All decision points have Yes/No paths defined
- [ ] All paths lead to an End point
- [ ] Error handling paths are included
- [ ] Database operations are marked with dashed lines
- [ ] Swimlanes are defined for each system/tier
- [ ] Color coding is consistent
- [ ] Step descriptions are complete
- [ ] API payloads are documented
- [ ] Stage transition rules are clear

---

## 12. Change Log

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-18 | Technical Documentation | Initial version |

---

*End of Document*

# Validation Plan - OCMS 17 Temporary Suspension

**Document Information**
- Version: 1.0
- Date: 10/01/2026
- Source: OCMS 17 Functional Document, TS Rules Excel Worksheet

---

## 1. Frontend Validations (UI-Side)

These validations are performed by OCMS/PLUS Staff Portal before sending request to backend.

### 1.1 Form Field Validations

| ID | Field | Rule | Error Message |
|----|-------|------|---------------|
| FE-001 | Notice Selection | At least 1 notice must be selected | Please select at least one notice |
| FE-002 | Notice Selection | Maximum 10 notices per batch | Maximum 10 notices can be selected at once |
| FE-003 | TS Code | Must be selected from dropdown | Please select a suspension code |
| FE-004 | Suspension Remarks | Maximum 200 characters | Remarks cannot exceed 200 characters |
| FE-005 | Officer Name | Must not be empty | Officer name is required |

### 1.2 User Permission Validation

| ID | Check | Action if Failed |
|----|-------|------------------|
| FE-010 | User has TS permission | Hide "Apply TS" option from dropdown |
| FE-011 | User role is OIC or PLM | Display "Not authorized" message |

### 1.3 Notice Eligibility Validation (based on data from backend)

| ID | Field to Check | Validation Rule | Error Message |
|----|----------------|-----------------|---------------|
| FE-020 | last_processing_stage | Must be in allowed stages for selected TS code | TS cannot be applied at this processing stage |
| FE-021 | suspension_type | If "PS", check if TS allowed on top | TS cannot be applied on notices with PS |
| FE-022 | suspension_type | If "TS", check if new TS can override | Notice already has active TS |
| FE-023 | amount_paid | If > 0 and TS code doesn't allow paid notices | TS cannot be applied to paid notices |

### 1.4 UI State Management

| Scenario | UI Behavior |
|----------|-------------|
| All selected notices eligible | Enable "Submit" button, show count |
| Some notices ineligible | Show warning, list ineligible notices, allow proceed with eligible only |
| All notices ineligible | Disable "Submit" button, show error message |
| API call in progress | Disable "Submit" button, show loading spinner |
| API call success | Show success message with count, refresh notice list |
| API call failure | Show error message, keep form data |

---

## 2. Backend Validations (Server-Side)

These validations are performed by OCMS Backend when receiving TS request.

### 2.1 Authentication & Authorization

| Rule ID | Validation | Error Code | Error Message |
|---------|------------|------------|---------------|
| BE-001 | JWT token is valid | OCMS-4001 | You are not authorized. Please log in and try again. |
| BE-002 | User has TS permission | OCMS-4001 | You are not authorized to perform this operation. |
| BE-003 | Suspension source is valid (OCMS/PLUS/Backend) | OCMS-4000 | Invalid suspension source |

### 2.2 Request Payload Validation

| Rule ID | Field | Validation | Error Code | Error Message |
|---------|-------|------------|------------|---------------|
| BE-010 | noticeNo | Not empty, array with 1-10 items | OCMS-4000 | Invalid notice number list |
| BE-011 | suspensionType | Must be "TS" | OCMS-4000 | Invalid suspension type |
| BE-012 | reasonOfSuspension | Must exist in ocms_suspension_reason table | OCMS-4000 | Invalid suspension code |
| BE-013 | reasonOfSuspension | Status must be 'A' (Active) | OCMS-4000 | Suspension code is not active |
| BE-014 | suspensionRemarks | Max 200 characters | OCMS-4000 | Remarks exceed maximum length |
| BE-015 | officerAuthorisingSuspension | Not empty, max 50 characters | OCMS-4000 | Invalid officer name |

### 2.3 Notice Existence Validation

| Rule ID | Validation | Error Code | Error Message |
|---------|------------|------------|---------------|
| BE-020 | Notice exists in ocms_valid_offence_notice | OCMS-4001 | Notice not found |

### 2.4 Notice Payment Validation

| Rule ID | Validation | Error Code | Error Message |
|---------|------------|------------|---------------|
| BE-023 | Notice is not paid or partially paid | OCMS-4003 | TS cannot be applied to paid notices |

### 2.5 Source Authorization Validation

| Rule ID | Validation | Error Code | Error Message |
|---------|------------|------------|---------------|
| BE-030 | If source = "OCMS", check suspension_source = 'OCMS_FE' in ocms_suspension_reason | OCMS-4000 | Source not authorized to use this Suspension Code |
| BE-031 | If source = "PLUS", check suspension_source = 'PLUS' in ocms_suspension_reason | OCMS-4000 | Source not authorized to use this Suspension Code |
| BE-032 | If source = "Backend", check suspension_source = 'OCMS_BE' in ocms_suspension_reason | OCMS-4000 | Source not authorized to use this Suspension Code |

**Note:** The `suspension_source` field in `ocms_suspension_reason` table determines which source is authorized. Values: OCMS_FE (OCMS Staff Portal), PLUS (PLUS Staff Portal), OCMS_BE (OCMS Backend Auto).

### 2.6 Processing Stage Validation

| Rule ID | Validation | Error Code | Error Message |
|---------|------------|------------|---------------|
| BE-040 | last_processing_stage must be in allowed stages for TS code | OCMS-4002 | TS Code cannot be applied due to Last Processing Stage |

**Note:** Allowed stages per TS code are hardcoded in application logic (not stored in DB). See Section 3 for allowed stages per TS code.

### 2.7 Existing Suspension Validation

| Rule ID | Current Suspension | New TS Code | Action | Response |
|---------|-------------------|-------------|--------|----------|
| BE-050 | PS (any code) | Any TS | Check if PS code in [DIP, MID, FOR, RIP, RP2] | OCMS-4002 if PS not in allowed list |
| BE-051 | TS (same/later revival date) | Any TS | Return success without re-applying | OCMS-2001 (already has TS) |
| BE-052 | TS (earlier revival date) | Any TS | Apply new TS (update revival date) | OCMS-2000 |
| BE-053 | No suspension | Any TS | Apply directly | OCMS-2000 |

---

## 3. Business Rules - 18 TS Suspension Codes

Complete rules for all 18 TS codes from TS Rules Excel Worksheet.

**Important Notes on DB Fields:**
- **suspension_source** field in `ocms_suspension_reason` controls which source can use each TS code:
  - `OCMS_FE` = OCMS Staff Portal Manual TS Allowed
  - `PLUS` = PLUS Staff Portal Manual TS Allowed
  - `OCMS_BE` = OCMS Backend Auto TS Allowed
- **Looping TS** and **Allowed Stages** are NOT stored in DB - they are hardcoded in application logic
- **no_of_days_for_revival** field stores the suspension duration in days

### 3.1 TS Code: ACR (ACRA)

| Rule | Value |
|------|-------|
| Description | ACRA |
| Looping TS | No |
| Suspension Days | 21 days |
| Allowed Stages | ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3, CPC, CFC |
| OCMS Backend Auto TS | Yes |
| OCMS Staff Manual TS | Yes |
| PLUS Staff Manual TS | No |
| Auto Trigger Scenario | ACRA data exception detected during ROV sync |

**Validation Logic:**
```
IF suspension_source = "OCMS" AND reasonOfSuspension = "ACR" THEN
  IF last_processing_stage IN ["ROV", "ENA", "RD1", "RD2", "RR3", "DN1", "DN2", "DR3", "CPC", "CFC"] THEN
    ALLOW
  ELSE
    REJECT with OCMS-4002
  END IF
ELSE IF suspension_source = "PLUS" THEN
  REJECT with OCMS-4000 (PLUS not authorized)
END IF
```

---

### 3.2 TS Code: APE (Appeal Extension)

| Rule | Value |
|------|-------|
| Description | Appeal Extension |
| Looping TS | No |
| Suspension Days | 14 days |
| Allowed Stages | NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3, CPC, CFC |
| OCMS Backend Auto TS | No |
| OCMS Staff Manual TS | No |
| PLUS Staff Manual TS | Yes |
| Auto Trigger Scenario | N/A (Manual only) |

**Validation Logic:**
```
IF suspension_source = "PLUS" AND reasonOfSuspension = "APE" THEN
  IF last_processing_stage IN ["NPA", "ROV", "ENA", "RD1", "RD2", "RR3", "DN1", "DN2", "DR3", "CPC", "CFC"] THEN
    ALLOW
  ELSE
    REJECT with OCMS-4002
  END IF
ELSE IF suspension_source IN ["OCMS", "Backend"] THEN
  REJECT with OCMS-4000 (Not authorized)
END IF
```

---

### 3.3 TS Code: APP (Appeal)

| Rule | Value |
|------|-------|
| Description | Appeal |
| Looping TS | No |
| Suspension Days | 21 days |
| Allowed Stages | NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3, CPC, CFC |
| OCMS Backend Auto TS | No |
| OCMS Staff Manual TS | No |
| PLUS Staff Manual TS | Yes |
| Auto Trigger Scenario | N/A (Manual only) |

**Validation Logic:**
```
IF suspension_source = "PLUS" AND reasonOfSuspension = "APP" THEN
  IF last_processing_stage IN ["NPA", "ROV", "ENA", "RD1", "RD2", "RR3", "DN1", "DN2", "DR3", "CPC", "CFC"] THEN
    ALLOW
  ELSE
    REJECT with OCMS-4002
  END IF
ELSE
  REJECT with OCMS-4000
END IF
```

---

### 3.4 TS Code: CCE (Call Centre Enquiry)

| Rule | Value |
|------|-------|
| Description | Call Centre Enquiry |
| Looping TS | No |
| Suspension Days | 1 day |
| Allowed Stages | NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3, CPC, CFC |
| OCMS Backend Auto TS | No |
| OCMS Staff Manual TS | No |
| PLUS Staff Manual TS | Yes |
| Auto Trigger Scenario | N/A (Manual only) |

---

### 3.5 TS Code: CLV (Classified Vehicles) **[LOOPING]**

| Rule | Value |
|------|-------|
| Description | Classified Vehicles |
| **Looping TS** | **Yes - Auto re-apply immediately after revival** |
| Suspension Days | 21 days |
| Allowed Stages | **RR3, DR3 ONLY** |
| OCMS Backend Auto TS | Yes |
| OCMS Staff Manual TS | Yes |
| PLUS Staff Manual TS | No |
| Auto Trigger Scenario | Classified vehicle detected during processing |

**Special Looping Logic:**
```
WHEN due_date_of_revival is reached:
  1. Revive existing TS with revival_reason = "Auto Revival - Looping TS"
  2. IMMEDIATELY create new TS with same code "CLV"
  3. Set new due_date_of_revival = CURRENT_TIMESTAMP + 90 days
  4. Repeat until manually stopped by OIC
```

**Validation Logic:**
```
IF reasonOfSuspension = "CLV" THEN
  IF last_processing_stage IN ["RR3", "DR3"] THEN
    ALLOW
  ELSE
    REJECT with OCMS-4002 "CLV can only be applied at RR3 or DR3 stage"
  END IF
END IF
```

---

### 3.6 TS Code: FPL (Furnished Particulars Late)

| Rule | Value |
|------|-------|
| Description | Furnished Particulars Late |
| Looping TS | No |
| Suspension Days | 14 days |
| Allowed Stages | NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3, CPC, CFC |
| OCMS Backend Auto TS | No |
| OCMS Staff Manual TS | Yes |
| PLUS Staff Manual TS | No |

---

### 3.7 TS Code: HST (House Tenants) **[LOOPING]**

| Rule | Value |
|------|-------|
| Description | House Tenants |
| **Looping TS** | **Yes - Auto re-apply immediately after revival** |
| Suspension Days | 30 days |
| Allowed Stages | ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3, CPC, CFC |
| OCMS Backend Auto TS | Yes |
| OCMS Staff Manual TS | Yes (From HST screen only) |
| PLUS Staff Manual TS | No |
| Auto Trigger Scenario | House Tenants data exception detected |

**Special Looping Logic:**
```
WHEN due_date_of_revival is reached:
  1. Revive existing TS with revival_reason = "Auto Revival - Looping TS"
  2. IMMEDIATELY create new TS with same code "HST"
  3. Set new due_date_of_revival = CURRENT_TIMESTAMP + 30 days
  4. Repeat until:
     - House Tenants case resolved in HST screen, OR
     - Manually revived by OIC
```

---

### 3.8 TS Code: INS (Insufficient Particulars)

| Rule | Value |
|------|-------|
| Description | Insufficient Particulars |
| Looping TS | No |
| Suspension Days | 14 days |
| Allowed Stages | ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3, CPC, CFC |
| OCMS Backend Auto TS | No |
| OCMS Staff Manual TS | Yes |
| PLUS Staff Manual TS | No |

---

### 3.9 TS Code: MS (Miscellaneous)

| Rule | Value |
|------|-------|
| Description | Miscellaneous |
| Looping TS | No |
| Suspension Days | 21 days |
| Allowed Stages | NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3, CPC, CFC |
| OCMS Backend Auto TS | No |
| OCMS Staff Manual TS | Yes |
| PLUS Staff Manual TS | Yes |

---

### 3.10 TS Code: NRO (Offender Data Exception from MHA or DataHive)

| Rule | Value |
|------|-------|
| Description | Offender Data Exception from MHA or DataHive |
| Looping TS | Note: If OCMS auto send this Notice to MHA/DH queue after revival, then it is not auto looping. It is the natural exception handling process for MHA/DH that applies the TS-NRO again. |
| Suspension Days | 21 days |
| Allowed Stages | ROV, ENA, RD1, RD2, DN1, DN2, CPC, CFC |
| OCMS Backend Auto TS | Yes |
| OCMS Staff Manual TS | Yes |
| PLUS Staff Manual TS | No |
| Auto Trigger Scenario | MHA or DataHive data exception detected |

---

### 3.11 TS Code: OLD (Pending Investigations)

| Rule | Value |
|------|-------|
| Description | Pending Investigations |
| Looping TS | No |
| Suspension Days | 21 days |
| Allowed Stages | NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3, CPC, CFC |
| OCMS Backend Auto TS | No |
| OCMS Staff Manual TS | Yes |
| PLUS Staff Manual TS | No |

---

### 3.12 TS Code: OUT (Enquiry Outstanding Notice EP)

| Rule | Value |
|------|-------|
| Description | Enquiry Outstanding Notice (EP) |
| Looping TS | No |
| Suspension Days | 21 days |
| Allowed Stages | NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3, CPC, CFC |
| OCMS Backend Auto TS | No |
| OCMS Staff Manual TS | Yes |
| PLUS Staff Manual TS | No |

---

### 3.13 TS Code: PAM (Partially Matched)

| Rule | Value |
|------|-------|
| Description | Partially Matched |
| Looping TS | No |
| Suspension Days | 21 days |
| Allowed Stages | **All stages (including unexecuted Warrant of Arrest)** |
| OCMS Backend Auto TS | Yes |
| OCMS Staff Manual TS | Yes |
| PLUS Staff Manual TS | No |
| Auto Trigger Scenario | Partially matched offender data detected |

**Special Rule:** PAM is the ONLY TS code that can be applied at ALL processing stages, including Warrant of Arrest stage.

---

### 3.14 TS Code: PDP (Pending Driver Furnish)

| Rule | Value |
|------|-------|
| Description | Pending Driver Furnish |
| Looping TS | No |
| Suspension Days | 21 days |
| Allowed Stages | ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3, CPC, CFC |
| OCMS Backend Auto TS | Yes |
| OCMS Staff Manual TS | Yes |
| PLUS Staff Manual TS | No |
| Auto Trigger Scenario | Pending driver particulars detected |

---

### 3.15 TS Code: PRI (Pending Release from Prison)

| Rule | Value |
|------|-------|
| Description | Pending Release from Prison |
| Looping TS | No |
| Suspension Days | 180 days |
| Allowed Stages | NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3, CPC, CFC |
| OCMS Backend Auto TS | No |
| OCMS Staff Manual TS | Yes |
| PLUS Staff Manual TS | Yes |

---

### 3.16 TS Code: RED (Pay Reduced Amount)

| Rule | Value |
|------|-------|
| Description | Pay Reduced Amount (refer to OCMS 16) |
| Looping TS | No |
| Suspension Days | 14 days |
| Allowed Stages | NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3, CPC, CFC |
| OCMS Backend Auto TS | No |
| OCMS Staff Manual TS | No |
| PLUS Staff Manual TS | Yes |

---

### 3.17 TS Code: ROV (Vehicle Ownership Data Exception from ROV)

| Rule | Value |
|------|-------|
| Description | Vehicle Ownership Data Exception from ROV |
| Looping TS | Note: If OCMS auto send this Notice to ROV queue after revival, then it is not auto looping. It is the natural exception handling process for ROV that applies the TS-ROV again. |
| Suspension Days | 21 days |
| Allowed Stages | **ROV ONLY** |
| OCMS Backend Auto TS | Yes |
| OCMS Staff Manual TS | Yes |
| PLUS Staff Manual TS | No |
| Auto Trigger Scenario | ROV data exception detected |

**Special Rule:** ROV is ONLY applicable at ROV processing stage.

---

### 3.18 TS Code: SYS (System issue)

| Rule | Value |
|------|-------|
| Description | System issue |
| Looping TS | No |
| Suspension Days | 21 days |
| Allowed Stages | NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3, CPC, CFC |
| OCMS Backend Auto TS | Yes |
| OCMS Staff Manual TS | Yes |
| PLUS Staff Manual TS | No |
| Auto Trigger Scenario | System error detected during processing |

---

### 3.19 TS Code: UNC (Returned as unclaimed)

| Rule | Value |
|------|-------|
| Description | Returned as unclaimed |
| Looping TS | No |
| Suspension Days | 21 days |
| Allowed Stages | NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3, CPC, CFC |
| OCMS Backend Auto TS | No |
| OCMS Staff Manual TS | Yes |
| PLUS Staff Manual TS | No |

**Special Rule:** TS-UNC CANNOT be applied to any notice with existing PS (including exceptions like DIP, MID, FOR, RIP, RP2).

---

## 4. Decision Trees

### 4.1 Can TS be Applied? (Decision Tree)

```
START: Receive TS Request
  |
  ├─> Check: Notice exists?
  |     ├─> NO: Return OCMS-4004 "Notice not found"
  |     └─> YES: Continue
  |
  ├─> Check: User authorized?
  |     ├─> NO: Return OCMS-4001 "Not authorized"
  |     └─> YES: Continue
  |
  ├─> Check: TS code exists and active?
  |     ├─> NO: Return OCMS-4000 "Invalid suspension code"
  |     └─> YES: Continue
  |
  ├─> Check: Source authorized for this TS code?
  |     ├─> NO: Return OCMS-4000 "Source not authorized"
  |     └─> YES: Continue
  |
  ├─> Check: Processing stage in allowed stages?
  |     ├─> NO: Return OCMS-4002 "Invalid processing stage"
  |     └─> YES: Continue
  |
  ├─> Check: Existing suspension?
  |     ├─> PS exists
  |     |     ├─> PS code in [DIP, MID, FOR, RIP, RP2] AND new TS != UNC?
  |     |     |     ├─> YES: ALLOW (TS on top of PS)
  |     |     |     └─> NO: Return OCMS-4002 "Cannot apply TS on PS"
  |     |
  |     ├─> TS exists
  |     |     └─> Revive existing TS with 'TSR', then apply new TS
  |     |
  |     └─> No suspension: ALLOW
  |
  └─> SUCCESS: Apply TS
```

### 4.2 Looping TS Revival Decision Tree

```
START: Auto Revival Cron Runs
  |
  ├─> Check: due_date_of_revival reached?
  |     ├─> NO: Skip
  |     └─> YES: Continue
  |
  ├─> Check: reason_of_suspension is looping code (CLV or HST)?
  |     ├─> NO:
  |     |     └─> Revive normally, set revival_reason = "Auto Revival"
  |     |
  |     └─> YES: Looping TS
  |           |
  |           ├─> Step 1: Revive existing TS
  |           |     └─> UPDATE ocms_suspended_notice
  |           |         SET date_of_revival = CURRENT_TIMESTAMP,
  |           |             revival_reason = "Auto Revival - Looping TS"
  |           |
  |           ├─> Step 2: Create new TS with same code
  |           |     └─> INSERT INTO ocms_suspended_notice
  |           |         (same TS code, new sr_no, new due_date_of_revival)
  |           |
  |           └─> Step 3: Keep notice suspension_type = 'TS'
  |                 (No interruption in suspension)
```

### 4.3 Multiple TS Applied - Which Revival Date?

```
Scenario: Notice has multiple TS applied

Example:
  TS-APP applied on 2025-03-01 (21 days) → Revival: 2025-03-22
  TS-CCE applied on 2025-03-05 (1 day)   → Revival: 2025-03-06
  TS-MS applied on 2025-03-10 (14 days)  → Revival: 2025-03-24

Decision:
  The notice will use the LATEST revival date = 2025-03-24 (TS-MS)

Logic:
  SELECT MAX(due_date_of_revival) FROM ocms_suspended_notice
  WHERE notice_no = ? AND date_of_revival IS NULL
```

---

## 5. Assumptions Log

| ID | Assumption | Reason | Needs Confirmation |
|----|------------|--------|-------------------|
| A-001 | TS codes stored in database table ocms_suspension_reason | Follows OCMS18 pattern of parameter table | No - confirmed from FD |
| A-002 | Looping TS (CLV, HST) auto re-apply runs via auto revival cron | Standard practice for timed operations | No - confirmed from TS Rules |
| A-003 | When multiple TS applied, use MAX(due_date_of_revival) | From FD Section 2.1 Use Case | No - confirmed from FD |
| A-004 | sr_no uses nextval/sequence, not SELECT MAX+1 | Follows OCMS18 SQL pattern | Yes - verify with existing implementation |
| A-005 | HST manual TS can only be applied from HST screen | From TS Rules Excel note | Yes - verify HST screen implementation |

---

## Summary

**Total TS Codes:** 18
- Looping Codes: 2 (CLV, HST)
- Auto Backend Codes: 8 (ACR, CLV, HST, NRO, PAM, PDP, ROV, SYS)
- OCMS Staff Manual: 13 codes
- PLUS Staff Manual: 6 codes (APE, APP, CCE, MS, PRI, RED)

**Validation Layers:**
1. Frontend: 8 rules (FE-001 to FE-023)
2. Backend: 50+ rules (BE-001 to BE-053 + 18 TS code rules)

**Special Cases:**
- CLV: Only RR3/DR3 stages
- HST: Looping + HST screen only
- ROV: Only ROV stage
- PAM: All stages including Warrant of Arrest
- UNC: Cannot be applied on ANY PS

**Next Steps:**
1. Create plan_flowchart.md with flow structure for 5+ sections
2. Create detailed Technical Flow drawio with all validations
3. Generate Technical Document

---

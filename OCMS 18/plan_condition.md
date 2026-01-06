# OCMS 18 - Permanent Suspension Condition Plan

## Document Information

| Item | Details |
|------|---------|
| Version | 1.0 |
| Date | 2026-01-05 |
| Source Document | v1.4_OCMS 18_Functional_Document.md |
| Source Code | ocmsadminapi, ocmsadmincron |
| Author | Technical Documentation |

---

## 1. Frontend Validations

### 1.1 User Permission Check

| Rule ID | Field/Action | Validation | UI Behavior | Error Message |
|---------|--------------|------------|-------------|---------------|
| FE-001 | Apply PS Menu | OIC must have PS permission | Hide "Apply PS" if no permission | N/A (option hidden) |
| FE-002 | User Role | Check role from JWT claims | Control dropdown visibility | N/A |

### 1.2 Notice Selection

| Rule ID | Field/Action | Validation | UI Behavior | Error Message |
|---------|--------------|------------|-------------|---------------|
| FE-003 | Notice Selection | Min 1 notice required | Disable "Select" button | "Please select at least one notice" |
| FE-004 | Notice Selection | Max 10 notices per batch | Block after 10 | "Maximum 10 notices per batch" |

### 1.3 Last Processing Stage (LPS)

| Rule ID | Field/Action | Validation | UI Behavior | Error Message |
|---------|--------------|------------|-------------|---------------|
| FE-005 | LPS Check | Stage must be in allowed list | Show popup with ineligible notices | "Unable to PS the following Notice(s) at this Last Processing Stage" |
| FE-006 | Court Stage | CRT, CRC NOT allowed | Block with error | "Notice is under Court processing" |

**Allowed Stages:** NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3, CPC, CFC

### 1.4 Suspension Detail Form

| Rule ID | Field/Action | Validation | UI Behavior | Error Message |
|---------|--------------|------------|-------------|---------------|
| FE-007 | Suspension Type | Auto-populated "PS" | Read-only | N/A |
| FE-008 | Suspension Code | Required field | Disable Apply until selected | "Please select a Suspension Reason" |
| FE-009 | Suspension Code | Filter by source permission | Show only allowed codes | N/A |
| FE-010 | Suspension Remarks | Max 200 characters | Character counter | "Remarks cannot exceed 200 characters" |

### 1.5 Paid Notice Check

| Rule ID | Field/Action | Validation | UI Behavior | Error Message |
|---------|--------------|------------|-------------|---------------|
| FE-011 | Paid Notice | If paid, only APP/CFA/VST allowed | Block other codes | "Paid notices only allow APP, CFA, or VST" |

### 1.6 Confirmation Dialog

| Rule ID | Field/Action | Validation | UI Behavior | Error Message |
|---------|--------------|------------|-------------|---------------|
| FE-012 | Confirm Action | Show before processing | "Apply PS to [N] notice(s)?" | N/A |

### 1.7 PS Report Form

| Rule ID | Field/Action | Validation | UI Behavior | Error Message |
|---------|--------------|------------|-------------|---------------|
| FE-013 | Date From | Required | Highlight field | "Date From is required" |
| FE-014 | Date To | Required | Highlight field | "Date To is required" |
| FE-015 | Date Range | Max 1 year | Block submit | "Date range cannot exceed 1 year" |
| FE-016 | Date Order | To >= From | Block submit | "Date To must be after Date From" |

---

## 2. Backend Validations

### 2.1 Request Field Validation

| Rule ID | Field | Validation | Error Code | Error Message |
|---------|-------|------------|------------|---------------|
| BE-001 | noticeNo | Required, non-empty array | OCMS-4001 | "Notice number list is empty" |
| BE-002 | noticeNo | Max 10 items | OCMS-4007 | "Batch size exceeds limit of 10 notices" |
| BE-003 | suspensionType | Required | OCMS-4007 | "Suspension Type is missing" |
| BE-004 | suspensionType | Must be "PS" | OCMS-4007 | "Invalid Suspension Type" |
| BE-005 | reasonOfSuspension | Required | OCMS-4007 | "Reason of Suspension is missing" |
| BE-006 | reasonOfSuspension | Valid PS code | OCMS-4007 | "Invalid Suspension Code" |
| BE-007 | officerAuthorisingSuspension | Required | OCMS-4007 | "Officer Authorising Suspension is missing" |
| BE-008 | suspensionSource | Required | OCMS-4000 | "Suspension Source is missing" |
| BE-009 | suspensionRemarks | Max 200 chars | OCMS-4007 | "Suspension remarks exceed 200 characters" |
| BE-010 | dateOfSuspension | Not future date | OCMS-4007 | "Invalid Date of Suspension" |
| BE-011 | caseNo | Required for PLUS | OCMS-4007 | "Case Number is required for PLUS" |

### 2.2 Authentication Validation

| Rule ID | Check | Error Code | Error Message |
|---------|-------|------------|---------------|
| BE-012 | JWT Token present | OCMS-4001 | "Unauthorized Access" |
| BE-013 | JWT Token valid | OCMS-4001 | "Invalid JWT token" |
| BE-014 | API Key valid (external) | OCMS-4001 | "Invalid Auth Token" |

### 2.3 Notice Validation

| Rule ID | Check | Error Code | Error Message |
|---------|-------|------------|---------------|
| BE-015 | Notice exists in DB | OCMS-4001 | "Invalid Notice Number" |

### 2.4 Source Permission Validation

| Rule ID | Source | Allowed PS Codes | Error Code |
|---------|--------|------------------|------------|
| BE-016 | PLUS | APP, CAN, CFA, OTH, VST | OCMS-4000 |
| BE-017 | OCMS Staff | ANS, CAN, CFA, CFP, DBB, DIP, FCT, FOR, FTC, IST, MID, OTH, RIP, RP2, SCT, SLC, SSV, VCT, VST, WWC, WWF, WWP | OCMS-4000 |
| BE-018 | OCMS Backend | ANS, DBB, DIP, FOR, MID, RIP, RP2, FP, PRA, CFP, IST, WWC, WWF, WWP | OCMS-4000 |

**Error Message:** "Source not authorized to use this Suspension Code"

### 2.5 Processing Stage Validation

| Rule ID | Check | Condition | Error Code | Error Message |
|---------|-------|-----------|------------|---------------|
| BE-019 | General LPS | Must be in allowed list | OCMS-4002 | "PS Code cannot be applied due to Last Processing Stage" |
| BE-020 | Court Stage | CRT/CRC NOT allowed | OCMS-4002 | "Notice is under Court processing" |
| BE-021 | ROV Code | Only ROV stage | OCMS-4002 | "ROV code can only be applied at ROV stage" |
| BE-022 | CLV Code | Only RR3/DR3 stages | OCMS-4002 | "CLV code can only be applied at RR3 or DR3 stage" |
| BE-023 | NRO Code | Exclude RR3/DR3 | OCMS-4002 | "NRO code cannot be applied at RR3/DR3 stages" |

### 2.6 Paid Notice Validation

| Rule ID | Check | Error Code | Error Message |
|---------|-------|------------|---------------|
| BE-024 | Paid + non-refund code | OCMS-4003 | "Paid/partially paid notices only allow APP, CFA, or VST" |

**Refund Codes (allowed on paid):** APP, CFA, VST

### 2.7 Overlap Validation (Existing PS)

| Rule ID | Scenario | Action | Error Code |
|---------|----------|--------|------------|
| BE-025 | Same PS code exists | Return success (idempotent) | OCMS-2001 |
| BE-026 | Existing = Exception code | Allow stacking | N/A |
| BE-027 | New = CRS, Existing = Exception | Allow | N/A |
| BE-028 | New = CRS, Existing = Non-exception | Block | OCMS-4008 |
| BE-029 | New = EPR, Existing = Non-exception | Revive old (CSR) first | N/A |

**Exception Codes:** DIP, FOR, MID, RIP, RP2
**CRS Codes:** FP, PRA

**Error Message (OCMS-4008):** "Cannot apply PS-FP/PRA on existing PS"

### 2.8 Revival Validation

| Rule ID | Check | Error Code | Error Message |
|---------|-------|------------|---------------|
| BE-030 | Active PS exists | OCMS-4005 | "No active PS suspension found" |
| BE-031 | Notice is PS suspended | OCMS-4005 | "Notice does not have active PS" |
| BE-032 | revivalReason required | OCMS-4007 | "Revival Reason is missing" |
| BE-033 | officer required | OCMS-4007 | "Officer Authorising Revival is missing" |

---

## 3. Business Rules

### 3.1 Global PS Rules

| Rule ID | Rule Name | Description |
|---------|-----------|-------------|
| BIZ-001 | Display Most Recent | Staff Portal displays most recently applied active PS |
| BIZ-002 | Revival Date NULL | PS never has due_date_of_revival (permanent) |
| BIZ-003 | Revive Before New EPR | When applying new EPR PS on existing PS, revive old with CSR first |
| BIZ-004 | Exception No Revival | DIP, FOR, MID, RIP, RP2 allow stacking without revival |
| BIZ-005 | CRS Backend Only | FP, PRA only applied by OCMS Backend during payment |
| BIZ-006 | CRS Exception Only | FP/PRA only allowed on exception PS codes |
| BIZ-007 | Refund Trigger | APP, CFA, VST on paid notices trigger refund |
| BIZ-008 | TS Not on PS | TS cannot be applied to notice with PS (except exception codes) |
| BIZ-009 | No Furnish | Notices with PS are not furnishable via eService |
| BIZ-010 | No Hirer/Driver | DIP, MID, RIP, RP2 need revival before adding hirer/driver |
| BIZ-011 | Appeal Allowed | All notices can be linked to Appeal regardless of PS |
| BIZ-012 | Payment Exception | DIP, MID, FOR, RIP, RP2 notices are payable via eService/AXS |

### 3.2 Source Permission Matrix

| Source | Allowed PS Codes |
|--------|------------------|
| PLUS | APP, CAN, CFA, OTH, VST |
| OCMS Staff | ANS, CAN, CFA, CFP, DBB, DIP, FCT, FOR, FTC, IST, MID, OTH, RIP, RP2, SCT, SLC, SSV, VCT, VST, WWC, WWF, WWP |
| OCMS Backend | ANS, DBB, DIP, FOR, MID, RIP, RP2, FP, PRA, CFP, IST, WWC, WWF, WWP |

### 3.3 Auto PS Trigger Scenarios

| Scenario | PS Code | Trigger Condition |
|----------|---------|-------------------|
| Advisory Notice | ANS | Notice type = Advisory Notice |
| Double Booking | DBB | Duplicate notice detected |
| Diplomatic Vehicle | DIP | Diplomatic vehicle at RD2/DN2 end |
| Foreign Vehicle | FOR | Foreign-registered vehicle |
| MINDEF Vehicle | MID | MINDEF vehicle at RD2/DN2 end |
| Fully Paid | FP | Notice fully paid |
| Partial Payment | PRA | Paid with reduced amount |
| Deceased On/After | RIP | Owner deceased on/after offence date |
| Deceased Before | RP2 | Owner deceased before offence date |

### 3.4 Special Code Categories

| Category | Codes | Special Behavior |
|----------|-------|------------------|
| Exception Codes | DIP, FOR, MID, RIP, RP2 | Allow TS, allow CRS, allow stacking |
| Refund Codes | APP, CFA, VST | Trigger refund on paid notices |
| CRS Codes | FP, PRA | Backend only, payment-triggered |
| Post-MVP | WWC, WWF, WWP | Court-related, future implementation |

### 3.5 Revival Rules

| Rule ID | Rule | Description |
|---------|------|-------------|
| BIZ-013 | CSR Auto Revival | System uses CSR when auto-reviving for new PS |
| BIZ-014 | Latest After Revival | After revival, apply PS with latest date to VON |
| BIZ-015 | Clear on No PS | If no remaining PS, clear VON suspension fields |
| BIZ-016 | Refund on FP Revival | Reviving PS-FP triggers refund identification |

---

## 4. Decision Trees

### 4.1 Apply PS Flow

```
[1] Validate Request Fields
    ├─ Invalid? → Return error
    └─ Valid? → Continue

[2] Authenticate (JWT)
    ├─ Invalid? → OCMS-4001
    └─ Valid? → Continue

[3] Query Notice
    ├─ Not found? → OCMS-4001
    └─ Found? → Continue

[4] Check Source Permission
    ├─ Not allowed? → OCMS-4000
    └─ Allowed? → Continue

[5] Check Processing Stage
    ├─ Court stage? → OCMS-4002
    ├─ Not in allowed list? → OCMS-4002
    └─ Allowed? → Continue

[6] Check Paid Notice
    ├─ Paid + code NOT in [APP,CFA,VST]? → OCMS-4003
    └─ OK? → Continue

[7] Check Existing PS
    ├─ Same code? → OCMS-2001 (idempotent)
    ├─ Exception code? → Allow stacking
    ├─ CRS on non-exception? → OCMS-4008
    └─ EPR on non-exception? → Revive old, apply new

[8] Update Database
    └─ VON + eVON + suspended_notice

[9] Return OCMS-2000 Success
```

### 4.2 Revive PS Flow

```
[1] Validate Request
    └─ Check required fields

[2] Query Notice
    └─ Must exist

[3] Check Active PS
    ├─ No active PS? → OCMS-4005
    └─ Has active PS? → Continue

[4] Check Refund (FP/PRA)
    └─ Log refund identification

[5] Update suspended_notice
    └─ Set revival fields

[6] Check Remaining PS
    ├─ Other PS exist? → Apply latest to VON
    └─ No other PS? → Clear VON fields

[7] Return Success
```

---

## 5. Assumptions Log

| ID | Assumption | Basis | Status |
|----|------------|-------|--------|
| ASM-001 | Frontend handles OIC permission via JWT | No backend user permission check | Confirmed |
| ASM-002 | Batch limit 10 notices | FD mentions "batches of 10" | To implement |
| ASM-003 | Remarks max 200 chars | FD Section 3 | To implement |
| ASM-004 | Date not future validation | FD error codes | To implement |
| ASM-005 | PLUS codes include CFA, OTH | FD Section 5.3 | Confirmed |
| ASM-006 | VST in refund codes | Code PS_REFUND_CODES | Confirmed |

---

## 6. Change Log

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-05 | Technical Documentation | Initial version |

---

*End of Document*

# Condition Plan: Military Vehicle Notice Processing

## Overview

| Attribute | Value |
| --- | --- |
| Feature Name | Notice Processing Flow for Military Vehicles |
| Version | v1.0 |
| Author | Claude |
| Created Date | 15/01/2026 |
| Last Updated | 15/01/2026 |
| Status | Draft |
| FD Reference | OCMS 14 - Section 5 |
| TD Reference | OCMS 14 - Section 4 |

---

## 1. Purpose

This document outlines the business rules, validations, and conditions for Military Vehicle Notice Processing in OCMS. It covers frontend validations, backend validations, decision logic, and suspension rules.

---

## 2. Vehicle Detection Rules

### 2.1 Military Vehicle Identification

| Rule ID | Rule Name | Condition | Result |
| --- | --- | --- | --- |
| MID-DET-001 | MID Prefix Check | Vehicle number starts with "MID" | Type = 'I' |
| MID-DET-002 | MINDEF Prefix Check | Vehicle number starts with "MINDEF" | Type = 'I' |
| MID-DET-003 | MID Suffix Check | Vehicle number ends with "MID" | Type = 'I' |
| MID-DET-004 | MINDEF Suffix Check | Vehicle number ends with "MINDEF" | Type = 'I' |

### 2.2 Detection Priority

| Priority | Check | If Match |
| --- | --- | --- |
| 1 | UPL Vehicle (Offence Type U) | Return 'X' |
| 2 | Diplomatic Vehicle (S+CC/CD/TC/TE) | Return 'D' |
| 3 | Military Vehicle (MID/MINDEF) | Return 'I' |
| 4 | VIP Vehicle (CAS/FOMS check) | Return 'V' |
| 5 | LTA Checksum Valid | Return 'S' (Local) |
| 6 | Default | Return 'F' (Foreign) |

---

## 3. Notice Creation Conditions

### 3.1 Pre-conditions for Notice Creation

| Rule ID | Condition | Validation | Action if Fail |
| --- | --- | --- | --- |
| MID-CRE-001 | Data Validation | All mandatory fields present | Reject notice |
| MID-CRE-002 | Duplicate Notice Number | Notice number not exists | Reject notice |
| MID-CRE-003 | Offence Type Valid | Type in (O, E, U) | Reject notice |
| MID-CRE-004 | Vehicle Type Detected | Type = 'I' | Route to standard flow |

### 3.2 Notice Creation Rules

| Rule ID | Rule | Description |
| --- | --- | --- |
| MID-CRE-010 | Hardcoded Owner | Always use MINDEF as owner |
| MID-CRE-011 | Hardcoded Address | Always use Kranji Camp 3 address |
| MID-CRE-012 | Initial Stage | Set last_processing_stage = 'NPA' |
| MID-CRE-013 | Next Stage | Set next_processing_stage = 'RD1' |
| MID-CRE-014 | Processing Date | Set next_processing_date = current date |
| MID-CRE-015 | Payment Allowed | Set payment_acceptance_allowed = 'Y' |

---

## 4. Double Booking (DBB) Rules

### 4.1 Duplicate Detection Criteria

| Rule ID | Field | Comparison Logic |
| --- | --- | --- |
| MID-DBB-001 | vehicle_no | Exact match |
| MID-DBB-002 | notice_date_and_time | Exact match (date + time) |
| MID-DBB-003 | computer_rule_code | Exact match |
| MID-DBB-004 | parking_lot_no | Exact match (NULL = NULL) |
| MID-DBB-005 | pp_code | Exact match |

### 4.2 DBB Action

| Condition | Action |
| --- | --- |
| All 5 criteria match | Apply PS-DBB suspension |
| Any criteria different | No duplicate, continue processing |

---

## 5. Advisory Notice (AN) Rules

### 5.1 AN Qualification Criteria

| Rule ID | Condition | Required Value |
| --- | --- | --- |
| MID-AN-001 | Offence Type | Must be Type O (Parking) |
| MID-AN-002 | Vehicle Type | Must be 'I' (Military) |
| MID-AN-003 | Current Stage | Must be at NPA |
| MID-AN-004 | Suspension Status | No active suspension |
| MID-AN-005 | AN Eligibility | Based on OCMS 10 rules |

### 5.2 AN Processing Rules

| Rule ID | Rule | Description |
| --- | --- | --- |
| MID-AN-010 | Set AN Flag | If qualifies, set an_flag = 'Y' |
| MID-AN-011 | Generate Letter | Create AN Letter PDF |
| MID-AN-012 | Send to Vendor | Drop PDF to TOPPAN via SFTP |
| MID-AN-013 | Apply Suspension | Suspend with PS-ANS |
| MID-AN-014 | Stop Processing | Notice exits flow after PS-ANS |

---

## 6. Stage Processing Rules

### 6.1 Processing Stage Transitions

| Current Stage | Next Stage | Trigger | Letter Generated |
| --- | --- | --- | --- |
| NPA | RD1 | End of day CRON | RD1 Reminder |
| RD1 | RD2 | End of RD1 period | RD2 Reminder |
| RD2 | PS-MID | End of RD2 period | None (Suspended) |
| DN1 | DN2 | End of DN1 period | DN2 Letter |
| DN2 | PS-MID | End of DN2 period | None (Suspended) |

### 6.2 Bypass Rules for Military Vehicles

| Rule ID | Standard Step | Military Vehicle Action |
| --- | --- | --- |
| MID-BYP-001 | LTA VRLS Check | BYPASS - Not in LTA registry |
| MID-BYP-002 | MHA Local ID Check | BYPASS - Use hardcoded MINDEF |
| MID-BYP-003 | DataHive FIN Check | BYPASS - Use hardcoded MINDEF |
| MID-BYP-004 | DataHive Company Check | BYPASS - Use hardcoded MINDEF |
| MID-BYP-005 | ENA Stage | BYPASS - No eNotification sent |
| MID-BYP-006 | RR3/DR3 Stage | BYPASS - Suspend at RD2/DN2 |
| MID-BYP-007 | Court Stage | BYPASS - Never escalate to court |

---

## 7. Furnish Driver/Hirer Rules

### 7.1 Furnish Eligibility

| Rule ID | Condition | Furnish Allowed |
| --- | --- | --- |
| MID-FUR-001 | Stage = NPA | No |
| MID-FUR-002 | Stage = ROV | No |
| MID-FUR-003 | Stage = RD1 | Yes |
| MID-FUR-004 | Stage = RD2 | Yes |
| MID-FUR-005 | Suspended with PS-MID | No |
| MID-FUR-006 | Suspended with PS-ANS | No |

### 7.2 Furnish Validation Rules

| Rule ID | Validation | Action if Fail |
| --- | --- | --- |
| MID-FUR-010 | Driver/Hirer ID valid format | Reject application |
| MID-FUR-011 | Driver/Hirer not deceased | Auto-reject |
| MID-FUR-012 | Driver/Hirer address valid | Manual OIC review |
| MID-FUR-013 | Same person as owner | Reject (cannot furnish self) |

### 7.3 Post-Furnish Processing

| Furnished Party | Next Stage | Processing Path |
| --- | --- | --- |
| Driver | DN1 | DN1 → DN2 → PS-MID |
| Hirer | RD1 | RD1 → RD2 → PS-MID |

### 7.4 MHA/DataHive Check After Furnish

| Rule ID | Rule | Description |
| --- | --- | --- |
| MID-FUR-020 | MHA Check | Validate Driver/Hirer NRIC/FIN with MHA |
| MID-FUR-021 | DataHive Check | Get latest particulars from DataHive |
| MID-FUR-022 | Check Timing | Before RD2/DN2 stage transition |

---

## 8. Suspension Rules

### 8.1 PS-MID Suspension Conditions

| Rule ID | Condition | Required Value |
| --- | --- | --- |
| MID-SUS-001 | Vehicle Type | Must be 'I' (Military) |
| MID-SUS-002 | Current Stage | RD2 or DN2 |
| MID-SUS-003 | Notice Status | Outstanding (unpaid) |
| MID-SUS-004 | Existing PS | No CRS reason of suspension |

### 8.2 PS-MID Application Stages

| Rule ID | Stage | PS-MID Allowed |
| --- | --- | --- |
| MID-SUS-010 | NPA | Yes (Manual by OIC) |
| MID-SUS-011 | RD1 | Yes (Manual by OIC) |
| MID-SUS-012 | RD2 | Yes (Auto at end of stage) |
| MID-SUS-013 | DN1 | Yes (Manual by OIC) |
| MID-SUS-014 | DN2 | Yes (Auto at end of stage) |

### 8.3 PS-MID Source Permissions

| Source | Can Apply PS-MID |
| --- | --- |
| OCMS Staff Portal | Yes |
| OCMS Backend (CRON) | Yes |
| PLUS Staff Portal | No |

### 8.4 PS-MID Stacking Rules

| Rule ID | Rule | Description |
| --- | --- | --- |
| MID-SUS-020 | TS on PS-MID | Allowed - TS can be applied on top of PS-MID |
| MID-SUS-021 | PS on PS-MID | Not Allowed - Must revive PS-MID first |
| MID-SUS-022 | PS-FP/PS-PRA | Allowed - Can apply without revival |

### 8.5 Post-Suspension Rules

| Rule ID | Rule | Value |
| --- | --- | --- |
| MID-SUS-030 | Payment Allowed | Yes (via eService/AXS) |
| MID-SUS-031 | Furnish Allowed | No |
| MID-SUS-032 | Revival Date | Always NULL (permanent) |
| MID-SUS-033 | Court Escalation | Blocked |

---

## 9. Payment Rules

### 9.1 Payment Eligibility by Stage

| Stage | Payment Allowed |
| --- | --- |
| NPA | Yes |
| ROV | Yes |
| RD1 | Yes |
| RD2 | Yes |
| DN1 | Yes |
| DN2 | Yes |
| PS-MID Suspended | Yes |

### 9.2 Payment Processing Rules

| Rule ID | Rule | Description |
| --- | --- | --- |
| MID-PAY-001 | Successful Payment | Apply PS-FP suspension |
| MID-PAY-002 | Processing Stops | Notice exits flow after PS-FP |
| MID-PAY-003 | Refund on PS | If notice paid then PS applied, trigger refund logic |

---

## 10. eNotification Rules

### 10.1 eNotification Eligibility

| Stage | eNotification Allowed |
| --- | --- |
| NPA | No |
| ROV | No |
| RD1 | No |
| RD2 | No |
| PS-MID Suspended | No |

**Note:** Military Vehicle Notices bypass the ENA stage entirely. No eNotification is sent at any stage.

---

## 11. Exit Conditions

### 11.1 Notice Exit Rules

| Rule ID | Exit Condition | Description |
| --- | --- | --- |
| MID-EXIT-001 | Payment Made | Notice suspended with PS-FP |
| MID-EXIT-002 | Unpaid X Years | Send for archival after review |
| MID-EXIT-003 | Other PS Applied | e.g., PS-APP, PS-CFA cancellation |
| MID-EXIT-004 | PS-ANS Applied | Advisory Notice suspended |
| MID-EXIT-005 | PS-DBB Applied | Double booking detected |

---

## 12. Daily Re-check Rules (CRON)

### 12.1 DipMidForRecheckJob Conditions

| Rule ID | Condition | Description |
| --- | --- | --- |
| MID-RCK-001 | Execution Time | 11:59 PM daily |
| MID-RCK-002 | Target Stages | RD2 or DN2 |
| MID-RCK-003 | Target Types | 'D', 'I', 'F' |
| MID-RCK-004 | No Active PS | suspension_type IS NULL |

### 12.2 Re-check Action

| Condition | Action |
| --- | --- |
| Type 'I' at RD2/DN2 without PS | Re-apply PS-MID |
| Type 'D' at RD2/DN2 without PS | Re-apply PS-DIP |
| Type 'F' at RD2/DN2 without PS | Re-apply PS-FOR |

---

## 13. Decision Trees

### 13.1 Military Vehicle Notice Processing Flow

```
START
  │
  ▼
[Offence Data Received]
  │
  ▼
[Validate Data] ──No──► [Reject Notice]
  │ Yes
  ▼
[Check Duplicate Notice Number] ──Duplicate──► [Reject Notice]
  │ No Duplicate
  ▼
[Detect Vehicle Registration Type]
  │
  ▼
[Type = 'I' (Military)?] ──No──► [Route to Standard Flow]
  │ Yes
  ▼
[Create Notice at NPA with MINDEF details]
  │
  ▼
[Check Double Booking] ──DBB Found──► [Apply PS-DBB] ──► END
  │ No DBB
  ▼
[Offence Type?]
  │
  ├── Type O ──► [AN Qualification Check]
  │               │
  │               ├── Qualifies ──► [AN Sub-flow] ──► [PS-ANS] ──► END
  │               │
  │               └── Not Qualify ──► [Prepare RD1]
  │
  ├── Type E ──► [Prepare RD1]
  │
  └── Type U ──► [UPL Workflow] (KIV)
```

### 13.2 RD1/RD2 Processing Decision Tree

```
[Notice at RD1]
  │
  ▼
[Payment Made?] ──Yes──► [Apply PS-FP] ──► END
  │ No
  ▼
[Furnish Submitted?] ──Yes──► [Validate Driver/Hirer]
  │ No                          │
  ▼                             ├── Approved ──► [Route to DN1/RD1]
[End of RD1 Stage]              │
  │                             └── Rejected ──► [Continue RD1]
  ▼
[Prepare for RD2]
  │
  ▼
[Notice at RD2]
  │
  ▼
[Payment Made?] ──Yes──► [Apply PS-FP] ──► END
  │ No
  ▼
[Furnish Submitted?] ──Yes──► [Validate Driver/Hirer]
  │ No                          │
  ▼                             └── [Process similar to RD1]
[End of RD2 Stage]
  │
  ▼
[Outstanding?] ──Yes──► [Apply PS-MID] ──► END
  │ No
  ▼
END (Paid)
```

### 13.3 Furnish Driver/Hirer Decision Tree

```
[Furnish Application Received]
  │
  ▼
[Sync to Intranet]
  │
  ▼
[Auto-Validation]
  │
  ├── Pass ──► [Auto Approve] ──► [Create Driver/Hirer Record]
  │                                │
  │                                ├── Driver ──► [Next Stage = DN1]
  │                                │
  │                                └── Hirer ──► [Next Stage = RD1]
  │
  └── Fail ──► [OIC Manual Review]
               │
               ├── Approve ──► [Create Driver/Hirer Record]
               │
               └── Reject ──► [Remove Pending Status]
                              │
                              ▼
                              [Allow Re-furnish]
```

---

## 14. Allowed Functions Summary

### 14.1 Functions by Notice Condition

| Notice Condition | eNotification | Payment | Furnish Driver |
| --- | --- | --- | --- |
| NPA | No | Yes | No |
| ROV | No | Yes | No |
| RD1 | No | Yes | Yes |
| RD2 | No | Yes | Yes |
| DN1 | No | Yes | No |
| DN2 | No | Yes | No |
| PS-MID Suspended | No | Yes | No |
| PS-ANS Suspended | No | No | No |
| PS-FP Suspended | No | No | No |

---

## 15. Error Handling

### 15.1 Error Scenarios

| Error Code | Scenario | Action |
| --- | --- | --- |
| MID-ERR-001 | Invalid vehicle number format | Reject notice creation |
| MID-ERR-002 | Missing mandatory fields | Reject notice creation |
| MID-ERR-003 | Duplicate notice number | Reject notice creation |
| MID-ERR-004 | SFTP upload failed | Retry, send error email |
| MID-ERR-005 | Furnish validation failed | Route to OIC review |
| MID-ERR-006 | PS-MID application failed | Log error, manual review |

---

## 16. Assumptions Log

| ID | Assumption | Basis | Impact |
| --- | --- | --- | --- |
| ASM-001 | MINDEF address is hardcoded in backend code | Backend code analysis | No LTA/MHA check needed |
| ASM-002 | PS-MID is applied automatically at end of RD2/DN2 | FD Section 5.5 | CRON job handles this |
| ASM-003 | MHA/DataHive check only for furnished Driver/Hirer | FD Section 5.2.5 | Not for original MINDEF owner |
| ASM-004 | Type U flow is KIV (pending requirements) | FD Section 5.2.3 | Not implemented yet |
| ASM-005 | AN qualification based on OCMS 10 rules | FD Section 5.2.4 | External dependency |

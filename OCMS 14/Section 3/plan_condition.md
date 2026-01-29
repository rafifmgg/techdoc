# Condition Plan: Notice Processing Flow for Deceased Offenders

## Overview

| Attribute | Value |
| --- | --- |
| Feature Name | Notice Processing Flow for Deceased Offenders |
| Version | v1.1 |
| Author | Claude |
| Created Date | 15/01/2026 |
| Last Updated | 27/01/2026 |
| Status | Draft |
| FD Reference | OCMS 14 - Section 4 |
| TD Reference | OCMS 14 - Section 3 |

---

## 1. Reference Documents

| Document Type | Reference | Description |
| --- | --- | --- |
| Functional Document | OCMS 14 - Section 4 | Notice Processing Flow for Deceased Offenders |
| Technical Document | OCMS 14 - Section 3 | Technical implementation |
| Backend Code | PermanentSuspensionHelper.java | PS-RIP/RP2 suspension logic |
| Backend Code | MhaPsRipSuspensionFlowService.java | RIP suspension test flow |
| Backend Code | MhaPsRp2SuspensionFlowService.java | RP2 suspension test flow |
| Related FD | OCMS 8 - Section 3.2.2 | MHA output file processing |
| Related FD | OCMS 8 - Section 7.5 | DataHive deceased FIN processing |

---

## 2. Business Conditions

### 2.1 Detecting Deceased Offenders

**Description:** Determines if an offender is deceased based on data from MHA (NRIC) or DataHive (FIN).

**Reference:** FD Section 4.3 - Detecting Deceased Offenders

#### Condition Matrix

| Condition ID | Condition Name | Input Fields | Logic | Output/Action |
| --- | --- | --- | --- | --- |
| C001 | MHA Life Status Check | life_status (from MHA) | life_status = 'D' | Offender is deceased |
| C002 | DataHive FIN Check | FIN, D90 dataset | FIN found in D90 dataset | Offender is deceased, life_status = 'D' |
| C003 | Offender Alive | life_status | life_status = 'A' OR not in D90 | Continue normal processing |

---

#### Condition Details

**C001: MHA Life Status Check (NRIC Holders)**

| Attribute | Value |
| --- | --- |
| Description | Check if NRIC holder is deceased based on MHA response |
| Trigger | MHA returns owner/hirer/driver particulars |
| Input | life_status field from MHA response |
| Logic | life_status equals 'D' |
| Output | Mark offender as deceased, proceed to suspension logic |
| Else | Continue normal processing |

```
IF MHA.life_status = 'D'
THEN Mark offender as deceased
     Proceed to Suspension Code Determination (C004/C005)
ELSE Continue normal processing
```

---

**C002: DataHive FIN Check (FIN Holders)**

| Attribute | Value |
| --- | --- |
| Description | Check if FIN holder is in DataHive's "Dead Foreign Pass Holders" dataset |
| Trigger | FIN lookup during notice processing |
| Input | FIN number |
| Logic | FIN exists in V_DH_MHA_FINDEATH (D90 dataset) |
| Output | Set life_status = 'D', proceed to suspension logic |
| Else | Set life_status = 'A', continue normal processing |

```
IF FIN EXISTS IN V_DH_MHA_FINDEATH
THEN Set life_status = 'D'
     Get date_of_death from D90
     Proceed to Suspension Code Determination (C004/C005)
ELSE Set life_status = 'A'
     Continue normal processing
```

---

**C003: Offender Alive**

| Attribute | Value |
| --- | --- |
| Description | Default case when offender is not deceased |
| Trigger | MHA returns life_status = 'A' OR FIN not in D90 |
| Input | life_status |
| Logic | life_status = 'A' |
| Output | Continue with standard notice processing flow |

```
IF life_status = 'A'
THEN Continue standard notice processing
     No RIP/RP2 suspension applied
```

---

### 2.2 Suspension Code Determination

**Description:** Determines whether to apply PS-RIP or PS-RP2 based on the relationship between date of death and offence date.

**Reference:** FD Section 4.4 - Suspending Notices with Deceased Offenders

#### Condition Matrix

| Condition ID | Condition Name | Input Fields | Logic | Output/Action |
| --- | --- | --- | --- | --- |
| C004 | Apply PS-RIP | date_of_death, offence_date | date_of_death >= offence_date | Apply PS-RIP suspension |
| C005 | Apply PS-RP2 | date_of_death, offence_date | date_of_death < offence_date | Apply PS-RP2 suspension |

---

#### Condition Details

**C004: Apply PS-RIP (Death On or After Offence Date)**

| Attribute | Value |
| --- | --- |
| Description | Apply PS-RIP when offender died on or after the offence date |
| Trigger | Deceased offender detected (C001 or C002 is true) |
| Input | date_of_death, notice_date_and_time (offence_date) |
| Logic | date_of_death >= offence_date |
| Output | Apply permanent suspension with reason code 'RIP' |
| Meaning | Offender was alive at time of offence but deceased now |

```
IF date_of_death >= offence_date
THEN Apply PS-RIP
     suspension_type = 'PS'
     epr_reason_of_suspension = 'RIP'
     Create ocms_suspended_notice record with reason = 'RIP'
```

**PS-RIP Full Name:** Motorist Deceased On or After Offence Date

---

**C005: Apply PS-RP2 (Death Before Offence Date)**

| Attribute | Value |
| --- | --- |
| Description | Apply PS-RP2 when offender died before the offence date |
| Trigger | Deceased offender detected (C001 or C002 is true) |
| Input | date_of_death, notice_date_and_time (offence_date) |
| Logic | date_of_death < offence_date |
| Output | Apply permanent suspension with reason code 'RP2' |
| Meaning | Offender was already deceased at time of offence (identity may have been wrongly used) |

```
IF date_of_death < offence_date
THEN Apply PS-RP2
     suspension_type = 'PS'
     epr_reason_of_suspension = 'RP2'
     Create ocms_suspended_notice record with reason = 'RP2'
```

**PS-RP2 Full Name:** Motorist Deceased Before Offence Date

---

### 2.3 RIP Hirer/Driver Furnished Report Eligibility

**Description:** Determines if a notice should be included in the daily "RIP Hirer/Driver Furnished" report.

**Reference:** FD Section 4.7 - RIP Hirer/Driver Furnished Report

#### Condition Matrix

| Condition ID | Condition Name | Input Fields | Logic | Output/Action |
| --- | --- | --- | --- | --- |
| C006 | RP2 Report Eligible | suspension, offender_type, life_status | See below | Include in report |
| C007 | RP2 Report Not Eligible | suspension, offender_type, life_status | Does not meet C006 | Exclude from report |

---

**C006: RP2 Report Eligibility**

| Attribute | Value |
| --- | --- |
| Description | Check if notice meets criteria for RIP Hirer/Driver Furnished report |
| Trigger | Daily cron job for report generation |
| Logic | ALL conditions must be true |

**All Conditions Must Be True:**

| # | Condition | Table | Field | Value |
| --- | --- | --- | --- | --- |
| 1 | PS-RP2 applied today | ocms_suspended_notice | date_of_suspension | = CURRENT_DATE |
| 2 | Suspension type is PS | ocms_suspended_notice | suspension_type | = 'PS' |
| 3 | Reason is RP2 | ocms_suspended_notice | reason_of_suspension | = 'RP2' |
| 4 | Not yet revived | ocms_suspended_notice | date_of_revival | IS NULL |
| 5 | Current offender is Hirer or Driver | ocms_offence_notice_owner_driver | owner_driver_indicator | IN ('H', 'D') |
| 6 | Is current offender | ocms_offence_notice_owner_driver | offender_indicator | = 'Y' |
| 7 | Offender is deceased | ocms_offence_notice_owner_driver | life_status | = 'D' |

```
IF ocms_suspended_notice.suspension_type = 'PS'
   AND ocms_suspended_notice.reason_of_suspension = 'RP2'
   AND CAST(ocms_suspended_notice.date_of_suspension AS DATE) = CURRENT_DATE
   AND ocms_suspended_notice.date_of_revival IS NULL
   AND ocms_offence_notice_owner_driver.owner_driver_indicator IN ('H', 'D')
   AND ocms_offence_notice_owner_driver.offender_indicator = 'Y'
   AND ocms_offence_notice_owner_driver.life_status = 'D'
THEN Include notice in "RIP Hirer/Driver Furnished" report
ELSE Exclude from report
```

---

### 2.4 PS-RIP/RP2 Suspension Rules

**Description:** Business rules governing PS-RIP and PS-RP2 suspensions.

**Reference:** FD Section 4.4 - Suspending Notices with Deceased Offenders

#### Suspension Rules Matrix

| Rule ID | Rule Name | Condition | Allowed | Description |
| --- | --- | --- | --- | --- |
| R001 | Allowed Stages | Current stage | NPA, eNA, ROV, RD1, RD2, RR3, DN1, DN2, DR3, CPC | Stages where RIP/RP2 can be applied |
| R002 | OCMS Staff Apply | Source = OCMS Staff | Yes | Staff can manually apply PS-RIP/RP2 |
| R003 | PLUS Staff Apply | Source = PLUS | No | PLUS cannot apply PS-RIP/RP2 |
| R004 | Backend Auto Apply | Source = CRON | Yes | System can auto-apply PS-RIP/RP2 |
| R005 | Allow TS on Top | Existing PS = RIP/RP2 | Yes | TS can be applied on notice with PS-RIP/RP2 |
| R006 | Allow PS on Top | Existing PS = RIP/RP2 | Yes | Other PS can stack on PS-RIP/RP2 |
| R007 | CRS without Revival | Existing PS = RIP/RP2 | Yes | PS-FP/PRA can apply without PS revival |
| R008 | Non-CRS with Revival | Existing PS = RIP/RP2, New PS = non-CRS | Yes | PS-APP etc. requires PS revival first |

---

#### Rule Details

**R001: Allowed Processing Stages**

| Stage Code | Stage Name | RIP/RP2 Allowed |
| --- | --- | --- |
| NPA | Notice Pending Acknowledgement | Yes |
| eNA | Electronic Notice Acknowledgement | Yes |
| ROV | Re-Offer of Voluntary | Yes |
| RD1 | Reminder 1 | Yes |
| RD2 | Reminder 2 | Yes |
| RR3 | Re-Reminder 3 | Yes |
| DN1 | Demand Note 1 | Yes |
| DN2 | Demand Note 2 | Yes |
| DR3 | Demand Reminder 3 | Yes |
| CPC | Civil Prosecution Consideration | Yes |

---

**R005-R008: Exception Code Behavior**

PS-RIP and PS-RP2 are part of the **5 Exception Codes** (DIP, FOR, MID, RIP, RP2) which have special stacking rules:

```
5 Exception Codes Allow:
1. TS suspension can be applied on top of existing PS (R005)
2. Other PS can stack without revival (R006)
3. CRS PS (FP/PRA) can apply WITHOUT PS revival (R007)
4. Non-CRS PS (APP, etc.) requires PS revival first (R008)
```

---

### 2.5 PS Revival and Redirect Conditions

**Description:** Conditions for OIC to revive PS-RIP/RP2 and redirect notice.

**Reference:** FD Section 4.8 - Redirecting PS-RIP & PS-RP2 Notices

#### Condition Matrix

| Condition ID | Condition Name | Input Fields | Logic | Output/Action |
| --- | --- | --- | --- | --- |
| C008 | Can Revive PS | user_role, suspension | Authorized OIC + Active PS | Allow revival |
| C009 | Redirect to Owner | redirect_target | RIP wrongly furnished | Redirect to Owner |
| C010 | Redirect to Hirer/Driver | redirect_target | Next-of-Kin furnished | Redirect to new Hirer/Driver |

---

**C008: PS Revival Authorization**

| Attribute | Value |
| --- | --- |
| Description | Check if user can revive PS-RIP/RP2 suspension |
| Trigger | OIC initiates revival from Staff Portal |
| Input | user_role, active_suspension |
| Logic | User is authorized OIC AND notice has active PS-RIP/RP2 |
| Output | Allow PS revival and update database |

```
IF user IS authorized OIC
   AND notice.suspension_type = 'PS'
   AND notice.epr_reason_of_suspension IN ('RIP', 'RP2')
   AND ocms_suspended_notice.due_date_of_revival IS NULL
THEN Allow PS revival
     Set due_date_of_revival = NOW()
     Proceed to redirect
```

---

**C009: Redirect to Owner (Wrongly Furnished RIP)**

| Attribute | Value |
| --- | --- |
| Description | Redirect notice to Owner when RIP ID was wrongly furnished |
| Trigger | OIC selects to redirect to Owner |
| Scenario | RIP ID was incorrectly provided; actual offender is the Owner |
| Output | Update offender_indicator, redirect notice stage |

---

**C010: Redirect to Hirer/Driver (Next-of-Kin Furnished)**

| Attribute | Value |
| --- | --- |
| Description | Redirect notice when deceased offender's Next-of-Kin furnishes new Hirer/Driver |
| Trigger | OIC updates offender details with NOK-provided information |
| Scenario | Deceased's family provides actual driver/hirer information |
| Output | Update offender details, redirect notice to new offender |

---

## 3. Decision Tree

### 3.1 Deceased Offender Detection and Suspension Flow

```
START
  │
  ├─► Receive life_status from MHA/DataHive
  │     │
  │     ├─ life_status = 'D'? ──────────────────────────────────────────┐
  │     │     │                                                         │
  │     │     ├─ YES ─► Update offender particulars                    │
  │     │     │           │                                             │
  │     │     │           ├─► date_of_death >= offence_date?           │
  │     │     │           │     │                                       │
  │     │     │           │     ├─ YES ─► Apply PS-RIP ─────────────────┤
  │     │     │           │     │                                       │
  │     │     │           │     └─ NO ──► Apply PS-RP2 ─────────────────┤
  │     │     │           │                   │                         │
  │     │     │           │                   ├─► Offender is H or D?  │
  │     │     │           │                   │     │                   │
  │     │     │           │                   │     ├─ YES ─► Add to   │
  │     │     │           │                   │     │         RP2 Report│
  │     │     │           │                   │     │                   │
  │     │     │           │                   │     └─ NO ──► Skip     │
  │     │     │           │                   │               report   │
  │     │     │           │                   │                         │
  │     │     │           └───────────────────┴─────────────────────────┤
  │     │     │                                                         │
  │     │     └─ NO ──► Continue normal processing ─────────────────────┤
  │     │                                                               │
  │     └───────────────────────────────────────────────────────────────┘
  │
END
```

### 3.2 Decision Table - Suspension Code

| life_status | date_of_death >= offence_date | Suspension Code | Report Eligible |
| --- | --- | --- | --- |
| A | N/A | None | No |
| D | TRUE | PS-RIP | No |
| D | FALSE | PS-RP2 | If H/D offender |

### 3.3 Decision Table - RP2 Report Eligibility

| PS-RP2 Today | Active (not revived) | Offender Type | life_status | Report Include |
| --- | --- | --- | --- | --- |
| Yes | Yes | H or D | D | Yes |
| Yes | Yes | O | D | No |
| Yes | No | H or D | D | No |
| No | Yes | H or D | D | No |

---

## 4. Validation Rules

### 4.1 Field Validations

| Field Name | Data Type | Required | Validation Rule | Error Message |
| --- | --- | --- | --- | --- |
| life_status | String(1) | Yes | Must be 'A' or 'D' | Invalid life status |
| date_of_death | DateTime | Conditional | Required if life_status = 'D' | Date of death required for deceased |
| offence_date | DateTime | Yes | Must exist in notice | Invalid offence date |
| suspension_type | String(2) | Yes | Must be 'PS' for RIP/RP2 | Invalid suspension type |
| reason_of_suspension | String(3) | Yes | Must be 'RIP' or 'RP2' | Invalid suspension reason |

### 4.2 Business Validations

| Validation | Rule | Error Code |
| --- | --- | --- |
| Notice exists | notice_no must exist in database | OCMS-4001 |
| Source authorized | PLUS cannot apply RIP/RP2 | OCMS-4000 |
| Stage allowed | Must be in allowed stages list | OCMS-4002 |
| Not paid | RIP/RP2 cannot apply to paid notices | OCMS-4003 |

---

## 5. Exception Handling

### 5.1 System Exceptions

| Exception Code | Exception Name | Condition | Handling |
| --- | --- | --- | --- |
| SEX001 | MHA Connection Error | Cannot connect to MHA | Log error, retry with backoff |
| SEX002 | DataHive Query Error | D90 query fails | Log error, default life_status='A' |
| SEX003 | Database Error | Insert/Update fails | Rollback transaction, log error |
| SEX004 | Null Date of Death | life_status='D' but no DoD | Log warning, use current date |

### 5.2 Business Exceptions

| Exception Code | Exception Name | Condition | Handling |
| --- | --- | --- | --- |
| BEX001 | Already Suspended | PS-RIP/RP2 already active | Return success (idempotent) |
| BEX002 | Invalid Notice | Notice not found | Return error OCMS-4001 |
| BEX003 | Source Not Allowed | PLUS tries to apply RIP/RP2 | Return error OCMS-4000 |
| BEX004 | Paid Notice | Notice has payment record | Return error OCMS-4003 |

---

## 6. Test Scenarios

### 6.1 Condition Test Cases

| Test ID | Condition | Test Input | Expected Output | Status |
| --- | --- | --- | --- | --- |
| TC001 | C001 - MHA Deceased | life_status='D' from MHA | Proceed to suspension | - |
| TC002 | C002 - DataHive Deceased | FIN found in D90 | life_status='D', proceed to suspension | - |
| TC003 | C003 - Alive | life_status='A' | Continue normal processing | - |
| TC004 | C004 - PS-RIP | DoD=2024-10-01, offence=2024-09-01 | Apply PS-RIP | - |
| TC005 | C005 - PS-RP2 | DoD=2024-08-01, offence=2024-09-01 | Apply PS-RP2 | - |
| TC006 | C006 - RP2 Report Eligible | PS-RP2 today, H/D offender, life_status=D | Include in report | - |
| TC007 | C006 - RP2 Report Not Eligible (Owner) | PS-RP2 today, O offender | Exclude from report | - |
| TC008 | C008 - Revive PS | OIC authorized, active PS-RIP | Allow revival | - |
| TC009 | R003 - PLUS Cannot Apply | Source=PLUS, code=RIP | Return error OCMS-4000 | - |
| TC010 | R007 - CRS without Revival | Existing PS-RIP, apply PS-FP | Allow without revival | - |

### 6.2 Edge Cases

| Test ID | Scenario | Input | Expected Behavior |
| --- | --- | --- | --- |
| EC001 | Same day death and offence | DoD = offence_date | Apply PS-RIP (>= condition) |
| EC002 | Missing date_of_death | life_status='D', DoD=null | Log warning, use current date |
| EC003 | Already has PS-RIP | Apply PS-RIP again | Idempotent - return success |
| EC004 | PS-RP2 on Owner | owner_driver_indicator='O' | Apply PS-RP2, exclude from report |

---

## 7. UI Display Rules

### 7.1 RIP Superscript Display

**Reference:** FD Section 4.6 - Displaying Notices with PS-RIP and PS-RP2

| Rule | Condition | Display |
| --- | --- | --- |
| Show superscript "R" | Active PS-RIP or PS-RP2 | Notice No with superscript R (e.g., "A12345678^R") |
| Hide superscript | No active RIP/RP2 or revived | Normal notice number |

**Display Locations:**
- Search Notice result summary list
- View Notice fixed header box
- View Notice Overview tab

**Detection Logic:**
```
IF ocms_valid_offence_notice.suspension_type = 'PS'
   AND ocms_valid_offence_notice.epr_reason_of_suspension IN ('RIP', 'RP2')
THEN Display superscript "R" next to Notice Number
```

---

## 8. Changelog

| Version | Date | Author | Changes |
| --- | --- | --- | --- |
| 1.0 | 15/01/2026 | Claude | Initial version based on FD Section 4 and backend code analysis |
| 1.1 | 27/01/2026 | Claude | Data Dictionary alignment: Fixed field name `reason_suspension_date` → `date_of_suspension`, added table references to condition fields, fixed `due_date_of_revival` → `date_of_revival` for revival check |

# Condition Plan: Foreign Vehicle Notice Processing

## Overview

| Attribute | Value |
| --- | --- |
| Feature Name | Foreign Vehicle Notice Processing |
| Version | v2.0 |
| Author | Claude |
| Created Date | 15/01/2026 |
| Last Updated | 27/01/2026 |
| Status | Revised |
| FD Reference | OCMS 14 - Section 3 |
| TD Reference | OCMS 14 - Section 2 |

---

## 1. Reference Documents

| Document Type | Reference | Description |
| --- | --- | --- |
| Functional Document | OCMS 14 - Section 3 | Foreign Vehicle Notice Processing |
| Technical Document | OCMS 14 - Section 2 | Technical implementation |
| Backend Code | AdminFeeServiceImpl.java | Admin fee processing logic |
| Backend Code | AdminFeeHelper.java | Admin fee helper functions |
| External Spec | vHub External Agency API v1.0 | vHub API specification |
| External Spec | REPCCS Interfaces v2.0 | REPCCS interface specification |

---

## 2. Business Conditions

### 2.1 Foreign Vehicle Notice Creation & Suspension

**Description:** When a notice is created for a foreign vehicle, it is automatically suspended with PS-FOR.

**Reference:** FD Section 3.1 - Notice Processing Flow for Foreign Vehicles

#### Condition Matrix

| Condition ID | Condition Name | Input Fields | Logic | Output/Action |
| --- | --- | --- | --- | --- |
| C001 | Auto PS-FOR on Creation | vehicle_registration_type | vehicle_registration_type = 'F' | Apply PS-FOR suspension |
| C002 | PS-FOR at NPA Stage | processing_stage | processing_stage = 'NPA' | Backend auto applies PS-FOR |
| C003 | Manual PS-FOR Application | processing_stage | stage IN (NPA, eNA, ROV, RD1, RD2, RR3, DN1, DN2, DR3, CPC, CFC) | Allow manual PS-FOR |

---

### 2.2 Exit Conditions for Foreign Vehicle Notice

**Description:** Conditions that cause a Foreign Vehicle Notice to exit the FOR processing flow.

**Reference:** FD Section 3.2.1 - Exit Conditions for a Foreign Vehicle Notice

#### Condition Matrix

| Condition ID | Condition Name | Condition | Result |
| --- | --- | --- | --- |
| C010 | Payment Received | amount_paid > 0 | Exit flow, send 'S' (Settled) to vHub |
| C011 | TS Applied | suspension_type = 'TS' | Exit flow, send 'C' (Cancelled) to vHub |
| C012 | PS Applied (Other) | epr_reason_of_suspension != 'FOR' | Exit flow, send 'C' (Cancelled) to vHub |
| C013 | Notice Archived | Notice archived/purged | Exit flow, send 'C' (Cancelled) to vHub |

---

### 2.3 PS-FOR Suspension Rules

**Description:** Rules governing the PS-FOR suspension behavior.

**Reference:** FD Section 3.3.3 - Suspension, Furnish hirer/driver, Payment Rules for PS-FOR

#### Condition Details

**C020: Notice Payability with PS-FOR**

| Attribute | Value |
| --- | --- |
| Description | Notice remains payable when suspended with PS-FOR |
| Field | payment_acceptance_allowed |
| Logic | If epr_reason_of_suspension = 'FOR' THEN payment_acceptance_allowed = 'Y' |
| Output | Notice can be paid via eService and AXS |

```
IF epr_reason_of_suspension = 'FOR'
THEN payment_acceptance_allowed = 'Y'
```

---

**C021: Notice Furnishability with PS-FOR**

| Attribute | Value |
| --- | --- |
| Description | Notice is NOT furnishable when suspended with PS-FOR |
| Field | furnishable |
| Logic | If epr_reason_of_suspension = 'FOR' THEN furnishable = 'N' |
| Output | Notice cannot be furnished via eService |

```
IF epr_reason_of_suspension = 'FOR'
THEN furnishable = 'N'
```

---

**C022: Additional TS over PS-FOR**

| Attribute | Value |
| --- | --- |
| Description | TS can be applied over PS-FOR without revival |
| Exception | TS-UNC cannot be applied |
| Logic | Allow TS application, PS-FOR auto-reapplies after TS revival |

```
IF epr_reason_of_suspension = 'FOR' AND new_suspension_type = 'TS'
THEN
    IF new_suspension_code = 'UNC'
    THEN REJECT
    ELSE ALLOW (no PS revival required)
```

---

**C023: Additional PS over PS-FOR**

| Attribute | Value |
| --- | --- |
| Description | New PS requires PS-FOR revival (with exceptions) |
| CRS Exceptions | PS-FP, PS-PRA can be applied WITHOUT revival |
| Non-CRS | Other PS codes require PS-FOR revival |

```
IF epr_reason_of_suspension = 'FOR' AND new_suspension_type = 'PS'
THEN
    IF new_suspension_code IN ('FP', 'PRA')
    THEN ALLOW WITHOUT REVIVAL (update CRS reason only)
    ELSE REQUIRE PS-FOR REVIVAL
```

---

**C024: Manual Hirer/Driver Update with PS-FOR**

| Attribute | Value |
| --- | --- |
| Description | OCMS users can update Hirer/Driver without revival |
| Logic | Allow edit, no notification triggered |

```
IF epr_reason_of_suspension = 'FOR' AND action = 'UPDATE_HIRER_DRIVER'
THEN ALLOW (no PS revival, no notification)
```

---

### 2.4 vHub Notice Classification Conditions

**Description:** Conditions for classifying notices to send to vHub.

**Reference:** FD Section 3.4.1.1 - Use Case for vHub API

#### Condition Matrix - Outstanding Notices (Status 'O')

| Condition ID | Condition Name | Input Fields | Logic |
| --- | --- | --- | --- |
| C030 | New Outstanding FOR | suspension_type, epr_reason_of_suspension, amount_paid, offence_date | suspension_type = 'PS' AND epr_reason_of_suspension = 'FOR' AND amount_paid = 0 AND offence_date <= (today - FOR_days) |
| C031 | Updated Outstanding FOR | same as C030 + last_update_date | C030 AND (pp_code OR composition_amount OR suspension changed in last 24h) |
| C032 | Revived Outstanding | same as C030 + previous_status | Previously sent as 'C' or 'S', now revived and unpaid |

```sql
-- Outstanding Notice Query (Field names per Data Dictionary)
SELECT notice_no, vehicle_no, notice_date_and_time, computer_rule_code,
       pp_code, vehicle_category, composition_amount, administration_fee,
       suspension_type, epr_reason_of_suspension, amount_paid,
       vhub_sent_flag  -- [NEW FIELD] proposed for OCMS 14
FROM ocms_valid_offence_notice
WHERE suspension_type = 'PS'
AND epr_reason_of_suspension = 'FOR'
AND amount_paid = 0
AND notice_date_and_time <= DATEADD(day, -@FOR_days, GETDATE())
```

---

#### Condition Matrix - Settled Notices (Status 'S')

| Condition ID | Condition Name | Input Fields | Logic |
| --- | --- | --- | --- |
| C040 | Newly Paid FOR | amount_paid, payment_date | amount_paid > 0 AND payment_date >= (today - 24 hours) AND previously_sent_to_vhub = 'Y' |

```sql
-- Settled Notice Query (Field names per Data Dictionary)
SELECT notice_no, vehicle_no, notice_date_and_time, computer_rule_code,
       pp_code, vehicle_category, composition_amount, administration_fee,
       amount_paid, payment_date,
       vhub_sent_flag  -- [NEW FIELD] proposed for OCMS 14
FROM ocms_valid_offence_notice
WHERE epr_reason_of_suspension = 'FOR'
AND amount_paid > 0
AND payment_date >= DATEADD(hour, -24, GETDATE())
```

---

#### Condition Matrix - Cancelled Notices (Status 'C')

| Condition ID | Condition Name | Input Fields | Logic |
| --- | --- | --- | --- |
| C050 | TS Applied Today | suspension_type, suspension_date | suspension_type = 'TS' AND suspension_date = today AND previously_sent_to_vhub = 'Y' |
| C051 | PS Applied Today | epr_reason_of_suspension, suspension_date | epr_reason_of_suspension != 'FOR' AND suspension_date = today AND previously_sent_to_vhub = 'Y' |
| C052 | Scheduled for Archive | archive_date | archive_date = tomorrow AND previously_sent_to_vhub = 'Y' |

```sql
-- Cancelled Notice Query (TS/PS today) - Field names per Data Dictionary
SELECT notice_no, vehicle_no, notice_date_and_time, computer_rule_code,
       pp_code, vehicle_category, composition_amount, administration_fee,
       suspension_type, epr_reason_of_suspension, epr_date_of_suspension,
       vhub_sent_flag  -- [NEW FIELD] proposed for OCMS 14
FROM ocms_valid_offence_notice
WHERE (
    (suspension_type = 'TS' AND CAST(epr_date_of_suspension AS DATE) = CAST(GETDATE() AS DATE))
    OR
    (suspension_type = 'PS' AND epr_reason_of_suspension != 'FOR' AND CAST(epr_date_of_suspension AS DATE) = CAST(GETDATE() AS DATE))
)
AND vhub_sent_flag = 'Y'  -- [NEW FIELD]
```

---

### 2.5 REPCCS/CES EHT Listed Vehicle Conditions

**Description:** Conditions for notices to be sent to REPCCS and CES EHT.

**Reference:** FD Section 3.5.2, 3.6.2

#### Condition Matrix

| Condition ID | Condition Name | Logic |
| --- | --- | --- |
| C060 | Eligible for REPCCS/CES | suspension_type = 'PS' AND epr_reason_of_suspension = 'FOR' AND amount_paid = 0 AND offence_date_and_time <= (current_date - FOR_days) |

```sql
-- Listed Vehicle Query (Field names per Data Dictionary)
SELECT vehicle_no, notice_date_and_time, computer_rule_code, pp_code, composition_amount
FROM ocms_valid_offence_notice
WHERE suspension_type = 'PS'
AND epr_reason_of_suspension = 'FOR'
AND amount_paid = 0
AND notice_date_and_time <= DATEADD(day, -@FOR_days, GETDATE())
```

---

### 2.6 Admin Fee Conditions

**Description:** Conditions for applying administration fee to unpaid FOR notices.

**Reference:** FD Section 3.7 - Heavier Penalties for Unpaid Foreign Vehicle Notices

#### Condition Matrix

| Condition ID | Condition Name | Input Fields | Logic |
| --- | --- | --- | --- |
| C070 | Eligible for Admin Fee | suspension_type, epr_reason_of_suspension, amount_paid, offence_date, admin_fee_applied | suspension_type = 'PS' AND epr_reason_of_suspension = 'FOR' AND amount_paid = 0 AND offence_date <= (today - FOD_days) AND admin_fee_applied = 'N' |

```sql
-- Admin Fee Eligibility Query (Field names per Data Dictionary)
SELECT notice_no, composition_amount, administration_fee, amount_paid,
       notice_date_and_time, suspension_type, epr_reason_of_suspension
FROM ocms_valid_offence_notice
WHERE suspension_type = 'PS'
AND epr_reason_of_suspension = 'FOR'
AND amount_paid = 0
AND notice_date_and_time <= DATEADD(day, -@FOD_days, GETDATE())
AND (administration_fee IS NULL OR administration_fee = 0)
```

#### Admin Fee Application

| Attribute | Value |
| --- | --- |
| Description | Apply AFO amount to notice composition |
| Parameter | AFO (Admin Fee Amount) |
| Update Fields | administration_fee, amount_payable (per Data Dictionary) |
| Post-Action | Send updated notice to vHub as 'O' (Outstanding) |

```
-- Admin Fee Update (Field names per Data Dictionary)
UPDATE ocms_valid_offence_notice
SET administration_fee = @AFO_amount,
    amount_payable = composition_amount + @AFO_amount,
    upd_date = GETDATE(),
    upd_user_id = 'ocmsiz_app_conn'
WHERE <C070 conditions>
```

---

### 2.7 Vehicle Type Mapping Conditions

**Description:** Mapping OCMS vehicle types to vHub vehicle types.

**Reference:** FD Section 3.4.1.2 - vHub API Sub Flows

#### Condition Matrix

| Condition ID | OCMS Vehicle Category | vHub Vehicle Type | Logic |
| --- | --- | --- | --- |
| C080 | H (Heavy Vehicle) | B (Bus) | Map 'H' to 'B' |
| C081 | Y | M (Motorcycle) | Map 'Y' to 'M' |
| C082 | C (Car) | C | Direct mapping |
| C083 | M (Motorcycle) | M | Direct mapping |
| C084 | B (Bus) | B | Direct mapping |

```
CASE vehicle_category
    WHEN 'H' THEN 'B'
    WHEN 'Y' THEN 'M'
    ELSE vehicle_category
END AS vhub_vehicle_type
```

---

### 2.8 SLIFT Integration Conditions

**Description:** Conditions for SLIFT encryption/decryption service usage.

**Reference:** FD Section 3.4.2 - vHub SFTP Interface, Functional Flowchart

#### Condition Matrix

| Condition ID | Condition Name | Direction | Logic | Action |
| --- | --- | --- | --- | --- |
| C090 | Encrypt Outbound File | OCMS → External | File generated for SFTP transfer | Call SLIFT encrypt before Azure upload |
| C091 | Decrypt Inbound File | External → OCMS | File downloaded from SFTP | Call SLIFT decrypt before processing |
| C092 | SLIFT Failure | Any | SLIFT returns error | Retry 3x, then log error and send email |

#### SLIFT Usage Per Interface

| Interface | Direction | SLIFT Operation | Timing |
| --- | --- | --- | --- |
| vHub SFTP | Outbound | Encrypt | After file generation, before Azure upload |
| vHub ACK | Inbound | Decrypt | After Azure download, before parsing |
| vHub NTL | Inbound | Decrypt | After Azure download, before parsing |
| REPCCS Listed | Outbound | Encrypt | After file generation, before Azure upload |
| CES EHT Tagged | Outbound | Encrypt | After file generation, before Azure upload |

---

### 2.9 Batching Conditions

**Description:** Conditions for batch processing and record count thresholds.

**Reference:** Functional Flowchart

#### Condition Matrix

| Condition ID | Condition Name | Threshold | Logic | Action |
| --- | --- | --- | --- | --- |
| C100 | API Batch Size | 50 | Record count per API call | Batch records into groups of 50 |
| C101 | SFTP Batch Check | 200 | Total record count | If > 200 records, use SFTP instead of API |
| C102 | Single File per Batch | 1 | Files per SFTP transfer | Generate single XML file per batch date |

```
-- Batch Size Decision (per Flowchart)
IF @record_count > 200 THEN
    USE SFTP METHOD
ELSE
    USE API METHOD (batch size 50)
END IF
```

---

## 3. Decision Trees

### 3.1 PS-FOR Application Flow

```
START
  │
  ├─► Notice Created with vehicle_registration_type = 'F'?
  │     │
  │     ├─ YES ─► Processing Stage = 'NPA'?
  │     │           │
  │     │           ├─ YES ─► Auto Apply PS-FOR ─► END
  │     │           │
  │     │           └─ NO ──► Manual PS-FOR by User ─► END
  │     │
  │     └─ NO ──► Standard Notice Flow ─► END
END
```

---

### 3.2 vHub Notice Classification Decision Tree

```
START
  │
  ├─► Notice has PS-FOR and was sent to vHub before?
  │     │
  │     ├─ NO ──► Is notice past FOR period and unpaid?
  │     │           │
  │     │           ├─ YES ─► Send as 'O' (Outstanding) ─► END
  │     │           │
  │     │           └─ NO ──► Do not send yet ─► END
  │     │
  │     └─ YES ─► Check current status:
  │                 │
  │                 ├─► Paid in last 24h?
  │                 │     │
  │                 │     └─ YES ─► Send as 'S' (Settled) ─► END
  │                 │
  │                 ├─► TS/PS (non-FOR) applied today?
  │                 │     │
  │                 │     └─ YES ─► Send as 'C' (Cancelled) ─► END
  │                 │
  │                 ├─► Scheduled for archive tomorrow?
  │                 │     │
  │                 │     └─ YES ─► Send as 'C' (Cancelled) ─► END
  │                 │
  │                 └─► Fields updated (PP code, amount, etc)?
  │                       │
  │                       └─ YES ─► Send as 'O' (Outstanding) ─► END
END
```

---

### 3.3 Admin Fee Decision Tree

```
START
  │
  ├─► Notice has PS-FOR?
  │     │
  │     ├─ NO ──► Not eligible ─► END
  │     │
  │     └─ YES ─► Notice unpaid (amount_paid = 0)?
  │                 │
  │                 ├─ NO ──► Not eligible ─► END
  │                 │
  │                 └─ YES ─► Past FOD period?
  │                             │
  │                             ├─ NO ──► Not eligible yet ─► END
  │                             │
  │                             └─ YES ─► Admin fee already applied?
  │                                         │
  │                                         ├─ YES ─► Skip ─► END
  │                                         │
  │                                         └─ NO ──► Apply AFO ─► END
END
```

---

## 4. Validation Rules

### 4.1 Parameter Validations

| Field Name | Data Type | Required | Validation Rule | Error Message |
| --- | --- | --- | --- | --- |
| FOR | Integer | Yes | Must be > 0 | FOR parameter must be positive |
| FOD | Integer | Yes | Must be > 0 | FOD parameter must be positive |
| AFO | Decimal | Yes | Must be >= 0 | AFO parameter must be non-negative |

### 4.2 Notice Field Validations

| Field Name | Data Type | Required | Validation Rule | Error Message |
| --- | --- | --- | --- | --- |
| vehicle_registration_type | varchar(1) | Yes | Must be 'F' for FOR flow | Invalid vehicle registration type |
| suspension_type | varchar(2) | Yes | Must be 'PS' for FOR | Invalid suspension type |
| epr_reason_of_suspension | varchar(3) | Yes | Must be 'FOR' | Invalid EPR reason |
| offence_date_and_time | datetime2 | Yes | Must be valid date | Invalid offence date |

### 4.3 vHub API Validations

| Field Name | Data Type | Max Length | Validation Rule |
| --- | --- | --- | --- |
| ViolationNo | varchar | 10 | Not null, alphanumeric |
| OffenceDateTime | varchar | 14 | Format: YYYYMMDDHHmmss |
| VehicleNo | varchar | 14 | Not null |
| ViolationStatus | varchar | 1 | Must be 'O', 'S', or 'C' |

---

## 5. Exception Handling

### 5.1 System Exceptions

| Exception Code | Exception Name | Condition | Handling |
| --- | --- | --- | --- |
| SEX001 | Parameter Not Found | FOR/FOD/AFO not in DB | Log error, send email, stop processing |
| SEX002 | vHub API Timeout | API call exceeds timeout | Retry 2x, log error, continue with next batch |
| SEX003 | SFTP Connection Failed | Cannot connect to SFTP | Retry 2x, log error, send email |
| SEX004 | Database Query Timeout | Query exceeds timeout | Retry 1x, log error |
| SEX005 | SLIFT Service Unavailable | Cannot connect to SLIFT | Retry 3x, log error, send email |
| SEX006 | SLIFT Encryption Failed | Encryption operation failed | Log error, abort transfer, send email |
| SEX007 | SLIFT Decryption Failed | Decryption operation failed | Log error, mark file as failed, send email |
| SEX008 | Azure Blob Upload Failed | Cannot upload to Azure | Retry 2x, log error, send email |

### 5.2 Business Exceptions

| Exception Code | Exception Name | Condition | Handling |
| --- | --- | --- | --- |
| BEX001 | No Eligible Notices | No notices match criteria | Log info, complete successfully |
| BEX002 | vHub Record Error | vHub returns error for record | Log error, add to error report |
| BEX003 | Duplicate Batch | Same batch date already exists | Skip duplicate records |

---

## 6. Test Scenarios

### 6.1 PS-FOR Application Tests

| Test ID | Condition | Test Input | Expected Output |
| --- | --- | --- | --- |
| TC001 | Auto PS-FOR on NPA | vehicle_registration_type='F', stage='NPA' | PS-FOR applied automatically |
| TC002 | Manual PS-FOR | vehicle_registration_type='F', stage='ROV' | PS-FOR allowed by user |
| TC003 | PS-FOR Payability | epr_reason_of_suspension='FOR' | payment_acceptance_allowed='Y' |
| TC004 | PS-FOR Not Furnishable | epr_reason_of_suspension='FOR' | furnishable='N' |

### 6.2 vHub Classification Tests

| Test ID | Condition | Test Input | Expected Output |
| --- | --- | --- | --- |
| TC010 | Outstanding New | PS-FOR, unpaid, past FOR period | Status = 'O' |
| TC011 | Settled | PS-FOR, paid in last 24h | Status = 'S' |
| TC012 | Cancelled TS | TS applied today | Status = 'C' |
| TC013 | Cancelled Archive | Archive tomorrow | Status = 'C' |
| TC014 | Updated Outstanding | PS-FOR, pp_code changed | Status = 'O' |

### 6.3 Admin Fee Tests

| Test ID | Condition | Test Input | Expected Output |
| --- | --- | --- | --- |
| TC020 | Eligible | PS-FOR, unpaid, past FOD | Admin fee applied |
| TC021 | Not Eligible - Paid | PS-FOR, amount_paid > 0 | No admin fee |
| TC022 | Not Eligible - Not Past FOD | PS-FOR, within FOD period | No admin fee |
| TC023 | Already Applied | PS-FOR, admin_fee > 0 | Skip |

### 6.4 Vehicle Type Mapping Tests

| Test ID | OCMS Type | Expected vHub Type |
| --- | --- | --- |
| TC030 | H | B |
| TC031 | Y | M |
| TC032 | C | C |
| TC033 | M | M |

---

## 7. Decision Tables

### 7.1 PS-FOR Suspension Rules

| PS-FOR Active | New Suspension | Type | Action |
| --- | --- | --- | --- |
| Yes | TS (not UNC) | TS | Allow without revival |
| Yes | TS-UNC | TS | Reject |
| Yes | PS-FP | PS | Allow without revival (update CRS only) |
| Yes | PS-PRA | PS | Allow without revival (update CRS only) |
| Yes | PS-APP | PS | Require PS-FOR revival |
| Yes | PS-CFA | PS | Require PS-FOR revival |

### 7.2 vHub Status Classification

| Unpaid | Past FOR | TS Today | PS Today | Archive Tomorrow | Paid 24h | Status |
| --- | --- | --- | --- | --- | --- | --- |
| Yes | Yes | No | No | No | No | O |
| No | - | No | No | No | Yes | S |
| - | - | Yes | No | No | No | C |
| - | - | No | Yes | No | No | C |
| - | - | No | No | Yes | No | C |

---

## 8. Assumptions Log

| ID | Assumption | Rationale |
| --- | --- | --- |
| A001 | PS-FOR is only auto-applied at NPA stage | Based on FD Section 3.3.3 |
| A002 | vHub 'Settled' only for payments in last 24 hours | Based on FD Section 3.4.1.1 |
| A003 | Vehicle type 'Y' maps to 'M' for vHub | [ASSUMPTION] Based on 'H' to 'B' mapping logic |
| A004 | TS-UNC cannot be applied over PS-FOR | Based on FD Section 3.3.3 |
| A005 | Archive notice triggers 'Cancelled' status to vHub | Based on FD Section 3.4.1.1 |

---

## 9. Changelog

| Version | Date | Author | Changes |
| --- | --- | --- | --- |
| 1.0 | 15/01/2026 | Claude | Initial version based on FD Section 3 |
| 2.0 | 27/01/2026 | Claude | Added SLIFT Integration Conditions (Section 2.8), Batching Conditions (Section 2.9), SLIFT exceptions (SEX005-008). Fixed field name: administration_fee per Data Dictionary. Aligned with Functional Flowchart. |

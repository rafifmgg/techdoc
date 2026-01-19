# Condition Plan: OCMS 14 Section 6 - VIP Vehicle Notice Processing

## Overview

| Attribute | Value |
| --- | --- |
| Feature Name | VIP Vehicle Notice Processing |
| Version | v1.0 |
| Author | Claude |
| Created Date | 15/01/2026 |
| Last Updated | 15/01/2026 |
| Status | Draft |
| Functional Document | v1.8_OCMS 14 Functional Document Section 7 |
| Technical Document | OCMS 14 Technical Doc Section 6 |

---

## 1. Reference Documents

| Document Type | Reference | Description |
| --- | --- | --- |
| Functional Document | OCMS 14 FD Section 7 | VIP Vehicle Notice Processing |
| Technical Document | OCMS 14 TD Section 6 | Technical specifications |
| Data Dictionary | intranet.json | Database table definitions |
| Related FD | OCMS 7 | Vehicle Registration Type Check |
| Related FD | OCMS 21 | Double Booking Function |
| Related FD | OCMS 41 | Furnish Driver/Hirer |

---

## 2. Business Conditions

### 2.1 VIP Vehicle Detection

**Description:** Detect if vehicle has valid VIP Parking Label during notice creation.

**Reference:** Functional Document Section 7.1, OCMS 7

#### Condition Matrix

| Condition ID | Condition Name | Input Fields | Logic | Output/Action |
| --- | --- | --- | --- | --- |
| C001 | VIP Vehicle Check | vehicle_no | Query CAS/FOMS VIP_VEHICLE table | Return vehicle_registration_type = 'V' |
| C002 | VIP Label Active | vip_label_status | status = 'ACTIVE' | Proceed as VIP |
| C003 | VIP Label Expired | vip_label_status | status = 'EXPIRED' | Process as standard vehicle |

#### Condition Details

**C001: VIP Vehicle Check**

| Attribute | Value |
| --- | --- |
| Description | Check if vehicle number exists in CAS/FOMS VIP_VEHICLE table with valid label |
| Trigger | During notice creation, after vehicle_registration_type check |
| Input | vehicle_no from raw offence data |
| Logic | Query VIP_VEHICLE table WHERE vehicle_no = input AND status = 'ACTIVE' |
| Output | If found: vehicle_registration_type = 'V' |
| Else | Proceed to next check (other vehicle types) |

```
IF vehicle_no EXISTS IN vip_vehicle TABLE
   AND vip_vehicle.status = 'ACTIVE'
THEN vehicle_registration_type = 'V'
ELSE continue_vehicle_type_check()
```

---

### 2.2 Offence Type Classification

**Description:** Classify notice by offence type to determine processing flow.

**Reference:** Functional Document Section 7.2.1

#### Condition Matrix

| Condition ID | Condition Name | Input Fields | Logic | Output/Action |
| --- | --- | --- | --- | --- |
| C010 | Type O Check | computer_rule_code | Rule code indicates Parking Offence | Process via Type O&E workflow |
| C011 | Type E Check | computer_rule_code | Rule code indicates Payment Evasion | Process via Type O&E workflow |
| C012 | Type U Check | computer_rule_code | Rule code indicates UPL | Process via UPL workflow (KIV) |

#### Condition Details

**C010/C011: Type O & E Processing**

| Attribute | Value |
| --- | --- |
| Description | Parking Offence and Payment Evasion notices follow VIP processing flow |
| Trigger | After notice creation |
| Input | offence_notice_type ('O' or 'E') |
| Logic | If type = 'O' or 'E' AND vehicle_registration_type = 'V' |
| Output | Process via VIP Notice workflow (Section 7.2.2) |

**C012: Type U Processing**

| Attribute | Value |
| --- | --- |
| Description | UPL notices follow standard UPL workflow |
| Trigger | After notice creation |
| Input | offence_notice_type = 'U' |
| Logic | Type U notices do not follow VIP-specific flow |
| Output | Process via standard UPL workflow |

---

### 2.3 Advisory Notice (AN) Qualification

**Description:** Determine if VIP notice qualifies for Advisory Notice processing.

**Reference:** Functional Document Section 7.2.4

#### Condition Matrix

| Condition ID | Condition Name | Input Fields | Logic | Output/Action |
| --- | --- | --- | --- | --- |
| C020 | AN Eligible | offence_notice_type, an_criteria | Type O AND meets AN criteria | Set an_flag = 'Y', process via AN flow |
| C021 | AN Not Eligible | offence_notice_type | Type E OR not meeting criteria | Set an_flag = 'N', process via standard flow |

#### Condition Details

**C020: AN Qualification Check**

| Attribute | Value |
| --- | --- |
| Description | Check if Type O notice qualifies for Advisory Notice |
| Trigger | After notice created at NPA, before TS-OLD |
| Input | offence_notice_type, computer_rule_code, other AN criteria |
| Logic | Type = 'O' AND meets_an_criteria() |
| Output | an_flag = 'Y', next_processing_stage = 'RD1' |
| Else | an_flag = 'N', apply TS-OLD |

```
IF offence_notice_type = 'O'
   AND meets_advisory_notice_criteria()
THEN
   an_flag = 'Y'
   next_processing_stage = 'RD1'
   PROCESS via AN sub-flow (Section 7.2.4)
ELSE
   an_flag = 'N'
   APPLY TS-OLD suspension
   PROCESS via standard VIP flow (Section 7.2.2)
```

---

### 2.4 Double Booking Check

**Description:** Detect duplicate notices issued to same vehicle for same offence.

**Reference:** Functional Document Section 7.2.2 Step 5, OCMS 21

#### Condition Matrix

| Condition ID | Condition Name | Input Fields | Logic | Output/Action |
| --- | --- | --- | --- | --- |
| C030 | Double Booking Detected | vehicle_no, offence_date, rule_code, pp_code, lot_no | Duplicate exists | Apply PS-DBB |
| C031 | No Double Booking | comparison_result | No duplicate found | Continue processing |

#### Condition Details

**C030: Double Booking Detection**

| Attribute | Value |
| --- | --- |
| Description | Detect duplicate notices after notice creation |
| Trigger | After notice created at NPA |
| Input | vehicle_no, offence_date, offence_time, computer_rule_code, pp_code, parking_lot_no |
| Logic | Compare against existing notices with same combination |

```
IF EXISTS (
   SELECT 1 FROM ocms_valid_offence_notice
   WHERE vehicle_no = :vehicle_no
     AND DATE(notice_date_and_time) = :offence_date
     AND computer_rule_code = :rule_code
     AND pp_code = :pp_code
     AND (parking_lot_no = :lot_no OR (parking_lot_no IS NULL AND :lot_no IS NULL))
     AND notice_no != :current_notice_no
)
THEN
   APPLY PS-DBB to duplicate notice
ELSE
   CONTINUE processing
```

---

### 2.5 TS-OLD Suspension and Revival

**Description:** TS-OLD suspension for OIC investigation and revival conditions.

**Reference:** Functional Document Section 7.2.2 Steps 7-8

#### Condition Matrix

| Condition ID | Condition Name | Input Fields | Logic | Output/Action |
| --- | --- | --- | --- | --- |
| C040 | Apply TS-OLD | vehicle_registration_type, an_flag | Type V AND not AN | Apply TS-OLD (21 days) |
| C041 | Manual Revival | oic_action | OIC manually revives | Revive suspension, proceed to ROV |
| C042 | Auto Revival | due_date_of_revival | due_date <= current_date | Auto revive, proceed to ROV |

#### Condition Details

**C040: Apply TS-OLD**

| Attribute | Value |
| --- | --- |
| Description | Apply TS-OLD suspension for OIC investigation |
| Trigger | After notice creation, if not AN |
| Input | vehicle_registration_type = 'V', an_flag = 'N' |
| Logic | Apply 21-day temporary suspension |

```
IF vehicle_registration_type = 'V'
   AND an_flag = 'N'
THEN
   suspension_type = 'TS'
   epr_reason_of_suspension = 'OLD'
   due_date_of_revival = CURRENT_DATE + 21 days
   next_processing_stage = 'ROV'
```

**C041/C042: TS-OLD Revival**

| Attribute | Value |
| --- | --- |
| Description | Revive TS-OLD suspension (manual or auto) |
| Trigger | OIC action OR suspension expires |
| Input | Manual: OIC clicks revive; Auto: due_date_of_revival <= current_date |
| Output | Clear suspension, proceed to ROV for LTA check |

```
IF suspension_type = 'TS' AND epr_reason_of_suspension = 'OLD'
   AND (manual_revival OR due_date_of_revival <= CURRENT_DATE)
THEN
   suspension_type = NULL
   epr_reason_of_suspension = NULL
   next_processing_stage = 'ROV'
   next_processing_date = CURRENT_DATE
   CREATE revival record in ocms_suspended_notice
```

---

### 2.6 Vehicle Type Re-check at ROV

**Description:** Re-check vehicle type after LTA ownership response.

**Reference:** Functional Document Section 7.2.2 Step 10

#### Condition Matrix

| Condition ID | Condition Name | Input Fields | Logic | Output/Action |
| --- | --- | --- | --- | --- |
| C050 | Still VIP | lta_response, vip_check | Vehicle still has valid VIP label | Continue VIP flow |
| C051 | No Longer VIP | lta_response, vip_check | VIP label expired/cancelled | Convert to standard flow |

#### Condition Details

**C050: Vehicle Type Re-check**

| Attribute | Value |
| --- | --- |
| Description | After ROV stage, verify vehicle is still VIP |
| Trigger | After receiving LTA vehicle ownership response |
| Input | LTA response, current VIP status |
| Logic | Query VIP_VEHICLE table again to confirm status |

```
IF vehicle_registration_type = 'V'
   AND last_processing_stage = 'ROV'
THEN
   RE-CHECK VIP status from CAS/FOMS
   IF still_vip
      next_processing_stage = 'RD1'
      next_processing_date = CURRENT_DATE
   ELSE
      [ASSUMPTION] Convert to standard processing
```

---

### 2.7 Furnish Driver/Hirer Validation

**Description:** Validate furnished driver/hirer particulars.

**Reference:** Functional Document Section 7.2.5

#### Condition Matrix

| Condition ID | Condition Name | Input Fields | Logic | Output/Action |
| --- | --- | --- | --- | --- |
| C060 | Auto Approve | id_type, id_number, validation_result | All validations pass | Auto approve, redirect notice |
| C061 | Manual Review | validation_result | Some validations fail | OIC manual review required |
| C062 | Rejected | oic_decision | OIC rejects particulars | Remove furnish pending status |

#### Condition Details

**C060: Furnish Auto Approval**

| Attribute | Value |
| --- | --- |
| Description | Auto approve if all validation criteria met |
| Trigger | After furnish application received |
| Input | Driver/Hirer particulars |
| Logic | MHA validation pass, format valid, criteria met |

```
IF furnish_application.validation_status = 'VALID'
   AND mha_check_passed
   AND meets_auto_approval_criteria
THEN
   furnish_status = 'APPROVED'
   IF furnish_type = 'DRIVER'
      next_processing_stage = 'DN1'
   ELSE IF furnish_type = 'HIRER'
      next_processing_stage = 'RD1'
   next_processing_date = CURRENT_DATE
```

**C061: Manual Review Required**

| Attribute | Value |
| --- | --- |
| Description | OIC manual review when auto-approval criteria not met |
| Trigger | Validation incomplete or criteria not met |
| Input | Validation results |
| Output | Status = 'PENDING_OIC_REVIEW' |

**C062: Furnish Rejection**

| Attribute | Value |
| --- | --- |
| Description | OIC rejects furnished particulars |
| Trigger | OIC clicks reject |
| Output | Remove furnish pending status, allow re-furnish |

```
IF oic_decision = 'REJECTED'
THEN
   UPDATE notice: remove furnish_pending flag
   SYNC to internet: allow new furnish attempt
```

---

### 2.8 TS-CLV Application and Looping

**Description:** Apply TS-CLV at CPC stage (RR3/DR3 for MVP1) with looping behavior.

**Reference:** Functional Document Section 7.5, 7.7

#### Condition Matrix

| Condition ID | Condition Name | Input Fields | Logic | Output/Action |
| --- | --- | --- | --- | --- |
| C070 | Apply TS-CLV | stage, outstanding | At CPC AND outstanding | Apply TS-CLV |
| C071 | TS-CLV Expired | due_date_of_revival | Expired AND type still V | Re-apply TS-CLV |
| C072 | Manual Revival CLV | oic_action, type_change | OIC revives AND changes V→S | Exit looping, continue standard flow |
| C073 | Payment Made | payment_status | Fully paid | Apply PS-FP, stop processing |

#### Condition Details

**C070: Apply TS-CLV**

| Attribute | Value |
| --- | --- |
| Description | Apply TS-CLV at CPC stage for outstanding VIP notices |
| Trigger | End of RR3/DR3 stage (MVP1), CPC stage (post-MVP1) |
| Input | vehicle_registration_type, last_processing_stage, suspension_type |

```
IF vehicle_registration_type = 'V'
   AND last_processing_stage IN ('RR3', 'DR3')
   AND next_processing_stage = 'CPC'
   AND next_processing_date <= CURRENT_DATE
   AND (suspension_type IS NULL OR suspension_type = 'TS')
   AND payment_status != 'PAID'
THEN
   last_processing_stage = 'CPC'
   suspension_type = 'TS'
   epr_reason_of_suspension = 'CLV'
   CREATE suspension record
```

**C071: TS-CLV Looping**

| Attribute | Value |
| --- | --- |
| Description | Re-apply TS-CLV when expired (looping behavior) |
| Trigger | TS-CLV due_date_of_revival expired |
| Input | suspension_type = 'TS', reason = 'CLV', vehicle_registration_type = 'V' |

```
IF vehicle_registration_type = 'V'
   AND suspension_type = 'TS'
   AND epr_reason_of_suspension = 'CLV'
   AND due_date_of_revival <= CURRENT_DATE
THEN
   CREATE revival record (auto revival)
   RE-APPLY TS-CLV with new due_date_of_revival
   CREATE new suspension record
```

**C072: Manual Revival CLV (Exit Looping)**

| Attribute | Value |
| --- | --- |
| Description | OIC manually revives TS-CLV and changes vehicle type V→S |
| Trigger | OIC action via Staff Portal |
| Input | OIC revives AND changes vehicle_registration_type to 'S' |
| Output | Exit TS-CLV loop, continue standard processing |

```
IF oic_manually_revives_clv
   AND oic_changes_vehicle_type_to_s
THEN
   vehicle_registration_type = 'S'
   suspension_type = NULL
   epr_reason_of_suspension = NULL
   CONTINUE standard processing flow
```

---

## 3. Decision Trees

### 3.1 VIP Notice Creation Decision Flow

```
START (Notice Creation)
  │
  ├─► Vehicle Registration Type = 'V'?
  │     │
  │     ├─ NO ─► Process via standard flow ─► END
  │     │
  │     └─ YES ─► Offence Type?
  │                 │
  │                 ├─ Type U ─► Process via UPL flow ─► END
  │                 │
  │                 └─ Type O or E ─► Check Double Booking
  │                                     │
  │                                     ├─ DBB Found ─► Apply PS-DBB ─► END
  │                                     │
  │                                     └─ No DBB ─► AN Qualification?
  │                                                    │
  │                                                    ├─ YES (AN) ─► AN Flow ─► END
  │                                                    │
  │                                                    └─ NO ─► Apply TS-OLD ─► END
  │
END
```

### 3.2 TS-OLD Revival Decision Flow

```
START (TS-OLD Applied)
  │
  ├─► Due Date Reached OR OIC Manual Revival?
  │     │
  │     ├─ NO ─► Wait for expiry/action ─► LOOP
  │     │
  │     └─ YES ─► Revive Suspension
  │                 │
  │                 └─► LTA Vehicle Ownership Check (ROV)
  │                       │
  │                       └─► Re-check VIP Status
  │                             │
  │                             ├─ Still VIP ─► Prepare RD1 ─► END
  │                             │
  │                             └─ Not VIP ─► [Convert to standard] ─► END
  │
END
```

### 3.3 TS-CLV Looping Decision Flow

```
START (TS-CLV Applied at CPC)
  │
  ├─► Payment Made?
  │     │
  │     ├─ YES ─► Apply PS-FP ─► Processing Stops ─► END
  │     │
  │     └─ NO ─► TS-CLV Expired?
  │               │
  │               ├─ NO ─► Wait for expiry ─► LOOP
  │               │
  │               └─ YES ─► OIC Manually Revived?
  │                           │
  │                           ├─ NO ─► Auto Re-apply TS-CLV ─► LOOP
  │                           │
  │                           └─ YES ─► Vehicle Type Changed to 'S'?
  │                                       │
  │                                       ├─ YES ─► Exit Loop, Standard Flow ─► END
  │                                       │
  │                                       └─ NO ─► Re-apply TS-CLV ─► LOOP
  │
END
```

### 3.4 Furnish Driver/Hirer Decision Flow

```
START (Furnish Application Received)
  │
  ├─► Sync to Intranet
  │     │
  │     └─► Validate Particulars
  │           │
  │           ├─► Auto Approval Criteria Met?
  │           │     │
  │           │     ├─ YES ─► Auto Approve ─► Update Stage (DN1/RD1) ─► END
  │           │     │
  │           │     └─ NO ─► OIC Manual Review
  │           │               │
  │           │               ├─ APPROVE ─► Update Stage (DN1/RD1) ─► END
  │           │               │
  │           │               └─ REJECT ─► Remove Furnish Pending ─► END
  │
END
```

---

## 4. Validation Rules

### 4.1 Field Validations

| Field Name | Data Type | Required | Validation Rule | Error Code | Error Message |
| --- | --- | --- | --- | --- | --- |
| notice_no | string | Yes | 10 chars, alphanumeric | VAL001 | Notice number is required |
| vehicle_no | string | Yes | max 14 chars | VAL002 | Vehicle number is required |
| vehicle_registration_type | string | Yes | Enum: S, F, D, I, V | VAL003 | Invalid vehicle registration type |
| offence_notice_type | string | Yes | Enum: O, E, U | VAL004 | Invalid offence type |
| suspension_type | string | No | Enum: TS, PS | VAL005 | Invalid suspension type |
| epr_reason_of_suspension | string | No | max 3 chars | VAL006 | Invalid suspension reason |
| an_flag | string | No | Enum: Y, N | VAL007 | Invalid AN flag |

### 4.2 Cross-Field Validations

| Validation ID | Fields Involved | Rule | Error Code | Error Message |
| --- | --- | --- | --- | --- |
| XF001 | suspension_type, epr_reason_of_suspension | If suspension_type set, reason required | XF001 | Suspension reason required |
| XF002 | vehicle_registration_type, suspension_type | If type V at CPC, must have TS-CLV | XF002 | VIP at CPC must have TS-CLV |
| XF003 | an_flag, offence_notice_type | an_flag = Y only for Type O | XF003 | AN flag only valid for Type O |
| XF004 | due_date_of_revival, suspension_type | If TS, due_date required | XF004 | Revival date required for TS |

### 4.3 Business Rule Validations

| Rule ID | Rule Name | Description | Condition | Error Code | Error Message |
| --- | --- | --- | --- | --- | --- |
| BR001 | VIP Notice Payable | VIP notices remain payable under TS-OLD/TS-CLV | payment_acceptance_allowed = 'Y' | BR001 | Notice must remain payable |
| BR002 | VIP No eNotification | VIP notices bypass ENA stage | No SMS/email sent | BR002 | eNotification not allowed for VIP |
| BR003 | VIP Furnish Allowed | Furnish allowed at ROV, RD1, RD2 stages | Check stage and suspension | BR003 | Furnish not allowed at this stage |
| BR004 | TS-CLV Looping | TS-CLV auto re-applies if type still V | vehicle_registration_type = 'V' | BR004 | TS-CLV must loop for VIP |

---

## 5. Status Transitions

### 5.1 VIP Notice Processing Stages

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                                                             │
│  ┌─────────┐   TS-OLD    ┌─────────┐   Revived   ┌─────────┐               │
│  │   NPA   │ ──────────► │ TS-OLD  │ ──────────► │   ROV   │               │
│  └─────────┘             └─────────┘             └─────────┘               │
│       │                                               │                     │
│       │ (AN)                                          │ LTA Check           │
│       ▼                                               ▼                     │
│  ┌─────────┐                                    ┌─────────┐                │
│  │ AN Flow │ ─────────────────────────────────► │   RD1   │                │
│  └─────────┘                                    └─────────┘                │
│                                                      │                      │
│                                                      │ MHA/DH               │
│                                                      ▼                      │
│                                                 ┌─────────┐                │
│                                                 │   RD2   │                │
│                                                 └─────────┘                │
│                                                      │                      │
│                                                      │ MHA/DH               │
│                                                      ▼                      │
│                                                 ┌─────────┐                │
│                                                 │   RR3   │                │
│                                                 └─────────┘                │
│                                                      │                      │
│                                                      │ Outstanding          │
│                                                      ▼                      │
│                                                 ┌─────────┐   TS-CLV       │
│                                                 │   CPC   │ ──────────►    │
│                                                 └─────────┘   (Looping)    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 Stage Transition Matrix

| From Stage | To Stage | Trigger | Condition | Actions |
| --- | --- | --- | --- | --- |
| NPA | TS-OLD | Notice created | Type V, not AN | Apply TS-OLD |
| NPA | RD1 | Notice created | Type V, AN qualified | Generate AN letter |
| TS-OLD | ROV | Revived | Manual or auto revival | LTA ownership check |
| ROV | RD1 | LTA complete | MHA/DH checks done | Generate RD1 letter |
| RD1 | RD2 | Outstanding | After RD1 period | Generate RD2 letter |
| RD2 | RR3 | Outstanding | After RD2 period | Generate RR3 letter |
| RR3 | CPC | Outstanding | After RR3 period | Apply TS-CLV |

### 5.3 Suspension State Definitions

| Suspension | Type | Code | Description | Payable | Furnishable | Terminal |
| --- | --- | --- | --- | --- | --- | --- |
| TS-OLD | TS | OLD | OIC Investigation | Yes | No | No |
| TS-CLV | TS | CLV | Classified Vehicles | Yes | No | No (Looping) |
| PS-FP | PS | FP | Fully Paid | No | No | Yes |
| PS-DBB | PS | DBB | Double Booking | No | No | Yes |
| PS-ANS | PS | ANS | Advisory Notice Sent | No | No | Yes |

---

## 6. Allowed Functions Matrix

### 6.1 Functions by Notice Condition (VIP)

**Reference:** Functional Document Section 7.4

| Notice Stage | eNotification | Payment | Furnish Driver |
| --- | --- | --- | --- |
| NPA | No | Yes | No |
| TS-OLD | No | Yes | No |
| ROV | No | Yes | Yes |
| RD1 | No | Yes | Yes (Owner only) |
| DN1 | No | Yes | No |
| RD2 | No | Yes | Yes (Owner only) |
| DN2 | No | Yes | No |
| RR3/DR3 | No | Yes | No |
| CPC (TS-CLV) | No | Yes | No |

### 6.2 Suspension Rules for TS-CLV

**Reference:** Functional Document Section 7.7.2

| Condition | Rule | Description |
| --- | --- | --- |
| Applicable Stages | CPC (MVP1: RR3, DR3) | TS-CLV applied at these stages |
| OCMS User Apply | Yes | Authorized users can apply via Staff Portal |
| PLUS User Apply | No | Not available in PLUS Staff Portal |
| Payable After Suspension | Yes | Via eService and AXS |
| Furnishable After Suspension | No | Not allowed |
| Allow Additional TS | Yes | Other TS can be applied |
| Allow Additional PS | Yes | PS can be applied |
| Edit Vehicle Type | KIV | Post-MVP1, V→S allows court progression |

---

## 7. Calculation Formulas

### 7.1 TS-OLD Due Date Calculation

| Attribute | Value |
| --- | --- |
| Description | Calculate TS-OLD suspension expiry date |
| Formula | `due_date_of_revival = current_date + suspension_period` |
| Input Fields | current_date, suspension_period (default: 21 days) |
| Output Field | due_date_of_revival |

```
due_date_of_revival = CURRENT_DATE + INTERVAL '21' DAY
-- Note: Period configurable in OCMS_Suspension_Reason table
```

### 7.2 TS-CLV Due Date Calculation

| Attribute | Value |
| --- | --- |
| Description | Calculate TS-CLV suspension expiry date |
| Formula | `due_date_of_revival = current_date + suspension_period` |
| Input Fields | current_date, suspension_period (configurable) |
| Output Field | due_date_of_revival |

```
due_date_of_revival = CURRENT_DATE + INTERVAL suspension_period DAY
-- Suspension period retrieved from OCMS_Suspension_Reason table
```

---

## 8. Condition-Action Mapping

### 8.1 VIP Notice Creation

| Scenario | Conditions | Actions | Database Updates |
| --- | --- | --- | --- |
| VIP Type O/E | veh_reg_type=V, type=O/E, no_dbb, not_an | Apply TS-OLD | suspension_type='TS', reason='OLD' |
| VIP Type O AN | veh_reg_type=V, type=O, an_qualified | Generate AN letter | an_flag='Y', stage='RD1' |
| VIP Type U | veh_reg_type=V, type=U | Standard UPL flow | No VIP-specific update |
| VIP DBB | duplicate_found | Apply PS-DBB | suspension_type='PS', reason='DBB' |

### 8.2 TS-OLD Revival

| Scenario | Conditions | Actions | Database Updates |
| --- | --- | --- | --- |
| Manual Revival | OIC clicks revive | Revive, proceed to ROV | Clear suspension, stage='ROV' |
| Auto Revival | due_date reached | Auto revive, proceed to ROV | Clear suspension, stage='ROV' |

### 8.3 TS-CLV Application

| Scenario | Conditions | Actions | Database Updates |
| --- | --- | --- | --- |
| Outstanding at RR3/DR3 | type=V, stage=RR3/DR3, outstanding | Apply TS-CLV | suspension_type='TS', reason='CLV' |
| TS-CLV Expired | type=V, CLV expired | Re-apply TS-CLV | New suspension record |
| Paid | payment made | Apply PS-FP | Stop processing |
| Type Changed V→S | OIC changes type | Exit loop | type='S', clear suspension |

---

## 9. Exception Handling

### 9.1 Business Exceptions

| Exception Code | Exception Name | Condition | Handling |
| --- | --- | --- | --- |
| BEX001 | VIP Check Failed | CAS/FOMS unavailable | Log error, process as standard |
| BEX002 | LTA Check Failed | LTA timeout | Retry, log if persistent |
| BEX003 | MHA Check Failed | MHA unavailable | Proceed without update, retry later |
| BEX004 | Invalid Suspension State | Wrong suspension applied | Log, notify admin |

### 9.2 System Exceptions

| Exception Code | Exception Name | Condition | Handling |
| --- | --- | --- | --- |
| SEX001 | Database Error | DB connection failed | Retry 3 times, then log and notify |
| SEX002 | SFTP Error | Letter upload failed | Retry with backoff, alert |
| SEX003 | Email Error | Report email failed | Retry, log for manual send |

---

## 10. Test Scenarios

### Condition Test Cases

| Test ID | Condition | Test Input | Expected Output | Status |
| --- | --- | --- | --- | --- |
| TC001 | VIP Detection - Found | Valid VIP vehicle | vehicle_registration_type = 'V' | - |
| TC002 | VIP Detection - Not Found | Non-VIP vehicle | Continue other checks | - |
| TC003 | AN Qualification - Yes | Type O + AN criteria | an_flag = 'Y' | - |
| TC004 | AN Qualification - No | Type E | an_flag = 'N', apply TS-OLD | - |
| TC005 | TS-OLD Manual Revival | OIC revives | Suspension cleared, stage = ROV | - |
| TC006 | TS-OLD Auto Revival | Due date reached | Auto revive, stage = ROV | - |
| TC007 | TS-CLV Application | Outstanding at RR3 | TS-CLV applied | - |
| TC008 | TS-CLV Loop | CLV expired, type V | Re-apply TS-CLV | - |
| TC009 | TS-CLV Exit | Type changed V→S | Exit loop, standard flow | - |
| TC010 | Payment Made | Full payment | PS-FP applied, stop | - |

### Edge Cases

| Test ID | Scenario | Test Input | Expected Output |
| --- | --- | --- | --- |
| EC001 | VIP label expires mid-processing | Label expires at RD2 | Continue VIP flow until completion |
| EC002 | Double suspension | Existing TS + new TS | Handle per business rule |
| EC003 | Furnish after TS-CLV | Attempt furnish | Reject - not furnishable |
| EC004 | Multiple TS-CLV loops | 5+ loops | Continue looping until V→S or paid |

---

## 11. Assumptions Log

| ID | Assumption | Reason | Impact |
| --- | --- | --- | --- |
| A001 | CAS connection disabled | Backend code shows disabled | May use alternate VIP check method |
| A002 | AN criteria same as standard | FD doesn't specify VIP-specific AN criteria | Follow OCMS 7 AN rules |
| A003 | TS-CLV period configurable | FD references configuration table | Period stored in OCMS_Suspension_Reason |
| A004 | Furnish validation same as standard | FD references OCMS 41 | Follow OCMS 41 validation rules |
| A005 | Edit vehicle type KIV | FD marks as post-MVP1 | Not implemented in current scope |

---

## 12. Changelog

| Version | Date | Author | Changes |
| --- | --- | --- | --- |
| 1.0 | 15/01/2026 | Claude | Initial version |

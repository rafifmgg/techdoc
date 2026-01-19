# Condition Plan: Diplomatic Vehicle Notice Processing

## Overview

| Attribute | Value |
| --- | --- |
| Feature Name | Notice Processing Flow for Diplomatic Vehicles |
| Version | v1.0 |
| Author | Claude |
| Created Date | 15/01/2026 |
| Last Updated | 15/01/2026 |
| Status | Draft |
| FD Reference | OCMS 14 - Section 6 |
| TD Reference | OCMS 14 - Section 5 |

---

## 1. Reference Documents

| Document Type | Reference | Description |
| --- | --- | --- |
| Functional Document | OCMS 14 Section 6 | Notice Processing Flow for Diplomatic Vehicles |
| Technical Document | OCMS 14 Section 5 | Technical Documentation |
| Data Dictionary | intranet.json | Database table definitions |
| Related FD | OCMS 10 | Advisory Notice Qualification |
| Related FD | OCMS 21 | Double Booking Function |

---

## 2. Business Conditions

### 2.1 Vehicle Registration Type Detection

**Description:** Determine if vehicle is Diplomatic type based on registration number format.

**Reference:** Functional Document Section 2.3, Step 5

#### Condition Matrix

| Condition ID | Condition Name | Input Fields | Logic | Output/Action |
| --- | --- | --- | --- | --- |
| C001 | Check Diplomatic Format | vehicle_no | Prefix = 'S' AND Suffix IN ('CC','CD','TC','TE') | Return 'D' |
| C002 | Not Diplomatic | vehicle_no | Does not match C001 | Check Military (Step 6) |

#### Condition Details

**C001: Diplomatic Vehicle Check**

| Attribute | Value |
| --- | --- |
| Description | Check if vehicle number matches Diplomatic format |
| Trigger | During vehicle registration type detection |
| Input | vehicle_no (String) |
| Logic | Prefix = 'S' AND Suffix IN ('CC', 'CD', 'TC', 'TE') |
| Output | Return vehicle_registration_type = 'D' |
| Else | Proceed to Military check (Step 6) |

```
IF vehicle_no.prefix = 'S'
   AND vehicle_no.suffix IN ('CC', 'CD', 'TC', 'TE')
THEN return 'D' (Diplomatic)
ELSE proceed to Military check
```

### 2.2 Offence Type Routing

**Description:** Route notice to appropriate workflow based on offence type.

**Reference:** Functional Document Section 6.2.1

#### Condition Matrix

| Condition ID | Condition Name | Input Fields | Logic | Output/Action |
| --- | --- | --- | --- | --- |
| C003 | Type O or E | offence_notice_type | Type IN ('O', 'E') | Process via DIP Type O&E workflow |
| C004 | Type U | offence_notice_type | Type = 'U' | Process via UPL workflow (KIV) |

#### Condition Details

**C003: Type O and E Processing**

| Attribute | Value |
| --- | --- |
| Description | Parking (O) and Payment Evasion (E) offences |
| Trigger | After vehicle type detection |
| Input | offence_notice_type |
| Logic | offence_notice_type IN ('O', 'E') |
| Output | Process via Diplomatic Vehicle workflow |
| Else | Check for Type U |

**C004: Type U Processing (KIV)**

| Attribute | Value |
| --- | --- |
| Description | Unauthorised Parking Lot offences |
| Trigger | After vehicle type detection |
| Input | offence_notice_type |
| Logic | offence_notice_type = 'U' |
| Output | Process via UPL workflow |
| Note | **KIV - Not implemented yet** |

### 2.3 Double Booking Detection

**Description:** Detect duplicate notices for same vehicle, date/time, and location.

**Reference:** Functional Document Section 6.2.2, Step 5

#### Condition Matrix

| Condition ID | Condition Name | Input Fields | Logic | Output/Action |
| --- | --- | --- | --- | --- |
| C005 | Duplicate Found | Multiple fields | All match criteria | Apply PS-DBB suspension |
| C006 | No Duplicate | Multiple fields | Not all match | Continue processing |

#### Condition Details

**C005: Double Booking Check**

| Attribute | Value |
| --- | --- |
| Description | Check for duplicate notices |
| Trigger | After notice creation at NPA |
| Input | vehicle_no, notice_date_and_time, computer_rule_code, parking_lot_no, pp_code |
| Logic | All fields match existing notice |
| Output | Apply PS-DBB suspension |
| Else | Continue to AN qualification check |

```
IF EXISTS notice WHERE
   vehicle_no = :new_vehicle_no
   AND notice_date_and_time = :new_notice_date_and_time
   AND computer_rule_code = :new_computer_rule_code
   AND (parking_lot_no = :new_parking_lot_no OR (parking_lot_no IS NULL AND :new_parking_lot_no IS NULL))
   AND pp_code = :new_pp_code
THEN apply PS-DBB
ELSE continue processing
```

### 2.4 Advisory Notice (AN) Qualification

**Description:** Determine if Type O notice qualifies for Advisory Notice processing.

**Reference:** Functional Document Section 6.2.4, OCMS 10

#### Condition Matrix

| Condition ID | Condition Name | Input Fields | Logic | Output/Action |
| --- | --- | --- | --- | --- |
| C007 | Qualifies for AN | offence_notice_type, AN criteria | Type O AND meets AN criteria | Process via AN sub-flow |
| C008 | Does Not Qualify | offence_notice_type, AN criteria | Does not meet criteria | Continue to ROV stage |

#### Condition Details

**C007: AN Qualification Check**

| Attribute | Value |
| --- | --- |
| Description | Check if notice qualifies for Advisory Notice |
| Trigger | After double booking check |
| Input | offence_notice_type, vehicle_history, offence_criteria |
| Logic | Refer to OCMS 10 Functional Document |
| Output | Set an_flag = 'Y', process via AN sub-flow, apply PS-ANS |
| Else | Continue to ROV stage |

### 2.5 PS-DIP Suspension Eligibility

**Description:** Determine if notice is eligible for PS-DIP suspension at end of RD2/DN2.

**Reference:** Functional Document Section 6.7

#### Condition Matrix

| Condition ID | Condition Name | Input Fields | Logic | Output/Action |
| --- | --- | --- | --- | --- |
| C009 | Eligible for PS-DIP | Multiple fields | All criteria met | Apply PS-DIP |
| C010 | Not Eligible | Multiple fields | Paid or suspended | Skip PS-DIP |

#### Condition Details

**C009: PS-DIP Eligibility**

| Attribute | Value |
| --- | --- |
| Description | Check if notice should receive PS-DIP |
| Trigger | CRON job at end of RD2/DN2 stage |
| Input | vehicle_registration_type, crs_reason_of_suspension, last_processing_stage, next_processing_stage |
| Logic | See below |
| Output | Apply PS-DIP suspension |
| Else | Do not apply PS-DIP |

```
IF vehicle_registration_type = 'D'
   AND crs_reason_of_suspension IS NULL
   AND last_processing_stage IN ('RD2', 'DN2')
   AND next_processing_stage IN ('RR3', 'DR3')
THEN apply PS-DIP
ELSE skip (notice is paid or already suspended)
```

---

## 3. Decision Trees

### 3.1 Diplomatic Vehicle Notice Processing Decision Flow

```
START
  │
  ├─► Is vehicle_registration_type = 'D'?
  │     │
  │     ├─ YES ─► Is offence_notice_type IN ('O', 'E')?
  │     │           │
  │     │           ├─ YES ─► Create Notice at NPA
  │     │           │           │
  │     │           │           ├─► Double Booking Detected?
  │     │           │           │     │
  │     │           │           │     ├─ YES ─► Apply PS-DBB ─► END
  │     │           │           │     │
  │     │           │           │     └─ NO ──► Qualifies for AN?
  │     │           │           │                 │
  │     │           │           │                 ├─ YES ─► AN Sub-flow ─► PS-ANS ─► END
  │     │           │           │                 │
  │     │           │           │                 └─ NO ──► ROV Stage (LTA/MHA/DH)
  │     │           │                                         │
  │     │           │                                         ├─► RD1 Stage
  │     │           │                                         │     │
  │     │           │                                         │     ├─► Payment Made? ─► PS-FP ─► END
  │     │           │                                         │     │
  │     │           │                                         │     ├─► Driver Furnished? ─► DN1 Flow
  │     │           │                                         │     │
  │     │           │                                         │     └─► Outstanding ─► RD2 Stage
  │     │           │                                         │                           │
  │     │           │                                         │                           ├─► Payment Made? ─► PS-FP ─► END
  │     │           │                                         │                           │
  │     │           │                                         │                           ├─► Driver Furnished? ─► DN2 Flow
  │     │           │                                         │                           │
  │     │           │                                         │                           └─► Outstanding at End of RD2?
  │     │           │                                         │                                 │
  │     │           │                                         │                                 └─ YES ─► Apply PS-DIP ─► END
  │     │           │
  │     │           └─ NO (Type U) ─► UPL Workflow (KIV) ─► END
  │     │
  │     └─ NO ──► Other Vehicle Type Processing ─► END
  │
END
```

### 3.2 PS-DIP Application Decision Table

| vehicle_reg_type | crs_reason | last_stage | next_stage | Action |
| --- | --- | --- | --- | --- |
| D | NULL | RD2 | RR3 | Apply PS-DIP |
| D | NULL | DN2 | DR3 | Apply PS-DIP |
| D | FP | RD2 | - | No Action (Paid) |
| D | PRA | RD2 | - | No Action (Paid) |
| D | NULL | RD1 | RD2 | No Action (Not at end) |
| I | NULL | RD2 | RR3 | Apply PS-MID (not DIP) |

### 3.3 Furnish Driver/Hirer Decision Table

| Notice Stage | Furnish Allowed | Notes |
| --- | --- | --- |
| NPA | No | Too early |
| ROV | Yes | After LTA check |
| RD1 | Yes | First reminder sent |
| RD2 | Yes | Second reminder sent |
| DN1 | Yes | Driver notice stage |
| DN2 | Yes | Driver notice stage |
| PS-DIP | **No** | Notice suspended |

---

## 4. Validation Rules

### 4.1 Field Validations

| Field Name | Data Type | Required | Validation Rule | Error Code | Error Message |
| --- | --- | --- | --- | --- | --- |
| vehicle_no | String | Yes | Not empty, valid format | VAL001 | Vehicle number is required |
| vehicle_registration_type | String | Yes | Must be 'D' for this flow | VAL002 | Invalid vehicle registration type |
| offence_notice_type | String | Yes | Must be 'O', 'E', or 'U' | VAL003 | Invalid offence notice type |
| notice_no | String | Yes | Unique, valid format | VAL004 | Invalid notice number |
| notice_date_and_time | DateTime | Yes | Valid date, not future | VAL005 | Invalid notice date/time |
| computer_rule_code | String | Yes | Valid rule code | VAL006 | Invalid rule code |

### 4.2 Cross-Field Validations

| Validation ID | Fields Involved | Rule | Error Code | Error Message |
| --- | --- | --- | --- | --- |
| XF001 | vehicle_registration_type, vehicle_no | If type='D', vehicle_no must match DIP format | XF001 | Vehicle number does not match Diplomatic format |
| XF002 | offence_notice_type, an_flag | If type='E', an_flag must be 'N' | XF002 | Type E cannot qualify for Advisory Notice |

### 4.3 Business Rule Validations

| Rule ID | Rule Name | Description | Condition | Error Code | Error Message |
| --- | --- | --- | --- | --- | --- |
| BR001 | PS-DIP Source Check | PLUS cannot apply PS-DIP | source = 'PLUS' AND ps_code = 'DIP' | BR001 | PLUS is not authorized to apply PS-DIP |
| BR002 | Furnish After PS-DIP | Cannot furnish after PS-DIP | suspension_type = 'PS' AND epr_reason = 'DIP' AND action = 'FURNISH' | BR002 | Notice cannot be furnished after PS-DIP suspension |
| BR003 | Payment After PS-DIP | Payment allowed after PS-DIP | suspension_type = 'PS' AND epr_reason = 'DIP' AND action = 'PAYMENT' | - | Allowed |

---

## 5. Status Transitions

### 5.1 Notice Processing Stage Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                                                             │
│  ┌──────────┐              ┌──────────┐              ┌──────────┐          │
│  │   NPA    │ ──────────►  │   ROV    │ ──────────►  │   RD1    │          │
│  └──────────┘   Next Day   └──────────┘   Next Day   └──────────┘          │
│       │         (LTA/MHA/    (Owner     (1st Letter)                        │
│       │          DataHive)    Check)                                        │
│       │                                      │                              │
│       │                                      │ Payment ──► PS-FP ──► END    │
│       │                                      │                              │
│       │                                      │ Furnish ──► DN1 ──► DN2     │
│       │                                      │                              │
│       │                                      ▼                              │
│       │                               ┌──────────┐                          │
│       │                               │   RD2    │                          │
│       │                               └──────────┘                          │
│       │                                    │                                │
│       │                                    │ Payment ──► PS-FP ──► END      │
│       │                                    │                                │
│       │                                    │ Furnish ──► DN2                │
│       │                                    │                                │
│       │                                    │ Outstanding                    │
│       │                                    ▼                                │
│       │                               ┌──────────┐                          │
│       │   PS-DBB ◄──── Double         │  PS-DIP  │ ◄──── End of RD2/DN2    │
│       │                Booking        └──────────┘       (Outstanding)      │
│       │                                    │                                │
│       │   PS-ANS ◄──── AN Qualified       │ Payment Still Allowed          │
│       │                                    │                                │
│                                            ▼                                │
│                                     Notice Processing                       │
│                                         Stops                               │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 Stage Transition Matrix

| From Stage | To Stage | Trigger | Condition | Allowed Actions |
| --- | --- | --- | --- | --- |
| NPA | ROV | CRON | End of day | Payment |
| NPA | PS-DBB | System | Double booking detected | - |
| NPA | PS-ANS | CRON | AN qualified | - |
| ROV | RD1 | CRON | End of day after LTA/MHA/DH | Payment, Furnish |
| RD1 | RD2 | CRON | End of RD1 duration | Payment, Furnish |
| RD1 | DN1 | User | Driver furnished | Payment |
| RD2 | PS-DIP | CRON | End of RD2, outstanding | Payment |
| RD2 | PS-FP | System | Payment received | - |
| DN1 | DN2 | CRON | End of DN1 duration | Payment |
| DN2 | PS-DIP | CRON | End of DN2, outstanding | Payment |

### 5.3 Suspension Type Definitions

| Suspension Type | Code | Description | Payable | Furnishable |
| --- | --- | --- | --- | --- |
| PS | DIP | Diplomatic Vehicle | Yes | No |
| PS | DBB | Double Booking | No | No |
| PS | ANS | Advisory Notice Suspension | No | No |
| PS | FP | Full Payment | No | No |
| TS | Various | Temporary Suspension | Varies | Varies |

---

## 6. Allowed Functions by Notice Condition

### 6.1 Function Availability Matrix

| Notice Condition | eNotification | Payment | Furnish Driver |
| --- | --- | --- | --- |
| NPA | No | Yes | No |
| ROV | No | Yes | Yes |
| RD1 | No | Yes | Yes |
| RD2 | No | Yes | Yes |
| DN1 | No | Yes | Yes |
| DN2 | No | Yes | Yes |
| PS-DIP | No | **Yes** | **No** |
| PS-FP | No | No | No |
| PS-DBB | No | No | No |
| PS-ANS | No | No | No |

---

## 7. Exit Conditions

### 7.1 Exit Condition Matrix

| No. | Exit Condition | Description | Final State |
| --- | --- | --- | --- |
| 1 | Notice Paid | Payment received successfully | PS-FP |
| 2 | Unpaid After X Years | Notice archived after X years | Archived |
| 3 | Permanently Suspended | Other PS code applied (APP, CFA, etc.) | PS-[code] |
| 4 | Double Booking | Duplicate notice detected | PS-DBB |
| 5 | Advisory Notice | AN qualification met | PS-ANS |

---

## 8. Exception Handling

### 8.1 Business Exceptions

| Exception Code | Exception Name | Condition | Handling |
| --- | --- | --- | --- |
| BEX001 | LTA Check Failed | LTA API returns error | Retry 3 times, log, continue with available data |
| BEX002 | MHA Check Failed | MHA API returns error | Retry 3 times, log, continue with LTA data |
| BEX003 | DataHive Check Failed | DataHive API returns error | Retry 3 times, log, continue with available data |
| BEX004 | Invalid Vehicle Format | Vehicle number doesn't match DIP format | Route to other vehicle type flow |
| BEX005 | Duplicate Notice | Double booking detected | Apply PS-DBB, stop processing |

### 8.2 System Exceptions

| Exception Code | Exception Name | Condition | Handling |
| --- | --- | --- | --- |
| SEX001 | Database Error | DB connection failed | Retry 3 times, then log and notify |
| SEX002 | SFTP Upload Failed | Letter file upload failed | Retry 3 times, send alert email |
| SEX003 | Timeout | Process exceeds timeout | Cancel and rollback, log error |

---

## 9. PS-DIP Specific Rules

### 9.1 Suspension Application Rules

| Rule | Value | Description |
| --- | --- | --- |
| Can be applied at stages | NPA, ROV, RD1, RD2, DN1, DN2 | Auto at end of RD2/DN2 |
| OCMS Staff can apply | Yes | Via Staff Portal |
| PLUS can apply | **No** | Not displayed in PLUS |
| OCMS Backend can apply | Yes | Via CRON job |

### 9.2 Post-Suspension Rules

| Rule | Value | Description |
| --- | --- | --- |
| Payment allowed | Yes | Via eService and AXS |
| Furnish allowed | **No** | Cannot furnish after PS-DIP |
| Additional TS allowed | Yes | TS can be applied on top |
| Additional PS allowed | Yes | PS can be applied on top |

### 9.3 Exception Code Behavior

PS-DIP is part of the 5 PS Exception Codes (DIP, FOR, MID, RIP, RP2):

| Behavior | Standard PS | Exception PS (DIP) |
| --- | --- | --- |
| Allow TS on top | No | **Yes** |
| Allow FP/PRA without revival | No | **Yes** |
| Allow PS stacking | No | **Yes** |

---

## 10. Test Scenarios

### 10.1 Condition Test Cases

| Test ID | Condition | Test Input | Expected Output | Status |
| --- | --- | --- | --- | --- |
| TC001 | C001 - DIP Detection TRUE | vehicle_no = 'SCC1234A' | type = 'D' | - |
| TC002 | C001 - DIP Detection FALSE | vehicle_no = 'SBA1234A' | type != 'D' | - |
| TC003 | C003 - Type O Processing | offence_type = 'O', veh_type = 'D' | DIP workflow | - |
| TC004 | C004 - Type U Processing | offence_type = 'U', veh_type = 'D' | UPL workflow | - |
| TC005 | C005 - Double Booking | Duplicate notice exists | PS-DBB applied | - |
| TC006 | C009 - PS-DIP Eligible | Outstanding at RD2 | PS-DIP applied | - |
| TC007 | C010 - PS-DIP Not Eligible | Paid at RD2 | No PS-DIP | - |

### 10.2 Edge Cases

| Test ID | Scenario | Test Input | Expected Output |
| --- | --- | --- | --- |
| EC001 | LTA returns no data | Valid DIP notice | Continue with default address |
| EC002 | MHA timeout | Valid DIP notice | Retry 3 times, then continue |
| EC003 | Payment at PS-DIP | PS-DIP suspended notice | Payment allowed, apply PS-FP |
| EC004 | Furnish at PS-DIP | PS-DIP suspended notice | Furnish rejected |
| EC005 | PLUS tries PS-DIP | Source = PLUS | Error - not authorized |

---

## 11. Changelog

| Version | Date | Author | Changes |
| --- | --- | --- | --- |
| 1.0 | 15/01/2026 | Claude | Initial version |

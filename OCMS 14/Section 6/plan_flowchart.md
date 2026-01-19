# Flowchart Plan: OCMS 14 Section 6 - VIP Vehicle Notice Processing

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
| plan_api.md | Section 6/plan_api.md |
| plan_condition.md | Section 6/plan_condition.md |

---

## 1. Flowchart Tabs Structure

Based on Functional Document Section 7, the following tabs will be created:

| Tab # | Tab Name | Type | Description |
| --- | --- | --- | --- |
| 1 | VIP_High_Level | High-Level | Overview of VIP notice processing |
| 2 | VIP_Type_O_E | Detailed | Type O & E processing flow (20 steps) |
| 3 | VIP_Advisory_Notice | Sub-flow | AN qualification and letter generation |
| 4 | VIP_Furnish_Driver | Sub-flow | Furnish driver/hirer flow |
| 5 | VIP_TS_CLV_Cron | Cron | TS-CLV suspension cron job |
| 6 | VIP_Classified_Report | Cron | Daily report generation |

---

## 2. Swimlane Definitions

| Swimlane | Color Code | Description |
| --- | --- | --- |
| Staff Portal / PLUS Portal | #dae8fc (light blue) | Frontend user interface |
| eService Portal | #dae8fc (light blue) | Public portal for vehicle owners |
| OCMS Admin API Intranet | #d5e8d4 (light green) | Backend API services |
| OCMS Cron Service | #d5e8d4 (light green) | Scheduled batch jobs |
| Database (Intranet) | #fff2cc (light yellow) | Intranet zone database |
| Database (Internet) | #fff2cc (light yellow) | Internet zone database |
| External System | #f8cecc (light red) | LTA, MHA, DataHive, CAS/FOMS |

---

## 3. Tab 1: VIP_High_Level

### Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | VIP Vehicle Notice High-Level Processing Flow |
| Section | 7.2.1 |
| Trigger | Raw offence data received with vehicle_registration_type = 'V' |
| Frequency | Real-time (on notice creation) |
| Systems Involved | OCMS Admin API, Database, CAS/FOMS |

### Process Steps Table

| Step | Swimlane | Type | Definition | Description |
| --- | --- | --- | --- | --- |
| 1 | External | Start | Raw offence data received | Data from REPCCS, CES-EHT, EEPS, PLUS, OCMS Staff Portal |
| 2 | Backend | Process | Validate data | Validate format, structure, mandatory fields |
| 3 | Backend | Process | Check duplicate Notice Number | Compare against existing notices |
| 4 | Backend | Decision | Identify Offence Type | Type O, E, or U? |
| 5a | Backend | Process | Type O & E Workflow | Process via VIP Type O&E flow |
| 5b | Backend | Process | Type U Workflow | Process via standard UPL flow (KIV) |
| 6 | Backend | Decision | Vehicle reg type = 'V'? | Check if VIP vehicle |
| 7a | Backend | Process | Apply TS-OLD | 21-day suspension for OIC investigation |
| 7b | Backend | Process | Standard flow | Continue standard Type O/E flow |
| 8 | Backend | Process | OIC manually revives OR auto revival | After investigation or expiry |
| 9 | Backend | Process | Process through RD1 → RD2 → RR3 | Stage transitions with MHA/DH checks |
| 10 | Backend | Decision | Notice outstanding at RR3/DR3? | Check if unpaid |
| 11a | Backend | Process | Apply TS-CLV (looping) | At CPC stage (RR3/DR3 for MVP1) |
| 11b | Backend | Process | Apply PS-FP | If payment made |
| 12 | Backend | Decision | OIC manually revives CLV? | Check for manual revival |
| 13a | Backend | Process | Auto re-apply TS-CLV | If expired and type still 'V' |
| 13b | Backend | Process | Exit loop, continue standard flow | If type changed V→S |
| 14 | Backend | End | Processing complete | Notice exits VIP flow |

### Decision Logic

| Decision | Input | Condition | True Action | False Action |
| --- | --- | --- | --- | --- |
| Offence Type? | computer_rule_code | Type O or E | VIP Type O&E flow | Type U → UPL flow |
| Vehicle reg type = 'V'? | vehicle_registration_type | = 'V' | Apply TS-OLD | Standard flow |
| Outstanding at RR3/DR3? | payment_status | Not paid | Apply TS-CLV | Apply PS-FP |
| OIC manually revives? | oic_action | Manual revival | Check type change | Auto re-apply CLV |

---

## 4. Tab 2: VIP_Type_O_E

### Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | VIP Vehicle Notice Processing (Type O & E) |
| Section | 7.2.2 |
| Trigger | Notice created with vehicle_registration_type = 'V', Type O or E |
| Frequency | Real-time |
| Systems Involved | OCMS Admin API, Database, LTA, MHA, DataHive, SFTP |

### Process Steps Table

| Step | Swimlane | Type | Definition | Description |
| --- | --- | --- | --- | --- |
| 1 | Backend | Start | Start | Process begins for Type O or E offences |
| 2 | Backend | Process | Duplicate Notice Details Check | Compare vehicle_no, offence_date, rule_code, pp_code, lot_no |
| 3 | Backend | Process | Detect Vehicle Registration Type | Identify type = 'V' (VIP) |
| 4 | Database | Process | Notice Created at NPA | Insert into ocms_valid_offence_notice |
| 5 | Backend | Process | Check for Double Booking | Detect duplicates, apply PS-DBB if found |
| 6 | Backend | Decision | AN Qualification Check | Type O AND meets AN criteria? |
| 6a | Backend | Sub-flow | VIP AN Sub-flow | Process via Section 7.2.4 |
| 7 | Database | Process | Suspend Notice with TS-OLD | Apply 21-day suspension |
| 8 | Backend | Decision | TS-OLD revived? | Manual or auto revival |
| 9 | External | Process | LTA vehicle ownership check | Query LTA VRLS |
| 10 | Backend | Process | Check Vehicle type after LTA at ROV | Re-verify VIP status |
| 11 | Backend | Process | Prepare RD1 for MHA/DH checks | Submit to MHA and DataHive |
| 12 | Database | Process | Prepare for RD1 | Generate letter, update stage |
| 13 | Backend | Process | Notice at RD1 Stage | Payment and furnish available |
| 14 | Backend | Process | Prepare RD2 for MHA/DH checks | Submit to MHA and DataHive |
| 15 | Database | Process | Prepare for RD2 | Generate letter, update stage |
| 16 | Backend | Process | Notice at RD2 Stage | Payment and furnish available |
| 17 | Backend | Process | Prepare RR3 for MHA/DH checks | Submit to MHA and DataHive |
| 18 | Database | Process | Prepare for RR3 | Generate letter, update stage |
| 19 | Backend | Process | Notice at RR3 Stage | Payment only |
| 20 | Backend | Decision | Notice outstanding at end of RR3? | Check payment status |
| 20a | Database | Process | Apply TS-CLV at CPC | Looping suspension |
| 20b | Backend | End | End | Payment made, PS-FP applied |

### Database Operations

| Step | Operation | Table | Fields |
| --- | --- | --- | --- |
| 4 | INSERT | ocms_valid_offence_notice | All notice fields, vehicle_registration_type='V' |
| 7 | UPDATE | ocms_valid_offence_notice | suspension_type='TS', epr_reason_of_suspension='OLD' |
| 7 | INSERT | ocms_suspended_notice | New suspension record |
| 12,15,18 | UPDATE | ocms_valid_offence_notice | last_processing_stage, next_processing_stage |
| 20a | UPDATE | ocms_valid_offence_notice | suspension_type='TS', epr_reason_of_suspension='CLV' |
| 20a | INSERT | ocms_suspended_notice | TS-CLV suspension record |

### API Payload Box (Step 9 - LTA Check)

```
LTA VRLS Request:
{
  "vehicleNo": "SBA1234A",
  "requestType": "OWNERSHIP"
}

LTA VRLS Response:
{
  "vehicleNo": "SBA1234A",
  "ownerName": "JOHN DOE",
  "ownerIdType": "NRIC",
  "ownerIdNo": "S1234567A"
}
```

### API Payload Box (Step 11 - MHA Check)

```
MHA Request:
{
  "idType": "NRIC",
  "idNumber": "S1234567A"
}

MHA Response:
{
  "idNumber": "S1234567A",
  "name": "JOHN DOE",
  "address": {...},
  "isDeceased": false
}
```

---

## 5. Tab 3: VIP_Advisory_Notice

### Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | VIP Vehicle Advisory Notice Sub-flow |
| Section | 7.2.4 |
| Trigger | Type O notice qualifies for AN |
| Frequency | Real-time + Daily cron |
| Systems Involved | OCMS Admin API, OCMS Cron, Database, SFTP |

### Process Steps Table

| Step | Swimlane | Type | Definition | Description |
| --- | --- | --- | --- | --- |
| 1 | Backend | Start | Start | Type O offence classified |
| 2 | Backend | Process | Duplicate Notice Details Check | Same as main flow |
| 3 | Backend | Process | Detect Vehicle Registration Type | Type = 'V' |
| 4 | Database | Process | Notice Created at NPA | Insert notice |
| 5 | Backend | Process | Check for Double Booking | Apply PS-DBB if found |
| 6 | Backend | Decision | AN Qualification Check | Meets AN criteria? |
| 6-No | Backend | Process | Standard VIP flow | Continue Type O&E flow |
| 7 | Database | Process | Update AN_Flag = 'Y' | Set next_processing_stage = 'RD1' |
| 8 | Cron | Start | AN Letter Generation Cron | Daily scheduled job |
| 9 | Database | Process | Query for Notices | vehicle_reg_type='V', an_flag='Y', stage='NPA', next_stage='ROV' |
| 10 | Backend | Process | Generate AN Letter PDF | Create PDF for each notice |
| 11 | External | Process | Drop Letters into SFTP | Send to printing vendor |
| 12 | Database | Process | Update Notice Stage | last_processing_stage='RD1', next='RD2' |
| 13 | Database | Process | Suspend Notices with PS-ANS | Apply PS-ANS after sending |
| 14 | Backend | End | End | AN flow complete |

### Query Condition (Step 9)

```sql
SELECT notice_no, vehicle_no, ...
FROM ocms_valid_offence_notice
WHERE vehicle_registration_type = 'V'
  AND an_flag = 'Y'
  AND last_processing_stage = 'NPA'
  AND next_processing_stage = 'ROV'
  AND next_processing_date <= CURRENT_DATE
  AND suspension_type IS NULL
  AND epr_reason_of_suspension IS NULL
```

---

## 6. Tab 4: VIP_Furnish_Driver

### Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | Furnish Driver/Hirer for VIP Vehicle Notices |
| Section | 7.2.5 |
| Trigger | Vehicle owner submits furnish application via eService |
| Frequency | Real-time |
| Systems Involved | eService Portal, OCMS Admin API, Database (Internet & Intranet) |

### Process Steps Table

| Step | Swimlane | Type | Definition | Description |
| --- | --- | --- | --- | --- |
| 1 | eService | Start | Start | Owner accesses eService portal |
| 2 | eService | Process | Furnish Driver/Hirer & Submit | Submit driver/hirer particulars |
| 3 | Database (Internet) | Process | Update Notice & Driver/Hirer Info | Insert into eocms_furnish_application |
| 4 | Cron | Process | Sync Driver/Hirer Info to Intranet | Scheduled sync job |
| 5 | Backend | Process | Validate Driver/Hirer Particulars | Check against MHA, format validation |
| 6 | Backend | Decision | Auto approve criteria met? | Validation passed? |
| 6a | Backend | Process | OIC Reviews Driver/Hirer Particulars | Manual review required |
| 7 | Backend | Decision | OIC Approves? | Manual decision |
| 7a | Database | Process | Update Internet Notice for rejection | Remove furnish pending status |
| 8 | Database | Process | Auto Approve Driver/Hirer | Update furnish status |
| 9 | Database | Process | Driver/Hirer Particulars Added | Create new record in owner_driver table |
| 10 | Backend | Decision | Driver or Hirer? | Check furnish type |
| 10a | Database | Process | Change Next Stage to DN1 | For driver |
| 10b | Database | Process | Change Next Stage to RD1 | For hirer |
| 11 | Cron | Process | Prepare for DN1/RD1 | Generate letter, update stage |
| 12 | Backend | Process | Notice at DN1/RD1 Stage | Continue processing |
| 13-19 | Backend | Process | Continue through DN2/RD2 → DR3/RR3 | Same as main flow |
| 19 | Backend | Decision | Outstanding at DR3/RR3? | Check payment |
| 19a | Database | Process | Apply TS-CLV at CPC | Looping suspension |
| 20 | Backend | End | End | Processing complete |

### Database Operations

| Step | Operation | Table | Zone | Description |
| --- | --- | --- | --- | --- |
| 3 | INSERT | eocms_furnish_application | Internet | Store furnish request |
| 4 | INSERT | ocms_furnish_application | Intranet | Sync from internet |
| 7a | UPDATE | eocms_furnish_application | Internet | Set status rejected |
| 9 | INSERT | ocms_offence_notice_owner_driver | Intranet | Add driver/hirer record |
| 10a/10b | UPDATE | ocms_valid_offence_notice | Intranet | Update next_processing_stage |

### API Payload Box (Step 2 - Furnish Request)

```
Furnish Request:
{
  "noticeNo": "500500303J",
  "furnishType": "DRIVER",
  "particulars": {
    "idType": "NRIC",
    "idNumber": "S1234567A",
    "name": "JOHN DOE",
    "contactNo": "91234567"
  }
}

Furnish Response:
{
  "success": true,
  "applicationId": "FA20260115001",
  "status": "PENDING_APPROVAL"
}
```

---

## 7. Tab 5: VIP_TS_CLV_Cron

### Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | Suspend VIP Notices with TS-CLV Cron |
| Section | 7.5 |
| Trigger | Daily scheduled job at EOD |
| Frequency | Daily |
| Systems Involved | OCMS Cron Service, Database |

### Process Steps Table

| Step | Swimlane | Type | Definition | Description |
| --- | --- | --- | --- | --- |
| 1 | Cron | Start | CRON - Suspend Notice | Scheduled job starts |
| 2 | Database | Process | Query Valid Offence Notice Table | Find eligible VIP notices |
| 3 | Cron | Decision | Records Found? | Any notices to suspend? |
| 3-No | Cron | End | End | No records, terminate |
| 4 | Database | Process | Move Notices into CPC stage | Update last_processing_stage |
| 4 | Database | Process | Suspend Notice with TS-CLV | Apply suspension |
| 5 | Database | Process | Patch Notices details | Update all stage fields |
| 6 | Database | Process | Create new suspension record | Insert into ocms_suspended_notice |
| 7 | Cron | End | End | Processing complete |

### Query Condition (Step 2)

```sql
SELECT notice_no
FROM ocms_valid_offence_notice
WHERE vehicle_registration_type = 'V'
  AND last_processing_stage IN ('RR3', 'DR3')
  AND next_processing_stage = 'CPC'
  AND next_processing_date <= CURRENT_DATE
  AND (suspension_type IS NULL OR suspension_type = 'TS')
```

### Database Updates (Step 5)

```sql
UPDATE ocms_valid_offence_notice
SET last_processing_stage = 'CPC',
    last_processing_date = CURRENT_TIMESTAMP,
    next_processing_stage = 'CSD',
    next_processing_date = NULL,
    prev_processing_stage = 'RR3', -- or 'DR3'
    prev_processing_date = {previous_date},
    suspension_type = 'TS',
    epr_reason_of_suspension = 'CLV',
    epr_date_of_suspension = CURRENT_TIMESTAMP,
    due_date_of_revival = CURRENT_DATE + {suspension_period}
WHERE notice_no = :notice_no
```

### Additional Cron: Auto Re-apply TS-CLV

| Step | Swimlane | Type | Definition | Description |
| --- | --- | --- | --- | --- |
| 1 | Cron | Start | Auto Re-apply TS-CLV Cron | Daily at 00:00 |
| 2 | Database | Process | Query expired TS-CLV | Find VIP notices with expired CLV |
| 3 | Cron | Decision | Records Found? | Any to re-apply? |
| 4 | Database | Process | Create revival record | Auto revival entry |
| 5 | Database | Process | Re-apply TS-CLV | New suspension with new due date |
| 6 | Database | Process | Create new suspension record | Insert new TS-CLV |
| 7 | Cron | End | End | Loop continues |

### Query Condition (Auto Re-apply)

```sql
SELECT notice_no
FROM ocms_valid_offence_notice
WHERE vehicle_registration_type = 'V'
  AND last_processing_stage = 'CPC'
  AND suspension_type = 'TS'
  AND epr_reason_of_suspension = 'CLV'
  AND due_date_of_revival <= CURRENT_DATE
```

---

## 8. Tab 6: VIP_Classified_Report

### Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | Generate Classified Vehicle Notices Report |
| Section | 7.9.1 |
| Trigger | Daily scheduled cron job |
| Frequency | Daily (time TBD) |
| Systems Involved | OCMS Cron Service, Database, CAS, Email Service |

### Process Steps Table

| Step | Swimlane | Type | Definition | Description |
| --- | --- | --- | --- | --- |
| 1 | Cron | Start | Start | Cron job initiated |
| 2 | External (CAS) | Process | Retrieve Active VIP Vehicles from CAS | Query VIP_VEHICLE table |
| 3 | Cron | Decision | Any Active VIP Vehicles Found? | Check if records returned |
| 3-No | Cron | Process | Generate empty report | No active VIP vehicles |
| 4 | Cron | Process | Extract Vehicle Numbers | Get list of vehicle_no |
| 5 | Database | Process | Retrieve Notice Details from OCMS | Query valid_offence_notice |
| 6 | Cron | Process | Store VIP Notice Details | Temporary data store |
| 7 | Cron | Process | Extract Notice Numbers | Get list of notice_no |
| 8 | Database | Process | Retrieve Current Offender Details | Query owner_driver table |
| 9 | Cron | Process | Store Current Offender Details | Temporary data store |
| 10 | Cron | Process | Calculate Outstanding Notice Count | Count where crs_reason_of_suspension IS NULL |
| 11 | Cron | Process | Calculate Settled/Paid Notice Count | Total - Outstanding |
| 12 | Cron | Process | Retrieve Data stored in Temp | Prepare for report |
| 13 | Cron | Process | Format Report Data | Structure data for Excel |
| 14 | Cron | Process | Generate Report | Create Excel with 3 sheets |
| 15 | Cron | Process | Attach Report to Email | Prepare email with attachment |
| 16 | External | Process | Send Email | Send to OICs |
| 17 | Database | Process | Update Batch Job Status | Log completion |
| 18 | Cron | End | End | Report generation complete |

### Query Conditions

**Step 2 - Active VIP Vehicles:**
```sql
SELECT vehicle_no, vip_label_status, label_expiry_date
FROM vip_vehicle
WHERE status = 'ACTIVE'
```

**Step 5 - Notice Details:**
```sql
SELECT von.*, pp.pp_name
FROM ocms_valid_offence_notice von
LEFT JOIN ocms_carpark pp ON von.pp_code = pp.pp_code
WHERE von.vehicle_no IN (:vip_vehicle_numbers)
```

**Step 8 - Offender Details:**
```sql
SELECT *
FROM ocms_offence_notice_owner_driver
WHERE notice_no IN (:notice_numbers)
  AND is_current = 'Y'
```

**Amended Notices (V→S):**
```sql
SELECT von.*, sn.date_of_suspension, sn.date_of_revival
FROM ocms_valid_offence_notice von
JOIN ocms_suspended_notice sn ON von.notice_no = sn.notice_no
WHERE sn.reason_of_suspension = 'CLV'
  AND sn.date_of_revival IS NOT NULL
  AND von.vehicle_registration_type = 'S'
```

### Report Output Structure

**Sheet 1: Summary**
| Field | Value |
| --- | --- |
| Report Date | {current_date} |
| Total Notices Issued (Type V) | {count} |
| Outstanding Notices (Unpaid) | {count} |
| Settled Notices (Paid) | {count} |
| Notices Amended (V→S) | {count} |

**Sheet 2: Type V Notices Detail**
| Column | Source |
| --- | --- |
| S/N | Row number |
| Notice No | notice_no |
| Vehicle No | vehicle_no |
| Notice Date | notice_date_and_time |
| Offence Code | computer_rule_code |
| Place of Offence | pp_name |
| Composition Amount ($) | composition_amount |
| Amount Payable ($) | amount_payable |
| Payment Status | Derived (Outstanding/Settled) |
| Suspension Type | suspension_type |
| Suspension Reason | epr_reason_of_suspension |

**Sheet 3: Amended Notices (V→S)**
| Column | Source |
| --- | --- |
| S/N | Row number |
| Notice No | notice_no |
| Vehicle No | vehicle_no |
| Notice Date | notice_date_and_time |
| Original Type | 'V (VIP)' |
| Amended Type | 'S (Singapore)' |
| Suspension Date (TS-CLV) | date_of_suspension |
| Revival Date (TS-CLV) | date_of_revival |
| Place of Offence | pp_name |
| Composition Amount ($) | composition_amount |
| Amount Payable ($) | amount_payable |

---

## 9. Error Handling Paths

### Common Error Scenarios

| Flow | Error Point | Error Type | Handling Path |
| --- | --- | --- | --- |
| VIP Type O&E | LTA Check | Timeout | Retry 3x → Log error → Continue with existing data |
| VIP Type O&E | MHA Check | Unavailable | Proceed without update → Retry in next batch |
| VIP Furnish | Validation | Invalid data | Return error → Allow re-submission |
| TS-CLV Cron | DB Update | Failed | Log error → Skip notice → Continue batch |
| Report | CAS Query | Failed | Generate report with warning → Notify admin |
| Report | Email Send | Failed | Retry → Log for manual send |

---

## 10. Flowchart Checklist

Before creating .drawio files:

- [x] All tabs identified and named
- [x] All steps have clear definitions
- [x] All decision points have Yes/No paths
- [x] All paths lead to End point
- [x] Error handling paths included
- [x] Database operations identified with dashed lines
- [x] Swimlanes defined for each system/tier
- [x] API payloads documented
- [x] Query conditions documented
- [x] Color coding defined

---

## 11. Technical Document Mapping

| Flowchart Tab | Tech Doc Section | Content |
| --- | --- | --- |
| VIP_High_Level | Overview | High-level process description |
| VIP_Type_O_E | Main Flow | Detailed step-by-step with data mapping |
| VIP_Advisory_Notice | Sub-flow | AN processing with queries |
| VIP_Furnish_Driver | Sub-flow | Furnish flow with validation rules |
| VIP_TS_CLV_Cron | Cron Jobs | Suspension logic and looping |
| VIP_Classified_Report | Reports | Report generation and distribution |

---

## 12. Changelog

| Version | Date | Author | Changes |
| --- | --- | --- | --- |
| 1.0 | 15/01/2026 | Claude | Initial version |

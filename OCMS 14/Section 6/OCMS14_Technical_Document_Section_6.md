# OCMS 14 – VIP Vehicle Notice Processing

**Prepared by**

[COMPANY LOGO]

[COMPANY NAME]

---

## Version History

| Version | Updated By | Date | Changes |
| --- | --- | --- | --- |
| v1.0 | [Author Name] | 16/01/2026 | Document Initiation |
| v1.1 | Claude | 19/01/2026 | Fixed Critical Issues: Replaced SYSTEM with ocmsiz_app_conn in all SQL statements |
| v1.2 | Claude | 19/01/2026 | Added Appendix A (Sync Flag), Appendix E (Eligibility by Source) |
| v1.3 | Claude | 19/01/2026 | Updated furnish field names to match Data Dictionary (txn_ref_no, furnish_id_no, etc.) |

---

## Table of Content

| Section | Content | Pages |
| --- | --- | --- |
| 1 | VIP Vehicle Notice Processing (Type O & E) | [X] |
| 1.1 | Use Case | [X] |
| 1.2 | High Level Flow | [X] |
| 1.3 | Type O & E Processing Flow | [X] |
| 1.3.1 | API Specification | [X] |
| 1.3.2 | Data Mapping | [X] |
| 1.3.3 | Database Operations | [X] |
| 1.3.4 | Success Outcome | [X] |
| 1.3.5 | Error Handling | [X] |
| 2 | Advisory Notice Processing | [X] |
| 2.1 | Use Case | [X] |
| 2.2 | Advisory Notice Flow | [X] |
| 2.2.1 | Data Mapping | [X] |
| 2.2.2 | Database Operations | [X] |
| 2.2.3 | Success Outcome | [X] |
| 2.2.4 | Error Handling | [X] |
| 3 | Furnish Driver/Hirer | [X] |
| 3.1 | Use Case | [X] |
| 3.2 | Furnish Driver/Hirer Flow | [X] |
| 3.2.1 | API Specification | [X] |
| 3.2.2 | Data Mapping | [X] |
| 3.2.3 | Database Operations | [X] |
| 3.2.4 | Success Outcome | [X] |
| 3.2.5 | Error Handling | [X] |
| 4 | TS-CLV Suspension Processing | [X] |
| 4.1 | Use Case | [X] |
| 4.2 | TS-CLV Suspension Cron Flow | [X] |
| 4.2.1 | Data Mapping | [X] |
| 4.2.2 | Database Operations | [X] |
| 4.2.3 | Success Outcome | [X] |
| 4.2.4 | Error Handling | [X] |
| 5 | Classified Vehicle Notices Report | [X] |
| 5.1 | Use Case | [X] |
| 5.2 | Report Generation Flow | [X] |
| 5.2.1 | Data Mapping | [X] |
| 5.2.2 | Database Operations | [X] |
| 5.2.3 | Success Outcome | [X] |
| 5.2.4 | Error Handling | [X] |
| A | Sync Flag Mechanism | [X] |
| B | Suspension Type Reference | [X] |
| C | Stage Transition Reference | [X] |
| D | Allowed Functions Matrix | [X] |
| E | Eligibility Scenarios by Source | [X] |

---

# Section 1 – VIP Vehicle Notice Processing (Type O & E)

## 1.1 Use Case

1. The OCMS system identifies VIP vehicles based on the Vehicle Registration Type = 'V', which indicates the vehicle has a valid VIP Parking Label registered in the CAS/FOMS system.

2. When a notice is created for a VIP vehicle with offence Type O (Parking Offence) or Type E (Payment Evasion):<br>a. The system applies TS-OLD (Temporary Suspension - OIC Investigation) for 21 days<br>b. OIC has the opportunity to investigate the notice during the suspension period<br>c. After TS-OLD revival (manual or auto), the notice proceeds through RD1 → RD2 → RR3 stages

3. VIP notices remain payable throughout the processing flow, including during TS-OLD and TS-CLV suspensions.

4. At the end of RR3/DR3 stage (MVP1) or CPC stage (Post-MVP1), if the notice is still outstanding, the system applies TS-CLV (Temporary Suspension - Classified Vehicles), which is a looping suspension that auto re-applies until:<br>a. The notice is fully paid, OR<br>b. OIC manually revives and changes the vehicle type from 'V' to 'S'

## 1.2 High Level Flow

<!-- Insert flow diagram here -->
![High Level Flow](./images/section1-high-level-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Raw Offence Data Received | System receives raw offence data from various sources (REPCCS, CES-EHT, EEPS, PLUS Portal, OCMS Staff Portal) | Data intake |
| Validate Data | System validates data format, structure, and mandatory fields | Data validation |
| Check Duplicate Notice Number | Compare against existing notices to prevent duplicates | Duplicate check |
| Identify Offence Type | Classify notice as Type O, E, or U | Type classification |
| Type O & E Workflow | Process VIP notices through Type O & E specific workflow | VIP processing |
| Vehicle Registration Type Check | Determine if vehicle_registration_type = 'V' | VIP detection |
| Apply TS-OLD | Apply 21-day temporary suspension for OIC investigation | Suspension applied |
| OIC Revival or Auto Revival | OIC manually revives or system auto-revives after 21 days | Suspension revived |
| Process RD1 → RD2 → RR3 | Stage transitions with MHA/DataHive checks and letter generation | Stage processing |
| Outstanding Check | Determine if notice is still outstanding at end of RR3/DR3 | Payment check |
| Apply TS-CLV | Apply looping suspension at CPC stage | CLV suspension |
| TS-CLV Looping | Auto re-apply TS-CLV when expired until type changed or paid | Looping logic |

## 1.3 Type O & E Processing Flow

<!-- Insert flow diagram here -->
![Type O & E Processing Flow](./images/section1-type-oe-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | Process begins for Type O or E offences | Entry point |
| Duplicate Notice Details Check | Compare vehicle_no, offence_date, rule_code, pp_code, lot_no against existing notices | Duplicate validation |
| Detect Vehicle Registration Type | System identifies vehicle_registration_type = 'V' from CAS/FOMS VIP_VEHICLE table | VIP detection |
| Notice Created at NPA | Insert notice record into database with status at NPA stage | Notice creation |
| Check for Double Booking | Detect duplicate notices for same offence, apply PS-DBB if found | DBB check |
| AN Qualification Check | Check if Type O notice qualifies for Advisory Notice processing | AN eligibility |
| Suspend Notice with TS-OLD | Apply 21-day temporary suspension for OIC investigation | TS-OLD applied |
| TS-OLD Revival Check | Determine if OIC manually revived or suspension expired | Revival check |
| LTA Vehicle Ownership Check | Query LTA VRLS for vehicle ownership details | LTA integration |
| Re-check Vehicle Type at ROV | Verify vehicle is still VIP after LTA response | VIP re-verification |
| Prepare RD1 for MHA/DH Checks | Submit offender NRIC/FIN to MHA and DataHive for validation | MHA/DH check |
| Prepare for RD1 | Generate RD1 reminder letter, update stage to RD1 | Stage update |
| Notice at RD1 Stage | Notice available for payment and furnish (owner only) | RD1 stage |
| Prepare RD2 for MHA/DH Checks | Submit to MHA and DataHive before RD2 transition | MHA/DH check |
| Prepare for RD2 | Generate RD2 reminder letter, update stage to RD2 | Stage update |
| Notice at RD2 Stage | Notice available for payment and furnish (owner only) | RD2 stage |
| Prepare RR3 for MHA/DH Checks | Submit to MHA and DataHive before RR3 transition | MHA/DH check |
| Prepare for RR3 | Generate RR3 final reminder letter, update stage to RR3 | Stage update |
| Notice at RR3 Stage | Notice available for payment only | RR3 stage |
| Outstanding Check at End of RR3 | Determine if notice is still unpaid | Payment status |
| Apply TS-CLV at CPC | Move to CPC stage and apply looping TS-CLV suspension | TS-CLV applied |
| End | Notice payment made, PS-FP applied, processing stops | Exit point |

### 1.3.1 API Specification

#### API Consume

##### LTA VRLS - Vehicle Ownership Check

| Field | Value |
| --- | --- |
| API Name | lta-vrls-ownership |
| URL | External LTA VRLS endpoint |
| Description | Retrieve vehicle ownership details from LTA |
| Method | POST |
| Header | `{ "Authorization": "Bearer [token]", "Content-Type": "application/json" }` |
| Payload | `{ "vehicleNo": "SBA1234A", "requestType": "OWNERSHIP" }` |
| Response | `{ "vehicleNo": "SBA1234A", "ownerName": "JOHN DOE", "ownerIdType": "NRIC", "ownerIdNo": "S1234567A", "registrationDate": "2020-01-15", "vehicleType": "CAR" }` |
| Response Failure | `{ "appCode": "LTA-5000", "message": "Service unavailable" }` |

##### MHA - Offender Particulars Validation

| Field | Value |
| --- | --- |
| API Name | mha-particulars-validation |
| URL | External MHA endpoint |
| Description | Validate and retrieve offender particulars |
| Method | POST |
| Header | `{ "Authorization": "Bearer [token]", "Content-Type": "application/json" }` |
| Payload | `{ "idType": "NRIC", "idNumber": "S1234567A" }` |
| Response | `{ "idNumber": "S1234567A", "name": "JOHN DOE", "dateOfBirth": "1980-01-15", "nationality": "SINGAPOREAN", "address": { "block": "123", "street": "ORCHARD ROAD", "unit": "#01-01", "postalCode": "238888" }, "isDeceased": false }` |
| Response Failure | `{ "appCode": "MHA-5000", "message": "Service unavailable" }` |

##### DataHive - Address Enrichment

| Field | Value |
| --- | --- |
| API Name | datahive-address-enrichment |
| URL | External DataHive endpoint |
| Description | Enrich offender address information |
| Method | POST |
| Header | `{ "Authorization": "Bearer [token]", "Content-Type": "application/json" }` |
| Payload | `{ "queryType": "NRIC", "queryValue": "S1234567A" }` |
| Response | `{ "queryValue": "S1234567A", "currentAddress": { "fullAddress": "123 ORCHARD ROAD #01-01 SINGAPORE 238888", "lastUpdated": "2025-12-01" }, "uen": null }` |
| Response Failure | `{ "appCode": "DH-5000", "message": "Service unavailable" }` |

### 1.3.2 Data Mapping

| Zone | Database Table | Field Name | Description |
| --- | --- | --- | --- |
| Intranet | ocms_valid_offence_notice | notice_no | Unique notice identifier |
| Intranet | ocms_valid_offence_notice | vehicle_no | Vehicle registration number |
| Intranet | ocms_valid_offence_notice | vehicle_registration_type | Vehicle type ('V' for VIP) |
| Intranet | ocms_valid_offence_notice | offence_notice_type | Type O, E, or U |
| Intranet | ocms_valid_offence_notice | notice_date_and_time | Date/time of offence |
| Intranet | ocms_valid_offence_notice | computer_rule_code | Offence rule code |
| Intranet | ocms_valid_offence_notice | pp_code | Carpark code |
| Intranet | ocms_valid_offence_notice | pp_name | Carpark name |
| Intranet | ocms_valid_offence_notice | parking_lot_no | Parking lot number |
| Intranet | ocms_valid_offence_notice | composition_amount | Original fine amount |
| Intranet | ocms_valid_offence_notice | amount_payable | Amount due after adjustments |
| Intranet | ocms_valid_offence_notice | payment_status | Payment status (U=Unpaid, FP=Fully Paid) |
| Intranet | ocms_valid_offence_notice | suspension_type | TS or PS |
| Intranet | ocms_valid_offence_notice | epr_reason_of_suspension | EPR suspension reason (OLD, CLV) |
| Intranet | ocms_valid_offence_notice | epr_date_of_suspension | EPR suspension date |
| Intranet | ocms_valid_offence_notice | due_date_of_revival | Due date for revival |
| Intranet | ocms_valid_offence_notice | last_processing_stage | Current processing stage |
| Intranet | ocms_valid_offence_notice | last_processing_date | Stage transition date |
| Intranet | ocms_valid_offence_notice | next_processing_stage | Next stage to transition |
| Intranet | ocms_valid_offence_notice | next_processing_date | Date for next transition |
| Intranet | ocms_valid_offence_notice | prev_processing_stage | Previous stage |
| Intranet | ocms_valid_offence_notice | prev_processing_date | Previous stage date |
| Intranet | ocms_valid_offence_notice | an_flag | Advisory Notice flag (Y/N) |
| Intranet | ocms_valid_offence_notice | cre_date | Record creation timestamp |
| Intranet | ocms_valid_offence_notice | cre_user_id | User who created record |
| Intranet | ocms_valid_offence_notice | upd_date | Last update timestamp |
| Intranet | ocms_valid_offence_notice | upd_user_id | User who last updated |
| Intranet | ocms_suspended_notice | notice_no | Notice identifier |
| Intranet | ocms_suspended_notice | date_of_suspension | Suspension start date |
| Intranet | ocms_suspended_notice | sr_no | Sequence number |
| Intranet | ocms_suspended_notice | suspension_source | Source system (OCMS) |
| Intranet | ocms_suspended_notice | suspension_type | TS or PS |
| Intranet | ocms_suspended_notice | reason_of_suspension | Suspension reason code |
| Intranet | ocms_suspended_notice | officer_authorising_suspension | Authorizing officer |
| Intranet | ocms_suspended_notice | due_date_of_revival | Revival due date |
| Intranet | ocms_suspended_notice | suspension_remarks | Additional remarks |
| Intranet | ocms_suspended_notice | date_of_revival | Actual revival date |
| Intranet | ocms_suspended_notice | revival_reason | Reason for revival |
| Intranet | ocms_suspended_notice | officer_authorising_revival | Officer who revived |
| Intranet | ocms_suspended_notice | process_indicator | Processing status |
| Intranet | ocms_suspended_notice | cre_date | Record creation timestamp |
| Intranet | ocms_suspended_notice | cre_user_id | User who created record |
| External | vip_vehicle | vehicle_no | Vehicle registration number |
| External | vip_vehicle | status | VIP label status (ACTIVE) |
| External | vip_vehicle | label_expiry_date | VIP label expiry date |

### 1.3.3 Database Operations

**Step 4 - Notice Created at NPA:**
```sql
INSERT INTO ocms_valid_offence_notice
(notice_no, vehicle_no, vehicle_registration_type,
offence_notice_type, notice_date_and_time,
computer_rule_code, pp_code, pp_name,
parking_lot_no, composition_amount,
amount_payable, payment_status,
last_processing_stage, next_processing_stage,
next_processing_date, an_flag,
cre_date, cre_user_id)
VALUES
(:notice_no, :vehicle_no, 'V',
:offence_type, :notice_datetime,
:rule_code, :pp_code, :pp_name,
:lot_no, :comp_amt, :amt_payable, 'U',
'NPA', 'ROV', :next_date, :an_flag,
CURRENT_TIMESTAMP, 'ocmsiz_app_conn')
```

**Step 7 - Apply TS-OLD (21 days):**
```sql
UPDATE ocms_valid_offence_notice
SET suspension_type = 'TS',
    epr_reason_of_suspension = 'OLD',
    epr_date_of_suspension = CURRENT_TIMESTAMP,
    due_date_of_revival = CURRENT_DATE + 21,
    upd_date = CURRENT_TIMESTAMP,
    upd_user_id = 'ocmsiz_app_conn'
WHERE notice_no = :notice_no

INSERT INTO ocms_suspended_notice
(notice_no, date_of_suspension, sr_no,
suspension_source, suspension_type,
reason_of_suspension,
officer_authorising_suspension,
due_date_of_revival, suspension_remarks,
process_indicator, cre_date, cre_user_id)
VALUES (:notice_no, CURRENT_TIMESTAMP,
:sr_no, 'OCMS', 'TS', 'OLD', 'ocmsiz_app_conn',
CURRENT_DATE + 21, 'VIP OIC Investigation',
'PROCESSED', CURRENT_TIMESTAMP,
'ocmsiz_app_conn')
```

**Step 11/13/15 - Update Stage (RD1/RD2/RR3):**
```sql
UPDATE ocms_valid_offence_notice
SET last_processing_stage = :new_stage,
    last_processing_date = CURRENT_TIMESTAMP,
    next_processing_stage = :next_stage,
    next_processing_date = :calculated_date,
    prev_processing_stage = :from_stage,
    prev_processing_date = :from_date,
    upd_date = CURRENT_TIMESTAMP,
    upd_user_id = 'ocmsiz_app_conn'
WHERE notice_no = :notice_no
```

**Step 18 - Apply TS-CLV at CPC:**
```sql
UPDATE ocms_valid_offence_notice
SET last_processing_stage = 'CPC',
    last_processing_date = CURRENT_TIMESTAMP,
    next_processing_stage = 'CSD',
    suspension_type = 'TS',
    epr_reason_of_suspension = 'CLV',
    epr_date_of_suspension = CURRENT_TIMESTAMP,
    due_date_of_revival = :calculated_due_date,
    upd_date = CURRENT_TIMESTAMP,
    upd_user_id = 'ocmsiz_app_conn'
WHERE notice_no = :notice_no

INSERT INTO ocms_suspended_notice
(notice_no, date_of_suspension, sr_no,
suspension_source, suspension_type,
reason_of_suspension,
officer_authorising_suspension,
due_date_of_revival, suspension_remarks,
process_indicator, cre_date, cre_user_id)
VALUES (:notice_no, CURRENT_TIMESTAMP,
:sr_no, 'OCMS', 'TS', 'CLV', 'ocmsiz_app_conn',
:due_date, 'VIP CLV at CPC stage',
'PROCESSED', CURRENT_TIMESTAMP,
'ocmsiz_app_conn')
```

**Notes:**
- Audit user for Intranet operations: `ocmsiz_app_conn`
- Insert order: Parent table (ocms_valid_offence_notice) first, then child table (ocms_suspended_notice)

### 1.3.4 Success Outcome

- VIP notice successfully created with vehicle_registration_type = 'V'
- TS-OLD suspension applied for 21 days at NPA stage
- Notice proceeds through ROV → RD1 → RD2 → RR3 stages with MHA/DH validation
- Reminder letters generated at each stage transition
- Notice remains payable throughout the processing flow
- If payment made, PS-FP applied and processing stops
- If outstanding at RR3/DR3, TS-CLV applied at CPC stage
- The workflow reaches the End state without triggering any error-handling paths

### 1.3.5 Error Handling

#### Application Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| VIP Check Failed | CAS/FOMS database unavailable | Process notice as standard type, log error for review |
| LTA Check Timeout | LTA VRLS service not responding | Retry 3 times, proceed with existing data if persistent |
| MHA Check Failed | MHA service unavailable | Proceed without update, retry in next batch |
| Double Booking Error | PS-DBB application failed | Log error, notify admin for manual review |
| Suspension Application Failed | Unable to apply TS-OLD/TS-CLV | Log error, add to retry queue |

#### API Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| General Server Error | OCMS-5000 | Something went wrong. Please try again later. | Server encountered an unexpected condition |
| Bad Request | OCMS-4000 | Invalid request. Please check and try again. | Invalid request syntax |
| Notice Not Found | OCMS-4004 | Notice not found. | Notice number does not exist |
| Invalid Suspension State | VIP-004 | Invalid suspension state for this operation. | Business rule violation |
| External Service Error | VIP-008 | External service temporarily unavailable. | LTA/MHA/DH service error |

---

# Section 2 – Advisory Notice Processing

## 2.1 Use Case

1. Type O offences for VIP vehicles that meet specific Advisory Notice (AN) criteria can be processed via the AN sub-flow instead of the standard reminder letter flow.

2. When a Type O VIP notice qualifies for AN:<br>a. The system sets an_flag = 'Y'<br>b. The notice skips TS-OLD suspension<br>c. An Advisory Notice letter is generated and sent to the vehicle owner<br>d. After AN is sent, the notice is suspended with PS-ANS (Permanent Suspension - Advisory Notice Sent)

3. Refer to FD Section 7.2.4 for detailed AN qualification criteria.

## 2.2 Advisory Notice Flow

<!-- Insert flow diagram here -->
![Advisory Notice Flow](./images/section2-advisory-notice-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | Type O offence classified for VIP vehicle | Entry point |
| Duplicate Notice Details Check | Same duplicate check as main flow | Duplicate validation |
| Detect Vehicle Registration Type | Identify vehicle_registration_type = 'V' | VIP detection |
| Notice Created at NPA | Insert notice record at NPA stage | Notice creation |
| Check for Double Booking | Apply PS-DBB if duplicate found | DBB check |
| AN Qualification Check | Determine if notice meets AN criteria | AN eligibility |
| Standard VIP Flow | If not AN qualified, continue Type O&E flow | Alternative path |
| Update AN_Flag = 'Y' | Set an_flag and next_processing_stage = 'RD1' | AN flagged |
| AN Letter Generation Cron | Daily scheduled job to generate AN letters | Cron trigger |
| Query for Notices | Retrieve VIP notices with an_flag = 'Y' ready for AN | Query execution |
| Generate AN Letter PDF | Create PDF letter for each qualifying notice | Letter generation |
| Drop Letters into SFTP | Send letter files to printing vendor | SFTP transfer |
| Update Notice Stage | Update last_processing_stage = 'RD1' | Stage update |
| Suspend Notices with PS-ANS | Apply PS-ANS after letter sent | PS-ANS applied |
| End | AN flow complete | Exit point |

### 2.2.1 Data Mapping

| Zone | Database Table | Field Name | Description |
| --- | --- | --- | --- |
| Intranet | ocms_valid_offence_notice | notice_no | Unique notice identifier |
| Intranet | ocms_valid_offence_notice | vehicle_no | Vehicle registration number |
| Intranet | ocms_valid_offence_notice | vehicle_registration_type | Vehicle type ('V' for VIP) |
| Intranet | ocms_valid_offence_notice | offence_notice_type | Type O (AN qualified) |
| Intranet | ocms_valid_offence_notice | an_flag | Advisory Notice flag (Y/N) |
| Intranet | ocms_valid_offence_notice | notice_date_and_time | Date/time of offence |
| Intranet | ocms_valid_offence_notice | computer_rule_code | Offence rule code |
| Intranet | ocms_valid_offence_notice | pp_code | Carpark code |
| Intranet | ocms_valid_offence_notice | pp_name | Carpark name |
| Intranet | ocms_valid_offence_notice | composition_amount | Original fine amount |
| Intranet | ocms_valid_offence_notice | last_processing_stage | Current processing stage |
| Intranet | ocms_valid_offence_notice | next_processing_stage | Next stage to transition |
| Intranet | ocms_valid_offence_notice | next_processing_date | Date for next transition |
| Intranet | ocms_valid_offence_notice | suspension_type | TS or PS |
| Intranet | ocms_valid_offence_notice | upd_date | Last update timestamp |
| Intranet | ocms_valid_offence_notice | upd_user_id | User who last updated |
| Intranet | ocms_suspended_notice | notice_no | Notice identifier |
| Intranet | ocms_suspended_notice | date_of_suspension | Suspension start date |
| Intranet | ocms_suspended_notice | sr_no | Sequence number |
| Intranet | ocms_suspended_notice | suspension_source | Source system (OCMS) |
| Intranet | ocms_suspended_notice | suspension_type | PS |
| Intranet | ocms_suspended_notice | reason_of_suspension | ANS (Advisory Notice Sent) |
| Intranet | ocms_suspended_notice | officer_authorising_suspension | ocmsiz_app_conn (auto) |
| Intranet | ocms_suspended_notice | cre_date | Record creation timestamp |
| Intranet | ocms_suspended_notice | cre_user_id | User who created record |

### 2.2.2 Database Operations

**Query AN Notices:**
```sql
SELECT notice_no, vehicle_no, vehicle_registration_type,
       offence_notice_type, an_flag, notice_date_and_time,
       computer_rule_code, pp_code, pp_name,
       composition_amount, last_processing_stage,
       next_processing_stage, next_processing_date
FROM ocms_valid_offence_notice
WHERE vehicle_registration_type = 'V'
  AND an_flag = 'Y'
  AND last_processing_stage = 'NPA'
  AND next_processing_stage = 'ROV'
  AND next_processing_date <= CURRENT_DATE
  AND suspension_type IS NULL
```

**Update Notice Stage after AN Letter Sent:**
```sql
UPDATE ocms_valid_offence_notice
SET last_processing_stage = 'RD1',
    last_processing_date = CURRENT_TIMESTAMP,
    next_processing_stage = 'RD2',
    next_processing_date = :calculated_date,
    upd_date = CURRENT_TIMESTAMP,
    upd_user_id = 'ocmsiz_app_conn'
WHERE notice_no = :notice_no
```

**Apply PS-ANS:**
```sql
INSERT INTO ocms_suspended_notice
(notice_no, date_of_suspension, sr_no,
suspension_source, suspension_type,
reason_of_suspension,
officer_authorising_suspension,
cre_date, cre_user_id)
VALUES
(:notice_no, CURRENT_TIMESTAMP, :sr_no,
'OCMS', 'PS', 'ANS', 'ocmsiz_app_conn',
CURRENT_TIMESTAMP, 'ocmsiz_app_conn')
```

### 2.2.3 Success Outcome

- AN qualification check correctly identifies eligible Type O notices
- an_flag set to 'Y' for qualifying notices
- Advisory Notice letter successfully generated and sent via SFTP
- Notice stage updated to RD1 after AN letter sent
- PS-ANS applied to suspend notice after AN processing
- The workflow reaches the End state without triggering any error-handling paths

### 2.2.4 Error Handling

#### Application Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| AN Criteria Evaluation Failed | Unable to determine AN eligibility | Default to standard VIP flow |
| SFTP Transfer Failed | Letter upload to printing vendor failed | Retry with backoff, alert admin |
| Stage Update Failed | Unable to update notice stage | Log error, add to retry queue |
| PS-ANS Application Failed | Unable to apply permanent suspension | Log error for manual review |

#### API Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| General Server Error | OCMS-5000 | Something went wrong. Please try again later. | Server error |
| Letter Generation Error | AN-001 | Unable to generate advisory notice letter. | PDF generation failed |
| SFTP Connection Error | AN-002 | Unable to connect to letter service. | SFTP connection failed |

---

# Section 3 – Furnish Driver/Hirer

## 3.1 Use Case

1. Vehicle owners can furnish driver or hirer particulars for VIP vehicle notices via the eService Portal.

2. Furnish is allowed at the following stages:<br>a. ROV - After TS-OLD revival<br>b. RD1 - Owner only<br>c. RD2 - Owner only

3. When furnish application is submitted:<br>a. Application is stored in Internet database (eocms_furnish_application)<br>b. Sync to Intranet database (ocms_furnish_application)<br>c. System validates particulars against MHA<br>d. If auto-approve criteria met, application is automatically approved<br>e. If not, OIC reviews and approves/rejects manually

4. After approval:<br>a. Driver furnish: Notice moves to DN1 stage<br>b. Hirer furnish: Notice moves to RD1 stage (reset for new offender)

5. Refer to FD OCMS 41 for detailed furnish validation rules.

## 3.2 Furnish Driver/Hirer Flow

<!-- Insert flow diagram here -->
![Furnish Driver/Hirer Flow](./images/section3-furnish-driver-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | Owner accesses eService Portal | Entry point |
| Furnish Driver/Hirer & Submit | Owner submits driver/hirer particulars | Application submission |
| Update Notice & Driver/Hirer Info | Insert into eocms_furnish_application in Internet zone | Internet DB update |
| Sync Driver/Hirer Info to Intranet | Scheduled sync job copies to Intranet | Data sync |
| Validate Driver/Hirer Particulars | Check against MHA, format validation | Validation |
| Auto Approve Criteria Check | Determine if auto-approval criteria met | Auto-approve check |
| OIC Reviews Particulars | Manual review required if not auto-approved | Manual review |
| OIC Decision | OIC approves or rejects application | Decision point |
| Update for Rejection | Remove furnish pending status in Internet DB | Rejection handling |
| Auto Approve | Update furnish status to approved | Auto-approval |
| Driver/Hirer Particulars Added | Create record in ocms_offence_notice_owner_driver | Record creation |
| Driver or Hirer Check | Determine furnish type for stage routing | Type check |
| Change Next Stage to DN1 | For driver furnish | Driver route |
| Change Next Stage to RD1 | For hirer furnish | Hirer route |
| Continue Processing | Process through DN2/RD2 → DR3/RR3 stages | Stage continuation |
| Outstanding at DR3/RR3 | Check if notice still unpaid | Payment check |
| Apply TS-CLV at CPC | Apply looping suspension if outstanding | TS-CLV applied |
| End | Processing complete | Exit point |

### 3.2.1 API Specification

#### API for eService

##### API Furnish Driver/Hirer

| Field | Value |
| --- | --- |
| API Name | furnish-driver-hirer |
| URL | UAT: https://[domain]/ocms/v1/notices/{noticeNo}/furnish <br> PRD: https://[domain]/ocms/v1/notices/{noticeNo}/furnish |
| Description | Submit driver or hirer particulars for a notice |
| Method | POST |
| Header | `{ "Authorization": "Bearer [token]", "Content-Type": "application/json" }` |
| Payload | `{ "furnishType": "DRIVER", "particulars": { "idType": "NRIC", "idNumber": "S1234567A", "name": "JOHN DOE", "contactNo": "91234567", "email": "john@example.com" }, "supportingDocuments": [{ "docType": "RENTAL_AGREEMENT", "fileName": "rental.pdf", "fileBase64": "..." }] }` |
| Response | `{ "appCode": "OCMS-2000", "message": "Success", "data": { "noticeNo": "500500303J", "applicationId": "FA20260115001", "status": "PENDING_APPROVAL", "submittedDate": "2026-01-15T10:30:00Z" } }` |
| Response (Empty) | `{ "appCode": "OCMS-2000", "message": "Success", "data": [] }` |
| Response Failure | `{ "appCode": "OCMS-4000", "message": "Invalid furnish request" }` |

### 3.2.2 Data Mapping

| Zone | Database Table | Field Name | Type | Description |
| --- | --- | --- | --- | --- |
| Internet | eocms_furnish_application | txn_ref_no | varchar(30) | Unique application identifier (PK) |
| Internet | eocms_furnish_application | notice_no | varchar(10) | Notice identifier |
| Internet | eocms_furnish_application | hirer_driver_indicator | varchar(1) | D=Driver, H=Hirer |
| Internet | eocms_furnish_application | furnish_id_type | varchar(1) | N=NRIC, F=FIN, P=Passport |
| Internet | eocms_furnish_application | furnish_id_no | varchar(12) | ID number |
| Internet | eocms_furnish_application | furnish_name | varchar(66) | Offender name |
| Internet | eocms_furnish_application | furnish_tel_code | varchar(3) | Country code |
| Internet | eocms_furnish_application | furnish_tel_no | varchar(12) | Contact number |
| Internet | eocms_furnish_application | furnish_email_addr | varchar(320) | Email address |
| Internet | eocms_furnish_application | status | varchar(20) | Application status |
| Internet | eocms_furnish_application | is_sync | varchar(1) | Sync flag (default 'N') |
| Intranet | ocms_furnish_application | txn_ref_no | varchar(30) | Synced application ID (PK) |
| Intranet | ocms_furnish_application | notice_no | varchar(10) | Notice identifier |
| Intranet | ocms_furnish_application | hirer_driver_indicator | varchar(1) | D=Driver, H=Hirer |
| Intranet | ocms_furnish_application | status | varchar(20) | Application status |
| Intranet | ocms_offence_notice_owner_driver | notice_no | Notice identifier |
| Intranet | ocms_offence_notice_owner_driver | owner_driver_indicator | O=Owner, D=Driver, H=Hirer |
| Intranet | ocms_offence_notice_owner_driver | id_type | NRIC, FIN, PASSPORT |
| Intranet | ocms_offence_notice_owner_driver | id_no | ID number |
| Intranet | ocms_offence_notice_owner_driver | name | Offender name |
| Intranet | ocms_offence_notice_owner_driver | date_of_birth | Date of birth |
| Intranet | ocms_offence_notice_owner_driver | life_status | A=Alive, D=Deceased |
| Intranet | ocms_offence_notice_owner_driver | email_addr | Email address |
| Intranet | ocms_offence_notice_owner_driver | lta_entity_type | Entity type from LTA |
| Intranet | ocms_offence_notice_owner_driver | offender_indicator | Y/N - Current offender |
| Intranet | ocms_offence_notice_owner_driver | offender_tel_code | Telephone country code |
| Intranet | ocms_offence_notice_owner_driver | offender_tel_no | Telephone number |
| Intranet | ocms_offence_notice_owner_driver | passport_place_of_issue | Passport issue place |
| Intranet | ocms_offence_notice_owner_driver | is_sync | Sync status |
| Intranet | ocms_offence_notice_owner_driver | mha_dh_check_allow | MHA/DH check allowed |
| Intranet | ocms_offence_notice_owner_driver | cre_date | Record creation timestamp |
| Intranet | ocms_offence_notice_owner_driver | cre_user_id | User who created record |
| Intranet | ocms_valid_offence_notice | next_processing_stage | Next stage (DN1 or RD1) |

### 3.2.3 Database Operations

**Insert Driver/Hirer Record:**
```sql
INSERT INTO ocms_offence_notice_owner_driver
(notice_no, owner_driver_indicator,
id_type, id_no, name, date_of_birth,
life_status, email_addr, lta_entity_type,
offender_indicator, offender_tel_code,
offender_tel_no, passport_place_of_issue,
is_sync, mha_dh_check_allow,
cre_date, cre_user_id)
VALUES
(:notice_no, :owner_driver_indicator,
:id_type, :id_no, :name, :dob,
'A', :email, :entity_type,
'Y', :tel_code, :tel_no, :passport_poi,
'N', 'Y',
CURRENT_TIMESTAMP, 'ocmsiz_app_conn')
```

**Notes:**
- Audit user for Internet operations: `ocmsez_app_conn`
- Audit user for Intranet operations: `ocmsiz_app_conn`
- Insert order: eocms_furnish_application (Internet) first, then sync to ocms_furnish_application (Intranet), then ocms_offence_notice_owner_driver

### 3.2.4 Success Outcome

- Furnish application successfully submitted via eService Portal
- Application synced from Internet to Intranet database
- Particulars validated against MHA
- Auto-approval applied for qualifying applications
- OIC notified for manual review when required
- Driver/hirer record created in ocms_offence_notice_owner_driver
- Notice stage updated to DN1 (driver) or RD1 (hirer)
- The workflow reaches the End state without triggering any error-handling paths

### 3.2.5 Error Handling

#### Application Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Furnish Not Allowed | Notice stage does not allow furnish | Return error, notify user |
| Invalid Particulars | ID number format invalid | Return validation error |
| MHA Validation Failed | MHA service unavailable | Queue for retry, notify OIC |
| Sync Failed | Internet to Intranet sync error | Retry, alert admin if persistent |
| Stage Update Failed | Unable to update notice stage | Log error, add to retry queue |

#### API Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| General Server Error | OCMS-5000 | Something went wrong. Please try again later. | Server error |
| Bad Request | OCMS-4000 | Invalid request. Please check and try again. | Invalid syntax |
| Notice Not Found | OCMS-4004 | Notice not found. | Notice number does not exist |
| Furnish Not Allowed | OCMS-4010 | Furnish not allowed at this stage. | Business rule violation |
| Invalid ID Format | OCMS-4011 | Invalid ID number format. | Validation failed |

---

# Section 4 – TS-CLV Suspension Processing

## 4.1 Use Case

1. TS-CLV (Temporary Suspension - Classified Vehicles) is applied to VIP vehicle notices that remain outstanding after the reminder letter stages.

2. TS-CLV is applied at:<br>a. MVP1: End of RR3/DR3 stage<br>b. Post-MVP1: CPC stage (when court processing is enabled)

3. TS-CLV is a "looping" temporary suspension that:<br>a. Auto re-applies when the suspension period expires<br>b. Continues looping until OIC manually revives and changes vehicle type from 'V' to 'S'<br>c. Or until the notice is fully paid

4. VIP notices under TS-CLV remain payable via eService and AXS.

5. Refer to FD Section 7.5 and 7.7 for detailed TS-CLV rules.

## 4.2 TS-CLV Suspension Cron Flow

<!-- Insert flow diagram here -->
![TS-CLV Suspension Cron Flow](./images/section4-ts-clv-cron-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start - Suspend Notice Cron | Daily scheduled job starts at EOD | Cron start |
| Query Valid Offence Notice Table | Find VIP notices at RR3/DR3 ready for CPC | Query execution |
| Records Found Check | Determine if any notices to process | Record check |
| No Records - End | Terminate if no records found | Early exit |
| Move Notices into CPC Stage | Update last_processing_stage = 'CPC' | Stage update |
| Suspend Notice with TS-CLV | Apply TS-CLV suspension | Suspension applied |
| Patch Notice Details | Update all stage and suspension fields | Field update |
| Create New Suspension Record | Insert into ocms_suspended_notice | Record creation |
| End | Processing complete | Exit point |

**Auto Re-apply TS-CLV Cron:**

| Step | Description | Brief Description |
| --- | --- | --- |
| Start - Auto Re-apply Cron | Daily scheduled job at 00:00 | Cron start |
| Query Expired TS-CLV | Find VIP notices with expired CLV at CPC | Query execution |
| Records Found Check | Determine if any notices to process | Record check |
| Create Revival Record | Auto revival entry | Revival created |
| Re-apply TS-CLV | New suspension with new due date | Suspension re-applied |
| Create New Suspension Record | Insert new TS-CLV record | Record creation |
| End | Loop continues | Exit point |

### 4.2.1 Data Mapping

| Zone | Database Table | Field Name | Description |
| --- | --- | --- | --- |
| Intranet | ocms_valid_offence_notice | notice_no | Unique notice identifier |
| Intranet | ocms_valid_offence_notice | vehicle_no | Vehicle registration number |
| Intranet | ocms_valid_offence_notice | vehicle_registration_type | Vehicle type ('V' for VIP) |
| Intranet | ocms_valid_offence_notice | offence_notice_type | Type O, E, or U |
| Intranet | ocms_valid_offence_notice | last_processing_stage | Current processing stage |
| Intranet | ocms_valid_offence_notice | last_processing_date | Stage transition date |
| Intranet | ocms_valid_offence_notice | next_processing_stage | Next stage to transition |
| Intranet | ocms_valid_offence_notice | next_processing_date | Date for next transition |
| Intranet | ocms_valid_offence_notice | prev_processing_stage | Previous stage |
| Intranet | ocms_valid_offence_notice | prev_processing_date | Previous stage date |
| Intranet | ocms_valid_offence_notice | suspension_type | TS or PS |
| Intranet | ocms_valid_offence_notice | epr_reason_of_suspension | EPR suspension reason (CLV) |
| Intranet | ocms_valid_offence_notice | epr_date_of_suspension | EPR suspension date |
| Intranet | ocms_valid_offence_notice | due_date_of_revival | Revival due date |
| Intranet | ocms_valid_offence_notice | payment_status | Payment status (U=Unpaid) |
| Intranet | ocms_valid_offence_notice | composition_amount | Original fine amount |
| Intranet | ocms_valid_offence_notice | amount_payable | Amount due |
| Intranet | ocms_valid_offence_notice | upd_date | Last update timestamp |
| Intranet | ocms_valid_offence_notice | upd_user_id | User who last updated |
| Intranet | ocms_suspended_notice | notice_no | Notice identifier |
| Intranet | ocms_suspended_notice | date_of_suspension | Suspension start date |
| Intranet | ocms_suspended_notice | sr_no | Sequence number |
| Intranet | ocms_suspended_notice | suspension_source | Source system (OCMS) |
| Intranet | ocms_suspended_notice | suspension_type | TS |
| Intranet | ocms_suspended_notice | reason_of_suspension | CLV |
| Intranet | ocms_suspended_notice | officer_authorising_suspension | ocmsiz_app_conn (auto) |
| Intranet | ocms_suspended_notice | due_date_of_revival | Revival due date |
| Intranet | ocms_suspended_notice | suspension_remarks | Additional remarks |
| Intranet | ocms_suspended_notice | process_indicator | Processing status |
| Intranet | ocms_suspended_notice | date_of_revival | Actual revival date |
| Intranet | ocms_suspended_notice | cre_date | Record creation timestamp |
| Intranet | ocms_suspended_notice | cre_user_id | User who created record |

### 4.2.2 Database Operations

**Query VIP Notices at RR3/DR3 for TS-CLV:**
```sql
SELECT notice_no, vehicle_no, vehicle_registration_type,
       offence_notice_type, last_processing_stage,
       last_processing_date, next_processing_stage,
       next_processing_date, prev_processing_stage,
       prev_processing_date, suspension_type,
       epr_reason_of_suspension, epr_date_of_suspension,
       due_date_of_revival, payment_status,
       composition_amount, amount_payable
FROM ocms_valid_offence_notice
WHERE vehicle_registration_type = 'V'
  AND last_processing_stage IN ('RR3', 'DR3')
  AND next_processing_stage = 'CPC'
  AND next_processing_date <= CURRENT_DATE
  AND (suspension_type IS NULL OR suspension_type = 'TS')
  AND payment_status != 'FP'
```

**Update Notice with TS-CLV:**
```sql
UPDATE ocms_valid_offence_notice
SET last_processing_stage = 'CPC',
    last_processing_date = CURRENT_TIMESTAMP,
    next_processing_stage = 'CSD',
    next_processing_date = NULL,
    prev_processing_stage = :from_stage,
    prev_processing_date = :from_date,
    suspension_type = 'TS',
    epr_reason_of_suspension = 'CLV',
    epr_date_of_suspension = CURRENT_TIMESTAMP,
    due_date_of_revival = :calculated_due_date,
    upd_date = CURRENT_TIMESTAMP,
    upd_user_id = 'ocmsiz_app_conn'
WHERE notice_no = :notice_no
```

**Insert TS-CLV Suspension Record:**
```sql
INSERT INTO ocms_suspended_notice
(notice_no, date_of_suspension, sr_no,
suspension_source, suspension_type,
reason_of_suspension,
officer_authorising_suspension,
due_date_of_revival, suspension_remarks,
process_indicator, cre_date, cre_user_id)
VALUES
(:notice_no, CURRENT_TIMESTAMP,
:next_sr_no, 'OCMS', 'TS', 'CLV',
'ocmsiz_app_conn', :calculated_due_date,
'Auto TS-CLV at CPC stage', 'PROCESSED',
CURRENT_TIMESTAMP, 'ocmsiz_app_conn')
```

**Query for Auto Re-apply TS-CLV:**
```sql
SELECT notice_no, vehicle_registration_type,
       last_processing_stage, suspension_type,
       epr_reason_of_suspension, due_date_of_revival
FROM ocms_valid_offence_notice
WHERE vehicle_registration_type = 'V'
  AND last_processing_stage = 'CPC'
  AND suspension_type = 'TS'
  AND epr_reason_of_suspension = 'CLV'
  AND due_date_of_revival <= CURRENT_DATE
```

### 4.2.3 Success Outcome

- VIP notices successfully moved to CPC stage
- TS-CLV suspension applied with correct due_date_of_revival
- Suspension record created in ocms_suspended_notice
- Expired TS-CLV successfully re-applied (looping behavior)
- Revival record created for auto re-applied suspensions
- The workflow reaches the End state without triggering any error-handling paths

### 4.2.4 Error Handling

#### Application Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Query Execution Failed | Unable to query eligible notices | Log error, retry next batch |
| Stage Update Failed | Unable to update notice stage | Skip notice, log for manual review |
| Suspension Record Creation Failed | Unable to insert suspension record | Log error, add to retry queue |
| Configuration Error | Suspension period not configured | Use default period, alert admin |

#### API Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| General Server Error | OCMS-5000 | Something went wrong. Please try again later. | Server error |
| Database Error | OCMS-5001 | Database operation failed. | DB connection error |
| Invalid Suspension State | VIP-004 | Invalid suspension state. | Business rule violation |

---

# Section 5 – Classified Vehicle Notices Report

## 5.1 Use Case

1. The Classified Vehicle Notices Report is a daily report generated for OICs to monitor VIP vehicle notices.

2. The report includes:<br>a. Summary of total issued, outstanding, settled, and amended notices<br>b. Detailed list of all Type V notices with payment status<br>c. List of notices amended from type 'V' to 'S' (VIP to Singapore)

3. The report is generated as an Excel file with 3 sheets and emailed to configured OIC recipients.

4. Report timing to be confirmed with users (TBD).

5. Refer to FD Section 7.9.1 for detailed report specifications.

## 5.2 Report Generation Flow

<!-- Insert flow diagram here -->
![Report Generation Flow](./images/section5-report-generation-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | Cron job initiated at scheduled time | Cron start |
| Retrieve Active VIP Vehicles from CAS | Query VIP_VEHICLE table for active vehicles | CAS query |
| Any Active VIP Vehicles Found | Check if records returned from CAS | Record check |
| Generate Empty Report | Create report with no data if no VIP vehicles | Empty report |
| Extract Vehicle Numbers | Get list of vehicle_no from CAS response | Data extraction |
| Retrieve Notice Details from OCMS | Query ocms_valid_offence_notice for VIP notices | OCMS query |
| Store VIP Notice Details | Temporary data storage | Data storage |
| Extract Notice Numbers | Get list of notice_no for offender query | Data extraction |
| Retrieve Current Offender Details | Query ocms_offence_notice_owner_driver | Offender query |
| Store Current Offender Details | Temporary data storage | Data storage |
| Calculate Outstanding Notice Count | Count where crs_reason_of_suspension IS NULL | Count calculation |
| Calculate Settled/Paid Notice Count | Total - Outstanding | Count calculation |
| Retrieve Data from Temp | Prepare data for report generation | Data retrieval |
| Format Report Data | Structure data for Excel output | Data formatting |
| Generate Report | Create Excel file with 3 sheets | Report generation |
| Attach Report to Email | Prepare email with Excel attachment | Email preparation |
| Send Email | Send to configured OIC recipients | Email sent |
| Update Batch Job Status | Log completion status | Status update |
| End | Report generation complete | Exit point |

### 5.2.1 Data Mapping

| Zone | Database Table | Field Name | Description |
| --- | --- | --- | --- |
| External | vip_vehicle | vehicle_no | Vehicle registration number |
| External | vip_vehicle | vip_label_status | VIP label status |
| External | vip_vehicle | status | Active/Inactive status |
| External | vip_vehicle | label_expiry_date | VIP label expiry date |
| Intranet | ocms_valid_offence_notice | notice_no | Unique notice identifier |
| Intranet | ocms_valid_offence_notice | vehicle_no | Vehicle registration number |
| Intranet | ocms_valid_offence_notice | vehicle_registration_type | Vehicle type ('V' for VIP) |
| Intranet | ocms_valid_offence_notice | notice_date_and_time | Date/time of offence |
| Intranet | ocms_valid_offence_notice | computer_rule_code | Offence rule code |
| Intranet | ocms_valid_offence_notice | pp_code | Carpark code |
| Intranet | ocms_valid_offence_notice | pp_name | Carpark name |
| Intranet | ocms_valid_offence_notice | composition_amount | Original fine amount |
| Intranet | ocms_valid_offence_notice | amount_payable | Amount due |
| Intranet | ocms_valid_offence_notice | payment_status | Payment status |
| Intranet | ocms_valid_offence_notice | last_processing_stage | Current stage |
| Intranet | ocms_valid_offence_notice | suspension_type | TS or PS |
| Intranet | ocms_valid_offence_notice | epr_reason_of_suspension | EPR suspension reason |
| Intranet | ocms_valid_offence_notice | crs_reason_of_suspension | CRS suspension reason |
| Intranet | ocms_offence_notice_owner_driver | notice_no | Notice identifier |
| Intranet | ocms_offence_notice_owner_driver | owner_driver_indicator | O=Owner, D=Driver, H=Hirer |
| Intranet | ocms_offence_notice_owner_driver | id_type | ID type |
| Intranet | ocms_offence_notice_owner_driver | id_no | ID number |
| Intranet | ocms_offence_notice_owner_driver | name | Offender name |
| Intranet | ocms_offence_notice_owner_driver | date_of_birth | Date of birth |
| Intranet | ocms_offence_notice_owner_driver | life_status | A=Alive, D=Deceased |
| Intranet | ocms_offence_notice_owner_driver | email_addr | Email address |
| Intranet | ocms_offence_notice_owner_driver | lta_entity_type | Entity type from LTA |
| Intranet | ocms_offence_notice_owner_driver | offender_indicator | Y/N - Current offender |
| Intranet | ocms_offence_notice_owner_driver | offender_tel_no | Telephone number |
| Intranet | ocms_suspended_notice | notice_no | Notice identifier |
| Intranet | ocms_suspended_notice | reason_of_suspension | Suspension reason code |
| Intranet | ocms_suspended_notice | date_of_suspension | Suspension start date |
| Intranet | ocms_suspended_notice | date_of_revival | Revival date |
| Intranet | ocms_carpark | pp_code | Carpark code |
| Intranet | ocms_carpark | pp_name | Carpark name |

### 5.2.2 Database Operations

**Query Active VIP Vehicles from CAS:**
```sql
SELECT vehicle_no, vip_label_status, label_expiry_date
FROM vip_vehicle
WHERE status = 'ACTIVE'
```

**Query Notice Details from OCMS:**
```sql
SELECT von.notice_no, von.vehicle_no,
       von.vehicle_registration_type,
       von.notice_date_and_time, von.computer_rule_code,
       von.pp_code, von.pp_name,
       von.composition_amount, von.amount_payable,
       von.suspension_type, von.epr_reason_of_suspension,
       von.crs_reason_of_suspension,
       von.last_processing_stage, von.payment_status
FROM ocms_valid_offence_notice von
WHERE von.vehicle_no IN (:vip_vehicle_list)
```

**Query Offender Details:**
```sql
SELECT notice_no, owner_driver_indicator,
       id_type, id_no, name, date_of_birth,
       life_status, email_addr, lta_entity_type,
       offender_indicator, offender_tel_no
FROM ocms_offence_notice_owner_driver
WHERE notice_no IN (:notice_list)
  AND offender_indicator = 'Y'
```

**Query Amended Notices (V→S):**
```sql
SELECT von.notice_no, von.vehicle_no,
       von.notice_date_and_time, von.pp_name,
       von.composition_amount, von.amount_payable,
       sn.date_of_suspension, sn.date_of_revival
FROM ocms_valid_offence_notice von
JOIN ocms_suspended_notice sn
  ON von.notice_no = sn.notice_no
WHERE sn.reason_of_suspension = 'CLV'
  AND sn.date_of_revival IS NOT NULL
  AND von.vehicle_registration_type = 'S'
```

**Report Output Structure:**

**Sheet 1: Summary**
| Field | Description |
| --- | --- |
| Report Date | Current date |
| Total Notices Issued (Type V) | Count of all Type V notices |
| Outstanding Notices (Unpaid) | Count where crs_reason_of_suspension IS NULL |
| Settled Notices (Paid) | Total - Outstanding |
| Notices Amended (V→S) | Count of notices changed from V to S |

**Sheet 2: Type V Notices Detail**
| Column | Source Field |
| --- | --- |
| S/N | Row number |
| Notice No | notice_no |
| Vehicle No | vehicle_no |
| Notice Date | notice_date_and_time |
| Offence Code | computer_rule_code |
| Place of Offence | pp_name (from ocms_carpark) |
| Composition Amount ($) | composition_amount |
| Amount Payable ($) | amount_payable |
| Payment Status | Derived (Outstanding/Settled) |
| Suspension Type | suspension_type |
| Suspension Reason | epr_reason_of_suspension |

**Sheet 3: Amended Notices (V→S)**
| Column | Source Field |
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

### 5.2.3 Success Outcome

- CAS/FOMS query successfully retrieves active VIP vehicles
- OCMS query retrieves all related notice details
- Offender details successfully retrieved for all notices
- Outstanding and settled counts correctly calculated
- Excel report generated with 3 sheets (Summary, Type V Detail, Amended)
- Email successfully sent to all configured OIC recipients
- Batch job status logged as completed
- The workflow reaches the End state without triggering any error-handling paths

### 5.2.4 Error Handling

#### Application Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| CAS Query Failed | Unable to retrieve VIP vehicles from CAS | Generate report with warning, notify admin |
| OCMS Query Failed | Unable to retrieve notice details | Log error, terminate with notification |
| Excel Generation Failed | Unable to create Excel file | Log error, retry or notify admin |
| Email Send Failed | Unable to send email to recipients | Retry, log for manual send if persistent |

#### API Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| General Server Error | OCMS-5000 | Something went wrong. Please try again later. | Server error |
| CAS Connection Error | RPT-001 | Unable to connect to CAS service. | External service error |
| Email Service Error | RPT-002 | Unable to send report email. | Email service unavailable |
| Report Generation Error | RPT-003 | Unable to generate report. | Excel creation failed |

---

# Appendix A – Sync Flag Mechanism

## A.1 Overview

The sync flag (`is_sync`) is used to track synchronization status between Intranet and Internet databases. This ensures data consistency across both environments.

## A.2 Sync Flag Values

| Value | Description | Action Required |
| --- | --- | --- |
| Y | Synchronized | No action needed |
| N | Pending sync | Sync job will process |
| F | Failed sync | Retry required |

## A.3 Tables with Sync Flag

| Zone | Table | Sync Flag Field | Sync Direction |
| --- | --- | --- | --- |
| Intranet | ocms_valid_offence_notice | is_sync | Intranet → Internet |
| Intranet | ocms_offence_notice_owner_driver | is_sync | Intranet → Internet |
| Intranet | ocms_suspended_notice | is_sync | Intranet → Internet |
| Internet | eocms_furnish_application | is_sync | Internet → Intranet |

## A.4 Sync Flow

### Intranet to Internet Sync

```
1. Notice created/updated in Intranet
2. Set is_sync = 'N' on affected record
3. Sync cron job queries records where is_sync = 'N'
4. Push data to Internet database
5. On success: Set is_sync = 'Y'
6. On failure: Set is_sync = 'F', log error
7. Retry job picks up is_sync = 'F' records
```

### Internet to Intranet Sync (Furnish Application)

```
1. Furnish application submitted via eService
2. Record created in eocms_furnish_application with is_sync = 'N'
3. Sync cron job queries records where is_sync = 'N'
4. Push data to Intranet (ocms_furnish_application)
5. On success: Set is_sync = 'Y' in Internet
6. On failure: Set is_sync = 'F', retry later
```

## A.5 Sync Flag Update Logic

**When to set is_sync = 'N':**
- New record inserted
- Record updated (stage change, suspension applied, payment received)
- Any field modification requiring internet sync

**When to set is_sync = 'Y':**
- Sync job successfully pushed data to target database
- Manual sync confirmation

**When to set is_sync = 'F':**
- Sync job failed after all retries
- Connection timeout to target database
- Data validation failed at target

---

# Appendix B – Suspension Type Reference

| Suspension Code | Type | Description | Payable | Furnishable | Terminal |
| --- | --- | --- | --- | --- | --- |
| TS-OLD | TS | OIC Investigation (21 days) | Yes | No | No |
| TS-CLV | TS | Classified Vehicles (Looping) | Yes | No | No |
| PS-FP | PS | Fully Paid | No | No | Yes |
| PS-DBB | PS | Double Booking | No | No | Yes |
| PS-ANS | PS | Advisory Notice Sent | No | No | Yes |

---

# Appendix C – Stage Transition Reference

| From Stage | To Stage | Trigger | Actions |
| --- | --- | --- | --- |
| NPA | TS-OLD | Notice created, Type V, not AN | Apply TS-OLD suspension |
| NPA | RD1 | Notice created, Type V, AN qualified | Generate AN letter |
| TS-OLD | ROV | Manual or auto revival | LTA ownership check |
| ROV | RD1 | LTA complete | MHA/DH checks, generate RD1 letter |
| RD1 | RD2 | Outstanding after RD1 period | MHA/DH checks, generate RD2 letter |
| RD2 | RR3 | Outstanding after RD2 period | MHA/DH checks, generate RR3 letter |
| RR3 | CPC | Outstanding after RR3 period | Apply TS-CLV suspension |
| CPC | CPC | TS-CLV expired, type still V | Re-apply TS-CLV (looping) |
| CPC | Standard | Type changed V→S | Exit loop, continue standard flow |

---

# Appendix D – Allowed Functions Matrix (VIP Notices)

| Notice Stage | eNotification | Payment | Furnish Driver | Comments |
| --- | --- | --- | --- | --- |
| NPA | No | Yes | No | VIP bypass eNotification |
| TS-OLD | No | Yes | No | Under investigation |
| ROV | No | Yes | Yes | After revival |
| RD1 | No | Yes | Yes (Owner only) | Owner can furnish |
| DN1 | No | Yes | No | Driver stage |
| RD2 | No | Yes | Yes (Owner only) | Owner can furnish |
| DN2 | No | Yes | No | Driver stage |
| RR3/DR3 | No | Yes | No | Final reminder |
| CPC (TS-CLV) | No | Yes | No | Under TS-CLV |

---

# Appendix E – Eligibility Scenarios by Source

## E.1 Overview

Different source systems have different permissions for VIP notice operations. This appendix documents the eligibility matrix for each source.

## E.2 Source Systems

| Source Code | Source Name | Zone | Description |
| --- | --- | --- | --- |
| SP | OCMS Staff Portal | Intranet | OIC operations |
| PLUS | PLUS Staff Portal | Intranet | PLUS contractor operations |
| ES | eService Portal | Internet | Public self-service |
| CRON | Backend Cron | Intranet | Automated batch jobs |
| AXS | AXS Kiosk | Internet | Payment terminals |

## E.3 Eligibility Matrix by Source

### E.3.1 Notice Operations

| Operation | Staff Portal | PLUS Portal | eService | Cron | AXS |
| --- | --- | --- | --- | --- | --- |
| View VIP Notice | Yes | Yes | Yes (Own) | N/A | No |
| Search VIP Notice | Yes | Yes | Yes (Own) | N/A | No |
| Create Notice | No | No | No | Yes | No |
| Edit Notice | Yes (Limited) | No | No | No | No |

### E.3.2 Suspension Operations

| Operation | Staff Portal | PLUS Portal | eService | Cron | AXS |
| --- | --- | --- | --- | --- | --- |
| Apply TS-OLD | Yes | No | No | Yes (Auto) | No |
| Revive TS-OLD (Manual) | Yes | No | No | No | No |
| Revive TS-OLD (Auto) | No | No | No | Yes | No |
| Apply TS-CLV | Yes | No | No | Yes (Auto) | No |
| Revive TS-CLV (Manual) | Yes | No | No | No | No |
| Revive TS-CLV (Auto) | No | No | No | Yes | No |
| Apply PS-FP | No | No | No | Yes (Auto) | No |
| Apply PS-DBB | No | No | No | Yes (Auto) | No |
| Apply PS-ANS | No | No | No | Yes (Auto) | No |

### E.3.3 Payment Operations

| Operation | Staff Portal | PLUS Portal | eService | Cron | AXS |
| --- | --- | --- | --- | --- | --- |
| Make Payment | No | No | Yes | No | Yes |
| View Payment History | Yes | Yes | Yes (Own) | N/A | No |
| Generate Receipt | Yes | No | Yes | No | Yes |

### E.3.4 Furnish Operations

| Operation | Staff Portal | PLUS Portal | eService | Cron | AXS |
| --- | --- | --- | --- | --- | --- |
| Submit Furnish | No | No | Yes (Owner) | No | No |
| Review Furnish | Yes | No | No | No | No |
| Approve Furnish | Yes | No | No | Yes (Auto) | No |
| Reject Furnish | Yes | No | No | No | No |

### E.3.5 Report Operations

| Operation | Staff Portal | PLUS Portal | eService | Cron | AXS |
| --- | --- | --- | --- | --- | --- |
| Generate CV Report | Yes | No | No | Yes (Auto) | No |
| View CV Report | Yes | No | No | N/A | No |
| Download CV Report | Yes | No | No | N/A | No |

### E.3.6 Vehicle Type Operations

| Operation | Staff Portal | PLUS Portal | eService | Cron | AXS |
| --- | --- | --- | --- | --- | --- |
| View Vehicle Type | Yes | Yes | Yes | N/A | No |
| Change V→S | Yes (OIC) | No | No | No | No |
| Change S→V | No | No | No | No | No |

## E.4 Stage-Based Eligibility

### E.4.1 Furnish Eligibility by Stage

| Stage | Staff Portal | eService | Furnish Type Allowed |
| --- | --- | --- | --- |
| NPA | No | No | None |
| TS-OLD | No | No | None |
| ROV | No | Yes | Driver/Hirer |
| RD1 | No | Yes | Driver/Hirer (Owner only) |
| DN1 | No | No | None |
| RD2 | No | Yes | Driver/Hirer (Owner only) |
| DN2 | No | No | None |
| RR3/DR3 | No | No | None |
| CPC (TS-CLV) | No | No | None |

### E.4.2 Payment Eligibility by Stage

| Stage | eService | AXS | Notes |
| --- | --- | --- | --- |
| NPA | Yes | Yes | VIP remains payable |
| TS-OLD | Yes | Yes | VIP remains payable during investigation |
| ROV | Yes | Yes | After revival |
| RD1 | Yes | Yes | Reminder stage |
| DN1 | Yes | Yes | Driver stage |
| RD2 | Yes | Yes | Reminder stage |
| DN2 | Yes | Yes | Driver stage |
| RR3/DR3 | Yes | Yes | Final reminder |
| CPC (TS-CLV) | Yes | Yes | VIP remains payable under TS-CLV |

## E.5 Implementation Notes

### E.5.1 Source Detection

```
Source detection logic:
1. Check request header for source identifier
2. Staff Portal: source_code = 'SP', zone = 'INTRANET'
3. PLUS Portal: source_code = 'PLUS', zone = 'INTRANET'
4. eService: source_code = 'ES', zone = 'INTERNET'
5. Cron: source_code = 'CRON', zone = 'INTRANET'
6. AXS: source_code = 'AXS', zone = 'INTERNET'
```

### E.5.2 Eligibility Check Flow

```
1. Identify source system (SP/PLUS/ES/CRON/AXS)
2. Identify operation requested
3. Check eligibility matrix for source + operation
4. IF not eligible:
   - Return error: "Operation not allowed for this source"
   - Error code: OCMS-4030
5. IF eligible:
   - Check stage-based eligibility (if applicable)
   - Proceed with operation
```

### E.5.3 Error Codes for Eligibility

| Error Code | Message | Scenario |
| --- | --- | --- |
| OCMS-4030 | Operation not allowed for this source | Source not in eligibility matrix |
| OCMS-4031 | Operation not allowed at this stage | Stage restriction |
| OCMS-4032 | Furnish not allowed for this user | Non-owner attempting furnish |
| OCMS-4033 | Payment channel not available | Payment via restricted channel |

---

*Document End*

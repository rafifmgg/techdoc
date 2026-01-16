# OCMS 14 – Notice Processing Flow for Special Types of Vehicles

**Prepared by**

NCS Pte Ltd

---

<!--
IMPORTANT: If information already exists in the Functional Document (FD),
refer to FD instead of duplicating content.
-->

---

## Version History

| Version | Updated By | Date | Changes |
| --- | --- | --- | --- |
| v1.0 | Claude | 15/01/2026 | Document Initiation - Section 4 |

---

## Table of Content

| Section | Content | Pages |
| --- | --- | --- |
| 4 | Notice Processing Flow for Military Vehicles | 1 |
| 4.1 | Use Case | 1 |
| 4.2 | High Level Flow | 2 |
| 4.3 | Type O & E Processing Flow | 3 |
| 4.3.1 | Data Mapping | 7 |
| 4.3.2 | Success Outcome | 8 |
| 4.3.3 | Error Handling | 8 |
| 4.4 | Advisory Notice (AN) Sub-flow | 9 |
| 4.5 | Furnish Driver/Hirer Sub-flow | 11 |
| 4.6 | PS-MID Suspension Flow | 14 |

---

# Section 4 – Notice Processing Flow for Military Vehicles

## 4.1 Use Case

1. Military vehicles are identified by registration numbers containing MID or MINDEF prefix/suffix patterns. These vehicles are registered under MINDEF and bypass normal LTA vehicle registry lookups.

2. The Military Vehicle Notice Processing Flow is triggered when:<br>a. A new notice is created with vehicle_registration_type = 'I' (Military)<br>b. The vehicle number matches MID/MINDEF pattern (e.g., MID2221, MINDEF123)

3. Key characteristics of Military Vehicle processing:<br>a. Owner details are hardcoded to MINDEF (no LTA VRLS lookup)<br>b. Owner address uses standard MINDEF address at Kranji Camp 3<br>c. Notice bypasses ENA (Electronic Notification) stage<br>d. Notice ends with PS-MID (Permanent Suspension - MID) instead of Court Referral

4. Military Vehicle notices follow this stage progression:<br>a. Type O/E: NPA → RD1 → RD2 → PS-MID<br>b. With Furnished Driver: NPA → RD1 → DN1 → DN2 → PS-MID<br>c. With Furnished Hirer: NPA → RD1 → RD2 → PS-MID

5. Refer to FD Section 5 for detailed process flow of the Military Vehicle Notice Processing.

---

## 4.2 High Level Flow

<!-- Insert flow diagram here -->
![High Level Flow](./images/section4-high-level-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | Notice Creation detects vehicle_registration_type = 'I' | Flow entry point |
| Pre-conditions | Validate raw offence data, check duplicate notice, detect MID vehicle | Data validation |
| Identify Offence Type | Determine if notice is Type O, E, or U | Route by offence type |
| Type O & E Workflow | Process parking/ERP offence for military vehicle | Main processing flow |
| Type U Workflow | Process UPL offence for military vehicle (KIV) | UPL flow - pending requirements |
| End of RD2/DN2 | Check if notice is outstanding at end of RD2 or DN2 stage | Stage completion check |
| Outstanding? | Decision point - is notice still unpaid? | Payment status check |
| Apply PS-MID | If outstanding, apply Permanent Suspension - MID | Suspend notice |
| Apply PS-FP | If paid, apply Permanent Suspension - Full Payment | Close notice |
| End | Notice processing completes, no further action | Flow exit point |

### Stage Progression Summary

| Notice Type | Stage Flow | Final State |
| --- | --- | --- |
| Type O (no furnish) | NPA → RD1 → RD2 → PS-MID | Suspended |
| Type O (furnished driver) | NPA → RD1 → DN1 → DN2 → PS-MID | Suspended |
| Type O (furnished hirer) | NPA → RD1 → RD2 → PS-MID | Suspended |
| Type E (no furnish) | NPA → RD1 → RD2 → PS-MID | Suspended |
| Type O (with AN) | NPA → RD1 (AN Letter) → PS-ANS | Suspended |
| All types (paid) | Any stage → PS-FP | Closed |

### Systems Bypassed for Military Vehicles

| System | Normal Purpose | Bypass Reason |
| --- | --- | --- |
| LTA VRLS | Vehicle owner lookup | Military vehicles not in LTA registry |
| MHA | NRIC validation | MINDEF uses UEN, not personal NRIC |
| DataHive | FIN/company profile | Not applicable for military entity |
| ENA | Electronic notification | Military vehicles skip ENA stage |

---

## 4.3 Type O & E Processing Flow

<!-- Insert flow diagram here -->
![Type O & E Processing Flow](./images/section4-type-o-e-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | Begin Type O/E processing for Military Vehicle | Flow entry point |
| Duplicate Notice Check | Query existing notices with same veh_no, date, rule, lot, pp_code | Detect duplicate |
| Duplicate Found? | Decision - does duplicate exist? | Branch based on result |
| Apply PS-DBB | If duplicate found, apply Double Booking suspension | End with PS-DBB |
| Create Notice at NPA | Insert notice record with MINDEF owner details | Create notice |
| AN Qualify? (Type O only) | Check if Type O notice qualifies for Advisory Notice | AN eligibility check |
| AN Sub-flow | If AN qualified, process Advisory Notice (Section 4.4) | End with PS-ANS |
| Prepare for RD1 | Generate RD1 Reminder Letter via TOPPAN SFTP | Letter generation |
| Notice at RD1 | Update last_processing_stage = 'RD1' | Stage update |
| Payment at RD1? | Check if payment received during RD1 stage | Payment check |
| Apply PS-FP (RD1) | If paid at RD1, apply Full Payment suspension | End with PS-FP |
| Prepare for RD2 | Generate RD2 Reminder Letter via TOPPAN SFTP | Letter generation |
| Notice at RD2 | Update last_processing_stage = 'RD2' | Stage update |
| Payment at RD2? | Check if payment received during RD2 stage | Payment check |
| Apply PS-FP (RD2) | If paid at RD2, apply Full Payment suspension | End with PS-FP |
| Outstanding at RD2? | Check if notice still outstanding at end of RD2 | Final payment check |
| Apply PS-MID | If outstanding, apply MID permanent suspension | Suspend notice |
| End | Processing complete | Flow exit point |

### Notice Creation Details

#### Hardcoded MINDEF Owner Details

| Field | Value | Description |
| --- | --- | --- |
| name | MINDEF | Ministry of Defence |
| id_type | B | Business entity type |
| id_no | T08GA0011B | MINDEF UEN |
| owner_driver_indicator | O | Owner record |
| offender_indicator | Y | Is offender |

#### Hardcoded MINDEF Address

| Field | Value |
| --- | --- |
| type_of_address | MINDEF_ADDRESS |
| bldg_name | Kranji Camp 3 |
| blk_hse_no | 151 |
| street_name | Choa Chu Kang Way |
| postal_code | 688248 |

### Double Booking Check (DBB) Criteria

All 5 conditions must match an existing notice to trigger PS-DBB:

| Criteria | Field | Match Type |
| --- | --- | --- |
| 1 | vehicle_no | Exact match |
| 2 | notice_date_and_time | Exact match |
| 3 | computer_rule_code | Exact match |
| 4 | parking_lot_no | Exact match (null = null) |
| 5 | pp_code | Exact match |

### Decision Logic

| ID | Decision | Condition | Yes Action | No Action |
| --- | --- | --- | --- | --- |
| D1 | Duplicate Notice? | All 5 DBB criteria match | Apply PS-DBB, End | Continue to Create Notice |
| D2 | AN Qualify? | Type O + meets AN criteria (OCMS 10) | AN Sub-flow | Prepare RD1 |
| D3 | Payment at RD1? | payment_status = 'PAID' at RD1 | Apply PS-FP, End | Prepare RD2 |
| D4 | Payment at RD2? | payment_status = 'PAID' at RD2 | Apply PS-FP, End | Check Outstanding |
| D5 | Outstanding at RD2? | Outstanding at end of RD2 | Apply PS-MID | Already Paid |

### Database Operations

| Step | Operation | Table | Action |
| --- | --- | --- | --- |
| DBB Check | SELECT | ocms_valid_offence_notice | Query by 5 DBB criteria |
| Create Notice | INSERT | ocms_valid_offence_notice | Insert notice at NPA stage |
| Create Notice | INSERT | ocms_offence_notice_owner_driver | Insert MINDEF owner details |
| Create Notice | INSERT | ocms_offence_notice_owner_driver_address | Insert MINDEF address |
| Create Notice | INSERT | ocms_offence_notice_detail | Insert notice details |
| Update RD1 | UPDATE | ocms_valid_offence_notice | Set last_processing_stage = 'RD1' |
| Update RD2 | UPDATE | ocms_valid_offence_notice | Set last_processing_stage = 'RD2' |
| PS-MID | UPDATE | ocms_valid_offence_notice | Set suspension_type = 'PS', epr_reason = 'MID' |
| PS-MID | INSERT | ocms_suspended_notice | Create suspension record |

### External System Integration

#### TOPPAN SFTP (Letter Generation)

| Field | Value |
| --- | --- |
| Integration Type | SFTP File Transfer |
| Vendor | TOPPAN (Printing) |
| Purpose | Send letter generation requests |
| Trigger | End of day CRON job |
| Letter Types | RD1 Letter, RD2 Letter, DN1 Letter, DN2 Letter |

---

### 4.3.1 Data Mapping

#### Database Data Mapping

| Zone | Database Table | Field Name | Description |
| --- | --- | --- | --- |
| Intranet | ocms_valid_offence_notice | vehicle_registration_type | Value = 'I' for Military |
| Intranet | ocms_valid_offence_notice | vehicle_no | Vehicle number (MID/MINDEF pattern) |
| Intranet | ocms_valid_offence_notice | last_processing_stage | Current stage (NPA/RD1/RD2) |
| Intranet | ocms_valid_offence_notice | next_processing_stage | Next stage in flow |
| Intranet | ocms_valid_offence_notice | suspension_type | PS when suspended |
| Intranet | ocms_valid_offence_notice | epr_reason_of_suspension | MID for military suspension |
| Intranet | ocms_offence_notice_owner_driver | name | MINDEF (hardcoded) |
| Intranet | ocms_offence_notice_owner_driver | id_no | T08GA0011B (hardcoded) |
| Intranet | ocms_offence_notice_owner_driver_address | postal_code | 688248 (hardcoded) |
| Intranet | ocms_suspended_notice | reason_of_suspension | MID / ANS / DBB / FP |
| Internet | eocms_valid_offence_notice | - | Synced copy for eService portal |

#### Input Parameters

| Parameter | Type | Description | Source |
| --- | --- | --- | --- |
| notice_no | String | Unique notice number | Notice creation |
| vehicle_no | String | Vehicle number (MID/MINDEF pattern) | Raw offence data |
| vehicle_registration_type | String | Must be 'I' for this flow | Detection result |
| offence_notice_type | String | O (On-street), E (ERP), U (UPL) | Raw offence data |
| computer_rule_code | String | Offence rule code | Raw offence data |
| pp_code | String | Car park code | Raw offence data |
| parking_lot_no | String | Parking lot number (optional) | Raw offence data |
| notice_date_and_time | DateTime | Date and time of offence | Raw offence data |

#### Stage Values

| Stage Code | Stage Name | Description |
| --- | --- | --- |
| NPA | Notice Pending Action | Initial stage after creation |
| RD1 | Reminder 1 | First reminder letter sent |
| RD2 | Reminder 2 | Second reminder letter sent |
| DN1 | Driver Notice 1 | First driver notice sent |
| DN2 | Driver Notice 2 | Second driver notice sent |

#### Suspension Types

| Type | Code | Description |
| --- | --- | --- |
| PS-MID | suspension_type='PS', reason='MID' | Permanent Suspension - Military |
| PS-ANS | suspension_type='PS', reason='ANS' | Permanent Suspension - Advisory Notice |
| PS-DBB | suspension_type='PS', reason='DBB' | Permanent Suspension - Double Booking |
| PS-FP | suspension_type='PS', reason='FP' | Permanent Suspension - Full Payment |

---

### 4.3.2 Success Outcome

- Military vehicle notice is successfully created with MINDEF owner details
- Notice progresses through stages: NPA → RD1 → RD2
- Reminder letters (RD1, RD2) are generated and sent to TOPPAN for printing
- At end of RD2 (if outstanding), PS-MID suspension is applied
- Notice processing stops after PS-MID - no court referral
- If payment received at any stage, PS-FP is applied and notice closes

---

### 4.3.3 Error Handling

#### Application Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Duplicate Notice Number | Notice number already exists | Return error - reject duplicate |
| DB Insert Failure | Failed to insert notice record | Log error, rollback transaction |
| SFTP Connection Error | Cannot connect to TOPPAN SFTP | Log error, retry in next CRON cycle |
| Invalid Vehicle Number | Vehicle number format invalid | Log warning, continue processing |

#### Condition-Based Error Handling

| Condition ID | Error Condition | Action |
| --- | --- | --- |
| MID-CRE-001 | Notice number already exists | Reject creation, return duplicate error |
| MID-DBB-001 | All 5 DBB criteria match existing notice | Apply PS-DBB, stop processing |
| MID-LTR-001 | SFTP transfer fails | Queue for retry in next CRON cycle |

---

## 4.4 Advisory Notice (AN) Sub-flow

<!-- Insert flow diagram here -->
![Advisory Notice Sub-flow](./images/section4-an-subflow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| CRON Start | AN Letter Generation Job triggers (end of day) | Job entry point |
| Query Notices for AN | Query notices with AN eligibility criteria | Database query |
| Records Found? | Decision - any notices qualify for AN? | Check result count |
| No Records | If no qualifying notices, end job | Job exit |
| Generate AN Letter PDF | Create Advisory Notice letter for each notice | PDF generation |
| Drop to SFTP | Upload PDFs to TOPPAN SFTP server | File transfer |
| Update Stage to RD1 | Set last_processing_stage = 'RD1' | Stage update |
| Apply PS-ANS | Apply Permanent Suspension - Advisory Notice | Suspend notice |
| End | AN processing complete | Flow exit point |

### AN Eligibility Criteria

| Criterion | Field | Condition |
| --- | --- | --- |
| 1 | vehicle_registration_type | = 'I' (Military) |
| 2 | an_flag | = 'Y' |
| 3 | last_processing_stage | = 'NPA' |
| 4 | next_processing_stage | = 'RD1' |
| 5 | next_processing_date | <= CURRENT_DATE |
| 6 | suspension_type | IS NULL |
| 7 | epr_reason_of_suspension | IS NULL |

### AN Query

```sql
SELECT * FROM ocms_valid_offence_notice
WHERE vehicle_registration_type = 'I'
  AND an_flag = 'Y'
  AND last_processing_stage = 'NPA'
  AND next_processing_stage = 'RD1'
  AND next_processing_date <= CURRENT_DATE
  AND suspension_type IS NULL
  AND epr_reason_of_suspension IS NULL
```

### Database Operations

| Step | Operation | Table | Action |
| --- | --- | --- | --- |
| Query | SELECT | ocms_valid_offence_notice | Get AN eligible notices |
| Update Stage | UPDATE | ocms_valid_offence_notice | Set last_processing_stage = 'RD1' |
| Apply PS-ANS | UPDATE | ocms_valid_offence_notice | Set suspension_type = 'PS', epr_reason = 'ANS' |
| Record Suspension | INSERT | ocms_suspended_notice | Create suspension record |

---

## 4.5 Furnish Driver/Hirer Sub-flow

<!-- Insert flow diagram here -->
![Furnish Driver/Hirer Sub-flow](./images/section4-furnish-subflow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | Vehicle Owner initiates furnish via eService Portal | Flow entry point |
| Submit Application | Owner submits Driver/Hirer particulars | Form submission |
| Store in Internet DB | Application stored with "furnish pending approval" status | Database insert |
| CRON Sync | Scheduled job syncs data to Intranet database | Data sync |
| Validate Particulars | Backend validates Driver/Hirer information | Auto-validation |
| Pass Validation? | Decision - does data pass auto-validation? | Validation check |
| Auto Approve | If validation passes, system approves automatically | Auto-approval |
| OIC Manual Review | If validation fails, route to OIC for manual review | Manual review |
| OIC Decision | OIC approves or rejects the application | Human decision |
| Reject | If OIC rejects, remove pending status and allow re-furnish | Rejection handling |
| Create Driver/Hirer Record | Insert approved Driver/Hirer into owner_driver table | Record creation |
| Driver or Hirer? | Determine if furnished person is Driver or Hirer | Route decision |
| Set Next Stage DN1 | If Driver, next stage is DN1 | Driver flow |
| Set Next Stage RD1 | If Hirer, next stage remains RD1 | Hirer flow |
| MHA/DataHive Check | Validate Driver/Hirer NRIC/FIN before RD2/DN2 | External validation |
| Continue to RD2/DN2 | Resume normal stage progression | Flow continues |
| End | Furnish process complete | Flow exit point |

### Furnish Eligibility by Stage

| Stage | Furnish Allowed | Reason |
| --- | --- | --- |
| NPA | No | Notice just created, not yet notified |
| ROV | No | Awaiting review |
| RD1 | Yes | Owner received first reminder |
| RD2 | Yes | Owner received second reminder |
| DN1 | No | Already furnished to Driver |
| DN2 | No | Already furnished to Driver |
| PS-MID | No | Notice suspended, no changes allowed |

### Furnish Application Fields

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| notice_no | String | Yes | Notice number |
| furnish_type | String | Yes | D (Driver) or H (Hirer) |
| name | String | Yes | Driver/Hirer name |
| id_type | String | Yes | NRIC or FIN |
| id_no | String | Yes | ID number |
| address_type | String | Yes | Address type |
| blk_hse_no | String | No | Block/house number |
| street_name | String | Yes | Street name |
| postal_code | String | Yes | Postal code |

### External System Integration

#### MHA/DataHive Check (Furnished Driver/Hirer Only)

| Field | Value |
| --- | --- |
| Integration Type | External API |
| System | MHA (for NRIC) / DataHive (for FIN) |
| Purpose | Validate Driver/Hirer identity |
| Trigger | Before RD2/DN2 stage processing |
| Note | Only called for furnished Driver/Hirer, not for MINDEF owner |

### Database Operations

| Step | Operation | Table | Zone | Action |
| --- | --- | --- | --- | --- |
| Submit | INSERT | eocms_furnish_application | Internet | Store application |
| Sync | SELECT/INSERT | ocms_furnish_application | Intranet | Copy from Internet |
| Create Record | INSERT | ocms_offence_notice_owner_driver | Intranet | Add Driver/Hirer |
| Create Address | INSERT | ocms_offence_notice_owner_driver_address | Intranet | Add address |
| Update Stage | UPDATE | ocms_valid_offence_notice | Intranet | Set next_processing_stage |

---

## 4.6 PS-MID Suspension Flow

<!-- Insert flow diagram here -->
![PS-MID Suspension Flow](./images/section4-ps-mid-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| CRON Start | End of RD2/DN2 daily job (11:59 PM) | Job entry point |
| Query VON Table | Get MID notices at RD2/DN2 without existing CRS suspension | Database query |
| Records Found? | Decision - any notices qualify for PS-MID? | Check result count |
| No Records | If no qualifying notices, end job | Job exit |
| For Each Notice | Loop through qualifying notices | Begin loop |
| Update VON Intranet | Set suspension_type='PS', epr_reason='MID' in Intranet | Intranet update |
| Update eVON Internet | Sync suspension status to Internet database | Internet sync |
| Insert Suspended Notice | Create record in ocms_suspended_notice table | Record creation |
| End Loop | Move to next notice | Loop control |
| End | PS-MID job complete, processing stops | Flow exit point |

### PS-MID Query Criteria

| Criterion | Field | Condition |
| --- | --- | --- |
| 1 | vehicle_registration_type | = 'I' (Military) |
| 2 | crs_reason_of_suspension | IS NULL |
| 3 | last_processing_stage | IN ('RD2', 'DN2') |
| 4 | next_processing_stage | IN ('RR3', 'DR3') |

### PS-MID Query

```sql
SELECT * FROM ocms_valid_offence_notice
WHERE vehicle_registration_type = 'I'
  AND crs_reason_of_suspension IS NULL
  AND last_processing_stage IN ('RD2', 'DN2')
  AND next_processing_stage IN ('RR3', 'DR3')
```

### PS-MID Update Fields

| Field | Value | Description |
| --- | --- | --- |
| suspension_type | PS | Permanent Suspension |
| epr_reason_of_suspension | MID | Military vehicle reason |
| epr_date_of_suspension | CURRENT_TIMESTAMP | Suspension date/time |

### Suspended Notice Record

| Field | Value |
| --- | --- |
| notice_no | Notice number |
| suspension_type | PS |
| reason_of_suspension | MID |
| date_of_suspension | CURRENT_TIMESTAMP |
| suspension_source | OCMS |
| sr_no | Running number |
| officer_authorising_suspension | System or OIC name |

### Daily Re-check (DipMidForRecheckJob)

| Attribute | Value |
| --- | --- |
| Schedule | Daily at 11:59 PM |
| Purpose | Re-apply PS-MID if accidentally revived |
| Scope | Vehicle types D, I, F at RD2/DN2 without PS |

The daily re-check ensures that if a Military vehicle notice is accidentally revived (suspension removed), the PS-MID is re-applied before the notice can progress to RR3/DR3 (Court Referral stages).

### Re-check Query

```sql
SELECT * FROM ocms_valid_offence_notice
WHERE vehicle_registration_type IN ('D', 'I', 'F')
  AND last_processing_stage IN ('RD2', 'DN2')
  AND suspension_type IS NULL
```

### Database Operations

| Step | Operation | Table | Zone | Action |
| --- | --- | --- | --- | --- |
| Query | SELECT | ocms_valid_offence_notice | Intranet | Get PS-MID candidates |
| Update | UPDATE | ocms_valid_offence_notice | Intranet | Set PS-MID suspension |
| Sync | UPDATE | eocms_valid_offence_notice | Internet | Sync suspension status |
| Record | INSERT | ocms_suspended_notice | Intranet | Create suspension record |

---

## Appendix

### A. Military Vehicle Patterns

| Pattern | Example | Description |
| --- | --- | --- |
| MID prefix | MID2221 | Vehicle number starts with 'MID' |
| MID suffix | 2221MID | Vehicle number ends with 'MID' |
| MINDEF prefix | MINDEF123 | Vehicle number starts with 'MINDEF' |
| MINDEF suffix | 123MINDEF | Vehicle number ends with 'MINDEF' |

### B. Stage Progression Matrix

| Current Stage | Next Stage (Normal) | Next Stage (Payment) | Next Stage (Furnish Driver) | Next Stage (Furnish Hirer) |
| --- | --- | --- | --- | --- |
| NPA | RD1 | PS-FP | - | - |
| RD1 | RD2 | PS-FP | DN1 | RD2 |
| RD2 | PS-MID | PS-FP | - | PS-MID |
| DN1 | DN2 | PS-FP | - | - |
| DN2 | PS-MID | PS-FP | - | - |

### C. Suspension Types for Military Vehicles

| Suspension | Code | Trigger | Effect |
| --- | --- | --- | --- |
| PS-MID | suspension_type='PS', reason='MID' | Outstanding at RD2/DN2 | Processing stops |
| PS-ANS | suspension_type='PS', reason='ANS' | AN Letter issued | Processing stops |
| PS-DBB | suspension_type='PS', reason='DBB' | Double booking detected | Notice rejected |
| PS-FP | suspension_type='PS', reason='FP' | Full payment received | Notice closed |
| TS on PS-MID | suspension_type='TS' + existing PS-MID | Temporary suspension while PS-MID | Temporarily lifted |

### D. CRON Jobs Summary

| Job Name | Schedule | Purpose |
| --- | --- | --- |
| PrepareForRD1Job | End of day | Process NPA → RD1 |
| PrepareForRD2Job | End of RD1 | Process RD1 → RD2 |
| PrepareForDN1Job | End of day | Process furnished notices → DN1 |
| PrepareForDN2Job | End of DN1 | Process DN1 → DN2 |
| DipMidForRecheckJob | 11:59 PM daily | Re-apply PS-MID at RD2/DN2 |
| ANLetterGenerationJob | End of day | Generate AN Letters |
| SuspendMIDNoticesJob | End of RD2/DN2 | Apply PS-MID suspension |
| FurnishSyncJob | Scheduled | Sync furnish data Internet → Intranet |

### E. Reference Documents

| Document | Section | Description |
| --- | --- | --- |
| OCMS 14 FD | Section 5 | Notice Processing Flow for Military Vehicles |
| OCMS 14 TD | Section 1 | Detecting Vehicle Registration Type |
| OCMS 10 FD | - | Advisory Notice criteria and processing |
| OCMS 11 TD | - | Notice Processing Flow for Local Vehicles |

---

**End of Section 4**

# API Plan: Military Vehicle Notice Processing

## Overview

| Attribute | Value |
| --- | --- |
| Feature Name | Notice Processing Flow for Military Vehicles |
| Version | v1.1 |
| Author | Claude |
| Created Date | 15/01/2026 |
| Last Updated | 27/01/2026 |
| Status | Draft |
| FD Reference | OCMS 14 - Section 5 |
| TD Reference | OCMS 14 - Section 4 |

---

## 1. Purpose

This document outlines the API and service calls involved in the Military Vehicle Notice Processing flow within OCMS. The function handles notices issued to vehicles with registration type "I" (Military/MID), which are identified by MID or MINDEF prefix/suffix patterns.

---

## 2. API Overview

### 2.1 Internal Service Calls

Military Vehicle Notice Processing is an **internal workflow** within OCMS. It uses the following internal services:

| Service | Type | Purpose |
| --- | --- | --- |
| SpecialVehUtils | Internal Library | Detect vehicle registration type (MID/MINDEF) |
| CreateNoticeService | Internal Service | Create Military Vehicle Notice at NPA stage |
| PermanentSuspensionHelper | Internal Service | Apply PS-MID suspension |
| TemporarySuspensionHelper | Internal Service | Handle TS on PS-MID notices |
| DipMidForRecheckHelper | CRON Helper | Re-apply PS-MID at RD2/DN2 |

---

## 3. Vehicle Registration Type Detection

### 3.1 Military Vehicle Detection

| Attribute | Value |
| --- | --- |
| Type | Internal Library Call |
| Class | `SpecialVehUtils` |
| Method | `checkVehregistration(vehicleNo, sourceProvidedType)` |
| Purpose | Detect if vehicle is Military (Type I) |

#### Input

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| vehicleNo | String | Yes | Vehicle registration number |
| sourceProvidedType | String | No | Type provided by data source |

#### Output

| Return Type | Description |
| --- | --- |
| String | Vehicle registration type code |

#### Vehicle Type Codes

| Code | Type | Description |
| --- | --- | --- |
| I | Military | MID/MINDEF vehicle |
| D | Diplomat | Diplomatic vehicle |
| V | VIP | VIP parking label vehicle |
| F | Foreign | Foreign vehicle |
| S | Local | Singapore registered vehicle |
| X | UPL | Unlicensed parking lot vehicle |

#### Military Vehicle Detection Logic

```
IF vehicleNo starts with "MID" OR "MINDEF"
   OR vehicleNo ends with "MID" OR "MINDEF"
THEN return "I" (Military)
```

---

## 4. Notice Creation API

### 4.1 Create Military Vehicle Notice

| Attribute | Value |
| --- | --- |
| Type | Internal Service |
| Service | CreateNoticeService |
| Method | `createNotice(noticeData)` |
| Purpose | Create notice with hardcoded MINDEF address |

#### Input - Notice Data

| Field | Type | Required | Description | Sample Value |
| --- | --- | --- | --- | --- |
| notice_no | String | Yes | Notice number | 500500303J |
| vehicle_no | String | Yes | Vehicle number | MID2221 |
| vehicle_registration_type | String | Yes | Must be 'I' for Military | I |
| offence_notice_type | String | Yes | O, E, or U | O |
| computer_rule_code | integer | Yes | Offence rule code | 10300 |
| pp_code | String | Yes | Car park code | A0005 |
| parking_lot_no | String | No | Parking lot number | 12 |
| notice_date_and_time | DateTime | Yes | Offence date/time | 2025-03-21 19:00:02 |

#### Output - Created Notice

| Field | Value | Description |
| --- | --- | --- |
| last_processing_stage | NPA | Initial stage |
| next_processing_stage | RD1 | Next stage |
| next_processing_date | Current Date | Same day processing |
| payment_acceptance_allowed | Y | Notice is payable |

#### Hardcoded MINDEF Owner Details

| Field | Value |
| --- | --- |
| name | MINDEF |
| id_type | B |
| id_no | T08GA0011B |
| owner_driver_indicator | O |
| offender_indicator | Y |

#### Hardcoded MINDEF Address

| Field | Value |
| --- | --- |
| type_of_address | MINDEF_ADDRESS |
| bldg_name | Kranji Camp 3 |
| blk_hse_no | 151 |
| street_name | Choa Chu Kang Way |
| postal_code | 688248 |

---

## 5. Double Booking Check API

### 5.1 Check Duplicate Notice

| Attribute | Value |
| --- | --- |
| Type | Database Query |
| Table | ocms_valid_offence_notice |
| Purpose | Detect duplicate notices for PS-DBB suspension |

#### Query Criteria

| Field | Comparison | Description |
| --- | --- | --- |
| vehicle_no | = | Same vehicle number |
| notice_date_and_time | = | Same offence date/time |
| computer_rule_code | = | Same rule code |
| parking_lot_no | = (null = null) | Same lot (blank matches blank) |
| pp_code | = | Same car park code |

#### Output

| Result | Action |
| --- | --- |
| Duplicate Found | Apply PS-DBB suspension |
| No Duplicate | Continue processing |

---

## 6. Advisory Notice (AN) Check API

### 6.1 AN Qualification Check

| Attribute | Value |
| --- | --- |
| Type | Internal Service |
| Purpose | Determine if Type O notice qualifies for Advisory Notice |

#### Query Criteria for AN Letter Generation

```sql
SELECT notice_no, vehicle_no, vehicle_registration_type,
       an_flag, last_processing_stage, next_processing_stage,
       next_processing_date, offence_notice_type
FROM ocms_valid_offence_notice
WHERE vehicle_registration_type = 'I'
  AND an_flag = 'Y'
  AND last_processing_stage = 'NPA'
  AND next_processing_stage = 'RD1'
  AND next_processing_date <= CURRENT_DATE
  AND suspension_type IS NULL
  AND epr_reason_of_suspension IS NULL
```

#### Output

| Result | Action |
| --- | --- |
| Qualifies for AN | Set an_flag = 'Y', generate AN Letter, apply PS-ANS |
| Does Not Qualify | Continue to RD1 processing |

---

## 7. Stage Processing APIs

### 7.1 Prepare for RD1

| Attribute | Value |
| --- | --- |
| Type | CRON Job |
| Schedule | End of day |
| Purpose | Generate RD1 Reminder Letter |

#### Process Steps

1. Query notices at NPA with next_processing_stage = RD1
2. Add notices to RD1 Letter generation list
3. Send list to TOPPAN (printing vendor) via SFTP
4. Update last_processing_stage = RD1
5. Set next_processing_stage = RD2

### 7.2 Prepare for RD2 - MHA & DataHive Check

| Attribute | Value |
| --- | --- |
| Type | CRON Job |
| Schedule | Before RD2 letter generation |
| Purpose | Validate offender particulars with MHA and DataHive |

#### Process Steps

1. Query notices at RD1 with furnished Driver/Hirer
2. For each notice with Driver/Hirer:
   - Call MHA API to validate NRIC/FIN
   - Call DataHive API to get latest particulars
3. Update offender details if changed
4. Log validation results

#### Query Criteria

```sql
SELECT * FROM ocms_valid_offence_notice von
INNER JOIN ocms_offence_notice_owner_driver od
  ON von.notice_no = od.notice_no
WHERE von.vehicle_registration_type = 'I'
  AND von.last_processing_stage = 'RD1'
  AND od.owner_driver_indicator = 'D'
```

### 7.3 Prepare for RD2 - Letter Generation

| Attribute | Value |
| --- | --- |
| Type | CRON Job |
| Schedule | End of RD1 stage |
| Purpose | Generate RD2 Reminder Letter |

#### Process Steps

1. Query outstanding notices at RD1
2. Add notices to RD2 Letter generation list
3. Send list to TOPPAN via SFTP
4. Update last_processing_stage = RD2
5. Set next_processing_stage = RR3

### 7.4 Prepare for DN1 (Type E / Furnished Driver)

| Attribute | Value |
| --- | --- |
| Type | CRON Job |
| Schedule | End of day |
| Purpose | Generate DN1 Letter for Driver/Hirer |

### 7.5 Prepare for DN2 - MHA & DataHive Check

| Attribute | Value |
| --- | --- |
| Type | CRON Job |
| Schedule | Before DN2 letter generation |
| Purpose | Validate Driver particulars with MHA and DataHive |

#### Process Steps

1. Query notices at DN1 stage
2. For each notice:
   - Call MHA API to validate Driver NRIC/FIN
   - Call DataHive API to get latest particulars
3. Update Driver details if changed
4. Log validation results

#### Query Criteria

```sql
SELECT * FROM ocms_valid_offence_notice von
INNER JOIN ocms_offence_notice_owner_driver od
  ON von.notice_no = od.notice_no
WHERE von.vehicle_registration_type = 'I'
  AND von.last_processing_stage = 'DN1'
  AND od.owner_driver_indicator = 'D'
```

### 7.6 Prepare for DN2 - Letter Generation

| Attribute | Value |
| --- | --- |
| Type | CRON Job |
| Schedule | End of DN1 stage |
| Purpose | Generate DN2 Letter |

---

## 8. Furnish Driver/Hirer API

### 8.1 Submit Furnish Application

| Attribute | Value |
| --- | --- |
| Type | eService Portal API |
| Source | Internet (Public) |
| Purpose | Vehicle Owner submits Driver/Hirer particulars |

#### Input

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| notice_no | String | Yes | Notice number |
| driver_name | String | Yes | Driver/Hirer name |
| driver_id_type | String | Yes | ID type (NRIC/FIN) |
| driver_id_no | String | Yes | ID number |
| driver_address | Object | Yes | Driver address details |

#### Process Flow

1. Owner submits via eService Portal
2. OCMS adds to Internet database with "furnish pending approval"
3. Scheduled job syncs to Intranet
4. Backend validates Driver/Hirer particulars
5. Auto-approve if criteria met, else OIC manual review
6. If approved: Create Driver/Hirer record, change next stage to DN1/RD1
7. If rejected: Remove "furnish pending approval" status

### 8.2 Sync Furnish Data (Internet → Intranet)

| Attribute | Value |
| --- | --- |
| Type | CRON Job |
| Direction | Internet → Intranet |
| Purpose | Sync furnished Driver/Hirer application from Internet to Intranet |

#### Tables Involved

| Table | Database | Operation |
| --- | --- | --- |
| eocms_furnish_application | Internet | SELECT |
| ocms_furnish_application | Intranet | INSERT/UPDATE |

### 8.3 Sync Furnish Rejection (Intranet → Internet)

| Attribute | Value |
| --- | --- |
| Type | CRON Job |
| Direction | Intranet → Internet |
| Purpose | Sync furnish rejection status back to Internet for eService display |

#### Process Steps

1. Query rejected furnish applications in Intranet
2. Update corresponding records in Internet database
3. Remove "furnish pending approval" flag
4. Allow re-furnish via eService

#### Query Criteria

```sql
SELECT * FROM ocms_furnish_application
WHERE approval_status = 'REJECTED'
  AND is_sync_to_internet = 'N'
```

#### Update Internet

```sql
UPDATE eocms_furnish_application
SET approval_status = 'REJECTED',
    rejection_reason = :rejection_reason,
    upd_date = CURRENT_TIMESTAMP,
    upd_user_id = 'OCMS_SYNC'
WHERE txn_no = :txn_no

UPDATE eocms_valid_offence_notice
SET furnish_pending_approval = 'N',
    upd_date = CURRENT_TIMESTAMP,
    upd_user_id = 'OCMS_SYNC'
WHERE notice_no = :notice_no
```

#### Tables Involved

| Table | Database | Operation |
| --- | --- | --- |
| ocms_furnish_application | Intranet | SELECT |
| eocms_furnish_application | Internet | UPDATE |
| eocms_valid_offence_notice | Internet | UPDATE |

---

## 9. Type U - OIC Compoundability Decision API

### 9.1 OIC Mark Notice as Compoundable

| Attribute | Value |
| --- | --- |
| Type | Staff Portal API |
| Method | POST |
| Endpoint | /api/notices/{notice_no}/compoundability |
| Purpose | OIC marks Type U notice as compoundable or non-compoundable |

#### Input

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| notice_no | String | Yes | Notice number |
| is_compoundable | Boolean | Yes | True if compoundable |
| officer_id | String | Yes | OIC user ID |
| remarks | String | No | Decision remarks |

#### Process Steps

1. Validate notice exists and is Type U Military
2. Validate notice is at NPA stage (pending decision)
3. Update notice with compoundability decision
4. If compoundable: Set next_processing_stage = 'DN1'
5. If not compoundable: Apply PS-MID suspension

#### Update Fields (Compoundable = Yes)

```sql
UPDATE ocms_valid_offence_notice
SET compoundability_flag = 'Y',
    compoundability_decision_date = CURRENT_TIMESTAMP,
    compoundability_officer = :officer_id,
    next_processing_stage = 'DN1',
    upd_date = CURRENT_TIMESTAMP,
    upd_user_id = :officer_id
WHERE notice_no = :notice_no
  AND vehicle_registration_type = 'I'
  AND offence_notice_type = 'U'
  AND last_processing_stage = 'NPA'
```

#### Update Fields (Compoundable = No)

```sql
-- Apply PS-MID suspension
UPDATE ocms_valid_offence_notice
SET compoundability_flag = 'N',
    compoundability_decision_date = CURRENT_TIMESTAMP,
    compoundability_officer = :officer_id,
    suspension_type = 'PS',
    epr_reason_of_suspension = 'MID',
    epr_date_of_suspension = CURRENT_TIMESTAMP,
    upd_date = CURRENT_TIMESTAMP,
    upd_user_id = :officer_id
WHERE notice_no = :notice_no
```

---

## 10. Suspension APIs

### 10.1 Apply PS-MID Suspension

| Attribute | Value |
| --- | --- |
| Type | Internal Service |
| Service | PermanentSuspensionHelper |
| Purpose | Apply permanent suspension for Military Vehicles |

#### Trigger Conditions

- Notice is outstanding at end of RD2 or DN2
- Vehicle registration type = 'I'
- No existing CRS reason of suspension

#### Query for PS-MID Application

**Note:** Query uses `crs_reason_of_suspension` to check no CRS/court suspension exists. Update uses `suspension_type` and `epr_reason_of_suspension`.

```sql
SELECT notice_no, vehicle_no, vehicle_registration_type,
       last_processing_stage, next_processing_stage,
       suspension_type, crs_reason_of_suspension, epr_reason_of_suspension
FROM ocms_valid_offence_notice
WHERE vehicle_registration_type = 'I'
  AND crs_reason_of_suspension IS NULL  -- No court/CRS suspension
  AND suspension_type IS NULL           -- No existing PS/TS
  AND last_processing_stage IN ('RD2', 'DN2')
  AND next_processing_stage IN ('RR3', 'DR3')
  AND next_processing_date <= DATEADD(day, 1, CURRENT_DATE)
```

#### Update Fields

| Field | Value | Description |
| --- | --- | --- |
| suspension_type | PS | Permanent Suspension |
| epr_reason_of_suspension | MID | MINDEF Vehicle |
| epr_date_of_suspension | Current DateTime | Suspension timestamp |

#### Insert into ocms_suspended_notice

| Field | Value |
| --- | --- |
| notice_no | Notice number |
| suspension_type | PS |
| reason_of_suspension | MID |
| date_of_suspension | Current DateTime |
| suspension_source | OCMS |
| sr_no | Running number |
| officer_authorising_suspension | System or OIC name |

### 10.2 Daily Re-check CRON (DipMidForRecheckJob)

| Attribute | Value |
| --- | --- |
| Type | CRON Job |
| Schedule | Daily at 11:59 PM |
| Purpose | Re-apply PS-MID if accidentally revived |

#### Query Criteria

```sql
SELECT notice_no, vehicle_no, vehicle_registration_type,
       last_processing_stage, next_processing_stage,
       suspension_type, epr_reason_of_suspension
FROM ocms_valid_offence_notice
WHERE vehicle_registration_type IN ('D', 'I', 'F')
  AND last_processing_stage IN ('RD2', 'DN2')
  AND suspension_type IS NULL
```

#### Process

1. Query notices at RD2/DN2 without active PS
2. For type 'I': Re-apply PS-MID
3. Prevent progression to RR3/DR3

---

## 11. Letter Generation API (SFTP)

### 11.1 Send Letters to TOPPAN

| Attribute | Value |
| --- | --- |
| Type | SFTP Interface |
| Vendor | TOPPAN (Printing) |
| Purpose | Send letter generation files |

#### Letter Types

| Letter | Stage | Purpose |
| --- | --- | --- |
| RD1 Letter | RD1 | First Reminder Letter |
| RD2 Letter | RD2 | Second Reminder Letter |
| DN1 Letter | DN1 | First Driver Notice |
| DN2 Letter | DN2 | Second Driver Notice |
| AN Letter | NPA | Advisory Notice |

---

## 12. Database Operations Summary

### 12.1 Tables Modified

| Table | Operations | Purpose |
| --- | --- | --- |
| ocms_valid_offence_notice | INSERT, UPDATE | Notice records (Intranet) |
| eocms_valid_offence_notice | INSERT, UPDATE | Notice records (Internet) |
| ocms_offence_notice_owner_driver | INSERT | Owner/Driver details |
| ocms_offence_notice_owner_driver_address | INSERT | MINDEF address |
| ocms_offence_notice_detail | INSERT | Notice additional details |
| ocms_suspended_notice | INSERT | Suspension records |
| ocms_furnish_application | INSERT, UPDATE | Furnish requests |

### 12.2 Sync Operations

| Direction | Tables | Trigger |
| --- | --- | --- |
| Intranet → Internet | ocms_valid_offence_notice | Notice creation, stage changes |
| Internet → Intranet | ocms_furnish_application | Furnish submission |

---

## 13. External System Integrations

### 13.1 Systems NOT Called for Military Vehicles

| System | Reason |
| --- | --- |
| LTA VRLS | Military vehicles not in LTA registry |
| MHA | Bypass local ID check |
| DataHive | Bypass FIN/company profile check |
| eNotification | ENA stage bypassed |

### 13.2 MHA/DataHive Check (Furnished Driver/Hirer Only)

| Attribute | Value |
| --- | --- |
| Trigger | After furnish approval, before RD2/DN2 |
| Purpose | Validate Driver/Hirer particulars |

---

## 14. CRON Jobs Summary

| Job Name | Schedule | Purpose |
| --- | --- | --- |
| PrepareForRD1Job | End of day | Process NPA → RD1 |
| PrepareForRD2MhaCheckJob | Before RD2 letter gen | MHA/DataHive check for Driver/Hirer |
| PrepareForRD2Job | End of RD1 | Process RD1 → RD2 |
| PrepareForDN1Job | End of day | Process furnished notices → DN1 |
| PrepareForDN2MhaCheckJob | Before DN2 letter gen | MHA/DataHive check for Driver |
| PrepareForDN2Job | End of DN1 | Process DN1 → DN2 |
| DipMidForRecheckJob | 11:59 PM daily | Re-apply PS-MID at RD2/DN2 |
| ANLetterGenerationJob | End of day | Generate AN Letters |
| SuspendMIDNoticesJob | End of RD2/DN2 | Apply PS-MID suspension |
| SyncFurnishToIntranetJob | Periodic | Sync furnish from Internet → Intranet |
| SyncFurnishRejectionJob | Periodic | Sync rejection from Intranet → Internet |

---

## 15. Data Mapping

### 15.1 Database Tables Affected

| Table | Purpose | Key Fields |
| --- | --- | --- |
| ocms_valid_offence_notice | Store notice and suspension status | suspension_type, epr_reason_of_suspension, epr_date_of_suspension |
| ocms_suspended_notice | Suspension history records | reason_of_suspension, date_of_suspension, process_indicator |
| ocms_offence_notice_owner_driver | Store offender particulars | name, id_type, id_no, offender_indicator |
| ocms_offence_notice_owner_driver_addr | Store offender address | type_of_address, bldg_name, street_name, postal_code |

### 15.2 Field Mapping - ocms_valid_offence_notice (Suspension Fields)

| Database Field | Type | Value | Description |
| --- | --- | --- | --- |
| notice_no | varchar(10) | From notice | Notice number (PK) |
| vehicle_registration_type | varchar(1) | 'I' | Military vehicle |
| suspension_type | varchar(2) | 'PS' | Permanent Suspension |
| epr_reason_of_suspension | varchar(3) | 'MID' or 'ANS' | EPR suspension reason code |
| epr_date_of_suspension | datetime2(7) | Current timestamp | When EPR suspension applied |
| crs_reason_of_suspension | varchar(3) | NULL | CRS suspension (court-related) |
| due_date_of_revival | datetime2(7) | NULL for PS | Anticipated revival date |
| last_processing_stage | varchar(3) | 'RD2' or 'DN2' | Stage when suspended |
| next_processing_stage | varchar(3) | 'RR3' or 'DR3' | Stage blocked by PS |
| next_processing_date | datetime2(7) | Date | Next processing date |
| upd_date | datetime2(7) | Current timestamp | Record update timestamp |
| upd_user_id | varchar(50) | 'ocmsiz_app_conn' | User who updated |

### 15.3 Field Mapping - ocms_suspended_notice

| Database Field | Type | Value | Description |
| --- | --- | --- | --- |
| notice_no | varchar(10) | From notice | Notice number (PK) |
| date_of_suspension | datetime2(7) | Current timestamp | When suspension applied (PK) |
| sr_no | integer | Auto-generated | Suspension record number (PK) |
| suspension_source | varchar(4) | 'OCMS' | Source of suspension (NOT NULL) |
| suspension_type | varchar(2) | 'PS' | Permanent Suspension (NOT NULL) |
| reason_of_suspension | varchar(3) | 'MID' or 'ANS' | Based on suspension type (NOT NULL) |
| officer_authorising_suspension | varchar(50) | 'SYSTEM' or OIC ID | Officer who authorized (NOT NULL) |
| due_date_of_revival | datetime2(7) | NULL | Anticipated revival date (PS never auto-revives) |
| suspension_remarks | varchar(200) | NULL | Optional remarks |
| date_of_revival | datetime2(7) | NULL | Actual revival date (set when revived) |
| revival_reason | varchar(3) | NULL | Reason for revival (e.g., 'PSR') |
| officer_authorising_revival | varchar(50) | NULL | Officer who authorized revival |
| revival_remarks | varchar(200) | NULL | Remarks for revival |
| process_indicator | varchar(64) | 'dip_mid_for_recheck' | Indicates source process (CRON job name) |
| cre_date | datetime2(7) | Current timestamp | Record creation timestamp |
| cre_user_id | varchar(50) | 'ocmsiz_app_conn' | User who created |
| upd_date | datetime2(7) | NULL | Record update timestamp |
| upd_user_id | varchar(50) | NULL | User who updated |

### 15.4 Field Mapping - ocms_offence_notice_owner_driver

| Database Field | Type | Description | Sample Value |
| --- | --- | --- | --- |
| notice_no | varchar(10) | Notice number (PK) | 500500303J |
| owner_driver_indicator | varchar(1) | 'O'=Owner, 'H'=Hirer, 'D'=Driver (PK) | O |
| name | varchar(66) | Full name | MINDEF |
| id_type | varchar(1) | ID type | B |
| id_no | varchar(12) | NRIC/FIN/UEN number | T08GA0011B |
| offender_indicator | varchar(1) | 'Y'=Current offender, 'N'=Not current | Y |
| life_status | varchar(1) | 'A'=Alive, 'D'=Deceased | A |
| date_of_death | datetime2(7) | Date of death if deceased | NULL |
| email_addr | varchar(320) | Email address | NULL |
| offender_tel_code | varchar(3) | Phone country code | NULL |
| offender_tel_no | varchar(12) | Phone number | NULL |
| cre_date | datetime2(7) | Record creation timestamp | Current timestamp |
| cre_user_id | varchar(50) | User who created | ocmsiz_app_conn |
| upd_date | datetime2(7) | Record update timestamp | NULL |
| upd_user_id | varchar(50) | User who updated | NULL |

### 15.5 Field Mapping - ocms_offence_notice_owner_driver_addr

| Database Field | Type | Description | Sample Value |
| --- | --- | --- | --- |
| notice_no | varchar(10) | Notice number (PK) | 500500303J |
| owner_driver_indicator | varchar(1) | O/H/D (PK) | O |
| type_of_address | varchar(20) | Address type (PK) | MINDEF_ADDRESS |
| bldg_name | varchar(65) | Building name | Kranji Camp 3 |
| blk_hse_no | varchar(10) | Block/house number | 151 |
| floor_no | varchar(3) | Floor number | NULL |
| unit_no | varchar(5) | Unit number | NULL |
| postal_code | varchar(6) | Postal code | 688248 |
| street_name | varchar(32) | Street name | Choa Chu Kang Way |
| cre_date | datetime2(7) | Record creation timestamp | Current timestamp |
| cre_user_id | varchar(50) | User who created | ocmsiz_app_conn |

---

## 16. Changelog

| Version | Date | Author | Changes |
| --- | --- | --- | --- |
| 1.0 | 15/01/2026 | Claude | Initial version based on FD Section 5 |
| 1.1 | 27/01/2026 | Claude | Data Dictionary alignment: Added complete field mappings (Section 15), fixed computer_rule_code type (String→integer), added suspension_source and process_indicator fields, added audit fields, verified field names against intranet.json |

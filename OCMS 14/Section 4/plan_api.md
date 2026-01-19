# API Plan: Military Vehicle Notice Processing

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
| computer_rule_code | String | Yes | Offence rule code | 10300 |
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
SELECT * FROM ocms_valid_offence_notice
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

### 7.2 Prepare for RD2

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

### 7.3 Prepare for DN1 (Type E / Furnished Driver)

| Attribute | Value |
| --- | --- |
| Type | CRON Job |
| Schedule | End of day |
| Purpose | Generate DN1 Letter for Driver/Hirer |

### 7.4 Prepare for DN2

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

### 8.2 Sync Furnish Data

| Attribute | Value |
| --- | --- |
| Type | CRON Job |
| Purpose | Sync furnished Driver/Hirer from Internet to Intranet |

---

## 9. Suspension APIs

### 9.1 Apply PS-MID Suspension

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

```sql
SELECT * FROM ocms_valid_offence_notice
WHERE vehicle_registration_type = 'I'
  AND crs_reason_of_suspension IS NULL
  AND last_processing_stage IN ('RD2', 'DN2')
  AND next_processing_stage IN ('RR3', 'DR3')
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

### 9.2 Daily Re-check CRON (DipMidForRecheckJob)

| Attribute | Value |
| --- | --- |
| Type | CRON Job |
| Schedule | Daily at 11:59 PM |
| Purpose | Re-apply PS-MID if accidentally revived |

#### Query Criteria

```sql
SELECT * FROM ocms_valid_offence_notice
WHERE vehicle_registration_type IN ('D', 'I', 'F')
  AND last_processing_stage IN ('RD2', 'DN2')
  AND suspension_type IS NULL
```

#### Process

1. Query notices at RD2/DN2 without active PS
2. For type 'I': Re-apply PS-MID
3. Prevent progression to RR3/DR3

---

## 10. Letter Generation API (SFTP)

### 10.1 Send Letters to TOPPAN

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

## 11. Database Operations Summary

### 11.1 Tables Modified

| Table | Operations | Purpose |
| --- | --- | --- |
| ocms_valid_offence_notice | INSERT, UPDATE | Notice records (Intranet) |
| eocms_valid_offence_notice | INSERT, UPDATE | Notice records (Internet) |
| ocms_offence_notice_owner_driver | INSERT | Owner/Driver details |
| ocms_offence_notice_owner_driver_address | INSERT | MINDEF address |
| ocms_offence_notice_detail | INSERT | Notice additional details |
| ocms_suspended_notice | INSERT | Suspension records |
| ocms_furnish_application | INSERT, UPDATE | Furnish requests |

### 11.2 Sync Operations

| Direction | Tables | Trigger |
| --- | --- | --- |
| Intranet → Internet | ocms_valid_offence_notice | Notice creation, stage changes |
| Internet → Intranet | ocms_furnish_application | Furnish submission |

---

## 12. External System Integrations

### 12.1 Systems NOT Called for Military Vehicles

| System | Reason |
| --- | --- |
| LTA VRLS | Military vehicles not in LTA registry |
| MHA | Bypass local ID check |
| DataHive | Bypass FIN/company profile check |
| eNotification | ENA stage bypassed |

### 12.2 MHA/DataHive Check (Furnished Driver/Hirer Only)

| Attribute | Value |
| --- | --- |
| Trigger | After furnish approval, before RD2/DN2 |
| Purpose | Validate Driver/Hirer particulars |

---

## 13. CRON Jobs Summary

| Job Name | Schedule | Purpose |
| --- | --- | --- |
| PrepareForRD1Job | End of day | Process NPA → RD1 |
| PrepareForRD2Job | End of RD1 | Process RD1 → RD2 |
| PrepareForDN1Job | End of day | Process furnished notices → DN1 |
| PrepareForDN2Job | End of DN1 | Process DN1 → DN2 |
| DipMidForRecheckJob | 11:59 PM daily | Re-apply PS-MID at RD2/DN2 |
| ANLetterGenerationJob | End of day | Generate AN Letters |
| SuspendMIDNoticesJob | End of RD2/DN2 | Apply PS-MID suspension |

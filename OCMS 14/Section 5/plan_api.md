# API Plan: Diplomatic Vehicle Notice Processing

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

## 1. Purpose

This document outlines the API and service calls involved in the Diplomatic Vehicle Notice Processing flow within OCMS. The function handles notices issued to vehicles with registration type "D" (Diplomatic), which are identified by Prefix "S" and Suffix "CC", "CD", "TC", or "TE".

**Key Differences from Standard Flow:**
- Bypasses ENA stage (no SMS/email notifications)
- Includes LTA, MHA, and DataHive checks for owner information
- Outstanding notices at RD2/DN2 are suspended with PS-DIP
- Notices remain payable but not furnishable after PS-DIP

---

## 2. API Overview

### 2.1 Internal Service Calls

Diplomatic Vehicle Notice Processing uses the following internal services:

| Service | Type | Purpose |
| --- | --- | --- |
| SpecialVehUtils | Internal Library | Detect vehicle registration type (Diplomatic) |
| CreateNoticeService | Internal Service | Create Diplomatic Vehicle Notice at NPA stage |
| PermanentSuspensionHelper | Internal Service | Apply PS-DIP suspension |
| TemporarySuspensionHelper | Internal Service | Handle TS on PS-DIP notices |
| DipMidForRecheckHelper | CRON Helper | Re-apply PS-DIP at RD2/DN2 |

### 2.2 External System Integrations

| System | Type | Purpose |
| --- | --- | --- |
| LTA VRLS | External API | Vehicle ownership and registration details |
| MHA | External API | Local ID check, address verification |
| DataHive | External API | FIN check, company profile, address |
| TOPPAN | SFTP | Letter printing vendor |

---

## 3. Vehicle Registration Type Detection

### 3.1 Diplomatic Vehicle Detection

| Attribute | Value |
| --- | --- |
| Type | Internal Library Call |
| Class | `SpecialVehUtils` |
| Method | `checkVehregistration(vehicleNo, sourceProvidedType)` |
| Purpose | Detect if vehicle is Diplomatic (Type D) |

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
| D | Diplomat | Diplomatic vehicle |
| I | Military | MID/MINDEF vehicle |
| V | VIP | VIP parking label vehicle |
| F | Foreign | Foreign vehicle |
| S | Local | Singapore registered vehicle |
| X | UPL | Unlicensed parking lot vehicle |

#### Diplomatic Vehicle Detection Logic

```
IF vehicleNo prefix = "S"
   AND vehicleNo suffix IN ("CC", "CD", "TC", "TE")
THEN return "D" (Diplomatic)
```

#### Sample Diplomatic Vehicle Numbers

| Vehicle Number | Prefix | Suffix | Result |
| --- | --- | --- | --- |
| SCC1234A | S | CC | Diplomatic |
| SCD5678B | S | CD | Diplomatic |
| STC9012C | S | TC | Diplomatic |
| STE3456D | S | TE | Diplomatic |

---

## 4. Notice Creation API

### 4.1 Create Diplomatic Vehicle Notice

| Attribute | Value |
| --- | --- |
| Type | Internal Service |
| Service | CreateNoticeService |
| Method | `createNotice(noticeData)` |
| Purpose | Create notice at NPA stage with ROV as next stage |

#### Input - Notice Data

| Field | Type | Required | Description | Sample Value |
| --- | --- | --- | --- | --- |
| notice_no | String | Yes | Notice number | 500500303J |
| vehicle_no | String | Yes | Vehicle number | SCC1234A |
| vehicle_registration_type | String | Yes | Must be 'D' for Diplomatic | D |
| offence_notice_type | String | Yes | O, E, or U | O |
| computer_rule_code | String | Yes | Offence rule code | 10300 |
| pp_code | String | Yes | Car park code | A0005 |
| parking_lot_no | String | No | Parking lot number | 12 |
| notice_date_and_time | DateTime | Yes | Offence date/time | 2025-03-21 19:00:02 |

#### Output - Created Notice

| Field | Value | Description |
| --- | --- | --- |
| last_processing_stage | NPA | Initial stage |
| next_processing_stage | ROV | Next stage (LTA check) |
| next_processing_date | Current Date | Same day processing |
| payment_acceptance_allowed | Y | Notice is payable |

#### Database Insert - ocms_valid_offence_notice

| Field | Value | Description |
| --- | --- | --- |
| notice_no | 500500303J | Notice number |
| vehicle_registration_type | D | Diplomatic |
| last_processing_stage | NPA | Initial stage |
| next_processing_stage | ROV | LTA check stage |
| next_processing_date | Current Date | Processing date |
| an_flag | N or Y | Advisory Notice flag |
| suspension_type | NULL | No suspension yet |
| epr_reason_of_suspension | NULL | No suspension yet |

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
| Reference | OCMS 10 Functional Document |

#### Query Criteria for AN Letter Generation

```sql
SELECT * FROM ocms_valid_offence_notice
WHERE vehicle_registration_type = 'D'
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
| Qualifies for AN | Set an_flag = 'Y', process via AN sub-flow (Section 6.2.4), apply PS-ANS |
| Does Not Qualify | Continue to ROV stage for LTA check |

---

## 7. LTA Vehicle Ownership Check API

### 7.1 Query LTA VRLS

| Attribute | Value |
| --- | --- |
| Type | External API Call |
| System | LTA VRLS (Vehicle Registration Licensing System) |
| Purpose | Retrieve vehicle ownership and registration details |
| Stage | ROV (Registration of Vehicle) |

#### Request

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| vehicleNo | String | Yes | Vehicle registration number |

#### Response - Vehicle Details

| Field | Type | Description | Sample Value |
| --- | --- | --- | --- |
| vehicleNo | String | Vehicle number | SCC1234A |
| vehicleMake | String | Vehicle make | TOYOTA |
| vehicleModel | String | Vehicle model | CAMRY |
| vehicleColour | String | Vehicle colour | WHITE |
| vehicleCategory | String | Vehicle category | C (Car) |
| registrationDate | Date | Registration date | 2020-01-15 |

#### Response - Owner Details

| Field | Type | Description | Sample Value |
| --- | --- | --- | --- |
| ownerName | String | Owner name | EMBASSY OF COUNTRY X |
| ownerIdType | String | ID type | U (UEN) |
| ownerIdNo | String | ID number | T08GA0123X |
| ownerAddress | Object | Owner address | Address object |

#### Error Handling

| Error Code | Description | Action |
| --- | --- | --- |
| LTA-001 | Vehicle not found | Log error, continue with available data |
| LTA-002 | Service unavailable | Retry 3 times, then send alert email |
| LTA-003 | Timeout | Retry with backoff |

---

## 8. MHA Check API

### 8.1 Query MHA for Owner Details

| Attribute | Value |
| --- | --- |
| Type | External API Call |
| System | MHA (Ministry of Home Affairs) |
| Purpose | Verify local ID, get latest registered address |
| Stage | After LTA check, before RD1 |

#### Request

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| idType | String | Yes | ID type (NRIC/FIN/UEN) |
| idNo | String | Yes | ID number |

#### Response

| Field | Type | Description | Sample Value |
| --- | --- | --- | --- |
| name | String | Full name | EMBASSY OF COUNTRY X |
| idType | String | ID type | U |
| idNo | String | ID number | T08GA0123X |
| address | Object | Latest registered address | Address object |
| lifeStatus | String | Alive/Dead status | A |

#### Error Handling

| Error Code | Description | Action |
| --- | --- | --- |
| MHA-001 | ID not found | Continue with LTA data |
| MHA-002 | Service unavailable | Retry 3 times |
| MHA-003 | Timeout | Retry with backoff |

---

## 9. DataHive Check API

### 9.1 Query DataHive

| Attribute | Value |
| --- | --- |
| Type | External API Call |
| System | DataHive |
| Purpose | Get FIN details, company profile, additional address info |
| Stage | After MHA check, before RD1 |

#### Request

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| idType | String | Yes | ID type |
| idNo | String | Yes | ID number |
| queryType | String | Yes | NRIC/FIN/UEN |

#### Response - Individual (FIN)

| Field | Type | Description |
| --- | --- | --- |
| name | String | Full name |
| finNo | String | FIN number |
| passType | String | Pass type |
| passStatus | String | Pass status |
| address | Object | Registered address |

#### Response - Company (UEN)

| Field | Type | Description |
| --- | --- | --- |
| companyName | String | Company name |
| uen | String | UEN number |
| registeredAddress | Object | Registered address |
| businessStatus | String | Business status |

---

## 10. Stage Processing APIs

### 10.1 Process ROV Stage (CRON)

| Attribute | Value |
| --- | --- |
| Type | CRON Job |
| Schedule | End of day |
| Purpose | Process LTA/MHA/DataHive checks, prepare for RD1 |

#### Query Criteria

```sql
SELECT * FROM ocms_valid_offence_notice
WHERE vehicle_registration_type = 'D'
  AND last_processing_stage = 'NPA'
  AND next_processing_stage = 'ROV'
  AND next_processing_date <= CURRENT_DATE
  AND suspension_type IS NULL
```

#### Process Steps

1. Query LTA for vehicle ownership
2. Query MHA for owner details
3. Query DataHive for additional info
4. Store owner/address in database
5. Update last_processing_stage = ROV
6. Set next_processing_stage = RD1

### 10.2 Prepare for RD1 (CRON)

| Attribute | Value |
| --- | --- |
| Type | CRON Job |
| Schedule | End of day |
| Purpose | Generate RD1 Reminder Letter |

#### Query Criteria

```sql
SELECT * FROM ocms_valid_offence_notice
WHERE vehicle_registration_type = 'D'
  AND last_processing_stage = 'ROV'
  AND next_processing_stage = 'RD1'
  AND next_processing_date <= CURRENT_DATE
  AND suspension_type IS NULL
```

#### Process Steps

1. Query notices at ROV with next_processing_stage = RD1
2. Add notices to RD1 Letter generation list
3. Send list to TOPPAN (printing vendor) via SFTP
4. Update last_processing_stage = RD1
5. Set next_processing_stage = RD2

### 10.3 Prepare for RD2 (CRON)

| Attribute | Value |
| --- | --- |
| Type | CRON Job |
| Schedule | End of RD1 stage |
| Purpose | Generate RD2 Reminder Letter |

#### Query Criteria

```sql
SELECT * FROM ocms_valid_offence_notice
WHERE vehicle_registration_type = 'D'
  AND last_processing_stage = 'RD1'
  AND next_processing_stage = 'RD2'
  AND next_processing_date <= CURRENT_DATE
  AND suspension_type IS NULL
  AND crs_reason_of_suspension IS NULL
```

#### Process Steps

1. Re-check MHA/DataHive for updated address
2. Query outstanding notices at RD1
3. Add notices to RD2 Letter generation list
4. Send list to TOPPAN via SFTP
5. Update last_processing_stage = RD2
6. Set next_processing_stage = RR3

### 10.4 Prepare for DN1 (Type E / Furnished Driver)

| Attribute | Value |
| --- | --- |
| Type | CRON Job |
| Schedule | End of day |
| Purpose | Generate DN1 Letter for Driver/Hirer |

### 10.5 Prepare for DN2

| Attribute | Value |
| --- | --- |
| Type | CRON Job |
| Schedule | End of DN1 stage |
| Purpose | Generate DN2 Letter |

---

## 11. Furnish Driver/Hirer API

### 11.1 Submit Furnish Application

| Attribute | Value |
| --- | --- |
| Type | eService Portal API |
| Source | Internet (Public) |
| Purpose | Vehicle Owner submits Driver/Hirer particulars |
| Reference | Section 6.2.5 |

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
4. Backend validates Driver/Hirer particulars via MHA/DataHive
5. Auto-approve if criteria met, else OIC manual review
6. If approved: Create Driver/Hirer record, change next stage to DN1
7. If rejected: Remove "furnish pending approval" status

#### Furnish Allowed Stages

| Stage | Furnish Allowed |
| --- | --- |
| NPA | No |
| ROV | Yes |
| RD1 | Yes |
| RD2 | Yes |
| PS-DIP | **No** |

### 11.2 Sync Furnish Data

| Attribute | Value |
| --- | --- |
| Type | CRON Job |
| Purpose | Sync furnished Driver/Hirer from Internet to Intranet |

---

## 12. Suspension APIs

### 12.1 Apply PS-DIP Suspension

| Attribute | Value |
| --- | --- |
| Type | Internal Service |
| Service | PermanentSuspensionHelper |
| Purpose | Apply permanent suspension for Diplomatic Vehicles |

#### Trigger Conditions

- Notice is outstanding at end of RD2 or DN2
- Vehicle registration type = 'D'
- No existing CRS reason of suspension

#### Query for PS-DIP Application

```sql
SELECT * FROM ocms_valid_offence_notice
WHERE vehicle_registration_type = 'D'
  AND crs_reason_of_suspension IS NULL
  AND last_processing_stage IN ('RD2', 'DN2')
  AND next_processing_stage IN ('RR3', 'DR3')
```

#### Update Fields - ocms_valid_offence_notice

| Field | Value | Description |
| --- | --- | --- |
| suspension_type | PS | Permanent Suspension |
| epr_reason_of_suspension | DIP | Diplomatic Vehicle |
| epr_date_of_suspension | Current DateTime | Suspension timestamp |

#### Insert into ocms_suspended_notice

| Field | Value |
| --- | --- |
| notice_no | Notice number |
| suspension_type | PS |
| reason_of_suspension | DIP |
| date_of_suspension | Current DateTime |
| suspension_source | ocmsiz_app_conn |
| sr_no | Running number |
| officer_authorising_suspension | System |
| cre_user_id | ocmsiz_app_conn |
| cre_date | Current DateTime |

### 12.2 PS-DIP Suspension Rules

| Rule | Value | Description |
| --- | --- | --- |
| Applicable Stages | NPA, ROV, RD1, RD2, DN1, DN2 | Auto at end of RD2/DN2 |
| OCMS Staff can apply | Yes | Via Staff Portal |
| PLUS can apply | **No** | Not available in PLUS |
| Payable after suspension | Yes | Via eService & AXS |
| Furnishable after suspension | **No** | Cannot furnish after PS-DIP |
| Allow additional TS | Yes | TS can stack on PS-DIP |
| Allow additional PS | Yes | PS can stack on PS-DIP |

### 12.3 Daily Re-check CRON (DipMidForRecheckJob)

| Attribute | Value |
| --- | --- |
| Type | CRON Job |
| Schedule | Daily at 11:59 PM |
| Purpose | Re-apply PS-DIP if accidentally revived |

#### Query Criteria

```sql
SELECT * FROM ocms_valid_offence_notice
WHERE vehicle_registration_type IN ('D', 'I', 'F')
  AND last_processing_stage IN ('RD2', 'DN2')
  AND suspension_type IS NULL
```

#### Process

1. Query notices at RD2/DN2 without active PS
2. For type 'D': Re-apply PS-DIP
3. Prevent progression to RR3/DR3

---

## 13. Letter Generation API (SFTP)

### 13.1 Send Letters to TOPPAN

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

## 14. Database Operations Summary

### 14.1 Tables Modified

| Table | Operations | Purpose |
| --- | --- | --- |
| ocms_valid_offence_notice | INSERT, UPDATE | Notice records (Intranet) |
| eocms_valid_offence_notice | INSERT, UPDATE | Notice records (Internet) |
| ocms_offence_notice_owner_driver | INSERT, UPDATE | Owner/Driver details |
| ocms_offence_notice_owner_driver_addr | INSERT, UPDATE | Owner/Driver address |
| ocms_offence_notice_detail | INSERT | Notice additional details |
| ocms_suspended_notice | INSERT | Suspension records |
| ocms_furnish_application | INSERT, UPDATE | Furnish requests |

### 14.2 Sync Operations

| Direction | Tables | Trigger |
| --- | --- | --- |
| Intranet → Internet | ocms_valid_offence_notice | Notice creation, stage changes, suspension |
| Internet → Intranet | ocms_furnish_application | Furnish submission |

---

## 15. External System Integrations Summary

### 15.1 Systems Called for Diplomatic Vehicles

| System | Stage | Purpose | Required |
| --- | --- | --- | --- |
| LTA VRLS | ROV | Vehicle ownership | Yes |
| MHA | ROV, RD2 | Address verification | Yes |
| DataHive | ROV, RD2 | FIN/company profile | Yes |
| TOPPAN | RD1, RD2, DN1, DN2 | Letter printing | Yes |

### 15.2 Systems NOT Called for Diplomatic Vehicles

| System | Reason |
| --- | --- |
| eNotification (SMS/Email) | ENA stage bypassed |

---

## 16. CRON Jobs Summary

| Job Name | Schedule | Purpose |
| --- | --- | --- |
| ProcessROVJob | End of day | LTA/MHA/DataHive checks |
| PrepareForRD1Job | End of day | Process ROV → RD1 |
| PrepareForRD2Job | End of RD1 | Process RD1 → RD2 |
| PrepareForDN1Job | End of day | Process furnished notices → DN1 |
| PrepareForDN2Job | End of DN1 | Process DN1 → DN2 |
| DipMidForRecheckJob | 11:59 PM daily | Re-apply PS-DIP at RD2/DN2 |
| ANLetterGenerationJob | End of day | Generate AN Letters |
| SuspendDIPNoticesJob | End of RD2/DN2 | Apply PS-DIP suspension |

---

## 17. Flow Comparison: DIP vs Standard vs MID

| Step | Standard Flow | MID Flow | DIP Flow |
| --- | --- | --- | --- |
| LTA Check | Yes | **No** | Yes |
| MHA Check | Yes | **No** | Yes |
| DataHive Check | Yes | **No** | Yes |
| ENA Stage | Yes | **No** | **No** |
| eNotification | Yes | **No** | **No** |
| Owner Address | From LTA/MHA/DH | Hardcoded MINDEF | From LTA/MHA/DH |
| End Stage | Court | RD2/DN2 + PS-MID | RD2/DN2 + PS-DIP |

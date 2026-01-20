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
| v1.1 | Claude | 18/01/2026 | Updated queries and database operations based on data dictionary |
| v1.2 | Claude | 19/01/2026 | Yi Jie Review Fixes: (1) Replaced SELECT * with specific fields, (2) Added API response format standards, (3) Added eligibility scenarios by source, (4) Fixed flowchart references, (5) Added sync flag documentation, (6) Clarified batch job logging approach, (7) Added push mechanism details, (8) Documented sequential/parallel error flow |
| v1.3 | Claude | 19/01/2026 | Data Dictionary Alignment: (1) Fixed table name ocms_offence_notice_owner_driver_addr, (2) Fixed computer_rule_code type to integer, (3) Marked sync flag fields as [NEW FIELD] for database schema |

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

<!--
FLOWCHART REFERENCE:
File: OCMS14-Technical_Flowchart_Section_4.drawio
Tab: 4.2 High Level Flow
-->

> **Diagram:** Refer to `OCMS14-Technical_Flowchart_Section_4.drawio` → Tab "4.2 High Level Flow"

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

### API Response Format Standards

All internal service responses follow the standard OCMS API format:

#### Success Response
```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Notice created successfully"
  }
}
```

#### Error Response
```json
{
  "data": {
    "appCode": "OCMS-5001",
    "message": "Duplicate notice number detected"
  }
}
```

#### List Response (with Pagination)
```json
{
  "total": 150,
  "limit": 10,
  "skip": 0,
  "data": [
    {
      "noticeNo": "500500303J",
      "vehicleNo": "MID2221",
      "lastProcessingStage": "RD1"
    }
  ]
}
```

#### Common App Codes for Military Vehicle Processing

| App Code | Description |
| --- | --- |
| OCMS-2000 | Success |
| OCMS-5001 | Duplicate notice number |
| OCMS-5002 | Invalid vehicle number format |
| OCMS-5003 | Missing mandatory fields |
| OCMS-5004 | Double booking detected (PS-DBB applied) |
| OCMS-5005 | Notice already suspended |
| OCMS-5010 | SFTP connection failed |
| OCMS-5011 | Letter generation failed |

---

## 4.3 Type O & E Processing Flow

<!--
FLOWCHART REFERENCE:
File: OCMS14-Technical_Flowchart_Section_4.drawio
Tab: 4.3 Type O & E Processing Flow
-->

> **Diagram:** Refer to `OCMS14-Technical_Flowchart_Section_4.drawio` → Tab "4.3 Type O & E Processing"

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

#### Notice Creation Fields (ocms_valid_offence_notice)

| Field | Type | Value/Source | Description |
| --- | --- | --- | --- |
| notice_no | varchar(10) | Generated | Primary key, unique notice number |
| vehicle_no | varchar(14) | Raw offence data | Vehicle number (MID/MINDEF pattern) |
| vehicle_registration_type | varchar(1) | 'I' | Military vehicle type |
| offence_notice_type | varchar(1) | Raw offence data | O (On-street), E (ERP), U (UPL) |
| computer_rule_code | integer | Raw offence data | Offence rule code |
| pp_code | varchar(5) | Raw offence data | Car park code |
| parking_lot_no | varchar(5) | Raw offence data | Parking lot number (optional) |
| notice_date_and_time | datetime2(7) | Raw offence data | Date and time of offence |
| last_processing_stage | varchar(3) | 'NPA' | Initial stage |
| next_processing_stage | varchar(3) | 'RD1' | Next stage in flow |
| payment_acceptance_allowed | varchar(1) | 'Y' | Notice is payable |

#### Hardcoded MINDEF Owner Details (ocms_offence_notice_owner_driver)

| Field | Type | Value | Description |
| --- | --- | --- | --- |
| notice_no | varchar(10) | Notice number | Foreign key |
| owner_driver_indicator | varchar(1) | O | Owner record |
| name | varchar(66) | MINDEF | Ministry of Defence |
| id_type | varchar(1) | B | Business entity type |
| id_no | varchar(12) | T08GA0011B | MINDEF UEN |
| offender_indicator | varchar(1) | Y | Is offender |
| mha_dh_check_allow | varchar(1) | N | Bypass MHA/DataHive check |

#### Hardcoded MINDEF Address (ocms_offence_notice_owner_driver_addr)

| Field | Type | Value | Description |
| --- | --- | --- | --- |
| notice_no | varchar(10) | Notice number | Primary key |
| owner_driver_indicator | varchar(1) | O | Primary key |
| type_of_address | varchar(20) | MINDEF_ADDRESS | Primary key - address type |
| bldg_name | varchar(65) | Kranji Camp 3 | Building name |
| blk_hse_no | varchar(10) | 151 | Block/house number |
| street_name | varchar(32) | Choa Chu Kang Way | Street name |
| postal_code | varchar(6) | 688248 | Postal code |

### Double Booking Check (DBB) Criteria

All 5 conditions must match an existing notice to trigger PS-DBB:

| Criteria | Field | Match Type |
| --- | --- | --- |
| 1 | vehicle_no | Exact match |
| 2 | notice_date_and_time | Exact match |
| 3 | computer_rule_code | Exact match |
| 4 | parking_lot_no | Exact match (null = null) |
| 5 | pp_code | Exact match |

#### Double Booking Check Query

```sql
SELECT notice_no, vehicle_no, notice_date_and_time,
       computer_rule_code, parking_lot_no, pp_code,
       last_processing_stage, suspension_type
FROM ocms_valid_offence_notice
WHERE vehicle_no = :vehicleNo
  AND notice_date_and_time = :noticeDateTime
  AND computer_rule_code = :ruleCode
  AND (parking_lot_no = :lotNo OR (parking_lot_no IS NULL AND :lotNo IS NULL))
  AND pp_code = :ppCode
```

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
| Create Notice | INSERT | ocms_offence_notice_owner_driver_addr | Insert MINDEF address |
| Create Notice | INSERT | ocms_offence_notice_detail | Insert notice details |
| Update RD1 | UPDATE | ocms_valid_offence_notice | Set last_processing_stage = 'RD1' |
| Update RD2 | UPDATE | ocms_valid_offence_notice | Set last_processing_stage = 'RD2' |
| PS-MID | UPDATE | ocms_valid_offence_notice | Set suspension_type = 'PS', epr_reason = 'MID' |
| PS-MID | INSERT | ocms_suspended_notice | Create suspension record |

### External System Integration

#### TOPPAN SFTP (Letter Generation)

| Field | Value |
| --- | --- |
| Integration Name | TOPPAN Letter Generation |
| Type | SFTP File Transfer |
| Direction | OCMS → TOPPAN (Outbound) |
| Protocol | SFTP |
| Host | UAT: [configured in environment] <br> PRD: [configured in environment] |
| Port | 22 |
| Authentication | SSH Key-based |
| Trigger | End of day CRON job |
| File Format | PDF letter files |

##### Letter Types Generated

| Letter Code | Letter Name | Stage | Description |
| --- | --- | --- | --- |
| RD1 | Reminder Letter 1 | NPA → RD1 | First reminder to MINDEF |
| RD2 | Reminder Letter 2 | RD1 → RD2 | Second reminder to MINDEF |
| DN1 | Driver Notice 1 | RD1 → DN1 | First notice to furnished Driver |
| DN2 | Driver Notice 2 | DN1 → DN2 | Second notice to furnished Driver |
| AN | Advisory Notice | NPA → RD1 | Advisory Notice for eligible Type O |

##### SFTP File Transfer Flow

| Step | Action | Description |
| --- | --- | --- |
| 1 | Generate PDF | CRON job generates letter PDF with notice details |
| 2 | Create Batch | Group letters into batch file |
| 3 | Connect SFTP | Establish SFTP connection to TOPPAN server |
| 4 | Upload Files | Transfer PDF files to TOPPAN drop folder |
| 5 | Verify Transfer | Confirm successful file transfer |
| 6 | Update Status | Update letter_generated_flag in database |

##### Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| SFTP Connection Failure | Cannot establish connection to TOPPAN server | Log error, retry 3 times, queue for next CRON cycle |
| File Transfer Failure | File upload fails during transfer | Log error, rollback batch, retry in next cycle |
| Authentication Failure | SSH key authentication fails | Alert admin, log error, stop processing |

#### MHA/DataHive Check (Furnished Driver/Hirer Only)

| Field | Value |
| --- | --- |
| Integration Name | MHA/DataHive Identity Validation |
| Type | External API |
| Direction | OCMS → MHA/DataHive (Outbound) |
| Protocol | HTTPS REST API |
| Method | POST |
| Trigger | Before RD2/DN2 stage processing (for furnished Driver/Hirer only) |
| Purpose | Validate Driver/Hirer NRIC/FIN identity |

##### When Called

| Condition | System Called | Purpose |
| --- | --- | --- |
| Furnished Driver/Hirer with NRIC | MHA | Validate Singapore citizen/PR identity |
| Furnished Driver/Hirer with FIN | DataHive | Validate foreigner identity |
| MINDEF Owner (id_type = 'B') | NOT CALLED | UEN does not require identity check |

##### Response Handling

| Response | Action |
| --- | --- |
| Valid Identity | Continue to RD2/DN2 processing |
| Invalid Identity | Flag for OIC review, hold processing |
| System Error | Retry 3 times, then queue for manual review |

**Note:** MHA/DataHive check is only performed for furnished Driver/Hirer (mha_dh_check_allow = 'Y'). MINDEF owner record has mha_dh_check_allow = 'N' to bypass this check.

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
| Intranet | ocms_offence_notice_owner_driver_addr | postal_code | 688248 (hardcoded) |
| Intranet | ocms_suspended_notice | reason_of_suspension | MID / ANS / DBB / FP |
| Internet | eocms_valid_offence_notice | - | Synced copy for eService portal |

#### Sync Flag Documentation

**[NEW FIELD]** The sync mechanism fields below are new fields to be added to the database schema for this feature.

The sync mechanism uses flags in both Intranet and Internet databases to track synchronization status:

##### Intranet Sync Flags (ocms_valid_offence_notice) [NEW FIELD]

| Field | Type | Description | Values |
| --- | --- | --- | --- |
| sync_to_internet | varchar(1) | Mark notice for Internet sync | Y = Needs sync, N = Synced |
| sync_timestamp | datetime2(7) | Last successful sync time | Timestamp |

##### Internet Sync Flags (eocms_valid_offence_notice)

| Field | Type | Description | Values |
| --- | --- | --- | --- |
| is_sync | varchar(1) | Mark record for Intranet sync | Y = Synced, N = Needs sync |
| sync_timestamp | datetime2(7) | Last successful sync time | Timestamp |

##### Sync Flow Logic

| Direction | Trigger | Sync Flag Update |
| --- | --- | --- |
| Intranet → Internet | Notice created/updated | Set `sync_to_internet = 'Y'`, CRON picks up and syncs, then set `sync_to_internet = 'N'` |
| Internet → Intranet | Furnish submitted | Set `is_sync = 'N'`, CRON picks up and syncs, then set `is_sync = 'Y'` |

##### Sync Failure Handling

| Scenario | Action | Flag State |
| --- | --- | --- |
| Sync success | Clear flag | `sync_to_internet = 'N'` or `is_sync = 'Y'` |
| Sync failure | Keep flag, retry | Flag remains, picked up by next CRON |
| Multiple failures | Alert OIC | Flag remains, logged for manual review |

#### Push/Sync Mechanism Details

This section clarifies how data is pushed/synced between Intranet and Internet zones.

##### Intranet → Internet Sync (Notice Data)

| Attribute | Value |
| --- | --- |
| Mechanism | **CRON Job + Direct Database Write** |
| Job Name | NoticeSyncJob |
| Frequency | Every 5 minutes |
| Connection | Intranet DB reads from ocms_valid_offence_notice, writes to eocms_valid_offence_notice |
| Authentication | Database connection credentials (not API) |

**Process Flow:**
1. CRON job runs on Intranet server
2. Query records where `sync_to_internet = 'Y'`
3. Direct INSERT/UPDATE to Internet database (eocms_valid_offence_notice)
4. On success: Set `sync_to_internet = 'N'`
5. On failure: Log error, retry in next cycle

##### Internet → Intranet Sync (Furnish Data)

| Attribute | Value |
| --- | --- |
| Mechanism | **CRON Job + Direct Database Read** |
| Job Name | FurnishSyncJob |
| Frequency | Every 5 minutes |
| Connection | Intranet DB reads from eocms_furnish_application, writes to ocms_furnish_application |
| Authentication | Database connection credentials (not API) |

**Process Flow:**
1. CRON job runs on Intranet server
2. Query records where `is_sync = 'N'` from Internet database
3. Direct INSERT to Intranet database (ocms_furnish_application)
4. On success: Update Internet record `is_sync = 'Y'`
5. On failure: Log error, retry in next cycle

##### Why Direct Database (Not API)?

| Reason | Description |
| --- | --- |
| Performance | Direct DB is faster for bulk sync operations |
| Simplicity | No API layer overhead |
| Security | Database connection is within controlled network |
| Reliability | No HTTP timeout issues |

##### Retry Mechanism

| Scenario | Retry Count | Action After Max Retry |
| --- | --- | --- |
| Database connection failure | 3 times | Log error, send email alert, wait for next CRON cycle |
| Record insert/update failure | 3 times per record | Skip record, log error, continue with next record |
| Timeout | 1 time | Abort current batch, retry full batch in next cycle |

#### Input Parameters

| Parameter | Type | Description | Source |
| --- | --- | --- | --- |
| notice_no | String | Unique notice number | Notice creation |
| vehicle_no | String | Vehicle number (MID/MINDEF pattern) | Raw offence data |
| vehicle_registration_type | String | Must be 'I' for this flow | Detection result |
| offence_notice_type | String | O (On-street), E (ERP), U (UPL) | Raw offence data |
| computer_rule_code | integer | Offence rule code | Raw offence data |
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

#### Eligibility Scenarios by Source

The following matrix defines which actions are allowed from different system sources for Military Vehicle notices:

##### Action Permissions by Source

| Action | OCMS Staff Portal | PLUS Staff Portal | eService Portal | Backend CRON |
| --- | --- | --- | --- | --- |
| View Notice | ✅ Yes | ✅ Yes (Read-only) | ✅ Yes (Own notice) | N/A |
| Create Notice | ✅ Yes | ❌ No | ❌ No | ✅ Yes (Batch) |
| Update Notice | ✅ Yes | ❌ No | ❌ No | ✅ Yes (Stage) |
| Make Payment | ✅ Yes | ❌ No | ✅ Yes (AXS/eService) | ❌ No |
| Furnish Driver/Hirer | ✅ Yes | ❌ No | ✅ Yes | ❌ No |
| Apply PS-MID | ✅ Yes (Manual) | ❌ No | ❌ No | ✅ Yes (Auto) |
| Revive PS-MID | ✅ Yes (OIC only) | ❌ No | ❌ No | ❌ No |
| Apply TS on PS-MID | ✅ Yes | ❌ No | ❌ No | ❌ No |
| Generate Letters | ✅ Yes (Trigger) | ❌ No | ❌ No | ✅ Yes (Scheduled) |

##### Validation Rules by Source

| Source | Pre-validation | Post-validation |
| --- | --- | --- |
| **OCMS Staff Portal** | User role check, notice ownership | Audit trail logged with staff ID |
| **PLUS Staff Portal** | Read-only access, no write operations | N/A |
| **eService Portal** | ID verification, notice must belong to user | Payment confirmation, receipt generation |
| **Backend CRON** | Job schedule validation, lock check | Batch completion status, error logging |

##### Source-Specific Implementation Notes

| Source | Zone | Database User | Notes |
| --- | --- | --- | --- |
| OCMS Staff Portal | Intranet | ocmsiz_app_conn | Full access for authorized staff |
| PLUS Staff Portal | Intranet | ocmsiz_app_conn | View-only, query via API |
| eService Portal | Internet | ocmsez_app_conn | Public access, limited actions |
| Backend CRON | Intranet | ocmsiz_app_conn | System automation |

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

#### Sequential vs Parallel Processing

This section clarifies whether database operations are sequential or parallel, and what happens when one operation fails.

##### Notice Creation - Sequential Processing (Transaction)

All operations during notice creation are **SEQUENTIAL** within a single database transaction:

| Step | Operation | Table | If Fails |
| --- | --- | --- | --- |
| 1 | INSERT | ocms_valid_offence_notice (VON) | **Rollback all**, return error |
| 2 | INSERT | ocms_offence_notice_owner_driver (ONOD) | **Rollback all**, return error |
| 3 | INSERT | ocms_offence_notice_owner_driver_addr (ONOD_ADDR) | **Rollback all**, return error |
| 4 | INSERT | ocms_offence_notice_detail (OND) | **Rollback all**, return error |

**Important:**
- Parent table (VON) must be inserted first
- If ONOD insert fails, VON is rolled back
- If ONOD_ADDR insert fails, both VON and ONOD are rolled back
- All or nothing - either all inserts succeed or all fail

##### Stage Update - Sequential Processing

Stage updates are also **SEQUENTIAL**:

| Step | Operation | Table | If Fails |
| --- | --- | --- | --- |
| 1 | UPDATE | ocms_valid_offence_notice (VON) | Rollback, log error, retry |
| 2 | UPDATE | eocms_valid_offence_notice (eVON) | Log error, mark for sync retry |

**Note:** eVON update failure does NOT rollback VON update. Sync is eventually consistent.

##### PS-MID Application - Sequential Processing (Transaction)

| Step | Operation | Table | If Fails |
| --- | --- | --- | --- |
| 1 | UPDATE | ocms_valid_offence_notice (VON) | **Rollback all**, log error |
| 2 | INSERT | ocms_suspended_notice (SN) | **Rollback all**, log error |
| 3 | UPDATE | eocms_valid_offence_notice (eVON) | Log error, mark for sync |

**Note:** Steps 1 and 2 are in same transaction. Step 3 (Internet sync) is separate and eventually consistent.

##### Batch Processing - Per-Record Processing

CRON jobs process records **INDEPENDENTLY** (not in single transaction):

| Scenario | Behavior |
| --- | --- |
| Record 1 succeeds | Continue to Record 2 |
| Record 2 fails | Log error, **continue** to Record 3 |
| Record 3 succeeds | Continue to Record 4 |

**Important:** One record failure does NOT stop processing of other records.

##### Error Response Summary

| Operation Type | Processing | On Failure |
| --- | --- | --- |
| Notice Creation | Sequential (single transaction) | Rollback all, return error |
| Stage Update | Sequential | Rollback Intranet, sync later |
| PS-MID Apply | Sequential (VON+SN transaction) | Rollback VON+SN, sync later |
| CRON Batch | Per-record independent | Log error, continue next record |
| Sync Job | Per-record independent | Flag for retry, continue next |

##### Error Response Format

When an error occurs, return standard error response:

```json
{
  "data": {
    "appCode": "OCMS-5001",
    "message": "Notice creation failed: Duplicate notice number detected"
  }
}
```

| App Code | Error Type | Recovery Action |
| --- | --- | --- |
| OCMS-5001 | Duplicate notice | Check existing notice, no retry |
| OCMS-5002 | Invalid data | Fix input data, retry |
| OCMS-5003 | DB error | Auto-retry 3 times, then manual |
| OCMS-5004 | DBB detected | No retry (PS-DBB applied) |
| OCMS-5010 | SFTP error | Auto-retry in next CRON cycle |

---

## 4.4 Advisory Notice (AN) Sub-flow

<!--
FLOWCHART REFERENCE:
File: OCMS14-Technical_Flowchart_Section_4.drawio
Tab: 4.4 Advisory Notice Sub-flow
-->

> **Diagram:** Refer to `OCMS14-Technical_Flowchart_Section_4.drawio` → Tab "4.4 AN Sub-flow"

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

### Database Operations

| Step | Operation | Table | Action |
| --- | --- | --- | --- |
| Query | SELECT | ocms_valid_offence_notice | Get AN eligible notices |
| Update Stage | UPDATE | ocms_valid_offence_notice | Set last_processing_stage = 'RD1' |
| Apply PS-ANS | UPDATE | ocms_valid_offence_notice | Set suspension_type = 'PS', epr_reason = 'ANS' |
| Record Suspension | INSERT | ocms_suspended_notice | Create suspension record |

#### UPDATE ocms_valid_offence_notice (PS-ANS)

```sql
UPDATE ocms_valid_offence_notice
SET suspension_type = 'PS',
    epr_reason_of_suspension = 'ANS',
    epr_date_of_suspension = CURRENT_TIMESTAMP,
    last_processing_stage = 'RD1',
    upd_date = CURRENT_TIMESTAMP,
    upd_user_id = 'ocmsiz_app_conn'
WHERE notice_no = :noticeNo
```

#### INSERT ocms_suspended_notice (PS-ANS)

| Field | Type | Value | Description |
| --- | --- | --- | --- |
| notice_no | varchar(10) | Notice number | Primary key |
| date_of_suspension | datetime2(7) | CURRENT_TIMESTAMP | Primary key |
| sr_no | integer | Running number | Primary key |
| suspension_type | varchar(2) | PS | Permanent Suspension |
| reason_of_suspension | varchar(3) | ANS | Advisory Notice Suspension |
| suspension_source | varchar(8) | OCMS | System source |
| officer_authorising_suspension | varchar(50) | SYSTEM | Auto-authorized |
| due_date_of_revival | datetime2(7) | NULL | Permanent (no revival) |
| process_indicator | varchar(64) | ANLetterGenerationJob | CRON job name |

---

## 4.5 Furnish Driver/Hirer Sub-flow

<!--
FLOWCHART REFERENCE:
File: OCMS14-Technical_Flowchart_Section_4.drawio
Tab: 4.5 Furnish Driver/Hirer Sub-flow
-->

> **Diagram:** Refer to `OCMS14-Technical_Flowchart_Section_4.drawio` → Tab "4.5 Furnish Sub-flow"

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

### Furnish Application Fields (eocms_furnish_application)

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| txn_no | varchar(20) | Yes | Primary key - unique submission reference |
| notice_no | varchar(10) | Yes | Notice number |
| vehicle_no | varchar(14) | Yes | Vehicle number |
| offence_date | datetime2(7) | Yes | Date of offence |
| pp_code | varchar(5) | Yes | Car park code |
| pp_name | varchar(100) | Yes | Car park name |
| last_processing_stage | varchar(3) | Yes | Notice's current stage |
| furnish_name | varchar(66) | Yes | Driver/Hirer name |
| furnish_id_type | varchar(1) | Yes | ID type (NRIC/FIN) |
| furnish_id_no | varchar(12) | Yes | ID number |
| furnish_mail_blk_no | varchar(10) | Yes | Block/house number |
| furnish_mail_floor | varchar(3) | No | Floor number |
| furnish_mail_street_name | varchar(32) | Yes | Street name |
| furnish_mail_unit_no | varchar(5) | No | Unit number |
| furnish_mail_bldg_name | varchar(65) | No | Building name |
| furnish_mail_postal_code | varchar(6) | Yes | Postal code |
| furnish_tel_code | varchar(4) | No | Country code |
| furnish_tel_no | varchar(12) | No | Contact number |
| furnish_email_addr | varchar(320) | No | Email address |
| owner_driver_indicator | varchar(1) | Yes | D (Driver) or H (Hirer) |
| hirer_owner_relationship | varchar(1) | Yes | Relationship code |
| status | varchar(1) | Yes | P (Pending), A (Approved), R (Rejected) |

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
| Create Address | INSERT | ocms_offence_notice_owner_driver_addr | Intranet | Add address |
| Update Stage | UPDATE | ocms_valid_offence_notice | Intranet | Set next_processing_stage |

#### INSERT ocms_offence_notice_owner_driver (Furnished Driver/Hirer)

| Field | Type | Value | Description |
| --- | --- | --- | --- |
| notice_no | varchar(10) | Notice number | Primary key |
| owner_driver_indicator | varchar(1) | D or H | Primary key - Driver or Hirer |
| name | varchar(66) | furnish_name | Driver/Hirer name |
| id_type | varchar(1) | furnish_id_type | ID type (NRIC/FIN) |
| id_no | varchar(12) | furnish_id_no | ID number |
| offender_indicator | varchar(1) | Y | Is offender |
| mha_dh_check_allow | varchar(1) | Y | Enable MHA/DataHive check |

#### INSERT ocms_offence_notice_owner_driver_addr (Furnished Address)

| Field | Type | Value | Description |
| --- | --- | --- | --- |
| notice_no | varchar(10) | Notice number | Primary key |
| owner_driver_indicator | varchar(1) | D or H | Primary key |
| type_of_address | varchar(20) | furnished_mail | Primary key - address type |
| blk_hse_no | varchar(10) | furnish_mail_blk_no | Block/house number |
| street_name | varchar(32) | furnish_mail_street_name | Street name |
| postal_code | varchar(6) | furnish_mail_postal_code | Postal code |
| floor_no | varchar(3) | furnish_mail_floor | Floor number |
| unit_no | varchar(5) | furnish_mail_unit_no | Unit number |
| bldg_name | varchar(65) | furnish_mail_bldg_name | Building name |

#### UPDATE ocms_valid_offence_notice (After Furnish Approval)

```sql
-- For Driver
UPDATE ocms_valid_offence_notice
SET next_processing_stage = 'DN1',
    upd_date = CURRENT_TIMESTAMP,
    upd_user_id = 'ocmsiz_app_conn'
WHERE notice_no = :noticeNo

-- For Hirer
UPDATE ocms_valid_offence_notice
SET next_processing_stage = 'RD1',
    upd_date = CURRENT_TIMESTAMP,
    upd_user_id = 'ocmsiz_app_conn'
WHERE notice_no = :noticeNo
```

---

## 4.6 PS-MID Suspension Flow

<!--
FLOWCHART REFERENCE:
File: OCMS14-Technical_Flowchart_Section_4.drawio
Tab: 4.6 PS-MID Suspension Flow
-->

> **Diagram:** Refer to `OCMS14-Technical_Flowchart_Section_4.drawio` → Tab "4.6 PS-MID Suspension"

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
SELECT notice_no, vehicle_no, vehicle_registration_type,
       last_processing_stage, next_processing_stage,
       suspension_type, crs_reason_of_suspension
FROM ocms_valid_offence_notice
WHERE vehicle_registration_type = 'I'
  AND crs_reason_of_suspension IS NULL
  AND last_processing_stage IN ('RD2', 'DN2')
  AND next_processing_stage IN ('RR3', 'DR3')
```

### UPDATE ocms_valid_offence_notice (Intranet)

```sql
UPDATE ocms_valid_offence_notice
SET suspension_type = 'PS',
    epr_reason_of_suspension = 'MID',
    epr_date_of_suspension = CURRENT_TIMESTAMP,
    upd_date = CURRENT_TIMESTAMP,
    upd_user_id = 'ocmsiz_app_conn'
WHERE notice_no = :noticeNo
```

| Field | Type | Value | Description |
| --- | --- | --- | --- |
| suspension_type | varchar(2) | PS | Permanent Suspension |
| epr_reason_of_suspension | varchar(4) | MID | Military vehicle reason |
| epr_date_of_suspension | datetime2(7) | CURRENT_TIMESTAMP | Suspension timestamp |
| upd_date | datetime2(7) | CURRENT_TIMESTAMP | Record update timestamp |
| upd_user_id | varchar(50) | ocmsiz_app_conn | Intranet DB user |

### UPDATE eocms_valid_offence_notice (Internet Sync)

```sql
UPDATE eocms_valid_offence_notice
SET suspension_type = 'PS',
    epr_reason_of_suspension = 'MID',
    epr_date_of_suspension = CURRENT_TIMESTAMP,
    is_sync = 'N',
    upd_date = CURRENT_TIMESTAMP,
    upd_user_id = 'ocmsiz_app_conn'
WHERE notice_no = :noticeNo
```

| Field | Type | Value | Description |
| --- | --- | --- | --- |
| is_sync | varchar(1) | N | Mark for sync to Intranet |

### INSERT ocms_suspended_notice

| Field | Type | Value | Description |
| --- | --- | --- | --- |
| notice_no | varchar(10) | Notice number | Primary key (PK1) |
| date_of_suspension | datetime2(7) | CURRENT_TIMESTAMP | Primary key (PK2) |
| sr_no | numeric(3,0) | Running number | Primary key (PK3) |
| suspension_type | varchar(2) | PS | Permanent Suspension |
| reason_of_suspension | varchar(4) | MID | MINDEF reason code |
| suspension_source | varchar(4) | OCMS | Source system |
| officer_authorising_suspension | varchar(50) | SYSTEM | System or OIC name |
| due_date_of_revival | datetime2(7) | NULL | NULL for permanent suspension |
| suspension_remarks | varchar(256) | NULL | No remarks |
| process_indicator | varchar(50) | DipMidForRecheckJob | CRON job identifier |
| cre_date | datetime2(7) | CURRENT_TIMESTAMP | Record creation timestamp |
| cre_user_id | varchar(50) | ocmsiz_app_conn | Intranet DB user |

**Note:** The `ocms_suspended_notice` table uses a composite primary key consisting of `notice_no` + `date_of_suspension` + `sr_no`. This allows multiple suspension records for the same notice (e.g., temporary suspension while PS-MID is active).

### Daily Re-check (DipMidForRecheckJob)

| Attribute | Value |
| --- | --- |
| CRON Job | DipMidForRecheckJob |
| Schedule | Daily at 11:59 PM |
| Purpose | Re-apply PS-MID if accidentally revived |
| Scope | Vehicle types D (Diplomat), I (Military), F (Foreign) at RD2/DN2 without PS |

The daily re-check ensures that if a Military vehicle notice is accidentally revived (suspension removed), the PS-MID is re-applied before the notice can progress to RR3/DR3 (Court Referral stages).

### Re-check Query

```sql
SELECT notice_no, vehicle_no, vehicle_registration_type,
       last_processing_stage, next_processing_stage,
       suspension_type
FROM ocms_valid_offence_notice
WHERE vehicle_registration_type IN ('D', 'I', 'F')
  AND last_processing_stage IN ('RD2', 'DN2')
  AND suspension_type IS NULL
```

### PS-MID Suspension Rules

| Rule | Value | Description |
| --- | --- | --- |
| Payment Allowed | Yes | Can pay via eService/AXS |
| Furnish Allowed | No | Cannot furnish after PS-MID |
| Revival Date | NULL | Permanent suspension (no auto-revival) |
| Court Escalation | Blocked | Cannot progress to RR3/DR3 |

### Database Operations

| Step | Operation | Table | Zone | Action |
| --- | --- | --- | --- | --- |
| Query | SELECT | ocms_valid_offence_notice | Intranet | Get PS-MID candidates |
| Update | UPDATE | ocms_valid_offence_notice | Intranet | Set PS-MID suspension |
| Sync | UPDATE | eocms_valid_offence_notice | Internet | Sync suspension status (is_sync = 'N') |
| Record | INSERT | ocms_suspended_notice | Intranet | Create suspension record with process_indicator |

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

#### CRON Job Logging Approach

Per Yi Jie guidelines, logging approach differs by job frequency:

| Job Name | Frequency | Log To | Reason |
| --- | --- | --- | --- |
| PrepareForRD1Job | Daily | Batch Job Table | Daily job, track completion status |
| PrepareForRD2Job | Daily | Batch Job Table | Daily job, track completion status |
| PrepareForDN1Job | Daily | Batch Job Table | Daily job, track completion status |
| PrepareForDN2Job | Daily | Batch Job Table | Daily job, track completion status |
| DipMidForRecheckJob | Daily | Batch Job Table | Daily job, track completion status |
| ANLetterGenerationJob | Daily | Batch Job Table | Daily job, track completion status |
| SuspendMIDNoticesJob | Daily | Batch Job Table | Daily job, track completion status |
| **FurnishSyncJob** | **Frequent** | **Application Logs Only** | **High frequency sync, no batch table logging** |
| **NoticeSyncJob** | **Frequent** | **Application Logs Only** | **High frequency sync, no batch table logging** |

##### Batch Job Table Logging

For daily jobs, record to `ocms_batch_job_log`:

| Field | Description |
| --- | --- |
| job_name | Shedlock job name |
| start_time | Job start timestamp (record immediately when job starts) |
| end_time | Job completion timestamp |
| status | SUCCESS / FAILED / RUNNING |
| records_processed | Count of records processed |
| error_message | Error details if failed |

**Important:** Record `start_time` immediately when job starts to identify:
- Jobs that started but didn't end (timeout/stuck)
- Jobs that didn't start at all
- Long running jobs

##### Application Log Only (Frequent Sync Jobs)

For high-frequency sync jobs (FurnishSyncJob, NoticeSyncJob):
- Log to application logs only (not batch table)
- Log format: `[TIMESTAMP] [JOB_NAME] [LEVEL] [MESSAGE]`
- Log error messages for failed sync attempts
- No batch table entry created

**Archival:** Batch job table records are deleted after 3 months.

### E. Reference Documents

| Document | Section | Description |
| --- | --- | --- |
| OCMS 14 FD | Section 5 | Notice Processing Flow for Military Vehicles |
| OCMS 14 TD | Section 1 | Detecting Vehicle Registration Type |
| OCMS 10 FD | - | Advisory Notice criteria and processing |
| OCMS 11 TD | - | Notice Processing Flow for Local Vehicles |

---

**End of Section 4**

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
| v1.0 | Claude | 15/01/2026 | Document Initiation - Section 5 |
| v1.1 | Claude | 19/01/2026 | Fix SELECT *, add token refresh, Shedlock naming, batch job standards |
| v1.2 | Claude | 20/01/2026 | Fix table name: ocms_offence_notice_owner_driver_addr; Fix suspension_source to 4-char code (align with data dictionary) |

---

## Table of Content

| Section | Content | Pages |
| --- | --- | --- |
| 5 | Notice Processing Flow for Diplomatic Vehicles | 1 |
| 5.1 | Use Case | 1 |
| 5.2 | High Level Flow | 2 |
| 5.3 | Type O & E Processing Flow | 4 |
| 5.3.1 | Data Mapping | 9 |
| 5.3.2 | Success Outcome | 11 |
| 5.3.3 | Error Handling | 11 |
| 5.4 | Advisory Notice (AN) Sub-flow | 12 |
| 5.5 | Furnish Driver/Hirer Sub-flow | 14 |
| 5.6 | PS-DIP Suspension Flow | 17 |
| Appendix A | Flowchart Reference | 20 |
| Appendix B | Related Documents | 20 |
| Appendix C | Glossary | 21 |
| Appendix D | CRON Job Standards | 21 |

---

# Section 5 – Notice Processing Flow for Diplomatic Vehicles

## 5.1 Use Case

1. Diplomatic vehicles are identified by the registration number pattern with Prefix "S" and Suffix "CC", "CD", "TC", or "TE". These vehicles are registered under foreign embassies and high commissions.

2. The Diplomatic Vehicle Notice Processing Flow is triggered when:<br>a. A new notice is created with vehicle_registration_type = 'D' (Diplomatic)<br>b. The vehicle number matches diplomatic pattern (e.g., SCC1234A, SCD5678B)

3. Key characteristics of Diplomatic Vehicle processing:<br>a. Owner details are retrieved from LTA, MHA, and DataHive (NOT hardcoded)<br>b. Owner address obtained from external system checks<br>c. Notice bypasses ENA (Electronic Notification) stage - no SMS/email<br>d. Notice ends with PS-DIP (Permanent Suspension - Diplomatic) instead of Court Referral

4. Diplomatic Vehicle notices follow this stage progression:<br>a. Type O/E: NPA → ROV → RD1 → RD2 → PS-DIP<br>b. With Furnished Driver: NPA → ROV → RD1 → DN1 → DN2 → PS-DIP<br>c. With Furnished Hirer: NPA → ROV → RD1 → RD2 → PS-DIP

5. Refer to FD Section 6 for detailed process flow of the Diplomatic Vehicle Notice Processing.

### Key Differences from Standard Flow

| Aspect | Standard Flow | Diplomatic Vehicle Flow |
| --- | --- | --- |
| LTA VRLS Check | Included | **Included** |
| MHA Check | Included | **Included** |
| DataHive Check | Included | **Included** |
| ENA Stage | Included | **Bypassed** (No SMS/Email) |
| End Stage | Court Referral | **PS-DIP** |
| Payment after Suspension | Depends | **Allowed** |
| Furnish after Suspension | Depends | **NOT Allowed** |

### Key Differences: DIP vs MID

| Aspect | MID (Military) | DIP (Diplomatic) |
| --- | --- | --- |
| LTA Check | Bypass | **Included** |
| MHA Check | Bypass | **Included** |
| DataHive Check | Bypass | **Included** |
| Owner Address | Hardcoded MINDEF | **From LTA/MHA/DH** |
| Next Stage from NPA | RD1 | **ROV** |
| ENA Stage | Bypass | Bypass |
| Suspension Code | PS-MID | **PS-DIP** |

---

## 5.2 High Level Flow

<!-- Insert flow diagram here -->
![High Level Flow](./images/section5-high-level-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | Notice Creation detects vehicle_registration_type = 'D' | Flow entry point |
| Pre-conditions | Validate raw offence data, check duplicate notice, detect DIP vehicle | Data validation |
| Identify Offence Type | Determine if notice is Type O, E, or U | Route by offence type |
| Type O & E Workflow | Process parking/ERP offence for diplomatic vehicle | Main processing flow |
| Type U Workflow | Process UPL offence for diplomatic vehicle (KIV) | UPL flow - pending requirements |
| End of RD2/DN2 | Check if notice is outstanding at end of RD2 or DN2 stage | Stage completion check |
| Outstanding? | Decision point - is notice still unpaid? | Payment status check |
| Apply PS-DIP | If outstanding, apply Permanent Suspension - DIP | Suspend notice |
| Apply PS-FP | If paid, apply Permanent Suspension - Full Payment | Close notice |
| End | Notice processing completes, no further action | Flow exit point |

### Stage Progression Summary

| Notice Type | Stage Flow | Final State |
| --- | --- | --- |
| Type O (no furnish) | NPA → ROV → RD1 → RD2 → PS-DIP | Suspended |
| Type O (furnished driver) | NPA → ROV → RD1 → DN1 → DN2 → PS-DIP | Suspended |
| Type O (furnished hirer) | NPA → ROV → RD1 → RD2 → PS-DIP | Suspended |
| Type E (no furnish) | NPA → ROV → RD1 → RD2 → PS-DIP | Suspended |
| Type O (with AN) | NPA → RD1 (AN Letter) → PS-ANS | Suspended |
| All types (paid) | Any stage → PS-FP | Closed |

### Systems Included for Diplomatic Vehicles

| System | Purpose | Included |
| --- | --- | --- |
| LTA VRLS | Vehicle owner lookup | Yes |
| MHA | Address verification | Yes |
| DataHive | FIN/company profile | Yes |
| ENA | Electronic notification | **No (Bypassed)** |
| TOPPAN | Letter printing | Yes |

### Diplomatic Vehicle Format

| Component | Pattern | Examples |
| --- | --- | --- |
| Prefix | S | SCC, SCD, STC, STE |
| Suffix | CC, CD, TC, TE | - |
| Sample | SCC1234A | Diplomatic Corps |
| Sample | SCD5678B | Diplomatic Corps |
| Sample | STC9012C | Technical Staff |
| Sample | STE3456D | Technical Staff |

---

## 5.3 Type O & E Processing Flow

<!-- Insert flow diagram here -->
![Type O & E Processing Flow](./images/section5-type-o-e-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | Begin Type O/E processing for Diplomatic Vehicle | Flow entry point |
| Duplicate Notice Check | Query existing notices with same veh_no, date, rule, lot, pp_code | Detect duplicate |
| Duplicate Found? | Decision - does duplicate exist? | Branch based on result |
| Apply PS-DBB | If duplicate found, apply Double Booking suspension | End with PS-DBB |
| Create Notice at NPA | Insert notice record with next_stage = ROV | Create notice |
| Double Booking? | Check for double booking after creation | DBB check |
| Apply PS-DBB (2) | If double booking, apply PS-DBB suspension | End with PS-DBB |
| AN Qualify? (Type O only) | Check if Type O notice qualifies for Advisory Notice | AN eligibility check |
| AN Sub-flow | If AN qualified, process Advisory Notice (Section 5.4) | End with PS-ANS |
| ROV Stage Processing | Query LTA, MHA, DataHive for owner information | External API calls |
| Update Owner/Address | Store owner and address details in database | Database update |
| Prepare for RD1 | Generate RD1 Reminder Letter via TOPPAN SFTP | Letter generation |
| Notice at RD1 | Update last_processing_stage = 'RD1' | Stage update |
| Payment at RD1? | Check if payment received during RD1 stage | Payment check |
| Apply PS-FP (RD1) | If paid at RD1, apply Full Payment suspension | End with PS-FP |
| MHA/DataHive Re-check | Re-check address before RD2 (update if changed) | Address re-validation |
| Prepare for RD2 | Generate RD2 Reminder Letter via TOPPAN SFTP | Letter generation |
| Notice at RD2 | Update last_processing_stage = 'RD2' | Stage update |
| Payment at RD2? | Check if payment received during RD2 stage | Payment check |
| Apply PS-FP (RD2) | If paid at RD2, apply Full Payment suspension | End with PS-FP |
| Outstanding at RD2? | Check if notice still outstanding at end of RD2 | Final payment check |
| Apply PS-DIP | If outstanding, apply DIP permanent suspension | Suspend notice |
| End | Processing complete | Flow exit point |

### ROV Stage Processing Details

| Step | External System | Purpose | Input | Output |
| --- | --- | --- | --- | --- |
| 1 | LTA VRLS | Get vehicle ownership | vehicle_no | Owner name, ID, address |
| 2 | MHA | Verify owner identity | id_type, id_no | Latest address, life_status |
| 3 | DataHive | Get additional info | id_type, id_no | FIN details, company profile |

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
| D2 | Double Booking? | Same veh, same date/time | Apply PS-DBB, End | Continue |
| D3 | AN Qualify? | Type O + meets AN criteria (OCMS 10) | AN Sub-flow | ROV Stage |
| D4 | Payment at RD1? | payment_status = 'PAID' at RD1 | Apply PS-FP, End | Prepare RD2 |
| D5 | Payment at RD2? | payment_status = 'PAID' at RD2 | Apply PS-FP, End | Check Outstanding |
| D6 | Outstanding at RD2? | Outstanding at end of RD2 | Apply PS-DIP | Already Paid |

### Database Operations

| Step | Operation | Table | Action |
| --- | --- | --- | --- |
| DBB Check | SELECT | ocms_valid_offence_notice | Query by 5 DBB criteria |
| Create Notice | INSERT | ocms_valid_offence_notice | Insert notice at NPA stage, next_stage=ROV |
| Create Notice | INSERT | ocms_offence_notice_owner_driver | Insert owner details from LTA |
| Create Notice | INSERT | ocms_offence_notice_owner_driver_addr | Insert address from MHA/DH |
| Create Notice | INSERT | ocms_offence_notice_detail | Insert notice details |
| ROV Stage | UPDATE | ocms_valid_offence_notice | Set last_stage='ROV', next_stage='RD1' |
| Update RD1 | UPDATE | ocms_valid_offence_notice | Set last_processing_stage = 'RD1' |
| Update RD2 | UPDATE | ocms_valid_offence_notice | Set last_processing_stage = 'RD2' |
| PS-DIP | UPDATE | ocms_valid_offence_notice | Set suspension_type='PS', epr_reason='DIP' |
| PS-DIP | INSERT | ocms_suspended_notice | Create suspension record |
| Sync | UPDATE | eocms_valid_offence_notice | Sync to Internet database |

### External System Integration

#### LTA VRLS (Vehicle Registration)

| Field | Value |
| --- | --- |
| Integration Type | REST API |
| System | LTA VRLS |
| Purpose | Get vehicle ownership details |
| Stage | ROV |
| Token Handling | If token expired, auto refresh and continue processing |
| Retry | 3 times on failure |
| Alert | Send email alert after all retries fail |
| Fallback | Continue with available data |

#### MHA (Address Verification)

| Field | Value |
| --- | --- |
| Integration Type | REST API |
| System | MHA |
| Purpose | Verify owner identity, get latest address |
| Stage | ROV, before RD2 |
| Token Handling | If token expired, auto refresh and continue processing |
| Retry | 3 times on failure |
| Alert | Send email alert after all retries fail |
| Fallback | Continue with LTA data |

#### DataHive (Profile Lookup)

| Field | Value |
| --- | --- |
| Integration Type | REST API |
| System | DataHive |
| Purpose | Get FIN details, company profile |
| Stage | ROV, before RD2 |
| Token Handling | If token expired, auto refresh and continue processing |
| Retry | 3 times on failure |
| Alert | Send email alert after all retries fail |
| Fallback | Continue with available data |

#### TOPPAN SFTP (Letter Generation)

| Field | Value |
| --- | --- |
| Integration Type | SFTP File Transfer |
| Vendor | TOPPAN (Printing) |
| Purpose | Send letter generation requests |
| Trigger | End of day CRON job |
| Letter Types | RD1 Letter, RD2 Letter, DN1 Letter, DN2 Letter, AN Letter |

---

### 5.3.1 Data Mapping

#### Database Data Mapping

| Zone | Database Table | Field Name | Description |
| --- | --- | --- | --- |
| Intranet | ocms_valid_offence_notice | vehicle_registration_type | Value = 'D' for Diplomatic |
| Intranet | ocms_valid_offence_notice | vehicle_no | Vehicle number (S + CC/CD/TC/TE pattern) |
| Intranet | ocms_valid_offence_notice | last_processing_stage | Current stage (NPA/ROV/RD1/RD2) |
| Intranet | ocms_valid_offence_notice | next_processing_stage | Next stage in flow |
| Intranet | ocms_valid_offence_notice | suspension_type | PS when suspended |
| Intranet | ocms_valid_offence_notice | epr_reason_of_suspension | DIP for diplomatic suspension |
| Intranet | ocms_offence_notice_owner_driver | name | Owner name from LTA |
| Intranet | ocms_offence_notice_owner_driver | id_no | Owner ID from LTA |
| Intranet | ocms_offence_notice_owner_driver_addr | postal_code | Address from MHA/DataHive |
| Intranet | ocms_suspended_notice | reason_of_suspension | DIP / ANS / DBB / FP |
| Intranet | ocms_suspended_notice | suspension_source | OCMS (4-char code) |
| Internet | eocms_valid_offence_notice | - | Synced copy for eService portal |

#### Input Parameters

| Parameter | Type | Description | Source |
| --- | --- | --- | --- |
| notice_no | String | Unique notice number | Notice creation |
| vehicle_no | String | Vehicle number (DIP pattern) | Raw offence data |
| vehicle_registration_type | String | Must be 'D' for this flow | Detection result |
| offence_notice_type | String | O (On-street), E (ERP), U (UPL) | Raw offence data |
| computer_rule_code | String | Offence rule code | Raw offence data |
| pp_code | String | Car park code | Raw offence data |
| parking_lot_no | String | Parking lot number (optional) | Raw offence data |
| notice_date_and_time | DateTime | Date and time of offence | Raw offence data |

#### Stage Values

| Stage Code | Stage Name | Description |
| --- | --- | --- |
| NPA | Notice Pending Action | Initial stage after creation |
| ROV | Registration of Vehicle | LTA/MHA/DataHive check stage |
| RD1 | Reminder 1 | First reminder letter sent |
| RD2 | Reminder 2 | Second reminder letter sent |
| DN1 | Driver Notice 1 | First driver notice sent |
| DN2 | Driver Notice 2 | Second driver notice sent |

#### Suspension Types

| Type | Code | Description |
| --- | --- | --- |
| PS-DIP | suspension_type='PS', reason='DIP' | Permanent Suspension - Diplomatic |
| PS-ANS | suspension_type='PS', reason='ANS' | Permanent Suspension - Advisory Notice |
| PS-DBB | suspension_type='PS', reason='DBB' | Permanent Suspension - Double Booking |
| PS-FP | suspension_type='PS', reason='FP' | Permanent Suspension - Full Payment |

#### Audit User Fields

| Field | Intranet Value | Internet Value | Notes |
| --- | --- | --- | --- |
| cre_user_id | ocmsiz_app_conn | ocmsez_app_conn | NOT 'SYSTEM' |
| upd_user_id | ocmsiz_app_conn | ocmsez_app_conn | NOT 'SYSTEM' |

---

### 5.3.2 Success Outcome

- Diplomatic vehicle notice is successfully created with owner details from LTA/MHA/DataHive
- Notice progresses through stages: NPA → ROV → RD1 → RD2
- Owner information is updated with latest data from external systems
- Reminder letters (RD1, RD2) are generated and sent to TOPPAN for printing
- At end of RD2 (if outstanding), PS-DIP suspension is applied
- Notice processing stops after PS-DIP - no court referral
- If payment received at any stage, PS-FP is applied and notice closes
- Payment is allowed even after PS-DIP suspension

---

### 5.3.3 Error Handling

#### Application Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Duplicate Notice Number | Notice number already exists | Return error - reject duplicate |
| DB Insert Failure | Failed to insert notice record | Log error, rollback transaction |
| LTA API Failure | Cannot connect to LTA VRLS | Retry 3 times, then continue |
| MHA API Failure | Cannot connect to MHA | Retry 3 times, then use LTA data |
| DataHive API Failure | Cannot connect to DataHive | Retry 3 times, then continue |
| SFTP Connection Error | Cannot connect to TOPPAN SFTP | Log error, retry in next CRON cycle |

#### API Error Codes

| App Code | Description | Action |
| --- | --- | --- |
| OCMS-2000 | PS Success | Continue processing |
| OCMS-4001 | Invalid Notice Number | Reject request |
| OCMS-4003 | Paid notice | Only APP/CFA/VST allowed |
| OCMS-4008 | Cannot apply PS on existing PS | Use exception codes |
| OCMS-4000 | Source not authorized | Reject request |

#### Condition-Based Error Handling

| Condition ID | Error Condition | Action |
| --- | --- | --- |
| DIP-CRE-001 | Notice number already exists | Reject creation, return duplicate error |
| DIP-DBB-001 | All 5 DBB criteria match existing notice | Apply PS-DBB, stop processing |
| DIP-LTA-001 | LTA service unavailable | Retry 3 times, continue with partial data |
| DIP-MHA-001 | MHA service unavailable | Retry 3 times, use LTA data |
| DIP-DH-001 | DataHive service unavailable | Retry 3 times, continue |
| DIP-LTR-001 | SFTP transfer fails | Queue for retry in next CRON cycle |

---

## 5.4 Advisory Notice (AN) Sub-flow

<!-- Insert flow diagram here -->
![Advisory Notice Sub-flow](./images/section5-an-subflow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | AN qualified notice detected (Type O) | Flow entry point |
| Set an_flag = 'Y' | Mark notice as Advisory Notice eligible | Database update |
| Double Booking? | Check for double booking condition | DBB check |
| Apply PS-DBB | If DBB found, apply suspension | End with PS-DBB |
| Update next_stage | Set next_processing_stage = 'RD1' | Stage update |
| CRON: Generate AN Letter | Generate Advisory Notice letter PDF | PDF generation |
| Send to TOPPAN | Upload PDF to TOPPAN via SFTP | File transfer |
| Apply PS-ANS | Apply Permanent Suspension - Advisory Notice | Suspend notice |
| End | AN processing complete | Flow exit point |

### AN Eligibility Criteria

| Criteria | Description |
| --- | --- |
| Offence Type | Must be Type O (On-street parking) |
| Vehicle Type | vehicle_registration_type = 'D' |
| AN Flag | an_flag = 'Y' |
| Stage | last_processing_stage = 'NPA', next_stage = 'RD1' |
| Suspension | suspension_type IS NULL |
| Refer to | OCMS 10 Functional Document for detailed AN criteria |

### Database Operations

| Step | Operation | Table | Action |
| --- | --- | --- | --- |
| Set Flag | UPDATE | ocms_valid_offence_notice | Set an_flag = 'Y' |
| Update Stage | UPDATE | ocms_valid_offence_notice | Set next_processing_stage = 'RD1' |
| PS-ANS | UPDATE | ocms_valid_offence_notice | Set suspension_type='PS', reason='ANS' |
| PS-ANS | INSERT | ocms_suspended_notice | Create suspension record |

### Data Source Attribution

| Field | Source |
| --- | --- |
| an_flag | ocms_valid_offence_notice.an_flag |
| next_processing_stage | ocms_valid_offence_notice.next_processing_stage |

---

## 5.5 Furnish Driver/Hirer Sub-flow

<!-- Insert flow diagram here -->
![Furnish Driver/Hirer Sub-flow](./images/section5-furnish-subflow.png)

NOTE: Due to page size limit, the full-sized image is appended.

### Internet (eService Portal)

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | Vehicle owner submits furnish application | Flow entry point |
| Submit Furnish | Owner enters driver/hirer details | Form submission |
| Store in Internet DB | Save to pending furnish table | Database insert |

### Intranet (CRON Service)

| Step | Description | Brief Description |
| --- | --- | --- |
| Sync Furnish | CRON syncs pending furnish to Intranet | Data synchronization |
| Validate via MHA/DH | Validate driver identity via MHA/DataHive | External API call |
| Auto-approve? | Check if auto-approval criteria met | Decision point |
| Auto Approve | If criteria met, auto-approve furnish | Automatic approval |
| OIC Manual Review | If not auto-approved, queue for OIC review | Manual review |
| Approved? | OIC decision - approve or reject | Decision point |
| Create Driver Record | If approved, create driver/hirer record | Database insert |
| Remove Pending | If rejected, remove pending status | Database update |
| Change Stage to DN1 | Update next_processing_stage = 'DN1' | Stage change |
| End | Furnish processing complete | Flow exit point |

### Furnish Allowed Stages

| Stage | Furnish Allowed | Notes |
| --- | --- | --- |
| NPA | No | Too early in processing |
| ROV | Yes | After LTA check |
| RD1 | Yes | First reminder sent |
| RD2 | Yes | Second reminder sent |
| DN1 | Yes | Already furnished, can re-furnish |
| DN2 | Yes | Already furnished, can re-furnish |
| **PS-DIP** | **No** | Notice suspended, cannot furnish |

### Database Operations

| Step | Operation | Database | Table | Action |
| --- | --- | --- | --- | --- |
| Submit | INSERT | Internet | eocms_request_driver_particulars | Create pending request |
| Sync | SELECT | Internet | eocms_request_driver_particulars | Read pending requests |
| Sync | INSERT | Intranet | Furnish staging table | Copy to Intranet |
| Sync | UPDATE | Internet | eocms_request_driver_particulars | Set sync_flag = 'Y' |
| Create | INSERT | Intranet | ocms_offence_notice_owner_driver | Create driver record |
| Stage | UPDATE | Intranet | ocms_valid_offence_notice | Set next_stage = 'DN1' |

### Sync Flag Values

| Flag | Description | Location |
| --- | --- | --- |
| sync_flag = 'N' | Pending sync to Intranet | Internet DB |
| sync_flag = 'Y' | Synced to Intranet | Internet DB |

### Data Source Attribution

| Field | Source |
| --- | --- |
| Driver/Hirer record | ocms_offence_notice_owner_driver |
| Furnish request | eocms_request_driver_particulars |

---

## 5.6 PS-DIP Suspension Flow

<!-- Insert flow diagram here -->
![PS-DIP Suspension Flow](./images/section5-ps-dip-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| CRON Start | Suspension job triggered at end of RD2/DN2 | Job entry point |
| Query VON | Query eligible DIP notices for suspension | Database query |
| Records Found? | Decision - any notices found? | Check result count |
| No Records | If no qualifying notices, log and end | Job exit |
| FOR EACH Notice | Loop through each eligible notice | Loop start |
| Update VON | Update notice with PS-DIP suspension details | Database update |
| Insert OSN | Create suspension record in ocms_suspended_notice | Database insert |
| Sync to Internet | Update Internet database | Data sync |
| End | CRON job complete | Flow exit point |

### Query Criteria

```sql
SELECT notice_no, vehicle_no, vehicle_registration_type,
       last_processing_stage, next_processing_stage,
       suspension_type, epr_reason_of_suspension, crs_reason_of_suspension,
       payment_status, outstanding_amount
FROM ocms_valid_offence_notice
WHERE vehicle_registration_type = 'D'
  AND crs_reason_of_suspension IS NULL
  AND last_processing_stage IN ('RD2', 'DN2')
  AND next_processing_stage IN ('RR3', 'DR3')
```

**Note:** Do not use `SELECT *`. Always specify only the fields required for the operation.

### Database Operations

| Step | Operation | Table | Fields |
| --- | --- | --- | --- |
| Query | SELECT | ocms_valid_offence_notice | WHERE criteria above |
| Update VON | UPDATE | ocms_valid_offence_notice | suspension_type='PS', epr_reason='DIP', epr_date=NOW() |
| Insert OSN | INSERT | ocms_suspended_notice | notice_no, suspension_type, reason, date, source |
| Sync | UPDATE | eocms_valid_offence_notice | suspension_type, epr_reason, epr_date |

### PS-DIP Suspension Record

| Field | Value | Source |
| --- | --- | --- |
| notice_no | From query | ocms_valid_offence_notice.notice_no |
| suspension_type | 'PS' | Constant |
| reason_of_suspension | 'DIP' | Constant (Diplomatic) |
| date_of_suspension | NOW() | System timestamp |
| suspension_source | 'OCMS' | Constant (4-char source code) |
| cre_user_id | 'ocmsiz_app_conn' | Constant (NOT 'SYSTEM') |
| cre_date | NOW() | System timestamp |
| sr_no | Running number | SQL Server Sequence |

### PS-DIP Suspension Rules

| Rule | Description |
| --- | --- |
| Applicable Stages | NPA, ROV, RD1, RD2, DN1, DN2 |
| Auto-applied | At end of RD2/DN2 (CRON) |
| Manual Apply | OCMS Staff Portal only |
| PLUS Apply | **NOT Allowed** |
| Payment after PS-DIP | **Allowed** |
| Furnish after PS-DIP | **NOT Allowed** |
| TS Stacking | **Allowed** (PS Exception Code) |
| PS Stacking | **Allowed** (PS Exception Code) |

### PS Exception Codes

PS-DIP is one of the PS Exception Codes that allow special stacking behavior:

| Exception Code | Description |
| --- | --- |
| DIP | Diplomatic Vehicle |
| FOR | Foreign Vehicle |
| MID | Military Vehicle |
| RIP | Rest in Peace (Deceased) |
| RP2 | Rest in Peace 2 |

These codes allow:
- TS stacking on top of PS
- PS stacking without revival
- PS-FP/PRA without revival

### Source Permission Matrix

| Action | PLUS | OCMS Staff | CRON/Backend |
| --- | --- | --- | --- |
| Apply PS-DIP | No | Yes | Yes |
| Payment after PS-DIP | Yes | Yes | Yes |
| Furnish after PS-DIP | No | No | No |
| Add TS on PS-DIP | Yes | Yes | Yes |
| Add PS on PS-DIP | No | Yes | Yes |

### API Endpoint

**POST /ocms/v1/suspension/apply**

Request:
```json
{
  "noticeNo": "A1234567890",
  "suspensionType": "PS",
  "reasonOfSuspension": "DIP",
  "suspensionSource": "OCMS",
  "suspensionRemarks": "Diplomatic Vehicle",
  "officerAuthorisingSuspension": "SYSTEM"
}
```

Response (Success):
```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "PS Success"
  }
}
```

### Error Handling

| App Code | Description | Action |
| --- | --- | --- |
| OCMS-4001 | Invalid Notice Number | Log error, skip notice |
| OCMS-4003 | Paid notice | Only APP/CFA/VST allowed |
| OCMS-4008 | Cannot apply PS on existing PS | Check exception codes |
| OCMS-4000 | Source not authorized | Reject request |

---

## Appendix A: Flowchart Reference

The technical flowchart for this section is available in:
- File: `OCMS14-Technical_Flowchart_Section_5.drawio`
- Tabs:
  1. Section_5_High_Level - High-level overview
  2. Section_5_DIP_Type_OE - Type O & E processing flow (3 swimlanes: Backend, External System, Database)
  3. Section_5_DIP_AN_Subflow - Advisory Notice sub-flow (3 swimlanes: Backend, External System, Database)
  4. Section_5_DIP_Furnish_Subflow - Furnish Driver/Hirer sub-flow (4 swimlanes: Frontend, Backend, External System, Database)
  5. Section_5_PS_DIP_Suspension - PS-DIP suspension CRON process (3 swimlanes: Backend, Database Intranet, Database Internet)

---

## Appendix B: Related Documents

| Document | Reference |
| --- | --- |
| Functional Document | OCMS 14 - Section 6 |
| Advisory Notice Criteria | OCMS 10 - AN Processing |
| Data Dictionary | docs/data-dictionary/ |
| API Specification | plan_api.md |
| Validation Rules | plan_condition.md |

---

## Appendix C: Glossary

| Term | Description |
| --- | --- |
| DIP | Diplomatic (vehicle type code 'D') |
| PS-DIP | Permanent Suspension - Diplomatic |
| VON | ocms_valid_offence_notice (table) |
| OSN | ocms_suspended_notice (table) |
| ROV | Registration of Vehicle (stage) |
| ENA | Electronic Notification Action (bypassed) |
| AN | Advisory Notice |
| DBB | Double Booking |
| VRLS | Vehicle Registration Licensing System (LTA) |
| MHA | Ministry of Home Affairs |
| TOPPAN | Printing vendor |

---

## Appendix D: CRON Job Standards

### Shedlock Naming Convention

| Job Name | Shedlock Name | Schedule | Purpose |
| --- | --- | --- | --- |
| ProcessROVJob | `process_rov_dip` | End of day | LTA/MHA/DataHive checks |
| PrepareForRD1Job | `prepare_rd1_dip` | End of day | Process ROV → RD1 |
| PrepareForRD2Job | `prepare_rd2_dip` | End of RD1 | Process RD1 → RD2 |
| PrepareForDN1Job | `prepare_dn1_dip` | End of day | Process furnished notices → DN1 |
| PrepareForDN2Job | `prepare_dn2_dip` | End of DN1 | Process DN1 → DN2 |
| DipMidForRecheckJob | `recheck_dip_mid_for` | 11:59 PM daily | Re-apply PS-DIP at RD2/DN2 |
| ANLetterGenerationJob | `generate_an_letter` | End of day | Generate AN Letters |
| SuspendDIPNoticesJob | `suspend_dip_notices` | End of RD2/DN2 | Apply PS-DIP suspension |

### Naming Pattern

| Type | Pattern | Example |
| --- | --- | --- |
| File/Report | `[action]_[subject]_[suffix]` | `generate_report_daily` |
| API/Other | `[action]_[subject]` | `sync_payment` |
| Special | `[action]_[subject][term]` | `process_photosUpload` |

### Batch Job Standards

| Standard | Value | Description |
| --- | --- | --- |
| Job Tracking | Record start time immediately | When job starts, log start time first |
| Memory Safety | Break up process | Don't wait until job ends to log |
| Archival Period | **3 months** | Batch job records deleted after 3 months |
| Failure Detection | Identify stuck jobs | Can identify jobs that started but didn't end |

### External API Integration Standards

| Standard | Value |
| --- | --- |
| Token Handling | If token expired, auto refresh and continue processing |
| Retry Mechanism | Retry 3 times on connection failure |
| Alert | Send email alert after all retries fail |
| Fallback | Continue processing with available data |

---

*End of Document*

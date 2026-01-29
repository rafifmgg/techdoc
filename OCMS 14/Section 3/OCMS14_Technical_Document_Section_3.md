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
| v1.0 | Claude | 16/01/2026 | Document Initiation - Section 3 (Deceased Offenders) |
| v1.1 | Claude | 18/01/2026 | Added API Specification tables per template format (Section 3.3.2, 3.5.1) |
| v1.2 | Claude | 19/01/2026 | Yi Jie compliance fixes: Added batch job tracking section, Shedlock naming, aligned API response format with guidelines |
| v1.3 | Claude | 19/01/2026 | Data Dictionary compliance: Fixed ins_user_id→cre_user_id, offence_date→notice_date_and_time, removed non-existent current_offender_id, SQL Server syntax (GETDATE) |
| v1.4 | Claude | 20/01/2026 | Code alignment: Updated Shedlock naming to snake_case per Yi Jie #14, corrected schedule to 02:00 AM per actual code |
| v1.5 | Claude | 20/01/2026 | Code alignment: Updated report columns (7→13) per RipHirerDriverReportHelper.java, added missing error codes (OCMS-4002, OCMS-4005, OCMS-4008), replaced OCMS-5000 with OCMS-4007 per actual code |
| v1.6 | Claude | 27/01/2026 | Data Dictionary alignment: Added epr_date_of_suspension, suspension_source, process_indicator fields; Fixed query revival check (due_date_of_revival → date_of_revival); Added field types to data mapping |

---

## Table of Content

| Section | Content | Pages |
| --- | --- | --- |
| 3 | Notice Processing Flow for Deceased Offenders | 1 |
| 3.1 | Use Case | 1 |
| 3.2 | High Level Processing Flow | 2 |
| 3.3 | Detect Deceased and Apply PS Suspension | 3 |
| 3.3.1 | External System Integration | 5 |
| 3.3.2 | API Specification | 6 |
| 3.3.3 | Data Mapping | 7 |
| 3.3.4 | Success Outcome | 8 |
| 3.3.5 | Error Handling | 8 |
| 3.4 | Generate RIP Hirer/Driver Furnished Report | 9 |
| 3.4.1 | Data Mapping | 10 |
| 3.4.2 | Success Outcome | 11 |
| 3.4.3 | Error Handling | 11 |
| 3.5 | Redirect PS-RIP/RP2 Notices | 12 |
| 3.5.1 | API Specification | 13 |
| 3.5.2 | Data Mapping | 14 |
| 3.5.3 | Success Outcome | 15 |
| 3.5.4 | Error Handling | 15 |

---

# Section 3 – Notice Processing Flow for Deceased Offenders

## 3.1 Use Case

1. OCMS receives the life status of an Offender when it performs the Owner, Hirer or Driver particulars checks with MHA for NRIC holders or DataHive for FIN holders. Refer to OCMS 8 – Retrieve Offender Particulars Functional Specifications Document.

2. OCMS detects that the Offender is deceased when:<br>a. MHA returns the Life Status of the Offender (NRIC holder) as "D – Dead", OR<br>b. DataHive responds that the FIN holder is listed in the Dead Foreign Pass Holders dataset.

3. Upon detection, the Notice will be processed as a Special Notice where OCMS will permanently suspend the Notice with PS-RIP or PS-RP2, so that the Notice no longer continues along the standard processing flow.

4. OICs will be notified of the Notice's deceased offender status via the following ways:<br>a. The "RIP Hirer/Driver Furnished" report which lists notices where PS-RP2 has been applied and the deceased Offender is either the Hirer or Driver<br>b. A superscript "R" beside a Notice that has an active PS-RIP or PS-RP2 suspension, displayed beside the Notice no. on all view screens including search results<br>c. The "Date of Death" field in the Owner/Hirer/Driver particulars section will be populated if the date is provided by MHA or DataHive

5. Refer to FD Section 4 for detailed business rules and suspension code behaviors.

---

## 3.2 High Level Processing Flow

<!-- Insert flow diagram here -->
![High Level Flow](./images/section3-high-level-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Identify Deceased Offender | Offender is identified as deceased either through MHA returning NRIC Life Status as "D", or FIN holder found in DataHive's "Dead Foreign Pass Holders" dataset (D90) | External system identifies deceased |
| Update Offender Particulars | OCMS updates the offender's particulars in the database to store the deceased status (life_status, date_of_death) | Store deceased status in DB |
| Check Date of Death vs Offence Date | System compares date_of_death with offence_date to determine which suspension code to apply | Determine suspension code |
| Apply PS-RIP or PS-RP2 | Notice is permanently suspended using PS-RIP (if DoD >= offence_date) or PS-RP2 (if DoD < offence_date) | Apply permanent suspension |
| Generate RP2 Report | A daily report titled "RIP Hirer Driver Furnished" is generated for eligible RP2 notices where offender is Hirer or Driver | Generate daily report |
| OIC Manual Follow-up | OIC reviews PS-RIP and PS-RP2 Notices and may revive PS and redirect Notice to Owner, Hirer, or Driver | Manual OIC action |
| End | Process flow ends; Notice may continue standard processing if redirected after PS revival | Process complete |

---

## 3.3 Detect Deceased and Apply PS Suspension

<!-- Insert flow diagram here -->
![Detect and Suspend Flow](./images/section3-detect-suspend-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | Process begins when MHA/DataHive returns offender particulars | Flow entry point |
| Receive life_status | Receive life_status field from MHA/DataHive response | Receive external data |
| Check life_status = 'D'? | Decision: Check if life_status equals 'D' (deceased) | Deceased check |
| Continue Normal Processing | If life_status = 'A' (alive), continue standard notice processing | Normal flow |
| Update Offender Particulars | If deceased, update life_status='D' and date_of_death in database | Update offender data |
| Check date_of_death >= offence_date? | Compare date of death with offence date | Date comparison |
| Apply PS-RIP | If DoD >= offence_date, apply PS-RIP (Motorist Deceased On/After Offence Date) | Apply RIP suspension |
| Apply PS-RP2 | If DoD < offence_date, apply PS-RP2 (Motorist Deceased Before Offence Date) | Apply RP2 suspension |
| Call PermanentSuspensionHelper.processPS() | Call internal service to apply permanent suspension | Execute PS service |
| Check appCode = 0? | Check if suspension was applied successfully | Validate result |
| End (PS Applied) | Suspension successfully applied to notice | Success exit |
| Log Error and Continue | If error occurred, log error details | Error handling |
| End (Error Logged) | Process ends with error logged for investigation | Error exit |

### Suspension Code Determination

| Condition | Suspension Code | Description |
| --- | --- | --- |
| date_of_death >= offence_date | PS-RIP | Motorist Deceased On or After Offence Date |
| date_of_death < offence_date | PS-RP2 | Motorist Deceased Before Offence Date |

### Batch Job Configuration

**Shedlock Job Configuration:**

| Attribute | Value |
| --- | --- |
| Job Name | apply_deceased_suspension |
| Naming Convention | `[action]_[subject]` (API/other operations) |
| Schedule | Real-time (triggered by MHA/DataHive response) |
| Lock Duration | N/A (event-driven, not scheduled) |

**Job Tracking:**

| Tracking Aspect | Implementation |
| --- | --- |
| Logging Type | Application logs only (frequent sync job) |
| Start Time Recording | Record start time immediately when processing begins |
| Error Logging | Log to application logs with notice_no and error details |
| Stuck Job Detection | Track notices with life_status='D' but no PS applied |
| Metrics | Count of RIP/RP2 suspensions applied per day |

**Note:** This is a frequent sync operation triggered by MHA/DataHive responses. Per Yi Jie guideline, frequent sync jobs should NOT log to batch job table - use application logs only for error messages.

---

### 3.3.1 External System Integration

#### MHA Life Status Check (NRIC Holders)

| Field | Value |
| --- | --- |
| Integration Type | External API - SFTP File Exchange |
| System | Ministry of Home Affairs (MHA) |
| Purpose | Retrieve life status and date of death for NRIC holders |
| Reference | OCMS 8 - Section 3.2.2 |

**MHA Response Fields:**

| Field | Type | Description |
| --- | --- | --- |
| life_status | String(1) | 'A' = Alive, 'D' = Dead |
| date_of_death | Date | Date of death (if deceased) |
| name | String | Full name of the person |
| address | String | Registered address |

#### DataHive FIN Death Check (FIN Holders)

| Field | Value |
| --- | --- |
| Integration Type | External Database Query |
| System | DataHive |
| Dataset | Dead Foreign Pass Holders (D90) |
| View | V_DH_MHA_FINDEATH |
| Purpose | Check if FIN holder is in the deceased dataset |
| Reference | OCMS 8 - Section 7.5 |

**DataHive Query:**

```sql
SELECT FIN, DATE_OF_DEATH, REFERENCE_PERIOD
FROM V_DH_MHA_FINDEATH
WHERE FIN = '<fin_number>'
```

**Life Status Derivation:**

| Condition | life_status | Description |
| --- | --- | --- |
| FIN found in D90 dataset | D | FIN holder is deceased |
| FIN not found in D90 dataset | A | FIN holder is alive (default) |

---

### 3.3.2 API Specification

#### API Apply PS-RIP/RP2 Suspension

| Field | Value |
| --- | --- |
| API Name | processPS |
| URL | Internal Service Call (PermanentSuspensionHelper.processPS()) |
| Description | Apply permanent suspension (PS-RIP or PS-RP2) to notice when deceased offender detected |
| Method | POST |
| Trigger | Auto-triggered by CRON when deceased offender detected |
| Header | N/A (Internal service call) |
| Payload | `{ "noticeNo": "A12345678X", "suspensionSource": "CRON", "reasonOfSuspension": "RIP", "officerAuthorisingSuspension": "SYSTEM" }` |
| Response | `{ "data": { "appCode": "OCMS-2000", "message": "PS suspension applied successfully" }, "noticeNo": "A12345678X" }` |
| Response Failure | `{ "data": { "appCode": "OCMS-4001", "message": "Invalid Notice Number" } }` |

#### Request Parameters

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| noticeNo | String | Yes | Notice number to suspend |
| suspensionSource | String | Yes | Source: 'CRON' for auto-suspension |
| reasonOfSuspension | String | Yes | 'RIP' or 'RP2' |
| suspensionRemarks | String | No | Additional remarks |
| officerAuthorisingSuspension | String | Yes | 'SYSTEM' for auto-suspension |

#### Sample Request

```json
{
  "noticeNo": "A12345678X",
  "suspensionSource": "CRON",
  "reasonOfSuspension": "RIP",
  "officerAuthorisingSuspension": "SYSTEM"
}
```

#### Response (Success)

```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "PS suspension applied successfully"
  },
  "noticeNo": "A12345678X"
}
```

#### Response (Error)

```json
{
  "data": {
    "appCode": "OCMS-4001",
    "message": "Invalid Notice Number"
  }
}
```

---

### 3.3.3 Data Mapping

#### Database Tables Affected

**Table: ocms_offence_notice_owner_driver**

| Zone | Field Name | Type | Description |
| --- | --- | --- | --- |
| Intranet | life_status | varchar(1) | Life status indicator ('A' or 'D') |
| Intranet | date_of_death | datetime2(7) | Date of death from MHA/DataHive |
| Intranet | upd_date | datetime2(7) | Record update timestamp |
| Intranet | upd_user_id | varchar(50) | Audit user: 'ocmsiz_app_conn' |

**Table: ocms_valid_offence_notice**

| Zone | Field Name | Type | Description |
| --- | --- | --- | --- |
| Intranet | suspension_type | varchar(2) | 'PS' for permanent suspension |
| Intranet | epr_reason_of_suspension | varchar(3) | 'RIP' or 'RP2' |
| Intranet | epr_date_of_suspension | datetime2(7) | Timestamp when EPR suspension applied |
| Intranet | upd_date | datetime2(7) | Record update timestamp |
| Intranet | upd_user_id | varchar(50) | Audit user: 'ocmsiz_app_conn' |

**Table: ocms_suspended_notice**

| Zone | Field Name | Type | Description |
| --- | --- | --- | --- |
| Intranet | notice_no | varchar(10) | Notice number (PK) |
| Intranet | date_of_suspension | datetime2(7) | Timestamp when suspension applied (PK) |
| Intranet | sr_no | integer | Suspension record serial number (PK) |
| Intranet | suspension_source | varchar(4) | Source: 'CRON' for auto-suspension |
| Intranet | suspension_type | varchar(2) | 'PS' |
| Intranet | reason_of_suspension | varchar(3) | 'RIP' or 'RP2' |
| Intranet | officer_authorising_suspension | varchar(50) | 'SYSTEM' for auto-suspension |
| Intranet | due_date_of_revival | datetime2(7) | NULL (PS never auto-revives) |
| Intranet | process_indicator | varchar(64) | Process source: 'fetch_datahive_uen_fin' or 'manual' |
| Intranet | cre_user_id | varchar(50) | Audit user: 'ocmsiz_app_conn' |

**Audit User Note:** All database operations use `ocmsiz_app_conn` as the audit user for intranet database writes.

**Insert Order:**
1. UPDATE ocms_offence_notice_owner_driver (life_status, date_of_death)
2. UPDATE ocms_valid_offence_notice (suspension_type, epr_reason_of_suspension, epr_date_of_suspension)
3. INSERT ocms_suspended_notice (new suspension record with suspension_source, process_indicator)

---

### 3.3.4 Success Outcome

- Offender particulars updated with life_status = 'D' and date_of_death populated
- Notice suspended with appropriate PS code (PS-RIP or PS-RP2) based on date comparison
- New record inserted into ocms_suspended_notice table
- Notice flagged with superscript "R" for display in Staff Portal
- The workflow reaches the End state without triggering any error-handling paths

---

### 3.3.5 Error Handling

#### Application Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| MHA Connection Error | Cannot connect to MHA system | Log error, retry with backoff |
| DataHive Query Error | D90 dataset query fails | Log error, default to life_status='A' |
| Database Error | Unable to insert/update record | Rollback transaction, log error |
| Null Date of Death | life_status='D' but date_of_death is null | Log warning, use current date |

#### API Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| Notice Already PS | OCMS-2001 | Notice already has this PS code | Idempotent - return success |
| Invalid Notice Number | OCMS-4001 | Invalid Notice Number | Notice not found in database |
| Source Not Allowed | OCMS-4000 | Suspension Source is missing | PLUS cannot apply RIP/RP2 |
| Stage Not Allowed | OCMS-4002 | Stage not allowed for this suspension type | Notice stage does not permit PS |
| Paid Notice | OCMS-4003 | Paid/partially paid notices only allow APP, CFA, or VST | Cannot apply RIP/RP2 to paid notice |
| No Active PS Found | OCMS-4005 | No active PS-RIP/RP2 found for this notice | Revival requires active PS |
| Cannot Apply FP/PRA | OCMS-4008 | Cannot apply FP or PRA suspension on this notice | CRS PS restriction |
| System Error | OCMS-4007 | System error. Please inform Administrator | Unexpected system error |

---

## 3.4 Generate RIP Hirer/Driver Furnished Report

<!-- Insert flow diagram here -->
![RIP Report Flow](./images/section3-rip-report-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Cron Start (Daily) | Daily scheduled job initiates report generation | Cron job trigger |
| Query for RP2 Notices | Query database for notices with PS-RP2 applied today + deceased Hirer/Driver | Database query |
| Any records? | Check if query returns any matching records | Record check |
| End (No Report) | If no records found, end process without generating report | No report needed |
| Format Data for Report | Format query results according to report template | Data formatting |
| Generate Report File (.xlsx) | Generate Excel report file with formatted data | File generation |
| Generated OK? | Check if report file was generated successfully | Generation check |
| Send Email to OICs | Send report via email to OICs for follow-up | Email notification |
| End | Report generation process completes successfully | Success exit |
| Log Error | Log error details if report generation failed | Error logging |
| End (Error) | Process ends with error logged | Error exit |

**Shedlock Job Configuration:**

| Attribute | Value |
| --- | --- |
| Job Name | `generate_rip_hirer_driver_report` |
| Naming Pattern | `[action]_[subject]_[suffix]` (file/report operations) |
| Schedule | Daily 02:00 AM |
| Cron Expression | `0 0 2 * * ?` |
| Lock At Least | 1 minute |
| Lock At Most | 15 minutes |

*Note: Naming follows Yi Jie Guideline #14 for file/report operations*

---

### 3.4.1 Data Mapping

#### Query for RIP Report

| Zone | Database Table | Field Name | Type | Description |
| --- | --- | --- | --- | --- |
| Intranet | ocms_suspended_notice | notice_no | varchar(10) | Notice number |
| Intranet | ocms_suspended_notice | suspension_type | varchar(2) | Must be 'PS' |
| Intranet | ocms_suspended_notice | reason_of_suspension | varchar(3) | Must be 'RP2' |
| Intranet | ocms_suspended_notice | date_of_suspension | datetime2(7) | Must be today |
| Intranet | ocms_suspended_notice | date_of_revival | datetime2(7) | Must be NULL (not yet revived) |
| Intranet | ocms_valid_offence_notice | notice_no | varchar(10) | Notice number |
| Intranet | ocms_valid_offence_notice | notice_date_and_time | datetime2(7) | Offence date/time for report |
| Intranet | ocms_offence_notice_owner_driver | name | varchar(66) | Offender name |
| Intranet | ocms_offence_notice_owner_driver | id_no | varchar(12) | Offender ID (NRIC/FIN) |
| Intranet | ocms_offence_notice_owner_driver | owner_driver_indicator | varchar(1) | Must be 'H' or 'D' |
| Intranet | ocms_offence_notice_owner_driver | offender_indicator | varchar(1) | Must be 'Y' |
| Intranet | ocms_offence_notice_owner_driver | life_status | varchar(1) | Must be 'D' |
| Intranet | ocms_offence_notice_owner_driver | date_of_death | datetime2(7) | Date of death |

**Note on Revival Check:**
- `date_of_revival` = Actual date when suspension was lifted/resolved
- `due_date_of_revival` = Anticipated date when suspension will be reviewed
- To check if NOT revived, use `date_of_revival IS NULL`

**Query (Specific Fields):**

```sql
SELECT
  von.notice_no,
  ond.name AS offender_name,
  ond.id_no AS offender_id,
  ond.owner_driver_indicator,
  ond.life_status,
  ond.date_of_death,
  von.notice_date_and_time AS offence_date,
  sn.date_of_suspension
FROM ocms_suspended_notice sn
JOIN ocms_valid_offence_notice von
  ON sn.notice_no = von.notice_no
JOIN ocms_offence_notice_owner_driver ond
  ON von.notice_no = ond.notice_no
WHERE sn.suspension_type = 'PS'
  AND sn.reason_of_suspension = 'RP2'
  AND CAST(sn.date_of_suspension AS DATE) = CAST(GETDATE() AS DATE)
  AND sn.date_of_revival IS NULL  -- Not yet revived
  AND ond.owner_driver_indicator IN ('H', 'D')
  AND ond.offender_indicator = 'Y'
  AND ond.life_status = 'D'
```

**Audit Note:** Query executed as user `ocmsiz_app_conn` (read-only operation).

#### Report Output Columns

| # | Column | Source Field | Description |
| --- | --- | --- | --- |
| 1 | S/N | Generated | Sequential row number |
| 2 | Notice No | von.notice_no | Notice number |
| 3 | Vehicle No | von.vehicle_no | Vehicle registration number |
| 4 | Offender Name | ond.name | Name of deceased offender |
| 5 | ID Type | ond.id_type | Type of ID (NRIC/FIN) |
| 6 | ID No | ond.id_no | NRIC/FIN of deceased |
| 7 | Owner/Driver/Hirer | ond.owner_driver_indicator | O (Owner), H (Hirer), or D (Driver) |
| 8 | Suspension Date | sn.date_of_suspension | Date PS-RP2 was applied |
| 9 | Notice Date | von.notice_date_and_time | Notice date and time |
| 10 | Offence Rule Code | von.offence_rule_code | Offence code reference |
| 11 | Place of Offence | von.place_of_offence | Location where offence occurred |
| 12 | Composition Amount | von.composition_amount | Original composition amount |
| 13 | Amount Payable | von.amount_payable | Current amount payable |

**Note:** Date of Death column is not included in actual code implementation. Pending BA clarification if this should be added.

---

### 3.4.2 Success Outcome

- Query successfully retrieves all notices matching RP2 report criteria
- Report file (.xlsx) generated with all required columns
- Email sent to OICs with report attachment
- The workflow reaches the End state without triggering any error-handling paths

---

### 3.4.3 Error Handling

#### Application Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Database Query Error | Unable to execute report query | Log error, retry on next schedule |
| File Generation Error | Unable to create Excel file | Log error, alert administrator |
| Email Send Error | Unable to send email to OICs | Log error, store report for manual retrieval |

---

## 3.5 Redirect PS-RIP/RP2 Notices

<!-- Insert flow diagram here -->
![Redirect RIP Flow](./images/section3-redirect-rip-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | OIC initiates PS revival from Staff Portal | Manual trigger |
| OIC revives PS-RIP or PS-RP2 | OIC revives the permanent suspension from Staff Portal | Revival action |
| Update Suspension | Set date_of_revival and revival_reason in database | Update suspension record |
| OIC updates particulars | OIC updates Owner/Hirer/Driver particulars in Staff Portal | Update offender details |
| Update Offender Data | Backend updates offender data in database | Save offender changes |
| Redirect Notice | Redirect Notice to new Offender (Stage: RD1 or DN1) | Update notice stage |
| Continue Standard Flow | Notice continues standard processing flow | Normal processing |
| End | Redirect process completes | Flow exit |

**Redirect Scenarios:**

| Scenario | Description | Action |
| --- | --- | --- |
| Wrongly Furnished RIP | RIP ID was incorrectly provided | Redirect Notice to Owner |
| NOK Furnishes Hirer | Deceased offender's Next-of-Kin provides Hirer details | Redirect Notice to Hirer |
| NOK Furnishes Driver | Deceased offender's Next-of-Kin provides Driver details | Redirect Notice to Driver |

---

### 3.5.1 API Specification

#### API Revive PS-RIP/RP2 Suspension

| Field | Value |
| --- | --- |
| API Name | revivePS |
| URL | UAT: https://[domain]/ocms/v1/suspendednotice/revive <br> PRD: https://[domain]/ocms/v1/suspendednotice/revive |
| Description | Revive permanent suspension (PS-RIP or PS-RP2) from Staff Portal |
| Method | POST |
| Source | OCMS Staff Portal (Manual action by OIC) |
| Header | `{ "Authorization": "Bearer [token]", "Content-Type": "application/json" }` |
| Payload | `{ "noticeNo": "A12345678X", "revivalReason": "Redirect to correct offender", "officerAuthorisingRevival": "OIC001" }` |
| Response | `{ "data": { "appCode": "OCMS-2000", "message": "PS Revival successful" }, "noticeNo": "A12345678X" }` |
| Response Failure | `{ "data": { "appCode": "OCMS-4001", "message": "Notice does not have active PS-RIP/RP2" } }` |

#### Request Parameters

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| noticeNo | String | Yes | Notice number to revive |
| revivalReason | String | Yes | Reason for revival |
| officerAuthorisingRevival | String | Yes | OIC ID who authorized revival |

#### Sample Request

```json
{
  "noticeNo": "A12345678X",
  "revivalReason": "Redirect to correct offender",
  "officerAuthorisingRevival": "OIC001"
}
```

#### Response (Success)

```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "PS Revival successful"
  },
  "noticeNo": "A12345678X"
}
```

#### Response (Error)

```json
{
  "data": {
    "appCode": "OCMS-4001",
    "message": "Notice does not have active PS-RIP/RP2"
  }
}
```

---

### 3.5.2 Data Mapping

#### Database Tables Updated

**Table: ocms_suspended_notice (Revival)**

| Zone | Field Name | Type | Value | Description |
| --- | --- | --- | --- | --- |
| Intranet | date_of_revival | datetime2(7) | CURRENT_TIMESTAMP | Actual timestamp when PS was revived |
| Intranet | revival_reason | varchar(3) | 'PSR' | Permanent Suspension Revival |
| Intranet | officer_authorising_revival | varchar(50) | OIC ID | OIC who authorized revival |
| Intranet | revival_remarks | varchar(200) | User input | Remarks for revival |
| Intranet | upd_date | datetime2(7) | CURRENT_TIMESTAMP | Record update timestamp |
| Intranet | upd_user_id | varchar(50) | 'ocmsiz_app_conn' | Audit user |

**Table: ocms_valid_offence_notice (Clear Suspension)**

| Zone | Field Name | Type | Value | Description |
| --- | --- | --- | --- | --- |
| Intranet | suspension_type | varchar(2) | NULL | Clear suspension after revival |
| Intranet | epr_reason_of_suspension | varchar(3) | NULL | Clear suspension reason |
| Intranet | epr_date_of_suspension | datetime2(7) | NULL | Clear suspension date |
| Intranet | last_processing_stage | varchar(3) | 'RD1' or 'DN1' | Redirect stage |
| Intranet | upd_date | datetime2(7) | CURRENT_TIMESTAMP | Record update timestamp |
| Intranet | upd_user_id | varchar(50) | 'ocmsiz_app_conn' | Audit user |

**Table: ocms_offence_notice_owner_driver (Update Offender)**

| Zone | Field Name | Type | Value | Description |
| --- | --- | --- | --- | --- |
| Intranet | offender_indicator | varchar(1) | 'Y' | Set for new current offender |
| Intranet | (previous offender) | varchar(1) | 'N' | Clear previous offender flag |
| Intranet | upd_date | datetime2(7) | CURRENT_TIMESTAMP | Record update timestamp |
| Intranet | upd_user_id | varchar(50) | 'ocmsiz_app_conn' | Audit user |

**Table: ocms_offence_notice_owner_driver_addr (Update Address)**

| Zone | Field Name | Type | Description |
| --- | --- | --- | --- |
| Intranet | type_of_address | varchar(20) | Address type: lta_reg, lta_mail, mha_reg, furnished_mail |
| Intranet | blk_hse_no | varchar(10) | Block/house number |
| Intranet | floor_no | varchar(3) | Floor number |
| Intranet | unit_no | varchar(5) | Unit number |
| Intranet | postal_code | varchar(6) | Postal code |
| Intranet | street_name | varchar(32) | Street name |
| Intranet | bldg_name | varchar(65) | Building name |
| Intranet | upd_date | datetime2(7) | Record update timestamp |
| Intranet | upd_user_id | varchar(50) | Audit user: 'ocmsiz_app_conn' |

**Audit User Note:** All database operations use `ocmsiz_app_conn` as the audit user. Officer ID is captured in `officer_authorising_revival` field.

---

### 3.5.3 Success Outcome

- PS-RIP or PS-RP2 suspension successfully revived
- Suspension record updated with date_of_revival and revival_reason
- Notice details updated with new offender particulars
- Notice redirected to appropriate processing stage (RD1 or DN1)
- Notice continues along standard processing flow
- The workflow reaches the End state without triggering any error-handling paths

---

### 3.5.4 Error Handling

#### Application Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Unauthorized User | User not authorized to revive PS | Display error, block action |
| No Active PS | Notice does not have active PS-RIP/RP2 | Display error message |
| Database Error | Unable to update record | Rollback transaction, log error |

#### API Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| System Error | OCMS-4007 | System error. Please inform Administrator | Unexpected system error |
| Bad Request | OCMS-4000 | Invalid request. Please check and try again. | Invalid syntax |
| Unauthorized Access | OCMS-4001 | You are not authorized. Please log in and try again. | Auth failed |
| No Active PS Found | OCMS-4005 | No active PS-RIP/RP2 found for this notice | Revival requires active PS |

---

## Appendix A - Suspension Code Reference

| Code | Full Name | Condition | Description |
| --- | --- | --- | --- |
| RIP | Motorist Deceased On or After Offence Date | date_of_death >= offence_date | Offender was alive at time of offence but deceased now |
| RP2 | Motorist Deceased Before Offence Date | date_of_death < offence_date | Offender was already deceased at time of offence |

**Database Fields for Suspension:**

| Table | Field | Type | Description |
| --- | --- | --- | --- |
| ocms_valid_offence_notice | suspension_type | varchar(2) | 'PS' for permanent suspension |
| ocms_valid_offence_notice | epr_reason_of_suspension | varchar(3) | 'RIP' or 'RP2' |
| ocms_valid_offence_notice | epr_date_of_suspension | datetime2(7) | When EPR suspension applied |
| ocms_suspended_notice | reason_of_suspension | varchar(3) | 'RIP' or 'RP2' |
| ocms_suspended_notice | date_of_suspension | datetime2(7) | When suspension record created |
| ocms_suspended_notice | date_of_revival | datetime2(7) | When actually revived (NULL if active) |
| ocms_suspended_notice | due_date_of_revival | datetime2(7) | Anticipated review date (NULL for PS) |

**PS-RIP/RP2 Exception Code Behavior:**

PS-RIP and PS-RP2 are part of the 5 Exception Codes (DIP, FOR, MID, RIP, RP2) which have special rules:
- TS suspension can be applied on top of existing PS-RIP/RP2
- Other PS can stack without revival
- CRS PS (FP/PRA) can apply WITHOUT PS revival
- Non-CRS PS (APP, etc.) requires PS revival first

Refer to FD Section 4.4 for detailed suspension code behaviors.

---

## Appendix B - UI Display Rules

### RIP Superscript Display

| Rule | Condition | Display |
| --- | --- | --- |
| Show superscript "R" | Active PS-RIP or PS-RP2 (not revived) | Notice No with superscript R (e.g., "A12345678^R") |
| Hide superscript | No active RIP/RP2 OR already revived | Normal notice number |

**Display Locations:**
- Search Notice result summary list
- View Notice fixed header box
- View Notice Overview tab

**Detection Logic:**

```
-- Check in ocms_valid_offence_notice
IF suspension_type = 'PS'
   AND epr_reason_of_suspension IN ('RIP', 'RP2')
THEN Check if revived in ocms_suspended_notice

-- Check if NOT revived in ocms_suspended_notice
IF date_of_revival IS NULL  -- Actual revival date is blank
THEN Display superscript "R" next to Notice Number
ELSE Display normal Notice Number (suspension was revived)
```

**Note on Revival Fields:**
- `date_of_revival` = Actual date when suspension was lifted (NULL = not yet revived)
- `due_date_of_revival` = Anticipated date for review (not used for display logic)

Refer to FD Section 4.6 for detailed UI display specifications.

---

## Appendix C - Related Documents

| Document | Section | Description |
| --- | --- | --- |
| OCMS 14 FD | Section 4 | Notice Processing Flow for Deceased Offenders |
| OCMS 8 FD | Section 3.2.2 | MHA output file processing |
| OCMS 8 FD | Section 7.5 | DataHive deceased FIN processing |

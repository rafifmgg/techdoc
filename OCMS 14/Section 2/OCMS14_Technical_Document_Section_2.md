# OCMS 14 – Foreign Vehicle Notice Processing

**Prepared by**

NCS Pte Ltd

---

## Version History

| Version | Updated By | Date | Changes |
| --- | --- | --- | --- |
| v1.0 | Claude | 16/01/2026 | Document Initiation |

---

## Table of Content

| Section | Content |
| --- | --- |
| 2 | Foreign Vehicle Notice Processing |
| 2.1 | Use Case |
| 2.2 | High Level Flow |
| 2.3 | vHub Update Violation API Interface |
| 2.3.1 | Get FOR Parameter Sub-flow |
| 2.3.2 | Prepare Settled Notices Sub-flow |
| 2.3.3 | Prepare Cancelled Notices Sub-flow |
| 2.3.4 | Prepare Outstanding Notices Sub-flow |
| 2.3.5 | Call vHub API Sub-flow |
| 2.3.6 | Data Mapping |
| 2.3.7 | Success Outcome |
| 2.3.8 | Error Handling |
| 2.4 | vHub SFTP Interface |
| 2.4.1 | Data Mapping |
| 2.4.2 | Success Outcome |
| 2.4.3 | Error Handling |
| 2.5 | REPCCS Listed Vehicles Interface |
| 2.5.1 | Data Mapping |
| 2.5.2 | Success Outcome |
| 2.5.3 | Error Handling |
| 2.6 | CES EHT Tagged Vehicles Interface |
| 2.6.1 | Data Mapping |
| 2.6.2 | Success Outcome |
| 2.6.3 | Error Handling |
| 2.7 | Admin Fee Processing |
| 2.7.1 | Data Mapping |
| 2.7.2 | Success Outcome |
| 2.7.3 | Error Handling |

---

# Section 2 – Foreign Vehicle Notice Processing

## 2.1 Use Case

1. Foreign vehicle (FOR) notices are automatically suspended with PS-FOR status upon creation.

2. After a configurable number of days (FOR parameter), unpaid FOR notices are sent to enforcement agencies:<br>a. vHub (ICA border enforcement)<br>b. REPCCS (car park enforcement)<br>c. CES EHT (Certis enforcement)

3. An admin fee is added to unpaid FOR notices after a configurable period (FOD parameter).

4. The system sends notice updates to vHub with three status types:<br>a. Outstanding (O) - unpaid notices<br>b. Settled (S) - paid notices<br>c. Cancelled (C) - notices that are TS-ed, PS-ed, or archived

5. Unpaid notices are eventually archived and removed from the database after the retention period.

---

## 2.2 High Level Flow

![High Level Flow](./images/section2-highlevel-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Pre-condition | Raw offence data source sends offence data with veh reg type = F | Data sources: REPCCS, EHT, EEPS, PLUS Staff Portal & OCMS Staff Portal |
| Pre-condition | Process offence data & detect veh reg type = F | System identifies foreign vehicle registration |
| Pre-condition | Create Notice for Foreign Vehicle | Notice is created in the system |
| PS-FOR Suspension | Suspend Notice with PS-FOR | Notice is automatically suspended with PS-FOR status |
| Enforcement | Send the vehicle number to vHub, REPCCS and Certis for enforcement | After X days, notice info sent to enforcement agencies |
| Admin Fee | Add Admin Fee | After Y days, admin fee is added to unpaid notices |
| Archival | Unpaid Notice will be archived and removed from the database | After Z years, notices are archived |

---

## 2.3 vHub Update Violation API Interface

### CRON Job Specification

| Attribute | Value |
| --- | --- |
| CRON Name | vHub Update Violation API Interface |
| Trigger | Scheduled (TBD timing) |
| Frequency | Daily |
| Purpose | Send FOR notices to vHub via REST API |
| Shedlock Name | vhub_update_violation_api |

### Main Flow

![vHub API Main Flow](./images/section2-vhub-api-main.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | CRON job triggers at scheduled time | Entry point of the workflow |
| Get FOR Parameter | Query ocms_parameter table for FOR value and calculate date | Refer to sub-flow 2.3.1 |
| Prepare Settled List | Query FOR notices paid in last 24 hours | Refer to sub-flow 2.3.2 |
| Prepare Cancelled List | Query FOR notices TS-ed/PS-ed today or scheduled for archival | Refer to sub-flow 2.3.3 |
| Prepare Outstanding List | Query unpaid FOR notices past FOR period | Refer to sub-flow 2.3.4 |
| Consolidate Lists | Merge all 3 lists for vHub API call | Combine Settled, Cancelled, Outstanding |
| Any records to send? | Decision: Check if there are records to process | If no records, log and end |
| Call vHub API | Send records to vHub in batches of 50 | Refer to sub-flow 2.3.5 |
| Consolidate errors | Collect all interfacing errors | Prepare error report |
| Any errors? | Decision: Check if any errors occurred | Determine if email needed |
| Send error email | Send interfacing error email to operation team | Notify operations of failures |
| End | Process completes | Exit point of workflow |

---

### 2.3.1 Get FOR Parameter Sub-flow

![Get FOR Parameter](./images/section2-get-for-param.png)

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | Sub-flow entry point | Begin parameter retrieval |
| Query ocms_parameter | Query table for FOR value | param_id = 'FOR', param_code = 'NPA' |
| Parameter found? | Decision: Check if parameter exists | Verify configuration |
| Throw error | Stop processing if parameter not found | Critical configuration missing |
| Calculate date | Today minus FOR days | Determine cutoff date |
| Store in memory | Save calculated date for subsequent queries | Used in list preparation |
| End Sub-flow | Return to main flow | Continue processing |

#### Query Specification

| Attribute | Value |
| --- | --- |
| Table | ocms_parameter |
| Zone | Intranet |
| Fields | param_value |
| Condition | param_id = 'FOR' AND param_code = 'NPA' |

---

### 2.3.2 Prepare Settled Notices Sub-flow

![Settled Notices](./images/section2-settled-notices.png)

**Settled (Paid) Criteria:**
- FOR Notices which have been paid in the last 24 hours

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | Sub-flow entry point | Begin settled list preparation |
| Query VON | Get FOR notices paid in last 24 hours | Query ocms_valid_offence_notice |
| Any records? | Decision: Check if paid notices found | Determine if processing needed |
| Call REPCCS API | Get car park list from REPCCS | Used to populate location names |
| Store car park list | Save in memory as dictionary | Enable fast lookups |
| For each notice | Loop through all notices | Process each record |
| Query PP code dictionary | Get car park name for notice | Lookup by pp_code |
| PP code found? | Decision: Check if car park exists | Handle missing codes |
| Skip record | Continue with remaining records | Exclude invalid records |
| Add car park name | Add location name to record | Populate vHub payload field |
| Vehicle category = H or Y? | Decision: Check vehicle type mapping | Handle special cases |
| Map vehicle type | H → B (Bus), Y → M (Motorcycle) | vHub type conversion |
| Gen Settled records | Generate list following vHub format | Prepare for API call |
| Set status = 'S' | Mark records as Settled | Violation status code |
| End Sub-flow | Return to main flow | Continue processing |

#### Query Specification

| Attribute | Value |
| --- | --- |
| Table | ocms_valid_offence_notice |
| Zone | Intranet |
| Conditions | vehicle_registration_type = 'F'<br>amount_paid > 0<br>payment_date >= CURRENT_DATE - 1 day |

#### Vehicle Type Mapping

| OCMS Category | vHub Type | Description |
| --- | --- | --- |
| C | C | Car |
| M | M | Motorcycle |
| H | B | Heavy Vehicle → Bus |
| Y | M | Mapped to Motorcycle |
| B | B | Bus |

**Note:** vHub does not have 'H' (Heavy Vehicle) type, so OCMS maps 'H' to 'B' (Bus).

---

### 2.3.3 Prepare Cancelled Notices Sub-flow

![Cancelled Notices](./images/section2-cancelled-notices.png)

**Cancelled Criteria:**
- FOR Notices which were TS-ed or PS-ed today
- FOR Notices which are scheduled for archival tomorrow

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | Sub-flow entry point | Begin cancelled list preparation |
| Query TS/PS notices | Get FOR notices TS-ed or PS-ed today | Query for recent suspensions |
| Query archive notices | Get FOR notices scheduled for archival tomorrow | Query for pending archival |
| Combine data | Merge both query results into 1 list | Consolidate cancelled records |
| Any records? | Decision: Check if records found | Determine if processing needed |
| Query PP code dictionary | Get car park name for notices | Lookup locations |
| Vehicle category = H or Y? | Decision: Check vehicle type mapping | Handle special cases |
| Map vehicle type | H → B, Y → M | vHub type conversion |
| Notice in Settled list? | Decision: Check for duplicates | Avoid sending twice |
| Remove from Cancelled list | Remove duplicate record | Settled takes priority |
| Gen Cancelled records | Generate list with status = 'C' | Prepare for API call |
| End Sub-flow | Return to main flow | Continue processing |

#### Query Specification - TS/PS Notices

| Attribute | Value |
| --- | --- |
| Table | ocms_valid_offence_notice |
| Zone | Intranet |
| Conditions | vehicle_registration_type = 'F'<br>suspension_type IN ('TS', 'PS')<br>suspension_date = CURRENT_DATE<br>epr_reason_of_suspension != 'FOR' |

#### Query Specification - Archive Notices

| Attribute | Value |
| --- | --- |
| Table | ocms_valid_offence_notice |
| Zone | Intranet |
| Conditions | vehicle_registration_type = 'F'<br>archive_date = CURRENT_DATE + 1 day |

**Note:** A settled notice takes priority. If a notice appears in both Settled and Cancelled lists, it should only be sent as Settled.

---

### 2.3.4 Prepare Outstanding Notices Sub-flow

![Outstanding Notices](./images/section2-outstanding-notices.png)

**Outstanding (Unpaid) Criteria:**
- FOR Notices which remain unpaid X days after Offence Date
- Unpaid older FOR Notices in which VON fields were updated (eg. PP code, suspension lifted)

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | Sub-flow entry point | Begin outstanding list preparation |
| Query new FOR notices | Get FOR notices X days after Offence Date | First-time send to vHub |
| Query updated FOR notices | Get older FOR notices with recent updates | Re-send updated records |
| Check ocms_offence_avss | Batch query to check if records sent today | Avoid duplicate sends |
| Any match in AVSS? | Decision: Check for today's records | Identify duplicates |
| Remove duplicates | Remove records already sent today | Prevent re-sending |
| Combine data | Merge both lists into 1 | Consolidate outstanding records |
| Any records? | Decision: Check if records found | Determine if processing needed |
| Query PP code dictionary | Get car park name for notices | Lookup locations |
| PP code found? | Decision: Check if car park exists | Handle missing codes |
| Skip record | Continue with remaining records | Exclude invalid records |
| Add car park name | Add location name to record | Populate vHub payload field |
| Vehicle category = H or Y? | Decision: Check vehicle type mapping | Handle special cases |
| Map vehicle type | H → B, Y → M | vHub type conversion |
| Notice in Settled/Cancelled? | Decision: Check for duplicates | Avoid sending twice |
| Remove from Outstanding | Remove duplicate record | Settled/Cancelled takes priority |
| Gen Outstanding records | Generate list with status = 'O' | Prepare for API call |
| End Sub-flow | Return to main flow | Continue processing |

#### Query Specification - New FOR Notices

| Attribute | Value |
| --- | --- |
| Table | ocms_valid_offence_notice |
| Zone | Intranet |
| Conditions | vehicle_registration_type = 'F'<br>notice_date_and_time = CURRENT_DATE - FOR days<br>suspension_type = 'PS'<br>epr_reason_of_suspension = 'FOR'<br>amount_paid = 0 |

#### Query Specification - Updated FOR Notices

| Attribute | Value |
| --- | --- |
| Table | ocms_valid_offence_notice |
| Zone | Intranet |
| Conditions | vehicle_registration_type = 'F'<br>notice_date_and_time < CURRENT_DATE - FOR days<br>suspension_type = 'PS'<br>epr_reason_of_suspension = 'FOR'<br>amount_paid = 0<br>upd_date >= CURRENT_DATE - 1 day |

#### AVSS Duplicate Check Query

| Attribute | Value |
| --- | --- |
| Table | ocms_offence_avss |
| Zone | Intranet |
| Conditions | offence_no IN (...)<br>batch_date = CURRENT_DATE<br>record_status = 'O' |

**Note:** Check AVSS because VON query is general, not restricted by last updated date/time. This step reduces chance of re-sending duplicate for new FOR Notice.

---

### 2.3.5 Call vHub API Sub-flow

![Call vHub API](./images/section2-call-vhub-api.png)

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | Sub-flow entry point | Begin API call process |
| Prepare batch of 50 | Split records into batches | Max 50 records per request |
| Call vHub API | Send POST request to vHub | Update Violation Request API |
| API call success? | Decision: Check HTTP response | Verify connection |
| Retry 2x | Retry API call up to 2 times | Handle transient failures |
| Retry success? | Decision: Check retry result | Determine recovery |
| Add batch to error log | Log entire batch as failed | Track failed records |
| Process API response | Parse response per record | Handle individual results |
| For each record | Loop through response records | Process each result |
| Record sent OK? (code=0) | Decision: Check record status | Verify success |
| Save to DB with success | Insert to ocms_offence_avss with success status | Record successful send |
| Save to DB with error | Insert to ocms_offence_avss with error code | Record failed send |
| More records? | Decision: Check for remaining records | Continue loop |
| More batches? | Decision: Check for remaining batches | Continue processing |
| Consolidate errors | Collect all errors for email | Prepare error report |
| End Sub-flow | Return to main flow | Continue processing |

#### API Specification

| Attribute | Value |
| --- | --- |
| Type | REST API |
| Method | POST |
| Direction | OCMS → vHub |
| Batch Size | 50 records per request |
| Reference | vHub External Agency API Interface Requirement Specifications v1.0 |

#### Request Payload Fields

| vHub Field | OCMS Field | Source Table | Description |
| --- | --- | --- | --- |
| ViolationNo | offence_no | ocms_offence_avss | Notice number |
| OffenceDateTime | offence_date + offence_time | ocms_offence_avss | Format: YYYYMMDDHHmmss |
| OffenceCode | offence_code | ocms_offence_avss | Offence rule code |
| Location | location | ocms_offence_avss | Offence location |
| OffenceDescription | offence_description | ocms_offence_avss | Description of offence |
| VehicleNo | vehicle_no | ocms_offence_avss | Vehicle registration |
| VehicleType | vehicle_type | ocms_offence_avss | B=Bus, M=Motorcycle, C=Car |
| VehicleMake | vehicle_make | ocms_offence_avss | Vehicle make |
| VehicleColor | vehicle_color | ocms_offence_avss | Vehicle color |
| CompositionAmount | composition_amount | ocms_offence_avss | Fine amount |
| AdminFee | admin_fee | ocms_offence_avss | Admin fee amount |
| TotalAmount | total_amount | ocms_offence_avss | Total payable |
| ViolationStatus | violation_status | ocms_offence_avss | O/S/C |

#### Violation Status Codes

| Code | Status | Description |
| --- | --- | --- |
| O | Outstanding | Unpaid notices |
| S | Settled | Paid notices |
| C | Cancelled | TS-ed, PS-ed, or archived notices |

#### Response Codes

| Response Code | Description | Action |
| --- | --- | --- |
| 0 | Success | Record sent successfully |
| 1 | Error | Log error, add to error report |

#### Error Codes from vHub

| Error Code | Description |
| --- | --- |
| E_REC_001 | Exceed Field Limit |
| E_REC_002 | Invalid Format |
| E_REC_003 | Missing Required Field |

---

### 2.3.6 Data Mapping

#### ocms_valid_offence_notice (Source)

| Zone | Database Table | Field Name |
| --- | --- | --- |
| Intranet | ocms_valid_offence_notice | offence_no, vehicle_no, offence_date_and_time, offence_rule, pp_code, vehicle_category, composition_amount, admin_fee, suspension_type, epr_reason_of_suspension, amount_paid, payment_date, archive_date |

#### ocms_offence_avss (Target)

| Zone | Database Table | Field Name |
| --- | --- | --- |
| Intranet | ocms_offence_avss | batch_date, offence_no, offence_date, offence_time, offence_code, location, vehicle_no, vehicle_type, violation_status, vhub_api_status_code, vhub_api_error_code, vhub_api_error_description |

#### ocms_parameter (Reference)

| Zone | Database Table | Field Name |
| --- | --- | --- |
| Intranet | ocms_parameter | param_id, param_code, param_value |

---

### 2.3.7 Success Outcome

- FOR parameter value is successfully retrieved from ocms_parameter.
- Settled, Cancelled, and Outstanding notice lists are successfully prepared.
- All records are successfully sent to vHub API.
- API responses are processed and stored in ocms_offence_avss.
- No error emails are sent (all records processed successfully).
- Workflow completes and reaches End state.

---

### 2.3.8 Error Handling

#### Application Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| FOR parameter not found | param_id = 'FOR' not in ocms_parameter | Stop processing, throw error |
| REPCCS API failure | Unable to get car park list | Retry 3 times (immediate, 1s, 1s) |
| Database query timeout | Query takes too long | Retry 1 time, log error |

#### API Error Handling

| Error Scenario | Action | Recovery |
| --- | --- | --- |
| API call timeout | Retry up to 2 times | Log error, continue with next batch |
| API returns error code | Log error per record | Add to error report, send email |
| Partial batch failure | Process successful records | Failed records retried in next run |

#### Error Codes

| Error Code | Description | Action |
| --- | --- | --- |
| E_REC_001 | Exceed Field Limit | Log error, review field lengths |
| E_REC_002 | Invalid Format | Log error, review data format |
| E_REC_003 | Missing Required Field | Log error, review mandatory fields |

---

## 2.4 vHub SFTP Interface

### CRON Job Specification

| Attribute | Value |
| --- | --- |
| CRON Name | vHub Violation Case Create/Update SFTP |
| Trigger | Scheduled (TBD timing) |
| Frequency | Daily |
| Purpose | Send FOR notices to vHub via SFTP (backup/reconciliation) |

### Flow Description

![vHub SFTP Flow](./images/section2-vhub-sftp.png)

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | CRON job triggers at scheduled time | Entry point of workflow |
| Prepare lists | Same logic as API flow | Settled, Cancelled, Outstanding |
| Generate XML file | Create XML with violation records | File format: XML |
| Upload to Azure | Store file in Azure Blob Storage | Backup storage |
| Azure success? | Decision: Check upload result | Verify Azure upload |
| Retry 2x | Retry Azure upload | Handle transient failures |
| Upload to SFTP | Transfer file to vHub SFTP server | File transfer |
| SFTP success? | Decision: Check upload result | Verify SFTP upload |
| Retry 2x, send error email | Retry then notify operations | Handle failures |
| Save to DB | Store results in ocms_offence_avss_sftp | Record processing |
| End | Process completes | Exit point of workflow |

#### File Specification

| Attribute | Value |
| --- | --- |
| Direction | OCMS → vHub |
| Format | XML |
| File Name | URA_VHUB_VIOLATION_YYYYMMDD_HHMMSS.xml |
| Reference | vHub External Agency Interface Requirement Specifications v1.3 |

---

### 2.4.1 Data Mapping

| Zone | Database Table | Field Name |
| --- | --- | --- |
| Intranet | ocms_valid_offence_notice | Source data for XML |
| Intranet | ocms_offence_avss_sftp | Processing results |
| External | Azure Blob Storage | File storage |
| External | vHub SFTP Server | File destination |

---

### 2.4.2 Success Outcome

- XML file is successfully generated with all violation records.
- File is uploaded to Azure Blob Storage.
- File is transferred to vHub SFTP server.
- Results are stored in ocms_offence_avss_sftp.
- No error emails are sent.

---

### 2.4.3 Error Handling

| Error Scenario | Action | Recovery |
| --- | --- | --- |
| File generation failed | Log error | Send interfacing error email |
| Azure upload failed | Retry up to 2 times | Send interfacing error email |
| SFTP connection failed | Retry up to 2 times | Send interfacing error email |
| SFTP upload failed | Retry up to 2 times | Send interfacing error email |

---

## 2.5 REPCCS Listed Vehicles Interface

### CRON Job Specification

| Attribute | Value |
| --- | --- |
| CRON Name | Gen REP Listed Vehicle |
| Trigger | Scheduled (TBD timing) |
| Frequency | Daily |
| Purpose | Send listed vehicles to REPCCS for car park enforcement |

### Flow Description

![REPCCS Listed Vehicles](./images/section2-repccs-listed.png)

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | CRON job triggers at scheduled time | Entry point of workflow |
| Get FOR parameter | Query ocms_parameter for FOR value | Get days threshold |
| Query listed vehicles | Get qualifying notices from VON | Apply query conditions |
| Generate file | Create Listed Vehicle file | Format: CSV/TXT |
| Upload to Azure | Store file in Azure Blob Storage | Backup storage |
| Upload to SFTP | Transfer file to REPCCS SFTP server | File transfer |
| SFTP success? | Decision: Check upload result | Verify upload |
| Send error email | Notify operations of failure | Handle failures |
| End | Process completes | Exit point of workflow |

#### Query Conditions

| Field | Condition |
| --- | --- |
| suspension_type | = 'PS' |
| epr_reason_of_suspension | = 'FOR' |
| amount_paid | = 0 |
| notice_date_and_time | <= CURRENT_DATE - FOR days |

---

### 2.5.1 Data Mapping

#### File Fields

| REPCCS Field | OCMS Field | Source Table |
| --- | --- | --- |
| VEHICLE_NO | vehicle_no | ocms_valid_offence_notice |
| OFFENCE_DATE | offence_date_and_time | ocms_valid_offence_notice |
| OFFENCE_CODE | offence_rule | ocms_valid_offence_notice |
| LOCATION | pp_code | ocms_valid_offence_notice |
| AMOUNT | composition_amount | ocms_valid_offence_notice |

#### File Specification

| Attribute | Value |
| --- | --- |
| Direction | OCMS → REPCCS |
| Format | CSV/TXT |
| Frequency | Daily |
| Reference | REPCCS Interfaces to Other URA System(s) v2.0 |

---

### 2.5.2 Success Outcome

- Listed vehicle file is successfully generated.
- File is uploaded to Azure Blob Storage.
- File is transferred to REPCCS SFTP server.
- REPCCS receives vehicle list for car park enforcement.

---

### 2.5.3 Error Handling

| Error Scenario | Action | Recovery |
| --- | --- | --- |
| Query failure | Log error | Send interfacing error email |
| File generation failed | Log error | Send interfacing error email |
| SFTP upload failed | Retry | Send interfacing error email |

---

## 2.6 CES EHT Tagged Vehicles Interface

### CRON Job Specification

| Attribute | Value |
| --- | --- |
| CRON Name | Gen CES Tagged Vehicle List |
| Trigger | Scheduled (TBD timing) |
| Frequency | Daily |
| Purpose | Send tagged vehicles to CES EHT for Certis enforcement |

### Flow Description

![CES EHT Tagged Vehicles](./images/section2-ces-eht.png)

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | CRON job triggers at scheduled time | Entry point of workflow |
| Get FOR parameter | Query ocms_parameter for FOR value | Get days threshold |
| Query tagged vehicles | Get qualifying notices from VON | Apply query conditions |
| Generate file | Create Tagged Vehicle file | Format: CSV/TXT |
| Upload to Azure | Store file in Azure Blob Storage | Backup storage |
| Upload to SFTP | Transfer file to CES EHT SFTP server | File transfer |
| SFTP success? | Decision: Check upload result | Verify upload |
| Send error email | Notify operations of failure | Handle failures |
| End | Process completes | Exit point of workflow |

---

### 2.6.1 Data Mapping

#### File Fields

| CES EHT Field | OCMS Field | Source Table |
| --- | --- | --- |
| VEHICLE_NO | vehicle_no | ocms_valid_offence_notice |
| OFFENCE_DATE | offence_date_and_time | ocms_valid_offence_notice |
| OFFENCE_CODE | offence_rule | ocms_valid_offence_notice |
| AMOUNT | composition_amount | ocms_valid_offence_notice |

#### File Specification

| Attribute | Value |
| --- | --- |
| Direction | OCMS → CES EHT |
| Format | CSV/TXT |
| Frequency | Daily |

---

### 2.6.2 Success Outcome

- Tagged vehicle file is successfully generated.
- File is uploaded to Azure Blob Storage.
- File is transferred to CES EHT SFTP server.
- CES EHT receives vehicle list for Certis enforcement.

---

### 2.6.3 Error Handling

| Error Scenario | Action | Recovery |
| --- | --- | --- |
| Query failure | Log error | Send interfacing error email |
| File generation failed | Log error | Send interfacing error email |
| SFTP upload failed | Retry | Send interfacing error email |

---

## 2.7 Admin Fee Processing

### CRON Job Specification

| Attribute | Value |
| --- | --- |
| CRON Name | Admin Fee Processing |
| Trigger | Scheduled (TBD timing) |
| Frequency | Daily |
| Purpose | Add admin fee to unpaid FOR notices |

### Flow Description

![Admin Fee Processing](./images/section2-admin-fee.png)

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | CRON job triggers at scheduled time | Entry point of workflow |
| Get FOD parameter | Query ocms_parameter for FOD value (days) | Days before admin fee |
| Get AFO parameter | Query ocms_parameter for AFO value (amount) | Admin fee amount |
| Query eligible notices | Get FOR notices unpaid past FOD period | Apply query conditions |
| Any records? | Decision: Check if records found | Determine if processing needed |
| Batch update notices | Add AFO amount to composition | Update composition_amount |
| Prepare for vHub | Updated notices sent to vHub as Outstanding | Next API run sends updates |
| End | Process completes | Exit point of workflow |

#### Query Conditions

| Field | Condition |
| --- | --- |
| vehicle_registration_type | = 'F' |
| suspension_type | = 'PS' |
| epr_reason_of_suspension | = 'FOR' |
| amount_paid | = 0 |
| notice_date_and_time | <= CURRENT_DATE - FOD days |
| admin_fee | = 0 (not yet applied) |

#### Admin Fee Calculation

```
new_composition = composition_amount + AFO
admin_fee = AFO
```

**Example:** If AFO = $10 and original composition = $50, new composition = $60

---

### 2.7.1 Data Mapping

#### Parameter Table

| Parameter ID | Code | Description |
| --- | --- | --- |
| FOD | NPA | Days before adding admin fee |
| AFO | NPA | Admin fee amount |

#### Database Update

| Zone | Database Table | Field Name |
| --- | --- | --- |
| Intranet | ocms_valid_offence_notice | composition_amount, admin_fee, upd_date, upd_user_id |

#### Update Statement

```sql
UPDATE ocms_valid_offence_notice
SET composition_amount = composition_amount + <AFO>,
    admin_fee = <AFO>,
    upd_date = CURRENT_TIMESTAMP,
    upd_user_id = 'ocmsiz_app_conn'
WHERE offence_no IN (...)
```

---

### 2.7.2 Success Outcome

- FOD and AFO parameter values are successfully retrieved.
- Eligible notices are identified and updated.
- Admin fee is added to composition amount.
- Updated notices are picked up by next vHub API run as Outstanding.

---

### 2.7.3 Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| FOD parameter not found | param_id = 'FOD' not in ocms_parameter | Stop processing, log error |
| AFO parameter not found | param_id = 'AFO' not in ocms_parameter | Stop processing, log error |
| Update failed | Database update error | Rollback transaction, log error |

---

## Appendix A: System Architecture

### Systems Involved

| System | Type | Purpose |
| --- | --- | --- |
| OCMS Backend | Internal | Main processing system |
| vHub | External | Border enforcement (ICA) |
| REPCCS | External | Car park enforcement |
| CES EHT | External | Certis enforcement |
| Azure Blob Storage | External | File storage |
| SFTP Server | External | File transfer |

### Integration Methods

| External System | Method | Direction |
| --- | --- | --- |
| vHub | REST API | OCMS → vHub |
| vHub | SFTP | OCMS → vHub |
| vHub | SFTP (ACK) | vHub → OCMS |
| REPCCS | SFTP | OCMS → REPCCS |
| CES EHT | SFTP | OCMS → CES EHT |

---

## Appendix B: Database Tables

### Primary Tables

| Table | Zone | Purpose |
| --- | --- | --- |
| ocms_valid_offence_notice | Intranet | Main notice table |
| ocms_offence_avss | Intranet | vHub API records |
| ocms_offence_avss_sftp | Intranet | vHub SFTP records |
| ocms_parameter | Intranet | System parameters |

### Key Fields in ocms_offence_avss

| Field | Type | Description |
| --- | --- | --- |
| batch_date | datetime2 | Date batch was generated |
| offence_no | varchar(10) | Notice number |
| offence_date | datetime2 | Date of offence |
| offence_time | datetime2 | Time of offence |
| offence_code | integer | Offence rule code |
| location | varchar(100) | Offence location |
| vehicle_no | varchar(14) | Vehicle number |
| vehicle_type | varchar(1) | Vehicle type code |
| violation_status | varchar(1) | O/S/C |
| vhub_api_status_code | varchar(1) | 0=Success, 1=Error |
| vhub_api_error_code | varchar(20) | Error code from vHub |
| vhub_api_error_description | varchar(200) | Error description |

---

*End of Document*

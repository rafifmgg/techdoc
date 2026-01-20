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

> **Reference:** Refer to FD OCMS 14 Section 3.1 - 3.2 for complete business requirements and user stories.

**Summary of Use Case (from FD):**

1. Foreign vehicle (FOR) notices are automatically suspended with PS-FOR status upon creation.

2. After a configurable number of days (FOR parameter), unpaid FOR notices are sent to enforcement agencies:<br>a. vHub (ICA border enforcement)<br>b. REPCCS (car park enforcement)<br>c. CES EHT (Certis enforcement)

3. An admin fee is added to unpaid FOR notices after a configurable period (FOD parameter).

4. The system sends notice updates to vHub with three status types:<br>a. Outstanding (O) - unpaid notices<br>b. Settled (S) - paid notices<br>c. Cancelled (C) - notices that are TS-ed, PS-ed, or archived

5. Unpaid notices are eventually archived and removed from the database after the retention period.

**Additional FD References:**
| Topic | FD Reference |
| --- | --- |
| PS-FOR Suspension Rules | FD Section 3.3.3 |
| Exit Conditions | FD Section 3.2.1 |
| vHub API Specification | FD Section 3.4.1 |
| vHub SFTP Specification | FD Section 3.4.2 |
| REPCCS Interface | FD Section 3.5 |
| CES EHT Interface | FD Section 3.6 |
| Admin Fee Rules | FD Section 3.7 |

### CRON Job Schedule Summary

| # | CRON Job | Time | CRON Expression | Shedlock Name |
| --- | --- | --- | --- | --- |
| 1 | Admin Fee Processing | 01:00 AM | `0 0 1 * * ?` | admin_fee_processing |
| 2 | vHub Update Violation API | 02:00 AM | `0 0 2 * * ?` | vhub_update_violation_api |
| 3 | vHub SFTP Create/Update | 03:00 AM | `0 0 3 * * ?` | vhub_sftp_create_update |
| 4 | REPCCS Listed Vehicle | 04:00 AM | `0 0 4 * * ?` | gen_rep_listed_vehicle |
| 5 | CES EHT Tagged Vehicle | 04:30 AM | `0 30 4 * * ?` | gen_ces_tagged_vehicle |
| 6 | Batch Job Stuck Detection | Every 30 min | `0 */30 * * * ?` | batch_job_stuck_detection |

> **Note:** All CRON timings are suggested values and need to be confirmed with Operations team.

### System Actors & User Roles

#### System Actors (WHO triggers/uses this feature)

| Actor Type | Actor Name | Role | Description |
| --- | --- | --- | --- |
| **System** | OCMS Scheduler | Trigger | Initiates all CRON jobs based on schedule |
| **System** | OCMS Backend | Processor | Executes business logic, queries, API calls |
| **System** | vHub System | Receiver | Receives violation records for ICA enforcement |
| **System** | REPCCS System | Receiver | Receives listed vehicles for car park enforcement |
| **System** | CES EHT System | Receiver | Receives tagged vehicles for Certis enforcement |
| **System** | Azure Blob | Storage | Stores backup files before SFTP transfer |
| **System** | Email Service | Notifier | Sends error alert emails to operations |

#### User Roles (WHO is affected/notified)

| Role | Access | Responsibility |
| --- | --- | --- |
| **Operations Team** | Receive error emails | Monitor job failures, investigate issues |
| **System Admin** | CRON configuration | Configure job schedules, parameters |
| **DBA** | Database access | Monitor batch job tracking, archival |
| **ICA Officer** | vHub system | Act on outstanding violations at border |
| **REPCCS Operator** | REPCCS system | Enforce parking violations at car parks |
| **Certis Officer** | CES EHT system | Enforce parking violations on-ground |

#### Data Flow by Actor

```
┌─────────────────────────────────────────────────────────────────────┐
│                         ACTOR DATA FLOW                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  OCMS Scheduler ──► OCMS Backend ──┬──► vHub (ICA)                  │
│       │                            │                                 │
│       │                            ├──► REPCCS (Car Park)           │
│       │                            │                                 │
│       │                            ├──► CES EHT (Certis)            │
│       │                            │                                 │
│       │                            └──► Azure Blob (Backup)         │
│       │                                                              │
│       └──► On Error ──► Email Service ──► Operations Team           │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

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
| Trigger | Scheduled |
| CRON Expression | `0 0 2 * * ?` (Daily at 02:00 AM) |
| Frequency | Daily |
| Purpose | Send FOR notices to vHub via REST API |
| Shedlock Name | vhub_update_violation_api |
| Lock Duration | 2 hours (max) |

> **Note:** CRON timing to be confirmed with Operations team. Suggested 02:00 AM to avoid peak hours.

### Batch Job Tracking

**Purpose:** Track job execution to identify stuck jobs, failed jobs, and jobs that did not start.

> **[NEW TABLE PROPOSAL]** The table `ocms_batch_job_tracking` below is a **proposed new table** for OCMS 14.
> Current Data Dictionary has `ocms_batch_job` with limited fields. Schema change request required.

#### Tracking Table: ocms_batch_job_tracking [NEW TABLE]

| Field | Type | Description | Status |
| --- | --- | --- | --- |
| job_id | varchar(50) | Unique job identifier (UUID) | NEW |
| job_name | varchar(100) | Shedlock name (e.g., vhub_update_violation_api) | NEW |
| job_start_time | datetime2 | Timestamp when job started | NEW |
| job_end_time | datetime2 | Timestamp when job ended (NULL if still running) | NEW |
| job_status | varchar(20) | STARTED, COMPLETED, FAILED, STUCK | NEW |
| records_processed | integer | Number of records processed | NEW |
| records_success | integer | Number of successful records | NEW |
| records_failed | integer | Number of failed records | NEW |
| error_message | varchar(500) | Error message if failed | NEW |
| cre_date | datetime2 | Record creation date | NEW |
| upd_date | datetime2 | Record update date | NEW |

**Alternative:** Use existing `ocms_batch_job` table from Data Dictionary:
| Field | Type | Description |
| --- | --- | --- |
| batch_job_id | integer | Primary key (existing) |
| name | varchar(64) | Job name (existing) |
| run_status | varchar(1) | S=Success, F=Failed (existing) |
| start_run | datetime2 | Start timestamp (existing) |
| end_run | datetime2 | End timestamp (existing) |
| log_text | text | Detailed log (existing) |

#### Tracking Flow

| Step | Action | Description |
| --- | --- | --- |
| 1 | Job Start | INSERT record with job_status = 'STARTED', job_start_time = NOW() |
| 2 | Processing | UPDATE records_processed count periodically (every 100 records) |
| 3 | Job End (Success) | UPDATE job_status = 'COMPLETED', job_end_time = NOW() |
| 4 | Job End (Failure) | UPDATE job_status = 'FAILED', error_message = [error] |
| 5 | Stuck Detection | Separate CRON checks for jobs with STARTED status > 2 hours |

#### Stuck Job Detection CRON

| Attribute | Value |
| --- | --- |
| CRON Name | Batch Job Stuck Detection |
| Frequency | Every 30 minutes |
| Purpose | Detect jobs that started but did not end |
| Action | Mark as STUCK, send alert email |

**Detection Query:**
```sql
SELECT job_id, job_name, job_start_time
FROM ocms_batch_job_tracking
WHERE job_status = 'STARTED'
AND job_start_time < DATEADD(hour, -2, GETDATE())
```

#### Archival Policy

| Rule | Value |
| --- | --- |
| Retention Period | 3 months |
| Archive Frequency | Monthly |
| Action | DELETE records older than 3 months |

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
| Retry 3x | Retry API call up to 2 times | Handle transient failures |
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

#### Token Handling

| Attribute | Value |
| --- | --- |
| Authentication | OAuth 2.0 Bearer Token |
| Token Storage | Cached in memory with expiry timestamp |
| Token Expiry | As per vHub specification (typically 1 hour) |

**Token Refresh Flow:**

| Step | Action | Description |
| --- | --- | --- |
| 1 | Check Token | Before API call, check if token exists and is valid |
| 2 | Token Valid? | If token exists and not expired → Use existing token |
| 3 | Token Invalid/Expired | If token is null, expired, or invalid → Request new token |
| 4 | Request New Token | Call vHub token endpoint with credentials |
| 5 | Store Token | Cache new token with expiry timestamp |
| 6 | Retry API Call | Continue with API call using new token |
| 7 | Token Refresh Failure | If token refresh fails after 3 attempts → Log error, send alert email |

**Token Error Handling:**

| Scenario | Action | Recovery |
| --- | --- | --- |
| Token expired during batch | Auto-refresh token | Retry current batch with new token |
| Token refresh failed | Retry 3 times | Log error, skip batch, continue with next |
| Invalid credentials | Stop processing | Alert operations team immediately |

**Important:** Processing must NOT stop due to token expiry. System must auto-refresh and continue.

#### Retry Standard (Per Yi Jie Guideline #13)

| Scenario | Retry Count | Wait Between Retries | After All Retries Fail |
| --- | --- | --- | --- |
| vHub API connection failure | 3 times | Exponential backoff (1s, 2s, 4s) | Send email alert, continue next batch |
| vHub API timeout | 3 times | Exponential backoff | Send email alert, continue next batch |
| SFTP connection failure | 3 times | Fixed interval (5s) | Send email alert, end job |
| Azure upload failure | 3 times | Fixed interval (5s) | Send email alert, skip SFTP |

> **Yi Jie Standard:** "In the event OCMS calls external API and fails to connect, it should auto retry for 3 times before it stops fully and trigger email alert."

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

#### Sample API Request

```json
{
  "AgencyCode": "URA",
  "BatchDate": "20260119",
  "TotalRecords": 2,
  "Violations": [
    {
      "ViolationNo": "VON2026001",
      "OffenceDateTime": "20260115103000",
      "OffenceCode": "PKG001",
      "Location": "Orchard Road Car Park A",
      "OffenceDescription": "Illegal parking in no-parking zone",
      "VehicleNo": "SJK1234A",
      "VehicleType": "C",
      "VehicleMake": "Toyota",
      "VehicleColor": "White",
      "CompositionAmount": 70.00,
      "AdminFee": 10.00,
      "TotalAmount": 80.00,
      "ViolationStatus": "O"
    },
    {
      "ViolationNo": "VON2026002",
      "OffenceDateTime": "20260110140000",
      "OffenceCode": "PKG002",
      "Location": "Marina Bay Car Park B",
      "OffenceDescription": "Overstay in parking lot",
      "VehicleNo": "WMK5678B",
      "VehicleType": "C",
      "VehicleMake": "Honda",
      "VehicleColor": "Black",
      "CompositionAmount": 50.00,
      "AdminFee": 0.00,
      "TotalAmount": 50.00,
      "ViolationStatus": "S"
    }
  ]
}
```

#### Sample API Response - Success

```json
{
  "ResponseCode": "0",
  "ResponseMessage": "Success",
  "BatchDate": "20260119",
  "ProcessedRecords": 2,
  "Results": [
    {
      "ViolationNo": "VON2026001",
      "StatusCode": "0",
      "StatusMessage": "Record accepted"
    },
    {
      "ViolationNo": "VON2026002",
      "StatusCode": "0",
      "StatusMessage": "Record accepted"
    }
  ]
}
```

#### Sample API Response - Partial Error

```json
{
  "ResponseCode": "1",
  "ResponseMessage": "Partial Success",
  "BatchDate": "20260119",
  "ProcessedRecords": 2,
  "SuccessCount": 1,
  "ErrorCount": 1,
  "Results": [
    {
      "ViolationNo": "VON2026001",
      "StatusCode": "0",
      "StatusMessage": "Record accepted"
    },
    {
      "ViolationNo": "VON2026002",
      "StatusCode": "1",
      "ErrorCode": "E_REC_002",
      "ErrorMessage": "Invalid Format - OffenceDateTime format incorrect"
    }
  ]
}
```

#### OCMS Internal Response Format

After processing vHub API response, OCMS returns internal status:

```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "vHub API batch processed successfully"
  }
}
```

Error response:

```json
{
  "data": {
    "appCode": "OCMS-5001",
    "message": "vHub API connection failed after 3 retries"
  }
}
```

---

### 2.3.6 Data Mapping

#### ocms_valid_offence_notice (Source) - Detailed Field Specification

> **Note:** Field names aligned with Data Dictionary (Generated 2026-01-05)

| Field Name | Data Type | Nullable | Default | Description | Status |
| --- | --- | --- | --- | --- | --- |
| notice_no | varchar(10) | No | - | Primary key, notice number | DD |
| vehicle_no | varchar(14) | No | - | Vehicle registration number | DD |
| notice_date_and_time | datetime2(7) | No | - | Date and time of notice | DD |
| computer_rule_code | integer | No | - | Rule code for computation | DD |
| pp_code | varchar(5) | No | - | Car park code | DD |
| pp_name | varchar(100) | Yes | NULL | Car park name | DD |
| vehicle_category | varchar(1) | No | - | Vehicle category: C, M, H, Y, B | DD |
| composition_amount | decimal(19,2) | No | - | Composition amount | DD |
| administration_fee | decimal(19,2) | Yes | NULL | Administrative fee | DD |
| suspension_type | varchar(2) | Yes | NULL | TS or PS | DD |
| epr_reason_of_suspension | varchar(3) | Yes | NULL | EPR suspension reason (e.g., FOR) | DD |
| epr_date_of_suspension | datetime2(7) | Yes | NULL | EPR suspension date | DD |
| amount_paid | decimal(19,2) | Yes | NULL | Amount paid by offender | DD |
| amount_payable | decimal(19,2) | Yes | NULL | Total payable amount | DD |
| payment_acceptance_allowed | varchar(1) | No | 'Y' | Payment allowed (Y/N) | DD |
| vehicle_registration_type | varchar(1) | Yes | NULL | L = Local, F = Foreign | DD |
| upd_date | datetime2(7) | No | - | Last update timestamp | DD |
| upd_user_id | varchar(50) | No | - | Last update user | DD |
| **vhub_sent_flag** | varchar(1) | No | 'N' | Y = Sent, N = Not Sent | **[NEW FIELD]** |
| **vhub_sent_date** | datetime2(7) | Yes | NULL | Timestamp of first successful vHub send | **[NEW FIELD]** |

**Legend:** DD = Exists in Data Dictionary, [NEW FIELD] = Proposed new field for OCMS 14

#### Sync Flag Specification [NEW FIELDS]

> **[NEW FIELD PROPOSAL]** The fields below are **proposed new fields** for OCMS 14.
> Schema change request required to add these fields to `ocms_valid_offence_notice`.

| Field | Type | Description | Values |
| --- | --- | --- | --- |
| vhub_sent_flag | varchar(1) | Indicates if notice has been sent to vHub | Y = Sent, N = Not Sent |
| vhub_sent_date | datetime2(7) | Date/time when notice was first sent to vHub | Timestamp of first successful send |

**Sync Flag Usage:**
- Set to 'Y' after first successful vHub API call
- Used to determine if notice should be sent as update (Settled/Cancelled) vs new (Outstanding)
- Query Settled/Cancelled notices only where `vhub_sent_flag = 'Y'`

#### ocms_offence_avss (Target) - Detailed Field Specification

> **Note:** Field names and types aligned with Data Dictionary (Generated 2026-01-05)

| Field Name | Data Type | Nullable | Default | Description | Status |
| --- | --- | --- | --- | --- | --- |
| batch_date | datetime2(7) | No (PK) | - | Date batch was generated | DD |
| offence_no | varchar(10) | No | - | Notice number | DD |
| offence_date | datetime2(7) | No | - | Date of offence | DD |
| offence_time | datetime2(7) | No | - | Time of offence | DD |
| offence_code | **integer** | No | - | Offence rule code | DD |
| location | varchar(100) | Yes | NULL | Offence location | DD |
| offence_description | varchar(210) | Yes | NULL | Description of offence | DD |
| vehicle_no | varchar(14) | No | - | Vehicle registration number | DD |
| vehicle_type | varchar(1) | Yes | NULL | Vehicle type: B, M, C | DD |
| vehicle_make | varchar(50) | Yes | NULL | Vehicle make | DD |
| vehicle_color | varchar(15) | Yes | NULL | Vehicle color | DD |
| amount_payable | decimal(19,2) | Yes | NULL | Amount payable for offence | DD |
| record_status | varchar(1) | Yes | NULL | Record status | DD |
| receipt_series | varchar(2) | Yes | NULL | Receipt series (settled) | DD |
| receipt_no | varchar(16) | Yes | NULL | Receipt number (settled) | DD |
| receipt_check_digit | varchar(1) | Yes | NULL | Receipt check digit | DD |
| crs_date_of_suspension | datetime2(7) | Yes | NULL | CRS suspension date | DD |
| sent_to_vhub | varchar(1) | Yes | NULL | Sent to vHub indicator | DD |
| ack_from_vhub | varchar(1) | Yes | NULL | ACK received from vHub | DD |
| sent_vhub_datetime | datetime2(7) | No | - | Timestamp sent to vHub | DD |
| ack_from_vhub_datetime | datetime2(7) | Yes | NULL | Timestamp ACK received | DD |
| vhub_api_status_code | varchar(1) | Yes | NULL | 0=Success, 1=Error | DD |
| vhub_api_error_code | varchar(10) | Yes | NULL | Error code from vHub | DD |
| vhub_api_error_description | varchar(255) | Yes | NULL | Error description | DD |
| cre_date | datetime2(7) | No | - | Record creation date | DD |
| cre_user_id | varchar(10) | No | - | Created by | DD |
| upd_date | datetime2(7) | Yes | NULL | Record update date | DD |
| upd_user_id | varchar(50) | Yes | NULL | Updated by | DD |

**Legend:** DD = Exists in Data Dictionary

#### ocms_parameter (Reference) - Detailed Field Specification

> **Note:** Field names aligned with Data Dictionary (Generated 2026-01-05)

| Field Name | Data Type | Nullable | Default | Description | Status |
| --- | --- | --- | --- | --- | --- |
| parameter_id | varchar(20) | No (PK) | - | Unique identifier (e.g., FOR, FOD, AFO) | DD |
| code | varchar(20) | No (PK) | - | Code for the parameter (e.g., NPA) | DD |
| value | varchar(200) | No | - | Main value of the parameter | DD |
| description | varchar(200) | No | - | Brief description of the parameter | DD |
| cre_date | datetime2(7) | No | - | Date when parameter was created | DD |
| cre_user_id | varchar(50) | No | - | User ID of the creator | DD |
| upd_date | datetime2(7) | Yes | NULL | Date when parameter was last updated | DD |
| upd_user_id | varchar(50) | Yes | NULL | User ID who last updated | DD |

**Legend:** DD = Exists in Data Dictionary

**Query Example:**
```sql
SELECT value
FROM ocms_parameter
WHERE parameter_id = 'FOR' AND code = 'NPA'
```

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
| API call timeout | Retry up to 3 times | Log error, continue with next batch |
| API returns error code | Log error per record | Add to error report, send email |
| Partial batch failure | Process successful records | Failed records retried in next run |

#### Error Codes

| Error Code | Description | Action |
| --- | --- | --- |
| E_REC_001 | Exceed Field Limit | Log error, review field lengths |
| E_REC_002 | Invalid Format | Log error, review data format |
| E_REC_003 | Missing Required Field | Log error, review mandatory fields |

---

### 2.3.9 Processing Model & Transaction Boundaries

#### Sequential vs Parallel Processing

| Process Step | Model | Reason |
| --- | --- | --- |
| Prepare Settled List | **Parallel** | Can run simultaneously with Cancelled and Outstanding |
| Prepare Cancelled List | **Parallel** | Can run simultaneously with Settled and Outstanding |
| Prepare Outstanding List | **Parallel** | Can run simultaneously with Settled and Cancelled |
| Consolidate Lists | **Sequential** | Must wait for all 3 lists to complete |
| Call vHub API (per batch) | **Sequential** | Each batch must complete before next batch |
| Process API Response | **Sequential** | Process each record in batch sequentially |
| Save to AVSS | **Sequential** | Must match API response processing |

#### Transaction Boundaries

| Transaction Scope | Operations Included | Rollback Behavior |
| --- | --- | --- |
| **List Preparation** | No transaction (read-only queries) | N/A - read only |
| **Single Batch API Call** | API call + AVSS insert for batch | If API fails: no AVSS insert for batch |
| **Single Record Save** | INSERT into ocms_offence_avss | If insert fails: log error, continue next record |
| **VON Sync Flag Update** | UPDATE vhub_sent_flag | If update fails: retry once, log error |

#### Failure Handling - What Continues vs Stops

| Scenario | Action | Continue? |
| --- | --- | --- |
| Parameter query fails | Stop entire job | NO - Critical config missing |
| REPCCS API fails (after 3 retries) | Skip car park name enrichment | YES - Continue without car park names |
| Single batch API call fails | Log batch, move to next batch | YES - Other batches continue |
| Single record in batch fails | Log record, continue next record | YES - Other records continue |
| All batches fail | Send error email, end job | YES - Job completes with errors |
| AVSS insert fails | Log error, continue | YES - Don't stop for DB insert failure |
| VON sync flag update fails | Retry once, log error | YES - Continue processing |

#### Processing Sequence Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                    PROCESSING SEQUENCE                                │
├──────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  1. Get FOR Parameter ──► [STOP if not found]                        │
│           │                                                           │
│           ▼                                                           │
│  2. ┌─────────────┬─────────────┬─────────────┐  (PARALLEL)          │
│     │  Settled    │  Cancelled  │ Outstanding │                       │
│     │   List      │    List     │    List     │                       │
│     └──────┬──────┴──────┬──────┴──────┬──────┘                       │
│            │             │             │                              │
│            └─────────────┼─────────────┘                              │
│                          ▼                                            │
│  3. Consolidate Lists ──► [CONTINUE even if some lists empty]        │
│           │                                                           │
│           ▼                                                           │
│  4. For each Batch of 50 (SEQUENTIAL):                               │
│     ┌─────────────────────────────────────────┐                       │
│     │  Call vHub API                          │                       │
│     │      │                                  │                       │
│     │      ├─► Success ──► Save to AVSS       │                       │
│     │      │               Update VON flag    │                       │
│     │      │                                  │                       │
│     │      └─► Fail ────► Log error           │                       │
│     │                     [CONTINUE next batch]│                       │
│     └─────────────────────────────────────────┘                       │
│           │                                                           │
│           ▼                                                           │
│  5. Consolidate Errors ──► Send Error Email (if any errors)          │
│           │                                                           │
│           ▼                                                           │
│  6. END (Job always completes, may have errors)                       │
│                                                                       │
└──────────────────────────────────────────────────────────────────────┘
```

**Key Principle:** Job should always complete. Individual failures are logged and reported, but do not stop overall processing.

---

## 2.4 vHub SFTP Interface

### CRON Job Specification

| Attribute | Value |
| --- | --- |
| CRON Name | vHub Violation Case Create/Update SFTP |
| Trigger | Scheduled |
| CRON Expression | `0 0 3 * * ?` (Daily at 03:00 AM) |
| Frequency | Daily |
| Purpose | Send FOR notices to vHub via SFTP (backup/reconciliation) |
| Shedlock Name | vhub_sftp_create_update |
| Lock Duration | 1 hour (max) |

> **Note:** CRON timing to be confirmed with Operations team. Suggested 03:00 AM (after API job completes).

### Flow Description

![vHub SFTP Flow](./images/section2-vhub-sftp.png)

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | CRON job triggers at scheduled time | Entry point of workflow |
| Prepare lists | Same logic as API flow | Settled, Cancelled, Outstanding |
| Generate XML file | Create XML with violation records | File format: XML |
| Upload to Azure | Store file in Azure Blob Storage | Backup storage |
| Azure success? | Decision: Check upload result | Verify Azure upload |
| Retry 3x | Retry Azure upload | Handle transient failures |
| Upload to SFTP | Transfer file to vHub SFTP server | File transfer |
| SFTP success? | Decision: Check upload result | Verify SFTP upload |
| Retry 3x, send error email | Retry then notify operations | Handle failures |
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
| Azure upload failed | Retry up to 3 times | Send interfacing error email |
| SFTP connection failed | Retry up to 3 times | Send interfacing error email |
| SFTP upload failed | Retry up to 3 times | Send interfacing error email |

---

## 2.5 REPCCS Listed Vehicles Interface

### CRON Job Specification

| Attribute | Value |
| --- | --- |
| CRON Name | Gen REP Listed Vehicle |
| Trigger | Scheduled |
| CRON Expression | `0 0 4 * * ?` (Daily at 04:00 AM) |
| Frequency | Daily |
| Purpose | Send listed vehicles to REPCCS for car park enforcement |
| Shedlock Name | gen_rep_listed_vehicle |
| Lock Duration | 30 minutes (max) |

> **Note:** CRON timing to be confirmed with Operations team. Suggested 04:00 AM.

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
| Trigger | Scheduled |
| CRON Expression | `0 30 4 * * ?` (Daily at 04:30 AM) |
| Frequency | Daily |
| Purpose | Send tagged vehicles to CES EHT for Certis enforcement |
| Shedlock Name | gen_ces_tagged_vehicle |
| Lock Duration | 30 minutes (max) |

> **Note:** CRON timing to be confirmed with Operations team. Suggested 04:30 AM.

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
| Trigger | Scheduled |
| CRON Expression | `0 0 1 * * ?` (Daily at 01:00 AM) |
| Frequency | Daily |
| Purpose | Add admin fee to unpaid FOR notices |
| Shedlock Name | admin_fee_processing |
| Lock Duration | 30 minutes (max) |

> **Note:** CRON timing to be confirmed with Operations team. Suggested 01:00 AM (before vHub API job).

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

## 2.8 Edge Cases & Boundary Conditions

### 2.8.1 Data Edge Cases

| Edge Case | Scenario | Handling |
| --- | --- | --- |
| Empty lists | No Settled/Cancelled/Outstanding notices found | Log "No records to process", end job successfully |
| Very large batch | More than 10,000 records in single run | Process in batches of 50, no upper limit |
| Duplicate notices | Same notice in multiple lists (Settled + Cancelled) | Settled takes priority, remove from Cancelled |
| NULL pp_code | Notice without car park code | Skip car park name enrichment, send with empty location |
| Invalid vehicle category | Vehicle category not in mapping (H, Y, C, M, B) | Log warning, use original category value |
| Payment on same day as send | Notice paid after Outstanding list prepared | Will be sent as Outstanding, then Settled next day |
| TS applied same day | TS applied after Outstanding list prepared | Will be sent as Outstanding, then Cancelled next day |

### 2.8.2 Date/Time Edge Cases

| Edge Case | Scenario | Handling |
| --- | --- | --- |
| Midnight boundary | Job runs at 00:00, date changes during processing | Use job start time as reference throughout |
| DST transition | Daylight saving time change during processing | Use UTC internally, convert for display only |
| Future dated notice | offence_date_and_time > current date | Exclude from processing (not yet eligible) |
| Very old notice | Notice older than retention period | Include if still meets PS-FOR criteria |
| Payment at 23:59 | Payment just before midnight | Include in Settled if within 24-hour window |

### 2.8.3 API Edge Cases

| Edge Case | Scenario | Handling |
| --- | --- | --- |
| vHub timeout | API call exceeds 30 seconds | Retry up to 3 times with exponential backoff |
| Partial success | Some records in batch succeed, some fail | Process all responses, log failed records |
| Empty response | vHub returns empty response body | Treat as error, retry batch |
| Malformed response | Response cannot be parsed | Log error, skip batch, continue next |
| Rate limiting | vHub returns 429 Too Many Requests | Wait 60 seconds, retry batch |
| vHub maintenance | vHub returns 503 Service Unavailable | Retry 3 times, then skip and alert |

### 2.8.4 Concurrent Processing Edge Cases

| Edge Case | Scenario | Handling |
| --- | --- | --- |
| Double job execution | Shedlock fails, job runs twice | Second instance blocked by Shedlock |
| Long running job | Job exceeds expected duration | Stuck job detection marks as STUCK after 2 hours |
| Notice updated during processing | Notice data changes while in memory | Use snapshot from query time, next run catches updates |
| Admin fee applied during processing | Admin fee CRON runs during vHub CRON | Both CRONs have separate schedules, no conflict |

### 2.8.5 Numeric Boundary Conditions

| Field | Min Value | Max Value | Handling |
| --- | --- | --- | --- |
| composition_amount | 0.00 | 99999.99 | Validate within range before send |
| admin_fee | 0.00 | 999.99 | AFO parameter validated on configuration |
| FOR parameter | 1 | 365 | Validated as positive integer |
| FOD parameter | 1 | 365 | Validated as positive integer |
| Batch size | 1 | 50 | Fixed at 50, cannot be changed |
| offence_no length | 1 | 10 | Truncate if exceeds (log warning) |
| vehicle_no length | 1 | 14 | Truncate if exceeds (log warning) |

### 2.8.6 Special Character Handling

| Field | Special Characters | Handling |
| --- | --- | --- |
| Location | &, <, >, ", ' | XML encode for SFTP, JSON escape for API |
| OffenceDescription | Unicode characters | UTF-8 encoding, validate before send |
| VehicleNo | Spaces, hyphens | Preserve as-is, vHub accepts |
| VehicleMake | Non-ASCII characters | Replace with ASCII equivalent or remove |

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

### Key Fields in ocms_valid_offence_notice (Sync Related)

| Field | Type | Nullable | Default | Description |
| --- | --- | --- | --- | --- |
| vhub_sent_flag | varchar(1) | No | 'N' | Y = Sent to vHub, N = Not Sent |
| vhub_sent_date | datetime2 | Yes | NULL | Timestamp of first successful vHub send |

### Key Fields in ocms_offence_avss

| Field | Type | Nullable | Default | Description |
| --- | --- | --- | --- | --- |
| batch_date | datetime2 | No | - | Date batch was generated |
| offence_no | varchar(10) | No | - | Notice number |
| offence_date | datetime2 | No | - | Date of offence |
| offence_time | datetime2 | No | - | Time of offence |
| offence_code | integer | No | - | Offence rule code |
| location | varchar(100) | Yes | NULL | Offence location |
| vehicle_no | varchar(14) | No | - | Vehicle number |
| vehicle_type | varchar(1) | No | - | Vehicle type code |
| violation_status | varchar(1) | No | - | O/S/C |
| vhub_api_status_code | varchar(1) | No | - | 0=Success, 1=Error |
| vhub_api_error_code | varchar(20) | Yes | NULL | Error code from vHub |
| vhub_api_error_description | varchar(200) | Yes | NULL | Error description |

---

*End of Document*

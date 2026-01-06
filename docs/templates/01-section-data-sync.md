# Section [X] – [Data Sync Feature Title]

<!--
Template for: Data Synchronization Between Systems/Zones
Use this template when documenting:
- Data sync between Intranet and Internet
- Database replication flows
- Real-time and batch synchronization
-->

## Use Case

- [Primary use case description - what business need does this solve?]
- [Secondary use case if applicable]

- The synchronization method works by:
  - [Method 1: e.g., Real-time API push when data is modified]
  - [Method 2: e.g., Cron job pulls data periodically]
  - [Method 3: e.g., Batch sync for reconciliation]

- This method ensures that [describe the consistency guarantee].

- This enables [describe the business benefit, e.g., payment channels always have updated data].

---

## [Flow Name 1] - Real-time Push

<!-- Insert flow diagram image here -->
![Flow Diagram](./images/flow-diagram.png)

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | Entry point of the process flow | The workflow begins here. |
| Insert into [source] table | Add a new record to the source database | Data is inserted into the source table for processing. |
| Success? ([source] insert) | Decision point to check insertion status | Verifies whether the source table insert was successful. |
| Response error | Return an error response | If the source insert fails, an error response is returned and the process stops. |
| Insert into [target] table | Add a new record to the target database | Data from source is inserted into the target table. |
| Success? ([target] insert) | Decision point to check insertion status | Verifies whether the target table insert was successful. |
| Log error | Record an error in logs | If the target insert fails, the error is logged and the process stops. |
| Patch is_sync to true | Update source table record | Marks the record in the source table as synchronized. |
| Response success | Return a success response | Indicates successful completion of the synchronization process. |
| End | Exit point of the process flow | The workflow ends here. |

### Data Mapping

#### [Table Name 1]

| Zone | Database Table | Field Name |
| --- | --- | --- |
| [Source Zone] | [table_name] | Refer to [Reference Document] |
| [Target Zone] | [table_name] | Refer to [Reference Document] |

#### [Table Name 2]

| Zone | Database Table | Field Name |
| --- | --- | --- |
| [Source Zone] | [table_name] | Refer to [Reference Document] |
| [Target Zone] | [table_name] | Refer to [Reference Document] |

### Success Outcome

- Data is successfully inserted into the [source] table.
- Data is successfully inserted into the [target] table.
- The source table record is updated with is_sync set to true.
- A success response is returned to indicate completion.
- The workflow reaches the End state without triggering any error-handling paths.

### Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| [Source] insert failure | The insert into the [source] table is not successful | Flow goes to response error and then End |
| [Target] insert failure | The insert into the [target] table is not successful | Flow goes to log error and then End |

---

## [Flow Name 2] - Cron Retry Sync

<!-- Insert flow diagram image here -->
![Cron Flow Diagram](./images/cron-flow-diagram.png)

| Step | Description | Brief Description |
| --- | --- | --- |
| Cron Start | Entry point for scheduled execution | The process is triggered by a scheduled cron job. |
| Query [source] table for records where is_sync = false | Retrieve unsynchronized records | Reads source records where is_sync flag is set to false. |
| Success? (Query) | Decision point to check if query succeeded | Verifies whether the query to fetch unsynced records was successful. |
| Log error | Record error for troubleshooting | Logs details of the failed query into application log. |
| Transform & validate for [target] schema | Prepare data to match target schema | Remove fields not in target table; validate required fields. |
| Push to [target] | Insert or send data to target database | Transfers records to target database using upsert semantics. |
| Success? (Push) | Decision point to check if push succeeded | Verifies whether data transfer was successful. |
| Log error | Record error for troubleshooting | Logs details of the failed push into application log. |
| Patch is_sync to true | Update synchronization status | Sets is_sync flag to true for successfully pushed records. |
| Response success | Return a success message | Confirms synchronization completed without errors. |
| End | Exit point of the process flow | Marks termination of the cron job workflow. |

### Data Mapping

Refer to Section [X.X.X]

### Success Outcome

- Cron job starts the workflow.
- Source is queried for records where is_sync = false.
- Records are pushed to the target.
- Source records are patched with is_sync = true.
- A success response is returned.
- The workflow ends without triggering any error path.

### Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Query failure | The query for records with is_sync = false is not successful | Flow logs the error and ends. |
| Push failure | The operation that pushes data to target is not successful | Flow logs the error and ends. |

---

## [Flow Name 3] - Update with Rollback

<!-- Insert flow diagram image here -->
![Update Flow Diagram](./images/update-flow-diagram.png)

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | Entry point of the process flow | The workflow begins here. |
| Update [source] table | Update an existing record in source database | Executes an update on the source table. |
| Update [target] table | Update an existing record in target database | Executes an update on the target table. |
| Both success? | Decision point to verify update success | Checks if both updates were successful. |
| Response success | Return a success message | Sent if both updates succeed. |
| Rollback update | Reverse the changes made during update | Reverts both updates when either fails. |
| Response error | Return an error message | Sent after rollback to indicate failure. |
| End | Exit point of the process flow | Marks termination of the process. |

### Data Mapping

#### [Table Name]

| Zone | Database Table | Field Name |
| --- | --- | --- |
| [Source Zone] | [table_name] | Refer to [Reference Document] |
| [Target Zone] | [table_name] | Refer to [Reference Document] |

### Success Outcome

- Update into [source] table completes.
- Update into [target] table completes.
- Both success? evaluates to yes.
- Response success is returned.
- Workflow reaches End without entering rollback path.

### Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Any update fails | Either update is not successful | Flow goes to rollback, then response error, then End. |

---

## [Flow Name 4] - End Day Sync Check

<!-- Insert flow diagram image here -->
![End Day Check Diagram](./images/end-day-check-diagram.png)

| Step | Description | Brief Description |
| --- | --- | --- |
| Cron summary batch job | Process initiation | Scheduled job triggers end-day sync process. |
| Retrieve [target] table records | Data extraction | Fetch records where is_sync = false and status = S. |
| Any record? | Record check | Decision point to verify if eligible records found. |
| Log no record | No data outcome | If no records exist, log result and move to status update. |
| Update/insert to [source] table | Data sync to source | Insert or update retrieved records into source table. |
| Any fail record(s)? | Validation | Check if there are any failed transactions. |
| Log error | Failure handling | If failures occurred, log the error and continue. |
| Update [target] table | Data sync confirmation | Mark records as synced: is_sync = true. |
| Update status cron in batch_job table | Status update | Update cron job status regardless of earlier outcomes. |
| Summary batch job report | Reporting | Generate summary report of batch job run. |

### Data Mapping

Refer to Section [X.X.X]

### Success Outcome

- Records retrieved – Eligible transactions are successfully fetched.
- Records synced – Successfully inserted/updated into source table.
- Target table updated – Marked with is_sync = true.
- No sync errors detected – Both updates complete without failures.
- Status updated – Cron job status updated in batch_job table.
- Summary report generated – Batch job report created.
- Process completed – End-day sync finishes successfully.

### Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| No record found | No eligible transactions retrieved | Process logs "no record" before status update. |
| Source update/insert failure | Failure during source table sync | Error is logged, then proceeds to status update. |
| Target update failure | Failure during target table update | Error is logged before continuing to status update. |

---

## Scenarios

### Scenario 1: [Scenario Name]

<!-- Insert scenario flow diagram here -->
![Scenario Diagram](./images/scenario-1-diagram.png)

| Step | Action | Description |
| --- | --- | --- |
| 1 | [Action Name] | [Description of what happens] |
| 2 | [Action Name] | [Description of what happens] |
| 3 | [Action Name] | [Description of what happens] |

**Expected Result:** [Describe the expected outcome]

### Scenario 2: [Scenario Name]

<!-- Insert scenario flow diagram here -->
![Scenario Diagram](./images/scenario-2-diagram.png)

| Step | Action | Description |
| --- | --- | --- |
| 1 | [Action Name] | [Description of what happens] |
| 2 | [Action Name] | [Description of what happens] |
| 3 | [Action Name] | [Description of what happens] |

**Expected Result:** [Describe the expected outcome]

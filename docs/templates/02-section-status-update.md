# Section [X] – [Status Update Feature Title]

<!--
Template for: Status Update Between Systems
Use this template when documenting:
- Payment status synchronization
- Transaction status updates
- Status reconciliation flows
-->

## Use Case

- When [trigger event occurs, e.g., a notice is successfully paid]:
  - [System] updates the [entity]'s status in the [Source] database
  - [System] records the transaction details (e.g., date/time, method, reference number) in the [Source] database
  - The updated status and transaction details are synchronized to the [Target] database

- The [Source] backend will send the status update to the [Target] via [method, e.g., cron job].

- During processing of the data retrieved from [Source], the [Target] backend will:
  - Verify that the [entity] details from [Source] match the corresponding record in [Target]
  - Update the [Target] record's status after successful validation
  - [Additional action, e.g., Suspend the record if fully processed]

---

## High Level Flow

<!-- Insert high-level flow diagram here -->
![High Level Flow Diagram](./images/high-level-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

<!-- Insert detailed flow diagram here -->
![Detailed Flow Diagram](./images/detailed-flow.png)

| Step | Description | Brief Description |
| --- | --- | --- |
| Cron Pull Start | Process initiation | The cron job is triggered every [X] minutes to start pulling updates. |
| Retrieve [source] table | Data retrieval | Query the source table for records where is_sync = false and status = [STATUS]. |
| Any record? | Decision point | Checks if the query retrieved any records. If none, log "no record" and end. |
| Update/insert to [target] table | Data synchronization | Records from source are inserted or updated into target table. |
| Any fail record(s)? | Decision point | Verifies if there are any failed records during update/insert. |
| Update [source] table | Status update | Updates source table by setting is_sync = true and upd_date = today. |
| Update status cron in batch_job | Process tracking | Updates the cron job status in the batch job tracking table. |
| End | Completion | The process ends after updating the cron status. |

---

## [Detailed Flow Name]

<!-- Insert detailed flow diagram here -->
![Detailed Process Flow](./images/detailed-process-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

<!-- Insert additional flow diagram if needed -->
![Additional Flow Diagram](./images/additional-flow.png)

| Step | Description | Brief Description |
| --- | --- | --- |
| Cron Start — Runs every [X] mins | Start | Starts the job on schedule. |
| Retrieve [source] table | Process | Retrieve transactions from source table. |
| SELECT FROM [table] WHERE is_sync = false AND status = [X] | Note | Query criteria for pulling unsynced records. |
| Any record? | Decision | Checks if any rows match the criteria. |
| Log no record | Process | Logs "no record" when none are found. |
| Insert into [target] table | Process | Insert records into target table. |
| Success? | Decision | Verifies whether update/insert succeeded. |
| Log error | Process | Logs an error when update/insert fails. |
| End | Terminator | Flow ends after error logging. |
| Query [main_table] using [key_field] | Process | Fetch main record using key field. |
| [field] match? | Decision | Determines if field values match. |
| [condition check] | Decision | Checks if condition is met. |
| Update [action] | Process | Updates when condition is met. |
| Update [tables] | Note | [Field updates description] |
| Insert [related_table] | Note | [Insert details] |
| [comparison] | Decision | Amount/value comparison. |
| Match? | Decision | Checks if values match. |
| Apply [action_type] | Process | Applies specific action when matched. |
| Update [tables] | Note | [Field updates for this path] |
| Any error? | Decision | Error check. |
| Log error | Process | Logs error when present. |
| Rollback update | Process | Rollback when there is an error. |
| Check record left | Process | If yes, process next record. If no, continue. |
| End | Terminator | Flow ends after final processing. |

---

### Data Mapping

#### Insert Transaction Status

| Field | Source Table | Target Table | Transformation |
| --- | --- | --- | --- |
| [field_name] | [source_table].[field] | [target_table].[field] | [transformation if any] |
| [field_name] | [source_table].[field] | [target_table].[field] | [transformation if any] |
| [field_name] | [source_table].[field] | [target_table].[field] | [transformation if any] |
| [field_name] | [source_table].[field] | [target_table].[field] | [transformation if any] |

#### Update Status to Main Table

| Field | Condition | Value |
| --- | --- | --- |
| [field_name] | [condition] | [value] |
| [field_name] | [condition] | [value] |
| [field_name] | [condition] | [value] |

**Condition Logic:**
- If [condition A]: Apply [Action A]
- If [condition B]: Apply [Action B]
- If [condition C]: Apply [Action C]

#### Insert Related Record (if applicable)

| Field | Value/Source |
| --- | --- |
| [field_name] | [value or source] |
| [field_name] | [value or source] |
| [field_name] | [value or source] |

---

### Success Outcome

- Cron job triggers successfully on schedule.
- Records retrieved from source where is_sync = false and status = [X].
- Records successfully inserted/updated into target table.
- Main table records updated with correct status.
- Related records inserted where applicable.
- Source table updated with is_sync = true.
- Batch job status updated.
- Process completes without errors.

---

### Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| No records found | Query returns no matching records | Log "no record" and end process normally. |
| Insert/update failure | Target table insert/update fails | Log error, rollback changes, continue to next record. |
| Validation mismatch | Source and target data do not match | Log validation error, skip record, continue processing. |
| Source update failure | Failed to update is_sync in source | Log error, record remains unsynced for retry. |
| Batch job update failure | Failed to update batch job status | Log error, process still considered complete. |

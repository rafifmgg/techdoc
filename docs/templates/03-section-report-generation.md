# Section [X] – [Report Generation Feature Title]

<!--
Template for: Report Generation (Email Reports)
Use this template when documenting:
- Daily/Weekly/Monthly automated reports
- Exception reports
- Summary reports sent via email
-->

## Use Case

- [System] generates a [frequency, e.g., daily/weekly/monthly] report on [report subject].

- The report lists [entities] that encountered the following [conditions/exceptions]:

| [Category Type] | Scenario |
| --- | --- |
| [Category 1] | [Description of when this occurs] |
| [Category 2] | [Description of when this occurs] |
| [Category 3] | [Description of when this occurs] |
| [Category 4] | [Description of when this occurs] |

- A scheduled cron service will generate the report [frequency], including [special days if applicable, e.g., Sundays and public holidays].

- The report will be emailed to [recipients, e.g., authorized Officers] to notify them of [purpose] so that they can [action required].

- When there are no [records/exceptions], [System] will send the report with the remark "[No records message]".

---

## Diagram Flow Image

<!-- Insert flow diagram here -->
![Report Generation Flow](./images/report-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

<!-- Insert detailed flow diagram if needed -->
![Detailed Report Flow](./images/report-flow-detailed.png)

| Step | Description | Brief Description |
| --- | --- | --- |
| Cron Start | Scheduled job initiation | Starts the automated [frequency] process. |
| Get list [entities] | Retrieve eligible records | Fetches [entities] matching criteria using the provided query. |
| Any result? | Decision point for data presence | Checks if any records match the selection criteria. |
| Generate report – [records found] | Compile report details | Creates a report listing and detailing all matching records. |
| Generate report – no [records] | Compile empty report | Creates a report stating "[No records message]" when no matching data is found. |
| Success? | Verify report generation | Confirms that the report was successfully created. |
| Email to [primary recipients] | Dispatch results | Sends the generated report to primary recipients. |
| Email to [error notification recipients] | Notify on failure | Sends an email to error notification recipients if generation fails. |
| End | Terminate process | Ends the flow after emails are sent. |

---

### Data Mapping

#### Email Summary Report

| Zone | Database Table | Field Name | Report Field Name |
| --- | --- | --- | --- |
| [Zone] | [table_name] | [field_name] | [Display Name in Report] |
| [Zone] | [table_name] | [field_name] | [Display Name in Report] |
| [Zone] | [table_name] | [field_name] | [Display Name in Report] |
| [Zone] | [table_name] | [field_name] | [Display Name in Report] |
| [Zone] | [table_name] | [field_name] | [Display Name in Report] |
| [Zone] | [table_name] | [field_name] | [Display Name in Report] |

**Query Criteria:**
```sql
SELECT [fields]
FROM [table]
WHERE [conditions]
  AND [date_field] BETWEEN [start_date] AND [end_date]
ORDER BY [order_field]
```

**Email Template:**
- Subject: [Email subject template]
- Recipients: [List of recipients]
- Attachment: [Attachment format, e.g., Excel file]
- Body: [Email body template]

---

### Success Outcome

- [Records] are found, report is generated, and email template is built successfully.
- No [records] are found, a "[no records]" report is generated, and email template is built successfully.
- In either outcome above, the email is successfully sent to [recipients] with the report in [format].

---

### Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Report generation failure | Occurs when the system cannot successfully generate the report | Sends a failure notification email to [error recipients], then terminates the process. |
| Email template failure | Occurs when the system cannot build the email template | Sends a failure notification email to [error recipients], then terminates the process. |
| Email sending failure | Occurs when the email cannot be sent | Logs error, retries [X] times, then notifies [error recipients]. |
| Database query failure | Occurs when the data retrieval query fails | Logs error, sends notification to [error recipients], terminates process. |

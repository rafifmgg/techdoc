# Section [X] – [Export/Finance Report Feature Title]

<!--
Template for: Export Reports (Excel/CSV for Finance/External)
Use this template when documenting:
- Monthly finance reports
- Data export for external stakeholders
- Detailed Excel/CSV report generation
-->

## Use Case

[System] generates a [frequency, e.g., monthly] report with details of [subject matter, e.g., notices that have been paid] in the past [period].

The report lists all [entities] that have been [action/status] via the following channels:

- [Channel 1, e.g., eService Portal]
- [Channel 2, e.g., AXS]
- [Channel 3, e.g., Staff Portal manual update for offline payment methods]

The report will also list all [additional entities if applicable, e.g., notices that require refund].

A scheduled cron service will generate the report on [schedule, e.g., the first calendar day of each month], including Sundays and public holidays.

The report will be emailed to authorized [recipients, e.g., Finance Officers] so that they can [purpose, e.g., keep track of monthly collections].

---

## Diagram Flow Image

<!-- Insert flow diagram here -->
![Export Report Flow](./images/export-report-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

<!-- Insert detailed flow diagram if needed -->
![Detailed Export Flow](./images/export-report-detailed.png)

| Step | Description | Brief Description |
| --- | --- | --- |
| Cron Start | Initiate automated process | The process begins with a scheduled cron job. |
| Get List of [Entities] | Fetch eligible records | Retrieves a list of [entities] matching criteria from the previous [period]. |
| Get [Additional Data] | Retrieve related information | Extracts additional fields by joining tables using [key field]. |
| Any Result? | Check if any records matched | Verifies if any data was returned; if no results, the flow ends. |
| Generate Report | Compile report into [format] | Creates a [format, e.g., XLS] report containing summary and detailed records. |
| Success? | Confirm report generation | Checks if the file was generated successfully. |
| Email to [Primary Recipients] | Send report to recipients | If successful, the file is emailed to primary recipients. |
| Email to [Error Recipients] (on failure) | Notify teams on failure | If generation fails, an error notification is sent. |
| End | Terminate flow | Ends the cron job after either success or failure. |

---

### Data Mapping

#### Excel Mapping

| Zone | Database Table | Field Name | Report Field Name |
| --- | --- | --- | --- |
| [Zone] | [table_name] | [field_name] | [Column Header in Excel] |
| [Zone] | [table_name] | [field_name] | [Column Header in Excel] |
| [Zone] | [table_name] | [field_name] | [Column Header in Excel] |
| [Zone] | [table_name] | [field_name] | [Column Header in Excel] |
| [Zone] | [table_name] | [field_name] | [Column Header in Excel] |
| [Zone] | [table_name] | [field_name] | [Column Header in Excel] |
| [Zone] | [table_name] | [field_name] | [Column Header in Excel] |
| [Zone] | [table_name] | [field_name] | [Column Header in Excel] |
| [Zone] | [table_name] | [field_name] | [Column Header in Excel] |
| [Zone] | [table_name] | [field_name] | [Column Header in Excel] |

**Query Criteria:**
```sql
SELECT [fields]
FROM [main_table] m
JOIN [related_table] r ON m.[key] = r.[key]
WHERE [conditions]
  AND [date_field] BETWEEN [start_date] AND [end_date]
ORDER BY [order_field]
```

**Excel File Structure:**
- Filename: [filename_template, e.g., `PAID_NOTICES_YYYYMM.xlsx`]
- Sheet 1: Summary
  - [Summary field 1]
  - [Summary field 2]
  - [Total count/amount]
- Sheet 2: Detail Records
  - [All mapped fields as columns]

**Email Template:**
- Subject: [Email subject template, e.g., `Monthly Paid Notices Report - {Month} {Year}`]
- Recipients: [List of recipients]
- CC: [CC recipients if any]
- Attachment: [Filename format]
- Body:
  ```
  Dear [Recipient],

  Please find attached the [report name] for [period].

  Summary:
  - Total Records: {count}
  - Total Amount: {amount}

  Regards,
  [System Name]
  ```

---

### Success Outcome

- Eligible [entities] are found → the [format] report (with defined tabs and fields) is generated successfully → emailed to recipients.
- No eligible records are found → the process ends without generating a report (per the "Any Result? = No" branch).

---

### Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Report generation failure | The [format] file cannot be created during the "Generate Report" step. | Send an error notification email to [error recipients]; then end the process. |
| Database query failure | The data retrieval query fails or times out. | Log error, send notification to [error recipients], terminate process. |
| Email sending failure | The email with attachment cannot be sent. | Retry [X] times, then notify [error recipients] if still failing. |
| File size exceeded | Generated file exceeds email attachment limit. | Split into multiple files or upload to shared location and send link. |

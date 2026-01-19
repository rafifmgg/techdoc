# OCMS 20 - Unclaimed Reminders: API Plan

## Document Information

| Item | Value |
|------|-------|
| Version | 1.0 |
| Date | 2026-01-07 |
| Feature | Unclaimed Reminders & HST Suspension |
| Source | Functional Document v1.1, Backend Code |

---

## 1. Internal APIs (OCMS Staff Portal to Backend)

### 1.1 Check Unclaimed Notices

Validate notice numbers and retrieve Notice & Reminder Letter details before submission.

| Property | Value |
|----------|-------|
| Endpoint | `/v1/check-unclaimed` |
| Method | POST |
| Content-Type | application/json |
| Authentication | Bearer Token |

**Request Body:**
```json
{
  "noticeNumbers": ["500500303J", "500500304K", "500500305L"]
}
```

**Success Response (200 OK):**
```json
{
  "data": [
    {
      "noticeNo": "500500303J",
      "dateOfLetter": "2025-11-22T00:00:00",
      "lastProcessingStage": "RD1",
      "idNumber": "S1234567A",
      "idType": "N",
      "ownerHirerIndicator": "O",
      "offenderName": "John Tan",
      "currentSuspension": null,
      "validationStatus": "OK"
    },
    {
      "noticeNo": "500500304K",
      "dateOfLetter": "2025-11-20T00:00:00",
      "lastProcessingStage": "RD2",
      "idNumber": "S7654321B",
      "idType": "N",
      "ownerHirerIndicator": "H",
      "offenderName": "Mary Lim",
      "currentSuspension": "TS/UNC",
      "validationStatus": "ALREADY_SUSPENDED"
    },
    {
      "noticeNo": "500500305L",
      "dateOfLetter": null,
      "lastProcessingStage": null,
      "idNumber": null,
      "idType": null,
      "ownerHirerIndicator": null,
      "offenderName": null,
      "currentSuspension": null,
      "validationStatus": "NOT_FOUND"
    }
  ]
}
```

**Error Response (400 Bad Request):**
```json
{
  "data": {
    "appCode": "OCMS-4000",
    "message": "noticeNumbers is required"
  }
}
```

---

### 1.2 Submit Unclaimed Reminders

Process unclaimed reminder submissions, apply TS-UNC suspension, and generate report.

| Property | Value |
|----------|-------|
| Endpoint | `/v1/submit-unclaimed` |
| Method | POST |
| Content-Type | application/json |
| Authentication | Bearer Token |

**Request Body:**
```json
[
  {
    "noticeNo": "500500303J",
    "dateOfLetter": "2025-11-22T00:00:00",
    "lastProcessingStage": "RD1",
    "idNumber": "S1234567A",
    "idType": "N",
    "ownerHirerIndicator": "O",
    "dateOfReturn": "2025-11-25T00:00:00",
    "reasonForUnclaim": "NSP",
    "unclaimRemarks": "Letter returned - not at address",
    "reasonOfSuspension": "UNC",
    "daysOfRevival": 10,
    "suspensionRemarks": "Unclaimed reminder processed"
  },
  {
    "noticeNo": "500500306M",
    "dateOfLetter": "2025-11-21T00:00:00",
    "lastProcessingStage": "RD1",
    "idNumber": "S9876543C",
    "idType": "N",
    "ownerHirerIndicator": "D",
    "dateOfReturn": "2025-11-25T00:00:00",
    "reasonForUnclaim": "RTS",
    "unclaimRemarks": "Return to sender",
    "reasonOfSuspension": "UNC",
    "daysOfRevival": 10,
    "suspensionRemarks": ""
  }
]
```

**Success Response (200 OK):**
```json
{
  "data": {
    "name": "unclaimed/20251125143052-Unclaimed-Report.xlsx",
    "processedCount": 2,
    "successCount": 2,
    "failedCount": 0
  }
}
```

**Partial Success Response (200 OK):**
```json
{
  "data": {
    "name": "unclaimed/20251125143052-Unclaimed-Report.xlsx",
    "processedCount": 2,
    "successCount": 1,
    "failedCount": 1,
    "failedRecords": [
      {
        "noticeNo": "500500306M",
        "errorCode": "OCMS-5002",
        "errorMessage": "Notice not found in system"
      }
    ]
  }
}
```

**Error Response (400 Bad Request):**
```json
{
  "data": {
    "appCode": "OCMS-4000",
    "message": "reasonOfSuspension must be 'UNC'"
  }
}
```

---

### 1.3 List Unclaimed Reports

Retrieve list of all unclaimed reports (both Reminder Reports and Batch Data Reports).

| Property | Value |
|----------|-------|
| Endpoint | `/v1/unclaimed/reports` |
| Method | GET |
| Content-Type | application/json |
| Authentication | Bearer Token |

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| startDate | string | No | Filter from date (ISO format: yyyy-MM-dd) |
| endDate | string | No | Filter to date (ISO format: yyyy-MM-dd) |
| reportType | string | No | Filter by type: REMINDER or BATCH_DATA |

**Request Example:**
```
GET /v1/unclaimed/reports?startDate=2025-11-01&endDate=2025-11-30&reportType=REMINDER
```

**Success Response (200 OK):**
```json
{
  "data": [
    {
      "reportId": 1,
      "reportDate": "2025-11-25T14:30:52",
      "reportType": "REMINDER",
      "generatedBy": "JOHNLEE",
      "reportUrl": "unclaimed/20251125143052-Unclaimed-Report.xlsx",
      "recordCount": 5
    },
    {
      "reportId": 2,
      "reportDate": "2025-11-26T09:15:00",
      "reportType": "BATCH_DATA",
      "generatedBy": "SYSTEM",
      "reportUrl": "unclaimed/20251126091500-Unclaimed-Batch-Data-Report.xlsx",
      "recordCount": 12
    }
  ],
  "totalReports": 2
}
```

---

### 1.4 Get Latest Batch Data Report

Retrieve the latest Unclaimed Batch Data Report (contains MHA/DataHive results).

| Property | Value |
|----------|-------|
| Endpoint | `/v1/unclaimed/reports/batch-data` |
| Method | GET |
| Content-Type | application/json |
| Authentication | Bearer Token |

**Success Response (200 OK):**
```json
{
  "data": {
    "reportId": 5,
    "reportDate": "2025-11-26T09:15:00",
    "reportUrl": "unclaimed/20251126091500-Unclaimed-Batch-Data-Report.xlsx",
    "recordCount": 15,
    "generatedBy": "SYSTEM"
  }
}
```

**No Report Available Response (200 OK):**
```json
{
  "data": {
    "reportId": null,
    "reportDate": null,
    "reportUrl": "",
    "recordCount": 0,
    "message": "No batch data report available"
  }
}
```

---

## 2. Internal APIs (Cron Service)

### 2.1 Execute Unclaimed Batch Data Report Job

Trigger the scheduled job to generate Unclaimed Batch Data Report from MHA/DataHive results.

| Property | Value |
|----------|-------|
| Endpoint | `/v1/unclaimed/report/execute` |
| Method | POST |
| Content-Type | application/json |
| Authentication | Internal Service Token |

**Request Body:** None required

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Unclaimed Batch Data Report generated successfully",
  "jobName": "UnclaimedBatchDataReportJob",
  "reportType": "Excel",
  "reportUrl": "unclaimed/20251126091500-Unclaimed-Batch-Data-Report.xlsx",
  "recordCount": 15,
  "timestamp": 1732608900000
}
```

**Error Response (500 Internal Server Error):**
```json
{
  "success": false,
  "message": "Error executing Unclaimed Batch Data report generation job: Database connection failed",
  "jobName": "UnclaimedBatchDataReportJob",
  "timestamp": 1732608900000
}
```

---

### 2.2 Generate Report for Specific Date

Manually generate Unclaimed Batch Data Report for a specific date.

| Property | Value |
|----------|-------|
| Endpoint | `/v1/unclaimed/report/generate` |
| Method | POST |
| Content-Type | application/json |
| Authentication | Internal Service Token |

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| reportDate | string | No | Report date (format: yyyy-MM-dd). Defaults to current date if not provided. |

**Request Example:**
```
POST /v1/unclaimed/report/generate?reportDate=2025-11-25
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Unclaimed Batch Data Report generated successfully for date: 2025-11-25",
  "jobName": "UnclaimedBatchDataReportJob",
  "reportType": "Excel",
  "reportDate": "2025-11-25",
  "reportUrl": "unclaimed/20251125-Unclaimed-Batch-Data-Report.xlsx",
  "recordCount": 8,
  "timestamp": 1732521600000
}
```

**Invalid Date Response (400 Bad Request):**
```json
{
  "success": false,
  "message": "Invalid date format. Please use yyyy-MM-dd format.",
  "jobName": "UnclaimedBatchDataReportJob",
  "timestamp": 1732521600000
}
```

---

## 3. External APIs (MHA/DataHive Integration)

### 3.1 MHA Enquiry (Outbound - via Scheduled Cron)

Query MHA for NRIC address information.

| Property | Value |
|----------|-------|
| Integration Type | SFTP File Drop |
| Trigger | Scheduled Cron (MHA Enquiry Cron) |
| Source Table | ocms_nro_temp / ocms_adhoc_nro_queue |
| Query Reason | UNC (Unclaimed) or HST |

**Enquiry File Format:**
```
NRIC,QUERY_REASON,NOTICE_NO
S1234567A,UNC,500500303J
S7654321B,UNC,500500304K
```

**Response Handling:**
- Results stored in `ocms_temp_unc_hst_addr` table
- Processed by MHA Results Processing Cron

---

### 3.2 DataHive Enquiry (Outbound - via Scheduled Cron)

Query DataHive for NRIC/FIN/UEN address and status information.

| Property | Value |
|----------|-------|
| Integration Type | API / SFTP |
| Trigger | Scheduled Cron (DataHive Query Cron) |
| Source Table | ocms_nro_temp / ocms_adhoc_nro_queue |
| Query Reason | UNC (Unclaimed) or HST |

**Query Types:**
- NRIC: Personal address from DataHive
- FIN: Foreign ID address
- UEN: Company registered address

**Response Handling:**
- Results stored in `ocms_temp_unc_hst_addr` table
- Processed by DataHive Results Processing Cron

---

## 4. Error Codes

### 4.1 Client Errors (4xxx)

| Error Code | HTTP Status | Message | Description |
|------------|-------------|---------|-------------|
| OCMS-4000 | 400 | noticeNumbers is required | Request missing required noticeNumbers field |
| OCMS-4001 | 400 | noticeNumbers cannot be empty | Empty array provided for noticeNumbers |
| OCMS-4002 | 400 | Request body cannot be empty | Empty request body on POST |
| OCMS-4003 | 400 | noticeNo is required for all records | One or more records missing noticeNo |
| OCMS-4004 | 400 | reasonOfSuspension must be 'UNC' | Invalid suspension reason code |
| OCMS-4005 | 400 | Invalid date format. Please use yyyy-MM-dd format | Date parameter format error |
| OCMS-4006 | 400 | daysOfRevival must be a positive number | Invalid revival period |

### 4.2 Server Errors (5xxx)

| Error Code | HTTP Status | Message | Description |
|------------|-------------|---------|-------------|
| OCMS-5000 | 500 | Error checking unclaimed notices: [details] | General error during notice validation |
| OCMS-5001 | 500 | Error generating report: [details] | Report generation failed |
| OCMS-5002 | 500 | Notice not found | Notice does not exist in database |
| OCMS-5003 | 500 | Error processing unclaimed submission: [details] | General submission processing error |
| OCMS-5004 | 500 | Failed to upload report to Blob Storage | Azure Blob upload failed |
| OCMS-5005 | 500 | Error retrieving reports: [details] | Database query error |
| OCMS-5006 | 500 | Failed to apply TS-UNC suspension | Suspension creation failed |
| OCMS-5007 | 500 | Failed to update RDP/DN tables | Database update error |

### 4.3 Validation Status Codes (for check-unclaimed response)

| Status | Description |
|--------|-------------|
| OK | Notice valid and ready for unclaimed processing |
| NOT_FOUND | Notice number not found in system |
| ALREADY_SUSPENDED | Notice already has active TS-UNC suspension |
| PERMANENT_SUSPENSION | Notice has permanent suspension (PS) |
| COURT_STAGE | Notice is in Court stage (cannot apply TS-UNC) |
| NO_OFFENDER | No offender record found for notice |

---

## 5. Data Transfer Objects (DTOs)

### 5.1 UnclaimedReminderDto

```java
public class UnclaimedReminderDto {
    private String noticeNo;              // Notice number (required)
    private LocalDateTime dateOfLetter;   // Date of reminder letter
    private String lastProcessingStage;   // Current processing stage
    private String idNumber;              // Offender ID number
    private String idType;                // ID type (N=NRIC, F=FIN, U=UEN)
    private String ownerHirerIndicator;   // O=Owner, H=Hirer, D=Driver
    private LocalDateTime dateOfReturn;   // Date letter was returned
    private String reasonForUnclaim;      // Reason code (NSP, RTS, etc.)
    private String unclaimRemarks;        // Remarks on envelope
    private String reasonOfSuspension;    // Must be "UNC"
    private Integer daysOfRevival;        // Suspension period in days
    private String suspensionRemarks;     // Optional remarks
}
```

### 5.2 UnclaimedBatchDataDto

```java
public class UnclaimedBatchDataDto {
    private String noticeNo;              // Notice number
    private String idNo;                  // Offender ID
    private String idType;                // ID type
    private String offenderName;          // Offender name
    private String previousAddress;       // Address used for reminder
    private String newAddress;            // New address from MHA/DataHive
    private String addressSource;         // MHA or DataHive
    private LocalDateTime dateRetrieved;  // Date address was retrieved
    private String invalidAddrTag;        // Invalid address indicator
    private String addressIndicator;      // L=Local, B=Blank, O=Overseas
}
```

### 5.3 UnclaimedProcessingResult

```java
public class UnclaimedProcessingResult {
    private String noticeNo;              // Notice number
    private boolean success;              // Processing success flag
    private String message;               // Success/error message
    private String errorCode;             // Error code if failed
    private String errorMessage;          // Detailed error message
}
```

---

## 6. Report Columns

### 6.1 Unclaimed Reminder Report (10 columns)

| # | Column Name | Source |
|---|-------------|--------|
| 1 | S/N | Auto-generated |
| 2 | Notice Number | noticeNo |
| 3 | Date of Reminder | dateOfLetter |
| 4 | Reminder Returned | lastProcessingStage |
| 5 | ID Number | idNumber |
| 6 | ID Type | idType |
| 7 | Owner/Hirer/Driver Indicator | ownerHirerIndicator |
| 8 | Date of Return | dateOfReturn |
| 9 | Reason of Return | reasonForUnclaim |
| 10 | Remarks on Envelope | unclaimRemarks |

### 6.2 Unclaimed Batch Data Report (15 columns)

| # | Column Name | Source |
|---|-------------|--------|
| 1 | S/N | Auto-generated |
| 2 | Notice Number | notice_no |
| 3 | Offender ID | id_no |
| 4 | ID Type | id_type |
| 5 | Block/House No | blk_hse_no |
| 6 | Street Name | street_name |
| 7 | Floor No | floor_no |
| 8 | Unit No | unit_no |
| 9 | Building Name | bldg_name |
| 10 | Postal Code | postal_code |
| 11 | Address Type | address_type |
| 12 | Invalid Address Tag | invalid_addr_tag |
| 13 | Last Change Date | last_change_address_date |
| 14 | Response Date | response_date_time |
| 15 | Query Reason | query_reason |

---

## 7. Database Tables Referenced

| Table | Operations | Description |
|-------|------------|-------------|
| ocms_valid_offence_notice | SELECT, UPDATE | Notice details and suspension status |
| ocms_offence_notice_owner_driver | SELECT | Offender details |
| ocms_suspended_notice | SELECT, INSERT | Suspension records |
| ocms_request_driver_particulars | SELECT, UPDATE | RDP records for Owner/Hirer |
| ocms_driver_notice | SELECT, UPDATE | DN records for Driver |
| ocms_nro_temp | INSERT | Queue for MHA/DataHive queries |
| ocms_temp_unc_hst_addr | SELECT, UPDATE | MHA/DataHive results storage |
| ocms_unclaimed_report | SELECT, INSERT | Report metadata storage |

---

## Appendix A: Reason for Unclaim Codes

| Code | Description |
|------|-------------|
| NSP | No Such Person |
| RTS | Return to Sender |
| UNC | Unclaimed |
| REF | Refused |
| MOV | Moved |
| OTH | Other |

## Appendix B: ID Type Codes

| Code | Description |
|------|-------------|
| N | NRIC |
| F | FIN |
| U | UEN |
| P | Passport |

## Appendix C: Owner/Hirer/Driver Indicator

| Code | Description |
|------|-------------|
| O | Owner |
| H | Hirer |
| D | Driver |

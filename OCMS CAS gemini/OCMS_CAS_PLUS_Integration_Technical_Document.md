# OCMS CAS & PLUS Integration Technical Document

**Prepared by**

[COMPANY LOGO]

[COMPANY NAME]

---

## Version History

| Version | Updated By | Date | Changes |
| :--- | :--- | :--- | :--- |
| v1.0 | [Author Name] | [DD/MM/YYYY] | Document Initiation |

---

## Table of Content

| Section | Content |
| :--- | :--- |
| 1 | OCMS-CAS Integration |
| 1.1 | Use Case |
| 1.2 | Flow: Get Payment Status (OCMS for CAS) |
| 1.3 | Flow: Update Court Notices (CAS to OCMS) |
| 1.4 | Flow: Refresh VIP Vehicle Table (CAS to OCMS) |
| 2 | OCMS-PLUS Integration |
| 2.1 | Use Case |
| 2.2 | Flow: Ad-hoc Notice Query (OCMS for PLUS) |
| 3 | New Database Tables |
| 3.1 | Table: `ocms_vip_vehicle` |
| 3.2 | Table: `ocms_cas_emcons` |
| 3.3 | Table: `ocms_court_case` |

---

# Section 1 – OCMS-CAS Integration

## 1.1 Use Case

To integrate OCMS with the Court Admin System (CAS) for exchanging notice statuses, payment details, and court-related information. This involves both OCMS providing data to CAS and CAS pushing updates back to OCMS.

## 1.2 Flow: Get Payment Status (OCMS for CAS)

![Flow Diagram](./images/API_1_Get_Payment_Status.png)

| Step | Description | Brief Description |
| :--- | :--- | :--- |
| **Start** | CAS initiates a scheduled job. | Scheduled process starts (Once a day, TBC). |
| **Prepare Request** | CAS prepares a list of unpaid court notice numbers to query. | Collects notice numbers for the API call. |
| **Call API 1** | CAS calls the OCMS Intranet API `v1/getPaymentStatus`. | Sends the list of notices to OCMS. |
| **Process Request** | OCMS processes the request, filtering for FP and EMCONS notices. | OCMS filters notices and prepares response data. |
| **Update `ocms_cas_emcons`**| If a notice is an EMCON notice, OCMS creates a new record in the `ocms_cas_emcons` table. | Logs EMCON notices to a dedicated table. |
| **Return Response** | OCMS returns the detailed status for each requested notice. | Sends the processed data back to CAS. |
| **End** | The process is complete. | Workflow terminates. |

### API Specification

| Field | Value |
| :--- | :--- |
| **API Name** | `getPaymentStatus` |
| **URL** | `/v1/getPaymentStatus` |
| **Description** | Called by CAS to get updated status for outstanding NOPO or EMCON notices. |
| **Method** | `POST` |
| **Request** | ```json
{
  "notice_nos": ["N001", "N002"]
}
``` |
| **Response** | ```json
{
  "total": 1,
  "limit": 10,
  "skip": 0,
  "data": [
    {
      "notice_no": "string",
      "last_processing_stage": "string",
      "last_processing_date": "datetime",
      "next_processing_stage": "string",
      "emcons_flag": "string",
      "suspension_type": "string",
      "crs_reason_of_suspension": "string",
      "epr_reason_of_suspension": "string",
      "payment_status": "string",
      "amount_payable": "decimal",
      "amount_paid": "decimal",
      "payment_allowance_flag": "string"
    }
  ]
}
``` |


### Data Mapping

| Zone | Database Table | Field Name |
| :--- | :--- | :--- |
| Intranet | `ocms_valid_offence_notice` | (Multiple fields, e.g., `payment_status`, `amount_payable`) |
| Intranet | `ocms_offence_notice_detail` | (Multiple fields) |
| Intranet | `ocms_cas_emcons` | `notice_no`, `emcons_flag` |

### Success Outcome

*   OCMS successfully receives the request from CAS.
*   For each notice number, OCMS returns the requested details.
*   If applicable, new records for EMCON notices are successfully created in the `ocms_cas_emcons` table.
*   CAS receives a success response with the requested data.

### Error Handling

#### Application Error Handling
| Error Scenario | Definition | Brief Description |
| :--- | :--- | :--- |
| Invalid Notice Number | A notice number provided by CAS does not exist in OCMS. | OCMS should log the invalid number and continue processing valid ones. The response for the invalid notice should indicate the error. |
| Database Error | OCMS fails to write to the `ocms_cas_emcons` table. | The error should be logged, and the specific notice should be marked as failed in the response. |

#### API Error Handling
| Error Scenario | App Error Code | User Message | Brief Description |
| :--- | :--- | :--- | :--- |
| General Server Error | OCMS-5000 | Something went wrong. Please try again later. | Server error |
| Bad Request | OCMS-4000 | Invalid request. Please check and try again. | Invalid syntax in the request |


## 1.3 Flow: Update Court Notices (CAS to OCMS)

![Flow Diagram](./images/API_2_Update_Court_Notices.png)

| Step | Description | Brief Description |
| :--- | :--- | :--- |
| **Start** | CAS initiates a scheduled job after API 1 completes. | Scheduled process starts. |
| **Prepare Request** | CAS prepares a list of court notices with updated information. | Collects data for notices requiring updates in OCMS. |
| **Call API 2** | CAS calls the OCMS Intranet API `v1/updateCourtNotices`. | Sends the updated notice data to OCMS. |
| **Process Request** | OCMS receives the request and validates the data, checking the "dayend" flag. | OCMS validates and processes the incoming updates. |
| **Update OCMS Tables** | OCMS updates its internal tables (`ocms_valid_offence_notice`, etc.) with the new data. | Persists the changes to the OCMS database. |
| **Return Response** | OCMS returns a success or failure response for the batch. | Informs CAS of the outcome of the update operation. |
| **End** | The process is complete. | Workflow terminates. |

### API Specification

| Field | Value |
| :--- | :--- |
| **API Name** | `updateCourtNotices` |
| **URL** | `/v1/updateCourtNotices` |
| **Description** | Called by CAS to update OCMS court NOPO records after API 1. |
| **Method** | `POST` |
| **Request** | ```json
[
  {
    "suspension_type": "TS",
    "notice_no": "string",
    "last_eburar_update": "string",
    "atoms_flag": "string",
    "last_processing_date": "datetime",
    "next_processing_stage": "string",
    "amount_payable": "decimal",
    "payment_allowance": "string",
    "epr_reason_of_suspension": "string"
  }
]
``` |
| **Response** | ```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Success"
  }
}
``` |

### Data Mapping

| Zone | Database Table | Field Name |
| :--- | :--- | :--- |
| Intranet | `ocms_valid_offence_notice` | `suspension_type`, `atoms_flag`, `last_processing_date`, `next_processing_stage`, `amount_payable`, `epr_reason_of_suspension` |
| Intranet | `ocms_offence_notice_detail` | (Potentially other fields based on `eburar` update) |

### Success Outcome

*   OCMS successfully receives and validates the batch update request from CAS.
*   All notice records in the batch are successfully updated in the OCMS database.
*   CAS receives a success response confirming the batch was processed.

### Error Handling

#### Application Error Handling
| Error Scenario | Definition | Brief Description |
| :--- | :--- | :--- |
| "Dayend" Flag Condition Not Met | The update is attempted at a time that does not match the "dayend" business rule. | The API should reject the request with a specific business error, indicating the timing is incorrect. |
| Partial Batch Failure | Some records in the batch are valid, but others fail validation or database update. | The API should process the valid records and return a response detailing which records succeeded and which failed. |

#### API Error Handling
| Error Scenario | App Error Code | User Message | Brief Description |
| :--- | :--- | :--- | :--- |
| General Server Error | OCMS-5000 | Something went wrong. Please try again later. | Server error |
| Validation Error | OCMS-4022 | Invalid data provided for one or more notices. | One or more fields in the request failed validation. |


## 1.4 Flow: Refresh VIP Vehicle Table (CAS to OCMS)

![Flow Diagram](./images/API_3_Refresh_VIP_Vehicle.png)

| Step | Description | Brief Description |
| :--- | :--- | :--- |
| **Start** | OCMS Intranet initiates a scheduled job. | Scheduled process starts (Once a day). |
| **Call API 3** | OCMS calls the CAS API to get the full list of VIP vehicles. | Fetches the complete VIP vehicle dataset from CAS. |
| **Receive Response** | OCMS receives the list of VIP vehicles from CAS. | Ingests the data from the CAS API response. |
| **Truncate Table** | OCMS truncates the `ocms_vip_vehicle` table. | Clears the existing VIP vehicle data for a full refresh. |
| **Populate Table** | OCMS populates the `ocms_vip_vehicle` table with the data received from CAS. | Inserts the new VIP vehicle list into the table. |
| **End** | The process is complete. | Workflow terminates. |

### API Specification

| Field | Value |
| :--- | :--- |
| **API Name** | `getVipVehicles` (Proposed) |
| **URL** | (To be provided by CAS) |
| **Description** | Called by OCMS to refresh the full `ocms_vip_vehicle` table daily. |
| **Method** | `POST` |
| **Request** | ```json
{}
``` |
| **Response** | ```json
[
  {
    "vip_vehicle": "string",
    "vehicle_no": "string",
    "description": "string",
    "status": "string"
  }
]
```
*Note: This is an API consumed from an external system (CAS). The exact response format is determined by the CAS API specification.* |

### Data Mapping

| Zone | Database Table | Field Name(s) |
| :--- | :--- | :--- |
| Intranet | `ocms_vip_vehicle` | `vip_vehicle`, `vehicle_no`, `description`, `status` |

### Success Outcome

*   OCMS successfully calls the CAS API and receives a complete list of VIP vehicles.
*   The `ocms_vip_vehicle` table in the OCMS database is successfully truncated.
*   The `ocms_vip_vehicle` table is successfully populated with the fresh data from CAS.

### Error Handling

#### Application Error Handling
| Error Scenario | Definition | Brief Description |
| :--- | :--- | :--- |
| CAS API Unreachable | The OCMS process is unable to connect to the CAS API endpoint. | The error should be logged, and an alert should be sent to the operations team. The existing `ocms_vip_vehicle` table should not be truncated. |
| Empty Response from CAS | CAS API returns an empty or invalid list. | The error should be logged, and an alert sent. The existing `ocms_vip_vehicle` table should not be truncated. |
| Database Failure | OCMS fails to truncate or insert into its local `ocms_vip_vehicle` table. | The transaction should be rolled back, and a critical error should be logged and alerted. |


---

# Section 2 – OCMS-PLUS Integration

## 2.1 Use Case

To provide an ad-hoc mechanism for the PLUS system to retrieve updated notice information, including court payment due dates.

## 2.2 Flow: Ad-hoc Notice Query (OCMS for PLUS)

![Flow Diagram](./images/API_4_Adhoc_Notice_Query.png)

| Step | Description | Brief Description |
| :--- | :--- | :--- |
| **Start** | PLUS system initiates an ad-hoc request for notice details. | On-demand request from PLUS. |
| **Call API 4** | PLUS calls the OCMS Intranet API via the IZ-APIM with a list of notice numbers. | The request is securely routed through the API gateway. |
| **Process Request** | OCMS retrieves the requested notice details from its database. | Fetches data for the specified notices. |
| **Update `ocms_court_case`**| If applicable, OCMS creates a new record in the `ocms_court_case` table with the notice number and court payment due date. | Logs court case data for tracking. |
| **Return Response** | OCMS returns the response data with the relevant notice details to PLUS. | Sends the processed data back to PLUS. |
| **End** | The process is complete. | Workflow terminates. |

### API Specification

| Field | Value |
| :--- | :--- |
| **API Name** | `getNoticeUpdate` (Proposed) |
| **URL** | (To be exposed via IZ-APIM) |
| **Description** | Called by PLUS on an ad-hoc basis to get updated details for a list of notices. |
| **Method** | `POST` |
| **Request** | ```json
{
  "notice_nos": ["N003", "N004"]
}
``` |
| **Response** | ```json
{
  "total": 1,
  "limit": 10,
  "skip": 0,
  "data": [
    {
      "notice_no": "string",
      "amount_payable": "decimal",
      "payment_allowance_flag": "string",
      "court_payment_due_date": "datetime",
      "suspension_type": "string",
      "epr_reason_of_suspension": "string"
    }
  ]
}
``` |

### Data Mapping

| Zone | Database Table | Field Name(s) |
| :--- | :--- | :--- |
| Intranet | `ocms_valid_offence_notice` | `amount_payable`, `suspension_type`, `epr_reason_of_suspension` |
| Intranet | `ocms_court_case` | `notice_no`, `court_payment_due_date` |

### Success Outcome

*   OCMS successfully receives the ad-hoc request from PLUS via the IZ-APIM.
*   The requested notice details are retrieved and returned in the response.
*   If applicable, a new record is created in the `ocms_court_case` table.

### Error Handling

#### Application Error Handling
| Error Scenario | Definition | Brief Description |
| :--- | :--- | :--- |
| PS-WWP Condition Not Met | The logic for PS-WWP-only fields is applied incorrectly. | The response should not contain the `Suspension type` or `EPR Reason of suspension` fields if the notice is not a PS-WWP case. |
| Database Error | OCMS fails to write to the `ocms_court_case` table. | The error should be logged, but the main response to PLUS should still be sent if the primary data retrieval was successful. |

#### API Error Handling
| Error Scenario | App Error Code | User Message | Brief Description |
| :--- | :--- | :--- | :--- |
| General Server Error | OCMS-5000 | Something went wrong. Please try again later. | Server error |
| Not Found | OCMS-4004 | One or more notice numbers could not be found. | A requested notice does not exist. |


---

# Section 3 – New Database Tables

## 3.1 Use Case

To store new data entities required for the CAS and PLUS integrations.

## 3.2 Table: `ocms_vip_vehicle`

This table is refreshed daily via API 3 from CAS and stores the full list of VIP vehicles.

| Column Name | Data Type (Assumed) | Nullable | Description |
| :--- | :--- | :--- | :--- |
| `vip_vehicle` | VARCHAR(255) | No | The primary identifier for the VIP vehicle record. |
| `vehicle_no` | VARCHAR(50) | No | The vehicle registration number. |
| `description` | VARCHAR(255) | Yes | A description associated with the VIP vehicle. |
| `status` | VARCHAR(50) | No | The status of the VIP vehicle entry. |
| `cre_date` | DATETIME2 | No | Record creation timestamp. |
| `cre_user_id` | VARCHAR(50) | No | ID of the user or process that created the record. |
| `upd_date` | DATETIME2 | Yes | Record last update timestamp. |
| `upd_user_id` | VARCHAR(50) | Yes | ID of the user or process that last updated the record. |

*Note: Data types are assumed based on context and should be finalized during implementation.*


## 3.3 Table: `ocms_cas_emcons`

This table is created via API 1 to host notice numbers associated with the `emcons_flag`.

| Column Name | Data Type (Assumed) | Nullable | Description |
| :--- | :--- | :--- | :--- |
| `notice_no` | VARCHAR(50) | No | The notice number from an EMCON notice. (Primary Key) |
| `emcons_flag` | VARCHAR(50) | Yes | The flag associated with the EMCON notice, likely indicating a specific status or type. |
| `cre_date` | DATETIME2 | No | Record creation timestamp. |
| `cre_user_id` | VARCHAR(50) | No | ID of the user or process that created the record. |
| `upd_date` | DATETIME2 | Yes | Record last update timestamp. |
| `upd_user_id` | VARCHAR(50) | Yes | ID of the user or process that last updated the record. |

*Note: Data types are assumed based on context and should be finalized during implementation.*


## 3.4 Table: `ocms_court_case`

This table is populated via API 4 to store court-related payment due dates for notices queried by PLUS.

| Column Name | Data Type (Assumed) | Nullable | Description |
| :--- | :--- | :--- | :--- |
| `notice_no` | VARCHAR(50) | No | The notice number. (Primary Key) |
| `court_payment_due_date` | DATETIME2 | Yes | The payment due date associated with the court case. |
| `cre_date` | DATETIME2 | No | Record creation timestamp. |
| `cre_user_id` | VARCHAR(50) | No | ID of the user or process that created the record. |
| `upd_date` | DATETIME2 | Yes | Record last update timestamp. |
| `upd_user_id` | VARCHAR(50) | Yes | ID of the user or process that last updated the record. |

*Note: Data types are assumed based on context and should be finalized during implementation.*

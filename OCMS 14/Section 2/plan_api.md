# API Plan: Foreign Vehicle Notice Processing

## Overview

| Attribute | Value |
| --- | --- |
| Feature Name | Foreign Vehicle Notice Processing |
| Version | v2.0 |
| Author | Claude |
| Created Date | 15/01/2026 |
| Last Updated | 27/01/2026 |
| Status | Revised |
| FD Reference | OCMS 14 - Section 3 |
| TD Reference | OCMS 14 - Section 2 |

---

## 1. Purpose

This document outlines the APIs, CRON jobs, and external system integrations involved in the Foreign Vehicle (FOR) Notice Processing flow. The flow handles notices for foreign-registered vehicles, including:
- Automatic suspension with PS-FOR
- Sending vehicle information to vHub, REPCCS, and CES EHT for enforcement
- Adding administration fees for unpaid notices
- Archival of old notices

---

## 2. System Architecture Overview

### 2.1 Systems Involved

| System | Type | Purpose |
| --- | --- | --- |
| OCMS Backend | Internal | Main processing system |
| vHub | External | Border enforcement (ICA) |
| REPCCS | External | Car park enforcement |
| CES EHT | External | Certis enforcement |
| SLIFT | External | File encryption/decryption service |
| Azure Blob Storage | External | File storage |
| SFTP Server | External | File transfer |

### 2.2 Integration Methods

| External System | Method | Direction |
| --- | --- | --- |
| vHub | REST API | OCMS → vHub |
| vHub | SFTP | OCMS → SLIFT → Azure → SFTP → vHub |
| vHub | SFTP (ACK) | vHub → SFTP → Azure → SLIFT → OCMS |
| vHub | SFTP (NTL) | vHub → SFTP → Azure → SLIFT → OCMS |
| REPCCS | SFTP | OCMS → SLIFT → Azure → SFTP → REPCCS |
| CES EHT | SFTP | OCMS → SLIFT → Azure → SFTP → CES EHT |
| SLIFT | Internal API | OCMS ↔ SLIFT (Encrypt/Decrypt) |

---

## 3. Internal CRON Jobs

### 3.1 vHub Update Violation API CRON

| Attribute | Value |
| --- | --- |
| CRON Name | vHub Update Violation API Interface |
| Trigger | Scheduled (TBD timing) |
| Frequency | Daily |
| Purpose | Send FOR notices to vHub via API |

#### Process Flow

| Step | Action | Description |
| --- | --- | --- |
| 1 | Start CRON | Scheduled job triggers |
| 2 | Get FOR Parameter | Query `ocms_parameter` for FOR value |
| 3 | Calculate Date | Today's date minus FOR days |
| 4 | Prepare Settled List | FOR notices paid in last 24 hours |
| 5 | Prepare Cancelled List | FOR notices TS-ed/PS-ed today or scheduled for archival |
| 6 | Prepare Outstanding List | Unpaid FOR notices past FOR period |
| 7 | Consolidate Lists | Merge all lists for sending |
| 8 | Call vHub API | Batch of 50 records per request |
| 9 | Process Response | Handle success/error per record |
| 10 | Store Results | Save to `ocms_offence_avss` |
| 11 | Send Error Email | If any errors occurred |

---

### 3.2 vHub SFTP Create/Update CRON

| Attribute | Value |
| --- | --- |
| CRON Name | vHub Violation Case Create/Update SFTP |
| Trigger | Scheduled (TBD timing) |
| Frequency | Daily |
| Purpose | Send FOR notices to vHub via SFTP (backup/reconciliation) |

#### Process Flow

| Step | Action | Description |
| --- | --- | --- |
| 1 | Start CRON | Scheduled job triggers |
| 2 | Get FOR Parameter | Query `ocms_parameter` for FOR value |
| 3 | Prepare Notice Lists | Same logic as API (Settled, Cancelled, Outstanding) |
| 4 | Generate XML File | Create XML with violation records |
| 5 | Send to SLIFT | Encrypt file using SLIFT service |
| 6 | Upload to Azure | Store encrypted file in Azure Blob Storage |
| 7 | Upload to SFTP | Transfer encrypted file to vHub SFTP server |
| 8 | Store Results | Save to `ocms_offence_avss_sftp` |
| 9 | Send Error Email | If any errors occurred |

---

### 3.3 vHub SFTP ACK Processing CRON

| Attribute | Value |
| --- | --- |
| CRON Name | vHub SFTP ACK Processing |
| Trigger | Scheduled (TBD timing) |
| Frequency | Daily |
| Purpose | Process acknowledgment files from vHub |

#### Process Flow

| Step | Action | Description |
| --- | --- | --- |
| 1 | Start CRON | Scheduled job triggers |
| 2 | Download ACK File | Get ACK file from vHub SFTP |
| 3 | Upload to Azure | Store encrypted file in Azure Blob |
| 4 | Send to SLIFT | Decrypt file using SLIFT service |
| 5 | Parse ACK File | Read success/error status per record |
| 6 | Update Records | Update `ocms_offence_avss_sftp` with ACK status |
| 7 | Send Error Email | If any errors in ACK file |

---

### 3.4 vHub NTL (No Trace Letter) CRON

| Attribute | Value |
| --- | --- |
| CRON Name | vHub NTL Processing |
| Trigger | Scheduled (TBD timing) |
| Frequency | Daily |
| Purpose | Process NTL responses from vHub |

#### Process Flow

| Step | Action | Description |
| --- | --- | --- |
| 1 | Start CRON | Scheduled job triggers |
| 2 | Download NTL File | Get NTL file from vHub SFTP |
| 3 | Upload to Azure | Store encrypted file in Azure Blob |
| 4 | Send to SLIFT | Decrypt file using SLIFT service |
| 5 | Parse NTL File | Read NTL records |
| 6 | Update Records | Update notice records based on NTL data |
| 7 | Send Error Email | If any errors occurred |

---

### 3.5 REPCCS Listed Vehicles CRON

| Attribute | Value |
| --- | --- |
| CRON Name | Gen REP Listed Vehicle |
| Trigger | Scheduled (TBD timing) |
| Frequency | Daily |
| Purpose | Send listed vehicles to REPCCS for enforcement |

#### Process Flow

| Step | Action | Description |
| --- | --- | --- |
| 1 | Start CRON | Scheduled job triggers |
| 2 | Get FOR Parameter | Query `ocms_parameter` for FOR value |
| 3 | Generate Listed Vehicle File | Query qualifying notices |
| 4 | Send to SLIFT | Encrypt file using SLIFT service |
| 5 | Upload to Azure | Store encrypted file in Azure Blob Storage |
| 6 | Upload to SFTP | Transfer encrypted file to REPCCS SFTP server |
| 7 | Send Error Email | If any errors occurred |

#### Query Conditions

| Field | Condition |
| --- | --- |
| suspension_type | = 'PS' |
| epr_reason_of_suspension | = 'FOR' |
| amount_paid | = 0 |
| notice_date_and_time | <= current_date - FOR days |

---

### 3.6 CES EHT Tagged Vehicles CRON

| Attribute | Value |
| --- | --- |
| CRON Name | Gen CES Tagged Vehicle List |
| Trigger | Scheduled (TBD timing) |
| Frequency | Daily |
| Purpose | Send tagged vehicles to CES EHT for enforcement |

#### Process Flow

| Step | Action | Description |
| --- | --- | --- |
| 1 | Start CRON | Scheduled job triggers |
| 2 | Get FOR Parameter | Query `ocms_parameter` for FOR value |
| 3 | Generate Tagged Vehicle File | Query qualifying notices |
| 4 | Send to SLIFT | Encrypt file using SLIFT service |
| 5 | Upload to Azure | Store encrypted file in Azure Blob Storage |
| 6 | Upload to SFTP | Transfer encrypted file to CES EHT SFTP server |
| 7 | Send Error Email | If any errors occurred |

---

### 3.7 Admin Fee CRON

| Attribute | Value |
| --- | --- |
| CRON Name | Admin Fee Processing |
| Trigger | Scheduled (TBD timing) |
| Frequency | Daily |
| Purpose | Add admin fee to unpaid FOR notices |

#### Process Flow

| Step | Action | Description |
| --- | --- | --- |
| 1 | Start CRON | Scheduled job triggers |
| 2 | Get FOD Parameter | Query `ocms_parameter` for FOD value (days) |
| 3 | Get AFO Parameter | Query `ocms_parameter` for AFO value (amount) |
| 4 | Query Eligible Notices | FOR notices unpaid past FOD period |
| 5 | Batch Update Notices | Add AFO amount to composition |
| 6 | Prepare for vHub | Updated notices sent to vHub as Outstanding |

---

## 4. External API Integration

### 4.1 vHub Update Violation Request API

| Attribute | Value |
| --- | --- |
| Type | REST API |
| Method | POST |
| Direction | OCMS → vHub |
| Batch Size | 50 records per request |
| Reference | vHub External Agency API Interface Requirement Specifications v1.0 |

#### Request Payload (per Data Dictionary)

| vHub Field | OCMS Field | Data Type | Source Table | Description |
| --- | --- | --- | --- | --- |
| ViolationReportNo | offence_no | varchar(10) | ocms_offence_avss | Notice number |
| OffenceDateTime | offence_date + offence_time | datetime2(7) | ocms_offence_avss | Format: YYYYMMDDHHmmss |
| OffenceReferenceNo | offence_code | integer | ocms_offence_avss | Offence rule code |
| OffenceLocation | location | varchar(100) | ocms_offence_avss | Offence location |
| OffenceDescription | offence_description | varchar(210) | ocms_offence_avss | Description of offence |
| VehicleRegistrationNumber | vehicle_no | varchar(14) | ocms_offence_avss | Vehicle registration |
| VehicleType | vehicle_type | varchar(1) | ocms_offence_avss | B=Bus, M=Motorcycle, C=Car |
| VehicleMake | vehicle_make | varchar(50) | ocms_offence_avss | Vehicle make |
| VehicleColor | vehicle_color | varchar(15) | ocms_offence_avss | Vehicle color |
| OffenceFineAmount | amount_payable | decimal(19,2) | ocms_offence_avss | Total amount payable |
| Status | record_status | varchar(1) | ocms_offence_avss | O/S/C (Outstanding/Settled/Cancelled) |
| PaymentDatetime | crs_date_of_suspension | datetime2(7) | ocms_offence_avss | Payment date (for settled notices) |
| ReceiptNo | receipt_no | varchar(16) | ocms_offence_avss | Receipt number (for settled notices) |

#### Violation Status Codes

| Code | Status | Description |
| --- | --- | --- |
| O | Outstanding | Unpaid notices |
| S | Settled | Paid notices |
| C | Cancelled | TS-ed, PS-ed, or archived notices |

#### Response Handling

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

### 4.2 REPCCS Car Park API

| Attribute | Value |
| --- | --- |
| Type | REST API |
| Method | GET |
| Direction | OCMS → REPCCS |
| Purpose | Get car park codes and names |
| API Owner | REPCCS (External System) |

> **Note on HTTP Method:** This API uses GET method as an **exception** to Yi Jie Guideline #8 (All APIs must use POST).
> - **Reason:** This is an existing REPCCS external API that OCMS consumes. OCMS does not control the API design.
> - **Risk Mitigation:** No sensitive data is passed in the request (read-only lookup).
> - **Alternative:** If REPCCS updates their API to support POST, OCMS should migrate accordingly.

#### Usage

- Called during vHub notice preparation
- Retrieves full list of car park codes
- Used to populate location/car park name in vHub payload
- Read-only operation (no data modification)

#### Retry Logic

| Attempt | Wait Time |
| --- | --- |
| 1 | Immediate |
| 2 | 1 second |
| 3 | 1 second |

> **Yi Jie Compliant:** Retry 3 times before failing (per Guideline #13).

---

## 5. SFTP File Interfaces

### 5.1 vHub SFTP - Violation Create/Update

| Attribute | Value |
| --- | --- |
| Direction | OCMS → vHub |
| Format | XML |
| Frequency | Daily |
| Reference | vHub External Agency Interface Requirement Specifications v1.3 |

#### File Naming Convention

```
URA_VHUB_VIOLATION_YYYYMMDD_HHMMSS.xml
```

---

### 5.2 vHub SFTP - Acknowledgment

| Attribute | Value |
| --- | --- |
| Direction | vHub → OCMS |
| Format | XML |
| Frequency | Daily |
| Purpose | Acknowledge receipt and processing status |

---

### 5.3 REPCCS SFTP - Listed Vehicles

| Attribute | Value |
| --- | --- |
| Direction | OCMS → REPCCS |
| Format | CSV/TXT |
| Frequency | Daily |
| Reference | REPCCS Interfaces to Other URA System(s) v2.0 |

#### Data Fields

| REPCCS Field | OCMS Field | Source Table |
| --- | --- | --- |
| VEHICLE_NO | vehicle_no | ocms_valid_offence_notice |
| OFFENCE_DATE | offence_date_and_time | ocms_valid_offence_notice |
| OFFENCE_CODE | offence_rule | ocms_valid_offence_notice |
| LOCATION | pp_code | ocms_valid_offence_notice |
| AMOUNT | composition_amount | ocms_valid_offence_notice |

---

### 5.4 CES EHT SFTP - Tagged Vehicles

| Attribute | Value |
| --- | --- |
| Direction | OCMS → CES EHT |
| Format | CSV/TXT |
| Frequency | Daily |

#### Data Fields

| CES EHT Field | OCMS Field | Source Table |
| --- | --- | --- |
| VEHICLE_NO | vehicle_no | ocms_valid_offence_notice |
| OFFENCE_DATE | offence_date_and_time | ocms_valid_offence_notice |
| OFFENCE_CODE | offence_rule | ocms_valid_offence_notice |
| AMOUNT | composition_amount | ocms_valid_offence_notice |

---

## 6. Database Tables

### 6.1 Primary Tables

| Table | Zone | Purpose |
| --- | --- | --- |
| ocms_valid_offence_notice | Intranet | Main notice table |
| ocms_offence_avss | Intranet | vHub API records |
| ocms_offence_avss_sftp | Intranet | vHub SFTP records |
| ocms_parameter | Intranet | System parameters |

### 6.2 ocms_offence_avss Table (per Data Dictionary)

| Field | Type | PK | Nullable | Description |
| --- | --- | --- | --- | --- |
| batch_date | datetime2(7) | Yes | No | Date the batch was generated |
| offence_no | varchar(10) | No | No | Notice number |
| offence_date | datetime2(7) | No | No | Date of offence |
| offence_time | datetime2(7) | No | No | Time of offence |
| offence_code | integer | No | No | Offence rule code |
| location | varchar(100) | No | Yes | Location of offence |
| offence_description | varchar(210) | No | Yes | Description of the offence |
| vehicle_no | varchar(14) | No | No | Vehicle number |
| vehicle_type | varchar(1) | No | Yes | Vehicle type |
| vehicle_make | varchar(50) | No | Yes | Vehicle make |
| vehicle_color | varchar(15) | No | Yes | Vehicle color |
| amount_payable | decimal(19,2) | No | Yes | Amount payable for the offence |
| record_status | varchar(1) | No | Yes | Record status (O/S/C) |
| receipt_series | varchar(2) | No | Yes | Receipt series (for settled notices) |
| receipt_no | varchar(16) | No | Yes | Receipt number (for settled notices) |
| receipt_check_digit | varchar(1) | No | Yes | Receipt check digit (for settled notices) |
| crs_date_of_suspension | datetime2(7) | No | Yes | CRS date of suspension |
| sent_to_vhub | varchar(1) | No | Yes | Indicator if sent to vHub |
| ack_from_vhub | varchar(1) | No | Yes | Indicator if acknowledgement received |
| sent_vhub_datetime | datetime2(7) | No | No | Timestamp when sent to vHub |
| ack_from_vhub_datetime | datetime2(7) | No | Yes | Timestamp when ACK received |
| vhub_api_status_code | varchar(1) | No | Yes | API status code (0=Success, 1=Error) |
| vhub_api_error_code | varchar(10) | No | Yes | API error code from vHub |
| vhub_api_error_description | varchar(255) | No | Yes | API error description from vHub |
| cre_date | datetime2(7) | No | No | Record creation timestamp |
| cre_user_id | varchar(10) | No | No | ID of user/system that created record |
| upd_date | datetime2(7) | No | Yes | Record last update timestamp |
| upd_user_id | varchar(50) | No | Yes | ID of user/system that last updated |

### 6.3 ocms_offence_avss_sftp Table (per Data Dictionary)

| Field | Type | PK | Nullable | Description |
| --- | --- | --- | --- | --- |
| batch_date | datetime2(7) | Yes | No | Date the batch was generated |
| offence_no | varchar(10) | No | No | Notice number |
| offence_date | datetime2(7) | No | No | Date of offence |
| offence_time | datetime2(7) | No | No | Time of offence |
| offence_code | integer | No | No | Offence rule code |
| location | varchar(100) | No | Yes | Location of offence |
| offence_description | varchar(210) | No | Yes | Description of the offence |
| vehicle_no | varchar(14) | No | No | Vehicle number |
| vehicle_type | varchar(1) | No | Yes | Vehicle type |
| vehicle_make | varchar(50) | No | Yes | Vehicle make |
| vehicle_color | varchar(15) | No | Yes | Vehicle color |
| amount_payable | decimal(19,2) | No | Yes | Total amount payable |
| record_status | varchar(1) | No | Yes | Status of the offence record (O/S/C) |
| receipt_series | varchar(2) | No | Yes | Receipt series (for settled notices) |
| receipt_no | varchar(16) | No | Yes | Receipt number (for settled notices) |
| receipt_check_digit | varchar(1) | No | Yes | Receipt check digit (for settled notices) |
| crs_date_of_suspension | datetime2(7) | No | Yes | CRS date of suspension |
| sent_to_vhub | varchar(1) | No | Yes | Indicator if sent to vHub |
| ack_from_vhub | varchar(1) | No | Yes | Indicator if ACK received from vHub |
| sent_vhub_datetime | datetime2(7) | No | No | Timestamp when sent to vHub via SFTP |
| ack_from_vhub_datetime | datetime2(7) | No | Yes | Timestamp when ACK received via SFTP |
| vhub_sftp_status_code | varchar(1) | No | Yes | Status code from vHub SFTP |
| vhub_sftp_error_code | varchar(10) | No | Yes | Error code from vHub SFTP |
| vhub_sftp_error_description | varchar(255) | No | Yes | Error description from vHub SFTP |
| cre_date | datetime2(7) | No | No | Record creation timestamp |
| cre_user_id | varchar(10) | No | No | ID of user/system that created record |
| upd_date | datetime2(7) | No | Yes | Record last update timestamp |
| upd_user_id | varchar(50) | No | Yes | ID of user/system that last updated |

### 6.4 Parameter Table Values

| Parameter ID | Code | Description |
| --- | --- | --- |
| FOR | NPA | Days before sending to enforcement |
| FOD | NPA | Days before adding admin fee |
| AFO | NPA | Admin fee amount |

---

## 7. SLIFT Integration

### 7.1 Overview

SLIFT (Secure Lift) is the encryption/decryption service used for all SFTP file transfers. All files sent to or received from external systems via SFTP must be encrypted/decrypted using SLIFT.

| Attribute | Value |
| --- | --- |
| Service Type | Internal API |
| Purpose | File encryption and decryption |
| Protocol | HTTPS |
| Integration | Azure-based |

### 7.2 SLIFT Operations

| Operation | Direction | Usage |
| --- | --- | --- |
| Encrypt | OCMS → SLIFT | Before uploading to Azure/SFTP |
| Decrypt | SLIFT → OCMS | After downloading from Azure/SFTP |

### 7.3 SLIFT Flow for Outbound Files (OCMS → External)

| Step | Action | Description |
| --- | --- | --- |
| 1 | Generate File | OCMS generates XML/CSV file |
| 2 | Call SLIFT Encrypt | Send file to SLIFT for encryption |
| 3 | Receive Encrypted File | SLIFT returns encrypted file |
| 4 | Upload to Azure Blob | Store encrypted file in Azure Blob Storage |
| 5 | Upload to SFTP | Transfer encrypted file to external SFTP server |

### 7.4 SLIFT Flow for Inbound Files (External → OCMS)

| Step | Action | Description |
| --- | --- | --- |
| 1 | Download from SFTP | Get encrypted file from external SFTP |
| 2 | Upload to Azure Blob | Store encrypted file in Azure Blob Storage |
| 3 | Call SLIFT Decrypt | Send file to SLIFT for decryption |
| 4 | Receive Decrypted File | SLIFT returns decrypted file |
| 5 | Process File | OCMS processes the decrypted file |

### 7.5 SLIFT Error Handling

| Error Scenario | Action | Recovery |
| --- | --- | --- |
| SLIFT service unavailable | Retry up to 3 times | Log error, send interfacing error email |
| Encryption failed | Log error | Send interfacing error email, abort transfer |
| Decryption failed | Log error | Send interfacing error email, mark file as failed |
| Invalid file format | Log error | Send interfacing error email |

---

## 8. Error Handling

### 8.1 API Error Handling

| Scenario | Action | Recovery |
| --- | --- | --- |
| API call timeout | Retry up to 2 times | Log error, continue with next batch |
| API returns error | Log error code | Add to error report, send email |
| Partial batch failure | Process successful records | Retry failed records in next run |

### 8.2 SFTP Error Handling

| Scenario | Action | Recovery |
| --- | --- | --- |
| SFTP connection failed | Retry up to 2 times | Send interfacing error email |
| File upload failed | Retry up to 2 times | Send interfacing error email |
| File generation failed | Log error | Send interfacing error email |

### 8.3 Database Error Handling

| Scenario | Action | Recovery |
| --- | --- | --- |
| Parameter not found | Log error | Stop processing, send email |
| Query timeout | Retry 1 time | Log error, send email |
| Update failed | Rollback transaction | Log error, retry in next run |

---

## 9. Vehicle Type Mapping

### 9.1 OCMS to vHub Vehicle Type Mapping

| OCMS Vehicle Category | vHub Vehicle Type | Description |
| --- | --- | --- |
| C | C | Car |
| M | M | Motorcycle |
| H | B | Heavy Vehicle → Bus |
| Y | M | [ASSUMPTION] Mapped to Motorcycle |
| B | B | Bus |

**Note:** vHub does not have 'H' (Heavy Vehicle) type, so OCMS maps 'H' to 'B' (Bus).

---

## 10. Dependencies

| Service/System | Type | Purpose |
| --- | --- | --- |
| OCMS Parameter Service | Internal | Get FOR, FOD, AFO values |
| REPCCS Car Park API | External | Get car park codes |
| vHub API | External | Send violation records |
| SLIFT Service | Internal | File encryption/decryption |
| Azure Blob Storage | External | File storage |
| SFTP Server | External | File transfer |
| Email Service | Internal | Send error notifications |

---

## 11. Assumptions Log

| ID | Assumption | Rationale |
| --- | --- | --- |
| A001 | vHub API batch size is 50 records | Based on FD Section 3.4.1.2 |
| A002 | SFTP file format is XML for vHub | Based on FD Section 3.4.2 |
| A003 | REPCCS/CES EHT file format is CSV/TXT | [ASSUMPTION] Common format for listed vehicles |
| A004 | All CRON jobs have retry logic | Based on flowchart sub-flows |
| A005 | Vehicle type 'Y' maps to 'M' | [ASSUMPTION] Based on similar mapping for 'H' |

---

## 12. Changelog

| Version | Date | Author | Changes |
| --- | --- | --- | --- |
| 1.0 | 15/01/2026 | Claude | Initial version based on FD Section 3 |
| 2.0 | 27/01/2026 | Claude | Added SLIFT integration to all SFTP flows, fixed field names per Data Dictionary |
| 2.1 | 27/01/2026 | Claude | Added complete ocms_offence_avss and ocms_offence_avss_sftp table definitions per Data Dictionary (28 fields each). Fixed: record_status (not violation_status), vhub_api_error_code varchar(10) (not 20), vhub_api_error_description varchar(255) (not 200). Added SLIFT Integration section (Section 7). Fixed section numbering. |

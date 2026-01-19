# API Plan: Foreign Vehicle Notice Processing

## Overview

| Attribute | Value |
| --- | --- |
| Feature Name | Foreign Vehicle Notice Processing |
| Version | v1.0 |
| Author | Claude |
| Created Date | 15/01/2026 |
| Last Updated | 15/01/2026 |
| Status | Draft |
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
| Azure Blob Storage | External | File storage |
| SFTP Server | External | File transfer |

### 2.2 Integration Methods

| External System | Method | Direction |
| --- | --- | --- |
| vHub | REST API | OCMS → vHub |
| vHub | SFTP | OCMS → vHub |
| vHub | SFTP (ACK) | vHub → OCMS |
| REPCCS | SFTP | OCMS → REPCCS |
| CES EHT | SFTP | OCMS → CES EHT |

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
| 5 | Upload to Azure | Store file in Azure Blob Storage |
| 6 | Upload to SFTP | Transfer file to vHub SFTP server |
| 7 | Store Results | Save to `ocms_offence_avss_sftp` |
| 8 | Send Error Email | If any errors occurred |

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
| 3 | Parse ACK File | Read success/error status per record |
| 4 | Update Records | Update `ocms_offence_avss_sftp` with ACK status |
| 5 | Send Error Email | If any errors in ACK file |

---

### 3.4 vHub NTL (No Trace Letter) CRON

| Attribute | Value |
| --- | --- |
| CRON Name | vHub NTL Processing |
| Trigger | Scheduled (TBD timing) |
| Frequency | Daily |
| Purpose | Process NTL responses from vHub |

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
| 4 | Upload to Azure | Store file in Azure Blob Storage |
| 5 | Upload to SFTP | Transfer file to REPCCS SFTP server |
| 6 | Send Error Email | If any errors occurred |

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
| 4 | Upload to Azure | Store file in Azure Blob Storage |
| 5 | Upload to SFTP | Transfer file to CES EHT SFTP server |
| 6 | Send Error Email | If any errors occurred |

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

#### Request Payload

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

#### Usage

- Called during vHub notice preparation
- Retrieves full list of car park codes
- Used to populate location/car park name in vHub payload

#### Retry Logic

| Attempt | Wait Time |
| --- | --- |
| 1 | Immediate |
| 2 | 1 second |
| 3 | 1 second |

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

### 6.2 Key Fields in ocms_offence_avss

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

### 6.3 Parameter Table Values

| Parameter ID | Code | Description |
| --- | --- | --- |
| FOR | NPA | Days before sending to enforcement |
| FOD | NPA | Days before adding admin fee |
| AFO | NPA | Admin fee amount |

---

## 7. Error Handling

### 7.1 API Error Handling

| Scenario | Action | Recovery |
| --- | --- | --- |
| API call timeout | Retry up to 2 times | Log error, continue with next batch |
| API returns error | Log error code | Add to error report, send email |
| Partial batch failure | Process successful records | Retry failed records in next run |

### 7.2 SFTP Error Handling

| Scenario | Action | Recovery |
| --- | --- | --- |
| SFTP connection failed | Retry up to 2 times | Send interfacing error email |
| File upload failed | Retry up to 2 times | Send interfacing error email |
| File generation failed | Log error | Send interfacing error email |

### 7.3 Database Error Handling

| Scenario | Action | Recovery |
| --- | --- | --- |
| Parameter not found | Log error | Stop processing, send email |
| Query timeout | Retry 1 time | Log error, send email |
| Update failed | Rollback transaction | Log error, retry in next run |

---

## 8. Vehicle Type Mapping

### 8.1 OCMS to vHub Vehicle Type Mapping

| OCMS Vehicle Category | vHub Vehicle Type | Description |
| --- | --- | --- |
| C | C | Car |
| M | M | Motorcycle |
| H | B | Heavy Vehicle → Bus |
| Y | M | [ASSUMPTION] Mapped to Motorcycle |
| B | B | Bus |

**Note:** vHub does not have 'H' (Heavy Vehicle) type, so OCMS maps 'H' to 'B' (Bus).

---

## 9. Dependencies

| Service/System | Type | Purpose |
| --- | --- | --- |
| OCMS Parameter Service | Internal | Get FOR, FOD, AFO values |
| REPCCS Car Park API | External | Get car park codes |
| vHub API | External | Send violation records |
| Azure Blob Storage | External | File storage |
| SFTP Server | External | File transfer |
| Email Service | Internal | Send error notifications |

---

## 10. Assumptions Log

| ID | Assumption | Rationale |
| --- | --- | --- |
| A001 | vHub API batch size is 50 records | Based on FD Section 3.4.1.2 |
| A002 | SFTP file format is XML for vHub | Based on FD Section 3.4.2 |
| A003 | REPCCS/CES EHT file format is CSV/TXT | [ASSUMPTION] Common format for listed vehicles |
| A004 | All CRON jobs have retry logic | Based on flowchart sub-flows |
| A005 | Vehicle type 'Y' maps to 'M' | [ASSUMPTION] Based on similar mapping for 'H' |

---

## 11. Changelog

| Version | Date | Author | Changes |
| --- | --- | --- | --- |
| 1.0 | 15/01/2026 | Claude | Initial version based on FD Section 3 |

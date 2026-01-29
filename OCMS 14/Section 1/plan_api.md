# API Plan: Detecting Vehicle Registration Type

## Overview

| Attribute | Value |
| --- | --- |
| Feature Name | Detecting Vehicle Registration Type |
| Version | v2.2 |
| Author | Claude |
| Created Date | 15/01/2026 |
| Last Updated | 27/01/2026 |
| Status | Revised |
| FD Reference | OCMS 14 - Section 2 |
| TD Reference | OCMS 14 - Section 1 |

---

## 1. Purpose

This document outlines the API and service calls involved in the Vehicle Registration Type Check function within OCMS Notice Creation flow. The function determines the vehicle registration type (Foreign, Local, Diplomatic, Military, VIP, or UPL) based on the vehicle number and source-provided data.

---

## 2. API Overview

### 2.1 Internal Service Calls

The Vehicle Registration Type Check is an **internal function** within the Notice Creation flow. It does not expose external APIs but uses the following internal services:

| Service | Type | Purpose |
| --- | --- | --- |
| LTA Checksum Utility | Internal Library | Validate Singapore vehicle number format |
| VIP Vehicle Service | Internal Service | Query ocms_vip_vehicle database |

---

## 3. External System Integration

### 3.1 LTA Checksum Validation

| Attribute | Value |
| --- | --- |
| Type | Internal Library Call |
| Library | `validateGenerateVehicleNoSuffix.ValidateRegistrationNo` |
| Method | `ValidateRegistrationNo.validate(vehicleNo)` |
| Purpose | Validate if vehicle number is a valid Singapore vehicle |

#### Input

| Parameter | Data Type | Max Length | Required | Nullable | Description |
| --- | --- | --- | --- | --- | --- |
| vehicleNo | VARCHAR | 20 | Yes | No | Vehicle registration number to validate (uppercase) |

#### Output

| Return Type | Description |
| --- | --- |
| boolean | `true` if valid Singapore vehicle, `false` otherwise |

#### Exception Handling

| Exception | Return Code | Description | Action |
| --- | --- | --- | --- |
| VehicleNoException | 1 | Valid Singapore vehicle | Return true |
| VehicleNoException | Other | Invalid vehicle number format | Return false |
| Any Exception | - | Library error | Log error, return false, proceed to next check |

---

### 3.2 OCMS VIP Vehicle Query

| Attribute | Value |
| --- | --- |
| Type | Database Query |
| Database | OCMS Database (Intranet) |
| Table | ocms_vip_vehicle |
| Service | VipVehicleService |

#### Query Pattern

```sql
SELECT vehicle_no
FROM ocms_vip_vehicle
WHERE vehicle_no = '<vehicleNo>'
AND status = 'A'
```

**Note:** Do NOT use `SELECT *`. Only select required fields.

#### Input

| Parameter | Data Type | Max Length | Required | Nullable | Description |
| --- | --- | --- | --- | --- | --- |
| vehicleNo | VARCHAR | 20 | Yes | No | Vehicle registration number (uppercase) |

#### Output

| Return Type | Description |
| --- | --- |
| boolean | `true` if vehicle found with status 'A' (Active), `false` otherwise |

#### VIP Status Values

| Status | Data Type | Description |
| --- | --- | --- |
| A | varchar(1) | Active - Valid VIP Parking Label |
| D | varchar(1) | Defunct - Expired/Invalid VIP Parking Label |

#### Error Handling

| Error Scenario | Action |
| --- | --- |
| Connection failure | Log error, return 'S' (Local Default) |
| Query timeout | Log error, return 'S' (Local Default) |
| No record found | Return 'S' (Local Default) |

---

## 4. Data Flow

### 4.1 Input from Source Systems

The Vehicle Registration Type Check receives data from the following sources during Notice Creation:

| Source System | Field Provided | Data Type | Allowed Values | Description |
| --- | --- | --- | --- | --- |
| REPCCS | vehicle_registration_type | varchar(1) | F, null | May provide 'F' for Foreign vehicles |
| CES-EHT | vehicle_registration_type | varchar(1) | F, null | May provide 'F' for Foreign vehicles |
| EEPS | vehicle_registration_type | varchar(1) | F, null | May provide 'F' for Foreign vehicles |
| PLUS | vehicle_registration_type | varchar(1) | F, null | May provide 'F' for Foreign vehicles |
| OCMS Staff Portal | vehicle_registration_type | varchar(1) | F, null | May provide 'F' for Foreign vehicles |

### 4.2 Output to Notice Creation

| Field | Data Type | Max Length | Nullable | Description |
| --- | --- | --- | --- | --- |
| vehicle_registration_type | varchar | 1 | Yes | Single character code representing vehicle type (per Data Dictionary) |

#### Vehicle Registration Type Codes

| Code | Type | Data Type | Detection Method |
| --- | --- | --- | --- |
| F | Foreign Vehicle | varchar(1) | Source-provided = 'F' |
| S | Local Vehicle | varchar(1) | LTA Checksum validation returns true OR default when no VIP match |
| D | Diplomatic Vehicle | varchar(1) | Prefix 'S' + Suffix (CC/CD/TC/TE) |
| I | Military Vehicle | varchar(1) | Prefix/Suffix = MID or MINDEF |
| V | VIP Vehicle | varchar(1) | Found in ocms_vip_vehicle with status = 'A' |
| X | UPL Dummy Vehicle | varchar(1) | Vehicle number blank/null + Offence Type = 'U' |

---

## 5. Data Mapping

### 5.1 Input Parameters (Complete Specification) - Per Data Dictionary

| Parameter | Data Type | Max Length | Required | Nullable | Allowed Values | Source | Description |
| --- | --- | --- | --- | --- | --- | --- | --- |
| vehicleNo | varchar | 14 | Conditional | No | Alphanumeric, 'N.A', 'UNLICENSED_PARKING' | Notice creation input | Required for offenceType O and E. Per DD: varchar(14) NOT NULL |
| sourceProvidedType | varchar | 1 | No | Yes | F, S, D, I, null | Source system | If 'F', returns Foreign immediately |
| offenceType | varchar | 1 | Yes | No | O, E, U | Notice creation input | O=On-street, E=ERP, U=UPL. Per DD: offence_notice_type varchar(1) NOT NULL |

### 5.2 Database Tables Affected - Per Data Dictionary

#### Intranet Zone (intranet.json)

| Table | Field Name | Data Type | Max Length | Nullable | Description | Source |
| --- | --- | --- | --- | --- | --- | --- |
| ocms_valid_offence_notice | vehicle_registration_type | varchar | 1 | Yes | Stores the determined vehicle type | System calculated |
| ocms_valid_offence_notice | vehicle_no | varchar | 14 | No | Vehicle number used for type check | Notice input |
| ocms_valid_offence_notice | offence_notice_type | varchar | 1 | No | Type of offence (O/E/U) | Notice input |
| ocms_valid_offence_notice | upd_user_id | varchar | 50 | Yes | Audit user for update | ocmsiz_app_conn |
| ocms_valid_offence_notice | upd_date | datetime2(7) | - | Yes | Update timestamp | System generated |

#### Internet Zone (internet.json)

| Table | Field Name | Data Type | Max Length | Nullable | Description | Sync From |
| --- | --- | --- | --- | --- | --- | --- |
| eocms_valid_offence_notice | vehicle_registration_type | varchar | 1 | Yes | Synced vehicle type | Intranet VON |
| eocms_valid_offence_notice | vehicle_no | varchar | 14 | No | Synced vehicle number | Intranet VON |
| eocms_valid_offence_notice | offence_notice_type | varchar | 1 | No | Type of offence (O/E/U) | Intranet VON |
| eocms_valid_offence_notice | upd_user_id | varchar | 50 | Yes | Audit user for update | ocmsez_app_conn |
| eocms_valid_offence_notice | is_sync | varchar | 1 | No | Sync flag (default: 'N') | System managed |

#### VIP Vehicle Table (OCMS - intranet.json)

| Table | Field Name | Data Type | Max Length | Nullable | Description |
| --- | --- | --- | --- | --- | --- |
| ocms_vip_vehicle | vehicle_no | varchar | 14 | No | VIP vehicle number (PK) |
| ocms_vip_vehicle | status | varchar | 1 | No | A=Active, D=Defunct |

### 5.3 Audit User Configuration

| Zone | Field | Value | Description |
| --- | --- | --- | --- |
| Intranet | cre_user_id | ocmsiz_app_conn | Database user for creation |
| Intranet | upd_user_id | ocmsiz_app_conn | Database user for update |
| Internet | cre_user_id | ocmsez_app_conn | Database user for creation |
| Internet | upd_user_id | ocmsez_app_conn | Database user for update |

**IMPORTANT:** Do NOT use "SYSTEM" for audit user fields.

---

## 6. Error Handling

### 6.1 Error Codes

| Error Code | HTTP Status | Error Condition | Error Message |
| --- | --- | --- | --- |
| OCMS-1401 | 400 | Blank vehicle number for Type O | Vehicle number is required for On-street (O) offence type |
| OCMS-1402 | 400 | Blank vehicle number for Type E | Vehicle number is required for ERP (E) offence type |

### 6.2 Error Response Format

```json
{
  "data": {
    "appCode": "OCMS-1401",
    "message": "Vehicle number is required for On-street (O) offence type"
  }
}
```

### 6.3 Error Scenarios

| Scenario | Error Code | Handling | Result |
| --- | --- | --- | --- |
| LTA validation exception | - | Exception caught, log error | Proceed to next check |
| OCMS database unavailable | - | Exception caught, log error | Return 'S' (Local Default) |
| Vehicle number blank for Type O | OCMS-1401 | Return error response | Notice creation fails |
| Vehicle number blank for Type E | OCMS-1402 | Return error response | Notice creation fails |
| Vehicle number blank for Type U | - | Valid scenario | Return 'X' (UPL Dummy) |

---

## 7. Database Operations

### 7.1 Update Operation

| Attribute | Value |
| --- | --- |
| Operation Type | UPDATE |
| Target Table | ocms_valid_offence_notice |
| Timing | After vehicle type determined |
| Transaction | Part of Notice Creation transaction |

### 7.2 Insert/Update Order

| Step | Operation | Table | Description |
| --- | --- | --- | --- |
| 1 | INSERT | ocms_valid_offence_notice (VON) | Parent table first |
| 2 | UPDATE | ocms_valid_offence_notice (VON) | Vehicle registration type |
| 3 | INSERT | ocms_offence_notice_dtl (OND) | Child table after parent |

---

## 8. Processing Path

### 8.1 Post-Detection Routing

| Detected Type | Processing Path | Document Reference |
| --- | --- | --- |
| F | Foreign Vehicle Processing | OCMS 14 TD Section 3 |
| S | Local Vehicle Processing | OCMS 11 TD |
| D | Diplomatic Vehicle Processing | OCMS 14 TD Section 6 |
| I | Military Vehicle Processing | OCMS 14 TD Section 5 |
| V | VIP Vehicle Processing | OCMS 14 TD Section 7 |
| X | UPL Processing | OCMS 14 TD (UPL section) |

---

## 9. Dependencies

| Service/System | Type | Purpose |
| --- | --- | --- |
| LTA Checksum Library | External Library | Vehicle number validation |
| OCMS Database (Intranet) | External Database | VIP vehicle lookup |
| Notice Creation Service | Internal Service | Parent flow that calls this function |

---

## 10. Changelog

| Version | Date | Author | Changes |
| --- | --- | --- | --- |
| 1.0 | 15/01/2026 | Claude | Initial version based on FD Section 2 |
| 2.0 | 19/01/2026 | Claude | Revised: Added complete data types, error codes (OCMS-XXXX format), audit user info, database operations, processing path |
| 2.1 | 19/01/2026 | Claude | Aligned with Data Dictionary: varchar(14) for vehicle_no, varchar(1) for type fields, corrected nullable settings |
| 2.2 | 27/01/2026 | Claude | Aligned with FD: Updated default behavior - after VIP check fails, return 'S' (Local) per FD Step 8. Removed 'Foreign default for unmatched' |

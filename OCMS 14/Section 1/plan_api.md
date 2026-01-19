# API Plan: Detecting Vehicle Registration Type

## Overview

| Attribute | Value |
| --- | --- |
| Feature Name | Detecting Vehicle Registration Type |
| Version | v1.0 |
| Author | Claude |
| Created Date | 15/01/2026 |
| Last Updated | 15/01/2026 |
| Status | Draft |
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
| VIP Vehicle Service | Internal Service | Query CAS VIP_VEHICLE database |

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

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| vehicleNo | String | Yes | Vehicle registration number to validate |

#### Output

| Return Type | Description |
| --- | --- |
| boolean | `true` if valid Singapore vehicle, `false` otherwise |

#### Exception Handling

| Exception | Return Code | Description |
| --- | --- | --- |
| VehicleNoException | 1 | Valid Singapore vehicle |
| VehicleNoException | Other | Invalid vehicle number format |

---

### 3.2 CAS VIP Vehicle Query

| Attribute | Value |
| --- | --- |
| Type | Database Query |
| Database | CAS Database |
| Table | VIP_VEHICLE |
| Service | VipVehicleService |

#### Query Pattern

```sql
SELECT vehicle_no
FROM VIP_VEHICLE
WHERE vehicle_no = '<vehicleNo>'
AND status = 'A'
```

#### Input

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| vehicleNo | String | Yes | Vehicle registration number (uppercase) |

#### Output

| Return Type | Description |
| --- | --- |
| boolean | `true` if vehicle found with status 'A' (Active), `false` otherwise |

#### VIP Status Values

| Status | Description |
| --- | --- |
| A | Active - Valid VIP Parking Label |
| D | Defunct - Expired/Invalid VIP Parking Label |

---

## 4. Data Flow

### 4.1 Input from Source Systems

The Vehicle Registration Type Check receives data from the following sources during Notice Creation:

| Source System | Field Provided | Description |
| --- | --- | --- |
| REPCCS | vehicle_registration_type | May provide 'F' for Foreign vehicles |
| CES-EHT | vehicle_registration_type | May provide 'F' for Foreign vehicles |
| EEPS | vehicle_registration_type | May provide 'F' for Foreign vehicles |
| PLUS | vehicle_registration_type | May provide 'F' for Foreign vehicles |
| OCMS Staff Portal | vehicle_registration_type | May provide 'F' for Foreign vehicles |

### 4.2 Output to Notice Creation

| Field | Type | Description |
| --- | --- | --- |
| vehicle_registration_type | varchar(1) | Single character code representing vehicle type |

#### Vehicle Registration Type Codes

| Code | Type | Detection Method |
| --- | --- | --- |
| F | Foreign Vehicle | Source-provided = 'F' OR default for unmatched vehicles |
| S | Local Vehicle | LTA Checksum validation returns true |
| D | Diplomatic Vehicle | Prefix 'S' + Suffix (CC/CD/TC/TE) |
| I | Military Vehicle | Prefix/Suffix = MID or MINDEF |
| V | VIP Vehicle | Found in CAS VIP_VEHICLE with status = 'A' |
| X | UPL Dummy Vehicle | Vehicle number blank/null + Offence Type = 'U' |

---

## 5. Data Mapping

### 5.1 Database Tables Affected

| Zone | Database Table | Field Name | Description |
| --- | --- | --- | --- |
| Intranet | ocms_valid_offence_notice | vehicle_registration_type | Stores the determined vehicle type |
| Intranet | ocms_valid_offence_notice | vehicle_no | Vehicle number used for type check |
| Internet | eocms_valid_offence_notice | vehicle_registration_type | Synced vehicle type for internet zone |
| CAS | VIP_VEHICLE | vehicle_no | VIP vehicle lookup table |
| CAS | VIP_VEHICLE | status | VIP status (A=Active, D=Defunct) |

---

## 6. Error Handling

### 6.1 Error Scenarios

| Scenario | Handling | Result |
| --- | --- | --- |
| LTA validation fails | Exception caught, returns false | Proceed to next check |
| CAS database unavailable | Exception caught, returns false | Default to non-VIP |
| Vehicle number is null/blank | Check offence type | Return 'X' if Type U, else error |
| Vehicle number blank for Type O/E | Return error | Notice creation fails |

### 6.2 Error Codes

| Error Code | Message | Scenario |
| --- | --- | --- |
| N/A | Offence Type O & E do not allow blank vehicle number | Blank vehicle number for Type O or E offences |

---

## 7. Dependencies

| Service/System | Type | Purpose |
| --- | --- | --- |
| LTA Checksum Library | External Library | Vehicle number validation |
| CAS Database | External Database | VIP vehicle lookup |
| Notice Creation Service | Internal Service | Parent flow that calls this function |

---

## 8. Changelog

| Version | Date | Author | Changes |
| --- | --- | --- | --- |
| 1.0 | 15/01/2026 | Claude | Initial version based on FD Section 2 |

# OCMS 14 – Notice Processing Flow for Special Types of Vehicles

**Prepared by**

NCS Pte Ltd

---

<!--
IMPORTANT: If information already exists in the Functional Document (FD),
refer to FD instead of duplicating content.
-->

---

## Version History

| Version | Updated By | Date | Changes |
| --- | --- | --- | --- |
| v1.0 | Claude | 15/01/2026 | Document Initiation - Section 1 |
| v2.0 | Claude | 19/01/2026 | Revised based on Yi Jie feedback - Added data types, audit user, error codes, processing path |
| v2.1 | Claude | 19/01/2026 | Aligned data types with Data Dictionary - varchar(14) for vehicle_no, varchar(1) for type fields |
| v2.2 | Claude | 27/01/2026 | Aligned with FD Step 8: Changed CAS to OCMS ocms_vip_vehicle, removed Last char check and Foreign default, after VIP check fails return 'S' (Local Default) |

---

## Table of Content

| Section | Content | Pages |
| --- | --- | --- |
| 1 | Detecting Vehicle Registration Type | 1 |
| 1.1 | Use Case | 1 |
| 1.2 | High Level Flow | 2 |
| 1.3 | Vehicle Registration Type Check Flow | 3 |
| 1.3.1 | Data Mapping | 5 |
| 1.3.2 | Database Operations | 7 |
| 1.3.3 | Processing Path by Vehicle Type | 8 |
| 1.3.4 | Success Outcome | 9 |
| 1.3.5 | Error Handling | 9 |

---

# Section 1 – Detecting Vehicle Registration Type

## 1.1 Use Case

### 1.1.1 Overview (5W1H)

| Question | Description |
| --- | --- |
| **WHAT** | Vehicle Registration Type Detection - A function that determines the vehicle registration type based on vehicle number format and source-provided data |
| **WHY** | Vehicle registration type determines the processing path for notice creation. Different vehicle types (Foreign, Local, Diplomatic, Military, VIP) have different validation rules, payment terms, and processing workflows. This detection ensures notices are routed to the correct processing path. |
| **WHERE** | Internal function within Notice Creation flow. Integrates with LTA Checksum Library and OCMS ocms_vip_vehicle table. |
| **WHEN** | Triggered during Notice Creation when a new notice is received from any source system (SPPS, PLUS, AXS, REPCCS, CES-EHT, EEPS, Staff Portal) |
| **WHO** | System Actors: Notice Creation Service, LTA Checksum Library, OCMS Database. No direct user interaction - fully automated process. |
| **HOW** | Sequential checks against vehicle number: source-provided type → blank check → LTA validation → format matching → VIP lookup → Local Default |

### 1.1.2 Use Case Description

1. During the Notice Creation flow, the system detects the vehicle registration type by running multiple checks against the vehicle number and source-provided type.

2. The Vehicle Registration Type Check is triggered when:<br>a. A new notice is created from any source (SPPS, PLUS, AXS, REPCCS, CES-EHT, EEPS, Staff Portal)<br>b. The system needs to determine how to process the vehicle based on its registration type

3. The detection function returns one of the following vehicle registration types:<br>a. F = Foreign Vehicle<br>b. S = Singapore/Local Vehicle<br>c. D = Diplomatic Vehicle<br>d. I = Military Vehicle<br>e. V = VIP Vehicle<br>f. X = UPL Dummy Vehicle

4. Refer to FD Section 2.3 for detailed process flow of the Vehicle Registration Type Check.

### 1.1.3 Business Context

| Vehicle Type | Business Impact | Processing Difference |
| --- | --- | --- |
| F (Foreign) | Foreign vehicles have different payment channels and extended payment terms | Processed via Section 3 workflow |
| S (Local) | Standard Singapore vehicle processing | Processed via OCMS 11 workflow |
| D (Diplomatic) | Diplomatic immunity considerations | Processed via Section 6 workflow |
| I (Military) | Military vehicle special handling | Processed via Section 5 workflow |
| V (VIP) | VIP parking label holders have different eligibility rules | Processed via Section 7 workflow |
| X (UPL Dummy) | Unauthorized Parking Lot with no vehicle number | UPL-specific processing |

---

## 1.2 High Level Flow

<!-- Insert flow diagram here -->
![High Level Flow](./images/section1-high-level-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | Notice Creation Flow triggers Vehicle Registration Type Check | Flow entry point |
| Receive Input | System receives input parameters: vehicleNo, sourceProvidedType, offenceType | Receive input parameters |
| Check Vehicle Registration Type | Function performs sequential checks to determine vehicle type | Execute detection function |
| Return Result | Return determined vehicle_registration_type value | Return type value |
| Store Result | Store result in ocms_valid_offence_notice.vehicle_registration_type | Save to database |
| Continue Notice Flow | Continue Notice Creation Flow with determined vehicle type | Flow continues |
| End | Vehicle Registration Type Check completes | Flow exit point |

---

## 1.3 Vehicle Registration Type Check Flow

<!-- Insert flow diagram here -->
![Vehicle Registration Type Check Flow](./images/section1-vehicle-reg-type-check.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | Begin vehicle registration type detection | Flow entry point |
| Check Source Foreign | Check if sourceProvidedType = 'F' | Source-provided foreign check |
| Return F (Source) | If source indicates foreign, return 'F' immediately | Return Foreign type |
| Check Blank Vehicle | Check if vehicleNo is blank, null, or 'N.A' | Blank vehicle number check |
| Check Offence Type U | If vehicle blank, check if offenceType = 'U' (UPL) | UPL offence type check |
| Return X | If UPL offence with blank vehicle, return 'X' | Return UPL Dummy type |
| Return Error | If non-UPL offence (O/E) with blank vehicle, return error | Error - blank not allowed |
| LTA Checksum Validation | Call LTA library to validate Singapore vehicle format | LTA checksum validation |
| Return S (LTA Valid) | If LTA checksum passes, vehicle is Singapore registered | Return Singapore type |
| Check Diplomatic Format | Check if vehicle matches diplomatic format (S + CC/CD/TC/TE) | Diplomatic format check |
| Return D | If diplomatic format detected, return 'D' | Return Diplomatic type |
| Check Military Format | Check if vehicle matches military format (MID/MINDEF) | Military format check |
| Return I | If military format detected, return 'I' | Return Military type |
| Query VIP Database | Query OCMS ocms_vip_vehicle table for active VIP status | VIP database lookup |
| Return V | If found in VIP database with status 'A', return 'V' | Return VIP type |
| Return S (Local Default) | If not found in VIP database, return 'S' (per FD Step 8) | Return Local Default |
| End | Vehicle registration type determined | Flow exit point |

### External System Integration

#### LTA Checksum Validation

| Field | Value |
| --- | --- |
| Integration Type | Internal Library Call |
| Library | validateGenerateVehicleNoSuffix.ValidateRegistrationNo |
| Method | `ValidateRegistrationNo.validate(vehicleNo)` |
| Purpose | Validate if vehicle number is a valid Singapore-registered vehicle |
| Input | vehicleNo (String) |
| Output | boolean (true = valid Singapore vehicle, false = not valid) |
| Error Handling | On exception, log error and proceed to next check (Diplomatic format check) |

#### OCMS VIP Vehicle Query

| Field | Value |
| --- | --- |
| Integration Type | Database Query |
| Database | OCMS Database (Intranet) |
| Table | ocms_vip_vehicle |
| Query | `SELECT vehicle_no FROM ocms_vip_vehicle WHERE vehicle_no = ? AND status = 'A'` |
| Purpose | Check if vehicle is registered as VIP |
| Input | vehicleNo (String, uppercase) |
| Output | Record found = VIP vehicle, No record = not VIP |
| Error Handling | On connection failure, log error and return 'S' (Local Default per FD Step 8) |

---

### 1.3.1 Data Mapping

#### Input Parameters

| Parameter | Data Type | Max Length | Required | Nullable | Allowed Values | Source | Description |
| --- | --- | --- | --- | --- | --- | --- | --- |
| vehicleNo | varchar | 14 | Conditional | No | Any alphanumeric, 'N.A', 'UNLICENSED_PARKING' | Notice creation input | Vehicle registration number. Required for offenceType O and E. Per Data Dictionary: varchar(14) NOT NULL. |
| sourceProvidedType | varchar | 1 | No | Yes | F, S, D, I, null | Source system (REPCCS, CES-EHT, EEPS, PLUS, Staff Portal) | Vehicle type provided by source system. If 'F', system returns Foreign immediately. |
| offenceType | varchar | 1 | Yes | No | O, E, U | Notice creation input | Type of offence: O=On-street, E=ERP, U=UPL. Per Data Dictionary: offence_notice_type varchar(1) NOT NULL. |

#### Output Values

| Value | Data Type | Description | Processing Path |
| --- | --- | --- | --- |
| F | varchar(1) | Foreign Vehicle | Foreign vehicle processing (Section 3) |
| S | varchar(1) | Singapore/Local Vehicle | Local vehicle processing (OCMS 11) |
| D | varchar(1) | Diplomatic Vehicle | Diplomatic processing (Section 6) |
| I | varchar(1) | Military Vehicle | Military processing (Section 5) |
| V | varchar(1) | VIP Vehicle | VIP processing (Section 7) |
| X | varchar(1) | UPL Dummy Vehicle | UPL processing |

#### Database Table Mapping

**Intranet Zone:** *(Per Data Dictionary: intranet.json)*

| Table | Field Name | Data Type | Max Length | Nullable | Description | Source |
| --- | --- | --- | --- | --- | --- | --- |
| ocms_valid_offence_notice | vehicle_registration_type | varchar | 1 | Yes | Stores detected vehicle type (F/S/D/I/V/X) | System calculated |
| ocms_valid_offence_notice | vehicle_no | varchar | 14 | No | Vehicle number being checked | Notice creation input |
| ocms_valid_offence_notice | offence_notice_type | varchar | 1 | No | Type of offence (O/E/U) | Notice creation input |

**Internet Zone (Sync):** *(Per Data Dictionary: internet.json)*

| Table | Field Name | Data Type | Max Length | Nullable | Description | Sync From |
| --- | --- | --- | --- | --- | --- | --- |
| eocms_valid_offence_notice | vehicle_registration_type | varchar | 1 | Yes | Synced vehicle type for internet zone | ocms_valid_offence_notice.vehicle_registration_type |
| eocms_valid_offence_notice | vehicle_no | varchar | 14 | No | Synced vehicle number | ocms_valid_offence_notice.vehicle_no |
| eocms_valid_offence_notice | offence_notice_type | varchar | 1 | No | Type of offence (O/E/U) | ocms_valid_offence_notice.offence_notice_type |
| eocms_valid_offence_notice | is_sync | varchar | 1 | No | Sync flag (default: 'N') | System managed |

**VIP Vehicle Table (OCMS - intranet.json):**

| Table | Field Name | Data Type | Max Length | Nullable | Description |
| --- | --- | --- | --- | --- | --- |
| ocms_vip_vehicle | vehicle_no | varchar | 14 | No | VIP vehicle number (Primary Key) |
| ocms_vip_vehicle | status | varchar | 1 | No | VIP status: A=Active, D=Defunct |

---

### 1.3.2 Database Operations

#### Update Operation

The vehicle registration type is stored as part of the Notice Creation flow. This is an **UPDATE** operation on an existing record.

| Attribute | Value |
| --- | --- |
| Operation Type | UPDATE |
| Target Table | ocms_valid_offence_notice |
| Timing | After vehicle registration type is determined, before continuing Notice Creation flow |
| Transaction | Part of Notice Creation transaction |

#### Update Fields

| Field | Value | Source |
| --- | --- | --- |
| vehicle_registration_type | Determined type (F/S/D/I/V/X) | System calculated |
| upd_dt | Current timestamp | System generated |
| upd_user_id | ocmsiz_app_conn | Database user (Intranet) |

#### Audit User Configuration

| Zone | Field | Value | Description |
| --- | --- | --- | --- |
| Intranet | cre_user_id | ocmsiz_app_conn | Database user for record creation |
| Intranet | upd_user_id | ocmsiz_app_conn | Database user for record update |
| Internet | cre_user_id | ocmsez_app_conn | Database user for record creation |
| Internet | upd_user_id | ocmsez_app_conn | Database user for record update |

**Note:** Do NOT use "SYSTEM" for audit user fields. Always use the database user.

#### Insert/Update Order

| Step | Operation | Table | Description |
| --- | --- | --- | --- |
| 1 | INSERT | ocms_valid_offence_notice (VON) | Parent table - Notice record created first |
| 2 | UPDATE | ocms_valid_offence_notice (VON) | Vehicle registration type updated |
| 3 | INSERT | ocms_offence_notice_dtl (OND) | Child table - Notice details created after parent |

---

### 1.3.3 Processing Path by Vehicle Type

After the vehicle registration type is determined, the Notice Creation flow routes to the appropriate processing path.

#### Processing Path Decision Matrix

| Detected Type | Type Description | Processing Section | Document Reference |
| --- | --- | --- | --- |
| F | Foreign Vehicle | Foreign Vehicle Processing | OCMS 14 TD Section 3 |
| S | Singapore/Local Vehicle | Local Vehicle Processing | OCMS 11 TD |
| D | Diplomatic Vehicle | Diplomatic Vehicle Processing | OCMS 14 TD Section 6 |
| I | Military Vehicle | Military Vehicle Processing | OCMS 14 TD Section 5 |
| V | VIP Vehicle | VIP Vehicle Processing | OCMS 14 TD Section 7 |
| X | UPL Dummy Vehicle | UPL Processing | OCMS 14 TD (UPL section) |

#### Source System Eligibility

| Source System | Can Provide Type 'F' | Allowed Offence Types | Notes |
| --- | --- | --- | --- |
| REPCCS | Yes | O, E | Can mark foreign vehicles |
| CES-EHT | Yes | E | ERP offences only |
| EEPS | Yes | E | ERP offences only |
| PLUS | Yes | O, E | Can mark foreign vehicles |
| Staff Portal | Yes | O, E, U | Manual notice creation |
| AXS | No | O, E | System determines type |

---

### 1.3.4 Success Outcome

- Vehicle registration type is successfully determined based on input parameters
- The determined type is stored in `ocms_valid_offence_notice.vehicle_registration_type`
- Audit fields updated: `upd_dt` = current timestamp, `upd_user_id` = ocmsiz_app_conn
- Notice Creation Flow continues with the appropriate processing path based on vehicle type
- The workflow reaches the End state without triggering any error-handling paths

---

### 1.3.5 Error Handling

#### Error Codes

| Error Code | Error Condition | Error Message | HTTP Status | Action |
| --- | --- | --- | --- | --- |
| OCMS-1401 | Blank vehicle number for Type O offence | Vehicle number is required for On-street (O) offence type | 400 | Return error, notice creation fails |
| OCMS-1402 | Blank vehicle number for Type E offence | Vehicle number is required for ERP (E) offence type | 400 | Return error, notice creation fails |

#### Error Response Format

```json
{
  "data": {
    "appCode": "OCMS-1401",
    "message": "Vehicle number is required for On-street (O) offence type"
  }
}
```

#### Application Error Handling

| Error Scenario | Error Code | Definition | Action |
| --- | --- | --- | --- |
| Blank Vehicle for Type O | OCMS-1401 | vehicleNo is blank/null and offenceType = 'O' | Return error response, notice creation fails |
| Blank Vehicle for Type E | OCMS-1402 | vehicleNo is blank/null and offenceType = 'E' | Return error response, notice creation fails |
| LTA Library Exception | - | LTA Checksum library throws exception | Log error, proceed to Diplomatic format check |
| OCMS Database Error | - | Unable to connect to OCMS ocms_vip_vehicle table | Log error, return 'S' (Local Default per FD Step 8) |

#### Condition-Based Error Handling

| Condition ID | Error Condition | Error Code | Action |
| --- | --- | --- | --- |
| C003a | vehicleNo is blank AND offenceType = 'O' | OCMS-1401 | Return error - notice cannot be created |
| C003b | vehicleNo is blank AND offenceType = 'E' | OCMS-1402 | Return error - notice cannot be created |

---

## Appendix

### A. Vehicle Registration Type Detection Order

The detection follows a strict sequential order. Once a type is determined, the function returns immediately without checking subsequent conditions.

| Priority | Check | Condition | Result if True |
| --- | --- | --- | --- |
| 1 | Source-provided type | sourceProvidedType = 'F' | Return 'F' (Foreign) |
| 2 | UPL blank vehicle | vehicleNo is blank AND offenceType = 'U' | Return 'X' (UPL Dummy) |
| 3 | Non-UPL blank vehicle | vehicleNo is blank AND offenceType IN ('O', 'E') | Return Error |
| 4 | LTA Checksum | LTA validation returns true | Return 'S' (Singapore) |
| 5 | Diplomatic format | vehicleNo starts with 'S' AND ends with CC/CD/TC/TE | Return 'D' (Diplomatic) |
| 6 | Military format | vehicleNo starts/ends with MID or contains MINDEF | Return 'I' (Military) |
| 7 | VIP database | Found in ocms_vip_vehicle with status = 'A' | Return 'V' (VIP) |
| 8 | Local Default | Not found in VIP database (per FD Step 8) | Return 'S' (Local Default) |

### B. Diplomatic Vehicle Suffixes

| Suffix | Description |
| --- | --- |
| CC | Consular Corps |
| CD | Corps Diplomatique |
| TC | Technical Corps |
| TE | Technical Embassy |

### C. Military Vehicle Patterns

| Pattern | Description | Example |
| --- | --- | --- |
| MID prefix | Vehicle number starts with 'MID' | MID1234 |
| MID suffix | Vehicle number ends with 'MID' | 1234MID |
| MINDEF | Vehicle number contains 'MINDEF' | MINDEF123 |

### D. Blank Vehicle Values

The following values are considered as "blank" vehicle number:

| Value | Description |
| --- | --- |
| null | Null value |
| "" | Empty string |
| " " | Whitespace only |
| "N.A" | Not Available marker |
| "UNLICENSED_PARKING" | UPL marker |

### E. Sync to Internet Zone

| Attribute | Value |
| --- | --- |
| Sync Direction | Intranet → Internet |
| Source Table | ocms_valid_offence_notice |
| Target Table | eocms_valid_offence_notice |
| Sync Trigger | After Notice Creation completes successfully |
| Sync Fields | vehicle_registration_type, vehicle_no, and other notice fields |

### F. Reference Documents

| Document | Section | Description |
| --- | --- | --- |
| OCMS 14 FD | Section 2 | Detecting Vehicle Registration Type |
| OCMS 14 FD | Section 2.3 | Process Flow of the Vehicle Registration Type Check |
| OCMS 11 TD | - | Notice Processing Flow for Local Vehicles |
| OCMS 14 TD | Section 3 | Notice Processing Flow for Foreign Vehicles |
| OCMS 14 TD | Section 5 | Notice Processing Flow for Military Vehicles |
| OCMS 14 TD | Section 6 | Notice Processing Flow for Diplomatic Vehicles |
| OCMS 14 TD | Section 7 | Notice Processing Flow for VIP Vehicles |

---

**End of Section 1**

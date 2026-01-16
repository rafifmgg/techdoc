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

---

## Table of Content

| Section | Content | Pages |
| --- | --- | --- |
| 1 | Detecting Vehicle Registration Type | 1 |
| 1.1 | Use Case | 1 |
| 1.2 | High Level Flow | 2 |
| 1.3 | Vehicle Registration Type Check Flow | 3 |
| 1.3.1 | Data Mapping | 5 |
| 1.3.2 | Success Outcome | 6 |
| 1.3.3 | Error Handling | 6 |

---

# Section 1 – Detecting Vehicle Registration Type

## 1.1 Use Case

1. During the Notice Creation flow, the system detects the vehicle registration type by running multiple checks against the vehicle number and source-provided type.

2. The Vehicle Registration Type Check is triggered when:<br>a. A new notice is created from any source (SPPS, PLUS, AXS, etc.)<br>b. The system needs to determine how to process the vehicle based on its registration type

3. The detection function returns one of the following vehicle registration types:<br>a. F = Foreign Vehicle<br>b. S = Singapore/Local Vehicle<br>c. D = Diplomatic Vehicle<br>d. I = Military Vehicle<br>e. V = VIP Vehicle<br>f. X = UPL Dummy Vehicle

4. Refer to FD Section 2.3 for detailed process flow of the Vehicle Registration Type Check.

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
| Query VIP Database | Query CAS VIP_VEHICLE table for active VIP status | VIP database lookup |
| Return V | If found in VIP database with status 'A', return 'V' | Return VIP type |
| Check Last Character | Check if last character of vehicle number is alphabet (A-Z) | Last character check |
| Return S (Fallback) | If last character is letter, assume local vehicle | Return Singapore fallback |
| Return F (Default) | If no other condition matches, default to foreign | Return Foreign default |
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

#### CAS VIP Vehicle Query

| Field | Value |
| --- | --- |
| Integration Type | Database Query |
| Database | CAS Database |
| Table | VIP_VEHICLE |
| Query | `SELECT vehicle_no FROM VIP_VEHICLE WHERE vehicle_no = ? AND status = 'A'` |
| Purpose | Check if vehicle is registered as VIP |
| Input | vehicleNo (String) |
| Output | Record found = VIP vehicle, No record = not VIP |

---

### 1.3.1 Data Mapping

#### Database Data Mapping

| Zone | Database Table | Field Name | Description |
| --- | --- | --- | --- |
| Intranet | ocms_valid_offence_notice | vehicle_registration_type | Stores detected vehicle type (F/S/D/I/V/X) |
| Intranet | ocms_valid_offence_notice | vehicle_no | Vehicle number being checked |
| External | VIP_VEHICLE (CAS) | vehicle_no | VIP vehicle number |
| External | VIP_VEHICLE (CAS) | status | VIP status (A=Active, D=Defunct) |

#### Input Parameters

| Parameter | Type | Description | Source |
| --- | --- | --- | --- |
| vehicleNo | String | Vehicle registration number | Notice creation input |
| sourceProvidedType | String | Type provided by notice source (F/S/null) | Source system |
| offenceType | String | Type of offence (O=On-street, E=ERP, U=UPL) | Notice creation input |

#### Output Values

| Value | Description | Determines |
| --- | --- | --- |
| F | Foreign Vehicle | Foreign vehicle processing (Section 3) |
| S | Singapore/Local Vehicle | Local vehicle processing (OCMS 11) |
| D | Diplomatic Vehicle | Diplomatic processing (Section 6) |
| I | Military Vehicle | Military processing (Section 5) |
| V | VIP Vehicle | VIP processing (Section 7) |
| X | UPL Dummy Vehicle | UPL processing |

---

### 1.3.2 Success Outcome

- Vehicle registration type is successfully determined based on input parameters
- The determined type is stored in `ocms_valid_offence_notice.vehicle_registration_type`
- Notice Creation Flow continues with the appropriate processing path based on vehicle type
- The workflow reaches the End state without triggering any error-handling paths

---

### 1.3.3 Error Handling

#### Application Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Blank Vehicle for Non-UPL | vehicleNo is blank/null and offenceType is 'O' or 'E' | Return error - blank vehicle not allowed for On-street (O) or ERP (E) offence types |
| LTA Library Error | LTA Checksum library throws exception | Log error and continue with next check |
| VIP Database Connection Error | Unable to connect to CAS VIP_VEHICLE database | Log error and continue with fallback checks |

#### Condition-Based Error Handling

| Condition ID | Error Condition | Action |
| --- | --- | --- |
| C003 | vehicleNo is blank AND offenceType IN ('O', 'E') | Return error - notice cannot be created without vehicle number for these offence types |

---

## Appendix

### A. Vehicle Registration Type Detection Order

The detection follows a strict sequential order. Once a type is determined, the function returns immediately without checking subsequent conditions.

| Priority | Check | Result if True |
| --- | --- | --- |
| 1 | Source-provided type = 'F' | Return 'F' (Foreign) |
| 2 | Vehicle blank + UPL offence | Return 'X' (UPL Dummy) |
| 3 | Vehicle blank + Non-UPL | Return Error |
| 4 | LTA Checksum valid | Return 'S' (Singapore) |
| 5 | Diplomatic format | Return 'D' (Diplomatic) |
| 6 | Military format | Return 'I' (Military) |
| 7 | VIP database match | Return 'V' (VIP) |
| 8 | Last char is letter | Return 'S' (Local fallback) |
| 9 | Default | Return 'F' (Foreign default) |

### B. Diplomatic Vehicle Suffixes

| Suffix | Description |
| --- | --- |
| CC | Consular Corps |
| CD | Corps Diplomatique |
| TC | Technical Corps |
| TE | Technical Embassy |

### C. Military Vehicle Patterns

| Pattern | Description |
| --- | --- |
| MID prefix | Vehicle number starts with 'MID' |
| MID suffix | Vehicle number ends with 'MID' |
| MINDEF | Vehicle number contains 'MINDEF' |

### D. Reference Documents

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

# OCMS 14 – Detecting Vehicle Registration Type

**Prepared by**

[COMPANY LOGO]

[COMPANY NAME]

---

## Version History

| Version | Updated By | Date | Changes |
| --- | --- | --- | --- |
| v1.0 | [Author Name] | [DD/MM/YYYY] | Document Initiation |

---

## Table of Content

| Section | Content | Pages |
| --- | --- | --- |
| 1 | Detecting Vehicle Registration Type | [X] |
| 1.1 | Use Case | [X] |
| 1.2 | High Level Flow | [X] |
| 1.3 | Vehicle Registration Type Detection Flow | [X] |
| 1.3.1 | API Specification | [X] |
| 1.3.2 | Data Mapping | [X] |
| 1.3.3 | Success Outcome | [X] |
| 1.3.4 | Error Handling | [X] |

---

# Section 1 – Detecting Vehicle Registration Type

## 1.1 Use Case

1. OCMS identifies the vehicle registration type during Notice creation by processing raw Offence Data received from REPCCS, CES-EHT, EEPS, PLUS, and the OCMS Staff Portal (and other potential new offence sources).

2. If the data source indicates that the vehicle registration type is "F – Foreign," OCMS accepts it and proceeds with Notice creation.

3. If the vehicle registration type provided is "S – Local," OCMS checks the vehicle number using a backend detection algorithm to determine if it is a:<br>a. Diplomatic vehicle<br>b. Military vehicle<br>c. Vehicle with a valid VIP Parking Label; or<br>d. Local vehicle<br><br>Based on the outcome, OCMS assigns the correct registration type and creates the Notice, before routing the Notice to the appropriate processing flow.

## 1.2 High Level Flow

<!-- Insert high level flow diagram here -->
![High Level Flow](./images/section1-highlevel-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

**Flow Overview:**

The vehicle registration type detection is integrated within the Notice Creation processing flow. The detection occurs at Step 4 of the Notice creation process, executed by the `SpecialVehUtils.checkVehregistration()` method.

| Step | Process | Description |
| --- | --- | --- |
| 1 | Receive Notice Data | OCMS Admin API receives offence data from source systems (REPCCS, CES-EHT, EEPS, PLUS, Staff Portal) via API endpoints |
| 2 | Validate Mandatory Fields | System validates all mandatory fields using `NoticeValidationHelper.validateMandatoryFieldsForBatch()` method |
| 3 | Valid? | Decision point: If validation fails, return error response (appCode: OCMS-4000) and end process. If valid, continue to vehicle type detection |
| 4 | **Detect Vehicle Type (Step 4)** | **System executes 9-step detection algorithm using `SpecialVehUtils.checkVehregistration()` method. Returns vehicle registration type: F, S, D, I, V, or X** |
| 5 | Create Notice Record | System inserts notice record into OCMS_VALID_OFFENCE_NOTICE table with detected vehicle registration type |
| 6 | Foreign or VIP? | Decision point: Check if detected type is Foreign (F) or VIP (V) |
| 7 | Create Suspension | If Foreign or VIP, system inserts suspension record into SUSPENDED_NOTICE table (PS-FOR for Foreign, TS-OLD for VIP) |
| 8 | Return Success Response | System returns success response with appCode: OCMS-2000, message: Success, and generated notice number |
| 9 | End | Process completes successfully |

**Error Path:**

If validation fails at Step 3, system returns error response (appCode: OCMS-4000, message: Error details) and ends process without creating notice.

## 1.3 Vehicle Registration Type Detection Flow

<!-- Insert detailed flow diagram here -->
![Detection Flow Diagram](./images/section1-detection-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

**Detection Algorithm:**

The system executes a 9-step sequential detection algorithm to determine vehicle registration type. The detection follows strict sequential order with early exit optimization (first match terminates flow).

| Step | Process | Description | Technical Details |
| --- | --- | --- | --- |
| **DP-1** | Check Source Registration Type = "F"? | System checks if source system provided vehicleRegistrationType equals "F" (Foreign). If yes, accept as Foreign vehicle and skip all further validation. This honors source system's designation to avoid unnecessary processing. | Method: `SpecialVehUtils.checkVehregistration()` line 45-48. Rule: BE-001. Return: "F" if source type = "F", otherwise continue. |
| **DP-2** | Check Vehicle Number Blank/Null? | System validates if vehicle number is blank, null, empty string, or "N.A". If yes, proceed to check Offence Type. If no, continue to LTA validation. | Method: `SpecialVehUtils.checkVehregistration()` line 51-62. Rule: BE-002, BE-003. Condition: `if (vehNo == null \|\| vehNo.isEmpty() \|\| "N.A".equalsIgnoreCase(vehNo))` |
| **DP-2a** | Check Offence Type = "U"? | If vehicle number is blank, check if Offence Type equals "U" (UPL - Unlicensed Parking). If yes, return "X" (UPL Dummy Vehicle). If no, return error OCMS-4000 (Missing vehicle number mandatory for Offence Type O and E). | Rule: BE-002. Return: "X" if Type U, Error OCMS-4000 if Type O/E. Error Message: "Vehicle number is mandatory for Offence Type O and E" |
| **DP-3** | Call LTA Checksum Validation | System calls LTA Checksum Utility library using `ValidateRegistrationNo.validate(vehNo)` method. Vehicle number is converted to uppercase and trimmed before validation. | Method: `SpecialVehUtils.isSg()` line 154-175. Library: `validateGenerateVehicleNoSuffix.ValidateRegistrationNo`. Rule: BE-004. Exception: VehicleNoException caught and logged at DEBUG level. |
| **DP-3 Result** | Return Code = 1? | Check LTA validation result. If return code equals 1 (valid local vehicle), return "S" (Local Vehicle) and end detection. If return code not equal 1 or exception thrown, continue to diplomatic pattern check. | Fail-Safe: LTA failure does not block processing. System logs error and continues to next validation. Return: "S" if valid, continue if invalid. |
| **DP-4** | Check Diplomatic Pattern? | System checks if vehicle number matches diplomatic vehicle format using regex patterns. ALL conditions must be met: prefix "S" AND suffix in ["CC", "CD", "TC", "TE"]. If matched, return "D" (Diplomatic Vehicle). Otherwise continue to military pattern check. | Method: `SpecialVehUtils.isDip()` line 110-116. Rule: BE-005. Pattern: `^S.*CC$` OR `^S.*CD$` OR `^S.*TC$` OR `^S.*TE$`. Examples: SXX1234CC, S5678CD are diplomatic. |
| **DP-5** | Check Military Pattern? | System checks if vehicle number matches military vehicle format. ANY ONE condition must be met: prefix in ["MID", "MINDEF"] OR suffix in ["MID", "MINDEF"]. If matched, return "I" (Military Vehicle). Otherwise continue to VIP database query. | Method: `SpecialVehUtils.isMid()` line 123-128. Rule: BE-006. Logic: `startsWith("MID") OR endsWith("MID") OR startsWith("MINDEF") OR endsWith("MINDEF")`. Examples: MID1234X, SXX1234MID, MINDEF123. |
| **DP-6** | Query CAS VIP Database | System queries CAS database VIP_VEHICLE table using JPA query. Vehicle number is converted to uppercase before query. Query: `SELECT v FROM VipVehicle v WHERE v.vehicleNo = :vehicleNo AND v.status = 'A'`. If vehicle not found, continue to alphabet suffix check. If found, proceed to VIP status validation. | Method: `SpecialVehUtils.isVip()` line 135-147, `VipVehicleService.isVipVehicle()`. Database: CAS. Table: VIP_VEHICLE. Rule: BE-007. Fail-Safe: Database exception returns false (non-VIP), allows processing to continue. |
| **DP-7** | Check VIP Status = "A"? | If VIP vehicle found in database, check VIP_VEHICLE.status field. If status equals "A" (Active), return "V" (VIP Vehicle) and end detection. If status equals "D" (Defunct) or not found, continue to alphabet suffix check. | Rule: BE-008 (CAS transition period). Query filters: WHERE status = 'A'. Return: "V" if Active, continue if Defunct or not found. Note: FOMS implementation (validity date range check) planned for future. |
| **DP-8** | Check Last Character Alphabet? | System checks if last character of vehicle number is an alphabet using `Character.isLetter(vehNo.charAt(length-1))`. This edge case catches Singapore vehicles that failed LTA validation but have letter suffix. If matched, return "S" (Local Vehicle). Otherwise continue to default classification. | Method: `SpecialVehUtils.checkVehregistration()` line 84-87. Rule: BE-010. Purpose: Edge case for local vehicles that failed LTA checksum but have valid letter suffix pattern. |
| **DP-9** | Default Classification | If none of the above criteria are met, system defaults to "F" (Foreign Vehicle) for conservative classification. This flags unrecognized vehicle formats for manual review. | Rule: BE-012. Return: "F" (Foreign). Note: Code implementation defaults to Foreign. FD implies default to Local - business clarification recommended. |
| **End** | Detection Complete | Detection algorithm completes and returns vehicle registration type code to Notice Creation flow for storage in OCMS_VALID_OFFENCE_NOTICE.VEHICLE_REGISTRATION_TYPE field. | Possible return values: F, S, D, I, V, X, or Error. |

**Vehicle Registration Type Codes:**

| Type Code | Description | Detection Criteria | Return Step |
| --- | --- | --- | --- |
| **F** | Foreign Vehicle | Source-provided type = "F" OR no pattern matches (default) | DP-1 or DP-9 |
| **X** | UPL Dummy Vehicle | Blank/null vehicle number for Offence Type 'U' | DP-2a |
| **S** | Local Vehicle | LTA checksum validation passed OR alphabet suffix | DP-3 or DP-8 |
| **D** | Diplomatic Vehicle | Prefix = "S" AND Suffix in ["CC", "CD", "TC", "TE"] | DP-4 |
| **I** | Military Vehicle | Prefix OR Suffix in ["MID", "MINDEF"] | DP-5 |
| **V** | VIP Vehicle | Found in VIP_VEHICLE with status 'A' | DP-7 |

### 1.3.1 API Specification

#### API for eService

##### API Create Notice (Standard)

| Field | Value |
| --- | --- |
| API Name | create-notice |
| URL | UAT: https://[domain]/ocms/v1/create-notice <br> PRD: https://[domain]/ocms/v1/create-notice |
| Description | Standard notice creation from various sources with vehicle type detection at Step 4 |
| Method | POST |
| Header | `{ "Authorization": "Bearer [token]", "Content-Type": "application/json" }` |
| Payload | `{ "vehicleNo": "SBA1234Z", "vehicleRegistrationType": "S", "offenceNoticeType": "O", "noticeNo": "P1234567890", "compositionAmount": 100.00, "computerRuleCode": 101, "creDate": "2026-01-12T10:30:00", "creUserId": "ocmsiz_app_conn", "lastProcessingDate": "2026-01-12", "lastProcessingStage": "01", "noticeDateAndTime": "2026-01-10T14:30:00", "ppCode": "PP001", "ppName": "URA Car Park", "vehicleCategory": "C", "subsystemLabel": "003" }` |
| Response (Success) | `{ "HTTPStatusCode": "200", "HTTPStatusDescription": "Success", "data": { "appCode": "OCMS-2000", "message": "Notice successfully created", "noticeNo": "P1234567890" } }` |
| Response (Duplicate) | `{ "HTTPStatusCode": "400", "HTTPStatusDescription": "Bad Request", "data": { "appCode": "OCMS-4000", "message": "Notice no already exist" } }` |
| Response (Error) | `{ "HTTPStatusCode": "400", "HTTPStatusDescription": "Bad Request", "data": { "appCode": "OCMS-4000", "message": "Missing Mandatory Fields" } }` |

**Vehicle Type Detection:**
- Execution Point: Step 4 in `CreateNoticeServiceImpl.processSingleNotice()` flow
- Method: `SpecialVehUtils.checkVehregistration(vehicleNo, sourceProvidedType)`
- Detection Timing: BEFORE notice record creation
- Storage: Detected type stored in OCMS_VALID_OFFENCE_NOTICE.VEHICLE_REGISTRATION_TYPE field

---

##### API REPCCS Webhook Create Notice

| Field | Value |
| --- | --- |
| API Name | repccsWebhook |
| URL | UAT: https://[domain]/ocms/v1/repccsWebhook <br> PRD: https://[domain]/ocms/v1/repccsWebhook |
| Description | Notice creation from REPCCS system via webhook with transaction audit logging and duplicate notice check |
| Method | POST |
| Header | `{ "Authorization": "Bearer [token]", "Content-Type": "application/json" }` |
| Payload | `{ "transactionId": "REP-20260112-001", "vehicleNo": "SXX1234CC", "offenceNoticeType": "O", "noticeNo": "R5005003012", "noticeDateAndTime": "2026-01-10T14:30:00", "compositionAmount": 150.00, "computerRuleCode": 201, "vehicleCategory": "C", "subsystemLabel": "020" }` |
| Response (Success) | `{ "HTTPStatusCode": "200", "HTTPStatusDescription": "Success", "data": { "appCode": "OCMS-2000", "message": "OK" } }` |
| Response (Duplicate) | `{ "HTTPStatusCode": "400", "HTTPStatusDescription": "Bad Request", "data": { "appCode": "OCMS-4000", "message": "Notice no Already exists" } }` |

**Special Features:**
- Transaction audit logging with transaction ID before processing
- Duplicate notice check ACTIVE before vehicle type detection
- Response format modified per REPCCS requirement: message="OK", noticeNo removed from success response
- Subsystem label: "020" (REPCCS_ENC_CODE_020) or "021"

---

##### API CES/EHT Webhook Create Notice

| Field | Value |
| --- | --- |
| API Name | cesWebhook-create-notice |
| URL | UAT: https://[domain]/ocms/v1/cesWebhook-create-notice <br> PRD: https://[domain]/ocms/v1/cesWebhook-create-notice |
| Description | Real-time notice creation from CES/EHT system via webhook with subsystem label validation (range 030-999) |
| Method | POST |
| Header | `{ "Authorization": "Bearer [token]", "Content-Type": "application/json" }` |
| Payload | `{ "vehicleNo": "MID1234X", "offenceNoticeType": "E", "noticeNo": "E0301234567", "noticeDateAndTime": "2026-01-10T14:30:00", "compositionAmount": 200.00, "computerRuleCode": 301, "vehicleCategory": "C", "subsystemLabel": "030" }` |
| Response (Success) | `{ "HTTPStatusCode": "200", "HTTPStatusDescription": "Success", "data": { "appCode": "OCMS-2000", "message": "Notice successfully created", "noticeNo": "E0301234567" } }` |
| Response (Invalid Subsystem) | `{ "HTTPStatusCode": "400", "HTTPStatusDescription": "Bad Request", "data": { "appCode": "OCMS-4000", "message": "subsystemLabel Not in Range 030 - 999" } }` |

**Special Validations:**
- Subsystem label validation: 3-8 characters, first 3 characters numeric (030-999 range)
- Duplicate notice check enabled
- Offence rule code validation active

---

#### API Consume

##### API LTA Checksum Utility

| Field | Value |
| --- | --- |
| API Name | LTA Checksum Validation |
| Integration Type | Java Library (validateGenerateVehicleNoSuffix.jar) |
| Description | Validate vehicle registration number using LTA official checksum algorithm. Supports all Singapore vehicle formats with checksum validation. |
| Method | Library Call |
| Class | ValidateRegistrationNo |
| Function | `validate(String vehicleNo)` |
| Input | Vehicle registration number (String, uppercase, trimmed) |
| Output (Valid) | Return value: true (valid local vehicle, checksum passed) |
| Output (Invalid) | Return value: false (invalid format) OR VehicleNoException thrown |
| Exception | VehicleNoException caught and logged at DEBUG level with return code and message |
| Error Handling | Fail-safe design: Exception returns false and continues to next validation check. Does NOT block processing. |
| Supported Formats | Single letter prefix (E,F,G,Q,S,W,X,Y), Double letter prefix (QA,QB,PA,XA,SBA,SKM), Triple letter prefix (SAG,SPF,SBS,CSS,LTA) |
| Usage | Step DP-3 in detection algorithm. Method: `SpecialVehUtils.isSg()` line 154-175 |

**Data Source:** LTA official library (external dependency)

---

##### API CAS VIP Vehicle Status Query

| Field | Value |
| --- | --- |
| API Name | VIP Vehicle Status Check |
| Integration Type | Database Query (JPA Repository) |
| Description | Check if vehicle has active VIP Parking Label in CAS database. Query returns true only if vehicle found with status 'A' (Active). |
| Method | Database Query |
| Database | CAS (Compliance Assessment Services) |
| Table | VIP_VEHICLE |
| Query | `SELECT v FROM VipVehicle v WHERE v.vehicleNo = :vehicleNo AND v.status = 'A'` |
| Input | Vehicle registration number (String, converted to uppercase) |
| Output (VIP Found) | Boolean: true (vehicle found with status 'A' - Active VIP label) |
| Output (Not Found) | Boolean: false (vehicle not found OR status 'D' - Defunct OR status not 'A') |
| Error Handling | Exception caught and logged at ERROR level. Returns false (fail-safe: allows processing to continue if CAS unavailable). |
| Fields Queried | VEHICLE_NO (Primary Key), STATUS ('A' = Active, 'D' = Defunct) |
| Usage | Step DP-6 and DP-7 in detection algorithm. Method: `SpecialVehUtils.isVip()` line 135-147, `VipVehicleService.isVipVehicle()` |
| Performance Note | Query positioned late in detection sequence (after format checks) to minimize unnecessary database calls |

**Data Source:** CAS database VIP_VEHICLE table

**Future Enhancement:** FOMS implementation will add validity date range check (VALIDITY_START_DATE, VALIDITY_END_DATE compared with offence date).

### 1.3.2 Data Mapping

#### Input Data Fields

| Zone | Database Table | Field Name | Data Type | Source | Mandatory | Description |
| --- | --- | --- | --- | --- | --- | --- |
| Source System | Request Payload | vehicleNo | String | REPCCS, CES-EHT, EEPS, PLUS, Staff Portal | Conditional* | Vehicle registration number (uppercase, trimmed) |
| Source System | Request Payload | vehicleRegistrationType | String | Source system (optional) | No | Source-provided registration type (if available) |
| Source System | Request Payload | offenceNoticeType | String | Source system | Yes | Offence notice type: U, O, E |
| Source System | Request Payload | noticeDateAndTime | DateTime | Source system | Yes | Date and time of offence occurrence |
| Source System | Request Payload | computerRuleCode | Integer | Source system | Yes | Offence rule code for validation |
| Source System | Request Payload | vehicleCategory | String | Source system | Yes | Vehicle category: C, M, H, B, A |
| Source System | Request Payload | subsystemLabel | String | Source system | Yes | Source system identifier (3-8 characters) |

*Mandatory except for Offence Type 'U' (UPL - Unlicensed Parking)

#### Output Data Fields

| Zone | Database Table | Field Name | Data Type | Possible Values | Description |
| --- | --- | --- | --- | --- | --- |
| Intranet | OCMS_VALID_OFFENCE_NOTICE | VEHICLE_REGISTRATION_TYPE | String | F, S, D, I, V, X | Detected vehicle registration type from 9-step algorithm |
| Intranet | OCMS_VALID_OFFENCE_NOTICE | VEHICLE_NO | String | Alphanumeric | Vehicle registration number (uppercase, trimmed, standardized) |
| Intranet | OCMS_VALID_OFFENCE_NOTICE | OFFENCE_NOTICE_TYPE | String | U, O, E | Offence notice type from source |
| Intranet | OCMS_VALID_OFFENCE_NOTICE | NOTICE_NO | String | Alphanumeric | System-generated notice number (unique identifier) |
| Intranet | SUSPENDED_NOTICE | SUSPENSION_TYPE | String | PS, TS | PS-FOR for Foreign (F), TS-OLD for VIP (V) |

#### Database Tables Accessed

| Zone | Database Table | Field Name | Access Type | Usage | Data Source |
| --- | --- | --- | --- | --- | --- |
| CAS | VIP_VEHICLE | VEHICLE_NO | READ | Lookup | Primary key for VIP vehicle query |
| CAS | VIP_VEHICLE | STATUS | READ | Validation | VIP label status ('A' = Active, 'D' = Defunct) |
| CAS | VIP_VEHICLE | VALIDITY_START_DATE | READ | Validation (FOMS) | VIP label validity start date (planned future) |
| CAS | VIP_VEHICLE | VALIDITY_END_DATE | READ | Validation (FOMS) | VIP label validity end date (planned future) |
| Intranet | OCMS_VALID_OFFENCE_NOTICE | VEHICLE_REGISTRATION_TYPE | WRITE | Insert | Store detected registration type |
| Intranet | OCMS_VALID_OFFENCE_NOTICE | VEHICLE_NO | WRITE | Insert | Store vehicle registration number |
| Intranet | OCMS_VALID_OFFENCE_NOTICE | NOTICE_NO | WRITE | Insert | Store generated notice number |
| Intranet | SUSPENDED_NOTICE | SUSPENSION_TYPE | WRITE | Insert | Create PS-FOR for Foreign, TS-OLD for VIP |
| Intranet | SUSPENDED_NOTICE | REASON_OF_SUSPENSION | WRITE | Insert | Reason: "FOR" for Foreign, "OLD" for VIP |

#### Vehicle Registration Type Codes (Data Source Attribution)

| Type Code | Description | Detection Criteria | Data Source | Example |
| --- | --- | --- | --- | --- |
| **F** | Foreign Vehicle | Source-provided type = "F" OR no pattern matches (default) | Source system OR Detection algorithm default | Source: User input OR System calculated |
| **X** | UPL Dummy Vehicle | Blank/null vehicle number for Offence Type 'U' | Detection algorithm (BE-002) | Source: System calculated (blank vehicle number) |
| **S** | Local Vehicle | LTA checksum valid OR alphabet suffix | LTA library OR Detection algorithm (BE-004, BE-010) | Source: LTA Checksum Utility OR Pattern match |
| **D** | Diplomatic Vehicle | Prefix = "S" AND Suffix in ["CC", "CD", "TC", "TE"] | Detection algorithm (BE-005) | Source: Pattern match (regex) |
| **I** | Military Vehicle | Prefix OR Suffix in ["MID", "MINDEF"] | Detection algorithm (BE-006) | Source: Pattern match (string check) |
| **V** | VIP Vehicle | Found in VIP_VEHICLE with status 'A' | CAS database VIP_VEHICLE table (BE-007, BE-008) | Source: Database: CAS.VIP_VEHICLE |

#### Vehicle Number Format Examples

| Registration Type | Prefix | Suffix | Example | Pattern/Logic | Data Source |
| --- | --- | --- | --- | --- | --- |
| Diplomatic | S | CC, CD, TC, TE | SXX1234CC, S5678CD | Regex: ^S.*CC$, ^S.*CD$, ^S.*TC$, ^S.*TE$ | Pattern match |
| Military | MID, MINDEF | MID, MINDEF | MID1234X, SXX1234MID, MINDEF123 | startsWith/endsWith "MID" or "MINDEF" | Pattern match |
| Local (LTA Valid) | Various | Various | SBA1234Z, SXX9876A | LTA checksum validation passed | LTA library |
| Local (Edge Case) | Various | Alphabet | ABC1234X | Last character is letter | Pattern match |
| UPL Dummy | N/A | N/A | N.A, blank, null | Blank for Offence Type 'U' | System constant |
| Foreign (Default) | Various | Various | ABC123, XYZ789 | No pattern matches | Default classification |

**Note on Audit Fields:**
- Audit user fields populated using system connection user (Data Source: Configuration)
- Intranet Zone: `ocmsiz_app_conn` (Source: System constant)
- Internet Zone: `ocmsez_app_conn` (Source: System constant)

**Insert Order:**
- Parent table (OCMS_VALID_OFFENCE_NOTICE) INSERT FIRST with vehicle registration type
- Child table (SUSPENDED_NOTICE) INSERT AFTER (if Foreign or VIP)

### 1.3.3 Success Outcome

- Vehicle registration type successfully determined using sequential 9-step detection algorithm (DP-1 through DP-9)
- Detected registration type (F, S, D, I, V, or X) stored in OCMS_VALID_OFFENCE_NOTICE.VEHICLE_REGISTRATION_TYPE field
- Notice record successfully inserted into OCMS_VALID_OFFENCE_NOTICE table with all mandatory fields
- For Foreign vehicles (F): PS-FOR (Permanent Suspension - Foreign) suspension record automatically created in SUSPENDED_NOTICE table with reason_of_suspension = "FOR"
- For VIP vehicles (V): TS-OLD (Temporary Suspension - OLD) suspension record automatically created in SUSPENDED_NOTICE table
- Notice routed to appropriate processing flow based on registration type:
  - Foreign (F) → Suspension flow (PS-FOR created)
  - VIP (V) → Temporary suspension flow (TS-OLD created)
  - Diplomatic (D) → Special handling flow
  - Military (I) → MINDEF owner assignment flow with standardized owner details
  - Local (S) → Standard notice processing flow
  - UPL (X) → Unlicensed parking flow
- API returns success response: HTTPStatusCode 200, appCode OCMS-2000, message "Notice successfully created" (or "OK" for REPCCS), generated notice number
- Detection algorithm completes without errors, no external dependency failures blocked processing
- The workflow reaches the End state without triggering any error-handling paths

### 1.3.4 Error Handling

#### Application Error Handling

| Error Scenario | Definition | Brief Description | Data Source |
| --- | --- | --- | --- |
| Missing Vehicle Number (Non-UPL) | Vehicle number is blank, null, empty string, or "N.A" for Offence Type 'O' or 'E' | Return error code OCMS-4000 with message "Vehicle number is mandatory for Offence Type O and E". Vehicle number is required for non-UPL notices. Processing stops at DP-2a, no notice created. | Source: Validation rule BE-002 |
| LTA Validation Failure | LTA Checksum Utility throws VehicleNoException or returns false (invalid format) | Log error at DEBUG level with vehicle number, return code, and exception message. System continues to next validation check (Diplomatic pattern at DP-4). Fail-safe design: LTA failure does NOT block processing. System remains operational even if LTA library unavailable. | Source: External library error (LTA) |
| CAS Database Connection Failure | Unable to query VIP_VEHICLE table due to database exception, connection timeout, or CAS unavailable | Log error at ERROR level with vehicle number and exception message. Return false (vehicle classified as non-VIP). System continues to alphabet suffix check (DP-8). Fail-safe design: Allows processing to continue if CAS database unavailable. | Source: Database connection error (CAS) |
| Invalid Vehicle Format | Vehicle number does not match any known pattern (Foreign, Local, Diplomatic, Military, VIP, UPL) | Default to Foreign vehicle classification (return "F" at DP-9). Flag for manual review. No error returned to user, processing continues with conservative classification for unrecognized formats. | Source: Detection algorithm default (BE-012) |
| Duplicate Notice Number | Notice number already exists in OCMS_VALID_OFFENCE_NOTICE table (checked before vehicle type detection) | Return error code OCMS-4000 with message "Notice no already exist". Processing stops, no notice created. Applies to REPCCS and EHT sources. EEPS, PLUS, Staff Portal generate notice numbers internally so will not encounter this error. | Source: Database query (duplicate check) |
| Invalid Subsystem Label | Subsystem label validation failure for CES/EHT webhook: length not 3-8 characters OR first 3 characters not numeric OR numeric value not in range 030-999 | Return error code OCMS-4000 with specific message: "subsystemLabel Length < 3", "subsystemLabel Length > 8", "Subsystem label must be a 3-digit numeric value", or "subsystemLabel Not in Range 030 - 999". Processing stops, no notice created. Applies to CES webhook only. | Source: Input validation (BE-016) |
| Invalid Offence Rule Code | Offence rule code not found in OFFENCE_RULE_CODE table OR not valid for current date and vehicle category | Return error code OCMS-4000 with error type "RULE_CODE_ERROR". Processing stops, no notice created. Database query: find offenceNoticeType by ruleCode, vehicleCategory, and current date within effectiveStartDate and effectiveEndDate range. | Source: Database query (OFFENCE_RULE_CODE table) |

#### API Error Handling

| Error Scenario | App Error Code | User Message | Brief Description | Data Source |
| --- | --- | --- | --- | --- |
| Missing Mandatory Fields | OCMS-4000 | Missing Mandatory Fields | Required fields (vehicleNo for O/E types, compositionAmount, computerRuleCode, etc.) are blank or null. Validation fails before vehicle type detection at Step 2. | Validation: NoticeValidationHelper |
| Bad Request | OCMS-4000 | Invalid request. Please check and try again. | Invalid input format or failed validation. Generic client error for malformed requests. | Input validation |
| Notice Already Exists | OCMS-4000 | Notice no already exist | Duplicate notice number detected in OCMS_VALID_OFFENCE_NOTICE table. Applies to REPCCS and EHT sources (other sources generate notice numbers). | Database query |
| Subsystem Label Error | OCMS-4000 | subsystemLabel Not in Range 030 - 999 | Subsystem label validation failure for CES/EHT webhook. Length or range check failed (must be 3-8 chars, numeric prefix 030-999). | Input validation |
| Rule Code Error | OCMS-4000 | RULE_CODE_ERROR | Offence rule code not found in OFFENCE_RULE_CODE table or not valid for current date and vehicle category. | Database query |
| Unauthorized Access | OCMS-4001 | You are not authorized to access this resource. Please log in and try again. | Authorization token invalid, missing, or expired. Authentication failure. | Authentication service |
| Resource Not Found | OCMS-4004 | The page or resource you are looking for could not be found. | Requested resource does not exist. API endpoint or entity not found. | System |
| General Server Error | OCMS-5000 | Something went wrong. Please try again later. | Internal server error during detection or notice creation. Generic system error for unexpected failures. | System |
| Database Connection Failed | OCMS-5001 | Unable to save data due to server issues. | Database connection error. Unable to insert notice record into OCMS_VALID_OFFENCE_NOTICE table. | Database connection |
| Request Timeout | OCMS-5002 | The request timed out. | Request processing exceeded timeout threshold. Long-running operation timeout. | System |
| Unexpected Error | OCMS-5003 | Something went wrong. Please try again later. | Unexpected system error not covered by other error codes. Catch-all for unhandled exceptions. | System |

**Standard Error Codes Reference:**

Refer to FD Section 2 for complete error code definitions and business rules.

| Code | Category | HTTP Status | Description | Usage |
| --- | --- | --- | --- | --- |
| OCMS-2000 | Success | 200 | Operation completed successfully | All successful operations |
| OCMS-4000 | Client Error | 400 | Bad Request - Invalid input or validation failure | Input validation errors |
| OCMS-4001 | Client Error | 401 | Unauthorized Access | Authentication failures |
| OCMS-4004 | Client Error | 404 | Resource Not Found | Entity or endpoint not found |
| OCMS-5000 | Server Error | 500 | Internal Server Error | System errors |
| OCMS-5001 | Server Error | 500 | Database Connection Failed | Database connection issues |

**Error Handling Strategy:**

1. **Fail-Safe Design for External Dependencies:**
   - LTA Checksum Utility failure: Returns false, continues to next validation check (Diplomatic pattern)
   - CAS VIP_VEHICLE query failure: Returns false (non-VIP), continues to alphabet suffix check
   - System remains operational even if external dependencies (LTA library, CAS database) are unavailable
   - No blocking errors for external API/database failures

2. **Detailed Logging for Troubleshooting:**
   - All errors logged with context: vehicle number, error code, exception message, stack trace
   - LTA failures logged at DEBUG level (expected behavior for invalid vehicle numbers)
   - CAS failures logged at ERROR level (unexpected system issue)
   - Comprehensive audit trail for troubleshooting and system monitoring

3. **Generic User Messages:**
   - Error messages to user are generic to avoid exposing system internals and technical details
   - Detailed technical errors stored in application logs only
   - User-friendly messages guide user to corrective actions

4. **Early Exit on Critical Errors:**
   - Missing mandatory fields: Immediate error response at Step 3, stop before vehicle type detection
   - Duplicate notice number: Immediate error response at validation, stop before processing
   - Invalid input format: Immediate error response at validation, no notice created

5. **Conservative Classification for Edge Cases:**
   - Unrecognized vehicle formats default to Foreign (F) rather than blocking notice creation
   - Allows notice to be created and flagged for manual review by operations team
   - Better to create notice with Foreign designation than to reject valid notice

---

## Business Rules

Refer to Functional Document Section 2 for detailed business rules including:

- **BR-01: Vehicle Registration Type Priority** - Foreign vehicle designation ("F") from source takes precedence over all other checks to avoid unnecessary processing
- **BR-02: Registration Type Classification Hierarchy** - Vehicle types detected in strict sequential order: Foreign (source) → UPL → LTA validation → Diplomatic pattern → Military pattern → VIP database → Alphabet suffix → Default Foreign
- **BR-03: Sequential Validation Process** - Detection algorithm follows strict sequential order with no parallel checks. Early exit optimization: first match terminates detection flow.
- **BR-04: VIP Label Validity Rules** - Transition period (CAS): Only status matters ('A' = valid, 'D' = invalid). FOMS implementation (planned): Both status AND validity date range matter (offence date must be within validity period).
- **BR-05: Mandatory Vehicle Number Exception** - Vehicle number mandatory for Offence Types 'O' and 'E'. Optional for Type 'U' (UPL - Unlicensed Parking). Error OCMS-4000 returned if vehicle number missing for non-UPL types.
- **BR-06: Notice Routing by Registration Type** - Vehicle registration type determines Notice processing flow and automatic suspension creation. Different registration types follow different downstream processes.

## Validation Rules

Refer to Functional Document Section 2.3 and plan_condition.md for detailed validation rules including:

- **Backend Validations**: Rule BE-001 through BE-019 covering vehicle type detection (9 steps), notice creation validations, and data quality checks
- **Business Logic Validations**: LTA checksum validation using official library, diplomatic/military pattern matching using regex, VIP database lookup with fail-safe error handling
- **Error Handling Strategy**: Fail-safe design for external dependencies (LTA, CAS), detailed logging at appropriate levels (DEBUG for expected failures, ERROR for system issues), conservative classification for unrecognized formats

---

**Document Notes:**

1. This technical document describes the implementation of vehicle registration type detection as specified in Functional Document Section 2. The detection algorithm is executed as part of the Notice Creation workflow at Step 4, prior to Notice record creation.

2. Vehicle type detection method: `SpecialVehUtils.checkVehregistration(vehicleNo, sourceProvidedType)` executed in `CreateNoticeServiceImpl.processSingleNotice()` at Step 4.

3. Detection results determine subsequent Notice processing flows and automatic suspension creation: PS-FOR (Permanent Suspension - Foreign) for Foreign vehicles (F), TS-OLD (Temporary Suspension - OLD) for VIP vehicles (V).

4. Fail-safe design: External API failures (LTA Checksum Utility library, CAS VIP_VEHICLE database query) do not block processing. System logs error and continues to next validation check, allowing Notice creation to proceed.

5. FOMS implementation (validity date range check for VIP labels using VALIDITY_START_DATE and VALIDITY_END_DATE fields) is planned for future release and not yet implemented. Current implementation uses CAS database with status field only ('A' = Active, 'D' = Defunct).

6. Default classification difference: Code implementation defaults to Foreign ("F") at final step, Functional Document implies Local ("S") as default. Business clarification recommended for edge case handling.

7. Flow diagrams: Extract images from technical flowchart file OCMS14-Technical_Flowchart_Section_1.drawio and save to images folder as section1-highlevel-flow.png and section1-detection-flow.png.

8. All APIs use POST method only per OCMS standards. No GET, PUT, PATCH, or DELETE methods allowed.

9. Response format uses appCode and message fields (NOT status field) per OCMS API guidelines.

10. Data source attribution: All data fields documented with source (User input, System calculated, Database table, External API, Configuration, etc.) for developer clarity.

---

**Source Documents:**
- OCMS 14 Functional Document v1.8 Section 2
- OCMS14-Functional_Flowchart_Section_2.drawio (Functional flowchart)
- OCMS14-Technical_Flowchart_Section_1.drawio (Technical flowchart)
- Backend Code Analysis: SpecialVehUtils.java, VipVehicleService.java, CreateNoticeServiceImpl.java, NoticeValidationHelper.java
- plan_api.md v1.0 (API specifications)
- plan_condition.md v1.0 (Validation rules BE-001 to BE-019, Business rules BR-01 to BR-06)
- plan_flowchart.md v1.0 (Flowchart technical design)

---

*End of Section 1*

# Condition Plan: Detecting Vehicle Registration Type

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

## 1. Reference Documents

| Document Type | Reference | Description |
| --- | --- | --- |
| Functional Document | OCMS 14 - Section 2 | Detecting Vehicle Registration Type |
| Technical Document | OCMS 14 - Section 1 | Technical implementation |
| Backend Code | SpecialVehUtils.java | Vehicle registration type check logic |
| Backend Code | VipVehicleService.java | VIP vehicle query service |

---

## 2. Business Conditions

### 2.1 Vehicle Registration Type Detection

**Description:** Determines the vehicle registration type based on source-provided data and vehicle number format.

**Reference:** FD Section 2.3 - Process Flow of the Vehicle Registration Type Check

#### Condition Matrix

| Condition ID | Condition Name | Input Fields | Logic | Output/Action |
| --- | --- | --- | --- | --- |
| C001 | Foreign Vehicle Check | sourceProvidedType | sourceProvidedType = 'F' | Return 'F' |
| C002 | UPL Vehicle Check | vehicleNo, offenceType | vehicleNo is blank/null AND offenceType = 'U' | Return 'X' |
| C003a | Blank Vehicle Error (Type O) | vehicleNo, offenceType | vehicleNo is blank/null AND offenceType = 'O' | Return Error OCMS-1401 |
| C003b | Blank Vehicle Error (Type E) | vehicleNo, offenceType | vehicleNo is blank/null AND offenceType = 'E' | Return Error OCMS-1402 |
| C004 | Singapore Vehicle Check | vehicleNo | LTA Checksum validation = true | Return 'S' |
| C005 | Diplomatic Vehicle Check | vehicleNo | Prefix = 'S' AND Suffix IN ('CC', 'CD', 'TC', 'TE') | Return 'D' |
| C006 | Military Vehicle Check | vehicleNo | Prefix/Suffix = 'MID' OR 'MINDEF' | Return 'I' |
| C007 | VIP Vehicle Check | vehicleNo | Found in ocms_vip_vehicle with status = 'A' | Return 'V' |
| C008 | Local Vehicle Default | vehicleNo | No VIP match found | Return 'S' |

---

#### Condition Details

**C001: Foreign Vehicle Check**

| Attribute | Value |
| --- | --- |
| Description | Check if source system marked the vehicle as Foreign |
| Trigger | Start of Vehicle Registration Type Check |
| Input | sourceProvidedType (from source system) |
| Input Data Type | varchar(1), Nullable |
| Logic | sourceProvidedType equals 'F' |
| Output | Return 'F' (Foreign Vehicle) |
| Else | Proceed to C002 |

```
IF sourceProvidedType = 'F'
THEN Return 'F'
ELSE Proceed to next check
```

---

**C002: UPL Vehicle Check**

| Attribute | Value |
| --- | --- |
| Description | Check if vehicle is UPL (Unauthorized Parking Lot) with blank vehicle number |
| Trigger | After C001 fails |
| Input | vehicleNo, offenceType |
| Input Data Type | vehicleNo: varchar(14), offenceType: varchar(1) |
| Logic | vehicleNo is NULL/empty/blank OR equals 'N.A' OR equals 'UNLICENSED_PARKING' |
| Output | Return 'X' (UPL Dummy Vehicle) if offenceType = 'U' |
| Else | Proceed to C003a/C003b if offenceType is 'O' or 'E' |

```
IF vehicleNo IS NULL OR vehicleNo = '' OR vehicleNo = 'N.A' OR vehicleNo = 'UNLICENSED_PARKING'
THEN
    IF offenceType = 'U'
    THEN Return 'X'
    ELSE Return Error (C003a or C003b)
```

**Blank Vehicle Values:**

| Value | Description |
| --- | --- |
| null | Null value |
| "" | Empty string |
| " " | Whitespace only |
| "N.A" | Not Available marker |
| "UNLICENSED_PARKING" | UPL marker |

---

**C003a: Blank Vehicle Error (Type O)**

| Attribute | Value |
| --- | --- |
| Description | Error condition for blank vehicle number with On-street offence |
| Trigger | vehicleNo is blank AND offenceType = 'O' |
| Input | vehicleNo, offenceType |
| Input Data Type | vehicleNo: varchar(14), offenceType: varchar(1) |
| Logic | vehicleNo is blank AND offenceType = 'O' |
| Error Code | OCMS-1401 |
| Error Message | Vehicle number is required for On-street (O) offence type |
| Output | Return error to Notice Creation flow |

```
IF vehicleNo IS BLANK AND offenceType = 'O'
THEN Return Error {
    "data": {
        "appCode": "OCMS-1401",
        "message": "Vehicle number is required for On-street (O) offence type"
    }
}
```

---

**C003b: Blank Vehicle Error (Type E)**

| Attribute | Value |
| --- | --- |
| Description | Error condition for blank vehicle number with ERP offence |
| Trigger | vehicleNo is blank AND offenceType = 'E' |
| Input | vehicleNo, offenceType |
| Input Data Type | vehicleNo: varchar(14), offenceType: varchar(1) |
| Logic | vehicleNo is blank AND offenceType = 'E' |
| Error Code | OCMS-1402 |
| Error Message | Vehicle number is required for ERP (E) offence type |
| Output | Return error to Notice Creation flow |

```
IF vehicleNo IS BLANK AND offenceType = 'E'
THEN Return Error {
    "data": {
        "appCode": "OCMS-1402",
        "message": "Vehicle number is required for ERP (E) offence type"
    }
}
```

---

**C004: Singapore Vehicle Check (LTA Validation)**

| Attribute | Value |
| --- | --- |
| Description | Validate vehicle number using LTA Checksum utility |
| Trigger | After C002 passes (vehicle number is not blank) |
| Input | vehicleNo |
| Input Data Type | varchar(14) |
| Logic | Call LTA ValidateRegistrationNo.validate(vehicleNo) |
| Output | Return 'S' (Local/Singapore) if validation returns true |
| Else | Proceed to C005 |
| Error Handling | On exception, log error and proceed to C005 |

```
TRY
    IF LTA.ValidateRegistrationNo.validate(vehicleNo) = TRUE
    THEN Return 'S'
    ELSE Proceed to Diplomatic check
CATCH Exception
    Log error
    Proceed to Diplomatic check
```

---

**C005: Diplomatic Vehicle Check**

| Attribute | Value |
| --- | --- |
| Description | Check if vehicle number matches Diplomatic vehicle format |
| Trigger | After C004 fails (not a valid Singapore vehicle) |
| Input | vehicleNo |
| Input Data Type | varchar(14) |
| Logic | vehicleNo starts with 'S' AND ends with 'CC', 'CD', 'TC', or 'TE' |
| Output | Return 'D' (Diplomatic) if matches |
| Else | Proceed to C006 |

```
IF vehicleNo MATCHES '^S.*CC$' OR '^S.*CD$' OR '^S.*TC$' OR '^S.*TE$'
THEN Return 'D'
ELSE Proceed to Military check
```

**Diplomatic Vehicle Suffixes:**

| Suffix | Description |
| --- | --- |
| CC | Consular Corps |
| CD | Corps Diplomatique |
| TC | Technical Corps |
| TE | Technical Embassy |

---

**C006: Military Vehicle Check**

| Attribute | Value |
| --- | --- |
| Description | Check if vehicle number matches Military vehicle format |
| Trigger | After C005 fails (not a Diplomatic vehicle) |
| Input | vehicleNo |
| Input Data Type | varchar(14) |
| Logic | vehicleNo starts with OR ends with 'MID' or 'MINDEF' |
| Output | Return 'I' (Military) if matches |
| Else | Proceed to C007 |

```
IF vehicleNo STARTS WITH 'MID' OR 'MINDEF'
   OR vehicleNo ENDS WITH 'MID' OR 'MINDEF'
THEN Return 'I'
ELSE Proceed to VIP check
```

**Military Vehicle Patterns:**

| Pattern | Description | Example |
| --- | --- | --- |
| MID prefix | Vehicle number starts with 'MID' | MID1234 |
| MID suffix | Vehicle number ends with 'MID' | 1234MID |
| MINDEF | Vehicle number contains 'MINDEF' | MINDEF123 |

---

**C007: VIP Vehicle Check**

| Attribute | Value |
| --- | --- |
| Description | Query OCMS database to check if vehicle has active VIP Parking Label |
| Trigger | After C006 fails (not a Military vehicle) |
| Input | vehicleNo |
| Input Data Type | varchar(14) |
| Logic | Query ocms_vip_vehicle table for vehicleNo with status = 'A' |
| Output | Return 'V' (VIP) if found with active status |
| Else | Proceed to C008 (Return 'S' as Local default) |
| Error Handling | On connection failure, log error and proceed to C008 |

```
TRY
    IF EXISTS (SELECT 1 FROM ocms_vip_vehicle WHERE vehicle_no = vehicleNo AND status = 'A')
    THEN Return 'V'
    ELSE Return 'S' (Local Vehicle Default)
CATCH Exception
    Log error
    Return 'S' (Local Vehicle Default)
```

**VIP Status Values:**

| Status | Data Type | Description |
| --- | --- | --- |
| A | varchar(1) | Active - Valid VIP Parking Label |
| D | varchar(1) | Defunct - Expired/Invalid VIP Parking Label |

**VIP Status Transition (Future - FOMS Implementation):**

| Current System (OCMS VIP Table) | Future System (FOMS) |
| --- | --- |
| status = 'A' | status = 'A' AND offence_date BETWEEN start_date AND end_date |
| status = 'D' | status = 'D' OR offence_date NOT BETWEEN start_date AND end_date |

---

**C008: Local Vehicle Default**

| Attribute | Value |
| --- | --- |
| Description | Default to Local vehicle if no VIP match found (per FD Section 2.3 Step 8) |
| Trigger | After C007 fails (not a VIP vehicle) |
| Input | vehicleNo |
| Input Data Type | varchar(14) |
| Logic | No VIP match found - confirm as Local vehicle |
| Output | Return 'S' (Local) |
| Else | N/A - This is the final default |

```
IF NOT_FOUND_IN_VIP_DATABASE
THEN Return 'S' (Local Vehicle)
```

**Note:** Per FD Section 2.3 Step 8: "If the vehicle does not meet the criteria for 'V' (VIP), OCMS confirms that the vehicle registration type is 'S' (Local Vehicle)"

---

## 3. Decision Tree

### 3.1 Vehicle Registration Type Check Flow

```
START
  │
  ├─► Source provided type = 'F'?
  │     │
  │     ├─ YES ─► Return 'F' (Foreign) ─► END
  │     │
  │     └─ NO ──► Vehicle number blank/null?
  │                 │
  │                 ├─ YES ─► Offence Type = 'U'?
  │                 │           │
  │                 │           ├─ YES ─► Return 'X' (UPL) ─► END
  │                 │           │
  │                 │           └─ NO ──► Offence Type = 'O'?
  │                 │                       │
  │                 │                       ├─ YES ─► Return ERROR OCMS-1401 ─► END
  │                 │                       │
  │                 │                       └─ NO ──► Return ERROR OCMS-1402 ─► END
  │                 │
  │                 └─ NO ──► LTA Checksum Valid?
  │                             │
  │                             ├─ YES ─► Return 'S' (Local) ─► END
  │                             │
  │                             └─ NO ──► Is Diplomatic format?
  │                                         │
  │                                         ├─ YES ─► Return 'D' ─► END
  │                                         │
  │                                         └─ NO ──► Is Military format?
  │                                                     │
  │                                                     ├─ YES ─► Return 'I' ─► END
  │                                                     │
  │                                                     └─ NO ──► In VIP DB (status='A')?
  │                                                                 │
  │                                                                 ├─ YES ─► Return 'V' ─► END
  │                                                                 │
  │                                                                 └─ NO ──► Return 'S' (Local Default) ─► END
END
```

### Decision Table

| Source='F' | Blank VehNo | Type | LTA Valid | Diplomatic | Military | VIP DB | Result |
| --- | --- | --- | --- | --- | --- | --- | --- |
| TRUE | - | - | - | - | - | - | F |
| FALSE | TRUE | U | - | - | - | - | X |
| FALSE | TRUE | O | - | - | - | - | ERROR (OCMS-1401) |
| FALSE | TRUE | E | - | - | - | - | ERROR (OCMS-1402) |
| FALSE | FALSE | - | TRUE | - | - | - | S |
| FALSE | FALSE | - | FALSE | TRUE | - | - | D |
| FALSE | FALSE | - | FALSE | FALSE | TRUE | - | I |
| FALSE | FALSE | - | FALSE | FALSE | FALSE | TRUE | V |
| FALSE | FALSE | - | FALSE | FALSE | FALSE | FALSE | S (Local Default) |

---

## 4. Validation Rules

### 4.1 Field Validations - Per Data Dictionary

| Field Name | Data Type | Max Length | Required | Nullable | Validation Rule | Error Code | Error Message |
| --- | --- | --- | --- | --- | --- | --- | --- |
| vehicleNo | varchar | 14 | Conditional | No | Not blank for Type O | OCMS-1401 | Vehicle number is required for On-street (O) offence type |
| vehicleNo | varchar | 14 | Conditional | No | Not blank for Type E | OCMS-1402 | Vehicle number is required for ERP (E) offence type |
| vehicleNo | varchar | 14 | No | No | Uppercase conversion | - | Auto-converted to uppercase |
| sourceProvidedType | varchar | 1 | No | Yes | Must be 'F', 'S', 'D', 'I' or null if provided | - | - |
| offenceType | varchar | 1 | Yes | No | Must be 'O', 'E', or 'U' | - | - |

### 4.2 Format Validations

| Vehicle Type | Format Pattern | Example | Data Type |
| --- | --- | --- | --- |
| Diplomatic | `^S.*CC$` or `^S.*CD$` or `^S.*TC$` or `^S.*TE$` | S123CC, S456CD | varchar(14) |
| Military | Starts/Ends with `MID` or `MINDEF` | MID1234, 1234MID, MINDEF123 | varchar(14) |
| Singapore | LTA Checksum validation pass | SBA1234A, SKM5678B | varchar(14) |
| UPL | Blank, null, 'N.A', or 'UNLICENSED_PARKING' | N.A, UNLICENSED_PARKING | varchar(14) |

---

## 5. Exception Handling

### 5.1 System Exceptions

| Exception Code | Exception Name | Condition | Handling | Result |
| --- | --- | --- | --- | --- |
| SEX001 | LTA Validation Error | LTA library throws exception | Catch exception, log error | Proceed to C005 (Diplomatic check) |
| SEX002 | OCMS Database Error | VIP query fails | Catch exception, log error | Proceed to C008 (Local fallback check) |
| SEX003 | Null Input | vehicleNo is null | Check offence type | Return X (if Type U) or error |

### 5.2 Business Exceptions

| Error Code | Exception Name | Condition | Error Message | HTTP Status |
| --- | --- | --- | --- | --- |
| OCMS-1401 | Blank Vehicle for Type O | vehicleNo blank AND offenceType = 'O' | Vehicle number is required for On-street (O) offence type | 400 |
| OCMS-1402 | Blank Vehicle for Type E | vehicleNo blank AND offenceType = 'E' | Vehicle number is required for ERP (E) offence type | 400 |

### 5.3 Error Response Format

```json
{
  "data": {
    "appCode": "OCMS-1401",
    "message": "Vehicle number is required for On-street (O) offence type"
  }
}
```

---

## 6. Database Operations

### 6.1 Update Operation

| Attribute | Value |
| --- | --- |
| Operation Type | UPDATE |
| Target Table | ocms_valid_offence_notice |
| Timing | After vehicle type determined |
| Transaction | Part of Notice Creation transaction |

### 6.2 Audit User Configuration

| Zone | Field | Value |
| --- | --- | --- |
| Intranet | cre_user_id | ocmsiz_app_conn |
| Intranet | upd_user_id | ocmsiz_app_conn |
| Internet | cre_user_id | ocmsez_app_conn |
| Internet | upd_user_id | ocmsez_app_conn |

**IMPORTANT:** Do NOT use "SYSTEM" for audit user fields.

### 6.3 Insert/Update Order

| Step | Operation | Table | Description |
| --- | --- | --- | --- |
| 1 | INSERT | ocms_valid_offence_notice (VON) | Parent table first |
| 2 | UPDATE | ocms_valid_offence_notice (VON) | Vehicle registration type |
| 3 | INSERT | ocms_offence_notice_dtl (OND) | Child table after parent |

---

## 7. Processing Path

### 7.1 Post-Detection Routing

| Detected Type | Description | Processing Path | Document Reference |
| --- | --- | --- | --- |
| F | Foreign Vehicle | Foreign Vehicle Processing | OCMS 14 TD Section 3 |
| S | Singapore/Local Vehicle | Local Vehicle Processing | OCMS 11 TD |
| D | Diplomatic Vehicle | Diplomatic Vehicle Processing | OCMS 14 TD Section 6 |
| I | Military Vehicle | Military Vehicle Processing | OCMS 14 TD Section 5 |
| V | VIP Vehicle | VIP Vehicle Processing | OCMS 14 TD Section 7 |
| X | UPL Dummy Vehicle | UPL Processing | OCMS 14 TD (UPL section) |

---

## 8. Test Scenarios

### Condition Test Cases

| Test ID | Condition | Test Input | Expected Output | Error Code | Status |
| --- | --- | --- | --- | --- | --- |
| TC001 | C001 - Foreign from source | sourceType='F', vehNo='ABC123' | F | - | - |
| TC002 | C002 - UPL blank vehicle | vehNo=null, offenceType='U' | X | - | - |
| TC003 | C002 - UPL N.A vehicle | vehNo='N.A', offenceType='U' | X | - | - |
| TC004 | C003a - Error blank Type O | vehNo=null, offenceType='O' | ERROR | OCMS-1401 | - |
| TC005 | C003b - Error blank Type E | vehNo=null, offenceType='E' | ERROR | OCMS-1402 | - |
| TC006 | C004 - Valid SG vehicle | vehNo='SBA1234A' | S | - | - |
| TC007 | C005 - Diplomatic CC | vehNo='S123CC' | D | - | - |
| TC008 | C005 - Diplomatic CD | vehNo='S456CD' | D | - | - |
| TC009 | C005 - Diplomatic TC | vehNo='S789TC' | D | - | - |
| TC010 | C005 - Diplomatic TE | vehNo='S012TE' | D | - | - |
| TC011 | C006 - Military MID prefix | vehNo='MID1234' | I | - | - |
| TC012 | C006 - Military MID suffix | vehNo='1234MID' | I | - | - |
| TC013 | C006 - Military MINDEF | vehNo='MINDEF123' | I | - | - |
| TC014 | C007 - VIP vehicle | vehNo='VIP123' (in OCMS with A) | V | - | - |
| TC015 | C007 - Defunct VIP | vehNo='VIP456' (in OCMS with D) | S | - | - |
| TC016 | C008 - Local default | vehNo='ABC123' (not in VIP DB) | S | - | - |
| TC017 | Exception - LTA error | vehNo='ERROR123' (triggers LTA exception) | Continue to C005 | - | - |
| TC018 | Exception - OCMS DB error | vehNo='DBFAIL123' (triggers OCMS exception) | S (Local Default) | - | - |

---

## 9. Changelog

| Version | Date | Author | Changes |
| --- | --- | --- | --- |
| 1.0 | 15/01/2026 | Claude | Initial version based on FD Section 2 and backend code analysis |
| 2.0 | 19/01/2026 | Claude | Revised: Split C003 into C003a/C003b, added OCMS-XXXX error codes, added complete data types, audit user config, database operations, processing path, enhanced test cases |
| 2.1 | 19/01/2026 | Claude | Aligned with Data Dictionary: varchar(14) for vehicle_no, varchar(1) for type fields, corrected nullable settings |
| 2.2 | 27/01/2026 | Claude | Aligned with FD: Removed C009 (Foreign default), updated C008 to Local Default per FD Step 8. After VIP check fails, return 'S' (Local) instead of checking last char letter |

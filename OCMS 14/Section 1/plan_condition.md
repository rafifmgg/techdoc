# Condition Plan: Detecting Vehicle Registration Type

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
| C003 | Blank Vehicle Error | vehicleNo, offenceType | vehicleNo is blank/null AND offenceType IN ('O', 'E') | Return Error |
| C004 | Singapore Vehicle Check | vehicleNo | LTA Checksum validation = true | Return 'S' |
| C005 | Diplomatic Vehicle Check | vehicleNo | Prefix = 'S' AND Suffix IN ('CC', 'CD', 'TC', 'TE') | Return 'D' |
| C006 | Military Vehicle Check | vehicleNo | Prefix/Suffix = 'MID' OR 'MINDEF' | Return 'I' |
| C007 | VIP Vehicle Check | vehicleNo | Found in CAS VIP_VEHICLE with status = 'A' | Return 'V' |
| C008 | Local Vehicle Fallback | vehicleNo | Last character is alphabet | Return 'S' |
| C009 | Foreign Vehicle Default | vehicleNo | No other condition matches | Return 'F' |

---

#### Condition Details

**C001: Foreign Vehicle Check**

| Attribute | Value |
| --- | --- |
| Description | Check if source system marked the vehicle as Foreign |
| Trigger | Start of Vehicle Registration Type Check |
| Input | sourceProvidedType (from source system) |
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
| Logic | vehicleNo is NULL/empty/blank OR equals 'N.A' OR equals 'UNLICENSED_PARKING' |
| Output | Return 'X' (UPL Dummy Vehicle) if offenceType = 'U' |
| Else | Proceed to C003 if offenceType is 'O' or 'E' |

```
IF vehicleNo IS NULL OR vehicleNo = '' OR vehicleNo = 'N.A' OR vehicleNo = 'UNLICENSED_PARKING'
THEN
    IF offenceType = 'U'
    THEN Return 'X'
    ELSE Return Error (C003)
```

---

**C003: Blank Vehicle Error**

| Attribute | Value |
| --- | --- |
| Description | Error condition for blank vehicle number with Type O or E offences |
| Trigger | vehicleNo is blank AND offenceType is 'O' or 'E' |
| Input | vehicleNo, offenceType |
| Logic | vehicleNo is blank AND offenceType IN ('O', 'E') |
| Output | Return error to Notice Creation flow |
| Else | N/A - This is an error condition |

```
IF vehicleNo IS BLANK AND offenceType IN ('O', 'E')
THEN Return Error "Offence Type O & E do not allow blank vehicle number"
```

---

**C004: Singapore Vehicle Check (LTA Validation)**

| Attribute | Value |
| --- | --- |
| Description | Validate vehicle number using LTA Checksum utility |
| Trigger | After C002 passes (vehicle number is not blank) |
| Input | vehicleNo |
| Logic | Call LTA ValidateRegistrationNo.validate(vehicleNo) |
| Output | Return 'S' (Local/Singapore) if validation returns true |
| Else | Proceed to C005 |

```
IF LTA.ValidateRegistrationNo.validate(vehicleNo) = TRUE
THEN Return 'S'
ELSE Proceed to Diplomatic check
```

---

**C005: Diplomatic Vehicle Check**

| Attribute | Value |
| --- | --- |
| Description | Check if vehicle number matches Diplomatic vehicle format |
| Trigger | After C004 fails (not a valid Singapore vehicle) |
| Input | vehicleNo |
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
| Logic | vehicleNo starts with OR ends with 'MID' or 'MINDEF' |
| Output | Return 'I' (Military) if matches |
| Else | Proceed to C007 |

```
IF vehicleNo STARTS WITH 'MID' OR 'MINDEF'
   OR vehicleNo ENDS WITH 'MID' OR 'MINDEF'
THEN Return 'I'
ELSE Proceed to VIP check
```

---

**C007: VIP Vehicle Check**

| Attribute | Value |
| --- | --- |
| Description | Query CAS database to check if vehicle has active VIP Parking Label |
| Trigger | After C006 fails (not a Military vehicle) |
| Input | vehicleNo |
| Logic | Query CAS VIP_VEHICLE table for vehicleNo with status = 'A' |
| Output | Return 'V' (VIP) if found with active status |
| Else | Proceed to C008 |

```
IF EXISTS (SELECT 1 FROM VIP_VEHICLE WHERE vehicle_no = vehicleNo AND status = 'A')
THEN Return 'V'
ELSE Proceed to Local fallback check
```

**VIP Status Transition (Future - FOMS Implementation):**

| Current System (CAS) | Future System (FOMS) |
| --- | --- |
| status = 'A' | status = 'A' AND offence_date BETWEEN start_date AND end_date |
| status = 'D' | status = 'D' OR offence_date NOT BETWEEN start_date AND end_date |

---

**C008: Local Vehicle Fallback**

| Attribute | Value |
| --- | --- |
| Description | Check if last character is alphabet (edge case for Singapore vehicles) |
| Trigger | After C007 fails (not a VIP vehicle) |
| Input | vehicleNo |
| Logic | Last character of vehicleNo is a letter (A-Z) |
| Output | Return 'S' (Local) if matches |
| Else | Proceed to C009 |

```
IF LAST_CHAR(vehicleNo) IS LETTER
THEN Return 'S'
ELSE Proceed to Foreign default
```

---

**C009: Foreign Vehicle Default**

| Attribute | Value |
| --- | --- |
| Description | Default to Foreign vehicle if no other condition matches |
| Trigger | After all other checks fail |
| Input | vehicleNo |
| Logic | No specific pattern matched |
| Output | Return 'F' (Foreign) |
| Else | N/A - This is the final default |

```
IF NO_OTHER_CONDITION_MATCHED
THEN Return 'F'
```

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
  │                 │           └─ NO ──► Return ERROR ─► END
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
  │                                                     └─ NO ──► In VIP DB?
  │                                                                 │
  │                                                                 ├─ YES ─► Return 'V' ─► END
  │                                                                 │
  │                                                                 └─ NO ──► Last char letter?
  │                                                                             │
  │                                                                             ├─ YES ─► Return 'S' ─► END
  │                                                                             │
  │                                                                             └─ NO ──► Return 'F' ─► END
END
```

### Decision Table

| Source='F' | Blank VehNo | Type='U' | LTA Valid | Diplomatic | Military | VIP DB | Last=Letter | Result |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TRUE | - | - | - | - | - | - | - | F |
| FALSE | TRUE | TRUE | - | - | - | - | - | X |
| FALSE | TRUE | FALSE | - | - | - | - | - | ERROR |
| FALSE | FALSE | - | TRUE | - | - | - | - | S |
| FALSE | FALSE | - | FALSE | TRUE | - | - | - | D |
| FALSE | FALSE | - | FALSE | FALSE | TRUE | - | - | I |
| FALSE | FALSE | - | FALSE | FALSE | FALSE | TRUE | - | V |
| FALSE | FALSE | - | FALSE | FALSE | FALSE | FALSE | TRUE | S |
| FALSE | FALSE | - | FALSE | FALSE | FALSE | FALSE | FALSE | F |

---

## 4. Validation Rules

### 4.1 Field Validations

| Field Name | Data Type | Required | Validation Rule | Error Code | Error Message |
| --- | --- | --- | --- | --- | --- |
| vehicleNo | String | Conditional | Not blank for Type O/E | ERR001 | Offence Type O & E do not allow blank vehicle number |
| vehicleNo | String | No | Uppercase conversion | - | Auto-converted to uppercase |
| sourceProvidedType | String | No | Must be 'F', 'S', 'D', or 'I' if provided | - | - |
| offenceType | String | Yes | Must be 'O', 'E', or 'U' | - | - |

### 4.2 Format Validations

| Vehicle Type | Format Pattern | Example |
| --- | --- | --- |
| Diplomatic | `^S.*CC$` or `^S.*CD$` or `^S.*TC$` or `^S.*TE$` | S123CC, S456CD |
| Military | Starts/Ends with `MID` or `MINDEF` | MID1234, 1234MID, MINDEF123 |
| Singapore | LTA Checksum validation pass | SBA1234A, SKM5678B |
| UPL | Blank, null, 'N.A', or 'UNLICENSED_PARKING' | N.A, UNLICENSED_PARKING |

---

## 5. Exception Handling

### 5.1 System Exceptions

| Exception Code | Exception Name | Condition | Handling |
| --- | --- | --- | --- |
| SEX001 | LTA Validation Error | LTA library throws exception | Catch exception, proceed to next check |
| SEX002 | CAS Database Error | VIP query fails | Default to non-VIP, log error |
| SEX003 | Null Input | vehicleNo is null | Check offence type, return X or error |

### 5.2 Business Exceptions

| Exception Code | Exception Name | Condition | Handling |
| --- | --- | --- | --- |
| BEX001 | Blank Vehicle for Type O/E | vehicleNo blank AND offenceType IN ('O','E') | Return error to Notice Creation |

---

## 6. Test Scenarios

### Condition Test Cases

| Test ID | Condition | Test Input | Expected Output | Status |
| --- | --- | --- | --- | --- |
| TC001 | C001 - Foreign from source | sourceType='F', vehNo='ABC123' | F | - |
| TC002 | C002 - UPL blank vehicle | vehNo=null, offenceType='U' | X | - |
| TC003 | C002 - UPL N.A vehicle | vehNo='N.A', offenceType='U' | X | - |
| TC004 | C003 - Error blank Type O | vehNo=null, offenceType='O' | ERROR | - |
| TC005 | C004 - Valid SG vehicle | vehNo='SBA1234A' | S | - |
| TC006 | C005 - Diplomatic CC | vehNo='S123CC' | D | - |
| TC007 | C005 - Diplomatic CD | vehNo='S456CD' | D | - |
| TC008 | C005 - Diplomatic TC | vehNo='S789TC' | D | - |
| TC009 | C005 - Diplomatic TE | vehNo='S012TE' | D | - |
| TC010 | C006 - Military MID prefix | vehNo='MID1234' | I | - |
| TC011 | C006 - Military MID suffix | vehNo='1234MID' | I | - |
| TC012 | C006 - Military MINDEF | vehNo='MINDEF123' | I | - |
| TC013 | C007 - VIP vehicle | vehNo='VIP123' (in CAS with A) | V | - |
| TC014 | C007 - Defunct VIP | vehNo='VIP456' (in CAS with D) | S or F | - |
| TC015 | C008 - Local fallback | vehNo='ABC123X' (ends with letter) | S | - |
| TC016 | C009 - Foreign default | vehNo='12345' (no match) | F | - |

---

## 7. Changelog

| Version | Date | Author | Changes |
| --- | --- | --- | --- |
| 1.0 | 15/01/2026 | Claude | Initial version based on FD Section 2 and backend code analysis |

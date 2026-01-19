# Flowchart Plan: Detecting Vehicle Registration Type

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

## 1. Diagram Sections (Tabs)

The Technical Flowchart will contain the following tabs/sections:

| Tab # | Tab Name | Description | Priority |
| --- | --- | --- | --- |
| 1 | Section_1_High_Level | High-level overview of Vehicle Registration Type Check | High |
| 2 | Section_1_Vehicle_Reg_Type_Check | Detailed flow of vehicle registration type detection | High |

---

## 2. Systems Involved (Swimlanes)

| System/Tier | Color Code | Hex | Description |
| --- | --- | --- | --- |
| Notice Creation Flow | Light Blue | #dae8fc | Parent flow that calls this function |
| Backend API | Light Green | #d5e8d4 | OCMS Admin API processing |
| External System (LTA) | Light Yellow | #fff2cc | LTA Checksum validation library |
| External System (CAS) | Light Yellow | #fff2cc | CAS VIP Vehicle database |
| Database (Intranet) | Light Yellow | #fff2cc | Intranet database operations |

---

## 3. Tab 1: Section_1_High_Level

### 3.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | Vehicle Registration Type Check - High Level |
| Section | 1.1 |
| Trigger | Notice Creation process requests vehicle type identification |
| Frequency | Real-time (per notice creation) |
| Systems Involved | Backend API, LTA Library, CAS Database |
| Expected Outcome | Return vehicle registration type code (F/S/D/I/V/X) |

### 3.2 High Level Flow Diagram (ASCII)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         NOTICE CREATION FLOW                             │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│    ┌───────────┐                                                         │
│    │   Start   │                                                         │
│    └─────┬─────┘                                                         │
│          │                                                               │
│          ▼                                                               │
│    ┌─────────────────────────────────────────────────────────────────┐  │
│    │              FUNCTION: Check Vehicle Registration Type          │  │
│    │                                                                  │  │
│    │   Input: vehicleNo, sourceProvidedType, offenceType             │  │
│    │   Output: vehicleRegistrationType (F/S/D/I/V/X)                 │  │
│    │                                                                  │  │
│    │   Checks performed:                                              │  │
│    │   1. Source provided Foreign check                               │  │
│    │   2. UPL/Blank vehicle check                                     │  │
│    │   3. LTA Checksum validation (Singapore)                         │  │
│    │   4. Diplomatic format check                                     │  │
│    │   5. Military format check                                       │  │
│    │   6. VIP database lookup                                         │  │
│    │   7. Local fallback / Foreign default                            │  │
│    └─────────────────────────────────────────────────────────────────┘  │
│          │                                                               │
│          ▼                                                               │
│    ┌─────────────────────┐                                              │
│    │ Return to Notice    │                                              │
│    │ Creation with       │                                              │
│    │ vehicle_reg_type    │                                              │
│    └─────────┬───────────┘                                              │
│              │                                                           │
│              ▼                                                           │
│    ┌───────────┐                                                         │
│    │    End    │                                                         │
│    └───────────┘                                                         │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 3.3 Process Steps Table (High Level)

| Step | Definition | Brief Description |
| --- | --- | --- |
| Start | Entry point | Notice Creation flow initiates vehicle type check |
| Check Vehicle Registration Type | Function call | Determine vehicle type based on input parameters |
| Return Result | Output | Return vehicle_registration_type to Notice Creation |
| End | Exit point | Function complete, continue Notice Creation |

---

## 4. Tab 2: Section_1_Vehicle_Reg_Type_Check

### 4.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | Vehicle Registration Type Check - Detailed Flow |
| Section | 1.2 |
| Trigger | checkVehregistration() method called |
| Frequency | Real-time (per notice) |
| Systems Involved | Backend API, LTA Library, CAS Database |
| Expected Outcome | Return appropriate vehicle type code |

### 4.2 Detailed Flow Diagram (ASCII)

```
┌──────────────────┬────────────────────┬────────────────────┬──────────────┐
│   Backend API    │   LTA Library      │   CAS Database     │   Result     │
├──────────────────┼────────────────────┼────────────────────┼──────────────┤
│                  │                    │                    │              │
│ ┌──────────────┐ │                    │                    │              │
│ │    Start     │ │                    │                    │              │
│ └──────┬───────┘ │                    │                    │              │
│        │         │                    │                    │              │
│        ▼         │                    │                    │              │
│ ┌──────────────┐ │                    │                    │              │
│ │Source = 'F'? │ │                    │                    │              │
│ └──────┬───────┘ │                    │                    │              │
│   Yes/ │ \No     │                    │                    │              │
│       │  │       │                    │                    │              │
│  ┌────┘  └────┐  │                    │                    │              │
│  │            │  │                    │                    │              │
│  ▼            ▼  │                    │                    │              │
│ Return 'F'   ┌──────────────┐         │                    │              │
│  ────────►   │VehNo blank?  │         │                    │              │
│              └──────┬───────┘         │                    │              │
│                Yes/ │ \No             │                    │              │
│                    │  │               │                    │              │
│               ┌────┘  └────┐          │                    │              │
│               │            │          │                    │              │
│               ▼            ▼          │                    │              │
│        ┌────────────┐  ┌──────────────────────────────┐    │              │
│        │Type = 'U'? │  │                              │    │              │
│        └──────┬─────┘  │   LTA Checksum Validation    │───►│              │
│          Yes/ │ \No    │   validate(vehicleNo)        │    │              │
│              │  │      │                              │    │              │
│         ┌────┘  │      └──────────────┬───────────────┘    │              │
│         │       │                     │                    │              │
│         ▼       ▼               Valid/│\Invalid            │              │
│    Return 'X'  Return              ┌──┘ └──┐               │              │
│     ────────►  ERROR               │       │               │              │
│                ────────►           ▼       ▼               │              │
│                              Return 'S' ┌──────────────┐   │              │
│                               ────────► │Is Diplomatic?│   │              │
│                                         └──────┬───────┘   │              │
│                                           Yes/ │ \No       │              │
│                                               │  │         │              │
│                                          ┌────┘  └────┐    │              │
│                                          │            │    │              │
│                                          ▼            ▼    │              │
│                                    Return 'D'  ┌──────────────┐           │
│                                     ────────►  │Is Military?  │           │
│                                                └──────┬───────┘           │
│                                                  Yes/ │ \No               │
│                                                      │  │                 │
│                                                 ┌────┘  └────┐            │
│                                                 │            │            │
│                                                 ▼            ▼            │
│                                           Return 'I'   ┌─────────────────┐│
│                                            ────────►   │Query VIP_VEHICLE││
│                                                        │                 ││
│                                                        └────────┬────────┘│
│                                                           Found/│\Not     │
│                                                                │  Found   │
│                                                           ┌────┘  └────┐  │
│                                                           │            │  │
│                                                           ▼            ▼  │
│                                                     Return 'V'  ┌─────────┐
│                                                      ────────►  │Last char│
│                                                                 │letter?  │
│                                                                 └────┬────┘
│                                                                 Yes/ │ \No │
│                                                                     │  │   │
│                                                                ┌────┘  └───┐
│                                                                │           │
│                                                                ▼           ▼
│                                                          Return 'S'  Return 'F'
│                                                           ────────►   ────────►
│                                                                              │
│                                                                              │
│ ┌──────────────┐                                                             │
│ │     End      │◄────────────────────────────────────────────────────────────┘
│ └──────────────┘
│
└───────────────────────────────────────────────────────────────────────────────
```

### 4.3 Process Steps Table (Detailed)

| Step | Type | Definition | Brief Description | Next Step |
| --- | --- | --- | --- | --- |
| 1 | Start | Start Vehicle Reg Type Check | Function receives vehicleNo, sourceProvidedType, offenceType | 2 |
| 2 | Decision | Source = 'F'? | Check if source system marked as Foreign | Yes→3, No→4 |
| 3 | Process | Return 'F' | Return Foreign vehicle type | End |
| 4 | Decision | VehNo blank/null? | Check if vehicle number is blank, null, or 'N.A' | Yes→5, No→7 |
| 5 | Decision | OffenceType = 'U'? | Check if offence type is UPL | Yes→6, No→E1 |
| 6 | Process | Return 'X' | Return UPL Dummy vehicle type | End |
| 7 | Process | Call LTA Checksum | Validate vehicle number using LTA library | 8 |
| 8 | Decision | LTA Valid? | Check if LTA validation returns true | Yes→9, No→10 |
| 9 | Process | Return 'S' | Return Singapore/Local vehicle type | End |
| 10 | Decision | Is Diplomatic format? | Check prefix 'S' + suffix CC/CD/TC/TE | Yes→11, No→12 |
| 11 | Process | Return 'D' | Return Diplomatic vehicle type | End |
| 12 | Decision | Is Military format? | Check prefix/suffix MID or MINDEF | Yes→13, No→14 |
| 13 | Process | Return 'I' | Return Military vehicle type | End |
| 14 | Process | Query CAS VIP_VEHICLE | Query database for active VIP status | 15 |
| 15 | Decision | Found in VIP DB? | Check if vehicle exists with status = 'A' | Yes→16, No→17 |
| 16 | Process | Return 'V' | Return VIP vehicle type | End |
| 17 | Decision | Last char is letter? | Check if last character is alphabet | Yes→18, No→19 |
| 18 | Process | Return 'S' | Return Singapore/Local (fallback) | End |
| 19 | Process | Return 'F' | Return Foreign (default) | End |
| E1 | Error | Return Error | Blank vehicle not allowed for Type O/E | End |
| End | End | End | Function returns result to caller | - |

### 4.4 Decision Logic

| ID | Decision | Input | Condition | Yes Action | No Action |
| --- | --- | --- | --- | --- | --- |
| D1 | Source = 'F'? | sourceProvidedType | sourceProvidedType == 'F' | Return 'F' | Go to D2 |
| D2 | VehNo blank? | vehicleNo | vehicleNo is null/empty/'N.A' | Go to D3 | Go to Step 7 |
| D3 | OffenceType = 'U'? | offenceType | offenceType == 'U' | Return 'X' | Return Error |
| D4 | LTA Valid? | LTA result | ValidateRegistrationNo.validate() == true | Return 'S' | Go to D5 |
| D5 | Is Diplomatic? | vehicleNo | Matches ^S.*(CC\|CD\|TC\|TE)$ | Return 'D' | Go to D6 |
| D6 | Is Military? | vehicleNo | Starts/Ends with MID or MINDEF | Return 'I' | Go to Step 14 |
| D7 | Found in VIP DB? | Query result | EXISTS in VIP_VEHICLE with status='A' | Return 'V' | Go to D8 |
| D8 | Last char letter? | vehicleNo | Last character is A-Z | Return 'S' | Return 'F' |

### 4.5 Database Operations

| Step | Operation | Database | Table | Query/Action |
| --- | --- | --- | --- | --- |
| 14 | SELECT | CAS | VIP_VEHICLE | `SELECT 1 FROM VIP_VEHICLE WHERE vehicle_no = ? AND status = 'A'` |

### 4.6 External System Calls

| Step | System | Call | Input | Output |
| --- | --- | --- | --- | --- |
| 7 | LTA Library | ValidateRegistrationNo.validate() | vehicleNo (String) | boolean (true/false) |

---

## 5. Error Handling

| Error Point | Error Type | Condition | Handling | Recovery |
| --- | --- | --- | --- | --- |
| Step 5 (E1) | Business Error | Blank vehNo + Type O/E | Return error to caller | Notice creation fails |
| Step 7 | System Error | LTA library exception | Catch exception, return false | Proceed to next check |
| Step 14 | System Error | CAS DB unavailable | Catch exception, return false | Proceed to next check |

---

## 6. Output Mapping

### 6.1 Return Values

| Code | Vehicle Type | Stored In | Description |
| --- | --- | --- | --- |
| F | Foreign | ocms_valid_offence_notice.vehicle_registration_type | Foreign registered vehicle |
| S | Singapore/Local | ocms_valid_offence_notice.vehicle_registration_type | Singapore registered vehicle |
| D | Diplomatic | ocms_valid_offence_notice.vehicle_registration_type | Diplomatic vehicle |
| I | Military | ocms_valid_offence_notice.vehicle_registration_type | Military (MID/MINDEF) vehicle |
| V | VIP | ocms_valid_offence_notice.vehicle_registration_type | VIP parking label holder |
| X | UPL | ocms_valid_offence_notice.vehicle_registration_type | UPL dummy vehicle |

### 6.2 Data Flow to Next Process

After vehicle registration type is determined:

| Vehicle Type | Next Processing Flow | Reference |
| --- | --- | --- |
| F | Foreign Vehicle Processing | OCMS 14 Section 3 |
| S | Local Vehicle Processing | OCMS 11 |
| D | Diplomatic Vehicle Processing | OCMS 14 Section 6 |
| I | Military Vehicle Processing | OCMS 14 Section 5 |
| V | VIP Vehicle Processing | OCMS 14 Section 7 |
| X | UPL Processing | OCMS 14 |

---

## 7. Flowchart Checklist

Before creating the technical flowchart:

- [x] All steps have clear names
- [x] All decision points have Yes/No paths defined
- [x] All paths lead to an End point
- [x] Error handling paths are included
- [x] Database operations are identified
- [x] Swimlanes are defined for each system/tier
- [x] Color coding is specified
- [x] Step descriptions are complete
- [x] External system calls are documented
- [x] Return values are mapped

---

## 8. Notes for Technical Flowchart Creation

### 8.1 Shape Guidelines

| Element | Shape | Style |
| --- | --- | --- |
| Start/End | Terminator (rounded rectangle) | strokeWidth=2 |
| Process | Rectangle | rounded=1, arcSize=14 |
| Decision | Diamond | shape=mxgraph.flowchart.decision |
| Database | Cylinder | shape=cylinder |
| External Call | Rectangle with double border | shape=process |

### 8.2 Connector Guidelines

| Connection Type | Style |
| --- | --- |
| Normal flow | Solid arrow |
| Database operation | Dashed line |
| External system call | Dashed line |
| Error path | Red solid arrow |

### 8.3 Label Guidelines

| Decision | Yes Label | No Label |
| --- | --- | --- |
| Source = 'F'? | Yes (Foreign) | No |
| VehNo blank? | Yes (Blank) | No (Has value) |
| Type = 'U'? | Yes (UPL) | No (Type O/E) |
| LTA Valid? | Yes (Valid) | No (Invalid) |
| Is Diplomatic? | Yes (Diplomatic) | No |
| Is Military? | Yes (Military) | No |
| VIP found? | Yes (VIP) | No (Not found) |
| Last char letter? | Yes (Letter) | No (Number) |

---

## 9. Changelog

| Version | Date | Author | Changes |
| --- | --- | --- | --- |
| 1.0 | 15/01/2026 | Claude | Initial version based on plan_api.md and plan_condition.md |

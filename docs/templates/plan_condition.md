# Condition Plan: [Feature Name]

<!--
Template for: Business Condition and Logic Planning
Use this template when planning business rules, conditions, and decision logic.

NOTE: Use case and section structure should follow the Functional Document.
When creating the Technical Document, reference the related Functional Document.
-->

## Overview

| Attribute | Value |
| --- | --- |
| Feature Name | [Feature Name] |
| Version | v1.0 |
| Author | [Author Name] |
| Created Date | [DD/MM/YYYY] |
| Last Updated | [DD/MM/YYYY] |
| Status | Draft / In Review / Approved |
| Functional Document | [FD Reference Number] |
| Technical Document | OCMS [X] Technical Doc |

---

## 1. Reference Documents

| Document Type | Reference | Description |
| --- | --- | --- |
| Functional Document | [FD-XXX] | [Document title] |
| Technical Document | [OCMS-XXX] | [Document title] |
| Data Dictionary | [DD-XXX] | [Document title] |

---

## 2. Business Conditions

### 2.1 [Condition Category Name]

**Description:** [Brief description of this condition category]

**Reference:** Functional Document Section [X.X]

#### Condition Matrix

| Condition ID | Condition Name | Input Fields | Logic | Output/Action |
| --- | --- | --- | --- | --- |
| C001 | [Name] | [field_1, field_2] | [Logic description] | [Action to take] |
| C002 | [Name] | [field_1, field_3] | [Logic description] | [Action to take] |
| C003 | [Name] | [field_2, field_4] | [Logic description] | [Action to take] |

#### Condition Details

**C001: [Condition Name]**

| Attribute | Value |
| --- | --- |
| Description | [Detailed description] |
| Trigger | [When this condition is checked] |
| Input | [Input fields/values] |
| Logic | [Detailed logic/formula] |
| Output | [Result when condition is TRUE] |
| Else | [Result when condition is FALSE] |

```
IF [condition_expression]
THEN [action_1]
ELSE [action_2]
```

**C002: [Condition Name]**

| Attribute | Value |
| --- | --- |
| Description | [Detailed description] |
| Trigger | [When this condition is checked] |
| Input | [Input fields/values] |
| Logic | [Detailed logic/formula] |
| Output | [Result when condition is TRUE] |
| Else | [Result when condition is FALSE] |

---

### 2.2 [Condition Category Name 2]

<!-- Repeat structure as needed -->

---

## 3. Decision Tree

### 3.1 [Process Name] Decision Flow

```
START
  │
  ├─► Check Condition A?
  │     │
  │     ├─ YES ─► Action A1
  │     │           │
  │     │           └─► Check Condition B?
  │     │                 │
  │     │                 ├─ YES ─► Action B1 ─► END
  │     │                 │
  │     │                 └─ NO ──► Action B2 ─► END
  │     │
  │     └─ NO ──► Check Condition C?
  │                 │
  │                 ├─ YES ─► Action C1 ─► END
  │                 │
  │                 └─ NO ──► Action C2 ─► END
  │
END
```

### Decision Table

| Condition A | Condition B | Condition C | Result Action |
| --- | --- | --- | --- |
| TRUE | TRUE | - | Action B1 |
| TRUE | FALSE | - | Action B2 |
| FALSE | - | TRUE | Action C1 |
| FALSE | - | FALSE | Action C2 |

---

## 4. Validation Rules

### 4.1 Field Validations

| Field Name | Data Type | Required | Validation Rule | Error Code | Error Message |
| --- | --- | --- | --- | --- | --- |
| [field_name] | string | Yes | Not empty, max 100 chars | VAL001 | [Field] is required |
| [field_name] | integer | Yes | Range: 0-9999 | VAL002 | [Field] must be between 0-9999 |
| [field_name] | date | Yes | Format: YYYY-MM-DD, not future | VAL003 | [Field] must be valid date |
| [field_name] | enum | Yes | Values: [A, B, C] | VAL004 | [Field] must be A, B, or C |
| [field_name] | string | No | Pattern: [regex] | VAL005 | [Field] format invalid |

### 4.2 Cross-Field Validations

| Validation ID | Fields Involved | Rule | Error Code | Error Message |
| --- | --- | --- | --- | --- |
| XF001 | [field_1, field_2] | field_1 < field_2 | XF001 | [field_1] must be less than [field_2] |
| XF002 | [field_3, field_4] | If field_3 = 'A' then field_4 required | XF002 | [field_4] required when [field_3] is A |
| XF003 | [field_5, field_6] | field_5 + field_6 <= 100 | XF003 | Total must not exceed 100 |

### 4.3 Business Rule Validations

| Rule ID | Rule Name | Description | Condition | Error Code | Error Message |
| --- | --- | --- | --- | --- | --- |
| BR001 | [Rule Name] | [Description] | [Condition to check] | BR001 | [Error message] |
| BR002 | [Rule Name] | [Description] | [Condition to check] | BR002 | [Error message] |
| BR003 | [Rule Name] | [Description] | [Condition to check] | BR003 | [Error message] |

---

## 5. Status Transitions

### 5.1 [Entity Name] Status Flow

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  ┌──────────┐    Action A    ┌──────────┐    Action B      │
│  │  DRAFT   │ ─────────────► │ PENDING  │ ───────────────► │
│  └──────────┘                └──────────┘                   │
│       │                           │                         │
│       │ Action X                  │ Action C                │
│       ▼                           ▼                         │
│  ┌──────────┐                ┌──────────┐    Action D      │
│  │ CANCELLED│                │ APPROVED │ ───────────────► │
│  └──────────┘                └──────────┘                   │
│                                   │                         │
│                                   │ Action E                │
│                                   ▼                         │
│                              ┌──────────┐                   │
│                              │ COMPLETED│                   │
│                              └──────────┘                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Status Transition Matrix

| From Status | To Status | Action/Trigger | Condition | Allowed Roles |
| --- | --- | --- | --- | --- |
| DRAFT | PENDING | Submit | All required fields filled | User, Admin |
| DRAFT | CANCELLED | Cancel | - | User, Admin |
| PENDING | APPROVED | Approve | Validation passed | Manager, Admin |
| PENDING | REJECTED | Reject | - | Manager, Admin |
| APPROVED | COMPLETED | Complete | Process finished | System, Admin |

### Status Definitions

| Status | Code | Description | Next Possible Status |
| --- | --- | --- | --- |
| DRAFT | D | Initial state, editable | PENDING, CANCELLED |
| PENDING | P | Waiting for approval | APPROVED, REJECTED |
| APPROVED | A | Approved, processing | COMPLETED |
| REJECTED | R | Rejected, requires revision | DRAFT |
| CANCELLED | X | Cancelled, terminal state | - |
| COMPLETED | C | Completed, terminal state | - |

---

## 6. Calculation Formulas

### 6.1 [Calculation Name]

| Attribute | Value |
| --- | --- |
| Description | [What this calculation does] |
| Formula | `result = (field_1 * field_2) / field_3` |
| Input Fields | field_1, field_2, field_3 |
| Output Field | result |
| Precision | 2 decimal places |
| Rounding | Round half up |

**Example:**
```
Input:  field_1 = 100, field_2 = 0.15, field_3 = 2
Output: result = (100 * 0.15) / 2 = 7.50
```

### 6.2 [Calculation Name 2]

| Attribute | Value |
| --- | --- |
| Description | [What this calculation does] |
| Formula | `result = field_1 + field_2 - field_3` |
| Input Fields | field_1, field_2, field_3 |
| Output Field | result |

---

## 7. Condition-Action Mapping

### 7.1 [Process Name]

| Scenario | Conditions | Actions | Database Updates |
| --- | --- | --- | --- |
| Scenario 1 | C1=TRUE, C2=TRUE | Action A, Action B | Update table_x.field_1 = 'value' |
| Scenario 2 | C1=TRUE, C2=FALSE | Action C | Update table_x.field_2 = 'value' |
| Scenario 3 | C1=FALSE, C3=TRUE | Action D, Action E | Insert into table_y |
| Scenario 4 | C1=FALSE, C3=FALSE | Action F | No update |

### Detailed Scenarios

**Scenario 1: [Scenario Name]**

- **Trigger:** [When this scenario occurs]
- **Conditions:**
  - C1: [Condition 1 description] = TRUE
  - C2: [Condition 2 description] = TRUE
- **Actions:**
  1. [Action A description]
  2. [Action B description]
- **Database Updates:**
  ```sql
  UPDATE table_x SET field_1 = 'value' WHERE id = :id
  ```
- **Expected Result:** [What should happen]

---

## 8. Exception Handling

### 8.1 Business Exceptions

| Exception Code | Exception Name | Condition | Handling |
| --- | --- | --- | --- |
| BEX001 | [Exception Name] | [When this occurs] | [How to handle] |
| BEX002 | [Exception Name] | [When this occurs] | [How to handle] |
| BEX003 | [Exception Name] | [When this occurs] | [How to handle] |

### 8.2 System Exceptions

| Exception Code | Exception Name | Condition | Handling |
| --- | --- | --- | --- |
| SEX001 | Database Error | DB connection failed | Retry 3 times, then log and notify |
| SEX002 | Timeout | Process exceeds [X] seconds | Cancel and rollback |
| SEX003 | External Service Error | API call failed | Retry with backoff, then fail gracefully |

---

## 9. Test Scenarios

### Condition Test Cases

| Test ID | Condition | Test Input | Expected Output | Status |
| --- | --- | --- | --- | --- |
| TC001 | C001 - TRUE path | [input values] | [expected result] | - |
| TC002 | C001 - FALSE path | [input values] | [expected result] | - |
| TC003 | C002 - TRUE path | [input values] | [expected result] | - |
| TC004 | C002 - FALSE path | [input values] | [expected result] | - |
| TC005 | Combined C001+C002 | [input values] | [expected result] | - |

### Edge Cases

| Test ID | Scenario | Test Input | Expected Output |
| --- | --- | --- | --- |
| EC001 | Null value handling | field_1 = null | [expected behavior] |
| EC002 | Boundary value - min | field_1 = 0 | [expected behavior] |
| EC003 | Boundary value - max | field_1 = 9999 | [expected behavior] |
| EC004 | Invalid format | field_1 = 'abc' | Validation error |

---

## 10. Changelog

| Version | Date | Author | Changes |
| --- | --- | --- | --- |
| 1.0 | [DD/MM/YYYY] | [Author] | Initial version |
| 1.1 | [DD/MM/YYYY] | [Author] | [Changes description] |

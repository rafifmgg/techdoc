# Condition Plan: OCMS 16 - Reduction

## Overview

| Attribute | Value |
| --- | --- |
| Feature Name | Reduction |
| Version | v1.0 |
| Author | Claude |
| Created Date | 18/01/2026 |
| Last Updated | 18/01/2026 |
| Status | Draft |
| Functional Document | OCMS 16 Functional Document v1.3 |
| Technical Document | OCMS 16 Technical Doc |
| Source | Functional Document, Backend Code, Functional Flowchart |

---

## 1. Reference Documents

| Document Type | Reference | Description |
| --- | --- | --- |
| Functional Document | OCMS 16 FD v1.3 | Reduction Functional Specification |
| Functional Flowchart | OCMS16-Functional-Flowchart.drawio | High Level Reduction & Backend Reduction flows |
| Backend Code | ReductionController.java, ReductionValidator.java, ReductionRuleService.java | Implementation reference |

---

## 2. Business Conditions

### 2.1 Request Format Validation

**Description:** Validate that the API request data is complete and has valid structure.

**Reference:** Functional Document Section 3.2 - Step 3, Functional Flowchart Backend Reduction

#### Condition Matrix

| Condition ID | Condition Name | Input Fields | Logic | Output/Action |
| --- | --- | --- | --- | --- |
| C001 | Data Format Valid | All request fields | Check JSON structure and data types are correct | Continue to next validation |
| C002 | Mandatory Data Present | noticeNo, amountReduced, amountPayable, dateOfReduction, expiryDateOfReduction, reasonOfReduction, authorisedOfficer, suspensionSource | All mandatory fields are not null/blank | Continue to next validation |

#### Condition Details

**C001: Data Format Valid**

| Attribute | Value |
| --- | --- |
| Description | Validates that the request JSON structure is correct and all fields have valid data types |
| Trigger | When OCMS receives reduction request from PLUS |
| Input | Complete request body |
| Logic | JSON parseable, fields match expected types (string, decimal, datetime) |
| Output | Proceed to mandatory data check |
| Else | Return HTTP 400 with message "Invalid format" |

```
IF request is valid JSON AND all field types are correct
THEN proceed to C002
ELSE return "Invalid format"
```

**C002: Mandatory Data Present**

| Attribute | Value |
| --- | --- |
| Description | Validates that all required fields are provided and not empty |
| Trigger | After format validation passes |
| Input | noticeNo, amountReduced, amountPayable, dateOfReduction, expiryDateOfReduction, reasonOfReduction, authorisedOfficer, suspensionSource |
| Logic | All mandatory fields are NOT NULL and NOT BLANK |
| Output | Proceed to notice lookup |
| Else | Return HTTP 400 with message "Missing data" |

```
IF noticeNo IS NOT BLANK
   AND amountReduced IS NOT NULL
   AND amountPayable IS NOT NULL
   AND dateOfReduction IS NOT NULL
   AND expiryDateOfReduction IS NOT NULL
   AND reasonOfReduction IS NOT BLANK
   AND authorisedOfficer IS NOT BLANK
   AND suspensionSource IS NOT BLANK
THEN proceed to notice lookup
ELSE return "Missing data"
```

---

### 2.2 Notice Existence Validation

**Description:** Validate that the notice exists in OCMS database.

**Reference:** Functional Flowchart Backend Reduction

#### Condition Details

**C003: Notice Exists**

| Attribute | Value |
| --- | --- |
| Description | Check if notice number exists in ocms_valid_offence_notice table |
| Trigger | After mandatory data validation passes |
| Input | noticeNo |
| Logic | SELECT * FROM ocms_valid_offence_notice WHERE notice_no = :noticeNo |
| Output | Proceed to payment status check |
| Else | Return HTTP 404 with message "Notice not found" |

```
IF notice exists in database
THEN proceed to payment status check
ELSE return "Notice not found"
```

---

### 2.3 Payment Status Validation

**Description:** Validate that the notice has not been paid.

**Reference:** Functional Document Section 3.2 - Step 5, Functional Flowchart Backend Reduction

#### Condition Details

**C004: Notice Not Paid**

| Attribute | Value |
| --- | --- |
| Description | Check if notice has already been paid by examining CRS Reason of Suspension field |
| Trigger | After notice is found in database |
| Input | crs_reason_of_suspension from ocms_valid_offence_notice |
| Logic | crs_reason_of_suspension is NULL or BLANK (not "FP" or "PRA") |
| Output | Proceed to eligibility check |
| Else | Return HTTP 409 with message "Notice has been paid" |

```
IF crs_reason_of_suspension IS NULL OR crs_reason_of_suspension IS BLANK
THEN notice is outstanding, proceed to eligibility check
ELSE IF crs_reason_of_suspension = "FP" OR crs_reason_of_suspension = "PRA"
THEN return "Notice has been paid"
```

**Payment Status Values:**

| Value | Meaning | Can Reduce? |
| --- | --- | --- |
| NULL/BLANK | Notice is outstanding | Yes |
| FP | Full Payment made | No |
| PRA | Paid (PRA status) | No |

---

### 2.4 Eligibility Validation

**Description:** Validate that the notice is eligible for reduction based on Computer Rule Code and Last Processing Stage.

**Reference:** Functional Document Section 3.2 - Steps 6 & 7, Functional Flowchart Backend Reduction

#### Eligible Computer Rule Codes

| Code | Description |
| --- | --- |
| 30305 | Eligible for reduction at extended stages |
| 31302 | Eligible for reduction at extended stages |
| 30302 | Eligible for reduction at extended stages |
| 21300 | Eligible for reduction at extended stages |

#### Eligible Last Processing Stages

**For Eligible Computer Rule Codes (30305, 31302, 30302, 21300):**

| Stage | Description |
| --- | --- |
| NPA | Notice Pending Action |
| ROV | Revival |
| ENA | Enforcement Action |
| RD1 | Reminder 1 |
| RD2 | Reminder 2 |
| RR3 | Reminder 3 |
| DN1 | Demand Notice 1 |
| DN2 | Demand Notice 2 |
| DR3 | Demand Reminder 3 |

**For Non-Eligible Computer Rule Codes (Special Case):**

| Stage | Description |
| --- | --- |
| RR3 | Reminder 3 (special case) |
| DR3 | Demand Reminder 3 (special case) |

#### Condition Details

**C005: Computer Rule Code Eligibility**

| Attribute | Value |
| --- | --- |
| Description | Check if the notice's computer rule code is in the eligible list |
| Trigger | After payment status check passes |
| Input | computer_rule_code from ocms_valid_offence_notice |
| Logic | computer_rule_code IN (30305, 31302, 30302, 21300) |
| Output | If YES, check extended stage list (C006). If NO, check special case stages (C007) |

```
IF computer_rule_code IN (30305, 31302, 30302, 21300)
THEN check C006 (extended stage list)
ELSE check C007 (special case - RR3/DR3 only)
```

**C006: Last Processing Stage Eligibility (Extended List)**

| Attribute | Value |
| --- | --- |
| Description | For eligible computer rule codes, check if last processing stage is in the extended eligible list |
| Trigger | When computer_rule_code IS in eligible list |
| Input | last_processing_stage from ocms_valid_offence_notice |
| Logic | last_processing_stage IN (NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3) |
| Output | Proceed to amount validation |
| Else | Return HTTP 409 with message "Notice is not eligible" |

```
IF last_processing_stage IN ('NPA', 'ROV', 'ENA', 'RD1', 'RD2', 'RR3', 'DN1', 'DN2', 'DR3')
THEN notice is ELIGIBLE, proceed to amount validation
ELSE return "Notice is not eligible"
```

**C007: Last Processing Stage Eligibility (Special Case)**

| Attribute | Value |
| --- | --- |
| Description | For non-eligible computer rule codes, only RR3 and DR3 stages are allowed |
| Trigger | When computer_rule_code IS NOT in eligible list |
| Input | last_processing_stage from ocms_valid_offence_notice |
| Logic | last_processing_stage IN (RR3, DR3) |
| Output | Proceed to amount validation |
| Else | Return HTTP 409 with message "Notice is not eligible" |

```
IF last_processing_stage IN ('RR3', 'DR3')
THEN notice is ELIGIBLE (special case), proceed to amount validation
ELSE return "Notice is not eligible"
```

---

### 2.5 Amount Validation

**Description:** Validate that the reduction amounts are logically correct.

**Reference:** Backend Code - ReductionValidator.java

#### Condition Details

**C008: Amount Reduced Valid**

| Attribute | Value |
| --- | --- |
| Description | Validate that amount reduced is positive and not greater than original composition amount |
| Trigger | After eligibility check passes |
| Input | amountReduced (request), composition_amount (database) |
| Logic | amountReduced > 0 AND amountReduced <= composition_amount |
| Output | Proceed to amount payable check |
| Else | Return HTTP 400 with message "Invalid format" |

```
IF amountReduced > 0 AND amountReduced <= composition_amount
THEN proceed to C009
ELSE return "Invalid format"
```

**C009: Amount Payable Consistent**

| Attribute | Value |
| --- | --- |
| Description | Validate that amount payable equals original amount minus amount reduced |
| Trigger | After amount reduced validation |
| Input | amountPayable (request), amountReduced (request), composition_amount (database) |
| Logic | amountPayable = composition_amount - amountReduced |
| Output | Proceed to date validation |
| Else | Return HTTP 400 with message "Invalid format" |

```
IF amountPayable = (composition_amount - amountReduced)
THEN proceed to C010
ELSE return "Invalid format"
```

**C010: Amount Payable Non-Negative**

| Attribute | Value |
| --- | --- |
| Description | Validate that amount payable is not negative |
| Trigger | After amount consistency check |
| Input | amountPayable (request) |
| Logic | amountPayable >= 0 |
| Output | Proceed to date validation |
| Else | Return HTTP 400 with message "Invalid format" |

```
IF amountPayable >= 0
THEN proceed to date validation
ELSE return "Invalid format"
```

---

### 2.6 Date Validation

**Description:** Validate that the dates are logically correct.

**Reference:** Backend Code - ReductionValidator.java

#### Condition Details

**C011: Expiry Date After Reduction Date**

| Attribute | Value |
| --- | --- |
| Description | Validate that expiry date is after the reduction date |
| Trigger | After amount validation passes |
| Input | dateOfReduction, expiryDateOfReduction |
| Logic | expiryDateOfReduction > dateOfReduction |
| Output | Proceed to database update |
| Else | Return HTTP 400 with message "Invalid format" |

```
IF expiryDateOfReduction > dateOfReduction
THEN proceed to database update
ELSE return "Invalid format"
```

---

### 2.7 Idempotency Check

**Description:** Check if reduction has already been applied to prevent duplicate processing.

**Reference:** Backend Code - ReductionPersistenceService.java

#### Condition Details

**C012: Reduction Already Applied**

| Attribute | Value |
| --- | --- |
| Description | Check if notice already has TS-RED status (reduction already applied) |
| Trigger | After notice is loaded from database |
| Input | suspension_type, epr_reason_of_suspension from ocms_valid_offence_notice |
| Logic | suspension_type = "TS" AND epr_reason_of_suspension = "RED" |
| Output | Return HTTP 200 with message "Reduction Success" (idempotent response) |
| Else | Continue with validation flow |

```
IF suspension_type = "TS" AND epr_reason_of_suspension = "RED"
THEN return "Reduction Success" (no changes made - idempotent)
ELSE continue with validation flow
```

---

## 3. Decision Tree

### 3.1 Reduction Eligibility Decision Flow

Based on **Functional Flowchart - Backend Reduction** tab:

```
START
  │
  ├─► Data format correct?
  │     │
  │     ├─ NO ──► Return "Invalid format" ──► END
  │     │
  │     └─ YES ─► Mandatory data present?
  │                 │
  │                 ├─ NO ──► Return "Missing data" ──► END
  │                 │
  │                 └─ YES ─► Notice exists?
  │                             │
  │                             ├─ NO ──► Return "Notice not found" ──► END
  │                             │
  │                             └─ YES ─► Reduction already applied (TS-RED)?
  │                                         │
  │                                         ├─ YES ──► Return "Reduction Success" ──► END
  │                                         │
  │                                         └─ NO ─► Notice has been paid?
  │                                                   │
  │                                                   ├─ YES ──► Return "Notice has been paid" ──► END
  │                                                   │
  │                                                   └─ NO ─► Computer Rule Code Eligible?
  │                                                             │
  │                                                             ├─ YES ─► Stage in Extended List?
  │                                                             │           │
  │                                                             │           ├─ YES ─► Apply Reduction
  │                                                             │           │
  │                                                             │           └─ NO ──► Return "Not eligible" ──► END
  │                                                             │
  │                                                             └─ NO ─► Stage = RR3 or DR3?
  │                                                                       │
  │                                                                       ├─ YES ─► Apply Reduction
  │                                                                       │
  │                                                                       └─ NO ──► Return "Not eligible" ──► END
  │
  ▼
Apply Reduction (Transactional)
  │
  ├─► Update successful?
  │     │
  │     ├─ NO ──► Rollback ──► Return "Reduction fail" ──► END
  │     │
  │     └─ YES ─► Return "Reduction Success" ──► END
  │
END
```

### 3.2 Decision Table

| Data Format | Mandatory Data | Notice Exists | Already Reduced | Paid | Rule Code Eligible | Stage Eligible | Result |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Invalid | - | - | - | - | - | - | Invalid format |
| Valid | Missing | - | - | - | - | - | Missing data |
| Valid | Present | No | - | - | - | - | Notice not found |
| Valid | Present | Yes | Yes | - | - | - | Reduction Success (idempotent) |
| Valid | Present | Yes | No | Yes | - | - | Notice has been paid |
| Valid | Present | Yes | No | No | Yes | Yes (extended) | Apply Reduction |
| Valid | Present | Yes | No | No | Yes | No | Not eligible |
| Valid | Present | Yes | No | No | No | Yes (RR3/DR3) | Apply Reduction |
| Valid | Present | Yes | No | No | No | No | Not eligible |

---

## 4. Validation Rules

### 4.1 Field Validations

| Field Name | Data Type | Required | Validation Rule | Error Code | Error Message |
| --- | --- | --- | --- | --- | --- |
| noticeNo | string | Yes | Not blank | MISSING_DATA | Missing data |
| amountReduced | decimal | Yes | Not null, positive | MISSING_DATA / INVALID_FORMAT | Missing data / Invalid format |
| amountPayable | decimal | Yes | Not null, >= 0 | MISSING_DATA / INVALID_FORMAT | Missing data / Invalid format |
| dateOfReduction | datetime | Yes | Not null, valid format yyyy-MM-dd'T'HH:mm:ss | MISSING_DATA / INVALID_FORMAT | Missing data / Invalid format |
| expiryDateOfReduction | datetime | Yes | Not null, valid format, > dateOfReduction | MISSING_DATA / INVALID_FORMAT | Missing data / Invalid format |
| reasonOfReduction | string | Yes | Not blank | MISSING_DATA | Missing data |
| authorisedOfficer | string | Yes | Not blank | MISSING_DATA | Missing data |
| suspensionSource | string | Yes | Not blank | MISSING_DATA | Missing data |
| remarks | string | No | - | - | - |

### 4.2 Cross-Field Validations

| Validation ID | Fields Involved | Rule | Error Code | Error Message |
| --- | --- | --- | --- | --- |
| XF001 | amountReduced, composition_amount | amountReduced <= composition_amount | INVALID_FORMAT | Invalid format |
| XF002 | amountPayable, amountReduced, composition_amount | amountPayable = composition_amount - amountReduced | INVALID_FORMAT | Invalid format |
| XF003 | dateOfReduction, expiryDateOfReduction | expiryDateOfReduction > dateOfReduction | INVALID_FORMAT | Invalid format |

### 4.3 Business Rule Validations

| Rule ID | Rule Name | Description | Condition | Error Code | Error Message |
| --- | --- | --- | --- | --- | --- |
| BR001 | Notice Must Exist | Notice number must exist in database | noticeNo exists in ocms_valid_offence_notice | NOTICE_NOT_FOUND | Notice not found |
| BR002 | Notice Not Paid | Notice must not be paid (CRS not FP or PRA) | crs_reason_of_suspension IS NULL/BLANK | NOTICE_PAID | Notice has been paid |
| BR003 | Eligible Rule Code | If rule code is 30305/31302/30302/21300, check extended stage list | computer_rule_code IN eligible list | NOT_ELIGIBLE | Notice is not eligible |
| BR004 | Eligible Stage - Extended | For eligible codes, stage must be in extended list | last_processing_stage IN extended list | NOT_ELIGIBLE | Notice is not eligible |
| BR005 | Eligible Stage - Special | For non-eligible codes, stage must be RR3 or DR3 | last_processing_stage IN (RR3, DR3) | NOT_ELIGIBLE | Notice is not eligible |

---

## 5. Condition-Action Mapping

### 5.1 Reduction Process Scenarios

| Scenario | Conditions | Actions | Database Updates |
| --- | --- | --- | --- |
| Invalid Request | C001=FALSE | Return error | None |
| Missing Data | C001=TRUE, C002=FALSE | Return error | None |
| Notice Not Found | C001=TRUE, C002=TRUE, C003=FALSE | Return error | None |
| Already Reduced | C003=TRUE, C012=TRUE | Return success (idempotent) | None |
| Paid Notice | C003=TRUE, C012=FALSE, C004=FALSE | Return error | None |
| Not Eligible - Code | C004=TRUE, C005=FALSE, C007=FALSE | Return error | None |
| Not Eligible - Stage | C004=TRUE, C005=TRUE, C006=FALSE | Return error | None |
| Successful Reduction | All validations pass | Apply reduction | Update VON, Insert SN, Insert ROA, Update eVON |

### 5.2 Detailed Scenarios

**Scenario 1: Successful Reduction**

- **Trigger:** All validations pass
- **Conditions:**
  - C001-C011: All TRUE
  - C012: FALSE (not already reduced)
- **Actions:**
  1. Update ocms_valid_offence_notice with TS-RED
  2. Insert into ocms_suspended_notice
  3. Insert into ocms_reduced_offence_amount
  4. Update eocms_valid_offence_notice
- **Database Updates:**
  ```sql
  -- Step 1: Update VON
  UPDATE ocms_valid_offence_notice
  SET suspension_type = 'TS',
      epr_reason_of_suspension = 'RED',
      amount_payable = :amountPayable,
      epr_date_of_suspension = :dateOfReduction,
      due_date_of_revival = :expiryDateOfReduction
  WHERE notice_no = :noticeNo

  -- Step 2: Insert Suspended Notice
  INSERT INTO ocms_suspended_notice (...)

  -- Step 3: Insert Reduced Offence Amount
  INSERT INTO ocms_reduced_offence_amount (...)

  -- Step 4: Update Internet VON
  UPDATE eocms_valid_offence_notice
  SET suspension_type = 'TS',
      epr_reason_of_suspension = 'RED',
      epr_date_of_suspension = :dateOfReduction,
      amount_payable = :amountPayable
  WHERE notice_no = :noticeNo
  ```
- **Expected Result:** HTTP 200 "Reduction Success"

**Scenario 2: Notice Already Paid**

- **Trigger:** CRS reason indicates payment
- **Conditions:**
  - crs_reason_of_suspension = "FP" OR "PRA"
- **Actions:**
  1. Return error response
- **Database Updates:** None
- **Expected Result:** HTTP 409 "Notice has been paid"

**Scenario 3: Notice Not Eligible (Non-Eligible Code, Non-RR3/DR3 Stage)**

- **Trigger:** Rule code not in eligible list AND stage not RR3/DR3
- **Conditions:**
  - computer_rule_code NOT IN (30305, 31302, 30302, 21300)
  - last_processing_stage NOT IN (RR3, DR3)
- **Actions:**
  1. Return error response
- **Database Updates:** None
- **Expected Result:** HTTP 409 "Notice is not eligible"

---

## 6. Exception Handling

### 6.1 Business Exceptions

| Exception Code | Exception Name | Condition | Handling |
| --- | --- | --- | --- |
| INVALID_FORMAT | Invalid Request Format | Request JSON invalid or wrong data types | Return HTTP 400 "Invalid format" |
| MISSING_DATA | Missing Mandatory Data | Required fields not provided | Return HTTP 400 "Missing data" |
| NOTICE_NOT_FOUND | Notice Not Found | Notice number not in database | Return HTTP 404 "Notice not found" |
| NOTICE_PAID | Notice Already Paid | CRS = FP or PRA | Return HTTP 409 "Notice has been paid" |
| NOT_ELIGIBLE | Notice Not Eligible | Eligibility check failed | Return HTTP 409 "Notice is not eligible" |

### 6.2 System Exceptions

| Exception Code | Exception Name | Condition | Handling |
| --- | --- | --- | --- |
| DATABASE_ERROR | Database Error | DB connection or query failed | Rollback, return HTTP 500 "Reduction fail" |
| ROLLBACK_FAILURE | Rollback Failed | Transaction rollback failed | Log error, return HTTP 500 "Reduction fail" |
| SYSTEM_UNAVAILABLE | System Unavailable | Backend cannot access resources | Return HTTP 503 "System unavailable" |
| OPTIMISTIC_LOCK | Concurrent Modification | Another process modified the record | Return HTTP 500 "Reduction fail" with retry suggestion |

**Reference:** FD Section 3.3 - Reduction Scenarios and Outcomes

---

## 7. Test Scenarios

### 7.1 Condition Test Cases

| Test ID | Condition | Test Input | Expected Output | Reference |
| --- | --- | --- | --- | --- |
| TC001 | C001 - Invalid JSON | Malformed JSON | 400 "Invalid format" | FD 3.3 |
| TC002 | C002 - Missing noticeNo | noticeNo = null | 400 "Missing data" | FD 3.3 |
| TC003 | C003 - Notice not found | noticeNo = "INVALID123" | 404 "Notice not found" | FD 3.3 |
| TC004 | C004 - Notice paid | CRS = "FP" | 409 "Notice has been paid" | FD 3.3 |
| TC005 | C005+C006 - Eligible code, eligible stage | ruleCode=30305, stage=NPA | 200 "Reduction Success" | FD 3.2 |
| TC006 | C005+C006 - Eligible code, ineligible stage | ruleCode=30305, stage=ABC | 409 "Notice is not eligible" | FD 3.2 |
| TC007 | C007 - Non-eligible code, RR3 stage | ruleCode=99999, stage=RR3 | 200 "Reduction Success" | FD 3.2 |
| TC008 | C007 - Non-eligible code, DR3 stage | ruleCode=99999, stage=DR3 | 200 "Reduction Success" | FD 3.2 |
| TC009 | C007 - Non-eligible code, non-RR3/DR3 | ruleCode=99999, stage=NPA | 409 "Notice is not eligible" | FD 3.2 |
| TC010 | C012 - Already reduced | TS-RED status exists | 200 "Reduction Success" (idempotent) | Backend Code |
| TC011 | Update failure | DB error during update | 500 "Reduction fail" | FD 3.3 |
| TC012 | Partial update | Insert SN fails after VON update | 500 "Reduction fail" (rollback) | FD 3.3 |

### 7.2 Edge Cases

| Test ID | Scenario | Test Input | Expected Output |
| --- | --- | --- | --- |
| EC001 | Amount reduced = composition amount | amountReduced = 70, amountPayable = 0 | 200 "Reduction Success" |
| EC002 | Amount reduced > composition amount | amountReduced = 100, composition = 70 | 400 "Invalid format" |
| EC003 | Negative amount payable | amountPayable = -10 | 400 "Invalid format" |
| EC004 | Expiry date before reduction date | expiry < reduction date | 400 "Invalid format" |
| EC005 | Same expiry and reduction date | expiry = reduction date | 400 "Invalid format" |

---

## 8. Changelog

| Version | Date | Author | Changes |
| --- | --- | --- | --- |
| 1.0 | 18/01/2026 | Claude | Initial version based on FD v1.3, Backend Code, and Functional Flowchart |

# OCMS 21 - Duplicate Notices (DBB) - Condition & Validation Planning

## Document Information

| Field | Value |
|-------|-------|
| **Document Version** | 1.3 |
| **Date** | 2026-01-15 |
| **Epic** | OCMS 21 - Manage Duplicate Notices |
| **Feature** | Double Booking (DBB) Detection Conditions |
| **Source Documents** | v1.0_OCMS_21_Duplicate_Notices.md (FD) |
| **Code Reference** | NoticeValidationHelper.java |
| **Author** | Claude Code (Planning Phase) |

---

## Table of Contents

1. [Overview](#overview)
2. [Business Rules](#business-rules)
3. [DBB Criteria by Offence Type](#dbb-criteria-by-offence-type)
4. [Suspension Eligibility Conditions](#suspension-eligibility-conditions)
5. [Decision Trees](#decision-trees)
6. [Validation Rules](#validation-rules)
7. [Error Conditions](#error-conditions)
8. [Assumptions Log](#assumptions-log)

---

## 1. Overview

### Scope
DBB detection is a **backend-only automated function** that runs after notice creation. There are:
- ❌ **NO frontend validations** (no user input for DBB)
- ✅ **Backend validations only** (automated checks)
- ✅ **Type-specific criteria** (Type O vs Type E vs Type U)

### References
- **FD Section 3.3:** Type O DBB Criteria
- **FD Section 3.4:** Type E DBB Criteria
- **Code:** `NoticeValidationHelper.java:359-547`

---

## 2. Business Rules

### BR-DBB-001: DBB Detection Trigger
**Rule:** DBB check runs automatically AFTER notice creation succeeds.

**Conditions:**
- Notice has been created successfully in database
- Notice has offence type O or E (Type U → manual processing)

**FD Reference:** Section 2.2 - "DBB check is a post-creation function"

**Code Reference:** `CreateNoticeServiceImpl.java:1063`

```java
log.info("Step 6: Checking for double booking (duplicate offense details)");
boolean isDuplicateOffense = createNoticeHelper.checkDuplicateOffenseDetails(singleDtoMap, ocmsValidOffenceNoticeService);
```

---

### BR-DBB-002: Self-Matching Prevention
**Rule:** When querying for duplicates, exclude the current notice from results.

**Condition:** `notice_no != current_notice_no`

**FD Reference:** Section 3.3/3.4 - "Query excludes current notice"

**Code Reference:** `NoticeValidationHelper.java:435-439`

```java
// Skip comparing with itself if notice number is the same
if (dto.getNoticeNo() != null && dto.getNoticeNo().equals(notice.getNoticeNo())) {
    log.debug("Skipping self-comparison for notice {}", notice.getNoticeNo());
    continue;
}
```

---

### BR-DBB-003: Query Retry Logic
**Rule:** If DBB query fails, retry up to 3 times with exponential backoff.

**Conditions:**
- Retry attempt 1: Wait 100ms
- Retry attempt 2: Wait 200ms
- Retry attempt 3: Wait 400ms
- After 3 failures: Apply TS-OLD suspension

**FD Reference:** Section 3.3/3.4 - "If DB query fails, retry 3x, then apply TS-OLD"

**Code Reference:** `NoticeValidationHelper.java:394-428`

---

### BR-DBB-004: Suspension Type Determination
**Rule:** Later-created notice (by OCMS creation timestamp) gets PS-DBB suspension.

**Conditions:**
- Compare `notice_creation_datetime` (NOT offence time)
- Latest created notice → Suspended with PS-DBB
- Earlier created notice → Remains valid

**FD Reference:** Section 1.4 - "OCMS applies PS-DBB to notice with later creation date/time"

**Code Reference:** Implicit in logic (current notice is always latest)

---

### BR-DBB-005: Type U Manual Processing
**Rule:** Type U (UPL) offences do NOT undergo automated DBB check.

**Conditions:**
- If `offence_type = 'U'` → Route to manual processing (PIU/Deciding Officer)
- No DBB logic applied

**FD Reference:** Section 1.4 - "OCMS MVP will not process UPL (Type U) Offences"

---

## 3. DBB Criteria by Offence Type

### 3.1 Type O (Parking Offence) Criteria

**Rule ID:** BR-DBB-O-001

**Matching Conditions (ALL must match):**
1. ✅ **Computer Rule Code** = Same
2. ✅ **Car Park Code (PP Code)** = Same
3. ✅ **Vehicle Number** = Same
4. ✅ **Offence Date** = Same **DATE ONLY** (ignore time)

**FD Reference:** Section 3.3 - "Type O DBB Criteria and Handling"

**Code Reference:** `NoticeValidationHelper.java:450-455`

```java
if ("O".equals(offenseType)) {
    // Type O (Parking): Compare DATE only (ignore time)
    boolean dateOnlyMatch = noticeDateAndTime.toLocalDate().equals(existingNoticeDateTime.toLocalDate());
    dateTimeMatch = dateOnlyMatch;
    log.debug("Type O check - New: {}, Existing: {}, Match: {}",
            noticeDateAndTime.toLocalDate(), existingNoticeDateTime.toLocalDate(), dateTimeMatch);
}
```

**Example:**
```
New Notice:     2026-01-08 14:30:00 → Date: 2026-01-08
Existing Notice: 2026-01-08 09:15:00 → Date: 2026-01-08
Result: DATE MATCH (time difference ignored) → Potential duplicate
```

---

### 3.2 Type E (Payment Evasion) Criteria

**Rule ID:** BR-DBB-E-001

**Matching Conditions (ALL must match):**
1. ✅ **Computer Rule Code** = Same
2. ✅ **Car Park Code (PP Code)** = Same
3. ✅ **Vehicle Number** = Same
4. ✅ **Offence Date + Time** = Same **DATE + HH:MM** (ignore seconds)

**FD Reference:** Section 3.4 - "Type E DBB Criteria and Handling"

**Code Reference:** `NoticeValidationHelper.java:456-465`

```java
else if ("E".equals(offenseType)) {
    // Type E (Enforcement): Compare DATE + TIME (HH:MM only, ignore seconds)
    boolean dateMatch = noticeDateAndTime.toLocalDate().equals(existingNoticeDateTime.toLocalDate());
    boolean hourMatch = noticeDateAndTime.getHour() == existingNoticeDateTime.getHour();
    boolean minuteMatch = noticeDateAndTime.getMinute() == existingNoticeDateTime.getMinute();
    dateTimeMatch = dateMatch && hourMatch && minuteMatch;
    log.debug("Type E check - New: {} {}:{}, Existing: {} {}:{}, Match: {}",
            noticeDateAndTime.toLocalDate(), noticeDateAndTime.getHour(), noticeDateAndTime.getMinute(),
            existingNoticeDateTime.toLocalDate(), existingNoticeDateTime.getHour(), existingNoticeDateTime.getMinute(),
            dateTimeMatch);
}
```

**Example:**
```
New Notice:      2026-01-08 14:30:25 → Date+Time: 2026-01-08 14:30
Existing Notice: 2026-01-08 14:30:58 → Date+Time: 2026-01-08 14:30
Result: DATE + HH:MM MATCH (seconds ignored) → Potential duplicate
```

---

### 3.3 Comparison: Type O vs Type E

| Criteria | Type O (Parking) | Type E (Payment Evasion) |
|----------|------------------|--------------------------|
| Rule Code | ✅ Match | ✅ Match |
| PP Code | ✅ Match | ✅ Match |
| Vehicle Number | ✅ Match | ✅ Match |
| Date | ✅ Match (date only) | ✅ Match (date + time) |
| Time | ❌ Ignore | ✅ Match HH:MM (ignore seconds) |
| ANS Check | ✅ PS-ANS qualifies | ❌ PS-ANS does NOT qualify |

**FD Reference:** Section 1.4 - "Differences between Duplicate Notice Processing"

---

## 4. Suspension Eligibility Conditions

### 4.1 Eligibility Decision Tree

**Rule ID:** BR-DBB-ELIG-001

**Question:** Is the existing notice eligible to trigger DBB for the new notice?

**Decision Flow:**
```
IF existing notice suspension_type IS NULL → YES (active notice)
ELSE IF suspension_type = 'PS':
    IF epr_reason_of_suspension = 'FOR' → YES (Foreign notice)
    ELSE IF epr_reason_of_suspension = 'ANS' AND offence_type = 'O' → YES (Advisory Notice, Type O only)
    ELSE IF epr_reason_of_suspension = 'DBB' → YES (Already DBB suspended)
    ELSE IF crs_reason_of_suspension IN ('FP', 'PRA') → YES (Paid notice)
    ELSE → NO (Other PS codes)
ELSE IF suspension_type = 'TS' → YES (Temporary suspension treated as active)
ELSE → NO (Other suspension types)
```

**FD Reference:** Section 3.3/3.4 - Suspension eligibility flowcharts

**Code Reference:** `NoticeValidationHelper.java:476-520`

---

### 4.2 Eligible Suspension Codes

| Code | Type | Description | Type O | Type E | Reference |
|------|------|-------------|--------|--------|-----------|
| `NULL` | - | No suspension (active) | ✅ | ✅ | FD 3.3/3.4 |
| `FOR` | EPR | Foreign vehicle | ✅ | ✅ | FD 3.3/3.4 |
| `ANS` | EPR | Advisory Notice Sent | ✅ | ❌ | FD 3.3 only |
| `DBB` | EPR | Double Booking | ✅ | ❌ | FD 3.3 only |
| `FP` | CRS | Further Processing (Paid) | ✅ | ✅ | FD 3.3/3.4 |
| `PRA` | CRS | Pending Review Action (Paid) | ✅ | ✅ | FD 3.3/3.4 |

**⚠️ KEY DIFFERENCES (Updated 2026-01-13 per FD Compliance):**
- **Type O (FD 3.3):** FOR, ANS, DBB, FP, PRA all qualify
- **Type E (FD 3.4):** FOR, FP, PRA only - ANS and DBB NOT mentioned in FD 3.4

**Code Reference:** `NoticeValidationHelper.java:498-505`

```java
// OCMS 21: ANS check ONLY for Type O (Parking), NOT for Type E (Enforcement)
else if ("ANS".equals(eprReason) && "O".equals(offenseType)) {
    isDuplicate = true;
    duplicateReason = "Existing notice is PS-ANS (Advisory Notice Sent) - Type O only";
}
```

---

### 4.3 Ineligible Suspension Codes (Examples)

The following PS codes do NOT qualify existing notice for DBB:
- `APP` - Appeal
- `CAN` - Cancelled
- `CFA` - Call for Appearance
- `CFP` - Called for Payment
- `DIP` - Diplomatic
- `FCT` - Faulty Ticket
- `FTC` - First Time Compound
- (etc. - all other PS codes not in eligible list)

**FD Reference:** Section 3.3/3.4 - "Other reasons → Latest notice is NOT a duplicate"

**Code Reference:** `NoticeValidationHelper.java:510-514`

```java
else {
    // Other PS codes - NOT a duplicate
    log.info("Existing notice has PS-{}/{} - NOT qualifying for DBB (offenseType={})", eprReason, crsReason, offenseType);
    continue;
}
```

---

## 5. Decision Trees

### 5.1 Main DBB Detection Flow

```
START: Notice created successfully
  |
  ├─→ Is offence_type = 'U'?
  |    └─→ YES: Route to manual processing (END)
  |    └─→ NO: Continue
  |
  ├─→ Query DB for matching notices (vehicle, rule code, PP code)
  |    └─→ Query failed?
  |         ├─→ YES: Retry (up to 3x)
  |         |    └─→ Still failing? → Apply TS-OLD (END)
  |         └─→ NO: Continue
  |
  ├─→ Any matching notices found?
  |    └─→ NO: Not a duplicate (END - Continue normal processing)
  |    └─→ YES: Continue
  |
  ├─→ Check date/time match (Type O vs Type E logic)
  |    └─→ NO MATCH: Not a duplicate (END)
  |    └─→ MATCH: Continue
  |
  ├─→ Check suspension eligibility (FOR/ANS/DBB/FP/PRA)
  |    └─→ NOT ELIGIBLE: Not a duplicate (END)
  |    └─→ ELIGIBLE: IS A DUPLICATE
  |
  └─→ Apply PS-DBB suspension to current notice (END)
```

**FD Reference:** Section 3.2 - "High Level Process Flow"

---

### 5.2 Type O Date Match Decision

```
Type O Date Comparison:
  |
  ├─→ Extract date from new notice: newDate = notice_date_and_time.date()
  ├─→ Extract date from existing notice: existingDate = existing_notice_date_and_time.date()
  |
  └─→ IF newDate == existingDate
       └─→ DATE MATCH → Continue to suspension eligibility check
       └─→ ELSE → NO MATCH → Not a duplicate
```

---

### 5.3 Type E Date+Time Match Decision

```
Type E Date+Time Comparison:
  |
  ├─→ Extract date from both notices
  ├─→ Extract hour (HH) from both notices
  ├─→ Extract minute (MM) from both notices
  |
  └─→ IF (date == date) AND (hour == hour) AND (minute == minute)
       └─→ DATE+TIME MATCH → Continue to suspension eligibility check
       └─→ ELSE → NO MATCH → Not a duplicate
```

---

### 5.4 Suspension Eligibility Decision (Type O)

```
Existing Notice Suspension Status (Type O):
  |
  ├─→ suspension_type IS NULL?
  |    └─→ YES: ELIGIBLE (active notice)
  |
  ├─→ suspension_type = 'PS'?
  |    ├─→ epr_reason = 'FOR'? → YES: ELIGIBLE
  |    ├─→ epr_reason = 'ANS'? → YES: ELIGIBLE (Type O specific)
  |    ├─→ epr_reason = 'DBB'? → YES: ELIGIBLE
  |    ├─→ crs_reason = 'FP' OR 'PRA'? → YES: ELIGIBLE
  |    └─→ ELSE → NOT ELIGIBLE
  |
  └─→ suspension_type = 'TS'? → YES: ELIGIBLE (treat as active)
```

---

### 5.5 Suspension Eligibility Decision (Type E)

```
Existing Notice Suspension Status (Type E) - Updated 2026-01-13 per FD 3.4:
  |
  ├─→ suspension_type IS NULL?
  |    └─→ YES: ELIGIBLE (active notice)
  |
  ├─→ suspension_type = 'PS'?
  |    ├─→ epr_reason = 'FOR'? → YES: ELIGIBLE
  |    ├─→ Other epr_reason? → Check crs_reason
  |         ├─→ crs_reason = 'FP' OR 'PRA'? → YES: ELIGIBLE
  |         └─→ ELSE → NOT ELIGIBLE
  |
  └─→ suspension_type = 'TS'? → YES: ELIGIBLE (treat as active)

⚠️ FD 3.4 COMPLIANCE: Type E only checks FOR, FP, PRA
   - ANS check removed (not in FD 3.4)
   - DBB check removed (not in FD 3.4)
```

---

## 6. Validation Rules

### 6.1 Frontend Validations
**N/A** - DBB is a backend-only automated function. No user input required.

---

### 6.2 Backend Validations

#### VAL-DBB-001: Required Fields Check
**Rule:** All matching criteria fields must be present.

**Fields:**
- `vehicle_no` (NOT NULL)
- `notice_date_and_time` (NOT NULL)
- `computer_rule_code` (NOT NULL)
- `pp_code` (NOT NULL)

**Action if missing:** Skip DBB check (return false)

**Code Reference:** `NoticeValidationHelper.java:378-381`

```java
// Check if any required field is missing
if (vehicleNo == null || noticeDateAndTime == null || computerRuleCode == null || ppCode == null) {
    log.info("One or more required fields for duplicate check are null");
    return false;
}
```

---

#### VAL-DBB-002: Offence Type Validation
**Rule:** Offence type must be retrieved from `offense_rule_code` table.

**Action if missing:** Throw error, terminate notice creation.

**Code Reference:** `CreateNoticeServiceImpl.java:849-863`

---

#### VAL-DBB-003: Date/Time Null Check
**Rule:** Existing notice must have valid `notice_date_and_time`.

**Action if NULL:** Skip that existing notice in comparison.

**Code Reference:** `NoticeValidationHelper.java:441-445`

```java
LocalDateTime existingNoticeDateTime = notice.getNoticeDateAndTime();
if (existingNoticeDateTime == null) {
    log.debug("Skipping notice {} - no date/time", notice.getNoticeNo());
    continue;
}
```

---

## 7. Error Conditions

### 7.1 Query Failure → TS-OLD Fallback

**Error Condition:** DBB query fails after 3 retry attempts.

**Action:**
1. Log error with exception details
2. Set `dbbQueryFailed = true` flag
3. Apply TS-OLD suspension instead of PS-DBB
4. Set suspension remarks: `"TS-OLD: DBB query failed after 3 retries - [exception]"`

**FD Reference:** Section 3.3/3.4 - "On query failure → Apply TS-OLD"

**Code Reference:**
- Query failure detection: `NoticeValidationHelper.java:422-428`
- TS-OLD application: `CreateNoticeServiceImpl.java:1068-1157`

---

### 7.2 Notice Not Found (Post-Creation)

**Error Condition:** After creating notice, cannot retrieve it for PS-DBB update.

**Action:**
1. Log error: `"Could not find notice {} to update with PS-DBB fields"`
2. Continue to create suspended_notice record
3. Do NOT fail entire process

**Code Reference:** `CreateNoticeServiceImpl.java:1184-1186`

```java
} else {
    log.error("Could not find notice {} to update with PS-DBB fields", noticeNumber);
}
```

---

### 7.3 Internet DB Update Failure

**Error Condition:** Cannot update Internet DB (eocms_valid_offence_notice).

**Action:**
1. Log error
2. Do NOT fail entire process
3. Intranet suspension remains valid

**Code Reference:** `CreateNoticeServiceImpl.java:1204-1207`

```java
} catch (Exception e) {
    log.error("Error updating Internet DB for notice {}: {}", noticeNumber, e.getMessage());
    // Don't fail the entire process if Internet DB update fails
}
```

---

## 8. Assumptions Log

### 8.1 Assumption: PS-DBB is Permanent Suspension

**Assumption:** PS-DBB notices should have `due_date_of_revival = NULL` (permanent suspension).

**Source:** FD Section 3.5.1 - "NULL (as notice is permanently suspended)"

**Status:** ✅ RESOLVED - Flowchart updated to show `due_date_of_revival = NULL` for PS-DBB.

**Note:** For TS-OLD suspension, FD does not specify exact duration. Value should be based on system configuration.

---

### 8.2 Assumption: Later-Created Notice Gets Suspended

**Assumption:** The notice with the later OCMS creation timestamp (not offence time) is suspended.

**Source:** FD Section 1.4 - "OCMS applies PS-DBB to notice with later creation date/time"

**Current Code:** ✅ Implicit - current notice being created is always the "later" one.

**Validation:** Confirmed by code logic (existing notices are always earlier).

---

### 8.3 Assumption: Type U Manual Processing

**Assumption:** Type U (UPL) offences do NOT undergo automated DBB check.

**Source:** FD Section 1.4 - "OCMS MVP will not process UPL (Type U) Offences"

**Current Code:** ⚠️ Not explicitly handled - needs verification.

**Action Required:** Confirm Type U notices are routed to manual processing before DBB check.

---

### 8.4 Assumption: ANS Check Type O Only

**Assumption:** PS-ANS eligibility applies ONLY to Type O (Parking), NOT Type E (Payment Evasion).

**Source:** FD Section 3.3 vs 3.4 comparison

**Current Code:** ✅ Correctly implemented with `&& "O".equals(offenseType)` check.

**Validation:** Code aligns with FD requirement.

**Code Reference:** `NoticeValidationHelper.java:499`

---

### 8.5 Assumption: Self-Matching Prevention

**Assumption:** Current notice should be excluded from duplicate check query.

**Source:** FD Section 3.3/3.4 - "Query excludes current notice"

**Current Code:** ✅ Implemented with notice_no comparison.

**Validation:** Confirmed in code (skip if notice_no matches).

**Code Reference:** `NoticeValidationHelper.java:436-439`

---

### 8.6 Assumption: System Users

**Assumption:** All DBB operations use system connection users.

**Source:** FD Section 3.5.1

**Current Code:** ⚠️ Inconsistent - uses mix of `ocmsiz_app_conn` and `ocmsizmgr_conn`.

**Clarification Needed:**
- FD says: `ocmsiz_app_conn` for all operations
- Code uses: `ocmsizmgr_conn` for `officer_authorising_suspension`
- Code uses: `ocmsiz_app_conn` for `suspension_source`

**Action Required:** Clarify with BA which is correct.

---

## 9. Summary

### Conditions Implemented
- ✅ Type O date-only matching
- ✅ Type E date+time (HH:MM) matching
- ✅ Suspension eligibility checks (FOR/ANS/DBB/FP/PRA)
- ✅ ANS check Type O specific
- ✅ Query retry with exponential backoff
- ✅ TS-OLD fallback on query failure
- ✅ Self-matching prevention

### Conditions Needing Clarification
- ⚠️ PS-DBB revival date (should be NULL)
- ⚠️ System user names (FD vs Code mismatch)
- ⚠️ Type U routing (manual processing confirmation)

### No Frontend Validations
- DBB is backend-only automated function
- No user input or UI validation needed

---

**Document Status:** ✅ Verified against Flowchart Implementation

**Next Step:** Generate Technical Document

---

## Revision History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-01-08 | Initial planning document |
| 1.1 | 2026-01-13 | Updated Type E eligibility per FD 3.4 compliance |
| 1.2 | 2026-01-13 | Final verification - confirmed TS eligibility per FD 3.3 Step 5 |
| 1.3 | 2026-01-15 | Marked PS-DBB revival date issue as RESOLVED |

### v1.2 Changes (2026-01-13)

**TS Eligibility Clarification (FD Section 3.3 Step 5):**
- ✅ **TS IS ELIGIBLE** - per FD: "If suspension_type is not PS, OCMS determines that the latest Notice qualifies as a duplicate"
- ✅ Flowchart note3 updated accordingly

**Audit Fields Verified:**
- ✅ All UPDATE operations include `upd_user_id='ocmsiz_app_conn'`
- ✅ All INSERT operations include `cre_user_id='ocmsiz_app_conn'`
- ✅ `suspension_source='OCMS'` (not 'ocmsiz_app_conn')

---

### v1.1 Changes (2026-01-13)

**Type E Eligibility Updated (FD Section 3.4 Compliance):**
- ❌ Removed ANS from Type E eligibility (not in FD 3.4)
- ❌ Removed DBB from Type E eligibility (not in FD 3.4)
- ✅ Type E now only checks: FOR, FP, PRA (per FD 3.4 Step 8)

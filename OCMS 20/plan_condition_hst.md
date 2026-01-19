# OCMS 20 - HST Suspension: Condition Plan

## Document Information

| Item | Value |
|------|-------|
| Version | 1.0 |
| Date | 2026-01-07 |
| Feature | HST (House-to-House Search/Tenant) Suspension |
| Source | Functional Flow HST.drawio, Functional Document v1.1 |

---

## 1. Frontend Validations (Staff Portal)

### 1.1 Manual HST Suspension Form

| Field | Validation | Error Message |
|-------|------------|---------------|
| ID Number | Required, valid format (NRIC/FIN/UEN) | "Please enter a valid ID number" |
| ID Type | Auto-detected from ID number | - |
| Offender Name | Display only (from DB) | - |
| Address Fields | Display only, editable | - |

### 1.2 ID Number Format Validation

| ID Type | Format | Pattern |
|---------|--------|---------|
| NRIC (N) | S/T + 7 digits + letter | `^[ST]\d{7}[A-Z]$` |
| FIN (F) | F/G/M + 7 digits + letter | `^[FGM]\d{7}[A-Z]$` |
| UEN (U) | Various formats | Multiple patterns |

**Note:** UEN format varies as it is issued by multiple agencies, not just ACRA.

---

## 2. Backend Validations

### 2.1 Check Offender in HST (Rule ID: HST-VAL-001)

```
IF offender ID found in ocms_hst table
THEN
   Return "ID is suspended under HST"
   Block HST suspension creation
ELSE
   Allow HST suspension creation
   Return offender details and latest address
```

**Note:** `ocms_hst` does not have a status column. Existence in the table indicates active HST.

### 2.2 HST Suspension Creation (Rule ID: HST-VAL-002)

```
VALIDATE:
1. ID number is valid format
2. ID not already in ocms_hst table
3. User has permission to create HST

IF validation passed:
   1. INSERT record into ocms_hst (id_no, id_type, name, address fields, cre_date, cre_user_id)
   2. Query notices under this ID from ocms_offence_notice_owner_driver
   3. FOR EACH notice WHERE offender is current offender:
      - Check if notice can be suspended
      - If yes, INSERT TS-HST suspension into ocms_suspended_notice
   4. Return success with suspended notices list

IF validation failed:
   Return appropriate error code
```

### 2.3 Notice Suspension Eligibility (Rule ID: HST-VAL-003)

```
Notice CAN be suspended with TS-HST IF:
1. Notice is not already PS-suspended (Prosecution Suspended)
2. Notice is not in Court stage
3. Notice is before RD1 (for new notices under HST ID)

Notice CANNOT be suspended IF:
1. Already has PS suspension
2. In Court stage
3. Already has TS-HST suspension (for new notices check)
```

### 2.4 HST Update Validation (Rule ID: HST-VAL-004)

```
IF new address found from DataHive/MHA:
   1. Validate HST ID exists in ocms_hst
   2. UPDATE ocms_hst with new address fields (upd_date, upd_user_id)
   3. UPDATE address in ocms_offence_notice_owner_driver_addr for notices under this ID
   4. Return success

IF HST ID not found in ocms_hst:
   Return error OCMS-4003
```

### 2.5 HST Revival Validation (Rule ID: HST-VAL-005)

```
VALIDATE:
1. HST ID exists in ocms_hst
2. User has permission to revive

IF validation passed:
   1. DELETE from ocms_hst WHERE id_no = ?
   2. FOR EACH notice with TS-HST under this ID (date_of_revival IS NULL):
      - UPDATE ocms_suspended_notice SET:
        - date_of_revival = GETDATE()
        - revival_reason = ?
        - officer_authorising_revival = ?
        - revival_remarks = ?
        - upd_date, upd_user_id
      - Notice continues normal processing
   3. Return success

IF validation failed:
   Return appropriate error code
```

**Note:** Revival updates `date_of_revival` in `ocms_suspended_notice` (not `revived_ind` which does not exist).

---

## 3. Business Rules

### 3.1 HST Looping Mechanism (Rule ID: HST-BUS-001)

```
WHEN TS-HST suspension expires:
   IF no new address found from monthly DataHive check:
      Re-apply TS-HST suspension
      Continue looping until:
      - New address found, OR
      - Manual revival by OIC
```

**Reference:** OCMS 17 Functional Document for suspension expiry handling.

### 3.2 New Notice Under HST ID (Rule ID: HST-BUS-002)

```
WHEN new notice created for ID that is in HST:
   1. Check ocms_hst for this ID
   2. IF ID found in ocms_hst:
      - Apply TS-HST suspension before RD1
      - Notice does not proceed to normal workflow
```

**Reference:** OCMS 11 Functional Document for new notice creation.

### 3.3 Payment Handling (Rule ID: HST-BUS-003)

```
WHEN payment made for notice under TS-HST:
   1. Payment processed normally
   2. Notice suspended with PS-FP (Paid)
   3. TS-HST will NOT be revived
   4. Other notices under same HST ID continue as TS-HST
```

**Note:** Payment can occur at any point in the HST processing flow.

### 3.4 Monthly DataHive Check (Rule ID: HST-BUS-004)

```
MONTHLY CRON:
   FOR EACH active HST ID:
      1. Query DataHive for NRIC/FIN
      2. Query DataHive for UEN (if applicable)
      3. IF new address found:
         - Trigger Manual HST Update workflow
         - Generate HST Address Update Report
      4. IF no new address:
         - Continue TS-HST loop
         - Generate HST No Address Report
```

---

## 4. Decision Trees

### 4.1 HST Suspension Decision

```
                    [Start]
                       |
          [Check if ID in HST table]
                       |
        +--------[Found?]--------+
        |                        |
       Yes                       No
        |                        |
   [Return error:            [Query offender
    Already HST]              details]
                                 |
                      [Display to OIC]
                                 |
                      [OIC confirms]
                                 |
                      [Create HST ID]
                                 |
                      [Suspend notices
                       with TS-HST]
                                 |
                             [End]
```

### 4.2 HST Revival Decision

```
                    [Start]
                       |
          [Check if ID in ocms_hst]
                       |
        +--------[Found?]-------+
        |                        |
       Yes                       No
        |                        |
   [DELETE from ocms_hst]   [Return error:
        |                    Not found]
   [UPDATE date_of_revival
    in ocms_suspended_notice]
        |
   [Notices continue
    normal flow]
        |
      [End]
```

---

## 5. External API Conditions

### 5.1 DataHive Query

**Pre-conditions:**
- HST ID is active
- Monthly cron schedule triggered

**Post-conditions:**
- Address stored in ocms_temp_unc_hst_addr
- Report generated if new address found

**Error Handling:**
- Connection timeout: Retry 3 times
- After 3 failures: Log error, continue with next ID

### 5.2 MHA Query

**Pre-conditions:**
- ID queued in ocms_nro_temp
- MHA SFTP connection available

**Post-conditions:**
- Results processed via MHA results cron
- Address stored in temp table

**Error Handling:**
- Refer to MHA Integration Technical Document

---

## 6. Suspension Type Reference

| Code | Description | Duration |
|------|-------------|----------|
| TS-HST | House-to-House Search/Tenant | Loops until new address or revival |
| TS-UNC | Unclaimed Reminder | Standard period |
| PS | Prosecution Suspended | Until court decision |
| PS-FP | Full Payment | Permanent |

---

## 7. Assumptions Log

| ID | Assumption | Status |
|----|------------|--------|
| ASM-001 | UEN validation uses basic format check due to multiple issuing agencies | [ASSUMPTION] |
| ASM-002 | Monthly DataHive cron runs on 1st of each month | [ASSUMPTION] |
| ASM-003 | HST revival removes all TS-HST suspensions atomically | [ASSUMPTION] |

---

*End of Document*

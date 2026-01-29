# ocms_adhoc_nro_queue Table Analysis

## Document Information

| Attribute | Value |
|-----------|-------|
| Version | 1.0 |
| Date | 2026-01-27 |
| Status | Draft |
| Epic | OCMS 20 |
| Source | Functional_Flow_Unclaimed Reminders.drawio, v1.1_OCMS_20_Unclaimed_HST.md |

---

## 1. Table Overview

### 1.1 Purpose

The `ocms_adhoc_nro_queue` table serves as a **queue mechanism** for storing offender IDs that need to be submitted to external systems (MHA and DataHive) for address verification. This table handles ad-hoc queries that are **not related to processing stage changes**.

### 1.2 Use Cases

| Use Case | Query Reason | Description |
|----------|--------------|-------------|
| Unclaimed Reminders | UNC | Offender IDs from unclaimed reminder letters that need address verification |
| HST ID Verification | HST | HST (Hard-to-Serve) IDs that need address re-verification |
| Particulars Not Found | NA | IDs where particulars were not found and need re-query |

### 1.3 Table Lifecycle

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                          RECORD LIFECYCLE                                     │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│   ┌─────────┐    ┌─────────────┐    ┌──────────────┐    ┌─────────────────┐  │
│   │ INSERT  │───>│  MHA CRON   │───>│ DATAHIVE     │───>│ DELETE after    │  │
│   │ Record  │    │  Processes  │    │ CRON         │    │ Report Generated│  │
│   └─────────┘    └─────────────┘    └──────────────┘    └─────────────────┘  │
│                                                                               │
│   sent_to_mha = NULL   sent_to_mha = <date>   sent_to_datahive = <date>      │
│   sent_to_dh = NULL    sent_to_dh = NULL      Results in temp table          │
│                                                                               │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Field Analysis

### 2.1 id_no (Primary Key)

| Attribute | Value |
|-----------|-------|
| Data Type | varchar(12) |
| Nullable | No |
| Primary Key | Yes |
| Source Reference | FD Section 2.4.5 |

#### Reasoning

**From Flowchart:**
- Step: *"Put to ocms_adhoc_nro_queue to prepare for MHA query"*
- Step: *"IDs for query are added to ocms_adhoc_nro_queue"*

**Purpose:**
- Stores the offender's identification number (NRIC/FIN/UEN)
- This ID is used to query MHA and DataHive for the latest address information

**Why Primary Key:**
- Each ID should only exist once in the queue at any given time
- Prevents duplicate queries to external systems
- Ensures data integrity

**Sample Values:**
- `S1234567A` (NRIC)
- `G1234567N` (FIN)
- `12345678X` (UEN)

---

### 2.2 id_type

| Attribute | Value |
|-----------|-------|
| Data Type | varchar(1) |
| Nullable | No |
| Source Reference | FD Section 2.4.5 |

#### Reasoning

**From Flowchart:**
- Note: *"DataHive query for NRIC, FIN and UEN"*
- Different ID types require different external system queries

**Purpose:**
- Distinguishes between different types of identification numbers
- Determines which external system(s) to query

**Values and Processing Logic:**

| Value | ID Type | MHA Query | DataHive Query |
|-------|---------|-----------|----------------|
| N | NRIC (Singapore Citizen/PR) | Yes | Yes |
| F | FIN (Foreigner) | Yes | Yes |
| U | UEN (Company) | No | Yes |

**Why Needed:**
- MHA only accepts NRIC and FIN queries
- UEN (company IDs) can only be queried via DataHive
- Processing logic differs based on ID type

---

### 2.3 query_reason

| Attribute | Value |
|-----------|-------|
| Data Type | varchar(5) |
| Nullable | No |
| Source Reference | FD Section 2.4.5, Flowchart |

#### Reasoning

**From Flowchart:**
- Database query: *"Query param: query_reason = UNC"*

**From Functional Document:**
- *"The table ocms_adhoc_nro_queue logs records that must be submitted to MHA and/or DataHive for particulars checks that are not related to stage changes (e.g. UNC, TS-HST, or particulars not found)."*

**Purpose:**
- Tracks the reason why the ID was added to the queue
- Used for filtering during report generation
- Used for selective deletion after processing

**Values:**

| Value | Description | Use Case |
|-------|-------------|----------|
| UNC | Unclaimed | Unclaimed reminder letter processing |
| HST | HST ID | HST ID address verification |
| NA | Not Available | Particulars not found, need re-query |

**Why Needed:**
- **Filtering**: Unclaimed Batch Data Report only retrieves records where `query_reason = 'UNC'`
- **Deletion**: After processing, only delete records matching specific reason
- **Audit**: Track why each ID was queued

---

### 2.4 sent_to_mha_date

| Attribute | Value |
|-----------|-------|
| Data Type | datetime2(7) |
| Nullable | Yes |
| Source Reference | FD Section 2.4.5 |

#### Reasoning

**From Flowchart:**
- Step: *"CRON - MHA enquiry cron starts"*
- Step: *"Wait for MHA enquiry cron"*
- Step: *"Unclaimed Offender NRICs sent to MHA"*
- Note: *"Cron for MHA enquiry file will be modified to query for Notices entering the next stage and IDs in the ocms_adhoc_nro_queue"*

**Purpose:**
- Tracks when the record was included in the MHA enquiry file
- Indicates processing status for MHA

**Nullable Logic:**

| Value | Status | Action |
|-------|--------|--------|
| NULL | Pending | MHA cron should pick up this record |
| Has Value | Sent | MHA cron should skip this record |

**Why Needed:**
- **Prevent Duplicate Sends**: Cron filters `WHERE sent_to_mha_date IS NULL`
- **Status Tracking**: Know which records are pending vs sent
- **Audit Trail**: Record exact timestamp when sent to MHA
- **Error Recovery**: If MHA processing fails, can identify which records were affected

---

### 2.5 sent_to_datahive_date

| Attribute | Value |
|-----------|-------|
| Data Type | datetime2(7) |
| Nullable | Yes |
| Source Reference | FD Section 2.4.5 |

#### Reasoning

**From Flowchart:**
- Step: *"DataHive query for NRIC, FIN and UEN"*
- Step: *"DataHive results processed"*
- Note: *"The MHA result cron and DataHive FIN/UEN cron do not automatically run as a continuous sequence. They are separate crons which will be timed to run close to each other."*

**Purpose:**
- Tracks when the record was sent to DataHive
- Indicates processing status for DataHive

**Nullable Logic:**

| Value | Status | Action |
|-------|--------|--------|
| NULL | Pending | DataHive cron should pick up this record |
| Has Value | Sent | DataHive cron should skip this record |

**Why Separate from MHA:**
- MHA and DataHive are **separate cron jobs**
- They run at different times
- A record may be sent to MHA but not yet to DataHive (or vice versa)
- Need independent tracking for each external system

**Why Needed:**
- **Independent Processing**: Track MHA and DataHive separately
- **Prevent Duplicate Sends**: Each cron filters by its own date field
- **Flexible Scheduling**: Crons can run at different intervals
- **Error Recovery**: Identify which system had issues

---

### 2.6 Standard Audit Fields

#### 2.6.1 cre_date

| Attribute | Value |
|-----------|-------|
| Data Type | datetime2(7) |
| Nullable | No |

**Purpose:** Records when the queue entry was created

**Why Needed:**
- Audit trail for record creation
- Can be used to identify stale records (e.g., records older than X days)
- Debugging and troubleshooting

#### 2.6.2 cre_user_id

| Attribute | Value |
|-----------|-------|
| Data Type | varchar(50) |
| Nullable | No |

**Purpose:** Records who/what created the queue entry

**Sample Values:**
- `SYSTEM` - Created by automated process
- `JOHNLEE` - Created by user action

**Why Needed:**
- Audit trail
- Identify source of record (manual vs automated)

#### 2.6.3 upd_date

| Attribute | Value |
|-----------|-------|
| Data Type | datetime2(7) |
| Nullable | Yes |

**Purpose:** Records when the record was last updated

**Why Nullable:** New records have not been updated yet

**Why Needed:**
- Track when `sent_to_mha_date` or `sent_to_datahive_date` was populated
- Audit trail for modifications

#### 2.6.4 upd_user_id

| Attribute | Value |
|-----------|-------|
| Data Type | varchar(50) |
| Nullable | Yes |

**Purpose:** Records who/what last updated the record

**Why Nullable:** New records have not been updated yet

**Why Needed:**
- Audit trail
- Identify which cron/process updated the record

---

## 3. Process Flow Integration

### 3.1 Unclaimed Reminders Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     UNCLAIMED REMINDERS PROCESS                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  1. OIC submits Unclaimed Reminder Form                                      │
│                    │                                                         │
│                    ▼                                                         │
│  2. Backend identifies Unclaimed Offenders                                   │
│                    │                                                         │
│                    ▼                                                         │
│  3. INSERT into ocms_adhoc_nro_queue                                         │
│     ├─ id_no = <offender_id>                                                │
│     ├─ id_type = N/F/U                                                      │
│     ├─ query_reason = 'UNC'                                                 │
│     ├─ sent_to_mha_date = NULL                                              │
│     └─ sent_to_datahive_date = NULL                                         │
│                    │                                                         │
│                    ▼                                                         │
│  4. MHA Enquiry CRON runs                                                    │
│     ├─ SELECT WHERE sent_to_mha_date IS NULL                                │
│     ├─ Send to MHA                                                          │
│     └─ UPDATE sent_to_mha_date = CURRENT_TIMESTAMP                          │
│                    │                                                         │
│                    ▼                                                         │
│  5. MHA Results CRON runs                                                    │
│     ├─ Process MHA responses                                                │
│     └─ Store results in ocms_temp_unc_hst_addr                              │
│                    │                                                         │
│                    ▼                                                         │
│  6. DataHive CRON runs                                                       │
│     ├─ SELECT WHERE sent_to_datahive_date IS NULL                           │
│     ├─ Send to DataHive                                                     │
│     └─ UPDATE sent_to_datahive_date = CURRENT_TIMESTAMP                     │
│                    │                                                         │
│                    ▼                                                         │
│  7. Consolidate results in ocms_temp_unc_hst_addr                           │
│                    │                                                         │
│                    ▼                                                         │
│  8. Generate Unclaimed Batch Data Report                                     │
│                    │                                                         │
│                    ▼                                                         │
│  9. DELETE FROM ocms_adhoc_nro_queue WHERE query_reason = 'UNC'             │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 HST Flow Integration

The same table is used for HST ID verification with `query_reason = 'HST'`. The process is similar but triggered by HST-related workflows.

---

## 4. Data Dictionary Entry

```json
"ocms_adhoc_nro_queue": {
  "description": "Queue table for storing IDs to be submitted to MHA and/or DataHive for particulars checks not related to processing stage changes. Used for Unclaimed (UNC) and HST ID verification. Records are deleted after Batch Data Report is generated.",
  "columns": [
    {
      "name": "id_no",
      "type": "varchar(12)",
      "primary_key": true,
      "nullable": false,
      "default": null,
      "description": "Offender's ID number (NRIC/FIN/UEN) to be queried from MHA/DataHive for latest address.",
      "epic": "OCMS 20"
    },
    {
      "name": "id_type",
      "type": "varchar(1)",
      "primary_key": false,
      "nullable": false,
      "default": null,
      "description": "ID type indicator. Values: N (NRIC), F (FIN), U (UEN). Determines which external system to query.",
      "epic": "OCMS 20"
    },
    {
      "name": "query_reason",
      "type": "varchar(5)",
      "primary_key": false,
      "nullable": false,
      "default": null,
      "description": "Reason for the query. Values: UNC (Unclaimed), HST (HST ID check), NA (Particulars not found). Used to filter records for processing and deletion.",
      "epic": "OCMS 20"
    },
    {
      "name": "sent_to_mha_date",
      "type": "datetime2(7)",
      "primary_key": false,
      "nullable": true,
      "default": null,
      "description": "Date and time when record was sent to MHA enquiry cron. NULL indicates pending, value indicates sent.",
      "epic": "OCMS 20"
    },
    {
      "name": "sent_to_datahive_date",
      "type": "datetime2(7)",
      "primary_key": false,
      "nullable": true,
      "default": null,
      "description": "Date and time when record was sent to DataHive cron. NULL indicates pending, value indicates sent. Separate from MHA as they are different cron jobs.",
      "epic": "OCMS 20"
    },
    {
      "name": "cre_date",
      "type": "datetime2(7)",
      "primary_key": false,
      "nullable": false,
      "default": null,
      "description": "Date when the record was created (added to queue).",
      "epic": "OCMS 20"
    },
    {
      "name": "cre_user_id",
      "type": "varchar(50)",
      "primary_key": false,
      "nullable": false,
      "default": null,
      "description": "ID of the user/system that created the record.",
      "epic": "OCMS 20"
    },
    {
      "name": "upd_date",
      "type": "datetime2(7)",
      "primary_key": false,
      "nullable": true,
      "default": null,
      "description": "Date when the record was last updated.",
      "epic": "OCMS 20"
    },
    {
      "name": "upd_user_id",
      "type": "varchar(50)",
      "primary_key": false,
      "nullable": true,
      "default": null,
      "description": "ID of the user/system that last updated the record.",
      "epic": "OCMS 20"
    }
  ]
}
```

---

## 5. References

| Document | Section | Description |
|----------|---------|-------------|
| Functional_Flow_Unclaimed Reminders.drawio | Multiple tabs | Process flow showing queue usage |
| v1.1_OCMS_20_Unclaimed_HST.md | Section 2.4.5 | Table definition and field descriptions |
| plan_api_hst.md | Line 359 | HST flow reference to table |
| plan_condition.md | Lines 153, 167 | Pre-call conditions for adding to queue |

---

## 6. Open Questions

| ID | Question | Status |
|----|----------|--------|
| Q-001 | Table name inconsistency: `ocms_adhoc_queue` vs `ocms_adhoc_nro_queue` in FD | Needs clarification |
| Q-002 | Should `notice_no` be added as a field to link back to the source notice? | Needs clarification |
| Q-003 | Is composite PK (id_no + query_reason) needed to allow same ID for different reasons? | Needs clarification |

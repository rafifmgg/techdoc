# OCMS 14 – Notice Processing Flow for Deceased Offenders

**Prepared by**

[COMPANY LOGO]

[COMPANY NAME]

---

## Version History

| Version | Updated By | Date | Changes |
| --- | --- | --- | --- |
| v1.0 | [Author Name] | 13/01/2026 | Document Initiation - Section 3 Deceased Offenders |

---

## Table of Content

| Section | Content | Pages |
| --- | --- | --- |
| 3 | Notice Processing Flow for Deceased Offenders | [X] |
| 3.1 | Use Case | [X] |
| 3.2 | High Level Processing Flow | [X] |
| 3.3 | Detecting Deceased Offenders | [X] |
| 3.4 | Suspending Notices with Deceased Offenders | [X] |
| 3.5 | Updating Data for Notices with Deceased Offenders | [X] |
| 3.6 | Displaying Notices with PS-RIP and PS-RP2 in OCMS Staff Portal | [X] |
| 3.7 | RIP Hirer/Driver Furnished Report | [X] |
| 3.7.1 | Data Mapping | [X] |
| 3.7.2 | Success Outcome | [X] |
| 3.7.3 | Error Handling | [X] |
| 3.8 | Redirecting PS-RIP & PS-RP2 Notices | [X] |
| 3.8.1 | Data Mapping | [X] |
| 3.8.2 | Success Outcome | [X] |
| 3.8.3 | Error Handling | [X] |

---

# Section 3 – Notice Processing Flow for Deceased Offenders

## 3.1 Use Case

1. When MHA returns the NRIC offender Life Status as "D" (Deceased) or when a FIN holder is found in DataHive's "Dead Foreign Pass Holders" dataset, OCMS automatically updates the offender particulars with life status and date of death information.

2. OCMS automatically suspends the notice with PS-RIP or PS-RP2 based on date comparison logic:<br>a. If the deceased offender died on or after the offence date, apply PS-RIP (Motorist Deceased On or After Offence Date)<br>b. If the deceased offender died before the offence date, apply PS-RP2 (Motorist Deceased Before Offence Date)<br>c. PS-RIP and PS-RP2 suspensions can be applied at stages: NPA, eNA, ROV, RD1, RD2, RR3, DN1, DN2, DR3, CPC

3. When a notice is suspended with PS-RP2 and the deceased offender is a Hirer or Driver, the notice qualifies for inclusion in the "RIP Hirer/Driver Furnished Report":<br>a. A scheduled batch job generates the report daily at a configured time<br>b. The report includes notice number, offender name, NRIC/FIN, role (H/D), life status, date of death, offence date, and suspension date<br>c. The report is emailed to the OIC distribution list for review<br>d. If no qualifying records are found, no email is sent (expected behavior)

4. Officer-in-Charge (OIC) can revive PS-RIP or PS-RP2 suspensions and redirect notices to new offenders via OCMS Staff Portal:<br>a. OIC reviews the suspended notice and initiates revival by entering revival reason code and remarks<br>b. OIC updates the Owner, Hirer, or Driver particulars with the new offender details<br>c. Backend automatically triggers notice redirection workflow after successful particulars update<br>d. Notice is redirected to RD1 stage if new offender is Owner or Hirer, or to DN1 stage if new offender is Driver<br>e. Notice resumes standard processing workflow from the target stage

5. Notices with PS-RIP or PS-RP2 suspensions remain payable via eService and AXS channels but are not furnishable while the suspension is active.

6. Additional temporary suspensions (TS) and certain permanent suspensions (PS-FP, PS-PRA for CRS) can be applied to notices with active PS-RIP or PS-RP2 without requiring revival of the deceased offender suspension.

## 3.2 High Level Processing Flow

![High Level Flow](./images/section3-highlevel-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

**Flow Overview:**

The deceased offender processing flow is triggered when MHA returns life status "D" (Dead) for NRIC holders or when DataHive identifies a FIN holder in the "Dead Foreign Pass Holders" dataset. The system automatically updates offender particulars, determines the appropriate suspension code based on date comparison, and handles exception reporting and notice redirection workflows.

| Step | Process | Description |
| --- | --- | --- |
| 1 | Deceased Offender Detection | External systems (MHA for NRIC holders, DataHive for FIN holders) return deceased status. MHA provides life status through batch file processing (refer to OCMS 8 FD Section 3.2.2). DataHive provides deceased status through D90 dataset query (refer to OCMS 8 FD Section 7.5) |
| 2 | Update Offender Particulars | Backend updates offender record with life_status = 'D' and date_of_death. Update executes on ocms_offence_notice_owner_driver table where offender_indicator = 'Y' (current offender). Last_updated_by set to 'ocmsizmgr', last_updated_date set to SYSDATE |
| 3 | Suspension Code Determination | System compares date_of_death against offence_date to determine appropriate suspension code. Decision logic: If date_of_death >= offence_date then suspension_code = 'RIP', else suspension_code = 'RP2'. Validation checks: offence_date must exist (Rule SUS-001), date_of_death must exist (Rule SUS-002) |
| 4 | Apply PS-RIP Suspension | For deceased on or after offence date: System creates suspension record in ocms_suspended_notice with suspension_type = 'PS', reason_of_suspension = 'RIP'. Updates ocms_valid_offence_notice and eocms_valid_offence_notice with epr_reason_suspension = 'RIP'. Suspension can be applied at valid stages: NPA, eNA, ROV, RD1, RD2, RR3, DN1, DN2, DR3, CPC (Rule SUS-005) |
| 5 | Apply PS-RP2 Suspension | For deceased before offence date: System creates suspension record with reason_of_suspension = 'RP2'. Updates notice suspension fields with epr_reason_suspension = 'RP2'. If offender is Hirer or Driver (owner_driver_indicator IN ('H', 'D')), flag notice for RIP report generation (Condition C003) |
| 6 | Generate RIP Hirer/Driver Report | Daily scheduled batch job queries for PS-RP2 notices where deceased offender is Hirer or Driver. Report includes notice details, offender information, and relevant dates. System generates Excel or PDF report and sends via email to OIC distribution list. If no records found, process ends gracefully with no email sent (Rule RPT-007) |
| 7 | OIC Reviews and Redirects | OIC accesses suspended notice in Staff Portal. Initiates revival by submitting revival reason and remarks via POST /v1/suspension/revive API. System validates user authorization (Rule REV-001) and revival data. Upon success, suspension record updated with date_of_revival and officer_authorising_revival |
| 8 | Update New Offender Particulars | OIC submits new offender details via POST /v1/offender/update-particulars API. System validates all fields using OCMS 6 validation rules (Rules OFD-001 to OFD-010). Updates ocms_offence_notice_owner_driver and ocms_offence_notice_owner_driver_address with new particulars. Sets offender_indicator = 'N' initially |
| 9 | Notice Redirection Workflow | Backend automatically triggers redirection after successful particulars update. System updates old offender: offender_indicator = 'N'. Updates new offender: offender_indicator = 'Y'. Determines target processing stage based on owner_driver_indicator: 'RD1' if Owner or Hirer (Condition C004), 'DN1' if Driver (Condition C005) |
| 10 | Resume Standard Processing | Notice resumes from target stage (RD1 or DN1) following standard Notice lifecycle. Next_processing_date set to TRUNC(SYSDATE) for immediate processing. Updates synchronized between Intranet (ocms_valid_offence_notice) and Internet (eocms_valid_offence_notice) databases. Deceased offender record retained with offender_indicator = 'N' for audit trail |

**Special Processing Characteristics:**

- **Automatic Detection**: MHA and DataHive integrations automatically identify deceased offenders without manual intervention
- **Date-Based Logic**: Suspension code (RIP vs RP2) determined by comparing date of death against offence date
- **Exception Reporting**: RP2 cases where deceased was Hirer or Driver generate exception report for OIC review
- **Manual Redirection**: OIC-driven workflow for reviving suspensions and redirecting notices to new offenders
- **Notice Payability**: Notices remain payable despite active PS-RIP or PS-RP2 suspension
- **Database Synchronization**: All updates mirror between Intranet and Internet databases for consistency

## 3.3 Detecting Deceased Offenders

The system detects deceased offenders through two external integrations: MHA for NRIC holders and DataHive for FIN holders. Both integrations are managed by OCMS 8 backend processes.

### MHA Integration (NRIC Holders)

**Integration Type:** Batch file processing

**Data Source:** Ministry of Home Affairs (MHA)

**Reference:** OCMS 8 FD Section 3.2.2 "Process Output file returned by MHA"

**Process Flow:**

The MHA integration executes through scheduled batch processing. MHA returns a batch file containing life status information for NRIC holders. The backend processes this file and extracts life status indicators and dates of death for deceased individuals.

**Detection Logic:**

When MHA returns life_status = 'D' (Dead) in the batch file, the backend triggers the deceased offender workflow. The system extracts the associated date_of_death from the batch record.

**Data Processing:**

The backend updates the offender record in the database table ocms_offence_notice_owner_driver. The update sets life_status = 'D', date_of_death = value from MHA batch file, last_updated_by = 'ocmsizmgr', and last_updated_date = SYSDATE. The update applies only to records where offender_indicator = 'Y' (current offender) and the NRIC matches the batch file data.

**Validation Rule:**

Refer to Rule RIP-001 in plan_condition_section4.md: MHA Life Status Detection requires life_status returned by MHA to be 'D' (Dead). The detection is automatic with no error message as it is a passive data reception process.

### DataHive Integration (FIN Holders)

**Integration Type:** Database query or API call

**Data Source:** Immigration and Checkpoints Authority (ICA) DataHive platform

**Dataset:** D90 - "Dead Foreign Pass Holders"

**Reference:** OCMS 8 FD Section 7.5 "Process DataHive data for deceased FIN holders"

**Process Flow:**

The DataHive integration queries the D90 dataset to identify deceased foreign pass holders. The backend submits FIN holder lists to DataHive for verification against the deceased dataset.

**Detection Logic:**

When a FIN holder is found in the DataHive D90 "Dead Foreign Pass Holders" dataset, the backend triggers the deceased offender workflow. The system retrieves the date_of_death from the dataset record.

**Query Logic:**

```
IF FIN found in D90 dataset THEN
    life_status = 'D'
    Retrieve date_of_death from dataset
ELSE
    life_status = 'A'
    date_of_death = NULL
END IF
```

**Data Processing:**

The backend updates ocms_offence_notice_owner_driver with life_status = 'D', date_of_death = value from DataHive D90, last_updated_by = 'ocmsizmgr', and last_updated_date = SYSDATE. The update targets records where offender_indicator = 'Y' and the FIN matches the DataHive query result.

**Validation Rule:**

Refer to Rule RIP-002 in plan_condition_section4.md: DataHive Deceased Detection requires FIN to be found in DataHive D90 "Dead Foreign Pass Holders" dataset. The detection is automatic during DataHive query execution.

### Database Update Specification

**Table:** ocms_offence_notice_owner_driver

**Update Statement:**

```sql
UPDATE ocms_offence_notice_owner_driver
SET life_status = 'D',
    date_of_death = '[date_from_MHA_or_DataHive]',
    last_updated_by = 'ocmsizmgr',
    last_updated_date = SYSDATE
WHERE offender_id_no = '[NRIC_or_FIN]'
  AND offender_indicator = 'Y'
```

**Field Descriptions:**

- **life_status**: Offender life status indicator. Values: 'A' (Alive), 'D' (Dead)
- **date_of_death**: Date when offender deceased. Format: TIMESTAMP. Must not be future date (Rule RIP-003)
- **offender_indicator**: Current offender flag. Value 'Y' indicates current offender for the notice
- **last_updated_by**: Audit field. Value 'ocmsizmgr' indicates system update via batch process
- **last_updated_date**: Audit timestamp. Set to SYSDATE when update executes

**Post-Detection Trigger:**

After successful database update with life_status = 'D', the system automatically triggers the suspension workflow described in Section 3.4.

## 3.4 Suspending Notices with Deceased Offenders

When the system detects a deceased offender (life_status = 'D'), it automatically applies a permanent suspension to the notice. The suspension code is determined by comparing the date of death against the offence date.

### Suspension Code Determination Logic

**Decision Point:** Compare date_of_death vs offence_date

**Condition C001: PS-RIP (Motorist Deceased On or After Offence Date)**

**Description:** Apply PS-RIP suspension when the offender died on or after the date the offence occurred.

**Logic:**
```
IF date_of_death >= offence_date THEN
    suspension_code = 'RIP'
    suspension_type = 'PS'
END IF
```

**Trigger:** After MHA or DataHive returns life_status = 'D'

**Input Fields:**
- date_of_death (from MHA/DataHive)
- offence_date (from ocms_valid_offence_notice)

**Validation Rules:**
- Rule SUS-001: offence_date must exist in ocms_valid_offence_notice
- Rule SUS-002: date_of_death must exist in ocms_offence_notice_owner_driver
- Rule SUS-003: Apply PS-RIP if date_of_death >= offence_date

**Output:** Insert suspension record with reason_of_suspension = 'RIP', update notice suspension fields

**Condition C002: PS-RP2 (Motorist Deceased Before Offence Date)**

**Description:** Apply PS-RP2 suspension when the offender died BEFORE the date the offence occurred. This is an exception scenario indicating the deceased could not have committed the offence.

**Logic:**
```
IF date_of_death < offence_date THEN
    suspension_code = 'RP2'
    suspension_type = 'PS'
    IF owner_driver_indicator IN ('H', 'D') THEN
        FLAG for RIP report generation
    END IF
END IF
```

**Trigger:** After MHA or DataHive returns life_status = 'D'

**Input Fields:**
- date_of_death (from MHA/DataHive)
- offence_date (from ocms_valid_offence_notice)
- owner_driver_indicator (from ocms_offence_notice_owner_driver)

**Validation Rules:**
- Rule SUS-004: Apply PS-RP2 if date_of_death < offence_date
- Condition C003: RIP Report Eligibility check for Hirer/Driver cases

**Output:** Insert suspension record with reason_of_suspension = 'RP2', update notice suspension fields, flag for RIP report if offender is Hirer or Driver

### Database Updates for Suspension

**Table 1: ocms_valid_offence_notice (Intranet)**

**Update Statement:**
```sql
UPDATE ocms_valid_offence_notice
SET suspension_type = 'PS',
    epr_reason_suspension = '[RIP or RP2]',
    epr_reason_suspension_date = SYSDATE,
    last_updated_by = 'ocmsizmgr',
    last_updated_date = SYSDATE
WHERE notice_no = '[notice_number]'
```

**Field Descriptions:**
- **suspension_type**: Suspension classification. Value 'PS' indicates Permanent Suspension
- **epr_reason_suspension**: Suspension reason code. Values: 'RIP' (deceased on/after offence), 'RP2' (deceased before offence)
- **epr_reason_suspension_date**: Timestamp when suspension applied. Set to SYSDATE
- **last_updated_by**: Audit user. Value 'ocmsizmgr' for automated suspensions (Rule SUS-006)
- **last_updated_date**: Audit timestamp

**Table 2: eocms_valid_offence_notice (Internet)**

**Update Statement:**
```sql
UPDATE eocms_valid_offence_notice
SET suspension_type = 'PS',
    epr_reason_suspension = '[RIP or RP2]',
    epr_reason_suspension_date = SYSDATE,
    last_updated_by = 'ocmsizmgr',
    last_updated_date = SYSDATE
WHERE notice_no = '[notice_number]'
```

**Purpose:** Mirror the same suspension updates to Internet database for eService consistency.

**Table 3: ocms_suspended_notice**

**Insert Statement:**
```sql
INSERT INTO ocms_suspended_notice (
    notice_no,
    date_of_suspension,
    sr_no,
    suspension_source,
    suspension_type,
    reason_of_suspension,
    date_of_revival,
    revival_reason
) VALUES (
    '[notice_number]',
    SYSDATE,
    (SELECT NVL(MAX(sr_no), 0) + 1 FROM ocms_suspended_notice WHERE notice_no = '[notice_number]'),
    'Ocmsizmgr',
    'PS',
    '[RIP or RP2]',
    NULL,
    NULL
)
```

**Field Descriptions:**
- **notice_no**: Foreign key to ocms_valid_offence_notice
- **sr_no**: Suspension record sequence number. Calculated as MAX(sr_no) + 1 for the notice
- **date_of_suspension**: Timestamp when suspension created. Set to SYSDATE
- **suspension_source**: Source system. Value 'Ocmsizmgr' indicates automated suspension
- **suspension_type**: Value 'PS' (Permanent Suspension)
- **reason_of_suspension**: Suspension reason code. Values: 'RIP' or 'RP2'
- **date_of_revival**: Revival timestamp. NULL when suspension is active
- **revival_reason**: Revival reason code. NULL until suspension is revived

### Suspension Application Rules

**Valid Processing Stages:**

PS-RIP and PS-RP2 suspensions can be applied at the following notice processing stages:
- NPA (Notice Pending Assessment)
- eNA (Electronic Notice Assessment)
- ROV (Reminder to Owner)
- RD1 (Reminder to Driver 1)
- RD2 (Reminder to Driver 2)
- RR3 (Reminder to Re-register 3)
- DN1 (Demand Note 1)
- DN2 (Demand Note 2)
- DR3 (Demand to Re-register 3)
- CPC (Composition Payment Complete)

**Validation:** Rule SUS-005 checks that notice is at a valid processing stage before applying suspension.

**Notice Processing Behavior:**

| Condition | Rule | Description |
| --- | --- | --- |
| Notice is payable | Yes | Notice remains payable via eService and AXS channels despite active PS-RIP or PS-RP2 suspension |
| Notice is furnishable | No | Notice cannot be furnished while PS-RIP or PS-RP2 suspension is active. System blocks furnish driver/hirer functionality |
| Allow additional TS | Yes | Temporary Suspension (TS) can be applied to the notice when PS-RIP or PS-RP2 is active. Multiple suspensions can coexist |
| Allow additional PS (CRS) | Yes (WITHOUT revival) | CRS suspensions (PS-FP and PS-PRA) can be applied WITHOUT PS REVIVAL. EPR reason of suspension remains as PS-RIP or PS-RP2, and CRS reason of suspension updates as PS-FP or PS-PRA |
| Allow additional PS (non-CRS) | Yes (WITH revival) | For non-CRS suspensions (e.g., PS-APP) applied over PS-RIP or PS-RP2, PS revival IS REQUIRED before applying new suspension |

**User Authorization:**

- **OCMS Staff Portal Users:** Authorised OCMS users can manually apply PS-RIP or PS-RP2 suspensions using the Staff Portal with proper authorization role
- **PLUS Staff Portal Users:** PS-RIP and PS-RP2 are NOT displayed as suspension options in PLUS Staff Portal due to system boundary restrictions

## 3.5 Updating Data for Notices with Deceased Offenders

This section describes the data mapping and updates that occur when deceased offender processing executes.

### Data Mapping for Offender Particulars

**Source:** MHA Life Status Response or DataHive D90 Dataset

**Target:** ocms_offence_notice_owner_driver

| Source System | API/Source Field | Database Field | Transformation | Notes |
| --- | --- | --- | --- | --- |
| MHA | life_status | life_status | 'A' or 'D' | Direct mapping from batch file |
| MHA | date_of_death | date_of_death | TIMESTAMP format | Convert MHA date format to TIMESTAMP |
| DataHive | FIN in D90 dataset | life_status | 'D' if found, 'A' if not found | Conditional mapping based on dataset presence |
| DataHive | date_of_death (from D90) | date_of_death | TIMESTAMP format | Extract from D90 dataset record |
| System | N/A | last_updated_by | 'ocmsizmgr' | System audit user for automated updates |
| System | N/A | last_updated_date | SYSDATE | Current timestamp when update executes |

### Data Mapping for Suspension Records

**Source:** Date Comparison Logic

**Target:** ocms_valid_offence_notice, eocms_valid_offence_notice, ocms_suspended_notice

| Logic Output | Database Table | Database Field | Value | Notes |
| --- | --- | --- | --- | --- |
| Date comparison result | ocms_valid_offence_notice | suspension_type | 'PS' | Permanent Suspension |
| If date_of_death >= offence_date | ocms_valid_offence_notice | epr_reason_suspension | 'RIP' | Deceased on/after offence date |
| If date_of_death < offence_date | ocms_valid_offence_notice | epr_reason_suspension | 'RP2' | Deceased before offence date |
| Current timestamp | ocms_valid_offence_notice | epr_reason_suspension_date | SYSDATE | When suspension applied |
| Date comparison result | ocms_suspended_notice | reason_of_suspension | 'RIP' or 'RP2' | Matches epr_reason_suspension |
| Current timestamp | ocms_suspended_notice | date_of_suspension | SYSDATE | When suspension record created |
| System identifier | ocms_suspended_notice | suspension_source | 'Ocmsizmgr' | Automated suspension source |
| Suspension record sequence | ocms_suspended_notice | sr_no | MAX(sr_no) + 1 | Auto-increment per notice |

### Data Mapping for Suspension Revival

**Source:** Staff Portal API Request (POST /v1/suspension/revive)

**Target:** ocms_suspended_notice

| API Request Field | Database Field | Transformation | Validation Rule |
| --- | --- | --- | --- |
| revival_reason | revival_reason | Direct mapping (3-char code) | Must exist in OCMS_CODE_MASTER (Rule REV-005) |
| revival_remarks | revival_remarks | Direct mapping | Max 500 characters (Rule REV-007) |
| user_id | officer_authorising_revival | Direct mapping | User must have SUSPENSION_REVIVAL role (Rule REV-001) |
| Current timestamp | date_of_revival | SYSDATE | Timestamp when revival submitted |
| user_id | last_updated_by | Direct mapping | Audit field |
| Current timestamp | last_updated_date | SYSDATE | Audit timestamp |

**Patching ocms_valid_offence_notice:**

After revival, the system patches the notice suspension fields with the most recent active suspension, or sets to NULL if no active suspensions remain:

```sql
UPDATE ocms_valid_offence_notice
SET suspension_type = [most_recent_active_suspension_type OR NULL],
    epr_reason_suspension = [most_recent_active_reason OR NULL],
    epr_reason_suspension_date = [most_recent_active_date OR NULL],
    last_updated_by = '[user_id]',
    last_updated_date = SYSDATE
WHERE notice_no = '[notice_number]'
```

### Data Mapping for Notice Redirection

**Source:** Backend Automatic Redirection Workflow

**Target:** ocms_offence_notice_owner_driver, ocms_valid_offence_notice, eocms_valid_offence_notice

| Source Logic | Database Table | Database Field | Value | Notes |
| --- | --- | --- | --- | --- |
| Old offender record | ocms_offence_notice_owner_driver | offender_indicator | 'N' | Remove current offender status from deceased |
| New offender record | ocms_offence_notice_owner_driver | offender_indicator | 'Y' | Set new offender as current |
| If owner_driver_indicator IN ('O', 'H') | ocms_valid_offence_notice | next_processing_stage | 'RD1' | Redirect to Reminder to Driver 1 (Condition C004) |
| If owner_driver_indicator = 'D' | ocms_valid_offence_notice | next_processing_stage | 'DN1' | Redirect to Demand Note 1 (Condition C005) |
| Current date | ocms_valid_offence_notice | next_processing_date | TRUNC(SYSDATE) | Immediate processing |
| System user | ocms_valid_offence_notice | last_updated_by | 'ocmsizmgr' | Automated redirection |
| Current timestamp | ocms_valid_offence_notice | last_updated_date | SYSDATE | Audit timestamp |

**Database Synchronization:**

All updates to ocms_valid_offence_notice (Intranet) must synchronize to eocms_valid_offence_notice (Internet) to maintain consistency. Synchronization failure triggers Rule RED-006 validation and logs CRITICAL error.

**Audit Trail:**

Redirection actions are logged to ocms_audit_log table:

```sql
INSERT INTO ocms_audit_log (
    log_id,
    notice_no,
    action_type,
    action_description,
    old_offender_id,
    new_offender_id,
    target_processing_stage,
    created_by,
    created_date
) VALUES (
    [generated_log_id],
    '[notice_number]',
    'NOTICE_REDIRECTION',
    'Notice redirected from deceased offender to new offender',
    '[old_offender_id]',
    '[new_offender_id]',
    '[RD1 or DN1]',
    'ocmsizmgr',
    SYSDATE
)
```

## 3.6 Displaying Notices with PS-RIP and PS-RP2 in OCMS Staff Portal

Notices with active PS-RIP or PS-RP2 suspensions display special indicators in the OCMS Staff Portal to alert officers about the deceased offender status.

### Display Indicators

**RIP Superscript:**

Notices with PS-RIP or PS-RP2 suspensions display a superscript "R" indicator next to the notice number in the Staff Portal notice list and detail views. This visual marker helps officers quickly identify notices related to deceased offenders.

**Suspension Status:**

The notice detail page displays the full suspension information:
- **Suspension Type:** PS (Permanent Suspension)
- **Suspension Reason:** RIP (Motorist Deceased On or After Offence Date) or RP2 (Motorist Deceased Before Offence Date)
- **Suspension Date:** Date when suspension was applied
- **Suspension Source:** Ocmsizmgr (automated system suspension)

### Notice Accessibility

**Viewing Permissions:**

All authorized OCMS Staff Portal users can view notices with PS-RIP or PS-RP2 suspensions. No special permissions are required to view suspended notices.

**Search and Filter:**

Staff Portal search functionality includes filters for suspension types:
- Filter by Suspension Type: PS
- Filter by Suspension Reason: RIP, RP2
- Filter by Suspension Date Range

### Notice Actions

**Available Actions:**

For notices with active PS-RIP or PS-RP2 suspensions:

| Action | Available | Permission Required | Notes |
| --- | --- | --- | --- |
| View Notice Details | Yes | Standard user | All users can view |
| Make Payment | Yes (eService) | Public | Notice remains payable |
| Furnish Driver/Hirer | No | N/A | Blocked while suspended |
| Revive Suspension | Yes | SUSPENSION_REVIVAL | OIC only (Rule REV-001) |
| Update Offender Particulars | Yes | UPDATE_OFFENDER_PARTICULARS | OIC only (Rule OFD-001) |
| Apply Additional TS | Yes | TEMPORARY_SUSPENSION | Authorized users |
| Apply Additional PS (CRS) | Yes (without revival) | PERMANENT_SUSPENSION | PS-FP, PS-PRA allowed |
| Apply Additional PS (non-CRS) | Yes (with revival) | PERMANENT_SUSPENSION | Must revive PS-RIP/PS-RP2 first |

**Blocked Actions:**

The following actions are blocked while PS-RIP or PS-RP2 suspension is active:
- Furnish Driver Particulars
- Furnish Hirer Particulars
- Progress notice to next processing stage (automatic progression stopped)
- Generate eNotification (eNA stage bypassed)

### Post-Revival Display

After OIC revives a PS-RIP or PS-RP2 suspension:

**Display Changes:**
- RIP superscript "R" is removed from notice display
- Suspension Status shows: Date of Revival, Revival Reason, Officer Authorising Revival
- If no other active suspensions exist, suspension_type becomes NULL in notice fields

**Notice Behavior:**
- Notice becomes furnishable if at appropriate processing stage (RD1, RD2, DN1, DN2)
- Notice can progress to next processing stage according to standard workflow
- Notice history retains suspension record for audit purposes

## 3.7 RIP Hirer/Driver Furnished Report

The RIP Hirer/Driver Furnished Report is an exception report that identifies notices suspended with PS-RP2 where the deceased offender is a Hirer or Driver. This scenario indicates the deceased could not have committed the offence (died before offence date) but was previously furnished as the responsible party.

![RIP Report Flow](./images/section3-rip-report-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

### Report Generation Workflow

| Step | Description | Brief Description |
| --- | --- | --- |
| CRON Job Trigger | Scheduled CRON job triggers daily at configured time. Job name: GenerateRIPReportCron. Schedule timing TBD with users | Scheduled daily execution |
| Query RP2 Suspended Notices | System executes query to retrieve notices suspended with PS-RP2 where deceased offender is Hirer or Driver. Query applies multiple filter conditions (Condition C003) | Retrieve eligible records |
| Check Records Found | Decision point: If query returns result set, proceed to report generation. If no records found, log informational message and end gracefully | Validate query results |
| Consolidate Data | For qualifying records: System extracts notice number, offender name, NRIC/FIN, owner/driver indicator, life status, date of death, offence date, suspension date. Data sorted by notice_no ascending | Prepare report data |
| Format Report Data | System formats data according to report template specification. Template format: Excel (.xlsx) or PDF based on configuration. Columns: Notice Number, Offender Name, NRIC/FIN, Role (H/D), Life Status, Date of Death, Offence Date, Suspension Date | Generate report file |
| Generate Report File | System creates report file with standardized filename: RIP_Hirer_Driver_Furnished_Report_YYYYMMDD_HHMMSS.xlsx. File stored in configured directory for archival and manual retrieval | Create output file |
| Send Email to OIC | System sends email to OIC distribution list via SMTP. Email subject: "RIP Hirer/Driver Furnished Report - [Generation Date]". Email body includes summary with record count. Report file attached to email | Deliver report |
| Log Success or Error | System logs execution results. Success: Log timestamp, record count, file path, email delivery status. Error: Log error type, exception details, failed notice numbers if partial failure | Record execution status |

### Report Query Specification

**Query:** Get RP2 Suspended Notices with Deceased Hirer/Driver

```sql
SELECT
    sn.notice_no,
    onod.offender_name,
    onod.offender_id_no,
    onod.owner_driver_indicator,
    onod.life_status,
    onod.date_of_death,
    von.offence_date,
    sn.date_of_suspension
FROM ocms_suspended_notice sn
JOIN ocms_offence_notice_owner_driver onod
    ON sn.notice_no = onod.notice_no
JOIN ocms_valid_offence_notice von
    ON sn.notice_no = von.notice_no
WHERE sn.suspension_type = 'PS'
  AND sn.reason_of_suspension = 'RP2'
  AND TRUNC(sn.date_of_suspension) = TRUNC(SYSDATE)
  AND (sn.due_date_of_revival IS NULL OR sn.due_date_of_revival = '')
  AND onod.owner_driver_indicator IN ('H', 'D')
  AND onod.offender_indicator = 'Y'
  AND onod.life_status = 'D'
ORDER BY sn.notice_no ASC
```

**Query Filter Conditions (Condition C003: RIP Report Eligibility):**

| Filter Condition | Rule ID | Description |
| --- | --- | --- |
| suspension_type = 'PS' | RPT-001 | Permanent Suspension only |
| reason_of_suspension = 'RP2' | RPT-001 | Deceased before offence date |
| TRUNC(date_of_suspension) = TRUNC(SYSDATE) | RPT-002 | Suspended today |
| due_date_of_revival IS NULL OR = '' | RPT-006 | Not yet revived |
| owner_driver_indicator IN ('H', 'D') | RPT-003 | Hirer or Driver only |
| offender_indicator = 'Y' | RPT-004 | Current offender |
| life_status = 'D' | RPT-005 | Deceased status |

### Email Specification

**Email Configuration:**

| Field | Value |
| --- | --- |
| To | [OIC email distribution list - configured in system] |
| Subject | RIP Hirer/Driver Furnished Report - [Generation Date] |
| Attachment | RIP_Hirer_Driver_Furnished_Report_YYYYMMDD_HHMMSS.xlsx |

**Email Body Template:**

```
Dear OIC,

Please find attached the RIP Hirer/Driver Furnished Report for [Date].

Total records: [X]

This report contains notices that were suspended with PS-RP2 today where
the deceased offender is a Hirer or Driver (exception scenario requiring review).

Best regards,
OCMS System
```

**Validation Rule:**

Rule RPT-008: Email distribution list must be configured in the system. If OIC email list is not configured, the system logs error "Email distribution list not configured" and preserves the report file for manual distribution.

**No Records Handling:**

If the query returns 0 records (no qualifying notices found), the system:
- Logs informational message: "No RIP records found"
- Does NOT send email (expected behavior per Rule RPT-007)
- Ends CRON execution with SUCCESS status
- Updates CRON execution log with count = 0

### 3.7.1 Data Mapping

**Report Output Columns:**

| Report Column | Database Table | Database Field | Description |
| --- | --- | --- | --- |
| Notice Number | ocms_suspended_notice | notice_no | Unique notice identifier |
| Offender Name | ocms_offence_notice_owner_driver | offender_name | Full name of deceased offender |
| NRIC/FIN | ocms_offence_notice_owner_driver | offender_id_no | National ID or FIN |
| Role (H/D) | ocms_offence_notice_owner_driver | owner_driver_indicator | H = Hirer, D = Driver |
| Life Status | ocms_offence_notice_owner_driver | life_status | Value: 'D' (Deceased) |
| Date of Death | ocms_offence_notice_owner_driver | date_of_death | When offender deceased |
| Offence Date | ocms_valid_offence_notice | offence_date | When offence occurred |
| Suspension Date | ocms_suspended_notice | date_of_suspension | When PS-RP2 applied |

**Data Source Attribution:**

- **ocms_suspended_notice**: Provides suspension details (notice_no, reason_of_suspension, date_of_suspension)
- **ocms_offence_notice_owner_driver**: Provides offender particulars (name, ID, role, life status, date of death)
- **ocms_valid_offence_notice**: Provides offence details (offence_date)

**Database Join Relationships:**

- ocms_suspended_notice.notice_no → ocms_offence_notice_owner_driver.notice_no
- ocms_suspended_notice.notice_no → ocms_valid_offence_notice.notice_no

### 3.7.2 Success Outcome

- CRON job successfully triggers at scheduled daily time without execution errors
- Database query successfully executes and retrieves all qualifying RP2 suspended notices
- If no qualifying records found: CRON logs informational message and completes successfully with count = 0, no email sent (expected behavior)
- If qualifying records found: System consolidates data for all records
- Report file successfully generated in Excel or PDF format with standardized filename
- Report file saved to configured directory for archival
- Email successfully composed with correct subject, body template, and record count
- Report file successfully attached to email
- Email successfully delivered to all recipients in OIC distribution list
- CRON execution log successfully updated with execution results: timestamp, total count, success count, file path, email delivery status
- System logs SUCCESS status with execution duration
- The workflow reaches the End state without triggering unhandled error paths

### 3.7.3 Error Handling

#### Application Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Database Connection Failure | Unable to connect to Intranet database during query execution | System logs CRITICAL error, terminates CRON execution, notifies admin via alert mechanism. CRON marked as FAILED. Automatic retry in next scheduled execution |
| Query Execution Error | SQL query fails due to syntax error, missing table, or database timeout | System logs error with SQL exception details, terminates CRON execution, notifies admin. CRON marked as FAILED |
| Report Generation Failure | Excel or PDF file creation fails due to file I/O exception or template error | System logs error, preserves query data in memory or temp file, marks job as PARTIAL_FAILURE, notifies admin. Manual report generation required |
| File System Write Failure | Unable to save report file to configured directory due to permissions or disk space | System logs error with file path and exception, preserves data, marks job as PARTIAL_FAILURE. Manual intervention required to resolve file system issue |
| Email Delivery Failure | SMTP server unavailable, authentication failure, or network timeout | System logs error with SMTP error code, preserves report file for manual retrieval, marks job as PARTIAL_FAILURE, notifies admin. Report file accessible in configured directory |
| Email Distribution List Missing | OIC email list configuration is null or empty | System logs error "Email distribution list not configured" (Rule RPT-008), preserves report file, marks job as PARTIAL_FAILURE, notifies admin to configure email list |
| Partial Email Delivery | Some recipients receive email successfully, others fail | System logs warning with failed recipient addresses, marks job as PARTIAL_FAILURE. Successfully delivered emails do not resend |
| Zero Records - Normal | Query returns empty result set (no qualifying notices for the day) | System logs INFO message "No RIP records found", ends CRON execution with SUCCESS status and count = 0. No email sent (expected behavior per Rule RPT-007) |

#### Error Codes and Resolution

| HTTP Status | Error Code | Description | Resolution |
| --- | --- | --- | --- |
| N/A (Internal CRON) | CRON-DB-001 | Database connection failure | Check database connectivity, verify connection pool settings, retry in next scheduled execution |
| N/A (Internal CRON) | CRON-QUERY-002 | Query execution error | Review SQL query syntax, check table access permissions, verify database schema |
| N/A (Internal CRON) | CRON-RPT-003 | Report generation failure | Check report template configuration, verify file generation library dependencies |
| N/A (Internal CRON) | CRON-FS-004 | File system write failure | Verify directory permissions, check disk space availability, confirm file path configuration |
| N/A (Internal CRON) | CRON-EMAIL-005 | Email delivery failure | Verify SMTP server availability, check authentication credentials, confirm network connectivity |
| N/A (Internal CRON) | CRON-CFG-006 | Email distribution list not configured | Update application configuration with OIC email addresses, restart CRON scheduler |

#### Monitoring and Alerting

| Alert Condition | Alert Level | Alert Mechanism | Recipient |
| --- | --- | --- | --- |
| CRON Fails to Execute | CRITICAL | Email + SMS | Technical team lead, Database admin |
| Database Connection Failure | CRITICAL | Email + SMS | Technical team lead, Database admin |
| Report Generation Failure | WARNING | Email | Technical team, Operations team |
| Email Delivery Failure | WARNING | Email | Technical team, OIC supervisor |
| 3 Consecutive Execution Failures | CRITICAL | Email + SMS + Incident ticket | Technical team lead, IT manager |
| Execution Duration > 10 minutes | WARNING | Email | Technical team, Database admin |

**Internal Logging:**

All CRON execution events are logged to application log:

```
[TIMESTAMP] [INFO/WARN/ERROR] [GenerateRIPReportCron] [Message]

Examples:
2026-01-13 23:00:00 [INFO] [GenerateRIPReportCron] CRON started
2026-01-13 23:00:05 [INFO] [GenerateRIPReportCron] Query executed: 5 records found
2026-01-13 23:00:10 [INFO] [GenerateRIPReportCron] Report generated: RIP_Hirer_Driver_Furnished_Report_20260113_230010.xlsx
2026-01-13 23:00:15 [INFO] [GenerateRIPReportCron] Email sent successfully to 3 recipients
2026-01-13 23:00:16 [INFO] [GenerateRIPReportCron] CRON completed: Total=5, Success=5, File=RIP_Hirer_Driver_Furnished_Report_20260113_230010.xlsx
```

## 3.8 Redirecting PS-RIP & PS-RP2 Notices

The notice redirection workflow enables OIC to revive PS-RIP or PS-RP2 suspensions and redirect notices to new offenders when investigation determines the deceased person was not the actual offender. This workflow involves two sequential actions: suspension revival and offender particulars update.

![Redirect RIP Notices Flow](./images/section3-redirect-rip-flow.png)

NOTE: Due to page size limit, the full-sized image is appended.

### Redirection Workflow

| Step | Description | Brief Description |
| --- | --- | --- |
| OIC Reviews Notice | OIC accesses OCMS Staff Portal and navigates to notice detail page. Reviews suspended notice with PS-RIP or PS-RP2 to determine if redirection is appropriate | Manual review process |
| Revive Suspension | OIC clicks "Revive Suspension" button in Suspension tab. Enters revival_reason code from dropdown (3-character code from OCMS_CODE_MASTER). Enters revival_remarks text (max 500 characters). Submits revival request | Submit revival request |
| Validate Revival Request | Backend validates user authorization (Rule REV-001: User must have SUSPENSION_REVIVAL role). Validates revival_reason exists in code table (Rule REV-005). Validates suspension exists and not already revived (Rule REV-004). Validates notice exists (Rule REV-002) | Backend validation |
| Update Suspension Record | System updates ocms_suspended_notice table. Sets date_of_revival = SYSDATE, revival_reason = submitted code, officer_authorising_revival = user_id, revival_remarks = submitted text. WHERE notice_no AND sr_no AND date_of_revival IS NULL (Rule REV-004) | Update suspension |
| Patch Notice Suspension Fields | System patches ocms_valid_offence_notice and eocms_valid_offence_notice. Updates suspension fields to most recent active suspension OR NULL if no active suspensions. Synchronizes between Intranet and Internet databases | Update notice fields |
| Display Revival Success | Staff Portal displays success message: "Suspension revived successfully". Shows revival details: date_of_revival, revival_reason, officer name | Confirm revival |
| Update Offender Particulars | OIC navigates to Owner/Driver/Hirer Details tab. Clicks "Update Particulars" button. Enters or modifies offender details: owner_driver_indicator (O/H/D), offender_name, offender_id_type (NRIC/FIN/PASSPORT), offender_id_no, date_of_birth, address fields, contact_no (optional), email (optional) | Enter new offender data |
| Validate Particulars Data | Backend validates using OCMS 6 validation rules (refer to OCMS 6 FD Section 4). Checks: NRIC/FIN checksum (Rule OFD-003, OFD-004), mandatory fields (Rule OFD-005), valid owner/driver indicator (Rule OFD-006), duplicate check (Rule OFD-007), email format (Rule OFD-008 if provided), phone format (Rule OFD-009 if provided), address completeness (Rule OFD-010) | Validate input data |
| Insert/Update Offender Record | System inserts or updates ocms_offence_notice_owner_driver. Sets offender_indicator = 'N' initially (will be updated to 'Y' during redirection). Inserts address into ocms_offence_notice_owner_driver_address. Sets created_by = user_id | Create offender record |
| Auto-Trigger Redirection | Backend automatically triggers redirection workflow after successful particulars update. No manual trigger required from OIC (Rule RED-001) | Automatic redirection |
| Determine Target Stage | System evaluates owner_driver_indicator of new offender. If IN ('O', 'H'): target_stage = 'RD1' (Condition C004: Redirect to RD1). If = 'D': target_stage = 'DN1' (Condition C005: Redirect to DN1). Validates valid stage mapping (Rule RED-002) | Determine processing stage |
| Update Offender Indicators | System updates old deceased offender: offender_indicator = 'N' WHERE notice_no AND life_status = 'D' AND offender_indicator = 'Y'. Updates new offender: offender_indicator = 'Y' WHERE notice_no AND offender_id_no = new_offender_id. Validates single current offender per notice (Rule RED-004) | Swap offender status |
| Update Notice Processing Stage | System updates ocms_valid_offence_notice (Intranet). Sets next_processing_stage = RD1 or DN1, next_processing_date = TRUNC(SYSDATE) for immediate processing. Updates last_updated_by = 'ocmsizmgr', last_updated_date = SYSDATE | Set target stage |
| Synchronize to Internet DB | System updates eocms_valid_offence_notice (Internet) with same processing stage fields. Validates synchronization success (Rule RED-006: Log CRITICAL if sync fails) | Mirror to Internet DB |
| Log Audit Trail | System inserts redirection event into ocms_audit_log. Records: action_type = 'NOTICE_REDIRECTION', old_offender_id, new_offender_id, target_processing_stage, created_by = 'ocmsizmgr' | Create audit record |
| Display Redirection Success | Staff Portal displays success message: "Notice redirected to [Target Stage]". Shows redirection details: new offender name, new offender ID, target processing stage, next processing date | Confirm redirection |
| Resume Standard Processing | Notice resumes standard workflow from target stage (RD1 or DN1). Next CRON cycle picks up notice for processing. Deceased offender record retained with offender_indicator = 'N' for audit trail. RIP superscript "R" removed from Staff Portal display | Notice processing resumes |

### API Specification for Suspension Revival

**Endpoint:** POST /v1/suspension/revive

**Description:** OIC revives PS-RIP or PS-RP2 suspension from OCMS Staff Portal

**Authorization:** Required - User must have 'SUSPENSION_REVIVAL' permission (Rule REV-001)

**Request Payload:**

```json
{
  "notice_no": "500500303J",
  "suspension_sr_no": 1,
  "revival_reason": "PSR",
  "revival_remarks": "Permanent suspension revival - redirect to new offender",
  "user_id": "JOHNLEE"
}
```

**Request Field Validations:**

| Field | Type | Required | Validation Rule | Error Message |
| --- | --- | --- | --- | --- |
| notice_no | string | Yes | Max 20 chars, not empty | Notice number is required |
| suspension_sr_no | integer | Yes | Min: 1 | Suspension serial number is required |
| revival_reason | string | Yes | 3 chars, must exist in OCMS_CODE_MASTER (Rule REV-005) | Invalid revival reason code. Please select from dropdown |
| revival_remarks | string | No | Max 500 chars (Rule REV-007) | Revival remarks exceed maximum length (500 characters) |
| user_id | string | Yes | Max 50 chars, must have SUSPENSION_REVIVAL role (Rule REV-001) | You do not have permission to revive suspensions |

**Response (Success):**

```json
{
  "HTTPStatusCode": "200",
  "HTTPStatusDescription": "Success",
  "data": {
    "appCode": "OCMS-2000",
    "message": "Suspension revived successfully",
    "notice_no": "500500303J",
    "suspension_sr_no": 1,
    "date_of_revival": "2025-03-22T19:00:02"
  }
}
```

**Response (Error - Unauthorized):**

```json
{
  "HTTPStatusCode": "403",
  "HTTPStatusDescription": "Forbidden",
  "data": {
    "appCode": "OCMS-4030",
    "message": "You do not have permission to revive suspensions"
  }
}
```

**Response (Error - Invalid Revival Reason):**

```json
{
  "HTTPStatusCode": "400",
  "HTTPStatusDescription": "Bad Request",
  "data": {
    "appCode": "OCMS-4000",
    "message": "Invalid revival reason code. Please select from dropdown"
  }
}
```

**Response (Error - Suspension Already Revived):**

```json
{
  "HTTPStatusCode": "400",
  "HTTPStatusDescription": "Bad Request",
  "data": {
    "appCode": "OCMS-4000",
    "message": "Suspension has already been revived"
  }
}
```

### API Specification for Offender Particulars Update

**Endpoint:** POST /v1/offender/update-particulars

**Description:** OIC updates Owner, Hirer, or Driver particulars from OCMS Staff Portal for notice redirection

**Authorization:** Required - User must have 'UPDATE_OFFENDER_PARTICULARS' permission (Rule OFD-001)

**Note:** Refer to OCMS 6 Functional Document Section 4 for detailed API specification, validation rules, and field mappings.

**Request Payload:**

```json
{
  "notice_no": "500500303J",
  "offender_action": "ADD_NEW_OFFENDER",
  "offender_data": {
    "owner_driver_indicator": "D",
    "offender_name": "TAN AH KOW",
    "offender_id_type": "NRIC",
    "offender_id_no": "S1234567D",
    "date_of_birth": "1980-05-15",
    "address": {
      "block": "123",
      "street": "TAMPINES STREET 45",
      "unit": "12-345",
      "postal_code": "520123",
      "country": "SINGAPORE"
    },
    "contact_no": "91234567",
    "email": "tan@example.com"
  },
  "user_id": "JOHNLEE"
}
```

**Request Field Validations:**

Refer to OCMS 6 Functional Document Section 4 for complete validation rules. Key validations include:

| Field | Validation Rule | Error Message |
| --- | --- | --- |
| owner_driver_indicator | Must be 'O', 'H', or 'D' (Rule OFD-006) | Invalid owner/driver/hirer indicator |
| offender_name | Mandatory (Rule OFD-005) | Name is mandatory |
| offender_id_no | NRIC checksum or FIN format (Rules OFD-003, OFD-004) | Invalid NRIC checksum / Invalid FIN format |
| offender_id_no | No duplicate current offender (Rule OFD-007) | Offender with ID S1234567D is already designated as current offender |

**Response (Success):**

```json
{
  "HTTPStatusCode": "200",
  "HTTPStatusDescription": "Success",
  "data": {
    "appCode": "OCMS-2000",
    "message": "Offender particulars updated successfully",
    "notice_no": "500500303J",
    "offender_id_no": "S1234567D",
    "redirection_triggered": true
  }
}
```

**Response (Error - Validation Failure):**

```json
{
  "HTTPStatusCode": "400",
  "HTTPStatusDescription": "Bad Request",
  "data": {
    "appCode": "OCMS-4000",
    "message": "Validation failed",
    "errors": [
      {
        "field": "offender_id_no",
        "message": "Invalid NRIC checksum"
      },
      {
        "field": "offender_name",
        "message": "Name is mandatory"
      }
    ]
  }
}
```

**Response (Error - Duplicate Offender):**

```json
{
  "HTTPStatusCode": "400",
  "HTTPStatusDescription": "Bad Request",
  "data": {
    "appCode": "OCMS-4000",
    "message": "Offender with ID S1234567D is already designated as current offender"
  }
}
```

### Automatic Redirection Logic

**Trigger:** Successful offender particulars update via Staff Portal

**Processing Stage Determination:**

**Condition C004: Redirect to RD1 (Reminder to Driver 1)**

For new offenders who are Owner or Hirer:

```
IF new_offender.owner_driver_indicator IN ('O', 'H') THEN
    UPDATE ocms_valid_offence_notice
    SET next_processing_stage = 'RD1',
        next_processing_date = TRUNC(SYSDATE)
    WHERE notice_no = [notice_number]
END IF
```

**Condition C005: Redirect to DN1 (Demand Note 1)**

For new offenders who are Driver:

```
IF new_offender.owner_driver_indicator = 'D' THEN
    UPDATE ocms_valid_offence_notice
    SET next_processing_stage = 'DN1',
        next_processing_date = TRUNC(SYSDATE)
    WHERE notice_no = [notice_number]
END IF
```

**Validation Rules:**

- Rule RED-001: Backend automatically triggers redirection after particulars update
- Rule RED-002: Valid stage mapping - owner_driver_indicator must be 'O', 'H', or 'D'
- Rule RED-003: Offender indicator update - old = 'N', new = 'Y'
- Rule RED-004: Single current offender - only one offender per notice can have offender_indicator = 'Y'
- Rule RED-005: Next processing date set to TRUNC(SYSDATE) for immediate processing
- Rule RED-006: Database sync - updates must synchronize to both ocms and eocms databases

### 3.8.1 Data Mapping

**Data Mapping for Suspension Revival:**

| API Request Field | Database Table | Database Field | Notes |
| --- | --- | --- | --- |
| notice_no | ocms_suspended_notice | notice_no | WHERE condition |
| suspension_sr_no | ocms_suspended_notice | sr_no | WHERE condition |
| revival_reason | ocms_suspended_notice | revival_reason | 3-character code from OCMS_CODE_MASTER |
| revival_remarks | ocms_suspended_notice | revival_remarks | Free text, max 500 characters |
| user_id | ocms_suspended_notice | officer_authorising_revival | OIC user ID |
| SYSDATE | ocms_suspended_notice | date_of_revival | Timestamp when revived |
| user_id | ocms_suspended_notice | last_updated_by | Audit field |
| SYSDATE | ocms_suspended_notice | last_updated_date | Audit timestamp |

**Data Mapping for Notice Suspension Fields Patch:**

After revival, patch ocms_valid_offence_notice and eocms_valid_offence_notice:

| Logic | Database Table | Database Field | Value |
| --- | --- | --- | --- |
| Most recent active suspension OR NULL | ocms_valid_offence_notice | suspension_type | 'PS' or NULL if no active suspensions |
| Most recent active reason OR NULL | ocms_valid_offence_notice | epr_reason_suspension | Suspension code or NULL |
| Most recent active date OR NULL | ocms_valid_offence_notice | epr_reason_suspension_date | TIMESTAMP or NULL |
| User ID | ocms_valid_offence_notice | last_updated_by | OIC user ID |
| SYSDATE | ocms_valid_offence_notice | last_updated_date | Update timestamp |

**Data Mapping for Offender Particulars Update:**

Refer to OCMS 6 Functional Document Section 4 for complete data mapping. Key mappings:

| API Request Field | Database Table | Database Field | Initial Value | Notes |
| --- | --- | --- | --- | --- |
| offender_data.owner_driver_indicator | ocms_offence_notice_owner_driver | owner_driver_indicator | 'O', 'H', or 'D' | Determines redirect target stage |
| offender_data.offender_name | ocms_offence_notice_owner_driver | offender_name | Submitted name | Mandatory |
| offender_data.offender_id_no | ocms_offence_notice_owner_driver | offender_id_no | Submitted ID | NRIC/FIN with checksum validation |
| offender_data.date_of_birth | ocms_offence_notice_owner_driver | date_of_birth | YYYY-MM-DD | Mandatory |
| user_id | ocms_offence_notice_owner_driver | created_by | OIC user ID | Audit field |
| SYSDATE | ocms_offence_notice_owner_driver | created_date | Creation timestamp | Audit field |
| N/A (auto-set) | ocms_offence_notice_owner_driver | offender_indicator | 'N' initially | Updated to 'Y' during redirection |
| N/A (auto-set) | ocms_offence_notice_owner_driver | life_status | 'A' | Assume alive until MHA/DataHive check |

**Data Mapping for Notice Redirection:**

| Logic Source | Database Table | Database Field | Value | Notes |
| --- | --- | --- | --- | --- |
| Old deceased offender | ocms_offence_notice_owner_driver | offender_indicator | 'N' | WHERE life_status = 'D' AND offender_indicator = 'Y' |
| New offender | ocms_offence_notice_owner_driver | offender_indicator | 'Y' | WHERE offender_id_no = new_offender_id |
| Condition C004 or C005 | ocms_valid_offence_notice | next_processing_stage | 'RD1' or 'DN1' | Based on owner_driver_indicator |
| TRUNC(SYSDATE) | ocms_valid_offence_notice | next_processing_date | Current date | Immediate processing |
| System user | ocms_valid_offence_notice | last_updated_by | 'ocmsizmgr' | Automated redirection |
| SYSDATE | ocms_valid_offence_notice | last_updated_date | Current timestamp | Audit timestamp |

**Data Mapping for Audit Trail:**

| Data Source | Database Table | Database Field | Value |
| --- | --- | --- | --- |
| Generated | ocms_audit_log | log_id | System-generated unique ID |
| Notice | ocms_audit_log | notice_no | Notice number |
| Constant | ocms_audit_log | action_type | 'NOTICE_REDIRECTION' |
| Constant | ocms_audit_log | action_description | 'Notice redirected from deceased offender to new offender' |
| Old offender | ocms_audit_log | old_offender_id | Deceased offender NRIC/FIN |
| New offender | ocms_audit_log | new_offender_id | New offender NRIC/FIN |
| Condition result | ocms_audit_log | target_processing_stage | 'RD1' or 'DN1' |
| System user | ocms_audit_log | created_by | 'ocmsizmgr' |
| SYSDATE | ocms_audit_log | created_date | Timestamp |

### 3.8.2 Success Outcome

**Suspension Revival Success:**

- User has SUSPENSION_REVIVAL permission and is authorized to revive suspensions
- Revival reason code exists in OCMS_CODE_MASTER table with code_type = 'REVIVAL_REASON'
- Suspension record exists in ocms_suspended_notice with matching notice_no and sr_no
- Suspension is not already revived (date_of_revival IS NULL)
- Suspension record successfully updated with date_of_revival, revival_reason, officer_authorising_revival, revival_remarks
- Notice suspension fields in ocms_valid_offence_notice and eocms_valid_offence_notice successfully patched to most recent active suspension or NULL
- Success response returned to Staff Portal with appCode OCMS-2000
- Staff Portal displays success message: "Suspension revived successfully"
- Revival details visible in notice suspension history

**Offender Particulars Update Success:**

- User has UPDATE_OFFENDER_PARTICULARS permission and is authorized to update particulars
- All mandatory fields provided: offender_name, offender_id_no, date_of_birth, owner_driver_indicator, address fields
- NRIC checksum validation passes (for NRIC) or FIN format validation passes (for FIN)
- No duplicate current offender exists (no existing offender with same ID and offender_indicator = 'Y')
- Email format validation passes (if email provided)
- Phone number format validation passes (if contact_no provided)
- Address fields complete with block, street, postal_code
- Offender record successfully inserted into ocms_offence_notice_owner_driver
- Address record successfully inserted into ocms_offence_notice_owner_driver_address
- Success response returned to Staff Portal with appCode OCMS-2000 and redirection_triggered = true

**Notice Redirection Success:**

- Backend automatically triggers redirection workflow after successful particulars update
- System successfully determines target processing stage based on owner_driver_indicator: RD1 for Owner/Hirer (Condition C004), DN1 for Driver (Condition C005)
- Old deceased offender record successfully updated with offender_indicator = 'N'
- New offender record successfully updated with offender_indicator = 'Y'
- Only one offender per notice has offender_indicator = 'Y' (Rule RED-004 validated)
- Notice record in ocms_valid_offence_notice (Intranet) successfully updated with next_processing_stage and next_processing_date = TRUNC(SYSDATE)
- Notice record in eocms_valid_offence_notice (Internet) successfully synchronized with same processing stage fields
- Redirection event successfully logged in ocms_audit_log with action_type = 'NOTICE_REDIRECTION', old_offender_id, new_offender_id, target_processing_stage
- Staff Portal displays success message: "Notice redirected to [Target Stage]"
- Notice resumes standard processing workflow from target stage (RD1 or DN1)
- Deceased offender record retained with offender_indicator = 'N' for audit trail
- RIP superscript "R" removed from Staff Portal display (no active PS-RIP/PS-RP2)
- Notice becomes furnishable again at appropriate processing stages
- The workflow reaches the End state without triggering unhandled error paths

### 3.8.3 Error Handling

#### Application Error Handling for Suspension Revival

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Unauthorized User | User does not have SUSPENSION_REVIVAL permission role | System returns error response OCMS-4030: "You do not have permission to revive suspensions". User cannot proceed with revival action. OIC administrator must grant permission |
| Invalid Revival Reason Code | revival_reason does not exist in OCMS_CODE_MASTER where code_type = 'REVIVAL_REASON' | System returns error response OCMS-4000: "Invalid revival reason code. Please select from dropdown". User must select valid code from dropdown |
| Notice Not Found | notice_no does not exist in ocms_valid_offence_notice | System returns error response OCMS-4040: "Notice not found". User must verify notice number |
| Suspension Record Not Found | suspension_sr_no does not exist for the notice in ocms_suspended_notice | System returns error response OCMS-4040: "Suspension record not found". User must verify suspension serial number |
| Suspension Already Revived | date_of_revival IS NOT NULL in ocms_suspended_notice | System returns error response OCMS-4000: "Suspension has already been revived". User cannot revive same suspension twice |
| Invalid Suspension Type | suspension_type is not 'PS' or reason_of_suspension is not 'RIP' or 'RP2' | System returns error response OCMS-4000: "Invalid suspension type for revival". This workflow only handles PS-RIP and PS-RP2 |
| Revival Reason Format Error | revival_reason is not exactly 3 characters | System returns error response OCMS-4000: "Revival reason must be 3 characters". User must enter valid format |
| Revival Remarks Too Long | revival_remarks exceeds 500 characters | System returns error response OCMS-4000: "Revival remarks exceed maximum length (500 characters)". User must shorten remarks |
| Database Update Failure | Unable to update ocms_suspended_notice table due to database error | System returns error response OCMS-5000: "Something went wrong. Please try again later". Transaction rollback, no data changed |
| Database Sync Failure | Update to eocms_valid_offence_notice (Internet) fails after Intranet update | Partial success: Intranet updated, Internet out of sync. System logs CRITICAL error, synchronization CRON will retry |

#### Application Error Handling for Offender Particulars Update

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Unauthorized User | User does not have UPDATE_OFFENDER_PARTICULARS permission role | System returns error response OCMS-4030: "You do not have permission to update offender particulars". User cannot proceed |
| Notice Not Found | notice_no does not exist in ocms_valid_offence_notice | System returns error response OCMS-4040: "Notice not found". User must verify notice number |
| Invalid NRIC Checksum | NRIC checksum validation fails | System returns error response OCMS-4000 with field error: "Invalid NRIC checksum". User must enter valid NRIC |
| Invalid FIN Format | FIN does not match pattern [F|G|M]\d{7}[A-Z] | System returns error response OCMS-4000 with field error: "Invalid FIN format". User must enter valid FIN |
| Missing Mandatory Fields | offender_name, offender_id_no, date_of_birth, or owner_driver_indicator is null/empty | System returns error response OCMS-4000 with field errors listing missing fields. User must complete all required fields |
| Invalid Owner/Driver Indicator | owner_driver_indicator is not 'O', 'H', or 'D' | System returns error response OCMS-4000: "Invalid owner/driver/hirer indicator". User must select valid option |
| Duplicate Current Offender | Existing offender with same offender_id_no and offender_indicator = 'Y' for the notice | System returns error response OCMS-4090: "Offender with ID [ID_NUMBER] is already designated as current offender". User must verify offender ID |
| Invalid Email Format | Email does not match valid email pattern (if provided) | System returns error response OCMS-4000 with field error: "Invalid email format". User must enter valid email |
| Invalid Phone Format | contact_no is not 8 digits (if provided) | System returns error response OCMS-4000 with field error: "Invalid phone number format (must be 8 digits)". User must enter valid phone |
| Incomplete Address | block, street, or postal_code is missing | System returns error response OCMS-4000 with field error: "Incomplete address information". User must complete address fields |
| Database Insert Failure | Unable to insert into ocms_offence_notice_owner_driver or ocms_offence_notice_owner_driver_address | System returns error response OCMS-5000: "Something went wrong. Please try again later". Transaction rollback, no data changed |

#### Application Error Handling for Notice Redirection

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Invalid Processing Stage Mapping | owner_driver_indicator is not 'O', 'H', or 'D' after particulars update | System logs error: "Unable to determine processing stage". Manual intervention required to set stage |
| Multiple Current Offenders Detected | More than one offender has offender_indicator = 'Y' after redirection update | System logs CRITICAL error: "Multiple current offenders detected". Data integrity violation, manual cleanup required |
| Stage Update Failure | Unable to update next_processing_stage in ocms_valid_offence_notice | System logs error, redirection incomplete. Manual intervention required to set processing stage |
| Database Sync Failure | Update to eocms_valid_offence_notice (Internet) fails after Intranet update | System logs CRITICAL error. Intranet updated, Internet out of sync. Synchronization CRON will retry |
| Audit Log Insert Failure | Unable to insert redirection event into ocms_audit_log | System logs warning. Redirection completes successfully but audit trail not recorded |

#### API Error Codes

**Suspension Revival API:**

| HTTP Status | Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| 200 | OCMS-2000 | Suspension revived successfully | Success |
| 400 | OCMS-4000 | Invalid revival reason code. Please select from dropdown | Validation error: invalid code |
| 400 | OCMS-4000 | Suspension has already been revived | Validation error: already revived |
| 400 | OCMS-4000 | Revival reason must be 3 characters | Validation error: format |
| 400 | OCMS-4000 | Revival remarks exceed maximum length | Validation error: length |
| 400 | OCMS-4000 | Invalid suspension type for revival | Validation error: wrong type |
| 403 | OCMS-4030 | You do not have permission to revive suspensions | Authorization error |
| 404 | OCMS-4040 | Notice not found | Notice does not exist |
| 404 | OCMS-4040 | Suspension record not found | Suspension does not exist |
| 500 | OCMS-5000 | Something went wrong. Please try again later | Internal server error |
| 503 | OCMS-5030 | Database synchronization failure (Internet sync) | Sync error, will retry |

**Offender Particulars Update API:**

| HTTP Status | Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| 200 | OCMS-2000 | Offender particulars updated successfully | Success with redirection_triggered = true |
| 400 | OCMS-4000 | Validation failed | Validation errors with field-specific messages |
| 400 | OCMS-4000 | Missing mandatory fields | Required fields not provided |
| 400 | OCMS-4000 | Invalid NRIC checksum | NRIC validation failed |
| 400 | OCMS-4000 | Invalid FIN format | FIN validation failed |
| 400 | OCMS-4000 | Invalid owner/driver/hirer indicator | Invalid value for owner_driver_indicator |
| 400 | OCMS-4000 | Invalid email format | Email validation failed |
| 400 | OCMS-4000 | Invalid phone number format | Phone validation failed |
| 400 | OCMS-4000 | Incomplete address information | Address fields missing |
| 403 | OCMS-4030 | You do not have permission to update offender particulars | Authorization error |
| 404 | OCMS-4040 | Notice not found | Notice does not exist |
| 409 | OCMS-4090 | Offender with ID [ID_NUMBER] is already designated as current offender | Duplicate offender error |
| 500 | OCMS-5000 | Something went wrong. Please try again later | Internal server error |
| 503 | OCMS-5030 | Database synchronization failure (Internet sync) | Sync error, will retry |

---

**End of Section 3**

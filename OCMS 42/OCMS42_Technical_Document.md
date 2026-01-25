# OCMS 42 – Furnish Driver's or Hirer's Particulars via eService

**Prepared by**

MGG SOFTWARE

---

## Version History

| Version | Updated By | Date | Changes |
| --- | --- | --- | --- |
| v1.0 | Claude | 25/01/2026 | Document Initiation |
| v1.1 | Claude | 25/01/2026 | Added SQL queries, complete field lists per DD |

---

## Table of Content

| Section | Content | Pages |
| --- | --- | --- |
| 1 | Function: View Outstanding Furnishable Notices via eService | |
| 1.1 | Use Case | |
| 1.2 | High Level Process Flow: Login and Furnish | |
| 1.3 | High Level Process Flow: View Pending Submissions | |
| 1.4 | High Level Process Flow: View Approved Notices | |
| 1.5 | Backend: Get Furnishable Notices | |
| 1.5.1 | Data Mapping | |
| 1.5.2 | Success Outcome | |
| 1.5.3 | Error Handling | |
| 1.6 | Backend: Get Pending Submissions | |
| 1.6.1 | Data Mapping | |
| 1.6.2 | Success Outcome | |
| 1.6.3 | Error Handling | |
| 1.7 | Backend: Get Approved Submissions | |
| 1.7.1 | Data Mapping | |
| 1.7.2 | Success Outcome | |
| 1.7.3 | Error Handling | |
| 2 | Function: Furnish Submission via eService | |
| 2.1 | Use Case | |
| 2.2 | Furnish Submission Flow | |
| 2.2.1 | Data Mapping | |
| 2.2.2 | Success Outcome | |
| 2.2.3 | Error Handling | |
| 3 | Function: Furnish Submission Acknowledgements | |
| 3.1 | Use Case | |
| 3.2 | Acknowledgement Flow | |
| 3.2.1 | Success Outcome | |
| 3.2.2 | Error Handling | |
| 4 | Function: Furnish Process in OCMS Backend | |
| 4.1 | Use Case | |
| 4.2 | Internet Backend Processing | |
| 4.2.1 | Data Mapping | |
| 4.2.2 | Success Outcome | |
| 4.2.3 | Error Handling | |

---

# Section 1 – Function: View Outstanding Furnishable Notices via eService

## 1.1 Use Case

1. The "Furnish Driver's or Hirer's Particulars" eService allows vehicle owners or hirers to view outstanding notices that they need to furnish.

2. Owners or Hirers must log in to the system to access their notices. There are two login options depending on the user type:<br>a. Individual Users: Login via NRIC or FIN using SingPass.<br>b. Business Users (Company): Authorised Corppass users granted access by their companies will login via SingPass.

3. After selecting a login option, the User will be redirected to the Singpass portal to complete the login process.

4. If authentication is successful, the OCMS backend retrieves the notice(s) from the database and checks each notice to determine:<br>a. Whether the notice is furnishable<br>b. Whether the submitted furnishing request is still pending approval<br>c. Whether a previously submitted furnishing request has been approved

5. The notice(s) will be returned to the eService Portal, and the result will be classified and displayed onscreen:<br>a. Furnishable Notices: Notices that can be selected for furnishing<br>b. Non-Furnishable Notices: Display the message for Notices that cannot be furnished if there are non-furnishable notices under the owner/hirer's ID<br>c. Pending Submissions: Notices that have been furnished but are pending approval

---

## 1.2 High Level Process Flow: Login and Furnish

![Flow Diagram](./OCMS42_Flowchart.drawio - Tab 1.2)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | Motorist accesses the Furnish Driver's or Hirer's Particulars eService | User initiates the process |
| Login | Motorist click Singpass/Corppass login | User selects authentication method |
| Authentication | eService Backend starts authentication session with Singpass/Corppass | SPCP performs authentication |
| Auth Success | Singpass/Corppass returns authentication success | User identity verified |
| Query Furnishable | Portal requests Backend to retrieve Furnishable Notices | Backend queries PII and VON tables |
| Check Offender | Portal checks if Offender = Driver | Determines furnish eligibility |
| Display Warning | If Offender is Driver, display "There are other outstanding notice(s) for [Name] that cannot be furnished. Please proceed to pay." | Cannot furnish own notices |
| Display Furnishable | If notices are furnishable, display List of Furnishable Notices | User can select notices to furnish |
| Submit Furnish | Motorist proceeds to furnish and submit particulars | User fills in driver/hirer details |
| Validate | Portal UI validates submission | Frontend validation check |
| Process Submission | Backend processes submission and stores to DB | Data saved with status 'P' |
| Display Result | Portal displays success or failure message | User sees submission result |
| View Pending | User redirected to Pending Notices screen | Shows submitted notices |
| End | Process complete | |

---

## 1.3 High Level Process Flow: View Pending Submissions

![Flow Diagram](./OCMS42_Flowchart.drawio - Tab 1.3)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | User logged in and authenticated | User already authenticated |
| Navigate | Motorist clicks on Pending Submissions tab | User navigates to pending tab |
| Query | Portal requests Backend to retrieve Pending Submissions | Backend queries eocms_furnish_application |
| Check | Backend checks for pending submissions in past 6 months | Status = 'P' or 'S' |
| Display | If records exist, display List of Pending Submissions | Shows pending items |
| No Records | If no records, display "There are no Pending Submissions" | Empty state message |
| End | Process complete | |

---

## 1.4 High Level Process Flow: View Approved Notices

![Flow Diagram](./OCMS42_Flowchart.drawio - Tab 1.4)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | User logged in and authenticated | User already authenticated |
| Navigate | Motorist clicks on Approved Notices | User navigates to approved tab |
| Query | Portal requests Backend to retrieve Approved Notices | Backend queries eocms_furnish_application |
| Check | Backend checks for approved submissions in past 6 months | Status = 'A' |
| Display | If records exist, display List of Approved Notices | Shows approved items |
| No Records | If no records, display "There are no Approved Notices" | Empty state message |
| End | Process complete | |

---

## 1.5 Backend: Get Furnishable Notices

![Flow Diagram](./OCMS42_Flowchart.drawio - Tab 2.2)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| API Request | eService Portal sends POST request with SPCP ID | API receives authenticated ID |
| Query Owner/Driver | Query eocms_offence_notice_owner_driver for notices | Check offender_indicator = 'Y' |
| Check Records | If no records found, respond with empty list | No notices for this ID |
| Check Indicator | Detect owner_driver_indicator value | D = Driver, O = Owner |
| Driver Check | If indicator = 'D', respond that offender is driver | Cannot furnish own notices |
| Query VON | Query eocms_valid_offence_notice for outstanding notices | Filter by notice type, stage, suspension |
| Filter PS | Exclude notices with PS suspension | PS-ANS, PS-APP excluded |
| Query Existing | Query eocms_furnish_application for existing submissions | Exclude P, S, A status |
| Respond | Return list of furnishable notices to Portal | Filtered eligible notices |

### SQL Query - Get Owner/Driver Records

```sql
-- Step 1: Query PII zone for owner/driver records
SELECT
    notice_no,              -- varchar(10)
    id_no,                  -- varchar(114)
    id_type,                -- varchar(1)
    name,                   -- varchar(204)
    owner_driver_indicator, -- varchar(1): O=Owner, D=Driver
    offender_indicator      -- varchar(1): Y=Yes, N=No
FROM eocms_offence_notice_owner_driver
WHERE id_no = :spcpId
  AND offender_indicator = 'Y'
```

### SQL Query - Get Valid Offence Notices

```sql
-- Step 2: Query Internet zone for notice details
SELECT
    notice_no,              -- varchar(10)
    vehicle_no,             -- varchar(14)
    notice_date_and_time,   -- datetime2(7)
    offence_notice_type,    -- varchar(1): O, E, U
    last_processing_stage,  -- varchar(3)
    suspension_type,        -- varchar(2)
    crs_reason_of_suspension,  -- varchar(3)
    epr_reason_of_suspension   -- varchar(3)
FROM eocms_valid_offence_notice
WHERE notice_no IN (:noticeNumbers)
  AND offence_notice_type IN ('O', 'E')
  AND last_processing_stage NOT IN ('CLS', 'WOF', 'CNL')
  AND (suspension_type IS NULL
       OR suspension_type != 'PS'
       OR (suspension_type = 'PS'
           AND epr_reason_of_suspension NOT IN ('ANS', 'APP', 'DRV', 'HIR')))
```

### SQL Query - Check Existing Furnish Applications

```sql
-- Step 3: Exclude notices with existing submissions
SELECT notice_no, status
FROM eocms_furnish_application
WHERE notice_no IN (:noticeNumbers)
  AND owner_id_no = :spcpId
  AND status IN ('P', 'S', 'A')
```

### 1.5.1 Data Mapping

**Source: eocms_offence_notice_owner_driver (PII Zone)**

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| notice_no | varchar(10) | No | Notice number (PK) |
| id_no | varchar(114) | No | Owner/Driver ID (NRIC/FIN/UEN) |
| id_type | varchar(1) | Yes | ID type |
| name | varchar(204) | Yes | Full name |
| owner_driver_indicator | varchar(1) | No | O=Owner, D=Driver |
| offender_indicator | varchar(1) | Yes | Y=Offender, N=Not offender |

**Source: eocms_valid_offence_notice (Internet Zone)**

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| notice_no | varchar(10) | No | Notice number (PK) |
| vehicle_no | varchar(14) | No | Vehicle number |
| notice_date_and_time | datetime2(7) | No | Date/time of notice |
| offence_notice_type | varchar(1) | Yes | O=Original, E=Enquiry, U=Unknown |
| last_processing_stage | varchar(3) | Yes | Current processing stage |
| suspension_type | varchar(2) | Yes | PS=Permanent, TS=Temporary |
| crs_reason_of_suspension | varchar(3) | Yes | Court reason |
| epr_reason_of_suspension | varchar(3) | Yes | EPR reason |

### 1.5.2 Success Outcome

- Return list of furnishable notices with notice_no, vehicle_no, notice_date_and_time
- If offender is driver (owner_driver_indicator = 'D'), return isDriver = true with driver name
- If no records found, return empty list with appCode = "OCMS-2000"

### 1.5.3 Error Handling

Refer to FD Section 2 for detailed validation rules and error scenarios.

| Error Scenario | App Error Code | User Message |
| --- | --- | --- |
| Authentication Failed | OCMS-4001 | Authentication failed. Please try again. |
| Database Error | OCMS-5001 | Unable to retrieve notices. Please try again later. |
| General Server Error | OCMS-5000 | Something went wrong. Please try again later. |

---

## 1.6 Backend: Get Pending Submissions

![Flow Diagram](./OCMS42_Flowchart.drawio - Tab 2.3)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| API Request | eService Portal sends POST request with SPCP ID | API receives authenticated ID |
| Query | Query eocms_furnish_application for pending submissions | Past 6 months, status P or S |
| Check Records | Check if any records returned | Determine response type |
| No Records | If no records, respond with empty list | No pending submissions |
| Return List | If records exist, return list of pending submissions | Pending items found |

### SQL Query - Get Pending Submissions

```sql
SELECT
    txn_no,                 -- varchar(20) PK
    notice_no,              -- varchar(10)
    vehicle_no,             -- varchar(14)
    offence_date,           -- datetime2(7)
    furnish_name,           -- varchar(66)
    furnish_id_no,          -- varchar(12)
    status,                 -- varchar(1)
    cre_date                -- datetime2(7)
FROM eocms_furnish_application
WHERE owner_id_no = :spcpId
  AND cre_date >= DATEADD(month, -6, GETDATE())
  AND status IN ('P', 'S')
ORDER BY cre_date DESC
OFFSET :skip ROWS
FETCH NEXT :limit ROWS ONLY
```

### 1.6.1 Data Mapping

**Source: eocms_furnish_application (PII Zone)**

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| txn_no | varchar(20) | No | Transaction number (PK) |
| notice_no | varchar(10) | No | Notice number |
| vehicle_no | varchar(14) | No | Vehicle number |
| offence_date | datetime2(7) | No | Date of offence |
| furnish_name | varchar(66) | No | Furnished driver/hirer name |
| furnish_id_no | varchar(12) | No | Furnished driver/hirer ID |
| status | varchar(1) | No | P=Pending, S=Resubmission |
| cre_date | datetime2(7) | No | Submission date |

### 1.6.2 Success Outcome

- Return list of pending submissions with pagination (total, limit, skip)
- Status filter: 'P' (Pending) or 'S' (Resubmission)
- Date filter: Past 6 months from current date

### 1.6.3 Error Handling

| Error Scenario | App Error Code | User Message |
| --- | --- | --- |
| General Server Error | OCMS-5000 | Something went wrong. Please try again later. |

---

## 1.7 Backend: Get Approved Submissions

![Flow Diagram](./OCMS42_Flowchart.drawio - Tab 2.4)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| API Request | eService Portal sends POST request with SPCP ID | API receives authenticated ID |
| Query | Query eocms_furnish_application for approved submissions | Past 6 months, status A |
| Check Records | Check if any records returned | Determine response type |
| No Records | If no records, respond with empty list | No approved submissions |
| Return List | If records exist, return list of approved submissions | Approved items found |

### SQL Query - Get Approved Submissions

```sql
SELECT
    txn_no,                 -- varchar(20) PK
    notice_no,              -- varchar(10)
    vehicle_no,             -- varchar(14)
    offence_date,           -- datetime2(7)
    furnish_name,           -- varchar(66)
    furnish_id_no,          -- varchar(12)
    status,                 -- varchar(1)
    cre_date,               -- datetime2(7)
    upd_date                -- datetime2(7) approval date
FROM eocms_furnish_application
WHERE owner_id_no = :spcpId
  AND cre_date >= DATEADD(month, -6, GETDATE())
  AND status = 'A'
ORDER BY upd_date DESC
OFFSET :skip ROWS
FETCH NEXT :limit ROWS ONLY
```

### 1.7.1 Data Mapping

**Source: eocms_furnish_application (PII Zone)**

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| txn_no | varchar(20) | No | Transaction number (PK) |
| notice_no | varchar(10) | No | Notice number |
| vehicle_no | varchar(14) | No | Vehicle number |
| offence_date | datetime2(7) | No | Date of offence |
| furnish_name | varchar(66) | No | Furnished driver/hirer name |
| furnish_id_no | varchar(12) | No | Furnished driver/hirer ID |
| status | varchar(1) | No | A=Approved |
| cre_date | datetime2(7) | No | Submission date |
| upd_date | datetime2(7) | Yes | Approval date |

### 1.7.2 Success Outcome

- Return list of approved submissions with pagination (total, limit, skip)
- Status filter: 'A' (Approved)
- Date filter: Past 6 months from current date

### 1.7.3 Error Handling

| Error Scenario | App Error Code | User Message |
| --- | --- | --- |
| General Server Error | OCMS-5000 | Something went wrong. Please try again later. |

---

# Section 2 – Function: Furnish Submission via eService

## 2.1 Use Case

1. This Furnish Driver's or Hirer's eService allows the vehicle owner or hirer to furnish the driver's or hirer's details for parking offence notices.

2. The vehicle owner or hirer selects the specific notices for which they wish to submit the driver's or hirer's particulars.

3. Each submission can only be used to furnish details for one driver or hirer for multiple notices and vehicles. To furnish information for another driver or hirer, a new submission must be created.

4. The eService provides a Furnish Particulars Form for filling up and submitting the Driver's or Hirer's particulars.

5. After entering the details, the vehicle owner or hirer is prompted to review the particulars. They must then declare that the information submitted is accurate and true before finalizing the submission.

---

## 2.2 Furnish Submission Flow

Refer to FD Section 3.2 for detailed Furnish Particulars Form Sub-flow, Data Validation, and User Action/System Response.

| Step | Description | Brief Description |
| --- | --- | --- |
| Select Notices | User selects notices to furnish from furnishable list | Multiple notices can be selected |
| Fill Form | User fills in Driver's/Hirer's Particulars Form | Name, ID, Address, Contact |
| Frontend Validation | Portal validates required fields and formats | Client-side validation |
| Review | User reviews submitted particulars | Confirmation screen |
| Declaration | User declares information is accurate and true | Checkbox acknowledgement |
| Submit | Portal sends submission to Backend | API call with form data |
| Backend Validation | Backend validates submission data | Server-side validation |
| Store | Backend stores submission to database | Status = 'P', is_sync = 'N' |
| Respond | Backend responds with success/failure | Acknowledgement to portal |

### SQL Query - Insert Furnish Application

```sql
INSERT INTO eocms_furnish_application (
    -- Notice Info
    txn_no,                     -- varchar(20) PK - from SQL sequence
    notice_no,                  -- varchar(10)
    vehicle_no,                 -- varchar(14)
    offence_date,               -- datetime2(7)
    pp_code,                    -- varchar(5)
    pp_name,                    -- varchar(100)
    last_processing_stage,      -- varchar(3)

    -- Furnished Person Particulars
    furnish_name,               -- varchar(66)
    furnish_id_type,            -- varchar(1)
    furnish_id_no,              -- varchar(12)
    furnish_mail_blk_no,        -- varchar(10)
    furnish_mail_floor,         -- varchar(3)
    furnish_mail_street_name,   -- varchar(32)
    furnish_mail_unit_no,       -- varchar(5)
    furnish_mail_bldg_name,     -- varchar(65)
    furnish_mail_postal_code,   -- varchar(6)
    furnish_tel_code,           -- varchar(4)
    furnish_tel_no,             -- varchar(12)
    furnish_email_addr,         -- varchar(320)

    -- Relationship & Questionnaire
    owner_driver_indicator,     -- varchar(1): D/H
    hirer_owner_relationship,   -- varchar(1): F/E/L/O
    others_relationship_desc,   -- varchar(15)
    ques_one_ans,               -- varchar(32)
    ques_two_ans,               -- varchar(32)
    ques_three_ans,             -- varchar(32)
    rental_period_from,         -- datetime2(7)
    rental_period_to,           -- datetime2(7)

    -- Submitter (Owner) Info
    owner_name,                 -- varchar(66)
    owner_id_no,                -- varchar(12)
    owner_tel_code,             -- varchar(4)
    owner_tel_no,               -- varchar(20)
    owner_email_addr,           -- varchar(320)
    corppass_staff_name,        -- varchar(66)

    -- Status & System Fields
    status,                     -- varchar(1): 'P'
    is_sync,                    -- varchar(1): 'N'
    reason_for_review,          -- varchar(255)
    remarks,                    -- varchar(200)
    cre_date,                   -- datetime2(7)
    cre_user_id                 -- varchar(50): 'ocmsez_app_conn'
)
VALUES (
    NEXT VALUE FOR seq_furnish_txn_no,
    :noticeNo,
    :vehicleNo,
    :offenceDate,
    :ppCode,
    :ppName,
    :lastProcessingStage,

    :furnishName,
    :furnishIdType,
    :furnishIdNo,
    :furnishMailBlkNo,
    :furnishMailFloor,
    :furnishMailStreetName,
    :furnishMailUnitNo,
    :furnishMailBldgName,
    :furnishMailPostalCode,
    :furnishTelCode,
    :furnishTelNo,
    :furnishEmailAddr,

    :ownerDriverIndicator,
    :hirerOwnerRelationship,
    :othersRelationshipDesc,
    :quesOneAns,
    :quesTwoAns,
    :quesThreeAns,
    :rentalPeriodFrom,
    :rentalPeriodTo,

    :ownerName,
    :ownerIdNo,
    :ownerTelCode,
    :ownerTelNo,
    :ownerEmailAddr,
    :corppassStaffName,

    'P',
    'N',
    NULL,
    NULL,
    GETDATE(),
    'ocmsez_app_conn'
)
```

### 2.2.1 Data Mapping

**Target: eocms_furnish_application (PII Zone) - Full Field List**

**Notice Info:**

| Field | Type | Nullable | Source | Description |
|-------|------|----------|--------|-------------|
| txn_no | varchar(20) | No | SQL Sequence | PK - Submission reference number |
| notice_no | varchar(10) | No | User selection | Notice number |
| vehicle_no | varchar(14) | No | eocms_valid_offence_notice | Vehicle number |
| offence_date | datetime2(7) | No | eocms_valid_offence_notice | Date of offence |
| pp_code | varchar(5) | No | eocms_valid_offence_notice | Car park code |
| pp_name | varchar(100) | No | eocms_valid_offence_notice | Car park name |
| last_processing_stage | varchar(3) | No | eocms_valid_offence_notice | Current processing stage |

**Furnished Person Particulars:**

| Field | Type | Nullable | Source | Description |
|-------|------|----------|--------|-------------|
| furnish_name | varchar(66) | No | User input | Name of furnished driver/hirer |
| furnish_id_type | varchar(1) | No | User input | N=NRIC, F=FIN, P=Passport |
| furnish_id_no | varchar(12) | No | User input | ID number |
| furnish_mail_blk_no | varchar(10) | No | User input | Block/house number |
| furnish_mail_floor | varchar(3) | Yes | User input | Floor number |
| furnish_mail_street_name | varchar(32) | No | User input | Street name |
| furnish_mail_unit_no | varchar(5) | Yes | User input | Unit number |
| furnish_mail_bldg_name | varchar(65) | Yes | User input | Building name |
| furnish_mail_postal_code | varchar(6) | No | User input | Postal code |
| furnish_tel_code | varchar(4) | Yes | User input | Country code |
| furnish_tel_no | varchar(12) | Yes | User input | Contact number |
| furnish_email_addr | varchar(320) | Yes | User input | Email address |

**Relationship & Questionnaire:**

| Field | Type | Nullable | Source | Description |
|-------|------|----------|--------|-------------|
| owner_driver_indicator | varchar(1) | No | User input | D=Driver, H=Hirer |
| hirer_owner_relationship | varchar(1) | No | User input | F=Family, E=Employee, L=Leased, O=Others |
| others_relationship_desc | varchar(15) | No | User input | Required if relationship = O |
| ques_one_ans | varchar(32) | No | User input | Answer to Q1 |
| ques_two_ans | varchar(32) | No | User input | Answer to Q2 |
| ques_three_ans | varchar(32) | Yes | User input | Answer to Q3 |
| rental_period_from | datetime2(7) | Yes | User input | Required if relationship = L |
| rental_period_to | datetime2(7) | Yes | User input | Required if relationship = L |

**Submitter (Owner) Info:**

| Field | Type | Nullable | Source | Description |
|-------|------|----------|--------|-------------|
| owner_name | varchar(66) | No | SPCP callback | Submitter name |
| owner_id_no | varchar(12) | No | SPCP callback | Submitter ID |
| owner_tel_code | varchar(4) | Yes | User input | Country code |
| owner_tel_no | varchar(20) | Yes | User input | Contact number |
| owner_email_addr | varchar(320) | Yes | User input | Email address |
| corppass_staff_name | varchar(66) | Yes | SPCP callback | Corppass representative name |

**Status & System Fields:**

| Field | Type | Nullable | Source | Description |
|-------|------|----------|--------|-------------|
| status | varchar(1) | No | System | 'P' = Pending |
| is_sync | varchar(1) | No | System | 'N' = Not synced (default) |
| reason_for_review | varchar(255) | Yes | System | Auto-approval failure reason |
| remarks | varchar(200) | Yes | System | Officer remarks |
| cre_date | datetime2(7) | No | System | GETDATE() |
| cre_user_id | varchar(50) | No | System | 'ocmsez_app_conn' |
| upd_date | datetime2(7) | Yes | System | Last update date |
| upd_user_id | varchar(50) | Yes | System | Updated by |

### 2.2.2 Success Outcome

- Record successfully inserted to eocms_furnish_application
- txn_no generated from SQL Server sequence
- status set to 'P' (Pending)
- is_sync set to 'N' for Intranet sync pickup
- cre_user_id set to 'ocmsez_app_conn' (Internet zone)
- User receives success acknowledgement message

### 2.2.3 Error Handling

Refer to FD Section 3 for detailed validation rules and error scenarios.

| Error Scenario | App Error Code | User Message |
| --- | --- | --- |
| Missing Required Fields | OCMS-4001 | Please fill in all required fields. |
| Invalid ID Format | OCMS-4002 | Invalid ID number format. |
| Notice Already Submitted | OCMS-4003 | This notice has already been submitted for furnishing. |
| Database Error | OCMS-5001 | Unable to process submission. Please try again later. |
| General Server Error | OCMS-5000 | Something went wrong. Please try again later. |

---

# Section 3 – Function: Furnish Submission Acknowledgements

## 3.1 Use Case

1. After the furnishing has been submitted, OCMS Internet backend (BE) responds to the eService Portal, indicating whether the submission was successfully received or has failed due to validation or system errors.

2. When the eService Portal receives the furnish submission status, it will display a success or failure message on the UI to notify the vehicle owner or hirer of the submission result. An acknowledgement email will also be automatically sent to the vehicle owner or hirer to acknowledge the submission received.

---

## 3.2 Acknowledgement Flow

Refer to FD Section 4.2 for detailed Furnish Submission Outcomes Flow.

| Step | Description | Brief Description |
| --- | --- | --- |
| Receive Response | Portal receives response from Backend | Success or failure status |
| Display Success | If successful, display success message | "Your submission has been received." |
| Display Failure | If failed, display failure message | "Furnish was unsuccessful. Please try again." |
| Send Email | System sends acknowledgement email | Email notification to user |
| Redirect | Redirect user to Pending Submissions | View submitted notices |

### 3.2.1 Success Outcome

- Display success message: "Your submission has been received."
- Display submission reference number (txn_no)
- Redirect to Pending Submissions screen
- Send acknowledgement email to owner/hirer (owner_email_addr)

### 3.2.2 Error Handling

Refer to FD Section 4 for detailed error scenarios.

| Error Scenario | User Message |
| --- | --- |
| Submission Failed | Furnish was unsuccessful. Please try again. |
| Email Failed | Submission received but email notification failed. |

---

# Section 4 – Function: Furnish Process in OCMS Backend

## 4.1 Use Case

1. The eService sends the submitted driver's or hirer's particulars to OCMS via the Internet API.

2. The Internet Backend performs the necessary data requirement validation and stores the furnished record in the database.

3. If the Furnished Particulars Data is successfully stored in the database, the eService will respond that the submission has been successfully received. If there are errors in data validation or during the data storage process, the eService will respond that the submission has failed.

4. The OCMS Intranet supports the Furnish Hirer/Driver eService Portal through the following key functions:<br>a. Notice Synchronisation: Sync offence notice details from the OCMS Intranet to the Online Portal, enabling vehicle owners or hirers to furnish the driver or hirer particulars for their offences.<br>b. Furnished Submission Processing: Manage the processing workflow to determine whether furnished particulars can be auto-approved, require manual review, or must be resubmitted.

5. Once a decision is made, OCMS updates the Online Portal so that vehicle owners or hirers can view the outcomes of their submissions.

---

## 4.2 Internet Backend Processing

Refer to FD Section 5.2 for detailed Furnish Submission in OCMS Internet Backend Process Flow.

| Step | Description | Brief Description |
| --- | --- | --- |
| Receive Request | Internet Backend receives furnish submission request | API receives form data |
| Validate Data | Validate all required fields and business rules | Server-side validation |
| Generate TXN | Generate transaction number using SQL Server sequence | Unique identifier |
| Insert Record | Insert record to eocms_furnish_application | Status = 'P', is_sync = 'N' |
| Insert Doc | Insert supporting documents to eocms_furnish_application_doc | If documents attached |
| Respond | Return success/failure response to Portal | Acknowledgement |
| Sync to Intranet | Intranet sync job picks up new records | is_sync = 'N' records |
| Process Decision | Intranet processes and makes decision | Auto-approve or manual review |
| Update Status | Update submission status based on decision | 'A', 'R', or 'S' |

### SQL Query - Insert Supporting Documents

```sql
INSERT INTO eocms_furnish_application_doc (
    txn_no,         -- varchar(20) FK
    doc_name,       -- varchar(255)
    cre_date,       -- datetime2(7)
    cre_user_id     -- varchar(50)
)
VALUES (
    :txnNo,
    :docName,
    GETDATE(),
    'ocmsez_app_conn'
)
```

### 4.2.1 Data Mapping

Refer to FD Section 5.6 for detailed Database Tables Update.

**Target: eocms_furnish_application_doc (PII Zone)**

| Field | Type | Nullable | Source | Description |
|-------|------|----------|--------|-------------|
| txn_no | varchar(20) | No | FK from eocms_furnish_application | Transaction number |
| doc_name | varchar(255) | No | User upload | Document name |
| cre_date | datetime2(7) | No | System | GETDATE() |
| cre_user_id | varchar(50) | No | System | 'ocmsez_app_conn' |
| upd_date | datetime2(7) | Yes | System | Update date |
| upd_user_id | varchar(50) | Yes | System | Updated by |

### 4.2.2 Success Outcome

- Record successfully inserted to eocms_furnish_application
- Supporting documents inserted to eocms_furnish_application_doc
- status set to 'P' (Pending)
- is_sync set to 'N' for Intranet sync pickup
- cre_user_id set to 'ocmsez_app_conn' (Internet zone)
- Success response returned to eService Portal

### 4.2.3 Error Handling

Refer to FD Section 5 for detailed error scenarios.

| Error Scenario | App Error Code | User Message |
| --- | --- | --- |
| Validation Failed | OCMS-4001 | Invalid submission data. Please check and try again. |
| Database Error | OCMS-5001 | Unable to process submission. Please try again later. |
| General Server Error | OCMS-5000 | Something went wrong. Please try again later. |

---

# API Specification

## API for eService

### API Get Furnishable Notices

| Field | Value |
| --- | --- |
| API Name | getFurnishableNotices |
| URL | POST /ocms/v1/furnish/notices |
| Description | Retrieve list of notices that can be furnished by the authenticated user |
| Method | POST |
| Header | `{ "Authorization": "Bearer [token]", "Content-Type": "application/json" }` |

**Request Payload:**
```json
{
  "spcpId": "S1234567A",
  "idType": "NRIC",
  "authSource": "SINGPASS",
  "pagination": {
    "limit": 10,
    "skip": 0
  }
}
```

**Response (Success):**
```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Success",
    "isDriver": false,
    "pagination": {
      "total": 2,
      "limit": 10,
      "skip": 0
    },
    "notices": [
      {
        "noticeNo": "N1234567890",
        "vehicleNo": "SBA1234A",
        "noticeDateAndTime": "2025-01-15T14:00:00",
        "offenceNoticeType": "O",
        "lastProcessingStage": "ROV"
      }
    ]
  }
}
```

**Response (Offender is Driver):**
```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Success",
    "isDriver": true,
    "driverName": "TAN AH KOW",
    "notices": []
  }
}
```

### API Get Pending Submissions

| Field | Value |
| --- | --- |
| API Name | getPendingSubmissions |
| URL | POST /ocms/v1/furnish/pending |
| Description | Retrieve list of pending furnish submissions for the authenticated user |
| Method | POST |

**Request Payload:**
```json
{
  "spcpId": "S1234567A",
  "pagination": {
    "limit": 10,
    "skip": 0
  }
}
```

**Response (Success):**
```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Success",
    "pagination": {
      "total": 5,
      "limit": 10,
      "skip": 0
    },
    "submissions": [
      {
        "txnNo": "FU2026012500001",
        "noticeNo": "N1234567890",
        "vehicleNo": "SBA1234A",
        "offenceDate": "2025-01-15T14:00:00",
        "furnishName": "JOHN LEE",
        "furnishIdNo": "S9876543B",
        "status": "P",
        "creDate": "2026-01-25T10:30:00"
      }
    ]
  }
}
```

### API Get Approved Submissions

| Field | Value |
| --- | --- |
| API Name | getApprovedSubmissions |
| URL | POST /ocms/v1/furnish/approved |
| Description | Retrieve list of approved furnish submissions for the authenticated user |
| Method | POST |

**Request Payload:**
```json
{
  "spcpId": "S1234567A",
  "pagination": {
    "limit": 10,
    "skip": 0
  }
}
```

**Response (Success):**
```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Success",
    "pagination": {
      "total": 3,
      "limit": 10,
      "skip": 0
    },
    "submissions": [
      {
        "txnNo": "FU2026011500001",
        "noticeNo": "N1234567890",
        "vehicleNo": "SBA1234A",
        "offenceDate": "2025-01-15T14:00:00",
        "furnishName": "JOHN LEE",
        "furnishIdNo": "S9876543B",
        "status": "A",
        "creDate": "2026-01-15T10:30:00",
        "updDate": "2026-01-16T09:00:00"
      }
    ]
  }
}
```

### API Submit Furnish

| Field | Value |
| --- | --- |
| API Name | submitFurnish |
| URL | POST /ocms/v1/furnish/submit |
| Description | Submit driver's or hirer's particulars for selected notices |
| Method | POST |

**Request Payload:**
```json
{
  "spcpId": "S1234567A",
  "authSource": "SINGPASS",
  "notices": [
    {
      "noticeNo": "N1234567890",
      "vehicleNo": "SBA1234A",
      "offenceDate": "2025-01-15T14:00:00",
      "ppCode": "M0028",
      "ppName": "Magazine Road",
      "lastProcessingStage": "ROV"
    }
  ],
  "furnishParticulars": {
    "name": "JOHN LEE",
    "idType": "N",
    "idNo": "S9876543B",
    "mailBlkNo": "71",
    "mailFloor": "07",
    "mailStreetName": "BEDOK NORTH",
    "mailUnitNo": "21",
    "mailBldgName": "RESERVOIR RISE",
    "mailPostalCode": "470706",
    "telCode": "65",
    "telNo": "91234567",
    "emailAddr": "john.lee@email.com"
  },
  "ownerDriverIndicator": "D",
  "hirerOwnerRelationship": "F",
  "othersRelationshipDesc": null,
  "quesOneAns": "Yes",
  "quesTwoAns": "Yes",
  "quesThreeAns": null,
  "rentalPeriodFrom": null,
  "rentalPeriodTo": null,
  "ownerParticulars": {
    "name": "TAN AH KOW",
    "idNo": "S1234567A",
    "telCode": "65",
    "telNo": "81234567",
    "emailAddr": "tan.ahkow@email.com"
  },
  "corppassStaffName": null,
  "declaration": true
}
```

**Response (Success):**
```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Your submission has been received.",
    "txnNo": "FU2026012500001",
    "submissionDate": "2026-01-25T10:30:00"
  }
}
```

**Response (Failure):**
```json
{
  "data": {
    "appCode": "OCMS-4001",
    "message": "Invalid submission data. Please check and try again.",
    "errors": [
      {
        "field": "furnishParticulars.idNo",
        "message": "Invalid ID number format"
      }
    ]
  }
}
```

---

**END OF DOCUMENT**

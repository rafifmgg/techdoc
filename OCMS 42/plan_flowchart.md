# OCMS 42 - Flowchart Planning Document

## Document Information
| Item | Detail |
|------|--------|
| Version | 2.0 |
| Date | 2026-01-27 |
| Source | Functional Flow (ocms 42 functional flow.drawio) |
| Data Reference | Functional Document (v1.3_OCMS 42_Functional_Doc.md) |
| Feature | Furnish Driver's or Hirer's Particulars via eService |

**Note:** Flow structure from Functional Flow (updated), data fields from FD (for reference only).

---

## 1. Technical Flowchart Structure

### Tab Naming Convention

| Tab | Name | Type | Description | FD Section |
|-----|------|------|-------------|------------|
| 1.2 | High-Level Login and Furnish | High-Level | Main flow: Login via SPCP → Get Furnishable Notices → Submit Furnish Particulars → View Pending | Sec 2, 3 |
| 1.3 | High-Level View Pending | High-Level | View flow: Login via SPCP → Navigate to Pending tab → View list | Sec 2 |
| 1.4 | High-Level View Approved | High-Level | View flow: Login via SPCP → Navigate to Approved tab → View list | Sec 2 |
| 2.2 | Backend Get Furnishable Notices | Detailed | Backend API logic: Query owner_driver table → Check eligibility → Query VON → Filter by existing apps → Return list | Sec 2.2 |
| 2.3 | Backend Get Pending Submissions | Detailed | Backend API logic: Query furnish_application (status P/S, 6 months) → Return list | Sec 2 |
| 2.4 | Backend Get Approved Submissions | Detailed | Backend API logic: Query furnish_application (status A, 6 months) → Return list | Sec 2 |
| 3.2 | High-Level Furnish Submission | High-Level | Main flow: Select notice → Fill form → Validate → Submit | Sec 3 |
| 3.3 | Furnish Particular Form | Sub-flow | Form structure, OneMap API, field validations | Sec 3.2.2 |
| 3.4 | Check Eligibility | Sub-flow | Individual and Company eligibility rules | Sec 3.2.3 |
| 4.2 | Submission Acknowledge | High-Level | Portal display based on API response (success/fail) | Sec 4 |
| 5.2 | Internet Backend Processing | Detailed | Save to eocms_furnish_application | Sec 5.2 |
| 5.3 | Intranet Backend Processing | Detailed | CRON sync, approval, update offender | Sec 5.4 |

### Flow Description Summary

| Tab | Purpose | Key Decision Points | Output |
|-----|---------|---------------------|--------|
| 1.2 | Complete furnish journey | Is Driver? Has notices? Validation pass? | Success/Error message |
| 1.3 | View pending submissions | Any pending? | List or "No records" |
| 1.4 | View approved submissions | Any approved? | List or "No records" |
| 2.2 | Eligibility check logic | Has records? Is Driver? Stage valid? PS excluded? Existing app? | Furnishable list |
| 2.3 | Retrieve pending list | Any records? | Pending list |
| 2.4 | Retrieve approved list | Any records? | Approved list |
| 3.2 | Furnish submission flow | Is eligible? Validation pass? | Submit or error |
| 3.3 | Form input with validations | OneMap found? Fields valid? | Form data |
| 3.4 | Self-furnish check | Same ID? Furnish as hirer? | F01/F02 error or allow |
| 4.2 | Acknowledge submission | appCode = SUCCESS? | Success screen or error |
| 5.2 | Internet backend save | Valid? Save success? | txnNo or error |
| 5.3 | Intranet approval process | Auto-approve? Decision? | Status update, sync |

---

## 2. Swimlane Structure

### 2.1 High-Level Flows (3 Swimlanes)

| Swimlane | Zone | Description |
|----------|------|-------------|
| eService Portal (Internet) | Internet | Frontend user interactions |
| eService Backend (Internet) | Internet | Backend API processing |
| Singpass/Corppass | External | Authentication system |

### 2.2 Backend Detail Flows (2 Swimlanes)

| Swimlane | Zone | Description |
|----------|------|-------------|
| eService Backend | Internet | API processing logic |
| Database | Internet/PII | Database queries and operations |

### 2.3 Intranet Flows (2 Swimlanes)

| Swimlane | Zone | Description |
|----------|------|-------------|
| CRON Job | Intranet | Scheduled sync job |
| OCMS Intranet Backend | Intranet | Approval processing |

---

## 3. Flow Details - Section 1-2

### Tab 1.2: High-Level Login and Furnish

**Title:** High Level Process Flow: Login and Furnish Hirer and Driver

**Swimlanes:** eService Portal, eService Backend, Singpass/Corppass

#### Flow Steps

| # | Swimlane | Shape | Content | Connection |
|---|----------|-------|---------|------------|
| 1 | Portal | Start | Start | → 2 |
| 2 | Portal | Process (dashed) | Motorist click Singpass/Corppass login | → 3 |
| 3 | Backend | Process | Start authentication session | → 4 |
| 4 | Singpass | Process | Perform authentication | → 5 |
| 5 | Singpass | Process | Auth success | → 6 |
| 6 | Backend | Process | Respond success to portal | → 7 |
| 7 | Portal | Process | Receive auth success | → 8 |
| 8 | Portal | Process | Query Backend to retrieve Furnishable Notices | → 9 |
| 9 | Backend | Process | Retrieve Furnishable Notices from DB | → 10 |
| 10 | Backend | Process | Respond to Portal | → 11 |
| 11 | Portal | Decision | Offender = Driver? | Yes→12, No→13 |
| 12 | Portal | UI Display | "There are other outstanding notice(s) for [Name] that cannot be furnished. Please proceed to pay." | → End |
| 13 | Portal | Decision | Any Furnishable Notices? | No→14, Yes→15 |
| 14 | Portal | UI Display | "There are no furnishable notices" | → End |
| 15 | Portal | UI Display | List of furnishable Notices | → 16 |
| 16 | Portal | Process (dashed) | Motorist proceeds to furnish and submit | → 17 |
| 17 | Portal | Process | Portal UI validate submission | Pass→19, Fail→18 |
| 18 | Portal | Process (dashed) | Motorist edits submission | → 17 (loop) |
| 19 | Portal | Process | Send submission to backend | → 20 |
| 20 | Backend | Process | Process submission and store to DB | → 21 |
| 21 | Backend | Process | Respond to Portal | → 22 |
| 22 | Portal | Decision | Submission successful? | No→23, Yes→24 |
| 23 | Portal | UI Display | "Furnish was unsuccessful. Please try again." | → End |
| 24 | Portal | Process | Redirect to Pending Notices screen | → 25 |
| 25 | Portal | Process | Query Backend to retrieve Pending Notices | → 26 |
| 26 | Backend | Process | Retrieve Pending Notices from DB | → 27 |
| 27 | Backend | Process | Respond to Portal | → 28 |
| 28 | Portal | UI Display | List of Pending Notices | → End |
| 29 | Portal | End | End | |

---

### Tab 1.3: High-Level View Pending

**Title:** High Level Process Flow: View Pending Submissions

**Swimlanes:** eService Portal, eService Backend, Singpass/Corppass

#### Flow Steps

| # | Swimlane | Shape | Content | Connection |
|---|----------|-------|---------|------------|
| 1-15 | (Same as Tab 1.2) | | Login + Get Furnishable | |
| 16 | Portal | Process (dashed) | Motorist clicks on Pending Submissions tab | → 17 |
| 17 | Portal | Process | Query Backend to retrieve Pending Submissions | → 18 |
| 18 | Backend | Process | Retrieve Pending Submissions from DB | → 19 |
| 19 | Backend | Process | Respond to Portal | → 20 |
| 20 | Portal | Decision | Any Pending Submissions? | No→21, Yes→22 |
| 21 | Portal | UI Display | "There are no Pending Submissions" | → End |
| 22 | Portal | UI Display | List of Pending Submissions | → End |

---

### Tab 1.4: High-Level View Approved

**Title:** High Level Process Flow: View Approved Notices

**Swimlanes:** eService Portal, eService Backend, Singpass/Corppass

#### Flow Steps

| # | Swimlane | Shape | Content | Connection |
|---|----------|-------|---------|------------|
| 1-15 | (Same as Tab 1.2) | | Login + Get Furnishable | |
| 16 | Portal | Process (dashed) | Motorist clicks on Approved Notices | → 17 |
| 17 | Portal | Process | Query Backend to retrieve Approved Notices | → 18 |
| 18 | Backend | Process | Retrieve Approved Notices from DB | → 19 |
| 19 | Backend | Process | Respond to Portal | → 20 |
| 20 | Portal | Decision | Any Approved Submissions? | No→21, Yes→22 |
| 21 | Portal | UI Display | "There are no Approved Notices" | → End |
| 22 | Portal | UI Display | List of Approved Notices | → End |

---

### Tab 2.2: Backend Get Furnishable Notices

**Title:** eService Backend: Query for Furnishable Notices

**Swimlanes:** eService Backend, Database

#### Flow Steps

| # | Swimlane | Shape | Content | Query Box | Connection |
|---|----------|-------|---------|-----------|------------|
| 1 | Backend | Process (dashed) | SPCP auth success | | → 2 |
| 2 | Backend | Process (dashed) | eService Portal sends API request | **API Input:** ID number from SP or CP | → 3 |
| 3 | Backend | Process | eService Backend receives API request | | → 4 |
| 4 | Backend | Start | Start | | → 5 |
| 5 | Backend | Process | Query PII Owner Driver table to get list of Notices under SPCP ID | **QUERY DB: eocms_offence_notice_owner_driver** (a) id_no = Singpass/Corppass ID (b) offender_indicator = Y | → 6 |
| 6 | Backend | Decision | Any record returned? | | No→7, Yes→8 |
| 7 | Backend | Process | Respond to eService Portal - No records found | | → End |
| 8 | Backend | Process | Detect owner_driver_indicator | | → 9 |
| 9 | Backend | Decision | indicator value = D? | | Yes→10, No→11 |
| 10 | Backend | Process | Respond to eService Portal - Offender = Driver | **Note:** Staff Portal to display: "There are other outstanding notice(s) for [Name] that cannot be furnished. Please proceed to pay." | → End |
| 11 | Backend | Result | List of Notice Number(s) | | → 12 |
| 12 | Backend | Process | Use Notice Number(s) to batch query eVON for Outstanding Notices | **QUERY DB: eocms_valid_offence_notice** Select notices where: notice_no = Notice Number(s) provided, offence_notice_type = O or E, last_processing_stage = ROV or ENA or RD1 or RD2, crs_reason_of_suspension = Null. EXCLUDE notices where: suspension_type = PS AND epr_reason_of_suspension = ANS or APP or CAN or CFA or DBB or FCT or FTC or OTH or SCT or SLC or SSV or VCT or VST | → 13 |
| 13 | Backend | Result | List of outstanding Notice(s) that are not permanently suspended | **Note:** Query filters out the PS EPRs that cannot be overwritten as Internet DB does not have suspended_notices DB table to check PS-MID, PS-DIP, PS-RIP/RP2 and PS-FOR | → 14 |
| 14 | Backend | Process | Query Furnish Application table to check if Notice(s) can be furnished | **QUERY DB: eocms_furnish_application** notice_no = Notice Number(s) provided, owner_id_no = Singpass/Corppass ID, status is NOT = Pending (P), Resubmission (S) or Approved (A) | → 15 |
| 15 | Backend | Result | List of outstanding Notice(s) that are furnishable | | → 16 |
| 16 | Backend | Process | Respond to eService Portal - List of furnishable Notices | | → End |

---

### Tab 2.3: Backend Get Pending Submissions

**Title:** eService Backend: Query for Pending Submissions

**Swimlanes:** eService Backend, Database

#### Flow Steps

| # | Swimlane | Shape | Content | Query Box | Connection |
|---|----------|-------|---------|-----------|------------|
| 1 | Backend | Process (dashed) | SPCP auth success | | → 2 |
| 2 | Backend | Process (dashed) | eService Portal sends API request | **API Input:** ID number from SP or CP | → 3 |
| 3 | Backend | Process | eService Backend receives API request | | → 4 |
| 4 | Backend | Start | Start | | → 5 |
| 5 | Backend | Process | Query Furnish Application table to check if there are any Pending Submissions in past 6 months | **QUERY DB: eocms_furnish_application** (a) owner_id_no = Singpass/Corppass ID (b) cre_date = Past 6 months from today's date (c) status = Pending (P) or Resubmission (S) | → 6 |
| 6 | Backend | Decision | Any record returned? | | No→7, Yes→8 |
| 7 | Backend | Process | Respond to eService Portal - No records found | | → End |
| 8 | Backend | Result | List of Pending Submissions | | → 9 |
| 9 | Backend | Process | Respond to eService Portal - List of Pending Submissions | | → End |

---

### Tab 2.4: Backend Get Approved Submissions

**Title:** eService Backend: Query for Approved Submissions

**Swimlanes:** eService Backend, Database

#### Flow Steps

| # | Swimlane | Shape | Content | Query Box | Connection |
|---|----------|-------|---------|-----------|------------|
| 1 | Backend | Process (dashed) | SPCP auth success | | → 2 |
| 2 | Backend | Process (dashed) | eService Portal sends API request | **API Input:** ID number from SP or CP | → 3 |
| 3 | Backend | Process | eService Backend receives API request | | → 4 |
| 4 | Backend | Start | Start | | → 5 |
| 5 | Backend | Process | Query Furnish Application table to check if there are any Approved Notices in past 6 months | **QUERY DB: eocms_furnish_application** (a) owner_id_no = Singpass/Corppass ID (b) cre_date = Past 6 months from today's date (c) status = Approved (A) | → 6 |
| 6 | Backend | Decision | Any record returned? | | No→7, Yes→8 |
| 7 | Backend | Process | Respond to eService Portal - No records found | | → End |
| 8 | Backend | Result | List of Approved Submissions | | → 9 |
| 9 | Backend | Process | Respond to eService Portal - List of Approved Submissions | | → End |

---

## 4. Flow Details - Section 3

### Tab 3.2: High-Level Furnish Submission Flow

**Title:** High Level Process Flow: Furnish Submission via eService

**Swimlanes:** eService Portal, eService Backend

#### Flow Steps

| Step | Actor | Description | Notes |
|------|-------|-------------|-------|
| 1 | Portal | Start | User sudah login, lihat list furnishable notices |
| 2 | Portal | Select notice(s) to furnish | User pilih satu atau lebih notice |
| 3 | Portal | Click "Proceed to Furnish" | Navigate ke form |
| 4 | Portal | Display Driver/Hirer Particular Online Form | Form kosong ditampilkan |
| 5 | Portal | User input Furnish Particular Form | Refer to Tab 3.3 |
| 6 | Portal | Process: Check eligible to furnish | Refer to Tab 3.4 |
| 7 | Portal | Decision: Is eligible to furnish? | |
| 7a | Portal | No → Display "Disallow submission to furnish" | End atau loop |
| 7b | Portal | Yes → Proceed to validation | |
| 8 | Portal | Data validation | Check ID format, required fields |
| 9 | Portal | Decision: Validation success? | |
| 9a | Portal | No → Display validation error | Loop back to form |
| 9b | Portal | Yes → Display Summary to review | |
| 10 | Portal | User review and declare info is true | Checkbox declaration |
| 11 | Portal | User click "Confirm to Furnish" | Submit |
| 12 | Portal | Call OCMS Internet Backend API | POST /v1/furnishapplication |
| 13 | Backend | Receive and process request | Refer to Tab 5.2 |
| 14 | Portal | Display result (success/fail) | Refer to Tab 4.2 |
| 15 | Portal | End | |

---

### Tab 3.3: Furnish Particular Form Sub-flow

**Title:** Furnish Particular Form with Validations

**Swimlanes:** eService Portal, External System (OneMap API)

#### Flow Steps with Field-Level Validations

| Step | Action | Validation | Error Message (if fail) |
|------|--------|------------|-------------------------|
| 1 | Display Furnish Particular Form | - | Show all sections |
| 2 | User edits Owner Contact No. | If Country = Singapore: Must be 8 digits | "Invalid contact number" |
| 3 | User enters Owner Email | Format validation (contains @, valid domain) | "Invalid email address. Please enter again." |
| 4 | User enters Confirm Owner Email | Must match Email field | "Email addresses do not match. Please verify and re-enter." |
| 5 | User enters Driver/Hirer Name | Mandatory, character limit | "Please enter name" |
| 6 | User selects ID Type | Dropdown: NRIC, FIN, UEN, Passport | - |
| 7 | User enters ID No. | Validate based on ID Type (NRIC/FIN/UEN format) | "Invalid ID format" |
| 8 | User enters Postal Code | Must be 6 digits | "Invalid postal code" |
| 9 | User clicks Get Address | Call OneMap API | - |
| 9a | Frontend calls GET /onemapService/getOnemapToken | Get auth token | "Server Error" (if fail) |
| 9b | Frontend calls OneMap API with postal code | searchVal = postal code | - |
| 9c | Decision: Address found? | Check response.results | "Postal code does not exist." |
| 9d | Auto-populate: Block, Street, Building | From OneMap response | - |
| 10 | User enters Floor/Unit No. OR checks "No floor/unit no." | Conditional mandatory | - |
| 11 | User enters Driver/Hirer Contact No. | If Country = Singapore: Must be 8 digits | "Invalid contact number" |
| 12 | User enters Driver/Hirer Email | Format validation | "Invalid email address. Please enter again." |
| 13 | User enters Confirm Driver/Hirer Email | Must match Email field | "Email addresses do not match. Please verify and re-enter." |
| 14 | User selects Relationship to Owner | Dropdown: Employee, Vehicle is leased, Family, Others | - |
| 14a | If "Vehicle is leased" selected | Show Rental Period fields (mandatory) | - |
| 14b | If "Others" selected | Show specify relationship text field (mandatory) | - |
| 15 | User answers Verification Questions (Q1, Q2, Q3) | Mandatory based on selection | - |
| 16 | User uploads Supporting Documents | File format check, Max total 20MB | "Invalid file type or size exceeds 20MB" |
| 17 | User clicks Next | Check ALL mandatory fields filled & valid | Display all errors, scroll to first error |
| 18 | All valid? → Yes | Proceed to Summary/Review Page | - |
| 18a | All valid? → No | Display errors, user corrects, loop back | - |

#### Form Sections with Data Source

| Section | Field | Data Source | Target Field |
|---------|-------|-------------|--------------|
| Notice Details (Read-only) | Notice No | eocms_valid_offence_notice.notice_no | Display only |
| | Vehicle No | eocms_valid_offence_notice.vehicle_no | Display only |
| | Offence Date/Time | eocms_valid_offence_notice.notice_date_and_time | Display only |
| | Location | eocms_valid_offence_notice.offence_location | Display only |
| Owner Particulars (Read-only) | Owner Name | eocms_offence_notice_owner_driver.name | Display only |
| | Owner ID | eocms_offence_notice_owner_driver.id_no | Display only |
| Driver's/Hirer's Particulars | ID Type | User Input | furnish_id_type |
| | ID No. | User Input | furnish_id_no |
| | Name | User Input | furnish_name |
| | Block No | User Input | furnish_mail_blk_no |
| | Floor | User Input | furnish_mail_floor |
| | Street Name | User Input | furnish_mail_street_name |
| | Unit No | User Input | furnish_mail_unit_no |
| | Building Name | User Input | furnish_mail_bldg_name |
| | Postal Code | User Input | furnish_mail_postal_code |
| | Tel No | User Input | furnish_tel_no |
| | Email | User Input | furnish_email_addr |
| Q&A Selection | Furnish As (D/H) | User Input | owner_driver_indicator |
| | Relationship to Owner | User Input | hirer_owner_relationship |
| | Q1 Answer | User Input | ques_one_ans |
| | Q2 Answer | User Input | ques_two_ans |
| | Q3 Answer | User Input | ques_three_ans |
| Supporting Documents | File(s) | User Upload | eocms_offence_attachment |

---

### Tab 3.4: Check Eligibility Sub-flow

**Title:** Check Eligibility to Furnish (Self-Furnish Validation)

**Source:** FD Section 3.2.3, Code: `FurnishValidationServiceImpl.validateSelfFurnish()`

**Swimlanes:** eService Portal

#### Input Variables

| Variable | Source | Description |
|----------|--------|-------------|
| `ownerIdNo` | eocms_offence_notice_owner_driver.id_no | Owner's ID number |
| `ownerIdType` | eocms_offence_notice_owner_driver.id_type | Owner's ID type (N/F/B) |
| `furnishIdNo` | User input | Furnished person's ID number |
| `furnishIdType` | User input | Furnished person's ID type |
| `ownerDriverIndicator` | User input from Q&A | D=Driver, H=Hirer |

#### Flow A: Individual Owner (NRIC/FIN) - FD Section 3.2.3.1

| Step | Description |
|------|-------------|
| 1 | Check owner ID type: Is individual (NRIC or FIN)? |
| 2 | Compare owner ID with furnished ID |
| 3 | Decision: Same ID? |
| 3a | No → Allow submission (different person) |
| 3b | Yes → Check ownerDriverIndicator |
| 4 | Decision: Furnishing as Hirer (H)? |
| 4a | No (D) → Allow submission (furnishing self as driver is allowed) |
| 4b | Yes (H) → Error F01: "You cannot furnish yourself as the hirer." |

#### Flow B: Company Owner (UEN) - FD Section 3.2.3.2

| Step | Description |
|------|-------------|
| 1 | Check owner ID type: Is company (UEN)? |
| 2 | Check furnished ID type |
| 3 | Decision: Furnished ID is also UEN? |
| 3a | No → Allow submission (furnishing individual, no restriction) |
| 3b | Yes → Compare owner UEN with furnished UEN |
| 4 | Decision: Same UEN? |
| 4a | No → Allow submission (different company) |
| 4b | Yes → Error F02: "A company cannot furnish itself as the hirer." |

#### Error Messages

| Code | Message |
|------|---------|
| F01 | "You cannot furnish yourself as the hirer. Please provide another person's details." |
| F02 | "A company cannot furnish itself as the hirer. Please provide another entity's details." |

---

## 5. Flow Details - Section 4

### Tab 4.2: Furnish Submission Acknowledge Flow

**Title:** Handling of Furnish Submission Outcomes

**Swimlanes:** eService Portal

#### API Details (Verified from Code)

| Item | Value |
|------|-------|
| Endpoint | POST /v1/furnishapplication |
| Success Response | `{ data: { appCode: "SUCCESS", message, txnNo } }` |
| Error Response | `{ data: { appCode: "BAD_REQUEST" or "INTERNAL_SERVER_ERROR", message } }` |

#### Flow Steps

| Step | Actor | Description | Notes |
|------|-------|-------------|-------|
| 1 | Portal | Start - User submits furnishing particulars | From Section 3 |
| 2 | Portal | Call Backend API | POST /v1/furnishapplication |
| 3 | Portal | Decision: appCode = SUCCESS? | Check response |
| 3a | Portal | No → Display: Submission Failed | Show error message from response |
| 3b | Portal | Yes → Display: Submission Acknowledgement | Show success screen |
| 4 | Portal | User clicks Print or Furnish Another | |
| 5 | Portal | Decision: Action? | |
| 5a | Portal | Print → Print Acknowledgement | Browser print |
| 5b | Portal | Furnish Another → Redirect to Furnish Tab | |
| 6 | Portal | End | |

#### Success UI Display

| UI Field | Data Source |
|----------|-------------|
| Message | Static: "Your submission has been received." |
| Submission Reference No. | Source: eocms_furnish_application.txn_no |
| Number of Notice(s) Furnished | Source: Calculated (COUNT of notices) |
| Submission Date and Time | Source: eocms_furnish_application.cre_date |
| Notice No | Source: eocms_valid_offence_notice.notice_no |
| Vehicle No | Source: eocms_valid_offence_notice.vehicle_no |
| Offence Date | Source: eocms_valid_offence_notice.notice_date_and_time |
| Driver's/Hirer ID | Source: eocms_furnish_application.furnish_id_no |
| Driver's/Hirer Name | Source: eocms_furnish_application.furnish_name |
| Driver's/Hirer Address | Source: eocms_furnish_application.furnish_mail_* fields |
| Supporting Documents | Source: eocms_offence_attachment.file_name |

**Refer to FD Section 4.3** for complete success screen layout.

#### Error Display

| appCode | Message |
|---------|---------|
| BAD_REQUEST | Your submission could not be processed due to [validation error] |
| INTERNAL_SERVER_ERROR | Your submission could not be processed due to [system error] |

**Refer to FD Section 4.4** for complete error scenarios and messages.

---

## 6. Flow Details - Section 5

### Tab 5.2: Furnish Submission in OCMS Internet Backend

**Title:** Internet Backend Processing

**Refer to FD Section 5.2** for Internet backend process flow details.

**Swimlanes:** eService Backend

#### API Details

| Item | Value |
|------|-------|
| Endpoint | POST /v1/furnishapplication |
| Controller | FurnishSubmissionController.java |
| Service | FurnishSubmissionServiceImpl.java |

#### Complete Request Payload (from FurnishSubmissionRequestDTO.java)

| Group | Fields |
|-------|--------|
| **Notice Info** | noticeNo, vehicleNo, offenceDate, ppCode, ppName, lastProcessingStage |
| **Driver/Hirer Info** | furnishName, furnishIdType (N/F/B), furnishIdNo |
| **Address** | furnishMailBlkNo, furnishMailFloor, furnishMailStreetName, furnishMailUnitNo, furnishMailBldgName, furnishMailPostalCode |
| **Contact** | furnishTelCode, furnishTelNo, furnishEmailAddr |
| **Relationship** | ownerDriverIndicator (O/D/H), hirerOwnerRelationship (H/E/F/O/S), othersRelationshipDesc |
| **Questions** | quesOneAns, quesTwoAns, quesThreeAns |
| **Rental Period** | rentalPeriodFrom, rentalPeriodTo (if Vehicle Leased) |
| **Owner Info** | ownerName, ownerIdNo, ownerIdType, ownerTelCode, ownerTelNo, ownerEmailAddr, corppassStaffName |
| **Optional** | reasonForReview, remarks, supportingDocument[] (max 10 files) |

#### Flow Steps

| Step | Actor | Description | Notes |
|------|-------|-------------|-------|
| 1 | Portal | Portal sends POST /v1/furnishapplication | Trigger |
| 2 | Backend | Start - Receive API request | |
| 3 | Backend | Decision: Request valid? | Format check |
| 3a | Backend | No → Response: Invalid format | appCode: BAD_REQUEST |
| 3b | Backend | Yes → Check mandatory | |
| 4 | Backend | Decision: Mandatory data present? | |
| 4a | Backend | No → Response: Missing data | appCode: BAD_REQUEST |
| 4b | Backend | Yes → Generate txn_no | SQL Server Sequence |
| 5 | Backend | INSERT eocms_furnish_application | status='P', is_sync='N' |
| 6 | Backend | INSERT eocms_offence_attachment | For each file |
| 7 | Backend | Decision: Save successful? | |
| 7a | Backend | No → Response: Save failed | appCode: INTERNAL_SERVER_ERROR |
| 7b | Backend | Yes → Response: Success | appCode: SUCCESS, txnNo |
| 8 | Backend | End | |

#### Database Operations (DD Verified)

**Table: eocms_furnish_application (PII Zone)**

| Field | Data Type | Source |
|-------|-----------|--------|
| txn_no | varchar(20) | SQL Server Sequence |
| notice_no | varchar(10) | Request payload |
| vehicle_no | varchar(14) | eocms_valid_offence_notice.vehicle_no |
| offence_date | datetime2(7) | eocms_valid_offence_notice.notice_date_and_time |
| pp_code | varchar(5) | eocms_valid_offence_notice.pp_code |
| pp_name | varchar(100) | eocms_valid_offence_notice.pp_name |
| last_processing_stage | varchar(3) | eocms_valid_offence_notice.last_processing_stage |
| furnish_name | varchar(66) | User Input |
| furnish_id_type | varchar(1) | User Input (N=NRIC, F=FIN, U=UEN) |
| furnish_id_no | varchar(12) | User Input |
| furnish_mail_* | various | User Input (address fields) |
| owner_driver_indicator | varchar(1) | User Input (D=Driver, H=Hirer) |
| hirer_owner_relationship | varchar(20) | User Input |
| ques_one_ans | varchar(1) | User Input |
| ques_two_ans | varchar(1) | User Input |
| ques_three_ans | varchar(1) | User Input |
| status | varchar(1) | System Generated ('P' = Pending) |
| owner_name | varchar(66) | eocms_offence_notice_owner_driver.name |
| owner_id_no | varchar(12) | SPCP authenticated user ID |
| is_sync | varchar(1) | System Generated ('N' = Not synced) |
| cre_date | datetime2(7) | System Generated (GETDATE()) |
| cre_user_id | varchar(50) | Audit User (ocmsez_app_conn) |

---

### Tab 5.3: Furnish Process in OCMS Intranet Backend

**Title:** Intranet Backend Processing

**Refer to FD Section 5.4** for approval and storing furnished particulars.
**Refer to OCMS 41** for Auto-Approval Eligibility Check rules.

**Swimlanes:** CRON Job, OCMS Intranet Backend

#### Flow Steps

| Step | Actor | Description | Notes |
|------|-------|-------------|-------|
| 1 | CRON | Sync Furnish Driver/Hirer particulars from Internet to Intranet | WHERE is_sync = 'N' |
| 2 | CRON | Trigger Backend Processing | |
| 3 | Backend | Auto-Approval Eligibility Check | Refer to OCMS 41 |
| 4 | Backend | Decision: Auto approve? | Based on OCMS 41 rules |
| 4a | Backend | No → Send to OIC review queue | Manual review needed |
| 4b | Backend | Yes → Auto-approve and process | Skip OIC review |
| 5 | Backend | Decision: Decision? (from OIC or Auto) | |
| 5a | Backend | Reject → UPDATE status = 'R' | End |
| 5b | Backend | Approve → UPDATE status = 'A' | Continue |
| 6 | Backend | INSERT ocms_offence_notice_owner_driver | New Driver/Hirer record |
| 7 | Backend | UPDATE ocms_offence_notice_owner_driver (Owner) | offender_indicator = 'N' |
| 8 | Backend | Decision: owner_driver_indicator = D? | |
| 8a | Backend | Yes (Driver) → Set stage = 'DN1' | |
| 8b | Backend | No (Hirer) → Set stage = 'RD1' | |
| 9 | Backend | UPDATE ocms_valid_offence_notice | Merge point for DN1/RD1 |
| 10 | Backend | Sync all changes to Internet DB | See sync details below |
| 11 | Backend | End | |

#### Sync to Internet DB (Step 10 Details)

Based on FD Section 5.4, after Intranet processing, sync ALL changes to Internet:

| # | Table | Operation | Key Fields |
|---|-------|-----------|------------|
| 1 | eocms_furnish_application | UPDATE | status = 'A' |
| 2 | eocms_offence_notice_owner_driver | INSERT | New offender record |
| 3 | eocms_offence_notice_owner_driver_addr | INSERT | Address record |
| 4 | eocms_valid_offence_notice | UPDATE | next_processing_stage |

**Audit User:** `ocmsez_app_conn` (for Internet DB sync), `ocmsiz_app_conn` (for Intranet DB)

---

## 7. Error Codes Reference

### Furnish-Specific Error Codes

| Code | Constant | Message |
|------|----------|---------|
| F01 | ERROR_CODE_SELF_HIRER_INDIVIDUAL | You cannot furnish yourself as the hirer. Please provide another person's details. |
| F02 | ERROR_CODE_SELF_HIRER_COMPANY | A company cannot furnish itself as the hirer. Please provide another entity's details. |
| F03 | ERROR_CODE_DRIVER_NO_HIRER | As the driver, you must provide the hirer's details to proceed. |
| F04 | ERROR_CODE_HIRER_NO_DRIVER | As the hirer, you must provide the driver's details to proceed. |

### Standard OCMS Error Codes

**Success (2000-2999):**
| Code | Name | Message |
|------|------|---------|
| OCMS-2000 | SUCCESS | Furnish application submitted successfully. |
| OCMS-2001 | CREATED | Record created |
| OCMS-2002 | UPDATED | Record updated |

**Client Errors (4000-4999):**
| Code | Name | Usage |
|------|------|-------|
| OCMS-4000 | BAD_REQUEST | Invalid request format |
| OCMS-4005 | INVALID_INPUT | Field validation failed |
| OCMS-4004 | NOT_FOUND | Notice not found |
| OCMS-4006 | DUPLICATE_RECORD | Duplicate submission |

**Server Errors (5000-5999):**
| Code | Name | Usage |
|------|------|-------|
| OCMS-5000 | INTERNAL_SERVER_ERROR | General server error |
| OCMS-5001 | DATABASE_CONNECTION_FAILED | DB connection issue |
| OCMS-5003 | DATABASE_QUERY_ERROR | DB query/save failed |

### API Response Format

**Success Response:**
```json
{
  "data": {
    "appCode": "SUCCESS",
    "message": "Furnish application submitted successfully.",
    "txnNo": "FU1234567890"
  }
}
```

**Error Response:**
```json
{
  "data": {
    "appCode": "BAD_REQUEST",
    "message": "Furnish name is required."
  }
}
```

---

## 8. Shape Legend

| Shape | Usage | Style |
|-------|-------|-------|
| Ellipse | Start/End | Default |
| Rectangle | Process step | Default |
| Rectangle (dashed) | User action | Dashed border |
| Diamond | Decision point | Default |
| Rectangle (yellow) | Query/Response box | fillColor=#fff2cc |
| Rectangle (purple) | API/Query details | fillColor=#e1d5e7 |
| Rectangle (blue) | Note box | fillColor=#dae8fc |

---

## 9. Connection Rules

| From | To | Line Style |
|------|-----|------------|
| Process | Process | Solid |
| Process | Query Box | Dashed |
| Decision | Yes/No branches | Solid with label |
| API Call | Backend | Dashed |
| Response | Portal | Solid |

---

## 10. Database Tables Referenced

| Table | Zone | Used In | Operation | Key Fields (per DD) |
|-------|------|---------|-----------|---------------------|
| `eocms_offence_notice_owner_driver` | PII | Tab 2.2, 3.3, 5.3 | READ/WRITE | notice_no, id_no, id_type, name, owner_driver_indicator, offender_indicator |
| `eocms_offence_notice_owner_driver_addr` | PII | Tab 5.3 | WRITE | Address fields |
| `eocms_valid_offence_notice` | Internet | Tab 2.2, 3.3, 5.2, 5.3 | READ/WRITE | notice_no, vehicle_no, notice_date_and_time, offence_notice_type, last_processing_stage, suspension_type, crs_reason_of_suspension, epr_reason_of_suspension |
| `eocms_furnish_application` | PII | Tab 2.2-2.4, 5.2, 5.3 | READ/WRITE | See full field list in Section 6 |
| `eocms_offence_attachment` | PII | Tab 5.2 | WRITE | txn_no, notice_no, file_name, mime, size |

---

## 11. UI Messages Summary

| Scenario | Message |
|----------|---------|
| No furnishable notices | "There are no furnishable notices" |
| Offender is Driver | "There are other outstanding notice(s) for [Name] that cannot be furnished. Please proceed to pay." |
| No pending submissions | "There are no Pending Submissions" |
| No approved submissions | "There are no Approved Notices" |
| Submission failed | "Furnish was unsuccessful. Please try again." |
| Submission success | "Your submission has been received." |
| Self-furnish individual | "You cannot furnish yourself as the hirer. Please provide another person's details." |
| Self-furnish company | "A company cannot furnish itself as the hirer. Please provide another entity's details." |

---

## 12. Yi Jie Compliance Checklist

| Item | Status | Notes |
|------|--------|-------|
| API Method = POST | ✅ | All APIs use POST |
| Response Format | ✅ | `{ data: { appCode, message } }` |
| Audit User | ✅ | `ocmsiz_app_conn` / `ocmsez_app_conn` |
| Insert Order | ✅ | Parent (furnish_application) → Child (attachment) |
| No SELECT * | ✅ | Specific fields only |
| Data Source Attribution | ✅ | All fields have source |
| Error Codes | ✅ | From code: ErrorCodes.java, ValidationMessages.java |

---

## 13. Data Dictionary Compliance Checklist

| # | Item | Status | Notes |
|---|------|--------|-------|
| 1 | Table names match DD | ✅ | All table names verified |
| 2 | PK field correct | ✅ | txn_no (not application_id) |
| 3 | Audit fields correct | ✅ | cre_user_id, upd_user_id |
| 4 | Offence date field | ✅ | notice_date_and_time (VON), offence_date (furnish_app) |
| 5 | Furnished person fields | ✅ | furnish_name, furnish_id_type, furnish_id_no |
| 6 | Address fields | ✅ | Split: furnish_mail_blk_no, furnish_mail_floor, etc. |
| 7 | Sync flag | ✅ | is_sync (N/Y) |

---

## 14. Assumptions Log

| # | Assumption | Rationale | Status |
|---|------------|-----------|--------|
| 1 | Tab numbering follows FD section mapping | X.2 = first flow, X.3 = sub-flow | Confirmed |
| 2 | 3 swimlanes for High-Level flows | Portal, Backend, SPCP | Confirmed |
| 3 | 2 swimlanes for Backend Detail flows | Backend, Database | Confirmed |
| 4 | Yellow = Query/Response, Purple = Detail, Blue = Note | Standard color scheme | Confirmed |
| 5 | Dashed = API/Query connection | Visual distinction | Confirmed |
| 6 | Field names verified against DD | Synced from Excel 2026-01-19 | Confirmed |

---

## 15. Recent Updates

### 2026-01-27 - Version 2.0
- Merged plan_flowchart.md and plan_flowchart_section3-5.md into single file
- All tabs (1.2-5.3) now in one document

### Tab 3.4 Changes
- Simplified language (removed code syntax)
- Verified all notes against FD and actual code

### Tab 4.2 Changes
- Fixed API endpoint: `/furnish/submit` → `/v1/furnishapplication`
- Added response structure: `{ data: { appCode, message, txnNo } }`
- Added actual appCode values: `SUCCESS`, `BAD_REQUEST`, `INTERNAL_SERVER_ERROR`
- Changed decision box: "API Response success?" → "appCode = SUCCESS?"

### Tab 5.2 Changes
- Fixed API endpoint: `/furnish/submit` → `/v1/furnishapplication`
- Added complete request payload list (30+ fields from FurnishSubmissionRequestDTO.java)
- Organized fields by group

### Tab 5.3 Changes
- Added merge process box (UPDATE ocms_valid_offence_notice)
- Fixed flow: decision boxes → merge point → sync step
- Updated sync step to include all 4 tables being synced

---

## References

### Functional Flow
- `ocms42.drawio`

### Functional Document
- `v1.3_OCMS 42_Functional_Doc.md`

| TD Section | FD Reference | Content to Refer |
|------------|--------------|------------------|
| 3.2 | FD Section 3.1 | Use Case |
| 3.3 | FD Section 3.2.2 | Furnish Particular Form fields |
| 3.3 | FD Section 3.2.2.3 | Data Validation rules |
| 3.3 | FD Section 3.2.2.4 | User Action and System Response |
| 3.4 | FD Section 3.2.3 | Eligibility rules (Individual) |
| 3.4 | FD Section 3.2.3.2 | Eligibility rules (Company) |
| 4.2 | FD Section 4.3 | Success screen layout |
| 4.2 | FD Section 4.4 | Failure screen and error codes |
| 5.2 | FD Section 5.2 | Internet Backend Process Flow |
| 5.3 | FD Section 5.4 | Saving and Updating Furnished Particulars |

### Data Dictionary
- `Docs/data-dictionary/pii.json` - eocms_furnish_application, eocms_offence_attachment
- `Docs/data-dictionary/internet.json` - eocms_valid_offence_notice
- `Docs/data-dictionary/intranet.json` - ocms_furnish_application, ocms_valid_offence_notice

### External Reference
- **OCMS 41** - Auto-Approval Eligibility Check rules

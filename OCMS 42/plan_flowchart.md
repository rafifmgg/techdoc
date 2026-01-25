# OCMS 42 - Flowchart Planning Document

## Document Information
| Item | Detail |
|------|--------|
| Version | 1.3 |
| Date | 2026-01-25 |
| Source | Functional Flow (ocms 42 functional flow.drawio) |
| Data Reference | Functional Document (v1.3_OCMS 42_Functional_Doc.md) |
| Feature | Furnish Driver's or Hirer's Particulars via eService |

**Note:** Flow structure from Functional Flow (updated), data fields from FD (for reference only).

---

## 1. Technical Flowchart Structure

### Tab Naming Convention
Based on Functional Flow, the Technical Flowchart will have the following tabs:

| Tab | Name | Type | Description | FD Section |
|-----|------|------|-------------|------------|
| 1.2 | High-Level Login and Furnish | High-Level | Main flow: Login via SPCP → Get Furnishable Notices → Submit Furnish Particulars → View Pending | Sec 2, 3 |
| 1.3 | High-Level View Pending | High-Level | View flow: Login via SPCP → Navigate to Pending tab → View list | Sec 2 |
| 1.4 | High-Level View Approved | High-Level | View flow: Login via SPCP → Navigate to Approved tab → View list | Sec 2 |
| 2.2 | Backend Get Furnishable Notices | Detailed | Backend API logic: Query owner_driver table → Check eligibility → Query VON → Filter by existing apps → Return list | Sec 2.2 |
| 2.3 | Backend Get Pending Submissions | Detailed | Backend API logic: Query furnish_application (status P/S, 6 months) → Return list | Sec 2 |
| 2.4 | Backend Get Approved Submissions | Detailed | Backend API logic: Query furnish_application (status A, 6 months) → Return list | Sec 2 |

### Flow Description Summary

| Tab | Purpose | Key Decision Points | Output |
|-----|---------|---------------------|--------|
| 1.2 | Complete furnish journey | Is Driver? Has notices? Validation pass? | Success/Error message |
| 1.3 | View pending submissions | Any pending? | List or "No records" |
| 1.4 | View approved submissions | Any approved? | List or "No records" |
| 2.2 | Eligibility check logic | Has records? Is Driver? Stage valid? PS excluded? Existing app? | Furnishable list |
| 2.3 | Retrieve pending list | Any records? | Pending list |
| 2.4 | Retrieve approved list | Any records? | Approved list |

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

---

## 3. Flow Details by Tab

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

## 4. Shape Legend

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

## 5. Connection Rules

| From | To | Line Style |
|------|-----|------------|
| Process | Process | Solid |
| Process | Query Box | Dashed |
| Decision | Yes/No branches | Solid with label |
| API Call | Backend | Dashed |
| Response | Portal | Solid |

---

## 6. Database Tables Referenced

| Table | Zone | Used In | Operation | Key Fields (per DD) |
|-------|------|---------|-----------|---------------------|
| `eocms_offence_notice_owner_driver` | PII | Tab 2.2 | READ | notice_no, id_no, id_type, name, owner_driver_indicator, offender_indicator |
| `eocms_valid_offence_notice` | Internet | Tab 2.2 | READ | notice_no, vehicle_no, notice_date_and_time, offence_notice_type, last_processing_stage, suspension_type, crs_reason_of_suspension, epr_reason_of_suspension |
| `eocms_furnish_application` | PII | Tab 2.2, 2.3, 2.4 | READ/WRITE | See full field list below |
| `eocms_furnish_application_doc` | PII | Submit flow | WRITE | txn_no, doc_name, cre_date, cre_user_id |

### eocms_furnish_application Full Field List (per FD & DD)

**Notice Info:**
| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| txn_no | varchar(20) | No | PK - Submission reference number |
| notice_no | varchar(10) | No | Notice number |
| vehicle_no | varchar(14) | No | Vehicle number |
| offence_date | datetime2(7) | No | Date of offence |
| pp_code | varchar(5) | No | Car park code |
| pp_name | varchar(100) | No | Car park name |
| last_processing_stage | varchar(3) | No | Notice's current processing stage |

**Furnished Person Particulars:**
| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| furnish_name | varchar(66) | No | Name of furnished driver/hirer |
| furnish_id_type | varchar(1) | No | ID type (N=NRIC, F=FIN, P=Passport) |
| furnish_id_no | varchar(12) | No | ID number |
| furnish_mail_blk_no | varchar(10) | No | Block/house number |
| furnish_mail_floor | varchar(3) | Yes | Floor number |
| furnish_mail_street_name | varchar(32) | No | Street name |
| furnish_mail_unit_no | varchar(5) | Yes | Unit number |
| furnish_mail_bldg_name | varchar(65) | Yes | Building name |
| furnish_mail_postal_code | varchar(6) | No | Postal code |
| furnish_tel_code | varchar(4) | Yes | Country code |
| furnish_tel_no | varchar(12) | Yes | Contact number |
| furnish_email_addr | varchar(320) | Yes | Email address |

**Relationship & Questionnaire:**
| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| owner_driver_indicator | varchar(1) | No | D=Driver, H=Hirer |
| hirer_owner_relationship | varchar(1) | No | F=Family, E=Employee, L=Leased, O=Others |
| others_relationship_desc | varchar(15) | No | If relationship = O |
| ques_one_ans | varchar(32) | No | Answer to Q1 |
| ques_two_ans | varchar(32) | No | Answer to Q2 |
| ques_three_ans | varchar(32) | Yes | Answer to Q3 |
| rental_period_from | datetime2(7) | Yes | If relationship = L |
| rental_period_to | datetime2(7) | Yes | If relationship = L |

**Submitter (Owner) Info:**
| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| owner_name | varchar(66) | No | Submitter name |
| owner_id_no | varchar(12) | No | Submitter ID |
| owner_tel_code | varchar(4) | Yes | Country code |
| owner_tel_no | varchar(20) | Yes | Contact number |
| owner_email_addr | varchar(320) | Yes | Email address |
| corppass_staff_name | varchar(66) | Yes | Corppass representative name |

**Status & System Fields:**
| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| status | varchar(1) | No | P=Pending, A=Approved, R=Rejected, S=Resubmission |
| is_sync | varchar(1) | No | N=Not synced, Y=Synced (default N) |
| reason_for_review | varchar(255) | Yes | Auto-approval failure reason |
| remarks | varchar(200) | Yes | Officer remarks |
| cre_date | datetime2(7) | No | Record creation date |
| cre_user_id | varchar(50) | No | Created by (ocmsez_app_conn) |
| upd_date | datetime2(7) | Yes | Last update date |
| upd_user_id | varchar(50) | Yes | Updated by |

**Note:**
- All queries must use explicit column selection (NO SELECT *)
- Audit fields: `cre_user_id` / `upd_user_id` (not cre_user/upd_user)
- PK for furnish_application is `txn_no` (not application_id)
- Internet zone uses `ocmsez_app_conn` for cre_user_id
- Intranet zone uses `ocmsiz_app_conn` for upd_user_id

---

## 7. Key Decision Points

| Tab | Decision | Yes Branch | No Branch |
|-----|----------|------------|-----------|
| 1.2 | Offender = Driver? | Display warning, cannot furnish | Continue to check notices |
| 1.2 | Any Furnishable Notices? | Display list | Display "no notices" |
| 1.2 | Submission successful? | Redirect to Pending | Display error |
| 1.3 | Any Pending Submissions? | Display list | Display "no pending" |
| 1.4 | Any Approved Submissions? | Display list | Display "no approved" |
| 2.2 | Any record returned? | Continue processing | Return no records |
| 2.2 | indicator value = D? | Return offender is driver | Continue to query notices |
| 2.3 | Any record returned? | Return list | Return no records |
| 2.4 | Any record returned? | Return list | Return no records |

---

## 8. UI Messages Summary

| Scenario | Message |
|----------|---------|
| No furnishable notices | "There are no furnishable notices" |
| Offender is Driver | "There are other outstanding notice(s) for [Name] that cannot be furnished. Please proceed to pay." |
| No pending submissions | "There are no Pending Submissions" |
| No approved submissions | "There are no Approved Notices" |
| Submission failed | "Furnish was unsuccessful. Please try again." |
| Submission success | "Your submission has been received." |

---

## 9. Notes for Technical Flowchart

### 9.1 Visual Standards
1. **Yellow boxes** (`fillColor=#fff2cc`) for all Query and Response boxes
2. **Purple boxes** (`fillColor=#e1d5e7`) for API/Query details
3. **Blue boxes** (`fillColor=#dae8fc`) for Note boxes
4. **Dashed lines** for API calls and Query connections
5. **Solid lines** for normal flow connections

### 9.2 Naming Standards
1. **Expand abbreviations** in flow boxes:
   - VON → Valid Offence Notice
   - NPD → Notice Processing Date
   - SPCP → Singpass/Corppass
2. **Keep domain codes** as-is (PS, ROV, ENA, RD1, RD2, etc.)
3. **Match table/field names** with Data Dictionary exactly

### 9.3 Content Standards (Yi Jie Compliance)
1. **NO SELECT *** - All queries must specify explicit columns
2. **Include all decision branches** with Yes/No labels
3. **Show pagination** where applicable (total, limit, skip)
4. **Show audit fields** in insert operations (`cre_user_id`, `upd_user_id`)
5. **Show sync flag** (`is_sync = 'N'`) for Internet→Intranet data

### 9.4 Flow Box Content Guidelines
1. **Process boxes** - Active verb (Query, Insert, Validate, etc.)
2. **Decision boxes** - Question format (Any record? Is Driver?)
3. **User action boxes** - Dashed border, passive verb (User clicks...)
4. **API Input boxes** - Show required parameters
5. **Query boxes** - Show table name, key conditions

### 9.5 Error Handling
1. **Show error branches** for all decision points
2. **Include error response content** in note boxes
3. **Show rollback** for failed transactions

---

## 10. Data Dictionary Compliance Checklist

| # | Item | Status | Notes |
|---|------|--------|-------|
| 1 | Table names match DD | ✅ | eocms_offence_notice_owner_driver, eocms_valid_offence_notice, eocms_furnish_application |
| 2 | PK field correct | ✅ | txn_no (not application_id) |
| 3 | Audit fields correct | ✅ | cre_user_id, upd_user_id |
| 4 | Offence date field | ✅ | notice_date_and_time (VON), offence_date (furnish_app) |
| 5 | Furnished person fields | ✅ | furnish_name, furnish_id_type, furnish_id_no |
| 6 | Address fields | ✅ | Split: furnish_mail_blk_no, furnish_mail_floor, etc. |
| 7 | Sync flag | ✅ | is_sync (N/Y) |

---

## 11. Assumptions Log

| # | Assumption | Rationale | Status |
|---|------------|-----------|--------|
| 1 | Tab numbering follows FD section mapping | X.2 = first flow, X.3 = sub-flow | Confirmed |
| 2 | 3 swimlanes for High-Level flows | Portal, Backend, SPCP | Confirmed |
| 3 | 2 swimlanes for Backend Detail flows | Backend, Database | Confirmed |
| 4 | Yellow = Query/Response, Purple = Detail, Blue = Note | Standard color scheme | Confirmed |
| 5 | Dashed = API/Query connection | Visual distinction | Confirmed |
| 6 | Field names verified against DD | Synced from Excel 2026-01-19 | Confirmed |

# OCMS 21 - Manage Duplicate Notices

**Prepared by**

MGG TECHSOFT PTE LTD

---

## Version History

| Version | Updated By | Date | Changes |
| --- | --- | --- | --- |
| v1.0 | Claude Code | 14/01/2026 | Document Initiation |

---

## Table of Content

| Section | Content | Pages |
| --- | --- | --- |
| 1 | Explanation: Duplicate Notices Handling within Existing Create Notice Flow | |
| 1.1 | Use Case | |
| 1.2 | Revised Create Notice Processing Flow | |
| 2 | Function: Duplicate Notices Detection and Suspension | |
| 2.1 | Use Case | |
| 2.2 | High Level Processing Flow | |
| 2.2.1 | Data Mapping | |
| 2.2.2 | Success Outcome | |
| 2.2.3 | Error Handling | |
| 2.3 | Sub-flow: DBB Criteria and Handling for Parking Offence Notices (Type O) | |
| 2.3.1 | Data Mapping | |
| 2.3.2 | Success Outcome | |
| 2.3.3 | Error Handling | |
| 2.4 | Sub-flow: DBB Criteria and Handling for Payment Evasion Offence Notices (Type E) | |
| 2.4.1 | Data Mapping | |
| 2.4.2 | Success Outcome | |
| 2.4.3 | Error Handling | |
| 2.5 | Sub-flow: Apply PS-DBB Suspension | |
| 2.5.1 | Data Mapping | |
| 2.5.2 | Success Outcome | |
| 2.5.3 | Error Handling | |

---

# Section 1 - Explanation: Duplicate Notices Handling within Existing Create Notice Flow

## 1.1 Use Case

1. Duplicate Notices detection is a system function that runs automatically as part of post-Create Notice processing for newly created Notices.

2. The function provides automated detection and handling of duplicate Notices issued to the same vehicle on the same day. Where DBB criteria are met, OCMS will automatically and permanently suspend the later-created Notice with PS-DBB, while allowing the earlier Notice to remain valid.

## 1.2 Revised Create Notice Processing Flow

![Flow Diagram](./OCMS21_Flowchart.drawio - Tab 1.2)

NOTE: Refer to Tab 1.2 - DBB in Create Notice of the flowchart file.

| Step | Description | Brief Description |
| --- | --- | --- |
| Receive Data | Backend receives data to create Notice(s) from the calling system | Receive notice creation request |
| Check Duplicate Notice Number | OCMS queries the database to check if the Notice Number already exists | Verify notice number uniqueness |
| Handle Duplicate Notice | If the Notice Number already exists, OCMS returns error code OCMS-4000 with message indicating the Notice already exists and terminates the flow. If the Notice Number is not a duplicate of an existing Notice Number in the database, OCMS proceeds to the next step | Return error if duplicate |
| Identify Offence Type | OCMS identifies the Offence Type of the Notice based on the Computer Rule Code | Determine offence type |
| Route by Offence Type | (a) If Offence Type is Type U, OCMS routes the Notice to the Type U processing flow. (b) If Offence Type is Type O or E, OCMS routes the Notice to the Type O and E NOPO workflow | Route based on type |
| Check Vehicle Registration Type | OCMS checks the Vehicle Registration type | Verify vehicle registration |
| Create Notice | OCMS creates the Notice record in the database | Insert notice record |
| Check Double Booking | OCMS evaluates whether the created Notice results in a Duplicate Notice scenario by calling the DBB Detection function (refer to Section 2) | Call DBB detection |
| Apply PS-DBB Suspension | If the Notice is identified as a duplicate Notice, OCMS applies a PS-DBB suspension (refer to Section 2.5) | Suspend duplicate notice |
| Continue Processing | If the Notice is not identified as a duplicate Notice, OCMS proceeds to AN qualification (for Type O) or completes processing (for Type E) | Continue normal flow |

---

# Section 2 - Function: Duplicate Notices Detection and Suspension

## 2.1 Use Case

1. Duplicate Notices Detection and Suspension is a system function that runs automatically after a new Notice is created in OCMS.

2. The function checks whether the details of the new Notice duplicate those of an existing Notice issued to the same vehicle on the same day, in accordance with pre-defined Double Booking (DBB) criteria.

3. The DBB criteria differ by Offence Type:<br>a. Type O (Parking Offence): Matches by vehicle number, computer rule code, PP code, and date only (time is ignored)<br>b. Type E (Payment Evasion): Matches by vehicle number, computer rule code, PP code, date and time in HH:MM format (seconds are ignored)

4. If the Notice qualifies as a duplicate, OCMS automatically applies permanent suspension (PS-DBB) to the later-created Notice, while preserving the earlier Notice in its current state.

## 2.2 High Level Processing Flow

![Flow Diagram](./OCMS21_Flowchart.drawio - Tab 2.2)

NOTE: Refer to Tab 2.2 - High-Level DBB Processing of the flowchart file.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start DBB Check | Function is triggered from Create Notice flow after notice creation succeeds | Entry point from Create Notice |
| Extract Notice Data | Extract notice data: vehicle_no, computer_rule_code, pp_code, notice_date_and_time | Prepare query parameters |
| Validate Required Fields | Check if all required fields (vehicle_no, computer_rule_code, pp_code, notice_date_and_time) are present and not null | Validate input data |
| Route by Offence Type | (a) If Offence Type is Type O, route to Type O DBB Sub-flow (Section 2.3). (b) If Offence Type is Type E, route to Type E DBB Sub-flow (Section 2.4). (c) If Offence Type is Type U, skip DBB check (manual processing) | Route based on offence type |
| Return Result | Return duplicate detection result (duplicate detected with original notice number, or not duplicate) to the calling function | Return to Create Notice flow |

### 2.2.1 Data Mapping

**Query Parameters (Extracted from Created Notice):**

| Zone | Database Table | Field Name |
| --- | --- | --- |
| Intranet | ocms_valid_offence_notice | vehicle_no, computer_rule_code, pp_code, notice_date_and_time, offence_type |

### 2.2.2 Success Outcome

- Required fields are validated successfully
- Offence type is correctly identified and routed to appropriate sub-flow
- Duplicate detection result is returned to Create Notice flow
- Type U offences are skipped (no automated DBB check)

### 2.2.3 Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Missing Required Fields | One or more required fields (vehicle_no, computer_rule_code, pp_code, notice_date_and_time) is null | Skip DBB check, return not duplicate |
| Invalid Offence Type | Offence type is not O, E, or U | Skip DBB check, return not duplicate |

---

## 2.3 Sub-flow: DBB Criteria and Handling for Parking Offence Notices (Type O)

![Flow Diagram](./OCMS21_Flowchart.drawio - Tab 2.3)

NOTE: Refer to Tab 2.3 - Type O DBB Criteria of the flowchart file.

| Step | Description | Brief Description |
| --- | --- | --- |
| Query Matching Notices | Query existing notices with same vehicle_no, computer_rule_code, and pp_code | Find potential duplicates |
| Handle Query Error | If query fails, retry up to 3 times with exponential backoff (100ms, 200ms, 400ms). If all retries fail, flag for TS-OLD suspension and return | Retry with backoff |
| Check Matching Records | Evaluate whether any existing Notice records with the same offence details are returned. If no records found, return not duplicate | Check query results |
| Compare Date | For Type O, compare DATE ONLY (ignore time). If date does not match, skip to next record | Date-only comparison |
| Check Suspension Status | Check the suspension_type of the existing notice | Evaluate suspension |
| Check PS Eligibility | If suspension_type is PS, check epr_reason_of_suspension and crs_reason_of_suspension. Eligible reasons for Type O: FOR, ANS, DBB, FP, PRA | Check eligibility |
| Confirm Duplicate | If eligibility criteria are met, confirm the notice as a duplicate and store the original notice number | Mark as duplicate |
| Return Result | Return duplicate detection result to the calling function | Return result |

**Type O Suspension Eligibility (per FD Section 3.3):**

| Suspension Type | EPR/CRS Reason | Eligible as Duplicate |
| --- | --- | --- |
| NULL | - | Yes (active notice) |
| PS | FOR | Yes (Foreign) |
| PS | ANS | Yes (Advisory Notice - Type O only) |
| PS | DBB | Yes (Already DBB) |
| PS | FP (CRS) | Yes (Paid - Further Processing) |
| PS | PRA (CRS) | Yes (Paid - Pending Review) |
| PS | Other | No |
| TS/Other | - | Yes (treat as active) |

### 2.3.1 Data Mapping

**Query for Matching Notices:**

| Zone | Database Table | Field Name |
| --- | --- | --- |
| Intranet | ocms_valid_offence_notice | notice_no, vehicle_no, computer_rule_code, pp_code, notice_date_and_time, suspension_type, epr_reason_of_suspension, crs_reason_of_suspension |

### 2.3.2 Success Outcome

- Query successfully retrieves matching notices
- Date comparison (date only, ignore time) is performed correctly
- Suspension eligibility is evaluated per FD Section 3.3 criteria
- Duplicate notice is identified and original notice number is recorded
- Result is returned to High Level Processing Flow

### 2.3.3 Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Query Failure (All Retries) | Database query fails after 3 retry attempts | Apply TS-OLD suspension, return query failed flag |
| No Matching Records | No existing notices found with same offence details | Return not duplicate |
| Self-Matching | Query returns the current notice in results | Skip self-match, continue to next record |
| Null Date | Existing notice has null notice_date_and_time | Skip record, continue to next |

---

## 2.4 Sub-flow: DBB Criteria and Handling for Payment Evasion Offence Notices (Type E)

![Flow Diagram](./OCMS21_Flowchart.drawio - Tab 2.4)

NOTE: Refer to Tab 2.4 - Type E DBB Criteria of the flowchart file.

| Step | Description | Brief Description |
| --- | --- | --- |
| Query Matching Notices | Query existing notices with same vehicle_no, computer_rule_code, and pp_code | Find potential duplicates |
| Handle Query Error | If query fails, retry up to 3 times with exponential backoff (100ms, 200ms, 400ms). If all retries fail, flag for TS-OLD suspension and return | Retry with backoff |
| Check Matching Records | Evaluate whether any existing Notice records with the same offence details are returned. If no records found, return not duplicate | Check query results |
| Compare Date and Time | For Type E, compare DATE and TIME in HH:MM format (ignore seconds). If date or time (hour and minute) does not match, skip to next record | Date+Time comparison |
| Check Suspension Status | Check the suspension_type of the existing notice | Evaluate suspension |
| Check PS Eligibility | If suspension_type is PS, check epr_reason_of_suspension and crs_reason_of_suspension. Eligible reasons for Type E: FOR, FP, PRA only (per FD Section 3.4) | Check eligibility |
| Confirm Duplicate | If eligibility criteria are met, confirm the notice as a duplicate and store the original notice number | Mark as duplicate |
| Return Result | Return duplicate detection result to the calling function | Return result |

**Type E Suspension Eligibility (per FD Section 3.4):**

| Suspension Type | EPR/CRS Reason | Eligible as Duplicate |
| --- | --- | --- |
| NULL | - | Yes (active notice) |
| PS | FOR | Yes (Foreign) |
| PS | FP (CRS) | Yes (Paid - Further Processing) |
| PS | PRA (CRS) | Yes (Paid - Pending Review) |
| PS | Other | No |
| TS/Other | - | Yes (treat as active) |

**Note:** Type E does NOT include ANS or DBB as eligible reasons per FD Section 3.4.

### 2.4.1 Data Mapping

**Query for Matching Notices:**

| Zone | Database Table | Field Name |
| --- | --- | --- |
| Intranet | ocms_valid_offence_notice | notice_no, vehicle_no, computer_rule_code, pp_code, notice_date_and_time, suspension_type, epr_reason_of_suspension, crs_reason_of_suspension |

### 2.4.2 Success Outcome

- Query successfully retrieves matching notices
- Date and time comparison (date + HH:MM, ignore seconds) is performed correctly
- Suspension eligibility is evaluated per FD Section 3.4 criteria (FOR, FP, PRA only)
- Duplicate notice is identified and original notice number is recorded
- Result is returned to High Level Processing Flow

### 2.4.3 Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Query Failure (All Retries) | Database query fails after 3 retry attempts | Apply TS-OLD suspension, return query failed flag |
| No Matching Records | No existing notices found with same offence details | Return not duplicate |
| Self-Matching | Query returns the current notice in results | Skip self-match, continue to next record |
| Null Date/Time | Existing notice has null notice_date_and_time | Skip record, continue to next |
| Time Mismatch | Date matches but hour or minute does not match | Skip record, continue to next |

---

## 2.5 Sub-flow: Apply PS-DBB Suspension

When a Notice is identified as a duplicate, OCMS applies PS-DBB suspension to permanently suspend the later-created Notice. If DBB query fails after retries, OCMS applies TS-OLD suspension as a fallback.

| Step | Description | Brief Description |
| --- | --- | --- |
| Receive Duplicate Result | Receive result from DBB detection (duplicate found or query failed) | Get detection result |
| Determine Suspension Type | If duplicate found, apply PS-DBB. If query failed, apply TS-OLD | Select suspension type |
| Update VON Intranet | Update suspension fields in ocms_valid_offence_notice | Update main table |
| Update VON Internet | Update suspension fields in eocms_valid_offence_notice | Sync to Internet |
| Create Suspension Record | Insert new record in ocms_suspended_notice | Create audit record |

### 2.5.1 Data Mapping

**PS-DBB Suspension Update (Intranet):**

| Zone | Database Table | Field Name |
| --- | --- | --- |
| Intranet | ocms_valid_offence_notice | suspension_type='PS', epr_reason_of_suspension='DBB', epr_date_of_suspension, due_date_of_revival=NULL, upd_user_id='ocmsiz_app_conn' |

**PS-DBB Suspension Update (Internet):**

| Zone | Database Table | Field Name |
| --- | --- | --- |
| Internet | eocms_valid_offence_notice | suspension_type='PS', epr_reason_of_suspension='DBB', epr_date_of_suspension, due_date_of_revival=NULL, upd_user_id='ocmsez_app_conn' |

**PS-DBB Suspension Record Creation:**

| Zone | Database Table | Field Name |
| --- | --- | --- |
| Intranet | ocms_suspended_notice | notice_no, sr_no (MAX+1), date_of_suspension, suspension_source='OCMS', suspension_type='PS', epr_reason_of_suspension='DBB', officer_authorising_suspension='ocmsiz_app_conn', suspension_remarks, cre_user_id='ocmsiz_app_conn', upd_user_id='ocmsiz_app_conn' |

**TS-OLD Suspension Update (Query Failure Fallback):**

| Zone | Database Table | Field Name |
| --- | --- | --- |
| Intranet | ocms_valid_offence_notice | suspension_type='TS', epr_reason_of_suspension='OLD', epr_date_of_suspension, due_date_of_revival, upd_user_id='ocmsiz_app_conn' |
| Internet | eocms_valid_offence_notice | suspension_type='TS', epr_reason_of_suspension='OLD', epr_date_of_suspension, due_date_of_revival, upd_user_id='ocmsez_app_conn' |

**TS-OLD Suspension Record Creation:**

| Zone | Database Table | Field Name |
| --- | --- | --- |
| Intranet | ocms_suspended_notice | notice_no, sr_no (MAX+1), date_of_suspension, suspension_source='OCMS', suspension_type='TS', epr_reason_of_suspension='OLD', officer_authorising_suspension='ocmsiz_app_conn', suspension_remarks, due_date_of_revival, cre_user_id='ocmsiz_app_conn', upd_user_id='ocmsiz_app_conn' |

**Note:** Refer to FD Section 3.5 for complete field specifications.

### 2.5.2 Success Outcome

- Notice is successfully suspended in the database
- PS-DBB suspension is applied to the later-created notice (duplicate found)
- TS-OLD suspension is applied as fallback (query failed)
- Suspension record is created in ocms_suspended_notice table
- Both Intranet and Internet databases are updated with suspension status

### 2.5.3 Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Internet DB Update Failure | Failed to update Internet database | Log error, continue processing (non-blocking) |
| Suspension Record Creation Failure | Failed to create record in ocms_suspended_notice | Log error, retry up to 3 times |

---

**Document References:**
- Functional Document: v1.0_OCMS_21_Duplicate_Notices.md
- Technical Flowchart: OCMS21_Flowchart.drawio
- Planning Documents: plan_api.md, plan_condition.md, plan_flowchart.md

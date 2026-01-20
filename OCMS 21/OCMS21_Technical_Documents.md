# OCMS 21 – Manage Duplicate Notices

**Prepared by**

MGG TECHSOFT PTE LTD

---

## Version History

| Version | Updated By | Date | Changes |
| --- | --- | --- | --- |
| v1.0 | Mubiyarto Wibisono | 14/01/2026 | Document Initiation |

---

## Table of Contents

| Section | Content | Page |
| --- | --- | --- |
| 1 | Explanation: Duplicate Notices Handling within Existing Create Notice Flow | - |
| 1.1 | Use Case | - |
| 1.2 | Revised Create Notice Processing Flow | - |
| 2 | Function: Duplicate Notices Detection and Suspension | - |
| 2.1 | Use Case | - |
| 2.2 | High Level Processing Flow | - |
| 2.3 | Sub-flow: Type O DBB Criteria | - |
| 2.4 | Sub-flow: Type E DBB Criteria | - |
| 2.6 | Data Mapping | - |
| 2.7 | Success Outcome | - |
| 2.8 | Error Handling | - |

---

# Section 1 – Duplicate Notices Handling within Existing Create Notice Flow

## 1.1 Use Case

1. Duplicate Notices detection is a system function that runs automatically as part of post-Create Notice processing for newly created Notices.

2. The function provides automated detection and handling of duplicate Notices issued to the same vehicle on the same day. Where DBB criteria are met, OCMS will automatically and permanently suspend the later-created Notice with PS-DBB, while allowing the earlier Notice to remain valid.

## 1.2 Revised Create Notice Processing Flow

![Flow Diagram](./OCMS21_Flowchart.drawio - Tab 1.2)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Definition | Brief Description |
| --- | --- | --- |
| Receive Data | Receive notice creation request | Backend receives data to create Notice(s). |
| Check Duplicate Notice Number | Verify notice number uniqueness | OCMS queries the database to check if the Notice Number already exists. |
| Handle Duplicate Notice | Return error if duplicate | If the Notice Number already exists, OCMS returns error code OCMS-4000. If the Notice Number is not a duplicate Notice Number, OCMS proceeds to the next step. |
| Identify Offence Type | Determine offence type | OCMS identifies the Offence Type of the Notice. |
| Route by Offence Type | Route based on type | If Offence Type is Type U, OCMS routes the Notice to the Type U processing flow. If Offence Type is Type O or E, OCMS routes the Notice to the Type O and E NOPO workflow. |
| Check Vehicle Registration Type | Verify vehicle registration | OCMS checks the Vehicle Registration type. |
| Create Notice | Insert notice record | OCMS creates the Notice record in the database. |
| Check Double Booking | Call DBB detection | *Refer to Section 2* |
| Apply PS-DBB Suspension | Suspend duplicate notice | *Refer to Section 2.5* |
| Continue Processing | Continue normal flow | If the Notice is not identified as a duplicate Notice, OCMS proceeds to AN qualification (for Type O) or completes processing (for Type E). |

---

# Section 2 – Duplicate Notices Detection and Suspension

## 2.1 Use Case

1. Duplicate Notices Detection and Suspension is a system function that runs automatically after a new Notice is created in OCMS.

2. The function checks whether the details of the new Notice duplicate those of an existing Notice issued to the same vehicle on the same day, in accordance with pre-defined Double Booking (DBB) criteria.

3. When all DBB criteria are met, OCMS will automatically identify the Notice with the later OCMS creation date/time as the duplicate and permanently suspend that Notice under PS-DBB.

4. The earlier-created Notice remains valid and continues through normal processing, based on the workflow defined for the applicable offence type.

5. The objective of this function is to prevent double booking and ensure that a vehicle owner is not issued with more than one Notice for the same offence.

## 2.2 High Level Processing Flow

![Flow Diagram](./OCMS21_Flowchart.drawio - Tab 2.2)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Definition | Brief Description |
| --- | --- | --- |
| Start DBB Check | Entry point from Create Notice | Function is triggered from Create Notice flow after notice creation succeeds. |
| Extract Notice Data | Prepare query parameters | Query notice data. |
| Validate Required Fields | Validate input data | Check if all required fields are present and not null. |
| Route by Offence Type | Route based on offence type | If Type O, route to Type O DBB Sub-flow (*Refer to Section 2.3*). If Type E, route to Type E DBB Sub-flow (*Refer to Section 2.4*). |
| Return Result | Return to Create Notice flow | Return duplicate detection result. |

## 2.3 Sub-flow: Type O DBB Criteria

![Flow Diagram](./OCMS21_Flowchart.drawio - Tab 2.3)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Definition | Brief Description |
| --- | --- | --- |
| Query Matching Notices | Find potential duplicates | Query existing notices. |
| Handle Query Error | Retry with backoff | If query fails, retry up to 3 times with exponential backoff (100ms, 200ms, 400ms). If all retries fail, flag for TS-OLD suspension and return. |
| Check Matching Records | Check query results | Check whether any existing Notice records with the same offence details exist. If no records found, return not duplicate. |
| Compare Date | Date-only comparison | For Type O, compare DATE ONLY (ignore time). If date does not match, skip to next record. |
| Check Suspension Status | Evaluate suspension | Check the suspension_type of the existing notice. |
| Check PS Eligibility | Check eligibility | If suspension_type is PS, check epr_reason and crs_reason. Eligible reasons for Type O: FOR, ANS, DBB, FP, PRA. |
| Confirm Duplicate | Mark as duplicate | If eligibility criteria are found, confirm the notice as a duplicate and store the original notice number. |
| Return Result | Return result | Return duplicate detection result to the calling function. |

## 2.4 Sub-flow: Type E DBB Criteria

![Flow Diagram](./OCMS21_Flowchart.drawio - Tab 2.4)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Definition | Brief Description |
| --- | --- | --- |
| Query Matching Notices | Find potential duplicates | Query existing notices. |
| Handle Query Error | Retry with backoff | If query fails, retry up to 3 times with exponential backoff (100ms, 200ms, 400ms). If all retries fail, flag for TS-OLD suspension and return. |
| Check Matching Records | Check query results | Check whether any existing Notice records are found. If no records found, return not duplicate. |
| Compare Date and Time | Date+Time comparison | For Type E, compare DATE and TIME in HH:MM format. If date or time does not match, skip to next record. |
| Check Suspension Status | Evaluate suspension | Check the suspension_type of the existing notice. |
| Check PS Eligibility | Check eligibility | If suspension_type is PS, check epr_reason and crs_reason. Eligible reasons for Type E: FOR, FP, PRA only. |
| Confirm Duplicate | Mark as duplicate | If eligibility criteria are found, confirm the notice as a duplicate and store the original notice number. |
| Return Result | Return result | Return duplicate detection result to the calling function. |

## 2.6 Data Mapping

### 2.6.1 DBB Suspension - UPDATE Valid Offence Notice

| Zone | Database Table | Field Name | Update Value |
| --- | --- | --- | --- |
| Intranet and Internet | ocms_valid_offence_notice | suspension_type | 'PS' |
| Intranet and Internet | ocms_valid_offence_notice | epr_reason_of_suspension | 'DBB' |
| Intranet and Internet | ocms_valid_offence_notice | epr_date_of_suspension | 2025-01-12 19:00:02 |
| Intranet and Internet | ocms_valid_offence_notice | due_date_of_revival | NULL |
| Intranet and Internet | ocms_valid_offence_notice | upd_user_id | 'ocmsiz_app_conn' |
| Intranet and Internet | ocms_valid_offence_notice | upd_date | 2025-01-14 19:00:02 |

### 2.6.2 DBB Suspension - INSERT Suspended Notice Record

| Zone | Database Table | Field Name | Insert Value |
| --- | --- | --- | --- |
| Intranet | ocms_suspended_notice | notice_no | 500500303J |
| Intranet | ocms_suspended_notice | sr_no | 111 |
| Intranet | ocms_suspended_notice | date_of_suspension | 2025-01-12 19:00:02 |
| Intranet | ocms_suspended_notice | suspension_source | 'OCMS' |
| Intranet | ocms_suspended_notice | suspension_type | 'PS' |
| Intranet | ocms_suspended_notice | reason_of_suspension | 'DBB' |
| Intranet | ocms_suspended_notice | officer_authorising_suspension | 'ocmsiz_app_conn' |
| Intranet | ocms_suspended_notice | due_date_of_revival | NULL |
| Intranet | ocms_suspended_notice | suspension_remarks | 'DBB' |
| Intranet | ocms_suspended_notice | cre_user_id | 'ocmsiz_app_conn' |
| Intranet | ocms_suspended_notice | cre_date | 2025-01-12 19:00:02 |
| Intranet | ocms_suspended_notice | upd_user_id | 'ocmsiz_app_conn' |
| Intranet | ocms_suspended_notice | upd_date | 2025-01-12 19:00:02 |

## 2.7 Success Outcome

- DBB detection runs successfully after notice creation
- Duplicate notices are correctly identified based on Type O or Type E criteria
- PS-DBB suspension is applied to the later-created notice (permanent suspension)
- TS-OLD suspension is applied as fallback when query fails after 3 retries
- Suspension records are created in ocms_suspended_notice table
- Both Intranet and Internet databases are updated with suspension status
- The workflow reaches the End state without triggering any error-handling paths

## 2.8 Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Missing Required Fields | Skip DBB check, return not duplicate | One or more required fields (vehicle_no, computer_rule_code, pp_code, notice_date_and_time) is null |
| Query Failure (All Retries) | Apply TS-OLD suspension, return query failed flag | Database query fails after 3 retry attempts |
| No Matching Records | Return not duplicate | No existing notices found with same offence details |
| Null Date | Skip record, continue to next | Existing notice has null notice_date_and_time |
| Internet DB Update Failure | Log error, continue processing (non-blocking) | Failed to update Internet database |

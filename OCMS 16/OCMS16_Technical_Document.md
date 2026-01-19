# OCMS 16 – Reduction

**Prepared by**

Guthrie GTS

---

## Version History

| Version | Updated By | Date | Changes |
| --- | --- | --- | --- |
| v1.0 | Claude | 18/01/2026 | Document Initiation |

---

## Table of Content

| Section | Content | Pages |
| --- | --- | --- |
| 1 | Reduction Initiated by PLUS | 1 |
| 1.1 | Use Case | 1 |
| 1.2 | High Level Flow | 2 |
| 2 | Reduction Process in OCMS Backend | 3 |
| 2.1 | Use Case | 3 |
| 2.2 | PLUS Apply Reduction Flow | 4 |
| 2.2.1 | API Specification | 5 |
| 2.2.2 | Data Mapping | 7 |
| 2.2.3 | Success Outcome | 8 |
| 2.2.4 | Error Handling | 9 |
| 3 | Reduction Scenarios and Outcomes | 10 |

---

# Section 1 – Reduction Initiated by PLUS

## 1.1 Use Case

1. A Reduction refers to the scenario where PLUS triggers a request to OCMS to reduce the composition amount of a notice.

2. This occurs after PLUS officers review the appeal and determine that the notice qualifies for a reduction.

3. The PLUS system performs validation to decide whether a Notice is eligible for a Reduction before the reduction request is sent to the OCMS Intranet Backend.

4. When the OCMS Intranet Backend receives a reduction request, it performs the following actions:<br>a. Verifies whether the notice has been paid to determine if a reduction is required.<br>b. Checks Computer Rule Code eligibility: If the computer rule code is 30305/31302/30302/21300, composition amount can be reduced at NPA/ROV/ENA/RD1/RD2/RR3/DN1/DN2/DR3 stage.<br>c. Checks that the last processing stage of the notice is RR3/DR3 if the computer rule code is not in the eligible list.<br>d. Updates the notice's payable amount with the reduced amount and suspends the Notice with TS-RED.

**Note:** For MVP1, the reduction process is fully managed within the PLUS system, while the OCMS backend is only responsible for updating the notice with the data provided in the reduction request.

## 1.2 High Level Flow

![High Level Flow](./images/Reduction_High_Level.png)

*Refer to Tab: Reduction_High_Level*

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | PLUS officer initiates a reduction request for a notice via PLUS Staff Portal | Entry point |
| Initiate Reduction | Officer selects notice and initiates reduction request in PLUS system | User action |
| Check Eligibility | PLUS system checks if the notice is eligible for reduction based on pre-defined rules | Pre-condition validation |
| Send Request to OCMS | If eligible, PLUS sends a request to OCMS Intranet Backend for Reduction via API | API call to OCMS |
| Route Request | Request is routed through APIM to OCMS Backend | Gateway routing |
| Receive Request | OCMS Intranet Backend receives the request | Request reception |
| Validate Notice | OCMS validates format, mandatory data, payment status, and eligibility | Backend validation |
| Process Reduction | When validation passes, OCMS updates the notice with reduced amount and TS-RED suspension | Database update |
| Response to PLUS | OCMS responds to PLUS indicating whether the reduction was successful or failed | API response |
| End | Process complete | Exit point |

---

# Section 2 – Reduction Process in OCMS Backend

## 2.1 Use Case

1. The OCMS backend performs the reduction process when it receives a reduction request from the PLUS system via API through APIM.

2. OCMS validates the request format and mandatory fields to ensure the data is complete and correct.

3. OCMS verifies whether the notice has already been paid. If it has been paid, OCMS responds to PLUS that the notice has been paid and the reduction is not applicable.

4. If the notice is outstanding, OCMS checks the eligibility based on:<br>a. Computer Rule Code: If in eligible list (30305, 31302, 30302, 21300), check if last processing stage is in extended list (NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3).<br>b. If Computer Rule Code is not in eligible list, only notices at RR3/DR3 stage are eligible for reduction.

5. If the notice is eligible, OCMS updates the notice with the reduced payable amount provided in the request and suspends the notice with a temporary suspension reason "RED".

6. OCMS returns a response to PLUS indicating whether the reduction was successfully applied or if it failed.

**Note:** Reduction of notices can be performed at the RR3/DR3 stage and at court stages from CFC to CWT. For MVP1, only the RR3/DR3 stage is included.

## 2.2 PLUS Apply Reduction Flow

![PLUS Apply Reduction Flow](./images/PLUS_Apply_Reduction.png)

*Refer to Tab: PLUS_Apply_Reduction*

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Start | PLUS System sends a request to OCMS Intranet Backend for Reduction via API | Entry point |
| Receive Request | OCMS Intranet Backend receives the request routed through APIM | Request reception |
| Validate Request Format | OCMS checks if the API request data is complete and valid. If the request is invalid, OCMS responds to PLUS with "Invalid format" and ends the process | Format validation |
| Check Mandatory Data | OCMS checks whether all required fields are present. If data is missing, OCMS responds to PLUS with "Missing data" and ends the process | Mandatory field check |
| Query Notice | OCMS queries the notice by notice_no from ocms_valid_offence_notice table | Database lookup |
| Check Notice Exists | If notice is not found, OCMS responds to PLUS with "Notice not found" and ends the process | Existence check |
| Check Already Reduced | If notice already has TS-RED status, OCMS returns "Reduction Success" (idempotent response) without making changes | Idempotency check |
| Check Payment Status | OCMS checks the CRS Reason of Suspension field. If the value is "FP" or "PRA", the notice has been paid and OCMS responds with "Notice has been paid" | Payment validation |
| Check Computer Rule Code | If notice is outstanding, OCMS validates whether the notice's computer rule code matches any of the Eligible Computer Rule Codes (30305, 31302, 30302, 21300) | Eligibility check - Rule Code |
| Check Last Processing Stage (Extended) | For eligible codes, OCMS checks if last processing stage is in extended list (NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3). If not, respond "Notice is not eligible" | Eligibility check - Stage |
| Check Last Processing Stage (Special Case) | For non-eligible codes, OCMS checks if last processing stage is RR3 or DR3. If not, respond "Notice is not eligible" | Special case eligibility |
| Validate Amounts and Dates | OCMS validates that reduction amounts are consistent and expiry date is after reduction date | Amount/Date validation |
| Begin Transaction | OCMS begins database transaction for atomic updates | Transaction start |
| Update VON | OCMS updates ocms_valid_offence_notice with suspension_type='TS', epr_reason_of_suspension='RED', new amount_payable, epr_date_of_suspension, and due_date_of_revival | Intranet VON update |
| Insert Suspended Notice | OCMS inserts a new record into ocms_suspended_notice table to record the suspension | Suspension record |
| Insert Reduced Offence Amount | OCMS inserts a new record into ocms_reduced_offence_amount table to log the reduction | Reduction log |
| Update Internet eVON | OCMS updates eocms_valid_offence_notice with 4 fields: suspension_type, epr_reason_of_suspension, epr_date_of_suspension, amount_payable | Internet mirror |
| Check Update Success | If any update fails, OCMS performs rollback and responds with "Reduction fail" | Success validation |
| Commit Transaction | If all updates successful, OCMS commits the transaction | Transaction commit |
| Response Success | OCMS responds to PLUS with "Reduction Success" | Success response |
| End | Process complete | Exit point |

### 2.2.1 API Specification

#### API for PLUS System Integration

##### POST /v1/plus-apply-reduction

| Field | Value |
| --- | --- |
| API Name | plus-apply-reduction |
| URL | UAT: https://[uat-domain]/ocms/v1/plus-apply-reduction <br> PRD: https://[prd-domain]/ocms/v1/plus-apply-reduction |
| Description | Apply reduction to a notice by updating the payable amount and suspending with TS-RED |
| Method | POST |
| Header | `{ "Authorization": "Bearer [token]", "Content-Type": "application/json" }` |

**Request Payload:**

```json
{
  "noticeNo": "500500303J",
  "amountReduced": 55.00,
  "amountPayable": 15.00,
  "dateOfReduction": "2025-08-01T19:00:02",
  "expiryDateOfReduction": "2025-08-15T00:00:00",
  "reasonOfReduction": "RED",
  "authorisedOfficer": "JOHNLEE",
  "suspensionSource": "005",
  "remarks": "Reduction granted based on appeal review"
}
```

**Request Field Description:**

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| noticeNo | string | Yes | Notice number to be reduced |
| amountReduced | decimal | Yes | The amount reduced from the original composition amount |
| amountPayable | decimal | Yes | The new amount payable after reduction |
| dateOfReduction | datetime | Yes | Date and time when reduction is applied (format: yyyy-MM-dd'T'HH:mm:ss) |
| expiryDateOfReduction | datetime | Yes | Expiry date of the reduction offer / due date of revival |
| reasonOfReduction | string | Yes | Reason for the reduction |
| authorisedOfficer | string | Yes | Officer authorizing the reduction |
| suspensionSource | string | Yes | Source system code (e.g., "005" for PLUS) |
| remarks | string | No | Optional remarks for the reduction |

**Success Response (HTTP 200):**

```json
{
  "appCode": "OCMS-2000",
  "message": "Reduction Success"
}
```

**Error Responses:**

| HTTP Status | Response | Description |
| --- | --- | --- |
| 400 | `{ "appCode": "OCMS-4000", "message": "Invalid format" }` | Request data is incomplete or has invalid structure |
| 400 | `{ "appCode": "OCMS-4000", "message": "Missing data" }` | Mandatory fields are missing |
| 404 | `{ "appCode": "OCMS-4004", "message": "Notice not found" }` | Notice number does not exist in OCMS |
| 409 | `{ "appCode": "OCMS-4090", "message": "Notice has been paid" }` | Notice has already been fully paid |
| 409 | `{ "appCode": "OCMS-4091", "message": "Notice is not eligible" }` | Notice does not meet eligibility criteria |
| 500 | `{ "appCode": "OCMS-5000", "message": "Reduction fail" }` | Database update failed or internal error |
| 503 | `{ "appCode": "OCMS-5001", "message": "System unavailable" }` | OCMS backend cannot access database |

### 2.2.2 Data Mapping

#### Database Data Mapping

| Zone | Database Table | Field Name |
| --- | --- | --- |
| Intranet | ocms_valid_offence_notice | notice_no (WHERE clause) |
| Intranet | ocms_valid_offence_notice | suspension_type |
| Intranet | ocms_valid_offence_notice | epr_reason_of_suspension |
| Intranet | ocms_valid_offence_notice | amount_payable |
| Intranet | ocms_valid_offence_notice | epr_date_of_suspension |
| Intranet | ocms_valid_offence_notice | due_date_of_revival |
| Intranet | ocms_suspended_notice | notice_no |
| Intranet | ocms_suspended_notice | date_of_suspension |
| Intranet | ocms_suspended_notice | sr_no |
| Intranet | ocms_suspended_notice | suspension_source |
| Intranet | ocms_suspended_notice | suspension_type |
| Intranet | ocms_suspended_notice | reason_of_suspension |
| Intranet | ocms_suspended_notice | officer_authorising_suspension |
| Intranet | ocms_suspended_notice | due_date_of_revival |
| Intranet | ocms_suspended_notice | suspension_remarks |
| Intranet | ocms_reduced_offence_amount | notice_no |
| Intranet | ocms_reduced_offence_amount | date_of_reduction |
| Intranet | ocms_reduced_offence_amount | sr_no |
| Intranet | ocms_reduced_offence_amount | amount_reduced |
| Intranet | ocms_reduced_offence_amount | amount_payable |
| Intranet | ocms_reduced_offence_amount | reason_of_reduction |
| Intranet | ocms_reduced_offence_amount | expiry_date |
| Intranet | ocms_reduced_offence_amount | officer_authorising_reduction |
| Intranet | ocms_reduced_offence_amount | remarks |
| Internet | eocms_valid_offence_notice | notice_no (WHERE clause) |
| Internet | eocms_valid_offence_notice | suspension_type |
| Internet | eocms_valid_offence_notice | epr_reason_of_suspension |
| Internet | eocms_valid_offence_notice | epr_date_of_suspension |
| Internet | eocms_valid_offence_notice | amount_payable |

#### Request to Database Field Mapping

| Request Field | Database Field | Value/Transformation |
| --- | --- | --- |
| noticeNo | notice_no | Direct mapping (WHERE clause) |
| amountPayable | amount_payable | Direct mapping |
| dateOfReduction | epr_date_of_suspension, date_of_suspension, date_of_reduction | Direct mapping |
| expiryDateOfReduction | due_date_of_revival, expiry_date | Direct mapping |
| reasonOfReduction | reason_of_reduction | Direct mapping |
| authorisedOfficer | officer_authorising_suspension, officer_authorising_reduction | Direct mapping |
| suspensionSource | suspension_source | Direct mapping |
| amountReduced | amount_reduced | Direct mapping |
| remarks | suspension_remarks, remarks | Direct mapping |
| - | suspension_type | Fixed: "TS" |
| - | epr_reason_of_suspension, reason_of_suspension | Fixed: "RED" |
| - | sr_no | Auto-generated (SQL Server sequence) |

**Note:** The due_date_of_revival field does NOT exist in the Internet database schema. Only 4 fields are synced to Internet.

### 2.2.3 Success Outcome

When reduction is successfully applied:

- Notice's suspension_type is set to "TS" (Temporary Suspension)
- Notice's epr_reason_of_suspension is set to "RED" (Pay Reduced Amount)
- Notice's amount_payable is updated with the new reduced amount
- Notice's epr_date_of_suspension is set to the date of reduction
- Notice's due_date_of_revival is set to the expiry date of reduction
- New record is created in ocms_suspended_notice table
- New record is created in ocms_reduced_offence_amount table
- Internet database eocms_valid_offence_notice is mirrored with 4 fields
- Response "Reduction Success" is returned to PLUS system

### 2.2.4 Error Handling

#### Application Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Invalid Format | Request JSON is malformed or contains incorrect data types | Return HTTP 400 with "Invalid format" |
| Missing Data | One or more mandatory fields are not provided | Return HTTP 400 with "Missing data" |
| Notice Not Found | Notice number does not exist in ocms_valid_offence_notice | Return HTTP 404 with "Notice not found" |
| Notice Paid | CRS Reason of Suspension is "FP" or "PRA" | Return HTTP 409 with "Notice has been paid" |
| Not Eligible | Notice does not meet eligibility criteria for reduction | Return HTTP 409 with "Notice is not eligible" |
| Database Error | Database operation fails during update | Rollback transaction, return HTTP 500 with "Reduction fail" |
| System Unavailable | OCMS cannot access database or system error | Return HTTP 503 with "System unavailable" |

#### API Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| Invalid Format | OCMS-4000 | Invalid format | Request data is incomplete or has invalid structure |
| Missing Data | OCMS-4000 | Missing data | Mandatory fields are missing |
| Notice Not Found | OCMS-4004 | Notice not found | Notice number does not exist in OCMS |
| Notice Paid | OCMS-4090 | Notice has been paid | Notice has already been fully paid (CRS = FP/PRA) |
| Not Eligible | OCMS-4091 | Notice is not eligible | Notice does not meet eligibility criteria |
| Database Error | OCMS-5000 | Reduction fail | Database update failed or internal error |
| System Unavailable | OCMS-5001 | System unavailable | OCMS backend cannot access database |

#### Eligibility Rules

**Eligible Computer Rule Codes:**

| Code | Allowed Processing Stages |
| --- | --- |
| 30305 | NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3 |
| 31302 | NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3 |
| 30302 | NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3 |
| 21300 | NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3 |
| Others | RR3, DR3 only (Special Case) |

#### Transaction Management

All database updates are performed within a single transaction:

1. Update ocms_valid_offence_notice
2. Insert into ocms_suspended_notice
3. Insert into ocms_reduced_offence_amount
4. Update eocms_valid_offence_notice (Internet)

If any step fails, the entire transaction is rolled back and "Reduction fail" is returned.

#### Idempotency

If a notice already has TS-RED status (suspension_type='TS' AND epr_reason_of_suspension='RED'), the API returns "Reduction Success" without making any changes. This prevents duplicate reductions from retried requests.

---

# Section 3 – Reduction Scenarios and Outcomes

The table below lists the possible scenarios and outcomes that may occur and how OCMS handles each scenario:

| Scenario | Description | Outcome | Handling |
| --- | --- | --- | --- |
| Invalid Request Format | Request received by OCMS from PLUS is incomplete or has invalid structure | Rejected by OCMS | Respond to PLUS with "Invalid format" and end processing |
| Missing Mandatory Data | Request is missing mandatory data fields | Rejected by OCMS | Respond to PLUS with "Missing data" and end processing |
| Notice Not Found | Notice number does not exist in OCMS | Rejected by OCMS | Respond to PLUS with "Notice not found" and end processing |
| Notice Already Reduced | Notice already has TS-RED status | Accepted (Idempotent) | Respond to PLUS with "Reduction Success" without changes |
| Notice Has Been Paid | The notice has already been fully paid (CRS = FP or PRA) | Rejected by OCMS | Respond to PLUS with "Notice has been paid" and end processing |
| Notice Not Eligible (Rule Code) | Notice's computer rule code is not in eligible list and stage is not RR3/DR3 | Rejected by OCMS | Respond to PLUS with "Notice is not eligible" and end processing |
| Notice Not Eligible (Stage) | Notice's computer rule code is eligible but stage is not in extended list | Rejected by OCMS | Respond to PLUS with "Notice is not eligible" and end processing |
| Invalid Amounts | Amount reduced exceeds original or amount payable calculation is incorrect | Rejected by OCMS | Respond to PLUS with "Invalid format" and end processing |
| Invalid Dates | Expiry date is before or equal to reduction date | Rejected by OCMS | Respond to PLUS with "Invalid format" and end processing |
| Successful Update | Request is valid, notice is eligible, all updates succeed | Notice updated and suspended | Respond to PLUS with "Reduction Success" and end processing |
| Update Failure | Request passes validation but update fails internally (e.g., DB error) | Update unsuccessful | Rollback all changes, respond to PLUS with "Reduction fail" |
| Partial Update | Data updated but subsequent operation fails | Incomplete processing | Rollback all changes, respond to PLUS with "Reduction fail" |
| Backend System Unavailable | OCMS cannot access the database or system error occurs | Update fails | Respond to PLUS with "System unavailable" error and end processing |

---

## Appendix: Processing Stage Descriptions

| Stage Code | Description |
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

---

## Appendix: CRS Reason of Suspension Values

| Value | Meaning | Can Apply Reduction? |
| --- | --- | --- |
| NULL/BLANK | Notice is outstanding | Yes |
| FP | Full Payment made | No |
| PRA | Paid (PRA status) | No |

---

*End of Document*

# Flowchart Plan: OCMS 16 - Reduction

## Overview

| Attribute | Value |
| --- | --- |
| Feature Name | Reduction |
| Version | v1.1 |
| Author | Claude |
| Created Date | 18/01/2026 |
| Last Updated | 22/01/2026 |
| Status | Draft |
| Related Documents | plan_api.md, plan_condition.md |
| Functional Document | OCMS 16 FD v1.3 |
| Functional Flowchart | OCMS16-Functional-Flowchart.drawio |

**Scope Note (MVP1):**
- For MVP1, manual reduction can only be performed from the PLUS system
- Court stages (CFC to CWT) are NOT implemented - future scope beyond MVP1

---

## 1. Flowchart Tabs Structure

Based on CLAUDE.md guidelines and reference flowcharts, the Technical Flowchart should have the following tabs:

| Tab No | Tab Name | Purpose | Reference |
| --- | --- | --- | --- |
| 1 | Reduction_High_Level | Overview of entire reduction flow (simple) | FD Section 2.2 |
| 2 | PLUS_Apply_Reduction | Detailed backend processing flow | FD Section 3.2 |

---

## 2. Swimlane Definition

| Swimlane | Fill Color | Stroke Color | Description |
| --- | --- | --- | --- |
| PLUS Staff Portal | #dae8fc (light blue) | #6c8ebf | External frontend (PLUS system) |
| OCMS Admin API Intranet | #d5e8d4 (light green) | #82b366 | OCMS Backend API services |
| Intranet Database | #fff2cc (light yellow) | #d6b656 | OCMS Intranet database |
| Internet Database | #fff2cc (light yellow) | #d6b656 | OCMS Internet database |

---

## Section 2: High-Level Flow

### 2.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | Reduction via PLUS Staff Portal |
| Section | High Level |
| Trigger | PLUS officer initiates reduction for a notice |
| Frequency | On-demand (when appeal is approved) |
| Systems Involved | PLUS Staff Portal, APIM, OCMS Backend, Intranet DB, Internet DB |

### 2.2 High-Level Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                     HIGH LEVEL REDUCTION FLOW                                    │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  PLUS Staff Portal                                                               │
│  ┌─────────┐    ┌──────────────────┐    ┌──────────────────┐                    │
│  │  Start  │───►│ Initiate         │───►│ Check Eligibility │                   │
│  └─────────┘    │ Reduction        │    │ (Pre-condition)    │                   │
│                 └──────────────────┘    └────────┬───────────┘                   │
│                                                  │                               │
│                                                  │ If Eligible                   │
│                                                  ▼                               │
│                                         ┌──────────────────┐                     │
│                                         │ Send Reduction   │                     │
│                                         │ Request to OCMS  │──── API Request ───►│
│                                         └──────────────────┘                     │
│                                                                                  │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  APIM Gateway                                                                    │
│                                         ┌──────────────────┐                     │
│                               ─────────►│ Route Request    │──────────►          │
│                                         │ to OCMS Backend  │                     │
│                                         └──────────────────┘                     │
│                                                                                  │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  OCMS Backend                                                                    │
│                 ┌──────────────────┐    ┌──────────────────┐                     │
│       ─────────►│ Receive Request  │───►│ Validate Notice  │                     │
│                 └──────────────────┘    │ for Reduction    │                     │
│                                         └────────┬─────────┘                     │
│                                                  │                               │
│                                    ┌─────────────┴─────────────┐                 │
│                                    │                           │                 │
│                              Validation Pass            Validation Fail          │
│                                    │                           │                 │
│                                    ▼                           ▼                 │
│                         ┌──────────────────┐         ┌──────────────────┐        │
│                         │ Process          │         │ Response to PLUS │        │
│                         │ Reduction        │         │ (Fail/Not Eligible)│       │
│                         └────────┬─────────┘         └────────┬─────────┘        │
│                                  │                            │                  │
│                                  ▼                            │                  │
│                         ┌──────────────────┐                  │                  │
│                         │ Response to PLUS │                  │                  │
│                         │ (Success/Fail)   │                  │                  │
│                         └────────┬─────────┘                  │                  │
│                                  │                            │                  │
│                                  └────────────┬───────────────┘                  │
│                                               ▼                                  │
│                                         ┌───────────┐                            │
│                                         │    End    │                            │
│                                         └───────────┘                            │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 2.3 High-Level Steps Table

| Step | Swimlane | Definition | Brief Description |
| --- | --- | --- | --- |
| Start | PLUS Staff Portal | Entry point | PLUS officer initiates reduction for a notice |
| Initiate Reduction | PLUS Staff Portal | User action | Officer selects notice and initiates reduction request |
| Check Eligibility | PLUS Staff Portal | Pre-condition | PLUS system checks if notice is eligible for reduction |
| Send Request to OCMS | PLUS Staff Portal | API call | If eligible, PLUS sends reduction request to OCMS via API |
| Route Request | APIM | Gateway | Request is routed through APIM to OCMS Backend |
| Receive Request | OCMS Backend | Process | OCMS Backend receives the reduction request |
| Validate Notice | OCMS Backend | Validation | OCMS validates format, data, payment status, eligibility |
| Process Reduction | OCMS Backend | Process | If valid, OCMS updates notice with reduced amount and TS-RED |
| Response to PLUS | OCMS Backend | Response | Send success or failure response back to PLUS |
| End | - | Exit point | Process complete |

---

## Section 3: Detailed Flow

### 3.1 Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | PLUS Apply Reduction (Backend Processing) |
| Section | Detailed |
| Trigger | POST /v1/plus-apply-reduction API call from PLUS |
| Frequency | On-demand |
| Systems Involved | OCMS Admin API Intranet, Intranet DB, Internet DB |

### 3.2 Detailed Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                           PLUS APPLY REDUCTION - DETAILED FLOW                                       │
├──────────────────┬──────────────────────────────────────────────────┬───────────────┬───────────────┤
│  PLUS System     │  OCMS Admin API Intranet                         │ Intranet DB   │ Internet DB   │
├──────────────────┼──────────────────────────────────────────────────┼───────────────┼───────────────┤
│                  │                                                  │               │               │
│   ┌─────────┐    │                                                  │               │               │
│   │  Start  │    │                                                  │               │               │
│   └────┬────┘    │                                                  │               │               │
│        │         │                                                  │               │               │
│        ▼         │                                                  │               │               │
│ ┌─────────────┐  │                                                  │               │               │
│ │ POST        │  │                                                  │               │               │
│ │ /v1/plus-   │──┼─────────────────────────────────────►            │               │               │
│ │ apply-      │  │                                                  │               │               │
│ │ reduction   │  │                                                  │               │               │
│ └─────────────┘  │                                                  │               │               │
│                  │  ┌──────────────────┐                            │               │               │
│                  │  │ Receive Request  │                            │               │               │
│                  │  └────────┬─────────┘                            │               │               │
│                  │           │                                      │               │               │
│                  │           ▼                                      │               │               │
│                  │  ◆ Data format correct?                          │               │               │
│                  │  │                                               │               │               │
│                  │  ├── No ──► Return "Invalid format" ──► End      │               │               │
│                  │  │                                               │               │               │
│                  │  └── Yes                                         │               │               │
│                  │           │                                      │               │               │
│                  │           ▼                                      │               │               │
│                  │  ◆ Mandatory data present?                       │               │               │
│                  │  │                                               │               │               │
│                  │  ├── No ──► Return "Missing data" ──► End        │               │               │
│                  │  │                                               │               │               │
│                  │  └── Yes                                         │               │               │
│                  │           │                                      │               │               │
│                  │           ▼                                      │               │               │
│                  │  ┌──────────────────┐                            │               │               │
│                  │  │ Query Notice by  │- - - - - - - - - - - - - - ┼─► SELECT      │               │
│                  │  │ notice_no        │                            │   FROM VON    │               │
│                  │  └────────┬─────────┘◄ - - - - - - - - - - - - - ┼── Return      │               │
│                  │           │                                      │               │               │
│                  │           ▼                                      │               │               │
│                  │  ◆ Notice exists?                                │               │               │
│                  │  │                                               │               │               │
│                  │  ├── No ──► Return "Notice not found" ──► End    │               │               │
│                  │  │                                               │               │               │
│                  │  └── Yes                                         │               │               │
│                  │           │                                      │               │               │
│                  │           ▼                                      │               │               │
│                  │  ◆ Already reduced (TS-RED)?                     │               │               │
│                  │  │                                               │               │               │
│                  │  ├── Yes ─► Return "Reduction Success" ──► End   │               │               │
│                  │  │          (Idempotent)                         │               │               │
│                  │  │                                               │               │               │
│                  │  └── No                                          │               │               │
│                  │           │                                      │               │               │
│                  │           ▼                                      │               │               │
│                  │  ◆ Notice paid (CRS=FP/PRA)?                     │               │               │
│                  │  │                                               │               │               │
│                  │  ├── Yes ─► Return "Notice has been paid" ──► End│               │               │
│                  │  │                                               │               │               │
│                  │  └── No                                          │               │               │
│                  │           │                                      │               │               │
│                  │           ▼                                      │               │               │
│                  │  ◆ Computer Rule Code eligible?                  │               │               │
│                  │  │  (30305/31302/30302/21300)                    │               │               │
│                  │  │                                               │               │               │
│                  │  ├── Yes ─► ◆ Stage in extended list?            │               │               │
│                  │  │          │  (NPA/ROV/ENA/RD1/RD2/             │               │               │
│                  │  │          │   RR3/DN1/DN2/DR3)                 │               │               │
│                  │  │          │                                    │               │               │
│                  │  │          ├─ Yes ─► [Continue to Update]       │               │               │
│                  │  │          │                                    │               │               │
│                  │  │          └─ No ──► Return "Not eligible" ──► End              │               │
│                  │  │                                               │               │               │
│                  │  └── No ──► ◆ Stage = RR3 or DR3?                │               │               │
│                  │             │                                    │               │               │
│                  │             ├─ Yes ─► [Continue to Update]       │               │               │
│                  │             │                                    │               │               │
│                  │             └─ No ──► Return "Not eligible" ──► End              │               │
│                  │                                                  │               │               │
│                  │           │                                      │               │               │
│                  │           ▼                                      │               │               │
│                  │  ┌──────────────────┐                            │               │               │
│                  │  │ Validate Amounts │                            │               │               │
│                  │  │ and Dates        │                            │               │               │
│                  │  └────────┬─────────┘                            │               │               │
│                  │           │                                      │               │               │
│                  │           ▼                                      │               │               │
│                  │  ◆ Amounts/Dates valid?                          │               │               │
│                  │  │                                               │               │               │
│                  │  ├── No ──► Return "Invalid format" ──► End      │               │               │
│                  │  │                                               │               │               │
│                  │  └── Yes                                         │               │               │
│                  │           │                                      │               │               │
│                  │           ▼                                      │               │               │
│                  │  ┌──────────────────────────────────────────┐    │               │               │
│                  │  │ BEGIN TRANSACTION                        │    │               │               │
│                  │  └──────────────────────────────────────────┘    │               │               │
│                  │           │                                      │               │               │
│                  │           ▼                                      │               │               │
│                  │  ┌──────────────────┐                            │               │               │
│                  │  │ Step 1: Update   │- - - - - - - - - - - - - - ┼─► UPDATE      │               │
│                  │  │ VON with TS-RED  │                            │   VON         │               │
│                  │  │ and new amount   │◄ - - - - - - - - - - - - - ┼── OK          │               │
│                  │  └────────┬─────────┘                            │               │               │
│                  │           │                                      │               │               │
│                  │           ▼                                      │               │               │
│                  │  ┌──────────────────┐                            │               │               │
│                  │  │ Step 2: Insert   │- - - - - - - - - - - - - - ┼─► INSERT      │               │
│                  │  │ Suspended Notice │                            │   SN          │               │
│                  │  └────────┬─────────┘◄ - - - - - - - - - - - - - ┼── OK          │               │
│                  │           │                                      │               │               │
│                  │           ▼                                      │               │               │
│                  │  ┌──────────────────┐                            │               │               │
│                  │  │ Step 3: Insert   │- - - - - - - - - - - - - - ┼─► INSERT      │               │
│                  │  │ Reduced Offence  │                            │   ROA         │               │
│                  │  │ Amount           │◄ - - - - - - - - - - - - - ┼── OK          │               │
│                  │  └────────┬─────────┘                            │               │               │
│                  │           │                                      │               │               │
│                  │           ▼                                      │               │               │
│                  │  ┌──────────────────┐                            │               │               │
│                  │  │ Step 4: Update   │- - - - - - - - - - - - - - ┼ - - - - - - - ┼─► UPDATE      │
│                  │  │ Internet eVON    │                            │               │   eVON        │
│                  │  │ (4 fields)       │◄ - - - - - - - - - - - - - ┼ - - - - - - - ┼── OK          │
│                  │  └────────┬─────────┘                            │               │               │
│                  │           │                                      │               │               │
│                  │           ▼                                      │               │               │
│                  │  ◆ All updates successful?                       │               │               │
│                  │  │                                               │               │               │
│                  │  ├── No ──► ROLLBACK ──► Return "Reduction fail" ──► End         │               │
│                  │  │                                               │               │               │
│                  │  └── Yes                                         │               │               │
│                  │           │                                      │               │               │
│                  │           ▼                                      │               │               │
│                  │  ┌──────────────────────────────────────────┐    │               │               │
│                  │  │ COMMIT TRANSACTION                       │    │               │               │
│                  │  └──────────────────────────────────────────┘    │               │               │
│                  │           │                                      │               │               │
│                  │           ▼                                      │               │               │
│ ◄────────────────┼── Return "Reduction Success"                     │               │               │
│                  │           │                                      │               │               │
│   ┌─────────┐    │           ▼                                      │               │               │
│   │   End   │    │     ┌───────────┐                                │               │               │
│   └─────────┘    │     │    End    │                                │               │               │
│                  │     └───────────┘                                │               │               │
│                  │                                                  │               │               │
└──────────────────┴──────────────────────────────────────────────────┴───────────────┴───────────────┘
```

### 3.3 Detailed Steps Table

| Step | Swimlane | Type | Definition | Brief Description | Next Step |
| --- | --- | --- | --- | --- | --- |
| 1 | PLUS | Start | Entry point | PLUS sends POST /v1/plus-apply-reduction | 2 |
| 2 | Backend | Process | Receive Request | OCMS Backend receives the reduction request | 3 |
| 3 | Backend | Decision | Data format correct? | Check if JSON structure and data types are valid | Yes→4, No→E1 |
| 4 | Backend | Decision | Mandatory data present? | Check all required fields are provided | Yes→5, No→E2 |
| 5 | Backend | Process | Query Notice | SELECT notice_no, crs_reason_of_suspension, computer_rule_code, last_processing_stage, composition_amount, suspension_type, epr_reason_of_suspension, amount_payable FROM ocms_valid_offence_notice WHERE notice_no = ? | 6 |
| 6 | Backend | Decision | Notice exists? | Check if notice was found in database | Yes→7, No→E3 |
| 7 | Backend | Decision | Already reduced (TS-RED)? | Check suspension_type="TS" AND epr_reason="RED" | Yes→S1, No→8 |
| 8 | Backend | Decision | Notice paid? | Check if CRS reason = "FP" or "PRA" | Yes→E4, No→9 |
| 9 | Backend | Decision | Rule Code eligible? | Check if code IN (30305, 31302, 30302, 21300) | Yes→10, No→11 |
| 10 | Backend | Decision | Stage in extended list? | Check if stage IN (NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3) | Yes→12, No→E5 |
| 11 | Backend | Decision | Stage = RR3 or DR3? | Special case for non-eligible codes | Yes→12, No→E5 |
| 12 | Backend | Process | Validate Amounts/Dates | Check amount consistency and date logic | 13 |
| 13 | Backend | Decision | Amounts/Dates valid? | Verify business rules for amounts and dates | Yes→14, No→E1 |
| 14 | Backend | Process | BEGIN TRANSACTION | Start database transaction | 15 |
| 15 | Backend/DB | Process | Update VON | UPDATE ocms_valid_offence_notice SET suspension_type='TS', epr_reason_of_suspension='RED', amount_payable=?, epr_date_of_suspension=?, due_date_of_revival=? | 16 |
| 16 | Backend/DB | Process | Insert SN | INSERT INTO ocms_suspended_notice (...) | 17 |
| 17 | Backend/DB | Process | Insert ROA | INSERT INTO ocms_reduced_offence_amount (...) | 18 |
| 18 | Backend/Internet DB | Process | Update eVON | UPDATE eocms_valid_offence_notice SET 4 fields | 19 |
| 19 | Backend | Decision | All updates successful? | Check if all DB operations succeeded | Yes→20, No→E6 |
| 20 | Backend | Process | COMMIT | Commit transaction | 21 |
| 21 | Backend | Process | Return Success | Response: "Reduction Success" | End |
| S1 | Backend | Process | Return Success (Idempotent) | Response: "Reduction Success" (no changes) | End |
| E1 | Backend | Error | Invalid Format | Response: "Invalid format" | End |
| E2 | Backend | Error | Missing Data | Response: "Missing data" | End |
| E3 | Backend | Error | Not Found | Response: "Notice not found" | End |
| E4 | Backend | Error | Paid | Response: "Notice has been paid" | End |
| E5 | Backend | Error | Not Eligible | Response: "Notice is not eligible" | End |
| E6 | Backend | Error | Reduction Fail | ROLLBACK, Response: "Reduction fail" | End |

### 3.4 Decision Logic

| ID | Decision | Input | Condition | True Action | False Action |
| --- | --- | --- | --- | --- | --- |
| D1 | Data format correct? | Request body | JSON valid AND types correct | Continue | Return "Invalid format" |
| D2 | Mandatory data present? | All required fields | All NOT NULL/BLANK | Continue | Return "Missing data" |
| D3 | Notice exists? | Query result | Record found | Continue | Return "Notice not found" |
| D4 | Already reduced? | VON record | suspension_type='TS' AND epr_reason='RED' | Return Success (idempotent) | Continue |
| D5 | Notice paid? | VON.crs_reason | = 'FP' OR = 'PRA' | Return "Paid" | Continue |
| D6 | Rule Code eligible? | VON.computer_rule_code | IN (30305, 31302, 30302, 21300) | Check D7 | Check D8 |
| D7 | Stage in extended list? | VON.last_processing_stage | IN (NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3) | Continue to update | Return "Not eligible" |
| D8 | Stage = RR3/DR3? | VON.last_processing_stage | IN (RR3, DR3) | Continue to update | Return "Not eligible" |
| D9 | Amounts/Dates valid? | Request + VON | Business rules pass | Begin transaction | Return "Invalid format" |
| D10 | All updates successful? | Transaction result | All operations OK | Commit | Rollback |

### 3.5 Database Operations

| Step | Operation | Table | Zone | Fields | Notes |
| --- | --- | --- | --- | --- | --- |
| 5 | SELECT | ocms_valid_offence_notice | Intranet | notice_no, crs_reason_of_suspension, computer_rule_code, last_processing_stage, composition_amount, suspension_type, epr_reason_of_suspension, amount_payable, epr_date_of_suspension, due_date_of_revival | Load notice for validation (never use SELECT *) |
| 15 | UPDATE | ocms_valid_offence_notice | Intranet | suspension_type='TS', epr_reason_of_suspension='RED', amount_payable, epr_date_of_suspension, due_date_of_revival | Apply reduction |
| 16 | INSERT | ocms_suspended_notice | Intranet | notice_no, date_of_suspension, sr_no, suspension_source, suspension_type, reason_of_suspension, officer_authorising_suspension, due_date_of_revival, suspension_remarks | Record suspension |
| 17 | INSERT | ocms_reduced_offence_amount | Intranet | notice_no, date_of_reduction, sr_no, amount_reduced, amount_payable, reason_of_reduction, expiry_date, officer_authorising_reduction, remarks | Log reduction |
| 18 | UPDATE | eocms_valid_offence_notice | Internet | suspension_type='TS', epr_reason_of_suspension='RED', epr_date_of_suspension, amount_payable | Mirror to internet (4 fields only) |

**Note (Code Verified):** Internet DB only updates `eocms_valid_offence_notice`. There is NO insert to `eocms_suspended_notice` - the FD documentation mentioning internet suspended_notice is incorrect.

### 3.6 Error Handling

| Error Point | Error Type | HTTP Status | Response Message | Handling |
| --- | --- | --- | --- | --- |
| D1 | Validation | 400 | Invalid format | Return error, no DB changes |
| D2 | Validation | 400 | Missing data | Return error, no DB changes |
| D3 | Business | 404 | Notice not found | Return error, no DB changes |
| D5 | Business | 409 | Notice has been paid | Return error, no DB changes |
| D7/D8 | Business | 409 | Notice is not eligible | Return error, no DB changes |
| D9 | Validation | 400 | Invalid format | Return error, no DB changes |
| D10 | Technical | 500 | Reduction fail | Rollback transaction, return error |

### 3.7 API Payload Box (for Flowchart)

**Request Payload:**
```
POST /v1/plus-apply-reduction

{
  "noticeNo": "500500303J",
  "amountReduced": 55.00,
  "amountPayable": 15.00,
  "dateOfReduction": "2025-08-01T19:00:02",
  "expiryDateOfReduction": "2025-08-15T00:00:00",
  "reasonOfReduction": "RED",
  "authorisedOfficer": "JOHNLEE",
  "suspensionSource": "005",
  "remarks": "Reduction granted"
}
```

**Success Response:**
```
HTTP 200 OK

{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Reduction Success"
  }
}
```

**Error Response Examples:**
```
HTTP 400 Bad Request
{ "data": { "appCode": "OCMS-4000", "message": "Invalid format" } }

HTTP 400 Bad Request
{ "data": { "appCode": "OCMS-4000", "message": "Missing data" } }

HTTP 404 Not Found
{ "data": { "appCode": "OCMS-4004", "message": "Notice not found" } }

HTTP 409 Conflict
{ "data": { "appCode": "OCMS-4090", "message": "Notice has been paid" } }

HTTP 409 Conflict
{ "data": { "appCode": "OCMS-4091", "message": "Notice is not eligible" } }

HTTP 500 Internal Server Error
{ "data": { "appCode": "OCMS-5000", "message": "Reduction fail" } }
```

---

## 4. Flowchart Checklist

Before creating the .drawio flowchart:

- [x] All steps have clear names
- [x] All decision points have Yes/No paths defined
- [x] All paths lead to an End point
- [x] Error handling paths are included
- [x] Database operations are marked (dashed lines in .drawio)
- [x] Swimlanes are defined for each system/tier
- [x] Color coding is defined
- [x] Step descriptions are complete
- [x] API payload examples included
- [x] Eligibility logic documented (Computer Rule Code + Processing Stage)
- [x] Transaction boundaries defined (BEGIN/COMMIT/ROLLBACK)

---

## 5. Reference Information

### 5.1 Eligible Computer Rule Codes

| Code | Can reduce at stages |
| --- | --- |
| 30305 | NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3 |
| 31302 | NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3 |
| 30302 | NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3 |
| 21300 | NPA, ROV, ENA, RD1, RD2, RR3, DN1, DN2, DR3 |
| Others | RR3, DR3 only |

### 5.2 Processing Stage Descriptions

| Stage | Description |
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

### 5.3 CRS Reason Values for Payment Status

| Value | Meaning | Can Reduce? |
| --- | --- | --- |
| NULL/BLANK | Outstanding | Yes |
| FP | Full Payment | No |
| PRA | Paid (PRA) | No |

---

## 6. Changelog

| Version | Date | Author | Changes |
| --- | --- | --- | --- |
| 1.1 | 22/01/2026 | Claude | Code verification update: Added MVP1 scope note. Confirmed transaction flow matches code (ReductionPersistenceService.java). Note: Internet DB only updates eVON (no eSN insert). |
| 1.0 | 18/01/2026 | Claude | Initial version based on plan_api.md, plan_condition.md, and Functional Flowchart |

---

## 7. Code Verification Notes

**Verified from Codebase (22/01/2026):**

| Step | Code File | Status |
| --- | --- | --- |
| API Endpoint | `ReductionController.java:27` | ✅ POST /v1/plus-apply-reduction |
| Validation Flow | `ReductionValidator.java`, `ReductionRuleService.java` | ✅ Matches flowchart |
| Transaction Steps | `ReductionPersistenceService.java:54` | ✅ @Transactional with rollback |
| Internet DB Update | `ReductionPersistenceService.java` | ⚠️ Only eVON updated, NOT eSN inserted |
| Idempotency | `ReductionPersistenceService.java:213-233` | ✅ TS-RED check before processing |

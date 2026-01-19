# Condition Plan: OCMS CAS & PLUS Integration

This document outlines the business conditions and logic inferred from the integration diagram, following the `docs/templates/plan_condition.md` template.

---

## 1. Business Conditions

### API 1: `getPaymentStatus`
*   **Condition ID**: C001
*   **Condition Name**: Process FP and EMCONS notices only.
*   **Logic**: When CAS sends a list of notices, OCMS should filter or handle them based on whether they are "FP" or "EMCONS" notices. The exact logic for identifying these types needs to be defined.
*   **Action**: For matching notices, set the `emcons_flag` and potentially create a new record in the `ocms_cas_emcons` table.

### API 2: `updateCourtNotices`
*   **Condition ID**: C002
*   **Condition Name**: Day-end flag for court notices.
*   **Logic**: The "Update: Flag" is specified to be "only for court notices only based on dayend". This implies a time-based condition where updates are only processed after a certain time or when a day-end batch process is running.
*   **Action**: Apply the updates provided in the request payload.

### API 4: `getNoticeUpdate`
*   **Condition ID**: C003
*   **Condition Name**: PS-WWP Suspension/EPR Reason.
*   **Logic**: The response fields `Suspension type (TS/PS)` and `EPR Reason of suspension` are specified to be "use for PS-WWP only". This means the logic must check the notice type or status to determine if it's a "PS-WWP" case before populating these fields.
*   **Action**: Populate the relevant response fields only if the condition is met.

## 2. Validation Rules (Proposed)

Even though not detailed in the image, the following validations should be planned for each API:

| Rule ID | API(s) | Validation Rule | Error Message |
| --- | --- | --- | --- |
| V001 | 1, 2, 4 | Notice numbers must be valid and exist in the system. | "Invalid or non-existent notice number(s) provided." |
| V002 | 1, 2, 4 | Request payload must not be empty. | "Request body cannot be empty." |
| V003 | All | Standard authentication and authorization checks. | "Unauthorized." |

## 3. Status Transitions (Inferred)

The diagram mentions several status-related fields, implying that status transition logic must be documented.

| Entity | Status Fields | Notes |
| --- | --- | --- |
| **Notice** | `Payment status`, `Suspension type`, `CRS/EPR Reason of suspension`, `ATOMS flag` | The technical document must detail how these fields are updated and what state transitions they represent. For example, a `Payment status` changing from "Unpaid" to "Paid" is a key transition. |
| **VIP Vehicle** | `status` | The possible values and meaning of the `status` field for VIP vehicles must be defined. |

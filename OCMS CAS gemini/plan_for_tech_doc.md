# Technical Documentation Plan: OCMS CAS & PLUS Integration

This document outlines the plan for creating the technical documentation based on the provided integration diagram.

## 1. Document Structure (based on 00-master-template.md)

The technical document will be named **OCMS_CAS_PLUS_Integration_Technical_Document.md**.

### Section 1: OCMS-CAS Integration

*   **Use Case**: To integrate OCMS with the Court Admin System (CAS) for exchanging notice statuses, payment details, and court-related information. This involves both OCMS providing data to CAS and CAS pushing updates back to OCMS.
*   **Flow 1.1**: Get Payment Status (OCMS for CAS)
*   **Flow 1.2**: Update Court Notices (CAS to OCMS)
*   **Flow 1.3**: Refresh VIP Vehicle Table (CAS to OCMS)

### Section 2: OCMS-PLUS Integration

*   **Use Case**: To provide an ad-hoc mechanism for the PLUS system to retrieve updated notice information, including court payment due dates.
*   **Flow 2.1**: Ad-hoc Notice Query (OCMS for PLUS)

### Section 3: New Database Tables

*   **Use Case**: To store new data entities required for the CAS and PLUS integrations.
*   **3.1 Table**: `ocms_vip_vehicle`
*   **3.2 Table**: `ocms_cas_emcons`
*   **3.3 Table**: `ocms_court_case`

## 2. Next Steps

1.  **Create Technical Draw.io**: Develop a multi-page Draw.io file as outlined in `plan_flowchart.md`.
2.  **Draft Technical Document**: Write the `.md` document following this plan, using the `00-master-template.md` and filling in details from `plan_api.md` and `plan_condition.md`.
3.  **Review**: The draft will be reviewed against the `10-yijie-reviewer-checklist.md`.

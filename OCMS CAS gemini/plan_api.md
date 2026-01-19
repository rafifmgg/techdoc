# API Plan: OCMS CAS & PLUS Integration

This plan details the specifications for the 4 APIs identified in the integration diagram, following the `07-section-api-specification.md` template.

---

### 1. API 1: Get Payment Status (OCMS for CAS)

| Field | Value |
| :--- | :--- |
| **API Name** | `getPaymentStatus` |
| **URL** | `/v1/getPaymentStatus` |
| **Description** | Called by CAS to get updated status for outstanding NOPO or EMCON notices. |
| **Method** | `POST` |
| **Request** | `{ "noticeNos": ["N001", "N002"] }` |
| **Response** | A list of objects, each containing: `Notice No`, `Last Processing stage (for emcons/dnata)`, `Last Processing date (for emcons/dnata)`, `Next Processing stage (for emcons/dnata)`, `Last Processing date (for emcons/dnata)`, `emcons_flag`, `Suspension type (PS, TS)`, `CRS Reason of suspension (FP, PP,)`, `EPR Reason of suspension`, `Payment status`, `Amount payable`, `Amount Paid`, `Payment Allowance Flag`.|
| **Frequency** | Once a day (TBC) |

---

### 2. API 2: Update Court Notices (CAS to OCMS)

| Field | Value |
| :--- | :--- |
| **API Name** | `updateCourtNotices` |
| **URL** | `/v1/updateCourtNotices` |
| **Description** | Called by CAS to update OCMS court NOPO records after API 1. |
| **Method** | `POST` |
| **Request** | A list of objects, each containing the fields listed in the "Update" section of the diagram (e.g., `Suspension type`, `Notice No`, `ATOMS flag`, etc.). |
| **Response** | Standard success/failure response. |
| **Frequency** | Once a day (TBC) |

---

### 3. API 3: Refresh VIP Vehicle Table (CAS for OCMS)

| Field | Value |
| :--- | :--- |
| **API Name** | `getVipVehicles` (Proposed Name) |
| **URL** | (To be provided by CAS) |
| **Description** | Called by OCMS to refresh the full `ocms_vip_vehicle` table daily. |
| **Method** | `POST` |
| **Request** | (Likely an empty body to request all data) |
| **Response** | A list of objects, each containing: `vip_vehicle`, `vehicle_no`, `description`, `status`.|
| **Frequency** | Once a day |

---

### 4. API 4: Ad-hoc Notice Query (OCMS for PLUS)

| Field | Value |
| :--- | :--- |
| **API Name** | `getNoticeUpdate` (Proposed Name) |
| **URL** | (To be exposed via IZ-APIM) |
| **Description** | Called by PLUS on an ad-hoc basis to get updated details for a list of notices. |
| **Method** | `POST` |
| **Request** | `{ "noticeNos": ["N003", "N004"] }` |
| **Response** | A list of objects, each containing: `Notice No`, `Amount payable`, `Payment Allowance Flag`, `Court_payment_due_date`, `Suspension type (TS/PS)`, `EPR Reason of suspension`. |
| **Frequency** | Adhoc |

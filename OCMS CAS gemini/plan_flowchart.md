# Flowchart Plan: OCMS CAS & PLUS Integration

This document outlines the required flowcharts for the technical documentation, based on `docs/guidelines/plan_flowchart.md`.

## 1. Diagram Pages

A single Draw.io file, **OCMS_CAS_PLUS_Integration.drawio**, will be created with the following pages:

| Page Name | Description | Systems Involved |
| --- | --- | --- |
| **High_Level_Overview** | A top-level diagram showing all systems and the four main API interactions. | OCMS Intranet, CAS, PLUS, IZ-APIM, Payment eService |
| **API_1_Get_Payment_Status** | Detailed flow for CAS calling OCMS to get notice statuses. | OCMS Intranet, CAS |
| **API_2_Update_Court_Notices**| Detailed flow for CAS calling OCMS to update court NOPO. | OCMS Intranet, CAS |
| **API_3_Refresh_VIP_Vehicle**| Detailed flow for OCMS calling CAS to refresh the VIP vehicle list. | OCMS Intranet, CAS |
| **API_4_Adhoc_Notice_Query** | Detailed flow for PLUS calling OCMS via IZ-APIM to get notice details. | OCMS Intranet, IZ-APIM, PLUS |

## 2. Process Steps (High-Level)

### API 1: Get Payment Status
1.  **Start**: CAS initiates a scheduled job (Once a day).
2.  **Process**: CAS prepares a list of unpaid court notice numbers.
3.  **Process**: CAS calls OCMS Intranet API 1 (`v1/getPaymentStatus`).
4.  **Process**: OCMS processes the request, checking for FP and EMCONS notices.
5.  **Process**: OCMS creates a record in the new `ocms_cas_emcons` table if applicable.
6.  **Process**: OCMS returns the response with notice details.
7.  **End**.

### API 2: Update Court Notices
1.  **Start**: CAS initiates a scheduled job after API 1 completes.
2.  **Process**: CAS prepares a list of court notices to update.
3.  **Process**: CAS calls OCMS Intranet API 2 (`v1/updateCourtNotices`).
4.  **Process**: OCMS receives the request and updates its notice records based on the day-end flag.
5.  **Process**: OCMS returns a success/failure response.
6.  **End**.

### API 3: Refresh VIP Vehicle Table
1.  **Start**: OCMS Intranet initiates a scheduled job (Once a day).
2.  **Process**: OCMS calls the CAS API 3 to get the full VIP vehicle list.
3.  **Process**: OCMS truncates and refreshes the `ocms_vip_vehicle` table with the response data.
4.  **End**.

### API 4: Adhoc Notice Query
1.  **Start**: PLUS system initiates an ad-hoc request.
2.  **Process**: PLUS calls OCMS Intranet API 4 via the IZ-APIM with a list of notice numbers.
3.  **Process**: OCMS retrieves the requested notice details.
4.  **Process**: OCMS creates a record in the new `ocms_court_case` table if applicable.
5.  **Process**: OCMS returns the response data to PLUS.
6.  **End**.

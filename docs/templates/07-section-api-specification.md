# API Specification Template

<!--
Template for: API Specification Section
Use this template when documenting APIs in technical documentation.
Based on OCMS 50 approved document format.

API Types:
- API for eService: APIs provided by backend for frontend (eService, Staff Portal)
- API Provide: APIs provided to external systems (AXS, PLUS, etc.)
- API Consume: APIs consumed from external systems (URA PG, SPCP, etc.)
-->

---

## API Specification

### API for eService

<!--
Use this section for APIs that the backend provides to the frontend (eService, Staff Portal).
-->

#### API [API Name]

| Field | Value |
| --- | --- |
| API Name | [API name, e.g., parkingfines] |
| URL | UAT: https://[uat-domain]/ocms/v1/[endpoint] <br> PRD: https://[prd-domain]/ocms/v1/[endpoint] |
| Description | [Brief description of what the API does] |
| Method | [GET/POST/PUT/PATCH/DELETE] |
| Header | `{ "Authorization": "Bearer [token]", "Content-Type": "application/json" }` |
| Payload | `{ "field1": "value1", "field2": "value2" }` |
| Response | `{ "appCode": "OCMS-2000", "message": "Success", "data": { ... } }` |
| Response (Empty) | `{ "appCode": "OCMS-2000", "message": "Success", "data": [] }` |
| Response Failure | `{ "appCode": "OCMS-5000", "message": "Error message" }` |

<!--
IMPORTANT - Response Format:
- Use `appCode` and `message`, NOT `status`
- For batch operations: use `totalProcessed`, `successCount`, `errorCount`, `results[]`
- For single/list queries: use `appCode`, `message`, `data`
- Empty result returns empty array `[]` in data field (not error)
-->

---

### API Provide

<!--
Use this section for APIs that the system provides to external systems (AXS, PLUS, etc.).
-->

#### API [API Name]

| Field | Value |
| --- | --- |
| API Name | [API name] |
| URL | UAT: https://[uat-domain]/ocms/[service]/v1/[endpoint] <br> PRD: https://[prd-domain]/ocms/[service]/v1/[endpoint] |
| Description | [Brief description] |
| Method | [GET/POST] |
| Header | `{ "Content-Type": "application/json", "Ocp-Apim-Subscription-Key": "[APIM secret value]" }` |
| Payload | `{ "sender": "[SENDER]", "targetReceiver": "URA", ... }` |
| Response | `{ "status": "0", "data": [ ... ] }` |
| Response Failure | `{ "status": "1", "errorCode": "[code]", "errorMsg": "[message]" }` |

---

### API Consume

<!--
Use this section for APIs that the system consumes from external systems (URA PG, SPCP, LTA, etc.).
-->

#### API [External API Name]

| Field | Value |
| --- | --- |
| API Name | [External API name] |
| URL | UAT: https://[external-uat-domain]/api/v1/[endpoint] <br> PRD: https://[external-prd-domain]/api/v1/[endpoint] |
| Description | [Brief description] |
| Method | [GET/POST] |
| Header | `{ "Content-Type": "application/json", "Authorization": "[auth method]" }` |
| Payload | `{ ... }` |
| Response | `{ ... }` |
| Response Failure | `{ ... }` |

---

## Data Mapping

### Request/Response Data Mapping

<!--
Use this format for mapping API response fields to database fields.
-->

| Response Field | Description | Source |
| --- | --- | --- |
| [fieldName] | [Description of field] | [table.field_name] |
| [fieldName] | [Description of field] | [table.field_name] |

### UI Data Mapping

<!--
Use this format for mapping database fields to UI fields.
-->

| Zone | Database Table | Field Name | UI Field | Description |
| --- | --- | --- | --- | --- |
| [Zone] | [table_name] | [field_name] | [UI Label] | [Description] |
| [Zone] | [table_name] | [field_name] | [UI Label] | [Description] |

### Database Data Mapping

<!--
Use this format for simple database field mapping (no UI).
-->

| Zone | Database Table | Field Name |
| --- | --- | --- |
| [Zone] | [table_name] | [field_name] |
| [Zone] | [table_name] | [field_name] |

---

## Error Handling

### Application Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| [Error Type] | [When this error occurs] | [What happens] |
| [Error Type] | [When this error occurs] | [What happens] |

### API Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| General Server Error | OCMS-5000 | Something went wrong on our end. Please try again later. | Server encountered an unexpected condition |
| Bad Request | OCMS-4000 | The request could not be processed due to a syntax error. | Invalid request syntax |
| Unauthorized Access | OCMS-4001 | You are not authorized to access this resource. | Authentication failed or not provided |
| [Custom Error] | [OCMS-XXXX] | [User-friendly message] | [Technical description] |

---

## Quick Reference

### Standard API Error Codes

| Error Code | Category | Description |
| --- | --- | --- |
| OCMS-4000 | Client Error | Bad Request - Invalid syntax |
| OCMS-4001 | Client Error | Unauthorized - Authentication required |
| OCMS-4003 | Client Error | Forbidden - No permission |
| OCMS-4004 | Client Error | Not Found - Resource not found |
| OCMS-4022 | Client Error | Validation Error - Invalid data |
| OCMS-5000 | Server Error | Internal Server Error |
| OCMS-5001 | Server Error | Service Unavailable |
| OCMS-5002 | Server Error | Gateway Timeout |

### HTTP Methods

| Method | Use Case |
| --- | --- |
| GET | Retrieve data |
| POST | Create new resource / Submit data |
| PUT | Replace entire resource |
| PATCH | Partial update |
| DELETE | Remove resource |

### Common Headers

```json
{
  "Content-Type": "application/json",
  "Authorization": "Bearer [JWT token]",
  "Ocp-Apim-Subscription-Key": "[APIM subscription key]"
}
```

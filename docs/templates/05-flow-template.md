# [Flow Name]

<!--
Template for: Individual Process Flow
Use this template when documenting a single process flow within a section.
Structure matches OCMS 50 approved document format.
-->

<!-- Insert flow diagram here -->
![Flow Diagram](./images/flow-name-diagram.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| [Step Name] | [Detailed description of what happens] | [Short summary] |
| [Step Name] | [Detailed description of what happens] | [Short summary] |
| [Step Name] | [Detailed description of what happens] | [Short summary] |
| [Step Name] | [Detailed description of what happens] | [Short summary] |

---

## API Specification

<!--
Include this section if the flow involves API calls.
Use appropriate sub-section based on API type:
- API for eService: APIs provided by backend for frontend
- API Provide: APIs provided to external systems
- API Consume: APIs consumed from external systems
-->

### API for eService

#### API [API Name]

| Field | Value |
| --- | --- |
| API Name | [api_name] |
| URL | UAT: https://[domain]/ocms/v1/[endpoint] <br> PRD: https://[domain]/ocms/v1/[endpoint] |
| Description | [API description] |
| Method | [GET/POST] |
| Header | `{ "Authorization": "Bearer [token]", "Content-Type": "application/json" }` |
| Payload | `{ "field": "value" }` |
| Response | `{ "appCode": "OCMS-2000", "message": "Success", "data": { } }` |
| Response (Empty) | `{ "appCode": "OCMS-2000", "message": "Success", "data": [] }` |
| Response Failure | `{ "appCode": "OCMS-5000", "message": "Error message" }` |

---

## Data Mapping

### Database Data Mapping

| Zone | Database Table | Field Name |
| --- | --- | --- |
| [Source Zone] | [table_name] | [field_name] |
| [Target Zone] | [table_name] | [field_name] |

### UI Data Mapping

<!--
Include this section if UI fields need to be mapped.
-->

| Zone | Database Table | Field Name | UI Field | Description |
| --- | --- | --- | --- | --- |
| [Zone] | [table_name] | [field_name] | [UI Label] | [Description] |

---

## Success Outcome

- [Success condition 1]
- [Success condition 2]
- [Success condition 3]
- The workflow reaches the End state without triggering any error-handling paths.

---

## Error Handling

### Application Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| [Error Type 1] | [When this error occurs] | [What happens - action taken] |
| [Error Type 2] | [When this error occurs] | [What happens - action taken] |

### API Error Handling

<!--
Include this section if the flow involves API calls.
-->

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| General Server Error | OCMS-5000 | Something went wrong. Please try again later. | Server encountered an unexpected condition |
| Bad Request | OCMS-4000 | Invalid request. Please check and try again. | Invalid request syntax |
| Unauthorized Access | OCMS-4001 | You are not authorized. Please log in and try again. | Authentication failed |
| [Custom Error] | [OCMS-XXXX] | [User message] | [Technical description] |

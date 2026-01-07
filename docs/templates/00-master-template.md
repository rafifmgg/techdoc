# OCMS [NUMBER] – [FEATURE NAME]

**Prepared by**

[COMPANY LOGO]

[COMPANY NAME]

---

<!--
IMPORTANT: If information already exists in the Functional Document (FD),
refer to FD instead of duplicating content.

Example:
- "Refer to FD Section 2.3 for detailed validation rules"
- "See FD Appendix A for complete field list"
- "Error codes are documented in FD Section 4.5"
-->

---

## Version History

| Version | Updated By | Date | Changes |
| --- | --- | --- | --- |
| v1.0 | [Author Name] | [DD/MM/YYYY] | Document Initiation |
| v1.1 | [Author Name] | [DD/MM/YYYY] | [Description of changes] |

---

## Table of Content

| Section | Content | Pages |
| --- | --- | --- |
| 1 | [Section 1 Title] | [X] |
| 1.1 | Use Case | [X] |
| 1.2 | [Flow 1 Name] | [X] |
| 1.2.1 | Data Mapping | [X] |
| 1.2.2 | Success Outcome | [X] |
| 1.2.3 | Error Handling | [X] |
| 1.3 | [Flow 2 Name] | [X] |
| 1.3.1 | Data Mapping | [X] |
| 1.3.2 | Success Outcome | [X] |
| 1.3.3 | Error Handling | [X] |
| 1.4 | Scenario [Scenario Name] | [X] |
| 1.4.1 | [Sub-scenario 1] | [X] |
| 1.4.2 | [Sub-scenario 2] | [X] |
| 2 | [Section 2 Title] | [X] |
| 2.1 | Use Case | [X] |
| 2.2 | High Level Flow | [X] |
| 2.3 | [Flow Name] | [X] |
| 2.3.1 | Data Mapping | [X] |
| 2.3.2 | Success Outcome | [X] |
| 2.3.3 | Error Handling | [X] |

---

# Section 1 – [Section Title]

## Use Case

<!--
FORMAT: Use numbered list (1, 2, 3) for main points.
For sub-items, use a, b, c with <br> tag.

Example:
1. Main point one.

2. Main point two with sub-items:<br>a. Sub-item first<br>b. Sub-item second<br>c. Sub-item third

3. Main point three.
-->

1. [Copy paste main point 1 from Functional Document]

2. [Main point 2 with sub-items if any]:<br>a. [Sub-item a]<br>b. [Sub-item b]

3. [Main point 3]

## [Flow 1 Name]

<!-- Insert flow diagram here -->
![Flow Diagram](./images/section1-flow1-diagram.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| [Step Name] | [Detailed description of what happens] | [Short summary] |
| [Step Name] | [Detailed description of what happens] | [Short summary] |
| [Step Name] | [Detailed description of what happens] | [Short summary] |

### API Specification

#### API for eService

##### API [API Name]

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

### Data Mapping

| Zone | Database Table | Field Name |
| --- | --- | --- |
| [Source Zone] | [table_name] | [field_name] |
| [Target Zone] | [table_name] | [field_name] |

### Success Outcome

- [Success condition 1]
- [Success condition 2]
- [Success condition 3]

### Error Handling

#### Application Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| [Error Type 1] | [When this error occurs] | [What happens] |
| [Error Type 2] | [When this error occurs] | [What happens] |

#### API Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| General Server Error | OCMS-5000 | Something went wrong. Please try again later. | Server error |
| Bad Request | OCMS-4000 | Invalid request. Please check and try again. | Invalid syntax |

## [Flow 2 Name]

<!-- Insert flow diagram here -->
![Flow Diagram](./images/section1-flow2-diagram.png)

| Step | Description | Brief Description |
| --- | --- | --- |
| [Step Name] | [Detailed description of what happens] | [Short summary] |
| [Step Name] | [Detailed description of what happens] | [Short summary] |

### Data Mapping

| Zone | Database Table | Field Name |
| --- | --- | --- |
| [Source Zone] | [table_name] | [field_name] |
| [Target Zone] | [table_name] | [field_name] |

### Success Outcome

- [Success condition 1]
- [Success condition 2]

### Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| [Error Type 1] | [When this error occurs] | [What happens] |

## Scenario [Scenario Name]

### [Sub-scenario 1 Name]

<!-- Insert scenario flow diagram here -->
![Scenario Flow](./images/section1-scenario1-diagram.png)

| Step | Action | Description |
| --- | --- | --- |
| 1 | [Action] | [Description] |
| 2 | [Action] | [Description] |
| 3 | [Action] | [Description] |

### [Sub-scenario 2 Name]

<!-- Insert scenario flow diagram here -->
![Scenario Flow](./images/section1-scenario2-diagram.png)

| Step | Action | Description |
| --- | --- | --- |
| 1 | [Action] | [Description] |
| 2 | [Action] | [Description] |

---

# Section 2 – [Section Title]

## Use Case

1. [Copy paste main point 1 from Functional Document]

2. [Main point 2 with sub-items if any]:<br>a. [Sub-item a]<br>b. [Sub-item b]

3. [Main point 3]

## High Level Flow

<!-- Insert high level flow diagram here -->
![High Level Flow](./images/section2-highlevel-diagram.png)

## [Flow Name]

<!-- Insert detailed flow diagram here -->
![Flow Diagram](./images/section2-flow-diagram.png)

| Step | Description | Brief Description |
| --- | --- | --- |
| [Step Name] | [Detailed description of what happens] | [Short summary] |
| [Step Name] | [Detailed description of what happens] | [Short summary] |
| [Step Name] | [Detailed description of what happens] | [Short summary] |

### Data Mapping

| Zone | Database Table | Field Name |
| --- | --- | --- |
| [Source Zone] | [table_name] | [field_name] |
| [Target Zone] | [table_name] | [field_name] |

### Success Outcome

- [Success condition 1]
- [Success condition 2]

### Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| [Error Type 1] | [When this error occurs] | [What happens] |

---

# Section 3 – [Section Title]

## Use Case

1. [Copy paste main point 1 from Functional Document]

2. [Main point 2 with sub-items if any]:<br>a. [Sub-item a]<br>b. [Sub-item b]

3. [Main point 3]

## Diagram Flow Image

<!-- Insert flow diagram here -->
![Flow Diagram](./images/section3-flow-diagram.png)

| Step | Description | Brief Description |
| --- | --- | --- |
| [Step Name] | [Detailed description of what happens] | [Short summary] |
| [Step Name] | [Detailed description of what happens] | [Short summary] |
| [Step Name] | [Detailed description of what happens] | [Short summary] |

### Data Mapping

| Zone | Database Table | Field Name |
| --- | --- | --- |
| [Source Zone] | [table_name] | [field_name] |
| [Target Zone] | [table_name] | [field_name] |

### Success Outcome

- [Success condition 1]
- [Success condition 2]

### Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| [Error Type 1] | [When this error occurs] | [What happens] |

# Technical Documentation Templates

This folder contains templates for creating consistent technical documentation based on the OCMS documentation standards. Templates are aligned with **OCMS 43 approved document format**.

## Template Overview

| Template | Filename | Use Case |
| --- | --- | --- |
| Master Template | `00-master-template.md` | Complete document structure with multiple flows per section |
| Data Sync Section | `01-section-data-sync.md` | Synchronization between systems/zones |
| Status Update Section | `02-section-status-update.md` | Status update flows (e.g., payment status) |
| Report Generation | `03-section-report-generation.md` | Email reports (daily/weekly exceptions) |
| Export Report | `04-section-export-report.md` | Excel/CSV reports for finance/external |
| Flow Template | `05-flow-template.md` | Individual process flow (Diagram + API + Data Mapping + Success + Error) |
| Scenario Template | `06-scenario-template.md` | Use case scenarios with sub-scenarios |
| **API Specification** | `07-section-api-specification.md` | API documentation (eService, Provide, Consume) |
| **Yi Jie Reviewer** | `10-yijie-reviewer-checklist.md` | Checklist for reviewing tech docs |
| **How to Generate** | `11-how-to-generate-tech-doc.md` | Step-by-step guide from FD + Draw.io + Code |

> **Note:** Yi Jie Flow & Content Guideline moved to `docs/guidelines/09-yijie-flow-content-guideline.md`

---

## How to Use

### 1. Start with Master Template

Copy `00-master-template.md` and rename it to your document name:
```
v1.0 OCMS [NUMBER] Technical Doc.md
```

Fill in:
- Document title
- Version history
- Table of contents

### 2. Structure Each Section

Each section follows this pattern (from OCMS 43):

```
# Section X – [Title from FD]

## Use Case
[Copy paste from Functional Document]

## [Flow 1 Name]
[Diagram]
[Process Flow Table]
### API Specification (if applicable)
### Data Mapping
### Success Outcome
### Error Handling

## [Flow 2 Name]
[Diagram]
[Process Flow Table]
### API Specification (if applicable)
### Data Mapping
### Success Outcome
### Error Handling

## Scenario [Name]
### [Sub-scenario 1]
### [Sub-scenario 2]
```

### 3. Multiple Flows Per Section

A section can have multiple flows. Example from OCMS 43 Section 1:
- Intranet Push to Internet
- Cron Retry Intranet Push to Internet
- Intranet Update
- End Day Sync Check
- Scenario Intranet Push to Internet

### 4. Add High Level Flow (if needed)

Some sections include a High Level Flow before detailed flows:
```
## High Level Flow
[Overview diagram]

## [Detailed Flow Name]
[Detailed diagram + Data Mapping + Success + Error]
```

---

## Standard Section Structure

Based on OCMS 43 approved format:

```
# Section X – [Title]

## Use Case
[Copy paste from FD]

## [Flow Name]
[Flow diagram image]

### Process Flow Table
| Step | Description | Brief Description |
| --- | --- | --- |
| [Step Name] | [What happens] | [Short summary] |

### Data Mapping
| Zone | Database Table | Field Name |
| --- | --- | --- |
| [Zone] | [table] | [field] |

### Success Outcome
- [Condition 1]
- [Condition 2]

### Error Handling
| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| [Error] | [When] | [What happens] |
```

---

## Naming Conventions

### Document Naming
```
v[VERSION] OCMS [NUMBER] Technical Doc.md
```
Example: `v1.6.2 OCMS 43 Technical Doc.md`

### Image Naming
```
[section]-[flow-name]-diagram.png
```
Example: `section1-intranet-push-diagram.png`

---

## Refer to FD (Don't Duplicate)

If information already exists in the Functional Document, **refer to FD** instead of duplicating:

```markdown
<!-- Instead of copying all validation rules -->
Refer to FD Section 2.3 for detailed validation rules.

<!-- Instead of listing all error codes -->
See FD Section 4.5 for complete error code list.

<!-- Instead of copying field mappings -->
Field mappings are documented in FD Appendix A.
```

**When to refer vs copy:**
| Content | Action |
| --- | --- |
| Use Case | Copy from FD |
| Detailed validation rules | Refer to FD |
| Complete field list | Refer to FD |
| Error code definitions | Refer to FD |
| Business rules | Refer to FD |
| Flow diagram | Create new (technical view) |
| Data Mapping | Document in TD |

---

## Checklist Before Finalizing

- [ ] Version history updated
- [ ] Table of contents matches actual sections
- [ ] All diagrams included and referenced
- [ ] All process flow tables complete (Step | Description | Brief Description)
- [ ] All API specifications documented (if applicable)
- [ ] All data mappings complete
- [ ] Success outcomes defined for each flow
- [ ] Error handling documented for each flow (Application + API errors)
- [ ] Use Case copied from FD (not rewritten)
- [ ] References to FD added where applicable (avoid duplication)
- [ ] Validation flows documented (if flowchart has UI/Backend validation)
- [ ] User list API documented (if report/filter needs user dropdown)
- [ ] Response format uses appCode/message (NOT status)
- [ ] Empty response uses empty array `[]` in data field

---

## Quick Reference: Common Tables

### Data Mapping Table
```markdown
| Zone | Database Table | Field Name |
| --- | --- | --- |
| Intranet | [table_name] | [field_name] |
| Internet | [table_name] | [field_name] |
```

### Error Handling Table
```markdown
| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| [Error Type] | [When it occurs] | [What happens] |
```

### Process Flow Table
```markdown
| Step | Description | Brief Description |
| --- | --- | --- |
| [Step Name] | [What happens in this step] | [Short summary] |
| [Step Name] | [What happens in this step] | [Short summary] |
```

### API Specification Table
```markdown
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
```

**IMPORTANT - Response Format:**
- Use `appCode` and `message`, NOT `status`
- For batch operations: use `totalProcessed`, `successCount`, `errorCount`, `results[]`
- For single/list queries: use `appCode`, `message`, `data`
- Empty result returns empty array `[]` in data field (not error)

### API Error Handling Table
```markdown
| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| General Server Error | OCMS-5000 | Something went wrong. | Server error |
| Bad Request | OCMS-4000 | Invalid request. | Invalid syntax |
```

### Scenario Steps Table
```markdown
| Step | Action | Description |
| --- | --- | --- |
| 1 | [Action] | [Description] |
| 2 | [Action] | [Description] |
```

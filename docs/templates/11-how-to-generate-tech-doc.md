# How to Generate Technical Documentation

Step-by-step guide to create tech doc from Functional Document, Functional Draw.io, and existing code.

---

## Overview

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│ 1. Functional   │     │ 2. Functional   │     │ 3. Existing     │
│    Document     │     │    Draw.io      │     │    Code         │
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 ▼
                    ┌─────────────────────────┐
                    │ 4. Create Plan Files    │
                    │    - plan_api.md        │
                    │    - plan_condition.md  │
                    │    - plan_flowchart.md  │
                    └────────────┬────────────┘
                                 ▼
                    ┌─────────────────────────┐
                    │ 5. Create Tech Draw.io  │
                    │    (Multi-section)      │
                    └────────────┬────────────┘
                                 ▼
                    ┌─────────────────────────┐
                    │ 6. Compare with Code    │
                    │    (Verify accuracy)    │
                    └────────────┬────────────┘
                                 ▼
                    ┌─────────────────────────┐
                    │ 7. Generate Tech Doc    │
                    └────────────┬────────────┘
                                 ▼
                    ┌─────────────────────────┐
                    │ 8. Review & Convert     │
                    │    to Word              │
                    └─────────────────────────┘
```

---

## Step 1: Read Functional Document (FD)

### IMPORTANT: Section & Use Case Must Match FD

| Element | Rule |
|---------|------|
| **Section Title** | **COPY PASTE** from FD section title |
| **Use Case** | **COPY PASTE** from FD use case content |

**Example:**
```
FD Section: "Section 2: Function: Apply Permanent Suspension from Staff Portal"
     ↓
TD Section: "Section 1 – Apply Permanent Suspension from Staff Portal"

FD Use Case: "The OCMS Staff Portal allows OIC to apply..."
     ↓
TD Use Case: "The OCMS Staff Portal allows OIC to apply..." (same content)
```

### What to Extract

> **Important:** Skip Section 1 (User Story) from FD. Start extracting from Section 2.

| Section | Extract | Action |
|---------|---------|--------|
| Section Title | Section names from FD (from Section 2 onwards) | **COPY PASTE** |
| Use Case | Use case content from FD | **COPY PASTE** |
| Process Flow | Steps, decisions, conditions | Summarize |
| Validation Rules | FE validations, BE validations | Extract |
| API Endpoints | Request/response format | Extract |
| Error Codes | Error scenarios and messages | Extract |
| Data Mapping | Input/output fields, database tables | Extract |
| Business Rules | Special conditions, exceptions | Extract |

### Output
- [ ] List of section titles (copied from FD, skip Section 1)
- [ ] Use case content for each section (copied from FD)
- [ ] What are the main flows
- [ ] What validations are required
- [ ] What error codes exist

---

## Step 2: Review Functional Draw.io

### What to Check

| Element | Check |
|---------|-------|
| Swimlanes | Which systems/portals involved |
| Process Steps | Sequence of actions |
| Decision Points | Conditions and branches |
| API Calls | Which APIs are called |
| Error Paths | How errors are handled |

### Output
- [ ] List all swimlanes (systems)
- [ ] List all process steps
- [ ] List all decision points
- [ ] Identify missing flows

---

## Step 3: Analyze Existing Code

### Files to Find

| Type | Pattern | Purpose |
|------|---------|---------|
| Controller | `*Controller.java` | API endpoints |
| Helper | `*Helper.java` | Business logic |
| Service | `*Service.java`, `*ServiceImpl.java` | Service layer |
| Repository | `*Repository.java` | Database operations |
| Model | `*.java` in models/ | Data structures |

### What to Extract from Code

**From Controller:**
```java
// Find endpoints
@PostMapping("/v1/endpoint-name")
public ResponseEntity<?> methodName(@RequestBody Request request)

// Extract:
// - Endpoint URL
// - HTTP method
// - Request body structure
// - Response structure
```

**From Helper:**
```java
// Find validation logic
private static final Set<String> ALLOWED_CODES = ...
private static final List<String> ALLOWED_STAGES = ...

// Find error codes
return new Response("OCMS-4000", "Error message");

// Extract:
// - Allowed values
// - Validation rules
// - Error codes and messages
```

**From Service/Repository:**
```java
// Find database operations
repository.save(entity);
repository.findByField(value);

// Extract:
// - Tables affected
// - Fields updated
// - Query conditions
```

### Output
- [ ] List all API endpoints with request/response
- [ ] List all validation rules from code
- [ ] List all error codes from code
- [ ] List all database tables affected

---

## Step 4: Create Plan Files

### 4.1 Create plan_api.md

```markdown
# API Plan

## 1. Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| /v1/xxx | POST | xxx |

## 2. Request/Response

### API: [Name]
**Endpoint:** POST /v1/xxx

**Request:**
\`\`\`json
{ ... }
\`\`\`

**Response:**
\`\`\`json
{ ... }
\`\`\`

## 3. Error Codes

| Code | Message |
|------|---------|
| OCMS-4000 | xxx |
```

### 4.2 Create plan_condition.md

```markdown
# Validation Plan

## 1. Frontend Validations

| ID | Field | Rule | Error Message |
|----|-------|------|---------------|
| FE-001 | xxx | xxx | xxx |

## 2. Backend Validations

| ID | Step | Validation | Error Code |
|----|------|------------|------------|
| BE-001 | xxx | xxx | OCMS-4000 |

## 3. Business Rules

| Rule | Condition | Action |
|------|-----------|--------|
| xxx | xxx | xxx |
```

### 4.3 Create plan_flowchart.md

```markdown
# Flowchart Plan

## 1. Diagram Sections

| Section | Name | Description |
|---------|------|-------------|
| 1 | High Level Flow | Overview |
| 2 | UI Validation | Frontend checks |
| 3 | Backend Validation | Backend checks |
| 4 | [Flow Name] | Detailed flow |

## 2. Process Steps

| Step | Type | Description | Next |
|------|------|-------------|------|
| 1 | Start | xxx | 2 |
| 2 | Process | xxx | 3 |
| 3 | Decision | xxx | 4/E1 |
```

---

## Step 5: Create Technical Draw.io

### 5.1 Create Multi-Section File

| Section | Name Pattern | Content |
|---------|--------------|---------|
| 1 | [Feature] High Level Flow | Overview of all flows |
| 2 | UI-Side Validation | Frontend validation logic |
| 3 | Backend-Side Validation | Backend validation steps |
| 4 | [Source] Manual [Action] | Staff Portal flow |
| 5 | [External] [Action] | PLUS/External flow |
| 6 | Auto [Action] by Backend | Cron/Auto flow |
| 7 | [Feature] Report | Report generation |

### 5.2 Swimlane Colors

| Swimlane | Color | Hex |
|----------|-------|-----|
| Staff Portal | Purple | #e1d5e7 |
| PLUS Portal | Purple | #e1d5e7 |
| Backend API | Green | #d5e8d4 |
| External Backend | Yellow | #fff2cc |
| Cron Service | Yellow | #fff2cc |

### 5.3 Standard Elements

| Element | Shape | Use |
|---------|-------|-----|
| Start/End | Rounded rectangle (terminator) | Entry/exit points |
| Process | Rectangle | Actions |
| Decision | Diamond | Yes/No branches |
| Annotation | Text box with border | API payload, notes |

---

## Step 6: Compare with Code

### 6.1 Create Comparison Table

| # | Item | Flowchart | Code | Status |
|---|------|-----------|------|--------|
| 1 | API endpoint | /v1/xxx | Controller line X | ✅ Correct |
| 2 | Allowed codes | A, B, C | Helper line X | ⚠️ Mismatch |
| 3 | Validation | xxx | Not found | ❌ Missing |

### 6.2 Status Legend

| Status | Meaning | Action |
|--------|---------|--------|
| ✅ Correct | Flowchart matches code | None |
| ⚠️ Mismatch | Flowchart differs from code | Update flowchart or confirm with BA |
| ❌ Missing in Flowchart | Code has it, flowchart doesn't | Add to flowchart |
| ❌ Not Implemented | Flowchart has it, code doesn't | Confirm with BA |

### 6.3 Fix Discrepancies
- Update flowchart if code is correct
- Raise to BA if unclear which is correct
- Document any known gaps

---

## Step 7: Generate Tech Doc

### 7.1 IMPORTANT: Match FD Structure

| TD Element | Source | Action |
|------------|--------|--------|
| **Section Title** | FD Section Title | **COPY PASTE** |
| **Use Case** | FD Use Case | **COPY PASTE** |
| Flow Diagram | Tech Draw.io | Create new (technical view) |
| Data Mapping | plan_api.md + code | Write in TD |
| Validation Rules | FD | **REFER to FD** (don't duplicate) |
| Error Codes | FD | **REFER to FD** (don't duplicate) |
| Business Rules | FD | **REFER to FD** (don't duplicate) |

### 7.1.1 Refer to FD (Don't Duplicate)

If content already exists in FD, refer instead of copying:

```markdown
<!-- Good - Reference to FD -->
Refer to FD Section 3.2 for detailed validation rules.

<!-- Bad - Duplicating FD content -->
Validation Rules:
1. Field X must not be empty
2. Field Y must be numeric
... (copied from FD)
```

**When to copy vs refer:**
| Content | Action |
|---------|--------|
| Use Case | Copy from FD |
| Detailed validation rules | Refer to FD |
| Complete field list | Refer to FD |
| Error code definitions | Refer to FD |
| Business rules | Refer to FD |
| Flow diagram | Create new |
| Data Mapping | Document in TD |

### 7.2 Template Structure

> **Note:** Skip Section 1 (User Story) from FD. Tech Doc starts from Section 1 (which maps to FD Section 2).

```markdown
# OCMS [NUMBER] – [Feature Name from FD]

## Version History
## Table of Content

# Section 1 – [COPY Section 2 Title from FD]
## 1.1 Use Case
1. [First point from FD Use Case]

2. [Second point with sub-items if any]:<br>a. [Sub-item a]<br>b. [Sub-item b]

3. [Third point]

## 1.2 Process Flow
![Flow Diagram](./images/section1-flow.png)

| Step | Description | Brief Description |
| --- | --- | --- |
| [Step Name] | [What happens] | [Short summary] |

## 1.3 Data Mapping
| Zone | Database Table | Field Name |

## 1.4 Success Outcome
- [Success condition 1]
- [Success condition 2]

## 1.5 Error Handling
| Error Scenario | Definition | Brief Description |

# Section 2 – [COPY Section 3 Title from FD]
## 2.1 Use Case
1. [First point from FD Use Case]

2. [Second point with sub-items if any]:<br>a. [Sub-item a]<br>b. [Sub-item b]

## 2.2 Process Flow
## 2.3 Data Mapping
## 2.4 Success Outcome
## 2.5 Error Handling
...
```

**Section Mapping:**
| FD Section | TD Section | Content |
|------------|------------|---------|
| Section 1 (User Story) | ❌ Skip | Not included in TD |
| Section 2 | Section 1 | First functional section |
| Section 3 | Section 2 | Second functional section |
| Section N | Section N-1 | And so on... |

### 7.3 Required Elements Per Section (from Template)

Each section MUST have these elements:

| Element | Format | Required |
|---------|--------|----------|
| **Use Case** | Numbered list (1, 2, 3) with sub-items (a, b, c) | ✅ Yes |
| **Process Flow** | Diagram + Step table (Step \| Description \| Brief Description) | ✅ Yes |
| **API Specification** | API table (Name, URL, Method, Payload, Response) | ⚠️ If applicable |
| **Data Mapping** | Zone \| Database Table \| Field Name | ✅ Yes |
| **Success Outcome** | Bullet list of success conditions | ✅ Yes |
| **Error Handling** | Application Error + API Error tables | ✅ Yes |

### 7.4 API Specification Format

```markdown
### API Specification

#### API for eService

##### API [API Name]

| Field | Value |
| --- | --- |
| API Name | [api_name] |
| URL | UAT: https://[domain]/ocms/v1/[endpoint] <br> PRD: https://[domain]/ocms/v1/[endpoint] |
| Description | [API description] |
| Method | POST |
| Header | `{ "Authorization": "Bearer [token]", "Content-Type": "application/json" }` |
| Payload | `{ "field": "value" }` |
| Response | `{ "totalProcessed": 1, "successCount": 1, "errorCount": 0, "results": [{ "noticeNo": "500500303J", "appCode": "OCMS-2000", "message": "Success" }] }` |
| Response (Empty) | `{ "appCode": "OCMS-2000", "message": "Success", "data": [] }` |
| Response Failure | `{ "totalProcessed": 1, "successCount": 0, "errorCount": 1, "results": [{ "noticeNo": "500500303J", "appCode": "OCMS-4000", "message": "Error message" }] }` |
```

**IMPORTANT - API Rules:**
- **ALL APIs must use POST method** - No GET, PUT, PATCH, DELETE allowed
- Use `appCode` and `message`, NOT `status`
- For batch operations: use `totalProcessed`, `successCount`, `errorCount`, `results[]`
- For single/list queries: use `appCode`, `message`, `data`

**API Types:**
- **API for eService**: APIs provided by backend for frontend (eService, Staff Portal)
- **API Provide**: APIs provided to external systems (AXS, PLUS, etc.)
- **API Consume**: APIs consumed from external systems (URA PG, SPCP, LTA, etc.)

### 7.5 Data Mapping Format

```markdown
| Zone | Database Table | Field Name |
| --- | --- | --- |
| Intranet | ocms_valid_offence_notice | notice_no |
| Intranet | ocms_suspended_notice | suspension_type |
| Internet | ocms_suspended_notice | cre_user_id |
```

**Extended format (with UI mapping):**
```markdown
| Zone | Database Table | Field Name | UI Field | Description |
| --- | --- | --- | --- | --- |
| Internet | eocms_valid_offence_notice | vehicle_no | Vehicle No | Vehicle number |
| Internet | eocms_valid_offence_notice | notice_no | Notice No | Notice number |
```

**Notes to include:**
- Audit user fields: `ocmsiz_app_conn` (Intranet), `ocmsez_app_conn` (Internet)
- Insert order: Parent table first (UPDATE), then child table (INSERT)

### 7.6 Success Outcome Format

```markdown
## X.X Success Outcome

- [Main success condition - e.g., "Record successfully inserted into database"]
- [Secondary outcome - e.g., "Status updated in parent table"]
- [User feedback - e.g., "Success message displayed to user"]
- The workflow reaches the End state without triggering any error-handling paths
```

### 7.7 Error Handling Format

```markdown
## X.X Error Handling

### Application Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Invalid Input | Required field is empty or invalid | Return error response, no record created |
| Unauthorized | User not authorized for this action | Display error message, block action |
| Database Error | Unable to insert/update record | Log error, display system error message |

### API Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| General Server Error | OCMS-5000 | Something went wrong. Please try again later. | Server error |
| Bad Request | OCMS-4000 | Invalid request. Please check and try again. | Invalid syntax |
| Unauthorized Access | OCMS-4001 | You are not authorized. Please log in and try again. | Auth failed |
```

**Standard API Error Codes:**
| Code | Category | Description |
| --- | --- | --- |
| OCMS-4000 | Client Error | Bad Request |
| OCMS-4001 | Client Error | Unauthorized |
| OCMS-4004 | Client Error | Not Found |
| OCMS-5000 | Server Error | Internal Server Error |

### 7.8 Validation Flow Documentation

If the flowchart includes UI Validation and Backend Validation flows, include these as separate sub-sections in the tech doc:

```markdown
## X.X Scenario Validation

### X.X.1 UI-Side Validation Flow

<!-- Insert UI validation flow diagram here -->
![UI Validation Flow](./images/sectionX-ui-validation.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Form Submit | User clicks submit button | Initiate validation |
| Field Validation | Check all required fields | Validate input |
| Format Check | Verify data format (date, number, etc.) | Format validation |
| Success | All validations pass | Proceed to backend |
| Error | Validation fails | Display error message |

### X.X.2 Backend-Side Validation Flow

<!-- Insert backend validation flow diagram here -->
![Backend Validation Flow](./images/sectionX-backend-validation.png)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Description | Brief Description |
| --- | --- | --- |
| Receive Request | Backend receives API request | API entry point |
| Business Rule Check | Validate against business rules | Rule validation |
| Database Check | Check data existence/status | DB validation |
| Success | All validations pass | Process request |
| Error | Validation fails | Return error response |
```

**When to include Validation Flows:**
- Include if flowchart has separate UI Validation and Backend Validation tabs/sections
- Reference the validation flow diagrams from the Draw.io file
- Each validation flow should have its own diagram and step table

### 7.9 User List API for Dropdowns/Filters

For report or filter features that need user dropdown lists, document the API:

```markdown
#### API Get User List

| Field | Value |
| --- | --- |
| API Name | userlist |
| URL | UAT: https://[domain]/ocms/v1/userlist <br> PRD: https://[domain]/ocms/v1/userlist |
| Description | Retrieve list of users for dropdown filter |
| Method | POST |
| Header | `{ "Authorization": "Bearer [token]", "Content-Type": "application/json" }` |
| Payload | `{ "status": "A" }` |
| Response | `{ "appCode": "OCMS-2000", "message": "Success", "data": [{ "userId": "JOHNLEE", "firstName": "John", "lastName": "Lee" }] }` |
| Response (Empty) | `{ "appCode": "OCMS-2000", "message": "Success", "data": [] }` |
| Response Failure | `{ "appCode": "OCMS-5000", "message": "Error message" }` |
```

**Data Source:**
- User list typically comes from `ocms_user` table
- Fields: `user_id`, `first_name`, `last_name`, etc.

**Empty Response Handling:**
- When table is empty or no results found, return empty array `[]` in data field
- Do NOT return error code for empty results - this is a valid success response

---

## Step 8: Review & Convert to Word

### 8.1 Review Using Checklist

Use `docs/templates/10-yijie-reviewer-checklist.md` to review:

**Must Pass:**
- [ ] All sections have Use Case
- [ ] All sections have Process Flow with diagram placeholder
- [ ] All sections have Process Flow table (Step | Description | Brief Description)
- [ ] All sections have API Specification (if applicable)
- [ ] All sections have Data Mapping (Zone | Table | Field format)
- [ ] All sections have Success Outcome
- [ ] All sections have Error Handling (Application + API Error tables)
- [ ] Audit user fields documented (ocmsiz_app_conn / ocmsez_app_conn)
- [ ] Insert order documented (Parent first, Child after)
- [ ] Validation flows documented (if flowchart has UI/Backend validation tabs)
- [ ] User list API documented (if report/filter needs user dropdown)
- [ ] **ALL APIs use POST method** (no GET, PUT, PATCH, DELETE)
- [ ] Response format uses appCode/message (NOT status)
- [ ] Empty response uses empty array `[]` in data field

### 8.2 Convert to Word

Use the conversion tool:

```bash
# Single file conversion
python tools/md_to_word.py "OCMS XX/OCMS_Technical_Document.md"

# Directory conversion (all .md files)
python tools/md_to_word.py "./OCMS XX" --dir
```

**Requirements:**
```bash
pip install python-docx
```

**Output:** Creates `.docx` file in same directory as source `.md` file.

---

## Checklist

### Before Starting
- [ ] Have access to Functional Document
- [ ] Have access to Functional Draw.io
- [ ] Have access to codebase
- [ ] Created OCMS [NUMBER] folder

### Plan Files Created
- [ ] plan_api.md
- [ ] plan_condition.md
- [ ] plan_flowchart.md

### Draw.io Created
- [ ] Multi-section Draw.io file
- [ ] All flows covered
- [ ] Swimlanes colored correctly

### Comparison Done
- [ ] Compared flowchart with code
- [ ] Fixed discrepancies
- [ ] Documented gaps

### Tech Doc Generated
- [ ] All sections have Use Case (copied from FD)
- [ ] All sections have Process Flow (with diagram placeholder)
- [ ] All sections have Process Flow table (Step | Description | Brief Description)
- [ ] All sections have API Specification (if applicable)
- [ ] All sections have Data Mapping (Zone | Table | Field)
- [ ] All sections have Success Outcome
- [ ] All sections have Error Handling (Application + API Error tables)
- [ ] Audit user fields documented
- [ ] Insert order documented
- [ ] Validation flows documented (if flowchart has UI/Backend validation)
- [ ] User list API documented (if report/filter needs user dropdown)
- [ ] **ALL APIs use POST method** (no GET)
- [ ] Response format correct (appCode/message, NOT status)

### Final Review
- [ ] Follows Yi Jie guidelines (`docs/guidelines/09-yijie-flow-content-guideline.md`)
- [ ] Passes reviewer checklist (`10-yijie-reviewer-checklist.md`)
- [ ] Converted to Word using `tools/md_to_word.py`

---

## Prompt List (for Claude)

### Step 1: Read Functional Document
```
Read the functional document @[FD file path] and extract:
1. Section titles starting from Section 2 (skip Section 1 User Story)
2. Use case content for each section (will COPY PASTE to TD)
3. Actors
4. Main flows
5. Validation rules
6. Error codes
7. Data mapping

Note: Section 1 (User Story) is not needed in Tech Doc
```

### Step 2: Review Functional Draw.io
```
Review the functional draw.io @[drawio file path] and list:
1. All swimlanes (systems involved)
2. All process steps
3. All decision points
4. API calls
```

### Step 3: Analyze Code
```
Find and read the code files related to [feature name]:
1. Find Controller files
2. Find Helper files
3. Extract API endpoints, validation rules, and error codes
```

```
Compare the flowchart with the code and create a table showing:
- Which items are correct
- Which items are missing
- Which items have mismatch
```

### Step 4: Create Plan Files
```
Based on the FD and code, create plan_api.md with:
1. All API endpoints
2. Request/response examples
3. Error codes
```

```
Based on the FD and code, create plan_condition.md with:
1. Frontend validations
2. Backend validations
3. Business rules
```

```
Based on the FD and drawio, create plan_flowchart.md with:
1. Diagram sections needed
2. Process steps for each flow
```

### Step 5: Create Draw.io
```
Create a multi-section draw.io file based on the plan files with sections:
1. High Level Flow
2. UI-Side Validation
3. Backend-Side Validation
4. [Specific flows as needed]
```

```
Can you make the flow follow the example @[approved drawio path]
```

### Step 6: Compare with Code
```
Compare the flowchart with the actual code (not plan), make a table to list which one is:
- Correct
- Missing
- Not implemented yet
```

### Step 7: Generate Tech Doc
```
Generate the tech doc based on the FD @[FD file path] with:
- Skip Section 1 (User Story) from FD
- Section titles COPY PASTE from FD (starting from Section 2)
- Use Case content COPY PASTE from FD
- Renumber sections (FD Section 2 → TD Section 1)
- Each section must have: Use Case, Process Flow, Data Mapping, Success Outcome, Error Handling
```

### Step 8: Review & Fix
```
Review the tech doc using @docs/templates/10-yijie-reviewer-checklist.md
```

```
Compare tech doc with @docs/templates/00-master-template.md and fix any gaps
```

```
Update tech doc to match template format:
- Add flow diagram placeholders
- Add Process Flow tables (Step | Description | Brief Description)
- Add API Specification tables (if applicable)
- Add Data Mapping tables (Zone | Database Table | Field Name)
- Add Success Outcome sections
- Add Error Handling tables (Application Error + API Error)
```

### Step 9: Convert to Word
```
Convert the tech doc to Word format using tools/md_to_word.py
```

---

## Quick Command Reference

### Find Code Files
```bash
# Find controllers
find . -name "*Controller.java" | grep -i [feature]

# Find helpers
find . -name "*Helper.java" | grep -i [feature]

# Search for endpoint
grep -r "/v1/[endpoint]" --include="*.java"

# Search for error code
grep -r "OCMS-4" --include="*.java"
```

### Convert Markdown to Word
```bash
# Single file
python tools/md_to_word.py document.md

# With custom output name
python tools/md_to_word.py document.md output.docx

# All files in directory
python tools/md_to_word.py ./docs --dir
```

### Folder Structure
```
OCMS [NUMBER]/
├── OCMS[NUMBER]_Technical_Document.md
├── OCMS[NUMBER]_Technical_Document.docx  (generated)
├── OCMS [NUMBER].drawio
├── plan_api.md
├── plan_condition.md
├── plan_flowchart.md
└── images/
    ├── section1-flow.png
    ├── section2-flow.png
    └── ...
```

---

## Tools Available

| Tool | Location | Purpose |
|------|----------|---------|
| md_to_word.py | `tools/md_to_word.py` | Convert Markdown to Word |
| word_to_md.py | `tools/word_to_md.py` | Convert Word to Markdown |

### md_to_word.py Features
- Headers (H1-H6)
- Tables with borders and shaded headers
- Bold, italic, inline code
- Bullet and numbered lists
- Images (if file exists)
- Code blocks
- HTML comments (skipped)

### Installation
```bash
pip install python-docx mammoth beautifulsoup4
```

# Yi Jie - Reviewer Checklist

<!--
Checklist for reviewing technical documentation.
Use this to verify compliance with project standards.
-->

---

## API Design Review

### HTTP & Security
- [ ] **ALL APIs use POST method** (No GET, PUT, PATCH, DELETE allowed)
- [ ] No sensitive data exposed in URL

### Response Format
- [ ] Success response: `{ data: { appCode, message } }`
- [ ] List response includes: `total`, `limit`, `skip`, `data`
- [ ] Error response includes: `appCode`, `message`

### API Naming
- [ ] Endpoint names are **intuitive**
- [ ] CRUD endpoints follow **table name**
- [ ] No generic names like `/workflow`

### Response Data
- [ ] Returns **only required fields** for screen
- [ ] No excessive data (e.g., all 156 fields)
- [ ] Tab/view-specific data is separated

### Dropdown APIs
- [ ] Returns **only active records**

---

## Database Design Review

### Numbering
- [ ] Uses **SQL Server sequences** (not app-generated)

### Insert Order
- [ ] **Parent table updated first** (e.g., VON)
- [ ] **Child table updated after** (e.g., OND)

### Table Design
- [ ] Tables designed based on **functional use case**
- [ ] Parameterizable codes use **parameter table**
- [ ] Dropdown items use **standard code table**
- [ ] **No hardcoded values** in code

### Storage
- [ ] Templates stored in **ocms_template_store**
- [ ] Batch job archival: **3 months**

### Audit User Fields
- [ ] `cre_user_id` does NOT use "SYSTEM"
- [ ] `upd_user_id` does NOT use "SYSTEM"
- [ ] Intranet uses **`ocmsiz_app_conn`**
- [ ] Internet uses **`ocmsez_app_conn`**

### SQL Query
- [ ] **No `SELECT *`** used in queries
- [ ] Only **required fields** specified in SELECT statements
- [ ] Date range checks use **SQL query** (not manual comparison in code)

### Data Dictionary Compliance
- [ ] Table names match **data dictionary** (`docs/data-dictionary/`)
- [ ] Column names match **data dictionary** exactly
- [ ] Data types match **data dictionary** (varchar length, numeric precision)
- [ ] Primary keys match **data dictionary** definition
- [ ] Nullable fields match **data dictionary** (NULL vs NOT NULL)
- [ ] Default values match **data dictionary** if specified
- [ ] New tables/columns are **documented in data dictionary**

---

## File Handling Review

- [ ] Upload flow: **Temp folder → Permanent folder**
- [ ] Excel templates stored in **blob storage**
- [ ] Report output format is **Excel (.xlsx)** (not CSV/PDF)

---

## Validation Review

- [ ] **Both FE and BE** validation implemented
- [ ] Supports multiple offence types (O, E, U)
- [ ] Date range validation: **max 1 year** for manual reports

---

## External Integration Review

### Token
- [ ] **Auto refresh** on token expiry
- [ ] Processing **continues** after refresh

### Retry
- [ ] **3 retries** on connection failure
- [ ] **Email alert** after all retries fail

### App Code
- [ ] Correct **module-specific app code** used

---

## Batch Job Review

### Naming
- [ ] Follows Shedlock naming convention:
  - `[action]_[subject]_[suffix]` for file/report
  - `[action]_[subject]` for API/other
  - `[action]_[subject][term]` for special cases

### Tracking
- [ ] **Start time** recorded immediately when job starts
- [ ] Process broken up to handle memory loss scenarios
- [ ] Can identify stuck/incomplete jobs
- [ ] Can identify jobs that didn't start

### Logging
- [ ] Frequent sync jobs use **application logs** only
- [ ] No batch table logging for frequent syncs

---

## Documentation Content Review

### Completeness
- [ ] All JSON responses are **complete** (not truncated)
- [ ] API payloads include **context** (what data, why)
- [ ] Flow descriptions are **clear and complete**

### Focus
- [ ] Document maintains **focus and context** throughout
- [ ] No loss of document purpose in middle sections
- [ ] Each section relates to main topic

### Clarity
- [ ] Appropriate level of explanation (not over-explained)
- [ ] Separate validation API documented if exists
- [ ] Push mechanism explained (API or direct insert)

### Consistency
- [ ] **Diagram, table description, and FD are in sync**
- [ ] All sources combined into **1 comprehensive flow**
- [ ] All validation and logic in one place (not scattered)
- [ ] Single comprehensive diagram (not fragmented)

### Eligibility Scenarios
- [ ] **Key design documented** for eligibility checks
- [ ] Different scenarios covered for **each source**
- [ ] Decision matrix for source-specific allowed actions
- [ ] Clear implementation guidance per source

### FD Reference (Avoid Duplication)
- [ ] **References to FD** used where content already exists
- [ ] No unnecessary duplication of FD content
- [ ] Use Case copied from FD (required)
- [ ] Validation rules refer to FD (if detailed in FD)
- [ ] Error codes refer to FD (if documented in FD)

### Developer Usability
- [ ] Documentation **helps developers** understand implementation
- [ ] **Technical specs** sufficient for coding (data types, constraints)
- [ ] **Integration points** clearly identified (APIs, tables)
- [ ] **Error handling** approach documented
- [ ] **Edge cases** and boundary conditions addressed
- [ ] Developer can read this and know **what to build**

### Data Source Attribution
- [ ] **Every data field** has source explained
- [ ] Database fields show **table.column** source
- [ ] Calculated fields show **formula/logic**
- [ ] API response fields show **source table/API**
- [ ] Dropdown values reference **source table**
- [ ] No data shown **without source attribution**

---

## Programmer Self-Sufficiency Review

### 5W1H Test (Can programmer implement without asking tech writer?)
- [ ] **WHAT**: Feature/API/process clearly described
- [ ] **WHY**: Business context explained (purpose of the feature)
- [ ] **WHERE**: Integration points identified (which systems/modules)
- [ ] **WHEN**: Trigger conditions specified (what starts this process)
- [ ] **WHO**: User roles and system actors identified
- [ ] **HOW**: Technical implementation details provided

### Implementation Completeness
- [ ] **What to build**: Clear feature/function description provided
- [ ] **How to build**: Technical approach and patterns documented
- [ ] **What data**: All fields have data types, sources, constraints
- [ ] **What validations**: All business rules with clear conditions
- [ ] **What errors**: All error scenarios with codes and messages
- [ ] **What flow**: Complete flow from trigger to end state
- [ ] **What edge cases**: Boundary conditions and special scenarios

### Programmer Questions Prevention
Ask yourself: "If I'm a programmer, will I need to ask about..."
- [ ] Which table to insert/update? → Table names documented
- [ ] What is the field data type? → Data types specified
- [ ] What is the max length? → Field lengths documented
- [ ] Is this field nullable? → Nullable/required specified
- [ ] What is the default value? → Defaults documented
- [ ] What triggers this process? → Trigger clearly stated
- [ ] What happens if X fails? → Error handling documented
- [ ] What is the sequence/order? → Step sequence numbered
- [ ] Which API to call? → API endpoints listed
- [ ] What payload to send? → Request payload shown
- [ ] What response to expect? → Response structure shown

---

## Common Issues to Check

| Issue | Check For |
|-------|-----------|
| **Wrong HTTP method** | **GET/PUT/PATCH/DELETE instead of POST - ALL APIs must use POST** |
| Excessive response data | All fields returned instead of required |
| **SELECT * in queries** | Using SELECT * instead of specific fields |
| Hardcoded values | Magic numbers/strings in code |
| Missing retry | No retry on external API failure |
| Token stops processing | System stops when token expires |
| Wrong insert order | Child table before parent |
| App-generated sequences | Not using SQL Server sequences |
| Generic API names | `/workflow`, `/process`, `/data` |
| Direct file storage | Files not going through temp folder |
| Missing validation | Only FE or only BE validation |
| Incomplete JSON | Truncated or missing response fields |
| Missing sync flag | Sync flag not documented in tables |
| Lost document focus | Writer forgot document context |
| Different logic | Same process with different logic in different sections |
| **Out of sync** | Diagram vs table vs FD don't match |
| **Missing eligibility** | No design for different source scenarios |
| Scattered info | Validation/logic spread across multiple places |
| **Wrong audit user** | Using "SYSTEM" instead of db user |
| **Duplicated content** | Copying FD content instead of referring |
| **Not developer-friendly** | Doc doesn't help developers implement |
| **Missing data source** | Data shown without source attribution |
| **Data dictionary mismatch** | Table/column names don't match data dictionary |
| **Missing in data dictionary** | New tables/columns not documented in data dictionary |
| **Not self-sufficient** | Programmer needs to ask tech writer for missing info |
| **Missing 5W1H** | What/Why/Where/When/Who/How not fully answered |
| **Wrong report format** | Using CSV/PDF instead of Excel for reports |
| **Manual date comparison** | Comparing dates in code instead of using SQL query |

---

## Common Reviewer Questions

When reviewing, ask these questions if documentation is unclear:

| Question Type | Example Questions |
|---------------|-------------------|
| **Sync Flag** | "Where is the sync flag? I don't see sync flag in the table." |
| **Logging** | "Why log to batch table for frequent sync jobs?" |
| **Scenarios** | "Why treat as no update? What about scenario of update [field]?" |
| **Mechanism** | "How does this push work? Through API or direct insert?" |
| **Validity** | "All these still valid scenarios? Since no more logging to batch job table?" |
| **Error Flow** | "When will this scenario happen? This will be handled by the cron retry." |
| **Environment** | "Which environment doesn't match with which?" |
| **Logic** | "This process seems to have different logic comparing to above - why separate?" |
| **Sequential** | "Is it sequential or parallel? If one fails, will others continue?" |
| **Actions** | "All the actions in the box should be update and insert, not just insert right?" |
| **Consistency** | "Diagram vs table vs FD not in sync - which is correct?" |
| **Eligibility** | "Missing key design for different eligible scenarios per source" |
| **Developer Usability** | "Can this tech doc help developer? Is the implementation guidance clear?" |
| **Data Source** | "Where does this data come from? What is the source of this field?" |
| **Data Dictionary** | "Does this table/column exist in the data dictionary? Is the data type correct?" |
| **Programmer Self-Sufficiency** | "Can programmer implement this without asking tech writer? What info is missing?" |
| **5W1H Completeness** | "What triggers this? Who uses it? Where does it integrate? How to implement?" |

---

## Review Decision

| Result | Criteria |
|--------|----------|
| **APPROVED** | All checklist items passed |
| **MINOR REVISION** | 1-3 minor issues to fix |
| **MAJOR REVISION** | Critical issues (API format, security, data integrity) |
| **REJECTED** | Fundamental design issues |

---

## Reviewer Notes

```
Document: OCMS [NUMBER]
Reviewer:
Date:
Result: [ ] Approved  [ ] Minor  [ ] Major  [ ] Rejected

Comments:
-
-
-
```

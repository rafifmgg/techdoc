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

---

## File Handling Review

- [ ] Upload flow: **Temp folder → Permanent folder**
- [ ] Excel templates stored in **blob storage**

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

---

## Common Issues to Check

| Issue | Check For |
|-------|-----------|
| **Wrong HTTP method** | **GET/PUT/PATCH/DELETE instead of POST - ALL APIs must use POST** |
| Excessive response data | All fields returned instead of required |
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

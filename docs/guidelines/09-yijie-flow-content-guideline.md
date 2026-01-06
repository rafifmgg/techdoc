# Yi Jie - Flow & Content Guideline

<!--
Guidelines for creating technical documentation.
Follow these standards when writing your tech doc to pass review.
-->

---

## 1. API Design

### 1.1 HTTP Method
- Use **POST** method for all APIs
- No sensitive data in URL endpoints

### 1.2 Response Format

**Success/Error:**
```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "Success"
  }
}
```

**List with Pagination:**
```json
{
  "total": 100,
  "limit": 10,
  "skip": 0,
  "data": [...]
}
```

### 1.3 API Naming
- Name endpoints **intuitively** based on function
- Follow **table name** for CRUD operations
- Avoid generic names like `/workflow`

### 1.4 Response Data
- Return **only required fields** for current screen
- Don't return all 156 fields when only 10 are needed
- Each tab/view requests its own subset of fields

### 1.5 Dropdown APIs
- Return **only active records** from standard code table

---

## 2. Database Design

### 2.1 Numbering
- Use **SQL Server sequences** for all numbering (notice number, etc.)

### 2.2 Insert Order
- **Parent table first** (e.g., VON)
- **Child table after** (e.g., OND)

### 2.3 Table Design Principles
- Design tables based on **functional use case**
- Parameterizable codes → **parameter table**
- Dropdown/list items → **standard code table**
- **No hardcoded values**

### 2.4 Template Storage
- SMS/Email/PDF templates → **ocms_template_store** table

### 2.5 Archival
- Batch job records → **delete after 3 months**

### 2.6 Audit User Fields
- `cre_user_id` and `upd_user_id` **cannot use "SYSTEM"**
- Use database user instead:
  - Intranet: **`ocmsiz_app_conn`**
  - Internet: **`ocmsez_app_conn`**

---

## 3. File Handling

### 3.1 Upload Flow
```
Upload → Temporary Folder → (After success) → Permanent Folder
```

### 3.2 Excel Templates
- Store in **blob storage**

---

## 4. Validation

### 4.1 Dual Validation
- Implement validation on **both FE and BE**
- Support multiple offence types (O, E, U)

### 4.2 Date Range
- Manual report Excel: max **1 year** date range

---

## 5. External API Integration

### 5.1 Token Handling
- If token expired → **auto refresh** and continue
- Don't stop processing due to token expiry

### 5.2 Retry Mechanism
- On connection failure → **retry 3 times**
- After 3 failures → **trigger email alert**

### 5.3 App Code
- Each module has **different app code** (e.g., SLIFT)

---

## 6. Batch Job

### 6.1 Shedlock Naming

| Type | Pattern | Example |
|------|---------|---------|
| File/Report | `[action]_[subject]_[suffix]` | `generate_report_daily` |
| API/Other | `[action]_[subject]` | `sync_payment` |
| Special | `[action]_[subject][term]` | `process_photosUpload` |

### 6.2 Job Tracking
- **Break up process** to record start time only when it starts
- Don't wait until job ends to log - memory may be lost
- Helps identify:
  - Jobs that started but didn't end (timeout/stuck/loss connection)
  - Jobs that didn't start at all
  - Long running jobs

### 6.3 Frequent Sync Jobs
- **No need** to log to batch job table for frequent sync jobs
- Log only into **application logs** for error messages

---

## 7. Documentation Content

### 7.1 Flow Description
- Include **clear flow descriptions** for each process
- Example: "Stores the provided payment details into the transaction records"

### 7.2 API Payload Context
- Always show **complete API payload** examples
- Include context: what data is being sent and why

### 7.3 JSON Response
- Ensure JSON examples are **complete** (not truncated)
- Show full response structure

### 7.4 Focus and Context
- Maintain **document focus** throughout
- Don't lose context of what the document is about
- Each section should relate to the main topic

### 7.5 Explanation Level
- Some things **don't need detailed explanation**
- Focus on what's important for the reader
- Avoid over-explaining obvious flows

### 7.6 Consistency Across Sources
- **Diagram, table description, and FD must be in sync**
- Combine all sources into **1 comprehensive flow**
- Include all detailed validation and logic in one place
- Single comprehensive diagram works better than scattered info

### 7.7 Eligibility Scenarios
- Document **key design** for eligibility checks
- Show how to check and implement for **different scenarios**
- Cover **different sources** (e.g., Staff Portal, PLUS, Backend)
- Include decision matrix for each source's allowed actions

### 7.8 Reference to FD (Don't Duplicate)
- If information **already exists in FD**, refer to FD instead of copying
- Avoid duplicating content between FD and TD
- Use references like:
  - "Refer to FD Section X.X for detailed validation rules"
  - "See FD Appendix A for complete field list"
  - "Error codes are documented in FD Section X.X"

**When to refer vs copy:**
| Content | Action |
|---------|--------|
| Use Case | Copy from FD |
| Detailed validation rules | Refer to FD |
| Complete field list | Refer to FD |
| Error code definitions | Refer to FD |
| Business rules | Refer to FD |
| Flow diagram | Create new (technical view) |
| Data Mapping | Document in TD |

---

## 8. Quick Reference Table

| Topic | Rule |
|-------|------|
| HTTP Method | POST only |
| Response | `{ data: { appCode, message } }` |
| List Response | Include `total`, `limit`, `skip` |
| API Data | Only required fields |
| Dropdown | Active records only |
| Sequence | SQL Server sequences |
| Insert Order | Parent → Child |
| Config Values | Parameter table |
| Dropdown Values | Standard code table |
| Templates | ocms_template_store |
| Audit User (Intranet) | ocmsiz_app_conn (not SYSTEM) |
| Audit User (Internet) | ocmsez_app_conn (not SYSTEM) |
| File Upload | Temp folder → Permanent |
| Excel Template | Blob storage |
| Validation | FE + BE |
| Date Range | Max 1 year |
| Token Expiry | Auto refresh |
| API Retry | 3 times + email alert |
| Batch Archival | 3 months |
| Batch Job Start | Record start time immediately |
| Frequent Sync Log | Application logs only |
| Flow Description | Include clear description |
| API Payload | Complete with context |
| JSON Response | Complete, not truncated |
| Source Consistency | Diagram = Table = FD (all in sync) |
| Eligibility Design | Document scenarios per source |
| FD Reference | Refer to FD, don't duplicate |

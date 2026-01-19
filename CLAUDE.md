 # Project Documentation Rules
  ## 0. baca file didalam folder docs
  - data-dictionary -> source yang harus sesuai ketika buat flow dan document, jika tdak tersedia harus alert bahwa ada field / table baru
  - guidelines -> rules ketika generate file apapun, format drawio
  - templates -> format document, review checlist, step-step

  ## 1. Generate plan_api.md dan plan_condition.md

  ### Input Sources (Priority Order)
  | Priority | Source | Description |
  |----------|--------|-------------|
  | 1 | Functional Documentation | User stories, requirements |
  | 2 | Backend Code (if exists) | Controllers, Services, DTOs |
  | 3 | Frontend Code (if exists) | Components, API calls, validations |
  | 4 | External API Spec | Integration specifications |
  | 5 | Data Dictionary | Database tables and fields |

  ### Jika Kode TIDAK ADA (Feature Baru)
  1. Analyze functional doc thoroughly
  2. Research similar features yang sudah ada di codebase
  3. Review external API specifications
  4. Review data dictionary
  5. Buat INFORMED ASSUMPTIONS dengan tag [ASSUMPTION]
  6. ASK user untuk review assumptions sebelum lanjut

  ### Struktur plan_api.md
  - Document Information (version, date, source)
  - Internal APIs (endpoint, method, payload, response, errors)
  - External APIs (integrations)

  ### Struktur plan_condition.md
  - Frontend Validations (form fields, UI states)
  - Backend Validations (business rules dengan Rule ID)
  - External API Conditions (pre-call, response handling)
  - Decision Trees
  - Assumptions Log


  ## 2. Generate plan_flowchart.md

  ### Prerequisites (BLOCKING RULE)
  ⚠️ SEBELUM membuat plan_flowchart.md, HARUS check:
  1. plan_api.md HARUS ada
  2. plan_condition.md HARUS ada

  Jika salah satu TIDAK ADA → STOP dan inform user.

  ### Struktur plan_flowchart.md
  - Section 2: High-Level Flow (overview, simple)
  - Section 3: Detailed Flow (validations, conditions, DB queries)
  - 4 Swimlanes: Frontend, Backend, Database, External System

  ### Guideline
  Gunakan file: `docs/guidelines/guideline-plan-flowchart.md`


  ## 3. Create Flowchart (.drawio)

  ### Prerequisites (BLOCKING RULE)
  ⚠️ Flowchart HARUS berdasarkan plan_flowchart.md
  Jika plan_flowchart.md TIDAK ADA → STOP dan remind user.

  ### Reference Structure
  Gunakan flowchart yang sudah ada sebagai reference struktur:
  - File: `docs/drawio/[reference-flowchart].drawio`
  - Tab structure: 3.1, 3.2, 3.3, dst


  ## 4. Generate Technical Documentation

  ### Prerequisites (BLOCKING RULE)
  ⚠️ Technical doc HARUS berdasarkan Flowchart
  Jika Flowchart TIDAK ADA → STOP dan remind user.

  ### Templates
  Gunakan templates di: `docs/templates/`

  ### Content Rules
  DILARANG (NEVER include):
  - ❌ Nama file (e.g., "UserService.java")
  - ❌ Nama function (e.g., "validateRequest()")
  - ❌ Line numbers (e.g., "line 123")
  - ❌ Code references

  BOLEH (ALLOWED):
  - ✅ Conceptual descriptions
  - ✅ Business logic flow
  - ✅ API endpoints
  - ✅ Database table names
  - ✅ Field names


  ## 5. Language Rules

  ### Conversation
  - Gunakan Bahasa Indonesia untuk semua percakapan

  ### File Output
  - Semua file output HARUS dalam Bahasa Inggris
  - Termasuk: plan files, flowchart labels, technical documentation


  ## 6. Script Management
  Jika membuat temporary Python script:
  1. Create script
  2. Execute immediately
  3. Verify result
  4. DELETE script immediately
  5. Confirm deletion

  NEVER leave temporary scripts in working directory.
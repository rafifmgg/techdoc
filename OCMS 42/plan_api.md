# OCMS 42 - API Planning Document

## Document Information
| Item | Detail |
|------|--------|
| Version | 1.2 |
| Date | 2026-01-25 |
| Source | Functional Flow (ocms 42 functional flow.drawio) |
| Feature | Furnish Driver's or Hirer's Particulars via eService |
| FD Reference | v1.3_OCMS 42_Functional_Doc.md |

---

## 1. API Overview

| # | API Name | Method | Zone | Purpose | FD Section |
|---|----------|--------|------|---------|------------|
| 1 | Get Furnishable Notices | POST | Internet | Retrieve list of notices that can be furnished | Section 2 |
| 2 | Get Pending Submissions | POST | Internet | Retrieve pending furnish submissions (past 6 months) | Section 2 |
| 3 | Get Approved Submissions | POST | Internet | Retrieve approved furnish submissions (past 6 months) | Section 2 |
| 4 | Submit Furnish Particulars | POST | Internet | Submit driver/hirer particulars for furnishing | Section 3 |

**Note:** All APIs use POST method per Yi Jie standard (no GET for data retrieval).

---

## 2. API Specifications

### 2.1 Get Furnishable Notices

**Endpoint:** `/api/v1/furnish/notices/furnishable`
**Method:** POST
**Zone:** Internet (eService Backend)
**Auth Source:** Singpass (Individual) / Corppass (Company)

#### Request Payload
```json
{
  "idNumber": "S1234567A",
  "idType": "NRIC",
  "authToken": "Bearer xxx",
  "pagination": {
    "limit": 10,
    "skip": 0
  }
}
```

| Field | Type | Length | Mandatory | Description | Source |
|-------|------|--------|-----------|-------------|--------|
| idNumber | String | 9-10 | Y | Singpass/Corppass ID (NRIC/FIN/UEN) | From SPCP auth callback |
| idType | String | 4 | Y | ID Type: NRIC, FIN, UEN | From SPCP auth callback |
| authToken | String | - | Y | JWT token from SPCP | From SPCP auth callback |
| pagination.limit | Integer | - | N | Max records per page (default 10) | User input |
| pagination.skip | Integer | - | N | Records to skip (default 0) | User input |

#### Response - Success
```json
{
  "data": {
    "appCode": "SUCCESS",
    "message": "Furnishable notices retrieved successfully",
    "offenderIsDriver": false,
    "pagination": {
      "total": 5,
      "limit": 10,
      "skip": 0,
      "hasMore": false
    },
    "notices": [
      {
        "noticeNo": "N1234567890",
        "offenceDate": "2025-01-15",
        "offenceType": "O",
        "vehicleNo": "SBA1234A",
        "lastProcessingStage": "ROV",
        "ownerName": "TAN AH KOW",
        "ownerIdNo": "S1234567A",
        "ownerIdType": "NRIC"
      }
    ]
  }
}
```

**Response Field Data Sources:**
| Field | Source Table | Source Column | DD Type |
|-------|--------------|---------------|---------|
| noticeNo | eocms_valid_offence_notice | notice_no | varchar(10) |
| offenceDate | eocms_valid_offence_notice | notice_date_and_time | datetime2(7) |
| offenceType | eocms_valid_offence_notice | offence_notice_type | varchar(1) |
| vehicleNo | eocms_valid_offence_notice | vehicle_no | varchar(14) |
| lastProcessingStage | eocms_valid_offence_notice | last_processing_stage | varchar(3) |
| ownerName | eocms_offence_notice_owner_driver | name | varchar(204) |
| ownerIdNo | eocms_offence_notice_owner_driver | id_no | varchar(114) |
| ownerIdType | eocms_offence_notice_owner_driver | id_type | varchar(1) |

#### Response - No Records
```json
{
  "data": {
    "appCode": "NO_RECORDS",
    "message": "There are no furnishable notices",
    "offenderIsDriver": false,
    "pagination": {
      "total": 0,
      "limit": 10,
      "skip": 0,
      "hasMore": false
    },
    "notices": []
  }
}
```

#### Response - Offender is Driver
```json
{
  "data": {
    "appCode": "OFFENDER_IS_DRIVER",
    "message": "There are other outstanding notice(s) for [Name] that cannot be furnished. Please proceed to pay.",
    "offenderIsDriver": true,
    "offenderName": "TAN AH KOW",
    "pagination": {
      "total": 0,
      "limit": 10,
      "skip": 0,
      "hasMore": false
    },
    "notices": []
  }
}
```

#### Response - Token Expired/Invalid
```json
{
  "data": {
    "appCode": "AUTH_FAILED",
    "message": "Authentication token expired or invalid. Please re-login.",
    "offenderIsDriver": false,
    "notices": []
  }
}
```

#### Backend Logic (from Functional Flow Tab 2)

**Note:** All queries use explicit column selection (NO SELECT *).

**Step 1: Validate Token**
- Verify JWT token from SPCP is valid and not expired
- If invalid → Return AUTH_FAILED

**Step 2: Query Owner/Driver Table**
```sql
SELECT notice_no, id_no, id_type, name, owner_driver_indicator, offender_indicator
FROM eocms_offence_notice_owner_driver
WHERE id_no = :spcpId
  AND offender_indicator = 'Y'
```
- If no records → Return NO_RECORDS

**Step 3: Check Owner/Driver Indicator**
- If `owner_driver_indicator` = 'D' → Return OFFENDER_IS_DRIVER with name

**Step 4: Get Notice Numbers**
- Extract notice_no list from Step 2 result

**Step 5: Query Valid Offence Notice (eVON)**
```sql
SELECT notice_no, notice_date_and_time, offence_notice_type, vehicle_no, last_processing_stage
FROM eocms_valid_offence_notice
WHERE notice_no IN (:noticeNumbers)
  AND offence_notice_type IN ('O', 'E')
  AND last_processing_stage IN ('ROV', 'ENA', 'RD1', 'RD2')
  AND crs_reason_of_suspension IS NULL
  AND NOT (suspension_type = 'PS'
       AND epr_reason_of_suspension IN ('ANS', 'APP', 'CAN', 'CFA', 'DBB', 'FCT', 'FTC', 'OTH', 'SCT', 'SLC', 'SSV', 'VCT', 'VST'))
```

**Step 6: Filter Existing Applications**
```sql
SELECT notice_no
FROM eocms_furnish_application
WHERE notice_no IN (:noticeNumbers)
  AND owner_id_no = :spcpId
  AND status IN ('P', 'S', 'A')
```
- Exclude notices with existing Pending/Resubmission/Approved applications

**Step 7: Return Result**
- Combine data from Step 2 (owner info) and Step 5 (notice info)
- Apply pagination (limit/skip)
- Return filtered list with pagination metadata

---

### 2.2 Get Pending Submissions

**Endpoint:** `/api/v1/furnish/submissions/pending`
**Method:** POST
**Zone:** Internet (eService Backend)
**Auth Source:** Singpass (Individual) / Corppass (Company)

#### Request Payload
```json
{
  "idNumber": "S1234567A",
  "idType": "NRIC",
  "authToken": "Bearer xxx",
  "pagination": {
    "limit": 10,
    "skip": 0
  }
}
```

| Field | Type | Length | Mandatory | Description | Source |
|-------|------|--------|-----------|-------------|--------|
| idNumber | String | 9-10 | Y | Singpass/Corppass ID | From SPCP auth callback |
| idType | String | 4 | Y | ID Type: NRIC, FIN, UEN | From SPCP auth callback |
| authToken | String | - | Y | JWT token from SPCP | From SPCP auth callback |
| pagination.limit | Integer | - | N | Max records per page (default 10) | User input |
| pagination.skip | Integer | - | N | Records to skip (default 0) | User input |

#### Response - Success
```json
{
  "data": {
    "appCode": "SUCCESS",
    "message": "Pending submissions retrieved successfully",
    "pagination": {
      "total": 3,
      "limit": 10,
      "skip": 0,
      "hasMore": false
    },
    "submissions": [
      {
        "txnNo": "TXN202501150001",
        "noticeNo": "N1234567890",
        "offenceDate": "2025-01-15",
        "vehicleNo": "SBA1234A",
        "status": "P",
        "statusDescription": "Pending",
        "submissionDate": "2025-01-20",
        "furnishName": "LEE AH BENG",
        "furnishIdNo": "S7654321B"
      }
    ]
  }
}
```

**Response Field Data Sources:**
| Field | Source Table | Source Column | DD Type |
|-------|--------------|---------------|---------|
| submissionRefNo | eocms_furnish_application | txn_no | varchar(20) |
| noticeNo | eocms_furnish_application | notice_no | varchar(10) |
| offenceDate | eocms_furnish_application | offence_date | datetime2(7) |
| vehicleNo | eocms_furnish_application | vehicle_no | varchar(14) |
| status | eocms_furnish_application | status | varchar(1) |
| statusDescription | Derived | Lookup from status code | - |
| submissionDate | eocms_furnish_application | cre_date | datetime2(7) |
| furnishName | eocms_furnish_application | furnish_name | varchar(66) |
| furnishIdNo | eocms_furnish_application | furnish_id_no | varchar(12) |

#### Response - No Records
```json
{
  "data": {
    "appCode": "NO_RECORDS",
    "message": "There are no Pending Submissions",
    "pagination": {
      "total": 0,
      "limit": 10,
      "skip": 0,
      "hasMore": false
    },
    "submissions": []
  }
}
```

#### Backend Logic (from Functional Flow Tab 3)

**Note:** All queries use explicit column selection (NO SELECT *).

**Step 1: Validate Token**
- Verify JWT token from SPCP is valid and not expired
- If invalid → Return AUTH_FAILED

**Step 2: Query Furnish Applications**
```sql
SELECT txn_no, notice_no, offence_date, vehicle_no,
       status, cre_date, furnish_name, furnish_id_no
FROM eocms_furnish_application
WHERE owner_id_no = :spcpId
  AND cre_date >= DATEADD(month, -6, GETDATE())
  AND status IN ('P', 'S')
ORDER BY cre_date DESC
```

**Step 3: Apply Pagination**
- Count total records
- Apply OFFSET/FETCH for pagination

**Step 4: Return Result**
- If no records → Return NO_RECORDS
- Return list of pending submissions with pagination metadata

---

### 2.3 Get Approved Submissions

**Endpoint:** `/api/v1/furnish/submissions/approved`
**Method:** POST
**Zone:** Internet (eService Backend)
**Auth Source:** Singpass (Individual) / Corppass (Company)

#### Request Payload
```json
{
  "idNumber": "S1234567A",
  "idType": "NRIC",
  "authToken": "Bearer xxx",
  "pagination": {
    "limit": 10,
    "skip": 0
  }
}
```

| Field | Type | Length | Mandatory | Description | Source |
|-------|------|--------|-----------|-------------|--------|
| idNumber | String | 9-10 | Y | Singpass/Corppass ID | From SPCP auth callback |
| idType | String | 4 | Y | ID Type: NRIC, FIN, UEN | From SPCP auth callback |
| authToken | String | - | Y | JWT token from SPCP | From SPCP auth callback |
| pagination.limit | Integer | - | N | Max records per page (default 10) | User input |
| pagination.skip | Integer | - | N | Records to skip (default 0) | User input |

#### Response - Success
```json
{
  "data": {
    "appCode": "SUCCESS",
    "message": "Approved submissions retrieved successfully",
    "pagination": {
      "total": 2,
      "limit": 10,
      "skip": 0,
      "hasMore": false
    },
    "submissions": [
      {
        "txnNo": "TXN202501100001",
        "noticeNo": "N1234567890",
        "offenceDate": "2025-01-10",
        "vehicleNo": "SBA1234A",
        "status": "A",
        "statusDescription": "Approved",
        "submissionDate": "2025-01-12",
        "approvalDate": "2025-01-14",
        "furnishName": "LEE AH BENG",
        "furnishIdNo": "S7654321B"
      }
    ]
  }
}
```

**Response Field Data Sources:**
| Field | Source Table | Source Column | DD Type |
|-------|--------------|---------------|---------|
| submissionRefNo | eocms_furnish_application | txn_no | varchar(20) |
| noticeNo | eocms_furnish_application | notice_no | varchar(10) |
| offenceDate | eocms_furnish_application | offence_date | datetime2(7) |
| vehicleNo | eocms_furnish_application | vehicle_no | varchar(14) |
| status | eocms_furnish_application | status | varchar(1) |
| statusDescription | Derived | Lookup from status code | - |
| submissionDate | eocms_furnish_application | cre_date | datetime2(7) |
| approvalDate | eocms_furnish_application | upd_date | datetime2(7) |
| furnishName | eocms_furnish_application | furnish_name | varchar(66) |
| furnishIdNo | eocms_furnish_application | furnish_id_no | varchar(12) |

#### Response - No Records
```json
{
  "data": {
    "appCode": "NO_RECORDS",
    "message": "There are no Approved Notices",
    "pagination": {
      "total": 0,
      "limit": 10,
      "skip": 0,
      "hasMore": false
    },
    "submissions": []
  }
}
```

#### Backend Logic (from Functional Flow Tab 4)

**Note:** All queries use explicit column selection (NO SELECT *).

**Step 1: Validate Token**
- Verify JWT token from SPCP is valid and not expired
- If invalid → Return AUTH_FAILED

**Step 2: Query Furnish Applications**
```sql
SELECT txn_no, notice_no, offence_date, vehicle_no,
       status, cre_date, upd_date, furnish_name, furnish_id_no
FROM eocms_furnish_application
WHERE owner_id_no = :spcpId
  AND cre_date >= DATEADD(month, -6, GETDATE())
  AND status = 'A'
ORDER BY upd_date DESC
```

**Step 3: Apply Pagination**
- Count total records
- Apply OFFSET/FETCH for pagination

**Step 4: Return Result**
- If no records → Return NO_RECORDS
- Return list of approved submissions with pagination metadata

---

### 2.4 Submit Furnish Particulars

**Endpoint:** `/api/v1/furnish/submit`
**Method:** POST
**Zone:** Internet (eService Backend)
**Auth Source:** Singpass (Individual) / Corppass (Company)

#### Request Payload
```json
{
  "authToken": "Bearer xxx",
  "notices": [
    {
      "noticeNo": "N1234567890",
      "offenceDate": "2025-01-15"
    }
  ],
  "ownerParticulars": {
    "name": "TAN AH KOW",
    "idNo": "S1234567A",
    "telCode": "+65",
    "telNo": "91234567",
    "emailAddr": "tan@email.com"
  },
  "furnishParticulars": {
    "name": "LEE AH BENG",
    "idType": "N",
    "idNo": "S7654321B",
    "mailBlkNo": "456",
    "mailFloor": "02",
    "mailUnitNo": "02",
    "mailStreetName": "BUKIT TIMAH ROAD",
    "mailBldgName": "",
    "mailPostalCode": "259888",
    "telCode": "+65",
    "telNo": "81234567",
    "emailAddr": "lee@email.com"
  },
  "ownerDriverIndicator": "D",
  "hirerOwnerRelationship": "F",
  "othersRelationshipDesc": "",
  "quesOneAns": "Yes",
  "quesTwoAns": "No",
  "quesThreeAns": "",
  "rentalPeriodFrom": null,
  "rentalPeriodTo": null,
  "declaration": true
}
```

| Field | Type | DD Column | Length | Mandatory | Description |
|-------|------|-----------|--------|-----------|-------------|
| authToken | String | - | - | Y | JWT token from SPCP |
| notices | Array | - | - | Y | List of notices to furnish |
| notices[].noticeNo | String | notice_no | 10 | Y | Notice number |
| notices[].offenceDate | String | offence_date | - | Y | Offence date (datetime2) |
| ownerParticulars.name | String | owner_name | 66 | Y | Submitter name |
| ownerParticulars.idNo | String | owner_id_no | 12 | Y | Submitter ID (from SPCP) |
| ownerParticulars.telCode | String | owner_tel_code | 4 | N | Country code |
| ownerParticulars.telNo | String | owner_tel_no | 20 | N | Contact number |
| ownerParticulars.emailAddr | String | owner_email_addr | 320 | N | Email address |
| furnishParticulars.name | String | furnish_name | 66 | Y | Furnished person name |
| furnishParticulars.idType | String | furnish_id_type | 1 | Y | N=NRIC, F=FIN, U=UEN |
| furnishParticulars.idNo | String | furnish_id_no | 12 | Y | Furnished person ID |
| furnishParticulars.mailBlkNo | String | furnish_mail_blk_no | 10 | Y | Block/House number |
| furnishParticulars.mailFloor | String | furnish_mail_floor | 3 | N | Floor number |
| furnishParticulars.mailUnitNo | String | furnish_mail_unit_no | 5 | N | Unit number |
| furnishParticulars.mailStreetName | String | furnish_mail_street_name | 32 | Y | Street name |
| furnishParticulars.mailBldgName | String | furnish_mail_bldg_name | 65 | N | Building name |
| furnishParticulars.mailPostalCode | String | furnish_mail_postal_code | 6 | Y | Postal code |
| furnishParticulars.telCode | String | furnish_tel_code | 4 | N | Country code |
| furnishParticulars.telNo | String | furnish_tel_no | 12 | N | Contact number |
| furnishParticulars.emailAddr | String | furnish_email_addr | 320 | N | Email address |
| ownerDriverIndicator | String | owner_driver_indicator | 1 | Y | D=Driver, H=Hirer |
| hirerOwnerRelationship | String | hirer_owner_relationship | 1 | Y | F=Family, E=Employee, L=Leased, O=Others |
| othersRelationshipDesc | String | others_relationship_desc | 15 | N | Required if relationship = O |
| quesOneAns | String | ques_one_ans | 32 | Y | Answer to Q1 |
| quesTwoAns | String | ques_two_ans | 32 | Y | Answer to Q2 |
| quesThreeAns | String | ques_three_ans | 32 | N | Answer to Q3 |
| rentalPeriodFrom | DateTime | rental_period_from | - | N | Required if relationship = L |
| rentalPeriodTo | DateTime | rental_period_to | - | N | Required if relationship = L |
| declaration | Boolean | - | - | Y | Must be true |

#### Response - Success
```json
{
  "data": {
    "appCode": "SUCCESS",
    "message": "Your submission has been received.",
    "submissionRefNo": "1234567890",
    "submissionDateTime": "2025-01-25T14:00:00",
    "noticeCount": 1,
    "notices": [
      {
        "noticeNo": "N1234567890",
        "offenceDate": "2025-01-15",
        "offenceType": "O",
        "vehicleNo": "SBA1234A"
      }
    ]
  }
}
```

**Note (FD V1.3):** Owner details are NOT displayed in success response.

#### Response - Validation Failure
```json
{
  "data": {
    "appCode": "VALIDATION_FAILED",
    "message": "Validation failed. Please check input fields.",
    "errors": [
      {
        "field": "furnishParticulars.idNo",
        "code": "INVALID_ID",
        "message": "Invalid ID number format"
      }
    ]
  }
}
```

#### Response - Submission Failure
```json
{
  "data": {
    "appCode": "SUBMISSION_FAILED",
    "message": "Furnish was unsuccessful. Please try again.",
    "errors": []
  }
}
```

#### Response - Notice No Longer Furnishable
```json
{
  "data": {
    "appCode": "NOTICE_NOT_FURNISHABLE",
    "message": "One or more notices are no longer furnishable.",
    "invalidNotices": ["N1234567890"]
  }
}
```

#### Backend Logic (from Functional Flow Tab 2 - High Level)

**Note:** All queries use explicit column selection (NO SELECT *).

**Step 1: Validate Token**
- Verify JWT token from SPCP is valid and not expired
- If invalid → Return AUTH_FAILED

**Step 2: Validate Request Payload**
- Check all mandatory fields present
- Validate ID number formats (NRIC/FIN/UEN regex)
- Validate contact number format (if provided)
- Validate declaration = true
- If validation fails → Return VALIDATION_FAILED with field errors

**Step 3: Re-verify Notice Eligibility**
- For each notice in request, verify still furnishable
- Check no existing Pending/Resubmission/Approved application created since furnishable list was retrieved
- If any notice not furnishable → Return NOTICE_NOT_FURNISHABLE

**Step 4: Generate Transaction Number**
- Use SQL Server sequence: `NEXT VALUE FOR seq_furnish_txn_no`
- Format: TXNYYYYMMDDnnnn (e.g., TXN202501250001)

**Step 5: Insert into Database (Sequential - Parent First)**

**5a. Insert into `eocms_furnish_application` (Parent Table)**
```sql
INSERT INTO eocms_furnish_application (
    txn_no,                    -- PK: varchar(20)
    notice_no,                 -- varchar(10)
    vehicle_no,                -- varchar(14)
    offence_date,              -- datetime2(7)
    pp_code,                   -- varchar(5)
    pp_name,                   -- varchar(100)
    last_processing_stage,     -- varchar(3)
    -- Furnished person details
    furnish_name,              -- varchar(66)
    furnish_id_type,           -- varchar(1): N/F/U
    furnish_id_no,             -- varchar(12)
    furnish_mail_blk_no,       -- varchar(10)
    furnish_mail_floor,        -- varchar(3)
    furnish_mail_street_name,  -- varchar(32)
    furnish_mail_unit_no,      -- varchar(5)
    furnish_mail_bldg_name,    -- varchar(65)
    furnish_mail_postal_code,  -- varchar(6)
    furnish_tel_code,          -- varchar(4)
    furnish_tel_no,            -- varchar(12)
    furnish_email_addr,        -- varchar(320)
    -- Relationship & questionnaire
    owner_driver_indicator,    -- varchar(1): D/H
    hirer_owner_relationship,  -- varchar(1): F/E/L/O
    others_relationship_desc,  -- varchar(15)
    ques_one_ans,              -- varchar(32)
    ques_two_ans,              -- varchar(32)
    ques_three_ans,            -- varchar(32)
    rental_period_from,        -- datetime2(7)
    rental_period_to,          -- datetime2(7)
    -- Submitter (owner) details
    owner_name,                -- varchar(66)
    owner_id_no,               -- varchar(12)
    owner_tel_code,            -- varchar(4)
    owner_tel_no,              -- varchar(20)
    owner_email_addr,          -- varchar(320)
    corppass_staff_name,       -- varchar(66)
    -- Status & sync
    status,                    -- varchar(1): P
    is_sync,                   -- varchar(1): N
    reason_for_review,         -- varchar(255)
    remarks,                   -- varchar(200)
    -- Audit fields
    cre_date,                  -- datetime2(7)
    cre_user_id,               -- varchar(50)
    upd_date,                  -- datetime2(7)
    upd_user_id                -- varchar(50)
) VALUES (
    :txnNo,
    :noticeNo,
    :vehicleNo,
    :offenceDate,
    :ppCode,
    :ppName,
    :lastProcessingStage,
    :furnishName,
    :furnishIdType,
    :furnishIdNo,
    :furnishMailBlkNo,
    :furnishMailFloor,
    :furnishMailStreetName,
    :furnishMailUnitNo,
    :furnishMailBldgName,
    :furnishMailPostalCode,
    :furnishTelCode,
    :furnishTelNo,
    :furnishEmailAddr,
    :ownerDriverIndicator,
    :hirerOwnerRelationship,
    :othersRelationshipDesc,
    :quesOneAns,
    :quesTwoAns,
    :quesThreeAns,
    :rentalPeriodFrom,
    :rentalPeriodTo,
    :ownerName,
    :ownerIdNo,
    :ownerTelCode,
    :ownerTelNo,
    :ownerEmailAddr,
    :corppassStaffName,
    'P',
    'N',
    NULL,
    NULL,
    GETDATE(),
    'ocmsez_app_conn',
    GETDATE(),
    'ocmsez_app_conn'
)
```

**Audit Fields (per Data Dictionary):**
- `cre_user_id` = `ocmsez_app_conn` (Internet zone)
- `upd_user_id` = `ocmsez_app_conn` (Internet zone)
- Never use "SYSTEM" as audit user

**Sync Flag:**
- `is_sync` = 'N' (Not synced to Intranet yet)
- Will be updated to 'Y' after sync job

**Step 6: Handle Errors**
- If insert fails → Rollback transaction, Return SUBMISSION_FAILED
- Log error for investigation

**Step 7: Return Success Response**
- Return submission reference number and notice details
- Do NOT include owner details in response (FD V1.3)

#### Retry Mechanism
- Frontend should implement retry with exponential backoff
- Max 3 retries for timeout errors
- No retry for validation errors (400)
- No retry for auth errors (401)

---

## 3. Database Tables

| Table | Zone | Purpose | Usage |
|-------|------|---------|-------|
| `eocms_offence_notice_owner_driver` | PII | Owner/Driver linkage to notices | READ (query owner info) |
| `eocms_valid_offence_notice` | Internet | Outstanding notices (eVON) | READ (query notice info) |
| `eocms_furnish_application` | PII | Furnish application records | READ/WRITE |
| `eocms_furnish_application_doc` | PII | Supporting documents (if any) | WRITE (future use) |

### 3.1 Key Fields - eocms_furnish_application (from Data Dictionary)

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| txn_no | varchar(20) | N | **Primary Key** - Submission reference number |
| notice_no | varchar(10) | N | Notice number |
| vehicle_no | varchar(14) | N | Vehicle number |
| offence_date | datetime2(7) | N | Date of offence |
| pp_code | varchar(5) | N | Car park code |
| pp_name | varchar(100) | N | Car park name |
| last_processing_stage | varchar(3) | N | Notice processing stage |
| furnish_name | varchar(66) | N | Furnished person name |
| furnish_id_type | varchar(1) | N | N=NRIC, F=FIN, U=UEN |
| furnish_id_no | varchar(12) | N | Furnished person ID |
| furnish_mail_blk_no | varchar(10) | N | Block/house number |
| furnish_mail_floor | varchar(3) | Y | Floor number |
| furnish_mail_street_name | varchar(32) | N | Street name |
| furnish_mail_unit_no | varchar(5) | Y | Unit number |
| furnish_mail_bldg_name | varchar(65) | Y | Building name |
| furnish_mail_postal_code | varchar(6) | N | Postal code |
| furnish_tel_code | varchar(4) | Y | Country code |
| furnish_tel_no | varchar(12) | Y | Contact number |
| furnish_email_addr | varchar(320) | Y | Email address |
| owner_driver_indicator | varchar(1) | N | D=Driver, H=Hirer |
| hirer_owner_relationship | varchar(1) | N | F/E/L/O |
| others_relationship_desc | varchar(15) | N | If relationship = O |
| ques_one_ans | varchar(32) | N | Question 1 answer |
| ques_two_ans | varchar(32) | N | Question 2 answer |
| ques_three_ans | varchar(32) | Y | Question 3 answer |
| rental_period_from | datetime2(7) | Y | If relationship = L |
| rental_period_to | datetime2(7) | Y | If relationship = L |
| owner_name | varchar(66) | N | Submitter name |
| owner_id_no | varchar(12) | N | Submitter ID |
| owner_tel_code | varchar(4) | Y | Submitter country code |
| owner_tel_no | varchar(20) | Y | Submitter contact |
| owner_email_addr | varchar(320) | Y | Submitter email |
| corppass_staff_name | varchar(66) | Y | Corppass representative |
| status | varchar(1) | N | P/S/A/R |
| is_sync | varchar(1) | N | N (default) / Y |
| reason_for_review | varchar(255) | Y | Auto-approval failure reason |
| remarks | varchar(200) | Y | Officer remarks |
| cre_date | datetime2(7) | N | Creation timestamp |
| cre_user_id | varchar(50) | N | ocmsez_app_conn |
| upd_date | datetime2(7) | Y | Update timestamp |
| upd_user_id | varchar(50) | Y | ocmsez_app_conn |

---

## 4. Status Codes

| Status | Code | Description | Next Status |
|--------|------|-------------|-------------|
| Pending | P | Submission pending approval | S, A, R |
| Resubmission | S | Requires resubmission | P, A, R |
| Approved | A | Furnishing approved (Final) | - |
| Rejected | R | Furnishing rejected (Final) | - |

---

## 5. Error Codes

| Code | HTTP | Message | Scenario | User Action |
|------|------|---------|----------|-------------|
| SUCCESS | 200 | Operation successful | Normal flow | None |
| NO_RECORDS | 200 | No records found | No notices/submissions for user | Informational |
| OFFENDER_IS_DRIVER | 200 | Cannot furnish - offender is driver | User is already the driver | Pay the notice |
| AUTH_FAILED | 401 | Authentication failed | Token expired/invalid | Re-login |
| VALIDATION_FAILED | 400 | Validation failed | Input validation error | Fix input |
| INVALID_ID | 400 | Invalid ID number | ID format validation failed | Correct ID |
| MISSING_FIELD | 400 | Required field missing | Mandatory field not provided | Fill field |
| NOTICE_NOT_FURNISHABLE | 400 | Notice not furnishable | Notice status changed | Refresh list |
| SUBMISSION_FAILED | 500 | Submission unsuccessful | Backend processing error | Retry |

---

## 6. Integration Points

| System | Direction | Purpose | Mechanism |
|--------|-----------|---------|-----------|
| Singpass | Inbound | Individual authentication | OAuth 2.0 + OIDC |
| Corppass | Inbound | Company authentication | OAuth 2.0 + OIDC |
| eService Portal | Bidirectional | Frontend communication | REST API |
| OCMS Intranet | Outbound (sync) | Data synchronization for approval | Scheduled job (is_sync flag) |

### 6.1 Sync Mechanism to Intranet

**Type:** Scheduled batch job (not real-time API)

**Process:**
1. Job runs every X minutes (configurable)
2. Query `eocms_furnish_application` WHERE `is_sync` = 'N'
3. Push records to Intranet database
4. Update `is_sync` = 'Y' after successful sync
5. Intranet staff can then process approvals

**Note:** Sync is one-way (Internet → Intranet). Approval status sync back is separate job.

---

## 7. Eligibility by Auth Source

### 7.1 Singpass (Individual)

| Scenario | Eligible | Notes |
|----------|----------|-------|
| Individual is Owner | Yes | Can furnish to driver/hirer |
| Individual is Driver | No | Returns OFFENDER_IS_DRIVER |
| ID types allowed | NRIC, FIN | - |

### 7.2 Corppass (Company)

| Scenario | Eligible | Notes |
|----------|----------|-------|
| Company is Owner | Yes | Can furnish to driver/hirer |
| Authorized representative | Yes | Uses company UEN |
| ID types allowed | UEN | Company registration |

---

## 8. Edge Cases

| Scenario | Handling |
|----------|----------|
| User submits same notice twice simultaneously | First submission wins, second gets NOTICE_NOT_FURNISHABLE |
| Notice status changes during form fill | Re-verify before insert, return error if not furnishable |
| Token expires during long form fill | Return AUTH_FAILED, user re-authenticates |
| Multiple notices in single submission, one invalid | Reject entire submission, return list of invalid notices |
| DB connection timeout | Return SUBMISSION_FAILED with retry suggestion |
| Sync job fails | Records remain is_sync='N', retry in next batch |

---

## 9. Assumptions Log

| # | Assumption | Rationale | Status |
|---|------------|-----------|--------|
| 1 | All APIs use POST method | Yi Jie standard - no GET for data retrieval | Confirmed |
| 2 | 6 months lookback for pending/approved | Based on Functional Flow query conditions | Confirmed |
| 3 | Contact number is optional | Based on FD field requirements | Confirmed |
| 4 | Owner details not shown in success response | V1.3 FD change | Confirmed |
| 5 | Singpass provides ID after authentication | Standard SPCP flow | Confirmed |
| 6 | Pagination default limit = 10 | Standard practice | Assumption |
| 7 | Sequence format = FAYYYYMMDDnnnn | Common pattern | Assumption |
| 8 | Sync job runs every 5 minutes | To be confirmed with infra | Assumption |

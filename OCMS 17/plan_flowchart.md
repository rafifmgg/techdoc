# Flowchart Plan - OCMS 17 Temporary Suspension

**Document Information**
- Version: 1.1
- Date: 12/01/2026
- Source: OCMS 17 Functional Document, OCMS 18 Reference Flow

**Important Notes on DB Fields (Updated 12/01/2026):**
- `reason_source` field controls source authorization: OCMS_FE, PLUS, OCMS_BE
- `no_of_days_for_revival` field stores suspension duration (NOT suspension_days)
- `reason_of_suspension` is the TS code field (NOT code)
- **Looping** and **Allowed Stages** are NOT stored in DB - hardcoded in application logic

---

## 1. Diagram Sections (Tabs)

Following OCMS 18 structure with 7 main sections:

| Section | Name | Description | Swimlanes | Estimated Process Boxes |
|---------|------|-------------|-----------|------------------------|
| 1 | TS_High_Level | Overview of TS function | 2 (Staff Portal, Backend) | 35-40 |
| 2 | UI_Side_Validation | Frontend validation logic | 1 (Staff Portal) | 40-45 |
| 3 | Backend_Side_Validation | Backend validation steps | 1 (Backend) | 40-45 |
| 4 | OCMS_Staff_Portal_Manual_TS | OCMS manual TS flow (MOST DETAILED) | 2 (Staff Portal, Backend) | 70-80 |
| 5 | PLUS_Staff_Portal_Manual_TS | PLUS manual TS flow | 2 (PLUS Portal, Backend) | 50-60 |
| 6 | Auto_TS_Triggers | Backend auto TS scenarios | 1 (Backend Cron) | 35-40 |
| 7 | Looping_TS | CLV and HST looping logic | 1 (Backend Cron) | 30-35 |

**Total Tabs:** 7
**Total Process Boxes:** ~300-350 (very detailed like OCMS18)

---

## 2. Swimlane Colors (Follow OCMS18 Standard)

| Swimlane | Color | Hex | Usage |
|----------|-------|-----|-------|
| Staff Portal | Purple | #e1d5e7 | OCMS Staff Portal Frontend |
| PLUS Portal | Purple | #e1d5e7 | PLUS Staff Portal Frontend |
| OCMS Admin API Intranet | Green | #d5e8d4 | Backend API (Intranet) |
| OCMS Admin API Internet | Green | #d5e8d4 | Backend API (Internet) |
| Backend Cron Service | Yellow | #fff2cc | Auto TS / Looping TS |

---

## 3. Section 1: TS_High_Level

### 3.1 Purpose
Overview of all TS modes and high-level flow.

### 3.2 Swimlanes
- swim_fe: Staff Portal
- swim_be: OCMS Admin API Intranet

### 3.3 Process Steps

| Step ID | Type | Description | Next | API/DB Call | Yellow Box ID |
|---------|------|-------------|------|-------------|---------------|
| fe_start | Start | User access Staff Portal | fe_p1 | - | - |
| fe_p1 | Process | Navigate to Search Notice Page | fe_d1 | - | - |
| fe_d1 | Decision | Apply TS via which mode? | fe_p2_manual, fe_p2_plus, be_p1_auto | - | - |
| fe_p2_manual | Process | Select OCMS Staff Manual TS | fe_p3 | - | - |
| fe_p2_plus | Process | PLUS Staff selects PLUS Manual TS | plus_flow | - | - |
| be_p1_auto | Process | Backend Auto TS triggered | auto_flow | - | - |
| fe_p3 | Process | Search and select notices | fe_api1 | - | - |
| fe_api1 | Process | Call API to search notices | be_p4 | POST /validoffencenoticelist | api1 |
| be_p4 | Process | Backend validates and retrieves notices | be_db1 | - | - |
| be_db1 | Process | Query ocms_valid_offence_notice | fe_p5 | SELECT... | be_sql1 |
| fe_p5 | Process | Display search results | fe_p6 | - | - |
| fe_p6 | Process | User selects TS operation | fe_api2 | - | - |
| fe_api2 | Process | Call API to get TS codes | be_p7 | POST /suspensionreasonlist | api2 |
| be_p7 | Process | Backend retrieves TS codes | be_db2 | - | - |
| be_db2 | Process | Query ocms_suspension_reason | fe_p8 | SELECT... | be_sql2 |
| fe_p8 | Process | User selects TS code and enters remarks | fe_p9 | - | - |
| fe_p9 | Process | User clicks Apply TS button | fe_d2 | - | - |
| fe_d2 | Decision | Confirm Apply TS? | fe_api3, fe_end | - | - |
| fe_api3 | Process | Call API to apply TS | be_p10 | POST /staff-apply-suspension | api3 |
| be_p10 | Process | Backend validates TS request | be_d1 | - | - |
| be_d1 | Decision | Validation passed? | be_db3, be_err1 | - | - |
| be_db3 | Process | Update ocms_valid_offence_notice | be_db4 | UPDATE... | be_sql3 |
| be_db4 | Process | Insert ocms_suspended_notice | fe_p11 | INSERT... | be_sql4 |
| fe_p11 | Process | Display success message | fe_end | - | - |
| be_err1 | Process | Return error response | fe_p12 | - | be_api_err1 |
| fe_p12 | Process | Display error message | fe_end | - | - |
| fe_end | End | End | - | - | - |

### 3.4 Yellow Boxes (API/DB Operations)

| Box ID | Type | Content |
|--------|------|---------|
| api1 | API Call | POST /validoffencenoticelist<br>{ "$skip": 0, "$limit": 10, ... } |
| api2 | API Call | POST /suspensionreasonlist<br>{ "suspensionType": "TS", "status": "A" } |
| api3 | API Call | POST /staff-apply-suspension<br>{ "noticeNo": ["..."], "reasonOfSuspension": "ACR", ... } |
| be_sql1 | SQL Query | SELECT von.notice_no, von.vehicle_no, von.last_processing_stage,<br>       von.suspension_type, sn.reason_of_suspension, von.amount_paid<br>FROM ocms_valid_offence_notice von<br>LEFT JOIN ocms_suspended_notice sn ON von.notice_no = sn.notice_no<br>  AND sn.sr_no = (SELECT MAX(sr_no) FROM ocms_suspended_notice WHERE notice_no = von.notice_no)<br>WHERE von.notice_no IN (:noticeNo)<br><br>Note: reason_of_suspension from child table (latest suspension) |
| be_sql2 | SQL Query | SELECT reason_of_suspension, description, no_of_days_for_revival,<br>       reason_source, status<br>FROM ocms_suspension_reason<br>WHERE suspension_type = 'TS'<br>  AND status = 'A'<br>  AND reason_source = 'OCMS_FE' |
| be_sql3 | SQL Update | UPDATE ocms_valid_offence_notice<br>SET suspension_type = 'TS',<br>    -- Note: reason_of_suspension stored in ocms_suspended_notice, not here<br>    due_date_of_revival = CURRENT_TIMESTAMP + INTERVAL ? DAY,<br>    is_sync = 'N',<br>    upd_date = CURRENT_TIMESTAMP<br>WHERE notice_no = ? |
| be_sql4 | SQL Insert | INSERT INTO ocms_suspended_notice<br>(notice_no, sr_no, suspension_source,<br> suspension_type, reason_of_suspension,<br> officer_authorising_suspension,<br> due_date_of_revival, suspension_remarks,<br> date_of_suspension, cre_date, cre_user_id,<br> process_indicator)<br>VALUES (?, nextval, 'OCMS',<br> 'TS', ?, ?, CURRENT_TIMESTAMP + INTERVAL ? DAY, ?,<br> CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, 'manual') |
| be_api_err1 | Error Response | { "appCode": "OCMS-4002",<br>  "message": "TS Code cannot be applied" } |

---

## 4. Section 2: UI_Side_Validation

### 4.1 Purpose
Frontend validation before sending request to backend.

### 4.2 Swimlanes
- swim_fe: Staff Portal

### 4.3 Process Steps with Decision Points

| Step ID | Type | Description | Validation Check | Error Action |
|---------|------|-------------|------------------|--------------|
| val_start | Start | User initiates TS operation | - | - |
| val_p1 | Process | Check user permission | Has TS permission? | Hide TS option |
| val_d1 | Decision | Permission granted? | YES: Continue, NO: Hide option | - |
| val_p2 | Process | Check notice selection | At least 1 selected? Max 10? | Show error |
| val_d2 | Decision | Selection valid? | YES: Continue, NO: Error | - |
| val_p3 | Process | Check TS code selection | Code selected? | Show error |
| val_d3 | Decision | Code selected? | YES: Continue, NO: Error | - |
| val_p4 | Process | Check suspension remarks | Max 200 chars? | Show error |
| val_d4 | Decision | Remarks valid? | YES: Continue, NO: Error | - |
| val_p5 | Process | Check notice eligibility (from backend data) | - | - |
| val_p6 | Process | Check last_processing_stage | In allowed stages? | Mark ineligible |
| val_p7 | Process | Check existing suspension_type | Can apply TS? | Mark ineligible |
| val_d5 | Decision | All notices eligible? | ALL: Continue, SOME: Warn, NONE: Error | - |
| val_p8 | Process | If some ineligible, show warning | - | - |
| val_d6 | Decision | Proceed with eligible only? | YES: Continue, NO: Cancel | - |
| val_p9 | Process | Enable Submit button | - | - |
| val_end | End | Validation complete | - | - |

**Note:** All validation uses data received from backend API (validoffencenoticelist).

---

## 5. Section 3: Backend_Side_Validation

### 5.1 Purpose
Server-side validation when receiving TS request.

### 5.2 Swimlanes
- swim_be: OCMS Admin API Intranet

### 5.3 Process Steps (38+ process boxes)

| Step ID | Type | Description | Check | Error Code |
|---------|------|-------------|-------|------------|
| be_val_start | Start | Receive TS request | - | - |
| be_val_p1 | Process | Validate JWT token | Valid? | OCMS-4001 |
| be_val_d1 | Decision | Token valid? | YES: Continue, NO: Error | - |
| be_val_p2 | Process | Check user permission | Has TS permission? | OCMS-4001 |
| be_val_d2 | Decision | Permission granted? | YES: Continue, NO: Error | - |
| be_val_p3 | Process | Validate payload - noticeNo | Not empty? Array? | OCMS-4000 |
| be_val_p4 | Process | Validate payload - suspensionType | Must be "TS"? | OCMS-4000 |
| be_val_p5 | Process | Validate payload - reasonOfSuspension | Not empty? | OCMS-4000 |
| be_val_p6 | Process | Query ocms_suspension_reason | Code exists? | be_sql_val1 |
| be_val_sql1 | Process | Check TS code in database | - | OCMS-4000 |
| be_val_d3 | Decision | Code exists and active? | YES: Continue, NO: Error | - |
| be_val_p7 | Process | Check source authorization | OCMS/PLUS/Backend allowed? | - |
| be_val_d4 | Decision | Source authorized? | YES: Continue, NO: Error (OCMS-4000) | - |
| be_val_p8 | Process | Query ocms_valid_offence_notice | Notice exists? | be_sql_val2 |
| be_val_sql2 | Process | Check notice in database | - | OCMS-4004 |
| be_val_d5 | Decision | Notice exists? | YES: Continue, NO: Error | - |
| be_val_p9 | Process | Check notice status | Cancelled? Void? | - |
| be_val_d6 | Decision | Notice valid? | YES: Continue, NO: Error (OCMS-4002) | - |
| be_val_p10 | Process | Check last_processing_stage | In allowed stages? | - |
| be_val_d7 | Decision | Stage allowed? | YES: Continue, NO: Error (OCMS-4002) | - |
| be_val_p11 | Process | Check existing suspension | PS or TS? | - |
| be_val_d8 | Decision | Has suspension? | NO: Allow, PS: Check exception, TS: Revive+New | - |
| be_val_p12 | Process | If PS, check if code in [DIP, MID, FOR, RIP, RP2] | - | - |
| be_val_d9 | Decision | PS exception? | YES: Allow, NO: Error (OCMS-4002) | - |
| be_val_p13 | Process | If TS, revive existing with 'TSR' | UPDATE ocms_suspended_notice | be_sql_val3 |
| be_val_p14 | Process | All validations passed | Proceed to apply TS | - |
| be_val_end | End | Validation complete | - | - |

### 5.4 Yellow Boxes

| Box ID | Type | Content |
|--------|------|---------|
| be_sql_val1 | SQL Query | SELECT reason_of_suspension, description, no_of_days_for_revival,<br>       reason_source, status<br>FROM ocms_suspension_reason<br>WHERE reason_of_suspension = ? AND suspension_type = 'TS' |
| be_sql_val2 | SQL Query | SELECT von.notice_no, von.last_processing_stage,<br>       von.suspension_type, sn.reason_of_suspension<br>FROM ocms_valid_offence_notice von<br>LEFT JOIN ocms_suspended_notice sn ON von.notice_no = sn.notice_no<br>  AND sn.sr_no = (SELECT MAX(sr_no) FROM ocms_suspended_notice WHERE notice_no = von.notice_no)<br>WHERE von.notice_no = ?<br><br>Note: reason_of_suspension from child table |
| be_sql_val3 | SQL Update | UPDATE ocms_suspended_notice<br>SET date_of_revival = CURRENT_TIMESTAMP,<br>    revival_reason = 'TSR',<br>    upd_date = CURRENT_TIMESTAMP<br>WHERE notice_no = ? AND date_of_revival IS NULL |

---

## 6. Section 4: OCMS_Staff_Portal_Manual_TS (MOST DETAILED)

### 6.1 Purpose
Very detailed step-by-step flow of OCMS Staff applying TS (75+ process boxes like OCMS18).

### 6.2 Swimlanes
- swim_fe: Staff Portal
- swim_be: OCMS Admin API Intranet

### 6.3 Detailed Process Steps (70-80 boxes)

**Phase 1: Search and Select Notices (15 boxes)**

| Step ID | Description | API/DB |
|---------|-------------|--------|
| p4_start | User logged in to Staff Portal | - |
| p4_fe_p1 | Navigate to Search Notice page | - |
| p4_fe_p2 | Enter search criteria (notice no, vehicle no, etc.) | - |
| p4_fe_p3 | Click Search button | - |
| p4_fe_api1 | Portal calls validoffencenoticelist API | api1 |
| p4_be_p1 | Backend receives search request | - |
| p4_be_p2 | Validate search parameters | - |
| p4_be_d1 | Parameters valid? | Decision |
| p4_be_sql1 | Query ocms_valid_offence_notice with filters | be_sql1 |
| p4_be_p3 | Format response with pagination | - |
| p4_be_api_resp1 | Return search results | api_resp1 |
| p4_fe_p4 | Display search results in table | - |
| p4_fe_p5 | User reviews notice list | - |
| p4_fe_p6 | User selects one or more notices (checkbox) | - |
| p4_fe_p7 | Selected count displayed | - |

**Phase 2: Initiate TS Operation (10 boxes)**

| Step ID | Description | API/DB |
|---------|-------------|--------|
| p4_fe_p8 | User clicks "Apply Operation" dropdown | - |
| p4_fe_d1 | User has TS permission? | Decision (from user session) |
| p4_fe_p9 | If no permission: Hide "Apply TS" option | - |
| p4_fe_p10 | If has permission: Show "Apply TS" option | - |
| p4_fe_p11 | User selects "Apply TS" from dropdown | - |
| p4_fe_p12 | User clicks "Select" button | - |
| p4_fe_p13 | Portal validates selection count (1-10) | - |
| p4_fe_d2 | Count valid? | Decision |
| p4_fe_p14 | Detect Last Processing Stage of selected notices | - |
| p4_fe_p15 | Check if all notices at allowed stages | - |

**Phase 3: Suspension Detail Page (15 boxes)**

| Step ID | Description | API/DB |
|---------|-------------|--------|
| p4_fe_p16 | Redirect to Suspension Detail page | - |
| p4_fe_p17 | Display form with Suspension Type = TS (readonly) | - |
| p4_fe_api2 | Portal calls suspensionreasonlist API | api2 |
| p4_be_p4 | Backend receives request for TS codes | - |
| p4_be_sql2 | Query ocms_suspension_reason for TS codes | be_sql2 |
| p4_be_p5 | Filter codes by OCMS staff manual allowed | - |
| p4_be_api_resp2 | Return TS code list | api_resp2 |
| p4_fe_p18 | Populate TS Code dropdown | - |
| p4_fe_p19 | Display selected notice(s) information | - |
| p4_fe_p20 | OIC selects TS Code from dropdown | - |
| p4_fe_p21 | System shows code description and suspension days | - |
| p4_fe_p22 | OIC enters Suspension Remarks (optional, max 200) | - |
| p4_fe_p23 | Character count displayed (x/200) | - |
| p4_fe_p24 | Review suspension details summary | - |
| p4_fe_p25 | OIC clicks "Apply TS" button | - |

**Phase 4: Confirmation and Submit (10 boxes)**

| Step ID | Description | API/DB |
|---------|-------------|--------|
| p4_fe_d3 | Confirm Apply TS? (Popup) | Decision |
| p4_fe_p26 | If No: Return to form | - |
| p4_fe_p27 | If Yes: Disable button, show loading | - |
| p4_fe_p28 | Prepare API request payload | - |
| p4_fe_api3 | Portal calls staff-apply-suspension API | api3 |
| p4_be_p6 | Backend receives TS application request | - |
| p4_be_p7 | Validate authentication (JWT) | - |
| p4_be_p8 | Validate user permission | - |
| p4_be_p9 | Check if user has TS permission in database | - |
| p4_be_d2 | Permission granted? | Decision |

**Phase 5: Backend Validation (15 boxes)**

| Step ID | Description | API/DB |
|---------|-------------|--------|
| p4_be_p10 | Validate payload mandatory fields | - |
| p4_be_p11 | Validate payload format | - |
| p4_be_sql3 | Query TS code rules from ocms_suspension_reason | be_sql3 |
| p4_be_d3 | Code exists and active? | Decision |
| p4_be_p12 | Check source authorization (OCMS staff manual allowed) | - |
| p4_be_d4 | Source authorized? | Decision (OCMS-4000 if No) |
| p4_be_sql4 | Query notice details from ocms_valid_offence_notice | be_sql4 |
| p4_be_d5 | Notice exists? | Decision (OCMS-4004 if No) |
| p4_be_p13 | Check notice status (not cancelled, not void) | - |
| p4_be_d6 | Status valid? | Decision (OCMS-4002 if No) |
| p4_be_p14 | Check last_processing_stage vs allowed stages (hardcoded in app) | - |
| p4_be_d7 | Stage allowed? | Decision (OCMS-4002 if No) |
| p4_be_p15 | Check existing suspension | - |
| p4_be_d8 | Has PS or TS? | Decision |
| p4_be_p16 | If TS: Revive existing, If PS: Check exception | - |

**Phase 6: Apply TS (Database Operations) (15 boxes)**

| Step ID | Description | API/DB |
|---------|-------------|--------|
| p4_be_p17 | All validations passed | - |
| p4_be_p18 | Calculate due_date_of_revival (current + no_of_days_for_revival) | - |
| p4_be_p19 | Start database transaction | - |
| p4_be_sql5 | UPDATE ocms_valid_offence_notice (Step 1) | be_sql5 |
| p4_be_p20 | Set suspension_type = 'TS' | - |
| p4_be_p21 | Set reason_of_suspension = selected code | - |
| p4_be_p22 | Set due_date_of_revival = calculated date | - |
| p4_be_p23 | Set upd_date = CURRENT_TIMESTAMP | - |
| p4_be_sql6 | INSERT INTO ocms_suspended_notice (Step 2) | be_sql6 |
| p4_be_p24 | Generate sr_no using nextval | - |
| p4_be_p25 | Insert all suspension details | - |
| p4_be_p26 | Set process_indicator = 'manual' | - |
| p4_be_p27 | Set cre_user_id = ocmsiz_app_conn | - |
| p4_be_p28 | Commit transaction | - |
| p4_be_d9 | Transaction successful? | Decision |

**Phase 7: Response and Display Result (10 boxes)**

| Step ID | Description | API/DB |
|---------|-------------|--------|
| p4_be_p29 | If success: Build success response | - |
| p4_be_api_resp3 | Return success response with sr_no | api_resp3 |
| p4_fe_p29 | Portal receives API response | - |
| p4_fe_p30 | Parse response (totalProcessed, successCount, errorCount) | - |
| p4_fe_d4 | All notices successful? | Decision |
| p4_fe_p31 | If all success: Show success message with count | - |
| p4_fe_p32 | If partial: Show success + error details | - |
| p4_fe_p33 | Display detailed results table | - |
| p4_fe_p34 | Refresh notice list | - |
| p4_fe_end | End | - |

### 6.4 Error Handling (5 boxes)

| Step ID | Description | API/DB |
|---------|-------------|--------|
| p4_be_err1 | Validation failed at any point | - |
| p4_be_p_err2 | Build error response | be_api_err1 |
| p4_be_api_err_resp | Return error response | - |
| p4_fe_p_err1 | Portal receives error | - |
| p4_fe_p_err2 | Display error message to user | - |

### 6.5 Yellow Boxes (Full API/SQL Content)

| Box ID | Type | Full Content |
|--------|------|--------------|
| api1 | API Call | POST /validoffencenoticelist<br>{<br>  "$skip": 0,<br>  "$limit": 10,<br>  "$field": "noticeNo, vehicleNo, lastProcessingStage, suspensionType, reasonOfSuspension",<br>  "noticeNo": "500500303J",<br>  "lastProcessingStage": "RD1"<br>} |
| be_sql1 | SQL Query | SELECT von.notice_no, von.vehicle_no, von.offence_notice_type,<br>       von.last_processing_stage, von.next_processing_date,<br>       von.suspension_type, von.amount_paid,<br>       sn.reason_of_suspension<br>FROM ocms_valid_offence_notice von<br>LEFT JOIN ocms_suspended_notice sn ON von.notice_no = sn.notice_no<br>  AND sn.sr_no = (SELECT MAX(sr_no) FROM ocms_suspended_notice WHERE notice_no = von.notice_no)<br>WHERE von.notice_no LIKE '%' || :noticeNo || '%'<br>  AND von.last_processing_stage IN (:stages)<br>ORDER BY von.notice_no<br>LIMIT :limit OFFSET :skip<br><br>Note: reason_of_suspension from child table (sn), amount_paid from parent (von) |
| api_resp1 | API Response | {<br>  "appCode": "OCMS-2000",<br>  "message": "Success",<br>  "data": [{<br>    "noticeNo": "500500303J",<br>    "vehicleNo": "SBA1234A",<br>    "lastProcessingStage": "RD1",<br>    "suspensionType": null<br>  }],<br>  "pagination": {<br>    "totalRecords": 1<br>  }<br>} |
| api2 | API Call | POST /suspensionreasonlist<br>{<br>  "suspensionType": "TS",<br>  "status": "A"<br>} |
| be_sql2 | SQL Query | SELECT reason_of_suspension, description, no_of_days_for_revival,<br>       reason_source, status<br>FROM ocms_suspension_reason<br>WHERE suspension_type = 'TS'<br>  AND status = 'A'<br>  AND reason_source = 'OCMS_FE'<br>ORDER BY reason_of_suspension |
| api_resp2 | API Response | {<br>  "appCode": "OCMS-2000",<br>  "message": "Success",<br>  "data": [{<br>    "code": "ACR",<br>    "description": "ACRA",<br>    "suspensionDays": 21,<br>    "looping": false<br>  }, {<br>    "code": "FPL",<br>    "description": "Furnished Particulars Late",<br>    "suspensionDays": 14,<br>    "looping": false<br>  }]<br>} |
| api3 | API Call | POST /staff-apply-suspension<br>{<br>  "noticeNo": ["500500303J"],<br>  "suspensionType": "TS",<br>  "reasonOfSuspension": "ACR",<br>  "suspensionRemarks": "ACRA investigation",<br>  "officerAuthorisingSuspension": "JOHNLEE"<br>} |
| be_sql3 | SQL Query | SELECT reason_of_suspension, description, no_of_days_for_revival,<br>       reason_source, status<br>FROM ocms_suspension_reason<br>WHERE reason_of_suspension = :code<br>  AND suspension_type = 'TS'<br>  AND status = 'A'<br>  AND reason_source = 'OCMS_FE' |
| be_sql4 | SQL Query | SELECT von.notice_no, von.last_processing_stage,<br>       von.next_processing_date, von.suspension_type, von.amount_paid,<br>       sn.reason_of_suspension<br>FROM ocms_valid_offence_notice von<br>LEFT JOIN ocms_suspended_notice sn ON von.notice_no = sn.notice_no<br>  AND sn.sr_no = (SELECT MAX(sr_no) FROM ocms_suspended_notice WHERE notice_no = von.notice_no)<br>WHERE von.notice_no = :noticeNo |
| be_sql5 | SQL Update | UPDATE ocms_valid_offence_notice<br>SET suspension_type = 'TS',<br>    due_date_of_revival = CURRENT_TIMESTAMP + INTERVAL :days DAY,<br>    is_sync = 'N',<br>    upd_date = CURRENT_TIMESTAMP,<br>    upd_user_id = :userId<br>WHERE notice_no = :noticeNo<br><br>Note: reason_of_suspension NOT in this table - stored in ocms_suspended_notice (be_sql6) |
| be_sql6 | SQL Insert | INSERT INTO ocms_suspended_notice<br>(notice_no, sr_no, suspension_source, suspension_type,<br> reason_of_suspension, officer_authorising_suspension,<br> due_date_of_revival, suspension_remarks,<br> date_of_suspension, cre_date, cre_user_id,<br> process_indicator)<br>VALUES (:noticeNo, nextval, 'OCMS', 'TS',<br> :code, :officer,<br> CURRENT_TIMESTAMP + INTERVAL :days DAY, :remarks,<br> CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ocmsiz_app_conn',<br> 'manual')<br><br>Note: sr_no uses nextval for sequence |
| api_resp3 | API Success | {<br>  "totalProcessed": 1,<br>  "successCount": 1,<br>  "errorCount": 0,<br>  "results": [{<br>    "noticeNo": "500500303J",<br>    "srNo": 1234,<br>    "dueDateOfRevival": "2025-04-12T19:00:02",<br>    "appCode": "OCMS-2000",<br>    "message": "TS Success"<br>  }]<br>} |
| be_api_err1 | API Error | {<br>  "totalProcessed": 1,<br>  "successCount": 0,<br>  "errorCount": 1,<br>  "results": [{<br>    "noticeNo": "500500303J",<br>    "appCode": "OCMS-4002",<br>    "message": "TS Code cannot be applied due to Last Processing Stage"<br>  }]<br>} |

---

## 7. Section 5: PLUS_Staff_Portal_Manual_TS

Similar structure to Section 4, but:
- Use PLUS Portal swimlane (purple)
- Use OCMS Admin API Internet swimlane (green)
- API endpoint: POST /suspension/temporary (external API)
- Only 6 TS codes available (APE, APP, CCE, MS, PRI, RED)
- Check reason_source = 'PLUS' in ocms_suspension_reason
- Use ocmsez_app_conn for audit user

**Estimated:** 50-60 process boxes

---

## 8. Section 6: Auto_TS_Triggers

### 8.1 Purpose
Backend automatically applies TS when exception scenarios detected.

### 8.2 Swimlanes
- swim_cron: Backend Cron Service

### 8.3 Auto TS Scenarios (8 codes)

| Scenario | TS Code | Trigger | Process Boxes |
|----------|---------|---------|---------------|
| ACRA sync exception | ACR | ACRA cron detects data exception | 5-6 boxes |
| MHA/DataHive exception | NRO | MHA sync detects exception | 5-6 boxes |
| ROV exception | ROV | ROV sync detects exception | 5-6 boxes |
| House Tenants exception | HST | HST sync detects exception | 5-6 boxes |
| Pending driver furnish | PDP | Driver furnish check detects pending | 5-6 boxes |
| Partially matched | PAM | Offender match detects partial match | 5-6 boxes |
| System error | SYS | System detects processing error | 5-6 boxes |
| Classified Vehicles | CLV | CVL check detects classified vehicle | 5-6 boxes |

**Total:** 30-35 process boxes

---

## 9. Section 7: Looping_TS

### 9.1 Purpose
Auto revival and re-application of looping TS codes (CLV, HST).

### 9.2 Swimlanes
- swim_cron: Backend Cron Service

### 9.3 Process Steps (30-35 boxes)

| Step ID | Description | DB Operation |
|---------|-------------|--------------|
| loop_start | Auto revival cron runs (daily) | - |
| loop_p1 | Query notices with due_date_of_revival reached | SELECT... |
| loop_p2 | For each notice with TS | LOOP |
| loop_d1 | Is looping code (CLV or HST)? | Decision |
| loop_p3 | If No: Normal revival | UPDATE (set revival date) |
| loop_p4 | If Yes: Looping TS | - |
| loop_p5 | Step 1: Revive existing TS | UPDATE (set revival, reason='Auto Revival - Looping TS') |
| loop_p6 | Step 2: Create new TS with same code | INSERT (new sr_no using nextval) |
| loop_p7 | Set new due_date_of_revival | CURRENT + no_of_days_for_revival |
| loop_p8 | Ensure suspension_type remains 'TS' | - |
| loop_p9 | Log looping TS action | - |
| loop_end | End | - |

**Yellow Boxes:**
- SQL for revival: UPDATE ocms_suspended_notice...
- SQL for re-apply: INSERT INTO ocms_suspended_notice...

---

## Summary

**Total Sections:** 7
**Total Process Boxes:** ~300-350 (very detailed like OCMS18 Tab 5 with 75 boxes)
**Total Yellow Boxes (API/SQL):** ~50-60

**Key Mappings:**
- Search Process → API: validoffencenoticelist
- Get TS Codes → API: suspensionreasonlist
- Apply TS → API: staff-apply-suspension
- Each validation step → SQL queries
- Each database update → UPDATE/INSERT SQL with nextval

**Next Step:**
Create Technical Flow OCMS17.drawio with 7 tabs, following this plan exactly.

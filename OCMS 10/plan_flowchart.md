# Flowchart Planning Document - OCMS 10: Advisory Notices Processing

## Document Information
- **Version:** 2.0
- **Date:** 2026-01-22
- **Source Documents:**
  - Functional Document: v1.2_OCMS 10_Functional Document.md
  - Functional Flow: OCMS_10_Functional_Flow.drawio
  - plan_api.md
  - plan_condition.md
  - Backend Code: ocmsadminapi, ocmsadmincron
- **Reference Guidelines:** Docs/guidelines/plan_flowchart.md

**Change Log:**
- v2.0 (2026-01-22): Added Type E exclusion (BA confirmed). Added Tab 3.5.1 for Resend eAN Cron (1400hrs). Added Tab 3.8.1 for ANS Letter Reconciliation Report. Updated Tab 3.1 with Type E skip logic. Added Manual PS-ANS by OIC to Tab 3.7. Added TS-ROV/TS-NRO/TS-HST handling to relevant tabs.
- v1.9 (2026-01-22): Major update - Aligned all flows with corrected Technical Document. Removed assumed template content from Tab 3.5 and 3.6. Updated data mappings to use actual tables (ocms_email_notification_records, ocms_sms_notification_records, ocms_an_letter). Added references to OCMS 3, OCMS 5, OCMS 21 documents.
- v1.8 (2026-01-21): Data Dictionary validation fixes - Tab 3.1.1: ocms_ans_exemption_rules → ocms_an_exemption_rules, effective_start_date → start_effective_date, effective_end_date → end_effective_date, rule_remark → rule_remarks, removed is_active clause. Tab 3.1.2: code_value → code, code_description → description, is_active = 'Y' → code_status = 'A'.
- v1.7 (2026-01-21): Simplified Tab 3.1.2 - Moved exclude 2pm notice logic from Code to SQL query (AND notice_no != newNotice.notice_no). Removed code filter step.

---

## Section 2: High-Level Flow

### Tab 2.1: High-Level AN Processing Overview

#### Process Overview

| Attribute | Value |
|-----------|-------|
| Process Name | High-Level Advisory Notices Processing |
| Section | 2.1 |
| Trigger | Notice creation from multiple sources (REPCCS, CES, Staff Portal, PLUS Portal) |
| Frequency | Real-time |
| Systems Involved | Frontend (Staff Portal), Backend (OCMS API), Database (Intranet/Internet), External Systems (LTA, DataHive, MHA, SLIFT, REPCCS, CES) |

#### Process Steps Table

| Step | Definition | Brief Description |
|------|-----------|-------------------|
| Notice Creation | Create notice record | Refer to Technical Document OCMS 5 |
| AN Verification | Check qualification | Refer to Section 2 |
| Send eNotification | Electronic notification | Refer to Section 5 |
| Send AN Letter | Physical letter | Refer to Section 6 |
| Suspend with PS-ANS | Create suspension | Create PS-ANS suspension record for qualified AN |
| End | Process complete | AN processing completed |

#### Swimlanes Definition

| Swimlane | Color Code | Components |
|----------|-----------|------------|
| Frontend | Blue (#dae8fc) | Staff Portal, PLUS Portal |
| Backend | Purple (#e1d5e7) | OCMS API, Business Logic, Validation Services |
| Database | Yellow (#fff2cc) | Intranet DB, Internet DB |
| External System | Green (#d5e8d4) | REPCCS, CES, LTA, DataHive, MHA, SLIFT |

---

## Section 3: Detailed Flows

### Tab 3.1: AN Verification - Qualification Check

#### Process Overview

| Attribute | Value |
|-----------|-------|
| Process Name | AN Qualification Check |
| Section | 3.1 |
| Trigger | Notice creation with offence_notice_type='O' and vehicle_type in [S,D,V,I]. **Note:** CES notices with an_flag='Y' skip this check and go directly to PS-ANS (per FD v1.2 Section 4.2). **Important:** Type E (Enforcement) notices are NOT eligible for ANS - skip AN qualification entirely and proceed to standard flow (BA confirmed 2026-01-22). |
| Frequency | Real-time (per notice creation) |
| Systems Involved | Backend (AdvisoryNoticeHelper), Database (Intranet) |

#### Process Steps Table

| Step | Definition | Brief Description |
|------|-----------|-------------------|
| Check Offense Type | Validate offense type | Offense type must be 'O' |
| Check Vehicle Type | Validate vehicle type | Vehicle registration type must be in [S, D, V, I]. |
| Check Exemption Rules | Exemption validation | Refer to Section 2.3 |
| Check Past Offense | 24-month window check | Refer to Section 2.4 |
| Return Qualified | Pass qualification | Return qualified=true with AN details |
| Return Not Qualified | Fail qualification | Return qualified=false with reason |
| End | Check complete | Qualification result returned |

#### Swimlanes Definition

| Swimlane | Color | Steps |
|----------|-------|-------|
| Backend | Purple (#e1d5e7) | All validation steps |
| Database | Yellow (#fff2cc) | Query exemption rules, Query past offenses |

---

### Tab 3.1.1: ANS Exemption Rule Code Check Sub-Flow

#### Process Overview

| Attribute | Value |
|-----------|-------|
| Process Name | ANS Exemption Rule Code Check |
| Section | 3.1.1 (Sub-flow of Tab 3.1) |
| Trigger | Called from AN Qualification Check (Tab 3.1) when checking exemption rules |
| Frequency | Real-time (per notice creation) |
| Systems Involved | Backend (OCMS API), Database (Intranet - ocms_an_exemption_rules) |

#### Process Steps Table

| Step | Definition | Brief Description |
|------|-----------|-------------------|
| Identify Rule Code | Get computer_rule_code | Identify new Notice's Offence Rule Code. |
| Query Exemption DB | Database query | Query ocms_an_exemption_rules to check if Rule Code is in exemption list |
| Record Found? | Decision | Check if exemption record found for this rule code |
| Compare Dates | Date validation | Compare Notice's offence_date with Exemption Rule's |
| Within Period? | Decision | If no, notice will eligible for ANS. If yes, next checking |
| Rule 20412? | Specific rule check | If no, check another rule. If yes, check composition amount |
| Amount $80? | Amount check | Check if composition_amount = $80. If no, notice is eligible for ANS. If yes, notice not eligible for ANS |
| Decision: Rule 11210? | Specific rule check | Continue check when rule not 20412. If no, notice not eligible for ANS. If yes, will continue check |
| Loading Bay? | Remarks check | If yes, notice not eligible for ANS. If no, will continue check |
| Handicap Lot? | Remarks check | If no, notice eligible for ANS. If yes, notice not eligible for ANS |
| End | Sub-flow complete | ANS Exemption Rule Code Check completed |

#### Database Operations

| Operation | Table | Query/Action |
|-----------|-------|--------------|
| SELECT | ocms_an_exemption_rules | SELECT rule_code, start_effective_date, end_effective_date, rule_remarks, composition_amount FROM ocms_an_exemption_rules WHERE rule_code = [notice.computer_rule_code] |

#### Swimlanes Definition

| Swimlane | Color Code | Components |
|----------|-----------|------------|
| Backend | Purple (#e1d5e7) | OCMS API |
| Database | Yellow (#fff2cc) | ocms_an_exemption_rules |

---

### Tab 3.1.2: Past Offences Check Sub-Flow

#### Process Overview

| Attribute | Value |
|-----------|-------|
| Process Name | Past Offences Check |
| Section | 3.1.2 (Sub-flow of Tab 3.1) |
| Trigger | Called from AN Qualification Check (Tab 3.1) after exemption check passes |
| Frequency | Real-time (per notice creation) |
| Systems Involved | Backend (OCMS API), Database (Intranet - ocms_valid_offence_notice, ocms_standard_code) |

#### Process Steps Table

| Step | Definition | Brief Description |
|------|-----------|-------------------|
| Query Past Offences | Database query | Query ocms_valid_offence_notice for offences in last 24 months |
| Any Record Found? | Past offence check | Check if any past offence records found |
| Query ANS PS Reasons | Get valid reasons | Query ocms_standard_code for active ANS PS Reasons |
| Batch Check | Suspension check | For each past offence, check if it is suspended with one of the ANS PS Reasons |
| All Suspended? | Decision | Check if ALL past Notices are suspended with ANS PS Reasons |
| Return Qualified | Qualified result | Notice QUALIFIES as AN |
| Return Not Qualified | Not qualified result | Notice does NOT QUALIFY as AN |
| End | Sub-flow complete | Past Offences Check completed |

#### Database Operations

| Operation | Table | Query/Action |
|-----------|-------|--------------|
| SELECT | ocms_valid_offence_notice | SELECT notice_no, notice_date_and_time, suspension_type, epr_reason_of_suspension FROM ocms_valid_offence_notice WHERE vehicle_no = [newNotice.vehicle_no] AND notice_date_and_time BETWEEN DATEADD(month, -24, GETDATE()) AND GETDATE() AND notice_no != [newNotice.notice_no] |
| SELECT | ocms_standard_code | SELECT code, description FROM ocms_standard_code WHERE reference_code = 'ANS_PS_Reason' AND code_status = 'A' |

#### ANS PS Reasons Reference

| Code | Description | Effect |
|------|-------------|--------|
| CAN | Cancelled | Offence cancelled - still allows AN qualification |
| CFA | Court Fine Adjusted | Court adjusted - still allows AN qualification |
| DBB | Double Booking | Duplicate notice - still allows AN qualification |
| VST | Vehicle Stolen | Vehicle was stolen - still allows AN qualification |

#### Swimlanes Definition

| Swimlane | Color Code | Components |
|----------|-----------|------------|
| Backend | Purple (#e1d5e7) | OCMS API |
| Database | Yellow (#fff2cc) | ocms_valid_offence_notice, ocms_standard_code |

---

### Tab 3.2: Receive AN Data from REPCCS

> **Note (FD v1.2 Section 4.2):** OCMS DISREGARDS the anFlag value sent by REPCCS. OCMS performs its own AN Qualification check for all REPCCS notices.

#### Process Overview

| Attribute | Value |
|-----------|-------|
| Process Name | Receive AN Data from REPCCS |
| Section | 3.2 |
| Trigger | REPCCS webhook call to /v1/repccsWebhook |
| Frequency | Real-time (event-driven) |
| Systems Involved | External System (REPCCS), Backend (OCMS API), Database (Intranet) |

#### Process Steps Table

| Step | Definition | Brief Description |
|------|-----------|-------------------|
| Receive Webhook Payload | Parse request | Receive and parse RepWebHookPayloadDto |
| Validate Mandatory Fields | Field validation | Check all mandatory fields present |
| Check Duplicate Notice | Duplicate detection | Check if notice number already exists |
| Detect Vehicle Type | Vehicle type detection | Apply special vehicle detection. |
| Generate Notice Number | Create notice ID | Generate notice number if not provided |
| Insert Notice Records | Create records | Insert into ocms_valid_offence_notice and ocms_offence_notice_detail |
| Check Double Booking | DBB detection | Refer to Technical Document OCMS 21 |
| Run AN Qualification | Qualification check | Refer to Section 2 |
| Update AN Flags | Mark as AN | Set an_flag='Y' if qualified |
| Return Success Response | Response to REPCCS | Return success message |
| Return Error Response | Error to REPCCS | Return error message |
| End | Webhook complete | REPCCS webhook processing finished |

#### API Specification

**Refer to OCMS 3 Technical Document section 1.1.3**

#### Data Mapping

**Refer to OCMS 3 Technical Document section 2.1**

#### Swimlanes Definition

| Swimlane | Color | Steps |
|----------|-------|-------|
| External System | Green (#d5e8d4) | Send webhook payload, Receive response |
| Backend | Purple (#e1d5e7) | Validate fields, Detect vehicle type, Run qualification, Return response |
| Database | Yellow (#fff2cc) | Check duplicate, Insert notice records, Update AN flags |

---

### Tab 3.3: Receive AN Data from CES

> **CRITICAL (FD v1.2 Section 4.2):** OCMS uses the anFlag value directly from CES. If CES sends an_flag='Y', OCMS creates the notice with an_flag='Y' and **immediately suspends with PS-ANS WITHOUT performing AN Qualification check**.

#### Process Overview

| Attribute | Value |
|-----------|-------|
| Process Name | Receive AN Data from CES (Certis) |
| Section | 3.3 |
| Trigger | CES webhook call to /v1/cesWebhook-create-notice |
| Frequency | Real-time (event-driven) |
| Systems Involved | External System (CES), Backend (OCMS API), Database (Intranet) |

#### Process Steps Table

| Step | Definition | Brief Description |
|------|-----------|-------------------|
| Receive Webhook Payload | Parse request | Receive and parse CesCreateNoticeDto |
| Validate Mandatory Fields | Field validation | Check all mandatory fields present |
| Validate Subsystem Label | Subsystem validation | Check subsystemLabel format. |
| Check Duplicate Notice | Duplicate detection | Check if notice number already exists |
| Validate Offense Rule Code | Rule code validation | Check if computer_rule_code exists in offense_rule_code table |
| Detect Vehicle Type | Vehicle type detection | Apply special vehicle detection |
| Check EHT/Certis AN Flag | EHT check | Checking if EHT identified as AN |
| Decision: EHT with AN Flag? | EHT AN check | Check if EHT subsystem with an_flag='Y' |
| Create PS-ANS Suspension | Immediate suspension | Create notice, trigger PS-ANS suspension, early return |
| Insert Notice Records | Create records | Insert into ocms_valid_offence_notice and ocms_offence_notice_detail |
| Run AN Qualification | Qualification check | Refer to section 2 |
| Update AN Flags | Mark as AN | Set an_flag='Y' if qualified |
| Return Success Response | Response to CES | Return success message |
| Return Error Response | Error to CES | Return error message |
| End | Webhook complete | CES webhook processing finished |

#### API Specification

**CES Webhook API**

| Field | Value |
| --- | --- |
| API Name | cesWebhook-create-notice |
| URL | POST /v1/cesWebhook-create-notice |
| Description | Webhook endpoint to receive notice data from CES (Certis) system |
| Method | POST |
| Header | `{ "Content-Type": "application/json", "API-Key": "[API_KEY]" }` |
| Payload | `{ "transactionId": "CESNOPO823000001G", "subsystemLabel": "3030", "anFlag": "N", "noticeNo": "823000001G", "vehicleNo": "MGC1234X", "computerRuleCode": "30301", "compositionAmount": "70,00", "noticeDateAndTime": "2026-01-11T14:30:00", "offenceNoticeType": "O", "vehicleCategory": "I", "vehicleRegistrationType": "S", "ppCode": "A002", "ppName": "ALBERT Road", "creUserId": "ocmsiz_app_conn", "creDate": "2026-01-11T14:30:00" }` |
| Response | `{ "data": { "appCode": "OCMS-2000", "message": "Resource created successfully" } }` |
| Response Failure (Validation) | `{ "data": { "appCode": "OCMS-4000", "message": "Invalid input format or failed validation" } }` |

#### Swimlanes Definition

| Swimlane | Color | Steps |
|----------|-------|-------|
| External System | Green (#d5e8d4) | Send webhook payload, Receive response |
| Backend | Purple (#e1d5e7) | Validate fields, Check EHT flag, Run qualification, Return response |
| Database | Yellow (#fff2cc) | Check duplicate, Validate rule code, Insert notice records, Update AN flags |

---

### Tab 3.4: Retrieve Vehicle Owner Particulars (LTA + DataHive/MHA)

#### Process Overview

| Attribute | Value |
|-----------|-------|
| Process Name | Retrieve Vehicle Owner Particulars |
| Section | 3.4 |
| Trigger | After AN qualification passes (an_flag='Y') |
| Frequency | Real-time (per qualified AN) |
| Systems Involved | Backend (OCMS API), External Systems (LTA, DataHive, MHA) |

#### Process Steps Table

| Step | Definition | Brief Description |
|------|-----------|-------------------|
| Check Vehicle Type | Vehicle type validation | Check vehicle registration type |
| Decision: Military Vehicle? | Military check | Check if vehicle_registration_type = 'I' (Military) |
| Set Military Owner | Military handling | Set owner = MINDEF, skip LTA API call |
| Call LTA API | Ownership query | Call LTA API with vehicle number to retrieve owner details |
| Store Owner Details | Save owner info | Store vehicle owner name, NRIC/FIN, registration details in database |
| Check Suspension Status | Suspension validation | Check if notice is suspended |
| Check Exclusion List | Exclusion validation | Check if owner in eNotification exclusion list |
| Call DataHive API | Contact retrieval | Call DataHive API with owner NRIC to get mobile/email |
| Decision: Contact Found? | Contact availability | Check if mobile OR email retrieved from DataHive |
| Qualify for eAN | eNotification path | Set eAN eligible flag, proceed to eAN sending |
| Call MHA API | Address retrieval | Call MHA API with owner NRIC to get registered address |
| Qualify for AN Letter | Physical letter path | Set AN letter flag, proceed to letter generation |
| Manual Review Queue | Manual handling | Add to manual review queue for staff follow-up |
| End | Retrieval complete | Owner particulars retrieval process finished |

#### Data Mapping - Storing owner particulars from LTA/DataHive/MHA

| Zone | Database Table | Field Name | Update Value |
| --- | --- | --- | --- |
| Intranet | ocms_offence_notice_owner_driver | notice_no | [notice_no] |
| Intranet | ocms_offence_notice_owner_driver | owner_driver_indicator | O |
| Intranet | ocms_offence_notice_owner_driver | owner_nric_no | [from LTA] |
| Intranet | ocms_offence_notice_owner_driver | owner_name | [from LTA] |
| Intranet | ocms_offence_notice_owner_driver | owner_blk_hse_no | [from MHA] |
| Intranet | ocms_offence_notice_owner_driver | owner_street | [from MHA] |
| Intranet | ocms_offence_notice_owner_driver | owner_floor | [from MHA] |
| Intranet | ocms_offence_notice_owner_driver | owner_unit | [from MHA] |
| Intranet | ocms_offence_notice_owner_driver | owner_bldg | [from MHA] |
| Intranet | ocms_offence_notice_owner_driver | owner_postal_code | [from MHA] |
| Intranet | ocms_offence_notice_owner_driver | cre_user_id | ocmsiz_app_conn |
| Intranet | ocms_offence_notice_owner_driver | cre_date | Current timestamp |

#### Swimlanes Definition

| Swimlane | Color | Steps |
|----------|-------|-------|
| Backend | Purple (#e1d5e7) | Check vehicle type, Check suspension, Check exclusion list, Qualify for eAN/letter |
| External System (LTA) | Green (#d5e8d4) | LTA API call, Return owner details |
| External System (DataHive) | Green (#d5e8d4) | DataHive API call, Return contact info |
| External System (MHA) | Green (#d5e8d4) | MHA API call, Return registered address |
| Database | Yellow (#fff2cc) | Store owner details, Check exclusion list |

---

### Tab 3.5: Send eNotification (eAN)

#### Process Overview

| Attribute | Value |
|-----------|-------|
| Process Name | Send eNotification for Advisory Notice (eAN) |
| Section | 3.5 |
| Trigger | After owner contact retrieval (mobile OR email available) |
| Frequency | Real-time (per qualified eAN) |
| Systems Involved | Backend (OCMS API), External Systems (SMS Gateway, Email Service) |

#### Process Steps Table

| Step | Definition | Brief Description |
|------|-----------|-------------------|
| Retrieve Notice Details | Get notice info | Retrieve notice number, offense details, composition amount from database |
| Retrieve Owner Contact | Get contact info | Retrieve mobile number and/or email address from previous step |
| Generate eAN Content | Create message | Generate eAN message content with notice details, offense info, appeal instructions |
| Decision: Mobile Available? | Mobile check | Check if mobile number exists |
| Send SMS | SMS notification | Call SMS Gateway API to send eAN SMS to mobile number |
| Log SMS Sent | SMS logging | Log SMS sent status, timestamp, recipient |
| Decision: Email Available? | Email check | Check if email address exists |
| Send Email | Email notification | Call Email Service API to send eAN email |
| Log Email Sent | Email logging | Log email sent status, timestamp, recipient |
| Update eAN Status | Update database | Update notice with eAN sent flag, sent timestamp, notification method |
| Fallback to Letter | Physical letter path | If all retries fail, proceed to AN letter sending |
| End | eAN complete | eNotification sending process finished |

#### Data Mapping - Update after Sending email

| Zone | Database Table | Field Name | Correct Value |
| --- | --- | --- | --- |
| Intranet | ocms_email_notification_records | notice_no | [notice_no] |
| Intranet | ocms_email_notification_records | processing_stage | RD1 |
| Intranet | ocms_email_notification_records | content | [enotification content] |
| Intranet | ocms_email_notification_records | date_sent | [current timestamp] |
| Intranet | ocms_email_notification_records | email_addr | [owner email] |
| Intranet | ocms_email_notification_records | status | sent |
| Intranet | ocms_email_notification_records | subject | [notification subject] |
| Intranet | ocms_email_notification_records | cre_user_id | ocmsiz_app_conn |
| Intranet | ocms_email_notification_records | cre_date | [current timestamp] |

#### Data Mapping - Update after sending SMS

| Zone | Database Table | Field Name | Correct Value |
| --- | --- | --- | --- |
| Intranet | ocms_sms_notification_records | notice_no | [notice_no] |
| Intranet | ocms_sms_notification_records | processing_stage | RD1 |
| Intranet | ocms_sms_notification_records | content | [enotification content] |
| Intranet | ocms_sms_notification_records | date_sent | [current timestamp] |
| Intranet | ocms_sms_notification_records | mobile_no | [owner mobile] |
| Intranet | ocms_sms_notification_records | status | sent |
| Intranet | ocms_sms_notification_records | cre_user_id | ocmsiz_app_conn |
| Intranet | ocms_sms_notification_records | cre_date | [current timestamp] |

#### Data Mapping - Suspend after sending eNotification

| Zone | Database Table | Field Name | Correct Value |
| --- | --- | --- | --- |
| Intranet | ocms_suspended_notice | notice_no | [notice_no] |
| Intranet | ocms_suspended_notice | date_of_suspension | [current timestamp] |
| Intranet | ocms_suspended_notice | sr_no | [sequential] |
| Intranet | ocms_suspended_notice | suspension_type | PS |
| Intranet | ocms_suspended_notice | reason_of_suspension | ANS |
| Intranet | ocms_suspended_notice | suspension_source | OCMS |
| Intranet | ocms_suspended_notice | cre_user_id | ocmsiz_app_conn |
| Intranet | ocms_suspended_notice | cre_date | [current timestamp] |

#### Swimlanes Definition

| Swimlane | Color | Steps |
|----------|-------|-------|
| Backend | Purple (#e1d5e7) | Retrieve details, Generate content, Update status, Log completion |
| External System (SMS) | Green (#d5e8d4) | Send SMS, Return SMS result |
| External System (Email) | Green (#d5e8d4) | Send Email, Return email result |
| Database | Yellow (#fff2cc) | Retrieve notice details, Insert notification records |

---

### Tab 3.5.1: Resend eAN SMS and Email (Retry Cron)

#### Process Overview

| Attribute | Value |
|-----------|-------|
| Process Name | Resend Failed eAN SMS and Email |
| Section | 3.5.1 |
| Trigger | Scheduled cron job runs daily at **14:00 (1400hrs)** |
| Frequency | Daily |
| Systems Involved | Backend (OCMS Cron), Database (Intranet), External Systems (SMS Gateway, Email Service) |

#### Cron Schedule

| Cron Job | Schedule | Time |
|----------|----------|------|
| NotificationSmsEmailRetryJob | `0 0 14 * * ?` | Daily at 14:00 (2:00 PM) |
| ShedLock Name | `send_ena_reminder_retry` | - |

#### Process Steps Table

| Step | Definition | Brief Description |
|------|-----------|-------------------|
| Pre-condition | Scheduled cron | Scheduled cron job runs daily at 1400hrs to retry sending failed eAN SMSes and emails |
| Query DB for Failed Notifications | Get failed records | Query `ocms_sms_notification_records` and `ocms_email_notification_records` where status='E' and date_sent=today |
| Retry Sending | Resend notifications | System resends the failed SMS and email notifications |
| Update Status | Update records | If successful: Update status to 'S', suspend notice with PS-ANS. If failed again: Update processing stage from ENA → RD1 (redirect to letter flow) |
| Process All Records | Batch processing | Cron runs until re-send status for all records is updated |
| End | Cron job ends | Retry process completed |

#### Retry Logic

```
Query failed records:
  - ocms_sms_notification_records WHERE status='E' AND date_sent=TODAY
  - ocms_email_notification_records WHERE status='E' AND date_sent=TODAY

FOR each failed record DO
  Resend SMS/Email

  IF resend successful THEN
    Update status = 'S'
    Suspend notice with PS-ANS
  ELSE
    Update processing_stage from ENA → RD1
    Notice redirected to AN Letter flow
  END IF
END FOR
```

#### Location in Code

- Job File: `NotificationSmsEmailRetryJob.java`
- Schedule: `cron-schedules.properties` line 172

#### Swimlanes Definition

| Swimlane | Color | Steps |
|----------|-------|-------|
| Backend | Purple (#e1d5e7) | Query failed records, Retry sending, Update status |
| External System (SMS) | Green (#d5e8d4) | Resend SMS |
| External System (Email) | Green (#d5e8d4) | Resend Email |
| Database | Yellow (#fff2cc) | Query failed notifications, Update notification records |

---

### Tab 3.6: Send AN Letter (SLIFT)

#### Process Overview

| Attribute | Value |
|-----------|-------|
| Process Name | Send Advisory Notice Letter |
| Section | 3.6 |
| Trigger | After owner address retrieval OR eNotification failed |
| Frequency | Real-time (per qualified AN letter) |
| Systems Involved | Backend (OCMS API), External System (SLIFT/SFTP) |

#### Process Steps Table

| Step | Definition | Brief Description |
|------|-----------|-------------------|
| Retrieve Notice Details | Get notice info | Retrieve notice number, offense details, composition amount from database |
| Retrieve Owner Address | Get address info | Retrieve registered address from MHA API response |
| Retrieve Owner Name | Get owner name | Retrieve vehicle owner name from LTA API response |
| Generate Letter Template | Create letter | Generate AN letter PDF/XML with notice details, owner address, letter content |
| Populate Letter Fields | Fill template | Combine all fields |
| Format Letter Content | Format PDF/XML | Format letter content according to SLIFT requirements |
| Call SLIFT API | Submit for printing | Submit letter to SLIFT/SFTP for printing and mailing |
| Decision: SLIFT Success? | Submission result | Check if SLIFT accepted letter submission |
| Update Letter Status | Update database | Update notice with letter_sent_flag='Y' |
| Log Letter Sent | Completion logging | Log AN letter sent status, timestamp, recipient |
| Retry Submission | Retry logic | Retry failed submission (max 3 times with exponential backoff) |
| Manual Review Queue | Manual handling | Add to manual review queue if all retries fail |
| End | Letter complete | AN letter sending process finished |

#### Data Mapping - Insert after sending AN Letter

| Zone | Database Table | Field Name | Correct Value |
| --- | --- | --- | --- |
| Intranet | ocms_an_letter | notice_no | [notice_no] |
| Intranet | ocms_an_letter | date_of_processing | [current timestamp] |
| Intranet | ocms_an_letter | date_of_an_letter | [current timestamp] |
| Intranet | ocms_an_letter | owner_nric_no | [owner nric] |
| Intranet | ocms_an_letter | owner_name | [owner name] |
| Intranet | ocms_an_letter | owner_id_type | [id type] |
| Intranet | ocms_an_letter | owner_blk_hse_no | [blk hse no] |
| Intranet | ocms_an_letter | owner_street | [street] |
| Intranet | ocms_an_letter | owner_floor | [floor] |
| Intranet | ocms_an_letter | owner_unit | [unit] |
| Intranet | ocms_an_letter | owner_bldg | [building] |
| Intranet | ocms_an_letter | owner_postal_code | [postal code] |
| Intranet | ocms_an_letter | processing_stage | RD1 |
| Intranet | ocms_an_letter | cre_user_id | ocmsiz_app_conn |
| Intranet | ocms_an_letter | cre_date | [current timestamp] |

#### Swimlanes Definition

| Swimlane | Color | Steps |
|----------|-------|-------|
| Backend | Purple (#e1d5e7) | Retrieve details, Generate letter, Format content, Update status, Log completion |
| External System (SLIFT) | Green (#d5e8d4) | Submit letter, Return submission result |
| Database | Yellow (#fff2cc) | Retrieve notice/owner details, Insert letter record |

---

### Tab 3.7: Suspend Notice with PS-ANS

#### Process Overview

| Attribute | Value |
|-----------|-------|
| Process Name | Suspend Notice with PS-ANS |
| Section | 3.7 |
| Trigger | After AN qualification and notification (eAN or letter) sent, OR manual suspension by OIC via Staff Portal |
| Frequency | Real-time (per qualified AN) or On-demand (OIC manual) |
| Systems Involved | Backend (OCMS API), Database (Intranet), Frontend (Staff Portal for manual) |

#### Suspension Methods

| Method | Trigger | Source |
|--------|---------|--------|
| Automatic | After eAN sent successfully | `suspension_source` = 'OCMS' |
| Automatic | After AN Letter dropped to SFTP | `suspension_source` = 'OCMS' |
| Manual | OIC manually suspends via Staff Portal | `suspension_source` = 'STAFF_PORTAL' |

**Note:** OCMS OICs can manually suspend Notices with PS-ANS via the OCMS Staff Portal. Reference: `Ocms41ManualReviewService.java`, PS Report Section 6.3.2.

#### Process Steps Table

| Step | Definition | Brief Description |
|------|-----------|-------------------|
| Retrieve Notice Details | Get notice info | Retrieve notice number, notice date from database |
| Generate SR Number | Create serial number | Generate sequential sr_no for suspension record |
| Create Suspension Record | Insert suspension | Insert record into ocms_suspended_notice table |
| Update Notice with Suspension | Update notice | Update ocms_valid_offence_notice with suspension details |
| Sync to Internet DB | Mirror record | Insert/update suspension record in eocms_valid_offence_notice (Internet) |
| Log Suspension Created | Logging | Log PS-ANS suspension creation with timestamp |
| End | Suspension complete | PS-ANS suspension process finished |

#### Data Mapping - Suspend notice PS - ANS

| Zone | Database Table | Field Name | Correct Value |
| --- | --- | --- | --- |
| Intranet | ocms_suspended_notice | notice_no | [notice_no] |
| Intranet | ocms_suspended_notice | date_of_suspension | [current timestamp] |
| Intranet | ocms_suspended_notice | sr_no | [sequential] |
| Intranet | ocms_suspended_notice | suspension_type | PS |
| Intranet | ocms_suspended_notice | reason_of_suspension | ANS |
| Intranet | ocms_suspended_notice | suspension_source | OCMS |
| Intranet | ocms_suspended_notice | officer_authorising_suspension | ocmsizmgr_conn |
| Intranet | ocms_suspended_notice | due_date_of_revival | [current date + 30 days] |
| Intranet | ocms_suspended_notice | suspension_remarks | Advisory Notice detected - qualified AN |
| Intranet | ocms_suspended_notice | cre_user_id | ocmsiz_app_conn |
| Intranet | ocms_suspended_notice | cre_date | Current timestamp |

#### Data Mapping - Update after suspend notice

| Zone | Database Table | Field Name | Correct Value |
| --- | --- | --- | --- |
| Intranet & Internet | ocms_valid_offence_notice | suspension_type | PS |
| Intranet & Internet | ocms_valid_offence_notice | epr_reason_of_suspension | ANS |
| Intranet & Internet | ocms_valid_offence_notice | epr_date_of_suspension | Current timestamp |
| Intranet & Internet | ocms_valid_offence_notice | due_date_of_revival | Current date + 30 days |
| Intranet & Internet | ocms_valid_offence_notice | upd_user_id | ocmsiz_app_conn |
| Intranet & Internet | ocms_valid_offence_notice | upd_date | Current timestamp |

#### Swimlanes Definition

| Swimlane | Color | Steps |
|----------|-------|-------|
| Backend | Purple (#e1d5e7) | Generate SR number, Log suspension |
| Database | Yellow (#fff2cc) | Retrieve notice details, Create suspension record, Update notice, Sync to Internet |

---

### Tab 3.8: Generate ANS Reports

#### Process Overview

| Attribute | Value |
|-----------|-------|
| Process Name | Generate Advisory Notice Suspension Reports |
| Section | 3.8 |
| Trigger | Ad-hoc request from staff portal OR scheduled cron job |
| Frequency | On-demand / Daily (scheduled) |
| Systems Involved | Backend (OCMS API), Database (Intranet), Frontend (Staff Portal) |

#### Process Steps Table

| Step | Definition | Brief Description |
|------|-----------|-------------------|
| Receive Report Parameters | Get report criteria | Receive date range, vehicle number, subsystem filter from user |
| Validate Parameters | Parameter validation | Validate date range and required fields |
| Decision: Valid Parameters? | Validation result | Check if parameters are valid |
| Query ANS Suspension Data | Database query | Query ocms_suspended_notice joined with ocms_valid_offence_notice |
| Apply Filters | Filter results | Apply vehicle number, subsystem, date range filters |
| Decision: Any Records? | Record check | Check if query returned any records |
| Sort Results | Order data | Sort by suspension date descending |
| Format Report Data | Format output | Format data for report display (Excel/PDF) |
| Generate Report File | Create file | Generate Excel or PDF report file |
| Return Report | Download response | Return report file to user for download |
| Log Report Generated | Logging | Log report generation with parameters, timestamp, user |
| Return No Data Message | Empty result | Return message "No records found for the selected criteria" |
| Return Error Response | Error handling | Return error message for invalid parameters |
| End | Report complete | ANS report generation finished |

#### Swimlanes Definition

| Swimlane | Color | Steps |
|----------|-------|-------|
| Frontend | Blue (#dae8fc) | Receive report request, Download report |
| Backend | Purple (#e1d5e7) | Validate parameters, Apply filters, Format data, Generate report, Return response |
| Database | Yellow (#fff2cc) | Query suspended notices, Join notice details |

---

### Tab 3.8.1: ANS Letter Reconciliation Report

#### Process Overview

| Attribute | Value |
|-----------|-------|
| Process Name | ANS Letter Reconciliation Report |
| Section | 3.8.1 |
| Trigger | Scheduled cron job (daily) |
| Frequency | Daily |
| Systems Involved | Backend (OCMS Cron), Database (Intranet), External System (Toppan SFTP), Azure Blob Storage |

#### Purpose

Reconcile AN letters sent to Toppan (printing vendor) vs letters successfully printed:
- Compare letters in `ocms_an_letter` table with Toppan acknowledgement file
- Generate Excel report with reconciliation statistics
- Upload report to Azure Blob Storage
- Send report via email to support recipients

#### Process Steps Table

| Step | Definition | Brief Description |
|------|-----------|-------------------|
| Read Control Summary | Get sent letters | Read Control Summary Report from SFTP (letters sent to Toppan) |
| Read Acknowledgement File | Get printed letters | Read Toppan Acknowledgement File (letters successfully printed) |
| Query AN Letter Database | Get DB records | Query `ocms_an_letter` table for letters in the date range |
| Compare Records | Reconciliation | Compare sent letters vs printed letters vs database records |
| Calculate Statistics | Generate metrics | Calculate: total sent, printed successfully, missing, errors, match rate |
| Generate Excel Report | Create report | Generate Excel report with reconciliation details |
| Upload to Azure Blob | Store report | Upload report file to Azure Blob Storage |
| Send Email Report | Notify recipients | Send report via email to support team |
| Log Completion | Logging | Log reconciliation job completion |
| End | Job complete | ANS Letter Reconciliation Report finished |

#### Reconciliation Metrics

| Metric | Description |
|--------|-------------|
| Total Letters Sent | Count of letters in Control Summary Report |
| Letters Printed | Count of letters in Toppan Acknowledgement |
| Missing Letters | Letters sent but not in acknowledgement |
| Errors | Letters with printing errors |
| Match Rate | Percentage of successful prints |

#### Location in Code

- Job File: `AnsLetterReconciliationJob.java`
- Helper File: `AnsLetterReconciliationHelper.java`
- Database Table: `ocms_an_letter`

#### Swimlanes Definition

| Swimlane | Color | Steps |
|----------|-------|-------|
| Backend | Purple (#e1d5e7) | Read files, Compare records, Calculate statistics, Generate report |
| Database | Yellow (#fff2cc) | Query AN letter records |
| External System (Toppan) | Green (#d5e8d4) | Provide Control Summary, Provide Acknowledgement File |
| External System (Azure) | Green (#d5e8d4) | Store report file |

---

### Tab 3.9: Send Unqualified AN List (REPCCS/CES)

#### Process Overview

| Attribute | Value |
|-----------|-------|
| Process Name | Send Unqualified AN List to REPCCS/CES |
| Section | 3.9 |
| Trigger | Scheduled cron job (daily) |
| Frequency | Daily at configured time |
| Systems Involved | Backend (OCMS Cron), Database (Intranet), External Systems (REPCCS, CES) |

#### Process Steps Table

| Step | Definition | Brief Description |
|------|-----------|-------------------|
| Calculate Date Range | Determine period | Calculate date range for notices to process |
| Query Unqualified ANs | Database query | Query notices where an_flag='N' or NULL |
| Filter by Subsystem | Separate lists | Separate unqualified notices by subsystem (REPCCS vs CES) |
| Decision: Any REPCCS Records? | REPCCS check | Check if any unqualified notices for REPCCS |
| Format REPCCS List | Create payload | Format unqualified AN list for REPCCS API |
| Call REPCCS API | Send to REPCCS | Send unqualified AN list to REPCCS |
| Log REPCCS Sent | REPCCS logging | Log successful transmission to REPCCS |
| Decision: Any CES Records? | CES check | Check if any unqualified notices for CES |
| Format CES List | Create payload | Format unqualified AN list for CES API |
| Call CES API | Send to CES | Send unqualified AN list to CES |
| Log CES Sent | CES logging | Log successful transmission to CES |
| Update Notice Status | Mark as sent | Update notices with unqualified_list_sent_flag='Y' |
| Log Cron Complete | Completion logging | Log cron job completion with summary |
| End | Cron job finished | Unqualified AN list sending complete |

#### Unqualified AN Conditions

| Step | Condition | Add to Unqualified ANS vehicle? |
|------|-----------|--------------------------------|
| 1 | The vehicle doesn't have the offence in last 24 months | No |
| 2 | If the vehicle has past offence in last 24 months, check the suspension type of the notice | |
| 2.a | If the notice is not under suspension or suspended with TS (temporary suspension), add the vehicle to Unqualified list | Yes |
| 2.b | If PS, check the ANS_PS_Reason for the following PS reasons: CAN, CFA, DBB, VST. If PS reason for ALL offence records within the last 2 years does not fall in any of the ANS_PS_Reasons above, add vehicle number to unqualified ANS list | Yes |

#### Swimlanes Definition

| Swimlane | Color | Steps |
|----------|-------|-------|
| Backend | Purple (#e1d5e7) | Calculate date range, Filter by subsystem, Format lists, Update status, Log completion |
| Database | Yellow (#fff2cc) | Query unqualified ANs, Update notice status |
| External System (REPCCS) | Green (#d5e8d4) | Receive REPCCS list, Return response |
| External System (CES) | Green (#d5e8d4) | Receive CES list, Return response |

---

## End of Document

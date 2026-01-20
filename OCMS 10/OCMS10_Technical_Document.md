# OCMS 10 - Advisory Notices Processing

**Prepared by**

Accenture

---

## Version History

| Version | Updated By | Date | Changes |
| --- | --- | --- | --- |
| v1.0 | Accenture | 16/01/2026 | Document Initiation |

---

## Table of Content

| Section | Content | Pages |
| --- | --- | --- |
| 1 | Advisory Notices | [X] |
| 1.1 | Use Case | [X] |
| 1.2 | High Level Process Flow | [X] |
| 2 | AN Verification | [X] |
| 2.1 | Use Case | [X] |
| 2.2 | AN Qualification Check | [X] |
| 2.2.1 | Data Mapping | [X] |
| 2.2.2 | Success Outcome | [X] |
| 2.2.3 | Error Handling | [X] |
| 3 | Receiving AN Offence Data | [X] |
| 3.1 | Use Case | [X] |
| 3.2 | Receiving eligible ANs from REPCCS | [X] |
| 3.2.1 | Data Mapping | [X] |
| 3.2.2 | Success Outcome | [X] |
| 3.2.3 | Error Handling | [X] |
| 3.3 | Receiving eligible ANs from CES | [X] |
| 3.3.1 | Data Mapping | [X] |
| 3.3.2 | Success Outcome | [X] |
| 3.3.3 | Error Handling | [X] |
| 4 | Retrieving Vehicle Owner Particulars for Advisory Notices | [X] |
| 4.1 | Use Case | [X] |
| 4.2 | Retrieve Owner Particulars Flow | [X] |
| 4.2.1 | Data Mapping | [X] |
| 4.2.2 | Success Outcome | [X] |
| 4.2.3 | Error Handling | [X] |
| 5 | Sending eNotifications for AN (eAN) | [X] |
| 5.1 | Use Case | [X] |
| 5.2 | Send eNotification Flow | [X] |
| 5.2.1 | Data Mapping | [X] |
| 5.2.2 | Success Outcome | [X] |
| 5.2.3 | Error Handling | [X] |
| 6 | Sending AN Letter | [X] |
| 6.1 | Use Case | [X] |
| 6.2 | Send AN Letter Flow | [X] |
| 6.2.1 | Data Mapping | [X] |
| 6.2.2 | Success Outcome | [X] |
| 6.2.3 | Error Handling | [X] |
| 7 | Suspending Notices with PS-ANS | [X] |
| 7.1 | Use Case | [X] |
| 7.2 | Suspend PS-ANS Flow | [X] |
| 7.2.1 | Data Mapping | [X] |
| 7.2.2 | Success Outcome | [X] |
| 7.2.3 | Error Handling | [X] |
| 8 | Reports for ANS | [X] |
| 8.1 | Use Case | [X] |
| 8.2 | Generate ANS Reports Flow | [X] |
| 8.2.1 | Data Mapping | [X] |
| 8.2.2 | Success Outcome | [X] |
| 8.2.3 | Error Handling | [X] |
| 9 | Sending Unqualified AN to REPCCS and CES | [X] |
| 9.1 | Use Case | [X] |
| 9.2 | Send Unqualified AN List Flow | [X] |
| 9.2.1 | Data Mapping | [X] |
| 9.2.2 | Success Outcome | [X] |
| 9.2.3 | Error Handling | [X] |

---

# Section 1 - Advisory Notices

## Use Case

1. Advisory Notices (AN) refer to Offence Notices which are not compoundable (payable) and will not be further processed.

2. ANs are issued to Vehicle Owners in place of a parking fine and serve as formal warnings to notify them that they have committed an offence and to comply with Parking Rules.

3. After a new Notice is created, OCMS uses the offence details to verify whether the Notice qualifies as an AN.

4. AN verification is a process that consists of the following: Verifying whether the Notice meets the criteria that qualifies it as an AN.

5. Notices that qualify as AN will be updated with AN flag "Y" and redirected to the AN workflow where OCMS will send an eNotification (eAN) or a physical AN Letter to the Vehicle Owner to notify them of the AN.

6. Notices will be permanently suspended with suspension code as 'ANS' after the e-notification or AN letter is sent out, i.e, PS-ANS.

## High Level Process Flow

![Flow Diagram](./OCMS10_Flowchart.drawio - Tab 2.1)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Definition | Brief Description |
| --- | --- | --- |
| Start | Entry point | Notice data received from external source or manual creation |
| Notice Creation | Create notice record | Validate mandatory fields and create notice in system |
| AN Verification | Check qualification | Verify if notice qualifies as Advisory Notice (exemption rules + past offense check). Note: CES notices with an_flag='Y' skip qualification and go directly to PS-ANS |
| Decision: Qualifies? | AN qualification result | Check if notice passed all AN qualification criteria |
| Update AN Flags | Mark as AN | Set an_flag='Y', payment_acceptance_allowed='N' |
| Retrieve Owner Info | Get vehicle owner | Call LTA API to retrieve vehicle owner particulars |
| Decision: eAN Eligible? | Check contact availability | Determine if eNotification is possible based on contact info |
| Send eNotification | Electronic notification | Send SMS/Email to vehicle owner |
| Send AN Letter | Physical letter | Generate and send physical letter via SLIFT |
| Suspend with PS-ANS | Create suspension | Create PS-ANS suspension record for qualified AN |
| End | Process complete | AN processing completed |

---

# Section 2 - AN Verification

## Use Case

1. OCMS performs a verification process to determine whether a Notice qualifies as an AN.

2. OCMS reviews the Offence Details to determine:<br>a. Whether the Offence Rule Code of the new Notice is eligible for ANS, and/or<br>b. Whether the vehicle has any past offences in the last 24 months, and whether all past Notices were suspended with the following PS Suspension Codes: CAN, CFA, DBB, VST

3. A Notice is sent for AN Qualification after the Notice creation determines that it is eligible as an AN.

4. The objective of the AN Qualification check is to verify if the Notice qualifies as an AN.

5. The AN Qualification process checks the Offence Rule Code and Vehicle Number of the new notice to determine that it qualifies as an AN:<br>a. The Offence Rule Code is not listed in the ANS exemption list, OR if the Offence Rule Code is listed, the offence date is not within the active exemption period and does not bear the specific rule remark and composition amount listed; AND<br>b. The vehicle has no past Offence Notices created within the last 24 months from the current date, OR<br>c. The vehicle has one or more past Offence Notices created within the last 24 months from the current date, AND all past Offence Notices were suspended with ANS PS reason codes (CAN, CFA, DBB or VST).

## AN Qualification Check

![Flow Diagram](./OCMS10_Flowchart.drawio - Tab 3.1)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Definition | Brief Description |
| --- | --- | --- |
| Start | Qualification check | Check if notice qualifies as Advisory Notice |
| Check Offense Type | Validate offense type | Offense type must be 'O' (Offender) |
| Check Vehicle Type | Validate vehicle type | Vehicle registration type must be in [S, D, V, I] (local vehicles only) |
| Check Exemption Rules | Exemption validation | Check if offense is exempt from AN (Rule 20412 + $80, or Rule 11210 + LB/HL) |
| Check Past Offense | 24-month window check | Check for past qualifying offense in 24-month window |
| Decision: Has Past Offense? | Past offense validation | Check if vehicle has offense in past 24 months |
| Check ANS PS Reasons | Validate suspension reasons | If has past offenses, check if ALL are suspended with CAN/CFA/DBB/VST |
| Return Qualified | Pass qualification | Return qualified=true with AN details |
| Return Not Qualified | Fail qualification | Return qualified=false with reason |
| End | Check complete | Qualification result returned |

### Data Mapping

This data mapping documents the SELECT and UPDATE operations performed during AN qualification check.

| Zone | Database Table | Field Name | Operation/Update Value |
| --- | --- | --- | --- |
| Intranet | ocms_valid_offence_notice | notice_no | SELECT (lookup) |
| Intranet | ocms_valid_offence_notice | vehicle_no | SELECT (for past offense check) |
| Intranet | ocms_valid_offence_notice | offense_notice_type | SELECT (must be 'O') |
| Intranet | ocms_valid_offence_notice | vehicle_registration_type | SELECT (must be S/D/V/I) |
| Intranet | ocms_valid_offence_notice | computer_rule_code | SELECT (exemption check) |
| Intranet | ocms_valid_offence_notice | composition_amount | SELECT (exemption check) |
| Intranet | ocms_valid_offence_notice | vehicle_category | SELECT (exemption check) |
| Intranet | ocms_valid_offence_notice | notice_date_and_time | SELECT (24-month window) |
| Intranet | ocms_valid_offence_notice | an_flag | UPDATE to 'Y' if qualified |
| Intranet | ocms_valid_offence_notice | payment_acceptance_allowed | UPDATE to 'N' if qualified |
| Intranet | ocms_valid_offence_notice | epr_reason_of_suspension | SELECT (ANS PS check) |
| Intranet | ocms_valid_offence_notice | upd_user_id | UPDATE to ocmsiz_app_conn |
| Intranet | ocms_valid_offence_notice | upd_date | UPDATE to current timestamp |

### Success Outcome

- Notice passes all AN qualification checks (offense type = 'O', vehicle type in [S, D, V, I], not exempt, no past unsuspended offenses or all past offenses suspended with ANS PS reasons)
- Notice an_flag is updated to 'Y'
- Notice payment_acceptance_allowed is updated to 'N'
- Notice proceeds to eAN/AN Letter processing flow

### Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Offense type not 'O' | Notice offense type is not 'O' (Offender) | Notice does not qualify for AN, continue standard processing |
| Vehicle type not local | Vehicle registration type not in [S, D, V, I] | Notice does not qualify for AN, continue standard processing |
| Exempt offense | Rule 20412 + $80 or Rule 11210 + LB/HL | Notice is exempt from AN, continue standard processing |
| Has unsuspended past offense | Vehicle has past offense not suspended with ANS PS reasons | Notice does not qualify for AN, continue standard processing |
| Database query error | SQLException during past offense check | Log error, treat as not qualified |

---

# Section 3 - Receiving AN Offence Data

## Use Case

1. OCMS receives raw Offence data sent by the following backend enforcement systems for Notice creation and processing:<br>a. REPCCS<br>b. CES

2. REPCCS and CES use the ANSFlag field to indicate whether the Notice is eligible as an AN.

3. While OCMS receives the ANS Flag indicator sent by CES, OCMS will set the ANs flag directly based on CES data and will not perform the ANs Qualification check. If the CES AN indicator = 'Y', the notice shall be created with ANs flag = 'Y' and subsequently suspended with status/reason PS-ANS.

4. While OCMS receives the ANSFlag indicator sent by REPCCS, OCMS will not process the indicator value. It will disregard the indicator value sent by the backend enforcement systems. After AN qualification check for the notice, OCMS will update AN flag:<br>a. IF AN flag is 'Y' and the notice is not eligible for ANS at time of checking in OCMS, OCMS will update AN flag from 'Y' to 'N' and vice versa.<br>b. IF AN flag is 'Y' and the notice is eligible for ANS, OCMS won't update AN flag.

5. OCMS will perform the AN Qualification processes for every Notice sent by REPCCS and other sources (OCMS Staff Portal and PLUS Staff Portal).

## Receiving eligible ANs from REPCCS

![Flow Diagram](./OCMS10_Flowchart.drawio - Tab 3.2)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Definition | Brief Description |
| --- | --- | --- |
| Start | Webhook received | REPCCS sends notice data to OCMS webhook endpoint |
| Receive Webhook Payload | Parse request | Receive and parse RepWebHookPayloadDto |
| Validate Mandatory Fields | Field validation | Check all mandatory fields present |
| Check Duplicate Notice | Duplicate detection | Check if notice number already exists |
| Detect Vehicle Type | Vehicle type detection | Apply special vehicle detection (MID/MINDEF, UNLICENSED_PARKING) |
| Generate Notice Number | Create notice ID | Generate notice number if not provided |
| Insert Notice Records | Create records | Insert into ocms_valid_offence_notice and ocms_offence_notice_detail |
| Check Double Booking | DBB detection | Query for duplicate offense details |
| Run AN Qualification | Qualification check | Call checkQualification() for offense_type='O' and vehicle_type in [S,D,V,I] |
| Update AN Flags | Mark as AN | Set an_flag='Y', payment_acceptance_allowed='N' if qualified |
| Return Success Response | Response to REPCCS | Return HTTP 200 with OCMS-2000 and message "OK" |
| Return Error Response | Error to REPCCS | Return HTTP 400 with OCMS-4000 error |
| End | Webhook complete | REPCCS webhook processing finished |

### API Specification

#### REPCCS Webhook API

| Field | Value |
| --- | --- |
| API Name | repccsWebhook |
| URL | POST /v1/repccsWebhook |
| Description | Webhook endpoint to receive notice data from REPCCS system |
| Method | POST |
| Header | `{ "Content-Type": "application/json", "API-Key": "[API_KEY]" }` |
| Payload | `{ "transactionId": "string", "subsystemLabel": "string (8)", "anFlag": "string (1)", "noticeNo": "string (10)", "vehicleNo": "string (14)", "computerRuleCode": "integer", "compositionAmount": "decimal", "noticeDateAndTime": "datetime", "offenceNoticeType": "string (1)", "vehicleCategory": "string (1)", "vehicleRegistrationType": "string (1)", "ppCode": "string (5)", "ppName": "string (100)", "creUserId": "string", "creDate": "datetime" }` |
| Response | `{ "data": { "appCode": "OCMS-2000", "message": "OK" } }` |
| Response Failure | `{ "data": { "appCode": "OCMS-4000", "message": "Invalid input format or failed validation" } }` |

### Data Mapping

This data mapping documents the INSERT operation performed when creating a notice from REPCCS webhook.

| Zone | Database Table | Field Name | Insert Value |
| --- | --- | --- | --- |
| Intranet | ocms_valid_offence_notice | notice_no | From payload or generated |
| Intranet | ocms_valid_offence_notice | vehicle_no | From payload |
| Intranet | ocms_valid_offence_notice | computer_rule_code | From payload |
| Intranet | ocms_valid_offence_notice | composition_amount | From payload |
| Intranet | ocms_valid_offence_notice | notice_date_and_time | From payload |
| Intranet | ocms_valid_offence_notice | offense_notice_type | From payload |
| Intranet | ocms_valid_offence_notice | vehicle_category | From payload |
| Intranet | ocms_valid_offence_notice | vehicle_registration_type | From payload |
| Intranet | ocms_valid_offence_notice | pp_code | From payload |
| Intranet | ocms_valid_offence_notice | pp_name | From payload |
| Intranet | ocms_valid_offence_notice | subsystem_label | From payload |
| Intranet | ocms_valid_offence_notice | an_flag | 'Y' if qualified, else 'N' |
| Intranet | ocms_valid_offence_notice | payment_acceptance_allowed | 'N' if AN, else 'Y' |
| Intranet | ocms_valid_offence_notice | cre_user_id | ocmsiz_app_conn |
| Intranet | ocms_valid_offence_notice | cre_date | Current timestamp |
| Intranet | ocms_offence_notice_detail | notice_no | From parent record |
| Internet | eocms_valid_offence_notice | (all fields) | Mirror of intranet |
| Internet | eocms_valid_offence_notice | cre_user_id | ocmsez_app_conn |

**Notes:**
- REPCCS AN flag in payload is DISREGARDED - OCMS performs own qualification
- Audit user: `ocmsiz_app_conn` (Intranet), `ocmsez_app_conn` (Internet)

### Success Outcome

- Notice data validated successfully
- No duplicate notice detected
- Notice records created in ocms_valid_offence_notice and ocms_offence_notice_detail
- AN qualification check performed
- If qualified, an_flag set to 'Y' and payment_acceptance_allowed set to 'N'
- HTTP 200 response returned with OCMS-2000 and message "OK"

### Error Handling

| Error Scenario | App Error Code | User Message | Brief Description |
| --- | --- | --- | --- |
| Missing mandatory fields | OCMS-4000 | Invalid input format or failed validation | HTTP 400 returned |
| Duplicate notice number | OCMS-4000 | Invalid input format or failed validation | HTTP 400 returned |
| Invalid input format | OCMS-4000 | Invalid input format or failed validation | HTTP 400 returned |
| Internal server error | OCMS-5000 | Something went wrong. Please try again later. | HTTP 500 returned |

## Receiving eligible ANs from CES

![Flow Diagram](./OCMS10_Flowchart.drawio - Tab 3.3)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Definition | Brief Description |
| --- | --- | --- |
| Start | Webhook received | CES sends notice data to OCMS webhook endpoint |
| Receive Webhook Payload | Parse request | Receive and parse CesCreateNoticeDto |
| Validate Mandatory Fields | Field validation | Check all mandatory fields present |
| Validate Subsystem Label | Subsystem validation | Check subsystemLabel format (3-8 chars, 030-999 range) |
| Check Duplicate Notice | Duplicate detection | Check if notice number already exists |
| Validate Offense Rule Code | Rule code validation | Check if computer_rule_code exists in offense_rule_code table |
| Detect Vehicle Type | Vehicle type detection | Apply special vehicle detection (MID/MINDEF, UNLICENSED_PARKING) |
| Check EHT/Certis AN Flag | EHT check | If subsystem 030-999 AND an_flag='Y' in payload, skip AN qualification |
| Decision: EHT with AN Flag? | EHT AN check | Check if EHT subsystem with an_flag='Y' |
| Create PS-ANS Suspension | Immediate suspension | Create notice, trigger PS-ANS suspension, early return |
| Insert Notice Records | Create records | Insert into ocms_valid_offence_notice and ocms_offence_notice_detail |
| Run AN Qualification | Qualification check | Call checkQualification() if not EHT with AN flag |
| Update AN Flags | Mark as AN | Set an_flag='Y', payment_acceptance_allowed='N' if qualified |
| Return Success Response | Response to CES | Return HTTP 200 with OCMS-2000 |
| Return Error Response | Error to CES | Return HTTP 400 with OCMS-4000 or HTTP 226 with OCMS-2026 |
| End | Webhook complete | CES webhook processing finished |

### API Specification

#### CES Webhook API

| Field | Value |
| --- | --- |
| API Name | cesWebhook-create-notice |
| URL | POST /v1/cesWebhook-create-notice |
| Description | Webhook endpoint to receive notice data from CES (Certis) system |
| Method | POST |
| Header | `{ "Content-Type": "application/json", "API-Key": "[API_KEY]" }` |
| Payload | `{ "transactionId": "string", "subsystemLabel": "string (3-8, 030-999)", "anFlag": "string (1)", "noticeNo": "string (10)", "vehicleNo": "string (14)", "computerRuleCode": "integer", "compositionAmount": "decimal", "noticeDateAndTime": "datetime", "offenceNoticeType": "string (1)", "vehicleCategory": "string (1)", "vehicleRegistrationType": "string (1)", "ppCode": "string (5)", "ppName": "string (100)", "creUserId": "string", "creDate": "datetime" }` |
| Response | `{ "data": { "appCode": "OCMS-2000", "message": "Resource created successfully" } }` |
| Response Failure (Validation) | `{ "data": { "appCode": "OCMS-4000", "message": "Invalid input format or failed validation" } }` |
| Response Failure (Duplicate/Invalid Rule) | `{ "data": { "appCode": "OCMS-2026", "message": "Notice no Already exists / Invalid Offence Rule Code" } }` |

### Data Mapping

This data mapping documents the INSERT operation performed when creating a notice from CES webhook.

| Zone | Database Table | Field Name | Insert Value |
| --- | --- | --- | --- |
| Intranet | ocms_valid_offence_notice | notice_no | From payload |
| Intranet | ocms_valid_offence_notice | vehicle_no | From payload |
| Intranet | ocms_valid_offence_notice | computer_rule_code | From payload |
| Intranet | ocms_valid_offence_notice | composition_amount | From payload |
| Intranet | ocms_valid_offence_notice | notice_date_and_time | From payload |
| Intranet | ocms_valid_offence_notice | offense_notice_type | From payload |
| Intranet | ocms_valid_offence_notice | vehicle_category | From payload |
| Intranet | ocms_valid_offence_notice | vehicle_registration_type | From payload |
| Intranet | ocms_valid_offence_notice | pp_code | From payload |
| Intranet | ocms_valid_offence_notice | pp_name | From payload |
| Intranet | ocms_valid_offence_notice | subsystem_label | From payload (030-999) |
| Intranet | ocms_valid_offence_notice | an_flag | From payload (used directly) |
| Intranet | ocms_valid_offence_notice | payment_acceptance_allowed | 'N' if an_flag='Y', else 'Y' |
| Intranet | ocms_valid_offence_notice | cre_user_id | ocmsiz_app_conn |
| Intranet | ocms_valid_offence_notice | cre_date | Current timestamp |
| Intranet | ocms_offence_notice_detail | notice_no | From parent record |
| Intranet | ocms_suspended_notice | notice_no | Notice number (if an_flag='Y') |
| Intranet | ocms_suspended_notice | date_of_suspension | Current timestamp (if an_flag='Y') |
| Intranet | ocms_suspended_notice | sr_no | Sequential number (if an_flag='Y') |
| Intranet | ocms_suspended_notice | suspension_type | PS (if an_flag='Y') |
| Intranet | ocms_suspended_notice | reason_of_suspension | ANS (if an_flag='Y') |
| Internet | eocms_valid_offence_notice | (all fields) | Mirror of intranet |
| Internet | eocms_valid_offence_notice | cre_user_id | ocmsez_app_conn |

**Notes:**
- CES AN flag in payload is USED DIRECTLY - no AN qualification check performed
- If an_flag='Y', PS-ANS suspension created immediately
- Audit user: `ocmsiz_app_conn` (Intranet), `ocmsez_app_conn` (Internet)

### Success Outcome

- Notice data validated successfully (mandatory fields, subsystem label format, rule code)
- No duplicate notice detected
- Notice records created in ocms_valid_offence_notice and ocms_offence_notice_detail
- For CES with an_flag='Y': PS-ANS suspension created immediately without AN qualification check
- For CES with an_flag='N': AN qualification check performed, flags updated if qualified
- HTTP 200 response returned with OCMS-2000 and message "Resource created successfully"

### Error Handling

| Error Scenario | App Error Code | HTTP Status | User Message | Brief Description |
| --- | --- | --- | --- | --- |
| Missing mandatory fields | OCMS-4000 | 400 | Invalid input format or failed validation | Validation error |
| Invalid subsystem label format | OCMS-4000 | 400 | Subsystem label must be 3-8 characters | Length validation failed |
| Subsystem label not in range | OCMS-4000 | 400 | Subsystem label must be in range 030-999 | Range validation failed |
| Duplicate notice number | OCMS-2026 | 226 | Notice no Already exists | IM Used response |
| Invalid offense rule code | OCMS-2026 | 226 | Invalid Offence Rule Code | Rule code not found |
| Internal server error | OCMS-5000 | 500 | Something went wrong. Please try again later. | System error |

---

# Section 4 - Retrieving Vehicle Owner Particulars for Advisory Notices

## Use Case

1. An eNotification or a physical Advisory Notice Letter will be sent to Vehicle owners to notify them of the AN.

2. The process to determine whether to send an eNotification or physical letter involves:<br>a. Retrieving the Vehicle Owner's information from LTA<br>b. Detecting whether the Notice has been suspended<br>c. Detecting whether the Owner is in the eNotification Exclusion list

3. Following AN Verification, qualified ANs will be submitted to LTA to retrieve the Vehicle Owner information. If the vehicle is a military vehicle, the Owner is automatically identified as MINDEF and the Notice will not be sent to LTA for ownership checks.

4. After receiving the Owner information from LTA and before sending e-notification, OCMS checks:<br>a. Whether the Notice has been suspended - OCMS will halt AN processing for all suspended notices, except those suspended with TS-HST.<br>b. Whether the Owner is in the eNotification Exclusion list - If the Owner is listed, OCMS will send a physical AN letter.<br>c. Whether the Owner's registered ID type is Passport - If the Owner is a passport holder, OCMS will send a physical AN letter to the registered address returned by LTA.<br>d. Whether the vehicle registration type is 'V' (VIP), 'D' (Diplomatic) or 'I' (Military) - If the Owner is one of these 3 types, OCMS will send a physical AN letter.

5. To send an eNotification, OCMS queries DataHive to retrieve the mobile numbers of individuals or email addresses for business entities.

6. To send an AN Letter, OCMS queries MHA and/or DataHive (based on the Owner's ID type) to retrieve the latest registered address.

## Retrieve Owner Particulars Flow

![Flow Diagram](./OCMS10_Flowchart.drawio - Tab 3.4)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Definition | Brief Description |
| --- | --- | --- |
| Start | Owner retrieval | Begin vehicle owner particulars retrieval for qualified AN |
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

### Data Mapping

This data mapping documents the UPDATE operation performed when storing owner particulars from LTA/DataHive/MHA APIs.

| Zone | Database Table | Field Name | Update Value |
| --- | --- | --- | --- |
| Intranet | ocms_valid_offence_notice | notice_no | SELECT (lookup) |
| Intranet | ocms_valid_offence_notice | vehicle_no | SELECT (for LTA query) |
| Intranet | ocms_valid_offence_notice | vehicle_registration_type | SELECT (check military/VIP) |
| Intranet | ocms_valid_offence_notice | suspension_type | SELECT (halt if suspended) |
| Intranet | ocms_valid_offence_notice | owner_name | UPDATE from LTA response |
| Intranet | ocms_valid_offence_notice | owner_nric | UPDATE from LTA response |
| Intranet | ocms_valid_offence_notice | owner_mobile | UPDATE from DataHive response |
| Intranet | ocms_valid_offence_notice | owner_email | UPDATE from DataHive response |
| Intranet | ocms_valid_offence_notice | owner_address | UPDATE from MHA response |
| Intranet | ocms_valid_offence_notice | upd_user_id | ocmsiz_app_conn |
| Intranet | ocms_valid_offence_notice | upd_date | Current timestamp |

### Success Outcome

- Vehicle owner information retrieved successfully from LTA (or MINDEF set for military vehicles)
- Owner details stored in database
- eNotification eligibility determined based on contact availability and exclusion list
- Process proceeds to eAN sending or AN Letter generation

### Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| LTA API timeout | LTA API response exceeds 30 seconds | Retry 3 times with exponential backoff, then manual review queue |
| LTA owner not found | LTA returns no owner information | Add to manual review queue |
| DataHive no contact | DataHive returns no mobile or email | Proceed to AN Letter flow |
| MHA address not found | MHA returns no registered address | Add to manual review queue |
| Notice suspended | Notice has suspension type other than TS-HST | Halt AN processing |

---

# Section 5 - Sending eNotifications for AN (eAN)

## Use Case

1. OCMS eNotifications for Advisory Notices (eAN) are sent to Vehicle Owners via SMS or email to notify Owners of the Offences and inform them that they face penalties if they commit another offence.

2. If the Vehicle Owner is an individual, the eAN is sent as an SMS.

3. If the Vehicle Owner is a business entity, the eAN is sent as an email.

4. In the event that the Vehicle Owner is not eligible to receive an eNotification, OCMS will send an AN Letter instead. Refer to Section 6 of this document.

## Send eNotification Flow

![Flow Diagram](./OCMS10_Flowchart.drawio - Tab 3.5)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Definition | Brief Description |
| --- | --- | --- |
| Start | eAN sending | Begin eNotification sending process for qualified AN |
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

### Data Mapping

This data mapping documents the SELECT and UPDATE operations performed when sending eNotification.

| Zone | Database Table | Field Name | Operation/Update Value |
| --- | --- | --- | --- |
| Intranet | ocms_valid_offence_notice | notice_no | SELECT (lookup) |
| Intranet | ocms_valid_offence_notice | vehicle_no | SELECT (for message) |
| Intranet | ocms_valid_offence_notice | computer_rule_code | SELECT (for message) |
| Intranet | ocms_valid_offence_notice | composition_amount | SELECT (for message) |
| Intranet | ocms_valid_offence_notice | notice_date_and_time | SELECT (for message) |
| Intranet | ocms_valid_offence_notice | pp_name | SELECT (for message) |
| Intranet | ocms_valid_offence_notice | owner_mobile | SELECT (SMS recipient) |
| Intranet | ocms_valid_offence_notice | owner_email | SELECT (Email recipient) |
| Intranet | ocms_valid_offence_notice | ean_sent_flag | UPDATE to 'Y' |
| Intranet | ocms_valid_offence_notice | ean_sent_timestamp | UPDATE to current timestamp |
| Intranet | ocms_valid_offence_notice | notification_method | UPDATE to 'SMS' or 'EMAIL' |
| Intranet | ocms_valid_offence_notice | upd_user_id | ocmsiz_app_conn |
| Intranet | ocms_valid_offence_notice | upd_date | Current timestamp |

### Success Outcome

- eAN message generated with notice details
- SMS sent successfully to vehicle owner's mobile (for individuals)
- Email sent successfully to vehicle owner's email (for business entities)
- eAN sent status updated in database
- Process proceeds to PS-ANS suspension

### Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| SMS Gateway timeout | SMS API response exceeds 10 seconds | Retry 3 times, if fails proceed to email |
| SMS send failed | SMS Gateway returns error | Retry 3 times, if fails proceed to email or letter |
| Email Service timeout | Email API response exceeds 10 seconds | Retry 3 times, if fails proceed to letter |
| Email send failed | Email Service returns error | Retry 3 times, if fails proceed to AN Letter |
| All notifications failed | Both SMS and email failed after retries | Proceed to AN Letter sending (Section 6) |

---

# Section 6 - Sending AN Letter

## Use Case

1. OCMS sends the AN Letter to Vehicle Owners when any one of the following conditions is met:<br>a. The Owner is listed in the eNotification Exclusion List database<br>b. The Owner's contact information (mobile number or email address) cannot be retrieved from DataHive<br>c. eNotification sending and the subsequent re-send failed<br>d. Passport, VIP, diplomatic, military vehicle owners

## Send AN Letter Flow

![Flow Diagram](./OCMS10_Flowchart.drawio - Tab 3.6)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Definition | Brief Description |
| --- | --- | --- |
| Start | AN letter sending | Begin AN letter generation and sending process |
| Retrieve Notice Details | Get notice info | Retrieve notice number, offense details, composition amount from database |
| Retrieve Owner Address | Get address info | Retrieve registered address from MHA API response |
| Retrieve Owner Name | Get owner name | Retrieve vehicle owner name from LTA API response |
| Generate Letter Template | Create letter | Generate AN letter PDF/XML with notice details, owner address, letter content |
| Populate Letter Fields | Fill template | Populate letter with notice number, vehicle number, offense details, owner name/address |
| Format Letter Content | Format PDF/XML | Format letter content according to SLIFT requirements |
| Call SLIFT API | Submit for printing | Submit letter to SLIFT/SFTP for printing and mailing |
| Decision: SLIFT Success? | Submission result | Check if SLIFT accepted letter submission |
| Update Letter Status | Update database | Update notice with letter_sent_flag='Y', sent_timestamp, submission_id |
| Log Letter Sent | Completion logging | Log AN letter sent status, timestamp, recipient |
| Retry Submission | Retry logic | Retry failed submission (max 3 times with exponential backoff) |
| Manual Review Queue | Manual handling | Add to manual review queue if all retries fail |
| End | Letter complete | AN letter sending process finished |

### Data Mapping

This data mapping documents the SELECT and UPDATE operations performed when sending AN Letter via SLIFT.

| Zone | Database Table | Field Name | Operation/Update Value |
| --- | --- | --- | --- |
| Intranet | ocms_valid_offence_notice | notice_no | SELECT (lookup) |
| Intranet | ocms_valid_offence_notice | vehicle_no | SELECT (for letter) |
| Intranet | ocms_valid_offence_notice | computer_rule_code | SELECT (for letter) |
| Intranet | ocms_valid_offence_notice | composition_amount | SELECT (for letter) |
| Intranet | ocms_valid_offence_notice | notice_date_and_time | SELECT (for letter) |
| Intranet | ocms_valid_offence_notice | pp_name | SELECT (for letter) |
| Intranet | ocms_valid_offence_notice | owner_name | SELECT (for letter) |
| Intranet | ocms_valid_offence_notice | owner_address | SELECT (for letter) |
| Intranet | ocms_valid_offence_notice | letter_sent_flag | UPDATE to 'Y' |
| Intranet | ocms_valid_offence_notice | letter_sent_timestamp | UPDATE to current timestamp |
| Intranet | ocms_valid_offence_notice | slift_submission_id | UPDATE from SLIFT response |
| Intranet | ocms_valid_offence_notice | upd_user_id | ocmsiz_app_conn |
| Intranet | ocms_valid_offence_notice | upd_date | Current timestamp |

### Success Outcome

- AN letter PDF/XML generated successfully with all required details
- Letter submitted to SLIFT/SFTP for printing and mailing
- Letter sent status updated in database with submission ID
- Process proceeds to PS-ANS suspension

### Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Letter generation failed | PDF/XML generation error | Log error, retry generation, manual review if fails |
| SLIFT API timeout | SLIFT response exceeds 30 seconds | Retry 3 times with exponential backoff |
| SLIFT submission rejected | SLIFT validation error | Log error with reason, manual review queue |
| File too large | Letter file exceeds size limit | Compress PDF, retry, manual review if compression fails |
| All retries failed | SLIFT submission fails after 3 retries | Add to manual review queue |

---

# Section 7 - Suspending Notices with PS-ANS

## Use Case

1. OCMS automatically suspends AN Notices with PS-ANS after the following processing steps are completed:<br>a. The eAN eNotification has been sent successfully to the Vehicle Owner<br>b. The AN Letter has been deposited into the SFTP for the printing vendor

2. OCMS OICs can also manually suspend Notices with PS-ANS via the OCMS Staff Portal.

3. The PS-ANS suspension indicates that OCMS will no longer process the AN.

## Suspend PS-ANS Flow

![Flow Diagram](./OCMS10_Flowchart.drawio - Tab 3.7)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Definition | Brief Description |
| --- | --- | --- |
| Start | Suspension process | Begin PS-ANS suspension creation for qualified AN |
| Retrieve Notice Details | Get notice info | Retrieve notice number, notice date from database |
| Calculate Suspension Date | Set suspension date | Set date_of_suspension = current date |
| Calculate Revival Date | Set revival date | Calculate due_date_of_revival = current date + 30 days (configurable) |
| Generate SR Number | Create serial number | Generate sequential sr_no for suspension record |
| Create Suspension Record | Insert suspension | Insert record into ocms_suspended_notice table |
| Update Notice with Suspension | Update notice | Update ocms_valid_offence_notice with suspension details |
| Sync to Internet DB | Mirror record | Insert/update suspension record in eocms_valid_offence_notice (Internet) |
| Log Suspension Created | Logging | Log PS-ANS suspension creation with timestamp |
| End | Suspension complete | PS-ANS suspension process finished |

### Data Mapping

This data mapping documents the INSERT operation performed on database tables when suspending a notice with PS-ANS.

| Zone | Database Table | Field Name | Insert Value |
| --- | --- | --- | --- |
| Intranet | ocms_suspended_notice | notice_no | Notice number |
| Intranet | ocms_suspended_notice | date_of_suspension | Current timestamp |
| Intranet | ocms_suspended_notice | sr_no | Sequential number |
| Intranet | ocms_suspended_notice | suspension_type | PS |
| Intranet | ocms_suspended_notice | reason_of_suspension | ANS |
| Intranet | ocms_suspended_notice | suspension_source | ocmsiz_app_conn |
| Intranet | ocms_suspended_notice | officer_authorising_suspension | ocmsizmgr_conn |
| Intranet | ocms_suspended_notice | due_date_of_revival | Current date + 30 days |
| Intranet | ocms_suspended_notice | suspension_remarks | Advisory Notice detected - qualified AN |
| Intranet | ocms_suspended_notice | cre_user_id | ocmsiz_app_conn |
| Intranet | ocms_suspended_notice | cre_date | Current timestamp |
| Intranet | ocms_valid_offence_notice | suspension_type | PS |
| Intranet | ocms_valid_offence_notice | epr_reason_of_suspension | ANS |
| Intranet | ocms_valid_offence_notice | epr_date_of_suspension | Current timestamp |
| Intranet | ocms_valid_offence_notice | due_date_of_revival | Current date + 30 days |
| Intranet | ocms_valid_offence_notice | upd_user_id | ocmsiz_app_conn |
| Intranet | ocms_valid_offence_notice | upd_date | Current timestamp |
| Internet | eocms_valid_offence_notice | suspension_type | PS |
| Internet | eocms_valid_offence_notice | upd_user_id | ocmsez_app_conn |
| Internet | eocms_valid_offence_notice | upd_date | Current timestamp |

**Notes:**
- Audit user field `ocmsiz_app_conn` is used for intranet zone
- Audit user field `ocmsez_app_conn` is used for internet zone
- Insert order: ocms_suspended_notice first, then ocms_valid_offence_notice

### Success Outcome

- PS-ANS suspension record created in ocms_suspended_notice table
- ocms_valid_offence_notice updated with suspension_type='PS' and epr_reason_of_suspension='ANS'
- Revival date calculated and set (current date + 30 days)
- Suspension synced to Internet database (eocms_valid_offence_notice)
- AN processing completed

### Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Suspension insert failed | SQLException during ocms_suspended_notice insert | Log error, rollback, retry insert |
| Notice update failed | SQLException during ocms_valid_offence_notice update | Log error, rollback, ensure data consistency |
| Internet sync failed | Sync to eocms_valid_offence_notice failed | Log error, retry sync, manual verification |
| Revival date calculation error | Date calculation error | Use default 30 days, log warning |

---

# Section 8 - Reports for ANS

## Use Case

1. OCMS generates 2 types of reports for Advisory Notices:<br>a. Ad-hoc ANS Report<br>b. Reconciliation report on the number of AN Letters sent to printing vendor and report on the number of AN Letters printed by the printing vendor and any errors.

2. The Ad-Hoc ANS report is initiated by the OIC via the Staff Portal's Report Module.

3. The Report provides the list of ANs that match the search criteria.

## Generate ANS Reports Flow

![Flow Diagram](./OCMS10_Flowchart.drawio - Tab 3.8)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Definition | Brief Description |
| --- | --- | --- |
| Start | Report generation | Staff requests ANS report from portal |
| Receive Report Parameters | Get report criteria | Receive date range, vehicle number, subsystem filter from user |
| Validate Parameters | Parameter validation | Validate date range (max 31 days), required fields |
| Decision: Valid Parameters? | Validation result | Check if parameters are valid |
| Query ANS Suspension Data | Database query | Query ocms_suspended_notice joined with ocms_valid_offence_notice for reason_of_suspension='ANS' |
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

### Data Mapping

This data mapping documents the SELECT operation performed when generating ANS reports.

| Zone | Database Table | Field Name | Operation |
| --- | --- | --- | --- |
| Intranet | ocms_suspended_notice | notice_no | SELECT (join key) |
| Intranet | ocms_suspended_notice | date_of_suspension | SELECT (report field) |
| Intranet | ocms_suspended_notice | due_date_of_revival | SELECT (report field) |
| Intranet | ocms_suspended_notice | suspension_remarks | SELECT (report field) |
| Intranet | ocms_suspended_notice | reason_of_suspension | SELECT WHERE = 'ANS' |
| Intranet | ocms_valid_offence_notice | vehicle_no | SELECT (filter/report field) |
| Intranet | ocms_valid_offence_notice | subsystem_label | SELECT (filter/report field) |
| Intranet | ocms_valid_offence_notice | computer_rule_code | SELECT (report field) |
| Intranet | ocms_valid_offence_notice | composition_amount | SELECT (report field) |
| Intranet | ocms_valid_offence_notice | notice_date_and_time | SELECT (filter/report field) |
| Intranet | ocms_valid_offence_notice | pp_code | SELECT (report field) |
| Intranet | ocms_valid_offence_notice | pp_name | SELECT (report field) |

### Success Outcome

- Report parameters validated successfully
- ANS suspension data queried and filtered
- Report generated in Excel or PDF format
- Report downloaded by user
- Report generation logged

### Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Invalid date range | Date range exceeds 31 days | Return error "Date range exceeds 31 days" |
| Missing required fields | Required parameters not provided | Return error message for missing fields |
| Database query error | SQLException during query | Log error, return error message |
| No records found | Query returns empty result | Return friendly message "No records found for the selected criteria" |
| Report generation error | File creation failed | Log error, return error message |

---

# Section 9 - Sending Unqualified AN to REPCCS and CES

## Use Case

1. OCMS sends a list of vehicle numbers that do not qualify for ANS to REPCCS and CES so that their systems can use the list to decide whether a vehicle is eligible for AN.

2. The list is sent to REPCCS and CES at a scheduled time via SFTP file daily.

## Send Unqualified AN List Flow

![Flow Diagram](./OCMS10_Flowchart.drawio - Tab 3.9)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Definition | Brief Description |
| --- | --- | --- |
| Start | Cron job triggered | Daily scheduled job to send unqualified AN list |
| Calculate Date Range | Determine period | Calculate date range for notices to process (previous day) |
| Query Unqualified ANs | Database query | Query notices where an_flag='N' or NULL for REPCCS/CES subsystems |
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

**Unqualified AN Conditions (per FD v1.2 Section 10.2):**

| Step | Condition | Add to Unqualified ANS vehicle? |
| --- | --- | --- |
| 1 | The vehicle doesn't have the offence in last 24 months | No |
| 2 | If the vehicle has past offence in last 24 months, check the suspension type of the notice | |
| 2.a | If the notice is not under suspension or suspended with TS (temporary suspension), add the vehicle to Unqualified list | Yes |
| 2.b | If PS, check the ANS_PS_Reason for the following PS reasons: CAN, CFA, DBB, VST. If PS reason for ALL offence records within the last 2 years does not fall in any of the ANS_PS_Reasons above, add vehicle number to unqualified ANS list | Yes |

### Data Mapping

This data mapping documents the SELECT and UPDATE operations performed when sending unqualified AN list to REPCCS/CES.

| Zone | Database Table | Field Name | Operation/Update Value |
| --- | --- | --- | --- |
| Intranet | ocms_valid_offence_notice | notice_no | SELECT (for list) |
| Intranet | ocms_valid_offence_notice | vehicle_no | SELECT (for list) |
| Intranet | ocms_valid_offence_notice | subsystem_label | SELECT WHERE REPCCS or CES |
| Intranet | ocms_valid_offence_notice | notice_date_and_time | SELECT (date range filter) |
| Intranet | ocms_valid_offence_notice | an_flag | SELECT WHERE = 'N' or NULL |
| Intranet | ocms_valid_offence_notice | created_date | SELECT (date range filter) |
| Intranet | ocms_valid_offence_notice | unqualified_list_sent_flag | UPDATE to 'Y' |
| Intranet | ocms_valid_offence_notice | unqualified_list_sent_timestamp | UPDATE to current timestamp |
| Intranet | ocms_valid_offence_notice | upd_user_id | ocmsiz_app_conn |
| Intranet | ocms_valid_offence_notice | upd_date | Current timestamp |

### Success Outcome

- Unqualified AN list generated for both REPCCS and CES
- List transmitted successfully to REPCCS and CES via SFTP
- Notice status updated with unqualified_list_sent_flag='Y'
- Cron job completion logged with summary

### Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| REPCCS API timeout | REPCCS API response exceeds 60 seconds | Retry 3 times with exponential backoff, alert admin |
| REPCCS API error | REPCCS returns error | Log error, retry 3 times, alert admin after failures |
| CES API timeout | CES API response exceeds 60 seconds | Retry 3 times with exponential backoff, alert admin |
| CES API error | CES returns error | Log error, retry 3 times, alert admin after failures |
| Database update error | SQLException during status update | Log error, continue to next batch |

---

**End of Document**

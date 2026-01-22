# OCMS 10 - Advisory Notices Processing

**Prepared by**

Accenture

---

## Version History

| Version | Updated By | Date | Changes |
| --- | --- | --- | --- |
| v1.0 | Mubiyarto Wibisono | 16/01/2026 | Document Initiation |

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
| 2.2.1 | Diagram Flow Image | [X] |
| 2.3 | Exemption Rules Check | [X] |
| 2.3.1 | Diagram Flow Image | [X] |
| 2.3.2 | Success Outcome | [X] |
| 2.3.3 | Error Handling | [X] |
| 2.4 | Past Offences Check | [X] |
| 2.4.1 | Diagram Flow Image | [X] |
| 2.4.2 | Success Outcome | [X] |
| 2.4.3 | Error Handling | [X] |
| 3 | Receiving AN Offence Data | [X] |
| 3.1 | Use Case | [X] |
| 3.2 | Receiving eligible ANs from REPCCS | [X] |
| 3.2.1 | Diagram Flow Image | [X] |
| 3.2.2 | API Specification | [X] |
| 3.2.3 | Data Mapping | [X] |
| 3.2.4 | Success Outcome | [X] |
| 3.2.5 | Error Handling | [X] |
| 3.3 | Receiving eligible ANs from CES | [X] |
| 3.3.1 | Diagram Flow Image | [X] |
| 3.3.2 | API Specification | [X] |
| 3.3.3 | Data Mapping | [X] |
| 3.3.4 | Success Outcome | [X] |
| 3.3.5 | Error Handling | [X] |
| 4 | Retrieving Vehicle Owner Particulars for Advisory Notices | [X] |
| 4.1 | Use Case | [X] |
| 4.2 | Retrieve Owner Particulars Flow | [X] |
| 4.3 | Data Mapping | [X] |
| 4.3.1 | Storing owner particulars from LTA/DataHive/MHA | [X] |
| 4.4 | Success Outcome | [X] |
| 4.5 | Error Handling | [X] |
| 5 | Sending eNotifications for AN (eAN) | [X] |
| 5.1 | Use Case | [X] |
| 5.2 | Send eNotification Flow | [X] |
| 5.3 | Data Mapping | [X] |
| 5.3.1 | Update after Sending email | [X] |
| 5.3.2 | Update after sending SMS | [X] |
| 5.3.3 | Suspend after sending eNotification | [X] |
| 5.4 | Success Outcome | [X] |
| 5.5 | Error Handling | [X] |
| 6 | Sending AN Letter | [X] |
| 6.1 | Use Case | [X] |
| 6.2 | Send AN Letter Flow | [X] |
| 6.3 | Data Mapping | [X] |
| 6.3.1 | Insert after sending AN Letter | [X] |
| 6.4 | Success Outcome | [X] |
| 6.5 | Error Handling | [X] |
| 7 | Suspending Notices with PS-ANS | [X] |
| 7.1 | Use Case | [X] |
| 7.2 | Suspend PS-ANS Flow | [X] |
| 7.3 | Data Mapping | [X] |
| 7.3.1 | Suspend notice PS - ANS | [X] |
| 7.3.2 | Update after suspend notice | [X] |
| 7.4 | Success Outcome | [X] |
| 7.5 | Error Handling | [X] |
| 8 | Reports for ANS | [X] |
| 8.1 | Use Case | [X] |
| 8.2 | Generate ANS Reports Flow | [X] |
| 8.3 | Data Mapping | [X] |
| 8.3.1 | Generating ANS Reports | [X] |
| 8.4 | Success Outcome | [X] |
| 8.5 | Error Handling | [X] |
| 9 | Sending Unqualified AN to REPCCS and CES | [X] |
| 9.1 | Use Case | [X] |
| 9.2 | Send Unqualified AN List Flow | [X] |
| 9.3 | Data Mapping | [X] |
| 9.3.1 | Sending unqualified AN list | [X] |
| 9.4 | Success Outcome | [X] |
| 9.5 | Error Handling | [X] |

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
| Notice Creation | Create notice record | Refer to Technical Document OCMS 5 |
| AN Verification | Check qualification | Refer to Section 2 |
| Send eNotification | Electronic notification | Refer to Section 5 |
| Send AN Letter | Physical letter | Refer to Section 6 |
| Suspend with PS-ANS | Create suspension | Create PS-ANS suspension record for qualified AN |
| End | Process complete | AN processing completed |

---

# Section 2 - AN Verification

## Use Case

1. OCMS performs a verification process to determine whether a Notice qualifies as an AN.
2. The objective of the AN Qualification check is to verify if the Notice qualifies as an AN.
3. The AN Qualification process checks the Offence Rule Code and Vehicle Number of the new notice to determine that it qualifies as an AN:
   - The Offence Rule Code is not listed in the ANS exemption list, OR if the Offence Rule Code is listed, the offence date is not within the active exemption period and does not bear the specific rule remark and composition amount listed; AND
   - The vehicle has no past Offence Notices created within the last 24 months from the current date, OR
   - The vehicle has one or more past Offence Notices created within the last 24 months from the current date, AND all past Offence Notices were suspended with ANS PS reason codes (CAN, CFA, DBB or VST).

## AN Qualification Check

### Diagram Flow Image

![Flow Diagram](./OCMS10_Flowchart.drawio - Tab 3.1)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Definition | Brief Description |
| --- | --- | --- |
| Check Offense Type | Validate offense type | Offense type must be 'O' |
| Check Vehicle Type | Validate vehicle type | Vehicle registration type must be in [S, D, V, I]. |
| Check Exemption Rules | Exemption validation | Refer to Section 2.3 |
| Check Past Offense | 24-month window check | Refer to Section 2.4 |
| Return Qualified | Pass qualification | Return qualified=true with AN details |
| Return Not Qualified | Fail qualification | Return qualified=false with reason |
| End | Check complete | Qualification result returned |

## Exemption Rules Check

### Diagram Flow Image

![Flow Diagram](./OCMS10_Flowchart.drawio - Tab 3.1.1)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Definition | Brief Description |
| --- | --- | --- |
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

### Success Outcome

- Rule Code not found in exemption list: Notice IS ELIGIBLE for ANS
- Rule Code found but offence date not within exemption period: Notice IS ELIGIBLE for ANS
- Rule Code 20412 found but composition_amount != $80: Notice IS ELIGIBLE for ANS
- Rule Code 11210 found but remarks does not contain 'Loading Bay' or 'Handicap Lot': Notice IS ELIGIBLE for ANS
- Sub-flow returns result to main AN Qualification Process Flow

### Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Exemption record found with matching date | Rule code in exemption list within effective period | Notice NOT ELIGIBLE for ANS, return to standard processing |
| Rule 20412 with $80 amount | Specific exemption for Rule 20412 | Notice NOT ELIGIBLE for ANS |
| Rule 11210 with Loading Bay remark | Specific exemption for Rule 11210 | Notice NOT ELIGIBLE for ANS |
| Rule 11210 with Handicap Lot remark | Specific exemption for Rule 11210 | Notice NOT ELIGIBLE for ANS |
| Database query error | SQLException during exemption check | Log error, treat as eligible (fail-safe) |

## Past Offences Check

### Diagram Flow Image

![Flow Diagram](./OCMS10_Flowchart.drawio - Tab 3.1.2)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Definition | Brief Description |
| --- | --- | --- |
| Query Past Offences | Database query | Query ocms_valid_offence_notice for offences in last 24 months |
| Any Record Found? | Past offence check | Check if any past offence records found |
| Query ANS PS Reasons | Get valid reasons | Query ocms_standard_code for active ANS PS Reasons |
| Batch Check | Suspension check | For each past offence, check if it is suspended with one of the ANS PS Reasons |
| All Suspended? | Decision | Check if ALL past Notices are suspended with ANS PS Reasons |
| Return Qualified | Qualified result | Notice QUALIFIES as AN |
| Return Not Qualified | Not qualified result | Notice does NOT QUALIFY as AN |
| End | Sub-flow complete | Past Offences Check completed |

### Success Outcome

- No past offences found in last 24 months: Notice QUALIFIES as AN
- Past offences found AND ALL are suspended with ANS PS Reasons (CAN/CFA/DBB/VST): Notice QUALIFIES as AN
- Sub-flow returns result to main AN Qualification Process Flow

### Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Past offence not suspended | Vehicle has past offence without suspension | Notice does NOT QUALIFY as AN |
| Past offence suspended with non-ANS reason | Suspended with reason other than CAN/CFA/DBB/VST | Notice does NOT QUALIFY as AN |
| Past offence with TS suspension | Vehicle has past offence with Temporary Suspension | Notice does NOT QUALIFY as AN |
| Database query error | SQLException during past offence check | Log error, treat as not qualified |

---

# Section 3 - Receiving AN Offence Data

## Use Case

1. OCMS receives raw Offence data sent by the following backend enforcement systems for Notice creation and processing:
   - REPCCS
   - CES
2. REPCCS and CES use the ANSFlag field to indicate whether the Notice is eligible as an AN.
3. While OCMS receives the ANS Flag indicator sent by CES, OCMS will set the ANs flag directly based on CES data and will not perform the ANs Qualification check. If the CES AN indicator = 'Y', the notice shall be created with ANs flag = 'Y' and subsequently suspended with status/reason PS-ANS.
4. While OCMS receives the ANSFlag indicator sent by REPCCS, OCMS will not process the indicator value. It will disregard the indicator value sent by the backend enforcement systems. After AN qualification check for the notice, OCMS will update AN flag:
   - IF AN flag is 'Y' and the notice is not eligible for ANS at time of checking in OCMS, OCMS will update AN flag from 'Y' to 'N' and vice versa.
   - IF AN flag is 'Y' and the notice is eligible for ANS, OCMS won't update AN flag.
5. OCMS will perform the AN Qualification processes for every Notice sent by REPCCS and other sources (OCMS Staff Portal and PLUS Staff Portal).

## Receiving eligible ANs from REPCCS

### Diagram Flow Image

![Flow Diagram](./OCMS10_Flowchart.drawio - Tab 3.2)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Definition | Brief Description |
| --- | --- | --- |
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

### API Specification

**REPCCS Webhook API**

Refer to OCMS 3 Technical Document section 1.1.3

### Data Mapping

Refer to OCMS 3 Technical Document section 2.1

### Success Outcome

Refer to OCMS 3 Technical Document section 1.1.4

### Error Handling

Refer to OCMS 3 Technical Document section 1.1.3

## Receiving eligible ANs from CES

### Diagram Flow Image

![Flow Diagram](./OCMS10_Flowchart.drawio - Tab 3.3)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Definition | Brief Description |
| --- | --- | --- |
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

### API Specification

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

### Data Mapping

**Notice from CES**

| Zone | Database Table | Field Name | Correct Value |
| --- | --- | --- | --- |
| Intranet & Internet | ocms_valid_offence_notice | notice_no | 823000001G |
| Intranet & Internet | ocms_valid_offence_notice | vehicle_no | MGC1234X |
| Intranet & Internet | ocms_valid_offence_notice | computer_rule_code | 30301 |
| Intranet & Internet | ocms_valid_offence_notice | composition_amount | 70,00 |
| Intranet & Internet | ocms_valid_offence_notice | notice_date_and_time | 2026-01-11T14:30:00 |
| Intranet & Internet | ocms_valid_offence_notice | offense_notice_type | O |
| Intranet & Internet | ocms_valid_offence_notice | vehicle_category | I |
| Intranet & Internet | ocms_valid_offence_notice | vehicle_registration_type | S |
| Intranet & Internet | ocms_valid_offence_notice | pp_code | A002 |
| Intranet & Internet | ocms_valid_offence_notice | pp_name | ALBERT road |
| Intranet & Internet | ocms_valid_offence_notice | subsystem_label | 3030 |
| Intranet & Internet | ocms_valid_offence_notice | an_flag | Y |
| Intranet & Internet | ocms_valid_offence_notice | payment_acceptance_allowed | Y |
| Intranet & Internet | ocms_valid_offence_notice | cre_user_id | ocmsiz_app_conn |
| Intranet & Internet | ocms_valid_offence_notice | cre_date | 2026-01-11T14:30:00 |
| Intranet | ocms_offence_notice_detail | notice_no | 823000001G |
| Intranet | ocms_suspended_notice | notice_no | 823000001G |
| Intranet | ocms_suspended_notice | date_of_suspension | 2026-01-11T14:30:00 |
| Intranet | ocms_suspended_notice | sr_no | 112 |
| Intranet | ocms_suspended_notice | suspension_type | PS |
| Intranet | ocms_suspended_notice | reason_of_suspension | ANS |

### Success Outcome

- Notice data validated successfully (mandatory fields, subsystem label format, rule code)
- No duplicate notice detected
- Notice records created in ocms_valid_offence_notice and ocms_offence_notice_detail
- For CES with an_flag='Y': PS-ANS suspension created immediately without AN qualification check
- For CES with an_flag='N': AN qualification check performed, flags updated if qualified

### Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Missing mandatory fields | Invalid input format or failed validation | Validation error |
| Invalid subsystem label format | Subsystem label must be 3-8 characters | Length validation failed |
| Invalid offense rule code | Invalid Offence Rule Code | Rule code not found |
| Internal server error | Something went wrong. Please try again later. | System error |

---

# Section 4 - Retrieving Vehicle Owner Particulars for Advisory Notices

## Use Case

1. An eNotification or a physical Advisory Notice Letter will be sent to Vehicle owners to notify them of the AN.
2. The process to determine whether to send an eNotification or physical letter involves:
   - Retrieving the Vehicle Owner's information from LTA
   - Detecting whether the Notice has been suspended
   - Detecting whether the Owner is in the eNotification Exclusion list
3. Following AN Verification, qualified ANs will be submitted to LTA to retrieve the Vehicle Owner information. If the vehicle is a military vehicle, the Owner is automatically identified as MINDEF and the Notice will not be sent to LTA for ownership checks.
4. After receiving the Owner information from LTA and before sending e-notification, OCMS checks:
   - Whether the Notice has been suspended - OCMS will halt AN processing for all suspended notices, except those suspended with TS-HST.
   - Whether the Owner is in the eNotification Exclusion list - If the Owner is listed, OCMS will send a physical AN letter.
   - Whether the Owner's registered ID type is Passport - If the Owner is a passport holder, OCMS will send a physical AN letter to the registered address returned by LTA.
   - Whether the vehicle registration type is 'V' (VIP), 'D' (Diplomatic) or 'I' (Military) - If the Owner is one of these 3 types, OCMS will send a physical AN letter.
5. To send an eNotification, OCMS queries DataHive to retrieve the mobile numbers of individuals or email addresses for business entities.
6. To send an AN Letter, OCMS queries MHA and/or DataHive (based on the Owner's ID type) to retrieve the latest registered address.

## Retrieve Owner Particulars Flow

![Flow Diagram](./OCMS10_Flowchart.drawio - Tab 3.4)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Definition | Brief Description |
| --- | --- | --- |
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

## Data Mapping

### Storing owner particulars from LTA/DataHive/MHA

| Zone | Database Table | Field Name | Update Value |
| --- | --- | --- | --- |
| Intranet | ocms_offence_notice_owner_driver | notice_no | 500400058A |
| Intranet | ocms_offence_notice_owner_driver | owner_driver_indicator | O |
| Intranet | ocms_offence_notice_owner_driver | owner_nric_no | S8616980J |
| Intranet | ocms_offence_notice_owner_driver | owner_name | May |
| Intranet | ocms_offence_notice_owner_driver | owner_blk_hse_no | 12 |
| Intranet | ocms_offence_notice_owner_driver | owner_street | Arab Street |
| Intranet | ocms_offence_notice_owner_driver | owner_floor | 1 |
| Intranet | ocms_offence_notice_owner_driver | owner_unit | 112 |
| Intranet | ocms_offence_notice_owner_driver | owner_bldg | Arab Tower |
| Intranet | ocms_offence_notice_owner_driver | owner_postal_code | 40411 |
| Intranet | ocms_offence_notice_owner_driver | cre_user_id | ocmsiz_app_conn |
| Intranet | ocms_offence_notice_owner_driver | cre_date | 2026-01-11T14:30:00 |

## Success Outcome

- Vehicle owner information retrieved successfully from LTA (or MINDEF set for military vehicles)
- Owner details stored in database
- eNotification eligibility determined based on contact availability and exclusion list
- Process proceeds to eAN sending or AN Letter generation

## Error Handling

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

## Data Mapping

### Update after Sending email

| Zone | Database Table | Field Name | Correct Value |
| --- | --- | --- | --- |
| Intranet | ocms_email_notification_records | notice_no | 500400058A |
| Intranet | ocms_email_notification_records | processing_stage | RD1 |
| Intranet | ocms_email_notification_records | content | enotification for notices |
| Intranet | ocms_email_notification_records | date_sent | 2026-01-11T14:30:00 |
| Intranet | ocms_email_notification_records | email_addr | may@google.com |
| Intranet | ocms_email_notification_records | status | sent |
| Intranet | ocms_email_notification_records | subject | Notification Test |
| Intranet | ocms_email_notification_records | cre_date | ocmsiz_app_conn |
| Intranet | ocms_email_notification_records | cre_user_id | 2026-01-11T14:30:00 |

### Update after sending SMS

| Zone | Database Table | Field Name | Correct Value |
| --- | --- | --- | --- |
| Intranet | ocms_sms_notification_records | notice_no | 500400058A |
| Intranet | ocms_sms_notification_records | processing_stage | RD1 |
| Intranet | ocms_sms_notification_records | content | enotification for notices |
| Intranet | ocms_sms_notification_records | date_sent | 2026-01-11T14:30:00 |
| Intranet | ocms_sms_notification_records | mobile_no | 85645678 |
| Intranet | ocms_sms_notification_records | status | sent |
| Intranet | ocms_sms_notification_records | cre_date | ocmsiz_app_conn |
| Intranet | ocms_sms_notification_records | cre_user_id | 2026-01-11T14:30:00 |

### Suspend after sending eNotification

| Zone | Database Table | Field Name | Correct Value |
| --- | --- | --- | --- |
| Intranet | ocms_suspended_notice | notice_no | 500400058A |
| Intranet | ocms_suspended_notice | date_of_suspension | 2026-01-11T14:30:00 |
| Intranet | ocms_suspended_notice | sr_no | 1121 |
| Intranet | ocms_suspended_notice | suspension_type | PS |
| Intranet | ocms_suspended_notice | reason_of_suspension | ANS |
| Intranet | ocms_suspended_notice | suspension_source | OCMS |
| Intranet | ocms_suspended_notice | cre_date | ocmsiz_app_conn |
| Intranet | ocms_suspended_notice | cre_user_id | 2026-01-11T14:30:00 |

## Success Outcome

- eAN message generated with notice details
- SMS sent successfully to vehicle owner's mobile (for individuals)
- Email sent successfully to vehicle owner's email (for business entities)
- eAN sent status updated in database

## Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| SMS send failed | SMS Gateway returns error | Retry 3 times, if fails proceed to email or letter |
| Email Service timeout | Email API response exceeds 10 seconds | Retry 3 times, if fails proceed to letter |
| Email send failed | Email Service returns error | Retry 3 times, if fails proceed to AN Letter |
| All notifications failed | Both SMS and email failed after retries | Proceed to AN Letter sending (Section 6) |

---

# Section 6 - Sending AN Letter

## Use Case

1. OCMS sends the AN Letter to Vehicle Owners when any one of the following conditions is met:
   - The Owner is listed in the eNotification Exclusion List database
   - The Owner's contact information (mobile number or email address) cannot be retrieved from DataHive
   - eNotification sending and the subsequent re-send failed
   - Passport, VIP, diplomatic, military vehicle owners

## Send AN Letter Flow

![Flow Diagram](./OCMS10_Flowchart.drawio - Tab 3.6)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Definition | Brief Description |
| --- | --- | --- |
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

## Data Mapping

### Insert after sending AN Letter

| Zone | Database Table | Field Name | Correct Value |
| --- | --- | --- | --- |
| Intranet | ocms_an_letter | notice_no | 500400058A |
| Intranet | ocms_an_letter | date_of_processing | 2026-01-11T14:30:00 |
| Intranet | ocms_an_letter | date_of_an_letter | 2026-01-11T14:30:00 |
| Intranet | ocms_an_letter | owner_nric_no | S8616980J |
| Intranet | ocms_an_letter | owner_name | May |
| Intranet | ocms_an_letter | owner_id_type | N |
| Intranet | ocms_an_letter | owner_blk_hse_no | 12 |
| Intranet | ocms_an_letter | owner_street | Arab Street |
| Intranet | ocms_an_letter | owner_floor | 1 |
| Intranet | ocms_an_letter | owner_unit | 112 |
| Intranet | ocms_an_letter | owner_bldg | Arab Tower |
| Intranet | ocms_an_letter | owner_postal_code | 40411 |
| Intranet | ocms_an_letter | processing_stage | RD1 |
| Intranet | ocms_an_letter | cre_date | ocmsiz_app_conn |
| Intranet | ocms_an_letter | cre_user_id | 2026-01-11T14:30:00 |

## Success Outcome

- AN letter PDF/XML generated successfully with all required details
- Letter submitted to SLIFT/SFTP for printing and mailing
- Letter sent status updated in database with submission ID
- Process proceeds to PS-ANS suspension

## Error Handling

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

1. OCMS automatically suspends AN Notices with PS-ANS after the following processing steps are completed:
   - a. The eAN eNotification has been sent successfully to the Vehicle Owner
   - b. The AN Letter has been deposited into the SFTP for the printing vendor
2. OCMS OICs can also manually suspend Notices with PS-ANS via the OCMS Staff Portal.
3. The PS-ANS suspension indicates that OCMS will no longer process the AN.

## Suspend PS-ANS Flow

![Flow Diagram](./OCMS10_Flowchart.drawio - Tab 3.7)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Definition | Brief Description |
| --- | --- | --- |
| Retrieve Notice Details | Get notice info | Retrieve notice number, notice date from database |
| Generate SR Number | Create serial number | Generate sequential sr_no for suspension record |
| Create Suspension Record | Insert suspension | Insert record into ocms_suspended_notice table |
| Update Notice with Suspension | Update notice | Update ocms_valid_offence_notice with suspension details |
| Sync to Internet DB | Mirror record | Insert/update suspension record in eocms_valid_offence_notice (Internet) |
| Log Suspension Created | Logging | Log PS-ANS suspension creation with timestamp |
| End | Suspension complete | PS-ANS suspension process finished |

## Data Mapping

### Suspend notice PS - ANS

| Zone | Database Table | Field Name | Correct Value |
| --- | --- | --- | --- |
| Intranet | ocms_suspended_notice | notice_no | 500400058A |
| Intranet | ocms_suspended_notice | date_of_suspension | 2026-01-11T14:30:00 |
| Intranet | ocms_suspended_notice | sr_no | 1121 |
| Intranet | ocms_suspended_notice | suspension_type | PS |
| Intranet | ocms_suspended_notice | reason_of_suspension | ANS |
| Intranet | ocms_suspended_notice | suspension_source | OCMS |
| Intranet | ocms_suspended_notice | officer_authorising_suspension | ocmsizmgr_conn |
| Intranet | ocms_suspended_notice | due_date_of_revival | 2026-02-12T14:30:00 |
| Intranet | ocms_suspended_notice | suspension_remarks | Advisory Notice detected - qualified AN |
| Intranet | ocms_suspended_notice | cre_user_id | ocmsiz_app_conn |
| Intranet | ocms_suspended_notice | cre_date | Current timestamp |

### Update after suspend notice

| Zone | Database Table | Field Name | Correct Value |
| --- | --- | --- | --- |
| Intranet & Internet | ocms_valid_offence_notice | suspension_type | PS |
| Intranet & Internet | ocms_valid_offence_notice | epr_reason_of_suspension | ANS |
| Intranet & Internet | ocms_valid_offence_notice | epr_date_of_suspension | Current timestamp |
| Intranet & Internet | ocms_valid_offence_notice | due_date_of_revival | Current date + 30 days |
| Intranet & Internet | ocms_valid_offence_notice | upd_user_id | ocmsiz_app_conn |
| Intranet & Internet | ocms_valid_offence_notice | upd_date | Current timestamp |

## Success Outcome

- PS-ANS suspension record created in ocms_suspended_notice table
- ocms_valid_offence_notice updated with suspension_type='PS' and epr_reason_of_suspension='ANS'
- Revival date calculated and set (current date + 30 days)
- Suspension synced to Internet database (eocms_valid_offence_notice)

## Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Suspension insert failed | SQLException during ocms_suspended_notice insert | Log error, rollback, retry insert |
| Notice update failed | SQLException during ocms_valid_offence_notice update | Log error, rollback, ensure data consistency |
| Internet sync failed | Sync to eocms_valid_offence_notice failed | Log error, retry sync, manual verification |

---

# Section 8 - Reports for ANS

## Use Case

1. OCMS generates 2 types of reports for Advisory Notices:
   - a. Ad-hoc ANS Report
   - b. Reconciliation report on the number of AN Letters sent to printing vendor and report on the number of AN Letters printed by the printing vendor and any errors.
2. The Ad-Hoc ANS report is initiated by the OIC via the Staff Portal's Report Module.
3. The Report provides the list of ANs that match the search criteria.

## Generate ANS Reports Flow

![Flow Diagram](./OCMS10_Flowchart.drawio - Tab 3.8)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Definition | Brief Description |
| --- | --- | --- |
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

## Data Mapping

### Generating ANS Reports

| Zone | Database Table | Field Name | Correct Value |
| --- | --- | --- | --- |
| Intranet | ocms_suspended_notice | notice_no | 500400058A |
| Intranet | ocms_suspended_notice | date_of_suspension | 2026-01-11T14:30:00 |
| Intranet | ocms_suspended_notice | due_date_of_revival | 2026-02-12T14:30:00 |
| Intranet | ocms_suspended_notice | suspension_remarks | Advisory Notice detected - qualified AN |
| Intranet | ocms_suspended_notice | reason_of_suspension | ANS |
| Intranet | ocms_valid_offence_notice | vehicle_no | SGC1234X |
| Intranet | ocms_valid_offence_notice | subsystem_label | 004 |
| Intranet | ocms_valid_offence_notice | computer_rule_code | 10300 |
| Intranet | ocms_valid_offence_notice | composition_amount | 70,00 |
| Intranet | ocms_valid_offence_notice | notice_date_and_time | 2026-01-11T14:30:00 |
| Intranet | ocms_valid_offence_notice | pp_code | A002 |
| Intranet | ocms_valid_offence_notice | pp_name | Albert Road |

## Success Outcome

- Report parameters validated successfully
- ANS suspension data queried and filtered
- Report generated in Excel or PDF format
- Report downloaded by user
- Report generation logged

## Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| Missing required fields | Required parameters not provided | Return error message for missing fields |
| Database query error | SQLException during query | Log error, return error message |
| No records found | Query returns empty result | Return friendly message "No records found for the selected criteria" |
| Report generation error | File creation failed | Log error, return error message |

---

# Section 9 - Sending Unqualified AN to REPCCS and CES

## Use Case

1. OCMS sends a list of vehicle numbers that do not qualify for ANS to REPCCS and CES so that their systems can use the list to decide whether a vehicle is eligible for AN.
2. The list is sent to REPCCS and CES at a scheduled time via SFTP file daily.
3. Refer to Section 6 of the OCMS 2 Functional Document for the specifications of the interfacing with REPCCS.
4. The interface with CES is a new requirement/scope for OCMS 2 and will be based on the specifications for REPCCS.

## Unqualified AN Conditions

| Step | Condition | Add to Unqualified ANS vehicle? |
| --- | --- | --- |
| 1 | The vehicle doesn't have the offence in last 24 months | No |
| 2 | If the vehicle has past offence in last 24 months, check the suspension type of the notice | |
| 2.a | If the notice is not under suspension or suspended with TS (temporary suspension), add the vehicle to Unqualified list | Yes |
| 2.b | If PS, check the ANS_PS_Reason for the following PS reasons: CAN, CFA, DBB, VST. If PS reason for ALL offence records within the last 2 years does not fall in any of the ANS_PS_Reasons above, add vehicle number to unqualified ANS list | Yes |

## Send Unqualified AN List Flow

![Flow Diagram](./OCMS10_Flowchart.drawio - Tab 3.9)

NOTE: Due to page size limit, the full-sized image is appended.

| Step | Definition | Brief Description |
| --- | --- | --- |
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

## Data Mapping

### Sending unqualified AN list

| Zone | Database Table | Field Name | Operation/Update Value |
| --- | --- | --- | --- |
| Intranet | ocms_valid_offence_notice | notice_no | 500400058A |
| Intranet | ocms_valid_offence_notice | vehicle_no | SGC1234X |
| Intranet | ocms_valid_offence_notice | subsystem_label | 013339 |
| Intranet | ocms_valid_offence_notice | notice_date_and_time | 2026-01-11T14:30:00 |
| Intranet | ocms_valid_offence_notice | an_flag | Y |
| Intranet | ocms_valid_offence_notice | created_date | 2026-01-11T14:30:00 |
| Intranet | ocms_valid_offence_notice | unqualified_list_sent_flag | Y |
| Intranet | ocms_valid_offence_notice | unqualified_list_sent_timestamp | 2026-01-11T14:30:00 |
| Intranet | ocms_valid_offence_notice | upd_user_id | ocmsiz_app_conn |
| Intranet | ocms_valid_offence_notice | upd_date | 2026-01-11T14:30:00 |

## Success Outcome

- Unqualified AN list generated for both REPCCS and CES
- List transmitted successfully to REPCCS and CES via SFTP
- Notice status updated with unqualified_list_sent_flag='Y'
- Cron job completion logged with summary

## Error Handling

| Error Scenario | Definition | Brief Description |
| --- | --- | --- |
| REPCCS API timeout | REPCCS API response exceeds 60 seconds | Retry 3 times with exponential backoff, alert admin |
| REPCCS API error | REPCCS returns error | Log error, retry 3 times, alert admin after failures |
| CES API timeout | CES API response exceeds 60 seconds | Retry 3 times with exponential backoff, alert admin |
| CES API error | CES returns error | Log error, retry 3 times, alert admin after failures |
| Database update error | SQLException during status update | Log error, continue to next batch |

---

**End of Document**

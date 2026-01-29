**Section 7 – Notice Processing Flow for VIP Vehicles**

**7.1 Use Case
**

- The VIP Vehicle Notice processing flow is triggered when a Notice is created with the vehicle registration type “V”.
- VIP Vehicle Notices for Parking Offences (Type O) and Payment Evasion Offences (Type E) will not follow the standard processing flows for these offence types. Instead, they will be handled through the VIP Vehicle Notice processing flow.
- Only Unauthorised Parking Lot Offences (Type U) will continue to follow the UPL processing flow.
- The VIP Vehicle processing flow will:Once a notice/advisory notice is created for a VIP vehicle, it is immediately suspended under the temporary suspension code ‘OLD’ for 21 days (TS code and period of suspension will be configurable in the Suspension Reason UI screen in OCMS staff portal and stored in OCMS_Suspension_Reason table), during which the OIC will investigate the notice.
- Once the temporary suspension period has ended and the notice is revived, OCMS will perform a direct LTA interface to check for vehicle ownership details. It will then check MHA and DataHive for vehicle owner’s latest registered address and other information and proceed to the RD1 stage, where the first reminder letter/advisory notice letter is generated and sent for the offence.
- Bypass the ENA stage and eNotifications (SMS/email) are not sent for VIP vehicle notices.
- If payment is not received after the third reminder duration, the VIP vehicle notice will be suspended with TS-CLV (TS code and period of suspension will be configurable in the Suspension Reason UI screen in OCMS staff portal and stored in OCMS_Suspension_Reason table) at the CPC stage (for MVP1, the notice will be TS-CLV at the end of RR3/DR3).

- This processing flow ensures that TS-CLV notices will not escalate to court summons stage but remain payable through online and AXS channels.

- TS-CLV is a looping temporary suspension that automatically re-applies itself until it is manually lifted by an OIC and changes the vehicle registration type from ‘V’ to ‘S’ or if the notice is fully paid. If another TS suspension code is applied and later revived, OCMS will re-TS the notice under CLV again. 
**7.2 Processing

7.2.1 High-Level Processing Flow for VIP Vehicle Notice**

**
**

NOTE: Due to page size limit, the full-sized image is appended.
****

| No. | Process | Description |
| --- | --- | --- |
| 1 | Pre-condition: Raw Offence Data Received | Raw offence data is received from various sources such as REPCCS, CES-EHT, EEPS, PLUS Staff Portal, or OCMS Staff Portal. |
| 2 | Pre-condition: Validate Data | OCMS validates the offence data format, structure, and completeness of mandatory data fields. |
| 3 | Pre-condition: Check Duplicate Notice Number | The system checks for duplicate notice numbers to avoid issuing the same Notice twice. |
| 4 | Pre-condition: Identify Offence Type | OCMS proceeds to detect the Offence Rule Code to determine the Offence Type. If the Offence Type is Parking Offence (Type O) or Payment Evasion (Type E), OCMS will process the Notice along the Type O and E flow. If the Offence Type is Unauthorised Use of Parking Lot (Type U), OCMS will process the Notice along the Type U flow. |
| 5 | Workflow for Type O &amp; E | Type O and Type E Notices will be created and processed via this Workflow where Notices will be processed up to the RR3 or DR3 Processing Stages for MVP1. If the vehicle registration type is detected to be “V – VIP” during Notice creation, OCMS suspends the Notice with TS-OLD while the OIC conducts investigation to verify whether the Owner is a VIP. After the TS-OLD suspension expires, or after the OIC manually revives the suspension, the Notice continues along the Reminder workflow for RD and/or DN stages. At the end of the RR3/DR3 stages, outstanding unpaid notices will move to the court stage and be suspended with TS-CLV (looping TS) at CPC stage. Notices which have been paid will be suspended with PS-FP if payment is successful. Note : As court stage processing will only be implemented post-MVP1, the handling of VIP vehicle notices at the court stage has not been included in the current scope. |
| 6 | Workflow for Type U | Type U Notices will be created and processed along the UPL Notice processing workflow. Note: The requirements for UPL notice processing flow will be provided at a later stage. |

- **Processing Flow for Processing VIP Vehicle Notices (Type O &amp; E) **

NOTE: Due to page size limit, the full-sized image is appended.

**

**

| No. | Process | Description |
| --- | --- | --- |
| 1 | Start | The process begins for offences classified as Type O or Type E. |
| 2 | Duplicate Notice Details Check | The system compares new notice details against existing notices to avoid issuing the same Notice twice. Compare new notice details against existing notices based on: Vehicle number Offence Date and time Computer Rule code Parking lot number (null = null) If the lot number from both notices are blank, they will be matched.) Car park code |
| 3 | Detect Vehicle Registration Type | The system identifies the vehicle’s registration type. If it's a VIP Vehicle (Type = V), OCMS proceeds with the next step. |
| 4 | Notice Created at NPA | OCMS creates VIP vehicle notice and proceeds with next step. |
| 5 | Check for Double Booking | The system detects duplicate notices issued to the same vehicle for the same offence date, computer rule code, PP code, vehicle no. after the notice is created, and automatically suspend them under PS-DBB. Note : Refer to OCMS 21 functional document for details on Double booking function |
| 6 | AN Qualification Check | The system checks the Type O Notices to determine if the vehicle qualifies for Advisory Notice (AN) processing. If the Notice qualifies as an AN, OCMS processes the Notice along the VIP Vehicle ANS sub-flow. Refer to Section 7.2.4 If the Notice does not qualify as an AN, OCMS proceeds to Step 7 |
| 7 | Suspend Notice with TS-OLD | OCMS suspends the Notice with TS-OLD (21 days) for users to check that the notice is correctly issued. |
| 8 | TS-OLD revived | When the TS-OLD suspension is revived, the Notice proceeds to Step 9 for the LTA Vehicle Ownership check. The suspension may be revived in one of two ways: OIC manually revives the suspension after investigation The suspension auto revives upon expiry |
| 9 | LTA vehicle ownership check | The system sends a query to LTA to retrieve vehicle details and ownership information. |
| 10 | Check Vehicle type after LTA Vehicle Ownership Check at ROV | At ROV stage, the system then obtains and stores the Vehicle Ownership file returned by LTA. After receiving the LTA response, the system performs a check on the vehicle to determine if it is registered as a VIP vehicle and change the next processing stage to ‘RD1’ and the next processing date assigned to the current date. Note: Refer to Section 3.2.3 of OCMS 7 Functional Document for checking on VIP vehicle type The owner can also furnish driver once vehicle ownership records are returned after LTA check at ROV stage. |
| 11 | Prepare RD1 for MHA/DH checks | Prepare RD1 for MHA/DH checks scheduled job to validate and update offender particulars through MHA and DataHive before progressing to RD1. If the Notice is still outstanding at the end of the ROV stage, a scheduled job runs at the end of the day to process the Notice: Submits NRIC/FIN records to MHA and DataHive to retrieve latest offender particulars The retrieved data is processed and stored in the database. After MHA and DataHive checks are completed, the system runs prepare for RD1 scheduled job to proceed for stage change process. |
| 12 | Prepare for RD1 | A scheduled job runs at the end of the day to process the Notice: Add the Notice to the list of Notices for RD1 Reminder Letter generation Send the RD1 letter generation list to the printing vendor via SFTP Change the Notice’s Last Processing Stage from ROV to RD1 |
| 13 | Notice at RD1 Stage | The Notice enters the RD1 stage. At this stage: (a) The Owner can furnish the Driver/Hirer so that the Notice is redirected to the Driver/Hirer instead. Refer to Section 7.2.5. (b) The Owner can make payment for the offence, upon which the Notice will be suspended with the code PS-FP, stopping further processing. |
| 14 | Prepare RD2 for MHA/DH checks | Prepare RD2 for MHA/DH checks scheduled job to validate and update offender particulars through MHA and DataHive before progressing to RD2. If the Notice is still outstanding at the end of the RD1 stage, a scheduled job runs at the end of the day to process the Notice: Submits NRIC/FIN records to MHA and DataHive to retrieve latest offender particulars The retrieved data is processed and stored in the database. After MHA and DataHive checks are completed, the system runs prepare for RD2 scheduled job to proceed for stage change process. |
| 15 | Prepare for RD2 | A scheduled job runs at the end of the day to process the Notice: Add the Notice to the list of Notices for RD2 Reminder Letter generation Send the RD2 letter generation list to the printing vendor via SFTP Change the Notice’s Last Processing Stage from RD1 to RD2 |
| 16 | Notice at RD2 Stage | The Notice enters the RD2 stage. At this stage: (a) The Owner can furnish the Driver/Hirer so that the Notice is redirected to the Driver/Hirer instead. Refer to Section 7.2. 5. (b) The Owner can make payment for the offence, upon which the Notice will be suspended with the code PS-FP, stopping further processing. |
| 17 | Prepare RR3 for MHA/DH checks | Prepare RR3 for MHA/DH checks to validate and update offender particulars through MHA and DataHive before progressing to RR3. If the Notice is still outstanding at the end of the RD2 stage, a scheduled job to prepare RR3 for MHA and DataHive check runs at the end of the day to process the Notice: Submits NRIC/FIN records to MHA and DataHive to retrieve latest offender particulars The retrieved data is processed and stored in the database. After MHA and DataHive checks are completed, the system runs prepare for RR3 scheduled job to proceed for stage change process. |
| 18 | Prepare for RR3 | A scheduled job runs at the end of the day to process the Notice: (a) Add the Notice to the list of Notices for RR3 Reminder Letter generation (b) Send the RR3 letter generation list to the printing vendor via SFTP (c) Change the Notice’s Last Processing Stage from RD2 to RR3. |
| 19 | Notice at RR3 Stage | The Notice enters the RR3 stage. At this stage, the Offender can make payment for the Offence and the Notice will be suspended with PS-FP to stop the processing. |
| 20 | Notice is Outstanding at the End of RR3 stage | If the notice is outstanding at the end of RR3, the notice will go to CPC stage. At the CPC stage, the Offender can continue to make payment via the eService or AXS. Notice will be TS-CLV at CPC stage.* *Note: For MVP1, VIP notices will be TSed at end of RR3/DR3 stage as notices will not progress to court stage yet, |

**
**

- **Processing Flow for Processing VIP Vehicle Notices (Type U)(KIV)**
**
**

- **Sub-flow for VIP Vehicle Advisory Notice**

NOTE: Due to page size limit, the full-sized image is appended.

| No. | Process | Description |
| --- | --- | --- |
| 1 | Start | The process begins for offences classified as Type O or Type E. |
| 2 | Duplicate Notice Details Check | The system compares new notice details against existing notices to avoid issuing the same Notice twice. Compare new notice details against existing notices based on: Vehicle number Offence Date and time Computer Rule code Parking lot number (null = null) If the lot number from both notices are blank, they will be matched.) Car park code |
| 3 | Detect Vehicle Registration Type | The system identifies the vehicle’s registration type. If it's a VIP vehicle (Type =V), it proceeds to the next step. |
| 4 | Notice Created at NPA | OCMS proceeds to create the VIP Notice and proceeds to the next step. |
| 5 | Check for Double Booking | The system checks duplicate notices issued to the same vehicle for the same offence date, computer rule code, PP code, vehicle no. after the notice is created and automatically suspend them under PS-DBB. Note: Refer to OCMS 21 functional document for details on Double booking function |
| 6 | AN Qualification Check | The system checks Type O Notices to determine whether the vehicle qualifies for AN processing. If the Notice does not qualify as an AN, OCMS proceeds to process the VIP Vehicle Notice along the Type O and Type E workflow. If the Notice qualifies as an AN, OCMS updates the AN_Flag to ‘Y’ and change the next processing stage to ‘RD1’ and proceed to Step7. |
| 7 | AN Letter Generation | A scheduled job runs at the end of the day to prepare the AN Letters for sending to Offenders. |
| 8 | Query for Notices | The AN Letter generation process begins with OCMS querying the database to retrieve Advisory Notices where: vehicle_registration_type = V an_flag = Y last_processing_stage = NPA next_processing_stage = ROV next_processing_date = current_date or earlier suspension_type = null epr_reason_of_suspension = null |
| 9 | Generate AN Letter PDF | OCMS proceeds to generate the AN Letter in PDF format for each of the Notices returned from the query. |
| 10 | Drop Letters into SFTP | The generated PDF files are sent to the printing vendor via SFTP and the notice’s Last Processing Stage will be updated to RD1, the Next Processing Stage will be set to RD2, and the Next Processing Date will be set to same date as the last processing date |
| 11 | Suspend Notices with PS-ANS | OCMS suspend the Notices with PS-ANS after sending the Letters to the printing vendor. |
| 12 | End | The process concludes. |

**
7.2.5 Sub-flow for Furnish Driver/Hirer for VIP Vehicle Notices**

NOTE: Due to page size limit, the full-sized image is appended

| S/N | Process Name | Description of Process |
| --- | --- | --- |
| 1 | Start | The process begins when the Vehicle Owner accesses the eService portal to furnish driver/Hirer details for a VIP vehicle Notice. |
| 2 | Furnish Driver/Hirer &amp; Submit | The Vehicle Owner submits the Driver/Hirer's particulars for the VIP vehicle Notice. |
| 3 | Update Notice &amp; Driver/Hirer Info | OCMS adds the Driver/Hirer particulars to the Internet database and updates the Notice as “furnish pending approval”. Refer to OCMS 41 Functional Document . |
| 4 | Sync Driver/Hirer Info to the Intranet | A scheduled job syncs the furnished Driver/Hirer particulars from the Internet to the Intranet. |
| 5 | Validate Driver/Hirer Particulars | The OCMS Intranet backend validates the Driver/Hirer particulars to determine if the Particulars can be accepted: If the Driver/Hirer’s particulars do not meet the approval criteria, OCMS proceeds to Step 6. If the particulars meet the criteria, OCMS proceeds to Step 8 |
| 6 | OIC Reviews Driver/Hirer Particulars | An OIC will perform a manual review the Driver/Hirer particulars via the Staff Portal to determine if the Driver/Hirer can be accepted: If the Driver/Hirer’s particulars do not meet the approval criteria, OCMS proceeds to Step 7. If the particulars meet the criteria, OCMS proceeds to Step 8. |
| 7 | Update Internet Notice for rejected particulars | When the Driver/Hirer particulars for a Notice is rejected by the OIC, OCMS will update the Internet Notice to remove the “furnish pending approval” status, so that the Notice can be made available for another furnish Driver/Hirer attempt. Note: Once the furnish application data in the intranet database is processed, the updated data is synchronized to the eocms_furnish_application table in the internet database. |
| 8 | Auto Approve Driver/Hirer | During the validation of Driver/Hirer particulars, if the Driver/Hirer information meet the validation criteria, OCMS will auto approve the Driver/Hirer. |
| 9 | Driver/Hirer Particulars Added to Notice | If approved, OCMS will create a new record for the driver/Hirer particulars and update into the OCMS database. |
| 10 | Change Next Processing Stage to DN1/RD1 | Driver OCMS changes the Next Processing Stage to DN1 and set the next processing date to current date. Hirer OCMS changes the Next Processing Stage to RD1 and set the next processing date to current date. |
| 11 | Prepare for DN1/RD1 | A scheduled job runs at the end of the day to process the Notice: (a) Add the Notice to the list of Notices for DN1/RD1 Reminder Letter generation (b) Send the DN1/RD1 letter generation list to the printing vendor via SFTP (c) Change the Notice’s Last Processing Stage to DN1/RD1 |
| 12 | Notice at DN1/RD1 Stage | The Notice enters the DN1/RD1 stage. At this stage, the Offender can make payment for the Offence and the Notice will be suspended with PS-FP to stop the processing if payment is made. |
| 13 | Prepare DN2/RD2 for MHA/DH checks | If the Notice is still outstanding at the end of the RD1/DN1 stage, a scheduled job to prepare RD2/DN2 for MHA and DataHive check runs at the end of the day to validate and update offender particulars before progressing to RD2/DN2: Submits NRIC/FIN records to MHA and DataHive to retrieve latest offender particulars The retrieved data is processed and stored in the database. After MHA and DataHive checks are completed, the system runs prepare for RD2/DN2 scheduled job to proceed for stage change process. |
| 14 | Prepare for DN2/RD2 | After the MHA/DH checks, a scheduled job runs at the end of the day to process the Notice: (a) Add the Notice to the list of Notices for DN2/RD2 Reminder Letter generation (b) Send the DN2/RD2 letter generation list to the printing vendor via SFTP (c) Change the Notice’s Last Processing Stage from DN1/RD1 to DN2/RD2. |
| 15 | Notice at DN2/RD2 Stage | The Notice enters the DN2/RD2 stage. At this stage, the Offender can make payment for the Offence and the Notice will be suspended with PS-FP to stop the processing if payment is made. |
| 16 | Prepare RR3/DR3 for MHA/DH checks | If the Notice is still outstanding at the end of the RD2/DN2 stage, a scheduled job to prepare RR3/DR3 for MHA and DataHive check runs at the end of the day to validate and update offender particulars before progressing to DR3/RR3: Submits NRIC/FIN records to MHA and DataHive to retrieve latest offender particulars. The retrieved data is processed and stored in the database After MHA and DataHive checks are completed, the system runs prepare for RR3/DR3 scheduled job to proceed for stage change process. |
| 17 | Prepare for DR3/RR3 | After the MHA/DH checks, a scheduled job runs at the end of the day to process the Notice: (a) Add the Notice to the list of Notices for DR3/RR3 Reminder Letter generation (b) Send the DR3/RR3 letter generation list to the printing vendor via SFTP (c) Change the Notice’s Last Processing Stage from DN2/RD2 to DR3/RR3. |
| 18 | Notice at DR3/RR3 Stage | The Notice enters the DR3/RR3 stage. At this stage, the Offender can make payment for the Offence and the Notice will be suspended with PS-FP to stop the processing if payment is made. |
| 19 | Notice is Outstanding at the End of DR3/RR3 stage | If the notice is outstanding at the end of DR3/RR3, the notice will go to CPC stage and apply TS-CLV which is a looping TS. At the CPC stage, the Offender can continue to make payment via the eService or AXS. |

**
7.3 Attributes of a VIP Vehicle Notice **

**7.3.1 Create New VIP Vehicle Notice in the ocms_valid_offence_notice Intranet database table**

| Database Field | Description | Sample Value |
| --- | --- | --- |
| notice_no | Notice number | 500500303J |
| amount_payable | Total amount payable | 70 |
| an_flag | Indicates is the Notice is an Advisory Notice. Note: A foreign vehicle is not eligible for Advisory Notice | ‘N’ – No ‘Y’ – Yes |
| composition_amount | Composition amount for the Notice | 70 |
| computer_rule_code | Offence rule code | 10300 |
| due_date_of_revival | Revival date of the Temporary Suspension | Blank or null |
| epr_date_of_suspension | Date the EPR reason of suspension was applied | Blank or null |
| epr_reason_of_suspension | EPR reason of suspension code | Blank or null |
| last_processing_date | Date the Notice entered the last processing stage | 2025-03-22 |
| last_processing_stage | Last or current processing stage of the Notice | NPA |
| next_processing_date | Date the Notice will enter the next processing stage | 2025-03-22 |
| next_processing_stage | Next processing stage the Notice will enter | ROV |
| notice_date_and_time | Offence date and time | 2025-03-21 19:00:02 |
| offence_notice_type | Indicates the Offence type | ‘O’ – Parking Offence ‘E’ – Payment Evasion ‘U’ – UPL |
| parking_fee | Parking fee, if provided | 3.60 |
| parking_lot_no | Parking lot number | 12 |
| payment_acceptance_allowed | Indicates if the Notice is payable | Y |
| payment_due_date | Payment due date of the Notice at the last/current processing stage | Blank or null |
| pp_code | Car Park code | A0005 |
| prev_processing_date | Date the Notice entered the previous processing stage | Blank or null |
| prev_processing_stage | Previous processing stage before the current stage | Blank or null |
| rule_remarks | Rule remarks entered by the PW | Veh unattended |
| suspension_type | Type of Suspension | Blank or null |
| vehicle_category | Vehicle category | ‘C’ - Car ‘H’ - Heavy Vehicle ‘M’ - Motorcycle ‘Y’ - Large Motorcycle |
| vehicle_no | Vehicle number | SBB1383D |
| vehicle_registration_type | Vehicle registration type | ‘V’ - VIP |
| subsystem_label | Sub-system ID of the data source | 22523456 |
| warden_no | ID number of the parking warden | 1234A |
| is_sync | Indicates whether this record has been written to the Internet database | ‘Y’ – Yes ‘N’ - No |

**7.3.2 Create Owner in the ocms_offence_notice_owner_driver Intranet database table
**

Offender information will be retrieved from LTA and MHA/DataHive and populated into the database table

**
**

**7.3.3 Create Owner’s address in the ocms_offence_notice_owner_driver_address Intranet database table
 **Offender information will be retrieved from LTA and MHA/DataHive and populated into the database table

**7.3.4 Create Notice additional details in the ocms_offence_notice_detail Intranet database table
**Offence information sent by the data source (eg. CES, REPCCS, OCMS Staff Portal or PLUS Staff Portal) will be populated into the database table.

**7.4 Allowed Functions by Notice Condition for VIP Vehicle Offences**

| Notice Condition | eNotification Allowed | Payment Allowed | Furnish Driver Allowed |
| --- | --- | --- | --- |
| NPA | No | Yes | No |
| ROV | No | Yes | Yes |
| RD1/DN1 | No | Yes | RD1: Yes DN1: No |
| RD2/DN2 | No | Yes | RD1: Yes DN1: No |
| RR3/DR3 | No | Yes | No |

**7.5 Suspending Outstanding VIP Notices at CPC Stage (KIV until post OCMS MVP1)**

NOTE: Due to page size limit, the full-sized image is appended.

**
**

| No. | Process | Description |
| --- | --- | --- |
| 1 | CRON – Suspend Notice | The scheduled job runs at the end of the RR3/DR3 stage, before executing the "Prepare for CPC" scheduled job. Note: VIP vehicle notices will be TS-CLV at CPC stage post MVP1 when court process is implemented. |
| 2 | Query Valid Offence Notice Table | OCMS Intranet backend queries the validoffencenotice table to identify eligible records based on the following criteria: (a) vehicle_registration_type = ‘V’ (b) last_processing_stage = RR3/DR3 (c) next_processing_stage = CPC (d) next_processing_stage = Next day or earlier (e) suspension_type = null or TS |
| 3 | Records Found? | The system checks if any records meet the above criteria. (a) If no records are found, the process terminates. (b) If records are found, the process proceeds to suspend the notices. |
| 4 | Move Notices into CPC stage and suspend Notice with TS-CLV | Notice(s) that meet the query conditions will be: Moved into the Next Processing Stage which is CPC Suspended with 'TS-CLV' |
| 5 | Patch Notices details in the database | OCMS patches the latest processing stage and suspension details in the ocms_valid_offence_notice database: last_processing_stage = CPC last_processing_date = Current date/time next_processing_stage = CSD next_processing_date = null prev_processing_stage = RR3 or DR3 prev_processing_date = Date/time Notice entered RR3/DR3 After updating the Intranet database, OCMS proceeds to patch the corresponding Notice in the Internet database with the same data. |
| 6 | Create new suspension record | The system creates a new suspended notice record in the ocms_suspended_notice table to record the TS-CLV suspension. |
| 7 | End | Once the notice is suspended, the notice processing stops. |

**7.6 Exit Conditions for a VIP Vehicle Notice**

| No. | Exist Condition | Description |
| --- | --- | --- |
| 1 | Notice has been paid | The notice will no longer be processed after payment for the VIP Vehicle Notice has been made successfully. |

**7.7 Suspending VIP Vehicle Notices (KIV until post OCMS MVP1)**

- OCMS temporarily suspends VIP Vehicle Notices with reason of suspension ‘CLV’ if the Notice is still outstanding at the CPC stage. This is a looping TS as long as the vehicle_registration_type = 'V' . 
Note: For MVP1 implementation, TS-CLV will be applied at the end of RR3/DR3 stage. 

- 

**7.7.1 Suspension Code **

| Suspension Type | Reason of Suspension | Description |
| --- | --- | --- |
| TS | CLV | Classified (VIP) Vehicles |

**7.7.2 Suspension Rules **

| Condition | Rule | Description |
| --- | --- | --- |
| TS-CLV can be applied at these stages | CPC (For MVP1 - RR3, DR3) | TS-CLV will be applied automatically at CPC stage (for MVP1, TS-CLV will be applied at the end of RR3/DR3 stages) |
| Authorised OCMS users can apply TS-CLV via the OCMS Staff Portal | Yes | Authorised OCMS users can apply the suspension using the Staff Portal |
| PLUS users can apply TS-CLV | No | TS-CLV will not be displayed as a suspension option in the PLUS Staff Portal |
| Notice is payable after the suspension | Yes | Notice is payable via the eService and AXS |
| Notice is furnishable after the suspension | No | Notice is not furnishable |
| Allow additional TS | Yes | TS can be applied to the Notice when there is existing TS-CLV |
| Allow additional PS | Yes | PS can be applied to the Notice when there is existing TS-CLV |
| OCMS users can manually edit the offence details | KIV | The edit function is to be KIV until post OCMS MVP1. If users edit vehicle registration type from ‘V’ to ‘S’ at any stage, the notice will be able to proceed to court. If users edit back from ‘S’ to ‘V’, the notice will be TS-CLV if the notice is at CPC stage. Validation checks should be performed when user changes the vehicle registration type. |

**7.8 Updating Data for VIP Vehicles with TS-CLV suspension (KIV until post OCMS MVP1**)**
**

| Database Field | Description | Sample Value |
| --- | --- | --- |
| notice_no | Notice number | 500500303J |
| date_of_suspension | Date and time the suspension was applied | 2025-03-22 19:00:02 |
| sr_no | Serial number for the suspension, running number | 1 |
| suspension_source | Which system initiated the suspension | OCMS |
| suspension_type | Type of suspension | TS |
| reason_of_suspension | EPR suspension code that indicates the reason for the suspension | CLV |
| officer_authorising_suspension | Name of Officer who initiated the suspension | JOHN LEE |
| cre_date | Current date and time | 2025-03-22 19:00:02 |
| cre_user_id | User/System that created the record | ocmsiz_app_conn |

**
7.9 Classified Vehicle Notices Report **

- The daily “Classified Vehicle Notices Report” is generated to provide OICs with daily visibility and monitoring of all notices issued to VIP vehicles with active status in the VIP_Vehicle table**,** including:
a. Notices across all processing stages (NPA, RD1, RD2, RR3, CPC, etc.).
b. Notices under all suspension types (e.g., TS-OLD, TS-CLV, TS-APP, PS-FP) and Notices that are not under suspension 
c. Notices where the vehicle registration type is **‘V’ or ‘S’.**
d. Notices redirected to **drivers or hirers** after furnishing.

- The daily report provides both a quick summary of classified vehicles notices (issued, outstanding, settled) and full detailed information.

- The report will be sent to OICs via email **once per working day at a scheduled time** [Timing TBD with users]

- The email body will include a **concise summary** of notices (issued, outstanding, settled) for quick reference, in addition to the full report attached.

**
**

**7.9.1 Process Flow for Generating the Classified Vehicle Notices Report**

OTE: Due to page size limit, the full-sized image is appended.

| | | |
| --- | --- | --- |
| | | |
| | | |
| | | |
| | | |
| | | |
| | | |
| | | |
| | | |
| | | |

| S/N | Process Name | Description of Process |
| --- | --- | --- |
| 1 | Start | The Generate Classified Vehicle Notices Report process is initiated by a scheduled cron job. |
| 2 | Retrieve Active VIP Vehicles from CAS | OCMS queries the CAS database to retrieve the list of all VIP vehicles with an active status. |
| 3 | Any Active VIP Vehicles Found? | The system checks whether any active VIP vehicle records are returned from CAS. If no records are found, the system proceeds to generate a report indicating that no active VIP vehicles were found and the report generation process ends. If active VIP vehicles are found, the system stores the retrieved VIP vehicle details in a temporary data store for subsequent processing. |
| 4 | Extract Vehicle Numbers | OCMS then extracts the vehicle numbers from the list of active VIP vehicles retrieved from CAS for use in downstream queries. |
| 5 | Retrieve Notice Details from OCMS | OCMS uses the extracted vehicle numbers to query the OCMS valid offence notice database to retrieve all notices issued to the active VIP vehicles. |
| 6 | Store VIP Notice Details | OCMS stores the notice details retrieved from OCMS in a temporary data store. |
| 7 | Extract Notice Numbers | OCMS then extracts the notice numbers from the retrieved notice details for further processing. |
| 8 | Retrieve Current Offender Details | Using the extracted notice numbers, OCMS queries the OCMS OwnerDriver database to retrieve the current offender details for each notice. |
| 9 | Store Current Offender Details | The system stores the current offender details in a temporary data store. |
| 10 | Calculate Outstanding Notice Count | OCMS proceeds to process the list of Notices details retrieved from the valid offence notice database to determine the number of outstanding VIP Notices. It does this by counting the number of Notices that have the ‘null’ value in the crs_reason_of_suspension field. |
| 11 | Calculate Settled/Paid Notice Count | OCMS then calculates the number of settled or paid notices by subtracting the number of outstanding notices from the total number of notices retrieved. |
| 12 | Retrieve Data stored in Temp | OCMS retrieves all data stored in temporary data store to prepare for report preparation. |
| 13 | Format Report Data | The system formats the retrieved data into the required structure for report generation. |
| 14 | Generate Report | The system generates the classified vehicle notices report by populating the predefined report template with the formatted data. |
| 15 | Attach Report to Email | The generated report file is attached to an outgoing email. |
| 16 | Send Email | The system sends the email with the report attached to the configured recipients. |
| 17 | Update Batch Job Status | The system updates the batch job tracking information to reflect the completion status of the report generation process. |
| 18 | End | The report generation process ends. |


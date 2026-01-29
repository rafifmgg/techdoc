**Section 6:** **Notice Processing Flow for Diplomatic Vehicles**

**6.1 Use Case
**

- The Diplomatic Vehicle Notice processing flow is triggered when a Notice is created with the vehicle registration type “D”.
- Diplomatic Vehicle Notices for Parking Offences (Type O) and Payment Evasion Offences (Type E) will not follow the standard processing flows for these offence types. Instead, they will be handled through the Diplomatic Vehicle Notice processing flow.
- Only Unauthorised Parking Lot Offences (Type U) will continue to follow the UPL processing flow.
- The Diplomatic Vehicle processing flow will:Once a notice/advisory notice is created for a diplomatic vehicle, OCMS will perform a direct LTA interface to check for vehicle ownership details. It will then check MHA and DataHive for vehicle owner’s latest registered address and other information and proceed to the RD1 stage, where the first reminder letter/AN letter is generated and sent for the offence.
- Bypass the ENA stage and eNotifications (SMS/email) are not sent for DIP vehicle notices.
- If payment is not received after the second reminder duration, the diplomatic vehicle notice will be suspended with PS-DIP.

- This processing flow ensures that PS-DIP notices will remain payable through online and AXS channels.
**6.2 Processing

6.2.1 High-Level Processing flow for Diplomatic Vehicle Notice
**

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
| 5 | Workflow for Type O &amp; E | Type O and Type E Notices will be created and processed via this Workflow where Notices will be processed up to the RD2 or DN2 Processing Stages. Outstanding (unpaid) Notices will be suspended with PS-DIP at the end of the RD2/DN2 stages. Notices which have been paid will be suspended with PS-FP if payment is successful. |
| 6 | Workflow for Type U | Type U Notices will be created and processed along the UPL Notice processing workflow. Note: The requirements for UPL notice processing flow will be provided at a later stage. |

**6.2.2 Processing Flow for Processing Diplomatic Vehicle Notices (Type O &amp; E)**

NOTE: Due to page size limit, the full-sized image is appended

****

| No. | Process | Description |
| --- | --- | --- |
| 1 | Start | The process begins for offences classified as Type O or Type E. |
| 2 | Duplicate Notice Details Check | The system compares new notice details against existing notices to avoid issuing the same Notice twice. Compare new notice details against existing notices based on: Vehicle number Offence Date and time Computer Rule code Parking lot number (null = null) If the lot number from both notices are blank, they will be matched.) Car park code |
| 3 | Detect Vehicle Registration Type | The system identifies the vehicle’s registration type. If it's a Diplomatic Vehicle (Type = D), OCMS proceeds to perform the AN Qualification check. |
| 4 | Notice Created at NPA | The Diplomatic Vehicle Notice is created with the Last Processing Stage set to NPA, the Next Processing Stage set to ROV, and the Next Processing Date assigned as the current date. |
| 5 | Check for Double Booking | The system check on duplicate notices issued to the same vehicle for the same offence date and time after the notice is created, and automatically suspend them under PS-DBB. Note : Refer to OCMS 21 functional document for detail Double booking function |
| 6 | AN Qualification Check | The system checks the Type O Notices to determine if the vehicle qualifies for Advisory Notice (AN) processing. If the Notice qualifies as an AN, OCMS processes the Notice along the Diplomatic Vehicle ANS sub-flow. Refer to Section 6.2.4. If the Notice does not qualify as an AN, OCMS proceeds to Step 7. |
| 7 | LTA vehicle ownership check | When a Diplomatic Vehicle Notice is created, the system sends a query to LTA to retrieve vehicle details and ownership information. |
| 8 | Check DIP Vehicle type after LTA Vehicle Ownership Check at ROV | At ROV stage, the system then obtains and stores the Vehicle Ownership file returned by LTA. After receiving the LTA response, the system performs a check on the vehicle to determine if it is registered as a diplomatic vehicle. If the LTA Diplomatic flag is Y and vehicle registration type is ‘D’, change the Next Processing Stage to ‘RD1’ and Next Processing Date to current date. Note: Refer to Section 3.2 of OCMS 7 Functional Document for more checking DIP vehicle type The owner can also furnish driver once vehicle ownership records are returned after LTA check at ROV stage. |
| 9 | Prepare RD1 for MHA/DH checks | Prepare RD1 for MHA/DH checks scheduled job is to validate and update offender particulars through MHA and DataHive before progressing to RD1. If the Notice is still outstanding at the end of the ROV stage, a scheduled job runs at the end of the day to process the Notice: Submits NRIC/FIN records to MHA and DataHive to retrieve latest offender particulars The retrieved data is processed and stored in the database. After MHA and DataHive checks are completed,the system run prepare for RD1 scheduled job to proceed for stage change process. |
| 10 | Prepare for RD1 | A scheduled job runs at the end of the day to process the Notice: Add the Notice to the list of Notices for RD1 Reminder Letter generation Send the RD1 letter generation list to the printing vendor via SFTP Change the Notice’s Last Processing Stage from ROV to RD1 |
| 11 | Notice at RD1 Stage | The Notice enters the RD1 stage. At this stage: (a) The Owner can furnish the Driver/Hirer so that the Notice is redirected to the Driver/Hirer instead. Refer to Section 6.2.5. (b) The Owner can make payment for the offence, upon which the Notice will be suspended with the code PS-FP, stopping further processing. |
| 12 | Prepare RD2 for MHA/DH checks | Prepare RD2 for MHA/DH checks scheduled job is to validate and update offender particulars through MHA and DataHive before progressing to RD2. If the Notice is still outstanding at the end of the RD1 stage, a scheduled job runs at the end of the day to process the Notice: Submits NRIC/FIN records to MHA and DataHive to retrieve latest offender particulars The retrieved data is processed and stored in the database. After MHA and DataHive checks are completed,the system run prepare for RD2 scheduled job to proceed for stage change process. |
| 13 | Prepare for RD2 | If the Notice is still outstanding at the end of the RD1 stage, a scheduled job runs at the end of the day to process the Notice: Add the Notice to the list of Notices for RD2 Reminder Letter generation Send the RD2 letter generation list to the printing vendor via SFTP Change the Notice’s Last Processing Stage from RD1 to RD2 |
| 14 | Notice at RD2 Stage | The Notice enters the RD2 stage. At this stage: (a) The Owner can furnish the Driver so that the Notice is redirected to the Driver instead. Refer to Section 6.2.5. (b) The Owner can make payment for the offence, upon which the Notice will be suspended with the code PS-FP, stopping further processing. |
| 15 | Notice is Outstanding at the End of RD2 stage | If the notice is outstanding at the end of RD2, OCMS will automatically apply the PS-DIP suspension to stop the Notice process and prevent it from escalating to the Court stage. (Refer to Section 6.7) After the suspension, the Offender: Can continue to make payment via the eService or AXS Can no longer furnish a Driver/Hirer. |

**6.2.3 Processing Flow for Processing Diplomatic Vehicle Notices (Type U)(KIV)**

*UPL processing for Diplomatic vehicles to KIV for now. *

**6.2.4 Sub-flow for Diplomatic Vehicle Advisory Notice
**

NOTE: Due to page size limit, the full-sized image is appended.

| No. | Process | Description |
| --- | --- | --- |
| 1 | Start | The process begins for offences classified as Type O or Type E. |
| 2 | Duplicate Notice Details Check | The system compares new notice details against existing notices to avoid issuing the same Notice twice. Compare new notice details against existing notices based on: Vehicle number Offence Date and time Computer Rule code Parking lot number (null = null) If the lot number from both notices are blank, they will be matched.) Car park code |
| 3 | Detect Vehicle Registration Type | The system identifies the vehicle’s registration type. If it's a Diplomatic vehicle (Type = D), it proceeds to the next step. |
| 4 | Notice Created at NPA | OCMS proceeds to create the Diplomatic Vehicle Notice and proceed to check for double booking. |
| 5 | Check for Double Booking | The system checks duplicate notices issued to the same vehicle for the same offense date and time after the notice is created and automatically suspend them under PS-DBB. Note: Refer to OCMS 21 functional document for deatil Double booking function |
| 6 | AN Qualification Check | The system checks Type O Notices to determine whether the vehicle qualifies for AN processing. If the Notice does not qualify as an AN, OCMS proceeds to process the Military Vehicle Notice along the Type O and Type E workflow. If the Notice qualifies as an AN, OCMS update the AN_Flag to ‘Y’ and change the next processing stage to ‘RD1’ and then proceed to Step 7. |
| 7 | AN Letter Generation | A scheduled job runs at the end of the day to prepare the AN Letters for sending to Offenders. |
| 8 | Query for Notices | The AN Letter generation process begins with OCMS querying the database to retrieve Advisory Notices where anAN_flag = 'Y', the next processing date equals the current date, and the next processing stage is RD1. |
| 9 | Generate AN Letter PDF | OCMS proceeds to generate the AN Letter in PDF format for each of the Notices returned from the query. |
| 10 | Drop Letters into SFTP | The generated PDF files are sent to the printing vendor via SFTP and the notice’s Last Processing Stage will be updated to RD1, the Next Processing Stage will be set to RD2, and the Next Processing Date will be set to same date as the last processing date |
| 11 | Suspend Notices with PS-ANS | OCMS suspend the Notices with PS-ANS after sending the Letters to the printing vendor. |
| 12 | End | The process concludes. |

**6.2.5 Sub-flow for Furnish Driver/Hirer for Diplomatic Vehicle Notices

**

****

NOTE: Due to page size limit, the full-sized image is appended.
****

**6.3 Attributes of a Diplomatic Vehicle Notice **

6.3.1 Create New diplomatic vehicle Notice in the ocms_valid_offence_notice Intranet database table

| Database Field | Description | Sample Value |
| --- | --- | --- |
| notice_no | Notice number | 500500303J |
| amount_payable | Total amount payable | 70 |
| an_flag | Indicates is the Notice is an Advisory Notice. | ‘N’ – No ‘Y’ – Yes |
| composition_amount | Composition amount for the Notice | 70 |
| computer_rule_code | Offence rule code | 10300 |
| due_date_of_revival | Revival date of the Temporary Suspension | Blank or null |
| epr_date_of_suspension | Date the EPR reason of suspension was applied | 2025-03-22 19:00:02 |
| epr_reason_of_suspension | EPR reason of suspension code | DIP |
| last_processing_date | Date the Notice entered the last processing stage | 2025-03-22 |
| last_processing_stage | Last or current processing stage of the Notice | NPA |
| next_processing_date | Date the Notice will enter the next processing stage | 2025-03-22 |
| next_processing_stage | Next processing stage the Notice will enter | RD1 |
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
| suspension_type | Type of Suspension | TS PS Blank or null? |
| vehicle_category | Vehicle category | ‘C’ - Car ‘H’ - Heavy Vehicle ‘M’ - Motorcycle ‘Y’ - Large Motorcyle |
| vehicle_no | Vehicle number | S1234CD |
| vehicle_registration_type | Vehicle registration type | ‘D’ - Diplomatic |
| subsystem_label | Sub-system ID of the data source | 22523456 |
| warden_no | ID number of the parking warden | 1234A |
| is_sync | Indicates whether this record has been written to the Internet database | ‘Y’ – Yes ‘N’ - No |

 

**6.3.2 Create Owner in the ocms_offence_notice_owner_driver Intranet database table
Refer to Section 5.3.2 for create owner data details**

**
6.3.3 Create Owner’s address in the ocms_offence_notice_owner_driver_address Intranet database table
Refer to Section 5.3.3 for create owner’s addres details**

**6.3.4 Create Notice additional details in the ocms_offence_notice_detail Intranet database table
Refer to Section 5.3.4 for create notice additional details**

*
*** 

6.4 Allowed Functions by Notice Condition for Diplomatic Vehicle Offences**

| Notice Condition | eNotification Allowed | Payment Allowed | Furnish Driver Allowed |
| --- | --- | --- | --- |
| NPA | No | Yes | No |
| ROV | No | Yes | Yes |
| RD1 | No | Yes | Yes |
| RD2 | No | Yes | Yes |
| Suspended with PS-DIP | No | Yes | No |

**6.6 Exit Conditions for a Diplomatic Vehicle Notice**

| No. | Exist Condition | Description |
| --- | --- | --- |
| 1 | Notice has been paid | The notice will no longer be processed after payment for the Diplomatic Vehicle Notice has been made successfully. |
| 2 | Notice remains unpaid after X years | OCMS will send the Notice for archival if it remains unpaid X years after the Offence Date and is reviewed by OCMS users not to be further pursued. The notice will be purged from the database after archival. |
| 3 | Notice is permanently suspended ( with other PS code) | The Diplomatic Vehicle Notice will exit the process flow if it is permanently suspended with other PS code. e.g. cancelled as APP, CFA etc. |

**
6.7 Suspending Outstanding Diplomatic Vehicle Notices at the end of RD2/DN2**

1	OCMS permanently suspends Diplomatic Vehicle Notices if the Notices are still outstanding at the end of the RD2 or DN2 processing stages. 

2.	The Notices are suspended to prevent them from escalating to the Court stage.**
**

NOTE: Due to page size limit, the full-sized image is appended.
****

**
**

| No. | Process | Description |
| --- | --- | --- |
| 1 | CRON – Suspend Notice | The scheduled job runs at the end of the RD2 and DN2 stage, before executing the "Prepare for RR3 and DR3" scheduled job. |
| 2 | Query Valid Offence Notice Table | OCMS Intranet backend queries the valid_offence_notice table to identify eligible records based on the following criteria: (a) vehicle_registration_type = 'D' (b) crs_reason is blank (c) last_processing_stage = 'RD2' or ‘DN2’ (d) next_processing_stage = 'RR3' or ‘DR3’ |
| 3 | Records Found? | The system checks if any records meet the above criteria. (a)If no records are found, the process terminates. (b)If records are found, the process proceeds to suspend the notices. |
| 4 | Suspend Notice with PS-DIP | | Diplomatic notices that meet the criteria are suspended with reason of suspension code 'PS-DIP'. |
| |
| 5 | Add PS-DIP Suspended Notice Record | The system creates a new suspended notice record in the ocms_suspended_notice table for the suspension details. |
| 6 | Update Suspended Diplomat Notices in VON table | The system updates the ocms_valid_offence_notice table for suspended Diplomat Notices in both the intranet and internet databases with the following values: - suspensiontype = 'PS' - epr_reason_suspension = 'DIP' - epr_date_of _suspension = date and time of suspension |
| 7 | End | Once the notice is suspended, the notice processing stops. |

**6.7.1 Suspension Code **

| Suspension Type | Reason of Suspension | Description |
| --- | --- | --- |
| PS | DIP | Diplomatic Vehicle |

**6.7.2 Suspension Rules **

| Condition | Rule | Description |
| --- | --- | --- |
| PS-DIP can be applied at these stages | NPA, ROV, RD1, RD2, DN1, DN2 | PS-DIP will be applied automatically at the end of RD2/DN2 stages |
| Authorised OCMS users can apply PS-DIP via the OCMS Staff Portal | Yes | Authorised OCMS users can apply the suspension using the Staff Portal |
| PLUS users can apply PS-DIP | No | PS-DIP will not be displayed as a suspension option in the PLUS Staff Portal |
| Notice is payable after the suspension | Yes | Notice is payable via the eService and AXS |
| Notice is furnishable after the suspension | No | Notice is not furnishable once it has passed the expiry of the RD2/DN2 stage and PS-DIP has been applied. When a PS-DIP is applied over a TS (e.g. TS-APP), that Notice will not appear in the furnishable list within the eService for furnishing of driver details . |
| Allow additional TS | Yes | TS can be applied to the Notice when there is existing PS-DIP and notice is still outstanding. |
| Allow additional PS | Yes | PS can be applied to the Notice when there is existing PS-DIP and notice is still outstanding |

**6.8 Updating Data for Diplomatic Vehicles with PS-DIP suspension 
**Refer to OCMS 18 Section 2.4 for storing Permanent Suspension Data

| 6.8.1 Update Notice Suspension in ocms_valid_offence_notice Intranet database table officer_authorising_suspension | Name of Officer who initiated the suspension | usevocmsiz_app_conn |
| --- | --- | --- |


**Section 5 – Notice Processing Flow for Military Vehicles**

**5.1 Use Case**

- The Military Vehicle Notice processing flow begins when a Notice is created with the vehicle registration type “I”.

- Military Vehicle Notices for Parking Offence (Type O) Notices and Payment Evasion (Type E) Offence Notices will be processed through the Military Vehicle Notice processing flow, instead of the standard Type O and Type E flows.
- Military Vehicle Notices for Type O and Type E offences will:Bypass the LTA vehicle ownership check
- Bypass the MHA local ID check 
- Bypass the DataHive FIN and company profile check
- Bypass the ENA stage and no eNotification will be sent
- Enter the RD1 Processing Stage at the end of the day and initiate the sending of the first Reminder Letter/ANS letter.
- Notices for Unauthorised Parking Lot (UPL) offences (Type U) will follow the UPL Notice processing flow.
*Note: The requirements for UPL notice processing flow will be provided at a later stage. 
*

- When a Military Vehicle Notice reaches the end of the RD2 or DN2 processing stage for Type O and Type E offences and remains as Outstanding, a PS-MID suspension will be applied. 

- The Military Vehicle processing flow differs from the standard processing flows for the following reasons: The vehicle ownership records for military vehicles are not in LTA’s VRLS and there is a fixed address to send to Mindef for parking offence notices issued to military vehicles, therefore there is no need to request for owner details or perform MHA and DataHive checks to retrieve address. 
- The Notices need to be suspended at the end of RD2/DN2 stages to prevent them from escalating to the Court stage. 
**5.2 Processing Flow**

**5.2.1 High-Level Processing Flow for Military Vehicle Notices**

****

NOTE: Due to page size limit, the full-sized image is appended.
****

| No. | Process | Description |
| --- | --- | --- |
| 1 | Pre-condition: Raw Offence Data Received | Raw offence data is received from various sources such as REPCCS, CES-EHT, EEPS, PLUS Staff Portal, or OCMS Staff Portal. |
| 2 | Pre-condition: Validate Data | OCMS validates the offence data format, structure, and completeness of mandatory data fields. |
| 3 | Pre-condition: Check Duplicate Notice Number | The system checks for duplicate notice numbers to avoid issuing the same Notice twice. |
| 4 | Pre-condition: Identify Offence Type | OCMS proceeds to detect the Offence Rule Code to determine the Offence Type. If the Offence Type is Parking Offence (Type O) or Payment Evasion (Type E), OCMS will process the Notice along the Type O and E flow. If the Offence Type is Unauthorised Use of Parking Lot (Type U), OCMS will process the Notice along the Type U flow. |
| 5 | Workflow for Type O &amp; E | Type O and Type E Notices will be created and processed via this Workflow where Notices will be processed up to the RD2 or DN2 Processing Stages. Outstanding (unpaid) Notices will be suspended with PS-MID at the end of the RD2/DN2 stages. Notices which have been paid will be suspended with PS-FP if payment is successful. |
| 6 | Workflow for Type U | Type U Notices will be created and processed along the UPL Notice processing workflow. Note: The requirements for UPL notice processing flow will be provided at a later stage. |

**5.2.2 Process Flow for Processing Military Vehicle Notices (Type O &amp; E)
**

NOTE: Due to page size limit, the full-sized image is appended.

| No. | Process | Description |
| --- | --- | --- |
| 1 | Start | The process begins for offences classified as Type O or Type E. |
| 2 | Duplicate Notice Details Check | The system compares new notice details against existing notices to avoid issuing the same Notice twice. Compare new notice details against existing notices based on: Vehicle number Offence Date and time Computer Rule code Parking lot number (null = null) If the lot number from both notices are blank, they will be matched.) Car park code |
| 3 | Detect Vehicle Registration Type | The system identifies the vehicle’s registration type.If it's a Military Vehicle (Type = I), OCMS proceeds with next step. |
| 4 | Notice Created at NPA | The system retrieves the MINDEF address that is hardcoded in the code, creates the Military Vehicle Notice with the Notice’s Last Processing Stage set to NPA, the next processing stage set to RD1, and the next processing date assigned as the current date. |
| 5 | Check for Double Booking | The system checks on duplicate notices issued to the same vehicle for the same offence date, computer rule code, PP code, vehicle no. after the notice is created and automatically suspend them under PS-DBB. Note : Refer to OCMS 21 functional document for details on Double booking function |
| 6 | AN Qualification Check | The system checks the Type O Notices to determine if the vehicle qualifies for Advisory Notice (AN) processing. If the Notice qualifies as an AN, OCMS processes the Notice along the Military Vehicle ANS sub-flow. Refer to Section 5.2.4 If the Notice does not qualify as an AN, OCMS proceeds to Step 7 |
| 7 | Prepare for RD1 | A scheduled job runs at the end of the day to process the Notice: Add the Notice to the list of Notices for RD1 Reminder Letter generation Send the RD1 letter generation list to the printing vendor via SFTP Change the Notice’s Last Processing Stage from NPA to RD1 |
| 8 | Notice at RD1 Stage | The Notice enters the RD1 stage. At this stage: The Owner can furnish the Driver/Hirer so that the Notice is redirected to the Driver/Hierer instead. Refer to Section 5.2.5 . The Owner can make payment for the Offence and the Notice will be suspended with PS-FP to stop the processing. |
| 9 | Prepare for RD2 | If the Notice is still outstanding at the end of the RD1 stage, a scheduled job runs at the end of the day to process the Notice: Add the Notice to the list of Notices for RD2 Reminder Letter generation Send the RD2 letter generation list to the printing vendor via SFTP Change the Notice’s Last Processing Stage from RD1 to RD2 |
| 10 | Notice at RD2 Stage | The Notice enters the RD2 stage. At this stage: The Owner can furnish the Driver/Hirer so that the Notice is redirected to the Driver/Hirer instead. Refer to Section 5.2.5 . The Owner can make payment for the Offence and the Notice will be suspended with PS-FP to stop the processing. |
| 11 | Notice is Outstanding at the End of RD2 stage | If the notice is outstanding at the end of RD2, OCMS will automatically apply the PS-MID suspension to stop the Notice process and prevent it from escalating to the Court stage. Refer to Section 55 After the suspension, the Offender: Can continue to make payment via the eService or AXS Can no longer furnish a Driver/Hirer. |

**5.2.3 Process Flow for Processing Military Vehicle Notices (Type U) (KIV)**

**
**

NOTE: Due to page size limit, the full-sized image is appended.

**
**

| No. | Process | Description |
| --- | --- | --- |
| 1 | Start | The process begins for offences classified as Type U. |
| 2 | Duplicate Notice Details Check | Compare new notice details against existing notices based on: Vehicle number Offence Date and time Computer Rule code Parking lot number (null = null) If the lot number from both notices are blank, they will be matched.) Car park code If duplicate detected, apply PS-DBB to suspend notice permanently and add suspension record into Suspended Notice database table. If no duplicate detected, proceed to vehicle type identification. Note: More double booking (DBB) scenarios will be discussed under OCMS 21 later. |
| 3 | Detect Vehicle Registration Type | The system identifies the vehicle’s registration type. If it's a Military Vehicle (Type = I), OCMS proceeds to step 3 |
| 4 | Notice Created at NPA | The system retrieve the MINDEF address that is hardcoded in the code and he Military Vehicle Notice is created, and the Notice’s Last Processing Stage is NPA. |
| 5 | Stay at NPA Pending Decision | The Notice will remain at the NPA stage until the OIC determines whether it is compoundable (payable). If the Notice is not compoundable, OCMS will suspend the Notice with PS-MID immediately to prevent the Notice from escalating to the Court stage. If the Notice is compoundable, OCMS proceeds to Step 5. |
| 6 | Notice’s Next Processing Stage changed to DN1 | When the OIC indicates in the Staff Portal that the Notice is compoundable, OCMS will change the Notice’s Next Processing Stage from NPA to DN1. |
| 7 | Prepare for DN1 | A scheduled job runs at the end of the day to process the Notice: Add the Notice to the list of Notices for DN1 Reminder Letter generation Send the DN1 letter generation list to the printing vendor via SFTP Change the Notice’s Last Processing Stage from NPA to DN1 |
| 8 | Notice at DN1 Stage | The Notice enters the DN1 stage. At this stage, the Offender can make payment for the Offence and the Notice will be suspended with PS-FP to stop the processing. |
| 9 | Prepare for DN2 | If the Notice is still outstanding at the end of the DN1 stage, a scheduled job runs at the end of the day to process the Notice: Add the Notice to the list of Notices for DN Reminder Letter generation Send the DN2 letter generation list to the printing vendor via SFTP Change the Notice’s Last Processing Stage from DN1 to DN2 |
| 10 | Notice at DN2 Stage | The Notice enters the DN2 stage. At this stage, the Offender can make payment for the Offence and the Notice will be suspended with PS-FP to stop the processing. |
| 11 | Notice is Outstanding at the End of DN2 stage | If the notice is outstanding at the end of DN2, OCMS will automatically apply the PS-MID suspension to stop the Notice process and prevent it from escalating to the Court stage. After the suspension, the Offender can continue to make payment via the eService or AXS. |

** **

**5.2.4 Sub-flow for Military Vehicle Advisory Notice
**

NOTE: Due to page size limit, the full-sized image is appended.

| No. | Process | Description |
| --- | --- | --- |
| 1 | Start | The process begins for offences classified as Type O or Type E. |
| 2 | Duplicate Notice Details Check | The system compares new notice details against existing notices to avoid issuing the same Notice twice. Compare new notice details against existing notices based on: Vehicle number Offence Date and time Computer Rule code Parking lot number (null = null) If the lot number from both notices are blank, they will be matched.) Car park code |
| 3 | Detect Vehicle Registration Type | The system identifies the vehicle’s registration type. If it's a military vehicle (Type = I), it proceeds to the next step. |
| 4 | Notice Created at NPA | The system retrieves the MINDEF address that is hardcoded in the code, creates the Military Vehicle Notice with the Notice’s Last Processing Stage set to NPA, the next processing stage set to RD1, and the next processing date assigned as the current date. |
| 5 | Check for Double Booking | The system detects duplicate notices issued to the same vehicle for the same offence date, computer rule code, PP code, vehicle no. after the notice is created, and automatically suspend them under PS-DBB. Note : Refer to OCMS 21 functional document for deatails on Double booking function |
| 6 | AN Qualification Check | The system checks Type O Notices to determine whether the vehicle qualifies for AN processing. If the Notice does not qualify as an AN, OCMS proceeds to process the Military Vehicle Notice along the Type O and Type E workflow. If the Notice qualifies as an AN, OCMS updates the AN_Flag to ‘Y’ and change the next processing stage to ‘RD1’ and proceeds to next Step. |
| 7 | AN Letter Generation | A scheduled job runs at the end of the day to prepare the AN Letters for sending to Offenders. |
| 8 | Query for Notices | The AN Letter generation process begins with OCMS querying the database to retrieve Advisory Notices that meet these conditions: vehicle_registration_type = I an_flag = Y last_processing_stage = NPA next_processing_stage = RD1 next_processing_date = current_date or earlier suspension_type = null epr_reason_of_suspension = null |
| 9 | Generate AN Letter PDF | OCMS proceeds to generate the AN Letter in PDF format for each of the Notices returned from the query. |
| 10 | Drop Letters into SFTP | The generated PDF files are sent to the printing vendor via SFTP and the notice’s Last Processing Stage will be updated to RD1 , the Next Processing Stage will be set to RD2 , and the Next Processing Date will be set to same date as the last processing date. |
| 11 | Suspend Notices with PS-ANS | OCMS suspend the Notices with PS-ANS after sending the Letters to the printing vendor. |
| 12 | End | The process concludes. |

**

5.2.5 Sub-flow for Furnish Driver/Hirer (Type O &amp; E) **

**
**

NOTE: Due to page size limit, the full-sized image is appended.
****

| S/N | Process Name | Description of Process |
| --- | --- | --- |
| 1 | Start | The process begins when the Vehicle Owner accesses the eService portal to furnish driver details for a Military Notice. |
| 2 | Furnish Driver/Hirer &amp; Submit | The Vehicle Owner submits the Driver/Hirer's particulars for the Military Notice. |
| 3 | Update Notice &amp; Driver/Hirer Info | OCMS adds the Driver/Hirer particulars to the Internet database and updates the Notice as “furnish pending approval”. Refer to OCMS 41 Functional Document . |
| 4 | Sync Driver/Hirer Info to the Intranet | A scheduled job syncs the furnished Driver/Hirer particulars from the Internet to the Intranet. |
| 5 | Validate Driver/Hirer Particulars | The OCMS Intranet backend validates the Driver/Hirer particulars to determine if the Particulars can be accepted: If the Driver/Hirer’s particulars do not meet the approval criteria, OCMS proceeds to Step 6. If the particulars meet the criteria, OCMS proceeds to Step 8 |
| 6 | OIC Reviews Driver/Hirer Particulars | An OIC will perform a manual review of the Driver/Hirer particulars via the Staff Portal to determine if the Driver/Hirer can be accepted: If the Driver/Hirer’s particulars do not meet the approval criteria, OCMS proceeds to Step 7. If the particulars meet the criteria, OCMS proceeds to Step 8. |
| 7 | Update Internet Notice for rejected particulars | When the Driver/Hirer particulars for a Notice is rejected by the OIC, OCMS will update the Intranet Notice to remove the “furnish pending approval” status, so that the Notice can be made available for another furnish Driver/Hirer attempt. Note: Once the furnish application data in the intranet database is processed, the updated data is synchronized to the eocms_furnish_application table in the internet database. |
| 8 | Auto Approve Driver/Hirer | During the validation of Driver/Hirer particulars, if the Driver/Hirer information meet the validation criteria, OCMS will auto approve the Driver/Hirer. |
| 9 | Driver/Hirer ParticularsAdded to Notice | If approved, OCMS will create a new record for the Driver/Hirer particulars and update into the OCMS database. |
| 10 | Change Next Processing Stage to DN1/RD1 | Driver OCMS changes the Next Processing Stage to DN1 and set the next processing date to current date. Hirer OCMS changes the Next Processing Stage to RD1 and set the next processing date to current date. |
| 11 | Prepare for DN1/RD1 | A scheduled job runs at the end of the day to process the Notice: Add the Notice to the list of Notices for DN1/RD1 Reminder Letter generation Send the DN1/RD1 letter generation list to the printing vendor via SFTP Change the Notice’s Last Processing Stage to DN1/RD1 |
| 12 | Notice at RD1/DN1 Stage | The Notice enters the RD1/DN1 stage. At this stage, the Offender can make payment for the Offence and the Notice will be suspended with PS-FP to stop the processing if payment is made. |
| 13 | Prepare RD2/DN2 for MHA/DH checks | If the Notice is still outstanding at the end of the RD1/DN1 stage, a scheduled job to prepare RD2/DN2 for MHA and DataHive check runs at the end of the day to process the Notice. The objective is to validate and update offender particulars through MHA and DataHive before progressing to RD2/DN2: Submits NRIC/FIN records to MHA and DataHive to retrieve latest offender particulars The retrieved data is processed and stored in the database. After MHA and DataHive checks are completed, the system runs prepare for RD2/DN2 scheduled job to proceed for stage change process. |
| 14 | Prepare for RD2/DN2 | After the MHA/DH checks, a scheduled job runs at the end of the day to process the Notice: Add the Notice to the list of Notices for RD2/DN2 Reminder Letter generation Send the RD2/DN2 letter generation list to the printing vendor via SFTP Change the Notice’s Last Processing Stage from RD1/DN1 to RD2/DN2 |
| 15 | Notice at RD2/DN2 Stage | The Notice enters the RD2/DN2 stage. At this stage, the Offender can make payment for the Offence and the Notice will be suspended with PS-FP to stop the processing if payment is made. |
| 16 | Notice is Outstanding at the End of RD2/DN2 stage | If the notice is outstanding at the end of RD2/DN2, OCMS will automatically apply the PS-MID suspension to stop the Notice process and prevent it from escalating to the Court stage. Refer to Section 5.5 After the suspension, the Offender can continue to make payment via the eService or AXS. |

**5.2.6 Exit Conditions for a Military Vehicle Notice**

| No. | Exist Condition | Description |
| --- | --- | --- |
| 1 | Notice has been paid | The notice will no longer be processed after payment for the Military Vehicle Notice has been made successfully. |
| 2 | Notice remains unpaid after X years | OCMS will send the Notice for archival if it remains unpaid X years after the Offence Date and is reviewed not to be further pursued. The notice will be purged from the database after archival. |
| 3 | Notice is permanently suspended (with other PS code) | The Military Vehicle Notice will exit the process flow if it is permanently suspended with other PS code. e.g. cancelled as APP, CFA etc. |

**5.3 Attributes of a Military Vehicle Notice **

**5.3.1 Create New Military Vehicle Notice in the ocms_valid_offence_notice Intranet database table**

| Database Field | Description | Sample Value |
| --- | --- | --- |
| notice_no | Notice number | 500500303J |
| amount_payable | Total amount payable | 70 |
| an_flag | Indicates the Notice is an Advisory Notice. | ‘N’ – No ‘Y’ – Yes |
| composition_amount | Composition amount for the Notice | 70 |
| computer_rule_code | Offence rule code | 10300 |
| due_date_of_revival | Revival date of the Temporary Suspension | Blank or null |
| epr_date_of_suspension | Date the EPR reason of suspension was applied | Blank or null |
| epr_reason_of_suspension | EPR reason of suspension code | Blank or null |
| last_processing_date | Date the Notice entered the last processing stage | 2025-03-22 |
| last_processing_stage | Last or current processing stage of the Notice | NPA |
| next_processing_date | Date the Notice will enter the next processing stage | 2025-03-22 |
| next_processing_stage | Next processing stage the Notice will enter | RD1 |
| notice_date_and_time | Offence date and time | 2025-03-21 19:00:02 |
| offence_notice_type | Indicates the Offence type | ‘O’ – Parking Offence ‘E’ – Payment Evasion ‘U’ – UPL |
| parking_fee | Parking fee, if provided | 3.60 |
| parking_lot_no | Parking lot number | 12 |
| payment_acceptance_allowed | Indicates if the Notice is payable | Y |
| | | |
| pp_code | Car Park code | A0005 |
| prev_processing_date | Date the Notice entered the previous processing stage | Blank or null |
| prev_processing_stage | Previous processing stage before the current stage | Blank or null |
| rule_remarks | Rule remarks entered by the PW | Veh unattended |
| suspension_type | Type of Suspension | Blank or null |
| vehicle_category | Vehicle category | ‘C’ - Car ‘H’ - Heavy Vehicle ‘M’ - Motorcycle ‘Y’ - Large Motorcycle |
| vehicle_no | Vehicle number | MID2221 |
| vehicle_registration_type | Vehicle registration type | ‘I’ - Military |
| subsystem_label | Sub-system ID of the data source | 22523456 |
| warden_no | ID number of the parking warden | 1234A |
| is_sync | Indicates whether this record has been written to the Internet database | ‘Y’ – Yes ‘N’ - No |

**5.3.2 Create Owner in the ****ocms_offence_notice_owner_driver Intranet database table**

| Database Field | Description | Sample Value |
| --- | --- | --- |
| notice_no | Notice number | 500500303J |
| owner_driver_indicator | Indicator whether the record relates to an owner or driver. | ‘O’ |
| name | Name of the company associated with the offence notice. | MINDEF |
| email_addr | The primary email address of the driver/hirer/owner. | Blank or null |
| email_alt_addr | An alternate email address of the driver/hirer/owner. | Blank or null |
| id_type | Specific owner identification type. | B |
| id_no | Id number of the owner/driver | T08GA0011B |
| offender_indicator | Indicator to identify if the person is an offender. | Y |
| offender_mobile_code | Mobile country code for the offender. | Blank or null |
| offender_mobile_no | Mobile number of the offender. | Blank or null |
| offender_tel_code | Telephone country code for the offender. | Blank or null |
| offender_tel_no | Telephone number of the offender. | Blank or null |
| offender_tel_type | Type of telephone (home, work, etc.). | Blank or null |

**
5.3.3 Create Owner’s address in the ocms_offence_notice_owner_driver_address Intranet database table**

| Database Field | Description | Sample Value |
| --- | --- | --- |
| notice_no | Notice number | 500500303J |
| owner_driver_indicator | Indicator whether the record relates to an owner or driver. | ‘O’ |
| type_of_address | Type of the source address | MINDEF_ADDRESS |
| bldg_name | The building name of the address | Kranji Camp 3 |
| blk_hse_no | Block or house number of the address. | 151 |
| floor_no | Floor number of the source address | Blank or null |
| postal_code | Postal code of the address | 688248 |
| street_name | Street Name of the address | Choa Chu Kang Way |
| Unit no | Unit number of the address | Blank or null |

**5.3.4 Create Notice additional details in the ocms_offence_notice_detail Intranet database table

**

| Database Field | Description | Sample Value |
| --- | --- | --- |
| notice_no | Notice number | 500500303J |
| batch_date | Date the offence batch was processed. | NULL |
| batch_no | Batch number grouping multiple notices. | NULL |
| chasis_no | Chassis number of the vehicle involved in the offence. | SGM1234567890 |
| color_of_vehicle | Color of the vehicle at the time of the offence. | White |
| comments | Additional comments related to the offence. | NULL |
| cre_date | Creation date of the notice inside OCMS | 2025-03-21 T 19:00:10 |
| cre_user_id | User ID or Name of the person who created the record. | ocmsiz_app_conn |
| created_by | Alias for the creator of the record. | ocms_admin |
| created_dt | Date and time the record was created inside EHT REPCCS | 2025-03-21 T 19:00:02 |
| denom_invalid_coupon_1 | Denomination of the first invalid coupon. | $1.20 |
| denom_invalid_coupon_2 | Denomination of the second invalid coupon. | NULL |
| denom_of_valid_coupon | Denomination of the valid coupon used. | NULL |
| eht_id | ID of the electronic handheld terminal that issued the notice. | NULL |
| end_date | End date of the parking period | NULL |
| end_time | End time of the parking period. | NULL |
| expired_coupon_1_denom | Denomination of the first expired coupon. | NULL |
| expired_coupon_1_no | Number of the first expired coupon. | NULL |
| expired_coupon_1_subtype | Subtype of the first expired coupon. | NULL |
| expired_coupon_2_denom | Denomination of the second expired coupon. | NULL |
| expired_coupon_2_no | Number of the second expired coupon. | NULL |
| expired_coupon_2_subtype | Subtype of the second expired coupon. | NULL |
| expiry_time | Time at which the last valid coupon expired. | NULL |
| first_coupon_time | Time the first coupon was used on the given date. | NULL |
| image_id | Identifier for images associated with the offence. | NULL |
| invalid_coupon_no_1 | Number of the first invalid coupon reported. | A19021774 |
| invalid_coupon_1_creased_tab | Condition detail of the first invalid coupon's creased tab. | NULL |
| invalid_coupon_1_subtype | Subtype of the first invalid coupon. | NULL |
| condition_invalid_coupon_1 | Condition of the first invalid coupon. | Tore out wrong year 24F10F25T11AT20F |
| invalid_coupon_no_2 | Number of the second invalid coupon reported. | NULL |
| invalid_coupon_2_creased_tab | Condition detail of the second invalid coupon's creased tab. | NULL |
| invalid_coupon_2_subtype | Subtype of the second invalid coupon. | NULL |
| condition_invalid_coupon_2 | Condition of the second invalid coupon. | NULL |
| condition_invalid_coupon_3 | Condition of the third invalid coupon. | NULL |
| invalid_coupon_3_creased_tab | Condition detail of the third invalid coupon's creased tab. | NULL |
| denom_invalid_coupon_3 | Denomination of the third invalid coupon. | NULL |
| invalid_coupon_no_3 | Number of the third invalid coupon. | NULL |
| invalid_coupon_3_subtype | Subtype of the third invalid coupon. | NULL |
| | | |
| iu_no | In-vehicle unit number | NULL |
| last_modified_by | User ID of the last person who modified the record. | NULL |
| last_modified_dt | Date and time the record was last modified. | NULL |
| last_valid_coupon_denom | Denomination of the last valid coupon. | NULL |
| last_valid_coupon_expired_date | Expiry date of the last valid coupon. | NULL |
| last_valid_coupon_expired_time | Expiry time of the last valid coupon. | NULL |
| last_valid_coupon_no | Number of the last valid coupon. | NULL |
| last_valid_coupon_subtype | Subtype of the last valid coupon used. | NULL |
| lots_available_ind | Indicator if parking lots were available at the time of the offence. | NULL |
| model_of_vehicle | Model of the vehicle involved in the offence. | NULL |
| rep_obu_latitude | Longitude coordinates of the on-board unit at the time of the offence. | NULL |
| rep_obu_longitude | Longitude coordinates of the on-board unit at the time of the offence. | NULL |
| rep_operator_id | ID of the operator handling the offence processing. | NULL |
| rep_parking_end_dt | Date and time when the vehicle ends parking. | NULL |
| rep_parking_entry_dt | Date and time when the vehicle enters the car park. | NULL |
| rep_parking_exit_dt | Date and time when the vehicle exits the car park. | NULL |
| rep_parking_start_dt | Date and time when the vehicle started parking. | NULL |
| photo_no | Photo number associated with the offence. | NULL |
| rule_remark_1 | First remark related to the rule violation. | NULL |
| rule_remark_2 | Second remark related to the rule violation. | NULL |
| sys_non_printed_comment | System non-printed comments. | NULL |
| total_coupons_displayed | Total number of coupons displayed during the offence. | 0 |
| vehicle_make | Make of the vehicle involved in the offence. | Toyota |
| video_id | Video ID associated with the offence. | Null |
| rep_violation_code | Code identifying the specific violation. | 0 |

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
| | | |
| | | |

**5.4 Allowed Functions by Notice Condition for Military Vehicle Offences**

| Notice Condition | eNotification Allowed | Payment Allowed | Furnish Driver Allowed |
| --- | --- | --- | --- |
| NPA | No | Yes | No |
| ROV | No | Yes | No |
| RD1 | No | Yes | Yes |
| RD2 | No | Yes | Yes |
| Suspended with PS-MID | No | Yes | No |

**5.5 Suspending Outstanding Military Notices at the end of RD2/DN2**

- sare

NOTE: Due to page size limit, the full-sized image is appended.

| No. | Process | Description |
| --- | --- | --- |
| 1 | CRON – Suspend Notice | The scheduled job runs at the end of the RD2 and DN2 stage, before executing the "Prepare for RR3 and DR3" scheduled job. |
| 2 | Query Valid Offence Notice Table | OCMS Intranet backend queries the valid_offence_notice table to identify eligible records based on the following criteria: (a) vehicle_registration_type = 'I' (b) crs_reason_of_suspension is blank (c) last_processing_stage = 'RD2' or ‘DN2’ (d) next_processing_stage = 'RR3' or ‘DR3’ |
| 3 | Records Found? | The system checks if any records meet the above criteria. If no records are found, the process terminates. If records are found, the process proceeds to suspend the notices. |
| 4 | Suspend Notice with PS-MID | | Military notices that meet the criteria are suspended with reason of suspension code 'PS-MID'. |
| |
| 5 | Add PS-MID Suspended Notice Record | The system creates a new suspended notice record in the ocms_suspended_notice table for the suspension details. |
| 6 | Update Suspended Military Notices in VON table | The system updates the ocms_valid_offence_notice table for suspended Military Notices in both the intranet and internet databases with the following values: - suspension_type = 'PS' - epr_reason_suspension = 'MID' - epr_date_of_suspension = date and time of suspension |
| 7 | End | Once the notice is suspended, the notice processing stops. |

- 

**5.5.1 Suspension Code **

| Suspension Type | EPR Reason of Suspension | Description |
| --- | --- | --- |
| PS | MID | MINDEF Vehicle |

The 

**5.5.2 Suspension Rules **

| Condition | Rule | Description |
| --- | --- | --- |
| PS-MID can be applied at these stages | NPA, RD1, RD2, DN1, DN2 | PS-MID will be applied automatically at the end of RD2/DN2 stages |
| Authorised OCMS users can apply PS-MID via the OCMS Staff Portal | Yes | Authorised OCMS users can apply the suspension using the Staff Portal |
| PLUS users can apply PS-MID | No | PS-MID will not be displayed as a suspension option in the PLUS Staff Portal |
| Notice is payable after the suspension | Yes | Notice is payable via the eService and AXS |
| Notice is furnishable after PS-MID suspension | No | Notice is not furnishable |
| Allow additional TS | Yes | TS can be applied to the Notice when there is existing PS-MID |
| Allow additional PS | No | Existing PS-MID must be revived first before a new PS can be applied to the Notice. |

**5.6 Updating Data for Military Vehicles with PS-MID suspension 
**

**5.6.1 Update Notice Suspension in ocms_valid_offence_notice Intranet database table**

| Database Field | Description | Sample Value |
| --- | --- | --- |
| suspension_type | Type of suspension | PS |
| epr_reason_of_suspension | EPR suspension code that indicates the reason for the suspension | MID (MINDEF Vehicle) |
| epr_date_of_suspension | Date and time the suspension was applied | 2025-03-22 19:00:02 |

**5.6.2 Update Notice Suspension in eocms_valid_offence_notice Internet database table**

*Same fields as ocms_valid_offence_notice
*

**5.6.3 Update Suspension Details in ocms_suspended_notice database table**

| Database Field | Description | Sample Value |
| --- | --- | --- |
| notice_no | Notice number | 500500303J |
| date_of_suspension | Date and time the suspension was applied | 2025-03-22 19:00:02 |
| sr_no | Serial number for the suspension, running number | 1 |
| suspension_source | Which system initiated the suspension | OCMS |
| suspension_type | Type of suspension | PS |
| reason_of_suspension | EPR suspension code that indicates the reason for the suspension | MID |
| officer_authorising_suspension | Name of Officer who initiated the suspension | JOHN LEE |
| cre_date | Current date and time | 2025-03-22 19:00:02 |
| cre_user_id | User/System that created the record | ocmsiz_app_conn |


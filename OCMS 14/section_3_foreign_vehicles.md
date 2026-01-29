**Section 3 - Notice Processing flow for Foreign Vehicles**

**3.1 Use Case**

- The Foreign Vehicle Notice processing flow is triggered when a Notice is created with the vehicle registration type “F”.
- Foreign Vehicle Notices for Parking Offences (Type O), and Payment Evasion Offences (Type E) will not follow the standard processing flows for these offence types. Instead, they will be handled through the Foreign Vehicle Notice processing flow. Only Unauthorised Parking Lot Offences (Type U) will continue to follow the standard UPL processing flow.

- The Foreign Vehicle processing flow will:Suspend the foreign vehicle Notice with PS-FOR after Notice Creation
- Send the foreign vehicle number to vHUB, REPCCS, and Certis for enforcement if payment is not received and notice remains outstanding within X days from the offence date
- Add an administrative fee if the Notice remains unpaid for X days
- This processing flow ensures that owners of foreign vehicles remain accountable for their offences and are encouraged to settle Notices promptly.**Process Flow **

NOTE: Due to page size limit, the full-sized image is appended.

| No. | Process | Description |
| --- | --- | --- |
| | (Pre-condition) Receive Raw Offence Data | Raw offence data is sent from various sources (REPCCS, EHT, EEPS, PLUS Staff Portal, OCMS Staff Portal). The sources will label the vehicle registration type as ‘F’ to indicate that the Offence is committed by a foreign vehicle. |
| | (Pre-condition) Process Offence Data | OCMS processes the incoming offence data and detects the vehicle registration type as ‘F’. |
| | (Pre-condition) Create Foreign Vehicle Notice | OCMS automatically creates the Notice for the foreign vehicle. |
| 1 | Suspend Notice with PS-FOR | The notice is immediately suspended with the suspension code PS-FOR after Notice creation. This prevents OCMS from sending the Notice for local vehicle ownership check, owner particulars checks and sending of e-notifications and physical reminder letters. The Notice remains payable after the suspension but will not be furnishable. |
| 2 | Send Vehicle Number for Enforcement | If the notice remains outstanding for X days after the offence date, the vehicle number is sent to vHub, REPCCS, and Certis for enforcement. Note: The value X will be drawn from the Parameter ID ‘FOR’. |
| 3 | Add Administration Fee | If the notice remains outstanding for Y days after the offence date, an administration fee is added to the notice. The updated composition amount has to be sent to vHub. Note: The value Y will be drawn from the Parameter ID ‘FOD’. The Admin Fee amount will be drawn from the Parameter ID ‘AFO’ |
| 4 | Archive &amp; Purge Notice from the OCMS Database | If the notice still remains outstanding for Z years after the offence date,OICs will review the Notices and approve the ones that will no longer be pursued will should be be archived and purged from the database. This illustrates the lifecycle of a foreign vehicle Notice if it remains outstanding. Refer to Section 3.2.1 for the list of Exit Conditions for a foreign Vehicle Notice. |

**3.2.1 Exit Conditions for a Foreign Vehicle Notice**

| No. | Exit Condition | Description |
| --- | --- | --- |
| 1 | Notice has been paid | The notice will no longer be processed after payment for the foreign vehicle Notice has been made successfully. |
| 2 | Notice remains unpaid after Z years | OCMS will send the Notice for archival if it remains unpaid Z years after the Offence Date and is reviewed by OCMS users not to be further pursued. The notice will be purged from the database after archival. |
| 3 | Notice is permanently suspended ( with other PS code) | The Foreign Vehicle notice will exit the process flow if it is permanently suspended with other PS code. e.g. cancelled as APP, CFA, etc. |
| | | |

**3.3 Attributes of a Foreign Vehicle Notice **

**3.3.1 Create New foreign vehicle Notice in the ocms_valid_offence_notice Intranet database table**

| Database Field | Description | Sample Value |
| --- | --- | --- |
| notice_no | Notice number | 500500303J |
| amount_payable | Total amount payable | 70 |
| an_flag | Indicates is the Notice is an Advisory Notice. Note: A foreign vehicle is not eligible for Advisory Notice | ‘N’ – No |
| composition_amount | Composition amount for the Notice | 70 |
| computer_rule_code | Offence rule code | 10300 |
| due_date_of_revival | Revival date of the Temporary Suspension | Blank or null |
| epr_date_of_suspension | Date the EPR reason of suspension was applied | 2025-03-22 19:00:02 |
| epr_reason_of_suspension | EPR reason of suspension code | FOR |
| last_processing_date | Date the Notice entered the last processing stage | 2025-03-22 |
| last_processing_stage | Last or current processing stage of the Notice | NPA |
| next_processing_date | Date the Notice will enter the next processing stage | 2025-03-22 |
| next_processing_stage | Next processing stage the Notice will enter | ROV |
| notice_date_and_time | Offence date and time | 2025-03-21 19:00:02 |
| offence_notice_type | Indicates the Offence type | ‘O’ – Parking Offence ‘E’ – Payment Evasion ‘U’ – UPL |
| parking_fee | Parking fee, if provided | 3.60 |
| parking_lot_no | Parking lot number | 12 |
| payment_acceptance_allowed | Indicates if the Notice is payable | Y |
| payment_due_date | Payment due date of the Notice at the last/current processing stage | Blank null |
| pp_code | Car Park code | A0005 |
| prev_processing_date | Date the Notice entered the previous processing stage | Blank or null |
| prev_processing_stage | Previous processing stage before the current stage | Blank or null |
| rule_remark_1 | Rule remarks entered by the PW | Double Yellow Line |
| rule_remark_2 | Rule remarks entered by the PW | Near Substation |
| suspension_type | Type of Suspension | TS PS Blank or null? |
| vehicle_category | Vehicle category | ‘C’ - Car ‘HV’ - Heavy Vehicle ‘M’ - Motorcycle ‘Y’ - Large Motorcyle |
| vehicle_no | Vehicle number | SNC7392R |
| vehicle_registration_type | Vehicle registration type | ‘F’ - Foreign |
| subsystem_label | Sub-system ID of the data source | 22523456 |
| warden_no | ID number of the parking warden | 1234A |
| is_sync | Indicates whether this record has been written to the Internet database | ‘Y’ – Yes ‘N’ - No |

 

**3.3.2 Create New foreign vehicle Notice in the eocms_valid_offence_notice Internet database table**

*Refer to Section 3.2.1 as the same fields will be stored in the Internet database.*

**3.3.3 Suspension. Furnish hirer/driver, Payment Rules for PS-FOR**

| Condition | Rule | Description |
| --- | --- | --- |
| OCMS Backend can auto apply PS-FOR at this stage | NPA | OCMS Backend will auto apply PS-FOR during foreign vehicle Notice creation at NPA stage |
| Authorised OCMS users can manually apply PS-FOR at any of these stages | NPA,eNA, ROV, RD1, RD2, RR3, DN1, DN2, DR3, CPC, CFC | Authorised OCMS users can apply the suspension using the Staff Portal |
| PLUS users can apply PS-FOR | Not allowed | PS-FOR will not be displayed as a suspension option in the PLUS Staff Portal |
| Notice is payable via the eService or AXS | Yes | Notice is payable via the eService and AXS |
| Notice is furnishable via the eService | Not allowed | Notice is not furnishable |
| OCMS users can manually update the Notice’s Hirer or Driver | Yes | OCMS users can add/update the Hirer or Driver to the foreign vehicle notice without having to revive the PS-FOR The add/update Hirer or Driver action will not trigger any process or send out any e-notification or reminder to the hirer or driver furnished. *Note: This is in line with only allowing hirer/driver details to be edited in MVP1. The other offence details are not allowed to be edited. |
| OCMS users can manually edit the offence details | KIV | The edit function is to be KIV until post OCMS MVP1. |
| OCMS users can furnish a Hirer or Driver and auto trigger a redirect and change processing stage change | No | There will not be any redirection of the notice or change of stage for foreign vehicle notices. |
| PLUS users can manually add a Hirer or Driver to a Notice | Not allowed | PLUS users are not allowed to add a new Hirer or Driver via PLUS’s Update Driver functionality |
| Apply additional TS | Yes | TS can be applied to the outstanding Notice when there is existing PS-FOR,with the exception of TS-UNC. TS can be applied without reviving PS-FOR. Notice will be automatically PS-FOR once the TS suspension period is revived and the notice is still outstanding. |
| Apply additional PS | Yes | . Existing PS-FOR must be revived before the new PS suspension is applied. To allow CRS suspensions (PS-FP and PS-PRA) to be applied WITHOUT PS REVIVAL. i.e. the ERP reason of Suspension will be updated as PS-FOR and CRS Reason of Suspension will be updated as PS-FP. For non-CRS suspension (e.g. PS-APP) applied over PS-FOR, OCMS will auto revive the PS-FOR . when users apply another PS over it, e.g. APP, CFA, etc. |
| PLUS user can apply a Reduction to a foreign vehicle Notice | Yes | PLUS users can manually trigger a reduction from the PLUS Staff Portal. PLUS is only allowed to reduce to the original composition amount. For specific computer rule code offences e.g. 30305, 30302/31302, 21300, the composition amount of Car or HV only can be reduced to an amount below the original composition amount. *Note: There will not be any more Overparking rules when REP commences. Overparking rules may still prevail in CES (before REP commences). Reduction rules to refer to PLUS’s payment matrix. |

**3.3 Sending Foreign Vehicle Information to vHub for Enforcement**

All vHub functions listed in this section are drawn from the OCMS 35.2 User Story, attached below. 

The functions are listed in OCMS 14 – Foreign Vehicle section to illustrate the end-to-end flow for the processing of a foreign vehicle Notice. *Note: This section will also be copied over to OCMS35.2 when functional document for OCMS35 is written later on. 

**3.3.1 Notifying vHub via API**

**3.4.1.1 Use Case **

- OCMS will use vHub’s Update Violation Request API to send the list of foreign vehicles with compoundable offences for enforcement. 

- Based on vHub’s External Agency API Interface Requirement Specifications v1.0 (effective date 19 Mar 2025), OCMS will use the API to notify vHub of the following: New outstanding foreign vehicle Notices or Notices that have been updated, e.g. Offence date/time was corrected offence rule, vehicle number or composition amount edited. 
- Foreign vehicle Notices which have been settled/paid
- Foreign vehicle Notices which have been cancelled, e.g. Notice waived as appeal was successful, or Notice has been archived
- Foreign vehicle notices that have been temporarily suspended so that enforcement will be temporarily withheld till further update from OCMS on the notice status. 
- OCMS will send the status of the notices according to vHub’s 3 statuses:

| Status | Description |
| --- | --- |
| ‘O’ - Outstanding | Outstanding notices, i.e. PS-FOR Notices that were earlier sent as ‘C’ – cancelled or ‘S’- settled but is later revived and is still unpaid will be resent to vHub again as ‘O’ |
| ‘S’ – Settled | Paid notices (‘FP’ Fully Paid) |
| ‘C’ - Cancelled | All other TS or PS codes, e.g. APP, CCE, MS, OTH, MS, CFA, etc. TS codes are sent as ‘C’ – cancelled to vHub to temporarily withhold enforcement actions. |

- From the API specs, the details for interfacing are: 1 API call can contain multiple Violation Cases 
- For OCMS’s implementation, OCMS will send each Offence as one Violation Case.
- Refer to VHub Integration Specifications “WoG Violation Hub - External Agency API IRS v1.0”

- Based on Section 3.4.1.1(c), a schedule cron job will initiate the sending of the foreign vehicle notices to vHub at pre-defined timings which will be confirmed with users. 

- During the sending process, OCMS will send a batch of new, updated, paid, suspended and waived notices in each API call. 

- vHub will respond to OCMS in real-time and OCMS will process the response status thereafter. 
- If notices are not successfully processed, they will be consolidated and resent with the next batch of notices to be updated. 
**3.4.1.2 Process flow **

NOTE: Due to page size limit, the full-sized image is appended.

| No. | Process | Description |
| --- | --- | --- |
| 1 | vHub Update Violation API Interface cron starts | A scheduled cron job triggers the vHub Update Violation API interface. |
| 2 | Get FOR &amp; Calculate Date | A function runs to get the value of parameter ID FOR from the database and calculates the relevant date. |
| 3 | Prepare Settled Notices List | OCMS system prepares the list of Settled foreign vehicle Notices to be sent to vHub. Notices that qualify as Settled: FOR Notices which have been paid in the last 24 hours |
| 4 | Prepare Cancelled Notices List | OCMS system prepares a list of Cancelled foreign vehicle Notices for vHub. Notices that qualify as Cancelled: Notices that have an active PS Notices that have an active TS Notices that are scheduled for archival |
| 5 | Prepare Outstanding Notices List | The system prepares a list of Outstanding foreign vehicle Notices for vHub. Notices that qualify as Outstanding include: FOR Notices which remain unpaid X days after Offence Date Unpaid older FOR Notices in which VON fields were updated (eg. PP code, suspension lifted) |
| 6 | Consolidate Lists | All three lists (outstanding, settled, cancelled) are consolidated into one data batch. |
| 7 | Recheck Record Count | OCMS rechecks the record count to ensure that the total in the consolidated list matches the sum of records from all three lists. |
| | | |
| 8 | Call vHub Update Violation Request API &amp; Process Response | OCMS fetches a batch of 50 notices from the consolidated list to formulate the API request. OCMS then calls the vHub Update Violation Request API and receives the API response. |
| 9 | Process All Records &amp; Responses | Confirms that all records have been sent and all responses have been processed. |
| 10 | Check for Interfacing Errors | System checks if any interfacing errors were logged during the process. |
| 11 | Consolidate All Interfacing Errors | If errors exist, they are consolidated into a single list or report. |
| 12 | Send Interfacing Error Email | An email notification is sent out summarizing any interfacing errors detected. |
| 13 | End | Process ends. |

**
Sub-flows for vHub Violation Request API and Response **

NOTE: Due to page size limit, the full-sized image is appended.

**3.4.1.3 API response from vHub**

*All status, error codes and error descriptions are referenced from vHUb’s External Agency API Interface Requirement Specifications v1.0 (effective date 19 Mar 2025)*

| vHub Data Field | OCMS Data Field | OCMS Database Table | Sample Value |
| --- | --- | --- | --- |
| Violations |
| StatusCode | record_status | ocms_offence_avss | ‘0’ – Success ‘1’ - Error |
| ErrorCode | error_code | ocms_offence_avss | E_REC_002 |
| ErrorMessage | error description | ocms_offence_avss | Exceed Field Limit |
| ViolationReportNo | New field - Unique ID for the API call (Batch ID) | ocms_offence_avss | DDMMYYYY001 |

**3.4.1.4 Storing Data sent to vHub and vHub API Response**

After the Outstanding, Settled and Cancelled lists have been consolidated, OCMS stores the data in the ocms_offence_avss database table to prepare for sending to vHub.

*Note: The * *table follows the existing CAS database table of the same name. MGG added 2 more fields “error_code” and “error_description” which are drawn from the CAS OFFENCE_AVSS_Log database table.*

| offence_avss Database Field | Description | Sample Value |
| --- | --- | --- |
| batch_date | Date the batch was generated | 20240918125317 |
| offence_no | Notice Number | 500500303J |
| OffenceDateTime | Offence date in YYYYMMDDHHMMSS format to match HTX specs | 20250617140652 |
| | | |
| offence_code | Offence Rule Code | 3A |
| location | Car park name | Aliwal Street |
| offence_description | Offence rule code description | Parking other than in a parking lot |
| vehicle_no | Vehicle number | JQE888 |
| vehicle_type | Vehicle category | C |
| vehicle_make | Vehicle make | Honda |
| vehicle_color | Vehicle colour | Red |
| amount_payable | Amount payable | 70 |
| record_status | Indicates whether the Notice should be sent as a new case, an updated case or as a case that vHub should cancel | ;N’ – New ‘U’ – Update ‘R’ - Remove |
| vhub_status | Indicates the Notice’s condition that matches vHub status | ‘O’ - Outstanding ‘S’ - Settled ‘C’ - Cancelled |
| | | |
| | | |
| | | |
| | | |
| cre_date | Date the record was created | 20240918125317 |
| cre_user_id | OCMS system name | ocmsizmgr |
| receipt_series | Part of receipt no. reflecting mode of payment | Receipt Series Remarks MD Manual Receipt NT AXS RT Singpost UO URA Online TE Sri Rosyani (URA officer) SG Saba Mohammed (URA officer) TF Pang (URA officer) TD Mehron (URA officer) |
| receipt_no | Receipt number if Notice has been paid | DICNOC1234567890 |
| receipt_check_digit | Receipt check digit | A |
| crs_date_of_suspension | Date CRS Reason of Suspension was applied | 20240918125317 |
| sent_to_vhub | Flag to indicate if the record was sent to vHub | 1 – Record sent 0 – Record not sent |
| ack_from_vhub | Flag to indicate if vHub acknowledgement was received | 1 – vHub Ack file received 0 – Ack file not received |
| sent_vhub_datetime | Date and time Notice/record was sent to vHub | 20240918130004 |
| ack_from_vhub_datetime | Date and time vHub acknowledgement was received | Blank when sending data to vHub |
| vhub_api_status_code | Status code returned by vHub API | ‘0’ - Success ‘1’ - Error |
| vhub_api_error_code | Error code returned by vHub API | E_REC_001 Blank if record has no error |
| vhub_api_error_description | Error description returned by vHub API | Exceed Field Limit Blank if record has no error |

**3.4.1.5 Mapping of Data sent to vHub via API 
***All fields are referenced from vHUb’s External Agency API Interface Requirement Specifications v1.0 (effective date 19 Mar 2025)*

| vHub Data Field | OCMS Data Field | OCMS Database Table | Sample Value |
| --- | --- | --- | --- |
| Violations |
| ViolationReportNo | offence_no | ocms_offence_avss | 500500303J |
| VehicleRegistrationNumber | vehicle_no | ocms_offence_avss | JQE888 |
| VehicleType | vehicle_type | ocms_offence_avss | C |
| VehicleTypeDescription | blank | blank | blank |
| OffenceDateTime | offence_date offence_time | ocms_offence_avss | 20240918125317 |
| OffenceLocation | location | ocms_offence_avss | Aliwal Street |
| OffenceLongitude | blank | blank | blank |
| OffenceLatitude | blank | blank | blank |
| Offence Details |
| OffenceReferenceNo | offence_no | ocms_offence_avss | 500500303J |
| OffenceDescription | offence_description | ocms_offence_avss | Parking other than in a parking lot |
| Status | Status will be generated on the fly | Status will be generated on the fly | ‘S’ – Settled ‘O’ – Outstanding ‘C’ - Cancelled |
| OffenceDemeritPoints | blank | blank | blank |
| OffenceFineAmount | amount_payable | ocms_offence_avss | 70.00 |
| Payment Details |
| PaymentDatetime | crs_date_of_suspension | ocms_offence_avss | 20240926214209 |
| ReceiptNo | receipt_no | ocms_offence_avss | DICNOC1234567890 |
| FineAmountReceived | blank | blank | blank |
| PaymentMode | blank | blank | blank |
| BankAccountNo | blank | blank | blank |
| CreditCardNo | blank | blank | blank |
| CardholderName | blank | blank | blank |

**3.4.1.6 Storing vHub API response **

*Refer to Section 3.4.1.3 Storing Data sent to vHub and vHub API Response*

**3.4.2 Sending Violation Records to vHub daily via SFTP **

**3.4.2.1 Use Case **

- vHub’s Violation Case Create/Update SFTP interface performs the same function as vHub’s Update Violation Request API. 

- The use case of SFTP interface is for OCMS to generate the list of foreign vehicle offences to be sent to vHub as an end-day file, and HTX can use it to match the records received in the day.

- Based on vHub’s External Agency Interface Requirement Specifications v1.3 (effective date 26 Feb 2020), OCMS will uses the interface to provide vHub the list of records that were sent via API in the previous day: New outstanding foreign vehicle Notices or Notices that have been updated, e.g. Offence date/time was corrected, offence rule, vehicle number or composition amount edited.
- Foreign vehicle Notices which have been settled/paid
- Foreign vehicle Notices which have been cancelled, e.g. Notice waived as appeal was successful, or Notice has been archived.
- Foreign vehicle notices that have been temporarily suspended so that enforcement will be temporarily withheld till further update from OCMS on the notice status will be sent as cancelled. 
- 

- Based on Section 3.4.2.1(c), a schedule cron job will initiate the sending of the foreign vehicle notices to vHub based on the schedule below: 

| Activity | Time | Pre-condition |
| --- | --- | --- |
| Get records from ocms_offence_avss database table that were sent via API the previous day | 0600hr | |
| URA drops file into SFTP | 0605hr | |
| vHub picks up file | 0615hr | |
| vHub drops acknowledgement files into SFTP | 0710hr | |

- During the sending process, OCMS will send a list of new, updated, paid, suspended and waived notices in each interface file. 

- vHub will return an acknowledgement file to OCMS at pre-agreed timings and OCMS will process the response status thereafter. 
**3.4.2.2 Process flow for generating and sending the Violation Case end-day file **

NOTE: Due to page size limit, the full-sized image is appended.

****

| No. | Process | Description |
| --- | --- | --- |
| 1 | Generation of End-Day file starts | Scheduled CRON job initiates end-day file generation for foreign vehicle (FOR) offences. |
| 2 | Get FOR &amp; Calculate Date | A function runs to get the value of parameter ID FOR from the database and calculates the relevant date. |
| 3 | Prepare Settled Notices List | OCMS system prepares the list of Settled foreign vehicle Notices to be sent to vHub. Notices that qualify as Settled: FOR Notices which have been paid in the last 24 hours |
| 4 | Prepare Cancelled Notices List | OCMS system prepares a list of Cancelled foreign vehicle Notices for vHub. Notices that qualify as Cancelled: Notices that have an active PS Notices that have an active TS Notices that are scheduled for archival |
| 5 | Prepare Outstanding Notices List | The system prepares a list of Outstanding foreign vehicle Notices for vHub. Notices that qualify as Outstanding include: FOR Notices which remain unpaid X days after Offence Date Unpaid older FOR Notices in which VON fields were updated (eg. PP code, increased fine amount, suspension lifted) |
| 6 | Consolidate Lists | All three lists (outstanding, settled, cancelled) are consolidated into one data batch. |
| 7 | Recheck Record Count | OCMS rechecks the record count to ensure that the total in the consolidated list matches the sum of records from all three lists. |
| 8 | Generate End-Day XML File | A batch process generates the XML file representing the consolidated FOR violation records. If file generation fails, OCMS send an interface error email If file generation is successful, OCMS proceeds to Step 9 |
| 9 | Send to SLIFT for Encryption | The generated file is then sent to SLIFT for encryption before. |
| 10 | Encryption Success? | OCMS checks if encryption was successful. If encryption fails, OCMS send an interface error email If encryption is successful, OCMS proceeds to Step 11 |
| 11 | Upload to Azure Blob | If encryption is successful, the encrypted file is uploaded to Azure Blob storage. |
| 12 | Upload Success? | OCMS checks if the upload was successful. If upload fails, OCMS send an interface error email If upload is successful, OCMS proceeds to Step 14 |
| 13 | Upload to SFTP | The encrypted file is uploaded to the SFTP server for vHub to consume. |
| 14 | Upload Success? | A final check confirms whether SFTP upload was successful; otherwise, an error email is sent. If upload fails, OCMS send an interface error email. If upload is successful, OCMS proceeds to Step 15 |
| 15 | Store Records | If file generation is successful, the records are inserted into ocms_offence_avss_SFTP for tracking. |
| 16 | End | Process ends. |

**Sub-flows for vHub Violation Case Create/Update SFTP End-Day File Processing**

NOTE: Due to page size limit, the full-sized image is appended.

****

*Note about sub-flow: The processes to query for eligible Notices to send as Settled, Cancelled and Outstanding are similar to the ones uses for the API interface with vHub, except for: *

- *The date range used for querying Notices *
- *For the Outstanding List, OCMS does not need to query ocms_offence_avss to check records has been sent earlier in the day*
**3.4.2.3 Process flow for processing vHub acknowledgment file** 

NOTE: Due to page size limit, the full-sized image is appended.

****

| No. | Process | Description |
| --- | --- | --- |
| 1 | Processing of vHub Acknowledgement file starts | A scheduled CRON job triggers the process to retrieve and handle acknowledgement files sent from vHub. |
| 2 | Download File from vHub SFTP | OCMS downloads the acknowledgement file from the SFTP server. If download is unsuccessful, OCMS sends an interface error email. If download is successful, OCMS proceeds to Step 3. |
| 3 | Upload File to Azure Blob | If download is successful, the file is uploaded to Azure Blob storage for storage. If upload is unsuccessful, OCMS sends an interface error email. If upload is successful, OCMS proceeds to Step 4. |
| 4 | Send to SLIFT to Decrypt | The uploaded file is sent to the SLIFT service for decryption. If decryption is unsuccessful, OCMS sends an interface error email. If decryption is successful, OCMS proceeds to Step 5. |
| 5 | Process vHub Acknowledgement File | OCMS processes the acknowledgement file and updates vHub’s status to the corresponding records in the ocms_offence_avss_sftp database. |
| 6 | End | Process ends. |

**Sub-flow for processing vHub acknowledgment file **

**
**NOTE: Due to page size limit, the full-sized image is appended.

****

**3.4.2.3 Storing Data sent to vHub via SFTP and vHub Acknowledgement **

After the end-day file has been generated, OCMS stores the data in the ocms_offence_avss_sftp database table to prepare for sending to vHub.

*Note: Whether this new table is required depends on whether vHub needs an SFTP end-day file, and if OCMS needs to store vHub’s acknowledgement file data.*

| offence_avss Database Field | Description | Sample Value |
| --- | --- | --- |
| batch_date | Date the batch was generated | 20240918125317 |
| offence_no | Notice Number | 500500303J |
| OffenceDateTime | Offence date in YYYYMMDDHHMMSS format to match HTX specs | 20250617140652 |
| offence_code | Offence Rule Code | 3A |
| location | Car park name | Aliwal Street |
| offence_description | Offence rule code description | Parking other than in a parking lot |
| vehicle_no | Vehicle number | JQE888 |
| vehicle_type | Vehicle category | C |
| vehicle_make | Vehicle make | Honda |
| vehicle_color | Vehicle colour | Red |
| amount_payable | Amount payable | 70 |
| record_status | Indicates whether the Notice should be sent as a new case, an updated case or as a case that vHub should cancel | ;N’ – New ‘U’ – Update ‘R’ - Remove |
| vhub_status | Indicates the Notice’s condition that matches vHub status | ‘O’ - Outstanding ‘S’ - Settled ‘C’ - Cancelled |
| | | |
| | | |
| | | |
| | | |
| cre_date | Date the record was created | 20240918125317 |
| cre_user_id | OCMS system name | ocmsizmgr |
| receipt_series | Part of receipt no. reflecting mode of payment | NT Receipt Series Remarks MD Manual Receipt NT AXS RT Singpost UO URA Online TE Sri Rosyani (URA officer) SG Saba Mohammed (URA officer) TF Pang (URA officer) TD Mehron (URA officer) |
| receipt_no | Receipt number if Notice has been paid | DICNOC1234567890 |
| receipt_check_digit | Receipt check digit | A |
| crs_date_of_suspension | Date CRS Reason of Suspension was applied | 20240918125317 |
| sent_to_vhub | Flag to indicate if the record was sent to vHub | 1 – Record sent 0 – Record not sent |
| ack_from_vhub | Flag to indicate if vHub acknowledgement was received | 1 – vHub Ack file received 0 – Ack file not received |
| sent_vhub_datetime | Date and time Notice/record was sent to vHub | 20240918130004 |
| ack_from_vhub_datetime | Date and time vHub acknowledgement was received | Blank when sending data to vHub |
| vhub_sftp_status_code | Status code returned by vHub API | ‘0’ - Success ‘1’ - Error |
| vhub_sftp_error_code | Error code returned by vHub API | E_REC_001 Blank if record has no error |
| Vhub_sftp_error_description | Error description returned by vHub API | Exceed Field Limit Blank if record has no error |

**3.4.2.4 Logging of End-Day file and vHub Acknowlegement File processing **

- OCMS will log the status of the cron job that generates and sends the end-day file in the ocms_batch_job database table.
- OCMS will also log the status of the cron job that picks up and processes vHub’s acknowledgement file in the ocms_batch_job database table.
**3.4.3 vHub NTL data processing**

- **Use Case**
- vHub sends a daily NTL (Not to Land) file to OCMS via SFTP at a pre-defined scheduled time.

- The NTL file provides a list of vehicle numbers that were enforced against and labelled as NTL, and the offence and NTL information related to the vehicles: Details about the vehicle’s offences 
- Information of the NTL 
- OCMS runs a scheduled cron job to pick up the NTL file and store the data. 

- The NTL data will be added to the Daily Summary Report for sent to and received from vHub. 
**3.4.3.2 Process Flow for Processing NTL File**

NOTE: Due to page size limit, the full-sized image is appended.

| No. | Process | Description |
| --- | --- | --- |
| 1 | Process vHub NTL file cron starts | A scheduled cron job triggers the process to pick up vHub’s NTL file from the SFTP. |
| 2 | Download File from vHub SFTP | OCMS downloads the NTL file from the vHub SFTP server. |
| 3 | Download Success? | OCMS checks if the file download was successful. If download was unsuccessful, OCMS sends an interface error email and the process stops. If the download was successful, OCMS proceeds to Step 4. |
| 4 | Upload File to Azure Blob | OCMS uploads the file to Azure Blob Storage for storing. |
| 5 | Upload Success? | OCMS checks if the upload to Azure Blob was successful. If upload was unsuccessful, OCMS sends an interface error email and the process stops. If the upload was successful, OCMS proceeds to Step 6. |
| 6 | Send to SLIFT to Decrypt | OCMS then sends the file to SLIFT for decryption. |
| 7 | Decrypt Success? | OCMS checks if the file decryption via SLIFT was successful. If an error is encountered, OCMS sends an interface error email and the process stops. If decryption was successful, OCMS proceeds to Step 8. |
| 8 | Process NTL File | OCMS proceeds to read the file and save the NTL data to the OCMS database. |
| 9 | End | Process ends. |

**3.4.3.3 Sub-flow for processing NTL file**

NOTE: Due to page size limit, the full-sized image is appended.

**3.4.4 Daily Summary Report for records sent to and received from vHub **

- The vHub Daily Summary Report contains the record counts and details of records interfaced with vHub. 

- The report contains the following sections: Summary
- Records sent to vHub
- Records that encountered error during the sending process
- Records processed by vHub
- List of NTL vehicles from vHub 

- OICs can access the Reports module in the Staff Portal to export the report via the “VHub Reports Section”. 

- Sample Report

**3.5 Sending Foreign Vehicle Information to REPCCS for Enforcement 
**This function is drawn from the OCMS 33.1 User Story, attached below. 

The functions are listed in OCMS 14 – Foreign Vehicle section to illustrate the end-to-end flow for the processing of a foreign vehicle Notice. *Note: This section will also be copied over to OCMS33.1 when functional document for OCMS33 is written later on.

**3.5.1 Description of use case for sending FOR as listed vehicles **

- OCMS will sends a daily list of foreign vehicles with outstanding offences (also known as Listed Vehicles) to REPCCS for enforcement. 

- The list will be sent to REPCCS via SFTP once a day at a pre-agreed timing. 

- The interface is based on REPCCS’ Interfaces to Other URA System(s) and External System(s) v2.0 (dated 6 Dec 2023).
**3.5.2 Process Flow for Sending Listed Vehicles to REPCCS**

** 
**

NOTE: Due to page size limit, the full-sized image is appended.

| No. | Process | Description |
| --- | --- | --- |
| 1 | Gen REP Listed Vehicle cron starts cron starts | A scheduled cron job triggers the generation process to create or update violation cases for foreign vehicles. |
| 2 | Get FOR &amp; Calculate Date | A function runs to get the value of parameter ID FOR from the database and calculates the relevant date. Note: Refer to the sub-flow for “Get FOR parameter &amp; calculate date function” in Section 3.4.1.2 |
| 3 | Generate Listed Vehicle File | OCMS system generates the listed vehicle file containing all qualifying unpaid foreign vehicle notices. Conditions Suspension type = PS EPR_reason_of_suspension = FOR Amount_paid = 0 Notice_date_and_time = current date minus X days (where X refers to the FOR parameter) |
| 4 | Send to SLIFT for Encryption | The file is sent to SLIFT for encryption before transfer. |
| 5 | Encrypt Success? | OCMS checks whether the encryption via SLIFT was successful. If encryption fails, OCMS sends an interfacing error email. If encryption is successful, OCMS proceeds to Step 6. |
| 6 | Upload File to Azure Blob | If encryption is successful, the file is uploaded to an Azure Blob storage location. |
| 7 | Upload Success? | OCMS checks whether the upload to Azure Blob was successful. If upload fails, OCMS sends an interfacing error email. If upload is successful, OCMS proceeds to Step 8. |
| 8 | Upload File to SFTP | If the Azure upload succeeds, the file is then uploaded to the SFTP server for REPCCS to collect. If upload fails, OCMS sends an interfacing error email. If upload is successful, OCMS proceeds to Step 9. |
| 9 | End | The process is completed. |

**3.5.3 Sub-flow for Sending Listed Vehicles to REPCCS**

NOTE: Due to page size limit, the full-sized image is appended.

**3.5.4 Schedule for sending Listed Vehicles to REPCCS via SFTP**

| Activity | Time | Pre-condition |
| --- | --- | --- |
| OCMS generates file | TBD | |
| OCMS drops file into SFTP | TBD | |
| REPCCS picks up file | TBD | |

**
3.5.4 Data sent to REPCCS via SFTP **

| REPCCS Data Field | OCMS Data Field | OCMS Database Table | Sample Value |
| --- | --- | --- | --- |
| Licence Plate | vehicle_no | ocms_valid_offence_notice | JQE888 |
| OBU Label | OCMS will send blank value | | Blank value |
| Reason Description | OCMS will hardcode the reason | Outstanding notices. Call URA 63293540 during office hours. |

**3.6 Sending Foreign Vehicle Information to CES EHT for Enforcement 
**This function is drawn from the OCMS 33.1 User Story, attached below. 

 

**
3.6.1 Use Case**

- OCMS will send a daily list of foreign vehicles with outstanding offences (also known as Listed Vehicles) to Certis (CES) for enforcement. 
- The list will be sent to Certis CES via SFTP XX a day at a pre-agreed timing.

- The interface described in this section is based on the specifications for EHT Wanted Vehicles listed in OCMS’ “OCMS - CES Interfacing Specifications” v0.2 (updated 22 July 2025). 

**3.6.2 Process Flow for Generating Tagged Vehicles List for CES EHT**

NOTE: Due to page size limit, the full-sized image is appended.

| No. | Process | Description |
| --- | --- | --- |
| 1 | Gen CES Tagged Vehicle List cron starts | A scheduled cron job triggers the generation process to generate the list of foreign vehicles to send to Certis as Tagged Vehicles |
| 2 | Get FOR &amp; Calculate Date | A function runs to get the value of parameter ID FOR from the database and calculates the relevant date. |
| 3 | Generate Listed Vehicle File | OCMS system generates the listed vehicle file containing all qualifying unpaid foreign vehicle notices. Conditions Suspension type = PS EPR_reason_of_suspension = FOR Amount_paid = 0 Notice_date_and_time = current date minus X days (where X refers to the FOR parameter) |
| 4 | Send to SLIFT for Encryption | The file is sent to SLIFT for encryption before transfer. |
| 5 | Encrypt Success? | OCMS checks whether the encryption via SLIFT was successful. If encryption fails, OCMS sends an interfacing error email. If encryption is successful, OCMS proceeds to Step 6. |
| 6 | Upload File to Azure Blob | If encryption is successful, the file is uploaded to an Azure Blob storage location. |
| 7 | Upload Success? | OCMS checks whether the upload to Azure Blob was successful. If upload fails, OCMS sends an interfacing error email. If upload is successful, OCMS proceeds to Step 8. |
| 8 | Upload File to SFTP | If the Azure upload succeeds, the file is then uploaded to the SFTP server for vHub to collect. If upload fails, OCMS sends an interfacing error email. If upload is successful, OCMS proceeds to Step 9. |
| 9 | End | The process is completed. |

**3.6.3 Sub-flow for generating list of tagged vehicle for CES EHT
***Refer to Section 3.5.3 for the sub-flow as the process is identical.*

**3.6.4 Data sent to CES EHT via SFTP **

| | | | |
| --- | --- | --- | --- |
| | | | |
| | | |
| | | | |

| CES EHT Data Field | OCMS Data Field | OCMS Database Table | Sample Value |
| --- | --- | --- | --- |
| Licence Plate | vehicle_no | ocms_valid_offence_notice | SNC7291R |
| OBU Label | OCMS will send as blank value | | Blank |
| Reason Description | OCMS will hardcode the reason | Outstanding notices. Call URA 63293540 during office hours. |

**3.7 Heavier Penalties for Unpaid Foreign Vehicle Notices **

**3.7.1 About heavier penalties**

- A heavier penalty is applied onto a foreign vehicle Notice that remains unpaid for a pre-defined period after the Offence Date, for example 90 days after the Offence Date.
- The pre-defined period is defined by the parameter value ‘FOD’ in the Parameter database table. 
- When a foreign vehicle Notice meets the criteria for the parameter ‘FOD’, OCMS automatically applies an Admin Fee to the Notice. 
- The Admin Fee amount is defined by the parameter value ‘AFO’ in the Parameter database table. 
**3.7.2 Process flow for Heavier Penalties
**

NOTE: Due to page size limit, the full-sized image is appended.

****

**3.7.3 Managing ‘FOD’, ‘AFO’ and ‘FOR’ Parameters
***Refer to Section 2 of the OCMS 39 Functional Document.*


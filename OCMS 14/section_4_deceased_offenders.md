**Section 4 - Notice Processing Flow for Deceased Offenders**

**4.1 Use Case**

- OCMS receives the life status of an Offender when it performs the Owner, Hirer or Driver particulars checks with MHA for NRIC holders or DataHive for FIN holders. *Refer to OCMS 8 – Retrieve Offender Particulars Functional Specifications Document.
*
- OCMS detects that the Offender is deceased when:MHA returns the Life Status of the Offender (NRIC holder) as “D – Dead”, OR 
- DataHive responds that the FIN holder is listed in the Dead Foreign Pass Holders dataset. 

- Upon detection, the Notice will be processed as a Special Notice where OCMS will permanently suspend the Notice with PS-RIP or PS-RP2, so that the Notice no longer continues along the standard processing flow. 
- OICs will be notified of the Notice’s deceased offender status via the following ways, so that they can follow up on the Notice:The “RIP Hirer/Driver Furnished” report which lists the notices that meet these conditions: PS-RP2 has been applied to the Notice 
- The deceased Offender is either the Hirer or Driver
- A superscript “R” beside a Notice that has an active PS-RIP or PS-RP2 suspension. The superscript should be displayed beside the Notice no. on all view screens, including the search results list. 
- . The “Date of Death” field in the Owner/Hirer/Driver particulars section will be populated if the date is provided by MHA or DataHive.
**4.2 High Level Processing Flow**

| No. | Process | Description |
| --- | --- | --- |
| 1 | Identify Deceased Offender | Offender is identified as deceased either through: MHA returning NRIC Life Status as “D”, or FIN holder found in DataHive’s “Dead Foreign Pass Holders” dataset. |
| 2 | Update Offender Particulars | OCMS updates the offender’s particulars in the database to store the deceased status. |
| 3 | Suspend Notice | Based on the date of death, the notice is permanently suspended using either PS-RIP or PS-RP2 code. |
| 4 | Generate RP2 Report for Hirer/Driver | A report titled “RIP Hirer Driver Furnished” is generated for all eligible RP2 notices. |
| 5 | Manual follow up by OIC | The OIC reviews the PS-RIP and PS-RP2 Notices and may perform any of the follow-up actions below: Revive PS and redirect a Notice to the Owner if an RIP ID was wrongly furnished Revive PS and redirect a Notice to the Hirer or Driver if they are furnished by the Deceased Offender’s Next-of-Kin. |
| 6 | End | The process flow of a Notice with a Deceased Offender ends, and the Notice may continue along the standard Notice processing flow if it has been redirected after the PS-RIP or PS-RP2 is revived. |

**4.3 Detecting Deceased Offenders**

*Refer to OCMS 8 – Retrieve Offender Particulars Functional Specifications Document:*

- *Section 3.2.2 Process Output file returned by MHA*
- *Section 7.5 Process DataHive data for deceased FIN holders*
**4.4 Suspending Notices with Deceased Offenders**

- OCMS permanently suspends the Notices when MHA or DataHive identifies the current offender as deceased. 
- The Suspension Codes used indicate the reason for suspension, providing OICs with information on the scenario that triggered the suspension.

**4.4.1 Suspension Codes **

| Suspension Type | Suspension Code | Reason of Suspension |
| --- | --- | --- |
| PS | RIP | Motorist Deceased On or After Offence Date |
| PS | RP2 | Motorist Deceased Before Offence Date |

**4.4.2 Suspension, Furnish hirer/driver, Payment Rules **

| Condition | Rule | Description |
| --- | --- | --- |
| PS-RIP or PS-RP2 can be applied at these stages | NPA, eNA, ROV, RD1, RD2, RR3, DN1, DN2, DR3, CPC | The suspension code can be applied at any of the stages listed. |
| Authorised OCMS users can apply PS-RIP or PS-RP2 | Yes | Authorised OCMS users can apply the suspension using the Staff Portal |
| PLUS users can apply PS-RIP or PS-RP2 | No | PS-RIP and PS-RP2 will not be displayed as suspension options in the PLUS Staff Portal |
| Notice is payable | Yes | Notice is payable via the eService and AXS |
| Notice is furnishable | No | Notice is not furnishable |
| Allow additional TS | Yes | TS can be applied to the outstanding Notice when there is active PS-RIP or PS-RP2 |
| Allow additional PS | Yes | PS can be applied to the Notice when there is active PS-RIP or PS-RP2 To allow CRS suspensions (PS-FP and PS-PRA) to be applied WITHOUT PS REVIVAL. i.e. the ERP reason of Suspension will be updated as PS-RIP or PS-RP2 and CRS Reason of Suspension will be updated as PS-FP. For non-CRS suspension (e.g. PS-APP) applied over PS-RIP or PS-RP2, PS revival IS REQUIRED. |

**

4.5 Updating Data for Notices with Deceased Offenders 
**

**4.5.1 Update Offender Particulars in ocms_offence_notice_owner_driver database table**

| Database Field | Description | Sample Value |
| --- | --- | --- |
| life_status | Alive or dead status returned by MHA or DataHive. For DataHive, if FIN ID is found in the D90 dataset, life status will be updated as ‘D’. If FIN ID is not in D90, life status will be updated as ‘A’. This serves as the RIP indicator when the value is ‘D’. | ‘A’ – Alive ‘D’ – Dead |
| date_of_death | Date of death returned by MHA or DataHive | 2024-09-18 00:00:00 |

**4.5.2 Update Notice Suspension in ocms_valid_offence_notice Intranet database table**

| Database Field | Description | Sample Value |
| --- | --- | --- |
| suspension_type | Type of suspension | PS |
| epr_reason_suspension | EPR suspension code that indicates the reason for the suspension | ‘RIP’ – If Offender’s date of death is on or after Offence Date OR ‘RP2’ – If Offender’s date of death is before Offence date |
| epr_reason_suspension_date | Date and time the suspension was applied | 2025-03-22 19:00:02 |

**
**

**4.5.3 Update Notice Suspension in eocms_valid_offence_notice Internet database table**

*Same fields as ocms_valid_offence_notice
*

**4.5.4 Update Suspension Details in ocms_suspended_notice database table**

| Database Field | Description | Sample Value |
| --- | --- | --- |
| notice_no | Notice number | 500500303J |
| date_of_suspension | Date and time the suspension was applied | 2025-03-22 19:00:02 |
| sr_no | Serial number for the suspension, running number | 1 |
| suspension_source | Which system initiated the suspension | Ocmsizmgr – OCMS backend |
| suspension_type | Type of suspension | PS |
| reason_of_suspension | EPR suspension code that indicates the reason for the suspension | ‘RIP’ – If Offender’s date of death is on or after Offence Date ‘RP2’ – If Offender’s date of death is before Offence date |

**4.6 Displaying Notices with PS-RIP and PS-RP2 in the OCMS Staff Portal**

- Notices with an active PS-RIP or PS-RP2 suspension will display a superscript “R” next to the Notice Number. 

- This allows OICs to quickly identify the Notices with deceased Offenders at a glance. 
- The RIP superscript “R” will be displayed in the Search Notice result summary list, next to the Notice Number.

- The superscript will also be displayed in the fixed header box and Overview tab of the View Notice UI, next to the Notice Number.

- The RIP superscript will be activated when the OCMS Staff Portal detects that the Notice’s current suspension is PS-RIP or PS-RP2, even if it is not the latest suspension. 

- Specifically, it will be activated when the Staff Portal detects the following values in the Notice details returned in the ocms_suspended_notice API:

| Database Field | Description | Sample Value |
| --- | --- | --- |
| suspension_type | Type of suspension | PS |
| epr_reason_suspension | EPR suspension code that indicates the reason for the suspension | ‘RIP’ – If Offender’s date of death is on or after Offence Date OR ‘RP2’ – If Offender’s date of death is before Offence date |
| date_of_revival | Date and time the suspension was revived | BLANK |
| revival_reason | Three-letter code that represents the reason for the revival | BLANK |

- The RIP superscript will be removed when the PS-RIP or PS-RP2 suspension is lifted. 
**4.7 RIP Hirer/Driver Furnished Report **

- The “RIP Hirer/Driver Furnished Report” is sent only when OCMS encounters the following conditions while updating the Offender’s particulars received from MHA or DataHive:Notice has been suspended with PS-RP2 as Offender’s Date of Death is before the Offence Date 
- The Offender is a Hirer or Driver 

- The report will be sent to OICs via email once a day. Post MVP, a work item with the report will be routed to the users’ dashboard. The report will also be downloadable from Staff Portal. 
- OCMS will not send the report if no exceptions occurred on that day. 

- The report will be sent once a day, at a scheduled time daily. [Timing TBD with users]
**4.7.1 Process Flow for Generating the RIP Hirer/Driver Furnished Report**

NOTE: Due to page size limit, the full-sized image is appended.

****

| S/N | Process Name | Description of Process |
| --- | --- | --- |
| 1 | Start | The Generate RIP Hirer/Driver Furnished Report process begins. |
| 2 | Query RP2 Suspended Notices | OCMS performs a batch query to: Get the list of Notices which have been suspended with PS-RP2 today Get the corresponding list of Current Offenders who are Hirers or Drivers, and have been recorded as deceased |
| 3 | Any records? | OCMS checks if there are any records that match the criteria. If there are no records found, the process ends. If there are records that match the criteria, OCMS proceeds to Step 4. |
| 4 | List Qualified Results | OCMS consolidates the list of Notices and Offender details. |
| 5 | Format Data | OCMS formats the data according to the report template. |
| 6 | Generate Report | OCMS generates the report. |
| 7 | Send Email | OCMS sends the report to the OICs via email. |
| 8 | End | Process concludes. |

**4.7.2 Sample Report**

**
**

**4.8 Redirecting PS-RIP &amp; PS-RP2 Notices **

- As part of the follow-up actions for PS-RIP and PS-RP2 Notices, OICs will review the Notices and OIC may redirect a Notice under these scenarios: Redirect the Notice back to the Owner if they have wrongly furnished a deceased Hirer or Driver 
- Redirect the Notice to the Hirer or Driver if they were furnished by the deceased Offender’s next-of-kin.

- To redirect a Notice, the OIC will: Revive the PS-RIP or PS-RP2 suspension in the Staff Portal
- Update the Owner, Hirer or Driver particulars in the Staff Portal
3. The OCMS backend automatically redirects the notice to the updated offender after the OIC submits the updated Owner, Hirer, or Driver particulars.

**4.8.1 Process Flow for Redirecting a PS-RIP or PS-RP2 Notice**

****

****

| S/N | Process Name | Description of Process |
| --- | --- | --- |
| 1 | Start | The OIC’s workflow for redirecting a Notice begins. |
| 2 | Revive Notice | The OIC lifts the PS-RIP or PS-RP2 suspension to revive the Notice from OCMS Staff Portal. OCMS will update the following database tables: ocms_suspended_notice ocms_valid_offence_notice |
| 3 | Update Owner/Driver Details | The OIC updates the particulars of the Owner, Hirer, or Driver of the notice from OCMS Staff Portaland submits the update to OCMS Backend. |
| 4 | OCMS Backend receives the update request | OCMS Backend will update the particulars data after receiving the request. OCMS will update the following database tables : ocms_offence_notice_owner_driver ocms_offence_notice_owner_driver_address |
| 5 | Redirect Notice to New Offender | The OCMS Backend auto redirects the notice to the newly identified Offender. OCMS will update the following database tables: ocms_offence_notice_owner_driver ocms_valid_offence_notice The Notice will be redirected back to the RD1 processing stage if: The new Offender is a Hirer The new Offender is the Owner The Notice will be redirected back to the DN1 processing stage if the new Offender is a Driver. |
| 6 | Resume Standard Notice Processing | The redirected Notice continues along the regular Notice processing flow. |
| 7 | End | The OIC’s workflow for redirecting a Notice ends. |

**4.8.2 Data Updates for Notice Revival **

**Database Table: ocms_valid_offence_notice (Intranet)**

The following fields in the Notice record will be updated

| Database Field | Description | Sample Value |
| --- | --- | --- |
| suspension_type | This field will be patched with the most recent active suspension type. If there is more than 1 active suspension, OCMS will patch the value of the most recent suspension If there is no active suspension, the field will be patched to “null” | null |
| epr_reason_suspension | This field will be patched with the most recent active suspension. If there is more than 1 active suspension, OCMS will patch the value of the most recent suspension If there is no active suspension, the field will be patched to “null” | null |
| epr_reason_suspension_date | This field will be patched with the date/time of the most recent active suspension. If there is more than 1 active suspension, OCMS will patch the value of the most recent suspension If there is no active suspension, the field will be patched to “null” | null |

**Database Table: eocms_valid_offence_notice (Internet)**

The same fields will be updated to the Notice record in the Internet database. 

**Database Table: ocms_suspended_notice **

The following fields in the suspension record will be updated

| Database Field | Description | Sample Value |
| --- | --- | --- |
| date_of_revival | Date and time the suspension was revived | 2025-03-22 19:00:02 |
| revival_reason | Three-letter code that represents the reason for the revival | PSR |
| officer_authorising_revival | Name of the OIC who revived the Notice | JOHNLEE |
| revival_remarks | Description of the revival reason | Permanent suspension revival |

**4.8.5 Data Updates for Updating Owner, Hirer or Driver Particulars **

*Refer to Section 4 of the OCMS 6 Functional Document.*

**4.8.6 Data Updates for Redirecting a Notice **

**Database Table: ocms_offence_notice_owner_driver **

OCMS will update the field for **both** the Deceased Person and New Offender records.

| Database Field | Description | Sample Value |
| --- | --- | --- |
| offender_indicator (Deceased Person) | The Offender Indicator for the Deceased Person will be changed to ‘N’ - No | N |
| offender_indicator (New Offender) | The Offender Indicator for the New Offender will be changed to ‘Y’ - Yes | Y |

** **

**Database Table: ocms_valid_offence_notice (Intranet)**

| Database Field | Description | Sample Value |
| --- | --- | --- |
| | | |
| | | |
| Next Processing Stage | The Notice’s next processing stage | RD1 / DN1 |
| Next Processing Date | Date the Notice will be processed to enter the next processing stage | current date |
| h | | |
| | | |

**Database Table: eocms_valid_offence_notice (Internet)**

The same fields will be updated to the Notice record in the Internet database. 

**4.9 Pending items to be implemented post MVP **

For OCMS MVP, the screen functions to amend the RIP letter template, initiate a vehicle ownership check and to generate the Quarterly RIP Report, PDF reminder letters to next-of-kin, and RIP Letters Sent Report will not be implemented (OCMS 14.12 to 14.16). 


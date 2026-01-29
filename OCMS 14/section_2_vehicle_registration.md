**Section 2 - Detecting Vehicle Registration Type**

**2.1 Use Case**

- OCMS identifies the vehicle registration type during Notice creation by processing raw Offence Data received from REPCCS, CES-EHT, EEPS, PLUS, and the OCMS Staff Portal (and other potential new offence sources).

- If the data source indicates that the vehicle registration type is “F – Foreign,” OCMS accepts it and proceeds with Notice creation.

- If the vehicle registration type provided is “S – Local,” OCMS checks the vehicle number using a backend detection algorithm to determine if it is a:Diplomatic vehicle 
- Military vehicle 
- Vehicle with a valid VIP Parking Label; or 
- Local vehicle 

Based on the outcome, OCMS assigns the correct registration type and creates the Notice, before routing the Notice to the appropriate processing flow.

**2.2 Vehicle Registration Type Check Within the Notice Creation Flow**

- The vehicle registration type check is a function within OCMS’ Notice Creation processing flow.
 .
- The registration type is identified prior to Notice creation, so that the Notices can be created with the corresponding attributes that would determine the Notice’s next processing steps/flow after it has been created. 

- The diagram below (from the revised OCMS 11 high level flow) shows how the vehicle registration type check function supports the main flow. The function is highlighted in green for ease of identification.

*Note: Image below is for illustration purposes only. Please refer to the OCMS 11 Functional Document for the latest updates, comments and feedback. 
*

NOTE: Due to page size limit, the full-sized image is appended.

****

**2.3 Process Flow of the Vehicle Registration Type Check**

 

 

NOTE: Due to page size limit, the full-sized image is appended.

| No. | Process | Description |
| --- | --- | --- |
| 1 | Start Vehicle Registration Type Check | OCMS initiates the vehicle registration type check function. |
| 2 | Check if Source Sent Registration Type = F | OCMS checks the value of the vehicle registration type sent by the source. If the registration type provided is “F”, OCMS returns the value “F” (Foreign Vehicle) to the Notice creation processing flow. If the registration type provided is not “F”, OCMS determines that the vehicle registration type is ‘S’, ‘D’ or ‘I’ and proceeds to Step 3. |
| 3 | Check for blank or null Vehicle Number and the offence notice type is ‘U’ | OCMS detects whether the vehicle number was provided by the source. If the vehicle number was provided by the Source, OCMS proceeds to Step 4. If vehicle number field is blank or null, OCMS proceeds to check whether the Offence Type is ‘U’ (UPL notices). If the Offence Type is ‘U’, OCMS returns the value “X” (UPL Dummy Vehicle) to the Notice creation processing flow. If the Offence Type is ‘O’ or ‘E’, OCMS returns an error to te Create Notice flow as vehicle number must be available for both offence types. |
| 4 | Call LTA Checksum Utility for Vehicle Number Validation | OCMS calls the LTA checksum utility to validate the vehicle number. If the LTA validation Return Code is 1 , OCMS returns the value “S” (Local) to the Notice creation processing flow. If the LTA validation Return Code is not 1 , OCMS proceeds to Step 5. |
| 5 | Check for Diplomatic Vehicle Number Format | OCMS checks if the vehicle number meets all the criteria that qualifies it as a Diplomatic Vehicle: Prefix = S, AND Suffix = Either CC, CD, TC or TE If vehicle number meets the criteria, OCMS returns the value “D” (Diplomat) to the Notice creation processing flow If vehicle number does not meet the criteria, OCMS proceeds to Step 6. |
| 6 | Check for Military Vehicle Number Format | OCMS checks if the vehicle number meets either one of the following conditions that qualifies it as a Military Vehicle: Prefix = MID or MINDEF, OR Suffix = MID or MINDEF If vehicle number meets any one of the conditions criteria, OCMS returns the value “I” (Military) to the Notice creation processing flow If vehicle number does not meet any condition, OCMS proceeds to Step 7. |
| 7 | Query CAS to check whether Vehicle is a VIP vehicle | OCMS uses the vehicle number to query the CAS VIP_VEHICLE database (for MVP) to check if the vehicle number has a valid VIP Parking Label. If the vehicle number is not found, OCMS proceeds to step 8 If the vehicle number is found, OCMS proceeds to Step 7a to check the VIP Parking Label status |
| 7a | Check if the VIP Parking Label is valid | OCMS checks the status of the vehicle’s VIP Parking Label listed in the database. During the transition period using CAS If the status of the Parking Label is ‘A’ - Active, OCMS returns the value “V” (VIP) to the Notice creation processing flow If the status of the Parking Label is ‘D’ - Defunct, OCMS proceeds to Step 8 When FOMS is implemented If the status of the Parking Label is ‘A’ – Active and the offence date falls within the start and end dates of the validity, OCMS returns the value “V” (VIP) to the Notice creation processing flow If the status of the Parking Label is ‘D’ – Defunct or the offence date does not fall within the start and end dates of the validity, OCMS proceeds to Step 8 |
| 8 | Confirm vehicle is a Local Vehicle | If the vehicle does not meet the criteria for ‘V’ (VIP), OCMS confirms that the vehicle registration type is ‘S’ (Local Vehicle) and return the value ‘S’ to the Notice creation processing flow. |
| 9 | Return to Notice creation processing flow | When the vehicle registration type has been identified for a vehicle: The value is returned to the main Notice creation processing flow The vehicle registration type check ends |

 


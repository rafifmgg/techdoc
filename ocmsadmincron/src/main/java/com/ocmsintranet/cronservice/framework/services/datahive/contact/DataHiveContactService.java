package com.ocmsintranet.cronservice.framework.services.datahive.contact;

/**
 * Service interface for looking up contact information from DataHive
 * Supports both Singpass (NRIC/FIN) and Corppass (UEN) lookups
 */
public interface DataHiveContactService {
    
    /**
     * Look up contact information for a given owner ID
     * 
     * @param ownerId The owner ID to look up (NRIC, FIN, or UEN)
     * @param ownerIdType The type of owner ID
     * @param offenceNoticeNumber The offence notice number (for audit logging)
     * @return ContactLookupResult containing mobile/email availability information
     */
    ContactLookupResult lookupContact(String ownerId, String ownerIdType, String offenceNoticeNumber);
}
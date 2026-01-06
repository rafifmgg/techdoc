package com.ocmsintranet.cronservice.framework.services.datahive.contact;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result object for DataHive contact lookup operations
 * Contains information about whether mobile/email contacts were found
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactLookupResult {
    
    /**
     * Indicates if a mobile number was found (only applicable for NRIC/FIN)
     */
    private boolean mobileFound;
    
    /**
     * Indicates if an email address was found (applicable for all ID types)
     */
    private boolean emailFound;
    
    /**
     * The DataHive table queried (SINGPASS or CORPPASS)
     * Used for audit logging purposes
     */
    private String queryTable;
    
    /**
     * The actual mobile number found (if any)
     */
    private String mobileNumber;
    
    /**
     * The actual email address found (if any)
     */
    private String emailAddress;

    /**
     * Indicates if there was an error during request payload preparation
     * This includes validation errors, null/empty ID, invalid ID type, etc.
     */
    private boolean preparationError;

    /**
     * Indicates if there was an error during DataHive query execution
     * This includes connection errors, timeout, query failures, etc.
     */
    private boolean queryError;

    /**
     * Error message describing what went wrong (if any error occurred)
     */
    private String errorMessage;

    /**
     * Check if any contact information (mobile or email) was found
     * @return true if either mobile or email was found
     */
    public boolean hasContact() {
        return mobileFound || emailFound;
    }

    /**
     * Check if there was any error during the lookup process
     * @return true if there was either a preparation or query error
     */
    public boolean hasError() {
        return preparationError || queryError;
    }

    /**
     * Check if the lookup was successful (no errors and contact found)
     * @return true if no errors occurred and contact information was found
     */
    public boolean isSuccessful() {
        return !hasError() && hasContact();
    }
}
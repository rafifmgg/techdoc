// ============================================================================
// LtaSnowflakeProcessor.java - Snowflake and External System Integration
// ============================================================================
package com.ocmsintranet.cronservice.framework.workflows.agencies.lta.download.helpers;

import com.ocmsintranet.cronservice.framework.services.tablequery.TableQueryService;
import com.ocmsintranet.cronservice.framework.services.datahive.contact.DataHiveContactService;
import com.ocmsintranet.cronservice.framework.services.datahive.contact.ContactLookupResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime; 
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class LtaSnowflakeProcessor {
    
    @Autowired
    private TableQueryService tableQueryService;
    
    @Autowired
    private DataHiveContactService dataHiveContactService;

    public LtaSnowflakeProcessor(TableQueryService tableQueryService, 
                                DataHiveContactService dataHiveContactService) {
        this.tableQueryService = tableQueryService;
        this.dataHiveContactService = dataHiveContactService;
    }

    /**
     * Process Snowflake function for non-passport ID types
     * Checks for mobile/email and updates stage accordingly
     * 
     * This implementation queries different DataHive tables based on owner ID type:
     * - For NRIC (1) and FIN (9): Query V_DH_SNDGO_SINGPASSCONTACT_MASTER
     * - For UEN types (4-8, A-C): Query V_DH_GOVTECH_CORPPASS_DELTA
     */
    public void processSnowflakeFunction(Map<String, Object> record, LtaProcessingStageManager stageManager) {
        String offenceNoticeNumber = (String) record.get("offenceNoticeNumber");
        String vehicleNumber = (String) record.get("vehicleNumber");
        String ownerId = (String) record.get("ownerId");
        String ownerIdType = (String) record.get("ownerIdType");

        log.info("Processing Snowflake function for vehicle {} with notice {} (ID: {}, Type: {})",
                vehicleNumber, offenceNoticeNumber, ownerId, ownerIdType);

        // Call DataHiveContactService for contact lookup
        log.info("Starting DataHive contact lookup for notice {} - ID: {}, Type: {}",
                offenceNoticeNumber, ownerId, ownerIdType);

        ContactLookupResult lookupResult = dataHiveContactService.lookupContact(
                ownerId, ownerIdType, offenceNoticeNumber);

        log.info("DataHive lookup completed - Mobile: {}, Email: {}, Table: {}, HasContact: {}, HasError: {}, PrepError: {}, QueryError: {}",
                lookupResult.getMobileNumber(), lookupResult.getEmailAddress(),
                lookupResult.getQueryTable(), lookupResult.hasContact(),
                lookupResult.hasError(), lookupResult.isPreparationError(), lookupResult.isQueryError());

        // ===== 2-STAGE ERROR CHECKING (Per Diagram) =====

        // STAGE 1: Check for preparation errors (validation, ID type classification, query building)
        if (lookupResult.isPreparationError()) {
            log.error("Preparation error detected for notice {}: {}",
                    offenceNoticeNumber, lookupResult.getErrorMessage());
            log.info("Preparation error - logging and ending processing for notice {}", offenceNoticeNumber);
            // Per diagram: Preparation errors → Log error → End (no suspension, no further processing)
            return;
        }

        // STAGE 2: Check for query execution errors (DataHive connection, timeout, query failures)
        if (lookupResult.isQueryError()) {
            log.error("Query execution error detected for notice {}: {}",
                    offenceNoticeNumber, lookupResult.getErrorMessage());
            log.info("Query error - applying TS-SYS suspension for notice {}", offenceNoticeNumber);
            // Per diagram: Query errors → Apply TS-SYS → End (allows automatic retry via suspension revival)
            stageManager.applyTsSysStatus(record);
            return;
        }

        // SUCCESS PATH: No errors, process contact information
        log.info("DataHive lookup successful - processing contact information for notice {}", offenceNoticeNumber);

        if (lookupResult.hasContact()) {
            log.info("Contact found via DataHive for notice {}, updating stage to ENA", offenceNoticeNumber);

            // Update contact information in database if found
            if (lookupResult.isMobileFound() || lookupResult.isEmailFound()) {
                log.info("Updating contact info - Mobile found: {}, Email found: {}",
                        lookupResult.isMobileFound(), lookupResult.isEmailFound());
                updateContactInfoFromResult(record, lookupResult);
            }

            stageManager.updateProcessingStageToENA(record);
        } else {
            log.info("No contact found via DataHive for notice {}, updating to RD1", offenceNoticeNumber);
            stageManager.updateProcessingStageToRD1(record);
        }
    }

    /**
     * Update contact information from DataHive lookup result
     * 
     * @param record The LTA record containing offence notice information
     * @param lookupResult The result from DataHive contact lookup
     */
    private void updateContactInfoFromResult(Map<String, Object> record, ContactLookupResult lookupResult) {
        try {
            String offenceNoticeNumber = (String) record.get("offenceNoticeNumber");
            String ownerId = (String) record.get("ownerId");
            
            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo", offenceNoticeNumber);
            Map<String, Object> contactFields = new HashMap<>();
            
            // Update mobile number if available (only for Singpass)
            if (lookupResult.isMobileFound() && lookupResult.getMobileNumber() != null) {
                String mobileNumber = lookupResult.getMobileNumber();
                contactFields.put("offenderTelNo", mobileNumber);
                contactFields.put("offenderTelCode", "65"); // Default Singapore code
                log.info("Adding mobile number {} for notice {}", mobileNumber, offenceNoticeNumber);
            }
            
            // Update email address if available
            if (lookupResult.isEmailFound() && lookupResult.getEmailAddress() != null) {
                String emailAddress = lookupResult.getEmailAddress();
                contactFields.put("emailAddr", emailAddress);
                log.info("Adding email address {} for notice {}", emailAddress, offenceNoticeNumber);
            }
            
            // Add DataHive processing metadata
            // contactFields.put("datahiveProcessingDateTime", LocalDateTime.now());
            // contactFields.put("datahiveQueryTable", lookupResult.getQueryTable());
            
            if (!contactFields.isEmpty()) {
                List<Map<String, Object>> updatedRecords = tableQueryService.patch(
                    "ocms_offence_notice_owner_driver", filters, contactFields);
                
                if (updatedRecords != null && !updatedRecords.isEmpty()) {
                    log.info("Successfully updated contact information from DataHive for notice {} (ID: {})", 
                            offenceNoticeNumber, ownerId);
                } else {
                    log.warn("Failed to update contact information for notice {} (ID: {})", 
                            offenceNoticeNumber, ownerId);
                }
            } else {
                log.warn("No contact fields to update for notice {} (ID: {})", offenceNoticeNumber, ownerId);
            }
            
        } catch (Exception e) {
            log.error("Error updating contact information from result: {}", e.getMessage(), e);
        }
    }


    /**
     * Query passport data from Singapore & Compass systems
     * This would call external systems to get additional passport holder information
     */
    public void queryPassportData(Map<String, Object> record) {
        String ownerId = (String) record.get("ownerId");
        String offenceNoticeNumber = (String) record.get("offenceNoticeNumber");
        
        log.info("Querying passport data for owner ID: {} (Notice: {})", ownerId, offenceNoticeNumber);
        
        try {
            // In a real implementation, this would:
            // 1. Call Singapore passport verification system
            // 2. Call Compass system for additional data
            // 3. Update the owner_driver record with additional information
            // 4. Potentially check for address/contact information
            
            // Placeholder for external system calls
            log.info("Calling Singapore passport verification system for ID: {}", ownerId);
            // Map<String, Object> singaporeData = singaporePassportService.verifyPassport(ownerId);
            
            log.info("Calling Compass system for additional data for ID: {}", ownerId);
            // Map<String, Object> compassData = compassService.getPassportHolderData(ownerId);
            
            // Update owner_driver record with additional passport data if available
            Map<String, Object> filters = new HashMap<>();
            filters.put("noticeNo", offenceNoticeNumber);
            
            Map<String, Object> passportDataUpdates = new HashMap<>();
            // passportDataUpdates.put("passportVerified", true);
            // passportDataUpdates.put("verificationDate", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
            // Add other passport verification fields as needed
            
            if (!passportDataUpdates.isEmpty()) {
                List<Map<String, Object>> updatedRecords = tableQueryService.patch(
                    "ocms_offence_notice_owner_driver", filters, passportDataUpdates);
                
                if (updatedRecords != null && !updatedRecords.isEmpty()) {
                    log.info("Updated owner_driver record with passport verification data for notice {}", offenceNoticeNumber);
                } else {
                    log.warn("Failed to update owner_driver record with passport data for notice {}", offenceNoticeNumber);
                }
            }
            
        } catch (Exception e) {
            log.error("Error querying passport data for owner ID {}: {}", ownerId, e.getMessage(), e);
            // Continue processing even if passport verification fails
        }
    }
}
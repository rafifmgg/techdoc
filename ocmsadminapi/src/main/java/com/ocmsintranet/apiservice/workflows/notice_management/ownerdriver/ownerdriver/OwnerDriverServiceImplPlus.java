package com.ocmsintranet.apiservice.workflows.notice_management.ownerdriver;

import com.ocmsintranet.apiservice.crud.beans.CrudResponse;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriver;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriverId;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriverService;
import com.ocmsintranet.apiservice.workflows.notice_management.ownerdriver.dto.OffenceNoticeOwnerDriverPlusDto;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.dto.OffenceNoticeOwnerDriverWithAddressDto;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeService;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriverAddr.OcmsOffenceNoticeOwnerDriverAddr;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriverAddr.OcmsOffenceNoticeOwnerDriverAddrId;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriverAddr.OcmsOffenceNoticeOwnerDriverAddrService;
import com.ocmsintranet.apiservice.utilities.ParameterUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OwnerDriverServiceImplPlus implements OwnerDriverServicePlus {

    private final OcmsOffenceNoticeOwnerDriverService service;
    private final OcmsOffenceNoticeOwnerDriverAddrService addressService;
    private final OcmsValidOffenceNoticeService validOffenceNoticeService;

    @Override
    public ResponseEntity<?> processPlusOffenceNoticeOwnerDriver(Map<String, Object> requestBody) {
        log.info("Received PLUS offence notice owner driver request with body: {}", requestBody);

        try {
            // Validate that at least one of noticeNo or idNo is present
            if (requestBody == null) {
                log.warn("Request body is null");
                CrudResponse<?> errorResponse = createErrorResponse("Request body is required");
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            }

            // Check if any key starts with "noticeNo" or is "idNo"
            boolean hasNoticeNo = requestBody.keySet().stream().anyMatch(key -> key.startsWith("noticeNo"));
            boolean hasIdNo = requestBody.containsKey("idNo");

            if (!hasNoticeNo && !hasIdNo) {
                log.warn("Missing required field: at least one of noticeNo (or noticeNo with operators like noticeNo[$in]) or idNo must be provided");
                CrudResponse<?> errorResponse = createErrorResponse("At least one of noticeNo or idNo must be provided in the request body");
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            }

            // Create internal request body for offencenoticeownerdriverlist call
            Map<String, Object> internalRequestBody = new HashMap<>();

            // Copy all noticeNo related parameters (including noticeNo[$in], noticeNo[$ne], etc.)
            for (Map.Entry<String, Object> entry : requestBody.entrySet()) {
                if (entry.getKey().startsWith("noticeNo")) {
                    internalRequestBody.put(entry.getKey(), entry.getValue());
                }
            }

            // Copy idNo if present
            if (hasIdNo) {
                internalRequestBody.put("idNo", requestBody.get("idNo"));
            }

            // Handle pagination parameters
            Object skip = requestBody.get("$skip");
            if (skip != null) {
                internalRequestBody.put("$skip", skip);
            }

            Object limit = requestBody.get("$limit");
            if (limit != null) {
                internalRequestBody.put("$limit", limit);
            } else {
                internalRequestBody.put("$limit", 9999);
            }

            log.info("Calling internal offencenoticeownerdriverlist with parameters: {}", internalRequestBody);

            // Convert request body to the format expected by service
            Map<String, String[]> normalizedParams = ParameterUtils.normalizeRequestParameters(internalRequestBody);

            // Call internal service
            FindAllResponse<OffenceNoticeOwnerDriverWithAddressDto> internalResponse = service.getAllWithAddresses(normalizedParams);

            // Transform response to PLUS DTO format
            FindAllResponse<OffenceNoticeOwnerDriverPlusDto> plusResponse = transformToPlusOwnerDriverDto(internalResponse);

            log.info("Returning PLUS offence notice owner driver response with {} items",
                    plusResponse.getData().size());

            return ResponseEntity.ok(plusResponse);

        } catch (Exception e) {
            log.error("Error retrieving PLUS offence notice owner drivers: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public ResponseEntity<?> processPlusUpdateOffenceNoticeOwnerDriver(Map<String, Object> payload) {
        log.info("Received PLUS update offence notice owner driver request with body: {}", payload);

        try {
            // Validate required fields
            if (payload == null || !payload.containsKey("noticeNo") || !payload.containsKey("ownerDriverIndicator")) {
                log.warn("Missing required fields: noticeNo and ownerDriverIndicator");
                CrudResponse<?> errorResponse = createErrorResponse("Both noticeNo and ownerDriverIndicator are required in the request body");
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            }

            String noticeNo = (String) payload.get("noticeNo");
            String ownerDriverIndicator = (String) payload.get("ownerDriverIndicator");

            if (noticeNo == null || noticeNo.trim().isEmpty() ||
                ownerDriverIndicator == null || ownerDriverIndicator.trim().isEmpty()) {
                log.warn("Invalid noticeNo or ownerDriverIndicator values");
                CrudResponse<?> errorResponse = createErrorResponse("noticeNo and ownerDriverIndicator cannot be null or empty");
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            }

            // Validate noticeNo exists in OcmsValidOffenceNotice
            if (!validOffenceNoticeService.getById(noticeNo).isPresent()) {
                log.warn("Invalid noticeNo: {} - notice not found in database", noticeNo);
                CrudResponse<?> errorResponse = createErrorResponse("Invalid noticeNo: notice not found in database");
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            }

            // Create composite key
            OcmsOffenceNoticeOwnerDriverId entityId = new OcmsOffenceNoticeOwnerDriverId(noticeNo, ownerDriverIndicator);

            // Update owner/driver entity fields
            boolean entityUpdated = updateOwnerDriverEntity(entityId, payload);

            // Update address fields
            boolean addressUpdated = updateAddressEntity(entityId, payload);

            if (!entityUpdated && !addressUpdated) {
                log.info("No fields to update for noticeNo: {}, ownerDriverIndicator: {}", noticeNo, ownerDriverIndicator);
                CrudResponse<?> response = CrudResponse.success("No changes detected - update skipped");
                return new ResponseEntity<>(response, HttpStatus.OK);
            }

            log.info("Successfully updated offence notice owner driver: noticeNo={}, ownerDriverIndicator={}, entityUpdated={}, addressUpdated={}",
                    noticeNo, ownerDriverIndicator, entityUpdated, addressUpdated);

            // Return success response
            CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.UPDATE_SUCCESS);
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error updating PLUS offence notice owner driver: {}", e.getMessage(), e);
            CrudResponse<?> errorResponse = createErrorResponse("Error updating offence notice owner driver: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Transform internal OffenceNoticeOwnerDriverWithAddressDto response to PLUS DTO format
     */
    private FindAllResponse<OffenceNoticeOwnerDriverPlusDto> transformToPlusOwnerDriverDto(
            FindAllResponse<OffenceNoticeOwnerDriverWithAddressDto> internalResponse) {

        FindAllResponse<OffenceNoticeOwnerDriverPlusDto> plusResponse = new FindAllResponse<>();

        // Set pagination info
        plusResponse.setTotal(internalResponse.getTotal());
        plusResponse.setLimit(internalResponse.getLimit());
        plusResponse.setSkip(internalResponse.getSkip());

        // Transform data to PLUS DTO
        List<OffenceNoticeOwnerDriverPlusDto> transformedData = new ArrayList<>();

        if (internalResponse.getData() != null) {
            for (OffenceNoticeOwnerDriverWithAddressDto item : internalResponse.getData()) {
                transformedData.add(new OffenceNoticeOwnerDriverPlusDto(item));
            }
        }

        plusResponse.setData(transformedData);
        return plusResponse;
    }

    /**
     * Update owner/driver entity fields, create if not exists
     */
    private boolean updateOwnerDriverEntity(OcmsOffenceNoticeOwnerDriverId entityId, Map<String, Object> payload) {
        OcmsOffenceNoticeOwnerDriver patchedEntity = new OcmsOffenceNoticeOwnerDriver();
        boolean hasUpdates = false;

        // Check and update allowed fields
        if (payload.containsKey("idNo")) {
            patchedEntity.setIdNo((String) payload.get("idNo"));
            hasUpdates = true;
        }
        if (payload.containsKey("idType")) {
            patchedEntity.setIdType((String) payload.get("idType"));
            hasUpdates = true;
        }
        if (payload.containsKey("name")) {
            patchedEntity.setName((String) payload.get("name"));
            hasUpdates = true;
        }
        if (payload.containsKey("emailAddr")) {
            patchedEntity.setEmailAddr((String) payload.get("emailAddr"));
            hasUpdates = true;
        }
        if (payload.containsKey("offenderTelNo")) {
            patchedEntity.setOffenderTelNo((String) payload.get("offenderTelNo"));
            hasUpdates = true;
        }

        if (hasUpdates) {
            // Check if entity exists
            Optional<OcmsOffenceNoticeOwnerDriver> existingEntity = service.getById(entityId);

            if (existingEntity.isPresent()) {
                // Record exists - update it
                service.patch(entityId, patchedEntity);
                log.debug("Updated existing owner/driver entity for noticeNo: {}, ownerDriverIndicator: {}",
                        entityId.getNoticeNo(), entityId.getOwnerDriverIndicator());
            } else {
                // Record doesn't exist - create new one
                patchedEntity.setNoticeNo(entityId.getNoticeNo());
                patchedEntity.setOwnerDriverIndicator(entityId.getOwnerDriverIndicator());

                service.save(patchedEntity);
                log.debug("Created new owner/driver entity for noticeNo: {}, ownerDriverIndicator: {}",
                        entityId.getNoticeNo(), entityId.getOwnerDriverIndicator());

                // Create address entity from payload if mail address fields are present
                createAddressFromPayload(entityId, payload);
            }
        }

        return hasUpdates;
    }

    /**
     * Create address entity from payload mail address fields
     */
    private void createAddressFromPayload(OcmsOffenceNoticeOwnerDriverId entityId, Map<String, Object> payload) {
        OcmsOffenceNoticeOwnerDriverAddr address = new OcmsOffenceNoticeOwnerDriverAddr();
        boolean hasAddressData = false;

        // Check and set mail address fields
        if (payload.containsKey("mailPostalCode")) {
            address.setPostalCode((String) payload.get("mailPostalCode"));
            hasAddressData = true;
        }
        if (payload.containsKey("mailBldgName")) {
            address.setBldgName((String) payload.get("mailBldgName"));
            hasAddressData = true;
        }
        if (payload.containsKey("mailStreetName")) {
            address.setStreetName((String) payload.get("mailStreetName"));
            hasAddressData = true;
        }
        if (payload.containsKey("mailBlkHseNo")) {
            address.setBlkHseNo((String) payload.get("mailBlkHseNo"));
            hasAddressData = true;
        }
        if (payload.containsKey("mailFloorNo")) {
            address.setFloorNo((String) payload.get("mailFloorNo"));
            hasAddressData = true;
        }
        if (payload.containsKey("mailUnitNo")) {
            address.setUnitNo((String) payload.get("mailUnitNo"));
            hasAddressData = true;
        }

        if (hasAddressData) {
            // Set composite key fields
            address.setNoticeNo(entityId.getNoticeNo());
            address.setOwnerDriverIndicator(entityId.getOwnerDriverIndicator());
            address.setTypeOfAddress("furnished_mail");

            addressService.save(address);
            log.debug("Created address entity for noticeNo: {}, ownerDriverIndicator: {}, addressType: furnished_mail",
                    entityId.getNoticeNo(), entityId.getOwnerDriverIndicator());
        }
    }

    /**
     * Update address entity fields (furnished_mail only)
     */
    private boolean updateAddressEntity(OcmsOffenceNoticeOwnerDriverId entityId, Map<String, Object> payload) {
        OcmsOffenceNoticeOwnerDriverAddr address = new OcmsOffenceNoticeOwnerDriverAddr();
        boolean hasUpdates = false;

        // Only update mail address fields
        if (payload.containsKey("mailPostalCode")) {
            address.setPostalCode((String) payload.get("mailPostalCode"));
            hasUpdates = true;
        }
        if (payload.containsKey("mailBldgName")) {
            address.setBldgName((String) payload.get("mailBldgName"));
            hasUpdates = true;
        }
        if (payload.containsKey("mailStreetName")) {
            address.setStreetName((String) payload.get("mailStreetName"));
            hasUpdates = true;
        }
        if (payload.containsKey("mailBlkHseNo")) {
            address.setBlkHseNo((String) payload.get("mailBlkHseNo"));
            hasUpdates = true;
        }
        if (payload.containsKey("mailFloorNo")) {
            address.setFloorNo((String) payload.get("mailFloorNo"));
            hasUpdates = true;
        }
        if (payload.containsKey("mailUnitNo")) {
            address.setUnitNo((String) payload.get("mailUnitNo"));
            hasUpdates = true;
        }

        if (hasUpdates) {
            // Always use furnished_mail address type
            OcmsOffenceNoticeOwnerDriverAddrId addressId = new OcmsOffenceNoticeOwnerDriverAddrId(
                entityId.getNoticeNo(),
                entityId.getOwnerDriverIndicator(),
                "furnished_mail"
            );

            try {
                // Check if address record exists
                Optional<OcmsOffenceNoticeOwnerDriverAddr> existingAddress = addressService.getById(addressId);

                if (existingAddress.isPresent()) {
                    // Record exists - update it
                    addressService.patch(addressId, address);
                    log.debug("Updated existing address entity for noticeNo: {}, ownerDriverIndicator: {}, addressType: furnished_mail",
                            entityId.getNoticeNo(), entityId.getOwnerDriverIndicator());
                } else {
                    // Record doesn't exist - create new one
                    address.setNoticeNo(entityId.getNoticeNo());
                    address.setOwnerDriverIndicator(entityId.getOwnerDriverIndicator());
                    address.setTypeOfAddress("furnished_mail");

                    addressService.save(address);
                    log.debug("Created new address entity for noticeNo: {}, ownerDriverIndicator: {}, addressType: furnished_mail",
                            entityId.getNoticeNo(), entityId.getOwnerDriverIndicator());
                }
            } catch (Exception e) {
                log.error("Error processing address for noticeNo: {}, ownerDriverIndicator: {}, error: {}",
                        entityId.getNoticeNo(), entityId.getOwnerDriverIndicator(), e.getMessage());
                throw e;
            }
        }

        return hasUpdates;
    }

    /**
     * Create standardized error response
     */
    private CrudResponse<?> createErrorResponse(String message) {
        CrudResponse<?> errorResponse = new CrudResponse<>();
        CrudResponse.ResponseData<?> responseData = new CrudResponse.ResponseData<>();
        responseData.setAppCode(CrudResponse.AppCodes.BAD_REQUEST);
        responseData.setMessage(message);
        errorResponse.setData(responseData);
        return errorResponse;
    }
}

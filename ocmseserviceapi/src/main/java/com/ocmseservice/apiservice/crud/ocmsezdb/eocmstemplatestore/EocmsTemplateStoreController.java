package com.ocmseservice.apiservice.crud.ocmsezdb.eocmstemplatestore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocmseservice.apiservice.crud.BaseController;
import com.ocmseservice.apiservice.crud.beans.CrudResponse;
import com.ocmseservice.apiservice.crud.beans.FindAllResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1/eocmstemplatestore")
public class EocmsTemplateStoreController extends BaseController<EocmsTemplateStore, String, EocmsTemplateStoreService> {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Constructor with required dependencies
     */
    public EocmsTemplateStoreController(EocmsTemplateStoreService service) {
        super(service);
    }
    
    /**
     * Override the default create method to handle binary data conversion
     */
    @Override
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Object payload) {
        try {
            // Extract the ID from the payload
            String templateName = (String) ((Map<String, Object>) payload).get("templateName");
            
            if (templateName == null) {
                throw new IllegalArgumentException("templateName is required");
            }
            
            // Handle binary data conversion from Base64 string
            String binDataBase64 = (String) ((Map<String, Object>) payload).get("binData");
            
            // Create a new entity and set its properties manually
            EocmsTemplateStore entity = new EocmsTemplateStore();
            entity.setTemplateName(templateName);
            
            // Set other properties from the payload
            if (((Map<String, Object>) payload).containsKey("creUserId")) {
                entity.setCreUserId((String) ((Map<String, Object>) payload).get("creUserId"));
            }
            
            if (((Map<String, Object>) payload).containsKey("updUserId")) {
                entity.setUpdUserId((String) ((Map<String, Object>) payload).get("updUserId"));
            }
            
            // Convert Base64 string to byte array if provided
            if (binDataBase64 != null && !binDataBase64.isEmpty()) {
                entity.setBinData(Base64.getDecoder().decode(binDataBase64));
            }
            
            // Call the save method from service
            service.save(entity);
            
            // Return success response
            CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.SAVE_SUCCESS);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            // Return error response
            CrudResponse<?> errorResponse = CrudResponse.error(
                CrudResponse.AppCodes.BAD_REQUEST,
                "Error creating template store: " + e.getMessage()
            );
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * POST endpoint for listing template store records
     * Replaces the GET endpoint
     */
    @PostMapping("/templatestorelist")
    public ResponseEntity<FindAllResponse<EocmsTemplateStore>> getAllPost(@RequestBody(required = false) Map<String, Object> requestBody) {
        // Convert request body to the format expected by service
        Map<String, String[]> normalizedParams = new HashMap<>();
        
        if (requestBody != null) {
            requestBody.forEach((key, value) -> {
                if (value instanceof String) {
                    normalizedParams.put(key, new String[] { (String) value });
                } else if (value instanceof Number) {
                    normalizedParams.put(key, new String[] { value.toString() });
                } else if (value instanceof Boolean) {
                    normalizedParams.put(key, new String[] { value.toString() });
                } else if (value instanceof String[]) {
                    normalizedParams.put(key, (String[]) value);
                }
            });
        }
        
        FindAllResponse<EocmsTemplateStore> response = service.getAll(normalizedParams);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * POST endpoint for updating a template store record
     * Replaces the PATCH endpoint
     */
    @PostMapping("/templatestorepatch")
    public ResponseEntity<?> patchPost(@RequestBody Map<String, Object> payload) {
        try {
            // Extract the ID from the payload
            String templateName = (String) payload.get("templateName");
            
            if (templateName == null) {
                throw new IllegalArgumentException("templateName is required");
            }
            
            // Get existing entity
            Optional<EocmsTemplateStore> existingEntityOpt = service.getById(templateName);
            if (!existingEntityOpt.isPresent()) {
                throw new IllegalArgumentException("Template with name " + templateName + " not found");
            }
            
            EocmsTemplateStore existingEntity = existingEntityOpt.get();
            
            // Handle binary data conversion from Base64 string
            String binDataBase64 = (String) payload.get("binData");
            
            // Update properties from the payload
            if (payload.containsKey("updUserId")) {
                existingEntity.setUpdUserId((String) payload.get("updUserId"));
            }
            
            // Convert Base64 string to byte array if provided
            if (binDataBase64 != null && !binDataBase64.isEmpty()) {
                existingEntity.setBinData(Base64.getDecoder().decode(binDataBase64));
            }
            
            // Call the patch method from service
            service.patch(templateName, existingEntity);
            
            // Return success response
            CrudResponse<?> response = CrudResponse.success(CrudResponse.Messages.UPDATE_SUCCESS);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            CrudResponse<?> errorResponse = CrudResponse.error(
                CrudResponse.AppCodes.BAD_REQUEST,
                "Error updating template store: " + e.getMessage()
            );
            
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
}

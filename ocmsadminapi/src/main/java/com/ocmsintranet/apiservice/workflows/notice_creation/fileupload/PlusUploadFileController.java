package com.ocmsintranet.apiservice.workflows.notice_creation.fileupload;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;

@RestController
@RequestMapping("${api.version:v1}")
@Slf4j
public class PlusUploadFileController {

    private final UploadFileService uploadFileService;

    public PlusUploadFileController(UploadFileService uploadFileService) {
        this.uploadFileService = uploadFileService;
    }

    /**
     * Endpoint for uploading HHT files to temporary storage
     * The response from this endpoint contains file information that should be included
     * in the create notice payload when calling the create-notice endpoint.
     */
    @PostMapping("/plus-upload")
    public ResponseEntity<Map<String, Object>> uploadHhtFile(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file) {
        log.info("Received file upload request for HHT: {}", file.getOriginalFilename());
        return uploadFileService.uploadHhtFileToTemp(request, file);
    }
}

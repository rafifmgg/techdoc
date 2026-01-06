package com.ocmsintranet.cronservice.framework.workflows.agencies.toppan.report.helpers;

import com.ocmsintranet.cronservice.utilities.AzureBlobStorageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Helper class for Toppan report file retrieval and URL generation.
 * This class handles blob storage file operations and download URL generation.
 *
 * This helper provides functionality for:
 * 1. Retrieving Toppan report files from Azure Blob Storage based on date
 * 2. Generating secure download URLs with expiry
 * 3. Validating file existence and accessibility
 *
 * File naming conventions for 3 report types:
 * - DPT-URA-PDF-D2-YYYYMMDDHHMISS    (PDF D2 Report)
 * - DPT-URA-LOG-PDF-YYYYMMDDHHMISS  (LOG PDF Report)
 * - DPT-URA-LOG-D2-YYYYMMDDHHMISS   (LOG D2 Report)
 *
 * The files are expected to be pre-generated and stored in blob storage.
 */
@Slf4j
@Component
public class ToppanReportHelper {

    @Value("${blob.folder.toppan.download}")
    private String blobContainerName;

    @Value("${blob.url.expiry.hours:24}")
    private int urlExpiryHours;

    @Value("${toppan.report.baseurl}")
    private String toppanReportBaseUrl;

    private final AzureBlobStorageUtil azureBlobStorageUtil;

    // Download URL path - hardcoded as per requirement
    private static final String DOWNLOAD_URL_PATH = "/ocms/v1/download/blob";

    // File type constants
    public static final String FILE_TYPE_PDF_D2 = "PDF_D2";
    public static final String FILE_TYPE_LOG_PDF = "LOG_PDF";
    public static final String FILE_TYPE_LOG_D2 = "LOG_D2";
    public static final String FILE_TYPE_RD2_D2 = "RD2_D2";
    public static final String FILE_TYPE_DN2_D2 = "DN2_D2";

    // File prefix patterns
    private static final String PREFIX_PDF_D2 = "DPT-URA-PDF-D2-";
    private static final String PREFIX_LOG_PDF = "DPT-URA-LOG-PDF-";
    private static final String PREFIX_LOG_D2 = "DPT-URA-LOG-D2-";
    private static final String PREFIX_RD2_D2 = "DPT-URA-RD2-D2-";
    private static final String PREFIX_DN2_D2 = "DPT-URA-DN2-D2-";

    public ToppanReportHelper(AzureBlobStorageUtil azureBlobStorageUtil) {
        this.azureBlobStorageUtil = azureBlobStorageUtil;
    }

    /**
     * Checks if all 3 Toppan report files exist in blob storage for the specified date
     *
     * @param reportDate The date for which to check file existence (format: yyyy-MM-dd)
     * @return Map containing file existence status and metadata for all 3 files
     */
    public Map<String, Object> checkReportFileExists(String reportDate) {
        log.info("[Report] Checking Toppan report files existence for date: {}", reportDate);

        try {
            // Convert date format: yyyy-MM-dd -> YYYYMMDD
            String datePattern = reportDate.replace("-", "");

            log.info("[Report] Looking for files with date pattern: {} in container: {}", datePattern, blobContainerName);

            // List all files in the blob container with DPT-URA prefix
            List<String> allFiles = azureBlobStorageUtil.listFiles(blobContainerName);
            log.info("[Report] Total files found in blob with DPT-URA prefix: {}", allFiles.size());

            // Find the 3 specific files matching the date pattern
            Map<String, String> foundFiles = new HashMap<>();

            // Search for PDF-D2 file
            String pdfD2File = findFileByPattern(allFiles, PREFIX_PDF_D2, datePattern);
            if (pdfD2File != null) {
                foundFiles.put(FILE_TYPE_PDF_D2, pdfD2File);
                log.info("[Report] Found PDF-D2 file: {}", pdfD2File);
            }

            // Search for LOG-PDF file
            String logPdfFile = findFileByPattern(allFiles, PREFIX_LOG_PDF, datePattern);
            if (logPdfFile != null) {
                foundFiles.put(FILE_TYPE_LOG_PDF, logPdfFile);
                log.info("[Report] Found LOG-PDF file: {}", logPdfFile);
            }

            // Search for LOG-D2 file
            String logD2File = findFileByPattern(allFiles, PREFIX_LOG_D2, datePattern);
            if (logD2File != null) {
                foundFiles.put(FILE_TYPE_LOG_D2, logD2File);
                log.info("[Report] Found LOG-D2 file: {}", logD2File);
            }

            // Search for RD2-D2 file
            String rd2D2File = findFileByPattern(allFiles, PREFIX_RD2_D2, datePattern);
            if (rd2D2File != null) {
                foundFiles.put(FILE_TYPE_RD2_D2, rd2D2File);
                log.info("[Report] Found RD2-D2 file: {}", rd2D2File);
            }

            // Search for DN2-D2 file
            String dn2D2File = findFileByPattern(allFiles, PREFIX_DN2_D2, datePattern);
            if (dn2D2File != null) {
                foundFiles.put(FILE_TYPE_DN2_D2, dn2D2File);
                log.info("[Report] Found DN2-D2 file: {}", dn2D2File);
            }

            // Build result map
            Map<String, Object> result = new HashMap<>();
            result.put("reportDate", reportDate);
            result.put("containerName", blobContainerName);
            result.put("foundFiles", foundFiles);
            result.put("foundCount", foundFiles.size());

            boolean allFilesFound = foundFiles.size() == 5;
            result.put("allFilesFound", allFilesFound);

            if (allFilesFound) {
                log.info("[Report] All 5 report files found for date: {}", reportDate);
                result.put("status", "ALL_FILES_FOUND");
            } else {
                log.warn("[Report] Only {} out of 5 report files found for date: {}", foundFiles.size(), reportDate);
                result.put("status", "PARTIAL_FILES_FOUND");

                // Log which files are missing
                if (!foundFiles.containsKey(FILE_TYPE_PDF_D2)) {
                    log.warn("[Report] Missing: PDF-D2 file ({})", PREFIX_PDF_D2 + datePattern);
                }
                if (!foundFiles.containsKey(FILE_TYPE_LOG_PDF)) {
                    log.warn("[Report] Missing: LOG-PDF file ({})", PREFIX_LOG_PDF + datePattern);
                }
                if (!foundFiles.containsKey(FILE_TYPE_LOG_D2)) {
                    log.warn("[Report] Missing: LOG-D2 file ({})", PREFIX_LOG_D2 + datePattern);
                }
                if (!foundFiles.containsKey(FILE_TYPE_RD2_D2)) {
                    log.warn("[Report] Missing: RD2-D2 file ({})", PREFIX_RD2_D2 + datePattern);
                }
                if (!foundFiles.containsKey(FILE_TYPE_DN2_D2)) {
                    log.warn("[Report] Missing: DN2-D2 file ({})", PREFIX_DN2_D2 + datePattern);
                }
            }

            return result;

        } catch (Exception e) {
            log.error("[Report] Error checking file existence: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("allFilesFound", false);
            errorResult.put("foundCount", 0);
            errorResult.put("status", "ERROR");
            errorResult.put("error", e.getMessage());
            return errorResult;
        }
    }

    /**
     * Find a file in the list that matches the given prefix and date pattern
     *
     * @param files List of file paths
     * @param prefix File prefix to match (e.g., "DPT-URA-PDF-D2-")
     * @param datePattern Date pattern to match (e.g., "20241027")
     * @return Full file path if found, null otherwise
     */
    private String findFileByPattern(List<String> files, String prefix, String datePattern) {
        return files.stream()
                .filter(file -> file.contains(prefix + datePattern))
                .findFirst()
                .orElse(null);
    }

    /**
     * Generates download URLs for all 3 Toppan report files
     *
     * @param reportDate The date for which to generate download URLs (format: yyyy-MM-dd)
     * @return Map of file type to download URL, or empty map if files not found
     */
    public Map<String, String> generateDownloadUrls(String reportDate) {
        log.info("[Report] Generating download URLs for Toppan report date: {}", reportDate);

        Map<String, String> downloadUrls = new HashMap<>();

        try {
            // Check if files exist first
            Map<String, Object> fileCheck = checkReportFileExists(reportDate);
            boolean allFilesFound = (Boolean) fileCheck.getOrDefault("allFilesFound", false);

            if (!allFilesFound) {
                int foundCount = (Integer) fileCheck.getOrDefault("foundCount", 0);
                log.warn("[Report] Cannot generate all download URLs - only {} out of 3 files found", foundCount);
            }

            // Get the found files map
            @SuppressWarnings("unchecked")
            Map<String, String> foundFiles = (Map<String, String>) fileCheck.get("foundFiles");

            if (foundFiles != null && !foundFiles.isEmpty()) {
                // Generate download URL for each found file
                for (Map.Entry<String, String> entry : foundFiles.entrySet()) {
                    String fileType = entry.getKey();
                    String filePath = entry.getValue(); // This already contains the full path from listFiles

                    String downloadUrl = generateControllerBasedDownloadUrl(filePath);
                    downloadUrls.put(fileType, downloadUrl);

                    log.info("[Report] Generated download URL for {}: {}", fileType, downloadUrl);
                }
            }

            log.info("[Report] Generated {} download URLs for date: {}", downloadUrls.size(), reportDate);
            return downloadUrls;

        } catch (Exception e) {
            log.error("[Report] Error generating download URLs: {}", e.getMessage(), e);
            return downloadUrls; // Return empty map on error
        }
    }

    /**
     * Generates full download URL for blob file
     * Format: https://parking3.uraaz.gov.sg/ocms/v1/download/blob/offence/sftp/toppan/output/DPT-URA-PDF-D2-20241027143055?container=ocms
     *
     * @param blobPath The blob path returned from listFiles (e.g., "offence/sftp/toppan/output/DPT-URA-PDF-D2-20241027143055")
     * @return Complete download URL with domain, path, and container query parameter
     */
    private String generateControllerBasedDownloadUrl(String blobPath) {
        try {
            // Get base URL from toppan.report.baseurl property
            String baseUrl = toppanReportBaseUrl;

            // Clean up the blob path - remove leading slash if present
            String cleanPath = blobPath;
            if (cleanPath.startsWith("/")) {
                cleanPath = cleanPath.substring(1);
            }

            // Build the complete URL with container query parameter
            // Format: {baseUrl}/ocms/v1/download/blob/{cleanPath}?container=ocms
            // Example: https://parking3.uraaz.gov.sg/ocms/v1/download/blob/offence/sftp/toppan/output/DPT-URA-PDF-D2-20241027143055?container=ocms
            String downloadUrl = baseUrl + DOWNLOAD_URL_PATH + "/" + cleanPath + "?container=ocms";

            log.debug("[Report] Generated download URL: {}", downloadUrl);
            return downloadUrl;

        } catch (Exception e) {
            log.error("[Report] Error generating download URL: {}", e.getMessage(), e);
            return blobPath; // Return original path as fallback
        }
    }

}

package com.ocmsintranet.apiservice.workflows.reports.agency.toppan;

import com.ocmsintranet.apiservice.workflows.reports.agency.toppan.dto.ToppanReportRequestDto;
import com.ocmsintranet.apiservice.workflows.reports.agency.toppan.dto.ToppanReportResponseDto;
import com.ocmsintranet.apiservice.utilities.AzureBlobStorageUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ToppanReportServiceImpl implements ToppanReportService {

    // File prefix patterns for ACKNOWLEDGEMENT source
    private static final String PREFIX_PDF_D2 = "DPT-URA-PDF-D2-";
    private static final String PREFIX_LOG_PDF = "DPT-URA-LOG-PDF-";
    private static final String PREFIX_LOG_D2 = "DPT-URA-LOG-D2-";
    private static final String PREFIX_RD2_D2 = "DPT-URA-RD2-D2-";
    private static final String PREFIX_DN2_D2 = "DPT-URA-DN2-D2-";

    private final AzureBlobStorageUtil azureBlobStorageUtil;

    @Value("${azure.blob.toppan.input}")
    private String crsInputPath;

    @Value("${azure.blob.toppan.output}")
    private String acknowledgementOutputPath;

    @Value("${cors.allowed-origins}")
    private String corsAllowedOrigins;

    public ToppanReportServiceImpl(AzureBlobStorageUtil azureBlobStorageUtil) {
        this.azureBlobStorageUtil = azureBlobStorageUtil;
    }

    @Override
    public ResponseEntity<ToppanReportResponseDto> getToppanReport(ToppanReportRequestDto request) {
        try {
            log.info("Getting Toppan report for source: {}, reportDate: {}", request.getSource(), request.getReportDate());

            // Convert YYYY-MM-DD to YYYYMMDD for internal processing
            String reportDateYYYYMMDD = convertToYYYYMMDD(request.getReportDate());
            log.debug("Converted report date from {} to {}", request.getReportDate(), reportDateYYYYMMDD);

            // Determine folder path based on source
            String fullPath;

            if ("CRS".equalsIgnoreCase(request.getSource())) {
                fullPath = crsInputPath;
            } else if ("ACKNOWLEDGEMENT".equalsIgnoreCase(request.getSource())) {
                fullPath = acknowledgementOutputPath;
            } else {
                log.error("Invalid source type: {}", request.getSource());
                return ResponseEntity.badRequest().build();
            }

            // Parse container name and blob path from full path
            // Format: ocms/offence/sftp/toppan/input/ -> container: ocms, path: offence/sftp/toppan/input
            PathInfo pathInfo = parseContainerAndPath(fullPath);
            log.debug("Parsed container: {}, blob path: {}", pathInfo.containerName, pathInfo.blobPath);

            // Get files based on source type
            List<ToppanReportResponseDto.FileInfo> fileInfoList;

            if ("CRS".equalsIgnoreCase(request.getSource())) {
                // For CRS: list files in YYYYMMDD formatted folder (use converted YYYYMMDD format)
                String blobPathPrefix = pathInfo.blobPath + "/" + reportDateYYYYMMDD;
                log.info("Searching for CRS files in container: {}, path prefix: {}",
                        pathInfo.containerName, blobPathPrefix);

                List<AzureBlobStorageUtil.BlobFileInfo> blobFiles = azureBlobStorageUtil.listBlobsInContainer(
                        pathInfo.containerName,
                        blobPathPrefix
                );

                fileInfoList = convertBlobFilesToFileInfo(blobFiles, pathInfo.containerName);

            } else {
                // For ACKNOWLEDGEMENT: search for files with specific prefixes (use YYYYMMDD format)
                fileInfoList = getAcknowledgementFiles(pathInfo, reportDateYYYYMMDD);
            }

            // Build response
            ToppanReportResponseDto response = new ToppanReportResponseDto();
            response.setData(fileInfoList);
            response.setTotal(fileInfoList.size());
            response.setSource(request.getSource());
            response.setReportDate(request.getReportDate());

            log.info("Found {} files for source: {}, reportDate: {}", fileInfoList.size(), request.getSource(), request.getReportDate());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting Toppan report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Convert date from YYYY-MM-DD to YYYYMMDD format
     * @param reportDate Date in YYYY-MM-DD format (e.g., 2024-10-27)
     * @return Date in YYYYMMDD format (e.g., 20241027)
     */
    private String convertToYYYYMMDD(String reportDate) {
        if (reportDate == null || reportDate.length() != 10) {
            log.warn("Invalid report date format: {}, expected YYYY-MM-DD", reportDate);
            return reportDate;
        }

        try {
            // Remove dashes: 2024-10-27 -> 20241027
            return reportDate.replace("-", "");
        } catch (Exception e) {
            log.error("Error converting date: {}", reportDate, e);
            return reportDate;
        }
    }

    /**
     * Build the blob path prefix based on source type and report date
     * Different sources may have different path structures
     * @param source Source type (CRS or ACKNOWLEDGEMENT)
     * @param folderPath Base folder path
     * @param reportDate Report date (YYYY-MM-DD for CRS)
     * @return Full blob path prefix
     */
    private String buildBlobPathPrefix(String source, String folderPath, String reportDate) {
        // Remove leading slash if present for consistency
        String cleanPath = folderPath.startsWith("/") ? folderPath.substring(1) : folderPath;
        // Remove trailing slash if present
        cleanPath = cleanPath.endsWith("/") ? cleanPath.substring(0, cleanPath.length() - 1) : cleanPath;

        // For CRS: use YYYY-MM-DD format in path
        // Example: ocms/offence/sftp/toppan/input/2024-10-27
        return cleanPath + "/" + reportDate;
    }

    /**
     * Get ACKNOWLEDGEMENT files with specific prefixes
     * Searches for files matching: PREFIX + YYYYMMDD pattern
     */
    private List<ToppanReportResponseDto.FileInfo> getAcknowledgementFiles(PathInfo pathInfo, String reportDate) {
        List<ToppanReportResponseDto.FileInfo> allFiles = new ArrayList<>();

        // Define all prefixes with report date
        String[] filePrefixes = {
                PREFIX_PDF_D2 + reportDate,
                PREFIX_LOG_PDF + reportDate,
                PREFIX_LOG_D2 + reportDate,
                PREFIX_RD2_D2 + reportDate,
                PREFIX_DN2_D2 + reportDate
        };

        log.info("Searching for ACKNOWLEDGEMENT files with {} prefixes in container: {}, path: {}",
                filePrefixes.length, pathInfo.containerName, pathInfo.blobPath);

        // Search for files with each prefix
        for (String prefix : filePrefixes) {
            try {
                // Build the full path prefix: blobPath/PREFIX-YYYYMMDD
                String fullPrefix = pathInfo.blobPath + "/" + prefix;
                log.debug("Searching for files with prefix: {}", fullPrefix);

                List<AzureBlobStorageUtil.BlobFileInfo> blobFiles = azureBlobStorageUtil.listBlobsInContainer(
                        pathInfo.containerName,
                        fullPrefix
                );

                // Convert and add to result list
                List<ToppanReportResponseDto.FileInfo> filesForPrefix = convertBlobFilesToFileInfo(blobFiles, pathInfo.containerName);
                allFiles.addAll(filesForPrefix);

                log.debug("Found {} files with prefix: {}", filesForPrefix.size(), prefix);

            } catch (Exception e) {
                log.error("Error searching for files with prefix: {}", prefix, e);
                // Continue with next prefix even if one fails
            }
        }

        log.info("Total ACKNOWLEDGEMENT files found: {}", allFiles.size());
        return allFiles;
    }

    /**
     * Convert BlobFileInfo list to FileInfo DTO list
     */
    private List<ToppanReportResponseDto.FileInfo> convertBlobFilesToFileInfo(
            List<AzureBlobStorageUtil.BlobFileInfo> blobFiles,
            String containerName) {

        List<ToppanReportResponseDto.FileInfo> fileInfoList = new ArrayList<>();

        for (AzureBlobStorageUtil.BlobFileInfo blobFile : blobFiles) {
            ToppanReportResponseDto.FileInfo fileInfo = new ToppanReportResponseDto.FileInfo();
            fileInfo.setFileName(blobFile.getFileName());

            // Build custom download URL: {url.toppan.download}/blobPath
            String customDownloadUrl = buildDownloadUrl(blobFile.getBlobPath());
            fileInfo.setDownloadUrl(customDownloadUrl);

            fileInfo.setFileSize(blobFile.getFileSize());
            fileInfoList.add(fileInfo);
        }

        return fileInfoList;
    }

    /**
     * Build custom download URL from blob path with container parameter
     * Format: {first-cors-origin}/ocms/v1/download/blob/{blobPath}?container=ocms
     * Example: https://ocmssitintraapp.azurewebsites.net/ocms/v1/download/blob/offence/sftp/toppan/output/DPT-URA-RD2-D2-20251027150000?container=ocms
     */
    private String buildDownloadUrl(String blobPath) {
        // Remove leading slash if present
        String cleanPath = blobPath.startsWith("/") ? blobPath.substring(1) : blobPath;

        // Get the first CORS origin (comma-separated list)
        String baseOrigin = corsAllowedOrigins;
        if (corsAllowedOrigins != null && corsAllowedOrigins.contains(",")) {
            baseOrigin = corsAllowedOrigins.split(",")[0].trim();
        }

        // Ensure base origin doesn't end with slash
        if (baseOrigin != null && baseOrigin.endsWith("/")) {
            baseOrigin = baseOrigin.substring(0, baseOrigin.length() - 1);
        }

        // Build download URL: {origin}/ocms/v1/download/blob/{blobPath}?container=ocms
        return baseOrigin + "/ocms/v1/download/blob/" + cleanPath + "?container=ocms";
    }

    /**
     * Parse container name and blob path from full path
     * Format: ocms/offence/sftp/toppan/input/ -> container: ocms, path: offence/sftp/toppan/input
     * @param fullPath Full path including container name
     * @return PathInfo containing container name and blob path
     */
    private PathInfo parseContainerAndPath(String fullPath) {
        // Remove leading slash if present
        String cleanPath = fullPath.startsWith("/") ? fullPath.substring(1) : fullPath;
        // Remove trailing slash if present
        if (cleanPath.endsWith("/")) {
            cleanPath = cleanPath.substring(0, cleanPath.length() - 1);
        }

        // Split by first slash to get container and remaining path
        // Format: ocms/offence/sftp/toppan/input
        String[] parts = cleanPath.split("/", 2);

        if (parts.length >= 2) {
            // parts[0] = "ocms" (container name)
            // parts[1] = "offence/sftp/toppan/input" (blob path)
            String containerName = parts[0];
            String blobPath = parts[1];

            return new PathInfo(containerName, blobPath);
        } else if (parts.length == 1) {
            // Only container name provided
            log.warn("Only container name provided in path: {}, using empty blob path", fullPath);
            return new PathInfo(parts[0], "");
        }

        // Fallback: use entire path as blob path and empty container
        log.warn("Could not parse container from path: {}, using full path as blob path", fullPath);
        return new PathInfo("", cleanPath);
    }

    /**
     * Helper class to hold container name and blob path
     */
    private static class PathInfo {
        final String containerName;
        final String blobPath;

        PathInfo(String containerName, String blobPath) {
            this.containerName = containerName;
            this.blobPath = blobPath;
        }
    }
}

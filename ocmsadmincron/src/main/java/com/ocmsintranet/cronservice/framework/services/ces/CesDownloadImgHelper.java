package com.ocmsintranet.cronservice.framework.services.ces;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class CesDownloadImgHelper {

    //public static List<ImageFile> extractImages(byte[] fileContent, String fileName) {
    //        List<ImageFile> images = new ArrayList<>();
    public static CesDownloadZipHelper.ExtractionResult extractImages(byte[] fileContent, String fileName, String acceptableFormats) {
        List<CesDownloadAttachmentServiceImpl.ImageFile> images = new ArrayList<>();
        int rejectedCount = 0;

        // Parse acceptable formats into a list
        String[] formats = acceptableFormats.split(",");

        if (isAcceptableFormat(fileName, formats)) {

            // Parse informasi dari nama file
            String[] parts = fileName.split("_");
            if (parts.length >= 6) {
                String nopoNumber = parts[2]; // NOPO NUMBER

                CesDownloadAttachmentServiceImpl.ImageFile image = new ImageFileImpl();
                //image.setFileName(fileName);
                String justFileName = fileName;
                if (fileName.contains("/")) {
                    justFileName = fileName.substring(fileName.lastIndexOf('/') + 1);
                }
                image.setFileName(justFileName);
                image.setFileContent(fileContent);
                image.setMimeType(getMimeType(fileName));
                image.setNopoNumber(nopoNumber);

                images.add(image);
                log.info("Image accepted: {} - Parts: {}, NOPO Number: {}", justFileName, parts.length, nopoNumber);
            } else {
                // Log rejected file due to insufficient filename parts
                log.warn("Image rejected (insufficient filename parts): {} - Parts found: {}, Required: >= 6. Expected format: PART1_PART2_NOPO_PART4_PART5_PART6.jpg", fileName, parts.length);
                rejectedCount = 1;
            }
        } else {
            // Log rejected file that doesn't match acceptable format
            log.warn("File rejected (not JPEG format): {} - Acceptable formats: {}", fileName, acceptableFormats);
            rejectedCount = 1;
        }
        return new CesDownloadZipHelper.ExtractionResult(images, rejectedCount);
    }
    
    /**
     * Check if file name matches any of the acceptable formats
     */
    private static boolean isAcceptableFormat(String fileName, String[] formats) {
        String lowerCaseFileName = fileName.toLowerCase();
        for (String format : formats) {
            if (lowerCaseFileName.endsWith(format.trim().toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get MIME type based on file extension
     * Only handles JPEG files as per CES requirements
     */
    private static String getMimeType(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "application/octet-stream";
        }

        String lowerCaseFileName = fileName.toLowerCase();

        if (lowerCaseFileName.endsWith(".jpg") || lowerCaseFileName.endsWith(".jpeg")) {
            return "image/jpeg";
        }

        return "application/octet-stream";
    }

    // Implementasi interface ImageFile
    public static class ImageFileImpl implements CesDownloadAttachmentServiceImpl.ImageFile {
        private String fileName;
        private byte[] fileContent;
        private String mimeType;
        private String nopoNumber;

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public byte[] getFileContent() { return fileContent; }
        public void setFileContent(byte[] fileContent) { this.fileContent = fileContent; }
        public String getMimeType() { return mimeType; }
        public void setMimeType(String mimeType) { this.mimeType = mimeType; }
        public String getNopoNumber() { return nopoNumber; }
        public void setNopoNumber(String nopoNumber) { this.nopoNumber = nopoNumber; }
    }
}

package com.ocmsintranet.cronservice.framework.services.ces;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.ocmsintranet.cronservice.crud.ocmsizdb.BatchJobs.OcmsBatchJob;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeRepository;
import com.ocmsintranet.cronservice.framework.dto.ces.CesDownloadAttachmentLogDto;
import com.ocmsintranet.cronservice.crud.ocmsizdb.ocmsoffenceattachment.OcmsOffenceAttachment;
import com.ocmsintranet.cronservice.crud.ocmsizdb.ocmsoffenceattachment.OcmsOffenceAttachmentRepository;
import com.ocmsintranet.cronservice.framework.helper.BatchJobHelper;
import com.ocmsintranet.cronservice.utilities.ces.FileUploadUtil;
import com.ocmsintranet.cronservice.utilities.sftpComponent.SftpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CesDownloadAttachmentServiceImpl implements CesDownloadAttachmentService {

    private final SftpUtil sftpUtil;
    private final FileUploadUtil fileUploadUtil;
    private final OcmsOffenceAttachmentRepository attachmentRepository;
    private final OcmsValidOffenceNoticeRepository validOffenceNoticeRepository;
    private final BatchJobHelper batchJobHelper;

    @Value("${ces.download.server-name}")
    private String serverName;

    @Value("${ces.download.remote-file-path}")
    private String remoteFilePath;

    @Value("${blob.folder.ces.attachment}")
    private String attachmentPath;

    @Value("${cron.ces.upload.shedlock.name}")
    private String ces_generate;

    @Value("${cron.ces.attachment.shedlock.name}")
    private String ces_download;

    @Value("${ces.attachment.acceptable-formats}")
    private String acceptableFormats;

    @Override
    public OcmsBatchJob executeCesDownloadAttachmentFunction() {
        log.info("Starting CES_DOWNLOAD_ATTACHMENT processing workflow");

        // Step 1: Create and persist initial batch job
        OcmsBatchJob batchJob = batchJobHelper.createInitialBatchJob(ces_download);
        batchJobHelper.persistBatchJob(batchJob);

        CesDownloadAttachmentLogDto logDto = CesDownloadAttachmentLogDto.builder().build();

        int totalImagesUploaded = 0;
        int totalImagesFailed = 0;
        List<String> processedFiles = new ArrayList<>();

        try {
            if (sftpUtil.isDirectory(serverName, remoteFilePath)) {
                log.info("The path is a directory, not a file: {}", remoteFilePath);

                // Step 2: List all files in the directory
                List<String> files = sftpUtil.listFiles(serverName, remoteFilePath);
                logDto.setZipFileProcess(files.size());

                if (files.isEmpty()) {
                    log.warn("No files found in the directory: {}", remoteFilePath);
                    logDto.setImageUploaded(0);
                    logDto.setImageNotMatchValidation(0);
                    logDto.calculateImageCountProcess();
                    logDto.setOverallStatus(CesDownloadAttachmentLogDto.Status.SUCCESS);
                    batchJob = batchJobHelper.updateBatchJobSuccessCesRep(batchJob, logDto.toLogText());
                    return batchJob;
                }

                log.info("Found {} files to process in directory: {}", files.size(), remoteFilePath);

                // Step 3: Process each file in the directory
                for (String fileName : files) {
                    String filePath = remoteFilePath + "/" + fileName;
                    log.info("Processing file: {}", filePath);

                    try {
                        // Process single file with transaction
                        ProcessingResult result = processFileWithTransaction(fileName, filePath);

                        totalImagesUploaded += result.getSuccessCount();
                        totalImagesFailed += result.getFailedCount();

                        if (result.isFileProcessedSuccessfully()) {
                            processedFiles.add(filePath);
                        }

                    } catch (Exception e) {
                        log.error("Error processing file {}: {}", filePath, e.getMessage(), e);
                        totalImagesFailed++;
                        // Continue to next file instead of failing entire batch
                    }
                }

                // Step 4: Delete successfully processed files from SFTP
                deleteProcessedFiles(processedFiles);

                // Step 5: Build log DTO
                logDto.setImageUploaded(totalImagesUploaded);
                logDto.setImageNotMatchValidation(totalImagesFailed);
                logDto.calculateImageCountProcess();

                log.info("Processing completed - ZipFiles: {}, ImageUploaded: {}, ImageFailed: {}",
                        files.size(), totalImagesUploaded, totalImagesFailed);

                // If any image failed validation, set status as FAILED
                if (totalImagesFailed > 0) {
                    logDto.setOverallStatus(CesDownloadAttachmentLogDto.Status.FAILED);
                    batchJob = batchJobHelper.updateBatchJobFailureCesRep(batchJob, logDto.toLogText());
                } else {
                    logDto.setOverallStatus(CesDownloadAttachmentLogDto.Status.SUCCESS);
                    batchJob = batchJobHelper.updateBatchJobSuccessCesRep(batchJob, logDto.toLogText());
                }

            } else {
                log.warn("Remote path is not a directory: {}", remoteFilePath);
                logDto.setOverallStatus(CesDownloadAttachmentLogDto.Status.FAILED);
                batchJob = batchJobHelper.updateBatchJobFailureCesRep(batchJob, logDto.toLogText());
            }

        } catch (JSchException | SftpException e) {
            log.error("SFTP Error processing directory {}: {}", remoteFilePath, e.getMessage(), e);
            logDto.setOverallStatus(CesDownloadAttachmentLogDto.Status.FAILED);
            batchJob = batchJobHelper.updateBatchJobFailureCesRep(batchJob, logDto.toLogText());
        } catch (Exception e) {
            log.error("Unexpected error during CES_DOWNLOAD_ATTACHMENT: {}", e.getMessage(), e);
            logDto.setOverallStatus(CesDownloadAttachmentLogDto.Status.FAILED);
            batchJob = batchJobHelper.updateBatchJobFailureCesRep(batchJob, logDto.toLogText());
        } finally {
            batchJobHelper.persistBatchJob(batchJob);
            log.info("CES_DOWNLOAD_ATTACHMENT processing finished. Job ID: {}", batchJob.getBatchJobId());
        }

        return batchJob;
    }

    @Transactional
    private ProcessingResult processFileWithTransaction(String fileName, String filePath) throws Exception {
        ProcessingResult result = new ProcessingResult();

        byte[] fileContent = sftpUtil.downloadFile(serverName, filePath);

        if (fileContent == null || fileContent.length == 0) {
            log.warn("No file content found for {}", filePath);
            return result;
        }

        // Step 1: Extract images based on file type
        CesDownloadZipHelper.ExtractionResult extractionResult;
        if (fileName.toLowerCase().endsWith(".zip")) {
            extractionResult = CesDownloadZipHelper.extractImages(fileContent, acceptableFormats);
        } else {
            extractionResult = CesDownloadImgHelper.extractImages(fileContent, filePath, acceptableFormats);
        }

        // Count rejected files as failed
        if (extractionResult.getRejectedCount() > 0) {
            result.incrementFailed(extractionResult.getRejectedCount());
        }

        List<? extends ImageFile> imageFiles = extractionResult.getImages();
        if (imageFiles == null || imageFiles.isEmpty()) {
            log.warn("No valid JPEG images extracted from {}. File will be retained on SFTP.", filePath);
            // Do NOT mark as processed - keep file on SFTP
            return result;
        }

        log.info("Extracted {} images from {}", imageFiles.size(), filePath);

        // Step 2: Extract noticeNo
        String noticeNo = extractNoticeNoFromFileName(fileName);
        if (noticeNo == null) {
            log.warn("=== VALIDATION FAILED: NOTICE NUMBER EXTRACTION ===");
            log.warn("Cannot determine noticeNo from filename: {}", fileName);
            log.warn("Expected ZIP filename format: PART1_PART2_NOTICENO_PART4.zip (minimum 3 underscore-separated parts)");
            log.warn("File will be retained on SFTP with {} images marked as FAILED", imageFiles.size());
            // Count valid images as failed since they cannot be uploaded
            result.incrementFailed(imageFiles.size());
            // Do NOT mark as processed - keep file on SFTP
            return result;
        }

        log.info("Extracted noticeNo from ZIP filename: {} -> noticeNo: {}", fileName, noticeNo);

        // Step 3: Verify noticeNo exists in database
        if (!validOffenceNoticeRepository.existsById(noticeNo)) {
            log.warn("=== VALIDATION FAILED: NOTICE NUMBER NOT IN DATABASE ===");
            log.warn("Notice number '{}' does not exist in ocms_valid_offence_notice table", noticeNo);
            log.warn("ZIP file: {}", fileName);
            log.warn("All {} images from this ZIP will be marked as FAILED", imageFiles.size());
            log.warn("File will be retained on SFTP for manual review");
            // Count valid images as failed since they cannot be uploaded
            result.incrementFailed(imageFiles.size());
            // Do NOT mark as processed - keep file on SFTP
            return result;
        }

        log.info("Notice number '{}' verified in database. Proceeding with image upload.", noticeNo);

        String pathFolderBlob = attachmentPath.replaceAll("/+$", "") + "/" + noticeNo + "/";

        // Step 4: Process and collect attachments for batch save
        List<OcmsOffenceAttachment> attachmentsToSave = new ArrayList<>();

        for (ImageFile image : imageFiles) {
            try {
                String fullFileName = "OFFENCE_PHOTO_" + image.getFileName();
                log.info("Uploading image to Azure Blob Storage: {}", fullFileName);

                // Check for duplicates before upload
                if (attachmentRepository.existsByNoticeNoAndFileName(noticeNo, fullFileName)) {
                    log.warn("Duplicate attachment found for noticeNo: {} and fileName: {}. Skipping.", noticeNo, fullFileName);
                    result.incrementFailed();
                    continue;
                }

                FileUploadUtil.BlobUploadResult uploadResult = fileUploadUtil.uploadToBlob(
                        image.getFileContent(),
                        fullFileName,
                        pathFolderBlob,
                        "CESDownloadAttachment");

                if (uploadResult.isSuccess()) {
                    OcmsOffenceAttachment attachment = new OcmsOffenceAttachment();
                    attachment.setNoticeNo(noticeNo);
                    attachment.setFileName(fullFileName);
                    attachment.setMime(image.getMimeType());
                    attachment.setSize((long) image.getFileContent().length);
                    attachmentsToSave.add(attachment);
                    result.incrementSuccess();
                    log.info("Successfully uploaded {} to blob storage", fullFileName);
                } else {
                    log.error("Failed to upload {} to blob storage", fullFileName);
                    result.incrementFailed();
                }
            } catch (Exception e) {
                log.error("Error processing image {}: {}", image.getFileName(), e.getMessage(), e);
                result.incrementFailed();
                // Continue processing other images
            }
        }

        // Step 5: Batch save all attachments at once
        if (!attachmentsToSave.isEmpty()) {
            try {
                List<OcmsOffenceAttachment> savedAttachments = attachmentRepository.saveAll(attachmentsToSave);
                log.info("Successfully saved {} attachments to database for file: {}", savedAttachments.size(), fileName);

                // Only mark as processed if ALL images were successfully uploaded (no failures)
                if (result.getFailedCount() == 0 && result.getSuccessCount() > 0) {
                    result.setFileProcessedSuccessfully(true);
                    log.info("All {} images from file {} successfully uploaded. File will be deleted from SFTP.", result.getSuccessCount(), fileName);
                } else {
                    log.warn("File {} has {} failed images out of {}. File will be retained on SFTP.",
                            fileName, result.getFailedCount(), imageFiles.size());
                }
            } catch (Exception e) {
                log.error("Failed to batch save attachments for file {}: {}", fileName, e.getMessage(), e);
                throw e; // Rollback transaction
            }
        } else {
            // No attachments to save - all images failed (duplicates, upload failures, or errors)
            log.warn("No attachments saved for file {}. File will be retained on SFTP.", fileName);
        }

        return result;
    }

    private void deleteProcessedFiles(List<String> filePaths) {
        for (String filePath : filePaths) {
            try {
                sftpUtil.deleteFile(serverName, filePath);
                log.info("Successfully deleted file from SFTP: {}", filePath);
            } catch (Exception e) {
                log.warn("Failed to delete file from SFTP: {}. Manual cleanup may be required.", filePath, e);
            }
        }
    }

    private static class ProcessingResult {
        private int successCount = 0;
        private int failedCount = 0;
        private boolean fileProcessedSuccessfully = false;

        public void incrementSuccess() {
            successCount++;
        }

        public void incrementFailed() {
            failedCount++;
        }

        public void incrementFailed(int count) {
            failedCount += count;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailedCount() {
            return failedCount;
        }

        public boolean isFileProcessedSuccessfully() {
            return fileProcessedSuccessfully;
        }

        public void setFileProcessedSuccessfully(boolean fileProcessedSuccessfully) {
            this.fileProcessedSuccessfully = fileProcessedSuccessfully;
        }
    }


    private String extractNoticeNoFromFileName(String fileName) {
        if (fileName == null) return null;


        String baseName = fileName.replaceAll("(?i)\\.zip$", "");

        String[] parts = baseName.split("_");


        if (parts.length < 3) {
            log.warn("Filename format invalid (expected at least 3 underscore-separated parts): {}", fileName);
            return null;
        }

        return parts[2];
    }


    public interface ImageFile {
        String getFileName();

        void setFileName(String fileName);

        byte[] getFileContent();

        void setFileContent(byte[] fileContent);

        String getMimeType();

        void setMimeType(String mimeType);

        String getNopoNumber();

        void setNopoNumber(String nopoNumber);
    }
}


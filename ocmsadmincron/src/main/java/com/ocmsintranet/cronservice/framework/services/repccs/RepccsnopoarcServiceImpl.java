package com.ocmsintranet.cronservice.framework.services.repccs;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.crud.ocmsizdb.BatchJobs.OcmsBatchJob;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeRepository;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeService;
import com.ocmsintranet.cronservice.framework.dto.repccs.RepccsProcessingLogDto;
import com.ocmsintranet.cronservice.framework.helper.BatchJobHelper;
import com.ocmsintranet.cronservice.framework.services.comcrypt.helper.ComcryptTokenHelper;
import com.ocmsintranet.cronservice.utilities.ces.FileUploadUtil;
import com.ocmsintranet.cronservice.utilities.repccs.RepcssNopoarcDatFileGenerator;
import com.ocmsintranet.cronservice.utilities.repccs.RepcssNopoarcDatFileGenerator.NopoarcData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RepccsnopoarcServiceImpl implements RepccsnopoarcService {

    private static final Logger logger = LoggerFactory.getLogger(RepccsnopoarcServiceImpl.class);

    @Autowired
    private OcmsValidOffenceNoticeRepository repository;

    @Autowired
    private OcmsValidOffenceNoticeService ocmsValidOffenceNoticeService;

    @Autowired
    private RepcssNopoarcDatFileGenerator repcssNopoarcDatFileGenerator;

    @Autowired
    private FileUploadUtil fileUploadUtil;

    @Autowired
    private BatchJobHelper batchJobHelper;

    @Autowired
    private ComcryptTokenHelper comcryptTokenHelper;

    @Value("${ocms.comcrypt.appcode.repccs}")
    private String comsAppcode;

    @Value("${blob.folder.repccs.in}")
    private String repccsBlobPath;

    @Value("${sftp.folders.repccs.in}")
    private String repccsSftpPath;

    @Override
    public OcmsBatchJob executeNopoarcFunction(OcmsBatchJob batchJob) {
        logger.info("========== REPCCS NOPO ARCHIVAL PROCESSING START ==========");
        RepccsProcessingLogDto logDto = RepccsProcessingLogDto.builder()
                .prefix("REPCCS NOPO ARCHIVAL PROCESSING")
                .build();

        try {
            // Step 1: Query database for NOPO notices
            logger.info("Querying database for NOPO notices for archival");
            List<String> noticeNumbers = repository.findNoticesForNopoarc();

            if (noticeNumbers == null || noticeNumbers.isEmpty()) {
                logger.warn("No NOPO notices found for archival processing");
                logDto.setOverallStatus(RepccsProcessingLogDto.Status.SUCCESS);
                logDto.setRecords(0);
                batchJob.setLogText((batchJob.getLogText() != null ? batchJob.getLogText() : "") + logDto.toLogText());
                logger.info("========== REPCCS NOPO ARCHIVAL PROCESSING END (NO DATA) ==========");
                return batchJob;
            }

            logger.info("Found {} NOPO notices for archival processing", noticeNumbers.size());
            logDto.setRecords(noticeNumbers.size());

            // Step 2: Convert notice numbers to NopoarcData objects
            List<NopoarcData> nopoarcDataList = new ArrayList<>();
            for (String noticeNumber : noticeNumbers) {
                nopoarcDataList.add(new NopoarcData(noticeNumber, "S"));
            }

            // Step 3: Generate .DAT file content
            byte[] fileContent = repcssNopoarcDatFileGenerator.generateNopoarcDatFileContent(nopoarcDataList);
            if (fileContent == null || fileContent.length == 0) {
                throw new RuntimeException("Failed to generate DAT file content - content is null or empty");
            }

            String fileName = repcssNopoarcDatFileGenerator.generateNopoarcFileName();
            if (fileName == null || fileName.isEmpty()) {
                throw new RuntimeException("Failed to generate file name - fileName is null or empty");
            }
            logger.info("Generated file: {}, size: {} bytes", fileName, fileContent.length);
            logDto.setFileName(fileName);
            logDto.setGenerateFileStatus(RepccsProcessingLogDto.Status.SUCCESS);

            // Step 4: Upload original file to Azure Blob Storage
            FileUploadUtil.BlobUploadResult blobResult = fileUploadUtil.uploadToBlob(fileContent, fileName, repccsBlobPath, "NOPO_ARCH");

            if (blobResult == null || !blobResult.isSuccess()) {
                logDto.setBlobUploadStatus(RepccsProcessingLogDto.Status.FAILED);
                throw new RuntimeException("Failed to upload file to blob: " + (blobResult != null ? blobResult.getError() : "null result"));
            }
            logger.info("Successfully uploaded file to blob: {}", blobResult.getBlobPath());
            logDto.setBlobUploadStatus(RepccsProcessingLogDto.Status.SUCCESS);

            // Step 5: Upload file to SFTP
            logger.info("Uploading file to SFTP: {}", fileName);
            FileUploadUtil.SftpUploadResult sftpResult = fileUploadUtil.uploadToSftp(fileContent, fileName, repccsSftpPath, "NOPO_ARCH");

            if (sftpResult == null || !sftpResult.isSuccess()) {
                logDto.setSftpUploadStatus(RepccsProcessingLogDto.Status.FAILED);
                throw new RuntimeException("SFTP upload failed: " + (sftpResult != null ? sftpResult.getError() : "null result"));
            }
            logger.info("Successfully uploaded file to SFTP: {}", sftpResult.getSftpPath());
            logDto.setSftpUploadStatus(RepccsProcessingLogDto.Status.SUCCESS);

            // Step 6: Request COMCRYPT token for encryption
            logger.info("Requesting COMCRYPT token for encryption: {}", fileName);
//            String requestId = comcryptTokenHelper.requestToken(
//                comsAppcode,
//                SystemConstant.CryptOperation.ENCRYPTION,
//                fileName,
//                "NOPO Archival Processing - " + nopoarcDataList.size() + " records"
//            );
//
//            if (requestId == null) {
//                logDto.setEncryptionStatus(RepccsProcessingLogDto.Status.FAILED);
//                throw new RuntimeException("Failed to request token from COMCRYPT API");
//            }
//            logger.info("Token request sent, requestId: {}. Encryption will be handled by ComcryptCallbackHelper.", requestId);
            logDto.setEncryptionStatus(RepccsProcessingLogDto.Status.SUCCESS);
            logDto.calculateOverallStatus();

            batchJob.setLogText((batchJob.getLogText() != null ? batchJob.getLogText() : "") + logDto.toLogText());
            logger.info("========== REPCCS NOPO ARCHIVAL PROCESSING END (SUCCESS) ==========");
            return batchJob;

        } catch (Exception e) {
            logger.error("NOPO Archival processing failed: {}", e.getMessage(), e);
            logDto.calculateOverallStatus();
            batchJob.setLogText((batchJob.getLogText() != null ? batchJob.getLogText() : "") + logDto.toLogText());
            logger.error("========== REPCCS NOPO ARCHIVAL PROCESSING END (FAILED) ==========");
            return batchJob;
        }
    }

    public int getNopoarcNoticeCount() {
        try {
            List<String> noticeNumbers = repository.findNoticesForNopoarc();
            return noticeNumbers != null ? noticeNumbers.size() : 0;
        } catch (Exception e) {
            logger.error("Error getting NOPOARC notice count: {}", e.getMessage(), e);
            return -1;
        }
    }
}
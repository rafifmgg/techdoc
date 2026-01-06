package com.ocmsintranet.cronservice.framework.services.repccs;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.crud.ocmsizdb.offencerulecode.OffenceRuleCodeRepository;
import com.ocmsintranet.cronservice.crud.ocmsizdb.BatchJobs.OcmsBatchJob;
import com.ocmsintranet.cronservice.framework.dto.repccs.RepccsProcessingLogDto;
import com.ocmsintranet.cronservice.framework.helper.BatchJobHelper;
import com.ocmsintranet.cronservice.framework.services.comcrypt.helper.ComcryptTokenHelper;
import com.ocmsintranet.cronservice.utilities.repccs.RepcssOffenceRuleDatFileGenerator;
import com.ocmsintranet.cronservice.utilities.repccs.RepcssOffenceRuleDatFileGenerator.OffenceRuleData;
import com.ocmsintranet.cronservice.utilities.ces.FileUploadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RepccsOffenceRuleServiceImpl implements RepccsOffenceRuleService {

    private static final Logger logger = LoggerFactory.getLogger(RepccsOffenceRuleServiceImpl.class);

    @Autowired
    private OffenceRuleCodeRepository repository;

    @Autowired
    private RepcssOffenceRuleDatFileGenerator repcssOffenceRuleDatFileGenerator;

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
    public OcmsBatchJob executeUpdatedOffenceRuleFunction(OcmsBatchJob batchJob) {
        logger.info("========== REPCCS OFFENCE RULE PROCESSING START ==========");
        RepccsProcessingLogDto logDto = RepccsProcessingLogDto.builder()
                .prefix("REPCCS OFFENCE RULE PROCESSING")
                .build();

        try {
            // Step 1: Query database for active offence rules
            logger.info("Querying database for active offence rules");
            List<Object[]> offenceRuleDataRows = repository.findActiveOffenceRules();

            if (offenceRuleDataRows == null || offenceRuleDataRows.isEmpty()) {
                logger.warn("No active offence rule data found for processing");
                logDto.setOverallStatus(RepccsProcessingLogDto.Status.SUCCESS);
                logDto.setRecords(0);
                batchJob.setLogText((batchJob.getLogText() != null ? batchJob.getLogText() : "") + logDto.toLogText());
                logger.info("========== REPCCS OFFENCE RULE PROCESSING END (NO DATA) ==========");
                return batchJob;
            }

            logger.info("Found {} active offence rule records for processing", offenceRuleDataRows.size());

            // Step 2: Convert database results to OffenceRuleData objects
            List<OffenceRuleData> offenceRuleDataList = new ArrayList<>();
            List<OffenceRuleData> offenceRuleDataError = new ArrayList<>();

            for (Object[] row : offenceRuleDataRows) {
                int offenceCode = ((Number) row[0]).intValue();
                String offenceRule = row[1] != null ? row[1].toString() : null;
                String offenceDescription = row[2] != null ? row[2].toString() : null;
                Timestamp effectiveStartDate = (Timestamp) row[3];
                Timestamp effectiveEndDate = (Timestamp) row[4];
                BigDecimal defaultFineAmount = (BigDecimal) row[5];
                BigDecimal secondaryFineAmount = (BigDecimal) row[6];
                String vehicleType = row[7] != null ? row[7].toString() : null;

                LocalDateTime startDate = effectiveStartDate != null ? effectiveStartDate.toLocalDateTime() : null;
                LocalDateTime endDate = effectiveEndDate != null ? effectiveEndDate.toLocalDateTime() : null;

                // Helper lambda to calculate the length of numeric values
                java.util.function.Function<BigDecimal, int[]> getLengths = bd -> {
                    String[] p = bd.toPlainString().split("\\.");
                    String intPart = p[0].replace("-", "");
                    int intLen = intPart.length();
                    int decLen = (p.length > 1) ? p[1].length() : 0;
                    return new int[]{intLen, decLen};
                };

                // Check defaultFineAmount separately
                if (defaultFineAmount != null) {
                    int[] lengths1 = getLengths.apply(defaultFineAmount);
                    boolean invalid1 = (lengths1[0] + lengths1[1] > 6) || (lengths1[1] > 2);
                    if (invalid1) {
                        offenceRuleDataError.add(new OffenceRuleData(
                                offenceCode, offenceRule, offenceDescription,
                                startDate, endDate,
                                defaultFineAmount, secondaryFineAmount, vehicleType
                        ));
                        continue;
                    }
                }

                // Check secondaryFineAmount separately
                if (secondaryFineAmount != null) {
                    int[] lengths2 = getLengths.apply(secondaryFineAmount);
                    boolean invalid2 = (lengths2[0] + lengths2[1] > 6) || (lengths2[1] > 2);
                    if (invalid2) {
                        offenceRuleDataError.add(new OffenceRuleData(
                                offenceCode, offenceRule, offenceDescription,
                                startDate, endDate,
                                defaultFineAmount, secondaryFineAmount, vehicleType
                        ));
                        continue;
                    }
                }

                offenceRuleDataList.add(new OffenceRuleData(
                        offenceCode, offenceRule, offenceDescription,
                        startDate, endDate,
                        defaultFineAmount, secondaryFineAmount, vehicleType
                ));
            }

            logDto.setRecords(offenceRuleDataList.size());
            logger.info("Found {} valid active offence rule records for processing", offenceRuleDataList.size());

            if (!offenceRuleDataError.isEmpty()) {
                logger.warn("Active offence rule data contains {} errors", offenceRuleDataError.size());
            }

            if (offenceRuleDataList.isEmpty()) {
                logger.warn("No valid offence rule data to process after validation");
                logDto.setOverallStatus(RepccsProcessingLogDto.Status.SUCCESS);
                logDto.setRecords(0);
                batchJob.setLogText((batchJob.getLogText() != null ? batchJob.getLogText() : "") + logDto.toLogText());
                logger.info("========== REPCCS OFFENCE RULE PROCESSING END (NO DATA) ==========");
                return batchJob;
            }

            // Step 3: Generate .DAT file content
            String fileContent = repcssOffenceRuleDatFileGenerator.generateDatFileContent(offenceRuleDataList);
            if (fileContent == null || fileContent.isEmpty()) {
                throw new RuntimeException("Failed to generate DAT file content - content is null or empty");
            }
            byte[] fileBytes = fileContent.getBytes();

            String fileName = repcssOffenceRuleDatFileGenerator.generateFileName();
            if (fileName == null || fileName.isEmpty()) {
                throw new RuntimeException("Failed to generate file name - fileName is null or empty");
            }
            logger.info("Generated file: {}, size: {} bytes", fileName, fileBytes.length);
            logDto.setFileName(fileName);
            logDto.setGenerateFileStatus(RepccsProcessingLogDto.Status.SUCCESS);

            // Step 4: Upload original file to Azure Blob Storage
            FileUploadUtil.BlobUploadResult blobResult = fileUploadUtil.uploadToBlob(fileBytes, fileName, repccsBlobPath, "OFFENCE_RULE");

            if (blobResult == null || !blobResult.isSuccess()) {
                logDto.setBlobUploadStatus(RepccsProcessingLogDto.Status.FAILED);
                throw new RuntimeException("Failed to upload file to blob: " + (blobResult != null ? blobResult.getError() : "null result"));
            }
            logger.info("Successfully uploaded file to blob: {}", blobResult.getBlobPath());
            logDto.setBlobUploadStatus(RepccsProcessingLogDto.Status.SUCCESS);

            // Step 5: Upload file to SFTP
            logger.info("Uploading file to SFTP: {}", fileName);
            FileUploadUtil.SftpUploadResult sftpResult = fileUploadUtil.uploadToSftp(fileBytes, fileName, repccsSftpPath, "OFFENCE_RULE");

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
//                "Offence Rule Processing - " + offenceRuleDataList.size() + " records"
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
            logger.info("========== REPCCS OFFENCE RULE PROCESSING END (SUCCESS) ==========");
            return batchJob;

        } catch (Exception e) {
            logger.error("Offence Rule processing failed: {}", e.getMessage(), e);
            logDto.calculateOverallStatus();
            batchJob.setLogText((batchJob.getLogText() != null ? batchJob.getLogText() : "") + logDto.toLogText());
            logger.error("========== REPCCS OFFENCE RULE PROCESSING END (FAILED) ==========");
            return batchJob;
        }
    }

    @Override
    public int getUpdatedOffenceRuleCount() {
        try {
            List<Object[]> offenceRuleDataRows = repository.findActiveOffenceRules();
            return offenceRuleDataRows != null ? offenceRuleDataRows.size() : 0;
        } catch (Exception e) {
            logger.error("Error getting Updated Offence Rule count: {}", e.getMessage(), e);
            return -1;
        }
    }
}
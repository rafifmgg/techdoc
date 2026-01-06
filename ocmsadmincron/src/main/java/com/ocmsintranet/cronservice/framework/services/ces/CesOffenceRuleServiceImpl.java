package com.ocmsintranet.cronservice.framework.services.ces;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.crud.ocmsizdb.BatchJobs.OcmsBatchJob;
import com.ocmsintranet.cronservice.crud.ocmsizdb.offencerulecode.OffenceRuleCodeRepository;
import com.ocmsintranet.cronservice.framework.dto.ces.CesOffenceRuleData;
import com.ocmsintranet.cronservice.framework.dto.ces.CesProcessingLogDto;
import com.ocmsintranet.cronservice.framework.helper.UploadHelper;
import com.ocmsintranet.cronservice.framework.services.comcrypt.helper.ComcryptTokenHelper;
import com.ocmsintranet.cronservice.utilities.ces.CesOffenceRuleDatGenerator;
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
public class CesOffenceRuleServiceImpl implements CesOffenceRuleService {

    private static final Logger logger = LoggerFactory.getLogger(CesOffenceRuleService.class);

    @Autowired
    private OffenceRuleCodeRepository repository;

    @Autowired
    private CesOffenceRuleDatGenerator cesOffenceRuleDatGenerator;

    @Autowired
    private FileUploadUtil fileUploadUtil;

    @Value("${blob.folder.ces.in}")
    private String cesBlobPath;

    @Value("${ocms.comcrypt.appcode.ces.encrypt}")
    private String comsAppcode;

    @Autowired
    private ComcryptTokenHelper comcryptTokenHelper;

    @Autowired
    private UploadHelper uploadHelper;

    @Value("${sftp.folders.ces.in}")
    private String cesSftpPath;


    @Override
    public OcmsBatchJob executeCesOffenceRuleFunction(OcmsBatchJob batchJob) {
        logger.info("========== OFFENCE RULE PROCESSING START ==========");
        CesProcessingLogDto logDto = CesProcessingLogDto.builder()
                .prefix("OFFENCE RULE PROCESSING")
                .build();

        try {
            // Query active offence rules from database
            logger.info("Querying database for active offence rules");
            List<Object[]> offenceRuleDataRows = repository.findActiveOffenceRules();

            if (offenceRuleDataRows == null || offenceRuleDataRows.isEmpty()) {
                logger.warn("No active offence rule data found");
                logDto.setOverallStatus(CesProcessingLogDto.Status.SUCCESS);
                logDto.setRecords(0);
                batchJob.setLogText((batchJob.getLogText() != null ? batchJob.getLogText() : "") + logDto.toLogText());
                logger.info("========== OFFENCE RULE PROCESSING END (NO DATA) ==========");
                return batchJob;
            }

            logger.info("Found {} active offence rule records", offenceRuleDataRows.size());

            // Convert Object[] to OffenceRuleData objects with validation
            logger.info("Converting and validating offence rule data");
            List<CesOffenceRuleData> offenceRuleDataList = new ArrayList<>();
            List<CesOffenceRuleData> offenceRuleDataError = new ArrayList<>();

            for (Object[] row : offenceRuleDataRows) {
                String offenceCode = row[0] != null ? row[0].toString() : null;
                String offenceRule = row[1] != null ? row[1].toString() : null;
                String offenceDescription = row[2] != null ? row[2].toString() : null;
                Timestamp effectiveStartDate = (Timestamp) row[3];
                Timestamp effectiveEndDate = (Timestamp) row[4];
                BigDecimal defaultFineAmount = (BigDecimal) row[5];
                BigDecimal secondaryFineAmount = (BigDecimal) row[6];
                String vehicleType = row[7] != null ? row[7].toString() : null;

                LocalDateTime startDate = effectiveStartDate != null ? effectiveStartDate.toLocalDateTime() : null;
                LocalDateTime endDate = effectiveEndDate != null ? effectiveEndDate.toLocalDateTime() : null;

                // Helper lambda for validating fine amounts
                java.util.function.Function<BigDecimal, int[]> getLengths = bd -> {
                    String[] p = bd.toPlainString().split("\\.");
                    String intPart = p[0].replace("-", "");
                    int intLen = intPart.length();
                    int decLen = (p.length > 1) ? p[1].length() : 0;
                    return new int[]{intLen, decLen};
                };

                // Validate defaultFineAmount
                if (defaultFineAmount != null) {
                    int[] lengths1 = getLengths.apply(defaultFineAmount);
                    boolean invalid1 = (lengths1[0] + lengths1[1] > 6) || (lengths1[1] > 2);

                    if (invalid1) {
                        offenceRuleDataError.add(new CesOffenceRuleData(
                                offenceCode, offenceRule, offenceDescription,
                                startDate, endDate,
                                defaultFineAmount, secondaryFineAmount, vehicleType
                        ));
                        continue;
                    }
                }

                // Validate secondaryFineAmount
                if (secondaryFineAmount != null) {
                    int[] lengths2 = getLengths.apply(secondaryFineAmount);
                    boolean invalid2 = (lengths2[0] + lengths2[1] > 6) || (lengths2[1] > 2);

                    if (invalid2) {
                        offenceRuleDataError.add(new CesOffenceRuleData(
                                offenceCode, offenceRule, offenceDescription,
                                startDate, endDate,
                                defaultFineAmount, secondaryFineAmount, vehicleType
                        ));
                        continue;
                    }
                }

                // Add valid record
                offenceRuleDataList.add(new CesOffenceRuleData(
                        offenceCode, offenceRule, offenceDescription,
                        startDate, endDate,
                        defaultFineAmount, secondaryFineAmount, vehicleType
                ));
            }

            // Check if there are validation errors
            if (!offenceRuleDataError.isEmpty()) {
                logger.error("Active offence rule data contains validation errors");
                StringBuilder errorMessageBuilder = new StringBuilder();
                for (CesOffenceRuleData errorData : offenceRuleDataError) {
                    errorMessageBuilder.append(String.format(
                            "Offence Code: %s, OffenceRule: %s, DefaultFineAmount: %s; ",
                            errorData.getOffenceCode(),
                            errorData.getOffenceRule(),
                            errorData.getDefaultFineAmount()
                    ));
                }
                throw new RuntimeException(String.format(
                        "Failed to process %d active offence rule records due to validation errors. Details: %s",
                        offenceRuleDataError.size(), errorMessageBuilder
                ));
            }

            logger.info("Successfully validated {} offence rule records", offenceRuleDataList.size());
            logDto.setRecords(offenceRuleDataList.size());

            // Generate .dat file content
            String fileContent = cesOffenceRuleDatGenerator.generateDatFileContent(offenceRuleDataList);
            if (fileContent == null || fileContent.isEmpty()) {
                throw new RuntimeException("Failed to generate DAT file content - content is null or empty");
            }
            byte[] fileBytes = fileContent.getBytes();

            String fileName = cesOffenceRuleDatGenerator.generateFileName();
            if (fileName == null || fileName.isEmpty()) {
                throw new RuntimeException("Failed to generate file name - fileName is null or empty");
            }
            logger.info("Generated file: {}, size: {} bytes", fileName, fileBytes.length);
            logDto.setFileName(fileName);
            logDto.setGenerateFileStatus(CesProcessingLogDto.Status.SUCCESS);

            // Step 1: Upload to Azure Blob Storage
            FileUploadUtil.BlobUploadResult blobResult = fileUploadUtil.uploadToBlob(fileBytes, fileName, cesBlobPath, "OFFENCE_RULE");

            if (blobResult == null || !blobResult.isSuccess()) {
                logDto.setBlobUploadStatus(CesProcessingLogDto.Status.FAILED);
                throw new RuntimeException("Failed to upload file to blob: " + (blobResult != null ? blobResult.getError() : "null result"));
            }
            logger.info("Successfully uploaded file to blob: {}", blobResult.getBlobPath());
            logDto.setBlobUploadStatus(CesProcessingLogDto.Status.SUCCESS);

            // Step 2: Upload NON-ENCRYPTED file to SFTP (original .DAT file)
            logger.info("Uploading non-encrypted file to SFTP: {}", fileName);
            UploadHelper.SftpUploadResult nonEncryptedSftpResult = uploadHelper.uploadToCes(fileBytes, fileName, cesSftpPath, "OFFENCE_RULE");

            if (nonEncryptedSftpResult == null || !nonEncryptedSftpResult.isSuccess()) {
                logDto.setSftpNonEncryptedStatus(CesProcessingLogDto.Status.FAILED);
                throw new RuntimeException("SFTP upload (non-encrypted) failed: " + (nonEncryptedSftpResult != null ? nonEncryptedSftpResult.getError() : "null result"));
            }
            logger.info("Successfully uploaded non-encrypted file to SFTP: {}", nonEncryptedSftpResult.getSftpPath());
            logDto.setSftpNonEncryptedStatus(CesProcessingLogDto.Status.SUCCESS);

            // Step 3: PGP Encrypt file - Request token and encrypt
            logger.info("Starting PGP encryption for file: {}", fileName);
            String requestId = comcryptTokenHelper.requestToken(
                comsAppcode,
                SystemConstant.CryptOperation.ENCRYPTION,
                fileName,
                "Offence Rule Processing - " + offenceRuleDataList.size() + " records"
            );

            if (requestId == null) {
                logDto.setSftpEncryptedStatus(CesProcessingLogDto.Status.FAILED);
                throw new RuntimeException("Failed to request token from COMCRYPT API");
            }
            logger.info("Token request sent, requestId: {}. Encryption and SFTP upload will be handled by ComcryptCallbackHelper.", requestId);
            //logDto.setSftpEncryptedStatus(CesProcessingLogDto.Status.SUCCESS);
            //logDto.calculateOverallStatus();

            batchJob.setLogText((batchJob.getLogText() != null ? batchJob.getLogText() : "") + logDto.toLogText());
            logger.info("========== OFFENCE RULE PROCESSING END (SUCCESS) ==========");
            return batchJob;

        } catch (Exception e) {
            logger.error("Offence Rule processing failed: {}", e.getMessage(), e);
            logDto.calculateOverallStatus();
            batchJob.setLogText((batchJob.getLogText() != null ? batchJob.getLogText() : "") + logDto.toLogText());
            logger.error("========== OFFENCE RULE PROCESSING END (FAILED) ==========");
            return batchJob;
        }
    }
}
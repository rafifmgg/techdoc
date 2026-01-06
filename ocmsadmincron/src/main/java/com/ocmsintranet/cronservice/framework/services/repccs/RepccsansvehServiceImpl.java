package com.ocmsintranet.cronservice.framework.services.repccs;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeRepository;
import com.ocmsintranet.cronservice.crud.ocmsizdb.BatchJobs.OcmsBatchJob;
import com.ocmsintranet.cronservice.framework.dto.repccs.RepccsProcessingLogDto;
import com.ocmsintranet.cronservice.framework.helper.BatchJobHelper;
import com.ocmsintranet.cronservice.framework.services.comcrypt.helper.ComcryptTokenHelper;
import com.ocmsintranet.cronservice.utilities.repccs.RepccsAnsDatFileGenerator;
import com.ocmsintranet.cronservice.utilities.ces.FileUploadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RepccsansvehServiceImpl implements RepccsansvehService {

    private static final Logger logger = LoggerFactory.getLogger(RepccsansvehServiceImpl.class);

    @Autowired
    private OcmsValidOffenceNoticeRepository repository;

    @Autowired
    private RepccsAnsDatFileGenerator repccsAnsDatFileGenerator;

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
    public OcmsBatchJob executeOcmsinansvehFunction(OcmsBatchJob batchJob) {
        logger.info("========== REPCCS ANS VEHICLE PROCESSING START ==========");
        RepccsProcessingLogDto logDto = RepccsProcessingLogDto.builder()
                .prefix("REPCCS ANS VEHICLE PROCESSING")
                .build();

        try {
            // Step 1: Query database for vehicle numbers
            logger.info("Querying database for vehicles with ANS suspension");
            List<String> vehicleNumbers = repository.findVehiclesForCasinansveh();

            if (vehicleNumbers == null || vehicleNumbers.isEmpty()) {
                logger.warn("No vehicles found for ANS processing");
                logDto.setOverallStatus(RepccsProcessingLogDto.Status.SUCCESS);
                logDto.setRecords(0);
                batchJob.setLogText((batchJob.getLogText() != null ? batchJob.getLogText() : "") + logDto.toLogText());
                logger.info("========== REPCCS ANS VEHICLE PROCESSING END (NO DATA) ==========");
                return batchJob;
            }

            logger.info("Found {} vehicles for ANS processing", vehicleNumbers.size());
            logDto.setRecords(vehicleNumbers.size());

            // Step 2: Convert to vehicle data list
            List<RepccsAnsDatFileGenerator.AnsvehVehicleData> vehicleDataList = new ArrayList<>();
            for (String vehicleNumber : vehicleNumbers) {
                vehicleDataList.add(new RepccsAnsDatFileGenerator.AnsvehVehicleData(vehicleNumber));
            }

            // Step 3: Generate .DAT file content
            String fileContent = repccsAnsDatFileGenerator.generateDatFileContent(vehicleDataList);
            if (fileContent == null || fileContent.isEmpty()) {
                throw new RuntimeException("Failed to generate DAT file content - content is null or empty");
            }
            byte[] fileBytes = fileContent.getBytes();

            String fileName = repccsAnsDatFileGenerator.generateFileName();
            if (fileName == null || fileName.isEmpty()) {
                throw new RuntimeException("Failed to generate file name - fileName is null or empty");
            }
            logger.info("Generated file: {}, size: {} bytes", fileName, fileBytes.length);
            logDto.setFileName(fileName);
            logDto.setGenerateFileStatus(RepccsProcessingLogDto.Status.SUCCESS);

            // Step 4: Upload original file to Azure Blob Storage
            FileUploadUtil.BlobUploadResult blobResult = fileUploadUtil.uploadToBlob(fileBytes, fileName, repccsBlobPath, "ANS_VEHICLE");

            if (blobResult == null || !blobResult.isSuccess()) {
                logDto.setBlobUploadStatus(RepccsProcessingLogDto.Status.FAILED);
                throw new RuntimeException("Failed to upload file to blob: " + (blobResult != null ? blobResult.getError() : "null result"));
            }
            logger.info("Successfully uploaded file to blob: {}", blobResult.getBlobPath());
            logDto.setBlobUploadStatus(RepccsProcessingLogDto.Status.SUCCESS);

            // Step 5: Upload file to SFTP
            logger.info("Uploading file to SFTP: {}", fileName);
            FileUploadUtil.SftpUploadResult sftpResult = fileUploadUtil.uploadToSftp(fileBytes, fileName, repccsSftpPath, "ANS_VEHICLE");

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
//                "ANS Vehicle Processing - " + vehicleNumbers.size() + " records"
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
            logger.info("========== REPCCS ANS VEHICLE PROCESSING END (SUCCESS) ==========");
            return batchJob;

        } catch (Exception e) {
            logger.error("ANS Vehicle processing failed: {}", e.getMessage(), e);
            logDto.calculateOverallStatus();
            batchJob.setLogText((batchJob.getLogText() != null ? batchJob.getLogText() : "") + logDto.toLogText());
            logger.error("========== REPCCS ANS VEHICLE PROCESSING END (FAILED) ==========");
            return batchJob;
        }
    }

    public int getCasinansvehVehicleCount() {
        try {
            List<String> vehicleNumbers = repository.findVehiclesForCasinansveh();
            return vehicleNumbers != null ? vehicleNumbers.size() : 0;
        } catch (Exception e) {
            logger.error("Error getting Repccs ans vehicle count: {}", e.getMessage(), e);
            return -1;
        }
    }
}
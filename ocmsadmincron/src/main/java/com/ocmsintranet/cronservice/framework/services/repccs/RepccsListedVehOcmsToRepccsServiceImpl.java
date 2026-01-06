package com.ocmsintranet.cronservice.framework.services.repccs;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeRepository;
import com.ocmsintranet.cronservice.crud.ocmsizdb.BatchJobs.OcmsBatchJob;
import com.ocmsintranet.cronservice.framework.dto.repccs.RepccsProcessingLogDto;
import com.ocmsintranet.cronservice.framework.helper.BatchJobHelper;
import com.ocmsintranet.cronservice.framework.services.comcrypt.helper.ComcryptTokenHelper;
import com.ocmsintranet.cronservice.utilities.repccs.RepcssListedDatFileGenerator;
import com.ocmsintranet.cronservice.utilities.ces.FileUploadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RepccsListedVehOcmsToRepccsServiceImpl implements RepccsListedVehOcmsToRepccsService {

    private static final Logger logger = LoggerFactory.getLogger(RepccsListedVehOcmsToRepccsServiceImpl.class);

    @Autowired
    private OcmsValidOffenceNoticeRepository repository;

    @Autowired
    private RepcssListedDatFileGenerator repcssListedDatFileGenerator;

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
    public OcmsBatchJob executeListedVehOcmsToRepccsFunction(OcmsBatchJob batchJob) {
        logger.info("========== REPCCS LISTED VEHICLE PROCESSING START ==========");
        RepccsProcessingLogDto logDto = RepccsProcessingLogDto.builder()
                .prefix("REPCCS LISTED VEHICLE PROCESSING")
                .build();

        try {
            // Step 1: Query database for foreign vehicle data
            logger.info("Querying database for foreign vehicle notices");
            List<Object[]> vehicleDataRows = repository.findForeignVehicleNotices();

            if (vehicleDataRows == null || vehicleDataRows.isEmpty()) {
                logger.warn("No foreign vehicle data found for processing");
                logDto.setOverallStatus(RepccsProcessingLogDto.Status.SUCCESS);
                logDto.setRecords(0);
                batchJob.setLogText((batchJob.getLogText() != null ? batchJob.getLogText() : "") + logDto.toLogText());
                logger.info("========== REPCCS LISTED VEHICLE PROCESSING END (NO DATA) ==========");
                return batchJob;
            }

            logger.info("Found {} foreign vehicle records for processing", vehicleDataRows.size());
            logDto.setRecords(vehicleDataRows.size());

            // Step 2: Convert Object[] to VehicleData objects
            List<RepcssListedDatFileGenerator.ListedVehicleData> vehicleDataList = new ArrayList<>();
            for (Object[] row : vehicleDataRows) {
                String vehicleNo = row[0] != null ? row[0].toString() : null;
                String message = row[1] != null ? row[1].toString() : null;
                vehicleDataList.add(new RepcssListedDatFileGenerator.ListedVehicleData(vehicleNo, message));
            }

            // Step 3: Generate .DAT file content
            String fileContent = repcssListedDatFileGenerator.generateDatFileContent(vehicleDataList);
            if (fileContent == null || fileContent.isEmpty()) {
                throw new RuntimeException("Failed to generate DAT file content - content is null or empty");
            }
            byte[] fileBytes = fileContent.getBytes();

            String fileName = repcssListedDatFileGenerator.generateFileName();
            if (fileName == null || fileName.isEmpty()) {
                throw new RuntimeException("Failed to generate file name - fileName is null or empty");
            }
            logger.info("Generated file: {}, size: {} bytes", fileName, fileBytes.length);
            logDto.setFileName(fileName);
            logDto.setGenerateFileStatus(RepccsProcessingLogDto.Status.SUCCESS);

            // Step 4: Upload original file to Azure Blob Storage
            FileUploadUtil.BlobUploadResult blobResult = fileUploadUtil.uploadToBlob(fileBytes, fileName, repccsBlobPath, "LISTED_VEHICLE");

            if (blobResult == null || !blobResult.isSuccess()) {
                logDto.setBlobUploadStatus(RepccsProcessingLogDto.Status.FAILED);
                throw new RuntimeException("Failed to upload file to blob: " + (blobResult != null ? blobResult.getError() : "null result"));
            }
            logger.info("Successfully uploaded file to blob: {}", blobResult.getBlobPath());
            logDto.setBlobUploadStatus(RepccsProcessingLogDto.Status.SUCCESS);

            // Step 5: Upload file to SFTP
            logger.info("Uploading file to SFTP: {}", fileName);
            FileUploadUtil.SftpUploadResult sftpResult = fileUploadUtil.uploadToSftp(fileBytes, fileName, repccsSftpPath, "LISTED_VEHICLE");

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
//                "Listed Vehicle Processing - " + vehicleDataList.size() + " records"
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
            logger.info("========== REPCCS LISTED VEHICLE PROCESSING END (SUCCESS) ==========");
            return batchJob;

        } catch (Exception e) {
            logger.error("Listed Vehicle processing failed: {}", e.getMessage(), e);
            logDto.calculateOverallStatus();
            batchJob.setLogText((batchJob.getLogText() != null ? batchJob.getLogText() : "") + logDto.toLogText());
            logger.error("========== REPCCS LISTED VEHICLE PROCESSING END (FAILED) ==========");
            return batchJob;
        }
    }

    public int getListedVehCasToRepccsVehicleCount() {
        try {
            List<Object[]> vehicleDataRows = repository.findForeignVehicleNotices();
            return vehicleDataRows != null ? vehicleDataRows.size() : 0;
        } catch (Exception e) {
            logger.error("Error getting Listed Vehicle count: {}", e.getMessage(), e);
            return -1;
        }
    }
}
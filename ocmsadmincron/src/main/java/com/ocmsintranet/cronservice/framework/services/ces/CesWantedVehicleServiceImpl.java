package com.ocmsintranet.cronservice.framework.services.ces;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.crud.ocmsizdb.BatchJobs.OcmsBatchJob;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeRepository;
import com.ocmsintranet.cronservice.framework.dto.ces.CesProcessingLogDto;
import com.ocmsintranet.cronservice.framework.dto.ces.CesWantedVehicleData;
import com.ocmsintranet.cronservice.framework.helper.UploadHelper;
import com.ocmsintranet.cronservice.framework.services.comcrypt.helper.ComcryptTokenHelper;
import com.ocmsintranet.cronservice.utilities.ces.CesWantedVehiclesDatGenerator;
import com.ocmsintranet.cronservice.utilities.ces.FileUploadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CesWantedVehicleServiceImpl implements CesWantedVehicleService {

    private static final Logger logger = LoggerFactory.getLogger(CesWantedVehicleServiceImpl.class);

    @Autowired
    private OcmsValidOffenceNoticeRepository repository;

    @Autowired
    private CesWantedVehiclesDatGenerator cesWantedVehiclesDatGenerator;

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
    public OcmsBatchJob executeCesWantedVerhicleFunction(OcmsBatchJob batchJob) {
        logger.info("========== WANTED VEHICLE PROCESSING START ==========");
        CesProcessingLogDto logDto = CesProcessingLogDto.builder()
                .prefix("WANTED VEHICLE PROCESSING")
                .build();

        try {
            // Query foreign vehicle notices from database
            logger.info("Querying database for foreign vehicle notices");
            List<Object[]> vehicleDataRows = repository.findCertisForeignVehicleNotices();

            if (vehicleDataRows == null || vehicleDataRows.isEmpty()) {
                logger.warn("No foreign vehicle data found for Wanted Vehicle processing");
                logDto.setOverallStatus(CesProcessingLogDto.Status.SUCCESS);
                logDto.setRecords(0);
                batchJob.setLogText((batchJob.getLogText() != null ? batchJob.getLogText() : "") + logDto.toLogText());
                logger.info("========== WANTED VEHICLE PROCESSING END (NO DATA) ==========");
                return batchJob;
            }

            logger.info("Found {} foreign vehicle records for processing", vehicleDataRows.size());
            logDto.setRecords(vehicleDataRows.size());

            // Convert Object[] to CesWantedVehicleData objects
            List<CesWantedVehicleData> vehicleDataList = new ArrayList<>();
            for (Object[] row : vehicleDataRows) {
                String vehicleNo = row[0] != null ? row[0].toString() : null;
                String message = row[1] != null ? row[1].toString() : null;
                vehicleDataList.add(new CesWantedVehicleData(vehicleNo, message));
            }

            // Generate .dat file content
            String fileContent = cesWantedVehiclesDatGenerator.generateDatFileContent(vehicleDataList);
            if (fileContent == null || fileContent.isEmpty()) {
                throw new RuntimeException("Failed to generate DAT file content - content is null or empty");
            }
            byte[] fileBytes = fileContent.getBytes();

            String fileName = cesWantedVehiclesDatGenerator.generateFileName();
            if (fileName == null || fileName.isEmpty()) {
                throw new RuntimeException("Failed to generate file name - fileName is null or empty");
            }
            logger.info("Generated file: {}, size: {} bytes", fileName, fileBytes.length);
            logDto.setFileName(fileName);
            logDto.setGenerateFileStatus(CesProcessingLogDto.Status.SUCCESS);

            // Step 1: Upload to Azure Blob Storage
            FileUploadUtil.BlobUploadResult blobResult = fileUploadUtil.uploadToBlob(fileBytes, fileName, cesBlobPath, "WANTED_VEHICLE");

            if (blobResult == null || !blobResult.isSuccess()) {
                logDto.setBlobUploadStatus(CesProcessingLogDto.Status.FAILED);
                throw new RuntimeException("Failed to upload file to blob: " + (blobResult != null ? blobResult.getError() : "null result"));
            }
            logger.info("Successfully uploaded file to blob: {}", blobResult.getBlobPath());
            logDto.setBlobUploadStatus(CesProcessingLogDto.Status.SUCCESS);

            // Step 2: Upload NON-ENCRYPTED file to SFTP (original .DAT file)
            logger.info("Uploading non-encrypted file to SFTP: {}", fileName);
            UploadHelper.SftpUploadResult nonEncryptedSftpResult = uploadHelper.uploadToCes(fileBytes, fileName, cesSftpPath, "WANTED_VEHICLE");

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
                "Wanted Vehicle Processing - " + vehicleDataList.size() + " records"
            );

            if (requestId == null) {
                logDto.setSftpEncryptedStatus(CesProcessingLogDto.Status.FAILED);
                throw new RuntimeException("Failed to request token from COMCRYPT API");
            }
            logger.info("Token request sent, requestId: {}. Encryption and SFTP upload will be handled by ComcryptCallbackHelper.", requestId);
            logDto.setSftpEncryptedStatus(CesProcessingLogDto.Status.SUCCESS);
            logDto.calculateOverallStatus();

            batchJob.setLogText((batchJob.getLogText() != null ? batchJob.getLogText() : "") + logDto.toLogText());
            logger.info("========== WANTED VEHICLE PROCESSING END (SUCCESS) ==========");
            return batchJob;

        } catch (Exception e) {
            logger.error("Wanted Vehicle processing failed: {}", e.getMessage(), e);
            logDto.calculateOverallStatus();
            batchJob.setLogText((batchJob.getLogText() != null ? batchJob.getLogText() : "") + logDto.toLogText());
            logger.error("========== WANTED VEHICLE PROCESSING END (FAILED) ==========");
            return batchJob;
        }
    }
}

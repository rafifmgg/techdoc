package com.ocmsintranet.cronservice.framework.services.ces;

import com.ocmsintranet.cronservice.crud.beans.SystemConstant;
import com.ocmsintranet.cronservice.crud.ocmsizdb.BatchJobs.OcmsBatchJob;
import com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsValidOffenceNotice.OcmsValidOffenceNoticeRepository;
import com.ocmsintranet.cronservice.framework.dto.ces.CesAnsVehicleData;
import com.ocmsintranet.cronservice.framework.dto.ces.CesProcessingLogDto;
import com.ocmsintranet.cronservice.framework.helper.UploadHelper;
import com.ocmsintranet.cronservice.framework.services.comcrypt.helper.ComcryptTokenHelper;
import com.ocmsintranet.cronservice.utilities.ces.CesAnsVehicleDatGenerator;
import com.ocmsintranet.cronservice.utilities.ces.FileUploadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CesAnsVehicleServiceImpl implements CesAnsVehicleService {

    private static final Logger logger = LoggerFactory.getLogger(CesAnsVehicleServiceImpl.class);

    @Autowired
    private OcmsValidOffenceNoticeRepository repository;

    @Autowired
    private CesAnsVehicleDatGenerator cesAnsVehicleDatGenerator;

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
    public OcmsBatchJob executeCesAnsVehicleFunction(OcmsBatchJob batchJob) {
        logger.info("========== ANS VEHICLE PROCESSING START ==========");
        CesProcessingLogDto logDto = CesProcessingLogDto.builder()
                .prefix("ANS VEHICLE PROCESSING")
                .build();

        try {
            // Step 1: Query database for vehicle numbers
            logger.info("Querying database for vehicles with ANS suspension");
            List<String> vehicleNumbers = repository.findCertisVehiclesForCasinansveh();

            if (vehicleNumbers == null || vehicleNumbers.isEmpty()) {
                logger.warn("No vehicles found for ANS processing");
                logDto.setOverallStatus(CesProcessingLogDto.Status.SUCCESS);
                logDto.setRecords(0);
                batchJob.setLogText((batchJob.getLogText() != null ? batchJob.getLogText() : "") + logDto.toLogText());
                logger.info("========== ANS VEHICLE PROCESSING END (NO DATA) ==========");
                return batchJob;
            }

            logger.info("Found {} vehicles for ANS processing", vehicleNumbers.size());
            logDto.setRecords(vehicleNumbers.size());

            // Step 2: Convert to CesAnsVehicleData list
            List<CesAnsVehicleData> vehicleDataList = new ArrayList<>();
            for (String vehicleNumber : vehicleNumbers) {
                CesAnsVehicleData data = new CesAnsVehicleData();
                data.setLicencePlate(vehicleNumber);
                vehicleDataList.add(data);
            }

            // Validate vehicle data
            for (CesAnsVehicleData data : vehicleDataList) {
                if (data.getLicencePlate() == null || data.getLicencePlate().isEmpty()) {
                    throw new RuntimeException("Vehicle data validation failed: licence plate is empty");
                }
            }

            // Step 3: Generate .DAT file content
            String fileContent = cesAnsVehicleDatGenerator.generateDatFileContent(vehicleDataList);
            if (fileContent == null || fileContent.isEmpty()) {
                throw new RuntimeException("Failed to generate DAT file content - content is null or empty");
            }
            byte[] fileBytes = fileContent.getBytes();

            String fileName = cesAnsVehicleDatGenerator.generateFileName();
            if (fileName == null || fileName.isEmpty()) {
                throw new RuntimeException("Failed to generate file name - fileName is null or empty");
            }
            logger.info("Generated file: {}, size: {} bytes", fileName, fileBytes.length);
            logDto.setFileName(fileName);
            logDto.setGenerateFileStatus(CesProcessingLogDto.Status.SUCCESS);

            // Step 4: Upload original file to Azure Blob Storage (backup)
            FileUploadUtil.BlobUploadResult blobResult = fileUploadUtil.uploadToBlob(fileBytes, fileName, cesBlobPath, "ANS_VEHICLE");

            if (blobResult == null || !blobResult.isSuccess()) {
                logDto.setBlobUploadStatus(CesProcessingLogDto.Status.FAILED);
                throw new RuntimeException("Failed to upload file to blob: " + (blobResult != null ? blobResult.getError() : "null result"));
            }
            logger.info("Successfully uploaded file to blob: {}", blobResult.getBlobPath());
            logDto.setBlobUploadStatus(CesProcessingLogDto.Status.SUCCESS);

            // Step 5: Upload NON-ENCRYPTED file to SFTP (original .DAT file)
            logger.info("Uploading non-encrypted file to SFTP: {}", fileName);
            UploadHelper.SftpUploadResult nonEncryptedSftpResult = uploadHelper.uploadToCes(fileBytes, fileName, cesSftpPath, "ANS_VEHICLE");

            if (nonEncryptedSftpResult == null || !nonEncryptedSftpResult.isSuccess()) {
                logDto.setSftpNonEncryptedStatus(CesProcessingLogDto.Status.FAILED);
                throw new RuntimeException("SFTP upload (non-encrypted) failed: " + (nonEncryptedSftpResult != null ? nonEncryptedSftpResult.getError() : "null result"));
            }
            logger.info("Successfully uploaded non-encrypted file to SFTP: {}", nonEncryptedSftpResult.getSftpPath());
            logDto.setSftpNonEncryptedStatus(CesProcessingLogDto.Status.SUCCESS);

            // Step 6: PGP Encrypt file - Request token and encrypt
            logger.info("Starting PGP encryption for file: {}", fileName);
            String requestId = comcryptTokenHelper.requestToken(
                comsAppcode,
                SystemConstant.CryptOperation.ENCRYPTION,
                fileName,
                "ANS Vehicle Processing - " + vehicleNumbers.size() + " records"
            );

            if (requestId == null) {
                logDto.setSftpEncryptedStatus(CesProcessingLogDto.Status.FAILED);
                throw new RuntimeException("Failed to request token from COMCRYPT API");
            }
            logger.info("Token request sent, requestId: {}. Encryption and SFTP upload will be handled by ComcryptCallbackHelper.", requestId);
            logDto.setSftpEncryptedStatus(CesProcessingLogDto.Status.SUCCESS);
            logDto.calculateOverallStatus();

            batchJob.setLogText((batchJob.getLogText() != null ? batchJob.getLogText() : "") + logDto.toLogText());
            logger.info("========== ANS VEHICLE PROCESSING END (SUCCESS) ==========");
            return batchJob;

        } catch (Exception e) {
            logger.error("ANS Vehicle processing failed: {}", e.getMessage(), e);
            logDto.calculateOverallStatus();
            batchJob.setLogText((batchJob.getLogText() != null ? batchJob.getLogText() : "") + logDto.toLogText());
            logger.error("========== ANS VEHICLE PROCESSING END (FAILED) ==========");
            return batchJob;
        }
    }
}
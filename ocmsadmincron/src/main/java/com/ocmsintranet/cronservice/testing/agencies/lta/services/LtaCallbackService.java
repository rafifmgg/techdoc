package com.ocmsintranet.cronservice.testing.agencies.lta.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ocmsintranet.cronservice.testing.agencies.lta.models.TestStepResult;
import com.ocmsintranet.cronservice.testing.agencies.lta.helpers.LtaTestDatabaseHelper;
import com.ocmsintranet.cronservice.utilities.sftpComponent.SftpUtil;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Service to handle LTA result test flows.
 */
@Service
public class LtaCallbackService {

    private final SftpUtil sftpUtil;
    private final LtaTestDatabaseHelper dbHelper;

    private static final String SFTP_SERVER = "lta";
    private static final String SFTP_INPUT_PATH = "/vrls/input";
    private static final String SFTP_OUTPUT_PATH = "/vrls/output";

    @Autowired
    public LtaCallbackService(SftpUtil sftpUtil, LtaTestDatabaseHelper dbHelper) {
        this.sftpUtil = sftpUtil;
        this.dbHelper = dbHelper;
    }

    /**
     * Process result test flow.
     * This method lists files in SFTP container, downloads the latest file,
     * and uploads it to the output directory with a new name.
     *
     * @return List of test step results
     */
    public List<TestStepResult> processCallbackTest() {
        List<TestStepResult> results = new ArrayList<>();

        // Step 1: List files in SFTP container
        TestStepResult step1 = new TestStepResult(
            "Step 1: Listing files in SFTP container",
            "SUCCESS"
        );
        results.add(step1);

        try {
            // Get current date in YYYYMMDD format
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            String currentDate = dateFormat.format(new Date());
            String filePrefix = "VRL-URA-OFFENQ-D1-" + currentDate;

            step1.addDetail("Looking for files with prefix: " + filePrefix);
            step1.addDetail("In directory: " + SFTP_INPUT_PATH);

            // List files in SFTP container
            List<String> files = sftpUtil.listFiles(SFTP_SERVER, SFTP_INPUT_PATH);
            step1.setJsonData(files);

            // Filter files by prefix
            List<String> matchingFiles = new ArrayList<>();
            for (String file : files) {
                if (file.startsWith(filePrefix)) {
                    matchingFiles.add(file);
                }
            }

            step1.addDetail("Found " + matchingFiles.size() + " matching files");

            if (matchingFiles.isEmpty()) {
                step1.setStatus("FAILED");
                step1.addDetail("❌ No matching files found");
                return results;
            }

            // Sort files by last modified time (descending)
            matchingFiles.sort(Comparator.reverseOrder());

            String latestFile = matchingFiles.get(0);
            step1.addDetail("✅ Latest file: " + latestFile);

            // Step 2: Download file and process
            TestStepResult step2 = new TestStepResult(
                "Step 2: Downloading and processing file",
                "SUCCESS"
            );
            results.add(step2);

            try {
                // Full path to the file on SFTP server
                String fullSftpPath = SFTP_INPUT_PATH + "/" + latestFile;
                step2.addDetail("Downloading file from: " + fullSftpPath);

                // Download file
                byte[] fileContent = sftpUtil.downloadFile(SFTP_SERVER, fullSftpPath);
                step2.addDetail("Downloaded file size: " + fileContent.length + " bytes");

                // Create temporary directory if it doesn't exist
                String tempDir = System.getProperty("user.dir") + "/logs/lta/temp";
                Files.createDirectories(Paths.get(tempDir));

                // Save downloaded file to temporary location with original name
                String originalFilePath = tempDir + "/" + latestFile;
                Files.write(Paths.get(originalFilePath), fileContent);
                step2.addDetail("Saved original file to: " + originalFilePath);

                // Step 2 completed successfully
                step2.setStatus("SUCCESS");

                // Step 3: Process file content and create p7 file
                TestStepResult step3 = processFileAndCreateP7(fileContent, latestFile, tempDir, originalFilePath);
                results.add(step3);

            } catch (Exception e) {
                step2.setStatus("FAILED");
                step2.addDetail("❌ Error processing file: " + e.getMessage());
            }

        } catch (Exception e) {
            step1.setStatus("FAILED");
            step1.addDetail("❌ Error listing files: " + e.getMessage());
        }

        // Add summary
        TestStepResult summary = createSummary(results);
        results.add(summary);

        return results;
    }

    /**
     * Process file content and create p7 file.
     *
     * @param fileContent Original file content
     * @param latestFile Original file name
     * @param tempDir Temporary directory path
     * @param originalFilePath Path to the original file
     * @return TestStepResult containing the results of this step
     */
    private TestStepResult processFileAndCreateP7(byte[] fileContent, String latestFile, String tempDir, String originalFilePath) {
        TestStepResult step3 = new TestStepResult(
            "Step 3: Processing file content and creating p7 file",
            "SUCCESS"
        );

        // Define variables for use across steps
        String vehicleNumber = null;
        String newFileName = latestFile;
        String p7FilePath = null;

        try {
            // Read file content
            String fileContentStr = new String(fileContent, StandardCharsets.UTF_8);
            step3.addDetail("File content: " + fileContentStr);

            // Generate timestamp for new p7 filename
            SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = timestampFormat.format(new Date());
            newFileName = "VRL-URA-OFFREPLY-D2-" + timestamp;

            // Create p7 file content
            StringBuilder p7Content = new StringBuilder();
            p7Content.append("H").append(timestamp.substring(0, 8)).append("\n");

            // Process all data lines in the file
            String[] lines = fileContentStr.split("\n");
            int dataLineCount = 0;

            for (String line : lines) {
                if (line.startsWith("D")) { // Data line starts with D
                    dataLineCount++;
                    // Ekstrak data dari baris sesuai format:
                    // RecordType (A1) | Vehicle Registration Number (A12) | Notice Number (A10) | Date and Time of Offence (N12) | Processing Date (N8) | Processing Time (N4)
                    // String recordType = line.substring(0, 1); // D (tidak digunakan)
                    String currentVehicleNumber = line.substring(1, 13).trim();
                    String currentNoticeNumber = line.substring(13, 23).trim();
                    String dateTimeOffence = line.substring(23, 35).trim();
                    // String processingDate = line.substring(37, 45).trim(); (tidak digunakan)
                    // String processingTime = line.substring(45, 49).trim(); (tidak digunakan)

                    step3.addDetail("Processing vehicle number: " + currentVehicleNumber);
                    step3.addDetail("Processing notice number: " + currentNoticeNumber);
                    step3.addDetail("Processing date time offence: " + dateTimeOffence);

                    // Format tanggal dan waktu saat ini
                    String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
                    String time = new SimpleDateFormat("HHmm").format(new Date());
                    
                    // Periksa apakah noticeDateAndTime (dateTimeOffence) lebih besar dari hari ini
                    String noticeDateOnly = dateTimeOffence.substring(0, 8); // Ambil hanya bagian tanggal (yyyyMMdd)
                    String errorCode = "0"; // Default error code
                    
                    boolean isFutureDate = false;
                    if (noticeDateOnly.compareTo(today) > 0) {
                        // Jika tanggal pelanggaran lebih besar dari hari ini, set error code menjadi 4
                        errorCode = "4";
                        isFutureDate = true;
                        step3.addDetail("⚠️ Notice date " + noticeDateOnly + " is in the future. Setting error code to 4.");
                    }

                    // Look up vehicle data
                    com.ocmsintranet.cronservice.testing.agencies.lta.constants.VehicleDataConstants.VehicleData vehicleData =
                        com.ocmsintranet.cronservice.testing.agencies.lta.constants.VehicleDataConstants.getVehicleByRegistrationNumber(currentVehicleNumber);

                    if (vehicleData != null) {
                        step3.addDetail("Found vehicle data for: " + currentVehicleNumber);
                        step3.addDetail("Owner: " + vehicleData.getOwnerName());
                        step3.addDetail("Chassis: " + vehicleData.getChassisNumber());

                        // Gunakan metode createAppendixFRecord untuk membuat record dengan format yang tepat
                        String record = dbHelper.createAppendixFRecord(
                            currentVehicleNumber,
                            vehicleData.getChassisNumber(),
                            vehicleData.getDiplomaticFlag(), // diplomaticFlag
                            currentNoticeNumber, // noticeNumber
                            vehicleData.getOwnerIdType(),
                            vehicleData.getPassportPlace(), // passportPlace
                            vehicleData.getOwnerId(),
                            vehicleData.getOwnerName(),
                            vehicleData.getAddressType(),
                            vehicleData.getBlockHouseNumber(),
                            vehicleData.getStreetName(),
                            vehicleData.getFloorNumber(),
                            vehicleData.getUnitNumber(),
                            vehicleData.getBuildingName(),
                            vehicleData.getPostalCode(),
                            vehicleData.getMakeDescription(),
                            vehicleData.getPrimaryColour(),
                            vehicleData.getSecondaryColour(),
                            vehicleData.getRoadTaxExpiryDate(),
                            vehicleData.getUnladenWeight(),
                            vehicleData.getMaximumLadenWeight(),
                            vehicleData.getEffectiveOwnershipDate(),
                            vehicleData.getDeregistrationDate(), // deregistrationDate
                            isFutureDate ? "4" : vehicleData.getReturnErrorCode(), // Gunakan error code 4 jika tanggal di masa depan
                            today,
                            time,
                            vehicleData.getIuObuLabelNumber(),
                            vehicleData.getRegisteredAddressEffectiveDate(),
                            vehicleData.getMailingBlockHouse(),
                            vehicleData.getMailingStreetName(),
                            vehicleData.getMailingFloorNumber(),
                            vehicleData.getMailingUnitNumber(),
                            vehicleData.getMailingBuildingName(),
                            vehicleData.getMailingPostalCode(),
                            vehicleData.getMailingAddressEffectiveDate()  // mailingAddressEffectiveDate
                        );

                        // Tambahkan record ke konten p7
                        p7Content.append(record);
                    } else {
                        step3.addDetail("⚠️ No vehicle data found for: " + currentVehicleNumber);

                        // Gunakan metode createAppendixFRecord untuk membuat record dengan format yang tepat
                        // dan kode error "2" untuk kendaraan yang tidak ditemukan atau "4" untuk tanggal di masa depan
                        String record = dbHelper.createAppendixFRecord(
                            currentVehicleNumber,
                            "", // chassisNumber
                            "", // diplomaticFlag
                            currentNoticeNumber, // noticeNumber
                            "", // ownerIdType
                            "", // passportPlace
                            "", // ownerId
                            "", // ownerName
                            "", // addressType
                            "", // blockHouse
                            "", // streetName
                            "", // floorNumber
                            "", // unitNumber
                            "", // buildingName
                            "", // postalCode
                            "", // makeDescription
                            "", // primaryColour
                            "", // secondaryColour
                            "", // roadTaxExpiry
                            "", // unladenWeight
                            "", // maxLadenWeight
                            "", // effectiveOwnership
                            "", // deregistrationDate
                            isFutureDate ? "4" : "2", // errorCode (2 = not found, 4 = future date)
                            today,
                            time,
                            "", // iuNumber
                            "", // regAddressEffectiveDate
                            "", // mailingBlockHouse
                            "", // mailingStreetName
                            "", // mailingFloorNumber
                            "", // mailingUnitNumber
                            "", // mailingBuildingName
                            "", // mailingPostalCode
                            ""  // mailingAddressEffectiveDate
                        );

                        // Tambahkan record ke konten p7
                        p7Content.append(record);
                    }

                    // Add new line after each data record
                    p7Content.append("\n");

                    // Save the first vehicle number for reference
                    if (vehicleNumber == null) {
                        vehicleNumber = currentVehicleNumber;
                    }
                }
            }

            // If no data lines were found
            if (dataLineCount == 0) {
                step3.addDetail("⚠️ No vehicle data lines found in file");
                step3.setStatus("WARNING");
            }

            // Add trailer record with correct count
            p7Content.append("T").append(String.format("%06d", dataLineCount));

            // Save p7 file
            p7FilePath = tempDir + "/" + newFileName;
            Files.write(Paths.get(p7FilePath), p7Content.toString().getBytes());
            step3.addDetail("Created p7 file with " + dataLineCount + " data records at: " + p7FilePath);

            // Upload file to SFTP output directory
            String outputPath = SFTP_OUTPUT_PATH + "/" + newFileName;
            boolean uploadSuccess = sftpUtil.uploadFile(SFTP_SERVER, p7Content.toString().getBytes(), outputPath);

            if (uploadSuccess) {
                step3.addDetail("✅ Successfully uploaded file to: " + outputPath);
                step3.addDetail("📄 P7 Content: \n" + p7Content.toString());
            } else {
                step3.setStatus("FAILED");
                step3.addDetail("❌ Failed to upload file to: " + outputPath);
            }

            // Clean up temporary files
            Files.deleteIfExists(Paths.get(originalFilePath));
            step3.addDetail("Cleaned up original file");

            // Also clean up p7 file if it was created
            if (p7FilePath != null) {
                Files.deleteIfExists(Paths.get(p7FilePath));
                step3.addDetail("Cleaned up p7 file");
            }

        } catch (Exception e) {
            step3.setStatus("FAILED");
            step3.addDetail("❌ Error processing file content: " + e.getMessage());
            e.printStackTrace();
        }

        return step3;
    }

    /**
     * Create summary of test results.
     *
     * @param results List of test step results
     * @return TestStepResult containing the summary
     */
    private TestStepResult createSummary(List<TestStepResult> results) {
        TestStepResult summary = new TestStepResult(
            "Result Test Summary",
            results.stream().allMatch(r -> "SUCCESS".equals(r.getStatus())) ? "SUCCESS" : "PARTIAL"
        );

        long successCount = results.stream()
            .filter(step -> "SUCCESS".equals(step.getStatus()))
            .count();

        summary.addDetail("📊 Test Flow Statistics:");
        summary.addDetail("✅ Successful steps: " + successCount);
        summary.addDetail("❌ Failed steps: " + (results.size() - successCount));

        return summary;
    }
}

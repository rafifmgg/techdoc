package com.ocmsintranet.cronservice.testing.agencies.mha.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.ocmsintranet.cronservice.testing.agencies.mha.models.TestStepResult;

import com.ocmsintranet.cronservice.testing.agencies.mha.constants.MhaFileFormatConstants;
import com.ocmsintranet.cronservice.utilities.sftpComponent.SftpUtil;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.ocmsintranet.cronservice.framework.services.agencyFileExchange.helper.MhaFileFormatHelper;

/**
 * Service to handle MHA result test flows.
 */
@Service
public class MhaCallbackService {

    private final SftpUtil sftpUtil;

    private static final String SFTP_SERVER = "mha";

    @Value("${sftp.folders.mha.upload}")
    private String sftpInputPath;

    @Value("${sftp.folders.mha.download}")
    private String sftpOutputPath;

    @Autowired
    public MhaCallbackService(SftpUtil sftpUtil) {
        this.sftpUtil = sftpUtil;
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
            String filePrefix = MhaFileFormatConstants.INPUT_FILE_PREFIX + currentDate;

            step1.addDetail("Looking for files with prefix: " + filePrefix);
            step1.addDetail("In directory: " + sftpInputPath);

            // List files in SFTP container
            List<String> files = sftpUtil.listFiles(SFTP_SERVER, sftpInputPath);
            step1.setJsonData(String.join(", ", files));

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
                String fullSftpPath = sftpInputPath + "/" + latestFile;
                step2.addDetail("Downloading file from: " + fullSftpPath);

                // Download file
                byte[] fileContent = sftpUtil.downloadFile(SFTP_SERVER, fullSftpPath);
                step2.addDetail("Downloaded file size: " + fileContent.length + " bytes");

                // Create temporary directory if it doesn't exist
                String tempDir = System.getProperty("user.dir") + "/logs/mha/temp";
                Files.createDirectories(Paths.get(tempDir));

                // Save original file to temp directory
                String originalFilePath = tempDir + "/" + latestFile;
                Files.write(Paths.get(originalFilePath), fileContent);
                step2.addDetail("Saved original file to: " + originalFilePath);

                // Process file and create output file
                TestStepResult step3 = processFileAndCreateOutput(fileContent, latestFile, tempDir, originalFilePath);
                results.add(step3);

            } catch (Exception e) {
                step2.setStatus("FAILED");
                step2.addDetail("❌ Error downloading file: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (Exception e) {
            step1.setStatus("FAILED");
            step1.addDetail("❌ Error listing files: " + e.getMessage());
            e.printStackTrace();
        }

        // Add summary
        results.add(createSummary(results));

        return results;
    }

    /**
     * Process file content and create output file.
     *
     * @param fileContent Original file content
     * @param latestFile Original file name
     * @param tempDir Temporary directory path
     * @param originalFilePath Path to the original file
     * @return TestStepResult containing the results of this step
     */
    private TestStepResult processFileAndCreateOutput(byte[] fileContent, String latestFile, String tempDir, String originalFilePath) {
        TestStepResult step3 = new TestStepResult(
            "Step 3: Processing file and creating output file",
            "SUCCESS"
        );

        String outputFilePath = null;
        try {
            // Convert file content to string
            String content = new String(fileContent, StandardCharsets.UTF_8);
            step3.addDetail("File content length: " + content.length() + " characters");

            // Split content into lines
            String[] lines = content.split("\n");
            step3.addDetail("File contains " + lines.length + " lines");

            // Create new file name for output
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = dateTimeFormat.format(new Date());
            String newFileName = MhaFileFormatConstants.OUTPUT_FILE_PREFIX + timestamp + MhaFileFormatConstants.OUTPUT_FILE_EXTENSION;
            step3.addDetail("New file name: " + newFileName);

            // Create output file content
            StringBuilder outputContent = new StringBuilder();

            // Process header and data records
            int dataLineCount = 0;

            // Add header record
            // Format: [9 spaces][YYYYMMDD][HHMMSS][record count with leading zeros][186 spaces]
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            SimpleDateFormat timeFormat = new SimpleDateFormat("HHmmss");
            String today = dateFormat.format(new Date());
            String time = timeFormat.format(new Date());

            // Count data lines first to prepare header
            boolean isHeaderLine = true;
            for (String line : lines) {
                // MHA format: baris pertama selalu header, baris selanjutnya selalu record data
                if (isHeaderLine) {
                    // Ini adalah baris header, lewati
                    isHeaderLine = false;
                    continue;
                }

                // Hitung semua baris lainnya sebagai data
                if (line.length() >= MhaFileFormatConstants.INPUT_DATA_LENGTH) {
                    dataLineCount++;
                }
            }

            // Create header with correct record count
            StringBuilder header = new StringBuilder();
            header.append("         "); // 9 spaces
            header.append(today);
            header.append(time);
            header.append(String.format("%06d", dataLineCount)); // 6 digits with leading zeros
            // Add remaining spaces to reach 215 bytes
            int remainingSpaces = MhaFileFormatConstants.OUTPUT_HEADER_LENGTH - header.length();
            for (int i = 0; i < remainingSpaces; i++) {
                header.append(" ");
            }

            // Verifikasi panjang header
            if (header.length() != MhaFileFormatConstants.OUTPUT_HEADER_LENGTH) {
                step3.addDetail("⚠️ Warning: Header length is " + header.length() +
                              " but should be " + MhaFileFormatConstants.OUTPUT_HEADER_LENGTH);
            }

            outputContent.append(header).append("\n");

            // Process data records
            boolean firstRecord = true;
            boolean isFirstLine = true;

            for (String line : lines) {
                // MHA format: baris pertama selalu header, baris selanjutnya selalu record data
                if (isFirstLine) {
                    // Ini adalah baris header, lewati
                    isFirstLine = false;
                    continue;
                }

                // Proses semua baris lainnya sebagai data
                if (line.length() >= MhaFileFormatConstants.INPUT_DATA_LENGTH) {
                    try {
                        // Extract NRIC from input data record (positions 0-8)
                        String currentNric = line.substring(0, 9).trim();

                        // Extract URA Reference No from input data record (positions 9-18)
                        String uraRefNo = line.substring(9, 19).trim();

                        // Extract Batch Date Time from input data record (positions 19-32)
                        String batchDateTime = line.substring(19, 33).trim();

                        // Extract timeStamp from input data record (positions 33-56)
                        String timeStamp = line.substring(33, 56).trim();

                        // Generate current timestamp in YYYYMMDDHHMISS format
                        String currentTimestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

                        // Debug log untuk melihat nilai yang diekstrak
                        step3.addDetail("Processing line: NRIC=[" + currentNric + "], URA Ref=[" + uraRefNo + "], BatchDateTime=[" + batchDateTime + "], TimeStamp=[" + timeStamp + "]" );

                        // Log untuk record pertama
                        if (firstRecord) {
                            step3.addDetail("✅ First record NRIC: " + currentNric);
                            firstRecord = false;
                        }

                        // Create output data record
                        StringBuilder record = new StringBuilder();

                        // Cari data dari konstanta berdasarkan UIN
                        MhaFileFormatConstants.MhaTestData mhaData =
                            MhaFileFormatConstants.getTestDataByUin(currentNric);

                        // Berdasarkan definisi field dari user:
                        // [name, start, size, type, remark]

                        // UIN (1-9, 9 chars)
                        record.append(String.format("%-9s", currentNric));

                        if (mhaData != null) {
                            // Data ditemukan, gunakan data dari konstanta
                            step3.addDetail("✅ Found data for NRIC: " + currentNric);

                            // Name (10-75, 66 chars)
                            record.append(String.format("%-66s", mhaData.getName()));

                            // Date of Birth (76-83, 8 chars)
                            record.append(String.format("%-8s", mhaData.getDateOfBirth()));

                            // MHA Address Type (84, 1 char)
                            record.append(String.format("%-1s", mhaData.getMhaAddressType()));

                            // MHA Block No (85-94, 10 chars)
                            record.append(String.format("%-10s", mhaData.getMhaBlockNo()));

                            // MHA Street Name (95-126, 32 chars)
                            record.append(String.format("%-32s", mhaData.getMhaStreetName()));

                            // MHA Floor No (127-128, 2 chars)
                            record.append(String.format("%-2s", mhaData.getMhaFloorNo()));

                            // MHA Unit No (129-133, 5 chars)
                            record.append(String.format("%-5s", mhaData.getMhaUnitNo()));

                            // MHA Building Name (134-163, 30 chars)
                            record.append(String.format("%-30s", mhaData.getMhaBuildingName()));

                            // Filler (164-167, 4 chars)
                            record.append(String.format("%-4s", mhaData.getFiller()));

                            // MHA New Postal Code (168-173, 6 chars)
                            record.append(String.format("%-6s", mhaData.getMhaNewPostalCode()));

                            // Date of Death (174-181, 8 chars)
                            record.append(String.format("%-8s", mhaData.getDateOfDeath()));

                            // Life Status (182, 1 char)
                            record.append(String.format("%-1s", mhaData.getLifeStatus() != null ? mhaData.getLifeStatus() : " "));

                            // Invalid Address Tag (183, 1 char)
                            record.append(String.format("%-1s", mhaData.getInvalidAddressTag() != null ? mhaData.getInvalidAddressTag() : " "));
                        } else {
                            // Data tidak ditemukan, isi dengan spasi
                            step3.addDetail("⚠️ No data found for NRIC: " + currentNric);

                            // Name (10-75, 66 chars)
                            record.append(String.format("%-66s", ""));

                            // Date of Birth (76-83, 8 chars)
                            record.append(String.format("%-8s", ""));

                            // MHA Address Type (84, 1 char) - gunakan spasi sesuai permintaan
                            record.append(String.format("%-1s", " "));

                            // MHA Block No (85-94, 10 chars)
                            record.append(String.format("%-10s", ""));

                            // MHA Street Name (95-126, 32 chars)
                            record.append(String.format("%-32s", ""));

                            // MHA Floor No (127-128, 2 chars)
                            record.append(String.format("%-2s", ""));

                            // MHA Unit No (129-133, 5 chars)
                            record.append(String.format("%-5s", ""));

                            // MHA Building Name (134-163, 30 chars)
                            record.append(String.format("%-30s", ""));

                            // Filler (164-167, 4 chars)
                            record.append(String.format("%-4s", ""));

                            // MHA New Postal Code (168-173, 6 chars)
                            record.append(String.format("%-6s", ""));

                            // Date of Death (174-181, 8 chars)
                            record.append(String.format("%-8s", ""));

                            // Life Status (182, 1 char) - gunakan spasi sesuai permintaan
                            record.append(String.format("%-1s", " "));

                            // Invalid Address Tag (183, 1 char) - gunakan spasi sesuai permintaan
                            record.append(String.format("%-1s", " "));
                        }

                        // URA Reference No (184-193, 10 chars)
                        record.append(String.format("%-10s", uraRefNo));

                        // Batch Date Time (194-207, 14 chars)
                        record.append(String.format("%-14s", batchDateTime));

                        // Date of Address Change (208-215, 8 chars)
                        record.append(String.format("%-8s", ""));

                        // Add timestamp field (216-238, 23 chars) - current timestamp with padding
                        String timestampField = String.format("%-23s", currentTimestamp);
                        record.append(timestampField);

                        // Verifikasi panjang record
                        if (record.length() != MhaFileFormatConstants.OUTPUT_DATA_LENGTH) {
                            step3.addDetail("⚠️ Warning: Record length is " + record.length() +
                                          " but should be " + MhaFileFormatConstants.OUTPUT_DATA_LENGTH);
                        }

                        // Add record to output content
                        outputContent.append(record).append("\n");
                    } catch (Exception e) {
                        step3.addDetail("⚠️ Error processing line: " + e.getMessage());
                        step3.addDetail("Line content: [" + line + "]");
                    }
                }
            }

            // Save output file
            outputFilePath = tempDir + "/" + newFileName;
            Files.write(Paths.get(outputFilePath), outputContent.toString().getBytes());
            step3.addDetail("Created output file with " + dataLineCount + " data records at: " + outputFilePath);

            // Upload file to SFTP output directory
            String outputPath = sftpOutputPath + "/" + newFileName;
            boolean uploadSuccess = sftpUtil.uploadFile(SFTP_SERVER, outputContent.toString().getBytes(), outputPath);

            if (uploadSuccess) {
                step3.addDetail("✅ Successfully uploaded file to: " + outputPath);
                step3.addDetail("📄 Output Content: \n" + outputContent.toString());
            } else {
                step3.setStatus("FAILED");
                step3.addDetail("❌ Failed to upload file to: " + outputPath);
            }

            // Clean up temporary files
            Files.deleteIfExists(Paths.get(originalFilePath));
            step3.addDetail("Cleaned up original file");

            // Also clean up output file if it was created
            if (outputFilePath != null) {
                Files.deleteIfExists(Paths.get(outputFilePath));
                step3.addDetail("Cleaned up output file");
            }

            // Generate and upload MHA report files (TOT and EXP)
            try {
                int totalRecords = dataLineCount;
                String reportBaseTimestamp = timestamp;

                // Build TOT content (parsed by processControlTotalsReport)
                StringBuilder tot = new StringBuilder();
                String reportDate = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
                // Header lines aligning PAGE/DATE to the right (match sample files)
                tot.append(String.format("%-101s%s", "ZUR120OC CONTROL TOTALS LISTING", "PAGE : 1")).append("\n");
                tot.append(String.format("%-101s%s", "===============================", "DATE : " + reportDate)).append("\n");

                // Control totals per MHA sample
                int invalidUinFin = 0;
                int unmatchedValidUinFin = 0;
                int matchCount = totalRecords - invalidUinFin - unmatchedValidUinFin;
                tot.append("A) NO. OF RECORDS READ                         = ").append(totalRecords).append("\n");
                tot.append("B) NO. OF RECORDS MATCH                        = ").append(matchCount).append("\n");
                tot.append("C) NO. OF RECORDS WITH INVALID UIN/FIN         = ").append(invalidUinFin).append("\n");
                tot.append("D) NO. OF RECORDS WITH VALID UIN/FIN UNMATCHED = ").append(unmatchedValidUinFin).append("\n");
                tot.append("****  E N D  O F  R E P O R T  ****").append("\n");

                String totFileName = MhaFileFormatHelper.REPORT_FILE_PREFIX
                    + reportBaseTimestamp
                    + MhaFileFormatHelper.REPORT_TOT_SUFFIX
                    + MhaFileFormatConstants.OUTPUT_FILE_EXTENSION;
                String totSftpPath = sftpOutputPath + "/" + totFileName;
                boolean totUpload = sftpUtil.uploadFile(SFTP_SERVER, tot.toString().getBytes(StandardCharsets.UTF_8), totSftpPath);
                if (totUpload) {
                    step3.addDetail("✅ Successfully uploaded TOT report to: " + totSftpPath);
                    step3.addDetail("📄 TOT Content:\n" + tot.toString());
                } else {
                    step3.addDetail("❌ Failed to upload TOT report to: " + totSftpPath);
                }

                // Build EXP content (parsed by processExceptionsReport)
                StringBuilder exp = new StringBuilder();
                exp.append(String.format("%-101s%s", "DATA SHARING SYSTEM", "PAGE : 1")).append("\n");
                exp.append(String.format("%-101s%s", "ZUR120OC", "DATE : " + reportDate)).append("\n");
                String indent = "                                  "; // 34 spaces
                exp.append(indent).append("LIST OF ID NUMBERS WHICH ARE INVALID OR NOT FOUND IN MHA DATABASE").append("\n");
                exp.append(indent).append("=================================================================").append("\n");
                exp.append("\n");
                exp.append(indent).append("SERIAL NO   ID NUMBER   EXCEPTION STATUS").append("\n");
                exp.append(indent).append("=========   =========   ================").append("\n");
                // no data rows for now (no exceptions)
                final int serialWidth = 9; // align with header 'SERIAL NO'
                final int idWidth = 9;     // align with header 'ID NUMBER'
                exp.append(indent)
                   .append(String.format("%-" + serialWidth + "s", ""))
                   .append("   ")
                   .append(String.format("%-" + idWidth + "s", ""))
                   .append("   ")
                   .append("NO EXCEPTION RECORDS FOUND")
                   .append("\n");
                exp.append("\n");
                exp.append(indent).append("****  E N D  O F  R E P O R T  ****").append("\n");

                String expFileName = MhaFileFormatHelper.REPORT_FILE_PREFIX
                    + reportBaseTimestamp
                    + MhaFileFormatHelper.REPORT_EXP_SUFFIX
                    + MhaFileFormatConstants.OUTPUT_FILE_EXTENSION;
                String expSftpPath = sftpOutputPath + "/" + expFileName;
                boolean expUpload = sftpUtil.uploadFile(SFTP_SERVER, exp.toString().getBytes(StandardCharsets.UTF_8), expSftpPath);
                if (expUpload) {
                    step3.addDetail("✅ Successfully uploaded EXP report to: " + expSftpPath);
                    step3.addDetail("📄 EXP Content:\n" + exp.toString());
                } else {
                    step3.addDetail("❌ Failed to upload EXP report to: " + expSftpPath);
                }
            } catch (Exception genEx) {
                step3.addDetail("⚠️ Failed to generate/upload REPORT TOT/EXP: " + genEx.getMessage());
            }

            return step3;
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
        summary.addDetail("❌ Failed steps: " + (results.size() - successCount - 1)); // Exclude summary itself

        return summary;
    }
}

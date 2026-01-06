package com.ocmsintranet.cronservice.testing.agencies.lta.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ocmsintranet.cronservice.testing.agencies.lta.models.TestStepResult;
import com.ocmsintranet.cronservice.utilities.sftpComponent.SftpUtil;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Service to handle LTA error code test flows.
 * Generates files with specific error codes for testing.
 */
@Service
public class LtaErrorCodeService {

    private final SftpUtil sftpUtil;

    private static final String SFTP_SERVER = "lta";
    private static final String SFTP_OUTPUT_PATH = "/vrls/output";

    @Autowired
    public LtaErrorCodeService(SftpUtil sftpUtil) {
        this.sftpUtil = sftpUtil;
    }

    /**
     * Process error code A test (Total count does not match).
     * Creates a file with error code A and incorrect count in trailer.
     *
     * @return List of test step results
     */
    public List<TestStepResult> processErrorCodeA() {
        List<TestStepResult> results = new ArrayList<>();

        TestStepResult step = new TestStepResult(
            "Creating file with Error Code A (Total count does not match)",
            "SUCCESS"
        );
        results.add(step);

        try {
            // Generate timestamp for filename
            SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = timestampFormat.format(new Date());
            String fileName = "VRL-URA-OFFREPLY-D2-" + timestamp;

            // Create file content
            StringBuilder content = new StringBuilder();

            // Add header
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            String currentDate = dateFormat.format(new Date());
            content.append("H").append(currentDate).append("\n");

            // Add data record with error code A
            content.append("D");
            // Append spaces for all fields except Return Error Code
            for (int i = 0; i < 566; i++) {
                content.append(" ");
            }
            content.append("A"); // Error code A
            // Append remaining spaces
            for (int i = 0; i < 125; i++) {
                content.append(" ");
            }
            content.append("\n");

            // Add trailer with incorrect count (2 instead of 1)
            content.append("T000002");

            // Create temporary directory if it doesn't exist
            String tempDir = System.getProperty("user.dir") + "/logs/lta/temp";
            Files.createDirectories(Paths.get(tempDir));

            // Save file
            String filePath = tempDir + "/" + fileName;
            Files.write(Paths.get(filePath), content.toString().getBytes());
            step.addDetail("Created file with error code A at: " + filePath);

            // Upload file to SFTP output directory
            String outputPath = SFTP_OUTPUT_PATH + "/" + fileName;
            boolean uploadSuccess = sftpUtil.uploadFile(SFTP_SERVER, content.toString().getBytes(), outputPath);

            if (uploadSuccess) {
                step.addDetail("✅ Successfully uploaded file to: " + outputPath);
                step.addDetail("📄 File Content: \n" + content.toString());
            } else {
                step.setStatus("FAILED");
                step.addDetail("❌ Failed to upload file to: " + outputPath);
            }

            // Clean up temporary file
            Files.deleteIfExists(Paths.get(filePath));
            step.addDetail("Cleaned up temporary file");

        } catch (Exception e) {
            step.setStatus("FAILED");
            step.addDetail("❌ Error creating file: " + e.getMessage());
            e.printStackTrace();
        }

        // Add summary
        TestStepResult summary = createSummary(results);
        results.add(summary);

        return results;
    }

    /**
     * Process error code B test (Missing Trailer).
     * Creates a file with error code B and includes trailer (with comment for easy modification).
     *
     * @return List of test step results
     */
    public List<TestStepResult> processErrorCodeB() {
        List<TestStepResult> results = new ArrayList<>();

        TestStepResult step = new TestStepResult(
            "Creating file with Error Code B (Missing Trailer)",
            "SUCCESS"
        );
        results.add(step);

        try {
            // Generate timestamp for filename
            SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = timestampFormat.format(new Date());
            String fileName = "VRL-URA-OFFREPLY-D2-" + timestamp;

            // Create file content
            StringBuilder content = new StringBuilder();

            // Add header
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            String currentDate = dateFormat.format(new Date());
            content.append("H").append(currentDate).append("\n");

            // Add data record with error code B
            content.append("D");
            // Append spaces for all fields except Return Error Code
            for (int i = 0; i < 566; i++) {
                content.append(" ");
            }
            content.append("B"); // Error code B
            // Append remaining spaces
            for (int i = 0; i < 125; i++) {
                content.append(" ");
            }
            content.append("\n");

            // Add trailer (with comment for easy modification)
            // To simulate true error code B behavior, comment out the line below
            content.append("T000001");

            // Create temporary directory if it doesn't exist
            String tempDir = System.getProperty("user.dir") + "/logs/lta/temp";
            Files.createDirectories(Paths.get(tempDir));

            // Save file
            String filePath = tempDir + "/" + fileName;
            Files.write(Paths.get(filePath), content.toString().getBytes());
            step.addDetail("Created file with error code B at: " + filePath);

            // Upload file to SFTP output directory
            String outputPath = SFTP_OUTPUT_PATH + "/" + fileName;
            boolean uploadSuccess = sftpUtil.uploadFile(SFTP_SERVER, content.toString().getBytes(), outputPath);

            if (uploadSuccess) {
                step.addDetail("✅ Successfully uploaded file to: " + outputPath);
                step.addDetail("📄 File Content: \n" + content.toString());
            } else {
                step.setStatus("FAILED");
                step.addDetail("❌ Failed to upload file to: " + outputPath);
            }

            // Clean up temporary file
            Files.deleteIfExists(Paths.get(filePath));
            step.addDetail("Cleaned up temporary file");

        } catch (Exception e) {
            step.setStatus("FAILED");
            step.addDetail("❌ Error creating file: " + e.getMessage());
            e.printStackTrace();
        }

        // Add summary
        TestStepResult summary = createSummary(results);
        results.add(summary);

        return results;
    }

    /**
     * Process error code C test (Missing Header).
     * Creates a file with error code C and includes header (with comment for easy modification).
     *
     * @return List of test step results
     */
    public List<TestStepResult> processErrorCodeC() {
        List<TestStepResult> results = new ArrayList<>();

        TestStepResult step = new TestStepResult(
            "Creating file with Error Code C (Missing Header)",
            "SUCCESS"
        );
        results.add(step);

        try {
            // Generate timestamp for filename
            SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = timestampFormat.format(new Date());
            String fileName = "VRL-URA-OFFREPLY-D2-" + timestamp;

            // Create file content
            StringBuilder content = new StringBuilder();

            // Add header (with comment for easy modification)
            // To simulate true error code C behavior, comment out the line below
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            String currentDate = dateFormat.format(new Date());
            content.append("H").append(currentDate).append("\n");

            // Add data record with error code C
            content.append("D");
            // Append spaces for all fields except Return Error Code
            for (int i = 0; i < 566; i++) {
                content.append(" ");
            }
            content.append("C"); // Error code C
            // Append remaining spaces
            for (int i = 0; i < 125; i++) {
                content.append(" ");
            }
            content.append("\n");

            // Add trailer
            content.append("T000001");

            // Create temporary directory if it doesn't exist
            String tempDir = System.getProperty("user.dir") + "/logs/lta/temp";
            Files.createDirectories(Paths.get(tempDir));

            // Save file
            String filePath = tempDir + "/" + fileName;
            Files.write(Paths.get(filePath), content.toString().getBytes());
            step.addDetail("Created file with error code C at: " + filePath);

            // Upload file to SFTP output directory
            String outputPath = SFTP_OUTPUT_PATH + "/" + fileName;
            boolean uploadSuccess = sftpUtil.uploadFile(SFTP_SERVER, content.toString().getBytes(), outputPath);

            if (uploadSuccess) {
                step.addDetail("✅ Successfully uploaded file to: " + outputPath);
                step.addDetail("📄 File Content: \n" + content.toString());
            } else {
                step.setStatus("FAILED");
                step.addDetail("❌ Failed to upload file to: " + outputPath);
            }

            // Clean up temporary file
            Files.deleteIfExists(Paths.get(filePath));
            step.addDetail("Cleaned up temporary file");

        } catch (Exception e) {
            step.setStatus("FAILED");
            step.addDetail("❌ Error creating file: " + e.getMessage());
            e.printStackTrace();
        }

        // Add summary
        TestStepResult summary = createSummary(results);
        results.add(summary);

        return results;
    }

    /**
     * Create summary of test results.
     *
     * @param results List of test step results
     * @return TestStepResult containing the summary
     */
    private TestStepResult createSummary(List<TestStepResult> results) {
        TestStepResult summary = new TestStepResult(
            "Error Code Test Summary",
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

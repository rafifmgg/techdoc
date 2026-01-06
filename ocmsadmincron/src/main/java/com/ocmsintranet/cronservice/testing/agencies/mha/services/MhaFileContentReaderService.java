package com.ocmsintranet.cronservice.testing.agencies.mha.services;

import com.ocmsintranet.cronservice.testing.agencies.mha.constants.MhaFileFormatConstants;
import com.ocmsintranet.cronservice.testing.agencies.mha.models.TestStepResult;
import com.ocmsintranet.cronservice.utilities.sftpComponent.SftpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Service for reading content of MHA files from SFTP input folder.
 * Provides functionality to find and read the latest input file content.
 */
@Service
public class MhaFileContentReaderService {

    private static final Logger log = LoggerFactory.getLogger(MhaFileContentReaderService.class);
    private static final String SFTP_SERVER = "mha";

    @Autowired
    private SftpUtil sftpUtil;

    @Value("${sftp.folders.mha.upload}")
    private String sftpInputPath;

    /**
     * Read content from the latest MHA input file.
     *
     * @return List of TestStepResult containing the file content
     */
    public List<TestStepResult> readLatestFileContent() {
        List<TestStepResult> results = new ArrayList<>();

        // Step 1: Find latest input file
        TestStepResult step1 = new TestStepResult("Step 1: Finding latest MHA input file", "RUNNING");
        results.add(step1);

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            String currentDate = dateFormat.format(new Date());
            String filePrefix = MhaFileFormatConstants.INPUT_FILE_PREFIX + currentDate;

            step1.addDetail("🔍 Looking for files with prefix: " + filePrefix);
            step1.addDetail("📁 In directory: " + sftpInputPath);

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

            step1.addDetail("📄 Found " + matchingFiles.size() + " matching files");

            if (matchingFiles.isEmpty()) {
                step1.setStatus("FAILED");
                step1.addDetail("❌ No matching files found for today's date: " + currentDate);
                return results;
            }

            // Sort files by name (latest timestamp will be last)
            matchingFiles.sort(Comparator.reverseOrder());

            String latestFile = matchingFiles.get(0);
            step1.addDetail("✅ Latest file found: " + latestFile);
            step1.setStatus("SUCCESS");
            step1.setJsonData(latestFile);

            // Step 2: Read file content
            TestStepResult step2 = new TestStepResult("Step 2: Reading file content", "RUNNING");
            results.add(step2);

            try {
                String filePath = sftpInputPath + "/" + latestFile;
                step2.addDetail("📖 Reading content from: " + filePath);

                // Download and read file content
                byte[] fileBytes = sftpUtil.downloadFile(SFTP_SERVER, filePath);

                if (fileBytes == null || fileBytes.length == 0) {
                    step2.setStatus("FAILED");
                    step2.addDetail("❌ File is empty or could not be read");
                    return results;
                }

                String content = new String(fileBytes, "UTF-8");

                step2.addDetail("📊 File size: " + fileBytes.length + " bytes");
                step2.addDetail("📝 File content:");
                step2.addDetail(content);

                step2.setStatus("SUCCESS");
                // step2.setJsonData(content);

            } catch (Exception e) {
                step2.setStatus("FAILED");
                step2.addDetail("❌ Error reading file content: " + e.getMessage());
                log.error("Error reading MHA file content", e);
            }

        } catch (Exception e) {
            step1.setStatus("FAILED");
            step1.addDetail("❌ Error accessing SFTP server: " + e.getMessage());
            log.error("Error accessing SFTP for MHA file content reader", e);
        }

        return results;
    }
}

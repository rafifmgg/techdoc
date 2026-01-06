package com.ocmseservice.apiservice.workflows.uploadfile;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

@Component
@Slf4j
public class UploadFileCleanupScheduler {

    @Value("${upload.temp.directory:/temp/ocms/upload/hht}")
    private String tempUploadDirectory;
    
    /**
     * Scheduled job to clean up temporary files older than 7 days
     * Runs every Monday at 1:00 AM as specified in the workflow diagram
     */
    @Scheduled(cron = "0 0 1 ? * MON") // Run at 1:00 AM every Monday
    public void cleanupTemporaryFiles() {
        log.info("Starting scheduled cleanup of temporary files");
        
        try {
            Path directory = Paths.get(tempUploadDirectory);
            
            // Check if directory exists
            if (!Files.exists(directory)) {
                log.info("Temporary directory does not exist: {}", tempUploadDirectory);
                return;
            }
            
            // Calculate the cutoff date (7 days ago)
            LocalDateTime cutoffDate = LocalDateTime.now().minus(7, ChronoUnit.DAYS);
            log.info("Deleting files older than: {}", cutoffDate);
            
            // Walk through all timestamp directories in the temp folder
            try (Stream<Path> paths = Files.walk(directory, 1)) {
                paths.filter(Files::isDirectory)
                     .filter(path -> !path.equals(directory)) // Skip the root directory
                     .forEach(timestampDir -> {
                         String dirName = timestampDir.getFileName().toString();
                         
                         // Try to parse the timestamp directory name
                         try {
                             // If the directory name is a valid timestamp (yyyyMMddHHmmss)
                             if (dirName.matches("\\d{14}")) {
                                 int year = Integer.parseInt(dirName.substring(0, 4));
                                 int month = Integer.parseInt(dirName.substring(4, 6));
                                 int day = Integer.parseInt(dirName.substring(6, 8));
                                 int hour = Integer.parseInt(dirName.substring(8, 10));
                                 int minute = Integer.parseInt(dirName.substring(10, 12));
                                 int second = Integer.parseInt(dirName.substring(12, 14));
                                 
                                 LocalDateTime dirDate = LocalDateTime.of(year, month, day, hour, minute, second);
                                 
                                 // If directory is older than cutoff date, delete it and its contents
                                 if (dirDate.isBefore(cutoffDate)) {
                                     log.info("Deleting old directory: {}", timestampDir);
                                     deleteDirectoryRecursively(timestampDir);
                                 }
                             }
                         } catch (Exception e) {
                             log.warn("Failed to process directory: {}", dirName, e);
                         }
                     });
            }
            
            log.info("Completed scheduled cleanup of temporary files");
            
        } catch (IOException e) {
            log.error("Error during temporary file cleanup", e);
        }
    }
    
    /**
     * Recursively delete a directory and all its contents
     * 
     * @param directory Directory to delete
     * @throws IOException If deletion fails
     */
    private void deleteDirectoryRecursively(Path directory) throws IOException {
        if (Files.isDirectory(directory)) {
            try (Stream<Path> paths = Files.walk(directory)) {
                // Delete files first, then directories (in reverse order)
                paths.sorted((a, b) -> -a.compareTo(b))
                     .forEach(path -> {
                         try {
                             Files.delete(path);
                         } catch (IOException e) {
                             log.error("Failed to delete: {}", path, e);
                         }
                     });
            }
        }
    }
}

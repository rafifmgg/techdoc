package com.ocmsintranet.cronservice.framework.utilities;

import lombok.extern.slf4j.Slf4j;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for converting between string and byte array representations of file content.
 * This is used to ensure that file content is stored as byte arrays in the database
 * but can be displayed as human-readable strings when needed.
 */
@Slf4j
public class FileContentConverter {

    /**
     * Converts byte array content to a string using UTF-8 encoding.
     * 
     * @param content The byte array content to convert
     * @return The content as a string, or null if the input is null
     */
    public static String bytesToString(byte[] content) {
        if (content == null) {
            return null;
        }
        return new String(content, StandardCharsets.UTF_8);
    }

    /**
     * Converts string content to a byte array using UTF-8 encoding.
     * 
     * @param content The string content to convert
     * @return The content as a byte array, or null if the input is null
     */
    public static byte[] stringToBytes(String content) {
        if (content == null) {
            return null;
        }
        return content.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Logs the content of a byte array as a string for debugging purposes.
     * This is useful for viewing the human-readable content of files.
     * 
     * @param content The byte array content to log
     * @param label A label to identify the content in the log
     */
    public static void logContentAsString(byte[] content, String label) {
        if (content == null) {
            log.debug("{}: null", label);
            return;
        }
        
        String stringContent = bytesToString(content);
        log.debug("{} (length: {}): {}", label, content.length, 
                stringContent.length() > 500 ? stringContent.substring(0, 500) + "..." : stringContent);
    }
}

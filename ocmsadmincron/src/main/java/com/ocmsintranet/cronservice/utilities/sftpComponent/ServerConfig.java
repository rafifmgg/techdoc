package com.ocmsintranet.cronservice.utilities.sftpComponent;

/**
 * Configuration class for SFTP server details.
 * Used by SftpUtil to store server connection information.
 */
public class ServerConfig {
    private String host;
    private int port;
    private String username;
    private String secretName;

    public ServerConfig(String host, int port, String username, String secretName) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.secretName = secretName;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getSecretName() {
        return secretName;
    }

    @Override
    public String toString() {
        return String.format("%s@%s:%d", username, host, port);
    }
}

package com.ocmseservice.apiservice.utilities.logging;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.specialized.AppendBlobClient;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Appends log events to Azure Storage Blob.
 */
@Plugin(name = "AzureBlobAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class AzureBlobAppender extends AbstractAppender {

    private static final TokenCredential credential = new ManagedIdentityCredentialBuilder().maxRetry(3).build();

    private SizeBasedTriggeringPolicy triggeringPolicy; // Rolling logs implemention is included

    private final BlobContainerClient _container;
    private final String _prefix1;
    private final String _prefix2;

    private static final TimeZone AZURE_TIMEZONE = TimeZone.getTimeZone("Asia/Jakarta");

    String currentFile;
    AtomicLong currentSessionLogsLength = new AtomicLong(0);
    String currentDate;
    String currentHour;

    protected AzureBlobAppender(
            String name,
            Filter filter,
            Layout<? extends Serializable> layout,
            final boolean ignoreExceptions,
            final Property[] properties,
            String endpoint,
            String prefix1,
            String prefix2,
            SizeBasedTriggeringPolicy triggeringPolicy) {

        super(name, filter, layout, ignoreExceptions, properties);

        _container = new BlobContainerClientBuilder()
                .endpoint(endpoint)
                .credential(credential)
                .buildClient();

        if (!_container.exists()) {
            _container.create();
        }

        this.triggeringPolicy = triggeringPolicy;
        _prefix1 = prefix1;
        _prefix2 = prefix2;
    }

    @Override
    public void append(LogEvent event) {
        try {
            checkRollingPolicy(event);
            String name = getCurrentBlobName(_prefix1, _prefix2);
            AppendBlobClient append = _container.getBlobClient(name).getAppendBlobClient();

            if (!append.exists()) {
                append.create();
            }

            byte[] bytes = getLayout().toByteArray(event);
            currentSessionLogsLength.getAndAdd(bytes.length);

            try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
                append.appendBlock(in, bytes.length);
            }
        } catch (IOException e) {
            if (!ignoreExceptions()) {
                throw new LogAppenderException(e);
            }
        }
    }

    private String getInstanceId() {
        // Try Azure WebApp instance
        String instanceId = System.getenv("WEBSITE_INSTANCE_ID");

        // Try Azure VM/VMSS
        if (instanceId == null) {
            instanceId = System.getenv("COMPUTERNAME");
        }

        // Fallback to random ID if not running in Azure
        if (instanceId == null) {
            instanceId = UUID.randomUUID().toString().substring(0, 8);
        }

        return instanceId;
    }

    private String getCurrentBlobName(String _prefix1, String _prefix2) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd/HH");
        dateFormat.setTimeZone(AZURE_TIMEZONE);
        String formattedDate = dateFormat.format(new Date());

        if (currentFile == null) {
            String instanceId = getInstanceId();
            if (StringUtils.isEmpty(_prefix2)) {
                currentFile = String.format("%s/%s_%s.txt", formattedDate, _prefix1, instanceId);
            } else {
                currentFile = String.format("%s/%s/%s_%s.txt", _prefix1, formattedDate, _prefix2, instanceId);
            }
        }

        return currentFile;
    }

    private void checkRollingPolicy(LogEvent event) {
        String currentHourFormat = new SimpleDateFormat("yyyy/MM/dd/HH").format(new Date());

        boolean isHourMatch = currentHour != null && currentHour.equals(currentHourFormat);
        if (currentSessionLogsLength.get() + getLayout().toByteArray(event).length >= triggeringPolicy.getMaxFileSize()
                ||
                !isHourMatch) {
            currentFile = null;
            currentSessionLogsLength.set(0L);
            currentHour = currentHourFormat;
        }
    }

    /**
     * Create AzureBlobAppender.
     *
     * @param name    The name of the Appender.
     * @param prefix1 Specify directory structure. It becomes effective when WebApps
     *                is false.
     * @param prefix2 Specify directory structure. It becomes effective when WebApps
     *                is false. Can be null, empty or unset.
     * @param layout  The layout to format the message.
     * @param filter  The filter to filter the message.
     * @return AzureBlobAppender instance.
     */
    @PluginFactory
    public static AzureBlobAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginAttribute("prefix1") String prefix1,
            @PluginAttribute("prefix2") String prefix2,
            @PluginAttribute("endpoint") String endpoint,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter,
            @PluginElement("SizeBasedTriggeringPolicy") SizeBasedTriggeringPolicy triggeringPolicy) {

        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }

        try {
            return new AzureBlobAppender(name, filter, layout, true, Property.EMPTY_ARRAY, endpoint, prefix1, prefix2,
                    triggeringPolicy);
        } catch (Exception e) {
            throw new AppenderLoggingException(" is invalid.", e);
        }
    }
}
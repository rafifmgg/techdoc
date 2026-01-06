package com.ocmsintranet.cronservice.utilities.logging;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.specialized.AppendBlobClient;
import com.azure.storage.common.policy.RequestRetryOptions;
import com.azure.storage.common.policy.RetryPolicyType;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import java.util.concurrent.atomic.AtomicLong;

/**
 * Appends log events to Azure Storage Blob.
 */
@Plugin(name = "AzureBlobAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class AzureBlobAppender extends AbstractAppender {

    private static final Logger LOGGER = LogManager.getLogger(AzureBlobAppender.class);
    private static final TokenCredential credential = new DefaultAzureCredentialBuilder().maxRetry(3).build();

    private SizeBasedTriggeringPolicy triggeringPolicy;

    private final BlobContainerClient _container;
    private final String _prefix1;
    private final String _prefix2;

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

        try {
            _container = new BlobContainerClientBuilder()
                    .endpoint(endpoint)
                    .credential(credential)
                    .retryOptions(new RequestRetryOptions(RetryPolicyType.EXPONENTIAL, 3, 30, null, null, null))
                    .buildClient();

            if(!_container.exists()){
                _container.create();
            }
            
            LOGGER.info("u2705 Azure Blob Container initialized: {}", endpoint);
        } catch (Exception e) {
            LOGGER.error("u274c Failed to initialize Azure Blob Container: {}", e.getMessage(), e);
            throw new RuntimeException("Azure Blob Appender initialization failed", e);
        }
        
        this.triggeringPolicy = triggeringPolicy;
        if (this.triggeringPolicy == null) {
            SizeBasedTriggeringPolicy defaultPolicy = SizeBasedTriggeringPolicy.createPolicy("7168KB");
            if (defaultPolicy != null) {
                defaultPolicy.start();
                this.triggeringPolicy = defaultPolicy;
            }
        }
        
        _prefix1 = prefix1 != null ? prefix1 : "default";
        _prefix2 = prefix2 != null ? prefix2 : "logs";
    }

    @Override
    public void append(LogEvent event) {
        if (event == null) {
            return;
        }
        
        try {
            checkRollingPolicy(event);
            String name = getCurrentBlobName(_prefix1, _prefix2);
            AppendBlobClient append = _container.getBlobClient(name).getAppendBlobClient();
            
            if(!append.exists()){
                append.create();
            }

            Layout<? extends Serializable> layout = getLayout();
            if (layout == null) {
                LOGGER.error("Layout is null in AzureBlobAppender");
                return;
            }

            byte[] bytes = layout.toByteArray(event);
            if (bytes == null || bytes.length == 0) {
                LOGGER.warn("Layout produced null or empty bytes for event");
                return;
            }

            currentSessionLogsLength.getAndAdd(bytes.length);

            try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
                append.appendBlock(in, bytes.length);
            }
            
        } catch (Exception e) {
            LOGGER.error("Error appending to Azure Blob: {}", e.getMessage());
            if (!ignoreExceptions()) {
                throw new AppenderLoggingException("Error appending to Azure Blob: " + e.getMessage(), e);
            }
        }
    }

    private String getCurrentBlobName(String prefix1, String prefix2) {
        String dfmt = new SimpleDateFormat("yyyy/MM/dd/HH").format(new Date());

        if (currentFile == null) {
            if (StringUtils.isEmpty(prefix2)) {
                currentFile = String.format("%s/%s.txt", dfmt, prefix1);
            } else {
                currentFile = String.format("%s/%s/%s.txt", prefix1, dfmt, prefix2);
            }
        }

        return currentFile;
    }

    private void checkRollingPolicy(LogEvent event) {
        String currentHourFormat = new SimpleDateFormat("yyyy/MM/dd/HH").format(new Date());

        boolean isHourMatch = currentHour != null && currentHour.equals(currentHourFormat);
        long maxFileSize = triggeringPolicy != null ? triggeringPolicy.getMaxFileSize() : 7168 * 1024;
        
        // Get layout and calculate event size
        Layout<? extends Serializable> layout = getLayout();
        long eventSize = 0;
        if (layout != null && event != null) {
            byte[] eventBytes = layout.toByteArray(event);
            eventSize = eventBytes != null ? eventBytes.length : 0;
        }
        
        if (currentSessionLogsLength.get() + eventSize >= maxFileSize || !isHourMatch) {
            currentFile = null;
            currentSessionLogsLength.set(0L);
            currentHour = currentHourFormat;
        }
    }

    @PluginFactory
    public static AzureBlobAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginAttribute("prefix1") String prefix1,
            @PluginAttribute("prefix2") String prefix2,
            @PluginAttribute("endpoint") String endpoint,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter,
            @PluginElement("SizeBasedTriggeringPolicy") SizeBasedTriggeringPolicy triggeringPolicy) {

        // Validate required parameters
        if (StringUtils.isEmpty(name)) {
            LOGGER.error("AzureBlobAppender requires a name");
            return null;
        }

        if (StringUtils.isEmpty(endpoint)) {
            LOGGER.error("AzureBlobAppender requires an endpoint");
            return null;
        }

        // Ensure we have a valid layout
        if (layout == null) {
            LOGGER.info("No layout specified for AzureBlobAppender, creating default PatternLayout");
            layout = PatternLayout.newBuilder()
                    .withPattern("[%-5level] %d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %c{1} - %msg%n")
                    .build();
        }

        // Create default triggering policy if none provided
        if (triggeringPolicy == null) {
            triggeringPolicy = SizeBasedTriggeringPolicy.createPolicy("7168KB");
            if (triggeringPolicy != null) {
                triggeringPolicy.start();
            }
        }

        try {
            return new AzureBlobAppender(name, filter, layout, true, Property.EMPTY_ARRAY, 
                                       endpoint, prefix1, prefix2, triggeringPolicy);
        } catch (Exception e) {
            LOGGER.error("AzureBlobAppender creation failed: {}", e.getMessage(), e);
            return null;
        }
    }
}

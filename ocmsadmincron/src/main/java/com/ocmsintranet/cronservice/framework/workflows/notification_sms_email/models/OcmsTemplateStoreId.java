package com.ocmsintranet.cronservice.framework.workflows.notification_sms_email.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite primary key class for OcmsTemplateStore entity.
 * Combines template_name and version to allow multiple versions of the same template.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcmsTemplateStoreId implements Serializable {

    private String templateName;
    private Integer version;
}

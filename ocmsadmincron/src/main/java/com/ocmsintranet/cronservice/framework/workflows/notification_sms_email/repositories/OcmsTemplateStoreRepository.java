package com.ocmsintranet.cronservice.framework.workflows.notification_sms_email.repositories;

import com.ocmsintranet.cronservice.framework.workflows.notification_sms_email.models.OcmsTemplateStore;
import com.ocmsintranet.cronservice.framework.workflows.notification_sms_email.models.OcmsTemplateStoreId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for accessing OcmsTemplateStore entity.
 * Provides methods to fetch templates by name from the database.
 * Supports versioning - fetches the latest active version of each template.
 */
@Repository
public interface OcmsTemplateStoreRepository extends JpaRepository<OcmsTemplateStore, OcmsTemplateStoreId> {

    /**
     * Find the latest active version of a template by template name.
     * Returns the template with the highest version number where is_active = true.
     *
     * @param templateName The template name to search for
     * @return Optional containing the latest active template version if found
     */
    @Query("SELECT t FROM OcmsTemplateStore t " +
           "WHERE t.templateName = :templateName " +
           "AND t.isActive = true " +
           "ORDER BY t.version DESC " +
           "LIMIT 1")
    Optional<OcmsTemplateStore> findLatestActiveByTemplateName(@Param("templateName") String templateName);

    /**
     * Find specific version of a template by template name and version.
     *
     * @param templateName The template name
     * @param version The version number
     * @return Optional containing the template if found
     */
    Optional<OcmsTemplateStore> findByTemplateNameAndVersion(String templateName, Integer version);
}

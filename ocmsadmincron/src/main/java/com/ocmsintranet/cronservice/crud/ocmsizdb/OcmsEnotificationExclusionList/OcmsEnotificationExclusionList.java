package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsEnotificationExclusionList;

import com.ocmsintranet.cronservice.crud.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing the ocms_enotification_exclusion_list table.
 * This table manages an exclusion list to ensure that opted-out users do not receive notifications.
 * It tracks details such as the ID number, creation date, and user who created the opt-out record.
 */
@Entity
@Table(name = "ocms_enotification_exclusion_list", schema = "ocmsizmgr")
@Getter
@Setter
public class OcmsEnotificationExclusionList extends BaseEntity {

    @Id
    @Column(name = "id_no", length = 20, nullable = false)
    private String idNo;

    @Column(name = "remarks", length = 200)
    private String remarks;
    
    // Note: Audit fields (creDate, creUserId, updDate, updUserId) are inherited from BaseEntity
}

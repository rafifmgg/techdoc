package com.ocmsintranet.apiservice.crud.ocmsizdb.enotificationexclusionhistory;

import com.ocmsintranet.apiservice.crud.BaseEntity;
import com.ocmsintranet.apiservice.crud.annotations.NumberAble;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ocms_enotification_exclusion_history", schema = "ocmsizmgr")
@Setter
@Getter
@NoArgsConstructor
public class EnotificationExclusionHistory extends BaseEntity {
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_DELETE = "DELETE";

    @NotBlank
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @NumberAble
    @Column(name = "enotification_history_id", nullable = false)
    private Integer enotificationHistoryId;

    @Column(name = "action", nullable = false, length = 8)
    private String action;

    @Column(name = "id_no", nullable = false, length = 20)
    private String idNo;

    @Column(name = "remarks", length = 200)
    private String remarks;
}

package com.ocmsintranet.apiservice.crud.ocmsizdb.parameterhistory;

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
@Table(name = "ocms_parameter_history")
@Setter
@Getter
@NoArgsConstructor
public class ParameterHistory extends BaseEntity {
    public static final String ACTION_UPDATE = "UPDATE";

    @Column(name = "parameter_history_id", nullable = false)
    @NotBlank
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @NumberAble
    private Integer parameterHistoryId;

    @Column(name = "code", length = 20)
    private String code;

    @Column(name = "action", length = 8)
    private String action;

    @Column(name = "parameter_id", length = 20)
    private String parameterId;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "value", length = 200)
    private String value;
}

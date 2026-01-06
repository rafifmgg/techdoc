package com.ocmsintranet.apiservice.crud.ocmsizdb.parameter;

import com.ocmsintranet.apiservice.crud.BaseEntity;
import com.ocmsintranet.apiservice.crud.annotations.NonEditable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ocms_parameter", schema = "ocmsizmgr")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ParameterId.class)
public class Parameter extends BaseEntity {

    @Column(name = "parameter_id", length = 20, nullable = false)
    @Id
    @NotBlank
    @NonEditable
    private String parameterId;

    @Column(name = "code", nullable = false, length = 20)
    @NotBlank
    @NonEditable
    @Id
    private String code; // RD1

    @Column(name = "description", nullable = false, length = 200)
    @NotBlank
    private String description;

    @Column(name = "value", nullable = false, length = 200)
    @NotBlank
    private String value; // date of letter
}
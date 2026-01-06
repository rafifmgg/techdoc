package com.ocmsintranet.apiservice.crud.ocmsizdb.standardcode;

import com.ocmsintranet.apiservice.crud.BaseEntity;
import com.ocmsintranet.apiservice.crud.annotations.FindAble;
import com.ocmsintranet.apiservice.crud.annotations.InAble;
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
@Table(name = "ocms_standard_code", schema = "ocmsizmgr")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(StandardCodeId.class)
public class StandardCode extends BaseEntity {

    // public static final String C_CODE = "code";
    // public static final String C_REFERENCE_CODE = "referenceCode";
    // public static final String C_CODE_STATUS = "codeStatus";

    @Column(name = "reference_code", length = 20, nullable = false)
    @NotBlank
    @FindAble
    @InAble
    @NonEditable
    @Id
    private String referenceCode;

    @Column(name = "code", length = 20, nullable = false)
    @NotBlank
    @Id
    @FindAble
    @NonEditable
    private String code;

    @Column(name = "description", length = 200, nullable = false)
    @NotBlank
    @FindAble
    @InAble
    private String description;

    @Column(name = "code_status", length = 1, nullable = false)
    @NotBlank
    @FindAble
    @InAble
    private String codeStatus;
}

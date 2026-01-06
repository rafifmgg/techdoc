package com.ocmseservice.apiservice.crud.ocmsezdb.eocmsusermessage;

import com.ocmseservice.apiservice.crud.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "eocms_user_message", schema = "ocmsezmgr")
@Getter
@Setter
public class EocmsUserMessage extends BaseEntity {
    @Id
    @Column(name = "error_code", length = 10, nullable = false)
    private String errorCode;

    @Column(name = "error_message", length = 255, nullable = false)
    private String errorMessage;
}

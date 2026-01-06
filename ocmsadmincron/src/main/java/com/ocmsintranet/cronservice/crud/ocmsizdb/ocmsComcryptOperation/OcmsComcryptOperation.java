package com.ocmsintranet.cronservice.crud.ocmsizdb.ocmsComcryptOperation;

import com.ocmsintranet.cronservice.crud.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing the ocms_comcrypt_operation table.
 * Stores information about cryptographic operations performed by the system.
 * 
 * Primary Key: requestId
 */
@Entity
@Table(name = "ocms_comcrypt_operation", schema = "ocmsizmgr")
@Getter
@Setter
public class OcmsComcryptOperation extends BaseEntity {
    @Id
    @Column(name = "request_id", length = 50, nullable = false)
    private String requestId;

    @Column(name = "operation_type", length = 20, nullable = false)
    private String operationType;

    @Column(name = "file_name", length = 255, nullable = false)
    private String fileName;

    @Column(name = "token", length = 100)
    private String token;
}
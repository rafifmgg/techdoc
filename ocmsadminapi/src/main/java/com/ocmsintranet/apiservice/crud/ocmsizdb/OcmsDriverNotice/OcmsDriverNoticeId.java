package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsDriverNotice;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Composite primary key for OcmsDriverNotice entity
 * Primary Key: (dateOfProcessing, noticeNo)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class OcmsDriverNoticeId implements Serializable {
    
    private LocalDateTime dateOfProcessing;
    private String noticeNo;
}
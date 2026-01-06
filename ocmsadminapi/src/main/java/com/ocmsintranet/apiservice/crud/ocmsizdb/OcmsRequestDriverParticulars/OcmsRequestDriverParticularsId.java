package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsRequestDriverParticulars;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Composite primary key for OcmsRequestDriverParticulars entity
 * Primary Key: (dateOfProcessing, noticeNo)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class OcmsRequestDriverParticularsId implements Serializable {
    
    private LocalDateTime dateOfProcessing;
    private String noticeNo;
}
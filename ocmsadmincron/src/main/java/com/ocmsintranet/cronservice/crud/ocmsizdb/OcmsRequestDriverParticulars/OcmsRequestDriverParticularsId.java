package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsRequestDriverParticulars;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Composite primary key for OcmsRequestDriverParticulars entity
 * Primary Key: (dateOfProcessing, noticeNo)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcmsRequestDriverParticularsId implements Serializable {
    
    private LocalDateTime dateOfProcessing;
    private String noticeNo;
}
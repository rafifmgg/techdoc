package com.ocmseservice.apiservice.crud.ocmsezdb.eocmsrequestdriverparticulars;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EocmsRequestDriverParticularsId implements Serializable {
    @Column(name = "date_of_processing", nullable = false)
    private LocalDateTime dateOfProcessing;
    
    @Column(name = "notice_no", length = 10, nullable = false)
    private String noticeNo;
}

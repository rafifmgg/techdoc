package com.ocmseservice.apiservice.crud.ocmsezdb.eocmswebtxndetail;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EocmsWebTxnDetailId implements Serializable {

    @Column(name = "receipt_no", length = 16, nullable = false)
    private String receiptNo;

    @Column(name = "offence_notice_no", length = 10)
    private String offenceNoticeNo;
}


package com.ocmsintranet.cronservice.crud.ocmsezdb.webtxndetail;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite primary key class for EwebTxnDetail entity.
 * Primary key consists of receipt_no and offence_notice_no.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EwebTxnDetailId implements Serializable {

    private String receiptNo;
    private String offenceNoticeNo;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EwebTxnDetailId that = (EwebTxnDetailId) o;

        if (receiptNo != null ? !receiptNo.equals(that.receiptNo) : that.receiptNo != null) return false;
        return offenceNoticeNo != null ? offenceNoticeNo.equals(that.offenceNoticeNo) : that.offenceNoticeNo == null;
    }

    @Override
    public int hashCode() {
        int result = receiptNo != null ? receiptNo.hashCode() : 0;
        result = 31 * result + (offenceNoticeNo != null ? offenceNoticeNo.hashCode() : 0);
        return result;
    }
}

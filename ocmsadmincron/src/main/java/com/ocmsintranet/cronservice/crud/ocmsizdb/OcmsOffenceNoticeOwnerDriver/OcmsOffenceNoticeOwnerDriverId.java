package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite key class for OcmsOffenceNoticeOwnerDriver entity.
 * The primary key consists of notice_no and owner_driver_indicator.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcmsOffenceNoticeOwnerDriverId implements Serializable {
    
    private String noticeNo;
    private String ownerDriverIndicator;
    
    // Important: Override equals and hashCode for composite keys
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OcmsOffenceNoticeOwnerDriverId that = (OcmsOffenceNoticeOwnerDriverId) o;
        return Objects.equals(noticeNo, that.noticeNo) && 
               Objects.equals(ownerDriverIndicator, that.ownerDriverIndicator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(noticeNo, ownerDriverIndicator);
    }
    
    @Override
    public String toString() {
        return "OcmsOffenceNoticeOwnerDriverId{" +
                "noticeNo='" + noticeNo + '\'' +
                ", ownerDriverIndicator='" + ownerDriverIndicator + '\'' +
                '}';
    }
}
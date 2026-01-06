package com.ocmsintranet.apiservice.workflows.notice_management.ownerdriver.dto;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.dto.OffenceNoticeOwnerDriverWithAddressDto;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OffenceNoticeOwnerDriverPlusDto {
    private String noticeNo;
    private String ownerDriverIndicator;
    private String idNo;
    private String idType;
    private String name;
    private String emailAddr;
    private String offenderTelNo;
    private String offenderIndicator;
    private String regPostalCode;
    private String regBldgName;
    private String regStreetName;
    private String regBlkHseNo;
    private String regFloorNo;
    private String regUnitNo;
    private String mailPostalCode;
    private String mailBldgName;
    private String mailStreetName;
    private String mailBlkHseNo;
    private String mailFloorNo;
    private String mailUnitNo;
    
    // Constructor from OffenceNoticeOwnerDriverWithAddressDto
    public OffenceNoticeOwnerDriverPlusDto(OffenceNoticeOwnerDriverWithAddressDto source) {
        this.noticeNo = source.getOwner().getNoticeNo();
        this.ownerDriverIndicator = source.getOwner().getOwnerDriverIndicator();
        this.idNo = source.getOwner().getIdNo();
        this.idType = source.getOwner().getIdType();
        this.name = source.getOwner().getName();
        this.emailAddr = source.getOwner().getEmailAddr();
        this.offenderTelNo = source.getOwner().getOffenderTelNo();
        this.offenderIndicator = source.getOwner().getOffenderIndicator();
        
        // Map address fields (using mha_reg for reg* and furnished_mail for mail*)
        this.regPostalCode = source.getRegPostalCode();
        this.regBldgName = source.getRegBldgName();
        this.regStreetName = source.getRegStreetName();
        this.regBlkHseNo = source.getRegBlkHseNo();
        this.regFloorNo = source.getRegFloorNo();
        this.regUnitNo = source.getRegUnitNo();
        this.mailPostalCode = source.getMailPostalCode();
        this.mailBldgName = source.getMailBldgName();
        this.mailStreetName = source.getMailStreetName();
        this.mailBlkHseNo = source.getMailBlkHseNo();
        this.mailFloorNo = source.getMailFloorNo();
        this.mailUnitNo = source.getMailUnitNo();
    }
}

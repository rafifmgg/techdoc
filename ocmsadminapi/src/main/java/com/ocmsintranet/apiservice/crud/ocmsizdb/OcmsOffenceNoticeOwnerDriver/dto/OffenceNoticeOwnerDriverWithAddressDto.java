package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriver;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriverAddr.OcmsOffenceNoticeOwnerDriverAddr;

/**
 * Flattened DTO combining owner/driver entity fields and up to four address type blocks.
 * Address blocks are flattened with prefixes: ltaReg, ltaMail, reg, mail.
 */
@Data
@NoArgsConstructor
public class OffenceNoticeOwnerDriverWithAddressDto {

    // Flatten all fields from owner/driver entity at top-level
    @JsonUnwrapped
    private OcmsOffenceNoticeOwnerDriver owner;

    // ltaReg fields
    private String ltaRegBldgName;
    private String ltaRegBlkHseNo;
    private String ltaRegStreetName;
    private String ltaRegFloorNo;
    private String ltaRegUnitNo;
    private String ltaRegPostalCode;
    private String ltaRegAddressType;
    private String ltaRegInvalidAddrTag;
    private String ltaErrorCode;
    private LocalDateTime ltaRegEffectiveDate;
    private LocalDateTime ltaProcessingDateTime;
    private LocalDateTime ltaRegCreDate;
    private String ltaRegCreUserId;
    private LocalDateTime ltaRegUpdDate;
    private String ltaRegUpdUserId;

    // ltaMail fields
    private String ltaMailBldgName;
    private String ltaMailBlkHseNo;
    private String ltaMailStreetName;
    private String ltaMailFloorNo;
    private String ltaMailUnitNo;
    private String ltaMailPostalCode;
    private String ltaMailAddressType;
    private String ltaMailInvalidAddrTag;
    private String ltaMailErrorCode;
    private LocalDateTime ltaMailEffectiveDate;
    private LocalDateTime ltaMailProcessingDateTime;
    private LocalDateTime ltaMailCreDate;
    private String ltaMailCreUserId;
    private LocalDateTime ltaMailUpdDate;
    private String ltaMailUpdUserId;

    // reg fields (mha_reg)
    private String regBldgName;
    private String regBlkHseNo;
    private String regStreetName;
    private String regFloorNo;
    private String regUnitNo;
    private String regPostalCode;
    private String regAddressType;
    private String regInvalidAddrTag;
    private String regErrorCode;
    private LocalDateTime regEffectiveDate;
    private LocalDateTime regProcessingDateTime;
    private LocalDateTime regCreDate;
    private String regCreUserId;
    private LocalDateTime regUpdDate;
    private String regUpdUserId;

    // mail fields (furnished_mail)
    private String mailBldgName;
    private String mailBlkHseNo;
    private String mailStreetName;
    private String mailFloorNo;
    private String mailUnitNo;
    private String mailPostalCode;
    private String mailAddressType;
    private String mailInvalidAddrTag;
    private String mailErrorCode;
    private LocalDateTime mailEffectiveDate;
    private LocalDateTime mailProcessingDateTime;
    private LocalDateTime mailCreDate;
    private String mailCreUserId;
    private LocalDateTime mailUpdDate;
    private String mailUpdUserId;

    public static OffenceNoticeOwnerDriverWithAddressDto from(
            OcmsOffenceNoticeOwnerDriver owner,
            Map<String, OcmsOffenceNoticeOwnerDriverAddr> byType) {
        OffenceNoticeOwnerDriverWithAddressDto dto = new OffenceNoticeOwnerDriverWithAddressDto();
        dto.setOwner(owner);
        // fill each known type if present
        dto.fillFromAddress("lta_reg", byType.get("lta_reg"));
        dto.fillFromAddress("lta_mail", byType.get("lta_mail"));
        dto.fillFromAddress("mha_reg", byType.get("mha_reg"));
        dto.fillFromAddress("furnished_mail", byType.get("furnished_mail"));
        return dto;
    }

    private void fillFromAddress(String prefix, OcmsOffenceNoticeOwnerDriverAddr a) {
        if (a == null) return;
        switch (prefix) {
            case "lta_reg" -> {
                this.ltaRegBldgName = a.getBldgName();
                this.ltaRegBlkHseNo = a.getBlkHseNo();
                this.ltaRegStreetName = a.getStreetName();
                this.ltaRegFloorNo = a.getFloorNo();
                this.ltaRegUnitNo = a.getUnitNo();
                this.ltaRegPostalCode = a.getPostalCode();
                this.ltaRegAddressType = a.getAddressType();
                this.ltaRegInvalidAddrTag = a.getInvalidAddrTag();
                this.ltaErrorCode = a.getErrorCode();
                this.ltaRegEffectiveDate = a.getEffectiveDate();
                this.ltaProcessingDateTime = a.getProcessingDateTime();
                this.ltaRegCreDate = a.getCreDate();
                this.ltaRegCreUserId = a.getCreUserId();
                this.ltaRegUpdDate = a.getUpdDate();
                this.ltaRegUpdUserId = a.getUpdUserId();
            }
            case "lta_mail" -> {
                this.ltaMailBldgName = a.getBldgName();
                this.ltaMailBlkHseNo = a.getBlkHseNo();
                this.ltaMailStreetName = a.getStreetName();
                this.ltaMailFloorNo = a.getFloorNo();
                this.ltaMailUnitNo = a.getUnitNo();
                this.ltaMailPostalCode = a.getPostalCode();
                this.ltaMailAddressType = a.getAddressType();
                this.ltaMailInvalidAddrTag = a.getInvalidAddrTag();
                this.ltaMailEffectiveDate = a.getEffectiveDate();
                this.ltaMailCreDate = a.getCreDate();
                this.ltaMailCreUserId = a.getCreUserId();
                this.ltaMailUpdDate = a.getUpdDate();
                this.ltaMailUpdUserId = a.getUpdUserId();
            }
            case "mha_reg" -> {
                this.regBldgName = a.getBldgName();
                this.regBlkHseNo = a.getBlkHseNo();
                this.regStreetName = a.getStreetName();
                this.regFloorNo = a.getFloorNo();
                this.regUnitNo = a.getUnitNo();
                this.regPostalCode = a.getPostalCode();
                this.regAddressType = a.getAddressType();
                this.regInvalidAddrTag = a.getInvalidAddrTag();
                this.regEffectiveDate = a.getEffectiveDate();
                this.regCreDate = a.getCreDate();
                this.regCreUserId = a.getCreUserId();
                this.regUpdDate = a.getUpdDate();
                this.regUpdUserId = a.getUpdUserId();
            }
            case "furnished_mail" -> {
                this.mailBldgName = a.getBldgName();
                this.mailBlkHseNo = a.getBlkHseNo();
                this.mailStreetName = a.getStreetName();
                this.mailFloorNo = a.getFloorNo();
                this.mailUnitNo = a.getUnitNo();
                this.mailPostalCode = a.getPostalCode();
                this.mailAddressType = a.getAddressType();
                this.mailInvalidAddrTag = a.getInvalidAddrTag();
                this.mailEffectiveDate = a.getEffectiveDate();
                this.mailCreDate = a.getCreDate();
                this.mailCreUserId = a.getCreUserId();
                this.mailUpdDate = a.getUpdDate();
                this.mailUpdUserId = a.getUpdUserId();
            }
            default -> {}
        }
    }
}

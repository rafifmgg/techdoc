package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Composite ID class for OcmsOffenceNoticeOwnerDriver entity
 */
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class OcmsOffenceNoticeOwnerDriverId implements Serializable {

   private String noticeNo;
   private String ownerDriverIndicator;
    
    // Equals and hashCode methods are provided by Lombok's @Data
}
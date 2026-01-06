package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsSuspensionReason;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class OcmsSuspensionReasonId implements Serializable {
    private String suspensionType;
    private String reasonOfSuspension;
}

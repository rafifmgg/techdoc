package com.ocmsintranet.apiservice.crud.ocmsizdb.suspensionreason;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SuspensionReasonId implements Serializable {
  private String suspensionType;
  private String reasonOfSuspension;
}

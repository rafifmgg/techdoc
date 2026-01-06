package com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SuspendedNoticeId implements Serializable {
  private String noticeNo;
  private LocalDateTime dateOfSuspension;
  private Integer srNo;
}

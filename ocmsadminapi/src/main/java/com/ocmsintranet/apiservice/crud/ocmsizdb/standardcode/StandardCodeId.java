package com.ocmsintranet.apiservice.crud.ocmsizdb.standardcode;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class StandardCodeId implements Serializable {
  private String referenceCode;
  private String code;
}

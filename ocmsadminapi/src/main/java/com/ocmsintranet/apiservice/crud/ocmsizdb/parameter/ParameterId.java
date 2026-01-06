package com.ocmsintranet.apiservice.crud.ocmsizdb.parameter;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ParameterId implements Serializable {
  private String parameterId;
  private String code;
  
}

package com.ocmsintranet.apiservice.crud.ocmsizdb.referenceCode;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ocmsintranet.apiservice.crud.beans.ReferenceCode;

@RestController
@RequestMapping("/standardcode/${api.version}/getReferenceCode")
public class ReferenceCodeController {
    @Autowired 
    ReferenceCodeService service;
  
    @GetMapping("")
    public ResponseEntity<Map<String, List<ReferenceCode>>> getReferenceCodeList() {
        List<String> codes = service.getReferenceCodeList();
        List<ReferenceCode> response = codes.stream()
            .map(code -> new ReferenceCode(code))
            .collect(Collectors.toList());
        
        return ResponseEntity.status(HttpStatus.OK)
            .body(Map.of("data", response));
    }
  
    public void setService(ReferenceCodeService s) {
        this.service = s;
    }
}
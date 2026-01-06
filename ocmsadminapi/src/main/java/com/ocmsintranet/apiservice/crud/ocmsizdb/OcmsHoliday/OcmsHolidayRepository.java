package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsHoliday;

import org.springframework.stereotype.Repository;

import com.ocmsintranet.apiservice.crud.BaseRepository;
import java.time.LocalDateTime;

@Repository
public interface OcmsHolidayRepository extends BaseRepository<OcmsHoliday, LocalDateTime>{

}

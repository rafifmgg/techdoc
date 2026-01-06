package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsHoliday;

import org.springframework.stereotype.Repository;

import com.ocmsintranet.cronservice.crud.BaseRepository;
import java.time.LocalDateTime;

@Repository
public interface OcmsHolidayRepository extends BaseRepository<OcmsHoliday, LocalDateTime>{

}

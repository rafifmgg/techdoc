package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsHoliday;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class OcmsHolidayServiceImpl
    extends BaseImplement<OcmsHoliday, LocalDateTime, OcmsHolidayRepository>
    implements OcmsHolidayService {

    public OcmsHolidayServiceImpl(OcmsHolidayRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
}

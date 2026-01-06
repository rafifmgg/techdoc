package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.dto.OffenceNoticeOwnerDriverWithAddressDto;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriverAddr.OcmsOffenceNoticeOwnerDriverAddr;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriverAddr.OcmsOffenceNoticeOwnerDriverAddrService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service implementation for OcmsOffenceNoticeOwnerDriver entities
 */
@Service
public class OcmsOffenceNoticeOwnerDriverServiceImpl extends BaseImplement<OcmsOffenceNoticeOwnerDriver, OcmsOffenceNoticeOwnerDriverId, OcmsOffenceNoticeOwnerDriverRepository> 
        implements OcmsOffenceNoticeOwnerDriverService {
    
    private final OcmsOffenceNoticeOwnerDriverAddrService addressService;

    /**
     * Constructor with required dependencies
     */
    public OcmsOffenceNoticeOwnerDriverServiceImpl(OcmsOffenceNoticeOwnerDriverRepository repository, DatabaseRetryService retryService,
                                                   OcmsOffenceNoticeOwnerDriverAddrService addressService) {
        super(repository, retryService);
        this.addressService = addressService;
    }
    
    @Override
    @Transactional(readOnly = true)
    public FindAllResponse<OffenceNoticeOwnerDriverWithAddressDto> getAllWithAddresses(Map<String, String[]> params) {
        // Reuse base pagination/filtering; remove $field to ensure entity list (not map) from BaseImplement
        Map<String, String[]> effectiveParams = new HashMap<>(params != null ? params : Collections.emptyMap());
        effectiveParams.remove("$field");
        FindAllResponse<OcmsOffenceNoticeOwnerDriver> base = super.getAll(effectiveParams);
        List<OcmsOffenceNoticeOwnerDriver> owners = base.getData();
        if (owners == null || owners.isEmpty()) {
            return new FindAllResponse<>(base.getTotal(), base.getLimit(), base.getSkip(), Collections.emptyList());
        }

        // Collect keys for bulk fetch
        List<String> noticeNos = owners.stream().map(OcmsOffenceNoticeOwnerDriver::getNoticeNo).distinct().collect(Collectors.toList());
        List<String> indicators = owners.stream().map(OcmsOffenceNoticeOwnerDriver::getOwnerDriverIndicator).distinct().collect(Collectors.toList());
        List<String> types = Arrays.asList("lta_reg", "lta_mail", "mha_reg", "furnished_mail");

        List<OcmsOffenceNoticeOwnerDriverAddr> addrs = addressService
                .findByNoticeNoInAndOwnerDriverIndicatorInAndTypeOfAddressIn(noticeNos, indicators, types);

        // Group addresses by composite key (noticeNo|indicator) then by type
        Map<String, Map<String, OcmsOffenceNoticeOwnerDriverAddr>> byKeyThenType = addrs.stream()
                .collect(Collectors.groupingBy(
                        a -> key(a.getNoticeNo(), a.getOwnerDriverIndicator()),
                        Collectors.toMap(OcmsOffenceNoticeOwnerDriverAddr::getTypeOfAddress, a -> a, (a, b) -> a)
                ));

        List<OffenceNoticeOwnerDriverWithAddressDto> data = owners.stream()
                .map(o -> OffenceNoticeOwnerDriverWithAddressDto.from(o, byKeyThenType.getOrDefault(key(o.getNoticeNo(), o.getOwnerDriverIndicator()), Collections.<String, OcmsOffenceNoticeOwnerDriverAddr>emptyMap())))
                .collect(Collectors.toList());

        return new FindAllResponse<>(base.getTotal(), base.getLimit(), base.getSkip(), data);
    }

    private static String key(String noticeNo, String indicator) {
        return noticeNo + "|" + indicator;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OcmsOffenceNoticeOwnerDriver> findByNoticeNo(String noticeNo) {
        return repository.findByNoticeNo(noticeNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OcmsOffenceNoticeOwnerDriver> findByIdNo(String idNo) {
        return retryService.executeWithRetry(() -> repository.findByIdNo(idNo));
    }

}

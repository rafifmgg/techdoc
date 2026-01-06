package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeDetail;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for OcmsOffenceNoticeDetail entities
 */
@Service
public class OcmsOffenceNoticeDetailServiceImpl extends BaseImplement<OcmsOffenceNoticeDetail, String, OcmsOffenceNoticeDetailRepository> 
        implements OcmsOffenceNoticeDetailService {
    
    /**
     * Constructor with required dependencies
     */
    public OcmsOffenceNoticeDetailServiceImpl(OcmsOffenceNoticeDetailRepository repository, DatabaseRetryService retryService) {
        super(repository, retryService);
    }
    
    /**
     * Override the patch method to properly handle entity updates
     * This ensures that we only update the fields that are provided in the patch entity
     * and preserve existing values for fields that are not included in the patch
     */
    @Override
    @Transactional
    public OcmsOffenceNoticeDetail patch(String id, OcmsOffenceNoticeDetail patchEntity) {
        return retryService.executeWithRetry(() -> {
            // Find the existing entity
            OcmsOffenceNoticeDetail existingEntity = repository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Entity not found with ID: " + id));
            
            // Only update the fields that are not null in the patch entity
            // This preserves existing values for fields not included in the patch
            // if (patchEntity.getComments() != null) {
            //     existingEntity.setComments(patchEntity.getComments());
            // }
            if (patchEntity.getColorOfVehicle() != null) {
                existingEntity.setColorOfVehicle(patchEntity.getColorOfVehicle());
            }
            if (patchEntity.getConditionInvalidCoupon1() != null) {
                existingEntity.setConditionInvalidCoupon1(patchEntity.getConditionInvalidCoupon1());
            }
            if (patchEntity.getConditionInvalidCoupon2() != null) {
                existingEntity.setConditionInvalidCoupon2(patchEntity.getConditionInvalidCoupon2());
            }
            if (patchEntity.getConditionInvalidCoupon3() != null) {
                existingEntity.setConditionInvalidCoupon3(patchEntity.getConditionInvalidCoupon3());
            }
            if (patchEntity.getDenomInvalidCoupon1() != null) {
                existingEntity.setDenomInvalidCoupon1(patchEntity.getDenomInvalidCoupon1());
            }
            if (patchEntity.getDenomInvalidCoupon2() != null) {
                existingEntity.setDenomInvalidCoupon2(patchEntity.getDenomInvalidCoupon2());
            }
            if (patchEntity.getDenomInvalidCoupon3() != null) {
                existingEntity.setDenomInvalidCoupon3(patchEntity.getDenomInvalidCoupon3());
            }
            if (patchEntity.getDenomOfValidCoupon() != null) {
                existingEntity.setDenomOfValidCoupon(patchEntity.getDenomOfValidCoupon());
            }
            if (patchEntity.getExpiryTime() != null) {
                existingEntity.setExpiryTime(patchEntity.getExpiryTime());
            }
            // if (patchEntity.getInvalidCoupon1CreasedTab() != null) {
            //     existingEntity.setInvalidCoupon1CreasedTab(patchEntity.getInvalidCoupon1CreasedTab());
            // }
            // if (patchEntity.getInvalidCoupon1Subtype() != null) {
            //     existingEntity.setInvalidCoupon1Subtype(patchEntity.getInvalidCoupon1Subtype());
            // }
            // if (patchEntity.getInvalidCoupon2CreasedTab() != null) {
            //     existingEntity.setInvalidCoupon2CreasedTab(patchEntity.getInvalidCoupon2CreasedTab());
            // }
            // if (patchEntity.getInvalidCoupon2Subtype() != null) {
            //     existingEntity.setInvalidCoupon2Subtype(patchEntity.getInvalidCoupon2Subtype());
            // }
            // if (patchEntity.getInvalidCoupon3CreasedTab() != null) {
            //     existingEntity.setInvalidCoupon3CreasedTab(patchEntity.getInvalidCoupon3CreasedTab());
            // }
            if (patchEntity.getInvalidCouponNo1() != null) {
                existingEntity.setInvalidCouponNo1(patchEntity.getInvalidCouponNo1());
            }
            if (patchEntity.getInvalidCouponNo2() != null) {
                existingEntity.setInvalidCouponNo2(patchEntity.getInvalidCouponNo2());
            }
            if (patchEntity.getInvalidCouponNo3() != null) {
                existingEntity.setInvalidCouponNo3(patchEntity.getInvalidCouponNo3());
            }
            if (patchEntity.getVehicleMake() != null) {
                existingEntity.setVehicleMake(patchEntity.getVehicleMake());
            }
            if (patchEntity.getRepViolationCode() != null) {
                existingEntity.setRepViolationCode(patchEntity.getRepViolationCode());
            }
            
            // Save the updated entity
            return repository.save(existingEntity);
        });
    }
}
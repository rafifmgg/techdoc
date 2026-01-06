package com.ocmsintranet.apiservice.workflows.notice_management.retrieval.getnotice.mapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.dto.OffenceNoticeWithOwnerDto;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;

// import com.ocmsintranet.apiservice.workflows.notice_management.suspension.suspendednotice.dto.SuspendedNoticePlusDto;
import com.ocmsintranet.apiservice.workflows.notice_management.retrieval.getnotice.dto.PlusOffenceNoticeResponse;

/**
 * Mapper for converting between internal DTOs and PLUS API response DTOs
 */
@Component
public class NoticeResponseMapper {

    /**
     * Maps FindAllResponse with OffenceNoticeWithOwnerDto to PlusOffenceNoticeResponse
     *
     * @param response FindAllResponse containing OffenceNoticeWithOwnerDto objects
     * @param includeSuspendedInfo Whether to include suspended information
     * @param suspendedNoticesMap Map of notice numbers to suspended notices
     * @return PlusOffenceNoticeResponse formatted for PLUS API
     */
    // public PlusOffenceNoticeResponse plusOffenceNoticeResponse(
    //         FindAllResponse<OffenceNoticeWithOwnerDto> response,
    //         boolean includeSuspendedInfo,
    //         Map<String, List<SuspendedNoticePlusDto>> suspendedNoticesMap) {
    public PlusOffenceNoticeResponse plusOffenceNoticeResponse(
            FindAllResponse<OffenceNoticeWithOwnerDto> response,
            boolean includeSuspendedInfo) {

        PlusOffenceNoticeResponse plusResponse = new PlusOffenceNoticeResponse();
        plusResponse.setTotal(response.getTotal());
        plusResponse.setLimit(response.getLimit());
        plusResponse.setSkip(response.getSkip());

        // Map each DTO to the format expected in the PLUS API
        List<Map<String, Object>> data = response.getData().stream()
            // .map(dto -> mapOffenceNoticeToApiFormat(dto, includeSuspendedInfo, suspendedNoticesMap))
            .map(dto -> mapOffenceNoticeToApiFormat(dto, includeSuspendedInfo))
            .collect(Collectors.toList());

        plusResponse.setData(data);
        return plusResponse;
    }
    
    /**
     * Maps OffenceNoticeWithOwnerDto to the format expected in the PLUS API for offence notices
     *
     * @param dto OffenceNoticeWithOwnerDto to map
     * @param includeSuspendedInfo Whether to include suspended information
     * @param suspendedNoticesMap Map of notice numbers to suspended notices
     * @return Map containing the formatted data
     */
    // private Map<String, Object> mapOffenceNoticeToApiFormat(
    //         OffenceNoticeWithOwnerDto dto,
    //         boolean includeSuspendedInfo,
    //         Map<String, List<SuspendedNoticePlusDto>> suspendedNoticesMap) {
    private Map<String, Object> mapOffenceNoticeToApiFormat(
            OffenceNoticeWithOwnerDto dto,
            boolean includeSuspendedInfo) {

        Map<String, Object> result = new LinkedHashMap<>();

        // Map fields according to the PLUS API specification
        result.put("noticeNo", dto.getNoticeNo());
        result.put("compositionAmount", dto.getCompositionAmount());
        result.put("computerRuleCode", dto.getComputerRuleCode());
        result.put("eprDateOfSuspension", dto.getEprDateOfSuspension());
        result.put("eprReasonOfSuspension", dto.getEprReasonOfSuspension());
        result.put("lastProcessingStage", dto.getLastProcessingStage());
        result.put("lastProcessingDate", dto.getLastProcessingDate());
        result.put("nextProcessingDate", dto.getNextProcessingDate());
        result.put("nextProcessingStage", dto.getNextProcessingStage());
        result.put("noticeDateAndTime", dto.getNoticeDateAndTime());
        result.put("offenceNoticeType", dto.getOffenceNoticeType());
        result.put("parkingLotNo", dto.getParkingLotNo());
        result.put("ppCode", dto.getPpCode());
        result.put("ppName", dto.getPpName());
        result.put("suspensionType", dto.getSuspensionType());
        result.put("vehicleCategory", dto.getVehicleCategory());
        result.put("vehicleNo", dto.getVehicleNo());
        result.put("vehicleRegistrationType", dto.getVehicleRegistrationType());
        result.put("amountPayable", dto.getAmountPayable());
        result.put("crsDateOfSuspension", dto.getCrsDateOfSuspension());
        result.put("crsReasonOfSuspension", dto.getCrsReasonOfSuspension());
        result.put("otherRemark", dto.getOtherRemark());
        result.put("paymentDueDate", dto.getPaymentDueDate());
        result.put("dueDateOfRevival", dto.getDueDateOfRevival());
        result.put("subsystemLabel", dto.getSubsystemLabel());
        result.put("paymentAcceptanceAllowed", dto.getPaymentAcceptanceAllowed());
        result.put("wardenNo", dto.getWardenNo());
        // Conditionally add anFlag and suspended fields based on suspendedIndicator
        if (includeSuspendedInfo) {
            result.put("anFlag", dto.getAnFlag());
            // List<SuspendedNoticePlusDto> suspendedNotices =
            //     suspendedNoticesMap.getOrDefault(dto.getNoticeNo(), Collections.emptyList());
            // result.put("suspended", suspendedNotices);
        }

        result.put("paymentStatus", null);

        return result;
    }
}

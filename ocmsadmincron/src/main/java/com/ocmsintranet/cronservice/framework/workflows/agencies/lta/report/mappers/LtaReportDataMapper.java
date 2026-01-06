package com.ocmsintranet.cronservice.framework.workflows.agencies.lta.report.mappers;

import com.ocmsintranet.cronservice.framework.workflows.agencies.lta.report.dto.LtaReportData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Mapper class for converting database results to LtaReportData objects.
 * Handles both ResultSet and Map<String, Object> conversions.
 */
@Component
public class LtaReportDataMapper {

    /**
     * Maps a ResultSet row to LtaReportData object.
     *
     * @param rs ResultSet containing the data
     * @return LtaReportData object
     * @throws SQLException if there's an error reading from ResultSet
     */
    public LtaReportData mapFromResultSet(ResultSet rs) throws SQLException {
        return LtaReportData.builder()
                // Core notice information (from query)
                .noticeNo(rs.getString("notice_no"))
                .vehicleNo(rs.getString("vehicle_no"))
                .vehicleCategory(rs.getString("vehicle_category"))
                .offenceNoticeType(rs.getString("offence_notice_type"))
                .noticeDateAndTime(rs.getObject("notice_date_and_time", LocalDateTime.class))
                .compositionAmount(rs.getBigDecimal("composition_amount"))

                // Processing fields (from actual query)
                .processingDateTime(rs.getObject("processing_date_time", LocalDateTime.class))
                .errorCode(rs.getString("error_code")) // LTA response code

                // LTA Response specific fields
                .ltaDeregistrationDate(rs.getObject("lta_deregistration_date", LocalDateTime.class) != null ?
                    rs.getObject("lta_deregistration_date", LocalDateTime.class).toLocalDate() : null)

                // Car Park information (from query)
                .ppCode(rs.getString("pp_code")) // car park code
                .ppName(rs.getString("pp_name")) // car park name

                // Processing type information
                .suspensionType(rs.getString("suspension_type"))
                .eprReasonOfSuspension(rs.getString("epr_reason_of_suspension"))

                // Owner information (from query)
                .ownerName(rs.getString("name")) // from onod.name

                .build();
    }

}
package com.ocmsintranet.apiservice.workflows.reports.agency.mha;

import com.ocmsintranet.apiservice.crud.ocmsizdb.suspendednotice.SuspendedNoticeService;
import com.ocmsintranet.apiservice.workflows.reports.agency.mha.dto.MhaReportRequestDto;
import com.ocmsintranet.apiservice.workflows.reports.agency.mha.dto.MhaReportResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MhaReportServiceImpl implements MhaReportService {

    @Autowired
    private SuspendedNoticeService suspendedNoticeService;

    @Override
    public ResponseEntity<MhaReportResponseDto> getMhaReport(MhaReportRequestDto request) {
        try {
            log.info("Processing MHA report request with pagination: skip={}, limit={}",
                    request.getSkip(), request.getLimit());

            // Set default values if not provided
            if (request.getSkip() == null) {
                request.setSkip(0);
            }
            if (request.getLimit() == null) {
                request.setLimit(10);
            }

            // Validate date range is provided
            if (request.getDateFromCreated() == null || request.getDateToCreated() == null) {
                log.error("Date range is required for MHA report");
                return ResponseEntity.badRequest().build();
            }

            // Convert LocalDateTime to yyyy-MM-dd format
            String fromDate = request.getDateFromCreated().toLocalDate().toString();
            String toDate = request.getDateToCreated().toLocalDate().toString();

            // Build order by map from request (single field only), or use default
            Map<String, Integer> orderByFields = new HashMap<>();
            if (request.getSortField() != null && !request.getSortField().isEmpty()) {
                Integer direction = request.getSortDirection() != null ? request.getSortDirection() : 1;
                orderByFields.put(request.getSortField(), direction);
                log.info("Sorting by field: {}, direction: {}", request.getSortField(), direction);
            } else {
                orderByFields.put("notice_no", 1); // Default sort by notice_no ASC
                log.info("Using default sort: notice_no ASC");
            }

            // Call the custom suspended notice service
            List<Map<String, Object>> suspendedRecords = suspendedNoticeService.getRecordsForReport(
                fromDate,
                toDate,
                orderByFields
            );

            log.info("Retrieved {} suspended notice records", suspendedRecords.size());

            // Convert suspended notice records to MhaRecord DTOs
            List<MhaReportResponseDto.MhaRecord> mhaRecords = convertSuspendedRecordsToMhaRecords(suspendedRecords);

            // Apply pagination
            int total = mhaRecords.size();
            int fromIndex = Math.min(request.getSkip(), total);
            int toIndex = Math.min(request.getSkip() + request.getLimit(), total);
            List<MhaReportResponseDto.MhaRecord> paginatedRecords = mhaRecords.subList(fromIndex, toIndex);

            MhaReportResponseDto response = new MhaReportResponseDto();
            response.setData(paginatedRecords);
            response.setTotal(total);
            response.setSkip(request.getSkip());
            response.setLimit(request.getLimit());

            // Populate metrics map (control metrics) for summary information
            try {
                Map<String, Integer> metrics = suspendedNoticeService.getMhaControlMetrics(fromDate, toDate);
                response.setMetrics(metrics);
            } catch (Exception ex) {
                log.warn("Failed to fetch MHA control metrics: {}", ex.getMessage());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing MHA report: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String convertToString(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String) {
            return (String) obj;
        }
        if (obj instanceof Character) {
            return obj.toString();
        }
        return obj.toString();
    }

    /**
     * Convert suspended notice Map records to MhaRecord DTOs
     * Based on UNION ALL query with 23 columns from SuspendedNoticeServiceImpl
     */
    private List<MhaReportResponseDto.MhaRecord> convertSuspendedRecordsToMhaRecords(List<Map<String, Object>> suspendedRecords) {
        List<MhaReportResponseDto.MhaRecord> mhaRecords = new ArrayList<>();

        for (Map<String, Object> record : suspendedRecords) {
            MhaReportResponseDto.MhaRecord mhaRecord = new MhaReportResponseDto.MhaRecord();

            // Row 0: noticeNo
            mhaRecord.setNoticeNo(convertToString(record.get("noticeNo")));

            // Row 1: dataType ('SUCCESS' or 'ERROR')
            mhaRecord.setDataType(convertToString(record.get("dataType")));

            // Row 2-3: processing stages
            mhaRecord.setLastProcessingStage(convertToString(record.get("lastProcessingStage")));
            mhaRecord.setNextProcessingStage(convertToString(record.get("nextProcessingStage")));

            // Row 4: nextProcessingDate
            Object nextProcessingDate = record.get("nextProcessingDate");
            if (nextProcessingDate != null) {
                if (nextProcessingDate instanceof java.sql.Timestamp) {
                    mhaRecord.setNextProcessingDate(((java.sql.Timestamp) nextProcessingDate).toLocalDateTime());
                } else if (nextProcessingDate instanceof java.sql.Date) {
                    mhaRecord.setNextProcessingDate(((java.sql.Date) nextProcessingDate).toLocalDate().atStartOfDay());
                } else if (nextProcessingDate instanceof java.time.LocalDateTime) {
                    mhaRecord.setNextProcessingDate((java.time.LocalDateTime) nextProcessingDate);
                } else if (nextProcessingDate instanceof java.time.LocalDate) {
                    mhaRecord.setNextProcessingDate(((java.time.LocalDate) nextProcessingDate).atStartOfDay());
                }
            }

            // Row 5: noticeDateAndTime
            Object noticeDateAndTime = record.get("noticeDateAndTime");
            if (noticeDateAndTime != null) {
                if (noticeDateAndTime instanceof java.sql.Timestamp) {
                    mhaRecord.setNoticeDateAndTime(((java.sql.Timestamp) noticeDateAndTime).toLocalDateTime());
                } else if (noticeDateAndTime instanceof java.time.LocalDateTime) {
                    mhaRecord.setNoticeDateAndTime((java.time.LocalDateTime) noticeDateAndTime);
                }
            }

            // Row 6: processingDateTime
            Object processingDateTime = record.get("processingDateTime");
            if (processingDateTime != null) {
                if (processingDateTime instanceof java.sql.Timestamp) {
                    mhaRecord.setProcessingDateTime(((java.sql.Timestamp) processingDateTime).toLocalDateTime());
                } else if (processingDateTime instanceof java.time.LocalDateTime) {
                    mhaRecord.setProcessingDateTime((java.time.LocalDateTime) processingDateTime);
                }
            }

            // Row 7: effectiveDate
            Object effectiveDate = record.get("effectiveDate");
            if (effectiveDate != null) {
                if (effectiveDate instanceof java.sql.Timestamp) {
                    mhaRecord.setEffectiveDate(((java.sql.Timestamp) effectiveDate).toLocalDateTime());
                } else if (effectiveDate instanceof java.sql.Date) {
                    mhaRecord.setEffectiveDate(((java.sql.Date) effectiveDate).toLocalDate().atStartOfDay());
                } else if (effectiveDate instanceof java.time.LocalDateTime) {
                    mhaRecord.setEffectiveDate((java.time.LocalDateTime) effectiveDate);
                } else if (effectiveDate instanceof java.time.LocalDate) {
                    mhaRecord.setEffectiveDate(((java.time.LocalDate) effectiveDate).atStartOfDay());
                }
            }

            // Row 8: invalidAddrTag
            mhaRecord.setInvalidAddrTag(convertToString(record.get("invalidAddrTag")));

            // Row 9-13: identity fields
            mhaRecord.setIdType(convertToString(record.get("idType")));
            mhaRecord.setIdNo(convertToString(record.get("idNo")));
            mhaRecord.setName(convertToString(record.get("name")));

            // Row 12: dateOfBirth
            Object dateOfBirth = record.get("dateOfBirth");
            if (dateOfBirth != null) {
                if (dateOfBirth instanceof java.sql.Timestamp) {
                    mhaRecord.setDateOfBirth(((java.sql.Timestamp) dateOfBirth).toLocalDateTime());
                } else if (dateOfBirth instanceof java.sql.Date) {
                    mhaRecord.setDateOfBirth(((java.sql.Date) dateOfBirth).toLocalDate().atStartOfDay());
                } else if (dateOfBirth instanceof java.time.LocalDateTime) {
                    mhaRecord.setDateOfBirth((java.time.LocalDateTime) dateOfBirth);
                } else if (dateOfBirth instanceof java.time.LocalDate) {
                    mhaRecord.setDateOfBirth(((java.time.LocalDate) dateOfBirth).atStartOfDay());
                }
            }

            // Row 13: dateOfDeath
            Object dateOfDeath = record.get("dateOfDeath");
            if (dateOfDeath != null) {
                if (dateOfDeath instanceof java.sql.Timestamp) {
                    mhaRecord.setDateOfDeath(((java.sql.Timestamp) dateOfDeath).toLocalDateTime());
                } else if (dateOfDeath instanceof java.sql.Date) {
                    mhaRecord.setDateOfDeath(((java.sql.Date) dateOfDeath).toLocalDate().atStartOfDay());
                } else if (dateOfDeath instanceof java.time.LocalDateTime) {
                    mhaRecord.setDateOfDeath((java.time.LocalDateTime) dateOfDeath);
                } else if (dateOfDeath instanceof java.time.LocalDate) {
                    mhaRecord.setDateOfDeath(((java.time.LocalDate) dateOfDeath).atStartOfDay());
                }
            }

            // Row 14-19: address fields
            mhaRecord.setBldgName(convertToString(record.get("bldgName")));
            mhaRecord.setBlkHseNo(convertToString(record.get("blkHseNo")));
            mhaRecord.setStreetName(convertToString(record.get("streetName")));
            mhaRecord.setPostalCode(convertToString(record.get("postalCode")));
            mhaRecord.setFloorNo(convertToString(record.get("floorNo")));
            mhaRecord.setUnitNo(convertToString(record.get("unitNo")));

            // Row 20-22: suspension fields (only populated for ERROR records)
            mhaRecord.setSuspensionType(convertToString(record.get("suspensionType")));

            // Row 21: dueDateOfRevival
            Object dueDateOfRevival = record.get("dueDateOfRevival");
            if (dueDateOfRevival != null) {
                if (dueDateOfRevival instanceof java.sql.Timestamp) {
                    mhaRecord.setDueDateOfRevival(((java.sql.Timestamp) dueDateOfRevival).toLocalDateTime());
                } else if (dueDateOfRevival instanceof java.sql.Date) {
                    mhaRecord.setDueDateOfRevival(((java.sql.Date) dueDateOfRevival).toLocalDate().atStartOfDay());
                } else if (dueDateOfRevival instanceof java.time.LocalDateTime) {
                    mhaRecord.setDueDateOfRevival((java.time.LocalDateTime) dueDateOfRevival);
                } else if (dueDateOfRevival instanceof java.time.LocalDate) {
                    mhaRecord.setDueDateOfRevival(((java.time.LocalDate) dueDateOfRevival).atStartOfDay());
                }
            }

            // Row 22: eprReasonOfSuspension
            mhaRecord.setEprReasonOfSuspension(convertToString(record.get("eprReasonOfSuspension")));

            mhaRecords.add(mhaRecord);
        }

        return mhaRecords;
    }
}
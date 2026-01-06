package com.ocmsintranet.apiservice.workflows.reports.system.notification;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsEmailNotificationRecords.OcmsEmailNotificationRecordsRepository;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsSmsNotificationRecords.OcmsSmsNotificationRecordsRepository;
import com.ocmsintranet.apiservice.workflows.reports.system.notification.dto.NotificationReportRequestDto;
import com.ocmsintranet.apiservice.workflows.reports.system.notification.dto.NotificationReportResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificationReportServiceImpl implements NotificationReportService {

    @Autowired
    private OcmsEmailNotificationRecordsRepository emailRepository;

    @Autowired
    private OcmsSmsNotificationRecordsRepository smsRepository;

    @Autowired
    private EntityManager entityManager;

    @Override
    public ResponseEntity<NotificationReportResponseDto> getNotificationReport(NotificationReportRequestDto request) {
        try {
            log.info("Processing notification report request with pagination: skip={}, limit={}",
                    request.getSkip(), request.getLimit());

            // Set default values if not provided
            if (request.getSkip() == null) {
                request.setSkip(0);
            }
            if (request.getLimit() == null) {
                request.setLimit(10);
            }

            // Get total count first
            int totalCount = getTotalCount(request);

            // Get paginated results
            List<NotificationReportResponseDto.NotificationRecord> unifiedRecords = getCombinedNotifications(request);

            NotificationReportResponseDto response = new NotificationReportResponseDto();
            response.setData(unifiedRecords);
            response.setTotal(totalCount);
            response.setSkip(request.getSkip());
            response.setLimit(request.getLimit());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing notification report: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private List<NotificationReportResponseDto.NotificationRecord> getCombinedNotifications(NotificationReportRequestDto request) {
        StringBuilder sql = new StringBuilder();
        List<Object> parameters = new ArrayList<>();
        int paramIndex = 1;

        // Build unified query with UNION
        sql.append("SELECT notice_no, processing_stage, CONVERT(VARCHAR(MAX), content) as content, ")
           .append("status, msg_error, cre_date, ")
           .append("email_addr as contact, ")
           .append("date_sent, 'EMAIL' as type ")
           .append("FROM ocmsizmgr.ocms_email_notification_records WHERE 1=1 ");

        // Add email filters
        paramIndex = addFiltersToQuery(sql, parameters, paramIndex, request, true);

        sql.append(" UNION ALL ")
           .append("SELECT notice_no, processing_stage, CONVERT(VARCHAR(MAX), content) as content, ")
           .append("status, msg_error, cre_date, ")
           .append("CONCAT(ISNULL(mobile_code, ''), mobile_no) as contact, ")
           .append("date_sent, 'SMS' as type ")
           .append("FROM ocmsizmgr.ocms_sms_notification_records WHERE 1=1 ");

        // Add SMS filters
        paramIndex = addFiltersToQuery(sql, parameters, paramIndex, request, false);

        sql.append(" ORDER BY cre_date DESC");

        // Add pagination
        sql.append(" OFFSET ?").append(paramIndex).append(" ROWS FETCH NEXT ?").append(paramIndex + 1).append(" ROWS ONLY");
        parameters.add(request.getSkip());
        parameters.add(request.getLimit());

        // Execute native query
        jakarta.persistence.Query query = entityManager.createNativeQuery(sql.toString());
        for (int i = 0; i < parameters.size(); i++) {
            query.setParameter(i + 1, parameters.get(i));
        }

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        // Map results directly to DTOs
        List<NotificationReportResponseDto.NotificationRecord> records = new ArrayList<>(results.size());
        for (Object[] row : results) {
            NotificationReportResponseDto.NotificationRecord record = new NotificationReportResponseDto.NotificationRecord();
            record.setNoticeNo(convertToString(row[0]));
            record.setProcessingStage(convertToString(row[1]));
            record.setContent(convertToString(row[2]));
            record.setStatus(convertToString(row[3]));
            record.setMsgError(convertToString(row[4]));
            record.setCreDate(row[5] != null ? ((java.sql.Timestamp) row[5]).toLocalDateTime() : null);
            record.setContact(convertToString(row[6]));
            record.setDateSent(row[7] != null ? ((java.sql.Timestamp) row[7]).toLocalDateTime() : null);
            records.add(record);
        }

        return records;
    }

    private int getTotalCount(NotificationReportRequestDto request) {
        StringBuilder sql = new StringBuilder();
        List<Object> parameters = new ArrayList<>();
        int paramIndex = 1;

        // Build count query with UNION
        sql.append("SELECT COUNT(*) as total_count FROM (")
           .append("SELECT notice_no FROM ocmsizmgr.ocms_email_notification_records WHERE 1=1 ");

        // Add email filters
        paramIndex = addFiltersToQuery(sql, parameters, paramIndex, request, true);

        sql.append(" UNION ALL ")
           .append("SELECT notice_no FROM ocmsizmgr.ocms_sms_notification_records WHERE 1=1 ");

        // Add SMS filters
        paramIndex = addFiltersToQuery(sql, parameters, paramIndex, request, false);

        sql.append(") combined_results");

        // Execute count query
        jakarta.persistence.Query countQuery = entityManager.createNativeQuery(sql.toString());
        for (int i = 0; i < parameters.size(); i++) {
            countQuery.setParameter(i + 1, parameters.get(i));
        }

        Number result = (Number) countQuery.getSingleResult();
        return result.intValue();
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

    private int addFiltersToQuery(StringBuilder sql, List<Object> parameters, int paramIndex,
                                 NotificationReportRequestDto request, boolean isEmail) {

        if (request.getNoticeNo() != null && !request.getNoticeNo().trim().isEmpty()) {
            sql.append(" AND LOWER(notice_no) LIKE ?").append(paramIndex);
            parameters.add("%" + request.getNoticeNo().toLowerCase() + "%");
            paramIndex++;
        }

        if (request.getProcessingStage() != null && !request.getProcessingStage().trim().isEmpty()) {
            sql.append(" AND processing_stage = ?").append(paramIndex);
            parameters.add(request.getProcessingStage());
            paramIndex++;
        }

        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            sql.append(" AND status = ?").append(paramIndex);
            parameters.add(request.getStatus());
            paramIndex++;
        }

        if (request.getMsgError() != null && !request.getMsgError().trim().isEmpty()) {
            sql.append(" AND LOWER(msg_error) LIKE ?").append(paramIndex);
            parameters.add("%" + request.getMsgError().toLowerCase() + "%");
            paramIndex++;
        }

        // Email-specific filters
        if (isEmail) {
            if (request.getEmailAddr() != null && !request.getEmailAddr().trim().isEmpty()) {
                sql.append(" AND LOWER(email_addr) LIKE ?").append(paramIndex);
                parameters.add("%" + request.getEmailAddr().toLowerCase() + "%");
                paramIndex++;
            }

            if (request.getSubject() != null && !request.getSubject().trim().isEmpty()) {
                sql.append(" AND LOWER(subject) LIKE ?").append(paramIndex);
                parameters.add("%" + request.getSubject().toLowerCase() + "%");
                paramIndex++;
            }
        } else {
            // SMS-specific filters
            if (request.getMobileCode() != null && !request.getMobileCode().trim().isEmpty()) {
                sql.append(" AND LOWER(mobile_code) LIKE ?").append(paramIndex);
                parameters.add("%" + request.getMobileCode().toLowerCase() + "%");
                paramIndex++;
            }

            if (request.getMobileNo() != null && !request.getMobileNo().trim().isEmpty()) {
                sql.append(" AND LOWER(mobile_no) LIKE ?").append(paramIndex);
                parameters.add("%" + request.getMobileNo().toLowerCase() + "%");
                paramIndex++;
            }
        }

        // Date filters
        if (request.getDateFromCreated() != null && request.getDateToCreated() != null) {
            sql.append(" AND cre_date BETWEEN ?").append(paramIndex).append(" AND ?").append(paramIndex + 1);
            parameters.add(request.getDateFromCreated());
            parameters.add(request.getDateToCreated());
            paramIndex += 2;
        } else if (request.getDateFromCreated() != null) {
            sql.append(" AND cre_date >= ?").append(paramIndex);
            parameters.add(request.getDateFromCreated());
            paramIndex++;
        } else if (request.getDateToCreated() != null) {
            sql.append(" AND cre_date <= ?").append(paramIndex);
            parameters.add(request.getDateToCreated());
            paramIndex++;
        }

        if (request.getDateFromSent() != null && request.getDateToSent() != null) {
            sql.append(" AND date_sent BETWEEN ?").append(paramIndex).append(" AND ?").append(paramIndex + 1);
            parameters.add(request.getDateFromSent());
            parameters.add(request.getDateToSent());
            paramIndex += 2;
        } else if (request.getDateFromSent() != null) {
            sql.append(" AND date_sent >= ?").append(paramIndex);
            parameters.add(request.getDateFromSent());
            paramIndex++;
        } else if (request.getDateToSent() != null) {
            sql.append(" AND date_sent <= ?").append(paramIndex);
            parameters.add(request.getDateToSent());
            paramIndex++;
        }

        return paramIndex;
    }
}
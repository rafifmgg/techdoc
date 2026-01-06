package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice;

import com.ocmsintranet.apiservice.crud.BaseImplement;
import com.ocmsintranet.apiservice.crud.DatabaseRetryService;
import com.ocmsintranet.apiservice.crud.beans.FindAllResponse;
import com.ocmsintranet.apiservice.crud.beans.SystemConstant;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsOffenceNoticeOwnerDriver.OcmsOffenceNoticeOwnerDriver;
import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.dto.OffenceNoticeWithOwnerDto;
import com.ocmsintranet.apiservice.workflows.notice_creation.core.helpers.FileProcessingHelper;
import com.ocmsintranet.apiservice.workflows.notice_creation.core.dto.OffenceNoticeDto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.extern.slf4j.Slf4j;

/**
* Service implementation for OcmsValidOffenceNotice entities
*/
@Service
@Slf4j
public class OcmsValidOffenceNoticeServiceImpl extends BaseImplement<OcmsValidOffenceNotice, String, OcmsValidOffenceNoticeRepository> 
implements OcmsValidOffenceNoticeService {
    
    /**
    * Constructor with required dependencies
    */
    public OcmsValidOffenceNoticeServiceImpl(OcmsValidOffenceNoticeRepository repository, DatabaseRetryService retryService, EntityManager entityManager) {
        super(repository, retryService);
        this.entityManager = entityManager;
    }
    
    private final EntityManager entityManager;
    
    // --- Owner/Driver field set (left-joined entity alias: o) ---
    private static final Set<String> OWNER_FIELDS = Set.of(
    "idType", "idNo", "offenderIndicator", "name", "ownerDriverIndicator"
    );
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Singapore");
    
    private static final Pattern OR_KEY = Pattern.compile("^\\$or\\[(\\d+)]\\.(.*+)$");
    // Removed unused pattern: OP_SUFFIX
    
    @Autowired
    private FileProcessingHelper fileProcessingHelper;
    
    /**
    * Enhanced patch method that handles both entity updates and file processing
    * 
    * @param id The notice ID
    * @param partialEntity The entity with updated fields
    * @param payload The complete request payload for additional processing (files)
    * @return The updated entity
    */
    @Transactional
    public OcmsValidOffenceNotice patchWithFiles(String id, OcmsValidOffenceNotice partialEntity, Map<String, Object> payload) {
        // First, perform the standard patch operation using the parent method
        OcmsValidOffenceNotice updatedEntity = super.patch(id, partialEntity);
        
        // Get the user ID from the payload
        String userId = (String) payload.get("updUserId");
        if (userId == null) {
            userId = SystemConstant.User.DEFAULT_SYSTEM_USER_ID; // Default fallback if updUserId is not provided
        }
        
        // Check for photos or videos in the payload
        boolean hasPhotos = payload.containsKey("photos") && payload.get("photos") != null;
        boolean hasVideos = payload.containsKey("videos") && payload.get("videos") != null;
        
        if (hasPhotos || hasVideos) {
            // Create a DTO to hold the photos and videos
            OffenceNoticeDto fileDto = new OffenceNoticeDto();
            
            // Filter photos that contain "offencenoticefiles" in their name
            if (hasPhotos) {
                @SuppressWarnings("unchecked")
                List<String> allPhotos = (List<String>) payload.get("photos");
                List<String> filteredPhotos = allPhotos.stream()
                .filter(photo -> photo != null && photo.contains("offencenoticefiles"))
                .collect(Collectors.toList());
                
                if (!filteredPhotos.isEmpty()) {
                    fileDto.setPhotos(filteredPhotos);
                }
            }
            
            // Filter videos that contain "offencenoticefiles" in their name
            if (hasVideos) {
                @SuppressWarnings("unchecked")
                List<String> allVideos = (List<String>) payload.get("videos");
                List<String> filteredVideos = allVideos.stream()
                .filter(video -> video != null && video.contains("offencenoticefiles"))
                .collect(Collectors.toList());
                
                if (!filteredVideos.isEmpty()) {
                    fileDto.setVideos(filteredVideos);
                }
            }
            
            // Only process if there are valid files after filtering
            if ((fileDto.getPhotos() != null && !fileDto.getPhotos().isEmpty()) || 
            (fileDto.getVideos() != null && !fileDto.getVideos().isEmpty())) {
                
                // Create a new map with the DTO
                Map<String, Object> fileData = new HashMap<>();
                fileData.put("dto", fileDto);
                
                // Process the files
                boolean filesProcessed = fileProcessingHelper.processUploadedFile(id, fileData, userId);
                
                if (!filesProcessed) {
                    log.warn("Some files could not be processed for notice {}", id);
                }
            }
        }
        
        // After handling photos and videos
        boolean hasDeleteFiles = payload.containsKey("deleteAttachments") && payload.get("deleteAttachments") != null;
        
        if (hasDeleteFiles) {
            log.info("Processing file deletion for notice {}", id);
            // Process file deletion
            @SuppressWarnings("unchecked")
            List<String> filesToDelete = (List<String>) payload.get("deleteAttachments");
            
            if (filesToDelete != null && !filesToDelete.isEmpty()) {
                log.info("Found {} files to delete for notice {}", filesToDelete.size(), id);
                // Delete each file from database and blob storage
                for (String fileToDelete : filesToDelete) {
                    log.info("Deleting attachment {} for notice {}", fileToDelete, id);
                    boolean deleted = fileProcessingHelper.deleteAttachment(id, fileToDelete, userId);
                    if (!deleted) {
                        log.warn("Failed to delete attachment {} for notice {}", fileToDelete, id);
                    }
                }
            }
        }
        
        return updatedEntity;
    }
    
    
    /**
    * Override getAll to handle owner/driver filters
    * This method checks if any owner/driver filters are present and uses a JOIN query if needed
    */
    @Override
    public FindAllResponse<OcmsValidOffenceNotice> getAll(Map<String, String[]> params) {
        // Create a copy of params to avoid modifying the original
        Map<String, String[]> filteredParams = new HashMap<>(params);
        
        // Save the $field parameter to use it later
        String[] fieldParams = params.get("$field");
        
        // Check if any owner/driver filters are present
        boolean hasOwnerDriverFilters = filteredParams.containsKey("offenderIndicator") || 
        filteredParams.containsKey("ownerDriverIndicator") ||
        filteredParams.containsKey("idNo") ||
        filteredParams.containsKey("idType") ||
        filteredParams.containsKey("name");
        
        if (!hasOwnerDriverFilters) {
            // If no owner/driver filters, use the standard implementation
            // Make sure to pass the original $field parameter
            if (fieldParams != null) {
                filteredParams.put("$field", fieldParams);
            }
            return super.getAll(filteredParams);
        }
        
        // Extract owner/driver filter values
        String offenderIndicator = getParamValue(filteredParams, "offenderIndicator");
        String ownerDriverIndicator = getParamValue(filteredParams, "ownerDriverIndicator");
        String idNo = getParamValue(filteredParams, "idNo");
        String idType = getParamValue(filteredParams, "idType");
        String name = getParamValue(filteredParams, "name");
        
        // Remove owner/driver filters from the filtered parameters
        filteredParams.remove("offenderIndicator");
        filteredParams.remove("ownerDriverIndicator");
        filteredParams.remove("idNo");
        filteredParams.remove("idType");
        filteredParams.remove("name");
        
        // Get pagination parameters
        int skip = getSkipValue(filteredParams);
        int limit = getLimitValue(filteredParams);
        
        // Execute the join query to get notices that match owner/driver criteria
        List<OcmsValidOffenceNotice> joinResults = retryService.executeWithRetry(() -> 
        repository.findByOwnerDriverInfo(
            offenderIndicator,
            ownerDriverIndicator,
            idNo,
            idType,
            name
        )
        );
        
        // If there are no other filters, just apply pagination and return
        if (isOnlyPaginationAndSorting(filteredParams)) {
            int total = joinResults.size();
            List<OcmsValidOffenceNotice> pagedResults = joinResults.stream()
            .skip(skip)
            .limit(limit)
            .collect(Collectors.toList());
            
            return new FindAllResponse<>(total, limit, skip, pagedResults);
        }
        
        // Otherwise, apply the remaining filters using the base implementation
        // This is a bit of a hack, but it should work for most cases
        List<OcmsValidOffenceNotice> finalResults = new ArrayList<>();
        for (OcmsValidOffenceNotice notice : joinResults) {
            // Create a map with just this notice's ID
            Map<String, String[]> idParam = new HashMap<>();
            idParam.put("noticeNo", new String[]{notice.getNoticeNo()});
            
            // Merge with the filtered params
            Map<String, String[]> mergedParams = new HashMap<>(filteredParams);
            mergedParams.put("noticeNo", new String[]{notice.getNoticeNo()});
            
            // Make sure to include the $field parameter if it exists
            if (fieldParams != null) {
                mergedParams.put("$field", fieldParams);
            }
            
            // Use the base implementation to check if this notice matches the other filters
            FindAllResponse<OcmsValidOffenceNotice> singleResult = super.getAll(mergedParams);
            if (!singleResult.getData().isEmpty()) {
                finalResults.add(notice);
            }
        }
        
        // Apply pagination
        int total = finalResults.size();
        List<OcmsValidOffenceNotice> pagedResults = finalResults.stream()
        .skip(skip)
        .limit(limit)
        .collect(Collectors.toList());
        
        return new FindAllResponse<>(total, limit, skip, pagedResults);
    }
    
    /**
    * Helper method to get a parameter value from the params map
    */
    private String getParamValue(Map<String, String[]> params, String key) {
        String[] values = params.get(key);
        return values != null && values.length > 0 ? values[0] : null;
    }
    
    /**
    * Helper method to check if params only contain pagination and sorting parameters
    */
    private boolean isOnlyPaginationAndSorting(Map<String, String[]> params) {
        for (String key : params.keySet()) {
            if (!key.equals("$skip") && !key.equals("$limit") && 
            !key.equals("$sort") && !key.equals("$order") &&
            !key.equals("$field")) {
                return false;
            }
        }
        return true;
    }
    
    /**
    * Get the skip value from parameters
    */
    private int getSkipValue(Map<String, String[]> params) {
        final String SKIP_PARAM = "$skip";
        final int DEFAULT_SKIP = 0;
        
        if (params.containsKey(SKIP_PARAM) && params.get(SKIP_PARAM).length > 0) {
            try {
                return Integer.parseInt(params.get(SKIP_PARAM)[0]);
            } catch (NumberFormatException e) {
                return DEFAULT_SKIP;
            }
        }
        return DEFAULT_SKIP;
    }
    
    /**
    * Get the limit value from parameters
    */
    private int getLimitValue(Map<String, String[]> params) {
        final String LIMIT_PARAM = "$limit";
        final int DEFAULT_LIMIT = 10;
        final int MAX_LIMIT = 1000;
        
        if (params.containsKey(LIMIT_PARAM) && params.get(LIMIT_PARAM).length > 0) {
            try {
                int limit = Integer.parseInt(params.get(LIMIT_PARAM)[0]);
                return Math.min(limit, MAX_LIMIT);
            } catch (NumberFormatException e) {
                return DEFAULT_LIMIT;
            }
        }
        return DEFAULT_LIMIT;
    }
    
    @Override
    @Transactional(readOnly = true)
    public FindAllResponse<OffenceNoticeWithOwnerDto> getAllWithOwnerInfo(Map<String, String[]> params) {
        // Pagination
        int skip = intParam(params, "$skip", 0);
        int limit = intParam(params, "$limit", 20);

        // Check if filterByReceipt is enabled
        String filterByReceipt = first(params.get("filterByReceipt"));
        boolean shouldFilterByReceipt = "Y".equals(filterByReceipt);

        // Check if receiptNo filter is provided
        String receiptNo = first(params.get("receiptNo"));
        boolean hasReceiptNoFilter = receiptNo != null && !receiptNo.trim().isEmpty();

        // Base JPQL
        // Using proper JOIN with composite key
        StringBuilder jpql = new StringBuilder("SELECT n, o FROM OcmsValidOffenceNotice n " +
        "LEFT JOIN OcmsOffenceNoticeOwnerDriver o ON o.id.noticeNo = n.noticeNo ");

        // Add LEFT JOIN with OcmsWebTransactionDetail if filterByReceipt or receiptNo filter is enabled
        if (shouldFilterByReceipt || hasReceiptNoFilter) {
            jpql.append("LEFT JOIN OcmsWebTransactionDetail w ON w.offenceNoticeNo = n.noticeNo ");
        }

        jpql.append("WHERE 1=1 ");
        Map<String, Object> qp = new LinkedHashMap<>();
        AtomicInteger pIdx = new AtomicInteger(0);
        
        // --- Compute helper booleans per your spec ---
        boolean hasOwnerDriverFilters = params.keySet().stream()
        .map(OcmsValidOffenceNoticeServiceImpl::stripOrPrefix)
        .anyMatch(key -> isOwnerFieldKey(key));
        
        String offenderIndicator = first(params.get("offenderIndicator"));
        boolean hasOffenderIndicatorParam = offenderIndicator != null;
        
        // --- Apply offenderIndicator/name rules ---
        if (hasOffenderIndicatorParam && "Y".equals(offenderIndicator)) {
            if (!hasOwnerDriverFilters) {
                jpql.append(" AND (o.offenderIndicator = 'Y' OR o IS NULL) ");
            } else {
                jpql.append(" AND (o.offenderIndicator = 'Y') ");
            }
        } else if (!hasOwnerDriverFilters && !hasOffenderIndicatorParam) {
            jpql.append(" AND (o.offenderIndicator = 'Y' OR o IS NULL) ");
        }
        
        if (params.containsKey("name") && params.get("name").length > 0) {
            jpql.append(" AND LOWER(o.name) LIKE :name ");
            qp.put("name", "%" + params.get("name")[0].toLowerCase() + "%");
        }

        // --- Apply filterByReceipt filter ---
        if (shouldFilterByReceipt) {
            jpql.append(" AND w.receiptNo IS NOT NULL ");
        }

        // --- Apply receiptNo filter ---
        if (hasReceiptNoFilter) {
            String pName = "p" + pIdx.incrementAndGet();
            jpql.append(" AND w.receiptNo = :").append(pName).append(" ");
            qp.put(pName, receiptNo);
        }

        // --- Top-level filters (excluding special keys and handled fields) ---
        Map<String, String> topLevelFilters = params.entrySet().stream()
        .filter(e -> !isSpecialParam(e.getKey()))
        .filter(e -> !e.getKey().startsWith("$or["))
        .filter(e -> !e.getKey().equals("filterByReceipt")) // Exclude filterByReceipt from filters
        .filter(e -> !e.getKey().equals("receiptNo")) // Exclude receiptNo from filters (handled above)
        .collect(Collectors.toMap(Map.Entry::getKey, e -> first(e.getValue()), (a, b) -> b, LinkedHashMap::new));
        
        String topPred = buildPredicateBlock(topLevelFilters, qp, pIdx, "n", "o");
        if (!topPred.isBlank()) {
            jpql.append(" AND ").append(topPred);
        }
        
        // --- $or blocks ---
        List<Map<String, String>> orBlocks = extractOrBlocks(params);

        // Build each OR block's predicate; keep only non-empty ones
        List<String> orPredicates = new ArrayList<>();
        for (Map<String, String> block : orBlocks) {
            String pred = buildPredicateBlock(block, qp, pIdx, "n", "o");
            if (!pred.isBlank()) {
                orPredicates.add("(" + pred + ")");
            }
        }

        if (!orPredicates.isEmpty()) {
            jpql.append(" AND (").append(String.join(" OR ", orPredicates)).append(")");
        }
        
        // Sorting
        Map<String, Integer> sort = sortParams(params);
        if (!sort.isEmpty()) {
            jpql.append(" ORDER BY ");
            int c = 0;
            for (Map.Entry<String, Integer> e : sort.entrySet()) {
                if (c++ > 0) jpql.append(", ");
                String base = e.getKey();
                String alias = ownerField(base) ? "o" : "n";
                // Handle composite key fields in sorting
                String sortPath = ownerField(base) && isIdField(base) ? "id." + base : base;
                jpql.append(alias).append(".").append(sortPath).append(e.getValue() != null && e.getValue() == -1 ? " DESC" : " ASC");
            }
        }
        log.info("JPQL: {}", jpql.toString());
        
        // --- Count query (DISTINCT by notice PK to avoid join duplication) ---
        String countJpql = jpql.toString()
        .replaceFirst("SELECT n, o", "SELECT COUNT(DISTINCT n.noticeNo)")
        .replaceFirst("ORDER BY[\\s\\S]*$", "");
        log.info("Count JPQL: {}", countJpql);

        long total = 0L;
        try {
            TypedQuery<Long> countQ = entityManager.createQuery(countJpql, Long.class);
            qp.forEach(countQ::setParameter);
            // Use getResultList to avoid NoResultException in some providers
            List<Long> cnt = countQ.getResultList();
            total = (cnt.isEmpty() ? 0L : cnt.get(0));
        } catch (Exception ex) {
            // If the count fails for any reason, treat as 0 rather than erroring the request
            log.warn("Count query failed; returning empty result. JPQL={}, err={}", countJpql, ex.toString());
            total = 0L;
        }

        // If nothing to return (or the page is beyond the end), return an empty page
        if (total == 0L || skip >= total) {
            return new FindAllResponse<>(
                total,          // if your FindAllResponse expects int, use Math.toIntExact(total)
                limit,
                skip,
                Collections.emptyList()
            );
        }
        
        // --- Data query ---
        TypedQuery<Object[]> dataQ = entityManager.createQuery(jpql.toString(), Object[].class);
        qp.forEach(dataQ::setParameter);
        if (skip > 0) dataQ.setFirstResult(skip);
        if (limit > 0) dataQ.setMaxResults(limit);
        
        List<Object[]> rows = dataQ.getResultList();
        List<OffenceNoticeWithOwnerDto> data = rows.stream()
        .map(r -> toDto((OcmsValidOffenceNotice) r[0], (OcmsOffenceNoticeOwnerDriver) r[1]))
        .toList();
        
        // Build FindAllResponse — adjust to your actual constructor/builder
        return new FindAllResponse<OffenceNoticeWithOwnerDto>(total, limit, skip, data);
    }
    
    // Build a single block of predicates (no leading AND). Returns "" if none.
    private String buildPredicateBlock(Map<String, String> block,
    Map<String, Object> qp,
    AtomicInteger pIdx,
    String noticeAlias,
    String ownerAlias) {
        
        List<String> predicates = new ArrayList<>();
        
        for (Map.Entry<String, String> e : block.entrySet()) {
            String key = e.getKey();
            String val = e.getValue();
            
            // Skip fields already handled globally
            if ("offenderIndicator".equals(key) || "name".equals(key)) continue;
            
            String baseKey = baseField(key);
            String op = operatorOf(key); // $in, $gte, $lte, $null, or ""
            
            String alias = ownerField(baseKey) ? ownerAlias : noticeAlias;
            // Handle composite key fields for owner entity
            String fieldPath = ownerField(baseKey) && isIdField(baseKey) ? "id." + baseKey : baseKey;
            String param = "p" + pIdx.incrementAndGet();
            
            switch (op) {
                case "$in" -> {
                    List<String> parts = splitCsv(val);
                    List<String> nonEmpty = parts.stream()
                    .filter(s -> s != null && !s.isEmpty())
                    .toList();
                    boolean wantsNullOrEmpty = parts.stream().anyMatch(s -> s == null || s.isEmpty());
                    
                    if (!nonEmpty.isEmpty()) {
                        StringBuilder p = new StringBuilder();
                        p.append("(").append(alias).append(".").append(fieldPath)
                        .append(" IN :").append(param);
                        if (wantsNullOrEmpty) {
                            p.append(" OR ").append(alias).append(".").append(fieldPath).append(" IS NULL")
                            .append(" OR ").append(alias).append(".").append(fieldPath).append(" = ''");
                        }
                        p.append(")");
                        predicates.add(p.toString());
                        qp.put(param, nonEmpty);
                    } else if (wantsNullOrEmpty) {
                        predicates.add("(" + alias + "." + fieldPath + " IS NULL OR " + alias + "." + fieldPath + " = '' )");
                    }
                }
                case "$gte" -> {
                    predicates.add(alias + "." + fieldPath + " >= :" + param);
                    qp.put(param, castValue(val, baseKey));
                }
                case "$lte" -> {
                    predicates.add(alias + "." + fieldPath + " <= :" + param);
                    qp.put(param, castValue(val, baseKey));
                }
                case "$null" -> {
                    boolean isNull = Boolean.parseBoolean(val);
                    predicates.add(alias + "." + fieldPath + (isNull ? " IS NULL" : " IS NOT NULL"));
                }
                default -> {
                    // equality
                    predicates.add(alias + "." + fieldPath + " = :" + param);
                    qp.put(param, castValue(val, baseKey));
                }
            }
        }
        
        return String.join(" AND ", predicates);
    }
    
    
    // --- Helper: determine if key belongs to owner ---
    private static boolean ownerField(String baseKey) {
        return OWNER_FIELDS.contains(baseKey);
    }
    
    // --- Helper: determine if field is part of the composite key ---
    private static boolean isIdField(String baseKey) {
        return "noticeNo".equals(baseKey) || "ownerDriverIndicator".equals(baseKey);
    }
    
    private static boolean isOwnerFieldKey(String key) {
        String k = baseField(key);
        return OWNER_FIELDS.contains(k);
    }
    
    // --- Helper: parse base field and operator ---
    private static String baseField(String key) {
        // remove $or[index]. prefix if present
        key = stripOrPrefix(key);
        int i = key.indexOf('[');
        return i > 0 ? key.substring(0, i) : key;
    }
    
    private static String operatorOf(String key) {
        int i = key.indexOf('[');
        int j = key.indexOf(']');
        if (i > 0 && j > i) return key.substring(i + 1, j); // returns like $in, $gte
        return "";
    }
    
    private static String stripOrPrefix(String key) {
        Matcher m = OR_KEY.matcher(key);
        if (m.matches()) return m.group(2); // drop $or[N].
        return key;
    }
    
    // --- Helper: read params ---
    private static int intParam(Map<String, String[]> p, String key, int def) {
        try { return Integer.parseInt(first(p.get(key))); } catch (Exception e) { return def; }
    }
    
    private static String first(String[] arr) { return (arr == null || arr.length == 0) ? null : arr[0]; }
    
    private static List<String> splitCsv(String s) {
        if (s == null) return Collections.emptyList();
        return Arrays.asList(s.split(",", -1)); // keep empty tokens
    }
    
    /** Cast strings to appropriate Java types based on entity field types */
    private Object castValue(String v, String fieldName) {
        if (v == null) return null;
        
        // Check entity field type if fieldName is provided
        if (fieldName != null) {
            Class<?> fieldType = getFieldType(fieldName);
            if (fieldType != null) {
                // If field is a String in the entity, keep it as a string
                if (String.class.equals(fieldType)) {
                    return v;
                }
                
                // If field is a numeric type, try to convert to that type
                if (Integer.class.equals(fieldType) || int.class.equals(fieldType)) {
                    try { return Integer.parseInt(v); } catch (Exception ignored) {}
                }
                if (Long.class.equals(fieldType) || long.class.equals(fieldType)) {
                    try { return Long.parseLong(v); } catch (Exception ignored) {}
                }
                if (Double.class.equals(fieldType) || double.class.equals(fieldType)) {
                    try { return Double.parseDouble(v); } catch (Exception ignored) {}
                }
                if (BigDecimal.class.equals(fieldType)) {
                    try { return new BigDecimal(v); } catch (Exception ignored) {}
                }
                if (Boolean.class.equals(fieldType) || boolean.class.equals(fieldType)) {
                    if ("true".equalsIgnoreCase(v) || "false".equalsIgnoreCase(v)) {
                        return Boolean.parseBoolean(v);
                    }
                }
                // Handle date/time types
                if (LocalDateTime.class.equals(fieldType) || 
                    LocalDate.class.equals(fieldType) ||
                    ZonedDateTime.class.equals(fieldType) ||
                    OffsetDateTime.class.equals(fieldType)) {
                    return parseDateTime(v);
                }
            }
        }

        // Fallback to original behavior if field type is unknown
        Object dateVal = parseDateTime(v);
        if (dateVal != null) return dateVal;
        
        // Booleans / numbers
        if ("true".equalsIgnoreCase(v) || "false".equalsIgnoreCase(v)) return Boolean.parseBoolean(v);
        
        // Check if the value is an alphanumeric ID (contains both letters and numbers)
        if (containsLettersAndNumbers(v)) {
            return v; // Keep alphanumeric IDs as strings
        }
        
        try { return Long.parseLong(v); } catch (Exception ignored) {}
        try { return Double.parseDouble(v); } catch (Exception ignored) {}

        // Fallback: keep as string
        return v;
    }
    
    /**
     * Check if a string contains both letters and numbers
     */
    private boolean containsLettersAndNumbers(String s) {
        boolean hasLetters = false;
        boolean hasNumbers = false;
        
        for (char c : s.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetters = true;
            } else if (Character.isDigit(c)) {
                hasNumbers = true;
            }
            
            if (hasLetters && hasNumbers) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Parse a string as a date/time value
     */
    private Object parseDateTime(String v) {
        if (v == null) return null;
        
        // Try Offset/Zoned datetime (e.g., 2025-08-10T00:00:00+08:00 or with 'Z')
        try {
            return OffsetDateTime.parse(v).atZoneSameInstant(APP_ZONE).toLocalDateTime();
        } catch (Exception ignored) {}

        // Try local datetime (e.g., 2025-08-10T00:00:00)
        try {
            return LocalDateTime.parse(v, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception ignored) {}

        // Try local date (promote to start of day)
        try {
            LocalDate d = LocalDate.parse(v, DateTimeFormatter.ISO_LOCAL_DATE);
            return d.atStartOfDay();
        } catch (Exception ignored) {}
        
        return null;
    }
    
    /**
     * Get the field type from the entity classes
     */
    private Class<?> getFieldType(String fieldName) {
        // Try to find the field in OcmsValidOffenceNotice class
        try {
            Field field = OcmsValidOffenceNotice.class.getDeclaredField(fieldName);
            return field.getType();
        } catch (NoSuchFieldException e) {
            // Field not found in OcmsValidOffenceNotice, try OcmsOffenceNoticeOwnerDriver
            if (ownerField(fieldName)) {
                try {
                    Field field = OcmsOffenceNoticeOwnerDriver.class.getDeclaredField(fieldName);
                    return field.getType();
                } catch (NoSuchFieldException ex) {
                    // Field not found in either class
                    return null;
                }
            }
        }
        return null;
    }
    
    // Removed unused method: castValue(String)
    // This was previously used for backward compatibility but is no longer needed
    
    /**
    * Convert notice and owner/driver entities to DTO
    */
    private OffenceNoticeWithOwnerDto toDto(OcmsValidOffenceNotice notice, OcmsOffenceNoticeOwnerDriver owner) {
        OffenceNoticeWithOwnerDto dto = new OffenceNoticeWithOwnerDto(notice);
        
        // Add owner/driver fields if available
        if (owner != null) {
            dto.setIdType(owner.getIdType());
            dto.setIdNo(owner.getIdNo()); // Updated from getNricNo to getIdNo
            dto.setName(owner.getName());
            dto.setOffenderIndicator(owner.getOffenderIndicator());
            dto.setOwnerDriverIndicator(owner.getOwnerDriverIndicator());
        } 
        
        return dto;
    }
    
    // --- Special key detection ---
    private static boolean isSpecialParam(String key) {
        if (key == null) return false;
        if (key.startsWith("$")) return true; // $skip, $limit, $field, $sort[], $or[]
        return false;
    }
    
    // --- Sort parser ---
    private static Map<String,Integer> sortParams(Map<String,String[]> p) {
        Map<String,Integer> out = new LinkedHashMap<>();
        p.forEach((k,v) -> {
            if (k.startsWith("$sort[") && k.endsWith("]")) {
                String field = k.substring(6, k.length()-1);
                int dir = 1;
                try { if (v != null && v.length > 0) dir = Integer.parseInt(v[0]); } catch (Exception ignored) {}
                out.put(field, dir == -1 ? -1 : 1);
            }
        });
        return out;
    }
    
    // --- Extract $or blocks back into a list of key→value maps ---
    private static List<Map<String,String>> extractOrBlocks(Map<String,String[]> p) {
        Map<Integer, Map<String,String>> tmp = new TreeMap<>();
        p.forEach((k,v) -> {
            Matcher m = OR_KEY.matcher(k);
            if (m.matches()) {
                int idx = Integer.parseInt(m.group(1));
                String subKey = m.group(2);
                tmp.computeIfAbsent(idx, __ -> new LinkedHashMap<>())
                .put(subKey, first(v));
            }
        });
        return new ArrayList<>(tmp.values());
    }

    /**
     * Find notices by vehicle number
     */
    @Override
    public List<OcmsValidOffenceNotice> findByVehicleNo(String vehicleNo) {
        return retryService.executeWithRetry(() -> repository.findByVehicleNo(vehicleNo));
    }

    /**
     * Find notices by last processing stage
     */
    @Override
    public List<OcmsValidOffenceNotice> findByLastProcessingStage(String lastProcessingStage) {
        return retryService.executeWithRetry(() -> repository.findByLastProcessingStage(lastProcessingStage));
    }

    /**
     * Find notices by last processing stage and date
     */
    @Override
    public List<OcmsValidOffenceNotice> findByLastProcessingStageAndDate(String lastProcessingStage, LocalDate date) {
        return retryService.executeWithRetry(() ->
            repository.findByLastProcessingStageAndDate(lastProcessingStage, date));
    }
}
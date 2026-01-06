package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public class NoticeQueryRequest {
    @JsonProperty("$skip")
    private Integer skip = 0;

    @JsonProperty("$limit")
    private Integer limit = 20;

    @JsonProperty("$field")
    private String fieldCsv;

    @JsonProperty("$or")
    private List<Map<String, Object>> orConditions = new ArrayList<>();

    private final Map<String, Integer> sort = new LinkedHashMap<>();
    private final Map<String, Object> filters = new LinkedHashMap<>();

    private static final Pattern SORT_KEY = Pattern.compile("^\\$sort\\[(.+)]$");

    @JsonAnySetter
    public void onUnknown(String key, Object value) {
        Matcher m = SORT_KEY.matcher(key);
        if (m.matches()) {
            String f = m.group(1);
            Integer dir = coerceToInt(value, 1);
            if (dir != null && (dir == 1 || dir == -1)) sort.put(f, dir);
            return;
        }
        filters.put(key, value);
    }

    public static boolean looksLikeIsoDateTime(String s) {
        try { OffsetDateTime.parse(s); return true; } catch (DateTimeParseException ignored) { return false; }
    }

    public static Integer coerceToInt(Object v, Integer def) {
        if (v == null) return def;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return def; }
    }
}
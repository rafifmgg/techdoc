package com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.normalizer;

import org.springframework.stereotype.Component;

import com.ocmsintranet.apiservice.crud.ocmsizdb.OcmsValidOffenceNotice.dto.NoticeQueryRequest;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class DefaultNoticeQueryNormalizer implements QueryParamNormalizer {

    @Override
    public Map<String, String[]> toNormalizedParamMap(NoticeQueryRequest req) {
        if (req == null) req = new NoticeQueryRequest();
        Map<String, String[]> out = new LinkedHashMap<>();

        // Pagination
        out.put("$skip", new String[]{String.valueOf(Optional.ofNullable(req.getSkip()).orElse(0))});
        out.put("$limit", new String[]{String.valueOf(Optional.ofNullable(req.getLimit()).orElse(20))});

        // Projection
        if (req.getFieldCsv() != null && !req.getFieldCsv().isBlank()) {
            out.put("$field", new String[]{req.getFieldCsv()});
        }

        // Sort
        req.getSort().forEach((k,v) -> out.put("$sort["+k+"]", new String[]{String.valueOf(v)}));

        // Filters
        flatten("", req.getFilters(), out);

        // $or blocks
        for (int i = 0; i < req.getOrConditions().size(); i++) {
            flatten("$or["+i+"].", req.getOrConditions().get(i), out);
        }

        return out;
    }

    private static void flatten(String prefix, Map<String, Object> src, Map<String, String[]> out) {
        if (src == null) return;
        for (Map.Entry<String, Object> e : src.entrySet()) {
            String key = prefix + e.getKey();
            Object val = e.getValue();
            if (val == null) {
                out.put(key, new String[]{""});
            } else if (val instanceof Collection<?>) {
                List<String> parts = ((Collection<?>) val).stream().map(DefaultNoticeQueryNormalizer::s).collect(Collectors.toList());
                out.put(key, new String[]{String.join(",", parts)});
            } else if (val.getClass().isArray()) {
                int len = java.lang.reflect.Array.getLength(val);
                List<String> parts = new ArrayList<>(len);
                for (int i=0;i<len;i++) parts.add(s(java.lang.reflect.Array.get(val,i)));
                out.put(key, new String[]{String.join(",", parts)});
            } else if (val instanceof Map<?,?> m) {
                @SuppressWarnings("unchecked") Map<String,Object> nested = (Map<String,Object>) m;
                flatten(key+".", nested, out);
            } else {
                out.put(key, new String[]{s(val)});
            }
        }
    }

    private static String s(Object v) {
        if (v == null) return "";
        if (v instanceof String str) return str;
        if (v instanceof Number n) return String.valueOf(n);
        if (v instanceof Boolean b) return String.valueOf(b);
        return String.valueOf(v);
    }
}
package com.ocmseservice.apiservice.workflows.spcp.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for application cache configuration
 */
@Slf4j
@UtilityClass
public class AppCacheUtil {

    private static ConcurrentHashMap<String, String> cacheMap = new ConcurrentHashMap<>();

    /**
     * APIM header constant
     */
    public static final String APIM_HEADER = "Ocp-Apim-Subscription-Key";
    
    /**
     * SPCP APIM key constant
     */
    public static final String APIM_SPMS_KEY = "spms-spcpds-apim-subscription";
    
    /**
     * App ID parameter name
     */
    public static final String SPMS_GCC_APPID_KEY = "appId";
    
    /**
     * Session ID parameter name
     */
    public static final String SPMS_GCC_SESSIONID_KEY = "sessionId";
    
    /**
     * Auth transaction ID parameter name
     */
    public static final String SPMS_GCC_AUTH_TXN_ID_KEY = "authTxnId";

    /**
     * Retrieve config value by key
     *
     * @param key key
     * @return The associated key's value
     */
    public static String getValue(String key) {
        return get(key);
    }

    /**
     * Retrieve config value by key
     *
     * @param key key
     * @return The associated key's value
     */
    private static String get(String key) {
        String result = null;
        if (!cacheMap.isEmpty() && StringUtils.isNotEmpty(cacheMap.get(key)))
            result = cacheMap.get(key);

        return result;
    }

    /**
     * Reset and use newValues as new config
     *
     * @param newValues new values
     */
    public static void reload(Map<String, String> newValues) {
        if (!cacheMap.isEmpty())
            cacheMap.clear();

        cacheMap.putAll(newValues);
    }

    /**
     * Put new entry with the provide key and value pair
     *
     * @param key   key
     * @param value value
     */
    public static void put(String key, String value) {
        cacheMap.put(key, value);
    }
}

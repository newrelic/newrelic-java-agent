package com.nr.instrumentation.kafka.streams;

import java.util.concurrent.ConcurrentHashMap;

public class ClientIdToAppIdWithSuffixMap {
    private static final ConcurrentHashMap<String, String> applicationIdMap = new ConcurrentHashMap<>();

    public ClientIdToAppIdWithSuffixMap() {}

    public static ConcurrentHashMap<String, String> get() {
        return applicationIdMap;
    }

    public static String getAppIdOrDefault(String clientId, String defaultId) {
        return applicationIdMap.getOrDefault(clientId, defaultId);
    }

    public static void put(String clientId, String applicationId) {
        applicationIdMap.put(clientId, applicationId);
    }

    public static void remove(String clientId) {
        applicationIdMap.remove(clientId);
    }
}

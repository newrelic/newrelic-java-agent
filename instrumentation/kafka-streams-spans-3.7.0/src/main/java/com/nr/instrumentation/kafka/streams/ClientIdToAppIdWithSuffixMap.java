/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.instrumentation.kafka.streams;

import java.util.concurrent.ConcurrentHashMap;

// A global concurrent hashmap that maps client ids to application id with a possible suffix containing a configured client id.
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

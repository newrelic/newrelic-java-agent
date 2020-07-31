/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent.weaver.internal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public enum WeavePackageType {


    INTERNAL("/Internal"), // Internal weave packages
    FIELD("/Field"), // FIT team weave packages
    CUSTOM("/Custom"), // Custom weave packages
    UNKNOWN("/API"); // Direct API usage

    private static final String SUPPORTABILITY_API = "Supportability/API/";

    private final String supportabilityPostfix;
    private final ConcurrentMap<String, String> supportabilityCache;

    WeavePackageType(String supportabilityPostfix) {
        this.supportabilityPostfix = supportabilityPostfix;
        this.supportabilityCache = new ConcurrentHashMap<>();
    }

    public String getSupportabilityMetric(String supportabilityPrefix) {
        String result = supportabilityCache.get(supportabilityPrefix);
        if (result == null) {
            // Example: Supportability/API/Token/Field
            result = SUPPORTABILITY_API + supportabilityPrefix + supportabilityPostfix;
            supportabilityCache.putIfAbsent(supportabilityPrefix, result);
        }
        return result;
    }

    // Used to exclude Internal API calls from Supportability metrics
    public boolean isInternal() {
        boolean isInternal;
        switch (this) {
            case INTERNAL:
                isInternal = true;
                break;
            default:
                isInternal = false;
                break;
        }
        return isInternal;
    }
}

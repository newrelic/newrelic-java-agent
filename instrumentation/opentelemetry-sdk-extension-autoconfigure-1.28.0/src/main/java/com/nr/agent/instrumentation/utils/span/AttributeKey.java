/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation.utils.span;

public class AttributeKey {
    private final String key;
    private final String semanticConvention;
    private final String version;

    AttributeKey(String key, String semanticConventionString) {
        this.key = key;
        String [] conventionNameAndVersion = semanticConventionString.split(":");
        this.semanticConvention = conventionNameAndVersion[0];
        this.version = conventionNameAndVersion[1];
    }

    public String getKey() {
        return key;
    }

    public String getVersion() {
        return version;
    }

    public String getSemanticConvention() {
        return semanticConvention;
    }
}

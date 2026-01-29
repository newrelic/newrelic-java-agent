/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation.utils.span;

import java.util.regex.Pattern;

public class AttributeKey {
    private static final Pattern SEMANTIC_CONVENTION_SPLIT = Pattern.compile(",");

    private final String key;
    private final String [] semanticConventions;

    AttributeKey(String key, String semanticConventionString) {
        this.key = key;
        this.semanticConventions = SEMANTIC_CONVENTION_SPLIT.split(semanticConventionString);
    }

    public String getKey() {
        return key;
    }


    public String [] getSemanticConventions() {
        return semanticConventions;
    }
}

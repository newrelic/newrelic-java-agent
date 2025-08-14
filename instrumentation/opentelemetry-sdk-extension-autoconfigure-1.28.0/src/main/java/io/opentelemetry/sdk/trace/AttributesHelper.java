/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.sdk.trace;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;

import java.util.Map;

/**
 * Helper class for adding attributes to Spans
 */
public class AttributesHelper {
    private AttributesHelper() {
    }

    public static Attributes toAttributes(Map<String, Object> attributes) {
        AttributesBuilder builder = Attributes.builder();
        attributes.forEach((key, value) -> {
            if (value instanceof String) {
                builder.put(key, (String) value);
            } else if (value instanceof Float || value instanceof Double) {
                builder.put(key, ((Number) value).doubleValue());
            } else if (value instanceof Number) {
                builder.put(key, ((Number) value).longValue());
            }
        });
        return builder.build();
    }
}

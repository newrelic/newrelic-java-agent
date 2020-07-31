/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeepMapClone {
    /**
     * Recursively deep-copies a {@link Map} that contains only {@link Map}s, {@link List}s, {@link String}s, and primitives.
     * Any type outside these bounds is moved over as a reference.
     */
    public static Map<String, Object> deepCopy(Map<String, Object> settings) {
        Map<String, Object> result = new HashMap<>();
        if (settings == null) {
            return result;
        }
        for (Map.Entry<String, Object> entry : settings.entrySet()) {
            result.put(entry.getKey(), cloneElement(entry.getValue()));
        }
        return result;
    }

    private static List<Object> deepCopy(List<?> val) {
        List<Object> result = new ArrayList<>(val.size());
        for(Object element : val) {
            result.add(cloneElement(element));
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private static Object cloneElement(Object element) {
        if (element instanceof Map<?, ?>) {
            return deepCopy((Map<String, Object>) element);
        } else if (element instanceof List<?>) {
            return deepCopy((List<?>) element);
        }
        return element;
    }
}

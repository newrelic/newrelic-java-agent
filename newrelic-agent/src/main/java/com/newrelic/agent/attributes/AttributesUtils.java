/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class AttributesUtils {

    public static Map<String, String> appendAttributePrefixes(Map<String, Map<String, String>> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> toReturn = new HashMap<>();
        String prefix;
        Map<String, String> attributes;
        for (Entry<String, Map<String, String>> current : input.entrySet()) {
            prefix = current.getKey();
            attributes = current.getValue();
            if (attributes != null) {
                for (Entry<String, String> att : attributes.entrySet()) {
                    toReturn.put(prefix + att.getKey(), att.getValue());
                }
            }
        }
        return toReturn;
    }

}

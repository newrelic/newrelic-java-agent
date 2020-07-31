/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ConfigHelper {

    public static Map<String, Object> buildConfigMap(Map<String, Object> settingsMap) {
        Iterator<Map.Entry<String, Object>> it = settingsMap.entrySet().iterator();
        Map<String, Object> configuration = new HashMap<>();

        while (it.hasNext()){
            Map.Entry<String, Object> pair = it.next();
            configuration = buildConfigurationMap(configuration, pair.getKey(), pair.getValue());
        }
        return configuration;
    }


    private static Map<String, Object> buildConfigurationMap(Map<String, Object> existingConfig, String configurationString, Object value) {
        Map<String, Object> configMap = (existingConfig == null) ? new HashMap<String, Object>() : existingConfig;

        String[] split = configurationString.split(":");
        Map<String, Object> currentMap = configMap;

        for (int i = 0; i < split.length - 1; i++) {
            String key = split[i];

            if (!currentMap.containsKey(key)) {
                Map<String, Object> newMap = new HashMap<>();
                currentMap.put(key, newMap);
            }

            currentMap = (Map<String, Object>) currentMap.get(key);
        }

        currentMap.put(split[split.length - 1], value);
        return configMap;

    }
}

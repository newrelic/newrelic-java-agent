/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.bridge.logging;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.NewField;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.logging.Level;

public class Log4jUtils {

    private static final Map<Object, Object> linkingMetadataReflectFieldCache = AgentBridge.collectionFactory.createConcurrentWeakKeyedMap();

    private static final Map<Object, LinkingMetadataHolder> logEventToLinkingMetadataCache = AgentBridge.collectionFactory.createConcurrentWeakKeyedMap();

    public static void addLinkingMetadataToCache(Object logEvent, LinkingMetadataHolder metadata) {
        NewRelic.getAgent().getLogger().log(Level.INFO, "metadata add:: {0}", logEvent.toString());
        logEventToLinkingMetadataCache.put(logEvent, metadata);
    }

    public static LinkingMetadataHolder getLinkingMetadataFromCache(Object logEvent) {
        NewRelic.getAgent().getLogger().log(Level.INFO, "metadata get:: {0}", logEvent.toString());
        return logEventToLinkingMetadataCache.get(logEvent);
    }

    public static void removeLinkingMetadataFromCache(Object logEvent) {
        NewRelic.getAgent().getLogger().log(Level.INFO, "metadata remove:: {0}", logEvent.toString());
        logEventToLinkingMetadataCache.remove(logEvent);
    }

    /**
     * Gets the agent linking metadata from a LogEvent from Log4j.
     * This method relies on reflection to search for a new field from our Log4j 2.11+ instrumentation.
     *
     * @param logEvent an instrumented LogEvent instance
     * @return an opaque map of strings to strings
     */
    public static Map<String, String> getLinkingMetadata(Object logEvent) {
        if (logEvent == null) {
            return null;
        }
        Class<?> c = logEvent.getClass();

        Field cachedField = getFieldFromCache(c);

        if (cachedField != null) {
            return getLinkingMetadata(logEvent, cachedField);
        }

        Field[] fieldList = c.getFields();
        for (Field field : fieldList) {
            // Check if agentLinkingMetadata exists in LogEvent (instrumented in instrumentation:apache-log4j-2.11 and above)
            if (field.getAnnotationsByType(NewField.class).length != 0 && field.getName().equals("agentLinkingMetadata")) {
                Map<String, String> metadata = getLinkingMetadata(logEvent, field);
                if (metadata != null) {
                    linkingMetadataReflectFieldCache.put(c, field);
                    return metadata;
                }
            }
        }
        AgentBridge.getAgent().getLogger().log(Level.FINEST, "No linking metadata found from log4j's LogEvent instance {0}", logEvent);
        return null;
    }

    private static Field getFieldFromCache(Class<?> c) {
        try {
            Object v = linkingMetadataReflectFieldCache.get(c);
            if (v != null) {
                return (Field) v;
            }
        } catch (NullPointerException | ClassCastException ignored) {
        }
        return null;
    }

    private static Map<String, String> getLinkingMetadata(Object logEvent, Field field) {
        try {
            return (Map<String, String>) field.get(logEvent);
        } catch (IllegalAccessException | ClassCastException e) {
            AgentBridge.getAgent().getLogger().log(Level.FINEST,
                    "Exception from getting linking metadata from log4j's LogEvent instance {0}", logEvent, e);
            return null;
        }
    }
}

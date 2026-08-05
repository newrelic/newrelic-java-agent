/*
 * Copyright 2025 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.nr.instrumentation.kafka;

import com.newrelic.api.agent.NewRelic;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class ClusterIdHelper {

    private ClusterIdHelper() {}

    // A class's field/method structure never changes at runtime, so these reflective
    // handles are safe to cache per concrete class for the life of the JVM. This only
    // caches *where* to look, not the cluster id value itself — callers still read a
    // fresh value on every invocation, so a producer/consumer whose metadata hasn't
    // finished its first refresh yet keeps retrying the actual read, just without
    // re-walking the class hierarchy and re-resolving methods on every attempt.
    private static final ConcurrentHashMap<Class<?>, Field> METADATA_FIELD_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Method> FETCH_METHOD_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Method> CLUSTER_RESOURCE_METHOD_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Method> CLUSTER_ID_METHOD_CACHE = new ConcurrentHashMap<>();

    public static String fromProducer(Object producer) {
        return fromMetadataField(producer);
    }

    private static String fromMetadataField(Object obj) {
        try {
            Field metaField = METADATA_FIELD_CACHE.computeIfAbsent(obj.getClass(), ClusterIdHelper::findMetadataField);
            if (metaField == null) return null;
            Object meta = metaField.get(obj);
            if (meta == null) return null;

            Method fetchMethod = FETCH_METHOD_CACHE.computeIfAbsent(meta.getClass(), ClusterIdHelper::findFetchMethod);
            if (fetchMethod == null) return null;
            Object cluster = fetchMethod.invoke(meta);
            if (cluster == null) return null;

            Method crMethod = CLUSTER_RESOURCE_METHOD_CACHE.computeIfAbsent(cluster.getClass(), ClusterIdHelper::findClusterResourceMethod);
            if (crMethod == null) return null;
            Object cr = crMethod.invoke(cluster);
            if (cr == null) return null;

            Method clusterIdMethod = CLUSTER_ID_METHOD_CACHE.computeIfAbsent(cr.getClass(), ClusterIdHelper::findClusterIdMethod);
            if (clusterIdMethod == null) return null;
            String id = (String) clusterIdMethod.invoke(cr);
            if (id != null && !id.isEmpty()) {
                return id;
            }
        } catch (Exception e) { NewRelic.getAgent().getLogger().log(Level.FINEST, e, "NR Kafka cluster ID fetch failed"); }
        return null;
    }

    private static Field findMetadataField(Class<?> cls) {
        Class<?> c = cls;
        while (c != null && c != Object.class) {
            try {
                Field field = c.getDeclaredField("metadata");
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        return null;
    }

    private static Method findFetchMethod(Class<?> cls) {
        try {
            return cls.getMethod("fetch");
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static Method findClusterResourceMethod(Class<?> cls) {
        try {
            return cls.getMethod("clusterResource");
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static Method findClusterIdMethod(Class<?> cls) {
        try {
            return cls.getMethod("clusterId");
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}

/*
 * Copyright 2025 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.nr.instrumentation.kafka;

import com.newrelic.api.agent.NewRelic;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;

public final class ClusterIdHelper {

    private ClusterIdHelper() {}

    public static String fromProducer(Object producer) {
        return fromMetadataField(producer);
    }

    public static String fromConsumer(Object consumer) {
        return fromMetadataField(consumer);
    }

    private static String fromMetadataField(Object obj) {
        try {
            Field metaField = findField(obj.getClass(), "metadata");
            if (metaField == null) return null;
            metaField.setAccessible(true);
            Object meta = metaField.get(obj);
            if (meta == null) return null;
            Method fetchMethod = meta.getClass().getMethod("fetch");
            Object cluster = fetchMethod.invoke(meta);
            if (cluster == null) return null;
            Method crMethod = cluster.getClass().getMethod("clusterResource");
            Object cr = crMethod.invoke(cluster);
            if (cr != null) {
                Method clusterIdMethod = cr.getClass().getMethod("clusterId");
                String id = (String) clusterIdMethod.invoke(cr);
                if (id != null && !id.isEmpty()) {
                    return id;
                }
            }
        } catch (Exception e) { NewRelic.getAgent().getLogger().log(Level.FINEST, e, "NR Kafka cluster ID fetch failed"); }
        return null;
    }

    private static Field findField(Class<?> cls, String name) {
        while (cls != null && cls != Object.class) {
            try {
                return cls.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                cls = cls.getSuperclass();
            }
        }
        return null;
    }
}

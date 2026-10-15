/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport.otlp;

import com.newrelic.agent.Agent;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.InstrumentationScope;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.resource.v1.Resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds OTLP messages shared by every signal: {@code KeyValue}, {@code AnyValue}, {@code Resource} and
 * {@code InstrumentationScope} (see opentelemetry/proto/common/v1/common.proto and resource/v1/resource.proto).
 */
final class OtlpAttributes {

    static final int TRACE_ID_BYTES = 16;
    static final int SPAN_ID_BYTES = 8;

    static final String SCOPE_NAME = "newrelic-java-agent";
    static final String SERVICE_NAME = "service.name";

    private OtlpAttributes() {
    }

    /**
     * Builds a {@code KeyValue} for every entry of the map, skipping null maps, null keys, null values and excluded keys.
     */
    static List<KeyValue> buildAttributes(Map<String, ?> attributes, Set<String> excludedKeys) {
        if (attributes == null) {
            return Collections.emptyList();
        }
        List<KeyValue> result = new ArrayList<>(attributes.size());
        for (Map.Entry<String, ?> entry : attributes.entrySet()) {
            if (!excludedKeys.contains(entry.getKey())) {
                KeyValue attribute = buildAttribute(entry.getKey(), entry.getValue());
                if (attribute != null) {
                    result.add(attribute);
                }
            }
        }
        return result;
    }

    static KeyValue buildAttribute(String key, Object value) {
        if (key == null || value == null) {
            return null;
        }
        return KeyValue.newBuilder().setKey(key).setValue(buildAnyValue(value)).build();
    }

    /**
     * Builds the fields of an {@code AnyValue}. Values that aren't strings, booleans or numbers are written as strings,
     * which matches how they are serialized to JSON for the collector.
     */
    static AnyValue buildAnyValue(Object value) {
        if (value instanceof String) {
            return AnyValue.newBuilder().setStringValue((String) value).build();
        } else if (value instanceof Boolean) {
            return AnyValue.newBuilder().setBoolValue((Boolean) value).build();
        } else if (value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte) {
            return AnyValue.newBuilder().setIntValue(((Number) value).longValue()).build();
        } else if (value instanceof Number) {
            return AnyValue.newBuilder().setDoubleValue(((Number) value).doubleValue()).build();
        } else {
            return AnyValue.newBuilder().setStringValue(String.valueOf(value)).build();
        }
    }

    static Resource buildResource(Map<String, ?> resourceAttributes) {
        return Resource.newBuilder()
                .addAllAttributes(buildAttributes(resourceAttributes, Collections.<String>emptySet()))
                .build();
    }

    static InstrumentationScope buildScope() {
        InstrumentationScope.Builder builder = InstrumentationScope.newBuilder().setName(SCOPE_NAME);
        String version = Agent.getVersion();
        if (version != null) {
            builder.setVersion(version);
        }
        return builder.build();
    }

    /**
     * Converts a hex id (e.g. a trace id or span guid) to the fixed size byte array OTLP requires. Shorter ids are left
     * padded with zeros, which matches how W3C trace context handles them.
     *
     * @return the id bytes, or null if the id is missing, too long, not hex, or all zeros (which OTLP treats as invalid)
     */
    static byte[] hexToBytes(String hex, int byteLength) {
        if (hex == null || hex.isEmpty() || hex.length() > byteLength * 2) {
            return null;
        }
        int padding = byteLength * 2 - hex.length();
        byte[] result = new byte[byteLength];
        boolean allZeros = true;
        for (int i = 0; i < byteLength; i++) {
            int high = hexDigit(hex, 2 * i - padding);
            int low = hexDigit(hex, 2 * i + 1 - padding);
            if (high < 0 || low < 0) {
                return null;
            }
            result[i] = (byte) ((high << 4) | low);
            allZeros &= result[i] == 0;
        }
        return allZeros ? null : result;
    }

    // Positions before the start of the string are padding and read as zero
    private static int hexDigit(String hex, int index) {
        return index < 0 ? 0 : Character.digit(hex.charAt(index), 16);
    }
}

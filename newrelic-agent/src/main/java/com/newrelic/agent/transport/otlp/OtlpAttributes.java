/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport.otlp;

import com.newrelic.agent.Agent;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Encodes OTLP messages shared by every signal: {@code KeyValue}, {@code AnyValue}, {@code Resource} and
 * {@code InstrumentationScope} (see opentelemetry/proto/common/v1/common.proto and resource/v1/resource.proto).
 */
final class OtlpAttributes {

    static final int TRACE_ID_BYTES = 16;
    static final int SPAN_ID_BYTES = 8;

    static final String SCOPE_NAME = "newrelic-java-agent";
    static final String SERVICE_NAME = "service.name";

    // KeyValue
    private static final int KEY_VALUE_KEY = 1;
    private static final int KEY_VALUE_VALUE = 2;

    // AnyValue
    private static final int ANY_VALUE_STRING = 1;
    private static final int ANY_VALUE_BOOL = 2;
    private static final int ANY_VALUE_INT = 3;
    private static final int ANY_VALUE_DOUBLE = 4;

    // Resource
    private static final int RESOURCE_ATTRIBUTES = 1;

    // InstrumentationScope
    private static final int SCOPE_NAME_FIELD = 1;
    private static final int SCOPE_VERSION_FIELD = 2;

    private OtlpAttributes() {
    }

    /**
     * Writes every entry of the map as a {@code KeyValue} field, skipping null maps, null keys, null values and excluded keys.
     */
    static void writeAttributes(ProtobufWriter writer, int fieldNumber, Map<String, ?> attributes, Set<String> excludedKeys) {
        if (attributes == null) {
            return;
        }
        for (Map.Entry<String, ?> entry : attributes.entrySet()) {
            if (!excludedKeys.contains(entry.getKey())) {
                writeAttribute(writer, fieldNumber, entry.getKey(), entry.getValue());
            }
        }
    }

    static void writeAttribute(ProtobufWriter writer, int fieldNumber, String key, Object value) {
        if (key == null || value == null) {
            return;
        }
        writer.startMessage(fieldNumber);
        writer.writeString(KEY_VALUE_KEY, key);
        writer.startMessage(KEY_VALUE_VALUE);
        writeAnyValue(writer, value);
        writer.endMessage();
        writer.endMessage();
    }

    /**
     * Writes the fields of an {@code AnyValue}. Values that aren't strings, booleans or numbers are written as strings,
     * which matches how they are serialized to JSON for the collector.
     */
    static void writeAnyValue(ProtobufWriter writer, Object value) {
        if (value instanceof String) {
            writer.writeString(ANY_VALUE_STRING, (String) value);
        } else if (value instanceof Boolean) {
            writer.writeBool(ANY_VALUE_BOOL, (Boolean) value);
        } else if (value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte) {
            writer.writeVarint(ANY_VALUE_INT, ((Number) value).longValue());
        } else if (value instanceof Number) {
            writer.writeDouble(ANY_VALUE_DOUBLE, ((Number) value).doubleValue());
        } else {
            writer.writeString(ANY_VALUE_STRING, String.valueOf(value));
        }
    }

    static void writeResource(ProtobufWriter writer, int fieldNumber, Map<String, ?> resourceAttributes) {
        writer.startMessage(fieldNumber);
        writeAttributes(writer, RESOURCE_ATTRIBUTES, resourceAttributes, Collections.<String>emptySet());
        writer.endMessage();
    }

    static void writeScope(ProtobufWriter writer, int fieldNumber) {
        writer.startMessage(fieldNumber);
        writer.writeString(SCOPE_NAME_FIELD, SCOPE_NAME);
        String version = Agent.getVersion();
        if (version != null) {
            writer.writeString(SCOPE_VERSION_FIELD, version);
        }
        writer.endMessage();
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

/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport.otlp;

import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;

final class OtlpTestUtil {

    private OtlpTestUtil() {
    }

    /**
     * Converts decoded OTLP attributes to a map of plain Java values, failing on duplicate keys.
     */
    static Map<String, Object> toMap(List<KeyValue> attributes) {
        Map<String, Object> result = new HashMap<>();
        for (KeyValue attribute : attributes) {
            assertFalse("Duplicate attribute " + attribute.getKey(), result.containsKey(attribute.getKey()));
            result.put(attribute.getKey(), toJavaValue(attribute.getValue()));
        }
        return result;
    }

    static Object toJavaValue(AnyValue value) {
        switch (value.getValueCase()) {
            case STRING_VALUE:
                return value.getStringValue();
            case BOOL_VALUE:
                return value.getBoolValue();
            case INT_VALUE:
                return value.getIntValue();
            case DOUBLE_VALUE:
                return value.getDoubleValue();
            default:
                throw new AssertionError("Unexpected attribute value type " + value.getValueCase());
        }
    }

    static String toHex(com.google.protobuf.ByteString bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes.toByteArray()) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}

/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport.otlp;

import io.opentelemetry.proto.common.v1.AnyValue;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class OtlpAttributesTest {

    @Test
    public void hexToBytesDecodesFullLengthIds() {
        assertArrayEquals(new byte[] { 0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab, (byte) 0xcd, (byte) 0xef },
                OtlpAttributes.hexToBytes("0123456789abcdef", 8));
    }

    @Test
    public void hexToBytesIsCaseInsensitive() {
        assertArrayEquals(OtlpAttributes.hexToBytes("0123456789abcdef", 8), OtlpAttributes.hexToBytes("0123456789ABCDEF", 8));
    }

    @Test
    public void hexToBytesLeftPadsShortIds() {
        assertArrayEquals(new byte[] { 0, 0, 0, 0, 0, 0, 0x0a, (byte) 0xbc }, OtlpAttributes.hexToBytes("abc", 8));
    }

    @Test
    public void hexToBytesRejectsInvalidIds() {
        assertNull(OtlpAttributes.hexToBytes(null, 8));
        assertNull(OtlpAttributes.hexToBytes("", 8));
        assertNull(OtlpAttributes.hexToBytes("0123456789abcdef0", 8));
        assertNull(OtlpAttributes.hexToBytes("0123456789abcdeg", 8));
        assertNull(OtlpAttributes.hexToBytes("0000000000000000", 8));
    }

    @Test
    public void buildAnyValueMapsIntegersWithinInt64ToIntValue() {
        assertEquals(AnyValue.ValueCase.INT_VALUE, OtlpAttributes.buildAnyValue(3).getValueCase());
        assertEquals(AnyValue.ValueCase.INT_VALUE, OtlpAttributes.buildAnyValue(new AtomicInteger(3)).getValueCase());
        assertEquals(7L, OtlpAttributes.buildAnyValue(new AtomicLong(7L)).getIntValue());
        assertEquals(Long.MAX_VALUE, OtlpAttributes.buildAnyValue(BigInteger.valueOf(Long.MAX_VALUE)).getIntValue());
        assertEquals(Long.MIN_VALUE, OtlpAttributes.buildAnyValue(BigInteger.valueOf(Long.MIN_VALUE)).getIntValue());
    }

    @Test
    public void buildAnyValueMapsIntegersOutsideInt64ToStringValue() {
        BigInteger tooBig = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
        BigInteger tooSmall = BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE);
        assertEquals(tooBig.toString(), OtlpAttributes.buildAnyValue(tooBig).getStringValue());
        assertEquals(tooSmall.toString(), OtlpAttributes.buildAnyValue(tooSmall).getStringValue());
    }

    @Test
    public void buildAnyValueMapsFloatingPointToDoubleValue() {
        assertEquals(AnyValue.ValueCase.DOUBLE_VALUE, OtlpAttributes.buildAnyValue(1.5f).getValueCase());
        assertEquals(AnyValue.ValueCase.DOUBLE_VALUE, OtlpAttributes.buildAnyValue(2.5d).getValueCase());
        assertEquals(12.34d, OtlpAttributes.buildAnyValue(new BigDecimal("12.34")).getDoubleValue(), 0d);
    }

    @Test
    public void nonFiniteNumbersAreOmitted() {
        assertNull(OtlpAttributes.buildAnyValue(Double.NaN));
        assertNull(OtlpAttributes.buildAnyValue(Double.POSITIVE_INFINITY));
        assertNull(OtlpAttributes.buildAnyValue(Float.NEGATIVE_INFINITY));
        assertNull(OtlpAttributes.buildAttribute("key", Double.NaN));
        assertNull(OtlpAttributes.buildAttribute("key", Double.POSITIVE_INFINITY));
        assertEquals(1, OtlpAttributes.buildAttributes(java.util.Collections.<String, Object>singletonMap("key", 1.5d),
                java.util.Collections.<String>emptySet()).size());
        assertEquals(0, OtlpAttributes.buildAttributes(java.util.Collections.<String, Object>singletonMap("key", Double.NaN),
                java.util.Collections.<String>emptySet()).size());
    }
}

/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport.otlp;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
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
}

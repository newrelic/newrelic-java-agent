/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracing;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class W3CTraceParentValidatorTest {

    private final static String VALID_HEADER_55 = "00-12345678123456781234567812345678-1234123412341234-01";

    @Test
    public void testValidVersion55() throws Exception {
        W3CTraceParentValidator validator = W3CTraceParentValidator.forHeader(VALID_HEADER_55)
                .version("00")
                .build();
        assertTrue(validator.isValidVersion());
    }

    @Test
    public void testValidVersion55_withAnyOldHexVersionStringLength2() throws Exception {
        W3CTraceParentValidator validator = W3CTraceParentValidator.forHeader(VALID_HEADER_55)
                .version("af")
                .build();
        assertTrue(validator.isValidVersion());
    }

    @Test
    public void testInvalidVersion_badLength() throws Exception {
        W3CTraceParentValidator validator = W3CTraceParentValidator.forHeader(VALID_HEADER_55)
                .version("beef")
                .build();
        assertFalse(validator.isValidVersion());
    }

    @Test
    public void testInvalidVersion_startsWithNonHex() throws Exception {
        W3CTraceParentValidator validator = W3CTraceParentValidator.forHeader(VALID_HEADER_55)
                .version("ze")
                .build();
        assertFalse(validator.isValidVersion());
    }

    @Test
    public void testInvalidVersion() throws Exception {
        W3CTraceParentValidator validator = W3CTraceParentValidator.forHeader(VALID_HEADER_55)
                .version("ff")
                .build();
        assertFalse(validator.isValidVersion());
    }

    @Test
    public void testValidVersion_headerLengthWrong() throws Exception {
        W3CTraceParentValidator validator = W3CTraceParentValidator.forHeader("bbbbbbb")
                .version("00")
                .build();
        assertFalse(validator.isValidVersion());
    }

    @Test
    public void testValidTraceId() throws Exception {
        String id = "deadbeefdeadbeefdeadbeefdeadbeef";
        W3CTraceParentValidator validator = W3CTraceParentValidator.forHeader(VALID_HEADER_55)
                .traceId(id)
                .build();
        assertTrue(validator.isValidTraceId());
    }

    @Test
    public void testInvalidTraceId_invalidLength() throws Exception {
        String id = "deadbeefdeadbeefdeadbeefdeadbeefaaaaaaaaaa";
        W3CTraceParentValidator validator = W3CTraceParentValidator.forHeader(VALID_HEADER_55)
                .traceId(id)
                .build();
        assertFalse(validator.isValidTraceId());
    }

    @Test
    public void testInvalidTraceId() throws Exception {
        String id = "00000000000000000000000000000000";
        W3CTraceParentValidator validator = W3CTraceParentValidator.forHeader(VALID_HEADER_55)
                .traceId(id)
                .build();
        assertFalse(validator.isValidTraceId());
    }

    @Test
    public void testInvalidTraceId_containsNonHex() throws Exception {
        String id = "deadbeezdeadbeezdeadbeezdeadbeez";
        W3CTraceParentValidator validator = W3CTraceParentValidator.forHeader(VALID_HEADER_55)
                .traceId(id)
                .build();
        assertFalse(validator.isValidTraceId());
    }

    @Test
    public void testValidParentId() throws Exception {
        String id = "deadbeefdeadbeef";
        W3CTraceParentValidator validator = W3CTraceParentValidator.forHeader(VALID_HEADER_55)
                .parentId(id)
                .build();
        assertTrue(validator.isValidParentId());
    }

    @Test
    public void testInvalidParentId() throws Exception {
        String id = "0000000000000000";
        W3CTraceParentValidator validator = W3CTraceParentValidator.forHeader(VALID_HEADER_55)
                .parentId(id)
                .build();
        assertFalse(validator.isValidParentId());
    }

    @Test
    public void testInvalidParentId_badLength() throws Exception {
        String id = "deadbeefdeadbeefaaaaaaaaa";
        W3CTraceParentValidator validator = W3CTraceParentValidator.forHeader(VALID_HEADER_55)
                .parentId(id)
                .build();
        assertFalse(validator.isValidParentId());
    }

    @Test
    public void testInvalidParentId_containsNonHex() throws Exception {
        String id = "deadbeezdeadbeez";
        W3CTraceParentValidator validator = W3CTraceParentValidator.forHeader(VALID_HEADER_55)
                .parentId(id)
                .build();
        assertFalse(validator.isValidParentId());
    }

    @Test
    public void testValidFlags() throws Exception {
        String flags = "01";
        W3CTraceParentValidator validator = W3CTraceParentValidator.forHeader(VALID_HEADER_55)
                .flags(flags)
                .build();
        assertTrue(validator.isValidFlags());
    }

    @Test
    public void testInvalidFlags_badLength() throws Exception {
        String flags = "01a";
        W3CTraceParentValidator validator = W3CTraceParentValidator.forHeader(VALID_HEADER_55)
                .flags(flags)
                .build();
        assertFalse(validator.isValidFlags());
    }

    @Test
    public void testInvalidFlags_containsNonHex() throws Exception {
        String flags = "6z";
        W3CTraceParentValidator validator = W3CTraceParentValidator.forHeader(VALID_HEADER_55)
                .flags(flags)
                .build();
        assertFalse(validator.isValidFlags());
    }
}
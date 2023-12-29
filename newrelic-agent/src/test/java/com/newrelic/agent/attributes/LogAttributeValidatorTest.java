/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.logging.LogSenderServiceImpl;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.servlet.BasicRequestRootTracer;
import com.newrelic.agent.tracers.servlet.MockHttpRequest;
import com.newrelic.agent.tracers.servlet.MockHttpResponse;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LogAttributeValidatorTest {

    final static String ATTRIBUTE_TYPE = "custom";

    @BeforeClass
    public static void beforeClass() {
        MockServiceManager sm = new MockServiceManager();
        ServiceFactory.setServiceManager(sm);
    }

    @Test
    public void testVerifyTruncatedValue() {
        Map<String, Object> input = new HashMap<>();
        String longValue = Strings.padEnd("", 33000, 'e');
        String longExpectedValue = Strings.padEnd("", 32767, 'e');
        input.put("key", longValue);
        input.put("apple", "pie");
        input.put("sugar", "cream");

        Map<String, Object> expected = ImmutableMap.<String, Object>of("apple", "pie", "sugar", "cream", "key", longExpectedValue);

        AttributeValidator attributeValidator = new LogAttributeValidator(ATTRIBUTE_TYPE);
        attributeValidator.setTransactional(false);

        Map<String, Object> result = attributeValidator.verifyParametersAndReturnValues(input, LogSenderServiceImpl.METHOD);

        assertEquals(expected, result);
    }
}

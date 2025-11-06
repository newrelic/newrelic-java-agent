/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
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

public class AttributeValidatorTest {

    final String methodCalled = "method";
    final static String ATTRIBUTE_TYPE = "custom";

    @BeforeClass
    public static void beforeClass() {
        MockServiceManager sm = new MockServiceManager();
        ServiceFactory.setServiceManager(sm);
    }

    @After
    public void after() {
        Transaction.clearTransaction();
    }

    private BasicRequestRootTracer createDispatcherTracer() {
        Transaction tx = Transaction.getTransaction();
        MockHttpRequest httpRequest = new MockHttpRequest();
        MockHttpResponse httpResponse = new MockHttpResponse();
        ClassMethodSignature sig = new ClassMethodSignature(getClass().getName(), "dude", "()V");
        return new BasicRequestRootTracer(tx, sig, this, httpRequest, httpResponse);
    }

    @Test
    public void testVerifyParametersAndReturnValues() {
        Transaction t = Transaction.getTransaction();
        BasicRequestRootTracer tracer = createDispatcherTracer();
        t.getTransactionActivity().tracerStarted(tracer);

        CustomAttributeSender cas = new CustomAttributeSender();
        assertNull(cas.verifyParameterAndReturnValue("NaN", Double.NaN, "dude"));
        assertNull(cas.verifyParameterAndReturnValue("Infinity", Double.POSITIVE_INFINITY, "dude"));
        assertEquals(cas.verifyParameterAndReturnValue("AtomicLong", new AtomicLong(10), "dude"), 10L);
        assertEquals(cas.verifyParameterAndReturnValue("Boolean", true, "dude"), true);
        assertEquals(cas.verifyParameterAndReturnValue("Integer", 10, "dude"), Integer.valueOf(10));
    }

    @Test
    public void testVerifyHappyPath() {
        Map<String, Object> input = new HashMap<>();
        input.put("String", "Wizzy");
        input.put("Number", 12345);
        input.put("Boolean", true);
        input.put("Floating", 999.0989);

        Map<String, Object> expected = new HashMap<>(input);

        AttributeValidator attributeValidator = new AttributeValidator(ATTRIBUTE_TYPE);

        attributeValidator.setTransactional(false);
        Map<String, Object> result = attributeValidator.verifyParametersAndReturnValues(input, methodCalled);

        assertEquals(expected, result);

    }

    @Test
    public void testVerifyNullKey() {
        Map<String, Object> input = new HashMap<>();
        input.put(null, "Wizzy");
        input.put("apple", "pie");
        input.put("sugar", "cream");

        Map<String, Object> expected = ImmutableMap.<String, Object>of("apple", "pie", "sugar", "cream");

        AttributeValidator attributeValidator = new AttributeValidator(ATTRIBUTE_TYPE);

        attributeValidator.setTransactional(false);
        Map<String, Object> result = attributeValidator.verifyParametersAndReturnValues(input, methodCalled);

        assertEquals(expected, result);
    }

    @Test
    public void testVerifyNullValue() {
        Map<String, Object> input = new HashMap<>();
        input.put("cheese", null);
        input.put("apple", "pie");
        input.put("sugar", "cream");

        Map<String, Object> expected = ImmutableMap.<String, Object>of("apple", "pie", "sugar", "cream");

        AttributeValidator attributeValidator = new AttributeValidator(ATTRIBUTE_TYPE);

        attributeValidator.setTransactional(false);
        Map<String, Object> result = attributeValidator.verifyParametersAndReturnValues(input, methodCalled);

        assertEquals(expected, result);
    }

    @Test
    public void testVerifyNullMap() {
        Map<String, Object> expected = Collections.emptyMap();

        AttributeValidator attributeValidator = new AttributeValidator(ATTRIBUTE_TYPE);

        Map<String, Object> result = attributeValidator.verifyParametersAndReturnValues(null, methodCalled);

        assertEquals(expected.size(), result.size());
    }

    @Test
    public void testVerifyInvalidType() {
        Map<String, Object> input = new HashMap<>();
        input.put("cheese", new Object());
        input.put("apple", "pie");
        input.put("sugar", "cream");

        Map<String, Object> expected = ImmutableMap.<String, Object>of("apple", "pie", "sugar", "cream");

        AttributeValidator attributeValidator = new AttributeValidator(ATTRIBUTE_TYPE);

        attributeValidator.setTransactional(false);
        Map<String, Object> result = attributeValidator.verifyParametersAndReturnValues(input, methodCalled);

        assertEquals(expected, result);
    }

    @Test
    public void testVerifyKeyLength() {
        Map<String, Object> input = new HashMap<>();
        String longKey = Strings.padEnd("", 256, 'e');
        input.put(longKey, "the sound a scream makes");
        input.put("apple", "pie");
        input.put("sugar", "cream");

        Map<String, Object> expected = ImmutableMap.<String, Object>of("apple", "pie", "sugar", "cream");

        AttributeValidator attributeValidator = new AttributeValidator(ATTRIBUTE_TYPE);

        attributeValidator.setTransactional(false);
        Map<String, Object> result = attributeValidator.verifyParametersAndReturnValues(input, methodCalled);

        assertEquals(expected, result);
    }

    @Test
    public void testVerifyTruncatedValue() {
        Map<String, Object> input = new HashMap<>();
        String longValue = Strings.padEnd("", 300, 'e');
        String longExpectedValue = Strings.padEnd("", 255, 'e');
        input.put("key", longValue);
        input.put("apple", "pie");
        input.put("sugar", "cream");

        Map<String, Object> expected = ImmutableMap.<String, Object>of("apple", "pie", "sugar", "cream", "key", longExpectedValue);

        AttributeValidator attributeValidator = new AttributeValidator(ATTRIBUTE_TYPE);

        attributeValidator.setTransactional(false);
        Map<String, Object> result = attributeValidator.verifyParametersAndReturnValues(input, methodCalled);

        assertEquals(expected, result);
    }

    @Test
    public void testVerifySendOutsideTxn() {
        String methodCalled = "noticeError";
        Map<String, Object> input = new HashMap<>();
        input.put("key", "value");
        input.put("apple", "pie");
        input.put("sugar", "cream");

        Map<String, Object> expected = new HashMap<>(input);
        AttributeValidator attributeValidator = new AttributeValidator(ATTRIBUTE_TYPE);

        attributeValidator.setTransactional(false);
        Map<String, Object> result = attributeValidator.verifyParametersAndReturnValues(input, methodCalled);

        assertEquals(expected, result);
    }

    @Test
    public void testVerifyTransactionalNoActiveTransaction() {
        Map<String, Object> input = new HashMap<>();
        input.put("String", "Wizzy");
        input.put("Number", 12345);
        input.put("Boolean", true);
        input.put("Floating", 999.0989);

        Map<String, Object> expected = Collections.emptyMap();

        AttributeValidator attributeValidator = new AttributeValidator(ATTRIBUTE_TYPE);

        attributeValidator.setTransactional(true);
        Map<String, Object> result = attributeValidator.verifyParametersAndReturnValues(input, methodCalled);

        assertEquals(expected, result);
    }

    @Test
    public void testVerifyTransactionalWithActiveTransaction() {
        Transaction mockTransaction = mock(Transaction.class);
        when(mockTransaction.isInProgress()).thenReturn(true);
        Transaction.setTransaction(mockTransaction);

        Map<String, Object> input = new HashMap<>();
        input.put("String", "Wizzy");
        input.put("Number", 12345);
        input.put("Boolean", true);
        input.put("Floating", 999.0989);

        Map<String, Object> expected = new HashMap<>(input);

        AttributeValidator attributeValidator = new AttributeValidator(ATTRIBUTE_TYPE);

        attributeValidator.setTransactional(true);
        Map<String, Object> result = attributeValidator.verifyParametersAndReturnValues(input, methodCalled);

        assertEquals(expected, result);
    }

    @Test
    public void testVerifyParameterCapacityWhenNoTransaction() {
        String methodCalled = "noticeError";

        Map<String, Object> input = createAttributesMap(125);
        Map<String, Object> expected = createAttributesMap(64);

        AttributeValidator attributeValidator = new AttributeValidator(ATTRIBUTE_TYPE);

        Map<String, Object> result = attributeValidator.verifyParametersAndReturnValues(input, methodCalled);

        assertEquals(expected.size(), result.size());
    }

    @Test
    public void testVerifyParameterCapacityWhenThereIsTransaction() {
        Transaction mockTransaction = mock(Transaction.class);
        when(mockTransaction.isInProgress()).thenReturn(true);
        Transaction.setTransaction(mockTransaction);

        Map<String, Object> input = createAttributesMap(125);
        Map<String, Object> expected = createAttributesMap(64);

        AttributeValidator attributeValidator = new AttributeValidator(ATTRIBUTE_TYPE );
        attributeValidator.setTransactional(true);

        Map<String, Object> result = attributeValidator.verifyParametersAndReturnValues(input, methodCalled);

        assertEquals(expected.size(), result.size());
    }

    private Map<String, Object> createAttributesMap(int numberOfAttributes) {
        Map<String, Object> atts = new HashMap<>();
        for (int i = 0; i < numberOfAttributes; i++) {
            atts.put("key" + i, "value" + i);
        }
        return atts;
    }
}

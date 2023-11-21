/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.errors;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.config.ErrorCollectorConfig;
import com.newrelic.agent.config.StripExceptionConfigImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.collections.Sets;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TracedErrorTest {
    private static String appName;

    @BeforeClass
    public static void before() throws Exception {
        MockCoreService.getMockAgentAndBootstrapTheServiceManager();
        appName = ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName();
    }

    @AfterClass
    public static void after() throws Exception {
        ServiceManager serviceManager = ServiceFactory.getServiceManager();
        if (serviceManager != null) {
            serviceManager.stop();
        }
        ServiceFactory.setServiceManager(null);
    }

    @Test
    public void httpError() throws Exception {
        ErrorCollectorConfig errorCollectorConfig = ServiceFactory.getConfigService().getDefaultAgentConfig()
                .getErrorCollectorConfig();
        TracedError error = HttpTracedError
                .builder(errorCollectorConfig, appName, "dude", System.currentTimeMillis())
                .statusCodeAndMessage(403, null)
                .requestUri("/dude")
                .build();

        assertNull(error.stackTrace());
        assertEquals("HttpClientError 403", error.getMessage());
        assertEquals("HttpClientError 403", error.getExceptionClass());

        JSONArray serializedError = (JSONArray) AgentHelper.serializeJSON(error);
        assertNotNull(serializedError);

        assertEquals(error.getMessage(), serializedError.get(2));
        // assertNull(serializedError.get("s"))
    }

    @Test
    @SuppressWarnings("unchecked")
    public void attributesNull() throws Exception {
        ErrorCollectorConfig errorCollectorConfig = ServiceFactory.getConfigService().getDefaultAgentConfig()
                .getErrorCollectorConfig();
        TracedError error = HttpTracedError
                .builder(errorCollectorConfig, appName, "dude", System.currentTimeMillis())
                .statusCodeAndMessage(403, null)
                .requestUri("/dude")
                .prefixedAttributes(null)
                .userAttributes(null)
                .agentAttributes(null)
                .errorAttributes(null)
                .intrinsicAttributes(null)
                .build();

        JSONArray serializedError = (JSONArray) AgentHelper.serializeJSON(error);
        assertNotNull(serializedError);

        Map<String, Object> params = (Map<String, Object>) serializedError.get(4);
        assertEquals(3, params.size());
        assertNotNull(params.get("stack_traces"));

        Map<String, Object> agentAttributes = (Map<String, Object>) params.get("agentAttributes");
        assertEquals("/dude", agentAttributes.get("request.uri"));

        Map<String, Object> intrinsicsParam = (Map<String, Object>) params.get("intrinsics");
        assertEquals(false, intrinsicsParam.get("error.expected"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void attributesEmpty() throws Exception {
        Map<String, Map<String, String>> prefixes = new HashMap<>();
        Map<String, Object> agentParams = new HashMap<>();
        Map<String, Object> userParams = new HashMap<>();
        Map<String, String> errorParams = new HashMap<>();
        Map<String, Object> intrinsics = new HashMap<>();
        ErrorCollectorConfig errorCollectorConfig = ServiceFactory.getConfigService().getDefaultAgentConfig()
                .getErrorCollectorConfig();
        TracedError error = HttpTracedError
                .builder(errorCollectorConfig, appName, "dude", System.currentTimeMillis())
                .statusCodeAndMessage(403, null)
                .requestUri("/dude")
                .prefixedAttributes(prefixes)
                .userAttributes(userParams)
                .agentAttributes(agentParams)
                .errorAttributes(errorParams)
                .intrinsicAttributes(intrinsics)
                .build();

        JSONArray serializedError = (JSONArray) AgentHelper.serializeJSON(error);
        assertNotNull(serializedError);

        Map<String, Object> params = (Map<String, Object>) serializedError.get(4);
        assertEquals(3, params.size());
        assertNotNull(params.get("stack_traces"));
        Map<String, Object> intrinsicsParam = (Map<String, Object>) params.get("intrinsics");
        assertEquals(false, intrinsicsParam.get("error.expected"));

        Map<String, Object> serializedAgentAtts = (Map<String, Object>) params.get("agentAttributes");
        assertEquals("/dude", serializedAgentAtts.get("request.uri"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void httpAttributes() throws Exception {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("key1", "value1");
        Map<String, Map<String, String>> prefixes = new HashMap<>();
        prefixes.put("request.parameters.", requestParams);
        Map<String, Object> agentParams = new HashMap<>();
        agentParams.put("key2", 2L);
        Map<String, Object> userParams = new HashMap<>();
        userParams.put("key3", "value3");
        Map<String, String> errorParams = new HashMap<>();
        errorParams.put("key4", "value4");
        Map<String, Object> intrinsics = new HashMap<>();
        intrinsics.put("key5", "value5");
        ErrorCollectorConfig errorCollectorConfig = ServiceFactory.getConfigService().getDefaultAgentConfig()
                .getErrorCollectorConfig();
        TracedError error = HttpTracedError
                .builder(errorCollectorConfig, appName, "dude", System.currentTimeMillis())
                .statusCodeAndMessage(403, null)
                .requestUri("/dude")
                .prefixedAttributes(prefixes)
                .userAttributes(userParams)
                .agentAttributes(agentParams)
                .errorAttributes(errorParams)
                .intrinsicAttributes(intrinsics)
                .build();

        JSONArray serializedError = (JSONArray) AgentHelper.serializeJSON(error);
        assertNotNull(serializedError);

        Map<String, Object> params = (Map<String, Object>) serializedError.get(4);
        assertEquals(4, params.size());
        assertNotNull(params.get("stack_traces"));
        assertNotNull(params.get("agentAttributes"));
        assertNotNull(params.get("userAttributes"));
        assertNotNull(params.get("intrinsics"));

        Map<String, Object> atts = (Map<String, Object>) params.get("agentAttributes");
        assertEquals(2L, atts.get("key2"));
        assertEquals("/dude", atts.get("request.uri"));

        atts = (Map<String, Object>) params.get("userAttributes");
        assertEquals(2, atts.size());
        assertEquals("value4", atts.get("key4"));
        assertEquals("value3", atts.get("key3"));

        atts = (Map<String, Object>) params.get("intrinsics");
        assertEquals(2, atts.size());
        assertEquals("value5", atts.get("key5"));
        assertEquals(false, atts.get("error.expected"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void httpIntrinsicAttributes() throws Exception {
        Map<String, Object> agentAttributes = new HashMap<>();
        Map<String, Object> intrinsics = new HashMap<>();
        intrinsics.put("key5", "value5");
        ErrorCollectorConfig errorCollectorConfig = ServiceFactory.getConfigService().getDefaultAgentConfig()
                .getErrorCollectorConfig();
        TracedError error = HttpTracedError
                .builder(errorCollectorConfig, appName, "dude", System.currentTimeMillis())
                .statusCodeAndMessage(403, null)
                .requestUri("/dude")
                .agentAttributes(agentAttributes)
                .intrinsicAttributes(intrinsics)
                .build();

        JSONArray serializedError = (JSONArray) AgentHelper.serializeJSON(error);
        assertNotNull(serializedError);

        Map<String, Object> params = (Map<String, Object>) serializedError.get(4);
        assertEquals(3, params.size());
        assertNotNull(params.get("stack_traces"));
        assertNotNull(params.get("intrinsics"));

        Map<String, Object> serializedAgentAtts = (Map<String, Object>) params.get("agentAttributes");
        assertEquals("/dude", serializedAgentAtts.get("request.uri"));

        Map<String, Object> atts = (Map<String, Object>) params.get("intrinsics");
        assertEquals(2, atts.size());
        assertEquals("value5", atts.get("key5"));
        assertEquals(false, atts.get("error.expected"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void httpErrorAttributes() throws Exception {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("key1", "value1");
        Map<String, Map<String, String>> prefixes = new HashMap<>();
        prefixes.put("hello.", requestParams);
        Map<String, Object> agentAttributes = new HashMap<>();
        Map<String, String> errorParams = new HashMap<>();
        errorParams.put("key4", "value4");
        ErrorCollectorConfig errorCollectorConfig = ServiceFactory.getConfigService().getDefaultAgentConfig()
                .getErrorCollectorConfig();
        TracedError error = HttpTracedError
                .builder(errorCollectorConfig, appName, "dude", System.currentTimeMillis())
                .statusCodeAndMessage(403, null)
                .requestUri("/dude")
                .prefixedAttributes(prefixes)
                .agentAttributes(agentAttributes)
                .errorAttributes(errorParams)
                .build();

        JSONArray serializedError = (JSONArray) AgentHelper.serializeJSON(error);
        assertNotNull(serializedError);

        Map<String, Object> params = (Map<String, Object>) serializedError.get(4);
        assertEquals(4, params.size());
        assertNotNull(params.get("stack_traces"));
        assertNotNull(params.get("agentAttributes"));
        assertNotNull(params.get("userAttributes"));
        assertNotNull(params.get("intrinsics"));

        Map<String, Object> atts = (Map<String, Object>) params.get("agentAttributes");
        assertEquals(2, atts.size());
        assertEquals("value1", atts.get("hello.key1"));
        assertEquals("/dude", atts.get("request.uri"));

        atts = (Map<String, Object>) params.get("userAttributes");
        assertEquals(1, atts.size());
        assertEquals("value4", atts.get("key4"));

        Map<String, Object> intrinsicsParam = (Map<String, Object>) params.get("intrinsics");
        assertEquals(false, intrinsicsParam.get("error.expected"));
    }

    @Test
    public void throwableError() throws Exception {
        String msg = "Oh dude!";
        Error e = new UnsupportedClassVersionError(msg);
        ErrorCollectorConfig errorCollectorConfig = ServiceFactory.getConfigService().getDefaultAgentConfig()
                .getErrorCollectorConfig();
        TracedError error = ThrowableError
                .builder(errorCollectorConfig, null, null, e, System.currentTimeMillis())
                .build();

        assertNotNull(error.stackTrace());
        assertEquals(msg, error.getMessage());
        assertEquals(UnsupportedClassVersionError.class.getName(), error.getExceptionClass());

        JSONArray serializedError = (JSONArray) AgentHelper.serializeJSON(error);
        assertNotNull(serializedError);
        JSONObject params = (JSONObject) serializedError.get(4);

        assertNotNull(params.get("stack_trace"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testerrorWithTimeout() throws Exception {
        Throwable throwable = new Throwable();
        Map<String, Object> intrinsics = new HashMap<>();
        intrinsics.put("nr.timeoutCause", "token");

        ErrorCollectorConfig errorCollectorConfig = ServiceFactory.getConfigService().getDefaultAgentConfig()
                .getErrorCollectorConfig();
        TracedError error = ThrowableError
                .builder(errorCollectorConfig, "appName", "metricName", throwable, 100)
                .requestUri("requestUri")
                .intrinsicAttributes(intrinsics)
                .build();

        JSONArray serializedError = (JSONArray) AgentHelper.serializeJSON(error);

        Map<String, Object> attributes = (Map<String, Object>) serializedError.get(4);
        Map<String, Object> intrinsicAtt = (Map<String, Object>) attributes.get("intrinsics");

        assertNotNull(intrinsicAtt.get(AttributeNames.TIMEOUT_CAUSE));
    }

    @Test
    public void nestedExceptionsNoStripping() throws Exception {
        Exception nested2 = new NullPointerException("NPE");
        StackTraceElement stack1 = new StackTraceElement("Class1", "Method1", "Class1.java", 1);
        StackTraceElement stack2 = new StackTraceElement("Class2", "Method2", "Class2.java", 2);
        StackTraceElement stack3 = new StackTraceElement("Class3", "Method3", "Class3.java", 3);
        StackTraceElement[] stack_123 = new StackTraceElement[] { stack1, stack2, stack3 };
        nested2.setStackTrace(stack_123);

        Exception nested1 = new IOException("IO", nested2);
        StackTraceElement stack4 = new StackTraceElement("Class4", "Method4", "Class4.java", 4);
        StackTraceElement stack5 = new StackTraceElement("Class5", "Method5", "Class5.java", 5);
        StackTraceElement stack6 = new StackTraceElement("Class6", "Method6", "Class6.java", 6);
        StackTraceElement[] stack_456 = new StackTraceElement[] { stack4, stack5, stack6 };
        nested1.setStackTrace(stack_456);

        Exception top = new RuntimeException("RTE", nested1);
        StackTraceElement stack7 = new StackTraceElement("Class7", "Method7", "Class7.java", 7);
        StackTraceElement stack8 = new StackTraceElement("Class8", "Method8", "Class8.java", 8);
        StackTraceElement stack9 = new StackTraceElement("Class9", "Method9", "Class9.java", 9);
        StackTraceElement[] stack_789 = new StackTraceElement[] { stack7, stack8, stack9 };
        top.setStackTrace(stack_789);

        Exception suppressed1 = new IllegalStateException("ISE");
        StackTraceElement stack10 = new StackTraceElement("Class10", "Method10", "Class10.java", 10);
        StackTraceElement stack11 = new StackTraceElement("Class11", "Method11", "Class11.java", 11);
        StackTraceElement stack12 = new StackTraceElement("Class12", "Method12", "Class12.java", 12);
        suppressed1.setStackTrace(new StackTraceElement[] {stack10, stack11, stack12 });
        top.addSuppressed(suppressed1);

        ErrorCollectorConfig errorCollectorConfig = ServiceFactory.getConfigService().getDefaultAgentConfig()
                .getErrorCollectorConfig();

        TracedError error = ThrowableError
                .builder(errorCollectorConfig, null, null, top, System.currentTimeMillis())
                .build();

        Collection<String> stackTrace = error.stackTrace();
        assertNotNull(stackTrace);
        assertEquals(17, stackTrace.size());
        Iterator<String> it = stackTrace.iterator();
        assertTrue(it.next().contains("7"));
        assertTrue(it.next().contains("8"));
        assertTrue(it.next().contains("9"));
        String next = it.next();
        assertTrue(next.contains("Suppressed:"));
        assertTrue(next.contains(suppressed1.toString()));
        assertTrue(it.next().contains("10"));
        assertTrue(it.next().contains("11"));
        assertTrue(it.next().contains("12"));
        assertEquals("", it.next().trim());
        next = it.next();
        assertTrue(next.contains("caused by"));
        assertTrue(next.contains(nested1.toString()));
        assertTrue(it.next().contains("4"));
        assertTrue(it.next().contains("5"));
        assertTrue(it.next().contains("6"));
        assertEquals("", it.next().trim());
        next = it.next();
        assertTrue(next.contains("caused by"));
        assertTrue(next.contains(nested2.toString()));
        assertTrue(it.next().contains("1"));
        assertTrue(it.next().contains("2"));
        assertTrue(it.next().contains("3"));

        JSONArray serializedError = (JSONArray) AgentHelper.serializeJSON(error);
        assertNotNull(serializedError);
        JSONObject params = (JSONObject) serializedError.get(4);
        assertNotNull(params.get("stack_trace"));
    }

    @Test
    public void writeJsonString_whenTraceIsWithinTxn_reportsTxnGuid() {
        Throwable throwable = new Throwable();
        ErrorCollectorConfig errorCollectorConfig = ServiceFactory.getConfigService().getDefaultAgentConfig()
                .getErrorCollectorConfig();
        TracedError error = ThrowableError
                .builder(errorCollectorConfig, "appName", "metricName", throwable, 100)
                .requestUri("requestUri")
                .transactionGuid("123456")
                .build();

        JSONArray errorAsJson = convertTracedErrorToJsonArray(error);

        assertEquals(6, errorAsJson.size());
        assertEquals(100L, errorAsJson.get(0));
        assertEquals("metricName", errorAsJson.get(1));
        assertEquals("", errorAsJson.get(2));
        assertEquals("java.lang.Throwable", errorAsJson.get(3));
        assertTrue(errorAsJson.get(4) instanceof JSONObject);
        assertEquals("123456", errorAsJson.get(5));
    }

    @Test
    public void writeJsonString_whenTraceIsNotWithinTxn_reportsTxnGuid() {
        Throwable throwable = new Throwable();
        ErrorCollectorConfig errorCollectorConfig = ServiceFactory.getConfigService().getDefaultAgentConfig()
                .getErrorCollectorConfig();
        TracedError error = ThrowableError
                .builder(errorCollectorConfig, "appName", "metricName", throwable, 100)
                .requestUri("requestUri")
                .transactionData(null)
                .build();

        JSONArray errorAsJson = convertTracedErrorToJsonArray(error);

        assertEquals(5, errorAsJson.size());
        assertEquals(100L, errorAsJson.get(0));
        assertEquals("metricName", errorAsJson.get(1));
        assertEquals("", errorAsJson.get(2));
        assertEquals("java.lang.Throwable", errorAsJson.get(3));
        assertTrue(errorAsJson.get(4) instanceof JSONObject);
    }

    private JSONArray convertTracedErrorToJsonArray(TracedError tracedError) {
        // Write out the error to a String then convert back to JSONArray to do asserts
        JSONArray result = null;
        try {
            StringWriter sw = new StringWriter();
            tracedError.writeJSONString(sw);
            JSONParser parser = new JSONParser();
            result = (JSONArray) parser.parse(sw.toString());
        } catch (Exception e) {
            // Eat it
        }

        return result;
    }

    private String findNextCausedBy(Iterator<String> it) {
        return findNextContaining(it, "caused by");
    }

    private String findNextContaining(Iterator<String> it, String contains) {
        for (String value = ""; it.hasNext(); value = it.next()) {
            if (value != null && value.contains(contains)) {
                return value;
            }
        }

        throw new AssertionError("Ran out of elements looking for '"+ contains +"'!");
    }

    @Test
    public void nestedExceptionsStripAll() {
        Exception nested2 = new NullPointerException("Null Pointer Exception should be stripped");
        Exception nested1 = new IOException("IO Exception should be stripped", nested2);
        Exception top = new RuntimeException("RuntimeException should be stripped", nested1);
        Exception suppressed = new IllegalStateException("IllegalStateException should be stripped");
        top.addSuppressed(suppressed);

        ErrorCollectorConfig errorCollectorConfig = ServiceFactory.getConfigService().getDefaultAgentConfig()
                .getErrorCollectorConfig();

        TracedError error = ThrowableError
                .builder(errorCollectorConfig, null, null, top, System.currentTimeMillis())
                .errorMessageReplacer(new ErrorMessageReplacer(new StripExceptionConfigImpl(true, Collections.<String>emptySet())))
                .build();

        assertEquals(error.getMessage(), "Message removed by New Relic 'strip_exception_messages' setting");
        Collection<String> stackTrace = error.stackTrace();
        assertNotNull(stackTrace);
        Iterator<String> it = stackTrace.iterator();
        String next = findNextContaining(it, "Suppressed:");
        assertTrue(next, next.contains("Suppressed: java.lang.IllegalStateException: Message removed by New Relic 'strip_exception_messages' setting"));
        assertFalse(next.contains(suppressed.toString()));
        next = findNextCausedBy(it);
        assertTrue(next, next.contains("caused by java.io.IOException: Message removed by New Relic 'strip_exception_messages' setting"));
        assertFalse(next.contains(nested1.toString()));
        next = findNextCausedBy(it);
        assertTrue(next, next.contains("caused by java.lang.NullPointerException: Message removed by New Relic 'strip_exception_messages' setting"));
        assertFalse(next.contains(nested2.toString()));
    }

    @Test
    public void nestedExceptionsStripSome() {
        Exception nested2 = new NullPointerException("Null Pointer Exception should be stripped");
        Exception nested1 = new IOException("IO Exception should not be stripped", nested2);
        Exception top = new RuntimeException("RuntimeException should be stripped", nested1);
        Exception suppressed1 = new IllegalStateException("IllegalStateException should be stripped");
        Exception suppressed2 = new IllegalArgumentException("IllegalArgumentException should not be stripped");
        top.addSuppressed(suppressed1);
        nested1.addSuppressed(suppressed2);

        ErrorCollectorConfig errorCollectorConfig = ServiceFactory.getConfigService().getDefaultAgentConfig()
                .getErrorCollectorConfig();

        TracedError error = ThrowableError
                .builder(errorCollectorConfig, null, null, top, System.currentTimeMillis())
                .errorMessageReplacer(new ErrorMessageReplacer(new StripExceptionConfigImpl(true,
                        Sets.newSet("java.io.IOException", IllegalArgumentException.class.getName())))
                )
                .build();

        assertEquals(error.getMessage(), "Message removed by New Relic 'strip_exception_messages' setting");
        Collection<String> stackTrace = error.stackTrace();
        Iterator<String> it = stackTrace.iterator();
        String next = findNextContaining(it, "Suppressed:");
        assertTrue(next, next.contains("Suppressed: java.lang.IllegalStateException: Message removed by New Relic 'strip_exception_messages' setting"));
        assertFalse(next.contains(suppressed1.toString()));
        next = findNextCausedBy(it);
        assertTrue(next, next.contains("caused by java.io.IOException: IO Exception should not be stripped"));
        next = findNextContaining(it, "Suppressed:");
        assertTrue(next, next.contains("Suppressed: java.lang.IllegalArgumentException: IllegalArgumentException should not be stripped"));
        next = findNextCausedBy(it);
        assertTrue(next, next.contains("caused by java.lang.NullPointerException: Message removed by New Relic 'strip_exception_messages' setting"));
        assertFalse(next.contains(nested2.toString()));
    }
}

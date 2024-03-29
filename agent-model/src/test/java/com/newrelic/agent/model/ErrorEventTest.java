package com.newrelic.agent.model;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class ErrorEventTest {
    @Test
    public void writeJSONString_ErrorEvent_ValidOutput() throws IOException, ParseException {
        ErrorEvent event = baseErrorEvent();
        Writer out = new StringWriter();
        event.writeJSONString(out);
        String json = out.toString();
        System.out.println(json);

        //user and agent attributes
        assertTrue(json.contains("{\"user\":\"a\"}"));
        assertTrue(json.contains("{\"agent\":\"c\"}"));

        //distributed Trace Intrinsics
        assertTrue(json.contains("\"distributed\":\"b\""));

        //standard attributes
        assertTrue(json.contains("\"error.class\":\"e\""));
        assertTrue(json.contains("\"error.message\":\"check out this error\""));
        assertTrue(json.contains("\"transactionName\":\"buzzbuzz\""));
        assertTrue(json.contains("\"error.expected\":false"));
        assertTrue(json.contains("\"timestamp\":1"));
        assertTrue(json.contains("\"duration\":10"));
        assertTrue(json.contains("\"queueDuration\":11"));
        assertTrue(json.contains("\"externalDuration\":12"));
        assertTrue(json.contains("\"databaseDuration\":13"));
        assertTrue(json.contains("\"gcCumulative\":14"));
        assertTrue(json.contains("\"databaseCallCount\":15"));
        assertTrue(json.contains("\"externalCallCount\":16"));
        assertTrue(json.contains("\"port\":2020"));
        assertTrue(json.contains("\"priority\":1"));
        assertTrue(json.contains("\"guid\":\"abc\""));

        //hidden attributes
        assertTrue(json.contains("\"nr.transactionGuid\":\"abc\""));
        assertTrue(json.contains("\"nr.referringTransactionGuid\":\"def\""));
        assertTrue(json.contains("\"nr.syntheticsResourceId\":\"ghi\""));
        assertTrue(json.contains("\"nr.syntheticsMonitorId\":\"jkl\""));
        assertTrue(json.contains("\"nr.syntheticsJobId\":\"mno\""));
        assertTrue(json.contains("\"nr.syntheticsType\":\"scheduled\""));
        assertTrue(json.contains("\"nr.syntheticsInitiator\":\"cli\""));
        assertTrue(json.contains("\"nr.timeoutCause\":\"none\""));
        assertTrue(json.contains("\"nr.tripId\":\"wxyz\""));

        JSONParser parser = new JSONParser();
        JSONArray jsonArray = (JSONArray) parser.parse(json);
        JSONObject jsonObj = (JSONObject) jsonArray.get(0);
        assertEquals(3, jsonArray.size());
        assertEquals("val1", jsonObj.get("nr.syntheticsKey1"));


    }

    @Test
    public void isValid_ErrorEvent_returnsTrue() {
        assertTrue(baseErrorEvent().isValid());
    }

    @Test
    public void errorEvent_getsAttributes(){
        ErrorEvent event = baseErrorEvent();
        Map<String, Object> expectedAttributes = new HashMap<>();
        expectedAttributes.put("distributed", "b");

        assertEquals(event.getErrorClass(), "e");
        assertEquals(event.getErrorMessage(), "check out this error");
        assertEquals(event.getTransactionName(), "buzzbuzz");
        assertEquals(event.getDistributedTraceIntrinsics(), expectedAttributes);
        assertEquals(event.getTransactionGuid(), "abc");
    }
    @Test
    public void writeJSONString_handles_missingDataErrorEvent() throws IOException{
        ErrorEvent event = missingDataErrorEvent();
        Writer out = new StringWriter();
        event.writeJSONString(out);
        String json = out.toString();
        System.out.println(json);

        assertTrue(json.contains("\"error.class\":\"f\""));
        assertTrue(json.contains("\"error.message\":\"another error\""));
        assertTrue(json.contains("\"transactionName\":\"kayaks\""));
        assertTrue(json.contains("\"error.expected\":false"));

        assertFalse(json.contains("\"duration\":"));
        assertFalse(json.contains("\"queueDuration\":"));
        assertFalse(json.contains("\"externalDuration\":"));
        assertFalse(json.contains("\"databaseDuration\":"));
        assertFalse(json.contains("\"gcCumulative\":"));
        assertFalse(json.contains("\"databaseCallCount\":"));
        assertFalse(json.contains("\"externalCallCount\":"));
        assertFalse(json.contains("\"port\":"));
        assertFalse(json.contains("\"priority\":"));
        assertFalse(json.contains("\"nr.transactionGuid\":"));
        assertFalse(json.contains("\"nr.referringTransactionGuid\":"));
        assertFalse(json.contains("\"nr.syntheticsResourceId\":"));
        assertFalse(json.contains("\"nr.syntheticsMonitorId\":"));
        assertFalse(json.contains("\"nr.syntheticsJobId\":"));
        assertFalse(json.contains("\"nr.syntheticsType\":"));
        assertFalse(json.contains("\"nr.syntheticsInitiator\":"));
        assertFalse(json.contains("\"nr.timeoutCause\":"));
        assertFalse(json.contains("\"nr.tripId\":"));
    }

    private ErrorEvent missingDataErrorEvent(){
        Map<String, Object> emptyUserAttributes = new HashMap<>();
        Map<String, Object> emptyAgentAttributes = new HashMap<>();
        AttributeFilter attributeFilter = new AttributeFilter.PassEverythingAttributeFilter();
        return new ErrorEvent(
                "foo", 1, Float.NEGATIVE_INFINITY, emptyUserAttributes, "f",
                "another error", false, "kayaks",
                Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY,
                0, Float.NEGATIVE_INFINITY, 0, 0, null,
                null, null, null, null,
                null, null, null, Integer.MIN_VALUE, null,
                null, null, emptyAgentAttributes, attributeFilter
        );
    }
    private ErrorEvent baseErrorEvent(){
        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put("user", "a");
        Map<String, Object> distributedTraceIntrinsics = new HashMap<>();
        distributedTraceIntrinsics.put("distributed", "b");
        Map<String, Object> agentAttributes = new HashMap<>();
        agentAttributes.put("agent", "c");
        AttributeFilter attributeFilter = new AttributeFilter.PassEverythingAttributeFilter();
        Map<String, String> synthAttrs = new HashMap<>();
        synthAttrs.put("key1", "val1");
        return new ErrorEvent(
                "test", 1, 1, userAttributes,
                "e", "check out this error", false,
                "buzzbuzz", 10, 11, 12, 13,
                14, 15, 16,
                "abc", "def", "ghi", "jkl",
                "mno", "scheduled", "cli", synthAttrs,2020, "none", "wxyz",
                distributedTraceIntrinsics, agentAttributes, attributeFilter
        );
    }
}
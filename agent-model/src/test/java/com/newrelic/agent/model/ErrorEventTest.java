package com.newrelic.agent.model;

import org.json.simple.JSONArray;
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

        //hidden attributes
        assertTrue(json.contains("\"nr.transactionGuid\":\"abc\""));
        assertTrue(json.contains("\"nr.referringTransactionGuid\":\"def\""));
        assertTrue(json.contains("\"nr.syntheticsResourceId\":\"ghi\""));
        assertTrue(json.contains("\"nr.syntheticsMonitorId\":\"jkl\""));
        assertTrue(json.contains("\"nr.syntheticsJobId\":\"mno\""));
        assertTrue(json.contains("\"nr.timeoutCause\":\"none\""));
        assertTrue(json.contains("\"nr.tripId\":\"wxyz\""));

        JSONParser parser = new JSONParser();
        JSONArray jsonArray = (JSONArray) parser.parse(json);
        assertEquals(3, jsonArray.size());

    }

    private ErrorEvent baseErrorEvent(){
        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put("user", "a");
        Map<String, Object> distributedTraceIntrinsics = new HashMap<>();
        distributedTraceIntrinsics.put("distributed", "b");
        Map<String, Object> agentAttributes = new HashMap<>();
        agentAttributes.put("agent", "c");
        AttributeFilter attributeFilter = new AttributeFilter.PassEverythingAttributeFilter();
        return new ErrorEvent(
                "test", 1, 1, userAttributes,
                "e", "check out this error", false,
                "buzzbuzz", 10, 11, 12, 13,
                14, 15, 16,
                "abc", "def", "ghi", "jkl",
                "mno", 2020, "none", "wxyz",
                distributedTraceIntrinsics, agentAttributes, attributeFilter
        );
    }
}
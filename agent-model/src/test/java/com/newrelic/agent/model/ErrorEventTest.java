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
    public void testWriteJSONString() {
        ErrorEvent event = baseErrorEvent();
        Writer out = new StringWriter();
        try {
            event.writeJSONString(out);
        } catch(IOException e) {
        }
        System.out.print(out.toString());
        String json = out.toString();
        JSONParser parser = new JSONParser();
        JSONArray jsonArray;
        try {
            jsonArray = (JSONArray) parser.parse(json);
        } catch (ParseException e) {
            //do nothing
            jsonArray = new JSONArray();
        }
        assertEquals(3, jsonArray.size());

    }

    private ErrorEvent baseErrorEvent(){
        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put("user", "b");
        Map<String, Object> distributedTraceIntrinsics = new HashMap<>();
        distributedTraceIntrinsics.put("distributed", "b");
        Map<String, Object> agentAttributes = new HashMap<>();
        agentAttributes.put("agent", "c");
        AttributeFilter attributeFilter = new AttributeFilter.PassEverythingAttributeFilter();
        return new ErrorEvent(
                "test", 1, 1, userAttributes,
                "e", "check out this error", false,
                "transaction1", 10, 11, 12, 13,
                14, 15, 16,
                "abc", "def", "ghi", "jkl",
                "mno", 2020, "none", "wxyz",
                distributedTraceIntrinsics, agentAttributes, attributeFilter
        );
    }
}
package com.newrelic.agent.model;

import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class LogEventTest {

    @Test
    public void testConstructor() {
        LogEvent logEvent = new LogEvent(Collections.emptyMap(), 0);

        assertEquals(logEvent.getPriority(), 0, 0);
        assertNotNull(logEvent.getMutableUserAttributes());
        assertTrue(logEvent.getMutableUserAttributes().isEmpty());
    }

    @Test
    public void testPriorityAccessors() {
        LogEvent logEvent = new LogEvent(Collections.emptyMap(), 0);

        assertEquals(0, logEvent.getPriority(), 0);

        logEvent.setPriority(1);

        assertEquals(1, logEvent.getPriority(),0);
    }

    @Test
    public void testJsonString() throws IOException {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("key", "value");

        LogEvent logEvent = new LogEvent(attributes, 0);
        StringWriter writer = new StringWriter();

        logEvent.writeJSONString(writer);

        assertEquals("{\"key\":\"value\"}", writer.toString());
    }
}
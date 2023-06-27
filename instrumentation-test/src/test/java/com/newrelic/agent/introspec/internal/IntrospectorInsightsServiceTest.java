package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.introspec.Event;
import org.junit.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class IntrospectorInsightsServiceTest {
    @Test
    public void getName_returnsServiceName() {
        IntrospectorInsightsService introspectorInsightsService = new IntrospectorInsightsService();
        assertEquals("InsightsService", introspectorInsightsService.getName());
    }

    @Test
    public void controlMethods_behaveProperly() throws Exception {
        IntrospectorInsightsService introspectorInsightsService = new IntrospectorInsightsService();

        introspectorInsightsService.start();  //No op
        introspectorInsightsService.stop();  //No op

        assertTrue(introspectorInsightsService.isEnabled());
        assertTrue(introspectorInsightsService.isStarted());
        assertTrue(introspectorInsightsService.isStartedOrStarting());
        assertFalse(introspectorInsightsService.isStopped());
        assertFalse(introspectorInsightsService.isStoppedOrStopping());
    }

    @Test
    public void getLogger_returnsLoggerInstance() {
        IntrospectorInsightsService introspectorInsightsService = new IntrospectorInsightsService();

        assertNotNull(introspectorInsightsService.getLogger());
    }

    @Test
    public void recordCustomEvent_storeSuppliedLogEvent() {
        IntrospectorInsightsService introspectorInsightsService = new IntrospectorInsightsService();
        Map<String, String> attributes = new HashMap<>();
        attributes.put("key1", "val1");
        attributes.put("key2", "val2");

        introspectorInsightsService.recordCustomEvent("event", attributes);
        Collection<Event> events = introspectorInsightsService.getEvents("event");

        assertEquals(1, events.size());
        assertEquals(2, events.iterator().next().getAttributes().size());
    }

    @Test
    public void getEventTypes_returnsStoredEventTypes() {
        IntrospectorInsightsService introspectorInsightsService = new IntrospectorInsightsService();
        Map<String, String> attributes = new HashMap<>();

        introspectorInsightsService.recordCustomEvent("event", attributes);
        introspectorInsightsService.recordCustomEvent("event2", attributes);
        Collection<String> events = introspectorInsightsService.getEventTypes();

        assertEquals(2, events.size());
        assertTrue(events.contains("event"));
        assertTrue(events.contains("event2"));
    }

    @Test
    public void clearMethods_clearEventReservoir() {
        IntrospectorInsightsService introspectorInsightsService = new IntrospectorInsightsService();
        Map<String, String> attributes = new HashMap<>();

        introspectorInsightsService.recordCustomEvent("event", attributes);
        Collection<Event> events = introspectorInsightsService.getEvents("event");
        assertEquals(1, events.size());

        introspectorInsightsService.clear();
        events = introspectorInsightsService.getEvents("event");
        assertEquals(0, events.size());

        introspectorInsightsService.recordCustomEvent("event", attributes);
        events = introspectorInsightsService.getEvents("event");
        assertEquals(1, events.size());

        introspectorInsightsService.clearReservoir();
        events = introspectorInsightsService.getEvents("event");
        assertEquals(0, events.size());
    }

    @Test
    public void getEventHarvestIntervalMetric_returnsEmptyString() {
        IntrospectorInsightsService introspectorInsightsService = new IntrospectorInsightsService();
        assertEquals("", introspectorInsightsService.getEventHarvestIntervalMetric());
    }

    @Test
    public void getReportPeriodInSecondsMetric_returnsEmptyString() {
        IntrospectorInsightsService introspectorInsightsService = new IntrospectorInsightsService();
        assertEquals("", introspectorInsightsService.getReportPeriodInSecondsMetric());
    }

    @Test
    public void getEventHarvestLimitMetric_returnsEmptyString() {
        IntrospectorInsightsService introspectorInsightsService = new IntrospectorInsightsService();
        assertEquals("", introspectorInsightsService.getEventHarvestLimitMetric());
    }

    @Test
    public void getMaxSamplesStored_returnsZero() {
        IntrospectorInsightsService introspectorInsightsService = new IntrospectorInsightsService();
        assertEquals(0, introspectorInsightsService.getMaxSamplesStored());
    }
}

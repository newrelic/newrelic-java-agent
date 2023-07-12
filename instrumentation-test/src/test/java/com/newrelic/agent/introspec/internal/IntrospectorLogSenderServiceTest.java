package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.bridge.logging.LogAttributeKey;
import com.newrelic.agent.bridge.logging.LogAttributeType;
import com.newrelic.agent.model.LogEvent;
import org.junit.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class IntrospectorLogSenderServiceTest {
    @Test
    public void getName_returnsServiceName() {
        IntrospectorLogSenderService introspectorLogSenderService = new IntrospectorLogSenderService();
        assertEquals("LogSenderService", introspectorLogSenderService.getName());
    }

    @Test
    public void controlMethods_behaveProperly() throws Exception {
        IntrospectorLogSenderService introspectorLogSenderService = new IntrospectorLogSenderService();

        introspectorLogSenderService.start();  //No op
        introspectorLogSenderService.stop();  //No op

        assertTrue(introspectorLogSenderService.isEnabled());
        assertTrue(introspectorLogSenderService.isStarted());
        assertTrue(introspectorLogSenderService.isStartedOrStarting());
        assertFalse(introspectorLogSenderService.isStopped());
        assertFalse(introspectorLogSenderService.isStoppedOrStopping());
    }

    @Test
    public void getLogger_returnsLoggerInstance() {
        IntrospectorLogSenderService introspectorLogSenderService = new IntrospectorLogSenderService();

        assertNotNull(introspectorLogSenderService.getLogger());
    }

    @Test
    public void recordLogEvent_storeSuppliedLogEvent() {
        IntrospectorLogSenderService introspectorLogSenderService = new IntrospectorLogSenderService();
        Map<LogAttributeKey, String> attributes = new HashMap<>();
        attributes.put(new LogAttributeKey("key1", LogAttributeType.AGENT), "val1");
        attributes.put(new LogAttributeKey("key2", LogAttributeType.AGENT), "val2");

        introspectorLogSenderService.recordLogEvent(attributes);
        Collection<LogEvent> events = introspectorLogSenderService.getLogEvents();

        assertEquals(1, events.size());
        assertEquals(2, events.iterator().next().getUserAttributesCopy().size());
    }

    @Test
    public void clearMethods_clearEventReservoir() {
        IntrospectorLogSenderService introspectorLogSenderService = new IntrospectorLogSenderService();
        Map<LogAttributeKey, String> attributes = new HashMap<>();

        introspectorLogSenderService.recordLogEvent(attributes);
        Collection<LogEvent> events = introspectorLogSenderService.getLogEvents();
        assertEquals(1, events.size());

        introspectorLogSenderService.clear();
        events = introspectorLogSenderService.getLogEvents();
        assertEquals(0, events.size());

        introspectorLogSenderService.recordLogEvent(attributes);
        events = introspectorLogSenderService.getLogEvents();
        assertEquals(1, events.size());

        introspectorLogSenderService.clearReservoir();
        events = introspectorLogSenderService.getLogEvents();
        assertEquals(0, events.size());
    }

    @Test
    public void getEventHarvestIntervalMetric_returnsEmptyString() {
        IntrospectorLogSenderService introspectorLogSenderService = new IntrospectorLogSenderService();
        assertEquals("", introspectorLogSenderService.getEventHarvestIntervalMetric());
    }

    @Test
    public void getReportPeriodInSecondsMetric_returnsEmptyString() {
        IntrospectorLogSenderService introspectorLogSenderService = new IntrospectorLogSenderService();
        assertEquals("", introspectorLogSenderService.getReportPeriodInSecondsMetric());
    }

    @Test
    public void getEventHarvestLimitMetric_returnsEmptyString() {
        IntrospectorLogSenderService introspectorLogSenderService = new IntrospectorLogSenderService();
        assertEquals("", introspectorLogSenderService.getEventHarvestLimitMetric());
    }

    @Test
    public void getMaxSamplesStored_returnsZero() {
        IntrospectorLogSenderService introspectorLogSenderService = new IntrospectorLogSenderService();
        assertEquals(0, introspectorLogSenderService.getMaxSamplesStored());
    }
}

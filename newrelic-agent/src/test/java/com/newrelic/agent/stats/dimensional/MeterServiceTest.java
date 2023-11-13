package com.newrelic.agent.stats.dimensional;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.model.CustomInsightsEvent;
import com.newrelic.api.agent.metrics.Counter;
import com.newrelic.api.agent.metrics.Summary;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class MeterServiceTest extends TestCase {

    @Test
    public void testCounter() throws IOException {
        MeterService service = new MeterService();

        Counter counter = service.newCounter("test.metric");
        Summary badSummary = service.newSummary("test.metric");

        for (int i = 0; i < 5; i++) {
            counter.add(1, ImmutableMap.of("region", "us"));
            counter.add(1, ImmutableMap.of("region", "eu"));
            // no op
            badSummary.add(66d);
        }

        Collection<CustomInsightsEvent> events = service.harvestEvents().events;
        assertEquals(2, events.size());
        CustomInsightsEvent event = events.iterator().next();
        assertEquals("Metric", event.getType());
        Map<String, Object> intrinsics = event.getIntrinsics();
        assertEquals("test.metric", intrinsics.get("metric.name"));
        Number value = (Number) intrinsics.get("metric.count");
        assertEquals(5l, value.longValue());

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Writer writer = new OutputStreamWriter(out)) {
            event.writeJSONString(writer);
        }
        assertTrue(out.toString().contains("type"));
    }

    @Test
    public void testSummary() {
        MeterService service = new MeterService();

        Summary summary = service.newSummary("test.summary");
        for (int i = 0; i < 5; i++) {
            summary.add(i, ImmutableMap.of("region", "us"));
            summary.add(i, ImmutableMap.of("region", "eu", "shard", i));
        }

        Collection<CustomInsightsEvent> events = service.harvestEvents().events;
        assertEquals(6, events.size());
        CustomInsightsEvent event = events.iterator().next();
        assertEquals("Metric", event.getType());
        Map<String, Object> intrinsics = event.getIntrinsics();
        assertEquals("test.summary", intrinsics.get("metric.name"));
        List summaryValues = (List) intrinsics.get("metric.summary");
        assertEquals(4, summaryValues.size());
    }
}
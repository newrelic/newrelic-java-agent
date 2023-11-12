package com.newrelic.agent.stats.dimensional;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.model.CustomInsightsEvent;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DimensionalMetricAggregatorServiceTest extends TestCase {
    @Test
    public void testMixedTypes() throws IOException {
        DimensionalMetricAggregatorService service = new DimensionalMetricAggregatorService();

        for (int i = 0; i < 5; i++) {
            service.incrementCounter("test.metric", ImmutableMap.of("region", "us"));
            service.incrementCounter("test.metric", ImmutableMap.of("region", "eu"));
            // mix types
            service.addToSummary("test.metric", Collections.emptyMap(), 66d);
        }

        Collection<CustomInsightsEvent> events = service.harvestEvents().events;
        assertEquals(3, events.size());
    }

    @Test
    public void testCounter() throws IOException {
        DimensionalMetricAggregatorService service = new DimensionalMetricAggregatorService();

        for (int i = 0; i < 5; i++) {
            service.incrementCounter("test.metric", ImmutableMap.of("region", "us"));
            service.incrementCounter("test.metric", ImmutableMap.of("region", "eu"));
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
        DimensionalMetricAggregatorService service = new DimensionalMetricAggregatorService();

        for (int i = 0; i < 5; i++) {
            service.addToSummary("test.summary", ImmutableMap.of("region", "us"), i);
            service.addToSummary("test.summary", ImmutableMap.of("region", "eu", "shard", i), i);
        }

        Collection<CustomInsightsEvent> events = service.harvestEvents().events;
        assertEquals(6, events.size());
        CustomInsightsEvent event = events.iterator().next();
        assertEquals("Metric", event.getType());
        Map<String, Object> intrinsics = event.getIntrinsics();
        assertEquals("test.summary", intrinsics.get("metric.name"));
        List summary = (List) intrinsics.get("metric.summary");
        assertEquals(4, summary.size());
    }
}
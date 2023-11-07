package com.newrelic.agent.stats.dimensional;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.model.CustomInsightsEvent;
import junit.framework.TestCase;
import org.json.simple.parser.ParseException;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotEquals;

public class MetricAggregatesTest extends TestCase {

    @Test
    public void testGetHashEmptyMap() {
        assertEquals(Long.MAX_VALUE, MetricAggregates.getHash(ImmutableMap.of()));
    }

    @Test
    public void testGetHashMapOneItem() {
        assertEquals(MetricAggregates.getHash(ImmutableMap.of("test", 5)), MetricAggregates.getHash(ImmutableMap.of("test", 5)));
        assertNotEquals(MetricAggregates.getHash(ImmutableMap.of("test", 5)), MetricAggregates.getHash(ImmutableMap.of("test", 7)));
        assertNotEquals(MetricAggregates.getHash(ImmutableMap.of("test", 5)), MetricAggregates.getHash(ImmutableMap.of("test2", 5)));

        assertEquals(MetricAggregates.getHash(ImmutableMap.of("test", 5L)), MetricAggregates.getHash(ImmutableMap.of("test", 5L)));
        assertEquals(MetricAggregates.getHash(ImmutableMap.of("test", 5f)), MetricAggregates.getHash(ImmutableMap.of("test", 5f)));
        assertEquals(MetricAggregates.getHash(ImmutableMap.of("test", 5d)), MetricAggregates.getHash(ImmutableMap.of("test", 5d)));

        assertNotEquals(MetricAggregates.getHash(ImmutableMap.of("test", 5)), MetricAggregates.getHash(ImmutableMap.of("test", 5L)));
    }

    @Test
    public void testGetHashMap() {
        assertEquals(MetricAggregates.getHash(ImmutableMap.of("test", 5, "test2", 8)), MetricAggregates.getHash(ImmutableMap.of("test", 5, "test2", 8)));
        assertEquals(MetricAggregates.getHash(ImmutableMap.of("test", 5, "test2", 8L)), MetricAggregates.getHash(ImmutableMap.of("test2", 8L, "test", 5)));
    }

    @Test
    public void testCounter() throws IOException {
        DimensionalMetricAggregatorService service = new DimensionalMetricAggregatorService();

        for (int i = 0; i < 5; i++) {
            service.incrementCounter("test.metric", ImmutableMap.of("region", "us"));
            service.incrementCounter("test.metric", ImmutableMap.of("region", "eu"));
            // this will do nothing
            service.addToSummary("test.metric", Collections.emptyMap(), 66d);
        }

        Collection<CustomInsightsEvent> events = service.harvestEvents();
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
            // this will do nothing, wrong type
            service.incrementCounter("test.summary", Collections.emptyMap());
        }

        Collection<CustomInsightsEvent> events = service.harvestEvents();
        assertEquals(6, events.size());
        CustomInsightsEvent event = events.iterator().next();
        assertEquals("Metric", event.getType());
        Map<String, Object> intrinsics = event.getIntrinsics();
        assertEquals("test.summary", intrinsics.get("metric.name"));
        List summary = (List) intrinsics.get("metric.summary");
        assertEquals(4, summary.size());
    }
}
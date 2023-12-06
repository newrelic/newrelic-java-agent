package com.newrelic.agent.stats.dimensional;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.model.CustomInsightsEvent;
import com.newrelic.api.agent.metrics.Counter;
import com.newrelic.api.agent.metrics.Summary;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class MeterServiceTest extends TestCase {

    @Test
    public void testCounter() {
        MeterService service = new MeterService();

        Counter counter = service.newCounter("test.metric");
        Summary badSummary = service.newSummary("test.metric");

        for (int i = 0; i < 5; i++) {
            counter.add(1, ImmutableMap.of("region", "us"));
            counter.add(1, ImmutableMap.of("region", "eu"));
            // no op
            //badSummary.add(66d);
        }

        Collection<CustomInsightsEvent> events = MeterService.toEvents(service.metricDataSupplier.get());
        assertEquals(2, events.size());
        CustomInsightsEvent event = events.iterator().next();
        assertEquals("Metric", event.getType());
        Map<String, Object> intrinsics = event.getIntrinsics();
        assertEquals("test.metric", intrinsics.get("metric.name"));
        Number value = (Number) intrinsics.get("metric.count");
        assertEquals(5L, value.longValue());
    }

    @Test
    public void testSummary() {
        MeterService service = new MeterService();

        Summary summary = service.newSummary("test.summary");
        for (int i = 0; i < 5; i++) {
            summary.record(i, ImmutableMap.of("region", "us"));
            summary.record(i, ImmutableMap.of("region", "eu", "shard", i));
        }

        Collection<CustomInsightsEvent> events = MeterService.toEvents(service.metricDataSupplier.get());
        assertEquals(6, events.size());
        CustomInsightsEvent event = events.iterator().next();
        assertEquals("Metric", event.getType());
        Map<String, Object> intrinsics = event.getIntrinsics();
        assertEquals("test.summary", intrinsics.get("metric.name"));
        List summaryValues = (List) intrinsics.get("metric.summary");
        assertEquals(4, summaryValues.size());
    }

    @Test
    public void testCardinalityLimit() {
        final MeterService service = new MeterService(instrumentType -> 5);
        Summary summary = service.newSummary("test.summary");
        for (char c = 'a'; c < 'z'; c++) {
            summary.record(50D, ImmutableMap.of("component", "FFF" + c));
        }

        Collection<CustomInsightsEvent> events = MeterService.toEvents(service.metricDataSupplier.get());
        assertEquals(5, events.size());

        Optional<Object> overflow = events.stream().map(event -> event.getUserAttributesCopy().get("otel.metric.overflow")).filter(Objects::nonNull).findFirst();
        assertTrue(overflow.isPresent());
        assertEquals(Boolean.TRUE, overflow.get());

        for (char c = 'g'; c < 'z'; c++) {
            summary.record(50D, ImmutableMap.of("component", "FFF" + c));
        }

        events = MeterService.toEvents(service.metricDataSupplier.get());
        // it looks like the aggregation buckets are cleared out with each harvest
        assertEquals(5, events.size());
    }
}
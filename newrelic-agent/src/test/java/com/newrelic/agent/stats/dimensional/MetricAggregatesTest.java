package com.newrelic.agent.stats.dimensional;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.model.CustomInsightsEvent;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MetricAggregatesTest extends TestCase {

    @Test
    public void testMerge() {
        final CachingMapHasher mapHasher = new CachingMapHasher(SimpleMapHasher.INSTANCE);
        MetricAggregates aggregates = new MetricAggregates("metric.name", MetricType.summary, mapHasher);
        MetricAggregates other = new MetricAggregates("metric.name", MetricType.summary, mapHasher);

        Map<String, Object> usRegionMap = ImmutableMap.of("region", "us");
        for (int i = 0; i < 5; i++) {
            aggregates.getMeasure(usRegionMap).addToSummary(i);
            other.getMeasure(usRegionMap).addToSummary(i);
            other.getMeasure(ImmutableMap.of("region", "eu")).addToSummary(100 - i);
        }
        aggregates.getMeasure(ImmutableMap.of("region", "eu")).addToSummary(88);

        MetricAggregates merged = other.merge(aggregates);
        assertSame(other, merged);

        Collection<CustomInsightsEvent> events = new ArrayList<>();
        merged.harvest(events);

        assertEquals(2, events.size());

        Map<Object, List<CustomInsightsEvent>> byRegion = events.stream().collect(Collectors.groupingBy(event -> event.getUserAttributesCopy().get("region")));
        CustomInsightsEvent us = byRegion.get("us").iterator().next();
        List<Number> summary = (List<Number>) us.getIntrinsics().get("metric.summary");
        assertEquals(10l, summary.get(0));
        assertEquals(20d, summary.get(1));
        assertEquals(0d, summary.get(2));
        assertEquals(4d, summary.get(3));

        CustomInsightsEvent eu = byRegion.get("eu").iterator().next();
        summary = (List<Number>) eu.getIntrinsics().get("metric.summary");
        assertEquals(6l, summary.get(0));
        assertEquals(578d, summary.get(1));
        assertEquals(88d, summary.get(2));
        assertEquals(100d, summary.get(3));
    }
}
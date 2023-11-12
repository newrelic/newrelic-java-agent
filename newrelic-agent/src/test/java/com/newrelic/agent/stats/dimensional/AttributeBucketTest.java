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

public class AttributeBucketTest extends TestCase {
    @Test
    public void testMerge() {
        Map<String, Object> usRegionMap = ImmutableMap.of("region", "us");

        AttributeBucket bucket = new AttributeBucket(usRegionMap);
        AttributeBucket otherBucket = new AttributeBucket(usRegionMap);

        for (int i = 0; i < 5; i++) {
            bucket.getMeasure("my.metric", MetricType.summary).addToSummary(i);
            otherBucket.getMeasure("my.metric", MetricType.summary).addToSummary(100 - i);
        }

        AttributeBucket merged = otherBucket.merge(bucket);

        assertSame(otherBucket, merged);

        Collection<CustomInsightsEvent> events = new ArrayList<>();
        merged.harvest(events);

        assertEquals(1, events.size());

        Map<Object, List<CustomInsightsEvent>> byRegion = events.stream().collect(Collectors.groupingBy(event -> event.getUserAttributesCopy().get("region")));
        CustomInsightsEvent us = byRegion.get("us").iterator().next();
        List<Number> summary = (List<Number>) us.getIntrinsics().get("metric.summary");
        assertEquals(10l, summary.get(0));
        assertEquals(500d, summary.get(1));
        assertEquals(0d, summary.get(2));
        assertEquals(100d, summary.get(3));
    }
}
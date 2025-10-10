/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.newrelic.agent.model.PriorityAware;
import com.newrelic.agent.model.SpanCategory;
import com.newrelic.agent.model.SpanEvent;
import org.hamcrest.CoreMatchers;
import org.hamcrest.number.IsCloseTo;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class DistributedSamplingPriorityQueueTest {

    static class SimplePriorityAware implements PriorityAware {
        private final float priority;

        public SimplePriorityAware(float priority) {
            this.priority = priority;
        }

        @Override
        public float getPriority() {
            return priority;
        }

        public static final Comparator<SimplePriorityAware> COMPARATOR =new Comparator<SimplePriorityAware>() {
            @Override
            public int compare(SimplePriorityAware o1, SimplePriorityAware o2) {
                // descending priority
                return Float.compare(o2.getPriority(), o1.getPriority());
            }
        };
    }

    @Test
    public void resultsAreSameForBothRetryAll() {
        DistributedSamplingPriorityQueue<SimplePriorityAware> target = new DistributedSamplingPriorityQueue<>(5, SimplePriorityAware.COMPARATOR);

        target.add(new SimplePriorityAware( 1.2f));
        target.add(new SimplePriorityAware(0.1f));
        target.add(new SimplePriorityAware(1.3f));
        target.add(new SimplePriorityAware(1.4f));
        target.add(new SimplePriorityAware(1.5f));
        target.add(new SimplePriorityAware(1.6f));
        target.add(new SimplePriorityAware(0.7f));
        target.add(new SimplePriorityAware(2.3f));

        assertEquals(5, target.size());

        DistributedSamplingPriorityQueue<SimplePriorityAware> retryTarget = getRetryTarget();
        retryTarget.retryAll(target);
        validateRetryTargetContents(retryTarget);

        DistributedSamplingPriorityQueue<SimplePriorityAware> retryTargetList = getRetryTarget();
        retryTargetList.retryAll(target);
        validateRetryTargetContents(retryTargetList);
    }

    public void validateRetryTargetContents(DistributedSamplingPriorityQueue<SimplePriorityAware> retryTarget) {
        assertEquals(5, retryTarget.size());
        float total = 0;

        Set<Float> seenPriorities = new TreeSet<>();
        for(SimplePriorityAware element = retryTarget.poll(); element != null; element = retryTarget.poll()) {
            assertThat((double) element.getPriority(), CoreMatchers.anyOf(
                    IsCloseTo.closeTo(2.3, 0.001),
                    IsCloseTo.closeTo(1.7, 0.001),
                    IsCloseTo.closeTo(1.6, 0.001),
                    IsCloseTo.closeTo(1.5, 0.001),
                    IsCloseTo.closeTo(1.4, 0.001)
            ));
            seenPriorities.add(element.getPriority());
            total += element.getPriority();
        }
        assertEquals("Expected a correct sum", 8.5f, total, 0.0001);
        assertEquals("expected 5 distinct values", 5, seenPriorities.size());
    }

    public DistributedSamplingPriorityQueue<SimplePriorityAware> getRetryTarget() {
        DistributedSamplingPriorityQueue<SimplePriorityAware> retryTarget = new DistributedSamplingPriorityQueue<>(5, SimplePriorityAware.COMPARATOR);
        retryTarget.add(new SimplePriorityAware(1.1f));
        retryTarget.add(new SimplePriorityAware(0.9f));
        retryTarget.add(new SimplePriorityAware(1.7f));
        assertEquals(3, retryTarget.size());
        return retryTarget;
    }

    @Test
    public void testDefaultSort() {
        DistributedSamplingPriorityQueue<SpanEvent> eventPool = new DistributedSamplingPriorityQueue<>(10);
        seedEventPool(eventPool);

        List<SpanEvent> spanEvents = eventPool.asList();

        Set<String> highPriorityGuids = Sets.newHashSet("1", "4", "7");
        Set<String> mediumPriorityGuids = Sets.newHashSet("2", "5", "8");
        Set<String> lowPriorityGuids = Sets.newHashSet("3", "6", "9");

        // Should be sorted by priority (default sort)
        SpanEvent event = Iterables.get(spanEvents, 0);
        assertNotNull(event);
        assertTrue(highPriorityGuids.contains(event.getGuid()));
        assertEquals(5.0f, event.getPriority(), 0.0f);

        event = Iterables.get(spanEvents, 1);
        assertNotNull(event);
        assertTrue(highPriorityGuids.contains(event.getGuid()));
        assertEquals(5.0f, event.getPriority(), 0.0f);

        event = Iterables.get(spanEvents, 2);
        assertNotNull(event);
        assertTrue(highPriorityGuids.contains(event.getGuid()));
        assertEquals(5.0f, event.getPriority(), 0.0f);

        event = Iterables.get(spanEvents, 3);
        assertNotNull(event);
        assertTrue(mediumPriorityGuids.contains(event.getGuid()));
        assertEquals(3.0f, event.getPriority(), 0.0f);

        event = Iterables.get(spanEvents, 4);
        assertNotNull(event);
        assertTrue(mediumPriorityGuids.contains(event.getGuid()));
        assertEquals(3.0f, event.getPriority(), 0.0f);

        event = Iterables.get(spanEvents, 5);
        assertNotNull(event);
        assertTrue(mediumPriorityGuids.contains(event.getGuid()));
        assertEquals(3.0f, event.getPriority(), 0.0f);

        event = Iterables.get(spanEvents, 6);
        assertNotNull(event);
        assertTrue(lowPriorityGuids.contains(event.getGuid()));
        assertEquals(1.0f, event.getPriority(), 0.0f);

        event = Iterables.get(spanEvents, 7);
        assertNotNull(event);
        assertTrue(lowPriorityGuids.contains(event.getGuid()));
        assertEquals(1.0f, event.getPriority(), 0.0f);

        event = Iterables.get(spanEvents, 8);
        assertNotNull(event);
        assertTrue(lowPriorityGuids.contains(event.getGuid()));
        assertEquals(1.0f, event.getPriority(), 0.0f);
    }

    @Test
    public void testPriorityExternalSort() {
        DistributedSamplingPriorityQueue<SpanEvent> eventPool = new DistributedSamplingPriorityQueue<>(10, CUSTOM_COMPARATOR);
        seedEventPool(eventPool);

        List<SpanEvent> spanEvents = eventPool.asList();
        assertPriorityExternalEvents(spanEvents); // first, assert using what we get back from "asList()";

        spanEvents = new ArrayList<>();
        SpanEvent event;
        while ((event = eventPool.poll()) != null) {
            spanEvents.add(event);
        }
        assertPriorityExternalEvents(spanEvents); // second, assert using what we get back from "poll()";
    }

    @Test
    public void testQueueSize() {
        final DistributedSamplingPriorityQueue<SpanEvent> sizeTenQueue = new DistributedSamplingPriorityQueue<>(10);
        addSpanEvents(100, sizeTenQueue);
        assertEquals(10, sizeTenQueue.size());

        final DistributedSamplingPriorityQueue<SpanEvent> sizeOneQueue = new DistributedSamplingPriorityQueue<>(1);
        addSpanEvents(50, sizeOneQueue);
        assertEquals(1, sizeOneQueue.size());

        final DistributedSamplingPriorityQueue<SpanEvent> sizeZeroQueue = new DistributedSamplingPriorityQueue<>(0);
        addSpanEvents(33, sizeZeroQueue);
        assertEquals(0, sizeZeroQueue.size());
    }

    private void addSpanEvents(int numberToAdd, DistributedSamplingPriorityQueue<SpanEvent> queue) {
        SpanEvent spanEvent = new SpanEventFactory("Unit Test")
                .setGuid("9")
                .setCategory(SpanCategory.http)
                .setPriority(1.0f)
                .build();

        for (int i = 0; i < numberToAdd; i++) {
            queue.add(spanEvent);
        }
    }

    private void assertPriorityExternalEvents(List<SpanEvent> spanEvents) {
        // Should be sorted by priority and then sorted by type (external at top)
        SpanEvent event = Iterables.get(spanEvents, 0);
        assertNotNull(event);
        assertEquals("7", event.getGuid());
        assertEquals(SpanCategory.http, event.getCategory());
        assertEquals(5.0f, event.getPriority(), 0.0f);

        event = Iterables.get(spanEvents, 1);
        assertNotNull(event);
        assertEquals("4", event.getGuid());
        assertEquals(SpanCategory.datastore, event.getCategory());
        assertEquals(5.0f, event.getPriority(), 0.0f);

        event = Iterables.get(spanEvents, 2);
        assertNotNull(event);
        assertEquals("1", event.getGuid());
        assertEquals(SpanCategory.generic, event.getCategory());
        assertEquals(5.0f, event.getPriority(), 0.0f);

        event = Iterables.get(spanEvents, 3);
        assertNotNull(event);
        assertEquals("8", event.getGuid());
        assertEquals(SpanCategory.http, event.getCategory());
        assertEquals(3.0f, event.getPriority(), 0.0f);

        event = Iterables.get(spanEvents, 4);
        assertNotNull(event);
        assertEquals("5", event.getGuid());
        assertEquals(SpanCategory.datastore, event.getCategory());
        assertEquals(3.0f, event.getPriority(), 0.0f);

        event = Iterables.get(spanEvents, 5);
        assertNotNull(event);
        assertEquals("2", event.getGuid());
        assertEquals(SpanCategory.generic, event.getCategory());
        assertEquals(3.0f, event.getPriority(), 0.0f);

        event = Iterables.get(spanEvents, 6);
        assertNotNull(event);
        assertEquals("9", event.getGuid());
        assertEquals(SpanCategory.http, event.getCategory());
        assertEquals(1.0f, event.getPriority(), 0.0f);

        event = Iterables.get(spanEvents, 7);
        assertNotNull(event);
        assertEquals("6", event.getGuid());
        assertEquals(SpanCategory.datastore, event.getCategory());
        assertEquals(1.0f, event.getPriority(), 0.0f);

        event = Iterables.get(spanEvents, 8);
        assertNotNull(event);
        assertEquals("3", event.getGuid());
        assertEquals(SpanCategory.generic, event.getCategory());
        assertEquals(1.0f, event.getPriority(), 0.0f);
    }

    private void seedEventPool(DistributedSamplingPriorityQueue<SpanEvent> eventPool) {
        // Custom events
        SpanEvent spanEvent = new SpanEventFactory("appName")
                .setGuid("1")
                .setCategory(SpanCategory.generic)
                .setPriority(5.0f)
                .build();
        eventPool.add(spanEvent);

        spanEvent = new SpanEventFactory("appName")
                .setGuid("2")
                .setCategory(SpanCategory.generic)
                .setPriority(3.0f)
                .build();
        eventPool.add(spanEvent);

        spanEvent = new SpanEventFactory("appName")
                .setGuid("3")
                .setCategory(SpanCategory.generic)
                .setPriority(1.0f)
                .build();
        eventPool.add(spanEvent);

        // Datastore events
        spanEvent = new SpanEventFactory("appName")
                .setGuid("4")
                .setCategory(SpanCategory.datastore)
                .setPriority(5.0f)
                .build();
        eventPool.add(spanEvent);

        spanEvent = new SpanEventFactory("appName")
                .setGuid("5")
                .setCategory(SpanCategory.datastore)
                .setPriority(3.0f)
                .build();
        eventPool.add(spanEvent);

        spanEvent = new SpanEventFactory("appName")
                .setGuid("6")
                .setCategory(SpanCategory.datastore)
                .setPriority(1.0f)
                .build();
        eventPool.add(spanEvent);

        // External events
        spanEvent = new SpanEventFactory("appName")
                .setGuid("7")
                .setCategory(SpanCategory.http)
                .setPriority(5.0f)
                .build();
        eventPool.add(spanEvent);

        spanEvent = new SpanEventFactory("appName")
                .setGuid("8")
                .setCategory(SpanCategory.http)
                .setPriority(3.0f)
                .build();
        eventPool.add(spanEvent);

        spanEvent = new SpanEventFactory("appName")
                .setGuid("9")
                .setCategory(SpanCategory.http)
                .setPriority(1.0f)
                .build();
        eventPool.add(spanEvent);
    }

    static final Comparator<SpanEvent> CUSTOM_COMPARATOR = new Comparator<SpanEvent>() {
        @Override
        public int compare(SpanEvent left, SpanEvent right) {
            return ComparisonChain.start()
                    .compare(right.getPriority(), left.getPriority()) // Take highest priority first
                    .compare(left.getCategory(), right.getCategory())
                    .result();
        }
    };
}

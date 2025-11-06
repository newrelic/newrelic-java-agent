package com.newrelic.agent.model;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class AnalyticsEventTest {
    @Test
    public void isValidType_withNullType_returnsFalse() {
        Assert.assertFalse(AnalyticsEvent.isValidType(null));
    }

    @Test
    public void isValidType_withValidType_returnsTrue() {
        Assert.assertTrue(AnalyticsEvent.isValidType("1234_abcd"));
    }

    @Test
    public void isValidType_withInvalidType_returnsTrue() {
        Assert.assertFalse(AnalyticsEvent.isValidType(new String(new char[500]).replace("\0", "z")));
    }

    @Test
    public void constructorWithNullUserAttrs_createsEmptyMap() {
        AnalyticsEvent event = new TestAnalyticsEvent("type", 1, 0.9F, null);
        Assert.assertNotNull(event.getMutableUserAttributes());
        Assert.assertEquals(0, event.getMutableUserAttributes().size());
    }

    @Test
    public void constructorWithNonNullUserAttrs_createsMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("key1", "val1");
        map.put("key2", "val2");
        AnalyticsEvent event = new TestAnalyticsEvent("type", 1, 0.9F, map);
        Assert.assertNotNull(event.getMutableUserAttributes());
        Assert.assertEquals(2, event.getMutableUserAttributes().size());
    }

    @Test
    public void allGetters_returnProperly() {
        AnalyticsEvent event = new TestAnalyticsEvent("type", 1, 0.9F, null);
        Assert.assertEquals(1L, event.getTimestamp());
        Assert.assertEquals(0.9F, event.getPriority(), .1);
        Assert.assertEquals("type", event.getType());
        Assert.assertTrue(event.isValid());
    }

    @Test
    public void EqualsAndHashCode_returnsProperHash() {
        Assert.assertEquals(-1169950668, new TestAnalyticsEvent("type", 1, 0.9F, null).hashCode());

        TestAnalyticsEvent e1 = new TestAnalyticsEvent("type", 1, 0.9F, null);
        TestAnalyticsEvent e2 = new TestAnalyticsEvent("type", 1, 0.9F, null);
        Assert.assertEquals(e1, e2);
    }

    // Test concrete impl since super class is abstract
    public static class TestAnalyticsEvent extends AnalyticsEvent {
        protected TestAnalyticsEvent(String type, long timestamp, float priority, Map<String, ?> userAttributes) {
            super(type, timestamp, priority, userAttributes);
        }
    }
}

package com.newrelic.agent.tracers;

import org.junit.Assert;
import org.junit.Test;

public class TracerFlagsTest {
    @Test
    public void isRoot_returnsCorrectValue() {
        Assert.assertTrue(TracerFlags.isRoot(TracerFlags.DISPATCHER | TracerFlags.ASYNC));
        Assert.assertTrue(TracerFlags.isRoot(TracerFlags.DISPATCHER));
        Assert.assertTrue(TracerFlags.isRoot(TracerFlags.ASYNC));
        Assert.assertFalse(TracerFlags.isRoot(TracerFlags.LEAF));
        Assert.assertFalse(TracerFlags.isRoot(0));
    }

    @Test
    public void forceMandatoryRootFlags_setProperFlags() {
        Assert.assertEquals((TracerFlags.GENERATE_SCOPED_METRIC | TracerFlags.TRANSACTION_TRACER_SEGMENT), TracerFlags.forceMandatoryRootFlags(0));
    }

    @Test
    public void isAsync_returnsCorrectValue() {
        Assert.assertTrue(TracerFlags.isAsync(TracerFlags.ASYNC));
        Assert.assertFalse(TracerFlags.isAsync(TracerFlags.DISPATCHER));
        Assert.assertFalse(TracerFlags.isAsync(0));
    }

    @Test
    public void isDispatcher_returnsCorrectValue() {
        Assert.assertTrue(TracerFlags.isDispatcher(TracerFlags.DISPATCHER));
        Assert.assertFalse(TracerFlags.isDispatcher(TracerFlags.ASYNC));
        Assert.assertFalse(TracerFlags.isDispatcher(0));
    }

    @Test
    public void isCustom_returnsCorrectValue() {
        Assert.assertTrue(TracerFlags.isCustom(TracerFlags.CUSTOM));
        Assert.assertFalse(TracerFlags.isCustom(TracerFlags.ASYNC));
        Assert.assertFalse(TracerFlags.isCustom(0));
    }

    @Test
    public void getDispatcherFlags_returnsDispatcherFlag() {
        Assert.assertEquals(TracerFlags.DISPATCHER, TracerFlags.getDispatcherFlags(true));
        Assert.assertEquals(0, TracerFlags.getDispatcherFlags(false));
    }

    @Test
    public void clearAsync_removesAsyncFlagIfPresent() {
        Assert.assertEquals(0, TracerFlags.clearAsync(TracerFlags.ASYNC));
    }

    @Test
    public void clearSegment_removesSegmentFlagIfPresent() {
        Assert.assertEquals(0, TracerFlags.clearSegment(TracerFlags.TRANSACTION_TRACER_SEGMENT));
    }
}

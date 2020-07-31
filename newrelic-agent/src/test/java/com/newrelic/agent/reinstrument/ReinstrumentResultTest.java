/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.reinstrument;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ReinstrumentResultTest {

    @Test
    public void testgetStatusMap() {
        ReinstrumentResult result = new ReinstrumentResult();
        Map<String, Object> actual = result.getStatusMap();
        Assert.assertNotNull(actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        Assert.assertEquals(0, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));

        Assert.assertNull(actual.get(ReinstrumentResult.ERROR_KEY));
        Assert.assertNull(actual.get(ReinstrumentResult.RETRANSFORM_INIT_KEY));

        result.addErrorMessage("Hello");
        result.addErrorMessage("Hi there");
        result.setPointCutsSpecified(4);

        actual = result.getStatusMap();
        Assert.assertEquals("Hello, Hi there", actual.get(ReinstrumentResult.ERROR_KEY));
        Assert.assertEquals(4, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        Assert.assertNull(actual.get(ReinstrumentResult.RETRANSFORM_INIT_KEY));

        Set<String> initClasses = new HashSet<>();
        initClasses.add("theClass");
        initClasses.add("com.nr.test");
        result.setRetranformedInitializedClasses(initClasses);
        result.addErrorMessage("next message ");
        actual = result.getStatusMap();
        Assert.assertEquals("Hello, Hi there, next message ", actual.get(ReinstrumentResult.ERROR_KEY));
        Assert.assertEquals(4, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        Assert.assertTrue(((String) actual.get(ReinstrumentResult.RETRANSFORM_INIT_KEY)).contains("theClass"));
        Assert.assertTrue(((String) actual.get(ReinstrumentResult.RETRANSFORM_INIT_KEY)).contains("com.nr.test"));
    }

    @Test
    public void testToString() {
        ReinstrumentResult result = new ReinstrumentResult();
        Assert.assertNotNull(result.toString());

        result.addErrorMessage("yikes");
        result.addErrorMessage("hello");
        Assert.assertNotNull(result.toString());

        Set<String> initClasses = new HashSet<>();
        initClasses.add("yum");
        initClasses.add("fun");
        result.setRetranformedInitializedClasses(initClasses);
        Assert.assertNotNull(result.toString());

    }
}

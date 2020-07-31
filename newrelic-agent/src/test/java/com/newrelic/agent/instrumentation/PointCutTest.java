/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import java.util.HashMap;

import org.junit.Assert;

import org.junit.Test;

import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;

public class PointCutTest {

    @Test
    public void verifyEqualsAndHashCode() throws Exception {
        AgentTestUtils.createServiceManager(new HashMap<String, Object>());
        PointCut pc1 = new DefaultPointCut(new PointCutConfiguration("hi"), null, new ExactClassMatcher("myclass"),
                new ExactMethodMatcher("mymethod", "()V"));
        PointCut pc2 = new DefaultPointCut(new PointCutConfiguration("hi"), null, new ExactClassMatcher("myclass"),
                new ExactMethodMatcher("mymethod", "()V"));
        Assert.assertTrue(pc1.equals(pc2));
        Assert.assertEquals(pc1.hashCode(), pc2.hashCode());

        pc2 = new DefaultPointCut(new PointCutConfiguration("hi"), null, new ExactClassMatcher("myotherClass"),
                new ExactMethodMatcher("mymethod", "()V"));
        Assert.assertFalse(pc1.equals(pc2));
        Assert.assertNotSame(pc1.hashCode(), pc2.hashCode());

        pc2 = new DefaultPointCut(new PointCutConfiguration("hi"), null, new ExactClassMatcher("myclass"),
                new ExactMethodMatcher("myothermethod", "()V"));
        Assert.assertFalse(pc1.equals(pc2));
        Assert.assertNotSame(pc1.hashCode(), pc2.hashCode());
    }

}

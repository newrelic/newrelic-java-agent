/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile;

import java.util.List;

import org.junit.Assert;

import org.junit.Test;

import com.newrelic.agent.Agent;
import com.newrelic.agent.AgentHelper;

public class ProfileSegmentTest {

    private ProfileSegment createProfileSegment(StackTraceElement stackElement) {
        ProfiledMethod method = ProfiledMethod.newProfiledMethod(stackElement);
        if (method == null) {
            return null;
        }
        return ProfileSegment.newProfileSegment(method);
    }

    private Profile getProfile() {
        ProfilerParameters parameters = new ProfilerParameters(0L, 0L, 0L, false, false, Agent.isDebugEnabled(), null,
                null);
        return new Profile(parameters);
    }

    @Test
    public void testToString() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();

        ProfileSegment segment = createProfileSegment(stack[0]);
        Assert.assertNotNull(segment.toString());

        segment = createProfileSegment(stack[1]);
        Assert.assertNotNull(segment.toString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testToJsonStringNoParent() throws Exception {
        ProfileSegment segment = createProfileSegment(new StackTraceElement("com.acme.Rocket", "takeOff()",
                "rocket.java", 14));
        segment.incrementRunnableCallCount();
        segment.incrementRunnableCallCount();

        Object parse = AgentHelper.serializeJSON(segment);
        Assert.assertTrue(parse instanceof List);

        List data = (List) parse;
        Assert.assertEquals(4, data.size());
        List methodData = (List) data.get(0);
        Assert.assertEquals(segment.getMethod().getClassName(), methodData.get(0));
        Assert.assertEquals(segment.getMethod().getMethodName(), methodData.get(1));
        Assert.assertEquals(2l, data.get(1));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testJsonRealStack() throws Exception {
        Profile profile = getProfile();
        profile.addStackTrace(0, true, ThreadType.BasicThreadType.OTHER, Thread.currentThread().getStackTrace());
        ProfileSegment segment = profile.getProfileTree(ThreadType.BasicThreadType.OTHER).getRootSegments().iterator().next();

        Object parse = AgentHelper.serializeJSON(segment);
        Assert.assertTrue(parse instanceof List);

        List data = (List) parse;
        Assert.assertEquals(4, data.size());
    }
}

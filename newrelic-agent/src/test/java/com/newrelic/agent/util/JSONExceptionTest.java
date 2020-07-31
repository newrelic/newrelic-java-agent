/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import java.util.Collection;
import java.util.Map;

import org.junit.Assert;

import org.junit.Test;

import com.newrelic.agent.AgentHelper;

public class JSONExceptionTest {
    private static final String message = "Testing";

    @Test
    public void testJSON() throws Exception {
        JSONException ex = new JSONException(message);
        Map json = (Map) AgentHelper.serializeJSON(ex);

        Assert.assertEquals(1, json.size());

        Map errorMap = (Map) json.get("exception");
        Assert.assertNotNull(errorMap);

        Assert.assertEquals(message, errorMap.get("message"));
        Assert.assertNull(errorMap.get("type"));

        Object backtrace = errorMap.get("backtrace");
        Assert.assertNotNull(backtrace);

        Assert.assertTrue(((Collection) backtrace).size() > 0);
    }

    @Test
    public void wrappedException() throws Exception {
        JSONException ex = new JSONException(message, new ArrayIndexOutOfBoundsException());
        Map json = (Map) AgentHelper.serializeJSON(ex);

        Assert.assertEquals(1, json.size());

        Map errorMap = (Map) json.get("exception");
        Assert.assertNotNull(errorMap);

        Assert.assertEquals(message, errorMap.get("message"));
        Assert.assertEquals(ArrayIndexOutOfBoundsException.class.getName(), errorMap.get("type"));
    }
}

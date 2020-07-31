/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile.v2;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.util.StringMap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class Murmur3StringMapTest {
    @Test
    public void hasCorrectContents() {
        StringMap target = new Murmur3StringMap();

        assertNull(target.addString(null));
        assertEquals("abc123", target.addString("abc123"));
        assertEquals("deu/YhWl/74=", target.addString("some super long string like a class name"));

        assertEquals(
                ImmutableMap.<String, Object>of(
                        "abc123", "abc123",
                        "deu/YhWl/74=", "some super long string like a class name"),
                target.getStringMap());
    }
}
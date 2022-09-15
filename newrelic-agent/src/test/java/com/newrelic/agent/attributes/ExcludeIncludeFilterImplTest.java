/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class ExcludeIncludeFilterImplTest {

    @Test
    public void testExplicitInclude() {
        ExcludeIncludeFilterImpl filter = new ExcludeIncludeFilterImpl("any", Collections.singleton("exclude"), Collections.singleton("include"));
        assertFalse(filter.shouldInclude("exclude"));
        assertFalse(filter.shouldInclude("asdf"));
        assertTrue(filter.shouldInclude("include"));
    }

    @Test
    public void testIncludeByDefault() {
        ExcludeIncludeFilterImpl filter = new ExcludeIncludeFilterImpl("any", Collections.singleton("exclude"), Collections.emptySet());
        assertFalse(filter.shouldInclude("exclude"));
        assertTrue(filter.shouldInclude("asdf"));
        assertTrue(filter.shouldInclude("include"));
    }

}
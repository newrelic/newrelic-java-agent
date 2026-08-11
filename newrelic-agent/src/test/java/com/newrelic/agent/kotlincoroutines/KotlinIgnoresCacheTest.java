/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.kotlincoroutines;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class KotlinIgnoresCacheTest {

    @Test
    public void addIgnoredFramework_valueAppearsInResults() {
        KotlinIgnoresCache.addIgnoredFramework("com.nr.test.cache.unique1");

        List<String> frameworks = Arrays.asList(KotlinIgnoresCache.getIgnoredFrameworks());
        assertTrue(frameworks.contains("com.nr.test.cache.unique1"));
    }

    @Test
    public void getIgnoredFrameworks_returnsNonNullArray() {
        assertNotNull(KotlinIgnoresCache.getIgnoredFrameworks());
    }

    @Test
    public void getIgnoredFrameworks_returnsDifferentArrayEachCall() {
        // Each call produces a fresh copy so callers cannot mutate shared state.
        String[] first = KotlinIgnoresCache.getIgnoredFrameworks();
        String[] second = KotlinIgnoresCache.getIgnoredFrameworks();
        assertNotSame(first, second);
    }

    @Test
    public void addIgnoredFramework_deduplicatesEntries() {
        String unique = "com.nr.test.cache.unique2";
        KotlinIgnoresCache.addIgnoredFramework(unique);
        KotlinIgnoresCache.addIgnoredFramework(unique);

        long count = Arrays.stream(KotlinIgnoresCache.getIgnoredFrameworks())
                .filter(unique::equals)
                .count();
        assertEquals(1, count);
    }
}

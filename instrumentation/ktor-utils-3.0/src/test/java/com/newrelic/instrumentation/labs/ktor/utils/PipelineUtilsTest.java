/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.instrumentation.labs.ktor.utils;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PipelineUtilsTest {

    @Test
    public void testTracksHttpResponsePipeline() {
        assertTrue(PipelineUtils.tracePipeline("HttpResponsePipeline"));
    }

    @Test
    public void testDoesNotTrackUnlistedPipelines() {
        assertFalse(PipelineUtils.tracePipeline("ApplicationCallPipeline"));
        assertFalse(PipelineUtils.tracePipeline("OtherPipeline"));
    }

    @Test
    public void testDoesNotTrackEmptyName() {
        assertFalse(PipelineUtils.tracePipeline(""));
    }
}

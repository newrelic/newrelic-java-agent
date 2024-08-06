/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.sdk.trace;

import com.newrelic.api.agent.Config;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NRSpanBuilderTest {

    @Test
    public void testIsSpanBuilderEnabled() {
        assertTrue(NRSpanBuilder.isSpanBuilderEnabled(createConfig(null, null)));
        assertTrue(NRSpanBuilder.isSpanBuilderEnabled(createConfig(true, null)));
        assertTrue(NRSpanBuilder.isSpanBuilderEnabled(createConfig(null, true)));
        assertTrue(NRSpanBuilder.isSpanBuilderEnabled(createConfig(true, true)));
    }

    @Test
    public void testIsSpanBuilderDisabled() {
        assertFalse(NRSpanBuilder.isSpanBuilderEnabled(createConfig(false, true)));
        assertFalse(NRSpanBuilder.isSpanBuilderEnabled(createConfig(false, null)));
        assertFalse(NRSpanBuilder.isSpanBuilderEnabled(createConfig(null, false)));
        assertFalse(NRSpanBuilder.isSpanBuilderEnabled(createConfig(true, false)));
    }

    private Config createConfig(Boolean autoconfigureEnabled, Boolean spansEnabled) {
        Config config = mock(Config.class);
        if (autoconfigureEnabled != null) {
            when(config.getValue("opentelemetry.sdk.autoconfigure.enabled")).thenReturn(autoconfigureEnabled);
        }
        if (spansEnabled != null) {
            when(config.getValue("opentelemetry.sdk.spans.enabled")).thenReturn(spansEnabled);
        }
        return config;
    }

}
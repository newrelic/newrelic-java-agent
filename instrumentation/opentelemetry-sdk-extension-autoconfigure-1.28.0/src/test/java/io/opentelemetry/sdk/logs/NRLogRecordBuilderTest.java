/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.sdk.logs;

import com.newrelic.api.agent.Config;
import junit.framework.TestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NRLogRecordBuilderTest extends TestCase {

    public void testIsLogRecordBuilderEnabled() {
        assertTrue(NRLogRecordBuilder.isLogRecordBuilderEnabled(createConfig(null, null)));
        assertTrue(NRLogRecordBuilder.isLogRecordBuilderEnabled(createConfig(true, null)));
        assertTrue(NRLogRecordBuilder.isLogRecordBuilderEnabled(createConfig(null, true)));
        assertTrue(NRLogRecordBuilder.isLogRecordBuilderEnabled(createConfig(true, true)));
    }

    public void testIsLogRecordBuilderDisabled() {
        assertFalse(NRLogRecordBuilder.isLogRecordBuilderEnabled(createConfig(false, true)));
        assertFalse(NRLogRecordBuilder.isLogRecordBuilderEnabled(createConfig(false, null)));
        assertFalse(NRLogRecordBuilder.isLogRecordBuilderEnabled(createConfig(null, false)));
        assertFalse(NRLogRecordBuilder.isLogRecordBuilderEnabled(createConfig(true, false)));
    }

    private Config createConfig(Boolean autoconfigureEnabled, Boolean logsEnabled) {
        Config config = mock(Config.class);
        if (autoconfigureEnabled != null) {
            when(config.getValue("opentelemetry.sdk.autoconfigure.enabled", false)).thenReturn(autoconfigureEnabled);
        }
        if (logsEnabled != null) {
            when(config.getValue("opentelemetry.sdk.logs.enabled", true)).thenReturn(logsEnabled);
        }
        return config;
    }
}

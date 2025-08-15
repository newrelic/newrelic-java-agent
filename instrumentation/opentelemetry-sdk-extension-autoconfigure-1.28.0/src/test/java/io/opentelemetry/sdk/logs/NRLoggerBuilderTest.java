/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.sdk.logs;

import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.NewRelic;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.resources.Resource;
import junit.framework.TestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NRLoggerBuilderTest extends TestCase {
    final LoggerSharedState LOGGER_SHARED_STATE = new LoggerSharedState(Resource.empty(), LogLimits::getDefault, NoopLogRecordProcessor.getInstance(),
            Clock.getDefault());

    public void testBuild() {
        Logger logger = new NRLoggerBuilder(NewRelic.getAgent().getConfig(), "test-lib", LOGGER_SHARED_STATE).build();

        assertTrue(logger.getClass().getName(), logger.getClass().getName().startsWith(
                "io.opentelemetry.sdk.logs.NRLoggerBuilder"));
    }

    public void testBuildDisabled() {
        Config config = mock(Config.class);
        when(config.getValue("opentelemetry.instrumentation.test-lib.enabled", true)).thenReturn(false);
        Logger logger = new NRLoggerBuilder(config, "test-lib", LOGGER_SHARED_STATE).build();
        assertSame(OpenTelemetry.noop().getLogsBridge().get("test-lib"), logger);
    }
}

/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.sdk.logs;

import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.resources.Resource;
import junit.framework.TestCase;

public class NRLoggerBuilderTest extends TestCase {
    final LoggerSharedState LOGGER_SHARED_STATE = new LoggerSharedState(Resource.empty(), LogLimits::getDefault, NoopLogRecordProcessor.getInstance(),
            Clock.getDefault());

    public void testBuild() {
        Logger logger = new NRLoggerBuilder("test-lib", LOGGER_SHARED_STATE).build();

        assertTrue(logger.getClass().getName(), logger.getClass().getName().startsWith(
                "io.opentelemetry.sdk.logs.NRLoggerBuilder"));
    }
}

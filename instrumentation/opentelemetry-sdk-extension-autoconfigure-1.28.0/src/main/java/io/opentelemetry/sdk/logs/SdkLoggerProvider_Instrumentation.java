/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.sdk.logs;

import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.opentelemetry.api.logs.LoggerBuilder;

/**
 * Weaved to inject a New Relic Java agent implementation of an OpenTelemetry LoggerBuilder
 */
@Weave(type = MatchType.ExactClass, originalName = "io.opentelemetry.sdk.logs.SdkLoggerProvider")
public final class SdkLoggerProvider_Instrumentation {
    private final LoggerSharedState sharedState = Weaver.callOriginal();

    public LoggerBuilder loggerBuilder(String instrumentationScopeName) {
        final LoggerBuilder loggerBuilder = Weaver.callOriginal();
        Config config = NewRelic.getAgent().getConfig();

        // Generate the instrumentation module supportability metric
        NewRelic.incrementCounter("Supportability/Logging/Java/OpenTelemetryBridge/enabled");

        if (NRLogRecordBuilder.isLogRecordBuilderEnabled(config)) {
            // return our logger builder instead of the OTel instance
            return new NRLoggerBuilder(config, instrumentationNameOrDefault(instrumentationScopeName), sharedState);
        }
        return loggerBuilder;
    }

    /**
     * Returns the instrumentation name or the default value of "unknown" if not set.
     *
     * @param instrumentationScopeName the name of the instrumentation scope
     * @return the instrumentation name or a default value
     */
    private static String instrumentationNameOrDefault(String instrumentationScopeName) {
        return Weaver.callOriginal();
    }

}

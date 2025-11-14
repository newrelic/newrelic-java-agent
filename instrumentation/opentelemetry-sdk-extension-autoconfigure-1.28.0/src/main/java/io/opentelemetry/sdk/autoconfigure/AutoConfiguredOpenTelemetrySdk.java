/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.sdk.autoconfigure;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.logging.Level;

import static com.nr.agent.instrumentation.utils.config.OpenTelemetryConfig.isOpenTelemetryMetricsEnabled;
import static com.nr.agent.instrumentation.utils.config.OpenTelemetryConfig.isOpenTelemetrySdkAutoConfigureEnabled;

/**
 * Weaved to autoconfigure the OpenTelemetrySDK properties
 * and resources for compatability with New Relic.
 */
@Weave(type = MatchType.ExactClass)
public class AutoConfiguredOpenTelemetrySdk {

    /**
     * Creates a new {@link AutoConfiguredOpenTelemetrySdkBuilder} with the default configuration.
     * If OTel Metrics signals are enabled, it will append customizers for properties and resources.
     *
     * @return a new {@link AutoConfiguredOpenTelemetrySdkBuilder}
     */
    public static AutoConfiguredOpenTelemetrySdkBuilder builder() {
        final AutoConfiguredOpenTelemetrySdkBuilder builder = Weaver.callOriginal();

        if (isOpenTelemetrySdkAutoConfigureEnabled() || isOpenTelemetryMetricsEnabled()) {
            // Generate the instrumentation module enabled supportability metric
            NewRelic.incrementCounter("Supportability/Metrics/Java/OpenTelemetryBridge/enabled");

            NewRelic.getAgent().getLogger().log(Level.INFO, "Appending OpenTelemetry SDK customizers");
            builder.addPropertiesCustomizer(OpenTelemetrySDKCustomizer::applyProperties);
            builder.addResourceCustomizer(OpenTelemetrySDKCustomizer::applyResources);
            builder.addMeterProviderCustomizer(OpenTelemetrySDKCustomizer::applyMeterExcludes);
        } else {
            // Generate the instrumentation module disabled supportability metric
            NewRelic.incrementCounter("Supportability/Metrics/Java/OpenTelemetryBridge/disabled");
        }
        return builder;
    }
}

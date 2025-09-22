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

/**
 * Weaved to autoconfigure the OpenTelemetrySDK properties
 * and resources for compatability with New Relic.
 */
@Weave(type = MatchType.ExactClass)
public class AutoConfiguredOpenTelemetrySdk {

    /**
     * Creates a new {@link AutoConfiguredOpenTelemetrySdkBuilder} with the default configuration.
     * If the agent configuration yaml, system property `-Dnewrelic.config.opentelemetry.sdk.autoconfigure.enabled`,
     * or environment variable NEW_RELIC_OPENTELEMETRY_SDK_AUTOCONFIGURE_ENABLED is set to true,
     * it will append customizers for properties and resources.
     *
     * @return a new {@link AutoConfiguredOpenTelemetrySdkBuilder}
     */
    public static AutoConfiguredOpenTelemetrySdkBuilder builder() {
        final AutoConfiguredOpenTelemetrySdkBuilder builder = Weaver.callOriginal();
        final Boolean autoConfigure = NewRelic.getAgent().getConfig().getValue("opentelemetry.sdk.autoconfigure.enabled", false);

        if (autoConfigure == null || autoConfigure) {
            // Generate the instrumentation module supportability metric
            NewRelic.incrementCounter("Supportability/Metrics/Java/OpenTelemetryBridge/enabled");

            NewRelic.getAgent().getLogger().log(Level.INFO, "Appending OpenTelemetry SDK customizers");
            builder.addPropertiesCustomizer(OpenTelemetrySDKCustomizer::applyProperties);
            builder.addResourceCustomizer(OpenTelemetrySDKCustomizer::applyResources);
            builder.addMeterProviderCustomizer(OpenTelemetrySDKCustomizer::applyMeterExcludes);
        }
        return builder;
    }
}

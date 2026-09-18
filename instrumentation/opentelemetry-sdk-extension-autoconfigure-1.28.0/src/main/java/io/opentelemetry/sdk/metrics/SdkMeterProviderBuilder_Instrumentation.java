/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.sdk.metrics;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.opentelemetry.sdk.metrics.export.MetricReader;

@Weave(originalName = "io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder", type = MatchType.ExactClass)
public class SdkMeterProviderBuilder_Instrumentation {

    public SdkMeterProviderBuilder_Instrumentation registerMetricReader(MetricReader reader) {
        if (AgentBridge.serverlessApi.isServerlessModeEnabled()) {
            AgentBridge.serverlessApi.addMetricCollector(reader, o -> {
                if (o instanceof MetricReader) {
                    ((MetricReader) o).forceFlush();
                }
            });
        }
        return Weaver.callOriginal();
    }
}

/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class NoOpServerlessApi implements ServerlessApi {

    @Override
    public void setArn(String arn) {
        // No-op
    }

    @Override
    public void setFunctionVersion(String functionVersion) {
        // No-op
    }

    @Override
    public String getArn() {
        return null;
    }

    @Override
    public String getFunctionVersion() {
        return null;
    }

    @Override
    public boolean isApmLambdaModeEnabled() {
        return false;
    }

    @Override
    public void removeMetricCollector(Object metricReader) {
        // No-op
    }

    @Override
    public void addMetricCollector(Object metricReader, Consumer<Object> metricCollector) {
        // No-op
    }

    @Override
    public boolean otelHarvest(Supplier<String> metricMarshaller) {
        return false;
    }
}

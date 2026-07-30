/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.sdk.autoconfigure;

import io.opentelemetry.sdk.metrics.data.DelegatingMetricData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.resources.Resource;

/**
 * A delegating MetricData that overrides getResource() to return a merged Resource
 * containing updated service metadata attributes.
 */
final class ResourceOverlayMetricData extends DelegatingMetricData {

    private final Resource mergedResource;

    ResourceOverlayMetricData(MetricData delegate, Resource mergedResource) {
        super(delegate);
        this.mergedResource = mergedResource;
    }

    @Override
    public Resource getResource() {
        return mergedResource;
    }
}

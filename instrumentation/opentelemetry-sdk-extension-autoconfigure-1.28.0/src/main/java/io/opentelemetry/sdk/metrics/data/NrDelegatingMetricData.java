/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.sdk.metrics.data;

import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.resources.Resource;

import static java.util.Objects.requireNonNull;

/**
 * This backports the functionality of the OpenTelemetry DelegatingMetricData, which
 * wasn't introduced until OTel version 1.50.0. This class delegates to the methods another MetricData, so that we can alter the MetricData that's exported.
 */
public abstract class NrDelegatingMetricData implements MetricData {

    private final MetricData delegate;

    protected NrDelegatingMetricData(MetricData delegate) {
        this.delegate = requireNonNull(delegate, "delegate");
    }

    @Override
    public Resource getResource() {
        return delegate.getResource();
    }

    @Override
    public InstrumentationScopeInfo getInstrumentationScopeInfo() {
        return delegate.getInstrumentationScopeInfo();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public String getDescription() {
        return delegate.getDescription();
    }

    @Override
    public String getUnit() {
        return delegate.getUnit();
    }

    @Override
    public MetricDataType getType() {
        return delegate.getType();
    }

    @Override
    public Data<?> getData() {
        return delegate.getData();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof MetricData) {
            MetricData that = (MetricData) o;
            return getResource().equals(that.getResource())
                    && getInstrumentationScopeInfo().equals(that.getInstrumentationScopeInfo())
                    && getName().equals(that.getName())
                    && getDescription().equals(that.getDescription())
                    && getUnit().equals(that.getUnit())
                    && getType().equals(that.getType())
                    && getData().equals(that.getData());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int code = 1;
        code *= 1000003;
        code ^= getResource().hashCode();
        code *= 1000003;
        code ^= getInstrumentationScopeInfo().hashCode();
        code *= 1000003;
        code ^= getName().hashCode();
        code *= 1000003;
        code ^= getDescription().hashCode();
        code *= 1000003;
        code ^= getUnit().hashCode();
        code *= 1000003;
        code ^= getType().hashCode();
        code *= 1000003;
        code ^= getData().hashCode();
        return code;
    }

    @Override
    public String toString() {
        return "NrDelegatingMetricData{"
                + "resource="
                + getResource()
                + ", instrumentationScopeInfo="
                + getInstrumentationScopeInfo()
                + ", name="
                + getName()
                + ", description="
                + getDescription()
                + ", unit="
                + getUnit()
                + ", type="
                + getType()
                + ", data="
                + getData()
                + "}";
    }
}

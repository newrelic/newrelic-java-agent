/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.awsjavasdk2.services.lambda;

import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.regions.Region;

import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * Data necessary to calculate the ARN. This class is used as the key for the ARN cache.
 */
public class FunctionRawData {
    private final String functionRef;
    private final String qualifier;
    // the code only cares about the region, but the config is stored
    // to prevent unnecessary calls to get the region
    private final WeakReference<SdkClientConfiguration> config;
    private final WeakReference<Object> sdkClient;

    public FunctionRawData(String functionRef, String qualifier, SdkClientConfiguration config, Object sdkClient) {
        this.functionRef = functionRef;
        this.qualifier = qualifier;
        this.config = new WeakReference<>(config);
        this.sdkClient = new WeakReference<>(sdkClient);
    }

    public String getFunctionRef() {
        return functionRef;
    }

    public String getQualifier() {
        return qualifier;
    }

    public String getRegion() {
        SdkClientConfiguration config = this.config.get();
        if (config != null) {
            Region region = config.option(AwsClientOption.AWS_REGION);
            if (region != null) {
                return region.id();
            }
        }
        return null;
    }

    public Object getSdkClient() {
        return sdkClient.get();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FunctionRawData)) {
            return false;
        }
        FunctionRawData that = (FunctionRawData) o;
        if (sdkClient.get() == null || that.sdkClient.get() == null ||
            config.get() == null || that.config.get() == null) {
            return false;
        }
        return Objects.equals(functionRef, that.functionRef) &&
                Objects.equals(qualifier, that.qualifier) &&
                // config uses Object.equals, so should be fast
                Objects.equals(config.get(), that.config.get()) &&
                // sdkClient uses Object.equals, so should be fast
                Objects.equals(sdkClient.get(), that.sdkClient.get());
    }

    @Override
    public int hashCode() {
        return Objects.hash(functionRef, qualifier, config.get(), sdkClient.get());
    }
}

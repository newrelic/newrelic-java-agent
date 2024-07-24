/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.awsjavasdk2.services.lambda;

import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;

import java.util.Objects;

/**
 * Data necessary to calculate the ARN. This class is used as the key for the ARN cache.
 */
public class FunctionRawData {
    private final String functionRef;
    private final String qualifier;
    // the code only cares about the region, but the config is stored
    // to prevent unnecessary calls to get the region
    private final SdkClientConfiguration config;

    public FunctionRawData(String functionRef, String qualifier, SdkClientConfiguration config) {
        this.functionRef = functionRef;
        this.qualifier = qualifier;
        this.config = config;
    }

    public String getFunctionRef() {
        return functionRef;
    }

    public String getQualifier() {
        return qualifier;
    }

    public String getRegion() {
        return config.option(AwsClientOption.AWS_REGION).toString();
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
        return Objects.equals(functionRef, that.functionRef) && Objects.equals(qualifier, that.qualifier) &&
                // config uses Object.equals, so should be fast
                Objects.equals(config, that.config);
    }

    @Override
    public int hashCode() {
        return Objects.hash(functionRef, qualifier, config);
    }
}

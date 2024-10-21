/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.awsjavasdk1.services.lambda;

import java.util.Objects;

/**
 * Data necessary to calculate the ARN. This class is used as the key for the ARN cache.
 */
public class FunctionRawData {
    private final String functionRef;
    private final String qualifier;
    private final String region;

    public FunctionRawData(String functionRef, String qualifier, String region) {
        this.functionRef = functionRef;
        this.qualifier = qualifier;
        this.region = region;
    }

    public String getFunctionRef() {
        return functionRef;
    }

    public String getQualifier() {
        return qualifier;
    }

    public String getRegion() {
        return region;
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
                Objects.equals(region, that.region);
    }

    @Override
    public int hashCode() {
        return Objects.hash(functionRef, qualifier, region);
    }
}

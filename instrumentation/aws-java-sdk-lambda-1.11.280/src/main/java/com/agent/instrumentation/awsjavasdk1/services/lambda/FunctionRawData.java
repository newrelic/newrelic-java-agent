/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.awsjavasdk1.services.lambda;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;

import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * Data necessary to calculate the ARN. This class is used as the key for the ARN cache.
 */
public class FunctionRawData {
    private final String functionRef;
    private final String qualifier;
    private final String region;
    private final WeakReference<Object> sdkClient;
    private final WeakReference<AWSCredentialsProvider> credentialsProvider;

    public FunctionRawData(String functionRef, String qualifier, String region, Object sdkClient, AWSCredentialsProvider credentialsProvider) {
        this.functionRef = functionRef;
        this.qualifier = qualifier;
        this.region = region;
        this.sdkClient = new WeakReference<>(sdkClient);
        this.credentialsProvider = new WeakReference<>(credentialsProvider);
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

    public Object getSdkClient() {
        return sdkClient.get();
    }

    public String getAccessKey() {
        AWSCredentialsProvider credentialsProvider = this.credentialsProvider.get();
        if (credentialsProvider != null) {
            AWSCredentials credentials = credentialsProvider.getCredentials();
            if (credentials != null) {
                return credentials.getAWSAccessKeyId();
            }
        }
        return null;
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
        if (this.sdkClient.get() == null || that.sdkClient.get() == null ||
                this.credentialsProvider.get() == null || that.credentialsProvider.get() == null) {
            return false;
        }
        return Objects.equals(functionRef, that.functionRef) &&
                Objects.equals(qualifier, that.qualifier) &&
                Objects.equals(region, that.region) &&
                Objects.equals(sdkClient.get(), that.sdkClient.get()) &&
                Objects.equals(credentialsProvider.get(), that.credentialsProvider.get());
    }

    @Override
    public int hashCode() {
        return Objects.hash(functionRef,
                qualifier,
                region,
                sdkClient.get(),
                credentialsProvider.get());
    }
}

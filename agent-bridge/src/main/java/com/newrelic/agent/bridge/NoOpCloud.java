/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import com.newrelic.api.agent.CloudAccountInfo;

public class NoOpCloud implements CloudApi {

    public static final CloudApi INSTANCE = new NoOpCloud();

    private NoOpCloud() {
        // only instance should be the INSTANCE
    }

    @Override
    public void setAccountInfo(CloudAccountInfo cloudAccountInfo, String value) {
    }

    @Override
    public void setAccountInfo(Object sdkClient, CloudAccountInfo cloudAccountInfo, String value) {
    }

    @Override
    public String getAccountInfo(CloudAccountInfo cloudAccountInfo) {
        return null;
    }

    @Override
    public String getAccountInfo(Object sdkClient, CloudAccountInfo cloudAccountInfo) {
        return null;
    }

    @Override
    public String decodeAwsAccountId(String accessKey) {
        return null;
    }
}

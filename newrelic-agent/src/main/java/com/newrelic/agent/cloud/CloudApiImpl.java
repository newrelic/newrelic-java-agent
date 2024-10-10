/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.cloud;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.CloudApi;
import com.newrelic.api.agent.CloudAccountInfo;

/**
 * Facade for the Cloud API.
 */
public class CloudApiImpl implements CloudApi {

    private final CloudAccountInfoCache accountInfoCache;

    private CloudApiImpl() {
        this(new CloudAccountInfoCache());
        accountInfoCache.retrieveDataFromConfig();
    }

    // for testing
    CloudApiImpl(CloudAccountInfoCache accountInfoCache) {
        this.accountInfoCache = accountInfoCache;
    }

    // calling this method more than once will invalidate any Cloud API calls to set account info
    public static void initialize() {
        AgentBridge.cloud = new CloudApiImpl();
    }

    @Override
    public void setAccountInfo(CloudAccountInfo cloudAccountInfo, String value) {
        MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_CLOUD_SET_ACCOUNT_INFO + cloudAccountInfo.toString());
        accountInfoCache.setAccountInfo(cloudAccountInfo, value);
    }

    @Override
    public void setAccountInfo(Object sdkClient, CloudAccountInfo cloudAccountInfo, String value) {
        MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_CLOUD_SET_ACCOUNT_INFO_CLIENT + cloudAccountInfo.toString());
        accountInfoCache.setAccountInfo(sdkClient, cloudAccountInfo, value);
    }

    @Override
    public String getAccountInfo(CloudAccountInfo cloudAccountInfo) {
        // not recording metrics because this is for the internal API
        return accountInfoCache.getAccountInfo(cloudAccountInfo);
    }

    @Override
    public String getAccountInfo(Object sdkClient, CloudAccountInfo cloudAccountInfo) {
        // not recording metrics because this is for the internal API
        return accountInfoCache.getAccountInfo(sdkClient, cloudAccountInfo);
    }

}

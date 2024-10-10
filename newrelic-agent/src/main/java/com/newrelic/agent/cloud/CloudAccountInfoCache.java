/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.cloud;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.CloudAccountInfo;
import com.newrelic.api.agent.NewRelic;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * This class implements the account info methods from the Cloud API.
 */
public class CloudAccountInfoCache {
    private final LoadingCache<Object, Map<CloudAccountInfo, String>> cache;
    // this object is used to store data that is not related to a specific sdk client
    private static final Object NULL_CLIENT = new Object();

    CloudAccountInfoCache() {
        cache = Caffeine.newBuilder()
                .initialCapacity(4)
                .weakKeys()
                .executor(Runnable::run)
                .build((key) -> Collections.synchronizedMap(new EnumMap<>(CloudAccountInfo.class)));
    }

    public void setAccountInfo(CloudAccountInfo cloudAccountInfo, String value) {
        setAccountInfo(NULL_CLIENT, cloudAccountInfo, value);
    }

    public void setAccountInfo(Object sdkClient, CloudAccountInfo cloudAccountInfo, String value) {
        if (sdkClient == null) {
            return;
        }
        if (value == null) {
            Map<CloudAccountInfo, String> accountInfo = cache.getIfPresent(sdkClient);
            if (accountInfo != null) {
                accountInfo.remove(cloudAccountInfo);
            }
            return;
        }
        if (CloudAccountInfoValidator.validate(cloudAccountInfo, value)) {
            Map<CloudAccountInfo, String> accountInfo = cache.get(sdkClient);
            accountInfo.put(cloudAccountInfo, value);
        }
    }

    public String getAccountInfo(CloudAccountInfo cloudAccountInfo) {
        Map<CloudAccountInfo, String> accountInfo = cache.getIfPresent(NULL_CLIENT);
        if (accountInfo == null) {
            return null;
        }
        return accountInfo.get(cloudAccountInfo);
    }

    public String getAccountInfo(Object sdkClient, CloudAccountInfo cloudAccountInfo) {
        if (sdkClient == null) {
            return getAccountInfo(cloudAccountInfo);
        }
        Map<CloudAccountInfo, String> accountInfo = cache.getIfPresent(sdkClient);
        if (accountInfo == null) {
            return getAccountInfo(cloudAccountInfo);
        }
        return accountInfo.get(cloudAccountInfo);
    }

    void retrieveDataFromConfig() {
        AgentConfig agentConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();
        retrieveAwsAccountId(agentConfig);
    }

    private void retrieveAwsAccountId(AgentConfig agentConfig) {
        Object awsAccountId = agentConfig.getValue("cloud.aws.account_id");
        if (awsAccountId == null) {
            return;
        }

        NewRelic.getAgent().getLogger().log(Level.INFO, "Found AWS account ID configuration.");
        NewRelic.incrementCounter(MetricNames.SUPPORTABILITY_CONFIG_AWS_ACCOUNT_ID);
        setAccountInfo(CloudAccountInfo.AWS_ACCOUNT_ID, awsAccountId.toString());
    }
}

/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import com.newrelic.api.agent.Cloud;
import com.newrelic.api.agent.CloudAccountInfo;

/**
 * Internal Cloud API. This extends the public Cloud API and adds methods
 * for retrieving the data set by the public API methods.
 */
public interface CloudApi extends Cloud {

    /**
     * Return the general account information of the provided type.
     * This data is either set by {@link Cloud#setAccountInfo(CloudAccountInfo, String)}
     * or the agent config.
     */
    String getAccountInfo(CloudAccountInfo cloudAccountInfo);

    /**
     * Retrieves the account information for a cloud service SDK client.
     * If no data was recorded for the SDK client, the general account information will be returned.
     */
    String getAccountInfo(Object sdkClient, CloudAccountInfo cloudAccountInfo);
}

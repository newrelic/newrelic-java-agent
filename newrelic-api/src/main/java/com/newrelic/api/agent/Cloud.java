/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

/**
 * This interface defines methods to pass cloud services information to the agent.
 */
public interface Cloud {

    /**
     * <p>
     *     Sets the account information for a cloud service.
     * </p>
     * <p>
     *     This information is used by some instrumentation modules that cannot
     *     determine the resource id of the cloud service being invoked.
     * </p>
     * <p>
     *     The value provided to this method has priority over a value set in
     *     the agent configuration.
     * </p>
     * <p>
     *     Passing null as the value will remove the account information previously stored.
     * </p>
     * @param cloudAccountInfo the type of account information being stored
     * @param value the value to store
     */
    void setAccountInfo(CloudAccountInfo cloudAccountInfo, String value);


    /**
     * <p>
     *     Sets the account information for a cloud service SDK client.
     * </p>
     * <p>
     *     This information is used by some instrumentation modules that cannot
     *     determine the resource id of the cloud service being invoked.
     * </p>
     * <p>
     *     The value provided to this method has priority over a value set in
     *     the agent configuration or a value set using {@link #setAccountInfo(CloudAccountInfo, String)}.
     * </p>
     * <p>
     *     Passing null as the value will remove the account information previously stored.
     * </p>
     * @param sdkClient the SDK client object this account information is associated with
     * @param cloudAccountInfo the type of account information being stored
     * @param value the value to store
     */
    void setAccountInfo(Object sdkClient, CloudAccountInfo cloudAccountInfo, String value);
}

/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.utilization;

import java.util.Map;

interface CloudData {

    /**
     * get the attributes from the vendor
     */
    Map<String, String> getValueMap();

    /**
     * get vendor name
     */
    String getProvider();

    /**
     * test if the metadata object is the default empty instance
     */
    boolean isEmpty();

}

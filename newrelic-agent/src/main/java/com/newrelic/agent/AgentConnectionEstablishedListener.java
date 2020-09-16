/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import java.util.Map;

public interface AgentConnectionEstablishedListener {

    void onEstablished(String appName, String agentRunToken, Map<String, String> requestMetadata);

}

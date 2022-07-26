package com.nr.instrumentation.jakarta.ws.rs.api;/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.Interface, originalName = "jakarta.ws.rs.core.Request")
public class Request_Instrumentation {

    public String getMethod(){
        return Weaver.callOriginal();
    }
}

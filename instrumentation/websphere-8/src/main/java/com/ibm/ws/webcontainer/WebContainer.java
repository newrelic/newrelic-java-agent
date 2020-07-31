/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.ibm.ws.webcontainer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave
public class WebContainer {

    public static void notifyHttpServletResponseListenersPreHeaderCommit(HttpServletRequest request,
            HttpServletResponse response) {
        NewRelic.getAgent().getTransaction().addOutboundResponseHeaders();

        Weaver.callOriginal();
    }

}

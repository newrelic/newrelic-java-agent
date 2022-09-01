/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.ibm.ws.webcontainer;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Weave(type = MatchType.ExactClass, originalName = "com.ibm.ws.webcontainer.WebContainer")
public class WebContainer_Instrumentation {

    public static void notifyHttpServletResponseListenersPreHeaderCommit(HttpServletRequest request, HttpServletResponse response) {
        NewRelic.getAgent().getTransaction().addOutboundResponseHeaders();
        Weaver.callOriginal();
    }

}

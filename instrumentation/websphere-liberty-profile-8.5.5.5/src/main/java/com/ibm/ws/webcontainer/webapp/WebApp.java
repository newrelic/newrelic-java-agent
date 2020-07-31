/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.ibm.ws.webcontainer.webapp;

import java.util.logging.Level;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.servlet.error.ServletErrorReport;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.websphere.NRServletRequestListener;

@Weave
public abstract class WebApp implements ServletContext {
    protected WebAppEventSource eventSource;

    public void sendError(HttpServletRequest request, HttpServletResponse response, ServletErrorReport report) {
        NewRelic.noticeError(report);
        Weaver.callOriginal();
    }

    protected void registerGlobalWebAppListeners() {
        Weaver.callOriginal();

        AgentBridge.jmxApi.addJmxMBeanGroup("liberty");

        if (eventSource != null) {
            eventSource.addServletInvocationListener(new NRServletRequestListener());
            AgentBridge.getAgent().getLogger().log(Level.FINER,
                    "Registered WebSphere Liberty Profile servlet event listener for WebApp {0}.", this.getClass());
        } else {
            AgentBridge.getAgent().getLogger().log(Level.SEVERE,
                    "Failed to register WebSphere Liberty Profile servlet event listener for WebApp {0}.",
                    this.getClass());
        }
    }
}

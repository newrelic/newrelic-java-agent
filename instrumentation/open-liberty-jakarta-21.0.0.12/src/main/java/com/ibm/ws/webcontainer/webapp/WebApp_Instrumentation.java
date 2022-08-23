/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.ibm.ws.webcontainer.webapp;

import com.ibm.websphere.servlet.error.ServletErrorReport;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.websphere.NRServletRequestListener;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.logging.Level;

@Weave(type = MatchType.ExactClass, originalName = "com.ibm.ws.webcontainer.webapp.WebApp")
public abstract class WebApp_Instrumentation implements ServletContext {
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

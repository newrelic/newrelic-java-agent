/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.caucho.server.webapp;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.caucho.server.connection.CauchoRequest;
import com.caucho.server.connection.CauchoResponse;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave
public class ErrorPageManager {
    public void sendServletError(Throwable t, ServletRequest request, ServletResponse response) {
        AgentBridge.privateApi.reportException(t);
        Weaver.callOriginal();
    }

    public void sendError(CauchoRequest request, CauchoResponse response, int statusCode, String message) {
        AgentBridge.privateApi.reportHTTPError(message, statusCode, request.getRequestURI());
        Weaver.callOriginal();
    }
}

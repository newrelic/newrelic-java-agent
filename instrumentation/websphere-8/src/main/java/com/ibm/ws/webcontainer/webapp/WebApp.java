/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.ibm.ws.webcontainer.webapp;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.servlet.error.ServletErrorReport;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave
public abstract class WebApp implements IServletContext {

    protected static TraceNLS nls; // force weblogic-8 to fail loading in weblogic-7

    public void sendError(HttpServletRequest request, HttpServletResponse response, ServletErrorReport report) {
        NewRelic.noticeError(report);
        Weaver.callOriginal();
    }

}

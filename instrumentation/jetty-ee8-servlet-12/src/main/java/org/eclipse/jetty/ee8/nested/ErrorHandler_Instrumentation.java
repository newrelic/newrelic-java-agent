/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.jetty.ee8.nested;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jetty.ee8.servlet.ServerHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Weave(type = MatchType.ExactClass, originalName = "org.eclipse.jetty.ee8.nested.ErrorHandler")
public class ErrorHandler_Instrumentation {

    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
        final Throwable throwable = ServerHelper.getRequestError(request);

        // call the original implementation
        Weaver.callOriginal();

        if (throwable != null) {
            NewRelic.noticeError(throwable);
        }
    }


}

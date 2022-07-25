/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.jetty.server.handler;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.eclipse.jetty.server.Request;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Weave(type = MatchType.ExactClass, originalName = "org.eclipse.jetty.server.handler.ErrorHandler")
public abstract class ErrorHandler {
    @NewField
    private static final String EXCEPTION_ATTRIBUTE_NAME = "javax.servlet.error.exception";

    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
        final Throwable throwable = (Throwable) request.getAttribute(EXCEPTION_ATTRIBUTE_NAME);

        // call the original implementation
        Weaver.callOriginal();

        if (throwable != null) {
            NewRelic.noticeError(throwable);
        }
    }

}

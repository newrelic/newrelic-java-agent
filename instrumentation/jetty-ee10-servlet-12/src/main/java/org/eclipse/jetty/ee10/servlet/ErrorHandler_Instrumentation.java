/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.jetty.ee10.servlet;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

@Weave(type = MatchType.ExactClass, originalName = "org.eclipse.jetty.ee10.servlet.ErrorHandler")
public abstract class ErrorHandler_Instrumentation {
    @NewField
    private static final String EXCEPTION_ATTRIBUTE_NAME = "javax.servlet.error.exception";

    public boolean handle(Request request, Response response, Callback callback) {
        final Throwable throwable = (Throwable) request.getAttribute(EXCEPTION_ATTRIBUTE_NAME);

        // call the original implementation
        try {
            return Weaver.callOriginal();
        } finally {
            if (throwable != null) {
                NewRelic.noticeError(throwable);
            }
        }
    }

}

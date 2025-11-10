/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.jetty.ee10.servlet;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jetty12.ee10.servlet.ServerHelper;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

@Weave(type = MatchType.ExactClass, originalName = "org.eclipse.jetty.ee10.servlet.ErrorHandler")
public abstract class ErrorHandler_Instrumentation {
    @NewField
    private static final String EXCEPTION_ATTRIBUTE_NAME = "jakarta.servlet.error.exception";

    public boolean handle(Request request, Response response, Callback callback) {
        final Throwable throwable = ServerHelper.getRequestError(request);

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

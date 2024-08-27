/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.amazonaws.handlers;

import com.amazonaws.AmazonWebServiceRequest;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName ="com.amazonaws.handlers.AsyncHandler", type = MatchType.Interface)
public class AsyncHandler_Instrumentation<REQUEST extends AmazonWebServiceRequest, RESULT> {

    @NewField
    public Token token;

    @Trace(async = true)
    public void onError(Exception exception) {
        if (token != null) {
            token.linkAndExpire();
            token = null;
        }
        Weaver.callOriginal();
    }

    @Trace(async = true)
    public void onSuccess(REQUEST request, RESULT result) {
        if (token != null) {
            token.linkAndExpire();
            token = null;
        }
        Weaver.callOriginal();
    }
}
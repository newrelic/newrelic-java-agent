/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.asynchttpclient;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.Interface, originalName = "play.shaded.ahc.org.asynchttpclient.AsyncHandler")
public class AsyncHandler_Instrumentation<T> {

    @NewField
    public Token token;

    @Trace(async = true)
    public void onThrowable(Throwable t) {
        if (token != null) {
            token.linkAndExpire();
            token = null;
        }
        Weaver.callOriginal();
    }

    @Trace(async = true)
    public T onCompleted() throws Exception {
        if (token != null) {
            token.linkAndExpire();
            token = null;
        }
        return Weaver.callOriginal();
    }

}

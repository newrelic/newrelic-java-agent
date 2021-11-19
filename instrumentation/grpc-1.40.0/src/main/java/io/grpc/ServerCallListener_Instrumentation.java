/*
 *
 *  * Copyright 2021 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.grpc;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "io.grpc.ServerCall$Listener", type = MatchType.BaseClass)
public abstract class ServerCallListener_Instrumentation {

    @NewField
    public Token token;

    @Trace(async = true)
    public void onHalfClose() {
        // onHalfClose gets executed right before we enter customer code. This helps ensure that they will have a transaction available on the thread
        if (token != null) {
            token.linkAndExpire();
            this.token = null;
        }
        Weaver.callOriginal();
    }

}

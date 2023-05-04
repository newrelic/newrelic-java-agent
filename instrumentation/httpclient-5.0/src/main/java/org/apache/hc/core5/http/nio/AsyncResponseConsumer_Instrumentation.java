/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.hc.core5.http.nio;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type=MatchType.BaseClass, originalName = "org.apache.hc.core5.http.nio.AsyncResponseConsumer")
public class AsyncResponseConsumer_Instrumentation {

    @NewField
    public Token token;

    @Trace(async = true)
    public final void failed(final Exception cause) {
        if (token != null) {
            token.linkAndExpire();
            token = null;
        }

        Weaver.callOriginal();
    }
}

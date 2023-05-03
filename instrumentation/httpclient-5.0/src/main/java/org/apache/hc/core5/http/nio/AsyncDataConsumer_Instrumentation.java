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
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpException;

import java.io.IOException;
import java.util.List;

@Weave(type=MatchType.Interface, originalName = "org.apache.hc.core5.http.nio.AsyncDataConsumer")
public abstract class AsyncDataConsumer_Instrumentation {

    @NewField
    public Token token;

    @Trace(async = true)
    public final void streamEnd(final List<? extends Header> trailers) throws HttpException, IOException {
        if (token != null) {
            token.linkAndExpire();
            token = null;
        }

        Weaver.callOriginal();
    }

}

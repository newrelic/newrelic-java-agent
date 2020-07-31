/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.vertx.core.http.impl;

import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.vertx.instrumentation.VertxCoreUtil;
import io.vertx.core.MultiMap;

import java.net.UnknownHostException;

@Weave(originalName = "io.vertx.core.http.impl.HttpClientRequestBase")
abstract class HttpClientRequestBase_Instrumentation {

    @NewField
    public Segment segment;

    public abstract MultiMap headers();

    @Trace(async = true)
    public void handleException(Throwable t) {
        if (segment != null) {
            if (t instanceof UnknownHostException) {
                VertxCoreUtil.reportUnknownHost(segment);
            }
            final Token token = segment.getTransaction().getToken();
            segment.end();
            token.linkAndExpire();
        }
        Weaver.callOriginal();
    }

}

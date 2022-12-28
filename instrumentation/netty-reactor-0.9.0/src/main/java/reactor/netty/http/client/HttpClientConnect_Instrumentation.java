/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package reactor.netty.http.client;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import reactor.netty.Connection;
import reactor.util.context.Context;

@Weave(originalName = "reactor.netty.http.client.HttpClientConnect")
final class HttpClientConnect_Instrumentation {

    @Weave(originalName = "reactor.netty.http.client.HttpClientConnect$HttpObserver")
    static final class HttpObserver_Instrumentation {

        public Context currentContext() {
            return Weaver.callOriginal();
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        public void onUncaughtException(Connection connection, Throwable error) {
            Token token = currentContext().getOrDefault("newrelic-token", null);
            if (token != null && token.isActive()) {
                token.link();
            }
            Weaver.callOriginal();
        }
    }
}

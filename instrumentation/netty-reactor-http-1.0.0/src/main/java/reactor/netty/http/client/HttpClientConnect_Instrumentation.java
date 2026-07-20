/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package reactor.netty.http.client;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.ReactorNettyHelper;
import reactor.netty.Connection;
import reactor.netty.ConnectionObserver;
import reactor.util.context.Context;

@Weave(originalName = "reactor.netty.http.client.HttpClientConnect")
final class HttpClientConnect_Instrumentation {

    @Weave(originalName = "reactor.netty.http.client.HttpClientConnect$HttpIOHandlerObserver")
    static final class HttpIOHandlerObserver_Instrumentation {

        public Context currentContext() {
            return Weaver.callOriginal();
        }

        public void onStateChange(Connection connection, ConnectionObserver.State newState) {
            if (HttpClientState.REQUEST_PREPARED.equals(newState) && connection instanceof HttpClientRequest) {
                ReactorNettyHelper.handleRequestPrepared(connection, currentContext());
            } else if (HttpClientState.RESPONSE_RECEIVED.equals(newState) && connection instanceof HttpClientResponse) {
                ReactorNettyHelper.handleResponseReceived(connection);
            } else if (HttpClientState.RESPONSE_COMPLETED.equals(newState)
                    || ConnectionObserver.State.DISCONNECTING.equals(newState)
                    || ConnectionObserver.State.RELEASED.equals(newState)) {
                ReactorNettyHelper.cleanupOrphanedSegment(connection);
            }

            Weaver.callOriginal();
        }
    }

    @Weave(originalName = "reactor.netty.http.client.HttpClientConnect$HttpObserver")
    static final class HttpObserver_Instrumentation {

        public Context currentContext() {
            return Weaver.callOriginal();
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        public void onUncaughtException(Connection connection, Throwable throwable) {
            ReactorNettyHelper.cleanupOrphanedSegment(connection);
            Weaver.callOriginal();
        }
    }
}
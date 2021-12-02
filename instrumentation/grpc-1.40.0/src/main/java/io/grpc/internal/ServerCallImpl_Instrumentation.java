/*
 *
 *  * Copyright 2021 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.grpc.internal;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.grpc.ServerCallListener_Instrumentation;

@Weave(originalName = "io.grpc.internal.ServerCallImpl")
final class ServerCallImpl_Instrumentation {

    ServerStreamListener newServerStreamListener(ServerCallListener_Instrumentation listener) {
        // This is the point where a request comes into grpc
        // Store a token on the listener so we can bring the transaction into customer code
        listener.token = AgentBridge.getAgent().getTransaction().getToken();
        return Weaver.callOriginal();
    }
}

/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jetty10;

import com.newrelic.agent.bridge.AgentBridge;
import org.eclipse.jetty.server.HttpChannelState;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import java.io.IOException;

public final class AsyncListenerFactory {

    /*
     * The public enum Action was added in 9.1 and does not exist in 9.0.*. So declaring a variable of this type is
     * sufficient to prevent this instrumentation module from loading in 9.0.*.
     */
    public static HttpChannelState.Action isPostJetty90 = null;

    private AsyncListenerFactory() {
    }

    private static final AsyncListener ASYNC_LISTENER = new AsyncListener() {

        @Override
        public void onComplete(AsyncEvent asyncEvent) throws IOException {
            AgentBridge.asyncApi.completeAsync(asyncEvent.getAsyncContext());
        }

        @Override
        public void onTimeout(AsyncEvent asyncEvent) throws IOException {
            // do nothing
        }

        @Override
        public void onError(AsyncEvent asyncEvent) throws IOException {
            AgentBridge.asyncApi.errorAsync(asyncEvent.getAsyncContext(), asyncEvent.getThrowable());
        }

        @Override
        public void onStartAsync(AsyncEvent asyncEvent) throws IOException {
            // do nothing
        }

    };

    public static AsyncListener getAsyncListener() {
        return ASYNC_LISTENER;
    }
}

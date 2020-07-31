/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jboss7;

import java.io.IOException;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.weaver.CatchAndLog;

public final class AsyncListenerFactory {

    private AsyncListenerFactory() {
    }

    private static final AsyncListener ASYNC_LISTENER = new AsyncListener() {

        /**
         * JBoss calls {@link TomcatServletRequestListener#requestInitialized(javax.servlet.ServletRequestEvent)} before
         * calling {@link AsyncListener#onComplete(AsyncEvent)} so need to ignore the current transaction.
         */
        @Override
        @CatchAndLog
        public void onComplete(AsyncEvent asyncEvent) throws IOException {
            AgentBridge.asyncApi.completeAsync(asyncEvent.getAsyncContext());
            AgentBridge.getAgent().getTransaction().ignore();
        }

        @Override
        public void onTimeout(AsyncEvent asyncEvent) throws IOException {
            // do nothing
        }

        @Override
        @CatchAndLog
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

/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.glassfish6;

import java.io.IOException;

import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletRequest;

import org.apache.catalina.connector.Request_Instrumentation;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.CatchAndLog;

public final class AsyncListenerFactory {

    private AsyncListenerFactory() {
    }

    private static final AsyncListener ASYNC_LISTENER = new AsyncListener() {

        @Override
        @CatchAndLog
        public void onComplete(AsyncEvent asyncEvent) throws IOException {
            ServletRequest servletRequest = asyncEvent.getSuppliedRequest();
            Request_Instrumentation request = RequestFacadeHelper.getRequest(servletRequest);
            if (request != null) {
                AgentBridge.asyncApi.completeAsync(asyncEvent.getAsyncContext());
            }
        }

        @Override
        public void onTimeout(AsyncEvent asyncEvent) throws IOException {
            // do nothing
        }

        @Override
        @CatchAndLog
        public void onError(AsyncEvent asyncEvent) throws IOException {
            ServletRequest servletRequest = asyncEvent.getSuppliedRequest();
            Request_Instrumentation request = RequestFacadeHelper.getRequest(servletRequest);
            if (request != null) {
                AgentBridge.asyncApi.errorAsync(asyncEvent.getAsyncContext(), asyncEvent.getThrowable());
            }
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

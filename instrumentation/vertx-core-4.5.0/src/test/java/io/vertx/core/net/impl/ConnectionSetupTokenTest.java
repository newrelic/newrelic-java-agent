/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.vertx.core.net.impl;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.api.agent.Trace;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.spi.metrics.NetworkMetrics;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "io.vertx" })
public class ConnectionSetupTokenTest {

    @Test
    public void connectionBuiltDuringTransactionDoesNotHoldItOpen() {
        Vertx vertx = Vertx.vertx();
        EmbeddedChannel channel = buildConnectionInTransaction(vertx);
        try {
            // The connection and its close future are still open, but the transaction must already have finished.
            assertEquals(1, InstrumentationTestRunner.getIntrospector().getFinishedTransactionCount(2000));
        } finally {
            channel.close();
            vertx.close();
        }
    }

    @Trace(dispatcher = true)
    private EmbeddedChannel buildConnectionInTransaction(Vertx vertx) {
        ContextInternal context = (ContextInternal) vertx.getOrCreateContext();
        // Runs VertxHandler.handlerAdded on this thread, as an h2c-enabled server does while reading the first request
        // on a new connection.
        return new EmbeddedChannel(VertxHandler.create(chctx -> new TestConnection(context, chctx)));
    }

    private static class TestConnection extends ConnectionBase {

        TestConnection(ContextInternal context, ChannelHandlerContext chctx) {
            super(context, chctx);
        }

        @Override
        public NetworkMetrics metrics() {
            return null;
        }

        @Override
        protected void handleInterestedOpsChanged() {
        }
    }
}

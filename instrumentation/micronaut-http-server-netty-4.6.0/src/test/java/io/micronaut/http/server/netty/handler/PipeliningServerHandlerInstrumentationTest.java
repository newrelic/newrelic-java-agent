/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.micronaut.http.server.netty.handler;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.Weaver;
import io.netty.channel.ChannelHandlerContext_Instrumentation;
import io.netty.channel.ChannelPipeline_Instrumentation;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

public class PipeliningServerHandlerInstrumentationTest {

    @Test
    public void channelReadComplete_expiresTokenOnlyAfterCallingOriginal() {
        Token token = mock(Token.class);
        ChannelPipeline_Instrumentation pipeline = new ChannelPipeline_Instrumentation();
        pipeline.micronautToken = token;

        ChannelHandlerContext_Instrumentation ctx = new ChannelHandlerContext_Instrumentation() {
            @Override
            public ChannelPipeline_Instrumentation pipeline() {
                return pipeline;
            }
        };

        boolean[] tokenStillLiveDuringCallOriginal = new boolean[1];
        try (MockedStatic<Weaver> weaver = mockStatic(Weaver.class)) {
            weaver.when(Weaver::callOriginal).thenAnswer(invocation -> {
                tokenStillLiveDuringCallOriginal[0] = pipeline.micronautToken != null;
                return null;
            });

            new PipeliningServerHandler_Instrumentation() {}.channelReadComplete(ctx);
        }

        assertTrue("the token must still be linked while the original method's synchronous dispatch runs",
                tokenStillLiveDuringCallOriginal[0]);
        verify(token).expire();
        assertNull("the token must be released once the read is complete", pipeline.micronautToken);
    }

    @Test
    public void channelReadComplete_noToken_doesNothing() {
        ChannelPipeline_Instrumentation pipeline = new ChannelPipeline_Instrumentation();
        ChannelHandlerContext_Instrumentation ctx = new ChannelHandlerContext_Instrumentation() {
            @Override
            public ChannelPipeline_Instrumentation pipeline() {
                return pipeline;
            }
        };

        try (MockedStatic<Weaver> weaver = mockStatic(Weaver.class)) {
            new PipeliningServerHandler_Instrumentation() {}.channelReadComplete(ctx);
        }

        assertNull(pipeline.micronautToken);
    }

    @Test
    public void channelReadComplete_nullContext_doesNotThrow() {
        try (MockedStatic<Weaver> weaver = mockStatic(Weaver.class)) {
            new PipeliningServerHandler_Instrumentation() {}.channelReadComplete(null);
        }
    }
}

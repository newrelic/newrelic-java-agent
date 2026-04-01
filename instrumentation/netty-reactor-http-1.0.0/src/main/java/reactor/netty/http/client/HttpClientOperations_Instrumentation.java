/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package reactor.netty.http.client;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.NRInstrumentationContext;
import reactor.util.context.ContextView;

import java.util.logging.Level;

@Weave(type = MatchType.BaseClass, originalName = "reactor.netty.http.client.HttpClientOperations")
abstract class HttpClientOperations_Instrumentation {

    @NewField
    public NRInstrumentationContext nrContext;

    public abstract ContextView currentContextView();

    @Trace(async=true)
    protected void beforeMarkSentHeaders() {
        Token token = null;

        ContextView ctx = currentContextView();
        if (ctx != null) {
            token = ctx.getOrDefault("newrelic-token", null);
            if (token != null && token.isActive()) {
                token.link();
            }
        }

        if (nrContext == null) {
            nrContext = new NRInstrumentationContext(token);
        }

        nrContext.startSegment((HttpClientRequest) this);

        Weaver.callOriginal();
    }

    // TODO not entirely sure this is necessary, never seen it go through here
    @Trace(async=true)
    protected void afterInboundComplete() {
        if (nrContext != null) {
            nrContext.endSegment((HttpClientResponse) this, null);
        }

        Weaver.callOriginal();
        nrContext = null;
    }

    // TODO how do I handle UnknownHost exceptions?
    // we have several options, but none seem to actually work:
    // protected void onInboundClose() {                                                        // never called
    // protected void onOutboundError(Throwable err) {                                          // never called
    // protected void onInboundNext(ChannelHandlerContext ctx, Object msg) {                    // gets called, but no exception to grab
    // public final HttpClientOperations onDispose(Disposable onDispose) {                      // never called
    // static Throwable addOutboundErrorCause(Throwable exception, @Nullable Throwable cause) { // never called
}
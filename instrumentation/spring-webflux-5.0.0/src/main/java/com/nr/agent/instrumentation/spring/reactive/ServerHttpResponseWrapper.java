/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.spring.reactive;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.Weaver;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

public class ServerHttpResponseWrapper implements ServerHttpResponse {

    private final ServerHttpResponse delegate;
    private final Token token;

    public ServerHttpResponseWrapper(Token token, ServerHttpResponse response) {
        this.token = token;
        this.delegate = response;
    }

    public boolean setStatusCode(HttpStatus status) {
        return delegate.setStatusCode(status);
    }

    @Nullable
    public HttpStatus getStatusCode() {
        return delegate.getStatusCode();
    }

    public MultiValueMap<String, ResponseCookie> getCookies() {
        return delegate.getCookies();
    }

    public void addCookie(ResponseCookie cookie) {
        delegate.addCookie(cookie);
    }

    public DataBufferFactory bufferFactory() {
        return delegate.bufferFactory();
    }

    public void beforeCommit(Supplier<? extends Mono<Void>> action) {
        delegate.beforeCommit(action);
    }

    public boolean isCommitted() {
        return delegate.isCommitted();
    }

    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
        try {
            if (this.token != null) {
                this.token.expire();
            }
        } catch (Throwable t) {
            AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
        }

        return delegate.writeWith(body);
    }

    public Mono<Void> writeAndFlushWith(
            Publisher<? extends Publisher<? extends DataBuffer>> body) {
        try {
            if (this.token != null) {
                this.token.expire();
            }
        } catch (Throwable t) {
            AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
        }
        return delegate.writeAndFlushWith(body);
    }

    public Mono<Void> setComplete() {
        try {
            if (this.token != null) {
                this.token.expire();
            }
        } catch (Throwable t) {
            AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
        }
        return delegate.setComplete();
    }

    public HttpHeaders getHeaders() {
        return delegate.getHeaders();
    }

}

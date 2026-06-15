/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.modelcontextprotocol.client;

import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * A no-op transport used to satisfy McpAsyncClient's constructor in tests.
 * sendMessage returns an immediate error so that initialization fails fast
 * instead of waiting for a timeout.
 */
class NoOpMcpTransport implements McpClientTransport {

    @Override
    public Mono<Void> connect(Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> handler) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
        return Mono.error(new RuntimeException("NoOpMcpTransport: no transport configured"));
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Mono.empty();
    }

    @Override
    public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
        return null;
    }
}
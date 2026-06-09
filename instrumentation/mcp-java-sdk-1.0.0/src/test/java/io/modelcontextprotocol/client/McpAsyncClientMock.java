/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.modelcontextprotocol.client;

import io.modelcontextprotocol.spec.McpSchema;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Test stub for McpAsyncClient.
 * <p>
 *   Does NOT override callTool, readResource, or getPrompt.
 *   Overriding would cause virtual dispatch to skip the woven McpAsyncClient bytecode entirely,
 *   producing no segments. McpClientFeatures is package-private, so this class must live here.
 */
class McpAsyncClientMock extends McpAsyncClient {

    private static final McpClientFeatures.Async FEATURES = new McpClientFeatures.Async(
            new McpSchema.Implementation("test", "1.0"),
            null, Map.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(), null, null, false
    );

    McpAsyncClientMock() {
        super(new NoOpMcpTransport(), Duration.ofSeconds(5), Duration.ofSeconds(5), null, FEATURES);
    }
}
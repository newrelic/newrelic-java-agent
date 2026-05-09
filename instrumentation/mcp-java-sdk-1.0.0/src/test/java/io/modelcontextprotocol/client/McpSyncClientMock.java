/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.modelcontextprotocol.client;

import io.modelcontextprotocol.common.McpTransportContext;

/**
 * Test stub for McpSyncClient.
 * <p>
 *   Does NOT override callTool, readResource, or getPrompt.
 *   Overriding would cause virtual dispatch to skip the woven McpSyncClient bytecode entirely,
 *   producing no segments. Callers must wrap invocations in try-catch, the NoOpMcpTransport
 *   always errors, causing .block() to throw.
 *   Transport behavior comes from the McpAsyncClientMock delegate.
 */
public class McpSyncClientMock extends McpSyncClient {

    public McpSyncClientMock() {
        super(new McpAsyncClientMock(), () -> McpTransportContext.EMPTY);
    }
}
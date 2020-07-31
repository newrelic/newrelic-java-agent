/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport;

import org.json.simple.JSONObject;

import static org.junit.Assert.assertEquals;

/**
 * To use this, the startupOptions should contain "test-value":"test-sentinel"
 * and the connect response should contain "other":"value".
 */
class ConnectCycleNoSecuritySuccessClientWrapper extends ConnectCycleSuccessBaseClientWrapper {
    @Override
    protected ReadResult mapPreconnectRequest(Request request, JSONObject postedData, String newHost) {
        assertEquals("no-collector.example.com", request.getURL().getHost());
        assertEquals(false, postedData.get("high_security"));
        return ReadResult.create(
                HttpResponseCode.ACCEPTED,
                "{\"return_value\":{\"redirect_host\":\"" + newHost + "\"}}",
                null);
    }

    @Override
    protected ReadResult mapConnectRequest(Request request, JSONObject firstParameter) {
        assertEquals("new_host.example.com", request.getURL().getHost());
        assertEquals("test-value", firstParameter.get("test-sentinel"));
        return ReadResult.create(
                HttpResponseCode.ACCEPTED,
                "{\"return_value\":{\"agent_run_id\":\"my-run-id\",\"ssl\":true,\"other\":\"value\"}}",
                null);
    }
}

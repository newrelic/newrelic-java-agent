/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport;

import org.json.simple.JSONObject;

import static org.junit.Assert.assertEquals;

public class ConnectCycleLaspSuccessClientWrapper extends ConnectCycleSuccessBaseClientWrapper {
    @Override
    protected ReadResult mapPreconnectRequest(Request request, JSONObject postedData, String newHost) {
        assertEquals("no-collector.example.com", request.getURL().getHost());
        assertEquals(false, postedData.get("high_security"));
        assertEquals("ffff-ffff-ffff-ffff", postedData.get("security_policies_token"));
        return ReadResult.create(
                HttpResponseCode.ACCEPTED,
                "{\"return_value\":{\"redirect_host\":\"" + newHost + "\","
                        + "\"security_policies\":{"
                        + "\"record_sql\":{\"enabled\":true,\"required\":true},"
                        + "\"attributes_include\":{\"enabled\":false,\"required\":true},"
                        + "\"allow_raw_exception_messages\":{\"enabled\":true,\"required\":true},"
                        + "\"custom_events\":{\"enabled\":true,\"required\":true},"
                        + "\"custom_parameters\":{\"enabled\":false,\"required\":true},"
                        + "\"custom_instrumentation_editor\":{\"enabled\":true,\"required\":true},"
                        + "\"message_parameters\":{\"enabled\":true,\"required\":true},"
                        + "\"job_arguments\":{\"enabled\":true,\"required\":false}"
                        + "}"
                        + "}}",
                null);
    }

    @Override
    protected ReadResult mapConnectRequest(Request request, JSONObject firstParameter) {
        assertEquals("new_host.example.com", request.getURL().getHost());
        assertEquals("test-value", firstParameter.get("test-sentinel"));
        JSONObject securityPolicies = (JSONObject)firstParameter.get("security_policies");
        verifySecurityPolicyEnabled(securityPolicies, "record_sql");
        verifySecurityPolicyEnabled(securityPolicies, "allow_raw_exception_messages");
        verifySecurityPolicyEnabled(securityPolicies, "custom_events");
        verifySecurityPolicyEnabled(securityPolicies, "custom_instrumentation_editor");
        verifySecurityPolicyEnabled(securityPolicies, "message_parameters");

        verifySecurityPolicyDisabled(securityPolicies, "attributes_include");
        verifySecurityPolicyDisabled(securityPolicies, "custom_parameters");

        assertEquals(7, securityPolicies.size());

        return ReadResult.create(
                HttpResponseCode.ACCEPTED,
                "{\"return_value\":{\"agent_run_id\":\"my-run-id\",\"ssl\":true,\"other\":\"value\"}}",
                null);
    }

    private void verifySecurityPolicyEnabled(JSONObject securityPolicies, String key) {
        assertEquals(true, (((JSONObject)securityPolicies.get(key)).get("enabled")));
    }

    private void verifySecurityPolicyDisabled(JSONObject securityPolicies, String key) {
        assertEquals(false, (((JSONObject)securityPolicies.get(key)).get("enabled")));
    }
}

/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.otlp.audit;

import com.newrelic.api.agent.NewRelic;
import io.opentelemetry.sdk.common.export.HttpResponse;

import java.util.Arrays;
import java.util.Base64;
import java.util.logging.Level;

/**
 * Static helpers for OTLP audit response logging in the exporter transport layer.
 */
public final class OtlpAuditLogger {
    private static final String AUDIT_MODE = "audit_mode";
    private static final Boolean AUDIT_MODE_DEFAULT = false;

    private OtlpAuditLogger() {
    }

    public static boolean isAuditModeEnabled() {
        return NewRelic.getAgent().getConfig().getValue(AUDIT_MODE, AUDIT_MODE_DEFAULT);
    }

    public static void logAuditResponse(HttpResponse response) {
        try {
            int statusCode = response.getStatusCode();
            byte[] body = response.getResponseBody();
            if (body == null) {
                body = new byte[0];
            }
            String payload = Base64.getEncoder().encodeToString(body);
            int bytes = body.length;
            NewRelic.getAgent()
                    .getLogger()
                    .log(Level.INFO,
                            "Received OTLP/Metrics response: status={0}, bytes={1}, payload: {2}",
                            statusCode,
                            bytes,
                            payload);
        } catch (Exception ex) {
            NewRelic.getAgent()
                    .getLogger()
                    .log(Level.INFO, "Error logging OTLP/Metrics response: \n" + Arrays.toString(ex.getStackTrace()));
        }
    }
}

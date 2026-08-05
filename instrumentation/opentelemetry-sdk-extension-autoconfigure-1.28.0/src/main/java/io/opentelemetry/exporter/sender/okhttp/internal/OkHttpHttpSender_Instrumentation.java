/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.exporter.sender.okhttp.internal;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.utils.audit.OtlpAuditLogger;

import java.util.function.Consumer;

/**
 * Weaves OkHttpHttpSender to add audit mode logging for OTLP metric exports.
 *
 * <p>When audit_mode is enabled, wraps the HTTP response callback to log the status code and
 * response body (base64-encoded protobuf) at INFO level. This fires once per HTTP attempt
 * (including retries by the OTel SDK's RetryPolicy).
 */
@Weave(type = MatchType.ExactClass, originalName = "io.opentelemetry.exporter.sender.okhttp.internal.OkHttpHttpSender")
public abstract class OkHttpHttpSender_Instrumentation {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void send(Consumer marshaler, int contentLength, Consumer onResponse, Consumer onError) {
        if (OtlpAuditLogger.isAuditModeEnabled()) {
            final Consumer originalOnResponse = onResponse;
            onResponse =
                    new Consumer() {
                        @Override
                        public void accept(Object response) {
                            OtlpAuditLogger.logAuditResponse(response);
                            originalOnResponse.accept(response);
                        }
                    };
        }
        Weaver.callOriginal();
    }
}

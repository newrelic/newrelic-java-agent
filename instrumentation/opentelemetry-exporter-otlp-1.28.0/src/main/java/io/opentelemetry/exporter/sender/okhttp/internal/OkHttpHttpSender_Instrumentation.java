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
import com.nr.agent.instrumentation.otlp.audit.OtlpAuditLogger;
import io.opentelemetry.exporter.internal.http.HttpSender;

import java.io.OutputStream;
import java.util.function.Consumer;

/**
 * Instruments OkHttpHttpSender to log OTLP response bodies at INFO level when audit_mode is enabled.
 */
@Weave(type = MatchType.ExactClass, originalName = "io.opentelemetry.exporter.sender.okhttp.internal.OkHttpHttpSender")
public abstract class OkHttpHttpSender_Instrumentation {

    public void send(
            Consumer<OutputStream> marshaler,
            int contentLength,
            Consumer<HttpSender.Response> onResponse,
            Consumer<Throwable> onError) {
        if (OtlpAuditLogger.isAuditModeEnabled()) {
            final Consumer<HttpSender.Response> originalOnResponse = onResponse;
            onResponse = new Consumer<HttpSender.Response>() {
                @Override
                public void accept(HttpSender.Response response) {
                    OtlpAuditLogger.logAuditResponse(response);
                    originalOnResponse.accept(response);
                }
            };
        }
        Weaver.callOriginal();
    }
}

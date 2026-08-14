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
import io.opentelemetry.sdk.common.export.HttpResponse;
import io.opentelemetry.sdk.common.export.MessageWriter;

import java.util.function.Consumer;

/**
 * Instruments OkHttpHttpSender (opentelemetry-exporter-otlp 1.59.0+) to log OTLP
 * response bodies at INFO level when audit_mode is enabled.
 */
@Weave(type = MatchType.ExactClass, originalName = "io.opentelemetry.exporter.sender.okhttp.internal.OkHttpHttpSender")
public abstract class OkHttpHttpSender_Instrumentation {

    public void send(
            MessageWriter messageWriter,
            Consumer<HttpResponse> onResponse,
            Consumer<Throwable> onError) {
        if (OtlpAuditLogger.isAuditModeEnabled()) {
            final Consumer<HttpResponse> originalOnResponse = onResponse;
            onResponse = new Consumer<HttpResponse>() {
                @Override
                public void accept(HttpResponse response) {
                    OtlpAuditLogger.logAuditResponse(response);
                    originalOnResponse.accept(response);
                }
            };
        }
        Weaver.callOriginal();
    }
}

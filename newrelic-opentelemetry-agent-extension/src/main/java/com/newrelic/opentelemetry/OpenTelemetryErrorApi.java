/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.opentelemetry;

import com.newrelic.api.agent.ErrorApi;
import com.newrelic.api.agent.ErrorGroupCallback;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;

import java.util.Map;

import static com.newrelic.opentelemetry.OpenTelemetryNewRelic.logUnsupportedMethod;

/**
 * Note class is public because it is accessed from package
 * {@code com.newrelic.api.agent} after {@link com.newrelic.api.agent.NewRelic}
 * is rewritten.
 */
public final class OpenTelemetryErrorApi implements ErrorApi {

    private static final OpenTelemetryErrorApi INSTANCE = new OpenTelemetryErrorApi();

    private static final AttributeKey<Boolean> NEWRELIC_EXPECTED_ERROR_ATTRIBUTE_KEY = AttributeKey.booleanKey("expected.error");
    private static final Attributes EXPECTED_ERROR_ATTRIBUTES = Attributes.of(NEWRELIC_EXPECTED_ERROR_ATTRIBUTE_KEY, true);
    private static final Attributes UNEXPECTED_ERROR_ATTRIBUTES = Attributes.of(NEWRELIC_EXPECTED_ERROR_ATTRIBUTE_KEY, false);

    private OpenTelemetryErrorApi() {
    }

    static OpenTelemetryErrorApi getInstance() {
        return INSTANCE;
    }

    @Override
    public void noticeError(Throwable throwable, Map<String, ?> params) {
        noticeError(throwable, params, false);
    }

    @Override
    public void noticeError(Throwable throwable) {
        noticeError(throwable, false);
    }

    @Override
    public void noticeError(String message, Map<String, ?> params) {
        noticeError(new ReportedError(message), params);
    }

    @Override
    public void noticeError(String message) {
        noticeError(new ReportedError(message));
    }

    @Override
    public void noticeError(Throwable throwable, Map<String, ?> params, boolean expected) {
        Attributes attributes = OpenTelemetryNewRelic.toAttributes(params).putAll(expected ? EXPECTED_ERROR_ATTRIBUTES : UNEXPECTED_ERROR_ATTRIBUTES).build();
        Span.current().recordException(throwable, attributes);
        Span.current().setStatus(StatusCode.ERROR);
    }

    @Override
    public void noticeError(Throwable throwable, boolean expected) {
        Span.current().recordException(throwable, expected ? EXPECTED_ERROR_ATTRIBUTES : UNEXPECTED_ERROR_ATTRIBUTES);
        Span.current().setStatus(StatusCode.ERROR);
    }

    @Override
    public void noticeError(String message, Map<String, ?> params, boolean expected) {
        noticeError(new ReportedError(message), params, expected);
    }

    @Override
    public void noticeError(String message, boolean expected) {
        noticeError(new ReportedError(message), expected);
    }

    @Override
    public void setErrorGroupCallback(ErrorGroupCallback errorGroupCallback) {
        logUnsupportedMethod("ErrorApi", "setErrorGroupCallback");
    }

    private static class ReportedError extends Exception {
        private ReportedError(String message) {
            super(message);
        }
    }
}

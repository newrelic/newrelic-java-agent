package com.newrelic.opentelemetry;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;

import java.util.Map;

/**
 * Note class is public because it is accessed from package
 * {@code com.newrelic.api.agent} after {@link com.newrelic.api.agent.NewRelic}
 * is rewritten.
 */
public final class OpenTelemetryErrorApi {

    private static final OpenTelemetryErrorApi INSTANCE = new OpenTelemetryErrorApi();

    private static final AttributeKey<Boolean> NEWRELIC_EXPECTED_ERROR_ATTRIBUTE_KEY = AttributeKey.booleanKey("expected.error");
    private static final Attributes EXPECTED_ERROR_ATTRIBUTES = Attributes.of(NEWRELIC_EXPECTED_ERROR_ATTRIBUTE_KEY, true);
    private static final Attributes UNEXPECTED_ERROR_ATTRIBUTES = Attributes.of(NEWRELIC_EXPECTED_ERROR_ATTRIBUTE_KEY, false);

    private OpenTelemetryErrorApi() {
    }

    static OpenTelemetryErrorApi getInstance() {
        return INSTANCE;
    }

    public void noticeError(Throwable throwable, Map<String, ?> params) {
        noticeError(throwable, params, false);
    }

    public void noticeError(Throwable throwable) {
        noticeError(throwable, false);
    }

    public void noticeError(String message, Map<String, ?> params) {
        noticeError(new ReportedError(message), params);
    }

    public void noticeError(String message) {
        noticeError(new ReportedError(message));
    }

    public void noticeError(Throwable throwable, Map<String, ?> params, boolean expected) {
        Attributes attributes = OpenTelemetryNewRelic.toAttributes(params).putAll(expected ? EXPECTED_ERROR_ATTRIBUTES : UNEXPECTED_ERROR_ATTRIBUTES).build();
        Span.current().recordException(throwable, attributes);
        Span.current().setStatus(StatusCode.ERROR);
    }

    public void noticeError(Throwable throwable, boolean expected) {
        Span.current().recordException(throwable, expected ? EXPECTED_ERROR_ATTRIBUTES : UNEXPECTED_ERROR_ATTRIBUTES);
        Span.current().setStatus(StatusCode.ERROR);
    }

    public void noticeError(String message, Map<String, ?> params, boolean expected) {
        noticeError(new ReportedError(message), params, expected);
    }

    public void noticeError(String message, boolean expected) {
        noticeError(new ReportedError(message), expected);
    }

    private static class ReportedError extends Exception {
        private ReportedError(String message) {
            super(message);
        }
    }
}

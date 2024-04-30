package io.opentelemetry.sdk.autoconfigure;

import com.newrelic.agent.security.deps.com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributeType;
import io.opentelemetry.sdk.trace.ReadableSpan;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Predicate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SpanReader {
    private SpanReader() {}

    public static ReadableSpan readSpan(InputStream inputStream) throws IOException {
        return readSpan(inputStream, attribute -> true);
    }

    public static ReadableSpan readSpan(InputStream inputStream, Predicate<String> attributeFilter) throws IOException {
        final Map<String, Object> spanData = new ObjectMapper().readValue(
                inputStream, Map.class);
        ReadableSpan span = mock(ReadableSpan.class);
        when(span.getAttribute(any(AttributeKey.class))).thenAnswer(invocation -> {
            AttributeKey key = invocation.getArgument(0);

            if (!attributeFilter.test(key.getKey())) {
                return null;
            }
            Object value = spanData.get(key.getKey());
            if (value != null) {
                if (key.getType() == AttributeType.LONG && value instanceof Number) {
                    return ((Number) value).longValue();
                }
            }
            return value;
        });
        return span;
    }
}

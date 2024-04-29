package io.opentelemetry.sdk.autoconfigure;

import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.security.deps.com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.GenericParameters;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributeType;
import io.opentelemetry.sdk.trace.ReadableSpan;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SpanToTracerProcessorTest {
    @Test
    public void testReportDatabaseClientSpan() throws Exception {
        ReadableSpan span = readableSpan("java-db-span.json");
        ExitTracer tracer = mock(ExitTracer.class);
        SpanToTracerProcessor.reportClientSpan(span, tracer);
        final ArgumentCaptor<DatastoreParameters> dbParams = ArgumentCaptor.forClass(DatastoreParameters.class);
        verify(tracer, times(1)).reportAsExternal(dbParams.capture());
        assertEquals("mysql", dbParams.getValue().getProduct());
        assertEquals("owners", dbParams.getValue().getCollection());
        assertEquals("mysqlserver", dbParams.getValue().getHost());
        assertEquals(3306, dbParams.getValue().getPort().intValue());
        assertEquals("SELECT", dbParams.getValue().getOperation());
        assertEquals("petclinic", dbParams.getValue().getDatabaseName());
    }

    @Test
    public void testReportRpcClientSpan() throws Exception {
        ReadableSpan span = readableSpan("java-external-rpc-span.json");
        ExitTracer tracer = mock(ExitTracer.class);
        SpanToTracerProcessor.reportClientSpan(span, tracer);
        final ArgumentCaptor<GenericParameters> externalParams = ArgumentCaptor.forClass(GenericParameters.class);
        verify(tracer, times(1)).reportAsExternal(externalParams.capture());
        assertEquals("io.opentelemetry.grpc-1.6", externalParams.getValue().getLibrary());
        assertEquals("ResolveBoolean", externalParams.getValue().getProcedure());
        assertEquals("http://opentelemetry-demo-flagd:8013", externalParams.getValue().getUri().toString());
    }

    @Test
    public void testReportHttpClientSpan() throws Exception {
        ReadableSpan span = readableSpan("java-external-http-span.json");
        ExitTracer tracer = mock(ExitTracer.class);
        SpanToTracerProcessor.reportClientSpan(span, tracer);
        final ArgumentCaptor<GenericParameters> externalParams = ArgumentCaptor.forClass(GenericParameters.class);
        verify(tracer, times(1)).reportAsExternal(externalParams.capture());
        assertEquals("io.opentelemetry.java-http-client", externalParams.getValue().getLibrary());
        assertEquals("https://google.com", externalParams.getValue().getUri().toString());
        assertEquals("GET", externalParams.getValue().getProcedure());
    }

    static ReadableSpan readableSpan(String fileName) throws IOException {
        final Map<String, Object> spanData = new ObjectMapper().readValue(
                SpanToTracerProcessorTest.class.getResource(fileName), Map.class);
        ReadableSpan span = mock(ReadableSpan.class);
        when(span.getAttribute(any(AttributeKey.class))).thenAnswer(invocation -> {
            AttributeKey key = invocation.getArgument(0);

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
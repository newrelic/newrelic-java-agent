package io.opentelemetry.sdk.trace;

import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.security.deps.com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.GenericParameters;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ExitTracerSpanTest {
    @Test
    public void testReportDatabaseClientSpan() throws Exception {
        ExitTracer tracer = mock(ExitTracer.class);
        new ExitTracerSpan(tracer, SpanKind.CLIENT, readSpanAttributes("db-span.json")).end();
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
    public void testReportDatabaseClientSpanMissingSqlTable() throws Exception {
        ExitTracer tracer = mock(ExitTracer.class);
        Map<String, Object> attributes = readSpanAttributes("db-span.json");
        attributes.remove("db.sql.table");
        new ExitTracerSpan(tracer, SpanKind.CLIENT, attributes).end();
        final ArgumentCaptor<DatastoreParameters> dbParams = ArgumentCaptor.forClass(DatastoreParameters.class);
        verify(tracer, times(1)).reportAsExternal(dbParams.capture());
        assertEquals("mysql", dbParams.getValue().getProduct());
        assertNull(dbParams.getValue().getCollection());
        assertEquals("mysqlserver", dbParams.getValue().getHost());
        assertEquals(3306, dbParams.getValue().getPort().intValue());
        assertEquals("SELECT", dbParams.getValue().getOperation());
        assertEquals("petclinic", dbParams.getValue().getDatabaseName());
    }

    @Test
    public void testReportRpcClientSpan() throws Exception {
        ExitTracer tracer = mock(ExitTracer.class);
        new ExitTracerSpan(tracer, SpanKind.CLIENT, readSpanAttributes("external-rpc-span.json")).end();
        final ArgumentCaptor<GenericParameters> externalParams = ArgumentCaptor.forClass(GenericParameters.class);
        verify(tracer, times(1)).reportAsExternal(externalParams.capture());
        assertEquals("io.opentelemetry.grpc-1.6", externalParams.getValue().getLibrary());
        assertEquals("ResolveBoolean", externalParams.getValue().getProcedure());
        assertEquals("http://opentelemetry-demo-flagd:8013", externalParams.getValue().getUri().toString());
    }

    @Test
    public void testReportHttpClientSpan() throws Exception {
        ExitTracer tracer = mock(ExitTracer.class);
        new ExitTracerSpan(tracer, SpanKind.CLIENT, readSpanAttributes("external-http-span.json")).end();
        final ArgumentCaptor<GenericParameters> externalParams = ArgumentCaptor.forClass(GenericParameters.class);
        verify(tracer, times(1)).reportAsExternal(externalParams.capture());
        assertEquals("io.opentelemetry.java-http-client", externalParams.getValue().getLibrary());
        assertEquals("https://google.com", externalParams.getValue().getUri().toString());
        assertEquals("GET", externalParams.getValue().getProcedure());
    }

    @Test
    public void testReportHttpClientSpanWithCodeFunction() throws Exception {
        ExitTracer tracer = mock(ExitTracer.class);
        new ExitTracerSpan(tracer, SpanKind.CLIENT, readSpanAttributes("external-http-span.json"))
                .setAttribute(AttributeKey.stringKey("code.function"), "execute").end();
        final ArgumentCaptor<GenericParameters> externalParams = ArgumentCaptor.forClass(GenericParameters.class);
        verify(tracer, times(1)).reportAsExternal(externalParams.capture());
        assertEquals("io.opentelemetry.java-http-client", externalParams.getValue().getLibrary());
        assertEquals("https://google.com", externalParams.getValue().getUri().toString());
        assertEquals("execute", externalParams.getValue().getProcedure());
    }

    @Test
    public void testBadClientSpan() throws Exception {
        ExitTracer tracer = mock(ExitTracer.class);
        new ExitTracerSpan(tracer, SpanKind.CLIENT, readSpanAttributes("bad-client-span.json")).end();
        verify(tracer, times(0)).reportAsExternal(any(ExternalParameters.class));
    }

    static Map<String, Object> readSpanAttributes(String fileName) throws IOException {
        try (InputStream in = ExitTracerSpanTest.class.getResourceAsStream(fileName)) {
            return new ObjectMapper().readValue(in, Map.class);
        }
    }
}
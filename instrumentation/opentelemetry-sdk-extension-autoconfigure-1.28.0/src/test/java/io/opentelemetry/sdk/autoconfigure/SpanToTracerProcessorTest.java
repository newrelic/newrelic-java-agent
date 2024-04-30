package io.opentelemetry.sdk.autoconfigure;

import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.GenericParameters;
import io.opentelemetry.sdk.trace.ReadableSpan;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SpanToTracerProcessorTest {
    @Test
    public void testReportDatabaseClientSpan() throws Exception {
        ReadableSpan span = readSpan("db-span.json");
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
    public void testReportDatabaseClientSpanMissingSqlTable() throws Exception {
        ReadableSpan span = SpanReader.readSpan(SpanToTracerProcessor.class.getResourceAsStream("db-span.json"),
                attribute -> !"db.sql.table".equals(attribute));
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
        ReadableSpan span = readSpan("external-rpc-span.json");
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
        ReadableSpan span = readSpan("external-http-span.json");
        ExitTracer tracer = mock(ExitTracer.class);
        SpanToTracerProcessor.reportClientSpan(span, tracer);
        final ArgumentCaptor<GenericParameters> externalParams = ArgumentCaptor.forClass(GenericParameters.class);
        verify(tracer, times(1)).reportAsExternal(externalParams.capture());
        assertEquals("io.opentelemetry.java-http-client", externalParams.getValue().getLibrary());
        assertEquals("https://google.com", externalParams.getValue().getUri().toString());
        assertEquals("GET", externalParams.getValue().getProcedure());
    }

    @Test
    public void testBadClientSpan() throws Exception {
        ReadableSpan span = readSpan("bad-client-span.json");
        ExitTracer tracer = mock(ExitTracer.class);
        SpanToTracerProcessor.reportClientSpan(span, tracer);
        verify(tracer, times(0)).reportAsExternal(any(ExternalParameters.class));
    }

    static ReadableSpan readSpan(String fileName) throws IOException {
        try (InputStream in = SpanToTracerProcessorTest.class.getResourceAsStream(fileName)) {
            return SpanReader.readSpan(in);
        }
    }
}
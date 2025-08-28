/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.opentelemetry.sdk.trace;

import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.security.deps.com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.HttpParameters;
import com.nr.agent.instrumentation.utils.AttributesHelper;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ExitTracerSpanTest {
    private static final Consumer<ExitTracerSpan> END_HANDLER = span -> {};

    @Test
    public void testReportDatabaseClientSpan() throws Exception {
        final Map<String, Object> attributes = readSpanAttributes("db-span.json");

        ExitTracer tracer = mock(ExitTracer.class);
        final List<ReadWriteSpan> started = new ArrayList<>();
        final List<ReadableSpan> ended = new ArrayList<>();
        SpanProcessor processor = new SpanProcessor() {
            @Override
            public void onStart(Context parentContext, ReadWriteSpan span) {
                started.add(span);
            }

            @Override
            public boolean isStartRequired() {
                return true;
            }

            @Override
            public void onEnd(ReadableSpan span) {
                ended.add(span);
            }

            @Override
            public boolean isEndRequired() {
                return true;
            }
        };
        SpanBuilder spanBuilder = new TestTracerBuilder("test").addSpanProcessor(processor)
                .setResource(Resource.getDefault())
                .withTracer(tracer).build().spanBuilder((String) attributes.remove("name"));
        spanBuilder.setSpanKind(SpanKind.CLIENT).setAllAttributes(AttributesHelper.toAttributes(attributes)).startSpan().end();

        assertEquals(1, started.size());
        assertEquals(1, ended.size());
        ReadWriteSpan startedSpan = started.get(0);
        assertEquals("SELECT petclinic", startedSpan.getName());
        SpanData spanData = startedSpan.toSpanData();
        assertEquals(49, spanData.getAttributes().size());
        assertEquals(4, spanData.getResource().getAttributes().size());
        assertEquals("opentelemetry", spanData.getResource().getAttributes().get(AttributeKey.stringKey("telemetry.sdk.name")));

        //new ExitTracerSpan(tracer, SpanKind.CLIENT, attributes, END_HANDLER).end();
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
        new ExitTracerSpan(tracer, InstrumentationLibraryInfo.empty(), SpanKind.CLIENT, "", SpanContext.getInvalid(), Resource.empty(),attributes, END_HANDLER).end();
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
        new ExitTracerSpan(tracer, InstrumentationLibraryInfo.empty(), SpanKind.CLIENT, "", SpanContext.getInvalid(), Resource.empty(), readSpanAttributes("external-rpc-span.json"), END_HANDLER).end();
        final ArgumentCaptor<HttpParameters> externalParams = ArgumentCaptor.forClass(HttpParameters.class);
        verify(tracer, times(1)).reportAsExternal(externalParams.capture());
        assertEquals("io.opentelemetry.grpc-1.6", externalParams.getValue().getLibrary());
        assertEquals("ResolveBoolean", externalParams.getValue().getProcedure());
        assertEquals("http://opentelemetry-demo-flagd:8013", externalParams.getValue().getUri().toString());
    }

    @Test
    public void testReportHttpClientSpan() throws Exception {
        ExitTracer tracer = mock(ExitTracer.class);
        new ExitTracerSpan(tracer, InstrumentationLibraryInfo.empty(), SpanKind.CLIENT, "", SpanContext.getInvalid(), Resource.empty(),readSpanAttributes("external-http-span.json"), END_HANDLER).end();
        final ArgumentCaptor<HttpParameters> externalParams = ArgumentCaptor.forClass(HttpParameters.class);
        verify(tracer, times(1)).reportAsExternal(externalParams.capture());
        assertEquals("io.opentelemetry.java-http-client", externalParams.getValue().getLibrary());
        assertEquals("https://google.com", externalParams.getValue().getUri().toString());
        assertEquals("GET", externalParams.getValue().getProcedure());
    }

    @Test
    public void testReportHttpClientSpanWithCodeFunction() throws Exception {
        ExitTracer tracer = mock(ExitTracer.class);
        new ExitTracerSpan(tracer, InstrumentationLibraryInfo.empty(), SpanKind.CLIENT, "", SpanContext.getInvalid(), Resource.empty(),readSpanAttributes("external-http-span.json"), END_HANDLER)
                .setAttribute(AttributeKey.stringKey("code.function"), "execute").end();
        final ArgumentCaptor<HttpParameters> externalParams = ArgumentCaptor.forClass(HttpParameters.class);
        verify(tracer, times(1)).reportAsExternal(externalParams.capture());
        assertEquals("io.opentelemetry.java-http-client", externalParams.getValue().getLibrary());
        assertEquals("https://google.com", externalParams.getValue().getUri().toString());
        assertEquals("execute", externalParams.getValue().getProcedure());
    }

    @Test
    public void testBadClientSpan() throws Exception {
        ExitTracer tracer = mock(ExitTracer.class);
        new ExitTracerSpan(tracer, InstrumentationLibraryInfo.empty(), SpanKind.CLIENT, "", SpanContext.getInvalid(), Resource.empty(),readSpanAttributes("bad-client-span.json"), END_HANDLER).end();
        verify(tracer, times(0)).reportAsExternal(any(ExternalParameters.class));
    }

    public static Map<String, Object> readSpanAttributes(String fileName) throws IOException {
        try (InputStream in = ExitTracerSpanTest.class.getResourceAsStream(fileName)) {
            return new ObjectMapper().readValue(in, Map.class);
        }
    }
}
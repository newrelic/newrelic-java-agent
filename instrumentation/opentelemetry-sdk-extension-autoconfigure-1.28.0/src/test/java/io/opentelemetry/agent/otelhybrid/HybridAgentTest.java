/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package io.opentelemetry.agent.otelhybrid;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.SpanEvent;
import com.newrelic.api.agent.Trace;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.sdk.trace.ExitTracerSpan;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "io.opentelemetry" }, configName = "distributed_tracing.yml")
public class HybridAgentTest {
    static {
        System.setProperty("otel.java.global-autoconfigure.enabled", "true");
    }

    static final Tracer OTEL_TRACER = GlobalOpenTelemetry.get().getTracer("test", "1.0");
    public static final String SPAN_ID = "spanId";
    public static final String TRACE_ID = "traceId";
    public static final String PARENT_SPAN_ID = "parentSpanId";
    public static final String PARENT_TRACE_ID = "parentTraceId";

    // OpenTelemetry API and New Relic API can inject outbound trace context
    @Test
    public void doesNotCreateSegmentWithoutATransaction() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Map<String, String> spanDetails = doWorkInSpanWithoutTxn("Bar", SpanKind.INTERNAL);

        AssertionEvaluator.assertNoTxnExists(introspector);
        AssertionEvaluator.assertNoNewRelicSpanExists(introspector);
        AssertionEvaluator.assertSpanDetails(introspector, spanDetails);
    }

    // Starting transaction tests
    @Test
    public void createTxnWhenServerSpanCreated() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Map<String, String> spanDetails = createTransactionWhenServerSpanCreated("Foo");

        AssertionEvaluator.assertTxnExists(introspector, "WebTransaction/Uri/Unknown");
        AssertionEvaluator.assertSpanDetails(introspector, spanDetails);
    }

    // Starting transaction tests
    @Test
    public void createTxnWhenServerSpanCreatedFromRemoteParent() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Map<String, String> spanDetails = createTransactionWhenServerSpanCreatedFromRemoteContext("Bar");

        AssertionEvaluator.assertSpanCount(introspector, 1);
        AssertionEvaluator.assertTxnExists(introspector, "WebTransaction/Uri/Unknown");
        AssertionEvaluator.assertSpanDetails(introspector, spanDetails);
    }

    // Starting transaction tests
    @Test
    public void createTxnWithServerSpanCreatedFromRemoteParent() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Map<String, String> spanDetails = createTransactionWithServerSpanCreatedFromRemoteContext("EdgeCase");

        AssertionEvaluator.assertSpanCount(introspector, 1);
        AssertionEvaluator.assertTxnExists(introspector,
                "OtherTransaction/Custom/io.opentelemetry.agent.otelhybrid.HybridAgentTest/createTransactionWithServerSpanCreatedFromRemoteContext");
        AssertionEvaluator.assertSpanDetails(introspector, spanDetails);
    }

    // Creates OpenTelemetry segment in a transaction
    @Test
    public void createsOtelSegmentInTxn() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Map<String, String> spanDetails = createOtelSegmentInTxn("Foo", SpanKind.INTERNAL);

        AssertionEvaluator.assertSpanCount(introspector, 2);
        AssertionEvaluator.assertTxnExists(introspector, "OtherTransaction/Custom/io.opentelemetry.agent.otelhybrid.HybridAgentTest/createOtelSegmentInTxn");
        AssertionEvaluator.assertSpanDetails(introspector, spanDetails);
    }

    // Creates New Relic span as child of OpenTelemetry span
    @Test
    public void createsNewRelicSpanAsChildOfOtelSpan() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Map<String, String> spanDetails = newRelicSpanAsChildOfOtelSpan("foo", SpanKind.INTERNAL);

        AssertionEvaluator.assertSpanCount(introspector, 3);
        AssertionEvaluator.assertTxnExists(introspector,
                "OtherTransaction/Custom/io.opentelemetry.agent.otelhybrid.HybridAgentTest/newRelicSpanAsChildOfOtelSpan");
        AssertionEvaluator.assertSpanDetails(introspector, spanDetails);
    }

    // OpenTelemetry API can add custom attributes to spans
    @Test
    public void otelSpansCanAddAttributes() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Map<String, String> spanDetails = addAttributesToOtelSpan("foo", SpanKind.INTERNAL);

        AssertionEvaluator.assertSpanCount(introspector, 2);
        AssertionEvaluator.assertTxnExists(introspector, "OtherTransaction/Custom/io.opentelemetry.agent.otelhybrid.HybridAgentTest/addAttributesToOtelSpan");

        Collection<SpanEvent> spanEvents = introspector.getSpanEvents();
        SpanEvent[] eventArray = spanEvents.toArray(new SpanEvent[0]);
        AssertionEvaluator.assertUserAttributeExists("key1", "val1", eventArray[1].getUserAttributes());
        AssertionEvaluator.assertUserAttributeExists("key2", "val2", eventArray[1].getUserAttributes());
        AssertionEvaluator.assertSpanDetails(introspector, spanDetails);
    }

    // OpenTelemetry API can record errors
    @Test
    public void exceptionsAreRecordedOnOtelSpan() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Map<String, String> spanDetails = otelSpanRecordsException("foo", SpanKind.INTERNAL);

        AssertionEvaluator.assertSpanCount(introspector, 2);
        AssertionEvaluator.assertTxnExists(introspector, "OtherTransaction/Custom/io.opentelemetry.agent.otelhybrid.HybridAgentTest/otelSpanRecordsException");

        Collection<SpanEvent> spanEvents = introspector.getSpanEvents();
        SpanEvent[] eventArray = spanEvents.toArray(new SpanEvent[0]);
        AssertionEvaluator.assertExceptionExistsOnSpan(eventArray[1], "oops", "java.lang.Exception");
        AssertionEvaluator.assertSpanDetails(introspector, spanDetails);
    }

    // Inbound distributed tracing tests
    @Test
    public void externalCallWithW3CHeaderInjection() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Map<String, Map<String, String>> mapOfMaps = externalCallInjectsW3CHeaders("foo", SpanKind.CLIENT);

        Map<String, String> carrier = mapOfMaps.get("carrier");
        Map<String, String> spanDetails = mapOfMaps.get("spanDetails");

        AssertionEvaluator.assertSpanCount(introspector, 2);
        AssertionEvaluator.assertTxnExists(introspector,
                "OtherTransaction/Custom/io.opentelemetry.agent.otelhybrid.HybridAgentTest/externalCallInjectsW3CHeaders");
        AssertionEvaluator.assertCarrierContainsW3CTraceParent(carrier);
        AssertionEvaluator.assertSpanDetails(introspector, spanDetails);
    }

    // Inbound distributed tracing tests
    @Test
    public void distributedTracingSpanWithInboundContext() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Map<String, Map<String, String>> mapOfMaps = doWorkInSpanWithInboundContext("foo", SpanKind.SERVER);

        Map<String, String> carrier = mapOfMaps.get("carrier");
        Map<String, String> spanDetails = mapOfMaps.get("spanDetails");

        AssertionEvaluator.assertSpanCount(introspector, 1);
        AssertionEvaluator.assertTxnExists(introspector, "WebTransaction/Uri/Unknown");
        AssertionEvaluator.assertCarrierContainsW3CTraceParent(carrier);
        AssertionEvaluator.assertSpanDetails(introspector, spanDetails);
    }

    // OpenTelemetry API and New Relic API can inject outbound trace context
    @Test
    public void apisInjectOutboundTraceContext() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Map<String, String> spanDetails = testApisInTxn();

        AssertionEvaluator.assertTxnExists(introspector,
                "OtherTransaction/Custom/io.opentelemetry.agent.otelhybrid.HybridAgentTest/testApisInTxn");
        AssertionEvaluator.assertSpanCount(introspector, 3);
        AssertionEvaluator.assertSpanDetails(introspector, spanDetails);
    }

    @Trace(dispatcher = true)
    static Map<String, String> testApisInTxn() {
        Span span = OTEL_TRACER.spanBuilder("OTelSpan1").setSpanKind(SpanKind.CLIENT).startSpan();
        Map<String, String> spanDetails = new HashMap<>();
        try (Scope scope = span.makeCurrent()) {

            Map<String, String> fakeExternalHeaders = new HashMap<>();
            final TextMapPropagator propagator = W3CTraceContextPropagator.getInstance();
            TextMapSetter<Map<String, String>> setter = (carrier1, key, value) -> carrier1.put(key, value);
            Context context = Context.current().with(span);
            propagator.inject(context, fakeExternalHeaders, setter);

            // extract inbound trace context and make it the current scope
            Context extractedContext = propagator.extract(Context.current(), fakeExternalHeaders, getter);

            try (Scope extractedScope = extractedContext.makeCurrent()) {
                // create a new span using the extracted context/scope
                Span fooSpan = OTEL_TRACER.spanBuilder("foo").setSpanKind(SpanKind.CLIENT).startSpan();
                Context fooContext = Context.current().with(span);
                try (Scope fooScope = span.makeCurrent()) {
                    spanDetails = createOTelSpanDetailsMap(fooSpan);
                } catch (Throwable t) {
                    fooSpan.recordException(t);
                } finally {
                    fooSpan.end();
                }
            } catch (Throwable ignored) {
            }
        } catch (Throwable t) {
            span.recordException(t);
        } finally {
            span.end();
        }

        return spanDetails;
    }

    static Map<String, String> createOTelSpanDetailsMap(Span span) {
        Map<String, String> spanDetails = new HashMap<>();
        if (span instanceof ExitTracerSpan) {
            SpanContext spanContext = span.getSpanContext();
            if (spanContext != null && spanContext.isValid()) {
                spanDetails.put(SPAN_ID, spanContext.getSpanId());
                spanDetails.put(TRACE_ID, spanContext.getTraceId());
            }

            Span parentSpan = Span.wrap(((ExitTracerSpan) span).getParentSpanContext());
            if (parentSpan != null) {
                SpanContext parentSpanContext = parentSpan.getSpanContext();
                if (parentSpanContext != null && parentSpanContext.isValid()) {
                    spanDetails.put(PARENT_SPAN_ID, parentSpanContext.getSpanId());
                    spanDetails.put(PARENT_TRACE_ID, parentSpanContext.getTraceId());
                }
            }
        }
        return spanDetails;
    }

    static Map<String, String> doWorkInSpanWithoutTxn(String spanName, SpanKind spanKind) {
        Span span = OTEL_TRACER.spanBuilder(spanName).setSpanKind(spanKind).startSpan();
        Scope scope = span.makeCurrent();
        assertFalse(span.getSpanContext().isValid());

        Map<String, String> spanDetails = createOTelSpanDetailsMap(span);

        scope.close();
        span.end();

        return spanDetails;
    }

    @Trace(dispatcher = true)
    static Map<String, String> createOtelSegmentInTxn(String spanName, SpanKind spanKind) {
        Span span = OTEL_TRACER.spanBuilder(spanName).setSpanKind(spanKind).startSpan();
        Scope scope = span.makeCurrent();

        Map<String, String> spanDetails = createOTelSpanDetailsMap(span);

        scope.close();
        span.end();

        return spanDetails;
    }

    @Trace(dispatcher = true)
    static Map<String, String> newRelicSpanAsChildOfOtelSpan(String spanName, SpanKind spanKind) {
        Span span = OTEL_TRACER.spanBuilder(spanName).setSpanKind(spanKind).startSpan();
        Scope scope = span.makeCurrent();

        newRelicWorkTracer();

        Map<String, String> spanDetails = createOTelSpanDetailsMap(span);

        scope.close();
        span.end();

        return spanDetails;
    }

    @Trace
    static void newRelicWorkTracer() {
        // Do something
    }

    @Trace(dispatcher = true)
    static Map<String, String> addAttributesToOtelSpan(String spanName, SpanKind spanKind) {
        Span span = OTEL_TRACER.spanBuilder(spanName).setSpanKind(spanKind).startSpan();
        Scope scope = span.makeCurrent();

        span.setAttribute("key1", "val1");
        span.setAttribute("key2", "val2");

        Map<String, String> spanDetails = createOTelSpanDetailsMap(span);

        scope.close();
        span.end();

        return spanDetails;
    }

    @Trace(dispatcher = true)
    static Map<String, String> otelSpanRecordsException(String spanName, SpanKind spanKind) {
        Span span = OTEL_TRACER.spanBuilder(spanName).setSpanKind(spanKind).startSpan();
        Scope scope = span.makeCurrent();

        Map<String, String> spanDetails = createOTelSpanDetailsMap(span);

        try {
            throw new Exception("oops");
        } catch (Exception e) {
            span.recordException(e);
            span.setAttribute(AttributeKey.stringKey("error.type"), e.getClass().getCanonicalName());
        } finally {
            scope.close();
            span.end();
        }

        return spanDetails;
    }

    @Trace(dispatcher = true)
    static Map<String, Map<String, String>> externalCallInjectsW3CHeaders(String spanName, SpanKind spanKind) {
        final TextMapPropagator propagator = W3CTraceContextPropagator.getInstance();
        Span span = OTEL_TRACER.spanBuilder(spanName).setSpanKind(spanKind).startSpan();
        Context context = Context.current().with(span);
        Scope scope = span.makeCurrent();

        Map<String, String> carrier = new HashMap<>();
        TextMapSetter<Map<String, String>> setter = (carrier1, key, value) -> carrier1.put(key, value);
        propagator.inject(context, carrier, setter);

        Map<String, String> spanDetails = createOTelSpanDetailsMap(span);

        Map<String, Map<String, String>> mapOfMaps = new HashMap<>();
        mapOfMaps.put("carrier", carrier);
        mapOfMaps.put("spanDetails", spanDetails);

        scope.close();
        span.end();

        return mapOfMaps;
    }

    static Map<String, String> createTransactionWhenServerSpanCreated(String spanName) {
        Span span = OTEL_TRACER.spanBuilder(spanName).setSpanKind(SpanKind.SERVER).startSpan();
        Scope scope = span.makeCurrent();

        Map<String, String> spanDetails = createOTelSpanDetailsMap(span);

        scope.close();
        span.end();

        return spanDetails;
    }

    static Map<String, String> createTransactionWhenServerSpanCreatedFromRemoteContext(String spanName) {
        SpanContext remoteContext = SpanContext.createFromRemoteParent("da8bc8cc6d062849b0efcf3c169afb5a", "7d3efb1b173fecfa", TraceFlags.getSampled(),
                TraceState.getDefault());

        Span spanFromRemoteContext = OTEL_TRACER.spanBuilder(spanName).setSpanKind(SpanKind.SERVER)
                .setParent(Context.current().with(Span.wrap(remoteContext)))
                .startSpan();

        Scope scope = spanFromRemoteContext.makeCurrent();

        Map<String, String> spanDetails = createOTelSpanDetailsMap(spanFromRemoteContext);

        scope.close();
        spanFromRemoteContext.end();

        return spanDetails;
    }

    @Trace(dispatcher = true)
    static Map<String, String> createTransactionWithServerSpanCreatedFromRemoteContext(String spanName) {
        SpanContext remoteContext = SpanContext.createFromRemoteParent("da8bc8cc6d062849b0efcf3c169afb5a", "7d3efb1b173fecfa", TraceFlags.getSampled(),
                TraceState.getDefault());

        Span spanFromRemoteContext = OTEL_TRACER.spanBuilder(spanName).setSpanKind(SpanKind.SERVER)
                .setParent(Context.current().with(Span.wrap(remoteContext)))
                .startSpan();

        Scope scope = spanFromRemoteContext.makeCurrent();

        Map<String, String> spanDetails = createOTelSpanDetailsMap(spanFromRemoteContext);

        scope.close();
        spanFromRemoteContext.end();

        return spanDetails;
    }

    static Map<String, Map<String, String>> doWorkInSpanWithInboundContext(String spanName, SpanKind spanKind) {
        final TextMapPropagator propagator = W3CTraceContextPropagator.getInstance();

        // mock out span with incoming w3c header propagation
        TraceFlags traceFlags = TraceFlags.getDefault();
        TraceState traceState = TraceState.getDefault();
        SpanContext incomingSpanContext = SpanContext.create("da8bc8cc6d062849b0efcf3c169afb5a", "7d3efb1b173fecfa", traceFlags, traceState);
        Span incomingSpan = Span.wrap(incomingSpanContext);
        Context incomingContext = Context.current().with(incomingSpan);
        Scope incomingScope = incomingSpan.makeCurrent();

        Map<String, String> carrier = new HashMap<>();
        TextMapSetter<Map<String, String>> setter = (carrier1, key, value) -> carrier1.put(key, value);
        propagator.inject(incomingContext, carrier, setter);

        incomingScope.close();
        incomingSpan.end();

        // extract inbound trace context and make it the current scope
        Context extractedContext = propagator.extract(Context.current(), carrier, getter);
        Scope extractedScope = extractedContext.makeCurrent();

        // create a new span using the extracted context/scope
        Span span = OTEL_TRACER.spanBuilder(spanName).setSpanKind(spanKind).startSpan();
        Context context = Context.current().with(span);
        Scope scope = span.makeCurrent();

        Map<String, String> spanDetails = createOTelSpanDetailsMap(span);

        Map<String, Map<String, String>> mapOfMaps = new HashMap<>();
        mapOfMaps.put("carrier", carrier);
        mapOfMaps.put("spanDetails", spanDetails);

        extractedScope.close();
        scope.close();
        span.end();

        return mapOfMaps;
    }

    private static final TextMapGetter<Map<String, String>> getter = new TextMapGetter<Map<String, String>>() {
        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
            return carrier.keySet();
        }

        @Override
        public String get(Map<String, String> carrier, String key) {
            return carrier.get(key);
        }
    };

}

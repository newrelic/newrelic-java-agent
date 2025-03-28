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
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "io.opentelemetry" }, configName = "distributed_tracing.yml")
public class HybridAgentTest {
    static {
        System.setProperty("otel.java.global-autoconfigure.enabled", "true");
    }

    static final Tracer OTEL_TRACER = GlobalOpenTelemetry.get().getTracer("test", "1.0");

    @Test
    public void doesNotCreateSegmentWithoutATransaction() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        doWorkInSpanWithoutTxn("Bar", SpanKind.INTERNAL);

        AssertionEvaluator.assertNoTxnExists(introspector);
        AssertionEvaluator.assertNoNewRelicSpanExists(introspector);
    }

    @Test
    public void createTxnWhenServerSpanCreated() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        createTransactionWhenServerSpanCreated("Foo");
        AssertionEvaluator.assertTxnExists(introspector, "WebTransaction/Uri/Unknown");
    }

    @Test
    public void createsOtelSegmentInTxn() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        createOtelSegmentInTxn("Foo", SpanKind.INTERNAL);

        AssertionEvaluator.assertSpanCount(introspector, 2);
        AssertionEvaluator.assertTxnExists(introspector, "OtherTransaction/Custom/io.opentelemetry.agent.otelhybrid.HybridAgentTest/createOtelSegmentInTxn");
    }

    @Test
    public void createsNewRelicSpanAsChildOfOtelSpan() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        newRelicSpanAsChildOfOtelSpan("foo", SpanKind.INTERNAL);

        AssertionEvaluator.assertSpanCount(introspector, 3);
        AssertionEvaluator.assertTxnExists(introspector,
                "OtherTransaction/Custom/io.opentelemetry.agent.otelhybrid.HybridAgentTest/newRelicSpanAsChildOfOtelSpan");
    }

    @Test
    public void otelSpansCanAddAttributes() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        addAttributesToOtelSpan("foo", SpanKind.INTERNAL);

        AssertionEvaluator.assertSpanCount(introspector, 2);
        AssertionEvaluator.assertTxnExists(introspector, "OtherTransaction/Custom/io.opentelemetry.agent.otelhybrid.HybridAgentTest/addAttributesToOtelSpan");

        Collection<SpanEvent> spanEvents = introspector.getSpanEvents();
        SpanEvent[] eventArray = spanEvents.toArray(new SpanEvent[0]);
        AssertionEvaluator.assertUserAttributeExists("key1", "val1", eventArray[1].getUserAttributes());
        AssertionEvaluator.assertUserAttributeExists("key2", "val2", eventArray[1].getUserAttributes());
    }

    @Test
    public void exceptionsAreRecordedOnOtelSpan() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        otelSpanRecordsException("foo", SpanKind.INTERNAL);

        AssertionEvaluator.assertSpanCount(introspector, 2);
        AssertionEvaluator.assertTxnExists(introspector, "OtherTransaction/Custom/io.opentelemetry.agent.otelhybrid.HybridAgentTest/otelSpanRecordsException");

        Collection<SpanEvent> spanEvents = introspector.getSpanEvents();
        SpanEvent[] eventArray = spanEvents.toArray(new SpanEvent[0]);
        AssertionEvaluator.assertExceptionExistsOnSpan(eventArray[1], "oops", "java.lang.Exception");
    }

    @Test
    public void externalCallWithW3CHeaderInjection() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Map<String, String> carrier = externalCallInjectsW3CHeaders("foo", SpanKind.CLIENT);

        AssertionEvaluator.assertSpanCount(introspector, 2);
        AssertionEvaluator.assertTxnExists(introspector,
                "OtherTransaction/Custom/io.opentelemetry.agent.otelhybrid.HybridAgentTest/externalCallInjectsW3CHeaders");
        AssertionEvaluator.assertCarrierContainsW3CTraceParent(carrier);
    }

    static void doWorkInSpanWithoutTxn(String spanName, SpanKind spanKind) {
        Span span = OTEL_TRACER.spanBuilder(spanName).setSpanKind(spanKind).startSpan();
        Scope scope = span.makeCurrent();
        scope.close();
        span.end();
    }

    @Trace(dispatcher = true)
    static void createOtelSegmentInTxn(String spanName, SpanKind spanKind) {
        Span span = OTEL_TRACER.spanBuilder(spanName).setSpanKind(spanKind).startSpan();
        Scope scope = span.makeCurrent();
        scope.close();
        span.end();
    }

    @Trace(dispatcher = true)
    static void newRelicSpanAsChildOfOtelSpan(String spanName, SpanKind spanKind) {
        Span span = OTEL_TRACER.spanBuilder(spanName).setSpanKind(spanKind).startSpan();
        Scope scope = span.makeCurrent();

        newRelicWorkTracer();

        scope.close();
        span.end();
    }

    @Trace
    static void newRelicWorkTracer() {
        // Do something
    }

    @Trace(dispatcher = true)
    static void addAttributesToOtelSpan(String spanName, SpanKind spanKind) {
        Span span = OTEL_TRACER.spanBuilder(spanName).setSpanKind(spanKind).startSpan();
        Scope scope = span.makeCurrent();

        span.setAttribute("key1", "val1");
        span.setAttribute("key2", "val2");

        scope.close();
        span.end();
    }

    @Trace(dispatcher = true)
    static void otelSpanRecordsException(String spanName, SpanKind spanKind) {
        Span span = OTEL_TRACER.spanBuilder(spanName).setSpanKind(spanKind).startSpan();
        Scope scope = span.makeCurrent();

        try {
            throw new Exception("oops");
        } catch (Exception e) {
            span.recordException(e);
            span.setAttribute(AttributeKey.stringKey("error.type"), e.getClass().getCanonicalName());
        } finally {
            scope.close();
            span.end();
        }
    }

    @Trace(dispatcher = true)
    static Map<String, String> externalCallInjectsW3CHeaders(String spanName, SpanKind spanKind) {
        final TextMapPropagator propagator = W3CTraceContextPropagator.getInstance();
        Span span = OTEL_TRACER.spanBuilder(spanName).setSpanKind(spanKind).startSpan();
        Context context = Context.current().with(span);
        Scope scope = span.makeCurrent();

        Map<String, String> carrier = new HashMap<>();
        TextMapSetter<Map<String, String>> setter = (carrier1, key, value) -> carrier1.put(key, value);
        propagator.inject(context, carrier, setter);

        scope.close();
        span.end();

        return carrier;
    }

    static void createTransactionWhenServerSpanCreated(String spanName) {
        Span span = OTEL_TRACER.spanBuilder(spanName).setSpanKind(SpanKind.SERVER).startSpan();
        Scope scope = span.makeCurrent();
        scope.close();
        span.end();
    }
}

package io.opentelemetry.context;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.api.agent.Trace;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "io.opentelemetry" })
public class SpanTest {
    static {
        System.setProperty("otel.java.global-autoconfigure.enabled", "true");
    }
    static Tracer otelTracer = GlobalOpenTelemetry.get().getTracer("test");

    @Test
    public void testSimpleSpans() {
        simpleSpans();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());
        final String txName = introspector.getTransactionNames().iterator().next();
        assertEquals("OtherTransaction/Custom/io.opentelemetry.context.SpanTest/simpleSpans", txName);

        Map<String, TracedMetricData> metricsForTransaction = InstrumentationTestRunner.getIntrospector().getMetricsForTransaction(txName);

        assertEquals(3, metricsForTransaction.size());
        assertTrue(metricsForTransaction.containsKey("Java/io.opentelemetry.context.SpanTest/simpleSpans"));
        assertTrue(metricsForTransaction.containsKey("Span/MyCustomSpan"));
        assertTrue(metricsForTransaction.containsKey("Span/kid"));
    }

    @Test
    public void testAsyncSpans() throws InterruptedException {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            asyncSpans(executor);
            CountDownLatch latch = new CountDownLatch(1);
            executor.execute(latch::countDown);
            latch.await(1, TimeUnit.MINUTES);

            Introspector introspector = InstrumentationTestRunner.getIntrospector();
            assertEquals(1, introspector.getFinishedTransactionCount());
            final String txName = introspector.getTransactionNames().iterator().next();
            assertEquals("OtherTransaction/Custom/io.opentelemetry.context.SpanTest/asyncSpans", txName);

            Map<String, TracedMetricData> metricsForTransaction = InstrumentationTestRunner.getIntrospector().getMetricsForTransaction(txName);

            assertEquals(3, metricsForTransaction.size());
            assertTrue(metricsForTransaction.containsKey("Java/io.opentelemetry.context.SpanTest/asyncSpans"));
            assertTrue(metricsForTransaction.containsKey("Java/OpenTelemetry/AsyncScope"));
            assertTrue(metricsForTransaction.containsKey("Span/MyCustomAsyncSpan"));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testDatabaseSpan() {
        databaseSpan();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());
        final String txName = introspector.getTransactionNames().iterator().next();
        assertEquals("OtherTransaction/Custom/io.opentelemetry.context.SpanTest/databaseSpan", txName);

        Map<String, TracedMetricData> metricsForTransaction = InstrumentationTestRunner.getIntrospector().getMetricsForTransaction(txName);

        assertEquals(2, metricsForTransaction.size());
        assertTrue(metricsForTransaction.containsKey("Java/io.opentelemetry.context.SpanTest/databaseSpan"));
        assertTrue(metricsForTransaction.containsKey("Datastore/statement/mysql/owners/select"));
    }

    @Trace(dispatcher = true)
    static void databaseSpan() {
        Span span = otelTracer.spanBuilder("owners select").setSpanKind(SpanKind.CLIENT)
                .setAttribute("db.system", "mysql")
                .setAttribute("db.operation", "select")
                .setAttribute("db.sql.table", "owners")
                .startSpan();
        span.end();
    }

    @Trace(dispatcher = true)
    static void simpleSpans() {
        Span span = otelTracer.spanBuilder("MyCustomSpan").startSpan();
        Scope scope = span.makeCurrent();
        SpanContext spanContext = span.getSpanContext();
        assertNotNull(spanContext.getTraceId());
        assertNotNull(spanContext.getSpanId());
        assertSame(spanContext, span.getSpanContext());
        Span current = Span.current();
        assertEquals(span, current);
        Span kid = otelTracer.spanBuilder("kid").setParent(Context.current()).startSpan();
        kid.end();
        scope.close();
        span.end();

        withSpan();
    }

    @Trace(dispatcher = true)
    static void asyncSpans(Executor executor) {
        executor.execute(Context.current().wrap(SpanTest::asyncWork));
    }

    static void asyncWork() {
        Span span = otelTracer.spanBuilder("MyCustomAsyncSpan").startSpan();
        span.makeCurrent().close();
        span.end();
    }

    @WithSpan
    static void withSpan() {
        Span span = otelTracer.spanBuilder("kid").startSpan();
        span.end();
    }
}

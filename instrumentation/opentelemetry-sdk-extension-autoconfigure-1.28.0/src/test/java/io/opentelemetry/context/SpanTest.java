package io.opentelemetry.context;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.agent.util.LatchingRunnable;
import com.newrelic.api.agent.Trace;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.sdk.trace.AttributesHelper;
import io.opentelemetry.sdk.trace.ExitTracerSpan;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static io.opentelemetry.sdk.trace.ExitTracerSpanTest.readSpanAttributes;
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
    static final Tracer OTEL_TRACER = GlobalOpenTelemetry.get().getTracer("test", "1.0");

    @Test
    public void testInternalSpansNoTransaction() {
        Span span = OTEL_TRACER.spanBuilder("MyCustomSpan").startSpan();
        span.makeCurrent().close();
        span.end();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        // no transactions because there was no dispatcher trace around the spans
        assertEquals(0, introspector.getFinishedTransactionCount());
    }

    @Test
    public void testConsumerSpan() {
        Span span = OTEL_TRACER.spanBuilder("consume").setSpanKind(SpanKind.CONSUMER).startSpan();
        span.makeCurrent().close();
        span.end();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());

        final String txName = introspector.getTransactionNames().iterator().next();
        assertEquals("OtherTransaction/consume", txName);

        Map<String, TracedMetricData> metricsForTransaction = InstrumentationTestRunner.getIntrospector().getMetricsForTransaction(txName);

        assertEquals(1, metricsForTransaction.size());
        assertTrue(metricsForTransaction.keySet().toString(), metricsForTransaction.containsKey("Span/consume"));
    }

    @Test
    public void testServerSpan() throws IOException {
        Map<String, Object> attributes = readSpanAttributes("server-span.json");
        final String spanName = (String) attributes.remove("name");

        Span span = OTEL_TRACER.spanBuilder(spanName).setSpanKind(SpanKind.SERVER).startSpan();
        span.setAllAttributes(AttributesHelper.toAttributes(attributes));
        span.makeCurrent().close();
        span.end();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());

        final String txName = introspector.getTransactionNames().iterator().next();
        assertEquals("WebTransaction/Uri/owners", txName);

        Map<String, TracedMetricData> metricsForTransaction = InstrumentationTestRunner.getIntrospector().getMetricsForTransaction(txName);

        assertEquals(1, metricsForTransaction.size());
        assertTrue(metricsForTransaction.keySet().toString(), metricsForTransaction.containsKey("Span/GET /owners"));
    }

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
    public void testAsyncSpans() {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            asyncSpans(executor, SpanTest::asyncWork);
            LatchingRunnable.drain(executor);

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
    public void testAsyncSpansWithParentNotWorking() {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            asyncSpans(executor, context -> {
                // this is the correct parent, but it's transaction has already finished, so
                // we can't link it together
                Span parent = Span.fromContext(context);
                assertTrue(parent instanceof ExitTracerSpan);

                // however, we could use the trace id from the parent transaction.  We'd have two metric
                // transactions, but the distributed trace would display the relationship between the spans
                Span span = OTEL_TRACER.spanBuilder("OrphanedSpan").setParent(context).startSpan();
                span.makeCurrent().close();
                span.end();
            });
            LatchingRunnable.drain(executor);

            Introspector introspector = InstrumentationTestRunner.getIntrospector();
            // we have two transactions because the async activity isn't linked together
            assertEquals(2, introspector.getFinishedTransactionCount());
            final String txName = "OtherTransaction/Custom/io.opentelemetry.context.SpanTest/asyncSpans";
            assertTrue(introspector.getTransactionNames().contains("OtherTransaction/Custom/io.opentelemetry.context.SpanTest/asyncSpans"));
            assertTrue(introspector.getTransactionNames().contains("OtherTransaction/Custom"));

            Map<String, TracedMetricData> metricsForTransaction = InstrumentationTestRunner.getIntrospector().getMetricsForTransaction(txName);

            assertEquals(1, metricsForTransaction.size());
            assertTrue(metricsForTransaction.containsKey("Java/io.opentelemetry.context.SpanTest/asyncSpans"));
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
        Span span = OTEL_TRACER.spanBuilder("owners select").setSpanKind(SpanKind.CLIENT)
                .setAttribute("db.system", "mysql")
                .setAttribute("db.operation", "select")
                .setAttribute("db.sql.table", "owners")
                .startSpan();
        span.end();
    }

    @Trace(dispatcher = true)
    static void simpleSpans() {
        Span span = OTEL_TRACER.spanBuilder("MyCustomSpan").startSpan();
        Scope scope = span.makeCurrent();
        SpanContext spanContext = span.getSpanContext();
        assertNotNull(spanContext.getTraceId());
        assertNotNull(spanContext.getSpanId());
        assertSame(spanContext, span.getSpanContext());
        Span current = Span.current();
        assertEquals(span, current);
        Span kid = OTEL_TRACER.spanBuilder("kid").setParent(Context.current()).startSpan();
        kid.end();
        scope.close();
        span.end();

        withSpan();
    }

    @Trace(dispatcher = true)
    static void asyncSpans(Executor executor, Consumer<Context> consumer) {
        Context context = Context.current();
        executor.execute(Context.current().wrap(() -> consumer.accept(context)));
    }

    static void asyncWork(Context context) {
        Span span = OTEL_TRACER.spanBuilder("MyCustomAsyncSpan").startSpan();
        span.makeCurrent().close();
        span.end();
    }

    @WithSpan
    static void withSpan() {
        Span span = OTEL_TRACER.spanBuilder("kid").startSpan();
        span.end();
    }
}

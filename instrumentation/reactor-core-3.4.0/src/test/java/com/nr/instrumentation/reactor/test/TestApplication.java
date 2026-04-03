package com.nr.instrumentation.reactor.test;

import com.newrelic.agent.introspec.*;
import com.newrelic.api.agent.Trace;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = "reactor.core")
public class TestApplication {

    private static final String TRY_EMIT = "Java/reactor.core.publisher.NextProcessor/tryEmitValue";
    private static final String MONO_NEXT = "Custom/com.nr.instrumentation.reactor.test.TestMonoCoreSubscriber/onNext";
    private static final String MONO_COMPLETE = "Custom/com.nr.instrumentation.reactor.test.TestMonoCoreSubscriber/onComplete";
    private static final String MONO_SUBSCRIBE = "Custom/com.nr.instrumentation.reactor.test.TestMonoCoreSubscriber/onSubscribe";
    private static final String BEST_EFFORT_NEXT = "Java/reactor.core.publisher.SinkManyBestEffort/tryEmitNext";
    private static final String SERIALIZED_NEXT = "Java/reactor.core.publisher.SinkManySerialized/tryEmitNext";
    private static final String SUBSCRIBER_NEXT = "Custom/com.nr.instrumentation.reactor.test.TestFluxCoreSubscriber/onNext";
    private static final String  SERIALIZED_COMPLETE = "Java/reactor.core.publisher.SinkManySerialized/tryEmitComplete";
    private static final String BEST_EFFORT_COMPLETE = "Java/reactor.core.publisher.SinkManyBestEffort/tryEmitComplete";

    @Test
    public void doSinkOneTest() {
        testSinkOne();
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(5000);
        Assert.assertEquals("Expected one transaction", 1, finishedTransactionCount);

        Collection<String> txnNames = introspector.getTransactionNames();
        String txnName = txnNames.iterator().next();

        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(txnName);
        Set<String> names = metrics.keySet();
        Assert.assertTrue(names.contains(TRY_EMIT));
        Assert.assertTrue(names.contains(MONO_NEXT));
        Assert.assertTrue(names.contains(MONO_COMPLETE));

    }

    @Trace(dispatcher = true)
    public void testSinkOne() {

        Sinks.One<String> sink = Sinks.one();
        Mono<String> mono = sink.asMono();

        AwaitSingle awaitSingle = new AwaitSingle();
        mono.subscribe(new TestMonoCoreSubscriber(awaitSingle));

        Schedulers.single().schedule(() -> {
            Sinks.EmitResult result = sink.tryEmitValue("Hello");
            if(result.isFailure()) {
                System.out.println("EmitResult threw an error: "+ result);
            }
        });

        String result = awaitSingle.await();
        System.out.println("Await Single result : " + result);
    }

    @Test
    public void doTestSinkMany() {
        testSinkMany();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(5000);
        Assert.assertEquals("Expected one transaction", 1, finishedTransactionCount);

        Collection<String> txnNames = introspector.getTransactionNames();
        String txnName = txnNames.iterator().next();

        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(txnName);
        Set<String> names = metrics.keySet();
        Assert.assertTrue(names.contains(BEST_EFFORT_NEXT));
        Assert.assertTrue(names.contains(SERIALIZED_NEXT));
        Assert.assertTrue(names.contains(SUBSCRIBER_NEXT));
        Assert.assertTrue(names.contains(BEST_EFFORT_COMPLETE));
        Assert.assertTrue(names.contains(SERIALIZED_COMPLETE));

        TracedMetricData metric = metrics.get(BEST_EFFORT_NEXT);
        Assert.assertEquals(5, metric.getCallCount());

        metric = metrics.get(SERIALIZED_NEXT);
        Assert.assertEquals(5, metric.getCallCount());

        metric = metrics.get(SUBSCRIBER_NEXT);
        Assert.assertEquals(5, metric.getCallCount());

        metric = metrics.get(BEST_EFFORT_COMPLETE);
        Assert.assertEquals(1, metric.getCallCount());

        metric = metrics.get(SERIALIZED_COMPLETE);
        Assert.assertEquals(1, metric.getCallCount());

    }

    @Trace(dispatcher = true)
    public void testSinkMany() {
        Sinks.Many<String> hotSource = Sinks.many().multicast().directBestEffort();

        Flux<String> flux = hotSource.asFlux();
        AwaitMany awaitMany = new AwaitMany();
        flux.subscribe(new TestFluxCoreSubscriber(awaitMany, 5));

        Scheduler scheduler = Schedulers.single();
        String[] colors = new String[]{"Red", "Green", "Blue", "Pink", "Purple"};
        for(String color : colors) {
            CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
            scheduler.schedule(() -> {
                hotSource.emitNext(color, Sinks.EmitFailureHandler.FAIL_FAST);
                completableFuture.complete(true);
            }, 100, TimeUnit.MILLISECONDS);

            try {
                Boolean result = completableFuture.get();
            } catch (InterruptedException | ExecutionException ignored) {
            }

        }
        hotSource.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);

        List<String> results = awaitMany.await();
        System.out.println("Await Many result : " + results);
    }

    private void printSegments(TraceSegment segment, int indents) {
        String segmentName = segment.getName();
        String classname = segment.getClassName();
        String methodName = segment.getMethodName();
        int callCount = segment.getCallCount();
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < indents; i++) {
            sb.append("  ");
        }
        sb.append("Name: ").append(segmentName);
        sb.append(", Class: ").append(classname);
        sb.append(", Method: ").append(methodName);
        sb.append(", CallCount: ").append(callCount);
        Map<String, Object> attributes = segment.getTracerAttributes();
        if(attributes != null) {
            Set<String> keys = attributes.keySet();
            for(String key : keys) {
                Object value = attributes.get(key);
                sb.append(", Attribute: ").append(key);
                sb.append(": ");
                sb.append(value);
            }
        }
        System.out.println(sb.toString());
        List<TraceSegment> children = segment.getChildren();
        for(TraceSegment child : children) {
            printSegments(child, indents + 2);
        }
    }


}

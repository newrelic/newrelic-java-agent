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

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = "reactor.core")
public class TestApplication {


    private static final String TRY_EMIT = "Java/reactor.core.publisher.SinkOneSerialized/tryEmitValue";
    private static final String TRY_EMIT2 = "Java/reactor.core.publisher.SinkOneMulticast/tryEmitValue";
    private static final String MONO_NEXT = "Custom/com.nr.instrumentation.reactor.test.TestMonoCoreSubscriber/onNext";
    private static final String MONO_COMPLETE = "Custom/com.nr.instrumentation.reactor.test.TestMonoCoreSubscriber/onComplete";
    private static final String MONO_SUBSCRIBE = "Custom/com.nr.instrumentation.reactor.test.TestMonoCoreSubscriber/onSubscribe";
    private static final String BEST_EFFORT_NEXT = "Java/reactor.core.publisher.SinkManyBestEffort/tryEmitNext";
    private static final String SERIALIZED_NEXT = "Java/reactor.core.publisher.SinkManySerialized/tryEmitNext";
    private static final String SUBSCRIBER_NEXT = "Custom/com.nr.instrumentation.reactor.test.TestFluxCoreSubscriber/onNext";
    private static final String  SERIALIZED_COMPLETE = "Java/reactor.core.publisher.SinkManySerialized/tryEmitComplete";
    private static final String BEST_EFFORT_COMPLETE = "Java/reactor.core.publisher.SinkManyBestEffort/tryEmitComplete";
    private static final String txn1 = "OtherTransaction/Custom/com.nr.instrumentation.reactor.test.TestApplication/testMonoSub";
    private static final String txn2 = "OtherTransaction/Custom/com.nr.instrumentation.reactor.test.TestApplication/testMonoPub";
    private static final String txn3 = "OtherTransaction/Custom/com.nr.instrumentation.reactor.test.TestApplication/testFluxSub";
    private static final String txn4 = "OtherTransaction/Custom/com.nr.instrumentation.reactor.test.TestApplication/testFluxPub";
    private static final String WRAPPER = "Java/com.nr.instrumentation.reactor.NRRunnableWrapper/run";
    private static final String[] fluxArray = {"Message 1", "Message 2", "Message 3", "Message 4"};

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
        Assert.assertTrue(names.contains(TRY_EMIT2));
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

    @Test
    public void doMonoSubscribeOnTest() {
        testMonoSub();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(5000);
        Assert.assertEquals("Expected one transactions", 1, finishedTransactionCount);

        Collection<String> txnNames = introspector.getTransactionNames();
        boolean contains = txnNames.contains(txn1); // & txnNames.contains(txn2);
        Assert.assertTrue(contains);

        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(txn1);
        Set<String> names = metrics.keySet();
        Assert.assertTrue(names.contains(WRAPPER));
        Assert.assertTrue(names.contains(MONO_NEXT));
        Assert.assertTrue(names.contains(MONO_COMPLETE));

        Collection<TransactionTrace> traces = introspector.getTransactionTracesForTransaction(txn1);
        for (TransactionTrace transactionTrace : traces) {
            TraceSegment initialSegment = transactionTrace.getInitialTraceSegment();
            List<TraceSegment> children = initialSegment.getChildren();
            boolean passes = false;
			/*
			Assure that the subscribing occurs on another thread
			 */
            for (TraceSegment child : children) {
                if (child.getName().equals(WRAPPER)) {
                    Map<String, Object> initialAttributes = initialSegment.getTracerAttributes();
                    int initialThread;
                    if (initialAttributes.containsKey("thread.id")) {
                        initialThread = Integer.parseInt(initialAttributes.get("thread.id").toString());
                    } else {
                        initialThread = -1;
                    }
                    Map<String, Object> attributes = child.getTracerAttributes();
                    int childThread;
                    if (attributes.containsKey("thread.id")) {
                        childThread = Integer.parseInt(attributes.get("thread.id").toString());
                    } else {
                        childThread = -1;
                    }
                    Assert.assertTrue(initialThread != childThread);
                    passes = true;
                    break;
                }
            }
            Assert.assertTrue(passes);

        }
    }

    @Test
    public void doMonoPublishOnTest() {
        testMonoPub();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(5000);
        Assert.assertEquals("Expected one transaction", 1, finishedTransactionCount);

        Collection<String> txnNames = introspector.getTransactionNames();
        boolean contains = txnNames.contains(txn2);
        Assert.assertTrue(contains);

        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(txn2);
        Set<String> names = metrics.keySet();
        Assert.assertTrue(names.contains(WRAPPER));
        Assert.assertTrue(names.contains(MONO_NEXT));
        Assert.assertTrue(names.contains(MONO_COMPLETE));

        Collection<TransactionTrace> traces = introspector.getTransactionTracesForTransaction(txn2);
        for(TransactionTrace transactionTrace : traces) {
            TraceSegment initialSegment = transactionTrace.getInitialTraceSegment();
            List<TraceSegment> children = initialSegment.getChildren();
            boolean passes = false;
			/*
			Assure that the publishing occurs on another thread
			 */
            for(TraceSegment child : children) {
                if(child.getName().equals(WRAPPER)) {
                    Map<String, Object> initialAttributes = initialSegment.getTracerAttributes();
                    int initialThread;
                    if(initialAttributes.containsKey("thread.id")) {
                        initialThread = Integer.parseInt(initialAttributes.get("thread.id").toString());
                    } else {
                        initialThread = -1;
                    }
                    Map<String, Object> attributes = child.getTracerAttributes();
                    int childThread;
                    if(attributes.containsKey("thread.id")) {
                        childThread = Integer.parseInt(attributes.get("thread.id").toString());
                    }  else {
                        childThread = -1;
                    }
                    Assert.assertTrue(initialThread != childThread);
                    passes = true;
                }
            }
            Assert.assertTrue(passes);

        }

    }

    @Test
    public void doFluxPublishOnTest() {
        testFluxPub();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(5000);
        Assert.assertEquals("Expected one transaction", 1, finishedTransactionCount);

        Collection<String> txnNames = introspector.getTransactionNames();
        boolean contains = txnNames.contains(txn4);
        Assert.assertTrue(contains);

        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(txn4);
        Set<String> names = metrics.keySet();
        Assert.assertTrue(names.contains(WRAPPER));
        Assert.assertTrue(names.contains("Custom/com.nr.instrumentation.reactor.test.TestFluxCoreSubscriber/onNext"));
        TracedMetricData metricData = metrics.get("Custom/com.nr.instrumentation.reactor.test.TestFluxCoreSubscriber/onNext");
        Assert.assertNotNull(metricData);
        Assert.assertEquals(4, metricData.getCallCount());

    }

    @Test
    public void doFluxSubscribeOnTest() {
        testFluxSub();
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(5000);
        Assert.assertEquals("Expected one transaction", 1, finishedTransactionCount);

        Collection<String> txnNames = introspector.getTransactionNames();
        boolean contains = txnNames.contains(txn3); // & txnNames.contains(txn2);
        Assert.assertTrue(contains);

        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(txn3);
        Set<String> names = metrics.keySet();
        Assert.assertTrue(names.contains(WRAPPER));
    }

    @Test
    public void doScheduleTest() {
        testScheduler();
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        int finishedTransactionCount = introspector.getFinishedTransactionCount(5000);
        Assert.assertEquals("Expected one transaction", 1, finishedTransactionCount);
        String txnName = introspector.getTransactionNames().iterator().next();
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(txnName);
        Set<String> names = metrics.keySet();
        Assert.assertTrue(names.contains(WRAPPER));
        Collection<TransactionTrace> traces = introspector.getTransactionTracesForTransaction(txnName);
        TransactionTrace transactionTrace = traces.iterator().next();
        Assert.assertNotNull(transactionTrace);
        TraceSegment initial = transactionTrace.getInitialTraceSegment();
        Assert.assertNotNull(initial);
        List<TraceSegment> children = initial.getChildren();
        boolean passes = false;
		/*
		Ensure that execution is dispatched to another thread and that Mono actions occur on that thread
		 */
        for(TraceSegment child : children) {
            if(child.getName().equals(WRAPPER)) {
                List<TraceSegment> wrapperChildren = child.getChildren();
                Set<String> childNames = new HashSet<>();
                TraceSegment subscribeTrace = null;
                for(TraceSegment wrapperChild : wrapperChildren) {
                    String childName = wrapperChild.getName();
                    childNames.add(wrapperChild.getName());
                    if(childName.equals(MONO_SUBSCRIBE)) subscribeTrace = wrapperChild;
                }
                Assert.assertTrue(childNames.contains(MONO_SUBSCRIBE));
                Assert.assertNotNull(subscribeTrace);
                List<TraceSegment> subscriberChildren = subscribeTrace.getChildren();
                childNames.clear();
                for(TraceSegment subscriberChild : subscriberChildren) {
                    String childName = subscriberChild.getName();
                    childNames.add(subscriberChild.getName());
                }
                Assert.assertTrue(childNames.contains(MONO_NEXT));
                Assert.assertTrue(childNames.contains(MONO_COMPLETE));
            }
        }
    }


    @Trace(dispatcher = true)
    public void testScheduler() {
        System.out.println("Enter testScheduler");
        Mono<String> mono = getStringMono();
        AwaitSingle await = new AwaitSingle();
        Scheduler scheduler = Schedulers.single();
        scheduler.schedule(() -> {
            mono.subscribe(new TestMonoCoreSubscriber(await));
        });
        String result = await.await();
        System.out.println("TestScheduler result: " + result);

    }

    @Trace(dispatcher = true)
    public void testMonoPub() {
        System.out.println("Enter testMonoPub");
        AwaitSingle await = new AwaitSingle();
        Mono<String> mono = getStringMono();

        mono.publishOn(Schedulers.single()).subscribe(new TestMonoCoreSubscriber(await));
        await.await();

        System.out.println("Exit testMonoPub with result: " + await.getResult());

    }

    @Trace(dispatcher = true)
    public void testMonoSub() {
        System.out.println("Enter testMonoSub");
        AwaitSingle await = new AwaitSingle();

        Mono<String> mono = getStringMono().subscribeOn(Schedulers.single()).doOnSubscribe(new SubscriptionConsumer());

        mono.subscribe(new TestMonoCoreSubscriber(await));
        String result = await.await();
        System.out.println("Exit testMonoSub with result: " + result);

    }

    @Trace(dispatcher = true)
    public void testFluxSub() {
        System.out.println("Enter testFluxSub");

        Flux<String> flux = getStringFlux().subscribeOn(Schedulers.single());

        flux.subscribe(this::doSubscribeAction);

        List<String> list = flux.collectList().block();
        System.out.println("Exit testFluxSub with result: " + list);
    }

    @Trace(dispatcher = true)
    public void testFluxPub() {
        System.out.println("Enter testFluxPub");
        Flux<String> flux = getStringFlux().publishOn(Schedulers.single());
        AwaitMany await = new AwaitMany();

        flux.subscribe(new TestFluxCoreSubscriber(await,4));

        List<String> list = await.await();
        System.out.println("Exit testFluxPub with result: "+ list);

    }


    public Flux<String> getStringFlux() {
        return Flux.fromArray(fluxArray);
    }

    public Mono<String> getStringMono() {


        return Mono.fromCallable(() -> {
            try {
                Thread.sleep(100L);
            } catch(Exception ignored) {

            }
            return "hello";
        });
    }

    @Trace
    public void doSubscribeAction(String s) {
        try {
            Thread.sleep(100L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Result is "+s);
    }


}

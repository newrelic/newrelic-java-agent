package com.nr.instrumentation.reactor.test;

import com.newrelic.agent.introspec.*;
import com.newrelic.api.agent.Trace;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.concurrent.TimeUnit;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = "reactor.core")
public class TestApplication {
	
	private static final String txn1 = "OtherTransaction/Custom/com.nr.instrumentation.reactor.test.TestApplication/testMonoSub";
	private static final String txn2 = "OtherTransaction/Custom/com.nr.instrumentation.reactor.test.TestApplication/testMonoPub";
	private static final String txn3 = "OtherTransaction/Custom/com.nr.instrumentation.reactor.test.TestApplication/testFluxSub";
	private static final String txn4 = "OtherTransaction/Custom/com.nr.instrumentation.reactor.test.TestApplication/testFluxPub";
	private static final String WRAPPER = "Java/com.nr.instrumentation.reactor.NRRunnableWrapper/run";
	private static final String MONO_NEXT = "Custom/com.nr.instrumentation.reactor.test.TestMonoCoreSubscriber/onNext";
	private static final String MONO_COMPLETE = "Custom/com.nr.instrumentation.reactor.test.TestMonoCoreSubscriber/onComplete";
	private static final String MONO_SUBSCRIBE = "Custom/com.nr.instrumentation.reactor.test.TestMonoCoreSubscriber/onSubscribe";
	private static final String[] fluxArray = {"Message 1", "Message 2", "Message 3", "Message 4"};

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
		System.out.println("Transaction traces:  " + traces.size());
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
		System.out.println("Transaction traces:  " + traces.size());
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
	public void doMonoSinkTest() {
		monoSinkTest();
		Introspector introspector = InstrumentationTestRunner.getIntrospector();
		int finishedTransactionCount = introspector.getFinishedTransactionCount(5000);
		Assert.assertEquals("Expected one transaction", 1, finishedTransactionCount);

		Collection<String> txnNames = introspector.getTransactionNames();
		String txnName = txnNames.iterator().next();

		Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(txnName);
		Set<String> names = metrics.keySet();
		// indicates that execution went to another thread
		Assert.assertTrue(names.contains(WRAPPER));
		Assert.assertTrue(names.contains(MONO_NEXT));
		Assert.assertTrue(names.contains(MONO_COMPLETE));

	}

	@Trace(dispatcher = true)
	public void monoSinkTest() {
		Mono<String> delayedMono = Mono.create(sink -> {
			// Schedule an operation to emit a value after a delay
			Scheduler scheduler = Schedulers.single();
			scheduler.schedule(() -> {
				System.out.println("Emitting value from MonoSink...");
				sink.success("Hello from MonoSink!"); // Emit the value and complete
				scheduler.dispose();
			}, 500, TimeUnit.MILLISECONDS); // Delay for 500 MS

			// Optional: Handle cancellation
			sink.onDispose(() -> {
				System.out.println("MonoSink disposed (e.g., subscriber cancelled)");
				scheduler.dispose();
			});
		});

		AwaitSingle awaitSingle = new AwaitSingle();
		delayedMono.subscribe(new TestMonoCoreSubscriber(awaitSingle));
		String result = awaitSingle.await();
		System.out.println("Result of MonoSink is " + result);

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
				for(TraceSegment wrapperChild : wrapperChildren) {
					childNames.add(wrapperChild.getName());
				}
				Assert.assertTrue(childNames.contains(MONO_SUBSCRIBE));
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

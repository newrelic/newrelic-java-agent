package com.nr.redisson328.instrumentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.newrelic.agent.introspec.TraceSegment;
import com.newrelic.agent.introspec.TransactionTrace;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.runner.RunWith;
import org.reactivestreams.Publisher;
import org.redisson.Redisson;
import org.redisson.api.RBatch;
import org.redisson.api.RBucket;
import org.redisson.api.RDequeReactive;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.redisson.api.RedissonReactiveClient;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.api.agent.Trace;

import org.redisson.config.Config;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.redisson.command" , "io.netty.channel"})
public class RedissonClientTest {

	@Rule
	public GenericContainer redis = new GenericContainer(DockerImageName.parse("redis:5.0.3-alpine"))
			.withExposedPorts(6379);

	public RedissonClient client = null;
	public RedissonReactiveClient reactive = null;

	@Before
	public void before() throws IOException {
		Config config = new Config();
		config.useSingleServer().setAddress("redis://" + redis.getHost() + ":" + redis.getMappedPort(6379));

		client = Redisson.create(config);
		reactive = Redisson.create(config).reactive();
	}
	
	@AfterEach
	public void after() {
		reactive.shutdown();
	}
	
	@Test
	public void dequeReactiveTest() {
		testDequeReactive();
		Introspector introspector = InstrumentationTestRunner.getIntrospector();
		int count = introspector.getFinishedTransactionCount(2500);
		System.out.println("There are "+count+" transactions");
		assertEquals(1,count);
		Collection<String> txnNames = introspector.getTransactionNames();
		String txnName = "OtherTransaction/Custom/com.nr.redisson328.instrumentation.RedissonClientTest/testDequeReactive";
		assertTrue(txnNames.contains(txnName));
		
		Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(txnName);
		assertTrue(metrics.containsKey("Datastore/operation/Redis/DEL"));
		assertTrue(metrics.containsKey("Datastore/operation/Redis/LPOP"));
		assertTrue(metrics.containsKey("Datastore/operation/Redis/LPUSH"));
		assertTrue(metrics.containsKey("Datastore/operation/Redis/LLEN"));
		assertTrue(metrics.containsKey("Datastore/operation/Redis/RPUSH"));
	}
	
	@Trace(dispatcher=true)
	public void testDequeReactive() {
		RDequeReactive<String> deque = reactive.getDeque("testDeque");
		Integer size = sync(deque.size());
		System.out.println("Size is "+size);
		if(size != null && size > 0) {
			for(int i=0;i<size;i++) {
				sync(deque.removeLast());
			}
		}
		Boolean i = sync(deque.add("1"));
		System.out.println("result of add to deque is "+i);
		sync(deque.addFirst("2"));
		size = sync(deque.size());
		System.out.println("Size is "+size);
		String popResult = sync(deque.pop());
		size = sync(deque.size());
		System.out.println("Result of pop is "+popResult);
		System.out.println("Size is "+size);
		System.out.println("result of delete is "+sync(deque.delete()));
	}
	
	@Test
	public void queueTest() {
		testQueue();
		
		Introspector introspector = InstrumentationTestRunner.getIntrospector();
		int count = introspector.getFinishedTransactionCount(2500);
		assertEquals(count, 1);

		Collection<String> txnNames = introspector.getTransactionNames();
		String txnName = "OtherTransaction/Custom/com.nr.redisson328.instrumentation.RedissonClientTest/testQueue";
		assertTrue(txnNames.contains(txnName));
		Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(txnName);
		assertTrue(metrics.containsKey("Datastore/operation/Redis/EVAL"));
		assertTrue(metrics.containsKey("Datastore/operation/Redis/DEL"));
		assertTrue(metrics.containsKey("Datastore/operation/Redis/LLEN"));
		assertTrue(metrics.containsKey("Datastore/operation/Redis/RPUSH"));
		
	}
	
	@Test
	public void bucketTest() {
		testBucket();
		
		Introspector introspector = InstrumentationTestRunner.getIntrospector();
		int count = introspector.getFinishedTransactionCount(2500);
		assertEquals(count, 1);

		Collection<String> txnNames = introspector.getTransactionNames();
		String txnName = "OtherTransaction/Custom/com.nr.redisson328.instrumentation.RedissonClientTest/testBucket";
		assertTrue(txnNames.contains(txnName));
		Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(txnName);
		assertTrue(metrics.containsKey("Datastore/operation/Redis/SETNX"));
		assertTrue(metrics.containsKey("Datastore/operation/Redis/EVAL"));
		assertTrue(metrics.containsKey("Datastore/operation/Redis/STRLEN"));
		assertTrue(metrics.containsKey("Datastore/operation/Redis/GETSET"));
		assertTrue(metrics.containsKey("Datastore/operation/Redis/PSETEX"));
		assertTrue(metrics.containsKey("Datastore/operation/Redis/SET"));
	}
	
	@Trace(dispatcher=true)
	public void testBucket() {
		RBucket<String> bucket = client.getBucket("test");
		bucket.set("123");
		boolean isUpdated = bucket.compareAndSet("123", "4948");
		assertTrue("compareAndSet Failed",isUpdated);
		String prevObject = bucket.getAndSet("321");
		System.out.println("Result of getAndSet is "+prevObject);

        boolean isSet = bucket.trySet("901");
        System.out.println("Result of trySet is "+isSet);
        long objectSize = bucket.size();
        System.out.println("Result of size is "+objectSize);
        
        // set with expiration
        bucket.set("value", 10, TimeUnit.SECONDS);
        boolean isNewSet = bucket.trySet("nextValue", 10, TimeUnit.SECONDS);
        System.out.println("Result of isNewSet is "+isNewSet);

	}
	
	@Trace(dispatcher=true)
	public void testQueue() {
		RQueue<String> queue = client.getQueue("myQueue");
		
		queue.clear();
		
		boolean added = queue.add("1");
		assertTrue("1 not added", added);
		added = queue.add("2");
		assertTrue("2 not added", added);
		added = queue.add("3");
		assertTrue("3 not added", added);
		
		boolean contains = queue.contains("1");
		assertTrue("queue does not contain 1", contains);

		int size = queue.size();
		assertEquals(size, 3);
	}

	@Test
	public void batchTest() {
		testBatch();

		Introspector introspector = InstrumentationTestRunner.getIntrospector();
		int count = introspector.getFinishedTransactionCount(2500);
		assertEquals(count, 1);

		Collection<String> txnNames = introspector.getTransactionNames();
		String txnName = "OtherTransaction/Custom/com.nr.redisson328.instrumentation.RedissonClientTest/testBatch";
		assertTrue(txnNames.contains(txnName));

		Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(txnName);
		final String redisBatchMetricName = "Datastore/operation/Redis/BATCH-EXECUTE_HSET_HSET_INCR";
		final String redisGetMetricName = "Datastore/operation/Redis/GET";
		assertTrue(metrics.containsKey(redisBatchMetricName));
		assertTrue(metrics.containsKey(redisGetMetricName));

		Collection<TransactionTrace> traces = introspector.getTransactionTracesForTransaction(txnName);
		assertEquals(1, traces.size());

		TransactionTrace rootTrace = traces.stream().findFirst().get();
		List<TraceSegment> segments = rootTrace.getInitialTraceSegment().getChildren();
		assertEquals(2, segments.size());

		TraceSegment redisBatchSegment = segments.stream().filter(s -> {
			return s.getName().equals(redisBatchMetricName);
		}).findFirst().get();

		assertEquals(redis.getMappedPort(6379).toString(),
				redisBatchSegment.getTracerAttributes().get("port_path_or_id"));

		TraceSegment redisGetSegment = segments.stream().filter(s -> s.getName().equals(redisGetMetricName)).findFirst().get();
		assertEquals(redis.getMappedPort(6379).toString(),
				redisGetSegment.getTracerAttributes().get("port_path_or_id"));
	}

	@Trace(dispatcher=true)
	public void testBatch() {
		RBatch batch = client.createBatch();
		batch.getMap("userCache").fastPutAsync("user1", "data1 - " + LocalDateTime.now());
		batch.getMap("userCache").fastPutAsync("user2", "data2 - " + LocalDateTime.now());
		batch.getAtomicLong("counter").incrementAndGetAsync();
		batch.execute();
		client.getAtomicLong("counter").get();
		client.getMap("userCache");
	}

	private static <V> V sync(Publisher<V> obs) {
		return Mono.from(obs).block();
	}
}

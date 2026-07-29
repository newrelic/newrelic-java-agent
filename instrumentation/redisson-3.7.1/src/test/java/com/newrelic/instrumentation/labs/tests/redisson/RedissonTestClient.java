package com.newrelic.instrumentation.labs.tests.redisson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
//import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reactivestreams.Publisher;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RDequeReactive;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.redisson.api.RedissonReactiveClient;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
//import com.newrelic.agent.introspec.TraceSegment;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.api.agent.Trace;

import reactor.core.publisher.Mono;
import redis.embedded.RedisServer;


@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.redisson" })
public class RedissonTestClient {

	static RedissonClient client = null;
	static RedissonReactiveClient reactive = null;
	protected static RedisServer redisServer = null;
	
	@BeforeClass
	public static void beforeClass() throws IOException {
		InputStream stream = RedissonTestClient.class.getResourceAsStream("/redis.properties");
		if (stream != null) {
			System.out.println("Opened inputstream to redis.properties");
		} else {
			System.out.println("Failed to open inputstream to redis.properties");
		}
		Properties properties = new Properties();
		if (stream != null) {
			properties.load(stream);
		}

		boolean useEmbedded = Boolean.parseBoolean(properties.getProperty("redis-server-embedded", "true"));

		String host = properties.getProperty("redis-server-host", "localhost");
		int port = Integer.parseInt(properties.getProperty("redis-server-port", "6379"));

		if (useEmbedded) {
			redisServer = new RedisServer(port);
			redisServer.start();
			System.out.println("Embedded Redis Server started on port " + port);
		} else {
			System.out.println("Will use external Redis server on " + host + " and port " + port);
		}
		try {
			Thread.sleep(5000L);
		} catch (Exception e) {
			e.printStackTrace();
		}
		client = Redisson.create();
		reactive = Redisson.createReactive();
	}
	
	@AfterClass
	public static void afterClass() {
		if(redisServer != null) {
			redisServer.stop();
		}
	}
	
	@Test
	public void dequeReactiveTest() {
		testDequeReactive();
		Introspector introspector = InstrumentationTestRunner.getIntrospector();
		int count = introspector.getFinishedTransactionCount(2500);
		System.out.println("There are "+count+" transactions");
		assertEquals(1,count);
		Collection<String> txnNames = introspector.getTransactionNames();
		String txnName = "OtherTransaction/Custom/com.nr.instrumentation.redisson.RedissonTestClient/testDequeReactive";
		assertTrue(txnNames.contains(txnName));
		
			Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(txnName);
			assertTrue(metrics.containsKey("Datastore/statement/Redisson/Deque/ADD"));
			assertTrue(metrics.containsKey("Datastore/statement/Redisson/Deque/POP"));
			assertTrue(metrics.containsKey("Datastore/statement/Redisson/Deque/ADDFIRST"));
			assertTrue(metrics.containsKey("Datastore/statement/Redisson/Deque/SIZE"));
			assertTrue(metrics.containsKey("Datastore/statement/Redisson/Deque/DELETE"));
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
		Integer i = sync(deque.add("1"));
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
		String txnName = "OtherTransaction/Custom/com.nr.instrumentation.redisson.RedissonTestClient/testQueue";
		assertTrue(txnNames.contains(txnName));
		Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(txnName);
		assertTrue(metrics.containsKey("Datastore/statement/Redisson/Queue/ADD"));
		assertTrue(metrics.containsKey("Datastore/statement/Redisson/Queue/CLEAR"));
		assertTrue(metrics.containsKey("Datastore/statement/Redisson/Queue/SIZE"));
		assertTrue(metrics.containsKey("Datastore/statement/Redisson/Queue/CONTAINS"));
		
	}
	
	@Test
	public void bucketTest() {
		testBucket();
		
		Introspector introspector = InstrumentationTestRunner.getIntrospector();
		int count = introspector.getFinishedTransactionCount(2500);
		assertEquals(count, 1);

		Collection<String> txnNames = introspector.getTransactionNames();
		String txnName = "OtherTransaction/Custom/com.nr.instrumentation.redisson.RedissonTestClient/testBucket";
		assertTrue(txnNames.contains(txnName));
		Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(txnName);
		assertTrue(metrics.containsKey("Datastore/statement/Redisson/Bucket/GETANDSET"));
		assertTrue(metrics.containsKey("Datastore/statement/Redisson/Bucket/COMPAREANDSET"));
		assertTrue(metrics.containsKey("Datastore/statement/Redisson/Bucket/SIZE"));
		assertTrue(metrics.containsKey("Datastore/statement/Redisson/Bucket/TRYSET"));
		assertTrue(metrics.containsKey("Datastore/statement/Redisson/Bucket/SET"));
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
	
//	private void reportMetrics(Map<String, TracedMetricData> metrics) {
//		for(String name : metrics.keySet()) {
//			TracedMetricData data = metrics.get(name);
//			System.out.println("\tMetric: "+name);
//			System.out.println("\t\tCall Count: "+data.getCallCount());
//		}
//	}
//	
//	private void reportTrace(TraceSegment segment,int indent) {
//		StringBuffer sb = new StringBuffer();
//		for(int i=0;i<indent;i++) {
//			sb.append('\t');
//		}
//		String prefix = sb.toString();
//		System.out.println(prefix+"Segment name: " + segment.getName());
//		System.out.println(prefix+"Segment call count: " + segment.getCallCount());
//		System.out.println(prefix+"Segment class name: " + segment.getClassName());
//		System.out.println(prefix+"Segment method name: " + segment.getMethodName());
//		List<TraceSegment> children = segment.getChildren();
//		System.out.println(prefix+"Number of children: " + children.size());
//		for(TraceSegment child : children) {
//			reportTrace(child, indent+1);
//		}
//	}

	private static <V> V sync(Publisher<V> obs) {
		return Mono.from(obs).block();
	}
}

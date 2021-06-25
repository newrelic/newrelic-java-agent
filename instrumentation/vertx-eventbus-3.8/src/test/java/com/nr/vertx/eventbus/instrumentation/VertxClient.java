package com.nr.vertx.eventbus.instrumentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransactionNamePriority;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.eventbus.Message;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "io.vertx" })
public class VertxClient {

	private static final String TRANSACTION1 = "OtherTransaction/Test/Test/PublishMessage";
	private static final String MESSAGEHANDLE = "Custom/MessageHandler/handle";
	private static final String MESSAGERECEIVED = "Custom/MessageReceived";
	private static final String MESSAGEPUBLISH = "Custom/EventBusImpl/publish";


	@BeforeClass
	public static void beforeClass() {
	}


	@Test
	public void testPubSub() {
		CompletableFuture<String> f = new CompletableFuture<String>();
		Vertx vertx = Vertx.vertx();
		subscribe(f,vertx);
		publish(vertx);

		try {
			String result = f.get(2L, TimeUnit.SECONDS);
			System.out.println("Received message: "+result);
		} catch (Exception e) {
			e.printStackTrace();
		}

		Introspector introspector = InstrumentationTestRunner.getIntrospector();
		int count = introspector.getFinishedTransactionCount(2500);
		System.out.println("There are "+count+" transactions");
		assertEquals(count, 1);
		Collection<String> transactionNames = introspector.getTransactionNames();
		for(String trxName : transactionNames) {
			System.out.println("Transaction Name: "+trxName);
		}
		assertTrue("Transaction Name Not Found", transactionNames.contains(TRANSACTION1));
		Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(TRANSACTION1);
		Set<String> keys = metrics.keySet();
		assertTrue("Publish Not Found",keys.contains(MESSAGEPUBLISH));
		assertTrue("Message Handle Not Found",keys.contains(MESSAGEHANDLE));
		assertTrue("Message Received Not Found",keys.contains(MESSAGERECEIVED));		

	}



	@Trace(dispatcher=true)
	public void publish(Vertx vertx) {
		NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.CUSTOM_HIGH, true, "Test", "Test","PublishMessage");
		EventBus eb = vertx.eventBus();

		vertx.runOnContext(v -> eb.publish("news-feed", "Some news!"));
	}


	public void subscribe(CompletableFuture<String> f, Vertx vertx) {
		EventBus eb = vertx.eventBus();

		Handler<Message<String>> msgHandler = new Handler<Message<String>>() {

			@Override
			@Trace
			public void handle(Message<String> message) {
				NewRelic.getAgent().getTracedMethod().setMetricName("Custom","MessageReceived");
				f.complete(message.body().toString());

			}
		};

		eb.consumer("news-feed", msgHandler);

	}

	@Test
	public void testSendReceive() {
		
		
		VertxOptions options = new VertxOptions();
		EventBusOptions ebOptions = new EventBusOptions();
		ebOptions.setClustered(true);
		options.setEventBusOptions(ebOptions);
		
		CompletableFuture<Boolean> receiverCluster = new CompletableFuture<Boolean>();
		CompletableFuture<Boolean> messageSent = new CompletableFuture<Boolean>();
		CompletableFuture<Boolean> replySent = new CompletableFuture<Boolean>();
		
		Handler<AsyncResult<Vertx>> receiverResult = new Handler<AsyncResult<Vertx>>() {
			
			@Override
			public void handle(AsyncResult<Vertx> event) {
				if(event.succeeded()) {
					EventBus eb = event.result().eventBus();
					eb.consumer("ping-address", message -> receive(message, replySent));
					System.out.println("Created clustered instance for receiver");
					receiverCluster.complete(true);
				} else if(event.failed()) {
					System.out.println("Failed to create clustered instance for receiver");
					receiverCluster.complete(false);
				}
				
			}
		};
		Vertx.clusteredVertx(options , receiverResult);
		
		try {
			boolean receiverClustered = receiverCluster.get();
			System.out.println("receiverClustered is "+receiverClustered);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ExecutionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		Handler<AsyncResult<Vertx>> senderResult = new Handler<AsyncResult<Vertx>>() {
			
			@Override
			public void handle(AsyncResult<Vertx> event) {
				if(event.succeeded()) {
					System.out.println("Created clustered instance for sender");
					EventBus eb = event.result().eventBus();
					sendMessage(eb);
					messageSent.complete(true);
				} else if(event.failed()) {
					System.out.println("Failed to create clustered instance for sender");
					messageSent.complete(false);
				}
				
			}
		};
		Vertx.clusteredVertx(options , senderResult);
		try {
			boolean wasMessagSent = messageSent.get();
			System.out.println("Result of sending message was "+wasMessagSent);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			boolean replyWasSent = replySent.get();
			System.out.println("Result of reply sent is "+replyWasSent);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		

		Introspector introspector = InstrumentationTestRunner.getIntrospector();
		int count = introspector.getFinishedTransactionCount(2500);
		System.out.println("There are "+count+" transactions for send and receive");
		//assertEquals(count, 3);
		
		Collection<String> transactionNames = introspector.getTransactionNames();
		for(String trxName : transactionNames) {
			System.out.println("Transaction Name: "+trxName);
		}
		String transaction1 = "OtherTransaction/Test/Test/SendMessage";
		assertTrue("Send Message Transaction not found", transactionNames.contains(transaction1));
		String transaction2 = "OtherTransaction/EventBus/HandleMessage/ping-address";
		assertTrue("Send Message Transaction not found", transactionNames.contains(transaction2));
	}

	@Trace
	public void receive(Message<?> message,CompletableFuture<Boolean> replySent) {
		System.out.println("Received message: "+message);
		message.reply("Pong");
		System.out.println("Reply sent");	
		replySent.complete(true);
	}

	@Trace(dispatcher=true)
	public void sendMessage(EventBus eb) {
		NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.CUSTOM_HIGH, true, "Test", "Test","SendMessage");
		System.out.println("Sending message and waiting for reply");
		eb.request("ping-address", "ping!", reply -> {
			if (reply.succeeded()) {
				System.out.println(new Date()+": Received reply " + reply.result().body());
			} else {
				System.out.println(new Date()+": No reply");
			}
		});

	}
	

}

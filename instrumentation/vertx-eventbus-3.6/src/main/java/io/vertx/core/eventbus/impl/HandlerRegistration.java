package io.vertx.core.eventbus.impl;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.TransportType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.vertx.MessageHeaders;
import com.nr.instrumentation.vertx.NRCompletionWrapper;
import com.nr.instrumentation.vertx.NRMessageHandlerWrapper;
import com.nr.instrumentation.vertx.NRTaskWrapper;
import com.nr.instrumentation.vertx.VertxUtils;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.spi.metrics.EventBusMetrics;

@Weave
public abstract class HandlerRegistration<T> implements MessageConsumer<T>, Handler<Message<T>> {
	
	@NewField
	public Token token = null;
	
	private final String address = Weaver.callOriginal();

	@SuppressWarnings("rawtypes")
	public HandlerRegistration(Vertx vertx, EventBusMetrics metrics, EventBusImpl eventBus, String address,
			String repliedAddress, boolean localOnly,
			Handler<AsyncResult<Message<T>>> asyncResultHandler, long timeout) 
	{

	}

	public MessageConsumer<T> handler(Handler<Message<T>> handler)  {
		if(handler == null) {
			NRMessageHandlerWrapper<T> wrapper = new NRMessageHandlerWrapper<T>(handler);
			handler = wrapper;
		} else if(!(handler instanceof NRMessageHandlerWrapper)){
			NRMessageHandlerWrapper<T> wrapper = new NRMessageHandlerWrapper<T>(handler);
			handler = wrapper;
		}
		return Weaver.callOriginal();
	}


	@Trace(dispatcher=true)
	public void handle(Message<T> message) {

		NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_LOW, false, "EventBus", "HandleMessage",VertxUtils.normalize(address));
		MultiMap headers = message.headers();
		MessageHeaders msgHeaders = new MessageHeaders(headers);
		NewRelic.getAgent().getTransaction().acceptDistributedTraceHeaders(TransportType.Other, msgHeaders);
		NewRelic.getAgent().getTracedMethod().setMetricName(new String[] {"Custom","HandlerRegistration","handle"});
		NewRelic.getAgent().getTracedMethod().addCustomAttribute("Handler Address", address);
		Weaver.callOriginal();
	}

	public synchronized void completionHandler(Handler<AsyncResult<Void>> completionHandler) {
		if(completionHandler == null) {
			Token token = NewRelic.getAgent().getTransaction().getToken();
			if(token != null && token.isActive()) {
				NRCompletionWrapper<Void> wrapper = new NRCompletionWrapper<Void>(completionHandler,NewRelic.getAgent().getTransaction().getToken(),NewRelic.getAgent().getTransaction().startSegment("CompletionHandler"));
				completionHandler = wrapper;
			} else if(token != null) {
				token.expire();
				token = null;
			}
		}
		else if(!NRCompletionWrapper.class.isInstance(completionHandler)) {
			Token token = NewRelic.getAgent().getTransaction().getToken();
			if(token != null && token.isActive()) {
				NRCompletionWrapper<Void> wrapper = new NRCompletionWrapper<Void>(completionHandler,NewRelic.getAgent().getTransaction().getToken(),NewRelic.getAgent().getTransaction().startSegment("CompletionHandler"));
				completionHandler = wrapper;
			} else if(token != null) {
				token.expire();
				token = null;
			}
		}
		Weaver.callOriginal();
	}

	@Trace
	private void deliver(Handler<Message<T>> theHandler, Message<T> message, ContextInternal context) {
		NewRelic.getAgent().getTracedMethod().setMetricName(new String[] { "Custom", "HandlerRegistration","deliver"});
		NewRelic.getAgent().getTracedMethod().addCustomAttribute("Handler Address", address);
		Weaver.callOriginal();
	}

	public MessageConsumer<T> endHandler(Handler<Void> endHandler) {
		if(!(endHandler instanceof NRTaskWrapper)) {
			Token token = NewRelic.getAgent().getTransaction().getToken();
			if(token != null && token.isActive()) {
				NRTaskWrapper<Void> wrapper = new NRTaskWrapper<Void>(endHandler, token);
				endHandler = wrapper;
			} else if(token != null) {
				token.expire();
				token = null;
			}
		}
		return Weaver.callOriginal();
	}

	
	@Trace(dispatcher=true)
	public synchronized MessageConsumer<T> fetch(long amount) {
		NewRelic.getAgent().getTracedMethod().addCustomAttribute("Handler Address", address);
		
		return Weaver.callOriginal();
	}

}

package io.vertx.core.eventbus.impl;

import java.util.concurrent.ConcurrentMap;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.vertx.MessageHeaders;
import com.nr.instrumentation.vertx.NRCompletionWrapper;
import com.nr.instrumentation.vertx.NRWrappedReplyHandler;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryContext;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.impl.utils.ConcurrentCyclicSequence;

@SuppressWarnings({"rawtypes"})
@Weave(type=MatchType.BaseClass)
public abstract class EventBusImpl implements EventBus {

	protected final ConcurrentMap<String, ConcurrentCyclicSequence<HandlerHolder>> handlerMap = Weaver.callOriginal();

	@Trace(dispatcher=true)
	public <T> EventBus send(String address, Object message, DeliveryOptions options, Handler<AsyncResult<Message<T>>> replyHandler) {
		MultiMap headers = options.getHeaders();
		if(headers == null) {
			options.setHeaders(new CaseInsensitiveHeaders());
		}
		MessageHeaders msgHeaders = new MessageHeaders(options.getHeaders());
		NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(msgHeaders);
		options.setHeaders(msgHeaders.getMultimap());
		
		if (replyHandler != null) {
			Token token = NewRelic.getAgent().getTransaction().getToken();
			Segment segment = NewRelic.getAgent().getTransaction().startSegment("EventBus-Send");
			NRWrappedReplyHandler<T> wrapper = new NRWrappedReplyHandler<T>(token, segment, replyHandler);
			replyHandler = wrapper;
		}
		NewRelic.getAgent().getTracedMethod().setMetricName(new String[] {"Custom","EventBusImpl","send"});
		NewRelic.getAgent().getTracedMethod().addCustomAttribute("address", address);
		return Weaver.callOriginal();
	}

	@Trace(excludeFromTransactionTrace=true)
	protected MessageImpl createMessage(boolean send, String address, MultiMap headers, Object body, String codecName) {
		
		MessageHeaders msgHeaders;
		if(headers != null) {
			msgHeaders = new MessageHeaders(headers);
		} else {
			msgHeaders = new MessageHeaders(new CaseInsensitiveHeaders());
		}
		NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(msgHeaders);
		headers = msgHeaders.getMultimap();
		MessageImpl msg = Weaver.callOriginal();
		return msg;
	}

	@Trace(async=true)
	protected void callCompletionHandlerAsync(Handler<AsyncResult<Void>> completionHandler) {
		if(completionHandler instanceof NRCompletionWrapper) {
			NRCompletionWrapper<Void> wrapper = (NRCompletionWrapper<Void>)completionHandler;
			if(wrapper.token != null) {
				wrapper.token.linkAndExpire();
				wrapper.token = null;
			}
		}
		
		Weaver.callOriginal();
	}

	@Trace
	private <T> void deliverToHandler(MessageImpl msg, HandlerHolder<T> holder) {
		Weaver.callOriginal();
	}

	@Trace(dispatcher=true)
	public EventBus publish(String address, Object message, DeliveryOptions options) {
		MultiMap headers = options.getHeaders();
		if(headers == null) {
			options.setHeaders(new CaseInsensitiveHeaders());
		}
		MessageHeaders msgHeaders = new MessageHeaders(options.getHeaders());
		NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(msgHeaders);
		options.setHeaders(msgHeaders.getMultimap());
		
		NewRelic.getAgent().getTracedMethod().setMetricName(new String[] {"Custom","EventBusImpl","publish"});
		NewRelic.getAgent().getTracedMethod().addCustomAttribute("Address", address);
		return Weaver.callOriginal();
	}

	@Trace(dispatcher=true)
	protected <T> void sendReply(OutboundDeliveryContext<T> sendContext, MessageImpl replierMessage) {
		NewRelic.getAgent().getTracedMethod().setMetricName(new String[] {"Custom","EventBusImpl","sendReply"});
		if(replierMessage != null) {
			NewRelic.getAgent().getTracedMethod().addCustomAttribute("Replier-Address", replierMessage.address());
		}
		Weaver.callOriginal();
	}

	@Trace(async=true)
	protected <T> void sendReply(MessageImpl replyMessage, MessageImpl replierMessage, DeliveryOptions options, Handler<AsyncResult<Message<T>>> replyHandler) {
		
		MultiMap headers = options.getHeaders();
		if(headers == null) {
			options.setHeaders(new CaseInsensitiveHeaders());
		}
		MessageHeaders msgHeaders = new MessageHeaders(options.getHeaders());
		NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(msgHeaders);
		options.setHeaders(msgHeaders.getMultimap());
		if (replyHandler != null) {
			Token token = NewRelic.getAgent().getTransaction().getToken();
			Segment segment = NewRelic.getAgent().getTransaction().startSegment("EventBus-SendReply");
			NRWrappedReplyHandler<T> wrapper = new NRWrappedReplyHandler<T>(token, segment, replyHandler);
			replyHandler = wrapper;
		}
		TracedMethod traced = NewRelic.getAgent().getTracedMethod();
		
		traced.setMetricName(new String[] {"Custom","EventBusImpl","sendReply"});
		if(replyMessage != null && replyMessage.address() != null) {
			traced.addCustomAttribute("Reply-Address", replyMessage.address());
		}
		if(replierMessage != null && replierMessage.address() != null) {
			traced.addCustomAttribute("Replier-Address", replierMessage.address());
		}
		Weaver.callOriginal();
	}

	@Weave
	protected abstract static class OutboundDeliveryContext<T> implements DeliveryContext<T> {
		public final DeliveryOptions options = Weaver.callOriginal();
		
		@NewField
		public Token token = null;
		
		private OutboundDeliveryContext(MessageImpl message, DeliveryOptions options, HandlerRegistration<T> handlerRegistration, MessageImpl replierMessage) {
			
		}
	}
}

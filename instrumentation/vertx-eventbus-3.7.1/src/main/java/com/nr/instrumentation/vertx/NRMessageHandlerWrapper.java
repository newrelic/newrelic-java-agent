package com.nr.instrumentation.vertx;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransportType;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.Message;

public class NRMessageHandlerWrapper<T> implements Handler<Message<T>> {
	
	private Handler<Message<T>> delegate = null;
	
	private static boolean isTransformed = false;
	
	
	public NRMessageHandlerWrapper(Handler<Message<T>> d) {
		delegate = d;
		if(!isTransformed) {
			isTransformed = true;
			AgentBridge.instrumentation.retransformUninstrumentedClass(getClass());
		}
	}

	@Override
	@Trace(dispatcher=true)
	public void handle(Message<T> event) {
		NewRelic.getAgent().getTracedMethod().setMetricName(new String[] {"Custom","MessageHandler","handle"});
		if(event != null) {
			MultiMap headers = event.headers();
			if(headers != null) {
				MessageHeaders msgHeaders = new MessageHeaders(headers);
				NewRelic.getAgent().getTransaction().acceptDistributedTraceHeaders(TransportType.Other, msgHeaders);
			}
		}
		if(delegate != null) {
			delegate.handle(event);
		}
	}

}

package io.vertx.core.eventbus.impl.clustered;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransportType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.vertx.MessageHeaders;

import io.vertx.core.eventbus.impl.MessageImpl;

@Weave
public abstract class ClusteredMessage<U, V> extends MessageImpl<U, V> {
	
	@SuppressWarnings("unused")
	private void decodeHeaders() {
		Weaver.callOriginal();
		if(headers() != null) {
			MessageHeaders msgHeaders = new MessageHeaders(headers());
			NewRelic.getAgent().getTransaction().acceptDistributedTraceHeaders(TransportType.Other, msgHeaders);
		}
	}
}

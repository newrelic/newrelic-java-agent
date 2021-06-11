package io.vertx.core.eventbus.impl.clustered;

import java.net.URI;
import java.util.HashMap;

import com.newrelic.api.agent.GenericParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.vertx.MessageHeaders;
import com.nr.instrumentation.vertx.VertxUtils;

import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.impl.EventBusImpl;
import io.vertx.core.eventbus.impl.MessageImpl;
import io.vertx.core.net.impl.ServerID;
import io.vertx.core.spi.cluster.ChoosableIterable;

@Weave
public abstract class ClusteredEventBus extends EventBusImpl {
	
	private ServerID serverID = Weaver.callOriginal();
	
	@Trace(dispatcher=true)
	protected <T> void sendOrPub(OutboundDeliveryContext<T> sendContext) {
		if (!sendContext.options.isLocalOnly()) {
			Token token = NewRelic.getAgent().getTransaction().getToken();
			if(token != null && token.isActive()) {
				sendContext.token = token;
			} else if(token != null) {
				token.expire();
				token = null;
			}
		}
		Weaver.callOriginal();
	}

	
	@Trace(async=true)
	private <T> void sendToSubs(ChoosableIterable<ClusterNodeInfo> subs, OutboundDeliveryContext<T> sendContext) {
		if(sendContext.token != null) {
			sendContext.token.linkAndExpire();
			sendContext.token = null;
		}
		
		Weaver.callOriginal();
	}
	
	@Trace
	private <T> void clusteredSendReply(ServerID replyDest, OutboundDeliveryContext<T> sendContext) {
		if(!replyDest.equals(serverID)) {
			Message<?> message = sendContext.message();
			MultiMap headers = message.headers();
			MessageHeaders msgHeaders = new MessageHeaders(headers);
			NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(msgHeaders);
		}
		Weaver.callOriginal();
	}
	
	@SuppressWarnings("rawtypes")
	@Trace(dispatcher=true)
	private void sendRemote(ServerID theServerID, MessageImpl message) {
		TracedMethod traced = NewRelic.getAgent().getTracedMethod();
		String address = message.address();
		HashMap<String, Object> attributes = new HashMap<String, Object>();
		String host = theServerID.host;
		int port = theServerID.port;
		attributes.put("Address", address);
		attributes.put("Server-Host", host);
		attributes.put("Server-Port", port);
		
		
		traced.addCustomAttributes(attributes);
		address = VertxUtils.normalize(address);
		URI uri = URI.create("vertx://"+host+":"+port+"/"+address);
		GenericParameters params = GenericParameters.library("Vertx").uri(uri).procedure("sendRemote").build();
		traced.reportAsExternal(params);
		Weaver.callOriginal();
	}
}
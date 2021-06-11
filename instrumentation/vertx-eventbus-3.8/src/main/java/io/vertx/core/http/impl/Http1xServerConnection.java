package io.vertx.core.http.impl;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.vertx.HttpRequestWrapper;

import io.netty.handler.codec.http.LastHttpContent;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;

@Weave
public abstract class Http1xServerConnection {

	
	public HttpServerConnection handler(Handler<HttpServerRequest> handler) {
		if(!(handler instanceof HttpRequestWrapper)) {
			handler = new HttpRequestWrapper(handler);
		}
		return Weaver.callOriginal();
	}
	
	@Trace(dispatcher=true)
	public void handleMessage(Object msg) {
		String objectType = "Unknown";
		String classname = msg.getClass().getSimpleName();
		if(classname != null && !classname.isEmpty()) {
			objectType = classname;
		} else  if (msg == LastHttpContent.EMPTY_LAST_CONTENT){
			objectType = "LastContent";
		}
		NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Http1xServerConnection","handleMessage",objectType);
		Weaver.callOriginal();
	}
}

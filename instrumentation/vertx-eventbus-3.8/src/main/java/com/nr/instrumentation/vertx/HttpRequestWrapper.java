package com.nr.instrumentation.vertx;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.Transaction;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;

public class HttpRequestWrapper implements Handler<HttpServerRequest> {

	private static boolean isTransformed = false;
	private Handler<HttpServerRequest> delegate = null;

	public HttpRequestWrapper(Handler<HttpServerRequest> h) {
		delegate = h;
		if (!isTransformed) {
			isTransformed = true;
			AgentBridge.instrumentation.retransformUninstrumentedClass(getClass());
		} 
	}

	@Trace(dispatcher = true)
	public void handle(HttpServerRequest event) {
		Transaction transaction = NewRelic.getAgent().getTransaction();
		if (!transaction.isWebTransaction()) {
			transaction.convertToWebTransaction();
			NRVertxExtendedRequest req = new NRVertxExtendedRequest(event);
			transaction.setWebRequest(req);
		} 
		if (this.delegate != null)
			this.delegate.handle(event); 
	}
}

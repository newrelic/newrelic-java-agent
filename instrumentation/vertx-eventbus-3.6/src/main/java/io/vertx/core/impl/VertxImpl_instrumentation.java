package io.vertx.core.impl;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.vertx.NRCompletionWrapper;
import com.nr.instrumentation.vertx.NRFutureWrapper;

import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;

@Weave(originalName="io.vertx.core.impl.VertxImpl")
public abstract class VertxImpl_instrumentation {

	@Trace(dispatcher=true)
	public void runOnContext(Handler<Void> task) {
		Weaver.callOriginal();
	}
	
	@Trace
	public <T> void executeBlocking(Handler<Future<T>> blockingCodeHandler, boolean ordered,Handler<AsyncResult<T>> asyncResultHandler) {
		Token token = NewRelic.getAgent().getTransaction().getToken();
		NRFutureWrapper<T> wrapper = new NRFutureWrapper<T>(blockingCodeHandler, token);
		NRCompletionWrapper<T> resultWrapper = new NRCompletionWrapper<T>(asyncResultHandler,token,NewRelic.getAgent().getTransaction().startSegment("CompletionHandler"));
		asyncResultHandler = resultWrapper;
		blockingCodeHandler = wrapper;
		
		Weaver.callOriginal();
	}
	
	public void deployVerticle(String name, DeploymentOptions options, Handler<AsyncResult<String>> completionHandler) {
		Weaver.callOriginal();
	}

	@Weave(originalName="io.vertx.core.impl.VertxImpl$InternalTimerHandler")
	private static class InternalTimerHandler_instrumentation {
		
	    private final Handler<Long> handler = Weaver.callOriginal();

		@Trace
		public void handle(Void v) {
			String handlerClass = handler.getClass().getName();
			if(!ignore(handlerClass)) {
			} else {
				NewRelic.getAgent().getTransaction().ignore();
			}
			Weaver.callOriginal();
		}
		
		private boolean ignore(String classname) {
			if(classname.startsWith("io.vertx.core.http.impl.ConnectionManager")) return true;
			if(classname.startsWith("io.vertx.core.impl.HAManager")) return true;
			return false;
		}
	}
	
}

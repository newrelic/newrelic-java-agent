package io.vertx.core.impl;

import java.util.concurrent.Executor;
import java.util.logging.Level;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.vertx.NRTaskWrapper;

import io.netty.util.concurrent.Promise;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.spi.metrics.PoolMetrics;

@SuppressWarnings("rawtypes")
@Weave(type=MatchType.BaseClass)
abstract class ContextImpl {
	
	@Trace
	<T> void executeBlocking(Handler<Promise<T>> blockingCodeHandler,
		      Handler<AsyncResult<T>> resultHandler,
		      Executor exec, TaskQueue queue, PoolMetrics metrics) {
		Token token = NewRelic.getAgent().getTransaction().getToken();
		if(!NRTaskWrapper.class.isInstance(blockingCodeHandler)) {
			NRTaskWrapper<Promise<T>> wrapper1 = new NRTaskWrapper<Promise<T>>(blockingCodeHandler, token);
			wrapper1.name = "CodeHandler";
			blockingCodeHandler = wrapper1;
		}
		if(!NRTaskWrapper.class.isInstance(resultHandler)) {
			NRTaskWrapper<AsyncResult<T>>  wrapper2 = new NRTaskWrapper<AsyncResult<T>>(resultHandler, token);
			wrapper2.name = "ReplyHandler";
			resultHandler = wrapper2;
		}
		Weaver.callOriginal();
	}

	@Trace(excludeFromTransactionTrace=true)
	public void runOnContext(Handler<Void> hTask) {
		Weaver.callOriginal();
	}
	
	
	@Trace(async=true,excludeFromTransactionTrace=true)
	public final <T> void executeFromIO(T value, Handler<T> task) { 
		if(NRTaskWrapper.class.isInstance(task)) {
			NRTaskWrapper<T> wrapper = (NRTaskWrapper<T>)task;
			Token token = wrapper.getToken();
			if(token != null) {
				token.link();
			}
		}
		Weaver.callOriginal();
	 }
	
	@Trace(async=true,excludeFromTransactionTrace=true)
	<T> boolean executeTask(T arg, Handler<T> hTask) {
		if(NRTaskWrapper.class.isInstance(hTask)) {
			NRTaskWrapper<T> wrapper = (NRTaskWrapper<T>)hTask;
			Token token = wrapper.getToken();
			if(token != null) {
				token.link();
			}
		}
		return Weaver.callOriginal();
	}

	@Trace
	void executeAsync(Handler<Void> task) {
		Exception e = new Exception("call to executeAsync");
		NewRelic.getAgent().getLogger().log(Level.FINE, e, "Enter {0}.executeAsync({1})",getClass().getName(),task);
		if(!(task instanceof NRTaskWrapper) ) {
			Token t = NewRelic.getAgent().getTransaction().getToken();
			if(t != null && t.isActive()) {
				NRTaskWrapper<Void> wrapper = new NRTaskWrapper<Void>(task, t);
				task = wrapper;
			} else if(t != null) {
				t.expire();
				t = null;
			}
		}
		Weaver.callOriginal();
	}
	
	@Trace(async=true,excludeFromTransactionTrace=true)
    private static void lambda$null$0(Handler handler, AsyncResult result, Void v) {
		if(NRTaskWrapper.class.isInstance(handler)) {
			NRTaskWrapper<?> wrapper = (NRTaskWrapper<?>)handler;
			
			wrapper.linkAndExpireToken();
			if(wrapper.name != null) {
				NewRelic.getAgent().getTracedMethod().setMetricName(new String[] {"Custom","Context","lamdba-null",wrapper.name});
			}
		}
        Weaver.callOriginal();
    }
}

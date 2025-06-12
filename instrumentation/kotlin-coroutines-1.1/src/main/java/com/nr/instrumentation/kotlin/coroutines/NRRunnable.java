package com.nr.instrumentation.kotlin.coroutines;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;

public class NRRunnable implements Runnable {
	
	
	private static boolean isTranformed = false;
	private Runnable delegate = null;
	private Token token = null;
	
	public void expireAndNullToken() {
		token.expire();
		token = null;
	}
	
	public NRRunnable(Runnable d, Token t) {
		token = t;
		delegate = d;
		if(!isTranformed) {
			AgentBridge.instrumentation.retransformUninstrumentedClass(getClass());
			isTranformed = true;
		}
	}

	@Override
	@Trace(async=true)
	public void run() {
		NewRelic.getAgent().getTracedMethod().setMetricName("Custom","DispatchedTask",delegate.getClass().getSimpleName(),"run");
		if(token != null) {
			token.linkAndExpire();
			token = null;
		}
		delegate.run();
	}

}

package com.newrelic.instrumentation.kotlin.coroutines;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;

public class NRRunnable implements Runnable {
	
	private Runnable delegate = null;
	private Token token = null;
	private static boolean isTransformed = false;
	
	public NRRunnable(Runnable r,Token t) {
		if(!isTransformed) {
			AgentBridge.instrumentation.retransformUninstrumentedClass(getClass());
			isTransformed = true;
		}
		delegate = r;
		token = t;
	}

	@Override
	@Trace(async = true)
	public void run() {
		if(token != null) {
			token.linkAndExpire();
			token = null;
		}
		if(delegate != null) {
			delegate.run();
		}
	}

}

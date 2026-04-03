package com.nr.instrumentation.reactor;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;

public class NRRunnableWrapper implements Runnable {
	
	private Runnable delegate = null;
	
	private Token token = null;
	private static boolean isTransformed = false;
	
	public NRRunnableWrapper(Runnable r, Token t) {
		delegate = r;
		token = t;
		if(!isTransformed) {
			isTransformed = true;
			AgentBridge.instrumentation.retransformUninstrumentedClass(getClass());
		}
	}

	@Override
	@Trace(async=true)
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

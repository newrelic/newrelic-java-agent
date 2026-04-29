package com.nr.instrumentation.reactor;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;

public class ReactorUtils {

	public static NRRunnableWrapper getRunnableWrapper(Runnable r) {
		if(r instanceof NRRunnableWrapper) {return null;}

		Token currentToken = NewRelic.getAgent().getTransaction().getToken();
		if(currentToken != null) {
			if(currentToken.isActive()) {
				return new NRRunnableWrapper(r, currentToken);
			} else {
				currentToken.expire();
				currentToken = null;
				return null;
			}
		}

		return null;
	}
	
}

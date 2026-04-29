package com.nr.instrumentation.reactor;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;

import java.util.HashSet;
import java.util.Set;

public class ReactorUtils {

	/*
		Discovered that MonoDelay$MonoDelayRunnable can be dispatched but never run which leads
		to unexpired tokens.
		if others are discovered then add them to the list
	 */
	private static final Set<String> IGNORED_RUNNABLES = new HashSet<>();

	static {
		IGNORED_RUNNABLES.add("reactor.core.publisher.MonoDelay$MonoDelayRunnable");
	}

	public static NRRunnableWrapper getRunnableWrapper(Runnable r) {
		if(r instanceof NRRunnableWrapper) return null;
		if(IGNORED_RUNNABLES.contains(r.getClass().getName())) return null;

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

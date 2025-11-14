package com.newrelic.instrumentation.micronaut.http.client4;

import java.util.function.Consumer;
import java.util.logging.Level;

import org.reactivestreams.Subscription;

import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Transaction;

public class ReactorListener implements Runnable, Consumer<Subscription> {
	
	private Segment segment = null;
	private Transaction txn = null;
	private HttpParameters params = null;
	
	public ReactorListener(Transaction t, HttpParameters p) {
		txn = t;
		params = p;
	}

	@Override
	public void run() {
		try {
			if(segment != null) {
				segment.end();
				segment = null;
			}
		} catch (Exception e) {
			NewRelic.getAgent().getLogger().log(Level.FINEST, e, "An error occurred while trying to end segment in {0}",getClass().getName());
		}
	}



	@Override
	public void accept(Subscription t) {
		if(txn != null && segment == null) {
			if(params != null) {
				String proc = params.getProcedure();
				segment = txn.startSegment("MicronautClient/"+proc);
				segment.reportAsExternal(params);
			}
		}
	}


}

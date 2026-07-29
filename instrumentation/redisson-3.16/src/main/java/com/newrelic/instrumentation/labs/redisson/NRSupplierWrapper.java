package com.newrelic.instrumentation.labs.redisson;

import java.util.function.Supplier;

import org.redisson.api.RFuture;

import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;

public class NRSupplierWrapper<R> implements Supplier<RFuture<R>> {

	private Supplier<RFuture<R>> delegate = null;
	private String cmdName = null;
	private String subName = null;
	private DatastoreParameters params = null;
	
	
	
	public NRSupplierWrapper(Supplier<RFuture<R>> supplier, String cmd, String sub, DatastoreParameters p) {
		delegate = supplier;
		cmdName = cmd;
		subName = sub;
		params = p;
	}

	@Override
	public RFuture<R> get() {
		RFuture<R> future = delegate.get();
		Segment segment = NewRelic.getAgent().getTransaction().startSegment(cmdName + "-" + subName);
		NRBiConsumer<R> action = new NRBiConsumer<R>(segment, params );

		return (RFuture<R>) future.whenComplete(action);
	}

}

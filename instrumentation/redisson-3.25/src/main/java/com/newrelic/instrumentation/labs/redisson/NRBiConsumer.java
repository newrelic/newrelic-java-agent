package com.newrelic.instrumentation.labs.redisson;

import java.util.function.BiConsumer;

import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;

public class NRBiConsumer<T> implements BiConsumer<T, Throwable> {
	
	private Segment segment = null;
	private ExternalParameters params = null;
	
	

	public NRBiConsumer(Segment segment, ExternalParameters params) {
		super();
		this.segment = segment;
		this.params = params;
	}



	@Override
	@Trace
	public void accept(T t, Throwable u) {
		if(u != null) {
			NewRelic.noticeError(u);
		}
		if(segment != null) {
			if(params != null) {
				segment.reportAsExternal(params);
			}
			segment.end();
			segment = null;
		}
	}

}

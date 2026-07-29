package com.newrelic.instrumentation.labs.redisson;

import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

public class NRFutureListener<V> implements FutureListener<V> {
	
	private Segment segment = null;
	private ExternalParameters params = null;

	public NRFutureListener(Segment s, ExternalParameters p) {
		segment = s;
		params = p;
	}	

	@Override
	public void operationComplete(Future<V> future) throws Exception {
		if(future.isDone()) {
			if(future.isCancelled()) {
				if(segment != null) {
					segment.ignore();
					segment = null;
				}
			} else if(future.isSuccess()) {
				if(segment != null) {
					if(params != null) {
						segment.reportAsExternal(params);
					}
					segment.end();
					segment = null;
				}
			} else {
				// assume error
				Throwable t = future.cause();
				if(t != null) {
					NewRelic.noticeError(t);
				}
				segment.ignore();
				segment = null;
			}
		}
		
	}

}

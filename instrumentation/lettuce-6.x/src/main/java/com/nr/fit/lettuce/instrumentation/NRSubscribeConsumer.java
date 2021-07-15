package com.nr.fit.lettuce.instrumentation;

import java.util.function.Consumer;

import org.reactivestreams.Subscription;

public class NRSubscribeConsumer implements Consumer<Subscription> {
	
	private NRHolder holder = null;
	
	public NRSubscribeConsumer(NRHolder h) {
		holder = h;
	}

	@Override
	public void accept(Subscription t) {
		if(holder != null) {
			holder.startSegment();
		}
	}

}

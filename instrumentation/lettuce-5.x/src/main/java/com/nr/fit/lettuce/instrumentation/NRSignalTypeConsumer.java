package com.nr.fit.lettuce.instrumentation;

import java.util.function.Consumer;

import reactor.core.publisher.SignalType;

public class NRSignalTypeConsumer implements Consumer<SignalType> {

	private NRHolder holder = null;
	
	public NRSignalTypeConsumer(NRHolder h) {
		holder = h;
	}

	@Override
	public void accept(SignalType t) {
		if(holder != null && !holder.hasEnded()) {
			holder.end();
		}
	}

}

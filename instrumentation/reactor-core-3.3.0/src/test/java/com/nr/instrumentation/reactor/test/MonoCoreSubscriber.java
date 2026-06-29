package com.nr.instrumentation.reactor.test;

import org.reactivestreams.Subscription;

import com.newrelic.api.agent.Trace;

import reactor.core.CoreSubscriber;

public class MonoCoreSubscriber implements CoreSubscriber<String> {

	@Override
	@Trace
	public void onNext(String t) {
		System.out.println("Received string for onNext: " + t);
	}

	@Override
	@Trace
	public void onError(Throwable t) {
		System.out.println("Received error for onError: " + t);
	}

	@Override
	@Trace
	public void onComplete() {
		System.out.println("Mono has completed");
	}

	@Override
	@Trace
	public void onSubscribe(Subscription var1) {
		System.out.println("Mono was subscribed to by : " + var1);
	}

}

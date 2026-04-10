package com.nr.instrumentation.reactor.test;

import com.newrelic.api.agent.Trace;
import org.reactivestreams.Subscription;

import java.util.function.Consumer;

public class SubscriptionConsumer implements Consumer<Subscription> {

    @Override
    @Trace
    public void accept(Subscription subscription) {
        pause();

    }

    private void pause() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
    }
}

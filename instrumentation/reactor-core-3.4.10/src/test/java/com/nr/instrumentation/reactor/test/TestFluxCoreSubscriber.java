package com.nr.instrumentation.reactor.test;

import com.newrelic.api.agent.Trace;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;

import java.util.ArrayList;
import java.util.List;

public class TestFluxCoreSubscriber implements CoreSubscriber<String> {

    private AwaitMany await = null;
    List<String> result = null;
    private int numberOfItems  = 0;
    public TestFluxCoreSubscriber(AwaitMany a, int numberOfItems) {
        result = new ArrayList<>();
        await = a;
        this.numberOfItems = numberOfItems;
    }

    @Trace
    public void onNext(String t) {
        System.out.println("call to onNext with string: " + t);
        result.add(t);
    }

    @Override
    @Trace
    public void onError(Throwable t) {
        System.out.println("Object has error: " + t.getMessage());
        if (await != null) {
            List<String> result = new ArrayList<>();
            result.add(t.getMessage());
            await.done(result);
        }

    }

    @Override
    @Trace
    public void onComplete() {
        await.done(result);
        System.out.println("Object has completed");
    }

    @Override
    @Trace
    public void onSubscribe(Subscription s) {
        s.request(numberOfItems);
    }

}

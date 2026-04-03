package com.nr.instrumentation.reactor.test;

import com.newrelic.api.agent.Trace;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;

public class TestMonoCoreSubscriber implements CoreSubscriber<String> {

    private AwaitSingle await = null;
    String result = null;

    public TestMonoCoreSubscriber(AwaitSingle a) {
        await = a;
    }

    @Override
    @Trace
    public void onNext(String t) {
        System.out.println("call to onNext with string: " + t);
        result = t;
    }

    @Override
    @Trace
    public void onError(Throwable t) {
        System.out.println("Object has error: "+t.getMessage());
        if(await != null) {
            await.setResult(t.getMessage());
        }

    }

    @Override
    @Trace
    public void onComplete() {
        if(await != null) {
            synchronized(await) {
                await.setResult(result);
            }
        }
        System.out.println("Object has completed");
    }

    @Override
    @Trace
    public void onSubscribe(Subscription s) {
        s.request(1);
    }

}

package com.example;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave
public class Order {
    public void method() {
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("one");
        try {
            Thread.sleep(500L);
        } catch (InterruptedException e) {
            // do nothing
        }
        System.out.println("Order1 transaction = " + NewRelic.getAgent().getTransaction().getClass());
        segment.end();
        Weaver.callOriginal();
    }
}
package com.agent.instrumentation.awsjavasdk2.services.s3;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.function.BiConsumer;

public class ResultWrapper<T, U> implements BiConsumer<T, U> {
    private Segment segment;

    public ResultWrapper(Segment segment) {
        this.segment = segment;
    }

    @Override
    public void accept(T t, U u) {
        try {
            segment.end();
        } catch (Throwable t1) {
            AgentBridge.instrumentation.noticeInstrumentationError(t1, Weaver.getImplementationTitle());
        }
    }
}

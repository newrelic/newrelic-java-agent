package com.newrelic.instrumentation.labs.ktor.client;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import org.jetbrains.annotations.NotNull;

public class NRContinuationWrapper<T> implements Continuation<T> {

    private final Continuation<T> delegate;
    private static boolean isTransformed = false;
    private final Segment segment;

    protected NRContinuationWrapper(Continuation<T> delegate, HttpParameters httpParameters) {
        this.delegate = delegate;
        segment = NewRelic.getAgent().getTransaction().startSegment("Ktor-HttpRequest");
        if(httpParameters != null) {
            segment.reportAsExternal(httpParameters);
        }
        if(!isTransformed) {
            isTransformed = true;
            AgentBridge.instrumentation.retransformUninstrumentedClass(getClass());
        }
    }

    @Override
    @Trace
    public void resumeWith(@NotNull Object o) {
        if(segment != null) {
            segment.end();
        }
        delegate.resumeWith(o);
    }

    @Override
    public @NotNull CoroutineContext getContext() {
        return delegate.getContext();
    }
}

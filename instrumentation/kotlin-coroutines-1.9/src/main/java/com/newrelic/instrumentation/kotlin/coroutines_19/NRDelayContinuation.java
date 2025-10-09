package com.newrelic.instrumentation.kotlin.coroutines_19;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.CancellableContinuation;
import org.jetbrains.annotations.NotNull;

public class NRDelayContinuation<T> implements Continuation<T> {

    private final Continuation<T> delegate;
    private final Segment segment;

    public NRDelayContinuation(Continuation<T> delegate, String type) {
        this.delegate = delegate;
        String name = Utils.getContinuationString(delegate);
        segment = NewRelic.getAgent().getTransaction().startSegment(type);
        segment.addCustomAttribute("Continuation", name);
        boolean isCancellable = delegate instanceof CancellableContinuation;
        segment.addCustomAttribute("isCancellable", isCancellable);
    }

    @Override
    public void resumeWith(@NotNull Object o) {
        if(segment != null) {
            segment.end();
        }
        if(delegate != null) {
            delegate.resumeWith(o);
        }
    }

    @Override
    public @NotNull CoroutineContext getContext() {
        return delegate.getContext();
    }
}

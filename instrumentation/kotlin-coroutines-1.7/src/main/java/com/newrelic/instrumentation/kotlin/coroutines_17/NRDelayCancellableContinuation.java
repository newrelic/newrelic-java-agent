package com.newrelic.instrumentation.kotlin.coroutines_17;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import kotlin.Unit;
import kotlin.coroutines.CoroutineContext;
import kotlin.jvm.functions.Function1;
import kotlinx.coroutines.CancellableContinuation;
import kotlinx.coroutines.CoroutineDispatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/*
 *  Used to wrap the tracking of a call to the Coroutine delay function if tracking is enabled
 *  Will report the delay as a segment
 */
public class NRDelayCancellableContinuation<T> implements CancellableContinuation<T> {

    private final CancellableContinuation<T> delegate;
    private Segment segment;

    public NRDelayCancellableContinuation(CancellableContinuation<T> delegate, String type) {
        this.delegate = delegate;
        String name = Utils.getContinuationString(delegate);
        segment = NewRelic.getAgent().getTransaction().startSegment(type);
        segment.addCustomAttribute("CancellableContinuation", name);
    }

    @Override
    public void resumeWith(@NotNull Object o) {
        if(segment != null) {
            segment.end();
            segment = null;
        }
        if(delegate != null) {
            delegate.resumeWith(o);
        }
    }

    @Override
    public @NotNull CoroutineContext getContext() {
        return delegate.getContext();
    }

    @Override
    public boolean isActive() {
        return delegate.isActive();
    }

    @Override
    public boolean isCompleted() {
        return delegate.isCompleted();
    }

    @Override
    public boolean isCancelled() {
        return delegate.isCancelled();
    }

    @Override
    public @Nullable Object tryResume(T t, @Nullable Object o) {
        if(segment != null) {
            segment.end();
            segment = null;
        }
        return delegate.tryResume(t,o);
    }

    @Override
    public @Nullable Object tryResume(T t, @Nullable Object o, @Nullable Function1<? super Throwable, Unit> function1) {
        if(segment != null) {
            segment.end();
            segment = null;
        }
        return delegate.tryResume(t,o, function1);
    }

    @Override
    public @Nullable Object tryResumeWithException(@NotNull Throwable throwable) {
        NewRelic.noticeError(throwable);
        if(segment != null) {
            segment.end();
            segment = null;
        }
        return delegate.tryResumeWithException(throwable);
    }

    @Override
    public void completeResume(@NotNull Object o) {
        if(segment != null) {
            segment.end();
            segment = null;
        }
        delegate.completeResume(o);
    }

    @Override
    public void initCancellability() {
        delegate.initCancellability();
    }

    @Override
    public boolean cancel(@Nullable Throwable throwable) {
        if(segment != null) {
            segment.end();
            segment = null;
        }
        return delegate.cancel(throwable);
    }

    @Override
    public void invokeOnCancellation(@NotNull Function1<? super Throwable, Unit> function1) {
        if(segment != null) {
            segment.end();
            segment = null;
        }
        delegate.invokeOnCancellation(function1);
    }

    @Override
    public void resumeUndispatched(@NotNull CoroutineDispatcher coroutineDispatcher, T t) {
        if(segment != null) {
            segment.end();
            segment = null;
        }
        delegate.resumeUndispatched(coroutineDispatcher, t);
    }

    @Override
    public void resumeUndispatchedWithException(@NotNull CoroutineDispatcher coroutineDispatcher, @NotNull Throwable throwable) {
        if(segment != null) {
            segment.end();
            segment = null;
        }
        delegate.resumeUndispatchedWithException(coroutineDispatcher, throwable);
    }

    @Override
    public void resume(T t, @Nullable Function1<? super Throwable, Unit> function1) {
        if(segment != null) {
            segment.end();
            segment = null;
        }
        delegate.resume(t,function1);
    }
}

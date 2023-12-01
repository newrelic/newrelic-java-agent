/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.bridge.NoOpTracedMethod;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.api.agent.AttributeHolder;
import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.OutboundHeaders;
import com.newrelic.api.agent.Transaction;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class Segment implements com.newrelic.agent.bridge.TracedActivity, AttributeHolder {
    private volatile Tracer underlyingTracer;
    private volatile Tracer parent;
    private volatile WeakRefTransaction weakRefTransaction;

    private final long parentInitialExclusiveDuration;
    private final AtomicBoolean isFinished = new AtomicBoolean(false);
    private final String initiatingThread;

    public static final String UNNAMED_SEGMENT = "Unnamed Segment";
    public static final String START_THREAD = "start_thread";
    public static final String END_THREAD = "end_thread";

    /**
     * Construct a new {@link Segment}
     *
     * @param parent Parent Tracer of this {@link Segment}. Must not be null.
     * @param tracer Underlying Tracer supporting this {@link Segment}. Must not be null.
     */
    public Segment(Tracer parent, Tracer tracer) {
        this.initiatingThread = Thread.currentThread().getName();
        this.parent = parent;
        this.underlyingTracer = tracer;
        this.weakRefTransaction = new WeakRefTransaction(parent.getTransactionActivity().getTransaction());
        this.parentInitialExclusiveDuration = parent.getExclusiveDuration();
    }

    public Transaction getTransaction() {
        return weakRefTransaction;
    }

    @Override
    public void ignore() {
        ignoreIfUnfinished();
    }

    @Override
    public void reportAsExternal(ExternalParameters externalParameters) {
        Tracer tracer = underlyingTracer;
        if (tracer != null) {
            tracer.reportAsExternal(externalParameters);
        }
    }

    @Override
    public void setMetricName(String... metricNameParts) {
        Tracer tracer = underlyingTracer;
        if (tracer != null) {
            tracer.setMetricName(metricNameParts);
        }
    }

    @Override
    public void addOutboundRequestHeaders(OutboundHeaders outboundHeaders) {
        Tracer tracer = underlyingTracer;
        if (tracer != null) {
            tracer.addOutboundRequestHeaders(outboundHeaders);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TracedMethod getTracedMethod() {
        if (underlyingTracer == null) {
            return NoOpTracedMethod.INSTANCE;
        }
        return underlyingTracer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAsyncThreadName(String threadName) {
    }

    public Tracer getParent() {
        return parent;
    }

    public Tracer getTracer() {
        return underlyingTracer;
    }

    public long getParentInitialExclusiveDuration() {
        return this.parentInitialExclusiveDuration;
    }

    public boolean isFinished() {
        return isFinished.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void ignoreIfUnfinished() {
        if (!isFinished.getAndSet(true)) {
            Tracer tracer = parent;
            if (tracer != null) {
                tracer.getTransactionActivity().getTransaction().ignoreSegmentIfUnfinished(this);
            }

            // Remove references to underlying and parent tracer to prevent GC issues
            underlyingTracer = null;
            parent = null;
            weakRefTransaction = null;
        }
    }

    @Override
    public void finish() {
        finish(null, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void end() {
        finish(null, false);
    }

    @Override
    public void endAsync() {
        finish(null, true);
    }

    @Override
    public void addCustomAttribute(String key, Number value) {
        if (underlyingTracer != null) {
            underlyingTracer.addCustomAttribute(key, value);
        }
    }

    @Override
    public void addCustomAttribute(String key, String value) {
        if (underlyingTracer != null) {
            underlyingTracer.addCustomAttribute(key, value);
        }
    }

    @Override
    public void addCustomAttribute(String key, boolean value) {
        if (underlyingTracer != null) {
            underlyingTracer.addCustomAttribute(key, value);
        }
    }

    @Override
    public void addCustomAttributes(Map<String, Object> attributes) {
        if (underlyingTracer != null) {
            underlyingTracer.addCustomAttributes(attributes);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finish(final Throwable t) {
        finish(t, false);
    }

    private void finish(final Throwable t, boolean async) {
        if (!isFinished.getAndSet(true)) {

            markFinishTime();

            final Tracer tracer = parent;
            final Segment segment = this;
            final String endThreadName = Thread.currentThread().getName();

            if (tracer != null) {
                Runnable expireSegmentRunnable = new Runnable() {
                    @Override
                    public void run() {
                        tracer.getTransactionActivity()
                                .getTransaction()
                                .finishSegment(segment, t, parent, endThreadName);

                        // Remove references to underlying and parent tracer to prevent GC issues
                        underlyingTracer = null;
                        parent = null;
                        weakRefTransaction = null;
                    }
                };

                if (async) {
                    ServiceFactory.getExpirationService().expireSegment(expireSegmentRunnable);
                } else {
                    ServiceFactory.getExpirationService().expireSegmentInline(expireSegmentRunnable);
                }
            }

        }
    }

    public String getInitiatingThread() {
        return initiatingThread;
    }

    public void setTruncated() {
        Tracer tracer = underlyingTracer;
        if (tracer != null) {
            tracer.setMetricNameFormatInfo(tracer.getMetricName(), "Truncated/" + tracer.getMetricName(),
                    tracer.getTransactionSegmentUri());
        }
    }

    private void markFinishTime() {
        Tracer underlyingTracer = this.underlyingTracer;
        if (underlyingTracer != null) {
            underlyingTracer.markFinishTime();
        }
    }
}

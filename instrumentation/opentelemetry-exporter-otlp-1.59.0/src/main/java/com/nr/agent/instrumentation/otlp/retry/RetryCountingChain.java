/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.otlp.retry;

import com.newrelic.api.agent.NewRelic;
import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * An {@link Interceptor.Chain} that counts calls to {@link #proceed(Request)}. RetryInterceptor
 * calls proceed() exactly once per attempt in its retry loop, so the first call is the initial
 * attempt and every call after it is a retry.
 */
public class RetryCountingChain implements Interceptor.Chain {
    private static final String EXPORT_RETRY_METRIC = "Supportability/Metrics/Java/OpenTelemetryBridge/export/retry";

    private final Interceptor.Chain delegate;
    private final AtomicInteger attempts;

    public RetryCountingChain(Interceptor.Chain delegate) {
        this(delegate, new AtomicInteger());
    }

    private RetryCountingChain(Interceptor.Chain delegate, AtomicInteger attempts) {
        this.delegate = delegate;
        this.attempts = attempts;
    }

    @Override
    public Response proceed(Request request) throws IOException {
        // The first proceed() is the initial attempt; every one after it is a retry.
        if (attempts.getAndIncrement() > 0) {
            try {
                NewRelic.incrementCounter(EXPORT_RETRY_METRIC);
            } catch (Throwable t) {
                NewRelic.getAgent().getLogger().log(Level.FINER, t, "Unable to record OTLP export retry metric");
            }
        }
        return delegate.proceed(request);
    }

    @Override
    public Request request() {
        return delegate.request();
    }

    @Override
    public Connection connection() {
        return delegate.connection();
    }

    @Override
    public Call call() {
        return delegate.call();
    }

    @Override
    public int connectTimeoutMillis() {
        return delegate.connectTimeoutMillis();
    }

    @Override
    public Interceptor.Chain withConnectTimeout(int timeout, TimeUnit unit) {
        // Re-wrap the chain returned by the delegate, sharing the same attempt counter, so the
        // retry count survives if the SDK ever adjusts timeouts mid-chain.
        return new RetryCountingChain(delegate.withConnectTimeout(timeout, unit), attempts);
    }

    @Override
    public int readTimeoutMillis() {
        return delegate.readTimeoutMillis();
    }

    @Override
    public Interceptor.Chain withReadTimeout(int timeout, TimeUnit unit) {
        return new RetryCountingChain(delegate.withReadTimeout(timeout, unit), attempts);
    }

    @Override
    public int writeTimeoutMillis() {
        return delegate.writeTimeoutMillis();
    }

    @Override
    public Interceptor.Chain withWriteTimeout(int timeout, TimeUnit unit) {
        return new RetryCountingChain(delegate.withWriteTimeout(timeout, unit), attempts);
    }
}

/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.otlp.retry;

import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.NewRelic;
import junit.framework.TestCase;
import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RetryCountingChainTest extends TestCase {

    private static final String EXPORT_RETRY_METRIC = "Supportability/Metrics/Java/OpenTelemetryBridge/export/retry";

    public void testFirstProceedDoesNotRecordRetryMetric() throws IOException {
        Response response = mock(Response.class);
        FakeChain delegate = new FakeChain(mock(Request.class), response);
        RetryCountingChain chain = new RetryCountingChain(delegate);

        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            Response result = chain.proceed(mock(Request.class));

            assertSame(response, result);
            mockNewRelic.verify(() -> NewRelic.incrementCounter(anyString()), Mockito.never());
        }
        assertEquals(1, delegate.proceedCount);
    }

    public void testSecondAndThirdProceedEachRecordRetryMetricOnce() throws IOException {
        FakeChain delegate = new FakeChain(mock(Request.class), mock(Response.class));
        RetryCountingChain chain = new RetryCountingChain(delegate);
        Request request = mock(Request.class);

        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            chain.proceed(request); // initial attempt - no retry metric
            chain.proceed(request); // 1st retry
            chain.proceed(request); // 2nd retry

            mockNewRelic.verify(() -> NewRelic.incrementCounter(EXPORT_RETRY_METRIC), times(2));
        }
        assertEquals(3, delegate.proceedCount);
    }

    public void testWithReadTimeoutContinuesSameCount() throws IOException {
        FakeChain delegate = new FakeChain(mock(Request.class), mock(Response.class));
        RetryCountingChain chain = new RetryCountingChain(delegate);
        Request request = mock(Request.class);

        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            chain.proceed(request); // initial attempt, on the original chain

            Interceptor.Chain rewrapped = chain.withReadTimeout(5, TimeUnit.SECONDS);
            assertTrue(rewrapped instanceof RetryCountingChain);
            rewrapped.proceed(request); // continues as the 2nd attempt (1st retry)

            // Only one retry happened across the two chain instances - the attempt count is shared,
            // not reset by re-wrapping.
            mockNewRelic.verify(() -> NewRelic.incrementCounter(EXPORT_RETRY_METRIC), times(1));
        }
    }

    public void testWithConnectTimeoutAndWithWriteTimeoutAlsoContinueCount() throws IOException {
        FakeChain delegate = new FakeChain(mock(Request.class), mock(Response.class));
        RetryCountingChain chain = new RetryCountingChain(delegate);
        Request request = mock(Request.class);

        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            chain.proceed(request); // initial attempt
            chain.withConnectTimeout(1, TimeUnit.SECONDS).proceed(request); // 1st retry
            chain.withWriteTimeout(1, TimeUnit.SECONDS).proceed(request); // 2nd retry

            mockNewRelic.verify(() -> NewRelic.incrementCounter(EXPORT_RETRY_METRIC), times(2));
        }
    }

    public void testProceedStillDelegatesAndReturnsResponseWhenMetricCallThrows() throws IOException {
        Response response = mock(Response.class);
        FakeChain delegate = new FakeChain(mock(Request.class), response);
        RetryCountingChain chain = new RetryCountingChain(delegate);
        Request request = mock(Request.class);

        Agent agent = mock(Agent.class);
        Logger logger = mock(Logger.class);
        when(agent.getLogger()).thenReturn(logger);

        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(agent);
            mockNewRelic.when(() -> NewRelic.incrementCounter(anyString())).thenThrow(new RuntimeException("boom"));

            chain.proceed(request); // initial attempt - does not touch the metric API
            Response result = chain.proceed(request); // retry - metric call throws, must not propagate

            assertSame(response, result);
            verify(logger).log(eq(Level.FINER), any(Throwable.class), anyString());
        }
        assertEquals(2, delegate.proceedCount);
    }

    public void testPassThroughMethodsDelegate() {
        Request request = mock(Request.class);
        Response response = mock(Response.class);
        FakeChain delegate = new FakeChain(request, response);
        RetryCountingChain chain = new RetryCountingChain(delegate);

        assertSame(request, chain.request());
        // connection() is nullable per okhttp's contract; the wrapper must not substitute a value.
        assertNull(chain.connection());
        assertNotNull(chain.call());
        assertEquals(1000, chain.connectTimeoutMillis());
        assertEquals(2000, chain.readTimeoutMillis());
        assertEquals(3000, chain.writeTimeoutMillis());
    }

    // A hand-rolled Interceptor.Chain that counts proceed() calls and always returns the same
    // Response, standing in for RetryInterceptor's own chain during the retry loop.
    static class FakeChain implements Interceptor.Chain {
        private final Request request;
        private final Response response;
        int proceedCount = 0;

        FakeChain(Request request, Response response) {
            this.request = request;
            this.response = response;
        }

        @Override
        public Response proceed(Request request) throws IOException {
            proceedCount++;
            return response;
        }

        @Override
        public Request request() {
            return request;
        }

        @Override
        public Connection connection() {
            return null;
        }

        @Override
        public Call call() {
            return mock(Call.class);
        }

        @Override
        public int connectTimeoutMillis() {
            return 1000;
        }

        @Override
        public Interceptor.Chain withConnectTimeout(int timeout, TimeUnit unit) {
            return this;
        }

        @Override
        public int readTimeoutMillis() {
            return 2000;
        }

        @Override
        public Interceptor.Chain withReadTimeout(int timeout, TimeUnit unit) {
            return this;
        }

        @Override
        public int writeTimeoutMillis() {
            return 3000;
        }

        @Override
        public Interceptor.Chain withWriteTimeout(int timeout, TimeUnit unit) {
            return this;
        }
    }
}

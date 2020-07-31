/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import static org.mockito.Mockito.when;

import java.text.MessageFormat;
import java.util.logging.Level;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.newrelic.agent.circuitbreaker.CircuitBreakerService;
import com.newrelic.agent.instrumentation.InstrumentationImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.ClassMethodSignatures;
import com.newrelic.agent.tracers.TracerFlags;

/**
 * This class ensures that the "fast path" createTracer call doesn't depend on the Transaction class and doesn't cause a
 * Transaction to be created when it's executed. If it ever seems that code in this class requires mocking up a
 * Transaction, the problem is that a dependency on Transaction has been introduced into the Agent where it must not be
 * allowed for performance reasons.
 */
public class NoTransactionCreateTracerTest {

    // We do need to mock up the service manager because the createTracer() fast path touches the CircuitBreakerService.
    // The key is just not to mock up a Transaction or anything that requires a Transaction. In particular, do not mock
    // up the ConfigService, because this ensures that any accidental attempt to create a Transaction will fail.

    private static CircuitBreakerService cbs = Mockito.mock(CircuitBreakerService.class);
    private static TransactionTraceService tts = Mockito.mock(TransactionTraceService.class);

    @BeforeClass
    public static void beforeClass() throws Exception {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);

        when(cbs.isTripped()).thenReturn(false);
        serviceManager.setCircuitBreakerService(cbs);

        when(tts.isEnabled()).thenReturn(true);
        serviceManager.setTransactionTraceService(tts);

        ClassMethodSignatures.get().add(new ClassMethodSignature("firstClass", "firstMethod", "firstDesc"));
        ClassMethodSignatures.get().add(new ClassMethodSignature("secondClass", "secondMethod", "secondDesc"));
    }

    @Test
    public void testCreateTracerNoTx() throws InterruptedException {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                InstrumentationImpl ii = new InstrumentationImpl(new TestLogger());
                // If there is an attempt to create a Transaction, it will throw because the config
                // service doesn't exist. But this is caught and causes a null return, just like any
                // other failure of any time. Must debug at that point to figure out what happened.
                Assert.assertNotNull(ii.createTracer(new Object(), 1, null, TracerFlags.ASYNC));
                Assert.assertNull(Transaction.getTransaction(false));

            }
        };

        Thread thread = new Thread(runnable);
        thread.start();
        thread.join();
    }

    private static class TestLogger implements com.newrelic.api.agent.Logger {
        @Override
        public boolean isLoggable(Level level) {
            return true;
        }

        @Override
        public void log(Level level, String pattern, Object[] msg) {
            System.out.println(level.getName() + ": " + MessageFormat.format(pattern, msg));
        }

        @Override
        public void log(Level level, String pattern) {
            System.out.println(level.getName() + ": " + pattern);
        }

        @Override
        public void log(Level level, String pattern, Object part1) {
            System.out.println(level.getName() + ": " + MessageFormat.format(pattern, part1));
        }

        @Override
        public void log(Level level, String pattern, Object part1, Object part2) {
            System.out.println(level.getName() + ": " + MessageFormat.format(pattern, part1, part2));
        }

        @Override
        public void log(Level level, String pattern, Object part1, Object part2, Object part3) {
            System.out.println(level.getName() + ": " + MessageFormat.format(pattern, part1, part2, part3));
        }

        @Override
        public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4) {
            System.out.println(level.getName() + ": " + MessageFormat.format(pattern, part1, part2, part3, part4));
        }

        @Override
        public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4,
                Object part5) {
            System.out.println(level.getName() + ": " + MessageFormat.format(pattern, part1, part2, part3, part4, part5));
        }

        @Override
        public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4,
                Object part5, Object part6) {
            System.out.println(level.getName() + ": " + MessageFormat.format(pattern, part1, part2, part3, part4, part5,
                    part6));
        }

        @Override
        public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4,
                Object part5, Object part6, Object part7) {
            System.out.println(level.getName() + ": " + MessageFormat.format(pattern, part1, part2, part3, part4, part5,
                    part6, part7));
        }

        @Override
        public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4,
                        Object part5, Object part6, Object part7, Object... otherParts) {
            System.out.println(level.getName() + ": " + MessageFormat.format(pattern, part1, part2, part3, part4, part5,
                    part6, part7, otherParts));
        }

        @Override
        public void log(Level level, Throwable t, String pattern, Object[] msg) {
            log(level, t.getClass().getName() + ": " + t.getLocalizedMessage() + ": " + pattern, msg);
        }

        @Override
        public void log(Level level, Throwable t, String pattern) {
            log(level, t.getClass().getName() + ": " + t.getLocalizedMessage() + ": " + pattern);
        }

        @Override
        public void log(Level level, Throwable t, String pattern, Object part1) {
            log(level, t.getClass().getName() + ": " + t.getLocalizedMessage() + ": " + pattern, part1);
        }

        @Override
        public void log(Level level, Throwable t, String pattern, Object part1, Object part2) {
            log(level, t.getClass().getName() + ": " + t.getLocalizedMessage() + ": " + pattern, part1, part2);
        }

        @Override
        public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3) {
            log(level, t.getClass().getName() + ": " + t.getLocalizedMessage() + ": " + pattern, part1, part2, part3);
        }

        @Override
        public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3,
                Object part4) {
            log(level, t.getClass().getName() + ": " + t.getLocalizedMessage() + ": " + pattern, part1, part2, part3,
                    part4);
        }

        @Override
        public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3,
                Object part4, Object part5) {
            log(level, t.getClass().getName() + ": " + t.getLocalizedMessage() + ": " + pattern, part1, part2, part3,
                    part4, part5);
        }

        @Override
        public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3,
                Object part4, Object part5, Object part6) {
            log(level, t.getClass().getName() + ": " + t.getLocalizedMessage() + ": " + pattern, part1, part2, part3,
                    part4, part5, part6);
        }

        @Override
        public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3,
                Object part4, Object part5, Object part6, Object part7) {
            log(level, t.getClass().getName() + ": " + t.getLocalizedMessage() + ": " + pattern, part1, part2, part3,
                    part4, part5, part6, part7);
        }

        @Override
        public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3,
                        Object part4, Object part5, Object part6, Object part7, Object... otherParts) {
            log(level, t.getClass().getName() + ": " + t.getLocalizedMessage() + ": " + pattern, part1, part2, part3,
                    part4, part5, part6, part7, otherParts);
        }

        @Override
        public void logToChild(String childName, Level level, String pattern, Object part1, Object part2, Object part3,
                Object part4) {
            log(level, "Child logger: " + childName + ": " + pattern, part1, part2, part3, part4);
        }
    }
}

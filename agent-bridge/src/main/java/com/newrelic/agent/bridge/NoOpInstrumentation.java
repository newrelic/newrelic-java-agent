/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import java.io.Closeable;
import java.lang.reflect.Method;

public class NoOpInstrumentation implements Instrumentation {

    private final IdGenerator idGenerator = new IdGenerator() {
        @Override
        public String generateSpanId() {
            return "0000000000000000";
        }

        @Override
        public String generateTraceId() {
            return generateSpanId() + generateSpanId();
        }
    };

    @Override
    public ExitTracer createTracer(Object invocationTarget, int signatureId, String metricName, int flags) {
        return null;
    }

    @Override
    public void noticeInstrumentationError(Throwable throwable, String libraryName) {
    }

    @Override
    public void instrument(String className, String metricPrefix) {
    }

    @Override
    public void instrument(Method methodToInstrument, String metricPrefix) {
    }

    @Override
    public void instrument() {
    }

    @Override
    public void retransformUninstrumentedClass(Class<?> classToRetransform) {
    }

    @Override
    public Class<?> loadClass(ClassLoader classLoader, Class<?> theClass) throws ClassNotFoundException {
        return null;
    }

    @Override
    public Transaction getTransaction() {
        return NoOpTransaction.INSTANCE;
    }

    @Override
    public Transaction getTransactionOrNull() {
        return null;
    }

    @Override
    public int addToObjectCache(Object object) {
        return -1;
    }

    @Override
    public Object getCachedObject(int id) {
        return null;
    }

    @Override
    public void registerCloseable(String string, Closeable closeable) {
    }

    @Override
    public ExitTracer createTracer(Object invocationTarget, int signatureId, boolean dispatcher, String metricName,
            String tracerFactoryName, Object[] args) {
        return null;
    }

    @Override
    public ExitTracer createSqlTracer(Object invocationTarget, int signatureId, String metricName, int flags) {
        return null;
    }

  @Override
  public ExitTracer createScalaTxnTracer() {
    return null;
  }

    @Override
    public IdGenerator getIdGenerator() {
        return idGenerator;
    }
}

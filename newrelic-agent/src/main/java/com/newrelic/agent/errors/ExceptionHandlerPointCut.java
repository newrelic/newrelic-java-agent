/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.errors;

import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.tracers.ClassMethodSignature;

public final class ExceptionHandlerPointCut extends AbstractExceptionHandlerPointCut {

    private final int exceptionArgumentIndex;

    public ExceptionHandlerPointCut(ExceptionHandlerSignature sig) {
        super(new PointCutConfiguration("exception_handler"), sig.getClassMatcher(), sig.getMethodMatcher());
        exceptionArgumentIndex = sig.getExceptionArgumentIndex();
    }

    @Override
    protected Throwable getThrowable(ClassMethodSignature sig, Object[] args) {
        if (exceptionArgumentIndex >= 0) {
            return (Throwable) args[exceptionArgumentIndex];
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Throwable) {
                return (Throwable) args[i];
            }
        }
        return null;
    }

}

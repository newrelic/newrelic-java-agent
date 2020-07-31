/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.newrelic.agent.errors.ErrorAnalyzer;
import com.newrelic.agent.errors.ErrorMessageReplacer;
import com.newrelic.agent.errors.ReportableError;
import com.newrelic.agent.model.SpanError;
import com.newrelic.agent.tracers.ErrorTracer;
import com.newrelic.agent.transaction.TransactionThrowable;

import java.util.Objects;

public class SpanErrorBuilder {
    private final ErrorAnalyzer analyzer;
    private final ErrorMessageReplacer messageReplacer;

    public SpanErrorBuilder(ErrorAnalyzer analyzer, ErrorMessageReplacer messageReplacer) {
        this.analyzer = analyzer == null ? ErrorAnalyzer.DEFAULT : analyzer;
        this.messageReplacer = messageReplacer;
    }

    public SpanError buildSpanError(
            ErrorTracer tracer,
            boolean isRoot,
            int responseStatus,
            String statusMessage,
            TransactionThrowable transactionThrowable) {

        Throwable throwable = extractThrowable(tracer, transactionThrowable);

        int statusCode = isRoot ? responseStatus : ErrorAnalyzer.NO_STATUS;

        if (analyzer.isIgnoredError(statusCode, throwable)) {
          return new SpanError();
        }

        if (!isErrorSetByAPI(tracer)) {
            if (!analyzer.isReportable(statusCode, throwable)) {
                return new SpanError();
            }
        }

        SpanError result = new SpanError();

        result.setExpectedError(identifyExpectedError(statusCode, transactionThrowable));

        if (analyzer.isReportable(statusCode)) {
            result.setErrorStatus(statusCode);
            result.setErrorMessage(statusMessage);
        }

        if (throwable != null) {
            result.setErrorMessage(identifyErrorMessage(tracer, throwable));
            result.setErrorClass(identifyErrorClass(throwable));
        }

        return result;
    }

    private boolean identifyExpectedError(int statusCode, TransactionThrowable transactionThrowable) {
        return analyzer.isExpectedError(statusCode, transactionThrowable);
    }

    private Class<?> identifyErrorClass(Throwable throwable) {
        return throwable == null || throwable instanceof ReportableError ? null : throwable.getClass();
    }

    private String identifyErrorMessage(ErrorTracer tracer, Throwable throwable) {
        if (throwable == null) {
            return null;
        } else if (isErrorSetByAPI(tracer)) {
            // ReportableError is a fake exception class that we use the wrap calls to `noticeError(String)`
            // so the class is an internal that should not be exposed to the user.
            return throwable.getMessage();
        } else {
            return messageReplacer.getMessage(throwable);
        }
    }

    private boolean isErrorSetByAPI(ErrorTracer tracer) {
        return tracer.getException() != null && tracer.wasExceptionSetByAPI();
    }

    private Throwable extractThrowable(ErrorTracer tracer, TransactionThrowable transactionThrowable) {
        if (tracer.getException() != null) {
            return tracer.getException();
        } else if (transactionThrowable != null && Objects.equals(transactionThrowable.spanId, tracer.getGuid())) {
            return transactionThrowable.throwable;
        }

        return null;
    }

    public boolean areErrorsEnabled() {
        return analyzer.areErrorsEnabled();
    }
}

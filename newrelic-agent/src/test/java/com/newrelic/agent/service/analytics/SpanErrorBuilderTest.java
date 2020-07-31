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
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class SpanErrorBuilderTest {

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldBuildEmptySpanErrorWithoutReportableError() {
        when(mockAnalyzer.isReportable(eq(400), ArgumentMatchers.<Throwable>any())).thenReturn(false);
        when(mockTracer.getException()).thenReturn(null);

        SpanError target = new SpanErrorBuilder(mockAnalyzer, mockReplacer)
                .buildSpanError(mockTracer, true, 200, "fine", null);

        assertEmptySpanError(target);
    }

    @Test
    public void shouldBuildSpanErrorWithReportableStatusOnRoot() {
        when(mockAnalyzer.isReportable(eq(400), ArgumentMatchers.<Throwable>any())).thenReturn(true);
        when(mockAnalyzer.isReportable(400)).thenReturn(true);
        when(mockAnalyzer.isIgnoredError(anyInt(), ArgumentMatchers.<Throwable>any()))
                .thenReturn(false);
        when(mockTracer.getException()).thenReturn(null);

        SpanError target = new SpanErrorBuilder(mockAnalyzer, mockReplacer)
                .buildSpanError(mockTracer, true, 400, "fine", null);

        assertSpanError(target, 400, null, "fine", false);
    }

    @Test
    public void shouldBuildEmptySpanErrorWithReportableStatusOnNonRoot() {
        when(mockAnalyzer.isReportable(eq(400), ArgumentMatchers.<Throwable>any())).thenReturn(true);
        when(mockAnalyzer.isReportable(400)).thenReturn(true);
        when(mockAnalyzer.isIgnoredError(anyInt(), ArgumentMatchers.<Throwable>any()))
                .thenReturn(false);
        when(mockTracer.getException()).thenReturn(null);

        SpanError target = new SpanErrorBuilder(mockAnalyzer, mockReplacer)
                .buildSpanError(mockTracer, false, 400, "fine", null);

        assertEmptySpanError(target);
    }

    @Test
    public void shouldAddErrorClassAndMessageToSpanError() {
        when(mockAnalyzer.isReportable(anyInt(), ArgumentMatchers.<Throwable>any())).thenReturn(true);
        when(mockAnalyzer.isReportable(anyInt())).thenReturn(false);

        when(mockTracer.getException()).thenReturn(new RuntimeException());
        when(mockTracer.wasExceptionSetByAPI()).thenReturn(false);

        when(mockReplacer.getMessage(ArgumentMatchers.<Throwable>any())).thenReturn("replaced message");

        SpanError target = new SpanErrorBuilder(mockAnalyzer, mockReplacer)
                .buildSpanError(mockTracer, false, 400, "fine", null);

        assertSpanError(target, null, RuntimeException.class, "replaced message", false);
    }

    @Test
    public void shouldAddStatusAndExceptionToRootSpans() {
        when(mockAnalyzer.isReportable(anyInt(), ArgumentMatchers.<Throwable>any())).thenReturn(true);
        when(mockAnalyzer.isReportable(anyInt())).thenReturn(true);

        when(mockTracer.getException()).thenReturn(new RuntimeException());
        when(mockTracer.wasExceptionSetByAPI()).thenReturn(false);

        when(mockReplacer.getMessage(ArgumentMatchers.<Throwable>any())).thenReturn("replaced message");

        SpanError target = new SpanErrorBuilder(mockAnalyzer, mockReplacer)
                .buildSpanError(mockTracer, true, 400, "fine", null);

        assertSpanError(target, 400, RuntimeException.class, "replaced message", false);
    }

    @Test
    public void shouldSetExpectedErrorTrueForSpanErrorOnNonRootSpan() {
        final String spanGuid = "SPAN_GUID";
        TransactionThrowable fakeThrowable = new TransactionThrowable(
                new IllegalArgumentException("what did I do?"), true, spanGuid);
        when(mockTracer.getGuid()).thenReturn(spanGuid);
        when(mockTracer.wasExceptionSetByAPI()).thenReturn(true);
        when(mockTracer.getException()).thenReturn(fakeThrowable.throwable);
        when(mockAnalyzer.isExpectedError(0, fakeThrowable)).thenReturn(true);

        SpanError target = new SpanErrorBuilder(mockAnalyzer, mockReplacer)
                .buildSpanError(
                        mockTracer,
                        false,
                        400,
                        "This message will not be set",
                        fakeThrowable);
        assertSpanError(target, null, IllegalArgumentException.class, "what did I do?", true);
    }

    @Test
    public void shouldSetExpectedErrorTrueForSpanErrorOnRootSpan() {
        final String spanGuid = "SPAN_GUID";
        TransactionThrowable fakeThrowable = new TransactionThrowable(
                new IllegalArgumentException("what did I do?"), true, spanGuid);
        when(mockTracer.getGuid()).thenReturn(spanGuid);
        when(mockTracer.wasExceptionSetByAPI()).thenReturn(true);
        when(mockTracer.getException()).thenReturn(fakeThrowable.throwable);
        when(mockAnalyzer.isExpectedError(400, fakeThrowable)).thenReturn(true);
        when(mockAnalyzer.isReportable(400)).thenReturn(true);

        SpanError target = new SpanErrorBuilder(mockAnalyzer, mockReplacer)
                .buildSpanError(
                        mockTracer,
                        true,
                        400,
                        "This web response message will be overridden",
                        fakeThrowable);
        assertSpanError(target, 400, IllegalArgumentException.class, "what did I do?", true);
    }

    @Test
    public void shouldSetExpectedErrorTrueForExpectedStatusCodeButNoThrownException() {
        when(mockAnalyzer.isReportable(400)).thenReturn(true);
        when(mockAnalyzer.isReportable(400, (Throwable) null)).thenReturn(true);
        when(mockAnalyzer.isExpectedError(400, null)).thenReturn(true);
        when(mockTracer.getException()).thenReturn(null);

        SpanError target = new SpanErrorBuilder(mockAnalyzer, mockReplacer)
                .buildSpanError(mockTracer, true, 400, "fine", null);

        assertSpanError(target, 400, null, "fine", true);
    }

    @Test
    public void shouldIgnoreExpectedErrorOnThrowable() {
        final String spanGuid = "SPAN_GUID";
        TransactionThrowable fakeThrowable = new TransactionThrowable(
                new IllegalArgumentException("what did I do?"), true, spanGuid);
        Throwable exceptionOfThrowable = fakeThrowable.throwable;
        when(mockAnalyzer.isReportable(400, fakeThrowable)).thenReturn(true);
        when(mockAnalyzer.isReportable(anyInt())).thenReturn(true);
        when(mockAnalyzer.isIgnoredError(400, exceptionOfThrowable)).thenReturn(true);
        when(mockAnalyzer.isExpectedError(400, fakeThrowable)).thenReturn(true);
        when(mockTracer.getException()).thenReturn(exceptionOfThrowable);
        when(mockTracer.wasExceptionSetByAPI()).thenReturn(true);

        SpanError target = new SpanErrorBuilder(mockAnalyzer, mockReplacer)
                .buildSpanError(mockTracer, true, 400, "fine", fakeThrowable);

        assertEmptySpanError(target);
    }

    @Test
    public void shouldNotDoAnythingIfEitherAreIgnored() {
        when(mockAnalyzer.isReportable(anyInt(), ArgumentMatchers.<Throwable>any())).thenReturn(true);
        when(mockAnalyzer.isReportable(anyInt())).thenReturn(true);
        when(mockAnalyzer.isIgnoredError(anyInt(), ArgumentMatchers.<Throwable>any())).thenReturn(true);

        when(mockTracer.getException()).thenReturn(new RuntimeException());
        when(mockTracer.wasExceptionSetByAPI()).thenReturn(false);

        when(mockReplacer.getMessage(ArgumentMatchers.<Throwable>any())).thenReturn("replaced message");

        SpanError target = new SpanErrorBuilder(mockAnalyzer, mockReplacer)
                .buildSpanError(mockTracer, true, 400, "fine", null);

        assertEmptySpanError(target);
    }

    @Test
    public void transactionThrowableUsedIfTracerGuidMatches() {
        final String spanGuid = "SPAN_GUID";
        when(mockAnalyzer.isReportable(anyInt(), ArgumentMatchers.<Throwable>any())).thenReturn(true);
        when(mockAnalyzer.isReportable(anyInt())).thenReturn(false);
        when(mockAnalyzer.isIgnoredError(anyInt(), ArgumentMatchers.<Throwable>any()))
                .thenReturn(false);

        when(mockTracer.getException()).thenReturn(null);
        when(mockTracer.wasExceptionSetByAPI()).thenReturn(false);
        when(mockTracer.getGuid()).thenReturn(spanGuid);

        when(mockReplacer.getMessage(ArgumentMatchers.<Throwable>any())).thenReturn("replaced message");

        SpanError target = new SpanErrorBuilder(mockAnalyzer, mockReplacer)
                .buildSpanError(
                        mockTracer,
                        false,
                        400,
                        "fine",
                        new TransactionThrowable(new IllegalAccessException("something"), false, spanGuid));

        assertSpanError(target, null, IllegalAccessException.class, "replaced message",
                false);
    }

    @Test
    public void transactionThrowableNotUsedIfTracerGuidDoesNotMatch() {
        when(mockAnalyzer.isReportable(anyInt(), ArgumentMatchers.<Throwable>any())).thenReturn(true);
        when(mockAnalyzer.isReportable(anyInt())).thenReturn(false);
        when(mockAnalyzer.isIgnoredError(anyInt(), ArgumentMatchers.<Throwable>any()))
                .thenReturn(false);

        when(mockTracer.getException()).thenReturn(null);
        when(mockTracer.wasExceptionSetByAPI()).thenReturn(false);
        when(mockTracer.getGuid()).thenReturn("some guid");

        when(mockReplacer.getMessage(ArgumentMatchers.<Throwable>any())).thenReturn("replaced message");

        SpanError target = new SpanErrorBuilder(mockAnalyzer, mockReplacer)
                .buildSpanError(
                        mockTracer,
                        false,
                        400,
                        "fine",
                        new TransactionThrowable(new IllegalAccessException("something"), false,
                                "some other guid"));

        assertEmptySpanError(target);
    }

    @Test
    public void shouldBypassIgnoreReportableAndReplacementRulesForNoticedErrors() {
        when(mockAnalyzer.isReportable(anyInt(), ArgumentMatchers.<Throwable>any()))
                .thenThrow(new AssertionError("should not have been called"));
        when(mockAnalyzer.isReportable(anyInt())).thenReturn(false);

        when(mockTracer.getException()).thenReturn(new RuntimeException("~~ unstripped due to api ~~"));
        when(mockTracer.wasExceptionSetByAPI()).thenReturn(true);

        when(mockReplacer.getMessage(ArgumentMatchers.<Throwable>any()))
                .thenThrow(new AssertionError("should not have been called"));

        SpanError target = new SpanErrorBuilder(mockAnalyzer, mockReplacer)
                .buildSpanError(mockTracer, false, 400, "fine", null);

        assertSpanError(target, null, RuntimeException.class, "~~ unstripped due to api ~~",
                false);
    }

    @Test
    public void shouldOmitClassForNoticeErrorStringAPI() {
        when(mockAnalyzer.isReportable(anyInt(), ArgumentMatchers.<Throwable>any()))
                .thenThrow(new AssertionError("should not have been called"));
        when(mockAnalyzer.isReportable(anyInt())).thenReturn(false);

        when(mockTracer.getException()).thenReturn(new ReportableError("will not be replaced"));
        when(mockTracer.wasExceptionSetByAPI()).thenReturn(true);

        when(mockReplacer.getMessage(ArgumentMatchers.<Throwable>any()))
                .thenThrow(new AssertionError("should not have been called"));

        SpanError target = new SpanErrorBuilder(mockAnalyzer, mockReplacer)
                .buildSpanError(mockTracer, false, 400, "fine", null);

        assertSpanError(target, null, null, "will not be replaced", false);
    }

    private void assertEmptySpanError(SpanError target) {
        assertSpanError(target, null, null, null, false);
    }

    private void assertSpanError(SpanError target, Integer expectedStatus, Class<?> expectedClass,
            String expectedMessage, boolean expectedError) {
        assertEquals(expectedClass, target.getErrorClass());
        assertEquals(expectedStatus, target.getErrorStatus());
        assertEquals(expectedMessage, target.getErrorMessage());
        assertEquals(expectedError, target.isExpectedError());
    }

    @Mock
    public ErrorAnalyzer mockAnalyzer;

    @Mock
    public ErrorMessageReplacer mockReplacer;

    @Mock
    public ErrorTracer mockTracer;
}
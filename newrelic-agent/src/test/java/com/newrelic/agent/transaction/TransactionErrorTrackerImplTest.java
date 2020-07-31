/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import com.newrelic.agent.TransactionErrorPriority;
import org.junit.Test;

import javax.servlet.ServletException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class TransactionErrorTrackerImplTest {

    @Test
    public void handlesNullExceptions() {
        TransactionErrorTrackerImpl target = new TransactionErrorTrackerImpl();
        target.setThrowable(null, TransactionErrorPriority.TRACER, false, null);
        assertNull(target.getThrowable());
    }

    @Test
    public void acceptsNormalExceptions() {
        Exception ex = new RuntimeException("dude");
        TransactionErrorTrackerImpl target = new TransactionErrorTrackerImpl();
        target.setThrowable(ex, TransactionErrorPriority.TRACER, false, null);
        assertSame(ex, target.getThrowable().throwable);
    }

    @Test
    public void acceptsNoCauseServletExceptions() {
        ServletException ex2 = new ServletException("dude");
        TransactionErrorTrackerImpl target = new TransactionErrorTrackerImpl();
        target.setThrowable(ex2, TransactionErrorPriority.TRACER, false, null);
        assertSame(ex2, target.getThrowable().throwable);
    }

    @Test
    public void unwrapsServletErrors() {
        ServletException ex2 = new ServletException("dude");
        ServletException ex3 = new ServletException("dude", ex2);
        TransactionErrorTrackerImpl target = new TransactionErrorTrackerImpl();
        target.setThrowable(ex3, TransactionErrorPriority.TRACER, false, null);
        assertSame(ex2, target.getThrowable().throwable);
    }


    @Test
    public void noticeThenSetAsTracerPicksUpOriginalSpanNotCurrent() throws Exception {
        TransactionErrorTrackerImpl target = new TransactionErrorTrackerImpl();
        Throwable exc = new Throwable();

        target.noticeTracerException(exc, "span id 1");
        target.noticeTracerException(exc, "span id 2");
        target.setThrowable(exc, TransactionErrorPriority.TRACER, false, "current span");
        assertEquals("span id 1", target.getThrowable().spanId);
    }

    @Test
    public void noticeThenSetAsTracerSetsSpanToNullIfNoTracerAndNotNoticed() throws Exception {
        TransactionErrorTrackerImpl target = new TransactionErrorTrackerImpl();
        Throwable exc = new Throwable();

        target.setThrowable(exc, TransactionErrorPriority.TRACER, false, null);
        assertNull(target.getThrowable().spanId);
    }

    @Test
    public void noticeThenSetAsAPISetsSpanToNullIfNoTracerAndNotNoticed() throws Exception {
        TransactionErrorTrackerImpl target = new TransactionErrorTrackerImpl();
        Throwable exc = new Throwable();

        target.setThrowable(exc, TransactionErrorPriority.API, false, null);
        assertNull(target.getThrowable().spanId);
    }

    @Test
    public void noticeThenSetAsAPISetsSpanIfNoticedButNoSpan() throws Exception {
        TransactionErrorTrackerImpl target = new TransactionErrorTrackerImpl();
        Throwable exc = new Throwable();

        target.noticeTracerException(exc, "span id 1");
        target.setThrowable(exc, TransactionErrorPriority.API, false, null);
        assertEquals("span id 1", target.getThrowable().spanId);
    }

    @Test
    public void noticeThenSetAsAPIUsesCurrentSpan() throws Exception {
        TransactionErrorTrackerImpl target = new TransactionErrorTrackerImpl();
        Throwable exc = new Throwable();

        target.noticeTracerException(exc, "span id 1");
        target.noticeTracerException(exc, "span id 2");
        target.setThrowable(exc, TransactionErrorPriority.API, false, "current span");
        assertEquals("current span", target.getThrowable().spanId);
    }


}
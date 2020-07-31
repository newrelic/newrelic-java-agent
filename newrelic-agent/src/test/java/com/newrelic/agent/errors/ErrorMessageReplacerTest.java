/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.errors;

import com.newrelic.agent.config.StripExceptionConfigImpl;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class ErrorMessageReplacerTest {
    @Test
    public void shouldReplaceWhenEnabled() {
        ErrorMessageReplacer target = new ErrorMessageReplacer(
                new StripExceptionConfigImpl(true, null));

        assertEquals(
                ErrorMessageReplacer.STRIPPED_EXCEPTION_REPLACEMENT,
                target.getMessage(new Throwable("stripped")));
    }

    @Test
    public void shouldAllowSpecificClassesWhenEnabled() {
        ErrorMessageReplacer target = new ErrorMessageReplacer(
                new StripExceptionConfigImpl(
                        true,
                        Collections.singleton(RuntimeException.class.getName())));

        assertEquals(
                ErrorMessageReplacer.STRIPPED_EXCEPTION_REPLACEMENT,
                target.getMessage(new Throwable("stripped")));
        assertEquals(
                "NOT stripped",
                target.getMessage(new RuntimeException("NOT stripped")));
    }

    @Test
    public void shouldAllowAllClassesWhenDisabled() {
        ErrorMessageReplacer target = new ErrorMessageReplacer(
                new StripExceptionConfigImpl(
                        false,
                        Collections.singleton(RuntimeException.class.getName())));

        assertEquals(
                "NOT stripped!",
                target.getMessage(new Throwable("NOT stripped!")));
        assertEquals(
                "NOT stripped",
                target.getMessage(new RuntimeException("NOT stripped")));
    }

    @Test
    public void shouldHandleNullMessages() {
        ErrorMessageReplacer target = new ErrorMessageReplacer(
                new StripExceptionConfigImpl(
                        true,
                        Collections.singleton(RuntimeException.class.getName())));

        assertEquals("", target.getMessage(null));
        assertEquals("", target.getMessage(new RuntimeException((String)null)));
    }
}
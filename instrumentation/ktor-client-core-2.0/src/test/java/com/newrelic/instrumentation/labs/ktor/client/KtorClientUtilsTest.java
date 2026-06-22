/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.instrumentation.labs.ktor.client;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class KtorClientUtilsTest {

    @Test
    public void needsLeaf_trueForCIOEngine() {
        assertTrue(KtorClientUtils.needsLeaf("CIOEngine"));
    }

    @Test
    public void needsLeaf_trueForJavaHttpEngine() {
        assertTrue(KtorClientUtils.needsLeaf("JavaHttpEngine"));
    }

    @Test
    public void needsLeaf_falseForJettyEngine() {
        assertFalse(KtorClientUtils.needsLeaf("JettyHttp2Engine"));
    }

    @Test
    public void needsLeaf_falseForUnknownEngine() {
        assertFalse(KtorClientUtils.needsLeaf("OkHttpEngine"));
    }

    @Test
    public void needsLeaf_falseForEmptyString() {
        assertFalse(KtorClientUtils.needsLeaf(""));
    }

    @Test
    public void needsLeaf_falseForNull() {
        assertFalse(KtorClientUtils.needsLeaf(null));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getContinuationWrapper_returnsNullIfAlreadyWrapped() {
        // Mock bypasses the constructor so no agent runtime is needed.
        NRContinuationWrapper<Object> alreadyWrapped = mock(NRContinuationWrapper.class);

        NRContinuationWrapper<Object> result = KtorClientUtils.getContinuationWrapper(alreadyWrapped, null);

        assertNull(result);
    }
}

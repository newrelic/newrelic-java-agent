/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.kotlincoroutines;

import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.KotlinCoroutinesConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KotlinCoroutinesServiceTest {

    private KotlinCoroutinesConfig config;
    private KotlinCoroutinesService service;

    @Before
    public void setup() {
        new MockServiceManager();

        config = mock(KotlinCoroutinesConfig.class);
        when(config.getIgnoredContinuations()).thenReturn(new String[0]);
        when(config.getIgnoredRegExContinuations()).thenReturn(new String[0]);
        when(config.getIgnoredScopes()).thenReturn(new String[0]);
        when(config.getIgnoredRegexScopes()).thenReturn(new String[0]);
        when(config.getIgnoredDispatched()).thenReturn(new String[0]);
        when(config.getIgnoredRegexDispatched()).thenReturn(new String[0]);
        when(config.getIgnoredSuspends()).thenReturn(new String[0]);
        when(config.getIgnoredRegexSuspends()).thenReturn(new String[0]);

        service = new KotlinCoroutinesService(config);
    }

    @Test
    public void addCoroutineConfigListener_propagatesIgnoredFrameworksImmediately() {
        CoroutineConfigListener listener = mock(CoroutineConfigListener.class);

        service.addCoroutineConfigListener(listener);

        ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        verify(listener).configureIgnoredFrameworks(captor.capture());
        List<String> propagated = Arrays.asList(captor.getValue());
        // "kotlin" is seeded in the constructor
        assertTrue(propagated.contains("kotlin"));
    }

    @Test
    public void addSuspendsConfigListener_propagatesIgnoredFrameworksImmediately() {
        SuspendsConfigListener listener = mock(SuspendsConfigListener.class);

        service.addSuspendsConfigListener(listener);

        ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        verify(listener).configureIgnoredFrameworks(captor.capture());
        List<String> propagated = Arrays.asList(captor.getValue());
        assertTrue(propagated.contains("kotlin"));
    }

    @Test
    public void addIgnoredFramework_nullIsIgnored() {
        // Should not throw.
        service.addIgnoredFramework(null);
    }

    @Test
    public void addIgnoredFramework_emptyStringIsIgnored() {
        int before = KotlinIgnoresCache.getIgnoredFrameworks().length;
        service.addIgnoredFramework("");
        int after = KotlinIgnoresCache.getIgnoredFrameworks().length;
        assertTrue(after == before);
    }
}

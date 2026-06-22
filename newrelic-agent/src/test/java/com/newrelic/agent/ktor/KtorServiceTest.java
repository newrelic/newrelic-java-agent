/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.ktor;

import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.kotlincoroutines.KotlinCoroutinesService;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

public class KtorServiceTest {

    private KtorService ktorService;

    @Before
    public void setup() {
        new MockServiceManager();
        ktorService = new KtorService();
    }

    @Test
    public void isEnabled_returnsTrue() {
        assertTrue(ktorService.isEnabled());
    }

    @Test
    public void doStart_registersKtorAsIgnoredFramework() throws Exception {
        ktorService.start();

        KotlinCoroutinesService coroutinesService = ServiceFactory.getKotlinCoroutinesService();
        verify(coroutinesService).addIgnoredFramework("io.ktor");
    }
}

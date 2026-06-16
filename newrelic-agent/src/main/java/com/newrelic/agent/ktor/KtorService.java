/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.ktor;

import com.newrelic.agent.kotlincoroutines.KotlinCoroutinesService;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;

/**
 * Simple service that informs the Kotlin Coroutine instrumentation to ignore Suspend functions and Continuations
 * associated with Ktor
 */
public class KtorService extends AbstractService {

    public KtorService() {
        super("KtorService");
    }

    @Override
    protected void doStart() throws Exception {
        KotlinCoroutinesService kotlinCoroutinesService = ServiceFactory.getKotlinCoroutinesService();
        kotlinCoroutinesService.addIgnoredFramework("io.ktor");
    }

    @Override
    protected void doStop() throws Exception {
        KotlinCoroutinesService kotlinCoroutinesService = ServiceFactory.getKotlinCoroutinesService();

    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

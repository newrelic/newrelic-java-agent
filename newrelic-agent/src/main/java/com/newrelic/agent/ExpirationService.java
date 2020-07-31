/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.util.DefaultThreadFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ExpirationService extends AbstractService {

    private final ExecutorService tokenExpirationExecutor = Executors.newSingleThreadExecutor(new DefaultThreadFactory("New Relic Token Expiration Handler", true));

    private final ExecutorService segmentExpirationExecutor = Executors.newFixedThreadPool(2, new DefaultThreadFactory("New Relic Segment Expiration Handler", true));

    public ExpirationService() {
        super(ExpirationService.class.getSimpleName());
    }

    @Override
    protected void doStart() throws Exception {
    }

    @Override
    protected void doStop() throws Exception {
        tokenExpirationExecutor.shutdownNow();
        segmentExpirationExecutor.shutdownNow();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public Future<?> expireSegment(Runnable runnable) {
        return segmentExpirationExecutor.submit(runnable);
    }

    public void expireSegmentInline(Runnable runnable) {
        runnable.run();
    }

    public Future<?> expireToken(Runnable runnable) {
        return tokenExpirationExecutor.submit(runnable);
    }
}

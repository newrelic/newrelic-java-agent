/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.samplers;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

import com.newrelic.agent.service.NoopService;

public class NoopSamplerService extends NoopService implements SamplerService {

    public NoopSamplerService() {
        super("SamplerService");
    }

    @Override
    public Closeable addSampler(Runnable sampler, long period, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public Closeable addSampler(Runnable sampler, long initialDelay, long period, TimeUnit timeUnit) {
        return null;
    }
}

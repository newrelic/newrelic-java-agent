/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.samplers;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

import com.newrelic.agent.service.Service;

public interface SamplerService extends Service {

    /**
     * Schedules runnable to be run at given period.
     *
     * @param sampler
     * @param period
     * @param timeUnit
     * @return Closable when called will cancel execution of the sampler.
     */
    Closeable addSampler(Runnable sampler, long period, TimeUnit timeUnit);

    /**
     * Schedules runnable to be run at given period with the given initial delay
     *
     * @param sampler
     * @param initialDelay
     * @param period
     * @param timeUnit
     * @return Closable when called will cancel execution of the sampler.
     */
    Closeable addSampler(Runnable sampler, long initialDelay, long period, TimeUnit timeUnit);

}

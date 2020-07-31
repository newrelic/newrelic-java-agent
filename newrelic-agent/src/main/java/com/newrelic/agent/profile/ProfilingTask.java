/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile;

import com.newrelic.agent.HarvestListener;

public interface ProfilingTask extends Runnable, HarvestListener {

    void addProfile(ProfilerParameters parameters);

    void removeProfile(ProfilerParameters parameters);

}

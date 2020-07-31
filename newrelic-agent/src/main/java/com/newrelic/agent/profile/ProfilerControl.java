/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile;

public interface ProfilerControl {

    void startProfiler(ProfilerParameters parameters);

    int stopProfiler(Long profileId, boolean report);

    boolean isEnabled();

}
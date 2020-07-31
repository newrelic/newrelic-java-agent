/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service;

public class NoopService extends AbstractService {

    public NoopService(String serviceName) {
        super(serviceName);
    }

    @Override
    public final boolean isEnabled() {
        return false;
    }

    @Override
    protected final void doStart() throws Exception {
    }

    @Override
    protected final void doStop() throws Exception {
    }

}

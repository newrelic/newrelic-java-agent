/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.rpm;

public class TestRPMConnectionService extends RPMConnectionServiceImpl {

    private long appServerPortTimeout = RPMConnectionServiceImpl.APP_SERVER_PORT_TIMEOUT;
    private long initialAppServerPortDelay = RPMConnectionServiceImpl.INITIAL_APP_SERVER_PORT_DELAY;

    public TestRPMConnectionService() {
        super();
    }

    @Override
    public long getInitialAppServerPortDelay() {
        return initialAppServerPortDelay;
    }

    public void setInitialAppServerPortDelay(long initialAppServerPortDelay) {
        this.initialAppServerPortDelay = initialAppServerPortDelay;
    }

    @Override
    public long getAppServerPortTimeout() {
        return appServerPortTimeout;
    }

    public void setAppServerPortTimeout(long appServerPortTimeout) {
        this.appServerPortTimeout = appServerPortTimeout;
    }

}

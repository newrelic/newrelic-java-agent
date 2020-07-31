/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.IRPMService;
import com.newrelic.agent.rpm.RPMConnectionService;
import com.newrelic.agent.service.AbstractService;

class IntrospectorRPMConnectService extends AbstractService implements RPMConnectionService {

    protected IntrospectorRPMConnectService() {
        super(IntrospectorRPMConnectService.class.getName());
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void connect(IRPMService rpmService) {
    }

    @Override
    public void connectImmediate(IRPMService rpmService) {
    }

    @Override
    protected void doStart() throws Exception {
    }

    @Override
    protected void doStop() throws Exception {
    }

}

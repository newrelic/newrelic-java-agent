/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.rpm;

import com.newrelic.agent.Agent;
import com.newrelic.agent.ConnectionListener;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.RPMServiceManager;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.service.Service;
import com.newrelic.agent.service.ServiceFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public interface RPMConnectionService extends Service {

    void connect(IRPMService rpmService);

    void connectImmediate(IRPMService rpmService);

    /**
     * Wait for {@link #connectImmediate(IRPMService)} to finish.
     */
    default void awaitConnectImmediate(RPMServiceManager rpmServiceManager, int timeout, TimeUnit timeUnit) {
        final CountDownLatch latch = new CountDownLatch(1);
        final ConnectionListener listener = new ConnectionListener() {
            @Override
            public void connected(IRPMService rpmService, AgentConfig agentConfig) {
                System.err.println("Connected");
                latch.countDown();
            }

            @Override
            public void disconnected(IRPMService rpmService) {
            }
        };
        rpmServiceManager.addConnectionListener(listener);
        connectImmediate(rpmServiceManager.getRPMService());
        try {
            if (!rpmServiceManager.getRPMService().isConnected()) {
                latch.await(timeout, timeUnit);
            }
        } catch (InterruptedException e) {
            Agent.LOG.log(Level.FINER, e, e.getMessage());
        } finally {
            rpmServiceManager.removeConnectionListener(listener);
        }
    }
}

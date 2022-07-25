/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.jetty.server;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import org.eclipse.jetty.util.thread.ThreadPool;

import java.text.MessageFormat;

public class JettySampler implements Runnable {

    private final org.eclipse.jetty.server.Server server;

    public JettySampler(org.eclipse.jetty.server.Server server) {
        this.server = server;

        AgentBridge.publicApi.setServerInfo("jetty", org.eclipse.jetty.server.Server.getVersion());

        reportServerPort(server);
    }

    private void reportServerPort(Server server) {
        for (Connector connector : server.getConnectors()) {
            if (connector instanceof NetworkConnector) {
                AgentBridge.publicApi.setAppServerPort(((NetworkConnector) connector).getPort());
                return;
            }
        }
    }

    @Override
    public void run() {

        // we probably don't need to compute this each sample period
        int acceptorThreadCount = 0;
        for (Connector connector : server.getConnectors()) {
            if (connector instanceof AbstractConnector) {
                acceptorThreadCount += ((AbstractConnector) connector).getAcceptors();
            }
        }

        final ThreadPool threadPool = server.getThreadPool();
        if (threadPool != null) {
            final String name = threadPool.getClass().getSimpleName();

            int totalThreads, busyThreads;

            int threadCount = threadPool.getThreads();
            int idleCount = threadPool.getIdleThreads();

            // Jetty9 ExecutorThreadPool doesn't expose getMaxThreads(), so just approximate usage
            int maxThreadCount = threadCount;

            totalThreads = maxThreadCount;
            busyThreads = threadCount - idleCount;

            totalThreads -= acceptorThreadCount;
            busyThreads -= acceptorThreadCount;

            String metricName = MessageFormat.format("RequestThreads/{0}/total", name);
            NewRelic.recordMetric(metricName, totalThreads);

            metricName = MessageFormat.format("RequestThreads/{0}/busy", name);
            NewRelic.recordMetric(metricName, busyThreads);

            float instanceBusy = 0;
            if (totalThreads > 0) {
                instanceBusy = ((float) busyThreads) / ((float) totalThreads);
            }
            metricName = "Instance/Busy";
            NewRelic.recordMetric(metricName, instanceBusy);
        }
    }
}

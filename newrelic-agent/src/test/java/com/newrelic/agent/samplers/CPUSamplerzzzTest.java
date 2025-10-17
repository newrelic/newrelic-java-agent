/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.samplers;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.profile.ProfilerService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.CountStats;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsEngineImpl;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CPUSamplerzzzTest {

    private MockServiceManager createServiceManager(Map<String, Object> map) throws Exception {

        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);

        ThreadService threadService = new ThreadService();
        serviceManager.setThreadService(threadService);

        ConfigService configService = ConfigServiceFactory.createConfigService(AgentConfigImpl.createAgentConfig(map),
                map);
        serviceManager.setConfigService(configService);

        TransactionService transactionService = new TransactionService();
        serviceManager.setTransactionService(transactionService);

        ProfilerService profilerService = new ProfilerService();
        serviceManager.setProfilerService(profilerService);

        return serviceManager;
    }

    @Test
    public void test() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("host", "localhost");
        map.put("port", 3000);
        map.put("ssl", false);
        map.put("license_key", "bootstrap_newrelic_admin_license_key_000");

        createServiceManager(map);

        StatsEngine statsEngine = new StatsEngineImpl();
        CPUHarvester harvester = new CPUHarvester();

        for (int i = 0; i < 10000; i++) {
            harvester.recordCPU(statsEngine);
            statsEngine.getMetricNames();
        }

        List<MetricName> harvest = statsEngine.getMetricNames();
        CountStats stats = null;
        for (MetricName data : harvest) {
            if (MetricNames.CPU.equals(data.getName())) {
                stats = (CountStats) statsEngine.getStats(data);
                break;
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(out);
        stats.writeJSONString(writer);
        writer.close();

        System.err.println(out.toString());
    }
}

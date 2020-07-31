/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.samplers;

import java.io.File;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;

public class CPUSamplerService extends AbstractService implements HarvestListener {

    private final boolean enabled;
    private final IAgentLogger logger;
    private volatile AbstractCPUSampler cpuSampler;

    public CPUSamplerService() {
        super(CPUSamplerService.class.getSimpleName());
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        enabled = config.isCpuSamplingEnabled();
        logger = Agent.LOG.getChildLogger(this.getClass());
        if (!enabled) {
            logger.info("CPU Sampling is disabled");
        }
    }

    @Override
    protected void doStart() {
        if (enabled) {
            cpuSampler = createCPUSampler();
            if (cpuSampler != null) {
                logger.fine("Started CPU Sampler");
                ServiceFactory.getHarvestService().addHarvestListener(this);
            }
        }
    }

    @Override
    protected void doStop() {
        if (cpuSampler != null) {
            ServiceFactory.getHarvestService().removeHarvestListener(this);
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void beforeHarvest(String appName, StatsEngine statsEngine) {
        if (cpuSampler != null) {
            cpuSampler.recordCPU(statsEngine);
        }
    }

    @Override
    public void afterHarvest(String appName) {
        // ignore
    }

    private AbstractCPUSampler createCPUSampler() {
        try {
            AgentBridge.getAgent().getClass().getClassLoader().loadClass("com.sun.management.OperatingSystemMXBean");
            return new CPUHarvester();
        } catch (Exception e) {
            // ignore
        }

        try {
            int pid = ServiceFactory.getEnvironmentService().getProcessPID();
            File procStatFile = new File("/proc/" + pid + "/stat");
            if (procStatFile.exists()) {
                return new ProcStatCPUSampler(procStatFile);
            }

            String osName = System.getProperty("os.name");
            if ("windows".equals(osName.toLowerCase())) {
                // return new WindowsCPUSampler(agent);
                logger.warning("CPU sampling is currently unsupported on Windows platforms for non-Sun JVMs");
                return null;
            }
        } catch (Exception e) {
            logger.warning("An error occurred starting the CPU sampler");
            logger.log(Level.FINER, "CPU sampler error", e);
            return null;
        }

        logger.warning("CPU sampling is currently only supported in Sun JVMs");
        return null;
    }
}

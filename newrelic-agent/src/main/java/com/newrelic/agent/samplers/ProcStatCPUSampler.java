/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.samplers;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.weave.utils.Streams;

public class ProcStatCPUSampler extends AbstractCPUSampler {
    private final File statFile;
    private final long clockTicksPerSecond;

    public ProcStatCPUSampler(File statFile) throws Exception {
        this.statFile = statFile;

        clockTicksPerSecond = getClockTicksPerSecond();
        readCPUStats();
    }

    private long getClockTicksPerSecond() {
        long defaultClockTicks = 100l;
        // if needed, we can change the default clock tick here based on the platform
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        return config.getProperty("clock_ticks_per_second", defaultClockTicks);
    }

    @Override
    protected double getProcessCpuTime() {
        try {
            CPUStats stats = readCPUStats();
            Agent.LOG.finest("CPU Stats " + stats);
            if (stats == null) {
                return 0;
            } else {
                return stats.getSystemTime() + stats.getUserTime();
            }
        } catch (IOException e) {
            return 0;
        }
    }

    private CPUStats readCPUStats() throws IOException {
        ByteArrayOutputStream oStream = new ByteArrayOutputStream(Streams.DEFAULT_BUFFER_SIZE);
        String userTime = "", systemTime = "";

        try (FileInputStream iStream = new FileInputStream(statFile)) {
            Streams.copy(iStream, oStream);

            oStream.close();
            String[] stats = oStream.toString().split(" ");

            if (stats.length > 13) {
                userTime = stats[13];
                systemTime = stats[14];
                return new CPUStats(Long.parseLong(userTime), Long.parseLong(systemTime));
            }
        } catch (NumberFormatException e) {
            Agent.LOG.fine(MessageFormat.format("Badly formatted CPU jiffies: ''{0}'' user, ''{1}'' system", userTime,
                    systemTime));
            return null;
        }
        return null;
    }

    private class CPUStats {
        private final double userTime;
        private final double systemTime;

        public CPUStats(long userTime, long systemTime) {
            super();

            this.userTime = userTime / clockTicksPerSecond;
            this.systemTime = systemTime / clockTicksPerSecond;
        }

        public double getUserTime() {
            return userTime;
        }

        public double getSystemTime() {
            return systemTime;
        }

        @Override
        public String toString() {
            return "User: " + userTime + ", System: " + systemTime;
        }
    }

}

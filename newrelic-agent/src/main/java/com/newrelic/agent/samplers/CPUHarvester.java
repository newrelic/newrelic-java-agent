/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.samplers;

import java.lang.management.ManagementFactory;

import com.newrelic.agent.util.TimeConversion;
import com.sun.management.OperatingSystemMXBean;

/**
 * Samples CPU utilization using JMX. Sun Java 1.5 required.
 */
public class CPUHarvester extends AbstractCPUSampler {
    private final OperatingSystemMXBean osMBean;

    public CPUHarvester() {
        super();
        osMBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    }

    @Override
    protected double getProcessCpuTime() {
        return TimeConversion.convertNanosToSeconds(osMBean.getProcessCpuTime());
    }

}

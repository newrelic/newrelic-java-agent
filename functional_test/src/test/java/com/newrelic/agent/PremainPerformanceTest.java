/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import com.newrelic.agent.service.ServiceTiming;

public class PremainPerformanceTest {

    /**
     * Runs through a simple agent startup and prints out basic performance information
     */
    @Test
    public void simpleStartupTest() {
        long agentPremainTimeInMillis = Agent.getAgentPremainTimeInMillis();
        Set<ServiceTiming.ServiceNameAndTime> serviceInitializationTimings = ServiceTiming.getServiceInitializationTimings();
        Set<ServiceTiming.ServiceNameAndTime> serviceStartTimings = ServiceTiming.getServiceStartTimings();

        System.out.println("Premain: " + agentPremainTimeInMillis + "ms");
        for (ServiceTiming.ServiceNameAndTime serviceInitializationTiming : serviceInitializationTimings) {
            System.out.println("Service Init: " + serviceInitializationTiming.getServiceName() + ", Time: "
                    + TimeUnit.NANOSECONDS.toMillis(serviceInitializationTiming.getTime()) + "ms");
        }
        for (ServiceTiming.ServiceNameAndTime serviceStartTiming : serviceStartTimings) {
            System.out.println("Service Start: " + serviceStartTiming.getServiceName() + ", Time: "
                    + TimeUnit.NANOSECONDS.toMillis(serviceStartTiming.getTime()) + "ms");
        }

        // Let's do a very loose assertion that the premain time is sane.
        Assert.assertTrue(agentPremainTimeInMillis < 60*1000);
    }

}

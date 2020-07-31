/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.quartz;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TracedMetricData;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.quartz" })
public class QuartzSystemTest {

    static final JobDetail JOB_DETAIL = new JobDetail("jobname", "jobgroup", HelloJob.class);
    static final Trigger TRIGGER = new org.quartz.SimpleTrigger("trigger1", "group1");

    @Test
    public void ramJobStoreTransactionNameAndMetricTest() throws SchedulerException {
        Scheduler scheduler = new StdSchedulerFactory().getScheduler();
        scheduler.start();
        scheduler.scheduleJob(JOB_DETAIL, TRIGGER);
        scheduler.shutdown();
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String expectedTransactionName = "OtherTransaction/Java/org.quartz.simpl.RAMJobStore/acquireNextTrigger";
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(expectedTransactionName);
        assertEquals(1, metrics.get("Java/org.quartz.simpl.RAMJobStore/acquireNextTrigger").getCallCount());

    }
}

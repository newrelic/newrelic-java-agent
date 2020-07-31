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
import org.quartz.TriggerBuilder;
import org.quartz.core.jmx.JobDetailSupport;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.StdSchedulerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.quartz" })
public class QuartzSystemTest {

    @Test
    public void ramJobStoreTransactionNameAndMetricTest() throws Exception {
        JobDetail detail = buildJobDetail();
        final Trigger trigger = TriggerBuilder.newTrigger().build();
        Scheduler scheduler = new StdSchedulerFactory().getScheduler();
        scheduler.start();
        scheduler.scheduleJob(detail, trigger);
        scheduler.shutdown();
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String expectedTransactionName = "OtherTransaction/Java/org.quartz.simpl.RAMJobStore/acquireNextTriggers";
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(expectedTransactionName);
        assertEquals(1, metrics.get("Java/org.quartz.simpl.RAMJobStore/acquireNextTriggers").getCallCount());

    }

    protected JobDetail buildJobDetail() throws ClassNotFoundException {
        Map<String, Object> jobDetail = new HashMap<>();
        jobDetail.put("name", "jobname");
        jobDetail.put("group", "jobgroup");
        jobDetail.put("description", "jobdescr");
        jobDetail.put("jobClass", HelloJob.class.getName());
        return JobDetailSupport.newJobDetail(jobDetail);
    }
}

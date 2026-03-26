/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.threads.BasicThreadInfo;
import com.newrelic.agent.threads.ThreadNameNormalizer;
import com.newrelic.agent.threads.ThreadNames;
import com.newrelic.agent.threads.ThreadStateSampler;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * The thread service tracks the IDs of agent threads.
 */
public class ThreadService extends AbstractService implements ThreadNames {

    public static final String NAME_PATTERN_CFG_KEY = "thread_sampler.name_pattern";
    private final Map<Long, Boolean> agentThreadIds;
    private final Map<Long, String> threadIdToName = AgentBridge.collectionFactory.createConcurrentAccessTimeBasedEvictionMap(300, 32);
    private volatile ThreadNameNormalizer threadNameNormalizer;

    public ThreadService() {
        super(ThreadService.class.getSimpleName());
        agentThreadIds = new ConcurrentHashMap<>(6);
    }

    @Override
    protected void doStart() throws Exception {
        threadNameNormalizer = new ThreadNameNormalizer(ServiceFactory.getConfigService().getDefaultAgentConfig(), this);
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        if (config.getValue("thread_sampler.enabled", Boolean.TRUE)) {
            long sampleDelayInSeconds = config.getValue("thread_sampler.sample_delay_in_seconds", 60);
            long samplePeriodInSeconds = config.getValue("thread_sampler.sample_period_in_seconds", 60);

            if (samplePeriodInSeconds > 0) {
                ThreadStateSampler threadStateSampler = new ThreadStateSampler(ManagementFactory.getThreadMXBean(), threadNameNormalizer);
                ServiceFactory.getSamplerService().addSampler(threadStateSampler, sampleDelayInSeconds, samplePeriodInSeconds, TimeUnit.SECONDS);
            } else {
                Agent.LOG.log(Level.FINE, "The thread sampler is disabled because the sample period is {}", samplePeriodInSeconds);
            }
        } else {
            Agent.LOG.log(Level.FINE, "The thread sampler is disabled");
        }
    }

    @Override
    protected void doStop() throws Exception {
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public boolean isCurrentThreadAnAgentThread() {
        return Thread.currentThread() instanceof AgentThread;
    }

    public boolean isAgentThreadId(Long threadId) {
        return agentThreadIds.containsKey(threadId);
    }

    public ThreadNameNormalizer getThreadNameNormalizer() {
        return threadNameNormalizer;
    }

    @Override
    public String getThreadName(final BasicThreadInfo threadInfo) {
        return threadIdToName.computeIfAbsent(threadInfo.getId(), id -> threadInfo.getName());
    }

    /**
     * Get the IDs of threads owned by the agent.
     */
    public Set<Long> getAgentThreadIds() {
        return Collections.unmodifiableSet(agentThreadIds.keySet());
    }

    /**
     * Add the id of an agent thread.
     */
    public void registerAgentThreadId(long id) {
        agentThreadIds.put(id, Boolean.TRUE);
    }

    /**
     * A marker to identify agent threads.
     */
    public interface AgentThread {
    }

}

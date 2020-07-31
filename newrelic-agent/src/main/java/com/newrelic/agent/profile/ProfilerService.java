/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile;

import java.text.MessageFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.profile.v2.TransactionProfileService;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.DefaultThreadFactory;
import com.newrelic.agent.util.SafeWrappers;

public class ProfilerService extends AbstractService implements ProfilerControl {

    private static final String V2_PROFILER_FORMAT = "v2";

    private static final String PROFILER_THREAD_NAME = "New Relic Profiler Service";

    private ProfileSession currentSession;
    private final ScheduledExecutorService scheduledExecutor;
    private final com.newrelic.agent.profile.v2.ProfilerService newProfilerService;

    public ProfilerService() {
        super(ProfilerService.class.getSimpleName());
        ThreadFactory threadFactory = new DefaultThreadFactory(PROFILER_THREAD_NAME, true);
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);

        newProfilerService = new com.newrelic.agent.profile.v2.ProfilerService(scheduledExecutor);
    }

    @Override
    public boolean isEnabled() {
        return ServiceFactory.getConfigService().getDefaultAgentConfig().getThreadProfilerConfig().isEnabled();
    }

    @Override
    public synchronized void startProfiler(ProfilerParameters parameters) {
        
        if (V2_PROFILER_FORMAT.equals(parameters.getProfilerFormat())) {
            newProfilerService.startProfiler(parameters);
            return;
        }
        
        long samplePeriodInMillis = parameters.getSamplePeriodInMillis();
        long durationInMillis = parameters.getDurationInMillis();
        boolean enabled = ServiceFactory.getConfigService().getDefaultAgentConfig().getThreadProfilerConfig().isEnabled();
        if (!enabled || samplePeriodInMillis <= 0 || durationInMillis <= 0 || samplePeriodInMillis > durationInMillis) {
            getLogger().info(
                    MessageFormat.format(
                            "Ignoring the start profiler command: enabled={0}, samplePeriodInMillis={1}, durationInMillis={2}",
                            enabled, samplePeriodInMillis, durationInMillis));
            return;
        }

        ProfileSession oldSession = currentSession;
        if (oldSession != null && !oldSession.isDone()) {
            getLogger().info(
                    MessageFormat.format(
                            "Ignoring the start profiler command because a session is currently active. {0}",
                            oldSession.getProfileId()));
            return;
        }
        ProfileSession newSession = createProfileSession(parameters);
        newSession.start();
        currentSession = newSession;
    }

    @Override
    public synchronized int stopProfiler(Long profileId, boolean shouldReport) {
        ProfileSession session = currentSession;
        if (session != null && profileId.equals(session.getProfileId())) {
            session.stop(shouldReport);
            return 0;
        }
        return newProfilerService.stopProfiler(profileId, shouldReport);
    }

    synchronized void sessionCompleted(ProfileSession session) {
        if (currentSession != session) {
            return;
        }
        currentSession = null;
    }

    protected ProfileSession createProfileSession(ProfilerParameters parameters) {
        return new ProfileSession(this, parameters);
    }

    protected ScheduledExecutorService getScheduledExecutorService() {
        return SafeWrappers.safeExecutor(scheduledExecutor);
    }

    @Override
    protected void doStart() {
        addCommands();

        try {
            newProfilerService.start();
        } catch (Exception e) {
            Agent.LOG.log(Level.FINEST, e, e.getMessage());
        }
    }

    protected synchronized ProfileSession getCurrentSession() {
        return currentSession;
    }

    private void addCommands() {
        ServiceFactory.getCommandParser().addCommands(new StartProfilerCommand(this));
        ServiceFactory.getCommandParser().addCommands(new StopProfilerCommand(this));
    }

    @Override
    protected void doStop() {
        
        try {
            newProfilerService.stop();
        } catch (Exception e) {
            Agent.LOG.log(Level.FINEST, e, e.getMessage());
        }
        
        ProfileSession session = getCurrentSession();
        if (session != null) {
            session.stop(false);
        }

        if (scheduledExecutor != null) {
            scheduledExecutor.shutdown();
            try {
                if (!scheduledExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    getLogger().log(Level.FINE, "Profiler Service executor service did not terminate");
                }
            } catch (InterruptedException e) {
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
        }
    }

    public TransactionProfileService getTransactionProfileService() {
        return newProfilerService.getTransactionProfileService();
    }
}

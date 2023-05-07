/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile.v2;

import java.text.MessageFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionListener;
import com.newrelic.agent.profile.ProfilerControl;
import com.newrelic.agent.profile.ProfilerParameters;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.util.DefaultThreadFactory;
import com.newrelic.agent.util.SafeWrappers;

public class ProfilerService extends AbstractService implements ProfilerControl {

    private static final String PROFILER_THREAD_NAME = "New Relic Profiler Service";

    private volatile ProfileSession currentSession;
    private final ScheduledExecutorService scheduledExecutor;
    private final TransactionListener transactionListener = (transactionData, transactionStats) -> transactionFinished(transactionData);

    private final TransactionProfileService transactionProfileService;

    public ProfilerService() {
        this(Executors.newSingleThreadScheduledExecutor(new DefaultThreadFactory(PROFILER_THREAD_NAME, true)));
    }
    
    public ProfilerService(ScheduledExecutorService scheduledExecutorService) {
        super(ProfilerService.class.getSimpleName());
        this.scheduledExecutor = scheduledExecutorService;
        this.transactionProfileService = new TransactionProfileServiceImpl();
    }

    protected void transactionFinished(TransactionData transactionData) {
        ProfileSession session = getCurrentSession();
        if (null != session) {
            session.transactionFinished(transactionData);
        }
    }

    @Override
    public boolean isEnabled() {
        return ServiceFactory.getConfigService().getDefaultAgentConfig().getThreadProfilerConfig().isEnabled();
    }

    @Override
    public synchronized void startProfiler(ProfilerParameters parameters) {
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
        return -1;
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
    }

    protected ProfileSession getCurrentSession() {
        return currentSession;
    }

    @Override
    protected void doStop() {
        ProfileSession session = getCurrentSession();
        if (session != null) {
            session.stop(false);
        }
        
        ServiceFactory.getTransactionService().removeTransactionListener(transactionListener);

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
        return transactionProfileService;
    }
    
    private class TransactionProfileServiceImpl implements TransactionProfileService {

        @Override
        public boolean isTransactionProfileSessionActive() {
            return getTransactionProfileSession().isActive();
        }

        @Override
        public TransactionProfileSession getTransactionProfileSession() {
            if (ServiceFactory.getServiceManager().getCircuitBreakerService().isTripped()) {
                return TransactionProfileSessionImpl.NO_OP_TRANSACTION_PROFILE_SESSION;
            }
            ProfileSession session = currentSession;
            return session == null ? TransactionProfileSessionImpl.NO_OP_TRANSACTION_PROFILE_SESSION : session.getProfile().getTransactionProfileSession();
        }
        
    }
}

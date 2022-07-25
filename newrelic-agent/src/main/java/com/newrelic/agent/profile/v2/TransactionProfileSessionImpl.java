/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile.v2;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.simple.JSONObject;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.profile.ThreadType;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.threads.BasicThreadInfo;
import com.newrelic.agent.threads.ThreadNameNormalizer;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.IgnoreChildSocketCalls;
import com.newrelic.agent.tracers.Tracer;

public class TransactionProfileSessionImpl implements TransactionProfileSession {
    
    static final TransactionProfileService NO_OP_TRANSACTION_PROFILE_SERVICE = new TransactionProfileService() {

        @Override
        public boolean isTransactionProfileSessionActive() {
            return false;
        }

        @Override
        public TransactionProfileSession getTransactionProfileSession() {
            return NO_OP_TRANSACTION_PROFILE_SESSION;
        }
        
    };
    
    static final TransactionProfileSession NO_OP_TRANSACTION_PROFILE_SESSION = new TransactionProfileSession() {
        
        @Override
        public void writeJSONString(Writer out) throws IOException {
            JSONObject.writeJSONString(ImmutableMap.of(), out);
        }
        
        @Override
        public void transactionFinished(TransactionData transactionData) { }

        @Override
        public boolean isActive() {
            return false;
        }

        @Override
        public void noticeTracerStart(int signatureId, int tracerFlags, Tracer tracer) {
        }
    };

    private static final int STACK_CAPTURE_LIMIT_PER_METRIC_PER_PERIOD = 5;
    
    private final ThreadService threadService;
    /**
     * Transaction name to a transaction profile.
     */
    private final LoadingCache<String, TransactionProfile> transactionProfileTrees;

    private final DiscoveryProfile discoveryProfile;
    
    public TransactionProfileSessionImpl(final Profile profile, final ThreadNameNormalizer threadNameNormalizer) {
        this(profile, threadNameNormalizer, ServiceFactory.getThreadService());
    }
    
    private final LoadingCache<String, AtomicInteger> stackTraceLimits;
    private final Profile profile;
    
    protected TransactionProfileSessionImpl(final Profile profile, final ThreadNameNormalizer threadNameNormalizer, ThreadService threadService) {
        this.threadService = threadService;
        this.profile = profile; 
        this.transactionProfileTrees =
                Caffeine.newBuilder().executor(Runnable::run).build(
                        transactionName -> new TransactionProfile(profile, threadNameNormalizer));
        this.discoveryProfile = new DiscoveryProfile(profile, threadNameNormalizer);
        this.stackTraceLimits =
                Caffeine.newBuilder().expireAfterAccess(5, TimeUnit.SECONDS).executor(Runnable::run).build(
                        metricName -> new AtomicInteger(0));
    }

    @Override
    public void transactionFinished(TransactionData transactionData) {
        TransactionProfile txProfile = transactionProfileTrees.get(transactionData.getBlameMetricName());
        txProfile.transactionFinished(transactionData);
    }

    @Override
    public void writeJSONString(Writer out) throws IOException {
        ImmutableMap<String, Object> map = ImmutableMap.of(
                "transactions", transactionProfileTrees.asMap(),
                "discovery", discoveryProfile);
        JSONObject.writeJSONString(map, out);
    }
    

    @Override
    public void noticeTracerStart(int signatureId, int tracerFlags, Tracer tracer) {
        if (!threadService.isCurrentThreadAnAgentThread()) {
            if (tracer == null) {
                discoveryProfile.noticeStartTracer(signatureId);
                Thread currentThread = Thread.currentThread();
                profile.addStackTrace(new BasicThreadInfo(currentThread), currentThread.getStackTrace(), true, ThreadType.BasicThreadType.OTHER);
            } else if (tracer.isLeaf() || tracer instanceof IgnoreChildSocketCalls) {
                if (stackTraceLimits.get(tracer.getMetricName()).getAndIncrement() <
                        STACK_CAPTURE_LIMIT_PER_METRIC_PER_PERIOD) {
                    tracer.setAgentAttribute(DefaultTracer.BACKTRACE_PARAMETER_NAME, Thread.currentThread().getStackTrace());
                }
            }
        }
    }

    @Override
    public boolean isActive() {
        return !ServiceFactory.getServiceManager().getCircuitBreakerService().isTripped();
    }
}

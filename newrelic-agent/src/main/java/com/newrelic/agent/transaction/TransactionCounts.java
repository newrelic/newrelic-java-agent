/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.TransactionTracerConfig;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class TransactionCounts {

    private static final int APPROX_TRACER_SIZE = 128;
    private final int maxTransactionSize;
    private final int maxSegments;
    private final int maxTokens;
    private final AtomicInteger transactionSize = new AtomicInteger(0);
    private final AtomicInteger segmentCount = new AtomicInteger(0);
    private final AtomicInteger explainPlanCount = new AtomicInteger(0);
    private final AtomicInteger stackTraceCount = new AtomicInteger(0);
    private final AtomicInteger tokenCount = new AtomicInteger(0);
    private volatile boolean overSegmentLimit;
    private volatile boolean overTokenLimit;

    public TransactionCounts(AgentConfig config) {
        TransactionTracerConfig transactionTracerConfig = config.getTransactionTracerConfig();
        maxSegments = transactionTracerConfig.getMaxSegments();
        maxTransactionSize = config.getTransactionSizeLimit();
        maxTokens = transactionTracerConfig.getMaxTokens();
    }

    public void incrementSize(int size) {
        transactionSize.addAndGet(size);
    }

    public int getTransactionSize() {
        return transactionSize.intValue();
    }

    public void addTracer() {
        int count = segmentCount.incrementAndGet();
        transactionSize.addAndGet(APPROX_TRACER_SIZE);
        overSegmentLimit = count > maxSegments;
    }

    public void addTracers(int numberOfTracersToAdd) {
        int count = segmentCount.addAndGet(numberOfTracersToAdd);
        transactionSize.addAndGet(APPROX_TRACER_SIZE * numberOfTracersToAdd);
        overSegmentLimit = count > maxSegments;
    }

    public void getToken() {
        int count = tokenCount.incrementAndGet();
        overTokenLimit = count > maxTokens;
    }

    public boolean isOverTracerSegmentLimit() {
        return overSegmentLimit;
    }

    public boolean isOverTokenLimit() {
        return overTokenLimit;
    }

    public int getTokenCount() {
        return tokenCount.get();
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public int getSegmentCount() {
        return segmentCount.get();
    }

    public boolean isOverTransactionSize() {
        return transactionSize.intValue() > maxTransactionSize;
    }

    public boolean shouldGenerateTransactionSegment() {
        return !(isOverTracerSegmentLimit() || isOverTransactionSize());
    }

    public void incrementStackTraceCount() {
        stackTraceCount.incrementAndGet();
    }

    public int getStackTraceCount() {
        return stackTraceCount.intValue();
    }

    /**
     * Returns the number of explain plans that will be run for this transaction.
     * 
     */
    public int getExplainPlanCount() {
        return explainPlanCount.intValue();
    }

    public void incrementExplainPlanCountAndLogIfReachedMax(int max) {
        int updatedVal = explainPlanCount.incrementAndGet();
        if (updatedVal == max) {
            Agent.LOG.log(Level.FINER, "Reached the maximum number of explain plans.");
        }
    }

}

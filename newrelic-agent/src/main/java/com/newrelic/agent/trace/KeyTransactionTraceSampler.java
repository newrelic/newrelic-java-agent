/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.trace;

import com.newrelic.agent.Agent;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.config.AgentConfig;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class KeyTransactionTraceSampler extends TransactionTraceSampler {

    @Override
    protected boolean exceedsThreshold(TransactionData td) {
        if (!td.getAgentConfig().isApdexTSet(td.getBlameMetricName())) {
            return false;
        }

        long apdexTInNanos = getApdexTInNanos(td);

        if (td.getLegacyDuration() > apdexTInNanos) {
            return true;
        } else {
            Agent.LOG.log(Level.FINER, "Key transaction trace threshold not exceeded {0}. Threshold is {1}", td, TimeUnit.MILLISECONDS.convert(apdexTInNanos, TimeUnit.NANOSECONDS));
            return false;
        }
    }

    @Override
    protected long getScore(TransactionData td) {
        return (100 * td.getLegacyDuration()) / getApdexTInNanos(td);
    }

    private long getApdexTInNanos(TransactionData td) {
        AgentConfig agentConfig = td.getAgentConfig();
        long apdexTInMillis = agentConfig.getApdexTInMillis(td.getBlameMetricName());
        if (apdexTInMillis <= 0 ) {
            Agent.LOG.log(Level.FINE, "Invalid Apdex for key transaction {0}: {1}", td.getBlameMetricName(), apdexTInMillis);
            return 1;
        }

        return TimeUnit.NANOSECONDS.convert(apdexTInMillis, TimeUnit.MILLISECONDS);
    }
}

/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.extension;

import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionListener;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.Tracer;
import org.junit.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class CheckTTTransactionListener implements TransactionListener {

    // The key is the metric name. The value is the true if transaction trace segment should be enabled or false if
    // it should not
    private final Map<String, Boolean> expectedMetricNameToTTEnabled;
    private final Map<String, Boolean> actualMetricNameToTTEnabled;

    public CheckTTTransactionListener(Map<String, Boolean> pMetricNameToTTEndabled) {
        super();
        expectedMetricNameToTTEnabled = pMetricNameToTTEndabled;
        actualMetricNameToTTEnabled = new HashMap<>();
    }

    public void start() {
        ServiceFactory.getTransactionService().addTransactionListener(this);
    }

    public void stop() {
        ServiceFactory.getTransactionService().removeTransactionListener(this);
    }

    @Override
    public void dispatcherTransactionFinished(TransactionData pTransactionData, TransactionStats pTransactionStats) {
        for (Tracer current : pTransactionData.getTracers()) {
            String metricName = current.getMetricName();
            actualMetricNameToTTEnabled.put(metricName, Boolean.valueOf(current.isTransactionSegment()));
        }
    }

    public void verifyAllMetrics() {
        for (Entry<String, Boolean> current : expectedMetricNameToTTEnabled.entrySet()) {
            Boolean actualValue = actualMetricNameToTTEnabled.get(current.getKey());
            if (current.getValue()) {
                Assert.assertNotNull("There should have been a tracer with the metric name " + current.getKey(),
                        actualValue);
                Assert.assertEquals("The values did not match for the metric name" + current.getKey(),
                        current.getValue(), actualValue);
            } else {
                Assert.assertNull("There should have NOT been a tracer with the metric name " + current.getKey(),
                        actualValue);
            }

        }
    }
}

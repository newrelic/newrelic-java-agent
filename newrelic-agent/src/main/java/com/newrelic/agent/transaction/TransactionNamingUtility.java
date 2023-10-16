/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.api.agent.NewRelic;

import java.util.logging.Level;

public class TransactionNamingUtility {
    /**
     * @param one  Priority name to compare.
     * @param two Priority name to compare.
     * @param scheme TransactionNamingScheme to use for comparison
     * @return true if priority one is greater than priority two. false otherwise.
     */
    public static boolean isGreaterThan(TransactionNamePriority one, TransactionNamePriority two, TransactionNamingScheme scheme) {
        return comparePriority(one, two, scheme) > 0;
    }

    /**
     * @param one  Priority name to compare.
     * @param two Priority name to compare.
     * @param scheme TransactionNamingScheme to use for comparison
     * @return true if priority one is less than priority two. false otherwise.
     */
    public static boolean isLessThan(TransactionNamePriority one, TransactionNamePriority two, TransactionNamingScheme scheme) {
        return comparePriority(one, two, scheme) < 0;
    }

    /**
     * @param one  Priority name to compare.
     * @param two Priority name to compare.
     * @param scheme TransactionNamingScheme to use for comparison
     * @return a negative number, zero, or a positive number, as priority one is less than, equal to, or greater than priority two.
     */
    public static int comparePriority(TransactionNamePriority one, TransactionNamePriority two,
            TransactionNamingScheme scheme) {
        if (TransactionNamingScheme.RESOURCE_BASED.equals(scheme)) {
            if (NewRelic.getAgent().getLogger().isLoggable(Level.FINEST)) {
                Agent.LOG.log(Level.FINEST, "agent.transaction.TransactionNamingUtility::comparePriority: pathPriorityOneValue: {0}, " +
                                "pathPriorityTwoValue: {1}; return val will be {2}",
                        one.pathPriority, two.pathPriority, one.pathPriority - two.pathPriority);
            }
            return one.pathPriority - two.pathPriority;
        }
        else {
            if (NewRelic.getAgent().getLogger().isLoggable(Level.FINEST)) {
                Agent.LOG.log(Level.FINEST, "agent.transaction.TransactionNamingUtility::comparePriority: legacyPriorityOneValue: {0}, " +
                                "legacyPriorityTwoValue: {1}; return val will be {2}",
                        one.legacyPriority, two.legacyPriority, one.legacyPriority - two.legacyPriority);
            }
            return one.legacyPriority - two.legacyPriority;
        }
    }
}

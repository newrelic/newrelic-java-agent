/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import com.newrelic.agent.bridge.TransactionNamePriority;

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
            return one.pathPriority - two.pathPriority;
        }
        else {
            return one.legacyPriority - two.legacyPriority;
        }
    }
}

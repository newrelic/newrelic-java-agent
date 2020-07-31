/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

public class DisabledDestinationPredicate implements DestinationPredicate {

    @Override
    public boolean apply(String input) {
        return false;
    }

    @Override
    public boolean isPotentialConfigMatch(String key) {
        return false;
    }

}

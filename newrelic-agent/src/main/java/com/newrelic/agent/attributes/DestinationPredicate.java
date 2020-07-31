/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import com.google.common.base.Predicate;

public interface DestinationPredicate extends Predicate<String> {

    boolean isPotentialConfigMatch(String key);

}

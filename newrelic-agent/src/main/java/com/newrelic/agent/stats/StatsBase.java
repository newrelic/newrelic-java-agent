/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;

import org.json.simple.JSONStreamAware;

public interface StatsBase extends Cloneable, JSONStreamAware {

    boolean hasData();

    void reset();

    /**
     * Merge the given stats into this object.
     */
    void merge(StatsBase stats);

    /**
     * Every stats base should be cloneable.
     * 
     * @return A clone of the object.
     * @throws CloneNotSupportedException Thrown if the object can not be cloned. All StatsBase should be clonable.
     */
    Object clone() throws CloneNotSupportedException;

}

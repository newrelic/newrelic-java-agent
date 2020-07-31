/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx;

public enum JmxType {

    /**
     * A simple JMX metric will record the value for at each minute.
     */
    SIMPLE("simple"),
    /** A monotonically increasing type will record the rate of each for each minute. */
    MONOTONICALLY_INCREASING("monotonically_increasing");

    /**
     * Name found in the yml configuration file.
     */
    private String ymlName;

    JmxType(final String pYmlName) {
        ymlName = pYmlName;
    }

    /**
     * Gets the field ymlName.
     * 
     * @return the ymlName
     */
    public String getYmlName() {
        return ymlName;
    }

}

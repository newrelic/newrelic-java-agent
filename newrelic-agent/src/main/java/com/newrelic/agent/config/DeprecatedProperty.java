/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

public class DeprecatedProperty {
    public final String[] propertyName;
    public final String[] newPropertyName;

    public DeprecatedProperty(String[] propertyName, String[] newPropertyName) {
        this.propertyName = propertyName;
        this.newPropertyName = newPropertyName;
    }
}

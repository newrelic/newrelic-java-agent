/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

public class ServerProp {

    private final Object value;

    private ServerProp(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public static ServerProp createPropObject(Object value) {
        return new ServerProp(value);
    }

    @Override
    public String toString() {
        return value == null ? "" : value.toString();
    }

}

/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

public class SystemPropertyFactory {

    private static volatile SystemPropertyProvider SYSTEM_PROPERTY_PROVIDER = new SystemPropertyProvider();

    private SystemPropertyFactory() {
    }

    public static void setSystemPropertyProvider(SystemPropertyProvider provider) {
        SYSTEM_PROPERTY_PROVIDER = provider;
    }

    public static SystemPropertyProvider getSystemPropertyProvider() {
        return SYSTEM_PROPERTY_PROVIDER;
    }

}

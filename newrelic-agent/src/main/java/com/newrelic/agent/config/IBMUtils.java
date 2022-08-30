/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IBMUtils {
    private static final String IBM_VENDOR = "IBM Corporation";

    public static boolean isIbmJVM() {
        return IBM_VENDOR.equals(System.getProperty("java.vendor"));
    }

}

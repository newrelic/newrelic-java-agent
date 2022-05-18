/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.lettuce6.instrumentation;

public class Utils {

    public static boolean initialized = false;
    private static Utils instance = null;

    public static void init() {
        if (instance == null) {
            instance = new Utils();
            initialized = true;
        }
    }

}

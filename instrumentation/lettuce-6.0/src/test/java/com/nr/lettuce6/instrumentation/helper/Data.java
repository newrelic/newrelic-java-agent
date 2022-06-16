/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.lettuce6.instrumentation.helper;

public class Data {
    public final String key;
    public final String value;

    public Data(String key, String value) {
        this.key = key;
        this.value = value;
    }
}

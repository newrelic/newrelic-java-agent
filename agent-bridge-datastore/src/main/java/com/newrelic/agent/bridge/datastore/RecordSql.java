/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.datastore;

public enum RecordSql {

    obfuscated, raw, off;

    public static RecordSql get(String value) {
        if (value == null) {
            return obfuscated;
        }
        return Enum.valueOf(RecordSql.class, value);
    }
}

/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.logging;

import java.util.Objects;

public class LogAttributeKey {
    public final String key;
    public final LogAttributeType type;

    public LogAttributeKey(String key, LogAttributeType type) {
        this.key = key;
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public String getPrefixedKey() {
        if (key == null || type == null) {
            return key;
        }
        return type.applyPrefix(key);
    }

    public LogAttributeType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LogAttributeKey that = (LogAttributeKey) o;
        return Objects.equals(key, that.key) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, type);
    }
}

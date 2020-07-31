/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracing;

enum ParentType {
    Invalid(-1),
    App(0),
    Browser(1),
    Mobile(2);

    final int value;

    ParentType(int value) {
        this.value = value;
    }

    static ParentType getParentTypeFromValue(int value) {
        for (ParentType parentType : values()) {
            if (parentType.value == value) {
                return parentType;
            }
        }
        return null;
    }
}

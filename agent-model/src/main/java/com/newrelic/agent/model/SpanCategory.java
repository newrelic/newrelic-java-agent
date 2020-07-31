/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.model;

public enum SpanCategory {

    http,
    datastore,
    generic;

    public static SpanCategory fromString(String category) {
        for (SpanCategory spanCategory : SpanCategory.values()) {
            if (spanCategory.name().equals(category)) {
                return spanCategory;
            }
        }
        return SpanCategory.generic;
    }
}

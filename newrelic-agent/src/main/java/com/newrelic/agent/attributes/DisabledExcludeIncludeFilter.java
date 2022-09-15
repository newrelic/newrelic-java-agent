/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

/**
 * Implementation of {@link ExcludeIncludeFilter} that always returns false.
 */
public class DisabledExcludeIncludeFilter implements ExcludeIncludeFilter {

    public static final ExcludeIncludeFilter INSTANCE = new DisabledExcludeIncludeFilter();
    
    private DisabledExcludeIncludeFilter() {
        // use the INSTANCE
    }

    @Override
    public boolean shouldInclude(String key) {
        return false;
    }
}

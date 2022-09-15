/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;


public interface ExcludeIncludeFilter {
    boolean shouldInclude(String key);
}

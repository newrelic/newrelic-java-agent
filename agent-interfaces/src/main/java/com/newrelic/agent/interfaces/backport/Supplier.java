/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.interfaces.backport;

public interface Supplier<T> {

    // Backfill for java 7 language support.
    // This should go away when we can drop support for java 7
    T get();
}

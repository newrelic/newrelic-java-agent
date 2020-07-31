/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import java.util.concurrent.Callable;

/**
 * A holder for Weaver extension classes.
 */
public interface ExtensionHolder<T> {
    /**
     * Return the value associated with the instance in this ExtensionHolder. If the value is not present it will be
     * initialized (in a thread-safe manner) using valueLoader.
     */
    public T getExtension(Object instance, Callable<T> valueLoader);

    /**
     * Return the value associated with the instance in this ExtensionHolder.<br/>
     * In addition, the value will be removed from the backing map.
     */
    public T getAndRemoveExtension(Object instance);
}

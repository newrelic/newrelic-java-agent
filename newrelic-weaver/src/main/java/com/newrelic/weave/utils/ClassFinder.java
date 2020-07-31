/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.utils;

import java.net.URL;

/**
 * Finds URLs for class resources.  Implementations are expected to be threadsafe.
 */
public interface ClassFinder {

    /**
     * Find the URL for the specified internal class name.
     * @param internalName internal class name
     * @return URL for the specified internal class name, or <code>null</code> if could not be found
     */
    URL findResource(String internalName);
}

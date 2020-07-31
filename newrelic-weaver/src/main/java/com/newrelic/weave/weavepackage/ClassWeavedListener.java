/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage;

import com.newrelic.weave.utils.ClassCache;

/**
 * Callback interface for listening when target classes are weaved.
 */
public interface ClassWeavedListener {

    /**
     * Called after a target class is weaved.
     */
    void classWeaved(PackageWeaveResult weaveResult, ClassLoader classloader, ClassCache cache);
}

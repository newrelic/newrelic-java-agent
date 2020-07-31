/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage;

/**
 * Interface containing callback methods for listening to lifecycle events on a weave package.
 */
public interface WeavePackageLifetimeListener {

    /**
     * Called after a weavepackage is registered
     */
    void registered(WeavePackage weavepackage);

    /**
     * Called after a weavepackage is deregistered
     */
    void deregistered(WeavePackage weavepackage);

    /**
     * Called after a weavepackage is validated against a classloader for the first time.
     */
    void validated(PackageValidationResult packageResult, ClassLoader classloader);

}

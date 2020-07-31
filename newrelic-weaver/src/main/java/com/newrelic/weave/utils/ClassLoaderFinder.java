/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.utils;

import java.net.URL;

/**
 * A {@link ClassFinder} that finds class URLs using a {@link ClassLoader}.
 */
public class ClassLoaderFinder implements ClassFinder {

    private final ClassLoader classLoader;

    public ClassLoaderFinder(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public URL findResource(String internalName) {
        return classLoader.getResource(WeaveUtils.getClassResourceName(internalName));
    }
}

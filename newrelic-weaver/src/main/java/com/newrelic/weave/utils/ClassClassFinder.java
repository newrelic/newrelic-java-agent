/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;

/**
 * A {@link ClassFinder} used to get a resource URL from a specific {@link Class} instance.
 */
public class ClassClassFinder implements ClassFinder {
    private final URL baseLocation; // usually a url to a jar
    private final boolean isJar;

    public ClassClassFinder(Class<?> clazz) {
        ProtectionDomain pd = clazz.getProtectionDomain();
        if (null != pd) {
            CodeSource cs = pd.getCodeSource();
            if (cs != null) {
            baseLocation = cs.getLocation();
                if (baseLocation != null && baseLocation.toExternalForm().endsWith(".jar")) {
                isJar = true;
                } else {
                    isJar = false;
                }
            } else {
                baseLocation = null;
                isJar = false;
            }
        } else {
            baseLocation = null;
            isJar = false;
        }
    }

    public URL findResource(String internalName) {
        URL classLocation = null;
        if (null != baseLocation) {
            try {
                if (isJar) {
                    classLocation = new URL("jar:" + baseLocation.toExternalForm() + "!/"
                            + WeaveUtils.getClassResourceName(internalName));
                } else {
                    classLocation = new URL(baseLocation, WeaveUtils.getClassResourceName(internalName));
                }
            } catch (MalformedURLException ignored) {
            }
        }
        return classLocation;
    }
}

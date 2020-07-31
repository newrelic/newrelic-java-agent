/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import java.util.HashMap;
import java.util.Map;

public enum NonUrlClassLoaders {

    /** Non url classloader for jboss 6. */
    JBOSS_6(new String[] { "org.jboss.classloader.spi.base.BaseClassLoader" }),
    /** Non url classloader for jboss 7 */
    JBOSS_7(new String[] { "org.jboss.modules.ModuleClassLoader" }),
    /** No url class loader for websphere 8. */
    WEBSHPERE_8(new String[] { "com.ibm.ws.classloader.CompoundClassLoader" }),
    /** Non url class loader for weblogic. */
    WEBLOGIC(new String[] { "weblogic.utils.classloaders.GenericClassLoader",
            "weblogic.utils.classloaders.ChangeAwareClassLoader" });

    /** Classloader to be implemented */
    private String[] classLoaderNames;

    /** Look up table for strings to loader. */
    private static Map<String, NonUrlClassLoaders> LOADERS = new HashMap<>();

    static {
        for (NonUrlClassLoaders classLoader : NonUrlClassLoaders.values()) {
            String[] classes = classLoader.classLoaderNames;
            for (String current : classes) {
                LOADERS.put(current, classLoader);
            }
        }
    }

    /**
     * 
     * Creates this NonUrlClassLoaders.
     * 
     * @param classNames The class names for the enumeration.
     */
    NonUrlClassLoaders(String[] classNames) {
        classLoaderNames = classNames;
    }

    /**
     * Gets the loader based on the input name.
     * 
     * @param loaderCanonicalName The name of the class.
     * @return The enumeration associated with the class name.
     */
    public static NonUrlClassLoaders getNonUrlType(String loaderCanonicalName) {
        if (loaderCanonicalName != null) {
            return LOADERS.get(loaderCanonicalName);
        }
        return null;
    }
}

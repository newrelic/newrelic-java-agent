/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.instrumentation.context.InstrumentationContextManager;
import com.newrelic.agent.util.asm.Utils;
import com.newrelic.weave.UtilityClass;
import com.newrelic.weave.utils.WeaveUtils;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

public class InitProblemClasses {

    /**
     * Used to load classes that sometimes load while ASM is running. This causes classloading deadlocks.
     */
    public static final void loadInitialClasses() {
        try {

            Utils.getClassResourceName(Class.forName("java.net.URL"));
            UtilityClass.class.getName();

            // This is here to attempt to get around a Websphere J9 (JDK6) deadlock
            ClassLoader classLoader = UtilityClass.class.getClassLoader();
            classLoader.getResource(WeaveUtils.getClassResourceName(UtilityClass.class.getName()));

            // load code source classes before transformer activates
            // to prevent deadlock. See JAVA-1945
            if (null != InstrumentationContextManager.class.getProtectionDomain()) {
                InstrumentationContextManager.class.getProtectionDomain().getCodeSource();
            }
            if (null != Throwable.class.getProtectionDomain()) {
                Throwable.class.getProtectionDomain().getCodeSource();
            }

            // Go through a simple scenario involving ConcurrentHashMap iteration and singleton set iteration.
            // This resolves a ClassCircularityError that occurs in WeavePackageManager.getOptimizedWeavePackages().
            ConcurrentMap<Object, Object> optimizedWeavePackagePreload = new ConcurrentHashMap<>();

            // Load ConcurrentHashMap.Values class and ConcurrentHashMap.ValuesIterator
            optimizedWeavePackagePreload.values().iterator();

            optimizedWeavePackagePreload.put(new Object(), new Object());
            for (Map.Entry<Object, Object> entry : optimizedWeavePackagePreload.entrySet()) {
                Set<Object> singletonSet = Collections.singleton(entry.getKey());
                for (Object object : singletonSet) {
                    // no-op
                }
            }

            for (Object value : optimizedWeavePackagePreload.values()) {
                // no-op
            }

            // The following prevents the ReferenceHandler (gc related) thread from causing a startup deadlock on 1.6
            Reference.class.getName();
            InterruptedException.class.getName();
            ReferenceQueue.class.getName();
            try {
                Class.forName("sun.misc.Cleaner");
            } catch (Throwable e) {
                // This might happen on non-sun JVMs
            }
        } catch (Throwable e) {
            Agent.LOG.log(Level.FINE, e, "Exception while performing initial loading");
        }
    }

}

/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.builtin;

import java.security.ProtectionDomain;

import com.newrelic.agent.instrumentation.InstrumentedClass;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

/**
 * This Weave class is used only on "java.lang.ClassLoader" to override the java security manager when a configuration
 * flag is flipped in the agent. This allows instrumentation modules to avoid having to go through the SecurityManager.
 * 
 * This is configured and hooked up in {@link com.newrelic.agent.instrumentation.weaver.ClassLoaderClassTransformer}
 * when the SecurityManager is not null and the configuration flag has been set.
 */
@Weave(type = MatchType.ExactClass, originalName = "java.lang.ClassLoader")
public class ClassLoaderPackageAccessInstrumentation {

//    private void checkPackageAccess(Class<?> cls, ProtectionDomain pd) {
//        if (cls.isAnnotationPresent(InstrumentedClass.class)) {
//            return;
//        }
//        Weaver.callOriginal();
//    }

}

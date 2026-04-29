/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

class JpmsModuleHelper {

    private static final Method GET_MODULE;
    private static final Method IS_NAMED;
    private static final Method GET_ALL_LOADED;
    private static final Method GET_UNNAMED;
    private static final Method REDEFINE_MODULE;

    private static final AtomicBoolean mulePatched = new AtomicBoolean();

    static {
        Method getModule = null, isNamed = null, getAllLoaded = null,
               getUnnamed = null, redefineModule = null;
        try {
            Class<?> modCls = Class.forName("java.lang.Module");
            getModule = Class.class.getMethod("getModule");
            isNamed = modCls.getMethod("isNamed");
            getAllLoaded = Instrumentation.class.getMethod("getAllLoadedClasses");
            getUnnamed = ClassLoader.class.getMethod("getUnnamedModule");
            redefineModule = Instrumentation.class.getMethod("redefineModule",
                    modCls, Set.class, Map.class, Map.class, Set.class, Map.class);
        } catch (Throwable ignored) {
            // Java 8: leave null
        }

        GET_MODULE = getModule;
        IS_NAMED = isNamed;
        GET_ALL_LOADED = getAllLoaded;
        GET_UNNAMED = getUnnamed;
        REDEFINE_MODULE = redefineModule;
    }

    /**
     * Triggers a one-shot Mule module patch on the first {@code org/mule/runtime/} class.
     * No-op on Java 8, for non-Mule classes, and after the first call.
     */
    static void addReadsToUnnamedModule(Instrumentation inst, String className, ClassLoader cl) {
        if (GET_MODULE == null || inst == null || cl == null || className == null) return;
        if (!className.startsWith("org/mule/runtime/")) return;
        if (!mulePatched.compareAndSet(false, true)) return;
        try {
            patchMuleModules(unwrap(inst), cl);
        } catch (Throwable ignored) {}
    }

    // Single-pass scan: adds reads(unnamed + all named modules) to every named org.mule.runtime.* module.
    private static void patchMuleModules(Instrumentation inst, ClassLoader cl) throws Exception {
        Object unnamed = GET_UNNAMED.invoke(cl);
        Set<Object> reads = new HashSet<>(Collections.singleton(unnamed));
        Set<Object> muleModules = new HashSet<>();

        for (Class<?> cls : loadedClasses(inst)) {
            Object module = moduleOf(cls);
            if (!isNamed(module)) continue;

            reads.add(module);
            if (cls.getName().startsWith("org.mule.runtime.")) {
                muleModules.add(module);
            }
        }

        for (Object module : muleModules) {
            REDEFINE_MODULE.invoke(inst, module, reads,
                    Collections.emptyMap(), Collections.emptyMap(),
                    Collections.emptySet(), Collections.emptyMap());
        }
    }

    private static Instrumentation unwrap(Instrumentation inst) {
        for (Class<?> cls = inst.getClass(); cls != null && cls != Object.class; cls = cls.getSuperclass()) {
            try {
                Field field = cls.getDeclaredField("delegate");
                field.setAccessible(true);
                Object delegate = field.get(inst);
                return delegate instanceof Instrumentation ? unwrap((Instrumentation) delegate) : inst;
            } catch (NoSuchFieldException ignored) {
            } catch (Throwable t) { break; }
        }
        return inst;
    }

    private static Class<?>[] loadedClasses(Instrumentation inst) throws Exception {
        return (Class<?>[]) GET_ALL_LOADED.invoke(inst);
    }

    private static Object moduleOf(Class<?> cls) throws Exception {
        return GET_MODULE.invoke(cls);
    }

    private static boolean isNamed(Object module) throws Exception {
        return (boolean) IS_NAMED.invoke(module);
    }

    private JpmsModuleHelper() {}
}

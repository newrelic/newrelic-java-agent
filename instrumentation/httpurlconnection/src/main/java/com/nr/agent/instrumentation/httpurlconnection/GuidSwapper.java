/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.httpurlconnection;

import com.newrelic.api.agent.TracedMethod;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is an attempt to make swapping tracers' guids less hacky.
 */
class GuidSwapper {

    /**
     * Swaps the guids from the provided tracers.
     */
    static void swap(TracedMethod tracer1, TracedMethod tracer2) {
        if (tracer1 == tracer2 || tracer1 == null || tracer2 == null) {
            return;
        }
        GuidChanger changer1 = GuidSwapper.getGuidChanger(tracer1.getClass());
        GuidChanger changer2 = GuidSwapper.getGuidChanger(tracer2.getClass());

        if (changer1 != GuidChanger.NULL_GUIDCHANGER && changer2 != GuidChanger.NULL_GUIDCHANGER) {
            String guid1 = changer1.getGuid(tracer1);
            String guid2 = changer2.getGuid(tracer2);
            changer1.setGuid(tracer1, guid2);
            changer2.setGuid(tracer2, guid1);
        }
    }

    // this cache should not be big, maybe just a single entry, so there is no mechanism to expire items
    private static Map<Class<? extends TracedMethod>, GuidChanger> CACHE = new ConcurrentHashMap<>(8);

    /**
     * Get a GuidChanger for a given TracedMethod subclass.
     * In most, if not all, cases it will be a DefaultTracer.
     * But this makes sure that it will be able to read/write the guid regardless of class.
     *
     */
    private static GuidChanger getGuidChanger(Class<? extends TracedMethod> clazz) {
        GuidChanger guidChanger = CACHE.get(clazz);
        if (guidChanger != null) {
            return guidChanger;
        }

        Field field = null;
        for (Field declaredField : clazz.getDeclaredFields()) {
            if ("guid".equals(declaredField.getName())) {
                // make sure the field is not final
                if ((declaredField.getModifiers() & Modifier.FINAL) == 0 ) {
                    field = declaredField;
                    field.setAccessible(true);
                }
                break;
            }
        }
        guidChanger = GuidChanger.forField(field);
        CACHE.put(clazz, guidChanger);
        return guidChanger;
    }

    private static class GuidChanger {

        // Dummy instance for classes that do not have a guid
        static final GuidChanger NULL_GUIDCHANGER = new GuidChanger(null);

        static GuidChanger forField(Field field) {
            return field == null ? NULL_GUIDCHANGER : new GuidChanger(field);
        }

        private final Field declaredField;

        private GuidChanger(Field declaredField) {
            this.declaredField = declaredField;
        }

        public String getGuid(Object target) {
            try {
                return declaredField.get(target).toString();
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        public void setGuid(Object target, String guid) {
            try {
                declaredField.set(target, guid);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

}

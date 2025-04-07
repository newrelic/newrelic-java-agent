/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.utils;

import java.io.IOException;

import org.objectweb.asm.ClassWriter;

/**
 * The normal ClassWriter uses the context classloader to resolve classes as it tries to compute frames which doesn't
 * work for us because we're usually using alternate classloaders and referencing classes that can't be resolved through
 * normal methods (they may not even be loaded yet). Also, the base ClassWriter has a side effect of causing classes to
 * load which will cause trouble.
 * 
 * This writer overrides the methods that try to load classes and uses a {@link ClassInformationFinder} to understand
 * the class structure without actually loading any classes.
 */
class PatchedClassWriter extends ClassWriter {
    private static final String JAVA_LANG_THROWABLE = "java/lang/Throwable";
    private static final String JAVA_LANG_EXCEPTION = "java/lang/Exception";
    private final ClassInformationFinder classInfoFinder;

    PatchedClassWriter(int flags, ClassInformationFinder classInfoFinder) {
        super(flags);
        this.classInfoFinder = classInfoFinder;
    }

    // @Override
    // protected String getCommonSuperClass(final String type1, final String type2) {
    // Class<?> c, d;
    // try {
    // c = Class.forName(type1.replace('/', '.'), false, classLoader);
    // d = Class.forName(type2.replace('/', '.'), false, classLoader);
    // } catch (Exception e) {
    // throw new RuntimeException(e.toString());
    // }
    // if (c.isAssignableFrom(d)) {
    // return type1;
    // }
    // if (d.isAssignableFrom(c)) {
    // return type2;
    // }
    // if (c.isInterface() || d.isInterface()) {
    // return "java/lang/Object";
    // } else {
    // do {
    // c = c.getSuperclass();
    // } while (!c.isAssignableFrom(d));
    // return c.getName().replace('.', '/');
    // }
    // }

    @Override
    protected String getCommonSuperClass(final String type1, final String type2) {
        if (type1.equals(type2)) {
            return type1;
        }
        String commonSuperType = null;

        try {
            ClassInformation type1CI = classInfoFinder.getClassInformation(type1);
            ClassInformation type2CI = classInfoFinder.getClassInformation(type2);

            String[] type1Supers;
            if (null == type1CI) {
                type1Supers = new String[0];
            } else {
                type1Supers = type1CI.getAllSuperNames(classInfoFinder).toArray(new String[0]);
            }
            String[] type2Supers;
            if (null == type2CI) {
                type2Supers = new String[0];
            } else {
                type2Supers = type2CI.getAllSuperNames(classInfoFinder).toArray(new String[0]);
            }

            int type1Index = type1Supers.length - 1;
            int type2Index = type2Supers.length - 1;
            while (type1Index >= -1 && type2Index >= -1) {
                String type1Super;
                if (type1Index >= 0) {
                    type1Super = type1Supers[type1Index];
                } else {
                    type1Super = type1;
                }
                String type2Super;
                if (type2Index >= 0) {
                    type2Super = type2Supers[type2Index];
                } else {
                    type2Super = type2;
                }

                if (type1Super.equals(type2Super)) {
                    commonSuperType = type1Super;
                } else {
                    break;
                }
                type1Index--;
                type2Index--;
            }
        } catch (IOException ioe) {
        }

        if (null == commonSuperType) {
            //Required since Java 24
            if (areThrowableAndException(type1, type2)) {
                return JAVA_LANG_THROWABLE;
            }
            return WeaveUtils.JAVA_LANG_OBJECT_NAME;
        }

        return commonSuperType;
    }

    /**
     * getCommonSuperClass may have problematic behavior during the weaving of ClassLoaders.
     * It designates Object as the common supertype of all types, because we're actively hijacking the ClassLoaders at this time and the full class hierarchy may not be available.
     *
     * Starting in Java 24, a new class cast was introduced in ClassLoader.initSystemClassLoader that added a type check between Throwable and Exception. Because of the behavior
     * described above, the common supertype of Throwable and Exception was designated Object by the agent. This led to a VerifyError.
     *
     * This method specifically handles that edge case to prevent the VerifyError for JDK24+.
     */
    private static boolean areThrowableAndException(String type1, String type2){
        return (type1.equals(JAVA_LANG_THROWABLE) && type2.equals(JAVA_LANG_EXCEPTION)) || (type2.equals(JAVA_LANG_THROWABLE) && type1.equals(
                JAVA_LANG_EXCEPTION));
    }
}
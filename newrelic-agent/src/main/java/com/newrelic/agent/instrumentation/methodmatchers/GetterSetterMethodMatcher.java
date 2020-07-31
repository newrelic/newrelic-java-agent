/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.methodmatchers;

import java.util.Set;
import java.util.regex.Pattern;

import org.objectweb.asm.commons.Method;

/**
 * Method matcher to select standard getters and setters. Current implementation only looks at method name and
 * description. It does not analyze fields available in the class or the method body.
 */
public class GetterSetterMethodMatcher implements MethodMatcher {
    static final Pattern GETTER_METHOD_PATTERN = Pattern.compile("^get[A-Z][a-zA-Z0-9_]*$");
    static final Pattern IS_METHOD_PATTERN = Pattern.compile("^is[A-Z][a-zA-Z0-9_]*$");
    static final Pattern SETTER_METHOD_PATTERN = Pattern.compile("^set[A-Z][a-zA-Z0-9_]*$");
    static final Pattern GETTER_DESCRIPTION_PATTERN = Pattern.compile("^\\(\\)[^V].*$");
    static final Pattern IS_DESCRIPTION_PATTERN = Pattern.compile("^\\(\\)(?:Z|Ljava/lang/Boolean;)$");
    static final Pattern SETTER_DESCRIPTION_PATTERN = Pattern.compile("^\\(\\[?[A-Z][a-zA-Z0-9_/;]*\\)V$");
    private static GetterSetterMethodMatcher matcher = new GetterSetterMethodMatcher();

    private GetterSetterMethodMatcher() {
    }

    public static GetterSetterMethodMatcher getGetterSetterMethodMatcher() {
        return matcher;
    }

    @Override
    public boolean matches(int access, String name, String desc, Set<String> annotations) {
        if (GETTER_METHOD_PATTERN.matcher(name).matches()) {
            return GETTER_DESCRIPTION_PATTERN.matcher(desc).matches();
        }
        if (IS_METHOD_PATTERN.matcher(name).matches()) {
            return IS_DESCRIPTION_PATTERN.matcher(desc).matches();
        }
        if (SETTER_METHOD_PATTERN.matcher(name).matches()) {
            return SETTER_DESCRIPTION_PATTERN.matcher(desc).matches();
        }
        return false;
    }

    @Override
    public Method[] getExactMethods() {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        // Singleton, so use default.
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        // Singleton, so use default.
        return super.hashCode();
    }

}

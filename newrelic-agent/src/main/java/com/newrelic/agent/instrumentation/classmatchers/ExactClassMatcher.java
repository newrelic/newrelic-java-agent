/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.classmatchers;

import java.util.Arrays;
import java.util.Collection;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import com.newrelic.agent.util.Strings;

public class ExactClassMatcher extends ClassMatcher {

    private final Type type;
    private final String className;
    private final String internalName;

    public ExactClassMatcher(String className) {
        this.type = Type.getObjectType(Strings.fixInternalClassName(className));
        this.className = type.getClassName();
        this.internalName = type.getInternalName();
    }

    @Override
    public boolean isMatch(ClassLoader loader, ClassReader cr) {
        return cr.getClassName().equals(internalName);
    }

    @Override
    public boolean isMatch(Class<?> clazz) {
        return clazz.getName().equals(className);
    }

    public static ClassMatcher or(String... classNames) {
        return OrClassMatcher.createClassMatcher(classNames);
    }

    public String getInternalClassName() {
        return internalName;
    }

    @Override
    public boolean isExactClassMatcher() {
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ExactClassMatcher other = (ExactClassMatcher) obj;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ExactClassMatcher(" + internalName + ")";
    }

    @Override
    public Collection<String> getClassNames() {
        return Arrays.asList(internalName);
    }

}

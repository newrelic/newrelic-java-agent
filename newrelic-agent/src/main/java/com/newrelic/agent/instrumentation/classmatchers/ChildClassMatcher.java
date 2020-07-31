/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.classmatchers;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.util.Strings;
import com.newrelic.agent.util.asm.Utils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ChildClassMatcher extends ClassMatcher {

    private final String internalSuperClassName;
    private final String superClassName;
    private final boolean onlyMatchChildren;
    /**
     * This is designed for child classes which might be in the excludes file. The internalSuperClassName will also be
     * placed in this list.
     **/
    private final Set<String> classesToMatch;

    public ChildClassMatcher(String superClassName) {
        this(superClassName, true);
    }

    public ChildClassMatcher(String superClassName, boolean onlyMatchChildren) {
        this(superClassName, onlyMatchChildren, null);
    }

    public ChildClassMatcher(String superClassName, boolean onlyMatchChildren, String[] specificChildClasses) {
        superClassName = Strings.fixInternalClassName(superClassName);
        if (superClassName.indexOf('/') < 0) {
            throw new RuntimeException("Invalid class name format");
        }
        this.superClassName = Type.getObjectType(superClassName).getClassName();
        this.internalSuperClassName = superClassName;
        this.onlyMatchChildren = onlyMatchChildren;
        // create the list of child to match
        classesToMatch = new HashSet<>();
        classesToMatch.add(internalSuperClassName);
        if (specificChildClasses != null) {
            classesToMatch.addAll(Arrays.asList(specificChildClasses));
        }
    }

    @Override
    public boolean isMatch(ClassLoader loader, ClassReader cr) {
        if (loader == null) {
            loader = AgentBridge.getAgent().getClass().getClassLoader();
        }
        if (cr.getClassName().equals(internalSuperClassName)) {
            if (onlyMatchChildren) {
                return false;
            }
            return true;
        }

        return isSuperMatch(loader, cr.getSuperName());
    }

    private boolean isSuperMatch(ClassLoader loader, String superName) {

        do {
            if (superName.equals(internalSuperClassName)) {
                return true;
            }
            URL resource = Utils.getClassResource(loader, superName);
            if (resource == null) {
                return false;
            }
            try {
                try (InputStream inputStream = resource.openStream()) {
                    ClassReader cr = new ClassReader(inputStream);
                    superName = cr.getSuperName();
                }
            } catch (IOException ex) {
                // ignore
                return false;
            }
            // super name will be null when we get to java.lang.Object
        } while (superName != null);

        return false;
    }

    @Override
    public boolean isMatch(Class<?> clazz) {
        if (clazz.getName().equals(superClassName)) {
            if (onlyMatchChildren) {
                return false;
            }
            return true;
        }
        while ((clazz = clazz.getSuperclass()) != null) {
            if (clazz.getName().equals(superClassName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((internalSuperClassName == null) ? 0 : internalSuperClassName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ChildClassMatcher other = (ChildClassMatcher) obj;
        if (internalSuperClassName == null) {
            if (other.internalSuperClassName != null) {
                return false;
            }
        } else if (!internalSuperClassName.equals(other.internalSuperClassName)) {
            return false;
        }
        return true;
    }

    @Override
    public Collection<String> getClassNames() {
        return classesToMatch;
    }

}

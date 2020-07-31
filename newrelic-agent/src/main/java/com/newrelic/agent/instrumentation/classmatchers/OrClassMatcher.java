/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.classmatchers;

import com.newrelic.agent.util.Strings;
import org.objectweb.asm.ClassReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class OrClassMatcher extends ManyClassMatcher {

    public OrClassMatcher(ClassMatcher... matchers) {
        this(Arrays.asList(matchers));
    }

    public OrClassMatcher(Collection<ClassMatcher> matchers) {
        super(getOptimizedClassMatchers(matchers));
    }

    private static Collection<ClassMatcher> getOptimizedClassMatchers(Collection<ClassMatcher> matchers) {
        // this is a little ugly
        Collection<String> exactMatcherClassNames = new ArrayList<>(matchers.size());
        Collection<ClassMatcher> otherMatchers = new LinkedList<>();
        for (ClassMatcher matcher : matchers) {
            if (matcher instanceof ExactClassMatcher) {
                exactMatcherClassNames.add(((ExactClassMatcher) matcher).getInternalClassName());
            } else {
                otherMatchers.add(matcher);
            }
        }
        if (exactMatcherClassNames.size() <= 1) {
            return matchers;
        }

        otherMatchers.add(OrClassMatcher.createClassMatcher(exactMatcherClassNames.toArray(new String[exactMatcherClassNames.size()])));
        return otherMatchers;
    }

    @Override
    public boolean isMatch(ClassLoader loader, ClassReader cr) {
        for (ClassMatcher matcher : getClassMatchers()) {
            if (matcher.isMatch(loader, cr)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isMatch(Class<?> clazz) {
        for (ClassMatcher matcher : getClassMatchers()) {
            if (matcher.isMatch(clazz)) {
                return true;
            }
        }
        return false;
    }

    public static ClassMatcher getClassMatcher(ClassMatcher... classMatchers) {
        return getClassMatcher(Arrays.asList(classMatchers));
    }

    public static ClassMatcher getClassMatcher(Collection<ClassMatcher> classMatchers) {
        classMatchers = getOptimizedClassMatchers(classMatchers);
        if (classMatchers.size() == 0) {
            return new NoMatchMatcher();
        } else if (classMatchers.size() == 1) {
            return classMatchers.iterator().next();
        } else {
            return new OrClassMatcher(classMatchers);
        }
    }

    static ClassMatcher createClassMatcher(String... classNames) {
        if (classNames.length == 0) {
            return NoMatchMatcher.MATCHER;
        } else if (classNames.length == 1) {
            return new ExactClassMatcher(classNames[0]);
        }
        return new StringOrClassMatcher(classNames);
    }

    private static class StringOrClassMatcher extends ClassMatcher {

        private final Set<String> internalClassNames;
        private final Set<String> classNames;

        public StringOrClassMatcher(String... internalClassNames) {
            this.internalClassNames = new HashSet<>();
            for (String name : internalClassNames) {
                this.internalClassNames.add(Strings.fixInternalClassName(name));
            }
            this.classNames = new HashSet<>();
            for (String name : this.internalClassNames) {
                this.classNames.add(name.replace('/', '.'));
            }
        }

        @Override
        public boolean isMatch(ClassLoader loader, ClassReader cr) {
            return internalClassNames.contains(cr.getClassName());
        }

        @Override
        public boolean isMatch(Class<?> clazz) {
            return classNames.contains(clazz.getName());
        }

        @Override
        public String toString() {
            return classNames.toString();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((internalClassNames == null) ? 0 : internalClassNames.hashCode());
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
            StringOrClassMatcher other = (StringOrClassMatcher) obj;
            if (internalClassNames == null) {
                if (other.internalClassNames != null) {
                    return false;
                }
            } else if (!internalClassNames.equals(other.internalClassNames)) {
                return false;
            }
            return true;
        }

        @Override
        public boolean isExactClassMatcher() {
            return true;
        }

        @Override
        public Collection<String> getClassNames() {
            return internalClassNames;
        }
    }
}

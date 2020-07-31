/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.methodmatchers;

import org.objectweb.asm.commons.Method;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A method matcher which 'ors' a set of method matchers - if any of the child matchers match, this matcher matches.
 */
public final class OrMethodMatcher extends ManyMethodMatcher {
    private OrMethodMatcher(MethodMatcher... methodMatchers) {
        super(methodMatchers);
    }

    private OrMethodMatcher(Collection<MethodMatcher> methodMatchers) {
        super(methodMatchers);
    }

    @Override
    public boolean matches(int access, String name, String desc, Set<String> annotations) {
        for (MethodMatcher matcher : methodMatchers) {
            if (matcher.matches(access, name, desc, annotations)) {
                return true;
            }
        }
        return false;
    }

    public static final MethodMatcher getMethodMatcher(MethodMatcher... matchers) {
        return getMethodMatcher(Arrays.asList(matchers));
    }

    public static final MethodMatcher getMethodMatcher(Collection<MethodMatcher> matchers) {
        if (matchers.size() == 1) {
            return matchers.iterator().next();
        }

        Map<String, DescMethodMatcher> exactMatchers = new HashMap<>();
        List<MethodMatcher> otherMatchers = new LinkedList<>();
        for (MethodMatcher matcher : matchers) {
            if (matcher instanceof ExactMethodMatcher) {
                ExactMethodMatcher m = (ExactMethodMatcher) matcher;
                if (m.getDescriptions().isEmpty()) {
                    otherMatchers.add(matcher);
                } else {
                    DescMethodMatcher descMatcher = exactMatchers.get(m.getName());
                    if (descMatcher == null) {
                        descMatcher = new DescMethodMatcher(m.getDescriptions());
                        exactMatchers.put(m.getName().intern(), descMatcher);
                    } else {
                        descMatcher.addDescriptions(m.getDescriptions());
                    }
                }
            } else {
                otherMatchers.add(matcher);
            }
        }
        MethodMatcher matcher = new OrExactMethodMatchers(exactMatchers);
        if (otherMatchers.size() == 0) {
            return matcher;
        } else {
            otherMatchers.add(matcher);
            return new OrMethodMatcher(otherMatchers);
        }
    }

    @Override
    public String toString() {
        return "Or Match " + methodMatchers;
    }

    private static class OrExactMethodMatchers implements MethodMatcher {

        private final Map<String, DescMethodMatcher> exactMatchers;

        public OrExactMethodMatchers(Map<String, DescMethodMatcher> exactMatchers) {
            this.exactMatchers = exactMatchers;
        }

        @Override
        public boolean matches(int access, String name, String desc, Set<String> annotations) {
            DescMethodMatcher matcher = exactMatchers.get(name);
            if (matcher == null) {
                return false;
            } else {
                return matcher.matches(access, name, desc, annotations);
            }
        }

        @Override
        public String toString() {
            return exactMatchers.toString();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((exactMatchers == null) ? 0 : exactMatchers.hashCode());
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
            OrExactMethodMatchers other = (OrExactMethodMatchers) obj;
            if (exactMatchers == null) {
                if (other.exactMatchers != null) {
                    return false;
                }
            } else if (!exactMatchers.equals(other.exactMatchers)) {
                return false;
            }
            return true;
        }

        @Override
        public Method[] getExactMethods() {
            List<Method> methods = new ArrayList<>();
            for (Entry<String, DescMethodMatcher> entry : this.exactMatchers.entrySet()) {
                for (String desc : entry.getValue().descriptions) {
                    methods.add(new Method(entry.getKey(), desc));
                }
            }

            return methods.toArray(new Method[methods.size()]);
        }

    }

    private static class DescMethodMatcher implements MethodMatcher {

        private Set<String> descriptions;

        public DescMethodMatcher(Set<String> set) {
            this.descriptions = new HashSet<>(set);
        }

        public void addDescriptions(Set<String> desc) {
            descriptions.addAll(desc);
        }

        @Override
        public boolean matches(int access, String name, String desc, Set<String> annotations) {
            return descriptions.contains(desc);
        }

        @Override
        public String toString() {
            return descriptions.toString();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((descriptions == null) ? 0 : descriptions.hashCode());
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
            DescMethodMatcher other = (DescMethodMatcher) obj;
            if (descriptions == null) {
                if (other.descriptions != null) {
                    return false;
                }
            } else if (!descriptions.equals(other.descriptions)) {
                return false;
            }
            return true;
        }

        @Override
        public Method[] getExactMethods() {
            return null;
        }

    }

}

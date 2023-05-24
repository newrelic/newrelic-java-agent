/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.classmatchers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public abstract class ManyClassMatcher extends ClassMatcher {
    private final ClassMatcher[] matchers;
    private final boolean isExact;

    public ManyClassMatcher(Collection<ClassMatcher> matchers) {
        super();
        this.matchers = matchers.toArray(new ClassMatcher[matchers.size()]);
        isExact = determineIfExact(this.matchers);
    }

    private static boolean determineIfExact(ClassMatcher[] matchers) {
        for (ClassMatcher matcher : matchers) {
            if (!matcher.isExactClassMatcher()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isExactClassMatcher() {
        return isExact;
    }

    protected ClassMatcher[] getClassMatchers() {
        return matchers;
    }

    @Override
    public Collection<String> getClassNames() {
        Collection<String> classNames = new ArrayList<>();
        for (ClassMatcher matcher : matchers) {
            classNames.addAll(matcher.getClassNames());
        }
        return classNames;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(matchers);
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
        ManyClassMatcher other = (ManyClassMatcher) obj;
        if (!Arrays.equals(matchers, other.matchers))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + Arrays.toString(matchers) + ")";
    }

}

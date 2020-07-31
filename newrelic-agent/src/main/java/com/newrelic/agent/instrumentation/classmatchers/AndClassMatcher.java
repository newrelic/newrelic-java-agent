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

public class AndClassMatcher extends ManyClassMatcher {

    public AndClassMatcher(ClassMatcher... matchers) {
        this(Arrays.asList(matchers));
    }

    public AndClassMatcher(Collection<ClassMatcher> matchers) {
        super(matchers);
    }

    public static ClassMatcher getClassMatcher(ClassMatcher... classMatchers) {
        if (classMatchers.length == 0) {
            return new NoMatchMatcher();
        } else if (classMatchers.length == 1) {
            return classMatchers[0];
        } else {
            return new AndClassMatcher(classMatchers);
        }
    }

    @Override
    public boolean isMatch(ClassLoader loader, ClassReader cr) {
        for (ClassMatcher matcher : getClassMatchers()) {
            if (!matcher.isMatch(loader, cr)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isMatch(Class<?> clazz) {
        for (ClassMatcher matcher : getClassMatchers()) {
            if (!matcher.isMatch(clazz)) {
                return false;
            }
        }
        return true;
    }

}

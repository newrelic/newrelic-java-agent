/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.classmatchers;

import java.util.Collection;
import java.util.Collections;

import org.objectweb.asm.ClassReader;

public class NoMatchMatcher extends ClassMatcher {
    public static final ClassMatcher MATCHER = new NoMatchMatcher();

    @Override
    public boolean isMatch(ClassLoader loader, ClassReader cr) {
        return false;
    }

    @Override
    public boolean isMatch(Class<?> clazz) {
        return false;
    }

    @Override
    public Collection<String> getClassNames() {
        return Collections.emptyList();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
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
        return true;
    }

}

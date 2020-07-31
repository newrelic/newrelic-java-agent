/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

import com.newrelic.agent.util.InsertOnlyArray;

/**
 * A cache of ClassMethodSignatures. Signatures are add at class load time when we add method tracers and are used when
 * an instrumented method is invoked.
 * 
 * {@link ClassMethodSignature}s cannot be removed from this cache, so use it with that in mind.
 */
public class ClassMethodSignatures {
    private static final ClassMethodSignatures INSTANCE = new ClassMethodSignatures();

    private final InsertOnlyArray<ClassMethodSignature> signatures;

    ClassMethodSignatures() {
        this(1000);
    }

    ClassMethodSignatures(int capacity) {
        signatures = new InsertOnlyArray<>(capacity);
    }

    public static ClassMethodSignatures get() {
        return INSTANCE;
    }

    public ClassMethodSignature get(int index) {
        return signatures.get(index);
    }

    public int add(ClassMethodSignature signature) {
        return signatures.add(signature);
    }

    public int getIndex(ClassMethodSignature signature) {
        return signatures.getIndex(signature);
    }
}

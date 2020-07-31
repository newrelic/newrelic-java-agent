/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage;

import java.util.List;
import java.util.Map;

import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.ClassNode;

import com.google.common.collect.ImmutableMap;
import com.newrelic.weave.utils.ClassCache;
import com.newrelic.weave.utils.WeaveUtils;

/**
 * Stores the result of weaving a single class against a weave package.
 */
public class PackageWeaveResult {
    private final String className;
    private final ClassNode composite;
    private final PackageValidationResult validationResult;

    /**
     * original name -> list of weaved methods
     */
    private final Map<String, List<Method>> weavedMethods;

    /**
     * annotation proxy class name -> class bytes
     */
    private final Map<String, byte[]> annotationProxyClasses;

    public PackageWeaveResult(PackageValidationResult validationResult, String className, ClassNode composite,
            Map<String, List<Method>> weavedMethods, Map<String, byte[]> annotationProxyClasses) {
        this.validationResult = validationResult;
        this.className = className;
        this.composite = composite;
        this.weavedMethods = ImmutableMap.copyOf(weavedMethods);
        this.annotationProxyClasses = ImmutableMap.copyOf(annotationProxyClasses);
    }

    /**
     * Indicates whether any bytecode was weaved into the class.
     * 
     * @return <code>true</code> if bytecode was weaved
     */
    public boolean weavedClass() {
        return weavedMethods.size() > 0;
    }

    /**
     * Returns the byte representation of composite class or null if there was no match.
     */
    public byte[] getCompositeBytes(ClassCache cache) {
        if (this.weavedClass()) {
            return WeaveUtils.convertToClassBytes(composite, cache);
        } else {
            return null;
        }
    }

    /**
     * The target class name.
     */
    public String getClassName() {
        return className;
    }

    /**
     * The composite class (target + weave), or <code>null</code> if there was no match.
     */
    public ClassNode getComposite() {
        if (this.weavedClass()) {
            return composite;
        } else {
            return null;
        }
    }

    /**
     * original class name -> list of methods weaved
     */
    public Map<String, List<Method>> getWeavedMethods() {
        return weavedMethods;
    }

    /**
     * @return the {@link PackageValidationResult} this weave result originated from.
     */
    public PackageValidationResult getValidationResult() {
        return this.validationResult;
    }

    /**
     * @return a map of any newly added annotation proxy classes as a result of this weave package
     */
    public Map<String, byte[]> getAnnotationProxyClasses() {
        return annotationProxyClasses;
    }
}

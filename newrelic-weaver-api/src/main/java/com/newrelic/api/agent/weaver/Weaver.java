/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent.weaver;

import java.lang.annotation.Annotation;

/**
 * 
 * @see Weave
 */
public final class Weaver {

    private Weaver() {
    }

    /**
     * Invoke the original method implementation of a weaved method. This call is replaced with the original method
     * body.
     * 
     * @return
     */
    public static final <T> T callOriginal() {
        return null;
    }

    /**
     * Get the name of the weave package. This is the name defined in the weave package's manifest file (attribute
     * Implementation-Title).
     * 
     * @return
     */
    public static final String getImplementationTitle() {
        return "";
    }

    /**
     * Returns a view of the provided annotation if it is present on the current class. If no annotation matching
     * the provided class is present then null will be returned.
     * 
     * @param annotationClass the class of annotation to look for
     * @return the annotation, if present on the class, null otherwise
     */
    public static final <T extends Annotation> T getClassAnnotation(Class<T> annotationClass) {
        return null;
    }

    /**
     * Returns a view of the provided annotation if it is present on the current method. If no annotation that matches
     * the provided annotationClass is present then null will be returned.
     *
     * @param annotationClass the class of annotation to look for
     * @return the annotation, if present on the method, null otherwise
     */
    public static final <T extends Annotation> T getMethodAnnotation(Class<T> annotationClass) {
        return null;
    }

}

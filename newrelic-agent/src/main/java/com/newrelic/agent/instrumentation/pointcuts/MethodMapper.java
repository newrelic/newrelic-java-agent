/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.pointcuts;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.objectweb.asm.commons.Method;

/**
 * This annotation is used to map agent interface methods to existing methods declared in the target class (not a
 * superclass) that have been matched using the {@link InterfaceMapper}.
 */
@LoadOnBootstrap
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MethodMapper {
    static final String NULL = "";

    /**
     * If true, the original method should be invoked as an interface, otherwise virtual.
     * 
     */
    boolean invokeInterface() default true;

    /**
     * The original method name.
     * 
     */
    String originalMethodName();

    /**
     * The original method description. If not specified, the method descriptor of the method that this annotation is
     * applied to will be used.
     * 
     * @see Method#getDescriptor()
     */
    String originalDescriptor() default NULL;
}

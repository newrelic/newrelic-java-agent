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

/**
 * This annotation can be placed on agent interfaces to mix them into interfaces and classes as they are loaded. The
 * methods defined by the agent interface should already be defined in the mix-in target unless the methods use the
 * {@link MethodMapper} annotation.
 * 
 * @deprecated use the weaver
 */
@LoadOnBootstrap
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface InterfaceMapper {
    String originalInterfaceName();

    String[] className() default {};

    /**
     * A list of class names to skip.
     * 
     */
    String[] skip() default {};

    /**
     * In Agents through 3.12.0, the default value here was InterfaceImplementationClassVisitor.class. But now that this
     * class loads on the bootstrap, we cannot return that (the set of classes loaded on the bootstrap must be closed
     * with respect to load-time dependencies, which would require us to pull ASM to the bootstrap, which we don't want
     * to do). So we now just return a marker value, and callers who care have to substitute the old default (see
     * InterfaceImplementationClassTransformer; also, don't turn this comment into a Javadoc link.)
     * <p>
     * This work is part of the fix for JAVA-609.
     * 
     * @return the type of class visitor required for this mapper, or a default marker value as explained above.
     */
    Class<?> classVisitor() default Object.class;
}

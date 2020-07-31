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

import com.newrelic.agent.instrumentation.FieldAccessorGeneratingClassAdapter;
import com.newrelic.agent.instrumentation.StopProcessingException;

/**
 * This is used to create field accessors (getters and/or setters) on existing classes. This can be used to add
 * accessors for an existing field, or to add a new field with accessors.
 * 
 * @deprecated use the weaver
 */
@LoadOnBootstrap
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldAccessor {
    /**
     * The name of the field. If this is not an existing field, the {@link FieldAccessorGeneratingClassAdapter} will
     * automatically mangle the field name so that it does not conflict with existing field names.
     * 
     */
    String fieldName();

    /**
     * The description of the field. For example: "Ljava/lang/Object;".
     * 
     * @return the field descriptor
     */
    String fieldDesc() default "";

    /**
     * If false, the field will be created.
     * 
     * @throws a {@link StopProcessingException} if true and the field exists
     * @return true if the field exists
     */
    boolean existingField() default false;

    /**
     * If the field will be created, use the volatile keyword.
     * 
     * @return true if the field should be volatile
     */
    boolean volatileAccess() default false;
}

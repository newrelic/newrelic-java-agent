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

import com.newrelic.agent.instrumentation.InterfaceMixinClassTransformer;

/**
 * Marks interfaces which implement other interfaces that we want to instrument. The originalClassName indicates the
 * original interface can that the declaring interface should be mixed into.
 * 
 * The agent interface may mark its methods with the {@link FieldAccessor} annotation.
 * 
 * The {@link InterfaceMixinClassTransformer} will automatically find classes with this annotation and stitch them into
 * the original interfaces as they are loaded.
 * 
 * @deprecated use the weaver
 */
@LoadOnBootstrap
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface InterfaceMixin {
    String[] originalClassName();
}

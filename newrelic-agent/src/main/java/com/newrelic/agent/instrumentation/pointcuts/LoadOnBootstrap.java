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
 * Marks classes that are not {@link InterfaceMixin} classes, but should be loaded on the bootstrap class loader
 * by the dynamic jar generator. This annotation is used for a very small number of classes that are referenced by mixin
 * interfaces but are not themselves mixin interfaces.
 * <p>
 * This class was deprecated from day 1. It is required as part of the fix for JAVA-609.
 * 
 * @deprecated use the weaver
 */
@LoadOnBootstrap
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoadOnBootstrap {

}

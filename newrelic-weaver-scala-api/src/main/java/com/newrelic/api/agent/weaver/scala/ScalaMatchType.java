/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent.weaver.scala;

/**
 * The match type of a weave instrumentation class written in scala.
 */
public enum ScalaMatchType {
    /**
     * An exact class match. The weave instrumentation will be injected into the class with the exact same name as the
     * weave class.
     */
    ExactClass,
    /**
     * The weave instrumentation will be injected into all classes which implement a scala trait with the exact same
     * name as the weave class. The instrumentation will also be injected into any default implementations of the weaved
     * methods.
     */
    Trait,
    /**
     * The weave instrumentation will be injected into the scala object with the exact same name as the weave class.
     */
    Object
}
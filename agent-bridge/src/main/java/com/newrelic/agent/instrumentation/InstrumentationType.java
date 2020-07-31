/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

/**
 * Indicates why a method was instrumented - through custom configuration, an annotation, or the agent's built in
 * instrumentation sets.
 */
public enum InstrumentationType {

    /**
     * Instrumentation from custom extension xml.
     */
    RemoteCustomXml,
    /**
     * Instrumentation from custom extension xml.
     */
    LocalCustomXml,
    /**
     * Instrumentation from custom extension yaml.
     */
    CustomYaml,
    /**
     * Instrumentation added because of the agent's built in pointcuts. We report them all as the same instrumentation
     * type, causing APM to display them as timing points, even though some point cuts actually don't add tracers.
     */
    Pointcut,
    /** An @Trace annotation from weaved instrumentation. */
    TracedWeaveInstrumentation,
    /**
     * Instrumentation added because of an instrumentation packaged containing a weave class.
     */
    WeaveInstrumentation,
    /**
     * Instrumentation added because of a trace annotation on a method.
     */
    TraceAnnotation,
    /**
     * Custom instrumentation that's built into the agent. These are not legacy pointcuts or weaved instrumentation.
     * Currently all of the built in instrumentation times methods. If you want to create, non-timing built in
     * instrumentation, please create a new enum value. The UI assumes this is timed instrumentation.
     */
    BuiltIn,
    /**
     * This should not be used, but it indicates that a method was traced but the agent was unable to track the origin.
     */
    Unknown
}

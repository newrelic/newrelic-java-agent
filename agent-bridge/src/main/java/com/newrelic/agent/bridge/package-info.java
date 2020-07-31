/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

/**
 * The java agent is loaded on the system classloader, but it injects bytecode into numerous 
 * classes loaded on various other classloaders.  That injected bytecode needs to make calls
 * into the agent, but the classloader in which the bytecode is loaded often has no visibility to the 
 * system classloader (and thus the agent's classes).
 * That's where the bridge comes in.  The agent jar contains the agent-bridge jar and the public api jar, and 
 * when the agent starts it appends both of these jars to the bootstrap classloader 
 * (see {@link java.lang.instrument.Instrumentation#appendToBootstrapClassLoaderSearch(java.util.jar.JarFile)}).
 * 
 * Loading the
 * jars on the bootstrap classloader makes them visible to all classloaders.  The interfaces defined in 
 * this agent-bridge module are implemented in the main agent jar, and bytecode that we inject into classes
 * is able to invoke that agent implementation through these interfaces - they're the bridge allowing 
 * our injected code to invoke agent methods loaded through an inaccessible classloader. 
 */
package com.newrelic.agent.bridge;
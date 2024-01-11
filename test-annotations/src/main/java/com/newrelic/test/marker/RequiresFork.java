/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.test.marker;

/**
 * <p>
 * Marker interface to denote a unit/functional/instrumentation test requires a forked JVM.
 * Running on a forked VM takes longer, but some tests may "corrupt" the VM, causing itself
 * or other tests to fail.
 * </p>
 * <p>
 * This category currently only affects tests in newrelic-agent when the tests are run with
 * either <code>-PforkedTests</code> or <code>-PnonForkedTests</code>.
 * <ul>
 *     <li><em>-PforkedTests</em>: will only run the tests in this category, forking the JVM for each test;</li>
 *     <li><em>-PnonForkedTests</em>: will run all other tests, forking the JVM every so many tests.</li>
 * </ul>
 * </p>
 *
 * <p>
 * To mark your test with this category, add the following annotation to the class.
 * <pre>
 *  {@code @Category({ RequiresFork.class })}
 * </pre>
 * </p>
 */
public interface RequiresFork {
}

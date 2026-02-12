/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.test.marker;

/**
 * <p>
 * Marker interface to denote a unit/functional/instrumentation test has proven flaky.
 * This allows us to categorize these and more easily re-run them without having to
 * re-run the entire test suite.
 * </p>
 * <p>
 * This category currently only affects tests in newrelic-agent when the tests are run with
 * <code>-Pflaky</code>.
 * </p>
 * <p>
 * <b>Note: This annotation will override RequiresFork.</b>
 * </p>
 *
 * <p>
 * To mark your test with this category, add the following annotation to the class.
 * <pre>
 *  {@code @Category({ Flaky.class })}
 * </pre>
 * </p>
 */
public interface Flaky {
}

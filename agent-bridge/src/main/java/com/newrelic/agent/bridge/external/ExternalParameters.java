/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.external;

/**
 * @Deprecated Do not use. Use {@link com.newrelic.api.agent.ExternalParameters} instead.
 *
 * Interface for passing external parameters into the {@link TracedMethod}'s reportAsExternal. Use the
 * {@link ExternalParametersFactory} to pass in the correct input parameters to reportAsExternal.
 *
 * @since 3.26.0
 */
@Deprecated
public interface ExternalParameters extends com.newrelic.api.agent.ExternalParameters {
}

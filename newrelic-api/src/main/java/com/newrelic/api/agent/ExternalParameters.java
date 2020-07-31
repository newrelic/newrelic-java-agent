/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

/**
 * Interface for passing external parameters into the {@link TracedMethod}'s reportAsExternal.
 *
 * Depending on the information available to report, use one of the respective builders for the following objects:
 * {@link GenericParameters}, {@link HttpParameters}, {@link DatastoreParameters},
 * {@link MessageProduceParameters}, {@link MessageConsumeParameters}.
 *
 * @since 3.36.0
 */
public interface ExternalParameters {
}
